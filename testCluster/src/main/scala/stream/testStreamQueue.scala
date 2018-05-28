//import akka.actor._
//import akka.stream._
//import akka.stream.scaladsl._
//import scala.util._
//import akka.pattern._
//
//object Calculator {
//  trait Operations
//  case class Add(op1:Int, op2:Int) extends Operations
//  case class DisplayError(err: Exception) extends Operations
//  case object Stop extends Operations
//  case class ProduceError(err: Exception) extends Operations
//
//  def props(inputQueue: SourceQueueWithComplete[String]) = Props(new Calculator(inputQueue))
//}
//class Calculator(inputQueue: SourceQueueWithComplete[String]) extends Actor with ActorLogging{
//  import Calculator._
//  import context.dispatcher
//  override def receive: Receive = {
//    case Add(op1,op2) =>
//      val msg = s"$op1 + $op2 = ${op1 + op2}"
//      inputQueue.offer(msg)
//        .recover {
//          case e: Exception => DisplayError(e)}
//        .pipeTo(self).mapTo[String]
//    case QueueOfferResult =>
//      log.info("QueueOfferResult.Enqueued")
//    case QueueOfferResult.Enqueued =>
//      log.info("QueueOfferResult.Enqueued")
//    case QueueOfferResult.Dropped =>
//    case QueueOfferResult.Failure(cause) =>
//    case QueueOfferResult.QueueClosed  =>
//      log.info("QueueOfferResult.QueueClosed")
//
//    case Stop => inputQueue.complete()
//    case ProduceError(e) => inputQueue.fail(e)
//
//  }
//}
//
//
//object SourceQueueTest extends App {
//  implicit val sys = ActorSystem("demoSys")
//  implicit val ec = sys.dispatcher
//  implicit val mat = ActorMaterializer(
//    ActorMaterializerSettings(sys)
//      .withInputBuffer(initialSize = 16, maxSize = 16)
//  )
//
//  val source: Source[String, SourceQueueWithComplete[String]]  =
//    Source.queue[String](bufferSize = 16,
//      overflowStrategy = OverflowStrategy.backpressure)
//
//  val inputQueue: SourceQueueWithComplete[String] = source.toMat(Sink.foreach(println))(Keep.left).run()
//
//  inputQueue.watchCompletion().onComplete {
//    case Success(result) => println(s"Calculator ends with: $result")
//    case Failure(cause)  => println(s"Calculator ends with exception: ${cause.getMessage}")
//  }
//
//  val calc = sys.actorOf(Calculator.props(inputQueue),"calculator")
//
//  import Calculator._
//
//  calc ! Add(3,5)
//  scala.io.StdIn.readLine
//  calc ! Add(39,1)
//  scala.io.StdIn.readLine
//  calc ! ProduceError(new Exception("Boooooommm!"))
//  scala.io.StdIn.readLine
//  calc ! Add(1,1)
//
//  scala.io.StdIn.readLine
//  sys.terminate()
//
//}