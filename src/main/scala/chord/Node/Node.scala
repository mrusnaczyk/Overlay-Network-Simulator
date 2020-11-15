package chord.Node
//
import java.util
//
//import chord.FingerTable.Finger
//import org.slf4j.LoggerFactory
//
class Node(val id: Int, val m: Int, var nodeMap: util.HashMap[Int, Node]) {
//  private val Logger = LoggerFactory.getLogger(this.getClass);
//
//  // Finger table
//  var finger: List[Finger] = (0 to m - 1).toList
//    .map(k => {
//      val start = id + Math.pow(2, k).toInt
//      val end = (start + Math.pow(2, k).toInt) % Math.pow(2, m).toInt
//      new Finger(start, (start, end), null)
//    })
//    .toList
//
//  // Node's predecessor node
//  var predecessor: Node = this;
//
//  // Node's successor node, AKA the successor of the first finger
//  def successor() = finger(0).getNode()
//
//  def logSnapshot() = {
//    val fingerSnapshot = finger.map(finger =>
//      s"""
//      ${finger.startId} | [${finger.interval._1}, ${finger.interval._2}) | ${finger.node.id}"""
//    )
//    Logger.info(
//      s"""
//      Node ${id}
//      Pred ${predecessor.id} | Succ ${successor.get().id}
//      ${fingerSnapshot}
//      """
//    )
//  }
//
//  def findSuccessor(id: Int): Node = {
//    var id2 = id;
//    Logger.info(s"\t${this.id}.findSuccessor(id=${id})");
//    if(id2 < 0) {
//      id2 = Math.pow(2, m).toInt + id
//      Logger.info(s"\tCorrected to ${id2}")
//    }
//
////    val successor = finger.find(f => isInRange(id2, f.interval._1, f.interval._2, true, false)).toList(0).node
////    successor
//     val nPrime = findPredecessor(id)
//     nPrime.successor
//  }
//
//  def findPredecessor(id: Int): Node = {
//    var id2 = id;
//    Logger.info(s"${this.id}.findPredecessor(id=${id})");
//
//    if(id2 < 0) {
//      id2 = Math.pow(2, m).toInt + id
//      Logger.info(s"\tCorrected to ${id2}")
//    }
//
//     var nPrime = this
//
//    // if(nPrime.predecessor.id == nPrime.successor.id && nPrime.id == nPrime.successor.id)
//      // return nPrime
//    // if(isFirst)
//    // val isOnlyNode = (this.id == this.predecessor.id) && (this.id == this.successor.id)
//    // var successorId = if (isOnlyNode) this.successor.id + Math.pow(2, m).toInt else this.successor.id
//
//     while (!isInRange(id, nPrime.id, nPrime.successor.id, false, true)) {
//       nPrime = nPrime.closestPrecedingFinger(id)
//     }
//
//    // finger.find(f => f.isNodeInInterval(id)).toList(0).node.predecessor
//     nPrime
////    findSuccessor(id2).predecessor
//  }
//
//  def closestPrecedingFinger(id: Int): Node = {
//    for (i <- m - 1 to 0)
//      if (isInRange(finger(i).node.id, this.id, id, false, false))
//        finger(i).node
//
//    this
//  }
//
//  def join(existingNode: Node = null): Unit = {
//    Logger.info(s"New node ${id} join, using ref node ${if(existingNode == null) "null" else existingNode.id}")
//    Logger.info(s"Putting new node ${id} into nodeMap")
//
//    if (existingNode != null) {
//      Logger.info(s"Existing node ${existingNode.id} found")
//      initFingerTable(existingNode)
//      updateOthers()
//    } else {
//      for (i <- 0 to m - 1)
//        finger(i).node = this
//      predecessor = this
//    }
//  }
//
//  def initFingerTable(existingNode: Node): Unit = {
//    Logger.info(s"Setting successor of new node ${id}")
//    finger(0).node = existingNode.findSuccessor(finger(0).startId)
//    predecessor = successor.predecessor
//
//    successor.predecessor = this
//
//    for (i <- 1 to m - 1) {
//      if (
//        isInRange(finger(i).startId, this.id, finger(i - 1).node.id, true, false)
//      ) {
//        finger(i).node = finger(i - 1).node
//      }
//      else
//        finger(i).node = existingNode.findSuccessor(finger(i).startId)
//    }
//  }
//
//  def updateOthers(): Unit = {
//    for (i <- 0 to m - 1) {
//      val p = findPredecessor(id - Math.pow(2, i).toInt)
//      p.updateFingerTable(this, i)
//    }
//  }
//
//  def updateFingerTable(s: Node, i: Int): Unit = {
//    Logger.info("updating finger table")
//    if(isInRange(s.id, this.id, finger(i).node.id, true, false)) {
//      finger(i).node = s
////      val p = nodeMap.get(predecessor)
//      predecessor.updateFingerTable(s, i)
//    }
//  }
//
//  private def isInRange(
//      value: Int,
//      lowerBound: Int,
//      upperBound: Int,
//      includeLowerBound: Boolean,
//      includeUpperBound: Boolean
//  ): Boolean = {
//
//    if(upperBound < lowerBound) {
//      return isInRange(value, lowerBound, Math.pow(2, m).toInt - 1, includeLowerBound, true) ||
//             isInRange(value, 0, upperBound % Math.pow(2, m).toInt, true, includeUpperBound)
//    }
//
//
//    val conformsToLowerBound: Boolean =
//      (includeLowerBound && value >= lowerBound) ||
//        (!includeLowerBound && value > lowerBound)
//
//    val upperBoundFixed = /*if(upperBound < lowerBound) upperBound + Math.pow(2, m).toInt else */upperBound
//
//    val conformsToUpperBound: Boolean =
//      (includeUpperBound && value <= upperBoundFixed) ||
//        (!includeUpperBound && value < upperBoundFixed)
//
//    Logger.info(s"${if(includeLowerBound) "[" else "("}${lowerBound},${upperBoundFixed}${if(includeUpperBound) "]" else ")"}  ")
//
//    conformsToLowerBound && conformsToUpperBound
//  }
}
