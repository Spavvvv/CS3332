
package src.view.Dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import src.controller.Dashboard.DashboardController;
import src.model.ClassSession;
import src.model.system.schedule.ScheduleItem;
import src.view.components.Screen.BaseScreenView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Dashboard src.view showing training activities overview
 */
public class DashboardView extends BaseScreenView {
    private VBox root;
    private final String viewId = "dashboard";

    // Reference to controller
    private DashboardController dashboardController;

    // UI components that need updating
    private Label totalStudentsLabel;
    private Label totalClassesLabel;
    private Label attendanceRateLabel;
    private PieChart courseChart;
    private VBox scheduleList;
    private VBox todayClassesList;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initialize Dashboard View
     */
    public DashboardView() {
        super("Tổng quan", "dashboard");
        this.dashboardController = new DashboardController();
        initializeView();
    }

    @Override
    public void initializeView() {
        root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // 1. Dashboard Header
        VBox dashboardHeader = createDashboardHeader();

        // 2. Statistics Overview
        HBox statisticsSection = createStatisticsSection();

        // 3. Main content divided into 3 columns
        HBox mainContentSection = createMainContentSection();

        // Add all to root
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
        // Handle system messages
        switch (message) {
            case "data-updated":
                refreshView();
                break;
            case "user-login":
                // Handle user login
                break;
            case "user-logout":
                // Handle user logout
                break;
        }
    }

    @Override
    public boolean requiresAuthentication() {
        // Dashboard requires login
        return true;
    }

    /**
     * Create dashboard header section
     * @return VBox containing title and buttons
     */
    private VBox createDashboardHeader() {
        VBox header = new VBox(15);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #2196F3; -fx-background-radius: 10;");

        // Title and buttons
        HBox titleAndButtons = new HBox(10);
        titleAndButtons.setAlignment(Pos.CENTER_LEFT);

        // Dashboard title
        Label dashboardTitle = new Label("Dashboard");
        dashboardTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Function buttons
        Button scheduleButton = createActionButton("Lịch học", true);
        Button studentButton = createActionButton("Học viên", false);
        Button refreshButton = createActionButton("Làm mới", false);

        // Add event handler for Schedule button
        scheduleButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("schedule");
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Lịch học",
                        "Không thể thực hiện điều hướng do thiếu controller.");
            }
        });

        // Add event handler for Students button
        studentButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("students");
            } else {
                showErrorAlert("Lỗi điều hướng", "Không thể mở trang Học viên",
                        "Không thể thực hiện điều hướng do thiếu controller.");
            }
        });

        // Add event handler for Refresh button
        refreshButton.setOnAction(event -> refreshView());

        // Add elements to layout
        titleAndButtons.getChildren().addAll(dashboardTitle, spacer, refreshButton, scheduleButton, studentButton);

        // Add description line
        Label welcomeText = new Label("Xin chào! Đây là trang tổng quan giúp bạn theo dõi các hoạt động đào tạo.");
        welcomeText.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");

        header.getChildren().addAll(titleAndButtons, welcomeText);
        return header;
    }

    // Helper method to display error message
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Create action button in header
     * @param text Button content
     * @param isPrimary Define if button is primary or secondary
     * @return Styled Button
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
     * Create statistics overview section
     * @return HBox containing statistics cards
     */
    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER);

        // Statistics Card 1 - Total Students
        VBox studentStats = createStatsCard("Tổng số học viên", "0", "#4CAF50");
        totalStudentsLabel = (Label) studentStats.getChildren().get(0);

        // Statistics Card 2 - Total Classes
        VBox classStats = createStatsCard("Tổng số lớp học", "0", "#FF9800");
        totalClassesLabel = (Label) classStats.getChildren().get(0);

        // Statistics Card 3 - Attendance Rate
        VBox attendanceStats = createStatsCard("Tỷ lệ đi học", "0%", "#2196F3");
        attendanceRateLabel = (Label) attendanceStats.getChildren().get(0);

        statsContainer.getChildren().addAll(studentStats, classStats, attendanceStats);
        HBox.setHgrow(studentStats, Priority.ALWAYS);
        HBox.setHgrow(classStats, Priority.ALWAYS);
        HBox.setHgrow(attendanceStats, Priority.ALWAYS);

        return statsContainer;
    }

    /**
     * Create statistics card with title, value and color
     * @param title Statistics title
     * @param value Statistics value
     * @param color Color
     * @return VBox containing statistics card
     */
    private VBox createStatsCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Statistics value
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        // Statistics title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #757575;");

        // Add to card
        card.getChildren().addAll(valueLabel, titleLabel);
        return card;
    }

    /**
     * Create main content section divided into 3 columns
     * @return HBox containing 3 columns
     */
    private HBox createMainContentSection() {
        HBox mainContent = new HBox(20);

        // Column 1: Course chart
        VBox courseChartSection = createCourseChartSection();

        // Column 2: Schedule
        VBox scheduleSection = createScheduleSection();

        // Column 3: Today's classes
        VBox todayClassesSection = createTodayClassesSection();

        // Add to layout and configure size
        mainContent.getChildren().addAll(courseChartSection, scheduleSection, todayClassesSection);
        HBox.setHgrow(courseChartSection, Priority.ALWAYS);
        HBox.setHgrow(scheduleSection, Priority.ALWAYS);
        HBox.setHgrow(todayClassesSection, Priority.ALWAYS);

        return mainContent;
    }

    /**
     * Create course chart section
     * @return VBox containing chart and legend
     */
    private VBox createCourseChartSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label sectionTitle = new Label("Khóa học");
        // The text color is already set to black here in the original code
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: Black;");

        // Create pie chart
        courseChart = new PieChart();
        courseChart.setLabelsVisible(false);
        courseChart.setLegendVisible(false);

        // Initialize chart with empty data - will be updated in refreshView
        courseChart.setData(javafx.collections.FXCollections.observableArrayList());

        // Customize chart
        courseChart.setMaxHeight(200);

        // Create chart legend
        VBox legendBox = new VBox(10);
        legendBox.setPadding(new Insets(10, 0, 0, 0));

        // Legend items
        HBox legend1 = createLegendItem("Lớp chính - 8 buổi", Color.web("#1976D2"));
        HBox legend2 = createLegendItem("Lớp chính", Color.web("#42A5F5"));
        HBox legend3 = createLegendItem("Bổ trợ dài hạn", Color.web("#90CAF9"));
        HBox legend4 = createLegendItem("Bổ trợ ngắn hạn", Color.web("#BBDEFB"));

        legendBox.getChildren().addAll(legend1, legend2, legend3, legend4);
        section.getChildren().addAll(sectionTitle, courseChart, legendBox);

        return section;
    }

    /**
     * Create a legend item for chart
     * @param text Content
     * @param color Color
     * @return HBox containing legend item
     */
    private HBox createLegendItem(String text, Color color) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);

        CustomRectangle colorBox = new CustomRectangle();
        colorBox.setFill(color);

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: black;");
        item.getChildren().addAll(colorBox, label);

        return item;
    }

    /**
     * Apply custom colors to chart
     * @param chart Chart to customize
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
     * Create schedule section
     * @return VBox containing schedule list
     */
    private VBox createScheduleSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Title and add button
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("Lịch hẹn");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: Black;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addButton = new Button("+");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 8;");
        addButton.setOnAction(event -> {
            // Open dialog to add new schedule
            addNewSchedule(null);
        });

        headerBox.getChildren().addAll(sectionTitle, spacer, addButton);

        // Schedule list
        scheduleList = new VBox(15);

        // Temporarily add message when no schedules
        Label emptyMessage = new Label("Không có lịch hẹn sắp tới");
        emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
        scheduleList.getChildren().add(emptyMessage);

        section.getChildren().addAll(headerBox, scheduleList);
        return section;
    }

    /**
     * Create today's classes section
     * @return VBox containing class list
     */
    private VBox createTodayClassesSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        Label sectionTitle = new Label("Lớp học hôm nay");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: Black;");

        todayClassesList = new VBox(15);
        // List will be populated in updateTodayClasses()

        // View all button
        Button viewAllButton = new Button("Xem tất cả lớp học");
        viewAllButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3; -fx-padding: 8 15;");
        viewAllButton.setOnAction(event -> {
            if (navigationController != null) {
                navigationController.navigateTo("schedule");
            }
        });

        section.getChildren().addAll(sectionTitle, todayClassesList, viewAllButton);
        return section;
    }

    /**
     * Create a class item for a ClassSession
     * @param session Class session
     * @return HBox containing class info
     */
    private HBox createClassSessionRow(ClassSession session) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(5, 0, 5, 0));
        item.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");

        // Determine status color based on class status
        String statusColor;
        int status = session.getStatus();
        switch (status) {
            case 0:  // Past
                statusColor = "#2196F3"; // Blue for completed
                break;
            case 1:  // Ongoing
                statusColor = "#4CAF50"; // Green for active
                break;
            case 2:  // Upcoming
                statusColor = "#FFC107"; // Yellow for pending
                break;
            default:
                statusColor = "#757575"; // Grey for unknown
        }

        CustomCircle statusDot = new CustomCircle(5);
        statusDot.setFill(Color.web(statusColor));

        // Label for class name, aligned horizontally
        Label classNameLabel = new Label(session.getCourseName());
        // Keep existing style and add text color black
        classNameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: black;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsButton = new Button("Chi tiết");
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2196F3;");
        detailsButton.setOnAction(event -> showClassDetails(String.valueOf(session.getId())));

        // Add components directly to the HBox for horizontal alignment
        item.getChildren().addAll(statusDot, classNameLabel, spacer, detailsButton);
        return item;
    }

    /**
     * Create a schedule item for a ScheduleItem
     * @param item Schedule item
     * @return HBox containing schedule info
     */
    private HBox createScheduleItemRow(ScheduleItem item) {
        HBox row = new HBox(10);
        row.getStyleClass().add("schedule-item");

        VBox dateTimeBox = new VBox(5);
        Label dateLabel = new Label(item.getStartTime().toLocalDate().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("date-label");
        Label timeLabel = new Label(item.getStartTime().toLocalTime().format(TIME_FORMATTER));
        timeLabel.getStyleClass().add("time-label");
        dateTimeBox.getChildren().addAll(dateLabel, timeLabel);

        Label titleLabel = new Label(item.getName());
        titleLabel.getStyleClass().add("title-label");

        row.getChildren().addAll(dateTimeBox, titleLabel);
        return row;
    }

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public void onActivate() {
        // Update data when screen is activated
        refreshView();
    }

    @Override
    public void refreshView() {
        try {
            // Use controller to refresh all data
            dashboardController.refreshDashboard();

            // Update UI components with data from controller
            updateStatistics();
            updateCourseChart();
            updateScheduleList();
            updateTodayClasses();
        } catch (Exception e) {
            showErrorAlert("Error", "Error refreshing data", e.getMessage());
        }
    }

    /**
     * Update statistics from controller data
     */
    private void updateStatistics() {
        // Use controller to get model data
        totalStudentsLabel.setText(String.valueOf(dashboardController.getModel().getTotalStudents()));
        totalClassesLabel.setText(String.valueOf(dashboardController.getModel().getTotalClasses()));
        attendanceRateLabel.setText(String.format("%.1f%%", dashboardController.getModel().getAttendanceRate()));
    }

    /**
     * Update course chart from controller data
     */
    private void updateCourseChart() {
        // Use controller to get model data
        courseChart.setData(dashboardController.getModel().getCourseDistribution());
        applyCustomColorsToChart(courseChart);
    }

    /**
     * Update schedule list from controller data
     */
    private void updateScheduleList() {
        scheduleList.getChildren().clear();

        // Get schedule items from controller
        List<ScheduleItem> scheduleItems = dashboardController.getModel().getScheduleItems();

        if (scheduleItems.isEmpty()) {
            // If no schedules, display message
            Label emptyMessage = new Label("Không có lịch hẹn sắp tới");
            emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
            scheduleList.getChildren().add(emptyMessage);
        } else {
            // Display schedules
            for (ScheduleItem item : scheduleItems) {
                HBox scheduleRow = createScheduleItemRow(item);
                scheduleList.getChildren().add(scheduleRow);
            }
        }
    }

    /**
     * Update today's classes list from controller data
     */
    private void updateTodayClasses() {
        todayClassesList.getChildren().clear();

        // Get today's classes from controller
        List<ClassSession> todayClasses = dashboardController.getModel().getTodayClasses();

        if (todayClasses.isEmpty()) {
            Label emptyMessage = new Label("Không có lớp học nào hôm nay");
            emptyMessage.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
            todayClassesList.getChildren().add(emptyMessage);
        } else {
            for (ClassSession session : todayClasses) {
                HBox classRow = createClassSessionRow(session);
                todayClassesList.getChildren().add(classRow);
            }
        }
    }

    @Override
    public boolean onDeactivate() {
        // Perform necessary tasks before leaving screen
        return true; // Allow screen change
    }

    /**
     * Show class details
     * @param classId ID of the class to display
     */
    private void showClassDetails(String classId) {
        try {

            // Use controller to find the class session
            ClassSession foundSession = dashboardController.findClassSessionById(classId);

            if (foundSession != null) {
                // Save ClassSession object in MainController to access from detail screen
                if (mainController != null) {
                    mainController.setSessionDetail(foundSession);
                }

                // Navigate to class detail screen
                if (navigationController != null) {
                    navigationController.navigateTo("details-src.view");
                }
            } else {
                showErrorAlert("Lỗi", "Không tìm thấy lớp học",
                        "Không thể tìm thấy thông tin chi tiết cho lớp học với ID: " + classId);
            }
        } catch (NumberFormatException e) {
            showErrorAlert("Lỗi", "ID lớp học không hợp lệ",
                    "ID lớp học phải là một số nguyên: " + classId);
        }
    }

    /**
     * Add a new schedule
     * @param scheduleData Schedule data (not needed)
     */
    private void addNewSchedule(Object scheduleData) {
        // Create a custom dialog
        Dialog<ScheduleData> dialog = new Dialog<>();
        dialog.setTitle("Thêm lịch hẹn mới");
        dialog.setHeaderText("Nhập thông tin lịch hẹn");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Lưu", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField();
        titleField.setPromptText("Tiêu đề");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Mô tả");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");

        grid.add(new Label("Tiêu đề:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Ngày:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Thời gian:"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the title field by default
        titleField.requestFocus();

        // Convert the result to a schedule data object when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String title = titleField.getText();
                    String description = descriptionField.getText();
                    LocalDate date = datePicker.getValue();
                    LocalTime time = LocalTime.parse(timeField.getText(), TIME_FORMATTER);
                    return new ScheduleData(title, description, date, time);
                } catch (Exception e) {
                    showErrorAlert("Input Error", "Invalid input format",
                            "Please check your input values, especially the time format (HH:mm).");
                    return null;
                }
            }
            return null;
        });

        // Show the dialog and process the result
        Optional<ScheduleData> result = dialog.showAndWait();

        result.ifPresent(data -> {
            // Use controller to add the schedule item
            boolean success = dashboardController.addScheduleItem(
                    data.name,
                    data.description,
                    data.date,
                    data.time
            );

            if (success) {
                // Refresh the src.view to show the new schedule
                refreshView();
            } else {
                showErrorAlert("Error", "Failed to add schedule",
                        "Could not add the new schedule item. Please try again.");
            }
        });
    }

    /**
     * Helper class to store schedule form data
     */
    private static class ScheduleData {
        String name;
        String description;
        LocalDate date;
        LocalTime time;

        public ScheduleData(String name, String description, LocalDate date, LocalTime time) {
            this.name = name;
            this.description = description;
            this.date = date;
            this.time = time;
        }
    }

    /**
     * Custom Rectangle class for color legends
     */
    private static class CustomRectangle extends javafx.scene.shape.Rectangle {
        public CustomRectangle() {
            super(16, 16);
            setArcWidth(4);
            setArcHeight(4);
        }
    }

    /**
     * Custom Circle class for status indicators
     */
    private static class CustomCircle extends javafx.scene.shape.Circle {
        public CustomCircle(double radius) {
            super(radius);
        }
    }
}

