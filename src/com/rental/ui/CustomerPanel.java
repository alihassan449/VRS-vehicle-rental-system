package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.controller.RentalManagementController;
import com.rental.model.Customer;

/** FR1-FR5: register, search and update customers, view their rental history. */
public class CustomerPanel extends JPanel {

    private final RentalManagementController controller;
    private final Session session;

    private final JTextField idField = new JTextField(10);
    private final JTextField nameField = new JTextField(15);
    private final JTextField contactField = new JTextField(12);
    private final JTextField nationalIdField = new JTextField(12);
    private final JTextField addressField = new JTextField(20);
    private final JTextField licenseNoField = new JTextField(12);
    private final JTextField licenseExpiryField = new JTextField(10);
    private final JTextField searchField = new JTextField(15);

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Customer ID", "Name", "Contact",
            "National ID", "Address", "License No", "License Expiry", "License Valid?"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public CustomerPanel(RentalManagementController controller, Session session) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });
        showCustomers(controller.listCustomers());
        UiTheme.style(this);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Customer details - register (FR1) / update contact info (FR4)"));
        FormUtil.Grid g = new FormUtil.Grid(form, 3);
        g.add("Customer ID (blank = auto):", idField);
        g.add("Name:", nameField);
        g.add("Contact number:", contactField);
        g.add("National ID:", nationalIdField);
        g.add("Address:", addressField);
        g.add("Driving license no:", licenseNoField);
        g.add("License expiry (yyyy-MM-dd):", licenseExpiryField);
        g.newRow();

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton register = new JButton("Register Customer");
        register.addActionListener(e -> registerCustomer());
        JButton update = new JButton("Update Contact Info of Selected");
        update.addActionListener(e -> updateContact());
        JButton clear = new JButton("Clear Form");
        clear.addActionListener(e -> clearForm());
        buttons.add(register);
        buttons.add(update);
        buttons.add(clear);
        g.addButtons(buttons);
        return form;
    }

    private JPanel buildBottom() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Search by customer ID, national ID or name (FR2)"));
        JButton search = new JButton("Search");
        search.addActionListener(e -> showCustomers(controller.searchCustomers(searchField.getText())));
        JButton all = new JButton("Show All");
        all.addActionListener(e -> showCustomers(controller.listCustomers()));
        JButton history = new JButton("Rental History of Selected (FR3)");
        history.addActionListener(e -> showHistory());
        panel.add(searchField);
        panel.add(search);
        panel.add(all);
        panel.add(history);
        return panel;
    }

    private void registerCustomer() {
        try {
            Customer c = controller.registerCustomer(session.getCurrentStaff(), idField.getText(),
                    nameField.getText(), addressField.getText(), contactField.getText(),
                    nationalIdField.getText(), licenseNoField.getText(),
                    FormUtil.parseDate(licenseExpiryField.getText(), "License expiry"));
            clearForm();
            showCustomers(controller.listCustomers());
            FormUtil.showInfo(this, "Registered customer " + c.getCustomerID());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void updateContact() {
        try {
            String id = selectedId();
            controller.updateCustomerContact(session.getCurrentStaff(), id, addressField.getText(),
                    contactField.getText());
            showCustomers(controller.listCustomers());
            FormUtil.showInfo(this, "Contact information updated for " + id);
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private void showHistory() {
        try {
            String id = selectedId();
            FormUtil.showText(this, "Rental history", controller.customerHistoryReport(id).toString());
        } catch (Exception ex) {
            FormUtil.showError(this, ex);
        }
    }

    private String selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("Select a customer in the table first");
        }
        return (String) model.getValueAt(row, 0);
    }

    private void fillFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        idField.setText(String.valueOf(model.getValueAt(row, 0)));
        nameField.setText(String.valueOf(model.getValueAt(row, 1)));
        contactField.setText(String.valueOf(model.getValueAt(row, 2)));
        nationalIdField.setText(String.valueOf(model.getValueAt(row, 3)));
        addressField.setText(String.valueOf(model.getValueAt(row, 4)));
        licenseNoField.setText(String.valueOf(model.getValueAt(row, 5)));
        licenseExpiryField.setText(String.valueOf(model.getValueAt(row, 6)));
    }

    private void clearForm() {
        table.clearSelection();
        for (JTextField f : new JTextField[]{idField, nameField, contactField, nationalIdField,
                addressField, licenseNoField, licenseExpiryField}) {
            f.setText("");
        }
    }

    private void showCustomers(List<Customer> customers) {
        model.setRowCount(0);
        for (Customer c : customers) {
            model.addRow(new Object[]{c.getCustomerID(), c.getName(), c.getContactNumber(), c.getNationalID(),
                    c.getAddress(), c.getDrivingLicenseNo(), c.getLicenseExpiryDate(),
                    c.isLicenseValid() ? "Yes" : "No (expired)"});
        }
    }
}
