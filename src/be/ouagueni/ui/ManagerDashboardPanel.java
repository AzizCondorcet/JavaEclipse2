package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;

public class ManagerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Manager manager;
    private final Connection conn;

    public ManagerDashboardPanel(ClubFrame parentFrame, Manager manager, Connection conn) {
        this.parentFrame = parentFrame;
        this.manager = manager;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
                "Bienvenue " + manager.getFirstname() + " " + manager.getName() + " (Manager)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(130, 20, 80));

        // --- Panel des boutons ---
        JPanel topButtons = new JPanel(new GridLayout(1, 2, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JButton btnCreateBalade = new JButton("Créer une Balade");
        JButton btnViewBalades = new JButton("Voir toutes les balades");
        topButtons.add(btnCreateBalade);
        topButtons.add(btnViewBalades);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);
        add(northPanel, BorderLayout.NORTH);

        // --- Zone centrale : catégorie + balades ---
        JPanel contentPanel = createManagerContentPanel(manager);
        JScrollPane scroll = new JScrollPane(contentPanel);
        add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnLogout = new JButton("🔙 Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLogout);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions des boutons ---
        btnCreateBalade.addActionListener(this::showCreateBaladePanel);
        btnViewBalades.addActionListener(e -> JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Liste des balades"));
    }

    /** Contenu du manager : catégorie + balades */
    private JPanel createManagerContentPanel(Manager manager) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        Category category = manager.getCategory();
        if (category == null) {
            contentPanel.add(new JLabel("Aucune catégorie assignée."));
            return contentPanel;
        }

        // --- Catégorie ---
        JPanel categoryPanel = new JPanel();
        categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Votre Catégorie"));
        categoryPanel.add(new JLabel("Type : " + category.getNomCategorie().name()));
        contentPanel.add(categoryPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // --- Balades ---
        Calendar calendar = category.getCalendar();
        if (calendar != null && calendar.getRides() != null && !calendar.getRides().isEmpty()) {
            JLabel lblRides = new JLabel("Balades de votre catégorie :");
            lblRides.setFont(new Font("Segoe UI", Font.BOLD, 16));
            contentPanel.add(lblRides);
            contentPanel.add(Box.createVerticalStrut(10));
            for (Ride ride : calendar.getRides()) {
                JPanel rideCard = new JPanel();
                rideCard.setLayout(new BoxLayout(rideCard, BoxLayout.Y_AXIS));
                rideCard.setBorder(BorderFactory.createTitledBorder("Balade #" + ride.getnum()));
                StringBuilder sb = new StringBuilder();
                sb.append("Lieu de départ : ").append(ride.getStartPlace()).append("\n");
                sb.append("Date : ").append(ride.getStartDate()).append("\n");
                sb.append("Frais : ").append(ride.getFee()).append(" €\n");
                if (ride.getInscriptions() != null) {
                    sb.append("Inscrits : ").append(ride.getInscriptions().size()).append("\n");
                }
                JTextArea txt = new JTextArea(sb.toString());
                txt.setEditable(false);
                txt.setBackground(new Color(245, 250, 245));
                rideCard.add(txt);
                contentPanel.add(rideCard);
                contentPanel.add(Box.createVerticalStrut(10));
            }
        } else {
            contentPanel.add(new JLabel("Aucune balade programmée pour votre catégorie."));
        }

        return contentPanel;
    }

    /** Méthode pour ouvrir la création de balade */
    /** Méthode pour ouvrir la création de balade */
    private void showCreateBaladePanel(ActionEvent e) {
        // Crée le panel de création de balade
        CreateBaladePanel createPanel = new CreateBaladePanel(parentFrame, manager);

        // Ajoute-le au CardLayout du parentFrame
        parentFrame.addPanel(createPanel, "createBalade");

        // Affiche-le
        parentFrame.showPanel("createBalade");
    }

}
