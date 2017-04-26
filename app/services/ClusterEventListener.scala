package services

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorLogging, ActorSystem, Address, Props}
import akka.cluster.Cluster

@Singleton
class ClusterEventListenerComponent @Inject()(actorSystem: ActorSystem) {
  val clusterListener = actorSystem.actorOf(ClusterEventListener.props())
}

object ClusterEventListener {
  case object GetClusterNodes

  def props(): Props = {
    Props(classOf[ClusterEventListener])
  }
}

class ClusterEventListener extends Actor with ActorLogging {
  import akka.cluster.ClusterEvent._
  import ClusterEventListener._

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  def receive = handleMsg(Set.empty[Address])

  def handleMsg(nodes: Set[Address]): Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      context.become(handleMsg(nodes + member.address))

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
      context.become(handleMsg(nodes - member.address))

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)

    case _: MemberEvent  => // ignore

    case GetClusterNodes => sender() ! nodes
  }


}