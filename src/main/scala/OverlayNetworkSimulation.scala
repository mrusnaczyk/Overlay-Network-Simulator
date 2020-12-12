import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.{ActorSystem => ActorSystemTyped}
import akka.actor.{ActorSystem, BootstrapSetup, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import can.actors.{ClusterListener, NodeActor, UserActions, UserActor, ApiServer => CanApiServer}
import can.messages.InitNodeCommand
import can.util.DimensionRange
import chord.ChordSimulation
import chord.actors.{ChordNodeActor, ApiServer => ChordApiServer}
import chord.messages.InitSelfRequest
import com.typesafe.config.ConfigFactory
import data.{Movie, RuntimeStatistic}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.CollectionHasAsScala

object OverlayNetworkSimulation extends App {
  val MODE = System.getenv("MODE")
  val applicationConfig = ConfigFactory.load()
  val clusterName = applicationConfig.getString("clustering.cluster.name")
  implicit val timeout = Timeout(applicationConfig.getInt("cs441.OverlayNetwork.defaultTimeout").seconds)
  val movies = applicationConfig
    .getConfigList("cs441.OverlayNetwork.data").stream()
    .map(movieObj => (
      movieObj.getInt("hash"),
      new Movie(
        movieObj.getString("title"),
        movieObj.getInt("year"),
        movieObj.getDouble("revenue")
      )
    )
    )
    .collect(Collectors.toList[(Int, Movie)])
    .asScala.toList

  println(s"Starting in mode $MODE")

  MODE match {
    case "CAN_NODE"         => startCanNode()
    case "CHORD_NODE"       => startChordNode()
    case "CAN_SIMULATION"   => startCanSimulation()
    case "CHORD_SIMULATION" => startChordSimulation()
    case _                  => println(s"Error: mode $MODE does not match any valid modes")
  }

  def startCanNode() = {
    val system = ActorSystem(applicationConfig.getString("clustering.cluster.name"))
    val nodeId = System.getenv("NODE_ID").toInt
    val node = system.actorOf(Props[NodeActor], s"Node$nodeId")
    val maxWidth = applicationConfig.getInt("cs441.OverlayNetwork.can.maxWidth")

    val bootstrapNode = {
      if(nodeId == 0)
        Optional.ofNullable(null)
      else
        Optional.of("akka://cs441_cluster@seed:1600/user/Node0")
    }.asInstanceOf[Optional[String]]

    node ? InitNodeCommand(
      nodeId,
      bootstrapNode,
      List(
        new DimensionRange(0, maxWidth),
        new DimensionRange(0, maxWidth)
      )
    )
  }

  def startCanSimulation() = {
    val config = ConfigFactory.load()
    val system = ActorSystemTyped(ClusterListener(), clusterName)
    val userSystem = ActorSystem("cs441-useractor", BootstrapSetup(config = ConfigFactory.parseString("akka.actor.provider = local")))
    val cluster = Cluster.get(system)
    val apiServer = new CanApiServer(system.classicSystem)
    apiServer.startServer()


    Thread.sleep(2000)

    val users = List.from(0 to 2)
      .map(userId =>
        userSystem.actorOf(Props[UserActor], s"User${userId}")
      )

    Thread.sleep(2000)

    val stats = movies.flatMap(idMoviePair => {
      system.log.info(s"Write movie $idMoviePair")
      var startTime = System.nanoTime()

      Await.result(
        users(0) ? UserActions.WriteMovie(idMoviePair._1, idMoviePair._2),
        timeout.duration
      )

      val writeDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

      system.log.info(s"Read movie $idMoviePair")

      startTime = System.nanoTime()
      val readResult = Await.result(users(0) ? UserActions.ReadMovie(idMoviePair._1), timeout.duration).toString

      val readDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

      List(
        new RuntimeStatistic("CAN_WRITE_MOVIE", writeDuration.toInt, "ms"),
        new RuntimeStatistic("CAN_READ_MOVIE", readDuration.toInt, "ms")
      )
    })

    Thread.sleep(5000)

    val canWrites = stats
      .filter(stat => stat.category.equals("CAN_WRITE_MOVIE"))

    val canWriteAvg = canWrites
      .map(stat => stat.data)
      .sum / canWrites.length

    val canReads = stats
      .filter(stat => stat.category.equals("CAN_READ_MOVIE"))

    val canReadAvg = canReads
      .map(stat => stat.data)
      .sum / canReads.length

    stats.foreach(stat => system.log.info(stat.toString))

    system.log.info(s"CAN Avg Write Time: $canWriteAvg ms")
    system.log.info(s"CAN Avg Read Time: $canReadAvg ms")
  }

  def startChordNode() = {
    val system = ActorSystem(clusterName)
    val nodeId = System.getenv("NODE_ID").toInt
    val node = system.actorOf(Props[ChordNodeActor], s"Node$nodeId")
    val m = applicationConfig.getInt("cs441.OverlayNetwork.m")

    val bootstrapNode = {
      if(nodeId == 0)
        Option(null)
      else
        Option("akka://cs441_cluster@seed:1600/user/Node0")
    }

    Await.result(
      node ? InitSelfRequest(nodeId, m, bootstrapNode),
      timeout.duration
    )
  }

  def startChordSimulation() = {
    val config = ConfigFactory.load()
    val clusterName = config.getString("clustering.cluster.name")
    val system = ActorSystemTyped(ClusterListener(), clusterName)
    val userSystem = ActorSystem("cs441-useractor", BootstrapSetup(config = ConfigFactory.parseString("akka.actor.provider = local")))
    val cluster = Cluster.get(system)
    val apiServer = new ChordApiServer(system.classicSystem)
    apiServer.startServer()

   Thread.sleep(2000)

    val users = List.from(0 to config.getInt("cs441.OverlayNetwork.numUsers"))
      .map(userId =>
        userSystem.actorOf(Props[UserActor], s"User${userId}")
      )

    Thread.sleep(2000)

    val stats = movies.flatMap(idMoviePair => {
      system.log.info(s"Write movie $idMoviePair")
      var startTime = System.nanoTime()

      Await.result(
        users(0) ? UserActions.WriteMovie(idMoviePair._1, idMoviePair._2),
        timeout.duration
      )

      val writeDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

      system.log.info(s"Read movie $idMoviePair")

      startTime = System.nanoTime()
      val readResult = Await.result(users(0) ? UserActions.ReadMovie(idMoviePair._1), timeout.duration).toString

      val readDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)

      List(
        new RuntimeStatistic("CHORD_WRITE_MOVIE", writeDuration.toInt, "ms"),
        new RuntimeStatistic("CHORD_READ_MOVIE", readDuration.toInt, "ms")
      )
    })

    Thread.sleep(2000)

    val chordWrites = stats
      .filter(stat => stat.category.equals("CHORD_WRITE_MOVIE"))

    val chordWriteAvg = chordWrites
      .map(stat => stat.data)
      .sum / chordWrites.length

    val chordReads = stats
      .filter(stat => stat.category.equals("CHORD_READ_MOVIE"))

    val chordReadAvg = chordReads
      .map(stat => stat.data)
      .sum / chordReads.length

    stats.foreach(stat => system.log.info(stat.toString))

    system.log.info(s"CHORD Avg Write Time: $chordWriteAvg ms")
    system.log.info(s"CHORD Avg Read Time: $chordReadAvg ms")

  }

}
