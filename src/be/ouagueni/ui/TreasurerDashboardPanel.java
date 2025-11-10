package be.ouagueni.ui;

import be.ouagueni.model.Treasurer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final Connection conn;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
                "Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 130, 80));

        // --- Panel des boutons ---
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton btnSendReminder = new JButton("📧 Envoyer rappel lettre");
        JButton btnPayDriver = new JButton("💶 Payer conducteur");
        JButton btnClaimFee = new JButton("💰 Réclamer frais");
        topButtons.add(btnSendReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);
        add(northPanel, BorderLayout.NORTH);

        // --- Zone centrale d’infos ---
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel lblInfo = new JLabel("Tableau de bord du Trésorier");
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        contentPanel.add(lblInfo);
        contentPanel.add(Box.createVerticalStrut(20));

        JTextArea txtInfo = new JTextArea(
                "Fonctionnalités disponibles :\n\n" +
                        "• Envoyer des lettres de rappel aux membres\n" +
                        "• Effectuer les paiements aux conducteurs\n" +
                        "• Réclamer les frais des balades\n"
        );
        txtInfo.setEditable(false);
        txtInfo.setBackground(new Color(245, 250, 245));
        txtInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(txtInfo);

        JScrollPane scroll = new JScrollPane(contentPanel);
        add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnLogout = new JButton("🔙 Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions des boutons ---
        btnSendReminder.addActionListener(this::sendReminderLetter);
        btnPayDriver.addActionListener(this::payDriver);
        btnClaimFee.addActionListener(this::claimFee);
    }

    // --- Actions (à compléter) ---
    private void sendReminderLetter(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Envoyer rappel lettre");
    }

    private void payDriver(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Payer conducteur");
    }

    private void claimFee(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Réclamer frais");
    }
}
