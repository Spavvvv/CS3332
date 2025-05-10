package src.dao;

import src.model.person.Parent;
import src.model.person.Student;
import src.model.system.course.Course;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Student entities
 */
public class StudentDAO {
    private final Connection connection;
    private ParentDAO parentDAO;
    private CourseDAO courseDAO;

    /**
     * Constructor initializes database connection
     * We don't initialize ParentDAO and CourseDAO here to avoid circular dependency
     */
    public StudentDAO() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
        // No initialization of ParentDAO or CourseDAO in constructor
    }

    /**
     * Set ParentDAO - used to resolve circular dependency
     *
     * @param parentDAO The ParentDAO instance
     */
    public void setParentDAO(ParentDAO parentDAO) {
        this.parentDAO = parentDAO;
    }

    /**
     * Set CourseDAO - used to resolve circular dependency
     *
     * @param courseDAO The CourseDAO instance
     */
    public void setCourseDAO(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    /**
     * Get ParentDAO instance with lazy initialization
     *
     * @return ParentDAO instance
     */
    private ParentDAO getParentDAO() throws SQLException {
        if (parentDAO == null) {
            parentDAO = new ParentDAO();
        }
        return parentDAO;
    }

    /**
     * Get CourseDAO instance with lazy initialization
     *
     * @return CourseDAO instance
     */
    private CourseDAO getCourseDAO() throws SQLException {
        if (courseDAO == null) {
            courseDAO = new CourseDAO();
            courseDAO.setStudentDAO(this); // Set back reference
        }
        return courseDAO;
    }

    /**
     * Insert a new student into the database
     *
     * @param student the student to insert
     * @return true if successful
     */
    public boolean insertStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (id, name, gender, contact_number, birthday, email) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getId());
            statement.setString(2, student.getName());
            statement.setString(3, student.getGender());
            statement.setString(4, student.getContactNumber());
            statement.setString(5, student.getBirthday());
            statement.setString(6, student.getEmail());

            int rowsInserted = statement.executeUpdate();

            // If successful insertion and student has parent
            if (rowsInserted > 0 && student.getParent() != null) {
                linkStudentToParent(student.getId(), student.getParent().getId());
            }

            // If student has courses, link them
            if (rowsInserted > 0 && student.getCurrentCourses() != null && !student.getCurrentCourses().isEmpty()) {
                for (Course course : student.getCurrentCourses()) {
                    enrollStudentInCourse(student.getId(), course.getCourseId());
                }
            }

            return rowsInserted > 0;
        }
    }

    /**
     * Update an existing student in the database
     *
     * @param student the student to update
     * @return true if successful
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getName());
            statement.setString(2, student.getGender());
            statement.setString(3, student.getContactNumber());
            statement.setString(4, student.getBirthday());
            statement.setString(5, student.getEmail());
            statement.setString(6, student.getId());

            int rowsUpdated = statement.executeUpdate();

            // Update parent relationship if needed
            if (rowsUpdated > 0 && student.getParent() != null) {
                // First remove all existing parent-student links
                removeAllParentLinks(student.getId());
                // Then add the current parent
                linkStudentToParent(student.getId(), student.getParent().getId());
            }

            // Update course enrollments if needed
            if (rowsUpdated > 0 && student.getCurrentCourses() != null) {
                // First remove all existing course enrollments
                removeAllCourseEnrollments(student.getId());
                // Then add current courses
                for (Course course : student.getCurrentCourses()) {
                    enrollStudentInCourse(student.getId(), course.getCourseId());
                }
            }

            return rowsUpdated > 0;
        }
    }

    /**
     * Delete a student from the database
     *
     * @param studentId the ID of the student to delete
     * @return true if successful
     */
    public boolean deleteStudent(String studentId) throws SQLException {
        // First remove all parent links
        removeAllParentLinks(studentId);

        // Then remove all course enrollments
        removeAllCourseEnrollments(studentId);

        // Finally delete the student
        String sql = "DELETE FROM students WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get a student by ID
     *
     * @param studentId the student ID
     * @return the Student object or null if not found
     */
    public Student getStudentById(String studentId) throws SQLException {
        String sql = "SELECT * FROM students WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractStudentFromResultSet(resultSet);
                }
            }
        }

        return null;
    }

    /**
     * Find a student by ID (returns Optional)
     *
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(String studentId) {
        try {
            Student student = getStudentById(studentId);
            return Optional.ofNullable(student);
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding student by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get all students from the database
     *
     * @return List of all students
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                students.add(extractStudentFromResultSet(resultSet));
            }
        }

        return students;
    }

    /**
     * Search students by name or email
     *
     * @param searchTerm the search term
     * @return List of matching students
     */
    public List<Student> searchStudents(String searchTerm) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE name LIKE ? OR email LIKE ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        }

        return students;
    }

    /**
     * Enroll a student in a course
     *
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     */
    public boolean enrollStudentInCourse(String studentId, String courseId) throws SQLException {
        String sql = "INSERT INTO course_student (course_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Withdraw a student from a course
     *
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     */
    public boolean withdrawStudentFromCourse(String studentId, String courseId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE course_id = ? AND student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Link a student to a parent
     *
     * @param studentId the student ID
     * @param parentId  the parent ID
     * @return true if successful
     */
    public boolean linkStudentToParent(String studentId, String parentId) throws SQLException {
        String sql = "INSERT INTO parent_student (parent_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove all parent links for a student
     *
     * @param studentId the student ID
     */
    private void removeAllParentLinks(String studentId) throws SQLException {
        String sql = "DELETE FROM parent_student WHERE student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }

    /**
     * Remove all course enrollments for a student
     *
     * @param studentId the student ID
     */
    private void removeAllCourseEnrollments(String studentId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }

    /**
     * Get parent of a student
     *
     * @param studentId the student ID
     * @return Parent of the student or null if not found
     */
    public Parent getParentByStudentId(String studentId) throws SQLException {
        String sql = "SELECT parent_id FROM parent_student WHERE student_id = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String parentId = resultSet.getString("parent_id");
                    return getParentDAO().getById(parentId);
                }
            }
        }

        return null;
    }

    /**
     * Get courses for a student
     *
     * @param studentId the student ID
     * @return List of courses the student is enrolled in
     */
    public List<Course> getCoursesByStudentId(String studentId) throws SQLException {
        return getCourseDAO().getCoursesByStudentId(studentId);
    }

    /**
     * Helper method to extract a Student object from a ResultSet
     */
    private Student extractStudentFromResultSet(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String name = resultSet.getString("name");
        String gender = resultSet.getString("gender");
        String contactNumber = resultSet.getString("contact_number");
        String birthday = resultSet.getString("birthday");
        String email = resultSet.getString("email");

        // Create the student object
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setGender(gender);
        student.setContactNumber(contactNumber);
        student.setBirthday(birthday);
        student.setEmail(email);

        try {
            // Get parent information
            Parent parent = getParentByStudentId(id);
            student.setParent(parent);

            // Get course information
            List<Course> courses = getCoursesByStudentId(id);
            student.setCurrentCourses(courses);
        } catch (SQLException e) {
            // Log the exception but continue - we still want to return the student
            System.err.println("Error loading related data for student " + id + ": " + e.getMessage());
        }

        return student;
    }
}
