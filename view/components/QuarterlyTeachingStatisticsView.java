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

import java.time.Year;
import java.util.List;

public class QuarterlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    private ComboBox<Integer> fromQuarterComboBox;
    private ComboBox<Integer> fromYearComboBox;
    private ComboBox<Integer> toQuarterComboBox;
    private ComboBox<Integer> toYearComboBox;
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherQuarterlyStatisticsModel> statisticsTable;
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

    // Summary row components
    private Label q1SessionTotal;
    private Label q1HoursTotal;
    private Label q2SessionTotal;
    private Label q2HoursTotal;
    private Label totalSessionTotal;
    private Label totalHoursTotal;

    // Controller
    private QuarterlyTeachingStatisticsController controller;

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

        // Initialize controller after UI is set up
        controller = new QuarterlyTeachingStatisticsController(this);
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

        dayToggle = createToggleButton("Ngày", false);
        monthToggle = createToggleButton("Tháng", false);
        quarterToggle = createToggleButton("Quý", true);
        yearToggle = createToggleButton("Năm", false);

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Quarter/Year selection for date range
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);

        // Creating the quarter and year combos
        fromQuarterComboBox = new ComboBox<>();
        fromQuarterComboBox.setValue(1);
        fromQuarterComboBox.setPrefWidth(60);

        fromYearComboBox = new ComboBox<>();
        int currentYear = Year.now().getValue();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            fromYearComboBox.getItems().add(i);
        }
        fromYearComboBox.setValue(currentYear);
        fromYearComboBox.setPrefWidth(80);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);

        toQuarterComboBox = new ComboBox<>();
        toQuarterComboBox.setValue(2);
        toQuarterComboBox.setPrefWidth(60);

        toYearComboBox = new ComboBox<>();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            toYearComboBox.getItems().add(i);
        }
        toYearComboBox.setValue(currentYear);
        toYearComboBox.setPrefWidth(80);

        dateRangeBox.getChildren().addAll(
                fromLabel, fromQuarterComboBox, fromYearComboBox,
                toLabel, toQuarterComboBox, toYearComboBox
        );

        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        statusComboBox = new ComboBox<>();
        statusComboBox.setPrefWidth(120);

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Action buttons
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);

        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefSize(100, 30);
        searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        exportExcelButton = createActionButton("Excel", "excel-icon");
        exportPdfButton = createActionButton("PDF", "pdf-icon");
        printButton = createActionButton("Print", "print-icon");

        actionButtonsBox.getChildren().addAll(
                searchButton, exportExcelButton, exportPdfButton, printButton
        );

        // Combine all into filter bar
        filterBar = new HBox(20);
        filterBar.setPadding(new Insets(10));
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, actionButtonsBox);
        filterBar.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");

        root.getChildren().add(filterBar);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setPrefHeight(500);
        statisticsTable.setPrefWidth(800);
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // STT column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Teacher column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);

        Label teacherHeaderLabel = new Label("Giảng viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the Q1/2025 column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, String> q1Column = new TableColumn<>();

        Label q1HeaderLabel = new Label("Q1/2025");
        q1HeaderLabel.setTextFill(Color.BLACK);
        q1HeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1Column.setGraphic(q1HeaderLabel);

        // Q1 Sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> q1SessionsColumn = new TableColumn<>();
        q1SessionsColumn.setCellValueFactory(new PropertyValueFactory<>("q1Sessions"));
        q1SessionsColumn.setPrefWidth(80);
        q1SessionsColumn.setSortable(false);

        Label q1SessionsHeaderLabel = new Label("Buổi");
        q1SessionsHeaderLabel.setTextFill(Color.BLACK);
        q1SessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1SessionsColumn.setGraphic(q1SessionsHeaderLabel);

        // Q1 Hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Double> q1HoursColumn = new TableColumn<>();
        q1HoursColumn.setCellValueFactory(new PropertyValueFactory<>("q1Hours"));
        q1HoursColumn.setPrefWidth(80);
        q1HoursColumn.setSortable(false);

        Label q1HoursHeaderLabel = new Label("Giờ");
        q1HoursHeaderLabel.setTextFill(Color.BLACK);
        q1HoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1HoursColumn.setGraphic(q1HoursHeaderLabel);

        q1Column.getColumns().addAll(q1SessionsColumn, q1HoursColumn);

        // Create the Q2/2025 column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, String> q2Column = new TableColumn<>();

        Label q2HeaderLabel = new Label("Q2/2025");
        q2HeaderLabel.setTextFill(Color.BLACK);
        q2HeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2Column.setGraphic(q2HeaderLabel);

        // Q2 Sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> q2SessionsColumn = new TableColumn<>();
        q2SessionsColumn.setCellValueFactory(new PropertyValueFactory<>("q2Sessions"));
        q2SessionsColumn.setPrefWidth(80);
        q2SessionsColumn.setSortable(false);

        Label q2SessionsHeaderLabel = new Label("Buổi");
        q2SessionsHeaderLabel.setTextFill(Color.BLACK);
        q2SessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2SessionsColumn.setGraphic(q2SessionsHeaderLabel);

        // Q2 Hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Double> q2HoursColumn = new TableColumn<>();
        q2HoursColumn.setCellValueFactory(new PropertyValueFactory<>("q2Hours"));
        q2HoursColumn.setPrefWidth(80);
        q2HoursColumn.setSortable(false);

        Label q2HoursHeaderLabel = new Label("Giờ");
        q2HoursHeaderLabel.setTextFill(Color.BLACK);
        q2HoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2HoursColumn.setGraphic(q2HoursHeaderLabel);

        q2Column.getColumns().addAll(q2SessionsColumn, q2HoursColumn);

        // Create the total column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatisticsModel, Double> totalHoursColumn = new TableColumn<>();
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setPrefWidth(80);
        totalHoursColumn.setSortable(false);

        Label totalHoursHeaderLabel = new Label("Giờ");
        totalHoursHeaderLabel.setTextFill(Color.BLACK);
        totalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeaderLabel);

        totalColumn.getColumns().addAll(totalSessionsColumn, totalHoursColumn);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, q1Column, q2Column, totalColumn);

        // Add footer row for totals
        VBox tableContainer = new VBox();
        tableContainer.getChildren().add(statisticsTable);

        // Create totals row
        GridPane totalRow = createTotalRow();
        tableContainer.getChildren().add(totalRow);

        root.getChildren().add(tableContainer);
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

        ColumnConstraints q1SessionsColumnConstraint = new ColumnConstraints();
        q1SessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints q1HoursColumnConstraint = new ColumnConstraints();
        q1HoursColumnConstraint.setPrefWidth(80);

        ColumnConstraints q2SessionsColumnConstraint = new ColumnConstraints();
        q2SessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints q2HoursColumnConstraint = new ColumnConstraints();
        q2HoursColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalSessionsColumnConstraint = new ColumnConstraints();
        totalSessionsColumnConstraint.setPrefWidth(80);

        ColumnConstraints totalHoursColumnConstraint = new ColumnConstraints();
        totalHoursColumnConstraint.setPrefWidth(80);

        totalRow.getColumnConstraints().addAll(
                sttColumnConstraint,
                nameColumnConstraint,
                q1SessionsColumnConstraint,
                q1HoursColumnConstraint,
                q2SessionsColumnConstraint,
                q2HoursColumnConstraint,
                totalSessionsColumnConstraint,
                totalHoursColumnConstraint
        );

        // Create and add the "Tổng cộng" label
        Label totalLabel = new Label("Tổng cộng");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalLabel, 0);
        GridPane.setColumnSpan(totalLabel, 2);

        // Create and add the session totals for Q1
        q1SessionTotal = new Label("673");
        q1SessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q1SessionTotal, 2);

        // Create and add the hours totals for Q1
        q1HoursTotal = new Label("602");
        q1HoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q1HoursTotal, 3);

        // Create and add the session totals for Q2
        q2SessionTotal = new Label("311");
        q2SessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q2SessionTotal, 4);

        // Create and add the hours totals for Q2
        q2HoursTotal = new Label("288");
        q2HoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q2HoursTotal, 5);

        // Create and add the total session totals
        totalSessionTotal = new Label("984");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalSessionTotal, 6);

        // Create and add the total hours totals
        totalHoursTotal = new Label("890");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalHoursTotal, 7);

        totalRow.getChildren().addAll(
                totalLabel,
                q1SessionTotal,
                q1HoursTotal,
                q2SessionTotal,
                q2HoursTotal,
                totalSessionTotal,
                totalHoursTotal
        );

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

    @Override
    public void refreshView() {
        if (controller != null) {
            controller.loadData();
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();
        refreshView();
    }

    // Public setters for controller to update view
    public void setQuartersList(List<Integer> quarters) {
        fromQuarterComboBox.setItems(FXCollections.observableArrayList(quarters));
        toQuarterComboBox.setItems(FXCollections.observableArrayList(quarters));
        fromQuarterComboBox.setValue(quarters.get(0));
        toQuarterComboBox.setValue(quarters.get(1));
    }

    public void setStatusOptions(List<String> statusOptions) {
        statusComboBox.setItems(FXCollections.observableArrayList(statusOptions));
        statusComboBox.setValue(statusOptions.get(0));
    }

    public void updateTableData(ObservableList<TeacherQuarterlyStatisticsModel> data) {
        statisticsTable.setItems(data);
    }

    public void updateSummaryRow(double[] summaryData) {
        q1SessionTotal.setText(String.valueOf((int)summaryData[0]));
        q1HoursTotal.setText(String.valueOf((int)summaryData[1]));
        q2SessionTotal.setText(String.valueOf((int)summaryData[2]));
        q2HoursTotal.setText(String.valueOf((int)summaryData[3]));
        totalSessionTotal.setText(String.valueOf((int)summaryData[4]));
        totalHoursTotal.setText(String.valueOf((int)summaryData[5]));
    }

    public void showSuccessMessage(String message) {
        showSuccess(message);
    }

    // Getters for controller to retrieve view state
    public int getFromQuarter() {
        return fromQuarterComboBox.getValue();
    }

    public int getFromYear() {
        return fromYearComboBox.getValue();
    }

    public int getToQuarter() {
        return toQuarterComboBox.getValue();
    }

    public int getToYear() {
        return toYearComboBox.getValue();
    }

    public String getSelectedStatus() {
        return statusComboBox.getValue();
    }

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

    // Event handler setters
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