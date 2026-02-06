package bench;

import models.ConcurrencyModel;
import models.SequentialModel;

public class Main {
    public static void main(String[] args) throws Exception {
        ConcurrencyModel model = new SequentialModel();

        RunResult r = BenchmarkRunner.runOnce(model, 1_000_000);

        System.out.println("model=" + r.model);
        System.out.println("tasks=" + r.numTasks);
        System.out.println("seconds=" + r.seconds);
    }
}
