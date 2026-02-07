package bench;

import models.ConcurrencyModel;
import models.SequentialModel;
import workloads.cpu.CpuBusyWorkload;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        ConcurrencyModel model = new SequentialModel();

        RunResult r = BenchmarkRunner.runOnce(model, 1_000_000);

        int numTasks = 10_000;
        CpuBusyWorkload wl = new CpuBusyWorkload(
                1_000,   // workIters
                50       // mixIters
        );

        List<Runnable> tasks = wl.makeTasks(numTasks);

        long start = System.nanoTime();
        model.runAll(tasks);
        long end = System.nanoTime();

        double seconds = (end - start) / 1_000_000_000.0;

        System.out.println("model=" + r.model);
        System.out.println("tasks=" + r.numTasks);
        System.out.println("seconds=" + r.seconds);
    }
}
