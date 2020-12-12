package chord

//import java.io.{BufferedWriter, File, FileWriter}
//import java.security.MessageDigest
//import java.util.Optional
//import java.util.stream.Collectors
//
//import akka.actor.{ActorRef, ActorSystem, Props}
//import akka.pattern.ask
//import akka.util.Timeout
//import chord.actors.ChordNodeActor
//import chord.messages._
//import com.typesafe.config.ConfigFactory
//import data.Movie
//
//import scala.concurrent.Await
//import scala.concurrent.duration.DurationInt
//import scala.jdk.CollectionConverters.ListHasAsScala
//import cats.syntax.either._
//import io.circe.yaml.syntax.AsYaml
//
class ChordSimulation {
  println("Running Chord simulation...")
//  val appConfig = ConfigFactory.load()
//  val applicationConfig = ConfigFactory.load("application.conf")
//  val system: ActorSystem = ActorSystem("ChordOverlayNetwork")
//  implicit val timeout: Timeout = Timeout(10.seconds)
//  val m = applicationConfig.getInt("cs441.OverlayNetwork.m")
//  val snapshotBasePath = applicationConfig.getString("cs441.OverlayNetwork.snapshotBasePath")
//
//  val nodeIds = applicationConfig.getIntList("cs441.OverlayNetwork.network.nodes").asScala.toList
//  var nodes: List[ActorRef] = List()
//
//  def start(): Unit = {
//    nodes = nodeIds.map(
//      id => system.actorOf(Props[ChordNodeActor], s"Node$id")
//    )
//
//    nodes
//      .zip(nodeIds)
//      .foreach(nodeWithId => {
//        val node = nodeWithId._1
//        val id = nodeWithId._2
//
//        val refNode = {
//          if (id == 0) Optional.ofNullable(null)
//          else Optional.of(nodes.head)
//        }.asInstanceOf[Optional[ActorRef]]
//
//        val initRequest = node ? InitSelfRequest(id, m, refNode)
//        val initiatedNode = Await.result(initRequest, timeout.duration)
//        system.log.info(initiatedNode.toString)
//      })
//
//    Thread.sleep(1000)
//
//    for (node <- nodes) {
//      val snapshot = Await.result(node ? SnapshotRequest, timeout.duration)
//      system.log.info(s"Snapshot: $snapshot")
//    }
//
//    val movies = applicationConfig
//      .getConfigList("cs441.OverlayNetwork.data").stream()
//      .map(movieObj => new Movie(
//        movieObj.getString("title"),
//        movieObj.getInt("year"),
//        movieObj.getDouble("revenue")
//      ))
//      .collect(Collectors.toList[Movie])
//      .asScala.toList
//
//    for (movie <- movies) {
//      val writeRequest: Unit = makeWriteRequest(movie)
//      system.log.info(writeRequest.toString)
//    }
//
//    system.log.info(s"\tResponse: ${makeReadRequest("Sample movie A").toString()}")
//    system.log.info(
//      s"\tResponse: ${makeReadRequest("Sample movie C").toString()}"
//    )
//
//    generateAndSaveGlobalState(snapshotBasePath, nodes)
//    // Shutdown simulation
//    system.terminate()
//  }
//
//  /** Attempts to find the movie with the given `movieName`. If found, the corresponding `Movie` object is returned.
//    * Otherwise, a special 'Not found' `Movie` object is returned.
//    * @param movieName The title of the movie to find
//    * @return `Movie`
//    */
//  def makeReadRequest(movieName: String) = {
//    val movieTitleHash = hashString(movieName)
//    val movieReadRequest =
//      Await.result(nodes.head ? ReadMovieRequest(movieTitleHash), 2.seconds)
//
//    val movieResult = movieReadRequest match {
//      case Some(m: Movie) => m
//      case None           => new Movie("Movie_Not_Found", 0, 0.0)
//    }
//
//    movieResult
//  }
//
//  /** Sends a request to the ring to save the given movie
//    * @param movie The `Movie` to save
//    */
//  def makeWriteRequest(movie: Movie): Unit = {
//    val movieTitleHash = hashString(movie.title)
//
//    Await.result(nodes.head ? WriteMovieRequest(movieTitleHash, movie), 2.seconds)
//  }
//
//  /**
//    * Generates the global state for all of the Chord nodes, and outputs it into a file
//    * @param outputPath
//    * @param nodes
//    */
//  def generateAndSaveGlobalState(
//                                  outputPath: String,
//                                  nodes: List[ActorRef]
//                                ): Unit = {
//    val yaml = nodes.zipWithIndex
//      .map(node => {
//        val snapshotText =
//          Await.result(node._1 ? SnapshotRequest, timeout.duration).toString
//        val json = io.circe.jawn.parse(snapshotText).valueOr(throw _)
//        val yaml = json.asYaml.spaces2
//
//        s"""${node._2}:
//           |${yaml}
//           |""".stripMargin
//      })
//      .reduce((acc: String, curr: String) => s"""${acc}
//                                                |${curr}""".stripMargin)
//
//    system.log.debug(yaml)
//
//    val file = new File(
//      s"${outputPath}/output-${System.currentTimeMillis / 1000}.yaml"
//    )
//    val bw = new BufferedWriter(new FileWriter(file))
//    bw.write(yaml)
//    bw.close()
//  }
//
//  /**
//    * Given a string, computes the MD5 hash and returns its Int representation
//    * @param value
//    * @return
//    */
//  def hashString(value: String) = {
//    // Source: https://stackoverflow.com/questions/5992778/computing-the-md5-hash-of-a-string-in-scala
//    val hash = MessageDigest
//      .getInstance("MD5")
//      .digest(value.getBytes())
//      .map("%02X".format(_))
//      .reduce((acc: String, curr: String) => acc + curr)
//    Integer.parseUnsignedInt(hash.substring(0, 4), 16)
//  }
}
