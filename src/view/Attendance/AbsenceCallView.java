package src.view.Attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell; // Giữ lại nếu cột 'called' dùng
import javafx.scene.control.cell.ComboBoxTableCell; // Giữ lại cho cột 'status'
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell; // Giữ lại cho cột 'note'
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter; // Giữ lại cho status ComboBoxTableCell
import src.controller.Attendance.AbsenceCallController;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.view.components.Screen.BaseScreenView;
import src.controller.NavigationController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.logging.Logger;

public class AbsenceCallView extends BaseScreenView {

    private static final Logger LOGGER = Logger.getLogger(AbsenceCallView.class.getName());
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String BACKGROUND_COLOR = "#f5f5f5";
    private static final String HOMEWORK_BUTTON_COLOR = "#7e57c2";

    private Label titleLabel;
    private ComboBox<ClassSession> sessionSelector;
    private DatePicker fromDatePicker; // Sẽ được cập nhật và không cho người dùng sửa
    private Label currentSessionDayLabel;
    // private Button filterButton; // ĐÃ BỎ
    private Button returnButton;
    private Button homeworkButton;
    private Button saveButton; // Giữ lại
    private TableView<AbsenceRecord> tableView;

    private AbsenceCallController controller;

    private final ObservableList<String> statusOptions =
            FXCollections.observableArrayList("Chưa điểm danh", "Có mặt", "Vắng", "Có phép");

    public AbsenceCallView() {
        super("Gọi điện xác nhận", "absence-call-src.view");
        this.controller = new AbsenceCallController(this);
    }

    @Override
    public void initializeView() {
        root.setSpacing(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        HBox titleBar = createTitleBar();
        HBox filterBar = createFilterBar();
        this.tableView = createTableView(); // TableView vẫn editable

        root.getChildren().addAll(titleBar, filterBar, this.tableView);
        setupEventHandlers();

        if (this.controller != null) {
            this.controller.initializeController();
        } else {
            LOGGER.severe("AbsenceCallView: Controller is null during initializeView.");
            if (saveButton != null) saveButton.setDisable(true);
            // filterButton đã bị xóa
            if (sessionSelector != null) sessionSelector.setDisable(true);
        }
        updateCurrentSessionDayLabel();
    }

    @Override
    public void refreshView() {
        super.refreshView();
        if (controller != null) {
            controller.refreshData();
        }
        if (tableView != null) {
            tableView.refresh();
        }
        updateCurrentSessionDayLabel();
    }

    public void refreshTable() {
        if (tableView != null) {
            tableView.refresh();
        }
        updateCurrentSessionDayLabel();
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;");

        titleLabel = new Label("Gọi điện xác nhận");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        returnButton = new Button("← Quay lại");
        styleButton(returnButton, PRIMARY_COLOR);
        homeworkButton = new Button("Bài tập");
        styleButton(homeworkButton, HOMEWORK_BUTTON_COLOR);
        saveButton = new Button("✓ Lưu thông tin"); // Giữ lại saveButton
        styleButton(saveButton, "#4fc3f7"); // Màu của saveButton

        HBox buttonsBox = new HBox(10, returnButton, homeworkButton, saveButton); // Giữ lại saveButton
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        titleBar.getChildren().addAll(titleLabel, spacer, buttonsBox);
        return titleBar;
    }

    private HBox createFilterBar() {
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );

        Label sessionLabel = new Label("Buổi học:");
        sessionLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        sessionSelector = new ComboBox<>();
        sessionSelector.setPromptText("Chọn buổi học");
        sessionSelector.setPrefWidth(300);

        sessionSelector.setCellFactory(lv -> new ListCell<ClassSession>() {
            @Override
            protected void updateItem(ClassSession session, boolean empty) {
                super.updateItem(session, empty);
                if (empty || session == null) {
                    setText(null);
                } else {
                    String courseName = session.getCourseName() != null ? session.getCourseName() : "N/A";
                    String sessionDateStr = session.getDate() != null ? session.getDate().format(DateTimeFormatter.ofPattern("dd/MM")) : "N/A";
                    setText(String.format("%s - Buổi %d (%s)", courseName, session.getSessionNumber(), sessionDateStr));
                }
            }
        });

        sessionSelector.setConverter(new StringConverter<ClassSession>() {
            @Override
            public String toString(ClassSession session) {
                if (session == null) return null;
                String courseName = session.getCourseName() != null ? session.getCourseName() : "N/A";
                String sessionDateStr = session.getDate() != null ? session.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
                return String.format("%s - Buổi %d (%s)", courseName, session.getSessionNumber(), sessionDateStr);
            }
            @Override
            public ClassSession fromString(String string) { return null; }
        });


        Label dateDisplayLabel = new Label("Ngày được chọn:"); // Label cho DatePicker hiển thị
        dateDisplayLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        fromDatePicker = new DatePicker();
        fromDatePicker.setPrefWidth(150);
        fromDatePicker.setEditable(false); // SỬA ĐỔI: Không cho phép nhập liệu trực tiếp
        // fromDatePicker.setDisable(true); // Không disable hoàn toàn để vẫn thấy được màu sắc, chỉ là không cho user click mở calendar
        fromDatePicker.setMouseTransparent(true); // Ngăn sự kiện chuột để không mở popup
        fromDatePicker.setFocusTraversable(false); // Không cho focus bằng bàn phím

        // filterButton ĐÃ BỎ

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES); // Có thể dùng SOMETIMES hoặc ALWAYS tùy ý

        currentSessionDayLabel = new Label("Ngày học: (chưa chọn buổi)");
        currentSessionDayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        currentSessionDayLabel.setPadding(new Insets(0,10,0,10));

        // Bỏ filterButton khỏi filterBar
        filterBar.getChildren().addAll(sessionLabel, sessionSelector, dateDisplayLabel, fromDatePicker, spacer, currentSessionDayLabel);
        return filterBar;
    }

    private TableView<AbsenceRecord> createTableView() {
        TableView<AbsenceRecord> tv = new TableView<>();
        tv.setEditable(true); // TableView VẪN EDITABLE
        VBox.setVgrow(tv, Priority.ALWAYS);
        tv.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 5;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AbsenceRecord, Integer> idCol = new TableColumn<>();
        setBlackHeaderText(idCol, "STT");
        idCol.setCellValueFactory(new PropertyValueFactory<>("displayId"));
        idCol.setPrefWidth(50);
        idCol.setSortable(false);
        idCol.setEditable(false); // Cột STT không sửa

        TableColumn<AbsenceRecord, String> nameCol = new TableColumn<>();
        setBlackHeaderText(nameCol,"Họ và tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        nameCol.setPrefWidth(200);
        nameCol.setEditable(false); // Tên không sửa

        TableColumn<AbsenceRecord, String> classCol = new TableColumn<>();
        setBlackHeaderText(classCol,"Lớp học");
        classCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classCol.setPrefWidth(150);
        classCol.setEditable(false); // Lớp không sửa

        TableColumn<AbsenceRecord, String> dateCol = new TableColumn<>();
        setBlackHeaderText(dateCol, "Ngày nghỉ");
        dateCol.setCellValueFactory(cellData -> cellData.getValue().absenceDateFormattedProperty());
        dateCol.setPrefWidth(100);
        dateCol.setEditable(false); // Ngày không sửa

        // Cột status VẪN EDITABLE
        TableColumn<AbsenceRecord, String> statusCol = new TableColumn<>();
        setBlackHeaderText(statusCol, "Tình trạng");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(), statusOptions));
        statusCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setStatus(event.getNewValue());
            if ("Có phép".equalsIgnoreCase(event.getNewValue())) {
                record.setApproved(true);
            } else if (!"Vắng".equalsIgnoreCase(event.getNewValue()) && !"Chưa điểm danh".equalsIgnoreCase(event.getNewValue())) {
                if (!"Vắng".equalsIgnoreCase(event.getNewValue())) {
                    record.setApproved(false);
                }
            }
            if (controller != null) {
                controller.setDataChanged(true);
            }
            tableView.refresh(); // Cập nhật lại dòng để có thể thấy thay đổi (nếu có binding)
        });
        statusCol.setPrefWidth(120);
        statusCol.setEditable(true); // CHO PHÉP SỬA

        // Cột note VẪN EDITABLE
        TableColumn<AbsenceRecord, String> noteCol = new TableColumn<>();
        setBlackHeaderText(noteCol, "Ghi chú");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setCellFactory(TextFieldTableCell.forTableColumn());
        noteCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setNote(event.getNewValue());
            if (controller != null) controller.setDataChanged(true);
        });
        noteCol.setPrefWidth(200);
        noteCol.setEditable(true); // CHO PHÉP SỬA

        // Cột called VẪN EDITABLE
        TableColumn<AbsenceRecord, Boolean> calledCol = new TableColumn<>();
        setBlackHeaderText(calledCol, "Đã gọi");
        calledCol.setCellValueFactory(new PropertyValueFactory<>("called"));
        calledCol.setCellFactory(CheckBoxTableCell.forTableColumn(calledCol));
        // setOnEditCommit không cần thiết cho CheckBoxTableCell vì nó tự cập nhật model
        // Nếu muốn xử lý thêm sau khi thay đổi, bạn có thể lắng nghe property của record.calledProperty()
        // Hoặc đơn giản là đánh dấu dataChanged:
        calledCol.addEventHandler(TableColumn.CellEditEvent.ANY, event -> {
            if (controller != null) controller.setDataChanged(true);
        });
        calledCol.setPrefWidth(100);
        calledCol.setEditable(true); // CHO PHÉP SỬA

        tv.getColumns().addAll(idCol, nameCol, classCol, dateCol, statusCol, noteCol, calledCol);
        return tv;
    }

    private void setupEventHandlers() {
        NavigationController navController = super.getNavigationController();

        if (navController != null) {
            returnButton.setOnAction(e -> navController.goBack());
            homeworkButton.setOnAction(e ->
                    navController.navigateTo("classroom-attendance-src.view")); // Đảm bảo ID view này đúng
        } else {
            LOGGER.warning("AbsenceCallView: NavigationController from BaseScreenView is null. Navigation buttons will be disabled.");
            if (returnButton != null) returnButton.setDisable(true);
            if (homeworkButton != null) homeworkButton.setDisable(true);
        }

        if (this.controller != null) {
            // saveButton vẫn được giữ lại và gán sự kiện
            if (saveButton != null) {
                saveButton.setOnAction(e -> controller.handleSaveChanges());
            }

            // filterButton đã bị xóa, không còn event handler cho nó nữa

            sessionSelector.setOnAction(e -> {
                ClassSession selectedSession = sessionSelector.getValue();
                if (selectedSession != null && selectedSession.getDate() != null) {
                    fromDatePicker.setValue(selectedSession.getDate()); // Cập nhật DatePicker
                } else {
                    fromDatePicker.setValue(null); // Xóa ngày nếu không có session nào được chọn
                }
                controller.applyFilters(); // Vẫn gọi applyFilters để tải dữ liệu theo session mới
                updateCurrentSessionDayLabel();
            });

            // fromDatePicker giờ chỉ để hiển thị, không cần setOnAction cho nó nữa
            // vì người dùng không tương tác trực tiếp để thay đổi giá trị của nó.
            // fromDatePicker.setOnAction(e -> { ... }); // BỎ HOẶC COMMENT LẠI DÒNG NÀY

        } else {
            LOGGER.severe("AbsenceCallView: Controller is null during setupEventHandlers.");
            if (saveButton != null) saveButton.setDisable(true);
            if (sessionSelector != null) sessionSelector.setDisable(true);
        }
    }

    public void setSessionItems(ObservableList<ClassSession> sessions) {
        if (sessionSelector != null) {
            sessionSelector.setItems(sessions);
            // Tự động chọn session đầu tiên nếu có và cập nhật DatePicker
            if (sessions != null && !sessions.isEmpty()) {
                ClassSession firstSession = sessions.get(0);
                sessionSelector.setValue(firstSession); // Trigger onAction của sessionSelector
                if (firstSession.getDate() != null) {
                    fromDatePicker.setValue(firstSession.getDate());
                }
            } else {
                fromDatePicker.setValue(null);
            }
        }
        updateCurrentSessionDayLabel();
    }

    public void setAbsenceTableItems(ObservableList<AbsenceRecord> records) {
        if (tableView != null) {
            tableView.setItems(records);
            if (records == null || records.isEmpty()){
                tableView.setPlaceholder(new Label("Không có dữ liệu điểm danh cho buổi học này."));
            }
        } else {
            LOGGER.warning("AbsenceCallView: tableView is null when trying to set items.");
        }
    }

    public ClassSession getSelectedSession() {
        return sessionSelector != null ? sessionSelector.getSelectionModel().getSelectedItem() : null;
    }

    // getFromDate() không còn ý nghĩa là một filter chủ động từ người dùng nữa,
    // nhưng controller có thể vẫn dùng nó để biết ngày của session đang được chọn.
    public LocalDate getFromDate() {
        // Trả về giá trị của fromDatePicker, giá trị này được set bởi sessionSelector
        return fromDatePicker != null ? fromDatePicker.getValue() : null;
    }

    private void updateCurrentSessionDayLabel() {
        if (currentSessionDayLabel == null) return;

        ClassSession selectedSession = getSelectedSession();
        LocalDate dateToDisplay = null;

        if (selectedSession != null && selectedSession.getDate() != null) {
            dateToDisplay = selectedSession.getDate();
        }
        // Không lấy từ fromDatePicker nữa vì fromDatePicker giờ là hiển thị theo session

        if (dateToDisplay != null) {
            String dayOfWeek = dateToDisplay.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi", "VN"));
            DateTimeFormatter uiDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            currentSessionDayLabel.setText("Ngày học: " + dayOfWeek + " (" + dateToDisplay.format(uiDateFormatter) + ")");
        } else {
            currentSessionDayLabel.setText("Ngày học: (chưa chọn buổi)");
        }
    }

    private void styleButton(Button button, String backgroundColor) { styleButton(button, backgroundColor, "10 20"); }
    private void styleButton(Button button, String backgroundColor, String padding) {
        if (button == null) return;
        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: " + padding + ";" +
                        "-fx-background-radius: 20;" + // Giữ bo tròn
                        "-fx-cursor: hand;" + // Thêm cursor hand
                        "-fx-alignment: CENTER;"
        );
        button.setOnMouseEntered(e -> button.setOpacity(0.9));
        button.setOnMouseExited(e -> button.setOpacity(1.0));
    }

    public void showInfo(String message) { showAlert(Alert.AlertType.INFORMATION, "Thông báo", message); }
    public void showWarning(String message) { showAlert(Alert.AlertType.WARNING, "Cảnh báo", message); }
    public void showError(String message) { showAlert(Alert.AlertType.ERROR, "Lỗi", message); }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root != null && root.getScene() != null) { // Đảm bảo root và scene tồn tại
            alert.initOwner(root.getScene().getWindow());
        }
        alert.showAndWait();
    }

    public void selectSession(ClassSession session) {
        if (sessionSelector != null) {
            sessionSelector.getSelectionModel().select(session);
            // Event onAction của sessionSelector sẽ tự động cập nhật fromDatePicker và label
        }
        // updateCurrentSessionDayLabel(); // Không cần gọi ở đây nữa vì onAction của selector đã gọi
    }

    private void setBlackHeaderText(TableColumn<AbsenceRecord, ?> column, String title) {
        Label label = new Label(title);
        label.setTextFill(Color.BLACK); // Giữ màu đen cho header
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setAlignment(Pos.CENTER_LEFT); // Căn trái cho dễ đọc hơn
        label.setMaxWidth(Double.MAX_VALUE);
        HBox headerBox = new HBox(label); // Bọc trong HBox để có thể kiểm soát alignment tốt hơn nếu cần
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(5)); // Thêm padding cho header
        column.setGraphic(headerBox);
        column.setText(""); // Xóa text mặc định của header column
    }
}