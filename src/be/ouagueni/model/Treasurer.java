// be.ouagueni.model.Treasurer.java
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

    // 1. Rappel → retourne juste la liste des membres en dette
    public List<Member> sendReminderLetter(Connection conn) {
        return new TreasurerDAO(conn).getMembersInDebt();
    }

    // 2. Valider les paiements → retourne le nombre de paiements validés
    public int payDriver(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        List<Ride> pendingRides = dao.getRidesWithPendingPayments();
        if (pendingRides.isEmpty()) return 0;

        // On prend la première sortie en attente (ou tu peux en choisir une dans l'IHM)
        Ride ride = pendingRides.get(0);
        List<Member> passengers = dao.getPendingPassengersForRide(ride.getId());

        List<Integer> ids = passengers.stream()
                                      .map(Member::getIdMember)
                                      .toList();

        return dao.confirmPassengerPayments(ride.getId(), ids);
    }

    // 3. Réclamer frais → retourne une paire (sortie + liste des membres en dette)
    public record ClaimResult(Ride ride, List<Member> unpaidMembers) {}
    
    public ClaimResult claimFee(Connection conn)
    {
        TreasurerDAO dao = new TreasurerDAO(conn);
        List<Ride> rides = dao.getUnpaidRides();
        if (rides.isEmpty()) return new ClaimResult(null, List.of());

        Ride ride = rides.get(0); // première sortie avec dette
        List<Member> unpaid = dao.getUnpaidMembersForRide(ride.getId());
        return new ClaimResult(ride, unpaid);
    }
}