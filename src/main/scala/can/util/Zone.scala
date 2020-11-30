package can.util

/**
  * Represents a portion of a space that is being overseen by a node.
  * @param zoneRange The range of the zone. One `DimensionRange` object per dimension.
  * @param d Number of dimensions in the space of the simulation.
  */
class Zone (zoneRange: List[DimensionRange], d: Int) {
  // Check that the dimension ranges that were passed match the number of dimensions in the simulation
  assert(zoneRange.length == d)

  /**
    * Checks if a point is within the zone
    * @param point
    * @return
    */
  def isPointInZone(point: List[Int]): Boolean = {
    // Assert that the point we are given is d-dimensional
    assert(point.length == d)

    // Check if the the point is in range for all dimensions.
    point
        .zip(zoneRange)
        .forall(entry => {
          val pointOnDimension = entry._1
          val dimensionRange = entry._2

          dimensionRange.isPointInRange(pointOnDimension)
        })
  }
}
