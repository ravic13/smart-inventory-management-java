import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.regex.Pattern;

public class InventoryApp {

    // ================= DATABASE =================
    static final String DB_URL  = "jdbc:mysql://localhost:3306/inventory?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    static final String DB_USER = "root";
    static final String DB_PASS = "ravi@2025";

    // ================= THEME =================
    static final Color BG         = new Color(240, 244, 248);
    static final Color BORDER     = new Color(210, 218, 228);
    static final Color TEXT       = new Color(17, 24, 39);
    static final Color MUTED      = new Color(100, 116, 139);
    static final Color BLUE       = new Color(37, 99, 235);
    static final Color LIGHT_BLUE = new Color(219, 234, 254);
    static final Color GRAY_BTN   = new Color(226, 232, 240);
    static final Color RED        = new Color(220, 38, 38);
    static final Color GREEN      = new Color(22, 163, 74);

    static final String[] ROLES = {"Admin", "Inventory Manager", "Sales Staff"};
    static final Pattern GMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");
    static final SecureRandom RNG = new SecureRandom();

    static JFrame frame;
    static JTextField userField;
    static JPasswordField passField;
    static JComboBox<String> roleBox;

    // ================= MAIN =================
    public static void main(String[] args) {
        try {
            initDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database error:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(InventoryApp::buildLoginWindow);
    }

    // ================= DATABASE SETUP =================
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    static void initDatabase() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `user` ("
                    + "User_ID INT AUTO_INCREMENT PRIMARY KEY, "
                    + "Username VARCHAR(50) NOT NULL UNIQUE, "
                    + "Password VARCHAR(255) NOT NULL, "
                    + "Role VARCHAR(50) NOT NULL, "
                    + "gmail VARCHAR(100) NOT NULL, "
                    + "status VARCHAR(50))");

            addColumnIfMissing(st, "full_name", "VARCHAR(100)");
            addColumnIfMissing(st, "status", "VARCHAR(50)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS signup_request ("
                    + "request_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "full_name VARCHAR(100) NOT NULL, "
                    + "role VARCHAR(50) NOT NULL, "
                    + "gmail VARCHAR(100) NOT NULL, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
                    + "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    // Adds a column to `user` only if it doesn't exist yet (error 1060 = duplicate column)
    static void addColumnIfMissing(Statement st, String column, String definition) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE `user` ADD COLUMN " + column + " " + definition);
        } catch (SQLException ex) {
            if (ex.getErrorCode() != 1060) throw ex;
        }
    }

    static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================= LOGIN WINDOW =================
    static void buildLoginWindow() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        userField = styledField();
        passField = new JPasswordField();
        styleInput(passField);
        roleBox = new JComboBox<>(ROLES);
        styleInput(roleBox);

        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Please enter your account details");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton login  = button("Login", BLUE, Color.WHITE);
        JButton clear  = button("Clear", GRAY_BTN, TEXT);
        JButton signUp = button("Sign Up", LIGHT_BLUE, BLUE);
        fixSize(login, 340, 42);
        fixSize(clear, 340, 42);
        fixSize(signUp, 340, 42);

        login.addActionListener(e -> login());
        passField.addActionListener(e -> login());
        clear.addActionListener(e -> clearLogin());
        signUp.addActionListener(e -> openSignUp());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(32, 40, 28, 40)));

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));
        addField(card, "Username", userField);
        addField(card, "Password", passField);
        addField(card, "Role", roleBox);
        card.add(login);
        card.add(Box.createVerticalStrut(10));
        card.add(clear);
        card.add(Box.createVerticalStrut(10));
        card.add(signUp);
        card.add(Box.createVerticalStrut(20));

        JLabel footer = new JLabel("Sign in with your registered account");
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);

        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(BG);
        page.add(card);

        frame = new JFrame("System Portal - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(page);
        frame.getRootPane().setDefaultButton(login);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static void clearLogin() {
        userField.setText("");
        passField.setText("");
        roleBox.setSelectedIndex(0);
    }

    static void login() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (u.isEmpty() || p.isEmpty()) {
            warn(frame, "Please enter your username and password.");
            return;
        }

        String sql = "SELECT User_ID, Password, full_name FROM `user` WHERE Username = ? AND Role = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u);
            ps.setString(2, role);

            int id;
            String stored;
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    warn(frame, "Invalid username, password, or role.");
                    return;
                }
                id = rs.getInt("User_ID");
                stored = rs.getString("Password");
            }

            // Accepts hashed passwords (new accounts) and plain-text passwords (older rows).
            // Plain-text matches are upgraded to a hash automatically.
            String hashed = hash(p);
            boolean matchesHash = hashed.equals(stored);
            boolean matchesPlain = p.equals(stored);
            if (!matchesHash && !matchesPlain) {
                warn(frame, "Invalid username, password, or role.");
                return;
            }
            if (!matchesHash) {
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE `user` SET Password = ? WHERE User_ID = ?")) {
                    up.setString(1, hashed);
                    up.setInt(2, id);
                    up.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            dbError(frame, ex);
            return;
        }

        String name = fullName(u);
        clearLogin();
        info(frame, "Login successful.\nWelcome, " + name + " (" + role + ").");

        // Admin only: show the approval window first.
        // It is modal, so the code continues after the admin closes it.
        if ("Admin".equals(role)) {
            openApprovals();
        }

        // Then open the Dashboard (your existing Dashboard.java) for every role.
        frame.setVisible(false);
        new Dashboard(u, role);
    }

    // Returns the full name for a username, or the username if no name is stored
    static String fullName(String username) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT full_name FROM `user` WHERE Username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) return rs.getString(1);
            }
        } catch (SQLException ignored) {}
        return username;
    }

    // ================= SIGN UP DIALOG =================
    static void openSignUp() {
        JDialog d = new JDialog(frame, "Sign Up", true);

        JTextField nameField = styledField();
        JTextField gmailField = styledField();
        JComboBox<String> rb = new JComboBox<>(ROLES);
        styleInput(rb);

        JLabel title = new JLabel("Create an Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Your request will be sent to the admin for approval");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton submit = button("Submit for Approval", BLUE, Color.WHITE);
        JButton cancel = button("Cancel", GRAY_BTN, TEXT);
        fixSize(submit, 340, 42);
        fixSize(cancel, 340, 42);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(sub);
        form.add(Box.createVerticalStrut(22));
        addField(form, "Full Name", nameField);
        addField(form, "Gmail", gmailField);
        addField(form, "Role", rb);
        form.add(submit);
        form.add(Box.createVerticalStrut(10));
        form.add(cancel);

        submit.addActionListener(e -> {
            String name = nameField.getText().trim();
            String gmail = gmailField.getText().trim().toLowerCase();
            String role = (String) rb.getSelectedItem();

            if (name.isEmpty() || gmail.isEmpty()) {
                warn(d, "Please fill in your name and Gmail.");
                return;
            }
            if (!GMAIL.matcher(gmail).matches()) {
                warn(d, "Please enter a valid Gmail address (example@gmail.com).");
                return;
            }

            String sql = "INSERT INTO signup_request (full_name, role, gmail) VALUES (?, ?, ?)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, role);
                ps.setString(3, gmail);
                ps.executeUpdate();
            } catch (SQLException ex) {
                dbError(d, ex);
                return;
            }

            info(d, "Your request has been sent to the admin.\n"
                  + "You will receive your username and password once approved.");
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());

        d.setContentPane(form);
        d.pack();
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }

    // ================= ADMIN APPROVAL WINDOW =================
    static void openApprovals() {
        JDialog d = new JDialog(frame, "Admin - Sign-Up Approvals", true);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Request ID", "Full Name", "Role", "Gmail", "Requested At"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.getTableHeader().setBackground(BLUE);
        table.getTableHeader().setForeground(Color.WHITE);
        loadRequests(model);

        JLabel header = new JLabel("  Pending sign-up requests");
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setForeground(TEXT);
        header.setBorder(BorderFactory.createEmptyBorder(14, 10, 10, 10));

        JButton approve = button("Approve & Create Account", GREEN, Color.WHITE);
        JButton reject  = button("Reject", RED, Color.WHITE);
        JButton refresh = button("Refresh", GRAY_BTN, TEXT);
        JButton close   = button("Close", GRAY_BTN, TEXT);

        approve.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { warn(d, "Please select a request first."); return; }
            int id = (Integer) model.getValueAt(row, 0);
            String name = (String) model.getValueAt(row, 1);
            String role = (String) model.getValueAt(row, 2);
            String gmail = (String) model.getValueAt(row, 3);

            Credentials cr = createAccount(d, id, name, role, gmail);
            if (cr != null) {
                info(d, "Account created for " + name + ".\n\n"
                      + "Username: " + cr.username + "\n"
                      + "Password: " + cr.password + "\n\n"
                      + "Share these details with the user. They can now log in.");
                loadRequests(model);
            }
        });

        reject.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { warn(d, "Please select a request first."); return; }
            int id = (Integer) model.getValueAt(row, 0);
            try (Connection c = getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE signup_request SET status = 'REJECTED' WHERE request_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException ex) {
                dbError(d, ex);
                return;
            }
            loadRequests(model);
        });

        refresh.addActionListener(e -> loadRequests(model));
        close.addActionListener(e -> d.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttons.setBackground(Color.WHITE);
        buttons.add(refresh);
        buttons.add(reject);
        buttons.add(approve);
        buttons.add(close);

        d.setLayout(new BorderLayout());
        d.add(header, BorderLayout.NORTH);
        d.add(new JScrollPane(table), BorderLayout.CENTER);
        d.add(buttons, BorderLayout.SOUTH);
        d.getContentPane().setBackground(Color.WHITE);
        d.setSize(820, 460);
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }

    static void loadRequests(DefaultTableModel model) {
        model.setRowCount(0);
        String sql = "SELECT request_id, full_name, role, gmail, requested_at "
                   + "FROM signup_request WHERE status = 'PENDING' ORDER BY requested_at";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getTimestamp(5)
                });
            }
        } catch (SQLException ex) {
            dbError(null, ex);
        }
    }

    // Creates a username and password for an approved request, saves the user,
    // and marks the request as APPROVED. All steps succeed or none do.
    static Credentials createAccount(Component parent, int requestId, String name, String role, String gmail) {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                String username = makeUsername(c, name);
                String password = randomPassword(10);

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO `user` (Username, Password, Role, gmail, full_name, status) "
                      + "VALUES (?, ?, ?, ?, ?, 'ACTIVE')")) {
                    ps.setString(1, username);
                    ps.setString(2, hash(password));
                    ps.setString(3, role);
                    ps.setString(4, gmail);
                    ps.setString(5, name);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE signup_request SET status = 'APPROVED' WHERE request_id = ?")) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                c.commit();
                return new Credentials(username, password);
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            dbError(parent, ex);
            return null;
        }
    }

    // "Sophia Admin" -> "sophia.admin", or "sophia.admin2" if that name is taken
    static String makeUsername(Connection c, String name) throws SQLException {
        String base = name.toLowerCase()
                          .replaceAll("[^a-z0-9]+", ".")
                          .replaceAll("^\\.+|\\.+$", "");
        if (base.isEmpty()) base = "user";

        String candidate = base;
        int n = 1;
        while (usernameExists(c, candidate)) {
            n++;
            candidate = base + n;
        }
        return candidate;
    }

    static boolean usernameExists(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM `user` WHERE Username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static String randomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RNG.nextInt(chars.length())));
        }
        return sb.toString();
    }

    static class Credentials {
        final String username;
        final String password;
        Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    // ================= UI HELPERS =================
    static void addField(JPanel panel, String labelText, JComponent comp) {
        JLabel l = new JLabel(labelText);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createVerticalStrut(6));
        panel.add(comp);
        panel.add(Box.createVerticalStrut(14));
    }

    static JTextField styledField() {
        JTextField f = new JTextField();
        styleInput(f);
        return f;
    }

    static void styleInput(JComponent c) {
        c.setFont(new Font("SansSerif", Font.PLAIN, 15));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        fixSize(c, 340, 38);
    }

    static void fixSize(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static void warn(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Notice", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    static void dbError(Component parent, SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(parent, "Database error:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}