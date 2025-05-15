
package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/education_management";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    // Removed: private static Connection connection;

    public static Connection getConnection() throws SQLException {
        // Always create and return a new connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Each call to DriverManager.getConnection creates a new physical connection
            // or gets one from a driver-managed pool if the driver supports it.
            // For robust applications, a dedicated connection pool (e.g., HikariCP) is recommended.
            Connection newConnection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            // System.out.println("New database connection established."); // Optional: for debugging
            return newConnection;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found! Add it to your project dependencies.");
            // e.printStackTrace(); // Consider logging instead of printing stack trace directly in a library class
            throw new SQLException("JDBC Driver not found", e);
        } catch (SQLException e) {
            System.err.println("Connection failed! Please check details in your application log or console.");
            // More detailed error logging could be added here or rely on the caller to log.
            throw e;
        }
    }

    /**
     * This method is problematic if getConnection() returns new connections each time,
     * as it doesn't know WHICH connection to close.
     * It's better to use try-with-resources in the code that calls getConnection().
     * This method is left here for reference but should ideally be removed or re-thought
     * if you adopt the pattern of DAOs managing their own connection lifecycle.
     */
    @Deprecated
    public static void closeConnection(Connection connToClose) {
        if (connToClose != null) {
            try {
                connToClose.close();
                // System.out.println("Database connection closed.");
            } catch (SQLException e) {
                // e.printStackTrace(); // Consider logging
                System.err.println("Error closing provided connection: " + e.getMessage());
            }
        }
    }

    /**
     * These utility methods for executeQuery and executeUpdate might still be useful,
     * but they will now create a new connection, execute, and leave the connection open.
     * The ResultSet returned by executeQuery will be tied to this new connection.
     * The calling code MUST manage closing the PreparedStatement and the Connection
     * (and the ResultSet for executeQuery).
     *
     * For DAOs that already prepare their own statements (like your AttendanceDAO),
     * these helpers might not be necessary.
     */
    public static ResultSet executeQuery(String query, Object... params) throws SQLException {
        Connection conn = getConnection(); // Gets a new connection
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            // The caller is responsible for closing the ResultSet, PreparedStatement, and Connection.
            return stmt.executeQuery();
        } catch (SQLException e) {
            // Clean up statement and connection if an error occurs before returning
            if (stmt != null) try { stmt.close(); } catch (SQLException se) { /* log or ignore */ }
            if (conn != null) try { conn.close(); } catch (SQLException se) { /* log or ignore */ }
            throw e;
        }
        // NOTE: This pattern (returning ResultSet while PreparedStatement and Connection are open)
        // requires careful resource management by the caller. Typically, it's better if the method
        // that creates the ResultSet also processes it or if a try-with-resources block manages all three.
    }

    public static int executeUpdate(String query, Object... params) throws SQLException {
        // This try-with-resources will ensure the connection and statement are closed.
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
        // Connection and PreparedStatement are automatically closed here
    }
}

