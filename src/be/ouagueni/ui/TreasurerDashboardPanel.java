// be.ouagueni.ui.TreasurerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.Treasurer;
import javax.swing.*;
import java.awt.*;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final java.sql.Connection conn;

    // Boutons (on les garde en attribut pour plus de clarté)
    private JButton btnReminder;
    private JButton btnPayDriver;
    private JButton btnClaimFee;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, java.sql.Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;

        initUI();
        setupActions();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Titre
        JLabel lblTitle = new JLabel("Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(0, 100, 0));

        // Boutons centraux
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 30, 30));
        topButtons.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        btnReminder  = new JButton("Envoyer rappel de paiement");
        btnPayDriver = new JButton("Valider paiements covoiturage");
        btnClaimFee  = new JButton("Réclamer les frais non payés");

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 18);
        Dimension buttonSize = new Dimension(300, 80);

        for (JButton btn : new JButton[]{btnReminder, btnPayDriver, btnClaimFee}) {
            btn.setFont(buttonFont);
            btn.setPreferredSize(buttonSize);
            btn.setFocusPainted(false);
        }

        topButtons.add(btnReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        // Assemblage
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(lblTitle, BorderLayout.NORTH);
        northPanel.add(topButtons, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);
        add(new JLabel("Sélectionnez une action ci-dessus", JLabel.CENTER), BorderLayout.CENTER);

        // Bouton déconnexion
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(btnLogout);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        // SIMPLE. PROPRE. PARFAIT.
        btnReminder.addActionListener( e -> treasurer.sendReminderLetter(conn) );
        btnPayDriver.addActionListener( e -> treasurer.payDriver(conn) );
        btnClaimFee.addActionListener(  e -> treasurer.claimFee(conn) );
    }
}