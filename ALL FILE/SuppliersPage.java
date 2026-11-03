import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

/**
 * Suppliers Management Page
 * Displays all suppliers from the MySQL database with real-time search,
 * and full CRUD operations (Add, Edit, Delete, Refresh).
 */
public class SuppliersPage extends JPanel {

    private final Dashboard parentDashboard;
    private DefaultTableModel tableModel;
    private JTable suppliersTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JLabel countLabel;

    public SuppliersPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadSuppliers();
    }

    private void initComponents() {
        // --- 1. Top Controls Bar ---
        JPanel topPanel = UIHelper.createCardPanel();
        topPanel.setLayout(new BorderLayout(15, 0));

        // Left: Title & Count
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel pageTitle = new JLabel("\uD83D\uDC65  Suppliers Management");
        pageTitle.setFont(UIHelper.FONT_SUBHEADER);
        pageTitle.setForeground(UIHelper.TEXT_NAVY);

        countLabel = new JLabel("(0 suppliers)");
        countLabel.setFont(UIHelper.FONT_SMALL);
        countLabel.setForeground(UIHelper.TEXT_GRAY);

        leftPanel.add(pageTitle);
        leftPanel.add(countLabel);
        topPanel.add(leftPanel, BorderLayout.WEST);

        // Right: Search & Actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        JLabel searchIcon = UIHelper.createSearchIcon();
        searchField = UIHelper.createSearchField("Search suppliers...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        JButton addBtn = UIHelper.createSuccessButton("\u2795  Add Supplier");
        addBtn.addActionListener(e -> showAddSupplierDialog());

        JButton editBtn = UIHelper.createEditButton("\u270F\uFE0F  Edit");
        editBtn.addActionListener(e -> showEditSupplierDialog());

        JButton deleteBtn = UIHelper.createDangerButton("\uD83D\uDDD1\uFE0F  Delete");
        deleteBtn.addActionListener(e -> deleteSelectedSupplier());

        JButton refreshBtn = UIHelper.createSecondaryButton("\uD83D\uDD04  Refresh");
        refreshBtn.addActionListener(e -> {
            loadSuppliers();
            if (parentDashboard != null) parentDashboard.refreshData();
        });

        rightPanel.add(searchIcon);
        rightPanel.add(searchField);
        rightPanel.add(addBtn);
        rightPanel.add(editBtn);
        rightPanel.add(deleteBtn);
        rightPanel.add(refreshBtn);

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table Panel ---
        JPanel tableContainer = UIHelper.createCardPanel();
        tableContainer.setLayout(new BorderLayout());

        String[] columns = {"ID", "Supplier Name", "Phone", "Email", "Address"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        suppliersTable = new JTable(tableModel);
        UIHelper.styleTable(suppliersTable);

        suppliersTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // ID
        suppliersTable.getColumnModel().getColumn(1).setPreferredWidth(180);  // Name
        suppliersTable.getColumnModel().getColumn(2).setPreferredWidth(130);  // Phone
        suppliersTable.getColumnModel().getColumn(3).setPreferredWidth(180);  // Email
        suppliersTable.getColumnModel().getColumn(4).setPreferredWidth(260);  // Address

        rowSorter = new TableRowSorter<>(tableModel);
        suppliersTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(suppliersTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    /**
     * Loads supplier records live from MySQL
     */
    public void loadSuppliers() {
        tableModel.setRowCount(0);
        String sql = "SELECT Supplier_ID, Supplier_Name, Phone, Email, Address FROM supplier ORDER BY Supplier_ID ASC;";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("Supplier_ID");
                String name = rs.getString("Supplier_Name");
                String phone = rs.getString("Phone");
                String email = rs.getString("Email");
                String address = rs.getString("Address");

                tableModel.addRow(new Object[]{
                        id,
                        name,
                        phone != null ? phone : "-",
                        email != null ? email : "-",
                        address != null ? address : "-"
                });
                count++;
            }
            countLabel.setText("(" + count + " suppliers)");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to fetch suppliers from database:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddSupplierDialog() {
        SupplierFormDialog dialog = new SupplierFormDialog(parentDashboard, "Add New Supplier", null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadSuppliers();
            if (parentDashboard != null) parentDashboard.refreshData();
        }
    }

    private void showEditSupplierDialog() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = suppliersTable.convertRowIndexToModel(selectedRow);
        int supplierId = (int) tableModel.getValueAt(modelRow, 0);

        SupplierFormDialog dialog = new SupplierFormDialog(parentDashboard, "Edit Supplier", supplierId);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadSuppliers();
            if (parentDashboard != null) parentDashboard.refreshData();
        }
    }

    private void deleteSelectedSupplier() {
        int selectedRow = suppliersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a supplier to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = suppliersTable.convertRowIndexToModel(selectedRow);
        int supplierId = (int) tableModel.getValueAt(modelRow, 0);
        String supplierName = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete supplier:\n\"" + supplierName + "\" (ID: " + supplierId + ")?\n\nThis will remove them from the database.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM supplier WHERE Supplier_ID = ?;";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, supplierId);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Supplier deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadSuppliers();
                    if (parentDashboard != null) parentDashboard.refreshData();
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Could not delete supplier (it may be linked to products or purchase orders):\n" + e.getMessage(),
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Modal Dialog for Adding / Editing a Supplier
     */
    private static class SupplierFormDialog extends JDialog {
        private final Integer supplierId;
        private boolean saved = false;

        private JTextField nameField;
        private JTextField phoneField;
        private JTextField emailField;
        private JTextArea addressArea;

        public SupplierFormDialog(Frame owner, String title, Integer supplierId) {
            super(owner, title, true);
            this.supplierId = supplierId;
            setSize(420, 420);
            setLocationRelativeTo(owner);
            setResizable(false);
            setLayout(new BorderLayout());

            JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 12));
            formPanel.setBorder(new EmptyBorder(20, 25, 10, 25));
            formPanel.setBackground(Color.WHITE);

            nameField = new JTextField();
            UIHelper.styleTextField(nameField);
            phoneField = new JTextField();
            UIHelper.styleTextField(phoneField);
            emailField = new JTextField();
            UIHelper.styleTextField(emailField);
            addressArea = new JTextArea(3, 20);
            addressArea.setFont(UIHelper.FONT_BODY);
            addressArea.setForeground(UIHelper.TEXT_NAVY);
            addressArea.setBackground(Color.WHITE);
            addressArea.setLineWrap(true);
            addressArea.setWrapStyleWord(true);
            addressArea.setBorder(new EmptyBorder(5, 8, 5, 8));
            JScrollPane addressScroll = new JScrollPane(addressArea);
            addressScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_STRONG, 1));

            Font lblFont = new Font("Segoe UI", Font.BOLD, 14);
            JLabel lbl1 = new JLabel("Supplier Name * :"); lbl1.setFont(lblFont); lbl1.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl1);
            formPanel.add(nameField);
            JLabel lbl2 = new JLabel("Phone Number :"); lbl2.setFont(lblFont); lbl2.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl2);
            formPanel.add(phoneField);
            JLabel lbl3 = new JLabel("Email Address :"); lbl3.setFont(lblFont); lbl3.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl3);
            formPanel.add(emailField);
            JLabel lbl4 = new JLabel("Office Address :"); lbl4.setFont(lblFont); lbl4.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl4);
            formPanel.add(addressScroll);

            add(formPanel, BorderLayout.CENTER);

            // Bottom Buttons
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            btnPanel.setBackground(UIHelper.BG_LIGHT);

            JButton cancelBtn = UIHelper.createSecondaryButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = UIHelper.createSuccessButton("\uD83D\uDCBE  Save Supplier");
            saveBtn.addActionListener(e -> saveSupplier());

            btnPanel.add(cancelBtn);
            btnPanel.add(saveBtn);
            add(btnPanel, BorderLayout.SOUTH);

            if (supplierId != null) {
                loadExistingData();
            }
        }

        private void loadExistingData() {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM supplier WHERE Supplier_ID = ?;")) {
                ps.setInt(1, supplierId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nameField.setText(rs.getString("Supplier_Name"));
                        phoneField.setText(rs.getString("Phone"));
                        emailField.setText(rs.getString("Email"));
                        addressArea.setText(rs.getString("Address"));
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Could not load supplier: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void saveSupplier() {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressArea.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Supplier Name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                if (supplierId == null) {
                    // INSERT
                    String sql = "INSERT INTO supplier (Supplier_Name, Phone, Email, Address) VALUES (?, ?, ?, ?);";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, phone);
                        ps.setString(3, email);
                        ps.setString(4, address);
                        ps.executeUpdate();
                    }
                } else {
                    // UPDATE
                    String sql = "UPDATE supplier SET Supplier_Name=?, Phone=?, Email=?, Address=? WHERE Supplier_ID=?;";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, phone);
                        ps.setString(3, email);
                        ps.setString(4, address);
                        ps.setInt(5, supplierId);
                        ps.executeUpdate();
                    }
                }

                saved = true;
                dispose();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database operation failed:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }
}
