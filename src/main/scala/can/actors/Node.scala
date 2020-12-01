package can.actors

import java.util.Optional

import akka.actor.{Actor, ActorRef}
import can.messages.{HeartbeatCommand, InitNodeCommand, JoinCommand, TakeoverCommand}
import can.util.{NeighborStore, Zone}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

class Node extends Actor {
  private val LOGGER = LoggerFactory.getLogger(this.getClass);

  private val neighbors = new NeighborStore
  private val zones: ArrayBuffer[Zone] = ArrayBuffer()

  override def receive: Receive = {
    case InitNodeCommand(bootstrapNode) => handleInitNodeCommand(bootstrapNode)
    case HeartbeatCommand => handleHeartbeatCommand(sender)
    case JoinCommand => handleJoinCommand()
    case TakeoverCommand => handleTakeoverCommand()
  }

  // Akka message handling

  private def handleHeartbeatCommand(from: ActorRef): Unit = {
    LOGGER.info(s"[${self.toString}] Received Heartbeat")
  }

  private def handleJoinCommand(): Unit = {
    LOGGER.info(s"[${self.toString}] Received Join")
  }

  private def handleTakeoverCommand(): Unit = {
    LOGGER.info(s"[${self.toString}] Received Takeover")
  }

  private def handleInitNodeCommand(value: Optional[Node]): Unit = {
    LOGGER.info(s"[${self.toString}] Received Init Node")
  }

}
