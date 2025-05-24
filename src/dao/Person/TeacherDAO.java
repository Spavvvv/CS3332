package src.dao.Person;

import src.model.person.Teacher;
import src.utils.DatabaseConnection; // Still needed for the public wrapper methods
import src.model.person.Student; // Added import just in case for future dependencies or in extract method
import src.model.system.course.Course; // Added import just in case for future dependencies or in extract method


import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional; // Recommended for methods that might return null

/**
 * Data Access Object for Teacher entities.
 * Follows the pattern of using a DaoManager for dependency injection and
 * managing connections per-operation via wrapper methods.
 */
public class TeacherDAO {

    // Dependent DAOs - must be set externally by a DaoManager
    // Add dependencies here if TeacherDAO needs to call methods in other DAOs
    private CourseDAO courseDAO; // Added dependency placeholder

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     * The class-level connection is removed to manage connections per operation.
     */
    public TeacherDAO() {
        // Dependencies will be set by DaoManager
    }

    /**
     * Set CourseDAO - used for dependency injection.
     * This method must be called after TeacherDAO is instantiated to provide the dependency.
     * Add similar setters for any other DAOs TeacherDAO depends on.
     *
     * @param courseDAO The CourseDAO instance
     */
    public void setCourseDAO(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }

    /**
     * Check if CourseDAO dependency is set.
     * @throws IllegalStateException if CourseDAO dependency is not set.
     */
    private void checkCourseDAODependency() {
        if (this.courseDAO == null) {
            // Customize the message based on what operation needs the dependency
            // Example: "CourseDAO dependency has not been set on TeacherDAO. Cannot load courses for teacher."
            System.err.println("Warning: CourseDAO dependency not set on TeacherDAO. Functionality requiring it may fail.");
            // Decide whether to throw an exception or just warn. For critical dependencies, throw is better.
            // throw new IllegalStateException("CourseDAO dependency has not been set on TeacherDAO.");
        }
    }


    // --- Internal Methods (Package-private or Private) ---
    // These methods take a Connection as a parameter and perform the core SQL logic.
    // They typically throw SQLException and rely on dependencies already being set.

    /**
     * Internal method to insert a new teacher into the database using an existing connection.
     * Handles inserting into persons and teachers tables, and inserting subjects.
     *
     * @param conn   the active database connection
     * @param teacher the teacher to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalInsert(Connection conn, Teacher teacher) throws SQLException {
        // Chèn trực tiếp vào bảng `teachers`
        String sql = "INSERT INTO teachers (id, name, gender, contact_number, birthday, email) " + // Corrected: Use 'id'
                "VALUES (?, ?, ?, ?, ?, ?)"; // Corrected: 6 placeholders for 6 columns
        try (PreparedStatement personStatement = conn.prepareStatement(sql)) {
            personStatement.setString(1, teacher.getTeacherId()); // Value for 'id' (PK)
            personStatement.setString(2, teacher.getName());
            personStatement.setString(3, teacher.getGender());
            personStatement.setString(4, teacher.getContactNumber());
            personStatement.setDate(5, Date.valueOf(teacher.getBirthday()));
            personStatement.setString(6, teacher.getEmail());
            return personStatement.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to update an existing teacher in the database using an existing connection.
     * Handles updating persons and teachers tables, and updating subjects.
     *
     * @param conn   the active database connection
     * @param teacher the teacher to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalUpdate(Connection conn, Teacher teacher) throws SQLException {
        // Bước 1: Cập nhật bảng 'teachers'
        String sqlTeachers = "UPDATE teachers SET name = ?, gender = ?, contact_number = ?, birthday = ?, email = ? " +
                "WHERE id = ?"; // 'id' này là teachers.id (PK của bảng teachers)
        int teacherRowsAffected = 0;
        try (PreparedStatement psTeachers = conn.prepareStatement(sqlTeachers)) {
            psTeachers.setString(1, teacher.getName());
            psTeachers.setString(2, teacher.getGender());
            psTeachers.setString(3, teacher.getContactNumber()); // Sửa: contact_number là tham số thứ 3

            if (teacher.getBirthday() != null && !teacher.getBirthday().isEmpty()) {
                try {
                    // teacher.getBirthday() nên là chuỗi "yyyy-MM-dd"
                    psTeachers.setDate(4, java.sql.Date.valueOf(teacher.getBirthday())); // Sửa: birthday là tham số thứ 4
                } catch (IllegalArgumentException e) {
                    System.err.println("Lỗi định dạng ngày sinh khi cập nhật teachers: " + teacher.getBirthday() + " - " + e.getMessage());
                    psTeachers.setNull(4, java.sql.Types.DATE);
                }
            } else {
                psTeachers.setNull(4, java.sql.Types.DATE); // Sửa: birthday là tham số thứ 4
            }
            psTeachers.setString(5, teacher.getEmail()); // Sửa: email là tham số thứ 5
            psTeachers.setString(6, teacher.getTeacherId()); // teacher.getTeacherId() là teachers.id (PK)

            teacherRowsAffected = psTeachers.executeUpdate();
        }

        // Nếu không có user_id liên kết thì không cần cập nhật bảng users
        if (teacher.getId() == null || teacher.getId().isEmpty() || "N/A".equalsIgnoreCase(teacher.getId())) {
            System.out.println("Teacher không có user_id hợp lệ, bỏ qua cập nhật bảng users.");
            return teacherRowsAffected > 0;
        }

        // Bước 2: Cập nhật bảng 'users'
        String sqlUsers = "UPDATE users SET name = ?, gender = ?, contact_number = ?, birthday = ?, email = ? " +
                "WHERE id = ?"; // 'id' này là users.id (PK của bảng users, tương ứng teacher.getId())
        int userRowsAffected = 0;
        try (PreparedStatement psUsers = conn.prepareStatement(sqlUsers)) {
            psUsers.setString(1, teacher.getName());
            psUsers.setString(2, teacher.getGender());
            psUsers.setString(3, teacher.getContactNumber()); // Sửa: contact_number là tham số thứ 3

            if (teacher.getBirthday() != null && !teacher.getBirthday().isEmpty()) {
                try {
                    // teacher.getBirthday() nên là chuỗi "yyyy-MM-dd"
                    psUsers.setDate(4, java.sql.Date.valueOf(teacher.getBirthday())); // Sửa: birthday là tham số thứ 4
                } catch (IllegalArgumentException e) {
                    System.err.println("Lỗi định dạng ngày sinh khi cập nhật users: " + teacher.getBirthday() + " - " + e.getMessage());
                    psUsers.setNull(4, java.sql.Types.DATE);
                }
            } else {
                psUsers.setNull(4, java.sql.Types.DATE); // Sửa: birthday là tham số thứ 4
            }
            psUsers.setString(5, teacher.getEmail()); // Sửa: email là tham số thứ 5
            psUsers.setString(6, teacher.getId());    // teacher.getId() là users.id (PK)

            userRowsAffected = psUsers.executeUpdate();
        }

        if (teacherRowsAffected > 0) {
            if (userRowsAffected == 0 && !(teacher.getId() == null || teacher.getId().isEmpty() || "N/A".equalsIgnoreCase(teacher.getId()))) {
                System.out.println("Cập nhật bảng teachers thành công, nhưng không có dòng nào được cập nhật ở bảng users cho user_id: " + teacher.getId() + ". Dữ liệu có thể không thay đổi hoặc user_id không tồn tại.");
            }
            return true;
        }
        return false;
    }


    boolean internalDelete(Connection conn, String teacherIdInTeachersTable) throws SQLException {
        // Xóa từ bảng `teachers` trực tiếp dựa trên PK của nó
        String sql = "DELETE FROM teachers WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherIdInTeachersTable);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get a teacher by ID (person_id) using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     *
     * @param conn the active database connection
     * @param id the teacher ID (person_id)
     * @return the Teacher object or null if not found
     * @throws SQLException if a database access error occurs
     */
    Teacher getById(Connection conn, String id) throws SQLException {
        String sql = "SELECT id, user_id, name, gender, contact_number, birthday, email " +
                "FROM teachers WHERE user_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractTeacherFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Get a teacher by teacher ID using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @return the Teacher object or null if not found
     * @throws SQLException if a database access error occurs
     */
    Teacher getByTeacherId(Connection conn, String teacherId) throws SQLException {
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE t.teacher_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection

                    return teacher;
                }
            }
        }

        return null;
    }


    /**
     * Get all teachers from the database using an existing connection.
     * Primarily for internal use or by other DAOs with an active connection.
     *
     * @param conn the active database connection
     * @return List of all teachers
     * @throws SQLException if a database access error occurs
     */
    List<Teacher> getAll(Connection conn) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        // Thử truy vấn trực tiếp từ bảng teachers trước để kiểm tra
        String sql = "SELECT id, user_id, name, gender, contact_number, birthday, email " +
                // Thêm cột position nếu có trong bảng teachers và bạn muốn lấy nó
                // ", position " +
                "FROM teachers";
        System.out.println("[DEBUG TeacherDAO.getAll] Executing SQL: " + sql);
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                // extractTeacherFromResultSet cần phải khớp với các cột được chọn ở đây
                teachers.add(extractTeacherFromResultSet(resultSet));
            }
        }
        return teachers;
    }



    /**
     * Search teachers by name or email using an existing connection.
     *
     * @param conn the active database connection
     * @param searchTerm the search term
     * @return List of matching teachers
     * @throws SQLException if a database access error occurs
     */
    List<Teacher> internalSearchTeachers(Connection conn, String searchTerm) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT teacher_id, name, email FROM teachers " +
                "WHERE name LIKE ? OR email LIKE ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    teachers.add(extractTeacherFromResultSet(resultSet));
                }
            }
        }
        return teachers;
    }

    /**
     * Get teachers by subject using an existing connection.
     *
     * @param conn the active database connection
     * @param subject the subject name
     * @return List of teachers who teach the subject
     * @throws SQLException if a database access error occurs
     */
    List<Teacher> getTeachersBySubject(Connection conn, String subject) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT DISTINCT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "JOIN teacher_subjects ts ON t.person_id = ts.teacher_id " +
                "WHERE ts.subject = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, subject);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Get teachers by position using an existing connection.
     *
     * @param conn the active database connection
     * @param position the position
     * @return List of teachers with the given position
     * @throws SQLException if a database access error occurs
     */
    List<Teacher> getTeachersByPosition(Connection conn, String position) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE t.position = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, position);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Internal helper method to load subjects for a teacher using an existing connection.
     *
     * @param conn the active database connection
     * @param teacher the teacher object to load subjects into
     * @throws SQLException if a database access error occurs
     */



    /**
     * Internal method to add a subject to a teacher using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @param subject the subject to add
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalAddSubjectToTeacher(Connection conn, String teacherId, String subject) throws SQLException {
        String sql = "INSERT INTO teacher_subjects (teacher_id, subject) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.setString(2, subject);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to remove a subject from a teacher using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @param subject the subject to remove
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalRemoveSubjectFromTeacher(Connection conn, String teacherId, String subject) throws SQLException {
        String sql = "DELETE FROM teacher_subjects WHERE teacher_id = ? AND subject = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.setString(2, subject);

            return statement.executeUpdate() > 0;
        }
    }


    /**
     * Helper method to extract a Teacher object from a ResultSet.
     * This method does not interact with the database itself, only reads from the provided ResultSet.
     *
     * @param resultSet ResultSet containing teacher data (from persons and teachers tables)
     * @return the Teacher object
     * @throws SQLException if a database access error occurs
     */
    private Teacher extractTeacherFromResultSet(ResultSet resultSet) throws SQLException {
        String userId = resultSet.getString("user_id"); // users.id
        String teacherRecordPk = resultSet.getString("id"); // teachers.id (PK của bảng teachers)
        String name = resultSet.getString("name");
        String gender = resultSet.getString("gender");
        String contactNumber = resultSet.getString("contact_number");
        String birthday = resultSet.getString("birthday"); // Dạng String
        String email = resultSet.getString("email");
        return new Teacher(
                userId,            // Tham số 1: id (users.id) cho Person
                name,              // Tham số 2: name
                gender,            // Tham số 3: gender
                contactNumber,     // Tham số 4: contactNumber
                birthday,          // Tham số 5: birthday
                email,             // Tham số 6: email
                teacherRecordPk    // Tham số 7: teacherId (chính là teachers.id)
        );
    }

    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.
    // They delegate the core database logic to the internal methods.

    /**
     * Save a new teacher. Manages its own connection and transaction.
     *
     * @param teacher the teacher to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(Teacher teacher) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalInsert(conn, teacher);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error saving teacher: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing teacher. Manages its own connection and transaction.
     *
     * @param teacher the teacher to update
     * @return true if the update was successful, false otherwise
     */
    public boolean update(Teacher teacher) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalUpdate(conn, teacher);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error updating teacher: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a teacher. Manages its own connection and transaction.
     * Deletes related records in teacher_subjects and teachers tables before deleting from persons.
     *
     * @param id the ID of the teacher to delete (person_id)
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String id) { // Đổi tên tham số 'id' thành 'teacherRecordId' cho rõ ràng hơn
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            // Gọi internalDelete, truyền teacherRecordId (chính là teachers.id)
            boolean success = internalDelete(conn, id);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting teacher record with teachers.id: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find a teacher by ID (person_id). Manages its own connection.
     * Loads the teacher's subjects.
     *
     * @param id the teacher ID (person_id)
     * @return Optional containing the Teacher if found, empty Optional otherwise. Returns empty Optional on database error.
     */
    public Optional<Teacher> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getById(conn, id));
        } catch (SQLException e) {
            System.err.println("Error finding teacher by ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Find a teacher by teacher ID. Manages its own connection.
     * Loads the teacher's subjects.
     *
     * @param teacherId the teacher ID
     * @return Optional containing the Teacher if found, empty Optional otherwise. Returns empty Optional on database error.
     */
    public Optional<Teacher> findByTeacherId(String teacherId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getByTeacherId(conn, teacherId));
        } catch (SQLException e) {
            System.err.println("Error finding teacher by teacher ID: " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }


    /**
     * Get all teachers. Manages its own connection.
     * Loads subjects for all teachers.
     *
     * @return List of all teachers. Returns empty list on database error.
     */
    public List<Teacher> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAll(conn);
        } catch (SQLException e) {
            System.err.println("Error finding all teachers: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Search teachers by name or email. Manages its own connection.
     * Loads subjects for matching teachers.
     *
     * @param searchTerm the search term
     * @return List of matching teachers. Returns empty list on database error.
     */
    public List<Teacher> search(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalSearchTeachers(conn, searchTerm);
        } catch (SQLException e) {
            System.err.println("Error searching teachers for term: '" + searchTerm + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get teachers by subject. Manages its own connection.
     * Loads subjects for matching teachers.
     *
     * @param subject the subject name
     * @return List of teachers who teach the subject. Returns empty list on database error.
     */
    public List<Teacher> findTeachersBySubject(String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getTeachersBySubject(conn, subject);
        } catch (SQLException e) {
            System.err.println("Error finding teachers by subject: " + subject + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get teachers by position. Manages its own connection.
     * Loads subjects for matching teachers.
     *
     * @param position the position
     * @return List of teachers with the given position. Returns empty list on database error.
     */
    public List<Teacher> findTeachersByPosition(String position) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getTeachersByPosition(conn, position);
        } catch (SQLException e) {
            System.err.println("Error finding teachers by position: " + position + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Add a subject to a teacher. Manages its own connection.
     *
     * @param teacherId the teacher ID
     * @param subject the subject to add
     * @return true if successful, false otherwise (including on error)
     */
    public boolean addSubjectToTeacher(String teacherId, String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalAddSubjectToTeacher(conn, teacherId, subject);
        } catch (SQLException e) {
            System.err.println("Error adding subject " + subject + " to teacher " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove a subject from a teacher. Manages its own connection.
     *
     * @param teacherId the teacher ID
     * @param subject the subject to remove
     * @return true if successful, false otherwise (including on error)
     */
    public boolean removeSubjectFromTeacher(String teacherId, String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalRemoveSubjectFromTeacher(conn, teacherId, subject);
        } catch (SQLException e) {
            System.err.println("Error removing subject " + subject + " from teacher " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // The closeConnection method is removed as connections are managed per method call via try-with-resources.
}