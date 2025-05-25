package src.view.ClassList;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.util.Callback;
import javafx.util.Pair;
import src.dao.Person.CourseDAO;
import src.dao.Person.StudentDAO;
import src.model.person.Student;
import src.utils.DaoManager; // Đảm bảo bạn có lớp này để lấy DAO
import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AddStudentToCourseDialog extends Dialog<Pair<String, String>> {

    // Components for the left panel (adding students)
    private TextField studentIdField;
    private TextField studentNameField;
    private ListView<Student> studentListView;

    // Component for the right panel (displaying enrolled students)
    private TableView<Student> enrolledStudentsTableView;

    private String courseId;
    private String courseName;
    private CourseDAO courseDAO;
    private StudentDAO studentDAO;

    // Constructor nhận ClassInfo từ ClassListScreenView
    public AddStudentToCourseDialog(ClassListScreenView.ClassInfo classInfo) {
        this.studentDAO = DaoManager.getInstance().getStudentDAO(); // Lấy StudentDAO từ DaoManager
        this.courseDAO = DaoManager.getInstance().getCourseDAO();   // Lấy CourseDAO từ DaoManager

        this.courseId = classInfo.getCode();
        this.courseName = classInfo.getName();

        setTitle("Quản Lý Học Viên Lớp: " + this.courseName + " (Mã: " + this.courseId + ")");
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().setPrefWidth(1100);
        getDialogPane().setPrefHeight(600);

        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(15));

        VBox leftPanel = setupLeftPanel();
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        VBox rightPanel = setupRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftPanel, rightPanel);
        getDialogPane().setContent(mainLayout);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Thêm Học Viên Đã Chọn");
        okButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        final Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Đóng");

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Student selectedStudentFromList = studentListView.getSelectionModel().getSelectedItem();

                if (selectedStudentFromList == null || selectedStudentFromList.getId().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Nhập Liệu", "Bạn chưa chọn học viên từ danh sách bên trái.");
                    return null;
                }

                String selectedStudentId = selectedStudentFromList.getId();
                String selectedStudentName = selectedStudentFromList.getName();

                boolean success = courseDAO.addStudentToCourse(this.courseId, selectedStudentId);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Học viên " + selectedStudentName + " đã được thêm vào lớp " + this.courseName);
                    loadEnrolledStudents();      // Refresh bảng học viên đã ghi danh (phải)
                    loadAllStudentsForSelection(); // Refresh danh sách học viên có thể chọn (trái)
                    studentIdField.clear();
                    studentNameField.clear();
                    studentListView.getSelectionModel().clearSelection();
                    return new Pair<>(selectedStudentId, selectedStudentName);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm học viên. Có thể học viên đã có trong lớp hoặc có lỗi xảy ra.");
                    return null;
                }
            }
            return null;
        });

        loadAllStudentsForSelection();
        loadEnrolledStudents();

        Platform.runLater(() -> studentListView.requestFocus());
    }

    private VBox setupLeftPanel() {
        VBox leftPanelRoot = new VBox(10);
        leftPanelRoot.setPadding(new Insets(10));
        leftPanelRoot.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: #FAFAFA;");

        Label leftHeaderLabel = new Label("Chọn Học Viên Để Thêm Vào Lớp");
        leftHeaderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        leftHeaderLabel.setTextFill(Color.DARKBLUE);
        leftPanelRoot.getChildren().add(leftHeaderLabel);

        GridPane inputPane = new GridPane();
        inputPane.setHgap(10);
        inputPane.setVgap(10);
        inputPane.setPadding(new Insets(10, 0, 10, 0));

        Label studentIdLabel = new Label("Mã Học Viên:");
        studentIdLabel.setFont(Font.font("Arial", 14));
        studentIdField = new TextField();
        studentIdField.setEditable(false);
        studentIdField.setStyle("-fx-opacity: 0.9; -fx-background-color: #E8E8E8;");

        Label studentNameLabel = new Label("Tên Học Viên:");
        studentNameLabel.setFont(Font.font("Arial", 14));
        studentNameField = new TextField();
        studentNameField.setEditable(false);
        studentNameField.setStyle("-fx-opacity: 0.9; -fx-background-color: #E8E8E8;");

        inputPane.add(studentIdLabel, 0, 0);
        inputPane.add(studentIdField, 1, 0);
        inputPane.add(studentNameLabel, 0, 1);
        inputPane.add(studentNameField, 1, 1);
        GridPane.setHgrow(studentIdField, Priority.ALWAYS);
        GridPane.setHgrow(studentNameField, Priority.ALWAYS);
        leftPanelRoot.getChildren().add(inputPane);

        Label listLabel = new Label("Danh Sách Tất Cả Học Viên Có Thể Thêm:"); // Sửa label
        listLabel.setFont(Font.font("Arial", 16));
        listLabel.setTextFill(Color.DARKGREEN);

        studentListView = new ListView<>();
        studentListView.setPrefHeight(300);
        VBox.setVgrow(studentListView, Priority.ALWAYS);
        studentListView.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 1; -fx-border-radius: 3; -fx-padding: 3;");

        studentListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellBox = new HBox(10);
                    cellBox.setPadding(new Insets(3));
                    Label nameLabel = new Label(student.getName());
                    nameLabel.setFont(Font.font(14));
                    nameLabel.setTextFill(Color.BLACK);

                    Label idLabel = new Label("(" + student.getId() + ")");
                    idLabel.setFont(Font.font(13));
                    idLabel.setTextFill(Color.GRAY);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    cellBox.getChildren().addAll(nameLabel, spacer, idLabel);
                    setGraphic(cellBox);
                }
            }
        });

        studentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                studentIdField.setText(newVal.getId());
                studentNameField.setText(newVal.getName());
            } else {
                studentIdField.clear();
                studentNameField.clear();
            }
        });

        leftPanelRoot.getChildren().addAll(listLabel, studentListView);
        return leftPanelRoot;
    }

    private VBox setupRightPanel() {
        VBox rightPanelRoot = new VBox(10);
        rightPanelRoot.setPadding(new Insets(10));
        rightPanelRoot.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: #F0F8FF;");

        Label rightHeaderLabel = new Label("Học Viên Hiện Có Trong Lớp");
        rightHeaderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        rightHeaderLabel.setTextFill(Color.rgb(0, 100, 0));

        enrolledStudentsTableView = new TableView<>();
        enrolledStudentsTableView.setPrefHeight(400);
        VBox.setVgrow(enrolledStudentsTableView, Priority.ALWAYS);
        enrolledStudentsTableView.setPlaceholder(new Label("Chưa có học viên nào trong lớp này."));

        TableColumn<Student, String> idCol = new TableColumn<>("Mã HV");
        idCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        idCol.setPrefWidth(100);

        TableColumn<Student, String> nameCol = new TableColumn<>("Tên Học Viên");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Student, String> birthdayCol = new TableColumn<>("Ngày Sinh");
        birthdayCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getBirthday() != null ? cellData.getValue().getBirthday().toString() : "N/A"
        ));
        birthdayCol.setPrefWidth(120);

        TableColumn<Student, String> contactCol = new TableColumn<>("Số Điện Thoại");
        contactCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getContactNumber()));
        contactCol.setPrefWidth(150);

        TableColumn<Student, Void> actionCol = new TableColumn<>("Xóa");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(createDeleteButtonCellFactory());

        enrolledStudentsTableView.getColumns().addAll(idCol, nameCol, birthdayCol, contactCol, actionCol);

        rightPanelRoot.getChildren().addAll(rightHeaderLabel, enrolledStudentsTableView);
        return rightPanelRoot;
    }

    private Callback<TableColumn<Student, Void>, TableCell<Student, Void>> createDeleteButtonCellFactory() {
        return param -> new TableCell<>() {
            private final Button deleteButton = new Button("X");

            {
                deleteButton.setStyle("-fx-background-color: #FF4C4C; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                deleteButton.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    handleDeleteAction(student);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        };
    }

    private void handleDeleteAction(Student student) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác Nhận Xóa");
        confirmAlert.setHeaderText("Bạn có chắc muốn xóa học viên này khỏi lớp?");
        confirmAlert.setContentText("Học viên: " + student.getName() + " (Mã: " + student.getId() + ")");
        confirmAlert.initOwner(getDialogPane().getScene().getWindow()); // Set owner

        ButtonType confirmButton = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE); // Đổi tên biến để tránh trùng
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButtonType);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            boolean success = courseDAO.removeStudentFromCourse(courseId, student.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Xóa Thành Công", "Học viên " + student.getName() + " đã được xóa khỏi lớp.");
                loadEnrolledStudents();      // Refresh bảng học viên đã ghi danh (phải)
                loadAllStudentsForSelection(); // Refresh danh sách học viên có thể chọn (trái)
            } else {
                showAlert(Alert.AlertType.ERROR, "Xóa Thất Bại", "Không thể xóa học viên " + student.getName() + ". Xin thử lại.");
            }
        }
    }

    private void loadAllStudentsForSelection() {
        System.out.println("DEBUG: Bắt đầu phương thức loadAllStudentsForSelection...");
        List<Student> allStudentsInSystem = new ArrayList<>();
        try {
            allStudentsInSystem = studentDAO.findAll();
            if (allStudentsInSystem == null) {
                allStudentsInSystem = new ArrayList<>();
            }
        } catch (Exception e) { // Bắt Exception chung nếu findAll có thể ném lỗi
            System.err.println("LỖI: Exception khi lấy tất cả học viên từ hệ thống: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Tải Dữ Liệu", "Không thể tải danh sách tất cả học viên.");
            studentListView.setItems(FXCollections.emptyObservableList());
            studentListView.setPlaceholder(new Label("Lỗi tải danh sách học viên."));
            return;
        }
        System.out.println("DEBUG: Tổng số học viên trong hệ thống: " + allStudentsInSystem.size());

        List<Student> enrolledStudentsInThisCourse = new ArrayList<>();
        if (this.courseId != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // SỬA ĐỔI CHÍNH Ở ĐÂY:
                // courseDAO.getStudentsByCourseId đã trả về List<Student>
                enrolledStudentsInThisCourse = courseDAO.getStudentsByCourseId(conn, this.courseId);
                System.out.println("DEBUG: Số đối tượng Student đã ghi danh trong khóa học '" + this.courseId + "': " + enrolledStudentsInThisCourse.size());
            } catch (SQLException e) {
                System.err.println("LỖI: SQLException khi lấy danh sách học viên đã ghi danh: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi Tải Dữ Liệu", "Không thể tải danh sách học viên đã ghi danh để lọc.");
                // Trong trường hợp lỗi, danh sách availableStudentsForSelection sẽ là tất cả học viên
                // điều này có thể không mong muốn, nhưng đơn giản hơn là không hiển thị gì.
            }
        } else {
            System.out.println("DEBUG: courseId là null, không thể lấy danh sách học viên đã ghi danh.");
        }

        List<Student> availableStudentsForSelection = new ArrayList<>();
        if (!allStudentsInSystem.isEmpty()) {
            Set<String> enrolledIdsSet = enrolledStudentsInThisCourse.stream()
                    .map(Student::getId)
                    .collect(Collectors.toSet());

            for (Student student : allStudentsInSystem) {
                if (!enrolledIdsSet.contains(student.getId())) {
                    availableStudentsForSelection.add(student);
                }
            }
        }
        System.out.println("DEBUG: Số học viên có sẵn để lựa chọn (sau khi lọc): " + availableStudentsForSelection.size());

        ObservableList<Student> items = FXCollections.observableArrayList(availableStudentsForSelection);
        studentListView.setItems(items);
        if (items.isEmpty()) {
            studentListView.setPlaceholder(new Label("Không có học viên mới hoặc tất cả đã ở trong lớp."));
        } else {
            studentListView.setPlaceholder(new Label("Chọn học viên từ danh sách."));
        }
        System.out.println("DEBUG: Kết thúc phương thức loadAllStudentsForSelection.");
    }

    private void loadEnrolledStudents() {
        // Phương thức này vẫn đúng vì nó sử dụng getStudentIdsByCourseId (trả về List<String>)
        // và getStudentsByIds (nhận List<String>)
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> studentIds = courseDAO.getStudentIdsByCourseId(conn, this.courseId); // Giả sử CourseDAO có phương thức này
            List<Student> enrolledStudents = studentDAO.getStudentsByIds(conn, studentIds); // Giả sử StudentDAO có phương thức này

            ObservableList<Student> observableList = FXCollections.observableArrayList(enrolledStudents);
            enrolledStudentsTableView.setItems(observableList);
            if (observableList.isEmpty()) {
                enrolledStudentsTableView.setPlaceholder(new Label("Chưa có học viên nào trong lớp này."));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách học viên đã ghi danh trong lớp: " + e.getMessage());
            e.printStackTrace();
            enrolledStudentsTableView.setItems(FXCollections.emptyObservableList()); // Xóa dữ liệu cũ nếu có lỗi
            enrolledStudentsTableView.setPlaceholder(new Label("Lỗi tải danh sách học viên."));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}