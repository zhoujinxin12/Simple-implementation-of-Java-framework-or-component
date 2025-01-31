package com.Tomcat;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Response extends AbstractHttpServletResponse{
    private final byte SP = ' ';
    private final byte CR = '\r';
    private final byte LF = '\n';
    private int status = 200;
    private String message = "OK";
    private Map<String, String> headers = new HashMap<>();
    private Request request;
    private OutputStream socketOutputStream;
    private ResponseServletOutputStream responseServletOutputStream = new ResponseServletOutputStream();

    public Response() {
    }

    public Response(Request request) {
        this.request = request;
        try {
            this.socketOutputStream = request.getSocket().getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getStatus() {
        return status;
    }
    @Override
    public void setStatus(int i) {
        status = i;
    }
    @Override
    public void setStatus(int i, String s) {
        status = i;
        message = s;
    }
    @Override
    public void addHeader(String s, String s1) {
        headers.put(s, s1);
    }

    @Override
    public ResponseServletOutputStream getOutputStream() throws IOException {
        return responseServletOutputStream;
    }

    public void complete() {
        // 发送响应
        sendResponseLine();
        sendResponseHeader();
        sendResponseBody();
    }

    private void sendResponseBody() {
        try {
            socketOutputStream.write(getOutputStream().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponseHeader() {
        if (!headers.containsKey("Content-Length")) {
            headers.put("Content-Length", String.valueOf(responseServletOutputStream.getPos()));
        }
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "text/html;charset=utf-8");
        }
        try {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                socketOutputStream.write(entry.getKey().getBytes());
                socketOutputStream.write(':');
                socketOutputStream.write(entry.getValue().getBytes());
                socketOutputStream.write(CR);
                socketOutputStream.write(LF);
            }
            socketOutputStream.write(CR);
            socketOutputStream.write(LF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendResponseLine() {
        try {
            socketOutputStream.write(request.getProtocol().getBytes());
            socketOutputStream.write(SP);
            socketOutputStream.write(status);
            socketOutputStream.write(SP);
            socketOutputStream.write(message.getBytes());
            socketOutputStream.write(CR);
            socketOutputStream.write(LF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
