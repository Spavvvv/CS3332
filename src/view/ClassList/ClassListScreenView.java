package src.view.ClassList;

import src.dao.Classrooms.ClassroomDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Person.CourseDAO;
import src.dao.Person.StudentDAO;
import src.dao.Person.TeacherDAO;
import src.dao.ClassSession.ClassSessionDAO;
import src.model.holidays.HolidaysModel;
import src.model.person.Person;
import src.model.person.Permission; // Đảm bảo enum này tồn tại và có các quyền cần thiết
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
import javafx.util.Pair;
import src.utils.DaoManager;
import src.view.components.Screen.BaseScreenView;
import src.model.system.course.Course;
// CreateClassScreenView đã được import ở trên nếu cần

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.application.Platform;

public class ClassListScreenView extends BaseScreenView {
    // Constants
    private static final String PRIMARY_COLOR = "#5c6bc0";
    private static final String GREEN_COLOR = "#4CAF50";
    private static final String YELLOW_COLOR = "#FFC107";
    private static final String PURPLE_COLOR = "#673AB7";
    private static final String WHITE_COLOR = "#FFFFFF";
    private static final String BORDER_COLOR = "#e0e0e0";
    private static final String TEXT_COLOR = "#424242";
    private static final String LABEL_COLOR = "#64748B"; // Đã thêm

    // UI Components
    private Label titleLabel;
    private TextField searchField;
    private Label totalClassesCountLabel;
    private Button searchButton;
    private Button exportExcelButton;
    private Button createClassButton;
    private ComboBox<String> pageSizeComboBox;
    private ComboBox<String> filterComboBox;
    private TableView<ClassInfo> classesTable;

    // DAOs and Models
    private CourseDAO courseDAO;
    private ClassroomDAO classroomDAO;
    private ClassSessionDAO classSessionDAO;
    private TeacherDAO teacherDAO;
    private HolidaysModel holidaysModel;

    private final ObservableList<ClassInfo> classesData = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ClassListScreenView() {
        super("Lớp học", "classes");

        StudentDAO studentDAO = new StudentDAO();
        this.teacherDAO = new TeacherDAO(); // Khởi tạo teacherDAO

        this.courseDAO = new CourseDAO();   // Khởi tạo courseDAO
        this.courseDAO.setStudentDAO(studentDAO);
        this.courseDAO.setTeacherDAO(this.teacherDAO);
        HolidayDAO holidayDAOInstance = DaoManager.getInstance().getHolidayDAO(); // Giả sử DaoManager cung cấp HolidayDAO

        // 2. Khởi tạo HolidaysModel với HolidayDAO instance
        this.holidaysModel = new HolidaysModel(holidayDAOInstance);
        this.classSessionDAO = new ClassSessionDAO();
        this.classroomDAO = new ClassroomDAO();
        this.holidaysModel = new HolidaysModel(holidayDAOInstance);

        initializeView();
        initializeData();
    }

    private void initializeData() {
        this.classesData.clear();
        try {
            List<Course> coursesFromDb = courseDAO.findAll(); // Sử dụng this.courseDAO
            if (coursesFromDb == null || coursesFromDb.isEmpty()) {
                System.err.println("Không có khóa học nào được truy xuất từ cơ sở dữ liệu.");
            } else {
                System.out.println("Đã tải các khóa học: " + coursesFromDb.size());
                // int stt = 1; // stt sẽ được xử lý trong convertCoursesToClassInfo
                // List<ClassInfo> convertedInfo = convertCoursesToClassInfo(coursesFromDb);
                // this.classesData.setAll(convertedInfo);
                // Thay vì gọi addCourseToTableView nhiều lần, tạo list rồi setAll
                List<ClassInfo> infoList = new ArrayList<>();
                int stt = 1;
                for (Course course : coursesFromDb) {
                    infoList.add(createClassInfoFromCourse(course, stt++));
                }
                this.classesData.setAll(infoList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Lỗi khi tải dữ liệu lớp học: " + e.getMessage());
        }

        if (this.totalClassesCountLabel != null) {
            this.totalClassesCountLabel.setText(String.valueOf(this.classesData.size()));
        }
    }

    // Phương thức helper để tạo ClassInfo từ Course, tránh lặp code
    private ClassInfo createClassInfoFromCourse(Course course, int stt) {
        if (course == null) return null;

        int currentSessionNum = 0;
        if (this.classSessionDAO != null && course.getCourseId() != null) {
            try {
                currentSessionNum = this.classSessionDAO.findCurrentSessionNumberByDate(course.getCourseId(), LocalDate.now())
                        .orElse(0);
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy số buổi học hiện tại (createClassInfoFromCourse) cho khóa " + course.getCourseId() + ": " + e.getMessage());
            }
        }

        int totalSess = course.getTotalSessions();
        String displayedProgressString;
        float progressPercentage = 0f;

        if (totalSess > 0) {
            int effectiveCurrentSession = Math.min(currentSessionNum, totalSess);
            progressPercentage = ((float) effectiveCurrentSession / totalSess) * 100f;
            if (Float.isInfinite(progressPercentage) || Float.isNaN(progressPercentage)) {
                progressPercentage = (currentSessionNum >= totalSess) ? 100f : 0f;
            }
            displayedProgressString = currentSessionNum + "/" + totalSess;
        } else {
            displayedProgressString = currentSessionNum + "/0 (Tổng buổi KXD)";
            progressPercentage = 0f;
        }
        // Không nên cập nhật course.setProgress và DB ở đây vì đây là logic hiển thị
        // course.setProgress(progressPercentage);
        // if (this.courseDAO != null && course.getCourseId() != null) { ... this.courseDAO.updateProgress ... }


        LocalDate actualStartDate = course.getStartDate();
        LocalDate actualEndDate = course.getEndDate();
        String teacherName = (course.getTeacher() != null && course.getTeacher().getName() != null)
                ? course.getTeacher().getName() : "Chưa phân công";

        String classDateDisplay = course.getDaysOfWeekAsString();
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

        return new ClassInfo(stt, course.getCourseId(), course.getCourseName(), courseStatus,
                actualStartDate, actualEndDate, teacherName, classDateDisplay, displayedProgressString);
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
        HBox statisticsContainer = createStatisticsSection();
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

    // addCourseToTableView không còn được gọi trực tiếp từ initializeData nữa
    // Nó được thay thế bằng logic trong createClassInfoFromCourse và convertCoursesToClassInfo
    // private void addCourseToTableView(Course course, int stt) { ... }


    private HBox createTitleBar() {
        HBox titleBar = new HBox(15);
        titleBar.setPadding(new Insets(0, 0, 15, 0));
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-border-color: transparent transparent " + BORDER_COLOR + " transparent; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 10 0;");

        titleLabel = new Label("Danh sách Lớp học");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web(PRIMARY_COLOR));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        createClassButton = new Button();
        createClassButton.setStyle(
                "-fx-background-color: " + GREEN_COLOR + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 18;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 1, 1);"
        );
        Label plusIcon = new Label("➕");
        plusIcon.setTextFill(Color.WHITE);
        plusIcon.setFont(Font.font(14));
        HBox createContent = new HBox(5, plusIcon, new Label("Tạo lớp"));
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
                        "-fx-padding: 8 18;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 1, 1);"
        );
        Label excelIcon = new Label("📤");
        excelIcon.setTextFill(Color.WHITE);
        excelIcon.setFont(Font.font(14));
        HBox exportContent = new HBox(5, excelIcon, new Label("Xuất Excel"));
        exportContent.setAlignment(Pos.CENTER);
        exportExcelButton.setGraphic(exportContent);
        exportExcelButton.setOnAction(e -> exportToExcel());

        titleBar.getChildren().addAll(titleLabel, spacer, createClassButton, exportExcelButton);
        return titleBar;
    }

    private VBox createSingleStatCard(String title, String color, String initialValue,
                                      String valueLabelText, String iconString) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 18, 12, 18));
        card.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 2, 2);"
        );

        Label iconLabel = new Label(iconString);
        iconLabel.setTextFill(Color.WHITE);
        iconLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        Label titleTextLabel = new Label(title);
        titleTextLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleTextLabel.setTextFill(Color.WHITE);
        HBox titleBox = new HBox(8, iconLabel, titleTextLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        this.totalClassesCountLabel = new Label(initialValue);
        this.totalClassesCountLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        this.totalClassesCountLabel.setTextFill(Color.WHITE);

        Label descriptionLabel = new Label(valueLabelText);
        descriptionLabel.setFont(Font.font("System", 11));
        descriptionLabel.setTextFill(Color.web("#FFFFFFB3"));

        VBox statValueBox = new VBox(0, this.totalClassesCountLabel, descriptionLabel);
        statValueBox.setAlignment(Pos.CENTER_LEFT);

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
                "Lớp học hoạt động và sắp tới",
                "🏫"
        );
        totalClassesCard.setPrefWidth(280);
        totalClassesCard.setMaxWidth(Region.USE_PREF_SIZE);
        statsContainer.getChildren().add(totalClassesCard);
        return statsContainer;
    }

    private HBox createSearchAndFilterBar() {
        HBox searchAndFilterBar = new HBox(15);
        searchAndFilterBar.setPadding(new Insets(0, 0, 15, 0));
        searchAndFilterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("Trạng thái:");
        filterLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold;");
        filterComboBox = new ComboBox<>();
        filterComboBox.setPromptText("Tất cả");
        filterComboBox.getItems().addAll("Tất cả", "Đang học", "Sắp bắt đầu", "Đã kết thúc", "Chưa có lịch");
        filterComboBox.setValue("Tất cả");
        filterComboBox.setPrefWidth(160);
        filterComboBox.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4px; -fx-font-size: 13px;");

        HBox filterBox = new HBox(8, filterLabel, filterComboBox);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField = new TextField();
        searchField.setPromptText("Tìm kiếm theo tên, mã, GV...");
        searchField.setPrefWidth(280);
        searchField.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4px 0 0 4px; -fx-padding: 8px 12px; -fx-font-size: 13px;");

        searchButton = new Button();
        searchButton.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 0 4px 4px 0; -fx-cursor: hand; -fx-padding: 8px 15px;");
        Label searchIcon = new Label("🔍");
        searchIcon.setTextFill(Color.WHITE);
        searchButton.setGraphic(searchIcon);
        HBox searchBox = new HBox(0, searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER);

        searchAndFilterBar.getChildren().addAll(filterBox, spacer, searchBox);
        return searchAndFilterBar;
    }

    private void createClassesTable() {
        classesTable = new TableView<>(classesData);
        classesTable.setEditable(false);
        classesTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        classesTable.setMinHeight(400);
        VBox.setVgrow(classesTable, Priority.ALWAYS);

        classesTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 5px;" +
                        "-fx-background-radius: 5px;" +
                        "-fx-border-width: 1px;" +
                        "-fx-selection-bar: " + PRIMARY_COLOR + "30;" +
                        "-fx-selection-bar-text: " + TEXT_COLOR + ";"
        );
        classesTable.setTableMenuButtonVisible(false);
        classesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ClassInfo, Void> selectCol = new TableColumn<>();
        selectCol.setPrefWidth(40); selectCol.setMinWidth(40); selectCol.setMaxWidth(40);
        selectCol.setResizable(false);
        selectCol.setCellFactory(col -> {
            TableCell<ClassInfo, Void> cell = new TableCell<>() {
                private final CheckBox checkBox = new CheckBox();
                {
                    HBox hbox = new HBox(checkBox);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                    checkBox.setOnAction(event -> {
                        if (getTableRow() != null && getTableRow().getItem() != null) {
                            getTableRow().getItem().setSelected(checkBox.isSelected());
                        }
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                    } else {
                        checkBox.setSelected(getTableRow().getItem().isSelected());
                        setGraphic(getGraphic()); // Giữ lại HBox
                    }
                }
            };
            return cell;
        });

        TableColumn<ClassInfo, Integer> sttCol = new TableColumn<>("STT");
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setPrefWidth(50); sttCol.setMinWidth(50); sttCol.setMaxWidth(50);
        sttCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> codeCol = new TableColumn<>("Mã Lớp");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(100); codeCol.setMinWidth(100);

        TableColumn<ClassInfo, String> nameCol = new TableColumn<>("Tên Lớp");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220); nameCol.setMinWidth(180);
        nameCol.setCellFactory(column -> new TableCell<>() {
            private final Label nameLabel = new Label();
            private final Label statusLabelText = new Label();
            private final VBox content = new VBox(3, nameLabel, statusLabelText);
            {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 13px;");
                // statusLabelText style set in updateItem
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
                    statusLabelText.setText(classInfo.getStatus().toUpperCase());
                    String statusColor = GREEN_COLOR;
                    String textColor = WHITE_COLOR;
                    if ("ĐÃ KẾT THÚC".equalsIgnoreCase(classInfo.getStatus().toUpperCase())) { statusColor = "#757575"; }
                    else if ("SẮP BẮT ĐẦU".equalsIgnoreCase(classInfo.getStatus().toUpperCase())) { statusColor = YELLOW_COLOR; textColor = TEXT_COLOR;}
                    else if ("CHƯA CÓ LỊCH".equalsIgnoreCase(classInfo.getStatus().toUpperCase())) { statusColor = "#CFD8DC"; textColor = TEXT_COLOR;}

                    statusLabelText.setStyle(
                            "-fx-background-color: " + statusColor + ";" +
                                    "-fx-text-fill: " + textColor + ";" +
                                    "-fx-background-radius: 10px; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 8px;"
                    );
                    setGraphic(content); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, String> progressCol = new TableColumn<>("Tiến độ");
        progressCol.setCellValueFactory(new PropertyValueFactory<>("displayedProgress"));
        progressCol.setPrefWidth(120); progressCol.setMinWidth(100);
        progressCol.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label progressLabelText = new Label();
            private final HBox progressBox = new HBox(5, progressBar, progressLabelText);
            {
                progressBar.setPrefWidth(70);
                progressBar.setStyle("-fx-accent: " + GREEN_COLOR + ";");
                progressLabelText.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 12px;");
                progressBox.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null || item.equals("N/A") || !item.contains("/")) {
                    setText(item); setGraphic(null);
                } else {
                    try {
                        String[] parts = item.split("/");
                        if (parts.length == 2) {
                            long current = Long.parseLong(parts[0].trim());
                            String totalStr = parts[1].trim().split(" ")[0];
                            long total = Long.parseLong(totalStr);

                            progressBar.setProgress(total == 0 ? 0 : (double) current / total);
                            progressLabelText.setText(item);
                            setGraphic(progressBox); setText(null);
                        } else {
                            setText(item); setGraphic(null);
                        }
                    } catch (Exception e) {
                        setText(item); setGraphic(null);
                        System.err.println("Lỗi parse chuỗi tiến độ: '" + item + "' - " + e.getMessage());
                    }
                }
            }
        });

        TableColumn<ClassInfo, String> startDateCol = new TableColumn<>("Bắt đầu");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startDateCol.setPrefWidth(90); startDateCol.setMinWidth(90); startDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> endDateCol = new TableColumn<>("Kết thúc");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endDateCol.setPrefWidth(90); endDateCol.setMinWidth(90); endDateCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<ClassInfo, String> teacherCol = new TableColumn<>("Giáo viên");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(150); teacherCol.setMinWidth(120);
        teacherCol.setCellFactory(col -> new TableCell<ClassInfo, String>() {
            private final Button assignTeacherButton = new Button();
            {
                assignTeacherButton.setMinWidth(130);
                assignTeacherButton.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ClassInfo selectedClassInfo = getTableRow().getItem();
                        Person currentUser = getCurrentUser();
                        boolean canAddTeacher = currentUser != null && currentUser.getRole() != null &&
                                RolePermissions.hasPermission(currentUser.getRole(), Permission.ADDTEACHER_INTOCOURSE);
                        if (!canAddTeacher) {
                            showInfo("Bạn không có quyền thực hiện thao tác này."); return;
                        }
                        Optional<Course> courseOpt = courseDAO.findById(selectedClassInfo.getCode()); // SỬ DỤNG this.courseDAO
                        if (courseOpt.isEmpty()) {
                            showInfo("Không thể tìm thấy lớp học."); return;
                        }
                        Course course = courseOpt.get();
                        TeacherDAO teacherDAOInstance = courseDAO.getTeacherDAO(); // SỬ DỤNG this.courseDAO
                        if (teacherDAOInstance == null) {
                            System.err.println("Lỗi: TeacherDAO chưa được thiết lập trong CourseDAO.");
                            showInfo("Lỗi: Không thể tải thông tin giáo viên."); return;
                        }
                        AddTeacherIntoCourse dialog = new AddTeacherIntoCourse(course, courseDAO, teacherDAOInstance); // SỬ DỤNG this.courseDAO
                        dialog.show(); // Giả sử .show() là blocking
                        loadClasses(); // Tải lại sau khi dialog đóng
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setGraphic(null);
                } else {
                    boolean noTeacher = "Chưa phân công".equalsIgnoreCase(item) || "Chưa có".equalsIgnoreCase(item) || item.trim().isEmpty();
                    assignTeacherButton.setText(noTeacher ? "Phân công GV" : item);
                    assignTeacherButton.setStyle( (noTeacher
                            ? "-fx-background-color: " + YELLOW_COLOR + "; -fx-text-fill: " + TEXT_COLOR + ";"
                            : "-fx-background-color: transparent; -fx-text-fill: " + PRIMARY_COLOR + "; -fx-underline: true;")
                            + " -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 5px 10px; -fx-font-size: 11px; -fx-cursor: hand;");
                    setGraphic(assignTeacherButton); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, String> classDateCol = new TableColumn<>("Lịch học");
        classDateCol.setCellValueFactory(new PropertyValueFactory<>("classDate"));
        classDateCol.setPrefWidth(100); classDateCol.setMinWidth(90);
        classDateCol.setCellFactory(column -> new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "N/A".equals(item) || item.trim().isEmpty()) {
                    setText("N/A"); setGraphic(null); setTooltip(null);
                } else {
                    String[] days = item.split(",");
                    VBox daysBox = new VBox(1);
                    daysBox.setAlignment(Pos.CENTER_LEFT);
                    StringBuilder tooltipText = new StringBuilder();

                    for (int i = 0; i < days.length; i++) {
                        String day = days[i].trim();
                        if (!day.isEmpty()) {
                            if (i < 2) {
                                Label dayLabel = new Label(day);
                                dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_COLOR +";");
                                daysBox.getChildren().add(dayLabel);
                            }
                            if (tooltipText.length() > 0) tooltipText.append("\n");
                            tooltipText.append(day);
                        }
                    }
                    if (days.length > 2) {
                        Label moreLabel = new Label("... (+" + (days.length - 2) + " ngày)");
                        moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + LABEL_COLOR + ";");
                        daysBox.getChildren().add(moreLabel);
                    }
                    tooltip.setText(tooltipText.toString());
                    setTooltip(tooltip);
                    setGraphic(daysBox); setText(null);
                }
            }
        });

        TableColumn<ClassInfo, Void> studentsCol = new TableColumn<>("Học viên");
        studentsCol.setPrefWidth(90); studentsCol.setMinWidth(90);
        studentsCol.setCellFactory(col -> new TableCell<ClassInfo, Void>() {
            private final Button btn = new Button("DS HV");
            {
                btn.setStyle("-fx-background-color: " + PRIMARY_COLOR + " ; -fx-text-fill: white; -fx-background-radius: 4px; -fx-padding: 5px 10px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                btn.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ClassInfo selectedClassInfo = getTableRow().getItem();
                        // Giả định AddStudentToCourseDialog có constructor (String courseId, CourseDAO courseDAO)
                        AddStudentToCourseDialog dialog = new AddStudentToCourseDialog(selectedClassInfo); // SỬ DỤNG this.courseDAO
                        Optional<Pair<String, String>> result = dialog.showAndWait();
                        result.ifPresent(pair -> {
                            System.out.println("Dialog thêm/xóa học viên đã đóng, làm mới bảng...");
                            loadClasses();
                        });
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : btn);
            }
        });

        TableColumn<ClassInfo, Void> detailsDialogCol = new TableColumn<>("Chi tiết");
        detailsDialogCol.setPrefWidth(80); detailsDialogCol.setMinWidth(80);
        detailsDialogCol.setCellFactory(param -> new TableCell<ClassInfo, Void>() {
            private final Button btnDetails = new Button("Xem");
            {
                btnDetails.setStyle("-fx-background-color: " + PURPLE_COLOR + "; -fx-text-fill: white; -fx-background-radius: 4px; -fx-padding: 5px 10px; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnDetails.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ClassInfo selectedClassInfo = getTableRow().getItem();
                        Optional<Course> optionalCourse = courseDAO.findById(selectedClassInfo.getCode()); // SỬ DỤNG this.courseDAO
                        if (optionalCourse.isPresent()) {
                            Course course = optionalCourse.get();
                            TeacherDAO teacherDAOInstance = courseDAO.getTeacherDAO(); // SỬ DỤNG this.courseDAO
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
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : btnDetails);
            }
        });

        TableColumn<ClassInfo, Void> actionsCol = new TableColumn<>("Khác");
        actionsCol.setPrefWidth(60); actionsCol.setMinWidth(60); actionsCol.setMaxWidth(60);
        actionsCol.setCellFactory(col -> new TableCell<ClassInfo, Void>() {
            private final Button btn = new Button("•••");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_COLOR + "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 4px; -fx-padding: 5px 8px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;");
                btn.setOnAction(event -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        ClassInfo data = getTableRow().getItem();
                        handleMoreActions(data, btn);
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : btn);
            }
        });

        classesTable.getColumns().setAll(
                selectCol, sttCol, codeCol, nameCol, progressCol,
                startDateCol, endDateCol, teacherCol, classDateCol,
                studentsCol, detailsDialogCol, actionsCol
        );

        classesTable.setRowFactory(tv -> {
            TableRow<ClassInfo> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldVal, newVal) -> updateRowStyle(row));
            Platform.runLater(() -> updateRowStyle(row));
            return row;
        });
    }

    private void updateRowStyle(TableRow<ClassInfo> row) {
        String baseStyle = "-fx-border-color: " + BORDER_COLOR + " transparent transparent transparent; -fx-border-width: 1 0 0 0;";
        if (row.isEmpty() || row.getItem() == null) {
            row.setStyle("");
        } else if (row.isSelected()) {
            row.setStyle("-fx-background-color: #BBDEFB; " + baseStyle);
        } else {
            row.setStyle((row.getIndex() % 2 == 0 ? "-fx-background-color: white; " : "-fx-background-color: #F8F9FA; ") + baseStyle);
        }
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(e -> searchClasses(searchField.getText()));
        searchField.setOnAction(e -> searchClasses(searchField.getText()));
        filterComboBox.setOnAction(e -> filterClassesByStatus(filterComboBox.getValue()));
    }

    private List<ClassInfo> convertCoursesToClassInfo(List<Course> courses) {
        List<ClassInfo> classInfos = new ArrayList<>();
        int stt = 1;
        if (courses != null) {
            for (Course course : courses) {
                ClassInfo ci = createClassInfoFromCourse(course, stt++);
                if (ci != null) {
                    classInfos.add(ci);
                }
            }
        }
        return classInfos;
    }

    private void filterClassesByStatus(String statusFilter) {
        try {
            List<Course> coursesFromDb = this.courseDAO.findAll(); // SỬ DỤNG this.courseDAO
            List<ClassInfo> allItemsFromDB = convertCoursesToClassInfo(coursesFromDb);

            if (statusFilter == null || "Tất cả".equalsIgnoreCase(statusFilter)) {
                classesTable.setItems(FXCollections.observableArrayList(allItemsFromDB));
            } else {
                List<ClassInfo> tempList = allItemsFromDB.stream()
                        .filter(classInfo -> statusFilter.equalsIgnoreCase(classInfo.getStatus()))
                        .collect(Collectors.toList()); // Sửa lỗi FXCollections.toObservableList
                classesTable.setItems(FXCollections.observableArrayList(tempList));
            }
            if (this.totalClassesCountLabel != null) {
                this.totalClassesCountLabel.setText(String.valueOf(classesTable.getItems().size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Lỗi khi tải và lọc danh sách lớp học: " + e.getMessage());
        }
    }

    private void searchClasses(String keyword) {
        try {
            List<Course> coursesFromDb = this.courseDAO.findAll(); // SỬ DỤNG this.courseDAO
            List<ClassInfo> allItemsFromDB = convertCoursesToClassInfo(coursesFromDb);

            if (keyword == null || keyword.trim().isEmpty()) {
                classesTable.setItems(FXCollections.observableArrayList(allItemsFromDB));
            } else {
                String lowerCaseKeyword = keyword.toLowerCase();
                List<ClassInfo> tempList = allItemsFromDB.stream()
                        .filter(classInfo -> (classInfo.getName() != null && classInfo.getName().toLowerCase().contains(lowerCaseKeyword)) ||
                                (classInfo.getCode() != null && classInfo.getCode().toLowerCase().contains(lowerCaseKeyword)) ||
                                (classInfo.getTeacher() != null && classInfo.getTeacher().toLowerCase().contains(lowerCaseKeyword)))
                        .collect(Collectors.toList()); // Sửa lỗi FXCollections.toObservableList
                classesTable.setItems(FXCollections.observableArrayList(tempList));
            }
            if (this.totalClassesCountLabel != null) {
                this.totalClassesCountLabel.setText(String.valueOf(classesTable.getItems().size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Lỗi khi tải và tìm kiếm danh sách lớp học: " + e.getMessage());
        }
    }

    private void loadClasses() {
        Platform.runLater(this::initializeData);
    }

    private void exportToExcel() {
        showInfo("Chức năng Xuất Excel đang được phát triển.");
    }

    private void handleMoreActions(ClassInfo classInfo, Control anchorNode) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Chỉnh sửa thông tin");
        MenuItem deleteItem = new MenuItem("Xóa lớp học");

        Person currentUser = getCurrentUser();
        // SỬA LỖI Permission.UPDATE_CLASS
        boolean canEdit = currentUser != null && currentUser.getRole() != null &&
                RolePermissions.hasPermission(currentUser.getRole(), Permission.EDIT_CLASS);
        boolean canDelete = currentUser != null && currentUser.getRole() != null && RolePermissions.hasPermission(currentUser.getRole(), Permission.DELETE_CLASS);

        editItem.setDisable(!canEdit);
        deleteItem.setDisable(!canDelete);

        editItem.setOnAction(e -> {
            showInfo("Chức năng Chỉnh sửa lớp (chưa triển khai đầy đủ): " + classInfo.getName());
            // TODO: Mở dialog chỉnh sửa tương tự CreateClassScreenView nhưng với dữ liệu của classInfo
        });

        deleteItem.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText("Xóa lớp học: " + classInfo.getName() + " (Mã: " + classInfo.getCode() + ")");
            confirmAlert.setContentText("Bạn có chắc chắn muốn xóa lớp học này?\nMọi dữ liệu liên quan cũng có thể bị ảnh hưởng.");
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean deleted = this.courseDAO.delete(classInfo.getCode()); // SỬ DỤNG this.courseDAO
                if(deleted){
                    showInfo("Đã xóa lớp: " + classInfo.getName());
                    loadClasses();
                } else {
                    showInfo("Không thể xóa lớp: " + classInfo.getName() + ". Lỗi hoặc ràng buộc dữ liệu.");
                }
            }
        });

        contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
        contextMenu.show(anchorNode, javafx.geometry.Side.BOTTOM, 0, 5);
    }

    private void showCreateClassDialog() {
        Person currentUser = getCurrentUser();
        boolean canCreateClass = currentUser != null && currentUser.getRole() != null &&
                RolePermissions.hasPermission(currentUser.getRole(), Permission.CREATE_CLASS);

        if (!canCreateClass) {
            showInfo("Bạn không có quyền tạo lớp học mới.");
            return;
        }
        if (this.courseDAO == null || this.classroomDAO == null || this.teacherDAO == null || this.holidaysModel == null) {
            System.err.println("Lỗi: Một hoặc nhiều DAO/Model chưa được khởi tạo.");
            showInfo("Lỗi hệ thống: Không thể mở form tạo lớp. Vui lòng liên hệ quản trị viên.");
            return;
        }

        CreateClassScreenView createClassScreen = new CreateClassScreenView(
                this.courseDAO,
                this.classroomDAO,
                this.teacherDAO,
                this.holidaysModel,
                successfullySavedCourse -> {
                    loadClasses();
                }
        );
        createClassScreen.show();
    }

    @Override
    public void refreshView() {
        System.out.println(getViewId() + " đang làm mới dữ liệu từ cơ sở dữ liệu...");
        Platform.runLater(this::initializeData);
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        // SỬA LỖI getScene()
        if (root != null && root.getScene() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.showAndWait();
    }

    public static class ClassInfo {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty status;
        private final SimpleStringProperty startDate;
        private final SimpleStringProperty endDate;
        private final SimpleStringProperty teacher;
        private final SimpleStringProperty classDate;
        private final SimpleStringProperty displayedProgress;
        private final int stt;
        private boolean selected;

        public ClassInfo(int stt, String code, String name, String status,
                         LocalDate actualStartDate, LocalDate actualEndDate,
                         String teacherName, String classDateString,
                         String progressDisplayString) {
            this.stt = stt;
            this.code = new SimpleStringProperty(code != null ? code : "N/A");
            this.name = new SimpleStringProperty(name != null ? name : "N/A");
            this.status = new SimpleStringProperty(status != null ? status : "N/A");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            this.startDate = new SimpleStringProperty(actualStartDate != null ? actualStartDate.format(formatter) : "N/A");
            this.endDate = new SimpleStringProperty(actualEndDate != null ? actualEndDate.format(formatter) : "N/A");

            this.teacher = new SimpleStringProperty(teacherName != null && !teacherName.trim().isEmpty() ? teacherName : "Chưa có");
            this.classDate = new SimpleStringProperty(classDateString != null && !classDateString.trim().isEmpty() ? classDateString : "N/A");
            this.selected = false;
            this.displayedProgress = new SimpleStringProperty(progressDisplayString != null ? progressDisplayString : "0/0 (KXD)");
        }

        public int getStt() { return stt; }
        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getStatus() { return status.get(); }
        public String getStartDate() { return startDate.get(); }
        public String getEndDate() { return endDate.get(); }
        public String getTeacher() { return teacher.get(); }
        public String getClassDate() { return classDate.get(); }
        public String getDisplayedProgress() { return displayedProgress.get(); }
        public SimpleStringProperty displayedProgressProperty() { return displayedProgress; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}