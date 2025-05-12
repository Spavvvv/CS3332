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
    // Removed the 'connection' class member. Each method will manage its own connection or use a passed one.
    private ParentDAO parentDAO; // This will be injected
    private CourseDAO courseDAO; // This will be injected

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public StudentDAO() {
        // Dependencies ParentDAO and CourseDAO will be set by DaoManager or equivalent
    }

    /**
     * Set ParentDAO - used for dependency injection.
     *
     * @param parentDAO The ParentDAO instance
     */
    public void setParentDAO(ParentDAO parentDAO) {
        this.parentDAO = parentDAO;
    }

    /**
     * Set CourseDAO - used for dependency injection.
     *
     * @param courseDAO The CourseDAO instance
     */
    public void setCourseDAO(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    // Removed the private getParentDAO() and getCourseDAO() methods that used lazy initialization
    // Instead, access the injected DAOs directly and check for null if necessary,
    // or rely on the DaoManager to ensure they are always set.

    /**
     * Insert a new student into the database. Manages its own connection.
     *
     * @param student the student to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean insertStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (id, name, gender, contact_number, birthday, email) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection here
             PreparedStatement statement = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            prepareStatementFromStudent(statement, student);

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                if (student.getParent() != null) {
                    linkStudentToParent(conn, student.getId(), student.getParent().getId());
                }

                if (student.getCurrentCourses() != null && !student.getCurrentCourses().isEmpty()) {
                    for (Course course : student.getCurrentCourses()) {
                        enrollStudentInCourse(conn, student.getId(), course.getCourseId());
                    }
                }
                conn.commit(); // Commit transaction
            } else {
                conn.rollback(); // Rollback if insertion failed
            }

            return rowsInserted > 0;
        }
    }

    /**
     * Update an existing student in the database. Manages its own connection.
     *
     * @param student the student to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection here
             PreparedStatement statement = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            statement.setString(1, student.getName());
            statement.setString(2, student.getGender());
            statement.setString(3, student.getContactNumber());
            statement.setString(4, student.getBirthday());
            statement.setString(5, student.getEmail());
            statement.setString(6, student.getId());

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                // Update parent relationship
                removeAllParentLinks(conn, student.getId());
                if (student.getParent() != null) {
                    linkStudentToParent(conn, student.getId(), student.getParent().getId());
                }

                // Update course enrollments
                removeAllCourseEnrollments(conn, student.getId());
                if (student.getCurrentCourses() != null) {
                    for (Course course : student.getCurrentCourses()) {
                        enrollStudentInCourse(conn, student.getId(), course.getCourseId());
                    }
                }
                conn.commit(); // Commit transaction
            } else {
                conn.rollback(); // Rollback if update failed
            }

            return rowsUpdated > 0;
        }
    }

    /**
     * Delete a student from the database. Manages its own connection.
     *
     * @param studentId the ID of the student to delete
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteStudent(String studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) { // Get connection here
            conn.setAutoCommit(false); // Start transaction

            // Use the same connection for related operations within the transaction
            removeAllParentLinks(conn, studentId);
            removeAllCourseEnrollments(conn, studentId);

            String sql = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, studentId);
                int rowsDeleted = statement.executeUpdate();

                if (rowsDeleted > 0) {
                    conn.commit(); // Commit transaction
                } else {
                    conn.rollback(); // Rollback if deletion failed
                }
                return rowsDeleted > 0;
            }
        } // Connection is closed here
    }

    /**
     * Get a student by ID using an existing connection.
     * Helper for scenarios where a connection is already active.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return the Student object or null if not found
     * @throws SQLException if a database access error occurs, or ParentDAO/CourseDAO not set
     */
    public Student getStudentById(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Pass the connection down to extract method for related lookups
                    return extractStudentFromResultSet(conn, resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Get a student by ID. Manages its own connection.
     *
     * @param studentId the student ID
     * @return the Student object or null if not found
     * @throws SQLException if a database access error occurs, or ParentDAO/CourseDAO not set
     */
    public Student getStudentById(String studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and close connection here
            return getStudentById(conn, studentId); // Use the helper with the new connection
        }
    }

    /**
     * Find a student by ID (returns Optional) using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(Connection conn, String studentId) {
        try {
            // getStudentById will now handle the checks for injected DAOs
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException | IllegalStateException e) { // Catch IllegalStateException too
            System.err.println("Error finding student by ID using existing connection: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Find a student by ID (returns Optional). Manages its own connection.
     *
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and close connection here
            // getStudentById will now handle the checks for injected DAOs
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException | IllegalStateException e) { // Catch IllegalStateException too
            System.err.println("Error finding student by ID: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }


    /**
     * Get all students from the database. Manages its own connection.
     *
     * @return List of all students
     * @throws SQLException if a database access error occurs, or ParentDAO/CourseDAO not set
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection here
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                // Pass the connection down to extract method for related lookups
                students.add(extractStudentFromResultSet(conn, resultSet));
            }
        }

        return students;
    }

    /**
     * Search students by name or email. Manages its own connection.
     *
     * @param searchTerm the search term
     * @return List of matching students
     * @throws SQLException if a database access error occurs, or ParentDAO/CourseDAO not set
     */
    public List<Student> searchStudents(String searchTerm) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students WHERE name LIKE ? OR email LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection here
             PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Pass the connection down to extract method for related lookups
                    students.add(extractStudentFromResultSet(conn, resultSet));
                }
            }
        }

        return students;
    }

    /**
     * Enroll a student in a course using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean enrollStudentInCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "INSERT INTO course_student (course_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Enroll a student in a course. Manages its own connection.
     *
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean enrollStudentInCourse(String studentId, String courseId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return enrollStudentInCourse(conn, studentId, courseId);
        }
    }

    /**
     * Withdraw a student from a course using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean withdrawStudentFromCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE course_id = ? AND student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Withdraw a student from a course. Manages its own connection.
     *
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean withdrawStudentFromCourse(String studentId, String courseId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return withdrawStudentFromCourse(conn, studentId, courseId);
        }
    }

    /**
     * Link a student to a parent using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @param parentId  the parent ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean linkStudentToParent(Connection conn, String studentId, String parentId) throws SQLException {
        String sql = "INSERT INTO parent_student (parent_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, studentId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Link a student to a parent. Manages its own connection.
     *
     * @param studentId the student ID
     * @param parentId  the parent ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean linkStudentToParent(String studentId, String parentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return linkStudentToParent(conn, studentId, parentId);
        }
    }

    /**
     * Remove all parent links for a student using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @throws SQLException if a database access error occurs
     */
    private void removeAllParentLinks(Connection conn, String studentId) throws SQLException {
        String sql = "DELETE FROM parent_student WHERE student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }

    /**
     * Remove all course enrollments for a student using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @throws SQLException if a database access error occurs
     */
    private void removeAllCourseEnrollments(Connection conn, String studentId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }


    /**
     * Get parent of a student using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return Parent of the student or null if not found
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if ParentDAO dependency has not been set
     */
    private Parent getParentByStudentId(Connection conn, String studentId) throws SQLException {
        if (this.parentDAO == null) {
            throw new IllegalStateException("ParentDAO dependency has not been set on StudentDAO.");
        }
        String sql = "SELECT parent_id FROM parent_student WHERE student_id = ? LIMIT 1";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String parentId = resultSet.getString("parent_id");
                    // Call the injected ParentDAO, passing the connection
                    return this.parentDAO.getById(conn, parentId); // Use injected DAO
                }
            }
        }
        return null;
    }

    /**
     * Get courses for a student using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return List of courses the student is enrolled in
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if CourseDAO dependency has not been set
     */
    private List<Course> getCoursesByStudentId(Connection conn, String studentId) throws SQLException {
        if (this.courseDAO == null) {
            throw new IllegalStateException("CourseDAO dependency has not been set on StudentDAO.");
        }
        // Call the injected CourseDAO, passing the connection
        // This requires CourseDAO to have a getCoursesByStudentId(Connection conn, String studentId) method
        return this.courseDAO.getCoursesByStudentId(conn, studentId); // Use injected DAO
    }

    /**
     * Helper method to extract a Student object from a ResultSet
     * Accepts the active connection to allow loading related entities without closing resources.
     *
     * @param conn the active database connection
     * @param resultSet ResultSet containing student data
     * @return the Student object
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if ParentDAO or CourseDAO dependency has not been set
     */
    private Student extractStudentFromResultSet(Connection conn, ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String name = resultSet.getString("name");
        String gender = resultSet.getString("gender");
        String contactNumber = resultSet.getString("contact_number");
        String birthday = resultSet.getString("birthday");
        String email = resultSet.getString("email");

        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setGender(gender);
        student.setContactNumber(contactNumber);
        student.setBirthday(birthday);
        student.setEmail(email);

        try {
            // Get parent information - Use injected ParentDAO, Pass the connection
            // getParentByStudentId will check if parentDAO is set
            Parent parent = getParentByStudentId(conn, id);
            student.setParent(parent);

            // Get course information - Use injected CourseDAO, Pass the connection
            // getCoursesByStudentId will check if courseDAO is set
            List<Course> courses = getCoursesByStudentId(conn, id);
            student.setCurrentCourses(courses);
        } catch (SQLException | IllegalStateException e) { // Catch IllegalStateException too
            System.err.println("Error loading related data for student " + id + " using existing connection: " + e.getMessage());
            e.printStackTrace();
            // Continue and return the student even if related data failed to load
        }

        return student;
    }

    /**
     * Helper method to prepare a PreparedStatement from a Student object.
     * Does NOT manage connection or close the statement.
     *
     * @param statement the PreparedStatement to prepare
     * @param student the Student providing the values
     * @throws SQLException if a database access error occurs
     */
    private void prepareStatementFromStudent(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getId());
        statement.setString(2, student.getName());
        statement.setString(3, student.getGender());
        statement.setString(4, student.getContactNumber());
        statement.setString(5, student.getBirthday());
        statement.setString(6, student.getEmail());
    }


    // -- Public wrapper methods for external calls that need to manage their own connection --
    // These methods delegate to internal helpers that accept a Connection

    /**
     * Save a new student. Manages its own connection.
     * @param student the student to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(Student student) {
        try {
            return insertStudent(student);
        } catch (SQLException e) {
            System.err.println("Error saving student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing student. Manages its own connection.
     * @param student the student to update
     * @return true if the update was successful, false otherwise
     */
    public boolean update(Student student) {
        try {
            return updateStudent(student);
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a student. Manages its own connection.
     * @param studentId the ID of the student to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String studentId) {
        try {
            return deleteStudent(studentId);
        } catch (SQLException e) {
            System.err.println("Error deleting student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all students. Manages its own connection.
     * @return List of all students
     * @throws IllegalStateException if ParentDAO or CourseDAO dependency has not been set (propagated from helpers)
     */
    public List<Student> findAll() {
        try {
            return getAllStudents();
        } catch (SQLException | IllegalStateException e) { // Catch IllegalStateException too
            System.err.println("Error finding all students: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Search students. Manages its own connection.
     * @param searchTerm the search term
     * @return List of matching students
     * @throws IllegalStateException if ParentDAO or CourseDAO dependency has not been set (propagated from helpers)
     */
    public List<Student> search(String searchTerm) {
        try {
            return searchStudents(searchTerm);
        } catch (SQLException | IllegalStateException e) { // Catch IllegalStateException too
            System.err.println("Error searching students: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    /**
     * Find all students associated with a specific session ID.
     *
     * @param conn The active database connection.
     * @param sessionId The ID of the session.
     * @return A list of students associated with the session.
     * @throws SQLException if a database access error occurs.
     * @throws IllegalStateException if ParentDAO or CourseDAO dependency has not been set.
     */
    public List<Student> findBySessionId(Connection conn, String sessionId) throws SQLException {
        List<Student> students = new ArrayList<>();
        // TODO: Replace with your actual SQL query that joins students with sessions
        // This is a placeholder query assuming a linking table like 'session_student'
        String sql = "SELECT s.id, s.name, s.gender, s.contact_number, s.birthday, s.email " +
                "FROM students s " +
                "JOIN session_student ss ON s.id = ss.student_id " + // Assuming a linking table
                "WHERE ss.session_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Use the existing helper to extract student data and related entities
                    students.add(extractStudentFromResultSet(conn, resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding students by session ID: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be handled by the caller (ExamsController)
        } catch (IllegalStateException e) {
            System.err.println("Dependency not set while finding students by session ID: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be handled by the caller
        }

        return students;
    }

}
