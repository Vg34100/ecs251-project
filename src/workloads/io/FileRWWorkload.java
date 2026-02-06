package workloads.io;

import workloads.TaskFactoryWorkload;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileRWWorkload implements TaskFactoryWorkload {
    private final Path baseDir;
    private final int bytesPerTask;
    private final int chunkSize;

    public FileRWWorkload(Path baseDir, int bytesPerTask, int chunkSize) {
        if (bytesPerTask <= 0) throw new IllegalArgumentException("bytesPerTask must be > 0");
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be > 0");
        this.baseDir = baseDir;
        this.bytesPerTask = bytesPerTask;
        this.chunkSize = chunkSize;
    }

    @Override
    public String name() {
        return "io-file-rw-" + bytesPerTask + "B-chunk" + chunkSize;
    }

    @Override
    public Runnable newTask(int taskId) {
        return () -> {
            Path p = null;
            try {
                Files.createDirectories(baseDir);
                p = Files.createTempFile(baseDir, "io-task-" + taskId + "-", ".bin");

                byte[] buf = new byte[Math.min(chunkSize, bytesPerTask)];
                Arrays.fill(buf, (byte) 0xA5);

                // write
                int remaining = bytesPerTask;
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p))) {
                    while (remaining > 0) {
                        int n = Math.min(remaining, buf.length);
                        out.write(buf, 0, n);
                        remaining -= n;
                    }
                }

                // read
                long read = 0;
                try (InputStream in = new BufferedInputStream(Files.newInputStream(p))) {
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        read += n;
                    }
                }

                if (read != bytesPerTask) {
                    throw new IllegalStateException("expected " + bytesPerTask + " bytes, got " + read);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (p != null) {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                }
            }
        };
    }
}
