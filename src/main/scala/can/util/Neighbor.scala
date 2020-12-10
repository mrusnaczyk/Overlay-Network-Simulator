package can.util

import akka.actor.ActorRef

class Neighbor(private val node: ActorRef, private var zones: List[Zone]) extends Serializable{
  def getNode = this.node
  def getZones = this.zones;

  def patchZones(newZones: List[Zone]) = this.zones = newZones;

  override def toString: String =
    s"Neighbor(node = ${node.path.name}, zones = ${zones.toString}"
}
