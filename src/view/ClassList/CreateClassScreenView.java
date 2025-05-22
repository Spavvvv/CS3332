
package src.view.ClassList;

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
import src.dao.ClassSession.ClassSessionDAO;
import src.model.system.course.Course;
import src.dao.Person.CourseDAO; // Đảm bảo import này tồn tại và đúng

import java.time.LocalDate;
import java.time.LocalTime;
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
    private TextField roomIdField;
    private Button saveButton;
    private Button cancelButton;

    // Thêm biến thành viên cho CourseDAO
    private CourseDAO courseDAO;
    private CreateClassCallback callback;

    // SỬA ĐỔI 1: Interface CreateClassCallback chỉ định nghĩa chữ ký
    public interface CreateClassCallback {
        void onCourseCreated(Course course); // Bỏ phần implementation mặc định ở đây
    }

    // SỬA ĐỔI 2: Constructor nhận CourseDAO
    public CreateClassScreenView(CourseDAO courseDAO, CreateClassCallback callback) {
        this.courseDAO = courseDAO; // Lưu trữ CourseDAO được truyền vào
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

        Label courseIdLabel = new Label("Mã lớp:");
        courseIdField = new TextField();
        courseIdField.setPromptText("Ví dụ: LH001");
        setFieldStyle(courseIdField);

        Label courseNameLabel = new Label("Tên lớp:");
        courseNameField = new TextField();
        courseNameField.setPromptText("Ví dụ: Lớp Toán Nâng Cao");
        setFieldStyle(courseNameField);

        Label subjectLabel = new Label("Môn học:");
        subjectField = new TextField();
        subjectField.setPromptText("Ví dụ: Toán");
        setFieldStyle(subjectField);

        Label startDateLabel = new Label("Ngày bắt đầu:");
        startDatePicker = new DatePicker(LocalDate.now());
        setFieldStyle(startDatePicker);

        Label endDateLabel = new Label("Ngày kết thúc:");
        endDatePicker = new DatePicker(LocalDate.now().plusMonths(3));
        setFieldStyle(endDatePicker);

        Label roomIdLabel = new Label("Phòng học:");
        roomIdField = new TextField();
        roomIdField.setPromptText("Ví dụ: P101");
        setFieldStyle(roomIdField);
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

        formGrid.add(roomIdLabel, 0, row);
        formGrid.add(roomIdField, 1, row++);

        root.getChildren().add(formGrid);
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
        if (validateInputs()) { // validateInputs() cần được cập nhật để phản ánh ý nghĩa mới của các trường
            try {
                // 1. LẤY GIÁ TRỊ TỪ UI THEO Ý NGHĨA MỚI
                // Người dùng nhập MÃ PHÒNG HỌC (room_id) vào trường courseIdField (trường "Mã lớp" trên UI)
                String actualRoomId = courseIdField.getText().trim();

                // Người dùng nhập MÃ KHÓA HỌC (course_id) vào trường roomIdField (trường "Phòng học" trên UI)
                String actualCourseId = roomIdField.getText().trim();

                String courseName = courseNameField.getText().trim();
                String subject = subjectField.getText().trim();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                // Xử lý thông tin từ lịch biểu (giữ nguyên logic này nếu bạn vẫn dùng)
                List<String> daysOfWeek = new ArrayList<>(); // Ví dụ: ["Thứ 2", "Thứ 4"]
                LocalTime startTime = LocalTime.of(8, 0);    // Giờ mặc định
                LocalTime endTime = startTime.plusMinutes(90); // Giờ kết thúc mặc định

                // 2. TẠO ĐỐI TƯỢNG COURSE VỚI GIÁ TRỊ ĐÃ ĐƯỢC GÁN ĐÚNG
                Course course = new Course(
                        actualCourseId,  // Sử dụng giá trị từ roomIdField (nay là mã khóa học)
                        courseName,
                        subject,
                        startDate,
                        endDate
                );

                if (actualRoomId.isEmpty()) {
                    course.setRoomId(null); // Hoặc giữ nguyên actualRoomId nếu DB và logic cho phép chuỗi rỗng
                } else {
                    course.setRoomId(actualRoomId);
                }


                // Gán CLASS_ID là NULL theo yêu cầu
                course.setClassId(null);

                // Gán các thuộc tính khác (giữ nguyên nếu có)
                course.setDaysOfWeekList(daysOfWeek);
                course.setCourseSessionStartTime(startTime);
                course.setCourseSessionEndTime(endTime);
                // course.setProgress(0f); // Cân nhắc đặt progress mặc định nếu cần

                // 3. LƯU COURSE VÀO DATABASE
                boolean isSaved = courseDAO.save(course);

                if (isSaved) {
                    // SỬ DỤNG CLASSSESSIONDAO ĐỂ TẠO BUỔI HỌC (giữ nguyên logic này)
                    ClassSessionDAO classSessionDAO = new ClassSessionDAO();
//                    try (Connection conn = DatabaseConnection.getConnection()) {
//                        List<ClassSession> savedSessions = classSessionDAO.generateAndSaveSessionsForCourse(
//                                conn,
//                                course,           // Đối tượng Course vừa tạo
//                                null,             // Placeholder cho ClassroomDAO
//                                null,             // Placeholder cho TeacherDAO
//                                null,             // Placeholder cho HolidayDAO
//                                null              // Placeholder cho ScheduleDAO
//                        );
//
//                        if (!savedSessions.isEmpty()) {
//                            showAlert("Lưu lớp học và các buổi học thành công!");
//                        } else {
//                            showAlert("Lưu lớp học thành công nhưng không thể tạo buổi học tự động.");
//                        }
//                    } catch (java.sql.SQLException sessionEx) {
//                        showAlert("Lưu lớp học thành công, nhưng có lỗi khi tạo các buổi học: " + sessionEx.getMessage());
//                        sessionEx.printStackTrace();
//                    }

                    if (callback != null) {
                        callback.onCourseCreated(course);
                    }
                    stage.close();
                } else {
                    // Thông báo lỗi này đã phản ánh đúng ý nghĩa mới của trường và lỗi khóa ngoại bạn gặp
                    showAlert("Không thể lưu lớp học. Vui lòng kiểm tra lại:\n" +
                            "- Mã phòng học (nhập ở trường 'Mã lớp') phải là một mã phòng học hợp lệ và đã tồn tại trong hệ thống (nếu không được để trống).\n" +
                            "- Mã khóa học (nhập ở trường 'Phòng học') có thể đã bị trùng với một khóa học khác.");
                }
            } catch (Exception e) {
                showAlert("Lỗi khi tạo lớp học: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        // 1. VALIDATE "Mã Khóa Học" (actualCourseId, nhập từ roomIdField - UI là "Phòng học")
        String actualCourseId = roomIdField.getText().trim();
        if (actualCourseId.isEmpty()) {
            errorMessage.append("- Mã khóa học (nhập ở trường 'Phòng học') không được để trống.\n");
        } else {
            // Kiểm tra tính duy nhất của Mã Khóa Học
            if (this.courseDAO.findById(actualCourseId).isPresent()) {
                errorMessage.append("- Mã khóa học (nhập ở trường 'Phòng học') '" + actualCourseId + "' đã tồn tại. Vui lòng chọn mã khác.\n");
            }
        }

        // 2. VALIDATE "Mã Phòng Học" (actualRoomId, nhập từ courseIdField - UI là "Mã lớp")
        // Yêu cầu mới: Chỉ cần không để trống, không kiểm tra sự tồn tại ở client-side.
        String actualRoomId = courseIdField.getText().trim();
        if (actualRoomId.isEmpty()) {
            errorMessage.append("- Mã phòng học (nhập ở trường 'Mã lớp') không được để trống.\n");
        }
        // KHÔNG còn kiểm tra sự tồn tại của actualRoomId trong bảng 'rooms' ở đây nữa.

        // 3. CÁC VALIDATION KHÁC (Tên lớp, Môn học, Ngày bắt đầu, Ngày kết thúc)
        if (courseNameField.getText().trim().isEmpty()) {
            errorMessage.append("- Tên lớp không được để trống.\n");
        }
        if (subjectField.getText().trim().isEmpty()) {
            errorMessage.append("- Môn học không được để trống.\n");
        }
        if (startDatePicker.getValue() == null) {
            errorMessage.append("- Ngày bắt đầu không được để trống.\n");
        }
        if (endDatePicker.getValue() == null) {
            errorMessage.append("- Ngày kết thúc không được để trống.\n");
        } else if (startDatePicker.getValue() != null &&
                endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            errorMessage.append("- Ngày kết thúc phải sau ngày bắt đầu.\n");
        }

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

