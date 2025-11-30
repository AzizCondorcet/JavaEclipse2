package be.ouagueni.ui;

import be.ouagueni.model.Treasurer;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class TreasurerDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Treasurer treasurer;
    private final java.sql.Connection conn;

    private JButton btnReminder;
    private JButton btnPayDriver;
    private JButton btnClaimFee;

    public TreasurerDashboardPanel(ClubFrame parentFrame, Treasurer treasurer, java.sql.Connection conn) {
        this.parentFrame = parentFrame;
        this.treasurer = treasurer;
        this.conn = conn;
        initUI();
        setupActions();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("Bienvenue " + treasurer.getFirstname() + " " + treasurer.getName() + " (Trésorier)", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(0, 100, 0));

        JPanel topButtons = new JPanel(new GridLayout(1, 3, 30, 30));
        topButtons.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        btnReminder  = new JButton("Envoyer rappel de paiement");
        btnPayDriver = new JButton("Valider paiements");
        btnClaimFee  = new JButton("Réclamer les frais non payés");

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 18);
        Dimension buttonSize = new Dimension(300, 80);

        for (JButton btn : new JButton[]{btnReminder, btnPayDriver, btnClaimFee}) {
            btn.setFont(buttonFont);
            btn.setPreferredSize(buttonSize);
            btn.setFocusPainted(false);
        }

        topButtons.add(btnReminder);
        topButtons.add(btnPayDriver);
        topButtons.add(btnClaimFee);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(lblTitle, BorderLayout.NORTH);
        northPanel.add(topButtons, BorderLayout.CENTER);

        add(northPanel, BorderLayout.NORTH);
        add(new JLabel("Sélectionnez une action ci-dessus", JLabel.CENTER), BorderLayout.CENTER);

        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> parentFrame.showPanel("login"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(btnLogout);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void setupActions() {
        btnReminder.addActionListener(e -> handleReminder());
        btnPayDriver.addActionListener(e -> handlePayDriver());
        btnClaimFee.addActionListener(e -> handleClaimFee());
    }

    // ==================================================================
    // LOGIQUE UI
    // ==================================================================

    private void handleReminder() {
        List<Member> debtors = treasurer.sendReminderLetter(conn);

        if (debtors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tous les membres sont à jour !", "Parfait", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double total = debtors.stream().mapToDouble(m -> -m.getBalance()).sum();
        StringBuilder preview = new StringBuilder("<html><b>Membres en dette :</b><br><br>");
        for (Member m : debtors) {
            double due = -m.getBalance();
            preview.append(String.format("• %s %s → %.2f €<br>", m.getFirstname(), m.getName(), due));
        }
        preview.append("<br><b>Total dû : ").append(String.format("%.2f €", total)).append("</b></html>");

        int choice = JOptionPane.showConfirmDialog(this, preview.toString(), "Envoyer les rappels ?", 
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            String msg = debtors.size() == 1 ? "1 personne a reçu un mail de rappel." 
                : debtors.size() + " personnes ont reçu un mail de rappel.";
            JOptionPane.showMessageDialog(this, msg, "Rappels envoyés", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handlePayDriver() {
        List<Ride> rides = treasurer.payDriver(conn);

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie passée avec des passagers à valider.", 
                "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Ride selected = (Ride) JOptionPane.showInputDialog(this, "Sélectionnez la sortie à traiter :", 
            "Valider paiements covoiturage", JOptionPane.QUESTION_MESSAGE, null, rides.toArray(), rides.get(0));

        if (selected == null) return;

        List<Member> passengers = treasurer.getPassengersForRide(conn, selected.getId());
        if (passengers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun passager inscrit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JList<Member> list = new JList<>(passengers.toArray(new Member[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(8);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(450, 300));

        int option = JOptionPane.showConfirmDialog(this, scroll,
            "Sélectionnez les membres qui ont payé – " + selected.getStartPlace(),
            JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return;

        List<Integer> selectedIds = list.getSelectedValuesList()
            .stream().map(Member::getIdMember).collect(Collectors.toList());

        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun membre sélectionné.", "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int updated = treasurer.confirmPassengerPayments(conn, selected.getId(), selectedIds);
        String message = updated == 1 ? "1 paiement validé !" : updated + " paiements validés !";
        JOptionPane.showMessageDialog(this, message, "Succès", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleClaimFee() {
        List<Ride> rides = treasurer.claimFee(conn);

        if (rides.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune sortie avec passagers.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Ride selected = (Ride) JOptionPane.showInputDialog(this, "Sélectionnez la sortie à réclamer :", 
            "Réclamer les frais", JOptionPane.QUESTION_MESSAGE, null, rides.toArray(), rides.get(0));

        if (selected == null) return;

        List<Member> passengers = treasurer.getPassengersForRide(conn, selected.getId());
        if (passengers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucun passager inscrit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder("<html><b>Dettes pour : </b>")
            .append(selected.getStartPlace()).append(" (")
            .append(selected.getStartDate().toLocalDate()).append(")<br><br>");

        for (Member m : passengers) {
            msg.append("• ").append(m.getFirstname()).append(" ").append(m.getName()).append("<br>");
        }
        msg.append("</html>");

        JOptionPane.showMessageDialog(this, msg.toString(), "Membres à réclamer", JOptionPane.WARNING_MESSAGE);
    }
}