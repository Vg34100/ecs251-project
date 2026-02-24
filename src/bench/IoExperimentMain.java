package bench;

import models.ConcurrencyModel;
import models.FixedThreadPoolModel;
import models.VirtualThreadModel;
import models.WorkStealingModel;
import workloads.TaskFactoryWorkload;
import workloads.io.FileRWWorkload;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class IoExperimentMain {

    private static final int TRIALS = 3;

    public static void main(String[] args) throws Exception {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        runIoParameterSweep(availableProcessors);
    }

    private static void runIoParameterSweep(int availableProcessors) throws Exception {
        final String outCsv = "results/results-io-raw.csv";
        final int[] concurrencyLevels = {1, 2, 4, 8};
        final int[] ioSizesBytes = {
                64 * 1024,
                256 * 1024,
                1 * 1024 * 1024,
                4 * 1024 * 1024
        };
        final int chunkSize = 128 * 1024;
        final int tasksPerConcurrencyUnit = 8;

        Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "ecs251-io-bench");
        CsvWriter.ensureIoRawHeader(outCsv);

        System.out.println("=== IO parameter sweep: model x concurrency x io-size ===");

        for (int requestedConcurrency : concurrencyLevels) {
            int effectiveConcurrency = Math.max(1, Math.min(requestedConcurrency, availableProcessors));
            int numTasks = Math.max(32, effectiveConcurrency * tasksPerConcurrencyUnit);

            ConcurrencyModel[] models = {
                    new FixedThreadPoolModel(effectiveConcurrency),
                    new WorkStealingModel(effectiveConcurrency),
                    new VirtualThreadModel()
            };

            for (int ioBytesPerTask : ioSizesBytes) {
                TaskFactoryWorkload ioWorkload = new FileRWWorkload(baseDir, ioBytesPerTask, chunkSize);
                for (ConcurrencyModel model : models) {
                    runIoTrialsAndSaveRaw(outCsv, model, ioWorkload, numTasks, requestedConcurrency, ioBytesPerTask);
                }
            }
        }
    }

    private static void runIoTrialsAndSaveRaw(
            String outCsv,
            ConcurrencyModel model,
            TaskFactoryWorkload ioWorkload,
            int numTasks,
            int concurrencyLevel,
            int ioBytesPerTask
    ) throws InterruptedException {
        List<Runnable> warmupTasks = ioWorkload.buildTasks(numTasks);
        BenchmarkRunner.runOnce(model, ioWorkload.name() + "-warmup", warmupTasks);

        for (int trial = 1; trial <= TRIALS; trial++) {
            List<Runnable> tasks = ioWorkload.buildTasks(numTasks);
            RunResult r = BenchmarkRunner.runOnce(model, ioWorkload.name(), tasks);
            double throughput = r.seconds <= 0.0 ? 0.0 : (r.numTasks / r.seconds);

            System.out.println(
                    "[io-sweep] model=" + model.name() +
                            " concurrency=" + concurrencyLevel +
                            " ioBytesPerTask=" + ioBytesPerTask +
                            " trial=" + trial +
                            " tasks=" + r.numTasks +
                            " seconds=" + r.seconds +
                            " throughput(tasks/s)=" + throughput +
                            " avgLatency(us)=" + r.avgLatencyMicros
            );

            try {
                CsvWriter.appendIoRaw(
                        outCsv,
                        trial,
                        model.name(),
                        ioWorkload.name(),
                        concurrencyLevel,
                        ioBytesPerTask,
                        r.numTasks,
                        r.seconds,
                        throughput,
                        r.avgLatencyMicros
                );
            } catch (IOException ignored) {
                // if CSV fails, keep benchmark loop running
            }
        }
    }
}
