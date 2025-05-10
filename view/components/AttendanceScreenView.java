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


import src.dao.AttendanceDAO;
import src.dao.StudentDAO;
import src.dao.ClassSessionDAO;

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


    //DAO (idk why it's here ?)
    private AttendanceDAO AttendanceDAO;
    private StudentDAO StudentDAO;
    private ClassSessionDAO classSessionDAO;

    // Controller
    private AttendanceController attendanceController;

    public AttendanceScreenView() throws SQLException {
        super("Điểm danh", "attendance");
        sessions = new ArrayList<>();
        sessionAttendanceMap = new HashMap<>();


        // Initialize controller with DAOs
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
     */
    private void loadData() {
        try {
            // Use the controller to get data from database
            sessions = attendanceController.getAllClassSessions();

            // Get attendance data for all the sessions
            sessionAttendanceMap = new HashMap<>();
            for (ClassSession session : sessions) {
                List<Attendance> attendances = attendanceController.getAttendanceBySessionId(session.getId());
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
     */
    private VBox createClassCard(ClassSession session) {
        Long sessionId = session.getId();

        // Get attendance for session
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        // Calculate attendance statistics
        int[] attendanceStats = getAttendanceStats(sessionId);
        int presentCount = attendanceStats[0];
        int absentExcusedCount = attendanceStats[1];
        int absentUnexcusedCount = attendanceStats[2];
        int totalStudents = presentCount + absentExcusedCount + absentUnexcusedCount;

        // Check if attendance has been done for this session
        boolean isMarked = totalStudents > 0 && attendances.size() > 0;

        // Check if all absences have been notified
        boolean allAbsencesNotified = areAllAbsencesNotified(sessionId);

        // Create the card
        VBox card = new VBox(10);
        card.setPrefWidth(380);
        card.setPrefHeight(220);
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

        // Class name
        Label classNameLabel = new Label(session.getClassName());
        classNameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        classNameLabel.setTextFill(Color.web("#333333"));

        // Teacher name
        Label teacherLabel = new Label("Giáo viên: " + session.getTeacher());
        teacherLabel.setFont(Font.font("System", 14));
        teacherLabel.setTextFill(Color.web("#555555"));

        // Room
        Label roomLabel = new Label("Phòng: " + session.getRoom());
        roomLabel.setFont(Font.font("System", 14));
        roomLabel.setTextFill(Color.web("#555555"));

        // Schedule
        Label scheduleLabel = new Label("Lịch học: " + session.getSchedule());
        scheduleLabel.setFont(Font.font("System", 14));
        scheduleLabel.setTextFill(Color.web("#555555"));

        // Create HBox for attendance information
        HBox attendanceStatusBox = new HBox();
        attendanceStatusBox.setSpacing(10);
        attendanceStatusBox.setAlignment(Pos.CENTER_LEFT);

        if (isMarked) {
            // Has attendance data - show progress bar
            VBox progressBarContainer = new VBox(5);
            progressBarContainer.setPrefWidth(320);

            // Attendance status label
            HBox statusLabelBox = new HBox();
            statusLabelBox.setAlignment(Pos.CENTER_LEFT);

            Label attendanceStatusLabel = new Label("Điểm danh: " +
                    presentCount + " có mặt, " +
                    absentExcusedCount + " vắng có phép, " +
                    absentUnexcusedCount + " vắng không phép");
            attendanceStatusLabel.setFont(Font.font("System", 14));
            attendanceStatusLabel.setTextFill(Color.web("#555555"));

            // If unexcused absences and not all notified, show warning icon
            if (absentUnexcusedCount > 0 && !allAbsencesNotified) {
                Label warningLabel = new Label(" ⚠️ ");
                warningLabel.setFont(Font.font("System", 14));
                warningLabel.setTextFill(Color.web(RED_COLOR));
                warningLabel.setTooltip(new Tooltip("Chưa thông báo hết học sinh vắng không phép!"));

                statusLabelBox.getChildren().addAll(attendanceStatusLabel, warningLabel);
            } else {
                statusLabelBox.getChildren().add(attendanceStatusLabel);
            }

            // Create progress bar
            HBox progressBar = new HBox();
            progressBar.setPrefHeight(15);
            progressBar.setPrefWidth(320);

            // Create rectangles for each status
            double totalWidth = 320;
            double presentWidth = totalStudents > 0 ? (presentCount / (double) totalStudents) * totalWidth : 0;
            double absentExcusedWidth = totalStudents > 0 ? (absentExcusedCount / (double) totalStudents) * totalWidth : 0;
            double absentUnexcusedWidth = totalStudents > 0 ? (absentUnexcusedCount / (double) totalStudents) * totalWidth : 0;

            if (presentWidth > 0) {
                Rectangle presentRect = new Rectangle(presentWidth, 15);
                presentRect.setFill(Color.web(GREEN_COLOR));
                progressBar.getChildren().add(presentRect);
            }

            if (absentExcusedWidth > 0) {
                Rectangle absentExcusedRect = new Rectangle(absentExcusedWidth, 15);
                absentExcusedRect.setFill(Color.web(YELLOW_COLOR));
                progressBar.getChildren().add(absentExcusedRect);
            }

            if (absentUnexcusedWidth > 0) {
                Rectangle absentUnexcusedRect = new Rectangle(absentUnexcusedWidth, 15);
                absentUnexcusedRect.setFill(Color.web(RED_COLOR));
                progressBar.getChildren().add(absentUnexcusedRect);
            }

            progressBarContainer.getChildren().addAll(statusLabelBox, progressBar);
            attendanceStatusBox.getChildren().add(progressBarContainer);
        } else {
            // No attendance data - show "Not marked" text
            Label notMarkedLabel = new Label("Chưa điểm danh");
            notMarkedLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            notMarkedLabel.setTextFill(Color.web("#555555"));
            attendanceStatusBox.getChildren().add(notMarkedLabel);
        }

        // Tạo nút Điểm danh
        Button attendanceButton = new Button("Điểm danh");
        attendanceButton.setPrefHeight(35);
        attendanceButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 8 15;"
        );
        attendanceButton.setOnAction(e -> handleClassSelection(sessionId));

        // Add components to the card
        card.getChildren().addAll(
                classNameLabel,
                teacherLabel,
                roomLabel,
                scheduleLabel,
                attendanceStatusBox,
                attendanceButton
        );

        // Make the whole card clickable
        card.setOnMouseClicked(e -> handleClassSelection(sessionId));

        return card;
    }

    /**
     * Exports attendance data to Excel
     * Using controller to handle the export logic
     */
    private void exportToExcel() {
        try {
            boolean success = false;
            //attendanceController.exportToExcel(sessions, sessionAttendanceMap);
            if (success) {
                showInfo("Xuất dữ liệu Excel thành công!");
            } else {
                showError("Không thể xuất dữ liệu Excel.");
            }
        } catch (Exception e) {
            showError("Lỗi khi xuất dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Shows the attendance list screen
     * Using controller to handle navigation
     */
    private void viewAttendanceList() {
        try {
            navigationController.navigateTo("absence-call-table");

        } catch (Exception e) {
            showError("Lỗi khi mở danh sách điểm danh: " + e.getMessage());
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
     */
    private void handleClassSelection(Long sessionId) {
        try {
            navigationController.navigateTo("absence-call-view");
        } catch (Exception e) {
            showError("Lỗi khi chọn buổi học: " + e.getMessage());
        }
    }

    /**
     * Filters sessions by day
     * Uses controller for filtering logic
     */
    private void filterSessionsByDay(String day) {
        try {
            List<ClassSession> filteredSessions = attendanceController.filterSessionsByDay(
                    sessions, day);
            updateClassCards(filteredSessions);
            updateFilterButtonCounts(filteredSessions);
        } catch (Exception e) {
            showError("Lỗi khi lọc buổi học theo ngày: " + e.getMessage());
        }
    }

    /**
     * Filters sessions by attendance status
     * Uses controller for filtering logic
     */
    private void filterSessionsByStatus(String status) {
        try {
            List<ClassSession> filteredSessions;

            switch (status) {
                case "UNMARKED":
                    filteredSessions = sessions.stream()
                            .filter(session -> {
                                List<Attendance> attendances = sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>());
                                return attendances.isEmpty();
                            })
                            .collect(Collectors.toList());
                    break;
                case "MARKED":
                    filteredSessions = sessions.stream()
                            .filter(session -> {
                                List<Attendance> attendances = sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>());
                                return !attendances.isEmpty();
                            })
                            .collect(Collectors.toList());
                    break;
                case "ALL":
                default:
                    filteredSessions = sessions;
                    break;
            }

            updateClassCards(filteredSessions);
        } catch (Exception e) {
            showError("Lỗi khi lọc buổi học theo trạng thái: " + e.getMessage());
        }
    }

    /**
     * Search sessions by keyword
     * Uses controller for searching logic
     */
    private void searchSessions(String keyword) {
        try {
            if (keyword.isEmpty()) {
                updateClassCards(sessions);
                updateFilterButtonCounts(sessions);
                return;
            }

            List<ClassSession> filteredSessions = attendanceController.searchSessions(sessions, keyword);
            updateClassCards(filteredSessions);
            updateFilterButtonCounts(filteredSessions);
        } catch (Exception e) {
            showError("Lỗi khi tìm kiếm buổi học: " + e.getMessage());
        }
    }

    /**
     * Updates the filter button text with counts
     */
    private void updateFilterButtonCounts(List<ClassSession> filteredSessions) {
        if (filteredSessions == null) {
            return;
        }

        int total = filteredSessions.size();

        // Count unmarked and marked sessions
        int unmarked = 0;
        int marked = 0;

        for (ClassSession session : filteredSessions) {
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
            cardsPane.getChildren().add(noClassesLabel);
        } else {
            for (ClassSession session : filteredSessions) {
                VBox classCard = createClassCard(session);
                cardsPane.getChildren().add(classCard);
            }
        }
    }

    /**
     * Gets attendance statistics for a session
     * @param sessionId the session ID
     * @return array with [present count, absent excused count, absent unexcused count]
     */
    private int[] getAttendanceStats(Long sessionId) {
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        int presentCount = 0;
        int absentExcusedCount = 0;
        int absentUnexcusedCount = 0;

        for (Attendance attendance : attendances) {
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
     * Checks if all absences for a session have been notified
     * @param sessionId the session ID
     * @return true if all absences have been notified
     */
    private boolean areAllAbsencesNotified(Long sessionId) {
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());

        for (Attendance attendance : attendances) {
            // If not present, not excused, and not called, then not all absences are notified
            if (!attendance.isPresent() && !attendance.hasPermission() && !attendance.isCalled()) {
                return false;
            }
        }

        return true;
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
