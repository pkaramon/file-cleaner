package pl.edu.agh.to2.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.utils.TaskExecutor;
import pl.edu.agh.to2.repository.FileSizeStats;
import pl.edu.agh.to2.service.FileService;

import java.util.Optional;

@Component
public class ReportsViewController {
    private final FileService fileService;
    private TaskExecutor taskExecutor;
    @FXML
    private Label nothingFoundLabel;

    @FXML
    private Pane rootPane;

    @FXML
    private Label averageLabel;

    @FXML
    private Label stdLabel;

    @FXML
    private Label minLabel;

    @FXML
    private Label maxLabel;

    @FXML
    private Label countLabel;

    public ReportsViewController(FileService fileService) {
        this.fileService = fileService;
    }

    @FXML
    private void initialize() {
        taskExecutor = new TaskExecutor(rootPane);
    }


    public void show() {
        taskExecutor.run(fileService::getFileSizeStats, this::displayStats);
    }

    private void displayStats(Optional<FileSizeStats> fileSizeStats) {
        if (fileSizeStats.isPresent()) {
            var stats = fileSizeStats.get();
            nothingFoundLabel.setVisible(false);
            countLabel.setText("Number of files: %s".formatted(stats.count()));
            averageLabel.setText("Average file size: %s".formatted((long) stats.average()));
            maxLabel.setText("Max file size: %s".formatted(stats.max()));
            minLabel.setText("Min file size: %s".formatted(stats.min()));
            stdLabel.setText("File size sd: %s".formatted((long) stats.std()));
        } else {
            nothingFoundLabel.setVisible(true);
        }
    }
}
