package com.rental.ui.common;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public final class UiTheme {
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_HOVER = new Color(0x1D4ED8);
    public static final Color PRIMARY_DARK = new Color(0x1E3A8A);
    public static final Color SUCCESS = new Color(0x059669);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color BACKGROUND = new Color(0xF1F5F9);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xCBD5E1);
    public static final Color TEXT_PRIMARY = new Color(0x0F172A);
    public static final Color TEXT_MUTED = new Color(0x64748B);
    public static final Color TABLE_ALT_ROW = new Color(0xF8FAFC);
    public static final Color TABLE_SELECT = new Color(0xDBEAFE);
    public static final Color SIDEBAR_HOVER = new Color(0xEFF6FF);
    public static final Color BANNER_GREEN = new Color(0x1E293B);
    public static final Color BANNER_SUBTITLE = new Color(0xBFDBFE);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font PAGE_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font LABEL_BOLD = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 13); // <--- RESTORED HERE
    public static final Font KPI_FONT = new Font("Segoe UI", Font.BOLD, 32);

    public static final int INPUT_HEIGHT = 42;
    public static final int BUTTON_HEIGHT = 42;
    public static final int TABLE_ROW_HEIGHT = 48;
    private static final String VARIANT_PROP = "ui.variant";

    public enum ButtonVariant { PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING }
    private UiTheme() {}

    public static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
        } catch (Exception ignored) {}
    }

    public static JButton primaryButton(String text) { return makeButton(text, ButtonVariant.PRIMARY); }
    public static JButton secondaryButton(String text) { return makeButton(text, ButtonVariant.SECONDARY); }
    public static JButton successButton(String text) { return makeButton(text, ButtonVariant.SUCCESS); }
    public static JButton dangerButton(String text) { return makeButton(text, ButtonVariant.DANGER); }
    public static JButton warningButton(String text) { return makeButton(text, ButtonVariant.WARNING); }

    public static JButton makeButton(String text, ButtonVariant variant) {
        JButton b = new JButton(text);
        b.putClientProperty(VARIANT_PROP, variant);
        b.setFont(BUTTON_FONT);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        b.setUI(new RoundedButtonUI(variant));
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(d.width, 120), BUTTON_HEIGHT));
        return b;
    }

    public static void applyButtonVariant(JButton button, ButtonVariant variant) {
        button.putClientProperty(VARIANT_PROP, variant);
        button.repaint();
    }

    public static void style(Container root) {
        if (root instanceof JComponent && ((JComponent) root).getClientProperty("ui.preserveBg") == null && root.isOpaque()) {
            root.setBackground(BACKGROUND);
        }
        styleRecursive(root);
    }

    private static void styleRecursive(Component comp) {
        if (comp instanceof JButton) {
            JButton b = (JButton) comp;
            if (b.getClientProperty(VARIANT_PROP) == null) {
                b.setUI(new RoundedButtonUI(ButtonVariant.SECONDARY));
                b.setFont(BUTTON_FONT);
                b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            }
        } else if (comp instanceof JLabel) {
            JLabel lbl = (JLabel) comp;
            if (lbl.getClientProperty("ui.preserveFont") == null) lbl.setFont(LABEL_FONT);
        } else if (comp instanceof JTable) {
            styleTable((JTable) comp);
        } else if (comp instanceof JTextField || comp instanceof JPasswordField) {
            JTextField f = (JTextField) comp;
            f.setFont(LABEL_FONT);
            f.setMargin(new Insets(8, 12, 8, 12));
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            f.setPreferredSize(new Dimension(Math.max(f.getPreferredSize().width, 220), INPUT_HEIGHT));
        } else if (comp instanceof JComboBox) {
            JComboBox<?> c = (JComboBox<?>) comp;
            c.setFont(LABEL_FONT);
            c.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            c.setPreferredSize(new Dimension(Math.max(c.getPreferredSize().width, 180), INPUT_HEIGHT));
        } else if (comp instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) comp;
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        } else if (comp instanceof JPanel && ((JPanel) comp).isOpaque()) {
            JPanel p = (JPanel) comp;
            if (p.getClientProperty("ui.preserveBg") == null) p.setBackground(BACKGROUND);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) styleRecursive(child);
        }
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setFont(LABEL_FONT);
        table.setSelectionBackground(TABLE_SELECT);
        table.setSelectionForeground(PRIMARY_DARK);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        JTableHeader header = table.getTableHeader();
        header.setFont(LABEL_BOLD);
        header.setBackground(SURFACE);
        header.setForeground(TEXT_MUTED);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 44));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel) ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? SURFACE : TABLE_ALT_ROW);
                    c.setForeground(TEXT_PRIMARY);
                } else {
                    c.setBackground(TABLE_SELECT); c.setForeground(PRIMARY_DARK);
                }
                return c;
            }
        });
    }

    private static class RoundedButtonUI extends BasicButtonUI {
        private boolean hovered = false;
        RoundedButtonUI(ButtonVariant variant) { }

        @Override public void installUI(JComponent c) {
            super.installUI(c);
            c.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; c.repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; c.repaint(); }
            });
        }

        @Override public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonVariant v = (ButtonVariant) b.getClientProperty(VARIANT_PROP);
            if (v == null) v = ButtonVariant.SECONDARY;
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = b.getWidth(), h = b.getHeight();
            boolean pressed = b.getModel().isArmed() && b.getModel().isPressed();
            Color bg, fg; Color border = null;

            if (!b.isEnabled()) { bg = BACKGROUND; fg = TEXT_MUTED; border = BORDER; }
            else {
                switch (v) {
                    case PRIMARY: bg = pressed ? PRIMARY_DARK : (hovered ? PRIMARY_HOVER : PRIMARY); fg = Color.WHITE; break;
                    case SUCCESS: bg = pressed ? new Color(0x059669) : SUCCESS; fg = Color.WHITE; break;
                    case DANGER: bg = pressed ? new Color(0xDC2626) : DANGER; fg = Color.WHITE; break;
                    case WARNING: bg = pressed ? new Color(0xD97706) : WARNING; fg = Color.WHITE; break;
                    default: bg = pressed ? BORDER : (hovered ? SIDEBAR_HOVER : SURFACE); fg = TEXT_PRIMARY; border = BORDER; break;
                }
            }
            g2.setColor(bg); g2.fillRoundRect(0, 0, w, h, 8, 8);
            if (border != null) { g2.setColor(border); g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8); }
            g2.dispose();
            b.setForeground(fg);
            super.paint(g, c);
        }
    }
}