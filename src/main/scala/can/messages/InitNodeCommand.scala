package can.messages

import java.util.Optional

import can.actors.Node

case class InitNodeCommand (bootstrapNode: Optional[Node])
