package be.ouagueni.ui;

import javax.swing.*;

import be.ouagueni.model.Bike;
import be.ouagueni.model.Inscription;
import be.ouagueni.model.Member;
import be.ouagueni.model.Ride;

import java.awt.*;
import java.awt.event.*;
import java.util.Set;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 4592605923775948691L;
	private Member member; 

    public MainFrame(Member member) {
        this.member = member;
        initUI();
    }

    private void initUI() {
        setTitle("🏁 Club Cycliste - Tableau de bord membre");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // 🌟 Panel principal
        JPanel panelMain = new JPanel(new BorderLayout());
        panelMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Haut : infos du membre ---
        JLabel lblTitle = new JLabel("Bienvenue " + member.getFirstname() + " " + member.getName(), JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 70, 130));
        panelMain.add(lblTitle, BorderLayout.NORTH);

        // --- Centre : Liste des inscriptions ---
        JPanel panelInscriptions = new JPanel();
        panelInscriptions.setLayout(new BoxLayout(panelInscriptions, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(panelInscriptions);
        panelMain.add(scroll, BorderLayout.CENTER);

        if (member.getInscriptions() == null || member.getInscriptions().isEmpty()) {
            panelInscriptions.add(new JLabel("Aucune inscription trouvée."));
        } else {
            int i = 1;
            for (Inscription ins : member.getInscriptions()) {
                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                card.setBorder(BorderFactory.createTitledBorder("Inscription #" + ins.getId()));

                StringBuilder sb = new StringBuilder();
                sb.append("Passager : ").append(ins.isPassenger() ? "Oui" : "Non").append("\n");
                sb.append("Avec vélo : ").append(ins.isBike() ? "Oui" : "Non").append("\n");

                if (ins.getBikeObj() != null) {
                    Bike b = ins.getBikeObj();
                    sb.append("🚴 Vélo : ").append(b.getType())
                      .append(" - ").append(b.getWeight()).append(" kg\n");
                }

                if (ins.getRide() != null) {
                    Ride r = ins.getRide();
                    sb.append("🛣️ Trajet ").append(r.getnum())
                      .append(" depuis ").append(r.getStartPlace())
                      .append(" le ").append(r.getStartDate())
                      .append(" - ").append(r.getFee()).append(" €\n");

                    if (r.getCalendar() != null && r.getCalendar().getCategory() != null) {
                        sb.append("Catégorie : ")
                          .append(r.getCalendar().getCategory().getNomCategorie())
                          .append("\n");
                    }
                }

                JTextArea txt = new JTextArea(sb.toString());
                txt.setEditable(false);
                txt.setBackground(new Color(245, 245, 245));
                card.add(txt);

                panelInscriptions.add(card);
                i++;
            }
        }

        // --- Bas : boutons d’action ---
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        JButton btnPost = new JButton("🚗 Poster ses disponibilités de covoiturage");
        JButton btnReserve = new JButton("📅 Réserver une place");
        JButton btnCategorie = new JButton("🏷️ Choisir une ou plusieurs catégories");

        btnPost.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Fonctionnalité 'Poster ses disponibilités' à implémenter."));
        btnReserve.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Fonctionnalité 'Réserver une place' à implémenter."));
        btnCategorie.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Fonctionnalité 'Choisir une catégorie' à implémenter."));

        panelButtons.add(btnPost);
        panelButtons.add(btnReserve);
        panelButtons.add(btnCategorie);

        panelMain.add(panelButtons, BorderLayout.SOUTH);

        add(panelMain);
    }
}
