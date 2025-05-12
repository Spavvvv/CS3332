package src.controller;

import src.model.ClassSession; // Requires ClassSession model to use String IDs
import src.model.system.schedule.Schedule; // Requires Schedule model to use String IDs
import src.model.system.schedule.RoomSchedule; // Requires RoomSchedule model to use String IDs
import src.dao.ScheduleDAO; // Requires ScheduleDAO to use String IDs

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
 * Controller quản lý dữ liệu lịch học (Class Sessions)
 * Focuses only on schedule-related operations, interacting with ScheduleDAO.
 * Assumes Schedule model (and its subclasses like RoomSchedule) uses String IDs.
 * Assumes ClassSession model uses String ID.
 */
public class ScheduleController {
    private static final Logger LOGGER = Logger.getLogger(ScheduleController.class.getName());
    private final ScheduleDAO scheduleDAO;

    // Map to cache ClassSession objects by ID (using String for ID)
    private final Map<String, ClassSession> sessionCache; // Changed key type to String

    public ScheduleController() { // Constructor might throw SQLException if DAO initialization fails
        this.scheduleDAO = new ScheduleDAO();
        this.sessionCache = new HashMap<>();
        // Only initialize DAOs strictly needed for schedule management if any besides ScheduleDAO
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
            if (fromDate == null || toDate == null) {
                LOGGER.log(Level.WARNING, "Attempted to get schedule with null dates.");
                return new ArrayList<>();
            }
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            // Ensure endDateTime includes the whole day of toDate
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            // ScheduleDAO.findByTimeRange must accept LocalDateTime
            List<Schedule> schedules = scheduleDAO.findByTimeRange(startDateTime, endDateTime);
            List<ClassSession> sessions = convertToClassSessions(schedules);

            // Filter by teacher if specified. Note: Filtering by teacher here requires loading all schedules in range first,
            // which might be inefficient. A findByTimeRangeAndTeacher method in ScheduleDAO would be better.
            if (teacherName != null && !teacherName.trim().isEmpty()) {
                sessions = sessions.stream()
                        .filter(session -> session != null && session.isTaughtBy(teacherName)) // Requires isTaughtBy on ClassSession and null check
                        .collect(Collectors.toList());
            }

            return sessions;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving schedules", e);
            return new ArrayList<>();
        } catch (Exception e) { // Catch other potential exceptions during processing
            LOGGER.log(Level.SEVERE, "Unexpected error getting schedule", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách nhân sự (giáo viên) liên quan đến lịch học.
     * Lưu ý: Lấy giáo viên từ tất cả lịch học có thể không hiệu quả.
     * Cần phương thức riêng trong DAO hoặc TeacherController.
     * @return Danh sách tên giáo viên
     */
    public List<String> getTeachers() {
        try {
            // Retrieving all schedules might be inefficient for large datasets.
            // A dedicated DAO method to get distinct teachers from schedules would be better.
            List<Schedule> allSchedules = scheduleDAO.findAll();
            // Using convertToClassSessions to utilize caching and consistent object representation
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getTeacher) // Requires getTeacher on ClassSession
                    .filter(teacher -> teacher != null && !teacher.trim().isEmpty()) // Filter out empty/null teachers
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving teachers from schedules", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting teachers from schedules", e);
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
     * Lấy danh sách các phòng học liên quan đến lịch học.
     * Lưu ý: Lấy phòng từ tất cả lịch học có thể không hiệu quả.
     * Cần phương thức riêng trong DAO hoặc RoomController.
     * @return Danh sách phòng học
     */
    public List<String> getRooms() {
        try {
            // Retrieving all schedules might be inefficient for large datasets.
            // A dedicated DAO method to get distinct rooms from schedules would be better.
            List<Schedule> allSchedules = scheduleDAO.findAll();
            // Using convertToClassSessions to utilize caching and consistent object representation
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getRoom) // Requires getRoom on ClassSession
                    .filter(room -> room != null && !room.trim().isEmpty()) // Filter out empty/null rooms
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving rooms from schedules", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting rooms from schedules", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách các khóa học liên quan đến lịch học.
     * Lưu ý: Lấy khóa học từ tất cả lịch học có thể không hiệu quả.
     * Cần phương thức riêng trong DAO hoặc CourseController.
     * @return Danh sách tên khóa học
     */
    public List<String> getCourses() {
        try {
            // Retrieving all schedules might be inefficient for large datasets.
            // A dedicated DAO method to get distinct courses from schedules would be better.
            List<Schedule> allSchedules = scheduleDAO.findAll();
            // Using convertToClassSessions to utilize caching and consistent object representation
            return convertToClassSessions(allSchedules).stream()
                    .map(ClassSession::getCourseName) // Requires getCourseName on ClassSession
                    .filter(course -> course != null && !course.trim().isEmpty()) // Filter out empty/null course names
                    .distinct()
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving courses from schedules", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting courses from schedules", e);
            return new ArrayList<>();
        }
    }

    /**
     * Tìm buổi học theo ID (using String ID)
     * @param sessionId ID buổi học cần tìm (String)
     * @return ClassSession nếu tìm thấy, null nếu không
     */
    public ClassSession getSessionById(String sessionId) { // Changed parameter type to String
        // Check cache first
        if (sessionId != null && !sessionId.trim().isEmpty() && sessionCache.containsKey(sessionId)) {
            return sessionCache.get(sessionId);
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get session with null or empty ID");
            return null;
        }


        try {
            // ScheduleDAO.findById must accept String
            Schedule schedule = scheduleDAO.findById(sessionId); // Pass String ID directly
            if (schedule != null) {
                ClassSession session = convertToClassSession(schedule);
                // Put in cache using String ID
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(sessionId, session); // Uses String key
                    return session;
                } else if (session != null) {
                    LOGGER.log(Level.WARNING, "Converted schedule to ClassSession with null or empty ID: " + sessionId);
                    return session; // Return potentially incomplete session
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving session by ID: " + sessionId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting session by ID: " + sessionId, e);
        }
        return null;
    }

    /**
     * Lấy tất cả các buổi học của một giáo viên
     * Lưu ý: Lấy tất cả lịch học rồi lọc theo giáo viên không hiệu quả.
     * Cần phương thức riêng trong ScheduleDAO.
     * @param teacherName Tên giáo viên
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByTeacher(String teacherName) {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            return new ArrayList<>(); // Return empty list if teacher name is invalid
        }
        try {
            // Since ScheduleDAO doesn't have a direct method to find by teacher,
            // we'll retrieve all schedules and filter them. This can be inefficient for large datasets.
            // Consider adding a findByTeacher method to ScheduleDAO if this is a frequent operation.
            List<Schedule> allSchedules = scheduleDAO.findAll();
            // Using convertToClassSessions to utilize caching and consistent object representation
            return convertToClassSessions(allSchedules).stream()
                    .filter(session -> session != null && session.isTaughtBy(teacherName)) // Requires isTaughtBy
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving sessions by teacher: " + teacherName, e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by teacher: " + teacherName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả các buổi học trong một ngày
     * @param date Ngày cần lấy lịch
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByDate(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to get sessions with null date.");
            return new ArrayList<>(); // Return empty list if date is null
        }
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            // ScheduleDAO.findByTimeRange must accept LocalDateTime
            List<Schedule> schedules = scheduleDAO.findByTimeRange(startOfDay, endOfDay);
            // Using convertToClassSessions to utilize caching and consistent object representation
            return convertToClassSessions(schedules);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving sessions by date: " + date, e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by date: " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Xóa một buổi học (using String ID)
     * @param sessionId ID buổi học cần xóa (String)
     * @return true nếu xóa thành công
     */
    public boolean deleteSession(String sessionId) { // Changed parameter type to String
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete session with null or empty ID");
            return false;
        }
        try {
            // ScheduleDAO.delete must accept String
            boolean result = scheduleDAO.delete(sessionId); // Pass String ID directly
            if (result) {
                // Remove from cache using String ID
                sessionCache.remove(sessionId); // Uses String key
            }
            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting session: " + sessionId, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting session: " + sessionId, e);
            return false;
        }
    }

    /**
     * Thêm buổi học mới
     * @param session Buổi học cần thêm (Requires ClassSession model to use String ID)
     * @return true nếu thêm thành công
     */
    public boolean addSession(ClassSession session) {
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to add a null session");
            return false;
        }
        try {
            // Generate a new String ID if not already set or if it's empty
            if (session.getId() == null || session.getId().trim().isEmpty()) {
                session.setId(UUID.randomUUID().toString());
            } else {
                // If ID is already set, log a warning but proceed.
                // Consider adding a check here to see if the ID already exists in the DB
                // to prevent primary key violations, although UUIDs make this unlikely.
                LOGGER.log(Level.INFO, "Adding session with pre-existing ID: " + session.getId());
            }

            // Convert ClassSession to Schedule (RoomSchedule) - requires ClassSession.getId() to return String
            Schedule schedule = convertToSchedule(session);
            if (schedule == null) {
                LOGGER.log(Level.SEVERE, "Failed to convert ClassSession to Schedule for adding.");
                return false;
            }
            // ScheduleDAO.save must accept Schedule object with String ID
            boolean result = scheduleDAO.save(schedule);

            if (result) {
                // Put in cache using String ID
                if (session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session); // Uses String key
                } else {
                    LOGGER.log(Level.WARNING, "Added session with null or empty ID, cannot cache.");
                }
            }

            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding session", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error adding session", e);
            return false;
        }
    }

    private Schedule convertToSchedule(ClassSession session) {
        if (session == null) {
            return null;
        }

        // Ensure session.getId() returns String
        String id = session.getId(); // Get String ID
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "ClassSession with null or empty ID encountered during conversion to Schedule.");
            // Decide how to handle ClassSessions without IDs. Cannot create a Schedule without an ID.
            return null;
        }

        String name = session.getCourseName();
        if (name == null) name = "Unknown Course";


        // Store teacher in description
        String description = "Teacher: " + (session.getTeacher() != null ? session.getTeacher() : "Unknown");

        // Parse time slot
        String timeSlot = session.getTimeSlot();
        LocalTime startTime = LocalTime.MIDNIGHT; // Default if parsing fails
        LocalTime endTime = LocalTime.MIDNIGHT;   // Default if parsing fails

        if (timeSlot != null && timeSlot.contains(" - ")) {
            String[] timeSlotParts = timeSlot.split(" - ");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            try {
                startTime = LocalTime.parse(timeSlotParts[0].trim(), timeFormatter);
            } catch (java.time.format.DateTimeParseException e) {
                LOGGER.log(Level.WARNING, "Invalid start time format in time slot for session ID " + id + ": " + timeSlot, e);
            }
            try {
                endTime = LocalTime.parse(timeSlotParts[1].trim(), timeFormatter);
            } catch (java.time.format.DateTimeParseException e) {
                LOGGER.log(Level.WARNING, "Invalid end time format in time slot for session ID " + id + ": " + timeSlot, e);
            }
        } else {
            LOGGER.log(Level.WARNING, "Invalid or null time slot format for session ID " + id + ": " + timeSlot);
        }


        LocalDate date = session.getDate();
        if (date == null) {
            LOGGER.log(Level.WARNING, "Session has null date for session ID " + id + ". Using current date.");
            date = LocalDate.now(); // Default to today if date is null
        }

        LocalDateTime startDateTime = date.atTime(startTime);
        LocalDateTime endDateTime = date.atTime(endTime);

        // Ensure session.getRoom() returns String
        String room = session.getRoom() != null ? session.getRoom() : "Unknown";
        if (room.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Session has empty room ID for session ID " + id + ". Using 'Unknown'.");
            room = "Unknown";
        }


        // Create a RoomSchedule since ClassSession has room information
        // RoomSchedule constructor must accept String ID, String name, String description, LocalDateTime startTime, LocalDateTime endTime, String roomId, int capacity, String roomType
        try {
            // Note: Capacity and roomType are hardcoded here. You might need to get these from the ClassSession or elsewhere.
            return new RoomSchedule(
                    id, // Use String ID
                    name,
                    description,
                    startDateTime,
                    endDateTime,
                    room, // Use String room
                    30,  // Default capacity, adjust as needed
                    "CLASSROOM"  // Default room type, adjust as needed
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating RoomSchedule from ClassSession object with ID: " + id, e);
            return null; // Return null if RoomSchedule creation fails
        }
    }


    /**
     * Cập nhật thông tin buổi học (using String ID)
     * @param session Buổi học với thông tin đã cập nhật (Requires ClassSession model to use String ID)
     * @return true nếu cập nhật thành công
     */
    public boolean updateSession(ClassSession session) {
        if (session == null || session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null session or session with null/empty ID");
            return false;
        }
        try {
            // Convert ClassSession to Schedule (RoomSchedule) - requires ClassSession.getId() to return String
            Schedule schedule = convertToSchedule(session);
            if (schedule == null) {
                LOGGER.log(Level.SEVERE, "Failed to convert ClassSession to Schedule for updating session ID: " + session.getId());
                return false;
            }
            // ScheduleDAO.update must accept Schedule object with String ID
            boolean result = scheduleDAO.update(schedule);

            if (result) {
                // Update cache using String ID
                sessionCache.put(session.getId(), session); // Uses String key
            }

            return result;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating session: " + session.getId(), e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating session: " + session.getId(), e);
            return false;
        }
    }

    /**
     * Kiểm tra xem có xung đột lịch học không (cùng phòng, cùng giờ, cùng ngày) (using String ID)
     * @param session Buổi học cần kiểm tra (Requires ClassSession model to use String ID)
     * @return true nếu có xung đột, false nếu không
     */
    public boolean hasScheduleConflict(ClassSession session) {
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to check conflict for a null session");
            return false; // No conflict with a null session
        }
        try {
            String timeSlot = session.getTimeSlot();
            if (timeSlot == null) {
                LOGGER.log(Level.WARNING, "Session has null time slot: " + session.getId());
                return false; // Cannot check conflict with null time slot
            }

            String[] timeSlotParts = timeSlot.split(" - ");
            if (timeSlotParts.length != 2) {
                LOGGER.log(Level.WARNING, "Invalid time slot format for session ID " + session.getId() + ": " + timeSlot);
                return false; // Cannot check conflict with invalid time slot
            }

            LocalDate date = session.getDate();
            if (date == null) {
                LOGGER.log(Level.WARNING, "Session has null date: " + session.getId());
                return false; // Cannot check conflict with null date
            }
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime startTime;
            LocalTime endTime;

            try {
                startTime = LocalTime.parse(timeSlotParts[0].trim(), timeFormatter);
                endTime = LocalTime.parse(timeSlotParts[1].trim(), timeFormatter);
            } catch (java.time.format.DateTimeParseException e) {
                LOGGER.log(Level.WARNING, "Invalid time format in time slot for session ID " + session.getId() + ": " + timeSlot, e);
                return false; // Cannot check conflict with invalid time format
            }


            LocalDateTime startDateTime = date.atTime(startTime);
            LocalDateTime endDateTime = date.atTime(endTime);

            // ScheduleDAO.findByTimeRange must accept LocalDateTime
            List<Schedule> schedules = scheduleDAO.findByTimeRange(startDateTime, endDateTime);

            // Check for conflicts with the same room and overlapping time
            String sessionRoom = session.getRoom();
            if (sessionRoom == null || sessionRoom.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Session has null or empty room ID: " + session.getId());
                return false; // Cannot check conflict if the session doesn't have a valid room
            }

            for (Schedule schedule : schedules) {
                // Ensure schedule.getId() returns String
                // Skip the current session when checking for conflicts against itself (during updates)
                if (schedule != null && schedule.getId() != null && session.getId() != null && session.getId().equals(schedule.getId())) {
                    continue;
                }

                // We only care about RoomSchedules for room conflicts within this controller's scope
                if (schedule instanceof RoomSchedule) {
                    RoomSchedule roomSchedule = (RoomSchedule) schedule;

                    ClassSession existingSession = convertToClassSession(roomSchedule);

                    // Check if the existing session is in the same room, on the same date,
                    // and has an overlapping time slot.
                    // convertToClassSession should properly set Room, Date, and TimeSlot
                    // Assumes ClassSession.getRoom() returns String.
                    if (existingSession != null &&
                            existingSession.getRoom() != null &&
                            existingSession.getRoom().equals(sessionRoom) &&
                            existingSession.getDate() != null &&
                            existingSession.getDate().equals(date))
                    // Time overlap is already handled by the findByTimeRange query.
                    {
                        // Found a conflict in the same room at an overlapping time
                        LOGGER.log(Level.INFO, "Conflict found for session " + session.getId() + " with session " + existingSession.getId() + " in room " + sessionRoom);
                        return true;
                    }
                }
            }

            return false; // No conflict found
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking schedule conflicts", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error checking schedule conflicts", e);
            return false;
        }
    }

    /**
     * Convert a Schedule object to a ClassSession object (Handles String ID)
     * @param schedule The Schedule object to convert (Requires Schedule model to use String ID)
     * @return Equivalent ClassSession object (Requires ClassSession model to use String ID)
     */
    private ClassSession convertToClassSession(Schedule schedule) {
        if (schedule == null) {
            return null;
        }

        // Ensure schedule.getId() returns String
        String id = schedule.getId(); // Get String ID
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Schedule object with null or empty ID encountered during conversion.");
            // Decide how to handle schedules without IDs. Returning null might break logic.
            // Creating a ClassSession with a null ID might also cause issues.
            // Let's proceed, but log the issue.
        }

        String courseName = schedule.getName(); // Use schedule name as course name
        if (courseName == null) courseName = "Unknown Course";


        // Extract teacher from description (format expected: "Teacher: {name}")
        String teacher = "Unknown";
        if (schedule.getDescription() != null && schedule.getDescription().contains("Teacher:")) {
            String[] parts = schedule.getDescription().split("Teacher:");
            if (parts.length > 1) {
                teacher = parts[1].trim();
            }
        }

        // For room schedules, use the room ID
        String room = "Unknown";
        if (schedule instanceof RoomSchedule) {
            // Requires RoomSchedule.getRoomId() to return String
            String roomId = ((RoomSchedule) schedule).getRoomId();
            if (roomId != null && !roomId.trim().isEmpty()) {
                room = roomId;
            }
        } else {
            // Handle other Schedule types if necessary, but RoomSchedule is the main focus for ClassSession
            room = "N/A"; // Example fallback for non-RoomSchedules
        }


        // Extract date and time slot
        LocalDate date = null;
        String timeSlot = "Unknown - Unknown";
        if (schedule.getStartTime() != null && schedule.getEndTime() != null) {
            date = schedule.getStartTime().toLocalDate();
            try {
                timeSlot = String.format("%s - %s",
                        schedule.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        schedule.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error formatting time slot for schedule ID " + id, e);
            }
        } else if (schedule.getStartTime() != null) {
            date = schedule.getStartTime().toLocalDate();
            try {
                timeSlot = schedule.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - Unknown";
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error formatting start time for schedule ID " + id, e);
            }
        } else if (schedule.getEndTime() != null) {
            try {
                timeSlot = "Unknown - " + schedule.getEndTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error formatting end time for schedule ID " + id, e);
            }
        } else {
            // Date and time unknown
            date = null; // Keep date as null if unknown
            timeSlot = "Unknown - Unknown";
        }


        // Create ClassSession with String ID
        // Ensure ClassSession constructor accepts String ID, String courseName, String teacher, String room, LocalDate date, String timeSlot
        try {
            return new ClassSession(id, courseName, teacher, room, date, timeSlot); // ClassSession constructor must accept String ID
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating ClassSession from Schedule object with ID: " + id, e);
            return null; // Return null if ClassSession creation fails
        }
    }

    /**
     * Convert multiple Schedule objects to ClassSession objects (Handles String IDs)
     * @param schedules List of Schedule objects (Requires Schedule model to use String ID)
     * @return List of equivalent ClassSession objects (Requires ClassSession model to use String ID)
     */
    private List<ClassSession> convertToClassSessions(List<Schedule> schedules) {
        if (schedules == null) {
            return new ArrayList<>();
        }
        List<ClassSession> sessions = new ArrayList<>();

        for (Schedule schedule : schedules) {
            if (schedule != null) {
                ClassSession session = convertToClassSession(schedule);
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessions.add(session);
                    // Update cache using String ID
                    sessionCache.put(session.getId(), session); // Uses String key
                } else if (session != null) {
                    LOGGER.log(Level.WARNING, "Converted schedule to ClassSession with null or empty ID, cannot cache. Schedule ID: " + (schedule.getId() != null ? schedule.getId() : "null"));
                    sessions.add(session); // Add session even if ID is problematic, but don't cache
                } else {
                    LOGGER.log(Level.WARNING, "Failed to convert schedule to ClassSession. Schedule ID: " + (schedule != null && schedule.getId() != null ? schedule.getId() : "null schedule object"));
                }
            }
        }

        return sessions;
    }

    /**
     * Lấy danh sách các buổi học của một lớp (ClassSession).
     * Assumes ScheduleDAO has a method findByClassId that accepts String and returns List<Schedule>.
     * @param classId ID của lớp (String)
     * @return Danh sách ClassSession thuộc lớp này.
     */
    public List<ClassSession> getClassSessionsByClassId(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get sessions by null or empty class ID.");
            return new ArrayList<>();
        }
        try {
            // Assuming ScheduleDAO has a method to find schedules by class ID.
            // ScheduleDAO.findByClassId must accept String and return List<Schedule>
            List<Schedule> schedules = scheduleDAO.findByClassId(classId); // Requires findByClassId on ScheduleDAO

            return convertToClassSessions(schedules);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by class ID: " + classId, e);
            return new ArrayList<>();
        }
    }

    /*
     * The following methods were removed as they belong to other controllers/DAOs:
     * - getStudentById(String studentId): Belongs in StudentController/StudentDAO.
     * - getAttendanceBySessionId(String sessionId): Belongs in AttendanceController/AttendanceDAO.
     * - markAttendanceCalled(String attendanceId, boolean called): Belongs in AttendanceController/AttendanceDAO.
     * - updateAttendanceNote(String attendanceId, String note): Belongs in AttendanceController/AttendanceDAO.
     * - searchSessions(List<ClassSession> sessions, String keyword): This is a list filtering operation, not a core ScheduleController responsibility.
     */

}
