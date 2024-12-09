package pl.edu.agh.to2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import pl.edu.agh.to2.applicationEvent.StageReadyEvent;

public class App extends Application {

    private ConfigurableApplicationContext applicationContext;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(SpringApp.class).run();
    }

    @Override
    public void start(Stage primaryStage) {
        applicationContext.publishEvent(new StageReadyEvent(primaryStage));
    }

    @Override
    public void stop() {
        applicationContext.stop();
        Platform.exit();
    }
}