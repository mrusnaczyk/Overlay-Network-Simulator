package can.util

import akka.actor.ActorRef
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt

class Neighborhood(initialNeighbors: List[Neighbor], initialZone: Zone) {
  private val LOGGER = LoggerFactory.getLogger(this.getClass)
  // TODO: use config
  implicit val timeout = Timeout(2.seconds)
  private var neighbors: ArrayBuffer[Neighbor] = ArrayBuffer.from(initialNeighbors)
  private var zone: Zone = initialZone

  /**
    * Returns the zone
    */
  def getZone() = zone

  /**
    * Returns the list of `ActorRef` neighbors.
    */
  def getNeighbors() = neighbors.toList

  /**
    * Adds a neighbor to the `NeighborStore`.
    * @param neighbor `ActorRef` of the new neighbor to add.
    */
  def addNeighbor(neighbor: Neighbor): Unit = neighbors.addOne(neighbor)

  /**
    * Removes a neighbor from the `NeighborStore`.
    * @param neighbor `ActorRef` of the neighbor to remove.
    */
  def removeNeighbor(neighbor: Neighbor): Unit = {
    neighbors = neighbors.filter(n => !n.equals(neighbor));
  }

  def getNeighborsOfZone(zone: Zone): List[Neighbor] = {
//    LOGGER.info(neighbors.toString())
    neighbors.toList
      .filter(potentialNeighbor => {
        val neighborZones = potentialNeighbor.getZones
        neighborZones.exists(neighborZone => neighborZone.isNeighborOf(zone))
      })
  }

  def updateZone(newZone: Zone) = this.zone = newZone

  def splitNeighborhood(newNeighborhoodNode: ActorRef, thisNode: ActorRef, thisNodeZones: List[Zone]): Neighborhood = {
    val thisNodeAsNeighbor = new Neighbor(thisNode, thisNodeZones);
    val newZone = zone.split(newNeighborhoodNode);
    val newNeighbors = {
      // If this neighborhood has neighbors, pick out the ones that will be the neighbors of the new neighborhood
      if (neighbors.length > 0)
        thisNodeAsNeighbor +: getNeighborsOfZone(newZone)
      // Otherwise, the only neighbor of the new neighborhood is the current zone's node
      else
        List(thisNodeAsNeighbor)
    }

    new Neighborhood(newNeighbors, newZone)
  }

  override def toString: String = {
    s"""
       | zone = $zone
       | neighbors = ${neighbors.toList.map(n => "\n\t\t" + n.toString)}
       |""".stripMargin
  }
}
