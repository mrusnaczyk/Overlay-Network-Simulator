package can.messages

import java.util.Optional

import akka.actor.ActorRef
import can.util.DimensionRange

case class InitNodeCommand(
    id: Int,
    bootstrapNode: Optional[String],
    maxRange: List[DimensionRange]
)
