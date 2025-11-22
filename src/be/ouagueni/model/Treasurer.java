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

    // ==================================================================
    // 1. RAPPEL DE PAIEMENT
    // ==================================================================
    public record ReminderResult(
            List<Member> debtors,
            double totalDue,
            String previewMessage,      // Affiché dans la boîte de confirmation
            String confirmationMessage  // Affiché après "Oui"
    ) {}

    public ReminderResult prepareReminderLetter(Connection conn) {
        List<Member> debtors = new TreasurerDAO(conn).getMembersInDebt();

        if (debtors.isEmpty()) {
            return new ReminderResult(
                    List.of(),
                    0.0,
                    "Tous les membres sont à jour !",
                    "Aucun rappel à envoyer."
            );
        }

        double total = debtors.stream()
                .mapToDouble(m -> -m.getBalance())
                .sum();

        StringBuilder preview = new StringBuilder("<html><b>Membres en dette :</b><br><br>");
        for (Member m : debtors) {
            double due = -m.getBalance();
            preview.append(String.format("• %s %s → %.2f €<br>", m.getFirstname(), m.getName(), due));
        }
        preview.append("<br><b>Total dû : ").append(String.format("%.2f €", total)).append("</b></html>");

        String confirmation = debtors.size() == 1
                ? "1 personne va recevoir un mail de rappel."
                : debtors.size() + " personnes vont recevoir un mail de rappel.";

        return new ReminderResult(debtors, total, preview.toString(), confirmation);
    }

    // ==================================================================
    // 2. VALIDER PAIEMENTS COVOITURAGE
    // ==================================================================
    public record PaymentValidationResult(int updatedCount, String message) {}

    public PaymentValidationResult validateDriverPayments(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        List<Ride> pendingRides = dao.getRidesWithPendingPayments();

        if (pendingRides.isEmpty()) {
            return new PaymentValidationResult(0, "Aucun paiement à valider ou déjà tout réglé !");
        }

        Ride ride = pendingRides.get(0);
        List<Member> passengers = dao.getPendingPassengersForRide(ride.getId());
        List<Integer> ids = passengers.stream()
                .map(Member::getIdMember)
                .toList();

        int updated = dao.confirmPassengerPayments(ride.getId(), ids);

        String message = updated == 1
                ? "1 paiement validé avec succès !"
                : updated + " paiements validés avec succès !";

        return new PaymentValidationResult(updated, message);
    }

    // ==================================================================
    // 3. RÉCLAMER LES FRAIS NON PAYÉS
    // ==================================================================
    public record ClaimResult(
        Ride ride,
        List<Member> unpaidMembers,
        String formattedMessage
    ) {}

    public ClaimResult prepareClaimFee(Connection conn) {
        TreasurerDAO dao = new TreasurerDAO(conn);
        List<Ride> rides = dao.getUnpaidRides();

        if (rides.isEmpty()) {
            return new ClaimResult(null, List.of(), "Aucune dette en cours.");
        }

        Ride ride = rides.get(0);
        List<Member> unpaid = dao.getUnpaidMembersForRide(ride.getId());

        StringBuilder msg = new StringBuilder("<html><b>Dettes pour la sortie :</b> ")
                .append(ride.getStartPlace())
                .append(" (")
                .append(ride.getStartDate().toLocalDate())
                .append(")<br><br>");

        for (Member m : unpaid) {
            msg.append("• ").append(m.getFirstname()).append(" ").append(m.getName()).append("<br>");
        }
        msg.append("</html>");

        return new ClaimResult(ride, unpaid, msg.toString());
    }
}