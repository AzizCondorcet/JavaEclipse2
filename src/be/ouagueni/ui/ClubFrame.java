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
            ClubFrame frame = new ClubFrame();
            frame.setVisible(true);
        });
    }
    
    public ClubFrame() {
        this.conn = AppModel.getInstance().getConnection();

        setTitle("Club Cyclistes");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(new LoginPanel(this, conn), "login");
        mainPanel.add(new RegisterPanel(this, conn), "register"); 

        add(mainPanel);
        showPanel("login");
    }

    public void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    public void setCurrentUser(Person user) {
        this.currentUser = user;
    }

    public Person getCurrentUser() {
        return currentUser;
    }

    /** Crée dynamiquement le dashboard du membre */
    public void showMemberDashboard(Member member) {
        MemberDashboardPanel memberPanel = new MemberDashboardPanel(this, member, conn);
        mainPanel.add(memberPanel, "memberDashboard");
        showPanel("memberDashboard");
    }

    public void showManagerDashboard(Manager manager) {
        ManagerDashboardPanel managerPanel = new ManagerDashboardPanel(this, manager, conn);
        mainPanel.add(managerPanel, "managerDashboard");
        showPanel("managerDashboard");
    }

    public void showTreasurerDashboard(Treasurer treasurer) {
        TreasurerDashboardPanel treasurerPanel = new TreasurerDashboardPanel(this, treasurer, conn);
        mainPanel.add(treasurerPanel, "treasurerDashboard");
        showPanel("treasurerDashboard");
    }
    // Ajoute un panel dans le CardLayout
    public void addPanel(JPanel panel, String name) {
        mainPanel.add(panel, name);
    }

}
