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

    @Override public boolean create(Treasurer obj) { return false; }
    @Override public boolean delete(Treasurer obj) { return false; }
    @Override public boolean update(Treasurer obj) { return false; }
    @Override public Treasurer find(int id) { return null; }
    // =====================================================================
    // 1. Membres dont le solde global est négatif
    // =====================================================================
    public List<Member> getMembersInDebt() {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT m.idMember, p.namesPers, p.firstname, m.balance
            FROM Member m
            INNER JOIN Person p ON m.idPerson = p.id
            WHERE m.balance < 0
            ORDER BY p.namesPers, p.firstname
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Member m = new Member();
                m.setIdMember(rs.getInt("idMember"));
                m.setName(rs.getString("namesPers"));
                m.setFirstname(rs.getString("firstname"));
                m.setBalance(rs.getDouble("balance"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =====================================================================
    // 2. "Réclamer les frais non payés" → toutes les sorties avec passagers
    // =====================================================================
    public List<Ride> getUnpaidRides() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            INNER JOIN Inscription i ON r.idRide = i.idRide
            WHERE i.passenger = 1
            ORDER BY r.startDate DESC
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ride r = new Ride();
                r.setId(rs.getInt("idRide"));
                r.setnum(rs.getInt("num"));  // Correction : setNum (pas setnum)
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

    // =====================================================================
    // 3. "Valider paiements covoiturage" → sorties passées avec passagers
    // =====================================================================
    public List<Ride> getRidesWithPendingPayments() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            INNER JOIN Inscription i ON r.idRide = i.idRide
            WHERE i.passenger = 1
              AND r.startDate < NOW()
            ORDER BY r.startDate DESC
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ride r = new Ride();
                r.setId(rs.getInt("idRide"));
                r.setnum(rs.getInt("num"));  // Correction
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

    // =====================================================================
    // 4. Passagers d'une sortie
    // =====================================================================
    public List<Member> getPassengersForRide(int rideId) {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT m.idMember, p.namesPers, p.firstname, p.tel, m.balance, r.fee
            FROM Inscription i
            JOIN Member m ON i.idMember = m.idMember
            JOIN Person p ON m.idPerson = p.id
            JOIN Ride r ON i.idRide = r.idRide
            WHERE i.idRide = ?
              AND i.passenger = 1
            ORDER BY p.namesPers, p.firstname
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Member m = new Member();
                    m.setIdMember(rs.getInt("idMember"));
                    m.setName(rs.getString("namesPers"));
                    m.setFirstname(rs.getString("firstname"));
                    m.setTel(rs.getString("tel"));
                    m.setBalance(rs.getDouble("balance"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Compatibilité anciennes méthodes
    public List<Member> getPendingPassengersForRide(int rideId) {
        return getPassengersForRide(rideId);
    }
    public List<Member> getUnpaidMembersForRide(int rideId) {
        return getPassengersForRide(rideId);
    }

    // =====================================================================
    // 5. Créditer les membres sélectionnés
    // =====================================================================
    public int confirmPassengerPayments(int rideId, List<Integer> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return 0;

        double fee = 0;
        String sqlFee = "SELECT fee FROM Ride WHERE idRide = ?";
        try (PreparedStatement ps = connect.prepareStatement(sqlFee)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) fee = rs.getDouble("fee");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        if (fee <= 0) return 0;

        String sqlUpdate = "UPDATE Member SET balance = balance + ? WHERE idMember = ?";
        try {
            connect.setAutoCommit(false);
            try (PreparedStatement ps = connect.prepareStatement(sqlUpdate)) {
                for (Integer id : memberIds) {
                    ps.setDouble(1, fee);
                    ps.setInt(2, id);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                connect.commit();
                return (int) java.util.Arrays.stream(results)
                        .filter(r -> r > 0 || r == Statement.SUCCESS_NO_INFO)
                        .count();
            } catch (Exception e) {
                connect.rollback();
                e.printStackTrace();
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { connect.setAutoCommit(true); } catch (Exception ignored) {}
        }
    }
}