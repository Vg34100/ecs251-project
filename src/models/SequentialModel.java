package models;

import java.util.List;

public class SequentialModel implements ConcurrencyModel {
    @Override
    public String name() {
        return "sequential";
    }

    @Override
    public void runAll(List<Runnable> tasks) {
        for (Runnable task : tasks) {
            task.run();
        }
    }
}
