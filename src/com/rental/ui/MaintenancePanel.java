package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.controller.RentalManagementController;
import com.rental.model.MaintenanceRecord;

/** FR10, FR26, FR27: log and update vehicle maintenance (Fleet Staff). */
public class MaintenancePanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField vehicleField = new JTextField(10);
    private final JTextField typeField = new JTextField(14);
    private final JTextField descriptionField = new JTextField(22);
    private final JTextField costField = new JTextField(8);
    private final JComboBox<String> statusBox = new JComboBox<>(MaintenanceRecord.STATUSES);
    private final JComboBox<String> newStatusBox = new JComboBox<>(MaintenanceRecord.STATUSES);

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Record ID", "Vehicle", "Type",
            "Date", "Description", "Cost", "Completion Status", "Logged By"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public MaintenancePanel(RentalManagementController controller, Session session) {
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
                "Create maintenance record (FR26) - open maintenance blocks reservation/rental (FR27)"));
        FormUtil.Grid g = new FormUtil.Grid(form, 3);
        g.add("Vehicle registration no:", vehicleField);
        g.add("Maintenance type:", typeField);
        g.add("Description:", descriptionField);
        g.add("Cost:", costField);
        g.add("Completion status:", statusBox);
        JButton create = new JButton("Create Maintenance Record");
        create.addActionListener(e -> createRecord());
        g.add(create);
        return form;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Update the selected record"));
        JButton update = new JButton("Set Status");
        update.addActionListener(e -> updateStatus());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        panel.add(new JLabel("New status:"));
        panel.add(newStatusBox);
        panel.add(update);
        panel.add(refresh);
        return panel;
    }

    private void createRecord() {
        try {
            MaintenanceRecord r = controller.logMaintenance(session.getCurrentStaff(), vehicleField.getText(),
                    typeField.getText(), descriptionField.getText(),
                    FormUtil.parseDouble(costField.getText(), "Cost"), (String) statusBox.getSelectedItem());
            refresh();
            FormUtil.showInfo(this, "Created maintenance record " + r.getRecordID());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void updateStatus() {
        try {
            int row = table.getSelectedRow();
            if (row < 0) {
                throw new IllegalArgumentException("Select a maintenance record in the table first");
            }
            controller.updateMaintenanceStatus(session.getCurrentStaff(), (String) model.getValueAt(row, 0),
                    (String) newStatusBox.getSelectedItem());
            refresh();
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        for (MaintenanceRecord r : controller.listMaintenanceRecords()) {
            model.addRow(new Object[]{r.getRecordID(), r.getVehicle().getRegistrationNumber(),
                    r.getMaintenanceType(), r.getDate(), r.getDescription(), r.getCost(),
                    r.getCompletionStatus(), r.getLoggedBy() == null ? "-" : r.getLoggedBy().getName()});
        }
    }
}
