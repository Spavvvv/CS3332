package view.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import src.model.ClassSession;
import view.BaseScreenView;
import src.model.details.DetailsModel;
import src.controller.DetailsController;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Screen displaying details about a class session including
 * list of participating students, grade distribution and pass rate
 */
public class DetailsView extends BaseScreenView {
    private DetailsModel model;
    private DetailsController controller;

    // UI Components
    private Label headerLabel;
    private TableView<DetailsModel.StudentGradeModel> gradeTable;
    private BarChart<String, Number> gradeDistributionChart;
    private PieChart gradePieChart;
    private Button backButton;
    private Button exportGradesButton;

    public DetailsView() {
        super("Chi tiết buổi học", "details-view");
        this.controller = new DetailsController();
        this.model = new DetailsModel();
    }

    public DetailsView(ClassSession classSession) {
        super("Chi tiết buổi học", "details-view");
        this.controller = new DetailsController();

        // Load session details using controller
        if (classSession != null) {
            boolean loaded = controller.loadSessionDetails(classSession.getId());
            if (loaded) {
                this.model = controller.getCurrentModel();
            } else {
                this.model = new DetailsModel(classSession);
            }
        } else {
            this.model = new DetailsModel();
        }
    }

    @Override
    public void initializeView() {
        // Main layout setup
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E0E0E0; -fx-border-width: 1;");
        root.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Header and buttons
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0));

        // Header text
        ClassSession classSession = model.getClassSession();
        headerLabel = new Label(classSession != null ?
                "LỚP " + classSession.getClassName() + " - THÁNG " +
                        classSession.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")) + "/Điểm" :
                "NULL");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerLabel.setTextFill(Color.web("#0078d7"));

        // Buttons (positioned on the right)
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        backButton = new Button("← Quay lại");
        backButton.setStyle("-fx-background-color: #0078d7; -fx-text-fill: white; -fx-background-radius: 20;");
        backButton.setPadding(new Insets(8, 15, 8, 15));

        exportGradesButton = new Button("Xuất excel ▼");
        exportGradesButton.setStyle("-fx-background-color: #39ce1e; -fx-text-fill: white; -fx-background-radius: 20;");
        exportGradesButton.setPadding(new Insets(8, 15, 8, 15));

        buttonBox.getChildren().addAll(backButton, exportGradesButton);

        // Spacer to push buttons to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(headerLabel, spacer, buttonBox);
        mainLayout.setTop(headerBox);

        // Main content
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(0));
        mainLayout.setCenter(contentBox);

        // Student grade table
        gradeTable = createGradeTable();
        contentBox.getChildren().add(gradeTable);
        VBox.setVgrow(gradeTable, Priority.ALWAYS);

        // Charts - in HBox to display side by side
        HBox chartsBox = new HBox(20);
        chartsBox.setPrefHeight(400);

        // Grade distribution chart card
        VBox gradeDistributionCard = createCardLayout("Phổ điểm");
        gradeDistributionChart = createGradeDistributionChart();
        gradeDistributionCard.getChildren().add(gradeDistributionChart);
        HBox.setHgrow(gradeDistributionCard, Priority.ALWAYS);

        // Pass rate pie chart card
        VBox passRateCard = createCardLayout("Tỷ lệ đạt");
        gradePieChart = createPieChart();

        // HBox with pie chart and legend
        HBox pieChartWithLegend = new HBox(20);
        pieChartWithLegend.setAlignment(Pos.CENTER);

        // Vertical box for legend rows
        VBox legendVBox = new VBox(10);
        legendVBox.setAlignment(Pos.CENTER_LEFT);
        legendVBox.setPadding(new Insets(10, 0, 0, 0));

        // Add legend items
        addLegendItems(legendVBox);

        pieChartWithLegend.getChildren().addAll(gradePieChart, legendVBox);
        passRateCard.getChildren().add(pieChartWithLegend);
        HBox.setHgrow(passRateCard, Priority.ALWAYS);

        chartsBox.getChildren().addAll(gradeDistributionCard, passRateCard);
        contentBox.getChildren().add(chartsBox);

        // Load data from model via controller
        gradeTable.setItems(model.getStudentData());

        // Update charts based on table data
        updateCharts();

        // Set up event handlers
        setupEventHandlers();
    }

    /**
     * Create legend items for the pie chart
     */
    private void addLegendItems(VBox legendVBox) {
        // Create legend rows
        String[][] legendRows = {
                {"Mất gốc", "Yếu", "Trung bình"},
                {"Trung bình - Khá", "Khá", "Tốt", "Giỏi"}
        };

        Map<String, String> categoryColors = model.getCategoryColors();

        for (String[] row : legendRows) {
            HBox rowBox = new HBox(15);
            rowBox.setAlignment(Pos.CENTER_LEFT);

            for (String category : row) {
                HBox item = new HBox(5);
                item.setAlignment(Pos.CENTER_LEFT);

                // Circle with corresponding color
                Region colorBox = new Region();
                colorBox.setStyle(
                        "-fx-background-color: " + categoryColors.get(category) + ";" +
                                "-fx-min-width: 12px; -fx-min-height: 12px;" +
                                "-fx-max-width: 12px; -fx-max-height: 12px;"
                );

                // Label with larger font
                Label label = new Label(category);
                label.setFont(Font.font("System", FontWeight.NORMAL, 14));
                label.setTextFill(Color.BLACK);
                label.setStyle("-fx-opacity: 1.0;");

                item.getChildren().addAll(colorBox, label);
                rowBox.getChildren().add(item);
            }

            legendVBox.getChildren().add(rowBox);
        }
    }

    /**
     * Creates a table to display student grades
     */
    private TableView<DetailsModel.StudentGradeModel> createGradeTable() {
        TableView<DetailsModel.StudentGradeModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1;");

        // STT column
        TableColumn<DetailsModel.StudentGradeModel, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createObjectBinding(
                () -> cellData.getValue().getStt()));
        sttCol.setStyle("-fx-alignment: CENTER;");
        sttCol.setPrefWidth(50);

        // Student name column
        TableColumn<DetailsModel.StudentGradeModel, String> nameCol = new TableColumn<>("Họ và tên");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        nameCol.setPrefWidth(150);

        // Student ID column
        TableColumn<DetailsModel.StudentGradeModel, String> idCol = new TableColumn<>("Mã học sinh");
        idCol.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        idCol.setPrefWidth(100);
        idCol.setStyle("-fx-alignment: CENTER;");

        // Grade column
        TableColumn<DetailsModel.StudentGradeModel, String> gradeCol = new TableColumn<>("Điểm");
        gradeCol.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        gradeCol.setPrefWidth(80);
        gradeCol.setStyle("-fx-alignment: CENTER;");

        // Grade level column
        TableColumn<DetailsModel.StudentGradeModel, String> gradeLevelCol = new TableColumn<>("Phân loại");
        gradeLevelCol.setCellValueFactory(cellData -> cellData.getValue().gradeLevelProperty());
        gradeLevelCol.setPrefWidth(120);
        gradeLevelCol.setStyle("-fx-alignment: CENTER;");

        // Pass/fail column
        TableColumn<DetailsModel.StudentGradeModel, String> passCol = new TableColumn<>("Đạt");
        passCol.setCellValueFactory(cellData -> cellData.getValue().passProperty());
        passCol.setPrefWidth(60);
        passCol.setStyle("-fx-alignment: CENTER;");

        // Note column
        TableColumn<DetailsModel.StudentGradeModel, String> noteCol = new TableColumn<>("Ghi chú");
        noteCol.setCellValueFactory(cellData -> cellData.getValue().noteProperty());
        noteCol.setPrefWidth(150);

        table.getColumns().addAll(sttCol, nameCol, idCol, gradeCol, gradeLevelCol, passCol, noteCol);
        return table;
    }

    /**
     * Create a card layout with a title
     */
    private VBox createCardLayout(String title) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-border-radius: 4;");
        card.setPadding(new Insets(15));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        titleLabel.setTextFill(Color.BLACK);
        titleLabel.setPadding(new Insets(0, 0, 10, 0));

        card.getChildren().add(titleLabel);
        return card;
    }

    /**
     * Creates a bar chart for grade distribution
     */
    private BarChart<String, Number> createGradeDistributionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 1, 0.1);

        // X axis properties
        xAxis.setLabel("");
        xAxis.setTickLabelFill(Color.BLACK);
        xAxis.setTickLabelGap(0);
        xAxis.setStyle("-fx-tick-label-fill: black; -fx-tick-label-font-size: 10px;");

        // Y axis properties
        yAxis.setLabel("");
        yAxis.setTickLabelFill(Color.BLACK);
        yAxis.setTickLabelGap(5);
        yAxis.setTickUnit(1);
        yAxis.setStyle("-fx-tick-label-fill: black;");

        // Create chart
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("");
        barChart.setLegendVisible(true);
        barChart.setAnimated(false);
        barChart.setBarGap(5);
        barChart.setCategoryGap(5);
        barChart.setHorizontalGridLinesVisible(true);
        barChart.setHorizontalZeroLineVisible(true);
        barChart.setAlternativeRowFillVisible(false);
        barChart.setAlternativeColumnFillVisible(false);
        barChart.setStyle("-fx-horizontal-grid-line-color: #cccccc; -fx-horizontal-grid-line-opacity: 1.0;");

        // Set default values for X axis (grades from 0 to 10)
        ObservableList<String> categories = FXCollections.observableArrayList();
        for (double i = 0; i <= 10; i += 0.5) {
            // Format integers without decimal places, keep decimals as is
            String label = (i == Math.floor(i)) ? String.format("%.0f", i) : String.format("%.1f", i);
            categories.add(label);
        }
        xAxis.setCategories(categories);

        return barChart;
    }

    /**
     * Creates a pie chart for grade level distribution
     */
    private PieChart createPieChart() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("");
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        pieChart.setAnimated(false);
        pieChart.setPrefSize(200, 200);
        pieChart.setStyle("-fx-pie-label-visible: false; -fx-background-color: transparent;");

        return pieChart;
    }

    /**
     * Sets up event handlers for the view
     */
    private void setupEventHandlers() {
        backButton.setOnAction(e -> {
            navigationController.goBack();
        });

        exportGradesButton.setOnAction(e -> {
            boolean exported = false;
            //controller.exportSessionDetailsToExcel(model.getClassSession());
            if (exported) {
                // Show success notification (you might want to add a better notification system)
                System.out.println("Grades exported successfully");
            } else {
                System.out.println("Failed to export grades");
            }
        });
    }

    /**
     * Update both charts based on the data in the model
     */
    private void updateCharts() {
        updateGradeDistributionChart();
        updatePieChart();
    }

    /**
     * Update grade distribution chart from table data
     */
    private void updateGradeDistributionChart() {
        gradeDistributionChart.getData().clear();

        // Get chart data from controller
        XYChart.Series<String, Number> series = controller.getGradeDistributionData();

        // Find max value to adjust Y axis range
        int maxFrequency = 0;
        for (XYChart.Data<String, Number> data : series.getData()) {
            maxFrequency = Math.max(maxFrequency, data.getYValue().intValue());
        }

        // Set Y axis limits based on actual data
        NumberAxis yAxis = (NumberAxis) gradeDistributionChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Math.max(1, maxFrequency + 1));

        // Add series to chart
        gradeDistributionChart.getData().add(series);

        // Customize colors and sizes for columns
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                // Purple color for columns as in sample image
                node.setStyle("-fx-bar-fill: #5e35b1; -fx-bar-width: 3px;");

                // Add tooltip on mouse hover
                Tooltip tooltip = new Tooltip(
                        "Điểm " + data.getXValue() + ": " + data.getYValue() + " học sinh"
                );
                tooltip.setShowDelay(Duration.millis(50));

                Tooltip.install(node, tooltip);
            }
        }
    }

    /**
     * Update pie chart from table data
     */
    private void updatePieChart() {
        // Get data from controller
        ObservableList<PieChart.Data> pieChartData = controller.getPieChartData();
        Map<String, String> categoryColors = model.getCategoryColors();

        // Update chart
        gradePieChart.setData(pieChartData);

        // Customize colors and add tooltips
        for (PieChart.Data data : gradePieChart.getData()) {
            String category = data.getName();
            String color = categoryColors.getOrDefault(category, "#9ca3af");

            // Set color
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Add tooltip and show event on hover
            final Tooltip tooltip = new Tooltip(category + ": " + (int)data.getPieValue() + " học sinh");
            tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-text-fill: black; -fx-border-color: #E0E0E0;");

            // Make sure tooltip displays longer
            tooltip.setShowDelay(Duration.millis(50));
            tooltip.setHideDelay(Duration.millis(2000));

            // Use alternative approach to ensure tooltip displays
            data.getNode().setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    tooltip.show(data.getNode(), event.getScreenX() + 10, event.getScreenY() + 10);
                }
            });

            data.getNode().setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    tooltip.hide();
                }
            });

            data.getNode().setOnMouseMoved(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    tooltip.show(data.getNode(), event.getScreenX() + 10, event.getScreenY() + 10);
                }
            });
        }
    }

    /**
     * Load session details by ID
     */
    public boolean loadSession(long sessionId) {
        boolean loaded = controller.loadSessionDetails(sessionId);
        if (loaded) {
            this.model = controller.getCurrentModel();
            refreshView();
        }
        return loaded;
    }

    /**
     * Load session details by course name and date
     */
    public boolean loadSession(String courseName, java.time.LocalDate date) {
        boolean loaded = controller.loadSessionDetails(courseName, date);
        if (loaded) {
            this.model = controller.getCurrentModel();
            refreshView();
        }
        return loaded;
    }

    public void refreshView() {
        ClassSession classSession = model.getClassSession();
        if (classSession != null) {
            headerLabel.setText("LỚP " + classSession.getClassName() + " - THÁNG " +
                    classSession.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")) + "/Điểm");

            // Update table data
            gradeTable.setItems(model.getStudentData());

            // Update charts
            updateCharts();
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
