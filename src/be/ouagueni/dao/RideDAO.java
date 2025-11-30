package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import be.ouagueni.model.Bike;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import be.ouagueni.model.TypeCat;
import be.ouagueni.model.Vehicule;
import be.ouagueni.model.Inscription;


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
        String sql = """
            SELECT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            WHERE r.idRide = ?
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ride ride = new Ride();
                    ride.setId(rs.getInt("idRide"));
                    ride.setnum(rs.getInt("num"));
                    ride.setStartPlace(rs.getString("startPlace"));
                    ride.setStartDate(rs.getTimestamp("startDate").toLocalDateTime());
                    ride.setFee(rs.getDouble("fee"));
                    return ride;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; 
    }
	
	public Set<Ride> getAllRides() {
	    Set<Ride> rides = new HashSet<>();
	    Map<Integer, Ride> rideMap = new HashMap<>();

	    String sql = """
	        SELECT
	            r.idRide, r.num, r.startPlace, r.startDate, r.fee,
	            cal.idCalendar,
	            cat.idCategory, cat.Type AS catType,
	            v.idVehicule, v.seatNumber, v.bikeSpotNumber, v.idMemberDriver,
	            m.idMember AS driverId, p.firstname AS driverFirstname, p.namesPers AS driverName,
	            ins.idInscription, ins.passenger, ins.bike AS hasBike, ins.idBike,
	            pm.idMember AS passagerId, pp.firstname AS passagerFirstname, pp.namesPers AS passagerName,
	            b.weight, b.bikeType, b.length
	        FROM (((Ride r
	        INNER JOIN Calendar cal ON r.idCalendar = cal.idCalendar)
	        INNER JOIN Category cat ON cal.idCategory = cat.idCategory)
	        LEFT JOIN Ride_Vehicule rv ON r.idRide = rv.idRide)
	        LEFT JOIN Vehicule v ON rv.idVehicule = v.idVehicule
	        LEFT JOIN Member m ON v.idMemberDriver = m.idMember
	        LEFT JOIN Person p ON m.idPerson = p.id
	        LEFT JOIN Inscription ins ON r.idRide = ins.idRide
	        LEFT JOIN Member pm ON ins.idMember = pm.idMember
	        LEFT JOIN Person pp ON pm.idPerson = pp.id
	        LEFT JOIN Bike b ON ins.idBike = b.idBike
	        ORDER BY r.startDate ASC, ins.idInscription
	        """;

	    try (PreparedStatement ps = connect.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            int rideId = rs.getInt("idRide");

	            // On crée la Ride 
	            Ride ride = rideMap.get(rideId);
	            if (ride == null) {
	                ride = new Ride();
	                ride.setId(rideId);
	                ride.setnum(rs.getInt("num"));
	                ride.setStartPlace(rs.getString("startPlace"));

	                Timestamp ts = rs.getTimestamp("startDate");
	                if (ts != null) {
	                    ride.setStartDate(ts.toLocalDateTime());
	                }

	                ride.setFee(rs.getDouble("fee"));

	                // Calendar + Category
	                Calendar cal = new Calendar();
	                cal.setid(rs.getInt("idCalendar"));

	                Category cat = new Category();
	                cat.setid(rs.getInt("idCategory"));
	                TypeCat typeCat = TypeCat.fromId(rs.getInt("catType"));
	                if (typeCat != null) {
	                    cat.setNomCategorie(typeCat);
	                }
	                cal.setCategory(cat);
	                ride.setCalendar(cal);

	                rideMap.put(rideId, ride);
	                rides.add(ride);
	            }

	            if (rs.getObject("idVehicule") != null) {
	                int vehId = rs.getInt("idVehicule");

	                if (ride.getVehicles().stream().noneMatch(v -> v.getId() == vehId)) {
	                	    Vehicule v = new Vehicule();
	                    v.setId(vehId);
	                    v.setSeatNumber(rs.getInt("seatNumber"));
	                    v.setBikeSpotNumber(rs.getInt("bikeSpotNumber"));

	                    Member driver = new Member();
	                    driver.setIdMember(rs.getInt("driverId"));
	                    driver.setFirstname(rs.getString("driverFirstname"));
	                    driver.setName(rs.getString("driverName"));
	                    v.setDriver(driver);

	                    ride.addVehicle(v);
	                    System.out.println("Véhicule ID " + vehId + " chargé pour " + ride.getStartPlace());
	                }
	            }

	            // === CHARGEMENT DES INSCRIPTIONS ===
	            if (rs.getObject("idInscription") != null) {
	                int insId = rs.getInt("idInscription");

	                if (ride.getInscriptions().stream().noneMatch(i -> i.getId() == insId)) {
	                    Inscription ins = new Inscription();
	                    ins.setId(insId);
	                    ins.setPassenger(rs.getBoolean("passenger"));
	                    ins.setBike(rs.getBoolean("hasBike"));

	                    Member passager = new Member();
	                    passager.setIdMember(rs.getInt("passagerId"));
	                    passager.setFirstname(rs.getString("passagerFirstname"));
	                    passager.setName(rs.getString("passagerName"));
	                    ins.setMember(passager);

	                    if (rs.getObject("idBike") != null) {
	                        Bike bike = new Bike();
	                        bike.setId(rs.getInt("idBike"));
	                        bike.setWeight(rs.getDouble("weight"));
	                        bike.setTypeFromInt(rs.getInt("bikeType"));
	                        bike.setLength(rs.getDouble("length"));
	                        ins.setBikeObj(bike);
	                    }

	                    ride.addInscription(ins);
	                    System.out.println("Inscription ID " + insId + " chargée : " +
	                            passager.getFirstname() + " " + passager.getName() +
	                            " → " + ride.getStartPlace());
	                }
	            }
	        }

	    } catch (SQLException e) {
	        System.err.println("ERREUR SQL dans getAllRides() :");
	        e.printStackTrace();
	    }

	    return rides;
	}
	
	public Set<Vehicule> getVehiclesForRide(int rideId)
	{
	    Set<Vehicule> vehicles = new HashSet<>();

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
	            		Vehicule v = new Vehicule();
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
