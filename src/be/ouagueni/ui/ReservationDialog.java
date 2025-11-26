package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ReservationDialog extends JDialog {

    private final Member membre;
    private final AppModel model = AppModel.getInstance();

    // Composants UI que l'on va réutiliser
    private JList<Ride> listRides;
    private JCheckBox chkPassager;
    private JCheckBox chkVelo;
    private JComboBox<Bike> comboVelo;

    public ReservationDialog(ClubFrame parent, Member membre) {
        super(parent, "Réserver une balade", true);
        this.membre = membre;

        // Si aucune sortie compatible → message et fermeture immédiate
        if (model.getRidesCompatiblesPourMembre(membre).isEmpty()) {
            String types = membre.getBikes().stream()
                    .map(b -> model.getLibelleCategorie(b.getType()))
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));

            JOptionPane.showMessageDialog(parent,
                    "Aucune sortie disponible pour vos vélos : " + types,
                    "Aucune sortie compatible", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        initUI();
        pack();
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // ==================== LISTE DES SORTIES ====================
        List<Ride> rides = model.getRidesCompatiblesPourMembre(membre);
        listRides = new JList<>(rides.toArray(new Ride[0]));
        listRides.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listRides.setSelectedIndex(0);

        listRides.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    String cat = model.getLibelleCategorie(r.getCalendar().getCategory().getNomCategorie());
                    setText(String.format("%s | %s | %s | %.2f €",
                            r.getStartDate().toLocalDate(),
                            r.getStartPlace(),
                            cat,
                            r.getFee()));
                }
                return this;
            }
        });

        // ==================== OPTIONS DROITE ====================
        chkPassager = new JCheckBox("Je veux être passager");
        chkPassager.setSelected(true);

        chkVelo = new JCheckBox("Je veux transporter mon vélo");
        comboVelo = new JComboBox<>();
        comboVelo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bike b) {
                    setText(model.getLibelleCategorie(b.getType()) + " - " + b.getWeight() + " kg");
                } else if (value == null) {
                    setText("Aucun vélo compatible");
                }
                return this;
            }
        });

        // Mise à jour dynamique du combo selon la sortie sélectionnée
        listRides.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateVeloCombo();
            }
        });
        chkVelo.addActionListener(e -> comboVelo.setEnabled(chkVelo.isSelected()));

        updateVeloCombo(); // premier appel

        // Panel droit
        JPanel panelOptions = new JPanel();
        panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.Y_AXIS));
        panelOptions.setBorder(BorderFactory.createTitledBorder("Mes besoins"));
        panelOptions.add(Box.createVerticalStrut(20));
        panelOptions.add(chkPassager);
        panelOptions.add(Box.createVerticalStrut(15));
        panelOptions.add(chkVelo);
        panelOptions.add(Box.createVerticalStrut(10));
        panelOptions.add(comboVelo);
        panelOptions.add(Box.createVerticalGlue());

        // ==================== CENTRE ====================
        JPanel center = new JPanel(new GridLayout(1, 2, 30, 0));
        center.setBorder(new EmptyBorder(20, 20, 20, 20));
        center.add(new JScrollPane(listRides));
        center.add(panelOptions);
        add(center, BorderLayout.CENTER);

        // ==================== BOUTONS BAS ====================
        JButton btnReserver = new JButton("Réserver");
        JButton btnAnnuler = new JButton("Annuler");

        btnAnnuler.addActionListener(e -> dispose());

        btnReserver.addActionListener(e -> effectuerReservation());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnReserver);
        bottom.add(btnAnnuler);
        add(bottom, BorderLayout.SOUTH);
    }

    private void updateVeloCombo() {
        Ride ride = listRides.getSelectedValue();
        comboVelo.removeAllItems();

        if (ride == null) {
            comboVelo.setEnabled(false);
            chkVelo.setEnabled(false);
            return;
        }

        List<Bike> velosCompatibles = model.getVelosCompatiblesPourRide(membre, ride);

        if (velosCompatibles.isEmpty()) {
            comboVelo.addItem(null);
            comboVelo.setEnabled(false);
            chkVelo.setEnabled(false);
            chkVelo.setText("Je veux transporter mon vélo (aucun compatible)");
        } else {
            velosCompatibles.forEach(comboVelo::addItem);
            comboVelo.setSelectedIndex(0);
            comboVelo.setEnabled(chkVelo.isSelected());
            chkVelo.setEnabled(true);
            chkVelo.setText("Je veux transporter mon vélo");
        }
    }

    private void effectuerReservation() {
        Ride rideSelectionnee = listRides.getSelectedValue();
        if (rideSelectionnee == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean veutPassager = chkPassager.isSelected();
        boolean veutVelo = chkVelo.isSelected();
        Bike veloChoisi = veutVelo ? (Bike) comboVelo.getSelectedItem() : null;

        // Toute la logique métier est maintenant dans AppModel
        AppModel.ReservationResult resultat = model.reserverBaladeAvecVerificationComplete(
                membre, rideSelectionnee, veutPassager, veutVelo, veloChoisi);

        if (resultat.succes) {
            JOptionPane.showMessageDialog(this,
                    "<html><h2 style='color:#2E8B57; text-align:center;'>Réservation confirmée !</h2><br>" +
                    "<b>Sortie :</b> " + rideSelectionnee.getStartPlace() + "<br>" +
                    "<b>Date :</b> " + rideSelectionnee.getStartDate().toLocalDate() + "<br>" +
                    "<b>Forfait débité :</b> " + String.format("%.2f €", rideSelectionnee.getFee()) + "<br>" +
                    "<b>Nouveau solde :</b> " + String.format("%.2f €", resultat.nouveauSolde) + "<br><br>" +
                    "<b>Conducteur :</b> " + resultat.conducteur.getFirstname() + " " + resultat.conducteur.getName() +
                    "<br><br><i>Bon ride !</i></html>",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            // Message d'erreur déjà bien formaté dans le modèle
            JOptionPane.showMessageDialog(this, resultat.message,
                    "Réservation impossible", resultat.message.contains("déjà inscrit") ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
        }
    }
}