package com.rental.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Small helpers shared by all Swing panels: input parsing, message dialogs, form layout. */
public final class FormUtil {

    public static final String ANY = "(any)";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private FormUtil() {
    }

    public static LocalDate parseDate(String text, String fieldName) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(fieldName + " must be a date in yyyy-MM-dd format (e.g. 2026-12-31)");
        }
    }

    /** Blank text means "now" and is returned as null. */
    public static LocalDateTime parseDateTimeOrNull(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be in yyyy-MM-dd HH:mm format "
                    + "(e.g. 2026-12-31 14:30) or left blank for the current time");
        }
    }

    public static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
    }

    public static int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number");
        }
    }

    public static String[] withAny(Object[] values) {
        String[] result = new String[values.length + 1];
        result[0] = ANY;
        for (int i = 0; i < values.length; i++) {
            result[i + 1] = values[i].toString();
        }
        return result;
    }

    public static String[] names(Object[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].toString();
        }
        return result;
    }

    public static void showError(Component parent, Exception ex) {
        String title = ex instanceof SecurityException ? "Not authorized" : "Error";
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows printable text (invoice, receipt, report) in a monospaced scrollable dialog. */
    public static void showText(Component parent, String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(620, 420));
        JOptionPane.showMessageDialog(parent, scroll, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        form.add(field, gbc);
    }

    /**
     * Lays out label + field pairs in several columns, so a form with many fields stays short
     * (a one-column form pushes the table off the screen). Usage:
     * <pre>Grid g = new Grid(form, 3); g.add("Name:", nameField); ... g.addButtons(panel);</pre>
     */
    public static final class Grid {
        private final JPanel form;
        private final int columns;
        private final GridBagConstraints gbc = new GridBagConstraints();
        private int cell = 0;
        private int row = 0;

        public Grid(JPanel form, int columns) {
            this.form = form;
            this.columns = columns;
            form.setLayout(new GridBagLayout());
            gbc.insets = new Insets(3, 6, 3, 6);
            gbc.anchor = GridBagConstraints.WEST;
        }

        /** Adds "label field" in the next free cell. */
        public void add(String label, JComponent field) {
            gbc.gridwidth = 1;
            gbc.gridx = cell * 2;
            gbc.gridy = row;
            form.add(new JLabel(label), gbc);
            gbc.gridx = cell * 2 + 1;
            form.add(field, gbc);
            nextCell();
        }

        /** Adds a component (e.g. a button) without label in the next free cell. */
        public void add(JComponent component) {
            gbc.gridwidth = 2;
            gbc.gridx = cell * 2;
            gbc.gridy = row;
            form.add(component, gbc);
            gbc.gridwidth = 1;
            nextCell();
        }

        /** Starts a new row (use after the last field of a group). */
        public void newRow() {
            if (cell != 0) {
                cell = 0;
                row++;
            }
        }

        /** Adds a panel of buttons that uses a whole row. */
        public void addButtons(JComponent buttons) {
            newRow();
            gbc.gridwidth = columns * 2;
            gbc.gridx = 0;
            gbc.gridy = row;
            form.add(buttons, gbc);
            gbc.gridwidth = 1;
            row++;
        }

        private void nextCell() {
            cell++;
            if (cell >= columns) {
                cell = 0;
                row++;
            }
        }
    }
}
