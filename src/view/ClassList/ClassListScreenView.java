package src.view.ClassList;

import src.controller.MainController;
import src.dao.Classrooms.ClassroomDAO;
import src.dao.Person.CourseDAO;
import src.dao.Person.StudentDAO;
import src.dao.Person.TeacherDAO;
import src.dao.ClassSession.ClassSessionDAO; // Đảm bảo đường dẫn này đúng
import src.model.person.Person;
import src.model.person.Role;
import src.model.person.Permission;
import src.model.person.RolePermissions;
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
import javafx.util.Pair; // *** THÊM IMPORT CHO Pair ***
import src.view.components.Screen.BaseScreenView;
import src.model.system.course.Course;
import src.view.ClassList.CreateClassScreenView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ClassListScreenView extends BaseScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String GREEN_COLOR = "#4CAF50";
    private static final String YELLOW_COLOR = "#FFC107"; // *** THÊM LẠI YELLOW_COLOR ***
    private static final String PURPLE_COLOR = "#673AB7";
    private static final String WHITE_COLOR = "#FFFFFF";
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String TEXT_COLOR = "#424242";

    // UI Components
    private Label titleLabel;
    // private HBox statisticsContainer; // Bỏ nếu createStatisticsSection không gán cho nó nữa
    private TextField searchField;
    private Label totalClassesCountLabel; // Sẽ được cập nhật bởi createSingleStatCard
    private Button searchButton;
    private Button exportExcelButton;
    private Button createClassButton;
    private ComboBox<String> pageSizeComboBox;
    private ComboBox<String> filterComboBox;
    private TableView<ClassInfo> classesTable;

    // DAOs
    private CourseDAO courseDAO;
    private ClassroomDAO classroomDAO;
    private ClassSessionDAO classSessionDAO;

    // private MainController mainController; // Giữ lại nếu dùng

    private final ObservableList<ClassInfo> classes = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ClassListScreenView() {
        super("Lớp học", "classes");

        StudentDAO studentDAO = new StudentDAO();
        TeacherDAO teacherDAO = new TeacherDAO();

        this.courseDAO = new CourseDAO();
        this.courseDAO.setStudentDAO(studentDAO);
        this.courseDAO.setTeacherDAO(teacherDAO);

        this.classSessionDAO = new ClassSessionDAO();
        this.classroomDAO = new ClassroomDAO(); // Đảm bảo ClassroomDAO được khởi tạo

        initializeView();
        initializeData();
    }

    private void initializeData() {
        this.classes.clear();
        try {
            List<Course> coursesFromDb = courseDAO.findAll();
            if (coursesFromDb == null || coursesFromDb.isEmpty()) {
                System.err.println("Không có khóa học nào được truy xuất từ cơ sở dữ liệu.");
            } else {
                System.out.println("Đã tải các khóa học: " + coursesFromDb.size());
                int stt = 1;
                for (Course course : coursesFromDb) {
                    addCourseToTableView(course, stt++);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Lỗi khi tải dữ liệu lớp học: " + e.getMessage());
        }

        if (this.totalClassesCountLabel != null) {
            this.totalClassesCountLabel.setText(String.valueOf(this.classes.size()));
        }
    }

    @Override
    public void initializeView() {
        root.getChildren().clear();
        root.setSpacing(0);
        root.setPadding(Insets.EMPTY);
        root.setStyle("-fx-background-color: " + WHITE_COLOR + ";");

        BorderPane mainLayout = new BorderPane();
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(15));

        HBox titleBar = createTitleBar();
        HBox statisticsContainer = createStatisticsSection(); // Gán lại cho biến cục bộ nếu không dùng ở đâu khác
        HBox searchAndFilterBar = createSearchAndFilterBar();
        createClassesTable();

        contentBox.getChildren().addAll(
                titleBar,
                statisticsContainer,
                searchAndFilterBar,
                classesTable
        );
        VBox.setVgrow(classesTable, Priority.ALWAYS);
        mainLayout.setCenter(contentBox);
        root.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        setupEventHandlers();
    }


    private void addCourseToTableView(Course course, int stt) {
        if (course == null) {
            System.err.println("Không thể thêm khóa học null vào bảng.");
            return;
        }

        int currentSessionNum = 0; // *** KHAI BÁO Ở NGOÀI ***
        if (this.classSessionDAO != null && course.getCourseId() != null) {
            try {
                currentSessionNum = this.classSessionDAO.findCurrentSessionNumberByDate(course.getCourseId(), LocalDate.now())
                        .orElse(0);
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy số buổi học hiện tại cho khóa " + course.getCourseId() + ": " + e.getMessage());
            }
        } else {
            System.err.println("Cảnh báo: classSessionDAO hoặc courseId là null. Số buổi hiện tại mặc định là 0.");
        }

        int totalSess = 0;
        if (this.courseDAO != null && course.getClassId() != null) {
            try {
                Optional<Integer> totalSessionsOpt = this.courseDAO.findTotalSessionsForClass(course.getClassId());
                if (totalSessionsOpt.isPresent() && totalSessionsOpt.get() > 0) {
                    totalSess = totalSessionsOpt.get();
                } else {
                    System.out.println("Thông tin: total_sessions không tìm thấy cho classId " + course.getClassId() +
                            " (hoặc <=0). Sử dụng course.getTotalSessions() làm fallback cho course: " + course.getCourseName());
                    totalSess = course.getTotalSessions();
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy tổng số buổi cho classId " + course.getClassId() + ": " + e.getMessage() +
                        ". Sử dụng course.getTotalSessions() làm fallback.");
                totalSess = course.getTotalSessions();
            }
        } else {
            System.err.println("Cảnh báo: courseDAO hoặc classId là null cho khóa " + course.getCourseName() +
                    ". Sử dụng course.getTotalSessions() làm fallback.");
            totalSess = course.getTotalSessions();
        }

        if (totalSess <= 0) {
            System.err.println("Cảnh báo: totalSess vẫn <= 0 sau tất cả các bước cho khóa: " + course.getCourseName());
        }

        String displayedProgressString;
        float progressPercentage = 0f;

        if (totalSess > 0) {
            int effectiveCurrentSession = Math.min(currentSessionNum, totalSess);
            progressPercentage = ((float) effectiveCurrentSession / totalSess) * 100f;
            displayedProgressString = currentSessionNum + "/" + totalSess;
        } else {
            displayedProgressString = currentSessionNum + "/0";
        }

        course.setProgress(progressPercentage);
        if (this.courseDAO != null && course.getCourseId() != null) {
            try {
                this.courseDAO.updateProgress(course.getCourseId(), progressPercentage);
            } catch (Exception e) {
                System.err.println("Lỗi khi cập nhật progress vào DB cho khóa " + course.getCourseId() + ": " + e.getMessage());
            }
        } else {
            System.err.println("Cảnh báo: courseDAO hoặc courseId là null. Progress không được lưu vào DB.");
        }

        LocalDate actualStartDate = course.getStartDate();
        LocalDate actualEndDate = course.getEndDate();
        String teacherName = (course.getTeacher() != null && course.getTeacher().getName() != null)
                ? course.getTeacher().getName() : "Chưa phân công";

        String classDateDisplay = course.getDaysOfWeekAsString();
        if (classDateDisplay == null || classDateDisplay.trim().isEmpty()) {
            classDateDisplay = course.getDayOfWeek();
        }
        if (classDateDisplay == null || classDateDisplay.trim().isEmpty()) {
            classDateDisplay = "N/A";
        }

        String courseStatus;
        if (actualStartDate == null) {
            courseStatus = "Chưa có lịch";
        } else if (LocalDate.now().isBefore(actualStartDate)) {
            courseStatus = "Sắp bắt đầu";
        } else if (actualEndDate != null && LocalDate.now().isAfter(actualEndDate)) {
            courseStatus = "Đã kết thúc";
        } else {
            courseStatus = "Đang học";
        }

        ClassInfo classInfo = new ClassInfo(
                stt,
                course.getCourseId(),
                course.getCourseName(),
                courseStatus,
                actualStartDate,
                actualEndDate,
                teacherName,
                classDateDisplay,
                displayedProgressString
        );
        classes.add(classInfo);
    }

    private HBox createTitleBar() {
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");

        titleLabel = new Label("Lớp học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
        HBox createContent = new HBox(7, plusIcon, new Label("Tạo lớp học"));
        createContent.setAlignment(Pos.CENTER);
        createClassButton.setGraphic(createContent);
        createClassButton.setOnAction(e -> showCreateClassDialog());

        Person currentUser = getCurrentUser();
        boolean canCreateClass = currentUser != null && currentUser.getRole() != null &&
                RolePermissions.hasPermission(currentUser.getRole(), Permission.CREATE_CLASS);
        createClassButton.setVisible(canCreateClass);
        createClassButton.setManaged(canCreateClass);

        exportExcelButton = new Button();
        exportExcelButton.setStyle(
                "-fx-background-color: " + PRIMARY_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 2, 0, 0, 1);"
        );
        Label excelIcon = new Label("📊");
        excelIcon.setTextFill(Color.WHITE);
        HBox exportContent = new HBox(7, excelIcon, new Label("Xuất Excel"));
        exportContent.setAlignment(Pos.CENTER);
        exportExcelButton.setGraphic(exportContent);
        exportExcelButton.setOnAction(e -> exportToExcel());

        titleBar.getChildren().addAll(titleLabel, spacer, createClassButton, exportExcelButton);
        return titleBar;
    }

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

        Label iconLabel = new Label(iconString);
        iconLabel.setTextFill(Color.WHITE);
        iconLabel.setFont(Font.font("System", 18));

        Label titleTextLabel = new Label(title);
        titleTextLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleTextLabel.setTextFill(Color.WHITE);
        HBox titleBox = new HBox(10, iconLabel, titleTextLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        this.totalClassesCountLabel = new Label(initialValue);
        this.totalClassesCountLabel.setFont(Font.font("System", FontWeight.BOLD, 32));
        this.totalClassesCountLabel.setTextFill(Color.WHITE);

        Label descriptionLabel = new Label(valueLabelText);
        descriptionLabel.setFont(Font.font("System", 12));
        descriptionLabel.setTextFill(Color.WHITE);

        VBox statValueBox = new VBox(2, this.totalClassesCountLabel, descriptionLabel);
        statValueBox.setAlignment(Pos.CENTER_LEFT);
        statValueBox.setPadding(new Insets(8, 0, 0, 0));

        card.getChildren().addAll(titleBox, statValueBox);
        return card;
    }

    private HBox createStatisticsSection() {
        HBox statsContainer = new HBox(0);
        statsContainer.setPadding(new Insets(0, 0, 15, 0));
        statsContainer.setAlignment(Pos.CENTER_LEFT);
        VBox totalClassesCard = createSingleStatCard(
                "Tổng số Lớp học",
                PRIMARY_COLOR,
                "0",
                "Lớp học", // Đổi label mô tả
                "📚"
        );
        totalClassesCard.setPrefWidth(280);
        totalClassesCard.setMaxWidth(Region.USE_PREF_SIZE);
        statsContainer.getChildren().add(totalClassesCard);
        return statsContainer;
    }

    private HBox createSearchAndFilterBar() {
        HBox searchAndFilterBar = new HBox(15);
        searchAndFilterBar.setPadding(new Insets(15, 0, 15, 0));
        searchAndFilterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Lọc theo:"); // Đổi "Từ khóa" thành "Lọc theo"
        filterLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold;");
        filterComboBox = new ComboBox<>();
        filterComboBox.setPromptText("Tất cả trạng thái"); // Gợi ý rõ hơn
        // filterComboBox.getItems().addAll("Tất cả", "Đang học", "Sắp bắt đầu", "Đã kết thúc"); // Thêm các lựa chọn lọc
        filterComboBox.setPrefWidth(180); // Giảm độ rộng một chút
        filterComboBox.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4;");
        // filterComboBox.setOnAction(e -> filterClassesByStatus()); // Thêm event handler
        HBox filterBox = new HBox(10, filterLabel, filterComboBox);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label pageSizeLabel = new Label("Hiển thị:"); // Đổi "Cỡ trang"
        pageSizeLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        pageSizeComboBox = new ComboBox<>();
        pageSizeComboBox.getItems().addAll("10", "20", "50", "100", "Tất cả");
        pageSizeComboBox.setValue("20");
        pageSizeComboBox.setPrefWidth(90); // Tăng độ rộng
        pageSizeComboBox.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4;");
        HBox pageSizeBox = new HBox(10, pageSizeLabel, pageSizeComboBox);
        pageSizeBox.setAlignment(Pos.CENTER);

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm tên, mã lớp...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4 0 0 4; -fx-padding: 7;");

        searchButton = new Button();
        searchButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 0 4 4 0; -fx-cursor: hand; -fx-padding: 7 15;");
        Label searchIcon = new Label("🔍");
        searchIcon.setTextFill(Color.WHITE);
        searchButton.setGraphic(searchIcon);
        HBox searchBox = new HBox(0, searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER);

        HBox rightSideBox = new HBox(15, pageSizeBox, searchBox);
        rightSideBox.setAlignment(Pos.CENTER_RIGHT);

        searchAndFilterBar.getChildren().addAll(filterBox, spacer, rightSideBox);
        return searchAndFilterBar;
    }

    private void createClassesTable() {
        classesTable = new TableView<>(classes);
        classesTable.setEditable(false);
        classesTable.setPrefHeight(600);
        classesTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-width: 1;"
        );
        classesTable.setTableMenuButtonVisible(false);

        TableColumn<ClassInfo, Void> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40);
        selectCol.setResizable(false);
        selectCol.setCellFactory(col -> {
            TableCell<ClassInfo, Void> cell = new TableCell<>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    HBox hbox = new HBox(checkBox);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                    checkBox.setOnAction(event -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            ClassInfo data = getTableView().getItems().get(getIndex());
                            data.setSelected(checkBox.isSelected());
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        checkBox.setSelected(data.isSelected());
                        setGraphic(getGraphic()); // Giữ lại HBox chứa CheckBox
                    }
                }
            };
            return cell;
        });

        TableColumn<ClassInfo, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setPrefWidth(50);
        sttCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> codeCol = new TableColumn<>("Mã Lớp");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(120);

        TableColumn<ClassInfo, String> nameCol = new TableColumn<>("Tên Lớp");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        nameCol.setCellFactory(column -> new TableCell<>() {
            private final Label nameLabel = new Label();
            private final Label statusLabelText = new Label(); // Đổi tên để tránh nhầm lẫn
            private final VBox content = new VBox(3, nameLabel, statusLabelText);
            {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
                statusLabelText.setPadding(new Insets(2, 8, 2, 8));
                statusLabelText.setStyle("-fx-background-radius: 4; -fx-font-size: 11px;");
                content.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setGraphic(null);
                } else {
                    ClassInfo classInfo = getTableRow().getItem();
                    nameLabel.setText(item);
                    statusLabelText.setText(classInfo.getStatus());
                    String statusColor = GREEN_COLOR;
                    if ("Đã kết thúc".equalsIgnoreCase(classInfo.getStatus())) statusColor = "#757575";
                    else if ("Sắp bắt đầu".equalsIgnoreCase(classInfo.getStatus())) statusColor = YELLOW_COLOR;
                    else if ("Chưa có lịch".equalsIgnoreCase(classInfo.getStatus())) statusColor = "#BDBDBD";
                    statusLabelText.setStyle(
                            "-fx-background-color: " + statusColor + "20;" +
                                    "-fx-text-fill: " + statusColor + ";" +
                                    "-fx-background-radius: 4; -fx-font-size: 11px;"
                    );
                    setGraphic(content); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, String> progressCol = new TableColumn<>("Tiến độ");
        progressCol.setCellValueFactory(new PropertyValueFactory<>("displayedProgress"));
        progressCol.setPrefWidth(120);
        progressCol.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label progressLabelText = new Label(); // Đổi tên
            private final HBox progressBox = new HBox(5, progressBar, progressLabelText);
            {
                progressBar.setPrefWidth(60); progressBar.setStyle("-fx-accent: " + GREEN_COLOR + ";");
                progressLabelText.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-padding: 0 0 0 3;");
                progressBox.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("N/A") || !item.contains("/")) {
                    setText(item); setGraphic(null);
                } else {
                    try {
                        String[] parts = item.split("/");
                        long current = Long.parseLong(parts[0].trim());
                        long total = Long.parseLong(parts[1].trim());
                        progressBar.setProgress(total == 0 ? 0 : (double) current / total);
                        progressLabelText.setText(item);
                        setGraphic(progressBox); setText(null);
                    } catch (Exception e) {
                        setText(item); setGraphic(null);
                        System.err.println("Lỗi parse chuỗi tiến độ cho TableCell: '" + item + "' - " + e.getMessage());
                    }
                }
            }
        });

        TableColumn<ClassInfo, String> startDateCol = new TableColumn<>("Bắt đầu");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startDateCol.setPrefWidth(90); startDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> endDateCol = new TableColumn<>("Kết thúc");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endDateCol.setPrefWidth(90); endDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> teacherCol = new TableColumn<>("Giáo viên");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(150);
        teacherCol.setCellFactory(col -> new TableCell<ClassInfo, String>() {
            private final Button assignTeacherButton = new Button();
            {
                assignTeacherButton.setMinWidth(120);
                assignTeacherButton.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ClassInfo selectedClassInfo = getTableView().getItems().get(getIndex());
                        Person currentUser = getCurrentUser();
                        boolean canAddTeacher = currentUser != null && currentUser.getRole() != null &&
                                RolePermissions.hasPermission(currentUser.getRole(), Permission.ADDTEACHER_INTOCOURSE);
                        if (!canAddTeacher) {
                            showInfo("Bạn không có quyền thực hiện thao tác này."); return;
                        }
                        Optional<Course> courseOpt = courseDAO.findById(selectedClassInfo.getCode());
                        if (courseOpt.isEmpty()) {
                            showInfo("Không thể tìm thấy lớp học."); return;
                        }
                        Course course = courseOpt.get();
                        TeacherDAO teacherDAOInstance = courseDAO.getTeacherDAO();
                        if (teacherDAOInstance == null) {
                            System.err.println("Lỗi: TeacherDAO chưa được thiết lập trong CourseDAO.");
                            showInfo("Lỗi: Không thể tải thông tin giáo viên."); return;
                        }
                        AddTeacherIntoCourse dialog = new AddTeacherIntoCourse(course, courseDAO, teacherDAOInstance);
                        // *** SỬA LẠI dialog.show() NẾU AddTeacherIntoCourse KHÔNG PHẢI LÀ STAGE/DIALOG CHẶN ***
                        dialog.show(); // Hoặc showAndWait() nếu nó là Stage/Dialog và bạn muốn chặn
                        // Cân nhắc việc loadClasses() ở đây, hoặc nếu dialog có callback
                        // loadClasses(); // Tải lại sau khi dialog đóng (có thể cần cơ chế callback tốt hơn)
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null); setGraphic(null);
                } else {
                    assignTeacherButton.setText(item.isEmpty() || "Chưa phân công".equalsIgnoreCase(item) || "Chưa có".equalsIgnoreCase(item) ? "Thêm GV" : item);
                    assignTeacherButton.setStyle( (item.isEmpty() || "Chưa phân công".equalsIgnoreCase(item) || "Chưa có".equalsIgnoreCase(item))
                            ? "-fx-background-color: " + GREEN_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;"
                            : "-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4;");
                    setGraphic(assignTeacherButton); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, String> classDateCol = new TableColumn<>("Ngày học");
        classDateCol.setCellValueFactory(new PropertyValueFactory<>("classDate"));
        classDateCol.setPrefWidth(120);
        classDateCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "N/A".equals(item)) {
                    setText(item); setGraphic(null);
                } else {
                    String[] days = item.split(","); VBox daysBox = new VBox(1);
                    daysBox.setAlignment(Pos.CENTER_LEFT);
                    for (String day : days) {
                        if (!day.trim().isEmpty()) {
                            Label dayLabel = new Label(day.trim());
                            dayLabel.setStyle("-fx-font-size: 11px;");
                            daysBox.getChildren().add(dayLabel);
                        }
                    }
                    setGraphic(daysBox); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, Void> studentsCol = new TableColumn<>("Học viên");
        studentsCol.setPrefWidth(100);
        studentsCol.setCellFactory(col -> new TableCell<ClassInfo, Void>() {
            private final Button btn = new Button("DS HV");
            {
                btn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; -fx-cursor: hand; -fx-font-size: 11px;");
                btn.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ClassInfo selectedClassInfo = getTableView().getItems().get(getIndex());
                        // *** SỬA LẠI CONSTRUCTOR VÀ XỬ LÝ RESULT CỦA AddStudentToCourseDialog ***
                        AddStudentToCourseDialog dialog = new AddStudentToCourseDialog(selectedClassInfo); // Giả sử constructor nhận ClassInfo
                        dialog.showAndWait().ifPresent((Pair<String, String> pairResult) -> { // Xử lý Pair
                            // Chỉ cần ifPresent được gọi là đủ để làm mới
                            System.out.println("Dialog thêm/xóa học viên đã đóng, làm mới bảng...");
                            System.out.println("Kết quả từ dialog: " + pairResult.getKey() + " - " + pairResult.getValue());
                            loadClasses();
                        });
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size() ? null : btn);
            }
        });

        TableColumn<ClassInfo, Void> detailsDialogCol = new TableColumn<>("Chi tiết");
        detailsDialogCol.setPrefWidth(80);
        detailsDialogCol.setCellFactory(param -> new TableCell<ClassInfo, Void>() {
            private final Button btnDetails = new Button("Xem");
            {
                btnDetails.setStyle("-fx-background-color: " + PURPLE_COLOR + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; -fx-cursor: hand; -fx-font-size: 11px;");
                btnDetails.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ClassInfo selectedClassInfo = getTableView().getItems().get(getIndex());
                        Optional<Course> optionalCourse = courseDAO.findById(selectedClassInfo.getCode());
                        if (optionalCourse.isPresent()) {
                            Course course = optionalCourse.get();
                            TeacherDAO teacherDAOInstance = courseDAO.getTeacherDAO();
                            if (teacherDAOInstance == null) {
                                System.err.println("Lỗi: TeacherDAO chưa được thiết lập trong CourseDAO.");
                                showInfo("Lỗi: Không thể tải thông tin giáo viên."); return;
                            }
                            ClassDetailsDialog detailsDialog = new ClassDetailsDialog(course, teacherDAOInstance);
                            detailsDialog.show();
                        } else {
                            showInfo("Không tìm thấy thông tin chi tiết cho lớp học này.");
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size() ? null : btnDetails);
            }
        });

        TableColumn<ClassInfo, Void> actionsCol = new TableColumn<>("Khác");
        actionsCol.setPrefWidth(60);
        actionsCol.setCellFactory(col -> new TableCell<ClassInfo, Void>() {
            private final Button btn = new Button("⚙");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4; -fx-padding: 5 8; -fx-cursor: hand; -fx-font-size: 12px;");
                btn.setOnAction(event -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        ClassInfo data = getTableView().getItems().get(getIndex());
                        handleMoreActions(data, btn);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size() ? null : btn);
            }
        });

        classesTable.getColumns().addAll(
                selectCol, sttCol, codeCol, nameCol, progressCol,
                startDateCol, endDateCol, teacherCol, classDateCol,
                studentsCol, detailsDialogCol, actionsCol
        );

        classesTable.setRowFactory(tv -> {
            TableRow<ClassInfo> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldVal, newVal) -> {
                String baseStyle = "-fx-border-color: " + BORDER_COLOR + " transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;";
                if (newVal) {
                    row.setStyle("-fx-background-color: #e3f2fd; " + baseStyle);
                } else {
                    row.setStyle((row.getIndex() % 2 == 0 ? "-fx-background-color: white; " : "-fx-background-color: #f8f9fa; ") + baseStyle);
                }
            });
            // Set initial style correctly
            String baseStyle = "-fx-border-color: " + BORDER_COLOR + " transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0;";
            row.setStyle((row.getIndex() % 2 == 0 ? "-fx-background-color: white; " : "-fx-background-color: #f8f9fa; ") + baseStyle);
            return row;
        });
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(e -> searchClasses(searchField.getText()));
        searchField.setOnAction(e -> searchClasses(searchField.getText()));
        pageSizeComboBox.setOnAction(e -> updatePageSize());
    }

    private void updatePageSize() {
        String selectedSize = pageSizeComboBox.getValue();
        if (selectedSize != null) {
            showInfo("Cỡ trang (chưa triển khai): " + selectedSize);
        }
    }

    private void loadClasses() {
        initializeData();
    }

    private void exportToExcel() {
        showInfo("Chức năng Xuất Excel đang được phát triển.");
    }

    private void searchClasses(String keyword) {
        // Tạm thời sẽ filter trên client, nếu dữ liệu lớn cần filter ở server
        ObservableList<ClassInfo> allItems = FXCollections.observableArrayList();
        // Tạo một bản sao của danh sách gốc để không làm mất dữ liệu gốc khi filter
        // Hoặc tốt hơn là gọi lại initializeData() nếu muốn tìm kiếm từ DB.
        // Để đơn giản, filter trên danh sách `classes` hiện tại.
        // Cần một danh sách `originalClasses` để lưu trữ toàn bộ dữ liệu ban đầu.
        // Trong initializeData(): this.originalClasses.setAll(this.classes);
        // Sau đó filter từ this.originalClasses và set cho classesTable.setItems(filteredList);

        // Ví dụ đơn giản filter trực tiếp trên `classes` (sẽ mất dữ liệu gốc nếu không backup)
        // Tốt nhất là nên có một list gốc và một list hiển thị (đã filter)
        if (keyword == null || keyword.trim().isEmpty()) {
            // Nếu tìm kiếm từ DB, gọi lại initializeData() hoặc một phương thức tìm kiếm chuyên biệt
            initializeData(); // Tạm thời tải lại toàn bộ
            // classesTable.setItems(classes); // Nếu `classes` là list gốc không đổi
            return;
        }
        String lowerCaseKeyword = keyword.toLowerCase();
        ObservableList<ClassInfo> filteredList = FXCollections.observableArrayList();
        // Cần có danh sách gốc để filter, ở đây tạm filter trên `this.classes`
        // Điều này có nghĩa là nếu filter nhiều lần, nó sẽ filter trên kết quả filter trước đó.
        // Đây là một điểm cần cải thiện bằng cách giữ 1 list gốc.
        for (ClassInfo classInfo : this.classes) { // Nên filter từ 1 list gốc
            if ((classInfo.getName() != null && classInfo.getName().toLowerCase().contains(lowerCaseKeyword)) ||
                    (classInfo.getCode() != null && classInfo.getCode().toLowerCase().contains(lowerCaseKeyword)) ||
                    (classInfo.getTeacher() != null && classInfo.getTeacher().toLowerCase().contains(lowerCaseKeyword))) {
                filteredList.add(classInfo);
            }
        }
        classesTable.setItems(filteredList); // Hiển thị danh sách đã lọc
    }


    private void handleMoreActions(ClassInfo classInfo, Control anchorNode) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Chỉnh sửa");
        MenuItem deleteItem = new MenuItem("Xóa");

        editItem.setOnAction(e -> showInfo("Chỉnh sửa lớp (chưa triển khai): " + classInfo.getName()));
        deleteItem.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText("Xóa lớp học: " + classInfo.getName());
            confirmAlert.setContentText("Bạn có chắc chắn muốn xóa lớp học này?");
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean deleted = courseDAO.delete(classInfo.getCode()); // Giả sử có phương thức delete
                if(deleted){
                    showInfo("Đã xóa lớp: " + classInfo.getName());
                    loadClasses(); // Tải lại danh sách
                } else {
                    showInfo("Không thể xóa lớp: " + classInfo.getName());
                }
            }
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        contextMenu.show(anchorNode, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void showCreateClassDialog() {
        Person currentUser = getCurrentUser();
        boolean canCreateClass = currentUser != null && currentUser.getRole() != null &&
                RolePermissions.hasPermission(currentUser.getRole(), Permission.CREATE_CLASS);
        if (!canCreateClass) {
            showInfo("Bạn không có quyền tạo lớp học mới.");
            return;
        }
        if (this.courseDAO == null || this.classroomDAO == null) {
            System.err.println("Lỗi: CourseDAO hoặc ClassroomDAO chưa được khởi tạo.");
            showInfo("Lỗi hệ thống: Không thể mở form tạo lớp.");
            return;
        }
        CreateClassScreenView createClassScreen = new CreateClassScreenView(
                this.courseDAO,
                this.classroomDAO,
                successfullySavedCourse -> {
                    loadClasses();
                    showInfo("Đã tạo và lưu lớp học thành công: " + successfullySavedCourse.getCourseName());
                }
        );
        createClassScreen.show();
    }

    @Override
    public void refreshView() {
        System.out.println(getViewId() + " đang làm mới dữ liệu từ cơ sở dữ liệu...");
        initializeData();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class ClassInfo {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty status;
        private final SimpleStringProperty startDate;
        private final SimpleStringProperty endDate;
        private final SimpleStringProperty teacher;
        private final SimpleStringProperty manager;
        private final SimpleStringProperty classDate;
        private final SimpleStringProperty displayedProgress;
        private final int stt;
        private boolean selected;
        // private final LocalDate actualStartDate; // Giữ lại nếu còn dùng ở đâu đó
        // private final LocalDate actualEndDate;   // Giữ lại nếu còn dùng ở đâu đó

        public ClassInfo(int stt, String code, String name, String status,
                         LocalDate actualStartDate, LocalDate actualEndDate,
                         String teacherName, String classDateString,
                         String progressDisplayString) {
            this.stt = stt;
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.status = new SimpleStringProperty(status);

            // this.actualStartDate = actualStartDate; // Gán nếu cần giữ
            // this.actualEndDate = actualEndDate;     // Gán nếu cần giữ

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            this.startDate = new SimpleStringProperty(actualStartDate != null ? actualStartDate.format(formatter) : "N/A");
            this.endDate = new SimpleStringProperty(actualEndDate != null ? actualEndDate.format(formatter) : "N/A");

            this.teacher = new SimpleStringProperty(teacherName != null && !teacherName.trim().isEmpty() ? teacherName : "Chưa có");
            this.manager = new SimpleStringProperty(teacherName != null && !teacherName.trim().isEmpty() ? teacherName : "Chưa có");
            this.classDate = new SimpleStringProperty(classDateString != null ? classDateString : "N/A");
            this.selected = false;
            this.displayedProgress = new SimpleStringProperty(progressDisplayString);
        }

        public int getStt() { return stt; }
        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getStatus() { return status.get(); }
        public String getStartDate() { return startDate.get(); }
        public String getEndDate() { return endDate.get(); }
        public String getTeacher() { return teacher.get(); }
        public String getManager() { return manager.get(); }
        public String getClassDate() { return classDate.get(); }
        public String getDisplayedProgress() { return displayedProgress.get(); }
        public SimpleStringProperty displayedProgressProperty() { return displayedProgress; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}