package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class ManagerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Manager manager;

    public ManagerDashboardPanel(ClubFrame parentFrame, Manager manager, Connection conn) {
        this.parentFrame = parentFrame;
        this.manager = manager;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Titre
        JLabel lblTitle = new JLabel("Bienvenue " + manager.getFirstname() + " " + manager.getName() + " (Manager)", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(130, 20, 80));

        // Boutons du haut
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 10, 10));
        JButton btnCreate = new JButton("Créer une Balade");
        JButton btnViewAll = new JButton("Voir toutes les balades");
        JButton btnOptim = new JButton("Optimisation covoiturage");
        btnOptim.setToolTipText("Voir le plan de covoiturage optimisé");

        topButtons.add(btnCreate);
        topButtons.add(btnViewAll);
        topButtons.add(btnOptim);

        JPanel north = new JPanel(new BorderLayout());
        north.add(lblTitle, BorderLayout.NORTH);
        north.add(topButtons, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);

        // Contenu central → on délègue tout au modèle
        JPanel content = createContentFromModel();
        add(new JScrollPane(content), BorderLayout.CENTER);

        // Bouton déconnexion
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        add(south, BorderLayout.SOUTH);

        // Actions
        btnCreate.addActionListener(e -> showCreateBaladePanel());
        btnOptim.addActionListener(e -> ouvrirOptimisationCovoiturage());
        btnViewAll.addActionListener(e -> JOptionPane.showMessageDialog(this, "Fonctionnalité à venir"));
    }

    private JPanel createContentFromModel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Category cat = manager.getCategory();
        if (cat == null) {
            panel.add(new JLabel("Aucune catégorie assignée."));
            return panel;
        }

        // Affichage catégorie
        panel.add(createCategoryBox(cat));

        // Affichage des balades → on utilise uniquement AppModel
        List<Ride> rides = AppModel.getInstance().getRidesDuManager(manager);
        if (rides.isEmpty()) {
            panel.add(new JLabel("Aucune balade programmée."));
        } else {
            JLabel lbl = new JLabel("Balades de votre catégorie :");
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            panel.add(lbl);
            panel.add(Box.createVerticalStrut(10));

            for (Ride ride : rides) {
                panel.add(createRideCard(ride));
                panel.add(Box.createVerticalStrut(10));
            }
        }
        return panel;
    }

    private JPanel createCategoryBox(Category cat) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Votre Catégorie"));
        p.add(new JLabel("Type : " + cat.getNomCategorie().name()));
        return p;
    }

    private JPanel createRideCard(Ride ride) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Balade #" + ride.getnum()));

        String info = String.format(
            "Lieu : %s\nDate : %s\nFrais : %.2f €\nInscrits : %d",
            ride.getStartPlace(),
            ride.getStartDate(),
            ride.getFee(),
            ride.getInscriptions() != null ? ride.getInscriptions().size() : 0
        );

        JTextArea txt = new JTextArea(info);
        txt.setEditable(false);
        txt.setBackground(new Color(245, 250, 245));
        card.add(txt);
        return card;
    }

    private void showCreateBaladePanel() {
        CreateBaladePanel panel = new CreateBaladePanel(parentFrame, manager);
        parentFrame.addPanel(panel, "createBalade");
        parentFrame.showPanel("createBalade");
    }

    private void ouvrirOptimisationCovoiturage() {
        List<Ride> rides = AppModel.getInstance().getRidesAvecInscriptions(manager);
        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie avec inscriptions.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parentFrame, "Optimisation covoiturage", true);
        dialog.setSize(1000, 720);
        dialog.setLocationRelativeTo(this);

        DefaultListModel<Ride> model = new DefaultListModel<>();
        rides.forEach(model::addElement);

        JList<Ride> list = new JList<>(model);
        list.setCellRenderer((list1, value, index, isSelected, cellHasFocus) -> {
            Ride r = (Ride) value;
            String catName = r.getCalendar() != null && r.getCalendar().getCategory() != null
                    ? r.getCalendar().getCategory().getNomCategorie().name() : "NC";
            int inscrits = r.getInscriptions().size();
            int voitures = (int) r.getVehicles().stream().filter(v -> v.getDriver() != null).count();
            JLabel label = new JLabel(String.format("%s — %s (%s) — %d inscrits — %d voiture(s)",
                    r.getStartDate().toLocalDate(), r.getStartPlace(), catName, inscrits, voitures));
            label.setOpaque(true);
            if (isSelected) label.setBackground(list1.getSelectionBackground());
            return label;
        });

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Ride selected = list.getSelectedValue();
                if (selected != null) {
                    // LA MAGIE : on rafraîchit la sortie depuis la base AVANT l'optimisation
                    Ride rideAjour = AppModel.getInstance().rafraichirRideDepuisBase(selected.getId());

                    if (rideAjour != null) {
                        String rapport = AppModel.getInstance().genererRapportOptimisationCovoiturage(rideAjour);
                        textArea.setText(rapport);
                        textArea.setCaretPosition(0);
                    } else {
                        textArea.setText("Erreur : sortie introuvable.");
                    }
                }
            }
        });

        // Premier affichage
        if (!rides.isEmpty()) {
            list.setSelectedIndex(0);
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(textArea));
        split.setDividerLocation(380);

        JButton fermer = new JButton("Fermer");
        fermer.addActionListener(e -> dialog.dispose());

        dialog.setLayout(new BorderLayout());
        dialog.add(split, BorderLayout.CENTER);
        dialog.add(fermer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}