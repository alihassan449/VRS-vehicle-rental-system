package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.controller.RentalManagementController;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;

/** FR11-FR16: create, confirm, cancel reservations and check availability (Rental Staff). */
public class ReservationPanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField customerField = new JTextField(10);
    private final JTextField vehicleField = new JTextField(10);
    private final JTextField pickupField = new JTextField(10);
    private final JTextField returnField = new JTextField(10);
    private final JTextField availFromField = new JTextField(10);
    private final JTextField availToField = new JTextField(10);

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Reservation ID", "Customer",
            "Vehicle", "Pickup", "Return", "Estimated Charge", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public ReservationPanel(RentalManagementController controller, Session session) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        refresh();
        UiTheme.style(this);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                "Create reservation (FR11, FR14) and check availability (FR12) - dates as yyyy-MM-dd"));
        FormUtil.Grid g = new FormUtil.Grid(form, 3);
        g.add("Customer ID:", customerField);
        g.add("Vehicle registration no:", vehicleField);
        g.newRow();
        g.add("Pickup date:", pickupField);
        g.add("Return date:", returnField);
        JButton create = new JButton("Create Reservation");
        create.addActionListener(e -> createReservation());
        g.add(create);
        g.add("Availability from:", availFromField);
        g.add("Availability to:", availToField);
        JButton avail = new JButton("Show Available Vehicles");
        avail.addActionListener(e -> showAvailable());
        g.add(avail);
        return form;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Actions on the selected reservation"));
        JButton confirm = new JButton("Confirm (FR13)");
        confirm.addActionListener(e -> act("confirm"));
        JButton cancel = new JButton("Cancel (FR16)");
        cancel.addActionListener(e -> act("cancel"));
        JButton expire = new JButton("Mark Expired (FR15)");
        expire.addActionListener(e -> act("expire"));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        panel.add(confirm);
        panel.add(cancel);
        panel.add(expire);
        panel.add(refresh);
        return panel;
    }

    private void createReservation() {
        try {
            Reservation r = controller.createReservation(session.getCurrentStaff(), customerField.getText(),
                    vehicleField.getText(), FormUtil.parseDate(pickupField.getText(), "Pickup date"),
                    FormUtil.parseDate(returnField.getText(), "Return date"));
            refresh();
            FormUtil.showInfo(this, "Created reservation " + r.getReservationID()
                    + " (PENDING)\nEstimated charge: " + String.format("%.2f", r.getEstimatedRentalCharge()));
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void showAvailable() {
        try {
            LocalDate from = FormUtil.parseDate(availFromField.getText(), "Availability from");
            LocalDate to = FormUtil.parseDate(availToField.getText(), "Availability to");
            List<Vehicle> vehicles = controller.findAvailableVehicles(from, to);
            StringBuilder sb = new StringBuilder("Vehicles available from " + from + " to " + to + ":\n\n");
            for (Vehicle v : vehicles) {
                sb.append(String.format("%s | %s %s | %s | %.2f/day%n", v.getRegistrationNumber(), v.getMake(),
                        v.getModel(), v.getCategory(), v.getDailyRentalRate()));
            }
            if (vehicles.isEmpty()) {
                sb.append("(no vehicle is available for this period)");
            }
            FormUtil.showText(this, "Available vehicles", sb.toString());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void act(String action) {
        try {
            int row = table.getSelectedRow();
            if (row < 0) {
                throw new IllegalArgumentException("Select a reservation in the table first");
            }
            String id = (String) model.getValueAt(row, 0);
            if ("confirm".equals(action)) {
                controller.confirmReservation(session.getCurrentStaff(), id);
            } else if ("cancel".equals(action)) {
                controller.cancelReservation(session.getCurrentStaff(), id);
            } else {
                controller.expireReservation(session.getCurrentStaff(), id);
            }
            refresh();
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        for (Reservation r : controller.listReservations()) {
            model.addRow(new Object[]{r.getReservationID(), r.getCustomer().getCustomerID(),
                    r.getVehicle().getRegistrationNumber(), r.getPickupDate(), r.getReturnDate(),
                    r.getEstimatedRentalCharge(), r.getStatus()});
        }
    }
}
