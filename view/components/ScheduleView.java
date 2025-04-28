package view.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import src.model.ClassSession;
import src.controller.NavigationController;
import src.controller.MainController;
import src.controller.ScheduleController;
import view.ScreenView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduleView hiển thị lịch học theo tuần với các chức năng lọc và hiển thị thông tin lớp học.
 */
public class ScheduleView implements ScreenView {

    private VBox root;
    private String title;
    private String viewId;
    private NavigationController navigationController;
    private MainController mainController;
    private ScheduleController scheduleController;

    private ComboBox<String> teacherComboBox;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button searchButton;
    private Button prevWeekButton;
    private Button nextWeekButton;
    private GridPane scheduleGrid;

    // List to store class sessions
    private List<ClassSession> classSessions;
    // Map to store color assignments for classes
    private Map<String, String> classColorMap;

    public ScheduleView() {
        this.title = "Lịch học";
        this.viewId = "schedule";
        this.root = new VBox();
        this.classColorMap = new HashMap<>();
        scheduleController = new ScheduleController();
        initializeView();
    }

    @Override
    public void initializeView() {
        root.setSpacing(10);
        root.setPadding(new Insets(20));

        // Tạo control panel phía trên
        HBox controlPanel = createControlPanel();

        // Tạo lịch học
        scheduleGrid = createScheduleGrid();

        root.getChildren().addAll(controlPanel, scheduleGrid);
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(15);
        controlPanel.setAlignment(Pos.CENTER_LEFT);

        // Teacher filter
        Label teacherLabel = new Label("Nhân sự:");
        teacherComboBox = new ComboBox<>();
        teacherComboBox.getItems().add("Chọn");
        teacherComboBox.setValue("Chọn");
        teacherComboBox.setPrefWidth(150);

        // Search button
        searchButton = new Button();
        searchButton.setGraphic(new Label("🔍"));
        searchButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");

        // Date range
        prevWeekButton = new Button();
        prevWeekButton.setGraphic(new Label("◀"));
        prevWeekButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");

        Label fromLabel = new Label("Từ:");
        fromDatePicker = new DatePicker(LocalDate.now());
        fromDatePicker.setStyle("-fx-pref-width: 150px;");

        Label toLabel = new Label("Đến:");
        toDatePicker = new DatePicker(LocalDate.now().plusDays(6));
        toDatePicker.setStyle("-fx-pref-width: 150px;");

        nextWeekButton = new Button();
        nextWeekButton.setGraphic(new Label("▶"));
        nextWeekButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");

        // Add components to panel
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controlPanel.getChildren().addAll(
                teacherLabel, teacherComboBox, searchButton,
                spacer,
                prevWeekButton, fromLabel, fromDatePicker,
                toLabel, toDatePicker, nextWeekButton
        );

        // Set up event handlers
        prevWeekButton.setOnAction(e -> navigateToPreviousWeek());
        nextWeekButton.setOnAction(e -> navigateToNextWeek());
        searchButton.setOnAction(e -> refreshView());

        return controlPanel;
    }

    private GridPane createScheduleGrid() {
        GridPane grid = new GridPane();
        grid.setGridLinesVisible(true);
        grid.setStyle("-fx-border-color: #ddd;");

        // Column constraints
        ColumnConstraints timeCol = new ColumnConstraints(100);
        ColumnConstraints dayCol = new ColumnConstraints(150);

        grid.getColumnConstraints().add(timeCol);
        for (int i = 0; i < 7; i++) {
            grid.getColumnConstraints().add(dayCol);
        }

        // Create header row
        Label headerLabel = new Label("Thời gian");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-text-fill: black;");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPrefWidth(100);
        headerLabel.setPrefHeight(40);
        grid.add(headerLabel, 0, 0);

        String[] days = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-text-fill: black;");
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPrefWidth(150);
            dayLabel.setPrefHeight(40);
            grid.add(dayLabel, i + 1, 0);
        }

        // Date row
        Label dateLabel = new Label("");
        dateLabel.setStyle("-fx-padding: 10;");
        grid.add(dateLabel, 0, 1);

        return grid;
    }

    /**
     * Phương thức để điều hướng đến tuần trước
     */
    private void navigateToPreviousWeek() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate newFromDate = fromDate.minusDays(7);
        LocalDate newToDate = newFromDate.plusDays(6);

        fromDatePicker.setValue(newFromDate);
        toDatePicker.setValue(newToDate);

        refreshView();
    }

    /**
     * Phương thức để điều hướng đến tuần sau
     */
    private void navigateToNextWeek() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate newFromDate = fromDate.plusDays(7);
        LocalDate newToDate = newFromDate.plusDays(6);

        fromDatePicker.setValue(newFromDate);
        toDatePicker.setValue(newToDate);

        refreshView();
    }

    /**
     * Phương thức cập nhật giao diện lịch học
     */
    public void refreshView() {
        if (scheduleController == null) {
            return;
        }

        // Lấy danh sách lịch học từ controller
        String selectedTeacher = getSelectedTeacher();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        classSessions = scheduleController.getSchedule(fromDate, toDate, selectedTeacher);

        // Cập nhật danh sách giáo viên nếu chưa được khởi tạo
        if (teacherComboBox.getItems().size() <= 1) {
            List<String> teachers = scheduleController.getPersonnel();
            if (teachers != null && !teachers.isEmpty()) {
                teacherComboBox.getItems().clear();
                teacherComboBox.getItems().add("Chọn");
                teacherComboBox.getItems().addAll(teachers);
                teacherComboBox.setValue("Chọn");
            }
        }

        // Tạo lại bảng lịch học
        scheduleGrid = createScheduleGrid();
        populateSchedule();

        // Cập nhật giao diện
        if (root.getChildren().size() > 1) {
            root.getChildren().set(1, scheduleGrid);
        } else {
            root.getChildren().add(scheduleGrid);
        }
    }

    private void populateSchedule() {
        // Get the current week's dates
        LocalDate startDate = fromDatePicker.getValue();
        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(startDate.plusDays(i));
        }

        // Add date labels to date row
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 0; i < 7; i++) {
            Label dateLabel = new Label(weekDates.get(i).format(formatter));
            dateLabel.setStyle("-fx-padding: 10;");
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setPrefWidth(150);
            scheduleGrid.add(dateLabel, i + 1, 1);
        }

        // Get unique time slots and sort them
        Set<String> timeSlots = new HashSet<>();
        for (ClassSession session : classSessions) {
            timeSlots.add(session.getTimeSlot());
        }

        List<String> sortedTimeSlots = new ArrayList<>(timeSlots);
        Collections.sort(sortedTimeSlots);

        // Create time slot rows
        int rowIndex = 2;
        for (String timeSlot : sortedTimeSlots) {
            Label timeLabel = new Label(timeSlot);
            timeLabel.setStyle("-fx-padding: 10;");
            timeLabel.setAlignment(Pos.CENTER);
            scheduleGrid.add(timeLabel, 0, rowIndex);

            // Add session boxes for each day in this time slot
            for (int day = 0; day < 7; day++) {
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setFitToWidth(true);
                scrollPane.setStyle("-fx-background-color: transparent;");

                VBox dayContainer = new VBox(5);
                dayContainer.setPadding(new Insets(5));
                scrollPane.setContent(dayContainer);

                scheduleGrid.add(scrollPane, day + 1, rowIndex);

                LocalDate currentDate = weekDates.get(day);

                // Find sessions for this day and time slot
                List<ClassSession> daySessions = classSessions.stream()
                        .filter(session -> session.getDate().equals(currentDate) &&
                                session.getTimeSlot().equals(timeSlot))
                        .collect(Collectors.toList());

                for (ClassSession session : daySessions) {
                    VBox box = createSessionBox(session);
                    dayContainer.getChildren().add(box);
                }
            }

            rowIndex++;
        }

        // If no time slots were found, add an empty row
        if (timeSlots.isEmpty()) {
            Label emptyLabel = new Label("Không có lịch học");
            emptyLabel.setStyle("-fx-padding: 10;");
            scheduleGrid.add(emptyLabel, 0, 2, 8, 1);
        }
    }

    private VBox createSessionBox(ClassSession session) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: " + getColorForClass(session.getClassName()) +
                "; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label titleLabel = new Label(session.getClassName());
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        Label teacherLabel = new Label(session.getTeacher());
        teacherLabel.setStyle("-fx-font-size: 11px;");

        Label roomLabel = new Label(session.getRoom());
        roomLabel.setStyle("-fx-font-size: 11px;");

        box.getChildren().addAll(titleLabel, teacherLabel, roomLabel);

        // Add click handler to show more details
        box.setOnMouseClicked(e -> {
            if (navigationController != null) {
                // Store session details in main controller for access by details view
                mainController.setSessionDetail(session);
                // Navigate to session details view
                navigationController.navigateTo("classDetails");
            }
        });

        return box;
    }

    private String getColorForClass(String className) {
        if (!classColorMap.containsKey(className)) {
            // Generate a pastel color
            String[] colors = {
                    "#FF9999", "#99CC99", "#9999FF", "#FFFF99", "#FF99FF", "#99FFFF",
                    "#FF8844", "#88FF44", "#8844FF", "#FFFF44", "#FF44FF", "#44FFFF"
            };

            int index = classColorMap.size() % colors.length;
            classColorMap.put(className, colors[index]);
        }
        return classColorMap.get(className);
    }

    /**
     * Trả về giá trị giáo viên đang được chọn
     * @return Tên giáo viên đang được chọn, null nếu chọn tất cả
     */
    public String getSelectedTeacher() {
        String teacher = teacherComboBox.getValue();
        return "Chọn".equals(teacher) ? null : teacher;
    }

    /**
     * Thiết lập controller cho view
     * @param scheduleController Controller quản lý lịch học
     */
    public void setScheduleController(ScheduleController scheduleController) {
        this.scheduleController = scheduleController;
    }

    // ScreenView interface implementation

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public void onActivate() {
        // Tải dữ liệu khi view được kích hoạt
        refreshView();
    }

    @Override
    public boolean onDeactivate() {
        // Return true to allow deactivation
        return true;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setNavigationController(NavigationController navigationController) {
        this.navigationController = navigationController;
    }

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public boolean requiresAuthentication() {
        // This view requires authentication
        return true;
    }

    @Override
    public void handleSystemMessage(String message, Object data) {
        if ("REFRESH_SCHEDULE".equals(message)) {
            refreshView();
        } else if ("FILTER_BY_TEACHER".equals(message) && data instanceof String) {
            String teacherName = (String) data;
            teacherComboBox.setValue(teacherName);
            refreshView();
        } else if ("VIEW_DATE_RANGE".equals(message) && data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, LocalDate> dateRange = (Map<String, LocalDate>) data;
            if (dateRange.containsKey("fromDate") && dateRange.containsKey("toDate")) {
                fromDatePicker.setValue(dateRange.get("fromDate"));
                toDatePicker.setValue(dateRange.get("toDate"));
                refreshView();
            }
        }
    }

    @Override
    public Object handleAction(String actionId, Object params) {
        switch (actionId) {
            case "FILTER_BY_TEACHER":
                if (params instanceof String) {
                    teacherComboBox.setValue((String) params);
                    refreshView();
                }
                return true;

            case "NAVIGATE_TO_DATE":
                if (params instanceof LocalDate) {
                    LocalDate targetDate = (LocalDate) params;
                    // Set the date range to include the target date (start of week)
                    LocalDate startOfWeek = targetDate.minusDays(targetDate.getDayOfWeek().getValue() - 1);
                    LocalDate endOfWeek = startOfWeek.plusDays(6);

                    fromDatePicker.setValue(startOfWeek);
                    toDatePicker.setValue(endOfWeek);
                    refreshView();
                }
                return true;

            case "GET_SELECTED_DATE_RANGE":
                Map<String, LocalDate> dateRange = new HashMap<>();
                dateRange.put("fromDate", fromDatePicker.getValue());
                dateRange.put("toDate", toDatePicker.getValue());
                return dateRange;

            case "GET_SELECTED_TEACHER":
                return getSelectedTeacher();

            default:
                return null;
        }
    }
}
