package workloads.io;

import bench.BenchmarkRunner;
import bench.RunResult;
import models.ConcurrencyModel;
import models.FixedThreadPoolModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import workloads.TaskFactoryWorkload;

// how to run
// rm -rf /tmp/ecs251_out
// mkdir -p /tmp/ecs251_out
// javac -d /tmp/ecs251_out $(find src -name '*.java')
// java -cp /tmp/ecs251_out workloads.io.TestRunIoWorkload

public class TestRunIoWorkload {
    private static final int TIMEOUT_SECONDS = 30;

    public static void main(String[] args) {
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
        ConcurrencyModel model = new FixedThreadPoolModel(threads);

        int sleepMillis = 10;
        int cpuIterations = 8_000;
        int sleepTasks = 48;
        TaskFactoryWorkload sleep = new SleepIoWorkload(sleepMillis, cpuIterations);

        Path baseDir = Path.of(System.getProperty("java.io.tmpdir"), "ecs251-io");
        int bytesPerTask = 2_000_000;
        int chunkSize = 128 * 1024;
        int fileTasks = 12;
        TaskFactoryWorkload file = new FileRWWorkload(baseDir, bytesPerTask, chunkSize);

        clearDirectory(baseDir);

        RunResult sleepResult = runWithTimeout(model, sleep, sleepTasks, TIMEOUT_SECONDS);
        verifySleepRuntime(sleepResult.seconds, threads, sleepTasks, sleepMillis);

        RunResult fileResult = runWithTimeout(model, file, fileTasks, TIMEOUT_SECONDS);
        verifyFileRuntime(fileResult.seconds);
        verifyNoLeftoverFiles(baseDir);

        System.out.println("OK: IO sanity checks passed");
    }

    private static RunResult runWithTimeout(
            ConcurrencyModel model,
            TaskFactoryWorkload workload,
            int numTasks,
            int timeoutSeconds
    ) {
        ExecutorService oneShot = Executors.newSingleThreadExecutor();
        try {
            Callable<RunResult> run = () -> {
                List<Runnable> tasks = workload.buildTasks(numTasks);
                return BenchmarkRunner.runOnce(model, workload.name(), tasks);
            };
            Future<RunResult> future = oneShot.submit(run);
            RunResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            System.out.println(
                    result.model +
                            " tasks=" + result.numTasks +
                            " seconds=" + result.seconds +
                            " avgLatency(us)=" + result.avgLatencyMicros
            );
            return result;
        } catch (TimeoutException e) {
            throw new RuntimeException("workload timeout: " + workload.name(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (Exception e) {
            throw new RuntimeException("workload execution failed: " + workload.name(), e);
        } finally {
            oneShot.shutdownNow();
        }
    }

    private static void verifySleepRuntime(double seconds, int threads, int tasks, int sleepMillis) {
        double expectedSleep = Math.ceil(tasks / (double) threads) * sleepMillis / 1000.0;
        double minSeconds = Math.max(0.05, expectedSleep * 0.4);
        double maxSeconds = Math.max(5.0, expectedSleep * 20.0);
        verifyRuntimeBounds("SleepIO", seconds, minSeconds, maxSeconds);
    }

    private static void verifyFileRuntime(double seconds) {
        verifyRuntimeBounds("FileRW", seconds, 0.01, 20.0);
    }

    private static void verifyRuntimeBounds(String name, double seconds, double minSeconds, double maxSeconds) {
        if (seconds < minSeconds) {
            throw new IllegalStateException(name + " runtime too fast: " + seconds + "s < " + minSeconds + "s");
        }
        if (seconds > maxSeconds) {
            throw new IllegalStateException(name + " runtime too slow: " + seconds + "s > " + maxSeconds + "s");
        }
    }

    private static void verifyNoLeftoverFiles(Path baseDir) {
        if (!Files.exists(baseDir)) {
            return;
        }
        try (Stream<Path> s = Files.list(baseDir)) {
            if (s.findAny().isPresent()) {
                throw new IllegalStateException("leftover files found in " + baseDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clearDirectory(baseDir);
    }

    private static void clearDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
