package cesium

import java.io._
import java.net.URL
import java.util
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import akka.actor.{Actor, ActorSystem, Props, ReceiveTimeout}
import akka.routing.Broadcast
import akka.routing.RandomPool
import com.alibaba.fastjson.{JSON, JSONObject}
import scala.collection.mutable.ArrayBuffer
import scala.io.{Source, StdIn}
import scala.math._
case class LonLat(minLon:Double ,maxLon:Double, minLat:Double, maxLat:Double)
case class GetDateMsg(model:Int, zoom:Int, lonLat:LonLat)
case class tile(startX:Int, startY:Int, endX:Int, endY:Int)
case class TileInfo(x:Int, y:Int, z:Int)
case class TileNumber(x: Int,y: Int, z: Int)
case object TileFinished

class DownloadTileRoutee extends Actor {
  //var link = "http://assets.agi.com/stk-terrain/world/{z}/{x}/{y}.terrain?v=1.31376.0&f=TerrainTile"
  var link = "https://assets.cesium.com/1/{z}/{x}/{y}.terrain?v=1.1.0&"
  link +="access_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIyYjU1NTY5Mi1jNGQ4LTRiY2YtYWZhMC1iMTQyOTYzMTQwNDUiLCJpZCI6MjU5LCJhc3NldHMiOnsiMSI6eyJ0eXBlIjoiVEVSUkFJTiIsImV4dGVuc2lvbnMiOlt0cnVlLHRydWUsdHJ1ZV0sInB1bGxBcGFydFRlcnJhaW4iOnRydWV9fSwic3JjIjoiYjBkYzNkMWItODgzNi00MzAxLThiZjktZjQ5ZGNkNjYxODI3IiwiaWF0IjoxNTQ1ODgyMTU5LCJleHAiOjE1NDU4ODU3NTl9.8hRsvPNJ5AeRb5f_OaDlBdX8oXdh2KssDmmtotG7epE"

  var count = 0
  var excuteFailedTime = 0
  var failedArray:ArrayBuffer[TileInfo] = null

  def decompressTile(is:InputStream, os:OutputStream) ={
    //    val bais = new ByteArrayInputStream(in)
    //    val baos = new ByteArrayOutputStream()

    val BUFFER = 5120
    val gis = new GZIPInputStream(is)
    var count = 0
    val data = new Array[Byte](BUFFER)

    count = gis.read(data, 0, BUFFER)
    while (count != -1) {
      os.write(data, 0, count)
      count = gis.read(data, 0, BUFFER)
    }

    //      var len = in.read(tempBuffer)
    //      while (len != -1) {
    //        out.write(tempBuffer, 0, len)
    //        len = in.read(tempBuffer)
    //      }

    gis.close()
  }

  def writeData(i:Int, j:Int, z:Int, file:File):Boolean = {
    var in:InputStream = null
    val out = new FileOutputStream(file)

    try {
      //val url = new URL("http://assets.agi.com/stk-terrain/world/0/1/0.terrain?v=1.31376.0&f=TerrainTile")
      val url = new URL(link.replace("{x}", i + "").replace("{y}", j + "").replace("{z}", z + ""))
      //val url = link.replace("{x}", i + "").replace("{y}", j + "").replace("{z}", z + "")

      val conn = url.openConnection()
      conn.setRequestProperty("accept", "*/*")
      conn.setRequestProperty("connection", "Keep-Alive")
      conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:47.0) Gecko/20100101 Firefox/60.0")

      conn.connect()
      in = conn.getInputStream()

      //解压文件
      decompressTile(in, out)

      return true
    } catch {
      //System.out.println(e.getMessage());
      case e: Throwable =>
        //e.printStackTrace
        return false
    } finally {
      if(out != null)
        out.close()

      if(in != null)
        in.close()
    }
  }

  override def receive = {
    case TileInfo(x, y, z) =>

      val dir = new File(z + "/" + x)
      if (!dir.exists)
        dir.mkdirs

      val file = new File(z + "/" + x + "/" + y + ".terrain")
      if (!file.exists) {
        file.createNewFile
        if(writeData(x, y, z, file) == false)
          self ! TileInfo(x, y, z)
      }

    //      if(failedArray == null)
    //        failedArray = new ArrayBuffer[TileInfo]
    //
    //      if(writeData(x, y, z) == false) {
    //        failedArray += TileInfo(x, y, z)
    //      }

    case TileFinished =>
    //      val tmpFailedArray = failedArray
    //      failedArray = null
    //      if(excuteFailedTime < 3) {
    //        println(s"excute failed time=$excuteFailedTime number=" + tmpFailedArray.size)
    //        for (tileInfo <- tmpFailedArray)
    //          self ! tileInfo
    //      } else {
    //        println(s"failed=" + tmpFailedArray.size)
    //      }
    //
    //      excuteFailedTime = excuteFailedTime + 1

      println(s"send success 1")
      context.parent ! 1
  }
}

class Download extends Actor{

  private def getRange(min:Int, max:Int, xMin:Int, xMax:Int):Option[Tuple2[Int, Int]] = {
    if(xMin > max || xMax < min)
      return None
    //    else
    //      return Some(new Tuple2[Int, Int](min, max))

    else if(xMin <= min && xMax >= max)
      return Some(new Tuple2[Int, Int](min, max))
    else if (xMin >= min && xMax <= max)
      return Some(new Tuple2[Int, Int](xMin, xMax))
    else if (xMin <= min && xMax <= max)
      return Some(new Tuple2[Int, Int](min, xMax))
    else if (xMin >= min && xMax >= max)
      return Some(new Tuple2[Int, Int](xMin, max))
    else
      None

  }

  //  //由经纬度计算瓦片编号
  //  // zoom:瓦片层级 lon:经度 lat:纬度
  def getTileNumber(z: Int, lonLat: LonLat): util.ArrayList[tile] = {
    //贵州：minLon:103,maxLon:110,minLat:24,maxLat:30
    //全国：minLon:72,maxLon:137,minLat:18,maxLat:54

    //(137,54)
    //    val tile1Y = (1 - (log(tan(toRadians(30)) + 1/cos(toRadians(30))) / Pi)) / 2.0 * (1 << z)
    //    val tile1 = new TileNumber(((110 + 180.0) / 360.0 * (1 << z)).toInt,
    //      tile1Y.toInt,
    //      z)

    //(72,54)
    val tile2 = new TileNumber(
      ((lonLat.maxLon + 180.0) / 360.0 * (1 << z)).toInt,
      ((1 - log(tan(toRadians(lonLat.minLat)) + 1 / cos(toRadians(lonLat.minLat))) / Pi) / 2.0 * (1 << z)).toInt,
      z)

    //    //(72,18)
    //    val tile3 = new TileNumber(
    //      ((103 + 180.0) / 360.0 * (1 << z)).toInt,
    //      ((1 - log(tan(toRadians(24)) + 1 / cos(toRadians(24))) / Pi) / 2.0 * (1 << z)).toInt,
    //      z)

    //(137,18)
    val tile4 = new TileNumber(
      ((lonLat.minLon + 180.0) / 360.0 * (1 << z)).toInt,
      ((1 - log(tan(toRadians(lonLat.maxLat)) + 1 / cos(toRadians(lonLat.maxLat))) / Pi) / 2.0 * (1 << z)).toInt,
      z)

    //println(s"1=" + tile1.toString + "  2=" + tile2.toString + "  3=" + tile3.toString + "  4=" + tile4.toString)

    println(s"2=" + tile2.toString +  "  4=" + tile4.toString )

    tile(tile4.x, tile4.y, tile2.x, tile2.y)

    //获取layerJson中的范围
    val layerJson = readLayerJson
    val tiles = layerJson(z)
    var retTileList = new util.ArrayList[tile]

    for(i <- 0 to tiles.size - 1){
      val tileObj = tiles.get(i)
      val xRangeOpt = getRange(tileObj.startX, tileObj.endX, tile4.x, tile2.x)
      val yRangeOpt = getRange(tileObj.startY, tileObj.endY, tile4.y, tile2.y)

      if((!xRangeOpt.isEmpty) && (!yRangeOpt.isEmpty)) {
        val xRange = xRangeOpt.get
        val yRange = yRangeOpt.get
        val addTile = tile(xRange._1, yRange._1, xRange._2, yRange._2)
        println(s"" + addTile.toString)
        retTileList.add(addTile)
      }
    }

    retTileList
  }

  def readLayerJson():ArrayBuffer[util.ArrayList[tile]] = {

    val f = Source.fromFile("layer.json")
    val line = f.getLines().next
    val layer = JSON.parse(line).asInstanceOf[JSONObject]
    val availableArray = layer.getJSONArray("available")

    var retArray = new ArrayBuffer[util.ArrayList[tile]]

    for(i <- 0 to availableArray.size - 1) {
      val zoomArray = availableArray.get(i)
      retArray += JSON.parseArray(zoomArray.toString, classOf[tile]).asInstanceOf[util.ArrayList[tile]]
    }

    retArray
  }

  //启动本地路由器
  val router = {
    var workerNum = 32
    context.actorOf(RandomPool(workerNum).props(Props[DownloadTileRoutee]))
  }

  def readLogFile(filePath:String): Unit = {

    //从文件获取经纬度信息
    val logFile = new File(filePath)
    val fileReader = new BufferedRandomAccessFile(logFile, "r")

    //Cesium.js:452 GET http://172.22.128.221:8181/3d/terrainTiles/11/3238/1318.terrain?v=1.31376.0 404 (Not Found)
    val pat =  """[0-9]+/[0-9]+/[0-9]+""".r

    var readFin = 0
    while(readFin == 0) {
      val oneLine = fileReader.readOneLine()
      if(oneLine == null) {
        println("读文件完成")
        readFin = 1
      } else {
        val xyzOpt = pat.findFirstIn(oneLine)
        if(!xyzOpt.isEmpty) {
          val zxy = xyzOpt.get.split("/")
          println(s"x=${zxy(1).toInt}, y=${zxy(2).toInt}, z=${zxy(0).toInt}")
          router ! TileInfo(zxy(1).toInt, zxy(2).toInt, zxy(0).toInt)
        }
      }
    }

    fileReader.close()
  }

  //model: 0->取全球 1->取指定经纬度
  def getData(model:Int, z:Int, lonLat:LonLat):Unit = {
    println(s"-------------------------$z-------------------------")

    if (model == 0) {
      val layerJson = readLayerJson

      //获取世界的
      val tileNumberRangeList = layerJson(z)
      for (i <- 0 to tileNumberRangeList.size - 1) {
        val tileNumberRange = tileNumberRangeList.get(i)
        for (i <- tileNumberRange.startX to tileNumberRange.endX) {
          for (j <- tileNumberRange.startY to tileNumberRange.endY) {
            router ! TileInfo(i, j, z)
          }
        }
      }

      ///////////////////////////////////////////////////
//      if(tileNumberRangeList.size() == 0) {
//        val tile2 = new TileNumber(
//          ((lonLat.maxLon + 180.0) / 360.0 * (1 << z)).toInt,
//          ((1 - log(tan(toRadians(lonLat.minLat)) + 1 / cos(toRadians(lonLat.minLat))) / Pi) / 2.0 * (1 << z)).toInt,
//          z)
//        val tile4 = new TileNumber(
//          ((lonLat.minLon + 180.0) / 360.0 * (1 << z)).toInt,
//          ((1 - log(tan(toRadians(lonLat.maxLat)) + 1 / cos(toRadians(lonLat.maxLat))) / Pi) / 2.0 * (1 << z)).toInt,
//          z)
//
//        println(s"2=" + tile2.toString +  "  4=" + tile4.toString )
//
//        tile(tile4.x, tile4.y, tile2.x, tile2.y)
//
//        for (i <- tile4.x to tile2.x) {
//          for (j <- tile4.y to tile2.y) {
//            router ! TileInfo(i, j, z)
//          }
//        }
//      }
    } else if (model == 1) {
      //获取指定经纬度的
      //      val tileNumberRange = getTileNumber(z, lonLat)
      //      println(s"tileNumberRange=" + tileNumberRange.toString())
      //      for (i <- tileNumberRange.startX to tileNumberRange.endX) {
      //        for (j <- tileNumberRange.startY to tileNumberRange.endY) {
      //          router ! TileInfo(i, j, z)
      //        }
      //      }

      val tileNumberRangeList = getTileNumber(z, lonLat)
      for (i <- 0 to tileNumberRangeList.size - 1) {
        val tileNumberRange = tileNumberRangeList.get(i)
        for (i <- tileNumberRange.startX to tileNumberRange.endX) {
          for (j <- tileNumberRange.startY to tileNumberRange.endY) {
            router ! TileInfo(i, j, z)
          }
        }
      }

      ///////////////////////////////////////////////////
//      if(tileNumberRangeList.size() == 0) {
//        val tile2 = new TileNumber(
//          ((lonLat.maxLon + 180.0) / 360.0 * (1 << z)).toInt,
//          ((1 - log(tan(toRadians(lonLat.minLat)) + 1 / cos(toRadians(lonLat.minLat))) / Pi) / 2.0 * (1 << z)).toInt,
//          z)
//        val tile4 = new TileNumber(
//          ((lonLat.minLon + 180.0) / 360.0 * (1 << z)).toInt,
//          ((1 - log(tan(toRadians(lonLat.maxLat)) + 1 / cos(toRadians(lonLat.maxLat))) / Pi) / 2.0 * (1 << z)).toInt,
//          z)
//
//        println(s"2=" + tile2.toString +  "  4=" + tile4.toString )
//
//        tile(tile4.x, tile4.y, tile2.x, tile2.y)
//
//        for (i <- tile4.x to tile2.x) {
//          for (j <- tile4.y to tile2.y) {
//            router ! TileInfo(i, j, z)
//          }
//        }
//      }

    } else if (model == 2) {
      //readLogFile("G:\\akkaTest\\testNet\\localhost-1534918802783.log")
      readLogFile("G:\\akkaTest\\testNet\\1545877001822.log")
    }

    router ! Broadcast(TileFinished)
  }

  var finCount = 0;
  override def receive = {
    case GetDateMsg(model, zoom, lonLat) =>
      getData(model, zoom, lonLat)

    case successCount:Int =>
      finCount = finCount + successCount
      println(s"finished:" + finCount)

      if(finCount == 32)
        context.stop(self)
  }
}

object GetTiles {
  //val zoomMax = Array(1,2)

  def main(args:Array[String]):Unit = {

    val actorSystem = ActorSystem.create("getTiles")
    val downLoadActor = actorSystem.actorOf(Props[Download])

    //    while(true) {
    //      println(s"please input zoom:")
    //      val zoom = StdIn.readLine()
    //
    //      downLoadActor ! GetDateMsg(zoom.toInt)
    //    }
    //downLoadActor ! GetDateMsg(args(0).toInt, args(1).toInt)

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    //贵州：minLon:103,maxLon:110,minLat:24,maxLat:30
    //全国：minLon:72,maxLon:137,minLat:18,maxLat:54
    //        if(args(0).toInt == 0) { //全国
    //          downLoadActor ! GetDateMsg(args(0).toInt, args(1).toInt, null)
    //        } else {
    //          if(args.size < 6)
    //            println(s"parameter error!")
    //          else {
    //            val lonlat = LonLat(args(2).toDouble, args(3).toDouble, args(4).toDouble, args(5).toDouble)
    //            downLoadActor ! GetDateMsg(args(0).toInt, args(1).toInt, lonlat)
    //          }
    //        }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    val lonlat = LonLat(103, 110, 24, 30) //贵州
    //val lonlat = LonLat(106.7, 107, 26.9, 27.4) //小地方
    //val lonlat = LonLat(106, 107, 26, 28) //小地方
    //val lonlat = LonLat(72, 137, 18, 54) //全国
    //downLoadActor ! GetDateMsg(0, 10, null)
    //downLoadActor ! GetDateMsg(1, 14, lonlat)
    downLoadActor ! GetDateMsg(2, 14, lonlat) //第一个参数0：获取全国的 1：获取贵州的 2：从文件解析经纬度坐标



  }

}


