package view.components.StudentList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import src.dao.StudentDAO;
import src.model.person.Student;

import java.sql.SQLException;
import java.util.Random;
import java.util.regex.Pattern;

public class AddStudentDialog extends Stage {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]*$");

    private TextField idField;
    private TextField nameField;
    private ComboBox<String> genderComboBox;
    private TextField contactField;
    private DatePicker birthdayPicker;

    public AddStudentDialog(Stage parentStage) {
        setTitle("Thêm Học Viên Mới");
        initModality(Modality.WINDOW_MODAL);
        initOwner(parentStage);

        // Layout Setup
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        setupUI(grid);
        setupValidation();

        Scene scene = new Scene(grid, 500, 600);
        setScene(scene);
    }

    private void setupUI(GridPane grid) {
        Label idLabel = new Label("Mã học viên:");
        idField = new TextField();
        idField.setEditable(false);
        idField.setText(generateStudentID());

        Label nameLabel = new Label("Họ tên:");
        nameField = new TextField();

        Label genderLabel = new Label("Giới tính:");
        genderComboBox = new ComboBox<>();
        genderComboBox.getItems().addAll("Nam", "Nữ");

        Label contactLabel = new Label("Số điện thoại:");
        contactField = new TextField();

        Label birthdayLabel = new Label("Ngày sinh:");
        birthdayPicker = new DatePicker();
        Button addButton = new Button("Thêm");
        Button cancelButton = new Button("Hủy");

        grid.addRow(0, idLabel, idField);
        grid.addRow(1, nameLabel, nameField);
        grid.addRow(2, genderLabel, genderComboBox);
        grid.addRow(3, contactLabel, contactField);
        grid.addRow(4, birthdayLabel, birthdayPicker);
        grid.addRow(5, addButton, cancelButton);

        addButton.setOnAction(e -> handleAddStudent());
        cancelButton.setOnAction(e -> close());
    }

    private void setupValidation() {
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateName();
        });

        contactField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validatePhone();
        });


    }

    private String generateStudentID() {
        Random random = new Random();
        return "2" + String.format("%07d", random.nextInt(10000000));
    }

    private boolean validateName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showValidationError(nameField, "Họ tên không được để trống.");
            return false;
        }
        clearValidationError(nameField);
        return true;
    }

    private boolean validatePhone() {
        String contact = contactField.getText().trim();
        if (!PHONE_PATTERN.matcher(contact).matches()) {
            showValidationError(contactField, "Số điện thoại chỉ chứa chữ số và ký tự hợp lệ.");
            return false;
        }
        clearValidationError(contactField);
        return true;
    }

    private void handleAddStudent() {
        boolean isValid = validateName() & validatePhone();
        if (!isValid) {
            showError("Vui lòng kiểm tra lại các trường thông tin chưa hợp lệ.");
            return;
        }
        String studentSpecificId = idField.getText();
        String name = nameField.getText();
        String gender = genderComboBox.getValue();
        String contactNumber = contactField.getText();
        String birthday = birthdayPicker.getValue() != null ? birthdayPicker.getValue().toString() : null;

        // Kiểm tra lại các trường bắt buộc khác (nếu cần, dù đã có validate)
        if (studentSpecificId.isEmpty() || name.isEmpty()) {
            showError("Vui lòng nhập đầy đủ Mã học viên và Họ tên.");
            return;
        }
        Student newStudent = new Student();
        newStudent.setId(studentSpecificId);
        newStudent.setName(name);
        newStudent.setGender(gender);
        newStudent.setContactNumber(contactNumber);
        newStudent.setBirthday(birthday);
        String address = ""; // Hoặc lấy từ một trường nhập liệu địa chỉ nếu có
        try {
            StudentDAO studentDAO = new StudentDAO();
            boolean success = studentDAO.createStudentAndUserTransaction(newStudent, address);
            if (success) {
                showInfo("Thêm học viên và tạo tài khoản người dùng thành công!");
                close();
            } else {
                showError("Không thể thêm học viên. Vui lòng kiểm tra lại thông tin và thử lại.");
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace(); // In đầy đủ stack trace ra console
            showError("Lỗi cơ sở dữ liệu khi thêm học viên: " + sqlEx.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // In đầy đủ stack trace ra console
            showError("Đã xảy ra lỗi không mong muốn: " + e.getMessage());
        }
    }

    private void showValidationError(Control control, String message) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-background-color: lightyellow; -fx-text-fill: red;");
        Tooltip.install(control, tooltip);
    }

    private void clearValidationError(Control control) {
        control.setStyle(null);
        Tooltip.uninstall(control, null);
    }
    private void showInfo(String message) {
        // Hiển thị thông báo thành công dưới dạng AlertDialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showError(String message) {
        // Hiển thị thông báo lỗi dưới dạng AlertDialog
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
