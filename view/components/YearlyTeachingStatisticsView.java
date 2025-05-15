package view.components;

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
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import view.BaseScreenView;
import src.controller.YearlyStatisticsController;
import src.controller.NavigationController;

import java.util.Arrays;
import java.util.List;
import java.time.Year; // Import Year class

public class YearlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    private ComboBox<Integer> yearComboBox; // Changed from fromYear/toYear
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherYearlyStatisticsModel> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;
    private Label yearHeaderLabel; // Added to allow updating year in table header
    private GridPane totalRow; // Added to store reference to total row

    // Data
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    // Dynamically generate years (e.g., 10 years around the current year)
    private ObservableList<Integer> years = FXCollections.observableArrayList();


    // Controller reference
    private YearlyStatisticsController controller;

    public YearlyTeachingStatisticsView() {
        super("Thống kê giờ giảng", "yearly-teaching");
        // Initialize the controller with this view
        this.controller = new YearlyStatisticsController(this);
        // Generate years upon instantiation
        generateYears();
        // Note: Initial data load is now triggered at the end of initializeView()
        // to ensure UI components are ready.
    }

    private void generateYears() {
        int currentYear = Year.now().getValue();
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(i);
        }
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

        // Set up event handlers
        setupEventHandlers();

        // Load data (controller will load data and update the view)
        // Call loadData here to ensure UI components like totalRow are initialized
        controller.loadData();
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
        // Period selection toggle group (kept for consistency if needed elsewhere, but year is default here)
        periodToggleGroup = new ToggleGroup();

        HBox periodTypeBox = new HBox(10);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("Loại:");
        typeLabel.setTextFill(Color.BLACK);
        typeLabel.setPrefWidth(50);

        // Keep toggles but handle navigation in their actions
        ToggleButton dayToggle = createToggleButton("Ngày", false, "teaching-statistics"); // Added viewId
        ToggleButton monthToggle = createToggleButton("Tháng", false, "monthly-teaching"); // Added viewId
        ToggleButton quarterToggle = createToggleButton("Quý", false, "quarterly-teaching"); // Added viewId
        ToggleButton yearToggle = createToggleButton("Năm", true, "yearly-teaching"); // Added viewId

        // Set toggle handlers to navigate
        dayToggle.setOnAction(e -> { if (dayToggle.isSelected()) navigateToView("teaching-statistics"); });
        monthToggle.setOnAction(e -> { if (monthToggle.isSelected()) navigateToView("monthly-teaching"); });
        quarterToggle.setOnAction(e -> { if (quarterToggle.isSelected()) navigateToView("quarterly-teaching"); });
        yearToggle.setOnAction(e -> { if (yearToggle.isSelected()) navigateToView("yearly-teaching"); }); // Navigating to self keeps you here


        // Ensure the correct toggle is selected when this view is active
        periodToggleGroup.selectToggle(yearToggle);


        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Single Year selection
        HBox yearSelectionBox = new HBox(10);
        yearSelectionBox.setAlignment(Pos.CENTER_LEFT);

        Label yearLabel = new Label("Năm:"); // Changed label
        yearLabel.setTextFill(Color.BLACK);
        yearComboBox = new ComboBox<>(); // Changed from fromYearComboBox
        yearComboBox.setPrefWidth(100);
        yearComboBox.setItems(years);
        yearComboBox.setValue(Year.now().getValue()); // Set default to current year


        yearSelectionBox.getChildren().addAll(yearLabel, yearComboBox); // Changed children

        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(statusOptions));
        statusComboBox.setValue("Tất cả");
        statusComboBox.setPrefWidth(120);

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
        filterBar.getChildren().addAll(periodTypeBox, yearSelectionBox, statusBox, actionButtonsBar); // Changed children

        root.getChildren().add(filterBar);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(600);
        statisticsTable.getStyleClass().add("statistics-table");
        // CONSTRAINED_RESIZE_POLICY is deprecated, consider alternative layout approaches
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create the STT column with black header text
        TableColumn<TeacherYearlyStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherYearlyStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false);

        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the Year column with dynamic header
        TableColumn<TeacherYearlyStatisticsModel, String> yearColumn = new TableColumn<>();

        yearHeaderLabel = new Label(String.valueOf(Year.now().getValue())); // Default to current year
        yearHeaderLabel.setTextFill(Color.BLACK);
        yearHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearColumn.setGraphic(yearHeaderLabel);

        // Year Sessions sub-column
        TableColumn<TeacherYearlyStatisticsModel, Integer> yearSessionsColumn = new TableColumn<>();
        yearSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("yearSessions"));
        yearSessionsColumn.setPrefWidth(80);
        yearSessionsColumn.setSortable(false);

        Label yearSessionsHeaderLabel = new Label("Buổi");
        yearSessionsHeaderLabel.setTextFill(Color.BLACK);
        yearSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearSessionsColumn.setGraphic(yearSessionsHeaderLabel);

        // Year Hours sub-column
        TableColumn<TeacherYearlyStatisticsModel, Double> yearHoursColumn = new TableColumn<>();
        yearHoursColumn.setCellValueFactory(new PropertyValueFactory<>("yearHours"));
        yearHoursColumn.setPrefWidth(80);
        yearHoursColumn.setSortable(false);

        Label yearHoursHeaderLabel = new Label("Giờ");
        yearHoursHeaderLabel.setTextFill(Color.BLACK);
        yearHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearHoursColumn.setGraphic(yearHoursHeaderLabel);

        yearColumn.getColumns().addAll(yearSessionsColumn, yearHoursColumn);

        // Create the Total column
        TableColumn<TeacherYearlyStatisticsModel, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total Sessions sub-column
        TableColumn<TeacherYearlyStatisticsModel, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total Hours sub-column
        TableColumn<TeacherYearlyStatisticsModel, Double> totalHoursColumn = new TableColumn<>();
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setPrefWidth(80);
        totalHoursColumn.setSortable(false);

        Label totalHoursHeaderLabel = new Label("Giờ");
        totalHoursHeaderLabel.setTextFill(Color.BLACK);
        totalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeaderLabel);

        totalColumn.getColumns().addAll(totalSessionsColumn, totalHoursColumn);

        // Add all columns to the table
        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, yearColumn, totalColumn);

        // Create a VBox to hold the table and the total row
        VBox tableContainer = new VBox(10);
        tableContainer.getChildren().add(statisticsTable);

        // Create the total row using a grid pane for alignment with the table
        totalRow = createTotalRow(); // Initialize totalRow here
        tableContainer.getChildren().add(totalRow);

        root.getChildren().add(tableContainer);
    }

    private GridPane createTotalRow() {
        GridPane totalRow = new GridPane();
        totalRow.setPadding(new Insets(10, 0, 0, 0));
        totalRow.setHgap(10);

        // Column constraints to align with the table columns
        ColumnConstraints sttColumnConstraint = new ColumnConstraints();
        sttColumnConstraint.setPrefWidth(60);

        ColumnConstraints nameColumnConstraint = new ColumnConstraints();
        nameColumnConstraint.setPrefWidth(150);

        ColumnConstraints yearSessionsColumnConstraint = new ColumnConstraints();
        yearSessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints yearHoursColumnConstraint = new ColumnConstraints();
        yearHoursColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalSessionsColumnConstraint = new ColumnConstraints();
        totalSessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalHoursColumnConstraint = new ColumnConstraints();
        totalHoursColumnConstraint.setPrefWidth(80);

        totalRow.getColumnConstraints().addAll(
                sttColumnConstraint,
                nameColumnConstraint,
                yearSessionsColumnConstraint,
                yearHoursColumnConstraint,
                totalSessionsColumnConstraint,
                totalHoursColumnConstraint
        );

        // Create and add the "Tổng cộng" label
        Label totalLabel = new Label("Tổng cộng");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnSpan(totalLabel, 2); // Span across STT and Name columns
        totalRow.add(totalLabel, 0, 0);

        // Add the total values (these will be updated by the controller)
        // Assign IDs to these labels for easier lookup later
        Label yearSessionTotal = new Label("0");
        yearSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        yearSessionTotal.setId("yearSessionTotalLabel"); // Assign ID
        totalRow.add(yearSessionTotal, 2, 0); // Column index 2 for Year Sessions

        Label yearHoursTotal = new Label("0.0");
        yearHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        yearHoursTotal.setId("yearHoursTotalLabel"); // Assign ID
        totalRow.add(yearHoursTotal, 3, 0); // Column index 3 for Year Hours

        // Add the total values for the "Tổng cộng" column - these will show the same data
        // as the year totals in this single-year view context.
        Label overallTotalSessionTotal = new Label("0");
        overallTotalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        overallTotalSessionTotal.setId("overallTotalSessionTotalLabel"); // Assign ID
        totalRow.add(overallTotalSessionTotal, 4, 0); // Column index 4 for Total Sessions

        Label overallTotalHoursTotal = new Label("0.0");
        overallTotalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        overallTotalHoursTotal.setId("overallTotalHoursTotalLabel"); // Assign ID
        totalRow.add(overallTotalHoursTotal, 5, 0); // Column index 5 for Total Hours

        return totalRow;
    }

    private ToggleButton createToggleButton(String text, boolean selected, String viewId) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setToggleGroup(periodToggleGroup);
        toggleButton.setSelected(selected);
        toggleButton.setPrefHeight(30);
        toggleButton.setPrefWidth(80);
        // Store viewId in user data to retrieve it in handler
        toggleButton.setUserData(viewId);
        return toggleButton;
    }


    private Button createActionButton(String text, String iconStyle) {
        Button button = new Button(text);
        button.setPrefSize(40, 40);
        return button;
    }

    private void setupEventHandlers() {
        // Search button handler now gets a single year and calls controller.handleSearch
        searchButton.setOnAction(e -> controller.handleSearch(
                yearComboBox.getValue(), // Get selected year
                statusComboBox.getValue()
        ));

        // Add listener to the year ComboBox to trigger search when year changes
        if (yearComboBox != null) {
            yearComboBox.setOnAction(event -> controller.handleSearch(
                    yearComboBox.getValue(),
                    statusComboBox.getValue()
            ));
        }

        // Add listener to the status ComboBox to trigger search when status changes
        if (statusComboBox != null) {
            statusComboBox.setOnAction(event -> controller.handleSearch(
                    yearComboBox.getValue(),
                    statusComboBox.getValue()
            ));
        }


        exportExcelButton.setOnAction(e -> controller.handleExportExcel());
        exportPdfButton.setOnAction(e -> controller.handleExportPdf());
        printButton.setOnAction(e -> controller.handlePrint());
    }

    @Override
    public void refreshView() {
        // Delegate to controller, ensuring the current selected filters are used
        // Call loadData without arguments
        controller.loadData();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Restore the correct toggle button state
        Toggle yearToggle = null;
        for (Toggle toggle : periodToggleGroup.getToggles()) {
            if (toggle instanceof ToggleButton) {
                ToggleButton tb = (ToggleButton) toggle;
                // Check against the viewId stored in UserData
                if ("yearly-teaching".equals(tb.getUserData())) {
                    yearToggle = tb;
                    break;
                }
            }
        }

        if (yearToggle != null) {
            periodToggleGroup.selectToggle(yearToggle);
        }

        // Load data for the initially selected year and status when the view becomes active
        // Call loadData without arguments
        controller.loadData();
    }

    @Override
    public boolean onDeactivate() {
        super.onDeactivate();

        // Clean up resources (if any specific to this view's deactivation)
        // controller.cleanup(); // Assuming cleanup is handled by the controller's lifecycle

        return true;
    }

    // Data-related methods

    /**
     * Set the table data from the controller
     */
    public void setTableData(ObservableList<TeacherYearlyStatisticsModel> data) {
        statisticsTable.setItems(data);
    }

    /**
     * Update the summary row with totals
     * Now finds labels by ID for robustness.
     */
    public void updateSummary(StatisticsSummaryModel summary) {
        if (totalRow != null) {
            // Find labels by ID instead of index
            Label yearSessionTotalLabel = (Label) totalRow.lookup("#yearSessionTotalLabel");
            Label yearHoursTotalLabel = (Label) totalRow.lookup("#yearHoursTotalLabel");
            Label overallTotalSessionTotalLabel = (Label) totalRow.lookup("#overallTotalSessionTotalLabel");
            Label overallTotalHoursTotalLabel = (Label) totalRow.lookup("#overallTotalHoursTotalLabel");

            // Check if labels were found before updating
            if (yearSessionTotalLabel != null) {
                yearSessionTotalLabel.setText(String.valueOf(summary.getYearSessions()));
            } else {
                System.err.println("Label with ID 'yearSessionTotalLabel' not found in totalRow.");
            }
            if (yearHoursTotalLabel != null) {
                yearHoursTotalLabel.setText(String.format("%.1f", summary.getYearHours()));
            } else {
                System.err.println("Label with ID 'yearHoursTotalLabel' not found in totalRow.");
            }
            if (overallTotalSessionTotalLabel != null) {
                overallTotalSessionTotalLabel.setText(String.valueOf(summary.getTotalSessions()));
            } else {
                System.err.println("Label with ID 'overallTotalSessionTotalLabel' not found in totalRow.");
            }
            if (overallTotalHoursTotalLabel != null) {
                overallTotalHoursTotalLabel.setText(String.format("%.1f", summary.getTotalHours()));
            } else {
                System.err.println("Label with ID 'overallTotalHoursTotalLabel' not found in totalRow.");
            }

        } else {
            System.err.println("Total row GridPane is null. Cannot update summary.");
        }
    }

    /**
     * Update the year label in the table header
     */
    public void updateYearLabel(int year) {
        if (yearHeaderLabel != null) {
            yearHeaderLabel.setText(String.valueOf(year));
        }
    }

    /**
     * Show exporting message
     */
    public void showExportingMessage(String format) {
        // In a real application, this would show a toast or dialog
        System.out.println("Exporting to " + format + "...");
        showAlert("Thông báo", "Đang xuất file " + format + "...", Alert.AlertType.INFORMATION);
    }

    /**
     * Show printing message
     */
    public void showPrintingMessage() {
        // In a real application, this would show a toast or dialog
        System.out.println("Preparing to print...");
        showAlert("Thông báo", "Đang chuẩn bị in...", Alert.AlertType.INFORMATION);
    }

    /**
     * Navigate to another view
     */
    public void navigateToView(String viewId) {
        NavigationController navController = getNavigationController();
        if (navController != null) {
            navController.navigateTo(viewId);
        } else {
            System.err.println("Navigation controller is null. Cannot navigate to " + viewId);
            showAlert("Lỗi hệ thống", "Không thể thực hiện điều hướng.", Alert.AlertType.ERROR);
        }
    }

    /**
     * Helper method to show alerts.
     */
    public void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Getter for selected year for controller
    public int getSelectedYear() {
        return yearComboBox.getValue();
    }

    // Getter for selected status for controller
    public String getSelectedStatus() {
        return statusComboBox.getValue();
    }
}
