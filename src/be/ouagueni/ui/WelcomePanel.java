// be.ouagueni.ui.WelcomePanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class WelcomePanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private final AppModel model = AppModel.getInstance();
    private JTabbedPane tabbedPane;

    public WelcomePanel(ClubFrame parentFrame, Connection conn) {
        this.parentFrame = parentFrame;
        this.conn = conn;

        setLayout(new BorderLayout());

        // Titre principal
        JLabel title = new JLabel("Club Cycliste Ouagueni", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
        title.setForeground(new Color(0, 100, 180));
        add(title, BorderLayout.NORTH);

        // Onglets principaux
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        tabbedPane.addTab("Connexion", createLoginPanel());
        tabbedPane.addTab("Voir les sorties", createPublicRidesPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Connexion au compte");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitle, gbc);

        JTextField txtEmail = new JTextField(25);
        JPasswordField txtPassword = new JPasswordField(25);

        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(new JLabel("Nom:"), gbc);
        gbc.gridx = 1;
        panel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Mot de passe :"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        JButton btnLogin = new JButton("Se connecter");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setBackground(new Color(41, 128, 185));     
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);                   

        JButton btnRegister = new JButton("Créer un compte");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRegister.setBackground(new Color(46, 204, 113));  
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setBorderPainted(false);

        JButton btnQuit = new JButton("Quitter");
        btnQuit.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnQuit.setBackground(new Color(231, 76, 60));      
        btnQuit.setForeground(Color.WHITE);
        btnQuit.setFocusPainted(false);
        btnQuit.setBorderPainted(false);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        btnPanel.add(btnLogin);
        btnPanel.add(btnRegister);
        btnPanel.add(btnQuit);
        panel.add(btnPanel, gbc);

        // === ACTIONS ===
        btnQuit.addActionListener(e -> System.exit(0));

        btnLogin.addActionListener(e -> {
            String identifiant = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());

            if (identifiant.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.", "Erreur", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Person user = Person.login(identifiant, password, conn);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Identifiants incorrects !", "Échec connexion", JOptionPane.ERROR_MESSAGE);
                return;
            }

            parentFrame.setCurrentUser(user);
            if (user instanceof Member m) {
                parentFrame.showMemberDashboard(m);
            } else if (user instanceof Manager m) {
                parentFrame.showManagerDashboard(m);
            } else if (user instanceof Treasurer t) {
                parentFrame.showTreasurerDashboard(t);
            }
        });

        btnRegister.addActionListener(e -> parentFrame.showPanel("register"));

        return panel;
    }

    private JPanel createPublicRidesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel info = new JLabel("<html><h2>Sorties à venir – Ouvert à tous</h2>" +
                "<p style='font-size:14px; color:gray;'>Consultez les balades même sans compte !</p></html>");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(info, BorderLayout.NORTH);

        Map<TypeCat, List<Object[]>> ridesParCategorie = model.getPublicUpcomingRidesForTable();

        if (ridesParCategorie.isEmpty()) {
            JLabel noRide = new JLabel("<html><h3 style='color:#888; text-align:center;'>"+
                                      "Aucune sortie prévue pour le moment...<br>Revenez bientôt !</h3></html>");
            noRide.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(noRide, BorderLayout.CENTER);
            return panel;
        }

        JTabbedPane tabsCategories = new JTabbedPane();
        tabsCategories.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        String[] colonnes = {"Date", "Lieu de départ", "Forfait", "Inscrits"};

        for (Map.Entry<TypeCat, List<Object[]>> entry : ridesParCategorie.entrySet()) {
            TypeCat cat = entry.getKey();
            List<Object[]> lignes = entry.getValue();

            DefaultTableModel tableModel = new DefaultTableModel(colonnes, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            for (Object[] ligne : lignes) {
                tableModel.addRow(ligne);
            }

            JTable table = new JTable(tableModel);
            table.setRowHeight(35);
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(10, 8));

            JScrollPane scroll = new JScrollPane(table);
            String libelle = model.getLibelleCategorie(cat);
            tabsCategories.addTab(libelle + " (" + lignes.size() + ")", scroll);
        }

        panel.add(tabsCategories, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }
}