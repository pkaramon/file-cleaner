package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;
import pl.edu.agh.to2.gui.task.BackgroundTask;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Component
public class FileListViewController {
    private final SpringFXMLLoader loader;
    private final FileService fileService;

    @FXML
    private BorderPane borderPane;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private TableView<FileRow> fileTableView;
    @FXML
    private TableColumn<FileRow, String> pathColumn;
    @FXML
    private TableColumn<FileRow, String> sizeColumn;

    private String directoryPath;

    public FileListViewController(SpringFXMLLoader loader, FileService fileService) {
        this.loader = loader;
        this.fileService = fileService;
    }

    @FXML
    public void initialize() {
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        fileTableView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
    }

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
        updateFileList();
    }

    @FXML
    private void onShowAllClicked() {
        loadAllFiles();
    }

    @FXML
    private void onLargestClicked() {
        int n = 10;
        runLongRunningTask(() -> fileService.findLargestFilesIn(directoryPath, n), this::updateTable);
    }

    @FXML
    private void onDeleteClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();
        var selectedRowsCopy = FXCollections.observableArrayList(selectedRows);

        if (!selectedRowsCopy.isEmpty()) {
            runLongRunningTask(() -> {
                        selectedRowsCopy.forEach(row -> fileService.deleteFile(row.getPath()));
                        return fileService.findFilesInPath(directoryPath);
                    },
                    this::updateTable
            );
        }
    }

    private <T> void runLongRunningTask(Supplier<T> supplier,
                                        Consumer<T> onSuccess) {
        progressIndicator.setVisible(true);
        borderPane.setVisible(false);
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        BackgroundTask<T> task = new BackgroundTask<>(supplier);
        task.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            borderPane.setVisible(true);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(task.getException().getMessage());
            alert.showAndWait();
            progressIndicator.setVisible(false);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        executor.shutdown();
    }

    @FXML
    private void onShowLogsClicked() {
        var res = loader.load("/fxml/ActionLogListView.fxml");
        Stage stage = new Stage();
        stage.setTitle("Action Logs");
        stage.setScene(res.scene());
        stage.initModality(Modality.APPLICATION_MODAL);
        var controller = (ActionLogListViewController) res.controller();
        controller.show();
        stage.showAndWait();
    }

    private void updateFileList() {
        runLongRunningTask(() -> {
                    fileService.loadFromPath(directoryPath, Pattern.compile(".*"));
                    return fileService.findFilesInPath(directoryPath);
                },
                this::updateTable
        );
    }

    private void loadAllFiles() {
        runLongRunningTask(() -> fileService.findFilesInPath(directoryPath),
                this::updateTable
        );
    }

    private void updateTable(List<File> files) {
        ObservableList<FileRow> rows = FXCollections.observableArrayList();
        files.forEach(file -> rows.add(new FileRow(file.getPath(), file.getSize() + " bytes")));
        fileTableView.setItems(rows);
    }


    public static class FileRow {
        private final String path;
        private final String size;

        public FileRow(String path, String size) {
            this.path = path;
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public String getSize() {
            return size;
        }
    }
}
