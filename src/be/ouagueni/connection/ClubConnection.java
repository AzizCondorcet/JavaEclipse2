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
            // Chargement du driver UCanAccess
            Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");

            // Construction du chemin complet vers la base
            String dbPath = System.getProperty("user.dir") + File.separator + "BD-Proj-2003.accdb";
            File dbFile = new File(dbPath);

            // Vérifie si le fichier existe réellement
            if (!dbFile.exists()) {
                JOptionPane.showMessageDialog(null,
                    "❌ Base de données introuvable :\n" + dbFile.getAbsolutePath(),
                    "Erreur de connexion", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            // URL de connexion JDBC
            String url = "jdbc:ucanaccess://" + dbFile.getAbsolutePath();

            // Affichage du chemin de la base utilisée (diagnostic)
            System.out.println("🔗 Connexion à la base Access : " + dbFile.getAbsolutePath());

            // Connexion à la base
            snglConnection = DriverManager.getConnection(url);

            // Confirmation de succès
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

        // Sécurité : arrêt si la connexion n'a pas pu être établie
        if (snglConnection == null) {
            JOptionPane.showMessageDialog(null,
                "La base de données est inaccessible, fermeture du programme.",
                "Erreur critique", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
     * Retourne l'instance unique de la connexion.
     */
    public static Connection getInstance() {
        if (snglConnection == null) {
            new ClubConnection();
        }
        return snglConnection;
    }
}
