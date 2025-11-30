package be.ouagueni.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import be.ouagueni.model.Bike;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import be.ouagueni.model.Vehicule;

public class VehiculeDAO extends DAO<Vehicule> {
    public VehiculeDAO(Connection conn) 
    { 
        super(conn); 
    }

    @Override
    public boolean create(Vehicule vehicle) {
        if (vehicle == null || vehicle.getDriver() == null) {
            System.err.println("ERREUR : Véhicule ou conducteur null");
            return false;
        }
        
        int driverId = vehicle.getDriver().getIdMember();
        
        if (driverId <= 0) {
            System.err.println("ERREUR : ID du membre invalide : " + driverId);
            return false;
        }
        
        System.out.println("Création véhicule pour membre ID: " + driverId);

        Connection conn = connect;
        try {
            conn.setAutoCommit(false);

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

            int vehicleId;

            String getVehicleSql = "SELECT idVehicule FROM Vehicule WHERE idMemberDriver = ?";
            try (PreparedStatement ps = conn.prepareStatement(getVehicleSql)) {
                ps.setInt(1, driverId);  
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        vehicleId = rs.getInt("idVehicule");
                        vehicle.setId(vehicleId);
                        System.out.println("Véhicule existant réutilisé : ID = " + vehicleId);

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
    public boolean delete(Vehicule obj) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean update(Vehicule vehicle) {
        if (vehicle.getId() <= 0 || vehicle.getDriver() == null) {
            System.err.println("update() échoué : véhicule ID invalide ou driver null");
            return false;
        }

        try {
            connect.setAutoCommit(false);

            // 1. Mise à jour 
            String sqlCapacite = "UPDATE Vehicule SET seatNumber = ?, bikeSpotNumber = ? WHERE idVehicule = ?";
            try (PreparedStatement ps = connect.prepareStatement(sqlCapacite)) {
                ps.setInt(1, vehicle.getSeatNumber());
                ps.setInt(2, vehicle.getBikeSpotNumber());
                ps.setInt(3, vehicle.getId());
                int updated = ps.executeUpdate();
                System.out.println("Capacités véhicule ID " + vehicle.getId() + " mises à jour : " + updated + " ligne(s)");
            }

            for (Ride ride : vehicle.getRides()) {
                int rideId = ride.getId();
                int vehId = vehicle.getId();

                Set<Member> currentPassengers = vehicle.getPassengers();

                String deletePassagers = "DELETE FROM Vehicule_Passager WHERE idVehicule = ? AND idMember IN (SELECT idMember FROM Inscription WHERE idRide = ?)";
                try (PreparedStatement ps = connect.prepareStatement(deletePassagers)) {
                    ps.setInt(1, vehId);
                    ps.setInt(2, rideId);
                    ps.executeUpdate();
                }

                String insertPassagerSql = """
                    INSERT INTO Vehicule_Passager (idVehicule, idMember)
                    SELECT ?, ?
                    FROM (SELECT 1 AS x) AS tmp
                    WHERE NOT EXISTS (
                        SELECT 1 FROM Vehicule_Passager 
                        WHERE idVehicule = ? AND idMember = ?
                    )
                    """;

                try (PreparedStatement ps = connect.prepareStatement(insertPassagerSql)) {
                    for (Member p : currentPassengers) {
                        if (p != null && p.getIdMember() > 0) {
                            ps.setInt(1, vehId);
                            ps.setInt(2, p.getIdMember());
                            ps.setInt(3, vehId);
                            ps.setInt(4, p.getIdMember());
                            ps.addBatch();
                        }
                    }
                    int[] inserted = ps.executeBatch();
                    System.out.println("Passagers insérés pour ride " + rideId + " : " + java.util.Arrays.stream(inserted).sum());
                }

                Set<Bike> currentBikes = vehicle.getBikes();

                String deleteBikes = """
                    DELETE FROM Vehicule_Bike 
                    WHERE idVehicule = ? 
                      AND idBike IN (
                        SELECT idBike FROM Inscription 
                        WHERE idRide = ? AND bike = True AND idBike IS NOT NULL
                      )
                    """;
                try (PreparedStatement ps = connect.prepareStatement(deleteBikes)) {
                    ps.setInt(1, vehId);
                    ps.setInt(2, rideId);
                    ps.executeUpdate();
                }

                String insertBikeSql = """
                    INSERT INTO Vehicule_Bike (idVehicule, idBike)
                    SELECT ?, ?
                    FROM (SELECT 1 AS x) AS tmp
                    WHERE NOT EXISTS (
                        SELECT 1 FROM Vehicule_Bike 
                        WHERE idVehicule = ? AND idBike = ?
                    )
                    """;

                try (PreparedStatement ps = connect.prepareStatement(insertBikeSql)) {
                    for (Bike b : currentBikes) {
                        if (b != null && b.getId() > 0) {
                            ps.setInt(1, vehId);
                            ps.setInt(2, b.getId());
                            ps.setInt(3, vehId);
                            ps.setInt(4, b.getId());
                            ps.addBatch();
                        }
                    }
                    int[] inserted = ps.executeBatch();
                    System.out.println("Vélos insérés pour ride " + rideId + " : " + java.util.Arrays.stream(inserted).sum());
                }
            }

            connect.commit();
            System.out.println("update(Vehicle) terminé avec succès pour véhicule ID " + vehicle.getId());
            return true;

        } catch (SQLException e) {
            System.err.println("Erreur dans update(Vehicle) : " + e.getMessage());
            e.printStackTrace();
            try { connect.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { connect.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @Override
    public Vehicule find(int memberId) 
    { 
        String sql = "SELECT idVehicule, seatNumber, bikeSpotNumber FROM Vehicule WHERE idMemberDriver = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                		Vehicule v = new Vehicule();
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
    public boolean save(Vehicule vehicle) {
        if (vehicle == null || vehicle.getDriver() == null || vehicle.getDriver().getIdMember() <= 0) {
            return false;
        }
        int driverId = vehicle.getDriver().getIdMember();
        int vehicleId = vehicle.getId();
        try {
            connect.setAutoCommit(false);
            
            // 1. Vérifier doublon de disponibilité
            for (Ride ride : vehicle.getRides()) {
                String check = "SELECT 1 FROM Ride_Vehicule rv JOIN Vehicule v ON rv.idVehicule = v.idVehicule WHERE rv.idRide = ? AND v.idMemberDriver = ?";
                try (PreparedStatement ps = connect.prepareStatement(check)) {
                    ps.setInt(1, ride.getId());
                    ps.setInt(2, driverId);
                    if (ps.executeQuery().next()) {
                        throw new IllegalStateException("Vous avez déjà posté vos disponibilités pour cette sortie.");
                    }
                }
            }
            
            // 2. Créer ou mettre à jour le véhicule
            if (vehicleId <= 0) {
                String sql = "INSERT INTO Vehicule (seatNumber, bikeSpotNumber, idMemberDriver) VALUES (?, ?, ?)";
                try (PreparedStatement ps = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, vehicle.getSeatNumber());
                    ps.setInt(2, vehicle.getBikeSpotNumber());
                    ps.setInt(3, driverId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            vehicle.setId(keys.getInt(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE Vehicule SET seatNumber = ?, bikeSpotNumber = ? WHERE idVehicule = ?";
                try (PreparedStatement ps = connect.prepareStatement(sql)) {
                    ps.setInt(1, vehicle.getSeatNumber());
                    ps.setInt(2, vehicle.getBikeSpotNumber());
                    ps.setInt(3, vehicle.getId());
                    ps.executeUpdate();
                }
            }
            
            // 3. Lier aux rides 
            String checkLink = "SELECT COUNT(*) FROM Ride_Vehicule WHERE idRide = ? AND idVehicule = ?";
            String insertLink = "INSERT INTO Ride_Vehicule (idRide, idVehicule) VALUES (?, ?)";
            
            try (PreparedStatement checkPs = connect.prepareStatement(checkLink);
                 PreparedStatement insertPs = connect.prepareStatement(insertLink)) {
                
                for (Ride ride : vehicle.getRides()) {
                    checkPs.setInt(1, ride.getId());
                    checkPs.setInt(2, vehicle.getId());
                    
                    try (ResultSet rs = checkPs.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) == 0) {
                            insertPs.setInt(1, ride.getId());
                            insertPs.setInt(2, vehicle.getId());
                            insertPs.addBatch();
                        }
                    }
                }
                insertPs.executeBatch();
            }
            
            connect.commit();
            return true;
            
        } catch (IllegalStateException e) {
            try { connect.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
        } catch (Exception e) {
            try { connect.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            try { connect.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    } 
    public Vehicule findByDriverId(int memberId) throws SQLException {
        String sql = "SELECT idVehicule, seatNumber, bikeSpotNumber FROM Vehicule WHERE idMemberDriver = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                		Vehicule v = new Vehicule();
                    v.setId(rs.getInt("idVehicule"));
                    v.setSeatNumber(rs.getInt("seatNumber"));
                    v.setBikeSpotNumber(rs.getInt("bikeSpotNumber"));
                    return v;
                }
            }
        }
        return null;
    }
}