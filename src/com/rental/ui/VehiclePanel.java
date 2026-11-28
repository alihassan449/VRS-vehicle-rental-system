package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

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
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.model.Vehicle;

/** FR6-FR9: register vehicles (fleet staff), search by registration number / category / status. */
public class VehiclePanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField regField = new JTextField(10);
    private final JTextField makeField = new JTextField(10);
    private final JTextField modelField = new JTextField(10);
    private final JTextField yearField = new JTextField(6);
    private final JComboBox<String> categoryBox = new JComboBox<>(FormUtil.names(VehicleCategory.values()));
    private final JComboBox<String> transmissionBox = new JComboBox<>(new String[]{"Manual", "Automatic"});
    private final JComboBox<String> fuelBox = new JComboBox<>(new String[]{"Petrol", "Diesel", "Hybrid", "Electric"});
    private final JTextField mileageField = new JTextField(8);
    private final JTextField rateField = new JTextField(8);

    private final JTextField searchRegField = new JTextField(10);
    private final JComboBox<String> searchCategoryBox = new JComboBox<>(FormUtil.withAny(VehicleCategory.values()));
    private final JComboBox<String> searchStatusBox = new JComboBox<>(FormUtil.withAny(VehicleStatus.values()));

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Reg. No.", "Make", "Model", "Year",
            "Category", "Transmission", "Fuel", "Mileage", "Daily Rate", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public VehiclePanel(RentalManagementController controller, Session session) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // register form on top (compact, 3 columns); search bar directly above the table;
        // the table gets all the remaining space, so the vehicles are always visible
        add(buildForm(), BorderLayout.NORTH);
        JPanel listArea = new JPanel(new BorderLayout(4, 4));
        listArea.add(buildBottom(), BorderLayout.NORTH);
        listArea.add(new JScrollPane(table), BorderLayout.CENTER);
        add(listArea, BorderLayout.CENTER);

        showVehicles(controller.searchVehicles(null, null, null));
        UiTheme.style(this);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Register vehicle (FR6, FR7) - Fleet Staff only"));
        FormUtil.Grid g = new FormUtil.Grid(form, 3);
        g.add("Registration number:", regField);
        g.add("Make:", makeField);
        g.add("Model:", modelField);
        g.add("Year:", yearField);
        g.add("Category:", categoryBox);
        g.add("Transmission type:", transmissionBox);
        g.add("Fuel type:", fuelBox);
        g.add("Current mileage (km):", mileageField);
        g.add("Daily rental rate:", rateField);
        JButton register = new JButton("Register Vehicle");
        register.addActionListener(e -> registerVehicle());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(register);
        g.addButtons(buttonRow);
        return form;
    }

    private JPanel buildBottom() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Search vehicles (FR9) and manage status (FR8)"));
        JButton search = new JButton("Search");
        search.addActionListener(e -> search());
        JButton all = new JButton("Show All");
        all.addActionListener(e -> showVehicles(controller.searchVehicles(null, null, null)));
        JButton inactive = new JButton("Mark Selected INACTIVE");
        inactive.addActionListener(e -> setInactive(true));
        JButton reactivate = new JButton("Reactivate Selected");
        reactivate.addActionListener(e -> setInactive(false));

        panel.add(new JLabel("Reg. No.:"));
        panel.add(searchRegField);
        panel.add(new JLabel("Category:"));
        panel.add(searchCategoryBox);
        panel.add(new JLabel("Status:"));
        panel.add(searchStatusBox);
        panel.add(search);
        panel.add(all);
        panel.add(inactive);
        panel.add(reactivate);
        return panel;
    }

    private void registerVehicle() {
        try {
            Vehicle v = controller.registerVehicle(session.getCurrentStaff(), regField.getText(),
                    makeField.getText(), modelField.getText(), FormUtil.parseInt(yearField.getText(), "Year"),
                    VehicleCategory.valueOf((String) categoryBox.getSelectedItem()),
                    (String) transmissionBox.getSelectedItem(), (String) fuelBox.getSelectedItem(),
                    FormUtil.parseDouble(mileageField.getText(), "Current mileage"),
                    FormUtil.parseDouble(rateField.getText(), "Daily rental rate"));
            for (JTextField f : new JTextField[]{regField, makeField, modelField, yearField, mileageField, rateField}) {
                f.setText("");
            }
            showVehicles(controller.searchVehicles(null, null, null));
            FormUtil.showInfo(this, "Registered vehicle " + v.getRegistrationNumber());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void search() {
        try {
            String cat = (String) searchCategoryBox.getSelectedItem();
            String st = (String) searchStatusBox.getSelectedItem();
            showVehicles(controller.searchVehicles(searchRegField.getText(),
                    FormUtil.ANY.equals(cat) ? null : VehicleCategory.valueOf(cat),
                    FormUtil.ANY.equals(st) ? null : VehicleStatus.valueOf(st)));
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void setInactive(boolean inactive) {
        try {
            int row = table.getSelectedRow();
            if (row < 0) {
                throw new IllegalArgumentException("Select a vehicle in the table first");
            }
            controller.setVehicleInactive(session.getCurrentStaff(), (String) model.getValueAt(row, 0), inactive);
            showVehicles(controller.searchVehicles(null, null, null));
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void showVehicles(List<Vehicle> vehicles) {
        model.setRowCount(0);
        for (Vehicle v : vehicles) {
            model.addRow(new Object[]{v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getYear(),
                    v.getCategory(), v.getTransmissionType(), v.getFuelType(), v.getCurrentMileage(),
                    v.getDailyRentalRate(), v.getStatus()});
        }
    }

    /** Reload the table (called when the tab is shown). */
    public void refresh() {
        showVehicles(controller.searchVehicles(null, null, null));
    }
}
