package src.dao.Report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import java.sql.*;
import src.utils.DatabaseConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TeacherQuarterlyStatisticsDAO {

    private static final Logger LOGGER = Logger.getLogger(TeacherQuarterlyStatisticsDAO.class.getName());

    /**
     * Constructor.
     */
    public TeacherQuarterlyStatisticsDAO() {
        // Constructor is empty if no dependencies are injected
    }

    /**
     * Gets teacher statistics data for a specific year and status.
     * Retrieves data for all four quarters of the specified year and the annual total.
     * Sessions are counted by the number of entries, and hours are calculated
     * from the duration between start_time and end_time.
     *
     * @param year the year to retrieve statistics for
     * @param status the approval status to filter by ("Tất cả" for all statuses)
     * @return a list of teacher statistics for the specified year, or an empty list if none found or an error occurs.
     */
    public ObservableList<TeacherQuarterlyStatisticsModel> getTeacherStatistics(
            int year, String status) {

        ObservableList<TeacherQuarterlyStatisticsModel> statistics = FXCollections.observableArrayList();
        String query = buildStatisticsQuery(status);

        try (Connection conn = DatabaseConnection.getConnection(); // Assuming getConnection provides a connection that needs explicit closing
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            setStatisticsQueryParameters(pstmt, year, status);

            try (ResultSet rs = pstmt.executeQuery()) { // Wrap ResultSet in try-with-resources
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
            } // ResultSet closed by try-with-resources

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving teacher quarterly statistics for year " + year + " and status " + status, e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving teacher quarterly statistics for year " + year + " and status " + status, e);
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
        // Use t.id as well in GROUP BY if it's the primary key and name might not be unique
        query.append("SELECT t.id AS teacher_id, t.name AS teacher_name, ");

        // Aggregate sessions and hours for each quarter of the specified year
        // Calculate quarter from session_date: CEIL(MONTH(date) / 3.0)
        // Sessions are counted by summing 1 for each session within the condition
        // Hours are calculated by summing the time difference in minutes, divided by 60
        // Use COALESCE to return 0 if there are no sessions for a given quarter/year
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 1 THEN 1 ELSE 0 END), 0) AS q1_sessions, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 1 THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS q1_hours, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 2 THEN 1 ELSE 0 END), 0) AS q2_sessions, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 2 THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS q2_hours, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 3 THEN 1 ELSE 0 END), 0) AS q3_sessions, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 3 THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS q3_hours, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 4 THEN 1 ELSE 0 END), 0) AS q4_sessions, ");
        query.append("COALESCE(SUM(CASE WHEN CEIL(MONTH(cs.session_date) / 3.0) = 4 THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS q4_hours ");

        // Note: Annual totals are implicitly the sum of quarterly totals, so they are not strictly needed in the SELECT list
        // unless you explicitly want to select them calculated this way.
        // The current model only requires quarterly values. Removing explicit annual total columns
        // to simplify the query and match the model constructor's requirements.

        query.append("FROM teachers t ");
        // Use LEFT JOIN to include teachers even if they have no sessions in the specified year/status
        // Filter by the year derived from session_date in the JOIN condition
        query.append("LEFT JOIN class_sessions cs ON t.name = cs.teacher_name AND YEAR(cs.session_date) = ? ");


        // Add status filter if not "Tất cả" - applied within the JOIN condition
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                query.append("AND cs.status = ? "); // Assuming 'status' is the column name
            } else {
                LOGGER.log(Level.WARNING, "Unmapped status '" + status + "' encountered during query building.");
                // Decide how to handle unmapped status. Proceeding without filter here.
            }
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

        // Set the year parameter in the LEFT JOIN condition
        pstmt.setInt(paramIndex++, year);

        // Set status parameter if it's not "Tất cả" (1 time if applicable)
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                pstmt.setString(paramIndex++, dbStatus);
            }
            // If dbStatus is null, no parameter is set for the status filter
        }
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
