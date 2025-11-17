// be.ouagueni.ui.TreasurerDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
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
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
                "Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 130, 80));

        // --- Boutons ---
        JPanel topButtons = new JPanel(new GridLayout(1, 3, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnReminder = new JButton("Envoyer rappel lettre");
        JButton btnPayDriver = new JButton("Payer conducteur");
        JButton btnClaimFee = new JButton("Réclamer frais");

        topButtons.add(btnReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        // --- northPanel : CORRECT ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS)); // ← ICI, c'est bon
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        add(northPanel, BorderLayout.NORTH); // ← Ajouté ici

        // --- Zone centrale ---
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(new JLabel("Sélectionnez une action ci-dessus.", JLabel.CENTER));
        add(new JScrollPane(center), BorderLayout.CENTER);

        // --- Déconnexion ---
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        add(south, BorderLayout.SOUTH);

        // --- Actions ---
        btnReminder.addActionListener(this::sendReminderLetter);
        btnPayDriver.addActionListener(this::payDriver);
        btnClaimFee.addActionListener(this::claimFee);
    }

    // ========================================
    // 1. ENVOYER RAPPEL LETTRE
    // ========================================
    private void sendReminderLetter(ActionEvent e) {
        List<Member> debtors = treasurer.getMembersInDebt(conn);
        if (debtors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun membre en dette.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("Membres en dette :\n\n");
        for (Member m : debtors) {
            msg.append(String.format("• %s %s : %.2f €\n", m.getFirstname(), m.getName(), -m.getBalance()));
        }

        int choice = JOptionPane.showConfirmDialog(this, msg + "\n\nEnvoyer un rappel ?", "Rappel", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, debtors.size() + " rappels envoyés (simulation).", "Succès", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ========================================
    // 2. PAYER CONDUCTEUR
    // ========================================
    private void payDriver(ActionEvent e) {
        List<Object[]> payments = treasurer.getDriverPayments(conn);
        if (payments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun paiement à effectuer.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(payments.size() + 1, 3, 10, 10));
        panel.add(new JLabel("Conducteur"));
        panel.add(new JLabel("Montant"));
        panel.add(new JLabel("Payer ?"));

        JCheckBox[] boxes = new JCheckBox[payments.size()];
        for (int i = 0; i < payments.size(); i++) {
            Object[] p = payments.get(i);
            panel.add(new JLabel((String) p[0]));
            panel.add(new JLabel(String.format("%.2f €", (Double) p[1])));
            boxes[i] = new JCheckBox();
            boxes[i].setSelected(true);
            panel.add(boxes[i]);
        }

        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(panel), "Payer conducteurs", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION) {
            int count = 0;
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isSelected()) {
                    int memberId = (Integer) payments.get(i)[2];
                    double amount = (Double) payments.get(i)[1];
                    if (treasurer.payDriver(conn, memberId, amount)) {
                        count++;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, count + " paiements effectués.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ========================================
    // 3. RÉCLAMER FRAIS
    // ========================================
    private void claimFee(ActionEvent e) {
        List<Ride> rides = treasurer.getUnpaidRides(conn);
        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun frais à réclamer.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<Ride> combo = new JComboBox<>(rides.toArray(new Ride[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Ride r) {
                    setText(r.getStartDate().toLocalDate() + " - " + r.getStartPlace() + " (" + r.getFee() + " €)");
                }
                return this;
            }
        });

        int choice = JOptionPane.showConfirmDialog(this, combo, "Sélectionner une sortie", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION) {
            Ride selected = (Ride) combo.getSelectedItem();
            List<Member> unpaid = treasurer.getUnpaidMembersForRide(conn, selected.getId());
            if (unpaid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tous les membres ont payé.", "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder msg = new StringBuilder("Membres non payés :\n\n");
                for (Member m : unpaid) {
                    msg.append("• ").append(m.getFirstname()).append(" ").append(m.getName()).append("\n");
                }
                JOptionPane.showMessageDialog(this, msg.toString(), "À réclamer", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}