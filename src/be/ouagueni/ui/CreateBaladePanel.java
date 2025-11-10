package be.ouagueni.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import be.ouagueni.model.*;

public class CreateBaladePanel extends JPanel {

    private static final long serialVersionUID = -911708318470586665L;

    private JTextField txtNombrePlaces, txtLieuDepart, txtDateDepart, txtPrix;
    private JComboBox<TypeCat> cmbCategory;
    private JLabel lblCategoryManager;
    private JButton btnCreer, btnAnnuler;

    private final ClubFrame parentFrame;
    private final Manager manager;
    private final Category managerCategory;

    public CreateBaladePanel(ClubFrame parentFrame, Manager manager) {
        this.parentFrame = parentFrame;
        this.manager = manager;
        this.managerCategory = manager.getCategory();
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel fieldsPanel = createFieldsPanel();
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        add(fieldsPanel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        btnAnnuler = new JButton("Annuler");
        gbc.gridx = 0; add(btnAnnuler, gbc);

        btnCreer = new JButton("Créer la Balade");
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        add(btnCreer, gbc);

        btnCreer.addActionListener(e -> creerBalade());
        btnAnnuler.addActionListener(e -> {
            reinitialiserChamps();
            if (parentFrame != null) parentFrame.showManagerDashboard(manager);
        });
    }

    private JPanel createFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Informations de la Balade"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Catégorie
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Catégorie:"), gbc);
        gbc.gridx = 1;

        if (managerCategory != null) {
            lblCategoryManager = new JLabel(managerCategory.getNomCategorie().name());
            panel.add(lblCategoryManager, gbc);
        } else {
            cmbCategory = new JComboBox<>(TypeCat.values());
            panel.add(cmbCategory, gbc);
        }

        // Nombre de places
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Nombre de personnes:"), gbc);
        gbc.gridx = 1;
        txtNombrePlaces = new JTextField(20);
        panel.add(txtNombrePlaces, gbc);

        // Lieu de départ
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Lieu de départ:"), gbc);
        gbc.gridx = 1;
        txtLieuDepart = new JTextField(20);
        panel.add(txtLieuDepart, gbc);

        // Date de départ
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Date de départ:"), gbc);
        gbc.gridx = 1;
        txtDateDepart = new JTextField(20);
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        panel.add(txtDateDepart, gbc);

        // Indication format
        gbc.gridx = 1; gbc.gridy = 4;
        JLabel lblFormat = new JLabel("(Format: yyyy-MM-dd HH:mm)");
        lblFormat.setFont(lblFormat.getFont().deriveFont(10f));
        panel.add(lblFormat, gbc);

        // Prix
        gbc.gridx = 0; gbc.gridy = 5;
        panel.add(new JLabel("Prix (€):"), gbc);
        gbc.gridx = 1;
        txtPrix = new JTextField(20);
        panel.add(txtPrix, gbc);

        return panel;
    }

    private boolean validerChamps() {
        if (managerCategory == null && (cmbCategory == null || cmbCategory.getSelectedItem() == null)) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une catégorie.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (!txtNombrePlaces.getText().trim().matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir un nombre de personnes valide.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (txtLieuDepart.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir le lieu de départ.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (txtDateDepart.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir la date de départ.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (!txtPrix.getText().trim().matches("\\d+(\\.\\d{1,2})?")) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir un prix valide.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    private void creerBalade() {
        try {
            if (!validerChamps()) return;

            // Détermination de la catégorie
            Category category = managerCategory;
            if (category == null && cmbCategory != null) {
                TypeCat selected = (TypeCat) cmbCategory.getSelectedItem();
                category = new Category(0, selected, null, null);
            }

            // Création du calendrier (si besoin)
            Calendar calendar = category.getCalendar();
            if (calendar == null) {
                calendar = new Calendar(category);
                calendar.createCalendar(calendar, AppModel.getInstance().getConnection());
                category.setCalendar(calendar);
            }

            // Lecture des valeurs
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTimeDepart = LocalDateTime.parse(txtDateDepart.getText().trim(), formatter);
            double prix = Double.parseDouble(txtPrix.getText().trim().replace(",", "."));
            int nbPlaces = Integer.parseInt(txtNombrePlaces.getText().trim());

            // Création du ride
            Ride ride = new Ride(nbPlaces, txtLieuDepart.getText().trim(), dateTimeDepart, prix, calendar);
            boolean created = ride.createRide(ride, AppModel.getInstance().getConnection());

            if (created) {
                calendar.addRide(ride);
                JOptionPane.showMessageDialog(this, "✅ Balade créée avec succès !");
                reinitialiserChamps();
                if (parentFrame != null) parentFrame.showManagerDashboard(manager);
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la création de la balade.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }

        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Format de date incorrect (yyyy-MM-dd HH:mm).", "Erreur de format", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de la création : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reinitialiserChamps() {
        if (managerCategory == null && cmbCategory != null) cmbCategory.setSelectedIndex(0);
        txtNombrePlaces.setText("");
        txtLieuDepart.setText("");
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        txtPrix.setText("");
    }
}
