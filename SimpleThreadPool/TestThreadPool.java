/**
 * @author: 16404
 * @date: 2025/2/1 10:19
 **/

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TestThreadPool {
    public static void main(String[] args) {
        // 使用自定义线程池
        ThreadPool threadPool = new ThreadPool(
                2, // corePoolSize
                4, // maximumPoolSize
                1000, TimeUnit.MILLISECONDS,
                10, // queueCapacity
                (queue, task) -> System.out.println("任务被拒绝: " + task) // 拒绝策略
        );

        SynContainer synContainer = new SynContainer(10);
        threadPool.execute(new Producer(synContainer));
        threadPool.execute(new Consumer(synContainer));

        Programme programme = new Programme();
        threadPool.execute(new Actor(programme));
        threadPool.execute(new Audience(programme));

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        threadPool.shutdown();
    }
}

class BlockingQueue<T> {
    private final Deque<T> deque;
    private final ReentrantLock lock;
    private final Condition fullWaitSet;
    private final Condition emptyWaitSet;
    private final int capacity;
    private volatile boolean isShutdown = false;

    public BlockingQueue(int capacity) {
        this.deque = new ArrayDeque<>(capacity);
        this.lock = new ReentrantLock();
        this.fullWaitSet = lock.newCondition();
        this.emptyWaitSet = lock.newCondition();
        this.capacity = capacity;
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (deque.isEmpty() && !isShutdown) {
                if (nanos <= 0) return null;
                nanos = emptyWaitSet.awaitNanos(nanos);
            }
            return isShutdown ? null : deque.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (deque.isEmpty() && !isShutdown) {
                emptyWaitSet.await();
            }
            return isShutdown ? null : deque.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(T task) {
        lock.lock();
        try {
            if (isShutdown || deque.size() == capacity) return false;
            deque.addLast(task);
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void put(T task) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (deque.size() == capacity && !isShutdown) {
                fullWaitSet.await();
            }
            if (isShutdown) return;
            deque.addLast(task);
            emptyWaitSet.signal();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return deque.size();
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            isShutdown = true;
            fullWaitSet.signalAll();
            emptyWaitSet.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

class ThreadPool {
    private final BlockingQueue<Runnable> taskQueue;
    private final HashSet<Worker> workers = new HashSet<>();
    private final int corePoolSize;
    private final int maximumPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final RejectPolicy<Runnable> rejectPolicy;
    private volatile boolean isShutdown = false;

    public ThreadPool(int corePoolSize, int maximumPoolSize,
                      long keepAliveTime, TimeUnit timeUnit,
                      int queueCapacity, RejectPolicy<Runnable> rejectPolicy) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.taskQueue = new BlockingQueue<>(queueCapacity);
        this.rejectPolicy = rejectPolicy;
    }

    public void execute(Runnable task) {
        if (isShutdown) {
            rejectPolicy.reject(taskQueue, task);
            return;
        }

        synchronized (workers) {
            // 创建核心线程
            if (workers.size() < corePoolSize) {
                Worker worker = new Worker(task);
                workers.add(worker);
                worker.start();
            }
            // 尝试入队
            else if (taskQueue.offer(task)) {
                // 任务已加入队列
            }
            // 创建非核心线程
            else if (workers.size() < maximumPoolSize) {
                Worker worker = new Worker(task);
                workers.add(worker);
                worker.start();
            }
            // 执行拒绝策略
            else {
                rejectPolicy.reject(taskQueue, task);
            }
        }
    }

    public void shutdown() {
        synchronized (workers) {
            isShutdown = true;
            for (Worker worker : workers) {
                worker.interrupt();
            }
        }
        taskQueue.shutdown();
    }

    private class Worker extends Thread {
        private Runnable firstTask;

        Worker(Runnable firstTask) {
            this.firstTask = firstTask;
        }

        @Override
        public void run() {
            try {
                // 执行第一个任务
                if (firstTask != null) {
                    firstTask.run();
                    firstTask = null;
                }

                // 循环获取任务
                while (!isShutdown) {
                    Runnable task = taskQueue.poll(keepAliveTime, timeUnit);
                    if (task != null) {
                        task.run();
                    } else {
                        // 检查是否需要回收线程
                        synchronized (workers) {
                            if (workers.size() > corePoolSize) {
                                workers.remove(this);
                                break;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // 响应中断
            } finally {
                synchronized (workers) {
                    workers.remove(this);
                }
            }
        }
    }
}

interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}