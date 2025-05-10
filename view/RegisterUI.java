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
import javafx.util.StringConverter;
import src.controller.MainController;
import src.controller.NavigationController;
import java.io.*;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class RegisterUI {
    private Stage primaryStage;
    private StackPane root;
    private VBox registerCard;
    private TextField usernameField;
    private TextField emailField;
    private TextField fullNameField;
    private TextField phoneField;
    private DatePicker dobPicker; // DatePicker cho ngày sinh
    private TextField dobTextField; // TextField thêm cho nhập tay ngày sinh
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private ComboBox<String> roleComboBox;
    private CheckBox termsCheckBox;
    private NavigationController navigationController;
    private MainController mainController;
    private String currentTheme = "light"; // Mặc định là theme sáng
    // File path for storing user accounts
    private String ACCOUNTS_FILE;
    private final static String FILE_PATH = "D:\\3323\\3323\\UserAccount";
    private final static String TEACHER_FOLDER = FILE_PATH + "\\Teacher";
    private final static String STUDENT_FOLDER = FILE_PATH + "\\Student";
    private final static String PARENT_FOLDER = FILE_PATH + "\\Parent";

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

    // Create accounts file if it doesn't exist
    private void createAccountsFileIfNotExists(String username, String role) {
        ACCOUNTS_FILE = username + ".txt";
        String folderPath;
        folderPath = STUDENT_FOLDER;
        // Xác định đường dẫn thư mục dựa trên vai trò
        if (role.equals("Giáo viên")) {
            folderPath = TEACHER_FOLDER;
        } else if (role.equals("Học viên")) {
            folderPath = STUDENT_FOLDER;
        } else if(role.equals("Phụ Huynh")) { // Phụ huynh
            folderPath = PARENT_FOLDER;
        }

        File file = new File(folderPath, ACCOUNTS_FILE);
        if (!file.exists()) {
            try {
                // Tạo thư mục nếu chưa tồn tại
                File directory = new File(folderPath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                file.createNewFile();
                System.out.println("Created new accounts file: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating accounts file: " + e.getMessage());
            }
        }
    }


    // Check if username already exists in the file
    private boolean isUsernameTaken(String username) {
        // Kiểm tra trong tất cả các thư mục
        File teacherFile = new File(TEACHER_FOLDER, username + ".txt");
        File studentFile = new File(STUDENT_FOLDER, username + ".txt");
        File parentFile = new File(PARENT_FOLDER, username + ".txt");

        return teacherFile.exists() || studentFile.exists() || parentFile.exists();
    }


    // Check if email already exists in the file
    private boolean isEmailTaken(String email) {
        // Danh sách thư mục cần kiểm tra
        String[] directories = {TEACHER_FOLDER, STUDENT_FOLDER, PARENT_FOLDER};

        for (String directoryPath : directories) {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                continue;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    try (Scanner scanner = new Scanner(file)) {
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
                }
            }
        }
        return false;
    }


    // Kiểm tra xem ID đã tồn tại trong hệ thống chưa
    private boolean isIDTaken(String id) {
        String[] directories = {TEACHER_FOLDER, STUDENT_FOLDER, PARENT_FOLDER};

        for (String directoryPath : directories) {
            File directory = new File(directoryPath);
            if (!directory.exists() || !directory.isDirectory()) {
                continue;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    try (Scanner scanner = new Scanner(file)) {
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            String[] parts = line.split(",");
                            if (parts.length >= 9 && parts[8].trim().equals(id.trim())) {
                                return true;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        System.err.println("Error checking ID: " + e.getMessage());
                    }
                }
            }
        }
        return false;
    }


    // Tạo ID ngẫu nhiên 6 chữ số dựa trên vai trò
    private String generateUniqueID(String role) {
        // Xác định số đầu tiên dựa trên vai trò
        String firstDigit;
        if (role.equals("Giáo viên")) {
            firstDigit = "1";
        } else if (role.equals("Học viên")) {
            firstDigit = "2";
        } else { // Phụ huynh
            firstDigit = "3";
        }

        // Tạo 5 chữ số còn lại ngẫu nhiên
        StringBuilder randomDigits = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            randomDigits.append(random.nextInt(10));
        }

        return firstDigit + randomDigits.toString();
    }

    // Save a new account to the file
    private void saveAccount(String username, String password, String fullName, String email, String phone, String role, LocalDate dob, int age, String userId) {
        ACCOUNTS_FILE = username + ".txt";
        String folderPath;

        // Xác định đường dẫn thư mục dựa trên vai trò
        if (role.equals("Giáo viên")) {
            folderPath = TEACHER_FOLDER;
        } else if (role.equals("Học viên")) {
            folderPath = STUDENT_FOLDER;
        } else { // Phụ huynh
            folderPath = PARENT_FOLDER;
        }

        File file = new File(folderPath, ACCOUNTS_FILE);

        try (FileWriter writer = new FileWriter(file, true)) {
            // Format: username,password,fullName,email,phone,role,dob,age,userId
            String dobString = dob != null ? dob.format(DATE_FORMATTER) : "";
            String accountInfo = String.format("%s,%s,%s,%s,%s,%s,%s,%d,%s%n",
                    username.trim(), password, fullName.trim(), email.trim(), phone.trim(), role.trim(), dobString, age, userId);
            writer.write(accountInfo);
            System.out.println("Account saved: " + username + " with ID: " + userId + " in folder: " + folderPath);
        } catch (IOException e) {
            System.err.println("Error saving account: " + e.getMessage());
        }
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
        form.add(dobLabel, 0, 6); // Date of birth label
        form.add(dobInputBox, 0, 7); // Date of birth input

        // Cột 2
        form.add(phoneLabel, 1, 0);
        form.add(phoneField, 1, 1);
        form.add(roleLabel, 1, 2);
        form.add(roleComboBox, 1, 3);
        form.add(passLabel, 1, 4);
        form.add(passwordField, 1, 5);
        form.add(confirmPassLabel, 1, 6);
        form.add(confirmPasswordField, 1, 7);

        // Row span toàn bộ chiều rộng form
        // Không thêm themeBox vào trang đăng ký, chỉ đọc theme từ Login
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
        // Kiểm tra thông tin đăng ký
        if (fullNameField.getText().isEmpty() ||
                usernameField.getText().isEmpty() ||
                emailField.getText().isEmpty() ||
                phoneField.getText().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty() ||
                dobTextField.getText().isEmpty()) {

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

        // Kiểm tra và phân tích ngày sinh
        LocalDate dob = null;
        try {
            dob = LocalDate.parse(dobTextField.getText(), DATE_FORMATTER);

            if (dob.isAfter(LocalDate.now())) {
                showAlert("Lỗi đăng ký", "Ngày sinh không hợp lệ");
                return;
            }
        } catch (DateTimeParseException e) {
            showAlert("Lỗi đăng ký", "Định dạng ngày sinh không hợp lệ (DD/MM/YYYY)");
            return;
        }

        // Tính tuổi dựa trên năm hiện tại (2025)
        int birthYear = dob.getYear();
        int currentYear = Year.now().getValue();
        int age = currentYear - birthYear;

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

        // Lấy vai trò người dùng
        String role = roleComboBox.getValue();

        // Tạo ID độc nhất cho người dùng dựa trên vai trò
        String userId;
        do {
            userId = generateUniqueID(role);
        } while (isIDTaken(userId));

        // Sau khi đã kiểm tra hết, thì mớ
        // i tạo file
        String role1 = roleComboBox.getValue();
        createAccountsFileIfNotExists(username, role1);


        // Lưu tài khoản mới vào file (bao gồm ngày sinh, tuổi và ID)
        saveAccount(
                username,
                passwordField.getText(),
                fullNameField.getText(),
                emailField.getText(),
                phoneField.getText(),
                role,
                dob,
                age,
                userId
        );

        // Hiển thị thông báo đăng ký thành công kèm theo ID
        showSuccessDialog(userId);
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}