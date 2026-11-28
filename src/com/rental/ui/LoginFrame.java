package com.rental.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.rental.model.Staff;
import com.rental.repository.StaffRepository;

/** Login window: the staff member enters username and password; on success the main window opens. */
public class LoginFrame extends JFrame {

    private final StaffRepository staffRepository;
    private final Consumer<Staff> onLogin;

    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame(StaffRepository staffRepository, String storageName, Consumer<Staff> onLogin) {
        this.staffRepository = staffRepository;
        this.onLogin = onLogin;

        setTitle("Vehicle Rental Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UiTheme.PRIMARY_DARK);
        banner.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("Vehicle Rental Management System");
        title.setFont(UiTheme.HEADER_FONT);
        title.setForeground(Color.WHITE);
        JLabel storage = new JLabel("Storage: " + storageName);
        storage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        storage.setForeground(new Color(0xCFE0F5));
        banner.add(title, BorderLayout.CENTER);
        banner.add(storage, BorderLayout.SOUTH);
        add(banner, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiTheme.BACKGROUND);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(label("Username:"), gbc);
        gbc.gridx = 1;
        form.add(usernameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(label("Password:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        JButton login = new JButton("Login");
        login.setFont(UiTheme.BUTTON_FONT);
        login.addActionListener(e -> attemptLogin());
        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(login, gbc);

        messageLabel.setForeground(new Color(0xC62828));
        messageLabel.setFont(UiTheme.LABEL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(messageLabel, gbc);

        JLabel hint = new JLabel("<html><small>Default accounts: admin / admin123, "
                + "rental / rental123, fleet / fleet123</small></html>");
        hint.setForeground(Color.GRAY);
        gbc.gridy = 4;
        form.add(hint, gbc);
        add(form, BorderLayout.CENTER);

        usernameField.setFont(UiTheme.LABEL_FONT);
        passwordField.setFont(UiTheme.LABEL_FONT);
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow()); // Enter -> password
        passwordField.addActionListener(e -> attemptLogin());                       // Enter -> login
        getRootPane().setDefaultButton(login);

        pack();
        setLocationRelativeTo(null);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.LABEL_FONT);
        return l;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            return;
        }
        Optional<Staff> staff = staffRepository.authenticate(username, password);
        if (!staff.isPresent()) {
            messageLabel.setText("Wrong username or password");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            return;
        }
        passwordField.setText("");
        messageLabel.setText(" ");
        onLogin.accept(staff.get());
    }
}
