// --- Fixed ScheduleController.java ---

package src.controller.Schedules;

import src.model.ClassSession;
import src.dao.ClassSession.ClassSessionDAO; // Import ClassSessionDAO
import src.utils.DaoManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller quản lý dữ liệu lịch học (Class Sessions)
 * Interacts directly with ClassSessionDAO.
 * Assumes ClassSession model and ClassSessionDAO use String IDs.
 */
public class ScheduleController { // Renaming this to ClassSessionController might be more accurate conceptually
    private static final Logger LOGGER = Logger.getLogger(ScheduleController.class.getName());
    // Changed from ScheduleDAO to ClassSessionDAO
    private final ClassSessionDAO classSessionDAO;

    // Map to cache ClassSession objects by ID (using String for ID)
    private final Map<String, ClassSession> sessionCache;

    public ScheduleController() {
        // Get the ClassSessionDAO from DaoManager
        this.classSessionDAO = DaoManager.getInstance().getClassSessionDAO(); // Assumes DaoManager has getClassSessionDAO()
        this.sessionCache = new HashMap<>();
    }

    /**
     * Lấy danh sách lịch học (buổi học) trong khoảng thời gian.
     * Uses ClassSessionDAO.findByTimeRange.
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

            // Call ClassSessionDAO.findByTimeRange
            // FIX: Use findByTimeRange from ClassSessionDAO
            List<ClassSession> sessions = classSessionDAO.findByTimeRange(startDateTime, endDateTime); // Uses the new method

            // Filter by teacher if specified.
            // Ideally, ClassSessionDAO should have findByTimeRangeAndTeacher for efficiency.
            // Filtering here is less efficient if the dataset is large.
            if (teacherName != null && !teacherName.trim().isEmpty()) {
                sessions = sessions.stream()
                        .filter(session -> session != null && session.getTeacher() != null && session.getTeacher().equalsIgnoreCase(teacherName))
                        .collect(Collectors.toList());
            }

            // Add fetched sessions to cache
            sessions.forEach(session -> {
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session);
                }
            });

            return sessions;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting schedule", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách nhân sự (giáo viên) liên quan đến lịch học (buổi học).
     * Uses ClassSessionDAO.findAll and streams for distinct teachers.
     * Ideally, ClassSessionDAO should have a method to get distinct teacher names efficiently.
     * @return Danh sách tên giáo viên
     */
    public List<String> getTeachers() {
        try {
            // Retrieving all sessions and extracting teachers is inefficient for large datasets.
            // A dedicated DAO method to get distinct teachers would be much better.
            List<ClassSession> allSessions = classSessionDAO.findAll(); // Assumes findAll exists and returns List<ClassSession>
            return allSessions.stream()
                    .map(ClassSession::getTeacher)
                    .filter(teacher -> teacher != null && !teacher.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting teachers from class sessions", e);
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
     * Lấy danh sách các phòng học liên quan đến lịch học (buổi học).
     * Uses ClassSessionDAO.findAll and streams for distinct rooms.
     * Ideally, ClassSessionDAO should have a method to get distinct rooms efficiently.
     * @return Danh sách phòng học
     */
    public List<String> getRooms() {
        try {
            // Retrieving all sessions and extracting rooms is inefficient for large datasets.
            // A dedicated DAO method to get distinct rooms would be much better.
            List<ClassSession> allSessions = classSessionDAO.findAll(); // Assumes findAll exists and returns List<ClassSession>
            return allSessions.stream()
                    .map(ClassSession::getRoom)
                    .filter(room -> room != null && !room.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting rooms from class sessions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách các khóa học liên quan đến lịch học (buổi học).
     * Uses ClassSessionDAO.findAll and streams for distinct course names.
     * Ideally, ClassSessionDAO should have a method to get distinct course names efficiently.
     * @return Danh sách tên khóa học
     */
    public List<String> getCourses() {
        try {
            // Retrieving all sessions and extracting courses is inefficient for large datasets.
            // A dedicated DAO method to get distinct course names would be much better.
            List<ClassSession> allSessions = classSessionDAO.findAll(); // Assumes findAll exists and returns List<ClassSession>
            return allSessions.stream()
                    .map(ClassSession::getCourseName)
                    .filter(course -> course != null && !course.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting courses from class sessions", e);
            return new ArrayList<>();
        }
    }

    /**
     * Tìm buổi học theo ID (using String ID)
     * Uses ClassSessionDAO.findById and handles Optional return type.
     * @param sessionId ID buổi học cần tìm (String)
     * @return ClassSession nếu tìm thấy, null nếu không
     */
    public ClassSession getSessionById(String sessionId) {
        // Check cache first
        if (sessionId != null && !sessionId.trim().isEmpty() && sessionCache.containsKey(sessionId)) {
            return sessionCache.get(sessionId);
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get session with null or empty ID");
            return null;
        }

        try {
            // Call ClassSessionDAO directly to find session by ID
            // FIX: Handle Optional return type from findById
            Optional<ClassSession> sessionOptional = classSessionDAO.findById(sessionId); // findById returns Optional
            if (sessionOptional.isPresent()) {
                ClassSession session = sessionOptional.get();
                // Put in cache using String ID
                if (session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(sessionId, session);
                } else {
                    LOGGER.log(Level.WARNING, "Fetched session with null or empty ID, cannot cache. Session ID: " + sessionId);
                }
                return session;
            } else {
                return null; // Session not found
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting session by ID: " + sessionId, e);
        }
        return null;
    }

    /**
     * Lấy tất cả các buổi học của một giáo viên
     * Uses ClassSessionDAO.findByTeacherName.
     * @param teacherName Tên giáo viên
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByTeacher(String teacherName) {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get sessions by null or empty teacher name.");
            return new ArrayList<>(); // Return empty list if teacher name is invalid
        }
        try {
            // Call ClassSessionDAO directly to find sessions by teacher name
            // FIX: Use findByTeacherName from ClassSessionDAO
            List<ClassSession> sessions = classSessionDAO.findByTeacherName(teacherName); // Uses the new method

            // Add fetched sessions to cache
            sessions.forEach(session -> {
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session);
                }
            });

            return sessions;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by teacher: " + teacherName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy tất cả các buổi học trong một ngày
     * Uses ClassSessionDAO.findByDate.
     * @param date Ngày cần lấy lịch
     * @return Danh sách buổi học
     */
    public List<ClassSession> getSessionsByDate(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to get sessions with null date.");
            return new ArrayList<>(); // Return empty list if date is null
        }
        try {
            // Call ClassSessionDAO directly to find sessions by date
            // FIX: Use findByDate from ClassSessionDAO
            List<ClassSession> sessions = classSessionDAO.findByDate(date); // Uses the new method

            // Add fetched sessions to cache
            sessions.forEach(session -> {
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session);
                }
            });

            return sessions;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by date: " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Xóa một buổi học (using String ID)
     * Uses ClassSessionDAO.delete.
     * @param sessionId ID buổi học cần xóa (String)
     * @return true nếu xóa thành công
     */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete session with null or empty ID");
            return false;
        }
        try {
            // Call ClassSessionDAO directly to delete session by ID
            boolean result = classSessionDAO.delete(sessionId); // Assumes delete exists and accepts String
            if (result) {
                // Remove from cache using String ID
                sessionCache.remove(sessionId);
            }
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting session: " + sessionId, e);
            return false;
        }
    }

    /**
     * Thêm buổi học mới
     * Uses ClassSessionDAO.save.
     * @param session Buổi học cần thêm
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
                LOGGER.log(Level.INFO, "Adding session with pre-existing ID: " + session.getId());
            }

            // Call ClassSessionDAO directly to save the session
            boolean result = classSessionDAO.save(session); // Assumes save exists and accepts ClassSession

            if (result) {
                // Put in cache using String ID
                if (session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session);
                } else {
                    LOGGER.log(Level.WARNING, "Added session with null or empty ID, cannot cache.");
                }
            }

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error adding session", e);
            return false;
        }
    }

    /**
     * Cập nhật thông tin buổi học (using String ID)
     * Uses ClassSessionDAO.update.
     * @param session Buổi học với thông tin đã cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean updateSession(ClassSession session) {
        if (session == null || session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null session or session with null/empty ID");
            return false;
        }
        try {
            // Call ClassSessionDAO directly to update the session
            boolean result = classSessionDAO.update(session); // Assumes update exists and accepts ClassSession

            if (result) {
                // Update cache using String ID
                sessionCache.put(session.getId(), session);
            }

            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating session: " + session.getId(), e);
            return false;
        }
    }

    /**
     * Kiểm tra xem có xung đột lịch học không (cùng phòng, cùng giờ, cùng ngày) (using String ID)
     * Checks for conflicts with other ClassSessions in the same room at overlapping times using ClassSessionDAO.findByTimeRange.
     * @param session Buổi học cần kiểm tra
     * @return true nếu có xung đột, false nếu không
     */
    public boolean hasScheduleConflict(ClassSession session) {
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to check conflict for a null session");
            return false; // No conflict with a null session
        }
        try {
            LocalDate date = session.getDate();

            if (date == null ) {
                LOGGER.log(Level.WARNING, "Session has incomplete date/time information: " + session.getId());
                return false; // Cannot check conflict without complete date/time
            }

            LocalDateTime startDateTime = session.getStartTime();
            LocalDateTime endDateTime = session.getEndTime();

            // Fetch sessions that overlap with the given session's time range.
            // Uses ClassSessionDAO.findByTimeRange.
            // FIX: Use findByTimeRange from ClassSessionDAO
            List<ClassSession> potentialConflicts = classSessionDAO.findByTimeRange(startDateTime, endDateTime); // Uses the new method

            // Check for conflicts with the same room and overlapping time
            String sessionRoom = session.getRoom();
            if (sessionRoom == null || sessionRoom.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Session has null or empty room ID: " + session.getId());
                return false; // Cannot check conflict if the session doesn't have a valid room
            }

            for (ClassSession existingSession : potentialConflicts) {
                // Skip the current session when checking for conflicts against itself (during updates)
                if (existingSession != null && existingSession.getId() != null && session.getId() != null && session.getId().equals(existingSession.getId())) {
                    continue;
                }

                // Check if the existing session is in the same room and on the same date.
                // Time overlap is already handled by the findByTimeRange query based on start/end times.
                if (existingSession != null &&
                        existingSession.getRoom() != null &&
                        existingSession.getRoom().equals(sessionRoom) &&
                        existingSession.getDate() != null &&
                        existingSession.getDate().equals(date))
                {
                    // Found a conflict in the same room at an overlapping time
                    LOGGER.log(Level.INFO, "Conflict found for session " + session.getId() + " with session " + existingSession.getId() + " in room " + sessionRoom);
                    return true;
                }
            }

            return false; // No conflict found
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error checking schedule conflicts", e);
            return false;
        }
    }

    /**
     * Lấy danh sách các buổi học của một lớp (ClassSession).
     * Uses ClassSessionDAO.findByClassId.
     * @param classId ID của lớp (String)
     * @return Danh sách ClassSession thuộc lớp này.
     */
    public List<ClassSession> getClassSessionsByClassId(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get sessions by null or empty class ID.");
            return new ArrayList<>();
        }
        try {
            // Call ClassSessionDAO directly to find sessions by class ID.
            List<ClassSession> sessions = classSessionDAO.findByClassId(classId); // Assumes findByClassId exists and accepts String

            // Add fetched sessions to cache
            sessions.forEach(session -> {
                if (session != null && session.getId() != null && !session.getId().trim().isEmpty()) {
                    sessionCache.put(session.getId(), session);
                }
            });

            return sessions;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting sessions by class ID: " + classId, e);
            return new ArrayList<>();
        }
    }

    /*
     * Removed methods that belong to other controllers/DAOs.
     * Conversion methods removed as controller works directly with ClassSession.
     */

}
