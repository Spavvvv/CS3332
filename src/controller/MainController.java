package src.controller;

import src.model.ClassSession;
import view.*;
import view.components.*;
import src.model.attendance.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller chính của ứng dụng, quản lý logic nghiệp vụ
 */
public class MainController {
    private UI ui;
    private LoginUI loginUI;
    private RegisterUI registerUI;
    private NavigationController navigationController;
    private ClassSession currentSessionDetail; // Added to store current session detail

    // Field to store attendance records for the current session
    private List<Attendance> currentSessionAttendances;
    private AttendanceController attendanceController;


    /**
     * Constructor với UI và NavigationController
     * @param ui Interface người dùng
     * @param navigationController Controller điều hướng
     */
    public MainController(UI ui, NavigationController navigationController) {
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
     * Khởi tạo controller và đăng ký các màn hình
     */
    private void initialize() {
        registerViews();
        // Thiết lập main controller cho tất cả các view đã đăng ký
        setMainControllerForAllViews();
    }

    /**
     * Đăng ký các view với NavigationController
     */
    private void registerViews() {
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
        navigationController.registerView("learning-reports", new ReportView());
        navigationController.registerView("teaching-statistics", new TeachingStatistics());
        navigationController.registerView("monthly-teaching", new MonthlyTeachingStatistics());
        navigationController.registerView("quarterly-teaching", new QuarterlyTeachingStatistics());
        navigationController.registerView("yearly-teaching", new YearlyTeachingStatistics());
        navigationController.registerView("classrooms", new RoomView());
        navigationController.registerView("holidays", new HolidaysView());
    }

    /**
     * Thiết lập MainController cho tất cả các view đã đăng ký
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
     * Thiết lập MainController cho một view cụ thể
     * @param view View cần thiết lập MainController
     */
    public void setMainControllerForView(BaseScreenView view) {
        if (view != null) {
            view.setMainController(this);
        }
    }

    /**
     * Thực hiện hành động refresh màn hình hiện tại
     */
    public void refreshCurrentView() {
        navigationController.refreshCurrentScreen();
    }

    /**
     * Chuyển đến màn hình được chỉ định
     * @param route Định danh của màn hình
     */
    public void navigateTo(String route) {
        navigationController.navigateTo(route);
    }

    /**
     * Xử lý khi ứng dụng khởi động
     */
    public void onAppStart() {
        // Khởi tạo các tài nguyên, kết nối database, etc.
        System.out.println("Ứng dụng khởi động...");
        // Điều hướng đến màn hình mặc định
        navigateTo("dashboard");
    }

    /**
     * Xử lý khi ứng dụng kết thúc
     */
    public void onAppExit() {
        // Dọn dẹp tài nguyên, đóng kết nối, etc.
        System.out.println("Ứng dụng kết thúc...");
    }

    /**
     * Xử lý đăng nhập
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return true nếu đăng nhập thành công
     */
    public boolean login(String username, String password) {
        // TODO: Implement login logic
        return true;
    }

    /**
     * Xử lý đăng xuất
     */
    public void logout() {
        // TODO: Implement logout logic
        navigateTo("login");
    }

    /**
     * Lưu trữ thông tin chi tiết của một buổi học
     * @param session Buổi học cần lưu trữ
     */
    public void setSessionDetail(ClassSession session) {
        this.currentSessionDetail = session;
    }

    /**
     * Lấy thông tin chi tiết của buổi học đang được chọn
     * @return Thông tin buổi học
     */
    public ClassSession getSessionDetail() {
        return currentSessionDetail;
    }

    /**
     * Lấy thông tin chi tiết của buổi học theo ID
     * @param id ID của buổi học
     * @return Thông tin buổi học hoặc null nếu không tìm thấy
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
     * Xóa một buổi học khỏi hệ thống
     * @param id ID của buổi học cần xóa
     * @return true nếu xóa thành công
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
     * Lấy instance của MainController
     * @return Instance của MainController
     */
    public MainController getMainController() {
        return this;
    }

    /**
     * Lấy instance của NavigationController
     * @return Instance của NavigationController
     */
    public NavigationController getNavigationController() {
        return this.navigationController;
    }

    /**
     * Lấy instance của UI
     * @return Instance của UI
     */
    public UI getUI() {
        return this.ui;
    }

    public void setSessionAttendances(List<Attendance> attendances) {
        // Store the attendance records for the current session
        // This would typically be used when navigating from the attendance list to detailed attendance view
        this.currentSessionAttendances = attendances;
    }

    /**
     * Gets the attendance controller instance
     * @return The AttendanceController instance
     */
    public AttendanceController getAttendanceController() {
        // If you already have an attendance controller as a field, return it
        return this.attendanceController;

        // Or if you need to create it:
        // if (attendanceController == null) {
        //     attendanceController = new AttendanceController();
        // }
        // return attendanceController;
    }

    /**
     * Gets the list of class IDs assigned to the current teacher
     *
     * @return List of class IDs associated with the current teacher
     */
    public List<Long> getTeacherClassIds() {
        // This would typically involve a database query to fetch classes assigned to the current teacher
        // For now, we'll create a placeholder implementation that you can expand later

        List<Long> teacherClassIds = new ArrayList<>();

        // TODO: Replace with actual database query to get teacher's classes
        // Example implementation:
        // 1. Get the current logged-in teacher/user ID
        // 2. Query the database for classes assigned to this teacher
        // 3. Extract and return the class IDs

        // Mock data for demonstration
        // In a real implementation, you would query your database or service layer
        teacherClassIds.add(1L);
        teacherClassIds.add(2L);
        teacherClassIds.add(3L);

        return teacherClassIds;
    }

    /**
     * Sets the AttendanceController for this MainController
     *
     * @param attendanceController The AttendanceController instance
     */
    public void setAttendanceController(AttendanceController attendanceController) {
        this.attendanceController = attendanceController;
    }

    /**
     * Gets the list of attendance records for the current session
     *
     * @return List of attendance records
     */
    public List<Attendance> getCurrentSessionAttendances() {
        return this.currentSessionAttendances;
    }

    /**
     * Gets the current class ID based on the selected session
     *
     * @return The current class ID or -1 if no session is selected
     */
    public long getCurrentClassId() {
        if (currentSessionDetail != null) {
            return currentSessionDetail.getClassId();
        }
        return -1;
    }
}
