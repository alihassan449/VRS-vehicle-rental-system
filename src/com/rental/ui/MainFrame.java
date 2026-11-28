package com.rental.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import com.rental.AppContext;
import com.rental.model.*;
import com.rental.ui.common.*;
import com.rental.ui.customer.CustomerPanel;
import com.rental.ui.maintenance.MaintenancePanel;
import com.rental.ui.payment.PaymentPanel;
import com.rental.ui.rental.HandoverPanel;
import com.rental.ui.rental.ReturnPanel;
import com.rental.ui.report.ReportPanel;
import com.rental.ui.reservation.ReservationPanel;
import com.rental.ui.vehicle.VehiclePanel;
import com.rental.ui.admin.StaffManagementPanel;

public class MainFrame extends JFrame implements Navigator {
    private final AppContext ctx;
    private final Staff user;
    private final CardLayout workspaceLayout = new CardLayout();
    private final JPanel workspace = new JPanel(workspaceLayout);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, Component> cardComponents = new LinkedHashMap<>();

    private DashboardPanel dashboardPanel;
    private CustomerPanel customerPanel;
    private ReservationPanel reservationPanel;
    private HandoverPanel handoverPanel;
    private ReturnPanel returnPanel;
    private PaymentPanel paymentPanel;
    private VehiclePanel vehiclePanel;
    private MaintenancePanel maintenancePanel;
    private ReportPanel reportPanel;
    private StaffManagementPanel staffManagementPanel;
    private final JLabel useCaseBanner = new JLabel("SDA Use Case: Dashboard");

    public MainFrame(AppContext ctx, Staff user, Runnable onLogout) {
        this.ctx = ctx; this.user = user;
        setTitle("Vehicle Rental Management System"); setSize(1366, 820); setMinimumSize(new Dimension(1100, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null);

        buildWorkspace();
        JLabel who = new JLabel("Role: " + user.getRole());
        who.setFont(UiTheme.LABEL_BOLD); who.setForeground(Color.WHITE);
        
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UiTheme.BANNER_GREEN); banner.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));
        JLabel title = new JLabel("Vehicle Rental Management System"); title.setFont(new Font("Segoe UI", Font.BOLD, 22)); title.setForeground(Color.WHITE);
        useCaseBanner.setFont(UiTheme.LABEL_BOLD); useCaseBanner.setForeground(new Color(0xDCFCE7));
        JPanel bannerRight = new JPanel(); bannerRight.setOpaque(false); bannerRight.setLayout(new BoxLayout(bannerRight, BoxLayout.Y_AXIS));
        who.setAlignmentX(Component.RIGHT_ALIGNMENT); useCaseBanner.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bannerRight.add(who); bannerRight.add(Box.createVerticalStrut(4)); bannerRight.add(useCaseBanner);
        banner.add(title, BorderLayout.WEST); banner.add(bannerRight, BorderLayout.EAST);

        JPanel navBar = buildNavBar(onLogout);
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(banner, BorderLayout.NORTH); topContainer.add(navBar, BorderLayout.SOUTH);

        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(topContainer, BorderLayout.NORTH); add(workspace, BorderLayout.CENTER);
        navigateTo(Navigator.DASHBOARD);
    }

    private void buildWorkspace() {
        dashboardPanel = new DashboardPanel(ctx, user, this); addWorkspaceCard("dashboard", dashboardPanel);
        switch (user.getRole()) {
            case RENTAL_STAFF:
                customerPanel = new CustomerPanel(ctx.customerService, ctx.rentalService, ctx.lva, this);
                reservationPanel = new ReservationPanel(ctx.reservationService, ctx.customerService, this);
                handoverPanel = new HandoverPanel(ctx.reservationService, ctx.rentalService);
                returnPanel = new ReturnPanel(ctx.rentalService, this);
                paymentPanel = new PaymentPanel(ctx.paymentService, ctx.rentalService);
                reportPanel = new ReportPanel(ctx.reportService);
                addWorkspaceCard("customers", customerPanel); addWorkspaceCard("reservations", reservationPanel);
                addWorkspaceCard("handover", handoverPanel); addWorkspaceCard("return", returnPanel); addWorkspaceCard("payments", paymentPanel); addWorkspaceCard("reports", reportPanel);
                break;
            case FLEET_STAFF:
                vehiclePanel = new VehiclePanel(ctx.vehicleService, this);
                maintenancePanel = new MaintenancePanel(ctx.maintenanceService, ctx.vehicleService);
                addWorkspaceCard("fleet", vehiclePanel); addWorkspaceCard("maintenance", maintenancePanel);
                break;
            case ADMIN:
                staffManagementPanel = new StaffManagementPanel(ctx.authService);
                reportPanel = new ReportPanel(ctx.reportService);
                addWorkspaceCard("staff", staffManagementPanel);
                addWorkspaceCard("reports", reportPanel);
                break;
            default:
                reportPanel = new ReportPanel(ctx.reportService);
                addWorkspaceCard("reports", reportPanel);
                break;
        }
        useCaseBanner.setFont(UiTheme.LABEL_BOLD);
        useCaseBanner.setForeground(UiTheme.TEXT_MUTED);
        useCaseBanner.setBorder(BorderFactory.createEmptyBorder(0, 28, 16, 28));
        add(useCaseBanner, BorderLayout.SOUTH);
    }

    private void addWorkspaceCard(String key, Component comp) { cardComponents.put(key, comp); workspace.add(comp, key); }

    private JPanel buildNavBar(Runnable onLogout) {
        JPanel navBar = new JPanel(); navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS));
        navBar.setBackground(UiTheme.SURFACE);
        navBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER), BorderFactory.createEmptyBorder(0, 20, 0, 20)));

        addNavLink(navBar, Navigator.DASHBOARD, "Dashboard");
        if (user.getRole() == com.rental.enums.StaffRole.RENTAL_STAFF) {
            addNavLink(navBar, Navigator.CUSTOMERS_DIRECTORY, "Customers"); addNavLink(navBar, Navigator.RESERVATIONS_LIST, "Reservations");
            addNavLink(navBar, Navigator.HANDOVER, "Handover"); addNavLink(navBar, Navigator.RETURN, "Returns"); addNavLink(navBar, Navigator.PAYMENTS, "Payments"); addNavLink(navBar, Navigator.REPORTS, "Reports");
        } else if (user.getRole() == com.rental.enums.StaffRole.FLEET_STAFF) {
            addNavLink(navBar, Navigator.FLEET_DIRECTORY, "Fleet"); addNavLink(navBar, Navigator.MAINTENANCE, "Maintenance");
        } else if (user.getRole() == com.rental.enums.StaffRole.ADMIN) {
            addNavLink(navBar, Navigator.STAFF_MANAGEMENT, "Staff Management");
            addNavLink(navBar, Navigator.REPORTS, "Reports");
        } else {
            addNavLink(navBar, Navigator.REPORTS, "Reports");
        }
        navBar.add(Box.createHorizontalGlue());
        JButton logoutBtn = UiTheme.secondaryButton("Log Out"); logoutBtn.setForeground(UiTheme.DANGER);
        logoutBtn.addActionListener(e -> { dispose(); onLogout.run(); });
        JPanel wrap = new JPanel(); wrap.setOpaque(false); wrap.add(logoutBtn); navBar.add(wrap);
        return navBar;
    }

    private void addNavLink(JPanel parent, String route, String label) {
        NavButton btn = new NavButton(label); btn.addActionListener(e -> navigateTo(route)); navButtons.put(route, btn); parent.add(btn);
    }

    @Override public void navigateTo(String route) {
        String cardKey = route; String navKey = route;
        switch (route) {
            case Navigator.DASHBOARD: cardKey = "dashboard"; break;
            case Navigator.CUSTOMERS_DIRECTORY: cardKey = "customers"; customerPanel.showPage(CustomerPanel.PAGE_LIST); break;
            case Navigator.CUSTOMERS_CREATE: cardKey = "customers"; customerPanel.showPage(CustomerPanel.PAGE_FORM); navKey = Navigator.CUSTOMERS_DIRECTORY; break;
            case Navigator.CUSTOMERS_EDIT: cardKey = "customers"; customerPanel.showPage(CustomerPanel.PAGE_FORM); navKey = Navigator.CUSTOMERS_DIRECTORY; break;
            case Navigator.CUSTOMERS_PROFILE: cardKey = "customers"; customerPanel.showPage(CustomerPanel.PAGE_PROFILE); navKey = Navigator.CUSTOMERS_DIRECTORY; break;
            case Navigator.RESERVATIONS_LIST: cardKey = "reservations"; reservationPanel.showPage(ReservationPanel.PAGE_LIST); break;
            case Navigator.RESERVATIONS_CREATE: cardKey = "reservations"; reservationPanel.showPage(ReservationPanel.PAGE_CREATE); navKey = Navigator.RESERVATIONS_LIST; break;
            case Navigator.HANDOVER: cardKey = "handover"; break;
            case Navigator.RETURN: cardKey = "return"; break;
            case Navigator.PAYMENTS: cardKey = "payments"; break;
            case Navigator.FLEET_DIRECTORY: cardKey = "fleet"; vehiclePanel.showPage(VehiclePanel.PAGE_DIRECTORY); break;
            case Navigator.FLEET_ADD: cardKey = "fleet"; vehiclePanel.showPage(VehiclePanel.PAGE_ADD); navKey = Navigator.FLEET_DIRECTORY; break;
            case Navigator.MAINTENANCE: cardKey = "maintenance"; break;
            case Navigator.STAFF_MANAGEMENT:
                cardKey = "staff";
                if (staffManagementPanel != null) staffManagementPanel.refresh();
                break;
            case Navigator.REPORTS: cardKey = "reports"; break;
        }
        setUseCaseLabel(route);
        workspaceLayout.show(workspace, cardKey);
        Component comp = cardComponents.get(cardKey);
        if (comp instanceof Refreshable) ((Refreshable) comp).refresh();
        for (Map.Entry<String, NavButton> entry : navButtons.entrySet()) entry.getValue().setActive(entry.getKey().equals(navKey));
    }

    @Override public void navigateToCreateReservation(Customer c) { workspaceLayout.show(workspace, "reservations"); reservationPanel.startCreateForCustomer(c); for (NavButton b : navButtons.values()) b.setActive(false); navButtons.get(Navigator.RESERVATIONS_LIST).setActive(true); }
    @Override public void navigateToMaintenance(Vehicle v) { workspaceLayout.show(workspace, "maintenance"); maintenancePanel.preselectVehicle(v); for (NavButton b : navButtons.values()) b.setActive(false); navButtons.get(Navigator.MAINTENANCE).setActive(true); }

    private void setUseCaseLabel(String route) {
        String label;
        switch (route) {
            case Navigator.CUSTOMERS_DIRECTORY: label = "SDA: UC-02 Search Customer"; break;
            case Navigator.CUSTOMERS_CREATE: label = "SDA: UC-01 Register Customer · UC-13 Verify License"; break;
            case Navigator.CUSTOMERS_EDIT: label = "SDA: UC-03 Update Customer Info"; break;
            case Navigator.CUSTOMERS_PROFILE: label = "SDA: UC-02 Search Customer · Profile"; break;
            case Navigator.RESERVATIONS_LIST: label = "SDA: UC-06 / UC-07 Reservations"; break;
            case Navigator.RESERVATIONS_CREATE: label = "SDA: UC-06 · UC-14 · UC-15"; break;
            case Navigator.HANDOVER: label = "SDA: UC-08 Handover · UC-13 Verify License"; break;
            case Navigator.RETURN: label = "SDA: UC-09 Return · UC-16 Calculate Charges"; break;
            case Navigator.PAYMENTS: label = "SDA: UC-10 Record Payment & Print Receipt"; break;
            case Navigator.FLEET_DIRECTORY: label = "SDA: UC-05 Search Vehicle"; break;
            case Navigator.FLEET_ADD: label = "SDA: UC-04 Register Vehicle"; break;
            case Navigator.MAINTENANCE: label = "SDA: UC-11 Manage Maintenance"; break;
            case Navigator.STAFF_MANAGEMENT: label = "SDA: Staff Account Administration"; break;
            case Navigator.REPORTS: label = "SDA: UC-12 System Reports · UC-12.1–UC-12.4"; break;
            default: label = "SDA Use Case: Dashboard"; break;
        }
        useCaseBanner.setText(label);
    }

    private static class NavButton extends JButton {
        NavButton(String text) {
            super(text); setFont(UiTheme.LABEL_BOLD); setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
            setPreferredSize(new Dimension(getPreferredSize().width + 40, 52));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { putClientProperty("hover", true); repaint(); }
                @Override public void mouseExited(MouseEvent e) { putClientProperty("hover", false); repaint(); }
            });
        }
        void setActive(boolean active) { putClientProperty("active", active); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean active = Boolean.TRUE.equals(getClientProperty("active"));
            boolean hovered = Boolean.TRUE.equals(getClientProperty("hover"));
            if (active) { g2.setColor(UiTheme.BACKGROUND); g2.fillRect(0, 0, getWidth(), getHeight()); g2.setColor(UiTheme.PRIMARY); g2.fillRoundRect(10, getHeight() - 4, getWidth() - 20, 3, 3, 3); }
            else if (hovered) { g2.setColor(UiTheme.SIDEBAR_HOVER); g2.fillRect(0, 0, getWidth(), getHeight()); }
            g2.dispose(); setForeground(active ? UiTheme.PRIMARY : UiTheme.TEXT_MUTED); super.paintComponent(g);
        }
    }
}