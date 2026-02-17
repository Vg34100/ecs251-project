package bench;

import models.ConcurrencyModel;

import java.util.ArrayList;
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

        // Calculate per-task latency
        long totalLatencyNanos = 0;
        for (TimedTask t : timedTasks) {
            totalLatencyNanos += t.getLatencyNanos();
        }

        double totalSeconds = (end - start) / 1_000_000_000.0;

        // convert nanos -> micros for readability
        double avgLatencyMicros = timedTasks.isEmpty()
                ? 0.0
                : (totalLatencyNanos / (double) timedTasks.size()) / 1_000.0;

        return new RunResult(
                model.name(),
                timedTasks.size(),
                totalSeconds,
                avgLatencyMicros
        );
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
                base.avgLatencyMicros
        );
    }
}
