package src.view.ClassList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import src.dao.Person.TeacherDAO; // Giữ nguyên TeacherDAO cho việc lấy tên giáo viên
import src.model.system.course.Course;
import src.model.person.Teacher;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ClassDetailsDialog {
    private Stage dialogStage;
    // private VBox mainContainer; // Không cần là field nếu chỉ dùng cục bộ trong createDialog
    // private ScrollPane scrollPane; // Không cần là field nếu chỉ dùng cục bộ trong createDialog
    private TeacherDAO teacherDAO;

    private Course course; // Đối tượng Course chứa thông tin để hiển thị (đã được cập nhật)

    // Constants for styling (giữ nguyên)
    private static final String PRIMARY_COLOR = "#4F46E5";
    private static final String BORDER_COLOR = "#E2E8F0";
    // private static final String TEXT_COLOR = "#334155"; // Không dùng trực tiếp
    private static final String LABEL_COLOR = "#64748B";
    private static final String BG_COLOR = "#F8FAFC";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    public ClassDetailsDialog(Course course, TeacherDAO teacherDAO) {
        this.course = course;
        this.teacherDAO = teacherDAO;
        createDialog();
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Chi tiết Lớp học"); // Đổi lại tiêu đề
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setMinWidth(700);
        dialogStage.setMinHeight(600); // Tăng chiều cao một chút cho các trường mới

        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: white;");

        VBox formContainer = createFormContainer();
        VBox sidebar = createSidebar(); // Giữ lại sidebar nếu vẫn muốn dùng

        HBox contentLayout = new HBox(20);
        contentLayout.getChildren().addAll(formContainer, sidebar);
        HBox.setHgrow(formContainer, Priority.ALWAYS);

        HBox footer = createFooter();

        mainContainer.getChildren().addAll(contentLayout, footer);

        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(scrollPane, 700, 600);
        dialogStage.setScene(scene);
    }

    private VBox createFormContainer() {
        VBox form = new VBox(15);
        form.setStyle("-fx-background-color: white;");

        GridPane gridPane = new GridPane();
        gridPane.setVgap(12); // Điều chỉnh khoảng cách
        gridPane.setHgap(12);

        int row = 0;

        // Mã lớp và Tên lớp
        gridPane.add(createStyledLabel("Mã lớp:"), 0, row);
        gridPane.add(createReadOnlyTextField(course.getCourseId()), 1, row);
        gridPane.add(createStyledLabel("Tên lớp:"), 2, row);
        gridPane.add(createReadOnlyTextField(course.getCourseName()), 3, row);
        row++;

        // Môn học
        gridPane.add(createStyledLabel("Môn học:"), 0, row);
        gridPane.add(createReadOnlyTextField(course.getSubject()), 1, row, 3, 1); // Span 3 cột
        row++;

        // Ngày bắt đầu và Ngày kết thúc
        gridPane.add(createStyledLabel("Ngày bắt đầu:"), 0, row);
        gridPane.add(createReadOnlyTextField(
                course.getStartDate() != null ? course.getStartDate().format(DATE_FORMATTER) : "N/A"
        ), 1, row);
        gridPane.add(createStyledLabel("Ngày kết thúc:"), 2, row);
        gridPane.add(createReadOnlyTextField(
                course.getEndDate() != null ? course.getEndDate().format(DATE_FORMATTER) : "N/A"
        ), 3, row);
        row++;

        // Ngày học trong tuần (sử dụng getDaysOfWeekAsString)
        gridPane.add(createStyledLabel("Ngày học trong tuần:"), 0, row);
        gridPane.add(createReadOnlyTextField(
                course.getDaysOfWeekAsString() != null && !course.getDaysOfWeekAsString().isEmpty() ?
                        course.getDaysOfWeekAsString() : "N/A"
        ), 1, row, 3, 1); // Span 3 cột
        row++;

        // Giờ bắt đầu và Giờ kết thúc
        gridPane.add(createStyledLabel("Giờ bắt đầu:"), 0, row);
        gridPane.add(createReadOnlyTextField(
                course.getCourseStartTime() != null ? course.getCourseStartTime().format(TIME_FORMATTER) : "N/A"
        ), 1, row);
        gridPane.add(createStyledLabel("Giờ kết thúc:"), 2, row);
        gridPane.add(createReadOnlyTextField(
                course.getCourseEndTime() != null ? course.getCourseEndTime().format(TIME_FORMATTER) : "N/A"
        ), 3, row);
        row++;

        // Tổng số buổi và Tiến độ
        gridPane.add(createStyledLabel("Tổng số buổi:"), 0, row);
        gridPane.add(createReadOnlyTextField(String.valueOf(course.getTotalSessions())), 1, row); // Hiển thị totalSessions
        gridPane.add(createStyledLabel("Tiến độ:"), 2, row);
        gridPane.add(createReadOnlyTextField(String.format("%.2f%%", course.getProgress())), 3, row); // Hiển thị progress
        row++;


        // Phòng học và Sĩ số hiện tại
        gridPane.add(createStyledLabel("Phòng học (ID):"), 0, row);
        gridPane.add(createReadOnlyTextField(course.getRoomId() != null ? course.getRoomId() : "N/A"), 1, row);
        gridPane.add(createStyledLabel("Sĩ số hiện tại:"), 2, row);
        gridPane.add(createReadOnlyTextField(String.valueOf(course.getTotalCurrentStudent())), 3, row); // Hiển thị totalCurrentStudent
        row++;


        // Giáo viên và Mã nhóm SV (Class ID)
        gridPane.add(createStyledLabel("Giáo viên:"), 0, row);
        gridPane.add(createReadOnlyTextField(
                course.getTeacher() != null ? (course.getTeacher().getName() + " (ID: " + course.getTeacher().getId() + ")") : "N/A"
        ), 1, row, 3, 1); // Span 3 cột
        row++;

        gridPane.add(createStyledLabel("Mã nhóm SV (Class ID):"), 0, row);
        gridPane.add(createReadOnlyTextField(course.getClassId() != null ? course.getClassId() : "N/A"), 1, row, 3, 1);
        row++;


        // Không cần style lại label ở đây nếu createStyledLabel đã làm
        gridPane.setPadding(new Insets(15));
        form.getChildren().add(gridPane);
        return form;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        return label;
    }

    private TextField createReadOnlyTextField(String text) {
        TextField textField = new TextField(text);
        textField.setEditable(false);
        textField.setStyle(
                "-fx-background-color: #f8fafc; " + // Slightly different from BG_COLOR for contrast
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-padding: 8px 12px;" // Adjusted padding
        );
        textField.setPrefWidth(200); // Giữ nguyên hoặc điều chỉnh nếu cần
        return textField;
    }

    private VBox createSidebar() { // Giữ nguyên sidebar
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(200); // Có thể điều chỉnh
        sidebar.setStyle(
                "-fx-background-color: " + BG_COLOR + "; " +
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-width: 0 0 0 1; " + // Left border
                        "-fx-padding: 15;"
        );

        Label headerLabel = new Label("Thông tin thêm"); // Ví dụ
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + PRIMARY_COLOR + ";");

        // Bạn có thể thêm các thông tin khác vào sidebar nếu muốn
        Label placeholderLabel = new Label("Chưa có thông tin bổ sung.");
        placeholderLabel.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 12px;");

        sidebar.getChildren().addAll(headerLabel, placeholderLabel);
        HBox.setHgrow(sidebar, Priority.NEVER); // Sidebar không co giãn

        return sidebar;
    }

    private HBox createFooter() { // Giữ nguyên footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 0, 0, 0)); // Chỉ padding top

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8px 20px; " +
                        "-fx-background-radius: 4px;"
        );
        closeButton.setOnAction(e -> dialogStage.close());

        footer.getChildren().add(closeButton);
        return footer;
    }

    public void show() {
        dialogStage.showAndWait();
    }

    // getTeacherName không còn cần thiết nếu thông tin Teacher đã có trong đối tượng Course
    // và được hiển thị trực tiếp.
    // private String getTeacherName(String teacherId) {
    //     if (teacherId == null || teacherId.isEmpty()) {
    //         return "Không xác định";
    //     }
    //     Optional<Teacher> teacher = teacherDAO.findById(teacherId);
    //     return teacher.map(Teacher::getName).orElse("Không xác định (ID: " + teacherId + ")");
    // }
}