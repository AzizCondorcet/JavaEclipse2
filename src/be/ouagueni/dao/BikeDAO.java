package be.ouagueni.dao;

import be.ouagueni.model.Bike;
import be.ouagueni.model.Member;

import java.sql.*;
import java.util.Set;

public class BikeDAO extends DAO<Bike> {

    public BikeDAO(Connection conn) {
        super(conn);
    }

    @Override
    public boolean create(Bike bike) {
        String sql = "INSERT INTO Bike (weight, bikeType, length, idMember) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDouble(1, bike.getWeight());
            pstmt.setInt(2, bike.getType().toInt());
            pstmt.setDouble(3, bike.getLength());
            pstmt.setInt(4, bike.getOwner().getId());

            int affected = pstmt.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    bike.setId(keys.getInt(1));
                    bike.getOwner().getBikes().add(bike);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(Bike bike) {
        if (bike.getId() <= 0) return false;

        // Vérification directe 
        String checkSql = "SELECT COUNT(*) FROM Inscription WHERE idBike = ?";
        String deleteSql = "DELETE FROM Bike WHERE idBike = ?";

        try {
            try (PreparedStatement check = connect.prepareStatement(checkSql)) {
                check.setInt(1, bike.getId());
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // Vélo déjà réservé peut pas interdit
                }
            }

            try (PreparedStatement delete = connect.prepareStatement(deleteSql)) {
                delete.setInt(1, bike.getId());
                int rows = delete.executeUpdate();
                if (rows > 0) {
                    bike.getOwner().getBikes().remove(bike);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Bike bike) {
        if (bike.getId() <= 0) return false;

        // Même vérification
        String checkSql = "SELECT COUNT(*) FROM Inscription WHERE idBike = ?";
        String updateSql = "UPDATE Bike SET weight = ?, bikeType = ?, length = ?, idMember = ? WHERE idBike = ?";

        try {
            // 1. Vérification : utilisé dans une inscription 
            try (PreparedStatement check = connect.prepareStatement(checkSql)) {
                check.setInt(1, bike.getId());
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // Interdit de modifier un vélo réservé
                }
            }

            // 2. Mise à jour
            try (PreparedStatement pstmt = connect.prepareStatement(updateSql)) {
                pstmt.setDouble(1, bike.getWeight());
                pstmt.setInt(2, bike.getType().toInt());
                pstmt.setDouble(3, bike.getLength());
                pstmt.setInt(4, bike.getOwner().getId());
                pstmt.setInt(5, bike.getId());

                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Bike find(int id) {
        String sql = "SELECT * FROM Bike WHERE idBike = ?";

        try (PreparedStatement pstmt = connect.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Bike bike = new Bike();
                bike.setId(rs.getInt("idBike"));
                bike.setWeight(rs.getDouble("weight"));
                bike.setTypeFromInt(rs.getInt("bikeType"));
                bike.setLength(rs.getDouble("length"));

                int idMember = rs.getInt("idMember");
                Member owner = new MemberDAO(connect).find(idMember);
                if (owner != null) {
                    bike.setOwner(owner);
                    if (!owner.getBikes().contains(bike)) {
                        owner.getBikes().add(bike);
                    }
                }
                return bike;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<Bike> findByMember(Member member) {
        Set<Bike> bikes = new java.util.HashSet<>();
        String sql = "SELECT * FROM Bike WHERE idMember = ?";

        try (PreparedStatement pstmt = connect.prepareStatement(sql)) {
            pstmt.setInt(1, member.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Bike b = new Bike();
                b.setId(rs.getInt("idBike"));
                b.setWeight(rs.getDouble("weight"));
                b.setTypeFromInt(rs.getInt("bikeType"));
                b.setLength(rs.getDouble("length"));
                b.setOwner(member);
                bikes.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bikes;
    }
}