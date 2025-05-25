package src.view.settings;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import src.dao.Accounts.AccountDAO;
import src.dao.Person.CourseDAO;
import src.dao.Person.TeacherDAO;
import src.dao.Person.UserDAO;
import src.model.system.course.Course;
import src.model.person.Teacher;
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

    private TeacherDAO teacherDAO;
    private AccountDAO accountDAO;
    private CourseDAO courseDAO;
    private UserDAO userDAO;
    private List<Teacher> originalTeachersList;


    public SettingsView() {
        super("Cài đặt Trung Tâm", "settings_view");
        try {
            this.teacherDAO = new TeacherDAO();
            this.accountDAO = new AccountDAO();
            this.userDAO = new UserDAO();
            this.courseDAO = new CourseDAO();
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
        root.getChildren().addAll(teacherSection, courseSection); // Thêm cả hai mục
    }
    private void loadActualData() {
        teacherData.clear();
        courseData.clear();
        // Dữ liệu mẫu cho danh sách khóa học

        this.originalTeachersList = teacherDAO.findAll();

        if (teacherDAO != null) { // Kiểm tra teacherDAO trước khi gọi findAll
            this.originalTeachersList = teacherDAO.findAll();
        } else {
            showError("TeacherDAO chưa được khởi tạo. Không thể tải danh sách giáo viên.");
            if (teacherTable != null) {
                teacherTable.setPlaceholder(new Label("Lỗi tải dữ liệu giáo viên (DAO error)."));
            }
            return;
        }

        try {
            List<Teacher> teachersFromDB = teacherDAO.findAll();
            System.out.println("--- [SettingView] Dữ liệu được tải bởi teacherDAO.findAll() sau khi có thể đã update ---");
            if (this.originalTeachersList.isEmpty()) {
                System.out.println("--- [SettingView] originalTeachersList rỗng.");
            }
            for (Teacher teacherDebug : this.originalTeachersList) {
                // In ra thông tin của một vài giáo viên, đặc biệt là giáo viên bạn vừa sửa
                // Ví dụ: nếu bạn biết ID của giáo viên vừa sửa (giả sử là teacherModel.getId() trước khi gọi dialog)
                // if (teacherDebug.getId().equals(idCuaGiaoVienVuaSua)) { // idCuaGiaoVienVuaSua là biến bạn cần truyền vào hoặc xác định
                System.out.println("GV (trong originalTeachersList): ID=" + teacherDebug.getId() +
                        ", Tên=" + teacherDebug.getName() +
                        ", Email=" + teacherDebug.getEmail() +
                        ", Ngày sinh (model string)=" + teacherDebug.getBirthday());
                // }
            }
            System.out.println("----------------------------------------------------------------------------------");


            if (teachersFromDB.isEmpty()) { // Lưu ý: bạn đang dùng biến teachersFromDB ở đây,
                // nhưng phía trên lại gán kết quả findAll() cho this.originalTeachersList.
                // Cần đảm bảo bạn đang xử lý đúng danh sách.
                // Có lẽ bạn muốn dùng this.originalTeachersList ở đây.
                System.out.println("Không tìm thấy giáo viên nào trong cơ sở dữ liệu.");
            }
            if (teachersFromDB.isEmpty()) {
                System.out.println("Không tìm thấy giáo viên nào trong cơ sở dữ liệu.");
            }

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
                // if (teacherModel.getAddress() != null) { addressValue = teacherModel.getAddress(); } // Nếu Teacher model có getAddress()

                // SỬA LỜI GỌI CONSTRUCTOR CHO ĐÚNG VỚI THAY ĐỔI Ở TRÊN
                teacherData.add(new TeacherPlaceholder(
                        currentUserId,              // Tham số 1: userId
                        currentTeacherRecordId,     // Tham số 2: teacherRecordId
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
                // Error already shown
            } else if (teacherData.isEmpty()){
                teacherTable.setPlaceholder(new Label("Không có dữ liệu giáo viên."));
            }
        }
        if (courseTable != null) {
            courseTable.setItems(courseData);
        }
        if (courseDAO != null) {
            try {
                List<Course> coursesFromDB = courseDAO.findAll(); // Gọi findAll từ CourseDAO
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
            } catch (Exception e) { // Bắt Exception chung hoặc SQLException cụ thể nếu CourseDAO ném
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
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(140); // Điều chỉnh độ rộng nếu cần

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

        // Cột "Địa chỉ" đã được xóa khỏi đây

        TableColumn<TeacherPlaceholder, String> accountUsernameCol = new TableColumn<>("Tên tài khoản");
        accountUsernameCol.setCellValueFactory(new PropertyValueFactory<>("accountUsername"));
        accountUsernameCol.setPrefWidth(120);

        TableColumn<TeacherPlaceholder, String> passwordCol = new TableColumn<>("Mật khẩu"); // Thực tế không nên hiển thị pass
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
                            // Tìm Teacher model gốc
                            Teacher originalTeacher = findOriginalTeacherModel(selectedPlaceholder.getId());

                            if (originalTeacher != null) {
                                TeacherDetailsDialog detailDialog = new TeacherDetailsDialog(
                                        root.getScene().getWindow(),
                                        selectedPlaceholder,
                                        originalTeacher // Thêm tham số thứ ba
                                );
                                // Xử lý kết quả từ dialog (nếu cần, dựa trên việc dialog trả về Boolean)
                                Optional<Boolean> result = detailDialog.showAndWait();
                                if (result.isPresent() && result.get()) { // Nếu có thay đổi được lưu
                                    // Cập nhật vào DB và làm mới bảng
                                    boolean updateSuccess = teacherDAO.update(originalTeacher);
                                    if (updateSuccess) {
                                        loadActualData(); // Tải lại dữ liệu để làm mới bảng
                                    } else {
                                        showError("Không thể cập nhật thông tin giáo viên.");
                                    }
                                }
                            } else {
                                showError("Không tìm thấy dữ liệu giáo viên gốc tương ứng.");
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

        TableColumn<TeacherPlaceholder, Void> deleteCol = new TableColumn<>(""); // Không title
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
                        // Chỉ hiển thị nút nếu dòng có dữ liệu
                        // Tránh NullPointerException nếu getIndex() trả về giá trị ngoài khoảng của items
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
        teacherTable.setPrefHeight(250);

        section.getChildren().addAll(title, teacherTable);
        return section;
    }
    // Thêm hàm này vào lớp SettingView
    private Teacher findOriginalTeacherModel(String userId) {
        if (this.originalTeachersList == null) {
            return null;
        }
        for (Teacher teacherModel : this.originalTeachersList) {
            if (teacherModel.getId().equals(userId)) { // So sánh users.id
                return teacherModel;
            }
        }
        return null;
    }
    // Trong class SettingView.java

    private VBox createCourseListSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 20px; -fx-border-color: #dee2e6; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label title = new Label("Danh sách Khóa học");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #28a745;"); // Màu xanh cho khóa học

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
                        // Lấy đối tượng CoursePlaceholder từ dòng hiện tại
                        // Kiểm tra getIndex() để tránh lỗi nếu bảng trống hoặc index không hợp lệ
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
                        // Chỉ hiển thị nút nếu dòng có dữ liệu thực sự
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
        courseTable.setItems(courseData); // Gán ObservableList cho bảng
        courseTable.setPlaceholder(new Label("Đang tải dữ liệu hoặc không có khóa học nào..."));
        courseTable.setPrefHeight(250); // Điều chỉnh chiều cao nếu cần

        section.getChildren().addAll(title, courseTable);
        return section;
    }
    // Trong class SettingView.java

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
                success = courseDAO.delete(courseIdToDelete); // Gọi phương thức delete của CourseDAO
            } catch (Exception e) { // Bắt Exception chung phòng trường hợp CourseDAO.delete ném lỗi không mong muốn
                System.err.println("Lỗi khi gọi courseDAO.delete cho courseId " + courseIdToDelete + ": " + e.getMessage());
                e.printStackTrace();
                showError("Đã có lỗi xảy ra trong quá trình xóa khóa học: " + e.getMessage());
            }

            if (success) {
                showSuccess("Đã xóa thành công khóa học: " + courseToDelete.getCourseName());
                loadActualData(); // Tải lại toàn bộ dữ liệu để làm mới các bảng
            } else {
                // Nếu courseDAO.delete trả về false mà không ném lỗi
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
        loadActualData();
        System.out.println(getViewId() + " data refresh complete.");
    }

    public static class TeacherPlaceholder {
        private String userId;        private String teacherRecordId;
        private String name;
        private String gender;
        private String contactNumber;
        private LocalDate birthday;
        private String email;
        private String address; // Giữ lại trường address để TeacherDetailDialog có thể sử dụng
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
        // Trong lớp SettingView.TeacherPlaceholder
        public void setName(String name) { this.name = name; }
        public void setGender(String gender) { this.gender = gender; }
        public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
        public void setEmail(String email) { this.email = email; }
// accountUsername và passwordDisplay thường không được sửa đổi từ dialog này.

        public String getId() { return userId; }
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
        public void showError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        // @Override
        public void showSuccess(String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    public static class CoursePlaceholder {
        private String courseId;
        private String courseName;
        private String roomId;
        private String duration; // Sẽ là chuỗi "xx phút"
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
                        "- Bản ghi trong bảng Teachers (ID: " + teacherToDelete.getTeacherRecordId() + ")\n" +
                        "- Bản ghi trong bảng Users (ID: " + teacherToDelete.getUserId() + ")\n" +
                        "- Tài khoản đăng nhập (nếu có)\n\n" +
                        "Hành động này không thể hoàn tác."
        );

        Optional<ButtonType> result = confirmationDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String userIdToDelete = teacherToDelete.getUserId();
            String teacherRecordIdToDelete = teacherToDelete.getTeacherRecordId();

            boolean overallSuccess = false;
            StringBuilder errorMessageBuilder = new StringBuilder();

            // LƯU Ý QUAN TRỌNG VỀ TRANSACTION:
            // Các thao tác xóa dưới đây nên được thực hiện trong một giao dịch CSDL (database transaction)
            // để đảm bảo tính toàn vẹn dữ liệu. Nếu một bước thất bại, các bước trước đó nên được rollback.
            // Hiện tại, mỗi DAO tự quản lý transaction riêng, điều này không lý tưởng cho chuỗi thao tác này.
            // Để đơn giản, chúng ta sẽ thực hiện tuần tự. Trong môi trường production, bạn cần quản lý transaction tập trung.

            // Bước 1: Xóa bản ghi trong bảng 'teachers' (dùng teachers.id)
            boolean teacherRecordDeleted = teacherDAO.delete(teacherRecordIdToDelete);
            if (teacherRecordDeleted) {
                System.out.println("Đã xóa bản ghi giáo viên từ bảng 'teachers': " + teacherRecordIdToDelete);

                // Bước 2: Tìm và xóa tài khoản liên kết từ 'accounts'
                Optional<AccountDAO.Account> accountOpt = accountDAO.findByUserId(userIdToDelete);
                boolean accountOperationSuccess = true; // Giả định thành công nếu không có tài khoản

                if (accountOpt.isPresent()) {
                    String accountIdToDelete = accountOpt.get().getId();
                    boolean accountDeleted = accountDAO.delete(accountIdToDelete);
                    if (accountDeleted) {
                        System.out.println("Đã xóa tài khoản từ bảng 'accounts': " + accountIdToDelete);
                    } else {
                        String msg = "Không thể xóa tài khoản (ID: " + accountIdToDelete + ") cho user: " + userIdToDelete + ". ";
                        System.err.println(msg);
                        errorMessageBuilder.append(msg);
                        accountOperationSuccess = false;
                    }
                } else {
                    System.out.println("Không tìm thấy tài khoản liên kết với User ID: " + userIdToDelete + " trong bảng 'accounts'. Bỏ qua xóa tài khoản.");
                }

                // Chỉ tiếp tục xóa user nếu việc xóa teacher và account (nếu có) không gặp vấn đề nghiêm trọng
                // hoặc nếu bạn chấp nhận xóa user ngay cả khi account không xóa được (dữ liệu mồ côi).
                // Trong trường hợp này, chúng ta tiếp tục nếu không có lỗi nghiêm trọng từ account.
                if (accountOperationSuccess) {
                    // Bước 3: Xóa bản ghi trong bảng 'users' (dùng users.id)
                    boolean userDeleted = userDAO.delete(userIdToDelete);
                    if (userDeleted) {
                        System.out.println("Đã xóa người dùng từ bảng 'users': " + userIdToDelete);
                        overallSuccess = true; // Tất cả các bước chính đã thành công
                    } else {
                        String msg = "Không thể xóa bản ghi người dùng (ID: " + userIdToDelete + "). ";
                        System.err.println(msg);
                        errorMessageBuilder.append(msg);
                    }
                }
            } else {
                String msg = "Không thể xóa bản ghi giáo viên (ID Bảng Teachers: " + teacherRecordIdToDelete + "). ";
                System.err.println(msg);
                errorMessageBuilder.append(msg);
            }

            if (overallSuccess) {
                showSuccess("Đã xóa thành công giáo viên: " + teacherToDelete.getName());
            } else {
                showError("Xóa giáo viên thất bại. Chi tiết: " + (errorMessageBuilder.length() == 0 ? "Lỗi không xác định." : errorMessageBuilder.toString()) + "Vui lòng kiểm tra log.");
            }

            loadActualData(); // Tải lại dữ liệu để làm mới bảng
        }
    }
}