package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.Collectors;

public class MemberDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private final Member currentMember;
    private final JLabel lblBalance = new JLabel("");

    // AJOUTÉ : topButtons devient un attribut
    private final JPanel topButtons = new JPanel(new GridLayout(3, 2, 10, 10));

    private JButton btnDisponibilite;
    private JButton btnReserver;

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

        // --- Boutons en haut (maintenant via l'attribut) ---
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnDisponibilite = new JButton("Poster mes disponibilités");
        btnReserver = new JButton("Réserver une balade");
        JButton btnChoisirCategorie = new JButton("Choisir une catégorie");
        JButton btnPayerCotisation = new JButton("Payer cotisation");
        JButton btnAjouterFonds = new JButton("Ajouter fonds");

        topButtons.add(btnDisponibilite);
        topButtons.add(btnReserver);
        topButtons.add(btnChoisirCategorie);
        topButtons.add(btnPayerCotisation);
        topButtons.add(btnAjouterFonds);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(8));
        lblBalance.setAlignmentX(Component.CENTER_ALIGNMENT);
        northPanel.add(lblBalance);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        add(northPanel, BorderLayout.NORTH);

        // --- Liste des inscriptions ---
        JPanel listPanel = createMemberInscriptionsPanel(member);
        JScrollPane scroll = new JScrollPane(listPanel);
        add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnDeconnexion = new JButton("Déconnexion");
        btnDeconnexion.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDeconnexion);
        add(bottom, BorderLayout.SOUTH);

        // --- Actions ---
        btnDisponibilite.addActionListener(this::ouvrirDisponibilite);
        btnReserver.addActionListener(this::ouvrirReservation);
        btnChoisirCategorie.addActionListener(e -> choisirCategorie());
        btnPayerCotisation.addActionListener(this::handlePayerCotisation);
        btnAjouterFonds.addActionListener(this::handleAjouterFonds);

        // Initialisation
        refreshBalanceLabel(); // Met à jour les boutons dès le départ
    }
    
    private void choisirCategorie() {
        try {
            System.out.println("=== DÉBUT choisirCategorie() ===");

            // 1. Toutes les catégories existantes en base
            Set<Category> toutesLesCategories = Category.GetAll(conn);
            System.out.println("Toutes les catégories en base (" + toutesLesCategories.size() + ") :");
            toutesLesCategories.forEach(cat -> 
                System.out.println("  [ID=" + cat.getid() + "] " + cat.getNomCategorie())
            );

            // 2. Catégories que le membre possède déjà
            System.out.println("\nCatégories déjà possédées par " + 
                currentMember.getFirstname() + " " + currentMember.getName() + 
                " (" + currentMember.getCategories().size() + ") :");
            currentMember.getCategories().forEach(cat -> 
                System.out.println("  [ID=" + cat.getid() + "] " + cat.getNomCategorie())
            );

            // 3. IDs des catégories possédées (pour comparaison fiable)
            Set<Integer> idsPossedees = currentMember.getCategories().stream()
                    .map(Category::getid)
                    .collect(Collectors.toSet());
            System.out.println("\nIDs déjà possédées : " + idsPossedees);

            // 4. Filtrer les catégories disponibles
            List<Category> disponibles = toutesLesCategories.stream()
                    .filter(cat -> !idsPossedees.contains(cat.getid()))
                    .sorted(Comparator.comparing(c -> c.getNomCategorie().name()))
                    .collect(Collectors.toList());

            System.out.println("\nCatégories disponibles pour ajout (" + disponibles.size() + ") :");
            if (disponibles.isEmpty()) {
                System.out.println("  → Aucune catégorie disponible (le membre a déjà tout !)");
            } else {
                disponibles.forEach(cat -> 
                    System.out.println("  [ID=" + cat.getid() + "] " + cat.getNomCategorie())
                );
            }

            if (disponibles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Vous possédez déjà toutes les catégories disponibles !",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("=== FIN (aucune catégorie à ajouter) ===\n");
                return;
            }

            // 5. ComboBox
            JComboBox<Category> combo = new JComboBox<>(disponibles.toArray(new Category[0]));
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Category cat) {
                        setText(cat.getNomCategorie().toString());
                    }
                    return this;
                }
            });

            // 6. Dialogue
            int choix = JOptionPane.showConfirmDialog(
                this,
                combo,
                "Choisir une nouvelle catégorie",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (choix == JOptionPane.OK_OPTION) {
                Category categorieChoisie = (Category) combo.getSelectedItem();
                System.out.println("\nCatégorie sélectionnée : [ID=" + categorieChoisie.getid() + 
                    "] " + categorieChoisie.getNomCategorie());

                // Ajout dans le modèle
                currentMember.addCategory(categorieChoisie);
                System.out.println("→ Ajoutée dans l'objet Member (en mémoire)");

                // Persistance
                boolean succes = currentMember.update(conn);
                if (succes) {
                    System.out.println("MISE À JOUR EN BASE RÉUSSIE !");
                    JOptionPane.showMessageDialog(this,
                        "Catégorie " + categorieChoisie.getNomCategorie() + " ajoutée avec succès !",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    System.out.println("ÉCHEC de la mise à jour en base !");
                    currentMember.getCategories().remove(categorieChoisie); // rollback
                    JOptionPane.showMessageDialog(this,
                        "Erreur lors de l'enregistrement en base de données.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.out.println("Choix annulé par l'utilisateur.");
            }

            System.out.println("=== FIN choisirCategorie() ===\n");

        } catch (Exception ex) {
            System.out.println("ERREUR dans choisirCategorie() :");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Erreur inattendue : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /** Ouvre la fenêtre de gestion des disponibilités */
    private void ouvrirDisponibilite(ActionEvent e) {
        Set<Ride> ridesSet = Ride.allRides(conn);
        if (ridesSet.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie disponible.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Ride> rideList = new ArrayList<>(ridesSet);

        JDialog dialog = new JDialog(parentFrame, "Poster mes disponibilités", true);
        dialog.setSize(600, 400);
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
        rightPanel.add(new JLabel("Places passagers :"));
        JSpinner seatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        rightPanel.add(seatSpinner);

        rightPanel.add(new JLabel("Places vélo :"));
        JSpinner bikeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
        rightPanel.add(bikeSpinner);

        // --- CHARGEMENT DU VÉHICULE ---
        final Vehicle vehicleToSave;
        final boolean isNewVehicle;  // ← final ici

        try {
            vehicleToSave = Vehicle.getOrCreateForDriver(currentMember, conn);
            isNewVehicle = (vehicleToSave.getId() == 0);

            if (isNewVehicle) {
                rightPanel.setBorder(BorderFactory.createTitledBorder(
                    "<html><font color='orange'>Créer un nouveau véhicule</font></html>"
                ));
            } else {
                seatSpinner.setValue(vehicleToSave.getSeatNumber());
                bikeSpinner.setValue(vehicleToSave.getBikeSpotNumber());
                rightPanel.setBorder(BorderFactory.createTitledBorder(
                    "<html><font color='green'>Véhicule existant (ID: " + vehicleToSave.getId() + ")</font></html>"
                ));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(dialog, "Erreur de chargement du véhicule : " + ex.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
            dialog.dispose();
            return;
        }

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

            if (seats <= 0 && bikeSpots <= 0) {
                JOptionPane.showMessageDialog(dialog, "Proposez au moins une place.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                vehicleToSave.setSeatNumber(seats);
                vehicleToSave.setBikeSpotNumber(bikeSpots);
                vehicleToSave.addRide(rideSelectionnee);

                currentMember.postAvailability(rideSelectionnee, seats, bikeSpots, conn);

                String msg = isNewVehicle
                    ? "Nouveau véhicule créé et disponibilités postées !"
                    : "Véhicule mis à jour et disponibilités postées !";

                JOptionPane.showMessageDialog(dialog,
                    msg + "\n" +
                    seats + " place(s) passager, " + bikeSpots + " place(s) vélo",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();

            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Attention", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Erreur lors de l'enregistrement : " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
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

    private void ouvrirReservation(ActionEvent e)
    {
        Set<Ride> allRides = Ride.allRides(conn);

        List<Ride> futureRides = allRides.stream()
                .filter(r -> r.getStartDate() != null && r.getStartDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Ride::getStartDate))
                .collect(Collectors.toList());

        if (futureRides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie future disponible.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(parentFrame, "Réserver une balade", true);
        dialog.setSize(950, 650);
        dialog.setLocationRelativeTo(this);

        // === Liste des sorties ===
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Sorties disponibles"));

        JList<Ride> rideListUI = new JList<>(futureRides.toArray(new Ride[0]));
        rideListUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rideListUI.setSelectedIndex(0);
        rideListUI.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    String cat = r.getCalendar() != null && r.getCalendar().getCategory() != null
                            ? r.getCalendar().getCategory().getNomCategorie().name() : "Inconnue";
                    setText(String.format("%s | %s | %s | Forfait %.2f €",
                            r.getStartDate().toLocalDate(), r.getStartPlace(), cat, r.getFee()));
                }
                return this;
            }
        });
        leftPanel.add(new JScrollPane(rideListUI), BorderLayout.CENTER);

        // === Options de réservation ===
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Mes besoins"));

        JCheckBox chkPassenger = new JCheckBox("Je veux être passager");
        chkPassenger.setSelected(true);

        JCheckBox chkWithBike = new JCheckBox("Je veux transporter mon vélo");
        JComboBox<Object> comboBikes = new JComboBox<>();

        Set<Bike> memberBikes = currentMember.getBikes();

        // Gestion vélo absent
        if (memberBikes.isEmpty()) {
            chkWithBike.setEnabled(false);
            chkWithBike.setToolTipText("Vous n'avez pas encore enregistré de vélo");
            comboBikes.addItem("Aucun vélo enregistré");
            comboBikes.setEnabled(false);
            comboBikes.setForeground(Color.GRAY);
            comboBikes.setFont(comboBikes.getFont().deriveFont(Font.ITALIC));
        } else {
            memberBikes.forEach(comboBikes::addItem);
            comboBikes.setEnabled(false);
        }

        comboBikes.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bike b) {
                    setText(b.getType() + " - " + b.getWeight() + " kg - " + b.getLength() + " cm");
                } else {
                    setText("Aucun vélo enregistré");
                    setForeground(Color.GRAY);
                    setFont(getFont().deriveFont(Font.ITALIC));
                }
                return this;
            }
        });

        // Synchronisation case + combo
        chkWithBike.addActionListener(ev -> {
            boolean enabled = chkWithBike.isSelected() && !memberBikes.isEmpty();
            comboBikes.setEnabled(enabled);
            if (!enabled && comboBikes.getItemCount() > 0) {
                comboBikes.setSelectedIndex(0);
            }
        });

        JPanel bikePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bikePanel.add(chkWithBike);
        bikePanel.add(new JLabel("  Vélo : "));
        bikePanel.add(comboBikes);

        JLabel lblResume = new JLabel("<html><i>Recherche : 1 place passager</i></html>");
        ItemListener updateResume = ev -> {
            String txt = "<html><i>Recherche : ";
            if (chkPassenger.isSelected()) txt += "1 place passager";
            if (chkWithBike.isSelected()) txt += (chkPassenger.isSelected() ? " + " : "") + "1 place vélo";
            if (!chkPassenger.isSelected() && !chkWithBike.isSelected()) txt += "rien";
            lblResume.setText(txt + "</i></html>");
        };
        chkPassenger.addItemListener(updateResume);
        chkWithBike.addItemListener(updateResume);

        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(chkPassenger);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(bikePanel);
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(lblResume);
        rightPanel.add(Box.createVerticalGlue());

        // === Boutons ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnValider = new JButton("Réserver");
        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.addActionListener(e2 -> dialog.dispose());
        bottomPanel.add(btnValider);
        bottomPanel.add(btnAnnuler);

        // === ACTION RÉSERVATION ===
        btnValider.addActionListener(e2 -> {
            Ride selectedRide = rideListUI.getSelectedValue();
            if (selectedRide == null) {
                JOptionPane.showMessageDialog(dialog, "Sélectionnez une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean wantPassenger = chkPassenger.isSelected();
            boolean wantBikeRaw = chkWithBike.isSelected();

            if (!wantPassenger && !wantBikeRaw) {
                JOptionPane.showMessageDialog(dialog, "Choisissez au moins une option.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Cohérence vélo : on ne veut un vélo QUE si case cochée + vélo valide
            Bike selectedBike = null;
            boolean wantBike = false;

            if (wantBikeRaw) {
                Object sel = comboBikes.getSelectedItem();
                if (sel instanceof Bike bike && bike.getId() > 0) {
                    selectedBike = bike;
                    wantBike = true;
                } else {
                    JOptionPane.showMessageDialog(dialog, "Sélectionnez un vélo valide.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try {
                // Déjà inscrit ?
                if (selectedRide.getInscriptions().stream()
                        .anyMatch(ins -> ins.getMember() != null && ins.getMember().equals(currentMember))) {
                    JOptionPane.showMessageDialog(dialog, "Vous êtes déjà inscrit à cette sortie !", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // Trouver un véhicule
                Vehicle vehicle = selectedRide.findAvailableVehicle(wantPassenger, wantBike ? 1 : 0, conn);
                if (vehicle == null) {
                    String besoin = wantPassenger ? (wantBike ? "passager + vélo" : "passager") : "vélo";
                    JOptionPane.showMessageDialog(dialog, "<html>Aucune place disponible pour <b>" + besoin + "</b>.</html>", "Plus de place", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Création inscription 100% cohérente
                Inscription inscription = new Inscription();
                inscription.setMember(currentMember);
                inscription.setRide(selectedRide);
                inscription.setPassenger(wantPassenger);
                inscription.setBike(wantBike);
                inscription.setBikeObj(selectedBike);

                selectedRide.addInscription(inscription);
                currentMember.addInscription(inscription);
                if (wantPassenger) vehicle.addPassenger(currentMember);
                if (wantBike) vehicle.addBike(selectedBike);

                double newBalance = Math.round((currentMember.getBalance() - selectedRide.getFee()) * 100.0) / 100.0;
                currentMember.setBalance(newBalance);

                boolean success = inscription.create(conn)
                        && currentMember.update(conn)
                        && vehicle.update(conn);

                if (success) {
                    JOptionPane.showMessageDialog(dialog,
                        "<html><h3>Réservation confirmée !</h3>" +
                        "Sortie : " + selectedRide.getStartPlace() + "<br>" +
                        "Date : " + selectedRide.getStartDate().toLocalDate() + "<br>" +
                        "Forfait débité : " + String.format("%.2f €", selectedRide.getFee()) + "<br>" +
                        "Nouveau solde : " + String.format("%.2f €", newBalance) + "<br><br>" +
                        "Conducteur : " + vehicle.getDriver().getFirstname() + " " + vehicle.getDriver().getName() +
                        "</html>", "Succès", JOptionPane.INFORMATION_MESSAGE);

                    dialog.dispose();
                    refreshBalanceLabel();
                    removeAll();
                    add(createMemberInscriptionsPanel(currentMember), BorderLayout.CENTER);
                    revalidate();
                    repaint();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Échec de l'enregistrement en base.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                    "Erreur base de données : " + ex.getMessage(),
                    "Erreur SQL", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Assemblage
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.add(leftPanel);
        main.add(rightPanel);

        dialog.setLayout(new BorderLayout());
        dialog.add(main, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    /*2	* Handle paying the annual fee + additional category fees */
    private void handlePayerCotisation(ActionEvent e) {
        double due = currentMember.calculateBalance();
        double currentBalance = currentMember.getBalance();
        int count = currentMember.getCategories() != null ? currentMember.getCategories().size() : 0;

        if (due <= 0) {
            JOptionPane.showMessageDialog(this, "Aucune cotisation due.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String detail = String.format(
            "=== COTISATION À PAYER ===\n" +
            "Inscriptions : %d\n" +
            "Base : 20,00 €\n" +
            "Par category : 5,00 € × %d = %.2f €\n" +
            "────────────────────\n" +
            "<b>TOTAL DÛ : %.2f €</b>\n\n" +
            "Solde actuel : %.2f €\n" +
            "Montant à payer :",
            count, count, 5.0 * count, due,
            currentBalance
        );

        String input = JOptionPane.showInputDialog(this, detail, String.format("%.2f", due));
        if (input == null || input.trim().isEmpty()) return;

        double toPay;
        try {
            toPay = Double.parseDouble(input.replace(',', '.'));
            if (toPay <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double newBalance = currentBalance - toPay; // PAYER = - dette
        newBalance = Math.round(newBalance * 100.0) / 100.0;
        currentMember.setBalance(newBalance);

        boolean saved = currentMember.update(conn);

        String msg = String.format(
            "Paiement cotisation : -%.2f €\n" +
            "Inscriptions : %d\n" +
            "Ancien solde : %.2f €\n" +
            "Nouveau solde : %.2f €",
            toPay, count,
            currentBalance,
            newBalance
        );

        if (saved) {
            JOptionPane.showMessageDialog(this, msg + "\n\nEnregistré.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg + "\n\nÉCHEC sauvegarde.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }

        refreshBalanceLabel();
    }

    private void handleAjouterFonds(ActionEvent e)
    {
        String input = JOptionPane.showInputDialog(this, "Montant à verser (crédit) :", "Ajouter fonds");
        if (input == null || input.trim().isEmpty()) return;

        double amount;
        try {
            amount = Double.parseDouble(input.replace(',', '.'));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double currentBalance = currentMember.getBalance();
        double newBalance = currentBalance + amount; // VERSER = + crédit
        newBalance = Math.round(newBalance * 100.0) / 100.0;
        currentMember.setBalance(newBalance);

        boolean saved = currentMember.update(conn);

        String msg = String.format(
            "Versement : +%.2f €\n" +
            "Ancien solde : %.2f €\n" +
            "Nouveau solde : %.2f €",
            amount,
            currentBalance,
            newBalance
        );

        if (saved) {
            JOptionPane.showMessageDialog(this, msg + "\n\nEnregistré.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg + "\n\nÉCHEC sauvegarde.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }

        refreshBalanceLabel();
        
    }

    private void refreshBalanceLabel() {
        double balance = currentMember.getBalance();
        String text;

        if (balance > 0) {
            text = String.format("<html><font color='blue'><b>Crédit : %.2f €</b></font></html>", balance);
        } else if (balance == 0) {
            text = "<html><font color='green'><b>À jour</b></font></html>";
        } else {
            text = String.format("<html><font color='red'><b>Vous devez %.2f €</b></font></html>", -balance);
        }

        lblBalance.setText(text);

        // FONCTIONNE PARFAITEMENT MAINTENANT
        boolean peutParticiper = balance >= 0;
        Component[] components = topButtons.getComponents();
        for (Component c : components) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                String btnText = btn.getText();
                if (btnText.contains("Poster") || btnText.contains("Réserver")) {
                    btn.setEnabled(peutParticiper);
                    if (!peutParticiper) {
                        btn.setToolTipText("Règlez votre cotisation pour activer cette fonction");
                    } else {
                        btn.setToolTipText(null);
                    }
                }
            }
        }
    }

    /** Attempts to detect and call member payment methods. Returns true if invoked. */
    private boolean tryInvokePaymentOnMember(Member member, double amount) {
        Class<?> cls = member.getClass();
        try {
            // common method names that might charge the member
            String[] names = {"payFees", "payerCotisation", "deductBalance", "charge", "chargeFees", "pay", "debit"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 1);
                if (m != null) {
                    // try with (double) amount
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount);
                            return true;
                        } else if (params[0] == int.class || params[0] == Integer.class) {
                            m.invoke(member, (int) amount);
                            return true;
                        } else if (params[0] == Connection.class) {
                            m.invoke(member, conn);
                            return true;
                        } else if (params[0] == String.class) {
                            m.invoke(member, String.valueOf(amount));
                            return true;
                        }
                    } else if (params.length == 2 && params[1] == Connection.class) {
                        // maybe (double, Connection)
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount, conn);
                            return true;
                        }
                    }
                }
            }

            // fallback: try setBalance/getBalance to subtract
            Method getBal = findMethodIgnoreCase(cls, new String[]{"getBalance", "getSolde"}, 0);
            Method setBal = findMethodIgnoreCase(cls, new String[]{"setBalance", "setSolde"}, 1);
            if (getBal != null && setBal != null) {
                Object cur = getBal.invoke(member);
                double curVal = cur instanceof Number ? ((Number) cur).doubleValue() : Double.parseDouble(String.valueOf(cur));
                double newVal = curVal - amount;
                Class<?> param = setBal.getParameterTypes()[0];
                if (param == double.class || param == Double.class) setBal.invoke(member, newVal);
                else if (param == int.class || param == Integer.class) setBal.invoke(member, (int) newVal);
                else setBal.invoke(member, String.valueOf(newVal));
                return true;
            }
        } catch (Exception ex) {
            // ignore reflection exceptions, fall through to false
        }
        return false;
    }

    /** Attempts to detect and call member add-funds methods. Returns true if invoked. */
    private boolean tryInvokeAddFundsOnMember(Member member, double amount) {
        Class<?> cls = member.getClass();
        try {
            String[] names = {"addFunds", "addBalance", "deposit", "credit", "ajouterFonds", "crediter"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 1);
                if (m != null) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount);
                            return true;
                        } else if (params[0] == int.class || params[0] == Integer.class) {
                            m.invoke(member, (int) amount);
                            return true;
                        } else if (params[0] == String.class) {
                            m.invoke(member, String.valueOf(amount));
                            return true;
                        }
                    } else if (params.length == 2 && params[1] == Connection.class) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount, conn);
                            return true;
                        }
                    }
                }
            }

            // fallback: use getBalance/setBalance to add
            Method getBal = findMethodIgnoreCase(cls, new String[]{"getBalance", "getSolde"}, 0);
            Method setBal = findMethodIgnoreCase(cls, new String[]{"setBalance", "setSolde"}, 1);
            if (getBal != null && setBal != null) {
                Object cur = getBal.invoke(member);
                double curVal = cur instanceof Number ? ((Number) cur).doubleValue() : Double.parseDouble(String.valueOf(cur));
                double newVal = curVal + amount;
                Class<?> param = setBal.getParameterTypes()[0];
                if (param == double.class || param == Double.class) setBal.invoke(member, newVal);
                else if (param == int.class || param == Integer.class) setBal.invoke(member, (int) newVal);
                else setBal.invoke(member, String.valueOf(newVal));
                return true;
            }
        } catch (Exception ex) {
            // ignore reflection exceptions
        }
        return false;
    }

    /** Tries to find a method by several candidate names (case-insensitive) with given parameter count. */
    private Method findMethodIgnoreCase(Class<?> cls, String[] candidateNames, int paramCount) {
        Method[] methods = cls.getMethods();
        for (Method m : methods) {
            for (String cand : candidateNames) {
                if (m.getName().equalsIgnoreCase(cand) && m.getParameterTypes().length == paramCount) {
                    return m;
                }
            }
        }
        return null;
    }

    /** Get number of categories of a member using reflection; default to 1 if unknown */
    private int getMemberCategoriesCount(Member member) {
        Class<?> cls = member.getClass();
        try {
            // common getters that may return a Collection or array
            String[] names = {"getCategories", "getCategorySet", "getCategoriesSet", "getCategory", "getCategoriesList"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 0);
                if (m != null) {
                    Object res = m.invoke(member);
                    if (res instanceof Collection) {
                        return ((Collection<?>) res).size();
                    } else if (res != null && res.getClass().isArray()) {
                        return ((Object[]) res).length;
                    } else if (res instanceof Number) {
                        return ((Number) res).intValue();
                    }
                }
            }
        } catch (Exception ignored) {}
        // fallback: try to read a "getCategory" returning single category -> count = 1
        try {
            Method m = findMethodIgnoreCase(cls, new String[]{"getCategory"}, 0);
            if (m != null) return 1;
        } catch (Exception ignored) {}
        return 1;
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
