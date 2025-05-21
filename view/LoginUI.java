package view;
import src.dao.NotificationDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import src.controller.MainController;
import src.controller.NavigationController;
import src.model.person.Person;
import src.dao.AccountDAO;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.event.EventHandler;
import java.util.prefs.Preferences;
import src.model.Notification.NotificationService; // THÊM DÒNG NÀY

/**
 * Giao diện đăng nhập của ứng dụng
 */
public class LoginUI {
    private Stage primaryStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox rememberMeCheck;
    private String currentTheme = "light"; // Mặc định là theme sáng
    private NavigationController navigationController;
    private MainController mainController;
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Constructor với Stage chính
     * @param primaryStage Stage chính của ứng dụng
     */
    public LoginUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadSavedTheme();
        setupStage();
        loadSavedCredentials();
    }

    /**
     * Tải theme đã lưu
     */
    private void loadSavedTheme() {
        Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
        currentTheme = prefs.get("theme", "light");
    }

    /**
     * Lưu theme đã chọn
     * @param theme Theme cần lưu
     */
    private void saveTheme(String theme) {
        Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
        prefs.put("theme", theme);
    }

    /**
     * Tải thông tin đăng nhập đã lưu
     */
    private void loadSavedCredentials() {
        if (rememberMeCheck != null && usernameField != null && passwordField != null) { // Thêm kiểm tra null cho passwordField
            Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
            boolean rememberMe = prefs.getBoolean("rememberMe", false);

            if (rememberMe) {
                String savedUsername = prefs.get("savedUsername", "");
                String savedPassword = prefs.get("savedPassword", ""); // Lấy mật khẩu đã lưu
                usernameField.setText(savedUsername);
                passwordField.setText(savedPassword); // Điền mật khẩu đã lưu
                rememberMeCheck.setSelected(true);
            }
        }
    }

    /**
     * Lưu thông tin đăng nhập
     * @param username Tên đăng nhập cần lưu
     * @param remember True nếu cần lưu lại, false nếu không
     */
    private void saveCredentials(String username, String password, boolean remember) { // Thêm tham số password
        Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
        prefs.putBoolean("rememberMe", remember);

        if (remember) {
            prefs.put("savedUsername", username);
            prefs.put("savedPassword", password); // Lưu mật khẩu
        } else {
            prefs.remove("savedUsername");
            prefs.remove("savedPassword"); // Xóa mật khẩu đã lưu
        }
    }

    /**
     * Thiết lập Stage chính
     */
    private void setupStage() {
        StackPane root = new StackPane();
        applyTheme(root, currentTheme); // Áp dụng theme ban đầu
        VBox loginCard = createLoginCard();
        loginCard.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0.4)));
        root.getChildren().add(loginCard);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Đăng nhập - Hệ thống quản lý trung tâm");
        primaryStage.show();
    }

    /**
     * Tạo card đăng nhập
     * @return VBox chứa các thành phần đăng nhập
     */
    private VBox createLoginCard() {
        VBox card = new VBox(30);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 50, 50, 50));
        card.setMaxWidth(400);
        applyCardStyle(currentTheme, card); // Áp dụng style ban đầu cho card

        // Logo
        HBox logoBox = new HBox();
        logoBox.setAlignment(Pos.CENTER);
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
            logo.setFitHeight(80);
            logo.setPreserveRatio(true);
            logoBox.getChildren().add(logo);
        } catch (Exception e) {
            Label logoText = new Label("AITALK");
            logoText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            logoText.setStyle("-fx-text-fill: #0091EA;");
            logoBox.getChildren().add(logoText);
        }

        // Tiêu đề
        Label title = new Label("ĐĂNG NHẬP HỆ THỐNG");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #333;");

        // Form đăng nhập
        GridPane form = new GridPane();
        form.setVgap(15);
        form.setHgap(10);
        form.setAlignment(Pos.CENTER);

        // Ô username
        Label userLabel = new Label("Tên đăng nhập:");
        userLabel.setStyle("-fx-text-fill: #555;");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập");
        usernameField.setPrefHeight(40);
        usernameField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");
        // Ô password
        Label passLabel = new Label("Mật khẩu:");
        passLabel.setStyle("-fx-text-fill: #555;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu");
        passwordField.setPrefHeight(40);
        passwordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");

        // Quên mật khẩu
        Hyperlink forgotPass = new Hyperlink("Quên mật khẩu?");
        forgotPass.setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
        forgotPass.setOnAction(e -> showForgotPasswordDialog());

        // Ghi nhớ đăng nhập
        rememberMeCheck = new CheckBox("Ghi nhớ đăng nhập");
        rememberMeCheck.setStyle("-fx-text-fill: #555;");
        EventHandler<KeyEvent> enterKeyHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        };
        usernameField.setOnKeyPressed(enterKeyHandler);
        passwordField.setOnKeyPressed(enterKeyHandler);

        // Nút đăng nhập
        Button loginBtn = new Button("ĐĂNG NHẬP");
        loginBtn.setStyle("-fx-background-color: #0091EA; -fx-text-fill: white; -fx-font-weight: bold;");
        loginBtn.setPrefHeight(45);
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin());

        // Hiệu ứng hover cho nút
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle("-fx-background-color: #0077c1; -fx-text-fill: white; -fx-font-weight: bold;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle("-fx-background-color: #0091EA; -fx-text-fill: white; -fx-font-weight: bold;"));

        // Thêm các thành phần vào form
        form.add(userLabel, 0, 0);
        form.add(usernameField, 0, 1);
        form.add(passLabel, 0, 2);
        form.add(passwordField, 0, 3);
        form.add(forgotPass, 0, 4);
        form.add(rememberMeCheck, 0, 5);
        form.add(loginBtn, 0, 6);

        // Theme options
        HBox themeBox = createThemeBox(card);

        // Đăng ký tài khoản mới
        HBox registerBox = new HBox();
        registerBox.setAlignment(Pos.CENTER);
        Label registerLabel = new Label("Chưa có tài khoản? ");
        registerLabel.setStyle("-fx-text-fill: #555;");
        Hyperlink registerLink = new Hyperlink("Đăng ký ngay");
        registerLink.setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> gotoRegister());
        registerBox.getChildren().addAll(registerLabel, registerLink);

        card.getChildren().addAll(logoBox, title, form, themeBox, registerBox);

        return card;
    }

    /**
     * Hiển thị dialog quên mật khẩu
     */
    private void showForgotPasswordDialog() {
        // Tạo dialog quên mật khẩu
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Quên mật khẩu");
        dialog.setHeaderText("Nhập email hoặc tên đăng nhập của bạn");

        // Thiết lập các nút
        ButtonType resetButton = new ButtonType("Đặt lại mật khẩu", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButton, cancelButton);

        // Tạo grid và thêm các trường
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("Email hoặc tên đăng nhập");

        grid.add(new Label("Email hoặc tên đăng nhập:"), 0, 0);
        grid.add(emailField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // Kích hoạt nút đặt lại mật khẩu khi có dữ liệu
        Node resetButtonNode = dialog.getDialogPane().lookupButton(resetButton);
        resetButtonNode.setDisable(true);

        // Sử dụng cách thức đơn giản hơn để tránh lỗi với generic type
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            resetButtonNode.setDisable(newValue.trim().isEmpty());
        });

        // Xử lý kết quả
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButton) {
                return emailField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            // Gửi hướng dẫn đặt lại mật khẩu (mô phỏng)
            showAlert(Alert.AlertType.INFORMATION, "Đặt lại mật khẩu",
                    "Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn (nếu tài khoản tồn tại).");
        });
    }

    /**
     * Tạo hộp chọn theme
     * @param card Card chính cần áp dụng theme
     * @return HBox chứa các tùy chọn theme
     */
    private HBox createThemeBox(VBox card) {
        HBox themeBox = new HBox(15);
        themeBox.setAlignment(Pos.CENTER);
        Label themeLabel = new Label("Giao diện:");
        themeLabel.setStyle("-fx-text-fill: #555;");

        // Màu sắc có sẵn
        String[] themes = {"light", "dark", "blue", "purple", "green", "orange", "red"};
        Color[] colors = {Color.WHITE, Color.rgb(30, 30, 40), Color.DODGERBLUE, Color.MEDIUMPURPLE, Color.FORESTGREEN, Color.DARKORANGE, Color.FIREBRICK};

        // Tạo các ô màu
        HBox colorPalette = new HBox(5); // Khoảng cách giữa các ô màu
        for (int i = 0; i < themes.length; i++) {
            String theme = themes[i];
            Color color = colors[i];

            Circle colorCircle = new Circle(10); // Bán kính hình tròn
            colorCircle.setFill(color);
            colorCircle.setStroke(Color.LIGHTGRAY); // Viền trắng
            colorCircle.setStrokeWidth(2); // Độ dày viền
            colorCircle.setStyle("-fx-cursor: hand;"); // Hiển thị con trỏ tay khi di chuột qua

            // Đánh dấu theme hiện tại
            if (theme.equals(currentTheme)) {
                colorCircle.setStroke(Color.BLACK);
                colorCircle.setStrokeWidth(3);
            }

            // Xử lý sự kiện khi nhấp vào ô màu
            String finalTheme = theme;
            colorCircle.setOnMouseClicked(e -> {
                currentTheme = finalTheme;
                saveTheme(currentTheme); // Lưu theme
                applyTheme(primaryStage.getScene().getRoot(), currentTheme);
                applyCardStyle(currentTheme, card);

                // Cập nhật viền đánh dấu theme hiện tại
                for (javafx.scene.Node node : colorPalette.getChildren()) {
                    if (node instanceof Circle) {
                        ((Circle) node).setStroke(Color.LIGHTGRAY);
                        ((Circle) node).setStrokeWidth(2);
                    }
                }
                colorCircle.setStroke(Color.BLACK);
                colorCircle.setStrokeWidth(3);
            });

            colorPalette.getChildren().add(colorCircle);
        }

        themeBox.getChildren().addAll(themeLabel, colorPalette);
        return themeBox;
    }

    /**
     * Áp dụng theme cho root
     * @param root Root của scene
     * @param theme Theme cần áp dụng
     */
    private void applyTheme(javafx.scene.Parent root, String theme) {
        String backgroundColor = "#f5f5f5"; // Default light background
        String textColor = "#333"; // Default text color

        switch (theme) {
            case "dark":
                backgroundColor = "#2d2d2d";
                textColor = "#fff";
                break;
            case "blue":
                backgroundColor = "#e0ffff";
                textColor = "#00008b";
                break;
            case "purple":
                backgroundColor = "#e6e6ff";
                textColor = "#4b0082";
                break;
            case "green":
                backgroundColor = "#f0fff0";
                textColor = "#006400";
                break;
            case "orange":
                backgroundColor = "#faebd7";
                textColor = "#a0522d";
                break;
            case "red":
                backgroundColor = "#ffe4e1";
                textColor = "#8b0000";
                break;
        }

        root.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + ";");
    }

    /**
     * Áp dụng style cho card đăng nhập
     * @param theme Theme cần áp dụng
     * @param card Card cần áp dụng style
     */
    private void applyCardStyle(String theme, VBox card) {
        String cardColor = "white";
        String textColor = "#555"; // Màu chữ mặc định
        switch (theme) {
            case "dark":
                cardColor = "#333";
                textColor = "white"; // Chuyển chữ sang màu trắng khi theme là dark
                break;
            case "blue":
                cardColor = "#f0ffff";
                break;
            case "purple":
                cardColor = "#f0e6ff";
                break;
            case "green":
                cardColor = "#f5fffa";
                break;
            case "orange":
                cardColor = "#fffaf0";
                break;
            case "red":
                cardColor = "#fff0f5";
                break;
        }
        card.setStyle("-fx-background-color: " + cardColor + "; -fx-background-radius: 10;");

        // Áp dụng màu chữ cho các label trong card
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label) {
                ((Label) node).setStyle("-fx-text-fill: " + textColor + ";");
            } else if (node instanceof GridPane) {
                GridPane form = (GridPane) node;
                for (javafx.scene.Node formNode : form.getChildren()) {
                    if (formNode instanceof Label) {
                        ((Label) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof TextField) {
                        ((TextField) formNode).setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof PasswordField) {
                        ((PasswordField) formNode).setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof Hyperlink) {
                        ((Hyperlink) formNode).setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
                    } else if (formNode instanceof CheckBox) {
                        ((CheckBox) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    }
                }
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setStyle("-fx-text-fill: " + textColor + ";");
            } else if (node instanceof Hyperlink) {
                ((Hyperlink) node).setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
            } else if (node instanceof HBox) {
                for (javafx.scene.Node hboxNode : ((HBox) node).getChildren()) {
                    if (hboxNode instanceof Label) {
                        ((Label) hboxNode).setStyle("-fx-text-fill: " + textColor + ";");
                    }
                }
            }
        }
    }

    /**
     * Xử lý đăng nhập
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim(); // Lấy mật khẩu từ trường passwordField
        boolean rememberMe = rememberMeCheck.isSelected();

        // Kiểm tra nếu username hoặc password rỗng
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        try {
            // Sử dụng DAO để xác thực
            AccountDAO accountDAO = new AccountDAO();
            Optional<Person> userOptional = accountDAO.authenticate(username, password);

            if (userOptional.isPresent()) {
                // Đăng nhập thành công
                Person user = userOptional.get();
                System.out.println("Đăng nhập thành công với vai trò: " + user.getRole());

                // Lưu thông tin đăng nhập nếu cần
                saveCredentials(username, password, rememberMe); // Truyền cả password vào hàm saveCredentials

                // Gọi giao diện chính
                launchMainUI(user, user.getName());
            } else {
                // Đăng nhập thất bại
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tên đăng nhập hoặc mật khẩu không đúng");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đã xảy ra lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Khởi chạy giao diện chính sau khi đăng nhập thành công
     *
     * @param user Đối tượng người dùng đã đăng nhập
     * @param fullName Tên đầy đủ của người dùng
     */
    private void launchMainUI(Person user, String fullName) {
        try {
            // Đóng màn hình đăng nhập
            primaryStage.close();

            // Khởi tạo và hiển thị UI chính
            Stage uiStage = new Stage();
            UI ui = new UI();
            NotificationDAO notificationDAO = new NotificationDAO(); // Giả sử NotificationDAO có constructor mặc định và tự quản lý kết nối
            NotificationService notificationService = new NotificationService(notificationDAO); // Truyền DAO vào service

            navigationController = new NavigationController(ui);
            mainController = new MainController(ui, navigationController);


            // Liên kết UI với các controller
            ui.setControllers(mainController, navigationController, notificationService);

            // Truyền thông tin người dùng vào controller
            mainController.setCurrentUser(user);

            // Thêm dòng này để truyền trực tiếp thông tin người dùng cho UI
            ui.setCurrentUser(user);

            // Tạo và hiển thị scene
            Scene uiScene = ui.createScene();
            uiStage.setScene(uiScene);
            uiStage.setTitle("Hệ thống quản lý trung tâm - " + fullName);
            uiStage.setMaximized(true); // Mở rộng cửa sổ

            // Xử lý sự kiện đóng cửa sổ
            uiStage.setOnCloseRequest(event -> {
                mainController.onAppExit();
            });

            uiStage.show();

            // Khởi động ứng dụng
            mainController.onAppStart();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi khởi động",
                    "Không thể khởi động giao diện chính: " + e.getMessage());
            e.printStackTrace();

            // Mở lại màn hình đăng nhập trong trường hợp lỗi
            primaryStage.show();
        }
    }

    private void gotoRegister() {
        primaryStage.close();
        Stage registerStage = new Stage();
        RegisterUI registerUI = new RegisterUI(registerStage);
        registerUI.setControllers(mainController, navigationController);
    }

    /**
     * Thiết lập controllers
     * @param mainController Controller chính
     * @param navigationController Controller điều hướng
     */
    public void setControllers(MainController mainController, NavigationController navigationController) {
        this.mainController = mainController;
        this.navigationController = navigationController;
    }

    /**
     * Phương thức hiển thị màn hình đăng nhập
     * @param primaryStage Stage chính
     */
    public static void show(Stage primaryStage) {
        new LoginUI(primaryStage);
    }

    /**
     * Hiển thị cảnh báo
     * @param alertType Loại cảnh báo
     * @param title Tiêu đề cảnh báo
     * @param message Nội dung cảnh báo
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}