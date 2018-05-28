package hbase

import java.text.SimpleDateFormat
import java.util

import cn.com.skdb.api.SkHisval
import hbase.HbasePhoenix.statment
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{Cell, CellUtil, HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter.{CompareFilter, RegexStringComparator, RowFilter}
import org.apache.hadoop.hbase.util.Bytes

import scala.collection.mutable.ArrayBuffer

class HbaseJavaApi {

  import HbaseJavaApi._

  def loadIntoHbase(pointID:String, deptCode:String, hisVal:util.ArrayList[SkHisval]) = {

    val dataLen = hisVal.get(0).values.size()

    for(i <- 0 to dataLen - 1) {
      val time = hisVal.get(0).values.get(i).Time.getTime

      val rowkey = s"AI_" + pointID + deptCode + "_" + time
      var put = new Put(Bytes.toBytes(rowkey))

      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("time"), Bytes.toBytes(time))

      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("AXDY"), Bytes.toBytes(hisVal.get(0).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("BXDY"), Bytes.toBytes(hisVal.get(1).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("CXDY"), Bytes.toBytes(hisVal.get(2).values.get(i).Value))

      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("AXDL"), Bytes.toBytes(hisVal.get(3).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("BXDL"), Bytes.toBytes(hisVal.get(4).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("CXDL"), Bytes.toBytes(hisVal.get(5).values.get(i).Value))

      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("AXYGGL"), Bytes.toBytes(hisVal.get(6).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("BXYGGL"), Bytes.toBytes(hisVal.get(7).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("CXYGGL"), Bytes.toBytes(hisVal.get(8).values.get(i).Value))

      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("AXWGGL"), Bytes.toBytes(hisVal.get(9).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("BXWGGL"), Bytes.toBytes(hisVal.get(10).values.get(i).Value))
      put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("CXWGGL"), Bytes.toBytes(hisVal.get(11).values.get(i).Value))

      table.put(put)
    }
  }

  //獲取當前值,返回：時間戳和當前值
  def query(pointID:String, deptCode:String, columnName:String):Option[(Long, Double)] = {

    val scan = new Scan()
    val comp = new RegexStringComparator("AI_" + pointID + deptCode)
    val filter = new RowFilter(CompareOp.EQUAL, comp)
    scan.setFilter(filter)
    //    scan.setCaching(1)
    //    scan.setCacheBlocks(false)
    scan.setReversed(true)

    val resultScan = table.getScanner(scan).iterator()

    if (resultScan.hasNext == false) {
      return None

    } else {
      val r = resultScan.next()

      if (r.containsNonEmptyColumn("info".getBytes, columnName.getBytes)) {
        val colVal = Bytes.toDouble(CellUtil.cloneValue(r.getColumnCells("info".getBytes, columnName.getBytes).get(0)))
        println(s"$colVal")
      }

      val timeVal = Bytes.toLong(CellUtil.cloneValue(r.getColumnCells("info".getBytes, "time".getBytes).get(0)))
      println(s"$timeVal")

    }

//
//      val listCells = resultScan.next().listCells
//      listCells
//
//      var resultStr = s""
//      var qualifierStr = s""
//
//      for (i <- 0 to listCells.size() - 1) { //遍歷每個字段
//        val kv = listCells.get(i)
//        val qualifier = Bytes.toString(CellUtil.cloneQualifier(kv))
//        qualifierStr += " " + qualifier
//
//        if (qualifier.compareTo("time") == 0) {
//          resultStr += "  " + Bytes.toLong(CellUtil.cloneValue(kv))
//        } else {
//          resultStr += "  " + Bytes.toDouble(CellUtil.cloneValue(kv))
//        }
//      }
//
//      println(s"$qualifierStr")
//      println(s"$resultStr")
//    }

    return None
  }

  //獲取snapshot
  def query(rowKey: String): Option[ArrayBuffer[Double]] = {

    val get = new Get(Bytes.toBytes(rowKey))
    val result = table.get(get)
    val listCells = result.listCells()

    for (i <- 0 to listCells.size() - 1) {
      val kv = listCells.get(i)
      val qualifier = Bytes.toString(CellUtil.cloneQualifier(kv))
      println(s"qualifier=$qualifier")

      if (qualifier.compareTo("time") == 0) {
        println("value=" + Bytes.toLong(CellUtil.cloneValue(kv)))
      } else {
        println("value=" + Bytes.toDouble(CellUtil.cloneValue(kv)))
      }
      println("-------------------------------------------")
    }

    return None
  }

  //按時間段查詢
  def scan(pointid:String, deptCode:String, beginDate:Long, endDate:Long): Unit = {

    val scan = new Scan()
    val start_rowkey = s"AI_" + pointid + deptCode + "_" + beginDate
    val end_rowkey = s"AI_" + pointid + deptCode + "_" + endDate

    scan.setStartRow(Bytes.toBytes(start_rowkey))
    scan.setStopRow(Bytes.toBytes(end_rowkey))

    //var resultScan: ResultScanner = null
    val resultScan = table.getScanner(scan).iterator()




    var count = 0
    while(resultScan.hasNext) {//按行遍历
      count = count + 1
      val r = resultScan.next()

      val listCells = r.listCells()

      var resultStr = s""
      var qualifierStr = s""

      for (i <- 0 to listCells.size() - 1) { //遍歷每個字段
        val kv = listCells.get(i)


        val qualifier = Bytes.toString(CellUtil.cloneQualifier(kv))
        qualifierStr += " " + qualifier
        //println(s"qualifier=$qualifier")
        if(qualifier.compareTo("time") == 0) {
          //println("value" + Bytes.toLong(CellUtil.cloneValue(kv)))
          resultStr += "  " + Bytes.toLong(CellUtil.cloneValue(kv))
        } else {
          //println("value" + Bytes.toDouble(CellUtil.cloneValue(kv)))
          //resultStr += "  " + Bytes.toDouble(CellUtil.cloneValue(kv))
        }

      }

      //println(s"$qualifierStr")
      println(s"$resultStr")
      //println(s"count=$count")

    }

  }

}

object HbaseJavaApi {

  var connection:Connection = null
  var conf:Configuration  = null
  var table:Table = null

  def apply(): HbaseJavaApi = {

    conf = HBaseConfiguration.create()
    try {
      conf.set("hbase.zookeeper.quorum", "192.168.10.95:2181")
      connection = ConnectionFactory.createConnection(conf)
      table = connection.getTable(TableName.valueOf("skdb"))
    } catch  {
      case e:Throwable => e.printStackTrace()
    }

    new HbaseJavaApi()
  }

  def main(args:Array[String]) :Unit = {
    val hbase_java = HbaseJavaApi()

    //-------------------------- 導入hbase------------------------------
    //    val mysql = Mysql()
    //    val cpidsOpt = mysql.getPointidDeptcode
    //    val skdb = SKDB()
    //    val hisValOpt = skdb.getHistoryFromSKDB(cpidsOpt.get._3)
    //
    //    if(hisValOpt.isEmpty)
    //      println(s"get history failed")
    //    else{
    //      val hisVal = hisValOpt.get
    //      hbase_java.loadIntoHbase(cpidsOpt.get._1, cpidsOpt.get._2, hisVal)
    //    }
    //-----------------------------------------------------------------

    //hbase_java.query("skdb", "AI_111352_0300000051_1496419200000")


    //    val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //    val sdate = simpleDateFormat.parse("2017-06-01 00:00:00")
    //    val edate = simpleDateFormat.parse("2017-06-01 12:00:00")
    //    println(s"sdate=" + sdate.getTime + " edate=" + edate.getTime)
    //    hbase_java.scan("111352", "_0300000051", sdate.getTime, edate.getTime)


    hbase_java.query("111352", "_0300000051", "AXDY")


  }


}

