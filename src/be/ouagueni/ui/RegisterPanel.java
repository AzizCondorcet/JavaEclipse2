package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

public class RegisterPanel extends JPanel {

    private final ClubFrame frame;
    private final Connection conn;

    private JTextField txtLastName, txtFirstName, txtTel;
    private JPasswordField txtPassword;
    private JComboBox<TypeCat> comboCategory;

    private JTextField txtBikeWeight, txtBikeLength;
    private JComboBox<String> comboBikeType;

    private JButton btnRegister, btnBackToLogin;

    public RegisterPanel(ClubFrame frame, Connection conn) {
        this.frame = frame;
        this.conn = conn;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Création de compte");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(lblTitle, gbc);

        gbc.gridwidth = 1;

        // === INFORMATIONS PERSONNELLES ===
        addField(gbc, "Nom :", txtLastName = new JTextField(20), 1);
        addField(gbc, "Prénom :", txtFirstName = new JTextField(20), 2);
        addField(gbc, "Téléphone :", txtTel = new JTextField(20), 3);
        addField(gbc, "Mot de passe :", txtPassword = new JPasswordField(20), 4);
        addField(gbc, "Catégorie préférée :", comboCategory = new JComboBox<>(TypeCat.values()), 5);
        comboCategory.setRenderer(new CategoryRenderer());

        // === VÉLO OBLIGATOIRE ===
        JLabel lblBike = new JLabel("Votre vélo");
        lblBike.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        add(lblBike, gbc);

        gbc.gridwidth = 1;
        addField(gbc, "Poids (kg) :", txtBikeWeight = new JTextField(20), 7);
        addField(gbc, "Longueur (m) :", txtBikeLength = new JTextField(20), 8);

        gbc.gridx = 0; gbc.gridy = 9;
        add(new JLabel("Type de vélo :"), gbc);
        gbc.gridx = 1;
        comboBikeType = new JComboBox<>(new String[]{"Route", "Trial", "Descente", "Cross"});
        add(comboBikeType, gbc);

        // === BOUTONS ===
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnRegister = new JButton("Créer mon compte");
        btnBackToLogin = new JButton("Retour");
        btnPanel.add(btnRegister);
        btnPanel.add(btnBackToLogin);

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 2;
        add(btnPanel, gbc);

        // Actions
        btnRegister.addActionListener(this::handleRegister);
        btnBackToLogin.addActionListener(e -> frame.showPanel("login"));
    }

    private void addField(GridBagConstraints gbc, String label, JComponent field, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        add(new JLabel(label), gbc);
        gbc.gridx = 1;
        add(field, gbc);
    }

    private static class CategoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof TypeCat) {
                value = switch ((TypeCat) value) {
                    case RoadBike -> "Route";
                    case Trial -> "Trial";
                    case Downhill -> "Descente";
                    case Cross -> "Cross";
                };
                
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    
    private void handleRegister(ActionEvent e) {
        try {
            // --- RÉCUPÉRATION SIMPLE ---
            String lastName = txtLastName.getText().trim();
            String firstName = txtFirstName.getText().trim();
            String tel = txtTel.getText().trim();
            String password = new String(txtPassword.getPassword());
            TypeCat selectedCat = (TypeCat) comboCategory.getSelectedItem();
            String bikeTypeStr = (String) comboBikeType.getSelectedItem();

            double weight = Double.parseDouble(txtBikeWeight.getText().trim());
            double length = Double.parseDouble(txtBikeLength.getText().trim());

            // --- Mapper type vélo ---
            TypeCat bikeType = switch (bikeTypeStr) {
                case "Route" -> TypeCat.RoadBike;
                case "Trial" -> TypeCat.Trial;
                case "Descente" -> TypeCat.Downhill;
                case "Cross" -> TypeCat.Cross;
                default -> null;
            };

            if (bikeType == null) {
                JOptionPane.showMessageDialog(this, "Type de vélo invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // --- DÉLÉGUER À AppModel ---
            AppModel appModel = AppModel.getInstance();
            boolean succes = appModel.creerMembreComplet(
                lastName, firstName, tel, password, 
                selectedCat, weight, bikeType, length
            );

            if (succes) {
                JOptionPane.showMessageDialog(this,
                    "Compte créé avec succès !\nVous pouvez vous connecter.",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
                frame.showPanel("login");
                //Effacer les champs
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Erreur lors de la création du compte.\nVérifiez vos données.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Poids et longueur doivent être des nombres valides.", 
                "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Erreur inattendue : " + ex.getMessage(), 
                "Erreur", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void clearFields() {
        txtLastName.setText("");
        txtFirstName.setText("");
        txtTel.setText("");
        txtPassword.setText("");
        txtBikeWeight.setText("");
        txtBikeLength.setText("");
        comboCategory.setSelectedIndex(0);
        comboBikeType.setSelectedIndex(0);
    }



}