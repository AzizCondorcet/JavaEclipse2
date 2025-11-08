package be.ouagueni.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;


public class ClubConnection {
    private static Connection snglConnection = null;

    private ClubConnection() {
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");

            String dbPath = System.getProperty("user.dir") + File.separator + "BD-Proj-2003.accdb";
            String url = "jdbc:ucanaccess://" + dbPath;

            snglConnection = DriverManager.getConnection(url);

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de trouver le driver UCanAccess !\n" + e.getMessage());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de se connecter à la base de données Access.\n" + e.getMessage());
        }

        if (snglConnection == null) {
            JOptionPane.showMessageDialog(null,
                "La base de données est inaccessible, fermeture du programme.");
            System.exit(0);
        }
    }

    public static Connection getInstance() {
        if (snglConnection == null) {
            new ClubConnection();
        }
        return snglConnection;
    }
}