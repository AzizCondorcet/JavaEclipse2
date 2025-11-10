package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import be.ouagueni.model.Ride;
import be.ouagueni.model.Vehicle;

public class VehicleDAO extends DAO<Vehicle> {
    public VehicleDAO(Connection conn) { super(conn); }

    @Override
    public boolean create(Vehicle vehicle) {
        int driverId = vehicle.getDriver().getId();
        System.out.println("Création véhicule pour membre ID: " + driverId);

        // Vérifier si déjà un véhicule
        String checkSql = "SELECT COUNT(*) FROM Vehicule WHERE idMemberDriver = ?";
        try (PreparedStatement checkPs = connect.prepareStatement(checkSql)) {
            checkPs.setInt(1, driverId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new IllegalStateException("Vous avez déjà enregistré un véhicule. Un membre ne peut avoir qu'une seule voiture.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Insérer le véhicule
        String sql = "INSERT INTO Vehicule (seatNumber, bikeSpotNumber, idMemberDriver) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, vehicle.getSeatNumber());
            ps.setInt(2, vehicle.getBikeSpotNumber());
            ps.setInt(3, driverId);

            if (ps.executeUpdate() == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int vehicleId = rs.getInt(1);
                    vehicle.setId(vehicleId);

                    // LIER À TOUTES LES RIDES
                    for (Ride ride : vehicle.getRides()) {
                        String linkSql = "INSERT INTO Ride_Vehicule (idRide, idVehicule) VALUES (?, ?)";
                        try (PreparedStatement ps2 = connect.prepareStatement(linkSql)) {
                            ps2.setInt(1, ride.getId());
                            ps2.setInt(2, vehicleId);
                            ps2.executeUpdate();
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

	@Override
	public boolean delete(Vehicle obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Vehicle obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Vehicle find(int id) {
		// TODO Auto-generated method stub
		return null;
	}

}
