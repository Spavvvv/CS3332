package src.dao.Report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics; // Make sure this import is correct
import src.utils.DatabaseConnection; // Assuming DatabaseConnection is in src.utils package

import java.sql.Connection;
import java.sql.Date; // Import Date for setting Date parameters
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TeacherMonthlyStatisticsDAO {

    private static final Logger LOGGER = Logger.getLogger(TeacherMonthlyStatisticsDAO.class.getName());

    /**
     * Constructor.
     */
    public TeacherMonthlyStatisticsDAO() {
        // Constructor is empty if no dependencies are injected
    }

    /**
     * Fetches monthly teaching statistics for all teachers within a date range.
     * Assumes fromDate and toDate are calculated to represent the start and end
     * of the desired period (e.g., start and end of a month).
     * @param fromDate The start date of the range (inclusive).
     * @param toDate The end date of the range (inclusive).
     * @param status The status to filter by ("Tất cả" for all, or specific status).
     * @return An ObservableList of TeacherMonthlyStatistics, or an empty list if none found or an error occurs.
     */
    public ObservableList<TeacherMonthlyStatistics> getTeachingStatistics(LocalDate fromDate, LocalDate toDate, String status) { // Removed throws SQLException
        ObservableList<TeacherMonthlyStatistics> statistics = FXCollections.observableArrayList();

        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to fetch monthly teaching statistics with null date range. fromDate: " + fromDate + ", toDate: " + toDate);
            return statistics; // Return empty list for invalid date range
        }

        // Build the query with correct table names, join condition, and duration calculation
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT t.id AS teacher_id, t.name AS teacher_name, "); // Select id and name from teachers
        queryBuilder.append("COUNT(cs.session_id) as session_count, "); // Use class_sessions
        // Calculate duration from start_time and end_time.
        // IMPORTANT: ADJUST THE DURATION CALCULATION BASED ON YOUR DATABASE SYSTEM
        // Example for MySQL: TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0
        // Example for PostgreSQL: EXTRACT(EPOCH FROM (cs.end_time - cs.start_time)) / 3600.0
        // Using ChronoUnit.MINUTES in Java after fetching might be more portable
        // but aggregating in SQL is usually more efficient.
        // Assuming MySQL and calculating in hours as a DOUBLE:
        queryBuilder.append("COALESCE(SUM(TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0), 0.0) AS total_hours "); // Calculate duration in hours, use COALESCE for teachers with no sessions
        queryBuilder.append("FROM teachers t ");
        queryBuilder.append("LEFT JOIN class_sessions cs ON t.name = cs.teacher_name "); // Correct join condition based on schema
        queryBuilder.append("AND cs.session_date BETWEEN ? AND ? "); // Filter by date range in the JOIN clause for LEFT JOIN to work correctly

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(fromDate)); // Convert LocalDate to java.sql.Date
        params.add(Date.valueOf(toDate)); // Convert LocalDate to java.sql.Date

        // Add status filter if not "Tất cả"
        if (status != null && !status.equals("Tất cả")) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                queryBuilder.append("AND cs.status = ? "); // Assuming status column in class_sessions
                params.add(dbStatus);
            } else {
                LOGGER.log(Level.WARNING, "Attempted to filter by unmapped status: " + status);
                // If status is unmapped, we cannot apply the filter, so we proceed without it.
                // Consider if an empty list should be returned here instead depending on requirements.
            }
        }

        queryBuilder.append("GROUP BY t.id, t.name "); // Group by id and name
        queryBuilder.append("ORDER BY t.name"); // Order by name

        String query = queryBuilder.toString();

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection(); // Assuming DatabaseConnection provides a connection that needs explicit closing if not pool-managed externally
             PreparedStatement stmt = connection.prepareStatement(query)) {

            // Set parameters dynamically
            for (int i = 0; i < params.size(); i++) {
                // Basic type handling, may need expansion
                if (params.get(i) instanceof Date) { // Check for java.sql.Date
                    stmt.setDate(i + 1, (Date) params.get(i));
                } else if (params.get(i) instanceof String) {
                    stmt.setString(i + 1, (String) params.get(i));
                } else if (params.get(i) instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params.get(i));
                } else {
                    // Add handling for other types if needed
                    LOGGER.log(Level.WARNING, "Unhandled parameter type for monthly statistics query: " + params.get(i).getClass().getName());
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int stt = 1;
                while (rs.next()) {
                    String teacherId = rs.getString("teacher_id");
                    String teacherName = rs.getString("teacher_name");
                    int sessions = rs.getInt("session_count");
                    double hours = rs.getDouble("total_hours"); // COALESCE handles nulls

                    // Instantiate the model with the retrieved data
                    TeacherMonthlyStatistics teacherStats = new TeacherMonthlyStatistics(
                            stt++, teacherId, teacherName, sessions, hours
                    );
                    statistics.add(teacherStats);
                }
            } // ResultSet closed by try-with-resources

            // The LEFT JOIN with COALESCE handles teachers with no sessions in the range,
            // so no need for a separate query to fetch all teachers if the initial result is empty.
            // The original code's logic to fetch all teachers if `statistics.isEmpty()`
            // was necessary due to the JOIN, but LEFT JOIN makes it unnecessary.

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching monthly teaching statistics.", e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error fetching monthly teaching statistics.", e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        }

        return statistics;
    }

    /**
     * Convert UI status labels to database values.
     * Adjust these mappings if your database uses different values.
     * @param uiStatus The status string from the UI.
     * @return The corresponding database value, or null if not mapped.
     */
    private String mapStatusToDbValue(String uiStatus) {
        switch (uiStatus) {
            case "Đã duyệt":
                return "approved"; // Assuming 'approved' in DB
            case "Chưa duyệt":
                return "pending"; // Assuming 'pending' in DB
            case "Từ chối":
                return "rejected"; // Assuming 'rejected' in DB
            default:
                LOGGER.log(Level.WARNING, "Unmapped status received: " + uiStatus);
                return null;
        }
    }
}
