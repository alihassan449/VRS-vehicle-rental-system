package com.rental.ui.rental;

import com.rental.ui.common.UiTheme;
import com.rental.model.Vehicle;
import com.rental.model.Reservation;
import com.rental.ui.common.UiUtil;
import java.awt.*;
import java.time.LocalDateTime;
import javax.swing.*;

import com.rental.enums.FuelLevel;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Charge;
import com.rental.model.ChargeBreakdown;
import com.rental.model.RentalAgreement;
import com.rental.model.ReturnDetails;
import com.rental.service.RentalService;
import com.rental.util.Formats;

/**
 * UC-09 steps 2 - 4: enter the return details, see the charge breakdown and final
 * payable amount (include UC-16), then 'Finalize Return'.
 */
public class ReturnDialog extends JDialog {
    private final RentalService rentalService;
    private final RentalAgreement agreement;
    private final JTextField returnField = new JTextField(16);
    private final JTextField mileageField = new JTextField(16);
    private final JComboBox<FuelLevel> fuelBox = new JComboBox<>(FuelLevel.values());
    private final JCheckBox noDamageBox = new JCheckBox("No new damage observed");
    private final JTextArea damageArea = new JTextArea(3, 30);
    private final JTextArea breakdownArea = UiUtil.readOnlyArea(8, 40);
    private final JButton finalizeBtn = new JButton("Finalize Return");
    private final JLabel message = UiUtil.messageLabel();
    private boolean finalized;

    private ReturnDialog(Component parent, RentalService rentalService, RentalAgreement agreement) {
        super(SwingUtilities.getWindowAncestor(parent), "Process Vehicle Return", ModalityType.APPLICATION_MODAL);
        this.rentalService = rentalService;
        this.agreement = agreement;

        JTextArea handover = UiUtil.readOnlyArea(6, 46);
        handover.setText(describeHandover(agreement));

        returnField.setText(LocalDateTime.now().format(Formats.DATE_TIME));
        mileageField.setText(String.format("%.0f", agreement.getStartMileage()));
        fuelBox.setSelectedItem(agreement.getStartFuelLevel());
        damageArea.setLineWrap(true);
        damageArea.setWrapStyleWord(true);
        noDamageBox.addActionListener(e -> damageArea.setEnabled(!noDamageBox.isSelected()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        int row = 0;
        UiUtil.addRow(form, row++, "Actual return date and time (yyyy-MM-dd HH:mm):", returnField);
        UiUtil.addRow(form, row++, "Ending mileage:", mileageField);
        UiUtil.addRow(form, row++, "Return fuel level:", fuelBox);
        UiUtil.addRow(form, row++, "Newly observed damage:", new JScrollPane(damageArea));
        UiUtil.addRow(form, row++, "", noDamageBox);

        JButton calcBtn = new JButton("Calculate Charges");
        calcBtn.addActionListener(e -> calculate());
        finalizeBtn.setEnabled(false);
        finalizeBtn.addActionListener(e -> finalizeReturn());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        // any change to the entered values invalidates the displayed charges
        UiUtil.onChange(this::invalidateCharges, returnField, mileageField, fuelBox, noDamageBox, damageArea);

        JPanel left = new JPanel(new BorderLayout());
        left.add(UiUtil.titled("Original handover values", new JScrollPane(handover)), BorderLayout.NORTH);
        left.add(UiUtil.titled("Return details", form), BorderLayout.CENTER);
        left.add(UiUtil.flow(calcBtn), BorderLayout.SOUTH);
        JPanel right = UiUtil.titled("Charge breakdown and final payable amount", new JScrollPane(breakdownArea));

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        south.add(message, BorderLayout.NORTH);
        south.add(UiUtil.flow(finalizeBtn, cancel), BorderLayout.SOUTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 0));
        center.add(left);
        center.add(right);
        add(center, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        UiTheme.style(getContentPane());
        setSize(960, 520);
        setLocationRelativeTo(parent);
    }

    static String describeHandover(RentalAgreement a) {
        Reservation r = a.getReservation();
        return String.format("Agreement  : %s   [%s]%nCustomer   : %s (%s)%nVehicle    : %s %s [%s]%nPickup     : %s"
                        + "%nStart mileage: %.0f   Fuel: %s%nExisting damage: %s%nPlanned return: %s",
                a.getAgreementId(), a.getStatus(), r.getCustomer().getName(), r.getCustomer().getCustomerId(),
                r.getVehicle().getMake(), r.getVehicle().getModel(), r.getVehicle().getRegistrationNumber(),
                Formats.dateTime(a.getPickupDateTime()), a.getStartMileage(), a.getStartFuelLevel(),
                a.getExistingDamageNotes().isEmpty() ? "none" : a.getExistingDamageNotes(), r.getReturnDate());
    }

    private ReturnDetails readDetails() {
        LocalDateTime when = UiUtil.parseDateTime(returnField.getText(), "Return date and time");
        double mileage = UiUtil.parseNumber(mileageField.getText(), "Ending mileage");
        String damage = noDamageBox.isSelected() ? "" : damageArea.getText().trim();
        if (!noDamageBox.isSelected() && damage.isEmpty()) {
            throw new ValidationException("Record the newly observed damage, or tick 'No new damage observed'.");
        }
        return new ReturnDetails(when, mileage, (FuelLevel) fuelBox.getSelectedItem(), damage);
    }

    /** UC-09 steps 3.1 - 3.3: validate, calculate (UC-16) and display the breakdown. */
    private void calculate() {
        UiUtil.clear(message);
        try {
            ReturnDetails d = readDetails();
            ChargeBreakdown b = rentalService.previewReturn(agreement, d);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-36s %10.2f%n", "Base rental charge (" + agreement.getReservation().getRentalDays()
                    + " day(s) x " + Formats.money(agreement.getReservation().getVehicle().getDailyRentalRate()) + ")",
                    b.getBaseCharge()));
            for (Charge c : b.getAdditionalCharges()) {
                sb.append(String.format("%-36s %10.2f%n", c.getDescription(), c.getAmount()));
            }
            sb.append("------------------------------------------------\n");
            sb.append(String.format("%-36s %10.2f%n", "FINAL PAYABLE AMOUNT", b.getFinalAmount()));
            if (d.hasNewDamage()) {
                sb.append("\nNew damage recorded: the vehicle will be flagged for maintenance.\n");
            }
            breakdownArea.setText(sb.toString());
            finalizeBtn.setEnabled(true);
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Return cannot be processed", ex.getMessage());
        }
    }

    /** UC-09 steps 4.1 - 4.3. */
    private void finalizeReturn() {
        UiUtil.clear(message);
        try {
            ReturnDetails d = readDetails();
            ChargeBreakdown b = rentalService.finalizeReturn(agreement, d);
            Vehicle vehicle = agreement.getReservation().getVehicle();
            finalized = true;
            UiUtil.info(this, "Return finalized", "Return of agreement " + agreement.getAgreementId() + " saved.\n"
                    + "Final payable amount: " + Formats.money(b.getFinalAmount()) + "\nVehicle "
                    + vehicle.getRegistrationNumber() + " is now " + vehicle.getStatus()
                    + (d.hasNewDamage() ? " (flagged for maintenance - Fleet Staff continue in Maintenance Records)" : "")
                    + ".\nRecord the payment under 'Payments & Receipts'.");
            dispose();
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Return cannot be processed", ex.getMessage());
        }
    }

    private void invalidateCharges() {
        finalizeBtn.setEnabled(false);
        breakdownArea.setText("");
    }

    public static boolean showDialog(Component parent, RentalService rentalService, RentalAgreement agreement) {
        ReturnDialog d = new ReturnDialog(parent, rentalService, agreement);
        d.setVisible(true);
        return d.finalized;
    }
}
