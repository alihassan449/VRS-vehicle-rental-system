package com.rental.ui.customer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.CustomerSearchBy;
import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.model.Customer;
import com.rental.service.CustomerService;
import com.rental.service.RentalService;
import com.rental.service.SimulatedLvaService;
import com.rental.ui.common.Navigator;
import com.rental.ui.common.Refreshable;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.IdGenerator;

public class CustomerPanel extends JPanel implements Refreshable {
    public static final String PAGE_LIST = "LIST";
    public static final String PAGE_PROFILE = "PROFILE";
    public static final String PAGE_FORM = "FORM";

    private final CustomerService customerService;
    private final Navigator navigator;
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);

    // List Page
    private final JComboBox<CustomerSearchBy> searchBy = new JComboBox<>(CustomerSearchBy.values());
    private final JTextField searchField = new JTextField(20);
    private final JLabel listMessage = UiUtil.messageLabel();
    private final DefaultTableModel model = UiUtil.tableModel("ID", "Name", "Contact", "National ID", "License Status");
    private final JTable table = UiUtil.table(model);
    private final JButton proceedBtn = UiTheme.primaryButton("View Profile");
    private List<Customer> current = new ArrayList<>();

    // Profile Page
    private final JLabel pId = new JLabel("-");
    private final JLabel pName = new JLabel("-");
    private final JLabel pContact = new JLabel("-");
    private final JLabel pAddress = new JLabel("-");
    private final JLabel pLicense = new JLabel("-");
    private final JLabel pExpiry = new JLabel("-");

    // Form Page
    private final UiUtil.PlaceholderTextField formId = new UiUtil.PlaceholderTextField(22, "Automatically generated");
    private final UiUtil.PlaceholderTextField formName = new UiUtil.PlaceholderTextField(22, "e.g. Ali Ahmed");
    private final UiUtil.PlaceholderTextField formContact = new UiUtil.PlaceholderTextField(22, "e.g. +92 300 1234567");
    private final UiUtil.PlaceholderTextField formAddress = new UiUtil.PlaceholderTextField(22, "House, street, city");
    private final UiUtil.PlaceholderTextField formNationalId = new UiUtil.PlaceholderTextField(22, "e.g. 35202-1234567-1");
    private final UiUtil.PlaceholderTextField formLicense = new UiUtil.PlaceholderTextField(22, "e.g. DL-123456789");
    private final UiUtil.PlaceholderTextField formExpiry = new UiUtil.PlaceholderTextField(22, "e.g. 2029-12-31");
    private final JLabel formMessage = UiUtil.messageLabel();
    private final JLabel formTitle = new JLabel("Register New Customer");

    private Customer selectedCustomer;
    private boolean isEditMode = false;

    public CustomerPanel(CustomerService customerService, RentalService rentalService,
                         SimulatedLvaService lvaService, Navigator navigator) {
        this.customerService = customerService;
        this.navigator = navigator;
        setLayout(new BorderLayout());

        cardContainer.add(buildListPage(), PAGE_LIST);
        cardContainer.add(buildProfilePage(), PAGE_PROFILE);
        cardContainer.add(buildFormPage(), PAGE_FORM);
        add(cardContainer, BorderLayout.CENTER);

        loadList();
        UiTheme.style(this);
    }

    public void showPage(String page) {
        if (PAGE_FORM.equals(page)) openFormPage(selectedCustomer, selectedCustomer != null);
        else if (PAGE_PROFILE.equals(page)) { if (selectedCustomer != null) openProfilePage(); else cards.show(cardContainer, PAGE_LIST); }
        else { cards.show(cardContainer, PAGE_LIST); loadList(); }
    }

    private JPanel buildListPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton addBtn = UiTheme.primaryButton("Register Customer");
        addBtn.addActionListener(e -> openFormPage(null, false));
        page.add(UiUtil.pageHeader("Customer Directory", "Search by ID, Name, or National ID.", addBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));

        JButton searchBtn = UiTheme.primaryButton("Search");
        JButton allBtn = UiTheme.secondaryButton("Show All");
        searchBtn.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        allBtn.addActionListener(e -> { searchField.setText(""); loadList(); });

        JPanel searchContent = new JPanel(new BorderLayout(0, 8));
        searchContent.setOpaque(false);
        searchContent.add(UiUtil.flow(new JLabel("Search by:"), searchBy, new JLabel("Keyword:"), searchField, searchBtn, allBtn), BorderLayout.NORTH);
        searchContent.add(listMessage, BorderLayout.SOUTH);
        body.add(UiUtil.card(searchContent, 14), BorderLayout.NORTH);

        table.getSelectionModel().addListSelectionListener(e -> proceedBtn.setEnabled(table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openProfilePage();
            }
        });

        JPanel tableCardInner = new JPanel(new BorderLayout(0, 12));
        tableCardInner.setOpaque(false);
        tableCardInner.add(new JScrollPane(table), BorderLayout.CENTER);
        
        proceedBtn.setEnabled(false);
        proceedBtn.addActionListener(e -> openProfilePage());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        footer.add(UiUtil.flowRight(proceedBtn), BorderLayout.EAST);

        tableCardInner.add(footer, BorderLayout.SOUTH);
        body.add(UiUtil.card(tableCardInner, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to Directory");
        backBtn.addActionListener(e -> { cards.show(cardContainer, PAGE_LIST); loadList(); });
        JButton editBtn = UiTheme.primaryButton("Edit Info");
        editBtn.addActionListener(e -> openFormPage(selectedCustomer, true));
        JButton reserveBtn = UiTheme.secondaryButton("Create Reservation");
        reserveBtn.addActionListener(e -> { if (selectedCustomer != null) navigator.navigateToCreateReservation(selectedCustomer); });

        page.add(UiUtil.pageHeader("Customer Profile", "View customer details and proceed.", backBtn, reserveBtn, editBtn), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, UiTheme.PRIMARY),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        UiUtil.addRow(infoPanel, 0, "Customer ID:", pId);
        UiUtil.addRow(infoPanel, 1, "Full Name:", pName);
        UiUtil.addRow(infoPanel, 2, "Contact Number:", pContact);
        UiUtil.addRow(infoPanel, 3, "Address:", pAddress);
        UiUtil.addRow(infoPanel, 4, "License No:", pLicense);
        UiUtil.addRow(infoPanel, 5, "License Expiry:", pExpiry);
        
        body.add(UiUtil.card(infoPanel, 30));
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildFormPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back");
        backBtn.addActionListener(e -> cards.show(cardContainer, isEditMode ? PAGE_PROFILE : PAGE_LIST));

        formTitle.setFont(UiTheme.PAGE_TITLE_FONT);
        JPanel header = UiUtil.pageHeader(
                isEditMode ? "Edit Customer Information" : "Register a New Customer",
                isEditMode
                        ? "Update the customer's contact details. Identity fields are protected from editing."
                        : "Enter the customer's personal, contact, and driving license details.",
                backBtn);
        page.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createEmptyBorder(20, 26, 26, 26));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JPanel identity = new JPanel(new GridBagLayout());
        identity.setOpaque(false);
        addFormField(identity, 0, "Customer ID", formId, "Generated automatically");
        addFormField(identity, 1, "Full Name", formName, "Customer's legal name");
        addFormField(identity, 2, "National ID", formNationalId, "Government-issued identification");

        JPanel contact = new JPanel(new GridBagLayout());
        contact.setOpaque(false);
        addFormField(contact, 0, "Contact Number", formContact, "Mobile or primary phone");
        addFormField(contact, 1, "Address", formAddress, "Residential or mailing address");

        JPanel license = new JPanel(new GridBagLayout());
        license.setOpaque(false);
        addFormField(license, 0, "License Number", formLicense, "Driving licence identifier");
        addFormField(license, 1, "License Expiry", formExpiry, "Format: yyyy-MM-dd");

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(0, 0, 14, 14);
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        form.add(UiUtil.titled("Identity", identity), gc);
        gc.gridx = 1; gc.insets = new Insets(0, 14, 14, 0);
        form.add(UiUtil.titled("Contact", contact), gc);
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.insets = new Insets(0, 0, 0, 0);
        form.add(UiUtil.titled("Driving License", license), gc);

        JButton cancelBtn = UiTheme.secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> cards.show(cardContainer, isEditMode ? PAGE_PROFILE : PAGE_LIST));
        JButton saveBtn = UiTheme.successButton("Save Customer");
        saveBtn.addActionListener(e -> saveCustomer());

        JPanel actions = new JPanel(new BorderLayout(12, 8));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        actions.add(formMessage, BorderLayout.WEST);
        actions.add(UiUtil.flowRight(cancelBtn, saveBtn), BorderLayout.EAST);

        JPanel formCard = UiUtil.card(form, 24);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(formCard, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);

        body.add(UiUtil.scrollable(content), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private void addFormField(JPanel panel, int row, String label, JTextField field, String hint) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = row * 3; gc.insets = new Insets(12, 8, 5, 8);
        gc.anchor = GridBagConstraints.WEST;
        JLabel title = new JLabel(label);
        title.setFont(UiTheme.LABEL_BOLD);
        panel.add(title, gc);

        gc.gridy = row * 3 + 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 8, 0, 8);
        panel.add(field, gc);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(UiTheme.SMALL_FONT);
        hintLabel.setForeground(UiTheme.TEXT_MUTED);
        gc.gridy = row * 3 + 2; gc.insets = new Insets(3, 8, 12, 8);
        panel.add(hintLabel, gc);
    }

    private void search() {
        UiUtil.clear(listMessage);
        try {
            current = customerService.search((CustomerSearchBy) searchBy.getSelectedItem(), searchField.getText());
            fillResults();
        } catch (ValidationException ex) { UiUtil.showError(listMessage, ex.getMessage()); }
    }

    private void loadList() { current = customerService.findAll(); fillResults(); }

    private void fillResults() {
        model.setRowCount(0);
        for (Customer c : current) {
            model.addRow(new Object[]{ c.getCustomerId(), c.getName(), c.getContactNumber(), c.getNationalId(), c.isLicenseValid() ? "Valid" : "Expired" });
        }
        table.clearSelection();
        proceedBtn.setEnabled(false);
    }

    private void openProfilePage() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String id = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
        for (Customer c : current) if (c.getCustomerId().equals(id)) { selectedCustomer = c; break; }
        
        if (selectedCustomer != null) {
            pId.setText(selectedCustomer.getCustomerId());
            pName.setText(selectedCustomer.getName());
            pContact.setText(selectedCustomer.getContactNumber());
            pAddress.setText(selectedCustomer.getAddress() == null ? "-" : selectedCustomer.getAddress().toString());
            pLicense.setText(selectedCustomer.getDrivingLicense() == null ? "-" : selectedCustomer.getDrivingLicense().getLicenseNumber());
            pExpiry.setText(selectedCustomer.getDrivingLicense() == null || selectedCustomer.getDrivingLicense().getExpiryDate() == null ? "-" : selectedCustomer.getDrivingLicense().getExpiryDate().toString());
            cards.show(cardContainer, PAGE_PROFILE);
        }
    }

    private void openFormPage(Customer c, boolean edit) {
        isEditMode = edit;
        UiUtil.clear(formMessage);
        if (edit && c != null) {
            formTitle.setText("Edit Customer Info");
            formId.setText(c.getCustomerId()); formId.setEditable(false);
            formName.setText(c.getName()); formName.setEditable(false);
            formNationalId.setText(c.getNationalId()); formNationalId.setEditable(false);
            formLicense.setText(c.getDrivingLicense().getLicenseNumber()); formLicense.setEditable(false);
            formExpiry.setText(c.getDrivingLicense().getExpiryDate().toString()); formExpiry.setEditable(false);
            formContact.setText(c.getContactNumber());
            formAddress.setText(c.getAddress().toString());
        } else {
            formTitle.setText("Register New Customer");
            formId.setText(IdGenerator.next("CUS")); formId.setEditable(false);
            formName.setText(""); formName.setEditable(true);
            formNationalId.setText(""); formNationalId.setEditable(true);
            formLicense.setText(""); formLicense.setEditable(true);
            formExpiry.setText(LocalDate.now().plusYears(3).toString()); formExpiry.setEditable(true);
            formContact.setText(""); formAddress.setText("");
        }
        cards.show(cardContainer, PAGE_FORM);
    }

    private void saveCustomer() {
        UiUtil.clear(formMessage);
        try {
            if (isEditMode && selectedCustomer != null) {
                customerService.updateContactInfo(selectedCustomer.getCustomerId(), formContact.getText(), formAddress.getText());
                UiUtil.showOk(formMessage, "Customer updated successfully.");
                loadList(); cards.show(cardContainer, PAGE_LIST);
            } else {
                LocalDate expiry = UiUtil.parseDate(formExpiry.getText(), "Expiry");
                customerService.registerCustomer(formId.getText(), formName.getText(), formAddress.getText(),
                        formContact.getText(), formNationalId.getText(), formLicense.getText(), expiry);
                UiUtil.showOk(formMessage, "Customer registered successfully.");
                loadList(); cards.show(cardContainer, PAGE_LIST);
            }
        } catch (ValidationException | BusinessRuleException ex) { 
            UiUtil.showError(formMessage, ex.getMessage()); 
        } catch (Exception ex) {
            // This catches LvaUnavailableException safely without importing it
            UiUtil.showError(formMessage, "Error: " + ex.getMessage());
        }
    }

    @Override public void refresh() { if (PAGE_LIST.equals(cards.toString())) loadList(); }
}