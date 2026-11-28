package com.rental.ui.rental;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import com.rental.enums.ReservationStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.RentalAgreement;
import com.rental.model.Reservation;
import com.rental.service.RentalService;
import com.rental.service.ReservationService;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.Formats;

/**
 * Multi-page Vehicle Handover module (Rental Staff):
 * <ul>
 *   <li>QUEUE: Browse &amp; select Confirmed reservations (UC-08 step 1)</li>
 *   <li>PROCESS: Dedicated full-page Draft Rental Agreement &amp; Handover Authorization (UC-08 steps 2-5, UC-13)</li>
 * </ul>
 */
public class HandoverPanel extends JPanel implements Refreshable {
    private static final String PAGE_QUEUE = "QUEUE";
    private static final String PAGE_PROCESS = "PROCESS";

    private final ReservationService reservationService;
    private final RentalService rentalService;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);
    private String activePage = PAGE_QUEUE;

    // Queue page
    private final JTextField searchField = new JTextField(16);
    private final JCheckBox confirmedOnlyBox = new JCheckBox("Confirmed reservations only", true);
    private final JLabel queueMessage = UiUtil.messageLabel();
    private final DefaultTableModel model = UiUtil.tableModel("Reservation ID", "Customer", "Vehicle",
            "Pickup", "Return", "Est. Charge", "Status");
    private final JTable table = UiUtil.table(model);
    private final JTextArea details = UiUtil.readOnlyArea(6, 50);
    private List<Reservation> current = new ArrayList<>();

    // Process Handover page
    private final JTextArea draftSummary = UiUtil.readOnlyArea(6, 56);
    private final JTextField pickupField = new JTextField(20);
    private final JTextField mileageField = new JTextField(20);
    private final JComboBox<FuelLevel> fuelBox = new JComboBox<>(FuelLevel.values());
    private final JCheckBox noDamageBox = new JCheckBox("No existing damage on the vehicle");
    private final JTextArea damageArea = new JTextArea(4, 34);
    private final JLabel processMessage = UiUtil.messageLabel();
    private RentalAgreement activeDraft;

    public HandoverPanel(ReservationService reservationService, RentalService rentalService) {
        this.reservationService = reservationService;
        this.rentalService = rentalService;

        setLayout(new BorderLayout());
        cardContainer.add(buildQueuePage(), PAGE_QUEUE);
        cardContainer.add(buildProcessPage(), PAGE_PROCESS);
        add(cardContainer, BorderLayout.CENTER);

        loadQueue();
        UiTheme.style(this);
    }

    private JPanel buildQueuePage() {
        JPanel page = new JPanel(new BorderLayout(0, 12));

        JButton handoverBtn = UiTheme.successButton("Process Vehicle Handover >");
        handoverBtn.addActionListener(e -> startSelectedHandover());
        page.add(UiUtil.pageHeader(
                "Vehicle Handover (UC-08)",
                "Select a Confirmed reservation, verify the customer's driving license (UC-13), and authorize pickup.",
                handoverBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> loadQueue());
        searchField.addActionListener(e -> loadQueue());
        confirmedOnlyBox.addActionListener(e -> loadQueue());
        allBtn.addActionListener(e -> {
            searchField.setText("");
            confirmedOnlyBox.setSelected(false);
            loadQueue();
        });

        JPanel filterInner = new JPanel(new BorderLayout(0, 6));
        filterInner.setOpaque(false);
        filterInner.add(UiUtil.flow(
                new JLabel("Reservation ID / Customer / Vehicle:"), searchField,
                confirmedOnlyBox, searchBtn, allBtn), BorderLayout.NORTH);
        filterInner.add(queueMessage, BorderLayout.SOUTH);
        body.add(UiUtil.card(filterInner, 12), BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetails();
            }
        });

        JButton bottomHandoverBtn = UiTheme.successButton("Process Vehicle Handover >");
        bottomHandoverBtn.addActionListener(e -> startSelectedHandover());
        JPanel detailPanel = new JPanel(new BorderLayout(0, 8));
        detailPanel.setOpaque(false);
        detailPanel.add(UiUtil.titled("Selected Reservation Details", new JScrollPane(details)), BorderLayout.CENTER);
        detailPanel.add(UiUtil.flowRight(bottomHandoverBtn), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UiUtil.card(new JScrollPane(table), 10), detailPanel);
        split.setResizeWeight(0.65);
        split.setBorder(BorderFactory.createEmptyBorder());
        body.add(split, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildProcessPage() {
        JPanel page = new JPanel(new BorderLayout());

        JButton backBtn = UiTheme.secondaryButton("< Back to Handover Queue");
        backBtn.addActionListener(e -> showQueuePage());
        JLabel lvaBadge = UiUtil.badge("LVA License Verified: VALID", new Color(0xDCFCE7), new Color(0x166534));
        page.add(UiUtil.pageHeader(
                "Authorize Vehicle Handover (UC-08)",
                "Record pickup date/time, odometer mileage, fuel level, and vehicle condition.",
                lvaBadge, backBtn), BorderLayout.NORTH);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JPanel summaryCard = UiUtil.titled("Draft Rental Agreement Summary", new JScrollPane(draftSummary));
        summaryCard.setMaximumSize(new Dimension(820, 190));
        summaryCard.setAlignmentX(LEFT_ALIGNMENT);

        damageArea.setLineWrap(true);
        damageArea.setWrapStyleWord(true);
        noDamageBox.addActionListener(e -> damageArea.setEnabled(!noDamageBox.isSelected()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        UiUtil.addFormRow(form, row++, "Pickup Date & Time *", pickupField, "Format yyyy-MM-dd HH:mm");
        UiUtil.addFormRow(form, row++, "Starting Mileage *", mileageField, "Must be >= vehicle's recorded mileage.");
        UiUtil.addFormRow(form, row++, "Fuel Level *", fuelBox, "Fuel gauge level at handover.");
        UiUtil.addRow(form, row * 2, "Existing Damage:", new JScrollPane(damageArea));
        UiUtil.addRow(form, row * 2 + 1, "", noDamageBox);

        JButton authorizeBtn = UiTheme.successButton("Authorize Agreement Handover");
        JButton cancelBtn = UiTheme.secondaryButton("Cancel");
        authorizeBtn.addActionListener(e -> authorize());
        cancelBtn.addActionListener(e -> showQueuePage());

        JPanel formFooter = new JPanel(new BorderLayout(0, 8));
        formFooter.setOpaque(false);
        formFooter.add(processMessage, BorderLayout.NORTH);
        formFooter.add(UiUtil.flowRight(cancelBtn, authorizeBtn), BorderLayout.SOUTH);

        JPanel formInner = new JPanel(new BorderLayout(0, 10));
        formInner.setOpaque(false);
        formInner.add(form, BorderLayout.CENTER);
        formInner.add(formFooter, BorderLayout.SOUTH);

        JPanel formCard = UiUtil.titled("Handover Inspection & Authorization", formInner);
        formCard.setMaximumSize(new Dimension(820, 460));
        formCard.setAlignmentX(LEFT_ALIGNMENT);

        wrapper.add(summaryCard);
        wrapper.add(Box.createVerticalStrut(14));
        wrapper.add(formCard);
        wrapper.add(Box.createVerticalGlue());

        page.add(UiUtil.scrollable(wrapper), BorderLayout.CENTER);
        return page;
    }

    private void showQueuePage() {
        activePage = PAGE_QUEUE;
        activeDraft = null;
        loadQueue();
        cards.show(cardContainer, PAGE_QUEUE);
    }

    private void loadQueue() {
        UiUtil.clear(queueMessage);
        reservationService.expireOverdueReservations();
        current = new ArrayList<>();
        for (Reservation r : reservationService.search(searchField.getText())) {
            if (!confirmedOnlyBox.isSelected() || r.getStatus() == ReservationStatus.CONFIRMED) {
                current.add(r);
            }
        }
        model.setRowCount(0);
        for (Reservation r : current) {
            model.addRow(new Object[]{
                    r.getReservationId(),
                    r.getCustomer().getCustomerId() + " - " + r.getCustomer().getName(),
                    r.getVehicle().getRegistrationNumber(),
                    r.getPickupDate(),
                    r.getReturnDate(),
                    String.format("%.2f", r.getEstimatedCharge()),
                    r.getStatus()
            });
        }
        details.setText("");
        if (current.isEmpty()) {
            UiUtil.showError(queueMessage, "No matching reservations found.");
        }
    }

    private Reservation selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (Reservation r : current) {
            if (r.getReservationId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    private void showDetails() {
        Reservation r = selected();
        if (r == null) {
            return;
        }
        details.setText(String.format(
                "Reservation : %s   [%s]%nCustomer    : %s - %s (license expires %s)%n"
                + "Vehicle     : %s %s (%s), status %s%nPeriod      : %s to %s%nEst. charge : %.2f%n",
                r.getReservationId(), r.getStatus(), r.getCustomer().getCustomerId(), r.getCustomer().getName(),
                r.getCustomer().getDrivingLicense() == null ? "-" : r.getCustomer().getDrivingLicense().getExpiryDate(),
                r.getVehicle().getMake(), r.getVehicle().getModel(), r.getVehicle().getRegistrationNumber(),
                r.getVehicle().getStatus(), r.getPickupDate(), r.getReturnDate(), r.getEstimatedCharge()));
    }

    private void startSelectedHandover() {
        Reservation r = selected();
        if (r == null) {
            UiUtil.warn(this, "No Reservation Selected", "Select a Confirmed reservation first.");
            return;
        }
        RentalAgreement[] draft = new RentalAgreement[1];
        if (!UiUtil.run(this, null, () -> draft[0] = rentalService.startHandover(r))) {
            return;
        }
        activeDraft = draft[0];
        Reservation res = activeDraft.getReservation();
        draftSummary.setText(String.format(
                "DRAFT RENTAL AGREEMENT (from reservation %s)%nCustomer : %s (%s)%n"
                + "Vehicle  : %s %s [%s]%nPeriod   : %s to %s%nEstimated charge : %.2f%n"
                + "Driving license verified with the LVA: VALID%n",
                res.getReservationId(), res.getCustomer().getName(), res.getCustomer().getCustomerId(),
                res.getVehicle().getMake(), res.getVehicle().getModel(), res.getVehicle().getRegistrationNumber(),
                res.getPickupDate(), res.getReturnDate(), res.getEstimatedCharge()));
        pickupField.setText(LocalDateTime.now().format(Formats.DATE_TIME));
        mileageField.setText(String.format("%.0f", res.getVehicle().getCurrentMileage()));
        fuelBox.setSelectedItem(FuelLevel.FULL);
        noDamageBox.setSelected(false);
        damageArea.setEnabled(true);
        damageArea.setText("");
        UiUtil.clear(processMessage);

        activePage = PAGE_PROCESS;
        cards.show(cardContainer, PAGE_PROCESS);
    }

    private void authorize() {
        UiUtil.clear(processMessage);
        if (activeDraft == null) {
            return;
        }
        try {
            LocalDateTime pickup = UiUtil.parseDateTime(pickupField.getText(), "Pickup date and time");
            double mileage = UiUtil.parseNumber(mileageField.getText(), "Starting mileage");
            rentalService.authorizeHandover(
                    activeDraft,
                    pickup,
                    mileage,
                    (FuelLevel) fuelBox.getSelectedItem(),
                    noDamageBox.isSelected(),
                    damageArea.getText());
            UiUtil.info(this, "Handover Authorized",
                    "Rental agreement " + activeDraft.getAgreementId() + " is now Active and saved.\n"
                    + "Vehicle " + activeDraft.getReservation().getVehicle().getRegistrationNumber() + " is now "
                    + activeDraft.getReservation().getVehicle().getStatus() + "; reservation "
                    + activeDraft.getReservation().getReservationId() + " is "
                    + activeDraft.getReservation().getStatus() + ".");
            showQueuePage();
        } catch (ValidationException ex) {
            UiUtil.showError(processMessage, ex.getMessage());
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Handover Rejected", ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        if (PAGE_QUEUE.equals(activePage)) {
            loadQueue();
        }
    }
}
