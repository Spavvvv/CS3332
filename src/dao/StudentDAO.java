
package src.dao;

import javafx.scene.control.Alert;
import src.model.person.Parent;
import src.model.person.Student;
import src.model.system.course.Course;
import utils.DatabaseConnection;
import java.util.UUID;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Student entities
 * Modified to use explicit methods for loading related entities (Parent, Courses)
 * to avoid recursive loading issues.
 */
public class StudentDAO {
    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

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

    /**
     * Insert a new student into the database. Manages its own connection and transaction.
     * Related entities (Parent, Courses) must be linked separately *after* the student is inserted,
     * or included in the transaction here if the Student object already contains them.
     * This version links parent and courses within the transaction if present in the student object.
     *
     * @param student the student to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean insertStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (id, name, gender, contact_number, birthday, email, class_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // Assuming your students table has a class_id column. Adjust if needed.

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            prepareStatementFromStudent(statement, student); // This helper needs to be updated for class_id
            // If your prepareStatementFromStudent doesn't handle class_id, you'll need to set it here:
            // statement.setString(7, student.getClassId()); // Assuming Student model has getClassId()

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                // Link related entities within the same transaction
                if (student.getParent() != null && student.getParent().getId() != null) {
                    linkStudentToParent(conn, student.getId(), student.getParent().getId());
                }

                if (student.getCurrentCourses() != null && !student.getCurrentCourses().isEmpty()) {
                    for (Course course : student.getCurrentCourses()) {
                        if (course.getCourseId() != null) {
                            enrollStudentInCourse(conn, student.getId(), course.getCourseId());
                        }
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
     * Update an existing student in the database. Manages its own connection and transaction.
     * Updates basic student details and relationship links.
     *
     * @param student the student to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ?, class_id = ? WHERE id = ?";
        // Assuming your students table has a class_id column. Adjust if needed.

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            statement.setString(1, student.getName());
            statement.setString(2, student.getGender());
            statement.setString(3, student.getContactNumber());
            statement.setString(4, student.getBirthday());
            statement.setString(5, student.getEmail());
            statement.setString(6, student.getClassId()); // Assuming Student model has getClassId()
            statement.setString(7, student.getId());


            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                // Update parent relationship by removing all and re-adding if parent exists
                removeAllParentLinks(conn, student.getId());
                if (student.getParent() != null && student.getParent().getId() != null) {
                    linkStudentToParent(conn, student.getId(), student.getParent().getId());
                }

                // Update course enrollments by removing all and re-adding current ones
                removeAllCourseEnrollments(conn, student.getId());
                if (student.getCurrentCourses() != null) {
                    for (Course course : student.getCurrentCourses()) {
                        if (course.getCourseId() != null) {
                            enrollStudentInCourse(conn, student.getId(), course.getCourseId());
                        }
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
     * Delete a student from the database. Manages its own connection and transaction.
     * Removes related links before deleting the student record.
     *
     * @param studentId the ID of the student to delete
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteStudent(String studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            // Use the same connection for related operations within the transaction
            removeAllParentLinks(conn, studentId);
            removeAllCourseEnrollments(conn, studentId);
            // You might also want to remove student from student_session or attendance records if they exist
            // removeStudentFromSessions(conn, studentId); // Example

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
     * This method *only* retrieves the basic student details, including class_id.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return the Student object with basic details or null if not found
     * @throws SQLException if a database access error occurs
     */
    public Student getStudentById(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT id, name, gender, contact_number, birthday, email, class_id FROM students WHERE id = ?";
        // Assuming students table has class_id

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
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
     * Get a student by ID. Manages its own connection.
     * This method *only* retrieves the basic student details, including class_id.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param studentId the student ID
     * @return the Student object with basic details or null if not found
     * @throws SQLException if a database access error occurs
     */
    public Student getStudentById(String studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getStudentById(conn, studentId);
        }
    }

    /**
     * Find a student by ID (returns Optional) using an existing connection.
     * This method *only* retrieves the basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(Connection conn, String studentId) {
        try {
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by ID using existing connection: " + studentId, e);
            return Optional.empty();
        }
    }

    /**
     * Find a student by ID (returns Optional). Manages its own connection.
     * This method *only* retrieves the basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student by ID: " + studentId, e);
            return Optional.empty();
        }
    }

    /**
     * Get all students from the database. Manages its own connection.
     * This method *only* retrieves basic student details, including class_id.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @return List of all students with basic details
     * @throws SQLException if a database access error occurs
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email, class_id FROM students";
        // Assuming students table has class_id

        try (Connection conn = DatabaseConnection.getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                students.add(extractStudentFromResultSet(resultSet));
            }
        }
        return students;
    }

    /**
     * Search students by name or email. Manages its own connection.
     * This method *only* retrieves basic student details, including class_id.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param searchTerm the search term
     * @return List of matching students with basic details
     * @throws SQLException if a database access error occurs
     */
    public List<Student> searchStudents(String searchTerm) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email, class_id FROM students WHERE name LIKE ? OR email LIKE ?";
        // Assuming students table has class_id

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
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

    private boolean enrollStudentInCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "INSERT INTO course_student (course_id, student_id) VALUES (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean enrollStudentInCourse(String studentId, String courseId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return enrollStudentInCourse(conn, studentId, courseId);
        }
    }

    private boolean withdrawStudentFromCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE course_id = ? AND student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean withdrawStudentFromCourse(String studentId, String courseId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return withdrawStudentFromCourse(conn, studentId, courseId);
        }
    }

    private boolean linkStudentToParent(Connection conn, String studentId, String parentId) throws SQLException {
        String sql = "INSERT INTO parent_student (parent_id, student_id) VALUES (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean linkStudentToParent(String studentId, String parentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return linkStudentToParent(conn, studentId, parentId);
        }
    }

    private void removeAllParentLinks(Connection conn, String studentId) throws SQLException {
        String sql = "DELETE FROM parent_student WHERE student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }

    private void removeAllCourseEnrollments(Connection conn, String studentId) throws SQLException {
        String sql = "DELETE FROM course_student WHERE student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            statement.executeUpdate();
        }
    }

    private Parent getParentForStudent(Connection conn, String studentId) throws SQLException {
        if (this.parentDAO == null) {
            throw new IllegalStateException("ParentDAO dependency has not been set on StudentDAO. Cannot load parent.");
        }
        String sql = "SELECT parent_id FROM parent_student WHERE student_id = ? LIMIT 1";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String parentId = resultSet.getString("parent_id");
                    return this.parentDAO.getById(conn, parentId); // Assuming ParentDAO has getById(conn, id)
                }
            }
        }
        return null;
    }

    public Optional<Parent> getParentForStudent(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getParentForStudent(conn, studentId));
        } catch (SQLException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error getting parent for student " + studentId, e);
            return Optional.empty();
        }
    }

    private List<Course> getCoursesForStudent(Connection conn, String studentId) throws SQLException {
        if (this.courseDAO == null) {
            throw new IllegalStateException("CourseDAO dependency has not been set on StudentDAO. Cannot load courses.");
        }
        return this.courseDAO.getCoursesByStudentId(conn, studentId); // Assuming CourseDAO has this method
    }

    public List<Course> getCoursesForStudent(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesForStudent(conn, studentId);
        } catch (SQLException | IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Error getting courses for student " + studentId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to extract a Student object from a ResultSet.
     * This method extracts basic student data including class_id.
     * It does NOT load related entities (Parent, Courses).
     *
     * @param resultSet ResultSet containing student data
     * @return the Student object with basic details
     * @throws SQLException if a database access error occurs
     */
    private Student extractStudentFromResultSet(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String name = resultSet.getString("name");
        String gender = resultSet.getString("gender");
        String contactNumber = resultSet.getString("contact_number");
        String birthday = resultSet.getString("birthday");
        String email = resultSet.getString("email");
        String classId = resultSet.getString("class_id"); // Extract class_id

        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setGender(gender);
        student.setContactNumber(contactNumber);
        student.setBirthday(birthday);
        student.setEmail(email);
        student.setClassId(classId); // Set class_id on the student object

        student.setParent(null);
        student.setCurrentCourses(new ArrayList<>());

        return student;
    }

    /**
     * Helper method to prepare a PreparedStatement from a Student object.
     * Assumes student object has classId.
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
        statement.setString(7, student.getClassId()); // Assuming Student model has getClassId() and setClassId()
        // And the SQL for INSERT has 7 placeholders
    }

    public boolean save(Student student) {
        try {
            return insertStudent(student);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving student: " + student.getId(), e);
            return false;
        }
    }

    public boolean update(Student student) {
        try {
            return updateStudent(student);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student: " + student.getId(), e);
            return false;
        }
    }

    public boolean delete(String studentId) {
        try {
            return deleteStudent(studentId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting student: " + studentId, e);
            return false;
        }
    }

    public List<Student> findAll() {
        try {
            return getAllStudents();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all students", e);
            return new ArrayList<>();
        }
    }

    public List<Student> search(String searchTerm) {
        try {
            return searchStudents(searchTerm);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching students with term: " + searchTerm, e);
            return new ArrayList<>();
        }
    }

    public List<Student> findBySessionId(String sessionId) throws SQLException {
        List<Student> students = new ArrayList<>();
        // This SQL assumes a direct link or an intermediate table like session_student or attendance
        // If students are linked to sessions via their class, and sessions are linked to classes,
        // the join might be more complex (e.g., students -> classes -> class_sessions).
        // The current query assumes a table `session_student` links students directly to sessions.
        // If your schema is different (e.g., students have a class_id, and sessions have a class_id),
        // you would query students by the class_id associated with the session_id.

        // Let's assume for now that students are linked to sessions via an "attendance" or "session_student" table.
        // If students are primarily identified by their `class_id` for a session, it's better to first
        // get the `class_id` for the `sessionId` and then use `findByClassId`.
        // However, if a session can have students from multiple classes (unlikely for typical school structure)
        // or if there's a direct `session_student` link, this query is okay.

        String sql = "SELECT s.id, s.name, s.gender, s.contact_number, s.birthday, s.email, s.class_id " +
                "FROM students s " +
                "JOIN attendance a ON s.id = a.student_id " + // Or session_student table
                "WHERE a.session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by session ID: " + sessionId, e);
            throw e;
        }
        return students;
    }
  
     * Finds all students belonging to a specific class ID.
     * Manages its own connection.
     * Returns students with basic data only (including their class_id).
     *
     * @param classId The ID of the class.
     * @return A list of students belonging to the class. Returns an empty list if no students are found or on error.
     */
    public List<Student> findByClassId(String classId) {
        List<Student> students = new ArrayList<>();
        // Ensure your 'students' table has a 'class_id' column or similar foreign key to the 'classes' table.
        String sql = "SELECT id, name, gender, contact_number, birthday, email, class_id FROM students WHERE class_id = ?";

        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "findByClassId called with null or empty classId.");
            return students; // Return empty list for invalid input
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, classId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Uses the updated extractStudentFromResultSet which includes class_id
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by class ID: " + classId, e);
            // Depending on desired behavior, you might throw e or just return empty list
        }
        return students;
    }
}

