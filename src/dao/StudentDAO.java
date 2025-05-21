
package src.dao;

import javafx.scene.control.Alert;
import src.model.attendance.HomeworkSubmissionModel;
import src.model.attendance.StudentAttendanceData;
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
  
     //* Finds all students belonging to a specific class ID.
     //* Manages its own connection.
     //* Returns students with basic data only (including their class_id).
     //*
     //* @param classId The ID of the class.
     //* @return A list of students belonging to the class. Returns an empty list if no students are found or on error.
     //*/
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

    /**
     * Lấy điểm đúng giờ và chuyên cần mới nhất cho một học sinh trong một lớp cụ thể
     * và cập nhật vào đối tượng StudentAttendanceData đã cung cấp.
     *
     * @param attendanceData Đối tượng StudentAttendanceData cần cập nhật
     * @param classId ID của lớp học
     * @return True nếu tìm thấy và cập nhật điểm, false nếu ngược lại
     */
    public boolean populateStudentMetrics(StudentAttendanceData attendanceData, String classId) {
        String sql = "SELECT punctuality_score, awareness_score FROM student_metrics " +
                "WHERE student_id = ? AND class_id = ? " +
                "ORDER BY record_date DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, attendanceData.getStudent().getId());
            statement.setString(2, classId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int punctualityScore = (int) Math.round(resultSet.getDouble("punctuality_score"));
                    int awarenessScore = (int) Math.round(resultSet.getDouble("awareness_score"));

                    attendanceData.setPunctualityRating(punctualityScore);
                    attendanceData.setDiligenceRating(awarenessScore);
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy thông tin điểm cho học sinh " +
                    attendanceData.getStudent().getId() + " trong lớp " + classId, e);
        }

        return false; // Không tìm thấy dữ liệu hoặc có lỗi xảy ra
    }

    /**
     * Cập nhật điểm cho danh sách học sinh trong một lớp cụ thể.
     *
     * @param attendanceDataList Danh sách đối tượng StudentAttendanceData cần cập nhật
     * @param classId ID của lớp học
     */
    public void populateMetricsForStudentList(List<StudentAttendanceData> attendanceDataList, String classId) {
        for (StudentAttendanceData data : attendanceDataList) {
            populateStudentMetrics(data, classId);
        }
    }

    /**
     * Lưu thông tin điểm của học sinh từ đối tượng StudentAttendanceData vào cơ sở dữ liệu.
     * Tạo bản ghi mới với ngày hiện tại.
     *
     * @param attendanceData Đối tượng StudentAttendanceData chứa thông tin điểm
     * @param classId ID của lớp học
     * @param notes Ghi chú tùy chọn về thông tin điểm
     * @return True nếu lưu thành công, false nếu ngược lại
     */
    public boolean saveStudentMetrics(StudentAttendanceData attendanceData, String classId, String notes) {
        String sql = "INSERT INTO student_metrics (metric_id, student_id, class_id, record_date, " +
                "awareness_score, punctuality_score, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            // Tạo ID duy nhất cho bản ghi metric
            String metricId = UUID.randomUUID().toString();

            statement.setString(1, metricId);
            statement.setString(2, attendanceData.getStudent().getId());
            statement.setString(3, classId);
            statement.setDate(4, java.sql.Date.valueOf(java.time.LocalDate.now()));
            statement.setDouble(5, attendanceData.getDiligenceRating()); // Awareness score tương ứng với diligence
            statement.setDouble(6, attendanceData.getPunctualityRating());
            statement.setString(7, notes != null ? notes : attendanceData.getStudentSessionNotes());

            int rowsInserted = statement.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu thông tin điểm cho học sinh " +
                    attendanceData.getStudent().getId(), e);
            return false;
        }
    }

    /**
     * Cập nhật thông tin điểm của học sinh trong cơ sở dữ liệu từ đối tượng StudentAttendanceData.
     *
     * @param metricId ID của bản ghi metric cần cập nhật
     * @param attendanceData Đối tượng StudentAttendanceData chứa thông tin điểm đã cập nhật
     * @param notes Ghi chú tùy chọn về thông tin điểm
     * @return True nếu cập nhật thành công, false nếu ngược lại
     */
    public boolean updateStudentMetrics(String metricId, StudentAttendanceData attendanceData, String notes) {
        String sql = "UPDATE student_metrics SET awareness_score = ?, punctuality_score = ?, " +
                "notes = ? WHERE metric_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setDouble(1, attendanceData.getDiligenceRating()); // Awareness score tương ứng với diligence
            statement.setDouble(2, attendanceData.getPunctualityRating());
            statement.setString(3, notes != null ? notes : attendanceData.getStudentSessionNotes());
            statement.setString(4, metricId);

            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật thông tin điểm với ID " + metricId, e);
            return false;
        }
    }

    /**
     * Lọc học sinh dựa trên điểm đúng giờ và chuyên cần tối thiểu.
     *
     * @param students Danh sách học sinh cần lọc
     * @param classId ID của lớp học để tra cứu thông tin điểm
     * @param minPunctuality Điểm đúng giờ tối thiểu (-1 để không áp dụng bộ lọc)
     * @param minDiligence Điểm chuyên cần tối thiểu (-1 để không áp dụng bộ lọc)
     * @return Danh sách đối tượng StudentAttendanceData đạt tiêu chí
     */
    public List<StudentAttendanceData> filterStudentsByMetrics(List<Student> students, String classId,
                                                               int minPunctuality, int minDiligence) {
        List<StudentAttendanceData> filteredData = new ArrayList<>();

        for (Student student : students) {
            StudentAttendanceData data = new StudentAttendanceData(student);

            // Cập nhật với thông tin điểm mới nhất
            populateStudentMetrics(data, classId);

            // Áp dụng bộ lọc
            boolean matchesPunctuality = (minPunctuality == -1) || (data.getPunctualityRating() >= minPunctuality);
            boolean matchesDiligence = (minDiligence == -1) || (data.getDiligenceRating() >= minDiligence);

            if (matchesPunctuality && matchesDiligence) {
                filteredData.add(data);
            }
        }

        return filteredData;
    }

    /**
     * Lấy điểm đúng giờ và chuyên cần trung bình của học sinh qua tất cả các lớp học.
     *
     * @param studentId ID của học sinh
     * @return Đối tượng StudentAttendanceData với điểm trung bình
     */
    public StudentAttendanceData getAverageStudentMetrics(String studentId) {
        String sql = "SELECT AVG(punctuality_score) as avg_punctuality, " +
                "AVG(awareness_score) as avg_awareness " +
                "FROM student_metrics WHERE student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Lấy học sinh bằng ID
                    return findById(studentId)
                            .map(student -> {
                                StudentAttendanceData data = new StudentAttendanceData(student);
                                try {
                                    int avgPunctuality = (int) Math.round(resultSet.getDouble("avg_punctuality"));
                                    int avgAwareness = (int) Math.round(resultSet.getDouble("avg_awareness"));

                                    data.setPunctualityRating(avgPunctuality);
                                    data.setDiligenceRating(avgAwareness);
                                } catch (SQLException e) {
                                    LOGGER.log(Level.SEVERE, "Lỗi khi đọc dữ liệu từ ResultSet", e);
                                }
                                return data;
                            })
                            .orElse(null);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy điểm trung bình cho học sinh " + studentId, e);
        }

        return null;
    }

    /**
     * Lấy lịch sử điểm của học sinh trong một lớp cụ thể.
     *
     * @param studentId ID của học sinh
     * @param classId ID của lớp học
     * @return Danh sách các đối tượng StudentAttendanceData chứa dữ liệu lịch sử
     */
    public List<StudentAttendanceData> getStudentMetricsHistory(String studentId, String classId) {
        List<StudentAttendanceData> metricsHistory = new ArrayList<>();

        // Đầu tiên kiểm tra xem học sinh có tồn tại không
        Optional<Student> studentOptional = findById(studentId);
        if (!studentOptional.isPresent()) {
            return metricsHistory; // Trả về danh sách rỗng nếu không tìm thấy học sinh
        }

        Student student = studentOptional.get();
        String sql = "SELECT student_id, record_date, awareness_score, punctuality_score, notes " +
                "FROM student_metrics " +
                "WHERE student_id = ? AND class_id = ? " +
                "ORDER BY record_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, classId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    StudentAttendanceData data = new StudentAttendanceData(student);

                    data.setPunctualityRating((int) Math.round(resultSet.getDouble("punctuality_score")));
                    data.setDiligenceRating((int) Math.round(resultSet.getDouble("awareness_score")));
                    data.setStudentSessionNotes(resultSet.getString("notes"));

                    metricsHistory.add(data);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy lịch sử điểm cho học sinh " +
                    studentId + " trong lớp " + classId, e);
        }

        return metricsHistory;
    }

    /**
     * Lấy danh sách nộp bài tập về nhà của học sinh cho một buổi học cụ thể.
     *
     * @param sessionId ID của buổi học cần lấy dữ liệu
     * @return Danh sách HomeworkSubmissionModel chứa thông tin nộp bài tập
     * @throws SQLException nếu có lỗi truy cập cơ sở dữ liệu
     */
    public List<HomeworkSubmissionModel> getAttendanceForSession(String sessionId) throws SQLException {
        List<HomeworkSubmissionModel> studentData = new ArrayList<>();

        String sql = "SELECT s.student_submission_id, s.student_id, s.homework_id, s.submitted, " +
                "s.grade, s.submission_timestamp, s.evaluator_notes, s.checked_in_session_id, " +
                "s.punctuality_rating, s.diligence_rating, s.final_score " +
                "FROM student_homework_submissions s " +
                "WHERE s.checked_in_session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, sessionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    HomeworkSubmissionModel submission = new HomeworkSubmissionModel(
                            resultSet.getString("student_submission_id"),
                            resultSet.getString("student_id"),
                            resultSet.getString("homework_id"),
                            resultSet.getBoolean("submitted"),
                            resultSet.getDouble("grade"),
                            resultSet.getTimestamp("submission_timestamp") != null ?
                                    resultSet.getTimestamp("submission_timestamp").toLocalDateTime() : null,
                            resultSet.getString("evaluator_notes"),
                            resultSet.getString("checked_in_session_id")
                    );

                    // Đặt các giá trị đánh giá bổ sung
                    Integer punctualityRating = resultSet.getObject("punctuality_rating") != null ?
                            resultSet.getInt("punctuality_rating") : null;
                    submission.setPunctualityRating(punctualityRating);

                    Integer diligenceRating = resultSet.getObject("diligence_rating") != null ?
                            resultSet.getInt("diligence_rating") : null;
                    submission.setDiligenceRating(diligenceRating);

                    Integer finalScore = resultSet.getObject("final_score") != null ?
                            resultSet.getInt("final_score") : null;
                    submission.setFinalScore(finalScore);

                    studentData.add(submission);
                }
            }
        }

        return studentData;
    }

}

