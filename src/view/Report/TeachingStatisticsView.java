package src.view.Report;

import src.controller.Reports.TeachingStatisticsController;
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
import src.model.teaching.TeacherStatisticsModel; // Đảm bảo model này có getTotalCalculatedSessions() và getTotalCalculatedHours()
import src.view.components.Screen.BaseScreenView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TeachingStatisticsView extends BaseScreenView {

    // Controller
    private TeachingStatisticsController controller;

    // UI Components
    private HBox filterBar;
    private TableView<TeacherStatisticsModel> statisticsTable;

    // Filter components
    private Button dayButton;
    private Button monthButton;
    private Button quarterButton;
    private Button yearButton;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> statusComboBox;

    // Action buttons
    private Button searchButton;
    private Button exportExcelButton;

    // Date formatter
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Styles for period buttons
    private final String activeStyle = "-fx-background-color: #1976D2; -fx-text-fill: white;";
    private final String inactiveStyle = "-fx-background-color: #E0E0E0; -fx-text-fill: black;";


    public TeachingStatisticsView() {
        super("Thống kê giảng dạy", "teaching-statistics");
    }

    @Override
    public void initializeView() {
        this.controller = new TeachingStatisticsController();

        root.getChildren().clear();
        root.setSpacing(20);
        root.setPadding(new Insets(20));
        root.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

        createHeader();
        createFilterBar();
        createStatisticsTable(); // Phương thức này sẽ được sửa đổi để thêm cột tổng
        loadData();
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

        Label subtitleLabel = new Label("BÁO CÁO THEO NGÀY");
        subtitleLabel.setFont(Font.font("System", FontWeight.NORMAL, 16));
        subtitleLabel.setTextFill(Color.BLACK);

        headerContent.getChildren().addAll(titleLabel, subtitleLabel);
        headerContainer.getChildren().add(headerContent);

        if (!root.getChildren().isEmpty() && root.getChildren().get(0) != headerContainer) {
            if (root.getChildren().contains(headerContainer)) {
                root.getChildren().remove(headerContainer);
            }
            root.getChildren().add(0, headerContainer);
        } else if (root.getChildren().isEmpty()){
            root.getChildren().add(headerContainer);
        }
    }

    private void createFilterBar() {
        filterBar = new HBox();
        filterBar.setPadding(new Insets(10));
        filterBar.setBackground(new Background(new BackgroundFill(Color.web("#f5f5f5"), new CornerRadii(5), Insets.EMPTY)));
        filterBar.setPrefWidth(Double.MAX_VALUE);

        VBox filterContainer = new VBox(10);

        HBox topFilterRow = new HBox(20);
        topFilterRow.setAlignment(Pos.CENTER_LEFT);

        HBox periodTypeBox = new HBox(5);
        periodTypeBox.setAlignment(Pos.CENTER_LEFT);
        Label periodLabel = new Label("Chu kỳ:");
        periodLabel.setTextFill(Color.BLACK);

        dayButton = new Button("Ngày");
        dayButton.setPrefHeight(25); // Giữ kích thước nhất quán
        dayButton.setPrefWidth(70);
        dayButton.setStyle(inactiveStyle);
        dayButton.setOnAction(e -> handlePeriodButtonClick("day"));

        monthButton = new Button("Tháng");
        monthButton.setPrefHeight(25);
        monthButton.setPrefWidth(70);
        monthButton.setStyle(inactiveStyle);
        monthButton.setOnAction(e -> handlePeriodButtonClick("month"));

        quarterButton = new Button("Quý");
        quarterButton.setPrefHeight(25);
        quarterButton.setPrefWidth(70);
        quarterButton.setStyle(inactiveStyle);
        quarterButton.setOnAction(e -> handlePeriodButtonClick("quarter"));

        yearButton = new Button("Năm");
        yearButton.setPrefHeight(25);
        yearButton.setPrefWidth(70);
        yearButton.setStyle(inactiveStyle);
        yearButton.setOnAction(e -> handlePeriodButtonClick("year"));

        periodTypeBox.getChildren().addAll(periodLabel, dayButton, monthButton, quarterButton, yearButton);

        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);
        Label fromLabel = new Label("Từ:");
        fromLabel.setTextFill(Color.BLACK);
        fromDatePicker = new DatePicker(controller.getFilter().getFromDate());
        configureDatePicker(fromDatePicker);

        Label toLabel = new Label("Đến:");
        toLabel.setTextFill(Color.BLACK);
        toDatePicker = new DatePicker(controller.getFilter().getToDate());
        configureDatePicker(toDatePicker);
        dateRangeBox.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        ObservableList<String> statusOptions = FXCollections.observableArrayList(controller.getStatusOptions());
        statusComboBox = new ComboBox<>(statusOptions);
        statusComboBox.setValue(controller.getFilter().getStatus());
        statusComboBox.setPrefWidth(120);
        statusBox.getChildren().addAll(statusLabel, statusComboBox);

        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setAlignment(Pos.CENTER_RIGHT);
        searchButton = createActionButton("Tìm kiếm", "search-icon");
        searchButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
        searchButton.setPrefSize(100, 25); // Kích thước nút Tìm kiếm

        exportExcelButton = createActionButton("Excel", "excel-icon");
        exportExcelButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        exportExcelButton.setPrefSize(80, 25); // Kích thước nút Excel

        actionButtonsBox.getChildren().addAll(searchButton, exportExcelButton);

        topFilterRow.getChildren().addAll(periodTypeBox, dateRangeBox, statusBox, actionButtonsBox);
        filterContainer.getChildren().add(topFilterRow);
        filterBar.getChildren().add(filterContainer);

        if (!root.getChildren().contains(filterBar)) {
            root.getChildren().add(filterBar);
        }
    }

    private void createStatisticsTable() {
        statisticsTable = new TableView<>();
        statisticsTable.setEditable(false);
        statisticsTable.setPrefHeight(400);
        statisticsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); // Cho phép thanh trượt ngang

        // Cột STT
        TableColumn<TeacherStatisticsModel, Integer> sttColumn = new TableColumn<>();
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);
        // sttColumn.setSortable(false); // Tùy chọn
        Label sttHeaderLabel = new Label("STT");
        sttHeaderLabel.setTextFill(Color.BLACK);
        sttHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        sttColumn.setGraphic(sttHeaderLabel);

        // Cột Giáo viên
        TableColumn<TeacherStatisticsModel, String> teacherColumn = new TableColumn<>();
        teacherColumn.setCellValueFactory(cellData ->
                cellData.getValue().teacherNameProperty());
        teacherColumn.setPrefWidth(150);
        // teacherColumn.setSortable(false); // Tùy chọn
        Label teacherHeaderLabel = new Label("Giáo viên");
        teacherHeaderLabel.setTextFill(Color.BLACK);
        teacherHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        teacherColumn.setGraphic(teacherHeaderLabel);

        statisticsTable.getColumns().addAll(sttColumn, teacherColumn);

        // Tạo cột ngày động
        LocalDate startDate = controller.getFilter().getFromDate();
        LocalDate endDate = controller.getFilter().getToDate();

        final int MAX_DAYS_DISPLAY = 15;
        if (startDate != null && endDate != null && startDate.plusDays(MAX_DAYS_DISPLAY -1).isBefore(endDate)) {
            endDate = startDate.plusDays(MAX_DAYS_DISPLAY - 1);
        }

        if (startDate != null && endDate != null) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                final LocalDate dateForColumn = currentDate;

                TableColumn<TeacherStatisticsModel, String> dateColumn = new TableColumn<>();
                String formattedDate = dateFormatter.format(currentDate);
                Label dateHeaderLabel = new Label(formattedDate);
                dateHeaderLabel.setTextFill(Color.BLACK);
                dateHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                dateColumn.setGraphic(dateHeaderLabel);
                // dateColumn.setSortable(false); // Tùy chọn

                TableColumn<TeacherStatisticsModel, Integer> sessionColumn = new TableColumn<>();
                sessionColumn.setCellValueFactory(cellData ->
                        cellData.getValue().getDailyStatistic(dateForColumn).sessionsProperty().asObject());
                sessionColumn.setPrefWidth(50);
                // sessionColumn.setSortable(false); // Tùy chọn
                Label sessionHeaderLabel = new Label("Buổi");
                sessionHeaderLabel.setTextFill(Color.BLACK);
                sessionHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                sessionColumn.setGraphic(sessionHeaderLabel);

                TableColumn<TeacherStatisticsModel, Double> hoursColumn = new TableColumn<>();
                hoursColumn.setCellValueFactory(cellData ->
                        cellData.getValue().getDailyStatistic(dateForColumn).hoursProperty().asObject());
                hoursColumn.setPrefWidth(50);
                // hoursColumn.setSortable(false); // Tùy chọn
                Label hoursHeaderLabel = new Label("Giờ");
                hoursHeaderLabel.setTextFill(Color.BLACK);
                hoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
                hoursColumn.setGraphic(hoursHeaderLabel);

                dateColumn.getColumns().addAll(sessionColumn, hoursColumn);
                statisticsTable.getColumns().add(dateColumn);

                currentDate = currentDate.plusDays(1);
            }
        }

        // ***THÊM CỘT TỔNG (TƯƠNG TỰ MONTHLY)***
        TableColumn<TeacherStatisticsModel, Void> totalColumnParent = new TableColumn<>(); // Cột cha "Tổng cộng"
        Label totalParentHeaderLabel = new Label("Tổng cộng");
        totalParentHeaderLabel.setTextFill(Color.BLACK);
        totalParentHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalColumnParent.setGraphic(totalParentHeaderLabel);
        // totalColumnParent.setSortable(false); // Tùy chọn

        // Cột con: Tổng buổi dạy
        TableColumn<TeacherStatisticsModel, Integer> totalSessionsColumn = new TableColumn<>();
        totalSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("totalSessions"));
        totalSessionsColumn.setPrefWidth(80);
        // totalSessionsColumn.setSortable(false); // Tùy chọn
        Label totalSessionsHeaderLabel = new Label("Buổi");
        totalSessionsHeaderLabel.setTextFill(Color.BLACK);
        totalSessionsHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalSessionsColumn.setGraphic(totalSessionsHeaderLabel);
        // totalSessionsColumn.setStyle("-fx-alignment: CENTER-RIGHT;"); // Căn phải nếu muốn

        // Cột con: Tổng giờ dạy
        TableColumn<TeacherStatisticsModel, Double> totalHoursColumn = new TableColumn<>();
        // Giả sử TeacherStatisticsModel có thuộc tính/getter là "totalCalculatedHours"
        totalHoursColumn.setCellValueFactory(new PropertyValueFactory<>("totalHours"));
        totalHoursColumn.setPrefWidth(80);
        // totalHoursColumn.setSortable(false); // Tùy chọn
        Label totalHoursHeaderLabel = new Label("Giờ");
        totalHoursHeaderLabel.setTextFill(Color.BLACK);
        totalHoursHeaderLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        totalHoursColumn.setGraphic(totalHoursHeaderLabel);
        // totalHoursColumn.setStyle("-fx-alignment: CENTER-RIGHT;"); // Căn phải nếu muốn

        totalColumnParent.getColumns().addAll(totalSessionsColumn, totalHoursColumn);
        statisticsTable.getColumns().add(totalColumnParent); // Thêm cột cha "Tổng cộng" vào bảng


        if (!root.getChildren().contains(statisticsTable)) {
            int filterBarIndex = root.getChildren().indexOf(filterBar);
            if(filterBarIndex != -1 && filterBarIndex + 1 < root.getChildren().size()){
                root.getChildren().add(filterBarIndex + 1, statisticsTable);
            } else {
                root.getChildren().add(statisticsTable);
            }
        }
    }


    private void configureDatePicker(DatePicker datePicker) {
        datePicker.setPrefHeight(25);
        datePicker.setPrefWidth(115);
        datePicker.setEditable(false);
        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) return dateFormatter.format(date);
                return "";
            }
            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) return LocalDate.parse(string, dateFormatter);
                return null;
            }
        };
        datePicker.setConverter(converter);
        datePicker.setPromptText("dd/MM/yyyy");
    }

    @Override
    public void refreshView() {
        LocalDate fromDateVal = fromDatePicker != null ? fromDatePicker.getValue() : controller.getFilter().getFromDate();
        LocalDate toDateVal = toDatePicker != null ? toDatePicker.getValue() : controller.getFilter().getToDate();
        String statusVal = statusComboBox != null ? statusComboBox.getValue() : controller.getFilter().getStatus();

        if (filterBar != null) root.getChildren().remove(filterBar);
        if (statisticsTable != null) root.getChildren().remove(statisticsTable);

        createFilterBar();
        createStatisticsTable(); // Tạo lại bảng với các cột tổng

        if (fromDatePicker != null) fromDatePicker.setValue(fromDateVal);
        if (toDatePicker != null) toDatePicker.setValue(toDateVal);
        if (statusComboBox != null) {
            if(statusComboBox.getItems().isEmpty() && controller.getStatusOptions() != null) {
                statusComboBox.setItems(FXCollections.observableArrayList(controller.getStatusOptions()));
            }
            statusComboBox.setValue(statusVal);
        }

        loadData();

        if (dayButton != null) dayButton.setStyle(activeStyle); // Màn hình ngày mặc định active
        if (monthButton != null) monthButton.setStyle(inactiveStyle);
        if (quarterButton != null) quarterButton.setStyle(inactiveStyle);
        if (yearButton != null) yearButton.setStyle(inactiveStyle);

        if (searchButton != null) searchButton.setOnAction(e -> handleSearch());
        if (exportExcelButton != null) exportExcelButton.setOnAction(e -> handleExportExcel());
    }

    private void loadData() {
        ObservableList<TeacherStatisticsModel> teacherStatistics = controller.getTeacherStatistics();
        if (statisticsTable != null) {
            if (teacherStatistics != null) {
                // Quan trọng: Đảm bảo mỗi TeacherStatisticsModel trong teacherStatistics
                // đã có giá trị cho totalCalculatedSessions và totalCalculatedHours
                // được tính toán (thường là trong Controller hoặc trong Model khi dữ liệu thay đổi)
                statisticsTable.setItems(teacherStatistics);
            } else {
                statisticsTable.setItems(FXCollections.observableArrayList());
            }
        }
    }

    private Button createActionButton(String text, String iconStyle) {
        Button button = new Button(text);
        return button;
    }

    private void handlePeriodButtonClick(String periodType) {
        System.out.println("Selected period type: " + periodType);

        if (dayButton != null) dayButton.setStyle(periodType.equals("day") ? activeStyle : inactiveStyle);
        if (monthButton != null) monthButton.setStyle(periodType.equals("month") ? activeStyle : inactiveStyle);
        if (quarterButton != null) quarterButton.setStyle(periodType.equals("quarter") ? activeStyle : inactiveStyle);
        if (yearButton != null) yearButton.setStyle(periodType.equals("year") ? activeStyle : inactiveStyle);

        if (navigationController != null) {
            String targetScreenId = null;
            String periodTextForState = "Ngày"; // Mặc định cho màn hình hiện tại

            switch (periodType) {
                case "month":
                    targetScreenId = "monthly-teaching";
                    periodTextForState = "Tháng";
                    break;
                case "quarter":
                    targetScreenId = "quarterly-teaching";
                    periodTextForState = "Quý";
                    break;
                case "year":
                    targetScreenId = "yearly-teaching";
                    periodTextForState = "Năm";
                    break;
                case "day":
                    // Đã ở màn hình "Ngày", không cần điều hướng
                    break;
            }

            if (targetScreenId != null) {
                navigationController.navigateTo(targetScreenId);
            }
            // Luôn lưu trạng thái, kể cả khi không điều hướng (để cập nhật nếu người dùng click lại "Ngày")
            navigationController.saveToggleState("view_type", periodTextForState);
        }
    }

    private void handleSearch() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        String status = statusComboBox.getValue();

        if (fromDate == null || toDate == null) {
            showError("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
            return;
        }
        if (fromDate.isAfter(toDate)) {
            showError("Ngày bắt đầu không thể sau ngày kết thúc.");
            return;
        }
        if (status == null || status.trim().isEmpty()){
            showError("Vui lòng chọn trạng thái.");
            return;
        }

        controller.updateDateRange(fromDate, toDate);
        controller.updateStatus(status);
        System.out.println("Searching from " + fromDate + " to " + toDate + " with status: " + status);
        refreshView(); // Tạo lại bảng và tải lại dữ liệu dựa trên filter mới
    }

    private void handleExportExcel() {
        boolean success = controller.exportToExcel();
        if (success) {
            // Thay vì showSuccess ngay, controller nên trả về đường dẫn file hoặc thông báo cụ thể hơn
            // Ví dụ: String filePath = controller.exportToExcel();
            // if (filePath != null) showSuccess("Xuất Excel thành công: " + filePath);
            showSuccess("Đang xử lý xuất file Excel..."); // Hoặc thông báo từ controller
        }
        else showError("Không thể xuất file Excel. Vui lòng thử lại sau.");
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (searchButton != null) searchButton.setOnAction(e -> handleSearch());
        if (exportExcelButton != null) exportExcelButton.setOnAction(e -> handleExportExcel());

        // Khi kích hoạt màn hình này (màn hình Ngày), đảm bảo nút "Ngày" được active
        if (dayButton != null) dayButton.setStyle(activeStyle);
        if (monthButton != null) monthButton.setStyle(inactiveStyle);
        if (quarterButton != null) quarterButton.setStyle(inactiveStyle);
        if (yearButton != null) yearButton.setStyle(inactiveStyle);

        if (navigationController != null) {
            navigationController.saveToggleState("view_type", "Ngày");
        }
        // Không gọi refreshView() ở đây trừ khi có lý do cụ thể,
        // vì initializeView đã gọi và handleSearch sẽ gọi khi cần.
        // Tuy nhiên, nếu filter mặc định (ví dụ, ngày tháng hiện tại) cần được áp dụng khi active,
        // thì có thể gọi loadData() hoặc refreshView() có điều kiện.
        // Hiện tại, controller sẽ cung cấp filter mặc định khi initializeView.
    }

    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}