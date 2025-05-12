package view.components;

import src.controller.MonthlyTeachingStatisticsController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics;
import view.BaseScreenView; // Assuming BaseScreenView is in a 'view' package

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MonthlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    private ComboBox<Month> fromMonthComboBox;
    private ComboBox<Integer> fromYearComboBox;
    private ComboBox<Month> toMonthComboBox;
    private ComboBox<Integer> toYearComboBox;
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherMonthlyStatistics> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;
    private Label sessionTotal;
    private Label hoursTotal;
    private Label totalSessionTotal;
    private Label totalHoursTotal;

    // Toggle buttons
    private ToggleButton dayToggle;
    private ToggleButton monthToggle;
    private ToggleButton quarterToggle;
    private ToggleButton yearToggle;

    // Controller reference
    private final MonthlyTeachingStatisticsController controller;

    // Date formatter for ComboBox display
    private final StringConverter<Month> monthStringConverter = new StringConverter<Month>() {
        @Override
        public String toString(Month month) {
            return month == null ? "" : "Thg " + month.getValue();
        }

        @Override
        public Month fromString(String string) {
            // Not needed for this view, but must be implemented
            return null;
        }
    };

    // Year range for year combo boxes
    private final int START_YEAR = 2023;
    private final int END_YEAR = Year.now().getValue() + 5; // Show current year + 5 years

    public MonthlyTeachingStatisticsView() {
        super("Thống kê giờ giảng", "monthly-teaching"); // Ensure base view handles screen ID
        this.controller = new MonthlyTeachingStatisticsController();
    }

    @Override
    public void initializeView() {
        // Clear root
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

        // Action buttons bar (placed after filter bar in the VBox)
        actionButtonsBar = new HBox(10);
        actionButtonsBar.setAlignment(Pos.CENTER_RIGHT);
        exportExcelButton = createActionButton("Excel"); // Simplified action button creation
        exportPdfButton = createActionButton("PDF");
        printButton = createActionButton("Print");
        actionButtonsBar.getChildren().addAll(exportExcelButton, exportPdfButton, printButton);

        // Add filter and action bars to a container
        VBox topContainer = new VBox(10);
        topContainer.getChildren().addAll(filterBar, actionButtonsBar);
        root.getChildren().add(topContainer);

        // Add the table container after the filter/action bars
        VBox tableContainer = new VBox();
        tableContainer.getChildren().add(statisticsTable);

        // Create totals row
        GridPane totalRow = createTotalRow();
        tableContainer.getChildren().add(totalRow);

        root.getChildren().add(tableContainer);

        // Note: Initial data load and event handlers are set up in onActivate
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

        // Create toggle buttons - initial selection handled in onActivate
        dayToggle = createToggleButton("Ngày", false);
        monthToggle = createToggleButton("Tháng", false);
        quarterToggle = createToggleButton("Quý", false);
        yearToggle = createToggleButton("Năm", false);

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Month/Year selection for date range
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);

        // From month/year selector
        HBox fromDateBox = new HBox(5);
        fromMonthComboBox = new ComboBox<>();
        fromMonthComboBox.setPrefWidth(65);
        fromMonthComboBox.setItems(FXCollections.observableArrayList(Month.values())); // All months
        fromMonthComboBox.setConverter(monthStringConverter);

        fromYearComboBox = new ComboBox<>();
        fromYearComboBox.setPrefWidth(70);
        List<Integer> years = IntStream.rangeClosed(START_YEAR, END_YEAR).boxed().collect(Collectors.toList());
        fromYearComboBox.setItems(FXCollections.observableArrayList(years));

        fromDateBox.getChildren().addAll(fromMonthComboBox, fromYearComboBox);

        Label toLabel = new Label("đến:");
        toLabel.setTextFill(Color.BLACK);

        // To month/year selector
        HBox toDateBox = new HBox(5);
        toMonthComboBox = new ComboBox<>();
        toMonthComboBox.setPrefWidth(65);
        toMonthComboBox.setItems(FXCollections.observableArrayList(Month.values())); // All months
        toMonthComboBox.setConverter(monthStringConverter);

        toYearComboBox = new ComboBox<>();
        toYearComboBox.setPrefWidth(70);
        toYearComboBox.setItems(FXCollections.observableArrayList(years)); // Same year list

        toDateBox.getChildren().addAll(toMonthComboBox, toYearComboBox);

        dateRangeBox.getChildren().addAll(fromLabel, fromDateBox, toLabel, toDateBox);

        // Status dropdown
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        statusComboBox = new ComboBox<>();
        statusComboBox.setItems(FXCollections.observableArrayList(
                controller.getModel().getStatusOptions() // Get status options from model
        ));
        statusComboBox.setValue("Tất cả"); // Default value
        statusComboBox.setPrefWidth(120);

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Search button
        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefSize(100, 30);
        searchButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");

        // Creating top horizontal bar for filters
        filterBar = new HBox(20);
        filterBar.setPadding(new Insets(10));
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, searchButton);
        filterBar.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 1px;");
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setPrefHeight(400);
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create the STT column with black header text
        TableColumn<TeacherMonthlyStatistics, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherMonthlyStatistics, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false);

        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the month column (will dynamically update graphic)
        TableColumn<TeacherMonthlyStatistics, String> monthColumn = new TableColumn<>();
        // Initial graphic (will be updated in updateTableWithModelData)
        Label monthHeaderLabel = new Label("Tháng");
        monthHeaderLabel.setTextFill(Color.BLACK);
        monthHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        monthColumn.setGraphic(monthHeaderLabel);

        // Sessions sub-column
        TableColumn<TeacherMonthlyStatistics, Integer> sessionsColumn = new TableColumn<>();
        sessionsColumn.setCellValueFactory(new PropertyValueFactory<>("sessions"));
        sessionsColumn.setPrefWidth(80);
        sessionsColumn.setSortable(false);

        Label sessionsHeaderLabel = new Label("Buổi");
        sessionsHeaderLabel.setTextFill(Color.BLACK);
        sessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sessionsColumn.setGraphic(sessionsHeaderLabel);


        // Hours sub-column
        TableColumn<TeacherMonthlyStatistics, Double> hoursColumn = new TableColumn<>();
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));
        hoursColumn.setPrefWidth(80);
        hoursColumn.setSortable(false);

        Label hoursHeaderLabel = new Label("Giờ");
        hoursHeaderLabel.setTextFill(Color.BLACK);
        hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        hoursColumn.setGraphic(hoursHeaderLabel);

        monthColumn.getColumns().addAll(sessionsColumn, hoursColumn);

        // Create the total column
        TableColumn<TeacherMonthlyStatistics, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total sessions sub-column
        TableColumn<TeacherMonthlyStatistics, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total hours sub-column
        TableColumn<TeacherMonthlyStatistics, Double> totalHoursColumn = new TableColumn<>();
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setPrefWidth(80);
        totalHoursColumn.setSortable(false);

        Label totalHoursHeaderLabel = new Label("Giờ");
        totalHoursHeaderLabel.setTextFill(Color.BLACK);
        totalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeaderLabel);

        totalColumn.getColumns().addAll(totalSessionsColumn, totalHoursColumn);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, monthColumn, totalColumn);
    }

    private GridPane createTotalRow() {
        GridPane totalRow = new GridPane();
        totalRow.setPadding(new Insets(5));
        totalRow.setStyle("-fx-border-color: #ddd; -fx-background-color: #f9f9f9;");

        // Set column constraints to match table columns
        ColumnConstraints sttColumnConstraint = new ColumnConstraints();
        sttColumnConstraint.setPrefWidth(60);

        ColumnConstraints nameColumnConstraint = new ColumnConstraints();
        nameColumnConstraint.setPrefWidth(150);

        ColumnConstraints sessionsColumnConstraint = new ColumnConstraints();
        sessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints hoursColumnConstraint = new ColumnConstraints();
        hoursColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalSessionsColumnConstraint = new ColumnConstraints();
        totalSessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalHoursColumnConstraint = new ColumnConstraints();
        totalHoursColumnConstraint.setPrefWidth(80);


        totalRow.getColumnConstraints().addAll(
                sttColumnConstraint,
                nameColumnConstraint,
                sessionsColumnConstraint,
                hoursColumnConstraint,
                totalSessionsColumnConstraint,
                totalHoursColumnConstraint
        );

        // Create and add the "Tổng cộng" label
        Label totalLabel = new Label("Tổng cộng");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalLabel, 0);
        GridPane.setColumnSpan(totalLabel, 2); // Span across STT and Giáo viên columns

        // Create and add the session totals labels
        sessionTotal = new Label("0");
        sessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        sessionTotal.setAlignment(Pos.CENTER); // Center the text
        GridPane.setColumnIndex(sessionTotal, 2);
        GridPane.setHalignment(sessionTotal, javafx.geometry.HPos.CENTER); // Center horizontally

        hoursTotal = new Label("0.0");
        hoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        hoursTotal.setAlignment(Pos.CENTER); // Center the text
        GridPane.setColumnIndex(hoursTotal, 3);
        GridPane.setHalignment(hoursTotal, javafx.geometry.HPos.CENTER); // Center horizontally


        // Create and add the total session totals labels
        totalSessionTotal = new Label("0");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalSessionTotal.setAlignment(Pos.CENTER); // Center the text
        GridPane.setColumnIndex(totalSessionTotal, 4);
        GridPane.setHalignment(totalSessionTotal, javafx.geometry.HPos.CENTER); // Center horizontally

        totalHoursTotal = new Label("0.0");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalHoursTotal.setAlignment(Pos.CENTER); // Center the text
        GridPane.setColumnIndex(totalHoursTotal, 5);
        GridPane.setHalignment(totalHoursTotal, javafx.geometry.HPos.CENTER); // Center horizontally

        totalRow.getChildren().addAll(totalLabel, sessionTotal, hoursTotal, totalSessionTotal, totalHoursTotal);

        return totalRow;
    }


    private void updateTableWithModelData() {
        MonthlyTeachingStatisticsModel model = controller.getModel();
        statisticsTable.setItems(model.getTeacherStatisticsList());

        // Update totals
        sessionTotal.setText(String.valueOf(model.getTotalSessions()));
        hoursTotal.setText(String.format("%.1f", model.getTotalHours())); // Format hours to one decimal place
        totalSessionTotal.setText(String.valueOf(model.getTotalSessions()));
        totalHoursTotal.setText(String.format("%.1f", model.getTotalHours())); // Format hours to one decimal place


        // Update month column header graphic
        Month selectedMonth = fromMonthComboBox.getValue();
        int selectedYear = fromYearComboBox.getValue();
        // Find the column with the "Tháng" graphic
        for (TableColumn<TeacherMonthlyStatistics, ?> column : statisticsTable.getColumns()) {
            if (column.getGraphic() instanceof Label) {
                Label headerLabel = (Label) column.getGraphic();
                if (headerLabel.getText().equals("Tháng")) { // Identify the 'monthColumn' by its initial text
                    headerLabel.setText("Tháng " + selectedMonth.getValue() + "/" + selectedYear);
                    break; // Found and updated, exit loop
                }
            }
        }
    }

    private ToggleButton createToggleButton(String text, boolean selected) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setToggleGroup(periodToggleGroup);
        toggleButton.setSelected(selected);
        toggleButton.setPrefHeight(30);
        toggleButton.setPrefWidth(80);
        // Basic styling for toggle buttons
        toggleButton.setStyle("-fx-base: #E0E0E0;"); // Default color
        toggleButton.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                toggleButton.setStyle("-fx-base: #1976D2; -fx-text-fill: white;"); // Selected color
            } else {
                toggleButton.setStyle("-fx-base: #E0E0E0; -fx-text-fill: black;"); // Unselected color
            }
        });
        return toggleButton;
    }

    private Button createActionButton(String text) { // Simplified, icon handling would be added here
        Button button = new Button(text);
        button.setPrefSize(60, 30); // Adjust size as needed
        // Basic styling
        button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"); // Example green
        // Change style on hover for feedback
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"));
        return button;
    }


    @Override
    public void refreshView() {
        // This might be called when the view becomes active again
        // Ensure date selectors are set to current month/year
        YearMonth currentYearMonth = YearMonth.now();
        fromMonthComboBox.setValue(currentYearMonth.getMonth());
        fromYearComboBox.setValue(currentYearMonth.getYear());
        toMonthComboBox.setValue(currentYearMonth.getMonth());
        toYearComboBox.setValue(currentYearMonth.getYear());

        // Load initial data (current month) and update table
        controller.loadInitialData();
        updateTableWithModelData();
        // Ensure "Tháng" toggle is selected when the monthly view is refreshed
        monthToggle.setSelected(true);
    }

    // Event handlers
    private void handleSearch() {
        Month fromMonth = fromMonthComboBox.getValue();
        int fromYear = fromYearComboBox.getValue();
        Month toMonth = toMonthComboBox.getValue();
        int toYear = toYearComboBox.getValue();
        String status = statusComboBox.getValue();

        if (fromMonth == null || fromYear == 0 || toMonth == null || toYear == 0 || status == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn đầy đủ khoảng thời gian và trạng thái.", Alert.AlertType.WARNING);
            return;
        }

        // Delegate search to controller
        boolean success = controller.searchStatistics(fromMonth, fromYear, toMonth, toYear, status);

        if (!success) {
            showAlert("Lỗi kết nối hoặc truy vấn", "Không thể tải dữ liệu. Vui lòng kiểm tra kết nối cơ sở dữ liệu.", Alert.AlertType.ERROR);
            // The controller already clears the model data on error, so just update the table
        }

        // Always update the table to reflect the model's current state (either data or empty)
        updateTableWithModelData();
    }

    private void handleExportExcel() {
        showSuccess("Đang xuất file Excel...");
        // Implement actual export logic here
    }

    private void handleExportPdf() {
        showSuccess("Đang xuất file PDF...");
        // Implement actual export logic here
    }

    private void handlePrint() {
        showSuccess("Đang chuẩn bị in...");
        // Implement actual print logic here
    }

    // Helper method to show a success notification (replace with actual UI feedback)
    public void showSuccess(String message) {
        System.out.println("INFO: " + message); // Placeholder for actual UI notification
        // Example: Using a simple Alert for now
        // showAlert("Thông báo", message, Alert.AlertType.INFORMATION);
    }

    // Helper for showing Alerts
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    private void handlePeriodChange(ToggleButton selected) {
        if (selected != null) {
            String viewType = selected.getText();
            navigationController.saveToggleState("view_type", viewType);

            // Navigate to the corresponding view based on the selected toggle
            if (viewType.equals("Ngày")) {
                navigationController.navigateTo("teaching-statistics"); // Assuming this is the Daily view ID
            } else if (viewType.equals("Quý")) {
                navigationController.navigateTo("quarterly-teaching"); // Assuming this is the Quarterly view ID
            } else if (viewType.equals("Năm")) {
                navigationController.navigateTo("yearly-teaching"); // Assuming this is the Yearly view ID
            }
            // If "Tháng" is selected, stay on this view
        }
    }


    @Override
    public void onActivate() {
        super.onActivate();

        // Set up event handlers - only do this once when the view is activated
        searchButton.setOnAction(e -> handleSearch());
        exportExcelButton.setOnAction(e -> handleExportExcel());
        exportPdfButton.setOnAction(e -> handleExportPdf());
        printButton.setOnAction(e -> handlePrint());

        // Check saved view type or default to "Tháng"
        String savedViewType = navigationController.getSavedToggleState("view_type");

        if (savedViewType != null && !savedViewType.equals("Tháng")) {
            // If a different view type was saved, navigate to that view
            handlePeriodChange(periodToggleGroup.getToggles().stream()
                    .filter(toggle -> ((ToggleButton)toggle).getText().equals(savedViewType))
                    .map(toggle -> (ToggleButton)toggle)
                    .findFirst().orElse(monthToggle)); // Default to month toggle if not found
            return; // Exit onActivate as we're navigating away
        }

        // If saved type is "Tháng" or no type saved, ensure "Tháng" is selected and load data
        monthToggle.setSelected(true);
        navigationController.saveToggleState("view_type", "Tháng"); // Ensure "Tháng" is the saved state

        // Set initial date selectors to the current month and year
        YearMonth currentYearMonth = YearMonth.now();
        fromMonthComboBox.setValue(currentYearMonth.getMonth());
        fromYearComboBox.setValue(currentYearMonth.getYear());
        toMonthComboBox.setValue(currentYearMonth.getMonth());
        toYearComboBox.setValue(currentYearMonth.getYear());

        // Load data for the current month and update the table
        controller.loadInitialData();
        updateTableWithModelData();


        // Set up listeners for period toggles
        periodToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle instanceof ToggleButton) {
                handlePeriodChange((ToggleButton) newToggle);
            }
        });
    }
}
