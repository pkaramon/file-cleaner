package pl.edu.agh.to2.example;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringApp {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
