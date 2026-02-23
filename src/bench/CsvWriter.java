package bench;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvWriter {
    public static void append(String filename, RunResult r) throws IOException {
        ensureParentDirectory(filename);
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(r.model + "," + r.numTasks + "," + r.seconds + "," + r.avgLatencyMicros + "\n");
        }
    }

    public static void ensureIoRawHeader(String filename) throws IOException {
        ensureParentDirectory(filename);
        Path p = Path.of(filename);
        if (Files.exists(p) && Files.size(p) > 0) {
            return;
        }

        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write("trial,model,workload,concurrencyLevel,ioBytesPerTask,numTasks,seconds,throughputTasksPerSec,avgLatencyMicros\n");
        }
    }

    public static void appendIoRaw(
            String filename,
            int trial,
            String model,
            String workload,
            int concurrencyLevel,
            int ioBytesPerTask,
            int numTasks,
            double seconds,
            double throughputTasksPerSec,
            double avgLatencyMicros
    ) throws IOException {
        ensureParentDirectory(filename);
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(
                    trial + "," +
                    model + "," +
                    workload + "," +
                    concurrencyLevel + "," +
                    ioBytesPerTask + "," +
                    numTasks + "," +
                    seconds + "," +
                    throughputTasksPerSec + "," +
                    avgLatencyMicros + "\n"
            );
        }
    }

    private static void ensureParentDirectory(String filename) throws IOException {
        Path parent = Path.of(filename).getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
