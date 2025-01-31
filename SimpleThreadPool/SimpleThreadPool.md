# 1 基本概念

**进程**是**资源分配**的**最小单位**，**线程**是**CPU调度**的**最小单位**。

# 2 线程创建

## 2.1 继承 Thread 类（重点）

1. 自定义线程类，**继承Thread类**
2. 重写run()方法，编写线程执行体
3. 在主函数中创建一个线程对象，调用start()方法开启线程。

```java
public class TestThread extends Thread {
    @Override
    public void run() {
        //run方法线程方法体
        for (int i = 0; i < 20; i++) {
            System.out.println("我在看代码----" + i);
        }
    }
    public static void main(String[] args) {
        //创建一个线程对象
        TestThread testThread = new TestThread();
		//start开启线程
        testThread.start();
        //主线程
        for (int i = 0; i < 200; i++) {
            System.out.println("我在学习多线程-----" + i);
        }
    }
}
```

## 2.2 实现Runnable接口（重点）

1. 自定义线程类，**实现Runnable接口**
2. 重写run()方法，编写线程执行体
3. 执行线程需要丢入runnable接口实现类，调用start()方法。

```java
public class TestThread2 implements Runnable {
    @Override
    public void run() {
        //run方法线程方法体
        for (int i = 0; i < 20; i++) {
            System.out.println("我在看代码----" + i);
        }
    }
    public static void main(String[] args) {
        //创建一个线程对象
        TestThread2 testThread2 = new TestThread2();
        //创建线程对象，通过线程对象来开启线程，代理
        new Thread(testThread2).start();
        //主线程
        for (int i = 0; i < 200; i++) {
            System.out.println("我在学习多线程-----" + i);
        }
    }
}
```

以上两种方式的比较：

继承 Thread 类

- 子类继承 Thread 类具备多线程能力
- 启动线程：子类对象 .start()
- 不建议使用：**避免 OOP 单继承局限性**
- 实现 Runnable 接口

**实现接口 Runnable**

- 具有多线程能力
- 启动线程：传入目标对象 + Thread对象.start()
- 推荐使用：**避免单继承局限性，方便同一个对象被多个线程使用。**

## 2.3 实现Callable接口

实现Callable接口，重写call方法。

1. **实现 Callable 接口，需要返回值类型**：class TestCallable implements Callable<Boolean>{}
2. **重写 call 方法，需要抛出异常**：public Boolean call()
3. **创建目标对象**：TestCallable testThread1 =  new TestCallable()
4. **创建执行服务**：ExecutorService service = Executor.newFixedThreadPool(4);
5. **提交执行**：Future<Boolean> result = ser.submit(testThread1);
6. **获取结果**：boolean r = result.get();
7. **关闭服务**：service.shutdownNow():

```java
public class TestCallable implements Callable<Boolean> {
    private String url;  //网络历经
    private String name;  // 保存的文件名
    public TestCallable(String url, String name) {
        this.name = name;
        this.url = url;
    }
    //下载图片线程的执行体
    @Override
    public Boolean call() {
        WebDownloader webDownloader = new WebDownloader();
        webDownloader.downloader(url, name);
        System.out.println("下载了文件名为：" + name);
        return true;
    }
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        TestCallable testThread1 = new TestCallable("https://img-blog.csdnimg.cn/20210531145950543.png", "2.png");
        TestCallable testThread2 = new TestCallable("https://img-blog.csdnimg.cn/20210531145950543.png", "3.png");
        TestCallable testThread3 = new TestCallable("https://img-blog.csdnimg.cn/20210531145950543.png", "4.png");
        TestCallable testThread4 = new TestCallable("https://img-blog.csdnimg.cn/20210531145950543.png", "5.png");
        //创建执行服务:
        ExecutorService service = Executors.newFixedThreadPool(4);
        //提交执行:
        Future<Boolean> r1 = service.submit(testThread1);
        Future<Boolean> r2 = service.submit(testThread2);
        Future<Boolean> r3 = service.submit(testThread3);
        Future<Boolean> r4 = service.submit(testThread4);
        // 获取结果:
        boolean rs1 = r1.get();
        boolean rs2 = r2.get();
        boolean rs3 = r3.get();
        boolean rs4 = r4.get();
        //关闭服务:
        service.shutdownNow();

    }
    class WebDownloader {
        //下载方法
        public void downloader(String url, String name) {
            try {
                FileUtils.copyURLToFile(new URL(url), new File(name));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IO异常，downler方法出现问题");
            }
        }
    }
}
```

## 2.4 静态代理模式

### 2.4.1 Java 中动态代理与静态代理的本质区别

#### 1. 代理类的生成时机与方式
- **静态代理**  
  ⚙️ **编译时生成**：需手动编写代理类代码，代理类与被代理类实现同一接口。  
  📝 **显式调用**：在代理类中直接硬编码调用目标对象的方法。

- **动态代理**  
  ⚡ **运行时生成**：通过 `Proxy` 类和 `InvocationHandler` 接口动态创建代理对象。  
  🧬 **反射机制**：无需手动编写代理类，由 JVM 在内存中生成字节码。

---

#### 2. 代码维护性与灵活性
| **特性**     | 静态代理                         | 动态代理                                 |
| ------------ | -------------------------------- | ---------------------------------------- |
| **代码冗余** | 高（需为每个方法重复代理逻辑）   | 低（通过 `Invoke` 方法统一处理所有调用） |
| **扩展性**   | 差（每新增一个类需创建新代理类） | 强（一个处理器可代理多个接口/类）        |
| **适用场景** | 简单场景或需要精确控制个别方法   | AOP、日志、事务等横切关注点场景          |

---

#### 3. 接口依赖与实现方式
- **静态代理**  
  🔌 **支持两种模式**：  
  - 基于接口：代理类和目标类实现同一接口  
  - 基于继承：直接继承目标类（需覆写方法，但耦合度高）

- **动态代理（JDK 原生）**  
  🚫 **仅支持接口**：通过 `Proxy.newProxyInstance()` 代理接口方法。  
  💡 **第三方扩展**：CGLIB 库可通过继承方式代理无接口的类。

---

#### 4. 性能对比
| **维度**         | 静态代理               | 动态代理                 |
| ---------------- | ---------------------- | ------------------------ |
| **方法调用速度** | 快（直接调用无反射）   | 略慢（反射调用）         |
| **内存消耗**     | 低（编译时确定类结构） | 略高（运行时生成字节码） |
| **适用场景**     | 高性能敏感场景         | 灵活性与可维护性优先场景 |

---

### 2.4.2 代码示例对比

#### 静态代理实现
```java
// 接口
interface Database {
    void query(String sql);
}
// 目标类
class MySQL implements Database {
    public void query(String sql) {
        System.out.println("Executing: " + sql);
    }
}
// 静态代理类
class LogProxy implements Database {
    private MySQL mysql;    
    public LogProxy(MySQL mysql) {
        this.mysql = mysql;
    }    
    public void query(String sql) {
        System.out.println("[LOG] Start query: " + sql); // 手动添加日志
        mysql.query(sql);
        System.out.println("[LOG] End query");
    }
}
```

#### 动态代理实现

```java
import java.lang.reflect.*;

class LogHandler implements InvocationHandler {
    private Object target;    
    public LogHandler(Object target) {
        this.target = target;
    }    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("[LOG] Start method: " + method.getName());
        Object result = method.invoke(target, args); // 统一拦截所有方法
        System.out.println("[LOG] End method");
        return result;
    }
}
// 使用动态代理
Database mysql = new MySQL();
Database proxy = (Database) Proxy.newProxyInstance(
    mysql.getClass().getClassLoader(),
    new Class[]{Database.class},
    new LogHandler(mysql)
);
proxy.query("SELECT * FROM users");
```

### 2.4.3 本质区别总结

| **核心差异**     | 静态代理                 | 动态代理                       |
| :--------------- | :----------------------- | :----------------------------- |
| **设计哲学**     | 代码即约定（显式硬编码） | 运行时可编程（动态字节码增强） |
| **实现范式**     | 面向具体实现             | 面向抽象拦截                   |
| **设计模式演进** | 代理模式的传统实现       | 反射机制 + 代理模式的结合创新  |