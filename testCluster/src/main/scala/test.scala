import scala.util.matching.Regex

class test {

}

object test {
  def main(args:Array[String]) = {

    //数字和字母的组合正则表达式
    //val parentStr = """akka://CalPlatformNode/remote/akka.tcp/ThriftServiceMaster@192.168.10.91:10020/user/$f/$a/c1"""
    //val parentStr = "akka://CalPlatformNode/remote/akka.tcp/ThriftServiceMaster@192.168.10.91:10020/user/$f/$a/c1/"
    val parentStr = "akka://CalPlatformNode/remote/akka.tcp/ThriftServiceMaster@192.168.10.91:10020/user/$m/ZeroSequenceActorRouter/c1"
    //val parentPathPattern="""\\S/user/\\$\\S/\\$\\S/\\S""".r
    val parentPathPattern = "(\\S+/user/\\$\\S+/\\$\\S+/\\S+[^/])".r
    parentStr match{
      case parentPathPattern(tmp) => {
//        val reSendMsgReceive = context.actorSelection(deadLetter.recipient.path.parent)
//        reSendMsgReceive ! deadLetter.message
        println(s" ook =  $tmp")
      }
      case _=> println(s"failed")
    }

//    val pattern = new Regex("\\S+/user/\\$\\S+/\\$\\S+/\\S+")
//    println(pattern.findFirstIn(parentStr))

  }
}
