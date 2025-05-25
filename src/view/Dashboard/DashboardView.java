package src.view.Dashboard;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
// Logger import không còn cần thiết nếu không dùng trực tiếp ở đây
// import java.util.logging.Logger;
// import java.util.logging.Level;
import src.controller.Dashboard.DashboardController;
import src.model.ClassSession;
import src.model.system.schedule.ScheduleItem;
import src.view.components.Screen.BaseScreenView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Dashboard src.view showing training activities overview
 */
public class DashboardView extends BaseScreenView {
    // private VBox root; // 'root' đã được kế thừa từ BaseScreenView
    // private final String viewId = "dashboard"; // viewId đã được truyền vào super constructor

    private DashboardController dashboardController;

    private Label totalStudentsLabel;
    private Label totalClassesLabel;
    private Label attendanceRateLabel;
    private PieChart courseChart;
    private VBox scheduleList;
    private VBox todayClassesList;
    private VBox legendBox; // Biến thành viên cho legend của PieChart

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Danh sách màu cho PieChart và Legend
    private static final List<Color> PIE_CHART_COLORS = List.of(
            Color.web("#1976D2"), Color.web("#42A5F5"), Color.web("#673AB7"), Color.web("#90CAF9"),
            Color.web("#F06292"), Color.web("#CE93D8"), Color.web("#FF8A65"), Color.web("#FFB74D"),
            Color.web("#AED581"), Color.web("#A1887F"), // 10 màu cơ bản
            Color.web("#FFD54F"), Color.web("#795548"), Color.web("#4DB6AC"), Color.web("#7E57C2") // Thêm vài màu nữa
    );


    public DashboardView() {
        super("Tổng quan", "dashboard");
        this.dashboardController = new DashboardController(); // Khởi tạo Controller
        initializeView(); // Gọi initializeView từ constructor của BaseScreenView hoặc ở đây
    }

    @Override
    public void initializeView() {
        // 'root' VBox đã được BaseScreenView cung cấp và cấu hình cơ bản
        // Chỉ cần clear và thêm nội dung mới nếu cần, hoặc cấu hình padding/spacing
        root.getChildren().clear(); // Xóa nội dung cũ nếu có từ BaseScreenView
        root.setSpacing(20); // Khoảng cách giữa các thành phần con
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f6f8;"); // Màu nền xám nhạt hiện đại

        VBox dashboardHeader = createDashboardHeader();
        HBox statisticsSection = createStatisticsSection();
        HBox mainContentSection = createMainContentSection();

        root.getChildren().addAll(dashboardHeader, statisticsSection, mainContentSection);
        VBox.setVgrow(mainContentSection, Priority.ALWAYS); // Cho phép main content co giãn
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
                addNewSchedule(params); // params có thể là null
                break;
            default:
                // System.out.println("Hành động không xác định trong DashboardView: " + actionId);
                break;
        }
        return null;
    }

    @Override
    public void handleSystemMessage(String message, Object data) {
        switch (message) {
            case "data-updated":
                refreshView();
                break;
            // case "user-login": break;
            // case "user-logout": break;
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    private VBox createDashboardHeader() {
        VBox header = new VBox(10); // Giảm khoảng cách một chút
        // Bỏ padding ở đây nếu root đã có padding bao ngoài
        // header.setPadding(new Insets(0 0 20 0)); // Chỉ padding bottom
        // header.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 10;"); // Bỏ màu nền riêng nếu muốn đồng bộ

        HBox titleAndButtons = new HBox(10);
        titleAndButtons.setAlignment(Pos.CENTER_LEFT);

        Label dashboardTitle = new Label("Bảng Điều Khiển Tổng Quan"); // Tiêu đề rõ ràng hơn
        dashboardTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;"); // Màu chữ hiện đại

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = createActionButton("Làm mới", "🔄", false); // Icon làm mới
        Button scheduleButton = createActionButton("Lịch học", "📅", true); // Primary button
        Button studentButton = createActionButton("Học viên", "👥", false);


        scheduleButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("schedule"); // Giả sử ID của view lịch học
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Lịch học.", null);
            }
        });

        studentButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("students"); // Giả sử ID của view danh sách học viên
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Học viên.", null);
            }
        });

        refreshButton.setOnAction(event -> refreshView());

        titleAndButtons.getChildren().addAll(dashboardTitle, spacer, refreshButton, scheduleButton, studentButton);

        // Label welcomeText = new Label("Theo dõi các hoạt động đào tạo và quản lý lớp học hiệu quả.");
        // welcomeText.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

        header.getChildren().addAll(titleAndButtons); // Bỏ welcomeText nếu không cần thiết
        return header;
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        if (root != null && root.getScene() != null) { // Gán owner cho Alert
            alert.initOwner(root.getScene().getWindow());
        }
        alert.showAndWait();
    }

    private Button createActionButton(String text, String iconText, boolean isPrimary) {
        Button button = new Button(text);
        Label icon = new Label(iconText);
        icon.setStyle("-fx-font-size: 14px;"); // Kích thước icon
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT); // Icon bên trái chữ
        button.setGraphicTextGap(5);


        String primaryStyle = "-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-weight: bold;"; // Màu xanh dương hiện đại
        String secondaryStyle = "-fx-background-color: #E0E0E0; -fx-text-fill: #333333;"; // Màu xám nhạt

        button.setStyle((isPrimary ? primaryStyle : secondaryStyle) +
                "-fx-padding: 8px 15px; -fx-background-radius: 5px; -fx-border-radius: 5px; -fx-cursor: hand;");
        button.setOnMouseEntered(e -> button.setOpacity(0.9));
        button.setOnMouseExited(e -> button.setOpacity(1.0));
        return button;
    }

    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(20); // Giữ khoảng cách giữa các card
        statsContainer.setAlignment(Pos.CENTER); // Căn giữa các card

        VBox studentStats = createStatsCard("Tổng số học viên", "0", "👥", "#27AE60"); // Icon học viên, màu xanh lá
        totalStudentsLabel = (Label) ((VBox) studentStats.getChildren().get(1)).getChildren().get(0); // Lấy valueLabel


        VBox classStats = createStatsCard("Tổng số lớp học", "0", "🏫", "#2980B9"); // Icon lớp học, màu xanh dương
        totalClassesLabel = (Label) ((VBox) classStats.getChildren().get(1)).getChildren().get(0);

        VBox attendanceStats = createStatsCard("Tỷ lệ chuyên cần", "0%", "📊", "#8E44AD"); // Icon biểu đồ, màu tím
        attendanceRateLabel = (Label) ((VBox) attendanceStats.getChildren().get(1)).getChildren().get(0);


        statsContainer.getChildren().addAll(studentStats, classStats, attendanceStats);
        HBox.setHgrow(studentStats, Priority.ALWAYS);
        HBox.setHgrow(classStats, Priority.ALWAYS);
        HBox.setHgrow(attendanceStats, Priority.ALWAYS);

        return statsContainer;
    }

    private VBox createStatsCard(String title, String initialValue, String iconText, String titleColor) {
        VBox card = new VBox(5); // Giảm khoảng cách giữa các thành phần trong card
        card.setPadding(new Insets(15)); // Giảm padding một chút
        card.setAlignment(Pos.CENTER_LEFT); // Căn trái nội dung
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 2, 2);"); // Bóng mờ hơn
        card.setMinWidth(180); // Độ rộng tối thiểu

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(iconText);
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: " + titleColor + ";"); // Màu icon theo titleColor
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");
        headerBox.getChildren().addAll(iconLabel, titleLabel);

        Label valueLabel = new Label(initialValue);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
        valueLabel.setPadding(new Insets(5,0,0,0));

        VBox valueContainer = new VBox(valueLabel); // Chỉ chứa valueLabel để dễ lấy
        card.getChildren().addAll(headerBox, valueContainer);
        return card;
    }


    private HBox createMainContentSection() {
        HBox mainContent = new HBox(20);

        VBox courseChartSection = createCourseChartSection();
        VBox scheduleSection = createScheduleSection();
        VBox todayClassesSection = createTodayClassesSection();

        mainContent.getChildren().addAll(courseChartSection, scheduleSection, todayClassesSection);
        // Thiết lập Hgrow để các cột có thể co giãn đều hoặc theo tỷ lệ mong muốn
        HBox.setHgrow(courseChartSection, Priority.SOMETIMES); // Hoặc ALWAYS nếu muốn nó chiếm nhiều hơn
        HBox.setHgrow(scheduleSection, Priority.SOMETIMES);
        HBox.setHgrow(todayClassesSection, Priority.SOMETIMES);

        // Đặt tỷ lệ cho các cột nếu cần, ví dụ cột chart chiếm 1/3, lịch hẹn 1/3, lớp học hôm nay 1/3
        courseChartSection.setMaxWidth(Double.MAX_VALUE);
        scheduleSection.setMaxWidth(Double.MAX_VALUE);
        todayClassesSection.setMaxWidth(Double.MAX_VALUE);


        return mainContent;
    }

    private VBox createCourseChartSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2);");
        section.setPrefHeight(350); // Chiều cao cố định cho section này

        // SỬA TIÊU ĐỀ
        Label sectionTitle = new Label("Thống kê Lớp học"); // Đổi tiêu đề
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        courseChart = new PieChart();
        courseChart.setLabelsVisible(false);
        courseChart.setLegendVisible(false); // Tắt legend mặc định
        courseChart.setMinHeight(180); // Chiều cao tối thiểu cho chart
        courseChart.setMaxHeight(200); // Giới hạn chiều cao

        this.legendBox = new VBox(5); // Khoảng cách giữa các mục legend
        this.legendBox.setPadding(new Insets(10, 0, 0, 5)); // Padding cho legend box

        ScrollPane legendScrollPane = new ScrollPane(this.legendBox);
        legendScrollPane.setFitToWidth(true);
        legendScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        legendScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        legendScrollPane.setPrefHeight(100); // Chiều cao cho phần legend có scroll
        legendScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");


        section.getChildren().addAll(sectionTitle, courseChart, legendScrollPane); // Thêm scrollPane chứa legendBox
        VBox.setVgrow(courseChart, Priority.NEVER); // Không cho chart co giãn quá nhiều
        VBox.setVgrow(legendScrollPane, Priority.SOMETIMES);


        return section;
    }

    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(2,0,2,0)); // Padding nhỏ cho mỗi mục

        CustomRectangle colorBox = new CustomRectangle(12, 12); // Kích thước hình chữ nhật màu
        colorBox.setFill(color);

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #333333; -fx-font-size: 11px;"); // Font nhỏ hơn cho legend
        item.getChildren().addAll(colorBox, label);
        return item;
    }

    private void applyCustomColorsToChart(PieChart chart, ObservableList<PieChart.Data> dataList) {
        if (chart == null || dataList == null) return;
        for (int i = 0; i < dataList.size(); i++) {
            PieChart.Data data = dataList.get(i);
            Node node = data.getNode();
            if (node != null) {
                Color sliceColor = PIE_CHART_COLORS.get(i % PIE_CHART_COLORS.size()); // Lấy màu từ danh sách
                node.setStyle("-fx-pie-color: " + String.format("#%02X%02X%02X",
                        (int) (sliceColor.getRed() * 255),
                        (int) (sliceColor.getGreen() * 255),
                        (int) (sliceColor.getBlue() * 255)) + ";");
            }
        }
    }

    private VBox createScheduleSection() {
        VBox section = new VBox(10); // Giảm spacing
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2);");
        section.setPrefHeight(350);


        HBox headerBox = new HBox(); // Không cần spacing ở đây nếu dùng spacer
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Lịch trình/Sự kiện"); // Tiêu đề rõ hơn
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("+ Thêm mới");
        addButton.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-font-size: 11px; -fx-background-radius: 4px; -fx-cursor:hand;");
        addButton.setOnAction(event -> addNewSchedule(null));
        headerBox.getChildren().addAll(sectionTitle, spacer, addButton);

        scheduleList = new VBox(10); // Khoảng cách giữa các item
        ScrollPane scheduleScrollPane = new ScrollPane(scheduleList);
        scheduleScrollPane.setFitToWidth(true);
        scheduleScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scheduleScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scheduleScrollPane.setPrefHeight(250); // Chiều cao cho phần scroll
        scheduleScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");


        updateScheduleList(); // Tải dữ liệu ban đầu

        section.getChildren().addAll(headerBox, scheduleScrollPane);
        VBox.setVgrow(scheduleScrollPane, Priority.ALWAYS);
        return section;
    }

    private VBox createTodayClassesSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2);");
        section.setPrefHeight(350);


        Label sectionTitle = new Label("Lớp học hôm nay");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        todayClassesList = new VBox(10);
        ScrollPane todayClassesScrollPane = new ScrollPane(todayClassesList);
        todayClassesScrollPane.setFitToWidth(true);
        todayClassesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        todayClassesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        todayClassesScrollPane.setPrefHeight(220); // Chiều cao cho phần scroll
        todayClassesScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        updateTodayClasses(); // Tải dữ liệu ban đầu

        Button viewAllButton = new Button("Xem toàn bộ lịch học");
        viewAllButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498DB; -fx-padding: 8px 0px; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor:hand;");
        viewAllButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("schedule"); // Đảm bảo ID này đúng
            }
        });
        HBox buttonContainer = new HBox(viewAllButton);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT); // Căn nút sang phải
        buttonContainer.setPadding(new Insets(5,0,0,0));


        section.getChildren().addAll(sectionTitle, todayClassesScrollPane, buttonContainer);
        VBox.setVgrow(todayClassesScrollPane, Priority.ALWAYS);
        return section;
    }

    private HBox createClassSessionRow(ClassSession session) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 5, 8, 5)); // Tăng padding
        item.setStyle("-fx-border-color: #EAECEE; -fx-border-width: 0 0 1px 0;"); // Border dưới mỏng hơn

        String statusColor;
        String statusTooltip;
        // int sessionStatus = session.getStatus(); // Giả sử ClassSession có getStatus() trả về int (0: past, 1: ongoing, 2: upcoming)
        // Chúng ta sẽ tự xác định trạng thái dựa trên thời gian
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = session.getStartTime(); // Giả sử getStartTime trả về LocalDateTime
        LocalDateTime sessionEnd = session.getEndTime();     // Giả sử getEndTime trả về LocalDateTime

        if (sessionEnd != null && now.isAfter(sessionEnd)) {
            statusColor = "#7F8C8D"; // Xám (Past)
            statusTooltip = "Đã kết thúc";
        } else if (sessionStart != null && sessionEnd != null && now.isAfter(sessionStart) && now.isBefore(sessionEnd)) {
            statusColor = "#2ECC71"; // Xanh lá (Ongoing)
            statusTooltip = "Đang diễn ra";
        } else if (sessionStart != null && now.isBefore(sessionStart)) {
            statusColor = "#F39C12"; // Vàng (Upcoming)
            statusTooltip = "Sắp diễn ra";
        } else {
            statusColor = "#BDC3C7"; // Xám nhạt (Unknown)
            statusTooltip = "Không xác định";
        }


        CustomCircle statusDot = new CustomCircle(6); // Chấm tròn to hơn chút
        statusDot.setFill(Color.web(statusColor));
        Tooltip.install(statusDot, new Tooltip(statusTooltip));


        VBox classDetails = new VBox(2);
        Label classNameLabel = new Label(session.getCourse().getCourseName() != null ? session.getCourse().getCourseName() : "N/A");
        classNameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        String timeString = (session.getStartTime() != null ? session.getStartTime().toLocalTime().format(TIME_FORMATTER) : "N/A") +
                " - " +
                (session.getEndTime() != null ? session.getEndTime().toLocalTime().format(TIME_FORMATTER) : "N/A");
        Label timeLabel = new Label(timeString + (session.getRoom() != null ? " @ P."+session.getRoom() : ""));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");
        classDetails.getChildren().addAll(classNameLabel, timeLabel);


        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsButton = new Button("Chi tiết");
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #3498DB; -fx-font-size: 11px; -fx-font-weight:bold; -fx-cursor:hand;");
        detailsButton.setOnAction(event -> showClassDetails(session.getId())); // Giả sử session.getId() trả về session_id

        item.getChildren().addAll(statusDot, classDetails, spacer, detailsButton);
        return item;
    }

    private HBox createScheduleItemRow(ScheduleItem item) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8,5,8,5));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: #EAECEE; -fx-border-width: 0 0 1px 0;");


        VBox dateTimeBox = new VBox(2); // Khoảng cách nhỏ giữa ngày và giờ
        Label dateLabel = new Label(item.getStartTime().toLocalDate().format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        Label timeLabel = new Label(item.getStartTime().toLocalTime().format(TIME_FORMATTER) + " - " + item.getEndTime().toLocalTime().format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");
        dateTimeBox.getChildren().addAll(dateLabel, timeLabel);
        dateTimeBox.setPrefWidth(100); // Độ rộng cố định cho cột thời gian

        Label titleLabel = new Label(item.getName()); // Giả sử ScheduleItem có getName()
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);


        // Optional: Description
        // if (item.getDescription() != null && !item.getDescription().isEmpty()) {
        //     Label descLabel = new Label(item.getDescription());
        //     // ... style descLabel ...
        //     VBox textContent = new VBox(titleLabel, descLabel);
        //     row.getChildren().addAll(dateTimeBox, textContent);
        // } else {
        row.getChildren().addAll(dateTimeBox, titleLabel);
        // }
        return row;
    }

    @Override
    public Node getRoot() {
        return root; // Đảm bảo root được khởi tạo và trả về từ BaseScreenView hoặc ở đây
    }

    @Override
    public void onActivate() {
        refreshView();
    }

    @Override
    public void refreshView() {
        if (dashboardController == null) {
            // System.err.println("DashboardController is null in refreshView. Cannot refresh.");
            showErrorAlert("Lỗi Hệ Thống", "Không thể làm mới dữ liệu.", "Controller chưa được khởi tạo.");
            return;
        }
        try {
            dashboardController.refreshDashboard(); // Controller sẽ gọi model để tải lại
            updateStatistics();
            updateCourseChart();
            updateScheduleList();
            updateTodayClasses();
        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console để debug
            showErrorAlert("Lỗi Làm Mới Dữ Liệu", "Đã có lỗi xảy ra khi làm mới thông tin.", "Chi tiết: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        if (dashboardController == null || dashboardController.getModel() == null ||
                totalStudentsLabel == null || totalClassesLabel == null || attendanceRateLabel == null) {
            // System.err.println("Cannot update statistics: controller, model or labels are null.");
            return;
        }
        totalStudentsLabel.setText(String.valueOf(dashboardController.getModel().getTotalStudents()));
        totalClassesLabel.setText(String.valueOf(dashboardController.getModel().getTotalClasses())); // getTotalClasses() giờ là getTotalCourses()
        attendanceRateLabel.setText(String.format(Locale.US, "%.1f%%", dashboardController.getModel().getAttendanceRate()));
    }

    private void updateCourseChart() {
        if (dashboardController == null || dashboardController.getModel() == null || courseChart == null || legendBox == null) {
            // System.err.println("Cannot update course chart: controller, model, chart or legendBox is null.");
            return;
        }

        ObservableList<PieChart.Data> rawPieData = dashboardController.getModel().getCourseDistribution();
        ObservableList<PieChart.Data> processedPieData = FXCollections.observableArrayList();
        legendBox.getChildren().clear();

        if (rawPieData == null || rawPieData.isEmpty()) {
            courseChart.setData(FXCollections.observableArrayList());
            Label noDataLabel = new Label("Không có dữ liệu phân bổ lớp học.");
            noDataLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
            legendBox.getChildren().add(noDataLabel);
            return;
        }

        List<PieChart.Data> sortedData = new ArrayList<>(rawPieData);
        sortedData.sort(Comparator.comparingDouble(PieChart.Data::getPieValue).reversed());

        int maxSlices = 9; // Số lượng lớp học hiển thị trực tiếp
        double sumOfOthers = 0;

        if (sortedData.size() <= maxSlices + 1) { // Nếu tổng số <= 10, hiển thị tất cả
            processedPieData.addAll(sortedData);
        } else { // Nếu nhiều hơn 10, hiển thị top 9 và gộp còn lại
            for (int i = 0; i < maxSlices; i++) {
                processedPieData.add(sortedData.get(i));
            }
            for (int i = maxSlices; i < sortedData.size(); i++) {
                sumOfOthers += sortedData.get(i).getPieValue();
            }
            if (sumOfOthers > 0) {
                processedPieData.add(new PieChart.Data("Các lớp khác...", sumOfOthers));
            }
        }

        courseChart.setData(processedPieData);
        applyCustomColorsToChart(courseChart, processedPieData);

        int colorIndex = 0;
        for (PieChart.Data dataSlice : processedPieData) {
            Color sliceColor = PIE_CHART_COLORS.get(colorIndex % PIE_CHART_COLORS.size());
            // Hiển thị giá trị (số lượng buổi) bên cạnh tên trong legend
            String legendText = String.format("%s (%.0f buổi)", dataSlice.getName(), dataSlice.getPieValue());
            HBox legendItem = createLegendItem(legendText, sliceColor);
            legendBox.getChildren().add(legendItem);
            colorIndex++;
        }
    }


    private void updateScheduleList() {
        if (dashboardController == null || dashboardController.getModel() == null || scheduleList == null) {
            return;
        }
        scheduleList.getChildren().clear();
        List<ScheduleItem> scheduleItems = dashboardController.getModel().getScheduleItems();
        if (scheduleItems.isEmpty()) {
            Label emptyMessage = new Label("Không có lịch hẹn/sự kiện nào sắp tới.");
            emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-padding: 10px;");
            scheduleList.getChildren().add(emptyMessage);
        } else {
            for (ScheduleItem item : scheduleItems) {
                scheduleList.getChildren().add(createScheduleItemRow(item));
            }
        }
    }

    private void updateTodayClasses() {
        if (dashboardController == null || dashboardController.getModel() == null || todayClassesList == null) {
            return;
        }
        todayClassesList.getChildren().clear();
        List<ClassSession> todayClasses = dashboardController.getModel().getTodayClasses();
        if (todayClasses.isEmpty()) {
            Label emptyMessage = new Label("Không có lớp học nào diễn ra hôm nay.");
            emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-padding: 10px;");
            todayClassesList.getChildren().add(emptyMessage);
        } else {
            for (ClassSession session : todayClasses) {
                todayClassesList.getChildren().add(createClassSessionRow(session));
            }
        }
    }

    @Override
    public boolean onDeactivate() {
        return true;
    }

    private void showClassDetails(String sessionId) { // sessionId từ ClassSession
        if (dashboardController == null) return;
        try {
            ClassSession foundSession = dashboardController.findClassSessionById(sessionId);
            if (foundSession != null) {
                // Giả sử bạn có một cách để hiển thị chi tiết ClassSession
                // Hoặc nếu bạn muốn hiển thị chi tiết Course từ ClassSession:
                // Course courseOfSession = foundSession.getCourse(); // Nếu ClassSession có getCourse()
                // if (courseOfSession != null && courseOfSession.getCourseId() != null) {
                //    // Gọi dialog chi tiết Course
                // }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Chi tiết Buổi học");
                alert.setHeaderText("Thông tin buổi học ID: " + sessionId);
                alert.setContentText(
                        "Lớp: " + foundSession.getCourse().getCourseName() + "\n" +
                                "Giáo viên: " + foundSession.getTeacher() + "\n" +
                                "Phòng: " + foundSession.getRoom() + "\n" +
                                "Thời gian: " + (foundSession.getStartTime() != null ? foundSession.getStartTime().toLocalTime().format(TIME_FORMATTER) : "N/A") +
                                " - " + (foundSession.getEndTime() != null ? foundSession.getEndTime().toLocalTime().format(TIME_FORMATTER) : "N/A")
                );
                alert.showAndWait();

                // Nếu bạn có mainController và navigationController để điều hướng:
                // if (mainController != null) {
                //     mainController.setSessionDetail(foundSession);
                // }
                // if (navigationController != null) {
                //     navigationController.navigateTo("details_session_view_id"); // ID của view chi tiết buổi học
                // }

            } else {
                showErrorAlert("Lỗi", "Không tìm thấy buổi học", "ID: " + sessionId);
            }
        } catch (Exception e) {
            showErrorAlert("Lỗi", "Lỗi khi tìm buổi học", "Chi tiết: " + e.getMessage());
        }
    }

    private void addNewSchedule(Object scheduleData) { // scheduleData không được sử dụng ở đây
        Dialog<ScheduleData> dialog = new Dialog<>();
        dialog.setTitle("Thêm Lịch hẹn/Sự kiện Mới");
        dialog.setHeaderText("Nhập thông tin chi tiết");
        if (root != null && root.getScene() != null) { // Gán owner
            dialog.initOwner(root.getScene().getWindow());
        }

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        ((Button) dialog.getDialogPane().lookupButton(saveButtonType)).setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight:bold;");


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10)); // Giảm padding phải

        TextField titleField = new TextField();
        titleField.setPromptText("Tiêu đề sự kiện");
        titleField.setMinWidth(280);
        TextArea descriptionArea = new TextArea(); // Dùng TextArea cho mô tả dài
        descriptionArea.setPromptText("Mô tả chi tiết (nếu có)");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(3);


        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm (vd: 14:30)");

        grid.add(new Label("Tiêu đề:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Ngày:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Thời gian:"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(titleField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (titleField.getText().trim().isEmpty() || datePicker.getValue() == null || timeField.getText().trim().isEmpty()) {
                    showErrorAlert("Thiếu thông tin", "Tiêu đề, Ngày và Thời gian là bắt buộc.", null);
                    return null; // Ngăn dialog đóng
                }
                try {
                    LocalTime time = LocalTime.parse(timeField.getText().trim(), TIME_FORMATTER);
                    return new ScheduleData(titleField.getText().trim(), descriptionArea.getText().trim(), datePicker.getValue(), time);
                } catch (Exception e) {
                    showErrorAlert("Lỗi Định Dạng", "Thời gian không hợp lệ.", "Vui lòng nhập theo định dạng HH:mm (ví dụ: 14:30).");
                    return null; // Ngăn dialog đóng
                }
            }
            return null;
        });

        Optional<ScheduleData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            if (dashboardController != null) {
                boolean success = dashboardController.addScheduleItem(data.name, data.description, data.date, data.time);
                if (success) {
                    refreshView(); // Làm mới để hiển thị lịch trình mới
                } else {
                    showErrorAlert("Lỗi Lưu", "Không thể thêm lịch hẹn.", "Vui lòng thử lại sau.");
                }
            }
        });
    }

    private static class ScheduleData {
        String name;
        String description;
        LocalDate date;
        LocalTime time;
        public ScheduleData(String name, String description, LocalDate date, LocalTime time) {
            this.name = name; this.description = description; this.date = date; this.time = time;
        }
    }

    private static class CustomRectangle extends javafx.scene.shape.Rectangle {
        public CustomRectangle(double width, double height) {
            super(width, height);
            setArcWidth(4);
            setArcHeight(4);
        }
    }

    private static class CustomCircle extends javafx.scene.shape.Circle {
        public CustomCircle(double radius) {
            super(radius);
        }
    }
}