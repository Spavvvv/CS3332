package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

public class UI {
    private VBox mainContent;
    private VBox currentSubmenu = null;
    private Button lastActiveButton = null;

    public Scene createScene() {
        // Tạo BorderPane làm layout chính
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // 1. Tạo Header (MenuBar + Hotline + User Profile)
        HBox header = createHeader();
        root.setTop(header);

        // 2. Tạo Sidebar bên trái
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // 3. Tạo phần nội dung chính (Main Content)
        mainContent = createMainContent();
        root.setCenter(mainContent);

        // 4. Tạo Footer
        HBox footer = createFooter();
        root.setBottom(footer);

        // Tạo Scene
        return new Scene(root, 1200, 600);
    }

    // Tạo Header
    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #673ab7; -fx-alignment: center-left;");

        // Logo
        Label logo = new Label("AITALK");
        logo.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

        // Breadcrumb
        Label breadcrumb = new Label("Học viên > HỌC VIÊN");
        breadcrumb.setStyle("-fx-text-fill: white;");

        // Hotline
        Label hotline = new Label("Hotline: 0966945495 - 0977962582");
        hotline.setStyle("-fx-text-fill: white;");

        // Help Icon
        Label helpIcon = new Label("Help");
        helpIcon.setStyle("-fx-text-fill: white;");

        // User Profile
        Label userProfile = new Label("Lê Quang Huy");
        userProfile.setStyle("-fx-text-fill: white;");

        HBox.setHgrow(breadcrumb, Priority.ALWAYS);
        header.getChildren().addAll(logo, breadcrumb, hotline, helpIcon, userProfile);
        return header;
    }

    // Tạo Sidebar
    private VBox createSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: #e0e0e0; -fx-min-width: 200px;");

        // Menu items
        Button newsButton = createMenuButton("Nhắn tin", "chat");
        Button trainingButton = createMenuButton("Đào tạo", "education");
        Button studentButton = createMenuButton("Học viên", "students");
        Button reportButton = createMenuButton("Báo cáo", "report");
        //Button accountButton = createMenuButton("Tư khoản", "account");
        Button manageButton = createMenuButton("Quản lý", "manage");

        // Sub menus
        VBox trainingSubmenu = createTrainingSubmenu();
        VBox studentSubmenu = createStudentSubmenu();
        VBox reportSubmenu = createReportSubmenu();
        VBox manageSubmenu = createManageSubmenu();

        // Hide all submenus initially
        trainingSubmenu.setVisible(false);
        trainingSubmenu.setManaged(false);
        studentSubmenu.setVisible(false);
        studentSubmenu.setManaged(false);
        reportSubmenu.setVisible(false);
        reportSubmenu.setManaged(false);
        manageSubmenu.setVisible(false);
        manageSubmenu.setManaged(false);

        // Button click handlers for main menu
        trainingButton.setOnAction(e -> toggleSubmenu(trainingButton, trainingSubmenu));
        studentButton.setOnAction(e -> toggleSubmenu(studentButton, studentSubmenu));
        reportButton.setOnAction(e -> toggleSubmenu(reportButton, reportSubmenu));
        manageButton.setOnAction(e -> toggleSubmenu(manageButton, manageSubmenu));
        newsButton.setOnAction(e -> handleMainMenuClick(newsButton));
       // accountButton.setOnAction(e -> handleMainMenuClick(accountButton));

        sidebar.getChildren().addAll(
                newsButton,
                trainingButton, trainingSubmenu,
                studentButton, studentSubmenu,
                reportButton, reportSubmenu,
                //accountButton,
                manageButton, manageSubmenu
        );

        return sidebar;
    }

    // Tạo button cho menu chính
    private Button createMenuButton(String text, String icon) {
        Button button = new Button(text);
        button.setPadding(new Insets(10));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        // Thêm icon (giả lập - thực tế bạn cần thay thế bằng icon thực)
        // ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + icon + ".png")));
        // button.setGraphic(iconView);

        return button;
    }

    // Tạo submenu cho Đào tạo
    private VBox createTrainingSubmenu() {
        VBox submenu = new VBox(0);
        submenu.setStyle("-fx-background-color: #f0f0f0;");

        Button scheduleButton = createSubmenuButton("Lịch học", "calendar");
        Button scoreButton = createSubmenuButton("Điểm danh", "score");
        Button studyButton = createSubmenuButton("Học tập", "study");
        Button examButton = createSubmenuButton("Kỳ thi", "exam");
        Button certificateButton = createSubmenuButton("Chứng chỉ", "certificate");

        submenu.getChildren().addAll(scheduleButton, scoreButton, studyButton, examButton, certificateButton);
        return submenu;
    }

    // Tạo submenu cho Học viên
    private VBox createStudentSubmenu() {
        VBox submenu = new VBox(0);
        submenu.setStyle("-fx-background-color: #f0f0f0;");

        Button studentButton = createSubmenuButton("Học viên", "student");
        Button classButton = createSubmenuButton("Lớp học", "class");

        submenu.getChildren().addAll(studentButton, classButton);
        return submenu;
    }

    // Tạo submenu cho Báo cáo
    private VBox createReportSubmenu() {
        VBox submenu = new VBox(0);
        submenu.setStyle("-fx-background-color: #f0f0f0;");

        Button studyStatusButton = createSubmenuButton("Tình hình học tập", "study-status");
        Button workReportButton = createSubmenuButton("Báo cáo công việc", "work-report");
        Button teachingTimeButton = createSubmenuButton("Thống kê giờ giảng", "teaching-time");

        submenu.getChildren().addAll(studyStatusButton, workReportButton, teachingTimeButton);
        return submenu;
    }

    // Tạo submenu cho Quản lý
    private VBox createManageSubmenu() {
        VBox submenu = new VBox(0);
        submenu.setStyle("-fx-background-color: #f0f0f0;");

        Button classroomButton = createSubmenuButton("Phòng học", "classroom");
        Button newsButton = createSubmenuButton("Tin tức", "news");
        Button holidayButton = createSubmenuButton("Ngày nghỉ", "holiday");

        submenu.getChildren().addAll(classroomButton, newsButton, holidayButton);
        return submenu;
    }

    // Tạo button cho submenu
    private Button createSubmenuButton(String text, String icon) {
        Button button = new Button("  " + text);
        button.setPadding(new Insets(10, 10, 10, 20)); // Thêm padding bên trái để tạo hiệu ứng lồng
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        // Thêm icon (giả lập - thực tế bạn cần thay thế bằng icon thực)
        // ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + icon + ".png")));
        // button.setGraphic(iconView);

        // Xử lý sự kiện click cho các button trong submenu
        button.setOnAction(e -> {
            // Hiển thị nội dung tương ứng với button được click
            // Ở đây bạn có thể thêm logic để hiển thị nội dung tương ứng
            System.out.println("Clicked on: " + text);
        });

        return button;
    }

    // Xử lý hiển thị/ẩn submenu
    private void toggleSubmenu(Button button, VBox submenu) {
        // Ẩn submenu đang hiển thị (nếu có)
        if (currentSubmenu != null && currentSubmenu != submenu) {
            currentSubmenu.setVisible(false);
            currentSubmenu.setManaged(false);
            if (lastActiveButton != null) {
                lastActiveButton.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            }
        }

        // Hiển thị/ẩn submenu được chọn
        boolean isVisible = submenu.isVisible();
        submenu.setVisible(!isVisible);
        submenu.setManaged(!isVisible);

        // Cập nhật style cho button
        if (!isVisible) {
            button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #673ab7;");
            lastActiveButton = button;
            currentSubmenu = submenu;
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
            lastActiveButton = null;
            currentSubmenu = null;
        }

        // Cập nhật nội dung chính dựa trên menu được chọn
        updateMainContent(button.getText());
    }

    // Xử lý khi click vào menu chính (không có submenu)
    private void handleMainMenuClick(Button button) {
        // Ẩn submenu đang hiển thị (nếu có)
        if (currentSubmenu != null) {
            currentSubmenu.setVisible(false);
            currentSubmenu.setManaged(false);
            currentSubmenu = null;
        }

        // Cập nhật style cho tất cả các button
        if (lastActiveButton != null) {
            lastActiveButton.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        }

        // Đặt style cho button được chọn
        button.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #673ab7;");
        lastActiveButton = button;

        // Cập nhật nội dung chính dựa trên menu được chọn
        updateMainContent(button.getText());
    }

    // Cập nhật nội dung chính dựa trên menu được chọn
    private void updateMainContent(String menuName) {
        // Trong thực tế, bạn sẽ thay thế nội dung chính với nội dung tương ứng
        // Ở đây chúng ta chỉ thay đổi tiêu đề để minh họa
        Label title = (Label) mainContent.getChildren().get(0);
        title.setText(menuName + " - Trung Tâm Luyện Thi iClass");
    }

    // Tạo Main Content
    private VBox createMainContent() {
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(20));

        // Title
        Label title = new Label("Học viên - Trung Tâm Luyện Thi iClass");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Statistics Cards
        HBox stats = new HBox(10);
        stats.getChildren().addAll(
                createStatCard("Hẹn lịch", "Ngày: 0", "Tuần: 0"),
                createStatCard("Bắt đầu", "Ngày: 0", "Tuần: 0"),
                createStatCard("Kết thúc", "Ngày: 0", "Tuần: 0"),
                createStatCard("Dự 3 buổi", "Đã hết: 97"),
                createStatCard("Còn lại", "61"),
                createStatCard("Sinh nhật", "Ngày: 0", "Tuần: 0"),
                createStatCard("SN phụ huynh", "Ngày: 0", "Tuần: 0")
        );

        // Search Bar
        HBox searchBar = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Họ tên, điện thoại, email...");
        Button executeButton = new Button("Thực hiện");
        executeButton.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;");
        searchBar.getChildren().addAll(searchField, executeButton);

        // Table
        TableView<String> table = createTable();
        table.setPrefHeight(300);

        mainContent.getChildren().addAll(title, stats, searchBar, table);
        return mainContent;
    }

    // Tạo Stat Card
    private VBox createStatCard(String title, String... details) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        card.getChildren().add(titleLabel);

        for (String detail : details) {
            card.getChildren().add(new Label(detail));
        }

        return card;
    }

    // Tạo Table
    private TableView<String> createTable() {
        TableView<String> table = new TableView<>();

        // Tạo các cột
        TableColumn<String, String> sttCol = new TableColumn<>("STT");
        TableColumn<String, String> imageCol = new TableColumn<>("Ảnh");
        TableColumn<String, String> nameCol = new TableColumn<>("Họ và tên");
        TableColumn<String, String> birthCol = new TableColumn<>("Ngày sinh");
        TableColumn<String, String> classCol = new TableColumn<>("Lớp học");
        TableColumn<String, String> phoneCol = new TableColumn<>("Điện thoại");
        TableColumn<String, String> parentCol = new TableColumn<>("Phụ huynh");
        TableColumn<String, String> statusCol = new TableColumn<>("Trạng thái");
        TableColumn<String, String> advisorCol = new TableColumn<>("Tư vấn viên");
        TableColumn<String, String> scheduleCol = new TableColumn<>("Hẹn lịch");
        TableColumn<String, String> detailCol = new TableColumn<>("Chi tiết");

        table.getColumns().addAll(sttCol, imageCol, nameCol, birthCol, classCol, phoneCol, parentCol, statusCol, advisorCol, scheduleCol, detailCol);
        return table;
    }

    // Tạo Footer
    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(10));
        footer.setStyle("-fx-background-color: #f5f5f5; -fx-alignment: center;");

        Label copyright = new Label("Copyright © 2025 - CÔNG TY CỔ PHẦN GIÁO DỤC AILEARN. ALL RIGHTS RESERVED.");
        footer.getChildren().add(copyright);
        return footer;
    }
}
