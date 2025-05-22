
package src.view.Attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;
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
    private DatePicker fromDatePicker;
    private Label currentSessionDayLabel;
    private Button filterButton;
    private Button returnButton;
    private Button homeworkButton;
    private Button saveButton;
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
        this.tableView = createTableView();

        root.getChildren().addAll(titleBar, filterBar, this.tableView);
        setupEventHandlers();

        if (this.controller != null) {
            this.controller.initializeController();
        } else {
            LOGGER.severe("AbsenceCallView: Controller is null during initializeView. UI will not function correctly.");
            if (saveButton != null) saveButton.setDisable(true);
            if (filterButton != null) filterButton.setDisable(true);
            if (sessionSelector != null) sessionSelector.setDisable(true);
        }
        updateCurrentSessionDayLabel(); // Initial update
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
        saveButton = new Button("✓ Lưu thông tin");
        styleButton(saveButton, "#4fc3f7");

        HBox buttonsBox = new HBox(10, returnButton, homeworkButton, saveButton);
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
                setText("Buổi " + String.valueOf(empty || session == null ? null : session.getSessionNumber()));
            }
        });

        sessionSelector.setConverter(new StringConverter<ClassSession>() {
            @Override
            public String toString(ClassSession session) {
                return session == null ? null : "Buổi " + session.getSessionNumber();
            }

            @Override
            public ClassSession fromString(String string) {
                return null;
            }
        });


        Label fromLabel = new Label("Từ ngày:");
        fromLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");
        fromDatePicker = new DatePicker(LocalDate.now());
        fromDatePicker.setPrefWidth(150);

        filterButton = new Button("Lọc");
        styleButton(filterButton, PRIMARY_COLOR, "8 15");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        currentSessionDayLabel = new Label("Ngày học: (chưa chọn buổi)");
        currentSessionDayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PRIMARY_COLOR + ";");
        currentSessionDayLabel.setPadding(new Insets(0,10,0,10));

        filterBar.getChildren().addAll(sessionLabel, sessionSelector, fromLabel, fromDatePicker, filterButton, spacer, currentSessionDayLabel);
        return filterBar;
    }

    private TableView<AbsenceRecord> createTableView() {
        TableView<AbsenceRecord> tv = new TableView<>();
        tv.setEditable(true); // Ensure table is editable
        VBox.setVgrow(tv, Priority.ALWAYS);
        tv.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 5;");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Uncomment if you want columns to fill width

        TableColumn<AbsenceRecord, Integer> idCol = new TableColumn<>();
        setBlackHeaderText(idCol, "STT");
        idCol.setCellValueFactory(new PropertyValueFactory<>("displayId"));
        idCol.setPrefWidth(50);
        idCol.setSortable(false);
        idCol.setEditable(false);

        TableColumn<AbsenceRecord, String> nameCol = new TableColumn<>();
        setBlackHeaderText(nameCol, "Họ và tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        nameCol.setPrefWidth(200);
        nameCol.setEditable(false);

        TableColumn<AbsenceRecord, String> classCol = new TableColumn<>();
        setBlackHeaderText(classCol,"Lớp học");
        classCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classCol.setPrefWidth(150);
        classCol.setEditable(false);

        TableColumn<AbsenceRecord, String> dateCol = new TableColumn<>();
        setBlackHeaderText(dateCol, "Ngày nghỉ");
        dateCol.setCellValueFactory(cellData -> cellData.getValue().absenceDateFormattedProperty());
        dateCol.setPrefWidth(100);
        dateCol.setEditable(false);

        TableColumn<AbsenceRecord, String> statusCol = new TableColumn<>();
        setBlackHeaderText(statusCol, "Tình trạng");
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(), statusOptions));
        statusCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setStatus(event.getNewValue());
            // Automatically update 'approved' based on 'status'
            if ("Có phép".equalsIgnoreCase(event.getNewValue())) {
                record.setApproved(true);
            } else if (!"Vắng".equalsIgnoreCase(event.getNewValue()) && !"Chưa điểm danh".equalsIgnoreCase(event.getNewValue())) {
                // If status is "Có mặt" or other non-absence status (excluding "Vắng" explicitly),
                // then it shouldn't be marked as 'approved for absence'.
                // However, an explicit 'approved' checkbox might still be desired in some UIs.
                // For now, let's assume "Có phép" is the primary way to set 'approved' to true.
                // If status is not "Có phép" and not "Vắng", set approved to false.
                // This logic might need refinement based on how `approved` is truly used.
                // If `approved` is ONLY for "Vắng có phép", then this is okay.
                // If `approved` could be true even if status is "Có mặt" (which is unlikely), this needs change.
                if (!"Vắng".equalsIgnoreCase(event.getNewValue())) {
                    record.setApproved(false);
                }
            }


            if (controller != null) {
                controller.setDataChanged(true);
            }
            tableView.refresh();
        });
        statusCol.setPrefWidth(120);
        statusCol.setEditable(true);


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
        noteCol.setEditable(true);

        TableColumn<AbsenceRecord, Boolean> calledCol = new TableColumn<>();
        setBlackHeaderText(calledCol, "Đã gọi");
        calledCol.setCellValueFactory(new PropertyValueFactory<>("called"));
        calledCol.setCellFactory(CheckBoxTableCell.forTableColumn(calledCol));
        calledCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setCalled(event.getNewValue());
            if (controller != null) controller.setDataChanged(true);
        });
        calledCol.setPrefWidth(100);
        calledCol.setEditable(true);

        // Thêm các cột vào TableView, không bao gồm approvedCol
        tv.getColumns().addAll(idCol, nameCol, classCol, dateCol, statusCol, noteCol, calledCol);
        return tv;
    }

    private void setupEventHandlers() {
        NavigationController navController = super.getNavigationController();

        if (navController != null) {
            returnButton.setOnAction(e -> navController.goBack());
            homeworkButton.setOnAction(e ->
                    //mainController.setSessionDetail();
                    navController.navigateTo("classroom-attendance-src.view"));
        } else {
            LOGGER.warning("AbsenceCallView: NavigationController from BaseScreenView is null. Navigation buttons will be disabled.");
            if (returnButton != null) returnButton.setDisable(true);
            if (homeworkButton != null) homeworkButton.setDisable(true);
        }

        if (this.controller != null) {
            saveButton.setOnAction(e -> controller.handleSaveChanges());
            filterButton.setOnAction(e -> controller.applyFilters());
            sessionSelector.setOnAction(e -> {
                controller.applyFilters();
                updateCurrentSessionDayLabel();
            });
            fromDatePicker.setOnAction(e -> {
                // Chỉ cập nhật label, không tự động lọc khi chỉ thay đổi ngày
                updateCurrentSessionDayLabel();
                // Nếu bạn muốn lọc ngay khi ngày thay đổi, hãy gọi controller.applyFilters() ở đây
                controller.applyFilters();
            });
        } else {
            LOGGER.severe("AbsenceCallView: Controller is null during setupEventHandlers. Data-related actions will not be wired.");
            if (saveButton != null) saveButton.setDisable(true);
            if (filterButton != null) filterButton.setDisable(true);
            if (sessionSelector != null) sessionSelector.setDisable(true);
        }
    }

    public void setSessionItems(ObservableList<ClassSession> sessions) {
        if (sessionSelector != null) {
            sessionSelector.setItems(sessions);
        }
        updateCurrentSessionDayLabel();
    }

    public void setAbsenceTableItems(ObservableList<AbsenceRecord> records) {
        if (tableView != null) {
            tableView.setItems(records);
        } else {
            LOGGER.warning("AbsenceCallView: tableView is null when trying to set items.");
        }
    }

    public ClassSession getSelectedSession() {
        return sessionSelector != null ? sessionSelector.getSelectionModel().getSelectedItem() : null;
    }

    public LocalDate getFromDate() {
        return fromDatePicker != null ? fromDatePicker.getValue() : null;
    }

    private void updateCurrentSessionDayLabel() {
        if (currentSessionDayLabel == null) return;

        ClassSession selectedSession = getSelectedSession();
        LocalDate dateToDisplay = null;
        String context = "(chưa chọn buổi)";

        if (selectedSession != null && selectedSession.getDate() != null) {
            dateToDisplay = selectedSession.getDate();
            context = ""; // Context rỗng khi có buổi học được chọn
        }
        // Trường hợp không có session được chọn nhưng fromDatePicker có giá trị
        // thì không hiển thị ngày từ fromDatePicker ở đây nữa,
        // vì label này dành cho ngày của "buổi học hiện tại".

        if (dateToDisplay != null) {
            String dayOfWeek = dateToDisplay.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi", "VN"));
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            currentSessionDayLabel.setText("Ngày học: " + dayOfWeek + " (" + dateToDisplay.format(dateFormatter) + ") " + context);
        } else {
            currentSessionDayLabel.setText("Ngày học: " + context);
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
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;"
        );
    }

    public void showInfo(String message) { showAlert(Alert.AlertType.INFORMATION, "Thông báo", message); }
    public void showWarning(String message) { showAlert(Alert.AlertType.WARNING, "Cảnh báo", message); }
    public void showError(String message) { showAlert(Alert.AlertType.ERROR, "Lỗi", message); }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void selectSession(ClassSession session) {
        if (sessionSelector != null) {
            sessionSelector.getSelectionModel().select(session);
        }
        updateCurrentSessionDayLabel();
    }

    // Create a utility method to set up columns with black text
    private void setBlackHeaderText(TableColumn<AbsenceRecord, ?> column, String title) {
        Label label = new Label(title);
        label.setTextFill(Color.BLACK);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        column.setGraphic(label);
    }



}
