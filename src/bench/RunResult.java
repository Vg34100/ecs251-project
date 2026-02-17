package bench;

public class RunResult {
    public final String model;
    public final int numTasks;
    public final double seconds;
    public final double avgLatencyMicros;

    public RunResult(String model, int numTasks, double seconds, double avgLatencyMicros) {
        this.model = model;
        this.numTasks = numTasks;
        this.seconds = seconds;
        this.avgLatencyMicros = avgLatencyMicros;
    }
}
