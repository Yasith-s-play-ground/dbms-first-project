package lk.ijse.dep12.jdbc.first_project.db;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class SingletonConnection {

    private static final SingletonConnection INSTANCE = new SingletonConnection();
    private Connection CONNECTION;

    private SingletonConnection() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/application.properties"));

            String url = properties.getProperty("app.url");
            String username = properties.getProperty("app.username");
            String password = properties.getProperty("app.password");

            CONNECTION = DriverManager.getConnection(url, username, password);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e instanceof SQLException ? "Failed to establish database connection, try restarting" :
                    "Failed to load configurations")
                    .showAndWait();
            System.exit(1);
        }
    }

    public static SingletonConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() {
        return CONNECTION;
    }

}
