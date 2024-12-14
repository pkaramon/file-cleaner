package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.task.BackgroundTask;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class FileListViewController {

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

    public FileListViewController(FileService fileService) {
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
    @FXML
    public void onFindDuplicatesClicked() {
        Map<Long, List<File>> duplicates = fileService.findDuplicates();

        if (duplicates.isEmpty()) {
            // Jeśli nie znaleziono duplikatów
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Duplicates");
            alert.setHeaderText("No Duplicates Found");
            alert.setContentText("No duplicate files were found.");
            alert.showAndWait();
        } else {
            // Jeśli duplikaty zostały znalezione, wyświetl je
            duplicates.forEach((size, files) -> {
                System.out.println("Size: " + size);
                files.forEach(file -> System.out.println("  - " + file.getPath()));
            });

            // Zaktualizuj TableView (np. fileTableView)
            List<FileRow> duplicateFileRows = duplicates.values().stream()
                    .flatMap(List::stream)
                    .map(file -> new FileRow(file.getPath(), String.valueOf(file.getSize())))
                    .collect(Collectors.toList());

            fileTableView.getItems().setAll(duplicateFileRows);
        }
    }
    @FXML
    public void onDeleteDuplicatesClicked() {
        // Znajdź duplikaty
        Map<Long, List<File>> duplicates = fileService.findDuplicates();

        if (duplicates.isEmpty()) {
            // Jeśli brak duplikatów
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Duplicates");
            alert.setHeaderText("No Duplicates Found");
            alert.setContentText("No duplicate files were found.");
            alert.showAndWait();
        } else {
            // Jeśli duplikaty zostały znalezione, wywołaj metodę do ich usunięcia
            fileService.deleteDuplicates(duplicates);

            // Wyświetl komunikat o zakończeniu operacji
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Duplicates Deleted");
            alert.setHeaderText("Duplicates Removed");
            alert.setContentText("Duplicate files have been deleted.");
            alert.showAndWait();

            // Odśwież widok, jeśli to potrzebne
            onFindDuplicatesClicked(); // Ponowne wywołanie, by zaktualizować widok
        }
    }

    @FXML
    public void onArchiveDuplicatesClicked() {
        // Znajdź duplikaty
        Map<Long, List<File>> duplicates = fileService.findDuplicates();

        if (duplicates.isEmpty()) {
            // Jeśli brak duplikatów
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Duplicates");
            alert.setHeaderText("No Duplicates Found");
            alert.setContentText("No duplicate files were found.");
            alert.showAndWait();
        } else {
            // Użyj DirectoryChooser, aby wybrać katalog docelowy
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Directory for Archive");
            java.io.File selectedDirectory = directoryChooser.showDialog(null);

            if (selectedDirectory != null) {
                try {
                    // Wywołaj metodę archiwizującą
                    fileService.archiveDuplicates(duplicates, selectedDirectory);

                    // Wyświetl komunikat o sukcesie
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Duplicates Archived");
                    alert.setHeaderText("Duplicates Archived Successfully");
                    alert.setContentText("Duplicate files have been archived to: " + selectedDirectory.getAbsolutePath());
                    alert.showAndWait();

                    // Odśwież widok, jeśli to potrzebne
                    onFindDuplicatesClicked(); // Zaktualizuj widok po operacji
                } catch (IOException e) {
                    // Obsłuż błędy związane z archiwizacją
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error Archiving Files");
                    alert.setContentText("An error occurred while archiving the files: " + e.getMessage());
                    alert.showAndWait();
                }
            } else {
                // Użytkownik anulował wybór katalogu
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Directory Selected");
                alert.setHeaderText("No Directory Selected");
                alert.setContentText("Please select a directory to save the archive.");
                alert.showAndWait();
            }
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

    private void updateTable(java.util.List<File> files) {
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
