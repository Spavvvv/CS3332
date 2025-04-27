package view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Lớp cơ sở triển khai interface ScreenView.
 * Cung cấp các triển khai mặc định cho các phương thức của ScreenView
 * và chức năng bổ sung mà các màn hình cụ thể có thể kế thừa.
 */
public abstract class BaseScreenView implements ScreenView {

    protected VBox root;
    protected StringProperty title = new SimpleStringProperty("Unnamed Screen");

    public BaseScreenView() {
        root = new VBox();
        initializeView();
    }

    public BaseScreenView(String title) {
        this();
        this.title.set(title);
    }

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public void onActivate() {
        // Triển khai mặc định - các lớp con có thể ghi đè
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
    public String getTitle() {
        return title.get();
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
    protected void showError(String message) {
        // TODO: Triển khai hiển thị thông báo lỗi
        System.err.println("Error: " + message);
    }

    /**
     * Hiển thị thông báo thành công cho người dùng
     * @param message Nội dung thông báo
     */
    protected void showSuccess(String message) {
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
}
