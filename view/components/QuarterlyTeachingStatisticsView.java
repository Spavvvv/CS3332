package view.components;

import src.controller.NavigationController;
import src.controller.QuarterlyTeachingStatisticsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QuarterlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    public ComboBox<Integer> yearComboBox; // Changed from range
    private ToggleGroup periodToggleGroup;
    public ComboBox<String> statusComboBox;
    private TableView<TeacherQuarterlyStatisticsModel> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;

    // Toggle buttons
    public ToggleButton dayToggle;
    public ToggleButton monthToggle;
    private ToggleButton quarterToggle;
    public ToggleButton yearToggle;

    // Summary row components - Adjusted for Current Quarter and Annual Total
    private Label currentQuarterSessionTotal; // Total sessions for the displayed quarter
    private Label currentQuarterHoursTotal;   // Total hours for the displayed quarter
    private Label annualTotalSessionTotal;    // Annual total sessions
    private Label annualTotalHoursTotal;      // Annual total hours

    // Controller
    private QuarterlyTeachingStatisticsController controller;

    // Year range for year combo box
    private final int START_YEAR = 2023;
    private final int END_YEAR = Year.now().getValue() + 5; // Show current year + 5 years

    public QuarterlyTeachingStatisticsView() {
        super("Thống kê giờ giảng", "quarterly-teaching");
    }

    @Override
    public void initializeView() {
        // clear root
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

        // Initialize controller AFTER UI is set up and components are created
        controller = new QuarterlyTeachingStatisticsController(this);

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
        quarterToggle = createToggleButton("Quý", false); // Initial selection handled in onActivate
        yearToggle = createToggleButton("Năm", false);

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Year selection (single year, not a range)
        HBox yearSelectionBox = new HBox(10);
        yearSelectionBox.setAlignment(Pos.CENTER_LEFT);

        Label yearLabel = new Label("Năm:");
        yearLabel.setTextFill(Color.BLACK);

        yearComboBox = new ComboBox<>();
        yearComboBox.setPrefWidth(80);
        List<Integer> years = IntStream.rangeClosed(START_YEAR, END_YEAR).boxed().collect(Collectors.toList());
        yearComboBox.setItems(FXCollections.observableArrayList(years));
        // Default to current year in onActivate

        yearSelectionBox.getChildren().addAll(yearLabel, yearComboBox);

        // Status dropdown
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        statusComboBox = new ComboBox<>();
        statusComboBox.setPrefWidth(120);
        // Status options will be set by the controller

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Search button
        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefSize(100, 30);
        searchButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");

        // Creating top horizontal bar for filters
        // Removed the 'Từ' and 'Đến' quarter/year selectors
        filterBar = new HBox(20);
        filterBar.setPadding(new Insets(10));
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(periodTypeBox, yearSelectionBox, statusBox, searchButton);
        filterBar.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 1px;");
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setPrefHeight(400); // Adjusted height
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create the STT column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false); // Often teacher names are not sorted in reports

        Label teacherHeaderLabel = new Label("Giảng viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the CURRENT QUARTER column (will dynamically update graphic and data binding)
        TableColumn<TeacherQuarterlyStatisticsModel, String> currentQuarterDisplayColumn = new TableColumn<>();
        // Initial graphic (will be updated in updateTableData)
        Label quarterHeaderLabel = new Label("Quý");
        quarterHeaderLabel.setTextFill(Color.BLACK);
        quarterHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currentQuarterDisplayColumn.setGraphic(quarterHeaderLabel);

        // Sessions sub-column for the current quarter (binding set dynamically)
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> currentQuarterSessionsColumn = new TableColumn<>();
        currentQuarterSessionsColumn.setPrefWidth(80);
        currentQuarterSessionsColumn.setSortable(false);

        Label sessionsHeaderLabel = new Label("Buổi");
        sessionsHeaderLabel.setTextFill(Color.BLACK);
        sessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currentQuarterSessionsColumn.setGraphic(sessionsHeaderLabel);

        // Hours sub-column for the current quarter (binding set dynamically)
        TableColumn<TeacherQuarterlyStatisticsModel, Double> currentQuarterHoursColumn = new TableColumn<>();
        currentQuarterHoursColumn.setPrefWidth(80);
        currentQuarterHoursColumn.setSortable(false);

        Label hoursHeaderLabel = new Label("Giờ");
        hoursHeaderLabel.setTextFill(Color.BLACK);
        hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currentQuarterHoursColumn.setGraphic(hoursHeaderLabel);

        currentQuarterDisplayColumn.getColumns().addAll(currentQuarterSessionsColumn, currentQuarterHoursColumn);

        // Create the ANNUAL TOTAL column
        TableColumn<TeacherQuarterlyStatisticsModel, String> annualTotalColumn = new TableColumn<>();

        Label annualTotalHeaderLabel = new Label("Tổng cộng");
        annualTotalHeaderLabel.setTextFill(Color.BLACK);
        annualTotalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualTotalColumn.setGraphic(annualTotalHeaderLabel);

        // Total sessions sub-column (annual)
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> annualTotalSessionsColumn = new TableColumn<>();
        annualTotalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions")); // Binds to annual total in model
        annualTotalSessionsColumn.setPrefWidth(80);
        annualTotalSessionsColumn.setSortable(false);

        Label annualTotalSessionsHeaderLabel = new Label("Buổi");
        annualTotalSessionsHeaderLabel.setTextFill(Color.BLACK);
        annualTotalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualTotalSessionsColumn.setGraphic(annualTotalSessionsHeaderLabel);

        // Total hours sub-column (annual)
        TableColumn<TeacherQuarterlyStatisticsModel, Double> annualTotalHoursColumn = new TableColumn<>();
        annualTotalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours")); // Binds to annual total in model
        annualTotalHoursColumn.setPrefWidth(80);
        annualTotalHoursColumn.setSortable(false);

        Label annualTotalHoursHeaderLabel = new Label("Giờ");
        annualTotalHoursHeaderLabel.setTextFill(Color.BLACK);
        annualTotalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualTotalHoursColumn.setGraphic(annualTotalHoursHeaderLabel);

        annualTotalColumn.getColumns().addAll(annualTotalSessionsColumn, annualTotalHoursColumn);

        // Add columns to the table: STT, Teacher, Current Quarter, Annual Total
        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, currentQuarterDisplayColumn, annualTotalColumn);

        // Add the table to the root layout (it's already in tableContainer, which is added to root)
    }

    private GridPane createTotalRow() {
        GridPane totalRow = new GridPane();
        totalRow.setPadding(new Insets(5));
        totalRow.setStyle("-fx-border-color: #ddd; -fx-background-color: #f9f9f9;");

        // Define column constraints to align with table columns
        // We need columns for STT, Teacher, Current Quarter Sessions, Current Quarter Hours, Annual Total Sessions, Annual Total Hours
        ColumnConstraints sttConstraint = new ColumnConstraints();
        sttConstraint.setPrefWidth(60);

        ColumnConstraints nameConstraint = new ColumnConstraints();
        nameConstraint.setPrefWidth(150);

        ColumnConstraints quarterSessionsConstraint = new ColumnConstraints();
        quarterSessionsConstraint.setPrefWidth(80);

        ColumnConstraints quarterHoursConstraint = new ColumnConstraints();
        quarterHoursConstraint.setPrefWidth(80);

        ColumnConstraints totalSessionsConstraint = new ColumnConstraints();
        totalSessionsConstraint.setPrefWidth(80);

        ColumnConstraints totalHoursConstraint = new ColumnConstraints();
        totalHoursConstraint.setPrefWidth(80);


        totalRow.getColumnConstraints().addAll(
                sttConstraint,
                nameConstraint,
                quarterSessionsConstraint,
                quarterHoursConstraint,
                totalSessionsConstraint,
                totalHoursConstraint
        );

        // Create and add the "Tổng cộng" label for the footer (spans across STT and Teacher columns)
        Label footerTotalLabel = new Label("Tổng cộng");
        footerTotalLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(footerTotalLabel, 0);
        GridPane.setColumnSpan(footerTotalLabel, 2);
        GridPane.setHalignment(footerTotalLabel, javafx.geometry.HPos.LEFT);


        // Create and add the current quarter totals labels
        currentQuarterSessionTotal = new Label("0"); // Placeholder for current quarter sessions total
        currentQuarterSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        currentQuarterSessionTotal.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(currentQuarterSessionTotal, 2);
        GridPane.setHalignment(currentQuarterSessionTotal, javafx.geometry.HPos.CENTER);

        currentQuarterHoursTotal = new Label("0.0"); // Placeholder for current quarter hours total
        currentQuarterHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        currentQuarterHoursTotal.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(currentQuarterHoursTotal, 3);
        GridPane.setHalignment(currentQuarterHoursTotal, javafx.geometry.HPos.CENTER);

        // Create and add the annual total labels
        annualTotalSessionTotal = new Label("0"); // Placeholder for annual total sessions
        annualTotalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        annualTotalSessionTotal.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(annualTotalSessionTotal, 4);
        GridPane.setHalignment(annualTotalSessionTotal, javafx.geometry.HPos.CENTER);

        annualTotalHoursTotal = new Label("0.0"); // Placeholder for annual total hours
        annualTotalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        annualTotalHoursTotal.setAlignment(Pos.CENTER);
        GridPane.setColumnIndex(annualTotalHoursTotal, 5);
        GridPane.setHalignment(annualTotalHoursTotal, javafx.geometry.HPos.CENTER);

        // Add all labels to the total row
        totalRow.getChildren().addAll(
                footerTotalLabel,
                currentQuarterSessionTotal,
                currentQuarterHoursTotal,
                annualTotalSessionTotal,
                annualTotalHoursTotal
        );

        return totalRow;
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

    private Button createActionButton(String text) {
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
        if (controller != null) {
            // Ensure year selector is set to current year on refresh
            yearComboBox.setValue(Year.now().getValue());
            // Trigger the search for the current year and current quarter
            handleSearch();
        }
        // Ensure "Quý" toggle is selected when this view is refreshed
        quarterToggle.setSelected(true);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Set up event handlers - only do this once when the view is activated
        // Handlers are now set by the controller
        if (controller != null) {
            controller.setupEventHandlers();
        }


        // Check saved view type or default to "Quý"
        String savedViewType = navigationController.getSavedToggleState("view_type");

        if (savedViewType != null && !savedViewType.equals("Quý")) {
            // If a different view type was saved, navigate to that view
            handlePeriodChange(periodToggleGroup.getToggles().stream()
                    .filter(toggle -> ((ToggleButton)toggle).getText().equals(savedViewType))
                    .map(toggle -> (ToggleButton)toggle)
                    .findFirst().orElse(quarterToggle)); // Default to quarter toggle if not found
            return; // Exit onActivate as we're navigating away
        }

        // If saved type is "Quý" or no type saved, ensure "Quý" is selected and load data
        quarterToggle.setSelected(true);
        navigationController.saveToggleState("view_type", "Quý"); // Ensure "Quý" is the saved state

        // Set initial year selector to the current year
        yearComboBox.setValue(Year.now().getValue());

        // Load data for the current quarter of the current year
        if (controller != null) {
            controller.loadInitialData();
        }

        // Set up listeners for period toggles
        periodToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null && newToggle instanceof ToggleButton) {
                handlePeriodChange((ToggleButton) newToggle);
            }
        });
    }

    // Event handlers (now called by the controller)
    public void handleSearch() {
        Integer selectedYear = yearComboBox.getValue();
        String status = statusComboBox.getValue();

        if (selectedYear == null || status == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn năm và trạng thái.", Alert.AlertType.WARNING);
            return;
        }

        // Determine the current quarter number based on the current date
        int currentQuarterNumber = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

        // Delegate search to controller for the specified year and current quarter
        boolean success = controller.searchStatistics(selectedYear, currentQuarterNumber, status);

        if (!success) {
            showAlert("Lỗi kết nối hoặc truy vấn", "Không thể tải dữ liệu. Vui lòng kiểm tra kết nối cơ sở dữ liệu.", Alert.AlertType.ERROR);
            // The controller should handle clearing the model data on error
        }
        // Update table is called within the controller's searchStatistics method after data is loaded/cleared
    }


    public void handleExportExcel() {
        showSuccess("Đang xuất file Excel...");
        // Implement actual export logic here or in controller
    }

    public void handleExportPdf() {
        showSuccess("Đang xuất file PDF...");
        // Implement actual export logic here or in controller
    }

    public void handlePrint() {
        showSuccess("Đang chuẩn bị in...");
        // Implement actual print logic here or in controller
    }

    // Helper method to show a success notification (replace with actual UI feedback)
    public void showSuccess(String message) {
        System.out.println("INFO: " + message); // Placeholder for actual UI notification
        // Example: Using a simple Alert for now
        // showAlert("Thông báo", message, Alert.AlertType.INFORMATION);
    }


    // Helper for showing Alerts
    public void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void handlePeriodChange(ToggleButton selected) {
        if (selected != null) {
            String viewType = selected.getText();
            navigationController.saveToggleState("view_type", viewType);

            // Navigate to the corresponding view based on the selected toggle
            if (viewType.equals("Ngày")) {
                navigationController.navigateTo("teaching-statistics"); // Assuming Daily view ID
            } else if (viewType.equals("Tháng")) {
                navigationController.navigateTo("monthly-teaching"); // Assuming Monthly view ID
            } else if (viewType.equals("Năm")) {
                navigationController.navigateTo("yearly-teaching"); // Assuming Yearly view ID
            }
            // If "Quý" is selected, stay on this view
        }
    }


    // Public setters for controller to update view
    // Removed setQuartersList as we no longer have quarter range selectors

    public void setStatusOptions(List<String> statusOptions) {
        statusComboBox.setItems(FXCollections.observableArrayList(statusOptions));
        if (!statusOptions.isEmpty()) {
            statusComboBox.setValue(statusOptions.get(0)); // Set default value if options exist
        } else {
            statusComboBox.setValue(null); // No options, set to null
        }
    }

    public void updateTableData(ObservableList<TeacherQuarterlyStatisticsModel> data) {
        statisticsTable.setItems(data);
        updateTableColumnHeadersAndBinding(); // Update headers and binding based on current quarter
        updateSummaryRow(); // Update summary row based on the data
    }

    // Method to dynamically update column headers and cell value factories
    private void updateTableColumnHeadersAndBinding() {
        int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
        int selectedYear = yearComboBox.getValue();

        // Find the column designated for the current quarter display
        for (TableColumn<TeacherQuarterlyStatisticsModel, ?> column : statisticsTable.getColumns()) {
            if (column.getGraphic() instanceof Label) {
                Label headerLabel = (Label) column.getGraphic();
                if (headerLabel.getText().startsWith("Quý") || "Quý".equals(headerLabel.getText())) { // Identify the 'currentQuarterDisplayColumn'
                    // Update header text
                    headerLabel.setText("Quý " + currentQuarter + "/" + selectedYear);

                    // Find the sub-columns for sessions and hours under this quarter column
                    for (TableColumn<TeacherQuarterlyStatisticsModel, ?> subColumn : column.getColumns()) {
                        if (subColumn.getGraphic() instanceof Label) {
                            Label subHeaderLabel = (Label) subColumn.getGraphic();
                            if (subHeaderLabel.getText().equals("Buổi")) {
                                // Set the cell value factory based on the current quarter
                                switch (currentQuarter) {
                                    case 1:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q1Sessions"));
                                        break;
                                    case 2:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q2Sessions"));
                                        break;
                                    case 3:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q3Sessions"));
                                        break;
                                    case 4:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q4Sessions"));
                                        break;
                                }
                            } else if (subHeaderLabel.getText().equals("Giờ")) {
                                // Set the cell value factory based on the current quarter
                                switch (currentQuarter) {
                                    case 1:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q1Hours"));
                                        break;
                                    case 2:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q2Hours"));
                                        break;
                                    case 3:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q3Hours"));
                                        break;
                                    case 4:
                                        ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) subColumn).setCellValueFactory(new PropertyValueFactory<>("q4Hours"));
                                        break;
                                }
                            }
                        }
                    }
                    break; // Found and updated the quarter column, exit loop
                }
            }
        }
    }


    // Updated to calculate totals from the current table data
    public void updateSummaryRow() {
        int totalCurrentQuarterSessions = 0;
        double totalCurrentQuarterHours = 0.0;
        int totalAnnualSessions = 0;
        double totalAnnualHours = 0.0;

        int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

        ObservableList<TeacherQuarterlyStatisticsModel> items = statisticsTable.getItems();
        if (items != null) {
            for (TeacherQuarterlyStatisticsModel item : items) {
                // Accumulate current quarter totals
                switch (currentQuarter) {
                    case 1:
                        totalCurrentQuarterSessions += item.getQ1Sessions();
                        totalCurrentQuarterHours += item.getQ1Hours();
                        break;
                    case 2:
                        totalCurrentQuarterSessions += item.getQ2Sessions();
                        totalCurrentQuarterHours += item.getQ2Hours();
                        break;
                    case 3:
                        totalCurrentQuarterSessions += item.getQ3Sessions();
                        totalCurrentQuarterHours += item.getQ3Hours();
                        break;
                    case 4:
                        totalCurrentQuarterSessions += item.getQ4Sessions();
                        totalCurrentQuarterHours += item.getQ4Hours();
                        break;
                }
                // Accumulate annual totals (using total properties from the model)
                totalAnnualSessions += item.getTotalSessions();
                totalAnnualHours += item.getTotalHours();
            }
        }

        // Update the labels
        currentQuarterSessionTotal.setText(String.valueOf(totalCurrentQuarterSessions));
        currentQuarterHoursTotal.setText(String.format("%.1f", totalCurrentQuarterHours)); // Format hours

        annualTotalSessionTotal.setText(String.valueOf(totalAnnualSessions));
        annualTotalHoursTotal.setText(String.format("%.1f", totalAnnualHours)); // Format hours
    }

    // Getters for controller to retrieve view state
    public int getSelectedYear() {
        Integer year = yearComboBox.getValue();
        return year != null ? year : Year.now().getValue(); // Default to current year if null
    }

    public int getCurrentQuarter() {
        return (LocalDate.now().getMonthValue() - 1) / 3 + 1;
    }

    public String getSelectedStatus() {
        return statusComboBox.getValue();
    }

    // Removed getFromQuarter, getFromYear, getToQuarter, getToYear

    public boolean isDayToggleSelected() {
        return dayToggle.isSelected();
    }

    public boolean isMonthToggleSelected() {
        return monthToggle.isSelected();
    }

    public boolean isQuarterToggleSelected() {
        return quarterToggle.isSelected();
    }

    public boolean isYearToggleSelected() {
        return yearToggle.isSelected();
    }

    public void selectToggleByState(String state) {
        switch (state) {
            case "Ngày":
                dayToggle.setSelected(true);
                break;
            case "Tháng":
                monthToggle.setSelected(true);
                break;
            case "Quý":
                quarterToggle.setSelected(true);
                break;
            case "Năm":
                yearToggle.setSelected(true);
                break;
            default:
                quarterToggle.setSelected(true);
                break;
        }
    }

    // Event handler setters (for controller to inject handlers)
    public void setSearchButtonHandler(EventHandler<ActionEvent> handler) {
        searchButton.setOnAction(handler);
    }

    public void setExportExcelButtonHandler(EventHandler<ActionEvent> handler) {
        exportExcelButton.setOnAction(handler);
    }

    public void setExportPdfButtonHandler(EventHandler<ActionEvent> handler) {
        exportPdfButton.setOnAction(handler);
    }

    public void setPrintButtonHandler(EventHandler<ActionEvent> handler) {
        printButton.setOnAction(handler);
    }

    public void setDayToggleHandler(EventHandler<ActionEvent> handler) {
        dayToggle.setOnAction(handler);
    }

    public void setMonthToggleHandler(EventHandler<ActionEvent> handler) {
        monthToggle.setOnAction(handler);
    }

    public void setQuarterToggleHandler(EventHandler<ActionEvent> handler) {
        quarterToggle.setOnAction(handler);
    }

    public void setYearToggleHandler(EventHandler<ActionEvent> handler) {
        yearToggle.setOnAction(handler);
    }
}
