package messages

import java.util.Optional
import akka.actor.ActorRef

case class FindSuccessorRequest(nodeId: Int, originatingNodeId: Int, originatingNodeSuccessor: Optional[(Int, ActorRef)])
