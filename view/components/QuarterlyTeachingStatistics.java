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

public class QuarterlyTeachingStatistics extends BaseScreenView {

    // UI Components
    private ComboBox<Integer> fromQuarterComboBox;
    private ComboBox<Integer> fromYearComboBox;
    private ComboBox<Integer> toQuarterComboBox;
    private ComboBox<Integer> toYearComboBox;
    private ToggleGroup periodToggleGroup;
    private ComboBox<String> statusComboBox;
    private TableView<TeacherQuarterlyStatistics> statisticsTable;
    private HBox filterBar;
    private HBox actionButtonsBar;
    private Button searchButton;
    private Button exportExcelButton;
    private Button exportPdfButton;
    private Button printButton;

    // Data
    private ObservableList<TeacherQuarterlyStatistics> teacherStatisticsList;
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    private final List<Integer> quarters = Arrays.asList(1, 2, 3, 4);

    // Toggle buttons
    private ToggleButton dayToggle;
    private ToggleButton monthToggle;
    private ToggleButton quarterToggle;
    private ToggleButton yearToggle;

    // Toggle state key
    private static final String TOGGLE_STATE_KEY = "statistics_view_type";

    public QuarterlyTeachingStatistics() {
        super("Thống kê giờ giảng", "quarterly-teaching");
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

        dayToggle = createToggleButton("Ngày", false);
        monthToggle = createToggleButton("Tháng", false);
        quarterToggle = createToggleButton("Quý", true);
        yearToggle = createToggleButton("Năm", false);

        // Load toggle state if available
        NavigationController navController = getNavigationController();
        if (navController != null && navController.hasToggleState(TOGGLE_STATE_KEY)) {
            String savedState = navController.getSavedToggleState(TOGGLE_STATE_KEY);
            switch (savedState) {
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
        } else {
            quarterToggle.setSelected(true);
        }

        // Set up toggle listeners to save state
        dayToggle.setOnAction(e -> {
            if (dayToggle.isSelected() && navController != null) {
                navController.saveToggleState(TOGGLE_STATE_KEY, "Ngày");
                navigateToView("daily-teaching");
            }
        });

        monthToggle.setOnAction(e -> {
            if (monthToggle.isSelected() && navController != null) {
                navController.saveToggleState(TOGGLE_STATE_KEY, "Tháng");
                navigateToView("monthly-teaching");
            }
        });

        quarterToggle.setOnAction(e -> {
            if (quarterToggle.isSelected() && navController != null) {
                navController.saveToggleState(TOGGLE_STATE_KEY, "Quý");
            }
        });

        yearToggle.setOnAction(e -> {
            if (yearToggle.isSelected() && navController != null) {
                navController.saveToggleState(TOGGLE_STATE_KEY, "Năm");
                navigateToView("yearly-teaching");
            }
        });

        periodTypeBox.getChildren().addAll(typeLabel, dayToggle, monthToggle, quarterToggle, yearToggle);

        // Quarter/Year selection for date range
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);

        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);

        // Creating the quarter and year combos
        fromQuarterComboBox = new ComboBox<>(FXCollections.observableArrayList(quarters));
        fromQuarterComboBox.setValue(1);
        fromQuarterComboBox.setPrefWidth(60);

        fromYearComboBox = new ComboBox<>();
        int currentYear = Year.now().getValue();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            fromYearComboBox.getItems().add(i);
        }
        fromYearComboBox.setValue(2025);
        fromYearComboBox.setPrefWidth(80);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);

        toQuarterComboBox = new ComboBox<>(FXCollections.observableArrayList(quarters));
        toQuarterComboBox.setValue(2);
        toQuarterComboBox.setPrefWidth(60);

        toYearComboBox = new ComboBox<>();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            toYearComboBox.getItems().add(i);
        }
        toYearComboBox.setValue(2025);
        toYearComboBox.setPrefWidth(80);

        dateRangeBox.getChildren().addAll(fromLabel, fromQuarterComboBox, fromYearComboBox,
                toLabel, toQuarterComboBox, toYearComboBox);

        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(statusOptions));
        statusComboBox.setValue(statusOptions.get(0));
        statusComboBox.setPrefWidth(120);

        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Create action buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        searchButton = new Button("Tìm");
        searchButton.setPrefSize(80, 30);
        searchButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        buttonBox.getChildren().add(searchButton);

        // Put all the filter controls in a HBox
        filterBar = new HBox(20);
        filterBar.setPadding(new Insets(10));
        filterBar.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 5;");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, buttonBox);

        // Create action buttons for export/print
        actionButtonsBar = new HBox(10);
        actionButtonsBar.setPadding(new Insets(10, 0, 10, 0));
        actionButtonsBar.setAlignment(Pos.CENTER_RIGHT);

        exportExcelButton = createActionButton("Excel", "excel-icon");
        exportPdfButton = createActionButton("PDF", "pdf-icon");
        printButton = createActionButton("In", "print-icon");

        actionButtonsBar.getChildren().addAll(exportExcelButton, exportPdfButton, printButton);

        VBox filterContainer = new VBox(10);
        filterContainer.getChildren().addAll(filterBar, actionButtonsBar);

        root.getChildren().add(filterContainer);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        statisticsTable.setPrefHeight(500);
        statisticsTable.setStyle("-fx-border-color: #ddd;");

        // STT column with black header text
        TableColumn<TeacherQuarterlyStatistics, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);

        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Teacher column with black header text
        TableColumn<TeacherQuarterlyStatistics, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);

        Label teacherHeaderLabel = new Label("Giảng viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        // Create the Q1/2025 column with black header text
        TableColumn<TeacherQuarterlyStatistics, String> q1Column = new TableColumn<>();

        Label q1HeaderLabel = new Label("Q1/2025");
        q1HeaderLabel.setTextFill(Color.BLACK);
        q1HeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1Column.setGraphic(q1HeaderLabel);

        // Q1 Sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Integer> q1SessionsColumn = new TableColumn<>();
        q1SessionsColumn.setCellValueFactory(new PropertyValueFactory<>("q1Sessions"));
        q1SessionsColumn.setPrefWidth(80);
        q1SessionsColumn.setSortable(false);

        Label q1SessionsHeaderLabel = new Label("Buổi");
        q1SessionsHeaderLabel.setTextFill(Color.BLACK);
        q1SessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1SessionsColumn.setGraphic(q1SessionsHeaderLabel);

        // Q1 Hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Double> q1HoursColumn = new TableColumn<>();
        q1HoursColumn.setCellValueFactory(new PropertyValueFactory<>("q1Hours"));
        q1HoursColumn.setPrefWidth(80);
        q1HoursColumn.setSortable(false);

        Label q1HoursHeaderLabel = new Label("Giờ");
        q1HoursHeaderLabel.setTextFill(Color.BLACK);
        q1HoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q1HoursColumn.setGraphic(q1HoursHeaderLabel);

        q1Column.getColumns().addAll(q1SessionsColumn, q1HoursColumn);

        // Create the Q2/2025 column with black header text
        TableColumn<TeacherQuarterlyStatistics, String> q2Column = new TableColumn<>();

        Label q2HeaderLabel = new Label("Q2/2025");
        q2HeaderLabel.setTextFill(Color.BLACK);
        q2HeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2Column.setGraphic(q2HeaderLabel);

        // Q2 Sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Integer> q2SessionsColumn = new TableColumn<>();
        q2SessionsColumn.setCellValueFactory(new PropertyValueFactory<>("q2Sessions"));
        q2SessionsColumn.setPrefWidth(80);
        q2SessionsColumn.setSortable(false);

        Label q2SessionsHeaderLabel = new Label("Buổi");
        q2SessionsHeaderLabel.setTextFill(Color.BLACK);
        q2SessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2SessionsColumn.setGraphic(q2SessionsHeaderLabel);

        // Q2 Hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Double> q2HoursColumn = new TableColumn<>();
        q2HoursColumn.setCellValueFactory(new PropertyValueFactory<>("q2Hours"));
        q2HoursColumn.setPrefWidth(80);
        q2HoursColumn.setSortable(false);

        Label q2HoursHeaderLabel = new Label("Giờ");
        q2HoursHeaderLabel.setTextFill(Color.BLACK);
        q2HoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        q2HoursColumn.setGraphic(q2HoursHeaderLabel);

        q2Column.getColumns().addAll(q2SessionsColumn, q2HoursColumn);

        // Create the total column with black header text
        TableColumn<TeacherQuarterlyStatistics, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total sessions sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total hours sub-column with black header text
        TableColumn<TeacherQuarterlyStatistics, Double> totalHoursColumn = new TableColumn<>();
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
        Label q1SessionTotal = new Label("673");
        q1SessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q1SessionTotal, 2);

        // Create and add the hours totals for Q1
        Label q1HoursTotal = new Label("602");
        q1HoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q1HoursTotal, 3);

        // Create and add the session totals for Q2
        Label q2SessionTotal = new Label("311");
        q2SessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q2SessionTotal, 4);

        // Create and add the hours totals for Q2
        Label q2HoursTotal = new Label("288");
        q2HoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(q2HoursTotal, 5);

        // Create and add the total session totals
        Label totalSessionTotal = new Label("984");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalSessionTotal, 6);

        // Create and add the total hours totals
        Label totalHoursTotal = new Label("890");
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

    private void loadSampleData() {
        teacherStatisticsList = FXCollections.observableArrayList(
                new TeacherQuarterlyStatistics(1, "Trịnh Đình Đức", 8, 12.0, 0, 0.0),
                new TeacherQuarterlyStatistics(2, "Trịnh Đình Đức", 60, 57.0, 23, 21.0),
                new TeacherQuarterlyStatistics(3, "Hoàng Ngọc Hà", 9, 13.5, 7, 10.5),
                new TeacherQuarterlyStatistics(4, "Đinh Thị Ngọc Linh", 38, 40.5, 19, 19.5),
                new TeacherQuarterlyStatistics(5, "Trần Trung Hải", 180, 156.0, 68, 63.0),
                new TeacherQuarterlyStatistics(6, "Bùi Tuyết Mai", 31, 46.5, 18, 27.0),
                new TeacherQuarterlyStatistics(7, "Nguyễn Tiến Dũng", 42, 39.0, 26, 27.0),
                new TeacherQuarterlyStatistics(8, "Lê Quang Huy", 63, 24.0, 32, 15.0),
                new TeacherQuarterlyStatistics(9, "Vũ Nhật Quang", 28, 16.5, 14, 10.5),
                new TeacherQuarterlyStatistics(10, "Lê Văn Bảo", 21, 16.5, 8, 6.0),
                new TeacherQuarterlyStatistics(11, "Đỗ Tiến Dũng", 17, 25.5, 7, 10.5),
                new TeacherQuarterlyStatistics(12, "Nguyễn Khánh Linh", 16, 24.0, 8, 12.0),
                new TeacherQuarterlyStatistics(13, "Nguyễn Thị Kim", 16, 0.0, 8, 0.0),
                new TeacherQuarterlyStatistics(14, "Trần Thu Hiền", 16, 0.0, 8, 0.0),
                new TeacherQuarterlyStatistics(15, "Nguyễn Lê Thanh Thủy", 10, 0.0, 3, 0.0),
                new TeacherQuarterlyStatistics(16, "Hà Thị Ngọc", 30, 22.5, 18, 13.5),
                new TeacherQuarterlyStatistics(17, "Phạm Quỳnh Trang", 31, 46.5, 16, 24.0),
                new TeacherQuarterlyStatistics(18, "Trần Thu Hằng", 17, 25.5, 12, 18.0),
                new TeacherQuarterlyStatistics(19, "Nguyễn Minh Anh", 15, 0.0, 9, 0.0),
                new TeacherQuarterlyStatistics(20, "Kiều Thu Thảo", 16, 24.0, 7, 10.5),
                new TeacherQuarterlyStatistics(21, "Hoàng Thị Hương Giang", 4, 0.0, 0, 0.0),
                new TeacherQuarterlyStatistics(22, "Nguyễn Ngọc Hoa", 5, 12.5, 0, 0.0)
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

    @Override
    public void refreshView() {
        // Would typically reload data from a data source
        loadSampleData();
    }

    // Data model for teacher quarterly statistics
    public static class TeacherQuarterlyStatistics {
        private final SimpleStringProperty teacherName;
        private final SimpleIntegerProperty stt;
        private final SimpleIntegerProperty q1Sessions;
        private final SimpleDoubleProperty q1Hours;
        private final SimpleIntegerProperty q2Sessions;
        private final SimpleDoubleProperty q2Hours;
        private final SimpleIntegerProperty totalSessions;
        private final SimpleDoubleProperty totalHours;

        public TeacherQuarterlyStatistics(int stt, String teacherName,
                                          int q1Sessions, double q1Hours,
                                          int q2Sessions, double q2Hours) {
            this.stt = new SimpleIntegerProperty(stt);
            this.teacherName = new SimpleStringProperty(teacherName);
            this.q1Sessions = new SimpleIntegerProperty(q1Sessions);
            this.q1Hours = new SimpleDoubleProperty(q1Hours);
            this.q2Sessions = new SimpleIntegerProperty(q2Sessions);
            this.q2Hours = new SimpleDoubleProperty(q2Hours);
            this.totalSessions = new SimpleIntegerProperty(q1Sessions + q2Sessions);
            this.totalHours = new SimpleDoubleProperty(q1Hours + q2Hours);
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

        public int getQ1Sessions() {
            return q1Sessions.get();
        }

        public SimpleIntegerProperty q1SessionsProperty() {
            return q1Sessions;
        }

        public double getQ1Hours() {
            return q1Hours.get();
        }

        public SimpleDoubleProperty q1HoursProperty() {
            return q1Hours;
        }

        public int getQ2Sessions() {
            return q2Sessions.get();
        }

        public SimpleIntegerProperty q2SessionsProperty() {
            return q2Sessions;
        }

        public double getQ2Hours() {
            return q2Hours.get();
        }

        public SimpleDoubleProperty q2HoursProperty() {
            return q2Hours;
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
        int fromQuarter = fromQuarterComboBox.getValue();
        int fromYear = fromYearComboBox.getValue();
        int toQuarter = toQuarterComboBox.getValue();
        int toYear = toYearComboBox.getValue();
        String status = statusComboBox.getValue();

        // Log the search parameters
        System.out.println("Searching from Q" + fromQuarter + "/" + fromYear +
                " to Q" + toQuarter + "/" + toYear +
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
            System.out.println("Switching to daily view");
            NavigationController navController = getNavigationController();
            if (navController != null) {
                navController.navigateTo("daily-teaching");
            }
        } else if (selected.getText().equals("Tháng")) {
            // Switch to monthly view
            System.out.println("Switching to monthly view");
            NavigationController navController = getNavigationController();
            if (navController != null) {
                navController.navigateTo("monthly-teaching");
            }
        } else if (selected.getText().equals("Năm")) {
            // Switch to yearly view
            System.out.println("Switching to yearly view");
            NavigationController navController = getNavigationController();
            if (navController != null) {
                navController.navigateTo("yearly-teaching");
            }
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // Set up event handlers
        searchButton.setOnAction(e -> handleSearch());
        exportExcelButton.setOnAction(e -> handleExportExcel());
        exportPdfButton.setOnAction(e -> handleExportPdf());
        printButton.setOnAction(e -> handlePrint());
    }

    private void navigateToView(String viewId) {
        NavigationController navController = getNavigationController();
        if (navController != null) {
            navController.navigateTo(viewId);
        }
    }
}
