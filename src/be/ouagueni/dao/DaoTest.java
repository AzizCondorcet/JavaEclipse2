package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import be.ouagueni.connection.ClubConnection;

public class DaoTest {

    public static void main(String[] args) {
        System.out.println("=== Test d'accès à la base Access ===");
        
        try (Connection conn = ClubConnection.getInstance();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT * FROM Exemple";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int numero = rs.getInt("N°");  
                String champ1 = rs.getString("Champ1");

                System.out.println(numero + " - " + champ1);
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

