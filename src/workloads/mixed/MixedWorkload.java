package workloads.mixed;

import workloads.TaskFactoryWorkload;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

// meeting4: mixed workload combining CPU computation and simulated IO (sleep)
// represents a more realistic server-like task that does both compute and waiting
// each task sleeps for a short time (simulating IO wait) then does CPU work
public class MixedWorkload implements TaskFactoryWorkload {

    private final int sleepMillis;
    private final int workIters;
    private final int mixIters;
    private final LongAdder sink = new LongAdder(); // prevent JIT dead-code elimination

    public MixedWorkload(int sleepMillis, int workIters, int mixIters) {
        if (sleepMillis < 0) throw new IllegalArgumentException("sleepMillis must be >= 0");
        if (workIters < 0) throw new IllegalArgumentException("workIters must be >= 0");
        if (mixIters < 0) throw new IllegalArgumentException("mixIters must be >= 0");
        this.sleepMillis = sleepMillis;
        this.workIters = workIters;
        this.mixIters = mixIters;
    }

    @Override
    public String name() {
        return "mixed-sleep" + sleepMillis + "ms-cpu" + workIters + "x" + mixIters;
    }

    @Override
    public Runnable newTask(int taskId) {
        long seed = ThreadLocalRandom.current().nextLong();
        return () -> {
            // simulate IO wait
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // CPU work after IO wait, same xorshift loop as CpuBusyWorkload
            long x = seed;
            long acc = 0;
            for (int i1 = 0; i1 < workIters; i1++) {
                for (int i2 = 0; i2 < mixIters; i2++) {
                    x ^= (x << 13);
                    x ^= (x >>> 7);
                    x ^= (x << 17);
                    acc += x;
                }
            }
            sink.add(acc);
        };
    }

    public long sinkValue() {
        return sink.sum();
    }
}