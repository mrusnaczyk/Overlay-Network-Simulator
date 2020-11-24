package chord.messages

import java.util.Optional
import akka.actor.ActorRef

case class InitSelfRequest(nodeId: Int, m: Int, refNode: Optional[ActorRef])
