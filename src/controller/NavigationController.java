package src.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import view.UI;
import view.ScreenView;

/**
 Controller phụ trách điều hướng giữa các màn hình trong ứng dụng
 */
public class NavigationController {
    private UI ui;
    private Map<String, ScreenView> viewsMap;
    private String currentRoute = "";
    private ScreenView currentView = null;
    private List<String> navigationHistory;
    private static final int MAX_HISTORY_SIZE = 10;
    /**
     Constructor với UI
     @param ui Interface người dùng
     */
    public NavigationController(UI ui) {
        this.ui = ui;
        this.viewsMap = new HashMap<>();
        this.navigationHistory = new ArrayList<>();
    }


    /**
     Đăng ký một view vào hệ thống
     @param route      Đường dẫn để truy cập view
     @param screenView View cần đăng ký
     */
    public void registerView(String route, ScreenView screenView) {
        viewsMap.put(route, screenView);
        // Cung cấp reference đến NavigationController cho view có thể sử dụng
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
        // Kiểm tra xem view hiện tại có cho phép rời đi không
        if (currentView != null && !currentView.onDeactivate()) {
            return false;
        }
        ScreenView targetView = viewsMap.get(route);
        // Cập nhật UI với nội dung của view mới
        ui.setContent(targetView.getRoot());
        // Lưu route hiện tại vào lịch sử
        if (!currentRoute.isEmpty()) {
            updateNavigationHistory(currentRoute);
        }
        // Lưu lại route và view hiện tại
        currentRoute = route;
        currentView = targetView;
        // Thông báo cho view biết nó đã được kích hoạt
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
     Lấy view hiện tại
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
     * Lấy view tương ứng với route
     * @param route Đường dẫn cần lấy view
     * @return View tương ứng với route, null nếu route không tồn tại
     */
    public ScreenView getViewByRoute(String route) {
        return viewsMap.get(route);
    }

    /**
     * Lấy tất cả các view đã đăng ký
     * @return Danh sách các view
     */
    public List<ScreenView> getAllRegisteredViews() {
        return new ArrayList<>(viewsMap.values());
    }

    /**
     * Kiểm tra một view có đăng ký trong hệ thống không
     * @param view View cần kiểm tra
     * @return true nếu view đã đăng ký, false nếu không
     */
    public boolean isViewRegistered(ScreenView view) {
        return viewsMap.containsValue(view);
    }

    /**
     * Lấy route tương ứng với một view
     * @param view View cần lấy route
     * @return Route tương ứng với view, null nếu view không tồn tại
     */
    public String getRouteForView(ScreenView view) {
        for (Map.Entry<String, ScreenView> entry : viewsMap.entrySet()) {
            if (entry.getValue().equals(view)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
