package bench;

public class TimedTask implements Runnable {

    private final Runnable inner;
    private long startTime;
    private long endTime;

    public TimedTask(Runnable inner) {
        this.inner = inner;
    }

    @Override
    public void run() {
        startTime = System.nanoTime();
        inner.run();
        endTime = System.nanoTime();
    }

    public long getLatencyNanos() {
        return endTime - startTime;
    }
}
