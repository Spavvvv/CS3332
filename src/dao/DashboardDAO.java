package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import src.model.ClassSession;
import src.model.system.schedule.ScheduleItem;
import utils.DatabaseConnection;
import src.model.system.course.Course;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for retrieving Dashboard-related data.
 * Manages its own database connections per public operation.
 */
public class DashboardDAO {

    private static final Logger LOGGER = Logger.getLogger(DashboardDAO.class.getName());

    /**
     * Constructor.
     */
    public DashboardDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Adds a new dashboard event (schedule item) to the database.
     * Assumes the 'schedules' table exists with appropriate columns.
     *
     * @param scheduleItem The schedule item to add
     * @return true if successful, false otherwise
     */
    public boolean addDashboardEvent(ScheduleItem scheduleItem) {
        // Query updated to match the actual table structure, assuming 'schedules' table
        // has columns: name, description, start_time, end_time, schedule_type
        String query = "INSERT INTO schedules (name, description, start_time, end_time, schedule_type) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, scheduleItem.getTitle()); // Assuming getTitle() maps to 'name'
            stmt.setString(2, scheduleItem.getDescription()); // Assuming getDescription() maps to 'description'

            // Convert LocalDateTime to Timestamp for database
            stmt.setTimestamp(3, Timestamp.valueOf(scheduleItem.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(scheduleItem.getEndTime()));

            // Assuming a default type 'event' based on the query structure
            stmt.setString(5, "event");

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding dashboard event: " + (scheduleItem != null ? scheduleItem.getTitle() : "null"), e);
            return false;
        }
    }

    /**
     * Retrieves today's class sessions from the database.
     * Assumes 'class_sessions' table contains necessary denormalized fields.
     *
     * @return List of today's class sessions
     */
    public List<ClassSession> getTodayClasses() {
        // Adjusted query to select session_id explicitly and match column names used below
        String query = "SELECT c.session_id, c.course_name, c.teacher_name, c.room, c.session_date, " +
                "c.start_time, c.end_time, c.class_id FROM class_sessions c " +
                "WHERE c.session_date = ?";

        List<ClassSession> classes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection(); // Assuming DatabaseConnection is correctly implemented
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Retrieve data from the ResultSet, using correct column names
                    String id = rs.getString("session_id"); // Match query column name
                    String courseName = rs.getString("course_name");
                    String teacher = rs.getString("teacher_name");
                    String room = rs.getString("room");

                    // Check for null dates and times before converting
                    Date dateDb = rs.getDate("session_date");
                    LocalDate date = (dateDb != null) ? dateDb.toLocalDate() : null;

                    Time startTimeDb = rs.getTime("start_time");
                    LocalTime startTime = (startTimeDb != null) ? startTimeDb.toLocalTime() : null;

                    Time endTimeDb = rs.getTime("end_time");
                    LocalTime endTime = (endTimeDb != null) ? endTimeDb.toLocalTime() : null;

                    String classId = rs.getString("class_id"); // Assuming this links to Class/Course

                    // Create a minimal Course object with available information
                    // Assuming class_id from the database maps to the Course ID or similar identifier
                    Course course = new Course();
                    course.setCourseId(classId); // Using class_id as a Course identifier
                    course.setCourseName(courseName);

                    // Create ClassSession object using the appropriate constructor
                    // Assuming ClassSession has a constructor like:
                    // ClassSession(String id, Course course, String teacher, String room, LocalDate date, LocalTime startTime, LocalTime endTime, String classId)
                    ClassSession classSession = new ClassSession(id, course, teacher, room, date, startTime, endTime, classId);

                    classes.add(classSession);
                }
            }
            return classes;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving today's class sessions.", e);
            return new ArrayList<>(); // Return empty list on error
        }
    }


    /**
     * Gets the total number of students in the system with 'active' status.
     * Assumes 'students' table has 'status' column.
     *
     * @return The total number of active students
     */
    public int getTotalStudents() {
        String query = "SELECT COUNT(*) FROM students WHERE status = 'active'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total active students.", e);
            return 0; // Return 0 on error
        }
    }

    /**
     * Gets the total number of classes in the system with 'active' status.
     * Assumes 'classes' table has 'status' column.
     *
     * @return The total number of active classes
     */
    public int getTotalClasses() {
        String query = "SELECT COUNT(*) FROM classes WHERE status = 'active'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total active classes.", e);
            return 0; // Return 0 on error
        }
    }

    /**
     * Gets the attendance rate across all recorded attendance based on 'Present' status.
     * Assumes 'attendance' table has 'status' column.
     *
     * @return The attendance rate as a percentage (0.0 if no attendance records)
     */
    public double getAttendanceRate() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM attendance WHERE status = 'Có mặt') AS present_count, " + // Assuming 'Present' status
                "(SELECT COUNT(*) FROM attendance) AS total_count";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int presentCount = rs.getInt("present_count");
                int totalCount = rs.getInt("total_count");

                if (totalCount > 0) {
                    return ((double) presentCount / totalCount) * 100;
                }
            }
            return 0.0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting attendance rate.", e);
            return 0.0; // Return 0.0 on error
        }
    }

    /**
     * Gets the course distribution data for a pie chart.
     * Assumes 'courses' table has 'course_name' and 'course_id' as PK,
     * and 'class_sessions' table links via 'course_id'.
     *
     * @return ObservableList of PieChart.Data for the course distribution
     */
    public ObservableList<PieChart.Data> getCourseDistribution() {
        // Adjusted JOIN condition to use courses.course_id as primary key,
        // assuming class_sessions links to courses via course_id column
        String query = "SELECT c.course_name, COUNT(cs.course_id) as class_count " +
                "FROM courses c " +
                "JOIN class_sessions cs ON c.course_id = cs.course_id " + // Assumes courses PK is course_id and class_sessions foreign key is course_id
                "GROUP BY c.course_name";

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String courseName = rs.getString("course_name");
                int classCount = rs.getInt("class_count");

                pieChartData.add(new PieChart.Data(courseName, classCount));
            }

            return pieChartData;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting course distribution data.", e);
            return FXCollections.observableArrayList(); // Return empty list on error
        }
    }

    /**
     * Gets upcoming schedule data for the dashboard.
     * Assumes 'schedules' table has columns id, name, description, start_time, end_time, schedule_type.
     * Retrieves events starting from the current timestamp.
     *
     * @param limit Maximum number of records to return
     * @return List of Object arrays containing schedule data
     */
    public List<Object[]> getUpcomingSchedulesData(int limit) {
        String query = "SELECT id, name, description, start_time, end_time, schedule_type " +
                "FROM schedules " +
                "WHERE start_time >= CURRENT_TIMESTAMP " +
                "ORDER BY start_time " +
                "LIMIT ?";

        List<Object[]> scheduleData = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Populate Object array matching the likely expectation (e.g., for TableView)
                    Object[] row = new Object[6];
                    row[0] = rs.getString("id"); // Assuming id is String
                    row[1] = rs.getString("name");
                    row[2] = rs.getString("description");
                    row[3] = rs.getTimestamp("start_time"); // Timestamp for conversion to LocalDateTime
                    row[4] = rs.getTimestamp("end_time");   // Timestamp for conversion to LocalDateTime
                    row[5] = rs.getString("schedule_type");

                    scheduleData.add(row);
                }
            }
            return scheduleData;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting upcoming schedules data.", e);
            return new ArrayList<>(); // Return empty list on error
        }
    }
}
