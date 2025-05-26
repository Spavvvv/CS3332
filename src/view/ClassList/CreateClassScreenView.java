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
import src.controller.Course.CourseController;
import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Classrooms.ClassroomDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Notifications.RoomConflictException;
import src.dao.Person.CourseDAO;
import src.dao.Person.TeacherDAO; // Assuming you might want to select a teacher
import src.model.classroom.Classroom;
import src.model.holidays.HolidaysModel; // Import HolidaysModel
import src.model.person.Teacher; // Assuming you have a Teacher model
import src.model.system.course.Course;
import src.utils.DaoManager;
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
    private CourseController courseController;
    // DAOs and Models
    private CourseDAO courseDAO;
    private ClassroomDAO classroomDAO;
    private TeacherDAO teacherDAO; // For populating teacher ComboBox
    private HolidaysModel holidaysModel; // For checking holidays
    private CreateClassCallback callback;
    private ClassSessionDAO classSessionDAO;
    private HolidayDAO holidayDAO;

    public interface CreateClassCallback {
        void onCourseCreated(Course course);
    }

    public CreateClassScreenView(CourseDAO courseDAO, ClassroomDAO classroomDAO, TeacherDAO teacherDAO,
                                 HolidaysModel holidaysModel, CreateClassCallback callback) { // Giữ nguyên các tham số này

        // Gán các DAO được truyền vào
        this.courseDAO = courseDAO;
        this.classroomDAO = classroomDAO;
        this.teacherDAO = teacherDAO;
        this.holidaysModel = holidaysModel; // holidaysModel có thể được dùng để tạo holidayDAO
        this.callback = callback;           // Vẫn giữ callback nếu bạn có thể muốn dùng cho mục đích khác
        // hoặc để tương thích ngược, nhưng logic tạo session sẽ qua Controller.
        this.holidayDAO = DaoManager.getInstance().getHolidayDAO();

        // Khởi tạo các DAO mà CourseController cần nhưng không được truyền trực tiếp vào View
        this.classSessionDAO = new ClassSessionDAO(); // Tạo mới

        // Xử lý HolidayDAO:
        // Nếu HolidaysModel chính là HolidayDAO hoặc có thể cung cấp HolidayDAO
        this.holidayDAO = new HolidayDAO(); // Giả sử có constructor không tham số cho HolidayDAO
        // Đảm bảo CourseDAO (mà CourseController sẽ dùng) có các dependency cần thiết
        // Ví dụ, TeacherDAO đã được truyền vào CreateClassScreenView
        if (this.courseDAO != null && this.teacherDAO != null) {
            this.courseDAO.setTeacherDAO(this.teacherDAO); // Thiết lập TeacherDAO cho CourseDAO
            // this.courseDAO.setStudentDAO(new StudentDAO()); // Nếu CourseDAO cần StudentDAO
        }

        // Khởi tạo CourseController với tất cả các DAO cần thiết
        this.courseController = new CourseController(
                this.courseDAO,
                this.classSessionDAO,
                this.classroomDAO,
                this.teacherDAO,
                this.holidayDAO
        );

        initialize();
        setupListeners();
        Platform.runLater(this::calculateAndDisplayEndDate);
    }
    public void setClassSessionDAO(ClassSessionDAO classSessionDAO) { this.classSessionDAO = classSessionDAO; }
    public void setHolidayDAO(HolidayDAO holidayDAO) { this.holidayDAO = holidayDAO; }


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
        LocalDate DUMMY_MAX_END_DATE = startDate.plusYears(5);

        while (sessionsCounted < totalRequiredSessions) {
            if (currentDate.isAfter(DUMMY_MAX_END_DATE)) {
                calculatedEndDateField.setText("Lỗi: Vượt quá giới hạn tính");
                return;
            }

            boolean isCurrentDateHoliday = false;
            // Ưu tiên sử dụng holidaysModel được truyền vào
            if (this.holidaysModel != null) {
                // Giả định HolidaysModel có phương thức isHoliday() hoặc getHolidayForDate()
                // Nếu HolidaysModel có isHoliday():
                // isCurrentDateHoliday = this.holidaysModel.isHoliday(currentDate);

                // Nếu HolidaysModel chỉ có getHolidayForDate() (trả về Holiday object hoặc null):
                if (this.holidaysModel.getHolidayForDate(currentDate) != null) { // Lấy từ file HolidaysController.java
                    isCurrentDateHoliday = true;
                }
            }
            // Nếu không có holidaysModel hoặc nó không xác định được, có thể dùng holidayDAOInternal làm fallback (tùy chọn)
            // else if (this.holidayDAOInternal != null) {
            //     isCurrentDateHoliday = this.holidayDAOInternal.isHoliday(currentDate);
            // }


            if (isCurrentDateHoliday) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            if (selectedWeekdays.contains(currentDate.getDayOfWeek())) {
                sessionsCounted++;
            }

            if (sessionsCounted == totalRequiredSessions) {
                calculatedEndDateField.setText(currentDate.format(DATE_FORMATTER));
                return;
            }
            currentDate = currentDate.plusDays(1);
        }
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
            return;
        }

        // 1. Thu thập dữ liệu và tạo đối tượng Course (logic này giữ nguyên)
        String courseId = courseIdField.getText().trim();
        String courseName = courseNameField.getText().trim();
        String subject = subjectField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate;
        try {
            endDate = LocalDate.parse(calculatedEndDateField.getText().trim(), DATE_FORMATTER);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Định Dạng", "Ngày kết thúc không hợp lệ hoặc chưa được tính.");
            return;
        }
        LocalTime startTime = LocalTime.of(startHourPicker.getValue(), startMinutePicker.getValue());
        LocalTime endTimeValue = LocalTime.of(endHourPicker.getValue(), endMinutePicker.getValue());

        List<String> selectedDays = dayCheckBoxesList.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((DayOfWeek) cb.getUserData()).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH).toUpperCase())
                .collect(Collectors.toList());

        Classroom selectedRoom = roomComboBox.getSelectionModel().getSelectedItem();
        String roomId = (selectedRoom != null) ? selectedRoom.getRoomId() : null;
        Teacher selectedTeacher = teacherComboBox.getSelectionModel().getSelectedItem();
        int totalSessions = totalSessionsSpinner.getValue();

        Course newCourse = new Course(
                courseId, courseName, subject, startDate, endDate,
                startTime, endTimeValue, selectedDays, roomId,
                selectedTeacher,
                totalSessions,
                0.0f // progress ban đầu
        );

        // 2. Gọi phương thức của CourseController để xử lý toàn bộ nghiệp vụ
        try {
            // this.courseController phải được khởi tạo trước đó (thường trong constructor của CreateClassScreenView)
            if (this.courseController == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi Cấu Hình", "CourseController chưa được khởi tạo.");
                return;
            }

            boolean success = this.courseController.createCourseAndGenerateSessions(newCourse);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã tạo khóa học và lịch học chi tiết thành công!");
                // Nếu bạn vẫn dùng callback cho mục đích khác (ví dụ: thông báo cho một view khác để refresh)
                if (this.callback != null) {
                    // Lưu ý: newCourse ở đây là đối tượng từ UI. Nếu callback cần Course object
                    // đã được cập nhật ID từ DB (trong trường hợp DB tự tạo ID), thì
                    // CourseController.createCourseAndGenerateSessions() cần phải trả về
                    // đối tượng Course đã được cập nhật đó.
                    this.callback.onCourseCreated(newCourse);
                }
                stage.close(); // Đóng cửa sổ sau khi thành công
            } else {
                // Trường hợp này ít khi xảy ra nếu CourseController ném Exception khi có lỗi nghiêm trọng,
                // mà thường là do một điều kiện logic trong Controller/Service trả về false.
                showAlert(Alert.AlertType.WARNING, "Thông Báo", "Không thể hoàn tất việc tạo khóa học. Vui lòng kiểm tra log hệ thống.");
            }
        } catch (RoomConflictException rce) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Xung Đột Lịch", rce.getMessage());
        } catch (SQLException sqle) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Cơ Sở Dữ Liệu", "Đã xảy ra lỗi khi xử lý yêu cầu: " + sqle.getMessage());
            sqle.printStackTrace(); // Quan trọng cho việc debug
        } catch (Exception e) { // Bắt các lỗi chung khác nếu có từ Controller
            showAlert(Alert.AlertType.ERROR, "Lỗi Không Mong Muốn", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
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