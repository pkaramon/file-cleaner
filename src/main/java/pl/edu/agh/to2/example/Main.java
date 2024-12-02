package pl.edu.agh.to2.example;

import javafx.application.Application;
import pl.edu.agh.to2.example.connection.DatabaseConnectionProvider;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.toString());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter path: ");

        String path = scanner.nextLine();
        FileSearch fileSearch = new FileSearch(".*\\.txt$");

        try (Connection connection = DatabaseConnectionProvider.getConnection()) {
            List<File> foundFiles = fileSearch.searchDirectory(path);
            saveFilesToDatabase(connection, foundFiles);
        } catch (SQLException e) {
            log.warning("Error during connection initialization: " + e.getMessage());
        }
    }

    private static void saveFilesToDatabase(Connection connection, List<File> files) {
        String sql = "INSERT INTO files (name, size, path, last_modified) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (File file : files) {
                ps.setString(1, file.getName());
                ps.setLong(2, file.length());
                ps.setString(3, file.getAbsolutePath());
                ps.setTimestamp(4, new Timestamp(file.lastModified()));
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            log.info(results.length + " file names saved to the database.");
        } catch (SQLException e) {
            log.warning("Error saving file names to database: " + e.getMessage());
        }
    }
}
