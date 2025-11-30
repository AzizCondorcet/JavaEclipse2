package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import be.ouagueni.model.Calendar;

public class CalendarDAO extends DAO<Calendar> {
    public CalendarDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Calendar calendar) {
	    try {
	        System.out.println("calendar.getCategory().getid(): " + calendar.getCategory().getid());
	
	        // Vérifier si un calendrier existe pour cette catégorie
	        String sqlCheck = "SELECT idCalendar FROM Calendar WHERE idCategory = ?";
	        try (PreparedStatement psCheck = connect.prepareStatement(sqlCheck)) {
	            psCheck.setInt(1, calendar.getCategory().getid());
	            try (ResultSet rs = psCheck.executeQuery()) {
	                if (rs.next()) {
	                    // Calendrier existant trouvé, on met a jour
	                    calendar.setid(rs.getInt("idCalendar"));
	                }
	            }
	        }
	
	        // 2️⃣ Aucun calendrier existant
	        if (calendar.getid() == 0) {
	            String sqlInsert = "INSERT INTO Calendar (idCategory) VALUES (?)";
	            try (PreparedStatement psInsert = connect.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
	                psInsert.setInt(1, calendar.getCategory().getid());
	                int rows = psInsert.executeUpdate();
	                if (rows > 0) {
	                    try (ResultSet rsKeys = psInsert.getGeneratedKeys()) {
	                        if (rsKeys.next()) {
	                            calendar.setid(rsKeys.getInt(1)); // récupère l'ID 
	                        }
	                    }
	                } else {
	                    return false; 
	                }
	            }
	        }
	        return true; // tout est ok
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false; 
	}
	
	@Override
	public boolean delete(Calendar obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Calendar obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Calendar find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
