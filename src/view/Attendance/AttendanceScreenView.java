package src.view.Attendance;

import javafx.application.Platform; // Thêm nếu thiếu
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import src.controller.Attendance.AttendanceController;

import src.view.components.Screen.BaseScreenView;
import src.model.ClassSession;
import src.model.attendance.Attendance;

import java.time.DayOfWeek; // THÊM IMPORT NÀY
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.sql.SQLException;
// import java.util.logging.Logger; // Có thể bỏ nếu không dùng LOGGER trực tiếp ở đây nhiều

public class AttendanceScreenView extends BaseScreenView {

    // Constants (giữ nguyên)
    private static final String PRIMARY_COLOR = "#1976D2";
    private static final String SELECTED_DAY_COLOR = "#6667AB";
    private static final String GREEN_COLOR = "#4CAF50";
    private static final String LIGHT_GRAY = "#F5F5F5";
    private static final String WHITE_COLOR = "#FFFFFF";
    private static final String BORDER_COLOR = "#E0E0E0";
    private static final String YELLOW_COLOR = "#FFC107";
    private static final String RED_COLOR = "#F44336";

    // UI Components
    private Label titleLabel;
    private HBox dayFilterBox;
    private HBox statusFilterBox;
    private TextField searchField;
    private Button searchButton;
    private Button exportExcelButton;
    private Button attendanceListButton;
    private FlowPane cardsPane;

    private ToggleGroup dayToggleGroup;
    private List<ToggleButton> dayButtons;

    private Button allButton;
    private Button unmarkedButton;
    private Button markedButton;

    // Data
    private List<ClassSession> allLoadedSessions; // Đổi tên từ 'sessions' để rõ hơn đây là danh sách gốc
    private Map<String, List<Attendance>> sessionAttendanceMap;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Track current filter state
    private String currentStatusFilter = "ALL"; // Mặc định là "ALL"
    private String currentDayFilter = null; // Sẽ được set khi chọn nút ngày
    private String currentSearchKeyword = "";

    // private DatePicker datePicker; // BỎ VÌ KHÔNG DÙNG
    // private LocalDate currentDateFilter = LocalDate.now(); // BỎ VÌ KHÔNG DÙNG
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private AttendanceController attendanceController;

    public AttendanceScreenView() {
        super("Điểm danh", "attendance");
        allLoadedSessions = new ArrayList<>();
        sessionAttendanceMap = new HashMap<>();
        attendanceController = new AttendanceController();
        initializeView();
        // Không load data ở đây, sẽ load trong onActivate hoặc refreshView
    }

    @Override
    public void initializeView() {
        root.getChildren().clear();
        root.setSpacing(0);
        root.setPadding(Insets.EMPTY);
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");

        BorderPane mainLayout = new BorderPane();
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));

        HBox titleBar = createTitleBar();
        createDayFilterSection(); // Gán giá trị cho dayFilterBox và dayButtons
        createStatusFilterSection(); // Gán giá trị cho statusFilterBox và các nút status

        contentBox.getChildren().addAll(titleBar, dayFilterBox, statusFilterBox);

        HBox searchBox = createSearchSection();
        contentBox.getChildren().add(searchBox);

        cardsPane = new FlowPane();
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        // cardsPane.setPrefWrapLength(1200); // Có thể không cần nếu scrollPane fitToWidth

        ScrollPane scrollPane = new ScrollPane(cardsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true); // Cho phép scroll theo chiều dọc nếu cần
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS); // Cho scrollPane chiếm không gian còn lại

        contentBox.getChildren().add(scrollPane);
        mainLayout.setCenter(contentBox);
        root.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        setupEventHandlers();
        // Chọn ngày hiện tại trong tuần làm mặc định và áp dụng filter ban đầu
        setDefaultDayFilterAndApply();
    }

    private void setDefaultDayFilterAndApply() {
        LocalDate today = LocalDate.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek(); // Ví dụ MONDAY, TUESDAY
        String todayDayString = todayDayOfWeek.name(); // "MONDAY", "TUESDAY"

        // Tìm ToggleButton tương ứng với ngày hôm nay và chọn nó
        boolean daySet = false;
        if (dayButtons != null) {
            for (ToggleButton dayButton : dayButtons) {
                if (dayButton.getUserData().equals(todayDayString)) {
                    dayButton.setSelected(true); // Điều này sẽ trigger setOnAction
                    // currentDayFilter sẽ được set trong setOnAction của dayButton
                    // applyFilters() cũng sẽ được gọi từ đó.
                    daySet = true;
                    break;
                }
            }
        }
        // Nếu không tìm thấy (trường hợp hiếm), hoặc muốn đảm bảo filter được gọi
        if (!daySet) {
            // Nếu không có nút nào được chọn, bạn có thể không filter theo ngày
            // hoặc chọn một ngày mặc định khác.
            // Hiện tại, nếu không có nút ngày nào được chọn, applyFilters() vẫn chạy
            // và currentDayFilter có thể là null (sẽ hiển thị tất cả các ngày trong tuần hiện tại)
            // Để đảm bảo logic, nếu currentDayFilter vẫn null, ta có thể set nó là ngày hôm nay
            if (currentDayFilter == null && !dayButtons.isEmpty()) {
                dayButtons.get(today.getDayOfWeek().getValue() -1).setSelected(true);
                // setOnAction sẽ tự gọi applyFilters
            } else if (currentDayFilter != null) {
                applyFilters(); // Gọi nếu currentDayFilter đã được set bởi setSelected(true) ở trên
            } else {
                // Xử lý trường hợp không có nút ngày nào hoặc dayButtons rỗng
                // Có thể không cần làm gì nếu muốn hiển thị tất cả
            }
        }
    }


    private void setupEventHandlers() {
        if (dayButtons != null) {
            for (ToggleButton dayButton : dayButtons) {
                dayButton.setOnAction(e -> {
                    if (dayButton.isSelected()) {
                        currentDayFilter = (String) dayButton.getUserData();
                    } else {
                        // Nếu cho phép bỏ chọn tất cả, thì currentDayFilter có thể là null
                        // Hoặc nếu luôn phải có 1 ngày được chọn, logic của ToggleGroup sẽ xử lý
                        if (dayToggleGroup.getSelectedToggle() == null) {
                            currentDayFilter = null; // Hoặc giữ lại filter cũ nếu không muốn bỏ chọn
                        }
                    }
                    applyFilters();
                });
            }
        }

        if (allButton != null) {
            allButton.setOnAction(e -> {
                setActiveStatusButton(allButton);
                currentStatusFilter = "ALL";
                applyFilters();
            });
        }
        if (unmarkedButton != null) {
            unmarkedButton.setOnAction(e -> {
                setActiveStatusButton(unmarkedButton);
                currentStatusFilter = "UNMARKED";
                applyFilters();
            });
        }
        if (markedButton != null) {
            markedButton.setOnAction(e -> {
                setActiveStatusButton(markedButton);
                currentStatusFilter = "MARKED";
                applyFilters();
            });
        }

        if (searchButton != null) {
            searchButton.setOnAction(e -> {
                currentSearchKeyword = searchField.getText().trim();
                applyFilters();
            });
        }
        if (searchField != null) {
            searchField.setOnAction(e -> { // Tìm kiếm khi nhấn Enter
                currentSearchKeyword = searchField.getText().trim();
                applyFilters();
            });
        }

        if (exportExcelButton != null) exportExcelButton.setOnAction(e -> exportToExcel());
        if (attendanceListButton != null) attendanceListButton.setOnAction(e -> viewAttendanceList());
    }

    private void applyFilters() {
        List<ClassSession> currentlyProcessedSessions = new ArrayList<>(allLoadedSessions);

        // 1. Lọc theo từ khóa tìm kiếm (nếu có)
        if (currentSearchKeyword != null && !currentSearchKeyword.isEmpty()) {
            currentlyProcessedSessions = searchSessionsInternal(currentlyProcessedSessions, currentSearchKeyword);
        }

        // 2. SỬA ĐỔI: Lọc theo NGÀY TRONG TUẦN và TRONG TUẦN HIỆN TẠI
        if (currentDayFilter != null && !currentDayFilter.isEmpty()) {
            // Lấy tất cả các buổi học cho ngày trong tuần đã chọn (ví dụ: tất cả các Thứ Hai)
            List<ClassSession> sessionsForSelectedDayName = filterSessionsByDayInternal(currentlyProcessedSessions, currentDayFilter);

            // Xác định ngày bắt đầu và kết thúc của tuần hiện tại
            LocalDate today = LocalDate.now();
            DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY; // Tuần bắt đầu từ Thứ Hai
            LocalDate startOfWeek = today.with(firstDayOfWeek);
            LocalDate endOfWeek = startOfWeek.plusDays(6); // Thứ Hai + 6 ngày = Chủ Nhật

            // Lọc lại danh sách sessionsForSelectedDayName để chỉ giữ những buổi trong tuần hiện tại
            final LocalDate finalStartOfWeek = startOfWeek;
            final LocalDate finalEndOfWeek = endOfWeek;
            currentlyProcessedSessions = sessionsForSelectedDayName.stream()
                    .filter(session -> {
                        LocalDate sessionDate = session.getDate(); // Giả sử ClassSession có getDate() trả về LocalDate
                        return sessionDate != null &&
                                !sessionDate.isBefore(finalStartOfWeek) &&
                                !sessionDate.isAfter(finalEndOfWeek);
                    })
                    .collect(Collectors.toList());
        } else {
            // Nếu không có ngày nào trong tuần được chọn (currentDayFilter là null hoặc rỗng),
            // có thể bạn muốn hiển thị tất cả các buổi trong tuần hiện tại (sau khi đã search)
            // hoặc không hiển thị gì cả. Hiện tại, nó sẽ lấy currentlyProcessedSessions (đã qua search).
            // Để nhất quán, nếu không có currentDayFilter, có thể không nên lọc theo tuần.
            // Tuy nhiên, logic setDefaultDayFilterAndApply() thường đảm bảo currentDayFilter được chọn.
        }

        // 3. Lọc theo trạng thái điểm danh (áp dụng trên danh sách đã được lọc bởi tìm kiếm và ngày/tuần)
        List<ClassSession> finalFilteredSessions = filterSessionsByStatusInternal(currentlyProcessedSessions, currentStatusFilter);

        updateClassCards(finalFilteredSessions);
        // Cập nhật số lượng trên nút lọc trạng thái dựa trên danh sách đã lọc theo search và ngày/tuần
        updateFilterButtonCounts(currentlyProcessedSessions);
    }

    private void loadData() {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            allLoadedSessions = new ArrayList<>(); // Khởi tạo rỗng để tránh NullPointerException
            sessionAttendanceMap.clear();
            applyFilters(); // Cập nhật UI với danh sách rỗng
            return;
        }
        try {
            allLoadedSessions = attendanceController.getAllClassSessions();
            if (allLoadedSessions == null) allLoadedSessions = new ArrayList<>(); // Đảm bảo không null

            sessionAttendanceMap.clear();
            for (ClassSession session : allLoadedSessions) {
                if (session != null && session.getId() != null) {
                    List<Attendance> attendances = attendanceController.getAttendanceBySessionId(session.getId());
                    sessionAttendanceMap.put(session.getId(), attendances != null ? attendances : new ArrayList<>());
                }
            }
            // applyFilters sẽ được gọi bởi setDefaultDayFilterAndApply hoặc khi người dùng tương tác
            // không cần gọi ở đây nữa để tránh gọi nhiều lần khi khởi tạo.
            // Nếu setDefaultDayFilterAndApply không được gọi đúng, bạn có thể cần gọi applyFilters() ở đây.
            // Nhưng tốt nhất là để event handlers hoặc logic khởi tạo UI gọi nó.
        } catch (SQLException e) {
            showError("Lỗi khi kết nối với cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
            allLoadedSessions = new ArrayList<>();
            sessionAttendanceMap.clear();
            applyFilters(); // Cập nhật UI với danh sách rỗng nếu có lỗi
        } catch (Exception e) {
            showError("Lỗi khi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
            allLoadedSessions = new ArrayList<>();
            sessionAttendanceMap.clear();
            applyFilters();
        }
    }

    // ... (createTitleBar, createStatusFilterSection, createSearchSection giữ nguyên) ...
    // ... (createClassCard, exportToExcel, viewAttendanceList, setActiveStatusButton, handleClassSelection giữ nguyên) ...
    // ... (filterSessionsByDayInternal, filterSessionsByStatusInternal, searchSessionsInternal giữ nguyên) ...
    // ... (updateFilterButtonCounts, updateClassCards, getAttendanceStats, areAllAbsencesNotified giữ nguyên) ...

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setSpacing(15);
        // Loại bỏ padding ở đây nếu contentBox đã có padding tổng thể
        // titleBar.setPadding(new Insets(0, 0, 15, 0));
        // titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;");


        titleLabel = new Label("Điểm danh Buổi học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22)); // Kích thước font nhỏ hơn một chút
        titleLabel.setTextFill(Color.web("#2c3e50")); // Màu chữ hiện đại

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        exportExcelButton = new Button("Xuất Excel");
        styleStandardButton(exportExcelButton, PRIMARY_COLOR);


        attendanceListButton = new Button("DS Vắng Mặt"); // Đổi tên nút
        styleStandardButton(attendanceListButton, GREEN_COLOR); // Màu khác cho nút này

        titleBar.getChildren().addAll(titleLabel, spacer, exportExcelButton, attendanceListButton);
        return titleBar;
    }
    private void styleStandardButton(Button button, String backgroundColor) {
        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" + // Bo góc ít hơn
                        "-fx-padding: 8px 18px;" + // Padding chuẩn
                        "-fx-font-size: 12px;" +    // Font nhỏ hơn
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 1, 1);" // Bóng mờ nhẹ
        );
        button.setOnMouseEntered(e -> button.setOpacity(0.9));
        button.setOnMouseExited(e -> button.setOpacity(1.0));
    }


    private void createDayFilterSection() {
        dayFilterBox = new HBox(8); // Giảm khoảng cách giữa các nút
        dayFilterBox.setAlignment(Pos.CENTER_LEFT);
        // dayFilterBox.setPadding(new Insets(0, 0, 10, 0)); // Padding đã có ở contentBox

        Label dayFilterLabel = new Label("Ngày trong tuần:"); // Label rõ ràng hơn
        dayFilterLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        dayFilterLabel.setTextFill(Color.web("#333333"));
        // dayFilterLabel.setMinWidth(Region.USE_PREF_SIZE); // Để label tự co giãn

        dayToggleGroup = new ToggleGroup();
        dayButtons = new ArrayList<>();

        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String[] dayNames = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        for (int i = 0; i < days.length; i++) {
            ToggleButton dayButton = new ToggleButton(dayNames[i]);
            dayButton.setUserData(days[i]); // "MONDAY", "TUESDAY", ...
            dayButton.setToggleGroup(dayToggleGroup);
            dayButton.setPrefHeight(32); // Chiều cao nút
            dayButton.setMinWidth(45);  // Chiều rộng tối thiểu
            dayButton.setFont(Font.font("System", FontWeight.BOLD, 12));
            applyDayButtonStyle(dayButton, false); // Áp dụng style mặc định

            dayButton.selectedProperty().addListener((obs, oldSelected, newSelected) -> {
                applyDayButtonStyle(dayButton, newSelected);
                // Logic cập nhật currentDayFilter và gọi applyFilters đã có trong setupEventHandlers
            });
            dayButtons.add(dayButton);
        }
        dayFilterBox.getChildren().add(dayFilterLabel);
        dayFilterBox.getChildren().addAll(dayButtons);
    }

    private void applyDayButtonStyle(ToggleButton button, boolean isSelected) {
        if (isSelected) {
            button.setStyle(
                    "-fx-background-color: " + SELECTED_DAY_COLOR + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 5px;" + // Bo góc
                            "-fx-border-color: " + SELECTED_DAY_COLOR + "; -fx-border-width: 1px;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 3, 0, 1, 1);"
            );
        } else {
            button.setStyle(
                    "-fx-background-color: " + WHITE_COLOR + ";" + // Nền trắng
                            "-fx-text-fill: #444444;" +              // Chữ xám đậm
                            "-fx-background-radius: 5px;" +
                            "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 1px;" // Border xám nhạt
            );
        }
    }


    private void createStatusFilterSection() {
        statusFilterBox = new HBox(10);
        statusFilterBox.setAlignment(Pos.CENTER_LEFT);
        // statusFilterBox.setPadding(new Insets(0, 0, 10, 0));

        Label statusFilterLabel = new Label("Trạng thái ĐD:"); // Label ngắn gọn
        statusFilterLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        statusFilterLabel.setTextFill(Color.web("#333333"));
        // statusFilterLabel.setMinWidth(Region.USE_PREF_SIZE);

        allButton = new Button("Tất cả (0)");
        unmarkedButton = new Button("Chưa ĐD (0)");
        markedButton = new Button("Đã ĐD (0)");

        // Áp dụng style chung cho các nút status filter, style active sẽ được set riêng
        styleStatusButton(allButton, true); // Nút "Tất cả" active mặc định
        styleStatusButton(unmarkedButton, false);
        styleStatusButton(markedButton, false);

        statusFilterBox.getChildren().addAll(statusFilterLabel, allButton, unmarkedButton, markedButton);
    }

    private void styleStatusButton(Button button, boolean isActive) {
        String baseStyle = "-fx-background-radius: 20px; -fx-padding: 6px 15px; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
        if (isActive) {
            button.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white;" + baseStyle);
        } else {
            button.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-text-fill: #555555;" + baseStyle + "-fx-border-color:"+BORDER_COLOR+"; -fx-border-width:1px;");
        }
    }


    private HBox createSearchSection() {
        HBox searchBox = new HBox(0); // Không có khoảng cách giữa textfield và button
        searchBox.setAlignment(Pos.CENTER_LEFT);
        // searchBox.setPadding(new Insets(0, 0, 10, 0));

        searchField = new TextField();
        searchField.setPromptText("Tìm lớp, giáo viên...");
        searchField.setPrefHeight(36); // Đồng bộ chiều cao
        // searchField.setPrefWidth(300); // Bỏ nếu muốn Hgrow
        HBox.setHgrow(searchField, Priority.ALWAYS); // Cho phép searchField co giãn
        searchField.setStyle(
                "-fx-background-color: " + WHITE_COLOR + ";" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 1px 0 1px 1px;" + // Border ở 3 cạnh
                        "-fx-background-radius: 5px 0 0 5px;" + // Bo góc trái
                        "-fx-padding: 5px 15px;" +
                        "-fx-font-size: 13px;"
        );

        searchButton = new Button("Tìm");
        searchButton.setPrefHeight(36); // Đồng bộ chiều cao
        searchButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 0 5px 5px 0;" + // Bo góc phải
                        "-fx-border-color: " + PRIMARY_COLOR + ";" + // Border cùng màu nền
                        "-fx-border-width: 1px;" +
                        "-fx-padding: 5px 15px;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );
        Label searchIcon = new Label("🔍"); // Optional: Thêm icon nếu muốn
        searchIcon.setTextFill(Color.WHITE);
        // searchButton.setGraphic(searchIcon);

        searchBox.getChildren().addAll(searchField, searchButton);
        return searchBox;
    }

    private VBox createClassCard(ClassSession session) {
        String sessionId = session.getId(); // Đã là String
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());
        int[] attendanceStats = getAttendanceStats(sessionId); // Sử dụng sessionId (String)
        int presentCount = attendanceStats[0];
        int absentExcusedCount = attendanceStats[1];
        int absentUnexcusedCount = attendanceStats[2];
        int totalStudentsInSession = attendances.size(); // Tổng số SV trong buổi đó
        boolean isMarked = !attendances.isEmpty();
        boolean allAbsencesNotified = areAllAbsencesNotified(sessionId); // Sử dụng sessionId (String)

        VBox card = new VBox(8); // Giảm spacing
        card.setPrefWidth(360); // Độ rộng cố định hơn cho card
        card.setMinWidth(350);
        card.setPrefHeight(200); // Chiều cao cố định
        card.setPadding(new Insets(12)); // Padding đồng đều
        card.setStyle(
                "-fx-background-color: " + WHITE_COLOR + ";" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 8px;" + // Bo góc nhiều hơn
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 2, 2);" // Bóng mờ hơn
        );

        Label classNameLabel = new Label(session.getCourseName() != null ? session.getCourseName() : "N/A");
        classNameLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); // Font to hơn
        classNameLabel.setTextFill(Color.web("#333333"));
        classNameLabel.setWrapText(true);

        Label teacherLabel = new Label("GV: " + (session.getTeacher() != null ? session.getTeacher() : "N/A"));
        teacherLabel.setFont(Font.font("System", 13));
        teacherLabel.setTextFill(Color.web("#555555"));

        HBox teacherAndRoomBox = new HBox(10);
        Label roomLabel = new Label("Phòng: " + (session.getRoom() != null ? session.getRoom() : "N/A"));
        roomLabel.setFont(Font.font("System", 13));
        roomLabel.setTextFill(Color.web("#555555"));
        teacherAndRoomBox.getChildren().addAll(teacherLabel, roomLabel);


        String timeSlot = (session.getStartTime() != null ? session.getStartTime().toLocalTime().format(TIME_FORMATTER) : "N/A") +
                " - " +
                (session.getEndTime() != null ? session.getEndTime().toLocalTime().format(TIME_FORMATTER) : "N/A");
        Label scheduleLabel = new Label("Giờ học: " + timeSlot);
        scheduleLabel.setFont(Font.font("System", 13));
        scheduleLabel.setTextFill(Color.web("#555555"));


        VBox attendanceInfoBox = new VBox(5);
        attendanceInfoBox.setAlignment(Pos.TOP_LEFT);

        if (isMarked) {
            HBox statusLabelBox = new HBox(5);
            statusLabelBox.setAlignment(Pos.CENTER_LEFT);
            String attendanceText = String.format("Hiện diện: %d, Vắng CP: %d, Vắng KP: %d (Tổng: %d)",
                    presentCount, absentExcusedCount, absentUnexcusedCount, totalStudentsInSession);
            Label attendanceStatusLabel = new Label(attendanceText);
            attendanceStatusLabel.setFont(Font.font("System", 12)); // Font nhỏ hơn
            attendanceStatusLabel.setTextFill(Color.web("#555555"));
            attendanceStatusLabel.setWrapText(true);
            statusLabelBox.getChildren().add(attendanceStatusLabel);

            if (absentUnexcusedCount > 0 && !allAbsencesNotified) {
                Label warningLabel = new Label(" ⚠️");
                warningLabel.setFont(Font.font("System", 13));
                warningLabel.setTextFill(Color.web(RED_COLOR));
                warningLabel.setTooltip(new Tooltip("Còn HS vắng không phép chưa gọi điện xác nhận!"));
                statusLabelBox.getChildren().add(warningLabel);
            }

            HBox progressBarContainer = new HBox();
            progressBarContainer.setPrefHeight(8); // Thanh mỏng hơn
            progressBarContainer.setMaxWidth(Double.MAX_VALUE);
            progressBarContainer.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-background-radius: 4px;");
            progressBarContainer.setClip(new Rectangle(Double.MAX_VALUE, 8)); // Clip theo chiều cao

            double cardContentWidth = card.getPrefWidth() - card.getPadding().getLeft() - card.getPadding().getRight();


            if (totalStudentsInSession > 0) {
                double barWidthUnit = cardContentWidth / totalStudentsInSession;
                if (presentCount > 0) {
                    Rectangle presentRect = new Rectangle(presentCount * barWidthUnit, 8);
                    presentRect.setFill(Color.web(GREEN_COLOR));
                    progressBarContainer.getChildren().add(presentRect);
                }
                if (absentExcusedCount > 0) {
                    Rectangle absentExcusedRect = new Rectangle(absentExcusedCount * barWidthUnit, 8);
                    absentExcusedRect.setFill(Color.web(YELLOW_COLOR));
                    progressBarContainer.getChildren().add(absentExcusedRect);
                }
                if (absentUnexcusedCount > 0) {
                    Rectangle absentUnexcusedRect = new Rectangle(absentUnexcusedCount * barWidthUnit, 8);
                    absentUnexcusedRect.setFill(Color.web(RED_COLOR));
                    progressBarContainer.getChildren().add(absentUnexcusedRect);
                }
            }


            attendanceInfoBox.getChildren().addAll(statusLabelBox, progressBarContainer);
        } else {
            Label notMarkedLabel = new Label("Buổi học chưa điểm danh");
            notMarkedLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
            notMarkedLabel.setTextFill(Color.web(PRIMARY_COLOR)); // Màu khác cho nổi bật
            attendanceInfoBox.getChildren().add(notMarkedLabel);
        }

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        Button attendanceButton = new Button(isMarked ? "Xem/Sửa Điểm Danh" : "Bắt đầu Điểm Danh");
        attendanceButton.setPrefHeight(35);
        attendanceButton.setMaxWidth(Double.MAX_VALUE);
        attendanceButton.setStyle(
                "-fx-background-color: " + (isMarked ? GREEN_COLOR : PRIMARY_COLOR) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        attendanceButton.setOnAction(e -> handleClassSelection(session));

        card.getChildren().addAll(classNameLabel, teacherAndRoomBox, scheduleLabel, attendanceInfoBox, bottomSpacer, attendanceButton);
        return card;
    }
    // ... (Các phương thức còn lại giữ nguyên hoặc đã được sửa lỗi ở các bước trước) ...


    @Override
    public void refreshView() {
        // Gọi loadData để tải lại toàn bộ dữ liệu từ controller và DB
        // applyFilters() sẽ được gọi sau đó để cập nhật UI dựa trên filter hiện tại
        Platform.runLater(() -> { // Đảm bảo chạy trên JavaFX Application Thread
            loadData();
            // Sau khi loadData, currentDayFilter có thể đã được set lại (nếu loadData gọi setDefaultDayFilterAndApply)
            // hoặc applyFilters() được gọi ở cuối loadData().
            // Nếu không, hãy gọi applyFilters() ở đây để đảm bảo UI được cập nhật.
            // Tuy nhiên, setDefaultDayFilterAndApply() sẽ gọi applyFilters() nếu nó thay đổi lựa chọn.
            // Và loadData() ở cuối cũng gọi applyFilters().
            // Nên không cần gọi applyFilters() thêm ở đây.
            // Nếu setDefaultDayFilterAndApply được gọi cuối cùng trong initializeView thì nó cũng đã gọi applyFilters rồi
        });
    }

    @Override
    public void onActivate() {
        super.onActivate(); // Gọi super nếu có logic quan trọng
        // System.out.println(getViewId() + " activated. Refreshing data.");
        refreshView(); // Luôn làm mới dữ liệu khi màn hình được kích hoạt
    }

    // --- Các phương thức filter và search nội bộ (giữ nguyên logic, chỉ đảm bảo dùng allLoadedSessions) ---
    private List<ClassSession> filterSessionsByDayInternal(List<ClassSession> sessionsToFilter, String day) {
        if (attendanceController == null || sessionsToFilter == null || day == null || day.trim().isEmpty()) {
            return new ArrayList<>(sessionsToFilter); // Trả về bản sao nếu không có gì để lọc
        }
        try {
            return attendanceController.filterSessionsByDay(sessionsToFilter, day);
        } catch (Exception e) {
            showError("Lỗi khi lọc buổi học theo ngày: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(sessionsToFilter); // Trả về bản sao nếu có lỗi
        }
    }

    private List<ClassSession> filterSessionsByStatusInternal(List<ClassSession> sessionsToFilter, String status) {
        if (sessionsToFilter == null || sessionAttendanceMap == null || status == null) {
            return new ArrayList<>(sessionsToFilter);
        }
        // (Logic của filterSessionsByStatusInternal giữ nguyên)
        List<ClassSession> result = new ArrayList<>();
        switch (status) {
            case "UNMARKED":
                result = sessionsToFilter.stream()
                        .filter(session -> sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>()).isEmpty())
                        .collect(Collectors.toList());
                break;
            case "MARKED":
                result = sessionsToFilter.stream()
                        .filter(session -> !sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>()).isEmpty())
                        .collect(Collectors.toList());
                break;
            case "ALL":
            default:
                result = new ArrayList<>(sessionsToFilter);
                break;
        }
        return result;
    }

    private List<ClassSession> searchSessionsInternal(List<ClassSession> sessionsToSearch, String keyword) {
        if (attendanceController == null || sessionsToSearch == null) {
            return new ArrayList<>(sessionsToSearch);
        }
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return new ArrayList<>(sessionsToSearch);
            }
            return attendanceController.searchSessions(sessionsToSearch, keyword.trim());
        } catch (Exception e) {
            showError("Lỗi khi tìm kiếm buổi học: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(sessionsToSearch);
        }
    }
    // --- Các phương thức cập nhật UI (giữ nguyên logic) ---
    private void updateFilterButtonCounts(List<ClassSession> baseFilteredSessions) { // baseFilteredSessions là list đã lọc theo search và ngày/tuần
        if (baseFilteredSessions == null || sessionAttendanceMap == null || allButton == null || unmarkedButton == null || markedButton == null) {
            return;
        }
        int totalForDayAndSearch = baseFilteredSessions.size();
        int unmarked = 0;
        for (ClassSession session : baseFilteredSessions) {
            if (sessionAttendanceMap.getOrDefault(session.getId(), new ArrayList<>()).isEmpty()) {
                unmarked++;
            }
        }
        int marked = totalForDayAndSearch - unmarked;

        allButton.setText("Tất cả (" + totalForDayAndSearch + ")");
        unmarkedButton.setText("Chưa ĐD (" + unmarked + ")");
        markedButton.setText("Đã ĐD (" + marked + ")");
    }

    private void updateClassCards(List<ClassSession> sessionsToDisplay) {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        if (sessionsToDisplay == null || sessionsToDisplay.isEmpty()) {
            Label noClassesLabel = new Label("Không có buổi học nào phù hợp với bộ lọc hiện tại.");
            noClassesLabel.setFont(Font.font("System", 16));
            noClassesLabel.setTextFill(Color.gray(0.6));
            noClassesLabel.setPadding(new Insets(20));
            HBox centerPane = new HBox(noClassesLabel);
            centerPane.setAlignment(Pos.CENTER);
            centerPane.prefWidthProperty().bind(cardsPane.widthProperty()); // Để căn giữa trong FlowPane
            cardsPane.getChildren().add(centerPane);
        } else {
            for (ClassSession session : sessionsToDisplay) {
                if (session != null) { // Thêm kiểm tra null cho session
                    cardsPane.getChildren().add(createClassCard(session));
                }
            }
        }
    }
    // --- Các phương thức getAttendanceStats, areAllAbsencesNotified, exportToExcel, viewAttendanceList, setActiveStatusButton, handleClassSelection giữ nguyên logic ---
    private int[] getAttendanceStats(String sessionId) {
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());
        int presentCount = 0;
        int absentExcusedCount = 0;
        int absentUnexcusedCount = 0;
        for (Attendance attendance : attendances) {
            if (attendance.isPresent()) { // Giả sử có phương thức isPresent()
                presentCount++;
            } else if (attendance.hasPermission()) { // Giả sử có phương thức hasPermission()
                absentExcusedCount++;
            } else {
                absentUnexcusedCount++;
            }
        }
        return new int[]{presentCount, absentExcusedCount, absentUnexcusedCount};
    }

    private boolean areAllAbsencesNotified(String sessionId) {
        List<Attendance> attendances = sessionAttendanceMap.getOrDefault(sessionId, new ArrayList<>());
        for (Attendance attendance : attendances) {
            // Nếu vắng, không có phép, VÀ chưa gọi điện -> false
            if (!attendance.isPresent() && !attendance.hasPermission() && !attendance.isCalled()) {
                return false;
            }
        }
        return true; // Tất cả các trường hợp vắng không phép đã được gọi, hoặc không có trường hợp nào
    }
    private void exportToExcel() {
        if (attendanceController == null) {
            showError("Bộ điều khiển điểm danh chưa được khởi tạo.");
            return;
        }
        try {
            // Lấy danh sách session hiện đang hiển thị trên UI (đã qua filter)
            List<ClassSession> sessionsToExport = new ArrayList<>();
            for(Node node : cardsPane.getChildren()){
                if(node.getUserData() instanceof ClassSession){ // Giả sử bạn set UserData cho card là ClassSession
                    sessionsToExport.add((ClassSession) node.getUserData());
                }
            }
            // Nếu không set UserData, bạn cần lấy lại danh sách đã filter bằng cách gọi lại logic filter
            // Hoặc tốt nhất là controller nên có phương thức export dựa trên filter hiện tại.
            // Tạm thời, nếu không có cách lấy list đã filter, có thể export allLoadedSessions (đã load từ DB)
            // và nói rõ là đang export tất cả (hoặc theo filter cuối cùng nếu bạn lưu trạng thái filter ở controller)

            // boolean success = attendanceController.exportToExcel(allLoadedSessions, sessionAttendanceMap); // Xuất toàn bộ
            // Hoặc chỉ xuất những gì đang hiển thị (cần cách lấy lại list đã filter)
            List<ClassSession> currentlyDisplayedSessions = getCurrentDisplayedSessions();


            if (currentlyDisplayedSessions.isEmpty() && !allLoadedSessions.isEmpty()){
                Alert confirmExportAll = new Alert(Alert.AlertType.CONFIRMATION,
                        "Không có lớp nào đang được hiển thị theo bộ lọc hiện tại. Bạn có muốn xuất toàn bộ danh sách các buổi học đã tải không?",
                        ButtonType.YES, ButtonType.NO);
                confirmExportAll.setTitle("Xác nhận xuất Excel");
                confirmExportAll.setHeaderText("Xuất dữ liệu điểm danh");
                Optional<ButtonType> result = confirmExportAll.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    // Tiếp tục xuất allLoadedSessions
                } else {
                    showInfo("Đã hủy thao tác xuất Excel.");
                    return;
                }
            } else if (currentlyDisplayedSessions.isEmpty() && allLoadedSessions.isEmpty()) {
                showInfo("Không có dữ liệu buổi học nào để xuất.");
                return;
            }


            // Sử dụng currentlyDisplayedSessions nếu nó không rỗng, ngược lại là allLoadedSessions
            List<ClassSession> finalSessionsToExport = !currentlyDisplayedSessions.isEmpty() ? currentlyDisplayedSessions : allLoadedSessions;


            boolean success = false;
                    //attendanceController.exportAttendancesToExcel(finalSessionsToExport, sessionAttendanceMap);
            if (success) {
                showSuccess("Xuất dữ liệu Excel thành công!");
            } else {
                showError("Không thể xuất dữ liệu Excel. Có thể không có dữ liệu hoặc lỗi hệ thống.");
            }
        } catch (Exception e) {
            showError("Lỗi khi xuất dữ liệu Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private List<ClassSession> getCurrentDisplayedSessions() {
        // This method reconstructs the filtered list based on current filter states
        // It's better if applyFilters() itself returns or stores this final list
        List<ClassSession> result = new ArrayList<>(allLoadedSessions);
        if (currentSearchKeyword != null && !currentSearchKeyword.isEmpty()) {
            result = searchSessionsInternal(result, currentSearchKeyword);
        }
        if (currentDayFilter != null) {
            result = filterSessionsByDayInternal(result, currentDayFilter); // This gives all sessions for that day name
            // Further filter for current week
            LocalDate today = LocalDate.now();
            DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;
            LocalDate startOfWeek = today.with(firstDayOfWeek);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            final LocalDate finalStartOfWeek = startOfWeek;
            final LocalDate finalEndOfWeek = endOfWeek;
            result = result.stream()
                    .filter(session -> session.getDate() != null &&
                            !session.getDate().isBefore(finalStartOfWeek) &&
                            !session.getDate().isAfter(finalEndOfWeek))
                    .collect(Collectors.toList());
        }
        return filterSessionsByStatusInternal(result, currentStatusFilter);
    }


    private void viewAttendanceList() {
        if (navigationController == null) {
            showError("Bộ điều khiển điều hướng chưa được khởi tạo.");
            return;
        }
        try {
            // Navigate to a screen that shows a table of all absence records, possibly with filters
            // For example, "absence_records_table_view"
            navigationController.navigateTo("absence-call-table"); // ID của màn hình danh sách vắng mặt
        } catch (Exception e) {
            showError("Lỗi khi mở danh sách điểm danh tổng hợp: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void setActiveStatusButton(Button selectedButton) {
        if (allButton == null || unmarkedButton == null || markedButton == null) return;
        styleStatusButton(allButton, false);
        styleStatusButton(unmarkedButton, false);
        styleStatusButton(markedButton, false);
        if (selectedButton != null) {
            styleStatusButton(selectedButton, true);
        }
    }
    private void handleClassSelection(ClassSession session) {
        if (navigationController == null || mainController == null) {
            showError("Lỗi hệ thống: Controller điều hướng hoặc controller chính chưa sẵn sàng.");
            return;
        }
        if (session == null || session.getId() == null) {
            showError("Lỗi: Không có thông tin buổi học để mở chi tiết điểm danh.");
            return;
        }
        try {
            // Lưu session được chọn vào MainController để AbsenceCallView có thể truy cập
            mainController.setSessionDetail(session); // Giả sử MainController có phương thức này
            navigationController.navigateTo("absence-call-src.view"); // ID của AbsenceCallView
        } catch (Exception e) {
            showError("Lỗi khi mở màn hình điểm danh chi tiết: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // showError, showSuccess, showInfo (giữ nguyên)
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root != null && root.getScene() != null) alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root != null && root.getScene() != null) alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root != null && root.getScene() != null) alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}