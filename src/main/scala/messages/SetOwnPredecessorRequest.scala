package messages

import akka.actor.ActorRef

case class SetOwnPredecessorRequest(newPredecessor: (Int, ActorRef))
