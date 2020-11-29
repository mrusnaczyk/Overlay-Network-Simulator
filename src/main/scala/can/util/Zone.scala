package can.util

/**
  * Represents a portion of a space that is being overseen by a node.
  * @param zoneRange The range of the zone. One `DimensionRange` object per dimension.
  * @param d Number of dimensions in the space of the simulation.
  */
class Zone (zoneRange: List[DimensionRange], d: Int) {
  // Check that the dimension ranges that were passed match the number of dimensions in the simulation
  assert(zoneRange.length == d)
}
