package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;

public class UI {

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
        VBox mainContent = createMainContent();
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
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setStyle("-fx-background-color: #e0e0e0; -fx-min-width: 200px;");

        // Menu items
        Button newsButton = new Button("Nhắn tin");
        Button trainingButton = new Button("Đào tạo");
        Button studentButton = new Button("Học viên");
        studentButton.setStyle("-fx-background-color: #673ab7; -fx-text-fill: white;"); // Highlighted
        Button reportButton = new Button("Báo cáo");
        Button accountButton = new Button("Tư khoản");
        Button manageButton = new Button("Quản lý");

        sidebar.getChildren().addAll(newsButton, trainingButton, studentButton, reportButton, accountButton, manageButton);
        return sidebar;
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