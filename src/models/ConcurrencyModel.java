package models;

import java.util.List;

public interface ConcurrencyModel {
    String name();
    void runAll(List<Runnable> tasks) throws InterruptedException;
}
