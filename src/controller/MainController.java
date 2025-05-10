package src.controller;
import src.model.ClassSession;
import view.*;
import view.components.*;
import src.model.attendance.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import src.model.person.Person;
import src.model.person.Admin;
import src.model.person.Student;
import src.model.person.Teacher;
import src.model.person.Parent;
import javafx.scene.control.Alert;
import view.components.ClassList.ClassListScreenView;

/**
 Controller chính của ứng dụng, quản lý logic nghiệp vụ
 */
public class MainController {
    private UI ui;
    private LoginUI loginUI;
    private RegisterUI registerUI;
    private NavigationController navigationController;
    private ClassSession currentSessionDetail; // Added to store current session detail
    private Person currentUser;
    // Field to store attendance records for the current session
    private List<Attendance> currentSessionAttendances;
    private AttendanceController attendanceController;
    /**
     Constructor với UI và NavigationController
     @param ui Interface người dùng
     @param navigationController Controller điều hướng
     */
    public MainController(UI ui, NavigationController navigationController) throws SQLException {
        this.ui = ui;
        this.navigationController = navigationController;
        initialize();
    }
    public MainController(LoginUI ui, NavigationController navigationController) {
        this.loginUI = ui;
        this.navigationController = navigationController;
//initialize();
    }
    public MainController(RegisterUI ui, NavigationController navigationController) {
        this.registerUI = ui;
        this.navigationController = navigationController;
//initialize();
    }
    /**
     Khởi tạo controller và đăng ký các màn hình
     */
    private void initialize() throws SQLException {
        registerViews();

        // Thiết lập main controller cho tất cả các view đã đăng ký
        setMainControllerForAllViews();
    }
    /**
     Đăng ký các view với NavigationController
     */
    private void registerViews() throws SQLException {
// Đăng ký các views với NavigationController
        navigationController.registerView("dashboard", new DashboardView());
// TODO: Đăng ký thêm các views khác
// Ví dụ:
// navigationController.registerView("student/list", new StudentListView());
        navigationController.registerView("schedule", new ScheduleView());
        navigationController.registerView("classDetails", new ClassDetailsView());
        navigationController.registerView("attendance", new AttendanceScreenView());
        navigationController.registerView("absence-call-view", new AbsenceCallView());
        navigationController.registerView("absence-call-table", new AbsenceCallScreenView());
        navigationController.registerView("exams", new ExamsView());
        navigationController.registerView("details-view", new DetailsView());
        navigationController.registerView("ClassListView", new ClassListScreenView());
        navigationController.registerView("learning-reports", new ReportView());
        navigationController.registerView("teaching-statistics", new TeachingStatisticsView());
        navigationController.registerView("monthly-teaching", new MonthlyTeachingStatisticsView());
        navigationController.registerView("quarterly-teaching", new QuarterlyTeachingStatisticsView());
        navigationController.registerView("yearly-teaching", new YearlyTeachingStatisticsView());
        navigationController.registerView("classrooms", new RoomView());
        navigationController.registerView("holidays", new HolidaysView());
    }
    /**
     Thiết lập MainController cho tất cả các view đã đăng ký
     */
    private void setMainControllerForAllViews() {
        for (String route : navigationController.getRegisteredRoutes()) {
            ScreenView view = navigationController.getViewByRoute(route);
            if (view != null) {
// Cung cấp reference đến MainController cho view có thể sử dụng
                view.setMainController(this);
                System.out.println("MainController set for view: " + view.getTitle());
            }
        }
    }
    /**
     Thiết lập MainController cho một view cụ thể
     @param view View cần thiết lập MainController
     */
    public void setMainControllerForView(BaseScreenView view) {
        if (view != null) {
            view.setMainController(this);
        }
    }
    /**
     Thực hiện hành động refresh màn hình hiện tại
     */
    public void refreshCurrentView() {
        navigationController.refreshCurrentScreen();
    }
    /**
     Chuyển đến màn hình được chỉ định
     @param route Định danh của màn hình
     */
    public void navigateTo(String route) {
        navigationController.navigateTo(route);
    }
    /**
     Xử lý khi ứng dụng khởi động
     */
    public void onAppStart() {
// Khởi tạo các tài nguyên, kết nối database, etc.
        System.out.println("Ứng dụng khởi động...");
// Điều hướng đến màn hình mặc định
        navigateTo("dashboard");
    }
    /**
     Xử lý khi ứng dụng kết thúc
     */
    public void onAppExit() {
// Dọn dẹp tài nguyên, đóng kết nối, etc.
        System.out.println("Ứng dụng kết thúc...");
    }
    /**
     Xử lý đăng nhập
     @param username Tên đăng nhập
     @param password Mật khẩu
     @return true nếu đăng nhập thành công
     */
    public boolean login(String username, String password) {
// TODO: Implement login logic
        return true;
    }
    /**
     Xử lý đăng xuất
     */
    public void logout() {
// Xóa thông tin người dùng hiện tại
        this.currentUser = null;
// TODO: Implement logout logic
        navigateTo("login");
    }
    /**
     Lưu trữ thông tin chi tiết của một buổi học
     @param session Buổi học cần lưu trữ
     */
    public void setSessionDetail(ClassSession session) {
        this.currentSessionDetail = session;
    }
    /**
     Lấy thông tin chi tiết của buổi học đang được chọn
     @return Thông tin buổi học
     */
    public ClassSession getSessionDetail() {
        return currentSessionDetail;
    }
    /**
     Lấy thông tin chi tiết của buổi học theo ID
     @param id ID của buổi học
     @return Thông tin buổi học hoặc null nếu không tìm thấy
     */
    public ClassSession getClassSessionById(long id) {
// TODO: Implement - Truy vấn dữ liệu từ database dựa vào ID
// Giả lập: Nếu ID trùng với currentSessionDetail thì trả về
        if (currentSessionDetail != null && id == currentSessionDetail.getId()) {
            return currentSessionDetail;
        }
        return null;
    }
    /**
     Xóa một buổi học khỏi hệ thống
     @param id ID của buổi học cần xóa
     @return true nếu xóa thành công
     */
    public boolean deleteClassSession(long id) {
// TODO: Implement - Xóa session từ database
// Giả lập: Luôn trả về thành công
        if (currentSessionDetail != null && id == currentSessionDetail.getId()) {
// Thông báo cho các view rằng session đã bị xóa
            if (navigationController != null) {
                navigationController.getCurrentView().handleSystemMessage("class_deleted", id);
            }
            currentSessionDetail = null;
            return true;
        }
        return false;
    }
    /**
     Lấy instance của MainController
     @return Instance của MainController
     */
    public MainController getMainController() {
        return this;
    }
    /**
     Lấy instance của NavigationController
     @return Instance của NavigationController
     */
    public NavigationController getNavigationController() {
        return this.navigationController;
    }
    /**
     Lấy instance của UI
     @return Instance của UI
     */
    public UI getUI() {
        return this.ui;
    }
    /**
     Lưu thông tin danh sách điểm danh cho buổi học hiện tại
     @param attendances Danh sách điểm danh
     */
    public void setSessionAttendances(List<Attendance> attendances) {
        this.currentSessionAttendances = attendances;
    }
    /**
     Lấy đối tượng AttendanceController
     @return Đối tượng AttendanceController
     */
    public AttendanceController getAttendanceController() {
        return this.attendanceController;
    }
    /**
     Lấy danh sách các ID lớp học mà giáo viên hiện tại phụ trách
     @return Danh sách các ID lớp học
     */
    public List<Long> getTeacherClassIds() {
// Kiểm tra nếu người dùng hiện tại là giáo viên
        if (currentUser instanceof Teacher) {
// TODO: Truy vấn cơ sở dữ liệu để lấy danh sách các lớp của giáo viên
            List<Long> teacherClassIds = new ArrayList<>();
            // Giả lập dữ liệu cho mục đích demo
            teacherClassIds.add(1L);
            teacherClassIds.add(2L);
            teacherClassIds.add(3L);


            return teacherClassIds;


        }
// Trả về danh sách trống nếu không phải giáo viên
        return new ArrayList<>();
    }
    /**
     Thiết lập đối tượng AttendanceController
     @param attendanceController Đối tượng AttendanceController cần thiết lập
     */
    public void setAttendanceController(AttendanceController attendanceController) {
        this.attendanceController = attendanceController;
    }
    /**
     Lấy danh sách điểm danh của buổi học hiện tại
     @return Danh sách điểm danh
     */
    public List<Attendance> getCurrentSessionAttendances() {
        return this.currentSessionAttendances;
    }
    /**
     Lấy ID lớp học của buổi học hiện tại
     @return ID lớp học hoặc -1 nếu không có buổi học nào được chọn
     */
    public long getCurrentClassId() {
        if (currentSessionDetail != null) {
            return currentSessionDetail.getClassId();
        }
        return -1;
    }
    /**
     Đặt người dùng hiện tại cho hệ thống
     @param user Đối tượng Person hoặc các lớp con của nó
     */
    public void setCurrentUser(Object user) {
        if (user instanceof Person) {
            this.currentUser = (Person) user;
            System.out.println("Đã thiết lập người dùng hiện tại: " + currentUser.getName() + " (ID: " + currentUser.getId() + ")");
            // Cập nhật giao diện dựa trên vai trò người dùng nếu UI đã sẵn sàng
            if (ui != null) {
                updateUIBasedOnUserType();
            }


        } else {
            throw new IllegalArgumentException("Đối tượng người dùng phải là kiểu Person hoặc lớp con của Person");
        }
    }
    /**
     Lấy người dùng hiện tại
     @return Đối tượng Person của người dùng hiện tại
     */
    public Person getCurrentUser() {
        return currentUser;
    }
    /**
     Kiểm tra xem người dùng có phải là Admin hay không
     @return true nếu người dùng là Admin, ngược lại false
     */
    public boolean isAdmin() {
        return currentUser instanceof Admin;
    }
    /**
     Kiểm tra xem người dùng có phải là Student hay không
     @return true nếu người dùng là Student, ngược lại false
     */
    public boolean isStudent() {
        return currentUser instanceof Student;
    }
    /**
     Kiểm tra xem người dùng có phải là Teacher hay không
     @return true nếu người dùng là Teacher, ngược lại false
     */
    public boolean isTeacher() {
        return currentUser instanceof Teacher;
    }
    /**
     Kiểm tra xem người dùng có phải là Parent hay không
     @return true nếu người dùng là Parent, ngược lại false
     */
    public boolean isParent() {
        return currentUser instanceof Parent;
    }
    /**
     Cập nhật giao diện dựa trên loại người dùng hiện tại
     */
    private void updateUIBasedOnUserType() {
        if (ui == null || currentUser == null) {
            return;
        }
// Lấy thông tin người dùng
        String userName = currentUser.getName();
        String userDisplayRole = getUserDisplayRole();
// Thiết lập quyền truy cập tương ứng với loại người dùng
        setupAccessPermissions();
    }
    /**
     Lấy tên hiển thị của vai trò người dùng
     @return Tên hiển thị của vai trò
     */
    private String getUserDisplayRole() {
        if (currentUser == null) return "Khách";
        if (currentUser instanceof Admin) return "Quản trị viên";
        if (currentUser instanceof Teacher) return "Giáo viên";
        if (currentUser instanceof Student) return "Học viên";
        if (currentUser instanceof Parent) return "Phụ huynh";
        return "Khách";
    }
    /**
     Thiết lập quyền truy cập dựa trên loại người dùng
     */
    private void setupAccessPermissions() {
// Thiết lập quyền truy cập cho các chức năng dựa vào loại người dùng
        if (currentUser == null) return;
// Tùy thuộc vào UI của bạn, có thể cần thêm phương thức hoặc sửa lại
// Ví dụ:
/*
if (currentUser instanceof Admin) {
ui.showAdminFeatures(true);
ui.showTeacherFeatures(true);
ui.showStudentFeatures(true);
ui.showParentFeatures(true);
} else if (currentUser instanceof Teacher) {
ui.showAdminFeatures(false);
ui.showTeacherFeatures(true);
ui.showStudentFeatures(false);
ui.showParentFeatures(false);
} else if (currentUser instanceof Student) {
ui.showAdminFeatures(false);
ui.showTeacherFeatures(false);
ui.showStudentFeatures(true);
ui.showParentFeatures(false);
} else if (currentUser instanceof Parent) {
ui.showAdminFeatures(false);
ui.showTeacherFeatures(false);
ui.showStudentFeatures(false);
ui.showParentFeatures(true);
}
*/
    }
    /**
     Lấy thông tin của một ClassSession từ cơ sở dữ liệu
     @param sessionId ID của buổi học cần lấy thông tin
     @return Đối tượng ClassSession hoặc null nếu không tìm thấy
     */
    public ClassSession getClassSessionDetails(long sessionId) {
// Kiểm tra cache trước
        if (currentSessionDetail != null && currentSessionDetail.getId() == sessionId) {
            return currentSessionDetail;
        }
// TODO: Thực hiện truy vấn cơ sở dữ liệu ở đây
        return null; // Trả về null nếu không tìm thấy
    }
    /**
     Cập nhật thông tin của một ClassSession
     @param session Thông tin ClassSession cần cập nhật
     @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateClassSession(ClassSession session) {
        if (session == null) return false;
        try {
// TODO: Cập nhật trong cơ sở dữ liệu
            // Cập nhật cache
            if (currentSessionDetail != null && currentSessionDetail.getId() == session.getId()) {
                currentSessionDetail = session;
            }


            return true;


        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật buổi học: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    /**
     Tạo mới một ClassSession
     @param session Thông tin ClassSession cần tạo
     @return ID của ClassSession mới tạo, hoặc -1 nếu thất bại
     */
    public long createClassSession(ClassSession session) {
        if (session == null) return -1;
        try {
// TODO: Thêm vào cơ sở dữ liệu và lấy ID mới
            // Giả lập ID cho môi trường phát triển
            long newId = System.currentTimeMillis();
            session.setId(newId);


            return newId;


        } catch (Exception e) {
            System.err.println("Lỗi khi tạo buổi học mới: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    /**
     Lấy danh sách các ClassSession theo classId
     @param classId ID của lớp học cần lấy danh sách buổi học
     @return Danh sách các buổi học thuộc lớp
     */
    public List<ClassSession> getClassSessionsByClassId(long classId) {
        try {
// TODO: Truy vấn cơ sở dữ liệu để lấy danh sách các buổi học
            // Giả lập dữ liệu cho môi trường phát triển
            List<ClassSession> sessions = new ArrayList<>();
            // Thêm các buổi học giả lập


            return sessions;


        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách buổi học: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    /**
     Hiển thị cảnh báo
     @param alertType Loại cảnh báo
     @param title Tiêu đề cảnh báo
     @param message Nội dung cảnh báo
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
