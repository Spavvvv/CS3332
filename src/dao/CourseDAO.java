package src.dao;

import src.model.system.course.Course;
import src.model.person.Student;
import src.model.person.Teacher;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Course entities.
 * Follows the pattern of using a DaoManager for dependency injection and
 * managing connections per-operation via wrapper methods.
 */
public class CourseDAO {
    // Dependent DAOs - must be set externally by a DaoManager
    private StudentDAO studentDAO;
    private TeacherDAO teacherDAO;

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     * The class-level connection is removed to manage connections per operation.
     */
    public CourseDAO() {
        // Dependencies like StudentDAO and TeacherDAO will be set by DaoManager
    }

    /**
     * Set StudentDAO - used for dependency injection.
     * This method must be called after CourseDAO is instantiated to provide the dependency.
     *
     * @param studentDAO The StudentDAO instance
     */
    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Set TeacherDAO - used for dependency injection.
     * This method must be called after CourseDAO is instantiated to provide the dependency.
     *
     * @param teacherDAO The TeacherDAO instance
     */
    public void setTeacherDAO(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    /**
     * Check if StudentDAO dependency is set.
     * @throws IllegalStateException if StudentDAO dependency is not set.
     */
    private void checkStudentDAODependency() {
        if (this.studentDAO == null) {
            throw new IllegalStateException("StudentDAO dependency has not been set on CourseDAO. Cannot load students.");
        }
    }

    /**
     * Check if TeacherDAO dependency is set.
     * @throws IllegalStateException if TeacherDAO dependency is not set.
     */
    private void checkTeacherDAODependency() {
        if (this.teacherDAO == null) {
            throw new IllegalStateException("TeacherDAO dependency has not been set on CourseDAO. Cannot load teacher.");
        }
    }

    // --- Internal Methods (Package-private or Private) ---
    // These methods take a Connection as a parameter and perform the core SQL logic.
    // They typically throw SQLException and rely on dependencies already being set.

    /**
     * Internal method to insert a new course into the database using an existing connection.
     *
     * @param conn   the active database connection
     * @param course the course to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalInsert(Connection conn, Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_id, course_name, subject, start_date, end_date, " +
                "days_of_week, start_time, end_time, teacher_id, room_id, progress) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseId());
            statement.setString(2, course.getCourseName());
            statement.setString(3, course.getSubject());
            statement.setDate(4, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(5, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setString(6, course.getDaysOfWeekAsString());

            Time startTimeSql = course.getStartTime() != null ? Time.valueOf(course.getStartTime()) : null;
            Time endTimeSql = course.getEndTime() != null ? Time.valueOf(course.getEndTime()) : null;

            statement.setTime(7, startTimeSql);
            statement.setTime(8, endTimeSql);

            statement.setString(9, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(10, course.getRoomId());
            statement.setFloat(11, course.getProgress());

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0 && course.getStudents() != null) {
                for (Student student : course.getStudents()) {
                    // Use the same connection for relationship linking
                    addStudentToCourse(conn, course.getCourseId(), student.getId());
                }
            }

            return rowsInserted > 0;
        }
    }

    /**
     * Internal method to update an existing course in the database using an existing connection.
     *
     * @param conn   the active database connection
     * @param course the course to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalUpdate(Connection conn, Course course) throws SQLException {
        String sql = "UPDATE courses SET course_name = ?, subject = ?, start_date = ?, end_date = ?, " +
                "days_of_week = ?, start_time = ?, end_time = ?, teacher_id = ?, room_id = ?, " +
                "progress = ? WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getSubject());
            statement.setDate(3, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(4, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setString(5, course.getDaysOfWeekAsString());

            Time startTimeSql = course.getStartTime() != null ? Time.valueOf(course.getStartTime()) : null;
            Time endTimeSql = course.getEndTime() != null ? Time.valueOf(course.getEndTime()) : null;

            statement.setTime(6, startTimeSql);
            statement.setTime(7, endTimeSql);

            statement.setString(8, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(9, course.getRoomId());
            statement.setFloat(10, course.getProgress());
            statement.setString(11, course.getCourseId());

            int rowsUpdated = statement.executeUpdate();

            // Update student enrollments if needed
            if (rowsUpdated > 0) {
                // Use the same connection for related operations
                removeAllStudentsFromCourse(conn, course.getCourseId());
                if (course.getStudents() != null) {
                    for (Student student : course.getStudents()) {
                        addStudentToCourse(conn, course.getCourseId(), student.getId());
                    }
                }
            }

            return rowsUpdated > 0;
        }
    }

    /**
     * Internal method to delete a course from the database using an existing connection.
     *
     * @param conn the active database connection
     * @param courseId the ID of the course to delete
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalDelete(Connection conn, String courseId) throws SQLException {
        // Use the same connection for related operations
        removeAllStudentsFromCourse(conn, courseId);

        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get a course by ID using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     *
     * @param conn the active database connection
     * @param courseId the course ID
     * @return the Course object or null if not found
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    Course getById(Connection conn, String courseId) throws SQLException {
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    return extractCourseFromResultSet(conn, resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Get all courses from the database using an existing connection.
     * Primarily for internal use or by other DAOs with an active connection.
     *
     * @param conn the active database connection
     * @return List of all courses
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getAll(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses";

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                // Pass the connection down to the extraction helper
                courses.add(extractCourseFromResultSet(conn, resultSet));
            }
        }
        return courses;
    }

    /**
     * Add a student to a course by creating an enrollment record using an existing connection.
     *
     * @param conn the active database connection
     * @param courseId the course ID (maps to class_id in enrollment)
     * @param studentId the student ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean addStudentToCourse(Connection conn, String courseId, String studentId) throws SQLException {
        String sql = "INSERT INTO enrollment (student_id, class_id, enrollment_date, status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, courseId);
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.setString(4, "Active");

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove a student from a course by deleting the enrollment record using an existing connection.
     *
     * @param conn the active database connection
     * @param courseId the course ID (maps to class_id in enrollment)
     * @param studentId the student ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean removeStudentFromCourse(Connection conn, String courseId, String studentId) throws SQLException {
        String sql = "DELETE FROM enrollment WHERE class_id = ? AND student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove all students from a course by deleting enrollment records for that class using an existing connection.
     *
     * @param conn the active database connection
     * @param courseId the course ID (maps to class_id in enrollment)
     * @throws SQLException if a database access error occurs
     */
    private void removeAllStudentsFromCourse(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM enrollment WHERE class_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.executeUpdate();
        }
    }

    /**
     * Get courses by teacher ID using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @return List of courses taught by the teacher
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getCoursesByTeacherId(Connection conn, String teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE teacher_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get courses by student ID using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return List of courses the student is enrolled in
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getCoursesByStudentId(Connection conn, String studentId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.course_id, c.course_name, c.subject, c.start_date, c.end_date, c.days_of_week, " +
                "c.start_time, c.end_time, c.teacher_id, c.room_id, c.progress FROM courses c " +
                "JOIN enrollment e ON c.course_id = e.class_id " +
                "WHERE e.student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Search courses by name or subject using an existing connection.
     *
     * @param conn the active database connection
     * @param searchTerm the search term
     * @return List of matching courses
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> searchCourses(Connection conn, String searchTerm) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE course_name LIKE ? OR subject LIKE ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get courses by subject using an existing connection.
     *
     * @param conn the active database connection
     * @param subject the subject name
     * @return List of courses for the subject
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getCoursesBySubject(Connection conn, String subject) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE subject = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, subject);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get courses by date range using an existing connection.
     *
     * @param conn the active database connection
     * @param startDate the start date
     * @param endDate the end date
     * @return List of courses within the date range
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getCoursesByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE " +
                "NOT (end_date < ? OR start_date > ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get active courses using an existing connection.
     *
     * @param conn the active database connection
     * @return List of active courses
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getActiveCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE " +
                "start_date <= ? AND end_date >= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get upcoming courses using an existing connection.
     *
     * @param conn the active database connection
     * @return List of upcoming courses
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getUpcomingCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE start_date > ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Get completed courses using an existing connection.
     *
     * @param conn the active database connection
     * @return List of completed courses
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    List<Course> getCompletedCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, progress FROM courses WHERE end_date < ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to the extraction helper
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    /**
     * Internal method to update course progress using an existing connection.
     *
     * @param conn the active database connection
     * @param courseId the course ID
     * @param progress the new progress value
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalUpdateProgress(Connection conn, String courseId, float progress) throws SQLException {
        String sql = "UPDATE courses SET progress = ? WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setFloat(1, progress);
            statement.setString(2, courseId);

            return statement.executeUpdate() > 0;
        }
    }


    /**
     * Get students enrolled in a course by looking up enrollment records using an existing connection.
     * Requires the StudentDAO dependency to be set.
     *
     * @param conn the active database connection
     * @param courseId the course ID (maps to class_id in enrollment)
     * @return List of students enrolled in the course
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if StudentDAO dependency has not been set
     */
    List<Student> getStudentsByCourseId(Connection conn, String courseId) throws SQLException {
        checkStudentDAODependency();
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_id FROM enrollment WHERE class_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    // Use the injected StudentDAO and pass the existing connection
                    Student student = this.studentDAO.getStudentById(conn, studentId);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
        }
        return students;
    }

    /**
     * Helper method to extract a Course object from a ResultSet.
     * This method now requires a database connection to load related entities (Teacher, Students).
     *
     * @param conn the active database connection
     * @param resultSet ResultSet containing course data
     * @return the Course object
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set
     */
    private Course extractCourseFromResultSet(Connection conn, ResultSet resultSet) throws SQLException {
        String courseId = resultSet.getString("course_id");
        String courseName = resultSet.getString("course_name");
        String subject = resultSet.getString("subject");

        LocalDate startDate = null;
        Date startDateSql = resultSet.getDate("start_date");
        if (startDateSql != null) {
            startDate = startDateSql.toLocalDate();
        }

        LocalDate endDate = null;
        Date endDateSql = resultSet.getDate("end_date");
        if (endDateSql != null) {
            endDate = endDateSql.toLocalDate();
        }

        Course course = new Course(courseId, courseName, subject, startDate, endDate);

        String daysOfWeekString = resultSet.getString("days_of_week");
        course.setDaysOfWeekFromString(daysOfWeekString);

        Time startTimeSql = resultSet.getTime("start_time");
        LocalTime startTime = startTimeSql != null ? startTimeSql.toLocalTime() : null;
        course.setStartTime(startTime);

        Time endTimeSql = resultSet.getTime("end_time");
        LocalTime endTime = endTimeSql != null ? endTimeSql.toLocalTime() : null;
        course.setEndTime(endTime);

        String teacherId = resultSet.getString("teacher_id");
        String roomId = resultSet.getString("room_id");
        float progress = resultSet.getFloat("progress");

        course.setRoomId(roomId);
        course.setProgress(progress);

        // Set teacher if available - requires TeacherDAO
        if (teacherId != null && !teacherId.trim().isEmpty()) {
            checkTeacherDAODependency();
            // Use the injected TeacherDAO and pass the existing connection
            Teacher teacher = this.teacherDAO.getById(conn, teacherId);
            if (teacher != null) {
                course.setTeacher(teacher);
            }
        }

        // Load students - requires StudentDAO
        // Use the helper method that takes a connection
        List<Student> students = getStudentsByCourseId(conn, courseId); // Pass the connection
        if (students != null) {
            course.setStudents(students);
        } else {
            course.setStudents(new ArrayList<>());
        }

        return course;
    }

    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.
    // They delegate the core database logic to the internal methods.

    /**
     * Save a new course. Manages its own connection and transaction.
     *
     * @param course the course to save
     * @return true if saving was successful, false otherwise
     * @throws IllegalStateException if a required dependency (StudentDAO) has not been set (propagated from internalInsert)
     */
    public boolean save(Course course) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalInsert(conn, course);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error saving course: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing course. Manages its own connection and transaction.
     *
     * @param course the course to update
     * @return true if the update was successful, false otherwise
     * @throws IllegalStateException if a required dependency (StudentDAO) has not been set (propagated from internalUpdate)
     */
    public boolean update(Course course) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, course);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating course: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a course. Manages its own connection and transaction.
     *
     * @param courseId the ID of the course to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, courseId);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting course with ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find a course by ID. Manages its own connection.
     *
     * @param courseId the course ID
     * @return Optional containing the Course if found, empty Optional otherwise. Returns empty Optional on database error or if dependencies are not set.
     */
    public Optional<Course> findById(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getById(Connection, String) might throw IllegalStateException
            return Optional.ofNullable(getById(conn, courseId));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding course by ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get all courses. Manages its own connection.
     *
     * @return List of all courses. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getAll(Connection) might throw IllegalStateException
            return getAll(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding all courses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Add a student to a course. Manages its own connection.
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @return true if successful, false otherwise (including on error)
     */
    public boolean addStudentToCourse(String courseId, String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // No transaction needed for a single insert, auto-commit is fine by default
            return addStudentToCourse(conn, courseId, studentId);
        } catch (SQLException e) {
            System.err.println("Error adding student " + studentId + " to course " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a student from a course. Manages its own connection.
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @return true if successful, false otherwise (including on error)
     */
    public boolean removeStudentFromCourse(String courseId, String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // No transaction needed for a single delete, auto-commit is fine by default
            return removeStudentFromCourse(conn, courseId, studentId);
        } catch (SQLException e) {
            System.err.println("Error removing student " + studentId + " from course " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Get courses by teacher ID. Manages its own connection.
     *
     * @param teacherId the teacher ID
     * @return List of courses taught by the teacher. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findCoursesByTeacherId(String teacherId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getCoursesByTeacherId(Connection, String) might throw IllegalStateException
            return getCoursesByTeacherId(conn, teacherId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by teacher ID: " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get courses by student ID. Manages its own database connection.
     *
     * @param studentId the student ID
     * @return List of courses the student is enrolled in. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findCoursesByStudentId(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getCoursesByStudentId(Connection, String) might throw IllegalStateException
            return getCoursesByStudentId(conn, studentId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses for student ID: " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Search courses by name or subject. Manages its own connection.
     *
     * @param searchTerm the search term
     * @return List of matching courses. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> search(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // searchCourses(Connection, String) might throw IllegalStateException
            return searchCourses(conn, searchTerm);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error searching courses for term: '" + searchTerm + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get courses by subject. Manages its own connection.
     *
     * @param subject the subject name
     * @return List of courses for the subject. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findCoursesBySubject(String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getCoursesBySubject(Connection, String) might throw IllegalStateException
            return getCoursesBySubject(conn, subject);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by subject: " + subject + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get courses by date range. Manages its own connection.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return List of courses within the date range. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findCoursesByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getCoursesByDateRange(Connection, LocalDate, LocalDate) might throw IllegalStateException
            return getCoursesByDateRange(conn, startDate, endDate);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by date range (" + startDate + " to " + endDate + "): " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get active courses. Manages its own connection.
     *
     * @return List of active courses. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findActiveCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getActiveCourses(Connection) might throw IllegalStateException
            return getActiveCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding active courses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get upcoming courses. Manages its own connection.
     *
     * @return List of upcoming courses. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findUpcomingCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getUpcomingCourses(Connection) might throw IllegalStateException
            return getUpcomingCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding upcoming courses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get completed courses. Manages its own connection.
     *
     * @return List of completed courses. Returns empty list on database error or if dependencies are not set.
     */
    public List<Course> findCompletedCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getCompletedCourses(Connection) might throw IllegalStateException
            return getCompletedCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding completed courses: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Update course progress based on the course's start and end dates and current date.
     * Manages its own connection.
     *
     * @param courseId the course ID
     * @return true if successful, false otherwise (including on error or if course not found)
     * @throws IllegalStateException if a required dependency (StudentDAO, TeacherDAO) has not been set (propagated from findById)
     */
    public boolean updateProgressBasedOnDate(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Need to get the course first to calculate progress
            Course course = getById(conn, courseId); // getById(Connection, String) might throw IllegalStateException
            if (course == null) {
                System.err.println("Error updating progress: Course with ID " + courseId + " not found.");
                return false;
            }

            float calculatedProgress = (float) course.calculateProgressBasedOnDate();

            // Use the same connection to update the progress
            return internalUpdateProgress(conn, courseId, calculatedProgress);

        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating progress for course ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Update course progress. Manages its own connection.
     *
     * @param courseId the course ID
     * @param progress the new progress value
     * @return true if successful, false otherwise (including on error)
     */
    public boolean updateProgress(String courseId, float progress) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // No transaction needed for a single update, auto-commit is fine by default
            return internalUpdateProgress(conn, courseId, progress);
        } catch (SQLException e) {
            System.err.println("Error setting progress for course ID: " + courseId + " to " + progress + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Check if two courses have time conflicts. Does not interact with the database.
     *
     * @param course1 the first course
     * @param course2 the second course
     * @return true if there is a time conflict based on overlapping dates and time slots on common days.
     */
    public boolean hasTimeConflict(Course course1, Course course2) {
        // Check if date ranges overlap
        if (!course1.overlapsDateRange(course2)) {
            return false;
        }

        // Check if time slots overlap and if there's at least one common day of the week
        LocalTime start1 = course1.getStartTime();
        LocalTime end1 = course1.getEndTime();
        LocalTime start2 = course2.getStartTime();
        LocalTime end2 = course2.getEndTime();

        List<String> days1 = course1.getDaysOfWeek();
        List<String> days2 = course2.getDaysOfWeek();

        // Check for intersection of days of week
        boolean hasDayIntersection = false;
        if (days1 != null && days2 != null) {
            for (String day1 : days1) {
                if (day1 != null && !day1.trim().isEmpty() && days2.contains(day1.trim())) {
                    hasDayIntersection = true;
                    break;
                }
            }
        }

        if (!hasDayIntersection) {
            return false;
        }

        // Check if time periods overlap, handling potential null times
        if (start1 != null && end1 != null && start2 != null && end2 != null) {
            // Overlap if (start1 <= end2) and (start2 <= end1)
            return !(end1.isBefore(start2) || start1.isAfter(end2));
        } else {
            // If times aren't defined for both courses, there's no *specific* time slot conflict to check.
            return false;
        }
    }

    // The closeConnection method is removed as connections are managed per method call via try-with-resources.
}
