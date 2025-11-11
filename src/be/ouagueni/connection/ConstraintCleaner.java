package be.ouagueni.connection;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire pour détecter et supprimer les contraintes orphelines (notamment MEMBER_VEHICULEMEMBER)
 * dans une base Access via UCanAccess.
 */
public class ConstraintCleaner {

    public static void main(String[] args) {
        try (Connection conn = ClubConnection.getInstance()) {
            conn.setAutoCommit(false);

            List<String> fkNames = getForeignKeys(conn, "Member");

            if (fkNames.isEmpty()) {
                System.out.println("✅ Aucune contrainte étrangère trouvée sur la table Member.");
                return;
            }

            System.out.println("🔍 Contraintes trouvées sur la table Member :");
            for (String fk : fkNames) {
                System.out.println("   → " + fk);
            }

            // Suppression ciblée de la contrainte fantôme
            for (String fk : fkNames) {
                if (fk.toUpperCase().contains("MEMBER_VEHICULEMEMBER")) {
                    dropConstraint(conn, "Member", fk);
                }
            }

            conn.commit();
            System.out.println("✅ Vérification après nettoyage :");
            getForeignKeys(conn, "Member");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupère toutes les clés étrangères associées à une table.
     */
    private static List<String> getForeignKeys(Connection conn, String tableName) throws SQLException {
        List<String> fkNames = new ArrayList<>();
        String sql = "SELECT FK_NAME, FKTABLE_NAME, PKTABLE_NAME " +
                     "FROM INFORMATION_SCHEMA.SYSTEM_CROSSREFERENCE " +
                     "WHERE FKTABLE_NAME = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("=== Contraintes sur la table " + tableName + " ===");
                while (rs.next()) {
                    String fkName = rs.getString("FK_NAME");
                    String pkTable = rs.getString("PKTABLE_NAME");
                    System.out.println("   FK : " + fkName + " → " + pkTable);
                    fkNames.add(fkName);
                }
            }
        }
        return fkNames;
    }

    /**
     * Supprime une contrainte spécifique d'une table.
     */
    private static void dropConstraint(Connection conn, String tableName, String constraintName) {
        String sql = "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("🗑️  Contrainte supprimée : " + constraintName);
        } catch (SQLException e) {
            System.err.println("⚠️  Impossible de supprimer la contrainte " + constraintName +
                               " : " + e.getMessage());
        }
    }
}
