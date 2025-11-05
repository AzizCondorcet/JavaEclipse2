package be.ouagueni.ui;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.sql.Connection;
import be.ouagueni.connection.ClubConnection;
import be.ouagueni.controllers.CalendarController;
import be.ouagueni.controllers.CategoryController;
import be.ouagueni.controllers.RideController;
import be.ouagueni.model.Calendar;
import be.ouagueni.model.Category;
import be.ouagueni.model.Ride;
import be.ouagueni.model.TypeCat;

public class CreateBalade extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    
    // Champs pour Ride
    private JTextField txtNombrePlaces;
    private JTextField txtLieuDepart;
    private JTextField txtDateDepart;
    private JTextField txtPrix;
    private JComboBox<TypeCat> cmbCategory;
    
    private JButton btnCreer;
    private JButton btnAnnuler;
   

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    CreateBalade frame = new CreateBalade();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public CreateBalade() {
        setTitle("Créer une Balade");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 500, 400);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);
        contentPane.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Panel principal pour les champs
        JPanel fieldsPanel = createFieldsPanel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        contentPane.add(fieldsPanel, gbc);
        
        // Boutons
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        
        btnAnnuler = new JButton("Annuler");
        gbc.gridx = 0;
        contentPane.add(btnAnnuler, gbc);
        
        btnCreer = new JButton("Créer la Balade");
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        contentPane.add(btnCreer, gbc);
        
        // Actions des boutons
        btnCreer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                creerBalade();
            }
        });
        
        btnAnnuler.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private JPanel createFieldsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Informations de la Balade"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Catégorie (Type de vélo)
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Catégorie:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        cmbCategory = new JComboBox<>(TypeCat.values());
        panel.add(cmbCategory, gbc);
        
        // Nombre de places
        gbc.gridx = 0; 
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Nombre de person: "), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtNombrePlaces = new JTextField(20);
        panel.add(txtNombrePlaces, gbc);
        
        // Lieu de départ
        gbc.gridx = 0; 
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Lieu de départ:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtLieuDepart = new JTextField(20);
        panel.add(txtLieuDepart, gbc);
        
        // Date de départ
        gbc.gridx = 0; 
        gbc.gridy = 3;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Date de départ:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtDateDepart = new JTextField(20);
        // Pré-remplir avec la date/heure actuelle
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        panel.add(txtDateDepart, gbc);
        
        // Ajouter une note pour le format
        gbc.gridx = 1;
        gbc.gridy = 4;
        JLabel lblFormat = new JLabel("(Format: yyyy-MM-dd HH:mm)");
        lblFormat.setFont(lblFormat.getFont().deriveFont(10f));
        panel.add(lblFormat, gbc);
        
        // Prix
        gbc.gridx = 0; 
        gbc.gridy = 5;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Prix (€):"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPrix = new JTextField(20);
        panel.add(txtPrix, gbc);
        
        return panel;
    }
    
    private void creerBalade() {
        try {
            // Validation des champs
            if (!validerChamps()) {
                return;
            }
            Connection conn = ClubConnection.getInstance();
            TypeCat selectedType = (TypeCat) cmbCategory.getSelectedItem();
            Category category = new Category();
            category.setNomCategorie(selectedType);

            CategoryController controllerCat = new CategoryController(conn);
            boolean success = controllerCat.updateCategory(category);
            if (success) {
                
                JOptionPane.showMessageDialog(this, 
                    "category créée avec succès!\n\n" +
                    "Catégorie: " + selectedType + "\n" +
                    "id: " + category.getid() + "\n" +
                    JOptionPane.INFORMATION_MESSAGE);

            } else {
                JOptionPane.showMessageDialog(this, 
                    "Erreur lors de la création de la balade.\nVeuillez réessayer.", 
                    "Erreur", 
                    JOptionPane.ERROR_MESSAGE);
            }
            
            // 1️⃣ Créer / récupérer le Calendar pour cette catégorie
            Calendar calendar = new Calendar(category);
            CalendarController controllerCalendar = new CalendarController(conn);
            boolean successCalendar = controllerCalendar.createCalendar(calendar);
            if (!successCalendar) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la création du calendrier !", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2️⃣ Créer le Ride et l'associer au Calendar
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTimeDepart = LocalDateTime.parse(txtDateDepart.getText().trim(), formatter);
            double prix = Double.parseDouble(txtPrix.getText().trim().replace(",", "."));
            int num = Integer.parseInt(txtNombrePlaces.getText().trim());

            Ride ride = new Ride(
                    num,
                    txtLieuDepart.getText().trim(),
                    dateTimeDepart,
                    prix,
                    calendar // <-- Calendar déjà créé
            );

            RideController controller2 = new RideController(conn);
            boolean successRide = controller2.createRide(ride);

            if (successRide) {
                JOptionPane.showMessageDialog(this,
                    "Balade créée avec succès!\n\n" +
                    "Catégorie: " + ride.getCalendar().getCategory() + "\n" +
                    "Num: " + ride.getnum() + "\n" +
                    "Lieu: " + ride.getStartPlace() + "\n" +
                    ride.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n" +
                    "Prix: " + ride.getFee() + " €",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);

                calendar.addRide(ride); 
                for (Ride r : calendar.getRides())
                {
                    System.out.println(r.toString());
                }
                reinitialiserChamps();

            } else {
                JOptionPane.showMessageDialog(this,
                    "Erreur lors de la création de la balade.\nVeuillez réessayer.",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, 
                "Format de date incorrect!\nUtilisez le format: yyyy-MM-dd HH:mm\nExemple: 2025-11-15 14:30", 
                "Erreur de format", 
                JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Le prix doit être un nombre valide!\nExemple: 25.50", 
                "Erreur de format", 
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Erreur lors de la création: " + e.getMessage(), 
                "Erreur", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private boolean validerChamps() {
        if (cmbCategory.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une catégorie.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            cmbCategory.requestFocus();
            return false;
        }
        if (txtNombrePlaces.getText().trim().isEmpty() || !txtNombrePlaces.getText().trim().matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir un numéro valide.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            txtNombrePlaces.requestFocus();
            return false;
        }
        if (txtLieuDepart.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir le lieu de départ.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            txtLieuDepart.requestFocus();
            return false;
        }
        if (txtDateDepart.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir la date de départ.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            txtDateDepart.requestFocus();
            return false;
        }
        if (txtPrix.getText().trim().isEmpty() || !txtPrix.getText().trim().matches("\\d+(\\.\\d{1,2})?")) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir un prix valide.", "Champ manquant", JOptionPane.WARNING_MESSAGE);
            txtPrix.requestFocus();
            return false;
        }
        return true;
    }

    
    private void reinitialiserChamps() {
        cmbCategory.setSelectedIndex(0);
        txtNombrePlaces.setText("");
        txtLieuDepart.setText("");
        txtDateDepart.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        txtPrix.setText("");
        cmbCategory.requestFocus();
    }

}