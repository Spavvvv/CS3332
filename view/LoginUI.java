package view;

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
import src.model.person.Admin;
import src.model.person.Parent;
import src.model.person.Student;
import src.model.person.Teacher;
import src.model.person.Person;
import src.model.person.UserRole;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.prefs.Preferences;

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
    private final static String FILE_PATH = "C:\\Users\\tiend\\IdeaProjects\\CS3332";
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
        if (rememberMeCheck != null) {
            Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
            boolean rememberMe = prefs.getBoolean("rememberMe", false);

            if (rememberMe) {
                String savedUsername = prefs.get("savedUsername", "");
                usernameField.setText(savedUsername);
                rememberMeCheck.setSelected(true);
            }
        }
    }

    /**
     * Lưu thông tin đăng nhập
     * @param username Tên đăng nhập cần lưu
     * @param remember True nếu cần lưu lại, false nếu không
     */
    private void saveCredentials(String username, boolean remember) {
        Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
        prefs.putBoolean("rememberMe", remember);

        if (remember) {
            prefs.put("savedUsername", username);
        } else {
            prefs.remove("savedUsername");
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
        String password = passwordField.getText();
        boolean rememberMe = rememberMeCheck.isSelected();

        // Lưu thông tin đăng nhập nếu được chọn
        saveCredentials(username, rememberMe);

        // Kiểm tra đầu vào
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        // Đường dẫn đến file tài khoản
        String filePath = FILE_PATH + "\\" + username + ".txt";
        File file = new File(filePath);

        if (!file.exists()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Tên đăng nhập không tồn tại");
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");

                // Kiểm tra định dạng dữ liệu và xác thực mật khẩu
                if (parts.length >= 9 && parts[0].equals(username) && parts[1].equals(password)) {
                    // Đăng nhập thành công
                    System.out.println("Đăng nhập thành công!");

                    // Phân tích dữ liệu người dùng từ file
                    String userId = parts[8].trim(); // ID độc nhất
                    String fullName = parts[2].trim(); // Họ tên
                    String email = parts[3].trim(); // Email
                    String phone = parts[4].trim(); // Số điện thoại
                    String role = parts[5].trim(); // Vai trò
                    String dobString = parts[6].trim(); // Ngày sinh

                    // Lấy giới tính nếu có, mặc định là "Nam" nếu không có thông tin
                    String gender = parts.length > 7 ? parts[7].trim() : "Nam";

                    // Tạo đối tượng người dùng dựa trên vai trò
                    Person user = createPersonObject(role, userId, fullName, gender, phone, dobString, email);

                    // Tạo và hiển thị giao diện chính
                    launchMainUI(user, fullName);
                    return;
                }
            }
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Mật khẩu không đúng");
        } catch (FileNotFoundException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng nhập", "Không tìm thấy file tài khoản");
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Định dạng ngày tháng trong file tài khoản không hợp lệ");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Lỗi xảy ra khi đăng nhập: " + e.getMessage());
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

            // Tạo controllers và truyền thông tin người dùng đã đăng nhập
            NavigationController navigationController1 = new NavigationController(ui);
            MainController mainController1 = new MainController(ui, navigationController1);

            // Liên kết UI với các controller
            ui.setControllers(mainController1, navigationController1);

            // Truyền thông tin người dùng vào controller
            mainController1.setCurrentUser(user);

            // Thêm dòng này để truyền trực tiếp thông tin người dùng cho UI
            ui.setCurrentUser(user);

            // Tạo và hiển thị scene
            Scene uiScene = ui.createScene();
            uiStage.setScene(uiScene);
            uiStage.setTitle("Hệ thống quản lý trung tâm - " + fullName);
            uiStage.setMaximized(true); // Mở rộng cửa sổ

            // Xử lý sự kiện đóng cửa sổ
//            uiStage.setOnCloseRequest(event -> {
//                mainController1.onAppExit();
//            });

            uiStage.show();

            // Khởi động ứng dụng
            mainController1.onAppStart();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi khởi động",
                    "Không thể khởi động giao diện chính: " + e.getMessage());
            e.printStackTrace();

            // Mở lại màn hình đăng nhập trong trường hợp lỗi
            primaryStage.show();
        }
    }

    /**
     * Tạo đối tượng người dùng dựa trên vai trò
     * @param roleString Vai trò người dùng
     * @param userId ID người dùng
     * @param fullName Tên đầy đủ
     * @param gender Giới tính
     * @param phone Số điện thoại
     * @param dobString Ngày sinh (dạng chuỗi)
     * @param email Email
     * @return Đối tượng Person phù hợp với vai trò
     */
    private Person createPersonObject(String roleString, String userId, String fullName, String gender,
                                      String phone, String dobString, String email) {
        // Xác định vai trò
        switch (roleString.toLowerCase()) {
            case "admin":
                // Tạo đối tượng Admin
                String accessLevel = "1"; // Cấp độ truy cập mặc định
                return new Admin(userId, fullName, gender, phone, dobString, email, accessLevel);

            case "giáo viên":
                // Tạo đối tượng Teacher
                String teacherId = userId; // Sử dụng userId làm teacherId
                String position = "Giáo viên"; // Vị trí mặc định
                return new Teacher(userId, fullName, gender, phone, dobString, email, teacherId, position);

            case "học viên":
                // Tạo đối tượng Student với Parent rỗng
                return new Student(userId, fullName, gender, phone, dobString, email, null);

            case "phụ huynh":
                // Tạo đối tượng Parent
                String relationship = ""; // Mặc định mối quan hệ rỗng
                return new Parent(userId, fullName, gender, phone, dobString, email, relationship);

            default:
                // Nếu không xác định được vai trò, ném ngoại lệ
                throw new IllegalArgumentException("Vai trò không xác định: " + roleString);
        }
    }

    /**
     * Chuyển đến màn hình đăng ký
     */
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