package pl.edu.agh.to2.gui;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.applicationEvent.StageReadyEvent;
import pl.edu.agh.to2.gui.utils.SpringFXMLLoader;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext context;

    public StageInitializer(ApplicationContext context) {
        this.context = context;
    }
    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage primaryStage = event.getStage();

        SpringFXMLLoader loader = context.getBean(SpringFXMLLoader.class);
        Scene scene = loader.load("/fxml/MainView.fxml").scene();
        primaryStage.setScene(scene);
        primaryStage.setTitle("File Cleaner");
        primaryStage.show();
    }
}
