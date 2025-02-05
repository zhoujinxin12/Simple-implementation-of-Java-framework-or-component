import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author: 16404
 * @date: 2025/2/2 12:58
 *
 * 独占锁（写锁）一次只能被一个线程占有
 * 共享锁（读锁）多个线程可以同时占有
 * 读写、写写互斥
 * 读读共享
 **/
class WritePriorityReadWriteLock implements IReadWriteLock{
    /**
     * 读次数统计
     */
    private int readCount = 0;

    /**
     * 写次数统计
     */
    private int writeCount = 0;

    /**
     * 写优先的锁，记录写请求
     */
    private int writeRequestCount = 0;
    @Override
    public synchronized void lockRead() throws InterruptedException {
        while (writeCount > 0 || writeRequestCount > 0) {
            this.wait();
        }
        readCount ++ ;
    }

    @Override
    public synchronized void unlockRead() {
        readCount --;
        notifyAll();
    }

    @Override
    public synchronized void lockWrite() throws InterruptedException {
        writeRequestCount ++ ;
        while (readCount > 0 || writeCount > 0) {
            this.wait();
        }
        writeCount ++ ;
        writeRequestCount --;
    }

    @Override
    public synchronized void unlockWrite() {
        writeCount -- ;
        notifyAll();
    }
}

class ReaderPriorityReadWriteLock implements IReadWriteLock{
    /**
     * 读次数统计
     */
    private int readCount = 0;

    /**
     * 写次数统计
     */
    private int writeCount = 0;
    @Override
    public synchronized void lockRead() throws InterruptedException {
//        while (writeCount > 0 || writeRequestCount > 0) {
        while (writeCount > 0) {
            this.wait();
        }
        readCount ++ ;
    }

    @Override
    public synchronized void unlockRead() {
        readCount --;
        notifyAll();
    }

    @Override
    public synchronized void lockWrite() throws InterruptedException {
        while (readCount > 0 || writeCount > 0) {
            this.wait();
        }
        writeCount ++ ;
    }

    @Override
    public synchronized void unlockWrite() {
        writeCount -- ;
        notifyAll();
    }
}

interface IReadWriteLock {
    /**
     * 获取读锁
     */
    void lockRead() throws InterruptedException;
    /**
     * 释放读锁
     */
    void unlockRead();
    /**
     * 获取写锁
     */
    void lockWrite() throws InterruptedException;
    /**
     * 释放写锁
     */
    void unlockWrite();
}
class MyCacheLock {
    /**
     * 自定义缓存
     */
    private volatile Map<String, Object> map = new HashMap<>();
    // 读写锁：更加细粒度的控制
    // 写优先锁
//    private IReadWriteLock lock = new WritePriorityReadWriteLock();
    // 读优先锁
    private IReadWriteLock lock = new ReaderPriorityReadWriteLock();
    // 存，写，只同时希望一个线程写
    public void put(String key, Object value) throws InterruptedException {
        lock.lockWrite();
        try {
            System.out.println((Thread.currentThread().getName() + "写入" + key));
            map.put(key, value);
            System.out.println((Thread.currentThread().getName() + "写入完毕"));
        } finally {
            lock.unlockWrite();
        }
    }
    // 取，读
    public Object get(String key) throws InterruptedException {
        lock.lockRead();
        try {
            System.out.println(Thread.currentThread().getName() + "读取" + key);
            Object value = map.get(key);
            System.out.println(Thread.currentThread().getName() + "读取完毕: 值为" + value);
            Thread.sleep(300);
            return value;
        } finally {
            lock.unlockRead();
        }
    }
}

public class MyReadWriteLock {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        MyCacheLock myCache = new MyCacheLock();
        // 写入
        for (int i = 0; i < 5; i ++ ) {
            int finalI = i;
            new Thread(()->{
                try {
                    myCache.put(finalI+"", finalI+"");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, String.valueOf(i)).start();
        }
        // 读取
        for (int i = 0; i < 5; i ++ ) {
            int finalI = i;
            new Thread(()->{
                try {
                    myCache.get(finalI+"");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, String.valueOf(i)).start();
        }
    }

}