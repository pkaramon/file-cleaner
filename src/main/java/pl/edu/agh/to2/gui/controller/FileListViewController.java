package pl.edu.agh.to2.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.command.CommandRegistry;
import pl.edu.agh.to2.command.DeleteActionCommand;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;
import pl.edu.agh.to2.gui.utils.TaskExecutor;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.service.FileService;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class FileListViewController {
    private static final Logger logger = LoggerFactory.getLogger(FileListViewController.class);
    private final SpringFXMLLoader loader;
    private final FileService fileService;
    private final ActionLogRepository actionLogRepository;
    private final Clock clock;
    private final CommandRegistry commandRegistry = new CommandRegistry();
    private TaskExecutor taskExecutor;

    @FXML
    private Stage stage;

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

    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

    @FXML
    private TextField searchField;

    private String directoryPath;

    private String pattern;

    public FileListViewController(SpringFXMLLoader loader, FileService fileService, ActionLogRepository actionLogRepository, Clock clock) {
        this.loader = loader;
        this.fileService = fileService;
        this.actionLogRepository = actionLogRepository;
        this.clock = clock;
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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void onShowAllClicked() {
        loadAllFiles();
    }

    @FXML
    private void onLargestClicked() {
        Optional<Integer> n = askForNumberOfLargestFiles();
        if (n.isEmpty()) {
            return;
        }
        taskExecutor.run(() -> fileService.findLargestFilesIn(Path.of(directoryPath), n.get()), this::updateTable);
    }

    @FXML
    private void onDeleteClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();
        var selectedRowsCopy = FXCollections.observableArrayList(selectedRows);

        if (!selectedRowsCopy.isEmpty()) {
            taskExecutor.run(() -> {
                        selectedRowsCopy.forEach(row -> {
                                    DeleteActionCommand deleteCommand = new DeleteActionCommand(fileService, actionLogRepository, clock);
                                    deleteCommand.setFileToBeDeleted(Path.of(row.getPath()));
                                    commandRegistry.executeCommand(deleteCommand);
                                    updateUndoRedoButtons();
                                }

                        );
                        return fileService.findFilesInPath(Path.of(directoryPath));
                    },
                    this::updateTable
            );
        }
    }

    @FXML
    public void onFindDuplicatesClicked() {
        showModal("/fxml/GroupFilesView.fxml", "Find Duplicates", controller -> {
            var ctrl = (GroupFilesViewController) controller;
            ctrl.show(FileService::findDuplicatedGroups);
        });
    }

    @FXML
    private void onFindVersionsClicked() {
        showModal("/fxml/GroupFilesView.fxml", "Versioned Files", controller -> {
            Optional<Integer> maxDistance = askUserForMaxDistance();
            if (maxDistance.isEmpty()) {
                return;
            }
            ((GroupFilesViewController) controller).show(fs -> fs.findVersions(maxDistance.get()));
        });
    }


    @FXML
    private void onSelectNewPathClicked() {
        Scene scene = loader.load("/fxml/MainView.fxml").scene();

        Stage stage = new Stage();
        stage.setTitle("File Explorer");
        stage.setScene(scene);

        stage.show();
    }

    @FXML
    private void onReportsClicked() {
        showModal("/fxml/ReportsView.fxml", "Reports", controller -> {
            var ctrl = (ReportsViewController) controller;
            ctrl.show();
        });
    }

    private void showModal(String fxmlPath, String windowTitle, Consumer<Object> configureController) {
        var res = loader.load(fxmlPath);
        Stage stage = new Stage();
        stage.setTitle(windowTitle);
        stage.setScene(res.scene());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setMinHeight(720);
        stage.setMinWidth(1280);

        configureController.accept(res.controller());

        stage.showAndWait();
    }


    @FXML
    private void onUndoClicked() {
        commandRegistry.undo();
        updateFileList();
        updateUndoRedoButtons();
    }

    @FXML
    private void onRedoClicked() {
        commandRegistry.redo();
        updateFileList();
        updateUndoRedoButtons();
    }

    private void updateUndoRedoButtons() {
        undoButton.setDisable(!commandRegistry.canUndo());
        redoButton.setDisable(!commandRegistry.canRedo());
    }

    public void closeCurrentStage() {
        stage.close();
    }

    private Optional<Integer> askUserForMaxDistance() {
        Optional<Integer> result;
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
        return result;
    }

    private Optional<Integer> askForNumberOfLargestFiles() {
        Optional<Integer> result;
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

        return result;
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

    @FXML
    private void onSearchClicked() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (!searchText.isEmpty()) {
            taskExecutor.run(() -> {

                List<File> allFiles = fileService.findFilesInPath(Path.of(directoryPath));

                return allFiles.stream()
                        .filter(file -> {
                            String fileName = Path.of(file.getPath()).getFileName().toString().toLowerCase();
                            return fileName.contains(searchText);
                        })
                        .collect(Collectors.toList());
            }, this::updateTable);
        } else {
            updateFileList();
        }
        searchField.clear();
    }

    @FXML
    public void onOpenFileClicked() {
        var selectedRows = fileTableView.getSelectionModel().getSelectedItems();

        if (selectedRows.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No File Selected");
            alert.setHeaderText("Please select at least one file to open.");
            alert.showAndWait();
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            logger.error("Desktop is not supported on this system.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        for (FileRow row : selectedRows) {
            java.io.File file = new java.io.File(row.getPath());

            if (file.exists()) {
                try {
                    desktop.open(file);
                } catch (IOException e) {
                    logger.error("Error opening file: {}", file.getAbsolutePath(), e);
                }
            } else {
                logger.error("File does not exist: {}", file.getAbsolutePath());
            }
        }
    }
}
