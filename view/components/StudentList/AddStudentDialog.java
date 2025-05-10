package view.components.StudentList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Random;
import java.util.regex.Pattern;

public class AddStudentDialog extends Stage {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]*$");

    private TextField idField;
    private TextField nameField;
    private ComboBox<String> genderComboBox;
    private TextField contactField;
    private DatePicker birthdayPicker;
    private TextField emailField;

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

        Label emailLabel = new Label("Email:");
        emailField = new TextField();

        Button addButton = new Button("Thêm");
        Button cancelButton = new Button("Hủy");

        grid.addRow(0, idLabel, idField);
        grid.addRow(1, nameLabel, nameField);
        grid.addRow(2, genderLabel, genderComboBox);
        grid.addRow(3, contactLabel, contactField);
        grid.addRow(4, birthdayLabel, birthdayPicker);
        grid.addRow(5, emailLabel, emailField);
        grid.addRow(6, addButton, cancelButton);

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

        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateEmail();
        });
    }

    private String generateStudentID() {
        Random random = new Random();
        return "1" + String.format("%07d", random.nextInt(10000000));
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

    private boolean validateEmail() {
        String email = emailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showValidationError(emailField, "Email không đúng định dạng.");
            return false;
        }
        clearValidationError(emailField);
        return true;
    }

    private void handleAddStudent() {
        boolean isValid = validateName() & validatePhone() & validateEmail();
        if (!isValid) return;

        // Code to save student...
        close();
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
}
