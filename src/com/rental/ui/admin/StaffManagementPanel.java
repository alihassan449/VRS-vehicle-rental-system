package com.rental.ui.admin;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rental.enums.StaffRole;
import com.rental.model.Staff;
import com.rental.service.AuthService;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;
import com.rental.util.IdGenerator;

/** Administrator staff account management. */
public class StaffManagementPanel extends JPanel {
    private final AuthService authService;

    private final JTextField idField = new JTextField(18);
    private final UiUtil.PlaceholderTextField nameField = new UiUtil.PlaceholderTextField(18, "e.g. Muhammad Ahmed");
    private final UiUtil.PlaceholderTextField usernameField = new UiUtil.PlaceholderTextField(18, "e.g. ahmed.staff");
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JComboBox<StaffRole> roleBox = new JComboBox<>(
            new StaffRole[]{StaffRole.RENTAL_STAFF, StaffRole.FLEET_STAFF});
    private final JLabel message = UiUtil.messageLabel();

    private final DefaultTableModel model = UiUtil.tableModel("Staff ID", "Name", "Username", "Role");
    private final JTable table = UiUtil.table(model);

    public static final String PAGE_DIRECTORY = "STAFF_DIRECTORY";
    public static final String PAGE_CREATE = "STAFF_CREATE";
    private final CardLayout cards = new CardLayout();
    private final JPanel cardContainer = new JPanel(cards);

    public StaffManagementPanel(AuthService authService) {
        this.authService = authService;
        setLayout(new BorderLayout());
        cardContainer.add(buildDirectoryPage(), PAGE_DIRECTORY);
        cardContainer.add(buildCreatePage(), PAGE_CREATE);
        add(cardContainer, BorderLayout.CENTER);
        clearForm();
        refresh();
        UiTheme.style(this);
    }

    private JPanel buildDirectoryPage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton createBtn = UiTheme.primaryButton("+ Add Staff");
        createBtn.addActionListener(e -> showCreatePage());
        JButton deleteBtn = UiTheme.dangerButton("Delete Selected Staff");
        deleteBtn.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> deleteBtn.setEnabled(table.getSelectedRow() >= 0));
        deleteBtn.addActionListener(e -> deleteSelectedStaff());

        JPanel staffList = new JPanel(new BorderLayout(0, 10));
        staffList.setOpaque(false);
        JLabel title = new JLabel("Registered Staff");
        title.setFont(UiTheme.TITLE_FONT);
        staffList.add(title, BorderLayout.NORTH);
        staffList.add(new JScrollPane(table), BorderLayout.CENTER);
        staffList.add(UiUtil.flowRight(deleteBtn), BorderLayout.SOUTH);

        page.add(UiUtil.pageHeader("Staff Management",
                "Create and maintain Rental Staff and Fleet Staff accounts · SDA Staff Administration", createBtn), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));
        body.add(UiUtil.card(staffList, 14), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCreatePage() {
        JPanel page = new JPanel(new BorderLayout());
        JButton backBtn = UiTheme.secondaryButton("< Back to Staff Directory");
        backBtn.addActionListener(e -> cards.show(cardContainer, PAGE_DIRECTORY));
        page.add(UiUtil.pageHeader("Create Staff Account",
                "Enter account details and choose a staff role.", backBtn), BorderLayout.NORTH);

        JButton saveBtn = UiTheme.successButton("Save Staff Account");
        saveBtn.addActionListener(e -> saveStaff());
        JButton clearBtn = UiTheme.secondaryButton("Clear");
        clearBtn.addActionListener(e -> clearForm());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        UiUtil.addFormRow(form, 0, "Staff ID", idField, "Generated automatically");
        UiUtil.addFormRow(form, 1, "Full Name", nameField, "Employee display name");
        UiUtil.addFormRow(form, 2, "Username", usernameField, "Unique login username");
        passwordField.setToolTipText("Enter at least 6 characters");
        UiUtil.addFormRow(form, 3, "Password", passwordField, "Use at least 6 characters");
        UiUtil.addFormRow(form, 4, "Staff Role", roleBox, "Rental Staff or Fleet Staff");

        JPanel actions = new JPanel(new BorderLayout(12, 8));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        actions.add(message, BorderLayout.WEST);
        actions.add(UiUtil.flowRight(backBtn, clearBtn, saveBtn), BorderLayout.EAST);

        JPanel formWrap = new JPanel(new BorderLayout(0, 18));
        formWrap.setOpaque(false);
        formWrap.add(form, BorderLayout.CENTER);
        formWrap.add(actions, BorderLayout.SOUTH);

        JPanel card = UiUtil.card(formWrap, 24);
        card.setPreferredSize(new java.awt.Dimension(760, 520));
        card.setMinimumSize(new java.awt.Dimension(620, 480));
        card.setMaximumSize(new java.awt.Dimension(820, 620));
        JPanel center = new JPanel(new java.awt.GridBagLayout());
        center.setOpaque(false);
        center.add(card);

        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new java.awt.Dimension(260, 0));

        JLabel sidebarTitle = new JLabel("Staff Pages");
        sidebarTitle.setFont(UiTheme.TITLE_FONT);
        sidebarTitle.setBorder(BorderFactory.createEmptyBorder(8, 12, 14, 12));
        sidebar.add(sidebarTitle);

        JButton directoryLink = UiTheme.secondaryButton("Staff Directory");
        JButton createLink = UiTheme.primaryButton("Create Staff");
        directoryLink.setMaximumSize(new java.awt.Dimension(230, UiTheme.BUTTON_HEIGHT));
        createLink.setMaximumSize(new java.awt.Dimension(230, UiTheme.BUTTON_HEIGHT));
        directoryLink.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        createLink.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        directoryLink.addActionListener(e -> cards.show(cardContainer, PAGE_DIRECTORY));
        sidebar.add(javax.swing.Box.createVerticalStrut(8));
        sidebar.add(directoryLink);
        sidebar.add(javax.swing.Box.createVerticalStrut(10));
        sidebar.add(createLink);

        JPanel body = new JPanel(new java.awt.BorderLayout(18, 0));
        body.setBorder(BorderFactory.createEmptyBorder(22, 26, 26, 26));
        body.add(UiUtil.card(sidebar, 14), java.awt.BorderLayout.WEST);
        body.add(center, java.awt.BorderLayout.CENTER);
        page.add(UiUtil.scrollable(body), BorderLayout.CENTER);

        return page;
    }

    public void showCreatePage() {
        clearForm();
        cards.show(cardContainer, PAGE_CREATE);
    }

    private void clearForm() {
        idField.setText(IdGenerator.next("STF"));
        idField.setEditable(false);
        nameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        roleBox.setSelectedItem(StaffRole.RENTAL_STAFF);
        UiUtil.clear(message);
    }

    private void saveStaff() {
        UiUtil.clear(message);
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            UiUtil.showError(message, "Name, username, and password are required.");
            return;
        }

        if (password.length() < 6) {
            UiUtil.showError(message, "Password must contain at least 6 characters.");
            return;
        }

        if (authService.usernameExists(username)) {
            UiUtil.showError(message, "That username already exists. Choose a unique username.");
            return;
        }

        StaffRole role = (StaffRole) roleBox.getSelectedItem();
        authService.addUser(new Staff(idField.getText(), name, username, password, role), password);
        UiUtil.showOk(message, "Staff account saved successfully.");
        clearForm();
        refresh();
    }

    private void deleteSelectedStaff() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        String staffId = (String) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 1);
        Staff staff = authService.getUsers().stream()
                .filter(s -> s.getStaffId().equals(staffId)).findFirst().orElse(null);
        if (staff == null) return;
        if (staff.getRole() == StaffRole.ADMIN) {
            UiUtil.showError(message, "Administrator accounts cannot be deleted.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete Staff", "Delete staff account \"" + name + "\"?")) return;
        authService.deleteUser(staff);
        UiUtil.showOk(message, "Staff account deleted successfully.");
        refresh();
    }

    public void refresh() {
        model.setRowCount(0);
        List<Staff> users = authService.getUsers();
        for (Staff staff : users) {
            model.addRow(new Object[]{staff.getStaffId(), staff.getName(), staff.getUsername(), staff.getRole().toString()});
        }
    }
}
