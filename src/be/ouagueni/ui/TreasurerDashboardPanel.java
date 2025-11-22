// be.ouagueni.ui.TreasurerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.Treasurer;
import javax.swing.*;
import java.awt.*;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final java.sql.Connection conn;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, java.sql.Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // === Titre ===
        JLabel lblTitle = new JLabel(
            "Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)",
            JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(0, 100, 0));

        // === Boutons du haut ===
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 25, 25));
        topButtons.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));

        JButton btnReminder  = new JButton("Envoyer rappel de paiement");
        JButton btnPayDriver = new JButton("Valider paiements covoiturage");
        JButton btnClaimFee  = new JButton("Réclamer les frais non payés");

        // Un peu de style pour les boutons
        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 16);
        btnReminder.setFont(buttonFont);
        btnPayDriver.setFont(buttonFont);
        btnClaimFee.setFont(buttonFont);

        topButtons.add(btnReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        // Conteneur nord : titre + boutons
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(lblTitle, BorderLayout.NORTH);
        northPanel.add(topButtons, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);

        // === Message central ===
        JLabel lblCenter = new JLabel("Sélectionnez une action ci-dessus", JLabel.CENTER);
        lblCenter.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        lblCenter.setForeground(Color.GRAY);
        add(lblCenter, BorderLayout.CENTER);

        // === Bouton Déconnexion ===
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(btnLogout);
        add(southPanel, BorderLayout.SOUTH);

        // ==================================================================
        // ACTIONS – Tout délégué au Model (aucune logique métier ici !)
        // ==================================================================
        btnReminder.addActionListener(e -> showReminderLetter());
        btnPayDriver.addActionListener(e -> validatePayments());
        btnClaimFee.addActionListener(e -> claimFees());
    }

    // ==================================================================
    // Méthodes d'action (vue uniquement)
    // ==================================================================
    private void showReminderLetter() {
        var result = treasurer.prepareReminderLetter(conn);

        if (result.debtors().isEmpty()) {
            JOptionPane.showMessageDialog(this, result.previewMessage(), "Parfait", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                result.previewMessage(),
                "Envoyer les rappels ?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(
                    this,
                    result.confirmationMessage(),
                    "Rappels envoyés",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void validatePayments() {
        var result = treasurer.validateDriverPayments(conn);

        String title = result.updatedCount() == 0 ? "Info" : "Succès";
        int type = result.updatedCount() == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.INFORMATION_MESSAGE;

        JOptionPane.showMessageDialog(this, result.message(), title, type);
    }

    private void claimFees() {
        var result = treasurer.prepareClaimFee(conn);

        if (result.ride() == null) {
            JOptionPane.showMessageDialog(this, result.formattedMessage(), "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, result.formattedMessage(), "Membres à réclamer", JOptionPane.WARNING_MESSAGE);
        }
    }
}