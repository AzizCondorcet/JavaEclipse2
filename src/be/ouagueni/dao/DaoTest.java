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

            String sql = "SELECT * FROM Calendar";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                int numero = rs.getInt("idCalendar");  
                int numero2 = rs.getInt("idCategory");  

                System.out.println(numero + " - " + numero2);
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

