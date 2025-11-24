package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class DisponibiliteDialog extends JDialog {

    private final Member conducteur;
    private final AppModel model = AppModel.getInstance();

    public DisponibiliteDialog(ClubFrame parent, Member conducteur) {
        super(parent, "Poster mes disponibilités", true);
        this.conducteur = conducteur;

        List<Ride> ridesCompatibles = model.getRidesCompatiblesConducteur(conducteur);

        if (ridesCompatibles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Aucune sortie compatible avec vos catégories de vélo.",
                    "Aucune sortie", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        setSize(900, 550);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(15, 15));

        // === PANEL GAUCHE : Liste des sorties ===
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Sorties compatibles"));

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (Ride r : ridesCompatibles) {
            String txt = String.format("%s → %s (%s)",
                    r.getStartDate().toLocalDate(),
                    r.getStartPlace(),
                    r.getCalendar().getCategory().getNomCategorie().name());
            listModel.addElement(txt);
        }

        JList<String> listRides = new JList<>(listModel);
        listRides.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listRides.setSelectedIndex(0);
        left.add(new JScrollPane(listRides), BorderLayout.CENTER);

        // === PANEL DROIT : Formulaire véhicule ===
        JPanel right = new JPanel(new GridLayout(4, 2, 10, 20));
        right.setBorder(BorderFactory.createTitledBorder("Places proposées"));

        right.add(new JLabel("Places passagers :"));
        JSpinner spinPassagers = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        right.add(spinPassagers);

        right.add(new JLabel("Places vélo :"));
        JSpinner spinVelos = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
        right.add(spinVelos);

        // Chargement véhicule existant
        try {
            Vehicle v = Vehicle.getOrCreateForDriver(conducteur, model.getConnection());
            spinPassagers.setValue(v.getSeatNumber());
            spinVelos.setValue(v.getBikeSpotNumber());
        } catch (Exception ignored) {}

        // === BOUTONS ===
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Valider");
        JButton btnCancel = new JButton("Annuler");
        btnCancel.addActionListener(e -> dispose());
        bottom.add(btnOk);
        bottom.add(btnCancel);

        btnOk.addActionListener(e -> {
            int idx = listRides.getSelectedIndex();
            if (idx == -1) {
                JOptionPane.showMessageDialog(this, "Sélectionnez une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int passagers = (Integer) spinPassagers.getValue();
            int velos = (Integer) spinVelos.getValue();

            if (passagers <= 0 && velos <= 0) {
                JOptionPane.showMessageDialog(this, "Proposez au moins une place.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Ride ride = ridesCompatibles.get(idx);

            try {
                boolean success = model.posterDisponibilites(conducteur, ride, passagers, velos);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "<html><h3>Disponibilités enregistrées !</h3>" +
                            passagers + " place(s) passager<br>" +
                            velos + " place(s) vélo<br><br>" +
                            "<i>Sortie : " + ride.getStartPlace() + "</i></html>",
                            "Succès", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Assemblage
        JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        main.add(left);
        main.add(right);

        add(main, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}