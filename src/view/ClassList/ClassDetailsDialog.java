package src.view.ClassList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import src.dao.Person.TeacherDAO;
import src.model.system.course.Course;
import src.model.person.Teacher;

import java.util.Optional;

public class ClassDetailsDialog {
    private Stage dialogStage;
    private VBox mainContainer;
    private ScrollPane scrollPane;
    private TeacherDAO teacherDAO;

    // Class information to display
    private Course course;

    // Constants for styling
    private static final String PRIMARY_COLOR = "#4F46E5";
    private static final String BORDER_COLOR = "#E2E8F0";
    private static final String TEXT_COLOR = "#334155";
    private static final String LABEL_COLOR = "#64748B";
    private static final String BG_COLOR = "#F8FAFC";

    public ClassDetailsDialog(Course course, TeacherDAO teacherDAO) {
        this.course = course; // Pass the course object containing class details
        this.teacherDAO = teacherDAO; // Inject the TeacherDAO for managing teacher relationships
        createDialog();
    }

    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Xem lớp học");
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setMinWidth(700);
        dialogStage.setMinHeight(500);

        // Main container
        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(25));
        mainContainer.setStyle("-fx-background-color: white;");

        // Create form with class details
        VBox formContainer = createFormContainer();

        // Create sidebar to display additional history or other information
        VBox sidebar = createSidebar();

        // Combine form and sidebar
        HBox contentLayout = new HBox(20);
        contentLayout.getChildren().addAll(formContainer, sidebar);
        HBox.setHgrow(formContainer, Priority.ALWAYS);

        // Create footer with close button
        HBox footer = createFooter();

        // Add to main container
        mainContainer.getChildren().addAll(contentLayout, footer);

        // Create scroll pane
        scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: white;");

        // Create scene
        Scene scene = new Scene(scrollPane, 700, 500);
        dialogStage.setScene(scene);
    }

    private VBox createFormContainer() {
        VBox form = new VBox(15);
        form.setStyle("-fx-background-color: white;");

        // GridPane for the form layout
        GridPane gridPane = new GridPane();
        gridPane.setVgap(15);
        gridPane.setHgap(15);

        // Add labels and fields to display course information
        int row = 0;

        Label codeLabel = new Label("Mã lớp:");
        TextField codeField = createReadOnlyTextField(course.getCourseId());
        gridPane.add(codeLabel, 0, row);
        gridPane.add(codeField, 1, row);

        Label nameLabel = new Label("Tên lớp:");
        TextField nameField = createReadOnlyTextField(course.getCourseName());
        gridPane.add(nameLabel, 2, row);
        gridPane.add(nameField, 3, row);
        row++;

        Label subjectLabel = new Label("Môn học:");
        TextField subjectField = createReadOnlyTextField(course.getSubject());
        gridPane.add(subjectLabel, 0, row);
        gridPane.add(subjectField, 1, row, 3, 1);
        row++;

        Label startDateLabel = new Label("Ngày bắt đầu:");
        TextField startDateField = createReadOnlyTextField(
                course.getStartDate() != null ? course.getStartDate().toString() : "Không xác định"
        );
        gridPane.add(startDateLabel, 0, row);
        gridPane.add(startDateField, 1, row);

        Label endDateLabel = new Label("Ngày kết thúc:");
        TextField endDateField = createReadOnlyTextField(
                course.getEndDate() != null ? course.getEndDate().toString() : "Không xác định"
        );
        gridPane.add(endDateLabel, 2, row);
        gridPane.add(endDateField, 3, row);
        row++;

        Label dayOfWeekLabel = new Label("Ngày học:");
        TextField dayOfWeekField = createReadOnlyTextField(course.getDayOfWeek());
        gridPane.add(dayOfWeekLabel, 0, row);
        gridPane.add(dayOfWeekField, 1, row);
        row++;

        Label startTimeLabel = new Label("Giờ bắt đầu:");
        TextField startTimeField = createReadOnlyTextField(
                course.getCourseStartTime() != null ? course.getCourseStartTime().toString() : "Không xác định"
        );
        gridPane.add(startTimeLabel, 0, row);
        gridPane.add(startTimeField, 1, row);

        Label endTimeLabel = new Label("Giờ kết thúc:");
        TextField endTimeField = createReadOnlyTextField(
                course.getCourseEndTime() != null ? course.getCourseEndTime().toString() : "Không xác định"
        );
        gridPane.add(endTimeLabel, 2, row);
        gridPane.add(endTimeField, 3, row);
        row++;

        Label roomLabel = new Label("Mã phòng học:");
        TextField roomField = createReadOnlyTextField(course.getRoomId() != null ? course.getRoomId() : "Không xác định");
        gridPane.add(roomLabel, 0, row);
        gridPane.add(roomField, 1, row);
        row++;

        Label teacherLabel = new Label("Giáo viên:");
        TextField teacherField = createReadOnlyTextField(getTeacherName(course.getTeacher() != null ? course.getTeacher().getId() : null));
        gridPane.add(teacherLabel, 0, row);
        gridPane.add(teacherField, 1, row);

        // Style for all labels
        gridPane.getChildren().filtered(node -> node instanceof Label).forEach(node -> {
            Label label = (Label) node;
            label.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 13px;");
        });

        // Add padding to the grid
        gridPane.setPadding(new Insets(15));

        // Add the grid to form
        form.getChildren().add(gridPane);

        return form;
    }

    private TextField createReadOnlyTextField(String text) {
        TextField textField = new TextField(text);
        textField.setEditable(false);
        textField.setStyle(
                "-fx-background-color: #f8fafc; " +
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-radius: 4px; " +
                        "-fx-padding: 8 15;"
        );
        textField.setPrefWidth(200);
        return textField;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(200);
        sidebar.setStyle(
                "-fx-background-color: " + BG_COLOR + "; " +
                        "-fx-border-color: " + BORDER_COLOR + "; " +
                        "-fx-border-width: 0 0 0 1; " +
                        "-fx-padding: 15;"
        );

        Label headerLabel = new Label("Lịch sử");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label noHistoryLabel = new Label("Không có hoạt động gần đây");
        noHistoryLabel.setStyle("-fx-text-fill: " + LABEL_COLOR + "; -fx-font-size: 12px;");

        sidebar.getChildren().addAll(headerLabel, noHistoryLabel);

        return sidebar;
    }

    private HBox createFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 0, 0, 0));
        footer.setSpacing(10);

        Button closeButton = new Button("Đóng");
        closeButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 4px;"
        );
        closeButton.setOnAction(e -> dialogStage.close());

        footer.getChildren().add(closeButton);

        return footer;
    }

    public void show() {
        dialogStage.showAndWait();
    }

    private String getTeacherName(String teacherId) {
        if (teacherId == null || teacherId.isEmpty()) {
            return "Không xác định";
        }

        // Fetch teacher name using TeacherDAO
        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        return teacher.map(Teacher::getName).orElse("Không xác định");
    }
}