package services

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.contrib.pattern.{ClusterSingletonManager, ClusterSingletonProxy}

@Singleton
class CounterComponent @Inject() (actorSystem: ActorSystem) {

  // Initiates singleton in the Cluster
  actorSystem.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[Counter]),
    singletonName = "counter",
    role = None,
    terminationMessage = PoisonPill),
    name = "counter-singleton")

  // Initiates proxy for singleton in the current node
  val counter =
    actorSystem.actorOf(ClusterSingletonProxy.props(
      singletonPath = "/user/counter-singleton/counter",
      role = None),
      name = "counter-proxy")

}

object Counter {
  case object Count

  def props(): Props = {
    Props(classOf[Counter])
  }
}

class Counter extends Actor with ActorLogging {

  import Counter._

  def receive = handleMsg(0)

  def handleMsg(count: Int): Receive = {
    case Count => {
      val newCount = count + 1
      log.info("Msg received from {}, current count {}", sender(), newCount)
      sender() ! newCount
      context.become(handleMsg(newCount))
    }
  }

}

