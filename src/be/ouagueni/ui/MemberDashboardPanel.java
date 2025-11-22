// be.ouagueni.ui.MemberDashboardPanel.java
package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

public class MemberDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private final Member currentMember;
    private final JLabel lblBalance = new JLabel("", JLabel.CENTER);
    private final JPanel topButtons = new JPanel(new GridLayout(3, 2, 15, 15));

    public MemberDashboardPanel(ClubFrame parentFrame, Member member, Connection conn) {
        this.parentFrame = parentFrame;
        this.currentMember = member;
        this.conn = conn;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buildUI();
        refreshBalanceAndButtons();
    }

    private void buildUI() {
        // === En-tête ===
        JLabel title = new JLabel("Bienvenue " + currentMember.getFirstname() + " " + currentMember.getName() + " (Membre)", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(20, 80, 130));
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(title);
        north.add(Box.createVerticalStrut(10));
        north.add(lblBalance);
        north.add(Box.createVerticalStrut(15));
        north.add(topButtons);
        add(north, BorderLayout.NORTH);

        // === Boutons ===
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
        JButton[] buttons = {
            new JButton("Poster mes disponibilités"),
            new JButton("Réserver une balade"),
            new JButton("Choisir une catégorie"),
            new JButton("Payer cotisation"),
            new JButton("Ajouter fonds"),
            new JButton("") // case vide pour GridLayout 3x2
        };
        for (JButton b : buttons) {
            if (!b.getText().isEmpty()) {
                b.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                topButtons.add(b);
            }
        }
        buttons[0].addActionListener(e -> new AvailabilityDialog().setVisible(true));
        buttons[1].addActionListener(e -> new ReservationDialog().setVisible(true));
        buttons[2].addActionListener(e -> chooseCategory());
        buttons[3].addActionListener(e -> payCotisation());
        buttons[4].addActionListener(e -> depositFunds());

        // === Liste inscriptions ===
        add(new JScrollPane(createInscriptionsPanel()), BorderLayout.CENTER);

        // === Déconnexion ===
        JButton logout = new JButton("Déconnexion");
        logout.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(logout);
        add(south, BorderLayout.SOUTH);
    }

    // ====================== ACTIONS SIMPLES ======================
    private void chooseCategory() {
        try {
            var result = currentMember.getAvailableCategories(conn);
            if (!result.hasAvailable()) {
                JOptionPane.showMessageDialog(this, result.message(), "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JComboBox<Category> combo = new JComboBox<>(result.availableCategories().toArray(new Category[0]));
            combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JLabel l = new JLabel();
                if (value instanceof Category c) l.setText(c.getNomCategorie().toString());
                return l;
            });
            int choice = JOptionPane.showConfirmDialog(this, combo, "Choisir une catégorie", JOptionPane.OK_CANCEL_OPTION);
            if (choice == JOptionPane.OK_OPTION) {
                Category cat = (Category) combo.getSelectedItem();
                boolean ok = currentMember.addCategoryAndPersist(cat, conn);
                JOptionPane.showMessageDialog(this, ok ? "Catégorie ajoutée !" : "Échec", ok ? "Succès" : "Erreur",
                    ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void payCotisation() {
        var info = currentMember.getCotisationInfo();
        if (currentMember.getBalance() >= info.amountDue()) {
            JOptionPane.showMessageDialog(this, "Cotisation déjà payée !", "À jour", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(this, info.formattedDetails() + "\n\nMontant :", String.format("%.2f", info.amountDue()));
        if (input == null || input.isBlank()) return;
        try {
            double amount = Double.parseDouble(input.replace(',', '.'));
            boolean ok = currentMember.payAmount(amount, conn);
            JOptionPane.showMessageDialog(this, ok ? "Paiement enregistré !" : "Échec", ok ? "Succès" : "Erreur",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            refreshBalanceAndButtons();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void depositFunds() {
        String input = JOptionPane.showInputDialog(this, "Montant à créditer :");
        if (input == null || input.isBlank()) return;
        try {
            double amount = Double.parseDouble(input.replace(',', '.'));
            boolean ok = currentMember.depositFunds(amount, conn);
            JOptionPane.showMessageDialog(this, ok ? "Crédit ajouté !" : "Échec", ok ? "Succès" : "Erreur",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            refreshBalanceAndButtons();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================== DIALOGUES (Vue uniquement) ======================

    private class AvailabilityDialog extends JDialog 
    {
        AvailabilityDialog() {
            super((Frame) SwingUtilities.getWindowAncestor(MemberDashboardPanel.this), "Poster mes disponibilités", true);
            setSize(800, 550);
            setLocationRelativeTo(MemberDashboardPanel.this);

            Set<Ride> allRides = Ride.allRides(conn);
            List<Ride> eligible = allRides.stream()
                .filter(r -> currentMember.getBikes().stream()
                    .anyMatch(b -> b.getType() == r.getCalendar().getCategory().getNomCategorie()))
                .sorted(Comparator.comparing(Ride::getStartDate))
                .toList();

            if (eligible.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Aucune sortie compatible avec vos vélos.", "Info", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                return;
            }

            JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
            main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Liste des sorties
            DefaultListModel<String> model = new DefaultListModel<>();
            for (Ride r : eligible) model.addElement(r.getStartPlace() + " – " + r.getStartDate().toLocalDate());
            JList<String> list = new JList<>(model);
            list.setSelectedIndex(0);
            main.add(new JScrollPane(list));

            // Formulaire places
            JPanel form = new JPanel(new GridLayout(3, 2, 10, 15));
            form.add(new JLabel("Places passagers :"));
            JSpinner seats = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
            form.add(seats);
            form.add(new JLabel("Places vélo :"));
            JSpinner bikes = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
            form.add(bikes);
            main.add(form);

            // Boutons
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("Valider");
            JButton cancel = new JButton("Annuler");
            cancel.addActionListener(e -> dispose());
            btns.add(ok); btns.add(cancel);

            ok.addActionListener(e -> {
                Ride ride = eligible.get(list.getSelectedIndex());
                int s = (Integer) seats.getValue();
                int b = (Integer) bikes.getValue();
                var result = currentMember.postDriverAvailability(ride, s, b, conn);
                JOptionPane.showMessageDialog(this, result.message(), result.success() ? "Succès" : "Erreur",
                    result.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                if (result.success()) {
                    dispose();
                    refreshBalanceAndButtons();
                }
            });

            add(main, BorderLayout.CENTER);
            add(btns, BorderLayout.SOUTH);
        }
    }

    private class ReservationDialog extends JDialog {
        ReservationDialog() {
            super((Frame) SwingUtilities.getWindowAncestor(MemberDashboardPanel.this), "Réserver une balade", true);
            setSize(950, 650);
            setLocationRelativeTo(MemberDashboardPanel.this);

            List<Ride> futureRides = Ride.allRides(conn).stream()
                .filter(r -> r.getStartDate() != null && r.getStartDate().isAfter(java.time.LocalDateTime.now()))
                .sorted(Comparator.comparing(Ride::getStartDate))
                .toList();

            if (futureRides.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Aucune sortie future.", "Info", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                return;
            }

            JPanel main = new JPanel(new GridLayout(1, 2, 20, 0));
            main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Liste
            JList<Ride> list = new JList<>(futureRides.toArray(new Ride[0]));
            list.setCellRenderer((l, r, idx, sel, focus) -> new DefaultListCellRenderer()
                .getListCellRendererComponent(l, r.getStartDate().toLocalDate() + " | " + r.getStartPlace() + " | " +
                    r.getCalendar().getCategory().getNomCategorie() + " | " + r.getFee() + " €", idx, sel, focus));
            list.setSelectedIndex(0);
            main.add(new JScrollPane(list));

            // Options
            JPanel opts = new JPanel();
            opts.setLayout(new BoxLayout(opts, BoxLayout.Y_AXIS));
            JCheckBox pass = new JCheckBox("Je veux être passager", true);
            JCheckBox bike = new JCheckBox("Avec mon vélo");
            JComboBox<Bike> comboBike = new JComboBox<>(currentMember.getBikes().toArray(new Bike[0]));
            comboBike.setEnabled(false);
            bike.addActionListener(e -> comboBike.setEnabled(bike.isSelected() && currentMember.getBikes().size() > 0));
            opts.add(pass);
            opts.add(Box.createVerticalStrut(10));
            opts.add(bike);
            opts.add(comboBike);
            main.add(opts);

            // Boutons
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("Réserver");
            JButton cancel = new JButton("Annuler");
            cancel.addActionListener(e -> dispose());
            btns.add(ok); btns.add(cancel);

            ok.addActionListener(e -> {
                Ride ride = list.getSelectedValue();
                boolean asPass = pass.isSelected();
                boolean withB = bike.isSelected();
                Bike selectedBike = withB ? (Bike) comboBike.getSelectedItem() : null;

                var result = currentMember.bookRide(ride, asPass, withB, selectedBike, conn);

                JOptionPane.showMessageDialog(this, result.message(),
                    result.success() ? "Succès" : "Erreur",
                    result.success() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

                if (result.success()) {
                    dispose();
                    refreshBalanceAndButtons();
                }
            });

            add(main, BorderLayout.CENTER);
            add(btns, BorderLayout.SOUTH);
        }
    }

    // ====================== RAFRAÎCHISSEMENT ======================
    private void refreshBalanceAndButtons() {
        boolean ok = currentMember.canParticipate();
        String txt = currentMember.getBalanceStatus();
        lblBalance.setText("<html><b>" + txt + "</b></html>");
        lblBalance.setForeground(ok ? (currentMember.getBalance() > 0 ? Color.BLUE : Color.GREEN) : Color.RED);

        for (Component c : topButtons.getComponents()) {
            if (c instanceof JButton b && (b.getText().contains("Poster") || b.getText().contains("Réserver"))) {
                b.setEnabled(ok);
                b.setToolTipText(ok ? null : "Règlez votre cotisation");
            }
        }

        // Recharger la liste des inscriptions
        Component center = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (center != null) remove(center);
        add(new JScrollPane(createInscriptionsPanel()), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createInscriptionsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        if (currentMember.getInscriptions().isEmpty()) {
            p.add(new JLabel("Aucune inscription."));
            return p;
        }
        for (Inscription i : currentMember.getInscriptions()) {
            String txt = "<html><b>" + i.getRide().getStartPlace() + "</b> – " +
                         i.getRide().getStartDate().toLocalDate() + "<br>" +
                         "Passager : " + (i.isPassenger() ? "Oui" : "Non") +
                         " | Vélo : " + (i.isBike() ? "Oui" : "Non") + "</html>";
            JPanel card = new JPanel();
            card.setBorder(BorderFactory.createTitledBorder("Inscription"));
            card.add(new JLabel(txt));
            p.add(card);
        }
        return p;
    }
}