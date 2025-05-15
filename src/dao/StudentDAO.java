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

/**
 * Data Access Object for Student entities
 * Modified to use explicit methods for loading related entities (Parent, Courses)
 * to avoid recursive loading issues.
 */
public class StudentDAO {

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
        String sql = "INSERT INTO students (id, name, gender, contact_number, birthday, email) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false); // Start transaction

            prepareStatementFromStudent(statement, student);

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                // Link related entities within the same transaction
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
     * Update an existing student in the database. Manages its own connection and transaction.
     * Updates basic student details and relationship links.
     *
     * @param student the student to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    public boolean updateStudent(Student student) throws SQLException {
        String sql = "UPDATE students SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
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
                // Update parent relationship by removing all and re-adding if parent exists
                removeAllParentLinks(conn, student.getId());
                if (student.getParent() != null) {
                    linkStudentToParent(conn, student.getId(), student.getParent().getId());
                }

                // Update course enrollments by removing all and re-adding current ones
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
     * This method *only* retrieves the basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return the Student object with basic details or null if not found
     * @throws SQLException if a database access error occurs
     */
    public Student getStudentById(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Extract basic student data ONLY
                    return extractStudentFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Get a student by ID. Manages its own connection.
     * This method *only* retrieves the basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param studentId the student ID
     * @return the Student object with basic details or null if not found
     * @throws SQLException if a database access error occurs
     */
    public Student getStudentById(String studentId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getStudentById(conn, studentId); // Use the helper with the new connection
        }
    }

    /**
     * Find a student by ID (returns Optional) using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     * This method *only* retrieves the basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return Optional containing the Student if found, empty Optional otherwise
     */
    public Optional<Student> findById(Connection conn, String studentId) {
        try {
            // getStudentById will now return only basic student data
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException e) {
            System.err.println("Error finding student by ID using existing connection: " + e.getMessage());
            e.printStackTrace();
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
            // getStudentById will now return only basic student data
            return Optional.ofNullable(getStudentById(conn, studentId));
        } catch (SQLException e) {
            System.err.println("Error finding student by ID: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get all students from the database. Manages its own connection.
     * This method *only* retrieves basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @return List of all students with basic details
     * @throws SQLException if a database access error occurs
     */
    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                // Extract basic student data ONLY
                students.add(extractStudentFromResultSet(resultSet));
            }
        }

        return students;
    }

    /**
     * Search students by name or email. Manages its own connection.
     * This method *only* retrieves basic student details.
     * Related entities (Parent, Courses) are NOT loaded here.
     *
     * @param searchTerm the search term
     * @return List of matching students with basic details
     * @throws SQLException if a database access error occurs
     */
    public List<Student> searchStudents(String searchTerm) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email FROM students WHERE name LIKE ? OR email LIKE ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Extract basic student data ONLY
                    students.add(extractStudentFromResultSet(resultSet));
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

    // --- Methods for Explicitly Loading Related Entities ---

    /**
     * Get the parent of a student using an existing connection.
     * This method requires the ParentDAO dependency to be set.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return Parent of the student or null if not found or no parent linked
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if ParentDAO dependency has not been set
     */
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
                    // Call the injected ParentDAO to get the parent, passing the connection
                    // IMPORTANT: Call a method in ParentDAO that *doesn't* eagerly load children
                    // Assuming ParentDAO has a getById(Connection conn, String id) that returns Parent without children
                    return this.parentDAO.getById(conn, parentId);
                }
            }
        }
        return null;
    }

    /**
     * Get the parent for a given student ID. Manages its own connection.
     * Requires the ParentDAO dependency to be set.
     *
     * @param studentId the student ID
     * @return Optional containing the Parent if found and linked, empty Optional otherwise.
     * @throws IllegalStateException if ParentDAO dependency has not been set.
     */
    public Optional<Parent> getParentForStudent(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getParentForStudent(conn, studentId));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error getting parent for student " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }


    /**
     * Get courses for a student using an existing connection.
     * This method requires the CourseDAO dependency to be set.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return List of courses the student is enrolled in. Returns empty list if none or on error.
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if CourseDAO dependency has not been set
     */
    private List<Course> getCoursesForStudent(Connection conn, String studentId) throws SQLException {
        if (this.courseDAO == null) {
            throw new IllegalStateException("CourseDAO dependency has not been set on StudentDAO. Cannot load courses.");
        }
        // Assuming CourseDAO has a method like getCoursesByStudentId(Connection conn, String studentId)
        // that retrieves courses without necessarily loading all related entities of the course.
        return this.courseDAO.getCoursesByStudentId(conn, studentId);
    }

    /**
     * Get courses for a given student ID. Manages its own connection.
     * Requires the CourseDAO dependency to be set.
     *
     * @param studentId the student ID
     * @return List of courses the student is enrolled in. Returns empty list if none or on error.
     * @throws IllegalStateException if CourseDAO dependency has not been set.
     */
    public List<Course> getCoursesForStudent(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesForStudent(conn, studentId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error getting courses for student " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Helper method to extract a Student object from a ResultSet
     * This method *only* extracts the basic student data from the current row.
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

        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setGender(gender);
        student.setContactNumber(contactNumber);
        student.setBirthday(birthday);
        student.setEmail(email);

        // Related entities are NOT loaded here.
        student.setParent(null); // Ensure parent is null initially
        student.setCurrentCourses(new ArrayList<>()); // Ensure courses list is empty initially

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
    // Note: These will now return Students with basic data only.

    /**
     * Save a new student. Manages its own connection.
     * @param student the student to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(Student student) {
        try {
            // insertStudent now handles linking parent/courses if present in the student object
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
            // updateStudent now handles updating parent/course links based on the student object
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
     * Returns students with basic data only.
     *
     * @return List of all students
     * @throws IllegalStateException if CourseDAO dependency is needed for related loading (but not for basic find all)
     */
    public List<Student> findAll() {
        try {
            return getAllStudents(); // This returns students with basic data only
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected DAOs directly
            System.err.println("Error finding all students: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Search students. Manages its own connection.
     * Returns students with basic data only.
     *
     * @param searchTerm the search term
     * @return List of matching students
     * @throws IllegalStateException if CourseDAO dependency is needed for related loading (but not for basic search)
     */
    public List<Student> search(String searchTerm) {
        try {
            return searchStudents(searchTerm); // This returns students with basic data only
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected DAOs directly
            System.err.println("Error searching students: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find all students associated with a specific session ID.
     * Manages its own connection.
     * Returns students with basic data only.
     *
     * @param sessionId The ID of the session.
     * @return A list of students associated with the session (basic data only).
     * @throws SQLException if a database access error occurs.
     */
    public List<Student> findBySessionId(String sessionId) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.gender, s.contact_number, s.birthday, s.email " +
                "FROM students s " +
                "JOIN session_student ss ON s.id = ss.student_id " + // Assuming a linking table
                "WHERE ss.session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Use the basic helper to extract student data (NO related entities loaded here)
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding students by session ID: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be handled by the caller (e.g., AttendanceDAO/Controller)
        }

        return students;
    }

    /**
     * NEW METHOD: Creates a student and a corresponding user record within a single transaction.
     * Also links to a parent if parent information is provided in the Student object.
     *
     * @param student The student object containing all necessary information.
     *                The student.getId() should be the student-specific ID (e.g., "S0001").
     * @param address The address for the user. (Can be null if not applicable)
     * @return true if both records and links are created successfully, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean createStudentAndUserTransaction(Student student, String address) throws SQLException {
        Connection conn = null;
        String generatedUserId = null;

        // SQL đã được cập nhật: Loại bỏ cột 'email'
        String userInsertSql = "INSERT INTO users (id, name, gender, contact_number, birthday, address, active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        // SQL đã được cập nhật: Loại bỏ cột 'email'
        String studentInsertSql = "INSERT INTO students (id, user_id, name, gender, contact_number, birthday, address, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // Thông tin email từ đối tượng Student không còn được sử dụng trực tiếp trong INSERT nữa
            // String receivedEmail = student.getEmail();
            // System.out.println("DEBUG [StudentDAO - Start Transaction]: Đối tượng Student có email (nếu có): [" + receivedEmail + "]");
            // if (receivedEmail == null || receivedEmail.trim().isEmpty()) {
            //     System.out.println("INFO [StudentDAO]: Email trong đối tượng Student là null hoặc rỗng. Sẽ không chèn email.");
            // }

            // 1. Thêm vào bảng 'users'
            generatedUserId = UUID.randomUUID().toString(); // Tạo ID duy nhất cho user
            try (PreparedStatement userStmt = conn.prepareStatement(userInsertSql)) {
                userStmt.setString(1, generatedUserId);
                userStmt.setString(2, student.getName());
                userStmt.setString(3, student.getGender());
                userStmt.setString(4, student.getContactNumber());
                userStmt.setString(5, student.getBirthday());
                // Cột email đã được loại bỏ, điều chỉnh chỉ số cho các cột còn lại:
                // userStmt.setString(6, student.getEmail()); // Dòng này đã bị loại bỏ

                if (address != null && !address.trim().isEmpty()) {
                    userStmt.setString(6, address); // Trước đây là 7
                } else {
                    userStmt.setNull(6, Types.VARCHAR); // Trước đây là 7
                }
                userStmt.setBoolean(7, true); // Mặc định user active, trước đây là 8

                int userRowsInserted = userStmt.executeUpdate();
                if (userRowsInserted == 0) {
                    conn.rollback();
                    System.err.println("Thất bại khi tạo bản ghi user cho sinh viên: " + student.getName());
                    return false;
                }
            }

            // 2. Thêm vào bảng 'students'
            if (student.getId() == null || student.getId().trim().isEmpty()) {
                conn.rollback();
                System.err.println("Thiếu ID sinh viên. Không thể thêm vào bảng students.");
                throw new SQLException("ID sinh viên là bắt buộc để thêm bản ghi sinh viên.");
            }
            try (PreparedStatement studentStmt = conn.prepareStatement(studentInsertSql)) {
                studentStmt.setString(1, student.getId()); // ID riêng của sinh viên
                studentStmt.setString(2, generatedUserId); // Liên kết tới user vừa tạo
                studentStmt.setString(3, student.getName());
                studentStmt.setString(4, student.getGender());
                studentStmt.setString(5, student.getContactNumber());
                studentStmt.setString(6, student.getBirthday());
                // Cột email đã được loại bỏ, điều chỉnh chỉ số cho các cột còn lại:
                // studentStmt.setString(7, student.getEmail()); // Dòng này đã bị loại bỏ

                if (address != null && !address.trim().isEmpty()) { // Giả sử sinh viên cũng có cột địa chỉ
                    studentStmt.setString(7, address); // Trước đây là 8
                } else {
                    studentStmt.setNull(7, Types.VARCHAR); // Trước đây là 8
                }
                studentStmt.setString(8, "ACTIVE"); // Trạng thái của sinh viên, trước đây là 9

                int studentRowsInserted = studentStmt.executeUpdate();
                if (studentRowsInserted == 0) {
                    conn.rollback();
                    System.err.println("Thất bại khi tạo bản ghi student cho: " + student.getName());
                    return false;
                }
            }

            // 3. Liên kết với Parent (nếu có thông tin Parent trong đối tượng Student)
            if (student.getParent() != null && student.getParent().getId() != null && !student.getParent().getId().trim().isEmpty()) {
                // Giả sử bạn có phương thức linkStudentToParent
                boolean linked = linkStudentToParent(conn, student.getId(), student.getParent().getId());
                if (!linked) {
                    conn.rollback();
                    System.err.println("Thất bại khi liên kết sinh viên " + student.getId() + " với phụ huynh " + student.getParent().getId());
                    return false;
                }
            }

            // 4. Đăng ký khóa học (nếu có, sử dụng lại logic hiện có)
            if (student.getCurrentCourses() != null && !student.getCurrentCourses().isEmpty()) {
                for (Course course : student.getCurrentCourses()) {
                    // Giả sử bạn có phương thức enrollStudentInCourse
                    if (!enrollStudentInCourse(conn, student.getId(), course.getCourseId())) {
                        conn.rollback();
                        System.err.println("Thất bại khi đăng ký sinh viên " + student.getId() + " vào khóa học " + course.getCourseId());
                        return false;
                    }
                }
            }

            conn.commit(); // Commit transaction nếu tất cả thành công
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback nếu có lỗi SQL
                } catch (SQLException ex) {
                    System.err.println("Lỗi trong quá trình rollback database: " + ex.getMessage());
                }
            }
            System.err.println("Giao dịch database thất bại khi tạo student và user: " + e.getMessage());
            e.printStackTrace();
            throw e; // Ném lại exception để lớp gọi xử lý
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đóng kết nối database: " + e.getMessage());
                }
            }
        }
    }
}
