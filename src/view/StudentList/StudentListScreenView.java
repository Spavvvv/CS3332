package src.view.StudentList;
import javafx.scene.Node;
import javafx.stage.Stage;
import src.dao.Notifications.NotificationDAO;
import src.dao.Person.StudentDAO;
import src.model.person.Person;
import src.model.person.Student;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import src.model.Notification.NotificationService;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import src.view.components.Screen.BaseScreenView;
import src.model.person.Permission; // Import enum Permission (đảm bảo đường dẫn đúng)
import src.model.person.RolePermissions;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Màn hình Học viên
 * Hiển thị danh sách các học viên từ dữ liệu đăng ký
 */
public class StudentListScreenView extends BaseScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0"; // Indigo color for buttons and headers
    private static final String GREEN_COLOR = "#4CAF50"; // Green color for active students
    private static final String RED_COLOR = "#F44336"; // Red color for inactive students
    private static final String YELLOW_COLOR = "#FFC107"; // Yellow color for new students
    private static final String LIGHT_GRAY = "#f8f9fa"; // Light gray for background
    private static final String WHITE_COLOR = "#FFFFFF"; // White color
    private static final String BORDER_COLOR = "#e0e0e0"; // Border color
    private static final String TEXT_COLOR = "#424242"; // Main text color

    // Constants cho đường dẫn file
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // UI Components
    private Label titleLabel;
    private HBox statisticsContainer;
    private TextField searchField;
    private Button searchButton;
    private Button exportExcelButton;
    private Button addStudentButton;
    private ComboBox<String> pageSizeComboBox;
    private ComboBox<String> filterComboBox;
    private TableView<StudentInfo> studentsTable;

    // Data
    private ObservableList<StudentInfo> students = FXCollections.observableArrayList();
    private ObservableList<StudentInfo> filteredStudents = FXCollections.observableArrayList();

    public StudentListScreenView() {
        super("Học viên", "students");
        initializeView();
        initializeData();
    }

    private void initializeData() {
        // Clear old data
        students.clear();

        System.out.println("DEBUG: Starting initializeData method");

        try {
            // Instantiate the DAO and fetch data from the database
            StudentDAO studentDAO = new StudentDAO();
            System.out.println("DEBUG: Created StudentDAO instance");

            List<Student> studentList = studentDAO.getAllStudents(); // Fetch all students from the DB
            System.out.println("DEBUG: Retrieved student list. Size: " + (studentList != null ? studentList.size() : "null"));

            // Debug: Print student list details
            if (studentList != null) {
                for (Student s : studentList) {
                    System.out.println("DEBUG: Student found - ID: " + s.getId() + ", Name: " + s.getName());
                }
            }

            // Transform and populate each student into the ObservableList
            int stt = 1;
            if (studentList != null && !studentList.isEmpty()) {
                for (Student student : studentList) {
                    System.out.println("DEBUG: Processing student: " + student.getName());
                    StudentInfo studentInfo = new StudentInfo(
                            stt++,
                            student.getName(),
                            student.getBirthday(),
                            "Chưa có lớp",
                            student.getContactNumber(),
                            student.getStatus() != null ? student.getStatus().toUpperCase() : "Bảo lưu", // Gán trạng thái
                            student.getEmail(),
                            student.getId(),
                            student.getParentName() != null ? student.getParentName() : "Chưa điền",      // Gán Parent Name
                            student.getParentPhoneNumber() != null ? student.getParentPhoneNumber() : "Chưa điền" // Gán Parent Phone
                    );
                    students.add(studentInfo);
                    System.out.println("DEBUG: Added student to table: " + student.getName());
                }
            } else {
                System.out.println("DEBUG: Student list is empty or null");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Error retrieving student data: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DEBUG: Final students list size: " + students.size());

        // Set the filteredStudents list
        filteredStudents.setAll(students);
        System.out.println("DEBUG: Set filteredStudents. Size: " + filteredStudents.size());

        if (studentsTable != null) {
            studentsTable.setItems(filteredStudents);
            System.out.println("DEBUG: Set items to table");
            studentsTable.refresh();
            System.out.println("DEBUG: Refreshed table");
        } else {
            System.out.println("DEBUG: studentsTable is null");
        }
    }
    private int calculateAge(String birthDate) {
        try {
            LocalDate dob = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")); // ISO format
            LocalDate now = LocalDate.now();
            return Period.between(dob, now).getYears();
        } catch (DateTimeParseException e) {
            System.err.println("Lỗi định dạng ngày sinh: " + birthDate);
            return 0;
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

        // Create statistics section - chỉ giữ lại mục trạng thái
        statisticsContainer = createStatisticsSection();

        // Create search and filter section
        HBox searchAndFilterBar = createSearchAndFilterBar();

        // Create table
        createStudentsTable();
        System.out.println("DEBUG: Created students table");
        // Add components to contentBox in order
        contentBox.getChildren().addAll(
                titleBar,
                statisticsContainer,
                searchAndFilterBar,
                studentsTable
        );

        // Set VBox.setVgrow for table to make it fill available space
        VBox.setVgrow(studentsTable, Priority.ALWAYS);

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
     * Creates the title bar with title and action buttons
     */
    private HBox createTitleBar() {
        Person currentUser = getCurrentUser(); // Fetch the current user
        boolean canAddStudent = false;

// Check the ADD_STUDENT permission
        if (currentUser != null && currentUser.getRole() != null) {
            canAddStudent = RolePermissions.hasPermission(
                    currentUser.getRole(),
                    Permission.ADD_STUDENT
            );
        }

// Set visibility based on permission
        addStudentButton = new Button();
        addStudentButton.setVisible(canAddStudent);
        addStudentButton.setManaged(canAddStudent);
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");

        // Title
        titleLabel = new Label("Học viên - Trung Tâm Luyện Thi iClass");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        // Add a spacer to push the button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        addStudentButton.setStyle(
                "-fx-background-color: " + GREEN_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

// Icon cho nút Thêm học viên
        Label addIcon = new Label("➕");
        addIcon.setTextFill(Color.WHITE);
        HBox addContent = new HBox(7);
        addContent.setAlignment(Pos.CENTER);
        addContent.getChildren().addAll(addIcon, new Label("Thêm học viên"));
        addStudentButton.setGraphic(addContent);

// Thêm khoảng cách giữa các nút
        Region buttonSpacer = new Region();
        buttonSpacer.setPrefWidth(10);
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
        Label excelIcon = new Label("⚙");
        excelIcon.setTextFill(Color.WHITE);
        HBox exportContent = new HBox(7);
        exportContent.setAlignment(Pos.CENTER);
        exportContent.getChildren().addAll(excelIcon, new Label("Thực hiện"));
        exportExcelButton.setGraphic(exportContent);

        titleBar.getChildren().addAll(titleLabel, spacer, addStudentButton, buttonSpacer, exportExcelButton);
        return titleBar;
    }

    /**
     * Create statistics section - chỉ giữ lại mục trạng thái
     */
    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(20);
        statsContainer.setPadding(new Insets(0, 0, 15, 0));
        statsContainer.setAlignment(Pos.CENTER_LEFT);

        // Đếm số lượng học viên theo trạng thái
        long activeCount = students.stream().filter(s -> "Đang học".equals(s.getStatus())).count();
        long inactiveCount = students.stream().filter(s -> "Nghỉ học".equals(s.getStatus())).count();
        long newCount = students.stream().filter(s -> "Mới".equals(s.getStatus())).count();

        // Chỉ tạo 1 card thống kê trạng thái
        VBox statusCard = createStatCard("Trạng thái", PRIMARY_COLOR,
                String.valueOf(activeCount),
                String.valueOf(inactiveCount),
                String.valueOf(newCount),
                "Đang học", "Nghỉ học", "Mới", "👥");

        statsContainer.getChildren().add(statusCard);

        // Thêm region để lấp đầy không gian còn lại
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statsContainer.getChildren().add(spacer);

        return statsContainer;
    }

    /**
     * Create a status statistic card with icon
     */
    private VBox createStatCard(String title, String color,
                                String value1, String value2, String value3,
                                String label1, String label2, String label3,
                                String icon) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
        );
        card.setPrefWidth(300);

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
        HBox statsRow = new HBox(20);
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

        // Third stat
        VBox stat3 = new VBox(5);
        stat3.setAlignment(Pos.CENTER_LEFT);
        Label value3Label = new Label(value3);
        value3Label.setFont(Font.font("System", FontWeight.BOLD, 24));
        value3Label.setTextFill(Color.WHITE);
        Label label3Label = new Label(label3);
        label3Label.setTextFill(Color.WHITE);
        stat3.getChildren().addAll(value3Label, label3Label);

        statsRow.getChildren().addAll(stat1, stat2, stat3);
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
        searchAndFilterBar.setStyle("-fx-background-color: " + LIGHT_GRAY + "; -fx-background-radius: 5;");
        searchAndFilterBar.setPadding(new Insets(15));

        // Left side - Filter options
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Từ khóa:");
        filterLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Họ tên, điện thoại, email...");
        searchField.setPrefWidth(300);
        searchField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 7;"
        );

        // Filter button
        Button filterButton = new Button();
        filterButton.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 7;"
        );
        Label filterIcon = new Label("▼");
        filterButton.setGraphic(filterIcon);

        filterBox.getChildren().addAll(filterLabel, searchField, filterButton);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side - Page size selector
        HBox pageSizeBox = new HBox(10);
        pageSizeBox.setAlignment(Pos.CENTER_RIGHT);

        Label pageSizeLabel = new Label("Cỡ trang:");
        pageSizeLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold;");

        pageSizeComboBox = new ComboBox<>();
        pageSizeComboBox.getItems().addAll("10", "20", "50", "100");
        pageSizeComboBox.setValue("20");
        pageSizeComboBox.setPrefWidth(80);
        pageSizeComboBox.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 4;"
        );

        // Search button
        searchButton = new Button();
        searchButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 7 15;"
        );

        // Add search icon
        Label searchIcon = new Label("🔍");
        searchIcon.setTextFill(Color.WHITE);
        searchButton.setGraphic(searchIcon);

        pageSizeBox.getChildren().addAll(pageSizeLabel, pageSizeComboBox, searchButton);

        // Add all components to the searchAndFilterBar
        searchAndFilterBar.getChildren().addAll(filterBox, spacer, pageSizeBox);

        return searchAndFilterBar;
    }

    /**
     * Creates the table for students
     */
    private void createStudentsTable() {
        System.out.println("DEBUG: Starting createStudentsTable method");

        // Initialize the table
        studentsTable = new TableView<>();
        studentsTable.setEditable(false);
        studentsTable.setPrefHeight(600);

        // Initialize filteredStudents if null
        if (filteredStudents == null) {
            filteredStudents = FXCollections.observableArrayList();
            System.out.println("DEBUG: Initialized filteredStudents");
        }

        // Set items to table
        studentsTable.setItems(filteredStudents);
        System.out.println("DEBUG: Set items to table. Size: " + filteredStudents.size());

        // Styling the table
        String tableBaseStyle =
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-width: 1;";
// Styles for the header row
// Bạn có thể tùy chỉnh màu sắc và font chữ tại đây
        String headerRowStyle =
                ".table-view { " +
                        "-fx-table-header-row-background: #e3f2fd;" +  // Màu nền cho dòng tiêu đề
                        "-fx-font-weight: bold;" +                    // Chữ đậm
                        "-fx-text-fill: " + PRIMARY_COLOR + ";" +      // Màu chữ cho dòng tiêu đề
                        "-fx-background-radius: 5;" +                 // Bo tròn phần trên
                        "-fx-padding: 8px 12px;" +                    // Khoảng cách padding trong tiêu đề
                        "-fx-border-color: " + BORDER_COLOR + ";" +   // Màu đường viền xung quanh
                        "-fx-border-width: 0 0 2px 0;" +              // Đường viền phía dưới
                        "}";
        studentsTable.setStyle(tableBaseStyle + headerRowStyle);

        // Selection column with checkboxes
        TableColumn<StudentInfo, Void> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(30);
        selectCol.setCellFactory(col -> {
            return new TableCell<StudentInfo, Void>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    checkBox.setOnAction(event -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            StudentInfo data = getTableView().getItems().get(getIndex());
                            data.setSelected(checkBox.isSelected());
                        }
                    });
                }


                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            StudentInfo data = getTableView().getItems().get(getIndex());
                            checkBox.setSelected(data.isSelected());
                            setGraphic(checkBox);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
        });

        // STT column
        TableColumn<StudentInfo, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(cellData ->
                new javafx.beans.property.ReadOnlyObjectWrapper<>(studentsTable.getItems().indexOf(cellData.getValue()) + 1)
        );
        sttCol.setPrefWidth(50);
        sttCol.setStyle("-fx-alignment: CENTER;");
        // Họ và tên column
        TableColumn<StudentInfo, String> nameCol = new TableColumn<>("Họ và tên");
        nameCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getName());
            } else {
                return new SimpleStringProperty("");
            }
        });
        nameCol.setPrefWidth(150);

        // Ngày sinh column
        TableColumn<StudentInfo, String> birthDateCol = new TableColumn<>("Ngày sinh");
        birthDateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getBirthDate());
            } else {
                return new SimpleStringProperty("");
            }
        });
        birthDateCol.setPrefWidth(100);

        // Lớp học column
        TableColumn<StudentInfo, String> classCol = new TableColumn<>("Lớp học");
        classCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null && cellData.getValue().getClassName() != null) {
                return new SimpleStringProperty(cellData.getValue().getClassName());
            } else {
                return new SimpleStringProperty("Chưa có lớp"); // Placeholder nếu chưa có dữ liệu lớp học
            }
        });
        classCol.setPrefWidth(120);

        // Điện thoại column
        TableColumn<StudentInfo, String> phoneCol = new TableColumn<>("Điện thoại");
        phoneCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getPhone());
            } else {
                return new SimpleStringProperty("");
            }
        });
        phoneCol.setPrefWidth(120);
        // Email column
        TableColumn<StudentInfo, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getEmail());
            } else {
                return new SimpleStringProperty("");
            }
        });
        emailCol.setPrefWidth(180);

        // Trạng thái column
        TableColumn<StudentInfo, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                String status = cellData.getValue().getStatus(); // Lấy giá trị trạng thái từ StudentInfo
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    return new SimpleStringProperty("Đang học");
                } else {
                    return new SimpleStringProperty("Bảo lưu"); // Trả về "Bảo lưu" nếu null hoặc khác "ACTIVE"
                }
            } else {
                return new SimpleStringProperty("Bảo lưu"); // Giá trị mặc định
            }
        });
        statusCol.setPrefWidth(120);
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(item);
                    statusLabel.setPadding(new Insets(3, 10, 3, 10));

                    String statusColor;
                    switch (item) {
                        case "Đang học":
                            statusColor = GREEN_COLOR;
                            break;
                        case "Nghỉ học":
                            statusColor = RED_COLOR;
                            break;
                        case "Mới":
                            statusColor = YELLOW_COLOR;
                            break;
                        default:
                            statusColor = LIGHT_GRAY;
                    }

                    statusLabel.setStyle(
                            "-fx-background-color: " + statusColor + "20;" +
                                    "-fx-text-fill: " + statusColor + ";" +
                                    "-fx-background-radius: 4;"
                    );

                    setGraphic(statusLabel);
                    setText(null);
                }
            }
        });
        // Cột tên phụ huynh
        TableColumn<StudentInfo, String> parentCol = new TableColumn<>("Phụ huynh");
        parentCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getParentName()); // Sử dụng `getParentName()`
            } else {
                return new SimpleStringProperty("");
            }
        });
        parentCol.setPrefWidth(150);

// Cột số điện thoại phụ huynh
        TableColumn<StudentInfo, String> parentPhoneCol = new TableColumn<>("SĐT Phụ huynh");
        parentPhoneCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getParentPhone()); // Sử dụng `getParentPhone()`
            } else {
                return new SimpleStringProperty("");
            }
        });
        parentPhoneCol.setPrefWidth(150);

        // Chi tiết column
        TableColumn<StudentInfo, Void> detailsCol = new TableColumn<>("Chi tiết");
        detailsCol.setPrefWidth(80);
        detailsCol.setCellFactory(col -> {
            TableCell<StudentInfo, Void> cell = new TableCell<>() {
                private final Button btn = new Button();
                {
                    btn.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-border-color: " + BORDER_COLOR + ";" +
                                    "-fx-border-radius: 4;" +
                                    "-fx-padding: 3 8;" +
                                    "-fx-cursor: hand;"
                    );

                    Label eyeIcon = new Label("👁");
                    btn.setGraphic(eyeIcon);

                    btn.setOnAction(event -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            StudentInfo data = getTableView().getItems().get(getIndex());
                            handleViewDetails(data);
                        }
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
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        TableColumn<StudentInfo, Void> deleteActionColumn = new TableColumn<>("Hành động");
        deleteActionColumn.setCellFactory(param -> new TableCell<StudentInfo, Void>() {
            private final Button deleteButton = new Button("Xóa");
            {
                deleteButton.setStyle(
                        "-fx-background-color: #e53935; " +  // Màu đỏ đậm (có thể dùng #dc3545, #f44336, hoặc màu đỏ bạn thích)
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 5px; " +     // Bo tròn góc
                                "-fx-border-radius: 5px; " +
                                "-fx-padding: 6 12 6 12; " +        // Padding (top, right, bottom, left)
                                "-fx-cursor: hand; " +               // Con trỏ chuột hình bàn tay khi hover
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);" // Đổ bóng nhẹ
                );
                // Hiệu ứng khi di chuột qua (hover) - làm nút sáng hơn một chút
                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                        "-fx-background-color: #f44336; " + // Màu đỏ sáng hơn một chút khi hover
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-border-radius: 5px; " +
                                "-fx-padding: 6 12 6 12; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 6, 0, 0, 2);" // Bóng đậm hơn chút
                ));
                // Trở lại style gốc khi chuột rời đi
                deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                        "-fx-background-color: #e53935; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-border-radius: 5px; " +
                                "-fx-padding: 6 12 6 12; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);"
                ));
                deleteButton.setOnAction(event -> {
                    StudentInfo student = getTableView().getItems().get(getIndex());
                    // Confirmation dialog for deletion
                    Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmationAlert.setTitle("Xác nhận xóa");
                    confirmationAlert.setHeaderText(null);
                    confirmationAlert.setContentText("Bạn có chắc muốn xóa học sinh này?");
                    ButtonType confirmButtonType = new ButtonType("Chắc chắn", ButtonBar.ButtonData.OK_DONE);
                    ButtonType cancelButtonType = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirmationAlert.getButtonTypes().setAll(confirmButtonType, cancelButtonType);
                    confirmationAlert.showAndWait().ifPresent(type -> {
                        if (type == confirmButtonType) {
                            // Call database delete and refresh table
                            boolean success = deleteStudentFromDatabase(student.getUserId());
                            if (success) {
                                getTableView().getItems().remove(student);
                                getTableView().refresh();
                                showInfo("Đã xóa thành công học viên ");
                            } else {
                                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                                errorAlert.setTitle("Lỗi");
                                errorAlert.setHeaderText(null);
                                errorAlert.setContentText("Xóa học sinh thất bại. Vui lòng thử lại.");
                                errorAlert.showAndWait();
                            }
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
        // Add all columns to the table
        studentsTable.getColumns().setAll(
                selectCol,
                sttCol,
                nameCol,
                birthDateCol,
                classCol,
                phoneCol,
                parentCol, // Cột "Phụ huynh" (tên cha mẹ)
                parentPhoneCol, // Cột "SĐT Phụ huynh"
                emailCol,
                statusCol,
                detailsCol,
                deleteActionColumn
        );

        // Custom row styling
        studentsTable.setRowFactory(tv -> {
            TableRow<StudentInfo> row = new TableRow<StudentInfo>() {
                @Override
                protected void updateItem(StudentInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setStyle("-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                    } else {
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                        } else {
                            setStyle("-fx-background-color: #f8f9fa; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
                        }
                    }
                }
            };

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

        System.out.println("DEBUG: Finished setting up table");
    }
    private void setupEventHandlers() {
        // Export button (đổi tên thành Thực hiện button)
        exportExcelButton.setOnAction(e -> showActions());

        // Search functionality
        searchButton.setOnAction(e -> searchStudents(searchField.getText()));
        searchField.setOnAction(e -> searchStudents(searchField.getText()));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchStudents(newValue);
        });
        addStudentButton.setOnAction(e -> {
            showAddStudentDialog();
        });


        // Page size change
        pageSizeComboBox.setOnAction(e -> updatePageSize());
    }
    private void showAddStudentDialog() {
        // Lấy Stage từ Node gốc của màn hình hiện tại
        Stage primaryStage = (Stage) root.getScene().getWindow();

        NotificationDAO notificationDAO = new NotificationDAO(); // Giả sử NotificationDAO có constructor mặc định và tự quản lý kết nối
        NotificationService notificationService = new NotificationService(notificationDAO); // Truyền DAO vào service


        Person currentUser = getCurrentUser(); // Sử dụng phương thức getCurrentUser() đã có trong lớp của bạn
        String currentUserId = "SYSTEM_USER"; // Giá trị mặc định nếu không tìm thấy người dùng

        if (currentUser != null && currentUser.getId() != null && !currentUser.getId().isEmpty()) {
            currentUserId = currentUser.getId();
        } else {
            System.err.println("StudentListScreenView: Không thể xác định ID người dùng hiện tại. Sử dụng giá trị mặc định cho senderId.");
        }

        AddStudentDialog dialog = new AddStudentDialog(primaryStage, notificationService, currentUserId);

        // Hiển thị dialog
        dialog.showAndWait();

        // Cập nhật lại danh sách học viên sau khi đóng dialog
        refreshStudentData();
    }
    /**
     * Show action menu for Export button
     */


    /**
     * Cập nhật hiển thị thống kê
     */
    private void updateStatisticsDisplay() {
        // Code cập nhật hiển thị thống kê ở đây (nếu cần)
        // Ví dụ: cập nhật số lượng học viên theo trạng thái
        statisticsContainer.getChildren().clear();
        statisticsContainer.getChildren().add(createStatisticsSection());
    }

    private void showActions() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Thêm học viên");
        MenuItem importItem = new MenuItem("Nhập Excel");
        MenuItem exportItem = new MenuItem("Xuất Excel");
        MenuItem settingsItem = new MenuItem("Cài đặt");

        addItem.setOnAction(e -> showInfo("Thêm học viên mới"));
        importItem.setOnAction(e -> showInfo("Nhập danh sách học viên từ Excel"));
        exportItem.setOnAction(e -> showInfo("Xuất danh sách học viên sang Excel"));
        settingsItem.setOnAction(e -> showInfo("Cài đặt hiển thị"));

        contextMenu.getItems().addAll(addItem, importItem, exportItem, new SeparatorMenuItem(), settingsItem);
        contextMenu.show(exportExcelButton, javafx.geometry.Side.BOTTOM, 0, 0);
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
     * Search students by keyword
     */
    private void searchStudents(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            filteredStudents.setAll(students);
        } else {
            // Filter students by keyword
            ObservableList<StudentInfo> filtered = FXCollections.observableArrayList();
            String lowerKeyword = keyword.toLowerCase();

            for (StudentInfo student : students) {
                if (student.getName().toLowerCase().contains(lowerKeyword)) { // Chỉ tìm kiếm theo tên
                    filtered.add(student);
                }
            }

            filteredStudents.setAll(filtered);
        }

        // Cập nhật thống kê sau khi lọc
        updateStatistics();
    }

    /**
     * Cập nhật thống kê sau khi lọc dữ liệu
     */
    private void updateStatistics() {
        // Đếm số lượng học viên theo trạng thái trong danh sách đã lọc
        long activeCount = filteredStudents.stream().filter(s -> "Đang học".equals(s.getStatus())).count();
        long inactiveCount = filteredStudents.stream().filter(s -> "Nghỉ học".equals(s.getStatus())).count();
        long newCount = filteredStudents.stream().filter(s -> "Mới".equals(s.getStatus())).count();

        // Cập nhật lại phần thống kê
        statisticsContainer.getChildren().clear();
        VBox statusCard = createStatCard("Trạng thái", PRIMARY_COLOR,
                String.valueOf(activeCount),
                String.valueOf(inactiveCount),
                String.valueOf(newCount),
                "Đang học", "Nghỉ học", "Mới", "👥");

        statisticsContainer.getChildren().add(statusCard);

        // Thêm region để lấp đầy không gian còn lại
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        statisticsContainer.getChildren().add(spacer);
    }

    /**
     * Handle src.view details action
     */

    private void handleViewDetails(StudentInfo studentInfo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hồ sơ học viên: " + studentInfo.getName());
        alert.setHeaderText(null);
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: #f8f9fa;");
        // --- Phần 1: Thông tin cá nhân ---
        VBox personalInfoSection = new VBox(10);
        personalInfoSection.setPadding(new Insets(15));
        personalInfoSection.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );
        Label personalTitle = new Label("👤 Thông tin cá nhân");
        personalTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        personalTitle.setTextFill(Color.web("#0056b3"));
        GridPane personalGrid = new GridPane();
        personalGrid.setHgap(10);
        personalGrid.setVgap(8);
        // ID (Không chỉnh sửa)
        personalGrid.add(createDetailLabel("ID:", true), 0, 0);
        personalGrid.add(new HBox(5, createDetailLabel(studentInfo.getUserId(), false)), 1, 0);
        // Họ tên (Chỉnh sửa bằng TextField)
        personalGrid.add(createDetailLabel("Họ tên:", true), 0, 1);
        personalGrid.add(
                setupEditableTextField(studentInfo, studentInfo.getName(), "name", "Họ tên",
                        createEditButton("Họ tên")), // Truyền nút Sửa đã tạo
                1, 1
        );
        // Ngày sinh (Chỉnh sửa bằng DatePicker)
        personalGrid.add(createDetailLabel("Ngày sinh:", true), 0, 2);
        personalGrid.add(
                setupEditableDateField(studentInfo, studentInfo.getBirthDate(), "birthDate", "Ngày sinh",
                        createEditButton("Ngày sinh")),
                1, 2
        );

        // Phụ huynh (Chỉnh sửa bằng TextField)
        personalGrid.add(createDetailLabel("Phụ huynh:", true), 0, 3);
        personalGrid.add(
                setupEditableTextField(studentInfo, studentInfo.getParentName(), "parentName", "Phụ huynh",
                        createEditButton("Phụ huynh")),
                1, 3
        );
        // SĐT phụ huynh (Chỉnh sửa bằng TextField)
        personalGrid.add(createDetailLabel("SĐT phụ huynh:", true), 0, 4);
        HBox parentPhoneEditableBox = setupEditableTextField(
                studentInfo, studentInfo.getParentPhone(), "parentPhone", "SĐT phụ huynh",
                createEditButton("SĐT phụ huynh") // Nút Sửa
        );
        // Thêm nút Copy vào HBox này một cách cẩn thận
        // Nút copy sẽ nằm giữa Label/TextField và nút Sửa/Lưu
        // Cấu trúc của setupEditableTextField trả về HBox(labelOrInput, editButton)
        // Chúng ta cần chèn nút copy vào.
        HBox finalParentPhoneBox = new HBox(5);
        Node displayOrEditNodeForParentPhone = parentPhoneEditableBox.getChildren().get(0); // Label hoặc TextField
        Button editOrSaveButtonForParentPhone = (Button) parentPhoneEditableBox.getChildren().get(parentPhoneEditableBox.getChildren().size() -1 ); // Nút Sửa/Lưu
        finalParentPhoneBox.getChildren().addAll(
                displayOrEditNodeForParentPhone,
                createCopyButton(studentInfo.getParentPhone(), "Sao chép SĐT phụ huynh"), // Nút copy
                editOrSaveButtonForParentPhone
        );
        finalParentPhoneBox.setAlignment(Pos.CENTER_LEFT);
        // Cần đảm bảo logic của nút Sửa/Lưu vẫn hoạt động đúng với Node trong finalParentPhoneBox
        // Điều này phức tạp hơn, tạm thời đơn giản hóa SĐT phụ huynh chỉ có Sửa, không copy khi đang sửa.
        // Hoặc nút copy sẽ copy giá trị hiện tại của Label/TextField.
        // Đơn giản hóa: Tạo HBox riêng cho SĐT phụ huynh để xử lý dễ hơn
        Label parentPhoneValueLabel = createDetailLabel(studentInfo.getParentPhone(), false);
        Button parentPhoneEditButton = createEditButton("SĐT phụ huynh");
        HBox parentPhoneCellBox = new HBox(5,
                parentPhoneValueLabel,
                createCopyButton(studentInfo.getParentPhone(), "Sao chép SĐT phụ huynh"), // Copy giá trị label
                parentPhoneEditButton
        );
        parentPhoneCellBox.setAlignment(Pos.CENTER_LEFT);
        setupFieldEditLogic(studentInfo, "parentPhone", "SĐT phụ huynh", parentPhoneValueLabel, parentPhoneEditButton, parentPhoneCellBox, studentInfo.getParentPhone());
        personalGrid.add(parentPhoneCellBox, 1, 4);
        personalInfoSection.getChildren().addAll(personalTitle, createVerticalSpacer(10), personalGrid);
        // --- Phần 2: Thông tin liên hệ ---
        VBox contactInfoSection = new VBox(10); // Tương tự như trên
        contactInfoSection.setPadding(new Insets(15));
        contactInfoSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label contactTitle = new Label("📞 Thông tin liên hệ");
        contactTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        contactTitle.setTextFill(Color.web("#0056b3"));
        GridPane contactGrid = new GridPane();
        contactGrid.setHgap(10);
        contactGrid.setVgap(8);
        // Điện thoại (Chỉnh sửa bằng TextField)
        contactGrid.add(createDetailLabel("Điện thoại:", true), 0, 0);
        Label phoneValueLabel = createDetailLabel(studentInfo.getPhone(), false);
        Button phoneEditButton = createEditButton("SĐT");
        HBox phoneCellBox = new HBox(5,
                phoneValueLabel,
                createCopyButton(studentInfo.getPhone(), "Sao chép SĐT"),
                phoneEditButton
        );
        phoneCellBox.setAlignment(Pos.CENTER_LEFT);
        setupFieldEditLogic(studentInfo, "phone", "Điện thoại", phoneValueLabel, phoneEditButton, phoneCellBox, studentInfo.getPhone());
        contactGrid.add(phoneCellBox, 1, 0);

        // Email (Không có nút sửa trong code gốc, giữ nguyên)
        contactGrid.add(createDetailLabel("Email:", true), 0, 1);
        HBox emailBox = new HBox(5, createDetailLabel(studentInfo.getEmail(), false), createCopyButton(studentInfo.getEmail(), "Sao chép Email"));
        emailBox.setAlignment(Pos.CENTER_LEFT);
        contactGrid.add(emailBox, 1, 1);
        contactInfoSection.getChildren().addAll(contactTitle, createVerticalSpacer(10), contactGrid);
        // --- Phần 3: Thông tin học tập ---
        VBox academicInfoSection = new VBox(10); // Tương tự
        academicInfoSection.setPadding(new Insets(15));
        academicInfoSection.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label academicTitle = new Label("🎓 Thông tin học tập");
        academicTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        academicTitle.setTextFill(Color.web("#0056b3"));
        GridPane academicGrid = new GridPane();
        academicGrid.setHgap(10);
        academicGrid.setVgap(8);
        // Lớp học (Chỉnh sửa bằng ComboBox)
        academicGrid.add(createDetailLabel("Lớp học:", true), 0, 0);
        List<String> classOptions = Arrays.asList("Lớp A1", "Lớp B2", "Lớp C3", "Chưa xếp lớp"); // Lấy từ ClassDAO sau
        academicGrid.add(
                setupEditableComboBoxField(studentInfo, studentInfo.getClassName(), "className", "Lớp học",
                        createEditButton("Lớp học"), classOptions),
                1, 0
        );
        // Trạng thái (Chỉnh sửa bằng ComboBox)
        academicGrid.add(createDetailLabel("Trạng thái:", true), 0, 1);
        List<String> statusOptions = Arrays.asList("Đang học", "Nghỉ học", "Bảo lưu", "Mới"); // Các trạng thái hợp lệ
        // HBox cho trạng thái cần được xây dựng cẩn thận để giữ style màu mè
        Label statusValueLabelOriginal = createDetailLabel(studentInfo.getStatus(), false); // Label gốc để lấy style
        String initialStatusBgColor, initialStatusTextColor;
        // ... (copy logic switch case để lấy màu cho initialStatusBgColor, initialStatusTextColor)
        switch (studentInfo.getStatus()) {
            case "Đang học": initialStatusBgColor = GREEN_COLOR + "40"; initialStatusTextColor = GREEN_COLOR; break;
            case "Nghỉ học": initialStatusBgColor = RED_COLOR + "40"; initialStatusTextColor = RED_COLOR; break;
            case "Mới": initialStatusBgColor = YELLOW_COLOR + "40"; initialStatusTextColor = "#856404"; break;
            default: initialStatusBgColor = LIGHT_GRAY + "40"; initialStatusTextColor = TEXT_COLOR;
        }
        statusValueLabelOriginal.setStyle("-fx-background-color: " + initialStatusBgColor + ";" + "-fx-text-fill: " + initialStatusTextColor + ";" + "-fx-padding: 3px 8px;" + "-fx-background-radius: 4px;" + "-fx-font-weight: bold;");
        statusValueLabelOriginal.setMaxWidth(Double.MAX_VALUE);
        HBox statusCellBox = setupEditableComboBoxFieldWithStyledLabel(
                studentInfo, studentInfo.getStatus(), "status", "Trạng thái",
                createEditButton("Trạng thái"), statusOptions, statusValueLabelOriginal
        );
        statusCellBox.setAlignment(Pos.CENTER_LEFT);
        academicGrid.add(statusCellBox, 1, 1);
        academicInfoSection.getChildren().addAll(academicTitle, createVerticalSpacer(10), academicGrid);
        mainContent.getChildren().addAll(personalInfoSection, contactInfoSection, academicInfoSection);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(mainContent);
        dialogPane.setPrefWidth(550); // Tăng chiều rộng nếu cần
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        alert.getButtonTypes().setAll(new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE));
        alert.showAndWait();
    }
    private void setupFieldEditLogic(StudentInfo studentInfo, String fieldKey, String fieldDisplayName,
                                     Label valueLabel, Button actionButton, HBox container, String originalValueFromStudentInfo) {
        TextField editField = new TextField();
        editField.setPrefWidth(150); // Điều chỉnh nếu cần
        actionButton.setOnAction(event -> {
            Node currentDisplayNode = container.getChildren().get(0); // Node đầu tiên trong HBox là Label hoặc TextField
            if (actionButton.getText().equals("Sửa")) {
                editField.setText(valueLabel.getText()); // Lấy giá trị hiện tại từ Label
                // Thay thế Label bằng TextField trong container
                // Cần đảm bảo thứ tự các con của container không bị thay đổi ngoài ý muốn
                int valueLabelIndex = container.getChildren().indexOf(valueLabel);
                if (valueLabelIndex != -1) {
                    container.getChildren().set(valueLabelIndex, editField);
                } else if (container.getChildren().contains(currentDisplayNode) && currentDisplayNode instanceof Label) {
                    container.getChildren().set(container.getChildren().indexOf(currentDisplayNode), editField);
                }
                actionButton.setText("Lưu");
                editField.requestFocus();
            } else { // "Lưu"
                String newValue = editField.getText().trim();
                // originalValueFromStudentInfo đã được truyền vào
                if (newValue.isEmpty() && !"status".equals(fieldKey) && !"className".equals(fieldKey)) { // Một số trường có thể trống
                    showAlert(Alert.AlertType.ERROR, "Lỗi", fieldDisplayName + " không được để trống.");
                    valueLabel.setText(originalValueFromStudentInfo); // Khôi phục giá trị gốc

                    int editFieldIndex = container.getChildren().indexOf(editField);
                    if (editFieldIndex != -1) {
                        container.getChildren().set(editFieldIndex, valueLabel);
                    } else if (container.getChildren().contains(currentDisplayNode) && currentDisplayNode instanceof TextField){
                        container.getChildren().set(container.getChildren().indexOf(currentDisplayNode), valueLabel);
                    }
                    actionButton.setText("Sửa");
                    return;
                }
                saveIndividualFieldUpdate(studentInfo, fieldKey, newValue, originalValueFromStudentInfo, valueLabel, editField, actionButton, container, fieldDisplayName, null, null);
            }
        });
    }

    private HBox setupEditableTextField(StudentInfo studentInfo, String initialValue, String fieldKey, String fieldDisplayName, Button actionButton) {
        Label valueLabel = createDetailLabel(initialValue, false);
        TextField editField = new TextField();
        editField.setPrefWidth(150);
        HBox cellContent = new HBox(5, valueLabel, actionButton); // Ban đầu là Label và Button
        cellContent.setAlignment(Pos.CENTER_LEFT);
        actionButton.setOnAction(event -> {
            if (actionButton.getText().equals("Sửa")) {
                editField.setText(valueLabel.getText());
                cellContent.getChildren().set(0, editField); // Thay Label bằng TextField
                actionButton.setText("Lưu");
                editField.requestFocus();
            } else { // "Lưu"
                String newValue = editField.getText().trim();
                String originalValue = getOriginalValue(studentInfo, fieldKey, initialValue); // Lấy giá trị gốc thực sự
                if (newValue.isEmpty() && shouldNotBeEmpty(fieldKey)) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", fieldDisplayName + " không được để trống.");
                    valueLabel.setText(originalValue);
                    cellContent.getChildren().set(0, valueLabel);
                    actionButton.setText("Sửa");
                    return;
                }
                saveIndividualFieldUpdate(studentInfo, fieldKey, newValue, originalValue, valueLabel, editField, actionButton, cellContent, fieldDisplayName, null, null);
            }
        });
        return cellContent;
    }
    private HBox setupEditableComboBoxFieldWithStyledLabel(StudentInfo studentInfo, String initialValue, String fieldKey, String fieldDisplayName, Button actionButton, List<String> options, Label styledValueLabel) {
        // styledValueLabel đã được tạo và style từ bên ngoài
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(options));
        comboBox.setPrefWidth(150); // Hoặc Double.MAX_VALUE để chiếm không gian
        comboBox.setValue(initialValue);
        HBox cellContent = new HBox(5, styledValueLabel, actionButton); // Label đã style, và Button
        cellContent.setAlignment(Pos.CENTER_LEFT);
        // HBox.setHgrow(comboBox, Priority.ALWAYS); // Cho comboBox giãn ra nếu cần
        // HBox.setHgrow(styledValueLabel, Priority.ALWAYS);
        actionButton.setOnAction(event -> {
            if (actionButton.getText().equals("Sửa")) {
                comboBox.setValue(styledValueLabel.getText());
                cellContent.getChildren().set(0, comboBox); // Thay Label bằng ComboBox
                actionButton.setText("Lưu");
                comboBox.requestFocus();
            } else { // "Lưu"
                String newValue = comboBox.getValue();
                String originalValue = getOriginalValue(studentInfo, fieldKey, initialValue);
                if (newValue == null || newValue.isEmpty() && shouldNotBeEmpty(fieldKey)) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", fieldDisplayName + " không được để trống.");
                    styledValueLabel.setText(originalValue); // Khôi phục giá trị gốc
                    // Cập nhật lại style cho styledValueLabel dựa trên originalValue
                    updateLabelStyle(styledValueLabel, originalValue);
                    cellContent.getChildren().set(0, styledValueLabel);
                    actionButton.setText("Sửa");
                    return;
                }
                saveIndividualFieldUpdate(studentInfo, fieldKey, newValue, originalValue, styledValueLabel, comboBox, actionButton, cellContent, fieldDisplayName, null, null);
            }
        });
        return cellContent;
    }
    // Helper để lấy giá trị gốc chính xác từ StudentInfo
    private String getOriginalValue(StudentInfo studentInfo, String fieldKey, String fallbackInitialValue) {
        switch (fieldKey) {
            case "name": return studentInfo.getName();
            case "birthDate": return studentInfo.getBirthDate();
            case "parentName": return studentInfo.getParentName();
            case "parentPhone": return studentInfo.getParentPhone();
            case "phone": return studentInfo.getPhone();
            case "className": return studentInfo.getClassName();
            case "status": return studentInfo.getStatus();
            default: return fallbackInitialValue; // Hoặc throw exception nếu fieldKey không hợp lệ
        }
    }
    // Helper kiểm tra trường có được phép trống không
    private boolean shouldNotBeEmpty(String fieldKey) {
        // Ví dụ: tên, ngày sinh không được trống
        return fieldKey.equals("name") || fieldKey.equals("birthDate") || fieldKey.equals("phone");
    }
    // Helper cập nhật style cho Label (đặc biệt cho Trạng thái)
    private void updateLabelStyle(Label label, String status) {
        String bgColor, textColor;
        switch (status) {
            case "Đang học": bgColor = GREEN_COLOR + "40"; textColor = GREEN_COLOR; break;
            case "Nghỉ học": bgColor = RED_COLOR + "40"; textColor = RED_COLOR; break;
            case "Mới": bgColor = YELLOW_COLOR + "40"; textColor = "#856404"; break; // Giữ màu vàng cho text "Mới"
            default: bgColor = LIGHT_GRAY + "40"; textColor = TEXT_COLOR; // Màu mặc định
        }
        label.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-padding: 3px 8px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-font-weight: bold;"
        );
        label.setMaxWidth(Double.MAX_VALUE);
    }

    private HBox setupEditableDateField(StudentInfo studentInfo, String initialDateString,
                                        String fieldKey, String fieldDisplayName, Button actionButton) {
        Label valueLabel = createDetailLabel(initialDateString, false); // Hiển thị giá trị hiện tại
        DatePicker datePicker = new DatePicker();

        // Định dạng ISO (yyyy-MM-dd) để tương thích với cơ sở dữ liệu
        DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            if (initialDateString != null && !initialDateString.isEmpty()) {
                datePicker.setValue(LocalDate.parse(initialDateString, dbFormatter)); // Chuyển sang LocalDate
            }
        } catch (DateTimeParseException e) {
            System.err.println("Lỗi parse ngày tháng: " + initialDateString);
        }

        HBox cellContent = new HBox(5, valueLabel, actionButton);
        actionButton.setOnAction(event -> {
            if (actionButton.getText().equals("Sửa")) {
                cellContent.getChildren().set(0, datePicker);
                actionButton.setText("Lưu");
            } else { // Khi bấm "Lưu"
                LocalDate newDate = datePicker.getValue(); // Lấy giá trị từ DatePicker
                String formattedDate = (newDate != null) ? newDate.format(dbFormatter) : ""; // Đảm bảo định dạng yyyy-MM-dd

                if (formattedDate.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Ngày sinh không được để trống.");
                    return;
                }

                // Lưu vào cơ sở dữ liệu qua `saveIndividualFieldUpdate`
                saveIndividualFieldUpdate(studentInfo, fieldKey, formattedDate, initialDateString,
                        valueLabel, datePicker, actionButton, cellContent, fieldDisplayName, newDate, null);
            }
        });

        return cellContent;
    }
    private HBox setupEditableComboBoxField(StudentInfo studentInfo, String initialValue, String fieldKey, String fieldDisplayName, Button actionButton, List<String> options) {
        Label valueLabel = createDetailLabel(initialValue, false);
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(options));
        comboBox.setPrefWidth(150);
        comboBox.setValue(initialValue);
        HBox cellContent = new HBox(5, valueLabel, actionButton);
        cellContent.setAlignment(Pos.CENTER_LEFT);
        actionButton.setOnAction(event -> {
            if (actionButton.getText().equals("Sửa")) {
                comboBox.setValue(valueLabel.getText());
                cellContent.getChildren().set(0, comboBox);
                actionButton.setText("Lưu");
                comboBox.requestFocus();
            } else { // "Lưu"
                String newValue = comboBox.getValue();
                String originalValue = getOriginalValue(studentInfo, fieldKey, initialValue);
                if (newValue == null || newValue.isEmpty() && shouldNotBeEmpty(fieldKey)) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", fieldDisplayName + " không được để trống.");
                    valueLabel.setText(originalValue);
                    cellContent.getChildren().set(0, valueLabel);
                    actionButton.setText("Sửa");
                    return;
                }
                saveIndividualFieldUpdate(studentInfo, fieldKey, newValue, originalValue, valueLabel, comboBox, actionButton, cellContent, fieldDisplayName, null, null);
            }
        });
        return cellContent;
    }

// --- Phương thức phụ trợ (Helper Methods) ---

    private Label createDetailLabel(String text, boolean isTitle) {
        Label label = new Label(text);
        if (isTitle) {
            label.setFont(Font.font("System", FontWeight.BOLD, 13));
            label.setTextFill(Color.web("#495057")); // Màu xám đậm cho tiêu đề thuộc tính
        } else {
            label.setFont(Font.font("System", FontWeight.NORMAL, 13));
            label.setTextFill(Color.web("#212529")); // Màu đen cho giá trị
            label.setWrapText(true);
        }
        return label;
    }
    private Button createEditButton(String fieldName) {
        Button editBtn = new Button("Sửa");
        editBtn.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-background-color: #ffc107;" +
                        "-fx-text-fill: black;" +
                        "-fx-padding: 2px 6px;" +
                        "-fx-background-radius: 4px;"
        );
        editBtn.setOnAction(e -> {
            System.out.println("Sửa trường: " + fieldName);
            // TODO: mở hộp thoại chỉnh sửa
        });
        return editBtn;
    }


    private Button createCopyButton(String textToCopy, String buttonText) {
        Button copyButton = new Button("📋"); // Sử dụng icon copy
        Tooltip tooltip = new Tooltip(buttonText); // Hiển thị tooltip khi hover
        copyButton.setTooltip(tooltip);
        copyButton.setFont(Font.font("System", FontWeight.NORMAL, 12));
        copyButton.setPadding(new Insets(2, 5, 2, 5));
        copyButton.setStyle(
                "-fx-background-color: #e9ecef;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 3;" +
                        "-fx-background-radius: 3;" +
                        "-fx-text-fill: #495057;" +
                        "-fx-cursor: hand;"
        );

        copyButton.setOnAction(event -> {
            final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(textToCopy);
            clipboard.setContent(content);

            // Phản hồi trực quan
            String originalText = copyButton.getText();
            Tooltip originalTooltip = copyButton.getTooltip();
            copyButton.setText("✓ Đã chép");
            copyButton.setTooltip(new Tooltip("Đã sao chép!"));
            copyButton.setStyle( // Thay đổi style khi đã copy
                    "-fx-background-color: #d4edda;" + // Màu xanh lá nhạt
                            "-fx-border-color: #c3e6cb;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 3;" +
                            "-fx-background-radius: 3;" +
                            "-fx-text-fill: #155724;" + // Màu chữ xanh đậm
                            "-fx-cursor: default;"
            );

            // Đặt lại sau một khoảng thời gian
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(e -> {
                copyButton.setText(originalText);
                copyButton.setTooltip(originalTooltip);
                copyButton.setStyle( // Trả về style cũ
                        "-fx-background-color: #e9ecef;" +
                                "-fx-border-color: #ced4da;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 3;" +
                                "-fx-background-radius: 3;" +
                                "-fx-text-fill: #495057;" +
                                "-fx-cursor: hand;"
                );
            });
            pause.play();
        });
        return copyButton;
    }

    private Region createVerticalSpacer(double height) {
        Region spacer = new Region();
        spacer.setPrefHeight(height);
        return spacer;
    }



    @Override
    public void refreshView() {
        // Refresh the table data
        studentsTable.refresh();

        // Cập nhật thống kê
        updateStatistics();
    }

    /**
     * Refresh data from student account files
     */
    public void refreshStudentData() {
        initializeData();
        refreshView();
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
     * Class representing a row in the students table
     */
    public static class StudentInfo {
        private final SimpleStringProperty name;
        private final SimpleStringProperty birthDate;
        private final SimpleStringProperty className;
        private final SimpleStringProperty phone;
        private final SimpleStringProperty status;
        private final SimpleStringProperty email;
        private final SimpleStringProperty userId;
        private final SimpleStringProperty parentName;
        private final SimpleStringProperty parentPhone;
        private final int stt;
        private boolean selected;
        public StudentInfo(int stt, String name, String birthDate, String className, String phone,
                           String status, String email, String userId, String parentName, String parentPhone) {
            this.stt = stt;
            this.name = new SimpleStringProperty(name);
            this.birthDate = new SimpleStringProperty(birthDate);
            this.className = new SimpleStringProperty(className);
            this.phone = new SimpleStringProperty(phone);
            this.status = new SimpleStringProperty(status);
            this.email = new SimpleStringProperty(email);
            this.userId = new SimpleStringProperty(userId);
            this.selected = false;
            this.parentName = new SimpleStringProperty(parentName);
            this.parentPhone = new SimpleStringProperty(parentPhone);
        }
        public int getStt() {
            return stt;
        }
        // Name
        public String getName() {
            return name.get();
        }
        public void setName(String name) {
            this.name.set(name);
        }
        // BirthDate
        public String getBirthDate() {
            return birthDate.get();
        }
        public void setBirthDate(String birthDate) { // THÊM MỚI
            this.birthDate.set(birthDate);
        }
        // ClassName
        public String getClassName() {
            return className.get();
        }
        public void setClassName(String className) { // THÊM MỚI (nếu cần cho chỉnh sửa sau này)
            this.className.set(className);
        }
        // Phone
        public String getPhone() {
            return phone.get();
        }
        public void setPhone(String phone) { // THÊM MỚI
            this.phone.set(phone);
        }
        // ParentName
        public String getParentName() {
            return this.parentName.get();
        }
        public void setParentName(String parentName) { // THÊM MỚI
            this.parentName.set(parentName);
        }
        // ParentPhone
        public String getParentPhone() {
            return this.parentPhone.get();
        }
        public void setParentPhone(String parentPhone) { // THÊM MỚI
            this.parentPhone.set(parentPhone);
        }
        // Status
        public String getStatus() {
            return status.get();
        }
        public void setStatus(String status) {
            this.status.set(status);
        }
        // Email
        public String getEmail() {
            return email.get();
        }
        public void setEmail(String email) { // THÊM MỚI (nếu cần cho chỉnh sửa sau này)
            this.email.set(email);
        }
        // UserId
        public String getUserId() {
            return userId.get();
        }
        // Thông thường ID không nên có setter sau khi đã khởi tạo, nhưng nếu cần:
        // public void setUserId(String userId) {
        //     this.userId.set(userId);
        // }
        // Selected
        public boolean isSelected() {
            return selected;
        }
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        // Property accessors for TableView (nếu bạn dùng binding trực tiếp)
        public SimpleStringProperty nameProperty() {
            return name;
        }
        public SimpleStringProperty birthDateProperty() {
            return birthDate;
        }
        public SimpleStringProperty classNameProperty() {
            return className;
        }
        public SimpleStringProperty phoneProperty() {
            return phone;
        }
        public SimpleStringProperty statusProperty() {
            return status;
        }
        public SimpleStringProperty emailProperty() {
            return email;
        }
        public SimpleStringProperty userIdProperty() {
            return userId;
        }
        public SimpleStringProperty parentNameProperty() {
            return parentName;
        }
        public SimpleStringProperty parentPhoneProperty() {
            return parentPhone;
        }
    }
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private boolean deleteStudentFromDatabase(String studentId) {
        StudentDAO studentDAO = new StudentDAO(); // Instantiate DAO
        return studentDAO.delete(studentId); // Calls the DAO's delete method
    }
    private void saveIndividualFieldUpdate(StudentInfo studentInfo, String fieldKey, String newValueString, String originalValue,
                                           Label valueLabel, Node editControl, Button actionButton, HBox container, String fieldDisplayName,
                                           LocalDate newDateValue, List<String> newMultiSelectValues) {
        StudentDAO studentDAO = new StudentDAO(); // Tạo một đối tượng DAO
        boolean dbSuccess = false;

        try {
            Student studentToUpdate = new Student();
            studentToUpdate.setId(studentInfo.getUserId()); // ID của student
            studentToUpdate.setName(studentInfo.getName());
            studentToUpdate.setBirthday(studentInfo.getBirthDate());
            studentToUpdate.setContactNumber(studentInfo.getPhone());
            studentToUpdate.setParentName(studentInfo.getParentName());
            studentToUpdate.setParentPhoneNumber(studentInfo.getParentPhone());
            studentToUpdate.setStatus(studentInfo.getStatus());

            // Cập nhật cột tùy theo fieldKey
            switch (fieldKey) {
                case "name":
                    studentToUpdate.setName(newValueString);
                    studentInfo.setName(newValueString); // Cập nhật trong UI tạm thời
                    break;
                case "birthDate":
                    studentToUpdate.setBirthday(newValueString);
                    studentInfo.setBirthDate(newValueString);
                    break;
                case "phone":
                    studentToUpdate.setContactNumber(newValueString);
                    studentInfo.setPhone(newValueString);
                    break;
                case "parentName":
                    studentToUpdate.setParentName(newValueString);
                    studentInfo.setParentName(newValueString);
                    break;
                case "parentPhone":
                    studentToUpdate.setParentPhoneNumber(newValueString);
                    studentInfo.setParentPhone(newValueString);
                    break;
                case "status":
                    studentToUpdate.setStatus(newValueString);
                    studentInfo.setStatus(newValueString);
                    break;
                default:
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Cột không xác định: " + fieldKey);
                    return;
            }

            // Gửi yêu cầu cập nhật vào cơ sở dữ liệu
            dbSuccess = studentDAO.update(studentToUpdate);

            if (dbSuccess) {
                // Cập nhật giao diện nếu thao tác thành công
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật " + fieldDisplayName + " thành công.");
                valueLabel.setText(newValueString); // Cập nhật Label
                if ("status".equals(fieldKey)) {
                    updateLabelStyle(valueLabel, newValueString); // Đặc biệt xử lý style cho cột trạng thái
                }
                container.getChildren().set(container.getChildren().indexOf(editControl), valueLabel); // Thay label lại
                actionButton.setText("Sửa");
                refreshStudentTable(); // Làm mới bảng dữ liệu
            } else {
                throw new SQLException("Cập nhật thất bại tại cơ sở dữ liệu.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi Cơ Sở Dữ Liệu", "Không thể cập nhật " + fieldDisplayName + ": " + e.getMessage());
            valueLabel.setText(originalValue); // Khôi phục giá trị Label cũ
            if ("status".equals(fieldKey)) {
                updateLabelStyle(valueLabel, originalValue); // Khôi phục style trạng thái
            }
            container.getChildren().set(container.getChildren().indexOf(editControl), valueLabel); // Đặt lại Label
            actionButton.setText("Sửa");
        }
    }
    /**
     * Refreshes the data displayed in the students table.
     * It re-fetches the student data from the database and updates the TableView.
     */
    private void refreshStudentTable() {
        try {
            // Clear current items in the table
            studentsTable.getItems().clear();

            // Re-fetch the latest student data from the database
            StudentDAO studentDAO = new StudentDAO();
            List<Student> studentList = studentDAO.getAllStudents(); // Lấy danh sách học viên từ cơ sở dữ liệu

            // Convert Student objects into StudentInfo objects for the TableView
            ObservableList<StudentInfo> updatedStudentInfos = FXCollections.observableArrayList();

            int stt = 1;
            for (Student student : studentList) {
                StudentInfo studentInfo = new StudentInfo(
                        stt++,
                        student.getName(),
                        student.getBirthday(),
                        "Chưa có lớp", // Hoặc sử dụng student.getClassName() nếu có thông tin lớp
                        student.getContactNumber(),
                        student.getStatus() != null ? student.getStatus().toUpperCase() : "Bảo lưu",
                        student.getEmail(),
                        student.getId(),
                        student.getParentName() != null ? student.getParentName() : "Chưa điền",
                        student.getParentPhoneNumber() != null ? student.getParentPhoneNumber() : "Chưa điền"
                );
                updatedStudentInfos.add(studentInfo);
            }

            // Update the items in the TableView
            if (studentsTable != null) {
                studentsTable.setItems(updatedStudentInfos);
                studentsTable.refresh(); // Explicitly refresh the TableView to reflect new data
            }

        } catch (Exception e) {
            // Handle exceptions gracefully
            System.err.println("Error refreshing student table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
