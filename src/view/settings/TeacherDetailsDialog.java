package src.view.settings;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Window;
import src.model.person.Teacher; // Import model Teacher

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
// Removed unused imports: javafx.geometry.Pos, javafx.scene.Node, javafx.scene.layout.HBox,
// java.util.function.Consumer, java.util.function.Supplier

/**
 * Lớp Dialog hiển thị thông tin chi tiết của một giáo viên,
 * cho phép chỉnh sửa một số trường thông tin.
 * Các thay đổi sẽ được áp dụng lên đối tượng Teacher model khi nhấn "Lưu thay đổi".
 */
public class TeacherDetailsDialog extends Dialog<Boolean> { // Trả về Boolean (true nếu có thay đổi, false nếu không)

    // Định dạng ngày tháng cho hiển thị (dd/MM/yyyy) và cho model (yyyy-MM-dd)
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MODEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Teacher teacherModel; // Đối tượng Teacher model gốc để áp dụng thay đổi
    private boolean hasChanges = false; // Cờ để theo dõi xem có thay đổi nào được thực hiện không

    // Input Controls as class members
    private TextField nameField;
    private ComboBox<String> genderComboBox;
    private DatePicker birthdayPicker;
    private TextField contactNumberField;
    private TextField emailField;

    // To display non-editable info from placeholder
    private final SettingsView.TeacherPlaceholder placeholder;

    public TeacherDetailsDialog(Window owner, SettingsView.TeacherPlaceholder placeholder, Teacher teacherModel) {
        this.placeholder = placeholder; // Used for non-editable fields like ID and initial header
        this.teacherModel = teacherModel; // This is the model we will update

        initOwner(owner); // Thiết lập cửa sổ cha
        initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với các cửa sổ khác

        setTitle("Chỉnh sửa Thông tin Giáo viên");
        // Initialize header with the name from the teacherModel, as it's the source for editable fields
        getDialogPane().setHeaderText("Chỉnh sửa chi tiết cho Giáo viên: " + teacherModel.getName());
        getDialogPane().setPrefWidth(600); // Đặt chiều rộng ưu tiên cho dialog
        // Nếu bạn có file CSS riêng cho dialog:
        // getDialogPane().getStylesheets().add(getClass().getResource("/view/css/dialog_styles.css") != null ? getClass().getResource("/view/css/dialog_styles.css").toExternalForm() : "");


        // Tạo layout chính cho dialog
        GridPane grid = new GridPane();
        grid.setHgap(10); // Khoảng cách ngang
        grid.setVgap(12); // Khoảng cách dọc
        grid.setPadding(new Insets(20, 30, 20, 30)); // Đệm xung quanh

        int rowIndex = 0; // Biến đếm dòng

        // --- Các trường thông tin ---

        // Mã GV (User ID) - Không cho phép sửa
        grid.add(createHeaderLabel("Mã GV (User ID):"), 0, rowIndex);
        // Assuming placeholder.getId() is the correct value to display and is non-editable
        grid.add(createValueLabel(placeholder.getId()), 1, rowIndex++);

        // Họ tên - Cho phép sửa
        grid.add(createHeaderLabel("Họ tên:"), 0, rowIndex);
        nameField = new TextField(teacherModel.getName()); // Initialize with current name from model
        nameField.setPrefColumnCount(25);
        grid.add(nameField, 1, rowIndex++);

        // Giới tính - Cho phép sửa (sử dụng ComboBox)
        grid.add(createHeaderLabel("Giới tính:"), 0, rowIndex);
        genderComboBox = new ComboBox<>();
        genderComboBox.getItems().addAll("Nam", "Nữ", "Khác"); // Các lựa chọn giới tính
        if (teacherModel.getGender() != null && !teacherModel.getGender().isEmpty()) {
            genderComboBox.setValue(teacherModel.getGender());
        }
        grid.add(genderComboBox, 1, rowIndex++);

        // Ngày sinh - Cho phép sửa (sử dụng DatePicker)
        grid.add(createHeaderLabel("Ngày sinh:"), 0, rowIndex);
        birthdayPicker = new DatePicker();
        birthdayPicker.setPromptText("dd/MM/yyyy"); // Gợi ý định dạng
        String modelBirthday = teacherModel.getBirthday(); // Lấy chuỗi ngày "yyyy-MM-dd" từ model
        if (modelBirthday != null && !modelBirthday.isEmpty()) {
            try {
                birthdayPicker.setValue(LocalDate.parse(modelBirthday, MODEL_DATE_FORMATTER));
            } catch (DateTimeParseException ex) {
                // Nếu parse lỗi, DatePicker sẽ trống
                System.err.println("Lỗi parse ngày ban đầu từ model cho DatePicker: " + modelBirthday);
            }
        }
        // Sử dụng StringConverter để DatePicker hiển thị và parse theo định dạng "dd/MM/yyyy"
        birthdayPicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            @Override public String toString(LocalDate date) {
                return (date != null) ? date.format(DISPLAY_DATE_FORMATTER) : "";
            }
            @Override public LocalDate fromString(String string) {
                try {
                    return (string != null && !string.isEmpty()) ? LocalDate.parse(string, DISPLAY_DATE_FORMATTER) : null;
                } catch (DateTimeParseException ex) {
                    // Trả về null nếu người dùng nhập sai, việc kiểm tra sẽ thực hiện khi nhấn Lưu
                    return null;
                }
            }
        });
        grid.add(birthdayPicker, 1, rowIndex++);

        // Số điện thoại - Cho phép sửa
        grid.add(createHeaderLabel("Số điện thoại:"), 0, rowIndex);
        contactNumberField = new TextField(teacherModel.getContactNumber());
        contactNumberField.setPrefColumnCount(25);
        grid.add(contactNumberField, 1, rowIndex++);

        // Email - Cho phép sửa
        grid.add(createHeaderLabel("Email:"), 0, rowIndex);
        emailField = new TextField(teacherModel.getEmail());
        emailField.setPrefColumnCount(25);
        grid.add(emailField, 1, rowIndex++);

        // Tên tài khoản - Không cho phép sửa
        grid.add(createHeaderLabel("Tên tài khoản:"), 0, rowIndex);
        // Assuming placeholder.getAccountUsername() is correct for display
        grid.add(createValueLabel(placeholder.getAccountUsername()), 1, rowIndex++);

        // Thiết lập nội dung cho dialog pane
        getDialogPane().setContent(grid);

        // --- Buttons ---
        ButtonType saveButtonType = new ButtonType("Lưu thay đổi", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // Prevent dialog from closing on save if validation fails
        // This needs to be done after the dialog is shown or buttons are part of the pane.
        // A common way is to look up the button after the dialog pane is set.
        final Button btSave = (Button) getDialogPane().lookupButton(saveButtonType);
        btSave.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateAndSaveChanges()) {
                event.consume(); // Stop dialog from closing if validation/save fails
            }
        });

        // Thiết lập bộ chuyển đổi kết quả: trả về 'true' nếu có thay đổi, 'false' nếu không
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // The hasChanges flag is set within validateAndSaveChanges upon success
                return this.hasChanges;
            }
            // For Cancel or if closed via 'X'
            return false;
        });

        // Styling cơ bản cho dialog pane
        getDialogPane().setStyle("-fx-font-family: 'Arial'; -fx-font-size: 13px;");
    }

    // Tạo Label cho tiêu đề trường thông tin
    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        label.setStyle("-fx-text-fill: #2c3e50;"); // Màu chữ đậm hơn
        return label;
    }

    // Tạo Label cho giá trị trường thông tin (cho các trường không chỉnh sửa)
    private Label createValueLabel(String text) {
        Label label = new Label(text != null ? text : "N/A"); // Handle null text just in case
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        label.setStyle("-fx-text-fill: #34495e;"); // Màu chữ dịu hơn
        label.setWrapText(true); // Cho phép xuống dòng nếu text dài
        return label;
    }

    // Method to validate input and save changes to the teacherModel
    private boolean validateAndSaveChanges() {
        // Validate Name (Example: not empty)
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            showAlert("Lỗi đầu vào", "Họ tên không được để trống.");
            return false;
        }

        // Validate Birthday (already handled by DatePicker's converter to some extent)
        // StringConverter will return null if input is invalid.
        LocalDate newBirthdayDate = birthdayPicker.getValue();
        // Check if text was entered that didn't parse to a valid date
        if (newBirthdayDate == null && birthdayPicker.getEditor().getText() != null && !birthdayPicker.getEditor().getText().trim().isEmpty()) {
            showAlert("Lỗi định dạng ngày", "Ngày sinh không hợp lệ. Vui lòng chọn từ lịch hoặc nhập đúng định dạng dd/MM/yyyy.");
            return false;
        }

        // Validate Email (basic pattern, can be more sophisticated)
        String newEmail = emailField.getText().trim();
        // Allow empty email or validate if not empty
        if (!newEmail.isEmpty() && !newEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert("Lỗi đầu vào", "Địa chỉ email không hợp lệ. Vui lòng nhập email đúng định dạng hoặc để trống.");
            return false;
        }

        // --- All validations passed, proceed to update the model ---
        teacherModel.setName(newName);
        teacherModel.setGender(genderComboBox.getValue()); // getValue can be null if nothing selected and no default

        if (newBirthdayDate != null) {
            teacherModel.setBirthday(newBirthdayDate.format(MODEL_DATE_FORMATTER)); // Format to "yyyy-MM-dd"
        } else {
            teacherModel.setBirthday(null); // Allow clearing the birthday
        }
        teacherModel.setContactNumber(contactNumberField.getText().trim());
        teacherModel.setEmail(newEmail);

        this.hasChanges = true; // Mark that changes were successfully made
        // Update dialog header with new name if it changed
        getDialogPane().setHeaderText("Chi tiết cho Giáo viên: " + teacherModel.getName());
        return true; // Indicate success
    }

    // Hiển thị thông báo lỗi
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(getDialogPane().getScene().getWindow()); // Đảm bảo alert thuộc về dialog hiện tại
        alert.showAndWait();
    }
}