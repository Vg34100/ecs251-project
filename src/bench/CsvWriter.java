package bench;

import java.io.FileWriter;
import java.io.IOException;

public class CsvWriter {
    public static void append(String filename, RunResult r) throws IOException {
        try (FileWriter fw = new FileWriter(filename, true)) {
            fw.write(r.model + "," + r.numTasks + "," + r.seconds + "," + r.avgLatencyMicros + "\n");
        }
    }
}
