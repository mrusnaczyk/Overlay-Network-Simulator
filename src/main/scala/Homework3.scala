import java.io.{BufferedWriter, File, FileWriter}
import java.security.MessageDigest
import java.util.Optional
import java.util.stream.Collectors

import chord.actors.ChordNodeActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import can.util.{DimensionRange, Zone}
import cats.syntax.either._
import com.typesafe.config.ConfigFactory
import data.Movie
import io.circe.yaml.syntax.AsYaml
import chord.messages._
import can.actors.{ApiServer, NodeActor}
import can.messages._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Homework3 extends App {
  import system.dispatcher

  val applicationConfig = ConfigFactory.load("application.conf")
  val m = applicationConfig.getInt("cs441.OverlayNetwork.m")
  val snapshotBasePath = applicationConfig.getString("cs441.OverlayNetwork.snapshotBasePath")
  implicit val timeout: Timeout = Timeout(10.seconds)

  val system: ActorSystem = ActorSystem("ChordOverlayNetwork")
  val nodeIds = List(0, 2, 4, 6)

//  val cluster = Cluster(system)
//  AkkaManagement(system).start()

  var nodes = nodeIds.map(
    nodeId => system.actorOf(Props[NodeActor], s"Node$nodeId")
  )

  nodes
    .zip(nodeIds)
    .foreach(nodeWithId => {
      val node = nodeWithId._1
      val id = nodeWithId._2

      val refNode = {
        if (id == 0) Optional.ofNullable(null)
        else Optional.of(nodes.head)
      }.asInstanceOf[Optional[ActorRef]]

      val initRequest = node ? InitNodeCommand(id, refNode, List(
        new DimensionRange(0, 8),
        new DimensionRange(0, 8)
      ))
      val initiatedNode = Await.result(initRequest, timeout.duration)
      // TODO: move durations to config
//      system.scheduler.scheduleWithFixedDelay(
//        0.seconds, 250.milliseconds, node, SendHeartbeatCommand
//      )
//      cluster.join(node.path.address)

      nodes.foreach(item =>
        system.log.info(
          Await.result(item ? SnapshotCommand, timeout.duration).toString
        )
      )

      system.log.info(initiatedNode.toString)
    })
  Thread.sleep(1000)

  val apiServer = new ApiServer(system, nodes)
  apiServer.startServer()


//  system.log.info(Await.result(nodes(0) ? ReadMovieCommand(17), 5.seconds).toString())
//  system.log.info(Await.result(nodes(0) ? ReadMovieCommand(33), 5.seconds).toString())



//  system.terminate()




  /*println("Running Chord simulation...")

  val system: ActorSystem = ActorSystem("ChordOverlayNetwork")

  val nodeIds = applicationConfig.getIntList("cs441.OverlayNetwork.network.nodes").asScala.toList
  var nodes = nodeIds.map(
    id => system.actorOf(Props[ChordNodeActor], s"Node$id")
  )

  nodes
    .zip(nodeIds)
    .foreach(nodeWithId => {
      val node = nodeWithId._1
      val id = nodeWithId._2

      val refNode = {
        if (id == 0) Optional.ofNullable(null)
        else Optional.of(nodes.head)
      }.asInstanceOf[Optional[ActorRef]]

      val initRequest = node ? InitSelfRequest(id, m, refNode)
      val initiatedNode = Await.result(initRequest, timeout.duration)
      system.log.info(initiatedNode.toString)
    })

  Thread.sleep(1000)

  for (node <- nodes) {
    val snapshot = Await.result(node ? SnapshotRequest, timeout.duration)
    system.log.info(s"Snapshot: $snapshot")
  }

  val movies = applicationConfig
      .getConfigList("cs441.OverlayNetwork.data").stream()
      .map(movieObj => new Movie(
          movieObj.getString("title"),
          movieObj.getInt("year"),
          movieObj.getDouble("revenue")
      ))
      .collect(Collectors.toList[Movie])
      .asScala.toList

  for (movie <- movies) {
    val writeRequest: Unit = makeWriteRequest(movie)
    system.log.info(writeRequest.toString)
  }

  system.log.info(s"\tResponse: ${makeReadRequest("Sample movie A").toString()}")
  system.log.info(
    s"\tResponse: ${makeReadRequest("Sample movie C").toString()}"
  )

  generateAndSaveGlobalState(snapshotBasePath, nodes)
  // Shutdown simulation
  system.terminate()

  /** Attempts to find the movie with the given `movieName`. If found, the corresponding `Movie` object is returned.
    * Otherwise, a special 'Not found' `Movie` object is returned.
    * @param movieName The title of the movie to find
    * @return `Movie`
    */
  def makeReadRequest(movieName: String) = {
    val movieTitleHash = hashString(movieName)
    val movieReadRequest =
      Await.result(nodes.head ? ReadMovieRequest(movieTitleHash), 2.seconds)

    val movieResult = movieReadRequest match {
      case Some(m: Movie) => m
      case None           => new Movie("Movie_Not_Found", 0, 0.0)
    }

    movieResult
  }

  /** Sends a request to the ring to save the given movie
    * @param movie The `Movie` to save
    */
  def makeWriteRequest(movie: Movie): Unit = {
    val movieTitleHash = hashString(movie.title)

    Await.result(nodes.head ? WriteMovieRequest(movieTitleHash, movie), 2.seconds)
  }

  /**
    * Generates the global state for all of the Chord nodes, and outputs it into a file
    * @param outputPath
    * @param nodes
    */
  def generateAndSaveGlobalState(
      outputPath: String,
      nodes: List[ActorRef]
  ): Unit = {
    val yaml = nodes.zipWithIndex
      .map(node => {
        val snapshotText =
          Await.result(node._1 ? SnapshotRequest, timeout.duration).toString
        val json = io.circe.jawn.parse(snapshotText).valueOr(throw _)
        val yaml = json.asYaml.spaces2

        s"""${node._2}:
           |${yaml}
           |""".stripMargin
      })
      .reduce((acc: String, curr: String) => s"""${acc}
             |${curr}""".stripMargin)

    system.log.debug(yaml)

    val file = new File(
      s"${outputPath}/output-${System.currentTimeMillis / 1000}.yaml"
    )
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(yaml)
    bw.close()
  }

  /**
    * Given a string, computes the MD5 hash and returns its Int representation
    * @param value
    * @return
    */
  def hashString(value: String) = {
    // Source: https://stackoverflow.com/questions/5992778/computing-the-md5-hash-of-a-string-in-scala
    val hash = MessageDigest
      .getInstance("MD5")
      .digest(value.getBytes())
      .map("%02X".format(_))
      .reduce((acc: String, curr: String) => acc + curr)
    Integer.parseUnsignedInt(hash.substring(0, 4), 16)
  }*/
}
