package data

/**
  * Holds the data for one movie.
  * @param title
  * @param year
  * @param revenue
  */
class Movie(title: String, year: Int, revenue: Double) {
  /**
    * Returns the serialized Movie data in a easy-to-read format
    * @return
    */
  override def toString(): String = {
   return s"(title='${title}', year=${year}, revenue=${revenue})"
  }
}
