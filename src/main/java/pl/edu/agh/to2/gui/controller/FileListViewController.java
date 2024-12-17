package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.task.TaskExecutor;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class FileListViewController {
    private final SpringFXMLLoader loader;
    private final FileService fileService;
    private TaskExecutor taskExecutor;

    @FXML
    private Pane rootPane;
    @FXML
    private TableView<FileRow> fileTableView;
    @FXML
    private TableColumn<FileRow, String> pathColumn;
    @FXML
    private TableColumn<FileRow, String> sizeColumn;
    @FXML
    private TableColumn<FileRow, String> hashColumn;

    private String directoryPath;

    public FileListViewController(SpringFXMLLoader loader, FileService fileService) {
        this.loader = loader;
        this.fileService = fileService;
    }

    @FXML
    public void initialize() {
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        hashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        fileTableView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        taskExecutor = new TaskExecutor(rootPane);
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
        taskExecutor.run(() -> fileService.findLargestFilesIn(directoryPath, n), this::updateTable);
    }

    @FXML
    private void onDeleteClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();
        var selectedRowsCopy = FXCollections.observableArrayList(selectedRows);

        if (!selectedRowsCopy.isEmpty()) {
            taskExecutor.run(() -> {
                        selectedRowsCopy.forEach(row -> fileService.deleteFile(row.getPath()));
                        return fileService.findFilesInPath(directoryPath);
                    },
                    this::updateTable
            );
        }
    }

    @FXML
    public void onFindDuplicatesClicked() {
        List<List<File>> duplicates = fileService.findDuplicatedGroups();

        if (duplicates.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Duplicates");
            alert.setHeaderText("No Duplicates Found");
            alert.setContentText("No duplicate files were found.");
            alert.showAndWait();
        } else {
            List<File> duplicateFiles = duplicates.stream()
                    .flatMap(List::stream)
                    .toList();
            updateTable(duplicateFiles);
        }
    }

    @FXML
    public void onDeleteDuplicatesClicked() {
//        // Znajdź duplikaty
        List<List<File>> duplicates = fileService.findDuplicatedGroups();

        if (duplicates.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Duplicates");
            alert.setHeaderText("No Duplicates Found");
            alert.setContentText("No duplicate files were found.");
            alert.showAndWait();
        } else {
//            // Jeśli duplikaty zostały znalezione, wywołaj metodę do ich usunięcia
//            fileService.deleteDuplicates(duplicates);
//
//            // Wyświetl komunikat o zakończeniu operacji
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("Duplicates Deleted");
//            alert.setHeaderText("Duplicates Removed");
//            alert.setContentText("Duplicate files have been deleted.");
//            alert.showAndWait();
//
//            // Odśwież widok, jeśli to potrzebne
//            onFindDuplicatesClicked(); // Ponowne wywołanie, by zaktualizować widok
        }
    }

    @FXML
    public void onArchiveDuplicatesClicked() {
//        // Znajdź duplikaty
//        Map<Long, List<File>> duplicates = fileService.findDuplicatedGroups();
//
//        if (duplicates.isEmpty()) {
//            // Jeśli brak duplikatów
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setTitle("No Duplicates");
//            alert.setHeaderText("No Duplicates Found");
//            alert.setContentText("No duplicate files were found.");
//            alert.showAndWait();
//        } else {
//            // Użyj DirectoryChooser, aby wybrać katalog docelowy
//            DirectoryChooser directoryChooser = new DirectoryChooser();
//            directoryChooser.setTitle("Select Directory for Archive");
//            java.io.File selectedDirectory = directoryChooser.showDialog(null);
//
//            if (selectedDirectory != null) {
//                try {
//                    // Wywołaj metodę archiwizującą
//                    fileService.archiveDuplicates(duplicates, selectedDirectory);
//
//                    // Wyświetl komunikat o sukcesie
//                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
//                    alert.setTitle("Duplicates Archived");
//                    alert.setHeaderText("Duplicates Archived Successfully");
//                    alert.setContentText("Duplicate files have been archived to: " + selectedDirectory.getAbsolutePath());
//                    alert.showAndWait();
//
//                    // Odśwież widok, jeśli to potrzebne
//                    onFindDuplicatesClicked(); // Zaktualizuj widok po operacji
//                } catch (IOException e) {
//                    // Obsłuż błędy związane z archiwizacją
//                    Alert alert = new Alert(Alert.AlertType.ERROR);
//                    alert.setTitle("Error");
//                    alert.setHeaderText("Error Archiving Files");
//                    alert.setContentText("An error occurred while archiving the files: " + e.getMessage());
//                    alert.showAndWait();
//                }
//            } else {
//                // Użytkownik anulował wybór katalogu
//                Alert alert = new Alert(Alert.AlertType.WARNING);
//                alert.setTitle("No Directory Selected");
//                alert.setHeaderText("No Directory Selected");
//                alert.setContentText("Please select a directory to save the archive.");
//                alert.showAndWait();
//            }
//        }
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
        taskExecutor.run(() -> {
                    fileService.loadFromPath(directoryPath, Pattern.compile(".*"));
                    return fileService.findFilesInPath(directoryPath);
                },
                this::updateTable
        );
    }

    private void loadAllFiles() {
        taskExecutor.run(() -> fileService.findFilesInPath(directoryPath), this::updateTable);
    }

    private void updateTable(List<File> files) {
        ObservableList<FileRow> rows = FXCollections.observableArrayList();
        files.forEach(file -> rows.add(new FileRow(file.getPath(), file.getSize() + " bytes", file.getHash())));
        fileTableView.setItems(rows);
    }


    public static class FileRow {
        private final String path;
        private final String size;
        private final String hash;

        public FileRow(String path, String size, String hash) {
            this.path = path;
            this.size = size;
            this.hash = hash;
        }

        public String getPath() {
            return path;
        }

        public String getSize() {
            return size;
        }

        public String getHash() {
            return hash;
        }
    }
}
