package src.view.Report;

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

//Controller
import src.controller.Reports.ReportController;

import src.view.components.Screen.BaseScreenView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportView extends BaseScreenView {
    private static final Logger LOGGER = Logger.getLogger(ReportView.class.getName());

    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
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
        this.model = new ReportModel();
        this.controller = new ReportController(this.model, this);
    }

    @Override
    public void initializeView() {
        System.out.println("ReportView.initializeView() CALLED.");

        root.getChildren().clear();
        root.setSpacing(20);
        root.setPadding(new Insets(20));

        HBox filterPanel = createFilterPanel(); // Filter panel giờ không còn statusComboBox

        Text titleText = new Text("Báo cáo tình hình học tập");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#0078D7"));
        HBox titleBox = new HBox(titleText);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        this.metricsPanel = createMetricsPanel();
        VBox tableContainer = createReportTable();

        root.getChildren().addAll(titleBox, filterPanel, this.metricsPanel, tableContainer);

        if (this.reportTable != null && tableContainer.getChildren().contains(this.reportTable)) {
            VBox.setVgrow(this.reportTable, Priority.ALWAYS);
        }
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        this.controller.initializeEventHandlers();
        this.controller.loadInitialData();
    }


    private HBox createFilterPanel() {
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        toDatePicker = new DatePicker(LocalDate.now());

        // statusComboBox ĐÃ BỎ

        searchButton = new Button("Lọc dữ liệu"); // Nút này giờ sẽ lọc theo ngày
        searchButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white; -fx-font-weight: bold;");
        // Event handler của searchButton sẽ được controller thiết lập
        // và controller sẽ chỉ lấy fromDate, toDate.

        exportPdfButton = new Button("Xuất PDF");
        exportPdfButton.setStyle("-fx-background-color: #D9534F; -fx-text-fill: white;");
        exportPdfButton.setDisable(true);

        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle("-fx-background-color: #5CB85C; -fx-text-fill: white;");
        exportExcelButton.setDisable(true);

        printButton = new Button("In Báo Cáo");
        printButton.setStyle("-fx-background-color: #5BC0DE; -fx-text-fill: white;");
        printButton.setDisable(true);

        HBox filterPanelLayout = new HBox(15);
        filterPanelLayout.setPadding(new Insets(10));
        filterPanelLayout.setAlignment(Pos.CENTER_LEFT);
        filterPanelLayout.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DDDDDD; -fx-border-width: 1px; -fx-border-radius: 5; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 1, 1);");

        Label fromDateLabel = new Label("Từ ngày:");
        fromDateLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: #333;");

        Label toDateLabel = new Label("Đến ngày:");
        toDateLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: #333;");

        // statusLabel ĐÃ BỎ

        HBox dateFilterGroup = new HBox(8, fromDateLabel, fromDatePicker, new Label(" "), toDateLabel, toDatePicker);
        dateFilterGroup.setAlignment(Pos.CENTER_LEFT);

        // statusFilterGroup ĐÃ BỎ

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionButtonsGroup = new HBox(10, searchButton, exportPdfButton, exportExcelButton, printButton);
        actionButtonsGroup.setAlignment(Pos.CENTER_RIGHT);

        // Bỏ statusFilterGroup khỏi filterPanelLayout
        filterPanelLayout.getChildren().addAll(dateFilterGroup, spacer, actionButtonsGroup);

        return filterPanelLayout;
    }

    // ... (createMetricsPanel, createProgressCircleBox, createCriteriaMetricsBox, createReportTable giữ nguyên) ...
    // Đảm bảo các phương thức này không có tham chiếu nào đến statusComboBox hoặc logic status
    private HBox createMetricsPanel() {
        awarenessValue = new Label("0.0 sao");
        punctualityValue = new Label("0.0 điểm");
        homeworkScoreValue = new Label("0.0/10");

        String valueStyle = "-fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2C3E50;";
        awarenessValue.setStyle(valueStyle);
        punctualityValue.setStyle(valueStyle);
        homeworkScoreValue.setStyle(valueStyle);

        attendanceProgressBox = createProgressCircleBox("Chuyên cần", 0, "#3498DB");
        homeworkProgressBox = createProgressCircleBox("Bài tập VN", 0, "#9B59B6");
        VBox criteriaMetricsBox = createCriteriaMetricsBox(awarenessValue, punctualityValue, homeworkScoreValue);

        HBox metricsLayout = new HBox(20);
        metricsLayout.setAlignment(Pos.CENTER);
        metricsLayout.getChildren().addAll(attendanceProgressBox, homeworkProgressBox, criteriaMetricsBox);
        HBox.setHgrow(attendanceProgressBox, Priority.ALWAYS);
        HBox.setHgrow(homeworkProgressBox, Priority.ALWAYS);
        HBox.setHgrow(criteriaMetricsBox, Priority.ALWAYS);
        return metricsLayout;
    }

    private VBox createProgressCircleBox(String labelText, double percentage, String color) {
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(15));
        progressBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2); -fx-background-radius: 8px; -fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-border-radius: 8px;");
        progressBox.setPrefWidth(220);
        progressBox.setPrefHeight(180);

        StackPane progressIndicator = new StackPane();
        progressIndicator.setPrefSize(100, 100);

        Circle backgroundCircle = new Circle(45);
        backgroundCircle.setFill(Color.TRANSPARENT);
        backgroundCircle.setStroke(Color.web("#E0E0E0"));
        backgroundCircle.setStrokeWidth(7);

        Circle progressCircleItem = new Circle(45);
        progressCircleItem.setFill(Color.TRANSPARENT);
        progressCircleItem.setStroke(Color.web(color));
        progressCircleItem.setStrokeWidth(7);
        progressCircleItem.setRotate(-90);

        double circumference = 2 * Math.PI * 45;
        double clampedPercentage = Math.max(0, Math.min(100, percentage));
        double visibleDashLength = (clampedPercentage / 100.0) * circumference;
        double gapLength = Math.max(0.00001, circumference - visibleDashLength);

        progressCircleItem.getStrokeDashArray().clear();
        if (Double.isFinite(visibleDashLength) && Double.isFinite(gapLength)) {
            if (clampedPercentage >= 100.0) {
                progressCircleItem.getStrokeDashArray().addAll(circumference, 0.00001);
            } else if (clampedPercentage <= 0.0) {
                progressCircleItem.getStrokeDashArray().addAll(0.0, circumference);
            } else {
                progressCircleItem.getStrokeDashArray().addAll(visibleDashLength, gapLength);
            }
        } else {
            progressCircleItem.getStrokeDashArray().addAll(0.0, circumference);
            LOGGER.warning("Invalid dash array values for progress circle: percentage=" + percentage +
                    ", visibleDash=" + visibleDashLength + ", gap=" + gapLength);
        }

        Label percentageLabelText = new Label(String.format("%.0f%%", percentage));
        percentageLabelText.setFont(Font.font("System", FontWeight.BOLD, 16));
        percentageLabelText.setTextFill(Color.web(color));
        progressIndicator.getChildren().addAll(backgroundCircle, progressCircleItem, percentageLabelText);

        Label titleLabelText = new Label(labelText);
        titleLabelText.setFont(Font.font("System", FontWeight.NORMAL, 13));
        titleLabelText.setTextFill(Color.web("#444444"));

        progressBox.getChildren().addAll(progressIndicator, titleLabelText);
        return progressBox;
    }

    private VBox createCriteriaMetricsBox(Label awarenessVal, Label punctualityVal, Label homeworkScoreVal) {
        VBox criteriaBox = new VBox(12);
        criteriaBox.setAlignment(Pos.TOP_LEFT);
        criteriaBox.setPadding(new Insets(15));
        criteriaBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2); -fx-background-radius: 8px; -fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-border-radius: 8px;");
        criteriaBox.setPrefWidth(280);
        criteriaBox.setPrefHeight(180);

        Label criteriaHeading = new Label("Tiêu chí đánh giá TB");
        criteriaHeading.setFont(Font.font("System", FontWeight.BOLD, 15));
        criteriaHeading.setTextFill(Color.web("#333333"));
        criteriaHeading.setPadding(new Insets(0,0,5,0));

        String labelStyle = "-fx-font-size: 13px; -fx-text-fill: #555;";
        // awarenessVal, punctualityVal, homeworkScoreVal đã được style ở createMetricsPanel

        Label awarenessLabel = new Label("Ý thức học:"); awarenessLabel.setStyle(labelStyle);
        HBox awarenessBox = new HBox(5, awarenessLabel, awarenessVal);
        awarenessBox.setAlignment(Pos.CENTER_LEFT);

        Label punctualityLabel = new Label("Đúng giờ:"); punctualityLabel.setStyle(labelStyle);
        HBox punctualityBox = new HBox(5, punctualityLabel, punctualityVal);
        punctualityBox.setAlignment(Pos.CENTER_LEFT);

        Label homeworkScoreLabel = new Label("Điểm BTVN:"); homeworkScoreLabel.setStyle(labelStyle);
        HBox homeworkScoreBox = new HBox(5, homeworkScoreLabel, homeworkScoreVal);
        homeworkScoreBox.setAlignment(Pos.CENTER_LEFT);

        double labelWidth = 90;
        awarenessLabel.setPrefWidth(labelWidth);
        punctualityLabel.setPrefWidth(labelWidth);
        homeworkScoreLabel.setPrefWidth(labelWidth);

        criteriaBox.getChildren().addAll(criteriaHeading, awarenessBox, punctualityBox, homeworkScoreBox);
        return criteriaBox;
    }

    private VBox createReportTable() {
        reportTable = new TableView<>();
        VBox tableContainer = new VBox(10);
        tableContainer.setAlignment(Pos.TOP_CENTER);
        tableContainer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 2, 2); -fx-background-radius: 8px; -fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-padding:15px;");
        Label tableTitle = new Label("Chi tiết tình hình học tập theo lớp");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        tableTitle.setTextFill(Color.web("#2c3e50"));
        tableTitle.setPadding(new Insets(0,0,10,0));
        reportTable.setPlaceholder(new Label("Chọn khoảng thời gian và nhấn 'Lọc dữ liệu'."));
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ClassReportData, Integer> sttCol = new TableColumn<>(); setBlackHeaderText(sttCol, "STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setStyle("-fx-alignment: CENTER;"); sttCol.setPrefWidth(50); sttCol.setMinWidth(40); sttCol.setMaxWidth(60);

        TableColumn<ClassReportData, String> classNameCol = new TableColumn<>(); setBlackHeaderText(classNameCol, "Tên lớp");
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classNameCol.setPrefWidth(200); classNameCol.setMinWidth(150); // Tăng độ rộng

        TableColumn<ClassReportData, String> attendanceCol = new TableColumn<>(); setBlackHeaderText(attendanceCol, "Chuyên cần (Có mặt/Tổng)");
        attendanceCol.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        attendanceCol.setStyle("-fx-alignment: CENTER;"); attendanceCol.setPrefWidth(180); attendanceCol.setMinWidth(150);

        TableColumn<ClassReportData, String> homeworkCol = new TableColumn<>(); setBlackHeaderText(homeworkCol, "Bài tập (Nộp/Giao)");
        homeworkCol.setCellValueFactory(new PropertyValueFactory<>("homework"));
        homeworkCol.setStyle("-fx-alignment: CENTER;"); homeworkCol.setPrefWidth(180); homeworkCol.setMinWidth(150);

        TableColumn<ClassReportData, Double> awarenessCol = new TableColumn<>(); setBlackHeaderText(awarenessCol, "Ý thức (Sao)");
        awarenessCol.setCellValueFactory(new PropertyValueFactory<>("awareness"));
        awarenessCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else if (item == 0 && getTableRow() != null && getTableRow().getItem() != null && ((ClassReportData)getTableRow().getItem()).getClassName() != null) { setText("Chưa có"); setTextFill(Color.GRAY); setGraphic(null);setAlignment(Pos.CENTER); }
                else if (item == 0) { setText(null); setGraphic(null); }
                else { try { int stars = (int) Math.round(item); HBox starContainer = new HBox(1); starContainer.setAlignment(Pos.CENTER); for (int i = 0; i < 5; i++) { Text star = new Text("★"); star.setFont(Font.font(14)); star.setFill(i < stars ? Color.GOLD : Color.LIGHTGRAY); starContainer.getChildren().add(star); } setGraphic(starContainer); setText(null); setAlignment(Pos.CENTER); } catch (Exception e) { setText(String.format("%.1f",item)); setTextFill(Color.BLACK); setGraphic(null); setAlignment(Pos.CENTER); } }
            }
        });
        awarenessCol.setStyle("-fx-alignment: CENTER;"); awarenessCol.setPrefWidth(120); awarenessCol.setMinWidth(100);

        TableColumn<ClassReportData, Double> punctualityCol = new TableColumn<>(); setBlackHeaderText(punctualityCol, "Đúng giờ (Điểm)");
        punctualityCol.setCellValueFactory(new PropertyValueFactory<>("punctuality"));
        punctualityCol.setCellFactory(tc -> new TableCell<>() { @Override protected void updateItem(Double item, boolean empty) { super.updateItem(item, empty); if (empty || item == null) { setText(null); } else { setText(String.format("%.1f", item)); setTextFill(Color.BLACK); setAlignment(Pos.CENTER);} } });
        punctualityCol.setStyle("-fx-alignment: CENTER;"); punctualityCol.setPrefWidth(120); punctualityCol.setMinWidth(100);

        TableColumn<ClassReportData, String> homeworkScoreCol = new TableColumn<>(); setBlackHeaderText(homeworkScoreCol, "Điểm BTVN (TB)");
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

    // XÓA PHƯƠNG THỨC setClassStatusOptions
    // public void setClassStatusOptions(List<String> statuses) { ... }

    public void updateOverallMetrics(double attendancePercentage, double homeworkPercentage,
                                     double avgAwareness, double avgPunctuality, double avgHomeworkScore) {
        if (this.metricsPanel == null || this.awarenessValue == null || this.punctualityValue == null || this.homeworkScoreValue == null) {
            System.err.println("ReportView.updateOverallMetrics: UI components for metrics are not fully initialized. Update aborted.");
            return;
        }

        // Cập nhật progress circles (chỉ cần gọi lại createProgressCircleBox nếu giá trị thay đổi đáng kể)
        // Hoặc tốt hơn là có phương thức update riêng cho ProgressCircleBox
        // Tạm thời tạo mới:
        VBox oldAttendanceBox = this.attendanceProgressBox;
        this.attendanceProgressBox = createProgressCircleBox("Chuyên cần", attendancePercentage, "#3498DB");
        replacePanelChild(this.metricsPanel, oldAttendanceBox, this.attendanceProgressBox, 0);

        VBox oldHomeworkBox = this.homeworkProgressBox;
        this.homeworkProgressBox = createProgressCircleBox("Bài tập VN", homeworkPercentage, "#9B59B6");
        replacePanelChild(this.metricsPanel, oldHomeworkBox, this.homeworkProgressBox, 1);

        // Đảm bảo các Node con khác (criteriaBox) không bị ảnh hưởng sai vị trí
        // Nếu metricsPanel chỉ có 3 con, và chúng ta thay thế 2 con đầu, con thứ 3 (criteriaBox) sẽ giữ nguyên vị trí nếu replacePanelChild hoạt động đúng.


        awarenessValue.setText(String.format("%.1f sao", avgAwareness));
        punctualityValue.setText(String.format("%.1f điểm", avgPunctuality));
        homeworkScoreValue.setText(String.format("%.2f/10", avgHomeworkScore));
    }

    private void replacePanelChild(HBox panel, Node oldChild, Node newChild, int expectedIndex) {
        if (panel == null || newChild == null) {
            System.err.println("ReportView.replacePanelChild: Panel or newChild is null.");
            return;
        }
        int index = -1;
        if (oldChild != null) {
            index = panel.getChildren().indexOf(oldChild);
        }

        if (index != -1) {
            panel.getChildren().set(index, newChild);
        } else {
            // Nếu không tìm thấy oldChild (ví dụ, lần đầu tiên hoặc cấu trúc panel thay đổi)
            // Thêm newChild vào vị trí dự kiến nếu có thể, hoặc cuối cùng
            if (expectedIndex >= 0 && expectedIndex < panel.getChildren().size()) {
                // Cẩn thận: Nếu oldChild không có và chúng ta set vào expectedIndex,
                // nó có thể ghi đè một Node khác nếu expectedIndex đã có Node.
                // An toàn hơn là remove old (nếu có thể tìm bằng cách khác) rồi add new.
                // Hoặc đảm bảo panel được cấu trúc lại hoàn toàn.
                // Để đơn giản, nếu không tìm thấy oldChild, ta giả định có thể thêm vào cuối
                // hoặc nếu panel có số con cố định, ta có thể thêm vào đúng index nếu biết chắc.
                // Vì metricsPanel có 3 thành phần, nếu oldChild là con thứ nhất hoặc hai,
                // ta có thể set trực tiếp bằng index nếu chắc chắn.
                panel.getChildren().add(expectedIndex, newChild); // Thêm vào vị trí nếu chưa có gì ở đó
                // hoặc chèn vào, đẩy các cái khác ra sau.
                // Nếu muốn thay thế, phải remove trước.
                // Hiện tại, logic này có thể gây lỗi nếu expectedIndex đã có node.
                // Cách an toàn hơn khi oldChild không được tìm thấy:
                // panel.getChildren().remove(oldChild); // Sẽ không làm gì nếu oldChild không có
                // panel.getChildren().add(expectedIndex, newChild); // Có thể gây lỗi nếu index > size
                // Đơn giản nhất là thêm vào cuối nếu không tìm thấy để thay thế:
                // LOGGER.warning("replacePanelChild: oldChild not found. Adding newChild to panel. Index might be incorrect.");
                // panel.getChildren().add(newChild);
            } else if (expectedIndex >= 0 && expectedIndex == panel.getChildren().size()){
                panel.getChildren().add(newChild); // Thêm vào cuối nếu index là vị trí cuối cùng + 1
            }
            else { // Fallback, thêm vào cuối
                LOGGER.warning("replacePanelChild: oldChild not found and fallbackIndex issue. Adding newChild to panel. Index might be incorrect.");
                panel.getChildren().add(newChild);
            }
        }
        HBox.setHgrow(newChild, Priority.ALWAYS); // Đảm bảo Hgrow được áp dụng cho child mới
    }


    public void updateReportTable(ObservableList<ClassReportData> data) {
        if (reportTable == null) {
            System.err.println("ReportView.updateReportTable: reportTable is null!");
            if (controller != null) showError("Lỗi hiển thị: Bảng báo cáo chưa được khởi tạo.");
            return;
        }
        reportTable.setItems(data);
        if (data == null || data.isEmpty()) {
            reportTable.setPlaceholder(new Label("Không có dữ liệu cho tiêu chí đã chọn."));
        }
    }

    public void showError(String message) {
        System.err.println("SHOWING ERROR DIALOG: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (root != null && root.getScene() != null && root.getScene().getWindow() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.showAndWait();
    }

    // Getters cho Controller
    public DatePicker getFromDatePicker() { return fromDatePicker; }
    public DatePicker getToDatePicker() { return toDatePicker; }
    // public ComboBox<String> getStatusComboBox() { return statusComboBox; } // ĐÃ BỎ
    public Button getSearchButton() { return searchButton; }
    public Button getExportPdfButton() { return exportPdfButton; }
    public Button getExportExcelButton() { return exportExcelButton; }
    public Button getPrintButton() { return printButton; }


    private <S, T> void setBlackHeaderText(TableColumn<S, T> column, String title) {
        Label headerLabel = new Label(title);
        headerLabel.setTextFill(Color.BLACK);
        headerLabel.setFont(Font.font("System", FontWeight.NORMAL, 12)); // Font nhỏ hơn, không bold
        // headerLabel.setAlignment(Pos.CENTER_LEFT); // Căn trái cho header
        // headerLabel.setMaxWidth(Double.MAX_VALUE); // Cho phép label chiếm hết chiều rộng cột
        // HBox headerBox = new HBox(headerLabel);
        // headerBox.setAlignment(Pos.CENTER_LEFT);
        // headerBox.setPadding(new Insets(5));
        column.setGraphic(headerLabel);
        column.setText(""); // Quan trọng: Xóa text mặc định của header để chỉ graphic được hiển thị
    }
}