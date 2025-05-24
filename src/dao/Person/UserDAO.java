package src.dao.Person;


import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {
    public boolean delete(String userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        // Nên sử dụng dbConnector đã được khởi tạo trong constructor của UserDAO
        // Ví dụ: try (Connection conn = this.dbConnector.getConnection();
        // Hoặc nếu bạn có phương thức static trong DatabaseConnection:
        try (Connection conn = DatabaseConnection.getConnection(); // Giả sử bạn có phương thức static này
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa user với ID: " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ... (Các phương thức khác của UserDAO nếu có)
}
