import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Products Management Page
 * Displays all products from the MySQL database with real-time search,
 * and full CRUD operations (Add, Edit, Delete, Refresh).
 */
public class ProductsPage extends JPanel {

    private final Dashboard parentDashboard;
    private DefaultTableModel tableModel;
    private JTable productsTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JLabel countLabel;

    public ProductsPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadProducts();
    }

    private void initComponents() {
        // --- 1. Top Controls Bar ---
        JPanel topPanel = UIHelper.createCardPanel();
        topPanel.setLayout(new BorderLayout(15, 0));

        // Left: Title & Live count
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setBackground(Color.WHITE);

        JLabel pageTitle = new JLabel("\uD83D\uDCE6  Products Management");
        pageTitle.setFont(UIHelper.FONT_SUBHEADER);
        pageTitle.setForeground(UIHelper.TEXT_NAVY);

        countLabel = new JLabel("(0 products)");
        countLabel.setFont(UIHelper.FONT_SMALL);
        countLabel.setForeground(UIHelper.TEXT_GRAY);

        leftPanel.add(pageTitle);
        leftPanel.add(countLabel);
        topPanel.add(leftPanel, BorderLayout.WEST);

        // Center / Right: Search & Actions
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        JLabel searchIcon = UIHelper.createSearchIcon();
        searchField = UIHelper.createSearchField("Search products...");
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

        JButton addBtn = UIHelper.createSuccessButton("\u2795  Add Product");
        addBtn.addActionListener(e -> showAddProductDialog());

        JButton editBtn = UIHelper.createEditButton("\u270F\uFE0F  Edit");
        editBtn.addActionListener(e -> showEditProductDialog());

        JButton deleteBtn = UIHelper.createDangerButton("\uD83D\uDDD1\uFE0F  Delete");
        deleteBtn.addActionListener(e -> deleteSelectedProduct());

        JButton refreshBtn = UIHelper.createSecondaryButton("\uD83D\uDD04  Refresh");
        refreshBtn.addActionListener(e -> {
            loadProducts();
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

        // --- 2. Products Table Panel ---
        JPanel tableContainer = UIHelper.createCardPanel();
        tableContainer.setLayout(new BorderLayout());

        String[] columns = {
                "ID", "Product Name", "Category", "Buying Price", "Selling Price", "Quantity", "Min Stock", "Supplier"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        productsTable = new JTable(tableModel);
        UIHelper.styleTable(productsTable);

        // Set column widths
        productsTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(180);  // Name
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Category
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(95);   // Buy Price
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(95);   // Sell Price
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // Qty
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Min Stock
        productsTable.getColumnModel().getColumn(7).setPreferredWidth(140);  // Supplier

        rowSorter = new TableRowSorter<>(tableModel);
        productsTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    /**
     * Loads live product records from the database
     */
    public void loadProducts() {
        tableModel.setRowCount(0);
        String sql = "SELECT p.Product_ID, p.Product_Name, p.Category, p.Buying_Price, " +
                "p.Selling_Price, p.Quantity, p.Minimum_Stock, s.Supplier_Name " +
                "FROM product p " +
                "LEFT JOIN supplier s ON p.Supplier_ID = s.Supplier_ID " +
                "ORDER BY p.Product_ID ASC;";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("Product_ID");
                String name = rs.getString("Product_Name");
                String category = rs.getString("Category");
                double buyingPrice = rs.getDouble("Buying_Price");
                double sellingPrice = rs.getDouble("Selling_Price");
                int qty = rs.getInt("Quantity");
                int minStock = rs.getInt("Minimum_Stock");
                String supplierName = rs.getString("Supplier_Name");
                if (supplierName == null) supplierName = "None";

                tableModel.addRow(new Object[]{
                        id,
                        name,
                        category != null ? category : "General",
                        String.format("\u20B9%,.2f", buyingPrice),
                        String.format("\u20B9%,.2f", sellingPrice),
                        qty,
                        minStock,
                        supplierName
                });
                count++;
            }
            countLabel.setText("(" + count + " products)");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to fetch products from database:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddProductDialog() {
        ProductFormDialog dialog = new ProductFormDialog(parentDashboard, "Add New Product", null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadProducts();
            if (parentDashboard != null) parentDashboard.refreshData();
        }
    }

    private void showEditProductDialog() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productsTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);

        ProductFormDialog dialog = new ProductFormDialog(parentDashboard, "Edit Product", productId);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadProducts();
            if (parentDashboard != null) parentDashboard.refreshData();
        }
    }

    private void deleteSelectedProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = productsTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(modelRow, 0);
        String productName = (String) tableModel.getValueAt(modelRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete product:\n\"" + productName + "\" (ID: " + productId + ")?\n\nThis will permanently remove it from the database.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM product WHERE Product_ID = ?;";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, productId);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Product deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts();
                    if (parentDashboard != null) parentDashboard.refreshData();
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Could not delete product (it may be referenced by existing sales or purchase orders):\n" + e.getMessage(),
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Modal Dialog for Adding/Editing a Product
     */
    private static class ProductFormDialog extends JDialog {
        private final Integer productId;
        private boolean saved = false;

        private JTextField nameField;
        private JTextField categoryField;
        private JTextField buyingPriceField;
        private JTextField sellingPriceField;
        private JSpinner quantitySpinner;
        private JSpinner minStockSpinner;
        private JComboBox<SupplierItem> supplierCombo;

        public ProductFormDialog(Frame owner, String title, Integer productId) {
            super(owner, title, true);
            this.productId = productId;
            setSize(450, 480);
            setLocationRelativeTo(owner);
            setResizable(false);
            setLayout(new BorderLayout());

            JPanel formPanel = new JPanel(new GridLayout(7, 2, 12, 12));
            formPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
            formPanel.setBackground(Color.WHITE);

            nameField = new JTextField();
            UIHelper.styleTextField(nameField);
            categoryField = new JTextField();
            UIHelper.styleTextField(categoryField);
            buyingPriceField = new JTextField();
            UIHelper.styleTextField(buyingPriceField);
            sellingPriceField = new JTextField();
            UIHelper.styleTextField(sellingPriceField);
            quantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 999999, 1));
            quantitySpinner.setFont(UIHelper.FONT_BODY);
            minStockSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 999999, 1));
            minStockSpinner.setFont(UIHelper.FONT_BODY);
            supplierCombo = new JComboBox<>();
            supplierCombo.setFont(UIHelper.FONT_BODY);
            supplierCombo.setBackground(Color.WHITE);
            supplierCombo.setForeground(UIHelper.TEXT_NAVY);

            loadSuppliers();

            Font lblFont = new Font("Segoe UI", Font.BOLD, 14);
            JLabel lbl1 = new JLabel("Product Name * :"); lbl1.setFont(lblFont); lbl1.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl1);
            formPanel.add(nameField);
            JLabel lbl2 = new JLabel("Category :"); lbl2.setFont(lblFont); lbl2.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl2);
            formPanel.add(categoryField);
            JLabel lbl3 = new JLabel("Buying Price (\u20B9) * :"); lbl3.setFont(lblFont); lbl3.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl3);
            formPanel.add(buyingPriceField);
            JLabel lbl4 = new JLabel("Selling Price (\u20B9) * :"); lbl4.setFont(lblFont); lbl4.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl4);
            formPanel.add(sellingPriceField);
            JLabel lbl5 = new JLabel("Current Quantity :"); lbl5.setFont(lblFont); lbl5.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl5);
            formPanel.add(quantitySpinner);
            JLabel lbl6 = new JLabel("Minimum Stock :"); lbl6.setFont(lblFont); lbl6.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl6);
            formPanel.add(minStockSpinner);
            JLabel lbl7 = new JLabel("Supplier :"); lbl7.setFont(lblFont); lbl7.setForeground(UIHelper.TEXT_NAVY);
            formPanel.add(lbl7);
            formPanel.add(supplierCombo);

            add(formPanel, BorderLayout.CENTER);

            // Bottom Buttons
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            btnPanel.setBackground(UIHelper.BG_LIGHT);

            JButton cancelBtn = UIHelper.createSecondaryButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = UIHelper.createSuccessButton("\uD83D\uDCBE  Save Product");
            saveBtn.addActionListener(e -> saveProduct());

            btnPanel.add(cancelBtn);
            btnPanel.add(saveBtn);
            add(btnPanel, BorderLayout.SOUTH);

            if (productId != null) {
                loadExistingData();
            }
        }

        private void loadSuppliers() {
            supplierCombo.addItem(new SupplierItem(null, "-- None --"));
            try (Connection con = DBConnection.getConnection();
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT Supplier_ID, Supplier_Name FROM supplier ORDER BY Supplier_Name;")) {
                while (rs.next()) {
                    supplierCombo.addItem(new SupplierItem(rs.getInt("Supplier_ID"), rs.getString("Supplier_Name")));
                }
            } catch (SQLException ignored) {}
        }

        private void loadExistingData() {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM product WHERE Product_ID = ?;")) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nameField.setText(rs.getString("Product_Name"));
                        categoryField.setText(rs.getString("Category"));
                        buyingPriceField.setText(String.valueOf(rs.getDouble("Buying_Price")));
                        sellingPriceField.setText(String.valueOf(rs.getDouble("Selling_Price")));
                        quantitySpinner.setValue(rs.getInt("Quantity"));
                        minStockSpinner.setValue(rs.getInt("Minimum_Stock"));

                        int suppId = rs.getInt("Supplier_ID");
                        for (int i = 0; i < supplierCombo.getItemCount(); i++) {
                            SupplierItem item = supplierCombo.getItemAt(i);
                            if (item.id != null && item.id == suppId) {
                                supplierCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Could not load product details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void saveProduct() {
            String name = nameField.getText().trim();
            String category = categoryField.getText().trim();
            String buyStr = buyingPriceField.getText().trim();
            String sellStr = sellingPriceField.getText().trim();
            int qty = (int) quantitySpinner.getValue();
            int minStock = (int) minStockSpinner.getValue();
            SupplierItem selectedSupplier = (SupplierItem) supplierCombo.getSelectedItem();
            Integer supplierId = (selectedSupplier != null) ? selectedSupplier.id : null;

            if (name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields (Name, Buying Price, Selling Price).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double buyingPrice;
            double sellingPrice;
            try {
                buyingPrice = Double.parseDouble(buyStr);
                sellingPrice = Double.parseDouble(sellStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Prices must be valid numbers.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection con = DBConnection.getConnection()) {
                if (productId == null) {
                    // INSERT
                    String sql = "INSERT INTO product (Product_Name, Category, Buying_Price, Selling_Price, Quantity, Minimum_Stock, Supplier_ID) VALUES (?, ?, ?, ?, ?, ?, ?);";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, category.isEmpty() ? "General" : category);
                        ps.setBigDecimal(3, BigDecimal.valueOf(buyingPrice));
                        ps.setBigDecimal(4, BigDecimal.valueOf(sellingPrice));
                        ps.setInt(5, qty);
                        ps.setInt(6, minStock);
                        if (supplierId != null) ps.setInt(7, supplierId);
                        else ps.setNull(7, Types.INTEGER);
                        ps.executeUpdate();
                    }
                } else {
                    // UPDATE
                    String sql = "UPDATE product SET Product_Name=?, Category=?, Buying_Price=?, Selling_Price=?, Quantity=?, Minimum_Stock=?, Supplier_ID=? WHERE Product_ID=?;";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, category.isEmpty() ? "General" : category);
                        ps.setBigDecimal(3, BigDecimal.valueOf(buyingPrice));
                        ps.setBigDecimal(4, BigDecimal.valueOf(sellingPrice));
                        ps.setInt(5, qty);
                        ps.setInt(6, minStock);
                        if (supplierId != null) ps.setInt(7, supplierId);
                        else ps.setNull(7, Types.INTEGER);
                        ps.setInt(8, productId);
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

        private static class SupplierItem {
            final Integer id;
            final String name;
            SupplierItem(Integer id, String name) {
                this.id = id;
                this.name = name;
            }
            @Override
            public String toString() {
                return name;
            }
        }
    }
}
