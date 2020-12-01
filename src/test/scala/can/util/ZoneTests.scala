package can.util

import org.scalatest.funsuite.AnyFunSuite

class ZoneTests extends AnyFunSuite{
  test("isPointInZone returns true when point is within the range") {
    val numDimensions = 4
    val zone = new Zone(
      List(
        new DimensionRange(10, 30),
        new DimensionRange(0, 100),
        new DimensionRange(5, 20),
        new DimensionRange(500, 1000)
      ),
      numDimensions
    )
    val point = List(15, 3, 8, 700)

    assert(zone.isPointInZone(point))
  }
}
