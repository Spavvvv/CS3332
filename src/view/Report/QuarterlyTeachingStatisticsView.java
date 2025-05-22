package src.view.Report;

import src.controller.Reports.QuarterlyTeachingStatisticsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.HPos; // <<< SỬA Ở ĐÂY: Thêm import HPos
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import src.view.components.Screen.BaseScreenView;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QuarterlyTeachingStatisticsView extends BaseScreenView {

    // UI Components
    public ComboBox<Integer> yearComboBox;
    public ComboBox<String> statusComboBox;
    private TableView<TeacherQuarterlyStatisticsModel> statisticsTable;
    private HBox filterBar;
    private Button searchButton;
    private Button exportExcelButton;

    public Button dayButton;
    public Button monthButton;
    public Button quarterButton;
    public Button yearButton;

    private Label currentQuarterSessionTotal;
    private Label currentQuarterHoursTotal;
    private Label annualTotalSessionTotal;
    private Label annualTotalHoursTotal;

    private QuarterlyTeachingStatisticsController controller;
    private final int START_YEAR = 2023;
    private final int END_YEAR = Year.now().getValue() + 5;

    public QuarterlyTeachingStatisticsView() {
        super("Thống kê giảng dạy", "quarterly-teaching");
        controller = new QuarterlyTeachingStatisticsController();
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

        VBox tableContainer = new VBox();
        tableContainer.getChildren().add(statisticsTable);
        GridPane totalRow = createTotalRow();
        tableContainer.getChildren().add(totalRow);
        root.getChildren().add(tableContainer);

        controller.setView(this);
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

        Label subtitleLabel = new Label("BÁO CÁO THEO QUÝ");
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

        if (periodType.equals("quarter")) {
            button.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        } else {
            button.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        }
        button.setOnAction(e -> handlePeriodButtonClick(text));
        return button;
    }

    public void handlePeriodButtonClick(String periodType) {
        if(dayButton!=null) dayButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if(monthButton!=null) monthButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if(quarterButton!=null) quarterButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");
        if(yearButton!=null) yearButton.setStyle("-fx-background-color: #E0E0E0; -fx-text-fill: black;");

        String targetScreenId = null;
        switch (periodType) {
            case "Ngày":
                if(dayButton!=null) dayButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "teaching-statistics";
                break;
            case "Tháng":
                if(monthButton!=null) monthButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "monthly-teaching";
                break;
            case "Quý":
                if(quarterButton!=null) quarterButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                break;
            case "Năm":
                if(yearButton!=null) yearButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                targetScreenId = "yearly-teaching";
                break;
        }

        if (targetScreenId != null && navigationController != null) {
            navigationController.navigateTo(targetScreenId);
        }
        if (navigationController != null) {
            navigationController.saveToggleState("view_type", periodType);
        }
    }

    private void createFilterBar() {
        // Period type selection
        HBox periodTypeBox = new HBox(5);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label periodLabel = new Label("Chu kỳ:");
        periodLabel.setTextFill(Color.BLACK);
        dayButton = createStyledPeriodButton("Ngày", "day");
        monthButton = createStyledPeriodButton("Tháng", "month");
        quarterButton = createStyledPeriodButton("Quý", "quarter");
        yearButton = createStyledPeriodButton("Năm", "year");
        periodTypeBox.getChildren().addAll(periodLabel, dayButton, monthButton, quarterButton, yearButton);

        // Year selection
        HBox yearSelectionBox = new HBox(10);
        yearSelectionBox.setAlignment(Pos.CENTER_LEFT);
        Label yearLabel = new Label("Năm:");
        yearLabel.setTextFill(Color.BLACK);
        yearComboBox = new ComboBox<>();
        yearComboBox.setPrefWidth(80);
        List<Integer> years = IntStream.rangeClosed(START_YEAR, END_YEAR).boxed().collect(Collectors.toList());
        yearComboBox.setItems(FXCollections.observableArrayList(years));
        yearSelectionBox.getChildren().addAll(yearLabel, yearComboBox);

        // Status dropdown
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        statusComboBox = new ComboBox<>();
        statusComboBox.setValue("Tất cả");
        statusComboBox.setPrefWidth(120);
        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        // Action buttons
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_LEFT);
        searchButton = new Button("Tìm kiếm");
        searchButton.setPrefSize(100, 30);
        searchButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        exportExcelButton = new Button("Excel");
        exportExcelButton.setPrefSize(80, 30);
        exportExcelButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        actionButtonsBox.getChildren().addAll(searchButton, exportExcelButton);

        // Spacer to push action buttons to the right
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Main filter bar (HBox)
        filterBar = new HBox(20); // Spacing between major groups
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.setBackground(new Background(new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)));
        filterBar.setPrefWidth(Double.MAX_VALUE);
        filterBar.getChildren().addAll(periodTypeBox, yearSelectionBox, statusBox, spacer, actionButtonsBox);

        root.getChildren().add(filterBar);
    }


    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setPrefHeight(400);
        statisticsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TeacherQuarterlyStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setSortable(false);
        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        TableColumn<TeacherQuarterlyStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        teacherColumn.setPrefWidth(150);
        teacherColumn.setSortable(false);
        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        TableColumn<TeacherQuarterlyStatisticsModel, String> currentQuarterDisplayColumn = new TableColumn<>();
        Label quarterHeaderLabel = new Label("Quý");
        quarterHeaderLabel.setTextFill(Color.BLACK);
        quarterHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        currentQuarterDisplayColumn.setGraphic(quarterHeaderLabel);

        TableColumn<TeacherQuarterlyStatisticsModel, Integer> sessionsColumn = new TableColumn<>();
        Label sessionsHeaderLabel = new Label("Buổi");
        sessionsHeaderLabel.setTextFill(Color.BLACK);
        sessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sessionsColumn.setGraphic(sessionsHeaderLabel);

        TableColumn<TeacherQuarterlyStatisticsModel, Double> hoursColumn = new TableColumn<>();
        Label hoursHeaderLabel = new Label("Giờ");
        hoursHeaderLabel.setTextFill(Color.BLACK);
        hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        hoursColumn.setGraphic(hoursHeaderLabel);
        currentQuarterDisplayColumn.getColumns().addAll(sessionsColumn, hoursColumn);

        TableColumn<TeacherQuarterlyStatisticsModel, String> annualTotalColumn = new TableColumn<>();
        Label annualHeaderLabel = new Label("Tổng cộng");
        annualHeaderLabel.setTextFill(Color.BLACK);
        annualHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualTotalColumn.setGraphic(annualHeaderLabel);

        TableColumn<TeacherQuarterlyStatisticsModel, Integer> annualSessionsColumn = new TableColumn<>();
        Label annualSessionsHeaderLabel = new Label("Buổi");
        annualSessionsHeaderLabel.setTextFill(Color.BLACK);
        annualSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualSessionsColumn.setGraphic(annualSessionsHeaderLabel);
        annualSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));

        TableColumn<TeacherQuarterlyStatisticsModel, Double> annualHoursColumn = new TableColumn<>();
        Label annualHoursHeaderLabel = new Label("Giờ");
        annualHoursHeaderLabel.setTextFill(Color.BLACK);
        annualHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        annualHoursColumn.setGraphic(annualHoursHeaderLabel);
        annualHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        annualTotalColumn.getColumns().addAll(annualSessionsColumn, annualHoursColumn);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn, currentQuarterDisplayColumn, annualTotalColumn);
    }

    private GridPane createTotalRow() {
        GridPane totalRow = new GridPane();
        totalRow.setPadding(new Insets(5));
        totalRow.setStyle("-fx-border-color: #ddd; -fx-background-color: #f9f9f9;");

        ColumnConstraints sttColumnConstraint = new ColumnConstraints();
        sttColumnConstraint.setPrefWidth(60);
        ColumnConstraints nameColumnConstraint = new ColumnConstraints();
        nameColumnConstraint.setPrefWidth(150);
        ColumnConstraints quarterSessionsColConstraint = new ColumnConstraints();
        quarterSessionsColConstraint.setPrefWidth(80);
        ColumnConstraints quarterHoursColConstraint = new ColumnConstraints();
        quarterHoursColConstraint.setPrefWidth(80);
        ColumnConstraints annualSessionsColConstraint = new ColumnConstraints();
        annualSessionsColConstraint.setPrefWidth(80);
        ColumnConstraints annualHoursColConstraint = new ColumnConstraints();
        annualHoursColConstraint.setPrefWidth(80);
        totalRow.getColumnConstraints().addAll(
                sttColumnConstraint, nameColumnConstraint,
                quarterSessionsColConstraint, quarterHoursColConstraint,
                annualSessionsColConstraint, annualHoursColConstraint
        );

        Label totalLabel = new Label("Tổng cộng");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        GridPane.setColumnIndex(totalLabel, 0);
        GridPane.setColumnSpan(totalLabel, 2);

        currentQuarterSessionTotal = new Label("0");
        currentQuarterSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        // currentQuarterSessionTotal.setAlignment(Pos.CENTER); // <<< SỬA Ở ĐÂY: Không cần thiết
        GridPane.setColumnIndex(currentQuarterSessionTotal, 2);
        GridPane.setHalignment(currentQuarterSessionTotal, HPos.CENTER); // <<< SỬA Ở ĐÂY: Đảm bảo HPos

        currentQuarterHoursTotal = new Label("0.0");
        currentQuarterHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12)); // Đảm bảo không có lỗi đánh máy
        // currentQuarterHoursTotal.setAlignment(Pos.CENTER); // <<< SỬA Ở ĐÂY: Không cần thiết
        GridPane.setColumnIndex(currentQuarterHoursTotal, 3);
        GridPane.setHalignment(currentQuarterHoursTotal, HPos.CENTER); // <<< SỬA Ở ĐÂY: Đảm bảo HPos

        annualTotalSessionTotal = new Label("0");
        annualTotalSessionTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        // annualTotalSessionTotal.setAlignment(Pos.CENTER); // <<< SỬA Ở ĐÂY: Không cần thiết
        GridPane.setColumnIndex(annualTotalSessionTotal, 4);
        GridPane.setHalignment(annualTotalSessionTotal, HPos.CENTER); // <<< SỬA Ở ĐÂY: Đảm bảo HPos

        annualTotalHoursTotal = new Label("0.0");
        annualTotalHoursTotal.setFont(Font.font("System", FontWeight.BOLD, 12));
        // annualTotalHoursTotal.setAlignment(Pos.CENTER); // <<< SỬA Ở ĐÂY: Không cần thiết
        GridPane.setColumnIndex(annualTotalHoursTotal, 5);
        GridPane.setHalignment(annualTotalHoursTotal, HPos.CENTER); // <<< SỬA Ở ĐÂY: Đảm bảo HPos

        totalRow.getChildren().addAll(
                totalLabel, currentQuarterSessionTotal, currentQuarterHoursTotal,
                annualTotalSessionTotal, annualTotalHoursTotal
        );
        return totalRow;
    }

    @Override
    public void refreshView() {
        if (controller != null) {
            if (yearComboBox != null) {
                if (yearComboBox.getValue() == null && !yearComboBox.getItems().isEmpty()) { // Đảm bảo có giá trị
                    yearComboBox.setValue(Year.now().getValue());
                }
            }
            handleSearch();
        }
        handlePeriodButtonClick("Quý");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (controller != null) {
            controller.setupEventHandlers();
        }

        String savedViewType = null;
        if (navigationController != null) {
            savedViewType = navigationController.getSavedToggleState("view_type");
        }

        if (savedViewType != null && !savedViewType.equals("Quý")) {
            handlePeriodButtonClick(savedViewType);
            return;
        }

        handlePeriodButtonClick("Quý");
        if (navigationController != null) {
            navigationController.saveToggleState("view_type", "Quý");
        }

        if (yearComboBox != null) {
            if (yearComboBox.getValue() == null && !yearComboBox.getItems().isEmpty()) {
                yearComboBox.setValue(Year.now().getValue());
            }
        }

        if (controller != null) {
            controller.loadInitialData();
        }
    }

    public void handleSearch() {
        Integer selectedYear = null;
        if (yearComboBox != null) {
            selectedYear = yearComboBox.getValue();
        }

        String status = null;
        if (statusComboBox != null) {
            status = statusComboBox.getValue();
        }

        if (selectedYear == null || status == null) {
            showAlert("Lỗi nhập liệu", "Vui lòng chọn năm và trạng thái.", Alert.AlertType.WARNING);
            return;
        }

        int currentQuarterNumber = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

        if (controller != null) {
            boolean success = controller.searchStatistics(selectedYear, currentQuarterNumber, status);
            if (!success) {
                showAlert("Lỗi kết nối hoặc truy vấn", "Không thể tải dữ liệu. Vui lòng kiểm tra kết nối cơ sở dữ liệu.", Alert.AlertType.ERROR);
            }
        }
    }

    public void showSuccess(String message) {
        System.out.println("INFO: " + message);
        // showAlert("Thông báo", message, Alert.AlertType.INFORMATION);
    }

    public void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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

    public void updateTableData(ObservableList<TeacherQuarterlyStatisticsModel> data) {
        if (statisticsTable != null) {
            statisticsTable.setItems(data);
            updateTableColumnHeadersAndBinding();
            updateSummaryRow();
        }
    }

    private void updateTableColumnHeadersAndBinding() {
        if (statisticsTable == null || yearComboBox == null) return;

        int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
        int selectedYear = yearComboBox.getValue() != null ? yearComboBox.getValue() : Year.now().getValue();

        for (TableColumn<TeacherQuarterlyStatisticsModel, ?> column : statisticsTable.getColumns()) {
            if (column.getGraphic() instanceof Label) {
                Label headerLabel = (Label) column.getGraphic();
                if ("Quý".equals(headerLabel.getText()) || headerLabel.getText().startsWith("Quý ")) {
                    headerLabel.setText("Quý " + currentQuarter + "/" + selectedYear);
                    if (column.getColumns().size() == 2) {
                        TableColumn<TeacherQuarterlyStatisticsModel, ?> sessionsSubColumn = column.getColumns().get(0);
                        TableColumn<TeacherQuarterlyStatisticsModel, ?> hoursSubColumn = column.getColumns().get(1);

                        if (sessionsSubColumn.getGraphic() instanceof Label && ((Label)sessionsSubColumn.getGraphic()).getText().equals("Buổi")) {
                            switch (currentQuarter) {
                                case 1: ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) sessionsSubColumn).setCellValueFactory(new PropertyValueFactory<>("q1Sessions")); break;
                                case 2: ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) sessionsSubColumn).setCellValueFactory(new PropertyValueFactory<>("q2Sessions")); break;
                                case 3: ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) sessionsSubColumn).setCellValueFactory(new PropertyValueFactory<>("q3Sessions")); break;
                                case 4: ((TableColumn<TeacherQuarterlyStatisticsModel, Integer>) sessionsSubColumn).setCellValueFactory(new PropertyValueFactory<>("q4Sessions")); break;
                            }
                        }
                        if (hoursSubColumn.getGraphic() instanceof Label && ((Label)hoursSubColumn.getGraphic()).getText().equals("Giờ")) {
                            switch (currentQuarter) {
                                case 1: ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) hoursSubColumn).setCellValueFactory(new PropertyValueFactory<>("q1Hours")); break;
                                case 2: ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) hoursSubColumn).setCellValueFactory(new PropertyValueFactory<>("q2Hours")); break;
                                case 3: ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) hoursSubColumn).setCellValueFactory(new PropertyValueFactory<>("q3Hours")); break;
                                case 4: ((TableColumn<TeacherQuarterlyStatisticsModel, Double>) hoursSubColumn).setCellValueFactory(new PropertyValueFactory<>("q4Hours")); break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public void updateSummaryRow() {
        if (statisticsTable == null || currentQuarterSessionTotal == null) return;

        int totalCurrentQuarterSessions = 0;
        double totalCurrentQuarterHours = 0.0;
        int totalAnnualSessions = 0;
        double totalAnnualHours = 0.0;
        int currentQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;

        ObservableList<TeacherQuarterlyStatisticsModel> items = statisticsTable.getItems();
        if (items != null) {
            for (TeacherQuarterlyStatisticsModel item : items) {
                switch (currentQuarter) {
                    case 1: totalCurrentQuarterSessions += item.getQ1Sessions(); totalCurrentQuarterHours += item.getQ1Hours(); break;
                    case 2: totalCurrentQuarterSessions += item.getQ2Sessions(); totalCurrentQuarterHours += item.getQ2Hours(); break;
                    case 3: totalCurrentQuarterSessions += item.getQ3Sessions(); totalCurrentQuarterHours += item.getQ3Hours(); break;
                    case 4: totalCurrentQuarterSessions += item.getQ4Sessions(); totalCurrentQuarterHours += item.getQ4Hours(); break;
                }
                totalAnnualSessions += item.getTotalSessions();
                totalAnnualHours += item.getTotalHours();
            }
        }
        currentQuarterSessionTotal.setText(String.valueOf(totalCurrentQuarterSessions));
        currentQuarterHoursTotal.setText(String.format("%.1f", totalCurrentQuarterHours));
        annualTotalSessionTotal.setText(String.valueOf(totalAnnualSessions));
        annualTotalHoursTotal.setText(String.format("%.1f", totalAnnualHours));
    }

    public int getSelectedYear() {
        if (yearComboBox != null && yearComboBox.getValue() != null) {
            return yearComboBox.getValue();
        }
        return Year.now().getValue();
    }

    public int getCurrentQuarter() {
        return (LocalDate.now().getMonthValue() - 1) / 3 + 1;
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
}