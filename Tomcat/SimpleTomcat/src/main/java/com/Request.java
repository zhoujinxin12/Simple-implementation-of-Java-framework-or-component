package com;

import java.net.Socket;

public class Request extends AbstractHttpServletRequest {
    private String method;
    private String url;
    private String protocol;
    private Socket socket;

    public Request(String method, String url, String protocol, Socket socket) {
        this.method = method;
        this.url = url;
        this.protocol = protocol;
        this.socket = socket;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(url);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public Socket getSocket() {
        return socket;
    }
}
