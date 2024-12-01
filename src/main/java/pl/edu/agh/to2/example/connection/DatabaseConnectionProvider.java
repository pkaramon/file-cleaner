package pl.edu.agh.to2.example.connection;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnectionProvider {

    private static final Logger LOGGER = Logger.getGlobal();

    private static DataSource dataSource;

    static {
        try {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setURL("jdbc:postgresql://localhost:5432/postgres");
            ds.setUser("myuser");
            ds.setPassword("mypassword");
            dataSource = ds;

            LOGGER.info("Creating table files");
            create("CREATE TABLE IF NOT EXISTS files (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "size BIGINT NOT NULL" +
                    ");");

        } catch (Exception e) {
            LOGGER.info("Error during initialization: " + e.getMessage());
            throw new RuntimeException("Cannot initialize tables");
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void create(final String insertSql) throws SQLException {
        PreparedStatement ps = dataSource.getConnection().prepareStatement(insertSql);
        ps.execute();
    }
}
