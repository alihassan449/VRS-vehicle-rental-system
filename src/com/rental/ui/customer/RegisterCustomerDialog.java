package com.rental.ui.customer;

import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import java.awt.*;
import javax.swing.*;

import com.rental.exception.BusinessRuleException;
import com.rental.exception.LvaUnavailableException;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.service.CustomerService;

/**
 * UC-01 Register Customer (legacy dialog). The full-page form in CustomerPanel
 * is the primary UI; this dialog is retained for compatibility and shares the
 * same service flow.
 */
public class RegisterCustomerDialog extends JDialog {
    private final CustomerService customerService;
    private final JTextField idField = new JTextField(20);
    private final JTextField nameField = new JTextField(20);
    private final JTextField addressField = new JTextField(20);
    private final JTextField contactField = new JTextField(20);
    private final JTextField nationalIdField = new JTextField(20);
    private final JTextField licenseField = new JTextField(20);
    private final JTextField expiryField = new JTextField(20);
    private final JLabel message = UiUtil.messageLabel();
    private Customer registered;

    private RegisterCustomerDialog(Component parent, CustomerService customerService) {
        super(SwingUtilities.getWindowAncestor(parent), "Register New Customer", ModalityType.APPLICATION_MODAL);
        this.customerService = customerService;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        int r = 0;
        UiUtil.addRow(form, r++, "Customer ID:", idField);
        UiUtil.addRow(form, r++, "Name:", nameField);
        UiUtil.addRow(form, r++, "Address:", addressField);
        UiUtil.addRow(form, r++, "Contact Number:", contactField);
        UiUtil.addRow(form, r++, "National ID:", nationalIdField);
        UiUtil.addRow(form, r++, "Driving License Number:", licenseField);
        UiUtil.addRow(form, r++, "License Expiry (yyyy-MM-dd):", expiryField);

        JButton save = UiTheme.successButton("✓  Save & Verify License");
        JButton cancel = UiTheme.secondaryButton("✖  Cancel");
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        message.setPreferredSize(new Dimension(420, 44));
        south.add(message, BorderLayout.NORTH);
        south.add(UiUtil.flowRight(cancel, save), BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        UiTheme.style(getContentPane());
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(parent);
    }

    private void save() {
        UiUtil.clear(message);
        try {
            java.time.LocalDate expiry = UiUtil.parseDate(expiryField.getText(), "License expiry");
            Customer c = customerService.registerCustomer(idField.getText(), nameField.getText(),
                    addressField.getText(), contactField.getText(), nationalIdField.getText(),
                    licenseField.getText(), expiry);
            registered = c;
            UiUtil.info(this, "Registration Successful", "Registration Successful\nCustomer " + c.getCustomerId()
                    + " - " + c.getName() + " was registered.");
            dispose();
        } catch (ValidationException ex) {
            UiUtil.showError(message, ex.getMessage());
        } catch (LvaUnavailableException ex) {
            UiUtil.error(this, "LVA Unavailable", ex.getMessage() + "\nThe customer was not registered; you may retry.");
        } catch (BusinessRuleException ex) {
            UiUtil.error(this, "Registration halted", ex.getMessage());
            UiUtil.showError(message, ex.getMessage());
        }
    }

    public static Customer showDialog(Component parent, CustomerService customerService) {
        RegisterCustomerDialog d = new RegisterCustomerDialog(parent, customerService);
        d.setVisible(true);
        return d.registered;
    }
}