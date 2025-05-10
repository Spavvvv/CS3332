package view.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import view.BaseScreenView;

import src.model.absence.AbsenceRecord;
import src.dao.AbsenceRecordDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Gọi điện xác nhận nghỉ học
 */
public class AbsenceCallView extends BaseScreenView {

    // Constants for styling
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String SECONDARY_COLOR = "#ffffff";
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String BACKGROUND_COLOR = "#f5f5f5";

    // UI Components
    private Label titleLabel;
    private TextField searchField;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button searchButton;
    private Button returnButton;
    private Button saveButton;
    private TableView<AbsenceRecord> tableView;

    // Data
    private ObservableList<AbsenceRecord> absenceData = FXCollections.observableArrayList();
    private FilteredList<AbsenceRecord> filteredData;

    // DAO
    private AbsenceRecordDAO absenceRecordDAO;

    // Theo dõi thay đổi
    private boolean dataChanged = false;

    public AbsenceCallView() {
        super("Gọi điện xác nhận", "absence-call-view");
        // Initialize DAO
        absenceRecordDAO = new AbsenceRecordDAO();
    }

    /**
     * Sets the absence data from the controller
     * @param records The list of absence records to display
     */
    public void setAbsenceData(ObservableList<AbsenceRecord> records) {
        // Clear existing data and add new records
        absenceData.clear();
        absenceData.addAll(records);

        // Refresh the table
        if (tableView != null) {
            tableView.refresh();
        }

        // Reset the data changed flag since we're setting fresh data
        dataChanged = false;
    }

    @Override
    public void initializeView() {
        root.setSpacing(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Add components
        root.getChildren().addAll(
                createTitleBar(),
                createFilterBar(),
                createTableView()
        );

        // Load data from database
        loadDataFromDatabase();

        // Set up event handlers
        setupEventHandlers();
    }

    @Override
    public void refreshView() {
        super.refreshView();
        // Refresh data from database
        loadDataFromDatabase();
        if (tableView != null) {
            tableView.refresh();
        }
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;");

        // Title
        titleLabel = new Label("Gọi điện xác nhận");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Return button
        returnButton = new Button("← Quay lại");
        returnButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-content-display: CENTER"
        );
        returnButton.setGraphic(createButtonIcon("arrow-left", "white"));

        // Save button
        saveButton = new Button("✓ Lưu thông tin");
        saveButton.setStyle(
                "-fx-background-color: " + "#4fc3f7" + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-content-display: CENTER"
        );
        saveButton.setGraphic(createButtonIcon("save", "white"));

        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(returnButton, saveButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        titleBar.getChildren().addAll(titleLabel, spacer, buttons);

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

        // Search keyword field
        Label searchLabel = new Label("Từ khóa:");
        searchLabel.setStyle("-fx-font-weight: bold;");
        searchField = new TextField();
        searchField.setPromptText("Từ khóa");
        searchField.setPrefWidth(400);

        // Filter icon
        Button filterButton = new Button("Tìm");
        filterButton.setGraphic(createButtonIcon("filter", PRIMARY_COLOR));
        filterButton.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-content-display: CENTER"
        );

        // Date range
        Label fromLabel = new Label("Từ:");
        fromLabel.setStyle("-fx-font-weight: bold;");
        fromDatePicker = new DatePicker(LocalDate.now());
        fromDatePicker.setPrefWidth(150);

        Label toLabel = new Label("Đến:");
        toLabel.setStyle("-fx-font-weight: bold;");
        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(150);

        // Search button
        searchButton = new Button("Lọc");
        searchButton.setGraphic(createButtonIcon("search", "white"));
        searchButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 8 15;" +
                        "-fx-background-radius: 5;"+
                        "-fx-alignment: CENTER;" +
                        "-fx-content-display: CENTER"
        );

        // Add components to filter bar
        filterBar.getChildren().addAll(
                searchLabel, searchField,
                fromLabel, fromDatePicker,
                toLabel, toDatePicker,
                searchButton
        );

        return filterBar;
    }

    private TableView<AbsenceRecord> createTableView() {
        tableView = new TableView<>();
        tableView.setEditable(true);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Style
        tableView.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 5;");

        // Columns
        TableColumn<AbsenceRecord, Integer> idColumn = new TableColumn<>("STT");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);

        TableColumn<AbsenceRecord, ImageView> imageColumn = new TableColumn<>("Ảnh");
        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        imageColumn.setPrefWidth(60);
        imageColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(ImageView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });

        TableColumn<AbsenceRecord, String> nameColumn = new TableColumn<>("Họ và tên");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(200);

        TableColumn<AbsenceRecord, String> classNameColumn = new TableColumn<>("Lớp học");
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        classNameColumn.setPrefWidth(150);

        TableColumn<AbsenceRecord, String> dateColumn = new TableColumn<>("Ngày");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateColumn.setPrefWidth(100);

        TableColumn<AbsenceRecord, String> attendanceColumn = new TableColumn<>("Đi học");
        attendanceColumn.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        attendanceColumn.setPrefWidth(100);

        TableColumn<AbsenceRecord, String> noteColumn = new TableColumn<>("Ghi chú");
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteColumn.setPrefWidth(200);

        TableColumn<AbsenceRecord, Boolean> calledColumn = new TableColumn<>("Đã gọi điện");
        calledColumn.setCellValueFactory(cellData -> cellData.getValue().calledProperty());
        calledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(calledColumn));
        calledColumn.setPrefWidth(100);
        calledColumn.setEditable(true);

        TableColumn<AbsenceRecord, Boolean> approvedColumn = new TableColumn<>("Có phép");
        approvedColumn.setCellValueFactory(cellData -> cellData.getValue().approvedProperty());
        approvedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(approvedColumn));
        approvedColumn.setPrefWidth(100);
        approvedColumn.setEditable(true);

        // Add columns to table
        tableView.getColumns().addAll(
                idColumn, imageColumn, nameColumn, classNameColumn,
                dateColumn, attendanceColumn, noteColumn, calledColumn, approvedColumn
        );

        // Set data
        filteredData = new FilteredList<>(absenceData, p -> true);
        tableView.setItems(filteredData);

        return tableView;
    }

    private ImageView createButtonIcon(String iconName, String color) {
        // Placeholder for actual icons
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(Color.web(color));

        ImageView imageView = new ImageView();
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);

        // In a real application: imageView.setImage(new Image("/icons/" + iconName + ".png"));

        return imageView;
    }

    private void loadDataFromDatabase() {
        try {
            // Lấy dữ liệu từ database thông qua DAO
            List<AbsenceRecord> records = absenceRecordDAO.findAll();

            // Xóa dữ liệu hiện tại và thêm dữ liệu mới
            absenceData.clear();
            absenceData.addAll(records);

            // Reset dataChanged flag vì dữ liệu vừa mới load
            dataChanged = false;

        } catch (SQLException e) {
            showError("Lỗi khi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        // Return button event
        returnButton.setOnAction(e -> {
            if (dataChanged) {
                // Confirm if user wants to leave without saving
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Xác nhận");
                alert.setHeaderText("Dữ liệu chưa được lưu");
                alert.setContentText("Bạn có muốn lưu thay đổi trước khi quay lại không?");

                ButtonType buttonSave = new ButtonType("Lưu và quay lại");
                ButtonType buttonLeave = new ButtonType("Quay lại không lưu");
                ButtonType buttonCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(buttonSave, buttonLeave, buttonCancel);

                alert.showAndWait().ifPresent(type -> {
                    if (type == buttonSave) {
                        saveChanges();
                        navigateBack();
                    } else if (type == buttonLeave) {
                        navigateBack();
                    }
                    // Nếu là Cancel thì không làm gì cả
                });
            } else {
                navigateBack();
            }
        });

        // Save button event
        saveButton.setOnAction(e -> saveChanges());

        // Search button event
        searchButton.setOnAction(e -> applyFilters());

        // Track data changes
        tableView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                dataChanged = true;
            }
        });
    }

    private void navigateBack() {
        if (navigationController != null) {
            navigationController.goBack();
        } else {
            System.out.println("Navigation controller is null");
        }
    }

    private void saveChanges() {
        try {
            // Get all records from table
            List<AbsenceRecord> records = new ArrayList<>(tableView.getItems());

            // Save all records in a batch
            boolean success = absenceRecordDAO.updateBatch(records);

            if (success) {
                showInfo("Đã lưu thành công!");
                dataChanged = false;
            } else {
                showWarning("Không có thay đổi nào được lưu!");
            }

        } catch (SQLException e) {
            showError("Lỗi khi lưu dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        try {
            String keyword = searchField.getText().trim();
            LocalDate fromDate = fromDatePicker.getValue();
            LocalDate toDate = toDatePicker.getValue();

            // Lấy dữ liệu đã lọc từ database
            List<AbsenceRecord> filteredRecords = absenceRecordDAO.findByFilters(keyword, fromDate, toDate);

            // Cập nhật bảng
            absenceData.clear();
            absenceData.addAll(filteredRecords);

            // Reset dataChanged flag vì dữ liệu vừa mới load
            dataChanged = false;

        } catch (SQLException e) {
            showError("Lỗi khi lọc dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
