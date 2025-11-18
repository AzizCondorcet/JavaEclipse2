// be.ouagueni.ui.TreasurerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final Connection conn;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Titre
        JLabel lblTitle = new JLabel("Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(new Color(0, 100, 0));

        // Boutons
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 20, 20));
        topButtons.setBorder(BorderFactory.createEmptyBorder(20, 80, 20, 80));

        JButton btnReminder  = new JButton("Envoyer rappel de paiement");
        JButton btnPayDriver = new JButton("Valider paiements covoiturage");
        JButton btnClaimFee  = new JButton("Réclamer les frais non payés");

        topButtons.add(btnReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        JPanel north = new JPanel(new BorderLayout());
        north.add(lblTitle, BorderLayout.NORTH);
        north.add(topButtons, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // Message central
        add(new JLabel("Sélectionnez une action ci-dessus", JLabel.CENTER), BorderLayout.CENTER);

        // Déconnexion
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        add(south, BorderLayout.SOUTH);

        // ACTIONS → tout l'affichage est ici
        btnReminder.addActionListener(e -> showReminderLetter());
        btnPayDriver.addActionListener(e -> validatePayments());
        btnClaimFee.addActionListener(e -> claimFees());
    }

    private void showReminderLetter() {
        List<Member> debtors = treasurer.sendReminderLetter(conn);
        if (debtors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les membres sont à jour !", "Parfait", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("<html><b>Membres en dette :</b><br><br>");
        double total = 0;
        for (Member m : debtors) {
            double due = -m.getBalance();
            total += due;
            msg.append(String.format("• %s %s → %.2f €<br>", m.getFirstname(), m.getName(), due));
        }
        msg.append("<br><b>Total dû : ").append(String.format("%.2f €", total)).append("</b></html>");

        int choice = JOptionPane.showConfirmDialog(this, msg.toString(), "Envoyer les rappels ?", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, debtors.size() + " rappel(s) envoyé(s) !", "Succès", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void validatePayments() {
        int updated = treasurer.payDriver(conn);
        if (updated == 0) {
            JOptionPane.showMessageDialog(this, "Aucun paiement à valider ou déjà tout réglé !", "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, updated + " paiement(s) validé(s) avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void claimFees() {
        Treasurer.ClaimResult result = treasurer.claimFee(conn);
        if (result.ride() == null) {
            JOptionPane.showMessageDialog(this, "Aucune dette en cours.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("<html><b>Dettes pour la sortie :</b> ")
                .append(result.ride().getStartPlace()).append(" (")
                .append(result.ride().getStartDate().toLocalDate()).append(")<br><br>");

        for (Member m : result.unpaidMembers()) {
            msg.append("• ").append(m.getFirstname()).append(" ").append(m.getName()).append("<br>");
        }
        msg.append("</html>");

        JOptionPane.showMessageDialog(this, msg.toString(), "Membres à réclamer", JOptionPane.WARNING_MESSAGE);
    }
}