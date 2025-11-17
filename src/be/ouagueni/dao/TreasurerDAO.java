// be.ouagueni.dao.TreasurerDAO.java
package be.ouagueni.dao;

import be.ouagueni.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TreasurerDAO extends DAO<Treasurer> {

    public TreasurerDAO(Connection conn) {
        super(conn);
    }

    // ========================================
    // CRUD
    // ========================================
    @Override
    public boolean create(Treasurer obj) {
        String sql = "INSERT INTO Tresurer (idPerson) VALUES (?)";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, obj.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(Treasurer obj) {
        String sql = "DELETE FROM Tresurer WHERE idPerson = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, obj.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(Treasurer obj) {
        return true;
    }

    @Override
    public Treasurer find(int id) {
        String sql = "SELECT p.* FROM Person p JOIN Tresurer t ON p.id = t.idPerson WHERE t.idPerson = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Treasurer(
                        rs.getInt("id"),
                        rs.getString("namesPers"),
                        rs.getString("firstname"),
                        rs.getString("tel"),
                        rs.getString("psw")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ========================================
    // MÉTHODES MÉTIER
    // ========================================

    public List<Member> getMembersInDebt() {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT p.firstname, p.namesPers, m.balance, m.idMember FROM Member m JOIN Person p ON m.idPerson = p.id WHERE m.balance < 0";
        try (PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Member m = new Member();
                m.setFirstname(rs.getString("firstname"));
                m.setName(rs.getString("namesPers"));
                m.setBalance(rs.getDouble("balance"));
                m.setIdMember(rs.getInt("idMember"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> getDriverPayments()
    {
    List<Object[]> list = new ArrayList<>();
    String sql = """
    	    SELECT p.firstname, p.namesPers, v.seatNumber, v.bikeSpotNumber, m.idMember
    	    FROM Vehicule v
    	    JOIN Member m ON v.idMemberDriver = m.idMember
    	    JOIN Person p ON m.idPerson = p.id
    	    JOIN Ride_Vehicule rv ON v.idVehicule = rv.idVehicule
    	    JOIN Ride r ON rv.idRide = r.idRide
    	    WHERE r.startDate < NOW()
    	    """;
    try (PreparedStatement ps = connect.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String name = rs.getString("firstname") + " " + rs.getString("namesPers");
            int seats = rs.getInt("seatNumber");
            int bikes = rs.getInt("bikeSpotNumber");
            double amount = seats * 0.10 + bikes * 0.20;
            int memberId = rs.getInt("idMember");
            list.add(new Object[]{name, amount, memberId});
        }
    } catch (SQLException e) {
            e.printStackTrace();
    }
    return list;
}

    public boolean payDriver(int memberId, double amount) {
        String sql = "UPDATE Member SET balance = balance + ? WHERE idMember = ?";
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Ride> getUnpaidRides() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            JOIN Inscription i ON r.idRide = i.idRide
            JOIN Member m ON i.idMember = m.idMember
            WHERE m.balance < 0
            """;
        try (PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ride r = new Ride();
                r.setId(rs.getInt("idRide"));
                r.setnum(rs.getInt("num"));
                r.setStartPlace(rs.getString("startPlace"));
                r.setStartDate(rs.getTimestamp("startDate").toLocalDateTime());
                r.setFee(rs.getDouble("fee"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Member> getUnpaidMembersForRide(int rideId) {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT p.firstname, p.namesPers
            FROM Inscription i
            JOIN Member m ON i.idMember = m.idMember
            JOIN Person p ON m.idPerson = p.id
            WHERE i.idRide = ? AND m.balance < 0
            """;
        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Member m = new Member();
                    m.setFirstname(rs.getString("firstname"));
                    m.setName(rs.getString("namesPers"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}