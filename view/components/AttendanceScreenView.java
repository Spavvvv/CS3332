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
import src.model.ClassSession; // Requires ClassSession model to use String IDs
import src.model.attendance.Attendance; // Requires Attendance model to use String IDs

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.sql.SQLException;

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
    private VBox classesContainer; // Not used in the current layout
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
    // Map keys now use String for session IDs, matching the database schema and corrected DAOs
    private Map<String, List<Attendance>> sessionAttendanceMap;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Controller
    private AttendanceController attendanceController;

    public AttendanceScreenView() {
        super("Điểm danh", "attendance");
        sessions = new ArrayList<>();
        // Initialize map with String keys
        sessionAttendanceMap = new HashMap<>();

        // Initialize controller - DAOs should be managed within the controller
        attendanceController = new AttendanceController();


        initializeView();
        // Don't load data here - we'll load it in onActivate()
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
        contentBox.getChildren().addAll(titleBar, dayFilterBox, statusFilterBox);

        // Create search section
        HBox searchBox = createSearchSection();
        contentBox.getChildren().add(searchBox);

        // Create class cards container
        cardsPane = new FlowPane();
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        cardsPane.setPrefWrapLength(1200); // Set preferred wrap length

        ScrollPane scrollPane = new ScrollPane(cardsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentBox.getChildren().add(scrollPane);

        // Set the main content
        mainLayout.setCenter(contentBox);

        // Add main layout to root
        root.getChildren().add(mainLayout);

        // Setup event handlers
        setupEventHandlers();
    }

    /**
     * Sets up event handlers for buttons and filters
     */
    private void setupEventHandlers() {
        // Day filter button events
        for (ToggleButton dayButton : dayButtons) {
            dayButton.setOnAction(e -> {
                if (dayButton.isSelected()) {
                    String day = (String) dayButton.getUserData();
                    filterSessionsByDay(day);
                }
            });
        }

        // Status filter button events
        allButton.setOnAction(e -> {
            setActiveStatusButton(allButton);
            filterSessionsByStatus("ALL");
        });

        unmarkedButton.setOnAction(e -> {
            setActiveStatusButton(unmarkedButton);
            filterSessionsByStatus("UNMARKED");
        });

        markedButton.setOnAction(e -> {
            setActiveStatusButton(markedButton);
            filterSessionsByStatus("MARKED");
        });

        // Search button event
        searchButton.setOnAction(e -> {
            searchSessions(searchField.getText().trim());
        });

        // Search field enter key event
        searchField.setOnAction(e -> {
            searchSessions(searchField.getText().trim());
        });

        // Export Excel button event
        exportExcelButton.setOnAction(e -> {
            exportToExcel();
        });

        // Attendance List button event
        attendanceListButton.setOnAction(e -> {
            viewAttendanceList();
        });
    }

    /**
     * Loads data from database via controller
     * Requires ClassSession model to have String ID and ClassSessionDAO
     * and AttendanceDAO to return lists of models with String IDs.
     */
    private void loadData() {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            return;
        }
        try {
            // Use the controller to get data from database
            // Expecting ClassSession objects with String IDs from controller
            sessions = attendanceController.getAllClassSessions();

            // Get attendance data for all the sessions
            sessionAttendanceMap.clear(); // Clear previous data
            for (ClassSession session : sessions) {
                // session.getId() must return String now
                List<Attendance> attendances = attendanceController.getAttendanceBySessionId(session.getId());
                // Map key uses String
                sessionAttendanceMap.put(session.getId(), attendances);
            }

            updateClassCards(sessions);
            updateFilterButtonCounts(sessions);
        } catch (SQLException e) {
            showError("Lỗi khi kết nối với cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showError("Lỗi khi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the title bar with buttons
     */
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setSpacing(15);

        // Title label
        titleLabel = new Label("Điểm danh lớp học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#333333"));

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Export Excel button
        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 8 15;"
        );

        // Attendance List button
        attendanceListButton = new Button("Danh sách điểm danh");
        attendanceListButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 8 15;"
        );

        titleBar.getChildren().addAll(titleLabel, spacer, exportExcelButton, attendanceListButton);
        return titleBar;
    }

    /**
     * Creates the day filter section
     */
    private void createDayFilterSection() {
        dayFilterBox = new HBox(10);
        dayFilterBox.setAlignment(Pos.CENTER_LEFT);
        dayFilterBox.setPadding(new Insets(0, 0, 10, 0));

        Label dayFilterLabel = new Label("Ngày:");
        dayFilterLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        dayFilterLabel.setTextFill(Color.web("#333333"));
        dayFilterLabel.setMinWidth(60);

        dayToggleGroup = new ToggleGroup();
        dayButtons = new ArrayList<>();

        // Create day buttons for each day of the week
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String[] dayNames = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        for (int i = 0; i < days.length; i++) {
            ToggleButton dayButton = new ToggleButton(dayNames[i]);
            dayButton.setUserData(days[i]);
            dayButton.setToggleGroup(dayToggleGroup);
            dayButton.setPrefHeight(30);
            dayButton.setPrefWidth(40);
            dayButton.setStyle(
                    "-fx-background-color: " + LIGHT_GRAY + ";" +
                            "-fx-text-fill: #555555;" +
                            "-fx-background-radius: 5;" +
                            "-fx-focus-color: transparent;" +
                            "-fx-faint-focus-color: transparent;"
            );

            // Style for selected state
            dayButton.selectedProperty().addListener((obs, old, isSelected) -> {
                if (isSelected) {
                    dayButton.setStyle(
                            "-fx-background-color: " + SELECTED_DAY_COLOR + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 5;"
                    );
                } else {
                    dayButton.setStyle(
                            "-fx-background-color: " + LIGHT_GRAY + ";" +
                                    "-fx-text-fill: #555555;" +
                                    "-fx-background-radius: 5;"
                    );
                }
            });

            dayButtons.add(dayButton);
        }

        // Select today's day of the week by default
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;
        if (todayIndex >= 0 && todayIndex < dayButtons.size()) {
            dayButtons.get(todayIndex).setSelected(true);
        }

        dayFilterBox.getChildren().add(dayFilterLabel);
        dayFilterBox.getChildren().addAll(dayButtons);
    }

    /**
     * Creates the status filter section
     */
    private void createStatusFilterSection() {
        statusFilterBox = new HBox(10);
        statusFilterBox.setAlignment(Pos.CENTER_LEFT);
        statusFilterBox.setPadding(new Insets(0, 0, 10, 0));

        Label statusFilterLabel = new Label("Trạng thái:");
        statusFilterLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusFilterLabel.setTextFill(Color.web("#333333"));
        statusFilterLabel.setMinWidth(60);

        // All button
        allButton = new Button("Tất cả: 0");
        allButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Unmarked button
        unmarkedButton = new Button("Chưa điểm danh: 0");
        unmarkedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        // Marked button
        markedButton = new Button("Đã điểm danh: 0");
        markedButton.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-text-fill: #555555;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;"
        );

        statusFilterBox.getChildren().addAll(statusFilterLabel, allButton, unmarkedButton, markedButton);
    }

    /**
     * Creates the search section
     */
    private HBox createSearchSection() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm theo lớp, giáo viên...");
        searchField.setPrefHeight(35);
        searchField.setPrefWidth(300);
        searchField.setStyle(
                "-fx-background-color: " + LIGHT_GRAY + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 5 15;"
        );

        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefHeight(35);
        searchButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 5 15;"
        );

        searchBox.getChildren().addAll(searchField, searchButton);
        return searchBox;
    }

    /**
     * Creates a class card
     * Expects session to have a String ID.
     */
    private VBox createClassCard(ClassSession session) {
        // session.getId() must return String now
        String sessionId = session.getId();

        // Get attendance for session using String key
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        // Calculate attendance statistics using String session ID
        int[] attendanceStats = getAttendanceStats(sessionId);
        int presentCount = attendanceStats[0];
        int absentExcusedCount = attendanceStats[1];
        int absentUnexcusedCount = attendanceStats[2];
        // Assuming the total number of students for a session can be derived from the attendance list size
        int totalStudents = attendances.size();


        // Check if attendance has been done for this session
        // A session is considered 'marked' if there are attendance records associated with it.
        boolean isMarked = !attendances.isEmpty();


        // Check if all unexcused absences have been notified
        // Requires Attendance model to have isCalled() and hasPermission() methods
        boolean allAbsencesNotified = areAllAbsencesNotified(sessionId);

        // Create the card
        VBox card = new VBox(10);
        card.setPrefWidth(380);
        card.setPrefHeight(220); // Adjusted height slightly to fit content better
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: " + WHITE_COLOR + ";" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );

        // Add drop shadow effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.color(0, 0, 0, 0.1));
        card.setEffect(dropShadow);

        // Class name - Assuming ClassSession has getClassName() or similar
        Label classNameLabel = new Label(session.getClassName());
        classNameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        classNameLabel.setTextFill(Color.web("#333333"));
        classNameLabel.setWrapText(true); // Allow wrapping

        // Teacher name
        Label teacherLabel = new Label("Giáo viên: " + session.getTeacher());
        teacherLabel.setFont(Font.font("System", 14));
        teacherLabel.setTextFill(Color.web("#555555"));
        teacherLabel.setWrapText(true);

        // Room
        Label roomLabel = new Label("Phòng: " + session.getRoom());
        roomLabel.setFont(Font.font("System", 14));
        roomLabel.setTextFill(Color.web("#555555"));

        // Schedule - Assuming ClassSession has getSchedule() or getTimeSlot()
        Label scheduleLabel = new Label("Lịch học: " + session.getSchedule()); // Or session.getTimeSlot()
        scheduleLabel.setFont(Font.font("System", 14));
        scheduleLabel.setTextFill(Color.web("#555555"));

        // Create HBox for attendance information
        VBox attendanceInfoBox = new VBox(5); // Use VBox for stacked info and progress bar
        attendanceInfoBox.setAlignment(Pos.TOP_LEFT);

        if (isMarked) {
            // Has attendance data - show progress bar and stats
            // Attendance status label
            HBox statusLabelBox = new HBox(5); // HBox for label and optional warning icon
            statusLabelBox.setAlignment(Pos.CENTER_LEFT);

            Label attendanceStatusLabel = new Label("Điểm danh: " +
                    presentCount + " có mặt, " +
                    absentExcusedCount + " vắng có phép, " +
                    absentUnexcusedCount + " vắng không phép");
            attendanceStatusLabel.setFont(Font.font("System", 14));
            attendanceStatusLabel.setTextFill(Color.web("#555555"));
            attendanceStatusLabel.setWrapText(true);

            // If unexcused absences and not all notified, show warning icon
            // Requires Attendance model to have isCalled() and hasPermission() methods
            if (absentUnexcusedCount > 0 && !allAbsencesNotified) {
                Label warningLabel = new Label(" ⚠️"); // Added space for visual separation
                warningLabel.setFont(Font.font("System", 14));
                warningLabel.setTextFill(Color.web(RED_COLOR));
                warningLabel.setTooltip(new Tooltip("Chưa thông báo hết học sinh vắng không phép!"));
                statusLabelBox.getChildren().addAll(attendanceStatusLabel, warningLabel);
            } else {
                statusLabelBox.getChildren().add(attendanceStatusLabel);
            }

            // Create progress bar
            HBox progressBar = new HBox();
            progressBar.setPrefHeight(10); // Reduced height for sleeker look
            // Use HBox.setHgrow on the container to make it fill available width
            HBox progressBarContainer = new HBox();
            progressBarContainer.setPrefHeight(10);
            HBox.setHgrow(progressBarContainer, Priority.ALWAYS);
            progressBarContainer.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-background-radius: 5;"); // Background for total width
            progressBarContainer.setClip(new Rectangle(320, 10)); // Clip to card width

            // Create rectangles for each status - widths based on total students
            double totalWidth = 320; // Approximate width based on card size
            double presentWidth = totalStudents > 0 ? (presentCount / (double) totalStudents) * totalWidth : 0;
            double absentExcusedWidth = totalStudents > 0 ? (absentExcusedCount / (double) totalStudents) * totalWidth : 0;
            double absentUnexcusedWidth = totalStudents > 0 ? (absentUnexcusedCount / (double) totalStudents) * totalWidth : 0;

            // Ensure widths sum up to totalWidth if there are students
            double currentTotalWidth = presentWidth + absentExcusedWidth + absentUnexcusedWidth;
            if(totalStudents > 0 && currentTotalWidth > 0) {
                double scaleFactor = totalWidth / currentTotalWidth;
                presentWidth *= scaleFactor;
                absentExcusedWidth *= scaleFactor;
                absentUnexcusedWidth *= scaleFactor;
            }


            if (presentWidth > 0) {
                Rectangle presentRect = new Rectangle(presentWidth, 10);
                presentRect.setFill(Color.web(GREEN_COLOR));
                progressBarContainer.getChildren().add(presentRect);
            }

            if (absentExcusedWidth > 0) {
                Rectangle absentExcusedRect = new Rectangle(absentExcusedWidth, 10);
                absentExcusedRect.setFill(Color.web(YELLOW_COLOR));
                progressBarContainer.getChildren().add(absentExcusedRect);
            }

            if (absentUnexcusedWidth > 0) {
                Rectangle absentUnexcusedRect = new Rectangle(absentUnexcusedWidth, 10);
                absentUnexcusedRect.setFill(Color.web(RED_COLOR));
                progressBarContainer.getChildren().add(absentUnexcusedRect);
            }


            attendanceInfoBox.getChildren().addAll(statusLabelBox, progressBarContainer);
        } else {
            // No attendance data - show "Not marked" text
            Label notMarkedLabel = new Label("Chưa điểm danh");
            notMarkedLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            notMarkedLabel.setTextFill(Color.web("#555555"));
            attendanceInfoBox.getChildren().add(notMarkedLabel);
        }

        //Spacer to push the button down
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);


        // Tạo nút Điểm danh
        Button attendanceButton = new Button("Điểm danh");
        attendanceButton.setPrefHeight(35);
        attendanceButton.setMaxWidth(Double.MAX_VALUE); // Make button fill width
        attendanceButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 8 15;"
        );
        // Pass String sessionId to handler
        attendanceButton.setOnAction(e -> handleClassSelection(sessionId));


        // Add components to the card
        card.getChildren().addAll(
                classNameLabel,
                teacherLabel,
                roomLabel,
                scheduleLabel,
                attendanceInfoBox, // Add the container box
                bottomSpacer, // Add the spacer
                attendanceButton
        );

        // Make the whole card clickable (optional, button is already clickable)
        // card.setOnMouseClicked(e -> handleClassSelection(sessionId));

        return card;
    }

    /**
     * Exports attendance data to Excel
     * Using controller to handle the export logic
     */
    private void exportToExcel() {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            return;
        }
        try {
            // Pass sessions (which have String IDs) and the map (with String keys)
            boolean success = false;
            //attendanceController.exportToExcel(sessions, sessionAttendanceMap);
            if (success) {
                showInfo("Xuất dữ liệu Excel thành công!");
            } else {
                showError("Không thể xuất dữ liệu Excel. Có thể không có dữ liệu hoặc lỗi hệ thống.");
            }
        } catch (Exception e) {
            showError("Lỗi khi xuất dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows the attendance list screen
     * Using controller to handle navigation
     */
    private void viewAttendanceList() {
        if (navigationController == null) {
            showError("Bộ điều khiển điều hướng chưa được khởi tạo.");
            return;
        }
        try {
            navigationController.navigateTo("absence-call-table"); // Assuming this screen does not require parameters
        } catch (Exception e) {
            showError("Lỗi khi mở danh sách điểm danh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the active status filter button
     */
    private void setActiveStatusButton(Button selectedButton) {
        // Reset all button styles
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
     * Uses controller to navigate to attendance entry screen
     * Accepts String sessionId now.
     */
    private void handleClassSelection(String sessionId) {
        if (navigationController == null) {
            showError("Bộ điều khiển điều hướng chưa được khởi tạo.");
            return;
        }
        // Assuming absence-call-view requires the sessionId as a parameter
        // You might need to pass the session object or ID based on your navigation implementation
        try {
            // If your navigation system supports passing parameters, use it like this:
            // navigationController.navigateTo("absence-call-view", sessionId);
            // For now, navigating without parameter, assuming the target view
            // will load data based on some state or a different mechanism.
            // If the target view needs the session ID, modify navigationController.navigateTo
            // or pass the ClassSession object itself.
            showInfo("Navigating to Attendance Entry for Session ID: " + sessionId);
            // Example if navigation accepts parameters:
            // navigationController.navigateTo("absence-call-view", Map.of("sessionId", sessionId));
            navigationController.navigateTo("absence-call-view"); // Placeholder navigation
        } catch (Exception e) {
            showError("Lỗi khi chọn buổi học: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Filters sessions by day
     * Uses controller for filtering logic
     */
    private void filterSessionsByDay(String day) {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            return;
        }
        try {
            // Controller receives the full list of sessions (with String IDs) and the day string
            List<ClassSession> filteredSessions = attendanceController.filterSessionsByDay(sessions, day);
            updateClassCards(filteredSessions);
            updateFilterButtonCounts(filteredSessions);
        } catch (Exception e) {
            showError("Lỗi khi lọc buổi học theo ngày: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Filters sessions by attendance status
     * Uses controller for filtering logic
     */
    private void filterSessionsByStatus(String status) {
        // This filtering logic is kept in the view as it operates on the locally loaded 'sessions' list
        // and the 'sessionAttendanceMap', which are already managed by the view's state.
        // If this logic were complex or involved further data fetching, it might move to the controller.
        if (sessions == null || sessionAttendanceMap == null) {
            return;
        }

        List<ClassSession> filteredSessions;

        switch (status) {
            case "UNMARKED":
                filteredSessions = sessions.stream()
                        .filter(session -> {
                            // session.getId() must return String
                            List<Attendance> attendances = sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>());
                            return attendances.isEmpty();
                        })
                        .collect(Collectors.toList());
                break;
            case "MARKED":
                filteredSessions = sessions.stream()
                        .filter(session -> {
                            // session.getId() must return String
                            List<Attendance> attendances = sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>());
                            return !attendances.isEmpty();
                        })
                        .collect(Collectors.toList());
                break;
            case "ALL":
            default:
                filteredSessions = new ArrayList<>(sessions); // Return a copy to avoid modifying the original list
                break;
        }

        updateClassCards(filteredSessions);
    }

    /**
     * Search sessions by keyword
     * Uses controller for searching logic
     */
    private void searchSessions(String keyword) {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            return;
        }
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                updateClassCards(sessions); // Show all sessions if search is empty
                updateFilterButtonCounts(sessions);
                return;
            }

            // Controller searches within the current list of sessions (with String IDs)
            List<ClassSession> filteredSessions = attendanceController.searchSessions(sessions, keyword.trim());
            updateClassCards(filteredSessions);
            updateFilterButtonCounts(filteredSessions);
        } catch (Exception e) {
            showError("Lỗi khi tìm kiếm buổi học: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the filter button text with counts
     * Counts based on the provided list of filtered sessions.
     * Expects session.getId() to return String.
     */
    private void updateFilterButtonCounts(List<ClassSession> filteredSessions) {
        if (filteredSessions == null || sessionAttendanceMap == null) {
            return;
        }

        int total = filteredSessions.size();

        // Count unmarked and marked sessions within the filtered list
        int unmarked = 0;
        int marked = 0;

        for (ClassSession session : filteredSessions) {
            // session.getId() must return String
            List<Attendance> attendances = sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>());
            if (attendances.isEmpty()) {
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

        if (filteredSessions == null || filteredSessions.isEmpty()) {
            Label noClassesLabel = new Label("Không có lớp học nào phù hợp với bộ lọc");
            noClassesLabel.setFont(Font.font("System", 16));
            noClassesLabel.setTextFill(Color.gray(0.5));
            // Center the message
            VBox centerBox = new VBox(noClassesLabel);
            centerBox.setAlignment(Pos.CENTER);
            centerBox.prefWidthProperty().bind(cardsPane.widthProperty());
            cardsPane.getChildren().add(centerBox);
        } else {
            for (ClassSession session : filteredSessions) {
                VBox classCard = createClassCard(session);
                cardsPane.getChildren().add(classCard);
            }
        }
    }

    /**
     * Gets attendance statistics for a session
     * @param sessionId the session ID (String)
     * @return array with [present count, absent excused count, absent unexcused count]
     * Requires Attendance model to have isPresent(), hasPermission() methods
     */
    private int[] getAttendanceStats(String sessionId) {
        // Get attendance using String session ID
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        int presentCount = 0;
        int absentExcusedCount = 0;
        int absentUnexcusedCount = 0;

        for (Attendance attendance : attendances) {
            // Assuming Attendance model has these methods
            if (attendance.isPresent()) {
                presentCount++;
            } else if (attendance.hasPermission()) {
                absentExcusedCount++;
            } else {
                absentUnexcusedCount++;
            }
        }

        return new int[]{presentCount, absentExcusedCount, absentUnexcusedCount};
    }

    /**
     * Checks if all unexcused absences for a session have been notified
     * @param sessionId the session ID (String)
     * @return true if all unexcused absences have been notified, false otherwise
     * Requires Attendance model to have isPresent(), hasPermission(), isCalled() methods
     */
    private boolean areAllAbsencesNotified(String sessionId) {
        // Get attendance using String session ID
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        for (Attendance attendance : attendances) {
            // If not present AND (not excused OR not called), then not all absences are notified.
            // This logic seems slightly off from the variable name 'allAbsencesNotified'.
            // It checks if there's *any* unexcused absence that hasn't been called.
            // If 'allAbsencesNotified' means every unexcused absence HAS been called,
            // the condition should be: If not present AND NOT excused AND NOT called, return false.
            // Reverting to the likely intended logic: check if there is any UNEXCUSED and UNCALLED absence.
            if (!attendance.isPresent() && !attendance.hasPermission() && !attendance.isCalled()) {
                return false; // Found an unexcused, uncalled absence
            }
        }

        return true; // No unexcused, uncalled absences found
    }


    @Override
    public void refreshView() {
        loadData();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        loadData();
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
}
