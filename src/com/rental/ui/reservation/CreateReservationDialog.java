package com.rental.ui.reservation;

import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.rental.exception.BusinessRuleException;
import com.rental.exception.SchedulingConflictException;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.service.CustomerService;
import com.rental.service.ReservationService;

/**
 * UC-06 Create Reservation — legacy dialog retained for compatibility.
 * The full-page wizard in ReservationPanel is the primary UI.
 */
public class CreateReservationDialog extends JDialog {
    private final ReservationService reservationService;
    private final CustomerService customerService;

    private final JTextField customerSearch = new JTextField(16);
    private final JComboBox<Customer> customerBox = new JComboBox<>();
    private final JLabel customerInfo = new JLabel("No customer selected");
    private final JTextField pickupField = new JTextField(12);
    private final JTextField returnField = new JTextField(12);
    private final DefaultTableModel vehicleModel = UiUtil.tableModel("Reg. No.", "Make", "Model", "Category", "Daily Rate");
    private final JTable vehicleTable = UiUtil.table(vehicleModel);
    private final JTextArea review = UiUtil.readOnlyArea(7, 40);
    private final JLabel message = UiUtil.messageLabel();
    private final JButton confirmBtn = UiTheme.successButton("✓  Confirm Reservation");

    private List<Vehicle> available = new ArrayList<>();
    private Customer customer;
    private Reservation draft;
    private Reservation created;

    private CreateReservationDialog(Component parent, ReservationService rs, CustomerService cs) {
        super(SwingUtilities.getWindowAncestor(parent), "Create Reservation", ModalityType.APPLICATION_MODAL);
        this.reservationService = rs;
        this.customerService = cs;

        JButton findBtn = UiTheme.primaryButton("🔍  Find Customer");
        findBtn.addActionListener(e -> findCustomer());
        customerSearch.addActionListener(e -> findCustomer());
        customerBox.setVisible(false);
        customerBox.addActionListener(e -> {
            if (customerBox.getSelectedItem() != null) {
                setCustomer((Customer) customerBox.getSelectedItem());
            }
        });
        JPanel customerPanel = new JPanel(new BorderLayout(0, 8));
        customerPanel.setOpaque(false);
        customerPanel.add(UiUtil.flow(new JLabel("Customer ID / National ID / Name:"), customerSearch, findBtn, customerBox),
                BorderLayout.NORTH);
        customerPanel.add(customerInfo, BorderLayout.CENTER);

        JButton availBtn = UiTheme.primaryButton("🔍  Show Available Vehicles");
        availBtn.addActionListener(e -> showAvailable());
        pickupField.setToolTipText("yyyy-MM-dd");
        returnField.setToolTipText("yyyy-MM-dd");
        UiUtil.onChange(this::invalidateVehicles, pickupField, returnField);
        JPanel period = UiUtil.flow(new JLabel("Pickup date (yyyy-MM-dd):"), pickupField,
                new JLabel("Return date (yyyy-MM-dd):"), returnField, availBtn);

        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectVehicle();
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UiUtil.titled("①  Customer", customerPanel), BorderLayout.NORTH);
        top.add(UiUtil.titled("②  Rental Period", period), BorderLayout.CENTER);

        JPanel middle = new JPanel(new GridLayout(1, 2, 12, 0));
        middle.setOpaque(false);
        JScrollPane vehicleScroll = new JScrollPane(vehicleTable);
        vehicleScroll.setPreferredSize(new Dimension(460, 200));
        middle.add(UiUtil.titled("③  Available Vehicles — Select One", vehicleScroll));
        middle.add(UiUtil.titled("④  Review Reservation Details", new JScrollPane(review)));

        confirmBtn.setEnabled(false);
        confirmBtn.addActionListener(e -> confirm());
        JButton cancel = UiTheme.secondaryButton("✖  Close");
        cancel.addActionListener(e -> dispose());
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));
        south.add(message, BorderLayout.NORTH);
        south.add(UiUtil.flowRight(cancel, confirmBtn), BorderLayout.SOUTH);

        setLayout(new BorderLayout(8, 8));
        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        UiTheme.style(getContentPane());
        setSize(1000, 620);
        setMinimumSize(new Dimension(940, 580));
        setLocationRelativeTo(parent);
    }

    private void findCustomer() {
        UiUtil.clear(message);
        List<Customer> matches = customerService.searchCustomers(customerSearch.getText());
        customerBox.setVisible(false);
        if (matches.isEmpty()) {
            setCustomer(null);
            UiUtil.showError(message, "No records found: no customer matches '" + customerSearch.getText().trim() + "'.");
            return;
        }
        if (matches.size() > 1) {
            customerBox.removeAllItems();
            for (Customer c : matches) {
                customerBox.addItem(c);
            }
            customerBox.setVisible(true);
            setCustomer(matches.get(0));
        } else {
            setCustomer(matches.get(0));
        }
        revalidate();
    }

    private void setCustomer(Customer c) {
        customer = c;
        customerInfo.setText(c == null ? "No customer selected"
                : "<html>" + c.getCustomerId() + " - " + c.getName() + " | National ID " + c.getNationalId()
                + " | Contact " + c.getContactNumber() + "</html>");
        invalidateDraft();
    }

    private void showAvailable() {
        UiUtil.clear(message);
        invalidateVehicles();
        try {
            LocalDate pickup = UiUtil.parseDate(pickupField.getText(), "Pickup date");
            LocalDate ret = UiUtil.parseDate(returnField.getText(), "Return date");
            available = reservationService.findAvailableVehicles(pickup, ret);
            for (Vehicle v : available) {
                vehicleModel.addRow(new Object[]{v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getCategory(),
                        String.format("%.2f", v.getDailyRentalRate())});
            }
            if (available.isEmpty()) {
                UiUtil.showError(message, "No vehicle is available for this period. Change the dates or close the form.");
            }
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        }
    }

    private void selectVehicle() {
        int row = vehicleTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        invalidateDraft();
        UiUtil.clear(message);
        Vehicle v = available.get(vehicleTable.convertRowIndexToModel(row));
        try {
            LocalDate pickup = UiUtil.parseDate(pickupField.getText(), "Pickup date");
            LocalDate ret = UiUtil.parseDate(returnField.getText(), "Return date");
            draft = reservationService.buildDraft(customer, v, pickup, ret);
            review.setText(String.format("Customer         : %s (%s)%nVehicle          : %s %s [%s]%nCategory         : %s"
                            + "%nPickup date      : %s%nReturn date      : %s%nRental days      : %d%nDaily rate       : %.2f"
                            + "%nESTIMATED CHARGE : %.2f%n",
                    customer.getName(), customer.getCustomerId(), v.getMake(), v.getModel(), v.getRegistrationNumber(),
                    v.getCategory(), pickup, ret, draft.getRentalDays(), v.getDailyRentalRate(), draft.getEstimatedCharge()));
            confirmBtn.setEnabled(true);
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.showError(message, ex.getMessage());
            vehicleTable.clearSelection();
        }
    }

    private void confirm() {
        try {
            created = reservationService.confirmReservation(draft);
            UiUtil.info(this, "Reservation confirmed", "Reservation " + created.getReservationId() + " was confirmed.\n"
                    + "Vehicle " + created.getVehicle().getRegistrationNumber() + " is now "
                    + created.getVehicle().getStatus() + ".\nEstimated charge: "
                    + String.format("%.2f", created.getEstimatedCharge()));
            dispose();
        } catch (SchedulingConflictException ex) {
            UiUtil.error(this, "Scheduling Conflict", ex.getMessage());
            showAvailable();
        } catch (BusinessRuleException | IllegalStateException ex) {
            UiUtil.error(this, "Reservation not saved", ex.getMessage());
            showAvailable();
        }
    }

    private void invalidateDraft() {
        draft = null;
        confirmBtn.setEnabled(false);
        review.setText("");
    }

    private void invalidateVehicles() {
        invalidateDraft();
        available = new ArrayList<>();
        vehicleModel.setRowCount(0);
    }

    public static Reservation showDialog(Component parent, ReservationService rs, CustomerService cs) {
        CreateReservationDialog d = new CreateReservationDialog(parent, rs, cs);
        d.setVisible(true);
        return d.created;
    }
}