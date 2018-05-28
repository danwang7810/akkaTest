package promailbox


import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config



// We inherit, in this case, from UnboundedStablePriorityMailbox
// and seed it with the priority generator
//class MyPrioMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedStablePriorityMailbox(
//    // Create a new PriorityGenerator, lower prio means more important
//    PriorityGenerator {
//      // 'highpriority messages should be treated first if possible
//      case highpriority => 0
//
//      // 'lowpriority messages should be treated last if possible
//      case lowpriority  => 2
//
//      // PoisonPill when no other left
//      case PoisonPill   => 3
//
//      // We default to 1, which is in between high and low
//      case other    => 1
//    }
//)

// We create a new Actor that just prints out what it processes
class Logger extends Actor {
  //val log: LoggingAdapter = Logging(context.system, this)

  self ! lowpriority
  self ! lowpriority
  self ! highpriority(1)
  self ! pigdog
  self ! pigdog2
  self ! pigdog3
  self ! highpriority(2)
  self ! PoisonPill

  def receive = {
    case x ⇒ println(x.toString)
  }
}

object testMailbox  {
  def main(args:Array[String]) = {
    val system = ActorSystem.create("testMailbox")
    val a = system.actorOf(Props[Logger].withDispatcher("prio-dispatcher"))
  }

}

