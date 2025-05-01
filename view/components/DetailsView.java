package view.components;

import javafx.beans.property.SimpleStringProperty;
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

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Màn hình hiển thị thông tin chi tiết về buổi học bao gồm
 * danh sách học sinh tham gia, phổ điểm và tỷ lệ đạt
 */
public class DetailsView extends BaseScreenView {
    private ClassSession classSession;
    private Label headerLabel;
    private TableView<StudentGradeModel> gradeTable;
    private BarChart<String, Number> gradeDistributionChart;
    private PieChart gradePieChart;
    private Button backButton;
    private Button exportGradesButton;

    // Thêm hằng số phân loại điểm
    private static final double GRADE_MAT_GOC_MAX = 1.0;
    private static final double GRADE_YEU_MIN = 2.0;
    private static final double GRADE_YEU_MAX = 3.9;
    private static final double GRADE_TB_MIN = 4.0;
    private static final double GRADE_TB_MAX = 5.9;
    private static final double GRADE_TB_KHA_MIN = 6.0;
    private static final double GRADE_TB_KHA_MAX = 6.9;
    private static final double GRADE_KHA_MIN = 7.0;
    private static final double GRADE_KHA_MAX = 7.9;
    private static final double GRADE_TOT_MIN = 8.0;
    private static final double GRADE_TOT_MAX = 8.9;
    private static final double GRADE_GIOI_MIN = 9.0;

    // Màu sắc cho phân loại
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("Mất gốc", "#3b82f6");    // Xanh dương
        CATEGORY_COLORS.put("Yếu", "#ec4899");        // Hồng
        CATEGORY_COLORS.put("Trung bình", "#f97316"); // Cam
        CATEGORY_COLORS.put("Trung bình - Khá", "#fcd34d"); // Vàng
        CATEGORY_COLORS.put("Khá", "#14b8a6");        // Xanh lá mạ
        CATEGORY_COLORS.put("Tốt", "#a78bfa");        // Tím
        CATEGORY_COLORS.put("Giỏi", "#9ca3af");       // Xám
    }

    public DetailsView() {
        super("Chi tiết buổi học", "details-view");
    }

    public DetailsView(ClassSession classSession) {
        super("Chi tiết buổi học", "details-view");
        this.classSession = classSession;
    }

    @Override
    public void initializeView() {
        // Thiết lập layout chính
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #E0E0E0; -fx-border-width: 1;");
        root.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Phần header và buttons
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0));

        // Header text
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

        // Phần nội dung chính
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(0));
        mainLayout.setCenter(contentBox);

        // Bảng điểm học sinh
        gradeTable = createGradeTable();
        contentBox.getChildren().add(gradeTable);
        VBox.setVgrow(gradeTable, Priority.ALWAYS);

        // Phần biểu đồ - Đặt trong HBox để hiển thị cạnh nhau
        HBox chartsBox = new HBox(20);
        chartsBox.setPrefHeight(400);

        // Tạo card cho biểu đồ Phổ điểm
        VBox gradeDistributionCard = createCardLayout("Phổ điểm");
        gradeDistributionChart = createGradeDistributionChart();
        gradeDistributionCard.getChildren().add(gradeDistributionChart);
        HBox.setHgrow(gradeDistributionCard, Priority.ALWAYS);

        // Tạo card cho biểu đồ Tỷ lệ đạt
        VBox passRateCard = createCardLayout("Tỷ lệ đạt");
        gradePieChart = createPieChart();

        // Tạo HBox chứa pie chart và legend
        HBox pieChartWithLegend = new HBox(20);
        pieChartWithLegend.setAlignment(Pos.CENTER);

        // Tạo vbox đứng chứa các hàng legend
        VBox legendVBox = new VBox(10);
        legendVBox.setAlignment(Pos.CENTER_LEFT);
        legendVBox.setPadding(new Insets(10, 0, 0, 0));

        // Thêm các legend items
        addLegendItems(legendVBox);

        pieChartWithLegend.getChildren().addAll(gradePieChart, legendVBox);
        passRateCard.getChildren().add(pieChartWithLegend);
        HBox.setHgrow(passRateCard, Priority.ALWAYS);

        chartsBox.getChildren().addAll(gradeDistributionCard, passRateCard);
        contentBox.getChildren().add(chartsBox);

        // Thêm dữ liệu vào bảng
        addSampleData();

        // Cập nhật biểu đồ dựa trên dữ liệu bảng
        updateCharts();

        // Set up event handlers
        setupEventHandlers();
    }

    /**
     * Tạo các mục chú thích (legend) cho biểu đồ tròn
     */
    private void addLegendItems(VBox legendVBox) {
        // Tạo các hàng legend items
        String[][] legendRows = {
                {"Mất gốc", "Yếu", "Trung bình"},
                {"Trung bình - Khá", "Khá", "Tốt", "Giỏi"}
        };

        for (String[] row : legendRows) {
            HBox rowBox = new HBox(15);
            rowBox.setAlignment(Pos.CENTER_LEFT);

            for (String category : row) {
                HBox item = new HBox(5);
                item.setAlignment(Pos.CENTER_LEFT);

                // Circle với màu tương ứng
                Region colorBox = new Region();
                colorBox.setStyle(
                        "-fx-background-color: " + CATEGORY_COLORS.get(category) + ";" +
                                "-fx-min-width: 12px; -fx-min-height: 12px;" +
                                "-fx-max-width: 12px; -fx-max-height: 12px;"
                );

                // Nhãn với phông chữ lớn hơn và đậm hơn
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
     * Thêm dữ liệu mẫu vào bảng (phù hợp với hình ảnh đã gửi)
     */
    private void addSampleData() {
        ObservableList<StudentGradeModel> data = FXCollections.observableArrayList();

        data.add(new StudentGradeModel(1, "Nguyễn Văn A", "HS001", 8.5, "Tốt", true, ""));
        data.add(new StudentGradeModel(2, "Trần Thị B", "HS002", 9.5, "Giỏi", true, ""));
        data.add(new StudentGradeModel(3, "Lê Văn C", "HS003", 7.0, "Khá", true, ""));
        data.add(new StudentGradeModel(4, "Phạm Thị D", "HS004", 6.5, "Trung bình - Khá", true, ""));
        data.add(new StudentGradeModel(5, "Hoàng Văn E", "HS005", 5.0, "Trung bình", true, ""));
        data.add(new StudentGradeModel(6, "Ngô Thị F", "HS006", 3.5, "Yếu", false, "Cần cố gắng"));
        data.add(new StudentGradeModel(7, "Vũ Văn G", "HS007", 8.5, "Tốt", true, ""));
        data.add(new StudentGradeModel(8, "Đặng Thị H", "HS008", 9.5, "Giỏi", true, ""));
        data.add(new StudentGradeModel(9, "Bùi Văn I", "HS009", 7.0, "Khá", true, ""));
        data.add(new StudentGradeModel(10, "Đỗ Thị K", "HS010", 4.5, "Trung bình", false, "Cần cố gắng"));

        gradeTable.setItems(data);
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
     * Creates a table to display student grades
     * @return TableView component with student grades
     */
    private TableView<StudentGradeModel> createGradeTable() {
        TableView<StudentGradeModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1;");

        // STT column
        TableColumn<StudentGradeModel, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createObjectBinding(
                () -> cellData.getValue().getStt()));
        sttCol.setStyle("-fx-alignment: CENTER;");
        sttCol.setPrefWidth(50);

        // Student name column
        TableColumn<StudentGradeModel, String> nameCol = new TableColumn<>("Họ và tên");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        nameCol.setPrefWidth(150);

        // Student ID column
        TableColumn<StudentGradeModel, String> idCol = new TableColumn<>("Mã học sinh");
        idCol.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        idCol.setPrefWidth(100);
        idCol.setStyle("-fx-alignment: CENTER;");

        // Grade column
        TableColumn<StudentGradeModel, String> gradeCol = new TableColumn<>("Điểm");
        gradeCol.setCellValueFactory(cellData -> cellData.getValue().gradeProperty());
        gradeCol.setPrefWidth(80);
        gradeCol.setStyle("-fx-alignment: CENTER;");

        // Grade level column
        TableColumn<StudentGradeModel, String> gradeLevelCol = new TableColumn<>("Phân loại");
        gradeLevelCol.setCellValueFactory(cellData -> cellData.getValue().gradeLevelProperty());
        gradeLevelCol.setPrefWidth(120);
        gradeLevelCol.setStyle("-fx-alignment: CENTER;");

        // Pass/fail column
        TableColumn<StudentGradeModel, String> passCol = new TableColumn<>("Đạt");
        passCol.setCellValueFactory(cellData -> cellData.getValue().passProperty());
        passCol.setPrefWidth(60);
        passCol.setStyle("-fx-alignment: CENTER;");

        // Note column
        TableColumn<StudentGradeModel, String> noteCol = new TableColumn<>("Ghi chú");
        noteCol.setCellValueFactory(cellData -> cellData.getValue().noteProperty());
        noteCol.setPrefWidth(150);

        table.getColumns().addAll(sttCol, nameCol, idCol, gradeCol, gradeLevelCol, passCol, noteCol);
        return table;
    }

    /**
     * Creates a bar chart for grade distribution
     * @return BarChart component
     */
    private BarChart<String, Number> createGradeDistributionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 1, 0.1);

        // Thiết lập các thuộc tính của trục X
        xAxis.setLabel("");
        xAxis.setTickLabelFill(Color.BLACK);
        xAxis.setTickLabelGap(0);
        xAxis.setStyle("-fx-tick-label-fill: black; -fx-tick-label-font-size: 10px;");

        // Thiết lập các thuộc tính của trục Y
        yAxis.setLabel("");
        yAxis.setTickLabelFill(Color.BLACK);
        yAxis.setTickLabelGap(5);
        yAxis.setTickUnit(1);
        yAxis.setStyle("-fx-tick-label-fill: black;");

        // Tạo biểu đồ
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("");
        barChart.setLegendVisible(true);
        barChart.setAnimated(false);
        barChart.setBarGap(5);
        barChart.setCategoryGap(5);
        barChart.setHorizontalGridLinesVisible(true);
        barChart.setHorizontalZeroLineVisible(true);

        // Thêm CSS cho đường lưới để làm cho chúng rõ ràng hơn
        barChart.setStyle("-fx-background-color: transparent; -fx-legend-visible: true; " +
                "-fx-horizontal-grid-lines-visible: true; " +
                "-fx-horizontal-grid-line-color: #cccccc; " +  // Màu đường lưới
                "-fx-horizontal-grid-line-opacity: 0.8; " +    // Độ mờ
                "-fx-horizontal-zero-line-visible: true; " +
                "-fx-horizontal-zero-line-color: #666666;");   // Màu đường zero

        // Thiết lập các giá trị mặc định cho trục X (các mức điểm từ 0 đến 10)
        ObservableList<String> categories = FXCollections.observableArrayList();
        for (double i = 0; i <= 10; i += 0.5) {
            // Định dạng số nguyên không có phần thập phân, số thập phân giữ nguyên
            String label = (i == Math.floor(i)) ? String.format("%.0f", i) : String.format("%.1f", i);
            categories.add(label);
        }
        xAxis.setCategories(categories);

        return barChart;
    }

    /**
     * Creates a pie chart for grade level distribution
     * @return PieChart component
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
            // Handle back button click - navigate back to previous screen
            System.out.println("Back button clicked");
            navigationController.goBack();
        });

        exportGradesButton.setOnAction(e -> {
            // Handle export grades button click - export grades data
            System.out.println("Export grades button clicked");
        });
    }

    /**
     * Update both charts based on the data in the table
     */
    private void updateCharts() {
        updateGradeDistributionChart();
        updatePieChart();
    }

    /**
     * Cập nhật biểu đồ phổ điểm từ dữ liệu bảng
     */
    private void updateGradeDistributionChart() {
        gradeDistributionChart.getData().clear();

        // Chuẩn bị dữ liệu đúng các mức điểm 0-10 với bước 0.5
        TreeMap<String, Integer> gradeFrequency = new TreeMap<>();

        // Khởi tạo tất cả các mức điểm với số lượng 0
        for (double i = 0; i <= 10; i += 0.5) {
            String grade = (i == Math.floor(i)) ? String.format("%.0f", i) : String.format("%.1f", i);
            gradeFrequency.put(grade, 0);
        }

        // Đếm tần suất xuất hiện của từng điểm từ bảng
        for (StudentGradeModel student : gradeTable.getItems()) {
            String gradeStr = student.getGrade();
            gradeFrequency.put(gradeStr, gradeFrequency.getOrDefault(gradeStr, 0) + 1);
        }

        // Tìm giá trị lớn nhất để điều chỉnh phạm vi trục Y
        int maxFrequency = 0;

        // Tạo series dữ liệu
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Học viên");

        // Thêm dữ liệu vào series
        for (Map.Entry<String, Integer> entry : gradeFrequency.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            maxFrequency = Math.max(maxFrequency, entry.getValue());
        }

        // Đặt giới hạn cho trục Y dựa trên dữ liệu thực tế
        NumberAxis yAxis = (NumberAxis) gradeDistributionChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Math.max(0, maxFrequency + 1));  // Đảm bảo có ít nhất 5 ô hoặc lớn hơn giá trị max

        // Thêm series vào biểu đồ
        gradeDistributionChart.getData().add(series);

        // Tùy chỉnh màu sắc và kích thước cho các cột
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                // Màu tím cho các cột như trong hình mẫu
                node.setStyle("-fx-bar-fill: #5e35b1; -fx-bar-width: 3px;");

                // Thêm tooltip khi di chuột vào cột
                Tooltip tooltip = new Tooltip(
                        "Điểm " + data.getXValue() + ": " + data.getYValue() + " học sinh"
                );
                tooltip.setShowDelay(Duration.millis(50));

                Tooltip.install(node, tooltip);
            }
        }
    }

    /**
     * Cập nhật biểu đồ tròn từ dữ liệu bảng
     */
    private void updatePieChart() {
        // Đếm số lượng học sinh theo từng phân loại
        Map<String, Integer> categoryCounts = new HashMap<>();

        // Khởi tạo các phân loại với số lượng 0
        for (String category : CATEGORY_COLORS.keySet()) {
            categoryCounts.put(category, 0);
        }

        // Đếm số lượng từng loại từ bảng
        for (StudentGradeModel student : gradeTable.getItems()) {
            String gradeLevel = student.getGradeLevel();
            categoryCounts.put(gradeLevel, categoryCounts.getOrDefault(gradeLevel, 0) + 1);
        }

        // Tạo dữ liệu cho biểu đồ
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > 0) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }

        // Cập nhật biểu đồ
        gradePieChart.setData(pieChartData);

        // Tùy chỉnh màu sắc và thêm tooltip
        for (PieChart.Data data : gradePieChart.getData()) {
            String category = data.getName();
            String color = CATEGORY_COLORS.getOrDefault(category, "#9ca3af");

            // Thiết lập màu sắc
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            // Thêm tooltip và sự kiện hiển thị khi hover
            final Tooltip tooltip = new Tooltip(category + ": " + (int)data.getPieValue() + " học sinh");
            tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-text-fill: black; -fx-border-color: #E0E0E0;");

            // Đảm bảo tooltip hiển thị lâu hơn
            tooltip.setShowDelay(Duration.millis(50));
            tooltip.setHideDelay(Duration.millis(2000));

            // Sử dụng cách khác để đảm bảo tooltip hiển thị
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

    public void refreshView() {
        if (classSession != null) {
            headerLabel.setText("LỚP " + classSession.getClassName() + " - THÁNG " +
                    classSession.getDate().format(DateTimeFormatter.ofPattern("MM/yyyy")) + "/Điểm");

            // Cập nhật lại biểu đồ
            updateCharts();
        }
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    /**
     * Model lưu trữ thông tin điểm số của học sinh để hiển thị trong bảng
     */
    public static class StudentGradeModel {
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty studentId;
        private final SimpleStringProperty grade;
        private final SimpleStringProperty gradeLevel;
        private final SimpleStringProperty pass;
        private final SimpleStringProperty note;
        private final int stt;

        public StudentGradeModel(int stt, String fullName, String studentId,
                                 double grade, String gradeLevel, boolean pass, String note) {
            this.stt = stt;
            this.fullName = new SimpleStringProperty(fullName);
            this.studentId = new SimpleStringProperty(studentId);

            // Format điểm
            String gradeStr;
            if (grade == Math.floor(grade)) {
                gradeStr = String.format("%.0f", grade);
            } else {
                gradeStr = String.format("%.1f", grade);
            }

            this.grade = new SimpleStringProperty(gradeStr);
            this.gradeLevel = new SimpleStringProperty(gradeLevel);
            this.pass = new SimpleStringProperty(pass ? "✓" : "✗");
            this.note = new SimpleStringProperty(note);
        }

        // Getters
        public int getStt() { return stt; }
        public String getFullName() { return fullName.get(); }
        public String getStudentId() { return studentId.get(); }
        public String getGrade() { return grade.get(); }
        public String getGradeLevel() { return gradeLevel.get(); }
        public String getPass() { return pass.get(); }
        public String getNote() { return note.get(); }

        // Property getters for JavaFX binding
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty studentIdProperty() { return studentId; }
        public SimpleStringProperty gradeProperty() { return grade; }
        public SimpleStringProperty gradeLevelProperty() { return gradeLevel; }
        public SimpleStringProperty passProperty() { return pass; }
        public SimpleStringProperty noteProperty() { return note; }
    }
}
