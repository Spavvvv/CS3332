package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import java.sql.*;
import utils.DatabaseConnection;

public class TeacherQuarterlyStatisticsDAO {

    /**
     * Gets teacher statistics data for a specific year and status.
     * Retrieves data for all four quarters of the specified year and the annual total.
     * Sessions are counted by the number of entries, and hours are calculated
     * from the duration between start_time and end_time.
     *
     * @param year the year to retrieve statistics for
     * @param status the approval status to filter by ("Tất cả" for all statuses)
     * @return a list of teacher statistics for the specified year
     */
    public ObservableList<TeacherQuarterlyStatisticsModel> getTeacherStatistics(
            int year, String status) {

        ObservableList<TeacherQuarterlyStatisticsModel> statistics = FXCollections.observableArrayList();
        String query = buildStatisticsQuery(status);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            setStatisticsQueryParameters(pstmt, year, status);

            ResultSet rs = pstmt.executeQuery();
            int stt = 1;

            while (rs.next()) {
                String teacherName = rs.getString("teacher_name");
                int q1Sessions = rs.getInt("q1_sessions");
                double q1Hours = rs.getDouble("q1_hours");
                int q2Sessions = rs.getInt("q2_sessions");
                double q2Hours = rs.getDouble("q2_hours");
                int q3Sessions = rs.getInt("q3_sessions");
                double q3Hours = rs.getDouble("q3_hours");
                int q4Sessions = rs.getInt("q4_sessions");
                double q4Hours = rs.getDouble("q4_hours");
                // Annual totals are calculated in the query but not strictly needed by the model constructor
                // int totalSessions = rs.getInt("annual_total_sessions");
                // double totalHours = rs.getDouble("annual_total_hours");


                statistics.add(new TeacherQuarterlyStatisticsModel(
                        stt++, teacherName,
                        q1Sessions, q1Hours,
                        q2Sessions, q2Hours,
                        q3Sessions, q3Hours,
                        q4Sessions, q4Hours
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving teacher statistics: " + e.getMessage());
            // Return empty list on error
            return FXCollections.observableArrayList();
        }

        return statistics;
    }

    /**
     * Builds the SQL query for retrieving teacher quarterly and annual statistics.
     * Calculates the quarter from the session_date column, sessions by counting rows,
     * and hours from the time difference between start_time and end_time.
     * Joins teachers and class_sessions on teacher name.
     *
     * @param status the approval status filter ("Tất cả" for all statuses)
     * @return the SQL query string
     */
    private String buildStatisticsQuery(String status) {
        StringBuilder query = new StringBuilder();
        // Select t.name and alias it as teacher_name
        query.append("SELECT t.name AS teacher_name, ");

        // Aggregate sessions and hours for each quarter of the specified year
        // Calculate quarter from session_date: CEIL(MONTH(date) / 3.0)
        // Sessions are counted by summing 1 for each session within the condition
        // Hours are calculated by summing the time difference in minutes, divided by 60
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 1 AND YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS q1_sessions, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 1 AND YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS q1_hours, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 2 AND YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS q2_sessions, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 2 AND YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS q2_hours, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 3 AND YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS q3_sessions, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 3 AND YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS q3_hours, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 4 AND YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS q4_sessions, ");
        query.append("SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 4 AND YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS q4_hours, ");

        // Calculate annual totals for the specified year
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN 1 ELSE 0 END) AS annual_total_sessions, ");
        query.append("SUM(CASE WHEN YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END) AS annual_total_hours ");

        query.append("FROM teachers t ");
        // Corrected the JOIN condition to use t.name and cs.teacher_name
        query.append("JOIN class_sessions cs ON t.name = cs.teacher_name ");
        // Filter by the year derived from session_date
        // This WHERE clause ensures we only process records for the specified year
        // before applying the quarterly/annual SUMs.
        query.append("WHERE YEAR(cs.session_date) = ? ");

        // Added check for null status before appending the WHERE clause part
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            query.append("AND cs.approval_status = ? "); // Optional filter by status
        }

        // Group by teacher id and name. Grouping by id is generally preferred if it's unique.
        query.append("GROUP BY t.id, t.name ");
        // Order alphabetically by teacher name
        query.append("ORDER BY t.name");

        return query.toString();
    }

    /**
     * Sets the parameters for the statistics query.
     *
     * @param pstmt the prepared statement
     * @param year the year parameter
     * @param status the status parameter
     * @throws SQLException if a SQL error occurs
     */
    private void setStatisticsQueryParameters(PreparedStatement pstmt, int year, String status) throws SQLException {
        int paramIndex = 1;

        // Set year parameter for each quarter in the SELECT CASE statements (8 times for sessions/hours)
        for (int i = 0; i < 8; i++) {
            pstmt.setInt(paramIndex++, year);
        }

        // Set year parameter for the annual totals in the SELECT SUM CASE statements (2 times for sessions/hours)
        pstmt.setInt(paramIndex++, year); // Annual total sessions year
        pstmt.setInt(paramIndex++, year); // Annual total hours year

        // Set year parameter in the WHERE clause (1 time)
        pstmt.setInt(paramIndex++, year);

        // Set status parameter if it's not "Tất cả" (1 time if applicable)
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            pstmt.setString(paramIndex++, status);
        }
    }
}
