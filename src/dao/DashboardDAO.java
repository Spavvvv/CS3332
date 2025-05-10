package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import src.model.ClassSession;
import src.model.system.schedule.ScheduleItem;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DashboardDAO {

    /**
     * Adds a new dashboard event (schedule item) to the database
     *
     * @param scheduleItem The schedule item to add
     * @return true if successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean addDashboardEvent(ScheduleItem scheduleItem) throws SQLException {
        // Query updated to match the actual table structure, assuming 'schedules' table
        // has columns: name, description, start_time, end_time, schedule_type
        String query = "INSERT INTO schedules (name, description, start_time, end_time, schedule_type) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);

            // Using getTitle() and getDescription() as implied by controller logic
            stmt.setString(1, scheduleItem.getTitle());
            stmt.setString(2, scheduleItem.getDescription());

            // Convert LocalDateTime to Timestamp for database
            stmt.setTimestamp(3, Timestamp.valueOf(scheduleItem.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(scheduleItem.getEndTime()));

            // Default value for schedule_type as seen in the query structure
            stmt.setString(5, "event"); // Assuming a default type 'event'

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Retrieves today's class sessions from the database
     *
     * @return List of today's class sessions
     * @throws SQLException if a database error occurs
     */
    public List<ClassSession> getTodayClasses() throws SQLException {
        // Assuming class_sessions table contains denormalized fields course_name, teacher_name, room
        String query = "SELECT c.session_id, c.course_name, c.teacher_name, c.room, c.class_date, " +
                "c.start_time, c.end_time, c.class_id FROM class_sessions c " +
                "WHERE c.class_date = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<ClassSession> classes = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

            rs = stmt.executeQuery();

            while (rs.next()) {
                // Mapping columns to ClassSession constructor parameters
                long id = rs.getLong("id");
                String courseName = rs.getString("course_name");
                String teacher = rs.getString("teacher_name");
                String room = rs.getString("room");
                LocalDate date = rs.getDate("class_date").toLocalDate();
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();
                long classId = rs.getLong("class_id"); // Assuming this maps to a class/grouping ID

                // Create ClassSession object - ensure your ClassSession model matches these fields
                ClassSession classSession = new ClassSession(id, courseName, teacher, room, date, startTime, endTime, classId);
                classes.add(classSession);
            }

            return classes;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Gets the total number of students in the system
     *
     * @return The total number of students
     * @throws SQLException if a database error occurs
     */
    public int getTotalStudents() throws SQLException {
        // Assuming 'students' table has a 'status' column
        String query = "SELECT COUNT(*) FROM students WHERE status = 'active'";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Gets the total number of classes in the system
     *
     * @return The total number of classes
     * @throws SQLException if a database error occurs
     */
    public int getTotalClasses() throws SQLException {
        // Assuming 'classes' table has a 'status' column
        String query = "SELECT COUNT(*) FROM classes WHERE status = 'active'";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Gets the attendance rate across all classes based on 'Present' status
     *
     * @return The attendance rate as a percentage
     * @throws SQLException if a database error occurs
     */
    public double getAttendanceRate() throws SQLException {
        // Fixed the query to check for status = 'Present'
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM attendance WHERE status = 'Present') AS present_count, " + // Assuming 'Present' is the status
                "(SELECT COUNT(*) FROM attendance) AS total_count";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                int presentCount = rs.getInt("present_count");
                int totalCount = rs.getInt("total_count");

                if (totalCount > 0) {
                    return ((double) presentCount / totalCount) * 100;
                }
            }
            return 0.0;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Gets the course distribution data for the pie chart
     *
     * @return ObservableList of PieChart.Data for the course distribution
     * @throws SQLException if a database error occurs
     */
    public ObservableList<PieChart.Data> getCourseDistribution() throws SQLException {
        // Adjusted JOIN condition to use courses.course_id as primary key,
        // assuming class_sessions links to courses via course_id column
        String query = "SELECT c.course_name, COUNT(cs.id) as class_count " +
                "FROM courses c " +
                "JOIN class_sessions cs ON c.course_id = cs.course_id " + // Assumes courses PK is course_id
                "GROUP BY c.course_name";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                String courseName = rs.getString("course_name");
                int classCount = rs.getInt("class_count");

                pieChartData.add(new PieChart.Data(courseName, classCount));
            }

            return pieChartData;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Gets upcoming schedule data for the dashboard
     *
     * @param limit Maximum number of records to return
     * @return List of Object arrays containing schedule data
     * @throws SQLException if a database error occurs
     */
    public List<Object[]> getUpcomingSchedulesData(int limit) throws SQLException {
        // Assuming 'schedules' table has columns id, name, description, start_time, end_time, schedule_type
        String query = "SELECT id, name, description, start_time, end_time, schedule_type " +
                "FROM schedules " +
                "WHERE start_time >= CURRENT_TIMESTAMP " +
                "ORDER BY start_time " +
                "LIMIT ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Object[]> scheduleData = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, limit);

            rs = stmt.executeQuery();

            while (rs.next()) {
                // Populate Object array matching the controller's expectation
                Object[] row = new Object[6];
                row[0] = rs.getString("id"); // Assuming id is stored/retrieved as String if ScheduleItem id is String
                row[1] = rs.getString("name");
                row[2] = rs.getString("description");
                row[3] = rs.getTimestamp("start_time"); // Timestamp for conversion to LocalDateTime in controller
                row[4] = rs.getTimestamp("end_time");   // Timestamp for conversion to LocalDateTime in controller
                row[5] = rs.getString("schedule_type");

                scheduleData.add(row);
            }

            return scheduleData;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}
