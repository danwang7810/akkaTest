package SKDB

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, ClusterEvent}
import akka.event.Logging
import org.jboss.netty.logging.Log4JLoggerFactory

class SimpleClusterListener extends Actor with ActorLogging {

  //val log = Logging.getLogger(getContext.system, this)
  val cluster = Cluster.get(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState =>
      log.info("Current members: {}", state.members.mkString(", "))
    case MemberUp(member) =>
      log.info("！！！！！！！！！！！！！！ Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case LeaderChanged(member) =>
      log.info("?????????????????????????? leader change {}", member.get)
    case _: MemberEvent => // ignore
  }

}

object testCluster {
  def main(args:Array[String]):Unit = {
    val actorSystem = ActorSystem.create("ClusterSystem")
    val SimpleClusterListenerActor = actorSystem.actorOf(Props[SimpleClusterListener], name = "clusterListener")



  }
}
