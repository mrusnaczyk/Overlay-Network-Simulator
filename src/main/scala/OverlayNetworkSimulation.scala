import java.util.Optional

import akka.actor.typed.{ActorSystem => ActorSystemTyped}
import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import can.actors.{ApiServer, ClusterListener, NodeActor}
import can.messages.InitNodeCommand
import can.util.DimensionRange
import chord.ChordSimulation
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt

object OverlayNetworkSimulation extends App {
  val MODE = System.getenv("MODE")
  implicit val timeout = Timeout(2.seconds) // TODO: move to config
  println(s"Starting in mode $MODE")

  MODE match {
    case "CAN_USER_NODE"    => startCanUserNode()
    case "CAN_NODE"         => startCanNode()
    case "CHORD_NODE"       => startChordNode()
    case "CAN_SIMULATION"   => startCanSimulation()
    case "CHORD_SIMULATION" => startChordSimulation()
    case _                  => println(s"Error: mode $MODE does not match any valid modes")
  }

  def startCanUserNode() = {}

  def startCanNode() = {
    val system = ActorSystem("cs441_cancluster") // TODO: config
    val nodeId = System.getenv("NODE_ID").toInt
    val node = system.actorOf(Props[NodeActor], s"Node$nodeId")

    val bootstrapNode = {
      if(nodeId == 0)
        Optional.ofNullable(null)
      else
        Optional.of("akka://cs441_cancluster@seed:1600/user/Node0") // TODO: config
    }.asInstanceOf[Optional[String]]

    node ? InitNodeCommand(
      nodeId,
      bootstrapNode,
      List(
        new DimensionRange(0, 8), // TODO: config
        new DimensionRange(0, 8)
      )
    )
  }

  def startCanSimulation() = {
    val config = ConfigFactory.load()
    val clusterName = "cs441_cancluster" //TODO: config.getString("clustering.cluster.name")
    val system = ActorSystemTyped(ClusterListener(), clusterName)
    val cluster = Cluster.get(system)
    val apiServer = new ApiServer(system.classicSystem)
    apiServer.startServer()
  }

  def startChordUserNode() = {}

  def startChordNode() = {}

  def startChordSimulation() = {
    new ChordSimulation().start
  }

}
