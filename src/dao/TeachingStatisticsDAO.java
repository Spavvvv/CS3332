package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.TeacherStatisticsModel;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeachingStatisticsDAO {

    private static final Logger LOGGER = Logger.getLogger(TeachingStatisticsDAO.class.getName());

    /**
     * Constructor.
     */
    public TeachingStatisticsDAO() {
        // Constructor is empty if no dependencies are injected
    }

    /**
     * Get list of teachers with their statistics for a given date range and status.
     * This method is for displaying statistics, not for updating the total in the teachers table.
     * @param fromDate The start date of the range (inclusive).
     * @param toDate The end date of the range (inclusive).
     * @param status The status to filter by ("Tất cả" for all, or specific status).
     * @return An ObservableList of TeacherStatisticsModel, or an empty list if none found or an error occurs.
     */
    public ObservableList<TeacherStatisticsModel> getTeacherStatistics(
            LocalDate fromDate, LocalDate toDate, String status) { // Removed throws SQLException

        Map<String, TeacherStatisticsModel> teacherMap = new HashMap<>();

        StringBuilder queryBuilder = new StringBuilder(
                "SELECT t.id AS teacher_id, t.name AS teacher_name, " +
                        "cs.session_date, COUNT(cs.session_id) AS session_count, " +
                        // Using TIMESTAMPDIFF(HOUR, ...) which may need adjustment based on your specific SQL database
                        "SUM(TIMESTAMPDIFF(HOUR, cs.start_time, cs.end_time)) AS total_hours " +
                        "FROM teachers t " +
                        "JOIN class_sessions cs ON t.name = cs.teacher_name " +
                        "WHERE cs.session_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(fromDate);
        params.add(toDate);

        if (status != null && !status.equals("Tất cả")) {
            queryBuilder.append("AND cs.status = ? ");
            String statusValue = mapStatusToDbValue(status);
            if (statusValue != null) {
                params.add(statusValue);
            } else {
                LOGGER.log(Level.WARNING, "Attempted to filter by unmapped status: " + status);
                // Decide how to handle unmapped status - either return empty or proceed without filter
                // Returning empty list might be safer if status is critical for the query result meaning
                return FXCollections.observableArrayList();
            }
        }

        queryBuilder.append("GROUP BY t.id, t.name, cs.session_date ");
        queryBuilder.append("ORDER BY t.name, cs.session_date");

        // DatabaseConnection.executeQuery should handle resource closing internally based on typical implementations.
        // If it doesn't, this needs further adjustment to wrap in a try-with-resources.
        // Assuming executeQuery returns a ResultSet that needs to be closed:
        try (Connection connection = DatabaseConnection.getConnection(); // Assuming DatabaseConnection provides a connection that needs explicit closing if not pool-managed externally
             PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())) {

            // Set parameters dynamically
            for (int i = 0; i < params.size(); i++) {
                // Basic type handling, may need expansion
                if (params.get(i) instanceof LocalDate) {
                    stmt.setDate(i + 1, Date.valueOf((LocalDate) params.get(i)));
                } else if (params.get(i) instanceof String) {
                    stmt.setString(i + 1, (String) params.get(i));
                } else if (params.get(i) instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params.get(i));
                } else {
                    // Add handling for other types if needed
                    LOGGER.log(Level.WARNING, "Unhandled parameter type: " + params.get(i).getClass().getName());
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int counter = 1; // For the sequence number in TeacherStatisticsModel

                while (rs.next()) {
                    String teacherId = rs.getString("teacher_id");
                    String teacherName = rs.getString("teacher_name");
                    LocalDate sessionDate = rs.getDate("session_date").toLocalDate();
                    int sessions = rs.getInt("session_count");
                    double hours = rs.getDouble("total_hours");

                    TeacherStatisticsModel teacher = teacherMap.get(teacherId);
                    if (teacher == null) {
                        teacher = new TeacherStatisticsModel(counter++, teacherId, teacherName);
                        teacherMap.put(teacherId, teacher);
                    }

                    teacher.addDailyStatistic(sessionDate, sessions, hours);
                }
            } // ResultSet closed by try-with-resources
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving teacher statistics.", e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving teacher statistics.", e);
            // Return empty list on error
            return FXCollections.observableArrayList();
        }

        return FXCollections.observableArrayList(teacherMap.values());
    }

    /**
     * Calculates the total teaching hours for each teacher from class sessions
     * and updates the teaching_hour column in the teachers table.
     * This method calculates total hours across all sessions, regardless of date range or status.
     */
    public void calculateAndSaveTotalTeachingHours() {
        String selectHoursQuery =
                "SELECT t.id AS teacher_id, SUM(TIMESTAMPDIFF(HOUR, cs.start_time, cs.end_time)) AS total_hours " +
                        "FROM teachers t " +
                        "JOIN class_sessions cs ON t.name = cs.teacher_name " +
                        "GROUP BY t.id";

        String updateTeacherQuery = "UPDATE teachers SET teaching_hour = ? WHERE id = ?";

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection conn = DatabaseConnection.getConnection()) { // Assuming DatabaseConnection provides a connection that needs explicit closing
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement selectStmt = conn.prepareStatement(selectHoursQuery);
                 ResultSet rs = selectStmt.executeQuery();
                 PreparedStatement updateStmt = conn.prepareStatement(updateTeacherQuery)) {

                while (rs.next()) {
                    String teacherId = rs.getString("teacher_id");
                    double totalHours = rs.getObject("total_hours") != null ? rs.getDouble("total_hours") : 0.0;

                    updateStmt.setDouble(1, totalHours);
                    updateStmt.setString(2, teacherId);

                    updateStmt.executeUpdate(); // Execute update for each teacher
                }

                conn.commit(); // Commit transaction on success
                LOGGER.log(Level.INFO, "Successfully calculated and saved total teaching hours for all teachers.");

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error calculating or saving total teaching hours. Attempting rollback.", e);
                if (conn != null) {
                    try {
                        conn.rollback(); // Rollback on error
                        LOGGER.log(Level.INFO, "Rollback successful.");
                    } catch (SQLException rbex) {
                        LOGGER.log(Level.SEVERE, "Error during rollback.", rbex);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error calculating or saving total teaching hours. Attempting rollback.", e);
                if (conn != null) {
                    try {
                        conn.rollback(); // Rollback on error
                        LOGGER.log(Level.INFO, "Rollback successful.");
                    } catch (SQLException rbex) {
                        LOGGER.log(Level.SEVERE, "Error during rollback.", rbex);
                    }
                }
            } finally {
                // Restore auto-commit
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Error restoring auto-commit after calculateAndSaveTotalTeachingHours.", e);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting database connection for calculateAndSaveTotalTeachingHours.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting database connection for calculateAndSaveTotalTeachingHours.", e);
        }
    }


    /**
     * Convert UI status labels to database values
     * Assumes status column exists in class_sessions and stores these values.
     * @param uiStatus The status string from the UI.
     * @return The corresponding database value, or null if not mapped.
     */
    private String mapStatusToDbValue(String uiStatus) {
        switch (uiStatus) {
            case "Đã duyệt":
                return "approved";
            case "Chưa duyệt":
                return "pending";
            case "Từ chối":
                return "rejected";
            default:
                LOGGER.log(Level.WARNING, "Unmapped status received: " + uiStatus);
                return null;
        }
    }

    /**
     * Export statistics to Excel
     * Logs the action. Assumes user_id = 1 is a placeholder.
     * @param fromDate The start date of the range.
     * @param toDate The end date of the range.
     * @param status The status filter.
     * @return true if logging was successful, false otherwise.
     */
    public boolean exportToExcel(LocalDate fromDate, LocalDate toDate, String status) {
        String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        int userId = 1; // Assuming user_id is 1 for now - replace with actual user ID
        String dbStatus = mapStatusToDbValue(status);

        // Assuming DatabaseConnection.executeUpdate handles resource closing internally
        try {
            DatabaseConnection.executeUpdate(logQuery, "excel", fromDate, toDate, dbStatus, userId);
            LOGGER.log(Level.INFO, "Logged Excel export request for dates " + fromDate + " to " + toDate + " with status " + status);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error logging Excel export.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error logging Excel export.", e);
            return false;
        }
    }

    /**
     * Export statistics to PDF
     * Logs the action. Assumes user_id = 1 is a placeholder.
     * @param fromDate The start date of the range.
     * @param toDate The end date of the range.
     * @param status The status filter.
     * @return true if logging was successful, false otherwise.
     */
    public boolean exportToPdf(LocalDate fromDate, LocalDate toDate, String status) {
        String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        int userId = 1; // Assuming user_id is 1 for now - replace with actual user ID
        String dbStatus = mapStatusToDbValue(status);

        // Assuming DatabaseConnection.executeUpdate handles resource closing internally
        try {
            DatabaseConnection.executeUpdate(logQuery, "pdf", fromDate, toDate, dbStatus, userId);
            LOGGER.log(Level.INFO, "Logged PDF export request for dates " + fromDate + " to " + toDate + " with status " + status);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error logging PDF export.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error logging PDF export.", e);
            return false;
        }
    }

    /**
     * Log print requests
     * Logs the action. Assumes user_id = 1 is a placeholder.
     * @param fromDate The start date of the range.
     * @param toDate The end date of the range.
     * @param status The status filter.
     * @return true if logging was successful, false otherwise.
     */
    public boolean logPrintRequest(LocalDate fromDate, LocalDate toDate, String status) {
        String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        int userId = 1; // Assuming user_id is 1 for now - replace with actual user ID
        String dbStatus = mapStatusToDbValue(status);

        // Assuming DatabaseConnection.executeUpdate handles resource closing internally
        try {
            DatabaseConnection.executeUpdate(logQuery, "print", fromDate, toDate, dbStatus, userId);
            LOGGER.log(Level.INFO, "Logged print request for dates " + fromDate + " to " + toDate + " with status " + status);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error logging print request.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error logging print request.", e);
            return false;
        }
    }
}
