package pl.edu.agh.to2.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 400, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("File Explorer");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}