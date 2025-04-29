package src.controller;

import view.components.AttendanceScreenView;
import src.model.ClassSession;
import view.components.AbsenceCallView;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình điểm danh kết nối với MainController và NavigationController
 * để quản lý luồng dữ liệu và giao diện người dùng
 */
public class AttendanceController {

    private AttendanceScreenView attendanceView;
    private List<ClassSession> allSessions;
    private Map<Long, Boolean> sessionAttendanceStatus;

    // Reference đến các controller chính
    private MainController mainController;
    private NavigationController navigationController;

    /**
     * Constructor khởi tạo controller với view đã được cung cấp
     * @param attendanceView View điểm danh
     */
    public AttendanceController(AttendanceScreenView attendanceView) {
        this.attendanceView = attendanceView;
        this.allSessions = new ArrayList<>();
        this.sessionAttendanceStatus = new HashMap<>();

        // Lấy reference từ view đã được thiết lập bởi MainController
        this.mainController = attendanceView.getMainController();
        this.navigationController = attendanceView.getNavigationController();

        // Khởi tạo dữ liệu mẫu và thiết lập sự kiện
        initialize();
    }

    /**
     * Khởi tạo dữ liệu và thiết lập sự kiện
     */
    private void initialize() {
        // Tải dữ liệu mẫu
        loadSampleData();

        // Cập nhật giao diện
        updateAttendanceView();

        // Thiết lập các sự kiện
        setupEventHandlers();
    }

    /**
     * Tải dữ liệu mẫu cho màn hình điểm danh
     */
    private void loadSampleData() {
        allSessions.clear();
        sessionAttendanceStatus.clear();

        // Tạo dữ liệu mẫu cho buổi học
        allSessions.add(new ClassSession(1L, "LC Lớp 11A1", "Trần Trung Hải", "A101",
                LocalDate.of(2025, 5, 1), "Thứ 2 - 18:00, Thứ 5 - 18:00"));
        allSessions.add(new ClassSession(2L, "LC Lớp 12A1", "Nguyễn Văn An", "B202",
                LocalDate.of(2025, 5, 2), "Thứ 3 - 18:00, Thứ 7 - 18:00"));
        allSessions.add(new ClassSession(3L, "Toán Cao Cấp", "Lê Quang Huy", "C303",
                LocalDate.of(2025, 5, 3), "Thứ 4 - 19:30, Thứ 6 - 19:30"));
        allSessions.add(new ClassSession(4L, "IELTS 6.5+", "Vũ Nhật Quang", "D404",
                LocalDate.of(2025, 5, 2), "Thứ 5 - 17:30, Chủ nhật - 9:00"));

        // Thiết lập trạng thái điểm danh mặc định (ID chẵn = đã điểm danh, ID lẻ = chưa điểm danh)
        for (ClassSession session : allSessions) {
            sessionAttendanceStatus.put(session.getId(), session.getId() % 2 == 0);
        }
    }

    /**
     * Cập nhật giao diện điểm danh với dữ liệu hiện tại
     */
    private void updateAttendanceView() {
        // Cập nhật danh sách buổi học
        attendanceView.setSessions(allSessions);
    }

    /**
     * Thiết lập các trình xử lý sự kiện cho giao diện
     */
    private void setupEventHandlers() {
        // Tìm kiếm
        attendanceView.getSearchButton().setOnAction(e -> {
            String keyword = attendanceView.getSearchField().getText();
            searchSessions(keyword);
        });

        attendanceView.getSearchField().setOnAction(e -> {
            String keyword = attendanceView.getSearchField().getText();
            searchSessions(keyword);
        });

        // Xuất Excel
        attendanceView.getExportExcelButton().setOnAction(e -> exportExcel());

        // Xem danh sách vắng mặt
        attendanceView.getAttendanceListButton().setOnAction(e -> navigateToAbsenceList());
    }

    /**
     * Tìm kiếm buổi học theo từ khóa
     * @param keyword Từ khóa tìm kiếm
     */
    private void searchSessions(String keyword) {
        List<ClassSession> filteredSessions;

        if (keyword == null || keyword.trim().isEmpty()) {
            filteredSessions = new ArrayList<>(allSessions);
        } else {
            String lowerKeyword = keyword.toLowerCase();
            filteredSessions = allSessions.stream()
                    .filter(session ->
                            session.getCourseName().toLowerCase().contains(lowerKeyword) ||
                                    session.getTeacher().toLowerCase().contains(lowerKeyword) ||
                                    session.getRoom().toLowerCase().contains(lowerKeyword))
                    .collect(Collectors.toList());
        }

        // Cập nhật giao diện với kết quả tìm kiếm
        attendanceView.setSessions(filteredSessions);
    }

    /**
     * Xuất dữ liệu điểm danh ra file Excel
     */
    private void exportExcel() {
        try {
            // TODO: Implement actual Excel export logic here
            System.out.println("Đang xuất dữ liệu điểm danh ra Excel...");

            // Hiển thị thông báo thành công
            attendanceView.showSuccess("Đã xuất dữ liệu điểm danh thành công!");
        } catch (Exception e) {
            attendanceView.showError("Lỗi khi xuất file Excel: " + e.getMessage());
        }
    }

    /**
     * Chuyển đến màn hình danh sách vắng mặt
     */
    private void navigateToAbsenceList() {
        // Sử dụng NavigationController để chuyển đến màn hình danh sách vắng
        navigationController.navigateTo("absence-call");
    }

    /**
     * Xử lý khi chọn một lớp học cụ thể
     * @param sessionId ID của buổi học được chọn
     */
    public void handleClassSelection(Long sessionId) {
        // Tìm buổi học được chọn
        ClassSession selectedSession = allSessions.stream()
                .filter(session -> session.getId() == (sessionId))
                .findFirst()
                .orElse(null);

        if (selectedSession != null) {
            // Lưu thông tin buổi học vào MainController để sử dụng tại các màn hình khác
            mainController.setSessionDetail(selectedSession);

            // Chuyển đến màn hình chi tiết điểm danh
            if (navigationController.routeExists("attendance-detail")) {
                navigationController.navigateTo("attendance-detail");
            } else {
                // Nếu không có route cụ thể, có thể chuyển sang một view chung về danh sách học sinh
                attendanceView.showError("Chức năng xem chi tiết điểm danh chưa được triển khai");
            }
        } else {
            attendanceView.showError("Không tìm thấy thông tin buổi học với ID: " + sessionId);
        }
    }

    /**
     * Lọc buổi học theo ngày trong tuần
     * @param day Ngày cần lọc (Tất cả, Thứ 2, Thứ 3, v.v.)
     */
    public void filterSessionsByDay(String day) {
        List<ClassSession> filteredSessions;

        if ("Tất cả".equals(day)) {
            filteredSessions = new ArrayList<>(allSessions);
        } else {
            filteredSessions = allSessions.stream()
                    .filter(session ->
                            session.getTimeSlot().contains(day) ||
                                    getDayOfWeekFromDate(session.getDate()).equals(day))
                    .collect(Collectors.toList());
        }

        attendanceView.setSessions(filteredSessions);
    }

    /**
     * Lọc buổi học theo trạng thái điểm danh
     * @param status Trạng thái cần lọc (all, marked, unmarked)
     */
    public void filterSessionsByStatus(String status) {
        List<ClassSession> filteredSessions;

        switch (status) {
            case "all":
                filteredSessions = new ArrayList<>(allSessions);
                break;
            case "marked":
                filteredSessions = allSessions.stream()
                        .filter(session -> sessionAttendanceStatus.getOrDefault(session.getId(), false))
                        .collect(Collectors.toList());
                break;
            case "unmarked":
                filteredSessions = allSessions.stream()
                        .filter(session -> !sessionAttendanceStatus.getOrDefault(session.getId(), false))
                        .collect(Collectors.toList());
                break;
            default:
                filteredSessions = new ArrayList<>(allSessions);
        }

        attendanceView.setSessions(filteredSessions);
    }

    /**
     * Lấy tên thứ trong tuần từ ngày
     * @param date Ngày cần lấy thứ
     * @return Tên thứ trong tuần (Thứ 2, Thứ 3, v.v.)
     */
    private String getDayOfWeekFromDate(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        switch (dayOfWeek) {
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            case 7: return "Chủ nhật";
            default: return "";
        }
    }

    /**
     * Cập nhật trạng thái điểm danh cho một buổi học
     * @param sessionId ID của buổi học
     * @param marked Trạng thái điểm danh (true = đã điểm danh, false = chưa điểm danh)
     */
    public void updateAttendanceStatus(Long sessionId, boolean marked) {
        sessionAttendanceStatus.put(sessionId, marked);

        // Cập nhật lại hiển thị
        attendanceView.refreshView();
    }

    /**
     * Lấy danh sách tất cả các buổi học
     * @return Danh sách các buổi học
     */
    public List<ClassSession> getAllSessions() {
        return new ArrayList<>(allSessions);
    }

    /**
     * Kiểm tra trạng thái điểm danh của một buổi học
     * @param sessionId ID của buổi học
     * @return true nếu đã điểm danh, false nếu chưa
     */
    public boolean isSessionMarked(Long sessionId) {
        return sessionAttendanceStatus.getOrDefault(sessionId, false);
    }

    /**
     * Làm mới dữ liệu từ nguồn
     */
    public void refreshData() {
        // Trong một ứng dụng thực tế, đây là nơi bạn sẽ tải lại dữ liệu từ database
        loadSampleData();
        updateAttendanceView();
    }
}
