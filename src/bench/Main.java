package bench;

import java.util.List;
import models.ConcurrencyModel;
import models.SequentialModel;
import workloads.cpu.CpuBusyWorkload;

public class Main {
    public static void main(String[] args) throws Exception {
        ConcurrencyModel model = new SequentialModel();

        int numTasks = 10_000;
        CpuBusyWorkload wl = new CpuBusyWorkload(1_000, 50);
        List<Runnable> tasks = wl.makeTasks(numTasks);

        RunResult r = BenchmarkRunner.runOnce(model, tasks);

        System.out.println("model=" + r.model);
        System.out.println("tasks=" + r.numTasks);
        System.out.println("seconds=" + r.seconds);
        System.out.println("avgLatency(us)=" + r.avgLatencyMicros);

        // Optional CSV:
        CsvWriter.append("results.csv", r);
    }
}
