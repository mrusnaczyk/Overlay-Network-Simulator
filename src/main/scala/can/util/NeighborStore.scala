package can.util

import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

class NeighborStore {
  private var neighbors: ListBuffer[ActorRef] = ListBuffer()

  /**
    * Adds a neighbor to the `NeighborStore`.
    * @param neighbor `ActorRef` of the new neighbor to add.
    */
  def addNeighbor(neighbor: ActorRef): Unit = {
    neighbors.addOne(neighbor);
  }

  /**
    * Removes a neighbor from the `NeighborStore`.
    * @param neighbor `ActorRef` of the neighbor to remove.
    */
  def removeNeighbor(neighbor: ActorRef): Unit = {
    neighbors = neighbors.filter(n => !n.equals(neighbor));
  }
}
