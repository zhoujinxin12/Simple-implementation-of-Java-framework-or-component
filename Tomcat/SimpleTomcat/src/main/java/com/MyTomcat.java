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
    public String findWebappsParentURL(String baseUrl) {
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
                        if (file.isDirectory() && file.getName().equals(baseUrl)) {
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

    /**
     * 部署webapps下面的所有app
     * @param baseUrl
     */
    public void deployApps(String baseUrl) {
        // 1. 找到baseUrl文件夹（webapps）的绝对路径。
        baseUrl = findWebappsParentURL(baseUrl);
        File webapps = new File(baseUrl);
        System.out.println("webappsURL: " + webapps.getPath());
        // 2. Tomcat规定webapps存放若干个APP server
        //    遍历webapps目录下面的每个app，appName是文件夹的名字。
        for (String appName : webapps.list()) {
            // 3. 部署app。
            deployApp(webapps, appName);
        }
    }

    /**
     * 部署app
     * webapps是app所在目录的路径
     * appName是app的名字
     * @param webapps
     * @param appName
     */
    private void deployApp(File webapps, String appName) {
        // 1. 构建一个app的Context
        Context context = new Context(appName);

        System.out.println("Deploy app: " + appName);
        File appDirectory = new File(webapps, appName);
        // 2. 创建一个 webapps\appName\classes 的文件夹对象。
        //    Tomcat规定了App中的服务Servlet需要存放在classes文件夹下面。
        //    例如：webapps/myAppDemo/classes/com/SpringMVC/Handle/DispatcherServlet.class。
        File classDirectory = new File(appDirectory, "classes");

        // 3. 获得文件夹下面的所有文件。
        List<File> allFilePath = getAllFilePath(classDirectory);
        for (File file : allFilePath) {
            // 4. 获得.class 文件的全类名，为后续的类加载做准备。
            String name = file.getPath();
            name = name.replace(classDirectory.getPath() + "\\", "");
            name = name.replace(".class", "");
            name = name.replace("\\", ".");
            try {
                // 5. 获取类加载器加载类对象。
                WebappClassLoader classLoader = new WebappClassLoader(new URL[]{classDirectory.toURL()});
                Class<?> servletClass = classLoader.loadClass(name);
                // 6. 判断servletClass是不是继承了HttpServlet，同时看是否有WebServlet注解（标识这是一个Servlet）。
                if (HttpServlet.class.isAssignableFrom(servletClass)) {
                    if (servletClass.isAnnotationPresent(WebServlet.class)) {
                        WebServlet annotation = servletClass.getAnnotation(WebServlet.class);
                        String[] urlPatterns = annotation.urlPatterns();
                        // 7. @WebServlet(urlPatterns = "/zxx")，
                        //    urlPatterns存放了访问的url，它可以是一个String数组。
                        //    上面的配置可以通过 http://IP:Port/zxx 访问。
                        for (String urlPattern : urlPatterns) {
                            // 8. new一个servlet的实例对象，存入context容器中，为后续接收http请求做准备。
                            context.addUrlPatternMapping(urlPattern, (Servlet) servletClass.newInstance());
                        }
                    }
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
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            ExecutorService executorService = Executors.newFixedThreadPool(20);
            while (true) {
                Socket socket = serverSocket.accept();
                executorService.execute(new SocketProcessor(socket, this));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Context> getContextMap() {
        return contextMap;
    }
}
