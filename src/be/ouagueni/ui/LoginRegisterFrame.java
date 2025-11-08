/*package be.ouagueni.ui;

import java.awt.*;
import javax.swing.*;
import java.sql.Connection;

import be.ouagueni.connection.ClubConnection;
import be.ouagueni.controllers.PersonController;
import be.ouagueni.model.Bike;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Inscription;
import be.ouagueni.model.Manager;
import be.ouagueni.model.Member;
import be.ouagueni.model.Person;
import be.ouagueni.model.Ride;

public class LoginRegisterFrame extends JFrame {

	private static final long serialVersionUID = 1383895828793361505L;
	private JTabbedPane tabbedPane;
    private PersonController personController;
    private Connection conn;

    // === Onglet Connexion ===
    private JTextField txtNameLogin;
    private JPasswordField txtPasswordLogin;
    private JButton btnLogin;

    // === Onglet Création de compte ===
    private JTextField txtNameRegister;
    private JTextField txtFirstnameRegister;
    private JTextField txtTelRegister;
    private JPasswordField txtPasswordRegister;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    
    // --- Constructeur principal ---
    public LoginRegisterFrame(PersonController controller) {
        this.personController = controller;
        this.conn = ClubConnection.getInstance();
        initComponents();
    }

    public LoginRegisterFrame() {
        this.conn = ClubConnection.getInstance();
        this.personController = new PersonController(conn);
        initComponents();
    }

    private void initComponents() {
        setTitle("Club Moto - Connexion / Création de compte");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 380);
        setLocationRelativeTo(null);
        setResizable(false);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Connexion", createLoginPanel());
        tabbedPane.addTab("Créer un compte", createRegisterPanel());
        add(tabbedPane);
    }

    // --- PANEL CONNEXION ---
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblNom = new JLabel("Nom :");
        JLabel lblPassword = new JLabel("Mot de passe :");
        txtNameLogin = new JTextField(20);
        txtPasswordLogin = new JPasswordField(20);
        btnLogin = new JButton("Se connecter");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblNom, gbc);
        gbc.gridx = 1;
        panel.add(txtNameLogin, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblPassword, gbc);
        gbc.gridx = 1;
        panel.add(txtPasswordLogin, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        btnLogin.addActionListener(e -> handleLogin());

        return panel;
    }

	// --- LOGIQUE CONNEXION ---
	private void handleLogin() {
	    try {
	        String name = txtNameLogin.getText().trim();
	        String password = new String(txtPasswordLogin.getPassword()).trim();
	
	        if (name.isEmpty() || password.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.", "Erreur", JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	
	        // JRAME MODEL DAO 
	        // pas de buisnes logique ici <=== IMPORTANT
	        // Recuperé la connection comme dans le projet Ecole
	        // On peut changer les fleches dans le diag de classe
	        Person login = new Member(name, password);
	
	        boolean success = personController.CreatePerson(login);
	        if (success) {
	            Member member = (Member) login;
	
	            System.out.println("\n==============================");
	            System.out.println("💳 Informations du Membre");
	            System.out.println("==============================");
	            System.out.println("ID Membre: " + member.getId());
	            System.out.println("Nom complet: " + member.getFirstname() + " " + member.getName());
	            System.out.println("Téléphone: " + member.getTel());
	            System.out.println("Balance: " + member.getBalance() + "€");
	            System.out.println("Nombre d'inscriptions: " + member.getInscriptions().size());
	
	            // Parcourir toutes les inscriptions
	            System.out.println("\n📝 Inscriptions détaillées:");
	            int compteur = 1;
	            for (Inscription ins : member.getInscriptions()) {
	                System.out.println("----------------------------------------");
	                System.out.println("  " + compteur + ") Inscription #" + ins.getId());
	                System.out.println("     - Passager: " + (ins.isPassenger() ? "Oui" : "Non"));
	                System.out.println("     - Avec vélo: " + (ins.isBike() ? "Oui" : "Non"));
	
	                // 🔹 Afficher le vélo
	                if (ins.getBikeObj() != null) {
	                    Bike bike = ins.getBikeObj();
	                    System.out.println("     🚴 Vélo #" + bike.getid());
	                    System.out.println("        - Type: " + bike.getType());
	                    System.out.println("        - Poids: " + bike.getWeight() + " kg");
	                    System.out.println("        - Longueur: " + bike.getLength() + " cm");
	                } else if (ins.isBike()) {
	                    System.out.println("     ⚠️ Vélo non chargé !");
	                }
	
	                // 🔹 Afficher le trajet (Ride)
	                if (ins.getRide() != null) {
	                    Ride ride = ins.getRide();
	                    System.out.println("     🛣️ Trajet #" + ride.getId());
	                    System.out.println("        - Numéro: " + ride.getnum());
	                    System.out.println("        - Départ: " + ride.getStartPlace());
	                    System.out.println("        - Date de départ: " + ride.getStartDate());
	                    System.out.println("        - Tarif: " + ride.getFee() + "€");
	                } else {
	                    System.out.println("     ⚠️ Aucun trajet associé !");
	                }
	
	                // 🔹 Afficher le calendrier et la catégorie
	                if (ins.getRide() != null && ins.getRide().getCalendar() != null) {
	                    Calendar cal = ins.getRide().getCalendar();
	                    System.out.println("     📅 Calendar ID: " + cal.getid());
	                    if (cal.getCategory() != null) {
	                        Category cat = cal.getCategory();
	                        System.out.println("        - Catégorie ID: " + cat.getid());
	                        System.out.println("        - Type: " + (cat.getNomCategorie() != null ? cat.getNomCategorie() : "inconnu"));
	                        System.out.println("        - Manager ID: " + cat.getid());
	                    } else {
	                        System.out.println("        ⚠️ Catégorie non chargée !");
	                    }
	                } else {
	                    System.out.println("     ⚠️ Calendar non chargé !");
	                }
	
	                compteur++;
	            }
	
	            System.out.println("========================================\n");
	            new MainFrame(member).setVisible(true); 
	            this.dispose();
	
	        } else {
	            JOptionPane.showMessageDialog(this, "Nom ou mot de passe incorrect.", "Erreur", JOptionPane.ERROR_MESSAGE);
	        }
	
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        JOptionPane.showMessageDialog(this, "Erreur lors de la tentative de connexion : " + ex.getMessage(),
	                "Erreur", JOptionPane.ERROR_MESSAGE);
	    }
	}


    // --- PANEL INSCRIPTION ---
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = new JLabel("Nom :");
        JLabel lblFirstname = new JLabel("Prénom :");
        JLabel lblTel = new JLabel("Téléphone :");
        JLabel lblPassword = new JLabel("Mot de passe :");
        JLabel lblConfirm = new JLabel("Confirmer le mot de passe :");

        txtNameRegister = new JTextField(20);
        txtFirstnameRegister = new JTextField(20);
        txtTelRegister = new JTextField(20);
        txtPasswordRegister = new JPasswordField(20);
        txtConfirmPassword = new JPasswordField(20);
        btnRegister = new JButton("Créer le compte");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(lblName, gbc);
        gbc.gridx = 1; panel.add(txtNameRegister, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(lblFirstname, gbc);
        gbc.gridx = 1; panel.add(txtFirstnameRegister, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(lblTel, gbc);
        gbc.gridx = 1; panel.add(txtTelRegister, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(lblPassword, gbc);
        gbc.gridx = 1; panel.add(txtPasswordRegister, gbc);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(lblConfirm, gbc);
        gbc.gridx = 1; panel.add(txtConfirmPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; panel.add(btnRegister, gbc);

        btnRegister.addActionListener(e -> handleRegister());
        return panel;
    }

    // --- LOGIQUE INSCRIPTION ---
    private void handleRegister() {
        String name = txtNameRegister.getText().trim();
        String firstname = txtFirstnameRegister.getText().trim();
        String tel = txtTelRegister.getText().trim();
        String password = new String(txtPasswordRegister.getPassword()).trim();
        String confirm = new String(txtConfirmPassword.getPassword()).trim();

        if (name.isEmpty() || firstname.isEmpty() || tel.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Les mots de passe ne correspondent pas.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // TODO : Appel du contrôleur PersonController.create(...)
        boolean success = true; // à remplacer

        if (success) {
            JOptionPane.showMessageDialog(this, "Compte créé avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(0); // retour à la page de connexion
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de la création du compte.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- MAIN pour tester ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginRegisterFrame().setVisible(true));
    }
}*/
