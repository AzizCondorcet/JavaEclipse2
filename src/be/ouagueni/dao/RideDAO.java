package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import be.ouagueni.model.Ride;

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

}
