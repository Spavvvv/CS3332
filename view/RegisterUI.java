package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import src.controller.MainController;
import src.controller.NavigationController;

import java.io.*;
import java.util.Scanner;

public class RegisterUI {
    private Stage primaryStage;
    private TextField usernameField;
    private TextField emailField;
    private TextField fullNameField;
    private TextField phoneField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private ComboBox<String> roleComboBox;
    private CheckBox termsCheckBox;
    private NavigationController navigationController;
    private MainController mainController;

    // File path for storing user accounts
    private String ACCOUNTS_FILE;
    private final static String FILE_PATH = "C:\\Users\\Admin\\Documents\\University\\CS3332\\CS3332\\UserAccount";

    public RegisterUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setupStage();
    }

    // Create accounts file if it doesn't exist
    private void createAccountsFileIfNotExists(String username) {
        ACCOUNTS_FILE = username + ".txt";
        File file = new File(FILE_PATH, ACCOUNTS_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
                System.out.println("Created new accounts file: " + ACCOUNTS_FILE);
            } catch (IOException e) {
                System.err.println("Error creating accounts file: " + e.getMessage());
            }
        }
    }

    // Check if username already exists in the file
    private boolean isUsernameTaken(String username) {
        ACCOUNTS_FILE = username + ".txt";
        try (Scanner scanner = new Scanner(new File(FILE_PATH,ACCOUNTS_FILE))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].trim().equalsIgnoreCase(username.trim())) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error checking username: " + e.getMessage());
        }
        return false;
    }

    // Check if email already exists in the file
    private boolean isEmailTaken(String email) {
        ACCOUNTS_FILE = usernameField.getText() + ".txt";
        try (Scanner scanner = new Scanner(new File(FILE_PATH,ACCOUNTS_FILE))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[3].trim().equalsIgnoreCase(email.trim())) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error checking email: " + e.getMessage());
        }
        return false;
    }

    // Save a new account to the file
    private void saveAccount(String username, String password, String fullName, String email, String phone, String role) {
        ACCOUNTS_FILE = usernameField.getText() + ".txt";
        File file = new File(FILE_PATH,ACCOUNTS_FILE);
        try (FileWriter writer = new FileWriter(file, true)) {
            // Format: username,password,fullName,email,phone,role
            String accountInfo = String.format("%s,%s,%s,%s,%s,%s%n",
                    username.trim(), password, fullName.trim(), email.trim(), phone.trim(), role.trim());
            writer.write(accountInfo);
            System.out.println("Account saved: " + username);
        } catch (IOException e) {
            System.err.println("Error saving account: " + e.getMessage());
        }
    }

    private void setupStage() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #f5f5f5;"); // Light theme background

        VBox registerCard = createRegisterCard();
        registerCard.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0.4)));

        root.getChildren().add(registerCard);
        Scene scene = new Scene(root, 800, 650);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Đăng ký - Hệ thống quản lý trung tâm");
        primaryStage.show();
    }

    private VBox createRegisterCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30, 50, 40, 50));
        card.setMaxWidth(500);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Logo
        HBox logoBox = new HBox();
        logoBox.setAlignment(Pos.CENTER);
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
            logo.setFitHeight(70);
            logo.setPreserveRatio(true);
            logoBox.getChildren().add(logo);
        } catch (Exception e) {
            Label logoText = new Label("AITALK");
            logoText.setFont(Font.font("Arial", FontWeight.BOLD, 22));
            logoText.setStyle("-fx-text-fill: #0091EA;");
            logoBox.getChildren().add(logoText);
        }

        // Tiêu đề
        Label title = new Label("ĐĂNG KÝ TÀI KHOẢN");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setStyle("-fx-text-fill: #333;");

        // Form đăng ký
        GridPane form = new GridPane();
        form.setVgap(15);
        form.setHgap(10);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(10, 0, 10, 0));

        // Họ và tên
        Label nameLabel = new Label("Họ và tên:");
        nameLabel.setStyle("-fx-text-fill: #555;");
        fullNameField = new TextField();
        fullNameField.setPromptText("Nhập họ và tên đầy đủ");
        fullNameField.setPrefHeight(35);
        styleTextField(fullNameField);

        // Tên đăng nhập
        Label userLabel = new Label("Tên đăng nhập:");
        userLabel.setStyle("-fx-text-fill: #555;");
        usernameField = new TextField();
        usernameField.setPromptText("Nhập tên đăng nhập");
        usernameField.setPrefHeight(35);
        styleTextField(usernameField);

        // Real-time username validation
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !usernameField.getText().isEmpty()) { // When focus is lost and field is not empty
                if (isUsernameTaken(usernameField.getText())) {
                    usernameField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                    showTooltip(usernameField, "Tên đăng nhập đã tồn tại");
                } else {
                    usernameField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                }
            }
        });

        // Email
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-text-fill: #555;");
        emailField = new TextField();
        emailField.setPromptText("Nhập địa chỉ email");
        emailField.setPrefHeight(35);
        styleTextField(emailField);

        // Real-time email validation
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !emailField.getText().isEmpty()) { // When focus is lost and field is not empty
                if (!isValidEmail(emailField.getText())) {
                    emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                    showTooltip(emailField, "Email không đúng định dạng");
                } else if (isEmailTaken(emailField.getText())) {
                    emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                    showTooltip(emailField, "Email đã được sử dụng");
                } else {
                    emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                }
            }
        });

        // Số điện thoại
        Label phoneLabel = new Label("Số điện thoại:");
        phoneLabel.setStyle("-fx-text-fill: #555;");
        phoneField = new TextField();
        phoneField.setPromptText("Nhập số điện thoại");
        phoneField.setPrefHeight(35);
        styleTextField(phoneField);

        // Real-time phone validation
        phoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !phoneField.getText().isEmpty()) { // When focus is lost and field is not empty
                if (!isValidPhone(phoneField.getText())) {
                    phoneField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                    showTooltip(phoneField, "Số điện thoại không đúng định dạng");
                } else {
                    phoneField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                }
            }
        });

        // Vai trò
        Label roleLabel = new Label("Vai trò:");
        roleLabel.setStyle("-fx-text-fill: #555;");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Học viên", "Giáo viên", "Phụ huynh");
        roleComboBox.setValue("Học viên");
        roleComboBox.setPrefHeight(35);
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");

        // Mật khẩu
        Label passLabel = new Label("Mật khẩu:");
        passLabel.setStyle("-fx-text-fill: #555;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu (ít nhất 8 ký tự)");
        passwordField.setPrefHeight(35);
        styleTextField(passwordField);

        // Real-time password validation
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                if (newVal.length() < 8) {
                    passwordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                } else {
                    passwordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                }
            } else {
                styleTextField(passwordField);
            }
        });

        // Xác nhận mật khẩu
        Label confirmPassLabel = new Label("Xác nhận mật khẩu:");
        confirmPassLabel.setStyle("-fx-text-fill: #555;");
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Nhập lại mật khẩu");
        confirmPasswordField.setPrefHeight(35);
        styleTextField(confirmPasswordField);

        // Real-time confirm password validation
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !passwordField.getText().isEmpty()) {
                if (!newVal.equals(passwordField.getText())) {
                    confirmPasswordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                } else {
                    confirmPasswordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                }
            } else {
                styleTextField(confirmPasswordField);
            }
        });

        // Đồng ý điều khoản
        termsCheckBox = new CheckBox("Tôi đồng ý với các điều khoản và điều kiện sử dụng");
        termsCheckBox.setStyle("-fx-text-fill: #555;");

        // Hyperlink điều khoản
        Hyperlink termsLink = new Hyperlink("Xem điều khoản sử dụng");
        termsLink.setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
        termsLink.setOnAction(e -> showTermsDialog());

        // Nút đăng ký
        Button registerBtn = new Button("ĐĂNG KÝ");
        registerBtn.setStyle("-fx-background-color: #0091EA; -fx-text-fill: white; -fx-font-weight: bold;");
        registerBtn.setPrefHeight(40);
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> handleRegister());

        // Hiệu ứng hover cho nút
        registerBtn.setOnMouseEntered(e -> registerBtn.setStyle("-fx-background-color: #0077c1; -fx-text-fill: white; -fx-font-weight: bold;"));
        registerBtn.setOnMouseExited(e -> registerBtn.setStyle("-fx-background-color: #0091EA; -fx-text-fill: white; -fx-font-weight: bold;"));

        // Thêm các thành phần vào form (2 cột)
        // Cột 1
        form.add(nameLabel, 0, 0);
        form.add(fullNameField, 0, 1);
        form.add(userLabel, 0, 2);
        form.add(usernameField, 0, 3);
        form.add(emailLabel, 0, 4);
        form.add(emailField, 0, 5);
        // Cột 2
        form.add(phoneLabel, 1, 0);
        form.add(phoneField, 1, 1);
        form.add(roleLabel, 1, 2);
        form.add(roleComboBox, 1, 3);
        form.add(passLabel, 1, 4);
        form.add(passwordField, 1, 5);

        // Row span toàn bộ chiều rộng form
        form.add(confirmPassLabel, 0, 6, 2, 1);
        form.add(confirmPasswordField, 0, 7, 2, 1);
        form.add(termsCheckBox, 0, 8, 2, 1);
        form.add(termsLink, 0, 9, 2, 1);
        form.add(registerBtn, 0, 10, 2, 1);

        // Cài đặt chiều rộng cột
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(50);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(50);
        form.getColumnConstraints().addAll(column1, column2);

        // Đã có tài khoản
        HBox loginBox = new HBox();
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setSpacing(5);
        Label loginLabel = new Label("Đã có tài khoản?");
        loginLabel.setStyle("-fx-text-fill: #555;");
        Hyperlink loginLink = new Hyperlink("Đăng nhập ngay");
        loginLink.setStyle("-fx-text-fill: #0091EA; -fx-border-color: transparent;");
        loginLink.setOnAction(e -> returnToLogin());
        loginBox.getChildren().addAll(loginLabel, loginLink);

        card.getChildren().addAll(logoBox, title, form, loginBox);

        return card;
    }

    private void styleTextField(TextField textField) {
        textField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");
    }

    private void showTooltip(Control control, String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        Tooltip.install(control, tooltip);

        // Show the tooltip immediately
        tooltip.show(control,
                control.localToScreen(control.getBoundsInLocal()).getMinX(),
                control.localToScreen(control.getBoundsInLocal()).getMaxY());

        // Hide after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> tooltip.hide());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleRegister() {
        // Kiểm tra thông tin đăng ký
        if (fullNameField.getText().isEmpty() ||
                usernameField.getText().isEmpty() ||
                emailField.getText().isEmpty() ||
                phoneField.getText().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty()) {

            showAlert("Lỗi đăng ký", "Vui lòng điền đầy đủ thông tin đăng ký");
            return;
        }

        // Kiểm tra mật khẩu
        if (passwordField.getText().length() < 8) {
            showAlert("Lỗi đăng ký", "Mật khẩu phải có ít nhất 8 ký tự");
            return;
        }

        // Kiểm tra mật khẩu khớp nhau
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Lỗi đăng ký", "Mật khẩu và xác nhận mật khẩu không khớp");
            return;
        }

        // Kiểm tra đồng ý điều khoản
        if (!termsCheckBox.isSelected()) {
            showAlert("Lỗi đăng ký", "Bạn phải đồng ý với điều khoản sử dụng");
            return;
        }

        // Kiểm tra định dạng email
        if (!isValidEmail(emailField.getText())) {
            showAlert("Lỗi đăng ký", "Email không hợp lệ");
            return;
        }

        // Kiểm tra định dạng số điện thoại
        if (!isValidPhone(phoneField.getText())) {
            showAlert("Lỗi đăng ký", "Số điện thoại không hợp lệ");
            return;
        }

        String username = usernameField.getText();
        // Kiểm tra tên đăng nhập đã tồn tại
        if (isUsernameTaken(username)) {
            showAlert("Lỗi đăng ký", "Tên đăng nhập đã tồn tại trong hệ thống");
            usernameField.requestFocus();
            return;
        }

        // Kiểm tra email đã tồn tại
        if (isEmailTaken(emailField.getText())) {
            showAlert("Lỗi đăng ký", "Email đã được sử dụng trong hệ thống");
            emailField.requestFocus();
            return;
        }

        // Sau khi đã kiểm tra hết, thì mới tạo file
        createAccountsFileIfNotExists(username);
        // Lưu tài khoản mới vào file
        saveAccount(
                username,
                passwordField.getText(),
                fullNameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                roleComboBox.getValue()
        );

        // Hiển thị thông báo đăng ký thành công
        showSuccessDialog();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private boolean isValidPhone(String phone) {
        //Chỉ cần là number thì valid
        String phoneRegex = "^[0-9]+$";
        return phone.matches(phoneRegex);
    }

    private void showSuccessDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng ký thành công");
        alert.setHeaderText(null);
        alert.setContentText("Chúc mừng! Bạn đã đăng ký tài khoản thành công.\nVui lòng đăng nhập để sử dụng hệ thống.");

        // Thêm nút đến trang đăng nhập
        ButtonType loginButton = new ButtonType("Đến trang đăng nhập");
        alert.getButtonTypes().setAll(loginButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == loginButton) {
                returnToLogin();
            }
        });
    }

    private void showTermsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Điều khoản sử dụng");
        alert.setHeaderText("Điều khoản và điều kiện sử dụng hệ thống");

        String terms = "1. Bạn phải cung cấp thông tin chính xác khi đăng ký.\n\n" +
                "2. Bạn chịu trách nhiệm bảo mật tài khoản của mình.\n\n" +
                "3. Bạn không được sử dụng hệ thống cho các mục đích bất hợp pháp.\n\n" +
                "4. Chúng tôi có quyền từ chối dịch vụ nếu bạn vi phạm điều khoản.\n\n" +
                "5. Thông tin cá nhân của bạn sẽ được bảo mật theo chính sách riêng tư.\n\n" +
                "6. Chúng tôi có thể thay đổi điều khoản này mà không cần thông báo trước.";

        TextArea textArea = new TextArea(terms);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(300);
        textArea.setPrefWidth(400);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void returnToLogin() {
        primaryStage.close();
        Stage loginStage = new Stage();
        LoginUI loginUI = new LoginUI(loginStage);
        loginUI.setControllers(mainController, navigationController);
    }

    public void setControllers(MainController mainController, NavigationController navigationController) {
        this.mainController = mainController;
        this.navigationController = navigationController;
    }

    public static void show(Stage primaryStage) {
        new RegisterUI(primaryStage);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
