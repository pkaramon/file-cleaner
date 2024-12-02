package pl.edu.agh.to2.example;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class MainViewController {

    @FXML
    private TextField pathInput;

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
        if (path != null && !path.isEmpty()) {
            Main.setPath(path);
        } else {
            System.out.println("Path is empty!");
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FileListView.fxml"));
            Scene newScene = new Scene(loader.load());

            FileListViewController controller = loader.getController();
            controller.setDirectoryPath(path);

            Stage stage = (Stage) pathInput.getScene().getWindow();
            stage.setScene(newScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
