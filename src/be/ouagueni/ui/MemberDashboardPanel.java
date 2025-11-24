package be.ouagueni.ui;

import be.ouagueni.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.List;
import java.util.Set;

public class MemberDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Member currentMember;
    private final AppModel model = AppModel.getInstance();

    private final JLabel lblBalance = new JLabel("", SwingConstants.CENTER);
    private final JPanel topButtons = new JPanel(new GridLayout(3, 2, 10, 10));
    private JButton btnDisponibilite;
    private JButton btnReserver;
    private JTabbedPane tabbedPane;

    public MemberDashboardPanel(ClubFrame parentFrame, Member member, Connection conn) {
        this.parentFrame = parentFrame;
        this.currentMember = member;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buildHeader();
        buildMainContent();
        buildFooter();

        refreshBalanceAndButtons();
        refreshAll(); // Rafraîchit les deux onglets au démarrage
    }

    private void buildHeader() {
        JLabel lblTitle = new JLabel(
                "Bienvenue " + currentMember.getFirstname() + " " + currentMember.getName() + " (Membre)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(new Color(20, 80, 130));

        btnDisponibilite = new JButton("Poster mes disponibilités");
        btnReserver = new JButton("Réserver une balade");
        JButton btnChoisirCategorie = new JButton("Choisir une catégorie");
        JButton btnPayerCotisation = new JButton("Payer cotisation");
        JButton btnAjouterFonds = new JButton("Ajouter fonds");

        topButtons.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        topButtons.add(btnDisponibilite);
        topButtons.add(btnReserver);
        topButtons.add(btnChoisirCategorie);
        topButtons.add(btnPayerCotisation);
        topButtons.add(btnAjouterFonds);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(10));
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 16));
        northPanel.add(lblBalance);
        northPanel.add(Box.createVerticalStrut(15));
        northPanel.add(topButtons);

        add(northPanel, BorderLayout.NORTH);

        btnDisponibilite.addActionListener(this::ouvrirDisponibilite);
        btnReserver.addActionListener(this::ouvrirReservation);
        btnChoisirCategorie.addActionListener(e -> choisirCategorie());
        btnPayerCotisation.addActionListener(e -> payerCotisation());
        btnAjouterFonds.addActionListener(e -> ajouterFonds());
    }

    private void buildMainContent() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Mes inscriptions", createMemberInscriptionsPanel());
        tabbedPane.addTab("Mes vélos", createBikesPanel());
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void buildFooter() {
        JButton btnDeconnexion = new JButton("Déconnexion");
        btnDeconnexion.addActionListener(e -> parentFrame.showPanel("login"));

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnDeconnexion);
        add(south, BorderLayout.SOUTH);
    }

    // ====================== ACTIONS ======================
    private void choisirCategorie() {
        List<Category> disponibles = model.getCategoriesDisponiblesPourMembre(currentMember);
        if (disponibles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vous possédez déjà toutes les catégories disponibles !", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JComboBox<Category> combo = new JComboBox<>(disponibles.toArray(new Category[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category c) setText(c.getNomCategorie().toString());
                return this;
            }
        });

        int choix = JOptionPane.showConfirmDialog(this, combo, "Choisir une nouvelle catégorie", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choix == JOptionPane.OK_OPTION) {
            Category selected = (Category) combo.getSelectedItem();
            boolean success = model.ajouterCategorieAuMembre(currentMember, selected);
            JOptionPane.showMessageDialog(this,
                    success ? "Catégorie ajoutée avec succès !" : "Erreur lors de l'ajout.",
                    success ? "Succès" : "Erreur",
                    success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            refreshAll();
        }
    }

    private void ouvrirDisponibilite(ActionEvent e) {
        new DisponibiliteDialog(parentFrame, currentMember).setVisible(true);
        refreshAll();
    }

    private void ouvrirReservation(ActionEvent e) {
        new ReservationDialog(parentFrame, currentMember).setVisible(true);
        refreshAll();
    }

    private void payerCotisation() {
        double due = model.calculerCotisationDue(currentMember);
        if (due <= currentMember.getBalance()) {
            JOptionPane.showMessageDialog(this, "Votre cotisation est déjà à jour !", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "<html><h3>Cotisation à payer</h3>Montant dû : <b>" + String.format("%.2f €", due) + "</b></html>",
                String.format("%.2f", due));

        if (input == null || input.trim().isEmpty()) return;

        try {
            double montant = Double.parseDouble(input.replace(',', '.'));
            if (montant <= 0) throw new NumberFormatException();

            boolean success = model.payerCotisation(currentMember, montant);
            JOptionPane.showMessageDialog(this,
                    success ? "<html>Paiement enregistré !<br>Nouveau solde : <b>" + String.format("%.2f €", currentMember.getBalance()) + "</b></html>" : "Échec du paiement.",
                    success ? "Succès" : "Erreur",
                    success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            refreshAll();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajouterFonds() {
        String input = JOptionPane.showInputDialog(this, "Montant à créditer :", "Ajouter fonds");
        if (input == null || input.trim().isEmpty()) return;

        try {
            double montant = Double.parseDouble(input.replace(',', '.'));
            if (montant <= 0) throw new NumberFormatException();

            boolean success = model.ajouterFonds(currentMember, montant);
            JOptionPane.showMessageDialog(this,
                    success ? "<html>Crédit ajouté !<br>Nouveau solde : <b>" + String.format("%.2f €", currentMember.getBalance()) + "</b></html>" : "Échec.",
                    success ? "Succès" : "Erreur",
                    success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            refreshAll();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================== RAFRAÎCHISSEMENT ======================
    private void refreshBalanceAndButtons() {
        double balance = currentMember.getBalance();
        String texte = balance > 0
                ? "<html><font color='blue'><b>Crédit : " + String.format("%.2f €", balance) + "</b></font></html>"
                : balance == 0
                ? "<html><font color='green'><b>Solde à zéro</b></font></html>"
                : "<html><font color='red'><b>Dette : " + String.format("%.2f €", -balance) + "</b></font></html>";
        lblBalance.setText(texte);

        boolean peutParticiper = balance >= 0;
        btnDisponibilite.setEnabled(peutParticiper);
        btnReserver.setEnabled(peutParticiper);
    }

    private void refreshAll() {
        tabbedPane.setComponentAt(0, createMemberInscriptionsPanel());
        tabbedPane.setComponentAt(1, createBikesPanel());
        refreshBalanceAndButtons();
        revalidate();
        repaint();
    }

    // ====================== INSCRIPTIONS ======================
    private JPanel createMemberInscriptionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Set<Inscription> inscriptions = currentMember.getInscriptions();
        if (inscriptions == null || inscriptions.isEmpty()) {
            JLabel lbl = new JLabel("Aucune inscription pour le moment.");
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            lbl.setForeground(Color.GRAY);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(Box.createVerticalStrut(50));
            panel.add(lbl);
            return panel;
        }

        for (Inscription ins : inscriptions) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createTitledBorder("Inscription #" + ins.getId() + " — " +
                    (ins.getRide() != null ? ins.getRide().getStartDate().toLocalDate() : "Date inconnue")));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

            StringBuilder sb = new StringBuilder("<html>");
            sb.append("Passager : <b>").append(ins.isPassenger() ? "Oui" : "Non").append("</b><br>");
            sb.append("Avec vélo : <b>").append(ins.isBike() ? "Oui" : "Non").append("</b><br>");
            if (ins.getBikeObj() != null) {
                Bike b = ins.getBikeObj();
                sb.append("Vélo : <b>").append(b.getType()).append(" – ").append(b.getWeight()).append(" kg</b><br>");
            }
            if (ins.getRide() != null) {
                Ride r = ins.getRide();
                sb.append("Sortie : <b>").append(r.getStartPlace()).append(" – ").append(r.getStartDate().toLocalDate()).append("</b><br>");
                sb.append("Forfait : <b>").append(String.format("%.2f €", r.getFee())).append("</b>");
            }
            sb.append("</html>");

            JLabel lbl = new JLabel(sb.toString());
            lbl.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            card.add(lbl);
            panel.add(card);
            panel.add(Box.createVerticalStrut(12));
        }
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    // ====================== VÉLOS ======================
    private JPanel createBikesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("<html><h3>Mes vélos</h3></html>"), BorderLayout.WEST);

        JButton btnAdd = new JButton("Ajouter un vélo");
        btnAdd.addActionListener(e -> ouvrirBikeDialog(null));
        header.add(btnAdd, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<Bike> bikes = currentMember.getBikes();
        if (bikes == null || bikes.isEmpty()) {
            listPanel.add(new JLabel("Aucun vélo enregistré pour le moment."));
        } else {
            for (Bike bike : bikes) {
                listPanel.add(createBikeLine(bike));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBikeLine(Bike bike) {
        JPanel line = new JPanel(new BorderLayout());
        line.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel info = new JLabel("<html><b>" + bike.getType() + "</b> – " +
                bike.getWeight() + " kg – " + bike.getLength() + " cm</html>");

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnModif = new JButton("Modifier");
        JButton btnSupp = new JButton("Supprimer");

        btnModif.addActionListener(e -> ouvrirBikeDialog(bike));
        btnSupp.addActionListener(e -> supprimerVelo(bike));

        boutons.add(btnModif);
        boutons.add(btnSupp);

        line.add(info, BorderLayout.CENTER);
        line.add(boutons, BorderLayout.EAST);
        return line;
    }

    private void ouvrirBikeDialog(Bike bikeToEdit) {
        new BikeDialog(parentFrame, currentMember, bikeToEdit, this::refreshAll).setVisible(true);
    }

    private void supprimerVelo(Bike bike) {
        int rep = JOptionPane.showConfirmDialog(this,
                "Supprimer définitivement ce vélo ?\n" + bike.getType() + " – " + bike.getWeight() + " kg",
                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (rep == JOptionPane.YES_OPTION) {
            boolean ok = model.supprimerVeloDeMembre(currentMember, bike);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Vélo supprimé.");
            } else {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression,Vélo possiblement déjà présent sur le trajet", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
            refreshAll();
        }
    }
}