// be.ouagueni.model.Treasurer.java
package be.ouagueni.model;

import be.ouagueni.dao.TreasurerDAO;
import java.sql.Connection;
import java.util.List;

public class Treasurer extends Person {
    private static final long serialVersionUID = 1L;

    public Treasurer() {
        super();
    }

    public Treasurer(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }

    // ==============================================================
    // 1. RAPPEL DE DETTE (cotisations + covoiturage non payés)
    // ==============================================================
    public List<Member> getMembersInDebt(Connection conn) {
        return new TreasurerDAO(conn).getMembersInDebt();
    }

    // ==============================================================
    // 2. VALIDER LES PAIEMENTS COVOITURAGE (le plus utilisé en Belgique)
    // ==============================================================
    public List<Ride> getRidesWithPendingPayments(Connection conn) {
        return new TreasurerDAO(conn).getRidesWithPendingPayments();
    }

    public List<Member> getPendingPassengersForRide(Connection conn, int rideId) {
        return new TreasurerDAO(conn).getPendingPassengersForRide(rideId);
    }

    /** 
     * Valide que les passagers sélectionnés ont bien payé leur forfait (cash ou virement privé)
     * Met à jour : Inscription.paymentConfirmed = 1
     *             Member.balance += Ride.fee
     * @return nombre de membres mis à jour
     */
    public int confirmPassengerPayments(Connection conn, int rideId, List<Integer> passengerIds) {
        return new TreasurerDAO(conn).confirmPassengerPayments(rideId, passengerIds);
    }

    // ==============================================================
    // 3. RÉCLAMER / VOIR LES FRAIS NON PAYÉS (optionnel mais pratique)
    // ==============================================================
    public List<Ride> getUnpaidRides(Connection conn) {
        return new TreasurerDAO(conn).getUnpaidRides();
    }

    public List<Member> getUnpaidMembersForRide(Connection conn, int rideId) {
        return new TreasurerDAO(conn).getUnpaidMembersForRide(rideId);
    }
}