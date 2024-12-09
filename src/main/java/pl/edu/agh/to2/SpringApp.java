package pl.edu.agh.to2;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.edu.agh.to2.gui.App;

@SpringBootApplication
public class SpringApp {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
