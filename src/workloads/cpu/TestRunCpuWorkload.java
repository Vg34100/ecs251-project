package workloads.cpu;

import workloads.TaskFactoryWorkload;

public class TestRunCpuWorkload {
    public static void main(String[] args) {
        CpuBusyWorkload w = new CpuBusyWorkload(1_000, 50);

        int n = 10_000;
        long start = System.nanoTime();
        for (Runnable t : w.makeTasks(n)) t.run();
        long end = System.nanoTime();

        System.out.println("cpu-busy workIters=1000 mixIters=50 tasks=" + n + " seconds=" + (end - start) / 1e9);
    }
}
