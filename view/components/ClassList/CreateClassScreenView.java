package view.components.ClassList;

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
import src.model.system.course.Course;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CreateClassScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String GREEN_COLOR = "#4CAF50";
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
    private TextArea scheduleField;
    private Button saveButton;
    private Button cancelButton;

    // Callback interface for returning the created course
    public interface CreateClassCallback {
        void onCourseCreated(Course course);
    }

    private CreateClassCallback callback;

    public CreateClassScreenView(CreateClassCallback callback) {
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

        // Các trường nhập liệu
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

        Label scheduleLabel = new Label("Lịch học:");
        scheduleField = new TextArea();
        scheduleField.setPromptText("Ví dụ: Thứ 2 - 8:00\nThứ 4 - 8:00");
        scheduleField.setPrefRowCount(3);
        setFieldStyle(scheduleField);

        // Thêm các trường vào grid
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

        formGrid.add(scheduleLabel, 0, row);
        formGrid.add(scheduleField, 1, row);

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

        saveButton = new Button("Lưu");
        saveButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        buttonBox.getChildren().addAll(cancelButton, saveButton);
        root.getChildren().add(buttonBox);

        // Event handlers
        saveButton.setOnAction(e -> createCourse());
        cancelButton.setOnAction(e -> stage.close());
    }


    private void createCourse() {
        // Validate input fields
        if (validateInputs()) {
            try {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();

                // Parse schedule information from the text area
                // Format expected: "Thứ 2 - 8:00\nThứ 4 - 8:00"
                String scheduleText = scheduleField.getText();
                List<String> daysOfWeek = new ArrayList<>();
                LocalTime startTime = LocalTime.of(8, 0); // Default value
                LocalTime endTime = LocalTime.of(9, 30);  // Default value

                if (!scheduleText.isEmpty()) {
                    // Parse schedule text to extract days and times
                    String[] scheduleLines = scheduleText.split("\n");
                    for (String line : scheduleLines) {
                        // Extract day of week from each line
                        if (line.toLowerCase().contains("thứ")) {
                            String day = line.split("-")[0].trim();
                            daysOfWeek.add(day);
                        }

                        // Try to extract time if available
                        if (line.contains("-") && line.split("-").length > 1) {
                            String timeStr = line.split("-")[1].trim();
                            try {
                                // Assuming format like "8:00"
                                String[] timeParts = timeStr.split(":");
                                if (timeParts.length == 2) {
                                    startTime = LocalTime.of(
                                            Integer.parseInt(timeParts[0].trim()),
                                            Integer.parseInt(timeParts[1].trim())
                                    );
                                    // Set endTime to be 90 minutes after startTime by default
                                    endTime = startTime.plusMinutes(90);
                                }
                            } catch (Exception e) {
                                // If parsing fails, keep default values
                            }
                        }
                    }
                }

                // Create Course object with the updated constructor
                Course course = new Course(
                        courseIdField.getText(),
                        courseNameField.getText(),
                        subjectField.getText(),
                        startDate,
                        endDate,
                        startTime,
                        endTime,
                        daysOfWeek
                );

                // Set room ID if provided
                if (!roomIdField.getText().isEmpty()) {
                    course.setRoomId(roomIdField.getText());
                }

                // Callback to parent screen
                if (callback != null) {
                    callback.onCourseCreated(course);
                }

                // Close the dialog
                stage.close();

            } catch (Exception e) {
                showAlert("Lỗi khi tạo lớp học: " + e.getMessage());
            }
        }
    }



    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        if (courseIdField.getText().isEmpty()) {
            errorMessage.append("- Mã lớp không được để trống\n");
        }

        if (courseNameField.getText().isEmpty()) {
            errorMessage.append("- Tên lớp không được để trống\n");
        }

        if (subjectField.getText().isEmpty()) {
            errorMessage.append("- Môn học không được để trống\n");
        }

        if (startDatePicker.getValue() == null) {
            errorMessage.append("- Ngày bắt đầu không được để trống\n");
        }

        if (endDatePicker.getValue() == null) {
            errorMessage.append("- Ngày kết thúc không được để trống\n");
        } else if (startDatePicker.getValue() != null &&
                endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            errorMessage.append("- Ngày kết thúc phải sau ngày bắt đầu\n");
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
