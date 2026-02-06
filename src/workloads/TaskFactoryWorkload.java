package workloads;

import java.util.ArrayList;
import java.util.List;

public interface TaskFactoryWorkload extends Workload {
    Runnable newTask(int taskId);

    default List<Runnable> buildTasks(int numTasks) {
        List<Runnable> tasks = new ArrayList<>(numTasks);
        for (int i = 0; i < numTasks; i++) {
            tasks.add(newTask(i));
        }
        return tasks;
    }
}
