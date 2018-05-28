package hbase

import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util
import javax.naming.spi.DirStateFactory.Result

import cn.com.skdb.api.SkHisval

class HbasePhoenix {

  import HbasePhoenix._

  //cpids: util.List[String]
  def loadIntoHbase(pointID:String, deptCode:String, hisVal:util.ArrayList[SkHisval]) = {

    var sqlBase = s"UPSERT INTO SKDB VALUES('AI_" + pointID + deptCode + s"_" //后接时间戳
    val dataLen = hisVal.get(0).values.size()

    for(i <- 0 to dataLen - 1) {
      val time = hisVal.get(0).values.get(i).Time.getTime
      var sql = sqlBase + time + s"'"

      var values = s"," + time
      for (j <- 0 to 11) {
        values = values + "," + hisVal.get(j).values.get(i).Value
      }

      sql = sql + values + ")"

      println(s"sql=" + sql)

      try {
        val ret = statment.execute(sql)
        println(s"load into hbase successfully")

      } catch {
        case e:Throwable =>
          println(s"insert into hbase failed, error=" + e.printStackTrace())
      }
    }
  }

  //获取hbase的数据
  def getFromHbase() = {
    val sql = s"select AXDY,BXDY,CXDY from SKDB"
    val rs:ResultSet = statment.executeQuery(sql)

    while (rs.next) {
      val axdy = rs.getDouble("AXDY")
      val bxdy = rs.getDouble("BXDY")
      val cxdy = rs.getDouble("CXDY")

      println(s"axdy=$axdy, bxdy=$bxdy, cxdy=$cxdy")

    }

  }

}

object HbasePhoenix {
  val driver = "org.apache.phoenix.jdbc.PhoenixDriver"
  var con:Connection = null
  var statment:Statement = null
  var upsertBatchSize: Int = 0

  def apply(): HbasePhoenix = {
    try {
      Class.forName(driver);
    } catch {
      case e:ClassNotFoundException =>  e printStackTrace
    }

    con = DriverManager.getConnection("jdbc:phoenix:192.168.10.95:2181")
    con.setAutoCommit(true)
    statment = con.createStatement()

    new HbasePhoenix()
  }

  def main(args:Array[String]) = {

    val hbase = HbasePhoenix()


//    val mysql = Mysql()
//    val cpidsOpt = mysql.getPointidDeptcode
//    val skdb = SKDB()
//    val hisValOpt = skdb.getHistoryFromSKDB(cpidsOpt.get._3)
//
////    if(hisValOpt.isEmpty)
////      println(s"get history failed")
////    else{
////      val hisVal = hisValOpt.get
////      hbase.loadIntoHbase(cpidsOpt.get._1, cpidsOpt.get._2, hisVal)
////    }
//
//    hbase.getFromHbase

    //    println(s"connect hbase!")
    //    System.setProperty("hadoop.home.dir", "F:\\hbase\\hadoop-common-2.2.0-bin-master\\hadoop-common-2.2.0-bin-master")
    //
    //    var connection: Connection = null
    //    var statement: Statement = null
    //    try {
    //      Class.forName("org.apache.phoenix.jdbc.PhoenixDriver")
    //      connection = DriverManager.getConnection("jdbc:phoenix:192.168.10.95:2181")
    //      statement = connection.createStatement()
    //
    //      println(s"connect OK!!!!!!")
    //
    //      //    //val getRet = statement.execute("get 't1','rowkey001'")
    //      val getRet = statement.executeQuery("select * from t1")
    //      println(s"getRet=" + getRet.toString)
    //
    //      //statement.execute("upsert into yinxiang_note values (3, 'note of huhong')")
    //    } catch {
    //      case e: Exception =>
    //        println(s"connect failed")
    //        e.printStackTrace()
    //    } finally {
    //      try {
    //        connection.close()
    //        statement.close()
    //      } catch {
    //        case e: Exception => e.printStackTrace()
    //      }
    //    }
  }
}
