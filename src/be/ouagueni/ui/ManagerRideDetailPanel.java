package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;

public class ManagerRideDetailPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Manager manager;
    private final Ride ride;
    private final Connection conn;

    private JLabel lblRecapPersonnes, lblRecapVelos;
    private JTextField txtFee;

    public ManagerRideDetailPanel(ClubFrame parentFrame, Manager manager, Ride ride, Connection conn) {
        this.parentFrame = parentFrame;
        this.manager = manager;
        this.ride = ride;
        this.conn = conn;

        // Chargement complet des données via le modèle uniquement
        chargerDonneesCompletement();

        initUI();
        rafraichirRecapitulatif();
    }

    private void chargerDonneesCompletement() {
        // 1. Chargement des inscriptions (avec membres + bikeObj)


        // 2. Chargement des véhicules + passagers + vélos transportés
        ride.loadVehicles(conn); // ← ta méthode qui fait tout bien
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ===================== EN-TÊTE =====================
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createTitledBorder(
            "<html><h2>Balade #" + ride.getnum() + " — Gestion</h2></html>"));

        String info = String.format("""
            <html>
            <b>Lieu de départ :</b> %s<br>
            <b>Date :</b> %s<br>
            <b>Catégorie :</b> %s<br>
            <b>Inscrits :</b> %d membre(s)
            </html>
            """,
            ride.getStartPlace(),
            ride.getStartDate().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy à HH:mm", java.util.Locale.FRENCH)),
            ride.getCalendar().getCategory().getNomCategorie().name(),
            ride.getInscriptions().size()
        );
        header.add(new JLabel(info), BorderLayout.CENTER);

        // Forfait modifiable
        JPanel feePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        feePanel.add(new JLabel("<html><h3>Forfait :</h3></html>"));
        txtFee = new JTextField(String.format("%.2f", ride.getFee()), 10);
        txtFee.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtFee.setHorizontalAlignment(SwingConstants.RIGHT);
        feePanel.add(txtFee);
        feePanel.add(new JLabel("€"));

        JButton btnSaveFee = new JButton("Mettre à jour");
        btnSaveFee.setBackground(new Color(0, 120, 0));
        btnSaveFee.setForeground(Color.WHITE);
        btnSaveFee.addActionListener(e -> mettreAJourForfait());
        feePanel.add(btnSaveFee);

        header.add(feePanel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // ===================== RÉCAPITULATIF =====================
        JPanel recap = new JPanel(new GridLayout(1, 2, 20, 0));
        recap.setBorder(BorderFactory.createTitledBorder("<html><h3>Récapitulatif covoiturage</h3></html>"));

        lblRecapPersonnes = new JLabel();
        lblRecapPersonnes.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblRecapPersonnes.setHorizontalAlignment(SwingConstants.CENTER);

        lblRecapVelos = new JLabel();
        lblRecapVelos.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblRecapVelos.setHorizontalAlignment(SwingConstants.CENTER);

        recap.add(lblRecapPersonnes);
        recap.add(lblRecapVelos);
        add(recap, BorderLayout.WEST);

        // ===================== TABLEAU =====================
        String[] colonnes = {
            "Membre", "Conducteur", "Places pers. offertes", "Places vélo offertes",
            "Passager", "Vélo à transporter", "Transporté par"
        };
        DefaultTableModel model = new DefaultTableModel(colonnes, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        remplirTableau(model);

        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // ===================== BOUTON RETOUR =====================
        JButton btnRetour = new JButton("Retour au tableau de bord");
        btnRetour.addActionListener(e -> parentFrame.showManagerDashboard(manager));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.add(btnRetour);
        add(south, BorderLayout.SOUTH);
    }

    private void remplirTableau(DefaultTableModel model) {
        model.setRowCount(0);

        // Tous les conducteurs d'abord
        for (Vehicle v : ride.getVehicles()) {
            if (v.getDriver() == null) continue;

            model.addRow(new Object[]{
                v.getDriver().getFirstname(),
                "OUI",
                v.getSeatNumber(),
                v.getBikeSpotNumber(),
                "-", "-", "-"
            });
        }

        // Ensuite tous les inscrits
        for (Inscription ins : ride.getInscriptions()) {
            Member m = ins.getMember();
            if (m == null) continue;

            Vehicle veh = ride.getVehicles().stream()
                    .filter(v -> v.getDriver() != null && v.getDriver().equals(m))
                    .findFirst().orElse(null);

            boolean estConducteur = veh != null;

            String transportePar = "-";
            if ((ins.isPassenger() || ins.isBike()) && !estConducteur) {
                for (Vehicle v : ride.getVehicles()) {
                    if (v.getDriver() == null) continue;

                    boolean transporte = false;
                    if (ins.isPassenger() && v.getPassengers().contains(m)) transporte = true;
                    if (ins.isBike() && ins.getBikeObj() != null && v.getBikes().contains(ins.getBikeObj())) transporte = true;

                    if (transporte) {
                        transportePar = v.getDriver().getFirstname();
                        break;
                    }
                }
            }

            model.addRow(new Object[]{
                m.getFirstname(),
                estConducteur ? "OUI" : "Non",
                estConducteur ? veh.getSeatNumber() : "-",
                estConducteur ? veh.getBikeSpotNumber() : "-",
                ins.isPassenger() ? "Oui" : "Non",
                ins.isBike() ? "Oui" : "Non",
                transportePar
            });
        }
    }

    private void rafraichirRecapitulatif() {
        int besoinPersonnes = (int) ride.getInscriptions().stream().filter(Inscription::isPassenger).count();
        int besoinVelos     = (int) ride.getInscriptions().stream().filter(Inscription::isBike).count();

        int offrePersonnes = ride.getVehicles().stream()
                .filter(v -> v.getDriver() != null)
                .mapToInt(Vehicle::getAvailableSeatNumber)
                .sum();

        int offreVelos = ride.getVehicles().stream()
                .filter(v -> v.getDriver() != null)
                .mapToInt(v -> v.getAvailableBikeSpotNumber(ride))
                .sum();

        int deltaP = offrePersonnes - besoinPersonnes;
        int deltaV = offreVelos - besoinVelos;

        lblRecapPersonnes.setText(String.format(
            "<html><center>Places personnes<br>%d offertes / %d nécessaires<br><b style='font-size:18px;'>%s %d</b></center></html>",
            offrePersonnes, besoinPersonnes,
            deltaP >= 0 ? "Excédent" : "<span style='color:red;'>MANQUE</span>",
            Math.abs(deltaP)
        ));
        lblRecapPersonnes.setForeground(deltaP >= 0 ? new Color(0, 140, 0) : Color.RED);

        lblRecapVelos.setText(String.format(
            "<html><center>Places vélos<br>%d offertes / %d nécessaires<br><b style='font-size:18px;'>%s %d</b></center></html>",
            offreVelos, besoinVelos,
            deltaV >= 0 ? "Excédent" : "<span style='color:red;'>MANQUE</span>",
            Math.abs(deltaV)
        ));
        lblRecapVelos.setForeground(deltaV >= 0 ? new Color(0, 140, 0) : Color.RED);
    }

    private void mettreAJourForfait() {
        try {
            double nouveau = Double.parseDouble(txtFee.getText().trim().replace(",", "."));
            if (nouveau < 0) throw new Exception();

            double ancien = ride.getFee();
            ride.setFee(nouveau);

            boolean ok = true;

            if (ok) {
                JOptionPane.showMessageDialog(this,
                    "Forfait mis à jour de " + String.format("%.2f", ancien) + " € → " + String.format("%.2f", nouveau) + " €",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ride.setFee(ancien);
                txtFee.setText(String.format("%.2f", ancien));
                JOptionPane.showMessageDialog(this, "Échec de la sauvegarde en base.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide !", "Erreur", JOptionPane.WARNING_MESSAGE);
        }
    }
}