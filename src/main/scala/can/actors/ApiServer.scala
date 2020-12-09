package can.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import can.messages.{ReadMovieCommand, WriteMovieCommand}
import data.Movie
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ApiServer(actorSystem: ActorSystem, nodes: List[ActorRef]) {
  val LOGGER = LoggerFactory.getLogger(this.getClass)
  implicit val serverSystem: ActorSystem = actorSystem

  def startServer() = {
    implicit val timeout = Timeout(1.seconds)

    val getMovieAction = get {
      parameter("title".as[Int]) { key =>
        { // TODO: make movie title String and convert to int here
          LOGGER.info(s"GET /movie?title=$key")
          try {
            val result = Await
              .result(nodes(0) ? ReadMovieCommand(key), timeout.duration)
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
      parameter(
        "hashedId".as[Int],
        "title".as[String],
        "year".as[Int],
        "revenue".as[Double]
      ) { (hashedId, title, year, revenue) =>
        {
          val movie = new Movie(title, year, revenue)
          nodes(0) ! WriteMovieCommand(hashedId, movie)
          complete(
            StatusCode.int2StatusCode(201),
            s"Created $hashedId => $movie"
          )
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
      .newServerAt("localhost", 8080) // TODO: move to config
      .bind(route)
  }
}
