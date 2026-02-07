package workloads.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

public class CpuBusyWorkload {

    private final int workIters;
    private final int mixIters;
    private final LongAdder sink = new LongAdder(); // 防止 JIT 消掉計算

    public CpuBusyWorkload(int workIters, int mixIters) {
        this.workIters = workIters;
        this.mixIters = mixIters;
    }

    /**
     * Generate CPU-heavy tasks
     */
    public List<Runnable> makeTasks(int numTasks) {
        List<Runnable> tasks = new ArrayList<>(numTasks);

        for (int i = 0; i < numTasks; i++) {
            long seed = ThreadLocalRandom.current().nextLong();

            tasks.add(() -> {
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
            });
        }
        return tasks;
    }

    public long sinkValue() {
        return sink.sum();
    }
}
