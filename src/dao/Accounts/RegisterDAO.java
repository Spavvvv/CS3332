
package src.dao.Accounts;

// Import các lớp model cần thiết (giữ lại nếu được sử dụng)
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
// Giữ lại các import này nếu chúng được sử dụng ở các phương thức khác
import java.util.UUID;

public class RegisterDAO {
    private DatabaseConnection dbConnector;

    public RegisterDAO() {
        this.dbConnector = new DatabaseConnection();
    }

    // Đã thêm tham số 'gender' vào phương thức
    public boolean registerUser(String username, String password, String roleFromUI,
                                String fullName, String email, String phone,
                                LocalDate dob, int age, String gender, // <-- THÊM THAM SỐ GENDER
                                String userIdForUsersTable) {
        Connection conn = null;
        PreparedStatement accountStmt = null;
        PreparedStatement userStmt = null;
        PreparedStatement roleStmt = null; // Sử dụng chung PreparedStatement cho các bảng vai trò

        String newAccountId = UUID.randomUUID().toString();
        String roleForDB;

        switch (roleFromUI.trim()) {
            case "Giáo viên":
                roleForDB = "TEACHER";
                break;
            case "Admin":
                roleForDB = "ADMIN";
                break;
            default:
                System.err.println("Vai trò từ UI không xác định hoặc không chuẩn: " + roleFromUI);
                // Xử lý mạnh hơn hoặc trả về false nếu vai trò không hợp lệ
                return false;
        }

        try {
            conn = dbConnector.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Kiểm tra tên đăng nhập và email tồn tại
            if (isUsernameExistsInternal(conn, username)) {
                System.out.println("Tên đăng nhập đã tồn tại: " + username);
                return false;
            }

            if (isEmailExistsInternal(conn, email)) {
                System.out.println("Email đã tồn tại: " + email);
                return false;
            }

            // 2. Chèn vào bảng accounts
            String insertAccountSQL = "INSERT INTO accounts (id, username, password, role) VALUES (?, ?, ?, ?)";
            accountStmt = conn.prepareStatement(insertAccountSQL);
            accountStmt.setString(1, newAccountId);
            accountStmt.setString(2, username);
            accountStmt.setString(3, password);
            accountStmt.setString(4, roleForDB);
            int accountRowsAffected = accountStmt.executeUpdate();

            if (accountRowsAffected <= 0) {
                System.err.println("Không chèn được vào bảng accounts.");
                conn.rollback();
                return false;
            }

            // 3. Chèn vào bảng users
            // Đã thêm cột 'gender' vào câu lệnh INSERT
            String insertUserSQL = "INSERT INTO users (id, account_id, name, email, contact_number, birthday, active, gender, address) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            userStmt = conn.prepareStatement(insertUserSQL);
            userStmt.setString(1, userIdForUsersTable); // ID cho bảng users
            userStmt.setString(2, newAccountId);       // Liên kết với account vừa tạo
            userStmt.setString(3, fullName);
            userStmt.setString(4, email);
            userStmt.setString(5, phone);
            userStmt.setDate(6, java.sql.Date.valueOf(dob));
            userStmt.setBoolean(7, true);              // active
            // Đặt giá trị cho cột 'gender' từ tham số truyền vào
            if (gender != null && !gender.trim().isEmpty()) {
                userStmt.setString(8, gender.trim());
            } else {
                userStmt.setNull(8, Types.VARCHAR); // Nếu gender rỗng hoặc null, đặt là NULL
            }
            // Giả sử address có thể NULL nếu không có từ UI
            userStmt.setNull(9, Types.VARCHAR);        // address

            int userRowsAffected = userStmt.executeUpdate();
            if (userRowsAffected <= 0) {
                System.err.println("Không chèn được vào bảng users.");
                conn.rollback();
                return false;
            }

            // 4. Chèn vào bảng vai trò cụ thể và cấu trúc bảng đã cung cấp
            String insertRoleSQL = null;
            String roleTableId = UUID.randomUUID().toString(); // Tạo ID riêng cho bảng vai trò

            switch (roleForDB) {
                case "TEACHER":
                    // Dựa trên cấu trúc bảng teachers bạn cung cấp
                    insertRoleSQL = "INSERT INTO teachers (id, user_id, name, gender, contact_number, birthday, email, address) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    roleStmt = conn.prepareStatement(insertRoleSQL);
                    roleStmt.setString(1, roleTableId);      // teachers.id
                    roleStmt.setString(2, userIdForUsersTable); // teachers.user_id (liên kết với users.id)
                    roleStmt.setString(3, fullName);         // teachers.name
                    // Đặt giá trị cho cột 'gender' từ tham số truyền vào
                    if (gender != null && !gender.trim().isEmpty()) {
                        roleStmt.setString(4, gender.trim());
                    } else {
                        roleStmt.setNull(4, Types.VARCHAR); // Nếu gender rỗng hoặc null, đặt là NULL
                    }
                    roleStmt.setString(5, phone);            // teachers.contact_number
                    roleStmt.setDate(6, java.sql.Date.valueOf(dob)); // teachers.birthday
                    roleStmt.setString(7, email);            // teachers.email
                    // Giả sử address có thể NULL
                    roleStmt.setNull(8, Types.VARCHAR);    // teachers.address
                    System.out.println("Chuẩn bị chèn vào bảng teachers...");
                    break;
                case "ADMIN":
                    // Admin không cần lưu vào bảng riêng, chỉ lưu thông tin trong bảng users
                    System.out.println("Admin được tạo thành công với User ID: " + userIdForUsersTable);
                    break;
                default:
                    // Trường hợp vai trò không cần chèn vào bảng riêng
                    System.out.println("Không cần chèn vào bảng vai trò cụ thể cho vai trò: " + roleForDB);
                    // Không cần roleStmt, do đó không làm gì ở đây
                    break;
            }

            // Chỉ thực thi roleStmt nếu nó đã được khởi tạo
            if (roleStmt != null) {
                int roleRowsAffected = roleStmt.executeUpdate();
                if (roleRowsAffected <= 0) {
                    System.err.println("Không chèn được vào bảng vai trò '" + roleForDB + "' cho user ID: " + userIdForUsersTable);
                    conn.rollback(); // Rollback nếu chèn vào bảng vai trò thất bại
                    return false;
                }
                System.out.println("Đã tạo bản ghi trong bảng vai trò '" + roleForDB + "' với ID: " + roleTableId + " cho user ID: " + userIdForUsersTable);
            }

            // 5. Commit transaction nếu tất cả các bước thành công
            conn.commit();
            System.out.println("Đăng ký thành công cho username: " + username + " với Account ID: " + newAccountId + " và User ID: " + userIdForUsersTable + " với vai trò: " + roleForDB);
            return true;

        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi đăng ký: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ")");
            e.printStackTrace();
            try {
                if (conn != null && !conn.isClosed() && !conn.getAutoCommit()) {
                    System.err.println("Đang tiến hành rollback do lỗi.");
                    conn.rollback();
                    System.err.println("Rollback thành công do lỗi.");
                }
            } catch (SQLException exRollback) {
                System.err.println("Lỗi nghiêm trọng khi rollback: " + exRollback.getMessage());
                exRollback.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (accountStmt != null) accountStmt.close();
                if (userStmt != null) userStmt.close();
                if (roleStmt != null) roleStmt.close();
                if (conn != null) {
                    if (!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                    conn.close();
                    System.out.println("Kết nối được đóng trong finally của registerUser.");
                }
            } catch (SQLException exClose) {
                System.err.println("Lỗi khi đóng tài nguyên: " + exClose.getMessage());
                exClose.printStackTrace();
            }
        }
    }

    // Các phương thức isUsernameExistsInternal, isEmailExistsInternal, isUsernameExists, isEmailExists giữ nguyên
    private boolean isUsernameExistsInternal(Connection conn, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean isEmailExistsInternal(Connection conn, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE username = ?";
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra tên đăng nhập tồn tại (public): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra email tồn tại (public): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}

