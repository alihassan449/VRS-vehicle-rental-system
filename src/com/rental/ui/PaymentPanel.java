package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.controller.RentalManagementController;
import com.rental.model.Payment;
import com.rental.model.RentalAgreement;

/** FR24-FR25: record payments against a rental agreement and print receipt / invoice (Rental Staff). */
public class PaymentPanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField agreementField = new JTextField(10);
    private final JTextField amountField = new JTextField(10);
    private final JComboBox<String> methodBox = new JComboBox<>(
            new String[]{"Cash", "Card", "Bank Transfer", "Mobile Wallet"});

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Agreement ID", "Customer",
            "Total Amount", "Paid So Far", "Balance Due"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public PaymentPanel(RentalManagementController controller, Session session) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton invoice = new JButton("View Invoice of Selected (FR25)");
        invoice.addActionListener(e -> showInvoice());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        bottom.add(invoice);
        bottom.add(refresh);
        add(bottom, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                agreementField.setText(String.valueOf(model.getValueAt(table.getSelectedRow(), 0)));
            }
        });
        refresh();
        UiTheme.style(this);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Record payment (FR24) - receipt is printed (FR25)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        FormUtil.addRow(form, gbc, row++, "Agreement ID:", agreementField);
        FormUtil.addRow(form, gbc, row++, "Amount:", amountField);
        FormUtil.addRow(form, gbc, row++, "Payment method:", methodBox);

        JButton pay = new JButton("Record Payment & Show Receipt");
        pay.addActionListener(e -> recordPayment());
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(pay, gbc);
        return form;
    }

    private void recordPayment() {
        try {
            Payment p = controller.recordPayment(session.getCurrentStaff(), agreementField.getText(),
                    FormUtil.parseDouble(amountField.getText(), "Amount"), (String) methodBox.getSelectedItem());
            amountField.setText("");
            refresh();
            FormUtil.showText(this, "Payment receipt", p.generateReceipt());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void showInvoice() {
        try {
            FormUtil.showText(this, "Invoice", controller.getInvoice(agreementField.getText()));
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    public void refresh() {
        model.setRowCount(0);
        for (RentalAgreement a : controller.listAgreements()) {
            model.addRow(new Object[]{a.getAgreementID(), a.getCustomer().getName(), a.getTotalAmount(),
                    a.totalPaid(), a.balanceDue()});
        }
    }
}
