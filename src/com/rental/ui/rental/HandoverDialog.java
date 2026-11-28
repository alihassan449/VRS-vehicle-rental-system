package com.rental.ui.rental;

import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import java.awt.*;
import java.time.LocalDateTime;
import javax.swing.*;

import com.rental.enums.FuelLevel;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.service.RentalService;
import com.rental.util.Formats;

/**
 * UC-08 steps 3 - 5: record the pickup date/time, starting mileage, fuel level and any
 * existing damage on the draft rental agreement, then 'Authorize Agreement Handover'.
 */
public class HandoverDialog extends JDialog {
    private final RentalService rentalService;
    private final RentalAgreement draft;
    private final JTextField pickupField = new JTextField(16);
    private final JTextField mileageField = new JTextField(16);
    private final JComboBox<FuelLevel> fuelBox = new JComboBox<>(FuelLevel.values());
    private final JCheckBox noDamageBox = new JCheckBox("No existing damage on the vehicle");
    private final JTextArea damageArea = new JTextArea(4, 30);
    private final JLabel message = UiUtil.messageLabel();
    private boolean authorized;

    private HandoverDialog(Component parent, RentalService rentalService, RentalAgreement draft) {
        super(SwingUtilities.getWindowAncestor(parent), "Process Vehicle Handover", ModalityType.APPLICATION_MODAL);
        this.rentalService = rentalService;
        this.draft = draft;
        Reservation r = draft.getReservation();

        JTextArea summary = UiUtil.readOnlyArea(7, 46);
        summary.setText(String.format("DRAFT RENTAL AGREEMENT (from reservation %s)%nCustomer : %s (%s)%nVehicle  : %s %s [%s]"
                        + "%nPeriod   : %s to %s%nEstimated charge : %.2f%nDriving license verified with the LVA: valid%n",
                r.getReservationId(), r.getCustomer().getName(), r.getCustomer().getCustomerId(),
                r.getVehicle().getMake(), r.getVehicle().getModel(), r.getVehicle().getRegistrationNumber(),
                r.getPickupDate(), r.getReturnDate(), r.getEstimatedCharge()));

        pickupField.setText(LocalDateTime.now().format(Formats.DATE_TIME));
        mileageField.setText(String.format("%.0f", r.getVehicle().getCurrentMileage()));
        damageArea.setLineWrap(true);
        damageArea.setWrapStyleWord(true);
        noDamageBox.addActionListener(e -> damageArea.setEnabled(!noDamageBox.isSelected()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        int row = 0;
        UiUtil.addRow(form, row++, "Pickup date and time (yyyy-MM-dd HH:mm):", pickupField);
        UiUtil.addRow(form, row++, "Starting mileage:", mileageField);
        UiUtil.addRow(form, row++, "Fuel level:", fuelBox);
        UiUtil.addRow(form, row++, "Existing damage:", new JScrollPane(damageArea));
        UiUtil.addRow(form, row++, "", noDamageBox);

        JButton authorize = new JButton("Authorize Agreement Handover");
        JButton cancel = new JButton("Cancel");
        authorize.addActionListener(e -> authorize());
        cancel.addActionListener(e -> dispose());

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        south.add(message, BorderLayout.NORTH);
        south.add(UiUtil.flow(authorize, cancel), BorderLayout.SOUTH);
        add(new JScrollPane(summary), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        UiTheme.style(getContentPane());
        pack();
        setLocationRelativeTo(parent);
    }

    private void authorize() {
        UiUtil.clear(message);
        try {
            LocalDateTime pickup = UiUtil.parseDateTime(pickupField.getText(), "Pickup date and time");
            double mileage = UiUtil.parseNumber(mileageField.getText(), "Starting mileage");
            rentalService.authorizeHandover(draft, pickup, mileage, (FuelLevel) fuelBox.getSelectedItem(),
                    noDamageBox.isSelected(), damageArea.getText());
            authorized = true;
            UiUtil.info(this, "Handover authorized", "Rental agreement " + draft.getAgreementId() + " is Active.\n"
                    + "Vehicle " + draft.getReservation().getVehicle().getRegistrationNumber() + " is now "
                    + draft.getReservation().getVehicle().getStatus() + "; reservation "
                    + draft.getReservation().getReservationId() + " is " + draft.getReservation().getStatus() + ".");
            dispose();
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Handover rejected", ex.getMessage());
        }
    }

    /** Returns true if the handover was authorized. */
    public static boolean showDialog(Component parent, RentalService rentalService, RentalAgreement draft) {
        HandoverDialog d = new HandoverDialog(parent, rentalService, draft);
        d.setVisible(true);
        return d.authorized;
    }
}
