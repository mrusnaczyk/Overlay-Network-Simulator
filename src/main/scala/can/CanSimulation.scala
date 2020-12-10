package can

import java.util.Optional

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import can.actors.{ApiServer, NodeActor}
import can.messages._
import can.util.DimensionRange
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class CanSimulation {
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
  }
}
