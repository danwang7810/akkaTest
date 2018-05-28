package clickhouse

import java.text.SimpleDateFormat
import java.util
import java.util.{Date, List, Random}
import cn.com.skdb.api._

class SKDB {
  import SKDB._

  def getHistoryFromSKDB(cpids: util.List[String]):Option[util.ArrayList[SkHisval]] = {
    val hisValues = new util.ArrayList[SkHisval]

    val connect = createConnect()
    if(connect == null)
      return None

    try {
      //println(s"cpids=$cpids  sdate=$sdate edate=$edate")
      val ret:Int = connect.GetHistoryValueByCpid(cpids, sdate, edate, hisValues)
      if((ret != 0) || (hisValues.size() == 0)) {
        println(s"history is null")
        return None
      } else {
//        for(i <- 0 to hisValues.size() - 1)
//          println(s"" + hisValues.get(i).values.toString())

        return Some(hisValues)
      }
    } catch {
      case e:Throwable =>
        return None

    } finally {
      closeConnect(connect)
    }
  }
}


object SKDB {
  var hosts: List[String] = new util.ArrayList[String]
  hosts.add("192.168.10.181")
  //hosts.add("192.168.10.182")
  //    hosts.add("192.168.10.183")
  //    hosts.add("192.168.10.184")
  //    hosts.add("192.168.10.185")
  //    hosts.add("192.168.10.186")

  var port: Int = 9099
  ///var connect:ThriftConnect = createConnect

  val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val sdate = simpleDateFormat.parse("2017-06-01 00:00:00")
  val edate = simpleDateFormat.parse("2017-06-30 23:59:00")

  //创建连接
  def createConnect(): ThriftConnect = {
    var hostIndex = (new Random).nextInt(hosts.size)
    var createSuccess = false
    var connect: ThriftConnect = null
    var connectTryTime = 0

    while (createSuccess == false && connectTryTime < hosts.size()) {
      try {
        connect = new ThriftConnect(hosts.get(hostIndex), port, "", "")
        createSuccess = true
      } catch {
        //case e:Throwable =>
        case e: Any =>
          hostIndex = (hostIndex + 1) % hosts.size
          Thread.sleep(100) //100ms
          connectTryTime = connectTryTime + 1
      }
    }

    connect
  }

  def closeConnect(connect:ThriftConnect) = {
    if(connect != null)
      connect.CloseConnect()
  }

  def apply():SKDB = {



    new SKDB()
  }

}