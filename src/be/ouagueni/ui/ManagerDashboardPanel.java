package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

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
        JButton btnOptimiserCovoiturage = new JButton("Optimisation covoiturage");
        btnOptimiserCovoiturage.setIcon(UIManager.getIcon("FileView.directoryIcon")); // petite icône dossier (ou voiture si tu veux)
        btnOptimiserCovoiturage.setToolTipText("Voir le plan de covoiturage optimisé pour chaque sortie");
        topButtons.add(btnCreateBalade);
        topButtons.add(btnViewBalades);
        topButtons.add(btnOptimiserCovoiturage);

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
        btnOptimiserCovoiturage.addActionListener(e -> ouvrirOptimisationCovoiturage());
        btnViewBalades.addActionListener(e -> JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Liste des balades"));
    }

    private void ouvrirOptimisationCovoiturage() 
    {
    		
        Set<Ride> ridesDuManager = new HashSet<>();

        Category cat = manager.getCategory();
        if (cat != null && cat.getCalendar() != null && cat.getCalendar().getRides() != null) {
            ridesDuManager.addAll(cat.getCalendar().getRides());
        }

        // On garde seulement celles qui ont au moins une inscription
        List<Ride> ridesAvecInscriptions = ridesDuManager.stream()
                .filter(r -> r.getInscriptions() != null && !r.getInscriptions().isEmpty())
                .sorted(Comparator.comparing(Ride::getStartDate))
                .toList();

        if (ridesAvecInscriptions.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucune de vos sorties n'a encore d'inscriptions.",
                "Optimisation covoiturage", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parentFrame, "Optimisation du covoiturage", true);
        dialog.setSize(1000, 720);
        dialog.setLocationRelativeTo(this);

        // Liste des sorties à gauche
        DefaultListModel<Ride> model = new DefaultListModel<>();
        ridesAvecInscriptions.forEach(model::addElement);

        JList<Ride> listRides = new JList<>(model);
        listRides.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    String cat = r.getCalendar() != null && r.getCalendar().getCategory() != null
                            ? r.getCalendar().getCategory().getNomCategorie().name()
                            : "NC";
                    int inscrits = r.getInscriptions().size();
                    int vehicules = (int) r.getVehicles().stream().filter(v -> v.getDriver() != null).count();
                    setText(String.format("%s — %s (%s) — %d inscrits — %d voiture(s)",
                            r.getStartDate().toLocalDate(),
                            r.getStartPlace(),
                            cat,
                            inscrits,
                            vehicules));
                }
                return this;
            }
        });

        listRides.setSelectedIndex(0);
        JScrollPane scrollList = new JScrollPane(listRides);

        // Zone texte à droite
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setEditable(false);
        textArea.setBackground(new Color(250, 250, 250));
        JScrollPane scrollText = new JScrollPane(textArea);

        // Mise à jour automatique du rapport
        listRides.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Ride ride = listRides.getSelectedValue();
                if (ride != null) {
                    OptimisationCovoiturage.ResultatOptimisation resultat = OptimisationCovoiturage.optimiser(ride);
                    String rapport = OptimisationCovoiturage.genererRapport(ride, resultat);
                    textArea.setText(rapport);
                    textArea.setCaretPosition(0);
                }
            }
        });

        // Affichage initial
        Ride premiere = listRides.getSelectedValue();
        if (premiere != null) {
            OptimisationCovoiturage.ResultatOptimisation res = OptimisationCovoiturage.optimiser(premiere);
            textArea.setText(OptimisationCovoiturage.genererRapport(premiere, res));
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollList, scrollText);
        split.setDividerLocation(380);
        split.setResizeWeight(0.3);

        dialog.add(split, BorderLayout.CENTER);

        // Bouton fermer stylé
        JButton btnFermer = new JButton("Fermer");
        btnFermer.setPreferredSize(new Dimension(100, 35));
        btnFermer.addActionListener(e -> dialog.dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnFermer);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setVisible(true);
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
