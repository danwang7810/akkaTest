package clickhouse

import java.net.URLEncoder
import org.apache.http.HttpStatus
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.io.IOException

class SKCRestClient {

  def queryExecGet(sql:String) = {

    var targetURL = "http://192.168.10.173:8628?query=" //select count(1) from F_PI_PW_HL_6Y_I"

    targetURL += URLEncoder.encode("select * from F_PI_PW_HL_6Y_I limit 5","UTF-8")

    val httpCilent = HttpClients.createDefault
    val httpGet = new HttpGet(targetURL)//(URLEncoder.encode(targetURL,"UTF-8"))
    try {
      val response = httpCilent.execute(httpGet)
      /** 请求发送成功，并得到响应 **/
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        /** 读取服务器返回过来的json字符串数据 **/
        val strResult = EntityUtils.toString(response.getEntity())

        println(s"ret=$strResult")
      }

    } catch {
      case e: IOException =>
        e.printStackTrace()
    } finally try
      httpCilent.close() //释放资源

    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }


}

object SKCRestClient {

  def apply():SKCRestClient = {

    new SKCRestClient
  }

  def main(args:Array[String]):Unit = {
    val sKCRestClient = SKCRestClient()
    sKCRestClient.queryExecGet("")

  }
}

