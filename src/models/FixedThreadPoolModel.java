package models;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FixedThreadPoolModel implements ConcurrencyModel {

    private final int numThreads;

    public FixedThreadPoolModel(int numThreads) {
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads must be > 0");
        }
        this.numThreads = numThreads;
    }

    @Override
    public String name() {
        return "fixed-pool-" + numThreads;
    }

    @Override
    public void runAll(List<Runnable> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new java.util.ArrayList<>(tasks.size());

        try {
            for (Runnable task : tasks) {
                futures.add(pool.submit(task));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    for (Future<?> f : futures) {
                        f.cancel(true);
                    }
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw new RuntimeException(cause);
                }
            }
        } catch (InterruptedException e) {
            for (Future<?> f : futures) {
                f.cancel(true);
            }
            throw e;
        } finally {
            pool.shutdownNow();
        }
    }
}
