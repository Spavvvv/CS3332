package view.components.StudentList;
import javafx.stage.Stage;
import src.controller.MainController;
import src.dao.StudentDAO;
import src.model.person.Person;
import src.model.person.Student;
import view.components.StudentList.AddStudentDialog;
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
import src.model.person.Permission; // Import enum Permission (đảm bảo đường dẫn đúng)
import src.model.person.RolePermissions;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                            "Chưa có lớp", // Update with logic if className is available
                            student.getContactNumber(),
                            "Chưa điền", // Update with logic if parent details are available
                            "Đang học", // Replace with actual status logic if fetched
                            student.getEmail(),
                            student.getId()
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
            LocalDate dob = LocalDate.parse(birthDate, DATE_FORMATTER);
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
        studentsTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-width: 1;"
        );

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
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getClassName());
            } else {
                return new SimpleStringProperty("");
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
        // Phụ huynh column
        TableColumn<StudentInfo, String> parentCol = new TableColumn<>("Phụ huynh");
        parentCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() != null) {
                return new SimpleStringProperty(cellData.getValue().getParent());
            } else {
                return new SimpleStringProperty("");
            }
        });
        parentCol.setPrefWidth(150);

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
                if(cellData.getValue().toString() == "Active") {
                    return new SimpleStringProperty("Đang học");
                } else return new SimpleStringProperty("Đang bảo lưu");
            } else {
                return new SimpleStringProperty("");
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

        // Add all columns to the table
        studentsTable.getColumns().addAll(
                selectCol, sttCol, nameCol, birthDateCol, classCol,
                phoneCol, parentCol, emailCol, statusCol, detailsCol
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

        // Tạo dialog với tham số là stage chính
        AddStudentDialog dialog = new AddStudentDialog(primaryStage);

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
     * Handle view details action
     */

    private void handleViewDetails(StudentInfo studentInfo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hồ sơ học viên: " + studentInfo.getName());
        alert.setHeaderText(null); // Bỏ header mặc định

        // Main content VBox
        VBox mainContent = new VBox(20); // Khoảng cách giữa các phần
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: #f8f9fa;"); // Màu nền nhẹ cho dialog

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
        personalTitle.setTextFill(Color.web("#0056b3")); // Màu tiêu đề

        GridPane personalGrid = new GridPane();
        personalGrid.setHgap(10); // Khoảng cách ngang
        personalGrid.setVgap(8);  // Khoảng cách dọc

        personalGrid.add(createDetailLabel("ID:", true), 0, 0);
        personalGrid.add(createDetailLabel(studentInfo.getUserId(), false), 1, 0);

        personalGrid.add(createDetailLabel("Họ tên:", true), 0, 1);
        personalGrid.add(createDetailLabel(studentInfo.getName(), false), 1, 1);

        personalGrid.add(createDetailLabel("Ngày sinh:", true), 0, 2);
        personalGrid.add(createDetailLabel(studentInfo.getBirthDate(), false), 1, 2);

        personalGrid.add(createDetailLabel("Phụ huynh:", true), 0, 3);
        personalGrid.add(createDetailLabel(studentInfo.getParent(), false), 1, 3);

        personalInfoSection.getChildren().addAll(personalTitle, createVerticalSpacer(10), personalGrid);

        // --- Phần 2: Thông tin liên hệ ---
        VBox contactInfoSection = new VBox(10);
        contactInfoSection.setPadding(new Insets(15));
        contactInfoSection.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        Label contactTitle = new Label("📞 Thông tin liên hệ");
        contactTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        contactTitle.setTextFill(Color.web("#0056b3"));

        GridPane contactGrid = new GridPane();
        contactGrid.setHgap(10);
        contactGrid.setVgap(8);

        contactGrid.add(createDetailLabel("Điện thoại:", true), 0, 0);
        HBox phoneBox = new HBox(5, createDetailLabel(studentInfo.getPhone(), false), createCopyButton(studentInfo.getPhone(), "Sao chép SĐT"));
        phoneBox.setAlignment(Pos.CENTER_LEFT);
        contactGrid.add(phoneBox, 1, 0);

        contactGrid.add(createDetailLabel("Email:", true), 0, 1);
        HBox emailBox = new HBox(5, createDetailLabel(studentInfo.getEmail(), false), createCopyButton(studentInfo.getEmail(), "Sao chép Email"));
        emailBox.setAlignment(Pos.CENTER_LEFT);
        contactGrid.add(emailBox, 1, 1);

        contactInfoSection.getChildren().addAll(contactTitle, createVerticalSpacer(10), contactGrid);

        // --- Phần 3: Thông tin học tập ---
        VBox academicInfoSection = new VBox(10);
        academicInfoSection.setPadding(new Insets(15));
        academicInfoSection.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #dee2e6;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        Label academicTitle = new Label("🎓 Thông tin học tập");
        academicTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        academicTitle.setTextFill(Color.web("#0056b3"));

        GridPane academicGrid = new GridPane();
        academicGrid.setHgap(10);
        academicGrid.setVgap(8);

        academicGrid.add(createDetailLabel("Lớp học:", true), 0, 0);
        academicGrid.add(createDetailLabel(studentInfo.getClassName(), false), 1, 0);

        academicGrid.add(createDetailLabel("Trạng thái:", true), 0, 1);
        Label statusValueLabel = createDetailLabel(studentInfo.getStatus(), false);
        // Tùy chỉnh màu sắc cho trạng thái (giống như trong bảng)
        String statusBgColor;
        String statusTextColor;
        switch (studentInfo.getStatus()) {
            case "Đang học":
                statusBgColor = GREEN_COLOR + "40"; // Thêm alpha cho màu nền
                statusTextColor = GREEN_COLOR;
                break;
            case "Nghỉ học":
                statusBgColor = RED_COLOR + "40";
                statusTextColor = RED_COLOR;
                break;
            case "Mới":
                statusBgColor = YELLOW_COLOR + "40";
                statusTextColor = "#856404"; // Màu chữ đậm hơn cho nền vàng
                break;
            default:
                statusBgColor = LIGHT_GRAY + "40";
                statusTextColor = TEXT_COLOR;
        }
        statusValueLabel.setStyle(
                "-fx-background-color: " + statusBgColor + ";" +
                        "-fx-text-fill: " + statusTextColor + ";" +
                        "-fx-padding: 3px 8px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-font-weight: bold;"
        );
        statusValueLabel.setMaxWidth(Double.MAX_VALUE);
        HBox statusBox = new HBox(statusValueLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        academicGrid.add(statusBox, 1, 1);

        academicInfoSection.getChildren().addAll(academicTitle, createVerticalSpacer(10), academicGrid);

        // Thêm các phần vào nội dung chính
        mainContent.getChildren().addAll(personalInfoSection, contactInfoSection, academicInfoSection);

        // Thiết lập nội dung cho DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(mainContent);
        dialogPane.setPrefWidth(550); // Điều chỉnh chiều rộng dialog
        dialogPane.setPrefHeight(Region.USE_COMPUTED_SIZE); // Tự động tính chiều cao

        // Tùy chỉnh nút (nếu muốn)
        alert.getButtonTypes().setAll(new ButtonType("Đóng", ButtonBar.ButtonData.OK_DONE));

        alert.showAndWait();
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
        private final SimpleStringProperty parent;
        private final SimpleStringProperty status;
        private final SimpleStringProperty email;
        private final SimpleStringProperty userId;
        private final int stt;
        private boolean selected;

        public StudentInfo(int stt, String name, String birthDate, String className, String phone,
                           String parent, String status, String email, String userId) {
            this.stt = stt;
            this.name = new SimpleStringProperty(name);
            this.birthDate = new SimpleStringProperty(birthDate);
            this.className = new SimpleStringProperty(className);
            this.phone = new SimpleStringProperty(phone);
            this.parent = new SimpleStringProperty(parent);
            this.status = new SimpleStringProperty(status);
            this.email = new SimpleStringProperty(email);
            this.userId = new SimpleStringProperty(userId);
            this.selected = false;
        }

        public int getStt() {
            return stt;
        }

        public String getName() {
            return name.get();
        }

        public String getBirthDate() {
            return birthDate.get();
        }

        public String getClassName() {
            return className.get();
        }

        public String getPhone() {
            return phone.get();
        }

        public String getParent() {
            return parent.get();
        }

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String status) {
            this.status.set(status);
        }


        public String getEmail() {
            return email.get();
        }

        public String getUserId() {
            return userId.get();
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }


}
