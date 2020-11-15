//import chord.Node.Node
import java.util.{HashMap, Optional}

import actors.ChordNodeActor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import messages._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem

object Homework3 extends App {
  println("Homework 3 placeholder")
  // TODO: move to config
  val m = 3
  implicit val timeout = Timeout(10.seconds)

  val system: ActorSystem = ActorSystem("TestSystem")

  val node0 = system.actorOf(Props[ChordNodeActor], "Node0")
  var initRequest = node0 ? InitSelfRequest(0, m, Optional.ofNullable(null))
  var initResponse = Await.result(initRequest, timeout.duration)
  system.log.info(initResponse.toString)

  val node1 = system.actorOf(Props[ChordNodeActor], "Node1")
  initRequest = node1 ? InitSelfRequest(1, m, Optional.of(node0))
  initResponse = Await.result(initRequest, timeout.duration)
  system.log.info(initResponse.toString)

  val node2 = system.actorOf(Props[ChordNodeActor], "Node2")
  initRequest = node2 ? InitSelfRequest(3, m, Optional.of(node0))
  initResponse = Await.result(initRequest, timeout.duration)
  system.log.info(initResponse.toString)

  var snapshotRequest = node0 ? SnapshotRequest
  var snapshotResponse = Await.result(snapshotRequest, timeout.duration)
  system.log.info(snapshotResponse.toString)

  snapshotRequest = node1 ? SnapshotRequest
  snapshotResponse = Await.result(snapshotRequest, timeout.duration)
  system.log.info(snapshotResponse.toString)

  snapshotRequest = node2 ? SnapshotRequest
  snapshotResponse = Await.result(snapshotRequest, timeout.duration)
  system.log.info(snapshotResponse.toString)

  // Shutdown simulation
  system.terminate()
}
