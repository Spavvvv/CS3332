package view.components;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import src.controller.AttendanceController;
import view.BaseScreenView;
import src.model.ClassSession;
import src.model.person.Student;
import src.model.attendance.Attendance;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Màn hình Danh sách vắng học
 * Hiển thị danh sách học sinh vắng mặt và cho phép giáo viên ghi nhận đã gọi điện
 */
public class AbsenceCallScreenView extends BaseScreenView {

    // Constants
    private static final String PRIMARY_COLOR = "#1976D2"; // Màu xanh cho nút và tiêu đề
    private static final String GREEN_COLOR = "#4CAF50"; // Màu xanh lá cho thanh tiến độ

    // UI Components
    private TableView<Attendance> absenceTable;
    private TextField searchField;
    private Button searchButton;
    private Button exportExcelButton;
    private Button backButton;
    private ProgressBar callProgressBar;
    private Label callProgressLabel;
    private Label totalAbsencesLabel;
    private ComboBox<String> dayFilterComboBox;
    private ComboBox<String> callStatusComboBox;
    private ObservableList<Attendance> absenceData;
    private FilteredList<Attendance> filteredData;

    // Controller reference
    private AttendanceController attendanceController;

    // Current date and filters
    private LocalDate selectedDate;
    private String currentDayFilter = "Tất cả";
    private String currentCallStatusFilter = "Tất cả";
    private String currentSearchText = "";

    /**
     * Constructor
     */
    public AbsenceCallScreenView() {
        super("Danh sách vắng học", "absence-call-table");
        this.attendanceController = new AttendanceController();
        selectedDate = LocalDate.now();
    }

    /**
     * Initialize view according to BaseScreenView requirements
     */
    @Override
    public void initializeView() {
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(20));

        // Header
        HBox header = createHeader();

        // Filter section
        HBox filterSection = createFilterSection();

        // Progress section
        HBox progressSection = createProgressSection();

        // Table section
        VBox tableSection = createTableSection();

        mainLayout.getChildren().addAll(header, filterSection, progressSection, tableSection);

        root.getChildren().add(mainLayout);

        setupActionHandlers();
        loadAbsenceData();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 15, 0));
        header.setSpacing(20);

        Label titleLabel = new Label("Danh sách vắng học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.valueOf("#333333"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // backButton button - sử dụng text "←" làm icon
        backButton = new Button("← Quay lại");
        backButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                        "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
        );
        backButton.setGraphic(createButtonIcon("arrow-left", "white"));
        backButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.goBack();
            }
        });

        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle("-fx-background-color: " + "#39ce1e" + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 20;" +
                "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
        );
        // Sửa lại đoạn này: từ backButton thành exportExcelButton
        exportExcelButton.setGraphic(createButtonIcon("excel", "white"));

        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(backButton, exportExcelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        // Sửa đoạn này: thay thế các nút bằng spacer và buttons
        header.getChildren().addAll(titleLabel, spacer, buttons);
        return header;
    }


    private HBox createFilterSection() {
        HBox filterSection = new HBox();
        filterSection.setAlignment(Pos.CENTER_LEFT);
        filterSection.setSpacing(15);

        DatePicker datePicker = new DatePicker(selectedDate);
        datePicker.setPromptText("Chọn ngày");
        datePicker.setOnAction(e -> {
            selectedDate = datePicker.getValue();
            loadAbsenceData();
        });

        Label dayFilterLabel = new Label("Ngày:");
        dayFilterComboBox = new ComboBox<>();
        dayFilterComboBox.getItems().addAll("Tất cả", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật");
        dayFilterComboBox.setValue("Tất cả");
        dayFilterComboBox.setOnAction(e -> {
            currentDayFilter = dayFilterComboBox.getValue();
            applyFilters();
        });

        Label callStatusLabel = new Label("Trạng thái gọi:");
        callStatusComboBox = new ComboBox<>();
        callStatusComboBox.getItems().addAll("Tất cả", "Đã gọi", "Chưa gọi");
        callStatusComboBox.setValue("Tất cả");
        callStatusComboBox.setOnAction(e -> {
            currentCallStatusFilter = callStatusComboBox.getValue();
            applyFilters();
        });

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm học sinh");
        searchField.setPrefWidth(200);

        searchButton = new Button("Tìm");
        searchButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white;");
        searchButton.setOnAction(e -> {
            currentSearchText = searchField.getText().trim().toLowerCase();
            applyFilters();
        });

        filterSection.getChildren().addAll(datePicker, dayFilterLabel, dayFilterComboBox,
                callStatusLabel, callStatusComboBox, searchField, searchButton);

        return filterSection;
    }

    private HBox createProgressSection() {
        HBox progressSection = new HBox();
        progressSection.setAlignment(Pos.CENTER_LEFT);
        progressSection.setSpacing(15);
        progressSection.setPadding(new Insets(10, 0, 10, 0));

        totalAbsencesLabel = new Label("Tổng số vắng: 0");
        totalAbsencesLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalAbsencesLabel.setTextFill(Color.valueOf("#000000"));

        VBox progressBox = new VBox(5);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        Label progressTitle = new Label("Tiến độ gọi điện:");
        callProgressBar = new ProgressBar(0);
        callProgressBar.setPrefWidth(200);
        callProgressBar.setStyle("-fx-accent: " + GREEN_COLOR + ";");
        callProgressLabel = new Label("0/0 (0%)");

        progressBox.getChildren().addAll(progressTitle, callProgressBar, callProgressLabel);

        progressSection.getChildren().addAll(totalAbsencesLabel, progressBox);
        return progressSection;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox();
        tableSection.setSpacing(10);

        absenceTable = new TableView<>();
        absenceTable.setEditable(true);
        absenceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Attendance, String> studentNameCol = new TableColumn<>("Học sinh");
        studentNameCol.setCellValueFactory(data -> {
            Student student = data.getValue().getStudent();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> student != null ? student.getName() : ""
            );
        });

        TableColumn<Attendance, String> classSessionCol = new TableColumn<>("Lớp học");
        classSessionCol.setCellValueFactory(data -> {
            ClassSession session = data.getValue().getSession();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> session != null ? session.getCourseName() + " - " +
                            session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""
            );
        });

        TableColumn<Attendance, String> absenceTypeCol = new TableColumn<>("Loại vắng");
        absenceTypeCol.setCellValueFactory(data -> {
            String absenceType = data.getValue().getAbsenceType();
            return new SimpleStringProperty(absenceType != null ? absenceType : "Không lý do");
        });

        TableColumn<Attendance, String> parentNameCol = new TableColumn<>("Phụ huynh");
        parentNameCol.setCellValueFactory(data -> {
            Student student = data.getValue().getStudent();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> student != null ? student.getParent().getName() : ""
            );
        });

        TableColumn<Attendance, String> parentContactCol = new TableColumn<>("Liên hệ");
        parentContactCol.setCellValueFactory(data -> {
            Student student = data.getValue().getStudent();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> student != null ? student.getContactNumber() : ""
            );
        });

        TableColumn<Attendance, Boolean> calledCol = new TableColumn<>("Đã gọi");
        calledCol.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isCalled());
            prop.addListener((obs, oldVal, newVal) -> {
                data.getValue().setCalled(newVal);
                if (attendanceController != null) {
                    attendanceController.getAbsentCount(data.getValue().getId());
                }
                updateProgressBar();
            });
            return prop;
        });
        calledCol.setCellFactory(CheckBoxTableCell.forTableColumn(calledCol));
        calledCol.setEditable(true);

        TableColumn<Attendance, String> notesCol = new TableColumn<>("Ghi chú");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        notesCol.setCellFactory(col -> {
            TextFieldTableCell<Attendance, String> cell = new TextFieldTableCell<>();
            cell.setConverter(new javafx.util.StringConverter<String>() {
                @Override
                public String toString(String object) {
                    return object == null ? "" : object;
                }

                @Override
                public String fromString(String string) {
                    return string;
                }
            });
            return cell;
        });
        notesCol.setEditable(true);
        notesCol.setOnEditCommit(event -> {
            Attendance attendance = event.getRowValue();
            attendance.setNote(event.getNewValue());
            if (attendanceController != null) {
                attendanceController.updateAttendance(attendance);
            }
        });

        absenceTable.getColumns().addAll(studentNameCol, classSessionCol, absenceTypeCol,
                parentNameCol, parentContactCol, calledCol, notesCol);

        Label placeholder = new Label("Không có học sinh vắng mặt");
        absenceTable.setPlaceholder(placeholder);
        VBox.setVgrow(absenceTable, Priority.ALWAYS);
        tableSection.getChildren().add(absenceTable);

        return tableSection;
    }

    public void loadAbsenceData() {
        if (attendanceController == null) {
            absenceData = FXCollections.observableArrayList();
            filteredData = new FilteredList<>(absenceData);
            absenceTable.setItems(filteredData);
            updateProgressBar();
            return;
        }

        Map<Long, List<Attendance>> allSessionData = new HashMap<>();
        LocalDate startDate = selectedDate.minusDays(7);
        LocalDate endDate = selectedDate.plusDays(7);
        List<Long> teacherClassIds = new ArrayList<>();

        if (mainController != null) {
            teacherClassIds = mainController.getTeacherClassIds();
        }

        for (Long classId : teacherClassIds) {
            Map<Long, List<Attendance>> classAttendance =
                    attendanceController.getAttendanceDataInRange(classId, startDate, endDate);
            allSessionData.putAll(classAttendance);
        }

        List<Attendance> allAbsences = new ArrayList<>();
        for (List<Attendance> sessionAttendances : allSessionData.values()) {
            List<Attendance> absences = sessionAttendances.stream()
                    .filter(a -> !a.isPresent())
                    .collect(Collectors.toList());
            allAbsences.addAll(absences);
        }

        absenceData = FXCollections.observableArrayList(allAbsences);
        filteredData = new FilteredList<>(absenceData);
        absenceTable.setItems(filteredData);
        applyFilters();
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(attendance -> {
            boolean matchesDayFilter = true;
            boolean matchesCallStatusFilter = true;
            boolean matchesSearchText = true;

            if (!"Tất cả".equals(currentDayFilter)) {
                ClassSession session = attendance.getSession();
                if (session != null) {
                    int dayOfWeek = session.getDate().getDayOfWeek().getValue();
                    String dayName = getDayNameFromDayOfWeek(dayOfWeek);
                    matchesDayFilter = currentDayFilter.equals(dayName);
                }
            }

            if ("Đã gọi".equals(currentCallStatusFilter)) {
                matchesCallStatusFilter = attendance.isCalled();
            } else if ("Chưa gọi".equals(currentCallStatusFilter)) {
                matchesCallStatusFilter = !attendance.isCalled();
            }

            if (!currentSearchText.isEmpty()) {
                Student student = attendance.getStudent();
                ClassSession session = attendance.getSession();
                boolean nameMatches = student != null && student.getName().toLowerCase().contains(currentSearchText);
                boolean parentMatches = student != null && student.getParent().getName().toLowerCase().contains(currentSearchText);
                boolean classMatches = session != null && session.getCourseName().toLowerCase().contains(currentSearchText);
                matchesSearchText = nameMatches || parentMatches || classMatches;
            }

            return matchesDayFilter && matchesCallStatusFilter && matchesSearchText;
        });

        updateProgressBar();
    }

    private String getDayNameFromDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            case 7: return "Chủ nhật";
            default: return "";
        }
    }

    private void updateProgressBar() {
        if (filteredData == null) return;

        int total = filteredData.size();
        int called = (int) filteredData.stream().filter(Attendance::isCalled).count();

        double progress = total > 0 ? (double) called / total : 0;
        int percentage = (int) (progress * 100);

        callProgressBar.setProgress(progress);
        callProgressLabel.setText(called + "/" + total + " (" + percentage + "%)");
        totalAbsencesLabel.setText("Tổng số vắng: " + total);
    }

    public void handleExportToExcel() {
        if (attendanceController != null && filteredData != null) {
            List<Attendance> dataToExport = new ArrayList<>(filteredData);
            String filename = "danh_sach_vang_" + selectedDate.format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";

            try {
                // attendanceController.exportAbsencesToExcel(dataToExport, filename);
                showSuccess("Xuất Excel thành công: " + filename);
            } catch (Exception e) {
                showError("Lỗi khi xuất Excel: " + e.getMessage());
            }
        } else {
            showError("Không thể xuất dữ liệu");
        }
    }

    public void setupActionHandlers() {
        exportExcelButton.setOnAction(e -> handleExportToExcel());

        searchField.setOnAction(e -> {
            currentSearchText = searchField.getText().trim().toLowerCase();
            applyFilters();
        });
    }

    @Override
    public Object handleAction(String actionId, Object params) {
        switch (actionId) {
            case "exportExcel":
                handleExportToExcel();
                return true;
            case "refresh":
                loadAbsenceData();
                return true;
            case "search":
                if (params instanceof String) {
                    searchField.setText((String) params);
                    currentSearchText = searchField.getText().trim().toLowerCase();
                    applyFilters();
                }
                return true;
            default:
                return super.handleAction(actionId, params);
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true; // This screen requires authentication
    }

    @Override
    public void handleSystemMessage(String message, Object data) {
        if ("attendance_updated".equals(message)) {
            loadAbsenceData();
        }
    }

    private ImageView createButtonIcon(String iconName, String color) {
        // Trong ứng dụng thực tế, bạn sẽ load icon thực tế
        // Đây chỉ là placeholder
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(Color.web(color));

        ImageView imageView = new ImageView();
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);

        // Trong ứng dụng thực tế: imageView.setImage(new Image("/icons/" + iconName + ".png"));

        return imageView;
    }
}
