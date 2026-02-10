package models;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class FixedThreadPoolModel implements ConcurrencyModel {

    private final int numThreads;

    public FixedThreadPoolModel(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public String name() {
        return "fixed-pool-" + numThreads;
    }

    @Override
    public void runAll(List<Runnable> tasks) throws InterruptedException {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        CountDownLatch done = new CountDownLatch(tasks.size());

        // wrap tasks so we can count completion
        for (Runnable task : tasks) {
            queue.add(() -> {
                try {
                    task.run();
                } finally {
                    done.countDown();
                }
            });
        }

        Thread[] workers = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Thread(() -> {
                try {
                    while (true) {
                        Runnable task = queue.poll();
                        if (task == null) break;
                        task.run();
                    }
                } catch (Exception ignored) {
                }
            });
            workers[i].start();
        }

        // wait for all tasks
        done.await();

        // wait for workers to exit
        for (Thread t : workers) {
            t.join();
        }
    }
}
