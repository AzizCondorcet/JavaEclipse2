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

        topButtons.add(new JLabel()); 
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

        // ON RÉCUPÈRE LES RIDES VIA APPMODEL
        List<Ride> rides = AppModel.getInstance().getRidesDuManagerAvecStatus(manager);

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

    private JPanel createRideCard(Ride ride) {  // ← RIEN NE CHANGE !
    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    
    // ✅ STATUS VIA APPMODEL
    AppModel appModel = AppModel.getInstance();
    boolean terminee = appModel.isRideTerminee(ride);
    String status = appModel.getRideStatus(ride);
    
    // ✅ COULEUR DYNAMIQUE
    Color borderColor = terminee ? new Color(0, 150, 0) : new Color(150, 50, 150);
    Color bgColor = terminee ? new Color(240, 255, 240) : new Color(250, 245, 255);
    
    card.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(borderColor, 2),
        BorderFactory.createEmptyBorder(15, 15, 15, 15)
    ));
    card.setBackground(bgColor);

    String date = ride.getStartDate() != null
        ? ride.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy à HH:mm"))
        : "Date inconnue";

    JLabel lblTitle = new JLabel(ride.getStartPlace() + " — " + date);
    lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

    // ✅ STATUS DU MODEL
    JLabel lblStatus = new JLabel(status);
    lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
    lblStatus.setForeground(terminee ? new Color(0, 120, 0) : new Color(100, 50, 150));

    JLabel lblInfos = new JLabel(String.format(
        "<html><b>Frais :</b> %.2f € &nbsp;&nbsp;|&nbsp;&nbsp; " +
        "<b>Inscrits :</b> %d &nbsp;&nbsp;|&nbsp;&nbsp; " +
        "<b>Conducteurs :</b> %d</html>",
        ride.getFee(),
        ride.getInscriptions().size(),
        (int) ride.getVehicles().stream().filter(v -> v.getDriver() != null).count()
    ));

    card.add(lblTitle);
    card.add(Box.createVerticalStrut(5));
    card.add(lblStatus);  // ✅ AJOUTÉ
    card.add(Box.createVerticalStrut(8));
    card.add(lblInfos);

    return card;
}

    private void showCreateBaladePanel() {
        parentFrame.addPanel(new CreateBaladePanel(parentFrame, manager), "createBalade");
        parentFrame.showPanel("createBalade");
    }

    private void showOptimisationDialog() {
    List<Ride> ridesFutures = AppModel.getInstance().getRidesDuManager(manager).stream()
        .filter(ride -> !AppModel.getInstance().isRideTerminee(ride))  // ✅ SEULEMENT À VENIR
        .filter(r -> r.getInscriptions() != null && !r.getInscriptions().isEmpty())
        .toList();

    if (ridesFutures.isEmpty()) {
        //MESSAGE AVEC STATUS
        String message = "Aucune balade à optimiser.\n\n" +
            "• Toutes vos balades sont terminées 🟢\n" +
            "• Ou n'ont pas d'inscriptions";
        
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    //LISTE AVEC STATUS VISUEL
    JDialog dialog = new JDialog(parentFrame, "Optimisation Covoiturage – " + manager.getFirstname(), true);
    dialog.setSize(1100, 700);
    dialog.setLocationRelativeTo(this);

    DefaultListModel<Ride> model = new DefaultListModel<>();
    ridesFutures.forEach(model::addElement);

    JList<Ride> listRides = new JList<>(model);
    listRides.setCellRenderer((list, ride, index, sel, focus) -> {
        AppModel appModel = AppModel.getInstance();
        String status = appModel.getRideStatus(ride);  // ✅ STATUS
        int inscrits = ride.getInscriptions().size();
        int conducteurs = (int) ride.getVehicles().stream().filter(v -> v.getDriver() != null).count();
        
        String text = String.format("%s %s — %s — %d inscrit(s) — %d conducteur(s)",
            status, 
            ride.getStartDate().toLocalDate(),
            ride.getStartPlace(), inscrits, conducteurs);
        
        JLabel lbl = new JLabel(text);
        if (sel) lbl.setBackground(list.getSelectionBackground());
        lbl.setOpaque(true);
        return lbl;
    });

    // ✅ RAPPORT INCHANGÉ
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

    if (!ridesFutures.isEmpty()) listRides.setSelectedIndex(0);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(listRides), new JScrollPane(rapportArea));
    split.setDividerLocation(450);  //Plus large pour status

    JButton close = new JButton("Fermer");
    close.addActionListener(e -> dialog.dispose());

    dialog.setLayout(new BorderLayout());
    dialog.add(split, BorderLayout.CENTER);
    dialog.add(close, BorderLayout.SOUTH);
    dialog.setVisible(true);
}
}