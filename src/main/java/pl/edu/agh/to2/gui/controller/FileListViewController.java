package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.util.regex.Pattern;

@Component
public class FileListViewController {

    private final FileService fileService;

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
        loadAllFiles();
    }

    @FXML
    private void onShowAllClicked() {
        loadAllFiles();
    }

    @FXML
    private void onLargestClicked() {
        int n = 10;
        var largestFiles = fileService.findLargestFilesIn(directoryPath, n);
        updateTable(largestFiles);
    }

    @FXML
    private void onDeleteClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();
        if (!selectedRows.isEmpty()) {
            selectedRows.forEach(row -> fileService.deleteFile(row.getPath()));
            loadAllFiles();
        }
    }

    private void updateFileList() {
        fileService.loadFromPath(directoryPath, Pattern.compile(".*"));
    }
    private void loadAllFiles() {
        var files = fileService.findFilesInPath(directoryPath);
        updateTable(files);
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
