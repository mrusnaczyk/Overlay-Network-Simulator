import java.util.Optional

import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.{ActorSystem => ActorSystemTyped}
import akka.actor.{ActorSystem, BootstrapSetup, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import can.actors.{ApiServer, ClusterListener, NodeActor, UserActions, UserActor}
import can.messages.InitNodeCommand
import can.util.DimensionRange
import chord.ChordSimulation
import com.typesafe.config.ConfigFactory
import data.Movie

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object OverlayNetworkSimulation extends App {
  val MODE = System.getenv("MODE")
  implicit val timeout = Timeout(2.seconds) // TODO: move to config
  println(s"Starting in mode $MODE")

  MODE match {
    case "CAN_USER_NODE"    => startCanUserNode()
    case "CAN_NODE"         => startCanNode()
    case "CHORD_NODE"       => startChordNode()
    case "CAN_SIMULATION"   => startCanSimulation()
    case "CHORD_SIMULATION" => startChordSimulation()
    case _                  => println(s"Error: mode $MODE does not match any valid modes")
  }

  def startCanUserNode() = {}

  def startCanNode() = {
    val system = ActorSystem("cs441_cluster") // TODO: config
    val nodeId = System.getenv("NODE_ID").toInt
    val node = system.actorOf(Props[NodeActor], s"Node$nodeId")

    val bootstrapNode = {
      if(nodeId == 0)
        Optional.ofNullable(null)
      else
        Optional.of("akka://cs441_cluster@seed:1600/user/Node0") // TODO: config
    }.asInstanceOf[Optional[String]]

    node ? InitNodeCommand(
      nodeId,
      bootstrapNode,
      List(
        new DimensionRange(0, 8), // TODO: config
        new DimensionRange(0, 8)
      )
    )
  }

  def startCanSimulation() = {
    val config = ConfigFactory.load()
    val clusterName = "cs441_cluster" //TODO: config.getString("clustering.cluster.name")
    val system = ActorSystemTyped(ClusterListener(), clusterName)
    val userSystem = ActorSystem("cs441-useractor", BootstrapSetup(config = ConfigFactory.parseString("akka.actor.provider = local"))) // TODO: config
    val cluster = Cluster.get(system)
    val apiServer = new ApiServer(system.classicSystem)
    apiServer.startServer()

    Thread.sleep(2000)

    val users = List.from(0 to 2)
      .map(userId =>
        userSystem.actorOf(Props[UserActor], s"User${userId}")
      )

    Thread.sleep(2000)

    val movies = List(
      (11, new Movie("Inception11", 2011, 12.1111)),
      (25, new Movie("Inception25", 2022, 12.2222))
    )

    movies.foreach(idMoviePair =>
      Await.result(
        users(0) ? UserActions.WriteMovie(idMoviePair._1, idMoviePair._2),
        timeout.duration
      )
    )

    system.log.info(
      Await.result(users(0) ? UserActions.ReadMovie(11), timeout.duration).toString
    )

    system.log.info(
      Await.result(users(0) ? UserActions.ReadMovie(25), timeout.duration).toString
    )

  }

  def startChordUserNode() = {}

  def startChordNode() = {}

  def startChordSimulation() = {
    new ChordSimulation().start
  }

}
