package bench;

import models.ConcurrencyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BenchmarkRunner {

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

        // Run benchmark (wall-clock time)
        long start = System.nanoTime();
        model.runAll(new ArrayList<>(timedTasks));
        long end = System.nanoTime();

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

        return new RunResult(
                model.name(),
                timedTasks.size(),
                totalSeconds,
                avgLatencyMicros,
                p95LatencyMicros,
                p99LatencyMicros
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
                base.p99LatencyMicros
        );
    }
}