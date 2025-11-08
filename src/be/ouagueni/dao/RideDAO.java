package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Ride;
import be.ouagueni.model.TypeCat;

public class RideDAO extends DAO<Ride> {
    public RideDAO(Connection conn) { super(conn); }
	@Override
	public boolean create(Ride ride) {
	    String sql = "INSERT INTO Ride (num, startPlace, startDate, fee, idCalendar) VALUES (?, ?, ?, ?, ?)";
	    try (PreparedStatement ps = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
	    {
	    		ps.setInt(1, ride.getnum());
	        ps.setString(2, ride.getStartPlace());
	        ps.setTimestamp(3, java.sql.Timestamp.valueOf(ride.getStartDate()));
	        ps.setDouble(4, ride.getFee());
	        ps.setInt(5, (int) ride.getCalendar().getid()); 

	        int rows = ps.executeUpdate();
	        if (rows > 0) {
	            try (ResultSet rs = ps.getGeneratedKeys()) {
	                if (rs.next()) ride.setId(rs.getInt(1)); 
	            }
	            return true;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	@Override
	public boolean delete(Ride obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Ride obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Ride find(int id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<Ride> getAllRides() {
	    Set<Ride> rides = new HashSet<>();
	    
	    String sql = 
	        "SELECT r.idRide, r.num, r.startPlace, r.startDate, r.fee, " +
	        "cal.idCalendar, " +
	        "cat.idCategory, cat.Type " +
	        "FROM Ride r " +
	        "INNER JOIN Calendar cal ON r.idCalendar = cal.idCalendar " +
	        "INNER JOIN Category cat ON cal.idCategory = cat.idCategory " +
	        "ORDER BY r.startDate ASC";
	    
	    try (PreparedStatement ps = connect.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {
	        
	        while (rs.next()) {
	            // Créer le Ride
	            Ride ride = new Ride();
	            ride.setId(rs.getInt("idRide"));
	            ride.setnum(rs.getInt("num"));
	            ride.setStartPlace(rs.getString("startPlace"));
	            
	            java.sql.Timestamp timestamp = rs.getTimestamp("startDate");
	            if (timestamp != null) {
	                ride.setStartDate(timestamp.toLocalDateTime());
	            }
	            
	            ride.setFee(rs.getDouble("fee"));
	            
	            // Créer le Calendar
	            Calendar calendar = new Calendar();
	            calendar.setid(rs.getInt("idCalendar"));
	            
	            // Créer la Category
	            Category category = new Category();
	            category.setid(rs.getInt("idCategory"));
	            
	            int typeId = rs.getInt("Type");
	            TypeCat typeCat = switch (typeId) {
	                case 1 -> TypeCat.MountainBike;
	                case 2 -> TypeCat.RoadBike;
	                case 3 -> TypeCat.Trial;
	                case 4 -> TypeCat.Downhill;
	                case 5 -> TypeCat.Cross;
	                default -> null;
	            };
	            
	            if (typeCat != null) {
	                category.setNomCategorie(typeCat);
	            }
	            
	            // Associer les objets
	            calendar.setCategory(category);
	            ride.setCalendar(calendar);
	            
	            rides.add(ride);
	        }
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    return rides;
	}

}
