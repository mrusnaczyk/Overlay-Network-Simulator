package chord.messages

import scala.Option
import akka.actor.ActorRef

case class InitSelfRequest(nodeId: Int, m: Int, refNode: Option[String])
