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
import javafx.util.StringConverter;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class MonthlyTeachingStatistics extends BaseScreenView {

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

    // Data
    private ObservableList<TeacherMonthlyStatistics> teacherStatisticsList;
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

    // Toggle buttons
    private ToggleButton dayToggle;
    private ToggleButton monthToggle;
    private ToggleButton quarterToggle;
    private ToggleButton yearToggle;

    public MonthlyTeachingStatistics() {
        super("Thống kê giờ giảng", "monthly-teaching");
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
        monthToggle = createToggleButton("Tháng", true);
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
        fromMonthComboBox.setItems(FXCollections.observableArrayList(
                Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL,
                Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST,
                Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER
        ));
        fromMonthComboBox.setValue(Month.APRIL);
        fromMonthComboBox.setConverter(new StringConverter<Month>() {
            @Override
            public String toString(Month month) {
                return month == null ? "" : "Thg " + month.getValue();
            }

            @Override
            public Month fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });

        fromYearComboBox = new ComboBox<>();
        fromYearComboBox.setPrefWidth(70);
        fromYearComboBox.setItems(FXCollections.observableArrayList(2023, 2024, 2025, 2026));
        fromYearComboBox.setValue(2025);

        fromDateBox.getChildren().addAll(fromMonthComboBox, fromYearComboBox);

        // To month/year selector
        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);

        HBox toDateBox = new HBox(5);
        toMonthComboBox = new ComboBox<>();
        toMonthComboBox.setPrefWidth(65);
        toMonthComboBox.setItems(FXCollections.observableArrayList(
                Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL,
                Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST,
                Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER
        ));
        toMonthComboBox.setValue(Month.APRIL);
        toMonthComboBox.setConverter(fromMonthComboBox.getConverter());

        toYearComboBox = new ComboBox<>();
        toYearComboBox.setPrefWidth(70);
        toYearComboBox.setItems(FXCollections.observableArrayList(2023, 2024, 2025, 2026));
        toYearComboBox.setValue(2025);

        toDateBox.getChildren().addAll(toMonthComboBox, toYearComboBox);

        dateRangeBox.getChildren().addAll(fromLabel, fromDateBox, toLabel, toDateBox);

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
        TableColumn<TeacherMonthlyStatistics, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);
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

        // Create the month column (04/2025) with black header text
        TableColumn<TeacherMonthlyStatistics, String> monthColumn = new TableColumn<>();

        Label monthHeaderLabel = new Label("04/2025");
        monthHeaderLabel.setTextFill(Color.BLACK);
        monthHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        monthColumn.setGraphic(monthHeaderLabel);

        // Sessions sub-column with black header text
        TableColumn<TeacherMonthlyStatistics, Integer> sessionsColumn = new TableColumn<>();
        sessionsColumn.setCellValueFactory(new PropertyValueFactory<>("sessions"));
        sessionsColumn.setPrefWidth(80);
        sessionsColumn.setSortable(false);

        Label sessionsHeaderLabel = new Label("Buổi");
        sessionsHeaderLabel.setTextFill(Color.BLACK);
        sessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sessionsColumn.setGraphic(sessionsHeaderLabel);

        // Hours sub-column with black header text
        TableColumn<TeacherMonthlyStatistics, Double> hoursColumn = new TableColumn<>();
        hoursColumn.setCellValueFactory(new PropertyValueFactory<>("hours"));
        hoursColumn.setPrefWidth(80);
        hoursColumn.setSortable(false);

        Label hoursHeaderLabel = new Label("Giờ");
        hoursHeaderLabel.setTextFill(Color.BLACK);
        hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        hoursColumn.setGraphic(hoursHeaderLabel);

        monthColumn.getColumns().addAll(sessionsColumn, hoursColumn);

        // Create the total column with black header text
        TableColumn<TeacherMonthlyStatistics, String> totalColumn = new TableColumn<>();

        Label totalHeaderLabel = new Label("Tổng cộng");
        totalHeaderLabel.setTextFill(Color.BLACK);
        totalHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumn.setGraphic(totalHeaderLabel);

        // Total sessions sub-column with black header text
        TableColumn<TeacherMonthlyStatistics, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        totalSessionsColumn.setSortable(false);

        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);

        // Total hours sub-column with black header text
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
        GridPane.setColumnSpan(totalLabel, 2);

        // Create and add the session totals
        Label sessionTotal = new Label("311");
        sessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(sessionTotal, 2);

        // Create and add the hours totals
        Label hoursTotal = new Label("288");
        hoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(hoursTotal, 3);

        // Create and add the total session totals
        Label totalSessionTotal = new Label("311");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalSessionTotal, 4);

        // Create and add the total hours totals
        Label totalHoursTotal = new Label("288");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalHoursTotal, 5);

        totalRow.getChildren().addAll(totalLabel, sessionTotal, hoursTotal, totalSessionTotal, totalHoursTotal);

        return totalRow;
    }

    private void loadSampleData() {
        teacherStatisticsList = FXCollections.observableArrayList(
                new TeacherMonthlyStatistics(1, "Trịnh Đình Đức", 23, 21.0),
                new TeacherMonthlyStatistics(2, "Hoàng Ngọc Hà", 7, 10.5),
                new TeacherMonthlyStatistics(3, "Đinh Thị Ngọc Linh", 19, 19.5),
                new TeacherMonthlyStatistics(4, "Bùi Tuyết Mai", 18, 27.0),
                new TeacherMonthlyStatistics(5, "Nguyễn Tiến Dũng", 26, 27.0),
                new TeacherMonthlyStatistics(6, "Trần Trung Hải", 68, 63.0),
                new TeacherMonthlyStatistics(7, "Lê Quang Huy", 32, 15.0),
                new TeacherMonthlyStatistics(8, "Vũ Nhật Quang", 14, 10.5),
                new TeacherMonthlyStatistics(9, "Lê Văn Bảo", 8, 6.0),
                new TeacherMonthlyStatistics(10, "Đỗ Tiến Dũng", 7, 10.5),
                new TeacherMonthlyStatistics(11, "Nguyễn Khánh Linh", 8, 12.0),
                new TeacherMonthlyStatistics(12, "Nguyễn Thị Kim", 8, 0.0),
                new TeacherMonthlyStatistics(13, "Trần Thu Hiền", 8, 0.0),
                new TeacherMonthlyStatistics(14, "Nguyễn Lê Thanh Thủy", 3, 0.0),
                new TeacherMonthlyStatistics(15, "Hà Thị Ngọc", 18, 13.5),
                new TeacherMonthlyStatistics(16, "Phạm Quỳnh Trang", 16, 24.0),
                new TeacherMonthlyStatistics(17, "Trần Thu Hằng", 12, 18.0),
                new TeacherMonthlyStatistics(18, "Nguyễn Minh Anh", 9, 0.0),
                new TeacherMonthlyStatistics(19, "Kiều Thu Thảo", 7, 10.5)
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

    // Data model for teacher monthly statistics
    public static class TeacherMonthlyStatistics {
        private final SimpleStringProperty teacherName;
        private final SimpleIntegerProperty stt;
        private final SimpleIntegerProperty sessions;
        private final SimpleDoubleProperty hours;
        private final SimpleIntegerProperty totalSessions;
        private final SimpleDoubleProperty totalHours;

        public TeacherMonthlyStatistics(int stt, String teacherName, int sessions, double hours) {
            this.stt = new SimpleIntegerProperty(stt);
            this.teacherName = new SimpleStringProperty(teacherName);
            this.sessions = new SimpleIntegerProperty(sessions);
            this.hours = new SimpleDoubleProperty(hours);
            this.totalSessions = new SimpleIntegerProperty(sessions); // Same as sessions for the example
            this.totalHours = new SimpleDoubleProperty(hours);       // Same as hours for the example
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

        public int getSessions() {
            return sessions.get();
        }

        public SimpleIntegerProperty sessionsProperty() {
            return sessions;
        }

        public double getHours() {
            return hours.get();
        }

        public SimpleDoubleProperty hoursProperty() {
            return hours;
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
        Month fromMonth = fromMonthComboBox.getValue();
        int fromYear = fromYearComboBox.getValue();
        Month toMonth = toMonthComboBox.getValue();
        int toYear = toYearComboBox.getValue();
        String status = statusComboBox.getValue();

        YearMonth fromYearMonth = YearMonth.of(fromYear, fromMonth);
        YearMonth toYearMonth = YearMonth.of(toYear, toMonth);

        // Log the search parameters
        System.out.println("Searching from " + fromYearMonth + " to " + toYearMonth + " with status: " + status);

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

    private void checkToggles() {
        // Đảm bảo rằng nút "Tháng" đã được chọn khi màn hình này được hiển thị
        monthToggle.setSelected(true);
    }

    private void handlePeriodChange(ToggleButton selected) {
        if (selected.getText().equals("Ngày")) {
            // Chuyển sang TeachingStatistics mà không làm mất đi lựa chọn "Ngày"
            navigationController.saveToggleState("view_type", "Ngày");
            navigationController.navigateTo("teaching-statistics");
        } else if (selected.getText().equals("Quý")) {
            // Chuyển sang QuarterlyTeachingStatistics mà không làm mất đi lựa chọn "Quý"
            navigationController.saveToggleState("view_type", "Quý");
            navigationController.navigateTo("quarterly-teaching");
        } else if (selected.getText().equals("Năm")) {
            // Chuyển sang YearlyTeachingStatistics mà không làm mất đi lựa chọn "Năm"
            navigationController.saveToggleState("view_type", "Năm");
            navigationController.navigateTo("yearly-teaching");
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

        // Kiểm tra xem người dùng đã chọn một view_type nào đó chưa
        String savedViewType = navigationController.getSavedToggleState("view_type");
        if (savedViewType != null) {
            if (savedViewType.equals("Ngày")) {
                dayToggle.setSelected(true);
                // Chuyển hướng đến TeachingStatistics với view_type = "Ngày"
                navigationController.navigateTo("teaching-statistics");
                return;
            } else if (savedViewType.equals("Quý")) {
                quarterToggle.setSelected(true);
                // Chuyển hướng đến QuarterlyTeachingStatistics với view_type = "Quý"
                navigationController.navigateTo("quarterly-teaching");
                return;
            } else if (savedViewType.equals("Năm")) {
                yearToggle.setSelected(true);
                // Chuyển hướng đến YearlyTeachingStatistics với view_type = "Năm"
                navigationController.navigateTo("yearly-teaching");
                return;
            } else if (savedViewType.equals("Tháng")) {
                monthToggle.setSelected(true);
            }
        } else {
            // Mặc định sẽ là "Tháng" nếu không có lựa chọn nào được lưu
            monthToggle.setSelected(true);
            navigationController.saveToggleState("view_type", "Tháng");
        }

        // Set up period toggle handlers
        dayToggle.setOnAction(e -> {
            if (dayToggle.isSelected()) {
                handlePeriodChange(dayToggle);
            }
        });

        monthToggle.setOnAction(e -> {
            if (monthToggle.isSelected()) {
                navigationController.saveToggleState("view_type", "Tháng");
            }
        });

        quarterToggle.setOnAction(e -> {
            if (quarterToggle.isSelected()) {
                handlePeriodChange(quarterToggle);
            }
        });

        yearToggle.setOnAction(e -> {
            if (yearToggle.isSelected()) {
                handlePeriodChange(yearToggle);
            }
        });
    }
}
