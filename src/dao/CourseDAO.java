
package src.dao;

import src.model.system.course.Course;
import src.model.person.Student;
import src.model.person.Teacher;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List; // Removed Arrays import as it's unused
import java.util.Optional;

/**
 * Data Access Object for Course entities.
 */
public class CourseDAO {
    private StudentDAO studentDAO;
    private TeacherDAO teacherDAO;

    public CourseDAO() {
        // Dependencies will be set by DaoManager
    }

    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public void setTeacherDAO(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    private void checkStudentDAODependency() {
        if (this.studentDAO == null) {
            throw new IllegalStateException("StudentDAO dependency has not been set on CourseDAO.");
        }
    }

    private void checkTeacherDAODependency() {
        if (this.teacherDAO == null) {
            throw new IllegalStateException("TeacherDAO dependency has not been set on CourseDAO.");
        }
    }

    private boolean internalInsert(Connection conn, Course course) throws SQLException {
        // Added class_id to the SQL query and parameter setting
        String sql = "INSERT INTO courses (course_id, course_name, subject, start_date, end_date, " +
                "days_of_week, start_time, end_time, teacher_id, room_id, class_id, progress) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseId());
            statement.setString(2, course.getCourseName());
            statement.setString(3, course.getSubject());
            statement.setDate(4, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(5, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setString(6, course.getDaysOfWeekAsString());

            // Use renamed getters from Course model
            Time startTimeSql = course.getCourseStartTime() != null ? Time.valueOf(course.getCourseStartTime()) : null;
            Time endTimeSql = course.getCourseEndTime() != null ? Time.valueOf(course.getCourseEndTime()) : null;

            statement.setTime(7, startTimeSql);
            statement.setTime(8, endTimeSql);

            statement.setString(9, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(10, course.getRoomId());
            statement.setString(11, course.getClassId()); // Set class_id
            statement.setFloat(12, course.getProgress());

            int rowsInserted = statement.executeUpdate();

            // Student enrollment logic: uses course.getCourseId() as the key for enrollment.class_id
            // This assumes enrollment.class_id actually stores course_id.
            // If enrollment.class_id refers to classes.class_id, this logic might need adjustment
            // to use course.getClassId() instead. For now, keeping existing logic.
            if (rowsInserted > 0 && course.getStudents() != null && !course.getStudents().isEmpty()) {
                for (Student student : course.getStudents()) {
                    // Assuming addStudentToCourse's second parameter (courseId) refers to the ID used in 'enrollment' table
                    addStudentToCourse(conn, course.getCourseId(), student.getId());
                }
            }
            return rowsInserted > 0;
        }
    }

    private boolean internalUpdate(Connection conn, Course course) throws SQLException {
        // Added class_id to the SQL query and parameter setting
        String sql = "UPDATE courses SET course_name = ?, subject = ?, start_date = ?, end_date = ?, " +
                "days_of_week = ?, start_time = ?, end_time = ?, teacher_id = ?, room_id = ?, " +
                "class_id = ?, progress = ? WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getSubject());
            statement.setDate(3, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(4, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setString(5, course.getDaysOfWeekAsString());

            // Use renamed getters from Course model
            Time startTimeSql = course.getCourseStartTime() != null ? Time.valueOf(course.getCourseStartTime()) : null;
            Time endTimeSql = course.getCourseEndTime() != null ? Time.valueOf(course.getCourseEndTime()) : null;

            statement.setTime(6, startTimeSql);
            statement.setTime(7, endTimeSql);

            statement.setString(8, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(9, course.getRoomId());
            statement.setString(10, course.getClassId()); // Set class_id
            statement.setFloat(11, course.getProgress());
            statement.setString(12, course.getCourseId());

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                // Student enrollment logic: uses course.getCourseId() as the key for enrollment.class_id
                // See comment in internalInsert regarding this logic.
                removeAllStudentsFromCourse(conn, course.getCourseId());
                if (course.getStudents() != null && !course.getStudents().isEmpty()) {
                    for (Student student : course.getStudents()) {
                        addStudentToCourse(conn, course.getCourseId(), student.getId());
                    }
                }
            }
            return rowsUpdated > 0;
        }
    }

    private boolean internalDelete(Connection conn, String courseId) throws SQLException {
        // Student enrollment logic: uses courseId (which is courses.course_id) as the key for enrollment.class_id
        // See comment in internalInsert regarding this logic.
        // If enrollments should be removed based on courses.class_id, this would need to fetch
        // the class_id for the course first.
        removeAllStudentsFromCourse(conn, courseId); // Assumes courseId is the key for enrollment removal

        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    Course getById(Connection conn, String courseId) throws SQLException {
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractCourseFromResultSet(conn, resultSet);
                }
            }
        }
        return null;
    }

    List<Course> getAll(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses";

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                courses.add(extractCourseFromResultSet(conn, resultSet));
            }
        }
        return courses;
    }

    private boolean addStudentToCourse(Connection conn, String keyForEnrollment, String studentId) throws SQLException {
        // This method assumes 'keyForEnrollment' is the value to be inserted into enrollment.class_id
        // In internalInsert/Update, course.getCourseId() is passed as keyForEnrollment.
        String sql = "INSERT INTO enrollment (student_id, class_id, enrollment_date, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.setString(2, keyForEnrollment); // keyForEnrollment is used as enrollment.class_id
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.setString(4, "Active");
            return statement.executeUpdate() > 0;
        }
    }

    private boolean removeStudentFromCourse(Connection conn, String keyForEnrollment, String studentId) throws SQLException {
        // This method assumes 'keyForEnrollment' is the value used in enrollment.class_id
        String sql = "DELETE FROM enrollment WHERE class_id = ? AND student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, keyForEnrollment); // keyForEnrollment is used as enrollment.class_id
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    private void removeAllStudentsFromCourse(Connection conn, String keyForEnrollment) throws SQLException {
        // This method assumes 'keyForEnrollment' is the value used in enrollment.class_id
        String sql = "DELETE FROM enrollment WHERE class_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, keyForEnrollment); // keyForEnrollment is used as enrollment.class_id
            statement.executeUpdate();
        }
    }

    List<Course> getCoursesByTeacherId(Connection conn, String teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE teacher_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCoursesByStudentId(Connection conn, String studentId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added c.class_id to the SELECT query
        // The JOIN c.course_id = e.class_id assumes enrollment.class_id stores courses.course_id
        String sql = "SELECT c.course_id, c.course_name, c.subject, c.start_date, c.end_date, c.days_of_week, " +
                "c.start_time, c.end_time, c.teacher_id, c.room_id, c.class_id, c.progress FROM courses c " +
                "JOIN enrollment e ON c.course_id = e.class_id " + // This join condition might need review based on true meaning of enrollment.class_id
                "WHERE e.student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> searchCourses(Connection conn, String searchTerm) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE course_name LIKE ? OR subject LIKE ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCoursesBySubject(Connection conn, String subject) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE subject = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, subject);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCoursesByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE " +
                "NOT (end_date < ? OR start_date > ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getActiveCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE " +
                "start_date <= ? AND end_date >= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getUpcomingCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE start_date > ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCompletedCourses(Connection conn, LocalDate referenceDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Added class_id to the SELECT query
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, days_of_week, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress FROM courses WHERE end_date < ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(referenceDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    // Overloaded method for convenience, using today's date
    List<Course> getCompletedCourses(Connection conn) throws SQLException {
        return getCompletedCourses(conn, LocalDate.now());
    }


    private boolean internalUpdateProgress(Connection conn, String courseId, float progress) throws SQLException {
        String sql = "UPDATE courses SET progress = ? WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setFloat(1, progress);
            statement.setString(2, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    List<Student> getStudentsByCourseId(Connection conn, String courseIdForEnrollmentKey) throws SQLException {
        checkStudentDAODependency();
        List<Student> students = new ArrayList<>();
        // This query assumes enrollment.class_id stores the 'courseIdForEnrollmentKey' (which is courses.course_id).
        String sql = "SELECT student_id FROM enrollment WHERE class_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseIdForEnrollmentKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    Student student = this.studentDAO.getStudentById(conn, studentId);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
        }
        return students;
    }

    private Course extractCourseFromResultSet(Connection conn, ResultSet resultSet) throws SQLException {
        String courseId = resultSet.getString("course_id");
        String courseName = resultSet.getString("course_name");
        String subject = resultSet.getString("subject");

        LocalDate startDate = null;
        Date startDateSql = resultSet.getDate("start_date");
        if (startDateSql != null) startDate = startDateSql.toLocalDate();

        LocalDate endDate = null;
        Date endDateSql = resultSet.getDate("end_date");
        if (endDateSql != null) endDate = endDateSql.toLocalDate();

        Course course = new Course(courseId, courseName, subject, startDate, endDate);

        course.setDaysOfWeekFromString(resultSet.getString("days_of_week"));

        Time startTimeSql = resultSet.getTime("start_time");
        course.setCourseSessionStartTime(startTimeSql != null ? startTimeSql.toLocalTime() : null); // Use renamed setter

        Time endTimeSql = resultSet.getTime("end_time");
        course.setCourseSessionEndTime(endTimeSql != null ? endTimeSql.toLocalTime() : null); // Use renamed setter

        String teacherId = resultSet.getString("teacher_id");
        String roomId = resultSet.getString("room_id");
        String classId = resultSet.getString("class_id"); // Retrieve class_id
        float progress = resultSet.getFloat("progress");

        course.setRoomId(roomId);
        course.setClassId(classId); // Set class_id on Course object
        course.setProgress(progress);

        if (teacherId != null && !teacherId.trim().isEmpty()) {
            checkTeacherDAODependency();
            Teacher teacher = this.teacherDAO.getById(conn, teacherId);
            course.setTeacher(teacher);
        }

        // Fetches students based on course.course_id (passed as courseId to getStudentsByCourseId).
        // This aligns with the assumption that enrollment.class_id stores courses.course_id.
        List<Student> students = getStudentsByCourseId(conn, courseId);
        course.setStudents(students != null ? students : new ArrayList<>());

        return course;
    }

    // --- Public Wrapper Methods ---
    public boolean save(Course course) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Manage transaction
            boolean success = internalInsert(conn, course);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error saving course: " + e.getMessage());
            // Consider logging the stack trace or using a proper logger
            return false;
        }
    }

    public boolean update(Course course) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Manage transaction
            boolean success = internalUpdate(conn, course);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating course: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Manage transaction
            boolean success = internalDelete(conn, courseId);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting course with ID: " + courseId + ": " + e.getMessage());
            return false;
        }
    }

    public Optional<Course> findById(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getById(conn, courseId));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding course by ID: " + courseId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<Course> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAll(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding all courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean addStudentToCourse(String courseId, String studentId) {
        // This public method uses 'courseId' (courses.course_id) as the key for enrollment.
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Auto-commit is usually default, explicit transaction not strictly needed for single op.
            return addStudentToCourse(conn, courseId, studentId);
        } catch (SQLException e) {
            System.err.println("Error adding student " + studentId + " to course " + courseId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removeStudentFromCourse(String courseId, String studentId) {
        // This public method uses 'courseId' (courses.course_id) as the key for enrollment.
        try (Connection conn = DatabaseConnection.getConnection()) {
            return removeStudentFromCourse(conn, courseId, studentId);
        } catch (SQLException e) {
            System.err.println("Error removing student " + studentId + " from course " + courseId + ": " + e.getMessage());
            return false;
        }
    }

    public List<Course> findCoursesByTeacherId(String teacherId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByTeacherId(conn, teacherId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by teacher ID: " + teacherId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesByStudentId(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByStudentId(conn, studentId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses for student ID: " + studentId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> search(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return searchCourses(conn, searchTerm);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error searching courses for term: '" + searchTerm + "': " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesBySubject(String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesBySubject(conn, subject);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by subject: " + subject + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByDateRange(conn, startDate, endDate);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by date range (" + startDate + " to " + endDate + "): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findActiveCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getActiveCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding active courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findUpcomingCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getUpcomingCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding upcoming courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findCompletedCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCompletedCourses(conn, LocalDate.now());
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding completed courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Course> findCompletedCourses(LocalDate referenceDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCompletedCourses(conn, referenceDate);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding completed courses: " + e.getMessage());
            return new ArrayList<>();
        }
    }


    public boolean updateProgressBasedOnDate(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            Course course = getById(conn, courseId);
            if (course == null) {
                System.err.println("Error updating progress: Course with ID " + courseId + " not found.");
                conn.rollback(); // Rollback if course not found
                return false;
            }
            float calculatedProgress = (float) course.calculateProgressBasedOnDate();
            boolean success = internalUpdateProgress(conn, courseId, calculatedProgress);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating progress for course ID: " + courseId + ": " + e.getMessage());
            // A rollback might be needed here if the connection is still open and an error occurred after setAutoCommit(false)
            // However, try-with-resources will close the connection. If an error occurs before commit/rollback, DB handles it.
            return false;
        }
    }

    public boolean updateProgress(String courseId, float progress) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Auto-commit is fine for single update unless part of a larger transaction managed externally
            return internalUpdateProgress(conn, courseId, progress);
        } catch (SQLException e) {
            System.err.println("Error setting progress for course ID: " + courseId + " to " + progress + ": " + e.getMessage());
            return false;
        }
    }

    public boolean hasTimeConflict(Course course1, Course course2) {
        if (course1 == null || course2 == null) return false;
        if (!course1.overlapsDateRange(course2)) return false;

        // Use renamed getters for start/end times and daysOfWeek list
        LocalTime start1 = course1.getCourseStartTime();
        LocalTime end1 = course1.getCourseEndTime();
        LocalTime start2 = course2.getCourseStartTime();
        LocalTime end2 = course2.getCourseEndTime();

        List<String> days1 = course1.getDaysOfWeekList(); // Use renamed getter
        List<String> days2 = course2.getDaysOfWeekList(); // Use renamed getter

        boolean hasDayIntersection = false;
        if (days1 != null && !days1.isEmpty() && days2 != null && !days2.isEmpty()) {
            for (String day1Str : days1) {
                if (day1Str != null && !day1Str.trim().isEmpty()) {
                    for (String day2Str : days2) {
                        if (day1Str.trim().equalsIgnoreCase(day2Str != null ? day2Str.trim() : "")) {
                            hasDayIntersection = true;
                            break;
                        }
                    }
                }
                if (hasDayIntersection) break;
            }
        }
        if (!hasDayIntersection) return false;

        if (start1 != null && end1 != null && start2 != null && end2 != null) {
            return !(end1.isBefore(start2) || start1.isAfter(end2) || end1.equals(start2) || start1.equals(end2));
        }
        return false; // No specific time slot conflict if times are not defined
    }
}

