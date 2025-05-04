package view.components.ClassList;
import javafx.beans.property.SimpleStringProperty;
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
import view.BaseScreenView;

import java.time.format.DateTimeFormatter;
/**
 Màn hình Lớp học
 Hiển thị danh sách các lớp học và cho phép quản lý lớp học
 */
public class ClassListScreenView extends BaseScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0"; // Indigo color for buttons and headers
    private static final String GREEN_COLOR = "#4CAF50"; // Green color for progress
    private static final String YELLOW_COLOR = "#FFC107"; // Yellow color for in progress
    private static final String PURPLE_COLOR = "#673AB7"; // Purple color for stats
    private static final String LIGHT_GRAY = "#f8f9fa"; // Light gray for background
    private static final String WHITE_COLOR = "#FFFFFF"; // White color
    private static final String BORDER_COLOR = "#e0e0e0"; // Border color
    private static final String TEXT_COLOR = "#424242"; // Main text color
    // UI Components
    private Label titleLabel;
    private HBox statisticsContainer;
    private TextField searchField;
    private Button searchButton;
    private Button exportExcelButton;
    private ComboBox<String> pageSizeComboBox;
    private ComboBox<String> filterComboBox;
    private TableView<ClassInfo> classesTable;
    // Data
    private ObservableList<ClassInfo> classes = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public ClassListScreenView() {
        super("Lớp học", "classes");
        initializeData();
        initializeView();
    }
    private void initializeData() {
// database
        classes.add(new ClassInfo(1, "HS 1-1 Linh", "HS 1-1 Linh Phương", "Đang học", "0/100",
                "02/04/2025", "17/03/2026", "Admin Iclass", "Thứ 2 - 8:00\nThứ 3 - 8:00"));
        classes.add(new ClassInfo(2, "HS 1-1 Nguyễn Cao Xuân Phúc", "HS 1-1 Nguyễn Cao Xuân Phúc", "Đang học", "0/100",
                "01/04/2025", "16/03/2026", "Admin Iclass", "Thứ 2 - 8:00\nThứ 3 - 8:00"));
        classes.add(new ClassInfo(3, "HS 1-1 Nguyễn Đức Minh", "HS 1-1 Nguyễn Đức Minh", "Đang học", "0/100",
                "13/04/2025", "24/03/2026", "Admin Iclass", "Thứ 2 - 8:00\nThứ 3 - 8:00"));
        classes.add(new ClassInfo(4, "HS 1-1 Hoàng Hữu Nhật", "HS 1-1 Hoàng Hữu Nhật", "Đang học", "0/100",
                "02/04/2025", "17/03/2026", "Admin Iclass", "Thứ 2 - 8:00\nThứ 3 - 8:00"));
        classes.add(new ClassInfo(5, "HS 1-1 Hoàng Anh Khoa", "HS 1-1 Hoàng Anh Khoa TA", "Đang học", "0/50",
                "03/04/2025", "23/09/2025", "Admin Iclass", "Thứ 2 - 8:00\nThứ 3 - 8:00"));
        // Thêm trạng thái "Đang học" cho mỗi dòng
        for (ClassInfo info : classes) {
            info.setStatusLabel("Đang học");
        }


    }
    @Override
    public void initializeView() {
// Clear root
        root.getChildren().clear();
        // Setting up the root container
        root.setSpacing(0);
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");


        // Tạo BorderPane làm layout chính
        BorderPane mainLayout = new BorderPane();


        // Tạo VBox để chứa tất cả phần tử theo thứ tự từ trên xuống dưới
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));


        // Create title bar with export button
        HBox titleBar = createTitleBar();


        // Create statistics section
        statisticsContainer = createStatisticsSection();


        // Create search and filter section
        HBox searchAndFilterBar = createSearchAndFilterBar();


        // Create table
        createClassesTable();


        // Add components to contentBox in order
        contentBox.getChildren().addAll(
                titleBar,
                statisticsContainer,
                searchAndFilterBar,
                classesTable
        );


        // Set VBox.setVgrow for table to make it fill available space
        VBox.setVgrow(classesTable, Priority.ALWAYS);


        // Đặt contentBox vào phần CENTER của BorderPane
        mainLayout.setCenter(contentBox);


        // Thêm mainLayout vào root
        root.getChildren().add(mainLayout);


        // Đảm bảo mainLayout lấp đầy không gian có sẵn
        VBox.setVgrow(mainLayout, Priority.ALWAYS);


        // Set up event handlers
        setupEventHandlers();


    }
    /**
     Creates the title bar with title and action buttons
     */
    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");
// Title
        titleLabel = new Label("Lớp học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));
// Add a spacer to push the button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
// Export Excel button
        exportExcelButton = new Button();
        exportExcelButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );
// Icon cho nút Export
        Label excelIcon = new Label("📊");
        excelIcon.setTextFill(Color.WHITE);
        HBox exportContent = new HBox(7);
        exportContent.setAlignment(Pos.CENTER);
        exportContent.getChildren().addAll(excelIcon, new Label("Xuất Excel"));
        exportExcelButton.setGraphic(exportContent);
        titleBar.getChildren().addAll(titleLabel, spacer, exportExcelButton);
        return titleBar;
    }
    /**
     Create statistics cards for the top of the screen
     */
    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(20);
        statsContainer.setPadding(new Insets(0, 0, 15, 0));
        statsContainer.setAlignment(Pos.CENTER);
// Create 4 stat cards
        VBox openingCard = createStatCard("Khai giảng", GREEN_COLOR, "0", "11", "Tuần này", "Tháng này", "📝");
        VBox closingCard = createStatCard("Kết thúc", PRIMARY_COLOR, "0", "0", "Tuần này", "Tháng này", "🎓");
        VBox countCard = createStatCard("Sĩ số", YELLOW_COLOR, "1", "31", "Tối thiểu", "Tối đa", "👥");
        VBox statusCard = createStatCard("Trạng thái", PURPLE_COLOR, "0", "1", "Chờ duyệt", "Đang học", "⏱");
        statsContainer.getChildren().addAll(openingCard, closingCard, countCard, statusCard);
        return statsContainer;
    }
    /**
     Create a single statistic card with icon
     */
    private VBox createStatCard(String title, String color, String value1, String value2,
                                String label1, String label2, String icon) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(250);
// Card title with icon
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setTextFill(Color.WHITE);
        iconLabel.setFont(Font.font("System", 18));
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);
        titleBox.getChildren().addAll(iconLabel, titleLabel);
// Stats container
        HBox statsRow = new HBox(40);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.setPadding(new Insets(5, 0, 0, 0));
// First stat
        VBox stat1 = new VBox(5);
        stat1.setAlignment(Pos.CENTER_LEFT);
        Label value1Label = new Label(value1);
        value1Label.setFont(Font.font("System", FontWeight.BOLD, 24));
        value1Label.setTextFill(Color.WHITE);
        Label label1Label = new Label(label1);
        label1Label.setTextFill(Color.WHITE);
        stat1.getChildren().addAll(value1Label, label1Label);
// Second stat
        VBox stat2 = new VBox(5);
        stat2.setAlignment(Pos.CENTER_LEFT);
        Label value2Label = new Label(value2);
        value2Label.setFont(Font.font("System", FontWeight.BOLD, 24));
        value2Label.setTextFill(Color.WHITE);
        Label label2Label = new Label(label2);
        label2Label.setTextFill(Color.WHITE);
        stat2.getChildren().addAll(value2Label, label2Label);
        statsRow.getChildren().addAll(stat1, stat2);
        card.getChildren().addAll(titleBox, statsRow);
        return card;
    }
    /**
     * Creates a search and filter bar
     */
    private HBox createSearchAndFilterBar() {
        HBox searchAndFilterBar = new HBox(15);
        searchAndFilterBar.setPadding(new Insets(15, 0, 15, 0));
        searchAndFilterBar.setAlignment(Pos.CENTER_LEFT);
        // Left side - Filter options
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Từ khóa:");
        filterLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold;");

        filterComboBox = new ComboBox<>();
        filterComboBox.setPromptText("Từ khóa");
        filterComboBox.setPrefWidth(250);
        filterComboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;"
        );

        filterBox.getChildren().addAll(filterLabel, filterComboBox);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side - Search bar with page size
        HBox rightSideBox = new HBox(15);
        rightSideBox.setAlignment(Pos.CENTER_RIGHT);

        // Page size selector with label
        HBox pageSizeBox = new HBox(10);
        pageSizeBox.setAlignment(Pos.CENTER);

        Label pageSizeLabel = new Label("Cỡ trang");
        pageSizeLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");

        pageSizeComboBox = new ComboBox<>();
        pageSizeComboBox.getItems().addAll("10", "20", "50", "100");
        pageSizeComboBox.setValue("20");
        pageSizeComboBox.setPrefWidth(80);
        pageSizeComboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;"
        );

        pageSizeBox.getChildren().addAll(pageSizeLabel, pageSizeComboBox);

        // Search field with button
        HBox searchBox = new HBox(0);
        searchBox.setAlignment(Pos.CENTER);

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm");
        searchField.setPrefWidth(250);
        searchField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4 0 0 4;" +
                        "-fx-padding: 7;"
        );

        searchButton = new Button();
        searchButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 0 4 4 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 7 15;"
        );

        // Add search icon
        Label searchIcon = new Label("🔍");
        searchIcon.setTextFill(Color.WHITE);
        searchButton.setGraphic(searchIcon);

        searchBox.getChildren().addAll(searchField, searchButton);

        rightSideBox.getChildren().addAll(pageSizeBox, searchBox);

        // Add all components to the searchAndFilterBar
        searchAndFilterBar.getChildren().addAll(filterBox, spacer, rightSideBox);

        return searchAndFilterBar;
    }


    /**
     * Creates the table for classes
     */
    private void createClassesTable() {
        classesTable = new TableView<>();
        classesTable.setEditable(false);
        classesTable.setItems(classes);
        classesTable.setPrefHeight(600);

        // Styling the table
        classesTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-width: 1;"
        );


        // Selection column with checkboxes
        TableColumn<ClassInfo, Void> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(30);
        selectCol.setCellFactory(col -> {
            TableCell<ClassInfo, Void> cell = new TableCell<>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    checkBox.setOnAction(event -> {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        data.setSelected(checkBox.isSelected());
                    });
                }


                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        checkBox.setSelected(data.isSelected());
                        setGraphic(checkBox);
                    }
                }
            };
            return cell;
        });


        // STT column
        TableColumn<ClassInfo, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setPrefWidth(50);
        sttCol.setStyle("-fx-alignment: CENTER-LEFT;");


        // Mã column
        TableColumn<ClassInfo, String> codeCol = new TableColumn<>("Mã");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(150);


        // Tên lớp column
        TableColumn<ClassInfo, String> nameCol = new TableColumn<>("Tên lớp");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(180);
        nameCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Tiêu đề với "Đang học" ở dưới
                    VBox content = new VBox(3);
                    content.setAlignment(Pos.CENTER_LEFT);

                    // Tên lớp
                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-font-weight: bold;");

                    // Label trạng thái
                    Label statusLabel = new Label("Đang học");
                    statusLabel.setPadding(new Insets(2, 8, 2, 8));
                    statusLabel.setStyle(
                            "-fx-background-color: " + GREEN_COLOR + "20;" +
                                    "-fx-text-fill: " + GREEN_COLOR + ";" +
                                    "-fx-background-radius: 4;" +
                                    "-fx-font-size: 12px;"
                    );

                    content.getChildren().addAll(nameLabel, statusLabel);
                    setGraphic(content);
                    setText(null);
                }
            }
        });


        // Số buổi column
        TableColumn<ClassInfo, String> sessionsCol = new TableColumn<>("Số buổi");
        sessionsCol.setCellValueFactory(new PropertyValueFactory<>("numSessions"));
        sessionsCol.setPrefWidth(100);
        sessionsCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox progressBox = new HBox(5);
                    progressBox.setAlignment(Pos.CENTER_LEFT);


                    // Split the item to get current and total (format: "0/100")
                    String[] parts = item.split("/");
                    int current = Integer.parseInt(parts[0]);
                    int total = Integer.parseInt(parts[1]);


                    // Create the progress bar
                    ProgressBar progressBar = new ProgressBar((double)current/total);
                    progressBar.setPrefWidth(60);
                    progressBar.setStyle("-fx-accent: " + GREEN_COLOR + ";");


                    // Create the label
                    Label progressLabel = new Label(item);
                    progressLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-padding: 0 0 0 5;");


                    progressBox.getChildren().addAll(progressBar, progressLabel);
                    setGraphic(progressBox);
                    setText(null);
                }
            }
        });


        // Ngày bắt đầu column
        TableColumn<ClassInfo, String> startDateCol = new TableColumn<>("Ngày bắt đầu");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startDateCol.setPrefWidth(100);


        // Ngày kết thúc column
        TableColumn<ClassInfo, String> endDateCol = new TableColumn<>("Ngày kết thúc");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endDateCol.setPrefWidth(100);


        // Giáo viên column
        TableColumn<ClassInfo, String> teacherCol = new TableColumn<>("Giáo viên");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(120);


        // Quản lý column
        TableColumn<ClassInfo, String> managerCol = new TableColumn<>("Quản lý");
        managerCol.setCellValueFactory(new PropertyValueFactory<>("manager"));
        managerCol.setPrefWidth(120);


        // Ngày học column
        TableColumn<ClassInfo, String> classDateCol = new TableColumn<>("Ngày học");
        classDateCol.setCellValueFactory(new PropertyValueFactory<>("classDate"));
        classDateCol.setPrefWidth(150);
        classDateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Split by newline to show multiple days
                    String[] days = item.split("\n");
                    VBox daysBox = new VBox(3);
                    daysBox.setAlignment(Pos.CENTER_LEFT);

                    for (String day : days) {
                        Label dayLabel = new Label(day);
                        daysBox.getChildren().add(dayLabel);
                    }

                    setGraphic(daysBox);
                    setText(null);
                }
            }
        });


        // Học viên column with action button
        TableColumn<ClassInfo, Void> studentsCol = new TableColumn<>("Học viên");
        studentsCol.setCellFactory(col -> {
            TableCell<ClassInfo, Void> cell = new TableCell<>() {
                private final Button btn = new Button();
                {
                    // Button styling
                    btn.setStyle(
                            "-fx-background-color: " + PRIMARY_COLOR + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-padding: 5 15;" +
                                    "-fx-cursor: hand;"
                    );


                    // Content for button
                    HBox content = new HBox(5);
                    content.setAlignment(Pos.CENTER);
                    Label label = new Label("1/1 học viên");
                    label.setTextFill(Color.WHITE);
                    Label arrow = new Label("→");
                    arrow.setTextFill(Color.WHITE);
                    content.getChildren().addAll(label, arrow);
                    btn.setGraphic(content);


                    btn.setOnAction(event -> {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        handleViewStudents(data);
                    });
                }


                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
        studentsCol.setPrefWidth(120);


        // Khác column with action button
        TableColumn<ClassInfo, Void> actionsCol = new TableColumn<>("Khác");
        actionsCol.setCellFactory(col -> {
            TableCell<ClassInfo, Void> cell = new TableCell<>() {
                private final Button btn = new Button();
                {
                    btn.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-border-color: " + BORDER_COLOR + ";" +
                                    "-fx-border-radius: 30;" +
                                    "-fx-padding: 5 15;" +
                                    "-fx-cursor: hand;"
                    );


                    Label gearIcon = new Label("⚙");
                    gearIcon.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
                    btn.setGraphic(gearIcon);


                    btn.setOnAction(event -> {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        handleMoreActions(data);
                    });
                }


                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        });
        actionsCol.setPrefWidth(80);


        // Add all columns to the table
        classesTable.getColumns().addAll(
                selectCol, sttCol, codeCol, nameCol, sessionsCol,
                startDateCol, endDateCol, teacherCol, managerCol,
                classDateCol, studentsCol, actionsCol
        );


        // Customize table header style
        classesTable.setTableMenuButtonVisible(false);

        // Modern alternating row colors
        classesTable.setRowFactory(tv -> {
            TableRow<ClassInfo> row = new TableRow<>();
            row.setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
            row.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    row.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                } else {
                    if (row.getIndex() % 2 == 0) {
                        row.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                    } else {
                        row.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                    }
                }
            });
            return row;
        });
    }


    /**
     * Sets up event handlers for UI components
     */
    private void setupEventHandlers() {
        // Export Excel button
        exportExcelButton.setOnAction(e -> exportToExcel());


        // Search functionality
        searchButton.setOnAction(e -> searchClasses(searchField.getText()));
        searchField.setOnAction(e -> searchClasses(searchField.getText()));

        // Page size change
        pageSizeComboBox.setOnAction(e -> updatePageSize());
    }


    /**
     * Update number of rows shown in table
     */
    private void updatePageSize() {
        String selectedSize = pageSizeComboBox.getValue();
        if (selectedSize != null) {
            // In a real app, you would update the pagination here
            showInfo("Số dòng trên trang: " + selectedSize);
        }
    }


    /**
     * Export data to Excel
     */
    private void exportToExcel() {
        // This would integrate with an export service in a real implementation
        showInfo("Đang xuất danh sách lớp học sang Excel...");
    }


    /**
     * Search classes by keyword
     */
    private void searchClasses(String keyword) {
        // This would filter the table based on the search keyword
        showInfo("Đang tìm kiếm: " + keyword);
    }


    /**
     * Handle view students action
     */
    private void handleViewStudents(ClassInfo classInfo) {
        // Navigate to students list for this class
        showInfo("Xem danh sách học viên của lớp: " + classInfo.getName());
    }


    /**
     * Handle more actions for a class
     */
    private void handleMoreActions(ClassInfo classInfo) {
        // Show a context menu with more actions
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Chỉnh sửa");
        MenuItem deleteItem = new MenuItem("Xóa");
        MenuItem detailsItem = new MenuItem("Xem chi tiết");

        editItem.setOnAction(e -> showInfo("Chỉnh sửa lớp: " + classInfo.getName()));
        deleteItem.setOnAction(e -> showInfo("Xóa lớp: " + classInfo.getName()));
        detailsItem.setOnAction(e -> showInfo("Xem chi tiết lớp: " + classInfo.getName()));

        contextMenu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), detailsItem);

        Button source = (Button) ((HBox) classesTable.getScene().getFocusOwner()).getChildren().get(0);
        contextMenu.show(source, javafx.geometry.Side.BOTTOM, 0, 0);
    }


    @Override
    public void refreshView() {
        // Refresh the table data
        classesTable.refresh();
    }


    /**
     * Show information in a dialog
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    /**
     * Class representing a row in the classes table
     */
    public static class ClassInfo {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty status;
        private final SimpleStringProperty numSessions;
        private final SimpleStringProperty startDate;
        private final SimpleStringProperty endDate;
        private final SimpleStringProperty teacher;
        private final SimpleStringProperty manager;
        private final SimpleStringProperty classDate;
        private final int stt;
        private boolean selected;
        private String statusLabel;


        public ClassInfo(int stt, String code, String name, String status, String numSessions,
                         String startDate, String endDate, String teacher, String classDate) {
            this.stt = stt;
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.status = new SimpleStringProperty(status);
            this.numSessions = new SimpleStringProperty(numSessions);
            this.startDate = new SimpleStringProperty(startDate);
            this.endDate = new SimpleStringProperty(endDate);
            this.teacher = new SimpleStringProperty("");  // Default empty for sample
            this.manager = new SimpleStringProperty(teacher);  // Using teacher as manager for sample
            this.classDate = new SimpleStringProperty(classDate);
            this.selected = false;
        }


        public int getStt() {
            return stt;
        }


        public String getCode() {
            return code.get();
        }


        public String getName() {
            return name.get();
        }


        public String getStatus() {
            return status.get();
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String getStatusLabel() {
            return statusLabel;
        }


        public String getNumSessions() {
            return numSessions.get();
        }


        public String getStartDate() {
            return startDate.get();
        }


        public String getEndDate() {
            return endDate.get();
        }


        public String getTeacher() {
            return teacher.get();
        }


        public String getManager() {
            return manager.get();
        }


        public String getClassDate() {
            return classDate.get();
        }


        public boolean isSelected() {
            return selected;
        }


        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }


}
