package com.rental.ui.vehicle;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.FuelType;
import com.rental.enums.TransmissionType;
import com.rental.enums.VehicleCategory;
import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;
import com.rental.service.VehicleService;
import com.rental.ui.common.Navigator;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;

public class VehiclePanel extends JPanel implements Refreshable {
    public static final String PAGE_DIRECTORY = "DIRECTORY";
    public static final String PAGE_ADD = "ADD";
    public static final String PAGE_DETAILS = "DETAILS";
    private static final String ANY = "Any";

    private final VehicleService vehicleService;
    private final Navigator navigator;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);
    private String activePage = PAGE_DIRECTORY;

    // Directory
    private final JTextField regField = new JTextField(14);
    private final JComboBox<Object> categoryBox = new JComboBox<>();
    private final JComboBox<Object> statusBox = new JComboBox<>();
    private final JLabel message = UiUtil.messageLabel();
    private final DefaultTableModel model = UiUtil.tableModel("Reg. No.", "Make", "Model", "Year", "Category", "Status");
    private final JTable table = UiUtil.table(model);
    private final JButton detailsBtn = UiTheme.primaryButton("View Details");
    private final JButton maintBtn = UiTheme.secondaryButton("Open Maintenance");

    // Add Vehicle
    private final UiUtil.PlaceholderTextField addRegField = new UiUtil.PlaceholderTextField(20, "e.g. LED-4004");
    private final UiUtil.PlaceholderTextField addMakeField = new UiUtil.PlaceholderTextField(20, "e.g. Toyota");
    private final UiUtil.PlaceholderTextField addModelField = new UiUtil.PlaceholderTextField(20, "e.g. Corolla");
    private final UiUtil.PlaceholderTextField addYearField = new UiUtil.PlaceholderTextField(20, "e.g. 2026");
    private final JComboBox<TransmissionType> addTransBox = new JComboBox<>(TransmissionType.values());
    private final JComboBox<FuelType> addFuelBox = new JComboBox<>(FuelType.values());
    private final JTextField addMileageField = new JTextField(20);
    private final JTextField addRateField = new JTextField(20);
    private final JComboBox<VehicleCategory> addCatBox = new JComboBox<>(VehicleCategory.values());
    private final JLabel addMessage = UiUtil.messageLabel();

    // Details
    private final JLabel dRegVal = new JLabel("-");
    private final JLabel dMakeModelVal = new JLabel("-");
    private final JLabel dCatVal = new JLabel("-");
    private final JLabel dPowertrainVal = new JLabel("-");
    private final JLabel dMileageVal = new JLabel("-");
    private final JLabel dRateVal = new JLabel("-");
    private final JLabel dStatusVal = new JLabel("-");
    private final DefaultTableModel maintHistoryModel = UiUtil.tableModel("Record ID", "Date", "Type", "Cost", "Status", "Description");

    private List<Vehicle> current = new ArrayList<>();
    private Vehicle selected;

    public VehiclePanel(VehicleService vehicleService, Navigator navigator) {
        this.vehicleService = vehicleService;
        this.navigator = navigator;
        setLayout(new BorderLayout());

        cardContainer.add(buildDirectoryPage(), PAGE_DIRECTORY);
        cardContainer.add(buildAddPage(), PAGE_ADD);
        cardContainer.add(buildDetailsPage(), PAGE_DETAILS);
        add(cardContainer, BorderLayout.CENTER);

        search();
        UiTheme.style(this);
    }

    public void showPage(String page) {
        this.activePage = page;
        if (PAGE_DIRECTORY.equals(page)) search();
        else if (PAGE_ADD.equals(page)) UiUtil.clear(addMessage);
        else if (PAGE_DETAILS.equals(page)) populateDetails(selected);
        cards.show(cardContainer, page);
    }

    private JPanel buildDirectoryPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton addCta = UiTheme.primaryButton("Add New Vehicle");
        addCta.addActionListener(e -> navigator.navigateTo(Navigator.FLEET_ADD));
        page.add(UiUtil.pageHeader("Fleet Directory", "Search vehicles by registration, category, or status.", addCta), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        categoryBox.addItem(ANY);
        for (VehicleCategory c : VehicleCategory.values()) categoryBox.addItem(c);
        statusBox.addItem(ANY);
        for (VehicleStatus s : VehicleStatus.values()) statusBox.addItem(s);

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> search());
        allBtn.addActionListener(e -> { regField.setText(""); categoryBox.setSelectedIndex(0); statusBox.setSelectedIndex(0); search(); });

        JPanel filterInner = new JPanel(new BorderLayout(0, 8));
        filterInner.setOpaque(false);
        filterInner.add(UiUtil.flow(
                new JLabel("Reg. No.:"), regField, new JLabel("Category:"), categoryBox,
                new JLabel("Status:"), statusBox, searchBtn, allBtn), BorderLayout.NORTH);
        filterInner.add(message, BorderLayout.SOUTH);
        body.add(UiUtil.card(filterInner, 14), BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) selected = vehicleService.findByRegistrationNumber((String) model.getValueAt(row, 0));
                else selected = null;
                detailsBtn.setEnabled(selected != null);
                maintBtn.setEnabled(selected != null);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && selected != null) openDetailsPage(); }
        });

        detailsBtn.addActionListener(e -> openDetailsPage());
        maintBtn.addActionListener(e -> { if (selected != null) navigator.navigateToMaintenance(selected); });

        JPanel tableCardInner = new JPanel(new BorderLayout(0, 12));
        tableCardInner.setOpaque(false);
        tableCardInner.add(new JScrollPane(table), BorderLayout.CENTER);
        
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        footer.add(UiUtil.flowRight(detailsBtn, maintBtn), BorderLayout.EAST);
        
        tableCardInner.add(footer, BorderLayout.SOUTH);
        body.add(UiUtil.card(tableCardInner, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildAddPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to Fleet Directory");
        backBtn.addActionListener(e -> navigator.navigateTo(Navigator.FLEET_DIRECTORY));
        page.add(UiUtil.pageHeader("Register New Vehicle", "Add a fleet vehicle under a fixed category.", backBtn), BorderLayout.NORTH);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createEmptyBorder(22, 26, 26, 26));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int r = 0;
        UiUtil.addFormRow(form, r++, "Registration Number *", addRegField, "Unique plate (e.g. LED-4004).");
        UiUtil.addFormRow(form, r++, "Make *", addMakeField, "Manufacturer (e.g. Toyota).");
        UiUtil.addFormRow(form, r++, "Model *", addModelField, "Model name (e.g. Civic).");
        UiUtil.addFormRow(form, r++, "Year *", addYearField, "4-digit model year.");
        UiUtil.addFormRow(form, r++, "Category *", addCatBox, "Economy, Sedan, SUV, etc.");
        UiUtil.addFormRow(form, r++, "Transmission *", addTransBox, "Automatic or Manual.");
        UiUtil.addFormRow(form, r++, "Fuel Type *", addFuelBox, "Petrol, Diesel, Hybrid, Electric.");
        UiUtil.addFormRow(form, r++, "Mileage *", addMileageField, "Odometer reading (>= 0).");
        UiUtil.addFormRow(form, r++, "Daily Rate *", addRateField, "Rate per day (> 0).");

        JButton commitBtn = UiTheme.successButton("Commit to Fleet");
        JButton clearBtn = UiTheme.secondaryButton("Clear");
        JButton cancelBtn = UiTheme.secondaryButton("Cancel");
        commitBtn.addActionListener(e -> commitVehicle());
        clearBtn.addActionListener(e -> clearAddForm());
        cancelBtn.addActionListener(e -> navigator.navigateTo(Navigator.FLEET_DIRECTORY));

        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(14, 6, 0, 6));
        footer.add(addMessage, BorderLayout.NORTH);
        footer.add(UiUtil.flowRight(cancelBtn, clearBtn, commitBtn), BorderLayout.SOUTH);

        JPanel inner = new JPanel(new BorderLayout(0, 12));
        inner.setOpaque(false);
        inner.add(form, BorderLayout.CENTER);
        inner.add(footer, BorderLayout.SOUTH);

        JPanel card = UiUtil.card(inner, 22);
        card.setMaximumSize(new Dimension(820, 820));
        card.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.add(card);
        wrapper.add(Box.createVerticalGlue());

        page.add(UiUtil.scrollable(wrapper), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDetailsPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to Fleet Directory");
        JButton openMaintBtn = UiTheme.secondaryButton("Manage Maintenance Records");
        backBtn.addActionListener(e -> navigator.navigateTo(Navigator.FLEET_DIRECTORY));
        openMaintBtn.addActionListener(e -> { if (selected != null) navigator.navigateToMaintenance(selected); });
        page.add(UiUtil.pageHeader("Vehicle Details", "Specifications and service history.", backBtn, openMaintBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(20, 26, 20, 26)));

        JPanel specs = new JPanel(new GridBagLayout());
        specs.setOpaque(false);
        int r = 0;
        UiUtil.addRow(specs, r++, "Registration Number:", dRegVal);
        UiUtil.addRow(specs, r++, "Make & Model:", dMakeModelVal);
        UiUtil.addRow(specs, r++, "Category:", dCatVal);
        UiUtil.addRow(specs, r++, "Transmission / Fuel:", dPowertrainVal);
        UiUtil.addRow(specs, r++, "Current Mileage:", dMileageVal);
        UiUtil.addRow(specs, r++, "Daily Rental Rate:", dRateVal);
        UiUtil.addRow(specs, r++, "Current Status:", dStatusVal);
        body.add(UiUtil.card(specs, 20), BorderLayout.NORTH);

        JTable maintTable = UiUtil.table(maintHistoryModel);
        JPanel hist = UiUtil.card(UiUtil.titled("Service & Maintenance History", new JScrollPane(maintTable)), 14);
        body.add(hist, BorderLayout.CENTER);

        page.add(UiUtil.scrollable(body), BorderLayout.CENTER);
        return page;
    }

    private void commitVehicle() {
        UiUtil.clear(addMessage);
        try {
            Integer year = UiUtil.parseInt(addYearField.getText(), "Year");
            Double mileage = UiUtil.parseNumber(addMileageField.getText(), "Mileage");
            Double rate = UiUtil.parseNumber(addRateField.getText(), "Rate");
            Vehicle v = vehicleService.registerVehicle(
                    addRegField.getText(), addMakeField.getText(), addModelField.getText(), year,
                    (VehicleCategory) addCatBox.getSelectedItem(), (TransmissionType) addTransBox.getSelectedItem(),
                    (FuelType) addFuelBox.getSelectedItem(), mileage, rate);
            clearAddForm();
            UiUtil.info(this, "Registered", "Vehicle " + v.getRegistrationNumber() + " added.");
            selected = v;
            navigator.navigateTo(Navigator.FLEET_DIRECTORY);
        } catch (ValidationException | BusinessRuleException ex) { UiUtil.showError(addMessage, ex.getMessage()); }
    }

    private void clearAddForm() {
        addRegField.setText(""); addMakeField.setText(""); addModelField.setText("");
        addYearField.setText(""); addMileageField.setText(""); addRateField.setText("");
        UiUtil.clear(addMessage);
    }

    private void search() {
        UiUtil.clear(message);
        VehicleCategory cat = categoryBox.getSelectedItem() instanceof VehicleCategory ? (VehicleCategory) categoryBox.getSelectedItem() : null;
        VehicleStatus status = statusBox.getSelectedItem() instanceof VehicleStatus ? (VehicleStatus) statusBox.getSelectedItem() : null;
        current = vehicleService.search(regField.getText(), cat, status);
        model.setRowCount(0);
        for (Vehicle v : current) {
            model.addRow(new Object[]{ v.getRegistrationNumber(), v.getMake(), v.getModel(), v.getYear(), v.getCategory(), v.getStatus() });
        }
        table.clearSelection();
        detailsBtn.setEnabled(false);
        maintBtn.setEnabled(false);
        if (current.isEmpty()) UiUtil.showError(message, "No records found.");
    }

    private void openDetailsPage() {
        if (selected == null) return;
        populateDetails(selected);
        cards.show(cardContainer, PAGE_DETAILS);
    }

    private void populateDetails(Vehicle v) {
        maintHistoryModel.setRowCount(0);
        Vehicle fresh = vehicleService.findByRegistrationNumber(v.getRegistrationNumber());
        if (fresh != null) { v = fresh; selected = fresh; }
        dRegVal.setText(v.getRegistrationNumber());
        dMakeModelVal.setText(v.getMake() + " " + v.getModel() + " (" + v.getYear() + ")");
        dCatVal.setText(String.valueOf(v.getCategory()));
        dPowertrainVal.setText(v.getTransmissionType() + " / " + v.getFuelType());
        dMileageVal.setText(String.format("%.0f km", v.getCurrentMileage()));
        dRateVal.setText(String.format("%.2f / day", v.getDailyRentalRate()));
        dStatusVal.setText(String.valueOf(v.getStatus()));
        for (MaintenanceRecord m : v.getMaintenanceHistory()) {
            maintHistoryModel.addRow(new Object[]{ m.getRecordId(), m.getDate(), m.getMaintenanceType(), String.format("%.2f", m.getCost()), m.getCompletionStatus(), m.getDescription() });
        }
    }

    @Override public void refresh() { if (PAGE_DIRECTORY.equals(activePage)) search(); else if (PAGE_DETAILS.equals(activePage) && selected != null) populateDetails(selected); }
}