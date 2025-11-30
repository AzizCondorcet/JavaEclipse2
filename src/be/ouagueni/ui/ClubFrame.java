package be.ouagueni.ui;

import be.ouagueni.model.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
public class ClubFrame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final Connection conn;
    private Person currentUser;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            ClubFrame frame = new ClubFrame();
            frame.setVisible(true);
        });
    }

    public ClubFrame() {
        this.conn = AppModel.getInstance().getConnection();

        setTitle("Club Cyclistes – Gestion & Covoiturage");
        setSize(1100, 750);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new WelcomePanel(this, conn), "login");
        mainPanel.add(new RegisterPanel(this, conn), "register");

        add(mainPanel);

        showPanel("login");
    }
    // ==================================================================
    // Méthodes utilitaires
    // ==================================================================

    public void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void setCurrentUser(Person user) {
        this.currentUser = user;
    }

    public Person getCurrentUser() {
        return currentUser;
    }

    public Connection getConnection() {
        return conn;
    }

    public void showMemberDashboard(Member member) {
        MemberDashboardPanel panel = new MemberDashboardPanel(this, member, conn);
        mainPanel.add(panel, "memberDashboard");
        showPanel("memberDashboard");
    }

    public void showManagerDashboard(Manager manager) {
    		ManagerDashboardPanel panel = new ManagerDashboardPanel(this, manager);
        mainPanel.add(panel, "managerDashboard");
        showPanel("managerDashboard");
    }

    public void showTreasurerDashboard(Treasurer treasurer) {
        TreasurerDashboardPanel panel = new TreasurerDashboardPanel(this, treasurer, conn);
        mainPanel.add(panel, "treasurerDashboard");
        showPanel("treasurerDashboard");
    }

    public void addPanel(JPanel panel, String name) {
        mainPanel.add(panel, name);
        showPanel(name);
    }

    /** Permet de récupérer le ClubFrame depuis n’importe quel composant Swing */
    public static ClubFrame getInstance(Component component) {
        return (ClubFrame) SwingUtilities.getWindowAncestor(component);
    }
}