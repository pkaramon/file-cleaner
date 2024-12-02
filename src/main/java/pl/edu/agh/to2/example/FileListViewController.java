package pl.edu.agh.to2.example;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.File;
import java.util.Arrays;

public class FileListViewController {

    @FXML
    private ListView<String> fileListView;

    private String directoryPath;

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
        updateFileList();
    }

    private void updateFileList() {
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                fileListView.getItems().addAll(Arrays.asList(files));
            }
        } else {
            fileListView.getItems().add("Invalid directory path");
        }
    }
}
