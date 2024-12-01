package pl.edu.agh.to2.example;

import javafx.application.Application;
import pl.edu.agh.to2.example.connection.DatabaseConnectionProvider;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.toString());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter path: ");

        String path = scanner.nextLine();
        File folder = new File(path);

        if (!folder.exists() || !folder.isDirectory()) {
            log.warning("Invalid folder path provided.");
            return;
        }

        try (Connection connection = DatabaseConnectionProvider.getConnection()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File file : files) {
                    log.info("Found file: " + file.getName());
                    saveFileNameToDatabase(connection, file);
                }
            }
        } catch (SQLException e) {
            log.warning("Error during connection initialization: " + e.getMessage());
        }

        log.info("Hello world");
    }

    private static void saveFileNameToDatabase(Connection connection, File file) {
        String sql = "INSERT INTO files (name, size) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, file.getName());
            ps.setLong(2, file.length());
            ps.executeQuery();
            log.info("File name '" + file.getName() + "' saved to the database.");
        } catch (SQLException e) {
            log.warning("Error saving file name '" + file.getName() + "' to database: " + e.getMessage());
        }
    }
}
