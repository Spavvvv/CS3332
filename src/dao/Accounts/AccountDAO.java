package src.dao.Accounts;

import src.dao.Person.TeacherDAO;
import src.model.person.*;
import src.utils.DatabaseConnection;

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
     * @param role Role of the user (TEACHER, ADMIN)
     * @return true if successful, false otherwise
     */
    public boolean save(String username, String password, String role) {
        // Thủ tục hai bước: lưu account trước, sau đó tạo user record liên kết với account
        String sql = "INSERT INTO accounts (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Nên mã hóa trong môi trường production
            stmt.setString(3, role);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Lấy ID của account vừa tạo
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        String accountId = generatedKeys.getString(1);

                        // Tạo user record mới với account_id
                        String userSql = "INSERT INTO users (account_id, active) VALUES (?, true)";
                        try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                            userStmt.setString(1, accountId);
                            return userStmt.executeUpdate() > 0;
                        }
                    }
                }
            }
            return false;

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
     * @param userId Person ID to search for
     * @return Optional containing the account data if found
     */
    public Optional<Account> findByUserId(String userId) {
        // Thay đổi truy vấn để tìm account dựa trên user id từ bảng users
        String sql = "SELECT a.* FROM accounts a " +
                "JOIN users u ON u.account_id = a.id " +
                "WHERE u.id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding account by user ID: " + e.getMessage());
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
        // Truy vấn cả bảng accounts và users
        String sql = "SELECT a.*, u.* FROM accounts a " +
                "JOIN users u ON u.account_id = a.id " +
                "WHERE a.username = ? AND a.password = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Lấy thông tin từ bảng accounts
                    String accountId = rs.getString("a.id");
                    String role = rs.getString("a.role");

                    // Lấy thông tin từ bảng users
                    String userId = rs.getString("u.id");
                    String name = rs.getString("u.name");
                    String email = rs.getString("u.email");
                    boolean isActive = rs.getBoolean("u.active");

                    // Kiểm tra trạng thái hoạt động
                    if (!isActive) {
                        System.out.println("Tài khoản không hoạt động.");
                        return Optional.empty();
                    }

                    // Dựa vào vai trò, tạo đối tượng Person tương ứng
                    return createPersonObject(role, userId, name, email);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi trong quá trình xác thực: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // Hàm tạo đối tượng Person dựa vào vai trò
    private Optional<Person> createPersonObject(String role, String userId, String name, String email) {
        switch (role.toUpperCase()) {
            case "TEACHER":
                return Optional.of(new Teacher(userId, name, "Không xác định", email, "", "", userId, "Giáo viên"));
            case "ADMIN":
                return Optional.of(new Admin(userId, name, "Không xác định", email, "", "", null));
            default:
                return Optional.empty();
        }
    }

    /**
     * Load the appropriate Person subclass based on role and ID
     *
     * @param role User role
     * @param userId Person ID
     * @return Optional containing the Person object
     */
    private Optional<Person> loadPersonByRole(String role, String userId) throws SQLException {
        // Thay đổi cách lấy dữ liệu từ DAO tương ứng với từng loại người dùng
        switch (role.toUpperCase()) {
            case "TEACHER":
                TeacherDAO teacherDAO = new TeacherDAO();
                return teacherDAO.findById(userId).map(teacher -> (Person) teacher);
            case "ADMIN":
                String sql = "SELECT * FROM users WHERE id = ?";
                try (Connection conn = dbConnector.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            String email = rs.getString("email");
                            String gender = rs.getString("gender");
                            String contactNumber = rs.getString("contact_number");
                            String birthday = rs.getString("birthday");
                            // String address = rs.getString("address"); // Nếu cần
                            // ---- QUAN TRỌNG: Xác định nguồn gốc của 'accessLevel' ----
                            // Tương tự như trong createPersonObject, bạn cần quyết định cách lấy accessLevel.
                            // Ví dụ: lấy từ cột 'access_level' trong bảng 'users' nếu có, hoặc giá trị mặc định.
                            String accessLevel = "Full"; // << THAY ĐỔI HOẶC CẤU HÌNH GIÁ TRỊ NÀY
                            return Optional.of(new Admin(userId, name, gender, contactNumber, birthday, email, accessLevel));
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi tải dữ liệu Admin từ bảng users: " + e.getMessage());
                    e.printStackTrace();
                }
                return Optional.empty(); // Không tìm thấy Admin hoặc có lỗi
            default:
                // STUDENT và PARENT không còn được xử lý ở đây cho việc load Person qua account
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