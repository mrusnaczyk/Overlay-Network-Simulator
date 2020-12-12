package can.actors

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import can.messages.{ReadMovieCommand, WriteMovieCommand}
import com.typesafe.config.ConfigFactory
import com.typesafe.sslconfig.util.ConfigLoader
import data.Movie
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}


class ApiServer(system: ActorSystem) {
  val LOGGER = LoggerFactory.getLogger(this.getClass)
  val applicationConfig = ConfigFactory.load()
  implicit val serverSystem: ActorSystem = system

  def startServer() = {
    implicit val timeout = Timeout(applicationConfig.getInt("cs441.OverlayNetwork.defaultTimeout").seconds)
    val nodes = List(system.actorSelection("akka://cs441_cluster@seed:1600/user/Node0"))

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
      .newServerAt(
        applicationConfig.getString("cs441.OverlayNetwork.api.host"),
        applicationConfig.getInt("cs441.OverlayNetwork.api.port")
      )
      .bind(route)
  }
}
