package can.actors

import akka.actor.{Actor, ActorRef}
import can.messages.{HeartbeatCommand, JoinCommand, TakeoverCommand}
import can.util.{NeighborStore, Zone}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

class Node extends Actor {
  private val LOGGER = LoggerFactory.getLogger(this.getClass);

  private val neighbors = new NeighborStore
  private val zones: ArrayBuffer[Zone] = ArrayBuffer()

  override def receive: Receive = {
    case HeartbeatCommand => handleHeartbeatCommand(sender)
    case JoinCommand => handleJoinCommand()
    case TakeoverCommand => handleTakeover()
  }

  private def handleHeartbeatCommand(from: ActorRef): Unit = {
    LOGGER.info(s"[${self.toString}] Received Heartbeat")
  }

  private def handleJoinCommand(): Unit = {
    LOGGER.info(s"[${self.toString}] Received Join")
  }

  private def handleTakeover(): Unit = {
    LOGGER.info(s"[${self.toString}] Received Takeover")
  }
}
