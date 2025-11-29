package be.ouagueni.model;

import be.ouagueni.dao.TreasurerDAO;
import javax.swing.*;
import java.sql.Connection;
import java.util.List;

public class Treasurer extends Person {
    private static final long serialVersionUID = 1L;

    // ==================================================================
    // Constructeurs
    // ==================================================================
    public Treasurer() { super(); }

    public Treasurer(int id, String name, String firstname, String tel, String password) {
        super(id, name, firstname, tel, password);
    }

    // ==================================================================
    // 1. ENVOYER RAPPEL DE PAIEMENT → sendReminderLetter()
    // ==================================================================
    public void sendReminderLetter(Connection conn) {
        List<Member> debtors = new TreasurerDAO(conn).getMembersInDebt();

        if (debtors.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Tous les membres sont à jour !",
                "Parfait",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double total = debtors.stream().mapToDouble(m -> -m.getBalance()).sum();

        StringBuilder preview = new StringBuilder("<html><b>Membres en dette :</b><br><br>");
        for (Member m : debtors) {
            double due = -m.getBalance();
            preview.append(String.format("• %s %s → %.2f €<br>", m.getFirstname(), m.getName(), due));
        }
        preview.append("<br><b>Total dû : ").append(String.format("%.2f €", total)).append("</b></html>");

        int choice = JOptionPane.showConfirmDialog(null,
            preview.toString(),
            "Envoyer les rappels ?",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            String msg = debtors.size() == 1
                ? "1 personne va recevoir un mail de rappel."
                : debtors.size() + " personnes vont recevoir un mail de rappel.";

            JOptionPane.showMessageDialog(null, msg, "Rappels envoyés", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================================================================
    // 2. VALIDER PAIEMENTS COVOITURAGE → payDriver()
    // ==================================================================
    public void payDriver(Connection conn) {
        List<Ride> rides = new TreasurerDAO(conn).getRidesWithPendingPayments();

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Aucune sortie passée avec des passagers à valider.",
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Choix de la sortie
        Ride selected = (Ride) JOptionPane.showInputDialog(
            null,
            "Sélectionnez la sortie à traiter :",
            "Valider paiements covoiturage",
            JOptionPane.QUESTION_MESSAGE,
            null,
            rides.toArray(),
            rides.get(0)
        );

        if (selected == null) return;

        List<Member> passengers = new TreasurerDAO(conn).getPassengersForRide(selected.getId());

        if (passengers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Aucun passager inscrit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Liste avec cases à cocher
        JList<Member> list = new JList<>(passengers.toArray(new Member[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer((jlist, m, index, isSel, hasFocus) -> {
            String txt = String.format("%s %s → Solde : %.2f €", m.getFirstname(), m.getName(), m.getBalance());
            JLabel lbl = new JLabel(txt);
            if (isSel) {
                lbl.setBackground(new java.awt.Color(184, 207, 229));
                lbl.setOpaque(true);
            }
            return lbl;
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new java.awt.Dimension(500, 350));

        int option = JOptionPane.showConfirmDialog(null, scroll,
            "Cochez les membres qui ont payé – " + selected.getStartPlace(),
            JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        List<Integer> selectedIds = list.getSelectedValuesList()
            .stream()
            .map(Member::getIdMember)
            .toList();

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Aucun membre coché.", "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int updated = new TreasurerDAO(conn).confirmPassengerPayments(selected.getId(), selectedIds);

        String message = updated == 1
            ? "1 paiement validé avec succès !"
            : updated + " paiements validés avec succès !";

        JOptionPane.showMessageDialog(null, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================================================================
    // 3. RÉCLAMER LES FRAIS → claimFee()
    // ==================================================================
    public void claimFee(Connection conn) {
        List<Ride> rides = new TreasurerDAO(conn).getUnpaidRides();

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "Aucune sortie avec passagers.",
                "Information",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Ride selected = (Ride) JOptionPane.showInputDialog(
            null,
            "Sélectionnez la sortie à réclamer :",
            "Réclamer les frais",
            JOptionPane.QUESTION_MESSAGE,
            null,
            rides.toArray(),
            rides.get(0)
        );

        if (selected == null) return;

        List<Member> passengers = new TreasurerDAO(conn).getPassengersForRide(selected.getId());

        if (passengers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Aucun passager inscrit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("<html><b>Dettes pour la sortie :</b> ")
            .append(selected.getStartPlace())
            .append(" (")
            .append(selected.getStartDate().toLocalDate())
            .append(")<br><br>");

        for (Member m : passengers) {
            msg.append("• ").append(m.getFirstname()).append(" ").append(m.getName()).append("<br>");
        }
        msg.append("</html>");

        JOptionPane.showMessageDialog(null,
            msg.toString(),
            "Membres à réclamer",
            JOptionPane.WARNING_MESSAGE);
    }
}