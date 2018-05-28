package clickhouse;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

public class ClickhouseRestClientApi {

    public static void sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);

            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            // 提交模式
            conn.setRequestMethod("GET");// POST GET PUT DELETE
            conn.setRequestProperty("Authorization", "Basic YWRtaW46YWRtaW4=");//YWRtaW46YWRtaW4=");
            // 设置访问提交模式，表单提交
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);// 连接超时 单位毫秒
            conn.setReadTimeout(15000);// 读取超时 单位毫秒
            //读取请求返回值
//       conn.setDoOutput(true);// 是否输入参数
//
//       StringBuffer params = new StringBuffer();
//       // 表单参数与get形式一样
//       params.append("f292139625cd4d59fcff42360ce11fc");
//       byte[] bypes = params.toString().getBytes();
//       conn.getOutputStream().write(bypes);// 输入参数
            byte bytes[] = new byte[1024];
            InputStream inStream = conn.getInputStream();
            inStream.read(bytes, 0, inStream.available());
            System.out.println(new String(bytes, "utf-8"));


//            // 打开和URL之间的连接
//            URLConnection connection = realUrl.openConnection();
//            // 设置通用的请求属性
//            connection.setRequestProperty("accept", "*/*");
//            connection.setRequestProperty("connection", "Keep-Alive");
//            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
//            // 建立实际的连接
//            connection.connect();
//            // 获取所有响应头字段
//            Map<String, List<String>> map = connection.getHeaderFields();
//            // 遍历所有的响应头字段
//            for (String key : map.keySet()) {
//                System.out.println(key + "--->" + map.get(key));
//            }
//            // 定义 BufferedReader输入流来读取URL的响应
//            in = new BufferedReader(new InputStreamReader(
//                    connection.getInputStream()));
//            String line;
//            while ((line = in.readLine()) != null) {
//                result += line;
//            }
//        } catch (Exception e) {
//            System.out.println("发送GET请求出现异常！" + e);
//            e.printStackTrace();
//        }
//        // 使用finally块来关闭输入流
//        finally {
//            try {
//                if (in != null) {
//                    in.close();
//                }
//            } catch (Exception e2) {
//                e2.printStackTrace();
//            }
//        }
//        return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        sendGet("http://192.168.10.173:8628/", "query=SELECT * from F_PI_PW_HL_6Y_I limit 5");
    }


}
