package com.rental.ui.reservation;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.ReservationStatus;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.model.Reservation;
import com.rental.model.Vehicle;
import com.rental.service.CustomerService;
import com.rental.service.ReservationService;
import com.rental.ui.common.Navigator;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;

public class ReservationPanel extends JPanel implements Refreshable {
    public static final String PAGE_LIST = "LIST";
    public static final String PAGE_CREATE = "CREATE";
    private static final String ALL_STATUSES = "All Statuses";

    private final ReservationService reservationService;
    private final CustomerService customerService;
    private final Navigator navigator;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);
    private String activePage = PAGE_LIST;

    private final JTextField searchField = new JTextField(20);
    private final JComboBox<Object> statusFilter = new JComboBox<>();
    private final DefaultListModel<String> reservationListModel = new DefaultListModel<>();
    private final JList<String> reservationList = new JList<>(reservationListModel);
    private final JTextArea details = UiUtil.readOnlyArea(18, 38);
    private final JButton cancelBtn = UiTheme.dangerButton("Cancel Reservation");
    private final JButton handoverShortcutBtn = UiTheme.primaryButton("Go to Handover");
    private List<Reservation> current = new ArrayList<>();

    private final JTextField customerSearch = new JTextField(18);
    private final JComboBox<Customer> customerBox = new JComboBox<>();
    private final JLabel customerInfo = new JLabel("No customer selected");
    private final JTextField pickupField = new JTextField(14);
    private final JTextField returnField = new JTextField(14);
    private final DefaultTableModel vehicleModel = UiUtil.tableModel("Reg. No.", "Make", "Model", "Category", "Daily Rate");
    private final JTable vehicleTable = UiUtil.table(vehicleModel);
    private final JTextArea review = UiUtil.readOnlyArea(6, 44);
    private final JLabel createMessage = UiUtil.messageLabel();
    private final JButton confirmBtn = UiTheme.primaryButton("Save Reservation");

    private List<Vehicle> available = new ArrayList<>();
    private Customer selectedCustomer;
    private Reservation draft;
    private boolean updatingCustomerBox = false;

    private final JLabel listMessage = UiUtil.messageLabel();

    public ReservationPanel(ReservationService reservationService, CustomerService customerService, Navigator navigator) {
        this.reservationService = reservationService;
        this.customerService = customerService;
        this.navigator = navigator;
        setLayout(new BorderLayout());

        cardContainer.add(buildListPage(), PAGE_LIST);
        cardContainer.add(buildCreatePage(), PAGE_CREATE);
        add(cardContainer, BorderLayout.CENTER);

        loadList("");
        UiTheme.style(this);
    }

    public void showPage(String page) {
        this.activePage = page;
        if (PAGE_LIST.equals(page)) loadList(searchField.getText());
        else if (PAGE_CREATE.equals(page)) prepareCreatePage();
        cards.show(cardContainer, page);
    }

    public void startCreateForCustomer(Customer customer) {
        this.selectedCustomer = customer;
        showPage(PAGE_CREATE);
        if (customer != null) {
            Customer fresh = customerService.findById(customer.getCustomerId());
            setCreateCustomer(fresh != null ? fresh : customer);
            customerSearch.setText(customer.getCustomerId());
        }
    }

    private JPanel buildListPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton createCta = UiTheme.primaryButton("Create New Reservation");
        createCta.addActionListener(e -> navigator.navigateTo(Navigator.RESERVATIONS_CREATE));
        page.add(UiUtil.pageHeader("Reservations", "View, search, and manage bookings.", createCta), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        statusFilter.addItem(ALL_STATUSES);
        for (ReservationStatus st : ReservationStatus.values()) statusFilter.addItem(st);
        statusFilter.addActionListener(e -> loadList(searchField.getText()));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> loadList(searchField.getText()));
        searchField.addActionListener(e -> loadList(searchField.getText()));
        allBtn.addActionListener(e -> { searchField.setText(""); statusFilter.setSelectedIndex(0); loadList(""); });

        JPanel filterInner = new JPanel(new BorderLayout(0, 8));
        filterInner.setOpaque(false);
        filterInner.add(UiUtil.flow(new JLabel("Search:"), searchField, new JLabel("Status:"), statusFilter, searchBtn, allBtn), BorderLayout.NORTH);
        body.add(UiUtil.card(filterInner, 14), BorderLayout.NORTH);

        reservationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reservationList.setFont(UiTheme.LABEL_FONT);
        reservationList.setFixedCellHeight(58);
        reservationList.setBackground(UiTheme.SURFACE);
        reservationList.setSelectionBackground(UiTheme.TABLE_SELECT);
        reservationList.setSelectionForeground(UiTheme.PRIMARY_DARK);
        reservationList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        reservationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean has = reservationList.getSelectedIndex() >= 0;
                cancelBtn.setEnabled(has);
                handoverShortcutBtn.setEnabled(has);
                if (has) showSelectedDetails();
            }
        });

        cancelBtn.setEnabled(false);
        handoverShortcutBtn.setEnabled(false);
        cancelBtn.addActionListener(e -> cancelSelected());
        handoverShortcutBtn.addActionListener(e -> {
            if (selectedReservation() != null) navigator.navigateTo(Navigator.HANDOVER);
        });

        JLabel sidebarTitle = new JLabel("Reservations");
        sidebarTitle.setFont(UiTheme.TITLE_FONT);
        sidebarTitle.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JPanel sidebar = new JPanel(new BorderLayout(0, 8));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new java.awt.Dimension(330, 0));
        sidebar.add(sidebarTitle, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(reservationList), BorderLayout.CENTER);

        details.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        details.setForeground(UiTheme.TEXT_PRIMARY);
        details.setBackground(UiTheme.SURFACE);
        details.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel detailCard = new JPanel(new BorderLayout(0, 12));
        detailCard.setOpaque(false);
        JLabel detailTitle = new JLabel("Reservation Details");
        detailTitle.setFont(UiTheme.TITLE_FONT);
        detailTitle.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        detailCard.add(detailTitle, BorderLayout.NORTH);
        detailCard.add(new JScrollPane(details), BorderLayout.CENTER);

        JPanel statusActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        statusActions.setOpaque(false);
        JButton confirmStatusBtn = UiTheme.successButton("Confirm");
        JButton cancelStatusBtn = UiTheme.dangerButton("Cancel");
        JButton expireStatusBtn = UiTheme.warningButton("Expire");
        confirmStatusBtn.addActionListener(e -> changeReservationStatus(ReservationStatus.CONFIRMED));
        cancelStatusBtn.addActionListener(e -> changeReservationStatus(ReservationStatus.CANCELLED));
        expireStatusBtn.addActionListener(e -> changeReservationStatus(ReservationStatus.EXPIRED));
        statusActions.add(confirmStatusBtn);
        statusActions.add(cancelStatusBtn);
        statusActions.add(expireStatusBtn);
        statusActions.add(handoverShortcutBtn);
        detailCard.add(statusActions, BorderLayout.SOUTH);

        JPanel split = new JPanel(new java.awt.BorderLayout(18, 0));
        split.setOpaque(false);
        split.add(UiUtil.card(sidebar, 10), java.awt.BorderLayout.WEST);
        split.add(UiUtil.card(detailCard, 18), java.awt.BorderLayout.CENTER);
        body.add(UiUtil.card(split, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCreatePage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to List");
        backBtn.addActionListener(e -> navigator.navigateTo(Navigator.RESERVATIONS_LIST));
        page.add(UiUtil.pageHeader("Create Reservation", "Step-by-step booking.", backBtn), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        JButton findBtn = UiTheme.primaryButton("Find Customer");
        JButton listAllCustBtn = UiTheme.secondaryButton("Load All");
        findBtn.addActionListener(e -> findCustomer());
        customerSearch.addActionListener(e -> findCustomer());
        listAllCustBtn.addActionListener(e -> loadAllCustomersInPicker());
        customerBox.addActionListener(e -> { if (!updatingCustomerBox && customerBox.getSelectedItem() instanceof Customer) setCreateCustomer((Customer) customerBox.getSelectedItem()); });

        JPanel custTop = UiUtil.flow(new JLabel("Search:"), customerSearch, findBtn, listAllCustBtn, new JLabel("Matches:"), customerBox);
        customerInfo.setFont(UiTheme.LABEL_FONT);
        customerInfo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel custInner = new JPanel(new BorderLayout(0, 10));
        custInner.setOpaque(false); custInner.add(custTop, BorderLayout.NORTH); custInner.add(customerInfo, BorderLayout.CENTER);
        JPanel step1 = UiUtil.card(UiUtil.titled("1. Select Customer", custInner), 14);
        step1.setAlignmentX(LEFT_ALIGNMENT);

        JButton availBtn = UiTheme.primaryButton("Check Availability");
        JButton todayBtn = UiTheme.secondaryButton("Today +3 Days");
        availBtn.addActionListener(e -> showAvailable());
        todayBtn.addActionListener(e -> { pickupField.setText(LocalDate.now().toString()); returnField.setText(LocalDate.now().plusDays(3).toString()); showAvailable(); });
        UiUtil.onChange(this::invalidateVehicles, pickupField, returnField);
        JPanel step2 = UiUtil.card(UiUtil.titled("2. Rental Period", UiUtil.flow(new JLabel("Pickup:"), pickupField, new JLabel("Return:"), returnField, todayBtn, availBtn)), 14);
        step2.setAlignmentX(LEFT_ALIGNMENT);

        vehicleTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) selectVehicle(); });
        JPanel splitGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        splitGrid.setOpaque(false);
        splitGrid.add(UiUtil.card(UiUtil.titled("3. Available Vehicles", new JScrollPane(vehicleTable)), 14));
        splitGrid.add(UiUtil.card(UiUtil.titled("4. Review Estimate", new JScrollPane(review)), 14));
        splitGrid.setAlignmentX(LEFT_ALIGNMENT);

        confirmBtn.setEnabled(false);
        confirmBtn.addActionListener(e -> confirmReservation());
        JButton cancelCreateBtn = UiTheme.secondaryButton("Cancel");
        cancelCreateBtn.addActionListener(e -> navigator.navigateTo(Navigator.RESERVATIONS_LIST));
        JPanel footerInner = new JPanel(new BorderLayout(0, 10));
        footerInner.setOpaque(false); footerInner.add(createMessage, BorderLayout.NORTH); footerInner.add(UiUtil.flowRight(cancelCreateBtn, confirmBtn), BorderLayout.SOUTH);
        JPanel footerCard = UiUtil.card(footerInner, 14);
        footerCard.setAlignmentX(LEFT_ALIGNMENT);

        content.add(step1); content.add(Box.createVerticalStrut(16));
        content.add(step2); content.add(Box.createVerticalStrut(16));
        content.add(splitGrid); content.add(Box.createVerticalStrut(16));
        content.add(footerCard); content.add(Box.createVerticalGlue());

        page.add(UiUtil.scrollable(content), BorderLayout.CENTER);
        return page;
    }

    private void prepareCreatePage() {
        UiUtil.clear(createMessage);
        if (pickupField.getText().trim().isEmpty()) pickupField.setText(LocalDate.now().toString());
        if (returnField.getText().trim().isEmpty()) returnField.setText(LocalDate.now().plusDays(2).toString());
        loadAllCustomersInPicker();
    }

    private void loadAllCustomersInPicker() {
        List<Customer> all = customerService.findAll();
        updatingCustomerBox = true;
        try {
            customerBox.removeAllItems();
            for (Customer c : all) customerBox.addItem(c);
            if (selectedCustomer != null) customerBox.setSelectedItem(selectedCustomer);
            else if (!all.isEmpty()) setCreateCustomer(all.get(0));
        } finally { updatingCustomerBox = false; }
    }

    private void findCustomer() {
        UiUtil.clear(createMessage);
        String q = customerSearch.getText().trim();
        List<Customer> matches = q.isEmpty() ? customerService.findAll() : customerService.searchCustomers(q);
        updatingCustomerBox = true;
        try {
            customerBox.removeAllItems();
            for (Customer c : matches) customerBox.addItem(c);
            if (matches.isEmpty()) { setCreateCustomer(null); UiUtil.showError(createMessage, "No matches."); return; }
            setCreateCustomer(matches.get(0));
        } finally { updatingCustomerBox = false; }
    }

    private void setCreateCustomer(Customer c) {
        selectedCustomer = c;
        customerInfo.setText(c == null ? "No customer selected" : c.getCustomerId() + " - " + c.getName());
        invalidateDraft();
    }

    private void showAvailable() {
        UiUtil.clear(createMessage);
        invalidateVehicles();
        try {
            LocalDate pickup = UiUtil.parseDate(pickupField.getText(), "Pickup date");
            LocalDate ret = UiUtil.parseDate(returnField.getText(), "Return date");
            available = reservationService.findAvailableVehicles(pickup, ret);
            for (Vehicle v : available) {
                vehicleModel.addRow(new Object[]{ v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getCategory(), String.format("%.2f", v.getDailyRentalRate()) });
            }
            if (available.isEmpty()) UiUtil.showError(createMessage, "No vehicles available.");
        } catch (ValidationException ex) { UiUtil.showError(createMessage, ex.getMessage()); }
    }

    private void selectVehicle() {
        int row = vehicleTable.getSelectedRow();
        if (row < 0) return;
        invalidateDraft();
        UiUtil.clear(createMessage);
        Vehicle v = available.get(vehicleTable.convertRowIndexToModel(row));
        try {
            draft = reservationService.buildDraft(selectedCustomer, v, UiUtil.parseDate(pickupField.getText(), "Pickup date"), UiUtil.parseDate(returnField.getText(), "Return date"));
            review.setText(String.format("Customer: %s\nVehicle: %s\nEst. Charge: %.2f", selectedCustomer.getName(), v.getRegistrationNumber(), draft.getEstimatedCharge()));
            confirmBtn.setEnabled(true);
        } catch (Exception ex) { UiUtil.showError(createMessage, ex.getMessage()); vehicleTable.clearSelection(); }
    }

    private void confirmReservation() {
        try {
            reservationService.createPendingReservation(draft);
            UiUtil.info(this, "Saved", "Reservation saved as Pending for review.");
            invalidateVehicles();
            navigator.navigateTo(Navigator.RESERVATIONS_LIST);
        } catch (Exception ex) { UiUtil.error(this, "Error", ex.getMessage()); }
    }

    private void invalidateDraft() { draft = null; confirmBtn.setEnabled(false); review.setText(""); }
    private void invalidateVehicles() { invalidateDraft(); available = new ArrayList<>(); vehicleModel.setRowCount(0); }

    private void loadList(String keyword) {
        ReservationStatus filter = statusFilter.getSelectedItem() instanceof ReservationStatus ? (ReservationStatus) statusFilter.getSelectedItem() : null;
        current = new ArrayList<>();
        for (Reservation r : reservationService.search(keyword)) { if (filter == null || r.getStatus() == filter) current.add(r); }
        reservationListModel.clear();
        for (Reservation r : current) {
            reservationListModel.addElement("<html><b>" + r.getReservationId() + "</b>  ·  " + r.getStatus()
                    + "<br><font color='#64748B'>" + r.getCustomer().getName()
                    + "  ·  " + r.getVehicle().getRegistrationNumber() + "</font></html>");
        }
        reservationList.clearSelection();
        details.setText("Select a reservation from the sidebar to view full details.");
        cancelBtn.setEnabled(false);
        handoverShortcutBtn.setEnabled(false);
    }

    private Reservation selectedReservation() {
        int index = reservationList.getSelectedIndex();
        if (index < 0 || index >= current.size()) return null;
        return current.get(index);
    }

    private void showSelectedDetails() {
        Reservation r = selectedReservation();
        if (r != null) {
            details.setText(String.format(
                    "RESERVATION DETAILS\n\n"
                    + "Reservation ID:  %s\n"
                    + "Status:          %s\n\n"
                    + "Customer:        %s\n"
                    + "Vehicle:         %s\n"
                    + "Pickup Date:     %s\n"
                    + "Return Date:     %s\n"
                    + "Estimated Charge: %.2f",
                    r.getReservationId(), r.getStatus(), r.getCustomer().getName(),
                    r.getVehicle().getRegistrationNumber(), r.getPickupDate(),
                    r.getReturnDate(), r.getEstimatedCharge()));
        }
    }

    private void cancelSelected() {
        Reservation r = selectedReservation();
        if (r != null && UiUtil.confirm(this, "Cancel", "Cancel reservation?")) {
            UiUtil.run(this, listMessage, () -> reservationService.cancelReservation(r));
            loadList(searchField.getText());
        }
    }

    private void changeReservationStatus(ReservationStatus next) {
        Reservation r = selectedReservation();
        if (r == null) return;
        try {
            reservationService.setReservationStatus(r, next);
            UiUtil.info(this, "Status Updated", "Reservation changed to " + next + ".");
            loadList(searchField.getText());
        } catch (Exception ex) {
            UiUtil.error(this, "Status Change Rejected", ex.getMessage());
        }
    }

    @Override public void refresh() { if (PAGE_LIST.equals(activePage)) loadList(searchField.getText()); }
}