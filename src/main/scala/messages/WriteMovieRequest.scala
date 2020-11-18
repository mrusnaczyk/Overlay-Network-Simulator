package messages

import data.Movie

case class WriteMovieRequest(hashedMovieTitle: Int, movie: Movie)
