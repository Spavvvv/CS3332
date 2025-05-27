
package src.dao.Person;

import src.model.attendance.HomeworkSubmissionModel;
import src.model.attendance.StudentAttendanceData;
import src.model.person.Student;
import src.model.system.course.Course;


import src.utils.DatabaseConnection;

import java.time.LocalDate;
import java.util.UUID;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Data Access Object for Student entities
 * Modified to use explicit methods for loading related entities (Parent, Courses)
 * to avoid recursive loading issues.
 */
public class StudentDAO {
    private static final Logger LOGGER = Logger.getLogger(StudentDAO.class.getName());

    private CourseDAO courseDAO; // This will be injected

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public StudentDAO() {
        // Dependencies ParentDAO and CourseDAO will be set by DaoManager or equivalent
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
        String sql = "INSERT INTO students (id, user_id, name, gender, contact_number, birthday, email, " +
                "address, status, Parent_Name, Parent_PhoneNumber) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
    public boolean updateStudentAndUser(Student student) throws SQLException {
        String updateStudentSql = "UPDATE students SET name = ?, contact_number = ?, birthday = ?, Parent_Name = ?, Parent_PhoneNumber = ? WHERE id = ?";        String updateUserSql = "UPDATE users SET name = ?, contact_number = ?, birthday = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // Update the student table
            try (PreparedStatement studentStmt = conn.prepareStatement(updateStudentSql)) {
                studentStmt.setString(1, student.getName());
                studentStmt.setString(2, student.getContactNumber());
                studentStmt.setString(3, student.getBirthday());
                studentStmt.setString(4, student.getParentName());
                studentStmt.setString(5, student.getParentPhoneNumber());
                studentStmt.setString(6, student.getId());
                studentStmt.executeUpdate(); // No need for rollback handling here
            }

            // Update the user table
            try (PreparedStatement userStmt = conn.prepareStatement(updateUserSql)) {
                userStmt.setString(1, student.getName());
                userStmt.setString(2, student.getContactNumber());
                userStmt.setString(3, student.getBirthday());
                userStmt.setString(4, student.getUserId()); // Guaranteed user_id
                userStmt.executeUpdate(); // No need for rollback handling here
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // Rollback transaction if something breaks
            throw e; // Rethrow for upstream handling
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
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
        String getUserIdSql = "SELECT user_id FROM students WHERE id = ?";
        String deleteEnrollmentsSql = "DELETE FROM enrollment WHERE student_id = ?"; // New SQL to delete enrollments
        String deleteStudentSql = "DELETE FROM students WHERE id = ?";
        String deleteUserSql = "DELETE FROM users WHERE id = ?";

        Connection conn = null;
        PreparedStatement getUserIdStmt = null;
        PreparedStatement deleteEnrollmentsStmt = null; // PreparedStatement for deleting enrollments
        PreparedStatement deleteStudentStmt = null;
        PreparedStatement deleteUserStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu giao dịch

            // Bước 1: Lấy user_id từ bảng students trước
            getUserIdStmt = conn.prepareStatement(getUserIdSql);
            getUserIdStmt.setString(1, studentId);
            rs = getUserIdStmt.executeQuery();

            // Kiểm tra xem sinh viên có tồn tại không
            if (!rs.next()) {
                System.err.println("Không tìm thấy sinh viên với ID: " + studentId);
                conn.rollback();
                return false;
            }

            // Lưu user_id để dùng sau
            String userId = rs.getString("user_id");

            // Kiểm tra xem user_id có hợp lệ không
            if (userId == null) {
                System.err.println("Sinh viên có ID " + studentId + " không có user_id hợp lệ");
                conn.rollback();
                return false;
            }

            // Bước 2: Xóa các bản ghi liên quan trong bảng 'enrollment'
            deleteEnrollmentsStmt = conn.prepareStatement(deleteEnrollmentsSql);
            deleteEnrollmentsStmt.setString(1, studentId);
            deleteEnrollmentsStmt.executeUpdate(); // Không cần kiểm tra số hàng bị xóa ở đây, có thể không có enrollment nào

            // Bước 3: Xóa bản ghi student
            deleteStudentStmt = conn.prepareStatement(deleteStudentSql);
            deleteStudentStmt.setString(1, studentId);
            int studentRowsDeleted = deleteStudentStmt.executeUpdate();

            if (studentRowsDeleted == 0) {
                System.err.println("Không thể xóa sinh viên với ID: " + studentId + " (có thể đã bị xóa hoặc không tồn tại sau khi xóa enrollment).");
                conn.rollback(); // Rollback nếu không xóa được student (ví dụ: student không tồn tại)
                return false;
            }

            // Bước 4: Xóa bản ghi user sau khi đã xóa student thành công
            deleteUserStmt = conn.prepareStatement(deleteUserSql);
            deleteUserStmt.setString(1, userId);
            int userRowsDeleted = deleteUserStmt.executeUpdate();

            if (userRowsDeleted == 0) {
                System.err.println("Không thể xóa người dùng với ID: " + userId + " (có thể đã bị xóa hoặc user_id không đúng).");
                // Quyết định rollback hay không ở đây tùy thuộc vào logic nghiệp vụ.
                // Nếu việc student bị xóa mà user không bị xóa là chấp nhận được thì có thể commit.
                // Tuy nhiên, để nhất quán, nên rollback.
                conn.rollback();
                return false;
            }

            // Xác nhận giao dịch nếu tất cả các bước đều thành công
            conn.commit();
            System.out.println("Đã xóa thành công sinh viên (ID: " + studentId + "), các enrollment liên quan, và người dùng liên kết (ID: " + userId + ")");
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Hoàn tác giao dịch nếu có lỗi
                } catch (SQLException ex) {
                    System.err.println("Lỗi khi hoàn tác giao dịch: " + ex.getMessage());
                }
            }
            System.err.println("Lỗi khi xóa sinh viên và người dùng: " + e.getMessage());
            throw e;
        } finally {
            // Đóng tất cả các tài nguyên
            try {
                if (rs != null) rs.close();
                if (getUserIdStmt != null) getUserIdStmt.close();
                if (deleteEnrollmentsStmt != null) deleteEnrollmentsStmt.close(); // Close the new PreparedStatement
                if (deleteStudentStmt != null) deleteStudentStmt.close();
                if (deleteUserStmt != null) deleteUserStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
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
        String sql = "SELECT s.id AS s_id, s.name AS s_name, s.gender AS s_gender, " +
                "s.contact_number AS s_contact_number, s.birthday AS s_birthday, s.email AS s_email, " +
                "s.Parent_Name AS s_parent_name, s.Parent_PhoneNumber AS s_parent_phone_number " +
                // Thêm các cột khác từ bảng students với alias "s_" nếu extractStudentFromResultSet cần
                "FROM students s WHERE s.id = ?";
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
    public Student getStudentById(String studentId) throws SQLException { // Public wrapper
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getStudentById(conn, studentId);
        }
    }

    public List<Student> getStudentsByIds(Connection conn, List<String> studentIds) throws SQLException {
        List<Student> students = new ArrayList<>();
        if (studentIds == null || studentIds.isEmpty()) {
            return students;
        }

        String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT s.id AS s_id, s.name AS s_name, s.gender AS s_gender, " +
                "s.contact_number AS s_contact_number, s.birthday AS s_birthday, s.email AS s_email, " +
                "s.Parent_Name AS s_parent_name, s.Parent_PhoneNumber AS s_parent_phone_number " +
                // Thêm các cột khác từ bảng students với alias "s_" nếu extractStudentFromResultSet cần
                "FROM students s WHERE s.id IN (" + placeholders + ")";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) {
                statement.setString(i + 1, studentIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        }
        return students;
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
        String sql = "SELECT s.id AS s_id, s.name AS s_name, s.gender AS s_gender, " +
                "s.contact_number AS s_contact_number, s.birthday AS s_birthday, s.email AS s_email, " +
                "s.Parent_Name AS s_parent_name, s.Parent_PhoneNumber AS s_parent_phone_number " +
                // Thêm các cột khác từ bảng students với alias "s_" nếu extractStudentFromResultSet cần
                "FROM students s";

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
        String sql = "SELECT s.id AS s_id, s.name AS s_name, s.gender AS s_gender, " +
                "s.contact_number AS s_contact_number, s.birthday AS s_birthday, s.email AS s_email, " +
                "s.Parent_Name AS s_parent_name, s.Parent_PhoneNumber AS s_parent_phone_number " +
                "FROM students s WHERE s.name LIKE ? OR s.email LIKE ?";
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
    /**
     * Enroll a student in a course using an existing connection.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @param courseId  the course ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    // Phương thức enrollStudentInCourse (để thêm vào bảng 'enrollment')
    private boolean enrollStudentInCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM enrollment WHERE student_id = ? AND course_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, studentId);
            checkStmt.setString(2, courseId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    LOGGER.info("Student " + studentId + " is already enrolled in course " + courseId);
                    return true;
                }
            }
        }

        String sql = "INSERT INTO enrollment (enrollment_id, student_id, course_id, enrollment_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, studentId);
            statement.setString(3, courseId);
            statement.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
            statement.setString(5, "ENROLLED");
            return statement.executeUpdate() > 0;
        }
    }

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
    private boolean withdrawStudentFromCourse(Connection conn, String studentId, String courseId) {
        System.out.println("DEBUG: withdrawStudentFromCourse is currently not active. Skipping interaction.");
        return true; // Return true nhưng không thực hiện gì liên quan tới DB
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
        Student student = new Student();
        student.setId(resultSet.getString("s_id"));
        student.setName(resultSet.getString("s_name"));
        student.setGender(resultSet.getString("s_gender"));
        student.setContactNumber(resultSet.getString("s_contact_number"));
        student.setBirthday(resultSet.getString("s_birthday")); // Student model cần setter này
        student.setEmail(resultSet.getString("s_email"));
        // student.setCourseId(...); // Không còn cột này trong bảng students
        student.setParentName(resultSet.getString("s_parent_name"));
        student.setParentPhoneNumber(resultSet.getString("s_parent_phone_number"));

        // Kiểm tra và lấy các cột tùy chọn khác nếu chúng tồn tại trong ResultSet
        // (Cần một helper method 'hasColumn' hoặc kiểm tra metadata nếu các query khác nhau)
        // Ví dụ (giả sử bạn có hàm hasColumn):
        // if (hasColumn(resultSet, "s_user_id")) student.setUserId(resultSet.getString("s_user_id"));
        // if (hasColumn(resultSet, "s_address")) student.setAddress(resultSet.getString("s_address"));
        // if (hasColumn(resultSet, "s_status")) student.setStatus(resultSet.getString("s_status"));
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
        statement.setString(7, student.getStatus());
        statement.setString(8, student.getParentName());
        statement.setString(9, student.getParentPhoneNumber());
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
            // updateStudent now handles updating parent/course links based on the student object
            return updateStudentAndUser(student);
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

    /**
     * Finds all students ENROLLED in a specific course by querying the enrollment table.
     * Manages its own connection.
     * Cập nhật: Không còn lấy students.course_id nữa.
     *
     * @param courseIdToFilter The ID of the course to find enrolled students for.
     * @return A list of students enrolled in the specified course.
     */
    public List<Student> findByCourseId(String courseIdToFilter) {
        List<Student> students = new ArrayList<>();
        if (courseIdToFilter == null || courseIdToFilter.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "findByCourseId (StudentDAO) called with null or empty courseIdToFilter.");
            return students;
        }

        String sql = "SELECT s.id AS s_id, s.name AS s_name, s.gender AS s_gender, " +
                "s.contact_number AS s_contact_number, s.birthday AS s_birthday, s.email AS s_email, " +
                "s.Parent_Name AS s_parent_name, s.Parent_PhoneNumber AS s_parent_phone_number " +
                // Thêm các cột khác từ bảng students với alias "s_" nếu extractStudentFromResultSet cần
                // Ví dụ: ", s.user_id AS s_user_id, s.address AS s_address, s.status AS s_status "
                "FROM students s " +
                "JOIN enrollment e ON s.id = e.student_id " +
                "WHERE e.course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setString(1, courseIdToFilter);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(extractStudentFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding students by course ID (via enrollment): " + courseIdToFilter, e);
        }
        return students;
    }

    public boolean createStudentAndUserTransaction(Student student, String address) throws SQLException {
        Connection conn = null;
        String generatedUserId = null; // ID cho bảng users

        // SQL cho bảng users: Loại bỏ 'email', 'address' được sử dụng.
        String userInsertSql = "INSERT INTO users (id, name, gender, contact_number, birthday, address, active) VALUES (?, ?, ?, ?, ?, ?, ?)";

        // SQL cho bảng students:
        // Loại bỏ 'email'.
        // Thêm: address, status, parent_one_name, parent_one_contact, parent_two_name, parent_two_contact.
        // user_id là FK trỏ tới bảng users.
        // id là PK của bảng students (ví dụ S001).
        String studentInsertSql = "INSERT INTO students (id, user_id, name, gender, contact_number, birthday, " +
                "address, status, Parent_Name, Parent_PhoneNumber) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Thêm vào bảng 'users'
            generatedUserId = UUID.randomUUID().toString(); // Tạo ID duy nhất cho user
            try (PreparedStatement userStmt = conn.prepareStatement(userInsertSql)) {
                userStmt.setString(1, generatedUserId);
                userStmt.setString(2, student.getName());
                userStmt.setString(3, student.getGender());
                userStmt.setString(4, student.getContactNumber());
                userStmt.setString(5, student.getBirthday());

                // student.getEmail() không còn được sử dụng.
                // Sử dụng tham số 'address' cho cột address của user
                if (address != null && !address.trim().isEmpty()) {
                    userStmt.setString(6, address);
                } else {
                    userStmt.setNull(6, Types.VARCHAR);
                }
                userStmt.setBoolean(7, true); // Mặc định user active

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
                studentStmt.setString(1, student.getId());         // PK của student
                studentStmt.setString(2, generatedUserId);         // FK user_id
                studentStmt.setString(3, student.getName());
                studentStmt.setString(4, student.getGender());
                studentStmt.setString(5, student.getContactNumber());
                studentStmt.setString(6, student.getBirthday());

                // student.getEmail() không còn được sử dụng.
                // Sử dụng tham số 'address' cho cột address của student
                if (address != null && !address.trim().isEmpty()) {
                    studentStmt.setString(7, address);
                } else {
                    studentStmt.setNull(7, Types.VARCHAR);
                }

                // Sử dụng student.getStatus() hoặc một giá trị mặc định nếu null
                studentStmt.setString(8, student.getStatus() != null ? student.getStatus() : "ACTIVE"); // status

                // Thêm thông tin cha mẹ trực tiếp từ đối tượng Student
                studentStmt.setString(9, student.getParentName());
                studentStmt.setString(10, student.getParentPhoneNumber());



                int studentRowsInserted = studentStmt.executeUpdate();
                if (studentRowsInserted == 0) {
                    conn.rollback();
                    System.err.println("Thất bại khi tạo bản ghi student cho: " + student.getName());
                    return false;
                }
            }

            // 3. Liên kết với Parent (PHẦN NÀY ĐÃ BỊ LOẠI BỎ)
            // Logic "if (student.getParent() != null ... linkStudentToParent)" đã không còn cần thiết
            // vì thông tin cha mẹ đã được lưu trực tiếp vào bảng students.

            // 4. Đăng ký khóa học (nếu có, sử dụng lại logic hiện có)
            if (student.getCurrentCourses() != null && !student.getCurrentCourses().isEmpty()) {
                for (Course course : student.getCurrentCourses()) {
                    // Giả sử bạn có phương thức enrollStudentInCourse(Connection conn, String studentId, String courseId)
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
                "WHERE student_id = ? AND course_id = ? " +
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
        String sql = "INSERT INTO student_metrics (metric_id, student_id, course_id, record_date, " +
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
                "WHERE student_id = ? AND course_id = ? " +
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

}

