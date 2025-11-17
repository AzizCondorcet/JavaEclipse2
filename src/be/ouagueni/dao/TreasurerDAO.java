// be.ouagueni.dao.TreasurerDAO.java
package be.ouagueni.dao;

import be.ouagueni.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TreasurerDAO extends DAO<Treasurer> {

    public TreasurerDAO(Connection conn) {
        super(conn);
    }

    // ====================== CRUD ======================
    @Override
    public boolean create(Treasurer obj) { /* déjà implémenté chez toi */ return true; }
    @Override
    public boolean delete(Treasurer obj) { /* déjà implémenté */ return true; }
    @Override
    public boolean update(Treasurer obj) { return true; }
    @Override
    public Treasurer find(int id) { /* déjà implémenté */ return null; }

    // ====================== MÉTHODES MÉTIER ======================

    // 1. Membres qui ont une dette (cotisation ou covoiturage)
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

    // 2. Sorties qui ont encore des passagers non validés comme payés
 // Remplace uniquement ces méthodes dans ton TreasurerDAO

 // 1. Sorties avec des passagers qui n'ont pas encore payé (balance < 0)
 public List<Ride> getRidesWithPendingPayments() {
     List<Ride> list = new ArrayList<>();
     String sql = """
         SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
         FROM Ride r
         INNER JOIN Inscription i ON r.idRide = i.idRide
         INNER JOIN Member m ON i.idMember = m.idMember
         WHERE i.passenger = 0                   -- 0 = passager
           AND m.balance < 0                     -- dette non réglée
           AND r.startDate < Now()               -- sortie passée
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
     } catch (SQLException e) {
         e.printStackTrace();
     }
     return list;
 }

 // 2. Passagers d'une sortie donnée qui n'ont pas encore payé
 public List<Member> getPendingPassengersForRide(int rideId) {
	    List<Member> list = new ArrayList<>();
	    String sql = """
	        SELECT p.firstname, p.namesPers, p.tel, m.idMember, m.balance, r.fee
	        FROM ((Inscription i
	              INNER JOIN Member m ON i.idMember = m.idMember
	              INNER JOIN Person p ON m.idPerson = p.id)
	              INNER JOIN Ride r ON i.idRide = r.idRide)
	        WHERE i.idRide = ?
	          AND i.passenger = 0
	          AND m.balance < 0
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
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return list;
	}

 // 3. Valider les paiements → on crédite simplement le balance du passager
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
             for (Integer memberId : passengerIds) {
                 ps.setInt(1, rideId);
                 ps.setInt(2, memberId);
                 ps.addBatch();
             }
             int[] results = ps.executeBatch();
             connect.commit();

             int updated = 0;
             for (int r : results) {
                 if (r > 0) updated++;
             }
             return updated;
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

    // 3. (Optionnel) Sorties avec au moins un membre encore en dette
    public List<Ride> getUnpaidRides() {
        List<Ride> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT r.idRide, r.num, r.startPlace, r.startDate, r.fee
            FROM Ride r
            JOIN Inscription i ON r.idRide = i.idRide
            JOIN Member m ON i.idMember = m.idMember
            WHERE m.balance < 0
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}