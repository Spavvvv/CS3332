
package src.view.StudentList;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*; // Control, Tooltip, Alert, etc.
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import src.dao.Person.StudentDAO;
import src.model.Notification.NotificationService;
import src.model.person.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;
import java.util.regex.Pattern;

public class AddStudentDialog extends Stage {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]*$");

    private TextField idField;
    private TextField nameField;
    private ComboBox<String> genderComboBox;
    private TextField contactField;
    private TextField parentNameField;
    private TextField parentPhoneField;
    private DatePicker birthdayPicker;
    private NotificationService notificationService;
    private String currentUserId;
    public AddStudentDialog(Stage parentStage, NotificationService notificationService, String currentUserId) {
        // GÁN GIÁ TRỊ CHO BIẾN THÀNH VIÊN MỚI
        this.notificationService = notificationService;
        this.currentUserId = currentUserId;
        // KẾT THÚC PHẦN THAY ĐỔI
        setTitle("Thêm Học Viên Mới");
        initModality(Modality.WINDOW_MODAL);
        initOwner(parentStage);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);
        setupUI(grid);
        setupValidation();
        Scene scene = new Scene(grid, 500, 350);
        setScene(scene);
    }

    private void setupUI(GridPane grid) {
        Label idLabel = new Label("Mã học viên:");
        idField = new TextField();
        idField.setEditable(false);
        idField.setText(generateStudentID());

        Label nameLabel = new Label("Họ tên:");
        nameField = new TextField();
        nameField.setPromptText("Nhập họ tên học viên");

        Label genderLabel = new Label("Giới tính:");
        genderComboBox = new ComboBox<>();
        genderComboBox.getItems().addAll("Nam", "Nữ");
        genderComboBox.setPromptText("Chọn giới tính");

        Label contactLabel = new Label("Số điện thoại:");
        contactField = new TextField();
        contactField.setPromptText("Nhập số điện thoại");

        Label birthdayLabel = new Label("Ngày sinh:");
        birthdayPicker = new DatePicker();
        birthdayPicker.setPromptText("Chọn ngày sinh");

        Label parentNameLabel = new Label("Tên phụ huynh:");
        parentNameField = new TextField();
        parentNameField.setPromptText("Nhập tên phụ huynh");

        // Thêm vào trong phương thức setupUI
        Label parentPhoneLabel = new Label("SĐT phụ huynh:");
        parentPhoneField = new TextField();
        parentPhoneField.setPromptText("Nhập SĐT phụ huynh");

        Button addButton = new Button("Thêm");
        Button cancelButton = new Button("Hủy");

        grid.add(idLabel, 0, 0);
        grid.add(idField, 1, 0, 2, 1);

        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1, 2, 1);

        grid.add(genderLabel, 0, 2);
        grid.add(genderComboBox, 1, 2, 2, 1);

        grid.add(contactLabel, 0, 3);
        grid.add(contactField, 1, 3, 2, 1);

        grid.add(parentNameLabel, 0, 4);
        grid.add(parentNameField, 1, 4, 2, 1);
        grid.add(parentPhoneLabel, 0, 5);
        grid.add(parentPhoneField, 1, 5, 2, 1);

        grid.add(birthdayLabel, 0, 6);
        grid.add(birthdayPicker, 1, 6, 2, 1);

        GridPane buttonPane = new GridPane();
        buttonPane.setHgap(10);
        buttonPane.add(addButton, 0, 0);
        buttonPane.add(cancelButton, 1, 0);
        grid.add(buttonPane, 1, 7, 2, 1);

        addButton.setOnAction(e -> handleAddStudent());
        cancelButton.setOnAction(e -> close());
    }

    private void setupValidation() {
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateName(nameField);
            }
        });

        contactField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePhone(contactField);
            }
        });
        genderComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateGender();
            }
        });
        birthdayPicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if(!newVal) {
                validateBirthday();
            }
        });
        parentNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Khi focus mất đi
                validateName(parentNameField);
            }
        });
        parentPhoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Khi focus mất đi
                validatePhone(parentPhoneField);
            }
        });
    }

    private String generateStudentID() {
        Random random = new Random();
        return "2" + String.format("%07d", random.nextInt(10000000));
    }

    private boolean validateName(TextField NameField) {
        String name = NameField.getText().trim();
        if (name.isEmpty()) {
            showValidationError(NameField, "Họ tên không được để trống.");
            return false;
        }
        clearValidationError(NameField);
        return true;
    }

    private boolean validatePhone(TextField PhoneField) {
        String contact = PhoneField.getText().trim();
        if (contact.isEmpty()) {
            showValidationError(PhoneField, "Số điện thoại không được để trống.");
            return false;
        }
        if (!PHONE_PATTERN.matcher(contact).matches()) {
            showValidationError(contactField, "Số điện thoại không hợp lệ. Chỉ chấp nhận số, dấu cách, (, ), -, +.");
            return false;
        }
        clearValidationError(contactField);
        return true;
    }

    private boolean validateGender() {
        if (genderComboBox.getValue() == null) {
            showValidationError(genderComboBox, "Vui lòng chọn giới tính.");
            return false;
        }
        clearValidationError(genderComboBox);
        return true;
    }

    private boolean validateBirthday() {
        LocalDate birthday = birthdayPicker.getValue();
        if (birthday == null) {
            showValidationError(birthdayPicker, "Vui lòng chọn ngày sinh.");
            return false;
        }
        if (birthday.isAfter(LocalDate.now().minusYears(3))) { // Ví dụ: tuổi tối thiểu là 3
            showValidationError(birthdayPicker, "Ngày sinh không hợp lệ (quá nhỏ tuổi hoặc ở tương lai).");
            return false;
        }
        clearValidationError(birthdayPicker);
        return true;
    }

    private void handleAddStudent() {
        boolean isNameValid = validateName(nameField);
        boolean isPhoneValid = validatePhone(contactField);
        boolean isParentNameValid = validateName(parentNameField);
        boolean isParentPhoneValid = validatePhone(parentPhoneField);
        boolean isGenderValid = validateGender();
        boolean isBirthdayValid = validateBirthday();
        if (!isNameValid || !isPhoneValid || !isGenderValid || !isBirthdayValid || !isParentNameValid || !isParentPhoneValid) {
            showError("Vui lòng kiểm tra lại các trường thông tin chưa hợp lệ.");
            return;
        }
        String studentSpecificId = idField.getText();
        String name = nameField.getText().trim();
        String gender = genderComboBox.getValue();
        String contactNumber = contactField.getText().trim();
        LocalDate birthdayLocalDate = birthdayPicker.getValue();
        String birthdayString = (birthdayLocalDate != null) ? birthdayLocalDate.toString() : null;
        String parentName = parentNameField.getText().trim();
        String parentPhone = parentPhoneField.getText().trim();
        Student newStudent = new Student();
        newStudent.setId(studentSpecificId);
        newStudent.setName(name);
        newStudent.setGender(gender);
        newStudent.setContactNumber(contactNumber);
        newStudent.setBirthday(birthdayString);
        newStudent.setParentName(parentName);
        newStudent.setParentPhoneNumber(parentPhone);
        String address = ""; // Default
        try {
            StudentDAO studentDAO = new StudentDAO();
            boolean success = studentDAO.createStudentAndUserTransaction(newStudent, address);
            if (success) {
                showInfo("Thêm học viên và tạo tài khoản người dùng thành công!");
                // **BẮT ĐẦU LOGIC GỬI THÔNG BÁO ĐƯỢC THÊM VÀO**
                if (this.notificationService != null && this.currentUserId != null && !this.currentUserId.isEmpty()) {
                    String studentName = newStudent.getName(); // Lấy tên sinh viên vừa tạo
                    String studentId = newStudent.getId();   // Lấy ID sinh viên vừa tạo

                    // Tạo nội dung thông báo
                    String notificationMessage = String.format(
                            "Người dùng '%s' đã tạo học viên mới. Tên: %s, ID: %s.",
                            this.currentUserId, // ID của người tạo (Admin/Teacher)
                            studentName,       // Tên học viên mới
                            studentId          // ID học viên mới
                    );

                    // Gửi thông báo đến tất cả Admin
                    this.notificationService.sendNotificationToAdmins(notificationMessage, this.currentUserId);
                    System.out.println("Đã gửi thông báo tạo học viên mới tới Admins."); // Log để kiểm tra
                } else {
                    // Ghi log hoặc xử lý trường hợp notificationService hoặc currentUserId không được cung cấp
                    System.err.println("Không thể gửi thông báo: NotificationService hoặc currentUserId chưa được thiết lập trong AddStudentDialog.");
                    // Cân nhắc: Nếu muốn, bạn có thể gửi thông báo với senderId mặc định là "SYSTEM"
                    if (this.notificationService != null) {
                        String studentName = newStudent.getName();
                        String studentId = newStudent.getId();
                        String notificationMessage = String.format(
                                "Một học viên mới đã được tạo. Tên: %s, ID: %s. (Người tạo không xác định).",
                                studentName,
                                studentId
                        );
                        this.notificationService.sendNotificationToAdmins(notificationMessage, "SYSTEM");
                    }
                }
                // **KẾT THÚC LOGIC GỬI THÔNG BÁO**
                close(); // Đóng dialog sau khi thêm thành công và gửi thông báo
            } else {
                showError("Không thể thêm học viên. Vui lòng kiểm tra lại thông tin và thử lại.");
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
            showError("Lỗi cơ sở dữ liệu khi thêm học viên: " + sqlEx.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Đã xảy ra lỗi không mong muốn: " + e.getMessage());
        }
    }

    private void showValidationError(Control control, String message) {
        control.setStyle("-fx-border-color: red; -fx-border-width: 1.5;");
        Tooltip tooltip = new Tooltip(message);
        // Optional: style the tooltip
        // tooltip.setStyle("-fx-background-color: lightyellow; -fx-text-fill: red;");
        Tooltip.install(control, tooltip);
        // control.requestFocus(); // Cân nhắc nếu muốn hành vi này
    }

    private void clearValidationError(Control control) {
        control.setStyle(null);
        // SỬA LỖI Ở ĐÂY: Sử dụng control.getTooltip() thay vì Tooltip.getTooltip(control)
        Tooltip.uninstall(control, control.getTooltip());
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(this.getOwner());
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(this.getOwner());
        alert.showAndWait();
    }
}

