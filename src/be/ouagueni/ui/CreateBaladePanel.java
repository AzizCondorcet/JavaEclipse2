package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CreateBaladePanel extends JPanel {

    private static final long serialVersionUID = -911708318470586665L;

    private JTextField txtNombrePlaces, txtLieuDepart, txtDateDepart, txtPrix;
    private JComboBox<TypeCat> cmbCategory;
    private JLabel lblCategoryManager;
    private JButton btnCreer, btnAnnuler;

    private final ClubFrame parentFrame;
    private final Manager manager;

    public CreateBaladePanel(ClubFrame parentFrame, Manager manager) {
        this.parentFrame = parentFrame;
        this.manager = manager;
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

        btnCreer.addActionListener(e -> actionCreerBalade());
        btnAnnuler.addActionListener(e -> actionAnnuler());
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

        if (manager.getCategory() != null) {
            lblCategoryManager = new JLabel(manager.getCategory().getNomCategorie().name());
            panel.add(lblCategoryManager, gbc);
        } else {
            cmbCategory = new JComboBox<>(TypeCat.values());
            panel.add(cmbCategory, gbc);
        }

        // Nombre de places
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Nombre de personnes:"), gbc);
        gbc.gridx = 1;
        txtNombrePlaces = new JTextField(20);
        panel.add(txtNombrePlaces, gbc);

        // Lieu de départ
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Lieu de départ:"), gbc);
        gbc.gridx = 1;
        txtLieuDepart = new JTextField(20);
        panel.add(txtLieuDepart, gbc);

        // Date de départ
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Date de départ:"), gbc);
        gbc.gridx = 1;
        txtDateDepart = new JTextField(20);
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        panel.add(txtDateDepart, gbc);

        gbc.gridx = 1; gbc.gridy++;
        JLabel lblFormat = new JLabel("(Format: yyyy-MM-dd HH:mm)");
        lblFormat.setFont(lblFormat.getFont().deriveFont(10f));
        panel.add(lblFormat, gbc);

        // Prix
        gbc.gridx = 0; gbc.gridy++;
        panel.add(new JLabel("Prix (€):"), gbc);
        gbc.gridx = 1;
        txtPrix = new JTextField(20);
        panel.add(txtPrix, gbc);

        return panel;
    }

    // =============================================================================
    // ACTIONS
    // =============================================================================
    private void actionCreerBalade() {
        TypeCat categorieSelectionnee = (manager.getCategory() != null)
                ? manager.getCategory().getNomCategorie()
                : (TypeCat) cmbCategory.getSelectedItem();

        boolean succes = AppModel.getInstance().creerBalade(
            txtNombrePlaces.getText().trim(),
            txtLieuDepart.getText().trim(),
            txtDateDepart.getText().trim(),
            txtPrix.getText().trim(),
            categorieSelectionnee,
            manager
        );

        if (succes) {
            JOptionPane.showMessageDialog(this, "Balade créée avec succès !");
            reinitialiserChamps();
            parentFrame.showManagerDashboard(manager);
        } else {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la création.\nVérifiez les champs saisis.",
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionAnnuler() {
        reinitialiserChamps();
        parentFrame.showManagerDashboard(manager);
    }

    private void reinitialiserChamps() {
        txtNombrePlaces.setText("");
        txtLieuDepart.setText("");
        txtPrix.setText("");
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        if (cmbCategory != null) cmbCategory.setSelectedIndex(0);
    }
}