package src.dao;

import src.model.system.course.Course;
import src.model.system.course.CourseDate;
import src.model.person.Student;
import src.model.person.Teacher;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for Course entities
 */
public class CourseDAO {
    private final Connection connection;
    private StudentDAO studentDAO;
    private TeacherDAO teacherDAO;

    /**
     * Constructor initializes database connection only
     * We don't initialize other DAOs here to avoid circular dependency
     */
    public CourseDAO() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
    }

    /**
     * Set StudentDAO - used to resolve circular dependency
     *
     * @param studentDAO The StudentDAO instance
     */
    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Set TeacherDAO - used to resolve circular dependency
     *
     * @param teacherDAO The TeacherDAO instance
     */
    public void setTeacherDAO(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    /**
     * Get StudentDAO instance with lazy initialization
     *
     * @return StudentDAO instance
     */
    private StudentDAO getStudentDAO() throws SQLException {
        if (studentDAO == null) {
            studentDAO = new StudentDAO();
            studentDAO.setCourseDAO(this); // Set back reference
        }
        return studentDAO;
    }

    /**
     * Get TeacherDAO instance with lazy initialization
     *
     * @return TeacherDAO instance
     */
    private TeacherDAO getTeacherDAO() throws SQLException {
        if (teacherDAO == null) {
            teacherDAO = new TeacherDAO();
        }
        return teacherDAO;
    }

    /**
     * Insert a new course into the database
     *
     * @param course the course to insert
     * @return true if successful
     */
    public boolean insert(Course course) throws SQLException {
        // First insert the course details
        String sql = "INSERT INTO courses (course_id, course_name, subject, start_date, end_date, " +
                "days_of_week, start_time, end_time, teacher_id, room_id, progress) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            CourseDate date = course.getDate();
            statement.setString(1, course.getCourseId());
            statement.setString(2, course.getCourseName());
            statement.setString(3, course.getSubject());
            statement.setDate(4, Date.valueOf(date.getStartDate()));
            statement.setDate(5, Date.valueOf(date.getEndDate()));
            statement.setString(6, date.getDaysOfWeekAsString());
            statement.setTime(7, Time.valueOf(date.getStartTime()));
            statement.setTime(8, Time.valueOf(date.getEndTime()));
            statement.setString(9, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(10, course.getRoomId());
            statement.setFloat(11, (float) date.getProgressPercentage());

            int rowsInserted = statement.executeUpdate();

            // If insertion successful and course has students, add course-student relationships
            if (rowsInserted > 0 && course.getStudents() != null) {
                for (Student student : course.getStudents()) {
                    addStudentToCourse(course.getCourseId(), student.getId());
                }
            }

            return rowsInserted > 0;
        }
    }

    /**
     * Update an existing course in the database
     *
     * @param course the course to update
     * @return true if successful
     */
    public boolean update(Course course) throws SQLException {
        String sql = "UPDATE courses SET course_name = ?, subject = ?, start_date = ?, end_date = ?, " +
                "days_of_week = ?, start_time = ?, end_time = ?, teacher_id = ?, room_id = ?, " +
                "progress = ? WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            CourseDate date = course.getDate();
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getSubject());
            statement.setDate(3, Date.valueOf(date.getStartDate()));
            statement.setDate(4, Date.valueOf(date.getEndDate()));
            statement.setString(5, date.getDaysOfWeekAsString());
            statement.setTime(6, Time.valueOf(date.getStartTime()));
            statement.setTime(7, Time.valueOf(date.getEndTime()));
            statement.setString(8, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(9, course.getRoomId());
            statement.setFloat(10, (float) date.getProgressPercentage());
            statement.setString(11, course.getCourseId());

            int rowsUpdated = statement.executeUpdate();

            // Update student enrollments if needed
            if (rowsUpdated > 0 && course.getStudents() != null) {
                // First remove all existing student enrollments
                removeAllStudentsFromCourse(course.getCourseId());

                // Then add the current students
                for (Student student : course.getStudents()) {
                    addStudentToCourse(course.getCourseId(), student.getId());
                }
            }

            return rowsUpdated > 0;
        }
    }

    /**
     * Delete a course from the database
     *
     * @param courseId the ID of the course to delete
     * @return true if successful
     */
    public boolean delete(String courseId) throws SQLException {
        // First remove all student enrollments
        removeAllStudentsFromCourse(courseId);

        // Then delete the course
        String sql = "DELETE FROM courses WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get a course by ID
     *
     * @param courseId the course ID
     * @return the Course object or null if not found
     */
    public Course getById(String courseId) throws SQLException {
        String sql = "SELECT * FROM courses WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractCourseFromResultSet(resultSet);
                }
            }
        }

        return null;
    }

    /**
     * Get all courses from the database
     *
     * @return List of all courses
     */
    public List<Course> getAll() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                courses.add(extractCourseFromResultSet(resultSet));
            }
        }

        return courses;
    }

    /**
     * Add a student to a course
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @return true if successful
     */
    public boolean addStudentToCourse(String courseId, String studentId) throws SQLException {
        String sql = "INSERT INTO course_student (course_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove a student from a course
     *
     * @param courseId the course ID
     * @param studentId the student ID
     * @return true if successful
     */
    public boolean removeStudentFromCourse(String courseId, String studentId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE course_id = ? AND student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove all students from a course
     *
     * @param courseId the course ID
     */
    private void removeAllStudentsFromCourse(String courseId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.executeUpdate();
        }
    }

    /**
     * Get courses by teacher ID
     *
     * @param teacherId the teacher ID
     * @return List of courses taught by the teacher
     */
    public List<Course> getCoursesByTeacherId(String teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE teacher_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get courses by student ID
     *
     * @param studentId the student ID
     * @return List of courses the student is enrolled in
     */
    public List<Course> getCoursesByStudentId(String studentId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.* FROM courses c " +
                "JOIN course_student cs ON c.course_id = cs.course_id " +
                "WHERE cs.student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Search courses by name or subject
     *
     * @param searchTerm the search term
     * @return List of matching courses
     */
    public List<Course> searchCourses(String searchTerm) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE course_name LIKE ? OR subject LIKE ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get courses by subject
     *
     * @param subject the subject name
     * @return List of courses for the subject
     */
    public List<Course> getCoursesBySubject(String subject) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE subject = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get courses by date range
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return List of courses within the date range
     */
    public List<Course> getCoursesByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE " +
                "NOT (end_date < ? OR start_date > ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get active courses (courses that have started but not completed)
     *
     * @return List of active courses
     */
    public List<Course> getActiveCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM courses WHERE " +
                "start_date <= ? AND end_date >= ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get upcoming courses (courses that have not started yet)
     *
     * @return List of upcoming courses
     */
    public List<Course> getUpcomingCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM courses WHERE start_date > ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Get completed courses (courses that have ended)
     *
     * @return List of completed courses
     */
    public List<Course> getCompletedCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM courses WHERE end_date < ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(resultSet));
                }
            }
        }

        return courses;
    }

    /**
     * Update course progress based on CourseDate
     *
     * @param courseId the course ID
     * @return true if successful
     */
    public boolean updateProgressFromDate(String courseId) throws SQLException {
        Course course = getById(courseId);
        if (course == null) {
            return false;
        }

        CourseDate date = course.getDate();
        float progress = (float) date.getProgressPercentage();

        return updateProgress(courseId, progress);
    }

    /**
     * Update course progress
     *
     * @param courseId the course ID
     * @param progress the new progress value
     * @return true if successful
     */
    public boolean updateProgress(String courseId, float progress) throws SQLException {
        String sql = "UPDATE courses SET progress = ? WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setFloat(1, progress);
            statement.setString(2, courseId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get students enrolled in a course
     *
     * @param courseId the course ID
     * @return List of students enrolled in the course
     */
    public List<Student> getStudentsByCourseId(String courseId) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_id FROM course_student WHERE course_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    Student student = getStudentDAO().getStudentById(studentId);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
        }

        return students;
    }

    /**
     * Check if two courses have time conflicts
     *
     * @param course1 the first course
     * @param course2 the second course
     * @return true if there is a time conflict
     */
    public boolean hasTimeConflict(Course course1, Course course2) {
        // Check if dates overlap
        if (!course1.getDate().overlaps(course2.getDate())) {
            return false;
        }

        // Check if time slots overlap
        LocalTime start1 = course1.getDate().getStartTime();
        LocalTime end1 = course1.getDate().getEndTime();
        LocalTime start2 = course2.getDate().getStartTime();
        LocalTime end2 = course2.getDate().getEndTime();

        // Check days of week intersection
        boolean hasDayIntersection = false;
        String[] days1 = course1.getDate().getDaysOfWeekAsString().split(",");
        String[] days2 = course2.getDate().getDaysOfWeekAsString().split(",");

        for (String day1 : days1) {
            for (String day2 : days2) {
                if (day1.trim().equals(day2.trim())) {
                    hasDayIntersection = true;
                    break;
                }
            }
            if (hasDayIntersection) break;
        }

        if (!hasDayIntersection) {
            return false;
        }

        // Check if time periods overlap
        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    /**
     * Helper method to extract a Course object from a ResultSet
     */
    private Course extractCourseFromResultSet(ResultSet resultSet) throws SQLException {
        String courseId = resultSet.getString("course_id");
        String courseName = resultSet.getString("course_name");
        String subject = resultSet.getString("subject");
        LocalDate startDate = resultSet.getDate("start_date").toLocalDate();
        LocalDate endDate = resultSet.getDate("end_date").toLocalDate();
        String daysOfWeek = resultSet.getString("days_of_week");
        LocalTime startTime = resultSet.getTime("start_time").toLocalTime();
        LocalTime endTime = resultSet.getTime("end_time").toLocalTime();
        String teacherId = resultSet.getString("teacher_id");
        String roomId = resultSet.getString("room_id");

        // Create CourseDate object
        CourseDate courseDate = new CourseDate(startDate, endDate);
        courseDate.setStartTime(startTime);
        courseDate.setEndTime(endTime);
        courseDate.setDaysOfWeekFromString(daysOfWeek);

        // Create Course object
        Course course = new Course(courseId, courseName, subject, courseDate);
        course.setRoomId(roomId);

        // Set teacher if available
        if (teacherId != null) {
            Teacher teacher = getTeacherDAO().getById(teacherId);
            if (teacher != null) {
                course.setTeacher(teacher);
            }
        }

        // Load students
        List<Student> students = getStudentsByCourseId(courseId);
        for (Student student : students) {
            course.addStudent(student);
        }

        return course;
    }
}
