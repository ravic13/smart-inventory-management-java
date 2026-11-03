import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Enterprise Design System & Reusable High-Contrast UI Styler
 * Provides solid, high-visibility button rendering, anti-aliased typography,
 * alternating table striping, and consistent enterprise contrast across the system.
 */
public class UIHelper {

    // =========================================================================
    // 1. SPECIFIED ENTERPRISE COLOR PALETTE
    // =========================================================================
    public static final Color COLOR_PRIMARY     = new Color(37, 99, 235);   // Blue #2563EB (Primary, Login, Search)
    public static final Color COLOR_SUCCESS     = new Color(22, 163, 74);   // Green #16A34A (Add, Create, Save, Confirm)
    public static final Color COLOR_EDIT        = new Color(13, 148, 136);  // Teal #0D9488 (Edit, Update)
    public static final Color COLOR_WARNING     = new Color(217, 119, 6);   // Amber #D97706 (Warning actions)
    public static final Color COLOR_DANGER      = new Color(220, 38, 38);   // Red #DC2626 (Delete, Remove, Logout)
    public static final Color COLOR_SECONDARY   = new Color(71, 85, 105);   // Slate #475569 (Cancel, Back, Refresh)
    public static final Color COLOR_CLEAR_BG    = new Color(226, 232, 240); // Light Gray #E2E8F0 (Clear, Reset)
    public static final Color COLOR_CLEAR_FG    = new Color(15, 23, 42);    // Dark Slate #0F172A
    public static final Color COLOR_NAV_BG      = new Color(30, 41, 59);    // Dark Navy #1E293B (Sidebar unselected)
    public static final Color COLOR_NAV_ACTIVE  = new Color(37, 99, 235);   // Blue #2563EB (Sidebar active)
    public static final Color COLOR_CYAN_ACCENT = new Color(56, 189, 248);  // Cyan #38BDF8

    // Canvas & Neutral Layout Colors
    public static final Color NAVY_SIDEBAR      = new Color(15, 23, 42);    // #0F172A
    public static final Color NAVY_SURFACE      = new Color(30, 41, 59);    // #1E293B
    public static final Color BG_LIGHT          = new Color(241, 245, 249); // #F1F5F9
    public static final Color TEXT_NAVY         = new Color(15, 23, 42);    // #0F172A
    public static final Color TEXT_MUTED        = new Color(71, 85, 105);   // #475569
    public static final Color TEXT_GRAY         = new Color(100, 116, 139); // #64748B
    public static final Color BORDER_LIGHT      = new Color(226, 232, 240); // #E2E8F0
    public static final Color BORDER_STRONG     = new Color(203, 213, 225); // #CBD5E1

    // Backward-compatible color aliases
    public static final Color BLUE_PRIMARY      = COLOR_PRIMARY;
    public static final Color GREEN_SUCCESS     = COLOR_SUCCESS;
    public static final Color RED_DANGER        = COLOR_DANGER;
    public static final Color ORANGE_WARNING    = COLOR_WARNING;

    // Fonts - Crisp, Modern & Enterprise Consistent (Supporting both Latin and full Emoji rendering)
    public static final Font FONT_HEADER        = new Font("Segoe UI Emoji", Font.BOLD, 22);
    public static final Font FONT_SUBHEADER     = new Font("Segoe UI Emoji", Font.BOLD, 17);
    public static final Font FONT_BTN_BOLD      = new Font("Segoe UI Emoji", Font.BOLD, 14);
    public static final Font FONT_BODY_BOLD     = new Font("Segoe UI Emoji", Font.BOLD, 14);
    public static final Font FONT_BODY          = new Font("Segoe UI Emoji", Font.PLAIN, 14);
    public static final Font FONT_SMALL         = new Font("Segoe UI Emoji", Font.PLAIN, 12);

    // =========================================================================
    // 2. SOLID HIGH-CONTRAST BUTTON UI DELEGATE
    // Guarantees that no OS Look and Feel (e.g. Windows native) can wash out or override button colors.
    // =========================================================================
    public static class SolidButtonUI extends BasicButtonUI {
        private final int cornerRadius;

        public SolidButtonUI(int cornerRadius) {
            this.cornerRadius = cornerRadius;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = b.getWidth();
            int height = b.getHeight();

            Color bg = b.getBackground();
            if (!b.isEnabled()) {
                bg = new Color(226, 232, 240); // disabled background
            }

            // Fill solid background
            g2.setColor(bg);
            if (cornerRadius > 0) {
                g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
            } else {
                g2.fillRect(0, 0, width, height);
            }

            // Paint distinct border / focus ring
            if (b.hasFocus() && b.isEnabled()) {
                g2.setColor(new Color(96, 165, 250)); // #60A5FA focus highlight ring
                g2.setStroke(new BasicStroke(2.0f));
                if (cornerRadius > 0) {
                    g2.drawRoundRect(1, 1, width - 2, height - 2, cornerRadius, cornerRadius);
                } else {
                    g2.drawRect(1, 1, width - 2, height - 2);
                }
            } else {
                Color border = getBorderColor(bg);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                if (cornerRadius > 0) {
                    g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
                } else {
                    g2.drawRect(0, 0, width - 1, height - 1);
                }
            }

            g2.dispose();
            super.paint(g, c);
        }

        @Override
        protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (b.isEnabled()) {
                g2.setColor(b.getForeground());
            } else {
                g2.setColor(new Color(148, 163, 184)); // legible muted disabled text
            }

            super.paintText(g2, c, textRect, text);
            g2.dispose();
        }
    }

    // =========================================================================
    // 3. REUSABLE BUTTON STYLING (State Management & Factory Methods)
    // =========================================================================

    /**
     * Styles an existing JButton with specified background and foreground colors.
     * Guarantees solid background painting, hover effect, pressed effect, and hand cursor.
     */
    public static JButton styleButton(JButton btn, Color background, Color foreground) {
        return styleButton(btn, background, foreground, FONT_BTN_BOLD, new Insets(8, 18, 8, 18));
    }

    /**
     * Full parametric button styler supporting custom fonts and padding
     */
    public static JButton styleButton(JButton btn, Color background, Color foreground, Font font, Insets pad) {
        btn.setFont(font);
        btn.setBackground(background);
        btn.setForeground(foreground);
        btn.setUI(new SolidButtonUI(6));
        btn.setBorder(new EmptyBorder(pad.top, pad.left, pad.bottom, pad.right));

        Color hoverColor = getHoverColor(background);
        Color pressedColor = getPressedColor(background);

        // Interactive States: Hover, Pressed, Released
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverColor);
                    btn.repaint();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(background);
                    btn.repaint();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(pressedColor);
                    btn.repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (btn.isEnabled()) {
                    if (btn.contains(e.getPoint())) {
                        btn.setBackground(hoverColor);
                    } else {
                        btn.setBackground(background);
                    }
                    btn.repaint();
                }
            }
        });

        // Repaint on focus change for instant visual feedback
        btn.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { btn.repaint(); }
            @Override
            public void focusLost(FocusEvent e) { btn.repaint(); }
        });

        return btn;
    }

    /**
     * Derives a slightly darker shade for the hover state
     */
    public static Color getHoverColor(Color c) {
        int r = Math.max(0, c.getRed() - 25);
        int g = Math.max(0, c.getGreen() - 25);
        int b = Math.max(0, c.getBlue() - 25);
        return new Color(r, g, b);
    }

    /**
     * Derives a visibly darker shade for the pressed state
     */
    public static Color getPressedColor(Color c) {
        int r = Math.max(0, c.getRed() - 48);
        int g = Math.max(0, c.getGreen() - 48);
        int b = Math.max(0, c.getBlue() - 48);
        return new Color(r, g, b);
    }

    /**
     * Derives a crisp border outline color
     */
    public static Color getBorderColor(Color c) {
        int r = Math.max(0, c.getRed() - 36);
        int g = Math.max(0, c.getGreen() - 36);
        int b = Math.max(0, c.getBlue() - 36);
        return new Color(r, g, b);
    }

    // =========================================================================
    // 4. ACTION-SPECIFIC STYLING & FACTORY METHODS
    // =========================================================================

    public static JButton stylePrimaryButton(JButton btn) {
        return styleButton(btn, COLOR_PRIMARY, Color.WHITE);
    }
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        return stylePrimaryButton(btn);
    }

    public static JButton styleSuccessButton(JButton btn) {
        return styleButton(btn, COLOR_SUCCESS, Color.WHITE);
    }
    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        return styleSuccessButton(btn);
    }

    public static JButton styleEditButton(JButton btn) {
        return styleButton(btn, COLOR_EDIT, Color.WHITE);
    }
    public static JButton createEditButton(String text) {
        JButton btn = new JButton(text);
        return styleEditButton(btn);
    }

    public static JButton styleWarningButton(JButton btn) {
        return styleButton(btn, COLOR_WARNING, Color.WHITE);
    }
    public static JButton createWarningButton(String text) {
        JButton btn = new JButton(text);
        return styleWarningButton(btn);
    }

    public static JButton styleDangerButton(JButton btn) {
        return styleButton(btn, COLOR_DANGER, Color.WHITE);
    }
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        return styleDangerButton(btn);
    }

    public static JButton styleSecondaryButton(JButton btn) {
        return styleButton(btn, COLOR_SECONDARY, Color.WHITE);
    }
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        return styleSecondaryButton(btn);
    }

    public static JButton styleClearButton(JButton btn) {
        return styleButton(btn, COLOR_CLEAR_BG, COLOR_CLEAR_FG, FONT_BTN_BOLD, new Insets(8, 18, 8, 18));
    }
    public static JButton createClearButton(String text) {
        JButton btn = new JButton(text);
        return styleClearButton(btn);
    }

    // =========================================================================
    // 5. TABLE STYLING WITH GUARANTEED CONTRAST & ALTERNATING ROWS
    // =========================================================================

    /**
     * Styles a JTable with solid navy header (even on Windows L&F),
     * alternating row striping, and high-contrast selected rows.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(FONT_BODY);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(COLOR_PRIMARY);
        table.setSelectionForeground(Color.WHITE);

        // Custom Header Renderer to overcome Windows Look and Feel native override
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new TableCellRenderer() {
            private final DefaultTableCellRenderer delegate = new DefaultTableCellRenderer();
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                JLabel l = (JLabel) delegate.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                l.setOpaque(true);
                l.setBackground(NAVY_SURFACE); // #1E293B
                l.setForeground(Color.WHITE);
                l.setFont(FONT_BODY_BOLD);
                l.setBorder(new EmptyBorder(10, 14, 10, 14));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        // Alternating row renderer with legible dark text
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(COLOR_PRIMARY); // #2563EB
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_NAVY);
                }
                setBorder(new EmptyBorder(0, 14, 0, 14));
                return c;
            }
        });
    }

    // =========================================================================
    // 6. FORM CONTROLS & INPUT STYLING
    // =========================================================================

    /**
     * Styles a text field with clean white background, high-contrast dark text,
     * and royal blue focus highlight border.
     */
    public static JTextField styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_NAVY);
        field.setCaretColor(TEXT_NAVY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_PRIMARY, 2),
                        new EmptyBorder(5, 11, 5, 11)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_STRONG, 1),
                        new EmptyBorder(6, 12, 6, 12)
                ));
            }
        });
        return field;
    }

    /**
     * Styles a password field with high-contrast text and blue focus ring.
     */
    public static JPasswordField stylePasswordField(JPasswordField field) {
        field.setFont(FONT_BODY);
        field.setBackground(Color.WHITE);
        field.setForeground(TEXT_NAVY);
        field.setCaretColor(TEXT_NAVY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_PRIMARY, 2),
                        new EmptyBorder(5, 11, 5, 11)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_STRONG, 1),
                        new EmptyBorder(6, 12, 6, 12)
                ));
            }
        });
        return field;
    }

    /**
     * Creates a high-contrast search icon with guaranteed emoji font
     */
    public static JLabel createSearchIcon() {
        JLabel icon = new JLabel("\uD83D\uDD0D");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        icon.setForeground(TEXT_GRAY);
        return icon;
    }

    /**
     * Creates an input search field with high-contrast border
     */
    public static JTextField createSearchField(String placeholder) {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(280, 38));
        return styleTextField(field);
    }

    /**
     * Creates a crisp card panel with subtle border
     */
    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                new EmptyBorder(18, 20, 18, 20)
        ));
        return panel;
    }
}
