package workloads.io;

import workloads.TaskFactoryWorkload;

public class SleepIoWorkload implements TaskFactoryWorkload {
    private final int sleepMillis;
    private final int cpuIterations;

    public SleepIoWorkload(int sleepMillis, int cpuIterations) {
        if (sleepMillis < 0) throw new IllegalArgumentException("sleepMillis must be >= 0");
        if (cpuIterations < 0) throw new IllegalArgumentException("cpuIterations must be >= 0");
        this.sleepMillis = sleepMillis;
        this.cpuIterations = cpuIterations;
    }

    @Override
    public String name() {
        return "io-sleep-" + sleepMillis + "ms-cpu" + cpuIterations;
    }

    @Override
    public Runnable newTask(int taskId) {
        return () -> {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            long acc = 0;
            for (int i = 0; i < cpuIterations; i++) {
                acc += (taskId + 1L) * (i + 31L);
            }
            if (acc == Long.MIN_VALUE) System.out.print("");
        };
    }
}
