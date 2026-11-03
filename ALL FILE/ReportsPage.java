import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executive Analytics & Reports Page
 * Provides financial summaries, top selling products, and category-level inventory distribution.
 */
public class ReportsPage extends JPanel {

    private final Dashboard parentDashboard;

    // Stat metric labels
    private JLabel valuationLabel;
    private JLabel costLabel;
    private JLabel revenueLabel;
    private JLabel profitLabel;

    // Tables
    private DefaultTableModel topSellingTableModel;
    private DefaultTableModel categoryTableModel;

    public ReportsPage(Dashboard parentDashboard) {
        this.parentDashboard = parentDashboard;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_LIGHT);
        setBorder(new EmptyBorder(10, 5, 10, 5));

        initComponents();
        loadReportData();
    }

    private void initComponents() {
        // --- 1. Top Bar ---
        JPanel topPanel = UIHelper.createCardPanel();
        topPanel.setLayout(new BorderLayout(15, 0));

        JLabel title = new JLabel("\uD83D\uDCC8  Analytics & Executive Business Reports");
        title.setFont(UIHelper.FONT_SUBHEADER);
        title.setForeground(UIHelper.TEXT_NAVY);
        topPanel.add(title, BorderLayout.WEST);

        JButton refreshBtn = UIHelper.createPrimaryButton("\uD83D\uDD04  Refresh Reports");
        refreshBtn.addActionListener(e -> {
            loadReportData();
            if (parentDashboard != null) parentDashboard.refreshData();
        });
        topPanel.add(refreshBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- 2. Center Content Scrollable ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UIHelper.BG_LIGHT);

        // 4 KPI Summary Cards Row
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiRow.setBackground(UIHelper.BG_LIGHT);

        valuationLabel = new JLabel("\u20B90.00");
        costLabel = new JLabel("\u20B90.00");
        revenueLabel = new JLabel("\u20B90.00");
        profitLabel = new JLabel("\u20B90.00");

        kpiRow.add(createMetricCard("INVENTORY VALUATION", valuationLabel, UIHelper.BLUE_PRIMARY));
        kpiRow.add(createMetricCard("COST OF INVENTORY", costLabel, UIHelper.ORANGE_WARNING));
        kpiRow.add(createMetricCard("LIFETIME SALES REVENUE", revenueLabel, UIHelper.GREEN_SUCCESS));
        kpiRow.add(createMetricCard("POTENTIAL MARGIN", profitLabel, new Color(147, 51, 234)));

        centerPanel.add(kpiRow);
        centerPanel.add(Box.createVerticalStrut(18));

        // Two Analytics Tables Row
        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 18, 0));
        tablesRow.setBackground(UIHelper.BG_LIGHT);

        // Left: Top Selling Products
        JPanel topSellingCard = UIHelper.createCardPanel();
        topSellingCard.setLayout(new BorderLayout(0, 10));

        JLabel topSellingTitle = new JLabel("\uD83C\uDFC6  Top Selling Products");
        topSellingTitle.setFont(UIHelper.FONT_SUBHEADER);
        topSellingTitle.setForeground(UIHelper.TEXT_NAVY);
        topSellingCard.add(topSellingTitle, BorderLayout.NORTH);

        String[] topColumns = {"Product Name", "Units Sold", "Total Revenue"};
        topSellingTableModel = new DefaultTableModel(topColumns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable topTable = new JTable(topSellingTableModel);
        UIHelper.styleTable(topTable);

        // Bold green for revenue
        topTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(UIHelper.COLOR_PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UIHelper.TEXT_NAVY);
                    if (col == 2) {
                        c.setForeground(UIHelper.GREEN_SUCCESS);
                        setFont(UIHelper.FONT_BODY_BOLD);
                    }
                }
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        JScrollPane topScroll = new JScrollPane(topTable);
        topScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        topSellingCard.add(topScroll, BorderLayout.CENTER);
        tablesRow.add(topSellingCard);

        // Right: Category Distribution
        JPanel catCard = UIHelper.createCardPanel();
        catCard.setLayout(new BorderLayout(0, 10));

        JLabel catTitle = new JLabel("\uD83D\uDCCA  Category Breakdown");
        catTitle.setFont(UIHelper.FONT_SUBHEADER);
        catTitle.setForeground(UIHelper.TEXT_NAVY);
        catCard.add(catTitle, BorderLayout.NORTH);

        String[] catColumns = {"Category", "Product Varieties", "Total Stock (Units)"};
        categoryTableModel = new DefaultTableModel(catColumns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(categoryTableModel);
        UIHelper.styleTable(catTable);

        catTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(UIHelper.COLOR_PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UIHelper.TEXT_NAVY);
                }
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        JScrollPane catScroll = new JScrollPane(catTable);
        catScroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT));
        catCard.add(catScroll, BorderLayout.CENTER);
        tablesRow.add(catCard);

        centerPanel.add(tablesRow);

        JScrollPane mainScroll = new JScrollPane(centerPanel);
        mainScroll.setBorder(null);
        mainScroll.setBackground(UIHelper.BG_LIGHT);
        mainScroll.getViewport().setBackground(UIHelper.BG_LIGHT);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(mainScroll, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color borderColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(180, 105));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, borderColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIHelper.BORDER_LIGHT, 1),
                        new EmptyBorder(14, 16, 14, 16)
                )
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIHelper.FONT_BODY_BOLD);
        titleLabel.setForeground(UIHelper.TEXT_GRAY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(UIHelper.TEXT_NAVY);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Loads live analytical metrics and tables from MySQL
     */
    public void loadReportData() {
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement()) {

            // 1. Total Valuation & Cost
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(Quantity * Selling_Price), SUM(Quantity * Buying_Price) FROM product;")) {
                if (rs.next()) {
                    double val = rs.getDouble(1);
                    double cost = rs.getDouble(2);
                    valuationLabel.setText(String.format("\u20B9%,.2f", val));
                    costLabel.setText(String.format("\u20B9%,.2f", cost));
                    profitLabel.setText(String.format("\u20B9%,.2f", (val - cost)));
                }
            }

            // 2. Lifetime Revenue
            try (ResultSet rs = stmt.executeQuery("SELECT SUM(Total_Amount) FROM sales;")) {
                if (rs.next()) {
                    double rev = rs.getDouble(1);
                    revenueLabel.setText(String.format("\u20B9%,.2f", rev));
                }
            }

            // 3. Top Selling Products
            topSellingTableModel.setRowCount(0);
            String topSql = "SELECT p.Product_Name, SUM(s.Quantity) AS units_sold, SUM(s.Total_Amount) AS total_revenue " +
                    "FROM sales s " +
                    "JOIN product p ON s.Product_ID = p.Product_ID " +
                    "GROUP BY p.Product_ID, p.Product_Name " +
                    "ORDER BY units_sold DESC " +
                    "LIMIT 6;";
            try (ResultSet rs = stmt.executeQuery(topSql)) {
                while (rs.next()) {
                    topSellingTableModel.addRow(new Object[]{
                            rs.getString("Product_Name"),
                            rs.getInt("units_sold") + " units",
                            String.format("\u20B9%,.2f", rs.getDouble("total_revenue"))
                    });
                }
            }

            // 4. Category breakdown
            categoryTableModel.setRowCount(0);
            String catSql = "SELECT Category, COUNT(*) AS count, SUM(Quantity) AS total_stock " +
                    "FROM product " +
                    "GROUP BY Category " +
                    "ORDER BY total_stock DESC;";
            try (ResultSet rs = stmt.executeQuery(catSql)) {
                while (rs.next()) {
                    String cat = rs.getString("Category");
                    categoryTableModel.addRow(new Object[]{
                            cat != null ? cat : "General",
                            rs.getInt("count") + " products",
                            rs.getInt("total_stock") + " units in stock"
                    });
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading report metrics: " + e.getMessage());
        }
    }
}
