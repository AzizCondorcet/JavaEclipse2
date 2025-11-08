package be.ouagueni.model;

import java.sql.Connection;
import be.ouagueni.connection.ClubConnection;

public class AppModel {

    private static AppModel instance;
    private final Connection conn;

    private AppModel() {
        this.conn = ClubConnection.getInstance();
    }

    public static synchronized AppModel getInstance() {
        if (instance == null) {
            instance = new AppModel();
        }
        return instance;
    }

    public Connection getConnection() {
        return conn;
    }
}
