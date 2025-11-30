package be.ouagueni.connection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Gestionnaire de connexion unique à la base Access via UCanAccess.
 */
public class ClubConnection {
    private static Connection snglConnection = null;

    private ClubConnection() {
        try {
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");

            String dbPath = System.getProperty("user.dir") + File.separator + "BD-Proj-2003_2.accdb";
            File dbFile = new File(dbPath);

            if (!dbFile.exists()) {
                JOptionPane.showMessageDialog(null,
                    "Base de données introuvable :\n" + dbFile.getAbsolutePath(),
                    "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            String url = "jdbc:ucanaccess://" + dbFile.getAbsolutePath();

            System.out.println("🔗 Connexion à la base Access : " + dbFile.getAbsolutePath());

            snglConnection = DriverManager.getConnection(url);

            System.out.println("✅ Connexion établie avec succès.\n");

        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de trouver le driver UCanAccess !\n" + e.getMessage(),
                "Erreur de driver", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Impossible de se connecter à la base de données Access.\n" + e.getMessage(),
                "Erreur SQL", JOptionPane.ERROR_MESSAGE);
        }

        if (snglConnection == null) {
            JOptionPane.showMessageDialog(null,
                "La base de données est inaccessible, fermeture du programme.",
                "Erreur critique", JOptionPane.ERROR_MESSAGE);
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
