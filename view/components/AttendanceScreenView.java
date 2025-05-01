package view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import src.controller.AttendanceController;

import view.BaseScreenView;
import src.model.ClassSession;
import src.model.attendance.Attendance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Màn hình Điểm danh học sinh
 * Hiển thị thông tin chi tiết về lớp học và cho phép giáo viên điểm danh
 */
public class AttendanceScreenView extends BaseScreenView {

    // Constants
    private static final String PRIMARY_COLOR = "#1976D2"; // Blue color for buttons and headers
    private static final String SELECTED_DAY_COLOR = "#6667AB"; // Purple color for selected day
    private static final String GREEN_COLOR = "#4CAF50"; // Green color for progress bar
    private static final String LIGHT_GRAY = "#F5F5F5"; // Light gray for background
    private static final String WHITE_COLOR = "#FFFFFF"; // White color
    private static final String BORDER_COLOR = "#E0E0E0"; // Border color
    private static final String YELLOW_COLOR = "#FFC107"; // Yellow for excused absences
    private static final String RED_COLOR = "#F44336"; // Red for unexcused absences

    // UI Components
    private Label titleLabel;
    private HBox dayFilterBox;
    private HBox statusFilterBox;
    private TextField searchField;
    private Button searchButton;
    private Button exportExcelButton;
    private Button attendanceListButton;
    private VBox classesContainer;
    private FlowPane cardsPane;

    // Day filter buttons
    private ToggleGroup dayToggleGroup;
    private List<ToggleButton> dayButtons;

    // Status filter buttons
    private Button allButton;
    private Button unmarkedButton;
    private Button markedButton;

    // Data
    private List<ClassSession> sessions;
    private Map<Long, List<Attendance>> sessionAttendanceMap;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    //controller
    private AttendanceController attendanceController;

    public AttendanceScreenView() {
        super("Điểm danh", "attendance");
        sessions = new ArrayList<>();
        sessionAttendanceMap = new HashMap<>();
        //attendanceController = new AttendanceController(this);
        initializeView();
    }

    @Override
    public void initializeView() {
        // Clear root
        root.getChildren().clear();

        // Setting up the root container
        root.setSpacing(0);
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");

        // Tạo một BorderPane làm layout chính
        BorderPane mainLayout = new BorderPane();

        // Tạo VBox để chứa tất cả phần tử theo thứ tự từ trên xuống dưới
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));

        // Create title bar with buttons
        HBox titleBar = createTitleBar();

        // Create day filter section
        createDayFilterSection();

        // Create status filter section
        createStatusFilterSection();

        // Add components to contentBox in order
        contentBox.getChildren().addAll(
                titleBar,
                dayFilterBox,
                statusFilterBox
        );

        // Đặt contentBox vào phần TOP của BorderPane
        mainLayout.setTop(contentBox);

        // Create class cards container với ScrollPane
        ScrollPane cardsScrollPane = createClassCardsContainer();

        // Đặt cardsScrollPane vào phần CENTER của BorderPane
        mainLayout.setCenter(cardsScrollPane);

        // Thêm mainLayout vào root
        root.getChildren().add(mainLayout);

        // Đảm bảo mainLayout lấp đầy không gian có sẵn
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Set up event handlers
        setupEventHandlers();
    }

    /**
     * Creates the title bar with title and action buttons
     */
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;");

        // Title
        titleLabel = new Label("Điểm danh");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        // Add a spacer to push the buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Action buttons
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        // Attendance List button - Changed to "Danh sách vắng học" to match Attendance model's absence tracking
        attendanceListButton = createStyledButton("Danh sách vắng học", PRIMARY_COLOR);
        attendanceListButton.setOnAction(e ->{
            navigationController.navigateTo("absence-call-view");
        });

        // Export Excel button
        exportExcelButton = createStyledButton("Xuất excel", PRIMARY_COLOR);

        buttonsBox.getChildren().addAll(attendanceListButton, exportExcelButton);
        titleBar.getChildren().addAll(titleLabel, spacer, buttonsBox);

        return titleBar;
    }

    /**
     * Creates a styled button with rounded corners
     */
    private Button createStyledButton(String text, String bgColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20;"
        );
        return button;
    }

    /**
     * Creates the day filter section with toggle buttons
     */
    private void createDayFilterSection() {
        dayFilterBox = new HBox();
        dayFilterBox.setSpacing(10);
        dayFilterBox.setPadding(new Insets(10, 0, 20, 0));
        dayFilterBox.setAlignment(Pos.CENTER);

        dayToggleGroup = new ToggleGroup();
        dayButtons = new ArrayList<>();

        // Day options based on the screenshot
        String[] days = {"Tất cả", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};

        for (String day : days) {
            ToggleButton dayButton = new ToggleButton(day);
            dayButton.setToggleGroup(dayToggleGroup);
            dayButton.setUserData(day);

            // Apply style
            if (day.equals("Tất cả")) {
                dayButton.setStyle(
                        "-fx-background-color: " + SELECTED_DAY_COLOR + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 30;" +
                                "-fx-padding: 8 20;"
                );
            } else {
                dayButton.setStyle(
                        "-fx-background-color: " + LIGHT_GRAY + ";" +
                                "-fx-text-fill: #555555;" +
                                "-fx-background-radius: 30;" +
                                "-fx-padding: 8 20;"
                );
            }

            // Style for selected state
            dayButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    dayButton.setStyle(
                            "-fx-background-color: " + SELECTED_DAY_COLOR + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-padding: 8 20;"
                    );
                } else {
                    dayButton.setStyle(
                            "-fx-background-color: " + LIGHT_GRAY + ";" +
                                    "-fx-text-fill: #555555;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-padding: 8 20;"
                    );
                }
            });

            dayButtons.add(dayButton);
            dayFilterBox.getChildren().add(dayButton);
        }

        // Select first button by default
        if (!dayButtons.isEmpty()) {
            dayButtons.get(0).setSelected(true);
        }
    }

    /**
     * Creates the status filter section with buttons and search field
     * Updated to reflect Attendance model statuses
     */
    private void createStatusFilterSection() {
        statusFilterBox = new HBox();
        statusFilterBox.setSpacing(5);
        statusFilterBox.setPadding(new Insets(0, 0, 20, 0));
        statusFilterBox.setAlignment(Pos.CENTER_LEFT);

        // Create leftmost status filter buttons
        HBox leftStatusBox = new HBox(5);

        // Styling from the screenshot - using blue for "Tất cả" button
        allButton = new Button("Tất cả: 4");
        allButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Updated button text to match Attendance model terminology
        unmarkedButton = new Button("Chưa điểm danh: 2");
        unmarkedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Updated button text to match Attendance model terminology
        markedButton = new Button("Đã điểm danh: 2");
        markedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        leftStatusBox.getChildren().addAll(allButton, unmarkedButton, markedButton);

        // Create search field and button
        HBox searchBox = new HBox(0);
        searchBox.setAlignment(Pos.CENTER_RIGHT);

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm");
        searchField.setPrefWidth(220);
        searchField.setPadding(new Insets(8, 10, 8, 10));
        searchField.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-background-radius: 30 0 0 30;" +
                        "-fx-border-width: 0;"
        );

        searchButton = new Button("🔍");
        searchButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-background-radius: 0 30 30 0;" +
                        "-fx-padding: 8 15;"
        );

        searchBox.getChildren().addAll(searchField, searchButton);

        // Add a spacer to push the search box to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Build the final HBox
        statusFilterBox.getChildren().addAll(leftStatusBox, spacer, searchBox);
    }

    /**
     * Creates the container for class cards
     */
    private ScrollPane createClassCardsContainer() {
        classesContainer = new VBox();
        classesContainer.setSpacing(20);
        classesContainer.setPadding(new Insets(10, 20, 20, 20));

        cardsPane = new FlowPane();
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        cardsPane.setPrefWrapLength(1200); // Adjust based on your screen size

        classesContainer.getChildren().add(cardsPane);

        // Create ScrollPane
        ScrollPane scrollPane = new ScrollPane(classesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPadding(new Insets(0));
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scrollPane;
    }

    /**
     * Creates a card for a class session with attendance status information
     * Updated to reflect Attendance model
     */
    private VBox createClassCard(ClassSession session) {
        // Main card container
        VBox card = new VBox();
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setPadding(new Insets(0));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        // Add drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.1));
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(2);
        dropShadow.setRadius(4);
        card.setEffect(dropShadow);

        // Header with class name (blue bar)
        Label className = new Label(session.getCourseName());
        className.setFont(Font.font("System", FontWeight.BOLD, 18));
        className.setTextFill(Color.WHITE);
        className.setPadding(new Insets(15, 20, 15, 20));
        className.setPrefWidth(Double.MAX_VALUE);
        className.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-background-radius: 8 5 0 0;"
        );

        // Content grid for class information - matching the layout in the screenshot
        GridPane contentGrid = new GridPane();
        contentGrid.setPadding(new Insets(15, 20, 15, 20));
        contentGrid.setVgap(10);
        contentGrid.setHgap(10);

        // Add labels and values
        // Left column shows field names, right column shows values (right-aligned)
        contentGrid.add(createInfoLabel("Giảng viên:"), 0, 0);
        contentGrid.add(createInfoValue(session.getTeacher()), 1, 0);

        contentGrid.add(createInfoLabel("Phòng học:"), 0, 1);
        contentGrid.add(createInfoValue(session.getRoom()), 1, 1);

        contentGrid.add(createInfoLabel("Thời gian:"), 0, 2);
        contentGrid.add(createInfoValue(session.getTimeSlot()), 1, 2);

        contentGrid.add(createInfoLabel("Ngày:"), 0, 3);
        // Format date to match screenshot format: dd/MM/yyyy
        String formattedDate = session.getDate().format(dateFormatter);
        contentGrid.add(createInfoValue(formattedDate), 1, 3);

        contentGrid.add(createInfoLabel("Thứ:"), 0, 4);
        contentGrid.add(createInfoValue(session.getDayOfWeek()), 1, 4);

        // Get attendance stats for this session
        int[] attendanceStats = getAttendanceStats(session.getId());

        // Add new attendance summary row
        contentGrid.add(createInfoLabel("Trạng thái:"), 0, 5);

        // Create attendance status summary
        HBox attendanceStatusBox = new HBox(5);
        attendanceStatusBox.setAlignment(Pos.CENTER_RIGHT);

        if (attendanceStats[0] > 0) {
            Label presentLabel = new Label(attendanceStats[0] + " có mặt");
            presentLabel.setTextFill(Color.web(GREEN_COLOR));
            presentLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            attendanceStatusBox.getChildren().add(presentLabel);
        }

        if (attendanceStats[1] > 0) {
            Label absentExcusedLabel = new Label(attendanceStats[1] + " vắng có phép");
            absentExcusedLabel.setTextFill(Color.web(YELLOW_COLOR));
            absentExcusedLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            attendanceStatusBox.getChildren().add(absentExcusedLabel);
        }

        if (attendanceStats[2] > 0) {
            Label absentUnexcusedLabel = new Label(attendanceStats[2] + " vắng không phép");
            absentUnexcusedLabel.setTextFill(Color.web(RED_COLOR));
            absentUnexcusedLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            attendanceStatusBox.getChildren().add(absentUnexcusedLabel);
        }

        contentGrid.add(attendanceStatusBox, 1, 5);

        // Progress section matching the screenshot
        HBox progressSection = new HBox();
        progressSection.setAlignment(Pos.CENTER_LEFT);
        progressSection.setPadding(new Insets(10, 0, 0, 0));
        progressSection.setSpacing(10);

        Label progressLabel = new Label("Tiến độ:");
        progressLabel.setFont(Font.font("System", 14));
        progressLabel.setTextFill(Color.rgb(100, 100, 100));

        // Create a progress bar as shown in the screenshot
        StackPane progressBar = createProgressBar(session.getId());

        progressSection.getChildren().addAll(progressLabel, progressBar);

        // Notification status section - new addition based on Attendance model
        HBox notificationSection = new HBox();
        notificationSection.setAlignment(Pos.CENTER_LEFT);
        notificationSection.setPadding(new Insets(5, 0, 0, 0));
        notificationSection.setSpacing(10);

        // Only show if there are absences
        if (attendanceStats[1] + attendanceStats[2] > 0) {
            Label notificationLabel = new Label("Thông báo:");
            notificationLabel.setFont(Font.font("System", 14));
            notificationLabel.setTextFill(Color.rgb(100, 100, 100));

            // Get notification stats
            boolean allNotified = areAllAbsencesNotified(session.getId());

            Label notificationStatus = new Label(allNotified ? "Đã thông báo" : "Chưa thông báo");
            notificationStatus.setFont(Font.font("System", FontWeight.BOLD, 14));
            notificationStatus.setTextFill(allNotified ? Color.web(GREEN_COLOR) : Color.web(RED_COLOR));

            notificationSection.getChildren().addAll(notificationLabel, notificationStatus);
        }

        // Button section with "Chọn →" button
        HBox buttonSection = new HBox();
        buttonSection.setAlignment(Pos.CENTER);
        buttonSection.setPadding(new Insets(15, 0, 15, 0));

        Button chooseButton = new Button("Chọn →");
        chooseButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Set action for the choose button
        final Long sessionId = session.getId();
        chooseButton.setOnAction(e -> handleClassSelection(sessionId));

        buttonSection.getChildren().add(chooseButton);

        // Add all sections to the card
        VBox infoSection = new VBox();
        infoSection.setPadding(new Insets(0, 20, 0, 20));
        infoSection.getChildren().addAll(contentGrid, progressSection);

        // Only add notification section if there are absences
        if (attendanceStats[1] + attendanceStats[2] > 0) {
            infoSection.getChildren().add(notificationSection);
        }

        card.getChildren().addAll(className, infoSection, buttonSection);

        return card;
    }

    /**
     * Gets attendance statistics for a session
     * @param sessionId the session ID
     * @return array with [present count, absent excused count, absent unexcused count]
     */
    private int[] getAttendanceStats(Long sessionId) {
        int[] stats = new int[3]; // [present, absent excused, absent unexcused]

        List<Attendance> attendances = sessionAttendanceMap.get(sessionId);
        if (attendances == null || attendances.isEmpty()) {
            // Generate some random stats for demo purposes
            int total = 5 + (int)(Math.random() * 10); // Between 5-15 students
            stats[0] = (int)(total * (0.7 + Math.random() * 0.2)); // 70-90% present

            int absences = total - stats[0];
            stats[1] = (int)(absences * (Math.random() * 0.7)); // 0-70% of absences are excused
            stats[2] = absences - stats[1]; // Rest are unexcused

            return stats;
        }

        for (Attendance attendance : attendances) {
            if (attendance.isPresent()) {
                stats[0]++; // Present
            } else if (attendance.hasPermission()) {
                stats[1]++; // Absent with permission (excused)
            } else {
                stats[2]++; // Absent without permission (unexcused)
            }
        }

        return stats;
    }

    /**
     * Checks if all absences for a session have been notified
     * @param sessionId the session ID
     * @return true if all absences have been notified
     */
    private boolean areAllAbsencesNotified(Long sessionId) {
        List<Attendance> attendances = sessionAttendanceMap.get(sessionId);
        if (attendances == null || attendances.isEmpty()) {
            // For demo purposes, return random result
            return Math.random() > 0.5;
        }

        for (Attendance attendance : attendances) {
            if (!attendance.isPresent() && !attendance.isNotified()) {
                return false; // Found an absence that hasn't been notified
            }
        }

        return true; // All absences have been notified
    }

    /**
     * Tạo thanh tiến độ cải tiến theo yêu cầu
     */
    private StackPane createProgressBar(Long sessionId) {
        // Tạo container chính
        StackPane progressContainer = new StackPane();
        progressContainer.setPrefHeight(20);
        progressContainer.setPrefWidth(180);
        progressContainer.setMaxWidth(180);

        // Tạo giá trị tiến độ dựa trên sessionId
        int[] progressOptions = {8, 16, 24};
        int progressIndex = (int)(sessionId % 3);
        int progress = progressOptions[progressIndex];

        // Tạo background (nền xám)
        Rectangle bgRect = new Rectangle(180, 20);
        bgRect.setArcWidth(20);
        bgRect.setArcHeight(20);
        bgRect.setFill(Color.rgb(220, 220, 220));

        // Tính toán chiều rộng phần tiến độ
        double progressWidth = (progress / 40.0) * 180;

        // Tạo phần tiến độ (màu xanh)
        Rectangle progressRect = new Rectangle(progressWidth, 20);
        progressRect.setArcWidth(20);
        progressRect.setArcHeight(20);
        progressRect.setFill(Color.web(GREEN_COLOR));

        // Tạo văn bản hiển thị
        Label progressText = new Label(progress + "/40");
        progressText.setTextFill(Color.WHITE);
        progressText.setFont(Font.font("System", FontWeight.BOLD, 12));

        // Tạo container cho văn bản để đảm bảo nó chỉ hiển thị trong phần xanh
        StackPane textContainer = new StackPane(progressText);
        textContainer.setMaxWidth(progressWidth);
        textContainer.setClip(new Rectangle(progressWidth, 20));

        // Đặt các thành phần vào container
        StackPane.setAlignment(bgRect, Pos.CENTER_LEFT);
        StackPane.setAlignment(progressRect, Pos.CENTER_LEFT);
        StackPane.setAlignment(textContainer, Pos.CENTER_LEFT);

        // Thêm tất cả vào container chính
        progressContainer.getChildren().addAll(bgRect, progressRect, textContainer);

        return progressContainer;
    }

    /**
     * Creates an info label for the class card
     */
    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", 14));
        label.setTextFill(Color.rgb(100, 100, 100));
        return label;
    }

    /**
     * Creates an info value for the class card
     */
    private Label createInfoValue(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", 14));
        label.setTextFill(Color.rgb(50, 50, 50));
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHalignment(label, javafx.geometry.HPos.RIGHT);
        return label;
    }

    /**
     * Sets up event handlers for UI components
     */
    private void setupEventHandlers() {
        // Day filter buttons
        for (ToggleButton dayButton : dayButtons) {
            dayButton.setOnAction(e -> filterSessionsByDay((String) dayButton.getUserData()));
        }

        // Status filter buttons
        allButton.setOnAction(e -> {
            styleSelectedStatusButton(allButton);
            filterSessionsByStatus("all");
        });
        unmarkedButton.setOnAction(e -> {
            styleSelectedStatusButton(unmarkedButton);
            filterSessionsByStatus("unmarked");
        });
        markedButton.setOnAction(e -> {
            styleSelectedStatusButton(markedButton);
            filterSessionsByStatus("marked");
        });

        // Search button
        searchButton.setOnAction(e -> searchSessions(searchField.getText()));

        // Search field enter key
        searchField.setOnAction(e -> searchSessions(searchField.getText()));

        // Export Excel button
        exportExcelButton.setOnAction(e -> exportAttendanceData());
    }

    /**
     * Export attendance data to Excel
     * New method to utilize Attendance model
     */
    private void exportAttendanceData() {
        // This would integrate with an export service in a real implementation
        showInfo("Đang xuất dữ liệu điểm danh sang Excel...");
    }

    /**
     * Style the selected status button
     */
    private void styleSelectedStatusButton(Button selectedButton) {
        // Reset all buttons to default style
        allButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );
        unmarkedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );
        markedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Style the selected button
        selectedButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );
    }

    /**
     * Handles selection of a class for attendance
     */
    private void handleClassSelection(Long sessionId) {
        // Find the selected session from our sessions list
        ClassSession selectedSession = null;
        for (ClassSession session : sessions) {
            if (session.getId() == (sessionId)) {
                selectedSession = session;
                break;
            }
        }

        if (selectedSession != null) {
            // Store the selected session in MainController
            if (mainController != null) {
                mainController.setSessionDetail(selectedSession);

                // Pass any existing attendance records for this session
                List<Attendance> sessionAttendances = sessionAttendanceMap.get(sessionId);
                if (sessionAttendances != null) {
                    mainController.setSessionAttendances(sessionAttendances);
                }

                // Navigate to the attendance view
                mainController.navigateTo("absence-call-table");
            } else {
                // If mainController is null, show an error dialog
                showError("Không thể truy cập điểm danh: Main Controller Missing !!.");
            }
        } else {
            showError("Không tìm thấy thông tin buổi học với ID: " + sessionId);
        }
    }

    /**
     * Filters sessions by day
     */
    private void filterSessionsByDay(String day) {
        List<ClassSession> filteredSessions;

        if (day.equals("Tất cả")) {
            filteredSessions = new ArrayList<>(sessions);
        } else {
            filteredSessions = sessions.stream()
                    .filter(session -> session.getDayOfWeek().equals(day))
                    .collect(Collectors.toList());
        }

        updateClassCards(filteredSessions);

        // Update counts on buttons
        updateFilterButtonCounts(filteredSessions);
    }

    /**
     * Filters sessions by attendance status
     * Updated to use Attendance model's status
     */
    private void filterSessionsByStatus(String status) {
        List<ClassSession> filteredSessions = new ArrayList<>();

        switch (status) {
            case "all":
                filteredSessions = new ArrayList<>(sessions);
                break;

            case "unmarked":
                // Filter for sessions with no attendance records or none marked
                for (ClassSession session : sessions) {
                    List<Attendance> attendances = sessionAttendanceMap.get(session.getId());
                    if (attendances == null || attendances.isEmpty()) {
                        filteredSessions.add(session);
                    }
                }
                break;

            case "marked":
                // Filter for sessions with at least one attendance record
                for (ClassSession session : sessions) {
                    List<Attendance> attendances = sessionAttendanceMap.get(session.getId());
                    if (attendances != null && !attendances.isEmpty()) {
                        filteredSessions.add(session);
                    }
                }
                break;
        }

        updateClassCards(filteredSessions);
    }

    /**
     * Search sessions by keyword
     */
    private void searchSessions(String keyword) {
        List<ClassSession> filteredSessions;

        if (keyword == null || keyword.trim().isEmpty()) {
            filteredSessions = new ArrayList<>(sessions);
        } else {
            String lowercaseKeyword = keyword.toLowerCase();
            filteredSessions = sessions.stream()
                    .filter(session ->
                            session.getCourseName().toLowerCase().contains(lowercaseKeyword) ||
                                    session.getTeacher().toLowerCase().contains(lowercaseKeyword) ||
                                    session.getRoom().toLowerCase().contains(lowercaseKeyword))
                    .collect(Collectors.toList());
        }

        updateClassCards(filteredSessions);

        // Update counts on buttons
        updateFilterButtonCounts(filteredSessions);
    }

    /**
     * Updates the filter button text with counts
     */
    private void updateFilterButtonCounts(List<ClassSession> filteredSessions) {
        int total = filteredSessions.size();

        // Count based on actual attendance data
        int unmarked = 0;
        int marked = 0;

        for (ClassSession session : filteredSessions) {
            List<Attendance> attendances = sessionAttendanceMap.get(session.getId());
            if (attendances == null || attendances.isEmpty()) {
                unmarked++;
            } else {
                marked++;
            }
        }

        allButton.setText("Tất cả: " + total);
        unmarkedButton.setText("Chưa điểm danh: " + unmarked);
        markedButton.setText("Đã điểm danh: " + marked);
    }

    /**
     * Updates the class cards displayed
     */
    private void updateClassCards(List<ClassSession> filteredSessions) {
        cardsPane.getChildren().clear();

        if (filteredSessions.isEmpty()) {
            Label noClassesLabel = new Label("Không có lớp học nào phù hợp với bộ lọc");
            noClassesLabel.setFont(Font.font("System", 16));
            noClassesLabel.setTextFill(Color.gray(0.5));
            cardsPane.getChildren().add(noClassesLabel);
        } else {
            for (ClassSession session : filteredSessions) {
                VBox classCard = createClassCard(session);
                cardsPane.getChildren().add(classCard);
            }
        }
    }

    /**
     * Set the class session data and update UI
     */
    public void setSessions(List<ClassSession> sessions) {
        this.sessions = sessions;
        updateClassCards(sessions);
        updateFilterButtonCounts(sessions);
    }

    /**
     * Set attendance data for sessions
     * New method to support Attendance model
     */
    public void setAttendanceData(Map<Long, List<Attendance>> sessionAttendanceMap) {
        this.sessionAttendanceMap = sessionAttendanceMap;
        updateClassCards(sessions);
        updateFilterButtonCounts(sessions);
    }

    /**
     * Update attendance for a specific session
     * New method to support Attendance model
     */
    public void updateSessionAttendance(Long sessionId, List<Attendance> attendances) {
        sessionAttendanceMap.put(sessionId, attendances);
        refreshView();
    }

    /**
     * Add demo class data for testing
     * Modified to include attendance data generation
     */
    private void addDemoClasses() {
        sessions.clear();
        sessionAttendanceMap.clear();

        // Create demo sessions matching the screenshot format
        ClassSession demoSession1 = new ClassSession(
                1L,
                "LC Lớp 11A1",
                "Trần Trung Hải",
                "A101",
                LocalDate.of(2025, 5, 1),
                "Thứ 2 - 18:00, Thứ 5 - 18:00"
        );

        ClassSession demoSession2 = new ClassSession(
                2L,
                "LC Lớp 12A1",
                "Nguyễn Văn An",
                "B202",
                LocalDate.of(2025, 5, 2),
                "Thứ 3 - 18:00, Thứ 7 - 18:00"
        );

        ClassSession demoSession3 = new ClassSession(
                3L,
                "Toán Cao Cấp",
                "Lê Quang Huy",
                "C303",
                LocalDate.of(2025, 5, 3),
                "Thứ 4 - 19:30, Thứ 6 - 19:30"
        );

        ClassSession demoSession4 = new ClassSession(
                4L,
                "IELTS 6.5+",
                "Vũ Nhật Quang",
                "D404",
                LocalDate.of(2025, 5, 2),
                "Thứ 5 - 17:30, Chủ nhật - 9:00"
        );

        sessions.add(demoSession1);
        sessions.add(demoSession2);
        sessions.add(demoSession3);
        sessions.add(demoSession4);

        // Generate demo attendance data for sessions 2 and 4
        generateDemoAttendanceData(2L);
        generateDemoAttendanceData(4L);

        updateClassCards(sessions);
        updateFilterButtonCounts(sessions);
    }

    /**
     * Generate demo attendance data for a session
     * New method to support Attendance model
     */
    private void generateDemoAttendanceData(Long sessionId) {
        // This would be replaced with actual data in a real implementation
        List<Attendance> attendances = new ArrayList<>();

        // Create a few attendance records with different statuses
        for (int i = 0; i < 5; i++) {
            Attendance attendance = new Attendance();
            attendance.setId(i + 1);
            attendance.setSessionId(sessionId);

            // Randomly set attendance status
            double rand = Math.random();
            if (rand > 0.3) {
                // Present
                attendance.setPresent(true);
            } else if (rand > 0.15) {
                // Absent with permission
                attendance.setPresent(false);
                attendance.setHasPermission(true);
                attendance.setNote("Bệnh");

                // 50% chance of having called
                attendance.setCalled(Math.random() > 0.5);
            } else {
                // Absent without permission
                attendance.setPresent(false);
                attendance.setHasPermission(false);

                // 30% chance of having called
                attendance.setCalled(Math.random() > 0.7);
            }

            attendances.add(attendance);
        }

        sessionAttendanceMap.put(sessionId, attendances);
    }

    @Override
    public void refreshView() {
        if (sessions != null) {
            updateClassCards(sessions);
            updateFilterButtonCounts(sessions);
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();
        // Add demo data for testing
        addDemoClasses();
    }

    /**
     * Show information in a dialog
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error in a dialog
     */
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Getter methods for components (for controller access)
    public Button getExportExcelButton() {
        return exportExcelButton;
    }

    public Button getAttendanceListButton() {
        return attendanceListButton;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public TextField getSearchField() {
        return searchField;
    }
}
