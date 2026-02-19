package bench;

import models.ConcurrencyModel;
import models.FixedThreadPoolModel;
import models.VirtualThreadModel;
import models.WorkStealingModel;
import workloads.TaskFactoryWorkload;
import workloads.cpu.CpuBusyWorkload;
import workloads.io.SleepIoWorkload;

import java.io.IOException;
import java.util.List;

public class Main {

    private static final int TRIALS = 3;

    public static void main(String[] args) throws Exception {

        int threads = Runtime.getRuntime().availableProcessors();

        // meeting3: run all three models so we can compare side by side
        ConcurrencyModel[] models = {
                new FixedThreadPoolModel(threads),
                new WorkStealingModel(threads),
                new VirtualThreadModel()
        };

        int[] cpuTaskSizes = {1_000, 10_000, 50_000};
        int[] ioTaskSizes  = {50, 100, 200};

        // -------------------------
        // CPU workload
        // -------------------------
        CpuBusyWorkload cpu = new CpuBusyWorkload(
                1_000,  // workIters
                50      // mixIters
        );

        // -------------------------
        // IO workload (simulated)
        // -------------------------
        TaskFactoryWorkload io = new SleepIoWorkload(
                10,     // sleepMillis
                5_000   // cpuIterations
        );

        for (ConcurrencyModel model : models) {
            System.out.println("\n=== model=" + model.name() + " threads=" + threads + " ===");

            for (int n : cpuTaskSizes) {
                List<Runnable> tasks = cpu.makeTasks(n);
                runTrials(model, "cpu-busy", tasks);
            }

            for (int n : ioTaskSizes) {
                List<Runnable> tasks = io.buildTasks(n);
                runTrials(model, io.name(), tasks);
            }
        }
    }

    private static void runTrials(
            ConcurrencyModel model,
            String workloadName,
            List<Runnable> tasks
    ) throws InterruptedException {

        // simple warm-up run so first measurement isn't weird
        BenchmarkRunner.runOnce(model, workloadName + "-warmup", tasks);

        for (int t = 1; t <= TRIALS; t++) {

            RunResult r = BenchmarkRunner.runOnce(model, workloadName, tasks);

            System.out.println(
                    model.name() + " " +
                            workloadName + " " +
                            "trial=" + t + " " +
                            "tasks=" + r.numTasks + " " +
                            "seconds=" + r.seconds + " " +
                            "avgLatency(us)=" + r.avgLatencyMicros
            );

            // write to CSV so we can plot later if needed
            try {
                CsvWriter.append("results.csv", r);
            } catch (IOException ignored) {
                // if CSV fails, just keep running
            }
        }
    }
}