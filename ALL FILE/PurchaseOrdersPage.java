import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;

/**
 * Purchase Orders Management Page
 * Handles creation of purchase orders, supplier linking, and status transitions
 * (Pending -> Completed -> Cancelled) with automatic inventory receipt.
 */
public class PurchaseOrdersPage extends JPanel {

    private final Dashboard parentDashboard;
    private DefaultTableModel tableModel;
    private JTable ordersTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JComboBox<String> statusFilterCombo;
    private JLabel countLabel;

    public PurchaseOrdersPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadOrders();
    }

    private void initComponents() {
        // --- 1. Top Bar ---
        JPanel topPanel = UIHelper.createCardPanel();
        topPanel.setLayout(new BorderLayout(15, 0));

        // Left: Title & Count
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel pageTitle = new JLabel("\uD83D\uDCD1  Purchase Orders Management");
        pageTitle.setFont(UIHelper.FONT_SUBHEADER);
        pageTitle.setForeground(UIHelper.TEXT_NAVY);

        countLabel = new JLabel("(0 orders)");
        countLabel.setFont(UIHelper.FONT_SMALL);
        countLabel.setForeground(UIHelper.TEXT_GRAY);

        leftPanel.add(pageTitle);
        leftPanel.add(countLabel);
        topPanel.add(leftPanel, BorderLayout.WEST);

        // Right: Status Filter & Action Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        statusFilterCombo = new JComboBox<>(new String[]{"All Orders", "Pending", "Completed", "Cancelled"});
        statusFilterCombo.setFont(UIHelper.FONT_BODY);
        statusFilterCombo.addActionListener(e -> applyFilter());

        JButton createBtn = UIHelper.createSuccessButton("\u2795  Create PO");
        createBtn.addActionListener(e -> showCreateOrderDialog());

        JButton receiveBtn = UIHelper.createSuccessButton("\u2705  Receive Goods");
        receiveBtn.setToolTipText("Mark selected Pending order as Completed and add stock to inventory");
        receiveBtn.addActionListener(e -> markOrderReceived());

        JButton cancelBtn = UIHelper.createDangerButton("\u274C  Cancel PO");
        cancelBtn.addActionListener(e -> cancelSelectedOrder());

        JButton refreshBtn = UIHelper.createSecondaryButton("\uD83D\uDD04  Refresh");
        refreshBtn.addActionListener(e -> {
            loadOrders();
            if (parentDashboard != null) parentDashboard.refreshData();
        });

        rightPanel.add(new JLabel("Filter:"));
        rightPanel.add(statusFilterCombo);
        rightPanel.add(createBtn);
        rightPanel.add(receiveBtn);
        rightPanel.add(cancelBtn);
        rightPanel.add(refreshBtn);

        topPanel.add(rightPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- 2. Table Container ---
        JPanel tableContainer = UIHelper.createCardPanel();
        tableContainer.setLayout(new BorderLayout());

        String[] columns = {
                "Order ID", "Product Name", "Supplier Name", "Quantity", "Order Date", "Status", "Product_ID"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        ordersTable = new JTable(tableModel);
        UIHelper.styleTable(ordersTable);

        // Hide Product_ID column (col 6) from view, keeping it for backend actions
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Order ID
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(220);  // Product
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(200);  // Supplier
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Qty
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(130);  // Date
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(130);  // Status
        ordersTable.removeColumn(ordersTable.getColumnModel().getColumn(6)); // Hidden ID

        // Status renderer with color badges
        ordersTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                int modelRow = tbl.convertRowIndexToModel(row);
                String status = (String) tableModel.getValueAt(modelRow, 5);

                if (isSel) {
                    c.setBackground(UIHelper.COLOR_PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UIHelper.TEXT_NAVY);
                    if (col == 4) { // Status column in view
                        if ("Pending".equalsIgnoreCase(status)) {
                            c.setForeground(new Color(217, 119, 6)); // Amber
                            setFont(UIHelper.FONT_BODY_BOLD);
                        } else if ("Completed".equalsIgnoreCase(status) || "Received".equalsIgnoreCase(status)) {
                            c.setForeground(new Color(22, 163, 74)); // Emerald Green
                            setFont(UIHelper.FONT_BODY_BOLD);
                        } else if ("Cancelled".equalsIgnoreCase(status)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                            setFont(UIHelper.FONT_BODY_BOLD);
                        }
                    }
                }
                setHorizontalAlignment((col == 0 || col == 3 || col == 4) ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        rowSorter = new TableRowSorter<>(tableModel);
        ordersTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(ordersTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    /**
     * Loads purchase orders live from MySQL
     */
    public void loadOrders() {
        tableModel.setRowCount(0);
        String sql = "SELECT po.Order_ID, p.Product_Name, s.Supplier_Name, po.Quantity, po.Order_Date, po.Status, po.Product_ID " +
                "FROM purchase_order po " +
                "LEFT JOIN product p ON po.Product_ID = p.Product_ID " +
                "LEFT JOIN supplier s ON po.Supplier_ID = s.Supplier_ID " +
                "ORDER BY po.Order_ID DESC;";

        int count = 0;
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("Order_ID");
                String prodName = rs.getString("Product_Name");
                String suppName = rs.getString("Supplier_Name");
                int qty = rs.getInt("Quantity");
                Date date = rs.getDate("Order_Date");
                String status = rs.getString("Status");
                int prodId = rs.getInt("Product_ID");

                tableModel.addRow(new Object[]{
                        id,
                        prodName != null ? prodName : "(Product Deleted)",
                        suppName != null ? suppName : "(None)",
                        qty,
                        date != null ? date.toString() : "-",
                        status != null ? status : "Pending",
                        prodId
                });
                count++;
            }
            countLabel.setText("(" + count + " orders)");
            applyFilter();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load purchase orders: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilter() {
        String filter = (String) statusFilterCombo.getSelectedItem();
        if ("All Orders".equals(filter)) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)^" + filter + "$", 5));
        }
    }

    private void showCreateOrderDialog() {
        CreateOrderDialog dialog = new CreateOrderDialog(parentDashboard);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadOrders();
            if (parentDashboard != null) parentDashboard.refreshData();
        }
    }

    /**
     * Marks an order as Completed/Received and increments product stock in inventory
     */
    private void markOrderReceived() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = ordersTable.convertRowIndexToModel(selectedRow);
        int orderId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);
        int quantity = (int) tableModel.getValueAt(modelRow, 3);
        String currentStatus = (String) tableModel.getValueAt(modelRow, 5);
        int productId = (int) tableModel.getValueAt(modelRow, 6);

        if ("Completed".equalsIgnoreCase(currentStatus) || "Received".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This order has already been marked as Completed.", "Already Completed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if ("Cancelled".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Cannot receive a cancelled order.", "Order Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Receive goods for Order #%d?\n\nThis will mark the order as 'Completed' and automatically add %d units to '%s' inventory.",
                        orderId, quantity, productName),
                "Confirm Receipt of Goods", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Update purchase_order status to Completed
                String updateOrderSql = "UPDATE purchase_order SET Status = 'Completed' WHERE Order_ID = ?;";
                try (PreparedStatement ps = con.prepareStatement(updateOrderSql)) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }

                // 2. Add quantity to product stock
                String updateStockSql = "UPDATE product SET Quantity = Quantity + ? WHERE Product_ID = ?;";
                try (PreparedStatement ps = con.prepareStatement(updateStockSql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }

                // 3. Log to inventory_history
                String logSql = "INSERT INTO inventory_history (Product_ID, Stock_In, Stock_Out, Date) VALUES (?, ?, 0, CURDATE());";
                try (PreparedStatement ps = con.prepareStatement(logSql)) {
                    ps.setInt(1, productId);
                    ps.setInt(2, quantity);
                    ps.executeUpdate();
                }

                con.commit();
                JOptionPane.showMessageDialog(this,
                        String.format("Order #%d received!\nAdded %d units to %s.", orderId, quantity, productName),
                        "Order Completed", JOptionPane.INFORMATION_MESSAGE);

                loadOrders();
                if (parentDashboard != null) parentDashboard.refreshData();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSelectedOrder() {
        int selectedRow = ordersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = ordersTable.convertRowIndexToModel(selectedRow);
        int orderId = (int) tableModel.getValueAt(modelRow, 0);
        String currentStatus = (String) tableModel.getValueAt(modelRow, 5);

        if ("Completed".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "Cannot cancel an already completed and received order.", "Cannot Cancel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel Purchase Order #" + orderId + "?",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "UPDATE purchase_order SET Status = 'Cancelled' WHERE Order_ID = ?;";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, orderId);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Order #" + orderId + " cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                loadOrders();
                if (parentDashboard != null) parentDashboard.refreshData();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Dialog to Create a New Purchase Order
     */
    private static class CreateOrderDialog extends JDialog {
        private boolean saved = false;

        private JComboBox<ProductItem> productCombo;
        private JComboBox<SupplierItem> supplierCombo;
        private JSpinner quantitySpinner;

        public CreateOrderDialog(Frame owner) {
            super(owner, "Create New Purchase Order", true);
            setSize(420, 320);
            setLocationRelativeTo(owner);
            setResizable(false);
            setLayout(new BorderLayout());

            JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
            formPanel.setBorder(new EmptyBorder(25, 25, 15, 25));
            formPanel.setBackground(Color.WHITE);

            productCombo = new JComboBox<>();
            productCombo.setFont(UIHelper.FONT_BODY);
            productCombo.setBackground(Color.WHITE);
            productCombo.setForeground(UIHelper.TEXT_NAVY);

            supplierCombo = new JComboBox<>();
            supplierCombo.setFont(UIHelper.FONT_BODY);
            supplierCombo.setBackground(Color.WHITE);
            supplierCombo.setForeground(UIHelper.TEXT_NAVY);

            quantitySpinner = new JSpinner(new SpinnerNumberModel(10, 1, 99999, 1));
            quantitySpinner.setFont(UIHelper.FONT_BODY);

            loadData();

            Font lblFont = new Font("Segoe UI", Font.BOLD, 14);
            JLabel lbl1 = new JLabel("Product * :"); lbl1.setFont(lblFont); lbl1.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl1);
            formPanel.add(productCombo);
            JLabel lbl2 = new JLabel("Supplier * :"); lbl2.setFont(lblFont); lbl2.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl2);
            formPanel.add(supplierCombo);
            JLabel lbl3 = new JLabel("Order Quantity * :"); lbl3.setFont(lblFont); lbl3.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl3);
            formPanel.add(quantitySpinner);

            add(formPanel, BorderLayout.CENTER);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            btnPanel.setBackground(UIHelper.BG_LIGHT);

            JButton cancelBtn = UIHelper.createSecondaryButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = UIHelper.createSuccessButton("\uD83D\uDCBE  Place Order");
            saveBtn.addActionListener(e -> saveOrder());

            btnPanel.add(cancelBtn);
            btnPanel.add(saveBtn);
            add(btnPanel, BorderLayout.SOUTH);
        }

        private void loadData() {
            try (Connection con = DBConnection.getConnection();
                 Statement stmt = con.createStatement()) {

                // Products
                try (ResultSet rs = stmt.executeQuery("SELECT Product_ID, Product_Name FROM product ORDER BY Product_Name;")) {
                    while (rs.next()) {
                        productCombo.addItem(new ProductItem(rs.getInt("Product_ID"), rs.getString("Product_Name")));
                    }
                }

                // Suppliers
                try (ResultSet rs = stmt.executeQuery("SELECT Supplier_ID, Supplier_Name FROM supplier ORDER BY Supplier_Name;")) {
                    while (rs.next()) {
                        supplierCombo.addItem(new SupplierItem(rs.getInt("Supplier_ID"), rs.getString("Supplier_Name")));
                    }
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to load options: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void saveOrder() {
            ProductItem prod = (ProductItem) productCombo.getSelectedItem();
            SupplierItem supp = (SupplierItem) supplierCombo.getSelectedItem();
            int qty = (int) quantitySpinner.getValue();

            if (prod == null || supp == null) {
                JOptionPane.showMessageDialog(this, "Please ensure both product and supplier are selected.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "INSERT INTO purchase_order (Product_ID, Supplier_ID, Quantity, Order_Date, Status) VALUES (?, ?, ?, CURDATE(), 'Pending');";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, prod.id);
                ps.setInt(2, supp.id);
                ps.setInt(3, qty);
                ps.executeUpdate();

                saved = true;
                JOptionPane.showMessageDialog(this, "Purchase Order placed successfully with status 'Pending'!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to place order: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSaved() {
            return saved;
        }

        private static class ProductItem {
            final int id;
            final String name;
            ProductItem(int id, String name) { this.id = id; this.name = name; }
            @Override public String toString() { return name; }
        }

        private static class SupplierItem {
            final int id;
            final String name;
            SupplierItem(int id, String name) { this.id = id; this.name = name; }
            @Override public String toString() { return name; }
        }
    }
}
