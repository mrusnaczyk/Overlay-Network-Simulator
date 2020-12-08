package can.messages

import can.util.Zone

case class NeighborUpdateCommand(newZones: List[Zone])
