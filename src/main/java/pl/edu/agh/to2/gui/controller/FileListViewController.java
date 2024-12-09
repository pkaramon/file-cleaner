package pl.edu.agh.to2.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.service.FileService;
import pl.edu.agh.to2.gui.task.LoadLargestFiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
public class FileListViewController {

    private final FileService fileService;
    @FXML
    private ListView<String> fileListView;
    private String directoryPath;

    public FileListViewController(FileService fileService) {
        this.fileService = fileService;
    }

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
        updateFileList();
    }

    private void updateFileList() {
        fileListView.getItems().setAll("Loading files...");

        LoadLargestFiles task = new LoadLargestFiles(directoryPath, fileService);
        task.setOnSucceeded(event -> fileListView.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> fileListView.getItems().setAll("Error loading files."));

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(task);
        executorService.shutdown();
    }
}
