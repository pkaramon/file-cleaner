package pl.edu.agh.to2.gui.task;

import javafx.concurrent.Task;

import java.util.function.Supplier;

public class BackgroundTask<T> extends Task<T> {
    private final Supplier<T> supplier;

    public BackgroundTask(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected T call() {
        return supplier.get();
    }
}
