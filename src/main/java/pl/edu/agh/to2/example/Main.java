package pl.edu.agh.to2.example;

import pl.edu.agh.to2.example.connection.DatabaseConnectionProvider;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Application;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.toString());
    private static String path;

    public static void main(String[] args) {
        Application.launch(App.class);
    }

    public static void setPath(String newPath) {
        path = newPath;
        processPath();
    }

    private static void processPath() {
        if (path == null || path.isEmpty()) {
            log.warning("Path is null or empty.");
            return;
        }

        FileSearch fileSearch = new FileSearch(".*\\.txt$");

        try (Connection connection = DatabaseConnectionProvider.getConnection()) {
            List<File> foundFiles = fileSearch.searchDirectory(path);
            saveFilesToDatabase(connection, foundFiles);
            System.out.println("Press number of largest files to display: ");
            int n = 2;//TODO
            String sql = "SELECT name, size FROM files ORDER BY size DESC LIMIT ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, n);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println(rs.getString("name") + " " + rs.getLong("size"));
                }
            }
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
