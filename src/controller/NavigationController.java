package src.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import src.view.components.Screen.LoginUI;
import src.view.components.Screen.RegisterUI;
import src.view.components.Screen.UI;
import src.view.components.Screen.ScreenView;

/**
 Controller phụ trách điều hướng giữa các màn hình trong ứng dụng
 */
public class NavigationController {
    private UI ui;
    private LoginUI loginUI;
    private RegisterUI registerUI;
    private Map<String, ScreenView> viewsMap;
    private String currentRoute = "";
    private ScreenView currentView = null;
    private List<String> navigationHistory;
    private static final int MAX_HISTORY_SIZE = 10;

    // Map để lưu trữ trạng thái của các toggle buttons
    private Map<String, String> toggleStates = new HashMap<>();

    /**
     Constructor với UI
     @param ui Interface người dùng
     */
    public NavigationController(UI ui) {
        this.ui = ui;
        this.viewsMap = new HashMap<>();
        this.navigationHistory = new ArrayList<>();
    }
    public NavigationController(LoginUI ui) {
        this.loginUI = ui;
        this.viewsMap = new HashMap<>();
        this.navigationHistory = new ArrayList<>();
    }
    public NavigationController(RegisterUI ui) {
        this.registerUI = ui;
        this.viewsMap = new HashMap<>();
        this.navigationHistory = new ArrayList<>();
    }


    /**
     Đăng ký một src.view vào hệ thống
     @param route      Đường dẫn để truy cập src.view
     @param screenView View cần đăng ký
     */
    public void registerView(String route, ScreenView screenView) {
        viewsMap.put(route, screenView);
        // Cung cấp reference đến NavigationController cho src.view có thể sử dụng
        screenView.setNavigationController(this);
    }

    /**
     Điều hướng đến một màn hình cụ thể
     @param route Đường dẫn đến màn hình cần hiển thị
     @return true nếu điều hướng thành công, false nếu route không tồn tại
     */
    public boolean navigateTo(String route) {
        if (!viewsMap.containsKey(route)) {
            ui.showError("Route không tồn tại: " + route);
            return false;
        }

        // Kiểm tra xem src.view hiện tại có cho phép rời đi không
        if (currentView != null && !currentView.onDeactivate()) {
            return false;
        }

        ScreenView targetView = viewsMap.get(route);

        // Cập nhật UI với nội dung của src.view mới
        ui.setContent(targetView.getRoot());

        // Lưu route hiện tại vào lịch sử
        if (!currentRoute.isEmpty()) {
            updateNavigationHistory(currentRoute);
        }

        // Lưu lại route và src.view hiện tại
        currentRoute = route;
        currentView = targetView;

        // Gọi onShow() trước onActivate để chuẩn bị dữ liệu
        targetView.onShow();

        // Thông báo cho src.view biết nó đã được kích hoạt
        targetView.onActivate();

        return true;
    }


    /**
     Cập nhật lịch sử điều hướng
     @param route Route cần thêm vào lịch sử
     */
    private void updateNavigationHistory(String route) {
        navigationHistory.add(route);
        if (navigationHistory.size() > MAX_HISTORY_SIZE) {
            navigationHistory.remove(0);
        }
    }

    /**
     Làm mới màn hình hiện tại
     */
    public void refreshCurrentScreen() {
        if (currentView != null) {
            currentView.refreshView();
        }
    }

    /**
     Lấy route hiện tại
     @return Route hiện tại
     */
    public String getCurrentRoute() {
        return currentRoute;
    }

    /**
     Lấy src.view hiện tại
     @return View hiện tại
     */
    public ScreenView getCurrentView() {
        return currentView;
    }

    /**
     Kiểm tra một route có tồn tại không
     @param route Route cần kiểm tra
     @return true nếu route tồn tại, false nếu không
     */
    public boolean routeExists(String route) {
        return viewsMap.containsKey(route);
    }

    /**
     Quay lại màn hình trước đó dựa trên lịch sử điều hướng
     @return true nếu quay lại thành công, false nếu không thể quay lại
     */
    public boolean goBack() {
        if (navigationHistory.isEmpty()) {
            ui.showMessage("Không có trang trước đó trong lịch sử");
            return false;
        }
        // Lấy route cuối cùng trong lịch sử
        String previousRoute = navigationHistory.remove(navigationHistory.size() - 1);
        // Thực hiện điều hướng nhưng không cập nhật lịch sử
        if (!viewsMap.containsKey(previousRoute)) {
            ui.showError("Route không tồn tại: " + previousRoute);
            return false;
        }
        if (currentView != null && !currentView.onDeactivate()) {
            return false;
        }
        ScreenView targetView = viewsMap.get(previousRoute);
        ui.setContent(targetView.getRoot());
        currentRoute = previousRoute;
        currentView = targetView;
        targetView.onActivate();
        return true;
    }

    /**
     Trả về danh sách các route đã đăng ký
     @return Danh sách các route
     */
    public List<String> getAvailableRoutes() {
        return new ArrayList<>(viewsMap.keySet());
    }

    /**
     * Trả về danh sách các route đã đăng ký
     * @return Danh sách các route
     */
    public List<String> getRegisteredRoutes() {
        return new ArrayList<>(viewsMap.keySet());
    }

    /**
     * Lấy src.view tương ứng với route
     * @param route Đường dẫn cần lấy src.view
     * @return View tương ứng với route, null nếu route không tồn tại
     */
    public ScreenView getViewByRoute(String route) {
        return viewsMap.get(route);
    }

    /**
     * Lấy tất cả các src.view đã đăng ký
     * @return Danh sách các src.view
     */
    public List<ScreenView> getAllRegisteredViews() {
        return new ArrayList<>(viewsMap.values());
    }

    /**
     * Kiểm tra một src.view có đăng ký trong hệ thống không
     * @param view View cần kiểm tra
     * @return true nếu src.view đã đăng ký, false nếu không
     */
    public boolean isViewRegistered(ScreenView view) {
        return viewsMap.containsValue(view);
    }

    /**
     * Lấy route tương ứng với một src.view
     * @param view View cần lấy route
     * @return Route tương ứng với src.view, null nếu src.view không tồn tại
     */
    public String getRouteForView(ScreenView view) {
        for (Map.Entry<String, ScreenView> entry : viewsMap.entrySet()) {
            if (entry.getValue().equals(view)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Lưu trạng thái của một toggle button
     * @param key Khóa để lưu trạng thái
     * @param value Giá trị trạng thái
     */
    public void saveToggleState(String key, String value) {
        toggleStates.put(key, value);
    }

    /**
     * Lấy trạng thái đã lưu của một toggle button
     * @param key Khóa để lấy trạng thái
     * @return Giá trị trạng thái, null nếu không tồn tại
     */
    public String getSavedToggleState(String key) {
        return toggleStates.get(key);
    }

    /**
     * Kiểm tra xem một trạng thái có tồn tại không
     * @param key Khóa cần kiểm tra
     * @return true nếu trạng thái tồn tại, false nếu không
     */
    public boolean hasToggleState(String key) {
        return toggleStates.containsKey(key);
    }

    /**
     * Xóa một trạng thái đã lưu
     * @param key Khóa của trạng thái cần xóa
     */
    public void clearToggleState(String key) {
        toggleStates.remove(key);
    }

    /**
     * Xóa tất cả các trạng thái đã lưu
     */
    public void clearAllToggleStates() {
        toggleStates.clear();
    }
}
