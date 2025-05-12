package view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import src.model.report.ReportModel.ClassReportData;


import view.BaseScreenView;

import java.time.LocalDate;
import java.util.List;

public class ReportView extends BaseScreenView {

    // UI Components
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private ComboBox<String> statusComboBox;
    private Button searchButton;
    private Button exportPdfButton;
    private Button exportExcelButton;
    private Button printButton;
    private TableView<ClassReportData> reportTable;

    // Progress circles
    private VBox attendanceProgressBox;
    private VBox homeworkProgressBox;
    private Label awarenessValue;
    private Label punctualityValue;
    private Label homeworkScoreValue;

    // Current metrics values
    private double attendancePercentage = 0;
    private double homeworkPercentage = 0;

    public ReportView() {
        super("Báo cáo tình hình học tập", "learning-reports");
    }

    @Override
    public void initializeView() {
        root.setSpacing(20);
        root.setPadding(new Insets(20));

        // Add filter panel
        root.getChildren().add(createFilterPanel());

        // Add title
        Text titleText = new Text("Báo cáo tình hình học tập");
        titleText.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleText.setFill(Color.web("#0078D7"));
        HBox titleBox = new HBox(titleText);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(10, 0, 20, 0));
        root.getChildren().add(titleBox);

        // Add metrics cards
        root.getChildren().add(createMetricsPanel());

        // Add table
        root.getChildren().add(createReportTable());
    }

    private HBox createFilterPanel() {
        HBox filterPanel = new HBox(15);
        filterPanel.setPadding(new Insets(15));
        filterPanel.setAlignment(Pos.CENTER_LEFT);
        filterPanel.setStyle("-fx-background-color: #F0F0F0; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Date filter group (left side)
        HBox dateFilterGroup = new HBox(10);
        dateFilterGroup.setAlignment(Pos.CENTER_LEFT);

        // From date filter
        Label fromDateLabel = new Label("Từ ngày:");
        fromDateLabel.setTextFill(Color.BLACK);
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));

        // To date filter
        Label toDateLabel = new Label("Đến ngày:");
        toDateLabel.setTextFill(Color.BLACK);
        toDatePicker = new DatePicker(LocalDate.now());

        dateFilterGroup.getChildren().addAll(fromDateLabel, fromDatePicker, toDateLabel, toDatePicker);

        // Status filter (center)
        HBox statusFilterGroup = new HBox(10);
        statusFilterGroup.setAlignment(Pos.CENTER);

        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Tất cả", "Đang học", "Đã hoàn thành", "Đã hủy");
        statusComboBox.setValue("Tất cả");
        statusComboBox.setPrefWidth(150);

        statusFilterGroup.getChildren().addAll(statusLabel, statusComboBox);

        // Action buttons (right side)
        HBox actionButtonsGroup = new HBox(10);
        actionButtonsGroup.setAlignment(Pos.CENTER_RIGHT);

        searchButton = new Button("Tìm kiếm");
        searchButton.setStyle("-fx-background-color: #0078D7; -fx-text-fill: white;");

        exportPdfButton = new Button("Xuất PDF");
        exportPdfButton.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");

        exportExcelButton = new Button("Xuất Excel");
        exportExcelButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white;");

        printButton = new Button("In");
        printButton.setStyle("-fx-background-color: #34495E; -fx-text-fill: white;");

        actionButtonsGroup.getChildren().addAll(searchButton, exportPdfButton, exportExcelButton, printButton);

        // Add all groups to the filter panel with proper spacing
        HBox.setHgrow(dateFilterGroup, Priority.ALWAYS);
        HBox.setHgrow(statusFilterGroup, Priority.NEVER);
        HBox.setHgrow(actionButtonsGroup, Priority.ALWAYS);

        filterPanel.getChildren().addAll(dateFilterGroup, statusFilterGroup, actionButtonsGroup);

        return filterPanel;
    }

    private HBox createMetricsPanel() {
        HBox metricsPanel = new HBox(20);
        metricsPanel.setAlignment(Pos.CENTER);
        metricsPanel.setPadding(new Insets(15));

        // Create attendance progress box
        attendanceProgressBox = createProgressCircleBox("Tỷ lệ đi học", attendancePercentage, "#4CAF50");

        // Create homework progress box
        homeworkProgressBox = createProgressCircleBox("Tiến trình làm bài tập", homeworkPercentage, "#5D62F9");

        // Create criteria box
        VBox criteriaBox = createCriteriaMetricsBox();

        metricsPanel.getChildren().addAll(attendanceProgressBox, homeworkProgressBox, criteriaBox);

        return metricsPanel;
    }

    private VBox createProgressCircleBox(String label, double percentage, String color) {
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setPadding(new Insets(15));
        progressBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        progressBox.setPrefWidth(300);
        progressBox.setPrefHeight(200);

        StackPane progressIndicator = new StackPane();
        progressIndicator.setPrefSize(150, 150);

        // Background circle (gray)
        Circle backgroundCircle = new Circle(60);
        backgroundCircle.setFill(Color.TRANSPARENT);
        backgroundCircle.setStroke(Color.LIGHTGRAY);
        backgroundCircle.setStrokeWidth(10);

        // Progress circle (colored based on parameter)
        Circle progressCircle = new Circle(60);
        progressCircle.setFill(Color.TRANSPARENT);
        progressCircle.setStroke(Color.web(color));
        progressCircle.setStrokeWidth(10);

        // Set the stroke dash array to create a partial circle based on the percentage
        double circumference = 2 * Math.PI * 60;
        double dashArray = (percentage / 100) * circumference;
        progressCircle.getStrokeDashArray().addAll(dashArray, circumference);

        // Percentage label
        Label percentageLabel = new Label(String.format("%.1f%%", percentage));
        percentageLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        percentageLabel.setTextFill(Color.BLACK);

        progressIndicator.getChildren().addAll(backgroundCircle, progressCircle, percentageLabel);

        Label titleLabel = new Label(label);
        titleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        titleLabel.setTextFill(Color.BLACK);

        progressBox.getChildren().addAll(progressIndicator, titleLabel);

        return progressBox;
    }

    private VBox createCriteriaMetricsBox() {
        VBox criteriaBox = new VBox(10);
        criteriaBox.setAlignment(Pos.CENTER_LEFT);
        criteriaBox.setPadding(new Insets(15));
        criteriaBox.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        criteriaBox.setPrefWidth(300);
        criteriaBox.setPrefHeight(200);

        // Title for the criteria box
        Label criteriaHeading = new Label("Tiêu chí đánh giá");
        criteriaHeading.setFont(Font.font("System", FontWeight.BOLD, 14));
        criteriaHeading.setTextFill(Color.BLACK);
        criteriaHeading.setAlignment(Pos.CENTER);
        criteriaHeading.setPrefWidth(Double.MAX_VALUE);

        // Criteria items
        VBox criteriaItems = new VBox(10);
        criteriaItems.setPadding(new Insets(5, 0, 0, 0));

        // Learning awareness
        HBox awarenessBox = new HBox();
        awarenessBox.setAlignment(Pos.CENTER_LEFT);
        Label awarenessLabel = new Label("Ý thức học:");
        awarenessLabel.setPrefWidth(150);
        awarenessLabel.setTextFill(Color.BLACK);
        awarenessValue = new Label("0 sao/học viên");
        awarenessValue.setPrefWidth(150);
        awarenessValue.setTextFill(Color.BLACK);
        awarenessBox.getChildren().addAll(awarenessLabel, awarenessValue);

        // Punctuality
        HBox punctualityBox = new HBox();
        punctualityBox.setAlignment(Pos.CENTER_LEFT);
        Label punctualityLabel = new Label("Đúng giờ:");
        punctualityLabel.setPrefWidth(150);
        punctualityLabel.setTextFill(Color.BLACK);
        punctualityValue = new Label("0 điểm/học viên");
        punctualityValue.setPrefWidth(150);
        punctualityValue.setTextFill(Color.BLACK);
        punctualityBox.getChildren().addAll(punctualityLabel, punctualityValue);

        // Homework score
        HBox homeworkScoreBox = new HBox();
        homeworkScoreBox.setAlignment(Pos.CENTER_LEFT);
        Label homeworkScoreLabel = new Label("Kết quả bài tập:");
        homeworkScoreLabel.setPrefWidth(150);
        homeworkScoreLabel.setTextFill(Color.BLACK);
        homeworkScoreValue = new Label("0/10 điểm");
        homeworkScoreValue.setPrefWidth(150);
        homeworkScoreValue.setTextFill(Color.BLACK);
        homeworkScoreBox.getChildren().addAll(homeworkScoreLabel, homeworkScoreValue);

        criteriaItems.getChildren().addAll(awarenessBox, punctualityBox, homeworkScoreBox);

        criteriaBox.getChildren().addAll(criteriaHeading, criteriaItems);

        return criteriaBox;
    }

    private VBox createReportTable() {
        VBox tableContainer = new VBox(10);
        tableContainer.setAlignment(Pos.CENTER);
        tableContainer.setPadding(new Insets(15));
        tableContainer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");

        // Table title
        Label tableTitle = new Label("Chi tiết tình hình học tập theo lớp");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        tableTitle.setTextFill(Color.BLACK);
        tableTitle.setAlignment(Pos.CENTER);
        tableTitle.setPrefWidth(Double.MAX_VALUE);

        // Create table
        reportTable = new TableView<>();
        reportTable.setPlaceholder(new Label("Không có dữ liệu"));
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Define columns
        TableColumn<ClassReportData, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setStyle("-fx-alignment: CENTER;");
        sttCol.setPrefWidth(50);

        TableColumn<ClassReportData, String> classNameCol = new TableColumn<>("Tên lớp");
        classNameCol.setCellValueFactory(new PropertyValueFactory<>("className"));
        classNameCol.setPrefWidth(150);

        TableColumn<ClassReportData, String> attendanceCol = new TableColumn<>("Số buổi học (Học/Tổng)");
        attendanceCol.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        attendanceCol.setStyle("-fx-alignment: CENTER;");
        attendanceCol.setPrefWidth(150);

        TableColumn<ClassReportData, String> homeworkCol = new TableColumn<>("Số bài tập về nhà (Làm/Tổng)");
        homeworkCol.setCellValueFactory(new PropertyValueFactory<>("homework"));
        homeworkCol.setStyle("-fx-alignment: CENTER;");
        homeworkCol.setPrefWidth(180);

        TableColumn<ClassReportData, Double> awarenessCol = new TableColumn<>("Ý thức học");
        awarenessCol.setCellValueFactory(new PropertyValueFactory<>("awareness"));
        awarenessCol.setCellFactory(tc -> new TableCell<ClassReportData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item == 0) {
                    setText("Không có dữ liệu");
                    setTextFill(Color.GRAY);
                    setGraphic(null);
                } else {
                    try {
                        int stars = (int) Math.round(item);
                        HBox starContainer = new HBox(2);
                        starContainer.setAlignment(Pos.CENTER);

                        for (int i = 0; i < 5; i++) {
                            Text star = new Text("★");
                            star.setFill(i < stars ? Color.GOLD : Color.LIGHTGRAY);
                            starContainer.getChildren().add(star);
                        }

                        setGraphic(starContainer);
                        setText(null);
                    } catch (NumberFormatException e) {
                        setText(item.toString());
                        setTextFill(Color.BLACK);
                        setGraphic(null);
                    }
                }
            }
        });
        awarenessCol.setStyle("-fx-alignment: CENTER;");
        awarenessCol.setPrefWidth(120);

        TableColumn<ClassReportData, Double> punctualityCol = new TableColumn<>("Đúng giờ");
        punctualityCol.setCellValueFactory(new PropertyValueFactory<>("punctuality"));
        punctualityCol.setCellFactory(tc -> new TableCell<ClassReportData, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%.2f", item));
                    setTextFill(Color.BLACK);
                    setGraphic(null);
                }
            }
        });
        punctualityCol.setStyle("-fx-alignment: CENTER;");
        punctualityCol.setPrefWidth(120);

        TableColumn<ClassReportData, String> homeworkScoreCol = new TableColumn<>("Điểm bài tập");
        homeworkScoreCol.setCellValueFactory(new PropertyValueFactory<>("homeworkScore"));
        homeworkScoreCol.setStyle("-fx-alignment: CENTER;");
        homeworkScoreCol.setPrefWidth(120);

        // Add columns to table
        reportTable.getColumns().addAll(
                sttCol, classNameCol, attendanceCol, homeworkCol,
                awarenessCol, punctualityCol, homeworkScoreCol
        );

        tableContainer.getChildren().addAll(tableTitle, reportTable);

        return tableContainer;
    }

    @Override
    public void refreshView() {
        // Implemented by controller to refresh the view with new data
    }

    public void updateMetrics(double attendancePercentage, double homeworkPercentage) {
        this.attendancePercentage = attendancePercentage;
        this.homeworkPercentage = homeworkPercentage;

        // Update the attendance progress circle
        root.getChildren().remove(attendanceProgressBox);
        attendanceProgressBox = createProgressCircleBox("Tỷ lệ đi học", attendancePercentage, "#4CAF50");

        // Update the homework progress circle
        root.getChildren().remove(homeworkProgressBox);
        homeworkProgressBox = createProgressCircleBox("Tiến trình làm bài tập", homeworkPercentage, "#5D62F9");

        // Update metrics panel
        HBox metricsPanel = (HBox) root.getChildren().get(2);
        metricsPanel.getChildren().clear();
        metricsPanel.getChildren().addAll(attendanceProgressBox, homeworkProgressBox, createCriteriaMetricsBox());
    }

    public void updateReportTable(ObservableList<ClassReportData> data) {
        reportTable.setItems(data);

        // Calculate and update criteria values
        double avgAwareness = data.stream().mapToDouble(ClassReportData::getAwareness).average().orElse(0);
        double avgPunctuality = data.stream().mapToDouble(ClassReportData::getPunctuality).average().orElse(0);

        // Calculate average homework score
        double totalScore = 0;
        int count = 0;
        for (ClassReportData item : data) {
            String scoreStr = item.getHomeworkScore();
            try {
                if (scoreStr.contains("/")) {
                    String[] parts = scoreStr.split("/");
                    double score = Double.parseDouble(parts[0]);
                    totalScore += score;
                    count++;
                }
            } catch (Exception ignored) {
                // Ignore parsing errors
            }
        }
        double avgScore = count > 0 ? totalScore / count : 0;

        // Update criteria values in the UI
        awarenessValue.setText(String.format("%.1f sao/học viên", avgAwareness));
        punctualityValue.setText(String.format("%.2f điểm/học viên", avgPunctuality));
        homeworkScoreValue.setText(String.format("%.2f/10 điểm", avgScore));
    }

    // Getters for UI components to be used by the controller
    public DatePicker getFromDatePicker() {
        return fromDatePicker;
    }

    public DatePicker getToDatePicker() {
        return toDatePicker;
    }

    public ComboBox<String> getStatusComboBox() {
        return statusComboBox;
    }

    public Button getSearchButton() {
        return searchButton;
    }

    public Button getExportPdfButton() {
        return exportPdfButton;
    }

    public Button getExportExcelButton() {
        return exportExcelButton;
    }

    public Button getPrintButton() {
        return printButton;
    }

    public TableView<ClassReportData> getReportTable() {
        return reportTable;
    }
}
