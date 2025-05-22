package src.dao.Notifications;
import src.model.Notification.Notification;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    // Phương thức thêm một thông báo mới vào cơ sở dữ liệu
    public boolean addNotification(Notification notification) {
        String sql = "INSERT INTO notifications (notification_id, message, recipient_role, sender_id, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); // Lấy kết nối từ lớp tiện ích
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notification.getNotificationId());
            pstmt.setString(2, notification.getMessage());
            pstmt.setString(3, notification.getRecipientRole());
            pstmt.setString(4, notification.getSenderId());
            pstmt.setBoolean(5, notification.isRead());
            pstmt.setTimestamp(6, Timestamp.valueOf(notification.getCreatedAt()));

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding notification to DB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Phương thức lấy danh sách các thông báo cho một vai trò cụ thể (ví dụ: "ADMIN")
    public List<Notification> getNotificationsByRole(String role) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE recipient_role = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String notificationId = rs.getString("notification_id");
                String message = rs.getString("message");
                String recipientRole = rs.getString("recipient_role");
                String senderId = rs.getString("sender_id");
                boolean isRead = rs.getBoolean("is_read");
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                // Sử dụng constructor đầy đủ của Notification nếu bạn có
                Notification notification = new Notification(notificationId, message, recipientRole, senderId);
                notification.setRead(isRead);
                // Nếu constructor không tự set createdAt hoặc bạn muốn đảm bảo giá trị từ DB
                // notification.setCreatedAt(createdAt); // Cần thêm setter nếu chưa có hoặc điều chỉnh constructor

                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching notifications by role from DB: " + e.getMessage());
            e.printStackTrace();
        }
        return notifications;
    }

    // Phương thức lấy danh sách các thông báo CHƯA ĐỌC cho một vai trò cụ thể
    public List<Notification> getUnreadNotificationsByRole(String role) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE recipient_role = ? AND is_read = FALSE ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String notificationId = rs.getString("notification_id");
                String message = rs.getString("message");
                String recipientRole = rs.getString("recipient_role");
                String senderId = rs.getString("sender_id");
                // isRead ở đây luôn là false
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

                Notification notification = new Notification(notificationId, message, recipientRole, senderId);
                // notification.setRead(false); // Mặc định constructor đã set false
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching unread notifications by role from DB: " + e.getMessage());
            e.printStackTrace();
        }
        return notifications;
    }


    // Phương thức đánh dấu một thông báo là đã đọc
    public boolean markAsRead(String notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notificationId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error marking notification as read in DB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Phương thức đánh dấu TẤT CẢ thông báo của một vai trò là đã đọc
    public boolean markAllAsReadForRole(String role) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_role = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // Trả về true nếu có ít nhất 1 row được cập nhật
        } catch (SQLException e) {
            System.err.println("Error marking all notifications as read for role " + role + " in DB: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // Bạn có thể thêm các phương thức khác như deleteNotification, getNotificationById nếu cần
}

