package com.rental.ui.maintenance;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.exception.ValidationException;
import com.rental.model.MaintenanceRecord;
import com.rental.model.Vehicle;
import com.rental.service.MaintenanceService;
import com.rental.service.VehicleService;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;

public class MaintenancePanel extends JPanel implements Refreshable {
    private static final String PAGE_LIST = "LIST";
    private static final String PAGE_FORM = "FORM";
    private static final String PAGE_HISTORY = "HISTORY";
    private static final String IN_PROGRESS = "In Progress";
    private static final String COMPLETED = "Completed";

    private final MaintenanceService maintenanceService;
    private final VehicleService vehicleService;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);

    // List Page
    private final JTextField searchField = new JTextField(14);
    private final JCheckBox flaggedOnly = new JCheckBox("Flagged by returns only");
    private final JLabel flaggedBadge = UiUtil.badge("0 Flagged", Color.WHITE, UiTheme.WARNING);
    private final DefaultTableModel vehicleModel = UiUtil.tableModel("Reg. No.", "Vehicle", "Status", "Flag");
    private final JTable vehicleTable = UiUtil.table(vehicleModel);
    private final JButton proceedBtn = UiTheme.primaryButton("Log Maintenance Record");
    private final JButton historyBtn = UiTheme.secondaryButton("View Maintenance History");
    private List<Vehicle> vehicles = new ArrayList<>();

    // Form Page
    private final JLabel vehicleLabel = new JLabel("No vehicle selected");
    private final UiUtil.PlaceholderTextField typeField = new UiUtil.PlaceholderTextField(18, "e.g. Oil change or damage repair");
    private final UiUtil.PlaceholderTextField dateField = new UiUtil.PlaceholderTextField(14, "e.g. 2026-11-28");
    private final JTextArea descriptionArea = new JTextArea(5, 26);
    private final UiUtil.PlaceholderTextField costField = new UiUtil.PlaceholderTextField(14, "e.g. 2500");
    private final JComboBox<String> statusBox = new JComboBox<>(new String[]{IN_PROGRESS, COMPLETED});
    private final JLabel message = UiUtil.messageLabel();
    private Vehicle selected;

    // History Table
    private final DefaultTableModel historyModel = UiUtil.tableModel("Record", "Type", "Date", "Cost", "Status");
    private final JTable historyTable = UiUtil.table(historyModel);

    public MaintenancePanel(MaintenanceService maintenanceService, VehicleService vehicleService) {
        this.maintenanceService = maintenanceService;
        this.vehicleService = vehicleService;
        setLayout(new BorderLayout());

        cardContainer.add(buildListPage(), PAGE_LIST);
        cardContainer.add(buildFormPage(), PAGE_FORM);
        cardContainer.add(buildHistoryPage(), PAGE_HISTORY);
        add(cardContainer, BorderLayout.CENTER);

        loadVehicles();
        UiTheme.style(this);
    }

    public void preselectVehicle(Vehicle vehicle) {
        this.selected = vehicle;
        searchField.setText(""); flaggedOnly.setSelected(false);
        loadVehicles();
        if (selected != null) openFormPage();
    }

    private JPanel buildListPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.add(UiUtil.pageHeader("Maintenance Records", "Search vehicles to log repairs.", flaggedBadge), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> loadVehicles());
        searchField.addActionListener(e -> loadVehicles());
        flaggedOnly.addActionListener(e -> loadVehicles());
        flaggedOnly.setFont(UiTheme.LABEL_FONT);
        allBtn.addActionListener(e -> { searchField.setText(""); flaggedOnly.setSelected(false); loadVehicles(); });

        JPanel filterInner = new JPanel(new BorderLayout(0, 8));
        filterInner.setOpaque(false);
        filterInner.add(UiUtil.flow(new JLabel("Reg. No:"), searchField, searchBtn, allBtn, flaggedOnly), BorderLayout.NORTH);
        body.add(UiUtil.card(filterInner, 14), BorderLayout.NORTH);

        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = vehicleTable.getSelectedRow() >= 0;
            proceedBtn.setEnabled(hasSelection);
            historyBtn.setEnabled(hasSelection);
        });
        JPanel tableCardInner = new JPanel(new BorderLayout(0, 12));
        tableCardInner.setOpaque(false);
        tableCardInner.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        proceedBtn.setEnabled(false);
        proceedBtn.addActionListener(e -> openFormPage());
        historyBtn.addActionListener(e -> openHistoryPage());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        footer.add(UiUtil.flowRight(historyBtn, proceedBtn), BorderLayout.EAST);

        tableCardInner.add(footer, BorderLayout.SOUTH);
        body.add(UiUtil.card(tableCardInner, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildFormPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to List");
        backBtn.addActionListener(e -> { cards.show(cardContainer, PAGE_LIST); loadVehicles(); });
        page.add(UiUtil.pageHeader("UC-11 Maintenance Record",
                "Enter the repair details, cost, and status. After saving, open Maintenance History to complete the record.", backBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setBorder(BorderFactory.createEmptyBorder(20, 26, 26, 26));

        dateField.setText(LocalDate.now().toString());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(UiTheme.LABEL_FONT);
        descriptionArea.setMargin(new Insets(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JPanel repairDetails = new JPanel(new GridBagLayout());
        repairDetails.setOpaque(false);
        addMaintenanceField(repairDetails, 0, "Vehicle", vehicleLabel, null);
        addMaintenanceField(repairDetails, 1, "Maintenance Type *", typeField, "e.g. Oil change or damage repair");
        addMaintenanceField(repairDetails, 2, "Date *", dateField, "Format: yyyy-MM-dd");

        JPanel workDetails = new JPanel(new GridBagLayout());
        workDetails.setOpaque(false);
        addMaintenanceField(workDetails, 0, "Cost *", costField, "Enter the repair or service cost");
        addMaintenanceField(workDetails, 1, "Status *", statusBox, "In progress or completed");

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 12, 12);
        form.add(UiUtil.titled("Vehicle & Repair", repairDetails), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 12, 12, 0);
        form.add(UiUtil.titled("Cost & Status", workDetails), gc);

        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.insets = new Insets(0, 0, 0, 0);
        JPanel descriptionPanel = new JPanel(new BorderLayout(0, 6));
        descriptionPanel.setOpaque(false);
        JLabel descriptionLabel = new JLabel("Description *");
        descriptionLabel.setFont(UiTheme.LABEL_BOLD);
        descriptionPanel.add(descriptionLabel, BorderLayout.NORTH);
        descriptionPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);
        JLabel descriptionHint = new JLabel("Describe the issue, parts replaced, or work performed.");
        descriptionHint.setFont(UiTheme.SMALL_FONT);
        descriptionHint.setForeground(UiTheme.TEXT_MUTED);
        descriptionPanel.add(descriptionHint, BorderLayout.SOUTH);
        form.add(descriptionPanel, gc);

        JButton saveBtn = UiTheme.primaryButton("Save Record");
        saveBtn.addActionListener(e -> saveRecord());

        JPanel formWrap = new JPanel(new BorderLayout(0, 12));
        formWrap.setOpaque(false);
        formWrap.add(form, BorderLayout.CENTER);
        JPanel formActions = new JPanel(new BorderLayout(12, 8));
        formActions.setOpaque(false);
        formActions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        formActions.add(message, BorderLayout.WEST);
        formActions.add(UiUtil.flowRight(saveBtn), BorderLayout.EAST);
        formWrap.add(formActions, BorderLayout.SOUTH);

        JPanel leftPanel = UiUtil.card(UiUtil.titled("Log New Record", formWrap), 18);
        body.add(leftPanel, BorderLayout.CENTER);

        page.add(UiUtil.scrollable(body), BorderLayout.CENTER);
        return page;
    }

    private JPanel buildHistoryPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to List");
        backBtn.addActionListener(e -> { cards.show(cardContainer, PAGE_LIST); loadVehicles(); });
        page.add(UiUtil.pageHeader("Maintenance History",
                "View and complete maintenance records for the selected vehicle.", backBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));

        JButton completeBtn = UiTheme.primaryButton("Mark Record Completed");
        completeBtn.addActionListener(e -> completeRecord());

        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        footer.add(message, BorderLayout.WEST);
        footer.add(UiUtil.flowRight(completeBtn), BorderLayout.EAST);

        JPanel cardInner = new JPanel(new BorderLayout(0, 12));
        cardInner.setOpaque(false);
        historyTable.setRowHeight(48);
        cardInner.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        cardInner.add(footer, BorderLayout.SOUTH);

        body.add(UiUtil.card(cardInner, 16), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void addMaintenanceField(JPanel panel, int row, String label, java.awt.Component field, String hint) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = row * 3; gc.insets = new Insets(10, 8, 5, 8);
        gc.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel(label);
        title.setFont(UiTheme.LABEL_BOLD);
        panel.add(title, gc);

        gc.gridy = row * 3 + 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 8, 0, 8);
        panel.add(field, gc);

        if (hint != null) {
            JLabel hintLabel = new JLabel(hint);
            hintLabel.setFont(UiTheme.SMALL_FONT);
            hintLabel.setForeground(UiTheme.TEXT_MUTED);
            gc.gridy = row * 3 + 2; gc.insets = new Insets(3, 8, 10, 8);
            panel.add(hintLabel, gc);
        }
    }

    private void loadVehicles() {
        List<Vehicle> flagged = maintenanceService.findFlaggedVehicles();
        flaggedBadge.setText(flagged.size() + " Flagged");
        vehicles = new ArrayList<>(vehicleService.search(searchField.getText(), null, null));
        if (flaggedOnly.isSelected()) vehicles.retainAll(flagged);

        vehicleModel.setRowCount(0);
        for (Vehicle v : vehicles) {
            vehicleModel.addRow(new Object[]{ v.getRegistrationNumber(), v.getMake() + " " + v.getModel(), v.getStatus(), flagged.contains(v) ? "Needs Record" : "" });
        }
        vehicleTable.clearSelection();
        proceedBtn.setEnabled(false);
    }

    private void openFormPage() {
        int row = vehicleTable.getSelectedRow();
        if (row < 0) return;
        String reg = (String) vehicleModel.getValueAt(vehicleTable.convertRowIndexToModel(row), 0);
        selected = vehicleService.findByRegistrationNumber(reg);
        
        if (selected != null) {
            UiUtil.clear(message);
            vehicleLabel.setText(selected.getRegistrationNumber() + " - " + selected.getMake());
            if (maintenanceService.findFlaggedVehicles().contains(selected)) {
                typeField.setText("Damage repair"); descriptionArea.setText(maintenanceService.returnInfoFor(selected)); statusBox.setSelectedItem(IN_PROGRESS);
            } else {
                typeField.setText(""); descriptionArea.setText(""); statusBox.setSelectedItem(IN_PROGRESS);
            }
            cards.show(cardContainer, PAGE_FORM);
        }
    }

    private boolean selectVehicleForMaintenance() {
        int row = vehicleTable.getSelectedRow();
        if (row < 0) return false;
        String reg = (String) vehicleModel.getValueAt(vehicleTable.convertRowIndexToModel(row), 0);
        selected = vehicleService.findByRegistrationNumber(reg);
        return selected != null;
    }

    private void openHistoryPage() {
        if (!selectVehicleForMaintenance()) return;
        refreshHistory();
        cards.show(cardContainer, PAGE_HISTORY);
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        if (selected == null) return;
        for (MaintenanceRecord m : selected.getMaintenanceHistory()) {
            historyModel.addRow(new Object[]{ m.getRecordId(), m.getMaintenanceType(), m.getDate(), String.format("%.2f", m.getCost()), m.getCompletionStatus() });
        }
    }

    private void saveRecord() {
        UiUtil.run(this, message, () -> {
            if (selected == null) throw new ValidationException("Select a vehicle first.");
            boolean done = COMPLETED.equals(statusBox.getSelectedItem());
            maintenanceService.createRecord(selected, typeField.getText(), UiUtil.parseDate(dateField.getText(), "Date"), descriptionArea.getText(), UiUtil.parseNumber(costField.getText(), "Cost"), done);
            loadVehicles();
            UiUtil.showOk(message, "Record saved.");
        });
    }

    private void completeRecord() {
        int row = historyTable.getSelectedRow();
        UiUtil.run(this, message, () -> {
            if (selected == null || row < 0) throw new ValidationException("Select a history record first.");
            String id = (String) historyModel.getValueAt(row, 0);
            MaintenanceRecord rec = selected.getMaintenanceHistory().stream().filter(m -> m.getRecordId().equals(id)).findFirst().orElse(null);
            maintenanceService.completeRecord(rec);
            loadVehicles();
            UiUtil.showOk(message, "Record completed.");
        });
    }

    @Override public void refresh() { loadVehicles(); }
}