package src.dao.Person;

import src.model.person.Parent;
import src.model.person.Student;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Parent entities
 * Modified to use explicit methods for loading related entities (Children/Students)
 * to avoid recursive loading issues.
 */
public class ParentDAO {
    // Dependent DAO - must be set externally by a DaoManager
    private StudentDAO studentDAO;

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public ParentDAO() {
        // Dependencies like StudentDAO will be set by DaoManager or equivalent
    }

    /**
     * Set StudentDAO - used for dependency injection.
     * This method must be called after ParentDAO is instantiated to provide the StudentDAO dependency.
     *
     * @param studentDAO The StudentDAO instance
     */
    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    // --- Internal Methods (Package-private or Private) ---
    // These methods take a Connection as a parameter and perform the core SQL logic.
    // They typically throw SQLException.

    /**
     * Internal method to insert a new parent into the database using an existing connection.
     * This method *only* inserts the basic parent data.
     * Linking children must be handled separately, or within the transaction if children are present in the Parent object.
     * This version links children within the transaction if present in the parent object.
     *
     * @param conn   the active database connection
     * @param parent the parent to insert
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalInsert(Connection conn, Parent parent) throws SQLException {
        String sql = "INSERT INTO parents (id, name, gender, contact_number, birthday, email, relationship) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parent.getId());
            statement.setString(2, parent.getName());
            statement.setString(3, parent.getGender());
            // Corrected potentially swapped indices for birthday and contact_number based on common patterns
            statement.setString(4, parent.getContactNumber()); // Assuming contact_number is the 4th '?'
            statement.setString(5, parent.getBirthday());    // Assuming birthday is the 5th '?'
            statement.setString(6, parent.getEmail());
            statement.setString(7, parent.getRelationship());


            int rowsInserted = statement.executeUpdate();

            if (rowsInserted > 0) {
                // Link children within the same transaction if present in the parent object
                if (parent.getChildren() != null) {
                    for (Student child : parent.getChildren()) {
                        linkParentToChild(conn, parent.getId(), child.getId());
                    }
                }
            }
            return rowsInserted > 0;
        }
    }

    /**
     * Internal method to update an existing parent in the database using an existing connection.
     * Updates basic parent details and relationship links.
     *
     * @param conn   the active database connection
     * @param parent the parent to update
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalUpdate(Connection conn, Parent parent) throws SQLException {
        String sql = "UPDATE parents SET name = ?, gender = ?, contact_number = ?, " +
                "birthday = ?, email = ?, relationship = ? WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parent.getName());
            statement.setString(2, parent.getGender());
            statement.setString(3, parent.getContactNumber());
            statement.setString(4, parent.getBirthday());
            statement.setString(5, parent.getEmail());
            statement.setString(6, parent.getRelationship());
            statement.setString(7, parent.getId());

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated > 0) {
                // Update parent-child relationships - remove all and re-add current ones
                removeAllChildLinks(conn, parent.getId());
                if (parent.getChildren() != null) {
                    for (Student child : parent.getChildren()) {
                        linkParentToChild(conn, parent.getId(), child.getId());
                    }
                }
            }
            return rowsUpdated > 0;
        }
    }

    /**
     * Internal method to delete a parent from the database using an existing connection.
     * Removes related links before deleting the parent record.
     *
     * @param conn the active database connection
     * @param id   the ID of the parent to delete
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean internalDelete(Connection conn, String id) throws SQLException {
        // First, remove parent-child relationships using the shared connection
        removeAllChildLinks(conn, id);

        // Then delete the parent
        String sql = "DELETE FROM parents WHERE id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        }
    }


    /**
     * Get a parent by ID using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     * This method *only* retrieves the basic parent details.
     * Related entities (Children/Students) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param id   the parent ID
     * @return the Parent object with basic details or null if not found
     * @throws SQLException if a database access error occurs
     */
    Parent getById(Connection conn, String id) throws SQLException {
        String sql = "SELECT id, name, gender, contact_number, birthday, email, relationship FROM parents WHERE id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // Extract basic parent data ONLY
                    return extractParentFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Get all parents from the database using an existing connection.
     * Primarily for internal use or by other DAOs with an active connection.
     * This method *only* retrieves basic parent details.
     * Related entities (Children/Students) are NOT loaded here.
     *
     * @param conn the active database connection
     * @return List of all parents with basic details
     * @throws SQLException if a database access error occurs
     */
    List<Parent> getAll(Connection conn) throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email, relationship FROM parents";

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                // Extract basic parent data ONLY
                parents.add(extractParentFromResultSet(resultSet));
            }
        }
        return parents;
    }

    /**
     * Internal method to link a parent to a child in the database using an existing connection.
     *
     * @param conn     the active database connection
     * @param parentId the parent ID
     * @param childId  the child (student) ID
     * @return true if successful
     * @throws SQLException if a database access error occurs
     */
    private boolean linkParentToChild(Connection conn, String parentId, String childId) throws SQLException {
        String sql = "INSERT INTO parent_student (parent_id, student_id) VALUES (?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.setString(2, childId);

            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to remove all child links for a parent using an existing connection.
     *
     * @param conn the active database connection
     * @param parentId the parent ID
     * @throws SQLException if a database access error occurs
     */
    private void removeAllChildLinks(Connection conn, String parentId) throws SQLException {
        String sql = "DELETE FROM parent_student WHERE parent_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parentId);
            statement.executeUpdate();
        }
    }

    /**
     * Get parents for a specific student using an existing connection.
     * Use this when called from another DAO that already has a connection open.
     * This method *only* retrieves basic parent details.
     * Related entities (Children/Students of the parent) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param studentId the student ID
     * @return List of parents for the student with basic details
     * @throws SQLException if a database access error occurs
     */
    List<Parent> getParentsByStudentId(Connection conn, String studentId) throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.gender, p.contact_number, p.birthday, p.email, p.relationship " +
                "FROM parents p " +
                "JOIN parent_student ps ON p.id = ps.parent_id " +
                "WHERE ps.student_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Extract basic parent data ONLY
                    parents.add(extractParentFromResultSet(resultSet));
                }
            }
        }
        return parents;
    }

    /**
     * Internal method to search parents by name or email using an existing connection.
     * This method *only* retrieves basic parent details.
     * Related entities (Children/Students) are NOT loaded here.
     *
     * @param conn the active database connection
     * @param searchTerm the search term
     * @return List of matching parents with basic details
     * @throws SQLException if a database access error occurs
     */
    List<Parent> searchParents(Connection conn, String searchTerm) throws SQLException {
        List<Parent> parents = new ArrayList<>();
        String sql = "SELECT id, name, gender, contact_number, birthday, email, relationship FROM parents WHERE name LIKE ? OR email LIKE ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // Extract basic parent data ONLY
                    parents.add(extractParentFromResultSet(resultSet));
                }
            }
        }
        return parents;
    }

    // --- Methods for Explicitly Loading Related Entities ---

    /**
     * Helper method to load children for a parent using an existing connection.
     * Requires the StudentDAO dependency to be set.
     *
     * @param conn the active database connection
     * @param parentId the ID of the parent to load children for
     * @return List of students (children) associated with the parent. Returns empty list if none or on error.
     * @throws SQLException if a database access error occurs
     * @throws IllegalStateException if StudentDAO dependency has not been set on ParentDAO.
     */
    private List<Student> getChildrenForParent(Connection conn, String parentId) throws SQLException {
        if (this.studentDAO == null) {
            throw new IllegalStateException("StudentDAO dependency has not been set on ParentDAO. Cannot load children.");
        }
        List<Student> children = new ArrayList<>();
        String sql = "SELECT student_id FROM parent_student WHERE parent_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, parentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    // Use the injected StudentDAO to get the student, passing the same connection.
                    // IMPORTANT: Call a method in StudentDAO that *doesn't* eagerly load parent/courses.
                    // Assuming StudentDAO.getStudentById(Connection conn, String studentId) now returns Student without relations.
                    Student child = this.studentDAO.getStudentById(conn, studentId);
                    if (child != null) {
                        children.add(child);
                    }
                }
            }
        }
        return children;
    }

    /**
     * Get the children (students) for a given parent ID. Manages its own connection.
     * Requires the StudentDAO dependency to be set.
     *
     * @param parentId the parent ID
     * @return List of students (children) associated with the parent. Returns empty list if none or on error.
     * @throws IllegalStateException if StudentDAO dependency has not been set.
     */
    public List<Student> getChildrenForParent(String parentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getChildrenForParent(conn, parentId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error getting children for parent " + parentId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    /**
     * Helper method to extract a Parent object from a ResultSet
     * This method *only* extracts the basic parent data from the current row.
     * It does NOT load related entities (Children/Students).
     *
     * @param resultSet ResultSet containing parent data
     * @return the Parent object with basic details
     * @throws SQLException if a database access error occurs
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

        // Related entities are NOT loaded here.
        parent.setChildren(new ArrayList<>()); // Ensure children list is empty initially

        return parent;
    }

    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.
    // Note: These will now return Parents with basic data only.

    /**
     * Save a new parent. Manages its own connection and transaction.
     *
     * @param parent the parent to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(Parent parent) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalInsert(conn, parent);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error saving parent: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing parent. Manages its own connection and transaction.
     *
     * @param parent the parent to update
     * @return true if the update was successful, false otherwise
     */
    public boolean update(Parent parent) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, parent);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error updating parent: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a parent. Manages its own connection and transaction.
     *
     * @param id the ID of the parent to delete
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
            System.err.println("Error deleting parent with ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find a parent by ID. Manages its own connection.
     * Returns the parent with basic data only.
     *
     * @param id the parent ID
     * @return Optional containing the Parent if found, empty Optional otherwise. Returns empty Optional on database error.
     */
    public Optional<Parent> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getById(Connection, String) now returns parent with basic data only
            return Optional.ofNullable(getById(conn, id));
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected StudentDAO
            System.err.println("Error finding parent by ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get all parents. Manages its own connection.
     * Returns parents with basic data only.
     *
     * @return List of all parents. Returns empty list on database error.
     */
    public List<Parent> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAll(conn); // getAll(Connection) now returns parents with basic data only
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected StudentDAO
            System.err.println("Error finding all parents: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Search parents by name or email. Manages its own connection.
     * Returns parents with basic data only.
     *
     * @param searchTerm the search term
     * @return List of matching parents. Returns empty list on database error.
     */
    public List<Parent> search(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return searchParents(conn, searchTerm); // searchParents(Connection, String) now returns parents with basic data only
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected StudentDAO
            System.err.println("Error searching parents for term: '" + searchTerm + "': " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Get parents for a specific student. Manages its own connection.
     * Returns parents with basic data only.
     *
     * @param studentId the student ID
     * @return List of parents for the student. Returns empty list on database error.
     */
    public List<Parent> findParentsByStudentId(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // getParentsByStudentId(Connection, String) now returns parents with basic data only
            return getParentsByStudentId(conn, studentId);
        } catch (SQLException e) { // Catch only SQLException as basic retrieval doesn't need injected StudentDAO
            System.err.println("Error finding parents for student ID: " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Removed the public linkParentToChild and unlinkParentFromChild methods
    // as relationship management is primarily handled within the update/delete operations
    // or if needed, they would follow the public wrapper pattern.

}
