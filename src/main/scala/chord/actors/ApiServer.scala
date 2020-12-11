package chord.actors

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import can.messages.WriteMovieCommand
import chord.messages.ReadMovieRequest
import data.Movie
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


class ApiServer(system: ActorSystem) {
  val LOGGER = LoggerFactory.getLogger(this.getClass)
  implicit val serverSystem: ActorSystem = system

  def startServer() = {
    // TODO: make both config
    implicit val timeout = Timeout(3.seconds)
    val nodes = List(system.actorSelection("akka://cs441_cluster@seed:1600/user/Node0"))

    val getMovieAction = get {
      parameter("title".as[Int]) { key =>
      { // TODO: make movie title String and convert to int here
        LOGGER.info(s"GET /movie?title=$key")
        try {
          val result = Await
            .result(nodes(0) ? ReadMovieRequest(key), timeout.duration)
            .asInstanceOf[Movie]
          complete(StatusCode.int2StatusCode(200), result.toString())
        } catch {
          case e: Exception => {
            LOGGER.error(e.toString)
            complete(StatusCode.int2StatusCode(400), "Not Found")
          }
        }
      }
      }
    }

    val createMovieAction = post {
      parameter("hashedId".as[Int], "title".as[String], "year".as[Int], "revenue".as[Double]) {
        (hashedId, title, year, revenue) => {
          LOGGER.info(s"POST /movie?hashedId=$hashedId&title=$title&year=$year&revenue=$revenue")
          val movie = new Movie(title, year, revenue)
          try {
            nodes(0) ? WriteMovieCommand(hashedId, movie)
            complete(
              StatusCode.int2StatusCode(201),
              s"Created $hashedId => $movie"
            )
          } catch {
            case e: Exception =>
              complete(
                StatusCode.int2StatusCode(500),
                e.toString
              )
          }
        }
      }
    }

    val route = {
      path("movie") {
        concat(getMovieAction, createMovieAction)
      }
    }

    // Start server
    Http()
      .newServerAt("0.0.0.0", 8080) // TODO: move to config
      .bind(route)
  }
}
