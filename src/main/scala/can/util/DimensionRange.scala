package can.util

import org.slf4j.LoggerFactory

/**
  * Represents a range in a single dimension, as [from, to)
  *
  * @param from start of the range, inclusive,
  * @param to end of the range, exclusive
  */
class DimensionRange(var from: Int, var to: Int) extends Serializable{
  private val LOGGER = LoggerFactory.getLogger(this.getClass)

  /**
    * Checks if a point is within the range [from, to).
    * @param point Value to check if in range.
    * @return True if `point` is in range, false otherwise.
    */
  def isPointInRange(point: Int): Boolean = {
    LOGGER.debug(s"is point $point in range ($from, $to)")
    (point >= from && point < to)
  }

  def abutsOtherRange(other: DimensionRange) = {
    LOGGER.debug(s"$to == ${other.from} || ${other.to} == $from")
    if (this.to == other.from || other.to == this.from)
      true
    else
      false
  }

  /**
    * Tests if another DimensionRange is within the range of this one.
    * @param otherRange
    * @return
    */
  def isWithinRange(otherRange: DimensionRange): Boolean = {
    LOGGER.debug(s" $from >= ${otherRange.from} && $to <= ${otherRange.to}")
    if(this.from >= otherRange.from && this.to <= otherRange.to)
      true
    else
      false
  }

  /**
    * Returns the length of the dimension.
    */
  def length(): Int = Math.abs(to - from)

  /**
    * Returns the midpoint value of the dimension.
    */
  def midpoint(): Int = (to + from) / 2

  override def toString: String =
    s"DimensionRange(from = ${from}, to = ${to})"
}
