package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import be.ouagueni.model.Calendar;
import be.ouagueni.model.Ride;

public class CalendarDAO extends DAO<Calendar> {
    public CalendarDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Calendar calendar) {
	    try {
	        System.out.println("calendar.getCategory().getid(): " + calendar.getCategory().getid());
	
	        // 1️⃣ Vérifier si un calendrier existe déjà pour cette catégorie
	        String sqlCheck = "SELECT idCalendar FROM Calendar WHERE idCategory = ?";
	        try (PreparedStatement psCheck = connect.prepareStatement(sqlCheck)) {
	            psCheck.setInt(1, calendar.getCategory().getid());
	            try (ResultSet rs = psCheck.executeQuery()) {
	                if (rs.next()) {
	                    // Calendrier existant trouvé, on met à jour l'objet avec l'ID
	                    calendar.setid(rs.getInt("idCalendar"));
	                }
	            }
	        }
	
	        // 2️⃣ Aucun calendrier existant, on insère un nouveau si l'ID n'est pas déjà défini
	        if (calendar.getid() == 0) {
	            String sqlInsert = "INSERT INTO Calendar (idCategory) VALUES (?)";
	            try (PreparedStatement psInsert = connect.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
	                psInsert.setInt(1, calendar.getCategory().getid());
	                int rows = psInsert.executeUpdate();
	                if (rows > 0) {
	                    try (ResultSet rsKeys = psInsert.getGeneratedKeys()) {
	                        if (rsKeys.next()) {
	                            calendar.setid(rsKeys.getInt(1)); // récupère l'ID auto-généré
	                        }
	                    }
	                } else {
	                    return false; // insertion échouée
	                }
	            }
	        }
	        return true; // tout s'est bien passé
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false; // en cas d'erreur
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
