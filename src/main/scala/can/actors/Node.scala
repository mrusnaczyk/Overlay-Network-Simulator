package can.actors

import akka.actor.{Actor, ActorRef}
import can.messages.{HeartbeatCommand, JoinCommand, TakeoverCommand}
import org.slf4j.LoggerFactory

class Node extends Actor {
  private val LOGGER = LoggerFactory.getLogger(this.getClass);

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
