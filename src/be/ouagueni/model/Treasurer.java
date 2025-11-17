// be.ouagueni.model.Treasurer.java
package be.ouagueni.model;

import be.ouagueni.dao.TreasurerDAO;
import java.sql.Connection;
import java.util.List;

public class Treasurer extends Person {
    private static final long serialVersionUID = -7297704180550272373L;

    public Treasurer() { super(); }
    public Treasurer(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }

    // ========================================
    // 1. RAPPEL LETTRE
    // ========================================
    public List<Member> getMembersInDebt(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        return dao.getMembersInDebt();
    }

    // ========================================
    // 2. PAYER CONDUCTEUR
    // ========================================
    public List<Object[]> getDriverPayments(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        return dao.getDriverPayments();
    }

    public boolean payDriver(Connection conn, int memberId, double amount) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        return dao.payDriver(memberId, amount);
    }

    // ========================================
    // 3. RÉCLAMER FRAIS
    // ========================================
    public List<Ride> getUnpaidRides(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        return dao.getUnpaidRides();
    }

    public List<Member> getUnpaidMembersForRide(Connection conn, int rideId) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        return dao.getUnpaidMembersForRide(rideId);
    }
}