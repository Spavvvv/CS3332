package src.controller;

import java.util.HashMap;
import java.util.Map;
import view.UI;
import view.ScreenView;

/**
 * Controller phụ trách điều hướng giữa các màn hình trong ứng dụng
 */
public class NavigationController {
    private UI ui;
    private Map<String, ScreenView> viewsMap;
    private String currentRoute = "";
    private ScreenView currentView = null;

    /**
     * Constructor với UI
     * @param ui Interface người dùng
     */
    public NavigationController(UI ui) {
        this.ui = ui;
        this.viewsMap = new HashMap<>();
    }

    /**
     * Đăng ký một view vào hệ thống
     * @param route      Đường dẫn để truy cập view
     * @param screenView View cần đăng ký
     */
    public void registerView(String route, ScreenView screenView) {
        viewsMap.put(route, screenView);
        // Cung cấp reference đến NavigationController cho view có thể sử dụng
        screenView.setNavigationController(this);
    }

    /**
     * Điều hướng đến một màn hình cụ thể
     * @param route Đường dẫn đến màn hình cần hiển thị
     * @return true nếu điều hướng thành công, false nếu route không tồn tại
     */
    public boolean navigateTo(String route) {
        if (!viewsMap.containsKey(route)) {
            ui.showError("Route không tồn tại: " + route);
            return false;
        }

        // Kiểm tra xem view hiện tại có cho phép rời đi không
        if (currentView != null && !currentView.onDeactivate()) {
            return false;
        }

        ScreenView targetView = viewsMap.get(route);

        // Cập nhật UI với nội dung của view mới
        ui.setContent(targetView.getRoot());

        // Lưu lại route và view hiện tại
        currentRoute = route;
        currentView = targetView;

        // Thông báo cho view biết nó đã được kích hoạt
        targetView.onActivate();

        return true;
    }

    /**
     * Làm mới màn hình hiện tại
     */
    public void refreshCurrentScreen() {
        if (currentView != null) {
            currentView.refreshView();
        }
    }

    /**
     * Lấy route hiện tại
     * @return Route hiện tại
     */
    public String getCurrentRoute() {
        return currentRoute;
    }

    /**
     * Lấy view hiện tại
     * @return View hiện tại
     */
    public ScreenView getCurrentView() {
        return currentView;
    }

    /**
     * Kiểm tra một route có tồn tại không
     * @param route Route cần kiểm tra
     * @return true nếu route tồn tại, false nếu không
     */
    public boolean routeExists(String route) {
        return viewsMap.containsKey(route);
    }

    /**
     * Quay lại màn hình trước đó (phương thức dự phòng, cần triển khai stack lịch sử)
     * @return true nếu quay lại thành công, false nếu không thể quay lại
     */
    public boolean goBack() {
        // TODO: Implement history stack để hỗ trợ chức năng quay lại
        ui.showError("Chức năng quay lại chưa được triển khai");
        return false;
    }
}
