package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import be.ouagueni.model.TypeCat;
import be.ouagueni.model.Vehicle;

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
	    Map<Integer, Ride> rideMap = new HashMap<>(); // pour éviter les doublons

	    String sql = """
	        SELECT 
	            r.idRide, r.num, r.startPlace, r.startDate, r.fee,
	            cal.idCalendar,
	            cat.idCategory, cat.Type AS catType,
	            v.idVehicule, v.seatNumber, v.bikeSpotNumber, v.idMemberDriver,
	            m.idMember, p.firstname, p.namesPers
	        FROM (((Ride r
	        INNER JOIN Calendar cal ON r.idCalendar = cal.idCalendar)
	        INNER JOIN Category cat ON cal.idCategory = cat.idCategory)
	        LEFT JOIN Ride_Vehicule rv ON r.idRide = rv.idRide)
	        LEFT JOIN Vehicule v ON rv.idVehicule = v.idVehicule
	        LEFT JOIN Member m ON v.idMemberDriver = m.idMember
	        LEFT JOIN Person p ON m.idPerson = p.id
	        ORDER BY r.startDate ASC
	        """;

	    try (PreparedStatement ps = connect.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            int rideId = rs.getInt("idRide");

	            Ride ride = rideMap.get(rideId);
	            if (ride == null) {
	                ride = new Ride();
	                ride.setId(rideId);
	                ride.setnum(rs.getInt("num"));
	                ride.setStartPlace(rs.getString("startPlace"));
	                Timestamp ts = rs.getTimestamp("startDate");
	                if (ts != null) ride.setStartDate(ts.toLocalDateTime());
	                ride.setFee(rs.getDouble("fee"));

	                // Calendar + Category
	                Calendar cal = new Calendar();
	                cal.setid(rs.getInt("idCalendar"));
	                Category cat = new Category();
	                cat.setid(rs.getInt("idCategory"));
	                TypeCat typeCat = TypeCat.fromId(rs.getInt("catType"));
	                if (typeCat != null) cat.setNomCategorie(typeCat);
	                cal.setCategory(cat);
	                ride.setCalendar(cal);

	                rideMap.put(rideId, ride);
	                rides.add(ride);
	            }

	            // === CHARGEMENT DU VÉHICULE SI EXISTE ===
	            if (rs.getObject("idVehicule") != null) {
	                int vehId = rs.getInt("idVehicule");

	                // Éviter doublon véhicule
	                boolean alreadyAdded = ride.getVehicles().stream().anyMatch(v -> v.getId() == vehId);
	                if (!alreadyAdded) {
	                    Vehicle v = new Vehicle();
	                    v.setId(vehId);
	                    v.setSeatNumber(rs.getInt("seatNumber"));
	                    v.setBikeSpotNumber(rs.getInt("bikeSpotNumber"));

	                    Member driver = new Member();
	                    driver.setIdMember(rs.getInt("idMember"));
	                    driver.setFirstname(rs.getString("firstname"));
	                    driver.setName(rs.getString("namesPers"));
	                    // driver.setId(rs.getInt("idPerson")); // si besoin

	                    v.setDriver(driver);
	                    ride.addVehicle(v);
	                    System.out.println("Véhicule ID " + vehId + " chargé pour la sortie " + ride.getStartPlace());
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return rides;
	}
	
	public Set<Vehicle> getVehiclesForRide(int rideId)
	{
	    Set<Vehicle> vehicles = new HashSet<>();

	    String sql = """
	        SELECT v.idVehicule, v.seatNumber, v.bikeSpotNumber,
	               m.idMember, p.firstname, p.namesPers
	        FROM Ride_Vehicule rv
	        JOIN Vehicule v ON rv.idVehicule = v.idVehicule
	        JOIN Member m ON v.idMemberDriver = m.idMember
	        JOIN Person p ON m.idPerson = p.id
	        WHERE rv.idRide = ?
	        """;

	    try (PreparedStatement ps = connect.prepareStatement(sql)) {
	        ps.setInt(1, rideId);
	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                Vehicle v = new Vehicle();
	                v.setId(rs.getInt("idVehicule"));
	                v.setSeatNumber(rs.getInt("seatNumber"));
	                v.setBikeSpotNumber(rs.getInt("bikeSpotNumber"));

	                Member driver = new Member();
	                driver.setIdMember(rs.getInt("idMember"));
	                driver.setFirstname(rs.getString("firstname"));
	                driver.setName(rs.getString("namesPers"));

	                v.setDriver(driver);
	                vehicles.add(v);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return vehicles;
	}
}
