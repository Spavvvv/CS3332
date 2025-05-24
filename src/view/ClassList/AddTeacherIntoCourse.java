package src.view.ClassList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import src.dao.Person.CourseDAO;
import src.dao.Person.TeacherDAO;
import src.model.person.Teacher;
import src.model.system.course.Course;
import src.utils.DatabaseConnection; // Assuming you need to manage database connections

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AddTeacherIntoCourse {

    private final CourseDAO courseDAO;
    private final TeacherDAO teacherDAO;
    private final Stage stage;
    private final Course selectedCourse;
    private final Label currentTeacherLabel;
    private final ListView<Teacher> teacherListView;

    public AddTeacherIntoCourse(Course selectedCourse, CourseDAO courseDAO, TeacherDAO teacherDAO) {
        this.selectedCourse = selectedCourse;
        this.courseDAO = courseDAO;
        this.teacherDAO = teacherDAO;
        this.stage = new Stage();
        this.currentTeacherLabel = new Label();
        this.teacherListView = new ListView<>();
    }

    public void show() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Quản Lý Giáo Viên Cho Lớp: " + selectedCourse.getCourseName());
        stage.setWidth(600);
        stage.setHeight(500);

        VBox layout = createLayout();
        Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private VBox createLayout() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Header Label
        Label headerLabel = new Label("Quản Lý Giáo Viên Cho Lớp");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Current teacher display
        Label currentTeacherHeader = new Label("Giáo Viên Hiện Tại:");
        currentTeacherHeader.setStyle("-fx-font-weight: bold;");
        loadCurrentTeacher();

        // Teacher selection
        Label teacherSelectionHeader = new Label("Chọn Giáo Viên:");
        teacherSelectionHeader.setStyle("-fx-font-weight: bold;");
        loadTeacherList();

        // Assign button
        Button assignButton = new Button("Gán Giáo Viên");
        assignButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        assignButton.setOnAction(e -> assignTeacher());

        // Unassign button
        Button unassignButton = new Button("Hủy Gán Giáo Viên");
        unassignButton.setStyle("-fx-background-color: #FF4C4C; -fx-text-fill: white;");
        unassignButton.setOnAction(e -> unassignTeacher());

        HBox buttonBox = new HBox(10, assignButton, unassignButton);
        buttonBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(
                headerLabel,
                currentTeacherHeader,
                currentTeacherLabel,
                teacherSelectionHeader,
                teacherListView,
                buttonBox
        );

        return layout;
    }

    private void loadCurrentTeacher() {
        Teacher currentTeacher = selectedCourse.getTeacher();
        if (currentTeacher != null) {
            currentTeacherLabel.setText(currentTeacher.getName() + " (ID: " + currentTeacher.getId() + ")");
        } else {
            currentTeacherLabel.setText("Hiện chưa có giáo viên nào được gán.");
        }
    }

    private void loadTeacherList() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<Teacher> teachers = teacherDAO.findAll(); // Get all teachers
            ObservableList<Teacher> teacherObservableList = FXCollections.observableArrayList(teachers);
            teacherListView.setItems(teacherObservableList);
            teacherListView.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Teacher item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName() + " (ID: " + item.getId() + ")");
                    }
                }
            });
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách giáo viên: " + e.getMessage());
        }
    }

    private void assignTeacher() {
        Teacher selectedTeacher = teacherListView.getSelectionModel().getSelectedItem();
        if (selectedTeacher == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh Báo", "Vui lòng chọn một giáo viên.");
            return;
        }

        if (selectedCourse.getTeacher() != null && selectedCourse.getTeacher().getId().equals(selectedTeacher.getId())) {
            showAlert(Alert.AlertType.INFORMATION, "Thông Báo", "Giáo viên này đã được gán cho lớp.");
            return;
        }

        Optional<ButtonType> result = showConfirmation("Xác Nhận", "Bạn có chắc muốn gán giáo viên này vào lớp?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseDAO.assignTeacherToCourse(selectedCourse.getCourseId(), selectedTeacher.getId());
                selectedCourse.setTeacher(selectedTeacher); // Update in-memory data
                loadCurrentTeacher();
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã gán giáo viên cho lớp.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể gán giáo viên: " + e.getMessage());
            }
        }
    }

    private void unassignTeacher() {
        if (selectedCourse.getTeacher() == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh Báo", "Hiện không có giáo viên gán. Không cần hủy.");
            return;
        }

        Optional<ButtonType> result = showConfirmation(
                "Xác Nhận", "Bạn có chắc muốn hủy gán giáo viên này khỏi lớp?");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                courseDAO.removeTeacherFromCourse(selectedCourse.getCourseId());
                selectedCourse.setTeacher(null); // Update in-memory data
                loadCurrentTeacher();
                showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Đã hủy gán giáo viên.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy gán giáo viên: " + e.getMessage());
            }
        }
    }

    private Optional<ButtonType> showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        return alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}