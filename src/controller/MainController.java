package src.controller;

import src.model.ClassSession;
import view.UI;
import view.components.DashboardView;
import view.components.ScheduleView;
// import các view khác nếu cần

/**
 * Controller chính của ứng dụng, quản lý logic nghiệp vụ
 */
public class MainController {
    private UI ui;
    private NavigationController navigationController;
    private ClassSession currentSessionDetail; // Added to store current session detail

    /**
     * Constructor với UI và NavigationController
     * @param ui Interface người dùng
     * @param navigationController Controller điều hướng
     */
    public MainController(UI ui, NavigationController navigationController) {
        this.ui = ui;
        this.navigationController = navigationController;
        initialize();
    }

    /**
     * Khởi tạo controller và đăng ký các màn hình
     */
    private void initialize() {
        registerViews();
    }

    /**
     * Đăng ký các view với NavigationController
     */
    private void registerViews() {
        // Đăng ký các views với NavigationController
        navigationController.registerView("dashboard", new DashboardView());

        // TODO: Đăng ký thêm các views khác
        // Ví dụ:
        // navigationController.registerView("student/list", new StudentListView());
         navigationController.registerView("schedule", new ScheduleView());
    }

    /**
     * Thực hiện hành động refresh màn hình hiện tại
     */
    public void refreshCurrentView() {
        navigationController.refreshCurrentScreen();
    }

    /**
     * Chuyển đến màn hình được chỉ định
     * @param route Định danh của màn hình
     */
    public void navigateTo(String route) {
        navigationController.navigateTo(route);
    }

    /**
     * Xử lý khi ứng dụng khởi động
     */
    public void onAppStart() {
        // Khởi tạo các tài nguyên, kết nối database, etc.
        System.out.println("Ứng dụng khởi động...");

        // Điều hướng đến màn hình mặc định
        navigateTo("dashboard");
    }

    /**
     * Xử lý khi ứng dụng kết thúc
     */
    public void onAppExit() {
        // Dọn dẹp tài nguyên, đóng kết nối, etc.
        System.out.println("Ứng dụng kết thúc...");
    }

    /**
     * Xử lý đăng nhập
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return true nếu đăng nhập thành công
     */
    public boolean login(String username, String password) {
        // TODO: Implement login logic
        return true;
    }

    /**
     * Xử lý đăng xuất
     */
    public void logout() {
        // TODO: Implement logout logic
        navigateTo("login");
    }

    /**
     * Lưu trữ thông tin chi tiết của một buổi học
     * @param session Buổi học cần lưu trữ
     */
    public void setSessionDetail(ClassSession session) {
        this.currentSessionDetail = session;
    }

    /**
     * Lấy thông tin chi tiết của buổi học đang được chọn
     * @return Thông tin buổi học
     */
    public ClassSession getSessionDetail() {
        return currentSessionDetail;
    }
}
