package com.rental.ui.rental;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.FuelLevel;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Charge;
import com.rental.model.ChargeBreakdown;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.model.ReturnDetails;
import com.rental.model.Vehicle;
import com.rental.service.RentalService;
import com.rental.ui.common.Navigator;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.Formats;

/**
 * Multi-page Vehicle Return module (Rental Staff):
 * <ul>
 *   <li>QUEUE: Search &amp; select Active rental agreements (UC-09 step 1)</li>
 *   <li>PROCESS: Dedicated full-page Return Inspection &amp; UC-16 Charge Calculator</li>
 * </ul>
 */
public class ReturnPanel extends JPanel implements Refreshable {
    private static final String PAGE_QUEUE = "QUEUE";
    private static final String PAGE_PROCESS = "PROCESS";

    private final RentalService rentalService;
    private final Navigator navigator;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);
    private String activePage = PAGE_QUEUE;

    // Queue page
    private final JTextField searchField = new JTextField(16);
    private final JLabel queueMessage = UiUtil.messageLabel();
    private final DefaultTableModel model = UiUtil.tableModel("Agreement ID", "Customer", "Vehicle",
            "Pickup", "Planned Return", "Status");
    private final JTable table = UiUtil.table(model);
    private final JTextArea details = UiUtil.readOnlyArea(6, 50);
    private List<RentalAgreement> current = new ArrayList<>();

    // Process page (UC-09 / UC-16)
    private final JTextArea handoverArea = UiUtil.readOnlyArea(7, 46);
    private final JTextField returnField = new JTextField(18);
    private final JTextField mileageField = new JTextField(18);
    private final JComboBox<FuelLevel> fuelBox = new JComboBox<>(FuelLevel.values());
    private final JCheckBox noDamageBox = new JCheckBox("No new damage observed");
    private final JTextArea damageArea = new JTextArea(3, 30);
    private final JTextArea breakdownArea = UiUtil.readOnlyArea(12, 42);
    private final JButton finalizeBtn = UiTheme.successButton("Finalize Return");
    private final JLabel processMessage = UiUtil.messageLabel();
    private RentalAgreement activeAgreement;

    public ReturnPanel(RentalService rentalService, Navigator navigator) {
        this.rentalService = rentalService;
        this.navigator = navigator;

        setLayout(new BorderLayout());
        cardContainer.add(buildQueuePage(), PAGE_QUEUE);
        cardContainer.add(buildProcessPage(), PAGE_PROCESS);
        add(cardContainer, BorderLayout.CENTER);

        loadQueue();
        UiTheme.style(this);
    }

    private JPanel buildQueuePage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));

        JButton returnCta = UiTheme.successButton("Process Vehicle Return >");
        returnCta.addActionListener(e -> openReturnPage());
        page.add(UiUtil.pageHeader(
                "Vehicle Return (UC-09)",
                "Select an Active rental agreement to inspect return condition and calculate charges (UC-16).",
                returnCta), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All Active");
        searchBtn.addActionListener(e -> loadQueue());
        searchField.addActionListener(e -> loadQueue());
        allBtn.addActionListener(e -> {
            searchField.setText("");
            loadQueue();
        });

        JPanel filterInner = new JPanel(new BorderLayout(0, 6));
        filterInner.setOpaque(false);
        filterInner.add(UiUtil.flow(
                new JLabel("Agreement ID / Customer / Vehicle:"), searchField, searchBtn, allBtn),
                BorderLayout.NORTH);
        filterInner.add(queueMessage, BorderLayout.SOUTH);
        body.add(UiUtil.card(filterInner, 12), BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetails();
            }
        });

        JButton bottomReturnBtn = UiTheme.successButton("Process Vehicle Return >");
        bottomReturnBtn.addActionListener(e -> openReturnPage());
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(UiUtil.titled("Original Handover Values", new JScrollPane(details)), BorderLayout.CENTER);
        bottom.add(UiUtil.flowRight(bottomReturnBtn), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UiUtil.card(new JScrollPane(table), 10), bottom);
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        body.add(split, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildProcessPage() {
        JPanel page = new JPanel(new BorderLayout());

        JButton backBtn = UiTheme.secondaryButton("< Back to Active Rentals");
        backBtn.addActionListener(e -> showQueuePage());
        page.add(UiUtil.pageHeader(
                "Process Vehicle Return & Charges (UC-09 / UC-16)",
                "Enter return readings, calculate itemized charges, and finalize return.",
                backBtn), BorderLayout.NORTH);

        damageArea.setLineWrap(true);
        damageArea.setWrapStyleWord(true);
        noDamageBox.addActionListener(e -> damageArea.setEnabled(!noDamageBox.isSelected()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        UiUtil.addFormRow(form, row++, "Actual Return Date & Time *", returnField, "Format yyyy-MM-dd HH:mm");
        UiUtil.addFormRow(form, row++, "Ending Mileage *", mileageField, "Must be >= starting mileage.");
        UiUtil.addFormRow(form, row++, "Return Fuel Level *", fuelBox, "Fuel gauge level upon return.");
        UiUtil.addRow(form, row * 2, "Newly Observed Damage:", new JScrollPane(damageArea));
        UiUtil.addRow(form, row * 2 + 1, "", noDamageBox);

        JButton calcBtn = UiTheme.primaryButton("Calculate Charges (UC-16)");
        calcBtn.addActionListener(e -> calculateCharges());
        finalizeBtn.setEnabled(false);
        finalizeBtn.addActionListener(e -> finalizeReturn());
        JButton cancelBtn = UiTheme.secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> showQueuePage());

        UiUtil.onChange(this::invalidateCharges, returnField, mileageField, fuelBox, noDamageBox, damageArea);

        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.add(UiUtil.titled("Original Handover Values", new JScrollPane(handoverArea)), BorderLayout.NORTH);
        JPanel formWrap = new JPanel(new BorderLayout(0, 8));
        formWrap.setOpaque(false);
        formWrap.add(form, BorderLayout.CENTER);
        formWrap.add(UiUtil.flowRight(calcBtn), BorderLayout.SOUTH);
        left.add(UiUtil.titled("Return Inspection Details", formWrap), BorderLayout.CENTER);

        JPanel right = UiUtil.titled("Charge Breakdown & Final Payable Amount (UC-16)", new JScrollPane(breakdownArea));

        JPanel splitGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        splitGrid.setOpaque(false);
        splitGrid.add(left);
        splitGrid.add(right);

        JPanel footerInner = new JPanel(new BorderLayout(0, 8));
        footerInner.setOpaque(false);
        footerInner.add(processMessage, BorderLayout.NORTH);
        footerInner.add(UiUtil.flowRight(cancelBtn, finalizeBtn), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        body.add(splitGrid, BorderLayout.CENTER);
        body.add(UiUtil.card(footerInner, 12), BorderLayout.SOUTH);

        page.add(UiUtil.scrollable(body), BorderLayout.CENTER);
        return page;
    }

    private void showQueuePage() {
        activePage = PAGE_QUEUE;
        activeAgreement = null;
        loadQueue();
        cards.show(cardContainer, PAGE_QUEUE);
    }

    private void loadQueue() {
        UiUtil.clear(queueMessage);
        current = rentalService.search(searchField.getText(), true);
        model.setRowCount(0);
        for (RentalAgreement a : current) {
            model.addRow(new Object[]{
                    a.getAgreementId(),
                    a.getReservation().getCustomer().getCustomerId() + " - " + a.getReservation().getCustomer().getName(),
                    a.getReservation().getVehicle().getRegistrationNumber(),
                    Formats.dateTime(a.getPickupDateTime()),
                    a.getReservation().getReturnDate(),
                    a.getStatus()
            });
        }
        details.setText("");
        if (current.isEmpty()) {
            UiUtil.showError(queueMessage, "No active rental agreements found.");
        }
    }

    private RentalAgreement selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (RentalAgreement a : current) {
            if (a.getAgreementId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    private void showDetails() {
        RentalAgreement a = selected();
        if (a != null) {
            details.setText(describeHandover(a));
        }
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

    private void openReturnPage() {
        RentalAgreement a = selected();
        if (a == null) {
            UiUtil.warn(this, "No Agreement Selected", "Search for and select an active rental agreement first.");
            return;
        }
        activeAgreement = a;
        handoverArea.setText(describeHandover(a));
        returnField.setText(LocalDateTime.now().format(Formats.DATE_TIME));
        mileageField.setText(String.format("%.0f", a.getStartMileage()));
        fuelBox.setSelectedItem(a.getStartFuelLevel());
        noDamageBox.setSelected(false);
        damageArea.setEnabled(true);
        damageArea.setText("");
        invalidateCharges();
        UiUtil.clear(processMessage);

        activePage = PAGE_PROCESS;
        cards.show(cardContainer, PAGE_PROCESS);
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

    private void calculateCharges() {
        UiUtil.clear(processMessage);
        if (activeAgreement == null) {
            return;
        }
        try {
            ReturnDetails d = readDetails();
            ChargeBreakdown b = rentalService.previewReturn(activeAgreement, d);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-36s %10.2f%n",
                    "Base rental charge (" + activeAgreement.getReservation().getRentalDays()
                    + " day(s) x " + Formats.money(activeAgreement.getReservation().getVehicle().getDailyRentalRate()) + ")",
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
            UiUtil.showOk(processMessage, "Charges calculated. Review breakdown and click 'Finalize Return'.");
        } catch (ValidationException ex) {
            UiUtil.showError(processMessage, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Return Cannot Be Processed", ex.getMessage());
        }
    }

    private void finalizeReturn() {
        UiUtil.clear(processMessage);
        if (activeAgreement == null) {
            return;
        }
        try {
            ReturnDetails d = readDetails();
            ChargeBreakdown b = rentalService.finalizeReturn(activeAgreement, d);
            Vehicle vehicle = activeAgreement.getReservation().getVehicle();
            UiUtil.info(this, "Return Finalized",
                    "Return of agreement " + activeAgreement.getAgreementId() + " saved.\n"
                    + "Final payable amount: " + Formats.money(b.getFinalAmount()) + "\nVehicle "
                    + vehicle.getRegistrationNumber() + " is now " + vehicle.getStatus()
                    + (d.hasNewDamage() ? " (flagged for maintenance - Fleet Staff continue in Maintenance Records)" : "")
                    + ".\nProceed to 'Payments & Receipts' to record payment.");
            showQueuePage();
            navigator.navigateTo(Navigator.PAYMENTS);
        } catch (ValidationException ex) {
            UiUtil.showError(processMessage, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Return Cannot Be Processed", ex.getMessage());
        }
    }

    private void invalidateCharges() {
        finalizeBtn.setEnabled(false);
        breakdownArea.setText("Click 'Calculate Charges (UC-16)' after entering return details.");
    }

    @Override
    public void refresh() {
        if (PAGE_QUEUE.equals(activePage)) {
            loadQueue();
        }
    }
}
