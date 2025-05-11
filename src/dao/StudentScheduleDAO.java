package src.dao;

import src.model.system.schedule.StudentSchedule;
import src.model.system.course.Course;
// import src.model.person.Student; // Removed unused import
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for StudentSchedule operations
 */
public class StudentScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentScheduleDAO.class.getName());

    // If StudentScheduleDAO needs other DAOs, declare them here and add setters
    // For example:
    // private CourseDAO courseDAO;
    // public void setCourseDAO(CourseDAO courseDAO) { this.courseDAO = courseDAO; }


    /**
     * Constructor.
     */
    public StudentScheduleDAO() {
        // Constructor is empty if no dependencies are injected
    }

    /**
     * Find a student schedule by ID
     * @param id The ID of the student schedule to find.
     * @return The found StudentSchedule object, or null if not found or an error occurs.
     */
    public StudentSchedule findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find student schedule with null or empty ID.");
            return null;
        }

        String query = "SELECT s.*, ss.student_id " +
                "FROM schedules s " +
                "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                "WHERE s.id = ? AND s.schedule_type = 'STUDENT'";

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    StudentSchedule studentSchedule = new StudentSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("student_id")
                    );

                    // Load courses using the existing connection
                    loadStudentCourses(connection, studentSchedule);

                    return studentSchedule;
                } else {
                    LOGGER.log(Level.INFO, "No student schedule found with ID: " + id);
                    return null; // Schedule ID not found
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student schedule by ID: " + id, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding student schedule by ID: " + id, e);
            return null;
        }
    }

    /**
     * Find student schedules by student ID.
     * @param studentId The ID of the student.
     * @return A list of student schedules for the given student ID, or an empty list if none found or an error occurs.
     */
    public List<StudentSchedule> findByStudentId(String studentId) {
        List<StudentSchedule> schedules = new ArrayList<>();
        if (studentId == null || studentId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find student schedules with null or empty student ID.");
            return schedules;
        }

        String query = "SELECT s.*, ss.student_id " +
                "FROM schedules s " +
                "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                "WHERE ss.student_id = ? AND s.schedule_type = 'STUDENT'"; // Filter by student_id and type

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StudentSchedule studentSchedule = new StudentSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("student_id")
                    );

                    // Load courses using the same connection
                    loadStudentCourses(connection, studentSchedule);

                    schedules.add(studentSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student schedules by student ID: " + studentId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding student schedules by student ID: " + studentId, e);
        }

        return schedules;
    }


    // Helper method to load courses for a student using an existing connection
    private void loadStudentCourses(Connection connection, StudentSchedule studentSchedule) {
        // No need to close connection here, it's managed by the caller's try-with-resources
        String query = "SELECT c.course_id, c.course_name, c.subject, c.start_date, c.end_date, c.room_id " +
                "FROM courses c " +
                "JOIN student_course_enrollments sce ON c.course_id = sce.course_id " +
                "WHERE sce.student_id = ?";

        // Use try-with-resources for PreparedStatement and ResultSet
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentSchedule.getStudentId());
            try (ResultSet rs = stmt.executeQuery()) {
                List<Course> courses = new ArrayList<>();

                while (rs.next()) {
                    Course course = new Course(
                            rs.getString("course_id"),
                            rs.getString("course_name"),
                            rs.getString("subject"),
                            // Handle null dates if necessary
                            rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                            rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null
                    );
                    course.setRoomId(rs.getString("room_id"));

                    courses.add(course);
                }
                studentSchedule.setCourses(courses);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading courses for student ID: " + studentSchedule.getStudentId(), e);
            // No re-throw, just log the error
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading courses for student ID: " + studentSchedule.getStudentId(), e);
            // No re-throw, just log the error
        }
    }

    // Include other methods (save, update, delete) here following the same try-with-resources and logging patterns
    // Example save method (basic structure):
    /*
    public boolean save(StudentSchedule studentSchedule) {
        if (studentSchedule == null || studentSchedule.getId() == null || studentSchedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save a null student schedule or schedule with null/empty ID.");
            return false;
        }

        String insertScheduleSql = "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                                   "VALUES (?, ?, ?, ?, ?, 'STUDENT')";
        String insertStudentScheduleSql = "INSERT INTO student_schedules (schedule_id, student_id) " +
                                           "VALUES (?, ?)";

        Connection connection = null; // Declare connection for transaction and closing
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Start transaction

            // Save to the base schedules table
            try (PreparedStatement stmt = connection.prepareStatement(insertScheduleSql)) {
                stmt.setString(1, studentSchedule.getId());
                stmt.setString(2, studentSchedule.getName());
                stmt.setString(3, studentSchedule.getDescription());
                stmt.setTimestamp(4, studentSchedule.getStartTime() != null ? Timestamp.valueOf(studentSchedule.getStartTime()) : null);
                stmt.setTimestamp(5, studentSchedule.getEndTime() != null ? Timestamp.valueOf(studentSchedule.getEndTime()) : null);

                int baseResult = stmt.executeUpdate();
                if (baseResult == 0) {
                     LOGGER.log(Level.SEVERE, "Failed to insert base schedule for student schedule ID: " + studentSchedule.getId());
                     connection.rollback(); // Rollback if base insert fails
                     return false;
                }
            } // stmt closed by try-with-resources

            // Save to the student_schedules table
            try (PreparedStatement stmt = connection.prepareStatement(insertStudentScheduleSql)) {
                stmt.setString(1, studentSchedule.getId());
                stmt.setString(2, studentSchedule.getStudentId());

                int subclassResult = stmt.executeUpdate();
                if (subclassResult > 0) {
                    connection.commit(); // Commit transaction
                    LOGGER.log(Level.INFO, "Successfully saved student schedule with ID: " + studentSchedule.getId());
                    return true;
                } else {
                    LOGGER.log(Level.SEVERE, "Failed to insert student schedule details for ID: " + studentSchedule.getId() + ", rolling back.");
                    connection.rollback(); // Rollback if subclass insert fails
                    return false;
                }
            } // stmt closed by try-with-resources

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during saving student schedule with ID: " + studentSchedule.getId(), e);
            try {
                if (connection != null) connection.rollback(); // Rollback on SQL error
            } catch (SQLException rbex) {
                 LOGGER.log(Level.SEVERE, "Error during rollback after save failure for ID: " + studentSchedule.getId(), rbex);
            }
            return false;
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Unexpected error during saving student schedule with ID: " + studentSchedule.getId(), e);
             try {
                 if (connection != null) connection.rollback(); // Rollback on unexpected error
             } catch (SQLException rbex) {
                  LOGGER.log(Level.SEVERE, "Error during rollback after unexpected save failure for ID: " + studentSchedule.getId(), rbex);
             }
             return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore default
                    connection.close(); // Close connection
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection after saving student schedule with ID: " + studentSchedule.getId(), e);
                }
            }
        }
    }
    */
}
