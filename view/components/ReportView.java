
// File: view/components/ReportView.java
package view.components; // Hoặc package tương ứng của bạn

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

// Import ReportModel và ClassReportData
import src.model.report.ReportModel;
import src.model.report.ReportModel.ClassReportData;
// Import ReportController
import src.controller.ReportController;

import view.BaseScreenView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportView extends BaseScreenView {

    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> statusComboBox;
    private Button searchButton;
    private Button exportPdfButton;
    private Button exportExcelButton;
    private Button printButton;
    private TableView<ClassReportData> reportTable;
    private HBox metricsPanel;

    private VBox attendanceProgressBox;
    private VBox homeworkProgressBox;
    private Label awarenessValue;
    private Label punctualityValue;
    private Label homeworkScoreValue;

    private ReportController controller;
    private ReportModel model;

    public ReportView() {
        super("Báo cáo tình hình học tập", "learning-reports");
        System.out.println("ReportView constructor: START");

        // 1. Khởi tạo Model
        // ReportModel giờ tự khởi tạo ReportDAO bên trong nó.
        this.model = new ReportModel();
        System.out.println("ReportView constructor: ReportModel created.");

        // 2. Khởi tạo Controller, truyền model và view (this) vào
        this.controller = new ReportController(this.model, this);
        System.out.println("ReportView constructor: ReportController created and linked.");

        // 3. Xây dựng giao diện người dùng
        System.out.println("ReportView constructor: Calling initializeView()...");
        initializeView();
        System.out.println("ReportView constructor: initializeView() completed.");

        // 4. Yêu cầu Controller thiết lập các trình xử lý sự kiện
        System.out.println("ReportView constructor: Calling controller.initializeEventHandlers()...");
        if (this.controller != null) {
            this.controller.initializeEventHandlers();
        } else {
            System.err.println("ReportView constructor: CRITICAL - Controller is null BEFORE calling initializeEventHandlers!");
        }
        System.out.println("ReportView constructor: controller.initializeEventHandlers() completed (if controller was not null).");

        // 5. Yêu cầu Controller tải dữ liệu ban đầu
        System.out.println("ReportView constructor: Calling controller.loadInitialData()...");
        if (this.controller != null) {
            this.controller.loadInitialData();
        } else {
            System.err.println("ReportView constructor: CRITICAL - Controller is null BEFORE calling loadInitialData!");
        }
        System.out.println("ReportView constructor: controller.loadInitialData() completed (if controller was not null).");

        System.out.println("ReportView constructor: END");
    }

    // ... (Phần còn lại của ReportView.java không thay đổi)
    // initializeView(), createFilterPanel(), createMetricsPanel(), createProgressCircleBox(),
    // createCriteriaMetricsBox(), createReportTable(), refreshView(), setClassStatusOptions(),
    // updateOverallMetrics(), replacePanelChild(), updateReportTable(), showError(),
    // các getters, setBlackHeaderText() vẫn giữ nguyên như phiên bản trước.
    // Để cho ngắn gọn, tôi sẽ không lặp lại toàn bộ file ở đây.
    // Bạn chỉ cần đảm bảo hàm dựng được cập nhật như trên.

    // QUAN TRỌNG: Dưới đây là các phương thức còn lại của ReportView để bạn tiện tham khảo
    // và đảm bảo không có gì bị sót. Hãy copy phần này vào file ReportView.java của bạn.

    @Override
    public void initializeView() {
        System.out.println("ReportView.initializeView() CALLED.");

        root.getChildren().clear();
        root.setSpacing(20);
        root.setPadding(new Insets(20));

        HBox filterPanel = createFilterPanel();
        root.getChildren().add(filterPanel);

        Text titleText = new Text("Báo cáo tình hình học tập");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#0078D7"));
        HBox titleBox = new HBox(titleText);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(10, 0, 20, 0));
        root.getChildren().add(titleBox);

        this.metricsPanel = createMetricsPanel();
        root.getChildren().add(this.metricsPanel);

        VBox tableContainer = createReportTable();
        root.getChildren().add(tableContainer);

        if (this.reportTable != null && this.reportTable.getParent() instanceof VBox) {
            VBox.setVgrow(this.reportTable, Priority.ALWAYS);
        } else if (tableContainer.getChildren().contains(this.reportTable)) {
            VBox.setVgrow(this.reportTable, Priority.ALWAYS);
        }
        System.out.println("ReportView.initializeView() UI construction part completed.");
    }


    private HBox createFilterPanel() {
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        toDatePicker = new DatePicker(LocalDate.now());

        // ComboBox with predefined options
        statusComboBox = new ComboBox<>();
        statusComboBox.setPromptText("Chọn trạng thái");
        ObservableList<String> statuses = FXCollections.observableArrayList("Active", "Inactive");
        statusComboBox.setItems(statuses);
        statusComboBox.setPrefWidth(150);

        searchButton = new Button("Tìm kiếm");
        searchButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white;");
        searchButton.setOnAction(event -> refreshView()); // Trigger refreshView() when clicked

        exportPdfButton = new Button("Xuất PDF");
        exportPdfButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");

        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;");

        printButton = new Button("In");
        printButton.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");

        HBox filterPanelLayout = new HBox(15);
        filterPanelLayout.setPadding(new Insets(15));
        filterPanelLayout.setAlignment(Pos.CENTER_LEFT);
        filterPanelLayout.setStyle("-fx-background-color: #F0F0F0; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Labels with black text for From Date, To Date, and Status
        Label fromDateLabel = new Label("Từ ngày:");
        fromDateLabel.setTextFill(Color.BLACK);

        Label toDateLabel = new Label("Đến ngày:");
        toDateLabel.setTextFill(Color.BLACK);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);

        HBox dateFilterGroup = new HBox(10, fromDateLabel, fromDatePicker, toDateLabel, toDatePicker);
        dateFilterGroup.setAlignment(Pos.CENTER_LEFT);

        HBox statusFilterGroup = new HBox(10, statusLabel, statusComboBox);
        statusFilterGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionButtonsGroup = new HBox(10, searchButton, exportPdfButton, exportExcelButton, printButton);
        actionButtonsGroup.setAlignment(Pos.CENTER_RIGHT);

        filterPanelLayout.getChildren().addAll(dateFilterGroup, statusFilterGroup, spacer, actionButtonsGroup);

        return filterPanelLayout;
    }



    private HBox createMetricsPanel() {
        awarenessValue = new Label("0 sao/học viên");
        awarenessValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        awarenessValue.setTextFill(Color.web("#333"));
        punctualityValue = new Label("0 điểm/học viên");
        punctualityValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        punctualityValue.setTextFill(Color.web("#333"));
        homeworkScoreValue = new Label("0/10 điểm");
        homeworkScoreValue.setFont(Font.font("System", FontWeight.BOLD, 14));
        homeworkScoreValue.setTextFill(Color.web("#333"));
        attendanceProgressBox = createProgressCircleBox("Tỷ lệ đi học", 0, "#4CAF50");
        homeworkProgressBox = createProgressCircleBox("Tiến trình làm bài tập", 0, "#5D62F9");
        VBox criteriaBox = createCriteriaMetricsBox(awarenessValue, punctualityValue, homeworkScoreValue);
        HBox newMetricsPanel = new HBox(20);
        newMetricsPanel.setAlignment(Pos.CENTER);
        newMetricsPanel.setPadding(new Insets(15));
        newMetricsPanel.getChildren().addAll(attendanceProgressBox, homeworkProgressBox, criteriaBox);
        return newMetricsPanel;
    }

    private VBox createProgressCircleBox(String labelText, double percentage, String color) {
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(15));
        progressBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        progressBox.setPrefWidth(250); progressBox.setPrefHeight(180);
        StackPane progressIndicator = new StackPane();
        progressIndicator.setPrefSize(120, 120);
        Circle backgroundCircle = new Circle(50);
        backgroundCircle.setFill(Color.TRANSPARENT);
        backgroundCircle.setStroke(Color.LIGHTGRAY);
        backgroundCircle.setStrokeWidth(8);
        Circle progressCircleItem = new Circle(50);
        progressCircleItem.setFill(Color.TRANSPARENT);
        progressCircleItem.setStroke(Color.web(color));
        progressCircleItem.setStrokeWidth(8);
        progressCircleItem.setRotate(-90);
        double circumference = 2 * Math.PI * 50;
        double dashArray = (percentage / 100.0) * circumference;
        progressCircleItem.getStrokeDashArray().clear();
        if (dashArray > 0 && Double.isFinite(dashArray)) {
            progressCircleItem.getStrokeDashArray().addAll(dashArray, circumference - dashArray);
        } else {
            progressCircleItem.getStrokeDashArray().addAll(0.0, circumference);
        }
        Label percentageLabelText = new Label(String.format("%.0f%%", percentage));
        percentageLabelText.setFont(Font.font("System", FontWeight.BOLD, 18));
        percentageLabelText.setTextFill(Color.BLACK);
        progressIndicator.getChildren().addAll(backgroundCircle, progressCircleItem, percentageLabelText);
        Label titleLabel = new Label(labelText);
        titleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        titleLabel.setTextFill(Color.BLACK);
        progressBox.getChildren().addAll(progressIndicator, titleLabel);
        return progressBox;
    }

    private VBox createCriteriaMetricsBox(Label awarenessVal, Label punctualityVal, Label homeworkScoreVal) {
        VBox criteriaBox = new VBox(15);
        criteriaBox.setAlignment(Pos.CENTER_LEFT);
        criteriaBox.setPadding(new Insets(15));
        criteriaBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        criteriaBox.setPrefWidth(300); criteriaBox.setPrefHeight(180);
        Label criteriaHeading = new Label("Tiêu chí đánh giá trung bình");
        criteriaHeading.setFont(Font.font("System", FontWeight.BOLD, 16));
        criteriaHeading.setTextFill(Color.BLACK);
        criteriaHeading.setPrefWidth(Double.MAX_VALUE);
        criteriaHeading.setAlignment(Pos.CENTER);
        HBox awarenessBox = new HBox(5, new Label("Ý thức học:"), awarenessVal);
        awarenessBox.setAlignment(Pos.CENTER_LEFT);
        HBox punctualityBox = new HBox(5, new Label("Đúng giờ:"), punctualityVal);
        punctualityBox.setAlignment(Pos.CENTER_LEFT);
        HBox homeworkScoreBox = new HBox(5, new Label("Kết quả BTVN:"), homeworkScoreVal);
        homeworkScoreBox.setAlignment(Pos.CENTER_LEFT);
        ((Label)awarenessBox.getChildren().get(0)).setPrefWidth(100);
        ((Label)punctualityBox.getChildren().get(0)).setPrefWidth(100);
        ((Label)homeworkScoreBox.getChildren().get(0)).setPrefWidth(100);
        criteriaBox.getChildren().addAll(criteriaHeading, awarenessBox, punctualityBox, homeworkScoreBox);
        return criteriaBox;
    }

    private VBox createReportTable() {
        reportTable = new TableView<>();
        VBox tableContainer = new VBox(10);
        tableContainer.setAlignment(Pos.TOP_CENTER);
        tableContainer.setPadding(new Insets(15));
        tableContainer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        Label tableTitle = new Label("Chi tiết tình hình học tập theo lớp");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setTextFill(Color.BLACK);
        tableTitle.setAlignment(Pos.CENTER);
        tableTitle.setPrefWidth(Double.MAX_VALUE);
        reportTable.setPlaceholder(new Label("Đang tải dữ liệu hoặc chưa có dữ liệu..."));
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ClassReportData, Integer> sttCol = new TableColumn<>(); setBlackHeaderText(sttCol, "STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setStyle("-fx-alignment: CENTER;"); sttCol.setPrefWidth(50); sttCol.setMinWidth(50); sttCol.setMaxWidth(70);
        TableColumn<ClassReportData, String> classNameCol = new TableColumn<>(); setBlackHeaderText(classNameCol, "Tên lớp");
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classNameCol.setPrefWidth(180); classNameCol.setMinWidth(150);
        TableColumn<ClassReportData, String> attendanceCol = new TableColumn<>(); setBlackHeaderText(attendanceCol, "Số buổi học (Học/Tổng)");
        attendanceCol.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        attendanceCol.setStyle("-fx-alignment: CENTER;"); attendanceCol.setPrefWidth(180); attendanceCol.setMinWidth(150);
        TableColumn<ClassReportData, String> homeworkCol = new TableColumn<>(); setBlackHeaderText(homeworkCol, "Số bài tập về nhà (Làm/Tổng)");
        homeworkCol.setCellValueFactory(new PropertyValueFactory<>("homework"));
        homeworkCol.setStyle("-fx-alignment: CENTER;"); homeworkCol.setPrefWidth(200); homeworkCol.setMinWidth(180);
        TableColumn<ClassReportData, Double> awarenessCol = new TableColumn<>(); setBlackHeaderText(awarenessCol, "Ý thức học");
        awarenessCol.setCellValueFactory(new PropertyValueFactory<>("awareness"));
        awarenessCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                // Hiển thị "Chưa có" nếu điểm awareness là 0 và có tên lớp (để phân biệt với dòng trống)
                else if (item == 0 && getTableRow() != null && getTableRow().getItem() != null && ((ClassReportData)getTableRow().getItem()).getClassName() != null) { setText("Chưa có"); setTextFill(Color.GRAY); setGraphic(null); }
                else if (item == 0) { setText(null); setGraphic(null); } // Nếu item là 0 mà không có row context, không hiển thị gì
                else { try { int stars = (int) Math.round(item); HBox starContainer = new HBox(1); starContainer.setAlignment(Pos.CENTER); for (int i = 0; i < 5; i++) { Text star = new Text("★"); star.setFill(i < stars ? Color.GOLD : Color.LIGHTGRAY); starContainer.getChildren().add(star); } setGraphic(starContainer); setText(null); } catch (Exception e) { setText(String.format("%.1f",item)); setTextFill(Color.BLACK); setGraphic(null); } }
            }
        });
        awarenessCol.setStyle("-fx-alignment: CENTER;"); awarenessCol.setPrefWidth(120); awarenessCol.setMinWidth(100);
        TableColumn<ClassReportData, Double> punctualityCol = new TableColumn<>(); setBlackHeaderText(punctualityCol, "Đúng giờ (điểm)");
        punctualityCol.setCellValueFactory(new PropertyValueFactory<>("punctuality"));
        punctualityCol.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); if (empty || item == null) { setText(null); } else { setText(String.format("%.2f", item)); setTextFill(Color.BLACK); } } });
        punctualityCol.setStyle("-fx-alignment: CENTER;"); punctualityCol.setPrefWidth(120); punctualityCol.setMinWidth(100);
        TableColumn<ClassReportData, String> homeworkScoreCol = new TableColumn<>(); setBlackHeaderText(homeworkScoreCol, "Điểm BTVN (TB)");
        // homeworkScore trong ClassReportData là String, ví dụ "8.5/10" hoặc "7"
        homeworkScoreCol.setCellValueFactory(new PropertyValueFactory<>("homeworkScore"));
        homeworkScoreCol.setStyle("-fx-alignment: CENTER;"); homeworkScoreCol.setPrefWidth(120); homeworkScoreCol.setMinWidth(100);
        reportTable.getColumns().addAll(sttCol, classNameCol, attendanceCol, homeworkCol, awarenessCol, punctualityCol, homeworkScoreCol);
        tableContainer.getChildren().addAll(tableTitle, reportTable);
        VBox.setVgrow(reportTable, Priority.ALWAYS);
        return tableContainer;
    }

    @Override
    public void refreshView() {
        System.out.println("ReportView.refreshView() CALLED.");
        if (this.controller != null) {
            System.out.println("ReportView.refreshView(): Controller exists. Calling controller.loadReportData().");
            this.controller.loadReportData();
        } else {
            System.err.println("ReportView.refreshView(): CRITICAL - Controller is NULL. Cannot refresh data.");
            showError("Lỗi nghiêm trọng: Controller không khả dụng để làm mới dữ liệu.");
        }
    }

    public void setClassStatusOptions(List<String> statuses) {
        if (statusComboBox == null) { System.err.println("ReportView.setClassStatusOptions: statusComboBox is null!"); return; }
        ObservableList<String> statusItems = FXCollections.observableArrayList(statuses != null ? statuses : new ArrayList<>());
        String currentValue = statusComboBox.getValue();
        statusComboBox.setItems(statusItems);
        String defaultFilter = (controller != null) ? controller.getDefaultStatusFilter() : "Tất cả";
        if (currentValue != null && statusItems.contains(currentValue)) { statusComboBox.setValue(currentValue); }
        else if (statusItems.contains(defaultFilter)) { statusComboBox.setValue(defaultFilter); }
        else if (!statusItems.isEmpty()) { statusComboBox.setValue(statusItems.get(0)); }
        else { statusComboBox.setValue(null); statusComboBox.setPromptText("Không có trạng thái"); }
    }

    public void updateOverallMetrics(double attendancePercentage, double homeworkPercentage,
                                     double avgAwareness, double avgPunctuality, double avgHomeworkScore) {
        if (this.metricsPanel == null || this.attendanceProgressBox == null || this.homeworkProgressBox == null ||
                this.awarenessValue == null || this.punctualityValue == null || this.homeworkScoreValue == null) {
            System.err.println("ReportView.updateOverallMetrics: UI components for metrics are not fully initialized. Update aborted."); return;
        }
        VBox oldAttendanceBox = this.attendanceProgressBox;
        this.attendanceProgressBox = createProgressCircleBox("Tỷ lệ đi học", attendancePercentage, "#4CAF50");
        replacePanelChild(this.metricsPanel, oldAttendanceBox, this.attendanceProgressBox, 0);
        VBox oldHomeworkBox = this.homeworkProgressBox;
        this.homeworkProgressBox = createProgressCircleBox("Tiến trình làm bài tập", homeworkPercentage, "#5D62F9");
        replacePanelChild(this.metricsPanel, oldHomeworkBox, this.homeworkProgressBox, 1);
        awarenessValue.setText(String.format("%.1f sao", avgAwareness)); // Avg awareness từ model
        punctualityValue.setText(String.format("%.2f điểm", avgPunctuality)); // Avg punctuality từ model
        homeworkScoreValue.setText(String.format("%.2f/10", avgHomeworkScore)); // Avg homework score từ model
    }

    private void replacePanelChild(HBox panel, Node oldChild, Node newChild, int fallbackIndex) {
        if (panel == null || newChild == null) { System.err.println("ReportView.replacePanelChild: Panel or newChild is null."); return; }
        int index = -1; if (oldChild != null) { index = panel.getChildren().indexOf(oldChild); }
        if (index != -1) { panel.getChildren().set(index, newChild); }
        else { if (fallbackIndex >= 0 && fallbackIndex < panel.getChildren().size()) { panel.getChildren().add(fallbackIndex, newChild); } else { panel.getChildren().add(newChild); } }
    }

    public void updateReportTable(ObservableList<ClassReportData> data) {
        if (reportTable == null) { System.err.println("ReportView.updateReportTable: reportTable is null!"); if (controller != null) showError("Lỗi hiển thị: Bảng báo cáo chưa được khởi tạo."); return; }
        reportTable.setItems(data);
        if (data == null || data.isEmpty()) { reportTable.setPlaceholder(new Label("Không có dữ liệu cho tiêu chí đã chọn.")); }
    }

    public void showError(String message) {
        System.err.println("SHOWING ERROR DIALOG: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi"); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    public DatePicker getFromDatePicker() { return fromDatePicker; }
    public DatePicker getToDatePicker() { return toDatePicker; }
    public ComboBox<String> getStatusComboBox() { return statusComboBox; }
    public Button getSearchButton() { return searchButton; }
    public Button getExportPdfButton() { return exportPdfButton; }
    public Button getExportExcelButton() { return exportExcelButton; }
    public Button getPrintButton() { return printButton; }
    public TableView<ClassReportData> getReportTable() { return reportTable; }

    private <S, T> void setBlackHeaderText(TableColumn<S, T> column, String title) {
        Label headerLabel = new Label(title); headerLabel.setTextFill(Color.BLACK); headerLabel.setFont(Font.font("System"));
        column.setGraphic(headerLabel);
    }
}

