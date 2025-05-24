package src.dao.Accounts;

import src.dao.Person.TeacherDAO;
import src.model.person.*;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // Phương thức save này có vẻ khác với phiên bản trước đó trong artifact.
        // Sử dụng logic từ code bạn cung cấp.
        // Cần đảm bảo ID cho bảng accounts và users được xử lý đúng.
        // Schema của bạn yêu cầu ID cho accounts (VARCHAR 50, NOT NULL, PK).
        // Schema của bạn yêu cầu ID cho users (VARCHAR 50, NOT NULL, PK).

        String accountSql = "INSERT INTO accounts (id, username, password, role) VALUES (?, ?, ?, ?)";
        String userSql = "INSERT INTO users (id, account_id, name, active) VALUES (?, ?, ?, true)";

        // Tạo ID mới cho account và user (ví dụ sử dụng UUID)
        String newAccountId = java.util.UUID.randomUUID().toString().substring(0, Math.min(50, 36));
        String newUserId = java.util.UUID.randomUUID().toString().substring(0, Math.min(50, 36));
        // Tên mặc định cho user mới, bạn có thể muốn truyền tên này vào hàm save
        String defaultNameForNewUser = "Người dùng " + username;


        try (Connection conn = dbConnector.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            // Bước 1: Chèn vào bảng accounts
            try (PreparedStatement stmt = conn.prepareStatement(accountSql)) {
                stmt.setString(1, newAccountId); // Cung cấp ID cho account
                stmt.setString(2, username);
                stmt.setString(3, password); // Nên mã hóa trong môi trường production
                stmt.setString(4, role);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false; // Không chèn được account
                }
            }

            // Bước 2: Chèn vào bảng users, liên kết với account_id vừa tạo
            try (PreparedStatement userStmt = conn.prepareStatement(userSql)) {
                userStmt.setString(1, newUserId); // ID cho bảng users
                userStmt.setString(2, newAccountId); // account_id từ account vừa tạo
                userStmt.setString(3, defaultNameForNewUser); // Tên người dùng
                // Các trường khác của users có thể cần giá trị mặc định hoặc null
                // active đã được đặt là true trong câu SQL
                if (userStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false; // Không chèn được user
                }
            }

            conn.commit(); // Hoàn thành transaction
            return true;

        } catch (SQLException e) {
            System.err.println("Error saving account: " + e.getMessage());
            e.printStackTrace();
            // Cố gắng rollback nếu có lỗi và connection còn mở
            // try (Connection conn = dbConnector.getConnection()) { if (conn != null && !conn.isClosed()) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
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
        // Lưu ý: Khóa ngoại users.account_id tham chiếu đến accounts.id có ON DELETE SET NULL.
        // Nên việc xóa account sẽ tự động cập nhật users.account_id thành NULL cho các user liên quan.
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
     * Find an account by user ID (users.id)
     * This method is crucial for SettingView to link Teacher (User) to their Account.
     * @param userId ID of the user (from users.id)
     * @return Optional containing the account data if found
     */
    public Optional<Account> findByUserId(String userId) {
        String sql = "SELECT a.* FROM accounts a " +
                "JOIN users u ON u.account_id = a.id " +
                "WHERE u.id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // mapResultSetToAccount sẽ chỉ đọc các cột của bảng 'accounts'
                    return Optional.of(mapResultSetToAccount(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding account by user ID (" + userId + "): " + e.getMessage());
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
        String sql = "SELECT a.id as account_table_id, a.role, u.* FROM accounts a " + // Đổi tên a.id để tránh trùng với u.id
                "JOIN users u ON u.account_id = a.id " +
                "WHERE a.username = ? AND a.password = ?"; // Cân nhắc sử dụng password hashing

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("a.role"); // Lấy từ alias của bảng accounts
                    String userId = rs.getString("u.id"); // ID từ bảng users
                    String name = rs.getString("u.name");
                    String email = rs.getString("u.email");
                    String gender = rs.getString("u.gender");
                    String contactNumber = rs.getString("u.contact_number");
                    String birthday = rs.getString("u.birthday"); // Dạng String từ DB
                    boolean isActive = rs.getBoolean("u.active");

                    if (!isActive) {
                        System.out.println("Tài khoản người dùng (" + name + ") không hoạt động.");
                        return Optional.empty();
                    }

                    switch (role.toUpperCase()) {
                        case "TEACHER":
                            // Teacher constructor: (id, name, gender, contactNumber, birthday, email, teacherId, position)
                            // `userId` (từ users.id) được dùng làm ID chính cho Person.
                            // `teacherId` trong Teacher model có thể là users.id hoặc một ID riêng từ bảng teachers.
                            // Dựa trên TeacherDAO, teacher.getId() là users.id, teacher.getTeacherId() là teachers.id.
                            // Ở đây, chúng ta không có teachers.id trực tiếp, nên có thể dùng users.id cho teacherId.
                            return Optional.of(new Teacher(userId, name, gender, contactNumber, birthday, email, userId)); // Sử dụng userId cho teacherId, role cho position
                        case "ADMIN":
                            // Admin constructor: (id, name, gender, contactNumber, birthday, email, accessLevel)
                            return Optional.of(new Admin(userId, name, gender, contactNumber, birthday, email, "FullAccess")); // Giả định accessLevel
                        default:
                            System.err.println("Vai trò không xác định trong authenticate: " + role);
                            return Optional.empty();
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi trong quá trình xác thực: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // Hàm tạo đối tượng Person dựa vào vai trò (đã được tích hợp vào authenticate)
    // private Optional<Person> createPersonObject(String role, String userId, String name, String email) { ... }

    /**
     * Load the appropriate Person subclass based on role and ID
     *
     * @param role User role
     * @param userId Person ID (users.id)
     * @return Optional containing the Person object
     */
    private Optional<Person> loadPersonByRole(String role, String userId) throws SQLException {
        // Phương thức này có thể không cần thiết nếu authenticate đã xử lý việc tạo Person.
        // Tuy nhiên, giữ lại nếu có trường hợp sử dụng khác.
        switch (role.toUpperCase()) {
            case "TEACHER":
                TeacherDAO teacherDAO = new TeacherDAO(); // Cần xử lý SQLException từ constructor TeacherDAO
                // TeacherDAO.findById(userId) mong đợi users.id (vì Teacher.id là users.id)
                return teacherDAO.findById(userId).map(teacher -> (Person) teacher);
            case "ADMIN":
                // Lấy thông tin Admin từ bảng users
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
                            String accessLevel = "Full"; // Giả định
                            return Optional.of(new Admin(userId, name, gender, contactNumber, birthday, email, accessLevel));
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi tải dữ liệu Admin từ bảng users: " + e.getMessage());
                    e.printStackTrace();
                }
                return Optional.empty();
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
     * Map a ResultSet row to an Account object.
     * This method is critical for findByUserId to work correctly for SettingView.
     *
     * @param rs ResultSet containing account data (assumed to be columns from 'accounts' table)
     * @return Account object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getString("id"));
        account.setUsername(rs.getString("username"));
        account.setPassword(rs.getString("password")); // Chỉ lấy để map, không dùng trực tiếp
        account.setRole(rs.getString("role"));
        // Đã xóa: account.setPersonId(rs.getString("person_id")); // Dòng này gây lỗi
        return account;
    }

    /**
     * Inner class to represent Account data.
     * This class is used by SettingView indirectly via findByUserId.
     */
    public static class Account {
        private String id;
        private String username;
        private String password; // Chỉ dùng để map, không nên sử dụng trực tiếp
        // Đã xóa: private String personId; // Thuộc tính này gây lỗi
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

        // Đã xóa getter và setter cho personId
        // public String getPersonId() { return personId; }
        // public void setPersonId(String personId) { this.personId = personId; }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
