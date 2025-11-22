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

    // ====================== CRUD ======================
    @Override public boolean create(Treasurer obj) { return true; }
    @Override public boolean delete(Treasurer obj) { return true; }
    @Override public boolean update(Treasurer obj) { return true; }
    @Override public Treasurer find(int id) { return null; }

    // ====================== MÉTHODES MÉTIER CORRIGÉES ======================

    // 1. Tous les membres avec une dette globale (rappel général)
    public List<Member> getMembersInDebt() {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT p.firstname, p.namesPers, m.balance, m.idMember
            FROM Member m
            JOIN Person p ON m.idPerson = p.id
            WHERE m.balance < 0
            ORDER BY p.namesPers, p.firstname
            """;
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 2. Sorties passées où il reste au moins un passager non crédité du forfait
    public List<Ride> getRidesWithPendingPayments() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            INNER JOIN Inscription i ON r.idRide = i.idRide
            INNER JOIN Member m ON i.idMember = m.idMember
            WHERE i.passenger = 0
              AND r.startDate < Now()
              AND m.balance + r.fee <= 0                    -- pas encore crédité du forfait
            ORDER BY r.startDate DESC
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 3. Passagers d'une sortie donnée qui n'ont PAS encore été crédités du forfait
    public List<Member> getPendingPassengersForRide(int rideId) {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT p.firstname, p.namesPers, p.tel, m.idMember, m.balance, r.fee
            FROM Inscription i
            INNER JOIN Member m ON i.idMember = m.idMember
            INNER JOIN Person p ON m.idPerson = p.id
            INNER JOIN Ride r ON i.idRide = r.idRide
            WHERE i.idRide = ?
              AND i.passenger = 0
              AND m.balance + r.fee <= 0
            """;

        try (PreparedStatement ps = connect.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Member m = new Member();
                    m.setFirstname(rs.getString("firstname"));
                    m.setName(rs.getString("namesPers"));
                    m.setTel(rs.getString("tel"));
                    m.setIdMember(rs.getInt("idMember"));
                    m.setBalance(rs.getDouble("balance"));
                    list.add(m);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 4. Créditer les passagers sélectionnés
    public int confirmPassengerPayments(int rideId, List<Integer> passengerIds) {
        if (passengerIds == null || passengerIds.isEmpty()) return 0;

        String sql = """
            UPDATE Member 
            SET balance = balance + (SELECT fee FROM Ride WHERE idRide = ?) 
            WHERE idMember = ?
            """;

        try {
            connect.setAutoCommit(false);
            try (PreparedStatement ps = connect.prepareStatement(sql)) {
                for (Integer id : passengerIds) {
                    ps.setInt(1, rideId);
                    ps.setInt(2, id);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                connect.commit();
                return (int) java.util.Arrays.stream(results).filter(r -> r > 0).count();
            } catch (Exception e) {
                connect.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { connect.setAutoCommit(true); } catch (Exception ignored) {}
        }
    }

    // 5. Sorties avec au moins un passager non crédité (utilisé par "Réclamer les frais")
    public List<Ride> getUnpaidRides() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            INNER JOIN Inscription i ON r.idRide = i.idRide
            INNER JOIN Member m ON i.idMember = m.idMember
            WHERE i.passenger = 0
              AND m.balance + r.fee <= 0
            ORDER BY r.startDate DESC
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 6. Membres non crédités pour une sortie donnée (affiché dans "Réclamer")
    public List<Member> getUnpaidMembersForRide(int rideId) {
        List<Member> list = new ArrayList<>();
        String sql = """
            SELECT p.firstname, p.namesPers
            FROM Inscription i
            JOIN Member m ON i.idMember = m.idMember
            JOIN Person p ON m.idPerson = p.id
            JOIN Ride r ON i.idRide = r.idRide
            WHERE i.idRide = ?
              AND i.passenger = 0
              AND m.balance + r.fee <= 0
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}