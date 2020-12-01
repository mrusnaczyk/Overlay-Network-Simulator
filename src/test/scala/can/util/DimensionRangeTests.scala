package can.util

import org.scalatest.funsuite.AnyFunSuite

class DimensionRangeTests extends AnyFunSuite{
  test("isPointInRange returns true when point is within the range") {
    val range = new DimensionRange(10, 30)
    val point = 15

    assert(range.isPointInRange(point))
  }

  test("isPointInRange returns true when point is exactly the lower end of the range") {
    val range = new DimensionRange(10, 30)
    val point = 10

    assert(range.isPointInRange(point))
  }

  test("isPointInRange returns false when point is exactly the upper end of the range") {
    val range = new DimensionRange(10, 30)
    val point = 30

    assert(!range.isPointInRange(point))
  }
}
