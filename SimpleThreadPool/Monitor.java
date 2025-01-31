import java.util.LinkedList;
import java.util.Queue;

public class Monitor {
    public static void main(String[] args) {
        SynContainer synContainer = new SynContainer(10);
        new Thread(new Producer(synContainer)).start();
        new Thread(new Consumer(synContainer)).start();
    }
}

/**
 * 一个生产者生成的产品
 */
class Chicken{
    int id;

    public Chicken() {
    }

    public Chicken(int id) {
        this.id = id;
    }
}

/**
 * 容器
 */
class SynContainer {
    // 创建一个容器
    private final int size;
    private final Queue<Chicken> chickens;
    public SynContainer() {
        chickens = new LinkedList<Chicken>();
        this.size = 4;
    }

    public SynContainer(int size) {
        chickens = new LinkedList<Chicken>();
        this.size = size;
    }
    // 生产者生产的产品存入容器
    public synchronized void push(Chicken chicken) {
        // 如果容器满了就阻塞等待消费者消费
        if (chickens.size() >= size) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 如果没有满就将产品存入容器中。
        chickens.offer(chicken);
        // 如果消费者阻塞就唤醒，唤醒阻塞的消费者
        this.notifyAll();
    }

    // 消费者消费容器中的产品
    public synchronized Chicken pop() {
        // 如果容器是空的，消费者就阻塞
        if (chickens.isEmpty()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // 取出容器中的产品
        Chicken chicken = chickens.poll();
        // 唤醒所有进程包括生产者
        this.notifyAll();
        return chicken;
    }
}

/**
 * 生产者生产产品存入容器中
 */
class Producer implements Runnable {
    private final SynContainer synContainer;

    public Producer(SynContainer synContainer) {
        this.synContainer = synContainer;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i ++ ) {
            synContainer.push(new Chicken(i));
            System.out.println("生产了" + i + "只鸡" );
        }
    }
}

/**
 * 消费者消费产品
 */
class Consumer implements Runnable {
    private SynContainer synContainer;

    public Consumer(SynContainer synContainer) {
        this.synContainer = synContainer;
    }

    @Override
    public void run() {
       for (int i = 0; i < 100; i ++ ) {
           Chicken chicken = synContainer.pop();
           System.out.println("消费了-->" + chicken.id + "只鸡");
       }
    }
}
