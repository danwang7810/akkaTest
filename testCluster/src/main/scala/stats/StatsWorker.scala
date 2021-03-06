package stats

import akka.actor.Actor

//#worker
class StatsWorker extends Actor {
  var cache = Map.empty[String, Int]

  println(s"StatsWorker start")
  def receive = {
    case word: String =>
      val length = cache.get(word) match {
        case Some(x) => x
        case None =>
          val x = word.length
          cache += (word -> x)
          x
      }

      sender() ! length
  }
}
//#worker