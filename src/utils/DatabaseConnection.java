
package src.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger; // Import Logger

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/education_management";
    private static final String USERNAME = "root";
    //private static final String PASSWORD = "120804";
    private static final String PASSWORD = "123456";

    // Logger for this class
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection newConnection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            // LOGGER.log(Level.INFO, "New database connection established."); // Optional: for debugging
            return newConnection;
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found! Add it to your project dependencies.", e);
            throw new SQLException("JDBC Driver not found", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection failed! URL: " + URL + ", User: " + USERNAME, e);
            throw e;
        }
    }

    /**
     * Attempts to roll back the given database connection.
     * This method should be called in a catch block when a transaction fails.
     *
     * @param conn The connection to roll back. Can be null.
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                LOGGER.log(Level.INFO, "Attempting to rollback transaction.");
                conn.rollback();
                LOGGER.log(Level.INFO, "Transaction rollback successful.");
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during transaction rollback.", ex);
                // Depending on the application's error handling strategy,
                // you might consider re-throwing this as a runtime exception or a custom exception.
                // For now, it just logs the error.
            }
        } else {
            LOGGER.log(Level.WARNING, "Rollback attempted on a null connection.");
        }
    }

    /**
     * Closes the given database connection.
     * It's generally better to use try-with-resources in the code that calls getConnection().
     *
     * @param connToClose The connection to close. Can be null.
     */
    public static void closeConnection(Connection connToClose) { // Removed @Deprecated for now, as DAO uses it.
        if (connToClose != null) {
            try {
                if (!connToClose.isClosed()) {
                    // LOGGER.log(Level.INFO, "Closing database connection."); // Optional
                    connToClose.close();
                    // LOGGER.log(Level.INFO, "Database connection closed."); // Optional
                } else {
                    // LOGGER.log(Level.INFO, "Connection was already closed."); // Optional
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing database connection.", e);
            }
        }
    }

    /**
     * These utility methods for executeQuery and executeUpdate might still be useful,
     * but they will now create a new connection, execute, and leave the connection open for executeQuery.
     * The calling code MUST manage closing the PreparedStatement and the Connection
     * (and the ResultSet for executeQuery).
     *
     * For DAOs that already prepare their own statements (like your AttendanceDAO or the ClassSessionDAO),
     * these helpers might not be as necessary.
     */
    public static ResultSet executeQuery(String query, Object... params) throws SQLException {
        Connection conn = getConnection(); // Gets a new connection
        PreparedStatement stmt = null;
        // This flag helps ensure resources are cleaned up if an error occurs before returning the ResultSet
        boolean success = false;
        try {
            stmt = conn.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            ResultSet rs = stmt.executeQuery();
            success = true; // Mark as success only if executeQuery doesn't throw
            return rs; // The caller is responsible for closing the ResultSet, PreparedStatement, and Connection.
        } finally {
            // If executeQuery failed (success is false), and resources were allocated, clean them up here.
            // If executeQuery succeeded, these will not be closed here; the caller must do it.
            // This is a common pattern when a method returns a resource that needs to be managed externally.
            if (!success) {
                if (stmt != null) {
                    try { stmt.close(); } catch (SQLException se) { LOGGER.log(Level.WARNING, "Failed to close PreparedStatement on error in executeQuery.", se); }
                }
                if (conn != null) {
                    try { conn.close(); } catch (SQLException se) { LOGGER.log(Level.WARNING, "Failed to close Connection on error in executeQuery.", se); }
                }
            }
        }
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

