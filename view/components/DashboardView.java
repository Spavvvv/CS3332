package view.components;

import javafx.scene.control.Alert;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import view.BaseScreenView;
import src.controller.NavigationController;
import src.controller.MainController;
/**
 Màn hình Dashboard hiển thị tổng quan về hoạt động đào tạo
 */
public class DashboardView extends BaseScreenView {
    private VBox root;
    private final String viewId = "dashboard";
    // Các biến để lưu trữ các thành phần UI có thể cần cập nhật sau
    private Label totalStudentsLabel;
    private Label totalClassesLabel;
    private Label attendanceRateLabel;
    private PieChart courseChart;
    private VBox scheduleList;
    private VBox todayClassesList;

    // Khai báo lại các biến để sử dụng trong class này
    private NavigationController navigationController;
    private MainController mainController;

    /**
     Khởi tạo Dashboard View
     */
    public DashboardView() {
        super("Tổng quan", "dashboard");
        initializeView();
    }

    // Ghi đè để lưu lại giá trị của NavigationController
    @Override
    public void setNavigationController(NavigationController navigationController) {
        super.setNavigationController(navigationController);
        this.navigationController = navigationController;
    }

    // Ghi đè để lưu lại giá trị của MainController
    @Override
    public void setMainController(MainController mainController) {
        super.setMainController(mainController);
        this.mainController = mainController;
    }

    @Override
    public void initializeView() {
        root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");
        // 1. Header Dashboard
        VBox dashboardHeader = createDashboardHeader();


        // 2. Thống kê tổng quan
        HBox statisticsSection = createStatisticsSection();


        // 3. Phần nội dung chính chia làm 3 cột
        HBox mainContentSection = createMainContentSection();


        // Thêm tất cả vào root
        root.getChildren().addAll(dashboardHeader, statisticsSection, mainContentSection);
    }


    @Override
    public Object handleAction(String actionId, Object params) {
        switch (actionId) {
            case "refresh-data":
                refreshView();
                break;
            case "update-statistics":
                updateStatistics();
                break;
            case "update-course-chart":
                updateCourseChart();
                break;
            case "show-class-details":
                if (params instanceof String) {
                    showClassDetails((String) params);
                }
                break;
            case "add-schedule":
                addNewSchedule(params);
                break;
        }
        return null;
    }
    @Override
    public void handleSystemMessage(String message, Object data) {
// Xử lý các thông báo hệ thống
        switch (message) {
            case "data-updated":
                refreshView();
                break;
            case "user-login":
// Xử lý khi người dùng đăng nhập
                break;
            case "user-logout":
// Xử lý khi người dùng đăng xuất
                break;
        }
    }
    @Override
    public boolean requiresAuthentication() {
// Dashboard yêu cầu đăng nhập
        return true;
    }
    /**
     Tạo phần header của dashboard
     @return VBox chứa tiêu đề và các nút
     */
    private VBox createDashboardHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 10;");

        // Tiêu đề và các nút
        HBox titleAndButtons = new HBox(10);
        titleAndButtons.setAlignment(Pos.CENTER_LEFT);

        // Tiêu đề dashboard
        Label dashboardTitle = new Label("Dashboard");
        dashboardTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Spacer để đẩy các nút sang bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Các nút chức năng
        Button scheduleButton = createActionButton("Lịch học", true);
        Button studentButton = createActionButton("Học viên", false);

        // Thêm xử lý sự kiện cho nút Lịch học
        scheduleButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("schedule");
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Lịch học",
                        "Không thể thực hiện điều hướng do thiếu controller.");
            }
        });

        // Thêm xử lý sự kiện cho nút Học viên
        studentButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("students");
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Học viên",
                        "Không thể thực hiện điều hướng do thiếu controller.");
            }
        });

        // Thêm các phần tử vào layout
        titleAndButtons.getChildren().addAll(dashboardTitle, spacer, scheduleButton, studentButton);

        // Thêm dòng mô tả
        Label welcomeText = new Label("Xin chào! Đây là trang tổng quan giúp bạn theo dõi các hoạt động đào tạo.");
        welcomeText.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        header.getChildren().addAll(titleAndButtons, welcomeText);
        return header;
    }

    // Phương thức phụ trợ để hiển thị thông báo lỗi
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     Tạo nút hành động trong header
     @param text Nội dung nút
     @param isPrimary Xác định nút là primary hay secondary
     @return Button đã được style
     */
    private Button createActionButton(String text, boolean isPrimary) {
        Button button = new Button(text);
        if (isPrimary) {
            button.setStyle("-fx-background-color: white; -fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-padding: 8 15;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-text-fill: white; -fx-padding: 8 15;");
        }
        return button;
    }

    /**
     Tạo phần thống kê tổng quan
     @return HBox chứa các card thống kê
     */
    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER);
// Statistics Card 1 - Tổng số học viên
        VBox studentStats = createStatsCard("Tổng số học viên", "0", "#4CAF50");
        totalStudentsLabel = (Label) studentStats.getChildren().get(0);
// Statistics Card 2 - Tổng số lớp học
        VBox classStats = createStatsCard("Tổng số lớp học", "0", "#FF9800");
        totalClassesLabel = (Label) classStats.getChildren().get(0);
// Statistics Card 3 - Tỷ lệ đi học
        VBox attendanceStats = createStatsCard("Tỷ lệ đi học", "0%", "#2196F3");
        attendanceRateLabel = (Label) attendanceStats.getChildren().get(0);
        statsContainer.getChildren().addAll(studentStats, classStats, attendanceStats);
        HBox.setHgrow(studentStats, Priority.ALWAYS);
        HBox.setHgrow(classStats, Priority.ALWAYS);
        HBox.setHgrow(attendanceStats, Priority.ALWAYS);
        return statsContainer;
    }

    /**
     Tạo card thống kê với tiêu đề, giá trị và màu sắc
     @param title Tiêu đề thống kê
     @param value Giá trị thống kê
     @param color Màu sắc
     @return VBox chứa card thống kê
     */
    private VBox createStatsCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
// Giá trị thống kê
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
// Tiêu đề thống kê
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");
// Thêm vào card
        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    /**
     Tạo phần nội dung chính chia làm 3 cột
     @return HBox chứa 3 cột
     */
    private HBox createMainContentSection() {
        HBox mainContent = new HBox(20);
// Column 1: Biểu đồ khóa học
        VBox courseChartSection = createCourseChartSection();
// Column 2: Lịch hẹn
        VBox scheduleSection = createScheduleSection();
// Column 3: Lớp học hôm nay
        VBox todayClassesSection = createTodayClassesSection();
// Thêm vào layout và cấu hình kích thước
        mainContent.getChildren().addAll(courseChartSection, scheduleSection, todayClassesSection);
        HBox.setHgrow(courseChartSection, Priority.ALWAYS);
        HBox.setHgrow(scheduleSection, Priority.ALWAYS);
        HBox.setHgrow(todayClassesSection, Priority.ALWAYS);
        return mainContent;
    }

    /**
     Tạo phần biểu đồ khóa học
     @return VBox chứa biểu đồ và chú thích
     */
    private VBox createCourseChartSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        Label sectionTitle = new Label("Khóa học");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
// Tạo biểu đồ tròn
        courseChart = new PieChart();
        courseChart.setLabelsVisible(false);
        courseChart.setLegendVisible(false);
// Dữ liệu cho biểu đồ
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Lớp chính - 8 buổi", 40),
                new PieChart.Data("Lớp chính", 25),
                new PieChart.Data("Bổ trợ dài hạn", 20),
                new PieChart.Data("Bổ trợ ngắn hạn", 15)
        );
        courseChart.setData(pieChartData);
// Tùy chỉnh biểu đồ
        courseChart.setMaxHeight(200);
        applyCustomColorsToChart(courseChart);
// Tạo chú thích cho biểu đồ
        VBox legendBox = new VBox(10);
        legendBox.setPadding(new Insets(10, 0, 0, 0));
// Các mục chú thích
        HBox legend1 = createLegendItem("Lớp chính - 8 buổi", Color.web("#1976D2"));
        HBox legend2 = createLegendItem("Lớp chính", Color.web("#42A5F5"));
        HBox legend3 = createLegendItem("Bổ trợ dài hạn", Color.web("#90CAF9"));
        HBox legend4 = createLegendItem("Bổ trợ ngắn hạn", Color.web("#BBDEFB"));
        legendBox.getChildren().addAll(legend1, legend2, legend3, legend4);
        section.getChildren().addAll(sectionTitle, courseChart, legendBox);
        return section;
    }

    /**
     Tạo một mục chú thích cho biểu đồ
     @param text Nội dung
     @param color Màu sắc
     @return HBox chứa mục chú thích
     */
    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        CustomRectangle colorBox = new CustomRectangle();
        colorBox.setFill(color);
        Label label = new Label(text);
        item.getChildren().addAll(colorBox, label);
        return item;
    }

    /**
     Áp dụng màu sắc tùy chỉnh cho biểu đồ
     @param chart Biểu đồ cần tùy chỉnh
     */
    private void applyCustomColorsToChart(PieChart chart) {
        int i = 0;
        for (PieChart.Data data : chart.getData()) {
            String color;
            switch (i) {
                case 0: color = "#1976D2"; break;
                case 1: color = "#42A5F5"; break;
                case 2: color = "#90CAF9"; break;
                case 3: color = "#BBDEFB"; break;
                default: color = "#2196F3"; break;
            }
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
            i++;
        }
    }

    /**
     Tạo phần lịch hẹn
     @return VBox chứa danh sách lịch hẹn
     */
    private VBox createScheduleSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
// Tiêu đề và nút thêm
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label sectionTitle = new Label("Lịch hẹn");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addButton = new Button("+");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 8;");
        headerBox.getChildren().addAll(sectionTitle, spacer, addButton);
// Danh sách lịch hẹn
        scheduleList = new VBox(15);
// Tạm thời thêm thông báo khi không có lịch hẹn
        Label emptyMessage = new Label("Không có lịch hẹn sắp tới");
        emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
        scheduleList.getChildren().add(emptyMessage);
        section.getChildren().addAll(headerBox, scheduleList);
        return section;
    }

    /**
     Tạo phần lớp học hôm nay
     @return VBox chứa danh sách lớp học
     */
    private VBox createTodayClassesSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        Label sectionTitle = new Label("Lớp học hôm nay");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        todayClassesList = new VBox(15);
// Lớp học 1
        HBox class1 = createClassItem("08:15", "Lớp 12A1", "#4CAF50");
// Lớp học 2
        HBox class2 = createClassItem("10:00", "Lớp 12A2", "#6200EE");
        todayClassesList.getChildren().addAll(class1, class2);
// Nút xem tất cả
        Button viewAllButton = new Button("Xem tất cả lớp học");
        viewAllButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3; -fx-padding: 8 15;");
        section.getChildren().addAll(sectionTitle, todayClassesList, viewAllButton);
        return section;
    }

    /**
     Tạo một mục lớp học
     @param time Thời gian
     @param className Tên lớp
     @param statusColor Màu trạng thái
     @return HBox chứa thông tin lớp học
     */
    private HBox createClassItem(String time, String className, String statusColor) {
        HBox classItem = new HBox(10);
        classItem.setAlignment(Pos.CENTER_LEFT);
        classItem.setPadding(new Insets(5, 0, 5, 0));
        classItem.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
        CustomCircle statusDot = new CustomCircle(5);
        statusDot.setFill(Color.web(statusColor));
        VBox classDetails = new VBox(5);
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-weight: bold;");
        Label classNameLabel = new Label(className);
        classDetails.getChildren().addAll(timeLabel, classNameLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button detailsButton = new Button("Chi tiết");
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3;");
        classItem.getChildren().addAll(statusDot, classDetails, spacer, detailsButton);
        return classItem;
    }

    @Override
    public Node getRoot() {
        return root;
    }
    @Override
    public void onActivate() {
// Cập nhật dữ liệu khi màn hình được kích hoạt
        refreshView();
    }
    @Override
    public void refreshView() {
// Làm mới dữ liệu, có thể gọi API hoặc lấy dữ liệu từ service
        updateStatistics();
        updateCourseChart();
        updateScheduleList();
        updateTodayClasses();
    }
    /**
     Cập nhật số liệu thống kê
     */
    private void updateStatistics() {
// Trong thực tế, dữ liệu này sẽ được lấy từ service hoặc controller
        totalStudentsLabel.setText("284");
        totalClassesLabel.setText("105");
        attendanceRateLabel.setText("84%");
    }

    /**
     Cập nhật biểu đồ khóa học
     */
    private void updateCourseChart() {
// Trong thực tế, dữ liệu này sẽ được lấy từ service hoặc controller
        ObservableList<PieChart.Data> updatedData = FXCollections.observableArrayList(
                new PieChart.Data("Lớp chính - 8 buổi", 40),
                new PieChart.Data("Lớp chính", 25),
                new PieChart.Data("Bổ trợ dài hạn", 20),
                new PieChart.Data("Bổ trợ ngắn hạn", 15)
        );
        courseChart.setData(updatedData);
        applyCustomColorsToChart(courseChart);
    }

    /**
     Cập nhật danh sách lịch hẹn
     */
    private void updateScheduleList() {
// Trong thực tế, dữ liệu này sẽ được lấy từ service hoặc controller
// Nếu có lịch hẹn, xóa thông báo "không có lịch hẹn" và thêm các mục lịch hẹn
    }

    /**
     Cập nhật danh sách lớp học hôm nay
     */
    private void updateTodayClasses() {
// Trong thực tế, dữ liệu này sẽ được lấy từ service hoặc controller
    }

    @Override
    public boolean onDeactivate() {
// Thực hiện các tác vụ cần thiết trước khi rời khỏi màn hình
// Ví dụ: lưu trạng thái, xác nhận thay đổi chưa lưu, v.v.
        return true; // Cho phép chuyển màn hình
    }
    /**
     Hiển thị chi tiết của một lớp học
     @param classId ID của lớp học cần hiển thị
     */
    private void showClassDetails(String classId) {
// Trong thực tế, sẽ hiển thị chi tiết của lớp học dựa trên ID
        System.out.println("Hiển thị chi tiết lớp: " + classId);
// Có thể mở một dialog hoặc chuyển đến màn hình chi tiết
    }

    /**
     Thêm một lịch hẹn mới
     @param scheduleData Dữ liệu lịch hẹn
     */
    private void addNewSchedule(Object scheduleData) {
// Trong thực tế, sẽ thêm lịch hẹn mới vào danh sách
// Có thể làm mới giao diện sau khi thêm
        System.out.println("Thêm lịch hẹn mới: " + scheduleData);
    }

    /**
     Lớp helper để tạo hình chữ nhật màu
     */
    private class CustomRectangle extends Region {
        public CustomRectangle() {
            this.setMinSize(15, 15);
            this.setMaxSize(15, 15);
        }
        public void setFill(Color color) {
            this.setStyle("-fx-background-color: " + toRGBCode(color) + ";");
        }
        private String toRGBCode(Color color) {
            return String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
    }

    /**
     Lớp helper để tạo hình tròn màu
     */
    private class CustomCircle extends Region {
        public CustomCircle(double radius) {
            this.setMinSize(radius * 2, radius * 2);
            this.setMaxSize(radius * 2, radius * 2);
        }
        public void setFill(Color color) {
            this.setStyle("-fx-background-color: " + toRGBCode(color) + "; -fx-background-radius: 50%;");
        }
        private String toRGBCode(Color color) {
            return String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }
    }
}


