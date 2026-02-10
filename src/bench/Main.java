package bench;

import models.ConcurrencyModel;
import models.FixedThreadPoolModel;
import workloads.cpu.CpuBusyWorkload;
import workloads.io.SleepIoWorkload;
import workloads.TaskFactoryWorkload;

import java.util.List;

public class Main {

    private static final int TRIALS = 3;

    public static void main(String[] args) throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();
        ConcurrencyModel model = new FixedThreadPoolModel(threads);

        // Add one more workload size, and keep a middle one
        int[] cpuTaskSizes = {1_000, 10_000, 50_000};
        int[] ioTaskSizes  = {50, 100, 200};

        System.out.println("model=" + model.name() + " threads=" + threads);

        // -------------------------
        // CPU workload: CpuBusyWorkload
        // -------------------------
        CpuBusyWorkload cpu = new CpuBusyWorkload(
                1_000,  // workIters
                50      // mixIters
        );

        for (int n : cpuTaskSizes) {
            List<Runnable> tasks = cpu.makeTasks(n);
            runTrials(model, "cpu-busy", tasks);
        }

        // -------------------------
        // IO workload: SleepIoWorkload (simulated IO)
        // -------------------------
        TaskFactoryWorkload io = new SleepIoWorkload(
                10,     // sleepMillis
                5_000   // cpuIterations
        );

        for (int n : ioTaskSizes) {
            List<Runnable> tasks = io.buildTasks(n);
            runTrials(model, io.name(), tasks);
        }
    }

    private static void runTrials(ConcurrencyModel model, String workloadName, List<Runnable> tasks)
            throws InterruptedException {

        // Optional warm-up: one run not counted. Helps a bit with JIT noise.
        BenchmarkRunner.runOnce(model, workloadName + "-warmup", tasks);

        for (int t = 1; t <= TRIALS; t++) {
            RunResult r = BenchmarkRunner.runOnce(model, workloadName, tasks);

            System.out.println(
                    model.name() + " " +
                            workloadName + " " +
                            "trial=" + t + " " +
                            "tasks=" + r.numTasks + " " +
                            "seconds=" + r.seconds
            );
        }
    }
}
