
package src.model.Notification;

import src.dao.Notifications.NotificationDAO; // Import DAO

import java.util.List;
// import java.util.stream.Collectors; // Không còn cần thiết nếu DAO trả về đúng danh sách

public class NotificationService {
    // private final List<Notification> notificationList = new ArrayList<>(); // Xóa: Không còn lưu trong bộ nhớ
    private final NotificationDAO notificationDAO; // Thêm: Tham chiếu đến DAO

    // Constructor nhận NotificationDAO
    public NotificationService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    // Tạo và gửi thông báo đến Admins (lưu vào DB)
    public void sendNotificationToAdmins(String message, String senderId) {
        // Tạo ID duy nhất cho thông báo. Có thể xem xét dùng UUID nếu muốn chuẩn hơn.
        String notificationId = "NOTIF-" + System.currentTimeMillis();

        // Sử dụng constructor chính để tạo đối tượng Notification mới
        Notification notification = new Notification(
                notificationId,
                message,
                "ADMIN", // Vai trò nhận thông báo
                senderId
        );

        // Gọi DAO để thêm thông báo vào cơ sở dữ liệu
        boolean success = notificationDAO.addNotification(notification);
        if (success) {
            System.out.println("Thông báo đã được lưu vào DB và gửi đến Admin: " + notification);
        } else {
            System.err.println("Lỗi: Không thể lưu thông báo vào DB cho Admin: " + notification);
        }
    }

    // Lấy danh sách tất cả thông báo cho Admin từ DB
    public List<Notification> getNotificationsForAdmins() {
        // Gọi DAO để lấy tất cả thông báo cho vai trò "ADMIN"
        return notificationDAO.getNotificationsByRole("ADMIN");
        // Dòng filter cũ không còn cần thiết:
        // return notificationList.stream()
        //         .filter(n -> n.getRecipientRole().equals("ADMIN"))
        //         .collect(Collectors.toList());
    }

    // Lấy danh sách các thông báo CHƯA ĐỌC cho Admin từ DB
    public List<Notification> getUnreadNotificationsForAdmins() {
        return notificationDAO.getUnreadNotificationsByRole("ADMIN");
    }


    // Đánh dấu một thông báo là đã đọc trong DB
    public void markNotificationAsRead(String notificationId) {
        boolean success = notificationDAO.markAsRead(notificationId);
        if (success) {
            System.out.println("Thông báo '" + notificationId + "' đã được đánh dấu là đã đọc trong DB.");
        } else {
            System.err.println("Lỗi: Không thể đánh dấu thông báo '" + notificationId + "' là đã đọc trong DB.");
        }
        // Vòng lặp cũ không còn cần thiết:
        // for (Notification notification : notificationList) {
        //     if (notification.getNotificationId().equals(notificationId)) {
        //         notification.setRead(true);
        //         System.out.println("Thông báo đã được đánh dấu là đã đọc: " + notification);
        //         break;
        //     }
        // }
    }

    // Đánh dấu TẤT CẢ thông báo cho Admin là đã đọc trong DB
    public void markAllNotificationsAsReadForAdmins() {
        boolean success = notificationDAO.markAllAsReadForRole("ADMIN");
        if (success) {
            System.out.println("Tất cả thông báo chưa đọc cho ADMIN đã được đánh dấu là đã đọc trong DB.");
        } else {
            // Có thể không có thông báo nào để cập nhật, hoặc có lỗi
            System.out.println("Không có thông báo chưa đọc nào cho ADMIN để đánh dấu hoặc đã xảy ra lỗi khi cập nhật.");
        }
    }
}

