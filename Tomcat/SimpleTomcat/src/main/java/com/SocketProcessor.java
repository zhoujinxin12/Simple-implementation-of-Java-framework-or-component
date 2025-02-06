package com;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class SocketProcessor implements Runnable{
    private Socket socket;
    private MyTomcat tomcat;

    public SocketProcessor(Socket socket, MyTomcat tomcat) {
        this.socket = socket;
        this.tomcat = tomcat;
    }
    @Override
    public void run() {
        processSocket(socket);
    }

    private void processSocket(Socket socket) {
        // 处理 Socket 连接
        try {
            // 1. 读取socket中的输入流
            //    读取 HTTP 协议内容
            //    bytes 可能不够大，一次读取不完，后续优化需要循环读取
            //    循环读取输入流直到结束
            InputStream inputStream = socket.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytes)) != -1) {
                buffer.write(bytes, 0, bytesRead);
                if (bytesRead < bytes.length) {
                    break;
                }
            }
            byte[] requestBytes = buffer.toByteArray();

            // 2. 解析HTTP中的请求方法
            // 打印 HTTP 协议内容
            // requestBytes 内容包括请求头和请求体，请求头包括请求方法、请求路径、请求参数、请求协议等
            // 解析字节流，遇到第一个空格就推出循环
            int pos = 0;
            int begin = 0, end = 0;
            for (; pos < requestBytes.length; pos++, end++) {
                if (requestBytes[pos] == ' ') {
                    break;
                }
            }
            // 组合空格之前的字节流，转化成字符串就是请求方法（get/post）
            String method = new String(requestBytes, begin, end - begin);

            // 2. 解析HTTP请求路径
            begin = ++pos;
            end = begin;
            for (; pos < requestBytes.length; pos++, end++) {
                if (requestBytes[pos] == ' ') {
                    break;
                }
            }
            String path = new String(requestBytes, begin, end - begin);

            // 3. 解析HTTP请求协议
            begin = ++pos;
            end = begin;
            for (; pos < requestBytes.length; pos++, end++) {
                if (requestBytes[pos] == '\r') {
                    break;
                }
            }
            String protocol = new String(requestBytes, begin, end - begin);

            // 4. 初始化一个请求对象（包括方法、路径、协议和socket）和响应对象
            //    匹配Servlet，doGet，doPost
            Request request = new Request(method, path, protocol, socket);
            Response response = new Response(request);
            String requestUrl = request.getRequestURL().toString();
            System.out.printf("A request has arrived: method: %s; path: %s; protocol: %s\n", method, path, protocol);
            requestUrl = requestUrl.substring(1);
            String[] parts = requestUrl.split("/");
            if (parts.length <= 1) return;

            String appName = parts[0];
            Context context = tomcat.getContextMap().get(appName);

            Servlet servlet = context.getByUrlPattern(parts[1]);
            if (servlet == null) {
                DefaultServlet defaultServlet = new DefaultServlet();
                defaultServlet.service(request, response);
                response.complete();
//                response.setStatus(404, "Not Found");
//                response.complete();
                return;
            }
            servlet.service(request, response);

            // 5. 将响应内容写回到客户端
            response.complete();
        } catch (IOException e) {
            // 读取输入流异常
            Response response = new Response();
            response.setStatus(500, "Server Error");
            response.complete();
            throw new RuntimeException(e);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
}
