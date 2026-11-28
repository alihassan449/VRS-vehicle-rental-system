package com.rental.ui.report;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.time.LocalDate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.VehicleStatus;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.report.ReportService;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;

public class ReportPanel extends JPanel {
    private static final String[] TYPES = { "Vehicle Status", "Customer History", "Vehicle History", "Revenue" };
    private static final String ALL = "All Statuses";

    private final ReportService reportService;
    private String selectedType = TYPES[0];
    private final JButton[] typeButtons = new JButton[TYPES.length];

    private final CardLayout cards = new CardLayout();
    private final JPanel inputs = new JPanel(cards);
    private final JComboBox<Object> statusBox = new JComboBox<>();
    private final JTextField customerField = new JTextField(18);
    private final JTextField vehicleField = new JTextField(14);
    private final JTextField fromField = new JTextField(11);
    private final JTextField toField = new JTextField(11);
    private final JLabel message = UiUtil.messageLabel();
    private final DefaultTableModel outputModel = UiUtil.tableModel("Report Results");
    private final JTable output = UiUtil.table(outputModel);

    public ReportPanel(ReportService reportService) {
        this.reportService = reportService;
        setLayout(new BorderLayout());

        JButton printBtn = UiTheme.secondaryButton("Print Report");
        printBtn.addActionListener(e -> printCurrentReport());
        add(UiUtil.pageHeader("System Reports",
                "UC-12 Generate System Reports · UC-12.1–UC-12.4",
                printBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));

        statusBox.addItem(ALL);
        for (VehicleStatus s : ReportService.REPORT_STATUSES) statusBox.addItem(s);
        fromField.setText(LocalDate.now().minusMonths(1).toString());
        toField.setText(LocalDate.now().toString());

        JPanel catalog = new JPanel();
        catalog.setOpaque(false);
        catalog.setLayout(new javax.swing.BoxLayout(catalog, javax.swing.BoxLayout.Y_AXIS));
        catalog.setPreferredSize(new java.awt.Dimension(280, 0));
        JLabel catalogTitle = new JLabel("Report Catalog");
        catalogTitle.setFont(UiTheme.TITLE_FONT);
        catalogTitle.setBorder(BorderFactory.createEmptyBorder(8, 8, 14, 8));
        catalog.add(catalogTitle);

        for (int i = 0; i < TYPES.length; i++) {
            final int idx = i;
            JButton btn = UiTheme.secondaryButton((i + 1) + "  " + TYPES[i]);
            btn.setPreferredSize(new java.awt.Dimension(250, 48));
            btn.setMaximumSize(new java.awt.Dimension(250, 48));
            btn.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            btn.addActionListener(e -> selectType(idx));
            typeButtons[i] = btn;
            catalog.add(javax.swing.Box.createVerticalStrut(8));
            catalog.add(btn);
        }

        inputs.setOpaque(false);
        inputs.add(UiUtil.flow(new JLabel("Status:"), statusBox), TYPES[0]);
        inputs.add(UiUtil.flow(new JLabel("Customer ID / National ID / Name:"), customerField), TYPES[1]);
        inputs.add(UiUtil.flow(new JLabel("Vehicle Registration Number:"), vehicleField), TYPES[2]);
        inputs.add(UiUtil.flow(new JLabel("Start Date:"), fromField,
                new JLabel("End Date:"), toField), TYPES[3]);

        JButton generateBtn = UiTheme.primaryButton("Generate Report");
        generateBtn.addActionListener(e -> generate());

        JPanel paramInner = new JPanel(new BorderLayout(10, 10));
        paramInner.setOpaque(false);
        paramInner.add(inputs, BorderLayout.CENTER);
        paramInner.add(UiUtil.flowRight(generateBtn), BorderLayout.EAST);
        paramInner.add(message, BorderLayout.SOUTH);

        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setOpaque(false);
        main.add(UiUtil.card(UiUtil.titled("Report Parameters", paramInner), 14), BorderLayout.NORTH);
        main.add(UiUtil.card(UiUtil.titled("Generated Tabular Report", new JScrollPane(output)), 14), BorderLayout.CENTER);

        body.add(UiUtil.card(catalog, 14), BorderLayout.WEST);
        body.add(main, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        UiTheme.style(this);
        selectType(0);
    }

    private void selectType(int idx) {
        selectedType = TYPES[idx];
        cards.show(inputs, selectedType);
        UiUtil.clear(message);
        outputModel.setRowCount(0);
        for (int i = 0; i < typeButtons.length; i++) {
            UiTheme.applyButtonVariant(typeButtons[i],
                    i == idx ? UiTheme.ButtonVariant.PRIMARY : UiTheme.ButtonVariant.SECONDARY);
        }
    }

    private void setOutputColumns(String... columns) {
        outputModel.setColumnIdentifiers(columns);
        outputModel.setRowCount(0);
    }

    private void generate() {
        UiUtil.clear(message);
        try {
            if (selectedType.equals(TYPES[0])) {
                Object sel = statusBox.getSelectedItem();
                VehicleStatus filter = sel instanceof VehicleStatus ? (VehicleStatus) sel : null;
                java.util.List<Object[]> rows = reportService.vehicleStatusRows(filter);
                setOutputColumns("Registration", "Make", "Model", "Category", "Status");
                if (rows.isEmpty()) {
                    UiUtil.showError(message, "No records found.");
                } else for (Object[] row : rows) outputModel.addRow(row);
            } else if (selectedType.equals(TYPES[1])) {
                java.util.List<Object[]> rows = reportService.customerHistoryRows(customerField.getText());
                setOutputColumns("Agreement", "Vehicle", "Pickup", "Return", "Amount", "Status");
                if (rows.isEmpty()) UiUtil.showError(message, "No records found.");
                else for (Object[] row : rows) outputModel.addRow(row);
            } else if (selectedType.equals(TYPES[2])) {
                java.util.List<Object[]> rows = reportService.vehicleHistoryRows(vehicleField.getText());
                setOutputColumns("Record Type", "Record ID", "Customer", "Start Date", "End Date", "Amount/Cost", "Status/Type");
                if (rows.isEmpty()) UiUtil.showError(message, "No records found.");
                else for (Object[] row : rows) outputModel.addRow(row);
            } else {
                java.util.List<Object[]> rows = reportService.revenueRows(
                        UiUtil.parseDate(fromField.getText(), "Start"),
                        UiUtil.parseDate(toField.getText(), "End"));
                setOutputColumns("Payment", "Agreement", "Date", "Method", "Amount");
                if (rows.isEmpty()) UiUtil.showError(message, "No records found.");
                else for (Object[] row : rows) outputModel.addRow(row);
            }
        } catch (ValidationException | BusinessRuleException ex) {
            setOutputColumns("Report Results");
            UiUtil.showError(message, ex.getMessage());
        }
    }

    private void printCurrentReport() {
        if (outputModel.getRowCount() == 0) {
            UiUtil.warn(this, "Empty Report", "Generate a report first.");
            return;
        }
        StringBuilder text = new StringBuilder(selectedType).append('\n');
        for (int c = 0; c < outputModel.getColumnCount(); c++) {
            text.append(outputModel.getColumnName(c)).append('\t');
        }
        text.append('\n');
        for (int r = 0; r < outputModel.getRowCount(); r++) {
            for (int c = 0; c < outputModel.getColumnCount(); c++) {
                text.append(outputModel.getValueAt(r, c)).append('\t');
            }
            text.append('\n');
        }
        UiUtil.showPrintPreview(this, selectedType, text.toString());
    }
}