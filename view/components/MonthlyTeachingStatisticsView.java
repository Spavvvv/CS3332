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
import view.BaseScreenView;

import java.time.Month;
import java.time.format.DateTimeFormatter;

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

    // Date formatter
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

    public MonthlyTeachingStatisticsView() {
        super("Thống kê giờ giảng", "monthly-teaching");
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

        // Load initial data from controller
        controller.loadInitialData();
        updateTableWithModelData();
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
                return null;
            }
        });

        fromYearComboBox = new ComboBox<>();
        fromYearComboBox.setPrefWidth(70);
        fromYearComboBox.setItems(FXCollections.observableArrayList(
                2023, 2024, 2025, 2026, 2027
        ));
        fromYearComboBox.setValue(2025);
        fromDateBox.getChildren().addAll(fromMonthComboBox, fromYearComboBox);

        Label toLabel = new Label("đến:");
        toLabel.setTextFill(Color.BLACK);

        // To month/year selector
        HBox toDateBox = new HBox(5);
        toMonthComboBox = new ComboBox<>();
        toMonthComboBox.setPrefWidth(65);
        toMonthComboBox.setItems(FXCollections.observableArrayList(
                Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL,
                Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST,
                Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER
        ));
        toMonthComboBox.setValue(Month.APRIL);
        toMonthComboBox.setConverter(new StringConverter<Month>() {
            @Override
            public String toString(Month month) {
                return month == null ? "" : "Thg " + month.getValue();
            }

            @Override
            public Month fromString(String string) {
                return null;
            }
        });

        toYearComboBox = new ComboBox<>();
        toYearComboBox.setPrefWidth(70);
        toYearComboBox.setItems(FXCollections.observableArrayList(
                2023, 2024, 2025, 2026, 2027
        ));
        toYearComboBox.setValue(2025);
        toDateBox.getChildren().addAll(toMonthComboBox, toYearComboBox);

        dateRangeBox.getChildren().addAll(fromLabel, fromDateBox, toLabel, toDateBox);

        // Status dropdown
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        statusComboBox = new ComboBox<>();
        statusComboBox.setItems(FXCollections.observableArrayList(
                controller.getModel().getStatusOptions()
        ));
        statusComboBox.setValue("Tất cả");
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

        // Action buttons bar
        exportExcelButton = createActionButton("Excel", "icon-excel");
        exportPdfButton = createActionButton("PDF", "icon-pdf");
        printButton = createActionButton("Print", "icon-print");

        actionButtonsBar = new HBox(10);
        actionButtonsBar.setAlignment(Pos.CENTER_RIGHT);
        actionButtonsBar.getChildren().addAll(exportExcelButton, exportPdfButton, printButton);

        VBox topContainer = new VBox(10);
        topContainer.getChildren().addAll(filterBar, actionButtonsBar);

        root.getChildren().add(topContainer);
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

        // Create the month column with black header text
        TableColumn<TeacherMonthlyStatistics, String> monthColumn = new TableColumn<>();

        Label monthHeaderLabel = new Label("Tháng 4/2025");
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
        sessionTotal = new Label("0");
        sessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(sessionTotal, 2);

        // Create and add the hours totals
        hoursTotal = new Label("0.0");
        hoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(hoursTotal, 3);

        // Create and add the total session totals
        totalSessionTotal = new Label("0");
        totalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalSessionTotal, 4);

        // Create and add the total hours totals
        totalHoursTotal = new Label("0.0");
        totalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalHoursTotal, 5);

        totalRow.getChildren().addAll(totalLabel, sessionTotal, hoursTotal, totalSessionTotal, totalHoursTotal);

        return totalRow;
    }

    private void updateTableWithModelData() {
        MonthlyTeachingStatisticsModel model = controller.getModel();
        statisticsTable.setItems(model.getTeacherStatisticsList());

        // Update totals
        sessionTotal.setText(String.valueOf(model.getTotalSessions()));
        hoursTotal.setText(String.valueOf(model.getTotalHours()));
        totalSessionTotal.setText(String.valueOf(model.getTotalSessions()));
        totalHoursTotal.setText(String.valueOf(model.getTotalHours()));

        // Update month header
        Month selectedMonth = fromMonthComboBox.getValue();
        int selectedYear = fromYearComboBox.getValue();
        for (TableColumn<TeacherMonthlyStatistics, ?> column : statisticsTable.getColumns()) {
            if (column.getGraphic() instanceof Label && ((Label) column.getGraphic()).getText().startsWith("Tháng")) {
                ((Label) column.getGraphic()).setText("Tháng " + selectedMonth.getValue() + "/" + selectedYear);
                break;
            }
        }
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
        // Reload data from controller
        controller.loadInitialData();
        updateTableWithModelData();
    }

    // Event handlers
    private void handleSearch() {
        Month fromMonth = fromMonthComboBox.getValue();
        int fromYear = fromYearComboBox.getValue();
        Month toMonth = toMonthComboBox.getValue();
        int toYear = toYearComboBox.getValue();
        String status = statusComboBox.getValue();

        // Delegate search to controller
        boolean success = controller.searchStatistics(fromMonth, fromYear, toMonth, toYear, status);

        if (!success) {
            showAlert("Lỗi kết nối", "Không thể kết nối với cơ sở dữ liệu. Hiển thị dữ liệu mẫu.", Alert.AlertType.WARNING);
        }

        updateTableWithModelData();
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

    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

// 5. SQL Table Schema
/*
CREATE TABLE teachers (
    teacher_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    qualification VARCHAR(100),
    status ENUM('active', 'inactive') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE teaching_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_hours DECIMAL(5,2) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    group_name VARCHAR(100),
    status ENUM('Đã duyệt', 'Chưa duyệt', 'Từ chối') DEFAULT 'Chưa duyệt',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)
);

-- Insert sample teachers
INSERT INTO teachers (full_name) VALUES
('Trịnh Đình Đức'),
('Hoàng Ngọc Hà'),
('Đinh Thị Ngọc Linh'),
('Bùi Tuyết Mai'),
('Nguyễn Tiến Dũng'),
('Trần Trung Hải'),
('Lê Quang Huy'),
('Vũ Nhật Quang'),
('Lê Văn Bảo'),
('Đỗ Tiến Dũng'),
('Nguyễn Khánh Linh'),
('Nguyễn Thị Kim'),
('Trần Thu Hiền'),
('Nguyễn Lê Thanh Thủy'),
('Hà Thị Ngọc'),
('Phạm Quỳnh Trang'),
('Trần Thu Hằng'),
('Nguyễn Minh Anh'),
('Kiều Thu Thảo');

-- Sample sessions for April 2025
-- This is just a simplified example; in a real app, you would have many more sessions
*/