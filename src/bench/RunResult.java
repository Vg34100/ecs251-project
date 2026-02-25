package bench;

public class RunResult {
    public final String model;
    public final int numTasks;
    public final double seconds;
    public final double avgLatencyMicros;
    // meeting4: added percentile fields for richer latency reporting
    public final double p95LatencyMicros;
    public final double p99LatencyMicros;

    public RunResult(String model, int numTasks, double seconds, double avgLatencyMicros,
                     double p95LatencyMicros, double p99LatencyMicros) {
        this.model = model;
        this.numTasks = numTasks;
        this.seconds = seconds;
        this.avgLatencyMicros = avgLatencyMicros;
        this.p95LatencyMicros = p95LatencyMicros;
        this.p99LatencyMicros = p99LatencyMicros;
    }
}