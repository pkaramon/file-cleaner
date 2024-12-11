package pl.edu.agh.to2.gui.task;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskExecutor {
    private final Pane rootPane;
    private final ProgressIndicator progressIndicator;

    // WARNING: Does not work with BorderPane
    // If you need to use BorderPane, surround it with Pane
    public TaskExecutor(Pane rootPane) {
        this.rootPane = rootPane;
        this.progressIndicator = new ProgressIndicator();
        this.rootPane.getChildren().add(this.progressIndicator);
    }


    public <T> void run(Supplier<T> supplier,
                        Consumer<T> onSuccess) {
        showProgressIndicator();

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return supplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            hideProgressIndicator();
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(event -> {
            hideProgressIndicator();
            showErrorMessage(task);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        executor.shutdown();
    }


    private void showProgressIndicator() {
        rootPane.getChildren().forEach(node -> node.setVisible(false));
        progressIndicator.setVisible(true);
        progressIndicator.setManaged(true);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        centerProgressIndicator();
    }

    private void hideProgressIndicator() {
        rootPane.getChildren().forEach(node -> node.setVisible(true));
        progressIndicator.setManaged(false);
        progressIndicator.setVisible(false);
    }


    private void centerProgressIndicator() {
        progressIndicator
                .layoutXProperty()
                .bind(rootPane
                        .widthProperty()
                        .subtract(progressIndicator.widthProperty())
                        .divide(2));


        progressIndicator
                .layoutYProperty()
                .bind(rootPane
                        .heightProperty()
                        .subtract(progressIndicator.heightProperty())
                        .divide(2));
    }

    private <T> void showErrorMessage(Task<T> task) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(task.getException().getMessage());
        alert.showAndWait();
    }
}
