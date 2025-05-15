
package view.components;

import src.controller.TeachingStatisticsController;
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
import src.model.teaching.TeacherStatisticsModel;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TeachingStatisticsView extends BaseScreenView {

    // Controller
    private TeachingStatisticsController controller;

    // UI Components
    private HBox filterBar;
    private HBox actionButtonsBar;
    private TableView<TeacherStatisticsModel> statisticsTable;

    // Filter components (changed ToggleButton to Button)
    // private ToggleGroup periodToggleGroup; // Removed ToggleGroup
    private Button dayButton;
    private Button monthButton;
    private Button quarterButton;
    private Button yearButton;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> statusComboBox;

    // Action buttons
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;

    // Date formatter
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TeachingStatisticsView() {
        super("Thống kê giảng dạy", "teaching-statistics");
    }

    @Override
    public void initializeView() {
        // Initialize controller
        this.controller = new TeachingStatisticsController();

        // Set up the root VBox (already created in BaseScreenView)
        root.setSpacing(10);
        root.setPadding(new Insets(15));

        createFilterBar();
        createStatisticsTable();
        loadData();
    }

    private void createFilterBar() {
        // Period type selection (using Buttons instead of ToggleButtons)
        HBox periodTypeBox = new HBox(5);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);

        Label periodLabel = new Label("Chu kỳ:");
        periodLabel.setTextFill(Color.BLACK);

        // Create standard Buttons
        dayButton = createPeriodButton("Ngày", "day");
        monthButton = createPeriodButton("Tháng", "month");
        quarterButton = createPeriodButton("Quý", "quarter");
        yearButton = createPeriodButton("Năm", "year");

        // Add action handlers to buttons
        dayButton.setOnAction(e -> handlePeriodButtonClick("day"));
        monthButton.setOnAction(e -> handlePeriodButtonClick("month"));
        quarterButton.setOnAction(e -> handlePeriodButtonClick("quarter"));
        yearButton.setOnAction(e -> handlePeriodButtonClick("year"));


        periodTypeBox.getChildren().addAll(periodLabel, dayButton, monthButton, quarterButton, yearButton);

        // Date range selection
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);
        fromDatePicker = new DatePicker(controller.getFilter().getFromDate());
        configureDatePicker(fromDatePicker);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);
        toDatePicker = new DatePicker(controller.getFilter().getToDate());
        configureDatePicker(toDatePicker);

        dateRangeBox.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);

        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        // Convert List<String> to ObservableList<String>
        ObservableList<String> statusOptions = FXCollections.observableArrayList(controller.getStatusOptions());
        statusComboBox = new ComboBox<>(statusOptions);
        statusComboBox.setValue(controller.getFilter().getStatus());
        statusComboBox.setPrefWidth(150);

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Action buttons
        searchButton = createActionButton("🔍 Tìm", "search-icon");
        searchButton.setStyle("-fx-background-color: #5c6bc0; -fx-text-fill: white;");

        exportExcelButton = createActionButton("Excel", "excel-icon");
        exportExcelButton.setStyle("-fx-background-color: #39ce1e; -fx-text-fill: white;");

        exportPdfButton = createActionButton("PDF", "pdf-icon");
        exportPdfButton.setStyle("-fx-background-color: #e53935; -fx-text-fill: white;");

        printButton = createActionButton("In", "print-icon");
        printButton.setStyle("-fx-background-color: #616161; -fx-text-fill: white;");

        actionButtonsBar = new HBox(10);
        actionButtonsBar.getChildren().addAll(searchButton, exportExcelButton, exportPdfButton, printButton);
        actionButtonsBar.setAlignment(Pos.CENTER_RIGHT);

        // Combine all filter components
        VBox filterContainer = new VBox(10);

        HBox topFilterRow = new HBox(20);
        topFilterRow.setAlignment(Pos.CENTER_LEFT);
        topFilterRow.getChildren().addAll(periodTypeBox, dateRangeBox);

        HBox bottomFilterRow = new HBox(20);
        bottomFilterRow.setAlignment(Pos.CENTER_LEFT);
        bottomFilterRow.getChildren().addAll(statusBox, actionButtonsBar);

        filterContainer.getChildren().addAll(topFilterRow, bottomFilterRow);

        filterBar = new HBox();
        filterBar.getChildren().add(filterContainer);
        filterBar.setPadding(new Insets(10));
        filterBar.setBackground(new Background(new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)));
        filterBar.setPrefWidth(Double.MAX_VALUE);

        root.getChildren().add(filterBar);

        // No initial button state update needed as they are not toggle buttons
        // updateToggleButtonsState(); // Removed
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(600);

        // Create the STT column with black header text
        TableColumn<TeacherStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);

        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn);

        // Add date columns dynamically with black header text
        LocalDate startDate = controller.getFilter().getFromDate();
        LocalDate endDate = controller.getFilter().getToDate();

        // Limit to 14 days for UI simplicity if range is too large
        if (startDate != null && endDate != null && startDate.plusDays(14).isBefore(endDate)) {
            endDate = startDate.plusDays(14);
        }

        if (startDate != null && endDate != null) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                final LocalDate dateForColumn = currentDate; // For lambda capture

                // Create date column with black header text
                TableColumn<TeacherStatisticsModel, String> dateColumn = new TableColumn<>();
                dateColumn.setPrefWidth(100);

                String formattedDate = dateFormatter.format(currentDate);
                Label dateHeaderLabel = new Label(formattedDate);
                dateHeaderLabel.setTextFill(Color.BLACK);
                dateHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                dateColumn.setGraphic(dateHeaderLabel);

                // Each date has two sub-columns: Buổi and Giờ with black header text
                TableColumn<TeacherStatisticsModel, Integer> sessionColumn = new TableColumn<>();
                sessionColumn.setCellValueFactory(cellData ->
                        cellData.getValue().getDailyStatistic(dateForColumn).sessionsProperty().asObject());
                sessionColumn.setPrefWidth(50);

                Label sessionHeaderLabel = new Label("Buổi");
                sessionHeaderLabel.setTextFill(Color.BLACK);
                sessionHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                sessionColumn.setGraphic(sessionHeaderLabel);

                TableColumn<TeacherStatisticsModel, Double> hoursColumn = new TableColumn<>();
                hoursColumn.setCellValueFactory(cellData ->
                        cellData.getValue().getDailyStatistic(dateForColumn).hoursProperty().asObject());
                hoursColumn.setPrefWidth(50);

                Label hoursHeaderLabel = new Label("Giờ");
                hoursHeaderLabel.setTextFill(Color.BLACK);
                hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                hoursColumn.setGraphic(hoursHeaderLabel);

                dateColumn.getColumns().addAll(sessionColumn, hoursColumn);
                statisticsTable.getColumns().add(dateColumn);

                currentDate = currentDate.plusDays(1);
            }
        }


        root.getChildren().add(statisticsTable);
    }

    private void loadData() {
        ObservableList<TeacherStatisticsModel> teacherStatistics = controller.getTeacherStatistics();
        if (teacherStatistics != null) {
            statisticsTable.setItems(teacherStatistics);
        } else {
            // Handle error - display error message or empty table
            statisticsTable.setItems(FXCollections.observableArrayList());
            showError("Không thể tải dữ liệu từ cơ sở dữ liệu. Vui lòng thử lại sau.");
        }
    }

    // New method to create standard period buttons
    private Button createPeriodButton(String text, String periodType) {
        Button button = new Button(text);
        button.setPrefHeight(30);
        button.setPrefWidth(80);
        button.setUserData(periodType); // Store the period type in the button's userData
        button.setTextFill(Color.BLACK); // Set default text color
        return button;
    }

    private Button createActionButton(String text, String iconStyle) {
        Button button = new Button(text);
        button.setPrefSize(90, 40);
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
        super.refreshView();

        // Clear table and recreate based on current filter settings
        if (statisticsTable != null) {
            statisticsTable.getItems().clear();
            statisticsTable.getColumns().clear();

            // Remove the old table from the root
            root.getChildren().remove(statisticsTable);

            // Recreate and load
            createStatisticsTable();
            loadData();
        }
    }

    // Removed updateToggleButtonsState method

    private void handleSearch() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        String status = statusComboBox.getValue();

        // Validate dates
        if (fromDate == null || toDate == null) {
            showError("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
            return;
        }

        if (fromDate.isAfter(toDate)) {
            showError("Ngày bắt đầu không thể sau ngày kết thúc.");
            return;
        }

        controller.updateDateRange(fromDate, toDate);
        controller.updateStatus(status);

        System.out.println("Searching from " + fromDate + " to " + toDate + " with status: " + status);

        refreshView();
    }

    // New method to handle clicks on the period buttons
    private void handlePeriodButtonClick(String periodType) {
        controller.updatePeriodType(periodType);
        System.out.println("Selected period type: " + periodType);

        // Navigate or refresh based on period type
        if (periodType.equals("month")) {
            navigationController.navigateTo("monthly-teaching");
        } else if (periodType.equals("quarter")) {
            navigationController.navigateTo("quarterly-teaching");
        } else if (periodType.equals("year")) {
            navigationController.navigateTo("yearly-teaching");
        } else {
            // Assuming "day" is the default or requires refreshing the current view
            refreshView();
        }
    }


    private void handleExportExcel() {
        boolean success = controller.exportToExcel();
        if (success) {
            showSuccess("Đang xuất file Excel...");
        } else {
            showError("Không thể xuất file Excel. Vui lòng thử lại sau.");
        }
    }

    private void handleExportPdf() {
        boolean success = controller.exportToPdf();
        if (success) {
            showSuccess("Đang xuất file PDF...");
        } else {
            showError("Không thể xuất file PDF. Vui lòng thử lại sau.");
        }
    }

    private void handlePrint() {
        boolean success = controller.print();
        if (success) {
            showSuccess("Đang chuẩn bị in...");
        } else {
            showError("Không thể chuẩn bị in. Vui lòng thử lại sau.");
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // No initial button state update needed for standard buttons
        // updateToggleButtonsState(); // Removed

        // Set up event handlers for action buttons
        if (searchButton != null) {
            searchButton.setOnAction(e -> handleSearch());
            exportExcelButton.setOnAction(e -> handleExportExcel());
            exportPdfButton.setOnAction(e -> handleExportPdf());
            printButton.setOnAction(e -> handlePrint());
        }
        // Period button handlers are set in createFilterBar
    }

    @Override
    public boolean requiresAuthentication() {
        // This view requires authentication
        return true;
    }
}

