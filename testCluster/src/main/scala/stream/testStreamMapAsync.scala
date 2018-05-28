package stream

import java.io.{File, PrintWriter}
import akka.actor._
import akka.pattern._
import akka.stream._
import akka.stream.scaladsl._
import akka.routing._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class testMsg(t:String)

object Calculator {
  trait Operations
  case class Add(op1:Int, op2:Int) extends Operations
  case class DisplayError(err: Exception) extends Operations
  case object Stop extends Operations
  case class ProduceError(err: Exception) extends Operations

  def props(inputQueue: SourceQueueWithComplete[testMsg]) = Props(new Calculator(inputQueue))
}

class Calculator(inputQueue: SourceQueueWithComplete[testMsg]) extends Actor with ActorLogging{
  import Calculator._
  import context.dispatcher
  override def receive: Receive = {
    case Add(op1,op2) =>
      val msg = s"$op1 + $op2 = ${op1 + op2}"
      //println(s"Calculator receive msg=$msg")
      inputQueue.offer(testMsg(msg))
      .recover {
        case e: Exception => DisplayError(e)}
      .pipeTo(self).mapTo[String]
    case QueueOfferResult =>
      log.info("QueueOfferResult.Enqueued")
    case QueueOfferResult.Enqueued =>
      log.info("QueueOfferResult.Enqueued")
    case QueueOfferResult.Dropped =>
    case QueueOfferResult.Failure(cause) =>
    case QueueOfferResult.QueueClosed  =>
      log.info("QueueOfferResult.QueueClosed")

    case Stop => inputQueue.complete()

    case ProduceError(e) => inputQueue.fail(e)

  }
}


object StorageActor {

  val onInitMessage = "start"
  val onCompleteMessage = "done"
  val ackMessage = "ack"

  val writer = new PrintWriter(new File("testResult.txt" ))
  //val writer = FileIO.toPath(Paths.get("factorials.txt"))

  case class Query(rec: Int, qry: String) //模拟数据存写Query

  class DbException(cause: String) extends Exception(cause) //自定义存写异常

  class StorageActor extends Actor with ActorLogging { //存写操作Actor
    override def receive: Receive = {
      case `onInitMessage` => sender() ! ackMessage

      //      case Query(num,qry) =>
      //        var res: String = ""
      //        try {
      //          res = saveToDB(num,qry)
      //        } catch {
      //          case e: Exception => Error(num,qry) //模拟操作异常
      //        }
      //        //sender() ! res
      //        sender() ! ackMessage

      case testMsg(m) =>
        //println(s"testMsg ${self.path}  saving:$m")
        //println(s"$m")
        writer.write(m+"\n")
        writer.flush()

        //sender ! m + " ack"
        sender() ! ackMessage

      case `onCompleteMessage` => //clean up resources 释放资源
      case _ =>
    }

    //    def saveToDB(num: Int,qry: String): String = { //模拟存写函数
    //      val msg = s"${self.path} is saving: [$qry#$num]"
    //      if ( num % 3 == 0) Error(num,qry)        //模拟异常
    //      else {
    //        log.info(s"${self.path} is saving: [$qry#$num]")
    //        s"${self.path} is saving: [$qry#$num]"
    //      }
    //    }
    //
    //    def Error(num: Int,qry: String): String = {
    //      val msg = s"${self.path} is saving: [$qry#$num]"
    //      sender() ! msg
    //      throw new DbException(s"$msg blew up, boooooom!!!")
    //    }

    //    //验证异常重启
    //    //BackoffStrategy.onStop goes through restart process
    //    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    //      log.info(s"Restarting ${self.path.name} on ${reason.getMessage}")
    //      super.preRestart(reason, message)
    //    }
    //
    //    override def postRestart(reason: Throwable): Unit = {
    //      log.info(s"Restarted ${self.path.name} on ${reason.getMessage}")
    //      super.postRestart(reason)
    //    }
    //
    //    override def postStop(): Unit = {
    //      log.info(s"Stopped ${self.path.name}!")
    //      super.postStop()
    //    }
    //    //BackOffStrategy.onFailure dosn't go through restart process
    //    override def preStart(): Unit = {
    //      log.info(s"PreStarting ${self.path.name} ...")
    //      super.preStart()
    //    }


  }
  def props = Props(new StorageActor)
}

//自定义actor的监管策略
object StorageActorGuardian {  //带监管策略的StorageActor
  def props: Props = { //在这里定义了监管策略和StorageActor构建
    def decider: PartialFunction[Throwable, SupervisorStrategy.Directive] = {
      case _: StorageActor.DbException => SupervisorStrategy.Restart
    }

    val options = Backoff.onStop(StorageActor.props, "dbWriter", 100 millis, 500 millis, 0.0)
      .withManualReset
      .withSupervisorStrategy(
        OneForOneStrategy(maxNrOfRetries = 3, withinTimeRange = 1 second)(
          decider.orElse(SupervisorStrategy.defaultDecider)
        )
      )
    BackoffSupervisor.props(options)
  }
}

object IntegrateDemo extends  App{
    implicit val sys = ActorSystem("demoSys")
    implicit val ec = sys.dispatcher
    implicit val mat = ActorMaterializer(
      ActorMaterializerSettings(sys)
        .withInputBuffer(initialSize = 16777216, maxSize = 16777216) //1M
    )

    val source: Source[testMsg, SourceQueueWithComplete[testMsg]] =
      Source.queue[testMsg](bufferSize = 16777216, overflowStrategy = OverflowStrategy.backpressure)

    ///////////////////////////////
    val numOfActors = 3
    val routees: List[ActorRef] = List.fill(numOfActors)(
      sys.actorOf(StorageActorGuardian.props)
    )
    val routeePaths: List[String] = routees.map { ref => "/user/" + ref.path.name } //获取ActorPath

    val storageActorPool = sys.actorOf(
      RoundRobinGroup(routeePaths).props(), "starageActorPool"
    )
    ////////////////////////

    import StorageActor._
    val inputQueue: SourceQueueWithComplete[testMsg] = source.toMat(Sink.actorRefWithAck(storageActorPool, onInitMessage, ackMessage, onCompleteMessage))(Keep.left).run
    //val inputQueue: SourceQueueWithComplete[String] = source.toMat(Sink.foreach(println))(Keep.left).run()

    inputQueue.watchCompletion().onComplete {
      case Success(result) => println(s"Calculator ends with: $result")
      case Failure(cause) => println(s"Calculator ends with exception: ${cause.getMessage}")
    }

    val calc = sys.actorOf(Calculator.props(inputQueue), "calculator")

    import Calculator._

    var count = 0
    while (count < 100000) {
      //    calc ! Add(3, 5)
      //    calc ! Add(39, 1)
      //    calc ! ProduceError(new Exception("Boooooommm!"))
      //    scala.io.StdIn.readLine
      calc ! Add(count, 1)
      count = count + 1
    }

    scala.io.StdIn.readLine
    sys.terminate()

    ///////////////////////////////
    ////  implicit val timeout = Timeout(3 seconds)
    ////  val source  : Source[String, SourceQueueWithComplete[String]]  =
    ////    Source.queue[String](bufferSize = 16,  overflowStrategy = OverflowStrategy.backpressure).mapAsync(parallelism = 1){ n =>
    ////      (storageActorPool ? StorageActor.testMsg(n)).mapTo[String]
    ////    }
    ////    //.runWith(Sink.foreach(println))
    ////
    ////  val inputQueue: SourceQueueWithComplete[String] = source.toMat(Sink.foreach(println))(Keep.left).run()
    ////
    ////
    ////  inputQueue.watchCompletion().onComplete {
    ////    case Success(result) => println(s"Calculator ends with: $result")
    ////    case Failure(cause)  => println(s"Calculator ends with exception: ${cause.getMessage}")
    ////  }
    ////
    ////  val calc = sys.actorOf(Calculator.props(inputQueue),"calculator")
    ////
    ////  calc ! testMsg("111")
    ////  calc ! testMsg("222")
    ////  calc ! testMsg("333")
    ////  calc ! testMsg("444")
    ////  calc ! testMsg("555")
    ////  calc ! testMsg("666")
    //
    //  ////////////////////////////////////////////
    //  val testArray = Array[testMsg](testMsg("aaa"), testMsg("bbb"), testMsg("ccc"))
    //
    ////  implicit val timeout = Timeout(3 seconds)
    ////  Source.fromIterator(() => testArray.iterator).mapAsync(parallelism = 1){ n =>
    ////    (storageActorPool ? n).mapTo[String]
    ////  }.runWith(Sink.foreach(println))
    //
    //  import StorageActor._
    //
    //  Source.fromIterator(() => testArray.iterator).runWith(Sink.actorRefWithAck(
    //      storageActorPool, onInitMessage, ackMessage, onCompleteMessage))
    //
    ////  Source(Stream.from(1)).delay(3.second,DelayOverflowStrategy.backpressure)
    ////    .mapAsync(parallelism = 1){ n =>
    ////      (storageActorPool ? StorageActor.Query(n,s"Record")).mapTo[String]
    ////    }.runWith(Sink.foreach(println))

    scala.io.StdIn.readLine()
    sys.terminate()


}
