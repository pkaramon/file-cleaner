package pl.edu.agh.to2.example.view;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.example.file.File;
import pl.edu.agh.to2.example.file.FileService;

import java.util.List;
import java.util.regex.Pattern;

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
        fileService.loadFromPath(directoryPath, Pattern.compile(".*"));
        List<File> files = fileService.findLargestFilesIn(directoryPath, 10);
        files.forEach(file -> fileListView.getItems().add(
                "%s (%d bytes)".formatted(file.getPath(), file.getSize())
        ));
    }
}
