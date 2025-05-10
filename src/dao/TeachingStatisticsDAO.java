package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.TeacherStatisticsModel; // Ensure this model supports String for teacher ID
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TeachingStatisticsDAO {

    /**
     * Get list of teachers with their statistics for a given date range and status.
     * This method is for displaying statistics, not for updating the total in the teachers table.
     */
    public ObservableList<TeacherStatisticsModel> getTeacherStatistics(
            LocalDate fromDate, LocalDate toDate, String status) throws SQLException {

        // Use a map to easily group statistics by teacher ID
        Map<String, TeacherStatisticsModel> teacherMap = new HashMap<>();

        // SQL query to join teachers and class_sessions, calculate session count and duration
        // Joins on teachers.name = class_sessions.teacher_name based on your foreign key.
        // Duration calculation uses TIMESTAMPDIFF(HOUR, ...) which may need adjustment
        // based on your specific SQL database system and desired precision.
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT t.id AS teacher_id, t.name AS teacher_name, " +
                        "cs.session_date, COUNT(cs.session_id) AS session_count, " +
                        "SUM(TIMESTAMPDIFF(HOUR, cs.start_time, cs.end_time)) AS total_hours " +
                        "FROM teachers t " +
                        "JOIN class_sessions cs ON t.name = cs.teacher_name " + // Joining on name as per FK
                        "WHERE cs.session_date BETWEEN ? AND ? ");

        // Add status filter if needed (assuming status is in class_sessions)
        if (status != null && !status.equals("Tất cả")) {
            queryBuilder.append("AND cs.status = ? ");
        }

        // Group by teacher and date
        queryBuilder.append("GROUP BY t.id, t.name, cs.session_date ");
        // Order for consistent processing and potential display order
        queryBuilder.append("ORDER BY t.name, cs.session_date");

        List<Object> params = new ArrayList<>();
        params.add(fromDate);
        params.add(toDate);

        if (status != null && !status.equals("Tất cả")) {
            String statusValue = mapStatusToDbValue(status);
            if (statusValue != null) {
                params.add(statusValue);
            }
        }

        try (ResultSet rs = DatabaseConnection.executeQuery(queryBuilder.toString(), params.toArray())) {
            int counter = 1; // For the sequence number in TeacherStatisticsModel

            while (rs.next()) {
                String teacherId = rs.getString("teacher_id"); // Get teacher ID as String
                String teacherName = rs.getString("teacher_name");
                LocalDate sessionDate = rs.getDate("session_date").toLocalDate();
                int sessions = rs.getInt("session_count");
                double hours = rs.getDouble("total_hours");

                // Get or create the TeacherStatisticsModel for this teacher
                TeacherStatisticsModel teacher = teacherMap.get(teacherId);
                if (teacher == null) {
                    teacher = new TeacherStatisticsModel(counter++, teacherId, teacherName);
                    teacherMap.put(teacherId, teacher);
                }

                // Add the daily statistic to the teacher model
                teacher.addDailyStatistic(sessionDate, sessions, hours);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving teacher statistics: " + e.getMessage());
            throw e;
        }

        // Convert the map values (TeacherStatisticsModel objects) to an ObservableList
        return FXCollections.observableArrayList(teacherMap.values());
    }

    /**
     * Calculates the total teaching hours for each teacher from class sessions
     * and updates the teaching_hour column in the teachers table.
     * This method calculates total hours across all sessions, regardless of date range or status.
     */
    public void calculateAndSaveTotalTeachingHours() {
        // SQL to calculate total hours per teacher from class_sessions
        // Joins on teachers.name = class_sessions.teacher_name as per FK.
        // Duration calculation uses TIMESTAMPDIFF(HOUR, ...) - adjust as needed.
        String selectHoursQuery =
                "SELECT t.id AS teacher_id, SUM(TIMESTAMPDIFF(HOUR, cs.start_time, cs.end_time)) AS total_hours " +
                        "FROM teachers t " +
                        "JOIN class_sessions cs ON t.name = cs.teacher_name " +
                        "GROUP BY t.id";

        // SQL to update the teachers table
        String updateTeacherQuery = "UPDATE teachers SET teaching_hour = ? WHERE id = ?";

        Connection conn = null;
        PreparedStatement selectStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            // Optional: Set auto-commit to false for a transaction if updating multiple teachers
            // conn.setAutoCommit(false);

            selectStmt = conn.prepareStatement(selectHoursQuery);
            rs = selectStmt.executeQuery();

            updateStmt = conn.prepareStatement(updateTeacherQuery);

            while (rs.next()) {
                String teacherId = rs.getString("teacher_id");
                // Handle potential null sum if a teacher has no sessions
                double totalHours = rs.getObject("total_hours") != null ? rs.getDouble("total_hours") : 0.0;

                // Set parameters for the update statement
                updateStmt.setDouble(1, totalHours);
                updateStmt.setString(2, teacherId);

                // Add to batch for potentially more efficient updates, or execute directly
                updateStmt.executeUpdate(); // Execute update for each teacher
            }

            // If auto-commit was set to false, commit the transaction
            // conn.commit();

        } catch (SQLException e) {
            System.err.println("Error calculating and saving total teaching hours: " + e.getMessage());
            // If using transactions, rollback on error
            // if (conn != null) {
            //     try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            // }
        } finally {
            // Close resources
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (selectStmt != null) selectStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            // Do not close the connection here if DatabaseConnection manages a pool
            // If DatabaseConnection returns a new connection each time, close it.
            // try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }


    /**
     * Convert UI status labels to database values
     * Assumes status column exists in class_sessions and stores these values.
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
                System.err.println("Warning: Unmapped status '" + uiStatus + "'");
                return null;
        }
    }

    /**
     * Export statistics to Excel
     * Logs the action. Assumes user_id = 1 is a placeholder.
     */
    public boolean exportToExcel(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES (?, ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - replace with actual user ID
            int userId = 1;
            String dbStatus = mapStatusToDbValue(status);

            DatabaseConnection.executeUpdate(logQuery, "excel", fromDate, toDate, dbStatus, userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging Excel export: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export statistics to PDF
     * Logs the action. Assumes user_id = 1 is a placeholder.
     */
    public boolean exportToPdf(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES (?, ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - replace with actual user ID
            int userId = 1;
            String dbStatus = mapStatusToDbValue(status);

            DatabaseConnection.executeUpdate(logQuery, "pdf", fromDate, toDate, dbStatus, userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging PDF export: " + e.getMessage());
            return false;
        }
    }

    /**
     * Log print requests
     * Logs the action. Assumes user_id = 1 is a placeholder.
     */
    public boolean logPrintRequest(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES (?, ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - replace with actual user ID
            int userId = 1;
            String dbStatus = mapStatusToDbValue(status);

            DatabaseConnection.executeUpdate(logQuery, "print", fromDate, toDate, dbStatus, userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging print request: " + e.getMessage());
            return false;
        }
    }
}
