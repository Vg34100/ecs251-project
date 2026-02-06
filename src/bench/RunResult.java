package bench;

public class RunResult {
    public final String model;
    public final int numTasks;
    public final double seconds;

    public RunResult(String model, int numTasks, double seconds) {
        this.model = model;
        this.numTasks = numTasks;
        this.seconds = seconds;
    }
}
