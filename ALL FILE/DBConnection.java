
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String USER = "root";

    // Enter your actual MySQL root password here (or pass via System property or environment variable)
    private static String password = "ravi@2025";

    public static void setPassword(String newPassword) {
        password = newPassword;
    }

    public static String getPassword() {
        return password;
    }

    // Create and return a database connection
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found on classpath: " + e.getMessage());
        }
        return DriverManager.getConnection(URL, USER, password);
    }

    // Test the database connection
    public static void main(String[] args) {
        try (Connection connection = getConnection()) {

            System.out.println("SUCCESS: Connected to MySQL!");
            System.out.println("Database: " + connection.getCatalog());

        } catch (SQLException e) {

            System.out.println("FAILED: Could not connect to MySQL.");
            System.out.println("Reason: " + e.getMessage());
        }
    }
}
