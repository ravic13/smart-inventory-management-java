
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login extends JFrame implements ActionListener {

    private JTextField userText;
    private JPasswordField passText;
    private JComboBox<String> roleCombo;
    private JButton loginButton;
    private JButton clearButton;
    private JLabel messageLabel;

    public Login() {
        setTitle("System Portal - Login");
        setAppIcon();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(800, 600));

        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(UIHelper.BG_LIGHT);

        JPanel cardPanel = new JPanel();
        cardPanel.setPreferredSize(new Dimension(420, 510));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setLayout(null);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(25, 30, 25, 30)
        ));

        JLabel titleLabel = new JLabel("Welcome Back", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(UIHelper.TEXT_NAVY);
        titleLabel.setBounds(30, 25, 360, 35);
        cardPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel(
                "Please enter your account details",
                SwingConstants.CENTER
        );
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);
        subtitleLabel.setBounds(30, 62, 360, 20);
        cardPanel.add(subtitleLabel);

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(UIHelper.TEXT_NAVY);
        userLabel.setBounds(40, 95, 340, 20);
        cardPanel.add(userLabel);

        userText = new JTextField();
        UIHelper.styleTextField(userText);
        userText.setBounds(40, 120, 340, 38);
        cardPanel.add(userText);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLabel.setForeground(UIHelper.TEXT_NAVY);
        passLabel.setBounds(40, 168, 340, 20);
        cardPanel.add(passLabel);

        passText = new JPasswordField();
        UIHelper.stylePasswordField(passText);
        passText.setBounds(40, 193, 340, 38);
        cardPanel.add(passText);

        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleLabel.setForeground(UIHelper.TEXT_NAVY);
        roleLabel.setBounds(40, 241, 340, 20);
        cardPanel.add(roleLabel);

        String[] roles = {"Admin", "InventoryManager", "Sales Staff"};
        roleCombo = new JComboBox<>(roles);
        roleCombo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleCombo.setBackground(Color.WHITE);
        roleCombo.setForeground(UIHelper.TEXT_NAVY);
        roleCombo.setBounds(40, 266, 340, 38);
        roleCombo.setFocusable(false);
        cardPanel.add(roleCombo);

        // High-Contrast Primary Login Button (#2563EB solid with white text)
        loginButton = new JButton("Login");
        UIHelper.styleButton(loginButton, UIHelper.COLOR_PRIMARY, Color.WHITE, new Font("Segoe UI", Font.BOLD, 15), new Insets(8, 16, 8, 16));
        loginButton.setBounds(40, 322, 340, 44);
        loginButton.addActionListener(this);
        cardPanel.add(loginButton);

        // High-Contrast Clear Button (#E2E8F0 with dark #0F172A text)
        clearButton = new JButton("Clear");
        UIHelper.styleButton(clearButton, UIHelper.COLOR_CLEAR_BG, UIHelper.COLOR_CLEAR_FG, new Font("Segoe UI", Font.BOLD, 14), new Insets(6, 16, 6, 16));
        clearButton.setBounds(40, 376, 340, 40);
        clearButton.addActionListener(this);
        cardPanel.add(clearButton);

        messageLabel = new JLabel("", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        messageLabel.setBounds(20, 424, 380, 25);
        cardPanel.add(messageLabel);

        JLabel hintLabel = new JLabel(
                "Sign in with your registered account",
                SwingConstants.CENTER
        );
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hintLabel.setForeground(UIHelper.TEXT_MUTED);
        hintLabel.setBounds(20, 452, 380, 20);
        cardPanel.add(hintLabel);

        userText.addActionListener(this);
        passText.addActionListener(this);

        backgroundPanel.add(cardPanel);
        add(backgroundPanel);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setAppIcon() {
        String[] paths = {
            "icons8-inventory-48.png",
            "icons/icons8-inventory-48.png",
            "icons8-inventory-240.png"
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                setIconImage(new ImageIcon(file.getAbsolutePath()).getImage());
                return;
            }
        }

        System.out.println("App icon file not found. Check the icon filename/path.");
    }

    // Authenticate against the existing MySQL user table.
    private void performLogin() {
        String username = userText.getText().trim();
        String password = new String(passText.getPassword());
        String selectedRole = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Enter your username and password.", false);
            return;
        }

        String sql =
                "SELECT username, role FROM `user` " +
                "WHERE username = ? AND password = ? " +
                "AND LOWER(role) = LOWER(?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, selectedRole);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String databaseUsername = rs.getString("username");
                    String databaseRole = rs.getString("role");

                    JOptionPane.showMessageDialog(
                            this,
                            "Login successful!\nWelcome, "
                                    + databaseUsername + " ("
                                    + databaseRole + ").",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    dispose();
                    new Dashboard(databaseUsername, databaseRole);
                } else {
                    showMessage(
                            "Invalid username, password, or role.", false
                    );
                }
            }

        } catch (SQLException e) {
            showMessage("Database connection or query failed.", false);

            JOptionPane.showMessageDialog(
                    this,
                    "Could not authenticate the user.\n\n" + e.getMessage()
                            + "\n\nCheck DBConnection.java and the user table columns.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setForeground(
                success ? new Color(16, 185, 129) : new Color(225, 29, 72)
        );
        messageLabel.setText(message);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton
                || e.getSource() == userText
                || e.getSource() == passText) {
            performLogin();
        } else if (e.getSource() == clearButton) {
            userText.setText("");
            passText.setText("");
            roleCombo.setSelectedIndex(0);
            messageLabel.setText("");
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
            // Use the default Swing look and feel.
        }

        SwingUtilities.invokeLater(Login::new);
    }
}
