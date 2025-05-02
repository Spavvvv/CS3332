package view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import utils.DatabaseConnection;
import view.BaseScreenView;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    // Data containers
    private double attendancePercentage = 0;
    private double homeworkPercentage = 0;
    private ObservableList<ClassReportData> classReportData = FXCollections.observableArrayList();

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

        // Load data
        loadReportData();

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

        // Nhóm 1: Bộ lọc ngày (vẫn ở bên trái)
        HBox dateFilterGroup = new HBox(10);
        dateFilterGroup.setAlignment(Pos.CENTER_LEFT);

        // From date filter
        Label fromLabel = new Label("Từ:");
        fromLabel.setMinWidth(30);
        fromLabel.setTextFill(Color.BLACK);
        fromDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        fromDatePicker.setPrefWidth(150);

        // To date filter
        Label toLabel = new Label("Đến:");
        toLabel.setMinWidth(30);
        toLabel.setTextFill(Color.BLACK);
        toDatePicker = new DatePicker(LocalDate.now());
        toDatePicker.setPrefWidth(150);

        // Filter button
        /*Button filterButton = new Button("⏷");
        filterButton.setPrefWidth(30);
        filterButton.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 3; -fx-background-radius: 3;");*/

        dateFilterGroup.getChildren().addAll(fromLabel, fromDatePicker, toLabel, toDatePicker);

        // Sử dụng Region để tạo khoảng cách linh hoạt giữa nhóm 1 và các nhóm khác
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Nhóm 2 và 3 kết hợp: Trạng thái + Các nút hành động (đều ở bên phải)
        HBox rightSideGroup = new HBox(15); // Tăng khoảng cách giữa trạng thái và nút tìm kiếm
        rightSideGroup.setAlignment(Pos.CENTER_RIGHT);

        // Trạng thái (đặt trước nút tìm kiếm)
        Label statusLabel = new Label("Trạng thái:");
        statusLabel.setTextFill(Color.BLACK);
        statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Đang học", "Nghỉ học", "Tất cả");
        statusComboBox.setValue("Đang học");
        statusComboBox.setPrefWidth(150);

        // Search button
        searchButton = new Button();
        searchButton.setGraphic(new Text("🔍"));
        searchButton.setStyle("-fx-background-color: #5D62F9; -fx-text-fill: white; -fx-background-radius: 3;");
        searchButton.setPrefWidth(40);
        searchButton.setPrefHeight(30);
        searchButton.setOnAction(e -> loadReportData());

        // Export buttons - tạo style đồng nhất
        String buttonStyle = "-fx-background-color: #E0E0E0; -fx-background-radius: 3;";

        exportPdfButton = new Button();
        exportPdfButton.setGraphic(new Text("📕"));
        exportPdfButton.setStyle(buttonStyle);
        exportPdfButton.setPrefWidth(40);
        exportPdfButton.setPrefHeight(30);

        exportExcelButton = new Button();
        exportExcelButton.setGraphic(new Text("Excel"));
        exportExcelButton.setStyle(buttonStyle);
        exportExcelButton.setPrefWidth(40);
        exportExcelButton.setPrefHeight(30);

        printButton = new Button();
        printButton.setGraphic(new Text("🖨"));
        printButton.setStyle(buttonStyle);
        printButton.setPrefWidth(40);
        printButton.setPrefHeight(30);

        // Thêm tất cả các thành phần vào nhóm bên phải theo thứ tự: trạng thái, tìm kiếm, xuất PDF, xuất Excel, in
        rightSideGroup.getChildren().addAll(statusLabel, statusComboBox, searchButton, exportPdfButton, exportExcelButton, printButton);

        // Thêm tất cả các nhóm vào filter panel: nhóm ngày - spacer - nhóm phải
        filterPanel.getChildren().addAll(dateFilterGroup, spacer, rightSideGroup);

        return filterPanel;
    }



    private HBox createMetricsPanel() {
        HBox metricsPanel = new HBox(50); // Khoảng cách giữa các thành phần
        metricsPanel.setAlignment(Pos.CENTER);
        metricsPanel.setPrefHeight(220); // Chiều cao cố định


        // Attendance circular progress (Vòng tròn tiến trình Đi học)
        VBox attendanceBox = createCircularProgress(
                "Đi học",
                attendancePercentage, // Giá trị từ dữ liệu
                Color.web("#4CAF50") // Màu xanh lá
        );

        // Homework circular progress (Vòng tròn tiến trình Bài tập)
        VBox homeworkBox = createCircularProgress(
                "Bài tập",
                homeworkPercentage, // Giá trị từ dữ liệu
                Color.web("#5D62F9") // Màu xanh dương
        );

        // Criteria metrics box (Bảng Tiêu chí đánh giá)
        VBox criteriaBox = createCriteriaMetricsBox();

        // Thêm các thành phần vào HBox
        metricsPanel.getChildren().addAll(attendanceBox, homeworkBox, criteriaBox);

        return metricsPanel;
    }


    // Tính toán tiến trình điểm danh
    private double calculateAttendancePercentage() {
        int totalPresentDays = 0;
        int totalDays = 0;
        for (ClassReportData data : classReportData) {
            String[] attendanceParts = data.getAttendance().split("/");
            if (attendanceParts.length == 2) {
                try {
                    totalPresentDays += Integer.parseInt(attendanceParts[0]);
                    totalDays += Integer.parseInt(attendanceParts[1]);

                    // In ra để kiểm tra từng giá trị
                    System.out.println("  → Đã đi học: " + totalPresentDays + ", Tổng ngày: " + totalDays);

                } catch (NumberFormatException ignored) {
                    // Bỏ qua lỗi định dạng
                }
            }
        }
        return totalDays > 0 ? (totalPresentDays / (double) totalDays) * 100 : 0;
    }

    // Tính toán tiến trình bài tập về nhà
    private double calculateHomeworkPercentage() {
        int totalCompletedHomework = 0;
        int totalHomework = 0;
        for (ClassReportData data : classReportData) {
            String[] homeworkParts = data.getHomework().split("/");
            if (homeworkParts.length == 2) {
                try {
                    totalCompletedHomework += Integer.parseInt(homeworkParts[0]);
                    totalHomework += Integer.parseInt(homeworkParts[1]);

                    // In ra để kiểm tra từng giá trị
                    System.out.println("  → Đã làm: " + totalCompletedHomework + ", Tổng bài: " + totalHomework);

                } catch (NumberFormatException ignored) {
                    // Bỏ qua lỗi định dạng
                }
            }
        }
        return totalHomework > 0 ? (totalCompletedHomework / (double) totalHomework) * 100 : 0;
    }

    // Cập nhật vòng tròn tiến trình
    private VBox createCircularProgress(String label, double percentage, Color color) {
        VBox progressBox = new VBox(10);
        progressBox.setAlignment(Pos.CENTER);

        StackPane progressIndicator = new StackPane();

        Circle backgroundCircle = new Circle(60);
        backgroundCircle.setFill(Color.TRANSPARENT);
        backgroundCircle.setStroke(Color.web("#EEEEEE"));
        backgroundCircle.setStrokeWidth(12);

        Circle progressCircle = new Circle(60);
        progressCircle.setFill(Color.TRANSPARENT);
        progressCircle.setStroke(color);
        progressCircle.setStrokeWidth(12);

        // Tính toán độ dài nét (stroke dash array)
        double circumference = 2 * Math.PI * 60; // Chu vi của vòng tròn
        double dashLength = (percentage / 100.0) * circumference;
        double gapLength = circumference - dashLength;

        progressCircle.getStrokeDashArray().addAll(dashLength, gapLength);
        progressCircle.setRotate(-90); // Quay vòng tròn bắt đầu từ đỉnh

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
        criteriaBox.setPrefWidth(300); // Chiều rộng cố định
        criteriaBox.setPrefHeight(100); // Chiều cao cố định

        // Tiêu đề cho bảng
        Label criteriaHeading = new Label("Tiêu chí đánh giá");
        criteriaHeading.setFont(Font.font("System", FontWeight.BOLD, 14));
        criteriaHeading.setTextFill(Color.BLACK);
        criteriaHeading.setAlignment(Pos.CENTER);
        criteriaHeading.setPrefWidth(Double.MAX_VALUE);

        // Các tiêu chí
        VBox criteriaItems = new VBox(10);
        criteriaItems.setPadding(new Insets(5, 0, 0, 0));

        // Ý thức học
        HBox awarenessBox = new HBox();
        awarenessBox.setAlignment(Pos.CENTER_LEFT);
        Label awarenessLabel = new Label("Ý thức học:");
        awarenessLabel.setPrefWidth(150);
        awarenessLabel.setTextFill(Color.BLACK);
        Label awarenessValue = new Label("0 sao/học viên"); // Bạn có thể thay giá trị
        awarenessValue.setPrefWidth(150);
        awarenessValue.setTextFill(Color.BLACK);
        awarenessBox.getChildren().addAll(awarenessLabel, awarenessValue);

        // Đi học đúng giờ
        HBox punctualityBox = new HBox();
        punctualityBox.setAlignment(Pos.CENTER_LEFT);
        Label punctualityLabel = new Label("Đi học đúng giờ:");
        punctualityLabel.setPrefWidth(150);
        punctualityLabel.setTextFill(Color.BLACK);
        Label punctualityValue = new Label("1 sao/học viên"); // Bạn có thể thay giá trị
        punctualityValue.setPrefWidth(150);
        punctualityValue.setTextFill(Color.BLACK);
        punctualityBox.getChildren().addAll(punctualityLabel, punctualityValue);

        // Điểm BTVN
        HBox homeworkScoreBox = new HBox();
        homeworkScoreBox.setAlignment(Pos.CENTER_LEFT);
        Label homeworkScoreLabel = new Label("Điểm BTVN:");
        homeworkScoreLabel.setPrefWidth(150);
        homeworkScoreLabel.setTextFill(Color.BLACK);
        Label homeworkScoreValue = new Label("10%"); // Bạn có thể thay giá trị
        homeworkScoreValue.setPrefWidth(150);
        homeworkScoreValue.setTextFill(Color.BLACK);
        homeworkScoreBox.getChildren().addAll(homeworkScoreLabel, homeworkScoreValue);

        // Thêm các tiêu chí vào bảng
        criteriaItems.getChildren().addAll(awarenessBox, punctualityBox, homeworkScoreBox);
        criteriaBox.getChildren().addAll(criteriaHeading, criteriaItems);

        return criteriaBox;
    }


    private VBox createReportTable() {
        VBox tableContainer = new VBox(10);
        tableContainer.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-background-radius: 5;");
        tableContainer.setPadding(new Insets(15));
        tableContainer.setPrefHeight(400); // Increased height for the table

        // Title for the table
        Label tableTitle = new Label("Danh sách lớp học");
        tableTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        tableTitle.setTextFill(Color.BLACK); // Ensure text is black
        tableTitle.setPadding(new Insets(0, 0, 10, 0));

        tableContainer.getChildren().add(tableTitle);

        // Set up table
        reportTable = new TableView<>();
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.setPrefHeight(350); // Increased height
        reportTable.setItems(classReportData); // Make sure to set the items
        reportTable.setStyle("-fx-text-fill: black");

        // Apply style to make header text black
        reportTable.setStyle("-fx-table-header-text-color: black; -fx-text-fill: black;");

        // STT column
        TableColumn<ClassReportData, Integer> sttColumn = new TableColumn<>("STT");
        sttColumn.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttColumn.setPrefWidth(50);
        sttColumn.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-text-fill: black;");

        // Class name column
        TableColumn<ClassReportData, String> classNameColumn = new TableColumn<>("Tên lớp");
        classNameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        classNameColumn.setPrefWidth(250);
        classNameColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Attendance column
        TableColumn<ClassReportData, String> attendanceColumn = new TableColumn<>("Đi học");
        attendanceColumn.setCellValueFactory(new PropertyValueFactory<>("attendance"));
        attendanceColumn.setCellFactory(col -> new ProgressBarTableCell());
        attendanceColumn.setPrefWidth(120);
        attendanceColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Homework column
        TableColumn<ClassReportData, String> homeworkColumn = new TableColumn<>("Bài tập về nhà");
        homeworkColumn.setCellValueFactory(new PropertyValueFactory<>("homework"));
        homeworkColumn.setCellFactory(col -> new ProgressBarTableCell());
        homeworkColumn.setPrefWidth(120);
        homeworkColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Awareness column
        TableColumn<ClassReportData, Double> awarenessColumn = new TableColumn<>("Ý thức học");
        awarenessColumn.setCellValueFactory(new PropertyValueFactory<>("awareness"));
        awarenessColumn.setPrefWidth(100);
        awarenessColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Punctuality column
        TableColumn<ClassReportData, Double> punctualityColumn = new TableColumn<>("Đi học đúng giờ");
        punctualityColumn.setCellValueFactory(new PropertyValueFactory<>("punctuality"));
        punctualityColumn.setPrefWidth(120);
        punctualityColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Punctuality column
        TableColumn<ClassReportData, Double> homeWorkColumn = new TableColumn<>("Điểm BTVN");
        homeWorkColumn.setCellValueFactory(new PropertyValueFactory<>("homeworkScore"));
        homeWorkColumn.setPrefWidth(120);
        homeWorkColumn.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        // Add columns to table - removed the homework score column as requested
        reportTable.getColumns().addAll(
                sttColumn, classNameColumn, attendanceColumn, homeworkColumn,
                awarenessColumn, punctualityColumn, homeWorkColumn
        );

        // Make sure all cells in the table have black text
        reportTable.setRowFactory(tv -> {
            TableRow<ClassReportData> row = new TableRow<>();
            row.setStyle("-fx-text-fill: black;");
            return row;
        });

        tableContainer.getChildren().add(reportTable);

        return tableContainer;
    }

    private void loadReportData() {
        try {
            // Clear existing data
            classReportData.clear();

            // Prepare date parameters
            String fromDate = fromDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String toDate = toDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String status = statusComboBox.getValue();

            // SQL query (example - adjust according to your actual database schema)
            String query = "SELECT c.class_name, " +
                    "COUNT(DISTINCT a.student_id) as total_students, " +
                    "SUM(CASE WHEN a.status = 'present' THEN 1 ELSE 0 END) as present_count, " +
                    "COUNT(DISTINCT a.attendance_date) as total_days, " +
                    "SUM(CASE WHEN h.completed = true THEN 1 ELSE 0 END) as completed_homework, " +
                    "COUNT(DISTINCT h.homework_id) as total_homework, " +
                    "AVG(s.awareness_score) as awareness, " +
                    "AVG(s.punctuality_score) as punctuality, " +
                    "AVG(h.score) as homework_score " +
                    "FROM classes c " +
                    "LEFT JOIN attendance a ON c.class_id = a.class_id AND a.attendance_date BETWEEN ? AND ? " +
                    "LEFT JOIN homework h ON c.class_id = h.class_id AND h.assigned_date BETWEEN ? AND ? " +
                    "LEFT JOIN student_metrics s ON c.class_id = s.class_id " +
                    "WHERE c.status = ? " +
                    "GROUP BY c.class_id, c.class_name " +
                    "ORDER BY c.class_name";

            // Execute query (this is an example - modify to match your actual schema)
            // In a real application, you should use proper prepared statements with parameters
            // For now, we'll simulate the data from the image
            simulateData();

            // Tính toán giá trị thực tế
            attendancePercentage = calculateAttendancePercentage();
            homeworkPercentage = calculateHomeworkPercentage();

            //reportTable.setItems(classReportData);

            // In a real application, you would use this code:
            /*
            ResultSet rs = DatabaseConnection.executeQuery(query, fromDate, toDate, fromDate, toDate, status);
            int counter = 1;
            while (rs.next()) {
                ClassReportData data = new ClassReportData(
                    counter++,
                    rs.getString("class_name"),
                    rs.getInt("present_count") + "/" + rs.getInt("total_students") * rs.getInt("total_days"),
                    rs.getInt("completed_homework") + "/" + rs.getInt("total_homework"),
                    rs.getDouble("awareness"),
                    rs.getDouble("punctuality"),
                    rs.getDouble("homework_score") + "/10"
                );
                classReportData.add(data);
            }
            */

            // Update summary metrics (would be calculated from data in real application)
            // For now, we use the values from the image
            //attendancePercentage = 84.2;
            //homeworkPercentage = 76.2;

        } catch (Exception e) {
            showError("Lỗi khi tải dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void simulateData() {
        // This method simulates the data shown in the image
        classReportData.add(new ClassReportData(1, "Lớp 6A2", "194/209", "120/194", 231.5, 239.38, "4.74/10"));
        classReportData.add(new ClassReportData(2, "Lớp 10 Cảnh Diệu", "375/452", "323/376", 8.75, 159.7, "6.84/10"));
        classReportData.add(new ClassReportData(3, "Lớp 11A1", "553/657", "479/553", 0.0, 214.72, "6.98/10"));
        classReportData.add(new ClassReportData(4, "Lớp 11A2", "387/426", "294/362", 0.0, 225.81, "6.21/10"));
        classReportData.add(new ClassReportData(5, "Lớp 12A1", "464/584", "296/465", 0.0, 191.79, "5.27/10"));
    }

    @Override
    public void refreshView() {
        loadReportData();
    }

    // Data model class for report table
    public static class ClassReportData {
        private final int stt;
        private final String className;
        private final String attendance;
        private final String homework;
        private final double awareness;
        private final double punctuality;
        private final String homeworkScore;

        public ClassReportData(int stt, String className, String attendance, String homework,
                               double awareness, double punctuality, String homeworkScore) {
            this.stt = stt;
            this.className = className;
            this.attendance = attendance;
            this.homework = homework;
            this.awareness = awareness;
            this.punctuality = punctuality;
            this.homeworkScore = homeworkScore;
        }

        public int getStt() { return stt; }
        public String getClassName() { return className; }
        public String getAttendance() { return attendance; }
        public String getHomework() { return homework; }
        public double getAwareness() { return awareness; }
        public double getPunctuality() { return punctuality; }
        public String getHomeworkScore() { return homeworkScore; }
    }

    // Custom TableCell to display progress bars for attendance and homework completion
    private class ProgressBarTableCell extends TableCell<ClassReportData, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            VBox container = new VBox(5);
            container.setAlignment(Pos.CENTER_LEFT);

            // Parse the fraction format (e.g., "194/209")
            String[] parts = item.split("/");
            if (parts.length == 2) {
                try {
                    double current = Double.parseDouble(parts[0]);
                    double total = Double.parseDouble(parts[1]);
                    double progress = current / total;

                    // Create label with black text
                    Label label = new Label(item);
                    label.setTextFill(Color.BLACK);

                    // Create progress bar
                    ProgressBar progressBar = new ProgressBar(progress);
                    progressBar.setPrefWidth(Double.MAX_VALUE);

                    // Set color based on column
                    String columnTitle = getTableColumn().getText();
                    if (columnTitle.equals("Đi học")) {
                        progressBar.setStyle("-fx-accent: #4CAF50;"); // Green
                    } else if (columnTitle.equals("Bài tập về nhà")) {
                        progressBar.setStyle("-fx-accent: #5D62F9;"); // Blue
                    }

                    container.getChildren().addAll(label, progressBar);
                    setGraphic(container);
                    setText(null);
                } catch (NumberFormatException e) {
                    setText(item);
                    setTextFill(Color.BLACK); // Ensure text is black
                    setGraphic(null);
                }
            } else {
                setText(item);
                setTextFill(Color.BLACK); // Ensure text is black
                setGraphic(null);
            }
        }
    }
}
