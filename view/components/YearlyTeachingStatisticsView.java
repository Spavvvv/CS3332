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

public class YearlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    private ComboBox<Integer> fromYearComboBox;
    private ComboBox<Integer> toYearComboBox;
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
    private final List<Integer> years = Arrays.asList(2023, 2024, 2025, 2026);

    // Controller reference
    private YearlyStatisticsController controller;

    public YearlyTeachingStatisticsView() {
        super("Thống kê giờ giảng", "yearly-teaching");
        this.controller = new YearlyStatisticsController(this);
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
        // Period selection toggle group
        periodToggleGroup = new ToggleGroup();

        HBox periodTypeBox = new HBox(10);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label("Loại:");
        typeLabel.setTextFill(Color.BLACK);
        typeLabel.setPrefWidth(50);

        ToggleButton dayToggle = createToggleButton("Ngày", false);
        ToggleButton monthToggle = createToggleButton("Tháng", false);
        ToggleButton quarterToggle = createToggleButton("Quý", false);
        ToggleButton yearToggle = createToggleButton("Năm", true);

        // Set toggle handlers
        dayToggle.setOnAction(e -> {
            if (dayToggle.isSelected()) controller.handlePeriodChange(dayToggle);
        });

        monthToggle.setOnAction(e -> {
            if (monthToggle.isSelected()) controller.handlePeriodChange(monthToggle);
        });

        quarterToggle.setOnAction(e -> {
            if (quarterToggle.isSelected()) controller.handlePeriodChange(quarterToggle);
        });

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Year selection for date range
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);
        fromYearComboBox = new ComboBox<>();
        fromYearComboBox.setPrefWidth(100);
        fromYearComboBox.setItems(FXCollections.observableArrayList(years));
        fromYearComboBox.setValue(2025);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);
        toYearComboBox = new ComboBox<>();
        toYearComboBox.setPrefWidth(100);
        toYearComboBox.setItems(FXCollections.observableArrayList(years));
        toYearComboBox.setValue(2025);

        dateRangeBox.getChildren().addAll(fromLabel, fromYearComboBox, toLabel, toYearComboBox);

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
        filterBar.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, actionButtonsBar);

        root.getChildren().add(filterBar);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(600);
        statisticsTable.getStyleClass().add("statistics-table");
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

        // Create the 2025 column with black header text
        TableColumn<TeacherYearlyStatisticsModel, String> yearColumn = new TableColumn<>();

        yearHeaderLabel = new Label("2025");
        yearHeaderLabel.setTextFill(Color.BLACK);
        yearHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearColumn.setGraphic(yearHeaderLabel);

        // 2025 Sessions sub-column with black header text
        TableColumn<TeacherYearlyStatisticsModel, Integer> yearSessionsColumn = new TableColumn<>();
        yearSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("yearSessions"));
        yearSessionsColumn.setPrefWidth(80);
        yearSessionsColumn.setSortable(false);

        Label yearSessionsHeaderLabel = new Label("Buổi");
        yearSessionsHeaderLabel.setTextFill(Color.BLACK);
        yearSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearSessionsColumn.setGraphic(yearSessionsHeaderLabel);

        // 2025 Hours sub-column with black header text
        TableColumn<TeacherYearlyStatisticsModel, Double> yearHoursColumn = new TableColumn<>();
        yearHoursColumn.setCellValueFactory(new PropertyValueFactory<>("yearHours"));
        yearHoursColumn.setPrefWidth(80);
        yearHoursColumn.setSortable(false);

        Label yearHoursHeaderLabel = new Label("Giờ");
        yearHoursHeaderLabel.setTextFill(Color.BLACK);
        yearHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearHoursColumn.setGraphic(yearHoursHeaderLabel);

        yearColumn.getColumns().addAll(yearSessionsColumn, yearHoursColumn);

        // Create the Total column with black header text
        TableColumn<TeacherYearlyStatisticsModel, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total Sessions sub-column with black header text
        TableColumn<TeacherYearlyStatisticsModel, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total Hours sub-column with black header text
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
        totalRow = createTotalRow();
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
        GridPane.setColumnSpan(totalLabel, 2);
        totalRow.add(totalLabel, 0, 0);

        // Add the total values (these will be updated by the controller)
        Label yearSessionTotal = new Label("0");
        yearSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(yearSessionTotal, 2, 0);

        Label yearHoursTotal = new Label("0.0");
        yearHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(yearHoursTotal, 3, 0);

        Label totalSessionTotal = new Label("0");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(totalSessionTotal, 4, 0);

        Label totalHoursTotal = new Label("0.0");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(totalHoursTotal, 5, 0);

        return totalRow;
    }

    private ToggleButton createToggleButton(String text, boolean selected) {
        ToggleButton toggleButton = new ToggleButton(text);
        toggleButton.setToggleGroup(periodToggleGroup);
        toggleButton.setSelected(selected);
        toggleButton.setPrefHeight(30);
        toggleButton.setPrefWidth(80);
        return toggleButton;
    }

    private Button createActionButton(String text, String iconStyle) {
        Button button = new Button(text);
        button.setPrefSize(40, 40);
        return button;
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(e -> controller.handleSearch(
                fromYearComboBox.getValue(),
                toYearComboBox.getValue(),
                statusComboBox.getValue()
        ));
        exportExcelButton.setOnAction(e -> controller.handleExportExcel());
        exportPdfButton.setOnAction(e -> controller.handleExportPdf());
        printButton.setOnAction(e -> controller.handlePrint());
    }

    @Override
    public void refreshView() {
        // Delegate to controller
        controller.loadData();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Restore the correct toggle button state if necessary
        Toggle yearToggle = null;
        for (Toggle toggle : periodToggleGroup.getToggles()) {
            if (toggle instanceof ToggleButton) {
                ToggleButton tb = (ToggleButton) toggle;
                if (tb.getText().equals("Năm")) {
                    yearToggle = tb;
                    break;
                }
            }
        }

        if (yearToggle != null) {
            periodToggleGroup.selectToggle(yearToggle);
        }

        // Ensure data is up to date
        refreshView();
    }

    @Override
    public boolean onDeactivate() {
        super.onDeactivate();

        // Clean up resources
        controller.cleanup();

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
     */
    public void updateSummary(StatisticsSummaryModel summary) {
        // Update the totals in the UI
        Label yearSessionTotal = (Label) totalRow.getChildren().get(2);
        yearSessionTotal.setText(String.valueOf(summary.getYearSessions()));

        Label yearHoursTotal = (Label) totalRow.getChildren().get(3);
        yearHoursTotal.setText(String.format("%.1f", summary.getYearHours()));

        Label totalSessionTotal = (Label) totalRow.getChildren().get(4);
        totalSessionTotal.setText(String.valueOf(summary.getTotalSessions()));

        Label totalHoursTotal = (Label) totalRow.getChildren().get(5);
        totalHoursTotal.setText(String.format("%.1f", summary.getTotalHours()));
    }

    /**
     * Update the year label in the table header
     */
    public void updateYearLabel(int year) {
        yearHeaderLabel.setText(String.valueOf(year));
    }

    /**
     * Show exporting message
     */
    public void showExportingMessage(String format) {
        // In a real application, this would show a toast or dialog
        System.out.println("Exporting to " + format + "...");
    }

    /**
     * Show printing message
     */
    public void showPrintingMessage() {
        // In a real application, this would show a toast or dialog
        System.out.println("Preparing to print...");
    }

    /**
     * Navigate to another view
     */
    public void navigateToView(String viewId) {
        NavigationController navController = getNavigationController();
        if (navController != null) {
            navController.navigateTo(viewId);
        } else {
            System.out.println("Navigation controller is null");
        }
    }
}