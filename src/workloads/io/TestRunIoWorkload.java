package workloads.io;

import java.nio.file.Path;
import workloads.TaskFactoryWorkload;

// how to run
// rm -rf /tmp/ecs251_out
// mkdir -p /tmp/ecs251_out
// javac -d /tmp/ecs251_out $(find src -name '*.java')
// java -cp /tmp/ecs251_out workloads.io.TestRunIoWorkload

public class TestRunIoWorkload {
    public static void main(String[] args) {
        TaskFactoryWorkload sleep = new SleepIoWorkload(10, 5_000);
        TaskFactoryWorkload file = new FileRWWorkload(
                Path.of(System.getProperty("java.io.tmpdir"), "ecs251-io"),
                1_000_000,
                64 * 1024
        );

        runOnce(sleep, 50);
        runOnce(file, 10);
    }

    private static void runOnce(TaskFactoryWorkload w, int n) {
        long start = System.nanoTime();
        for (Runnable t : w.buildTasks(n)) t.run();
        long end = System.nanoTime();
        System.out.println(w.name() + " tasks=" + n + " seconds=" + (end - start) / 1e9);
    }
}
