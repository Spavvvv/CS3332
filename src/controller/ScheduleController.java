package src.controller;

import src.model.ClassSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller quản lý dữ liệu lịch học
 */
public class ScheduleController {

    // Giả lập dữ liệu - trong thực tế sẽ lấy từ database
    private List<ClassSession> sessions;

    public ScheduleController() {
        // Khởi tạo dữ liệu mẫu
        initSampleData();
    }

    /**
     * Lấy danh sách lịch học trong khoảng thời gian
     * @param fromDate Ngày bắt đầu
     * @param toDate Ngày kết thúc
     * @param teacherName Lọc theo tên giáo viên, null nếu không lọc
     * @return Danh sách các buổi học
     */
    public List<ClassSession> getSchedule(LocalDate fromDate, LocalDate toDate, String teacherName) {
        // Filter sessions by date range and teacher (if specified)
        return sessions.stream()
                .filter(session -> session.isInDateRange(fromDate, toDate))
                .filter(session -> teacherName == null || session.isTaughtBy(teacherName))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách nhân sự (giáo viên)
     * @return Danh sách tên giáo viên
     */
    public List<String> getTeachers() {
        // Extract unique teacher names
        return sessions.stream()
                .map(ClassSession::getTeacher)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách nhân sự (giáo viên) - tương thích với code cũ
     * @return Danh sách tên giáo viên
     */
    public List<String> getPersonnel() {
        return getTeachers();
    }

    /**
     * Lấy danh sách các phòng học
     * @return Danh sách phòng học
     */
    public List<String> getRooms() {
        return sessions.stream()
                .map(ClassSession::getRoom)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách các khóa học
     * @return Danh sách tên khóa học
     */
    public List<String> getCourses() {
        return sessions.stream()
                .map(ClassSession::getCourseName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Tìm buổi học theo ID
     * @param sessionId ID buổi học cần tìm
     * @return ClassSession nếu tìm thấy, null nếu không
     */
    public ClassSession getSessionById(long sessionId) {
        return sessions.stream()
                .filter(session -> session.getId() == sessionId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Lấy tất cả các buổi học của một giáo viên
     * @param teacherName Tên giáo viên
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByTeacher(String teacherName) {
        return sessions.stream()
                .filter(session -> session.isTaughtBy(teacherName))
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả các buổi học trong một ngày
     * @param date Ngày cần lấy lịch
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByDate(LocalDate date) {
        return sessions.stream()
                .filter(session -> session.getDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * Xóa một buổi học
     * @param sessionId ID buổi học cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteSession(long sessionId) {
        int initialSize = sessions.size();
        sessions = sessions.stream()
                .filter(session -> session.getId() != sessionId)
                .collect(Collectors.toList());
        return sessions.size() < initialSize;
    }

    /**
     * Thêm buổi học mới
     * @param session Buổi học cần thêm
     * @return true nếu thêm thành công
     */
    public boolean addSession(ClassSession session) {
        // Generate a new ID
        long newId = sessions.stream()
                .mapToLong(ClassSession::getId)
                .max()
                .orElse(0) + 1;
        session.setId(newId);
        return sessions.add(session);
    }

    /**
     * Cập nhật thông tin buổi học
     * @param session Buổi học với thông tin đã cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean updateSession(ClassSession session) {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId() == session.getId()) {
                sessions.set(i, session);
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem có xung đột lịch học không (cùng phòng, cùng giờ, cùng ngày)
     * @param session Buổi học cần kiểm tra
     * @return true nếu có xung đột, false nếu không
     */
    public boolean hasScheduleConflict(ClassSession session) {
        return sessions.stream()
                .anyMatch(s -> s.getId() != session.getId() &&
                        s.getDate().equals(session.getDate()) &&
                        s.getTimeSlot().equals(session.getTimeSlot()) &&
                        s.getRoom().equals(session.getRoom()));
    }

    // Khởi tạo dữ liệu mẫu
    private void initSampleData() {
        sessions = new ArrayList<>();

        // Thêm các buổi học mẫu
        LocalDate today = LocalDate.now();

        sessions.add(new ClassSession(1, "Lập trình Java", "Nguyễn Văn A", "P.201", today, "07:00 - 09:00"));
        sessions.add(new ClassSession(2, "Cơ sở dữ liệu", "Trần Thị B", "P.202", today, "09:15 - 11:15"));
        sessions.add(new ClassSession(3, "Toán rời rạc", "Lê Văn C", "P.301", today, "15:15 - 17:15"));

        sessions.add(new ClassSession(4, "Lập trình Web", "Nguyễn Văn A", "P.205", today.plusDays(1), "13:00 - 15:00"));
        sessions.add(new ClassSession(5, "Mạng máy tính", "Phạm Thị D", "P.302", today.plusDays(1), "15:15 - 17:15"));

        sessions.add(new ClassSession(6, "Lập trình Java", "Nguyễn Văn A", "P.201", today.plusDays(2), "07:00 - 09:00"));
        sessions.add(new ClassSession(7, "Cơ sở dữ liệu", "Trần Thị B", "P.202", today.plusDays(2), "09:15 - 11:15"));

        sessions.add(new ClassSession(8, "Thiết kế UI/UX", "Lê Văn C", "P.401", today.plusDays(3), "17:30 - 19:30"));
        sessions.add(new ClassSession(9, "Kiểm thử phần mềm", "Phạm Thị D", "P.305", today.plusDays(3), "13:00 - 15:00"));

        sessions.add(new ClassSession(10, "Lập trình Mobile", "Nguyễn Văn A", "P.201", today.plusDays(4), "07:00 - 09:00"));
        sessions.add(new ClassSession(11, "Trí tuệ nhân tạo", "Lê Văn C", "P.202", today.plusDays(4), "09:15 - 11:15"));
    }
}
