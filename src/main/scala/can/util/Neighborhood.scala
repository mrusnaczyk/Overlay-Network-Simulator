package can.util

import akka.actor.ActorRef
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationInt

class Neighborhood(initialNeighbors: List[Neighbor], initialZone: Zone) extends Serializable{
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
  def removeNeighbor(neighbor: ActorRef): Unit = {
    neighbors = neighbors.filter(n => !n.getNode.equals(neighbor));
  }

  def getNeighborsOfZone(zone: Zone): List[Neighbor] = {
    neighbors.toList
      .filter(potentialNeighbor => {
        val neighborZones = potentialNeighbor.getZones
        neighborZones.exists(neighborZone => neighborZone.isNeighborOf(zone))
      })
  }

  def updateZone(newZone: Zone) = this.zone = newZone

  def splitNeighborhood(newNeighborhoodNode: ActorRef, thisNode: ActorRef, thisNodeZones: List[Zone]): (Neighborhood, List[Neighbor]) = {
    val thisNodeAsNeighbor = new Neighbor(thisNode, thisNodeZones);

    // Zone for new neighborhood
    val newZone = zone.split(newNeighborhoodNode);
    // List of neighbors for new neighborhood
    val newNeighbors = {
      // If this neighborhood has neighbors, pick out the ones that will be the neighbors of the new neighborhood
      if (neighbors.length > 0)
        thisNodeAsNeighbor +: getNeighborsOfZone(newZone)
      // Otherwise, the only neighbor of the new neighborhood is the current zone's node
      else
        List(thisNodeAsNeighbor)
    }

    // This neighborhood's zone is now split, so recalculate neighbors
    val oldNeighbors = neighbors.toList
    val thisNodeNeighbors = getNeighborsOfZone(zone)
    this.neighbors.clear();
    this.neighbors.addAll(thisNodeNeighbors)

    LOGGER.info(oldNeighbors.toString)
    LOGGER.info(thisNodeNeighbors.toString)

    // Return new node's neighborhood, as well as neighbors from this neighborhood that need to update neighbors
    (new Neighborhood(newNeighbors, newZone), oldNeighbors.diff(thisNodeNeighbors))
  }

  override def toString: String = {
    s"""
       | zone = $zone
       | neighbors = ${neighbors.toList.map(n => "\n\t\t" + n.toString)}
       |""".stripMargin
  }
}
