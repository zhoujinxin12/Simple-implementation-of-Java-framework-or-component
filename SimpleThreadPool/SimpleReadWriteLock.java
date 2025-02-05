import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;

/**
 * @author: 16404
 * @date: 2025/2/3 13:31
 **/
class ReadWriteLockFairnessTest {
    private static final int THREAD_COUNT = 100;
    private static final AtomicInteger orderCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        testFairness(true);
        System.out.println("-------------------");
        testFairness(false);
    }

    private static void testFairness(boolean fair) throws InterruptedException {
        System.out.println("Testing " + (fair ? "Fair" : "Unfair") + " Lock");
        SimpleReadWriteLock lock = new SimpleReadWriteLock(fair);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    lock.writeLock().lock();
                    int order = orderCounter.incrementAndGet();
                    System.out.println("Thread " + threadId + " acquired lock, order: " + order);
                    TimeUnit.MILLISECONDS.sleep(10); // 模拟工作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.writeLock().unlock();
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // 启动所有线程
        endLatch.await(); // 等待所有线程完成
        orderCounter.set(0); // 重置计数器
    }
}


public class SimpleReadWriteLock implements ReadWriteLock {
    private final ReadLock readLock;
    private final WriteLock writeLock;
    private final Sync sync;

    public SimpleReadWriteLock() {
        this(false);
    }
    public SimpleReadWriteLock(boolean fair) {
        this.sync = fair ? new FairSync() : new NonfairSync();
        readLock = new ReadLock(this);
        writeLock = new WriteLock(this);
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    /**
     * Sync是有共享锁，多线程可以同时访问
     * 有互斥锁，线程可重入的。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        static final int SHARED_SHIFT = 16;
        static final int SHARED_UNIT = (1 << SHARED_SHIFT);
        static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
        // 一个共享之高SHARED_SHIFT位是读锁，低SHARED_SHIFT位是写锁
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        /**
         * 将共享值进行无符号的左移动（高位补0），获取c的高位
         * @param c 一个共享值，高16位是共享锁，写16位是互斥锁
         * @return 获取共享线程的数量
         */
        static int sharedCount(int c) {
            return (c >>> SHARED_SHIFT);
        }

        /**
         * 将共享值与EXCLUSIVE_MASK进行与操作，获取c的低EXCLUSIVE_MASK位
         * @param c 一个共享值，高16位是共享锁，写16位是互斥锁
         * @return 获取获得互斥锁的线程重入次数
         */
        static int exclusiveCount(int c) {
            return (c & EXCLUSIVE_MASK);
        }

        abstract boolean readerShouldBlock();
        abstract boolean writerShouldBlock();

        @Override
        protected final boolean tryAcquire(int acquires) {
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                if (w == 0 || current != getExclusiveOwnerThread()) {
                    return false;
                }
                if (w + acquires > MAX_COUNT) {
                    throw new Error("Maximum lock count exceeded");
                }
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires)) {
                return false;
            }
            setExclusiveOwnerThread(current);
            return true;
        }

        @Override
        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            int next = getState() - releases;
            boolean free = exclusiveCount(next) == 0;
            if (free) {
                setExclusiveOwnerThread(null);
            }
            setState(next);
            return free;
        }

        @Override
        protected final int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            while(true) {
                int c = getState();
                int w = exclusiveCount(c);
                if (w != 0 && getExclusiveOwnerThread() != current)
                    return -1;
                int r = sharedCount(c);
                if (readerShouldBlock() || r >= MAX_COUNT) {
                    return -1;
                }
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    return 1;
                }
            }
        }

        /**
         * tryReleaseShared 释放共享锁
         * @return 是否释放成功
         */
        @Override
        protected final boolean tryReleaseShared(int unused) {
            while(true) {
                // 一个共享值，高16位是共享锁，写16位是互斥锁
                int c = getState();
                int r = sharedCount(c);
                // 释放共享锁
                if (r == 0)
                    return false;
                int next = c - SHARED_UNIT;
                if (compareAndSetState(c, next))
                    return true;
            }
        }
        @Override
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
        final int getReadLockCount() {
            return sharedCount(getState());
        }
    }
    static final class NonfairSync extends Sync {

        @Override
        boolean readerShouldBlock() {
            // 如果有写锁在等待,则读锁应该阻塞
            return hasQueuedThreads() && getFirstQueuedThread() != Thread.currentThread();
        }
        @Override
        boolean writerShouldBlock() {
            // 非公平模式下,写锁总是尝试获取
            return false;
        }
    }

    static final class FairSync extends Sync {
        // 对于公平锁来说，如果阻塞队列中存在线程，读写操作都需要阻塞。
        @Override
        boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
        @Override
        boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    public static class ReadLock extends ILock{
        private final Sync sync;
        protected ReadLock(SimpleReadWriteLock lock) {
            this.sync = lock.sync;
        }
        public void lock() {
            sync.acquireShared(1);
        }
        public void unlock() {
            sync.releaseShared(1);
        }
    }

    public static class WriteLock extends ILock{
        private final Sync sync;
        protected WriteLock(SimpleReadWriteLock lock) {
            this.sync = lock.sync;
        }
        public void lock() {
            sync.acquire(1);
        }
        public void unlock() {
            sync.release(1);
        }
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }
    }
}

abstract class ILock implements Lock {

    @Override
    public void lock() {

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }
}