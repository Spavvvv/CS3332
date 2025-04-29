package src.controller;

import src.model.ClassSession;
import src.model.attendance.Attendance;
import src.model.person.Student;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller quản lý chức năng điểm danh
 * Xử lý nghiệp vụ liên quan đến việc điểm danh học sinh và quản lý dữ liệu điểm danh
 */
public class AttendanceController {

    // Lưu trữ dữ liệu điểm danh tạm thời (trong thực tế sẽ lưu vào database)
    private Map<Long, List<Attendance>> sessionAttendanceMap;

    // Tham chiếu đến MainController để lấy dữ liệu
    private MainController mainController;

    /**
     * Constructor mặc định
     */
    public AttendanceController() {
        sessionAttendanceMap = new HashMap<>();
    }

    /**
     * Constructor với MainController
     * @param mainController Controller chính của ứng dụng
     */
    public AttendanceController(MainController mainController) {
        this.mainController = mainController;
        sessionAttendanceMap = new HashMap<>();
    }

    /**
     * Thiết lập MainController
     * @param mainController Controller chính của ứng dụng
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Lấy danh sách điểm danh cho một buổi học
     * @param session Buổi học cần lấy dữ liệu
     * @return Danh sách điểm danh
     */
    public List<Attendance> getAttendanceForSession(ClassSession session) {
        if (session == null) return new ArrayList<>();

        Long sessionId = session.getId();

        // Nếu có dữ liệu điểm danh rồi thì trả về
        if (sessionAttendanceMap.containsKey(sessionId)) {
            return sessionAttendanceMap.get(sessionId);
        } else {
            List<Attendance> newAttendanceList = createAttendanceListFromClassSession(session);
            sessionAttendanceMap.put(sessionId, newAttendanceList);
            return newAttendanceList;
        }
    }

    /**
     * Tạo danh sách điểm danh dựa trên buổi học
     * @param session Buổi học cần tạo danh sách điểm danh
     * @return Danh sách điểm danh
     */
    private List<Attendance> createAttendanceListFromClassSession(ClassSession session) {
        List<Attendance> attendanceList = new ArrayList<>();

        if (session == null) {
            return attendanceList;
        }

        // Lấy thông tin buổi học đầy đủ từ MainController
        ClassSession fullSession = session;
        if (mainController != null) {
            ClassSession tempSession = mainController.getSessionDetail();
            if (tempSession != null && tempSession.getId() == session.getId()) {
                fullSession = tempSession;
            }
        }

        // Lấy danh sách học sinh từ buổi học
        List<Student> students = fullSession.getStudents();
        if (students == null || students.isEmpty()) {
            return attendanceList;
        }

        // Tạo dữ liệu điểm danh dựa trên danh sách học sinh
        for (Student student : students) {
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setSession(session);
            attendance.setPresent(true); // Mặc định tất cả học sinh đều có mặt
            attendance.setHasPermission(false);
            attendance.setCalled(false);
            attendance.setNote("");

            attendanceList.add(attendance);
        }

        return attendanceList;
    }

    /**
     * Lưu dữ liệu điểm danh cho một buổi học
     * @param session Buổi học cần lưu dữ liệu
     * @param attendanceList Danh sách điểm danh
     * @return true nếu lưu thành công
     */
    public boolean saveAttendanceData(ClassSession session, List<Attendance> attendanceList) {
        if (session == null || attendanceList == null) return false;

        try {
            // Cập nhật ngày điểm danh
            for (Attendance attendance : attendanceList) {
                if (attendance.getSession() == null) {
                    attendance.setSession(session);
                }
            }

            // Lưu vào map
            sessionAttendanceMap.put(session.getId(), attendanceList);

            // Trong thực tế sẽ lưu vào database
            System.out.println("Đã lưu dữ liệu điểm danh cho buổi " + session.getId() + " - " + attendanceList.size() + " học sinh");

            return true;
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu dữ liệu điểm danh: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi thông báo cho học sinh vắng mặt
     * @param attendanceList Danh sách điểm danh
     * @return Số lượng thông báo đã gửi
     */
    public int sendAbsenceNotifications(List<Attendance> attendanceList) {
        if (attendanceList == null || attendanceList.isEmpty()) return 0;

        // Lọc danh sách học sinh vắng mặt chưa được thông báo
        List<Attendance> absentStudents = attendanceList.stream()
                .filter(a -> !a.isPresent() && !a.isCalled())
                .collect(Collectors.toList());

        // Đánh dấu đã thông báo
        for (Attendance attendance : absentStudents) {
            attendance.setCalled(true);
        }

        // Trong thực tế sẽ gửi thông báo qua SMS, email, v.v.
        System.out.println("Đã gửi thông báo cho " + absentStudents.size() + " học sinh vắng mặt");

        return absentStudents.size();
    }

    /**
     * Lấy thông tin điểm danh của một học sinh cho một buổi học
     * @param sessionId ID của buổi học
     * @param studentId ID của học sinh
     * @return Thông tin điểm danh hoặc null nếu không tìm thấy
     */
    public Attendance getStudentAttendance(long sessionId, String studentId) {
        if (!sessionAttendanceMap.containsKey(sessionId)) return null;

        List<Attendance> attendanceList = sessionAttendanceMap.get(sessionId);
        for (Attendance attendance : attendanceList) {
            if (attendance.getStudent().getId().equals(studentId)) {
                return attendance;
            }
        }

        return null;
    }

    /**
     * Cập nhật thông tin điểm danh của một học sinh
     * @param sessionId ID của buổi học
     * @param studentId ID của học sinh
     * @param isPresent Trạng thái hiện diện (true = có mặt, false = vắng)
     * @param hasPermission Có phép hay không
     * @param note Ghi chú
     * @return true nếu cập nhật thành công
     */
    public boolean updateStudentAttendance(long sessionId, String studentId, boolean isPresent, boolean hasPermission, String note) {
        Attendance attendance = getStudentAttendance(sessionId, studentId);
        if (attendance == null) return false;

        attendance.setPresent(isPresent);
        attendance.setHasPermission(hasPermission);
        attendance.setNote(note);

        return true;
    }

    /**
     * Lấy số lượng học sinh vắng mặt trong một buổi học
     * @param sessionId ID của buổi học
     * @return Số lượng học sinh vắng mặt
     */
    public int getAbsentCount(long sessionId) {
        if (!sessionAttendanceMap.containsKey(sessionId)) return 0;

        List<Attendance> attendanceList = sessionAttendanceMap.get(sessionId);
        int count = 0;

        for (Attendance attendance : attendanceList) {
            if (!attendance.isPresent()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Lấy danh sách học sinh vắng mặt trong một buổi học
     * @param sessionId ID của buổi học
     * @return Danh sách học sinh vắng mặt
     */
    public List<Student> getAbsentStudents(long sessionId) {
        if (!sessionAttendanceMap.containsKey(sessionId)) return new ArrayList<>();

        List<Attendance> attendanceList = sessionAttendanceMap.get(sessionId);
        List<Student> absentStudents = new ArrayList<>();

        for (Attendance attendance : attendanceList) {
            if (!attendance.isPresent()) {
                absentStudents.add(attendance.getStudent());
            }
        }

        return absentStudents;
    }

    /**
     * Lấy thông tin tỷ lệ điểm danh cho một buổi học
     * @param sessionId ID của buổi học
     * @return Tỷ lệ học sinh có mặt (0.0 - 1.0)
     */
    public double getAttendanceRate(long sessionId) {
        if (!sessionAttendanceMap.containsKey(sessionId)) return 0.0;

        List<Attendance> attendanceList = sessionAttendanceMap.get(sessionId);
        if (attendanceList.isEmpty()) return 0.0;

        int presentCount = 0;
        for (Attendance attendance : attendanceList) {
            if (attendance.isPresent()) {
                presentCount++;
            }
        }

        return (double) presentCount / attendanceList.size();
    }

    /**
     * Lấy danh sách học sinh trong lớp từ thông tin buổi học
     * @param classId ID của lớp
     * @return Danh sách học sinh
     */
    public List<Student> getStudentsInClass(long classId) {
        // Vì chưa có chức năng lấy danh sách học sinh theo classId từ MainController
        // nên sẽ trích xuất từ sessionAttendanceMap nếu có buổi học của lớp đó
        List<Student> students = new ArrayList<>();

        // Duyệt qua tất cả các buổi học
        for (Map.Entry<Long, List<Attendance>> entry : sessionAttendanceMap.entrySet()) {
            Long sessionId = entry.getKey();
            List<Attendance> attendanceList = entry.getValue();

            if (!attendanceList.isEmpty()) {
                ClassSession session = attendanceList.get(0).getSession();

                // Kiểm tra xem buổi học có thuộc lớp này không
                if (session != null && session.getClassId() == classId) {
                    // Lấy danh sách học sinh từ buổi học này
                    for (Attendance attendance : attendanceList) {
                        Student student = attendance.getStudent();

                        // Kiểm tra xem học sinh đã có trong danh sách chưa
                        boolean exists = students.stream()
                                .anyMatch(s -> s.getId().equals(student.getId()));

                        if (!exists) {
                            students.add(student);
                        }
                    }

                    // Đã có danh sách học sinh, không cần kiểm tra thêm
                    if (!students.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return students;
    }

    /**
     * Lấy dữ liệu điểm danh trong khoảng thời gian
     * @param classId ID của lớp học
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách buổi học có dữ liệu điểm danh
     */
    public Map<Long, List<Attendance>> getAttendanceDataInRange(long classId, LocalDate startDate, LocalDate endDate) {
        Map<Long, List<Attendance>> result = new HashMap<>();

        // Lặp qua tất cả các buổi học đã có dữ liệu điểm danh
        for (Map.Entry<Long, List<Attendance>> entry : sessionAttendanceMap.entrySet()) {
            Long sessionId = entry.getKey();
            List<Attendance> attendanceList = entry.getValue();

            if (!attendanceList.isEmpty()) {
                ClassSession session = attendanceList.get(0).getSession();

                // Kiểm tra xem buổi học có thuộc lớp này và nằm trong khoảng thời gian không
                if (session != null && session.getClassId() == classId) {
                    LocalDate sessionDate = session.getDate();

                    if ((sessionDate.isEqual(startDate) || sessionDate.isAfter(startDate)) &&
                            (sessionDate.isEqual(endDate) || sessionDate.isBefore(endDate))) {
                        result.put(sessionId, attendanceList);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Thống kê số buổi vắng học của từng học sinh trong một lớp
     * @param classId ID của lớp học
     * @return Map chứa ID học sinh và số buổi vắng học
     */
    public Map<String, Integer> getAbsenceStatistics(long classId) {
        // Lấy danh sách học sinh trong lớp
        List<Student> students = getStudentsInClass(classId);
        Map<String, Integer> absenceCount = new HashMap<>();

        // Khởi tạo số buổi vắng mặt ban đầu là 0 cho mỗi học sinh
        for (Student student : students) {
            absenceCount.put(student.getId(), 0);
        }

        // Lặp qua tất cả các buổi học đã có dữ liệu điểm danh
        for (Map.Entry<Long, List<Attendance>> entry : sessionAttendanceMap.entrySet()) {
            Long sessionId = entry.getKey();
            List<Attendance> attendanceList = entry.getValue();

            if (!attendanceList.isEmpty()) {
                ClassSession session = attendanceList.get(0).getSession();

                // Kiểm tra xem buổi học có thuộc lớp này không
                if (session != null && session.getClassId() == classId) {
                    for (Attendance attendance : attendanceList) {
                        String studentId = attendance.getStudent().getId();

                        if (!attendance.isPresent() && absenceCount.containsKey(studentId)) {
                            absenceCount.put(studentId, absenceCount.get(studentId) + 1);
                        }
                    }
                }
            }
        }

        return absenceCount;
    }

    /**
     * Kiểm tra học sinh có nguy cơ nghỉ học (vắng mặt nhiều buổi liên tiếp)
     * @param classId ID của lớp học
     * @param threshold Ngưỡng số buổi vắng mặt liên tiếp
     * @return Danh sách học sinh có nguy cơ
     */
    public List<Student> getStudentsAtRisk(long classId, int threshold) {
        List<Student> studentsAtRisk = new ArrayList<>();

        // Lấy danh sách học sinh trong lớp
        List<Student> students = getStudentsInClass(classId);
        if (students.isEmpty()) {
            return studentsAtRisk;
        }

        Map<String, Integer> consecutiveAbsences = new HashMap<>();

        // Khởi tạo map với số buổi vắng mặt liên tiếp ban đầu là 0
        for (Student student : students) {
            consecutiveAbsences.put(student.getId(), 0);
        }

        // Lấy danh sách buổi học và sắp xếp theo thời gian
        List<ClassSession> sessions = new ArrayList<>();

        // Lặp qua tất cả các buổi học đã có dữ liệu điểm danh
        for (Map.Entry<Long, List<Attendance>> entry : sessionAttendanceMap.entrySet()) {
            Long sessionId = entry.getKey();
            List<Attendance> attendanceList = entry.getValue();

            if (!attendanceList.isEmpty()) {
                ClassSession session = attendanceList.get(0).getSession();

                // Kiểm tra xem buổi học có thuộc lớp này không
                if (session != null && session.getClassId() == classId) {
                    sessions.add(session);
                }
            }
        }

        if (sessions.isEmpty()) {
            return studentsAtRisk;
        }

        // Sắp xếp theo ngày
        sessions.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        // Duyệt qua tất cả các buổi học và đếm số buổi vắng mặt liên tiếp
        for (ClassSession session : sessions) {
            Long sessionId = session.getId();

            if (sessionAttendanceMap.containsKey(sessionId)) {
                List<Attendance> attendanceList = sessionAttendanceMap.get(sessionId);

                for (Attendance attendance : attendanceList) {
                    String studentId = attendance.getStudent().getId();

                    if (!attendance.isPresent()) {
                        // Tăng số buổi vắng mặt liên tiếp
                        if (consecutiveAbsences.containsKey(studentId)) {
                            consecutiveAbsences.put(studentId, consecutiveAbsences.get(studentId) + 1);

                            // Kiểm tra nếu số buổi vắng mặt liên tiếp vượt ngưỡng
                            if (consecutiveAbsences.get(studentId) >= threshold) {
                                // Thêm học sinh vào danh sách nguy cơ nếu chưa có
                                boolean alreadyAdded = studentsAtRisk.stream()
                                        .anyMatch(s -> s.getId().equals(studentId));

                                if (!alreadyAdded) {
                                    studentsAtRisk.add(attendance.getStudent());
                                }
                            }
                        }
                    } else {
                        // Reset số buổi vắng mặt liên tiếp
                        if (consecutiveAbsences.containsKey(studentId)) {
                            consecutiveAbsences.put(studentId, 0);
                        }
                    }
                }
            }
        }

        return studentsAtRisk;
    }

    /**
     * Kiểm tra học sinh có tỷ lệ đi học thấp
     * @param classId ID của lớp học
     * @param minAttendanceRate Tỷ lệ đi học tối thiểu
     * @return Danh sách học sinh có tỷ lệ đi học thấp
     */
    public List<Student> getStudentsWithLowAttendance(long classId, double minAttendanceRate) {
        List<Student> studentsWithLowAttendance = new ArrayList<>();

        // Lấy danh sách học sinh trong lớp
        List<Student> students = getStudentsInClass(classId);
        if (students.isEmpty()) {
            return studentsWithLowAttendance;
        }

        Map<String, Integer> totalSessions = new HashMap<>();
        Map<String, Integer> presentSessions = new HashMap<>();

        // Khởi tạo maps
        for (Student student : students) {
            totalSessions.put(student.getId(), 0);
            presentSessions.put(student.getId(), 0);
        }

        // Lặp qua tất cả các buổi học đã có dữ liệu điểm danh
        for (Map.Entry<Long, List<Attendance>> entry : sessionAttendanceMap.entrySet()) {
            Long sessionId = entry.getKey();
            List<Attendance> attendanceList = entry.getValue();

            if (!attendanceList.isEmpty()) {
                ClassSession session = attendanceList.get(0).getSession();

                // Kiểm tra xem buổi học có thuộc lớp này không
                if (session != null && session.getClassId() == classId) {
                    for (Attendance attendance : attendanceList) {
                        String studentId = attendance.getStudent().getId();

                        if (totalSessions.containsKey(studentId)) {
                            totalSessions.put(studentId, totalSessions.get(studentId) + 1);

                            if (attendance.isPresent()) {
                                presentSessions.put(studentId, presentSessions.get(studentId) + 1);
                            }
                        }
                    }
                }
            }
        }

        // Kiểm tra học sinh có tỷ lệ đi học thấp
        for (Student student : students) {
            String studentId = student.getId();
            int total = totalSessions.getOrDefault(studentId, 0);

            if (total > 0) {
                double attendanceRate = (double) presentSessions.getOrDefault(studentId, 0) / total;

                if (attendanceRate < minAttendanceRate) {
                    studentsWithLowAttendance.add(student);
                }
            }
        }

        return studentsWithLowAttendance;
    }

    /**
     * Bổ sung phương thức isNotified (alias cho isCalled)
     */
    public boolean isNotified(Attendance attendance) {
        return attendance.isCalled();
    }

    /**
     * Bổ sung phương thức setNotified (alias cho setCalled)
     */
    public void setNotified(Attendance attendance, boolean notified) {
        attendance.setCalled(notified);
    }
}
