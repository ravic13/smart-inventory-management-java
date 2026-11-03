import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

/**
 * Inventory Management & Stock Control Page
 * Allows stock tracking, Stock-In (receiving), Stock-Out (deduction),
 * threshold adjustment, and live audit logging into inventory_history.
 */
public class InventoryPage extends JPanel {

    private final Dashboard parentDashboard;
    private DefaultTableModel tableModel;
    private JTable inventoryTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JComboBox<String> filterCombo;
    private JLabel summaryLabel;

    public InventoryPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadInventory();
    }

    private void initComponents() {
        // --- 1. Top Controls Bar ---
        JPanel topPanel = UIHelper.createCardPanel();
        topPanel.setLayout(new BorderLayout(15, 0));

        // Left: Title & Summary
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel pageTitle = new JLabel("\uD83D\uDCCB  Inventory & Stock Control");
        pageTitle.setFont(UIHelper.FONT_SUBHEADER);
        pageTitle.setForeground(UIHelper.TEXT_NAVY);

        summaryLabel = new JLabel("(Loading...)");
        summaryLabel.setFont(UIHelper.FONT_SMALL);
        summaryLabel.setForeground(UIHelper.TEXT_GRAY);

        leftPanel.add(pageTitle);
        leftPanel.add(summaryLabel);
        topPanel.add(leftPanel, BorderLayout.WEST);

        // Right: Filters & Stock Action Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        filterCombo = new JComboBox<>(new String[]{"All Items", "Low Stock Only", "Out of Stock"});
        filterCombo.setFont(UIHelper.FONT_BODY);
        filterCombo.addActionListener(e -> applyFilter());

        JLabel searchIcon = UIHelper.createSearchIcon();
        searchField = UIHelper.createSearchField("Filter by name...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JButton stockInBtn = UIHelper.createSuccessButton("\uD83D\uDCE5  Stock In");
        stockInBtn.setToolTipText("Add received units to inventory");
        stockInBtn.addActionListener(e -> performStockIn());

        JButton stockOutBtn = UIHelper.createDangerButton("\uD83D\uDCE4  Stock Out");
        stockOutBtn.setToolTipText("Deduct damaged/transferred units");
        stockOutBtn.addActionListener(e -> performStockOut());

        JButton setMinBtn = UIHelper.createWarningButton("\u2699\uFE0F  Min Stock");
        setMinBtn.addActionListener(e -> adjustMinimumStock());

        JButton refreshBtn = UIHelper.createSecondaryButton("\uD83D\uDD04  Refresh");
        refreshBtn.addActionListener(e -> {
            loadInventory();
            if (parentDashboard != null) parentDashboard.refreshData();
        });

        rightPanel.add(new JLabel("Status:"));
        rightPanel.add(filterCombo);
        rightPanel.add(searchIcon);
        rightPanel.add(searchField);
        rightPanel.add(stockInBtn);
        rightPanel.add(stockOutBtn);
        rightPanel.add(setMinBtn);
        rightPanel.add(refreshBtn);

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table Panel ---
        JPanel tableContainer = UIHelper.createCardPanel();
        tableContainer.setLayout(new BorderLayout());

        String[] columns = {
                "ID", "Product Name", "Category", "Current Stock", "Min Threshold", "Stock Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        inventoryTable = new JTable(tableModel);
        UIHelper.styleTable(inventoryTable);

        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // ID
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(220);  // Name
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(140);  // Category
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Current Stock
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Min Stock
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(140);  // Status

        // Custom renderer for status column & highlight
        inventoryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);

                int modelRow = tbl.convertRowIndexToModel(row);
                int qty = (int) tableModel.getValueAt(modelRow, 3);
                int min = (int) tableModel.getValueAt(modelRow, 4);

                if (isSel) {
                    c.setBackground(UIHelper.COLOR_PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    if (qty <= 0) {
                        c.setBackground(new Color(254, 242, 242)); // Light red
                        c.setForeground(new Color(185, 28, 28));
                    } else if (qty <= min) {
                        c.setBackground(new Color(255, 251, 235)); // Light amber
                        c.setForeground(new Color(180, 83, 9));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                        c.setForeground(UIHelper.TEXT_NAVY);
                    }
                }

                if (col == 0 || col == 3 || col == 4 || col == 5) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        rowSorter = new TableRowSorter<>(tableModel);
        inventoryTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    /**
     * Loads live stock counts and statuses from MySQL
     */
    public void loadInventory() {
        tableModel.setRowCount(0);
        String sql = "SELECT Product_ID, Product_Name, Category, Quantity, Minimum_Stock " +
                "FROM product ORDER BY Quantity ASC;";

        int totalItems = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("Product_ID");
                String name = rs.getString("Product_Name");
                String cat = rs.getString("Category");
                int qty = rs.getInt("Quantity");
                int min = rs.getInt("Minimum_Stock");

                String status;
                if (qty <= 0) {
                    status = "\uD83D\uDD34  Out of Stock";
                    outOfStockCount++;
                } else if (qty <= min) {
                    status = "\u26A0\uFE0F  Low Stock";
                    lowStockCount++;
                } else {
                    status = "\uD83D\uDFE2  Optimal";
                }

                tableModel.addRow(new Object[]{
                        id,
                        name,
                        cat != null ? cat : "General",
                        qty,
                        min,
                        status
                });
                totalItems++;
            }

            summaryLabel.setText("(" + totalItems + " items | " + lowStockCount + " Low Stock | " + outOfStockCount + " Out of Stock)");
            applyFilter();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load inventory: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String searchText = searchField.getText().trim();
        String filterChoice = (String) filterCombo.getSelectedItem();

        rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int modelRow = entry.getIdentifier();
                String name = (String) tableModel.getValueAt(modelRow, 1);
                String cat = (String) tableModel.getValueAt(modelRow, 2);
                int qty = (int) tableModel.getValueAt(modelRow, 3);
                int min = (int) tableModel.getValueAt(modelRow, 4);

                // Text search match
                if (!searchText.isEmpty()) {
                    String combined = (name + " " + cat).toLowerCase();
                    if (!combined.contains(searchText.toLowerCase())) {
                        return false;
                    }
                }

                // Status dropdown match
                if ("Low Stock Only".equals(filterChoice)) {
                    return qty > 0 && qty <= min;
                } else if ("Out of Stock".equals(filterChoice)) {
                    return qty <= 0;
                }
                return true;
            }
        });
    }

    /**
     * Stock In - Receive inventory from supplier
     */
    private void performStockIn() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);
        int currentQty = (int) tableModel.getValueAt(modelRow, 3);

        String input = JOptionPane.showInputDialog(this,
                "Enter quantity to ADD for:\n\"" + productName + "\"\nCurrent stock: " + currentQty,
                "Stock In (Receive Goods)", JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        try {
            int addQty = Integer.parseInt(input.trim());
            if (addQty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // 1. Update product quantity
                    String updateSql = "UPDATE product SET Quantity = Quantity + ? WHERE Product_ID = ?;";
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setInt(1, addQty);
                        ps.setInt(2, productId);
                        ps.executeUpdate();
                    }

                    // 2. Audit in inventory_history
                    String historySql = "INSERT INTO inventory_history (Product_ID, Stock_In, Stock_Out, Date) VALUES (?, ?, 0, CURDATE());";
                    try (PreparedStatement ps = con.prepareStatement(historySql)) {
                        ps.setInt(1, productId);
                        ps.setInt(2, addQty);
                        ps.executeUpdate();
                    }

                    con.commit();
                    JOptionPane.showMessageDialog(this, "Successfully added " + addQty + " units to " + productName + "!", "Stock Updated", JOptionPane.INFORMATION_MESSAGE);
                    loadInventory();
                    if (parentDashboard != null) parentDashboard.refreshData();

                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                }
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Stock Out - Deduct damaged, expired, or transferred inventory
     */
    private void performStockOut() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);
        int currentQty = (int) tableModel.getValueAt(modelRow, 3);

        if (currentQty <= 0) {
            JOptionPane.showMessageDialog(this, "Product is already out of stock (Quantity is 0).", "Cannot Deduct", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "Enter quantity to DEDUCT for:\n\"" + productName + "\"\nCurrent stock: " + currentQty,
                "Stock Out (Deduct Stock)", JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        try {
            int deductQty = Integer.parseInt(input.trim());
            if (deductQty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than zero.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (deductQty > currentQty) {
                JOptionPane.showMessageDialog(this, "Cannot deduct " + deductQty + " units. Current available stock is only " + currentQty + ".", "Exceeds Stock", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                con.setAutoCommit(false);
                try {
                    // 1. Update product quantity
                    String updateSql = "UPDATE product SET Quantity = Quantity - ? WHERE Product_ID = ?;";
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setInt(1, deductQty);
                        ps.setInt(2, productId);
                        ps.executeUpdate();
                    }

                    // 2. Audit in inventory_history
                    String historySql = "INSERT INTO inventory_history (Product_ID, Stock_In, Stock_Out, Date) VALUES (?, 0, ?, CURDATE());";
                    try (PreparedStatement ps = con.prepareStatement(historySql)) {
                        ps.setInt(1, productId);
                        ps.setInt(2, deductQty);
                        ps.executeUpdate();
                    }

                    con.commit();
                    JOptionPane.showMessageDialog(this, "Successfully deducted " + deductQty + " units from " + productName + ".", "Stock Updated", JOptionPane.INFORMATION_MESSAGE);
                    loadInventory();
                    if (parentDashboard != null) parentDashboard.refreshData();

                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                }
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Set / Adjust Minimum Stock threshold
     */
    private void adjustMinimumStock() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a product to adjust minimum stock threshold.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = inventoryTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);
        int currentMin = (int) tableModel.getValueAt(modelRow, 4);

        String input = JOptionPane.showInputDialog(this,
                "Enter new Minimum Stock threshold for:\n\"" + productName + "\"\nCurrent threshold: " + currentMin,
                "Set Minimum Stock Threshold", JOptionPane.QUESTION_MESSAGE);

        if (input == null || input.trim().isEmpty()) return;

        try {
            int newMin = Integer.parseInt(input.trim());
            if (newMin < 0) {
                JOptionPane.showMessageDialog(this, "Threshold cannot be negative.", "Invalid Threshold", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "UPDATE product SET Minimum_Stock = ? WHERE Product_ID = ?;";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, newMin);
                ps.setInt(2, productId);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Minimum stock threshold updated to " + newMin + "!", "Updated", JOptionPane.INFORMATION_MESSAGE);
                loadInventory();
                if (parentDashboard != null) parentDashboard.refreshData();
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
