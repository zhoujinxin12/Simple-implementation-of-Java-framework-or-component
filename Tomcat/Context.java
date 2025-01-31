package com.Tomcat;

import javax.servlet.Servlet;
import java.util.HashMap;
import java.util.Map;

public class Context {
    //  每个应用独立的上下文
    private String appName;
    private Map<String, Servlet> urlPatternMapping = new HashMap<>();

    public void addUrlPatternMapping(String url, Servlet servlet) {
        urlPatternMapping.put(url, servlet);
    }

    public Servlet getByUrlPattern(String urlPattern) {
        if (urlPattern.charAt(0) != '/') {
            urlPattern = "/" + urlPattern;
        }
        if (!urlPatternMapping.containsKey(urlPattern)) {
            return null;
        }
        return urlPatternMapping.get(urlPattern);
    }

    public Context(String appName) {
        this.appName = appName;
    }
}
