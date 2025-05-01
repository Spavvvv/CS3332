package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LoginUI {
    private Stage primaryStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox rememberMeCheck;
    private String currentTheme = "light"; // Mặc định là theme sáng
    private NavigationController navigationController;
    private MainController mainController;
    private final static String FILE_PATH = "C:\\Users\\tiend\\IdeaProjects\\CS3332";

    public LoginUI(Stage primaryStage) {
        this.primaryStage = primaryStage;
        setupStage();
    }

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

    private VBox createLoginCard() {
        VBox card = new VBox(30);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 50, 50, 50));
        card.setMaxWidth(400);
        applyCardStyle(currentTheme, card, card); // Áp dụng style ban đầu cho card

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

            // Xử lý sự kiện khi nhấp vào ô màu
            String finalTheme = theme;
            colorCircle.setOnMouseClicked(e -> {
                currentTheme = finalTheme;
                applyTheme(primaryStage.getScene().getRoot(), currentTheme);
                applyCardStyle(currentTheme, card, card);
            });

            colorPalette.getChildren().add(colorCircle);
        }

        themeBox.getChildren().addAll(themeLabel, colorPalette);
        return themeBox;
    }

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

    private void applyCardStyle(String theme, VBox card, VBox cardBox) {
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
        cardBox.setStyle("-fx-background-color: " + cardColor + "; -fx-background-radius: 10;");

        // Áp dụng màu chữ cho các label trong card
        for (javafx.scene.Node node : cardBox.getChildren()) {
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
                    }
                }
            } else if (node instanceof CheckBox) {
                ((CheckBox) node).setStyle("-fx-text-fill: " + textColor + ";");
            } else if (node instanceof Hyperlink) {
                ((Hyperlink) node).setStyle("-fx-text-fill: " + textColor + ";");
            }
        }
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String filePath = FILE_PATH + "\\" + username + ".txt";
        File file = new File(filePath);

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi đăng nhập", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        if (!file.exists()) {
            showAlert("Lỗi đăng nhập", "Tên đăng nhập không tồn tại");
            return;
        }

        try (Scanner scanner = new Scanner(file)) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length >= 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    // Đăng nhập thành công
                    System.out.println("Đăng nhập thành công!");
                    primaryStage.close();

                    // Khởi tạo và hiển thị UI chính
                    Stage uiStage = new Stage();
                    UI ui = new UI();

                    NavigationController navigationController1 = new NavigationController(ui);
                    MainController mainController1 = new MainController(ui, navigationController1);

                    // Liên kết UI với các controller
                    ui.setControllers(mainController1, navigationController1);


                    //ui.setControllers(mainController, navigationController);


                    Scene uiScene = ui.createScene();
                    uiStage.setScene(uiScene);
                    uiStage.setTitle("Hệ thống quản lý trung tâm");
                    uiStage.show();


                    mainController1.onAppStart();
                    return;
                }
            }
            showAlert("Lỗi đăng nhập", "Mật khẩu không đúng");
        } catch (FileNotFoundException e) {
            showAlert("Lỗi đăng nhập", "Không tìm thấy file tài khoản");
        }
    }


    private void gotoRegister() {
        primaryStage.close();
        Stage registerStage = new Stage();
        RegisterUI registerUI = new RegisterUI(registerStage);
        registerUI.setControllers(mainController, navigationController);
    }

    public void setControllers(MainController mainController, NavigationController navigationController) {
        this.mainController = mainController;
        this.navigationController = navigationController;
    }

    // Phương thức khởi chạy
    public static void show(Stage primaryStage) {
        new LoginUI(primaryStage);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
