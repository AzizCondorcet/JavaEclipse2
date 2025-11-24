package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ReservationDialog extends JDialog {

    private final Member membre;
    private final AppModel model = AppModel.getInstance();

    public ReservationDialog(ClubFrame parent, Member membre) {
        super(parent, "Réserver une balade", true);
        this.membre = membre;

        List<Ride> rides = model.getRidesFutures();

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Aucune sortie future disponible.", "Info", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        setSize(1000, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // === Liste des sorties ===
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Sorties disponibles"));

        JList<Ride> listRides = new JList<>(rides.toArray(new Ride[0]));
        listRides.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listRides.setSelectedIndex(0);
        listRides.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    String cat = r.getCalendar() != null && r.getCalendar().getCategory() != null
                            ? r.getCalendar().getCategory().getNomCategorie().name() : "Inconnue";
                    setText(String.format("%s | %s | %s | %.2f €",
                            r.getStartDate().toLocalDate(), r.getStartPlace(), cat, r.getFee()));
                }
                return this;
            }
        });
        left.add(new JScrollPane(listRides), BorderLayout.CENTER);

        // === Options ===
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createTitledBorder("Mes besoins"));

        JCheckBox chkPassager = new JCheckBox("Je veux être passager");
        chkPassager.setSelected(true);
        JCheckBox chkVelo = new JCheckBox("Je veux transporter mon vélo");
        JComboBox<Bike> comboVelo = new JComboBox<>();

        if (membre.getBikes().isEmpty()) {
            chkVelo.setEnabled(false);
            comboVelo.addItem(null);
            comboVelo.setEnabled(false);
        } else {
            membre.getBikes().forEach(comboVelo::addItem);
            comboVelo.setEnabled(false);
        }

        comboVelo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bike b) {
                    setText(b.getType() + " - " + b.getWeight() + "kg");
                } else {
                    setText("Aucun vélo");
                }
                return this;
            }
        });

        chkVelo.addActionListener(e -> comboVelo.setEnabled(chkVelo.isSelected() && membre.getBikes().size() > 0));

        right.add(Box.createVerticalStrut(20));
        right.add(chkPassager);
        right.add(Box.createVerticalStrut(10));
        right.add(chkVelo);
        right.add(comboVelo);
        right.add(Box.createVerticalGlue());

        // === Boutons ===
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnReserver = new JButton("Réserver");
        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.addActionListener(e -> dispose());
        bottom.add(btnReserver);
        bottom.add(btnAnnuler);

        btnReserver.addActionListener(e -> {
            Ride ride = listRides.getSelectedValue();
            if (ride == null) {
                JOptionPane.showMessageDialog(this, "Sélectionnez une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean passager = chkPassager.isSelected();
            boolean avecVelo = chkVelo.isSelected();
            Bike velo = avecVelo ? (Bike) comboVelo.getSelectedItem() : null;

            if (!passager && !avecVelo) {
                JOptionPane.showMessageDialog(this, "Choisissez au moins une option.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AppModel.ReservationResult result = model.reserverBalade(membre, ride, passager, avecVelo, velo);

            if (result.succes) {
                JOptionPane.showMessageDialog(this,
                        "<html><h3>Réservation confirmée !</h3>" +
                        "Sortie : " + ride.getStartPlace() + "<br>" +
                        "Date : " + ride.getStartDate().toLocalDate() + "<br>" +
                        "Forfait débité : " + String.format("%.2f €", ride.getFee()) + "<br>" +
                        "Nouveau solde : " + String.format("%.2f €", result.nouveauSolde) + "<br><br>" +
                        "Conducteur : " + result.conducteur.getFirstname() + " " + result.conducteur.getName() +
                        "</html>", "Succès", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, result.message, "Impossible", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Assemblage
        JPanel center = new JPanel(new GridLayout(1, 2, 20, 0));
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        center.add(left);
        center.add(right);

        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}