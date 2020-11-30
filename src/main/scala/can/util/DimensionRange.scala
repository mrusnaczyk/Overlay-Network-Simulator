package can.util

/**
  * Represents a range in a single dimension, as [from, to)
  * @param from start of the range, inclusive,
  * @param to end of the range, exclusive
  */
class DimensionRange(var from: Int, var to: Int) {

  /**
    * Checks if a point is within the range [from, to).
    * @param point Value to check if in range.
    * @return True if `point` is in range, false otherwise.
    */
  def isPointInRange(point: Int): Boolean = (point >= from && point < to)
}
