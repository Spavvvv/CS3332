package src.dao;

import src.model.person.Person;
import src.model.person.Parent;
import src.model.person.Student;
import src.model.person.Teacher;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the Account entity
 * Handles database operations for user accounts
 */
public class AccountDAO {
    private final DatabaseConnection dbConnector;

    /**
     * Constructor with dependency injection
     */
    public AccountDAO() throws SQLException {
        dbConnector = new DatabaseConnection();
    }

    /**
     * Save a new account to the database
     *
     * @param username Username for the account
     * @param password Password for the account
     * @param personId ID of the associated person
     * @param role Role of the user (TEACHER, STUDENT, PARENT)
     * @return true if successful, false otherwise
     */
    public boolean save(String username, String password, String personId, String role) {
        String sql = "INSERT INTO accounts (username, password, person_id, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Should be hashed in production
            stmt.setString(3, personId);
            stmt.setString(4, role);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error saving account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing account
     *
     * @param id Account ID
     * @param username New username
     * @param password New password
     * @param role New role
     * @return true if successful, false otherwise
     */
    public boolean update(String id, String username, String password, String role) {
        String sql = "UPDATE accounts SET username = ?, password = ?, role = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Should be hashed in production
            stmt.setString(3, role);
            stmt.setString(4, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an account by ID
     *
     * @param id ID of the account to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM accounts WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find an account by ID
     *
     * @param id ID of the account to find
     * @return Optional containing the account data if found
     */
    public Optional<Account> findById(String id) {
        String sql = "SELECT * FROM accounts WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding account by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Find an account by username
     *
     * @param username Username to search for
     * @return Optional containing the account data if found
     */
    public Optional<Account> findByUsername(String username) {
        String sql = "SELECT * FROM accounts WHERE username = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding account by username: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Find an account by person ID
     *
     * @param personId Person ID to search for
     * @return Optional containing the account data if found
     */
    public Optional<Account> findByPersonId(String personId) {
        String sql = "SELECT * FROM accounts WHERE person_id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, personId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding account by person ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Get all accounts
     *
     * @return List of all accounts
     */
    public List<Account> findAll() {
        String sql = "SELECT * FROM accounts";
        List<Account> accounts = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                accounts.add(mapResultSetToAccount(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding all accounts: " + e.getMessage());
            e.printStackTrace();
        }

        return accounts;
    }

    /**
     * Authenticate a user by username and password
     *
     * @param username Username to authenticate
     * @param password Password to authenticate
     * @return Optional containing the Person object if authentication successful
     */
    public Optional<Person> authenticate(String username, String password) {
        String sql = "SELECT a.*, p.* FROM accounts a " +
                "JOIN persons p ON a.person_id = p.id " +
                "WHERE a.username = ? AND a.password = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Should be hashed in production

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String personId = rs.getString("person_id");

                    // Based on role, retrieve appropriate person type
                    return loadPersonByRole(role, personId);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Load the appropriate Person subclass based on role and ID
     *
     * @param role User role
     * @param personId Person ID
     * @return Optional containing the Person object
     */
    private Optional<Person> loadPersonByRole(String role, String personId) throws SQLException {
        switch (role.toUpperCase()) {
            case "TEACHER":
                TeacherDAO teacherDAO = new TeacherDAO();
                return teacherDAO.findById(personId).map(teacher -> (Person) teacher);
            case "STUDENT":
                StudentDAO studentDAO = new StudentDAO();
                return studentDAO.findById(personId).map(student -> (Person) student);
            case "PARENT":
                ParentDAO parentDAO = new ParentDAO();
                return parentDAO.findById(personId).map(parent -> (Person) parent);
            default:
                return Optional.empty();
        }
    }

    /**
     * Change password for an account
     *
     * @param id Account ID
     * @param newPassword New password
     * @return true if successful, false otherwise
     */
    public boolean changePassword(String id, String newPassword) {
        String sql = "UPDATE accounts SET password = ? WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPassword); // Should be hashed in production
            stmt.setString(2, id);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map a ResultSet row to an Account object
     *
     * @param rs ResultSet containing account data
     * @return Account object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getString("id"));
        account.setUsername(rs.getString("username"));
        account.setPassword(rs.getString("password"));
        account.setPersonId(rs.getString("person_id"));
        account.setRole(rs.getString("role"));
        return account;
    }

    /**
     * Inner class to represent Account data
     */
    public static class Account {
        private String id;
        private String username;
        private String password;
        private String personId;
        private String role;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPersonId() {
            return personId;
        }

        public void setPersonId(String personId) {
            this.personId = personId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
