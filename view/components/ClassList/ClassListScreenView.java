package view.components.ClassList;
import src.controller.MainController;
import src.dao.ClassroomDAO;
import src.dao.CourseDAO;
import src.dao.StudentDAO;
import src.dao.TeacherDAO;
import src.model.person.Person; // Import lớp Person (đảm bảo đường dẫn đúng với project của bạn)
import src.model.person.Role; // Import enum Role (đảm bảo đường dẫn đúng)
import src.model.person.Permission; // Import enum Permission (đảm bảo đường dẫn đúng)
import src.model.person.RolePermissions; // Import lớp RolePermissions (đảm bảo đường dẫn đúng)
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
import src.model.system.course.Course;
import view.components.ClassList.CreateClassScreenView;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private Label totalClassesCountLabel;
    private Button searchButton;
    private Button exportExcelButton;
    private Button createClassButton; // Khai báo nút tạo lớp học ở cấp độ lớp
    private ComboBox<String> pageSizeComboBox;
    private ComboBox<String> filterComboBox;
    private TableView<ClassInfo> classesTable;
    private CourseDAO courseDAO;
    private MainController mainController; // Để tham chiếu đến MainController
    private ClassroomDAO classroomDAO;
    private ObservableList<ClassInfo> classes = FXCollections.observableArrayList();

    // Data
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ClassListScreenView() {
        super("Lớp học", "classes");

        // KHỞI TẠO CourseDAO và các dependency của nó
        StudentDAO studentDAO = new StudentDAO();
        TeacherDAO teacherDAO = new TeacherDAO();

        this.courseDAO = new CourseDAO();
        this.courseDAO.setStudentDAO(studentDAO); // Quan trọng: inject dependency
        this.courseDAO.setTeacherDAO(teacherDAO); // Quan trọng: inject dependency
        // Nếu CourseDAO không có dependency, new CourseDAO() là đủ.
        initializeView();
        initializeData(); // Gọi sau khi courseDAO đã sẵn sàng
    }



    // Thay thế phương thức initializeData
    private void initializeData() {
        // Thay vì: classes = FXCollections.observableArrayList();
        // Hãy dùng:
        this.classes.clear(); // Xóa tất cả các mục khỏi danh sách hiện tại

        try {
            List<Course> coursesFromDb = courseDAO.findAll();
            if (coursesFromDb == null || coursesFromDb.isEmpty()) {
                System.err.println("Không có khóa học nào được truy xuất từ cơ sở dữ liệu.");
            } else {
                System.out.println("Đã tải các khóa học: " + coursesFromDb.size());
            }

            int stt = 1;
            if (coursesFromDb != null) {
                for (Course course : coursesFromDb) {
                    addCourseToTableView(course, stt++); // Phương thức này sẽ gọi this.classes.add()
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // showInfo("Lỗi khi tải dữ liệu lớp học từ cơ sở dữ liệu: " + e.getMessage());
        }

        // Cập nhật label tổng số lớp
        if (this.totalClassesCountLabel != null && this.classes != null) {
            this.totalClassesCountLabel.setText(String.valueOf(this.classes.size()));
        }
        // TableView sẽ tự động cập nhật vì nó đang "quan sát" cùng một đối tượng ObservableList classes
        // mà bạn vừa clear() và add() vào.
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

        // Nút tạo lớp học mới
        createClassButton = new Button();
        createClassButton.setStyle(
                "-fx-background-color: " + GREEN_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );

        Label plusIcon = new Label("➕");
        plusIcon.setTextFill(Color.WHITE);
        HBox createContent = new HBox(7);
        createContent.setAlignment(Pos.CENTER);
        createContent.getChildren().addAll(plusIcon, new Label("Tạo lớp học"));
        createClassButton.setGraphic(createContent);
        createClassButton.setOnAction(e -> showCreateClassDialog());
        Person currentUser = getCurrentUser();

// Kiểm tra quyền CREATE_CLASS
        boolean canCreateClass = false;
        if (currentUser != null && currentUser.getRole() != null) {
            canCreateClass = RolePermissions.hasPermission(currentUser.getRole(), Permission.CREATE_CLASS);
        } else {
            canCreateClass = false; // Không cho phép nếu user hoặc role không hợp lệ
        }

// Ẩn hoặc hiện nút dựa trên quyền
        createClassButton.setVisible(canCreateClass);
        createClassButton.setManaged(canCreateClass); // Quan trọng để không chiếm không gian khi ẩn
// ------------------------------------


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

        titleBar.getChildren().addAll(titleLabel, spacer, createClassButton, exportExcelButton);
        return titleBar;
    }
    // src/view/components/ClassList/ClassListScreenView.java
    private VBox createSingleStatCard(String title, String color, String initialValue,
                                      String valueLabelText, String iconString) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
        );

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(iconString);
        iconLabel.setTextFill(Color.WHITE);
        iconLabel.setFont(Font.font("System", 18)); // Sửa ở đây

        Label titleTextLabel = new Label(title);
        titleTextLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); // Giữ nguyên FontWeight.BOLD nếu muốn đậm
        titleTextLabel.setTextFill(Color.WHITE);

        titleBox.getChildren().addAll(iconLabel, titleTextLabel);

        VBox statValueBox = new VBox(2);
        statValueBox.setAlignment(Pos.CENTER_LEFT);
        statValueBox.setPadding(new Insets(8, 0, 0, 0));

        this.totalClassesCountLabel = new Label(initialValue);
        this.totalClassesCountLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        this.totalClassesCountLabel.setTextFill(Color.WHITE);

        Label descriptionLabel = new Label(valueLabelText);
        descriptionLabel.setFont(Font.font("System", 12)); // Sửa ở đây
        descriptionLabel.setTextFill(Color.WHITE);

        statValueBox.getChildren().addAll(this.totalClassesCountLabel, descriptionLabel);

        card.getChildren().addAll(titleBox, statValueBox);
        return card;
    }

    /**
     Create statistics cards for the top of the screen
     */
    // src/view/components/ClassList/ClassListScreenView.java
    /**
     * Tạo phần thống kê - giờ chỉ có 1 card tổng số lớp học
     */
    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(0); // HBox chứa các thẻ thống kê
        statsContainer.setPadding(new Insets(0, 0, 15, 0));
        // Căn chỉnh cho HBox, ví dụ: căn trái hoặc căn giữa nếu chỉ có 1 card
        statsContainer.setAlignment(Pos.CENTER_LEFT); // Hoặc Pos.CENTER
        VBox totalClassesCard = createSingleStatCard(
                "Tổng số Lớp học",   // Tiêu đề
                PRIMARY_COLOR,        // Màu sắc (ví dụ: màu Indigo)
                "0",                  // Giá trị ban đầu (sẽ được cập nhật)
                "Lớp đang hoạt động", // Nhãn mô tả dưới con số
                "📚"                  // Icon
        );

        // Đặt kích thước mong muốn cho card
        totalClassesCard.setPrefWidth(280); // Điều chỉnh chiều rộng nếu cần
        totalClassesCard.setMaxWidth(Region.USE_PREF_SIZE); // Để card không bị kéo giãn quá mức

        statsContainer.getChildren().add(totalClassesCard);
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
                    VBox content = new VBox(3);
                    content.setAlignment(Pos.CENTER_LEFT);
                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-font-weight: bold;");
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
                    String[] parts = item.split("/");
                    int current = Integer.parseInt(parts[0]);
                    int total = Integer.parseInt(parts[1]);
                    ProgressBar progressBar = new ProgressBar((double)current/total);
                    progressBar.setPrefWidth(60);
                    progressBar.setStyle("-fx-accent: " + GREEN_COLOR + ";");
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
        TableColumn<ClassInfo, String> dayOfWeekColumn = new TableColumn<>("Days");
        dayOfWeekColumn.setCellValueFactory(new PropertyValueFactory<>("classDate"));
        classesTable.getColumns().add(dayOfWeekColumn);
        dayOfWeekColumn.setPrefWidth(100);

        // Giáo viên column
        TableColumn<ClassInfo, String> teacherCol = new TableColumn<>("Giáo viên");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(120);
        teacherCol.setCellFactory(col -> new TableCell<ClassInfo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Button assignTeacherButton = new Button(item.isEmpty() ? "Thêm GV" : item);
                    assignTeacherButton.setStyle(item.isEmpty()
                            ? "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;"
                            : "-fx-background-color: #3f51b5; -fx-text-fill: white; -fx-font-weight: bold;");
                    assignTeacherButton.setMinWidth(100);

                    assignTeacherButton.setOnAction(event -> {
                        ClassInfo selectedClassInfo = getTableRow().getItem();

                        // Gọi CourseDAO và TeacherDAO (đảm bảo được khởi tạo sẵn)
                        Course course = courseDAO.findById(selectedClassInfo.getCode()).orElse(null);
                        if (course == null) {
                            showInfo("Không thể tìm thấy lớp học.");
                            return;
                        }
                        TeacherDAO teacherDAO = courseDAO.getTeacherDAO(); // Đảm bảo TeacherDAO đã được inject.

                        // Gọi AddTeacherIntoCourse Dialog
                        AddTeacherIntoCourse dialog = new AddTeacherIntoCourse(course, courseDAO, teacherDAO);
                        dialog.show();

                        // Làm mới dữ liệu sau thay đổi
                        loadClasses(); // Hàm này phải gọi lại backend để refresh bảng
                    });

                    setGraphic(assignTeacherButton);
                    setText(null);
                }
            }
        });
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
                    btn.setStyle(
                            "-fx-background-color: " + PRIMARY_COLOR + ";" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 30;" +
                                    "-fx-padding: 5 15;" +
                                    "-fx-cursor: hand;"
                    );
                    HBox content = new HBox(5);
                    content.setAlignment(Pos.CENTER);
                    Label label = new Label("Học viên");
                    label.setTextFill(Color.WHITE);
                    Label arrow = new Label("→");
                    arrow.setTextFill(Color.WHITE);
                    content.getChildren().addAll(label, arrow);
                    btn.setGraphic(content);
                    btn.setOnAction(event -> {
                        ClassInfo selectedClassInfo = getTableView().getItems().get(getIndex());
                        if (selectedClassInfo != null) {
                            AddStudentToCourseDialog dialog = new AddStudentToCourseDialog(selectedClassInfo);

                            // Hiển thị dialog và chờ kết quả trả về
                            dialog.showAndWait().ifPresent(result -> {
                                System.out.println("Thêm học viên thành công: " + result.getKey() + " - " + result.getValue());
                                // Làm mới danh sách lớp học sau khi thêm thành công
                                classesTable.refresh();
                            });
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

        // *** BEGIN MODIFICATION: Add Details Button Column ***
        TableColumn<ClassInfo, Void> detailsDialogCol = new TableColumn<>("Chi Tiết");
        detailsDialogCol.setPrefWidth(100); // Điều chỉnh độ rộng nếu cần
        detailsDialogCol.setCellFactory(param -> new TableCell<ClassInfo, Void>() {
            private final Button btnDetails = new Button("Xem");
            {
                btnDetails.setStyle(
                        "-fx-background-color: " + PURPLE_COLOR + ";" + // Hoặc màu khác tùy ý
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 4;" +
                                "-fx-padding: 5 10;" +
                                "-fx-cursor: hand;"
                );
                btnDetails.setOnAction(event -> {
                    // 1. Lấy đối tượng ClassInfo của dòng được chọn
                    ClassInfo selectedClassInfo = getTableView().getItems().get(getIndex());
                    if (selectedClassInfo != null) {
                        // 2. Lấy đối tượng Course đầy đủ từ courseDAO bằng courseId
                        //    selectedClassInfo.getCode() chính là courseId
                        Optional<Course> optionalCourse = courseDAO.findById(selectedClassInfo.getCode());
                        if (optionalCourse.isPresent()) {
                            Course course = optionalCourse.get(); // Đây là đối tượng Course bạn cần
                            // 3. Lấy đối tượng TeacherDAO từ courseDAO
                            //    (Giả sử courseDAO có phương thức getTeacherDAO())
                            TeacherDAO teacherDAOInstance = courseDAO.getTeacherDAO();
                            if (teacherDAOInstance == null) {
                                // Xử lý trường hợp TeacherDAO chưa được khởi tạo trong CourseDAO
                                // Ví dụ: Khởi tạo nó ở đây hoặc báo lỗi
                                System.err.println("Lỗi: TeacherDAO chưa được thiết lập trong CourseDAO.");
                                // Có thể hiển thị thông báo lỗi cho người dùng
                                showInfo("Lỗi: Không thể tải thông tin giáo viên. Vui lòng liên hệ quản trị viên.");
                                return;
                            }
                            // 4. Gọi constructor của ClassDetailsDialog và truyền Course, TeacherDAO vào
                            ClassDetailsDialog detailsDialog = new ClassDetailsDialog(course, teacherDAOInstance);
                            detailsDialog.show();
                        } else {
                            // Xử lý trường hợp không tìm thấy Course với ID tương ứng
                            showInfo("Không tìm thấy thông tin chi tiết cho lớp học này.");
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDetails);
                }
            }
        });
        // *** END MODIFICATION ***

        // Add all columns to the table
        classesTable.getColumns().addAll(
                selectCol, sttCol, codeCol, nameCol, sessionsCol,
                startDateCol, endDateCol, teacherCol,
                classDateCol, studentsCol,
                detailsDialogCol, // *** MODIFICATION: Added new column here ***
                actionsCol
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
        if (createClassButton.isVisible()) {
            createClassButton.setOnAction(e -> showCreateClassDialog());
        }
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
    private void loadClasses() {
        initializeData(); // Gọi lại initializeData để tải lại toàn bộ dữ liệu
        // Hoặc nếu bạn chỉ muốn cập nhật TableView mà không clear hoàn toàn:
        // classesTable.refresh(); // Dòng này thường dùng nếu ObservableList tự động cập nhật
        // nhưng để đảm bảo dữ liệu mới nhất từ DB, initializeData an toàn hơn.
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

        deleteItem.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText("Xóa lớp học");
            confirmAlert.setContentText("Bạn có chắc chắn muốn xóa lớp " + classInfo.getName() + "?");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                showInfo("Đã xóa lớp: " + classInfo.getName());
            }
        });

        detailsItem.setOnAction(e -> showInfo("Xem chi tiết lớp: " + classInfo.getName()));

        contextMenu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), detailsItem);

        Button source = (Button) ((HBox) classesTable.getScene().getFocusOwner()).getChildren().get(0);
        contextMenu.show(source, javafx.geometry.Side.BOTTOM, 0, 0);
    }
    private void addCourseToTableView(Course course) {
        addCourseToTableView(course, classes.size() + 1);
    }

    // Thêm phương thức addCourseToTableView với tham số stt
    // Trong ClassListScreenView.java
    private void addCourseToTableView(Course course, int stt) {
        String progress = "0/100"; // Mặc định
        if (course.getProgress() > 0) {
            progress = Math.round(course.getProgress()) + "/100";
        }

        // Sử dụng dateFormatter của lớp và kiểm tra null
        String startDateStr = "N/A"; // Giá trị mặc định nếu null
        if (course.getStartDate() != null) { // course.getDate().getStartDate() tương đương course.getStartDate()
            startDateStr = course.getStartDate().format(this.dateFormatter); //Sử dụng this.dateFormatter
        }

        String endDateStr = "N/A"; // Giá trị mặc định nếu null
        if (course.getEndDate() != null) { // course.getDate().getEndDate() tương đương course.getEndDate()
            endDateStr = course.getEndDate().format(this.dateFormatter); // Sử dụng this.dateFormatter
        }

        String classDate = course.getDayOfWeek() != null ? course.getDayOfWeek() : "N/A";
        String teacher = course.getTeacher() != null ? course.getTeacher().toString() : "Chưa phân công"; // Kiểm tra null cho teacher là tốt

        ClassInfo classInfo = new ClassInfo(
                stt,
                course.getCourseId(),
                course.getCourseName(),
                "Đang học", // Trạng thái hiện đang được đặt cố định
                progress,
                startDateStr, // Sử dụng chuỗi đã kiểm tra null
                endDateStr,   // Sử dụng chuỗi đã kiểm tra null
                teacher,
                classDate
        );

        classes.add(classInfo);
        // classesTable.refresh(); // <<--- XÓA DÒNG NÀY
    }
    // Thêm phương thức loadCoursesFromFile (không phải static)
    @SuppressWarnings("unchecked")
    // Thêm phương thức showCreateClassDialog
    private void showCreateClassDialog() {
        Person currentUser = getCurrentUser(); // Giả sử bạn có phương thức này
        boolean canCreateClass = false;
        if (currentUser != null && currentUser.getRole() != null) {
            canCreateClass = RolePermissions.hasPermission(currentUser.getRole(), Permission.CREATE_CLASS);
        }
        if (!canCreateClass) {
            showInfo("Bạn không có quyền tạo lớp học mới.");
            return;
        }
        // Đảm bảo courseDAO đã được khởi tạo và cấu hình đúng
        // Nếu courseDAO chưa được khởi tạo ở constructor hoặc initializeData,
        // bạn CẦN phải khởi tạo và cấu hình nó ở đây trước khi truyền đi.
        // Ví dụ (NẾU CHƯA LÀM Ở CONSTRUCTOR):
        if (this.classroomDAO == null) {
            this.classroomDAO = new ClassroomDAO(); // Ensure ClassroomDAO is initialized
        }
    if (this.courseDAO == null) {
        StudentDAO studentDAO = new StudentDAO(); // Cần khởi tạo thực tế
        TeacherDAO teacherDAO = new TeacherDAO(); // Cần khởi tạo thực tế
        this.courseDAO = new CourseDAO();
        this.courseDAO.setStudentDAO(studentDAO);
        this.courseDAO.setTeacherDAO(teacherDAO);
        return;
    }

        // Truyền this.courseDAO vào constructor của CreateClassScreenView
        CreateClassScreenView createClassScreen = new CreateClassScreenView(
                this.courseDAO,    // Pass existing CourseDAO
                this.classroomDAO, // Pass existing ClassroomDAO
                new CreateClassScreenView.CreateClassCallback() {
                    @Override
                    public void onCourseCreated(Course successfullySavedCourse) {
                        // Add the newly created course to the table view
                        addCourseToTableView(successfullySavedCourse, classes.size() + 1);
                        showInfo("Đã tạo và lưu lớp học thành công: " + successfullySavedCourse.getCourseName());
                    }
                }
        );
        createClassScreen.show();
    }

    @Override
    public void refreshView() {
        System.out.println(getViewId() + " đang làm mới dữ liệu từ cơ sở dữ liệu..."); // Thêm log để theo dõi
        initializeData(); // Gọi initializeData để tải lại và làm mới toàn bộ
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
