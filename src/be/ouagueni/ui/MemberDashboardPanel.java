package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.List;

public class MemberDashboardPanel extends JPanel {

    private final ClubFrame parentFrame;
    private final Connection conn;
    private final Member currentMember;
    private final JLabel lblBalance = new JLabel("");

    public MemberDashboardPanel(ClubFrame parentFrame, Member member, Connection conn) {
        this.parentFrame = parentFrame;
        this.currentMember = member;
        this.conn = conn;
        

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Titre ---
        JLabel lblTitle = new JLabel(
                "Bienvenue " + member.getFirstname() + " " + member.getName() + " (Membre)",
                JLabel.CENTER
        );
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(20, 80, 130));

        // --- Boutons en haut ---
        JPanel topButtons = new JPanel(new GridLayout(3, 2, 10, 10));
        topButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnDisponibilite = new JButton("🗓️ Poster mes disponibilités");
        JButton btnReserver = new JButton("🚴 Réserver une balade");
        JButton btnChoisirCategorie = new JButton("🏷️ Choisir une catégorie");
        JButton btnMesInscriptions = new JButton("📋 Voir mes inscriptions");

        JButton btnPayerCotisation = new JButton("🪙 Payer cotisation");
        JButton btnAjouterFonds = new JButton("➕ Ajouter fonds");

        topButtons.add(btnDisponibilite);
        topButtons.add(btnReserver);
        topButtons.add(btnChoisirCategorie);
        topButtons.add(btnMesInscriptions);
        topButtons.add(btnPayerCotisation);
        topButtons.add(btnAjouterFonds);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(lblTitle);
        northPanel.add(Box.createVerticalStrut(8));
        // balance label under title
        lblBalance.setAlignmentX(Component.CENTER_ALIGNMENT);
        northPanel.add(lblBalance);
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(topButtons);

        add(northPanel, BorderLayout.NORTH);

        // --- Liste des inscriptions (au centre) ---
        JPanel listPanel = createMemberInscriptionsPanel(member);
        JScrollPane scroll = new JScrollPane(listPanel);
        add(scroll, BorderLayout.CENTER);

        // --- Bouton déconnexion ---
        JButton btnDeconnexion = new JButton("🔙 Déconnexion");
        btnDeconnexion.addActionListener(e -> parentFrame.showPanel("login"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnDeconnexion);
        add(bottom, BorderLayout.SOUTH);

        	
        // --- Actions ---
        btnDisponibilite.addActionListener(this::ouvrirDisponibilite);
        btnReserver.addActionListener(this::ouvrirReservation);
        btnChoisirCategorie.addActionListener(e -> JOptionPane.showMessageDialog(this, "Fonctionnalité à venir : Choisir catégorie"));
        btnPayerCotisation.addActionListener(this::handlePayerCotisation);
        btnAjouterFonds.addActionListener(this::handleAjouterFonds);

        // initialize balance display
        refreshBalanceLabel();
    }
    
    
    
    /** Ouvre la fenêtre de gestion des disponibilités */
    private void ouvrirDisponibilite(ActionEvent e) {
    Set<Ride> ridesSet = Ride.allRides(conn);
    if (ridesSet.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Aucune sortie disponible.", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    List<Ride> rideList = new ArrayList<>(ridesSet);

    JDialog dialog = new JDialog(parentFrame, "Poster mes disponibilités", true);
    dialog.setSize(600, 400);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout(10, 10));

    // --- Panel gauche : Liste des rides ---
    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setBorder(BorderFactory.createTitledBorder("Sélectionner une sortie"));

    DefaultListModel<String> listModel = new DefaultListModel<>();
    for (Ride r : rideList) {
        String display = String.format("%s - %s (%s)",
            r.getStartDate().toLocalDate(),
            r.getStartPlace(),
            r.getCalendar().getCategory().getNomCategorie()
        );
        listModel.addElement(display);
    }

    JList<String> rideListUI = new JList<>(listModel);
    rideListUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    rideListUI.setSelectedIndex(0);
    JScrollPane scrollPane = new JScrollPane(rideListUI);
    leftPanel.add(scrollPane, BorderLayout.CENTER);

    // --- Panel droit : Formulaire ---
    JPanel rightPanel = new JPanel(new GridLayout(3, 2, 10, 10));
    rightPanel.setBorder(BorderFactory.createTitledBorder("Places disponibles"));

    rightPanel.add(new JLabel("Places passagers :"));
    JSpinner seatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    rightPanel.add(seatSpinner);

    rightPanel.add(new JLabel("Places vélo :"));
    JSpinner bikeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
    rightPanel.add(bikeSpinner);

    // --- CHARGER VIA LE MODEL (structure identique) ---
    Vehicle memberVehicle = null;
    boolean isNewVehicle = false;

    try {
        memberVehicle = currentMember.getOrCreateVehicle(conn);
        if (memberVehicle.getId() == 0) {
            isNewVehicle = true;
            rightPanel.setBorder(BorderFactory.createTitledBorder(
                "<html><font color='orange'>Créer un nouveau véhicule</font></html>"
            ));
        } else {
            seatSpinner.setValue(memberVehicle.getSeatNumber());
            bikeSpinner.setValue(memberVehicle.getBikeSpotNumber());
            rightPanel.setBorder(BorderFactory.createTitledBorder(
                "<html><font color='green'>Véhicule existant (ID: " + memberVehicle.getId() + ")</font></html>"
            ));
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(dialog, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        dialog.dispose();
        return;
    }

    // --- Panel bas : Boutons ---
    JPanel bottomPanel = new JPanel(new FlowLayout());
    JButton validerBtn = new JButton("Valider");
    JButton annulerBtn = new JButton("Annuler");

    annulerBtn.addActionListener(e2 -> dialog.dispose());

    final Vehicle finalVehicle = memberVehicle;
    final boolean finalIsNew = isNewVehicle;

    validerBtn.addActionListener(e2 -> {
        int selectedIndex = rideListUI.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(dialog, "Veuillez sélectionner une sortie.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Ride rideSelectionnee = rideList.get(selectedIndex);
        int seats = (Integer) seatSpinner.getValue();
        int bikeSpots = (Integer) bikeSpinner.getValue();

        if (seats <= 0 && bikeSpots <= 0) {
            JOptionPane.showMessageDialog(dialog, "Proposez au moins une place.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            finalVehicle.setSeatNumber(seats);
            finalVehicle.setBikeSpotNumber(bikeSpots);
            finalVehicle.addRide(rideSelectionnee);
            currentMember.postAvailability(rideSelectionnee, seats, bikeSpots, conn);

            String msg = finalIsNew ?
                "Nouveau véhicule créé et disponibilités postées !" :
                "Véhicule mis à jour et disponibilités postées !";

            JOptionPane.showMessageDialog(dialog,
                msg + "\n" +
                seats + " places passager, " + bikeSpots + " places vélo",
                "Succès", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog,
                "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    });

    bottomPanel.add(validerBtn);
    bottomPanel.add(annulerBtn);

    // --- Assemblage ---
    JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    mainPanel.add(leftPanel);
    mainPanel.add(rightPanel);

    dialog.add(mainPanel, BorderLayout.CENTER);
    dialog.add(bottomPanel, BorderLayout.SOUTH);

    dialog.setVisible(true);
}

    /** Ouvre la fenêtre de réservation */
    private void ouvrirReservation(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "⚙️ Fonctionnalité à venir : Réserver une balade");
    }

    /*2	* Handle paying the annual fee + additional category fees */
    private void handlePayerCotisation(ActionEvent e) {
        double due = currentMember.calculateBalance();
        double currentBalance = currentMember.getBalance();
        int count = currentMember.getInscriptions() != null ? currentMember.getInscriptions().size() : 0;

        if (due <= 0) {
            JOptionPane.showMessageDialog(this, "Aucune cotisation due.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String detail = String.format(
            "=== COTISATION À PAYER ===\n" +
            "Inscriptions : %d\n" +
            "Base : 20,00 €\n" +
            "Par inscription : 5,00 € × %d = %.2f €\n" +
            "────────────────────\n" +
            "<b>TOTAL DÛ : %.2f €</b>\n\n" +
            "Solde actuel : %.2f €\n" +
            "Montant à payer :",
            count, count, 5.0 * count, due,
            currentBalance
        );

        String input = JOptionPane.showInputDialog(this, detail, String.format("%.2f", due));
        if (input == null || input.trim().isEmpty()) return;

        double toPay;
        try {
            toPay = Double.parseDouble(input.replace(',', '.'));
            if (toPay <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double newBalance = currentBalance - toPay; // PAYER = - dette
        newBalance = Math.round(newBalance * 100.0) / 100.0;
        currentMember.setBalance(newBalance);

        boolean saved = currentMember.update(conn);

        String msg = String.format(
            "Paiement cotisation : -%.2f €\n" +
            "Inscriptions : %d\n" +
            "Ancien solde : %.2f €\n" +
            "Nouveau solde : %.2f €",
            toPay, count,
            currentBalance,
            newBalance
        );

        if (saved) {
            JOptionPane.showMessageDialog(this, msg + "\n\nEnregistré.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg + "\n\nÉCHEC sauvegarde.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }

        refreshBalanceLabel();
    }

    private void handleAjouterFonds(ActionEvent e)
    {
        String input = JOptionPane.showInputDialog(this, "Montant à verser (crédit) :", "Ajouter fonds");
        if (input == null || input.trim().isEmpty()) return;

        double amount;
        try {
            amount = Double.parseDouble(input.replace(',', '.'));
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Montant invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double currentBalance = currentMember.getBalance();
        double newBalance = currentBalance + amount; // VERSER = + crédit
        newBalance = Math.round(newBalance * 100.0) / 100.0;
        currentMember.setBalance(newBalance);

        boolean saved = currentMember.update(conn);

        String msg = String.format(
            "Versement : +%.2f €\n" +
            "Ancien solde : %.2f €\n" +
            "Nouveau solde : %.2f €",
            amount,
            currentBalance,
            newBalance
        );

        if (saved) {
            JOptionPane.showMessageDialog(this, msg + "\n\nEnregistré.", "Succès", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg + "\n\nÉCHEC sauvegarde.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }

        refreshBalanceLabel();
    }

    private void refreshBalanceLabel() 
    {
	    double balance = currentMember.getBalance();
	    String text;
	
	    if (balance > 0) {
	        text = String.format("<html><font color='blue'><b>Crédit : %.2f €</b></font></html>", balance);
	    } else if (balance == 0) {
	        text = "<html><font color='green'><b>À jour</b></font></html>";
	    } else {
	        text = String.format("<html><font color='red'><b>Vous devez %.2f €</b></font></html>", -balance);
	    }
	
	    lblBalance.setText(text);
    }

    /** Attempts to detect and call member payment methods. Returns true if invoked. */
    private boolean tryInvokePaymentOnMember(Member member, double amount) {
        Class<?> cls = member.getClass();
        try {
            // common method names that might charge the member
            String[] names = {"payFees", "payerCotisation", "deductBalance", "charge", "chargeFees", "pay", "debit"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 1);
                if (m != null) {
                    // try with (double) amount
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount);
                            return true;
                        } else if (params[0] == int.class || params[0] == Integer.class) {
                            m.invoke(member, (int) amount);
                            return true;
                        } else if (params[0] == Connection.class) {
                            m.invoke(member, conn);
                            return true;
                        } else if (params[0] == String.class) {
                            m.invoke(member, String.valueOf(amount));
                            return true;
                        }
                    } else if (params.length == 2 && params[1] == Connection.class) {
                        // maybe (double, Connection)
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount, conn);
                            return true;
                        }
                    }
                }
            }

            // fallback: try setBalance/getBalance to subtract
            Method getBal = findMethodIgnoreCase(cls, new String[]{"getBalance", "getSolde"}, 0);
            Method setBal = findMethodIgnoreCase(cls, new String[]{"setBalance", "setSolde"}, 1);
            if (getBal != null && setBal != null) {
                Object cur = getBal.invoke(member);
                double curVal = cur instanceof Number ? ((Number) cur).doubleValue() : Double.parseDouble(String.valueOf(cur));
                double newVal = curVal - amount;
                Class<?> param = setBal.getParameterTypes()[0];
                if (param == double.class || param == Double.class) setBal.invoke(member, newVal);
                else if (param == int.class || param == Integer.class) setBal.invoke(member, (int) newVal);
                else setBal.invoke(member, String.valueOf(newVal));
                return true;
            }
        } catch (Exception ex) {
            // ignore reflection exceptions, fall through to false
        }
        return false;
    }

    /** Attempts to detect and call member add-funds methods. Returns true if invoked. */
    private boolean tryInvokeAddFundsOnMember(Member member, double amount) {
        Class<?> cls = member.getClass();
        try {
            String[] names = {"addFunds", "addBalance", "deposit", "credit", "ajouterFonds", "crediter"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 1);
                if (m != null) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount);
                            return true;
                        } else if (params[0] == int.class || params[0] == Integer.class) {
                            m.invoke(member, (int) amount);
                            return true;
                        } else if (params[0] == String.class) {
                            m.invoke(member, String.valueOf(amount));
                            return true;
                        }
                    } else if (params.length == 2 && params[1] == Connection.class) {
                        if (params[0] == double.class || params[0] == Double.class) {
                            m.invoke(member, amount, conn);
                            return true;
                        }
                    }
                }
            }

            // fallback: use getBalance/setBalance to add
            Method getBal = findMethodIgnoreCase(cls, new String[]{"getBalance", "getSolde"}, 0);
            Method setBal = findMethodIgnoreCase(cls, new String[]{"setBalance", "setSolde"}, 1);
            if (getBal != null && setBal != null) {
                Object cur = getBal.invoke(member);
                double curVal = cur instanceof Number ? ((Number) cur).doubleValue() : Double.parseDouble(String.valueOf(cur));
                double newVal = curVal + amount;
                Class<?> param = setBal.getParameterTypes()[0];
                if (param == double.class || param == Double.class) setBal.invoke(member, newVal);
                else if (param == int.class || param == Integer.class) setBal.invoke(member, (int) newVal);
                else setBal.invoke(member, String.valueOf(newVal));
                return true;
            }
        } catch (Exception ex) {
            // ignore reflection exceptions
        }
        return false;
    }

    /** Tries to find a method by several candidate names (case-insensitive) with given parameter count. */
    private Method findMethodIgnoreCase(Class<?> cls, String[] candidateNames, int paramCount) {
        Method[] methods = cls.getMethods();
        for (Method m : methods) {
            for (String cand : candidateNames) {
                if (m.getName().equalsIgnoreCase(cand) && m.getParameterTypes().length == paramCount) {
                    return m;
                }
            }
        }
        return null;
    }

    /** Get number of categories of a member using reflection; default to 1 if unknown */
    private int getMemberCategoriesCount(Member member) {
        Class<?> cls = member.getClass();
        try {
            // common getters that may return a Collection or array
            String[] names = {"getCategories", "getCategorySet", "getCategoriesSet", "getCategory", "getCategoriesList"};
            for (String name : names) {
                Method m = findMethodIgnoreCase(cls, new String[]{name}, 0);
                if (m != null) {
                    Object res = m.invoke(member);
                    if (res instanceof Collection) {
                        return ((Collection<?>) res).size();
                    } else if (res != null && res.getClass().isArray()) {
                        return ((Object[]) res).length;
                    } else if (res instanceof Number) {
                        return ((Number) res).intValue();
                    }
                }
            }
        } catch (Exception ignored) {}
        // fallback: try to read a "getCategory" returning single category -> count = 1
        try {
            Method m = findMethodIgnoreCase(cls, new String[]{"getCategory"}, 0);
            if (m != null) return 1;
        } catch (Exception ignored) {}
        return 1;
    }

    /** Crée le panneau listant toutes les inscriptions du membre */
    private JPanel createMemberInscriptionsPanel(Member member) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Set<Inscription> inscriptions = member.getInscriptions();
        if (inscriptions == null || inscriptions.isEmpty()) {
            listPanel.add(new JLabel("Aucune inscription trouvée."));
            return listPanel;
        }

        for (Inscription ins : inscriptions) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createTitledBorder("Inscription #" + ins.getId()));

            StringBuilder sb = new StringBuilder();
            sb.append("Passager : ").append(ins.isPassenger() ? "Oui" : "Non").append("\n");
            sb.append("Avec vélo : ").append(ins.isBike() ? "Oui" : "Non").append("\n");

            if (ins.getBikeObj() != null) {
                Bike b = ins.getBikeObj();
                sb.append("Vélo : ").append(b.getType())
                  .append(" - ").append(b.getWeight()).append(" kg\n");
            }

            if (ins.getRide() != null) {
                Ride r = ins.getRide();
                sb.append("Trajet ").append(r.getnum())
                  .append(" depuis ").append(r.getStartPlace())
                  .append(" le ").append(r.getStartDate())
                  .append(" - ").append(r.getFee()).append(" €\n");
            }

            if (ins.getRide() != null && ins.getRide().getCalendar() != null) {
                Calendar cal = ins.getRide().getCalendar();
                if (cal.getCategory() != null && cal.getCategory().getNomCategorie() != null) {
                    sb.append("Catégorie : ").append(cal.getCategory().getNomCategorie().name()).append("\n");
                }
            }

            JTextArea txt = new JTextArea(sb.toString());
            txt.setEditable(false);
            txt.setBackground(new Color(245, 245, 245));
            card.add(txt);
            listPanel.add(card);
        }

        return listPanel;
    }
}
