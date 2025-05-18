package view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import src.controller.NavigationController;
import src.controller.MainController;
import src.model.person.Person;

/**
 * Lớp cơ sở triển khai interface ScreenView.
 * Cung cấp các triển khai mặc định cho các phương thức của ScreenView
 * và chức năng bổ sung mà các màn hình cụ thể có thể kế thừa.
 */
public abstract class BaseScreenView implements ScreenView {

    protected VBox root;
    protected StringProperty title = new SimpleStringProperty("Unnamed Screen");
    protected String viewId = "base-view";
    protected static NavigationController navigationController;
    protected static MainController mainController;
    private boolean initialized = false;

    public BaseScreenView() {
        root = new VBox();
        // Không gọi initializeView() ngay lập tức để tránh NullPointerException
        // khi lớp con đang trong quá trình khởi tạo
    }

    public BaseScreenView(String title) {
        this();
        this.title.set(title);
    }

    protected Person getCurrentUser() {
        if (mainController != null) {
            return mainController.getCurrentUser();
        }
        return null;
    }
    public BaseScreenView(String title, String viewId) {
        this(title);
        this.viewId = viewId;
    }

    @Override
    public Node getRoot() {
        // Đảm bảo view đã được khởi tạo khi yêu cầu root
        ensureInitialized();
        return root;
    }

    /**
     * Đảm bảo view đã được khởi tạo. Phương thức này được gọi
     * khi cần truy cập các thành phần đã khởi tạo của view.
     */
    protected void ensureInitialized() {
        if (!initialized) {
            initializeView();
            initialized = true;
        }
    }

    @Override
    public void onActivate() {
        // Đảm bảo view đã được khởi tạo trước khi active
        ensureInitialized();
        refreshView();
    }

    @Override
    public void refreshView() {
        // Triển khai mặc định - các lớp con nên ghi đè để cập nhật dữ liệu
    }

    @Override
    public boolean onDeactivate() {
        // Triển khai mặc định - trả về true để cho phép chuyển màn hình
        return true;
    }

    @Override
    public abstract void initializeView();

    @Override
    public String getTitle() {
        return title.get();
    }

    @Override
    public void setNavigationController(NavigationController navigationController) {
        this.navigationController = navigationController;
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Lấy đối tượng NavigationController
     * @return đối tượng NavigationController đã được thiết lập
     */
    @Override
    public NavigationController getNavigationController() {
        return this.navigationController;
    }

    /**
     * Lấy đối tượng MainController
     * @return đối tượng MainController đã được thiết lập
     */
    @Override
    public MainController getMainController() {
        return this.mainController;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public boolean requiresAuthentication() {
        // Mặc định không yêu cầu xác thực
        return false;
    }

    @Override
    public void handleSystemMessage(String message, Object data) {
        // Mặc định không xử lý, các lớp con có thể ghi đè
    }

    @Override
    public Object handleAction(String actionId, Object params) {
        // Mặc định không xử lý, các lớp con có thể ghi đè
        return null;
    }

    /**
     * Đặt tiêu đề cho màn hình
     * @param title Tiêu đề mới
     */
    public void setTitle(String title) {
        this.title.set(title);
    }

    /**
     * Trả về đối tượng StringProperty của tiêu đề
     * Hữu ích để binding với các phần tử UI khác
     * @return StringProperty của tiêu đề
     */
    public StringProperty titleProperty() {
        return title;
    }

    /**
     * Hiển thị thông báo lỗi cho người dùng
     * @param message Nội dung lỗi cần hiển thị
     */
    public void showError(String message) {
        // TODO: Triển khai hiển thị thông báo lỗi
        System.err.println("Error: " + message);
    }

    /**
     * Hiển thị thông báo thành công cho người dùng
     * @param message Nội dung thông báo
     */
    public void showSuccess(String message) {
        // TODO: Triển khai hiển thị thông báo thành công
        System.out.println("Success: " + message);
    }

    /**
     * Xác nhận hành động với người dùng
     * @param message Nội dung xác nhận
     * @return true nếu người dùng xác nhận, ngược lại false
     */
    protected boolean confirm(String message) {
        // TODO: Triển khai hộp thoại xác nhận
        // Mặc định trả về true
        return true;
    }

    @Override
    public void onShow() {
        // Đảm bảo view đã được khởi tạo trước khi hiển thị
        ensureInitialized();
        System.out.println("BaseScreenView.onShow() cho " + viewId);
    }
}
