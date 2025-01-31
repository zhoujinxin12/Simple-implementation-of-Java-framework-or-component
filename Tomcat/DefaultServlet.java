package com.Tomcat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DefaultServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Execute the default doGet");
        byte[] message = "404 Not Found".getBytes();
//        // resp.setStatus(200);
//        resp.addHeader("Content-Length", String.valueOf(message.length));
//        resp.setHeader("Content-Type", "text/html;charset=utf-8");
//        resp.setHeader("Authorization", "zxx");
        resp.setStatus(404, "Not Found");
        resp.getOutputStream().write(message);

    }
}
