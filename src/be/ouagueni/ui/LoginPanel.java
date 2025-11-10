package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;

public class LoginPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnQuit;

    public LoginPanel(ClubFrame parentFrame, Connection conn) {
        this.parentFrame = parentFrame;
        this.conn = conn;
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Connexion au Club Cycliste");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(lblTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        add(new JLabel("Name :"), gbc);
        txtEmail = new JTextField(20);
        gbc.gridx = 1;
        add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Mot de passe :"), gbc);
        txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy++;
        btnLogin = new JButton("Se connecter");
        add(btnLogin, gbc);

        gbc.gridx = 1;
        btnQuit = new JButton("Quitter");
        add(btnQuit, gbc);

        btnLogin.addActionListener(this::handleLogin);
        btnQuit.addActionListener(e -> System.exit(0));
    }

    /** Gère la tentative de connexion */
    private void handleLogin(ActionEvent e) {
    String name = txtEmail.getText().trim();
    String password = new String(txtPassword.getPassword());

    if (name.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.");
        return;
    }

    Person user = Person.login(name, password, conn); 

    if (user == null) {
        JOptionPane.showMessageDialog(this, "Identifiants incorrects !");
        return;
    }

    parentFrame.setCurrentUser(user);

    // Crée dynamiquement le dashboard selon le type
    if (user instanceof Member member) {
        parentFrame.showMemberDashboard(member);
    } else if (user instanceof Manager manager) {
        parentFrame.showManagerDashboard(manager);
    } else if (user instanceof Treasurer treasurer) {
        parentFrame.showTreasurerDashboard(treasurer);
    } else {
        JOptionPane.showMessageDialog(this, "Type d'utilisateur inconnu !");
    }
}

}
