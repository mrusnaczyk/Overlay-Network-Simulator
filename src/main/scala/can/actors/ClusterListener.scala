package can.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent._
import akka.cluster.typed.Cluster
import akka.cluster.typed.Subscribe

// Adapted from Akka cluster example project:
// https://github.com/akka/akka-samples/blob/2.6/akka-sample-cluster-scala/src/main/scala/sample/cluster/simple/ClusterListener.scala
object ClusterListener {
  def apply(): Behavior[ClusterEvent.ClusterDomainEvent] =
    Behaviors.setup { context =>
      Cluster(context.system).subscriptions ! Subscribe(context.self, classOf[ClusterEvent.ClusterDomainEvent])

      Behaviors.receiveMessagePartial {
        case MemberUp(member) =>
          context.log.debug("Member is Up: {}", member.address)
          Behaviors.same
        case UnreachableMember(member) =>
          context.log.debug("Member detected as unreachable: {}", member)
          Behaviors.same
        case MemberRemoved(member, previousStatus) =>
          context.log.debug("Member is Removed: {} after {}",
            member.address, previousStatus)
          Behaviors.same
        case LeaderChanged(member) =>
          context.log.info("Leader changed: " + member)
          Behaviors.same
        case any: MemberEvent =>
          context.log.info("Member Event: " + any.toString)
          Behaviors.same
      }
    }
}