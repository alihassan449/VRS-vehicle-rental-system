package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.controller.RentalManagementController;
import com.rental.model.RentalAgreement;

/** FR17-FR23, FR25: create agreement + handover, record return, show invoice (Rental Staff). */
public class RentalPanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField reservationField = new JTextField(10);
    private final JTextField pickupTimeField = new JTextField(14);
    private final JTextField startMileageField = new JTextField(8);
    private final JTextField pickupFuelField = new JTextField(8);
    private final JTextField existingDamageField = new JTextField(18);

    private final JTextField agreementField = new JTextField(10);
    private final JTextField returnTimeField = new JTextField(14);
    private final JTextField endMileageField = new JTextField(8);
    private final JTextField returnFuelField = new JTextField(8);
    private final JTextField newDamageField = new JTextField(18);
    private final JCheckBox maintenanceBox = new JCheckBox("Vehicle needs maintenance");

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Agreement ID", "Reservation",
            "Customer", "Vehicle", "Picked up", "Returned", "Total Amount", "Paid", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public RentalPanel(RentalManagementController controller, Session session) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel forms = new JPanel(new GridLayout(1, 2, 10, 0));
        forms.add(buildHandoverForm());
        forms.add(buildReturnForm());
        add(forms, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton invoice = new JButton("View Invoice of Selected (FR25)");
        invoice.addActionListener(e -> showInvoice());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        bottom.add(invoice);
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        refresh();
        UiTheme.style(this);
    }

    private JPanel buildHandoverForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Agreement + handover (FR17-FR19)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        FormUtil.addRow(form, gbc, row++, "Confirmed reservation ID:", reservationField);
        FormUtil.addRow(form, gbc, row++, "Pickup date/time (blank = now):", pickupTimeField);
        FormUtil.addRow(form, gbc, row++, "Starting mileage (km):", startMileageField);
        FormUtil.addRow(form, gbc, row++, "Fuel level (0-100 %):", pickupFuelField);
        FormUtil.addRow(form, gbc, row++, "Existing damage:", existingDamageField);

        JButton handover = new JButton("Create Agreement & Record Handover");
        handover.addActionListener(e -> handover());
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(handover, gbc);
        return form;
    }

    private JPanel buildReturnForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Return (FR20-FR23) - format yyyy-MM-dd HH:mm"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        FormUtil.addRow(form, gbc, row++, "Agreement ID:", agreementField);
        FormUtil.addRow(form, gbc, row++, "Return date/time (blank = now):", returnTimeField);
        FormUtil.addRow(form, gbc, row++, "Ending mileage (km):", endMileageField);
        FormUtil.addRow(form, gbc, row++, "Fuel level (0-100 %):", returnFuelField);
        FormUtil.addRow(form, gbc, row++, "New damage (; separated):", newDamageField);

        gbc.gridx = 1;
        gbc.gridy = row++;
        form.add(maintenanceBox, gbc);

        JButton ret = new JButton("Record Return & Calculate Charges");
        ret.addActionListener(e -> recordReturn());
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(ret, gbc);
        return form;
    }

    private void handover() {
        try {
            RentalAgreement a = controller.handoverVehicle(session.getCurrentStaff(), reservationField.getText(),
                    FormUtil.parseDateTimeOrNull(pickupTimeField.getText(), "Pickup date/time"),
                    FormUtil.parseDouble(startMileageField.getText(), "Starting mileage"),
                    FormUtil.parseDouble(pickupFuelField.getText(), "Fuel level"),
                    existingDamageField.getText());
            agreementField.setText(a.getAgreementID());
            refresh();
            FormUtil.showInfo(this, "Agreement " + a.getAgreementID() + " created.\n"
                    + "Handover recorded - vehicle " + a.getVehicle().getRegistrationNumber() + " is now RENTED.");
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void recordReturn() {
        try {
            String invoice = controller.returnVehicle(session.getCurrentStaff(), agreementField.getText(),
                    FormUtil.parseDateTimeOrNull(returnTimeField.getText(), "Return date/time"),
                    FormUtil.parseDouble(endMileageField.getText(), "Ending mileage"),
                    FormUtil.parseDouble(returnFuelField.getText(), "Fuel level"),
                    newDamageField.getText(), maintenanceBox.isSelected());
            refresh();
            FormUtil.showText(this, "Return recorded - invoice", invoice);
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void showInvoice() {
        try {
            int row = table.getSelectedRow();
            if (row < 0) {
                throw new IllegalArgumentException("Select an agreement in the table first");
            }
            FormUtil.showText(this, "Invoice", controller.getInvoice((String) model.getValueAt(row, 0)));
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        for (RentalAgreement a : controller.listAgreements()) {
            model.addRow(new Object[]{a.getAgreementID(), a.getReservation().getReservationID(),
                    a.getCustomer().getName(), a.getVehicle().getRegistrationNumber(), a.getPickupDateTime(),
                    a.getActualReturnDateTime() == null ? "-" : a.getActualReturnDateTime(),
                    a.getTotalAmount(), a.totalPaid(), a.isReturned() ? "Returned" : "Active"});
        }
    }
}
