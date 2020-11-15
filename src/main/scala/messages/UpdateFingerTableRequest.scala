package messages

import akka.actor.ActorRef

case class UpdateFingerTableRequest(s: (Int, ActorRef), i: Int)
