package src.view.components.Screen;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.prefs.Preferences;

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
import javafx.util.StringConverter;

import src.controller.MainController;
import src.controller.NavigationController;
import src.dao.Accounts.RegisterDAO;


public class RegisterUI {
    private Stage primaryStage;
    private StackPane root;
    private VBox registerCard;
    private TextField usernameField;
    private TextField passwordField;
    private TextField dobTextField;
    private TextField emailField;
    private TextField fullNameField;
    private TextField phoneField;
    private DatePicker dobPicker; //
    private PasswordField confirmPasswordField;
    private ComboBox roleComboBox;
    private CheckBox termsCheckBox;
    private NavigationController navigationController;
    private MainController mainController;
    private ComboBox genderComboBox;
    private String currentTheme = "light"; // Mặc định là theme sáng
    private RegisterDAO registerDAO;
    // Định dạng ngày tháng
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public RegisterUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadThemePreference(); // Tải theme đã lưu
        setupStage();
    }
    // Tải theme từ preferences
    private void loadThemePreference() {
        Preferences prefs = Preferences.userNodeForPackage(LoginUI.class);
        currentTheme = prefs.get("theme", "light");
    }
    // Tạo ID ngẫu nhiên 6 chữ số dựa trên vai trò
    private String generateUniqueID(String role) {
        // Xác định số đầu tiên dựa trên vai trò
        String firstDigit;
        if (role.equals("Giáo viên")) {
            firstDigit = "1";
        } else { // Admin
            firstDigit = "0";
        }
        // Tạo 5 chữ số còn lại ngẫu nhiên
        StringBuilder randomDigits = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            randomDigits.append(random.nextInt(10));
        }
        return firstDigit + randomDigits.toString();
    }
    private void setupStage() {
        root = new StackPane();
        applyTheme(root, currentTheme); // Áp dụng theme
        registerCard = createRegisterCard();
        registerCard.setEffect(new javafx.scene.effect.DropShadow(10, Color.gray(0.4)));
        applyCardStyle(currentTheme, registerCard); // Áp dụng style cho card theo theme
        root.getChildren().add(registerCard);
        Scene scene = new Scene(root, 800, 700);
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

        // Date of Birth - Cải tiến với kết hợp DatePicker và TextField
        Label dobLabel = new Label("Ngày sinh:");
        dobLabel.setStyle("-fx-text-fill: #555;");

        // Tạo HBox để chứa cả DatePicker và TextField
        HBox dobInputBox = new HBox(5);

        // TextField cho nhập tay
        dobTextField = new TextField();
        dobTextField.setPromptText("DD/MM/YYYY");
        dobTextField.setPrefHeight(35);
        HBox.setHgrow(dobTextField, Priority.ALWAYS); // Cho phép mở rộng để lấp đầy không gian
        styleTextField(dobTextField);

        // DatePicker cho chọn từ lịch
        dobPicker = new DatePicker();
        dobPicker.setPrefHeight(35);
        dobPicker.setPrefWidth(40);
        // Ẩn phần text, chỉ hiển thị nút lịch
        dobPicker.getEditor().setVisible(false);
        dobPicker.getEditor().setManaged(false);
        dobPicker.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd;");

        // Định dạng date picker để hiển thị theo format DD/MM/YYYY
        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return DATE_FORMATTER.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        };
        dobPicker.setConverter(converter);

        // Khi chọn từ date picker, cập nhật giá trị vào text field
        dobPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dobTextField.setText(DATE_FORMATTER.format(newVal));
                validateDateInput();
            }
        });

        // Khi nhập tay, cập nhật giá trị vào date picker
        dobTextField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Khi mất focus
                validateDateInput();
            }
        });

        // Thêm vào HBox
        dobInputBox.getChildren().addAll(dobTextField, dobPicker);

        // Vai trò
        Label roleLabel = new Label("Vai trò:");
        roleLabel.setStyle("-fx-text-fill: #555;");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Admin", "Giáo viên");
        roleComboBox.setValue("Giáo viên");
        roleComboBox.setEditable(false);
        roleComboBox.setPrefHeight(35);
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");

        // Giới tính
        Label genderLabel = new Label("Giới tính:");
        genderLabel.setStyle("-fx-text-fill: #555;");
        genderComboBox = new ComboBox<>();
        genderComboBox.getItems().addAll("Nam", "Nữ"); // Các tùy chọn giới tính
        genderComboBox.setPromptText("Chọn giới tính"); // Placeholder
        genderComboBox.setPrefHeight(35);
        genderComboBox.setMaxWidth(Double.MAX_VALUE);
        genderComboBox.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ddd; -fx-text-fill: #555;");

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
        // Cột 0
        form.add(nameLabel, 0, 0);
        form.add(fullNameField, 0, 1);
        form.add(userLabel, 0, 2);
        form.add(usernameField, 0, 3);
        form.add(emailLabel, 0, 4);
        form.add(emailField, 0, 5);
        form.add(dobLabel, 0, 6);
        form.add(dobInputBox, 0, 7);

        // Cột 1 - ĐÃ SỬA VỊ TRÍ HÀNG CHO CÁC THÀNH PHẦN PHÍA DƯỚI GENDER
        form.add(phoneLabel, 1, 0);
        form.add(phoneField, 1, 1);
        form.add(roleLabel, 1, 2);
        form.add(roleComboBox, 1, 3);

        // Vị trí mới cho Giới tính
        form.add(genderLabel, 1, 4); // Hàng 4, cột 1
        form.add(genderComboBox, 1, 5); // Hàng 5, cột 1

        // Vị trí mới cho Mật khẩu (đẩy xuống 2 hàng so với code cũ)
        form.add(passLabel, 1, 6); // Hàng 6, cột 1
        form.add(passwordField, 1, 7); // Hàng 7, cột 1

        // Vị trí mới cho Xác nhận mật khẩu (đẩy xuống 2 hàng so với code cũ)
        form.add(confirmPassLabel, 1, 8); // Hàng 8, cột 1
        form.add(confirmPasswordField, 1, 9); // Hàng 9, cột 1

        // Row span toàn bộ chiều rộng form (điều chỉnh số hàng bắt đầu)
        form.add(termsCheckBox, 0, 10, 2, 1); // Bắt đầu từ hàng 10
        form.add(termsLink, 0, 11, 2, 1);   // Bắt đầu từ hàng 11
        form.add(registerBtn, 0, 12, 2, 1);  // Bắt đầu từ hàng 12


        // Cài đặt chiều rộng cột (giữ nguyên)
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(50);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setPercentWidth(50);
        form.getColumnConstraints().addAll(column1, column2);

        // Đã có tài khoản (giữ nguyên)
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


    private boolean isUsernameTaken(String username) {
        RegisterDAO registerDAO = new RegisterDAO(); // Sử dụng DAO
        return registerDAO.isUsernameExists(username);
    }
    private boolean isEmailTaken(String email) {
        RegisterDAO registerDAO = new RegisterDAO(); // Sử dụng DAO
        return registerDAO.isEmailExists(email);
    }
    // Kiểm tra và xác thực đầu vào ngày tháng
    private void validateDateInput() {
        String dateText = dobTextField.getText().trim();
        try {
            if (!dateText.isEmpty()) {
                LocalDate date = LocalDate.parse(dateText, DATE_FORMATTER);
                if (date.isAfter(LocalDate.now())) {
                    dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                    showTooltip(dobTextField, "Ngày sinh không hợp lệ");
                } else {
                    dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
                    dobPicker.setValue(date);
                }
            }
        } catch (DateTimeParseException e) {
            dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(dobTextField, "Định dạng ngày không hợp lệ (DD/MM/YYYY)");
        }
    }
    private void applyTheme(javafx.scene.Parent parent, String theme) {
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
        parent.setStyle("-fx-background-color: " + backgroundColor + "; -fx-text-fill: " + textColor + ";");
    }
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
                        ((TextField) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof PasswordField) {
                        ((PasswordField) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof Hyperlink) {
                        ((Hyperlink) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof CheckBox) {
                        ((CheckBox) formNode).setStyle("-fx-text-fill: " + textColor + ";");
                    } else if (formNode instanceof HBox) {
                        for (javafx.scene.Node hboxNode : ((HBox) formNode).getChildren()) {
                            if (hboxNode instanceof Label) {
                                ((Label) hboxNode).setStyle("-fx-text-fill: " + textColor + ";");
                            } else if (hboxNode instanceof TextField) {
                                ((TextField) hboxNode).setStyle("-fx-text-fill: " + textColor + ";");
                            }
                        }
                    }
                }
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setStyle("-fx-text-fill: " + textColor + ";");
            } else if (node instanceof Hyperlink) {
                ((Hyperlink) node).setStyle("-fx-text-fill: " + textColor + ";");
            } else if (node instanceof HBox) {
                for (javafx.scene.Node hboxNode : ((HBox) node).getChildren()) {
                    if (hboxNode instanceof Label) {
                        ((Label) hboxNode).setStyle("-fx-text-fill: " + textColor + ";");
                    }
                }
            }
        }
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
        // 1. Get all data from form fields
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String role = roleComboBox.getValue().toString(); // Assumes ComboBox<String>
        String selectedGender = genderComboBox.getValue().toString(); // Assumes ComboBox<String>
        String dobText = dobTextField.getText().trim();

        // 2. Perform comprehensive client-side validation first
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || fullName.isEmpty() || email.isEmpty() || phone.isEmpty()
                || role == null || selectedGender == null || selectedGender.trim().isEmpty()
                || dobText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Vui lòng điền đầy đủ tất cả thông tin bắt buộc.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Mật khẩu xác nhận không khớp.");
            confirmPasswordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(confirmPasswordField, "Mật khẩu xác nhận không khớp");
            return;
        } else { // Matched or empty, reset style if it was error
            styleTextField(confirmPasswordField); // Assuming this is your default style method
        }


        if (password.length() < 8) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Mật khẩu phải có ít nhất 8 ký tự.");
            passwordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(passwordField, "Mật khẩu phải ít nhất 8 ký tự");
            return;
        } else {
            passwordField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;"); // Green if valid
        }


        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Email không đúng định dạng.");
            emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(emailField, "Email không đúng định dạng");
            return;
        }
        // Assuming successful validation for email format by this point, DAO will check existence

        if (!isValidPhone(phone)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Số điện thoại không đúng định dạng (chỉ chứa số).");
            phoneField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(phoneField, "Số điện thoại không đúng định dạng");
            return;
        }
        // Assuming successful validation for phone format

        LocalDate dob;
        int age;
        try {
            dob = LocalDate.parse(dobText, DATE_FORMATTER);
            if (dob.isAfter(LocalDate.now())) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Ngày sinh không được là một ngày trong tương lai.");
                dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                showTooltip(dobTextField, "Ngày sinh không hợp lệ");
                return;
            }
            age = Year.now().getValue() - dob.getYear();
            // Consider if day/month makes them not yet passed their birthday this year
            if (LocalDate.now().getDayOfYear() < dob.getDayOfYear() && LocalDate.now().getYear() == dob.getYear() + age) {
                age--; // Decrement age if birthday hasn't occurred this year.
            }
            // Add any other age validations (e.g., must be > 18)
            dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");

        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Ngày sinh không hợp lệ (định dạng DD/MM/YYYY).");
            dobTextField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
            showTooltip(dobTextField, "Định dạng ngày không hợp lệ (DD/MM/YYYY)");
            return;
        }

        if (!termsCheckBox.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi đăng ký", "Bạn phải đồng ý với điều khoản sử dụng.");
            return;
        }

        // 3. Instantiate DAO and perform DB operations
        RegisterDAO registerDAO;
        try {
            registerDAO = new RegisterDAO();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể khởi tạo dịch vụ dữ liệu (DAO). Vui lòng thử lại sau.");
            e.printStackTrace(); // Log for debugging
            return;
        }

        try {
            // Check if Admin role is selected and an Admin already exists
            if ("Admin".equals(role)) {
                if (registerDAO.isAdminExists()) { //
                    showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Chỉ được phép tạo một tài khoản Admin trong hệ thống.");
                    return;
                }
            }

            // Server-side validation for username uniqueness
            if (registerDAO.isUsernameExists(username)) { //
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Tên đăng nhập đã tồn tại.");
                usernameField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                showTooltip(usernameField, "Tên đăng nhập đã tồn tại");
                return;
            } else {
                usernameField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
            }

            // Server-side validation for email uniqueness
            if (registerDAO.isEmailExists(email)) { //
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Email đã được sử dụng.");
                emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #ff0000; -fx-text-fill: #555;");
                showTooltip(emailField, "Email đã được sử dụng");
                return;
            } else {
                emailField.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #00aa00; -fx-text-fill: #555;");
            }

            // If all checks pass, generate ID and proceed with registration
            String userId = generateUniqueID(role);

            boolean isRegistered = registerDAO.registerUser(username, password, role, fullName, email, phone, dob, age, selectedGender, userId); //

            if (isRegistered) {
                // showSuccessDialog(userId); // You have a showSuccessDialog, consider using it.
                showAlert(Alert.AlertType.INFORMATION, "Đăng ký thành công", "Tài khoản đã được tạo thành công! ID của bạn là: " + userId);
                clearRegisterFields();
            } else {
                // This 'else' might be due to the internal Admin check in registerUser method in DAO,
                // or other handled failures within registerUser.
                if ("Admin".equals(role)) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Đăng ký Admin thất bại. Có thể tài khoản Admin đã tồn tại hoặc đã xảy ra lỗi khác trong quá trình xử lý.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", "Đăng ký thất bại. Vui lòng thử lại hoặc kiểm tra lại thông tin đã nhập.");
                }
            }
        } catch (Exception e) { // Catch any other unexpected errors from DAO operations
            e.printStackTrace(); // Important for debugging
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Đã xảy ra lỗi không mong muốn trong quá trình đăng ký. Xin vui lòng thử lại sau.");
        }
    }
    private void clearRegisterFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        dobTextField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        termsCheckBox.setSelected(false);
    }


    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("[0-9]+");
    }





    private void showSuccessDialog(String userId) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng ký thành công");
        alert.setHeaderText(null);
        alert.setContentText("Chúc mừng! Bạn đã đăng ký tài khoản thành công.\nID của bạn là: " + userId + "\nVui lòng đăng nhập để sử dụng hệ thống.");
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
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);          // Dynamically set the Alert type
        alert.setTitle(title);                       // Set the title of the Alert
        alert.setHeaderText(null);                   // Keep the header empty
        alert.setContentText(message);               // Set the body content of the alert
        alert.showAndWait();                         // Wait until user responds
    }
}