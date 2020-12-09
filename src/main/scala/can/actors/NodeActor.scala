package can.actors

import java.util.Optional

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import can.messages._
import can.util.{DimensionRange, Neighbor, Neighborhood, Zone}
import com.typesafe.config.ConfigFactory
import data.Movie
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class NodeActor extends Actor {

  private val LOGGER = LoggerFactory.getLogger(this.getClass);
  private val APPLICATION_CONFIG = ConfigFactory.load("application.conf")
  private implicit val timeout = Timeout(2.seconds)

  // Number of dimensions
  private var id: Int = -1
  private val d = APPLICATION_CONFIG.getInt("cs441.OverlayNetwork.can.d")
  private var neighborhoods: ArrayBuffer[Neighborhood] = ArrayBuffer()
  private val movies: mutable.HashMap[List[Int], Movie] = new mutable.HashMap[List[Int], Movie]();

  override def receive: Receive = {
    case InitNodeCommand(id, bootstrapNode, maxRange) => {
      handleInitNodeCommand(id, bootstrapNode, maxRange)
      sender ! true
    }
    case SendHeartbeatCommand => handleSendHeartbeatCommand()
    case HeartbeatCommand     => handleHeartbeatCommand(sender)
    case JoinCommand          => sender ! handleJoinCommand(sender)
    case TakeoverCommand      => handleTakeoverCommand()
    case SnapshotCommand      => sender ! generateSnapshot()
    case NeighborUpdateCommand(newZones) =>
      handleNeighborUpdateCommand(sender, newZones)
    case RemoveNeighborCommand => handleRemoveNeighborCommand(sender)
    case ReadMovieCommand(hashedMovieTitle) =>
      sender ! handleReadMovieCommand(sender, hashedMovieTitle)
    case WriteMovieCommand(hashedMovieTitle, movie) =>
      handleWriteMovieCommand(hashedMovieTitle, movie)
  }

  // Akka message handling

  private def handleSendHeartbeatCommand() = {
    this.neighborhoods.foreach(neighborhood =>
      neighborhood
        .getNeighbors()
        .foreach(neighbor => {
          // Send HEARTBEAT command to indicate this node is alive
          neighbor.getNode ! HeartbeatCommand

          // If no HEARTBEAT has been received from the other node in a set period of time, send a TAKEOVER

        })
    )
  }

  private def handleHeartbeatCommand(from: ActorRef): Unit = {
    LOGGER.info(s"[${self.path.name}] HEARTBEAT from ${from.path.name}")
  }

  private def handleJoinCommand(sender: ActorRef): Neighborhood = {
    LOGGER.info(s"[${self.path.name}] JOIN from ${sender.path.name}")
    // If we have more than one neighborhood, give one to the new node and don't split
    if (neighborhoods.length > 1) {
      LOGGER.info(
        s"[${self.path.name}] Node has more than zone; giving away zones[1]"
      )
      neighborhoods.remove(
        1
      ) // TODO: fix neighborhoods to be the correct NeighborStore
    } else {
      LOGGER.info(
        s"[${self.path.name}] Node has only one zone; splitting zones[0]"
      )
      val thisNodeZones =
        neighborhoods.toList.map(neighborhood => neighborhood.getZone)
      val splitResult =
        neighborhoods(0).splitNeighborhood(sender, self, thisNodeZones)
      val newNeighborhood = splitResult._1
      val nodesToUpdate = splitResult._2

      nodesToUpdate.foreach(neighbor =>
        neighbor.getNode ! RemoveNeighborCommand
      )

      LOGGER.debug(s"[${self.path.name}] New neighborhood: ${newNeighborhood}")
      newNeighborhood
    }
  }

  private def handleTakeoverCommand(): Unit = {
    LOGGER.info(s"[${self.path.name}] TAKEOVER")
  }

  private def handleInitNodeCommand(
      id: Int,
      bootstrapNode: Optional[ActorRef],
      maxRange: List[DimensionRange]
  ) = {
    LOGGER.info(s"[${self.path.name}] INIT_NODE")

    this.id = id

    // If no bootstrap node is given, then it's assumed that the current node is the first/only node.
    if (!bootstrapNode.isPresent) {
      LOGGER.info("No bootstrap node detected; setting up as first/only node")

      neighborhoods.addOne(
        new Neighborhood(
          List(),
          new Zone(maxRange, d)
        )
      )
    }
    // Contact the bootstrap node to get neighborhoods and zones
    else {
      LOGGER.info(s"[${this.id}] Not first node")

      // Send request to bootstrap node to receive initial zone and neighborhood
      val neighborhood = Await
        .result(bootstrapNode.get ? JoinCommand, 2.seconds)
        .asInstanceOf[Neighborhood]

      LOGGER.info(neighborhood.toString)

      neighborhoods.addOne(neighborhood)

      // Collect list of zones this node is responsible for
      val zones = neighborhoods.toList.map(neighborhood => neighborhood.getZone)

      // Announce to other neighbors that we have joined
      neighborhoods.toList
        .foreach(neighborhood =>
          neighborhood
            .getNeighbors()
            .foreach(neighbor => sendNeighborUpdateCommand(neighbor, zones))
        )
    }
  }

  /** Updates the zones that the sender is responsible for in our neighbor records
    * @param sender
    * @param newZones
    */
  private def handleNeighborUpdateCommand(
      sender: ActorRef,
      newZones: List[Zone]
  ) = {
    LOGGER.info(
      s"[${self.path.name}] NEIGHBOR_UPDATE from ${sender.path.name} for zone ${newZones}"
    )

    /* For each neighborhood, try to find a neighbor which is the sender.
     * Wherever found, update the list of zones.*/
    this.neighborhoods.foreach(neighborhood => {
      newZones.foreach(zone => {
        // If a neighborhood is supposed to contain the zone, update/add it
        if (neighborhood.getZone().isNeighborOf(zone)) {
          val neighborhoodContainsSender = neighborhood
            .getNeighbors()
            .exists(neighbor => neighbor.getNode.equals(sender))

          // If the neighborhood contains the neighbor, update
          if (neighborhoodContainsSender) {
            neighborhood
              .getNeighbors()
              .foreach(neighbor =>
                if (neighbor.getNode.equals(sender))
                  neighbor.patchZones(newZones)
              )
          }
          // Otherwise add it as a new neighbor
          else {
            neighborhood.addNeighbor(
              new Neighbor(sender, newZones)
            )
          }
        }
      })
    })
  }

  private def handleRemoveNeighborCommand(sender: ActorRef) = {
    LOGGER.info(s"[${self.path.name}] REMOVE_NEIGHBOR ${sender.path.name}")
    this.neighborhoods.foreach(neighborhood =>
      neighborhood.removeNeighbor(sender)
    )
  }

  private def handleReadMovieCommand(sender: ActorRef, hashedMovieTitle: Int) = {
    LOGGER.info(
      s"[${self.path.address}] READ_MOVIE_REQUEST from ${sender.path.address} for hashed title ${hashedMovieTitle}"
    )
    val movieTitlePoint = hashedMovieTitle.toString
      .split("")
      .grouped(hashedMovieTitle.toString.length / d)
      .toList
      .map(grouping =>
        grouping
          .reduce((acc, curr) => acc + curr)
          .toInt
      )

    LOGGER.debug(
      s"[${self.path.name}] Movie title after conversion to point: $movieTitlePoint"
    )

    val responsibleNeighborhoodInThisNode =
      neighborhoods.filter(neighborhood =>
        neighborhood.getZone().isPointInZone(movieTitlePoint)
      )

    LOGGER.debug(
      s"[${self.path.name}] Filtered zones: ${responsibleNeighborhoodInThisNode}"
    )

    if (responsibleNeighborhoodInThisNode.length > 0)
      this.movies.get(movieTitlePoint).get
//      new Movie(s"Inception (From node $id)", 2012, 12.1)
    else {
      val neighborsWithDistance = neighborhoods.toList
        // Collect list of all neighbors across all neighborhoods
        .flatMap(neighborhood => neighborhood.getNeighbors)
        // From that list, get list of all zones across all neighborhoods
        .flatMap(neighbor => neighbor.getZones.map(zone => (neighbor, zone)))
        .map(neighboringZone =>
          (
            neighboringZone._1,
            neighboringZone._2.distanceToMidpoint(movieTitlePoint)
          )
        )

      LOGGER.info(neighborsWithDistance.toString)

      val closestNeighbor = neighborsWithDistance
        .minBy(neighborWithDistance => neighborWithDistance._2)
        ._1

      LOGGER.info(s"Closest neighbor: $closestNeighbor")

      Await
        .result(
          closestNeighbor.getNode ? ReadMovieCommand(hashedMovieTitle),
          timeout.duration
        )
        .asInstanceOf[Movie]
    }
  }

  private def handleWriteMovieCommand(hashedMovieTitle: Int, movie: Movie) = {
    LOGGER.info(
      s"[${self.path.name}] WRITE_MOVIE_REQUEST from ${sender.path.name} for hashed title ${hashedMovieTitle}: $movie"
    )
    val movieTitlePoint = hashedMovieTitle.toString
      .split("")
      .grouped(hashedMovieTitle.toString.length / d)
      .toList
      .map(grouping =>
        grouping
          .reduce((acc, curr) => acc + curr)
          .toInt
      )

    LOGGER.debug(
      s"[${self.path.name}] Movie title after conversion to point: $movieTitlePoint"
    )

    val responsibleNeighborhoodInThisNode =
      neighborhoods.filter(neighborhood =>
        neighborhood.getZone().isPointInZone(movieTitlePoint)
      )

    LOGGER.debug(
      s"[${self.path.name}] Filtered zones: ${responsibleNeighborhoodInThisNode}"
    )

    if (responsibleNeighborhoodInThisNode.length > 0) {
      this.movies.put(movieTitlePoint, movie)
      LOGGER.info(this.movies.toList.toString)
    } else {
      val neighborsWithDistance = neighborhoods.toList
        // Collect list of all neighbors across all neighborhoods
        .flatMap(neighborhood => neighborhood.getNeighbors)
        // From that list, get list of all zones across all neighborhoods
        .flatMap(neighbor => neighbor.getZones.map(zone => (neighbor, zone)))
        .map(neighboringZone =>
          (
            neighboringZone._1,
            neighboringZone._2.distanceToMidpoint(movieTitlePoint)
          )
        )

      LOGGER.info(neighborsWithDistance.toString)

      val closestNeighbor = neighborsWithDistance
        .minBy(neighborWithDistance => neighborWithDistance._2)
        ._1

      LOGGER.info(s"Closest neighbor: $closestNeighbor")

      closestNeighbor.getNode ! WriteMovieCommand(hashedMovieTitle, movie)
    }
  }

  def generateSnapshot() = {
    s"""
      | id = ${id}
      | d = ${d}
      | neighborhoods = ${neighborhoods}
      |""".stripMargin
  }

  /** Send a message to `neighbor` saying that this node is now responsible for the zones in `newZones`
    * @param neighbor - Neighbor to send announcement to
    * @param newZones - List of zones that this node is now responsible for
    */
  private def sendNeighborUpdateCommand(
      neighbor: Neighbor,
      newZones: List[Zone]
  ) = {
    if (self.equals(neighbor.getNode))
      LOGGER.warn(
        "Trying to send NeighborUpdateCommand to self? Something is wrong..."
      )
    else
      neighbor.getNode ! NeighborUpdateCommand(newZones)
  }
}
