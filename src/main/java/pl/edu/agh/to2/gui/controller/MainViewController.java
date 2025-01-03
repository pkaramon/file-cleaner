package pl.edu.agh.to2.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Component
public class MainViewController {

    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    private final SpringFXMLLoader loader;
    @FXML
    private TextField pathInput;

    @FXML
    private TextField regexpInput;

    private FileListViewController fileListViewController;
    public MainViewController(SpringFXMLLoader loader) {
        this.loader = loader;
    }

    @FXML
    public void onChoosePathClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Folder");
        File selectedDirectory = directoryChooser.showDialog(pathInput.getScene().getWindow());

        if (selectedDirectory != null) {
            pathInput.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    public void onAcceptClicked() {
        String path = pathInput.getText();
        String regexp = regexpInput.getText();
        if (path == null || path.trim().isEmpty()) {
            pathInput.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            logger.error("Path is empty");
            return;
        }
        pathInput.setStyle("");

        var res = loader.load("/fxml/FileListView.fxml");
        Scene scene = res.scene();


        if(this.fileListViewController != null) {
            fileListViewController.closeCurrentStage();
        }
        FileListViewController controller = (FileListViewController) res.controller();
        this.fileListViewController = controller;

        controller.setPattern(regexp);
        controller.setDirectoryPath(path);


        Stage stage = (Stage) pathInput.getScene().getWindow();
        stage.close();

        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.setMinHeight(720);
        newStage.setMinWidth(1280);
        controller.setStage(newStage);
        newStage.setTitle("File Explorer");
        newStage.show();
    }


}
