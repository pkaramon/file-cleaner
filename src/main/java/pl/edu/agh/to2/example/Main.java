package pl.edu.agh.to2.example;

import pl.edu.agh.to2.example.connection.DatabaseConnectionProvider;
import pl.edu.agh.to2.example.file.FileSystemService;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.toString());

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter path: ");

        String path = scanner.nextLine();
        FileSystemService fileSystemService = new FileSystemServiceImp();
        Pattern txtPattern = Pattern.compile(".*\\.txt$");

        try (Connection connection = DatabaseConnectionProvider.getConnection()) {
            List<File> foundFiles = StreamSupport.stream(fileSystemService.searchDirectory(path, txtPattern).spliterator(), false)
                    .toList();
            saveFilesToDatabase(connection, foundFiles);

            System.out.println("Press number of largest files to display: ");
            int n = scanner.nextInt();
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
