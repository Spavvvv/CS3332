package src.dao;

import src.model.system.schedule.StudentSchedule;
import src.model.system.course.Course;
import src.model.system.course.CourseDate;
import src.model.person.Student;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for StudentSchedule operations
 */
public class StudentScheduleDAO {

    /**
     * Find a student schedule by ID
     */
    public StudentSchedule findById(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = DatabaseConnection.getConnection();

            // Join the schedules table with the student_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.*, ss.student_id " +
                            "FROM schedules s " +
                            "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                            "WHERE s.id = ? AND s.schedule_type = 'STUDENT'"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                StudentSchedule studentSchedule = new StudentSchedule(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getString("student_id")
                );

                // Load courses
                loadStudentCourses(connection, studentSchedule);

                return studentSchedule;
            }

            return null;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    // Helper method to load courses for a student
    private void loadStudentCourses(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.prepareStatement(
                    "SELECT c.* FROM courses c " +
                            "JOIN student_course_enrollments sce ON c.course_id = sce.course_id " +
                            "WHERE sce.student_id = ?"
            );
            stmt.setString(1, studentSchedule.getStudentId());
            rs = stmt.executeQuery();

            List<Course> courses = new ArrayList<>();

            while (rs.next()) {
                CourseDate date = new CourseDate(
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate()
                );

                Course course = new Course(
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getString("subject"),
                        date
                );
                course.setRoomId(rs.getString("room_id"));

                courses.add(course);
            }

            studentSchedule.setCourses(courses);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Find all student schedules
     */
    public List<StudentSchedule> findAll() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<StudentSchedule> studentSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            stmt = connection.createStatement();

            rs = stmt.executeQuery(
                    "SELECT s.*, ss.student_id " +
                            "FROM schedules s " +
                            "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                            "WHERE s.schedule_type = 'STUDENT'"
            );

            while (rs.next()) {
                StudentSchedule studentSchedule = new StudentSchedule(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getString("student_id")
                );

                // Load courses
                loadStudentCourses(connection, studentSchedule);

                studentSchedules.add(studentSchedule);
            }

            return studentSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Save a student schedule
     */
    public boolean save(StudentSchedule studentSchedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert into base schedule table
            stmt = connection.prepareStatement(
                    "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                            "VALUES (?, ?, ?, ?, ?, 'STUDENT')"
            );
            stmt.setString(1, studentSchedule.getId());
            stmt.setString(2, studentSchedule.getName());
            stmt.setString(3, studentSchedule.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(studentSchedule.getStartTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(studentSchedule.getEndTime()));

            int result = stmt.executeUpdate();
            stmt.close();

            if (result > 0) {
                // Insert into student_schedules table
                stmt = connection.prepareStatement(
                        "INSERT INTO student_schedules (schedule_id, student_id) " +
                                "VALUES (?, ?)"
                );
                stmt.setString(1, studentSchedule.getId());
                stmt.setString(2, studentSchedule.getStudentId());

                result = stmt.executeUpdate();
            }

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Update a student schedule
     */
    public boolean update(StudentSchedule studentSchedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Update base schedule table
            stmt = connection.prepareStatement(
                    "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? " +
                            "WHERE id = ?"
            );
            stmt.setString(1, studentSchedule.getName());
            stmt.setString(2, studentSchedule.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(studentSchedule.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(studentSchedule.getEndTime()));
            stmt.setString(5, studentSchedule.getId());

            int result = stmt.executeUpdate();
            stmt.close();

            if (result > 0) {
                // Update student_schedules table
                stmt = connection.prepareStatement(
                        "UPDATE student_schedules SET student_id = ? " +
                                "WHERE schedule_id = ?"
                );
                stmt.setString(1, studentSchedule.getStudentId());
                stmt.setString(2, studentSchedule.getId());

                result = stmt.executeUpdate();
            }

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Delete a student schedule by ID
     */
    public boolean delete(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Delete from student_schedules table first
            stmt = connection.prepareStatement("DELETE FROM student_schedules WHERE schedule_id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
            stmt.close();

            // Then delete from base schedules table
            stmt = connection.prepareStatement("DELETE FROM schedules WHERE id = ?");
            stmt.setString(1, id);
            int result = stmt.executeUpdate();

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Find student schedules by student ID
     */
    public List<StudentSchedule> findByStudentId(String studentId) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<StudentSchedule> studentSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT s.*, ss.student_id " +
                            "FROM schedules s " +
                            "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                            "WHERE ss.student_id = ?"
            );
            stmt.setString(1, studentId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                StudentSchedule studentSchedule = new StudentSchedule(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getString("student_id")
                );

                // Load courses
                loadStudentCourses(connection, studentSchedule);

                studentSchedules.add(studentSchedule);
            }

            return studentSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }
}
