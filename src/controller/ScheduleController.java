package src.controller;

import src.model.ClassSession;
import src.model.system.schedule.Schedule;
import src.model.system.schedule.RoomSchedule;
import src.model.system.schedule.StudentSchedule;
import src.dao.ScheduleDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller quản lý dữ liệu lịch học
 * Updated to use ScheduleDAO for database operations
 */
public class ScheduleController {
    private static final Logger LOGGER = Logger.getLogger(ScheduleController.class.getName());
    private final ScheduleDAO scheduleDAO;

    // Map to cache ClassSession objects by ID for quick lookups
    private final Map<Long, ClassSession> sessionCache;

    public ScheduleController() {
        this.scheduleDAO = new ScheduleDAO();
        this.sessionCache = new HashMap<>();
        // No need to initialize sample data as we'll use the database
    }

    /**
     * Lấy danh sách lịch học trong khoảng thời gian
     * @param fromDate Ngày bắt đầu
     * @param toDate Ngày kết thúc
     * @param teacherName Lọc theo tên giáo viên, null nếu không lọc
     * @return Danh sách các buổi học
     */
    public List<ClassSession> getSchedule(LocalDate fromDate, LocalDate toDate, String teacherName) {
        try {
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            List<Schedule> schedules = scheduleDAO.findByTimeRange(startDateTime, endDateTime);
            List<ClassSession> sessions = convertToClassSessions(schedules);

            // Filter by teacher if specified
            if (teacherName != null && !teacherName.isEmpty()) {
                sessions = sessions.stream()
                        .filter(session -> session.isTaughtBy(teacherName))
                        .collect(Collectors.toList());
            }

            return sessions;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving schedules", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách nhân sự (giáo viên)
     * @return Danh sách tên giáo viên
     */
    public List<String> getTeachers() {
        try {
            List<Schedule> allSchedules = scheduleDAO.findAll();
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getTeacher)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving teachers", e);
            return new ArrayList<>();
        }
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
        try {
            List<Schedule> allSchedules = scheduleDAO.findAll();
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getRoom)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving rooms", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách các khóa học
     * @return Danh sách tên khóa học
     */
    public List<String> getCourses() {
        try {
            List<Schedule> allSchedules = scheduleDAO.findAll();
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getCourseName)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving courses", e);
            return new ArrayList<>();
        }
    }

    /**
     * Tìm buổi học theo ID
     * @param sessionId ID buổi học cần tìm
     * @return ClassSession nếu tìm thấy, null nếu không
     */
    public ClassSession getSessionById(long sessionId) {
        // Check cache first
        if (sessionCache.containsKey(sessionId)) {
            return sessionCache.get(sessionId);
        }

        try {
            Schedule schedule = scheduleDAO.findById(String.valueOf(sessionId));
            if (schedule != null) {
                ClassSession session = convertToClassSession(schedule);
                sessionCache.put(sessionId, session);
                return session;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving session by ID: " + sessionId, e);
        }
        return null;
    }

    /**
     * Lấy tất cả các buổi học của một giáo viên
     * @param teacherName Tên giáo viên
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByTeacher(String teacherName) {
        try {
            // Since ScheduleDAO doesn't have a direct method to find by teacher,
            // we'll retrieve all schedules and filter them
            List<Schedule> allSchedules = scheduleDAO.findAll();
            return convertToClassSessions(allSchedules).stream()
                    .filter(session -> session.isTaughtBy(teacherName))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving sessions by teacher: " + teacherName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả các buổi học trong một ngày
     * @param date Ngày cần lấy lịch
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByDate(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<Schedule> schedules = scheduleDAO.findByTimeRange(startOfDay, endOfDay);
            return convertToClassSessions(schedules);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving sessions by date: " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Xóa một buổi học
     * @param sessionId ID buổi học cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteSession(long sessionId) {
        try {
            boolean result = scheduleDAO.delete(String.valueOf(sessionId));
            if (result) {
                sessionCache.remove(sessionId);
            }
            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting session: " + sessionId, e);
            return false;
        }
    }

    /**
     * Thêm buổi học mới
     * @param session Buổi học cần thêm
     * @return true nếu thêm thành công
     */
    public boolean addSession(ClassSession session) {
        try {
            // Generate a new ID if not already set
            if (session.getId() <= 0) {
                String uniqueId = UUID.randomUUID().toString();
                session.setId(Long.parseLong(uniqueId.substring(0, 8), 16)); // Convert first 8 chars of UUID to long
            }

            Schedule schedule = convertToSchedule(session);
            boolean result = scheduleDAO.save(schedule);

            if (result) {
                sessionCache.put(session.getId(), session);
            }

            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding session", e);
            return false;
        }
    }

    /**
     * Cập nhật thông tin buổi học
     * @param session Buổi học với thông tin đã cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean updateSession(ClassSession session) {
        try {
            Schedule schedule = convertToSchedule(session);
            boolean result = scheduleDAO.update(schedule);

            if (result) {
                sessionCache.put(session.getId(), session);
            }

            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating session: " + session.getId(), e);
            return false;
        }
    }

    /**
     * Kiểm tra xem có xung đột lịch học không (cùng phòng, cùng giờ, cùng ngày)
     * @param session Buổi học cần kiểm tra
     * @return true nếu có xung đột, false nếu không
     */
    public boolean hasScheduleConflict(ClassSession session) {
        try {
            String[] timeSlotParts = session.getTimeSlot().split(" - ");
            if (timeSlotParts.length != 2) {
                return false;
            }

            LocalDate date = session.getDate();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            LocalTime startTime = LocalTime.parse(timeSlotParts[0], timeFormatter);
            LocalTime endTime = LocalTime.parse(timeSlotParts[1], timeFormatter);

            LocalDateTime startDateTime = date.atTime(startTime);
            LocalDateTime endDateTime = date.atTime(endTime);

            List<Schedule> schedules = scheduleDAO.findByTimeRange(startDateTime, endDateTime);

            // Check for conflicts with the same room
            for (Schedule schedule : schedules) {
                if (schedule instanceof RoomSchedule) {
                    RoomSchedule roomSchedule = (RoomSchedule) schedule;

                    // Skip the current session when checking
                    if (roomSchedule.getId().equals(String.valueOf(session.getId()))) {
                        continue;
                    }

                    ClassSession existingSession = convertToClassSession(roomSchedule);
                    if (existingSession.getRoom().equals(session.getRoom()) &&
                            existingSession.getDate().equals(session.getDate()) &&
                            existingSession.getTimeSlot().equals(session.getTimeSlot())) {
                        return true;
                    }
                }
            }

            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking schedule conflicts", e);
            return false;
        }
    }

    /**
     * Convert a Schedule object to a ClassSession object
     * @param schedule The Schedule object to convert
     * @return Equivalent ClassSession object
     */
    private ClassSession convertToClassSession(Schedule schedule) {
        String courseName = schedule.getName(); // Use schedule name as course name

        // Extract teacher from description (format expected: "Teacher: {name}")
        String teacher = "Unknown";
        if (schedule.getDescription() != null && schedule.getDescription().contains("Teacher:")) {
            teacher = schedule.getDescription().split("Teacher:")[1].trim();
        }

        // For room schedules, use the room ID
        String room = "Unknown";
        if (schedule instanceof RoomSchedule) {
            room = ((RoomSchedule) schedule).getRoomId();
        }

        // Extract date and time slot
        LocalDate date = schedule.getStartTime().toLocalDate();
        String timeSlot = String.format("%s - %s",
                schedule.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                schedule.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));

        long id = Long.parseLong(schedule.getId());

        return new ClassSession(id, courseName, teacher, room, date, timeSlot);
    }

    /**
     * Convert multiple Schedule objects to ClassSession objects
     * @param schedules List of Schedule objects
     * @return List of equivalent ClassSession objects
     */
    private List<ClassSession> convertToClassSessions(List<Schedule> schedules) {
        List<ClassSession> sessions = new ArrayList<>();

        for (Schedule schedule : schedules) {
            ClassSession session = convertToClassSession(schedule);
            sessions.add(session);
            // Update cache
            sessionCache.put(session.getId(), session);
        }

        return sessions;
    }

    /**
     * Convert a ClassSession object to a Schedule object (specifically a RoomSchedule)
     * @param session The ClassSession to convert
     * @return Equivalent Schedule object
     */
    private Schedule convertToSchedule(ClassSession session) {
        String id = String.valueOf(session.getId());
        String name = session.getCourseName();

        // Store teacher in description
        String description = "Teacher: " + session.getTeacher();

        // Parse time slot
        String[] timeSlotParts = session.getTimeSlot().split(" - ");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime startTime = LocalTime.parse(timeSlotParts[0], timeFormatter);
        LocalTime endTime = LocalTime.parse(timeSlotParts[1], timeFormatter);

        LocalDateTime startDateTime = session.getDate().atTime(startTime);
        LocalDateTime endDateTime = session.getDate().atTime(endTime);

        // Create a RoomSchedule since ClassSession has room information
        return new RoomSchedule(
                id,
                name,
                description,
                startDateTime,
                endDateTime,
                session.getRoom(),
                30,  // Default capacity, adjust as needed
                "CLASSROOM"  // Default room type, adjust as needed
        );
    }
}
