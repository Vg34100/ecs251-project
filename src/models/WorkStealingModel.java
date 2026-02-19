package models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
/*
* work-stealing model using ForkJoinPool with async mode
* asyncMode=true means FIFO ordering for worker queues, which works
* better for independent tasks (vs recursive divide-and-conquer)
*/
public class WorkStealingModel implements ConcurrencyModel {

    private final int parallelism;

    public WorkStealingModel(int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be > 0");
        }
        this.parallelism = parallelism;
    }

    @Override
    public String name() {
        return "work-stealing-" + parallelism;
    }

    @Override
    public void runAll(List<Runnable> tasks) throws InterruptedException {
        // asyncMode=true: each worker uses a FIFO queue instead of LIFO
        // better for independent tasks that don't fork children
        ForkJoinPool pool = new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        List<Future<?>> futures = new ArrayList<>(tasks.size());

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