package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rental.controller.RentalManagementController;
import com.rental.model.Staff;

/**
 * Main window of a logged-in staff member. Only the tabs that the staff member
 * is allowed to
 * use are created:
 * Rental Staff : Customers, Reservations, Rentals, Payments, Reports
 * Fleet Staff : Vehicles, Maintenance, Reports
 * Admin : all tabs
 * (The domain classes still check the permission on every action - the hidden
 * tabs are only
 * for convenience.)
 */
public class MainFrame extends JFrame {

    public MainFrame(RentalManagementController controller, Staff staff, String storageName,
            Runnable onLogout) {
        Session session = new Session(staff);

        setTitle("Vehicle Rental Management System - " + staff.getName() + " (" + staff.getDisplayRole() + ")");
        setSize(1150, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // more room for the tables
        setLayout(new BorderLayout());

        add(buildHeader(staff, storageName, onLogout), BorderLayout.NORTH);

        add(buildTabs(controller, session, staff), BorderLayout.CENTER);
    }

    /** Creates the tabs this staff member may use (see the class comment). */
    public static JTabbedPane buildTabs(RentalManagementController controller, Session session, Staff staff) {
        final List<Runnable> refreshers = new ArrayList<>();
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        boolean rentals = staff.canHandleRentals();
        boolean fleet = staff.canManageFleet();

        if (rentals) {
            tabs.addTab("Customers", new CustomerPanel(controller, session));
        }
        if (fleet) {
            final VehiclePanel vehicles = new VehiclePanel(controller, session);
            tabs.addTab("Vehicles", vehicles);
            refreshers.add(vehicles::refresh);
        }
        if (rentals) {
            final ReservationPanel reservations = new ReservationPanel(controller, session);
            final RentalPanel rentalPanel = new RentalPanel(controller, session);
            final PaymentPanel payments = new PaymentPanel(controller, session);
            tabs.addTab("Reservations", reservations);
            tabs.addTab("Rentals", rentalPanel);
            tabs.addTab("Payments", payments);
            refreshers.add(reservations::refresh);
            refreshers.add(rentalPanel::refresh);
            refreshers.add(payments::refresh);
        }
        if (fleet) {
            final MaintenancePanel maintenance = new MaintenancePanel(controller, session);
            tabs.addTab("Maintenance", maintenance);
            refreshers.add(maintenance::refresh);
        }
        tabs.addTab("Reports", new ReportPanel(controller));

        // keep every table up to date when the user switches tabs
        tabs.addChangeListener(e -> {
            for (Runnable r : refreshers) {
                r.run();
            }
        });
        addPlaceholders(tabs); // grey hint text (expected format) inside every empty text field
        return tabs;
    }

    // ------------------------------------------------------------------
    // placeholders (hint text)

    /**
     * Puts a grey hint inside every text field (e.g. National ID: 00000-0000000-0),
     * so the staff
     * member sees in which format the data must be typed. The hint is chosen from
     * the label that
     * stands in front of the field, and disappears as soon as something is typed.
     */
    private static void addPlaceholders(Container container) {
        String lastLabel = null;
        for (Component child : container.getComponents()) {
            if (child instanceof JLabel) {
                lastLabel = ((JLabel) child).getText();
            } else if (child instanceof JTextField && !(child instanceof JPasswordField)) {
                String hint = hintFor(lastLabel, borderOf(container));
                if (hint != null) {
                    installPlaceholder((JTextField) child, hint);
                }
                lastLabel = null;
            } else if (child instanceof Container) {
                addPlaceholders((Container) child);
            }
        }
    }

    private static Border borderOf(Container c) {
        return c instanceof javax.swing.JComponent ? ((javax.swing.JComponent) c).getBorder() : null;
    }

    /**
     * Hint text for a field, chosen from its label (or from the box title for the
     * search field).
     */
    static String hintFor(String label, Border containerBorder) {
        String l = label == null ? "" : label.trim().toLowerCase();
        if (l.isEmpty() && containerBorder instanceof TitledBorder) {
            l = String.valueOf(((TitledBorder) containerBorder).getTitle()).toLowerCase();
            if (l.contains("search by customer")) {
                return "Customer ID, National ID or name";
            }
            return null;
        }
        if (l.contains("blank = auto"))
            return "Leave blank (auto-generated)";
        if (l.contains("customer id"))
            return "e.g. CUS-0001";
        if (l.contains("national id"))
            return "00000-0000000-0";
        if (l.contains("contact number"))
            return "0300-0000000";
        if (l.contains("license expiry"))
            return "yyyy-MM-dd (e.g. 2030-12-31)";
        if (l.contains("driving license"))
            return "e.g. LIC-2024-001";
        if (l.contains("address"))
            return "House no, street, area, city";
        if (l.startsWith("name"))
            return "e.g. Ali Raza";
        if (l.contains("registration n") || l.startsWith("reg."))
            return "e.g. LEA-101";
        if (l.startsWith("make"))
            return "e.g. Toyota";
        if (l.startsWith("model"))
            return "e.g. Corolla";
        if (l.startsWith("year"))
            return "e.g. 2022";
        if (l.contains("current mileage"))
            return "e.g. 15000 (km)";
        if (l.contains("daily rental rate"))
            return "e.g. 100.00";
        if (l.contains("date/time"))
            return "yyyy-MM-dd HH:mm (blank = now)";
        if (l.contains("pickup date") || l.contains("return date") || l.startsWith("availability")
                || l.startsWith("from") || l.startsWith("to:")) {
            return "yyyy-MM-dd (e.g. 2026-12-31)";
        }
        if (l.contains("reservation id"))
            return "e.g. RES-0001";
        if (l.contains("agreement id"))
            return "e.g. AGR-0001";
        if (l.contains("starting mileage"))
            return "e.g. 15000 (km)";
        if (l.contains("ending mileage"))
            return "e.g. 15800 (km)";
        if (l.contains("fuel level"))
            return "0 - 100 (%)";
        if (l.contains("existing damage"))
            return "e.g. small scratch (blank = none)";
        if (l.contains("new damage"))
            return "e.g. cracked glass;broken mirror";
        if (l.startsWith("amount"))
            return "e.g. 300.00";
        if (l.contains("maintenance type"))
            return "e.g. Oil change";
        if (l.startsWith("description"))
            return "e.g. Engine oil replaced";
        if (l.startsWith("cost"))
            return "e.g. 85.00";
        return null;
    }

    /**
     * Shows the hint as a grey italic label inside the field while the field is
     * empty.
     */
    private static void installPlaceholder(final JTextField field, String hint) {
        final JLabel label = new JLabel(hint) {
            @Override
            public boolean contains(int x, int y) {
                return false; // clicks go to the text field below the hint
            }
        };
        label.setFont(field.getFont().deriveFont(Font.ITALIC));
        label.setForeground(new Color(0x9AA0A6));
        // the BorderLayout keeps the hint inside the field's own insets, so it lines up
        // with typed text
        field.setLayout(new BorderLayout());
        field.add(label, BorderLayout.CENTER);
        field.setToolTipText(hint);

        // a text field with "columns" ignores setPreferredSize, so widen it through its
        // column count
        // until the whole hint fits
        int columnWidth = field.getFontMetrics(field.getFont()).charWidth('m');
        int hintWidth = label.getPreferredSize().width + 8;
        int columnsNeeded = (hintWidth + columnWidth - 1) / columnWidth;
        if (columnsNeeded > field.getColumns()) {
            field.setColumns(columnsNeeded);
        }

        final Runnable update = () -> label.setVisible(field.getText().isEmpty());
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update.run();
            }
        });
        update.run();
    }

    private JPanel buildHeader(Staff staff, String storageName, final Runnable onLogout) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UiTheme.PRIMARY_DARK);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Vehicle Rental Management System");
        title.setFont(UiTheme.HEADER_FONT);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Storage: " + storageName);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xCFE0F5));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(title);
        texts.add(subtitle);
        banner.add(texts, BorderLayout.WEST);

        JPanel user = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        user.setOpaque(false);
        JLabel who = new JLabel("Logged in: " + staff.getName() + " (" + staff.getDisplayRole() + ")");
        who.setForeground(Color.WHITE);
        who.setFont(UiTheme.TITLE_FONT);
        JButton logout = new JButton("Logout");
        logout.setFont(UiTheme.BUTTON_FONT);
        logout.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        user.add(who);
        user.add(logout);
        banner.add(user, BorderLayout.EAST);
        return banner;
    }
}