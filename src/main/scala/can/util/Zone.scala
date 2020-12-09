package can.util

import akka.actor.ActorRef
import org.slf4j.LoggerFactory

/**
  * Represents a portion of a space that is being overseen by a node.
 *
  * @param zoneRange The range of the zone. One `DimensionRange` object per dimension.
  * @param d Number of dimensions in the space of the simulation.
  */
class Zone (var zoneRange: List[DimensionRange], d: Int) {
  private val LOGGER = LoggerFactory.getLogger(this.getClass)

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

  def split(newZoneNode: ActorRef): Zone = {
    // Find widest dimension and split along that dimension
    val widestDimension: DimensionRange = zoneRange.maxBy(dimension => dimension.length())
    LOGGER.debug(s"Splitting along ${widestDimension}")

    // Construct new split zone
    val zoneBoundariesForNewZone = zoneRange.map(dimension => {
      if(dimension.equals(widestDimension))
        new DimensionRange(widestDimension.midpoint(), widestDimension.to)
      else
        dimension
    })

    // Construct new zone range to replace the one in this `Zone` instance
    val zoneRangeForThis = zoneRange.map(dimension => {
      if(dimension.equals(widestDimension))
        new DimensionRange(widestDimension.from, widestDimension.midpoint())
      else
        dimension
    })

    // Replace this zone's boundaries with the new one
    this.zoneRange = zoneRangeForThis

    // Return newly constructed zone for the new node
    new Zone(zoneBoundariesForNewZone, d)
  }

  def isNeighborOf(otherZone: Zone): Boolean = {
    val thisZoneRange = this.zoneRange
    val otherZoneRange = otherZone.zoneRange

    val numMatchingDimensions = thisZoneRange
        .zip(otherZoneRange).zipWithIndex
        .count(item => {
          val index = item._2
          val dimensionRangePair = item._1
          val thisDimensionRange = dimensionRangePair._1
          val otherDimensionRange = dimensionRangePair._2

          LOGGER.debug(s"Comparing dimension ${index}: \n\t${thisDimensionRange}\n\t ${otherDimensionRange}")

          thisDimensionRange.isWithinRange(otherDimensionRange) || otherDimensionRange.isWithinRange(thisDimensionRange)
        })

    val mismatchedDimension = this.zoneRange.zip(otherZoneRange)
      .find(pair => !pair._1.isWithinRange(pair._2) && !pair._2.isWithinRange(pair._1)/*!pair._1.equals(pair._2)*/).get

    LOGGER.debug(s"mismatchedDimension $mismatchedDimension | matchDim $numMatchingDimensions | ${mismatchedDimension._1.abutsOtherRange(mismatchedDimension._2)}")

    if(numMatchingDimensions == d - 1 && mismatchedDimension._1.abutsOtherRange(mismatchedDimension._2))
      true
    else
      false
  }

  def distanceToMidpoint(point: List[Int]): Double = {
    // midpoint of the zone
    val midpoint: List[Int] = this.zoneRange.map(dimension =>
      (dimension.from + dimension.to) / 2
    )

    LOGGER.debug(s"Calculating distance from point $point to zone midpoint $midpoint")

    Math.sqrt(
      midpoint.zip(point)
        .map(dimension => Math.pow(dimension._1 - dimension._2, 2))
        .reduce((acc, curr) => acc + curr)
    )
  }

  override def toString: String =
    s"Zone (d = ${d}, zoneRange = ${zoneRange.toString})"

}
