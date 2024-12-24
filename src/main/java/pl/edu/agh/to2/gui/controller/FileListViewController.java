package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.task.TaskExecutor;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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

    private String pattern;

    public FileListViewController(SpringFXMLLoader loader, FileService fileService) {
        this.loader = loader;
        this.fileService = fileService;
    }


    @FXML
    public void initialize() {
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        hashColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));
        fileTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        taskExecutor = new TaskExecutor(rootPane);
    }

    public void setDirectoryPath(String path) {
        this.directoryPath = path;
        updateFileList();
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @FXML
    private void onShowAllClicked() {
        loadAllFiles();
    }

    @FXML
    private void onLargestClicked() {
        int n = askForNumberOfLargestFiles();
        taskExecutor.run(() -> fileService.findLargestFilesIn(Path.of(directoryPath), n), this::updateTable);
    }

    @FXML
    private void onDeleteClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();
        var selectedRowsCopy = FXCollections.observableArrayList(selectedRows);

        if (!selectedRowsCopy.isEmpty()) {
            taskExecutor.run(() -> {
                        selectedRowsCopy.forEach(row -> fileService.deleteFile(Path.of(row.getPath())));
                        return fileService.findFilesInPath(Path.of(directoryPath));
                    },
                    this::updateTable
            );
        }
    }

    @FXML
    public void onFindDuplicatesClicked() {
        var res = loader.load("/fxml/GroupFilesView.fxml");
        Stage stage = new Stage();
        stage.setTitle("Duplicated Files");
        stage.setScene(res.scene());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setMinHeight(720);
        stage.setMinWidth(1280);
        var controller = (GroupFilesViewController) res.controller();
        controller.show(FileService::findDuplicatedGroups);
        stage.showAndWait();
    }

    @FXML
    private void onFindVersionsClicked() {
        var res = loader.load("/fxml/GroupFilesView.fxml");
        Stage stage = new Stage();
        stage.setTitle("Versioned Files");
        stage.setScene(res.scene());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setMinHeight(720);
        stage.setMinWidth(1280);
        var controller = (GroupFilesViewController) res.controller();
        int maxDistance = askUserForMaxDistance();
        controller.show(fs -> fs.findVersions(maxDistance));
        stage.showAndWait();
    }

    private int askUserForMaxDistance() {
        Optional<Integer> result = Optional.empty();
        while (result.isEmpty()) {
            TextInputDialog dialog = new TextInputDialog("3");
            dialog.setTitle("Select max edit distance");
            dialog.setContentText("Please enter a max distance:");

            result = dialog.showAndWait().flatMap(s -> {
                        try {
                            return Optional.of(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    }
            );
        }
        return result.get();
    }

    private int askForNumberOfLargestFiles() {
        Optional<Integer> result = Optional.empty();
        while (result.isEmpty()) {
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Select number of largest files");
            dialog.setContentText("Please enter a number:");

            result = dialog.showAndWait().flatMap(s -> {
                        try {
                            return Optional.of(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    }
            );
        }
        return result.get();
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
                    fileService.loadFromPath(Path.of(directoryPath), Pattern.compile((pattern == null || pattern.isEmpty()) ? ".*" : pattern));
                    return fileService.findFilesInPath(Path.of(directoryPath));
                },
                this::updateTable
        );
    }

    private void loadAllFiles() {
        taskExecutor.run(() -> fileService.findFilesInPath(Path.of(directoryPath)), this::updateTable);
    }

    private void updateTable(List<File> files) {
        ObservableList<FileRow> rows = FXCollections.observableArrayList(
                files.stream().map(FileRow::new).toList()
        );
        fileTableView.setItems(rows);
    }


}
