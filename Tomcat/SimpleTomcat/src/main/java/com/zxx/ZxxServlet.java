package com.zxx;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/zxx")
public class ZxxServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("ZxxServlet doGet...");
        byte[] message = "hello, this is ZxxServlet".getBytes();
        // resp.setStatus(200);
        resp.addHeader("Content-Length", String.valueOf(message.length));
        resp.setHeader("Content-Type", "text/html;charset=utf-8");
        resp.setHeader("Authorization", "zxx");
        resp.getOutputStream().write(message);
    }
}
