package hbase

import java.io.File
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, List, Random}

import cn.com.skdb.api._

import scala.collection.mutable.ArrayBuffer

class SKDB {
  import SKDB._

  def getHistoryFromSKDB(cpids: util.List[String]):Option[util.ArrayList[SkHisval]] = {
    val hisValues = new util.ArrayList[SkHisval]

    try {

      if(connect.isEmpty)
        return None

      val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      val sdate = simpleDateFormat.parse("2017-06-01 00:00:00")
      val edate = simpleDateFormat.parse("2017-06-03 00:00:00")

      println(s"cpids=$cpids  sdate=$sdate edate=$edate")
      val ret:Int = connect.get.GetHistoryValueByCpid(cpids, sdate, edate, hisValues)
      if((ret != 0) || (hisValues.size() == 0)) {
        return None
      } else {
        return Some(hisValues)
      }

    } catch {
      case e:Throwable =>
        return None

    } finally {
      connect.get.CloseConnect()
    }
  }

}


object SKDB {
  var hosts: List[String] = new util.ArrayList[String]
  var port: Int = 9099
  var connect:Option[ThriftConnect] = createConnect

  //创建连接
  def createConnect(): Option[ThriftConnect] = {
    hosts.add("192.168.10.181")
//    hosts.add("192.168.10.182")
//    hosts.add("192.168.10.183")
//    hosts.add("192.168.10.184")
//    hosts.add("192.168.10.185")
//    hosts.add("192.168.10.186")

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

    Some(connect)
  }

  def apply():SKDB = {
    new SKDB()
  }

}