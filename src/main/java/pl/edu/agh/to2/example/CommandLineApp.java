package pl.edu.agh.to2.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.edu.agh.to2.example.file.FileService;

import java.nio.file.Path;


@SpringBootApplication
public class CommandLineApp implements CommandLineRunner {

    private final FileService fileService;


    public CommandLineApp(FileService fileService) {
        this.fileService = fileService;
    }

    public static void main(String[] args) {
        SpringApplication.run(CommandLineApp.class, args);
    }

    @Override
    public void run(String... args) {
        Path dir = Path.of("C:\\Users\\piotr\\Documents\\");
        fileService.loadFromPath("C:\\Users\\piotr\\Documents\\", ".*\\.txt");

        fileService.findLargestFiles(10).forEach(file -> {
            System.out.println("File name: " + file.getName() + ", size: " + file.getSize());
        });

    }
}
