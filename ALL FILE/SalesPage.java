import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;

/**
 * Sales Management & POS Billing Page
 * Allows recording new sales with automatic stock deduction and transaction history.
 */
public class SalesPage extends JPanel {

    private final Dashboard parentDashboard;

    // POS components
    private JComboBox<ProductSaleItem> productCombo;
    private JSpinner quantitySpinner;
    private JLabel unitPriceLabel;
    private JLabel totalAmountLabel;
    private JLabel availableStockLabel;

    // History components
    private DefaultTableModel salesTableModel;
    private JTable salesTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JLabel totalRevenueLabel;
    private JLabel transactionCountLabel;

    public SalesPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadProductsCombo();
        loadSalesHistory();
    }

    private void initComponents() {
        // --- 1. Top Section: New Sale Billing Card ---
        JPanel billingCard = UIHelper.createCardPanel();
        billingCard.setLayout(new BorderLayout(0, 12));

        JLabel billingTitle = new JLabel("\uD83D\uDCB3  New Sale / Point of Sale Billing");
        billingTitle.setFont(UIHelper.FONT_SUBHEADER);
        billingTitle.setForeground(UIHelper.TEXT_NAVY);
        billingCard.add(billingTitle, BorderLayout.NORTH);

        JPanel formGrid = new JPanel(new GridLayout(1, 6, 12, 0));
        formGrid.setBackground(Color.WHITE);

        // Product selection
        Font lblFont = new Font("Segoe UI", Font.BOLD, 14);

        JPanel productCol = new JPanel(new BorderLayout(0, 4));
        productCol.setBackground(Color.WHITE);
        JLabel l1 = new JLabel("Select Product:"); l1.setFont(lblFont); l1.setForeground(UIHelper.TEXT_NAVY);
        productCol.add(l1, BorderLayout.NORTH);
        productCombo = new JComboBox<>();
        productCombo.setFont(UIHelper.FONT_BODY);
        productCombo.setBackground(Color.WHITE);
        productCombo.setForeground(UIHelper.TEXT_NAVY);
        productCombo.addActionListener(e -> updatePOSCalculation());
        productCol.add(productCombo, BorderLayout.CENTER);

        // Available stock
        JPanel stockCol = new JPanel(new BorderLayout(0, 4));
        stockCol.setBackground(Color.WHITE);
        JLabel l2 = new JLabel("Available Stock:"); l2.setFont(lblFont); l2.setForeground(UIHelper.TEXT_NAVY);
        stockCol.add(l2, BorderLayout.NORTH);
        availableStockLabel = new JLabel("0 units");
        availableStockLabel.setFont(UIHelper.FONT_BODY_BOLD);
        availableStockLabel.setForeground(UIHelper.TEXT_NAVY);
        stockCol.add(availableStockLabel, BorderLayout.CENTER);

        // Quantity
        JPanel qtyCol = new JPanel(new BorderLayout(0, 4));
        qtyCol.setBackground(Color.WHITE);
        JLabel l3 = new JLabel("Quantity to Sell:"); l3.setFont(lblFont); l3.setForeground(UIHelper.TEXT_NAVY);
        qtyCol.add(l3, BorderLayout.NORTH);
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        quantitySpinner.setFont(UIHelper.FONT_BODY);
        quantitySpinner.addChangeListener(e -> updatePOSCalculation());
        qtyCol.add(quantitySpinner, BorderLayout.CENTER);

        // Unit Price
        JPanel unitCol = new JPanel(new BorderLayout(0, 4));
        unitCol.setBackground(Color.WHITE);
        JLabel l4 = new JLabel("Unit Price:"); l4.setFont(lblFont); l4.setForeground(UIHelper.TEXT_NAVY);
        unitCol.add(l4, BorderLayout.NORTH);
        unitPriceLabel = new JLabel("\u20B90.00");
        unitPriceLabel.setFont(UIHelper.FONT_BODY_BOLD);
        unitPriceLabel.setForeground(UIHelper.BLUE_PRIMARY);
        unitCol.add(unitPriceLabel, BorderLayout.CENTER);

        // Total
        JPanel totalCol = new JPanel(new BorderLayout(0, 4));
        totalCol.setBackground(Color.WHITE);
        JLabel l5 = new JLabel("Total Amount:"); l5.setFont(lblFont); l5.setForeground(UIHelper.TEXT_NAVY);
        totalCol.add(l5, BorderLayout.NORTH);
        totalAmountLabel = new JLabel("\u20B90.00");
        totalAmountLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        totalAmountLabel.setForeground(UIHelper.GREEN_SUCCESS);
        totalCol.add(totalAmountLabel, BorderLayout.CENTER);

        // Submit Button
        JPanel actionCol = new JPanel(new BorderLayout(0, 4));
        actionCol.setBackground(Color.WHITE);
        actionCol.add(new JLabel(" "), BorderLayout.NORTH);
        JButton completeSaleBtn = UIHelper.createSuccessButton("\u2705  Complete Sale");
        completeSaleBtn.addActionListener(e -> processSale());
        actionCol.add(completeSaleBtn, BorderLayout.CENTER);

        formGrid.add(productCol);
        formGrid.add(stockCol);
        formGrid.add(qtyCol);
        formGrid.add(unitCol);
        formGrid.add(totalCol);
        formGrid.add(actionCol);

        billingCard.add(formGrid, BorderLayout.CENTER);
        add(billingCard, BorderLayout.NORTH);

        // --- 2. Bottom Section: Sales History ---
        JPanel historyContainer = UIHelper.createCardPanel();
        historyContainer.setLayout(new BorderLayout(0, 12));

        // Subheader with search & totals
        JPanel subHeader = new JPanel(new BorderLayout(15, 0));
        subHeader.setBackground(Color.WHITE);

        JPanel leftSub = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftSub.setBackground(Color.WHITE);

        JLabel historyTitle = new JLabel("\uD83E\uDDFE  Sales History & Transactions");
        historyTitle.setFont(UIHelper.FONT_SUBHEADER);
        historyTitle.setForeground(UIHelper.TEXT_NAVY);

        transactionCountLabel = new JLabel("(0 sales)");
        transactionCountLabel.setFont(UIHelper.FONT_SMALL);
        transactionCountLabel.setForeground(UIHelper.TEXT_GRAY);

        totalRevenueLabel = new JLabel("Total Revenue: \u20B90.00");
        totalRevenueLabel.setFont(UIHelper.FONT_BODY_BOLD);
        totalRevenueLabel.setForeground(UIHelper.GREEN_SUCCESS);

        leftSub.add(historyTitle);
        leftSub.add(transactionCountLabel);
        leftSub.add(new JSeparator(SwingConstants.VERTICAL));
        leftSub.add(totalRevenueLabel);
        subHeader.add(leftSub, BorderLayout.WEST);

        // Search & Refresh
        JPanel rightSub = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightSub.setBackground(Color.WHITE);

        JLabel searchIcon = UIHelper.createSearchIcon();
        searchField = UIHelper.createSearchField("Search by product name...");
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

        JButton refreshBtn = UIHelper.createSecondaryButton("\uD83D\uDD04  Refresh");
        refreshBtn.addActionListener(e -> {
            loadProductsCombo();
            loadSalesHistory();
            if (parentDashboard != null) parentDashboard.refreshData();
        });

        rightSub.add(searchIcon);
        rightSub.add(searchField);
        rightSub.add(refreshBtn);
        subHeader.add(rightSub, BorderLayout.EAST);

        historyContainer.add(subHeader, BorderLayout.NORTH);

        // Table
        String[] columns = {"Sale ID", "Product Name", "Quantity Sold", "Total Amount", "Sale Date"};
        salesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        salesTable = new JTable(salesTableModel);
        UIHelper.styleTable(salesTable);

        salesTable.getColumnModel().getColumn(0).setPreferredWidth(80);   // Sale ID
        salesTable.getColumnModel().getColumn(1).setPreferredWidth(260);  // Product
        salesTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Qty
        salesTable.getColumnModel().getColumn(3).setPreferredWidth(140);  // Total Amount
        salesTable.getColumnModel().getColumn(4).setPreferredWidth(160);  // Date

        // Green styling for amount column
        salesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(UIHelper.COLOR_PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UIHelper.TEXT_NAVY);
                    if (col == 3) {
                        c.setForeground(new Color(22, 163, 74));
                        setFont(UIHelper.FONT_BODY_BOLD);
                    }
                }
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        rowSorter = new TableRowSorter<>(salesTableModel);
        salesTable.setRowSorter(rowSorter);

        JScrollPane scrollPane = new JScrollPane(salesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        historyContainer.add(scrollPane, BorderLayout.CENTER);

        add(historyContainer, BorderLayout.CENTER);
    }

    /**
     * Loads products into the POS ComboBox
     */
    public void loadProductsCombo() {
        productCombo.removeAllItems();
        String sql = "SELECT Product_ID, Product_Name, Selling_Price, Quantity FROM product ORDER BY Product_Name;";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productCombo.addItem(new ProductSaleItem(
                        rs.getInt("Product_ID"),
                        rs.getString("Product_Name"),
                        rs.getDouble("Selling_Price"),
                        rs.getInt("Quantity")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error loading products for POS: " + e.getMessage());
        }
        updatePOSCalculation();
    }

    private void updatePOSCalculation() {
        ProductSaleItem item = (ProductSaleItem) productCombo.getSelectedItem();
        if (item == null) {
            availableStockLabel.setText("0 units");
            unitPriceLabel.setText("\u20B90.00");
            totalAmountLabel.setText("\u20B90.00");
            return;
        }

        availableStockLabel.setText(item.stock + " units");
        if (item.stock <= 0) {
            availableStockLabel.setForeground(UIHelper.RED_DANGER);
        } else {
            availableStockLabel.setForeground(UIHelper.TEXT_NAVY);
        }

        unitPriceLabel.setText(String.format("\u20B9%,.2f", item.price));

        int qty = (int) quantitySpinner.getValue();
        double total = qty * item.price;
        totalAmountLabel.setText(String.format("\u20B9%,.2f", total));
    }

    /**
     * Executes the sale inside a JDBC database transaction
     */
    private void processSale() {
        ProductSaleItem item = (ProductSaleItem) productCombo.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a product first.", "No Product Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qty = (int) quantitySpinner.getValue();
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be at least 1.", "Invalid Quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (qty > item.stock) {
            JOptionPane.showMessageDialog(this,
                    "Insufficient stock!\nAvailable: " + item.stock + " units.\nRequested: " + qty + " units.",
                    "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalAmount = qty * item.price;

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Confirm Sale:\nProduct: %s\nQuantity: %d\nTotal: \u20B9%,.2f", item.name, qty, totalAmount),
                "Confirm POS Sale", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Insert into sales table
                String insertSaleSql = "INSERT INTO sales (Product_ID, Quantity, Total_Amount, Sale_Date) VALUES (?, ?, ?, CURDATE());";
                try (PreparedStatement ps = con.prepareStatement(insertSaleSql)) {
                    ps.setInt(1, item.id);
                    ps.setInt(2, qty);
                    ps.setBigDecimal(3, BigDecimal.valueOf(totalAmount));
                    ps.executeUpdate();
                }

                // 2. Deduct product quantity
                String updateStockSql = "UPDATE product SET Quantity = Quantity - ? WHERE Product_ID = ?;";
                try (PreparedStatement ps = con.prepareStatement(updateStockSql)) {
                    ps.setInt(1, qty);
                    ps.setInt(2, item.id);
                    ps.executeUpdate();
                }

                // 3. Log into inventory_history
                String logSql = "INSERT INTO inventory_history (Product_ID, Stock_In, Stock_Out, Date) VALUES (?, 0, ?, CURDATE());";
                try (PreparedStatement ps = con.prepareStatement(logSql)) {
                    ps.setInt(1, item.id);
                    ps.setInt(2, qty);
                    ps.executeUpdate();
                }

                con.commit();

                JOptionPane.showMessageDialog(this,
                        String.format("Sale completed successfully!\nInvoice Amount: \u20B9%,.2f", totalAmount),
                        "Sale Success", JOptionPane.INFORMATION_MESSAGE);

                quantitySpinner.setValue(1);
                loadProductsCombo();
                loadSalesHistory();
                if (parentDashboard != null) parentDashboard.refreshData();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to complete sale: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads sales history transactions live from MySQL
     */
    public void loadSalesHistory() {
        salesTableModel.setRowCount(0);
        String sql = "SELECT s.Sale_ID, p.Product_Name, s.Quantity, s.Total_Amount, s.Sale_Date " +
                "FROM sales s " +
                "JOIN product p ON s.Product_ID = p.Product_ID " +
                "ORDER BY s.Sale_ID DESC;";

        double totalRevenue = 0;
        int count = 0;

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("Sale_ID");
                String name = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                double amount = rs.getDouble("Total_Amount");
                Date date = rs.getDate("Sale_Date");

                salesTableModel.addRow(new Object[]{
                        id,
                        name,
                        qty,
                        String.format("\u20B9%,.2f", amount),
                        date != null ? date.toString() : "-"
                });

                totalRevenue += amount;
                count++;
            }

            transactionCountLabel.setText("(" + count + " sales)");
            totalRevenueLabel.setText(String.format("Total Revenue: \u20B9%,.2f", totalRevenue));

        } catch (SQLException e) {
            System.err.println("Error querying sales history: " + e.getMessage());
        }
    }

    private static class ProductSaleItem {
        final int id;
        final String name;
        final double price;
        final int stock;

        ProductSaleItem(int id, String name, double price, int stock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.stock = stock;
        }

        @Override
        public String toString() {
            return name + " (Stock: " + stock + ") - \u20B9" + String.format("%.2f", price);
        }
    }
}
