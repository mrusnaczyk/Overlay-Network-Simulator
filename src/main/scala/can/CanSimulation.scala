package can

import java.util.Optional

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import can.actors.{ApiServer, NodeActor, UserActions, UserActor}
import can.messages._
import can.util.DimensionRange
import com.typesafe.config.ConfigFactory
import data.Movie

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CanSimulation {
  val appConfig = ConfigFactory.load();
  val applicationConfig = ConfigFactory.load("application.conf")
  val snapshotBasePath = applicationConfig.getString("cs441.OverlayNetwork.snapshotBasePath")
  implicit val timeout: Timeout = Timeout(10.seconds)

  val system: ActorSystem = ActorSystem("ChordOverlayNetwork")
  val nodeIds = List(0, 2, 4, 6)

  //  val cluster = Cluster(system)
  //  AkkaManagement(system).start()
  def start(): Unit = {
    var nodes = nodeIds.map(
      nodeId => system.actorOf(Props[NodeActor], s"Node$nodeId")
    )

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
//        val initRequest = node ? InitNodeCommand(id, refNode, List(
//          new DimensionRange(0, 8),
//          new DimensionRange(0, 8)
//        ))
//        val initiatedNode = Await.result(initRequest, timeout.duration)
//        // TODO: move durations to config
//        //      system.scheduler.scheduleWithFixedDelay(
//        //        0.seconds, 250.milliseconds, node, SendHeartbeatCommand
//        //      )
//        //      cluster.join(node.path.address)
//
//        nodes.foreach(item =>
//          system.log.info(
//            Await.result(item ? SnapshotCommand, timeout.duration).toString
//          )
//        )
//
//        system.log.info(initiatedNode.toString)
//      })
    Thread.sleep(1000)

///////    val apiServer = new ApiServer(system, nodes)
//////    apiServer.startServer()

//    val userA = system.actorOf(Props[UserActor], s"User-A")
//
//    val movieA = new Movie("Inception1", 2012, 12.1111)
//    val movieB = new Movie("Inception2", 3301, 12.2222)
//
//    Await.result(userA ? UserActions.WriteMovie(11, movieA), timeout.duration)
//    Await.result(userA ? UserActions.WriteMovie(22, movieB), timeout.duration)
//
//    system.log.info(
//      Await.result(userA ? UserActions.ReadMovie(11), timeout.duration).toString
//    )
//
//    system.log.info(
//      Await.result(userA ? UserActions.ReadMovie(22), timeout.duration).toString
//    )

//    system.terminate()
  }
}
