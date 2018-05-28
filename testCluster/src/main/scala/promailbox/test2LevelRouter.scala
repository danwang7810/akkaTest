package promailbox

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration.Duration
import scala.util.Failure

class Router1LevelActor extends Actor{

  def createRouter:ActorRef ={
    val router = context.actorOf(RoundRobinPool(1).props(Props[Router2LevelActor]), "routerLevel1")
    router
  }

  val router1 = createRouter
  var orgActor:ActorRef = null

  override def receive = {
    case m:String if(! m.equals("routerLevel2 RSP")) =>
      orgActor = sender
      router1 ! m + " level1"
    case "routerLevel2 RSP" =>
      println(s"routerLevel1 sender =" + orgActor.path)
      orgActor ! "routerLevel1 RSP"
  }
}

class Router2LevelActor extends Actor{

  def createRouter():ActorRef ={
    val router = context.actorOf(RoundRobinPool(5).props(Props[Work]), "routerLevel2")
    router
  }

  val router1 = createRouter
  var orgActor:ActorRef = null

  override def receive = {

    case m:String if(! m.equals("work RSP")) =>
      orgActor = sender
      router1 ! m + " level2"
    case "work RSP"  =>
      println(s"receive workRsp")
      println(s"routerLevel2 sender =" + orgActor.path)
      orgActor ! "routerLevel2 RSP"
  }
}

class Work extends Actor{
  var orgActor:ActorRef = null

  override def receive = {
    case m:String => {
      orgActor = sender
      println(s"work sender path=" + sender.path)
      println(s"$m")
      sender ! "work RSP"
    }
  }
}

object test2LevelRouter {
  def main(args:Array[String]) = {
    val system = ActorSystem.create("test2LevelRouter")

    val testActor = system.actorOf(Props[Router1LevelActor], "orgsender")


    import system.dispatcher

    implicit val timeout = new Timeout(Duration.create(1, "seconds"))
    val fut = testActor ? "hello"
    fut.onComplete{
      case failed:Failure[akka.pattern.AskTimeoutException] => println(s"failed=" + failed)
      case result => println(s"$result")
    }

  }
}