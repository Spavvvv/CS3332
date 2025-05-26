package src.view.settings;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import src.dao.Accounts.AccountDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Person.CourseDAO;
import src.dao.Person.StudentDAO; // Unused, but kept as in original
import src.dao.Person.TeacherDAO;
import src.dao.Person.UserDAO;
import src.model.holidays.Holiday;
import src.model.system.course.Course;
import src.model.person.Teacher;
import src.utils.DaoManager;
import src.view.components.Screen.BaseScreenView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class SettingsView extends BaseScreenView {

    private ObservableList<TeacherPlaceholder> teacherData = FXCollections.observableArrayList();
    private ObservableList<CoursePlaceholder> courseData = FXCollections.observableArrayList();

    private TableView<TeacherPlaceholder> teacherTable;
    private TableView<CoursePlaceholder> courseTable;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private TextField holidayNameField;
    private DatePicker holidayStartDatePicker;
    private DatePicker holidayEndDatePicker;
    private ColorPicker holidayColorPicker;
    private Button saveHolidayButton;

    private TeacherDAO teacherDAO;
    private AccountDAO accountDAO;
    private CourseDAO courseDAO;
    private UserDAO userDAO;
    private List<Teacher> originalTeachersList;
    private HolidayDAO holidayDAO;



    public SettingsView() {
        super("Cài đặt Trung Tâm", "settings_view");
        try {
            teacherDAO = new TeacherDAO();
            accountDAO = new AccountDAO();
            userDAO = new UserDAO();
            courseDAO = new CourseDAO();
            courseDAO.setTeacherDAO(teacherDAO);
            this.holidayDAO = DaoManager.getInstance().getHolidayDAO();
        } catch (SQLException e) {
            System.err.println("Lỗi nghiêm trọng: Không thể khởi tạo DAO. " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initializeView() {
        root.setSpacing(20);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #f8f9fa;");

        loadActualData(); // Tải dữ liệu cho cả giáo viên và khóa học

        VBox teacherSection = createTeacherListSection();
        VBox courseSection = createCourseListSection(); // Gọi phương thức tạo mục khóa học
        VBox holidayCreationSection = createHolidayCreationSection(); // VBox này chứa form tạo ngày nghỉ

        // --- BẮT ĐẦU THAY ĐỔI: BỌC VÀ CĂN GIỮA KHU VỰC TẠO NGÀY NGHỈ ---
        HBox holidaySectionContainer = new HBox(holidayCreationSection); // Bọc holidayCreationSection vào HBox
        holidaySectionContainer.setAlignment(Pos.CENTER); // Căn giữa holidayCreationSection bên trong HBox
        // --- KẾT THÚC THAY ĐỔI ---

        VBox contentBox = new VBox(20); // Khoảng cách 20px giữa các khu vực
        contentBox.setPadding(new Insets(25)); // Padding cho toàn bộ nội dung bên trong ScrollPane
        // --- THAY ĐỔI: Thêm holidaySectionContainer thay vì holidayCreationSection trực tiếp ---
        contentBox.getChildren().addAll(teacherSection, courseSection, holidaySectionContainer);
        // --- KẾT THÚC THAY ĐỔI ---

        // Tạo ScrollPane và đặt contentBox vào đó
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- BẮT ĐẦU THAY ĐỔI: RÀNG BUỘC CHIỀU RỘNG CHO holidayCreationSection ---
        // Giới hạn chiều rộng tối đa của holidayCreationSection (VBox chứa form)
        // bằng một tỷ lệ phần trăm của chiều rộng viewport của ScrollPane.
        holidayCreationSection.maxWidthProperty().bind(
                Bindings.createDoubleBinding(
                        () -> scrollPane.getViewportBounds().getWidth() * 0.34, // Tỷ lệ 34% (gần 1/3)
                        scrollPane.viewportBoundsProperty()
                )
        );
        // Đặt một độ rộng tối thiểu hợp lý để UI không quá hẹp
        holidayCreationSection.setMinWidth(340); // Bạn có thể điều chỉnh giá trị này
        // --- KẾT THÚC THAY ĐỔI ---

        // Xóa các con cũ của root (nếu có) và thêm ScrollPane vào
        root.getChildren().clear();
        root.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
    }
    private void loadActualData() {
        teacherData.clear();
        courseData.clear();
        // Dữ liệu mẫu cho danh sách khóa học

        // this.originalTeachersList = teacherDAO.findAll(); // This line was redundant and potentially problematic before DAO check

        if (teacherDAO != null) { // Kiểm tra teacherDAO trước khi gọi findAll
            this.originalTeachersList = teacherDAO.findAll();
        } else {
            showError("TeacherDAO chưa được khởi tạo. Không thể tải danh sách giáo viên.");
            if (teacherTable != null) {
                teacherTable.setPlaceholder(new Label("Lỗi tải dữ liệu giáo viên (DAO error)."));
            }
            // Clear course data as well if dependent DAOs might be affected, or handle separately
            if (courseTable != null) {
                courseTable.setPlaceholder(new Label("Lỗi tải dữ liệu (TeacherDAO error ảnh hưởng CourseDAO)."));
            }
            return; // Exit if critical DAO is missing
        }

        try {
            // List<Teacher> teachersFromDB = teacherDAO.findAll(); // This was redundant, using originalTeachersList directly
            System.out.println("--- [SettingView] Dữ liệu được tải bởi teacherDAO.findAll() sau khi có thể đã update ---");
            if (this.originalTeachersList == null || this.originalTeachersList.isEmpty()) { // Added null check
                System.out.println("--- [SettingView] originalTeachersList rỗng hoặc null.");
            } else {
                for (Teacher teacherDebug : this.originalTeachersList) {
                    System.out.println("GV (trong originalTeachersList): ID=" + teacherDebug.getId() +
                            ", Tên=" + teacherDebug.getName() +
                            ", Email=" + teacherDebug.getEmail() +
                            ", Ngày sinh (model string)=" + teacherDebug.getBirthday());
                }
            }
            System.out.println("----------------------------------------------------------------------------------");


            if (this.originalTeachersList == null || this.originalTeachersList.isEmpty()) {
                System.out.println("Không tìm thấy giáo viên nào trong cơ sở dữ liệu.");
            } else {
                for (Teacher teacherModel : this.originalTeachersList) { // Lặp qua originalTeachersList
                    String accountUsernameDisplay = "N/A";
                    String passwordDisplay = "N/A"; // Nên là "********"

                    String currentUserId = teacherModel.getId(); // Đây là users.id từ Teacher model
                    String currentTeacherRecordId = teacherModel.getTeacherId(); // Đây là teachers.id từ Teacher model

                    if (currentUserId != null && !currentUserId.trim().isEmpty() && !currentUserId.equalsIgnoreCase("N/A") && accountDAO != null) {
                        Optional<AccountDAO.Account> accountOpt = accountDAO.findByUserId(currentUserId);
                        if (accountOpt.isPresent()) {
                            AccountDAO.Account account = accountOpt.get();
                            accountUsernameDisplay = account.getUsername();
                            passwordDisplay = "********";
                        } else {
                            // System.out.println("Không tìm thấy tài khoản cho user ID: " + currentUserId);
                        }
                    } else {
                        // System.out.println("Teacher " + currentTeacherRecordId + " (" + teacherModel.getName() + ") không có user_id hợp lệ hoặc AccountDAO null.");
                    }

                    LocalDate birthdayDate = null;
                    if (teacherModel.getBirthday() != null && !teacherModel.getBirthday().trim().isEmpty()) {
                        try {
                            birthdayDate = LocalDate.parse(teacherModel.getBirthday(), SQL_DATE_FORMATTER);
                        } catch (DateTimeParseException e) {
                            System.err.println("Lỗi parse ngày sinh cho giáo viên " + teacherModel.getName() + " với giá trị: " + teacherModel.getBirthday() + ". Lỗi: " + e.getMessage());
                        }
                    }

                    String addressValue = "N/A";
                    // if (teacherModel.getAddress() != null) { addressValue = teacherModel.getAddress(); } // If Teacher model has getAddress()

                    teacherData.add(new TeacherPlaceholder(
                            currentUserId,
                            currentTeacherRecordId,
                            teacherModel.getName(),
                            teacherModel.getGender(),
                            teacherModel.getContactNumber(),
                            birthdayDate,
                            teacherModel.getEmail(),
                            addressValue,
                            accountUsernameDisplay,
                            passwordDisplay
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải dữ liệu giáo viên từ cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
            showError("Lỗi tải dữ liệu giáo viên: " + e.getMessage());
            if (teacherTable != null) {
                teacherTable.setPlaceholder(new Label("Lỗi tải dữ liệu. Chi tiết: " + e.getMessage()));
            }
        }


        if (teacherTable != null) {
            teacherTable.setItems(teacherData);
            if (teacherData.isEmpty() && (teacherDAO == null || accountDAO == null) ) {
                // Error already shown or handled by initial DAO check
            } else if (teacherData.isEmpty()){
                teacherTable.setPlaceholder(new Label("Không có dữ liệu giáo viên."));
            }
        }
        // Course data loading
        if (courseDAO != null) {
            try {
                List<Course> coursesFromDB = courseDAO.findAll();
                if (coursesFromDB.isEmpty()) {
                    System.out.println("[SettingView] Không tìm thấy khóa học nào trong cơ sở dữ liệu.");
                }
                for (Course course : coursesFromDB) {
                    String durationStr = "N/A";
                    if (course.getCourseStartTime() != null && course.getCourseEndTime() != null) {
                        long minutes = java.time.Duration.between(course.getCourseStartTime(), course.getCourseEndTime()).toMinutes();
                        durationStr = minutes + " phút";
                    }

                    courseData.add(new CoursePlaceholder(
                            course.getCourseId(),
                            course.getCourseName(),
                            course.getRoomId(),
                            durationStr,
                            course.getDaysOfWeekAsString()
                    ));
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi tải dữ liệu khóa học: " + e.getMessage());
                e.printStackTrace();
                showError("Lỗi tải dữ liệu khóa học: " + e.getMessage());
            }
        } else {
            showError("CourseDAO chưa được khởi tạo. Không thể tải danh sách khóa học.");
        }

        if (courseTable != null) {
            courseTable.setItems(courseData);
            if (courseData.isEmpty() && courseDAO == null) {
                // Lỗi đã được hiển thị
            } else if (courseData.isEmpty()) {
                courseTable.setPlaceholder(new Label("Không có dữ liệu khóa học."));
            }
        }
    }


    private VBox createTeacherListSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 20px; -fx-border-color: #dee2e6; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label title = new Label("Danh sách Giáo viên");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: black;");
        teacherTable = new TableView<>();
        teacherTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<TeacherPlaceholder, String> idCol = new TableColumn<>("Mã GV");
        idCol.setCellValueFactory(new PropertyValueFactory<>("teacherRecordId")); // Assuming this should be teacherRecordId for display as "Mã GV"
        idCol.setPrefWidth(100); // Adjusted width

        TableColumn<TeacherPlaceholder, String> nameCol = new TableColumn<>("Họ tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(170);

        TableColumn<TeacherPlaceholder, String> genderCol = new TableColumn<>("Giới tính");
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        genderCol.setPrefWidth(70);

        TableColumn<TeacherPlaceholder, String> contactCol = new TableColumn<>("SĐT");
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));
        contactCol.setPrefWidth(100);

        TableColumn<TeacherPlaceholder, String> birthdayCol = new TableColumn<>("Ngày sinh");
        birthdayCol.setCellValueFactory(new PropertyValueFactory<>("formattedBirthday"));
        birthdayCol.setPrefWidth(90);

        TableColumn<TeacherPlaceholder, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(160);

        TableColumn<TeacherPlaceholder, String> accountUsernameCol = new TableColumn<>("Tên tài khoản");
        accountUsernameCol.setCellValueFactory(new PropertyValueFactory<>("accountUsername"));
        accountUsernameCol.setPrefWidth(120);

        TableColumn<TeacherPlaceholder, String> passwordCol = new TableColumn<>("Mật khẩu");
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("passwordDisplay"));
        passwordCol.setPrefWidth(80);

        TableColumn<TeacherPlaceholder, Void> detailCol = new TableColumn<>("Chi tiết");
        Callback<TableColumn<TeacherPlaceholder, Void>, TableCell<TeacherPlaceholder, Void>> cellFactory = param -> {
            final TableCell<TeacherPlaceholder, Void> cell = new TableCell<>() {
                private final Button btn = new Button("Xem");
                {
                    btn.setOnAction(event -> {
                        TeacherPlaceholder selectedPlaceholder = getTableView().getItems().get(getIndex());
                        if (selectedPlaceholder != null) {
                            Teacher originalTeacher = findOriginalTeacherModel(selectedPlaceholder.getUserId()); // Use UserId to find original

                            if (originalTeacher != null) {
                                TeacherDetailsDialog detailDialog = new TeacherDetailsDialog(
                                        root.getScene().getWindow(),
                                        selectedPlaceholder,
                                        originalTeacher
                                );
                                Optional<Boolean> result = detailDialog.showAndWait();
                                if (result.isPresent() && result.get()) {
                                    boolean updateSuccess = teacherDAO.update(originalTeacher);
                                    if (updateSuccess) {
                                        loadActualData();
                                    } else {
                                        showError("Không thể cập nhật thông tin giáo viên.");
                                    }
                                }
                            } else {
                                showError("Không tìm thấy dữ liệu giáo viên gốc tương ứng cho User ID: " + selectedPlaceholder.getUserId());
                            }
                        }
                    });
                    btn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 3 8 3 8;");
                    btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #138496; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 3 8 3 8;"));
                    btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 3 8 3 8;"));
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(btn);
                    }
                }
            };
            return cell;
        };

        detailCol.setCellFactory(cellFactory);
        detailCol.setPrefWidth(70);
        detailCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<TeacherPlaceholder, Void> deleteCol = new TableColumn<>("");
        deleteCol.setStyle("-fx-alignment: CENTER;");
        deleteCol.setPrefWidth(60);
        deleteCol.setSortable(false);

        Callback<TableColumn<TeacherPlaceholder, Void>, TableCell<TeacherPlaceholder, Void>> deleteCellFactory = param -> {
            final TableCell<TeacherPlaceholder, Void> cell = new TableCell<>() {
                private final Button btnDelete = new Button("X");
                {
                    btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;");
                    btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #c82333; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;"));
                    btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;"));

                    btnDelete.setOnAction(event -> {
                        TeacherPlaceholder selectedTeacher = getTableView().getItems().get(getIndex());
                        if (selectedTeacher != null &&
                                !"N/A".equals(selectedTeacher.getUserId()) &&
                                !"N/A".equals(selectedTeacher.getTeacherRecordId())) {
                            handleDeleteTeacher(selectedTeacher);
                        } else {
                            showError("Không thể thực hiện xóa do thiếu thông tin ID giáo viên.");
                        }
                    });
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        if (getIndex() >=0 && getIndex() < getTableView().getItems().size()) {
                            TeacherPlaceholder currentTeacher = getTableView().getItems().get(getIndex());
                            if (currentTeacher != null) {
                                setGraphic(btnDelete);
                            } else {
                                setGraphic(null);
                            }
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
            return cell;
        };
        deleteCol.setCellFactory(deleteCellFactory);

        teacherTable.getColumns().addAll(idCol, nameCol, genderCol, contactCol, birthdayCol, emailCol, accountUsernameCol, passwordCol, detailCol, deleteCol);
        teacherTable.setItems(teacherData);

        Label emptyTeacherLabel = new Label("Đang tải dữ liệu hoặc không có giáo viên nào...");
        emptyTeacherLabel.setStyle("-fx-text-fill: #6c757d; -fx-padding: 10px;");
        teacherTable.setPlaceholder(emptyTeacherLabel);
        // teacherTable.setPrefHeight(250); // Old value
        teacherTable.setPrefHeight(350); // MODIFIED: Increased preferred height

        section.getChildren().addAll(title, teacherTable);
        return section;
    }

    private Teacher findOriginalTeacherModel(String userId) { // Changed parameter to userId to match usage
        if (this.originalTeachersList == null) {
            return null;
        }
        for (Teacher teacherModel : this.originalTeachersList) {
            if (teacherModel.getId() != null && teacherModel.getId().equals(userId)) { // Added null check for teacherModel.getId()
                return teacherModel;
            }
        }
        return null;
    }

    private VBox createCourseListSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 20px; -fx-border-color: #dee2e6; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label title = new Label("Danh sách Khóa học");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #28a745;");

        courseTable = new TableView<>();
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CoursePlaceholder, String> courseIdCol = new TableColumn<>("Mã KH");
        courseIdCol.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        courseIdCol.setPrefWidth(100);

        TableColumn<CoursePlaceholder, String> courseNameCol = new TableColumn<>("Tên Khóa học");
        courseNameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        courseNameCol.setPrefWidth(280);

        TableColumn<CoursePlaceholder, String> roomIdCol = new TableColumn<>("Mã Phòng");
        roomIdCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        roomIdCol.setPrefWidth(100);

        TableColumn<CoursePlaceholder, String> durationCol = new TableColumn<>("Thời lượng");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setPrefWidth(100);

        TableColumn<CoursePlaceholder, String> dayOfWeekCol = new TableColumn<>("Ngày học");
        dayOfWeekCol.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek"));
        dayOfWeekCol.setPrefWidth(120);

        TableColumn<CoursePlaceholder, Void> deleteCourseCol = new TableColumn<>("Xóa");
        deleteCourseCol.setStyle("-fx-alignment: CENTER;");
        deleteCourseCol.setPrefWidth(60);
        deleteCourseCol.setSortable(false);

        Callback<TableColumn<CoursePlaceholder, Void>, TableCell<CoursePlaceholder, Void>> deleteCourseCellFactory = param -> {
            final TableCell<CoursePlaceholder, Void> cell = new TableCell<>() {
                private final Button btnDelete = new Button("X");
                {
                    btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;");
                    btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #c82333; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;"));
                    btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 3 8 3 8; -fx-font-size: 11px;"));

                    btnDelete.setOnAction(event -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            CoursePlaceholder selectedCourse = getTableView().getItems().get(getIndex());
                            if (selectedCourse != null && !"N/A".equals(selectedCourse.getCourseId())) {
                                handleDeleteCourse(selectedCourse);
                            } else {
                                showError("Không thể xóa khóa học do thiếu thông tin ID.");
                            }
                        }
                    });
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        CoursePlaceholder currentCourse = getTableView().getItems().get(getIndex());
                        if (currentCourse != null) {
                            setGraphic(btnDelete);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
            return cell;
        };
        deleteCourseCol.setCellFactory(deleteCourseCellFactory);

        courseTable.getColumns().addAll(courseIdCol, courseNameCol, roomIdCol, durationCol, dayOfWeekCol, deleteCourseCol);
        courseTable.setItems(courseData);
        courseTable.setPlaceholder(new Label("Đang tải dữ liệu hoặc không có khóa học nào..."));
        // courseTable.setPrefHeight(250); // Old value
        courseTable.setPrefHeight(350); // MODIFIED: Increased preferred height

        section.getChildren().addAll(title, courseTable);
        return section;
    }

    private void handleDeleteCourse(CoursePlaceholder courseToDelete) {
        if (courseDAO == null) {
            showError("Lỗi hệ thống: Module quản lý khóa học (CourseDAO) chưa được khởi tạo.");
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Xóa Khóa học");
        confirmationDialog.setHeaderText("Bạn có chắc chắn muốn xóa khóa học '" + courseToDelete.getCourseName() + "' (ID: " + courseToDelete.getCourseId() + ")?");
        confirmationDialog.setContentText("Hành động này sẽ xóa khóa học và tất cả các thông tin liên quan (ví dụ: đăng ký của sinh viên, lịch học). Hành động này không thể hoàn tác.");

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String courseIdToDelete = courseToDelete.getCourseId();
            boolean success = false;
            try {
                success = courseDAO.delete(courseIdToDelete);
            } catch (Exception e) {
                System.err.println("Lỗi khi gọi courseDAO.delete cho courseId " + courseIdToDelete + ": " + e.getMessage());
                e.printStackTrace();
                showError("Đã có lỗi xảy ra trong quá trình xóa khóa học: " + e.getMessage());
            }

            if (success) {
                showSuccess("Đã xóa thành công khóa học: " + courseToDelete.getCourseName());
                loadActualData();
            } else {
                showError("Xóa khóa học thất bại. Khóa học có thể không còn tồn tại hoặc có lỗi không xác định. Vui lòng kiểm tra log.");
            }
        }
    }

    @Override
    public void onActivate() {
        super.onActivate();
        refreshView();
        System.out.println(getViewId() + " activated and data refreshed.");
    }

    @Override
    public void refreshView() {
        super.refreshView();
        System.out.println(getViewId() + " refreshing data...");
        loadActualData(); // Ensure data is reloaded when view is refreshed or activated
        System.out.println(getViewId() + " data refresh complete.");
    }

    public static class TeacherPlaceholder {
        private String userId;        private String teacherRecordId;
        private String name;
        private String gender;
        private String contactNumber;
        private LocalDate birthday;
        private String email;
        private String address;
        private String accountUsername;
        private String passwordDisplay;


        public TeacherPlaceholder(String userId, String teacherRecordId, String name, String gender, String contactNumber,
                                  LocalDate birthday, String email, String address,
                                  String accountUsername, String passwordDisplay) {
            this.userId = (userId == null || userId.trim().isEmpty()) ? "N/A" : userId;
            this.teacherRecordId = (teacherRecordId == null || teacherRecordId.trim().isEmpty()) ? "N/A" : teacherRecordId;
            this.name = (name == null || name.trim().isEmpty()) ? "N/A" : name;
            this.gender = (gender == null || gender.trim().isEmpty()) ? "N/A" : gender;
            this.contactNumber = (contactNumber == null || contactNumber.trim().isEmpty()) ? "N/A" : contactNumber;
            this.birthday = birthday;
            this.email = (email == null || email.trim().isEmpty()) ? "N/A" : email;
            this.address = (address == null || address.trim().isEmpty()) ? "N/A" : address;
            this.accountUsername = (accountUsername == null || accountUsername.trim().isEmpty()) ? "N/A" : accountUsername;
            this.passwordDisplay = (passwordDisplay == null || passwordDisplay.trim().isEmpty()) ? "N/A" : passwordDisplay;
        }
        public void setName(String name) { this.name = name; }
        public void setGender(String gender) { this.gender = gender; }
        public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
        public void setEmail(String email) { this.email = email; }

        public String getId() { return userId; } // This was used as the main ID for finding original teacher.
        public String getName() { return name; }
        public String getGender() { return gender; }
        public String getContactNumber() { return contactNumber; }
        public LocalDate getBirthday() { return birthday; }
        public String getEmail() { return email; }
        public String getAddress() { return address; }
        public String getAccountUsername() { return accountUsername; }
        public String getPasswordDisplay() { return passwordDisplay; }
        public String getUserId() { return userId; }
        public String getTeacherRecordId() { return teacherRecordId; }

        public String getFormattedBirthday() {
            if (birthday != null) {
                return birthday.format(DATE_FORMATTER);
            }
            return "N/A";
        }
        // showError and showSuccess are not part of Placeholder static class, they belong to SettingsView instance
    }
    // Helper methods showError and showSuccess should be instance methods of SettingsView, not TeacherPlaceholder


    public static class CoursePlaceholder {
        private String courseId;
        private String courseName;
        private String roomId;
        private String duration;
        private String dayOfWeek;

        public CoursePlaceholder(String courseId, String courseName, String roomId, String duration, String dayOfWeek) {
            this.courseId = (courseId == null || courseId.trim().isEmpty()) ? "N/A" : courseId;
            this.courseName = (courseName == null || courseName.trim().isEmpty()) ? "N/A" : courseName;
            this.roomId = (roomId == null || roomId.trim().isEmpty()) ? "N/A" : roomId;
            this.duration = (duration == null || duration.trim().isEmpty()) ? "N/A" : duration;
            this.dayOfWeek = (dayOfWeek == null || dayOfWeek.trim().isEmpty()) ? "N/A" : dayOfWeek;
        }

        public String getCourseId() { return courseId; }
        public String getCourseName() { return courseName; }
        public String getRoomId() { return roomId; }
        public String getDuration() { return duration; }
        public String getDayOfWeek() { return dayOfWeek; }
    }
    private void handleDeleteTeacher(TeacherPlaceholder teacherToDelete) {
        if (teacherDAO == null || accountDAO == null || userDAO == null) {
            showError("Lỗi hệ thống: Không thể thực hiện thao tác xóa do thiếu các module quản lý dữ liệu.");
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Xóa Giáo viên");
        confirmationDialog.setHeaderText("Bạn có chắc chắn muốn xóa giáo viên '" + teacherToDelete.getName() + "'?");
        confirmationDialog.setContentText(
                "Thao tác này sẽ xóa toàn bộ thông tin liên quan đến giáo viên này:\n" +
                        "- Bản ghi trong bảng Teachers (ID GV: " + teacherToDelete.getTeacherRecordId() + ")\n" +
                        "- Bản ghi trong bảng Users (ID User: " + teacherToDelete.getUserId() + ")\n" +
                        "- Tài khoản đăng nhập (nếu có)\n\n" +
                        "Hành động này không thể hoàn tác."
        );

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String userIdToDelete = teacherToDelete.getUserId();
            String teacherRecordIdToDelete = teacherToDelete.getTeacherRecordId();

            boolean overallSuccess = false;
            StringBuilder errorMessageBuilder = new StringBuilder();

            // Attempt to delete teacher record first (references users.id, but logically tied to teacher role)
            // Or, if Teacher references User, User must be deleted carefully regarding FKs.
            // Assuming current FKs: teachers.user_id -> users.id, accounts.user_id -> users.id
            // Order: delete from 'teachers', then 'accounts', then 'users'.

            // Bước 1: Xóa bản ghi trong bảng 'teachers' (dùng teachers.id - which is teacherRecordIdToDelete)
            boolean teacherRecordDeleted = teacherDAO.delete(teacherRecordIdToDelete);
            if (teacherRecordDeleted) {
                System.out.println("Đã xóa bản ghi giáo viên từ bảng 'teachers': " + teacherRecordIdToDelete);

                // Bước 2: Tìm và xóa tài khoản liên kết từ 'accounts' (dùng users.id)
                Optional<AccountDAO.Account> accountOpt = accountDAO.findByUserId(userIdToDelete);
                boolean accountOperationSuccess = true;

                if (accountOpt.isPresent()) {
                    String accountIdToDelete = accountOpt.get().getId(); // Assuming Account object has getId() for account's primary key
                    boolean accountDeleted = accountDAO.delete(accountIdToDelete); // Assuming accountDAO.delete uses account's PK
                    if (accountDeleted) {
                        System.out.println("Đã xóa tài khoản từ bảng 'accounts' (ID tài khoản: " + accountIdToDelete + ") cho User ID: " + userIdToDelete);
                    } else {
                        String msg = "Không thể xóa tài khoản (ID tài khoản: " + accountIdToDelete + ") cho User ID: " + userIdToDelete + ". ";
                        System.err.println(msg);
                        errorMessageBuilder.append(msg);
                        accountOperationSuccess = false;
                    }
                } else {
                    System.out.println("Không tìm thấy tài khoản liên kết với User ID: " + userIdToDelete + " trong bảng 'accounts'. Bỏ qua xóa tài khoản.");
                }

                if (accountOperationSuccess) {
                    // Bước 3: Xóa bản ghi trong bảng 'users' (dùng users.id)
                    boolean userDeleted = userDAO.delete(userIdToDelete);
                    if (userDeleted) {
                        System.out.println("Đã xóa người dùng từ bảng 'users': " + userIdToDelete);
                        overallSuccess = true;
                    } else {
                        String msg = "Không thể xóa bản ghi người dùng (ID: " + userIdToDelete + "). Có thể do ràng buộc khóa ngoại hoặc bản ghi không tồn tại. ";
                        System.err.println(msg);
                        errorMessageBuilder.append(msg);
                        // Consider if teacher record should be restored or if this is an acceptable inconsistent state
                        // For now, we proceed with the outcome.
                    }
                } else {
                    // If account deletion failed, we might not want to delete the user yet to avoid orphaning teacher record if user deletion also fails.
                    // However, teacher record is already deleted. This highlights the need for transactions.
                    errorMessageBuilder.append("Do lỗi xóa tài khoản, người dùng (ID: ").append(userIdToDelete).append(") có thể chưa được xóa. ");
                }
            } else {
                String msg = "Không thể xóa bản ghi giáo viên (ID Bảng Teachers: " + teacherRecordIdToDelete + "). Người dùng và tài khoản chưa được xử lý. ";
                System.err.println(msg);
                errorMessageBuilder.append(msg);
            }

            if (overallSuccess) {
                showSuccess("Đã xóa thành công giáo viên: " + teacherToDelete.getName());
            } else {
                showError("Xóa giáo viên thất bại. Chi tiết: " + (errorMessageBuilder.length() == 0 ? "Lỗi không xác định." : errorMessageBuilder.toString()) + "Vui lòng kiểm tra log.");
            }

            loadActualData();
        }
    }
    private VBox createHolidayCreationSection() {
        VBox section = new VBox(18); // Khoảng cách giữa các nhóm con (tiêu đề, grid, button bar)
        section.setPadding(new Insets(30)); // Tăng padding bên trong khu vực
        section.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e0e0e0; " + // Viền màu xám nhạt hơn
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 10px; " + // Bo góc nhiều hơn
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 15, 0, 0, 5);" // Bóng đổ mềm mại hơn
        );

        Label title = new Label("Tạo Ngày nghỉ Mới");
        title.setFont(Font.font("System", FontWeight.BOLD, 20)); // Font hệ thống, đậm, kích thước 20
        title.setStyle("-fx-text-fill: #007bff;"); // Giữ màu xanh dương cho tiêu đề
        VBox.setMargin(title, new Insets(0, 0, 15, 0)); // Khoảng cách dưới cho tiêu đề

        GridPane grid = new GridPane();
        grid.setHgap(15); // Khoảng cách ngang giữa các cột trong grid
        grid.setVgap(12); // Khoảng cách dọc giữa các hàng trong grid

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setHalignment(HPos.LEFT); // Căn trái cho cột nhãn
        labelCol.setPrefWidth(110);      // Độ rộng ưu tiên cho cột nhãn (điều chỉnh nếu cần)
        labelCol.setMinWidth(Control.USE_PREF_SIZE); // Độ rộng tối thiểu bằng nội dung

        ColumnConstraints inputCol = new ColumnConstraints();
        inputCol.setHgrow(Priority.ALWAYS); // Cho phép cột nhập liệu mở rộng
        grid.getColumnConstraints().addAll(labelCol, inputCol);

        // Kiểu chung cho các nhãn và trường nhập liệu
        Font labelFont = Font.font("System", FontWeight.NORMAL, 13.5);
        String inputFieldStyle = "-fx-font-size: 13.5px; " +
                "-fx-background-radius: 5px; " +
                "-fx-border-radius: 5px; " +
                "-fx-border-color: #ced4da; " +
                "-fx-padding: 7px;";

        // Tên ngày nghỉ
        Label nameLabel = new Label("Tên ngày nghỉ:");
        nameLabel.setFont(labelFont);
        holidayNameField = new TextField();
        holidayNameField.setPromptText("Ví dụ: Lễ Quốc Khánh 2/9");
        holidayNameField.setStyle(inputFieldStyle);

        // Ngày bắt đầu
        Label startDateLabel = new Label("Ngày bắt đầu:");
        startDateLabel.setFont(labelFont);
        holidayStartDatePicker = new DatePicker();
        holidayStartDatePicker.setPromptText("Chọn ngày");
        holidayStartDatePicker.setMaxWidth(Double.MAX_VALUE);
        holidayStartDatePicker.setStyle(inputFieldStyle);

        // Ngày kết thúc
        Label endDateLabel = new Label("Ngày kết thúc:");
        endDateLabel.setFont(labelFont);
        holidayEndDatePicker = new DatePicker();
        holidayEndDatePicker.setPromptText("Chọn ngày");
        holidayEndDatePicker.setMaxWidth(Double.MAX_VALUE);
        holidayEndDatePicker.setStyle(inputFieldStyle);

        // Màu sự kiện
        Label colorLabel = new Label("Màu sự kiện:");
        colorLabel.setFont(labelFont);
        holidayColorPicker = new ColorPicker(javafx.scene.paint.Color.web("#87CEFA")); // Màu LightSkyBlue từ ảnh
        holidayColorPicker.setMaxWidth(Double.MAX_VALUE);
        holidayColorPicker.setStyle("-fx-font-size: 13px; -fx-color-label-visible: false; -fx-border-radius: 5px; -fx-border-color: #ced4da;");


        // Thêm các nhãn và trường nhập liệu vào grid
        grid.add(nameLabel, 0, 0);
        grid.add(holidayNameField, 1, 0);

        grid.add(startDateLabel, 0, 1);
        grid.add(holidayStartDatePicker, 1, 1);

        grid.add(endDateLabel, 0, 2);
        grid.add(holidayEndDatePicker, 1, 2);

        grid.add(colorLabel, 0, 3);
        grid.add(holidayColorPicker, 1, 3);

        // Nút Lưu Ngày nghỉ
        saveHolidayButton = new Button("Lưu Ngày nghỉ");
        saveHolidayButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        saveHolidayButton.setPrefHeight(38);
        saveHolidayButton.setDefaultButton(true);
        saveHolidayButton.setMinWidth(130); // Độ rộng tối thiểu cho nút

        String btnBaseStyle = "-fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6px; -fx-font-weight: bold;";
        String btnNormalColor = "-fx-background-color: #28a745;"; // Màu xanh lá
        String btnHoverColor = "-fx-background-color: #218838;";  // Màu xanh lá đậm hơn khi hover

        saveHolidayButton.setStyle(btnNormalColor + btnBaseStyle);
        saveHolidayButton.setOnMouseEntered(e -> saveHolidayButton.setStyle(btnHoverColor + btnBaseStyle));
        saveHolidayButton.setOnMouseExited(e -> saveHolidayButton.setStyle(btnNormalColor + btnBaseStyle));
        saveHolidayButton.setOnAction(e -> handleSaveHoliday()); // LOGIC KHÔNG THAY ĐỔI

        HBox buttonBar = new HBox(saveHolidayButton);
        buttonBar.setAlignment(Pos.CENTER_RIGHT); // Căn phải cho nút
        VBox.setMargin(buttonBar, new Insets(15, 0, 0, 0)); // Khoảng cách trên cho thanh nút

        section.getChildren().addAll(title, grid, buttonBar);
        return section;
    }

    private void handleSaveHoliday() {
        String name = holidayNameField.getText();
        LocalDate startDate = holidayStartDatePicker.getValue();
        LocalDate endDate = holidayEndDatePicker.getValue();
        javafx.scene.paint.Color selectedColor = holidayColorPicker.getValue();

        if (name == null || name.trim().isEmpty()) {
            showError("Tên ngày nghỉ không được để trống.");
            return;
        }
        if (startDate == null) {
            showError("Ngày bắt đầu không được để trống.");
            return;
        }
        if (endDate == null) {
            showError("Ngày kết thúc không được để trống.");
            return;
        }
        if (endDate.isBefore(startDate)) {
            showError("Ngày kết thúc không thể trước ngày bắt đầu.");
            return;
        }
        if (selectedColor == null) {
            showError("Vui lòng chọn một màu hiển thị.");
            return;
        }

        String colorHex = String.format("#%02X%02X%02X",
                (int) (selectedColor.getRed() * 255),
                (int) (selectedColor.getGreen() * 255),
                (int) (selectedColor.getBlue() * 255));

        Holiday newHoliday = new Holiday();
        newHoliday.setName(name);
        newHoliday.setStartDate(startDate);
        newHoliday.setEndDate(endDate);
        newHoliday.setColorHex(colorHex.toUpperCase());

        if (holidayDAO == null) {
            showError("Lỗi hệ thống: HolidayDAO chưa được khởi tạo. Không thể lưu ngày nghỉ.");
            return;
        }

        Holiday savedHoliday = holidayDAO.saveHoliday(newHoliday);

        if (savedHoliday != null && savedHoliday.getId() != null) {
            showSuccess("Đã lưu ngày nghỉ thành công: " + savedHoliday.getName());
            holidayNameField.clear();
            holidayStartDatePicker.setValue(null);
            holidayEndDatePicker.setValue(null);
            holidayColorPicker.setValue(javafx.scene.paint.Color.LIGHTBLUE);
        } else {
            showError("Không thể lưu ngày nghỉ. Vui lòng kiểm tra log của ứng dụng để biết thêm chi tiết.");
        }
    }
}