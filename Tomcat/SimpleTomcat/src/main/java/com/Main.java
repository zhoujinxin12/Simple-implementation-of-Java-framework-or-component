package com;

/**
 * @author: 16404
 * @date: 2025/2/6 20:41
 **/
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        String webapps = "webapps";
        // 1. 创建一个Tomcat实例
        MyTomcat myTomcat = new MyTomcat();
        // 2. 部署app（加载app的所有servlet到context容器中，将context容器存入contextMap中，其中key是appName）
        myTomcat.deployApps(webapps);
        // 3. 启动Tomcat。
        myTomcat.start();
    }
}