package chord.FingerTable

import java.util.Optional

import akka.actor.ActorRef
import org.slf4j.LoggerFactory

/**
  * Represents one entry in a {@Link Node}'s {@Link FingerTable}
  *
  * @param startId
  * @param interval
  * @param node
  */
class Finger(
    var startId: Int,
    var interval: (Int, Int),
    private var node: (Int, ActorRef)
) {
  private val Logger = LoggerFactory.getLogger(this.getClass());

  Logger.debug(
    s"""
    Creating new entry for node with params:
      (startId: ${startId}, interval: [${interval._1}, ${interval._2}), successor: ${node}
    """
  )

  def setNode(newNode: (Int, ActorRef)): Unit = this.node = newNode
  def getNode(): Optional[(Int, ActorRef)] = Optional.ofNullable(this.node)

  /**
    * Returns true if the `nodeId` is between the start and end of the interval `[start, end)`.
    *
    * @param nodeId
    * @return
    */
  def isNodeInInterval(nodeId: Int): Boolean =
    (nodeId >= interval._1 && nodeId < interval._2)
}
