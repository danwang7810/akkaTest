import akka.actor.Actor

import scala.collection.mutable.ArrayBuffer

case class PointPos(p:Tuple2[Float, Float])
/**
  * 回转数法判断点是否在多边形内部
  * p 待判断的点，格式：{ x: X坐标, y: Y坐标 }
  * poly 多边形顶点，数组成员的格式同 p
  * return 点 p 和多边形 poly 的几何关系
  */
class WindingNumberAlgorithm(poly: ArrayBuffer[Tuple2[Float, Float]]) extends Actor {

  def windingNumber(p: Tuple2[Float, Float]): Boolean = {

    val px = p._1
    val py = p._2
    var sum: Double = 0.0

    val l = poly.length
    var j = l - 1

    for (i <- 0 to l - 1) {
      val sx = poly(i)._1
      val sy = poly(i)._2
      val tx = poly(j)._1
      val ty = poly(j)._2

      // 点与多边形顶点重合或在多边形的边上
      if ((sx - px) * (px - tx) >= 0 && (sy - py) * (py - ty) >= 0 && (px - sx) * (ty - sy) == (py - sy) * (tx - sx)) {
        return true
      }

      // 点与相邻顶点连线的夹角
      var angle = Math.atan2(sy - py, sx - px) - Math.atan2(ty - py, tx - px)

      // 确保夹角不超出取值范围（-π 到 π）
      if (angle >= Math.PI) {
        angle = angle - Math.PI * 2
      } else if (angle <= -Math.PI) {
        angle = angle + Math.PI * 2
      }

      sum += angle

      j = i
    }

    // 计算回转数并判断点和多边形的几何关系
    if (Math.round(sum / Math.PI) == 0)
      return false
    else
      return true
  }

  override def receive = {
    case PointPos(p:Tuple2[Float, Float]) =>
      sender ! windingNumber(p)
  }

}

