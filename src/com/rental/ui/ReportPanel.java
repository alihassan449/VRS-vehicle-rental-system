package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.rental.controller.RentalManagementController;
import com.rental.enums.VehicleStatus;
import com.rental.report.Report;

/** FR28-FR31: fleet status, customer history, vehicle history and revenue reports (all staff). */
public class ReportPanel extends JPanel {

    private final RentalManagementController controller;

    private final JComboBox<String> statusBox = new JComboBox<>(FormUtil.withAny(VehicleStatus.values()));
    private final JTextField customerField = new JTextField(10);
    private final JTextField vehicleField = new JTextField(10);
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JTextArea output = new JTextArea();

    public ReportPanel(RentalManagementController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildControls(), BorderLayout.NORTH);
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(output), BorderLayout.CENTER);
        UiTheme.style(this);
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r1.setBorder(BorderFactory.createTitledBorder("Vehicle status report (FR28) - (any) shows Available, Reserved, Rented and Under Maintenance"));
        JButton b1 = new JButton("Generate");
        b1.addActionListener(e -> run(() -> {
            String s = (String) statusBox.getSelectedItem();
            return controller.vehicleStatusReport(FormUtil.ANY.equals(s) ? null : VehicleStatus.valueOf(s));
        }));
        r1.add(new JLabel("Status:"));
        r1.add(statusBox);
        r1.add(b1);
        panel.add(r1);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r2.setBorder(BorderFactory.createTitledBorder("Customer rental history (FR29)"));
        JButton b2 = new JButton("Generate");
        b2.addActionListener(e -> run(() -> controller.customerHistoryReport(customerField.getText().trim())));
        r2.add(new JLabel("Customer ID:"));
        r2.add(customerField);
        r2.add(b2);
        panel.add(r2);

        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r3.setBorder(BorderFactory.createTitledBorder("Vehicle rental and maintenance history (FR30)"));
        JButton b3 = new JButton("Generate");
        b3.addActionListener(e -> run(() -> controller.vehicleHistoryReport(vehicleField.getText().trim())));
        r3.add(new JLabel("Registration no:"));
        r3.add(vehicleField);
        r3.add(b3);
        panel.add(r3);

        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r4.setBorder(BorderFactory.createTitledBorder("Revenue report (FR31) - dates as yyyy-MM-dd"));
        JButton b4 = new JButton("Generate");
        b4.addActionListener(e -> run(() -> controller.revenueReport(
                FormUtil.parseDate(fromField.getText(), "From date"), FormUtil.parseDate(toField.getText(), "To date"))));
        r4.add(new JLabel("From:"));
        r4.add(fromField);
        r4.add(new JLabel("To:"));
        r4.add(toField);
        r4.add(b4);
        panel.add(r4);
        return panel;
    }

    private void run(java.util.function.Supplier<Report> generator) {
        try {
            output.setText(generator.get().toString());
            output.setCaretPosition(0);
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }
}
