package view.components;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
import view.BaseScreenView;
import src.controller.NavigationController;

import java.time.Year;
import java.util.Arrays;
import java.util.List;

public class YearlyTeachingStatistics extends BaseScreenView {

    // UI Components
    private ComboBox<Integer> fromYearComboBox;
    private ComboBox<Integer> toYearComboBox;
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherYearlyStatistics> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;

    // Data
    private ObservableList<TeacherYearlyStatistics> teacherStatisticsList;
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    private final List<Integer> years = Arrays.asList(2023, 2024, 2025, 2026);

    public YearlyTeachingStatistics() {
        super("Thống kê giờ giảng", "yearly-teaching");
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

        // Set up event handlers
        setupEventHandlers();
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
            if (dayToggle.isSelected()) handlePeriodChange(dayToggle);
        });

        monthToggle.setOnAction(e -> {
            if (monthToggle.isSelected()) handlePeriodChange(monthToggle);
        });

        quarterToggle.setOnAction(e -> {
            if (quarterToggle.isSelected()) handlePeriodChange(quarterToggle);
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
        TableColumn<TeacherYearlyStatistics, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Create the teacher column with black header text
        TableColumn<TeacherYearlyStatistics, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false);

        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the 2025 column with black header text
        TableColumn<TeacherYearlyStatistics, String> yearColumn = new TableColumn<>();

        Label yearHeaderLabel = new Label("2025");
        yearHeaderLabel.setTextFill(Color.BLACK);
        yearHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearColumn.setGraphic(yearHeaderLabel);

        // 2025 Sessions sub-column with black header text
        TableColumn<TeacherYearlyStatistics, Integer> yearSessionsColumn = new TableColumn<>();
        yearSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("yearSessions"));
        yearSessionsColumn.setPrefWidth(80);
        yearSessionsColumn.setSortable(false);

        Label yearSessionsHeaderLabel = new Label("Buổi");
        yearSessionsHeaderLabel.setTextFill(Color.BLACK);
        yearSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearSessionsColumn.setGraphic(yearSessionsHeaderLabel);

        // 2025 Hours sub-column with black header text
        TableColumn<TeacherYearlyStatistics, Double> yearHoursColumn = new TableColumn<>();
        yearHoursColumn.setCellValueFactory(new PropertyValueFactory<>("yearHours"));
        yearHoursColumn.setPrefWidth(80);
        yearHoursColumn.setSortable(false);

        Label yearHoursHeaderLabel = new Label("Giờ");
        yearHoursHeaderLabel.setTextFill(Color.BLACK);
        yearHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearHoursColumn.setGraphic(yearHoursHeaderLabel);

        yearColumn.getColumns().addAll(yearSessionsColumn, yearHoursColumn);

        // Create the total column with black header text
        TableColumn<TeacherYearlyStatistics, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total sessions sub-column with black header text
        TableColumn<TeacherYearlyStatistics, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total hours sub-column with black header text
        TableColumn<TeacherYearlyStatistics, Double> totalHoursColumn = new TableColumn<>();
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setPrefWidth(80);
        totalHoursColumn.setSortable(false);

        Label totalHoursHeaderLabel = new Label("Giờ");
        totalHoursHeaderLabel.setTextFill(Color.BLACK);
        totalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeaderLabel);

        totalColumn.getColumns().addAll(totalSessionsColumn, totalHoursColumn);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, yearColumn, totalColumn);

        // Create totals row
        GridPane totalRow = createTotalRow();

        // Add table and total row to container
        VBox tableContainer = new VBox();
        tableContainer.getChildren().addAll(statisticsTable, totalRow);

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

        // Add the total values (these would be calculated from the actual data)
        Label yearSessionTotal = new Label("984");
        yearSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(yearSessionTotal, 2, 0);

        Label yearHoursTotal = new Label("890");
        yearHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(yearHoursTotal, 3, 0);

        Label totalSessionTotal = new Label("984");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(totalSessionTotal, 4, 0);

        Label totalHoursTotal = new Label("890");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        totalRow.add(totalHoursTotal, 5, 0);

        return totalRow;
    }

    private void loadSampleData() {
        teacherStatisticsList = FXCollections.observableArrayList(
                new TeacherYearlyStatistics(1, "Trịnh Đình Đức", 8, 12.0),
                new TeacherYearlyStatistics(2, "Trịnh Đình Đức", 83, 78.0),
                new TeacherYearlyStatistics(3, "Hoàng Ngọc Hà", 16, 24.0),
                new TeacherYearlyStatistics(4, "Đinh Thị Ngọc Linh", 57, 60.0),
                new TeacherYearlyStatistics(5, "Trần Trung Hải", 248, 219.0),
                new TeacherYearlyStatistics(6, "Bùi Tuyết Mai", 49, 73.5),
                new TeacherYearlyStatistics(7, "Nguyễn Tiến Dũng", 68, 66.0),
                new TeacherYearlyStatistics(8, "Lê Quang Huy", 95, 39.0),
                new TeacherYearlyStatistics(9, "Vũ Nhật Quang", 42, 27.0),
                new TeacherYearlyStatistics(10, "Lê Văn Bảo", 29, 22.5),
                new TeacherYearlyStatistics(11, "Đỗ Tiến Dũng", 24, 36.0),
                new TeacherYearlyStatistics(12, "Nguyễn Khánh Linh", 24, 36.0),
                new TeacherYearlyStatistics(13, "Nguyễn Thị Kim", 24, 0.0),
                new TeacherYearlyStatistics(14, "Trần Thu Hiền", 24, 0.0),
                new TeacherYearlyStatistics(15, "Nguyễn Lê Thanh Thủy", 13, 0.0),
                new TeacherYearlyStatistics(16, "Hà Thị Ngọc", 48, 36.0),
                new TeacherYearlyStatistics(17, "Phạm Quỳnh Trang", 47, 70.5),
                new TeacherYearlyStatistics(18, "Trần Thu Hằng", 29, 43.5),
                new TeacherYearlyStatistics(19, "Nguyễn Minh Anh", 24, 0.0),
                new TeacherYearlyStatistics(20, "Kiều Thu Thảo", 23, 34.5),
                new TeacherYearlyStatistics(21, "Hoàng Thị Hương Giang", 4, 0.0),
                new TeacherYearlyStatistics(22, "Nguyễn Ngọc Hoa", 5, 12.5)
        );

        statisticsTable.setItems(teacherStatisticsList);
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
        searchButton.setOnAction(e -> handleSearch());
        exportExcelButton.setOnAction(e -> handleExportExcel());
        exportPdfButton.setOnAction(e -> handleExportPdf());
        printButton.setOnAction(e -> handlePrint());
    }

    @Override
    public void refreshView() {
        // Would typically reload data from a data source
        loadSampleData();
    }

    // Data model for teacher yearly statistics
    public static class TeacherYearlyStatistics {
        private final SimpleStringProperty teacherName;
        private final SimpleIntegerProperty stt;
        private final SimpleIntegerProperty yearSessions;
        private final SimpleDoubleProperty yearHours;
        private final SimpleIntegerProperty totalSessions;
        private final SimpleDoubleProperty totalHours;

        public TeacherYearlyStatistics(int stt, String teacherName,
                                       int yearSessions, double yearHours) {
            this.stt = new SimpleIntegerProperty(stt);
            this.teacherName = new SimpleStringProperty(teacherName);
            this.yearSessions = new SimpleIntegerProperty(yearSessions);
            this.yearHours = new SimpleDoubleProperty(yearHours);
            this.totalSessions = new SimpleIntegerProperty(yearSessions); // Same as year for single year view
            this.totalHours = new SimpleDoubleProperty(yearHours); // Same as year for single year view
        }

        public String getTeacherName() {
            return teacherName.get();
        }

        public SimpleStringProperty teacherNameProperty() {
            return teacherName;
        }

        public int getStt() {
            return stt.get();
        }

        public SimpleIntegerProperty sttProperty() {
            return stt;
        }

        public int getYearSessions() {
            return yearSessions.get();
        }

        public SimpleIntegerProperty yearSessionsProperty() {
            return yearSessions;
        }

        public double getYearHours() {
            return yearHours.get();
        }

        public SimpleDoubleProperty yearHoursProperty() {
            return yearHours;
        }

        public int getTotalSessions() {
            return totalSessions.get();
        }

        public SimpleIntegerProperty totalSessionsProperty() {
            return totalSessions;
        }

        public double getTotalHours() {
            return totalHours.get();
        }

        public SimpleDoubleProperty totalHoursProperty() {
            return totalHours;
        }
    }

    // Event handlers
    private void handleSearch() {
        int fromYear = fromYearComboBox.getValue();
        int toYear = toYearComboBox.getValue();
        String status = statusComboBox.getValue();

        // Log the search parameters
        System.out.println("Searching from year " + fromYear +
                " to year " + toYear +
                " with status: " + status);

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

    private void handlePeriodChange(ToggleButton selected) {
        if (selected.getText().equals("Ngày")) {
            // Switch to daily view
            navigateToView("teaching-statistics");
        } else if (selected.getText().equals("Tháng")) {
            // Switch to monthly view
            navigateToView("monthly-teaching");
        } else if (selected.getText().equals("Quý")) {
            // Switch to quarterly view
            navigateToView("quarterly-teaching");
        }
    }

    private void navigateToView(String viewId) {
        NavigationController navController = getNavigationController();
        if (navController != null) {
            navController.navigateTo(viewId);
        } else {
            System.out.println("Navigation controller is null");
        }
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
}
