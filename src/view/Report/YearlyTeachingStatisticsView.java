package src.view.Report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos; // Vẫn cần cho HBox, VBox
import javafx.geometry.HPos; // Import HPos để căn chỉnh trong GridPane
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import src.view.components.Screen.BaseScreenView;
import src.controller.Reports.YearlyStatisticsController;

import java.util.Arrays;
import java.util.List;
import java.time.Year;
// import java.util.stream.Collectors; // Kiểm tra xem có cần không
// import java.util.stream.IntStream; // Kiểm tra xem có cần không

public class YearlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    public ComboBox<Integer> yearComboBox;
    public ComboBox<String> statusComboBox;
    private TableView<TeacherYearlyStatisticsModel> statisticsTable;
    private HBox filterBar;
    private Button searchButton;
    private Button exportExcelButton;

    public Button dayButton;
    public Button monthButton;
    public Button quarterButton;
    public Button yearButton;

    private Label yearHeaderLabel;
    private GridPane totalRow;

    private ObservableList<Integer> yearsRange = FXCollections.observableArrayList();
    private YearlyStatisticsController controller;

    // Thêm hằng số này để chứa các tùy chọn trạng thái mặc định
    private final List<String> DEFAULT_STATUS_OPTIONS = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");

    private final int START_YEAR_OFFSET = 5;
    private final int END_YEAR_OFFSET = 5;

    public YearlyTeachingStatisticsView() {
        super("Thống kê giảng dạy", "yearly-teaching");
        this.controller = new YearlyStatisticsController(this);
        generateYears();
    }

    private void generateYears() {
        int currentYear = Year.now().getValue();
        yearsRange.clear();
        for (int i = currentYear - START_YEAR_OFFSET; i <= currentYear + END_YEAR_OFFSET; i++) {
            yearsRange.add(i);
        }
    }

    @Override
    public void initializeView() {
        root.getChildren().clear();
        root.setSpacing(20);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        createHeader();
        createFilterBar();
        createStatisticsTable();
    }

    private void createHeader() {
        HBox headerContainer = new HBox();
        headerContainer.setAlignment(Pos.CENTER);
        headerContainer.setPadding(new Insets(0, 0, 20, 0));

        VBox headerContent = new VBox(10);
        headerContent.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("THỐNG KÊ GIẢNG DẠY");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.BLACK);

        Label subtitleLabel = new Label("BÁO CÁO THEO NĂM");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        subtitleLabel.setTextFill(Color.BLACK);

        headerContent.getChildren().addAll(titleLabel, subtitleLabel);
        headerContainer.getChildren().add(headerContent);

        if (!root.getChildren().isEmpty()) {
            root.getChildren().add(0, headerContainer);
        } else {
            root.getChildren().add(headerContainer);
        }
    }

    private Button createStyledPeriodButton(String text, String periodType) {
        Button button = new Button(text);
        button.setPrefHeight(30);
        button.setPrefWidth(70);

        if (periodType.equals("year")) {
            button.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        } else {
            button.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        }
        button.setOnAction(e -> handlePeriodButtonClick(text));
        return button;
    }

    public void handlePeriodButtonClick(String periodType) {
        if (dayButton != null) dayButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if (monthButton != null) monthButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if (quarterButton != null) quarterButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if (yearButton != null) yearButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");

        String targetScreenId = null;
        switch (periodType) {
            case "Ngày":
                if (dayButton != null) dayButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "teaching-statistics";
                break;
            case "Tháng":
                if (monthButton != null) monthButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "monthly-teaching";
                break;
            case "Quý":
                if (quarterButton != null) quarterButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "quarterly-teaching";
                break;
            case "Năm":
                if (yearButton != null) yearButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                break;
        }

        if (targetScreenId != null && getNavigationController() != null) {
            getNavigationController().navigateTo(targetScreenId);
        }
        if (getNavigationController() != null) {
            getNavigationController().saveToggleState("view_type", periodType);
        }
    }

    private void createFilterBar() {
        // Period type selection
        HBox periodTypeBox = new HBox(5);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label periodLabel = new Label("Chu kỳ:");
        periodLabel.setTextFill(Color.BLACK);
        // QUAN TRỌNG: Đảm bảo bạn có phương thức createStyledPeriodButton và handlePeriodButtonClick
        // tương tự như trong QuarterlyTeachingStatisticsView
        dayButton = createStyledPeriodButton("Ngày", "day");
        monthButton = createStyledPeriodButton("Tháng", "month");
        quarterButton = createStyledPeriodButton("Quý", "quarter");
        yearButton = createStyledPeriodButton("Năm", "year"); // "year" sẽ được highlight
        periodTypeBox.getChildren().addAll(periodLabel, dayButton, monthButton, quarterButton, yearButton);
        // Single Year selection
        HBox yearSelectionBox = new HBox(10);
        yearSelectionBox.setAlignment(Pos.CENTER_LEFT);
        Label yearLabelText = new Label("Năm:");
        yearLabelText.setTextFill(Color.BLACK);
        // QUAN TRỌNG: Đảm bảo 'yearsRange' đã được khởi tạo với danh sách các năm.
        // Ví dụ: yearsRange = FXCollections.observableArrayList(IntStream.rangeClosed(START_YEAR, END_YEAR).boxed().collect(Collectors.toList()));
        yearComboBox = new ComboBox<>(yearsRange);
        yearComboBox.setPrefWidth(80);
        // Đặt giá trị mặc định cho yearComboBox, ví dụ năm hiện tại
        if (yearsRange != null && !yearsRange.isEmpty() && yearsRange.contains(Year.now().getValue())) {
            yearComboBox.setValue(Year.now().getValue());
        } else if (yearsRange != null && !yearsRange.isEmpty()) {
            yearComboBox.setValue(yearsRange.get(0)); // Hoặc một giá trị mặc định khác
        }
        // KHÔNG setOnAction gọi controller.handleSearch ở đây.
        // Việc tìm kiếm sẽ được xử lý qua searchButton.
        yearSelectionBox.getChildren().addAll(yearLabelText, yearComboBox);
        // Status selection
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        // QUAN TRỌNG: Đảm bảo 'DEFAULT_STATUS_OPTIONS' đã được định nghĩa.
        // Ví dụ: private final List<String> DEFAULT_STATUS_OPTIONS = Arrays.asList("Tất cả", "Đã duyệt", "Chờ duyệt");
        statusComboBox = new ComboBox<>(FXCollections.observableArrayList(DEFAULT_STATUS_OPTIONS));
        if (!DEFAULT_STATUS_OPTIONS.isEmpty()) {
            statusComboBox.setValue(DEFAULT_STATUS_OPTIONS.get(0)); // Đặt giá trị mặc định là "Tất cả" hoặc phần tử đầu tiên
        }
        statusComboBox.setPrefWidth(120);
        // KHÔNG setOnAction gọi controller.handleSearch ở đây.
        statusBox.getChildren().addAll(statusLabel, statusComboBox);
        // Action buttons (Search, Excel)
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_LEFT); // Căn chỉnh tương tự Quarterly
        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefSize(100, 30);
        searchButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        // Event handler cho searchButton sẽ được thiết lập bởi controller
        // thông qua một phương thức như setSearchButtonHandler(EventHandler<ActionEvent> handler)
        exportExcelButton = new Button("Excel");
        exportExcelButton.setPrefSize(80, 30);
        exportExcelButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        // Event handler cho exportExcelButton sẽ được thiết lập bởi controller
        // thông qua một phương thức như setExportExcelButtonHandler(EventHandler<ActionEvent> handler)
        actionButtonsBox.getChildren().addAll(searchButton, exportExcelButton);
        // Spacer để đẩy actionButtonsBox sang phải
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        // Khởi tạo filterBar là HBox chính
        filterBar = new HBox(20); // Spacing giữa các nhóm chính
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setBackground(new Background(new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)));
        filterBar.setPrefWidth(Double.MAX_VALUE);
        // Thêm tất cả các nhóm con và spacer vào filterBar
        filterBar.getChildren().addAll(periodTypeBox, yearSelectionBox, statusBox, spacer, actionButtonsBox);
        // Thêm filterBar vào root (hoặc container chính của src.view)
        root.getChildren().add(filterBar);
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(600);
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TeacherYearlyStatisticsModel, Integer> sttColumn = new TableColumn<>();
        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);

        TableColumn<TeacherYearlyStatisticsModel, String> teacherColumn = new TableColumn<>();
        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false);

        TableColumn<TeacherYearlyStatisticsModel, Void> yearOuterColumn = new TableColumn<>();
        yearHeaderLabel = new Label(String.valueOf(Year.now().getValue()));
        yearHeaderLabel.setTextFill(Color.BLACK);
        yearHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearOuterColumn.setGraphic(yearHeaderLabel);

        TableColumn<TeacherYearlyStatisticsModel, Integer> yearSessionsColumn = new TableColumn<>();
        Label yearSessionsHeader = new Label("Buổi");
        yearSessionsHeader.setTextFill(Color.BLACK);
        yearSessionsHeader.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearSessionsColumn.setGraphic(yearSessionsHeader);
        yearSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("yearSessions"));
        yearSessionsColumn.setSortable(false);

        TableColumn<TeacherYearlyStatisticsModel, Double> yearHoursColumn = new TableColumn<>();
        Label yearHoursHeader = new Label("Giờ");
        yearHoursHeader.setTextFill(Color.BLACK);
        yearHoursHeader.setFont(Font.font("System", FontWeight.NORMAL, 12));
        yearHoursColumn.setGraphic(yearHoursHeader);
        yearHoursColumn.setCellValueFactory(new PropertyValueFactory<>("yearHours"));
        yearHoursColumn.setSortable(false);
        yearOuterColumn.getColumns().addAll(yearSessionsColumn, yearHoursColumn);

        TableColumn<TeacherYearlyStatisticsModel, Void> totalOuterColumn = new TableColumn<>();
        Label totalHeader = new Label("Tổng cộng");
        totalHeader.setTextFill(Color.BLACK);
        totalHeader.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalOuterColumn.setGraphic(totalHeader);

        TableColumn<TeacherYearlyStatisticsModel, Integer> totalSessionsColumn = new TableColumn<>();
        Label totalSessionsHeader = new Label("Buổi");
        totalSessionsHeader.setTextFill(Color.BLACK);
        totalSessionsHeader.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeader);
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setSortable(false);

        TableColumn<TeacherYearlyStatisticsModel, Double> totalHoursColumn = new TableColumn<>();
        Label totalHoursHeader = new Label("Giờ");
        totalHoursHeader.setTextFill(Color.BLACK);
        totalHoursHeader.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeader);
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setSortable(false);
        totalOuterColumn.getColumns().addAll(totalSessionsColumn, totalHoursColumn);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, yearOuterColumn, totalOuterColumn);

        VBox tableContainer = new VBox();
        tableContainer.getChildren().add(statisticsTable);
        totalRow = createTotalRow();
        tableContainer.getChildren().add(totalRow);

        root.getChildren().add(tableContainer);
    }

    private GridPane createTotalRow() {
        GridPane newTotalRow = new GridPane();
        newTotalRow.setPadding(new Insets(5));
        newTotalRow.setStyle("-fx-border-color: #ddd; -fx-background-color: #f9f9f9;");

        ColumnConstraints col0 = new ColumnConstraints(60);
        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(25);
        ColumnConstraints col5 = new ColumnConstraints(); col5.setPercentWidth(25);
        newTotalRow.getColumnConstraints().addAll(col0, col1, col2, col3, col4, col5);

        Label totalLabelNode = new Label("Tổng cộng"); // Renamed to avoid conflict
        totalLabelNode.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalLabelNode, 0);
        GridPane.setColumnSpan(totalLabelNode, 2);

        Label yearSessionTotal = new Label("0");
        yearSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        yearSessionTotal.setId("yearSessionTotalLabel");
        GridPane.setColumnIndex(yearSessionTotal, 2);
        GridPane.setHalignment(yearSessionTotal, HPos.CENTER); // SỬA Ở ĐÂY

        Label yearHoursTotal = new Label("0.0");
        yearHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        yearHoursTotal.setId("yearHoursTotalLabel");
        GridPane.setColumnIndex(yearHoursTotal, 3);
        GridPane.setHalignment(yearHoursTotal, HPos.CENTER); // SỬA Ở ĐÂY

        Label overallTotalSessionTotal = new Label("0");
        overallTotalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        overallTotalSessionTotal.setId("overallTotalSessionTotalLabel");
        GridPane.setColumnIndex(overallTotalSessionTotal, 4);
        GridPane.setHalignment(overallTotalSessionTotal, HPos.CENTER); // SỬA Ở ĐÂY

        Label overallTotalHoursTotal = new Label("0.0");
        overallTotalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        overallTotalHoursTotal.setId("overallTotalHoursTotalLabel");
        GridPane.setColumnIndex(overallTotalHoursTotal, 5);
        GridPane.setHalignment(overallTotalHoursTotal, HPos.CENTER); // SỬA Ở ĐÂY

        newTotalRow.getChildren().addAll(totalLabelNode, yearSessionTotal, yearHoursTotal, overallTotalSessionTotal, overallTotalHoursTotal);
        return newTotalRow;
    }

    @Override
    public void refreshView() {
        if (controller != null) {
            controller.loadData();
        }
        handlePeriodButtonClick("Năm");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        String savedViewType = null;
        if (getNavigationController() != null) {
            savedViewType = getNavigationController().getSavedToggleState("view_type");
        }

        if (savedViewType != null && !savedViewType.equals("Năm")) {
            handlePeriodButtonClick(savedViewType);
            return;
        }

        handlePeriodButtonClick("Năm");
        if (getNavigationController() != null) {
            getNavigationController().saveToggleState("view_type", "Năm");
        }

        if (yearComboBox != null && yearComboBox.getValue() == null) {
            yearComboBox.setValue(Year.now().getValue());
        }

        if (controller != null) {
            controller.loadData();
        }
    }

    public void setTableData(ObservableList<TeacherYearlyStatisticsModel> data) {
        if (statisticsTable != null) {
            statisticsTable.setItems(data);
        }
    }

    public void updateSummary(StatisticsSummaryModel summary) {
        if (totalRow != null && summary != null) {
            Label yearSessionTotalLabel = (Label) totalRow.lookup("#yearSessionTotalLabel");
            Label yearHoursTotalLabel = (Label) totalRow.lookup("#yearHoursTotalLabel");
            Label overallTotalSessionTotalLabel = (Label) totalRow.lookup("#overallTotalSessionTotalLabel");
            Label overallTotalHoursTotalLabel = (Label) totalRow.lookup("#overallTotalHoursTotalLabel");

            if (yearSessionTotalLabel != null) yearSessionTotalLabel.setText(String.valueOf(summary.getYearSessions()));
            if (yearHoursTotalLabel != null) yearHoursTotalLabel.setText(String.format("%.1f", summary.getYearHours()));
            if (overallTotalSessionTotalLabel != null) overallTotalSessionTotalLabel.setText(String.valueOf(summary.getTotalSessions()));
            if (overallTotalHoursTotalLabel != null) overallTotalHoursTotalLabel.setText(String.format("%.1f", summary.getTotalHours()));
        } else {
            System.err.println("Cannot update summary: totalRow or summary is null.");
        }
    }

    public void updateYearLabel(int year) {
        if (yearHeaderLabel != null) {
            yearHeaderLabel.setText(String.valueOf(year));
        }
    }

    public void setStatusOptions(List<String> statusOptions) {
        if (statusComboBox != null) {
            statusComboBox.setItems(FXCollections.observableArrayList(statusOptions));
            if (statusOptions != null && !statusOptions.isEmpty()) {
                String currentValue = statusComboBox.getValue();
                if (currentValue == null || !statusOptions.contains(currentValue)) {
                    if (statusOptions.contains("Tất cả")) {
                        statusComboBox.setValue("Tất cả");
                    } else {
                        statusComboBox.setValue(statusOptions.get(0));
                    }
                }
            } else {
                statusComboBox.setValue(null);
            }
        }
    }

    public void showExportingMessage(String format) {
        showAlert("Thông báo", "Đang xuất file " + format + "...", Alert.AlertType.INFORMATION);
    }

    public void navigateToView(String viewId) {
        if (getNavigationController() != null) {
            getNavigationController().navigateTo(viewId);
        } else {
            System.err.println("Navigation controller is null. Cannot navigate to " + viewId);
            showAlert("Lỗi hệ thống", "Không thể thực hiện điều hướng.", Alert.AlertType.ERROR);
        }
    }

    public void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null); // Thường để null cho thông báo đơn giản
        alert.setContentText(message);
        alert.showAndWait();
    }

    public int getSelectedYear() {
        if (yearComboBox != null && yearComboBox.getValue() != null) {
            return yearComboBox.getValue();
        }
        return Year.now().getValue();
    }

    public String getSelectedStatus() {
        if (statusComboBox != null && statusComboBox.getValue() != null) { // Thêm kiểm tra null cho getValue()
            return statusComboBox.getValue();
        }
        return "Tất cả";
    }

    public void setSearchButtonHandler(EventHandler<ActionEvent> handler) {
        if (searchButton != null) {
            searchButton.setOnAction(handler);
        }
    }

    public void setExportExcelButtonHandler(EventHandler<ActionEvent> handler) {
        if (exportExcelButton != null) {
            exportExcelButton.setOnAction(handler);
        }
    }

    public void setYearComboBoxActionHandler(EventHandler<ActionEvent> handler) {
        if (yearComboBox != null) {
            yearComboBox.setOnAction(handler);
        }
    }

    public void setStatusComboBoxActionHandler(EventHandler<ActionEvent> handler) {
        if (statusComboBox != null) {
            statusComboBox.setOnAction(handler);
        }
    }
}