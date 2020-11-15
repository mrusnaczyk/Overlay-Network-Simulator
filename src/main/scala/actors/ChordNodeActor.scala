package actors

import java.util.Optional

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import chord.FingerTable.{Finger, FingerTable}
import messages._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ChordNodeActor extends Actor {
  // TODO: move to config
  implicit val timeout = Timeout(2.seconds)

  // id of -1 indicates inactive node
  private var nodeId: Int = -1;
  private var m: Int = -123456;
  private var fingerTable: FingerTable = null;
  private var predecessor: (Int, ActorRef) = null

  override def receive: Receive = {
    case SnapshotRequest => {
      context.system.log.info("SnapshotRequest")
      sender ! generateSnapshot()
    }

    // Instructs the node to initialize its internal state, including finger table
    case InitSelfRequest(nodeId, m, refNode) => {
      context.system.log.info("InitSelfRequest")
      initSelf(nodeId, m)
      join(refNode)
      sender ! true
    }

    case GetOwnPredecessorRequest => {
      context.system.log.info("GetOwnPredecessorRequest")
      sender ! this.predecessor
    }

    case SetOwnPredecessorRequest(newPredecessor) => {
      context.system.log.info("SetOwnPredecessorRequest")
      this.predecessor = newPredecessor
      sender ! true
    }

    case GetOwnSuccessorRequest => {
      context.system.log.info(s"GetOwnSuccessorRequest in node ${this.nodeId} (from: ${sender.toString()} | to: ${self.toString()})")
      sender ! this.fingerTable.finger(0).getNode().get()
    }

    // Performs a lookup to find the node actor that is responsible for the nodeId
    case LookupRequest(lookupTargetNodeId) => {
      context.system.log.info("LookupRequest")
      sender ! 1000
    }

    case FindSuccessorRequest(nodeId) => {
      context.system.log.info("FindSuccessorRequest")
      sender ! findSuccessor(nodeId)
    }

    case FindPredecessorRequest(nodeId) => {
      context.system.log.info("FindPredecessorRequest")
      sender ! findPredecessor(nodeId)
    }

    case GetClosestPrecedingFingerRequest(targetNodeId) => {
      context.system.log.info("GetClosestPrecedingFingerRequest")
      sender ! closestPrecedingFinger(targetNodeId)
    }

    case UpdateFingerTableRequest(s, i) => {
      context.system.log.info("UpdateFingerTableRequest")
      updateFingerTable(s, i)
    }

    // case ReadFileRequest()
    // case WritefileRequest()
  }

  /**
    * Generates a snapshot of the node's state
    * @return String representation of the node's state
    */
  private def generateSnapshot(): String = {
    val fingerTableSerialized =
      (0 until m)
        .map(fingerIndex => this.fingerTable.finger(fingerIndex))
        .map((finger: Finger) => {
          val fingerNodeExists = finger.getNode().isPresent
          s"fingerTableEntry = (${finger.startId}, ${finger.interval.toString}, ${if(fingerNodeExists) finger.getNode.get.toString else "N/A"})"
        })

    val nodeStateString = s"(id = ${this.nodeId}, m = ${this.m}, pred = ${predecessor.toString()}, fingerTable(0) = ${fingerTableSerialized})"

    nodeStateString
  }


  /**
    * Initializes a new node and creates a finger table for it.
    * @param nodeId - the ID to identify the node by
    * @param m - size of the finger table
    */
  private def initSelf(nodeId: Int, m: Int): Unit = {
    this.nodeId = nodeId
    this.m = m
    this.fingerTable = new FingerTable(nodeId, m)
  }

  /**
    * Attempts to find the node that is responsible for the given `nodeId`.
    * If founds, it returns it. Otherwise, returns null
    * @param targetNodeId ID of the node to look for
    * @return
    */
  private def lookupNode(targetNodeId: Int): Optional[(Int, ActorRef)] = {
    if(targetNodeId == this.nodeId) {
      return Optional.ofNullable((this.nodeId, self))
    }

    for(fingerIndex <- 0 until m) {
      val finger: Finger = fingerTable.finger(fingerIndex)

      if(finger.isNodeInInterval(targetNodeId))
        return finger.getNode()
    }

    Optional.ofNullable(null)
  }

  private def join(optionalRefNode: Optional[ActorRef]): Unit = {
    context.system.log.info("Joining DHT ring...")

    if(optionalRefNode.isPresent) {
      initFingerTable(optionalRefNode.get)
      context.system.log.info(generateSnapshot())
      updateOthers()
    }
    else {
      for (i <- 0 until m)
        fingerTable.updateFingerSuccessor(i, (this.nodeId, self))
      this.predecessor = (this.nodeId, self)
    }
    context.system.log.info(generateSnapshot())
  }

  private def initFingerTable(refNode: ActorRef): Unit = {
    context.system.log.info("Initiating finger table...")
    context.system.log.info(Await.result(refNode ? SnapshotRequest, 2.seconds).toString)

    // Get and set successor this node
    val firstFinger = fingerTable.finger(0)
    val successorRequest = refNode ? FindSuccessorRequest(firstFinger.startId)
    val successor = Await.result(successorRequest, 5.seconds).asInstanceOf[(Int, ActorRef)]

    context.system.log.info(successor.toString())
    context.system.log.info("aftersuccessor")

    fingerTable.updateFingerSuccessor(0, successor)

    // Get and set predecessor of this node
    val predecessorRequest = successor._2 ? GetOwnPredecessorRequest
    predecessor = Await.result(predecessorRequest, 5.seconds).asInstanceOf[(Int, ActorRef)]

    //successor.predecessor = this
    Await.result(
      successor._2 ? SetOwnPredecessorRequest((this.nodeId, self)),
      2.seconds
    )

    for (i <- 1 to m - 1) {
      val finger = fingerTable.finger(i)
      val prevFinger = fingerTable.finger(i-1)
      val prevFingerNodeId = prevFinger.getNode().get._1

      if (isInRange(finger.startId, this.nodeId, prevFingerNodeId, true, false)) {
        context.system.log.info("In range, updating...")
        fingerTable.updateFingerSuccessor(i, prevFinger.getNode().get())
      }
      else {
        context.system.log.info("Not in range")
        fingerTable.updateFingerSuccessor(i,
          Await.result(
            refNode ? FindSuccessorRequest(finger.startId),
            3.seconds
          ).asInstanceOf[(Int, ActorRef)]
        )
      }
    }
  }

  private def findSuccessor(targetNodeId: Int): (Int, ActorRef) = {
    context.system.log.info(s"findSuccessor${targetNodeId}")

    val nPrime = findPredecessor(targetNodeId)
    context.system.log.info(s"${self}")
    context.system.log.info(s"nprime in findSuccessor = ${nPrime.toString()}")
    context.system.log.info(s"${nPrime.equals(self)}")
//    context.system.log.info(Await.result(nPrime._2 ? SnapshotRequest, 2.seconds).toString)

    // TODO: fix?
    context.system.log.info(self.toString())
    if(nPrime._1.equals(nodeId))
      (nodeId, self)
    else
      Await.result(nPrime._2 ? GetOwnSuccessorRequest, 1.seconds)
           .asInstanceOf[(Int, ActorRef)]
  }

  private def findPredecessor(nodeId: Int): (Int, ActorRef) = {
    context.system.log.info(s"${this.nodeId}.findPredecessor(${nodeId})")

    if(nodeId == this.nodeId)
      return this.predecessor

    val correctedNodeId = {
      if(nodeId < 0)
        nodeId + Math.pow(2, m).toInt
      else
        nodeId
    }

    var nPrime = (this.nodeId, self)
    context.system.log.info(s"nodeId: ${correctedNodeId}, nPrimeId: ${nPrime._1}")

    var nPrimeSuccessor = {
      if (this.fingerTable.finger(0).getNode().get()._1 == nPrime._1)
        (
          this.fingerTable.finger(0).getNode().get()._1 + Math.pow(2, m).toInt,
          this.fingerTable.finger(0).getNode().get()._2
        )
      else
        this.fingerTable.finger(0).getNode().get()
    }
      /* *{
        if(nPrimeSuccessor.equals(self))
          correctedNodeId
        else
          Await.result(nPrimeSuccessor ? GetOwnIdRequest, 2.seconds).asInstanceOf[Int]
      }*/

    context.system.log.info("checkrange")
    while(!isInRange(correctedNodeId, nPrime._1, nPrimeSuccessor._1, false, true)) {
      context.system.log.info("notInRange | targetNodeId: ${nodeId}")
      context.system.log.info(generateSnapshot())


      if(nPrime._1 == this.nodeId) {
        nPrime = closestPrecedingFinger(correctedNodeId)
        nPrimeSuccessor = this.fingerTable.finger(0).getNode().get()
      } else {
        nPrime = Await.result(nPrime._2 ? GetClosestPrecedingFingerRequest(correctedNodeId), 2.seconds)
          .asInstanceOf[(Int, ActorRef)]
        nPrimeSuccessor = Await.result(nPrime._2 ? GetOwnSuccessorRequest, 2.seconds)
          .asInstanceOf[(Int, ActorRef)]
      }
    }
    context.system.log.info("done looking range")

    nPrime
  }

  private def closestPrecedingFinger(targetNodeId: Int): (Int, ActorRef) = {
    context.system.log.info(s"ClosestPrecedingFinger ${m}, ${targetNodeId}")

    for(i <- 1 to m) {
      // finger[i].node
      val fingerNode = fingerTable.finger(m - i).getNode().get()
      context.system.log.info(fingerNode.toString())

      // finger[i].node in (this, targetNodeId)
      val fingerNodeIsInRange = isInRange(
        fingerNode._1, nodeId, targetNodeId,
        false, false
      )

      if(fingerNodeIsInRange){
        return fingerNode
      }
    }

    context.system.log.info("done closest")
    (this.nodeId, self)
  }

  private def updateOthers(): Unit = {
    context.system.log.info(s"UpdateOthers in node ${this.nodeId}")
    for (i <- 0 to m-1) {
      val p = findPredecessor(nodeId - Math.pow(2, i).toInt + 1)
//      val p = lookupNode(nodeId - Math.pow(2, i).toInt).get
      context.system.log.info(s"in Node ${self}; Node ${p.toString()}")

      if(p._2.equals(self)) {
        context.system.log.info("updateOthers on self")
        updateFingerTable((this.nodeId,self), i)
      }
      else {
        p._2 ! UpdateFingerTableRequest((this.nodeId, self), i)
      }
    }
  }

  private def updateFingerTable(s: (Int, ActorRef), i: Int): Boolean = {
    context.system.log.info(s"UpdateFingerTable finger ${i} in node ${nodeId} with node ${s._1}")

    context.system.log.info(s._1.toString)

    val finger = fingerTable.finger(i).getNode().get()
    val fingerNodeId = {
      if(this.nodeId == finger._1)
        finger._1 + Math.pow(2, m).toInt
      else
        finger._1
    }

    if(s._1 == this.nodeId)
      return true

    if(isInRange(s._1, this.nodeId, fingerNodeId, true, false)) {
      fingerTable.updateFingerSuccessor(i, s)
      context.system.log.info("Updating finger")
      context.system.log.info(predecessor.toString())
      context.system.log.info(self.toString())

      predecessor._2 ! UpdateFingerTableRequest(s, i)
    }

    true
  }

  private def isInRange(
      value: Int,
      lowerBound: Int,
      upperBound: Int,
      includeLowerBound: Boolean,
      includeUpperBound: Boolean
  ): Boolean = {

    if(upperBound < lowerBound) {
      return isInRange(value, lowerBound, Math.pow(2, m).toInt - 1, includeLowerBound, true) ||
             isInRange(value, 0, upperBound % Math.pow(2, m).toInt, true, includeUpperBound)
    }

    val conformsToLowerBound: Boolean =
      (includeLowerBound && value >= lowerBound) ||
        (!includeLowerBound && value > lowerBound)

    val upperBoundFixed = /*if(upperBound < lowerBound) upperBound + Math.pow(2, m).toInt else */upperBound

    val conformsToUpperBound: Boolean =
      (includeUpperBound && value <= upperBoundFixed) ||
        (!includeUpperBound && value < upperBoundFixed)

    context.system.log.info(s"${value} in ${if(includeLowerBound) "[" else "("}${lowerBound},${upperBoundFixed}${if(includeUpperBound) "]" else ")"} (${value} in ${if(includeLowerBound) "[" else "("}${lowerBound},${upperBound}${if(includeUpperBound) "]" else ")"})")

    conformsToLowerBound && conformsToUpperBound
  }
}
