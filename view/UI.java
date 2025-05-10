package view;

import src.controller.MainController;
import src.controller.NavigationController;
import src.model.person.Person;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Lớp quản lý giao diện người dùng chính của ứng dụng
 */
public class UI {
    private BorderPane root;
    private VBox mainContent;
    private String currentUserName = "Dummy";
    private String currentUserContact = "";
    private String currentUserEmail = "";
    private String currentUserRole = "";
    private NavigationController navigationController;
    private MainController mainController;
    private Label pageTitleLabel;
    private Label breadcrumbPathLabel;
    private Label userLabel;

    // Biến để lưu trạng thái hiển thị submenu
    private VBox sidebar;
    private VBox trainingSubmenu;
    private VBox studentSubmenu;
    private VBox reportSubmenu;
    private VBox manageSubmenu;
    private String currentSelectedMenu = "";

    /**
     * Khởi tạo UI
     */
    public UI() {
        // Được khởi tạo trống, controller sẽ cung cấp tham chiếu sau
    }

    /**
     * Thiết lập các controller cho UI
     * @param mainController Controller chính của ứng dụng
     * @param navigationController Controller điều hướng
     */
    public void setControllers(MainController mainController, NavigationController navigationController) {
        this.mainController = mainController;
        this.navigationController = navigationController;
    }

    /**
     * Thiết lập thông tin người dùng hiện tại
     * @param person Đối tượng Person chứa thông tin người dùng
     */
    public void setCurrentUser(Person person) {
        if (person != null) {
            this.currentUserName = person.getName();
            this.currentUserContact = person.getContactNumber();
            this.currentUserEmail = person.getEmail();
            this.currentUserRole = determineUserRole(person);

            // Cập nhật UI nếu đã được khởi tạo
            updateUserDisplay();
        }
    }

    /**
     * Xác định vai trò người dùng từ loại đối tượng
     * @param person Đối tượng người dùng
     * @return Chuỗi mô tả vai trò
     */
    private String determineUserRole(Person person) {
        String className = person.getClass().getSimpleName();
        switch (className) {
            case "Admin":
                return "Quản trị viên";
            case "Teacher":
                return "Giáo viên";
            case "Student":
                return "Học viên";
            case "Parent":
                return "Phụ huynh";
            default:
                return "";
        }
    }

    /**
     * Cập nhật thông tin người dùng trên giao diện
     */
    private void updateUserDisplay() {
        if (userLabel != null) {
            userLabel.setText(currentUserName);
        }
    }

    /**
     * Tạo scene cho ứng dụng
     * @return Scene đã được khởi tạo
     */
    public Scene createScene() {
        // Tạo BorderPane làm layout chính
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // 1. Tạo Header (MenuBar + Hotline + User Profile)
        HBox header = createHeader();
        root.setTop(header);

        // 2. Tạo Sidebar bên trái
        sidebar = createSidebar();
        root.setLeft(sidebar);

        // 3. Tạo phần nội dung chính (Main Content)
        mainContent = new VBox(0);
        mainContent.setStyle("-fx-background-color: #f5f5f5;");
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(scrollPane);

        // 4. Tạo Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Tạo Scene
        return new Scene(root, 1200, 768);
    }

    /**
     * Tạo Header cho ứng dụng
     * @return HBox chứa các thành phần của header
     */
    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(8, 15, 8, 15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);
        header.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Logo
        Label logo = new Label("AITALK");
        logo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0091EA;");
        try {
            // Cố gắng tải logo nếu có
            ImageView logoImage = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
            logoImage.setFitHeight(30);
            logoImage.setPreserveRatio(true);
            logo.setGraphic(logoImage);
        } catch (Exception e) {
            // Nếu không có logo, chỉ hiển thị text
            System.out.println("Logo image not found, using text only");
        }

        // Menu toggle button
        Button menuToggle = new Button();
        menuToggle.setStyle("-fx-background-color: transparent;");
        try {
            ImageView menuIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/menu.png")));
            menuIcon.setFitHeight(24);
            menuIcon.setFitWidth(24);
            menuToggle.setGraphic(menuIcon);
        } catch (Exception e) {
            menuToggle.setText("≡");
            menuToggle.setStyle("-fx-background-color: transparent; -fx-font-size: 20px;");
        }

        // Xử lý sự kiện khi nhấn nút menu (thu gọn/mở rộng sidebar)
        menuToggle.setOnAction(e -> {
            if (sidebar.isVisible()) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            } else {
                sidebar.setVisible(true);
                sidebar.setManaged(true);
            }
        });

        // Tiêu đề trang
        pageTitleLabel = new Label("Tổng quan");
        pageTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Breadcrumb
        HBox breadcrumb = new HBox(5);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        Button homeButton = new Button();
        homeButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView homeIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/home.png")));
            homeIcon.setFitHeight(16);
            homeIcon.setFitWidth(16);
            homeButton.setGraphic(homeIcon);
        } catch (Exception e) {
            homeButton.setText("🏠");
            homeButton.setStyle("-fx-background-color: transparent;");
        }

        homeButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.navigateTo("dashboard");
            }
        });

        breadcrumbPathLabel = new Label("Tổng quan");
        breadcrumbPathLabel.setStyle("-fx-text-fill: #757575;");

        breadcrumb.getChildren().addAll(homeButton, breadcrumbPathLabel);

        // Hotline
        HBox hotlineBox = new HBox(5);
        hotlineBox.setAlignment(Pos.CENTER);

        try {
            ImageView phoneIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/phone.png")));
            phoneIcon.setFitHeight(16);
            phoneIcon.setFitWidth(16);
            hotlineBox.getChildren().add(phoneIcon);
        } catch (Exception e) {
            Label phoneEmoji = new Label("📞");
            hotlineBox.getChildren().add(phoneEmoji);
        }

        Label hotlineLabel = new Label("Hotline: 0888888888 - 0999999999");
        hotlineLabel.setStyle("-fx-text-fill: #757575;");
        hotlineBox.getChildren().add(hotlineLabel);

        // Help button
        Button helpButton = new Button();
        helpButton.setStyle("-fx-background-color: transparent;");
        try {
            ImageView helpIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/help.png")));
            helpIcon.setFitHeight(20);
            helpIcon.setFitWidth(20);
            helpButton.setGraphic(helpIcon);
        } catch (Exception e) {
            helpButton.setText("❓");
            helpButton.setStyle("-fx-background-color: transparent;");
        }

        helpButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.navigateTo("help");
            }
        });

        Label helpLabel = new Label("Help");
        helpLabel.setStyle("-fx-text-fill: #757575;");
        HBox helpBox = new HBox(5);
        helpBox.setAlignment(Pos.CENTER);
        helpBox.getChildren().addAll(helpButton, helpLabel);

        // User profile
        HBox userProfileBox = new HBox(10);
        userProfileBox.setAlignment(Pos.CENTER);

        // Tham chiếu đến userLabel để có thể cập nhật sau này
        userLabel = new Label(currentUserName);
        userLabel.setStyle("-fx-text-fill: #333;");

        try {
            ImageView avatarView = new ImageView(new Image(getClass().getResourceAsStream("/images/avatar.png")));
            avatarView.setFitHeight(30);
            avatarView.setFitWidth(30);
            avatarView.setStyle("-fx-background-radius: 15;");
            userProfileBox.getChildren().add(avatarView);
        } catch (Exception e) {
            Circle avatarCircle = new Circle(15, Color.LIGHTGRAY);
            userProfileBox.getChildren().add(avatarCircle);
        }

        userProfileBox.getChildren().add(userLabel);

        try {
            ImageView dropdownIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/dropdown.png")));
            dropdownIcon.setFitHeight(12);
            dropdownIcon.setFitWidth(12);
            userProfileBox.getChildren().add(dropdownIcon);
        } catch (Exception e) {
            Label dropdownSymbol = new Label("▼");
            dropdownSymbol.setStyle("-fx-font-size: 8px;");
            userProfileBox.getChildren().add(dropdownSymbol);
        }

        // Xử lý sự kiện click vào profile
        userProfileBox.setOnMouseClicked(e -> {
            if (navigationController != null) {
                navigationController.navigateTo("profile");
            }
        });

        // Thêm tooltip để hiển thị thông tin chi tiết người dùng
        Tooltip userTooltip = new Tooltip(
                "Họ tên: " + currentUserName + "\n" +
                        "Vai trò: " + currentUserRole + "\n" +
                        "SĐT: " + currentUserContact + "\n" +
                        "Email: " + currentUserEmail
        );
        Tooltip.install(userProfileBox, userTooltip);

        // Notification button
        Button notifButton = new Button();
        notifButton.setStyle("-fx-background-color: #00C853; -fx-background-radius: 50%;");
        notifButton.setMinSize(30, 30);
        notifButton.setMaxSize(30, 30);
        try {
            ImageView notifIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/notification.png")));
            notifIcon.setFitHeight(16);
            notifIcon.setFitWidth(16);
            notifButton.setGraphic(notifIcon);
        } catch (Exception e) {
            notifButton.setText("🔔");
            notifButton.setStyle("-fx-background-color: #00C853; -fx-background-radius: 50%; -fx-text-fill: white;");
        }

        notifButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.navigateTo("notifications");
            }
        });

        // Spacer để đẩy các phần tử qua bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(logo, menuToggle, pageTitleLabel, breadcrumb, spacer, hotlineBox, helpBox, userProfileBox, notifButton);
        return header;
    }

    /**
     * Tạo Sidebar cho ứng dụng
     * @return VBox chứa các nút điều hướng của sidebar
     */
    private VBox createSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setMinWidth(260);
        sidebar.setPrefWidth(260);
        sidebar.setMaxWidth(260);
        sidebar.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 0 0;");

        // Menu items
        Button chatButton = createSidebarButton("Nhắn tin", "message", "chat");

        HBox trainingHeader = createMenuHeaderWithToggle("Đào tạo", "training");
        trainingSubmenu = createSubmenu();
        trainingSubmenu.getChildren().addAll(
                createSubmenuButton("Lịch học", "schedule"),
                createSubmenuButton("Điểm danh", "attendance"),
                createSubmenuButton("Kỳ thi", "exams"),
                createSubmenuButton("Chứng chỉ", "certificates")
        );
        trainingSubmenu.setVisible(false);
        trainingSubmenu.setManaged(false);

        HBox studentHeader = createMenuHeaderWithToggle("Học viên", "students");
        studentSubmenu = createSubmenu();
        studentSubmenu.getChildren().addAll(
                createSubmenuButton("Học viên", "StudentListView"),
                createSubmenuButton("Lớp học", "ClassListView")
        );
        studentSubmenu.setVisible(false);
        studentSubmenu.setManaged(false);

        HBox reportHeader = createMenuHeaderWithToggle("Báo cáo", "reports");
        reportSubmenu = createSubmenu();
        reportSubmenu.getChildren().addAll(
                createSubmenuButton("Tình hình học tập", "learning-reports"),
                //createSubmenuButton("Báo cáo công việc", "work-reports"),
                createSubmenuButton("Thống kê giờ giảng", "teaching-statistics")
        );
        reportSubmenu.setVisible(false);
        reportSubmenu.setManaged(false);

        HBox manageHeader = createMenuHeaderWithToggle("Quản lý", "management");
        manageSubmenu = createSubmenu();
        manageSubmenu.getChildren().addAll(
                createSubmenuButton("Phòng học", "classrooms"),
                createSubmenuButton("Ngày nghỉ", "holidays"),
                createSubmenuButton("Cài đặt", "settings")
        );
        manageSubmenu.setVisible(false);
        manageSubmenu.setManaged(false);

        sidebar.getChildren().addAll(
                chatButton,
                trainingHeader, trainingSubmenu,
                studentHeader, studentSubmenu,
                reportHeader, reportSubmenu,
                manageHeader, manageSubmenu
        );

        return sidebar;
    }

    /**
     * Tạo header cho menu có thể mở rộng
     * @param text Chữ hiển thị
     * @param iconName Tên icon
     * @return HBox chứa header menu
     */
    private HBox createMenuHeaderWithToggle(String text, String iconName) {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        // Icon
        Node icon;
        try {
            ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/images/" + iconName + ".png")));
            iconView.setFitHeight(18);
            iconView.setFitWidth(18);
            icon = iconView;
        } catch (Exception e) {
            // Nếu không tìm thấy icon, dùng emoji hoặc text
            String emoji = "";
            switch (iconName) {
                case "training": emoji = "📚"; break;
                case "students": emoji = "👥"; break;
                case "reports": emoji = "📊"; break;
                case "management": emoji = "⚙️"; break;
                default: emoji = "•";
            }
            Label iconLabel = new Label(emoji);
            iconLabel.setMinWidth(30);
            icon = iconLabel;
        }

        // Label
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        // Toggle icon
        Label toggleIcon = new Label("▶");
        toggleIcon.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(icon, label, spacer, toggleIcon);

        // Hover effect
        header.setOnMouseEntered(e -> {
            header.setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand;");
            label.setStyle("-fx-text-fill: #333; -fx-font-size: 14px;");
        });

        header.setOnMouseExited(e -> {
            if (!iconName.equals(currentSelectedMenu)) {
                header.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                label.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            } else {
                header.setStyle("-fx-background-color: #e3f2fd; -fx-cursor: hand;");
                label.setStyle("-fx-text-fill: #0091EA; -fx-font-size: 14px;");
            }
        });

        // Click event to toggle submenu
        header.setOnMouseClicked(e -> {
            VBox submenu = null;
            switch (iconName) {
                case "training": submenu = trainingSubmenu; break;
                case "students": submenu = studentSubmenu; break;
                case "reports": submenu = reportSubmenu; break;
                case "management": submenu = manageSubmenu; break;
            }

            if (submenu != null) {
                boolean isVisible = submenu.isVisible();

                // Hide all submenus first
                trainingSubmenu.setVisible(false);
                trainingSubmenu.setManaged(false);
                studentSubmenu.setVisible(false);
                studentSubmenu.setManaged(false);
                reportSubmenu.setVisible(false);
                reportSubmenu.setManaged(false);
                manageSubmenu.setVisible(false);
                manageSubmenu.setManaged(false);

                // Reset all headers
                resetMenuHeaderStyles();

                // If the clicked submenu was already visible, we just closed it
                if (!isVisible) {
                    submenu.setVisible(true);
                    submenu.setManaged(true);
                    toggleIcon.setText("▼");
                    header.setStyle("-fx-background-color: #e3f2fd; -fx-cursor: hand;");
                    label.setStyle("-fx-text-fill: #0091EA; -fx-font-size: 14px;");
                    currentSelectedMenu = iconName;
                } else {
                    toggleIcon.setText("▶");
                    currentSelectedMenu = "";
                }
            }
        });

        return header;
    }

    /**
     * Reset styles for all menu headers
     */
    private void resetMenuHeaderStyles() {
        // Reset all menu headers in the sidebar
        for (Node node : sidebar.getChildren()) {
            if (node instanceof HBox && !(node instanceof Button)) {
                HBox header = (HBox) node;
                header.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                // Update label style
                for (Node child : header.getChildren()) {
                    if (child instanceof Label && !(((Label) child).getText().equals("▶") || ((Label) child).getText().equals("▼"))) {
                        ((Label) child).setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    }

                    // Reset toggle icon
                    if (child instanceof Label && (((Label) child).getText().equals("▶") || ((Label) child).getText().equals("▼"))) {
                        ((Label) child).setText("▶");
                    }
                }
            }
        }
    }

    /**
     * Tạo container cho submenu
     * @return VBox chứa các mục submenu
     */
    private VBox createSubmenu() {
        VBox submenu = new VBox(0);
        submenu.setPadding(new Insets(0, 0, 0, 30));
        submenu.setStyle("-fx-background-color: #f9f9f9;");
        return submenu;
    }

    /**
     * Tạo nút submenu
     * @param text Chữ hiển thị
     * @param route Đường dẫn điều hướng
     * @return Button đã được cấu hình
     */
    private Button createSubmenuButton(String text, String route) {
        Button button = new Button(text);
        button.setPadding(new Insets(10, 15, 10, 15));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 13px; -fx-border-color: transparent;");

        // Hover effect
        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: #e9e9e9; -fx-text-fill: #333; -fx-font-size: 13px;"));

        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 13px;"));

        // Click event
        button.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.navigateTo(route);
            }
        });

        return button;
    }

    /**
     * Tạo nút điều hướng cho sidebar
     * @param text Chữ hiển thị trên nút
     * @param iconName Tên icon
     * @param route Đường dẫn điều hướng
     * @return Button đã được cấu hình
     */
    private Button createSidebarButton(String text, String iconName, String route) {
        Button button = new Button(text);
        button.setPadding(new Insets(15, 20, 15, 20));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 14px; -fx-border-color: transparent;");

        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/images/" + iconName + ".png")));
            icon.setFitHeight(18);
            icon.setFitWidth(18);
            button.setGraphic(icon);
        } catch (Exception e) {
            // Nếu không tìm thấy icon, dùng emoji hoặc text
            String emoji = "";
            switch (iconName) {
                case "message": emoji = "💬"; break;
                case "training": emoji = "📚"; break;
                case "student": emoji = "👥"; break;
                case "report": emoji = "📊"; break;
                case "manage": emoji = "⚙️"; break;
                default: emoji = "•";
            }
            Label iconLabel = new Label(emoji);
            iconLabel.setMinWidth(30);
            button.setGraphic(iconLabel);
        }

        // Hover effect
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #333; -fx-font-size: 14px; -fx-border-color: transparent;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 14px; -fx-border-color: transparent;"));

        // Gắn sự kiện điều hướng
        button.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.navigateTo(route);
                // Ẩn tất cả submenu khi chuyển đến route khác
                hideAllSubmenus();
                resetMenuHeaderStyles();
                currentSelectedMenu = "";
            }
        });

        return button;
    }

    /**
     * Ẩn tất cả các submenu
     */
    private void hideAllSubmenus() {
        if (trainingSubmenu != null) {
            trainingSubmenu.setVisible(false);
            trainingSubmenu.setManaged(false);
        }
        if (studentSubmenu != null) {
            studentSubmenu.setVisible(false);
            studentSubmenu.setManaged(false);
        }
        if (reportSubmenu != null) {
            reportSubmenu.setVisible(false);
            reportSubmenu.setManaged(false);
        }
        if (manageSubmenu != null) {
            manageSubmenu.setVisible(false);
            manageSubmenu.setManaged(false);
        }
    }

    /**
     * Tạo Footer cho ứng dụng
     * @return HBox chứa các thành phần của footer
     */
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(15));
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 2 5 0;");

        VBox footerContent = new VBox(5);
        footerContent.setAlignment(Pos.CENTER);

        Label copyrightLabel = new Label("Copyright © 2025 CÔNG TY CỔ PHẦN GIẢI PHÁP GIÁO DỤC AILEARN. All rights reserved.");
        copyrightLabel.setStyle("-fx-text-fill: #666;");

        Label addressLabel = new Label("Địa chỉ: 367 Hoàng Quốc Việt, Cầu Giấy, Hà Nội");
        addressLabel.setStyle("-fx-text-fill: #666;");

        Label contactLabel = new Label("Hotline: 0966945495 - 0977962582 - Email: aitalkvietnam.edu@gmail.com");
        contactLabel.setStyle("-fx-text-fill: #666;");

        footerContent.getChildren().addAll(copyrightLabel, addressLabel, contactLabel);
        footer.getChildren().add(footerContent);

        return footer;
    }

    /**
     * Đặt nội dung cho phần chính của UI
     * @param content Node hiển thị trong phần nội dung chính
     */
    public void setContent(Node content) {
        mainContent.getChildren().clear();
        mainContent.getChildren().add(content);
    }

    /**
     * Cập nhật tiêu đề trang
     * @param title Tiêu đề mới
     */
    public void updatePageTitle(String title) {
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(title);
        }
        if (breadcrumbPathLabel != null) {
            breadcrumbPathLabel.setText(title);
        }
    }

    /**
     * Hiển thị thông báo lỗi
     * @param message Nội dung thông báo lỗi
     */
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo
     * @param message Nội dung thông báo
     */
    public void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị hộp thoại xác nhận
     * @param message Nội dung cần xác nhận
     * @return true nếu người dùng đồng ý, false nếu không
     */
    public boolean showConfirmDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType buttonTypeYes = new ButtonType("Đồng ý");
        ButtonType buttonTypeNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        return alert.showAndWait().orElse(buttonTypeNo) == buttonTypeYes;
    }

    /**
     * Hiển thị dialog nhập liệu
     * @param title Tiêu đề dialog
     * @param headerText Tiêu đề phụ
     * @param contentText Nội dung
     * @return Chuỗi người dùng nhập vào hoặc null nếu hủy
     */
    public String showInputDialog(String title, String headerText, String contentText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);
        return dialog.showAndWait().orElse(null);
    }

    /**
     * Lấy tên người dùng hiện tại
     * @return Tên người dùng
     */
    public String getCurrentUserName() {
        return currentUserName;
    }

    /**
     * Lấy thông tin liên hệ của người dùng hiện tại
     * @return Số điện thoại liên hệ
     */
    public String getCurrentUserContact() {
        return currentUserContact;
    }

    /**
     * Lấy email của người dùng hiện tại
     * @return Email người dùng
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    /**
     * Circle class for avatar placeholder (if image not available)
     */
    private class Circle extends StackPane {
        public Circle(double radius, Color color) {
            this.setMinSize(radius * 2, radius * 2);
            this.setMaxSize(radius * 2, radius * 2);

            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(radius);
            circle.setFill(color);

            Text text = new Text(currentUserName.substring(0, 1).toUpperCase());
            text.setStyle("-fx-font-size: 16px; -fx-fill: white;");
            text.setTextAlignment(TextAlignment.CENTER);

            this.getChildren().addAll(circle, text);
        }
    }
}
