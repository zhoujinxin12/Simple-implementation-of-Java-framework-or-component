import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: 16404
 * @date: 2025/2/4 14:36
 **/

public class Test {
    public static void main(String[] args) throws InterruptedException {
        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();
        new Thread(()->{
            lock1.lock();
            try {
                Thread.sleep(100);
                lock2.lock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock2.unlock();
                lock1.unlock();
            }
        }).start();

        new Thread(()->{
            lock2.lock();
            try {
                Thread.sleep(100);
                lock1.lock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock1.unlock();
                lock2.unlock();
            }
        }).start();
    }
}