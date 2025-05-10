package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.TeacherStatisticsModel;
import utils.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;



public class TeachingStatisticsDAO {


/**
 * Get list of teachers with their statistics
 */
    public ObservableList<TeacherStatisticsModel> getTeacherStatistics(
            LocalDate fromDate, LocalDate toDate, String status) throws SQLException {

        ObservableList<TeacherStatisticsModel> data = FXCollections.observableArrayList();

        // First, get all teachers
        String teacherQuery = "SELECT id, CONCAT(last_name, ' ', first_name) AS full_name " +
                "FROM teachers " +
                "ORDER BY last_name, first_name";

        try (ResultSet teacherRs = DatabaseConnection.executeQuery(teacherQuery)) {
            int counter = 1;

            while (teacherRs.next()) {
                int teacherId = teacherRs.getInt("id");
                String teacherName = teacherRs.getString("full_name");

                TeacherStatisticsModel teacher = new TeacherStatisticsModel(counter++, teacherId, teacherName);

                // Now get all teaching sessions for this teacher within date range
                loadTeacherStatistics(teacher, fromDate, toDate, status);

                data.add(teacher);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving teacher data: " + e.getMessage());
            throw e;
        }

        return data;
    }

    /**
     * Load teaching statistics for a specific teacher
     */
    private void loadTeacherStatistics(
            TeacherStatisticsModel teacher,
            LocalDate fromDate,
            LocalDate toDate,
            String status) throws SQLException {

        StringBuilder queryBuilder = new StringBuilder(
                "SELECT teaching_date, COUNT(*) AS session_count, SUM(duration) AS total_hours " +
                        "FROM teaching_sessions " +
                        "WHERE teacher_id = ? " +
                        "AND teaching_date BETWEEN ? AND ? ");

        // Add status filter if needed
        if (!status.equals("Tất cả")) {
            queryBuilder.append("AND status = ? ");
        }

        // Group by date
        queryBuilder.append("GROUP BY teaching_date");

        try {
            // Create parameter list based on whether status filter is used
            Object[] params;
            if (status.equals("Tất cả")) {
                params = new Object[]{teacher.getTeacherId(), fromDate, toDate};
            } else {
                String statusValue = mapStatusToDbValue(status);
                params = new Object[]{teacher.getTeacherId(), fromDate, toDate, statusValue};
            }

            ResultSet rs = DatabaseConnection.executeQuery(queryBuilder.toString(), params);

            // Process results and add to teacher model
            while (rs.next()) {
                LocalDate sessionDate = rs.getDate("teaching_date").toLocalDate();
                int sessions = rs.getInt("session_count");
                double hours = rs.getDouble("total_hours");

                teacher.addDailyStatistic(sessionDate, sessions, hours);
            }
        } catch (SQLException e) {
            System.err.println("Error loading statistics for teacher ID " +
                    teacher.getTeacherId() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Convert UI status labels to database values
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
                return null;
        }
    }

    /**
     * Export statistics to Excel
     */
    public boolean exportToExcel(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            // Log the export request
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES ('excel', ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - in a real app you would get the current user ID
            int userId = 1;
            DatabaseConnection.executeUpdate(logQuery, fromDate, toDate, mapStatusToDbValue(status), userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging Excel export: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export statistics to PDF
     */
    public boolean exportToPdf(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            // Log the export request
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES ('pdf', ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - in a real app you would get the current user ID
            int userId = 1;
            DatabaseConnection.executeUpdate(logQuery, fromDate, toDate, mapStatusToDbValue(status), userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging PDF export: " + e.getMessage());
            return false;
        }
    }

    /**
     * Log print requests
     */
    public boolean logPrintRequest(LocalDate fromDate, LocalDate toDate, String status) {
        try {
            // Log the print request
            String logQuery = "INSERT INTO export_logs (export_type, from_date, to_date, status, user_id) " +
                    "VALUES ('print', ?, ?, ?, ?)";

            // Assuming user_id is 1 for now - in a real app you would get the current user ID
            int userId = 1;
            DatabaseConnection.executeUpdate(logQuery, fromDate, toDate, mapStatusToDbValue(status), userId);

            return true;
        } catch (SQLException e) {
            System.err.println("Error logging print request: " + e.getMessage());
            return false;
        }
    }
}