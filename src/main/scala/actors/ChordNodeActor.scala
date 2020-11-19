package actors

import java.util.Optional

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import chord.FingerTable.{Finger, FingerTable}
import com.typesafe.config.ConfigFactory
import data.Movie
import messages._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ChordNodeActor extends Actor {
  val applicationConfig = ConfigFactory.load()
  implicit val timeout = Timeout(applicationConfig.getInt("cs441.OverlayNetwork.defaultTimeout").seconds)

  // id of -1 indicates inactive node
  private var nodeId: Int = -1;
  private var m: Int = -1;
  private var fingerTable: FingerTable = null
  private var predecessor: (Int, ActorRef) = null
  private var movies: mutable.HashMap[Int, Movie] =
    new mutable.HashMap[Int, Movie]();

  override def receive: Receive = {
    case SnapshotRequest => {
      context.system.log.info(s"[Node ${this.nodeId}] SnapshotRequest")
      sender ! generateSnapshot()
    }

    // Instructs the node to initialize its internal state, including finger table
    case InitSelfRequest(nodeId, m, refNode) => {
      context.system.log.info(s"[Node ${this.nodeId}] InitSelfRequest")
      initSelf(nodeId, m)
      join(refNode)
      sender ! true
    }

    case GetOwnPredecessorRequest => {
      context.system.log.info(s"[Node ${this.nodeId}] GetOwnPredecessorRequest")
      sender ! this.predecessor
    }

    case SetOwnPredecessorRequest(newPredecessor) => {
      context.system.log.info(s"[Node ${this.nodeId}] SetOwnPredecessorRequest")
      this.predecessor = newPredecessor
      sender ! true
    }

    case GetOwnSuccessorRequest => {
      context.system.log.info(s"[Node ${this.nodeId}] GetOwnSuccessorRequest")
      sender ! this.fingerTable.finger(0).getNode().get()
    }

    // Performs a lookup to find the node actor that is responsible for the nodeId
    case LookupRequest(lookupTargetNodeId) => {
      context.system.log.info(s"[Node ${this.nodeId}] LookupRequest")
      sender ! lookupNode(lookupTargetNodeId)
    }

    case FindSuccessorRequest(nodeId, originatingNodeId, originatingNodeSuccessor) => {
      context.system.log.info(s"[Node ${this.nodeId}] FindSuccessorRequest")
      sender ! findSuccessor(
        nodeId,
        originatingNodeId,
        originatingNodeSuccessor
      )
    }

    case FindPredecessorRequest(nodeId) => {
      context.system.log.info(s"[Node ${this.nodeId}] FindPredecessorRequest")
      sender ! findPredecessor(nodeId)
    }

    case GetClosestPrecedingFingerRequest(targetNodeId) => {
      context.system.log.info(s"[Node ${this.nodeId}] GetClosestPrecedingFingerRequest")
      sender ! closestPrecedingFinger(targetNodeId)
    }

    case UpdateFingerTableRequest(s, i) => {
      context.system.log.info(s"[Node ${this.nodeId}] UpdateFingerTableRequest")
      updateFingerTable(s, i)
    }

    case ReadMovieRequest(hashedMovieTitle) => {
      context.system.log.info(s"[Node ${this.nodeId}] ReadMovieRequest(hashedMovieId = ${hashedMovieTitle})");

      // Lookup the ID of the node that is responsible for the movie
      val ownerNode = lookupNode(hashedMovieTitle).get

      // Send a request to that node for the movie
      val movie = {
        if (ownerNode._1 == this.nodeId) {
          this.movies.get(hashedMovieTitle)
        } else {
          Await
            .result(
              ownerNode._2 ? ReadMovieRequest(hashedMovieTitle),
              timeout.duration
            )
        }
      }

      // Pass the returned movie back to the sender
      sender ! movie
    }

    case WriteMovieRequest(hashedMovieTitle, movie) => {
      context.system.log.info(s"[Node ${this.nodeId}] WriteMovieRequest(hashedMovieId = ${hashedMovieTitle})");

      // Lookup the ID of the node that is responsible for the movie
      val ownerNode = lookupNode(hashedMovieTitle)

      // Attempt to write the movie
      val movieWriteResult = {
        if (ownerNode.get._1 == this.nodeId) {
          this.movies.put(hashedMovieTitle, movie)
          true // Write was successful
        } else {
          // Attempt to write and pass back the result from the other node
          Await
            .result(
              ownerNode.get._2 ? WriteMovieRequest(hashedMovieTitle, movie),
              timeout.duration
            )
        }
      }

      // Pass the returned movie back to the sender
      sender ! movieWriteResult
    }
  }

  /** Generates a snapshot of the node's state
    * @return String representation of the node's state
    */
  private def generateSnapshot(): String = {
    val fingerTableSerialized =
      (0 until m)
        .map((fingerIndex: Int) => {
          val finger = this.fingerTable.finger(fingerIndex)
          val fingerNodeExists = finger.getNode().isPresent
          s"${fingerIndex}: 11"
          s""""(${finger.startId}, ${finger.interval.toString}, ${if (fingerNodeExists) finger.getNode.get.toString else "N/A"})""""
        })
        .reduce(
          (acc: String, curr: String) =>
            s"""${acc}, ${curr}""".stripMargin
        )

    val nodeStateString =
      s"""
        {
          "id": ${this.nodeId},
          "m": ${this.m},
          "pred": "${predecessor.toString()}",
          "fingerTable": [
            ${fingerTableSerialized}
          ]
        }
      """

    nodeStateString
  }

  /** Initializes a new node and creates a finger table for it.
    * @param nodeId - the ID to identify the node by
    * @param m - size of the finger table
    */
  private def initSelf(nodeId: Int, m: Int): Unit = {
    this.nodeId = nodeId
    this.m = m
    this.fingerTable = new FingerTable(nodeId, m)
  }

  /** Attempts to find the node that is responsible for the given `nodeId`.
    * If founds, it returns it. Otherwise, returns null
    * @param targetNodeId ID of the node to look for
    * @return
    */
  private def lookupNode(targetNodeId: Int): Optional[(Int, ActorRef)] = {
    // if targetNodeId is in range (pred.id, this.id], current node is what we're looking for
    if(isInRange(targetNodeId, this.predecessor._1, this.nodeId, false, true)) {
      return Optional.ofNullable((this.nodeId, self))
    }

    for (fingerIndex <- 0 until m) {
      val finger: Finger = fingerTable.finger(fingerIndex)

      if(isInRange(targetNodeId, finger.interval._1, finger.interval._2, true, false))
        return finger.getNode()
    }

    Optional.ofNullable(null)
  }

  /**
    * Joins a Chord ring, using `optionalRefNode` as a reference. If `optionalRefNode` is not a valid and active Node,
    * this node will be initiated as though it is the only/first node in the ring.
    * @param optionalRefNode The node to reference when obtaining info about the Chord ring.
    */
  private def join(optionalRefNode: Optional[ActorRef]): Unit = {
    context.system.log.info("Joining DHT ring...")

    if (optionalRefNode.isPresent) {
      initFingerTable(optionalRefNode.get)
      context.system.log.info(generateSnapshot())
      updateOthers()
    } else {
      for (i <- 0 until m)
        fingerTable.updateFingerSuccessor(i, (this.nodeId, self))
      this.predecessor = (this.nodeId, self)
    }
    context.system.log.info(generateSnapshot())
  }

  /**
    * Initializes the finger table, using `refNode` as a reference to obtain info about the other nodes in the ring.
    * @param refNode
    */
  private def initFingerTable(refNode: ActorRef): Unit = {
    context.system.log.info("Initiating finger table...")
    context.system.log.info(
      Await.result(refNode ? SnapshotRequest, timeout.duration).toString
    )

    // Get and set successor this node
    val firstFinger = fingerTable.finger(0)
    val successorRequest = refNode ? FindSuccessorRequest(
      firstFinger.startId,
      this.nodeId,
      this.fingerTable.finger(0).getNode()
    )
    val successor =
      Await.result(successorRequest, timeout.duration).asInstanceOf[(Int, ActorRef)]

    context.system.log.info(successor.toString())
    context.system.log.info("aftersuccessor")

    fingerTable.updateFingerSuccessor(0, successor)

    // Get and set predecessor of this node
    val predecessorRequest = successor._2 ? GetOwnPredecessorRequest
    predecessor =
      Await.result(predecessorRequest, timeout.duration).asInstanceOf[(Int, ActorRef)]

    //successor.predecessor = this
    Await.result(
      successor._2 ? SetOwnPredecessorRequest((this.nodeId, self)),
      timeout.duration
    )

    for (i <- 1 to m - 1) {
      val finger = fingerTable.finger(i)
      val prevFinger = fingerTable.finger(i - 1)
      val prevFingerNodeId = prevFinger.getNode().get._1

      if (
        isInRange(finger.startId, this.nodeId, prevFingerNodeId, true, false)
      ) {
        context.system.log.info("In range, updating...")
        fingerTable.updateFingerSuccessor(i, prevFinger.getNode().get())
      } else {
        context.system.log.info("Not in range")
        fingerTable.updateFingerSuccessor(
          i,
          Await
            .result(
              refNode ? FindSuccessorRequest(
                finger.startId,
                this.nodeId,
                this.fingerTable.finger(0).getNode()
              ),
              timeout.duration
            )
            .asInstanceOf[(Int, ActorRef)]
        )
      }
    }
  }

  /**
    * Finds the successor of a given `targetNodeId`, using the algorithm described in the Chord paper.
    * @param targetNodeId
    * @param originatingNode
    * @param originatingNodeSuccessor
    * @return
    */
  private def findSuccessor(
      targetNodeId: Int,
      originatingNode: Int,
      originatingNodeSuccessor: Optional[(Int, ActorRef)]
  ): (Int, ActorRef) = {
    context.system.log.info(s"findSuccessor${targetNodeId}")

    val nPrime = findPredecessor(targetNodeId)
    context.system.log.info(s"${self}")
    context.system.log.info(s"nprime in findSuccessor = ${nPrime.toString()}")
    context.system.log.info(s"${nPrime.equals(self)}")

    context.system.log.info(nPrime.toString())
    if (nPrime._1.equals(nodeId))
      this.fingerTable.finger(0).getNode.get
    else if (nPrime._1.equals(originatingNode))
      return originatingNodeSuccessor.get()
    else
      Await
        .result(nPrime._2 ? GetOwnSuccessorRequest, timeout.duration)
        .asInstanceOf[(Int, ActorRef)]
  }

  /**
    * Finds the successor of a given `nodeId`, using the algorithm described in the Chord paper.
    * @param nodeId
    * @return
    */
  private def findPredecessor(nodeId: Int): (Int, ActorRef) = {
    context.system.log.info(s"${this.nodeId}.findPredecessor(${nodeId})")

    if (nodeId == this.nodeId) {
      context.system.log.debug(s"called findPred on self, returning own predecessor: ${this.predecessor.toString()}")
      return this.predecessor
    }

    val correctedNodeId = {
      if (nodeId < 0)
        nodeId + Math.pow(2, m).toInt
      else
        nodeId
    }

    var nPrime = (this.nodeId, self)
    context.system.log.debug(s"nodeId: ${correctedNodeId}, nPrimeId: ${nPrime._1}")

    var nPrimeSuccessor = {
      if (this.fingerTable.finger(0).getNode().get()._1 == nPrime._1)
        (
          this.fingerTable.finger(0).getNode().get()._1 + Math.pow(2, m).toInt,
          this.fingerTable.finger(0).getNode().get()._2
        )
      else
        this.fingerTable.finger(0).getNode().get()
    }

    context.system.log.debug("checkrange")
    while (!isInRange(correctedNodeId, nPrime._1, nPrimeSuccessor._1, false, true)) {
      context.system.log.debug("notInRange | targetNodeId: ${nodeId}")
      context.system.log.debug(generateSnapshot())

      if (nPrime._1 == this.nodeId) {
        nPrime = closestPrecedingFinger(correctedNodeId)
        nPrimeSuccessor = this.fingerTable.finger(0).getNode().get()
      } else {
        nPrime = Await
          .result(
            nPrime._2 ? GetClosestPrecedingFingerRequest(correctedNodeId),
            timeout.duration
          )
          .asInstanceOf[(Int, ActorRef)]
        nPrimeSuccessor = Await
          .result(nPrime._2 ? GetOwnSuccessorRequest, timeout.duration)
          .asInstanceOf[(Int, ActorRef)]
      }
    }
    context.system.log.debug("done looking range")

    nPrime
  }

  /**
    * Finds the closest preceeding finger of a `targetNodeId`.
    * @param targetNodeId
    * @return
    */
  private def closestPrecedingFinger(targetNodeId: Int): (Int, ActorRef) = {
    context.system.log.info(s"ClosestPrecedingFinger ${m}, ${targetNodeId}")

    for (i <- 1 to m) {
      // finger[i].node
      val fingerNode = fingerTable.finger(m - i).getNode().get()
      context.system.log.info(fingerNode.toString())

      // finger[i].node in (this, targetNodeId)
      val fingerNodeIsInRange = isInRange(
        fingerNode._1,
        nodeId,
        targetNodeId,
        false,
        false
      )

      if (fingerNodeIsInRange) {
        return fingerNode
      }
    }

    context.system.log.debug("done closest")
    (this.nodeId, self)
  }

  /**
    * Attempts to update other nodes in the ring, whose fingers may be pointing at this node.
    */
  private def updateOthers(): Unit = {
    context.system.log.info(s"UpdateOthers in node ${this.nodeId}")
    for (i <- 0 to m - 1) {
      val p = findPredecessor(nodeId - Math.pow(2, i).toInt + 1)
      context.system.log.debug(s"in Node ${self}; Node ${p.toString()}")

      if (p._2.equals(self)) {
        context.system.log.debug("updateOthers on self")
        updateFingerTable((this.nodeId, self), i)
      } else {
        p._2 ! UpdateFingerTableRequest((this.nodeId, self), i)
      }
    }
  }

  /**
    * Update the finger table of this node, setting the successor of finger `i` to be node `s`.
    * Then, recursively update preceding nodes' fingers.
    * @param s New successor node.
    * @param i `i`th finger to update.
    * @return
    */
  private def updateFingerTable(s: (Int, ActorRef), i: Int): Boolean = {
    context.system.log.info(s"UpdateFingerTable finger ${i} in node ${nodeId} with node ${s._1}")
    context.system.log.debug(s._1.toString)

    val finger = fingerTable.finger(i).getNode().get()
    val fingerNodeId = {
      if (this.nodeId == finger._1)
        finger._1 + Math.pow(2, m).toInt
      else
        finger._1
    }

    if (s._1 == this.nodeId)
      return true

    if (isInRange(s._1, this.nodeId, fingerNodeId, true, false)) {
      fingerTable.updateFingerSuccessor(i, s)
      context.system.log.debug("Updating finger")
      context.system.log.debug(predecessor.toString())
      context.system.log.debug(self.toString())

      predecessor._2 ! UpdateFingerTableRequest(s, i)
    }

    true
  }

  /**
    * Checks if a int value is inside of a given range.
    * @param value The integer value to test
    * @param lowerBound Lower bound of the range
    * @param upperBound Upper bound of the range
    * @param includeLowerBound Whether to include the lower bound in the range.
    * @param includeUpperBound Whether to include the upper bound in the range.
    * @return
    */
  private def isInRange(
      value: Int,
      lowerBound: Int,
      upperBound: Int,
      includeLowerBound: Boolean,
      includeUpperBound: Boolean
  ): Boolean = {

    if (upperBound < lowerBound) {
      return isInRange(
        value,
        lowerBound,
        Math.pow(2, m).toInt - 1,
        includeLowerBound,
        true
      ) ||
        isInRange(
          value,
          0,
          upperBound % Math.pow(2, m).toInt,
          true,
          includeUpperBound
        )
    }

    val conformsToLowerBound: Boolean =
      (includeLowerBound && value >= lowerBound) ||
        (!includeLowerBound && value > lowerBound)

    val upperBoundFixed =
      /*if(upperBound < lowerBound) upperBound + Math.pow(2, m).toInt else */ upperBound

    val conformsToUpperBound: Boolean =
      (includeUpperBound && value <= upperBoundFixed) ||
        (!includeUpperBound && value < upperBoundFixed)

    context.system.log.debug(s"Check if ${value} in ${if (includeLowerBound) "["
    else "("}${lowerBound},${upperBoundFixed}${if (includeUpperBound) "]"
    else ")"} (${value} in ${if (includeLowerBound) "["
    else "("}${lowerBound},${upperBound}${if (includeUpperBound) "]" else ")"})")

    conformsToLowerBound && conformsToUpperBound
  }
}
