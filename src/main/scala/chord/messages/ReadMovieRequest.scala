package chord.messages

/**
  * Represents a request to retrieve movie data.
  * @param hashedMovieTitle Integer representation of the hash of the movie title
  */
case class ReadMovieRequest (hashedMovieTitle: Int)
