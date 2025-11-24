package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;

public class BikeDialog extends JDialog {
    public BikeDialog(ClubFrame parent, Member member, Bike bikeToEdit, Runnable onSave) {
        super(parent, bikeToEdit == null ? "Ajouter un vélo" : "Modifier le vélo", true);
        setSize(400, 300);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JComboBox<TypeCat> comboType = new JComboBox<>(TypeCat.values());
        JTextField txtPoids = new JTextField(10);
        JTextField txtLongueur = new JTextField(10);

        if (bikeToEdit != null) {
            comboType.setSelectedItem(bikeToEdit.getType());
            txtPoids.setText(String.valueOf(bikeToEdit.getWeight()));
            txtLongueur.setText(String.valueOf(bikeToEdit.getLength()));
        }

        panel.add(new JLabel("Type de vélo :"));
        panel.add(comboType);
        panel.add(new JLabel("Poids (kg) :"));
        panel.add(txtPoids);
        panel.add(new JLabel("Longueur (cm) :"));
        panel.add(txtLongueur);

        JButton btnSave = new JButton("Enregistrer");
        btnSave.addActionListener(e -> {
            try {
                TypeCat type = (TypeCat) comboType.getSelectedItem();
                double poids = Double.parseDouble(txtPoids.getText().replace(',', '.'));
                double longueur = Double.parseDouble(txtLongueur.getText().replace(',', '.'));

                Bike bike = bikeToEdit != null ? bikeToEdit : new Bike();
                bike.setType(type);
                bike.setWeight(poids);
                bike.setLength(longueur);
                bike.setOwner(member);

                boolean success = bikeToEdit == null ? bike.create(AppModel.getInstance().getConnection())
                                                    : bike.update(AppModel.getInstance().getConnection());

                if (success) {
                    if (bikeToEdit == null) member.getBikes().add(bike);
                    JOptionPane.showMessageDialog(this, "Vélo enregistré !");
                    dispose();
                    onSave.run();
                }
                else {
					JOptionPane.showMessageDialog(this, "Échec de l'enregistrement du vélo.Vélo possiblement déjà présent sur le trajet", "Erreur", JOptionPane.ERROR_MESSAGE);
				}
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Données invalides.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel south = new JPanel();
        south.add(btnSave);
        add(panel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }
}