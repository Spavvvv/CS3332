package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics; // Make sure this import is correct
import utils.DatabaseConnection; // Assuming DatabaseConnection is in utils package

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TeacherMonthlyStatisticsDAO {

    /**
     * Fetches monthly teaching statistics for all teachers within a date range.
     * Assumes fromDate and toDate are calculated to represent the start and end
     * of the desired period (e.g., start and end of a month).
     */
    public ObservableList<TeacherMonthlyStatistics> getTeachingStatistics(LocalDate fromDate, LocalDate toDate, String status) throws SQLException {
        ObservableList<TeacherMonthlyStatistics> statistics = FXCollections.observableArrayList();

        // Build the query with correct table names, join condition, and duration calculation
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT t.id AS teacher_id, t.name AS teacher_name, "); // Select id and name from teachers
        queryBuilder.append("COUNT(cs.session_id) as session_count, "); // Use class_sessions
        // Calculate duration from start_time and end_time.
        // Example for MySQL: TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0
        // Example for PostgreSQL: EXTRACT(EPOCH FROM (cs.end_time - cs.start_time)) / 3600.0
        // Using ChronoUnit.MINUTES in Java after fetching might be more portable
        // but aggregating in SQL is usually more efficient.
        // *** IMPORTANT: ADJUST THE DURATION CALCULATION BASED ON YOUR DATABASE SYSTEM ***
        // Assuming MySQL and calculating in hours as a DOUBLE:
        queryBuilder.append("SUM(TIMESTAMPDIFF(MINUTE, cs.start_time, cs.end_time) / 60.0) AS total_hours "); // Calculate duration in hours
        queryBuilder.append("FROM teachers t ");
        queryBuilder.append("LEFT JOIN class_sessions cs ON t.name = cs.teacher_name "); // Correct join condition based on schema
        queryBuilder.append("WHERE (cs.session_date IS NULL OR (cs.session_date BETWEEN ? AND ?)) "); // Filter by date range using BETWEEN

        List<Object> params = new ArrayList<>();
        params.add(fromDate);
        params.add(toDate);

        // Add status filter if not "Tất cả"
        if (status != null && !status.equals("Tất cả")) {
            String dbStatus = mapStatusToDbValue(status);
            if (dbStatus != null) {
                queryBuilder.append("AND cs.status = ? "); // Assuming status column in class_sessions
                params.add(dbStatus);
            }
        }

        queryBuilder.append("GROUP BY t.id, t.name "); // Group by id and name
        queryBuilder.append("ORDER BY t.name"); // Order by name

        String query = queryBuilder.toString();

        try {
            ResultSet rs = DatabaseConnection.executeQuery(query, params.toArray());

            int stt = 1;
            while (rs.next()) {
                String teacherId = rs.getString("teacher_id"); // Get teacher ID as String (VARCHAR)
                String teacherName = rs.getString("teacher_name"); // Get teacher name (VARCHAR)
                int sessions = rs.getInt("session_count");
                // Handle potential null sum (if a teacher has no sessions in the range)
                double hours = rs.getObject("total_hours") != null ? rs.getDouble("total_hours") : 0.0;

                // Instantiate the model with the retrieved data, including String teacherId
                TeacherMonthlyStatistics teacherStats = new TeacherMonthlyStatistics(
                        stt++, teacherId, teacherName, sessions, hours
                );
                statistics.add(teacherStats);
            }

            // If no records found with sessions in the range, fetch all teachers to display them
            // with 0 sessions and hours for the selected period.
            if (statistics.isEmpty()) {
                System.out.println("No sessions found in date range for current criteria. Fetching all teachers.");
                String allTeachersQuery = "SELECT id AS teacher_id, name AS teacher_name FROM teachers ORDER BY name";
                ResultSet allTeachersRs = DatabaseConnection.executeQuery(allTeachersQuery);

                stt = 1;
                while (allTeachersRs.next()) {
                    String teacherId = allTeachersRs.getString("teacher_id");
                    String teacherName = allTeachersRs.getString("teacher_name");
                    // Create model with 0 sessions and hours for teachers with no sessions in range
                    TeacherMonthlyStatistics teacherStats = new TeacherMonthlyStatistics(
                            stt++, teacherId, teacherName, 0, 0.0
                    );
                    statistics.add(teacherStats);
                }
                if (allTeachersRs != null) {
                    try { allTeachersRs.close(); } catch (SQLException ex) { ex.printStackTrace(); }
                }
            }


        } catch (SQLException e) {
            System.err.println("Error fetching monthly teaching statistics: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return statistics;
    }

    /**
     * Convert UI status labels to database values.
     * Adjust these mappings if your database uses different values.
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
                // If an unmapped status comes from UI, log a warning and return null
                // so the status filter is not applied for this case.
                System.err.println("Warning: Unmapped status '" + uiStatus + "'. Not applying status filter.");
                return null;
        }
    }

    // Removed getFallbackData() method
}
