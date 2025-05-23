
package view.components.ClassList;

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
import src.dao.ClassSessionDAO;
import src.dao.ClassroomDAO;
import src.model.ClassSession;
import src.model.Notification.RoomConflictException;
import src.model.classroom.Classroom;
import src.model.system.course.Course;
import src.dao.CourseDAO; // Đảm bảo import này tồn tại và đúng
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CreateClassScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0";
    // private static final String GREEN_COLOR = "#4CAF50"; // Không được sử dụng, có thể bỏ
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String WHITE_COLOR = "#FFFFFF";
    private static final String TEXT_COLOR = "#424242";

    // UI Components
    private Stage stage;
    private Scene scene;
    private VBox root;
    private TextField courseIdField;
    private TextField courseNameField;
    private TextField subjectField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Button saveButton;
    private Button cancelButton;
    private ComboBox<Classroom> roomComboBox; // ***** DECLARE roomComboBox HERE *****
    private ComboBox<Integer> starthourPicker;
    private ComboBox<Integer> startminutePicker;
    private ComboBox<String> dayOfWeekComboBox;
    private ComboBox<Integer> endHourPicker;
    private ComboBox<Integer> endMinutePicker;


    // Thêm biến thành viên cho CourseDAO
    private ClassroomDAO classroomDAO;
    private CourseDAO courseDAO;
    private CreateClassCallback callback;

    // SỬA ĐỔI 1: Interface CreateClassCallback chỉ định nghĩa chữ ký
    public interface CreateClassCallback {
        void onCourseCreated(Course course); // Bỏ phần implementation mặc định ở đây
    }

    // SỬA ĐỔI 2: Constructor nhận CourseDAO
    public CreateClassScreenView(CourseDAO courseDAO, ClassroomDAO classroomDAO, CreateClassCallback callback) {
        this.courseDAO = courseDAO;
        this.classroomDAO = classroomDAO; // THÊM: Khởi tạo classroomDAO
        this.callback = callback;
        initialize();
    }

    private void initialize() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Tạo lớp học mới");
        stage.initStyle(StageStyle.DECORATED);
        stage.setMinWidth(600);
        stage.setMinHeight(500);

        root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");

        createHeader();
        createForm();
        createButtons();

        scene = new Scene(root);
        stage.setScene(scene);
    }

    private void createHeader() {
        Label titleLabel = new Label("Tạo lớp học mới");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        root.getChildren().addAll(titleLabel, separator);
    }

    private void createForm() {
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        Label courseIdLabel = new Label("Mã lớp:"); // Sẽ là course_id
        courseIdField = new TextField();
        courseIdField.setPromptText("Ví dụ: JAVA001"); // Ví dụ cho course_id
        setFieldStyle(courseIdField);

        Label courseNameLabel = new Label("Tên lớp:");
        courseNameField = new TextField();
        courseNameField.setPromptText("Ví dụ: Lớp Lập Trình Java Cơ Bản");
        setFieldStyle(courseNameField);

        Label subjectLabel = new Label("Môn học:");
        subjectField = new TextField();
        subjectField.setPromptText("Ví dụ: Lập trình Java");
        setFieldStyle(subjectField);

        Label startDateLabel = new Label("Ngày bắt đầu:");
        startDatePicker = new DatePicker(LocalDate.now());
        setFieldStyle(startDatePicker);

        Label endDateLabel = new Label("Ngày kết thúc:");
        endDatePicker = new DatePicker(LocalDate.now().plusMonths(3));
        setFieldStyle(endDatePicker);

        Label roomIdLabel = new Label("Phòng học:"); // Label cho ComboBox chọn phòng
        roomComboBox = new ComboBox<>(); // THÊM: Khởi tạo ComboBox
        roomComboBox.setPromptText("Chọn phòng học");

        // Lấy danh sách phòng từ ClassroomDAO
        List<Classroom> availableRooms = new ArrayList<>();
        if (this.classroomDAO != null) {
            availableRooms = this.classroomDAO.findAll(); // Sử dụng phương thức findAll() từ ClassroomDAO
        } else {
            System.err.println("Lỗi: ClassroomDAO chưa được khởi tạo trong CreateClassScreenView.");
            // Có thể thêm một item thông báo lỗi vào ComboBox hoặc vô hiệu hóa nó
        }
        roomComboBox.setItems(FXCollections.observableArrayList(availableRooms));

        // Cấu hình cách hiển thị đối tượng Classroom trong ComboBox
        roomComboBox.setConverter(new StringConverter<Classroom>() {
            @Override
            public String toString(Classroom room) {
                if (room == null) {
                    return null;
                }
                // Hiển thị tên phòng và mã phòng (hoặc chỉ tên phòng tùy ý)
                return room.getRoomName() + " (" + (room.getCode() != null ? room.getCode() : room.getRoomId()) + ")";
            }

            @Override
            public Classroom fromString(String string) {
                // Cần thiết nếu ComboBox cho phép chỉnh sửa trực tiếp.
                // Tìm Classroom dựa trên chuỗi hiển thị.
                return roomComboBox.getItems().stream()
                        .filter(r -> toString(r).equals(string))
                        .findFirst().orElse(null);
            }
        });
        setFieldStyle(roomComboBox); // THÊM: Áp dụng style cho ComboBox (cần tạo phương thức này)
        Label dayOfWeekLabel = new Label("Ngày trong tuần:");
        dayOfWeekComboBox = new ComboBox<>(); // Đảm bảo bạn đã khai báo ở class level
        dayOfWeekComboBox.setPromptText("Chọn ngày");
        dayOfWeekComboBox.setItems(FXCollections.observableArrayList(
                "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"
        ));
        setFieldStyle(dayOfWeekComboBox); // Sử dụng phương thức setFieldStyle hiện có của bạn
        // Label và các ComboBox cho Giờ bắt đầu
        Label startTimeLabel = new Label("Giờ bắt đầu:");
        starthourPicker = new ComboBox<>();
        starthourPicker.setPromptText("Giờ");
        for (int i = 0; i <= 23; i++) { // Giờ từ 0 đến 23
            starthourPicker.getItems().add(i);
        }
        setFieldStyle(starthourPicker); // Áp dụng style
        startminutePicker = new ComboBox<>();
        startminutePicker.setPromptText("Phút");
        for (int i = 0; i <= 59; i += 5) { // Phút, ví dụ cách nhau 5 phút, hoặc 0-59
            startminutePicker.getItems().add(i);
        }
        setFieldStyle(startminutePicker); // Áp dụng style
        // Gom hai ComboBox giờ và phút vào một HBox cho đẹp
        HBox timeSelectionBox = new HBox(10); // 10 là khoảng cách
        timeSelectionBox.setAlignment(Pos.CENTER_LEFT);
        timeSelectionBox.getChildren().addAll(starthourPicker, new Label(":"), startminutePicker);
        Label endTimeLabel = new Label("Giờ kết thúc:");
        endHourPicker = new ComboBox<>();
        endHourPicker.setPromptText("Giờ");
        for (int i = 0; i <= 23; i++) {
            endHourPicker.getItems().add(i);
        }
        setFieldStyle(endHourPicker); // Áp dụng style, nếu có
        endMinutePicker = new ComboBox<>();
        endMinutePicker.setPromptText("Phút");
        for (int i = 0; i <= 59; i += 5) { // 5 phút mỗi bước
            endMinutePicker.getItems().add(i);
        }
        setFieldStyle(endMinutePicker); // Áp dụng style, nếu có
// Gom các tùy chọn giờ và phút vào một HBox
        HBox endTimeSelectionBox = new HBox(10);
        endTimeSelectionBox.setAlignment(Pos.CENTER_LEFT);
        endTimeSelectionBox.getChildren().addAll(endHourPicker, new Label(":"), endMinutePicker);
        int row = 0;
        formGrid.add(courseIdLabel, 0, row);
        formGrid.add(courseIdField, 1, row++);

        formGrid.add(courseNameLabel, 0, row);
        formGrid.add(courseNameField, 1, row++);

        formGrid.add(subjectLabel, 0, row);
        formGrid.add(subjectField, 1, row++);

        formGrid.add(startDateLabel, 0, row);
        formGrid.add(startDatePicker, 1, row++);

        formGrid.add(endDateLabel, 0, row);
        formGrid.add(endDatePicker, 1, row++);

        formGrid.add(roomIdLabel, 0, row); // Label cho phòng học
        formGrid.add(roomComboBox, 1, row++); // THÊM: ComboBox vào grid
        formGrid.add(startTimeLabel, 0, row);
        formGrid.add(timeSelectionBox, 1, row++);
        formGrid.add(endTimeLabel, 0, row);
        formGrid.add(endTimeSelectionBox, 1, row++);
        formGrid.add(dayOfWeekLabel, 0, row); // Giả sử 'row' đang được quản lý đúng
        formGrid.add(dayOfWeekComboBox, 1, row++);

        root.getChildren().add(formGrid);
    }
    private void setFieldStyle(ComboBox<?> comboBox) { // Sử dụng wildcard (?) để tổng quát hơn
        comboBox.setPrefWidth(350);
        comboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        // Padding cho ComboBox có thể cần điều chỉnh một chút so với TextField
                        // để giao diện trông cân đối. '7' hoặc '8' thường là lựa chọn tốt.
                        "-fx-padding: 7;"
        );
    }


    private void setFieldStyle(TextField field) {
        field.setPrefWidth(350);
        field.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 8;"
        );
    }

    private void setFieldStyle(TextArea area) {
        area.setPrefWidth(350);
        area.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 8;"
        );
    }

    private void setFieldStyle(DatePicker datePicker) {
        datePicker.setPrefWidth(350);
        datePicker.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 8;"
        );
    }

    private void createButtons() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        cancelButton = new Button("Hủy");
        cancelButton.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-text-fill: " + TEXT_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> stage.close());

        saveButton = new Button("Lưu");
        saveButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );
        // SỬA ĐỔI 3: Logic lưu nằm trong setOnAction của saveButton, gọi createCourse()
        saveButton.setOnAction(e -> createCourseAndNotify()); // Đổi tên phương thức để rõ ràng hơn

        buttonBox.getChildren().addAll(cancelButton, saveButton);
        root.getChildren().add(buttonBox);
    }

    // SỬA ĐỔI 3 (tiếp theo): Phương thức createCourse() giờ sẽ lưu và gọi callback
    private void createCourseAndNotify() {
        if (validateInputs()) { // validateInputs() vẫn thực hiện kiểm tra xung đột ở client-side
            try {
                String courseId = courseIdField.getText().trim();
                String courseName = courseNameField.getText().trim();
                String subject = subjectField.getText().trim();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                int startHour = starthourPicker.getValue();
                int startMinute = startminutePicker.getValue();
                LocalTime startTime = LocalTime.of(startHour, startMinute);

                int endHour = endHourPicker.getValue();
                int endMinute = endMinutePicker.getValue();
                LocalTime endTime = LocalTime.of(endHour, endMinute);

                String dayOfWeek = dayOfWeekComboBox.getValue();

                Classroom selectedRoom = roomComboBox.getSelectionModel().getSelectedItem();
                String selectedRoomId = selectedRoom != null ? selectedRoom.getRoomId() : null;

                Course course = new Course(courseId, courseName, subject, startDate, endDate);
                course.setCourseSessionStartTime(startTime);
                course.setCourseSessionEndTime(endTime);
                course.setDayOfWeek(dayOfWeek);
                course.setRoomId(selectedRoomId);

                // Gọi courseDAO.save() có thể ném RoomConflictException hoặc SQLException
                boolean isSaved = courseDAO.save(course);

                if (isSaved) { // Chỉ đạt được nếu save() thành công và trả về true
                    showAlert("Lưu lớp học thành công!");
                    if (callback != null) {
                        callback.onCourseCreated(course);
                    }
                    stage.close();
                } else {
                    // Trường hợp này xảy ra nếu save() trả về false (ví dụ: internalInsert thất bại mà không ném lỗi)
                    // và không có Exception nào được ném.
                    showAlert("Không thể lưu lớp học. Đã có lỗi không xác định xảy ra trong quá trình lưu.");
                }
            } catch (RoomConflictException rce) {
                // Bắt lỗi xung đột lịch cụ thể được ném từ DAO
                showAlert(rce.getMessage()); // Hiển thị thông báo lỗi chi tiết từ exception
            } catch (SQLException sqle) {
                // Bắt lỗi SQL có thể được ném từ DAO (ví dụ: lỗi kết nối, lỗi câu lệnh SQL trong internalInsert)
                showAlert("Lỗi cơ sở dữ liệu khi lưu lớp học: " + sqle.getMessage());
                sqle.printStackTrace();
            } catch (Exception e) { // Bắt các lỗi không mong muốn khác
                showAlert("Lỗi không mong muốn khi tạo lớp học: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        // --- Lấy giá trị từ các trường UI trước ---
        String courseIdFromField = courseIdField.getText().trim(); // Mã lớp từ người dùng nhập
        String courseNameFromField = courseNameField.getText().trim();
        String subjectFromField = subjectField.getText().trim();
        LocalDate startDateFromPicker = startDatePicker.getValue();
        LocalDate endDateFromPicker = endDatePicker.getValue();
        Classroom selectedRoomFromComboBox = roomComboBox.getSelectionModel().getSelectedItem();
        String dayOfWeekFromComboBox = dayOfWeekComboBox.getValue();
        Integer startHourFromPicker = starthourPicker.getValue();
        Integer startMinuteFromPicker = startminutePicker.getValue();
        Integer endHourFromPicker = endHourPicker.getValue();
        Integer endMinuteFromPicker = endMinutePicker.getValue();

        // --- Bắt đầu Validate ---

        // 1. Validate "Mã lớp" (courseIdFromField)
        if (courseIdFromField.isEmpty()) {
            errorMessage.append("- Mã lớp không được để trống.\n");
        } else {
            // Kiểm tra tính duy nhất của Mã Lớp (Course ID)
            if (this.courseDAO != null && this.courseDAO.findById(courseIdFromField).isPresent()) {
                errorMessage.append("- Mã lớp '").append(courseIdFromField).append("' đã tồn tại. Vui lòng chọn mã khác.\n");
            }
        }

        // 2. Validate Tên lớp
        if (courseNameFromField.isEmpty()) {
            errorMessage.append("- Tên lớp không được để trống.\n");
        }

        // 3. Validate Môn học
        if (subjectFromField.isEmpty()) {
            errorMessage.append("- Môn học không được để trống.\n");
        }

        // 4. Validate Ngày bắt đầu
        if (startDateFromPicker == null) {
            errorMessage.append("- Ngày bắt đầu không được để trống.\n");
        }

        // 5. Validate Ngày kết thúc
        if (endDateFromPicker == null) {
            errorMessage.append("- Ngày kết thúc không được để trống.\n");
        } else if (startDateFromPicker != null && endDateFromPicker.isBefore(startDateFromPicker)) {
            errorMessage.append("- Ngày kết thúc phải sau ngày bắt đầu.\n");
        }

        // 6. Validate Phòng học
        if (selectedRoomFromComboBox == null) {
            errorMessage.append("- Vui lòng chọn phòng học.\n");
        }

        // 7. Validate Ngày trong tuần
        if (dayOfWeekFromComboBox == null || dayOfWeekFromComboBox.trim().isEmpty()) {
            errorMessage.append("- Vui lòng chọn ngày trong tuần.\n");
        }

        // 8. Validate Giờ bắt đầu
        if (startHourFromPicker == null || startMinuteFromPicker == null) {
            errorMessage.append("- Vui lòng chọn giờ và phút bắt đầu.\n");
        }

        // 9. Validate Giờ kết thúc
        if (endHourFromPicker == null || endMinuteFromPicker == null) {
            errorMessage.append("- Vui lòng chọn giờ và phút kết thúc.\n");
        }

        // 10. Validate mối quan hệ giữa Giờ bắt đầu và Giờ kết thúc
        LocalTime proposedStartTime = null;
        LocalTime proposedEndTime = null;
        if (startHourFromPicker != null && startMinuteFromPicker != null &&
                endHourFromPicker != null && endMinuteFromPicker != null) {
            proposedStartTime = LocalTime.of(startHourFromPicker, startMinuteFromPicker);
            proposedEndTime = LocalTime.of(endHourFromPicker, endMinuteFromPicker);

            if (!proposedEndTime.isAfter(proposedStartTime)) {
                errorMessage.append("- Giờ kết thúc phải sau giờ bắt đầu.\n");
            }
        }

        // --- KẾT THÚC VALIDATE CƠ BẢN ---

        // Nếu có lỗi từ các validate cơ bản, hiển thị và dừng lại sớm
        if (errorMessage.length() > 0) {
            showAlert("Vui lòng sửa các lỗi sau:\n" + errorMessage.toString());
            return false;
        }

        // --- VALIDATE XUNG ĐỘT LỊCH PHÒNG HỌC (chỉ thực hiện nếu các trường cần thiết đã hợp lệ) ---
        // Tại thời điểm này, selectedRoomFromComboBox, dayOfWeekFromComboBox,
        // proposedStartTime, proposedEndTime đã được kiểm tra là không null (hoặc đã có lỗi ở trên)
        // và courseDAO cũng nên được kiểm tra.
        if (this.courseDAO != null && selectedRoomFromComboBox != null && dayOfWeekFromComboBox != null && proposedStartTime != null && proposedEndTime != null) {
            String selectedRoomId = selectedRoomFromComboBox.getRoomId();
            try {
                if (this.courseDAO.hasTimeConflictForRoom(selectedRoomId, dayOfWeekFromComboBox, proposedStartTime, proposedEndTime)) {
                    errorMessage.append("- Phòng học '").append(selectedRoomFromComboBox.getRoomName())
                            .append("' đã được đăng ký vào khung giờ từ ")
                            .append(proposedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append(" đến ").append(proposedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                            .append(" vào ngày '").append(dayOfWeekFromComboBox).append("'.\n")
                            .append("  Vui lòng chọn thời gian hoặc phòng khác.\n");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi kiểm tra xung đột lịch phòng: " + e.getMessage());
                errorMessage.append("- Lỗi hệ thống khi kiểm tra lịch phòng. Vui lòng thử lại sau hoặc liên hệ quản trị viên.\n");
                e.printStackTrace();
            }
        }

        // Kiểm tra cuối cùng và hiển thị thông báo lỗi nếu có sau khi kiểm tra xung đột
        if (errorMessage.length() > 0) {
            showAlert("Vui lòng sửa các lỗi sau:\n" + errorMessage.toString());
            return false;
        }

        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void show() {
        stage.showAndWait();
    }
}

