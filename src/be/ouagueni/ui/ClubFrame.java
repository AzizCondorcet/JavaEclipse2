package be.ouagueni.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.Set;

import be.ouagueni.model.*;

public class ClubFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private JPanel loginPanel;
    private JPanel dashboardPanel;
    private CreateBaladePanel createBaladePanel;

    private Person currentUser; // Peut être Member ou Manager
    private final Connection conn;

    public ClubFrame() {
        conn = AppModel.getInstance().getConnection();
        initUI();
    }

    private void initUI() {
        setTitle("Club cyclistes - Espace Membre/Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = createLoginPanel();
        mainPanel.add(loginPanel, "login");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // --- Écran Connexion ---
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Connexion", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(40, 60, 130));

        JTextField txtName = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JButton btnLogin = new JButton("Se connecter");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        gbc.gridwidth = 1; gbc.gridy++;
        panel.add(new JLabel("Nom :"), gbc);
        gbc.gridx = 1; panel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Mot de passe :"), gbc);
        gbc.gridx = 1; panel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        btnLogin.addActionListener(e -> {
            String name = txtName.getText().trim();
            String password = new String(txtPassword.getPassword()).trim();

            if (name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.");
                return;
            }

            try {
                Person p = Person.login(name, password, conn);

                if (p instanceof Tresurer) {
                    System.out.println("Trésorier connecté : " + p.getName() + " (ID = " + p.getId() + ")");
                    currentUser = p;
                    showTresurerDashboard();
                } else if (p instanceof Member) {
                    System.out.println("Member connecté : " + p.getName() + " (ID = " + p.getId() + ")");
                    currentUser = p;
                    showMemberDashboard();
                } else if (p instanceof Manager) {
                    System.out.println("Manager connecté : " + p.getName() + " (ID = " + p.getId() + ")");
                    currentUser = p;
                    showManagerDashboard();
                } else {
                    JOptionPane.showMessageDialog(this, "Nom ou mot de passe incorrect.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur de connexion : " + ex.getMessage());
            }
        });

        return panel;
    }

    // --- Écran Tableau de bord MEMBRE ---
    private void showMemberDashboard() {
        Member currentMember = (Member) currentUser;
        
        dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
            "Bienvenue " + currentMember.getFirstname() + " " + currentMember.getName() + " (Membre)",
            JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 80, 130));

        // --- Panel des boutons en haut ---
        JPanel topButtons = new JPanel(new GridLayout(2, 2, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnPostAvailability = new JButton("Poster ses disponibilités");
        JButton btnReservePlace = new JButton("Réserver une place");
        JButton btnChooseCategories = new JButton("Choisir catégories");
        JButton btnViewInscriptions = new JButton("Voir mes inscriptions");

        topButtons.add(btnPostAvailability);
        topButtons.add(btnReservePlace);
        topButtons.add(btnChooseCategories);
        topButtons.add(btnViewInscriptions);

        // --- Panel vertical contenant titre + boutons ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        dashboardPanel.add(northPanel, BorderLayout.NORTH);

        // --- Actions des boutons ---
        btnPostAvailability.addActionListener(e -> showPostAvailabilityPanel());
        btnReservePlace.addActionListener(e -> showReservePlacePanel());
        btnChooseCategories.addActionListener(e -> showChooseCategoriesPanel());
        btnViewInscriptions.addActionListener(e -> showMemberInscriptions());

        // --- Liste des inscriptions ---
        JPanel listPanel = createMemberInscriptionsPanel(currentMember);
        JScrollPane scroll = new JScrollPane(listPanel);
        dashboardPanel.add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnLogout = new JButton("🔙 Déconnexion");
        btnLogout.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLogout);
        dashboardPanel.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(dashboardPanel, "dashboard");
        mainPanel.revalidate();
        mainPanel.repaint();
        cardLayout.show(mainPanel, "dashboard");
    }

    // --- Écran Tableau de bord MANAGER ---
    void showManagerDashboard() {
        Manager currentManager = (Manager) currentUser;
        
        dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
            "Bienvenue " + currentManager.getFirstname() + " " + currentManager.getName() + " (Manager)",
            JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(130, 20, 80));

        // --- Panel des boutons en haut ---
        JPanel topButtons = new JPanel(new GridLayout(1, 2, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnCreateBalade = new JButton("Créer une Balade");
        JButton btnViewBalades = new JButton("Voir toutes les Balades");

        topButtons.add(btnCreateBalade);
        topButtons.add(btnViewBalades);

        // --- Panel vertical contenant titre + boutons ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        dashboardPanel.add(northPanel, BorderLayout.NORTH);

        // --- Actions des boutons ---
        btnCreateBalade.addActionListener(e -> showCreateBaladePanel());
        btnViewBalades.addActionListener(e -> showAllBalades());

        // --- Affichage de la catégorie et des balades ---
        JPanel contentPanel = createManagerContentPanel(currentManager);
        JScrollPane scroll = new JScrollPane(contentPanel);
        dashboardPanel.add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnLogout = new JButton("🔙 Déconnexion");
        btnLogout.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLogout);
        dashboardPanel.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(dashboardPanel, "dashboard");
        mainPanel.revalidate();
        mainPanel.repaint();
        cardLayout.show(mainPanel, "dashboard");
    }
    
    private void showTresurerDashboard() {
        Tresurer currentTresurer = (Tresurer) currentUser;
        
        dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
            "Bienvenue " + currentTresurer.getFirstname() + " " + currentTresurer.getName() + " (Trésorier)",
            JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 130, 80));

        // --- Panel des boutons en haut ---
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnSendReminder = new JButton("📧 Envoyer rappel lettre");
        JButton btnPayDriver = new JButton("💶 Payer conducteur");
        JButton btnClaimFee = new JButton("💰 Réclamer frais");

        topButtons.add(btnSendReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        // --- Panel vertical contenant titre + boutons ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        dashboardPanel.add(northPanel, BorderLayout.NORTH);

        // --- Actions des boutons ---
        btnSendReminder.addActionListener(e -> sendReminderLetter());
        btnPayDriver.addActionListener(e -> payDriver());
        btnClaimFee.addActionListener(e -> claimFee());

        // --- Zone centrale avec informations ---
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
        dashboardPanel.add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnLogout = new JButton("🔙 Déconnexion");
        btnLogout.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnLogout);
        dashboardPanel.add(bottom, BorderLayout.SOUTH);

        mainPanel.add(dashboardPanel, "dashboard");
        mainPanel.revalidate();
        mainPanel.repaint();
        cardLayout.show(mainPanel, "dashboard");
    }

    // --- Panel pour afficher les inscriptions d'un membre ---
    private JPanel createMemberInscriptionsPanel(Member member) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<Inscription> inscriptions = member.getInscriptions();
        if (inscriptions == null || inscriptions.isEmpty()) {
            listPanel.add(new JLabel("Aucune inscription trouvée."));
        } else {
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
                        sb.append("Catégorie : ")
                          .append(cal.getCategory().getNomCategorie().name())
                          .append("\n");
                    }
                }

                JTextArea txt = new JTextArea(sb.toString());
                txt.setEditable(false);
                txt.setBackground(new Color(245, 245, 245));
                card.add(txt);
                listPanel.add(card);
            }
        }
        
        return listPanel;
    }

    // --- Panel pour afficher le contenu du Manager (Category et Rides) ---
    private JPanel createManagerContentPanel(Manager manager) {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        Category category = manager.getCategory();
        
        if (category == null) {
            contentPanel.add(new JLabel("Aucune catégorie assignée."));
            return contentPanel;
        }

        // --- Affichage de la catégorie ---
        JPanel categoryPanel = new JPanel();
        categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
        categoryPanel.setBorder(BorderFactory.createTitledBorder("Votre Catégorie"));
        categoryPanel.add(new JLabel("Type : " + category.getNomCategorie().name()));
        contentPanel.add(categoryPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // --- Affichage des balades (Rides) ---
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

    // --- Méthode pour afficher CreateBaladePanel ---
    private void showCreateBaladePanel() {
        if (currentUser instanceof Manager manager) {
            createBaladePanel = new CreateBaladePanel(this, manager);
            mainPanel.add(createBaladePanel, "createBalade");
            cardLayout.show(mainPanel, "createBalade");
        } else {
            JOptionPane.showMessageDialog(this, "Seul un Manager peut créer une balade.", "Accès refusé", JOptionPane.WARNING_MESSAGE);
        }
    }



    // --- Méthodes à implémenter pour Member ---
    private void showPostAvailabilityPanel() { 
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Poster ses disponibilités");
    }
    
    private void showReservePlacePanel() {
    Member currentMember = (Member) currentUser;
    
    // Récupérer toutes les balades disponibles
    Set<Ride> allRides = Ride.allRides(conn);
    
    if (allRides == null || allRides.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Aucune balade disponible pour le moment.", "Information", JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    // Créer un panel pour afficher les balades
    JPanel reservePanel = new JPanel(new BorderLayout(10, 10));
    reservePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    
    // Titre
    JLabel lblTitle = new JLabel("Réserver une place pour une balade", JLabel.CENTER);
    lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
    lblTitle.setForeground(new Color(20, 80, 130));
    reservePanel.add(lblTitle, BorderLayout.NORTH);
    
    // Liste des balades
    JPanel ridesPanel = new JPanel();
    ridesPanel.setLayout(new BoxLayout(ridesPanel, BoxLayout.Y_AXIS));
    
    // Récupérer les inscriptions du membre pour vérifier s'il est déjà inscrit
    Set<Inscription> memberInscriptions = currentMember.getInscriptions();
    
    for (Ride ride : allRides) {
        JPanel rideCard = new JPanel();
        rideCard.setLayout(new BorderLayout(10, 10));
        rideCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Informations de la balade
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        
        JLabel lblNum = new JLabel("Balade #" + ride.getnum());
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JLabel lblPlace = new JLabel("Lieu de départ : " + ride.getStartPlace());
        JLabel lblDate = new JLabel("Date : " + ride.getStartDate());
        JLabel lblFee = new JLabel("Frais : " + ride.getFee() + " €");
        
        // Afficher la catégorie si disponible
        if (ride.getCalendar() != null && ride.getCalendar().getCategory() != null) {
            JLabel lblCategory = new JLabel("Catégorie : " + ride.getCalendar().getCategory().getNomCategorie().name());
            lblCategory.setForeground(new Color(100, 100, 100));
            infoPanel.add(lblCategory);
        }
        
        infoPanel.add(lblNum);
        infoPanel.add(lblPlace);
        infoPanel.add(lblDate);
        infoPanel.add(lblFee);
        
        rideCard.add(infoPanel, BorderLayout.CENTER);
        
        // Vérifier si le membre est déjà inscrit à cette balade
        boolean alreadyRegistered = false;
        if (memberInscriptions != null) {
            for (Inscription ins : memberInscriptions) {
                if (ins.getRide() != null && ins.getRide().getId() == ride.getId()) {
                    alreadyRegistered = true;
                    break;
                }
            }
        }
        
        // Afficher "Déjà réservé" ou le bouton de réservation
        if (alreadyRegistered) {
            JLabel lblReserved = new JLabel("Déjà réservé");
            lblReserved.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblReserved.setForeground(new Color(34, 139, 34)); // Vert
            rideCard.add(lblReserved, BorderLayout.EAST);
        } else {
            JButton btnReserve = new JButton("Réserver");
            btnReserve.addActionListener(e -> {
                showReservationDialog(currentMember, ride);
            });
            rideCard.add(btnReserve, BorderLayout.EAST);
        }
        
        ridesPanel.add(rideCard);
        ridesPanel.add(Box.createVerticalStrut(10));
    }
    
    JScrollPane scrollPane = new JScrollPane(ridesPanel);
    reservePanel.add(scrollPane, BorderLayout.CENTER);
    
    // Bouton retour
    JButton btnBack = new JButton("🔙 Retour au tableau de bord");
    btnBack.addActionListener(e -> cardLayout.show(mainPanel, "dashboard"));
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bottomPanel.add(btnBack);
    reservePanel.add(bottomPanel, BorderLayout.SOUTH);
    
    // Afficher le panel
    mainPanel.add(reservePanel, "reservePlace");
    cardLayout.show(mainPanel, "reservePlace");
}

    // Méthode pour afficher le dialogue de réservation
    private void showReservationDialog(Member member, Ride ride) {
    JDialog dialog = new JDialog(this, "Réserver pour la balade #" + ride.getnum(), true);
    dialog.setSize(500, 450);
    dialog.setLocationRelativeTo(this);
    
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
    // Informations de la balade
    JLabel lblInfo = new JLabel("Balade : " + ride.getStartPlace() + " - " + ride.getStartDate());
    lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
    panel.add(lblInfo);
    panel.add(Box.createVerticalStrut(10));
    
    JLabel lblFee = new JLabel("Frais : " + ride.getFee() + " €");
    lblFee.setForeground(new Color(100, 100, 100));
    panel.add(lblFee);
    panel.add(Box.createVerticalStrut(20));
    
    // --- PARTIE 1 : Type de réservation ---
    JLabel lblReservationType = new JLabel("Type de réservation :");
    lblReservationType.setFont(new Font("Segoe UI", Font.BOLD, 12));
    panel.add(lblReservationType);
    panel.add(Box.createVerticalStrut(10));
    
    ButtonGroup reservationType = new ButtonGroup();
    JRadioButton rbPassenger = new JRadioButton("Je serai passager (réserver une place)");
    JRadioButton rbDriver = new JRadioButton("Je serai conducteur (offrir des places)");
    reservationType.add(rbPassenger);
    reservationType.add(rbDriver);
    rbPassenger.setSelected(true);
    
    panel.add(rbPassenger);
    panel.add(rbDriver);
    panel.add(Box.createVerticalStrut(20));
    
    // --- PARTIE 2 : Panel PASSAGER ---
    JPanel passengerPanel = new JPanel();
    passengerPanel.setLayout(new BoxLayout(passengerPanel, BoxLayout.Y_AXIS));
    passengerPanel.setBorder(BorderFactory.createTitledBorder("Options passager"));
    
    JCheckBox chkBringBike = new JCheckBox("J'amène mon vélo");
    
    JLabel lblSelectBike = new JLabel("Sélectionner un vélo :");
    JComboBox<Bike> cbBikes = new JComboBox<>();
    cbBikes.addItem(null); // Option "Aucun vélo"
    
    // Remplir avec les vélos du membre
    Set<Bike> memberBikes = member.getBikes();
    if (memberBikes != null && !memberBikes.isEmpty()) {
        for (Bike bike : memberBikes) {
            cbBikes.addItem(bike);
        }
    }
    cbBikes.setEnabled(false);
    
    // Renderer personnalisé pour afficher les vélos
    cbBikes.setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText("-- Aucun vélo --");
            } else if (value instanceof Bike) {
                Bike bike = (Bike) value;
                setText("Vélo #" + bike.getid() + " - " + bike.getType() + 
                       " (" + bike.getWeight() + " kg)");
            }
            return this;
        }
    });
    
    chkBringBike.addActionListener(e -> cbBikes.setEnabled(chkBringBike.isSelected()));
    
    passengerPanel.add(chkBringBike);
    passengerPanel.add(Box.createVerticalStrut(10));
    passengerPanel.add(lblSelectBike);
    passengerPanel.add(cbBikes);
    
    panel.add(passengerPanel);
    panel.add(Box.createVerticalStrut(20));
    
    // --- PARTIE 3 : Panel CONDUCTEUR ---
    JPanel driverPanel = new JPanel();
    driverPanel.setLayout(new BoxLayout(driverPanel, BoxLayout.Y_AXIS));
    driverPanel.setBorder(BorderFactory.createTitledBorder("Options conducteur"));
    driverPanel.setVisible(false);
    
    JLabel lblVehicleInfo = new JLabel("Votre véhicule :");
    Vehicle memberVehicle = member.getDriver();
    
    JTextArea txtVehicleInfo = new JTextArea();
    txtVehicleInfo.setEditable(false);
    txtVehicleInfo.setBackground(new Color(245, 245, 245));
    
    if (memberVehicle != null) {
        txtVehicleInfo.setText(
            "Places passagers : " + memberVehicle.getSeatNumber() + "\n" +
            "Emplacements vélo : " + memberVehicle.getBikeSpotNumber()
        );
    } else {
        txtVehicleInfo.setText("Vous n'avez pas de véhicule enregistré.");
        txtVehicleInfo.setForeground(Color.RED);
    }
    
    JLabel lblSeatAvailable = new JLabel("Nombre de places passagers à offrir :");
    JSpinner spinnerSeats = new JSpinner(new SpinnerNumberModel(0, 0, 
        memberVehicle != null ? memberVehicle.getSeatNumber() : 0, 1));
    
    JLabel lblBikeSpotAvailable = new JLabel("Nombre d'emplacements vélo à offrir :");
    JSpinner spinnerBikeSpots = new JSpinner(new SpinnerNumberModel(0, 0, 
        memberVehicle != null ? memberVehicle.getBikeSpotNumber() : 0, 1));
    
    driverPanel.add(lblVehicleInfo);
    driverPanel.add(txtVehicleInfo);
    driverPanel.add(Box.createVerticalStrut(10));
    driverPanel.add(lblSeatAvailable);
    driverPanel.add(spinnerSeats);
    driverPanel.add(Box.createVerticalStrut(10));
    driverPanel.add(lblBikeSpotAvailable);
    driverPanel.add(spinnerBikeSpots);
    
    panel.add(driverPanel);
    panel.add(Box.createVerticalStrut(20));
    
    // --- Gestion affichage selon type ---
    rbPassenger.addActionListener(e -> {
        passengerPanel.setVisible(true);
        driverPanel.setVisible(false);
        dialog.revalidate();
        dialog.repaint();
    });
    
    rbDriver.addActionListener(e -> {
        passengerPanel.setVisible(false);
        driverPanel.setVisible(true);
        dialog.revalidate();
        dialog.repaint();
    });
    
    // --- Boutons ---
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton btnConfirm = new JButton("Confirmer");
    JButton btnCancel = new JButton("Annuler");
    
    btnConfirm.addActionListener(e -> {
        try {
            if (rbPassenger.isSelected()) {
                // ========== CAS PASSAGER ==========
                boolean hasBike = chkBringBike.isSelected();
                Bike selectedBike = null;
                
                if (hasBike) {
                    selectedBike = (Bike) cbBikes.getSelectedItem();
                    if (selectedBike == null) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Veuillez sélectionner un vélo.", 
                            "Attention", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                
                // Créer l'inscription passager
                Inscription inscription = new Inscription(0, true, hasBike);
                inscription.setMember(member);
                inscription.setRide(ride);
                
                if (selectedBike != null) {
                    inscription.setBikeObj(selectedBike);
                }
                
                // Sauvegarder inscription.save(conn);
                boolean success = true;
                
                if (success) {
                    // Mettre à jour le membre
                    member.addInscription(inscription);
                    
                    JOptionPane.showMessageDialog(dialog, 
                        "Réservation effectuée avec succès en tant que passager !", 
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    showMemberDashboard();
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Erreur lors de la réservation.", 
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                }
                
            } else if (rbDriver.isSelected()) {
                // ========== CAS CONDUCTEUR ==========
                if (memberVehicle == null) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Vous n'avez pas de véhicule enregistré.", 
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                int seatAvailable = (Integer) spinnerSeats.getValue();
                int bikeSpotAvailable = (Integer) spinnerBikeSpots.getValue();
                
                if (seatAvailable == 0 && bikeSpotAvailable == 0) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Vous devez offrir au moins une place passager ou vélo.", 
                        "Attention", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Créer l'inscription conducteur (passenger = false)
                Inscription inscription = new Inscription(0, false, false);
                inscription.setMember(member);
                inscription.setRide(ride);
                
                // Sauvegarder l'inscription inscription.save(conn);
                boolean success = true ;
                
                if (success) {
                    // Mettre à jour le membre
                    member.addInscription(inscription);
                    
                    // TODO: Enregistrer les places disponibles dans Vehicle
                    // Vous devrez peut-être créer une table de liaison 
                    // entre Vehicle, Ride et les disponibilités
                    
                    JOptionPane.showMessageDialog(dialog, 
                        "Vous êtes enregistré comme conducteur !\n" +
                        "Places passagers offertes : " + seatAvailable + "\n" +
                        "Emplacements vélo offerts : " + bikeSpotAvailable, 
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    showMemberDashboard();
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Erreur lors de l'enregistrement.", 
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog, 
                "Erreur : " + ex.getMessage(), 
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    });
    
    btnCancel.addActionListener(e -> dialog.dispose());
    
    buttonPanel.add(btnConfirm);
    buttonPanel.add(btnCancel);
    panel.add(buttonPanel);
    
    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setBorder(null);
    dialog.add(scrollPane);
    dialog.setVisible(true);
}
    
    private void showChooseCategoriesPanel() { 
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Choisir catégories");
    }
    
    private void showMemberInscriptions() {
        // Recharge le dashboard pour afficher les inscriptions
        showMemberDashboard();
    }

    // --- Méthodes à implémenter pour Manager ---
    private void showAllBalades() {
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Voir toutes les balades");
    }
    
    private void sendReminderLetter() {
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Envoyer rappel lettre");
    }

    private void payDriver() {
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Payer conducteur");
    }

    private void claimFee() {
        JOptionPane.showMessageDialog(this, "Fonctionnalité à implémenter : Réclamer frais");
    }

    // --- MAIN ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClubFrame().setVisible(true));
    }
}