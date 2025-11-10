package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class MemberDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private final Member currentMember;

    public MemberDashboardPanel(ClubFrame parentFrame, Member member, Connection conn) {
        this.parentFrame = parentFrame;
        this.currentMember = member;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
                "Bienvenue " + member.getFirstname() + " " + member.getName() + " (Membre)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 80, 130));

        // --- Boutons en haut ---
        JPanel topButtons = new JPanel(new GridLayout(2, 2, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnDisponibilite = new JButton("🗓️ Poster mes disponibilités");
        JButton btnReserver = new JButton("🚴 Réserver une balade");
        JButton btnChoisirCategorie = new JButton("🏷️ Choisir une catégorie");
        JButton btnMesInscriptions = new JButton("📋 Voir mes inscriptions");

        topButtons.add(btnDisponibilite);
        topButtons.add(btnReserver);
        topButtons.add(btnChoisirCategorie);
        topButtons.add(btnMesInscriptions);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        add(northPanel, BorderLayout.NORTH);

        // --- Liste des inscriptions (au centre) ---
        JPanel listPanel = createMemberInscriptionsPanel(member);
        JScrollPane scroll = new JScrollPane(listPanel);
        add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnDeconnexion = new JButton("🔙 Déconnexion");
        btnDeconnexion.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDeconnexion);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions ---
        btnDisponibilite.addActionListener(this::ouvrirDisponibilite);
        btnReserver.addActionListener(this::ouvrirReservation);
        btnChoisirCategorie.addActionListener(e -> JOptionPane.showMessageDialog(this, "Fonctionnalité à venir : Choisir catégorie"));
    }

    /** Ouvre la fenêtre de gestion des disponibilités */
    private void ouvrirDisponibilite(ActionEvent e) {
        Set<Ride> ridesSet = Ride.allRides(conn); // non-statique
        if (ridesSet.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie disponible.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Ride> rideList = new ArrayList<>(ridesSet);

        // 2. Créer la fenêtre
        JDialog dialog = new JDialog(parentFrame, "Poster mes disponibilités", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // --- Panel gauche : Liste des rides ---
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Sélectionner une sortie"));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Ride r : rideList) {
            String display = String.format("%s - %s (%s)",
                r.getStartDate().toLocalDate(),
                r.getStartPlace(),
                r.getCalendar().getCategory().getNomCategorie()
            );
            listModel.addElement(display);
        }

        JList<String> rideListUI = new JList<>(listModel);
        rideListUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rideListUI.setSelectedIndex(0);
        JScrollPane scrollPane = new JScrollPane(rideListUI);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Panel droit : Formulaire ---
        JPanel rightPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Places disponibles"));

        rightPanel.add(new JLabel("Places passagers :"));
        JSpinner seatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        rightPanel.add(seatSpinner);

        rightPanel.add(new JLabel("Places vélo :"));
        JSpinner bikeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
        rightPanel.add(bikeSpinner);

        // --- Panel bas : Boutons ---
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton validerBtn = new JButton("Valider");
        JButton annulerBtn = new JButton("Annuler");

        annulerBtn.addActionListener(e2 -> dialog.dispose());

        validerBtn.addActionListener(e2 -> {
            int selectedIndex = rideListUI.getSelectedIndex();
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(dialog, "Veuillez sélectionner une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Ride rideSelectionnee = rideList.get(selectedIndex);
            int seats = (Integer) seatSpinner.getValue();
            int bikeSpots = (Integer) bikeSpinner.getValue();

            // Appeler la méthode du membre connecté
            try {
            		currentMember.postAvailability(rideSelectionnee, seats, bikeSpots,conn);
                JOptionPane.showMessageDialog(dialog, 
                    "Disponibilités postées !\n" +
                    seats + " places passager, " + bikeSpots + " places vélo",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomPanel.add(validerBtn);
        bottomPanel.add(annulerBtn);

        // --- Assemblage ---
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /** Ouvre la fenêtre de réservation */
    private void ouvrirReservation(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Réserver une balade");
    }

    /** Crée le panneau listant toutes les inscriptions du membre */
    private JPanel createMemberInscriptionsPanel(Member member) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<Inscription> inscriptions = member.getInscriptions();
        if (inscriptions == null || inscriptions.isEmpty()) {
            listPanel.add(new JLabel("Aucune inscription trouvée."));
            return listPanel;
        }

        for (Inscription ins : inscriptions) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createTitledBorder("Inscription #" + ins.getId()));

            StringBuilder sb = new StringBuilder();
            sb.append("Passager : ").append(ins.isPassenger() ? "Oui" : "Non").append("\n");
            sb.append("Avec vélo : ").append(ins.isBike() ? "Oui" : "Non").append("\n");

            if (ins.getBikeObj() != null) {
                Bike b = ins.getBikeObj();
                sb.append("Vélo : ").append(b.getType())
                  .append(" - ").append(b.getWeight()).append(" kg\n");
            }

            if (ins.getRide() != null) {
                Ride r = ins.getRide();
                sb.append("Trajet ").append(r.getnum())
                  .append(" depuis ").append(r.getStartPlace())
                  .append(" le ").append(r.getStartDate())
                  .append(" - ").append(r.getFee()).append(" €\n");
            }

            if (ins.getRide() != null && ins.getRide().getCalendar() != null) {
                Calendar cal = ins.getRide().getCalendar();
                if (cal.getCategory() != null && cal.getCategory().getNomCategorie() != null) {
                    sb.append("Catégorie : ").append(cal.getCategory().getNomCategorie().name()).append("\n");
                }
            }

            JTextArea txt = new JTextArea(sb.toString());
            txt.setEditable(false);
            txt.setBackground(new Color(245, 245, 245));
            card.add(txt);
            listPanel.add(card);
        }

        return listPanel;
    }
}
