package src.view.Schedule;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import src.model.ClassSession;
import src.view.components.Screen.BaseScreenView;

/**
 * ClassDetailsView hiển thị thông tin chi tiết của một buổi học
 * xuất hiện khi người dùng nhấn vào một lớp học trong lịch
 */
public class ClassDetailsView extends BaseScreenView {
    private ClassSession session;
    private Button backButton;
    private Button editButton;
    private Button deleteButton;
    private GridPane detailsGrid;
    private Label titleLabel;
    private Label errorLabel;
    private HBox actionBox;

    public ClassDetailsView() {
        super("Chi tiết lớp học", "classroom-attendance-src.view");
    }

    @Override
    public void initializeView() {
        // Thiết lập layout chung
        root.setPadding(new Insets(20));
        root.setMinWidth(400);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(15);

        // Đảm bảo tất cả text đều có màu đen
        root.setStyle("-fx-text-fill: black;");

        // Tiêu đề lớp học
        titleLabel = new Label("Chi tiết lớp học");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: black;");

        // Label hiển thị lỗi
        errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: red;");
        errorLabel.setVisible(false);

        // Grid hiển thị thông tin chi tiết
        detailsGrid = new GridPane();
        detailsGrid.setHgap(15);
        detailsGrid.setVgap(10);
        detailsGrid.setPadding(new Insets(10, 0, 20, 0));
        detailsGrid.setAlignment(Pos.CENTER);
        detailsGrid.setStyle("-fx-text-fill: black;");

        // Khởi tạo các button hành động
        backButton = new Button("Quay lại");
        backButton.setStyle("-fx-text-fill: black;");
        backButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.goBack();
            }
        });

        // Box chứa các nút hành động
        actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.getChildren().addAll(backButton);

        // Thêm các thành phần vào layout chính
        root.getChildren().addAll(titleLabel, errorLabel, detailsGrid, actionBox);
    }

    @Override
    public void onShow() {
        // QUAN TRỌNG: Load session từ mainController mỗi khi src.view được hiển thị
        System.out.println("ClassDetailsView.onShow() được gọi");

        if (mainController != null) {
            System.out.println("mainController trong ClassDetailsView: CÓ GIÁ TRỊ");
            session = mainController.getSessionDetail();
            System.out.println("Session đã được load: " +
                    (session != null ? session.getCourseName() : "NULL"));
        } else {
            System.err.println("LỖI: mainController là null trong ClassDetailsView.onShow()");
        }

        refreshView();
    }

    @Override
    public void refreshView() {
        // Xóa nội dung grid hiện tại
        detailsGrid.getChildren().clear();

        // Ẩn thông báo lỗi
        errorLabel.setVisible(false);

        if (session == null) {
            errorLabel.setText("Không tìm thấy thông tin lớp học");
            errorLabel.setVisible(true);
            return;
        }

        // Cập nhật tiêu đề
        titleLabel.setText(session.getCourseName());

        // Thêm thông tin chi tiết vào grid
        int row = 0;

        addDetailRow(detailsGrid, "Mã lớp:", String.valueOf(session.getId()), row++);
        addDetailRow(detailsGrid, "Tên khóa học:", session.getCourseName(), row++);
        addDetailRow(detailsGrid, "Giảng viên:", session.getTeacher(), row++);
        addDetailRow(detailsGrid, "Phòng học:", session.getRoom(), row++);
        addDetailRow(detailsGrid, "Ngày học:", session.getFormattedDate(), row++);
        addDetailRow(detailsGrid, "Thứ:", session.getDayOfWeek(), row++);
        addDetailRow(detailsGrid, "Khung giờ:", session.getTimeSlot(), row++);
    }

    /**
     * Thêm một hàng thông tin vào grid
     */
    private void addDetailRow(GridPane grid, String labelText, String value, int rowIndex) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        Label valueLabel = new Label(value != null ? value : "");
        valueLabel.setStyle("-fx-text-fill: black;");
        grid.add(label, 0, rowIndex);
        grid.add(valueLabel, 1, rowIndex);
    }

    @Override
    public boolean requiresAuthentication() {
        return true; // Yêu cầu đăng nhập để xem chi tiết lớp học
    }

    @Override
    public void handleSystemMessage(String message, Object data) {
        if ("class_updated".equals(message) && data instanceof ClassSession) {
            this.session = (ClassSession) data;
            refreshView();
        } else if ("class_deleted".equals(message)) {
            // Xử lý khi lớp học bị xóa - có thể quay lại màn hình trước
            if (navigationController != null) {
                navigationController.goBack();
            }
        }
    }

    @Override
    public Object handleAction(String actionId, Object params) {
        if ("edit_class".equals(actionId)) {
            // Lưu trữ session hiện tại vào MainController trước khi chuyển trang
            if (mainController != null && session != null) {
                mainController.setSessionDetail(session);
            }
            // Sau đó chuyển đến màn hình chỉnh sửa lớp học
            if (navigationController != null) {
                navigationController.navigateTo("editClass");
                return true;
            }
        } else if ("delete_class".equals(actionId)) {
            // Gọi hàm xóa lớp học từ controller
            if (mainController != null && session != null) {
                boolean success = mainController.deleteClassSession(session.getId());
                if (success) {
                    // Nếu xóa thành công, quay lại màn hình trước
                    if (navigationController != null) {
                        navigationController.goBack();
                    }
                    return true;
                } else {
                    // Nếu xóa thất bại, hiển thị thông báo lỗi
                    errorLabel.setText("Không thể xóa lớp học. Vui lòng thử lại sau.");
                    errorLabel.setVisible(true);
                }
            }
        }
        return null;
    }
}
