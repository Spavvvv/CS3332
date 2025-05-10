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
import src.model.ClassSession; // Requires ClassSession model to use String IDs
import src.model.person.Student; // Requires Student model to use String IDs
import src.model.attendance.Attendance; // Requires Attendance model to use String IDs

import java.sql.SQLException;
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
        // Initialize controller - DAOs should be managed within the controller
        try {
            attendanceController = new AttendanceController();
        } catch (SQLException e) {
            // Handle the SQLException during controller initialization
            showError("Lỗi khởi tạo bộ điều khiển điểm danh: " + e.getMessage());
            e.printStackTrace();
            // Consider exiting or disabling functionality if controller fails to initialize
        }
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
        // Data loading is triggered in onActivate or explicitly called after initialization
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 15, 0));
        header.setSpacing(20);
        header.setAlignment(Pos.CENTER_LEFT); // Align children to the left center

        Label titleLabel = new Label("Danh sách vắng học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.valueOf("#333333"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // backButton button
        backButton = new Button("← Quay lại");
        backButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-content-display: LEFT;" // Align text to the left
        );
        // You might want to add a proper graphic here if you have icon files
        // backButton.setGraphic(createButtonIcon("arrow-left", "white")); // This requires actual icon loading logic
        // Simple text label graphic for now
        Label arrow = new Label("←");
        arrow.setTextFill(Color.WHITE);
        arrow.setFont(Font.font("System", FontWeight.BOLD, 12)); // Smaller font for arrow
        backButton.setGraphic(arrow);
        backButton.setContentDisplay(ContentDisplay.LEFT); // Ensure graphic is on the left

        backButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.goBack();
            } else {
                showError("Bộ điều khiển điều hướng chưa được khởi tạo.");
            }
        });

        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle("-fx-background-color: " + "#39ce1e" + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 20;" +
                "-fx-background-radius: 20;" +
                "-fx-alignment: CENTER;" +
                "-fx-content-display: LEFT;" // Align text to the left
        );
        // You might want to add a proper graphic here if you have icon files
        // exportExcelButton.setGraphic(createButtonIcon("excel", "white")); // This requires actual icon loading logic
        Label excelIcon = new Label("📊"); // Using an emoji as a placeholder graphic
        excelIcon.setTextFill(Color.WHITE);
        excelIcon.setFont(Font.font("System", 12));
        exportExcelButton.setGraphic(excelIcon);
        exportExcelButton.setContentDisplay(ContentDisplay.LEFT);

        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(backButton, exportExcelButton);
        buttons.setAlignment(Pos.CENTER_RIGHT); // Align buttons to the right center

        header.getChildren().addAll(titleLabel, spacer, buttons);
        return header;
    }


    private HBox createFilterSection() {
        HBox filterSection = new HBox();
        filterSection.setAlignment(Pos.CENTER_LEFT);
        filterSection.setSpacing(15);
        filterSection.setPadding(new Insets(0, 0, 10, 0)); // Add some bottom padding

        DatePicker datePicker = new DatePicker(selectedDate);
        datePicker.setPromptText("Chọn ngày");
        datePicker.setOnAction(e -> {
            selectedDate = datePicker.getValue();
            loadAbsenceData(); // Reload data when date changes
        });

        Label dayFilterLabel = new Label("Ngày:");
        dayFilterLabel.setFont(Font.font("System", FontWeight.BOLD, 12)); // Smaller font for label
        dayFilterComboBox = new ComboBox<>();
        dayFilterComboBox.getItems().addAll("Tất cả", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật");
        dayFilterComboBox.setValue("Tất cả");
        dayFilterComboBox.setOnAction(e -> {
            currentDayFilter = dayFilterComboBox.getValue();
            applyFilters();
        });

        Label callStatusLabel = new Label("Trạng thái gọi:");
        callStatusLabel.setFont(Font.font("System", FontWeight.BOLD, 12)); // Smaller font for label
        callStatusComboBox = new ComboBox<>();
        callStatusComboBox.getItems().addAll("Tất cả", "Đã gọi", "Chưa gọi");
        callStatusComboBox.setValue("Tất cả");
        callStatusComboBox.setOnAction(e -> {
            currentCallStatusFilter = callStatusComboBox.getValue();
            applyFilters();
        });

        // Spacer to push search to the right
        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);


        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm học sinh");
        searchField.setPrefWidth(200);
        searchField.setPrefHeight(30); // Match ComboBox height
        searchField.setStyle(
                "-fx-background-color: #f0f0f0; -fx-background-radius: 5; -fx-padding: 0 10;" // Added padding
        );

        searchButton = new Button("Tìm");
        searchButton.setPrefHeight(30); // Match ComboBox height
        searchButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> {
            currentSearchText = searchField.getText().trim().toLowerCase();
            applyFilters();
        });

        filterSection.getChildren().addAll(datePicker, dayFilterLabel, dayFilterComboBox,
                callStatusLabel, callStatusComboBox, filterSpacer, searchField, searchButton); // Added spacer

        return filterSection;
    }

    private HBox createProgressSection() {
        HBox progressSection = new HBox();
        progressSection.setAlignment(Pos.CENTER_LEFT);
        progressSection.setSpacing(25); // Increased spacing
        progressSection.setPadding(new Insets(10, 0, 15, 0)); // Adjusted padding

        totalAbsencesLabel = new Label("Tổng số vắng: 0");
        totalAbsencesLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalAbsencesLabel.setTextFill(Color.valueOf("#000000"));

        VBox progressBox = new VBox(3); // Reduced spacing
        progressBox.setAlignment(Pos.CENTER_LEFT);

        Label progressTitle = new Label("Tiến độ gọi điện:");
        progressTitle.setFont(Font.font("System", 12)); // Smaller font
        progressTitle.setTextFill(Color.valueOf("#555555")); // Gray color

        callProgressBar = new ProgressBar(0);
        callProgressBar.setPrefWidth(250); // Increased width
        callProgressBar.setPrefHeight(8); // Reduced height
        callProgressBar.setStyle("-fx-accent: " + GREEN_COLOR + "; -fx-control-inner-background: " + "#e0e0e0" + ";"); // Added background color

        callProgressLabel = new Label("0/0 (0%)");
        callProgressLabel.setFont(Font.font("System", 12)); // Smaller font
        callProgressLabel.setTextFill(Color.valueOf("#555555")); // Gray color


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
            // Ensure session.getId() and session.getClassName() and session.getDate() exist and are correct types
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> session != null ? session.getClassName() + " - " +
                            (session.getDate() != null ? session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Không rõ ngày") : ""
            );
        });

        TableColumn<Attendance, String> absenceTypeCol = new TableColumn<>("Loại vắng");
        absenceTypeCol.setCellValueFactory(data -> {
            String absenceType = data.getValue().getAbsenceType(); // Ensure getAbsenceType exists and returns String
            return new SimpleStringProperty(absenceType != null && !absenceType.isEmpty() ? absenceType : "Không lý do");
        });

        TableColumn<Attendance, String> parentNameCol = new TableColumn<>("Phụ huynh");
        parentNameCol.setCellValueFactory(data -> {
            Student student = data.getValue().getStudent();
            // Ensure student.getParent() exists and getParent().getName() exists and returns String
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> student != null && student.getParent() != null ? student.getParent().getName() : ""
            );
        });

        TableColumn<Attendance, String> parentContactCol = new TableColumn<>("Liên hệ");
        parentContactCol.setCellValueFactory(data -> {
            Student student = data.getValue().getStudent();
            // Ensure student.getContactNumber() exists and returns String
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> student != null ? student.getContactNumber() : ""
            );
        });

        TableColumn<Attendance, Boolean> calledCol = new TableColumn<>("Đã gọi");
        calledCol.setCellValueFactory(data -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(data.getValue().isCalled()); // Ensure isCalled exists
            prop.addListener((obs, oldVal, newVal) -> {
                // Ensure data.getValue().getId() returns String
                String attendanceId = data.getValue().getId();
                if (attendanceController != null && attendanceId != null) {
                    // Update the attendance record using the controller
                    attendanceController.markAttendanceCalled(attendanceId, newVal); // Controller method must accept String ID
                    data.getValue().setCalled(newVal); // Update model after successful controller call
                    updateProgressBar();
                } else {
                    // Revert the checkbox state if update fails (optional but good practice)
                    prop.removeListener((obs2, oldVal2, newVal2) -> {}); // Avoid infinite loop
                    prop.set(oldVal);
                    prop.addListener((obs2, oldVal2, newVal2) -> {
                        data.getValue().setCalled(newVal2);
                        if (attendanceController != null && attendanceId != null) {
                            attendanceController.markAttendanceCalled(attendanceId, newVal2);
                            updateProgressBar();
                        }
                    });
                    showError("Không thể cập nhật trạng thái gọi.");
                }
            });
            return prop;
        });
        calledCol.setCellFactory(CheckBoxTableCell.forTableColumn(calledCol));
        calledCol.setEditable(true);

        TableColumn<Attendance, String> notesCol = new TableColumn<>("Ghi chú");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("note")); // Ensure 'note' property exists and is String
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
            // Ensure attendance.getId() returns String
            String attendanceId = attendance.getId();
            String newNote = event.getNewValue();
            if (attendanceController != null && attendanceId != null) {
                attendance.setNote(newNote); // Update model
                // Update the attendance note using the controller
                attendanceController.updateAttendanceNote(attendanceId, newNote); // Controller method must accept String ID
            } else {
                showError("Không thể cập nhật ghi chú.");
            }
        });

        absenceTable.getColumns().addAll(studentNameCol, classSessionCol, absenceTypeCol,
                parentNameCol, parentContactCol, calledCol, notesCol);

        Label placeholder = new Label("Không có học sinh vắng mặt");
        placeholder.setTextFill(Color.gray(0.6)); // Gray text
        absenceTable.setPlaceholder(placeholder);
        VBox.setVgrow(absenceTable, Priority.ALWAYS);
        tableSection.getChildren().add(absenceTable);

        return tableSection;
    }

    /**
     * Loads absence data based on the selected date and teacher's classes.
     * Requires ClassSession and Attendance models with String IDs,
     * and AttendanceController methods accepting and returning objects with String IDs.
     * Assumes mainController.getTeacherClassIds() now returns List<String>.
     */
    public void loadAbsenceData() {
        if (attendanceController == null) {
            absenceData = FXCollections.observableArrayList();
            filteredData = new FilteredList<>(absenceData);
            absenceTable.setItems(filteredData);
            updateProgressBar();
            applyFilters(); // Apply filters even if no data loaded
            return;
        }

        // Get all teacher's class IDs (Assuming mainController.getTeacherClassIds() returns List<String>)
        List<String> teacherClassIds = new ArrayList<>();
        if (mainController != null) {
            // Assuming mainController.getTeacherClassIds() now returns List<String>
            // If it still returns List<Long>, you would need to convert them to String here.
            // For this fix, we assume it returns List<String>.
            List<Object> ids = mainController.getTeacherClassIds(); // Assuming it returns List<Object>
            for (Object id : ids) {
                if (id instanceof String) {
                    teacherClassIds.add((String) id);
                } else if (id != null) {
                    System.err.println("Unexpected class ID type from mainController: " + id.getClass().getName());
                }
            }
        } else {
            System.err.println("mainController is null. Cannot get teacher class IDs.");
            // Handle case where mainController is null - maybe show an error or load no data
            absenceData = FXCollections.observableArrayList();
            filteredData = new FilteredList<>(absenceData);
            absenceTable.setItems(filteredData);
            updateProgressBar();
            applyFilters();
            return; // Exit early if mainController is not available
        }


        List<Attendance> allAbsences = new ArrayList<>();
        try {
            // Get all class sessions for the date range we're interested in
            // Iterate over String class IDs
            for (String classId : teacherClassIds) {
                if (classId == null || classId.trim().isEmpty()) continue; // Skip invalid IDs

                // attendanceController.getClassSessionsByClassId must accept String
                List<ClassSession> classSessions = attendanceController.getClassSessionsByClassId(classId);

                // Filter sessions to match the selected date (exact date match)
                List<ClassSession> sessionsOnSelectedDate = classSessions.stream()
                        .filter(session -> session.getDate() != null && session.getDate().isEqual(selectedDate))
                        .collect(Collectors.toList());


                // Get attendance for each session on the selected date
                for (ClassSession session : sessionsOnSelectedDate) {
                    // session.getId() must return String
                    if (session.getId() == null || session.getId().trim().isEmpty()) continue; // Skip sessions with no ID

                    // Get all attendance records for this session
                    // attendanceController.getAttendanceBySessionId must accept String
                    List<Attendance> sessionAttendance = attendanceController.getAttendanceBySessionId(session.getId());

                    // Filter to only include absences (where isPresent is false)
                    List<Attendance> absences = sessionAttendance.stream()
                            .filter(a -> !a.isPresent()) // Requires isPresent() method in Attendance model
                            .collect(Collectors.toList());

                    // Add session and student info to Absence objects if needed for table display
                    // Assuming Attendance object already links back to Student and ClassSession
                    absences.forEach(a -> {
                        if (a.getStudent() == null) {
                            // Try to set student if not already set (might require fetching student by ID)
                            // This depends on how your Attendance model is populated
                            try {
                                Student student = attendanceController.getStudentById(a.getStudentId()); // Requires getStudentId() on Attendance and getStudentById(String) on Controller
                                a.setStudent(student); // Requires setStudent() on Attendance
                            } catch (SQLException e) {
                                System.err.println("Failed to get student for attendance " + a.getId() + ": " + e.getMessage());
                            }
                        }
                        if (a.getSession() == null) {
                            // Try to set session if not already set
                            try {
                                ClassSession sessionObj = attendanceController.getClassSessionById(a.getSessionId()); // Requires getSessionId() on Attendance and getClassSessionById(String) on Controller
                                a.setSession(sessionObj); // Requires setSession() on Attendance
                            } catch (SQLException e) {
                                System.err.println("Failed to get session for attendance " + a.getId() + ": " + e.getMessage());
                            }
                        }
                    });


                    allAbsences.addAll(absences);
                }
            }
        } catch (SQLException e) {
            showError("Không thể tải dữ liệu vắng học: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            absenceData = FXCollections.observableArrayList(); // Initialize empty list on error
            filteredData = new FilteredList<>(absenceData);
            absenceTable.setItems(filteredData);
            updateProgressBar();
            applyFilters();
            return;
        } catch (Exception e) {
            showError("Lỗi không xác định khi tải dữ liệu vắng học: " + e.getMessage());
            e.printStackTrace();
            absenceData = FXCollections.observableArrayList(); // Initialize empty list on error
            filteredData = new FilteredList<>(absenceData);
            absenceTable.setItems(filteredData);
            updateProgressBar();
            applyFilters();
            return;
        }


        absenceData = FXCollections.observableArrayList(allAbsences);
        filteredData = new FilteredList<>(absenceData);
        absenceTable.setItems(filteredData);
        // Apply filters after loading initial data
        applyFilters();
    }


    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(attendance -> {
            // Ensure attendance, student, and session objects are not null before accessing properties
            if (attendance == null) return false;
            Student student = attendance.getStudent();
            ClassSession session = attendance.getSession();

            boolean matchesDayFilter = true;
            boolean matchesCallStatusFilter = true;
            boolean matchesSearchText = true;

            // Day filter
            if (!"Tất cả".equals(currentDayFilter)) {
                if (session != null && session.getDate() != null) {
                    int dayOfWeek = session.getDate().getDayOfWeek().getValue();
                    String dayName = getDayNameFromDayOfWeek(dayOfWeek);
                    matchesDayFilter = currentDayFilter.equals(dayName);
                } else {
                    matchesDayFilter = false; // Session or date is null, doesn't match specific day
                }
            }

            // Call status filter
            if ("Đã gọi".equals(currentCallStatusFilter)) {
                matchesCallStatusFilter = attendance.isCalled(); // Requires isCalled()
            } else if ("Chưa gọi".equals(currentCallStatusFilter)) {
                matchesCallStatusFilter = !attendance.isCalled(); // Requires isCalled()
            }

            // Search text filter
            if (!currentSearchText.isEmpty()) {
                boolean nameMatches = student != null &&
                        student.getName() != null &&
                        student.getName().toLowerCase().contains(currentSearchText);

                boolean parentMatches = student != null &&
                        student.getParent() != null &&
                        student.getParent().getName() != null && // Requires getParent().getName()
                        student.getParent().getName().toLowerCase().contains(currentSearchText);

                boolean contactMatches = student != null &&
                        student.getContactNumber() != null && // Requires getContactNumber()
                        student.getContactNumber().toLowerCase().contains(currentSearchText);


                boolean classMatches = session != null &&
                        session.getClassName() != null && // Requires getClassName()
                        session.getClassName().toLowerCase().contains(currentSearchText);

                // Note: The controller's searchSessions method was designed to search the main session list,
                // not necessarily filter a list of attendance records by session properties.
                // For this view, direct matching on student/session properties is more appropriate.
                // Removing the call to attendanceController.searchSessions here.
                // If complex search on sessions is needed, the controller might need a different method.


                matchesSearchText = nameMatches || parentMatches || contactMatches || classMatches;
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
        int called = (int) filteredData.stream().filter(Attendance::isCalled).count(); // Requires isCalled()

        double progress = total > 0 ? (double) called / total : 0;
        int percentage = (int) (progress * 100);

        callProgressBar.setProgress(progress);
        callProgressLabel.setText(called + "/" + total + " (" + percentage + "%)");
        totalAbsencesLabel.setText("Tổng số vắng: " + total);
    }

    public void handleExportToExcel() {
        if (attendanceController != null && filteredData != null) {
            List<Attendance> dataToExport = new ArrayList<>(filteredData);
            // Include the selected date in the filename
            String filename = "danh_sach_vang_" + selectedDate.format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";

            try {
                // Ensure the controller has an export method that accepts a List<Attendance>
                // and uses the provided filename.
                // For example: attendanceController.exportAbsencesToExcel(dataToExport, filename);
                showError("Chức năng xuất Excel chưa được triển khai hoàn toàn trong bộ điều khiển.");
                // If the controller method existed and returned boolean:
                // boolean success = attendanceController.exportAbsencesToExcel(dataToExport, filename);
                // if (success) {
                //     showInfo("Xuất Excel thành công: " + filename);
                // } else {
                //     showError("Không thể xuất dữ liệu Excel.");
                // }
            } catch (Exception e) {
                showError("Lỗi khi xuất Excel: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showError("Không thể xuất dữ liệu. Bộ điều khiển hoặc dữ liệu không khả dụng.");
        }
    }

    public void setupActionHandlers() {
        exportExcelButton.setOnAction(e -> handleExportToExcel());

        // Removed the searchField.setOnAction as it's handled in createFilterSection
        // The searchButton handler is sufficient.
    }

    @Override
    public Object handleAction(String actionId, Object params) {
        switch (actionId) {
            case "exportExcel":
                handleExportToExcel();
                return true;
            case "refresh":
                loadAbsenceData(); // Reload data explicitly
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
        // This method could be used to react to changes from other parts of the application
        // For example, if attendance is updated elsewhere, a system message could trigger a refresh.
        if ("attendance_data_changed".equals(message)) {
            System.out.println("Received system message: attendance_data_changed. Reloading data.");
            loadAbsenceData(); // Reload data when notified of changes
        }
        // Add other relevant system messages here
    }

    private ImageView createButtonIcon(String iconName, String color) {
        // Placeholder for icon creation - you need actual icon loading logic here
        // Example using Rectangle as a placeholder:
        Rectangle rect = new Rectangle(16, 16);
        // You would map iconName to a specific shape or graphic
        // For now, just color the rectangle
        try {
            rect.setFill(Color.web(color));
        } catch (IllegalArgumentException e) {
            rect.setFill(Color.BLACK); // Default color if color string is invalid
        }


        ImageView imageView = new ImageView();
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);
        imageView.setPickOnBounds(true); // Allows clicking the transparent parts

        // In a real app, load image:
        // try {
        //     Image iconImage = new Image("/icons/" + iconName + "_" + color + ".png"); // Assuming icons are named like this
        //     imageView.setImage(iconImage);
        // } catch (IllegalArgumentException e) {
        //     System.err.println("Could not load icon: /icons/" + iconName + "_" + color + ".png");
        //     // Fallback to a default graphic or leave it empty
        //     imageView.setImage(null);
        // }

        // Returning the placeholder rectangle for now
        // If you want ImageView, you'd need to load an actual image into it.
        return imageView; // This will return an empty ImageView if image loading fails
        // return new ImageView(new Image(getClass().getResourceAsStream("/icons/" + iconName + ".png"))); // Example loading from resources
    }

    /**
     * Shows a simple success dialog.
     */
    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a simple error dialog.
     */
    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onActivate() {
        super.onActivate();
        // Load data when the screen is activated
        loadAbsenceData();
    }

    @Override
    public void refreshView() {
        // This method might be called to refresh the view without explicitly navigating
        loadAbsenceData();
    }

}
