package data

import org.scalatest.funsuite.AnyFunSuite

class MovieTest extends AnyFunSuite{
  test("toString() returns correct format for Movie") {
    val movie = new Movie("Random Horror Movie", 2020, 29.99)
    val expectedFormattedString =
      s"(title='${movie.title}', year=${movie.year}, revenue=${movie.revenue})"

    assert(movie.toString().equals(expectedFormattedString))
  }
}
