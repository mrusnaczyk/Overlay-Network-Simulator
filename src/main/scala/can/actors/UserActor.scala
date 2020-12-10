package can.actors

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import data.Movie
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object UserActions {
  case class ReadMovie(movieTitle: Int)
  case class WriteMovie(hashedMovieTitle: Int, movie: Movie)
}

class UserActor extends Actor {
  import UserActions._

  private val LOGGER = LoggerFactory.getLogger(this.getClass);
  private val APPLICATION_CONFIG = ConfigFactory.load("application.conf")
  private implicit val timeout = Timeout(2.seconds) // TODO: hardcoded
  private val http = Http(context.system)
  private implicit val executionContext = context.system.dispatcher
  private implicit val materializer = ActorMaterializer()

  override def receive: Receive = {
    case ReadMovie(movieTitle) => {
      // TODO: PUT IN CONFIG
      val requestPath = s"http://localhost:8080/movie?title=$movieTitle"
      val request = http
        .singleRequest(HttpRequest(uri = requestPath))
        .map {
          case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
            Await.result(Unmarshal(response.entity).to[String], timeout.duration)
          case _ =>
            "error"
        }

      sender ! Await.result(request, timeout.duration).toString
    }
    case WriteMovie(hashedMovieTitle, movie) => {
      // TODO: PUT IN CONFIG
      LOGGER.info(s"[${self.path.name}] WRITE_MOVIE")
      val requestPath = s"http://localhost:8080/movie?hashedId=$hashedMovieTitle&title=${movie.title}&year=${movie.year}&revenue=${movie.revenue}"
      val request = http
        .singleRequest(HttpRequest(HttpMethods.POST, requestPath))
        .map {
          case response @ HttpResponse(StatusCodes.Created, _, _, _) =>
            response.entity.toString
          case _ =>
            "error"
        }

      sender ! Await.result(request, timeout.duration)
    }
  }
}
