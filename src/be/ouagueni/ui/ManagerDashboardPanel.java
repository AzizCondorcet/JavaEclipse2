// be.ouagueni.ui.ManagerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ManagerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Manager manager;

    public ManagerDashboardPanel(ClubFrame parentFrame, Manager manager) {
        this.parentFrame = parentFrame;
        this.manager = manager;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // === TITRE ===
        JLabel title = new JLabel("Manager : " + manager.getFirstname() + " " + manager.getName(), JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(100, 0, 100));

        // === BOUTONS HAUT ===
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 30, 0));
        topButtons.setBorder(BorderFactory.createEmptyBorder(30, 100, 40, 100));

        JButton btnCreate = new JButton("Créer une Balade");
        JButton btnOptim = new JButton("Optimisation Covoiturage");

        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnOptim.setFont(new Font("Segoe UI", Font.BOLD, 18));

        topButtons.add(new JLabel()); // espace gauche
        topButtons.add(btnCreate);
        topButtons.add(btnOptim);

        JPanel north = new JPanel(new BorderLayout());
        north.add(title, BorderLayout.NORTH);
        north.add(topButtons, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        // === CONTENU CENTRAL : liste des balades ===
        JPanel center = createRidesPanel();
        add(new JScrollPane(center), BorderLayout.CENTER);

        // === DÉCONNEXION ===
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        add(south, BorderLayout.SOUTH);

        // === ACTIONS ===
        btnCreate.addActionListener(e -> showCreateBaladePanel());
        btnOptim.addActionListener(e -> showOptimisationDialog());
    }

    private JPanel createRidesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Category cat = manager.getCategory();
        if (cat == null) {
            panel.add(new JLabel("Aucune catégorie assignée au manager.", JLabel.CENTER));
            return panel;
        }

        panel.add(Box.createVerticalStrut(10));
        JLabel lblCat = new JLabel("Catégorie gérée : " + cat.getNomCategorie().name(), JLabel.CENTER);
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(lblCat);
        panel.add(Box.createVerticalStrut(20));

        // ON RÉCUPÈRE LES RIDES VIA APPMODEL (100 % fiable maintenant)
        List<Ride> rides = AppModel.getInstance().getRidesDuManager(manager);

        if (rides.isEmpty()) {
            panel.add(new JLabel("Aucune balade programmée pour le moment.", JLabel.CENTER));
            panel.add(Box.createVerticalStrut(200));
        } else {
            JLabel lblTitle = new JLabel("Balades programmées :", JLabel.LEFT);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            panel.add(lblTitle);
            panel.add(Box.createVerticalStrut(10));

            for (Ride ride : rides) {
                panel.add(createRideCard(ride));
                panel.add(Box.createVerticalStrut(12));
            }
        }

        panel.add(Box.createVerticalGlue()); // pousse tout en haut
        return panel;
    }

    private JPanel createRideCard(Ride ride) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 50, 150), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(new Color(250, 245, 255));

        String date = ride.getStartDate() != null
            ? ride.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy à HH:mm"))
            : "Date inconnue";

        JLabel lblTitle = new JLabel(ride.getStartPlace() + " — " + date);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel lblInfos = new JLabel(String.format(
            "<html><b>Frais :</b> %.2f € &nbsp;&nbsp;|&nbsp;&nbsp; " +
            "<b>Inscrits :</b> %d &nbsp;&nbsp;|&nbsp;&nbsp; " +
            "<b>Conducteurs :</b> %d</html>",
            ride.getFee(),
            ride.getInscriptions().size(),
            (int) ride.getVehicles().stream().filter(v -> v.getDriver() != null).count()
        ));

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblInfos);

        return card;
    }

    private void showCreateBaladePanel() {
        parentFrame.addPanel(new CreateBaladePanel(parentFrame, manager), "createBalade");
        parentFrame.showPanel("createBalade");
    }

    private void showOptimisationDialog() {
        List<Ride> ridesAvecInscrits = AppModel.getInstance().getRidesAvecInscriptions(manager);

        if (ridesAvecInscrits.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucune balade avec inscriptions à optimiser.",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Fenêtre d'optimisation (inchangée, elle marche nickel)
        JDialog dialog = new JDialog(parentFrame, "Optimisation Covoiturage – " + manager.getFirstname(), true);
        dialog.setSize(1100, 700);
        dialog.setLocationRelativeTo(this);

        DefaultListModel<Ride> model = new DefaultListModel<>();
        ridesAvecInscrits.forEach(model::addElement);

        JList<Ride> listRides = new JList<>(model);
        listRides.setCellRenderer((list, ride, index, sel, focus) -> {
            int inscrits = ride.getInscriptions().size();
            int conducteurs = (int) ride.getVehicles().stream().filter(v -> v.getDriver() != null).count();
            String text = String.format("%s — %s — %d inscrit(s) — %d conducteur(s)",
                ride.getStartDate().toLocalDate(),
                ride.getStartPlace(), inscrits, conducteurs);
            JLabel lbl = new JLabel(text);
            if (sel) lbl.setBackground(list.getSelectionBackground());
            lbl.setOpaque(true);
            return lbl;
        });

        JTextArea rapportArea = new JTextArea();
        rapportArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        rapportArea.setEditable(false);

        listRides.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Ride selected = listRides.getSelectedValue();
                if (selected != null) {
                    Ride rideFresh = AppModel.getInstance().rafraichirRideDepuisBase(selected.getId());
                    if (rideFresh != null) {
                        String rapport = AppModel.getInstance().genererRapportOptimisationCovoiturage(rideFresh);
                        rapportArea.setText(rapport);
                        rapportArea.setCaretPosition(0);
                    }
                }
            }
        });

        if (!ridesAvecInscrits.isEmpty()) listRides.setSelectedIndex(0);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(listRides), new JScrollPane(rapportArea));
        split.setDividerLocation(400);

        JButton close = new JButton("Fermer");
        close.addActionListener(e -> dialog.dispose());

        dialog.setLayout(new BorderLayout());
        dialog.add(split, BorderLayout.CENTER);
        dialog.add(close, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}