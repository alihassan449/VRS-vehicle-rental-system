package com.rental.ui.auth;

import java.awt.*;
import java.util.Optional;
import javax.swing.*;
import com.rental.model.Staff;
import com.rental.service.AuthService;
import com.rental.ui.common.UiTheme;
import com.rental.ui.common.UiUtil;

public class LoginDialog extends JDialog {
    private final AuthService authService;
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JLabel message = UiUtil.messageLabel();
    private Staff result;

    private LoginDialog(AuthService authService) {
        super((Frame) null, "Login", true);
        this.authService = authService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        UiUtil.addFormRow(form, 0, "Username", userField, "Enter your staff username");
        UiUtil.addFormRow(form, 1, "Password", passField, "Enter your account password");

        JButton login = UiTheme.primaryButton("Log In");
        login.setPreferredSize(new Dimension(180, 46));
        login.addActionListener(e -> attempt());
        getRootPane().setDefaultButton(login);

        JPanel hint = new JPanel(new BorderLayout(10, 8));
        hint.setOpaque(false);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel hintTitle = new JLabel("Demo Accounts");
        hintTitle.setFont(UiTheme.LABEL_BOLD);
        hintTitle.setForeground(UiTheme.TEXT_PRIMARY);
        JLabel hintText = new JLabel("<html>Rental Staff&nbsp;&nbsp; <b>rental / rental123</b><br>"
                + "Fleet Staff&nbsp;&nbsp; <b>fleet / fleet123</b><br>"
                + "Administrator&nbsp;&nbsp; <b>admin / admin123</b></html>");
        hintText.setFont(UiTheme.LABEL_FONT);
        hintText.setForeground(UiTheme.TEXT_MUTED);
        hint.add(hintTitle, BorderLayout.NORTH);
        hint.add(hintText, BorderLayout.CENTER);

        JPanel cardContent = new JPanel(new BorderLayout(0, 14));
        cardContent.setOpaque(false);
        cardContent.add(form, BorderLayout.NORTH);
        cardContent.add(message, BorderLayout.CENTER);
        cardContent.add(hint, BorderLayout.SOUTH);

        JPanel centerWrap = new JPanel(new BorderLayout(0, 16));
        centerWrap.setBackground(new java.awt.Color(0xEAF2FF));
        centerWrap.setBorder(BorderFactory.createEmptyBorder(30, 36, 28, 36));
        centerWrap.add(UiUtil.card(cardContent, 28), BorderLayout.CENTER);
        centerWrap.add(UiUtil.flowRight(login), BorderLayout.SOUTH);

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UiTheme.BANNER_GREEN);
        banner.setBorder(BorderFactory.createEmptyBorder(22, 32, 22, 32));
        JPanel bannerText = new JPanel();
        bannerText.setOpaque(false);
        bannerText.setLayout(new BoxLayout(bannerText, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Vehicle Rental Management System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(0xFFFFFF));
        JLabel subtitle = new JLabel("Authorized Staff Login");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(0xBFDBFE));
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        bannerText.add(title);
        bannerText.add(subtitle);
        banner.add(bannerText, BorderLayout.WEST);

        userField.setPreferredSize(new Dimension(420, UiTheme.INPUT_HEIGHT));
        passField.setPreferredSize(new Dimension(420, UiTheme.INPUT_HEIGHT));
        userField.setToolTipText("Staff username");
        passField.setToolTipText("Account password");

        userField.addActionListener(e -> passField.requestFocusInWindow());
        add(banner, BorderLayout.NORTH);
        add(centerWrap, BorderLayout.CENTER);
        UiTheme.style(getContentPane());
        pack();
        setSize(720, 620);
        setMinimumSize(new Dimension(680, 560));
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void attempt() {
        Optional<Staff> staff = authService.authenticate(userField.getText(), new String(passField.getPassword()));
        if (staff.isPresent()) { result = staff.get(); dispose(); }
        else { UiUtil.showError(message, "Invalid username or password."); passField.setText(""); passField.requestFocusInWindow(); }
    }

    public static Staff showLogin(AuthService authService) {
        LoginDialog d = new LoginDialog(authService); d.setVisible(true); return d.result;
    }
}