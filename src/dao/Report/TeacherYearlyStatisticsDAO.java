package src.dao.Report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import src.utils.DatabaseConnection; // Assuming this class handles database connection

public class TeacherYearlyStatisticsDAO {

    private static final Logger LOGGER = Logger.getLogger(TeacherYearlyStatisticsDAO.class.getName());

    /**
     * Constructor.
     */
    public TeacherYearlyStatisticsDAO() {
        // Constructor is empty if no dependencies are injected
    }

    /**
     * Get yearly teaching statistics for a specific year and approval status
     *
     * @param year Year for statistics
     * @param status Approval status filter ("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối")
     * @return Observable list of teacher statistics models, or an empty list if none found or an error occurs.
     */
    public ObservableList<TeacherYearlyStatisticsModel> getYearlyStatistics(int year, String status) {
        ObservableList<TeacherYearlyStatisticsModel> statistics = FXCollections.observableArrayList();
        String query = buildTeacherStatisticsQuery(status);

        // Use try-with-resources to ensure Connection, PreparedStatement, and ResultSet are closed
        try (Connection connection = DatabaseConnection.getConnection(); // Assuming DatabaseConnection provides a connection that needs explicit closing
             PreparedStatement statement = connection.prepareStatement(query)) {

            setTeacherStatisticsQueryParameters(statement, year, status);

            try (ResultSet resultSet = statement.executeQuery()) {
                int stt = 1; // Sequential numbering for the UI
                while (resultSet.next()) {
                    String teacherName = resultSet.getString("teacher_name");
                    // For a single-year src.view, the yearly stats are the total stats for that year
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
            } // ResultSet closed by try-with-resources

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving yearly teacher statistics for year " + year + " and status " + status, e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving yearly teacher statistics for year " + year + " and status " + status, e);
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
     * @return Statistics summary model with totals, or a default empty summary on error or no data.
     */
    public StatisticsSummaryModel calculateSummaryStatistics(int year, String status) {
        StatisticsSummaryModel summary = new StatisticsSummaryModel(0, 0.0, 0, 0.0); // Default empty summary
        String query = buildSummaryQuery(status);

        // Use try-with-resources to ensure Connection, PreparedStatement, and ResultSet are closed
        try (Connection connection = DatabaseConnection.getConnection(); // Assuming DatabaseConnection provides a connection that needs explicit closing
             PreparedStatement statement = connection.prepareStatement(query)) {

            setSummaryQueryParameters(statement, year, status);

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
            } // ResultSet closed by try-with-resources

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating yearly summary statistics for year " + year + " and status " + status, e);
            // Return default empty summary on error
            return new StatisticsSummaryModel(0, 0.0, 0, 0.0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error calculating yearly summary statistics for year " + year + " and status " + status, e);
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
        // Select t.id (if available) and t.name, alias it as teacher_name
        // Use t.id as well in GROUP BY if it's the primary key and name might not be unique
        query.append("SELECT t.id AS teacher_id, t.name AS teacher_name, ");

        // Calculate yearly sessions by counting rows for the specified year
        // Use COALESCE to return 0 if there are no sessions for a given teacher/year/status
        query.append("COALESCE(SUM(CASE WHEN YEAR(cs.session_date) = ? THEN 1 ELSE 0 END), 0) AS yearly_sessions, ");
        // Calculate yearly hours by summing the time difference in minutes, divided by 60, for the specified year
        // Use COALESCE to return 0.0 if there are no sessions for a given teacher/year/status
        query.append("COALESCE(SUM(CASE WHEN YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS yearly_hours ");

        query.append("FROM teachers t ");
        // Use LEFT JOIN to include teachers even if they have no sessions in the specified year/status
        // Join on teacher name as used in the previous query structure
        // Filter by year within the LEFT JOIN condition
        query.append("LEFT JOIN class_sessions cs ON t.name = cs.teacher_name AND YEAR(cs.session_date) = ? ");


        // Add status filter if applicable - applied within the JOIN condition
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                query.append("AND cs.status = ? "); // Assuming 'status' is the column name
            } else {
                LOGGER.log(Level.WARNING, "Unmapped status '" + status + "' encountered during teacher statistics query building.");
                // Decide how to handle unmapped status. Proceeding without filter here.
            }
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
     * Sets the parameters for the teacher statistics query.
     *
     * @param pstmt the prepared statement
     * @param year the year parameter
     * @param status the status parameter
     * @throws SQLException if a SQL error occurs
     */
    private void setTeacherStatisticsQueryParameters(PreparedStatement pstmt, int year, String status) throws SQLException {
        int paramIndex = 1;

        // Set year parameter for SUM CASE statements (2 times)
        pstmt.setInt(paramIndex++, year); // yearly_sessions year
        pstmt.setInt(paramIndex++, year); // yearly_hours year

        // Set year parameter in the LEFT JOIN condition (1 time)
        pstmt.setInt(paramIndex++, year);

        // Add status filter parameter if not "Tất cả"
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                pstmt.setString(paramIndex++, dbStatus);
            }
            // If dbStatus is null, no parameter is set for the status filter
        }
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
        // Use COALESCE to return 0 if there are no sessions for the year/status
        query.append("COALESCE(SUM(CASE WHEN YEAR(cs.session_date) = ? THEN 1 ELSE 0 END), 0) AS total_sessions, ");
        query.append("COALESCE(SUM(CASE WHEN YEAR(cs.session_date) = ? THEN TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0 ELSE 0 END), 0.0) AS total_hours ");

        query.append("FROM class_sessions cs ");
        // Filter by the specified year
        query.append("WHERE YEAR(cs.session_date) = ? ");


        // Add status filter if applicable
        if (status != null && !status.equals("Tất cả") && !status.trim().isEmpty()) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                query.append("AND cs.status = ? "); // Assuming 'status' is the column name
            } else {
                LOGGER.log(Level.WARNING, "Unmapped status '" + status + "' encountered during summary query building.");
                // Decide how to handle unmapped status. Proceeding without filter here.
            }
        }

        return query.toString();
    }

    /**
     * Sets the parameters for the summary statistics query.
     *
     * @param pstmt the prepared statement
     * @param year the year parameter
     * @param status the status parameter
     * @throws SQLException if a SQL error occurs
     */
    private void setSummaryQueryParameters(PreparedStatement pstmt, int year, String status) throws SQLException {
        int paramIndex = 1;

        // Set year parameter for SUM CASE statements (2 times)
        pstmt.setInt(paramIndex++, year); // total_sessions year
        pstmt.setInt(paramIndex++, year); // total_hours year

        // Set year parameter in the WHERE clause (1 time)
        pstmt.setInt(paramIndex++, year);

        // Add status filter parameter if not "Tất cả"
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
