package pl.edu.agh.to2.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import pl.edu.agh.to2.example.config.AppConfig;
import pl.edu.agh.to2.example.view.SpringFXMLLoader;

public class App extends Application {
    private AnnotationConfigApplicationContext context;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
    }


    @Override
    public void start(Stage primaryStage) {
        SpringFXMLLoader loader = context.getBean(SpringFXMLLoader.class);
        Scene scene = loader.load("/fxml/MainView.fxml").scene();
        primaryStage.setScene(scene);
        primaryStage.setTitle("File Explorer");
        primaryStage.show();
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }

}