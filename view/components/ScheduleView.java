
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
import src.controller.ScheduleController;
import view.BaseScreenView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduleView hiển thị lịch học theo tuần với các chức năng lọc và hiển thị thông tin lớp học.
 */
public class ScheduleView extends BaseScreenView {


    private ComboBox<String> teacherComboBox;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button searchButton;
    private Button prevWeekButton;
    private Button nextWeekButton;
    private GridPane scheduleGrid;
    private ScheduleController scheduleController;

    // List to store class sessions
    private List<ClassSession> classSessions;
    // Map to store color assignments for classes
    private Map<String, String> classColorMap;

    public ScheduleView() {
        super("Lịch học", "schedule");
        this.classColorMap = new HashMap<>();
        scheduleController = new ScheduleController();

        initializeView();
    }


    @Override
    public void initializeView() {
        // Đảm bảo root đã được xóa sạch trước khi bắt đầu
        root.getChildren().clear();

        root.setSpacing(10);
        root.setPadding(new Insets(20));

        // Initialize date pickers to the current week starting Monday
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        fromDatePicker = new DatePicker(startOfWeek);
        toDatePicker = new DatePicker(endOfWeek);
        fromDatePicker.setStyle("-fx-pref-width: 150px;");
        toDatePicker.setStyle("-fx-pref-width: 150px;");

        // Tạo control panel phía trên
        HBox controlPanel = createControlPanel();

        // Tạo lịch học
        scheduleGrid = createScheduleGrid();

        // Chỉ thêm các thành phần này vào root một lần
        root.getChildren().addAll(controlPanel, scheduleGrid);
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(15);
        controlPanel.setAlignment(Pos.CENTER_LEFT);

        // Teacher filter
        Label teacherLabel = new Label("Nhân sự:");
        teacherLabel.setTextFill(Color.BLACK);
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
        fromLabel.setTextFill(Color.BLACK);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);

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

        // Date row placeholder - populated later in populateSchedule
        Label dateLabel = new Label(""); // This label is just a placeholder
        dateLabel.setStyle("-fx-padding: 10;");
        grid.add(dateLabel, 0, 1);

        return grid;
    }

    /**
     * Phương thức để điều hướng đến tuần trước
     */
    private void navigateToPreviousWeek() {
        LocalDate fromDate = fromDatePicker.getValue();
        // Assuming fromDate is always the start of the week (Monday)
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
        // Assuming fromDate is always the start of the week (Monday)
        LocalDate newFromDate = fromDate.plusDays(7);
        LocalDate newToDate = newFromDate.plusDays(6);

        fromDatePicker.setValue(newFromDate);
        toDatePicker.setValue(newToDate);

        refreshView();
    }

    /**
     * Điền dữ liệu lịch học vào bảng đã được tạo sẵn
     */
    private void populateSchedule() {
        // Xóa tất cả các ô lịch trình (giữ lại tiêu đề)
        clearScheduleGrid();

        // Lấy ngày bắt đầu của tuần hiện tại (đã được thiết lập để luôn là Thứ 2)
        LocalDate startDate = fromDatePicker.getValue();
        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(startDate.plusDays(i));
        }

        // Thêm nhãn ngày vào hàng ngày
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 0; i < 7; i++) {
            Label dateLabel = new Label(weekDates.get(i).format(formatter));
            dateLabel.setStyle("-fx-padding: 10; -fx-text-fill: black;"); // Set text color to black
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setPrefWidth(150);
            // Add to grid cell for the correct day (i + 1 column)
            // Check if cell exists before adding or remove previous date label if needed
            // For simplicity, we clear the relevant row cells before adding new ones in clearScheduleGrid
            scheduleGrid.add(dateLabel, i + 1, 1);
        }

        // Lấy các khoảng thời gian duy nhất và sắp xếp chúng
        Set<String> timeSlots = new HashSet<>();
        for (ClassSession session : classSessions) {
            timeSlots.add(session.getTimeSlot());
        }

        List<String> sortedTimeSlots = new ArrayList<>(timeSlots);
        // Sort time slots - assumes timeSlot is in a sortable format like "HH:mm - HH:mm"
        Collections.sort(sortedTimeSlots);

        // Tạo các hàng khoảng thời gian
        int rowIndex = 2;
        for (String timeSlot : sortedTimeSlots) {
            Label timeLabel = new Label(timeSlot);
            timeLabel.setStyle("-fx-padding: 10; -fx-text-fill: black;"); // Set text color to black
            timeLabel.setAlignment(Pos.CENTER);
            scheduleGrid.add(timeLabel, 0, rowIndex);

            // Thêm ô phiên cho mỗi ngày trong khoảng thời gian này
            for (int day = 0; day < 7; day++) {
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setFitToWidth(true);
                // Preserve the background color setting
                scrollPane.setStyle("-fx-background-color: transparent;");

                VBox dayContainer = new VBox(5);
                dayContainer.setPadding(new Insets(5));
                scrollPane.setContent(dayContainer);

                // Add to the correct cell based on day index and row index
                scheduleGrid.add(scrollPane, day + 1, rowIndex);

                LocalDate currentDate = weekDates.get(day);

                // Tìm các phiên cho ngày và khoảng thời gian này
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

        // Nếu không tìm thấy khoảng thời gian nào, thêm một hàng trống
        if (timeSlots.isEmpty()) {
            Label emptyLabel = new Label("Không có lịch học");
            emptyLabel.setStyle("-fx-padding: 10; -fx-alignment: center; -fx-text-fill: black;"); // Also set text color for empty message
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            // Add empty label spanning across columns 0 to 7 (8 columns total) in row 2
            scheduleGrid.add(emptyLabel, 0, 2, 8, 1);
        }
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

        // Cập nhật dữ liệu trong grid - KHÔNG tạo grid mới
        populateSchedule();
    }

    /**
     * Xóa dữ liệu trong bảng lịch học hiện tại để chuẩn bị điền dữ liệu mới
     * Chỉ giữ lại 2 hàng đầu (hàng tiêu đề và hàng ngày)
     */
    private void clearScheduleGrid() {
        // Lấy tất cả các node hiện tại trong grid
        List<Node> toRemove = new ArrayList<>();

        // Xác định các node cần xóa - tất cả các node ở hàng > 1
        for (Node node : scheduleGrid.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            if (rowIndex != null && rowIndex > 1) {
                toRemove.add(node);
            }
        }

        // Also remove the previous date labels in row 1 (columns 1 to 7)
        for (Node node : scheduleGrid.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);
            if (rowIndex != null && rowIndex == 1 && colIndex != null && colIndex > 0) {
                toRemove.add(node);
            }
        }


        // Xóa các node đã xác định
        scheduleGrid.getChildren().removeAll(toRemove);
    }

    // Sửa phương thức createSessionBox để có kiểm tra
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

        // Thêm hiệu ứng khi hover
        box.setOnMouseEntered(e ->
                box.setStyle("-fx-background-color: " + getColorForClass(session.getClassName()) +
                        "; -fx-background-radius: 5; -fx-border-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 0);")
        );

        box.setOnMouseExited(e ->
                box.setStyle("-fx-background-color: " + getColorForClass(session.getClassName()) +
                        "; -fx-background-radius: 5; -fx-border-radius: 5;")
        );

        // Add click handler to show more details with proper checking using routeExists
        box.setOnMouseClicked(e -> {
            System.out.println("========== CLICK VÀO LỊCH ==========");
            System.out.println("Session được chọn: " + (session != null ? session.getCourseName() : "NULL"));
            System.out.println("mainController trong ScheduleView: " + (mainController != null ? "CÓ GIÁ TRỊ" : "NULL"));

            if (mainController != null) {
                mainController.setSessionDetail(session);
                System.out.println("Đã lưu session vào mainController");
            } else {
                System.err.println("LỖI: mainController là null trong ScheduleView");
            }

            if (navigationController != null) {
                System.out.println("Chuyển đến màn hình classDetails");
                navigationController.navigateTo("classDetails");
            } else {
                System.err.println("LỖI: navigationController là null trong ScheduleView");
            }
        });

        // Thêm cursor pointer để chỉ ra rằng phần tử này có thể click
        box.setCursor(javafx.scene.Cursor.HAND);

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

    // BaseScreenView overrides

    // Sửa phần onActivate để đảm bảo controllers được khởi tạo
    @Override
    public void onActivate() {
        refreshView();
    }

    @Override
    public boolean onDeactivate() {
        // Return true to allow deactivation
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
                LocalDate from = dateRange.get("fromDate");
                // Ensure the range starts on a Monday when navigating
                LocalDate startOfWeek = from.minusDays(from.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
                LocalDate endOfWeek = startOfWeek.plusDays(6);

                fromDatePicker.setValue(startOfWeek);
                toDatePicker.setValue(endOfWeek);
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
                    // Set the date range to include the target date (start of week - Monday)
                    LocalDate startOfWeek = targetDate.minusDays(targetDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
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

