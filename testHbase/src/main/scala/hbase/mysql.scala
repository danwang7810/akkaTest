package hbase

import java.io.File
import java.sql.Connection
import java.util
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer

class Mysql {
  import Mysql.con

  private val log = LoggerFactory.getLogger(Mysql.getClass)

  def getConnect(): Connection = {
    con
  }

  def closeConnect(connect:Connection): Unit = {
  }

  def getPointidDeptcode():Option[Tuple3[String, String, util.List[String]]] = {

    var cpids = new util.ArrayList[String](16)
    val sql = s"select pointId,deptCode from device  where deptCode='_0300000051'"

    //执行查询，获取数据
    val statement = con.createStatement()
    val rs = statement.executeQuery(sql)
    var devCodes = new ArrayBuffer[String]

    var pointID:String = null
    var code:String = null

    try {
      while (rs.next()) {
        pointID = rs.getString("pointId")
        code = rs.getString("deptCode")

        cpids.add ("AI_" + pointID + code + "_AXDY")
        cpids.add ("AI_" + pointID + code + "_BXDY")
        cpids.add ("AI_" + pointID + code + "_CXDY")
        cpids.add ("AI_" + pointID + code + "_AXDL")
        cpids.add ("AI_" + pointID + code + "_BXDL")
        cpids.add ("AI_" + pointID + code + "_CXDL")
//        cpids.add ("AI_" + pointID + code + "_AXWGGL")
//        cpids.add ("AI_" + pointID + code + "_AXYGGL")
//        cpids.add ("AI_" + pointID + code + "_CXWGGL")
//        cpids.add ("AI_" + pointID + code + "_CXYGGL")
        cpids.add ("AI_" + pointID + code + "_AXYGGL")
        cpids.add ("AI_" + pointID + code + "_BXYGGL")
        cpids.add ("AI_" + pointID + code + "_CXYGGL")
        cpids.add ("AI_" + pointID + code + "_AXWGGL")
        cpids.add ("AI_" + pointID + code + "_BXWGGL")
        cpids.add ("AI_" + pointID + code + "_CXWGGL")
      }

      return Some(new Tuple3[String, String, util.List[String]](pointID, code, cpids))
    } catch {
      case e : Throwable =>
        log.error(e.getMessage, e)
        return None
    } finally {
      rs.close()
      closeConnect(con)
    }
  }
}

object Mysql{
  //apply函数
  var con:Connection = null
  def apply():Mysql = {
    import java.sql.DriverManager
    val driver = "com.mysql.cj.jdbc.Driver" //com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://192.168.10.181:3306/jxdky?useUnicode=true&characterEncoding=utf-8&useSSL=false"
    val user = "root"
    val password = "usbw"

    //加载驱动程序//加载驱动程序
    Class.forName(driver)
    //1.getConnection()方法，连接MySQL数据库！！
    con = DriverManager.getConnection(url, user, password)
    new Mysql()
  }

  def main(args: Array[String]): Unit = {
    val mysql = Mysql()
    val ret = mysql.getPointidDeptcode
    println(ret.get.toString)
  }
}
