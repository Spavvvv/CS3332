package view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class TeachingStatistics extends BaseScreenView {

    // UI Components
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherStatistics> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;

    // Toggle buttons
    private ToggleButton dayToggle;
    private ToggleButton monthToggle;
    private ToggleButton quarterToggle;
    private ToggleButton yearToggle;

    // Data
    private ObservableList<TeacherStatistics> teacherStatisticsList;
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Static variable to track the currently selected period type
    private static String selectedPeriodType = "day";

    public TeachingStatistics() {
        super("Thống kê giờ giảng", "teaching-statistics");
    }

    @Override
    public void initializeView() {
        //clear root
        root.getChildren().clear();

        // Initialize the root layout
        root.setSpacing(20);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        // Create the header
        createHeader();

        // Create the filter bar
        createFilterBar();

        // Create the table
        createStatisticsTable();

        // Load sample data
        loadSampleData();
    }

    private void createHeader() {
        Label titleLabel = new Label("Thống kê giờ giảng");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#0099FF"));

        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        root.getChildren().add(titleBox);
    }

    private void createFilterBar() {
        // Period selection toggle group
        periodToggleGroup = new ToggleGroup();

        HBox periodTypeBox = new HBox(10);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("Loại:");
        typeLabel.setTextFill(Color.BLACK);
        typeLabel.setPrefWidth(50);

        // Create toggle buttons with corresponding period types
        dayToggle = createToggleButton("Ngày", "day");
        monthToggle = createToggleButton("Tháng", "month");
        quarterToggle = createToggleButton("Quý", "quarter");
        yearToggle = createToggleButton("Năm", "year");

        // Set action handlers for navigation
        dayToggle.setOnAction(e -> {
            if (dayToggle.isSelected()) {
                selectedPeriodType = "day";
                // If this is the current view, no need to navigate
            }
        });

        monthToggle.setOnAction(e -> {
            if (monthToggle.isSelected()) {
                selectedPeriodType = "month";
                navigationController.navigateTo("monthly-teaching");
            }
        });

        quarterToggle.setOnAction(e -> {
            if (quarterToggle.isSelected()) {
                selectedPeriodType = "quarter";
                navigationController.navigateTo("quarterly-teaching");
            }
        });

        yearToggle.setOnAction(e -> {
            if (yearToggle.isSelected()) {
                selectedPeriodType = "year";
                navigationController.navigateTo("yearly-teaching");
            }
        });

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Date range pickers
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);
        fromDatePicker = new DatePicker(LocalDate.of(2025, 4, 1));
        configureDatePicker(fromDatePicker);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);
        toDatePicker = new DatePicker(LocalDate.of(2025, 6, 30));
        configureDatePicker(toDatePicker);

        dateRangeBox.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);

        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(statusOptions));
        statusComboBox.setValue("Tất cả");
        statusComboBox.setPrefWidth(150);

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Action buttons
        searchButton = createActionButton("🔍", "search-icon");
        searchButton.setStyle("-fx-background-color: #5c6bc0; -fx-text-fill: white;");

        exportExcelButton = createActionButton("Excel", "excel-icon");
        exportExcelButton.setStyle("-fx-background-color: #39ce1e; -fx-border-color: #ddd;");

        exportPdfButton = createActionButton("📄", "pdf-icon");
        exportPdfButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd;");

        printButton = createActionButton("🖨", "print-icon");
        printButton.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd;");

        actionButtonsBar = new HBox(10);
        actionButtonsBar.getChildren().addAll(searchButton, exportExcelButton, exportPdfButton, printButton);

        // Combine all filter components
        filterBar = new HBox(20);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setBackground(new Background(new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)));
        filterBar.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, actionButtonsBar);

        root.getChildren().add(filterBar);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(600);

        // Create the STT column with black header text
        TableColumn<TeacherStatistics, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherStatistics, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);

        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn);

        // Add date columns dynamically with black header text
        String[] dates = {"01/04/2025", "02/04/2025", "03/04/2025", "04/04/2025", "05/04/2025",
                "06/04/2025", "08/04/2025", "09/04/2025", "10/04/2025", "11/04/2025", "12/04/2025"};

        for (String date : dates) {
            // Create date column with black header text
            TableColumn<TeacherStatistics, String> dateColumn = new TableColumn<>();
            dateColumn.setPrefWidth(100);

            Label dateHeaderLabel = new Label(date);
            dateHeaderLabel.setTextFill(Color.BLACK);
            dateHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            dateColumn.setGraphic(dateHeaderLabel);

            // Each date has two sub-columns: Buổi and Giờ with black header text
            TableColumn<TeacherStatistics, Integer> sessionColumn = new TableColumn<>();
            sessionColumn.setCellValueFactory(new PropertyValueFactory<>("sessions"));
            sessionColumn.setPrefWidth(50);

            Label sessionHeaderLabel = new Label("Buổi");
            sessionHeaderLabel.setTextFill(Color.BLACK);
            sessionHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            sessionColumn.setGraphic(sessionHeaderLabel);

            TableColumn<TeacherStatistics, Double> hoursColumn = new TableColumn<>();
            hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));
            hoursColumn.setPrefWidth(50);

            Label hoursHeaderLabel = new Label("Giờ");
            hoursHeaderLabel.setTextFill(Color.BLACK);
            hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
            hoursColumn.setGraphic(hoursHeaderLabel);

            dateColumn.getColumns().addAll(sessionColumn, hoursColumn);
            statisticsTable.getColumns().add(dateColumn);
        }

        root.getChildren().add(statisticsTable);
    }

    private void loadSampleData() {
        teacherStatisticsList = FXCollections.observableArrayList(
                new TeacherStatistics(1, "Trịnh Đình Đức"),
                new TeacherStatistics(2, "Hoàng Ngọc Hà"),
                new TeacherStatistics(3, "Đinh Thị Ngọc Linh"),
                new TeacherStatistics(4, "Bùi Tuyết Mai"),
                new TeacherStatistics(5, "Nguyễn Tiến Dũng"),
                new TeacherStatistics(6, "Trần Trung Hải"),
                new TeacherStatistics(13, "Trần Thu Hiền"),
                new TeacherStatistics(14, "Nguyễn Lê Thanh Thủy"),
                new TeacherStatistics(15, "Hà Thị Ngọc"),
                new TeacherStatistics(16, "Phạm Quỳnh Trang"),
                new TeacherStatistics(17, "Trần Thu Hằng"),
                new TeacherStatistics(18, "Nguyễn Minh Anh"),
                new TeacherStatistics(19, "Kiều Thu Thảo")
        );

        statisticsTable.setItems(teacherStatisticsList);
    }

    private ToggleButton createToggleButton(String text, String periodType) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setToggleGroup(periodToggleGroup);
        toggleButton.setSelected(selectedPeriodType.equals(periodType));
        toggleButton.setPrefHeight(30);
        toggleButton.setPrefWidth(80);
        toggleButton.setUserData(periodType); // Store the period type in the button's userData
        return toggleButton;
    }

    private Button createActionButton(String text, String iconStyle) {
        Button button = new Button(text);
        button.setPrefSize(40, 40);
        return button;
    }

    private void configureDatePicker(DatePicker datePicker) {
        datePicker.setPrefHeight(30);
        datePicker.setPrefWidth(110);
        datePicker.setEditable(false);

        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                }
                return null;
            }
        };

        datePicker.setConverter(converter);
        datePicker.setPromptText("dd/MM/yyyy");
    }

    @Override
    public void refreshView() {
        // Would typically reload data from a data source
        loadSampleData();
    }

    // Method to update toggle button states based on the stored value
    private void updateToggleButtonsState() {
        dayToggle.setSelected(selectedPeriodType.equals("day"));
        monthToggle.setSelected(selectedPeriodType.equals("month"));
        quarterToggle.setSelected(selectedPeriodType.equals("quarter"));
        yearToggle.setSelected(selectedPeriodType.equals("year"));
    }

    // Data model for teacher statistics
    public static class TeacherStatistics {
        private final SimpleStringProperty teacherName;
        private final int stt;
        private int sessions;
        private double hours;

        public TeacherStatistics(int stt, String teacherName) {
            this.stt = stt;
            this.teacherName = new SimpleStringProperty(teacherName);
            this.sessions = 1; // Default value
            this.hours = 1.5;  // Default value
        }

        public String getTeacherName() {
            return teacherName.get();
        }

        public SimpleStringProperty teacherNameProperty() {
            return teacherName;
        }

        public int getStt() {
            return stt;
        }

        public int getSessions() {
            return sessions;
        }

        public void setSessions(int sessions) {
            this.sessions = sessions;
        }

        public double getHours() {
            return hours;
        }

        public void setHours(double hours) {
            this.hours = hours;
        }
    }

    // Event handlers would be implemented here
    private void handleSearch() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        String status = statusComboBox.getValue();

        // Log the search parameters
        System.out.println("Searching from " + fromDate + " to " + toDate + " with status: " + status);

        // In a real application, this would query a database or service
        refreshView();
    }

    private void handleExportExcel() {
        showSuccess("Đang xuất file Excel...");
    }

    private void handleExportPdf() {
        showSuccess("Đang xuất file PDF...");
    }

    private void handlePrint() {
        showSuccess("Đang chuẩn bị in...");
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Update toggle button state every time the view is activated
        updateToggleButtonsState();

        // Set up event handlers
        searchButton.setOnAction(e -> handleSearch());
        exportExcelButton.setOnAction(e -> handleExportExcel());
        exportPdfButton.setOnAction(e -> handleExportPdf());
        printButton.setOnAction(e -> handlePrint());
    }
}
