package chord.messages

import akka.actor.ActorRef

case class FindSuccessorRequest(
  nodeId: Int,
  originatingNodeId: Int,
  originatingNodeSuccessor: Option[(Int, ActorRef)]
) extends Serializable
