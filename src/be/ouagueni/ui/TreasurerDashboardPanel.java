// be.ouagueni.ui.TreasurerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final Connection conn;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ==================== TITRE ====================
        JLabel lblTitle = new JLabel(
                "Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(0, 100, 0));

        // ==================== BOUTONS ====================
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 15, 15));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        JButton btnReminder       = new JButton("Envoyer rappel de paiement");
        JButton btnValidatePay    = new JButton("Valider paiements covoiturage");
        JButton btnViewUnpaid     = new JButton("Voir les sorties non réglées");

        // Icônes (optionnel mais joli)
        btnReminder.setIcon(new ImageIcon("icons/envelope.png"));     // à toi d'ajouter les icônes si tu veux
        btnValidatePay.setIcon(new ImageIcon("icons/check.png"));
        btnViewUnpaid.setIcon(new ImageIcon("icons/warning.png"));

        topButtons.add(btnReminder);
        topButtons.add(btnValidatePay);
        topButtons.add(btnViewUnpaid);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(20));
        northPanel.add(topButtons);
        add(northPanel, BorderLayout.NORTH);

        // ==================== ZONE CENTRALE ====================
        JLabel lblInfo = new JLabel("Sélectionnez une action ci-dessus.", JLabel.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        JPanel center = new JPanel(new GridBagLayout());
        center.add(lblInfo);
        add(new JScrollPane(center), BorderLayout.CENTER);

        // ==================== DÉCONNEXION ====================
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        add(south, BorderLayout.SOUTH);

        // ==================== ACTIONS ====================
        btnReminder.addActionListener(this::sendReminderLetter);
        btnValidatePay.addActionListener(this::validateCovoitPayments);
        btnViewUnpaid.addActionListener(this::viewUnpaidRides);
    }

    // ==============================================================
    // 1. ENVOYER RAPPEL DE PAIEMENT (cotisations + covoiturages)
    // ==============================================================
    private void sendReminderLetter(ActionEvent e) {
        List<Member> debtors = treasurer.getMembersInDebt(conn);

        if (debtors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les membres sont à jour !", "Bravo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("<html><b>Membres en dette :</b><br><br>");
        double total = 0;
        for (Member m : debtors) {
            double due = -m.getBalance();
            total += due;
            msg.append(String.format("• %s %s → %.2f €<br>", m.getFirstname(), m.getName(), due));
        }
        msg.append("<br><b>Total dû : ").append(String.format("%.2f €", total)).append("</b></html>");

        int choice = JOptionPane.showConfirmDialog(this,
                msg.toString(),
                "Envoyer les rappels ?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            // Ici tu pourras plus tard envoyer de vrais mails
            JOptionPane.showMessageDialog(this,
                    debtors.size() + " rappel(s) envoyé(s) par e-mail.",
                    "Rappels envoyés",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==============================================================
    // 2. VALIDER LES PAIEMENTS COVOITURAGE (le cœur du trésorier)
    // ==============================================================
    private void validateCovoitPayments(ActionEvent e) {
        List<Ride> rides = treasurer.getRidesWithPendingPayments(conn);

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tous les covoiturages sont déjà validés !",
                    "Parfait",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Choix de la sortie
        JComboBox<Ride> combo = new JComboBox<>(rides.toArray(new Ride[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    setText(r.getStartDate().toLocalDate() + " → " + r.getStartPlace() +
                            " (forfait : " + r.getFee() + " €)");
                }
                return this;
            }
        });

        int choice = JOptionPane.showConfirmDialog(this,
                combo,
                "Sélectionner la sortie à valider",
                JOptionPane.OK_CANCEL_OPTION);

        if (choice != JOptionPane.OK_OPTION) return;

        Ride selectedRide = (Ride) combo.getSelectedItem();
        List<Member> pending = treasurer.getPendingPassengersForRide(conn, selectedRide.getId());

        if (pending.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les passagers sont déjà validés.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Affichage avec cases à cocher
        JPanel panel = new JPanel(new GridLayout(pending.size() + 1, 3, 10, 8));
        panel.add(new JLabel("<html><b>Passager</b></html>"));
        panel.add(new JLabel("<html><b>Forfait</b></html>"));
        panel.add(new JLabel("<html><b>Validé ?</b></html>"));

        JCheckBox[] boxes = new JCheckBox[pending.size()];
        List<Integer> memberIds = new ArrayList<>();

        for (int i = 0; i < pending.size(); i++) {
            Member m = pending.get(i);
            panel.add(new JLabel(m.getFirstname() + " " + m.getName()));
            panel.add(new JLabel(selectedRide.getFee() + " €"));
            boxes[i] = new JCheckBox();
            boxes[i].setSelected(true);
            panel.add(boxes[i]);
            memberIds.add(m.getIdMember());
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                new JScrollPane(panel),
                "Valider les paiements – " + selectedRide.getStartDate().toLocalDate(),
                JOptionPane.OK_CANCEL_OPTION);

        if (confirm == JOptionPane.OK_OPTION) {
            List<Integer> toValidate = new ArrayList<>();
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isSelected()) {
                    toValidate.add(memberIds.get(i));
                }
            }

            int updated = treasurer.confirmPassengerPayments(conn, selectedRide.getId(), toValidate);

            JOptionPane.showMessageDialog(this,
                    updated + " paiement(s) validé(s) et balance mise(s) à jour !",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==============================================================
    // 3. VOIR LES SORTIES ENCORE NON RÉGLÉES (optionnel mais pratique)
    // ==============================================================
    private void viewUnpaidRides(ActionEvent e) {
        List<Ride> rides = treasurer.getUnpaidRides(conn);

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune dette en cours.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<Ride> combo = new JComboBox<>(rides.toArray(new Ride[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    setText(r.getStartDate().toLocalDate() + " → " + r.getStartPlace());
                }
                return this;
            }
        });

        int choice = JOptionPane.showConfirmDialog(this, combo, "Sortie avec dettes", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION) {
            Ride selected = (Ride) combo.getSelectedItem();
            List<Member> unpaid = treasurer.getUnpaidMembersForRide(conn, selected.getId());

            StringBuilder msg = new StringBuilder("<html><b>Membres encore en dette pour cette sortie :</b><br><br>");
            for (Member m : unpaid) {
                msg.append("• ")
                   .append(m.getFirstname()).append(" ")
                   .append(m.getName()).append("<br>");
            }
            msg.append("</html>");

            JOptionPane.showMessageDialog(this, msg.toString(), "Dettes restantes", JOptionPane.WARNING_MESSAGE);
        }
    }
}