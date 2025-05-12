package src.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import utils.DatabaseConnection; // Assuming this class handles database connection

public class TeacherYearlyStatisticsDAO {
    private Connection connection;

    public TeacherYearlyStatisticsDAO() {
        // Connection will be obtained when needed in methods,
        // relying on DatabaseConnection to provide a valid one (possibly from a pool)
    }

    /**
     * Ensures the database connection is open and valid.
     * If the current connection is null or closed, attempts to get a new one.
     * @throws SQLException if a connection cannot be established
     */
    private void ensureConnection() throws SQLException {
        // Check if the connection is null or closed.
        // Checking connection.isClosed() can also throw SQLException,
        // so we wrap it.
        boolean needsNewConnection = false;
        try {
            if (connection == null || connection.isClosed()) {
                needsNewConnection = true;
            }
        } catch (SQLException e) {
            // If isClosed() throws an exception, the connection is likely invalid.
            e.printStackTrace(); // Log the exception for debugging
            needsNewConnection = true;
        }

        if (needsNewConnection) {
            System.out.println("Attempting to obtain a new database connection.");
            try {
                this.connection = DatabaseConnection.getConnection();
                System.out.println("New database connection obtained successfully.");
            } catch (SQLException e) {
                System.err.println("Failed to obtain database connection: " + e.getMessage());
                throw e; // Rethrow the exception so the calling method knows it failed
            }
        }
    }


    /**
     * Get yearly teaching statistics for a specific year and approval status
     *
     * @param year Year for statistics
     * @param status Approval status filter ("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối")
     * @return Observable list of teacher statistics models
     */
    public ObservableList<TeacherYearlyStatisticsModel> getYearlyStatistics(int year, String status) {
        ObservableList<TeacherYearlyStatisticsModel> statistics = FXCollections.observableArrayList();
        try {
            ensureConnection(); // Ensure the connection is valid before proceeding

            // Use a try-with-resources to ensure statement and result set are closed
            try (PreparedStatement statement = connection.prepareStatement(buildTeacherStatisticsQuery(status))) {

                // Set parameters based on the query structure
                int paramIndex = 1;
                // Set year parameter for SUM CASE statements (2 times)
                statement.setInt(paramIndex++, year); // yearly_sessions year
                statement.setInt(paramIndex++, year); // yearly_hours year

                // Set year parameter in the WHERE clause (1 time)
                statement.setInt(paramIndex++, year);


                // Add status filter parameter if not "Tất cả"
                if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
                    statement.setString(paramIndex++, status);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    int stt = 1; // Sequential numbering for the UI
                    while (resultSet.next()) {
                        String teacherName = resultSet.getString("teacher_name");
                        // For a single-year view, the yearly stats are the total stats for that year
                        int yearlySessions = resultSet.getInt("yearly_sessions");
                        double yearlyHours = resultSet.getDouble("yearly_hours");

                        TeacherYearlyStatisticsModel model = new TeacherYearlyStatisticsModel(
                                stt++,
                                teacherName,
                                yearlySessions, // yearSessions = total for the selected year
                                yearlyHours,    // yearHours = total for the selected year
                                yearlySessions, // totalSessions = total for the selected year
                                yearlyHours     // totalHours = total for the selected year
                        );
                        statistics.add(model);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving yearly teacher statistics: " + e.getMessage());
            // Return empty list on error
            return FXCollections.observableArrayList();
        }

        return statistics;
    }

    /**
     * Calculate summary statistics for all teachers within a specific year
     *
     * @param year Year for statistics
     * @param status Approval status filter ("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối")
     * @return Statistics summary model with totals
     */
    public StatisticsSummaryModel calculateSummaryStatistics(int year, String status) {
        StatisticsSummaryModel summary = new StatisticsSummaryModel(0, 0.0, 0, 0.0); // Default empty summary
        try {
            ensureConnection(); // Ensure the connection is valid before proceeding

            // Use a try-with-resources to ensure statement and result set are closed
            try (PreparedStatement statement = connection.prepareStatement(buildSummaryQuery(status))) {

                // Set parameters based on the query structure
                int paramIndex = 1;
                // Set year parameter for SUM CASE statements (2 times)
                statement.setInt(paramIndex++, year); // total_sessions year
                statement.setInt(paramIndex++, year); // total_hours year

                // Set year parameter in the WHERE clause (1 time)
                statement.setInt(paramIndex++, year);


                // Add status filter parameter if not "Tất cả"
                if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
                    statement.setString(paramIndex++, status);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        // For a single-year summary, the year totals are the overall totals
                        int totalSessions = resultSet.getInt("total_sessions");
                        double totalHours = resultSet.getDouble("total_hours");

                        summary = new StatisticsSummaryModel(
                                totalSessions, // yearSessions = overall total for the selected year
                                totalHours,    // yearHours = overall total for the selected year
                                totalSessions, // totalSessions = overall total for the selected year
                                totalHours     // totalHours = overall total for the selected year
                        );
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error calculating yearly summary statistics: " + e.getMessage());
            // Return default empty summary on error
            return new StatisticsSummaryModel(0, 0.0, 0, 0.0);
        }

        return summary;
    }

    /**
     * Builds the SQL query for retrieving teacher yearly statistics for a single year
     * using the `teachers` and `class_sessions` tables.
     *
     * @param status the approval status filter ("Tất cả" for all statuses)
     * @return the SQL query string
     */
    private String buildTeacherStatisticsQuery(String status) {
        StringBuilder query = new StringBuilder();
        // Select t.name and alias it as teacher_name
        query.append("SELECT t.name AS teacher_name, ");

        // Calculate yearly sessions by counting rows for the specified year
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS yearly_sessions, ");
        // Calculate yearly hours by summing the time difference in minutes, divided by 60, for the specified year
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS yearly_hours ");

        query.append("FROM teachers t ");
        // Use LEFT JOIN to include teachers even if they have no sessions in the specified year
        // Join on teacher name as used in the previous query structure
        query.append("LEFT JOIN class_sessions cs ON t.name = cs.teacher_name ");
        // Filter by the specified year
        query.append("WHERE YEAR(cs.session_date) = ? ");


        // Add status filter if applicable
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            query.append("AND cs.approval_status = ? ");
        }

        // Group by teacher id and name (assuming teachers table has an id column).
        // If teachers table does not have an 'id' column, group by 't.name' only.
        // Assuming 'id' exists based on common database designs. Adjust if necessary.
        query.append("GROUP BY t.id, t.name ");
        // Order alphabetically by teacher name
        query.append("ORDER BY t.name");

        return query.toString();
    }

    /**
     * Builds the SQL query for retrieving the overall yearly statistics summary for a single year
     * using the `class_sessions` table.
     *
     * @param status the approval status filter ("Tất cả" for all statuses)
     * @return the SQL query string
     */
    private String buildSummaryQuery(String status) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        // Calculate total sessions and hours for the specified year across all teachers
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS total_sessions, ");
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS total_hours ");

        query.append("FROM class_sessions cs ");
        // Filter by the specified year
        query.append("WHERE YEAR(cs.session_date) = ? ");


        // Add status filter if applicable
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            query.append("AND cs.approval_status = ? ");
        }

        return query.toString();
    }


    /**
     * Close the database connection.
     * This method should be called when the application is shutting down.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error closing database connection: " + e.getMessage());
        }
        this.connection = null; // Set connection to null after closing
    }
}
