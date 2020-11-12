package chord.FingerTable

import org.slf4j.LoggerFactory

/**
  * Represents one entry in a {@Link Node}'s {@Link FingerTable}
  *
  * @param startId
  * @param interval
  * @param successor
  */
class Finger(
    private val startId: Int,
    private val interval: Tuple2[Int, Int],
    private val successor: Int,
    private val node: Node
) {
  private val Logger = LoggerFactory.getLogger(this.getClass());

  Logger.debug(
    """
    Creating new entry for Node %d with params:
      (startId: %d, interval: [%d,%d), successor: %d
    """
      .format(startId, interval._1, interval._2, successor)
  )

  def getStartId(): Int = this.startId;
  def getInterval(): Tuple2[Int, Int] = this.interval;
  def getSuccessor(): Int = this.successor;
  def getNode(): Node = this.node;

  /**
    * Returns true if the `nodeId` is between the start and end of the interval `[start, end)`.
    *
    * @param nodeId
    * @return
    */
  def isNodeInInterval(nodeId: Int): Boolean =
    (nodeId >= interval._1 && nodeId < interval._2)
}
