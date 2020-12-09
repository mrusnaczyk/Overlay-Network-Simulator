package can.messages

import data.Movie

case class WriteMovieCommand (hashedMovieTitle: Int, movie: Movie)
