package stats

import java.io.File

import scala.concurrent.duration._
import java.util.concurrent.ThreadLocalRandom

import SKDB.SimpleClusterListener
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RelativeActorPath
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus

object StatsSample {
  def main(args: Array[String]): Unit = {

    //    val systemConfig = ConfigFactory.parseFile(new File("stats1.conf"))
    //    val config = ConfigFactory.load(systemConfig)

    val config =
      ConfigFactory.parseString(s"akka.remote.netty.tcp.port=" + 2561).withFallback(
        ConfigFactory.parseString("akka.cluster.roles = [compute]")).
        withFallback(ConfigFactory.load("stats1"))

    val actorSystem = ActorSystem.create("ClusterSystem", config)
    //actorSystem.actorOf(Props[StatsWorker], name = "statsWorker")
    actorSystem.actorOf(Props[StatsService], name = "statsService")
  }

  //    if (args.isEmpty) {
  //      startup(Seq("2561", "2562", "0"))
  //      //StatsSampleClient.main(Array.empty)
  //    } else {
  //      startup(args)
  //    }
  //  }
  //
  //  def startup(ports: Seq[String]): Unit = {
  //    ports foreach { port =>
  //      // Override the configuration of the port when specified as program argument
  //      val config =
  //        ConfigFactory.parseString(s"akka.remote.netty.tcp.port=" + port).withFallback(
  //          ConfigFactory.parseString("akka.cluster.roles = [compute]")).
  //          withFallback(ConfigFactory.load("stats1"))
  //
  //      val system = ActorSystem("ClusterSystem", config)
  //
  //      val worker = system.actorOf(Props[StatsWorker], name = "statsWorker")
  //      val service = system.actorOf(Props[StatsService], name = "statsService")
  //
  //      println(s"worker path="+worker.path)
  //      println(s"service path="+service.path)
  //    }
  //  }
}
