package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import be.ouagueni.model.Ride;
import be.ouagueni.model.Vehicle;

public class VehicleDAO extends DAO<Vehicle> {
    public VehicleDAO(Connection conn) 
    { 
        super(conn); 
    }

	@Override
	public boolean create(Vehicle vehicle) {
	    int driverId = vehicle.getDriver().getId();
	    System.out.println("Création véhicule pour membre ID: " + driverId);
	
	    Connection conn = connect; // Assure-toi que connect est bien initialisé
	    try {
	        conn.setAutoCommit(false); // Début transaction
	
	        // === 1. VÉRIFIER QUE LE MEMBRE N'A PAS DÉJÀ SA VOITURE DANS CE RIDE ===
	        for (Ride ride : vehicle.getRides()) {
	            String checkSql = """
	                SELECT 1 FROM Ride_Vehicule rv
	                JOIN Vehicule v ON rv.idVehicule = v.idVehicule
	                WHERE rv.idRide = ? AND v.idMemberDriver = ?
	                """;
	            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
	                ps.setInt(1, ride.getId());
	                ps.setInt(2, driverId);
	                try (ResultSet rs = ps.executeQuery()) {
	                    if (rs.next()) {
	                        conn.rollback();
	                        System.err.println("ERREUR : Le membre " + driverId + " a déjà posté sa voiture pour le ride " + ride.getId());
	                        throw new IllegalStateException("Vous avez déjà posté votre voiture pour cette sortie.");
	                    }
	                }
	            }
	        }
	
	        // === 2. CRÉER OU RÉUTILISER LE VÉHICULE ===
	        int vehicleId;
	
	        // Si le membre a déjà une voiture → réutiliser
	        String getVehicleSql = "SELECT idVehicule FROM Vehicule WHERE idMemberDriver = ?";
	        try (PreparedStatement ps = conn.prepareStatement(getVehicleSql)) {
	            ps.setInt(1, driverId);
	            try (ResultSet rs = ps.executeQuery()) {
	                if (rs.next()) {
	                    vehicleId = rs.getInt("idVehicule");
	                    vehicle.setId(vehicleId);
	                    System.out.println("Véhicule existant réutilisé : ID = " + vehicleId);
	
	                    // Mettre à jour les places (si changé)
	                    String updateSql = "UPDATE Vehicule SET seatNumber = ?, bikeSpotNumber = ? WHERE idVehicule = ?";
	                    try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
	                        psUpdate.setInt(1, vehicle.getSeatNumber());
	                        psUpdate.setInt(2, vehicle.getBikeSpotNumber());
	                        psUpdate.setInt(3, vehicleId);
	                        psUpdate.executeUpdate();
	                    }
	                } else {
	                    // Créer nouveau véhicule
	                    String insertSql = "INSERT INTO Vehicule (seatNumber, bikeSpotNumber, idMemberDriver) VALUES (?, ?, ?)";
	                    try (PreparedStatement psInsert = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
	                        psInsert.setInt(1, vehicle.getSeatNumber());
	                        psInsert.setInt(2, vehicle.getBikeSpotNumber());
	                        psInsert.setInt(3, driverId);
	                        if (psInsert.executeUpdate() == 0) {
	                            conn.rollback();
	                            return false;
	                        }
	                        try (ResultSet genKeys = psInsert.getGeneratedKeys()) {
	                            if (genKeys.next()) {
	                                vehicleId = genKeys.getInt(1);
	                                vehicle.setId(vehicleId);
	                                System.out.println("Nouveau véhicule créé : ID = " + vehicleId);
	                            } else {
	                                conn.rollback();
	                                return false;
	                            }
	                        }
	                    }
	                }
	            }
	        }
	
	        // === 3. LIER LE VÉHICULE À CHAQUE RIDE (sans doublon) ===
	        for (Ride ride : vehicle.getRides()) {
	            String linkSql = "INSERT INTO Ride_Vehicule (idRide, idVehicule) VALUES (?, ?)";
	            try (PreparedStatement ps = conn.prepareStatement(linkSql)) {
	                ps.setInt(1, ride.getId());
	                ps.setInt(2, vehicleId);
	                ps.executeUpdate();
	                System.out.println("Lien créé : Ride " + ride.getId() + " ←→ Véhicule " + vehicleId);
	            }
	        }
	
	        conn.commit();
	        System.out.println("Transaction réussie : véhicule et liens créés.");
	        return true;
	
	    } catch (SQLException e) {
	        try {
	            conn.rollback();
	            System.err.println("Transaction annulée : " + e.getMessage());
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        }
	        e.printStackTrace();
	        return false;
	    } finally {
	        try {
	            conn.setAutoCommit(true);
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	    }
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
    public Vehicle find(int memberId) 
    { 
        String sql = "SELECT idVehicule, seatNumber, bikeSpotNumber FROM Vehicule WHERE idMemberDriver = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Vehicle v = new Vehicle();
                    v.setId(rs.getInt("idVehicule"));
                    v.setSeatNumber(rs.getInt("seatNumber"));
                    v.setBikeSpotNumber(rs.getInt("bikeSpotNumber"));
                    return v;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}