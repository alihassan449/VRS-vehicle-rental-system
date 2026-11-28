package com.rental.ui.vehicle;

import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import java.awt.*;
import javax.swing.*;

import com.rental.enums.FuelType;
import com.rental.enums.TransmissionType;
import com.rental.enums.VehicleCategory;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Vehicle;
import com.rental.service.VehicleService;

/** UC-04 Register Vehicle — legacy dialog retained for compatibility. */
public class AddVehicleDialog extends JDialog {
    private final VehicleService vehicleService;
    private final JTextField regField = new JTextField(18);
    private final JTextField makeField = new JTextField(18);
    private final JTextField modelField = new JTextField(18);
    private final JTextField yearField = new JTextField(18);
    private final JComboBox<TransmissionType> transmissionBox = new JComboBox<>(TransmissionType.values());
    private final JComboBox<FuelType> fuelBox = new JComboBox<>(FuelType.values());
    private final JTextField mileageField = new JTextField(18);
    private final JTextField rateField = new JTextField(18);
    private final JComboBox<VehicleCategory> categoryBox = new JComboBox<>(VehicleCategory.values());
    private Vehicle registered;

    private AddVehicleDialog(Component parent, VehicleService vehicleService) {
        super(SwingUtilities.getWindowAncestor(parent), "Add Vehicle", ModalityType.APPLICATION_MODAL);
        this.vehicleService = vehicleService;
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        int r = 0;
        UiUtil.addRow(form, r++, "Registration Number:", regField);
        UiUtil.addRow(form, r++, "Make:", makeField);
        UiUtil.addRow(form, r++, "Model:", modelField);
        UiUtil.addRow(form, r++, "Year:", yearField);
        UiUtil.addRow(form, r++, "Transmission Type:", transmissionBox);
        UiUtil.addRow(form, r++, "Fuel Type:", fuelBox);
        UiUtil.addRow(form, r++, "Current Mileage:", mileageField);
        UiUtil.addRow(form, r++, "Daily Rental Rate:", rateField);
        UiUtil.addRow(form, r++, "Vehicle Category:", categoryBox);

        JButton commit = UiTheme.successButton("✓  Commit to Fleet");
        JButton cancel = UiTheme.secondaryButton("✖  Cancel");
        commit.addActionListener(e -> commit());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(commit);
        add(form, BorderLayout.CENTER);
        JPanel south = UiUtil.flowRight(cancel, commit);
        south.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        add(south, BorderLayout.SOUTH);
        UiTheme.style(getContentPane());
        pack();
        setMinimumSize(new Dimension(600, getHeight()));
        setLocationRelativeTo(parent);
    }

    private void commit() {
        try {
            Integer year = UiUtil.parseInt(yearField.getText(), "Year");
            Double mileage = UiUtil.parseNumber(mileageField.getText(), "Current mileage");
            Double rate = UiUtil.parseNumber(rateField.getText(), "Daily rental rate");
            registered = vehicleService.registerVehicle(regField.getText(), makeField.getText(), modelField.getText(),
                    year, (VehicleCategory) categoryBox.getSelectedItem(),
                    (TransmissionType) transmissionBox.getSelectedItem(), (FuelType) fuelBox.getSelectedItem(),
                    mileage, rate);
            UiUtil.info(this, "Vehicle registered", "Vehicle " + registered.getRegistrationNumber()
                    + " was added to the fleet with status " + registered.getStatus() + ".");
            dispose();
        } catch (ValidationException ex) {
            UiUtil.warn(this, "Alert", ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Vehicle rejected", ex.getMessage());
        }
    }

    public static Vehicle showDialog(Component parent, VehicleService vehicleService) {
        AddVehicleDialog d = new AddVehicleDialog(parent, vehicleService);
        d.setVisible(true);
        return d.registered;
    }
}