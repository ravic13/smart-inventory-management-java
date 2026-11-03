import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enterprise Inventory Management Dashboard
 * Features a Navy sidebar with highlighted navigation, 5 live SQL KPI stat cards,
 * Low Stock Alerts table, Recent Sales table, and real MySQL JDBC connectivity.
 *
 * All data is queried LIVE from the MySQL database.
 * No hardcoded numbers or fake products.
 */
public class Dashboard extends JFrame {

    // ==========================================
    // DATABASE CONFIGURATION
    // ==========================================
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";

    // Set your MySQL password here (or pass via VM arg -Ddb.password=yourpass or ENV DB_PASSWORD):
    private static String dbPassword = "ravi@2025";

    // Persistent Database Connection kept open for session
    private Connection connection = null;

    // User context
    private final String loggedInUsername;
    private final String loggedInRole;

    // Exclusive Modern Theme Colors - High Contrast, Clean & Visible at First Glance
    private static final Color NAVY_SIDEBAR = new Color(15, 23, 42);          // #0F172A Deep Obsidian Navy
    private static final Color NAVY_MENU_BG = new Color(30, 41, 59);          // #1E293B High-contrast elevated button card (#1E293B)
    private static final Color NAVY_MENU_BORDER = new Color(71, 85, 105);     // #475569 Clear crisp border
    private static final Color NAVY_MENU_HOVER = new Color(51, 65, 85);       // #334155 Interactive hover
    private static final Color NAVY_MENU_HOVER_BORDER = new Color(96, 165, 250); // #60A5FA Sky Blue glow
    private static final Color BRIGHT_BLUE_ACCENT = new Color(37, 99, 235);   // #2563EB Blue Primary Action / Active Nav
    private static final Color CYAN_ACCENT_STRIP = new Color(56, 189, 248);   // #38BDF8 Neon Cyan Active Indicator
    private static final Color BG_LIGHT = new Color(248, 250, 252);           // #F8FAFC Ultra-clean canvas
    private static final Color TEXT_NAVY = new Color(15, 23, 42);             // #0F172A Sharp dark headings
    private static final Color TEXT_GRAY = new Color(100, 116, 139);          // #64748B Secondary body text
    private static final Color CARD_BORDER_BLUE = new Color(37, 99, 235);     // #2563EB Blue
    private static final Color CARD_BORDER_PURPLE = new Color(124, 58, 237);  // #7C3AED Violet
    private static final Color CARD_BORDER_RED = new Color(220, 38, 38);      // #DC2626 Crimson
    private static final Color CARD_BORDER_ORANGE = new Color(217, 119, 6);   // #D97706 Amber
    private static final Color CARD_BORDER_GREEN = new Color(22, 163, 74);    // #16A34A Green
    private static final Color LIGHT_RED_BG = new Color(254, 242, 242);      // #FEF2F2
    private static final Color LIGHT_RED_TEXT = new Color(185, 28, 28);       // #B91C1C

    // Dedicated fonts for navigation items: preserve emoji glyphs
    private static final Font NAV_EMOJI_FONT = new Font("Segoe UI Emoji", Font.PLAIN, 18);
    private static final Font NAV_TEXT_FONT = new Font("Segoe UI", Font.BOLD, 15);

    // Dynamic UI Labels for Stat Cards (Initial state: "-- N/A --")
    private JLabel totalProductsValueLabel;
    private JLabel totalSuppliersValueLabel;
    private JLabel lowStockItemsValueLabel;
    private JLabel pendingOrdersValueLabel;
    private JLabel todaySalesValueLabel;

    // Header Status & Timestamp Labels
    private JLabel systemStatusLabel;
    private JLabel lastUpdatedLabel;

    // Table Models (Populated solely via live SQL queries)
    private DefaultTableModel lowStockTableModel;
    private DefaultTableModel recentSalesTableModel;

    // Navigation tracking
    private JButton activeNavButton;
    private final List<JButton> navButtons = new ArrayList<>();

    // Multi-page navigation
    private CardLayout cardLayout;
    private JPanel pagesContainer;
    private String currentPageName = "Dashboard";
    private JLabel currentPageTitleLabel;

    private ProductsPage productsPage;
    private SuppliersPage suppliersPage;
    private InventoryPage inventoryPage;
    private SalesPage salesPage;
    private PurchaseOrdersPage purchaseOrdersPage;
    private ReportsPage reportsPage;

    /**
     * Default constructor for standalone launches
     */
    public Dashboard() {
        this("admin", "Admin role");
    }

    /**
     * Parameterized constructor storing user context
     */
    public Dashboard(String username, String role) {
        this.loggedInUsername = (username == null || username.trim().isEmpty()) ? "admin" : username.trim();
        this.loggedInRole = (role == null || role.trim().isEmpty()) ? "Admin role" : role.trim();

        initWindow();
        initComponents();

        // Connect and load fresh data on startup
        refreshData();

        setVisible(true);
    }

    // ==========================================
    // 1. DATABASE CONNECTION
    // ==========================================
    /**
     * Connects to MySQL 'inventory' database on localhost:3306.
     * Keeps the connection open for the session.
     * If connection fails, displays an error dialog and returns false.
     */
    public boolean connectToDatabase() {
        try {
            if (connection != null && !connection.isClosed() && connection.isValid(2)) {
                return true;
            }

            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Open live connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, dbPassword);
            return true;

        } catch (ClassNotFoundException e) {
            handleConnectionError("MySQL JDBC Driver not found.\nPlease ensure mysql-connector-j.jar is on the classpath.\nDetails: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            handleConnectionError("Could not connect to database.\n" + e.getMessage() +
                    "\n\nJDBC URL: " + DB_URL +
                    "\nUser: " + DB_USER +
                    "\n\nPlease verify that MySQL is running on localhost:3306 and database 'inventory' exists.");
            return false;
        } catch (Exception e) {
            handleConnectionError("Unexpected database connection error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handles connection failure gracefully without crashing the app:
     * - Shows error dialog: "Could not connect to database"
     * - All cards show: "-- N/A --"
     * - Tables show empty
     * - Status displays "Database Not Connected"
     */
    private void handleConnectionError(String errorMessage) {
        // Update status indicators
        if (systemStatusLabel != null) {
            systemStatusLabel.setText("● Database Disconnected");
            systemStatusLabel.setForeground(new Color(185, 28, 28));
            systemStatusLabel.setBackground(new Color(254, 242, 242));
            systemStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                    new EmptyBorder(4, 10, 4, 10)
            ));
        }
        if (lastUpdatedLabel != null) {
            lastUpdatedLabel.setText("Last Updated: Connection Failed");
            lastUpdatedLabel.setForeground(new Color(185, 28, 28));
        }

        // Reset all 5 stat cards to "-- N/A --"
        if (totalProductsValueLabel != null) totalProductsValueLabel.setText("-- N/A --");
        if (totalSuppliersValueLabel != null) totalSuppliersValueLabel.setText("-- N/A --");
        if (lowStockItemsValueLabel != null) lowStockItemsValueLabel.setText("-- N/A --");
        if (pendingOrdersValueLabel != null) pendingOrdersValueLabel.setText("-- N/A --");
        if (todaySalesValueLabel != null) todaySalesValueLabel.setText("-- N/A --");

        // Clear tables
        if (lowStockTableModel != null) lowStockTableModel.setRowCount(0);
        if (recentSalesTableModel != null) recentSalesTableModel.setRowCount(0);

        // Show non-fatal error dialog
        JOptionPane.showMessageDialog(
                this,
                "Could not connect to database.\n\n" + errorMessage,
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // ==========================================
    // 2. REFRESH DATA (LIVE SQL QUERIES)
    // ==========================================
    /**
     * Re-executes all live SQL queries, updates stat cards, fills tables,
     * and updates the "Last Updated: HH:MM:SS" timestamp.
     */
    public void refreshData() {
        // Ensure active database connection
        if (!connectToDatabase()) {
            return; // UI already put in safe "-- N/A --" disconnected state
        }

        boolean allQueriesSucceeded = true;

        // ----------------------------------------------------
        // Card 1: TOTAL PRODUCTS -> SELECT COUNT(*) FROM Product;
        // ----------------------------------------------------
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Product;")) {
            if (rs.next()) {
                totalProductsValueLabel.setText(String.format("%,d", rs.getInt(1)));
            } else {
                totalProductsValueLabel.setText("0");
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            totalProductsValueLabel.setText("-- N/A --");
            System.err.println("Error fetching Total Products: " + e.getMessage());
        }

        // ----------------------------------------------------
        // Card 2: SUPPLIERS -> SELECT COUNT(*) FROM Supplier;
        // ----------------------------------------------------
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Supplier;")) {
            if (rs.next()) {
                totalSuppliersValueLabel.setText(String.format("%,d", rs.getInt(1)));
            } else {
                totalSuppliersValueLabel.setText("0");
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            totalSuppliersValueLabel.setText("-- N/A --");
            System.err.println("Error fetching Suppliers: " + e.getMessage());
        }

        // ----------------------------------------------------
        // Card 3: LOW STOCK ITEMS -> SELECT COUNT(*) FROM Product WHERE Quantity <= Minimum_Stock;
        // ----------------------------------------------------
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Product WHERE Quantity <= Minimum_Stock;")) {
            if (rs.next()) {
                lowStockItemsValueLabel.setText(String.format("%,d", rs.getInt(1)));
            } else {
                lowStockItemsValueLabel.setText("0");
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            lowStockItemsValueLabel.setText("-- N/A --");
            System.err.println("Error fetching Low Stock count: " + e.getMessage());
        }

        // ----------------------------------------------------
        // Card 4: PENDING ORDERS -> SELECT COUNT(*) FROM Purchase_Order WHERE Status = 'Pending';
        // ----------------------------------------------------
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Purchase_Order WHERE Status = 'Pending';")) {
            if (rs.next()) {
                pendingOrdersValueLabel.setText(String.format("%,d", rs.getInt(1)));
            } else {
                pendingOrdersValueLabel.setText("0");
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            pendingOrdersValueLabel.setText("-- N/A --");
            System.err.println("Error fetching Pending Orders: " + e.getMessage());
        }

        // ----------------------------------------------------
        // Card 5: TODAY'S SALES -> SELECT SUM(Total_Amount) FROM Sales WHERE DATE(Sale_Date) = CURDATE();
        // ----------------------------------------------------
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SUM(Total_Amount) FROM Sales WHERE DATE(Sale_Date) = CURDATE();")) {
            if (rs.next()) {
                double todaySales = rs.getDouble(1);
                if (rs.wasNull()) {
                    todaySales = 0.0;
                }
                todaySalesValueLabel.setText(String.format("\u20B9%,.2f", todaySales));
            } else {
                todaySalesValueLabel.setText("\u20B90.00");
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            todaySalesValueLabel.setText("-- N/A --");
            System.err.println("Error fetching Today's Sales: " + e.getMessage());
        }

        // ----------------------------------------------------
        // 3. LOW STOCK ALERTS TABLE:
        // SELECT Product_ID, Product_Name, Quantity, Minimum_Stock
        // FROM Product WHERE Quantity <= Minimum_Stock ORDER BY Quantity ASC;
        // Columns: Product | Qty | Min | Status (Critical/Low based on severity)
        // ----------------------------------------------------
        lowStockTableModel.setRowCount(0);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT Product_ID, Product_Name, Quantity, Minimum_Stock " +
                     "FROM Product " +
                     "WHERE Quantity <= Minimum_Stock " +
                     "ORDER BY Quantity ASC;")) {
            while (rs.next()) {
                String productName = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                int minStock = rs.getInt("Minimum_Stock");

                // Determine severity status
                String status;
                if (qty <= 0) {
                    status = "Critical (Out of Stock)";
                } else if (qty <= Math.max(1, minStock / 2)) {
                    status = "Critical";
                } else {
                    status = "Low";
                }

                lowStockTableModel.addRow(new Object[]{productName, qty, minStock, status});
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            System.err.println("Error querying Low Stock alerts: " + e.getMessage());
        }

        // ----------------------------------------------------
        // 4. RECENT SALES TABLE:
        // SELECT s.Sale_ID, p.Product_Name, s.Quantity, s.Total_Amount, s.Sale_Date
        // FROM Sales s JOIN Product p ON s.Product_ID = p.Product_ID
        // ORDER BY s.Sale_Date DESC LIMIT 5;
        // Columns: Product | Qty | Amount
        // ----------------------------------------------------
        recentSalesTableModel.setRowCount(0);
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT s.Sale_ID, p.Product_Name, s.Quantity, s.Total_Amount, s.Sale_Date " +
                     "FROM Sales s " +
                     "JOIN Product p ON s.Product_ID = p.Product_ID " +
                     "ORDER BY s.Sale_Date DESC " +
                     "LIMIT 5;")) {
            while (rs.next()) {
                String productName = rs.getString("Product_Name");
                int qty = rs.getInt("Quantity");
                double amount = rs.getDouble("Total_Amount");

                recentSalesTableModel.addRow(new Object[]{
                        productName,
                        qty,
                        String.format("\u20B9%,.2f", amount)
                });
            }
        } catch (SQLException e) {
            allQueriesSucceeded = false;
            System.err.println("Error querying Recent Sales: " + e.getMessage());
        }

        // ----------------------------------------------------
        // Update Status & Timestamp
        // ----------------------------------------------------
        String timeString = new SimpleDateFormat("HH:mm:ss").format(new Date());
        if (lastUpdatedLabel != null) {
            lastUpdatedLabel.setText("Last Updated: " + timeString);
            lastUpdatedLabel.setForeground(new Color(100, 116, 139));
        }

        if (systemStatusLabel != null) {
            if (allQueriesSucceeded) {
                systemStatusLabel.setText("● Database Connected");
                systemStatusLabel.setForeground(new Color(5, 150, 105)); // Emerald
                systemStatusLabel.setBackground(new Color(236, 253, 245));
                systemStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(167, 243, 208), 1),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            } else {
                systemStatusLabel.setText("● Connected (Partial Query Error)");
                systemStatusLabel.setForeground(new Color(217, 119, 6)); // Amber
                systemStatusLabel.setBackground(new Color(254, 243, 199));
                systemStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(252, 211, 77), 1),
                        new EmptyBorder(4, 10, 4, 10)
                ));
            }
        }
    }

    /**
     * Backward-compatible alias for refreshData()
     */
    public void refresh() {
        refreshData();
    }

    // ==========================================
    // WINDOW SETUP & ICON
    // ==========================================
    private void initWindow() {
        setTitle("Enterprise Inventory Management System - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Full screen
        setMinimumSize(new Dimension(1150, 720));
        setLayout(new BorderLayout());

        // Load project icon if available
        try {
            File iconFile = new File("icons8-inventory-48.png");
            if (!iconFile.exists()) {
                iconFile = new File("icons/icons8-inventory-48.png");
            }
            if (!iconFile.exists()) {
                iconFile = new File("icons8-inventory-240.png");
            }
            if (iconFile.exists()) {
                setIconImage(new ImageIcon(iconFile.getAbsolutePath()).getImage());
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // MAIN UI LAYOUT
    // ==========================================
    private void initComponents() {
        // 1. LEFT SIDEBAR (BorderLayout.WEST)
        add(createSidebarPanel(), BorderLayout.WEST);

        // 2. MAIN CONTENT AREA (BorderLayout.CENTER)
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 15));
        mainContentPanel.setBackground(BG_LIGHT);
        mainContentPanel.setBorder(new EmptyBorder(15, 20, 20, 20));

        // Top Header
        mainContentPanel.add(createMainHeaderPanel(), BorderLayout.NORTH);

        // Central Multi-page Container with CardLayout
        cardLayout = new CardLayout();
        pagesContainer = new JPanel(cardLayout);
        pagesContainer.setBackground(BG_LIGHT);

        // Page 1: Dashboard Overview Panel (Cards + Alerts & Sales tables)
        pagesContainer.add(createDashboardOverviewPanel(), "Dashboard");

        // Page 2: Products Management Page
        productsPage = new ProductsPage(this);
        pagesContainer.add(productsPage, "Products");

        // Page 3: Suppliers Management Page
        suppliersPage = new SuppliersPage(this);
        pagesContainer.add(suppliersPage, "Suppliers");

        // Page 4: Inventory & Stock Control Page
        inventoryPage = new InventoryPage(this);
        pagesContainer.add(inventoryPage, "Inventory");

        // Page 5: Sales & POS Billing Page
        salesPage = new SalesPage(this);
        pagesContainer.add(salesPage, "Sales");

        // Page 6: Purchase Orders Page
        purchaseOrdersPage = new PurchaseOrdersPage(this);
        pagesContainer.add(purchaseOrdersPage, "Purchase Orders");

        // Page 7: Analytics & Reports Page
        reportsPage = new ReportsPage(this);
        pagesContainer.add(reportsPage, "Reports");

        mainContentPanel.add(pagesContainer, BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    /**
     * Builds the main Dashboard overview panel (Stat cards and quick overview tables)
     */
    private JPanel createDashboardOverviewPanel() {
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setBackground(BG_LIGHT);

        // Stat Cards Row
        centerContainer.add(createStatCardsRow());
        centerContainer.add(Box.createVerticalStrut(20));

        // Tables Row (Low Stock Alerts & Recent Sales)
        centerContainer.add(createTablesPanel());

        // Responsive Scroll Pane
        JScrollPane scrollPane = new JScrollPane(centerContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_LIGHT);
        scrollPane.getViewport().setBackground(BG_LIGHT);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_LIGHT);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Switches the active view to the requested page and reloads fresh database records
     */
    public void showPage(String pageName) {
        if (pageName == null) return;
        pageName = pageName.trim();
        this.currentPageName = pageName;
        if (cardLayout != null && pagesContainer != null) {
            cardLayout.show(pagesContainer, pageName);
            pagesContainer.revalidate();
            pagesContainer.repaint();
        }
        if (currentPageTitleLabel != null) {
            currentPageTitleLabel.setText(pageName);
        }

        // Live database reload on page navigation
        if ("Dashboard".equalsIgnoreCase(pageName)) {
            refreshData();
        } else if ("Products".equalsIgnoreCase(pageName) && productsPage != null) {
            productsPage.loadProducts();
        } else if ("Suppliers".equalsIgnoreCase(pageName) && suppliersPage != null) {
            suppliersPage.loadSuppliers();
        } else if ("Inventory".equalsIgnoreCase(pageName) && inventoryPage != null) {
            inventoryPage.loadInventory();
        } else if ("Sales".equalsIgnoreCase(pageName) && salesPage != null) {
            salesPage.loadProductsCombo();
            salesPage.loadSalesHistory();
        } else if ("Purchase Orders".equalsIgnoreCase(pageName) && purchaseOrdersPage != null) {
            purchaseOrdersPage.loadOrders();
        } else if ("Reports".equalsIgnoreCase(pageName) && reportsPage != null) {
            reportsPage.loadReportData();
        }
    }

    // ==========================================
    // 7. SIDEBAR MENU (FIXED & HIGH-CONTRAST)
    // ==========================================
    /**
     * Builds the Sidebar with fixed, crystal-clear buttons:
     * - "Dashboard" (always displays text clearly, not blank)
     * - "Products", "Suppliers", "Inventory", "Sales", "Purchase Orders", "Reports"
     * - Each with dedicated emoji icon
     * - Selected button highlighted in bright blue
     */
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(NAVY_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(20, 40, 70)));

        // Top Header of Sidebar
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(NAVY_SIDEBAR);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, NAVY_MENU_BORDER),
                new EmptyBorder(22, 20, 22, 20)
        ));

        JLabel logoLabel = new JLabel("Smart Inventory");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logoLabel.setForeground(Color.WHITE);

        try {
            File iconFile = new File("icons8-inventory-48.png");
            if (!iconFile.exists()) {
                iconFile = new File("icons/icons8-inventory-48.png");
            }
            if (iconFile.exists()) {
                ImageIcon origIcon = new ImageIcon(iconFile.getAbsolutePath());
                Image scaled = origIcon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
                logoLabel.setIconTextGap(10);
            }
        } catch (Exception ignored) {}

        JLabel subHeader = new JLabel("Enterprise Edition");
        subHeader.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subHeader.setForeground(new Color(96, 165, 250));  // #60A5FA vivid sky blue
        subHeader.setBorder(new EmptyBorder(5, 38, 0, 0));

        headerPanel.add(logoLabel);
        headerPanel.add(subHeader);
        sidebar.add(headerPanel, BorderLayout.NORTH);

        // Navigation Menu Buttons — discrete elevated cards with clear borders & spacing
        JPanel navMenuPanel = new JPanel();
        navMenuPanel.setLayout(new GridLayout(7, 1, 0, 8));
        navMenuPanel.setBackground(NAVY_SIDEBAR);
        navMenuPanel.setBorder(new EmptyBorder(16, 12, 16, 12));

        String[][] navItems = {
                {"\uD83D\uDCCA", "Dashboard"},
                {"\uD83D\uDCE6", "Products"},
                {"\uD83D\uDC65", "Suppliers"},
                {"\uD83D\uDCCB", "Inventory"},
                {"\uD83D\uDCB0", "Sales"},
                {"\uD83D\uDCD1", "Purchase Orders"},
                {"\uD83D\uDCC8", "Reports"}
        };

        navButtons.clear();

        for (int i = 0; i < navItems.length; i++) {
            String emoji = navItems[i][0];
            String title = navItems[i][1];
            boolean isSelected = (i == 0); // "Dashboard" selected by default

            JButton navBtn = createNavButton(emoji, title, isSelected);
            navButtons.add(navBtn);
            navMenuPanel.add(navBtn);

            if (isSelected) {
                activeNavButton = navBtn;
            }
        }

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(NAVY_SIDEBAR);
        centerWrapper.add(navMenuPanel, BorderLayout.NORTH);
        sidebar.add(centerWrapper, BorderLayout.CENTER);

        // Bottom Section: Logout Button (High-visibility Red #DC2626 card)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(NAVY_SIDEBAR);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 41, 59)),
                new EmptyBorder(14, 12, 18, 12)
        ));

        JButton sidebarLogoutBtn = new JButton("\uD83D\uDEAA  Sign Out");
        UIHelper.styleButton(sidebarLogoutBtn, UIHelper.COLOR_DANGER, Color.WHITE, new Font("Segoe UI Emoji", Font.BOLD, 14), new Insets(10, 14, 10, 14));
        sidebarLogoutBtn.setPreferredSize(new Dimension(0, 44));
        sidebarLogoutBtn.addActionListener(e -> performLogout());

        bottomPanel.add(sidebarLogoutBtn, BorderLayout.CENTER);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    /**
     * Creates a composite sidebar navigation button ensuring that the text (e.g. "Dashboard")
     * is ALWAYS prominent, high-contrast, and clearly visible at first glance, and the emoji
     * icon font is strictly preserved.
     */
    private JButton createNavButton(String emoji, String text, boolean isSelected) {
        JButton btn = new JButton();
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setLayout(new BorderLayout(14, 0));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setName(text);

        // Emoji icon — clear, distinct white with dedicated emoji font
        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(NAV_EMOJI_FONT);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setBorder(new EmptyBorder(0, 8, 0, 0));

        // Title text — crisp, prominent, bold 15px white
        JLabel textLabel = new JLabel(text);
        textLabel.setFont(NAV_TEXT_FONT);
        textLabel.setForeground(Color.WHITE);

        btn.putClientProperty("iconLabel", iconLabel);
        btn.putClientProperty("textLabel", textLabel);

        btn.add(iconLabel, BorderLayout.WEST);
        btn.add(textLabel, BorderLayout.CENTER);

        if (isSelected) {
            // Active: vivid royal blue bg + neon cyan left-strip border
            btn.setBackground(BRIGHT_BLUE_ACCENT);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 5, 0, 0, CYAN_ACCENT_STRIP),
                    new EmptyBorder(10, 10, 10, 14)
            ));
        } else {
            // Inactive: discrete elevated card with high-contrast visible border
            btn.setBackground(NAVY_MENU_BG);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(NAVY_MENU_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)
            ));
        }

        // Action when button is activated
        btn.addActionListener(e -> {
            selectNavButton(btn);
            showPage(text);
        });

        // Mouse hover effects & click forwarding (including clicking directly on icon/text)
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn != activeNavButton) {
                    btn.setBackground(NAVY_MENU_HOVER);
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(NAVY_MENU_HOVER_BORDER, 1),
                            new EmptyBorder(10, 14, 10, 14)
                    ));
                    textLabel.setForeground(Color.WHITE);
                    iconLabel.setForeground(Color.WHITE);
                    btn.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn != activeNavButton) {
                    btn.setBackground(NAVY_MENU_BG);
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(NAVY_MENU_BORDER, 1),
                            new EmptyBorder(10, 14, 10, 14)
                    ));
                    textLabel.setForeground(Color.WHITE);
                    iconLabel.setForeground(Color.WHITE);
                    btn.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectNavButton(btn);
                showPage(text);
            }
        };

        btn.addMouseListener(mouseHandler);
        iconLabel.addMouseListener(mouseHandler);
        textLabel.addMouseListener(mouseHandler);

        return btn;
    }

    private void selectNavButton(JButton targetBtn) {
        // Reset previously active button to inactive card style
        if (activeNavButton != null) {
            activeNavButton.setBackground(NAVY_MENU_BG);
            activeNavButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(NAVY_MENU_BORDER, 1),
                    new EmptyBorder(10, 14, 10, 14)
            ));
            refreshNavButtonLabels(activeNavButton);
            activeNavButton.repaint();
        }

        // Set new active button — vivid royal blue with cyan left indicator strip
        activeNavButton = targetBtn;
        activeNavButton.setBackground(BRIGHT_BLUE_ACCENT);
        activeNavButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, CYAN_ACCENT_STRIP),
                new EmptyBorder(10, 10, 10, 14)
        ));
        refreshNavButtonLabels(activeNavButton);
        activeNavButton.repaint();
    }

    /**
     * Preserves the Segoe UI Emoji font on the icon label and Segoe UI font on text label,
     * preventing icons from disappearing when items are clicked.
     */
    private void refreshNavButtonLabels(JButton btn) {
        if (btn == null) return;
        JLabel iconLbl = (JLabel) btn.getClientProperty("iconLabel");
        JLabel textLbl = (JLabel) btn.getClientProperty("textLabel");

        if (iconLbl != null) {
            iconLbl.setFont(NAV_EMOJI_FONT);
            iconLbl.setForeground(Color.WHITE);
        }
        if (textLbl != null) {
            textLbl.setFont(NAV_TEXT_FONT);
            textLbl.setForeground(Color.WHITE);
        }

        // Extra safeguard: inspect all child labels and maintain correct fonts
        for (Component comp : btn.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel lbl = (JLabel) comp;
                boolean isWest = comp.equals(((BorderLayout) btn.getLayout()).getLayoutComponent(BorderLayout.WEST));
                if (isWest) {
                    lbl.setFont(NAV_EMOJI_FONT);
                } else {
                    lbl.setFont(NAV_TEXT_FONT);
                }
                lbl.setForeground(Color.WHITE);
            }
        }
    }

    // Legacy stub — font styling now fully handled in refreshNavButtonLabels
    private void setButtonTextFont(JButton btn, int fontStyle) {
        for (Component comp : btn.getComponents()) {
            boolean isWest = comp.equals(((BorderLayout) btn.getLayout()).getLayoutComponent(BorderLayout.WEST));
            if (comp instanceof JLabel && !isWest) {
                comp.setFont(new Font("Segoe UI", fontStyle, 15));
            }
        }
    }

    // ==========================================
    // MAIN HEADER PANEL (Title, Status, Refresh, User Badge)
    // ==========================================
    private JPanel createMainHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(14, 22, 14, 22)
        ));

        // Left: "Dashboard" Title, Status Badge & Timestamp
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setBackground(Color.WHITE);

        currentPageTitleLabel = new JLabel("Dashboard");
        currentPageTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        currentPageTitleLabel.setForeground(TEXT_NAVY);

        systemStatusLabel = new JLabel("● Connecting...");
        systemStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        systemStatusLabel.setForeground(new Color(230, 126, 34));

        lastUpdatedLabel = new JLabel("Last Updated: --:--:--");
        lastUpdatedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lastUpdatedLabel.setForeground(TEXT_GRAY);

        left.add(currentPageTitleLabel);
        left.add(systemStatusLabel);
        left.add(new JSeparator(SwingConstants.VERTICAL));
        left.add(lastUpdatedLabel);
        headerPanel.add(left, BorderLayout.WEST);

        // Right: Refresh Button, DB Config, User Info Badge, Logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(Color.WHITE);

        // 6. REFRESH BUTTON (Solid Blue #2563EB Primary Action)
        JButton refreshBtn = new JButton("\uD83D\uDD04  Refresh");
        UIHelper.styleButton(refreshBtn, UIHelper.COLOR_PRIMARY, Color.WHITE, new Font("Segoe UI Emoji", Font.BOLD, 13), new Insets(7, 16, 7, 16));
        refreshBtn.addActionListener(e -> {
            refreshData();
            if ("Products".equals(currentPageName) && productsPage != null) {
                productsPage.loadProducts();
            } else if ("Suppliers".equals(currentPageName) && suppliersPage != null) {
                suppliersPage.loadSuppliers();
            } else if ("Inventory".equals(currentPageName) && inventoryPage != null) {
                inventoryPage.loadInventory();
            } else if ("Sales".equals(currentPageName) && salesPage != null) {
                salesPage.loadProductsCombo();
                salesPage.loadSalesHistory();
            } else if ("Purchase Orders".equals(currentPageName) && purchaseOrdersPage != null) {
                purchaseOrdersPage.loadOrders();
            } else if ("Reports".equals(currentPageName) && reportsPage != null) {
                reportsPage.loadReportData();
            }
        });

        // Quick DB Password Configuration Button (Slate #475569 Secondary Action)
        JButton dbConfigBtn = new JButton("\u2699\uFE0F  DB Config");
        UIHelper.styleButton(dbConfigBtn, UIHelper.COLOR_SECONDARY, Color.WHITE, new Font("Segoe UI Emoji", Font.BOLD, 12), new Insets(7, 14, 7, 14));
        dbConfigBtn.setToolTipText("Change MySQL Connection / Password");
        dbConfigBtn.addActionListener(e -> promptDatabaseConfig());

        // User info badge: "admin (Admin role)"
        String userBadgeText = "\uD83D\uDC64  " + loggedInUsername + " (" + loggedInRole + ")";
        JLabel userBadge = new JLabel(userBadgeText);
        userBadge.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        userBadge.setForeground(TEXT_NAVY);
        userBadge.setBackground(new Color(241, 245, 249));
        userBadge.setOpaque(true);
        userBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(7, 14, 7, 14)
        ));

        // Header Logout Button (Red #DC2626 Danger Action)
        JButton topLogoutBtn = new JButton("Logout");
        UIHelper.styleButton(topLogoutBtn, UIHelper.COLOR_DANGER, Color.WHITE, new Font("Segoe UI", Font.BOLD, 13), new Insets(6, 16, 6, 16));
        topLogoutBtn.setPreferredSize(new Dimension(88, 36));
        topLogoutBtn.addActionListener(e -> performLogout());

        right.add(refreshBtn);
        right.add(dbConfigBtn);
        right.add(userBadge);
        right.add(topLogoutBtn);

        headerPanel.add(right, BorderLayout.EAST);
        return headerPanel;
    }

    /**
     * Allows user to view or change the MySQL root password at runtime and reconnect.
     */
    private void promptDatabaseConfig() {
        JPasswordField passwordField = new JPasswordField(dbPassword);
        Object[] message = {
                "Database URL: " + DB_URL,
                "MySQL Username: " + DB_USER,
                "Enter MySQL root password:", passwordField
        };
        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Configure MySQL Database Connection",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (option == JOptionPane.OK_OPTION) {
            dbPassword = new String(passwordField.getPassword());
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (Exception ignored) {}
            connection = null;
            refreshData();
        }
    }

    // ==========================================
    // 2. FIVE STAT CARDS (Horizontal Row)
    // ==========================================
    private JPanel createStatCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 16, 0));
        row.setBackground(BG_LIGHT);

        // Card 1: TOTAL PRODUCTS (Blue border)
        totalProductsValueLabel = new JLabel("-- N/A --");
        row.add(createStatCard("TOTAL PRODUCTS", totalProductsValueLabel, CARD_BORDER_BLUE));

        // Card 2: SUPPLIERS (Purple border)
        totalSuppliersValueLabel = new JLabel("-- N/A --");
        row.add(createStatCard("SUPPLIERS", totalSuppliersValueLabel, CARD_BORDER_PURPLE));

        // Card 3: LOW STOCK ITEMS (Red border)
        lowStockItemsValueLabel = new JLabel("-- N/A --");
        row.add(createStatCard("LOW STOCK ITEMS", lowStockItemsValueLabel, CARD_BORDER_RED));

        // Card 4: PENDING ORDERS (Orange border)
        pendingOrdersValueLabel = new JLabel("-- N/A --");
        row.add(createStatCard("PENDING ORDERS", pendingOrdersValueLabel, CARD_BORDER_ORANGE));

        // Card 5: TODAY'S SALES (Green border)
        todaySalesValueLabel = new JLabel("-- N/A --");
        row.add(createStatCard("TODAY'S SALES", todaySalesValueLabel, CARD_BORDER_GREEN));

        return row;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color topBorderColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(180, 110));

        // 4px colored accent border at top
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, topBorderColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                        new EmptyBorder(16, 18, 16, 18)
                )
        ));

        // Card title: Bold 13px, high-contrast slate #475569
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(71, 85, 105));

        // Card number: Bold 26px, dark navy #0F172A
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(TEXT_NAVY);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // ==========================================
    // TABLES PANEL (Low Stock Alerts & Recent Sales)
    // ==========================================
    private JPanel createTablesPanel() {
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tablesPanel.setBackground(BG_LIGHT);

        // LEFT PANEL: Low Stock Alerts
        tablesPanel.add(createLowStockPanel());

        // RIGHT PANEL: Recent Sales
        tablesPanel.add(createRecentSalesPanel());

        return tablesPanel;
    }

    // ==========================================
    // 3. LOW STOCK ALERTS TABLE
    // ==========================================
    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel title = new JLabel("\u26A0\uFE0F  Low Stock Alerts");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        title.setForeground(TEXT_NAVY);
        panel.add(title, BorderLayout.NORTH);

        // Columns: Product | Qty | Min | Status
        String[] columns = {"Product", "Qty", "Min", "Status"};
        lowStockTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(lowStockTableModel);
        styleTable(table);

        // Red background for low stock rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(LIGHT_RED_BG);
                    c.setForeground(LIGHT_RED_TEXT);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else {
                    c.setBackground(new Color(254, 202, 202));
                    c.setForeground(new Color(127, 29, 29));
                }
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scroll.setPreferredSize(new Dimension(0, 360));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ==========================================
    // 4. RECENT SALES TABLE
    // ==========================================
    private JPanel createRecentSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel title = new JLabel("\uD83D\uDCB3  Recent Sales");
        title.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        title.setForeground(TEXT_NAVY);
        panel.add(title, BorderLayout.NORTH);

        // Columns: Product | Qty | Amount
        String[] columns = {"Product", "Qty", "Amount"};
        recentSalesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = new JTable(recentSalesTableModel);
        styleTable(table);

        // Alternating row styling & green currency for amount
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (isSel) {
                    c.setBackground(new Color(37, 99, 235));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_NAVY);
                    if (col == 2) {
                        c.setForeground(new Color(22, 163, 74)); // Green amount
                        setFont(new Font("Segoe UI", Font.BOLD, 13));
                    }
                }
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scroll.setPreferredSize(new Dimension(0, 360));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // Table Styling Helper
    private void styleTable(JTable table) {
        table.setRowHeight(36);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(37, 99, 235));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new TableCellRenderer() {
            private final DefaultTableCellRenderer delegate = new DefaultTableCellRenderer();
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                JLabel l = (JLabel) delegate.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(30, 41, 59));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setBorder(new EmptyBorder(8, 12, 8, 12));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
    }

    // Logout Helper
    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of Smart Inventory?",
                "Logout Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (Exception ignored) {}
            dispose();
            new Login();
        }
    }

    // ==========================================
    // MAIN METHOD (Test Run Standalone)
    // ==========================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new Dashboard("admin", "Admin role"));
    }
}
