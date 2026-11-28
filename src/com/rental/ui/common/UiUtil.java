package com.rental.ui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;

import com.rental.exception.BusinessRuleException;
import com.rental.exception.ValidationException;
import com.rental.util.Formats;

public final class UiUtil {
    public static final Color ERROR = new Color(0xEF4444);
    public static final Color OK = new Color(0x10B981);
    
    private UiUtil() {}

    public static JLabel messageLabel() {
        JLabel l = new JLabel(" "); 
        l.setFont(UiTheme.LABEL_FONT); 
        l.setForeground(ERROR); 
        return l;
    }
    
    public static void showError(JLabel label, String message) {
        label.setForeground(ERROR); 
        label.setText(message == null || message.isEmpty() ? " " : "<html><b>" + message + "</b></html>");
    }
    
    public static void showOk(JLabel label, String message) {
        label.setForeground(OK); 
        label.setText(message == null || message.isEmpty() ? " " : "<html><b>" + message + "</b></html>");
    }
    
    public static void clear(JLabel label) { 
        label.setText(" "); 
    }

    public static void info(Component p, String t, String m) { 
        JOptionPane.showMessageDialog(p, m, t, JOptionPane.INFORMATION_MESSAGE); 
    }
    
    public static void warn(Component p, String t, String m) { 
        JOptionPane.showMessageDialog(p, m, t, JOptionPane.WARNING_MESSAGE); 
    }
    
    public static void error(Component p, String t, String m) { 
        JOptionPane.showMessageDialog(p, m, t, JOptionPane.ERROR_MESSAGE); 
    }
    
    public static boolean confirm(Component p, String t, String m) { 
        return JOptionPane.showConfirmDialog(p, m, t, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; 
    }

    public static boolean run(Component p, JLabel inline, Runnable action) {
        try { 
            if (inline != null) clear(inline); 
            action.run(); 
            return true; 
        } 
        catch (ValidationException ex) { 
            if (inline != null) showError(inline, ex.getMessage()); 
            else warn(p, "Invalid data", ex.getMessage()); 
        }
        catch (BusinessRuleException ex) { 
            error(p, "Not allowed", ex.getMessage()); 
        }
        catch (RuntimeException ex) { 
            error(p, "Error", String.valueOf(ex.getMessage())); 
        }
        return false;
    }

    public static LocalDate parseDate(String text, String field) {
        try { return LocalDate.parse(text == null ? "" : text.trim()); } 
        catch (Exception ex) { throw new ValidationException(field + " must be a date (yyyy-MM-dd)."); }
    }
    
    public static LocalDateTime parseDateTime(String text, String field) {
        try { return LocalDateTime.parse(text == null ? "" : text.trim(), Formats.DATE_TIME); } 
        catch (Exception ex) { throw new ValidationException(field + " must be yyyy-MM-dd HH:mm."); }
    }
    
    public static double parseNumber(String text, String field) {
        try { return Double.parseDouble(text == null ? "" : text.trim()); } 
        catch (Exception ex) { throw new ValidationException(field + " must be a number."); }
    }
    
    public static int parseInt(String text, String field) {
        try { return Integer.parseInt(text == null ? "" : text.trim()); } 
        catch (Exception ex) { throw new ValidationException(field + " must be a whole number."); }
    }

    public static DefaultTableModel tableModel(String... cols) {
        return new DefaultTableModel(cols, 0) { 
            @Override public boolean isCellEditable(int r, int c) { return false; } 
        };
    }
    
    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m); 
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        UiTheme.styleTable(t); 
        return t;
    }
    
    public static JTextArea readOnlyArea(int r, int c) {
        JTextArea a = new JTextArea(r, c); 
        a.setEditable(false); 
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        a.setBackground(UiTheme.SURFACE); 
        a.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        return a;
    }

    public static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints(); 
        gbc.insets = new Insets(8, 8, 8, 12);
        gbc.gridy = row; gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label); 
        lbl.setFont(UiTheme.LABEL_BOLD); 
        form.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; 
        form.add(field, gbc);
    }
    
    public static void addFormRow(JPanel form, int row, String label, JComponent field, String hint) {
        GridBagConstraints gbc = new GridBagConstraints(); 
        gbc.insets = new Insets(10, 8, 2, 14);
        gbc.gridy = row * 2; gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label); 
        lbl.setFont(UiTheme.LABEL_BOLD); 
        form.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; 
        form.add(field, gbc);
        if (hint != null && !hint.isEmpty()) {
            GridBagConstraints hgbc = new GridBagConstraints(); 
            hgbc.insets = new Insets(0, 8, 8, 14);
            hgbc.gridy = row * 2 + 1; hgbc.gridx = 1; hgbc.weightx = 1; hgbc.fill = GridBagConstraints.HORIZONTAL;
            JLabel h = new JLabel(hint); 
            h.setFont(UiTheme.SMALL_FONT); 
            h.setForeground(UiTheme.TEXT_MUTED); 
            form.add(h, hgbc);
        }
    }

    public static JPanel flow(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)); 
        p.setOpaque(false); 
        for (Component c : parts) p.add(c); 
        return p;
    }
    
    public static JPanel flowRight(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8)); 
        p.setOpaque(false); 
        for (Component c : parts) p.add(c); 
        return p;
    }
    
    public static JPanel card(JComponent content, int pad) {
        JPanel p = new JPanel(new BorderLayout()); 
        p.setBackground(UiTheme.SURFACE);
        p.setBorder(new CompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1), BorderFactory.createEmptyBorder(pad, pad, pad, pad)));
        p.add(content, BorderLayout.CENTER); 
        return p;
    }
    
    public static JPanel titled(String title, JComponent content) {
        JPanel p = new JPanel(new BorderLayout(0, 12)); 
        p.setOpaque(false);
        JLabel lbl = new JLabel(title); 
        lbl.setFont(UiTheme.TITLE_FONT); 
        lbl.setForeground(UiTheme.TEXT_PRIMARY);
        p.add(lbl, BorderLayout.NORTH); 
        p.add(content, BorderLayout.CENTER); 
        return p;
    }
    
    public static JPanel pageHeader(String title, String subtitle, Component... right) {
        JPanel h = new JPanel(new BorderLayout(16, 8)); 
        h.setBackground(UiTheme.SURFACE);
        h.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER), BorderFactory.createEmptyBorder(20, 26, 20, 26)));
        JPanel left = new JPanel(); 
        left.setOpaque(false); 
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); 
        t.setFont(UiTheme.PAGE_TITLE_FONT); 
        t.setForeground(UiTheme.TEXT_PRIMARY); 
        left.add(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel s = new JLabel(subtitle); 
            s.setFont(UiTheme.SUBTITLE_FONT); 
            s.setForeground(UiTheme.TEXT_MUTED); 
            left.add(s);
        }
        h.add(left, BorderLayout.WEST);
        if (right != null && right.length > 0) h.add(flowRight(right), BorderLayout.EAST);
        return h;
    }
    
    public static JPanel kpiCard(String label, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)); 
        card.setBackground(UiTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER, 1), BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        JLabel t = new JLabel(label); 
        t.setFont(UiTheme.LABEL_FONT); 
        t.setForeground(UiTheme.TEXT_MUTED);
        value.setFont(UiTheme.KPI_FONT); 
        value.setForeground(UiTheme.TEXT_PRIMARY);
        card.add(t, BorderLayout.NORTH); 
        card.add(value, BorderLayout.CENTER);
        JPanel bar = new JPanel(); 
        bar.setBackground(accent); 
        bar.setPreferredSize(new Dimension(10, 4)); 
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }
    
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(" " + text + " "); 
        l.setOpaque(true); 
        l.setBackground(bg); 
        l.setForeground(fg);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13)); 
        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8)); 
        return l;
    }
    
    public static JScrollPane scrollable(JComponent content) {
        JScrollPane sp = new JScrollPane(content); 
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(20); 
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); 
        return sp;
    }
    
    public static void onChange(Runnable cb, JComponent... comps) {
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { cb.run(); }
        };
        for (JComponent c : comps) {
            if (c instanceof javax.swing.text.JTextComponent) ((javax.swing.text.JTextComponent) c).getDocument().addDocumentListener(dl);
            else if (c instanceof JComboBox) ((JComboBox<?>) c).addActionListener(e -> cb.run());
            else if (c instanceof AbstractButton) ((AbstractButton) c).addActionListener(e -> cb.run());
        }
    }
    
    public static void showPrintPreview(Component parent, String title, String text) {
        JTextArea area = readOnlyArea(26, 56); 
        area.setText(text); 
        area.setCaretPosition(0);
        JButton print = UiTheme.primaryButton("Print Document"); 
        JButton close = UiTheme.secondaryButton("Close");
        Window owner = SwingUtilities.getWindowAncestor(parent); 
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        print.addActionListener(e -> { if (printText(dialog, title, area)) info(dialog, "Printed", "Document sent to printer."); });
        close.addActionListener(e -> dialog.dispose());
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); 
        btns.add(print); 
        btns.add(close);
        dialog.add(btns, BorderLayout.SOUTH); 
        UiTheme.style(dialog.getContentPane());
        dialog.pack(); 
        dialog.setMinimumSize(new Dimension(760, 520)); 
        dialog.setLocationRelativeTo(parent); 
        dialog.setVisible(true);
    }
    
    public static boolean printText(Component p, String t, JTextArea a) {
        try { return a.print(new java.text.MessageFormat(t), null, true, null, null, true); } 
        catch (Exception ex) { error(p, "Printer Error", "Printing failed: " + ex.getMessage()); return false; }
    }
    
    public static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        public PlaceholderTextField(int columns, String placeholder) {
            super(columns);
            this.placeholder = placeholder;
            setToolTipText(placeholder);
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                java.awt.Insets insets = getInsets();
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(UiTheme.TEXT_MUTED);
                g2.setFont(getFont().deriveFont(java.awt.Font.PLAIN));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, insets.left, y);
                g2.dispose();
            }
        }
    }
}