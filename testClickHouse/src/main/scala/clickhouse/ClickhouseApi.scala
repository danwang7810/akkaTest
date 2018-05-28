package clickhouse

import java.io.{File, PrintWriter}
import java.sql.{ResultSet, Statement}
import java.text.SimpleDateFormat
import java.util
import ru.yandex.clickhouse.{ClickHouseConnection, ClickHouseConnectionImpl, ClickHouseDataSource, ClickHouseDriver}
import scala.collection.mutable.ArrayBuffer

class ClickhouseApi {
  import ClickhouseApi._

  private def makeCpids(pointid:String, deptcode:String):util.ArrayList[String] = {
    val cpids = new util.ArrayList[String]

    cpids.add("AI_" + pointid + deptcode + "_AXDY")
    cpids.add("AI_" + pointid + deptcode + "_BXDY")
    cpids.add("AI_" + pointid + deptcode + "_CXDY")

    cpids.add("AI_" + pointid + deptcode + "_AXDL" )
    cpids.add("AI_" + pointid + deptcode + "_BXDL")
    cpids.add("AI_" + pointid + deptcode + "_CXDL")

    cpids.add("AI_" + pointid + deptcode + "_AXWGGL")
    cpids.add("AI_" + pointid + deptcode + "_BXWGGL")
    cpids.add("AI_" + pointid + deptcode + "_CXWGGL")

    cpids.add("AI_" + pointid + deptcode + "_AXYGGL")
    cpids.add("AI_" + pointid + deptcode + "_BXYGGL")
    cpids.add("AI_" + pointid + deptcode + "_CXYGGL")

    cpids.add("AI_" + pointid + deptcode + "_GLYS")
    cpids.add("AI_" + pointid + deptcode + "_LXDL")
    cpids.add("AI_" + pointid + deptcode + "_WGGL")
    cpids.add("AI_" + pointid + deptcode + "_YGGL")

    return cpids
  }

  def querydb() = {
    val stmt = connect.createStatement
    val rs = stmt.executeQuery("select id,pointID,deptCode from device where id > 172748;")
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    var testNum = 0

    while(rs.next()) {
      val id = rs.getString(1)
      //val pointid = rs.getString(2)
      //val deptcode = rs.getString(3)
      val cpids = makeCpids(rs.getString(2), rs.getString(3))
      println(s"id=$cpids")
      val hisValsOpt = skdb.getHistoryFromSKDB(cpids)
      if(!hisValsOpt.isEmpty) {
        var colHead = "" + id + ","
        val hisVals = hisValsOpt.get
        var valLen = hisVals.get(0).values.size

        for(i <- 1 to 15) {
          if(valLen > hisVals.get(i).values.size)
            valLen = hisVals.get(i).values.size
        }

        if(valLen > 0) {
          for (n <- 0 to valLen - 1) {
            var col = colHead
            val time = hisVals.get(0).values.get(n).Time
            col += sdf.format(time) + "," + sdfDateTime.format(time) + ","

            for (m <- 0 to 15) {
              col += hisVals.get(m).values.get(n).Value
              if (m != 15)
                col += ","
            }

            writer.write(col + "\r\n")
          }
        }
      }
      writer.flush()
    }

    writer.close()
    rs.close()
    stmt.close()
  }

}

object ClickhouseApi {

  var connect:ClickHouseConnection = null
  val skdb = new SKDB()
  val writer = new PrintWriter(new File("jxdky-4.csv" ))

  //URL syntax: jdbc:clickhouse://<host>:<port>[/<database>], e.g. jdbc:clickhouse://localhost:8123/test
  //
  def apply(): ClickhouseApi = {
    val url = s"jdbc:clickhouse://192.168.10.95:8123/default" //8123
    val dataSource = new ClickHouseDataSource(url)
    connect =  dataSource.getConnection

    new ClickhouseApi()
  }

  def main(args:Array[String]):Unit = {
    val clickhouseApi = ClickhouseApi()
    clickhouseApi.querydb()

//    val cpid = "AI_100373_0800000470_AXDL"
//    //val receivePathPattern = "\\S+/user/\\$\\S+/\\$\\S+/\\S+[^/]".r
//
//
//    //val cpidPattern="""\S+_([0-9]+)_([0-9]+)_\S+""".r
//    val cpidPattern="""AI_([0-9]+)(_[0-9]+)_\S+""".r
//
//    val cpidPattern(pointID, deptCode) = cpid
//    println(s""+pointID  + "  " + deptCode)
//
////    cpid match {
////      case cpidPattern(pointID, deptCode) =>
////        println(s""+pointID  + "  " + deptCode)
////    }



  }
}
