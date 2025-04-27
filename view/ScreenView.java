package view;

import javafx.scene.Node;
import src.controller.NavigationController;
import src.controller.MainController;

/**
 * Interface cho tất cả các màn hình trong ứng dụng.
 * Mỗi màn hình phải triển khai các phương thức này để tương tác với hệ thống điều hướng.
 */
public interface ScreenView {

    /**
     * Trả về node gốc của màn hình
     * @return Node gốc cần được hiển thị
     */
    Node getRoot();

    /**
     * Được gọi khi màn hình được kích hoạt (được hiển thị)
     * Có thể được sử dụng để thực hiện các tác vụ như tải dữ liệu mới, cập nhật UI, etc.
     */
    void onActivate();

    /**
     * Được gọi khi cần làm mới màn hình
     * Thường được sử dụng sau khi có thay đổi dữ liệu
     */
    void refreshView();

    /**
     * Được gọi trước khi màn hình bị thay thế bởi màn hình khác
     * Có thể được sử dụng để lưu trạng thái, dọn dẹp tài nguyên, v.v.
     * @return true nếu có thể chuyển khỏi màn hình, false nếu không thể (ví dụ có form chưa lưu)
     */
    boolean onDeactivate();

    /**
     * Khởi tạo nội dung của màn hình
     * Tạo layout, controls, etc.
     */
    void initializeView();

    /**
     * Đặt tiêu đề cho màn hình
     * @return Tiêu đề của màn hình
     */
    String getTitle();

    /**
     * Thiết lập NavigationController cho view
     * @param navigationController Controller điều hướng
     */
    void setNavigationController(NavigationController navigationController);

    /**
     * Thiết lập MainController cho view
     * @param mainController Controller chính
     */
    void setMainController(MainController mainController);

    /**
     * Lấy ID duy nhất của màn hình
     * @return ID của màn hình, thường là route dùng để điều hướng
     */
    String getViewId();

    /**
     * Kiểm tra xem màn hình có yêu cầu quyền đăng nhập không
     * @return true nếu yêu cầu đăng nhập, false nếu không
     */
    boolean requiresAuthentication();

    /**
     * Phương thức được gọi khi có thông báo từ hệ thống
     * @param message Thông điệp cần xử lý
     * @param data Dữ liệu đi kèm (có thể là null)
     */
    void handleSystemMessage(String message, Object data);

    /**
     * Phương thức được gọi khi cần xử lý action từ người dùng
     * @param actionId ID của action cần xử lý
     * @param params Tham số đi kèm (có thể là null)
     * @return Kết quả xử lý (có thể là null)
     */
    Object handleAction(String actionId, Object params);
}
