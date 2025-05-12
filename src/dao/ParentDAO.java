package src.dao;

import src.model.person.Parent;
import src.model.person.Student;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Parent entities
 */
public class ParentDAO {
    private final Connection connection;
    private final StudentDAO studentDAO;

    /**
     * Constructor initializes database connection and dependent DAOs
     */
    public ParentDAO() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
        this.studentDAO = new StudentDAO();
    }

    /**
     * Insert a new parent into the database
     *
     * @param parent the parent to insert
     * @return true if successful
     */
    public boolean insert(Parent parent) throws SQLException {
        String sql = "INSERT INTO parents (id, name, gender, contact_number, birthday, email, relationship) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parent.getId());
            statement.setString(2, parent.getName());
            statement.setString(3, parent.getGender());
            statement.setString(4, parent.getContactNumber());
            statement.setString(5, parent.getBirthday());
            statement.setString(6, parent.getEmail());
            statement.setString(7, parent.getRelationship());

            int rowsInserted = statement.executeUpdate();

            // If insertion successful and parent has children, update parent-child relationships
            if (rowsInserted > 0 && parent.getChildren() != null) {
                for (Student child : parent.getChildren()) {
                    linkParentToChild(parent.getId(), child.getId());
                }
            }

            return rowsInserted > 0;
        }
    }

    /**
     * Update an existing parent in the database
     *
     * @param parent the parent to update
     * @return true if successful
     */
    public boolean update(Parent parent) throws SQLException {
        String sql = "UPDATE parents SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ?, relationship = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parent.getName());
            statement.setString(2, parent.getGender());
            statement.setString(3, parent.getContactNumber());
            statement.setString(4, parent.getBirthday());
            statement.setString(5, parent.getEmail());
            statement.setString(6, parent.getRelationship());
            statement.setString(7, parent.getId());

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Delete a parent from the database
     *
     * @param id the ID of the parent to delete
     * @return true if successful
     */
    public boolean delete(String id) throws SQLException {
        // First, remove parent-child relationships
        String unlinkSql = "DELETE FROM parent_student WHERE parent_id = ?";
        try (PreparedStatement unlinkStatement = connection.prepareStatement(unlinkSql)) {
            unlinkStatement.setString(1, id);
            unlinkStatement.executeUpdate();
        }

        // Then delete the parent
        String sql = "DELETE FROM parents WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get a parent by ID
     *
     * @param id the parent ID
     * @return the Parent object or null if not found
     */
    public Parent getById(String id) throws SQLException {
        String sql = "SELECT * FROM parents WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Parent parent = extractParentFromResultSet(resultSet);
                    // Load children
                    loadChildrenForParent(parent);
                    return parent;
                }
            }
        }

        return null;
    }

    /**
     * Get all parents from the database
     *
     * @return List of all parents
     */
    public List<Parent> getAll() throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT * FROM parents";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Parent parent = extractParentFromResultSet(resultSet);
                // Load children for each parent
                loadChildrenForParent(parent);
                parents.add(parent);
            }
        }

        return parents;
    }

    /**
     * Link a parent to a child in the database
     *
     * @param parentId the parent ID
     * @param childId the child (student) ID
     * @return true if successful
     */
    public boolean linkParentToChild(String parentId, String childId) throws SQLException {
        String sql = "INSERT INTO parent_student (parent_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, childId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Unlink a parent from a child in the database
     *
     * @param parentId the parent ID
     * @param childId the child (student) ID
     * @return true if successful
     */
    public boolean unlinkParentFromChild(String parentId, String childId) throws SQLException {
        String sql = "DELETE FROM parent_student WHERE parent_id = ? AND student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, childId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Get parents for a specific student
     *
     * @param studentId the student ID
     * @return List of parents for the student
     */
    public List<Parent> getParentsByStudentId(String studentId) throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT p.* FROM parents p " +
                "JOIN parent_student ps ON p.id = ps.parent_id " +
                "WHERE ps.student_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Parent parent = extractParentFromResultSet(resultSet);
                    // Load all children for this parent
                    loadChildrenForParent(parent);
                    parents.add(parent);
                }
            }
        }

        return parents;
    }

    /**
     * Helper method to extract a Parent object from a ResultSet
     */
    private Parent extractParentFromResultSet(ResultSet resultSet) throws SQLException {
        Parent parent = new Parent();
        parent.setId(resultSet.getString("id"));
        parent.setName(resultSet.getString("name"));
        parent.setGender(resultSet.getString("gender"));
        parent.setContactNumber(resultSet.getString("contact_number"));
        parent.setBirthday(resultSet.getString("birthday"));
        parent.setEmail(resultSet.getString("email"));
        parent.setRelationship(resultSet.getString("relationship"));
        return parent;
    }

    /**
     * Helper method to load children for a parent
     */
    private void loadChildrenForParent(Parent parent) throws SQLException {
        String sql = "SELECT student_id FROM parent_student WHERE parent_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, parent.getId());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    Student student = studentDAO.getStudentById(studentId);
                    if (student != null) {
                        parent.addChild(student);
                    }
                }
            }
        }
    }

    /**
     * Search parents by name or email
     *
     * @param searchTerm the search term
     * @return List of matching parents
     */
    public List<Parent> searchParents(String searchTerm) throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT * FROM parents WHERE name LIKE ? OR email LIKE ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Parent parent = extractParentFromResultSet(resultSet);
                    loadChildrenForParent(parent);
                    parents.add(parent);
                }
            }
        }

        return parents;
    }
}
