
package src.model.Notification;

import java.time.LocalDateTime;

public class Notification {
    private String notificationId; // ID của thông báo
    private String message; // Nội dung thông báo
    private String recipientRole; // Vai trò người nhận thông báo (ví dụ: ADMIN)
    private String senderId; // ID của tài khoản gửi (ví dụ: Teacher ID)
    private boolean isRead; // Trạng thái: Đã đọc/Chưa đọc
    private LocalDateTime createdAt; // Thời điểm tạo thông báo

    // Constructor chính để tạo thông báo mới (sẽ được lưu vào DB)
    public Notification(String notificationId, String message, String recipientRole, String senderId) {
        this.notificationId = notificationId;
        this.message = message;
        this.recipientRole = recipientRole;
        this.senderId = senderId;
        this.isRead = false; // Mặc định là chưa đọc khi tạo mới
        this.createdAt = LocalDateTime.now(); // Tự động gán thời điểm tạo khi tạo mới
    }

    // Constructor để tái tạo đối tượng Notification từ dữ liệu DB (được sử dụng bởi DAO)
    public Notification(String notificationId, String message, String recipientRole, String senderId, boolean isRead, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.message = message;
        this.recipientRole = recipientRole;
        this.senderId = senderId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters
    public String getNotificationId() {
        return notificationId;
    }

    public String getMessage() {
        return message;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public String getSenderId() {
        return senderId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    // (Không cần setter cho các trường khác nếu chúng chỉ được thiết lập qua constructor)
    // Nếu bạn cần setter cho createdAt cho DAO (ví dụ, nếu không dùng constructor đầy đủ ở trên):
    // public void setCreatedAt(LocalDateTime createdAt) {
    //     this.createdAt = createdAt;
    // }

    // Method: Hiển thị thông báo dưới dạng chuỗi
    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", message='" + message + '\'' +
                ", recipientRole='" + recipientRole + '\'' +
                ", senderId='" + senderId + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}

