package com;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyTomcat {
    private Map<String, Context> contextMap = new HashMap<>();
    private int port = 8080;
//    public static void main(String[] args) {
//        MyTomcat tomcat = new MyTomcat();
//        tomcat.deployApps();
//        tomcat.start();
//    }
    public MyTomcat() {

    }
    public MyTomcat(int port) {
        this.port = port;
    }
    public String findWebappsParentURL() {
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");
        File root = new File(currentDir);

        // BFS 队列
        Queue<File> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            File current = queue.poll();

            // 如果当前是目录，检查是否包含 webapps
            if (current.isDirectory()) {
                File[] files = current.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().equals("webapps")) {
                            // 找到 webapps，返回其父目录的 URL
                            return current.toString();
                        }
                        // 将子目录加入队列
                        queue.offer(file);
                    }
                }
            }
        }
        // 如果没有找到 webapps，返回 null 或抛出异常
        return null;
    }
    public void deployApps(String baseUrl ) {
        // 部署应用
//        System.out.println();
//        String baseUrl = findWebappsParentURL();
        File webapps = new File(baseUrl);
        System.out.println("webappsURL: " + webapps.getPath());
        for (String app : webapps.list()) {
            deployApp(webapps, app);
        }
    }

    private void deployApp(File webapps, String appName) {
        // 有哪些Servlet
        Context context = new Context(appName);

        // 部署应用
        System.out.println("Deploy app: " + appName);
        File appDirectory = new File(webapps, appName);
        File classDirectory = new File(appDirectory, "classes");

        List<File> allFilePath = getAllFilePath(classDirectory);
        for (File file : allFilePath) {
            // 获得类加载器
            String name = file.getPath();
            name = name.replace(classDirectory.getPath() + "\\", "");
            name = name.replace(".class", "");
            name = name.replace("\\", ".");
            // 读取文件
            try {
                WebappClassLoader classLoader = new WebappClassLoader(new URL[]{classDirectory.toURL()});
                Class<?> servletClass = classLoader.loadClass(name);
                if (HttpServlet.class.isAssignableFrom(servletClass)) {
                    // 判断servletClass是不是继承了HttpServlet
                    if (servletClass.isAnnotationPresent(WebServlet.class)) {
                        WebServlet annotation = servletClass.getAnnotation(WebServlet.class);
                        String[] urlPatterns = annotation.urlPatterns();

                        for (String urlPattern : urlPatterns) {
                            context.addUrlPatternMapping(urlPattern, (Servlet) servletClass.newInstance());
                        }
                    }
//                    HttpServlet servlet = (HttpServlet) servletClass.newInstance();
//                    System.out.println(servlet);
                }
            } catch (ClassNotFoundException | MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        contextMap.put(appName, context);
    }
    public List<File> getAllFilePath(File srcFile) {
        List<File> result = new ArrayList<>();
        File[] files = srcFile.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    result.addAll(getAllFilePath(file));
                } else {
                    result.add(file);
                }
            }
        }
        return result;
    }
    public void start() {
        // Socket 连接 TCP
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            ExecutorService executorService = Executors.newFixedThreadPool(20);
            while (true) {
                Socket socket = serverSocket.accept();
                executorService.execute(new SocketProcessor(socket, this));
                // InputStream inputStream = socket.getInputStream();
                // socket.getOutputStream().write();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Context> getContextMap() {
        return contextMap;
    }
}
