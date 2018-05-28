package stats

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.routing.FromConfig

//#service
class StatsService extends Actor {
  // This router is used both with lookup and deploy of routees. If you
  // have a router with only lookup of routees you can use Props.empty
  // instead of Props[StatsWorker.class].
  val workerRouter = context.actorOf(FromConfig.props(Props[StatsWorker]),
    name = "workerRouter")

  println(s"StatsService start")

  def receive = {
    case StatsJob(text) if text != "" =>

      println(s"text="+ text)

      val words = text.split(" ")
      val replyTo = sender() // important to not close over sender()
      // create actor that collects replies from workers
      val aggregator = context.actorOf(Props(
        classOf[StatsAggregator], words.size, replyTo))
      words foreach { word => {
//          val consistentHashableEnvelope = ConsistentHashableEnvelope(word, word)
//          println(s"envelope=" + consistentHashableEnvelope.message)
//          workerRouter.tell(consistentHashableEnvelope, aggregator)
        workerRouter.tell(word, aggregator)
        }
      }
  }
}

class StatsAggregator(expectedResults: Int, replyTo: ActorRef) extends Actor {
  var results = IndexedSeq.empty[Int]
  context.setReceiveTimeout(3.seconds)

  println(s"StatsAggregator start")

  def receive = {
    case wordCount: Int =>
      results = results :+ wordCount
      println(s"results.size="+ results.size + s"  expectedResults=$expectedResults")
      if (results.size == expectedResults) {
        val meanWordLength = results.sum.toDouble / results.size
        replyTo ! StatsResult(meanWordLength)
        context.stop(self)
      }
    case ReceiveTimeout =>
      replyTo ! JobFailed("Service unavailable, try again later")
      context.stop(self)
  }
}
//#service
