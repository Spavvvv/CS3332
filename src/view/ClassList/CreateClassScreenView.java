package src.view.ClassList;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import src.dao.Classrooms.ClassroomDAO;
import src.dao.Notifications.RoomConflictException;
import src.dao.Person.CourseDAO;
import src.dao.Person.TeacherDAO; // Assuming you might want to select a teacher
import src.model.classroom.Classroom;
import src.model.holidays.HolidaysModel; // Import HolidaysModel
import src.model.person.Teacher; // Assuming you have a Teacher model
import src.model.system.course.Course;
import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class CreateClassScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String WHITE_COLOR = "#FFFFFF";
    private static final String TEXT_COLOR = "#424242";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    // UI Components
    private Stage stage;
    private TextField courseIdField;
    private TextField courseNameField;
    private TextField subjectField;
    private Spinner<Integer> totalSessionsSpinner; // For total learning sessions
    private DatePicker startDatePicker;
    private TextField calculatedEndDateField; // To display calculated end date (read-only)
    private HBox daysOfWeekCheckBoxesContainer;
    private List<CheckBox> dayCheckBoxesList;
    private ComboBox<Classroom> roomComboBox;
    private ComboBox<Teacher> teacherComboBox; // To select a teacher
    private ComboBox<Integer> startHourPicker;
    private ComboBox<Integer> startMinutePicker;
    private ComboBox<Integer> endHourPicker;
    private ComboBox<Integer> endMinutePicker;
    private Button saveButton;

    // DAOs and Models
    private CourseDAO courseDAO;
    private ClassroomDAO classroomDAO;
    private TeacherDAO teacherDAO; // For populating teacher ComboBox
    private HolidaysModel holidaysModel; // For checking holidays
    private CreateClassCallback callback;

    public interface CreateClassCallback {
        void onCourseCreated(Course course);
    }

    public CreateClassScreenView(CourseDAO courseDAO, ClassroomDAO classroomDAO, TeacherDAO teacherDAO,
                                 HolidaysModel holidaysModel, CreateClassCallback callback) {
        this.courseDAO = courseDAO;
        this.classroomDAO = classroomDAO;
        this.teacherDAO = teacherDAO; // Initialize TeacherDAO
        this.holidaysModel = holidaysModel; // Initialize HolidaysModel
        this.callback = callback;
        initialize();
        setupListeners(); // Call setupListeners after UI is initialized
        // Initial calculation if start date is already set (e.g. to LocalDate.now())
        Platform.runLater(this::calculateAndDisplayEndDate);
    }

    private void initialize() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Tạo Khóa học Mới"); // Updated title
        stage.initStyle(StageStyle.DECORATED);
        stage.setMinWidth(700); // Adjusted width
        stage.setMinHeight(700); // Adjusted height

        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");

        createHeader(root);
        createForm(root);
        createButtons(root);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void createHeader(VBox root) {
        Label titleLabel = new Label("Tạo Khóa học Mới");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 26));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));
        titleLabel.setAlignment(Pos.CENTER);
        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER);


        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 15, 0));

        root.getChildren().addAll(titleBox, separator);
    }

    private void createForm(VBox root) {
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(12);
        // formGrid.setAlignment(Pos.CENTER); // Removed for more natural left alignment

        int row = 0;

        // Course ID, Course Name
        formGrid.add(createStyledLabel("Mã Khóa học:"), 0, row);
        courseIdField = new TextField(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        courseIdField.setPromptText("Ví dụ: JAVA001");
        setFieldStyle(courseIdField);
        formGrid.add(courseIdField, 1, row);

        formGrid.add(createStyledLabel("Tên Khóa học:"), 2, row);
        courseNameField = new TextField();
        courseNameField.setPromptText("Ví dụ: Lập Trình Java Cơ Bản");
        setFieldStyle(courseNameField);
        formGrid.add(courseNameField, 3, row);
        row++;

        // Subject, Total Sessions
        formGrid.add(createStyledLabel("Môn học:"), 0, row);
        subjectField = new TextField();
        subjectField.setPromptText("Ví dụ: Lập trình Java");
        setFieldStyle(subjectField);
        formGrid.add(subjectField, 1, row);

        formGrid.add(createStyledLabel("Tổng số buổi học:"), 2, row);
        totalSessionsSpinner = new Spinner<>(1, 200, 20); // Min 1, Max 200, Initial 20
        totalSessionsSpinner.setEditable(true);
        setFieldStyle(totalSessionsSpinner);
        formGrid.add(totalSessionsSpinner, 3, row);
        row++;

        // Start Date, Calculated End Date
        formGrid.add(createStyledLabel("Ngày bắt đầu:"), 0, row);
        startDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setPromptText("Chọn ngày bắt đầu");
        setFieldStyle(startDatePicker);
        formGrid.add(startDatePicker, 1, row);

        formGrid.add(createStyledLabel("Ngày kết thúc (dự kiến):"), 2, row);
        calculatedEndDateField = new TextField();
        calculatedEndDateField.setEditable(false);
        calculatedEndDateField.setPromptText("Sẽ được tính tự động");
        setFieldStyle(calculatedEndDateField);
        formGrid.add(calculatedEndDateField, 3, row);
        row++;

        // Days of Week CheckBoxes
        formGrid.add(createStyledLabel("Ngày học trong tuần:"), 0, row, 1, 1); // Label takes 1 column
        daysOfWeekCheckBoxesContainer = new HBox(8); // Spacing between checkboxes
        daysOfWeekCheckBoxesContainer.setAlignment(Pos.CENTER_LEFT);
        dayCheckBoxesList = new ArrayList<>();
        String[] vietnameseDays = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        DayOfWeek[] dayOfWeekEnums = DayOfWeek.values(); // MONDAY to SUNDAY

        for (int i = 0; i < dayOfWeekEnums.length; i++) {
            CheckBox cb = new CheckBox(vietnameseDays[i]);
            cb.setUserData(dayOfWeekEnums[i]); // Store DayOfWeek enum (MONDAY to SUNDAY)
            dayCheckBoxesList.add(cb);
            daysOfWeekCheckBoxesContainer.getChildren().add(cb);
        }
        formGrid.add(daysOfWeekCheckBoxesContainer, 1, row, 3, 1); // Checkboxes span 3 columns
        row++;


        // Start Time, End Time
        formGrid.add(createStyledLabel("Giờ bắt đầu (HH:mm):"), 0, row);
        startHourPicker = createHourPicker();
        startMinutePicker = createMinutePicker();
        HBox startTimeBox = new HBox(5, startHourPicker, new Label(":"), startMinutePicker);
        startTimeBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(startTimeBox, 1, row);

        formGrid.add(createStyledLabel("Giờ kết thúc (HH:mm):"), 2, row);
        endHourPicker = createHourPicker();
        endMinutePicker = createMinutePicker();
        HBox endTimeBox = new HBox(5, endHourPicker, new Label(":"), endMinutePicker);
        endTimeBox.setAlignment(Pos.CENTER_LEFT);
        formGrid.add(endTimeBox, 3, row);
        row++;

        // Room, Teacher
        formGrid.add(createStyledLabel("Phòng học:"), 0, row);
        roomComboBox = new ComboBox<>();
        roomComboBox.setPromptText("Chọn phòng học");
        if (this.classroomDAO != null) {
            try {
                roomComboBox.setItems(FXCollections.observableArrayList(this.classroomDAO.findAll()));
            } catch (Exception e) {
                System.err.println("Lỗi tải danh sách phòng học: " + e.getMessage());
            }
        }
        roomComboBox.setConverter(new ClassroomStringConverter());
        setFieldStyle(roomComboBox);
        formGrid.add(roomComboBox, 1, row);

        formGrid.add(createStyledLabel("Giáo viên:"), 2, row);
        teacherComboBox = new ComboBox<>();
        teacherComboBox.setPromptText("Chọn giáo viên");
        if (this.teacherDAO != null) {
            try {
                teacherComboBox.setItems(FXCollections.observableArrayList(this.teacherDAO.findAll()));
            } catch (Exception e) {
                System.err.println("Lỗi tải danh sách giáo viên: " + e.getMessage());
            }
        }
        teacherComboBox.setConverter(new TeacherStringConverter());
        setFieldStyle(teacherComboBox);
        formGrid.add(teacherComboBox, 3, row);
        row++;

        // You can add Class ID field here if needed, similar to other fields
        // formGrid.add(createStyledLabel("Mã Nhóm SV (Class ID):"), 0, row);
        // classIdField = new TextField(); setFieldStyle(classIdField);
        // formGrid.add(classIdField, 1, row);

        root.getChildren().add(formGrid);
    }
    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.NORMAL, 13));
        label.setTextFill(Color.web(TEXT_COLOR));
        return label;
    }

    private ComboBox<Integer> createHourPicker() {
        ComboBox<Integer> picker = new ComboBox<>();
        for (int i = 0; i <= 23; i++) picker.getItems().add(i);
        picker.setPromptText("Giờ");
        setFieldStyle(picker);
        return picker;
    }

    private ComboBox<Integer> createMinutePicker() {
        ComboBox<Integer> picker = new ComboBox<>();
        for (int i = 0; i <= 59; i += 5) picker.getItems().add(i); // Steps of 5 minutes
        picker.setPromptText("Phút");
        setFieldStyle(picker);
        return picker;
    }

    private void setupListeners() {
        startDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                DayOfWeek startDayEnum = newDate.getDayOfWeek();
                // Auto-check the corresponding day of the week checkbox
                for (CheckBox cb : dayCheckBoxesList) {
                    if (cb.getUserData().equals(startDayEnum)) {
                        // cb.setSelected(true); // Optional: auto-select the start day's checkbox
                        // It might be better to just inform the user or let them select all days manually
                    } else {
                        // cb.setSelected(false); // If you want only the start day to be initially selected
                    }
                }
            }
            calculateAndDisplayEndDate();
        });

        totalSessionsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> calculateAndDisplayEndDate());

        for (CheckBox cb : dayCheckBoxesList) {
            cb.selectedProperty().addListener((obs, oldSelected, newSelected) -> calculateAndDisplayEndDate());
        }
        // Add listeners for time pickers if they should also trigger end date or other calculations
        // For now, they don't directly affect the end date calculation based on sessions.
    }


    private void calculateAndDisplayEndDate() {
        LocalDate startDate = startDatePicker.getValue();
        Integer totalRequiredSessions = totalSessionsSpinner.getValue();
        List<DayOfWeek> selectedWeekdays = dayCheckBoxesList.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (DayOfWeek) cb.getUserData())
                .collect(Collectors.toList());

        if (startDate == null || totalRequiredSessions == null || totalRequiredSessions <= 0 || selectedWeekdays.isEmpty()) {
            calculatedEndDateField.setText("Chọn đủ thông tin");
            return;
        }

        LocalDate currentDate = startDate;
        int sessionsCounted = 0;
        LocalDate DUMMY_MAX_END_DATE = startDate.plusYears(5); // Safety break

        while (sessionsCounted < totalRequiredSessions) {
            if (currentDate.isAfter(DUMMY_MAX_END_DATE)) {
                calculatedEndDateField.setText("Lỗi: Vượt quá giới hạn tính");
                return;
            }

            // Check if current date is a holiday (using HolidaysModel)
            if (holidaysModel != null && holidaysModel.isHoliday(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue; // Skip to next day if it's a holiday
            }

            // Check if current date is one of the selected weekdays
            if (selectedWeekdays.contains(currentDate.getDayOfWeek())) {
                sessionsCounted++;
            }

            // If all sessions are counted, current date is the end date
            if (sessionsCounted == totalRequiredSessions) {
                calculatedEndDateField.setText(currentDate.format(DATE_FORMATTER));
                return;
            }
            currentDate = currentDate.plusDays(1);
        }
        // Fallback if loop finishes without meeting conditions (should ideally not happen with DUMMY_MAX_END_DATE)
        calculatedEndDateField.setText("Không thể tính");
    }


    private void setFieldStyle(Control field) { // General Control for DatePicker, ComboBox, Spinner
        if (field instanceof ComboBox || field instanceof DatePicker || field instanceof Spinner) {
            field.setPrefWidth(160); // Slightly less width for these controls in a 2-column setup
        } else if (field instanceof TextField) {
            ((TextField) field).setPrefWidth(160);
        }
        field.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4px;" +
                        "-fx-padding: 7px;" // Consistent padding
        );
        if (field instanceof Spinner) { // Spinner has complex internal structure
            ((Spinner<?>) field).getEditor().setStyle("-fx-background-color: transparent; -fx-padding: 1px 3px;");
        }
    }


    private void createButtons(VBox root) {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(20, 0, 0, 0)); // Add some top margin

        Button cancelButton = new Button("Hủy");
        cancelButton.setStyle(
                "-fx-background-color: #D1D5DB;" + // Light gray
                        "-fx-text-fill: #374151;" + // Darker gray text
                        "-fx-border-color: #9CA3AF;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-padding: 8px 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> stage.close());

        saveButton = new Button("Lưu Khóa học");
        saveButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-padding: 8px 20px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 1, 1);"
        );
        saveButton.setOnAction(e -> createCourseAndNotify());

        buttonBox.getChildren().addAll(cancelButton, saveButton);
        root.getChildren().add(buttonBox);
    }

    private void createCourseAndNotify() {
        if (!validateInputs()) {
            return; // Validation failed, messages shown by validateInputs
        }

        try {
            String courseId = courseIdField.getText().trim();
            String courseName = courseNameField.getText().trim();
            String subject = subjectField.getText().trim();
            LocalDate startDate = startDatePicker.getValue();

            LocalDate endDate;
            try {
                endDate = LocalDate.parse(calculatedEndDateField.getText().trim(), DATE_FORMATTER);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,"Lỗi Định Dạng", "Ngày kết thúc không hợp lệ hoặc chưa được tính.");
                return;
            }

            LocalTime startTime = LocalTime.of(startHourPicker.getValue(), startMinutePicker.getValue());
            LocalTime endTime = LocalTime.of(endHourPicker.getValue(), endMinutePicker.getValue());

            List<String> selectedDays = dayCheckBoxesList.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> ((DayOfWeek) cb.getUserData()).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH)) // "Mon", "Tue"
                    .collect(Collectors.toList());

            Classroom selectedRoom = roomComboBox.getSelectionModel().getSelectedItem();
            String roomId = selectedRoom != null ? selectedRoom.getRoomId() : null;

            Teacher selectedTeacher = teacherComboBox.getSelectionModel().getSelectedItem();
            // String teacherId = selectedTeacher != null ? selectedTeacher.getId() : null; // Not directly on Course model constructor like this

            int totalSessions = totalSessionsSpinner.getValue();

            // Using the comprehensive constructor of Course (ensure it matches your latest Course model)
            Course newCourse = new Course(
                    courseId, courseName, subject, startDate, endDate,
                    startTime, endTime, selectedDays, roomId,
                    null, // classId - you might want a field for this if still used in courses table
                    selectedTeacher,
                    totalSessions,
                    0.0f // Initial progress
            );
            // If your Course constructor doesn't take Teacher object directly,
            // you might need to set teacher ID if your Course model stores teacher_id as String
            // newCourse.setTeacherId(teacherId); // If Course model stores teacherId String
            // Or ensure your CourseDAO.save can handle the Teacher object within the Course object.


            boolean isSaved = courseDAO.save(newCourse);

            if (isSaved) {
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã tạo khóa học thành công!");
                if (callback != null) {
                    callback.onCourseCreated(newCourse);
                }
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR,"Lỗi Lưu", "Không thể lưu khóa học. Lỗi không xác định từ DAO.");
            }
        } catch (RoomConflictException rce) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Xung Đột Lịch", rce.getMessage());
        } catch (SQLException sqle) {
            showAlert(Alert.AlertType.ERROR,"Lỗi Cơ sở dữ liệu", "Lỗi khi lưu khóa học: " + sqle.getMessage());
            sqle.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR,"Lỗi Không Mong Muốn", "Đã xảy ra lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        StringBuilder errorMessages = new StringBuilder();

        if (courseIdField.getText().trim().isEmpty()) errorMessages.append("- Mã khóa học không được để trống.\n");
        else if (courseDAO.findById(courseIdField.getText().trim()).isPresent()) {
            errorMessages.append("- Mã khóa học '").append(courseIdField.getText().trim()).append("' đã tồn tại.\n");
        }
        if (courseNameField.getText().trim().isEmpty()) errorMessages.append("- Tên khóa học không được để trống.\n");
        if (subjectField.getText().trim().isEmpty()) errorMessages.append("- Môn học không được để trống.\n");
        if (totalSessionsSpinner.getValue() == null || totalSessionsSpinner.getValue() <= 0) errorMessages.append("- Tổng số buổi học phải lớn hơn 0.\n");
        if (startDatePicker.getValue() == null) errorMessages.append("- Ngày bắt đầu không được để trống.\n");

        List<DayOfWeek> selectedWeekdays = dayCheckBoxesList.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (DayOfWeek) cb.getUserData())
                .collect(Collectors.toList());
        if (selectedWeekdays.isEmpty()) errorMessages.append("- Phải chọn ít nhất một ngày học trong tuần.\n");

        if (startHourPicker.getValue() == null || startMinutePicker.getValue() == null) errorMessages.append("- Vui lòng chọn đầy đủ giờ và phút bắt đầu.\n");
        if (endHourPicker.getValue() == null || endMinutePicker.getValue() == null) errorMessages.append("- Vui lòng chọn đầy đủ giờ và phút kết thúc.\n");

        LocalTime startTime = null;
        LocalTime endTime = null;
        if (startHourPicker.getValue() != null && startMinutePicker.getValue() != null) {
            startTime = LocalTime.of(startHourPicker.getValue(), startMinutePicker.getValue());
        }
        if (endHourPicker.getValue() != null && endMinutePicker.getValue() != null) {
            endTime = LocalTime.of(endHourPicker.getValue(), endMinutePicker.getValue());
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            errorMessages.append("- Giờ kết thúc phải sau giờ bắt đầu.\n");
        }

        if (roomComboBox.getSelectionModel().getSelectedItem() == null) errorMessages.append("- Vui lòng chọn phòng học.\n");
        if (teacherComboBox.getSelectionModel().getSelectedItem() == null) errorMessages.append("- Vui lòng chọn giáo viên.\n");

        // Validate calculated end date
        if (calculatedEndDateField.getText().trim().isEmpty() ||
                calculatedEndDateField.getText().startsWith("Chọn") ||
                calculatedEndDateField.getText().startsWith("Không thể") ||
                calculatedEndDateField.getText().startsWith("Lỗi")) {
            errorMessages.append("- Không thể xác định ngày kết thúc. Kiểm tra lại thông tin lịch trình.\n");
        }


        if (errorMessages.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", errorMessages.toString());
            return false;
        }

        // Client-side conflict check (can be enhanced, DAO will do final check)
        // This part needs to iterate through selected days and check conflict for each.
        // For simplicity here, we rely on DAO's more robust check during save.
        // However, a basic client-side check can improve UX.
        Classroom selectedRoom = roomComboBox.getSelectionModel().getSelectedItem();
        if (selectedRoom != null && startTime != null && endTime != null && !selectedWeekdays.isEmpty()) {
            boolean conflictFoundOnClient = false;
            for (DayOfWeek day : selectedWeekdays) {
                // The CourseDAO.hasTimeConflictForRoom (or the new hasTimeConflictForRoomAndDay)
                // expects a numeric day. We need to convert 'day' (DayOfWeek enum) to numeric.
                // Assuming 1=Monday, ..., 7=Sunday (DayOfWeek.getValue())
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    if (courseDAO.hasTimeConflictForRoomAndDay(conn, selectedRoom.getRoomId(), day.getValue(), startTime, endTime, null)) {
                        errorMessages.append("- Xung đột lịch cho phòng ").append(selectedRoom.getRoomName())
                                .append(" vào ").append(day.getDisplayName(TextStyle.FULL, new Locale("vi")))
                                .append(" từ ").append(startTime.format(TIME_FORMATTER))
                                .append(" đến ").append(endTime.format(TIME_FORMATTER)).append(".\n");
                        conflictFoundOnClient = true;
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi client-side conflict check: " + e.getMessage());
                    //  errorMessages.append("- Lỗi kiểm tra xung đột lịch phía client.\n");
                    //  conflictFoundOnClient = true; // Potentially treat as conflict
                }
            }
            if(conflictFoundOnClient){
                showAlert(Alert.AlertType.WARNING, "Cảnh báo Xung đột Lịch", errorMessages.toString() + "Lưu ý: Đây là kiểm tra sơ bộ, hệ thống sẽ kiểm tra lại khi lưu.");
                // return false; // You might choose to stop the user here or let the DAO handle the final conflict check
            }
        }


        return true; // Assuming all checks pass
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage); // Ensure alert is owned by the dialog stage
        alert.showAndWait();
    }

    public void show() {
        stage.showAndWait();
    }

    // Helper StringConverter classes for ComboBoxes
    private static class ClassroomStringConverter extends StringConverter<Classroom> {
        @Override
        public String toString(Classroom room) {
            return room == null ? null : room.getRoomName() + " (ID: " + room.getRoomId() + ")";
        }
        @Override
        public Classroom fromString(String string) { return null; } // Not needed for non-editable ComboBox
    }

    private static class TeacherStringConverter extends StringConverter<Teacher> {
        @Override
        public String toString(Teacher teacher) {
            return teacher == null ? null : teacher.getName() + " (ID: " + teacher.getId() + ")";
        }
        @Override
        public Teacher fromString(String string) { return null; } // Not needed for non-editable ComboBox
    }
}