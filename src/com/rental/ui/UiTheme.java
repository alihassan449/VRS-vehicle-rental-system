package com.rental.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Look and feel of the application (colors, fonts, tables) and keyboard navigation:
 * pressing Enter or Down Arrow in a text field moves to the next field (Enter also in
 * combo boxes), so forms can be filled without the mouse.
 * Written for Java 11 (no pattern-matching instanceof).
 */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x1565C0);
    public static final Color PRIMARY_DARK = new Color(0x0D47A1);
    public static final Color BACKGROUND = new Color(0xF5F7FA);
    public static final Color TABLE_ALT_ROW = new Color(0xEAF1FB);

    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 20);

    private UiTheme() {
    }

    /** Switch to the Nimbus look and feel (call before creating any component). */
    public static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception ignored) {
            // keep the default look and feel
        }
    }

    /** Style a whole panel tree and enable keyboard navigation. Call at the end of each panel constructor. */
    public static void style(Container root) {
        root.setBackground(BACKGROUND);
        styleRecursive(root);
    }

    private static void styleRecursive(Component comp) {
        if (comp instanceof JButton) {
            JButton button = (JButton) comp;
            button.setFont(BUTTON_FONT);
            button.setFocusPainted(false);
            button.setForeground(PRIMARY_DARK);
            button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (comp instanceof JLabel) {
            ((JLabel) comp).setFont(LABEL_FONT);
        } else if (comp instanceof JTable) {
            styleTable((JTable) comp);
        } else if (comp instanceof JTextField) {
            JTextField field = (JTextField) comp;
            field.setFont(LABEL_FONT);
            forwardOnKey(field, KeyEvent.VK_ENTER);
            forwardOnKey(field, KeyEvent.VK_DOWN);
        } else if (comp instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) comp;
            combo.setFont(LABEL_FONT);
            forwardOnKey(combo, KeyEvent.VK_ENTER); // Down Arrow is needed to open the list
        } else if (comp instanceof JCheckBox) {
            JCheckBox box = (JCheckBox) comp;
            box.setFont(LABEL_FONT);
            forwardOnKey(box, KeyEvent.VK_ENTER);
            forwardOnKey(box, KeyEvent.VK_DOWN);
        }

        if (comp instanceof JComponent) {
            JComponent jc = (JComponent) comp;
            if (jc.getBorder() instanceof TitledBorder) {
                TitledBorder tb = (TitledBorder) jc.getBorder();
                tb.setTitleFont(TITLE_FONT);
                tb.setTitleColor(PRIMARY_DARK);
            }
        }

        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                styleRecursive(child);
            }
        }
    }

    private static void forwardOnKey(final JComponent comp, final int keyCode) {
        comp.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == keyCode) {
                    e.consume();
                    comp.transferFocus();
                }
            }
        });
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(24);
        table.setFont(LABEL_FONT);
        table.setSelectionBackground(PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(0xDDDDDD));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(PRIMARY_DARK);
        header.setForeground(Color.WHITE);
        header.setOpaque(true);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_ALT_ROW);
                }
                return c;
            }
        });
    }
}
