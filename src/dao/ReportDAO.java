// ReportDAO.java
package src.dao;

import utils.DatabaseConnection;
import src.model.report.ReportModel.ClassReportData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    /**
     * Retrieves class report data from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @param status Status filter for classes
     * @return List of ClassReportData objects
     * @throws SQLException If a database error occurs
     */
    public List<ClassReportData> getClassReportData(LocalDate fromDate, LocalDate toDate, String status) throws SQLException {
        List<ClassReportData> reportData = new ArrayList<>();

        // Format dates for SQL query
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Build the status condition for the SQL query
        String statusCondition = "true"; // Default to include all
        if (status != null && !status.equals("Tất cả")) {
            statusCondition = "c.status = ?";
        }

        // SQL query to get class report data
        String query = "SELECT c.class_id, c.class_name, " +
                "COUNT(DISTINCT a.session_id) as total_sessions, " +
                "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(DISTINCT a.attendance_id) as total_attendance_records, " +
                "COUNT(DISTINCT h.homework_id) as total_homework, " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_homework, " +
                "AVG(sm.awareness_score) as awareness, " +
                "AVG(sm.punctuality_score) as punctuality, " +
                "AVG(h.score) as homework_score, " +
                "COUNT(DISTINCT s.student_id) as student_count " +
                "FROM classes c " +
                "LEFT JOIN class_sessions cs ON c.class_id = cs.class_id " +
                "AND cs.session_date BETWEEN ? AND ? " +
                "LEFT JOIN attendance a ON cs.session_id = a.session_id " +
                "LEFT JOIN homework h ON c.class_id = h.class_id " +
                "AND h.assigned_date BETWEEN ? AND ? " +
                "LEFT JOIN student_metrics sm ON c.class_id = sm.class_id " +
                "AND sm.record_date BETWEEN ? AND ? " +
                "LEFT JOIN students s ON c.class_id = s.class_id " +
                "WHERE " + statusCondition + " " +
                "GROUP BY c.class_id, c.class_name " +
                "ORDER BY c.class_name";

        try (ResultSet rs = status.equals("Tất cả") ?
                DatabaseConnection.executeQuery(query, fromDateStr, toDateStr, fromDateStr, toDateStr, fromDateStr, toDateStr) :
                DatabaseConnection.executeQuery(query, fromDateStr, toDateStr, fromDateStr, toDateStr, fromDateStr, toDateStr, status)) {

            int counter = 1;
            while (rs.next()) {
                // Calculate attendance string representation
                int presentCount = rs.getInt("present_count");
                int totalSessions = rs.getInt("total_sessions");
                int studentCount = rs.getInt("student_count");
                int totalAttendancePossible = totalSessions * studentCount;
                String attendance = presentCount + "/" + (totalAttendancePossible > 0 ? totalAttendancePossible : 0);

                // Calculate homework string representation
                int completedHomework = rs.getInt("completed_homework");
                int totalHomework = rs.getInt("total_homework");
                String homework = completedHomework + "/" + totalHomework;

                // Format homework score
                double homeworkScore = rs.getDouble("homework_score");
                String formattedScore = String.format("%.2f/10", homeworkScore);

                // Create ClassReportData object
                ClassReportData data = new ClassReportData(
                        counter++,
                        rs.getString("class_name"),
                        attendance,
                        homework,
                        rs.getDouble("awareness"),
                        rs.getDouble("punctuality"),
                        formattedScore
                );

                reportData.add(data);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving class report data: " + e.getMessage());
            throw e;
        }

        return reportData;
    }

    /**
     * Gets the attendance statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of attendance
     * @throws SQLException If a database error occurs
     */
    public double getAttendancePercentage(LocalDate fromDate, LocalDate toDate) throws SQLException {
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String query = "SELECT " +
                "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(a.attendance_id) as total_attendance " +
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.session_id " +
                "WHERE cs.session_date BETWEEN ? AND ?";

        try (ResultSet rs = DatabaseConnection.executeQuery(query, fromDateStr, toDateStr)) {
            if (rs.next()) {
                int presentCount = rs.getInt("present_count");
                int totalAttendance = rs.getInt("total_attendance");

                return totalAttendance > 0 ? (presentCount / (double) totalAttendance) * 100 : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error calculating attendance percentage: " + e.getMessage());
            throw e;
        }

        return 0;
    }

    /**
     * Gets the homework completion statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of homework completion
     * @throws SQLException If a database error occurs
     */
    public double getHomeworkPercentage(LocalDate fromDate, LocalDate toDate) throws SQLException {
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String query = "SELECT " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_count, " +
                "COUNT(h.homework_id) as total_homework " +
                "FROM homework h " +
                "WHERE h.assigned_date BETWEEN ? AND ?";

        try (ResultSet rs = DatabaseConnection.executeQuery(query, fromDateStr, toDateStr)) {
            if (rs.next()) {
                int completedCount = rs.getInt("completed_count");
                int totalHomework = rs.getInt("total_homework");

                return totalHomework > 0 ? (completedCount / (double) totalHomework) * 100 : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error calculating homework percentage: " + e.getMessage());
            throw e;
        }

        return 0;
    }

    /**
     * Gets the list of distinct class statuses from the database
     *
     * @return List of class status values
     * @throws SQLException If a database error occurs
     */
    public List<String> getClassStatuses() throws SQLException {
        List<String> statuses = new ArrayList<>();
        statuses.add("Tất cả"); // Always include "All" option

        String query = "SELECT DISTINCT status FROM classes ORDER BY status";

        try (ResultSet rs = DatabaseConnection.executeQuery(query)) {
            while (rs.next()) {
                statuses.add(rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving class statuses: " + e.getMessage());
            throw e;
        }

        return statuses;
    }
}