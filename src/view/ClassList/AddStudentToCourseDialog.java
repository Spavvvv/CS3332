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
import src.utils.DaoManager;
import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AddStudentToCourseDialog extends Dialog<Pair<String, String>> {

    // Components for the left panel (adding students) - Old parts
    private TextField studentIdField;
    private TextField studentNameField;
    private ListView<Student> studentListView;

    // Component for the right panel (displaying enrolled students) - New part
    private TableView<Student> enrolledStudentsTableView;

    private String courseId;
    private String courseName;
    private CourseDAO courseDAO;
    private StudentDAO studentDAO;

    public AddStudentToCourseDialog(ClassListScreenView.ClassInfo classInfo) {
        this.studentDAO = new StudentDAO(); // Initialize StudentDAO
        this.courseDAO = DaoManager.getInstance().getCourseDAO(); // Initialize CourseDAO

        this.courseId = classInfo.getCode();
        this.courseName = classInfo.getName();

        setTitle("Quản Lý Học Viên Lớp: " + this.courseName + " (Mã: " + this.courseId + ")");
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().setPrefWidth(1100); // Increased width to accommodate both panels
        getDialogPane().setPrefHeight(600);

        // Setup main layout with HBox for two panels
        HBox mainLayout = new HBox(20); // 20px spacing between panels
        mainLayout.setPadding(new Insets(15));

        // Setup left panel for adding students
        VBox leftPanel = setupLeftPanel();
        HBox.setHgrow(leftPanel, Priority.ALWAYS); // Allow left panel to grow

        // Setup right panel for displaying enrolled students
        VBox rightPanel = setupRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS); // Allow right panel to grow

        mainLayout.getChildren().addAll(leftPanel, rightPanel);
        getDialogPane().setContent(mainLayout);

        // Buttons
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Thêm Học Viên Đã Chọn"); // Changed button text for clarity
        okButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        final Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Đóng");


        // Handle OK Button action (for adding student from the left panel)
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String selectedStudentId = studentIdField.getText().trim();
                String selectedStudentName = studentNameField.getText().trim();

                if (selectedStudentId.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Nhập Liệu", "Bạn chưa chọn học viên từ danh sách bên trái.");
                    return null; // Prevent dialog from closing
                }

                boolean success = courseDAO.addStudentToCourse(this.courseId, selectedStudentId);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành Công", "Học viên " + selectedStudentName + " đã được thêm vào lớp " + this.courseName);
                    loadEnrolledStudents(); // Refresh the list of enrolled students on the right
                    studentIdField.clear(); // Clear selection fields
                    studentNameField.clear();
                    studentListView.getSelectionModel().clearSelection(); // Clear ListView selection
                    return new Pair<>(selectedStudentId, selectedStudentName); // Return data if needed by caller
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm học viên. Có thể học viên đã có trong lớp hoặc có lỗi xảy ra.");
                    return null; // Prevent dialog from closing
                }
            }
            return null; // For Cancel or closing the dialog
        });

        // Initial load of data
        loadAllStudentsForSelection(); // For left panel ListView
        loadEnrolledStudents();      // For right panel TableView

        Platform.runLater(() -> studentListView.requestFocus());
    }

    private VBox setupLeftPanel() {
        VBox leftPanelRoot = new VBox(10);
        leftPanelRoot.setPadding(new Insets(10));
        leftPanelRoot.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: #FAFAFA;");


        // Header for left panel
        Label leftHeaderLabel = new Label("Chọn Học Viên Để Thêm Vào Lớp");
        leftHeaderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        leftHeaderLabel.setTextFill(Color.DARKBLUE);
        leftPanelRoot.getChildren().add(leftHeaderLabel);

        // Input Fields (Lấy ID và Tên học viên được chọn) - Kept from original
        GridPane inputPane = new GridPane();
        inputPane.setHgap(10);
        inputPane.setVgap(10);
        inputPane.setPadding(new Insets(10, 0, 10, 0)); // Adjusted padding

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

        // Student ListView for selection - Kept from original
        Label listLabel = new Label("Danh Sách Tất Cả Học Viên Hiện Có:");
        listLabel.setFont(Font.font("Arial", 16));
        listLabel.setTextFill(Color.DARKGREEN);

        studentListView = new ListView<>();
        studentListView.setPrefHeight(300); // Adjusted height
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
        rightHeaderLabel.setTextFill(Color.rgb(0, 100, 0)); // Màu Dark Green

        enrolledStudentsTableView = new TableView<>();
        enrolledStudentsTableView.setPrefHeight(400);
        VBox.setVgrow(enrolledStudentsTableView, Priority.ALWAYS);
        enrolledStudentsTableView.setPlaceholder(new Label("Chưa có học viên nào trong lớp này."));

        // Các cột thông tin sinh viên
        TableColumn<Student, String> idCol = new TableColumn<>("Mã HV");
        idCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        idCol.setPrefWidth(100);

        TableColumn<Student, String> nameCol = new TableColumn<>("Tên Học Viên");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Student, String> birthdayCol = new TableColumn<>("Ngày Sinh");
        birthdayCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBirthday()));
        birthdayCol.setPrefWidth(120);

        TableColumn<Student, String> contactCol = new TableColumn<>("Số Điện Thoại");
        contactCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getContactNumber()));
        contactCol.setPrefWidth(150);

        // Cột hành động (nút "X")
        TableColumn<Student, Void> actionCol = new TableColumn<>("Xóa");
        actionCol.setPrefWidth(80);
        actionCol.setCellFactory(createDeleteButtonCellFactory());

        // Thêm tất cả các cột vào TableView
        enrolledStudentsTableView.getColumns().addAll(idCol, nameCol, birthdayCol, contactCol, actionCol);

        rightPanelRoot.getChildren().addAll(rightHeaderLabel, enrolledStudentsTableView);
        return rightPanelRoot;
    }

    private Callback<TableColumn<Student, Void>, TableCell<Student, Void>> createDeleteButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Student, Void> call(final TableColumn<Student, Void> param) {
                return new TableCell<>() {
                    private final Button deleteButton = new Button("X");

                    {
                        deleteButton.setStyle("-fx-background-color: #FF4C4C; -fx-text-fill: white; -fx-font-weight: bold;");
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
        };
    }
    private void handleDeleteAction(Student student) {
        // Xác nhận trước khi xóa
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác Nhận Xóa");
        confirmAlert.setHeaderText("Bạn có chắc muốn xóa học viên này khỏi lớp?");
        confirmAlert.setContentText("Học viên: " + student.getName());
        ButtonType confirmButton = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            // Gọi DAO để xóa student khỏi course
            System.out.println("DEBUG: Bắt đầu xóa học viên " + student.getId() + " khỏi khóa học " + courseId);
            boolean success = courseDAO.removeStudentFromCourse(courseId, student.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Xóa Thành Công",
                        "Học viên " + student.getName() + " đã được xóa khỏi lớp.");
                loadEnrolledStudents(); // Refresh danh sách học viên
            } else {
                showAlert(Alert.AlertType.ERROR, "Xóa Thất Bại",
                        "Không thể xóa học viên " + student.getName() + ". Xin thử lại.");
            }
        }
    }

    private void loadAllStudentsForSelection() {
        System.out.println("DEBUG: Bắt đầu phương thức loadAllStudentsForSelection...");
        List<Student> allStudentsInSystem = studentDAO.findAll(); // Lấy tất cả học viên từ hệ thống
        if (allStudentsInSystem == null) {
            allStudentsInSystem = new ArrayList<>(); // Đảm bảo danh sách không null
        }
        System.out.println("DEBUG: Tổng số học viên trong hệ thống: " + allStudentsInSystem.size());
        List<Student> enrolledStudentsInThisCourse = new ArrayList<>();
        if (this.courseId != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Bước 1: Lấy ID của các học viên đã ghi danh trong khóa học hiện tại
                List<String> enrolledStudentIds = courseDAO.getStudentIdsByCourseId(conn, this.courseId);
                System.out.println("DEBUG: Số ID học viên đã ghi danh trong khóa học '" + this.courseId + "': " + enrolledStudentIds.size());
                if (!enrolledStudentIds.isEmpty()) {
                    // Bước 2: Lấy đối tượng Student cho các ID đã ghi danh
                    enrolledStudentsInThisCourse = studentDAO.getStudentsByIds(conn, enrolledStudentIds);
                    System.out.println("DEBUG: Số đối tượng Student đã ghi danh trong khóa học: " + enrolledStudentsInThisCourse.size());
                }
            } catch (SQLException e) {
                System.err.println("LỖI: SQLException khi lấy danh sách học viên đã ghi danh để lọc: " + e.getMessage());
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi Tải Dữ Liệu", "Không thể tải danh sách học viên đã ghi danh để thực hiện việc lọc.");
                // Trong trường hợp lỗi, có thể hiển thị tất cả học viên hoặc danh sách rỗng tùy theo yêu cầu
            }
        } else {
            System.out.println("DEBUG: courseId là null, không thể lọc dựa trên học viên đã ghi danh.");
        }
        // Bước 3: Lọc danh sách allStudentsInSystem để loại bỏ những học viên đã có trong enrolledStudentsInThisCourse
        List<Student> availableStudentsForSelection = new ArrayList<>();
        if (!allStudentsInSystem.isEmpty()) {
            // Tạo một Set chứa ID của các học viên đã ghi danh để việc kiểm tra hiệu quả hơn
            Set<String> enrolledIdsSet = enrolledStudentsInThisCourse.stream()
                    .map(Student::getId)
                    .collect(Collectors.toSet());
            for (Student student : allStudentsInSystem) {
                if (!enrolledIdsSet.contains(student.getId())) {
                    availableStudentsForSelection.add(student); // Chỉ thêm nếu học viên chưa có trong khóa học
                }
            }
        }
        System.out.println("DEBUG: Số học viên có sẵn để lựa chọn (sau khi lọc): " + availableStudentsForSelection.size());
        // Bước 4: Hiển thị danh sách học viên có sẵn lên ListView
        ObservableList<Student> items = FXCollections.observableArrayList(availableStudentsForSelection);
        studentListView.setItems(items);
        // Cập nhật placeholder cho ListView
        if (items.isEmpty()) {
            studentListView.setPlaceholder(new Label("Không có học viên mới để thêm hoặc tất cả đã ở trong lớp."));
        } else {
            // Bạn có thể giữ placeholder mặc định hoặc một thông báo khác
            // studentListView.setPlaceholder(new Label("Chọn học viên từ danh sách."));
        }
        System.out.println("DEBUG: Kết thúc phương thức loadAllStudentsForSelection.");
    }


    private void loadEnrolledStudents() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            List<String> studentIds = courseDAO.getStudentIdsByCourseId(conn, this.courseId);
            List<Student> enrolledStudents = studentDAO.getStudentsByIds(conn, studentIds);

            // Hiển thị dữ liệu lên TableView
            ObservableList<Student> observableList = FXCollections.observableArrayList(enrolledStudents);
            enrolledStudentsTableView.setItems(observableList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách học viên.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(getDialogPane().getScene().getWindow()); // Ensure alert is modal to this dialog
        alert.showAndWait();
    }

}