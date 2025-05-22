package src.dao.Person;

import src.model.person.Teacher;
import src.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Teacher entities.
 * Follows the pattern of using a DaoManager for dependency injection and
 * managing connections per-operation via wrapper methods.
 */
public class TeacherDAO {

    // Dependent DAOs - must be set externally by a DaoManager
    private CourseDAO courseDAO;

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public TeacherDAO() {
        // Dependencies will be set by DaoManager
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
     * Check if CourseDAO dependency is set.
     * @throws IllegalStateException if CourseDAO dependency is not set.
     */
    private void checkCourseDAODependency() {
        if (this.courseDAO == null) {
            System.err.println("Warning: CourseDAO dependency not set on TeacherDAO. Functionality requiring it may fail.");
        }
    }

    // --- Internal Methods (Package-private or Private) ---

    /**
     * Internal method to insert a new teacher into the database using an existing connection.
     *
     * @param conn   the active database connection
     * @param teacher the teacher to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalInsert(Connection conn, Teacher teacher) throws SQLException {
        // Insert into the teachers table
        String teacherSql = "INSERT INTO teachers (id, user_id, name, gender, contact_number, birthday, email, address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
            teacherStatement.setString(1, teacher.getId());
            teacherStatement.setString(2, teacher.getUserId());
            teacherStatement.setString(3, teacher.getName());
            teacherStatement.setString(4, teacher.getGender());
            teacherStatement.setString(5, teacher.getContactNumber());
            teacherStatement.setString(6, teacher.getBirthday());
            teacherStatement.setString(7, teacher.getEmail());
            teacherStatement.setString(8, teacher.getAddress());

            int teacherRowsInserted = teacherStatement.executeUpdate();

            if (teacherRowsInserted > 0 && teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
                // Insert teacher's subjects using the same connection
                return internalInsertTeacherSubjects(conn, teacher.getId(), teacher.getSubjects());
            }

            return teacherRowsInserted > 0;
        }
    }

    /**
     * Internal method to update an existing teacher in the database using an existing connection.
     *
     * @param conn   the active database connection
     * @param teacher the teacher to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalUpdate(Connection conn, Teacher teacher) throws SQLException {
        // Update the teachers table
        String teacherSql = "UPDATE teachers SET user_id = ?, name = ?, gender = ?, " +
                "contact_number = ?, birthday = ?, email = ?, address = ? WHERE id = ?";

        try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
            teacherStatement.setString(1, teacher.getUserId());
            teacherStatement.setString(2, teacher.getName());
            teacherStatement.setString(3, teacher.getGender());
            teacherStatement.setString(4, teacher.getContactNumber());
            teacherStatement.setString(5, teacher.getBirthday());
            teacherStatement.setString(6, teacher.getEmail());
            teacherStatement.setString(7, teacher.getAddress());
            teacherStatement.setString(8, teacher.getId());

            int teacherRowsUpdated = teacherStatement.executeUpdate();

            // If teacher row updated and subjects exist, update subjects
            if (teacherRowsUpdated > 0 && teacher.getSubjects() != null) {
                internalDeleteTeacherSubjects(conn, teacher.getId()); // Delete existing subjects
                // If subjects list is not empty, insert new subjects
                if (!teacher.getSubjects().isEmpty()) {
                    return internalInsertTeacherSubjects(conn, teacher.getId(), teacher.getSubjects());
                } else {
                    // Teacher row updated, and subjects were cleared (empty list provided)
                    return true;
                }
            } else if (teacherRowsUpdated > 0 && teacher.getSubjects() == null) {
                // Teacher row updated, but no subjects provided in the update, subjects remain as they are.
                return true;
            }

            return teacherRowsUpdated > 0;
        }
    }

    /**
     * Internal method to delete a teacher from the database using an existing connection.
     *
     * @param conn the active database connection
     * @param id the ID of the teacher to delete
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalDelete(Connection conn, String id) throws SQLException {
        // First delete teacher's subjects using the same connection
        internalDeleteTeacherSubjects(conn, id);

        // Then delete from teachers table using the same connection
        String teacherSql = "DELETE FROM teachers WHERE id = ?";
        try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
            teacherStatement.setString(1, id);
            return teacherStatement.executeUpdate() > 0;
        }
    }

    /**
     * Get a teacher by ID using an existing connection.
     *
     * @param conn the active database connection
     * @param id the teacher ID
     * @return the Teacher object or null if not found
     * @throws SQLException if a database access error occurs
     */
    Teacher getById(Connection conn, String id) throws SQLException {
        String sql = "SELECT * FROM teachers WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    internalLoadSubjectsForTeacher(conn, teacher);

                    return teacher;
                }
            }
        }

        return null;
    }

    /**
     * Get a teacher by user ID using an existing connection.
     *
     * @param conn the active database connection
     * @param userId the user ID
     * @return the Teacher object or null if not found
     * @throws SQLException if a database access error occurs
     */
    Teacher getByUserId(Connection conn, String userId) throws SQLException {
        String sql = "SELECT * FROM teachers WHERE user_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    internalLoadSubjectsForTeacher(conn, teacher);

                    return teacher;
                }
            }
        }

        return null;
    }

    /**
     * Get all teachers from the database using an existing connection.
     *
     * @param conn the active database connection
     * @return List of all teachers
     * @throws SQLException if a database access error occurs
     */
    List<Teacher> getAll(Connection conn) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT * FROM teachers";

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Teacher teacher = extractTeacherFromResultSet(resultSet);

                // Load teacher's subjects using the same connection
                internalLoadSubjectsForTeacher(conn, teacher);

                teachers.add(teacher);
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
        String sql = "SELECT * FROM teachers WHERE name LIKE ? OR email LIKE ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    internalLoadSubjectsForTeacher(conn, teacher);

                    teachers.add(teacher);
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
        String sql = "SELECT DISTINCT t.* " +
                "FROM teachers t " +
                "JOIN teacher_subjects ts ON t.id = ts.teacher_id " +
                "WHERE ts.subject = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, subject);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    internalLoadSubjectsForTeacher(conn, teacher);

                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Get teachers for a specific class using an existing connection.
     *
     * @param conn the active database connection
     * @param classId the class ID
     * @return the Teacher object or null if not found
     * @throws SQLException if a database access error occurs
     */
    Teacher getTeacherByClassId(Connection conn, String classId) throws SQLException {
        String sql = "SELECT t.* FROM teachers t " +
                "JOIN classes c ON t.id = c.teacher_id " +
                "WHERE c.class_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, classId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects using the same connection
                    internalLoadSubjectsForTeacher(conn, teacher);

                    return teacher;
                }
            }
        }

        return null;
    }

    /**
     * Internal helper method to load subjects for a teacher using an existing connection.
     *
     * @param conn the active database connection
     * @param teacher the teacher object to load subjects into
     * @throws SQLException if a database access error occurs
     */
    private void internalLoadSubjectsForTeacher(Connection conn, Teacher teacher) throws SQLException {
        String sql = "SELECT subject FROM teacher_subjects WHERE teacher_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacher.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    teacher.addSubject(resultSet.getString("subject"));
                }
            }
        }
    }

    /**
     * Internal helper method to insert teacher subjects using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @param subjects the list of subjects to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalInsertTeacherSubjects(Connection conn, String teacherId, List<String> subjects) throws SQLException {
        String sql = "INSERT INTO teacher_subjects (teacher_id, subject) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (String subject : subjects) {
                statement.setString(1, teacherId);
                statement.setString(2, subject);
                statement.addBatch();
            }

            int[] results = statement.executeBatch();
            return results.length == subjects.size() && Arrays.stream(results).allMatch(result -> result >= 0);
        }
    }

    /**
     * Internal helper method to delete teacher subjects using an existing connection.
     *
     * @param conn the active database connection
     * @param teacherId the teacher ID
     * @throws SQLException if a database access error occurs
     */
    private void internalDeleteTeacherSubjects(Connection conn, String teacherId) throws SQLException {
        String sql = "DELETE FROM teacher_subjects WHERE teacher_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.executeUpdate();
        }
    }

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
     *
     * @param resultSet ResultSet containing teacher data from teachers table
     * @return the Teacher object
     * @throws SQLException if a database access error occurs
     */
    private Teacher extractTeacherFromResultSet(ResultSet resultSet) throws SQLException {

        String dbId = resultSet.getString("id");
        String dbUserId = resultSet.getString("user_id");

        Teacher teacher = new Teacher(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("gender"),
                resultSet.getString("contact_number"),
                resultSet.getString("birthday"),
                resultSet.getString("email"),
                resultSet.getString("user_id"),
                resultSet.getString("address")
        );

        return teacher;
    }

    // --- Public Wrapper Methods ---

    /**
     * Save a new teacher. Manages its own connection and transaction.
     *
     * @param teacher the teacher to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(Teacher teacher) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalInsert(conn, teacher);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
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
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, teacher);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
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
     *
     * @param id the ID of the teacher to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, id);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting teacher with ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find a teacher by ID. Manages its own connection.
     *
     * @param id the teacher ID
     * @return Optional containing the Teacher if found, empty Optional otherwise
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
     * Find a teacher by user ID. Manages its own connection.
     *
     * @param userId the user ID
     * @return Optional containing the Teacher if found, empty Optional otherwise
     */
    public Optional<Teacher> findByUserId(String userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getByUserId(conn, userId));
        } catch (SQLException e) {
            System.err.println("Error finding teacher by user ID: " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get all teachers. Manages its own connection.
     *
     * @return List of all teachers
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
     *
     * @param searchTerm the search term
     * @return List of matching teachers
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
     *
     * @param subject the subject name
     * @return List of teachers who teach the subject
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
     * Find teacher for a specific class. Manages its own connection.
     *
     * @param classId the class ID
     * @return Optional containing the Teacher if found, empty Optional otherwise
     */
    public Optional<Teacher> findTeacherByClassId(String classId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getTeacherByClassId(conn, classId));
        } catch (SQLException e) {
            System.err.println("Error finding teacher for class ID: " + classId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Add a subject to a teacher. Manages its own connection.
     *
     * @param teacherId the teacher ID
     * @param subject the subject to add
     * @return true if successful, false otherwise
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
     * @return true if successful, false otherwise
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
}