-- =====================================================================
-- Inventory Management System Database Schema & Sample Data
-- Database Name: inventory
-- Compatible with MySQL 5.7+ and MySQL 8.x
-- =====================================================================

CREATE DATABASE IF NOT EXISTS inventory;
USE inventory;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `user` (
    `User_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Username` VARCHAR(50) NOT NULL UNIQUE,
    `Password` VARCHAR(100) NOT NULL,
    `Role` VARCHAR(30) NOT NULL DEFAULT 'Admin'
);

-- 2. Suppliers Table
CREATE TABLE IF NOT EXISTS `supplier` (
    `Supplier_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Supplier_Name` VARCHAR(100) NOT NULL,
    `Phone` VARCHAR(25),
    `Email` VARCHAR(100),
    `Address` TEXT
);

-- 3. Products Table
CREATE TABLE IF NOT EXISTS `product` (
    `Product_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Product_Name` VARCHAR(100) NOT NULL,
    `Category` VARCHAR(50) DEFAULT 'General',
    `Buying_Price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `Selling_Price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `Quantity` INT NOT NULL DEFAULT 0,
    `Minimum_Stock` INT NOT NULL DEFAULT 5,
    `Supplier_ID` INT,
    FOREIGN KEY (`Supplier_ID`) REFERENCES `supplier`(`Supplier_ID`) ON DELETE SET NULL
);

-- 4. Purchase Orders Table
CREATE TABLE IF NOT EXISTS `purchase_order` (
    `Order_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Product_ID` INT NOT NULL,
    `Supplier_ID` INT,
    `Quantity` INT NOT NULL,
    `Order_Date` DATE NOT NULL,
    `Status` VARCHAR(25) NOT NULL DEFAULT 'Pending',
    FOREIGN KEY (`Product_ID`) REFERENCES `product`(`Product_ID`) ON DELETE CASCADE,
    FOREIGN KEY (`Supplier_ID`) REFERENCES `supplier`(`Supplier_ID`) ON DELETE SET NULL
);

-- 5. Sales Transactions Table
CREATE TABLE IF NOT EXISTS `sales` (
    `Sale_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Product_ID` INT NOT NULL,
    `Quantity` INT NOT NULL,
    `Total_Amount` DECIMAL(10, 2) NOT NULL,
    `Sale_Date` DATE NOT NULL,
    FOREIGN KEY (`Product_ID`) REFERENCES `product`(`Product_ID`) ON DELETE CASCADE
);

-- 6. Inventory History / Audit Table
CREATE TABLE IF NOT EXISTS `inventory_history` (
    `History_ID` INT AUTO_INCREMENT PRIMARY KEY,
    `Product_ID` INT NOT NULL,
    `Stock_In` INT DEFAULT 0,
    `Stock_Out` INT DEFAULT 0,
    `Date` DATE NOT NULL,
    FOREIGN KEY (`Product_ID`) REFERENCES `product`(`Product_ID`) ON DELETE CASCADE
);

-- =====================================================================
-- Seed Initial Sample Data (Insert if empty)
-- =====================================================================

-- Default Users (admin & staff)
INSERT IGNORE INTO `user` (`User_ID`, `Username`, `Password`, `Role`) VALUES
(1, 'admin', 'admin123', 'Admin'),
(2, 'manager', 'manager123', 'Manager'),
(3, 'staff', 'staff123', 'Staff');

-- Sample Suppliers
INSERT IGNORE INTO `supplier` (`Supplier_ID`, `Supplier_Name`, `Phone`, `Email`, `Address`) VALUES
(1, 'TechLogix Electronics Ltd', '+91 98765 43210', 'sales@techlogix.in', 'Building 4, Electronic City, Bengaluru'),
(2, 'Apex Office Supplies', '+91 98450 11223', 'orders@apexsupplies.com', 'Plot 12, Industrial Area, Mumbai'),
(3, 'Global Components Co', '+91 91234 56789', 'contact@globalcomp.org', 'Sector 18, Noida');

-- Sample Products
INSERT IGNORE INTO `product` (`Product_ID`, `Product_Name`, `Category`, `Buying_Price`, `Selling_Price`, `Quantity`, `Minimum_Stock`, `Supplier_ID`) VALUES
(1, 'Logitech Wireless Mouse M185', 'Electronics', 650.00, 999.00, 45, 10, 1),
(2, 'Mechanical Gaming Keyboard RGB', 'Electronics', 1800.00, 2599.00, 18, 5, 1),
(3, 'Dell 24-inch Full HD Monitor', 'Electronics', 7200.00, 9499.00, 8, 5, 1),
(4, 'SanDisk 128GB Flash Drive 3.0', 'Accessories', 520.00, 799.00, 4, 15, 3),
(5, 'Ergonomic Desk Mesh Chair', 'Furniture', 4500.00, 6800.00, 3, 5, 2),
(6, 'A4 Copy Paper Reams (500 sheets)', 'Stationery', 210.00, 320.00, 0, 20, 2);

-- Sample Purchase Orders
INSERT IGNORE INTO `purchase_order` (`Order_ID`, `Product_ID`, `Supplier_ID`, `Quantity`, `Order_Date`, `Status`) VALUES
(1, 4, 3, 50, CURDATE(), 'Pending'),
(2, 6, 2, 100, CURDATE(), 'Pending');

-- Sample Sales
INSERT IGNORE INTO `sales` (`Sale_ID`, `Product_ID`, `Quantity`, `Total_Amount`, `Sale_Date`) VALUES
(1, 1, 2, 1998.00, CURDATE()),
(2, 2, 1, 2599.00, CURDATE()),
(3, 4, 3, 2397.00, CURDATE());
