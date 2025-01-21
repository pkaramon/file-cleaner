package pl.edu.agh.to2.gui.controller;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.utils.TaskExecutor;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.service.FileService;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javafx.collections.FXCollections.observableArrayList;

@Component
public class GroupFilesViewController {
    private final FileService fileService;
    private Function<FileService, List<List<File>>> getFileGroups;

    @FXML
    private ScrollPane scrollPane;
    private TaskExecutor taskExecutor;

    @FXML
    private Pane rootPane;

    private TextField searchField;
    private Button searchButton;
    private Button resetButton;

    public GroupFilesViewController(FileService fileService) {
        this.fileService = fileService;
    }

    @FXML
    private void initialize() {
        taskExecutor = new TaskExecutor(rootPane);

        searchField = new TextField();
        searchField.setPromptText("Search by file name...");
        searchButton = new Button("Search");
        resetButton = new Button("Reset");
        searchButton.setOnAction(event -> refresh());
        resetButton.setOnAction(event -> resetSearch());
    }

    public void show(Function<FileService, List<List<File>>> getFileGroups) {
        this.getFileGroups = getFileGroups;
        refresh();
    }

    private void refresh() {
        String searchText = searchField.getText().toLowerCase().trim();
        if (!searchText.isEmpty()) {
            taskExecutor.run(() -> {
                List<List<File>> allGroups = this.getFileGroups.apply(fileService);

                Predicate<File> containsSearchText = file ->
                        Path.of(file.getPath()).getFileName().toString().toLowerCase().contains(searchText);

                return allGroups
                        .stream()
                        .filter(group -> group.stream().anyMatch(containsSearchText))
                        .map(group -> group.stream()
                                .filter(containsSearchText)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());
            }, this::displayGroups);
        } else {
            taskExecutor.run(() -> this.getFileGroups.apply(fileService), this::displayGroups);
        }
    }

    private void resetSearch() {
        searchField.clear();
        refresh();
    }

    private void displayGroups(List<List<File>> filteredGroups) {
        VBox layout = new VBox(10);

        HBox searchLayout = new HBox(10);
        searchLayout.setStyle("-fx-padding: 10;");
        searchLayout.getChildren().addAll(new Label("Search:"), searchField, searchButton, resetButton);

        layout.getChildren().add(searchLayout);

        for (List<File> group : filteredGroups) {
            List<FileRow> fileRows = group
                    .stream()
                    .map(FileRow::new)
                    .collect(Collectors.toList());

            TableView<FileRow> tableView = createTableView(observableArrayList(fileRows));
            HBox buttonLayout = displayButtons(group, tableView);
            VBox tableLayout = new VBox(10, buttonLayout, tableView);

            layout.getChildren().add(tableLayout);
        }
        scrollPane.setContent(layout);
    }

    private HBox displayButtons(List<File> files, TableView<FileRow> tableView) {
        Button deleteAllBtn = new Button("Delete All");
        deleteAllBtn.setOnAction(event -> taskExecutor.run(() -> {
            files.forEach(f -> fileService.deleteFile(Path.of(f.getPath())));
            return null;
        }, __ -> refresh()));

        Button archiveBtn = new Button("Archive");
        archiveBtn.setOnAction(event -> archiveGroupOfFiles(files));

        Button deleteSelectedBtn = new Button("Delete Selected");
        deleteSelectedBtn.setOnAction(event -> taskExecutor.run(() -> {
            tableView.getSelectionModel().getSelectedItems().forEach(f -> fileService.deleteFile(Path.of(f.getPath())));
            return null;
        }, __ -> refresh()));

        return new HBox(10, deleteAllBtn, archiveBtn, deleteSelectedBtn);
    }

    private void archiveGroupOfFiles(List<File> files) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        java.io.File selectedFile = fileChooser.showSaveDialog(null);

        if (selectedFile == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No file selected");
            alert.showAndWait();
            return;
        }

        taskExecutor.run(() -> {
            fileService.archiveFiles(files, selectedFile.toPath());
            return null;
        }, __ -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Files archived successfully");
            alert.showAndWait();
            refresh();
        });
    }

    private TableView<FileRow> createTableView(ObservableList<FileRow> rows) {
        TableView<FileRow> tableView = new TableView<>();
        tableView.prefWidthProperty().bind(scrollPane.widthProperty().subtract(20));
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<FileRow, String> nameColumn = new TableColumn<>("Path");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("path"));

        TableColumn<FileRow, String> pathColumn = new TableColumn<>("Size");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("size"));

        TableColumn<FileRow, Long> sizeColumn = new TableColumn<>("Hash");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("hash"));

        tableView.getColumns().add(nameColumn);
        tableView.getColumns().add(pathColumn);
        tableView.getColumns().add(sizeColumn);

        tableView.setItems(rows);
        tableView.setMinWidth(600);

        return tableView;
    }
}
