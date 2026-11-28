package com.rental;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.rental.model.Staff;
import com.rental.repository.sqlserver.SqlServerConfig;
import com.rental.ui.LoginFrame;
import com.rental.ui.MainFrame;
import com.rental.ui.UiTheme;

/**
 * Application entry point (GUI part).
 * 1. Connects to SQL Server using db.properties (project root folder). If that is not possible the
 *    user can choose to continue in memory (data is lost on exit) or to exit.
 * 2. Shows the login window; after a successful login the main window opens with only the tabs
 *    allowed for that staff member. Logout returns to the login window.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.applyLookAndFeel();
            AppContext context = createContext();
            if (context != null) {
                showLogin(context);
            }
        });
    }

    private static AppContext createContext() {
        try {
            return AppContext.sqlServer(SqlServerConfig.load("db.properties"));
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.toString() : e.getMessage();
            Object[] options = {"Continue without database", "Exit"};
            int choice = JOptionPane.showOptionDialog(null,
                    "The SQL Server database could not be used:\n\n" + reason
                            + "\n\nSee README_DATABASE.md. You can continue in memory (demo only - "
                            + "everything is lost when the program closes).",
                    "Database not available", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[1]);
            return choice == 0 ? AppContext.inMemory() : null;
        }
    }

    private static void showLogin(final AppContext context) {
        final LoginFrame[] login = new LoginFrame[1];
        login[0] = new LoginFrame(context.getStaffRepository(), context.getStorageName(), (Staff staff) -> {
            login[0].setVisible(false);
            new MainFrame(context.getController(), staff, context.getStorageName(),
                    () -> login[0].setVisible(true)).setVisible(true);
        });
        login[0].setVisible(true);
    }
}
