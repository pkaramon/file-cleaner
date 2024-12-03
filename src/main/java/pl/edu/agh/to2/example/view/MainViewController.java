package pl.edu.agh.to2.example.view;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MainViewController {

    private final SpringFXMLLoader loader;
    @FXML
    private TextField pathInput;

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
        if (path == null || path.isEmpty()) {
            System.out.println("Path is empty!");
        }

        var res = loader.load("/fxml/FileListView.fxml");
        Scene scene = res.scene();
        FileListViewController controller = (FileListViewController) res.controller();
        controller.setDirectoryPath(path);
        Stage stage = (Stage) pathInput.getScene().getWindow();
        stage.setScene(scene);
    }

}
