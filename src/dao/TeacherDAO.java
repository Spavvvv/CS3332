package src.dao;

import src.model.person.Teacher;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Access Object for Teacher entities
 */
public class TeacherDAO {
    private final Connection connection;

    /**
     * Constructor initializes database connection
     */
    public TeacherDAO() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
    }

    /**
     * Insert a new teacher into the database
     *
     * @param teacher the teacher to insert
     * @return true if successful
     */
    public boolean insert(Teacher teacher) throws SQLException {
        // First insert into the persons table
        String personSql = "INSERT INTO persons (id, name, gender, contact_number, birthday, email, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement personStatement = connection.prepareStatement(personSql)) {
            personStatement.setString(1, teacher.getId());
            personStatement.setString(2, teacher.getName());
            personStatement.setString(3, teacher.getGender());
            personStatement.setString(4, teacher.getContactNumber());
            personStatement.setString(5, teacher.getBirthday());
            personStatement.setString(6, teacher.getEmail());
            personStatement.setString(7, teacher.getRole().toString());

            int personRowsInserted = personStatement.executeUpdate();

            if (personRowsInserted > 0) {
                // Then insert into the teachers table
                String teacherSql = "INSERT INTO teachers (person_id, teacher_id, position) VALUES (?, ?, ?)";

                try (PreparedStatement teacherStatement = connection.prepareStatement(teacherSql)) {
                    teacherStatement.setString(1, teacher.getId());
                    teacherStatement.setString(2, teacher.getTeacherId());
                    teacherStatement.setString(3, teacher.getPosition());

                    int teacherRowsInserted = teacherStatement.executeUpdate();

                    if (teacherRowsInserted > 0 && teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
                        // Insert teacher's subjects
                        return insertTeacherSubjects(teacher.getId(), teacher.getSubjects());
                    }

                    return teacherRowsInserted > 0;
                }
            }
        }

        return false;
    }

    /**
     * Update an existing teacher in the database
     *
     * @param teacher the teacher to update
     * @return true if successful
     */
    public boolean update(Teacher teacher) throws SQLException {
        // First update the persons table
        String personSql = "UPDATE persons SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ? WHERE id = ?";

        try (PreparedStatement personStatement = connection.prepareStatement(personSql)) {
            personStatement.setString(1, teacher.getName());
            personStatement.setString(2, teacher.getGender());
            personStatement.setString(3, teacher.getContactNumber());
            personStatement.setString(4, teacher.getBirthday());
            personStatement.setString(5, teacher.getEmail());
            personStatement.setString(6, teacher.getId());

            int personRowsUpdated = personStatement.executeUpdate();

            if (personRowsUpdated > 0) {
                // Then update the teachers table
                String teacherSql = "UPDATE teachers SET teacher_id = ?, position = ? WHERE person_id = ?";

                try (PreparedStatement teacherStatement = connection.prepareStatement(teacherSql)) {
                    teacherStatement.setString(1, teacher.getTeacherId());
                    teacherStatement.setString(2, teacher.getPosition());
                    teacherStatement.setString(3, teacher.getId());

                    int teacherRowsUpdated = teacherStatement.executeUpdate();

                    if (teacherRowsUpdated > 0 && teacher.getSubjects() != null) {
                        // Update teacher's subjects
                        deleteTeacherSubjects(teacher.getId()); // Delete existing subjects
                        return insertTeacherSubjects(teacher.getId(), teacher.getSubjects()); // Insert new subjects
                    }

                    return teacherRowsUpdated > 0;
                }
            }
        }

        return false;
    }

    /**
     * Delete a teacher from the database
     *
     * @param id the ID of the teacher to delete
     * @return true if successful
     */
    public boolean delete(String id) throws SQLException {
        // First delete teacher's subjects
        deleteTeacherSubjects(id);

        // Then delete from teachers table
        String teacherSql = "DELETE FROM teachers WHERE person_id = ?";
        try (PreparedStatement teacherStatement = connection.prepareStatement(teacherSql)) {
            teacherStatement.setString(1, id);
            teacherStatement.executeUpdate();
        }

        // Finally delete from persons table
        String personSql = "DELETE FROM persons WHERE id = ?";
        try (PreparedStatement personStatement = connection.prepareStatement(personSql)) {
            personStatement.setString(1, id);
            return personStatement.executeUpdate() > 0;
        }
    }

    /**
     * Get a teacher by ID
     *
     * @param id the teacher ID
     * @return the Teacher object or null if not found
     */
    public Teacher getById(String id) throws SQLException {
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects
                    loadSubjectsForTeacher(teacher);

                    return teacher;
                }
            }
        }

        return null;
    }

    /**
     * Get a teacher by teacher ID
     *
     * @param teacherId the teacher ID
     * @return the Teacher object or null if not found
     */
    public Teacher getByTeacherId(String teacherId) throws SQLException {
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE t.teacher_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects
                    loadSubjectsForTeacher(teacher);

                    return teacher;
                }
            }
        }

        return null;
    }

    /**
     * Get all teachers from the database
     *
     * @return List of all teachers
     */
    public List<Teacher> getAll() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.role = 'Teacher'";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Teacher teacher = extractTeacherFromResultSet(resultSet);

                // Load teacher's subjects
                loadSubjectsForTeacher(teacher);

                teachers.add(teacher);
            }
        }

        return teachers;
    }

    /**
     * Search teachers by name or email
     *
     * @param searchTerm the search term
     * @return List of matching teachers
     */
    public List<Teacher> searchTeachers(String searchTerm) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE p.role = 'Teacher' AND (p.name LIKE ? OR p.email LIKE ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects
                    loadSubjectsForTeacher(teacher);

                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Get teachers by subject
     *
     * @param subject the subject name
     * @return List of teachers who teach the subject
     */
    public List<Teacher> getTeachersBySubject(String subject) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT DISTINCT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "JOIN teacher_subjects ts ON t.person_id = ts.teacher_id " +
                "WHERE ts.subject = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subject);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects
                    loadSubjectsForTeacher(teacher);

                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Helper method to extract a Teacher object from a ResultSet
     */
    private Teacher extractTeacherFromResultSet(ResultSet resultSet) throws SQLException {
        return new Teacher(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("gender"),
                resultSet.getString("contact_number"),
                resultSet.getString("birthday"),
                resultSet.getString("email"),
                resultSet.getString("teacher_id"),
                resultSet.getString("position")
        );
    }

    /**
     * Helper method to load subjects for a teacher
     */
    private void loadSubjectsForTeacher(Teacher teacher) throws SQLException {
        String sql = "SELECT subject FROM teacher_subjects WHERE teacher_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacher.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    teacher.addSubject(resultSet.getString("subject"));
                }
            }
        }
    }

    /**
     * Helper method to insert teacher subjects
     */
    private boolean insertTeacherSubjects(String teacherId, List<String> subjects) throws SQLException {
        String sql = "INSERT INTO teacher_subjects (teacher_id, subject) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String subject : subjects) {
                statement.setString(1, teacherId);
                statement.setString(2, subject);
                statement.addBatch();
            }

            int[] results = statement.executeBatch();
            return Arrays.stream(results).allMatch(result -> result > 0);
        }
    }

    /**
     * Helper method to delete teacher subjects
     */
    private void deleteTeacherSubjects(String teacherId) throws SQLException {
        String sql = "DELETE FROM teacher_subjects WHERE teacher_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.executeUpdate();
        }
    }

    /**
     * Get teachers by position
     *
     * @param position the position
     * @return List of teachers with the given position
     */
    public List<Teacher> getTeachersByPosition(String position) throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        String sql = "SELECT p.*, t.teacher_id, t.position " +
                "FROM persons p " +
                "JOIN teachers t ON p.id = t.person_id " +
                "WHERE t.position = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, position);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Teacher teacher = extractTeacherFromResultSet(resultSet);

                    // Load teacher's subjects
                    loadSubjectsForTeacher(teacher);

                    teachers.add(teacher);
                }
            }
        }

        return teachers;
    }

    /**
     * Add a subject to a teacher
     *
     * @param teacherId the teacher ID
     * @param subject the subject to add
     * @return true if successful
     */
    public boolean addSubjectToTeacher(String teacherId, String subject) throws SQLException {
        String sql = "INSERT INTO teacher_subjects (teacher_id, subject) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.setString(2, subject);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Remove a subject from a teacher
     *
     * @param teacherId the teacher ID
     * @param subject the subject to remove
     * @return true if successful
     */
    public boolean removeSubjectFromTeacher(String teacherId, String subject) throws SQLException {
        String sql = "DELETE FROM teacher_subjects WHERE teacher_id = ? AND subject = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            statement.setString(2, subject);

            return statement.executeUpdate() > 0;
        }
    }


}
