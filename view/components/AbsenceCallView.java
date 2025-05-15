
package view.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell; // << ADDED
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter; // << ADDED
import src.controller.AbsenceCallController;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import view.BaseScreenView;
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

    // << ADDED: ObservableList for status choices
    private final ObservableList<String> statusOptions =
            FXCollections.observableArrayList("Chưa điểm danh", "Có mặt", "Vắng", "Có Phép");

    public AbsenceCallView() {
        super("Gọi điện xác nhận", "absence-call-view");
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
        sessionLabel.setStyle("-fx-font-weight: bold;");
        sessionSelector = new ComboBox<>();
        sessionSelector.setPromptText("Chọn buổi học");
        sessionSelector.setPrefWidth(300);

        sessionSelector.setCellFactory(lv -> new ListCell<ClassSession>() {
            @Override
            protected void updateItem(ClassSession session, boolean empty) {
                super.updateItem(session, empty);
                setText(empty || session == null ? null : session.getSummary());
            }
        });

        sessionSelector.setConverter(new StringConverter<ClassSession>() {
            @Override
            public String toString(ClassSession session) {
                return session == null ? null : session.getSummary();
            }

            @Override
            public ClassSession fromString(String string) {
                return null;
            }
        });


        Label fromLabel = new Label("Từ ngày:");
        fromLabel.setStyle("-fx-font-weight: bold;");
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

        TableColumn<AbsenceRecord, Integer> idCol = new TableColumn<>("STT");
        idCol.setCellValueFactory(new PropertyValueFactory<>("displayId"));
        idCol.setPrefWidth(50);
        idCol.setSortable(false);
        idCol.setEditable(false); // Typically IDs are not editable

        TableColumn<AbsenceRecord, String> nameCol = new TableColumn<>("Họ và tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        nameCol.setPrefWidth(200);
        nameCol.setEditable(false); // Student name might not be editable here

        TableColumn<AbsenceRecord, String> classCol = new TableColumn<>("Lớp học");
        classCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classCol.setPrefWidth(150);
        classCol.setEditable(false); // Class name might not be editable here

        TableColumn<AbsenceRecord, String> dateCol = new TableColumn<>("Ngày nghỉ");
        dateCol.setCellValueFactory(cellData -> cellData.getValue().absenceDateFormattedProperty());
        dateCol.setPrefWidth(100);
        dateCol.setEditable(false); // Date might not be editable here

        // << MODIFIED: Status column to be editable with ComboBox
        TableColumn<AbsenceRecord, String> statusCol = new TableColumn<>("Tình trạng");
        // This assumes AbsenceRecord has a statusProperty() returning StringProperty
        statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        statusCol.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(), statusOptions));
        statusCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setStatus(event.getNewValue());
            // Additional logic: if status is "Có mặt" or "Xin phép", "approved" might change.
            // This logic could be in the controller or model.
            // For example:
            if ("Xin phép".equals(event.getNewValue())) {
                record.setApproved(true); // Automatically set approved if "Xin phép"
            } else if ("Có mặt".equals(event.getNewValue())) {
                // If student is present, perhaps they are not "absent" and "approved" is irrelevant or false.
                // The exact logic depends on your application's rules.
                // record.setApproved(false); // Example
            }

            if (controller != null) {
                controller.setDataChanged(true);
            }
            tableView.refresh(); // Refresh to show any consequential changes (e.g., to approved status)
        });
        statusCol.setPrefWidth(120); // Increased width slightly for dropdown
        statusCol.setEditable(true);


        TableColumn<AbsenceRecord, String> noteCol = new TableColumn<>("Ghi chú");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setCellFactory(TextFieldTableCell.forTableColumn());
        noteCol.setOnEditCommit(event -> {
            AbsenceRecord record = event.getRowValue();
            record.setNote(event.getNewValue());
            if (controller != null) controller.setDataChanged(true);
        });
        noteCol.setPrefWidth(200);
        noteCol.setEditable(true);

        TableColumn<AbsenceRecord, Boolean> calledCol = new TableColumn<>("Đã gọi");
        calledCol.setCellValueFactory(new PropertyValueFactory<>("called"));
        calledCol.setCellFactory(CheckBoxTableCell.forTableColumn(calledCol));
        calledCol.setOnEditCommit(event -> { // Added OnEditCommit for CheckBox
            AbsenceRecord record = event.getRowValue();
            record.setCalled(event.getNewValue());
            if (controller != null) controller.setDataChanged(true);
        });
        calledCol.setPrefWidth(100);
        calledCol.setEditable(true);

        TableColumn<AbsenceRecord, Boolean> approvedCol = new TableColumn<>("Có phép");
        approvedCol.setCellValueFactory(new PropertyValueFactory<>("approved"));
        approvedCol.setCellFactory(CheckBoxTableCell.forTableColumn(approvedCol));
        approvedCol.setOnEditCommit(event -> { // Added OnEditCommit for CheckBox
            AbsenceRecord record = event.getRowValue();
            record.setApproved(event.getNewValue());
            if (controller != null) controller.setDataChanged(true);
        });
        approvedCol.setPrefWidth(100);
        approvedCol.setEditable(true);

        tv.getColumns().addAll(idCol, nameCol, classCol, dateCol, statusCol, noteCol, calledCol, approvedCol);
        return tv;
    }

    private void setupEventHandlers() {
        NavigationController navController = super.getNavigationController();

        if (navController != null) {
            returnButton.setOnAction(e -> navController.goBack());
            homeworkButton.setOnAction(e -> navController.navigateTo("homework-view"));
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
                updateCurrentSessionDayLabel();
            });
        } else {
            LOGGER.severe("AbsenceCallView: Controller is null during setupEventHandlers. Data-related actions will not be wired.");
            if (saveButton != null) saveButton.setDisable(true);
            if (filterButton != null) filterButton.setDisable(true);
            if (sessionSelector != null) sessionSelector.setDisable(true);
        }
    }

    public void setSessionItems(ObservableList<ClassSession> sessions) {
        sessionSelector.setItems(sessions);
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
        return sessionSelector.getSelectionModel().getSelectedItem();
    }

    public LocalDate getFromDate() {
        return fromDatePicker.getValue();
    }

    private void updateCurrentSessionDayLabel() {
        if (currentSessionDayLabel == null) return;

        ClassSession selectedSession = getSelectedSession();
        LocalDate dateToDisplay = null;
        String context = "(chưa chọn buổi)";

        if (selectedSession != null && selectedSession.getDate() != null) {
            dateToDisplay = selectedSession.getDate();
            context = "";
        }

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
}

