package bench;

import com.sun.management.OperatingSystemMXBean;
import models.ConcurrencyModel;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BenchmarkRunner {

    // meeting4: grab the OS bean once -- gives us process CPU load samples
    private static final OperatingSystemMXBean OS_BEAN =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    // Wraps tasks so we can measure per-task latency
    public static RunResult runOnce(
            ConcurrencyModel model,
            List<Runnable> tasks
    ) throws InterruptedException {

        // Wrap tasks with timing instrumentation
        List<TimedTask> timedTasks = new ArrayList<>(tasks.size());
        for (Runnable r : tasks) {
            timedTasks.add(new TimedTask(r));
        }

        // meeting4: sample CPU load in a background thread while the benchmark runs
        // getProcessCpuLoad() returns [0.0, 1.0] for this JVM process, or -1.0 if unavailable
        // we average samples taken every 50ms to get a rough utilization figure
        double[] cpuSamples = new double[1000]; // preallocate enough slots
        int[] sampleCount = {0};
        Thread cpuSampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                double load = OS_BEAN.getProcessCpuLoad();
                if (load >= 0.0 && sampleCount[0] < cpuSamples.length) {
                    cpuSamples[sampleCount[0]++] = load;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cpuSampler.setDaemon(true);
        cpuSampler.start();

        // Run benchmark (wall-clock time)
        long start = System.nanoTime();
        model.runAll(new ArrayList<>(timedTasks));
        long end = System.nanoTime();

        cpuSampler.interrupt();

        // Collect per-task latencies in nanos
        long totalLatencyNanos = 0;
        long[] latenciesNanos = new long[timedTasks.size()];
        for (int i = 0; i < timedTasks.size(); i++) {
            long l = timedTasks.get(i).getLatencyNanos();
            latenciesNanos[i] = l;
            totalLatencyNanos += l;
        }

        double totalSeconds = (end - start) / 1_000_000_000.0;

        // convert nanos -> micros for readability
        double avgLatencyMicros = timedTasks.isEmpty()
                ? 0.0
                : (totalLatencyNanos / (double) timedTasks.size()) / 1_000.0;

        // meeting4: compute p95 and p99 from sorted per-task latencies
        double p95LatencyMicros = 0.0;
        double p99LatencyMicros = 0.0;
        if (!timedTasks.isEmpty()) {
            Arrays.sort(latenciesNanos);
            p95LatencyMicros = percentile(latenciesNanos, 95) / 1_000.0;
            p99LatencyMicros = percentile(latenciesNanos, 99) / 1_000.0;
        }

        // average the CPU samples collected during the run
        double avgCpuLoad = -1.0;
        if (sampleCount[0] > 0) {
            double sum = 0.0;
            for (int i = 0; i < sampleCount[0]; i++) sum += cpuSamples[i];
            avgCpuLoad = sum / sampleCount[0];
        }

        return new RunResult(
                model.name(),
                timedTasks.size(),
                totalSeconds,
                avgLatencyMicros,
                p95LatencyMicros,
                p99LatencyMicros,
                avgCpuLoad
        );
    }

    // meeting4: nearest-rank percentile on a sorted array of nanos
    private static long percentile(long[] sorted, int pct) {
        if (sorted.length == 1) return sorted[0];
        // nearest rank: ceil(pct/100 * n), 1-indexed
        int idx = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
        idx = Math.max(0, Math.min(idx, sorted.length - 1));
        return sorted[idx];
    }

    // Overload so we can attach workload name but still reuse timing logic
    public static RunResult runOnce(
            ConcurrencyModel model,
            String workloadName,
            List<Runnable> tasks
    ) throws InterruptedException {

        RunResult base = runOnce(model, tasks);

        return new RunResult(
                model.name() + "-" + workloadName,
                base.numTasks,
                base.seconds,
                base.avgLatencyMicros,
                base.p95LatencyMicros,
                base.p99LatencyMicros,
                base.avgCpuLoad
        );
    }
}