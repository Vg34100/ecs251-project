package bench;

import models.ConcurrencyModel;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkRunner {

    public static RunResult runOnce(ConcurrencyModel model, int numTasks) throws InterruptedException {
        List<Runnable> tasks = new ArrayList<>(numTasks);

        // Placeholder tasks: intentionally do almost nothing
        for (int i = 0; i < numTasks; i++) {
            tasks.add(() -> {
                // empty placeholder
            });
        }

        long start = System.nanoTime();
        model.runAll(tasks);
        long end = System.nanoTime();

        double seconds = (end - start) / 1_000_000_000.0;
        return new RunResult(model.name(), numTasks, seconds);
    }

    public static RunResult runOnce(
            ConcurrencyModel model,
            String workloadName,
            List<Runnable> tasks
    ) throws InterruptedException {

        long start = System.nanoTime();
        model.runAll(tasks);
        long end = System.nanoTime();

        double seconds = (end - start) / 1_000_000_000.0;
        return new RunResult(model.name() + "-" + workloadName, tasks.size(), seconds);
    }
}
