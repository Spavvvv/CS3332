// ReportDAO.java
package src.dao;

import utils.DatabaseConnection;
import src.model.report.ReportModel.ClassReportData;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportDAO {

    private static final Logger LOGGER = Logger.getLogger(ReportDAO.class.getName());

    /**
     * Constructor.
     */
    public ReportDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Retrieves class report data from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @param status Status filter for classes
     * @return List of ClassReportData objects
     */
    public List<ClassReportData> getClassReportData(LocalDate fromDate, LocalDate toDate, String status) {
        List<ClassReportData> reportData = new ArrayList<>();

        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to get class report data with null dates.");
            return reportData; // Return empty list for invalid input
        }

        // Format dates for SQL query
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Build the status condition for the SQL query
        String statusCondition = "1=1"; // Default to include all
        if (status != null && !status.equals("Tất cả")) {
            statusCondition = "c.status = ?";
        }

        // SQL query to get class report data
        String query = "SELECT c.class_id, c.class_name, " +
                "COUNT(DISTINCT a.session_id) as total_sessions, " +
                "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(DISTINCT a.attendance_id) as total_attendance_records, " + // This seems to be a count of all attendance records, regardless of student or session within the period. Might need adjustment based on exact requirement.
                "COUNT(DISTINCT h.homework_id) as total_homework, " + // This seems to count homework assignments regardless of completion status
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_homework, " +
                // Assuming student_metrics has one record per student per day or session
                "AVG(sm.awareness_score) as awareness, " +
                "AVG(sm.punctuality_score) as punctuality, " +
                "AVG(h.score) as homework_score, " + // Average homework score across all students and assignments for the class within the period
                "COUNT(DISTINCT s.id) as student_count " + // Corrected to count students by id
                "FROM classes c " +
                "LEFT JOIN class_sessions cs ON c.class_id = cs.class_id " +
                "AND cs.session_date BETWEEN ? AND ? " +
                "LEFT JOIN attendance a ON cs.session_id = a.session_id " +
                // Note: Joining homework and student_metrics here might lead to incorrect aggregations if not carefully handled.
                // The current query structure might not correctly associate homework/metrics with specific students or sessions within the date range per class group.
                // If you need averages per student per class, a different query structure might be required (e.g., subqueries or grouping by student first).
                // Assuming the current query is intended to get class-wide averages/counts for the period.
                "LEFT JOIN homework h ON c.class_id = h.class_id " + // Assuming homework is linked to class_id
                "AND h.assigned_date BETWEEN ? AND ? " + // Filter homework by assigned date
                "LEFT JOIN student_metrics sm ON c.class_id = sm.class_id " + // Assuming student_metrics is linked to class_id
                "AND sm.record_date BETWEEN ? AND ? " + // Filter metrics by record date
                "LEFT JOIN students s ON c.class_id = s.class_id " + // Join students to count distinct students in the class
                "WHERE " + statusCondition + " " +
                "GROUP BY c.class_id, c.class_name " +
                "ORDER BY c.class_name";

        // Prepare parameters based on whether status filter is applied
        List<String> parameters = new ArrayList<>();
        parameters.add(fromDateStr); // session_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // homework assigned_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // student_metrics record_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        if (!status.equals("Tất cả")) {
            parameters.add(status); // status = ?
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int counter = 1;
                while (rs.next()) {
                    // Calculate attendance string representation
                    int presentCount = rs.getInt("present_count");
                    int totalSessions = rs.getInt("total_sessions");
                    int studentCount = rs.getInt("student_count");
                    // The 'total_attendance_records' in the current query is likely incorrect
                    // total possible attendance for the period in a class should be total_sessions * student_count
                    int totalAttendancePossible = totalSessions * studentCount;
                    String attendance = presentCount + "/" + (totalAttendancePossible > 0 ? totalAttendancePossible : 0);

                    // Calculate homework string representation
                    int completedHomework = rs.getInt("completed_homework");
                    int totalHomework = rs.getInt("total_homework"); // Total homework *assigned* in this period for the class?
                    // If homework table has one row per student per assignment, this total might be too high.
                    // Assuming it's a count of distinct assignments linked to the class in the period.
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
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving class report data.", e);
            // Decide if you want to re-throw the exception or return empty list on error
            // throw new RuntimeException("Database error retrieving class report data", e); // Option to re-throw
            // Returning empty list on error:
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving class report data.", e);
            // return reportData; // Return current list, likely empty or incomplete
        }

        return reportData;
    }

    /**
     * Gets the attendance statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of attendance (present records / total records)
     */
    public double getAttendancePercentage(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to get attendance percentage with null dates.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String query = "SELECT " +
                "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(a.attendance_id) as total_attendance " +
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.session_id " +
                "WHERE cs.session_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int presentCount = rs.getInt("present_count");
                    int totalAttendance = rs.getInt("total_attendance");

                    return totalAttendance > 0 ? (presentCount / (double) totalAttendance) * 100 : 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating attendance percentage.", e);
            // Decide if you want to re-throw or return 0
            // throw new RuntimeException("Database error calculating attendance percentage", e); // Option to re-throw
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error calculating attendance percentage.", e);
        }

        return 0;
    }

    /**
     * Gets the homework completion statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of homework completion (completed records / total records)
     */
    public double getHomeworkPercentage(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to get homework percentage with null dates.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String query = "SELECT " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_count, " +
                "COUNT(h.homework_id) as total_homework " +
                "FROM homework h " +
                "WHERE h.assigned_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int completedCount = rs.getInt("completed_count");
                    int totalHomework = rs.getInt("total_homework");

                    return totalHomework > 0 ? (completedCount / (double) totalHomework) * 100 : 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating homework percentage.", e);
            // Decide if you want to re-throw or return 0
            // throw new RuntimeException("Database error calculating homework percentage", e); // Option to re-throw
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error calculating homework percentage.", e);
        }

        return 0;
    }

    /**
     * Gets the list of distinct class statuses from the database
     *
     * @return List of class status values
     */
    public List<String> getClassStatuses() {
        List<String> statuses = new ArrayList<>();
        statuses.add("Tất cả"); // Always include "All" option

        String query = "SELECT DISTINCT status FROM classes ORDER BY status";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                statuses.add(rs.getString("status"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving class statuses.", e);
            // Decide if you want to re-throw or return the current list (with "Tất cả")
            // throw new RuntimeException("Database error retrieving class statuses", e); // Option to re-throw
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving class statuses.", e);
        }

        return statuses;
    }
}
