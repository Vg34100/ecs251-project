package models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/*
* coroutine-based model using Java 21 virtual threads (Project Loom)
* virtual threads are lightweight and managed by the JVM scheduler,
* not the OS -- each task gets its own virtual thread, so blocking
* (e.g. IO waits) yields the carrier thread instead of blocking it
* this is the closest thing to coroutines we have on the JVM
*/
public class VirtualThreadModel implements ConcurrencyModel {

    @Override
    public String name() {
        return "virtual-threads";
    }

    @Override
    public void runAll(List<Runnable> tasks) throws InterruptedException {
        // newVirtualThreadPerTaskExecutor: one virtual thread per task,
        // JVM mounts/unmounts them onto a small pool of carrier threads
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>(tasks.size());

        try {
            for (Runnable task : tasks) {
                futures.add(executor.submit(task));
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
            executor.shutdownNow();
        }
    }
}