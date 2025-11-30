package be.ouagueni.model;

import be.ouagueni.dao.TreasurerDAO;
import java.sql.Connection;
import java.util.List;

public class Treasurer extends Person {
    private static final long serialVersionUID = 1L;

    public Treasurer() { super(); }
    public Treasurer(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }

    public List<Member> sendReminderLetter(Connection conn) {
        return new TreasurerDAO(conn).getMembersInDebt();
    }

    public List<Ride> payDriver(Connection conn) {
        return new TreasurerDAO(conn).getRidesWithPendingPayments();
    }

    public List<Member> getPassengersForRide(Connection conn, int rideId) {
        return new TreasurerDAO(conn).getPassengersForRide(rideId);
    }

    public int confirmPassengerPayments(Connection conn, int rideId, List<Integer> memberIds) {
        return new TreasurerDAO(conn).confirmPassengerPayments(rideId, memberIds);
    }

    public List<Ride> claimFee(Connection conn) {
        return new TreasurerDAO(conn).getUnpaidRides();
    }
}