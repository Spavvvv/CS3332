package src.dao;

import src.model.person.Teacher;
import utils.DatabaseConnection; // Still needed for the public wrapper methods
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
        // Note: We assume the transaction is handled by the calling public method

        // First insert into the persons table
        String personSql = "INSERT INTO persons (id, name, gender, contact_number, birthday, email, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement personStatement = conn.prepareStatement(personSql)) {
            personStatement.setString(1, teacher.getId());
            personStatement.setString(2, teacher.getName());
            personStatement.setString(3, teacher.getGender());
            personStatement.setString(4, teacher.getContactNumber());
            personStatement.setString(5, teacher.getBirthday());
            personStatement.setString(6, teacher.getEmail());
            personStatement.setString(7, teacher.getRole()); // Ensure role is set to 'Teacher' in the model or here

            int personRowsInserted = personStatement.executeUpdate();

            if (personRowsInserted > 0) {
                // Then insert into the teachers table
                String teacherSql = "INSERT INTO teachers (person_id, teacher_id, position) VALUES (?, ?, ?)";

                try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
                    teacherStatement.setString(1, teacher.getId());
                    teacherStatement.setString(2, teacher.getTeacherId());
                    teacherStatement.setString(3, teacher.getPosition());

                    int teacherRowsInserted = teacherStatement.executeUpdate();

                    if (teacherRowsInserted > 0 && teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
                        // Insert teacher's subjects using the same connection
                        return internalInsertTeacherSubjects(conn, teacher.getId(), teacher.getSubjects());
                    }

                    return teacherRowsInserted > 0;
                }
            }
        }

        return false; // Return false if person insert failed
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
        // Note: We assume the transaction is handled by the calling public method

        // First update the persons table
        String personSql = "UPDATE persons SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ? WHERE id = ?";

        try (PreparedStatement personStatement = conn.prepareStatement(personSql)) {
            personStatement.setString(1, teacher.getName());
            personStatement.setString(2, teacher.getGender());
            personStatement.setString(3, teacher.getContactNumber());
            personStatement.setString(4, teacher.getBirthday()); // Corrected variable name from 'statement' to 'personStatement'
            personStatement.setString(5, teacher.getEmail());
            personStatement.setString(6, teacher.getId());

            int personRowsUpdated = personStatement.executeUpdate();

            if (personRowsUpdated > 0) {
                // Then update the teachers table
                String teacherSql = "UPDATE teachers SET teacher_id = ?, position = ? WHERE person_id = ?";

                try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
                    teacherStatement.setString(1, teacher.getTeacherId());
                    teacherStatement.setString(2, teacher.getPosition());
                    teacherStatement.setString(3, teacher.getId());

                    int teacherRowsUpdated = teacherStatement.executeUpdate();

                    // If teacher row updated and subjects exist, update subjects
                    // Note: This logic assumes subjects should always be updated if the teacher row updated.
                    // Consider if subjects should only be updated if the list is not null and has changes.
                    if (teacherRowsUpdated > 0 && teacher.getSubjects() != null) {
                        // Update teacher's subjects using the same connection
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

                    // Return true if teacher row was updated even if subjects weren't explicitly handled (e.g., subjects was null)
                    return teacherRowsUpdated > 0;
                }
            }
        }

        return false; // Return false if person update failed
    }


    /**
     * Internal method to delete a teacher from the database using an existing connection.
     * Handles deleting from teacher_subjects, teachers, and persons tables.
     *
     * @param conn the active database connection
     * @param id the ID of the teacher to delete (person_id)
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    boolean internalDelete(Connection conn, String id) throws SQLException {
        // Note: We assume the transaction is handled by the calling public method

        // First delete teacher's subjects using the same connection
        internalDeleteTeacherSubjects(conn, id);

        // Then delete from teachers table using the same connection
        String teacherSql = "DELETE FROM teachers WHERE person_id = ?";
        try (PreparedStatement teacherStatement = conn.prepareStatement(teacherSql)) {
            teacherStatement.setString(1, id);
            teacherStatement.executeUpdate();
        }

        // Finally delete from persons table using the same connection
        String personSql = "DELETE FROM persons WHERE id = ?";
        try (PreparedStatement personStatement = conn.prepareStatement(personSql)) {
            personStatement.setString(1, id);
            return personStatement.executeUpdate() > 0;
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
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.id = ?";

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
                    internalLoadSubjectsForTeacher(conn, teacher);

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
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.role = 'Teacher'";

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
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.role = 'Teacher' AND (p.name LIKE ? OR p.email LIKE ?)";

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
                    internalLoadSubjectsForTeacher(conn, teacher);

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
                    internalLoadSubjectsForTeacher(conn, teacher);

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
            // Check if all batch operations were successful (at least one row affected)
            return results.length == subjects.size() && Arrays.stream(results).allMatch(result -> result >= 0); // >= 0 for Statement.SUCCESS_NO_INFO or row count
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
     * This method does not interact with the database itself, only reads from the provided ResultSet.
     *
     * @param resultSet ResultSet containing teacher data (from persons and teachers tables)
     * @return the Teacher object
     * @throws SQLException if a database access error occurs
     */
    private Teacher extractTeacherFromResultSet(ResultSet resultSet) throws SQLException {
        return new Teacher(
                resultSet.getString("id"), // person.id
                resultSet.getString("name"), // person.name
                resultSet.getString("gender"), // person.gender
                resultSet.getString("contact_number"), // person.contact_number
                resultSet.getString("birthday"), // person.birthday
                resultSet.getString("email"), // person.email
                resultSet.getString("teacher_id"), // teachers.teacher_id
                resultSet.getString("position") // teachers.position
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
    public boolean delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalDelete(conn, id);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting teacher with ID: " + id + ": " + e.getMessage());
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
