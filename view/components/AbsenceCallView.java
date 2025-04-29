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
import view.BaseScreenView;

import java.time.LocalDate;

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

    public AbsenceCallView() {
        super("Gọi điện xác nhận", "absence-call-view");
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

        // Load sample data
        loadSampleData();

        // Set up event handlers
        setupEventHandlers();
    }

    @Override
    public void refreshView() {
        super.refreshView();
        // Refresh data if needed
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

        // Return button - sử dụng text "←" làm icon
        returnButton = new Button("← Quay lại");
        returnButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                        "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
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
                        "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                        "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
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
                        "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                        "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
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
                        "-fx-alignment: CENTER;" +  // Canh giữa nội dung
                        "-fx-content-display: CENTER"  // Hiển thị nội dung ở giữa
        );

        // Add components to filter bar
        filterBar.getChildren().addAll(
                searchLabel, searchField, filterButton,
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
        // Trong ứng dụng thực tế, bạn sẽ load icon thực tế
        // Đây chỉ là placeholder
        Rectangle rect = new Rectangle(16, 16);
        rect.setFill(Color.web(color));

        ImageView imageView = new ImageView();
        imageView.setFitHeight(16);
        imageView.setFitWidth(16);

        // Trong ứng dụng thực tế: imageView.setImage(new Image("/icons/" + iconName + ".png"));

        return imageView;
    }

    private void loadSampleData() {
        // Dữ liệu mẫu cho demo
        for (int i = 1; i <= 10; i++) {
            ImageView studentImage = new ImageView();
            studentImage.setFitHeight(30);
            studentImage.setFitWidth(30);

            AbsenceRecord record = new AbsenceRecord(
                    i,
                    studentImage,
                    "Học sinh " + i,
                    "Lớp " + (i % 3 + 1),
                    "29/04/2025",
                    "Vắng",
                    "Lý do " + i,
                    false,
                    false
            );

            absenceData.add(record);
        }
    }

    private void setupEventHandlers() {
        // Thiết lập các xử lý sự kiện

        // Xử lý sự kiện nút Quay lại
        returnButton.setOnAction(e -> {
            if (navigationController != null) {
                navigationController.goBack();
            } else {
                System.out.println("NavigationController chưa được thiết lập");
            }
        });

        // Xử lý sự kiện nút Lưu thông tin
        saveButton.setOnAction(e -> {
            saveChanges();
        });

        // Xử lý sự kiện nút Tìm kiếm
        searchButton.setOnAction(e -> {
            applyFilters();
        });

        // Xử lý sự kiện tìm kiếm theo từ khóa
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (record.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (record.getClassName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (record.getDate().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
    }

    private void saveChanges() {
        // Lưu các thay đổi vào database hoặc service
        showSuccess("Đã lưu thông tin thành công!");
    }

    private void applyFilters() {
        // Áp dụng các bộ lọc dựa trên từ khóa và ngày
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        filteredData.setPredicate(record -> {
            // TODO: Lọc theo ngày
            // Đây là demo, trong ứng dụng thực tế bạn sẽ cần kiểm tra ngày

            return true;
        });

        showSuccess("Đã áp dụng bộ lọc!");
    }

    @Override
    public void onShow() {
        super.onShow();
        // Cập nhật lại dữ liệu khi hiển thị view
        refreshView();
    }

    /**
     * Inner class đại diện cho bản ghi vắng mặt
     * Các thuộc tính public để module-info.java có thể truy cập
     */
    public static class AbsenceRecord {
        private final Integer id;
        private final ImageView image;
        private final String name;
        private final String className;
        private final String date;
        private final String attendance;
        private final String note;
        private final BooleanProperty called = new SimpleBooleanProperty();
        private final BooleanProperty approved = new SimpleBooleanProperty();

        public AbsenceRecord(Integer id, ImageView image, String name, String className,
                             String date, String attendance, String note,
                             boolean called, boolean approved) {
            this.id = id;
            this.image = image;
            this.name = name;
            this.className = className;
            this.date = date;
            this.attendance = attendance;
            this.note = note;
            this.called.set(called);
            this.approved.set(approved);
        }

        // Getters và Setters public
        public Integer getId() { return id; }

        public ImageView getImage() { return image; }

        public String getName() { return name; }

        public String getClassName() { return className; }

        public String getDate() { return date; }

        public String getAttendance() { return attendance; }

        public String getNote() { return note; }

        public boolean isCalled() { return called.get(); }

        public BooleanProperty calledProperty() { return called; }

        public void setCalled(boolean called) { this.called.set(called); }

        public boolean isApproved() { return approved.get(); }

        public BooleanProperty approvedProperty() { return approved; }

        public void setApproved(boolean approved) { this.approved.set(approved); }
    }

    // Getter để lấy dữ liệu cho controller
    public ObservableList<AbsenceRecord> getAbsenceData() {
        return absenceData;
    }

    // Setter để controller có thể cập nhật dữ liệu
    public void setAbsenceData(ObservableList<AbsenceRecord> data) {
        this.absenceData.setAll(data);
        if (tableView != null) {
            tableView.refresh();
        }
    }
}
