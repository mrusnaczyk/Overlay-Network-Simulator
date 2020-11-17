package chord.FingerTable

import akka.actor.ActorRef

class FingerTable(nodeId: Int, m: Int) {
  private val fingers: List[Finger] = (0 until m).toList
    .map(k => {
      val moduloNum = Math.pow(2, m).toInt

      val start = (nodeId + Math.pow(2, k).toInt) % moduloNum
      val end = (start + Math.pow(2, k).toInt) % moduloNum
      new Finger(start, (start, end), null)
    })

  /**
    * Gets a finger with the given finger index
    * @param fingerNum
    * @return
    */
  def finger(fingerNum: Int): Finger =
      fingers(fingerNum)

  /**
    * Updates the successor node of a finger
    * @param fingerNum Index of the finger to update
    * @param newSuccessor
    */
  def updateFingerSuccessor(fingerNum: Int, newSuccessor: (Int, ActorRef)) =
      fingers(fingerNum).setNode(newSuccessor)
}
