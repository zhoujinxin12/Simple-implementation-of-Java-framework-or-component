/**
 * @author: 16404
 * @date: 2025/2/1 10:19
 **/

public class Semaphore {
    public static void main(String[] args) {
        Programme programme = new Programme();
        new Thread(new Actor(programme)).start();
        new Thread(new Audience(programme)).start();
    }
}

/**
 * 产品是节目，由演员生产节目
 * 消费者消费节目
 * 生产者生产什么消费者就消费什么
 */
class Programme {
    // 演员表演，观众等待
    // 观众观看，演员等待
    String programmeName;  // 表演的节目
    boolean flag = true;

    // 表演
    public synchronized void action(String programmeName) {
        if (!flag) {
            // 节目未背消费就阻塞
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.programmeName = programmeName;

        System.out.println("演员表演了：" + programmeName);
        // 通知观众观看
        this.notify();
        this.flag = !this.flag;
    }

    // 观看
    public synchronized void watch() {
        if (flag) {
            // 节目在准备中
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("观众观看了：" + this.programmeName);
        // 通知演员表演
        this.notify();
        this.flag = !this.flag;
    }
}

// 消费者 -> 观众
class Audience implements Runnable {

    Programme programme;
    public Audience(Programme programme) {
        this.programme = programme;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 20; i ++ ) {
            programme.watch();
        }
    }
}

// 生产者 -> 演员
class Actor implements Runnable {
    Programme programme;
    public Actor(Programme programme) {
        this.programme = programme;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 20; i ++ ) {
            if (i % 3 == 0) {
                programme.action(i+": 来段青海摇");
            } else if (i % 3 == 1) {
                programme.action(i+": 宇将军飞踢");
            } else {
                programme.action(i+": java之父教学");
            }
        }
    }
}