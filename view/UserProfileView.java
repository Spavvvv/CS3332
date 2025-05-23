package view; // Hoặc package phù hợp với cấu trúc dự án của bạn

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField; // Thêm import cho PasswordField
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import src.dao.AccountDAO;
import src.model.person.Admin;
import src.model.person.Person;
import src.model.person.Teacher;
import javafx.stage.Stage; // Thêm import cho Stage

import java.sql.SQLException;
import java.util.Optional;

/**
 * Giao diện hiển thị thông tin cá nhân của người dùng (Portfolio).
 * Hiển thị username và vai trò của người dùng đang đăng nhập.
 */
public class UserProfileView extends BaseScreenView {

    private Label usernameLabelValue;
    private Label roleLabelValue;
    private Button changePasswordButton;
    private Button logoutButton; // Khai báo nút đăng xuất

    public UserProfileView() {
        super("Hồ sơ cá nhân", "profile");
        System.out.println("UserProfileView (" + this.hashCode() + "): Constructor called.");
    }

    @Override
    public void initializeView() {
        System.out.println("UserProfileView (" + this.hashCode() + "): Initializing view (initializeView)...");
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setSpacing(20);

        Label titleLabel = new Label("Thông Tin Tài Khoản");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #333;");

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(15);

        Label usernameLabel = new Label("Tên đăng nhập:");
        usernameLabel.setFont(Font.font("Arial", FontWeight.MEDIUM, 16));
        usernameLabel.setStyle("-fx-text-fill: #555;");

        usernameLabelValue = new Label("Đang tải...");
        usernameLabelValue.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        usernameLabelValue.setStyle("-fx-text-fill: #333333;");

        Label roleLabel = new Label("Vai trò:");
        roleLabel.setFont(Font.font("Arial", FontWeight.MEDIUM, 16));
        roleLabel.setStyle("-fx-text-fill: #555;");

        roleLabelValue = new Label("Đang tải...");
        roleLabelValue.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        roleLabelValue.setStyle("-fx-text-fill: #333333;");

        changePasswordButton = new Button("Đổi mật khẩu");
        changePasswordButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
        changePasswordButton.setOnAction(e -> handleChangePassword());

        logoutButton = new Button("Đăng xuất");
        logoutButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        logoutButton.setOnAction(e -> handleLogout());

        // HBox để chứa các nút
        HBox buttonsBox = new HBox(10); // Khoảng cách 10px giữa các nút
        buttonsBox.getChildren().addAll(changePasswordButton, logoutButton);

        infoGrid.add(usernameLabel, 0, 0);
        infoGrid.add(usernameLabelValue, 1, 0);
        infoGrid.add(roleLabel, 0, 1);
        infoGrid.add(roleLabelValue, 1, 1);
        // Thêm HBox chứa các nút vào GridPane
        infoGrid.add(buttonsBox, 0, 2, 2, 1); // Cột 0, hàng 2, kéo dài 2 cột, 1 hàng
        GridPane.setHalignment(buttonsBox, HPos.LEFT);


        root.getChildren().clear();
        root.getChildren().addAll(titleLabel, infoGrid);
        System.out.println("UserProfileView (" + this.hashCode() + "): View initialized.");
    }

    private void handleChangePassword() {
        Person currentUser = getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin người dùng hiện tại.");
            return;
        }

        // Bước 1: Yêu cầu nhập mật khẩu cũ
        TextInputDialog oldPasswordDialog = new TextInputDialog();
        oldPasswordDialog.setTitle("Xác thực");
        oldPasswordDialog.setHeaderText("Vui lòng nhập mật khẩu hiện tại của bạn.");
        // Sử dụng PasswordField trong dialog (nâng cao hơn, tạm thời dùng TextInputDialog cho đơn giản)
        // Hoặc tạo một Custom Dialog với PasswordField
        Label oldPasswordLabel = new Label("Mật khẩu hiện tại:");
        PasswordField oldPasswordField = new PasswordField();
        GridPane oldPassGrid = new GridPane();
        oldPassGrid.setHgap(10);
        oldPassGrid.setVgap(10);
        oldPassGrid.add(oldPasswordLabel, 0, 0);
        oldPassGrid.add(oldPasswordField, 1, 0);
        oldPasswordDialog.getDialogPane().setContent(oldPassGrid);
        // Ghi đè lại để lấy giá trị từ PasswordField
        oldPasswordDialog.setResultConverter(dialogButton -> {
            if (dialogButton == oldPasswordDialog.getDialogPane().getButtonTypes().get(0)) { // Nút OK
                return oldPasswordField.getText();
            }
            return null;
        });


        Optional<String> oldPasswordResult = oldPasswordDialog.showAndWait();

        if (!oldPasswordResult.isPresent() || oldPasswordResult.get().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Hủy bỏ", "Thao tác đổi mật khẩu đã bị hủy.");
            return;
        }
        String enteredOldPassword = oldPasswordResult.get();

        try {
            AccountDAO accountDAO = new AccountDAO();
            // Xác thực mật khẩu cũ
            // Lấy username của tài khoản từ currentUser.getId() (là users.id) -> accounts.username
            Optional<AccountDAO.Account> accOptional = accountDAO.findByUserId(currentUser.getId()); //
            if (!accOptional.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin tài khoản.");
                return;
            }
            // Sử dụng phương thức authenticate của AccountDAO để kiểm tra username và mật khẩu cũ
            Optional<Person> authenticatedUser = accountDAO.authenticate(accOptional.get().getUsername(), enteredOldPassword); //

            if (!authenticatedUser.isPresent()) {
                showAlert(Alert.AlertType.ERROR, "Sai mật khẩu", "Mật khẩu hiện tại không đúng.");
                return;
            }

            // Nếu mật khẩu cũ đúng, tiếp tục yêu cầu mật khẩu mới
            TextInputDialog newPasswordDialog = new TextInputDialog();
            newPasswordDialog.setTitle("Đổi mật khẩu");
            newPasswordDialog.setHeaderText("Nhập mật khẩu mới cho tài khoản: " + currentUser.getName());
            newPasswordDialog.setContentText("Mật khẩu mới:");

            Optional<String> newPasswordResult = newPasswordDialog.showAndWait();
            if (newPasswordResult.isPresent() && !newPasswordResult.get().trim().isEmpty()) {
                String newPassword = newPasswordResult.get().trim();

                TextInputDialog confirmPasswordDialog = new TextInputDialog();
                confirmPasswordDialog.setTitle("Xác nhận mật khẩu");
                confirmPasswordDialog.setHeaderText("Nhập lại mật khẩu mới để xác nhận.");
                confirmPasswordDialog.setContentText("Xác nhận mật khẩu mới:");
                Optional<String> confirmPasswordResult = confirmPasswordDialog.showAndWait();

                if (confirmPasswordResult.isPresent() && newPassword.equals(confirmPasswordResult.get())) {
                    String accountId = accOptional.get().getId(); // accountId từ Optional đã lấy ở trên
                    boolean success = accountDAO.changePassword(accountId, newPassword); //
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đổi mật khẩu thành công!");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đổi mật khẩu. Vui lòng thử lại.");
                    }
                } else if (confirmPasswordResult.isPresent()) {
                    showAlert(Alert.AlertType.WARNING, "Không khớp", "Mật khẩu xác nhận không khớp với mật khẩu mới.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Cơ sở dữ liệu", "Lỗi khi kết nối hoặc cập nhật cơ sở dữ liệu: " + ex.getMessage());
        }
    }

    private void handleLogout() {
        System.out.println("UserProfileView (" + this.hashCode() + "): Logging out...");
        if (getMainController() != null) {
            // Đóng cửa sổ hiện tại (cửa sổ chính của ứng dụng)
            Stage currentStage = (Stage) root.getScene().getWindow();
            currentStage.close();

            // Mở lại màn hình Login
            // Giả sử LoginUI.show(new Stage()) là cách bạn hiển thị màn hình login
            // Hoặc bạn có thể gọi một phương thức trong MainController để xử lý việc này
            // để tránh phụ thuộc trực tiếp vào LoginUI ở đây.
            // Tạm thời, chúng ta sẽ gọi trực tiếp để minh họa:
            LoginUI.show(new Stage()); // Cần import LoginUI và Stage

            // Gọi phương thức logout trong MainController để dọn dẹp session
            // và có thể điều hướng nội bộ nếu MainController quản lý trạng thái đăng nhập
            getMainController().logout(); // Phương thức này nên xóa currentUser và có thể làm gì đó khác
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất. MainController không tồn tại.");
        }
    }


    private void loadUserProfile() {
        System.out.println("UserProfileView (" + this.hashCode() + "): Attempting to load user profile data (loadUserProfile)...");
        Person currentUser = getCurrentUser();

        if (currentUser != null) {
            if (usernameLabelValue == null || roleLabelValue == null) {
                System.err.println("UserProfileView.loadUserProfile: LỖI - Labels (usernameLabelValue or roleLabelValue) is null.");
                if (usernameLabelValue == null) usernameLabelValue = new Label("Lỗi Label");
                if (roleLabelValue == null) roleLabelValue = new Label("");
            }
            final String userName = currentUser.getName();
            final String displayRole = determineUserRole(currentUser);

            Platform.runLater(() -> {
                System.out.println("UserProfileView.loadUserProfile (Platform.runLater): Setting user info - Name: " + userName + ", Role: " + displayRole);
                usernameLabelValue.setText(userName);
                roleLabelValue.setText(displayRole);
                System.out.println("UserProfileView.loadUserProfile (Platform.runLater): Labels updated on UI thread.");
            });

        } else {
            System.err.println("UserProfileView.loadUserProfile: currentUser is NULL. Cannot set profile information.");
            Platform.runLater(() -> {
                if (usernameLabelValue != null) usernameLabelValue.setText("N/A (User Null)");
                if (roleLabelValue != null) roleLabelValue.setText("N/A");
                System.out.println("UserProfileView.loadUserProfile (Platform.runLater): Labels set to N/A on UI thread.");
            });
        }
    }

    private String determineUserRole(Person user) {
        if (user == null) return "Không xác định";
        if (user instanceof Admin) {
            return "Quản trị viên";
        } else if (user instanceof Teacher) {
            return "Giáo viên";
        }
        else {
            return "Vai trò không xác định ("+ user.getClass().getSimpleName() +")";
        }
    }

    @Override
    public void refreshView() {
        super.refreshView();
        System.out.println("UserProfileView (" + this.hashCode() + "): Đang làm mới view (refreshView)...");

        if (getMainController() == null) {
            System.err.println("UserProfileView (" + this.hashCode() + "): LỖI - BaseScreenView.mainController (thông qua getMainController()) là NULL!");
            Platform.runLater(() -> {
                if(usernameLabelValue != null) usernameLabelValue.setText("Lỗi MC Null");
                if(roleLabelValue != null) roleLabelValue.setText("");
            });
            return;
        }
        System.out.println("UserProfileView (" + this.hashCode() + "): BaseScreenView.mainController (thông qua getMainController()) có hashCode: " + getMainController().hashCode());

        Person currentUser = getCurrentUser();
        if (currentUser == null) {
            System.err.println("UserProfileView (" + this.hashCode() + "): LỖI - currentUser (từ BaseScreenView.mainController.getCurrentUser()) là NULL!");
            if (usernameLabelValue == null || roleLabelValue == null) {
                System.err.println("UserProfileView (" + this.hashCode() + "): Labels (usernameLabelValue or roleLabelValue) is null BEFORE trying to set 'N/A (User Null)'.");
                ensureInitialized();
                if (usernameLabelValue == null) usernameLabelValue = new Label();
                if (roleLabelValue == null) roleLabelValue = new Label();
            }
            final Label finalUsernameLabelValue = usernameLabelValue;
            final Label finalRoleLabelValue = roleLabelValue;
            Platform.runLater(() -> {
                finalUsernameLabelValue.setText("N/A (User Null)");
                finalRoleLabelValue.setText("");
            });
        } else {
            System.out.println("UserProfileView (" + this.hashCode() + "): OK - Tìm thấy currentUser: " + currentUser.getName() + " (ID: " + currentUser.getId() + ", Role: " + currentUser.getClass().getSimpleName() + ")");
            if (usernameLabelValue == null || roleLabelValue == null) {
                System.err.println("UserProfileView (" + this.hashCode() + "): Labels chưa được khởi tạo khi refreshView được gọi (trước khi gọi loadUserProfile).");
                ensureInitialized();
                if (usernameLabelValue == null || roleLabelValue == null) {
                    System.err.println("UserProfileView (" + this.hashCode() + "): LỖI NGHIÊM TRỌNG - Labels vẫn null sau khi gọi ensureInitialized().");
                    return;
                }
            }
            loadUserProfile();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onActivate() {
        System.out.println("UserProfileView (" + this.hashCode() + "): View activated (onActivate).");
        super.onActivate();
    }

    @Override
    public void handleSystemMessage(String messageType, Object data) {
        super.handleSystemMessage(messageType, data);
        System.out.println("UserProfileView (" + this.hashCode() + ") nhận được thông điệp: " + messageType);
        if ("user_changed".equals(messageType) || "navigate_to_profile".equals(messageType)) {
            System.out.println("UserProfileView (" + this.hashCode() + "): User changed or navigated to profile, calling refreshView().");
            refreshView();
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}