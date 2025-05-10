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

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Connected to database successfully!");
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found! Add it to your project dependencies.");
                e.printStackTrace();
                throw new SQLException("JDBC Driver not found", e);
            } catch (SQLException e) {
                System.err.println("Connection failed! Please check:");
                System.err.println("1. MySQL server is running");
                System.err.println("2. Username and password are correct");
                System.err.println("3. Database exists or can be created");
                System.err.println("4. Server allows connections from this client");
                throw e;
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static ResultSet executeQuery(String query, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeQuery();
    }

    public static int executeUpdate(String query, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeUpdate();
    }
}
