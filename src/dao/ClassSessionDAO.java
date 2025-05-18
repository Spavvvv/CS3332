
package src.dao;

import src.model.ClassSession;
import src.model.system.course.Course;
import src.model.classroom.Classroom;
import src.model.person.Teacher;

import utils.DatabaseConnection;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing ClassSession entities in the database.
 * This class handles CRUD operations and more specialized queries for class sessions.
 *
 * Key functionality:
 * 1. Basic CRUD operations for ClassSession objects
 * 2. Batch generation of sessions for courses based on schedule parameters
 * 3. Finding sessions by various criteria (date, time range, teacher, etc.)
 * 4. Transaction management for session operations
 */
public class ClassSessionDAO {

    private static final Logger LOGGER = Logger.getLogger(ClassSessionDAO.class.getName());

    public ClassSessionDAO() {
        // Constructor
    }

    /**
     * Maps a database result set row to a ClassSession object.
     *
     * @param rs ResultSet positioned at the row to map
     * @return A populated ClassSession object
     * @throws SQLException If there's an error accessing the ResultSet
     */
    private ClassSession mapResultSetToClassSession(ResultSet rs) throws SQLException {
        ClassSession session = new ClassSession();
        session.setId(rs.getString("session_id"));
        session.setCourseId(rs.getString("course_id"));
        session.setClassId(rs.getString("class_id"));
        session.setCourseName(rs.getString("course_name")); // Denormalized

        // Handle timestamps properly, converting to LocalDateTime
        Timestamp dbStartTime = rs.getTimestamp("start_time");
        if (dbStartTime != null) {
            session.setStartTime(dbStartTime.toLocalDateTime());
        }

        Timestamp dbEndTime = rs.getTimestamp("end_time");
        if (dbEndTime != null) {
            session.setEndTime(dbEndTime.toLocalDateTime());
        }
        // Model's setStartTime/setEndTime also updates sessionDate and timeSlot

        session.setRoom(rs.getString("room"));         // Denormalized room name
        session.setTeacher(rs.getString("teacher_name"));   // Denormalized teacher name
        session.setSessionNumber(rs.getInt("session_number"));

        return session;
    }

    /**
     * Prepares a SQL statement with values from a ClassSession object.
     *
     * @param stmt The prepared statement to populate with values
     * @param session The ClassSession object containing the values
     * @param includeIdInValues Whether to include the ID as a parameter (for INSERT vs UPDATE)
     * @throws SQLException If there's an error setting statement parameters
     */
    private void prepareStatementFromClassSession(PreparedStatement stmt, ClassSession session, boolean includeIdInValues) throws SQLException {
        int paramIndex = 1;

        if (includeIdInValues) {
            stmt.setString(paramIndex++, session.getId());
        }
        stmt.setString(paramIndex++, session.getCourseId());
        stmt.setString(paramIndex++, session.getClassId());
        stmt.setString(paramIndex++, session.getCourseName()); // Denormalized

        if (session.getStartTime() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getStartTime()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }

        if (session.getEndTime() != null) {
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getEndTime()));
        } else {
            stmt.setNull(paramIndex++, Types.TIMESTAMP);
        }

        stmt.setString(paramIndex++, session.getRoom());     // Stores room_name
        stmt.setString(paramIndex++, session.getTeacher());  // Stores teacher_name
        stmt.setInt(paramIndex++, session.getSessionNumber());
    }

    /**
     * Creates a new class session record in the database.
     *
     * @param conn Active database connection
     * @param session The ClassSession to create
     * @return true if creation was successful, false otherwise
     * @throws SQLException If there's a database error
     */
    boolean internalCreate(Connection conn, ClassSession session) throws SQLException {
        String sql = "INSERT INTO class_sessions (session_id, course_id, class_id, course_name, " +
                "start_time, end_time, room, teacher_name, " +
                "session_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 9 placeholders for values, not 12 as in the original comment

        if (session.getId() == null || session.getId().trim().isEmpty()) {
            // Generate a fallback ID if one wasn't provided
            session.setId("SESS_FALLBACK_" + UUID.randomUUID().toString());
            LOGGER.log(Level.INFO, "internalCreate generated fallback session ID: {0} for course {1}",
                    new Object[]{session.getId(), session.getCourseName()});
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            prepareStatementFromClassSession(stmt, session, true); // Include ID in values
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing class session in the database.
     *
     * @param conn Active database connection
     * @param session The ClassSession to update
     * @return true if update was successful, false otherwise
     * @throws SQLException If there's a database error
     */
    boolean internalUpdate(Connection conn, ClassSession session) throws SQLException {
        String sql = "UPDATE class_sessions SET course_id = ?, class_id = ?, course_name = ?, " +
                "start_time = ?, end_time = ?, room = ?, teacher_name = ?, " +
                "session_number = ? " +
                "WHERE session_id = ?"; // 8 fields to set, 1 for WHERE clause (not 11 as in the original comment)

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            prepareStatementFromClassSession(stmt, session, false); // Don't include ID in SET values
            stmt.setString(9, session.getId()); // Set the session_id for the WHERE clause (index 9, not 12)
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Generates and saves class sessions for a course based on its schedule parameters.
     *
     * @param conn Active database connection
     * @param course The course to generate sessions for
     * @param classroomDAO DAO for classroom operations
     * @param teacherDAO DAO for teacher operations
     * @param holidayDAO Optional DAO for holiday checking
     * @param scheduleDAO Optional DAO for room scheduling
     * @return List of generated and saved class sessions
     * @throws SQLException If there's a database error
     */
    public List<ClassSession> generateAndSaveSessionsForCourse(
            Connection conn,
            Course course,
            src.dao.ClassroomDAO classroomDAO,
            src.dao.TeacherDAO teacherDAO,
            src.dao.HolidayDAO holidayDAO,
            src.dao.ScheduleDAO scheduleDAO
    ) throws SQLException {

        List<ClassSession> generatedSessions = new ArrayList<>();

        // Validate course and required properties
        if (course == null) {
            LOGGER.log(Level.WARNING, "Course object is null. Cannot generate sessions.");
            return generatedSessions;
        }

        if (course.getCourseId() == null || course.getStartDate() == null ||
                course.getEndDate() == null || course.getCourseStartTime() == null || course.getCourseEndTime() == null ||
                course.getDaysOfWeekAsString() == null || course.getDaysOfWeekAsString().trim().isEmpty() ||
                course.getClassId() == null) {
            LOGGER.log(Level.WARNING, "Course {0} has missing essential scheduling fields. Cannot generate sessions.",
                    course.getCourseId());
            return generatedSessions;
        }

        if (course.getStartDate().isAfter(course.getEndDate())) {
            LOGGER.log(Level.WARNING, "Course {0} start date is after end date. Cannot generate sessions.",
                    course.getCourseId());
            return generatedSessions;
        }

        // Get room and teacher information
        String actualRoomName = "N/A";
        String actualTeacherName = "N/A";
        Classroom classroomForSchedule = null;

        // Fetch Classroom details if available
        if (classroomDAO != null && course.getRoomId() != null && !course.getRoomId().trim().isEmpty()) {
            Optional<Classroom> classroomOpt = classroomDAO.findByRoomId(course.getRoomId());
            if (classroomOpt.isPresent()) {
                classroomForSchedule = classroomOpt.get();
                actualRoomName = classroomForSchedule.getRoomName();
            } else {
                LOGGER.log(Level.WARNING, "Classroom not found for ID: {0} for course {1}",
                        new Object[]{course.getRoomId(), course.getCourseId()});
            }
        }

        // Fetch Teacher details if available
        if (teacherDAO != null && course.getTeacherId() != null && !course.getTeacherId().trim().isEmpty()) {
            Optional<Teacher> teacherOpt = teacherDAO.findById(course.getTeacherId());
            if (teacherOpt.isPresent()) {
                actualTeacherName = teacherOpt.get().getName();
            } else {
                LOGGER.log(Level.WARNING, "Teacher not found for ID: {0} for course {1}",
                        new Object[]{course.getTeacherId(), course.getCourseId()});
            }
        }

        // Parse scheduled days from string representation
        Set<DayOfWeek> scheduledDays = new HashSet<>();
        try {
            String[] dayStrings = course.getDaysOfWeekAsString().toUpperCase().split(",");
            for (String dayStr : dayStrings) {
                if (!dayStr.trim().isEmpty()) {
                    scheduledDays.add(DayOfWeek.valueOf(dayStr.trim()));
                }
            }
            if (scheduledDays.isEmpty()) {
                LOGGER.log(Level.WARNING, "No valid days_of_week found for course {0}: {1}",
                        new Object[]{course.getCourseId(), course.getDaysOfWeekAsString()});
                return generatedSessions;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Error parsing days_of_week '" + course.getDaysOfWeekAsString() +
                    "' for course " + course.getCourseId(), e);
            throw new SQLException("Invalid days_of_week format for course " + course.getCourseId() +
                    ": " + course.getDaysOfWeekAsString(), e);
        }

        // Iterate through dates and generate sessions
        LocalDate currentIterDate = course.getStartDate();
        LocalDate courseEndDate = course.getEndDate();
        LocalTime sessionStartTimeOfDay = course.getCourseStartTime();
        LocalTime sessionEndTimeOfDay = course.getCourseEndTime();
        int sessionCounter = 1;

        while (!currentIterDate.isAfter(courseEndDate)) {
            if (scheduledDays.contains(currentIterDate.getDayOfWeek())) {
                // Check if this date is a holiday
                boolean isHoliday = false;
                if (holidayDAO != null) {
                    isHoliday = holidayDAO.isHoliday(conn, currentIterDate);
                }

                if (!isHoliday) {
                    // Generate session for this date
                    LocalDateTime actualSessionStartDateTime = LocalDateTime.of(currentIterDate, sessionStartTimeOfDay);
                    LocalDateTime actualSessionEndDateTime = LocalDateTime.of(currentIterDate, sessionEndTimeOfDay);

                    ClassSession newSession = new ClassSession();
                    // Generate a unique session ID pattern
                    String generatedSessionId = "SESS_" + course.getCourseId().replaceAll("[^a-zA-Z0-9]", "") + "_" +
                            currentIterDate.toString().replace("-", "") + "_" +
                            String.format("%03d", sessionCounter);

                    newSession.setId(generatedSessionId);
                    newSession.setCourseId(course.getCourseId());
                    newSession.setClassId(course.getClassId());
                    newSession.setCourseName(course.getCourseName());
                    newSession.setStartTime(actualSessionStartDateTime);
                    newSession.setEndTime(actualSessionEndDateTime);
                    newSession.setRoom(actualRoomName);
                    newSession.setTeacher(actualTeacherName);
                    newSession.setSessionNumber(sessionCounter);

                    // Save the session
                    boolean savedSuccessfully = internalCreate(conn, newSession);
                    if (savedSuccessfully) {
                        generatedSessions.add(newSession);
                        LOGGER.log(Level.FINER, "Generated and saved session: {0}", newSession.getId());

                        // Optional room schedule integration would go here
                        // Placeholder for now, as the full RoomSchedule implementation is commented
                    } else {
                        LOGGER.log(Level.WARNING, "Failed to save generated class session: {0} for course {1}. "
                                        + "Transaction should be rolled back by caller.",
                                new Object[]{newSession.getId(), course.getCourseId()});
                    }
                    sessionCounter++;
                } else {
                    LOGGER.log(Level.INFO, "Skipping session generation for course {0} on {1} as it is a holiday.",
                            new Object[]{course.getCourseId(), currentIterDate});
                }
            }
            currentIterDate = currentIterDate.plusDays(1);
        }

        LOGGER.log(Level.INFO, "Attempted to generate {0} sessions for course {1}, successfully saved {2}",
                new Object[]{(sessionCounter - 1), course.getCourseId(), generatedSessions.size()});
        return generatedSessions;
    }

    /**
     * Deletes future class sessions for a course.
     *
     * @param conn Active database connection
     * @param courseId The course ID to delete sessions for
     * @return Number of deleted sessions
     * @throws SQLException If there's a database error
     */
    public int deleteFutureSessionsByCourseId(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE course_id = ? AND start_time >= ?";
        int deletedRows = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            deletedRows = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted {0} future sessions for course ID: {1}",
                    new Object[]{deletedRows, courseId});
        }
        return deletedRows;
    }

    /**
     * Deletes all class sessions for a course.
     *
     * @param conn Active database connection
     * @param courseId The course ID to delete sessions for
     * @return Number of deleted sessions
     * @throws SQLException If there's a database error
     */
    public int deleteAllSessionsByCourseId(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE course_id = ?";
        int deletedRows = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            deletedRows = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted {0} (all) sessions for course ID: {1}",
                    new Object[]{deletedRows, courseId});
        }
        return deletedRows;
    }

    // Base SELECT statement for consistent column selection
    private String getBaseSelectClassSessionSQL() {
        return "SELECT session_id, course_id, class_id, course_name, " +
                "start_time, end_time, room, teacher_name, " +
                "session_number " +
                "FROM class_sessions";
    }

    // Internal query methods (using provided connection)

    List<ClassSession> internalFindAll(Connection conn) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapResultSetToClassSession(rs));
            }
        }
        return sessions;
    }

    ClassSession internalFindById(Connection conn, String id) throws SQLException {
        String sql = getBaseSelectClassSessionSQL() + " WHERE session_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClassSession(rs);
                }
            }
        }
        return null;
    }

    boolean internalDelete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE session_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    List<ClassSession> internalFindByClassId(Connection conn, String classId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE class_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByCourseId(Connection conn, String courseId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE course_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE DATE(start_time) BETWEEN ? AND ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDateTimeRange(Connection conn, LocalDateTime startDateTime, LocalDateTime endDateTime) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // Find sessions that overlap with the given range
        String sql = getBaseSelectClassSessionSQL() + " WHERE start_time < ? AND end_time > ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(endDateTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startDateTime));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByTeacherName(Connection conn, String teacherName) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE teacher_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDate(Connection conn, LocalDate date) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE DATE(start_time) = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    // Public wrapper methods (managing their own connections)

    public Optional<ClassSession> findById(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(internalFindById(conn, sessionId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class session by ID: " + sessionId, e);
            return Optional.empty();
        }
    }

    public Optional<ClassSession> findById(Connection conn, String sessionId) {
        try {
            return Optional.ofNullable(internalFindById(conn, sessionId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class session by ID using existing connection: " + sessionId, e);
            return Optional.empty();
        }
    }

    public List<ClassSession> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAll(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all class sessions", e);
            return new ArrayList<>();
        }
    }

    public boolean save(ClassSession session) {
        if (session.getId() == null || session.getId().trim().isEmpty()) {
            session.setId("SESS_SAVE_" + UUID.randomUUID().toString());
            LOGGER.log(Level.INFO, "Public save method generated new session ID: {0}", session.getId());
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalCreate(conn, session);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving class session: " + session.getId(), e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public boolean update(ClassSession session) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, session);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating class session with ID " + session.getId(), e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public boolean delete(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete class session with null or empty ID.");
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, sessionId);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting class session with ID " + sessionId, e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public List<ClassSession> findByCourseId(String courseId) {
        if (courseId == null || courseId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty course ID.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByCourseId(conn, courseId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by course ID " + courseId, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByClassId(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty class ID.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByClassId(conn, classId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by class ID " + classId, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateRange(conn, startDate, endDate);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by date range (" + startDate + " to " + endDate + ")", e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByTeacherName(String teacherName) {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty teacher name.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByTeacherName(conn, teacherName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by teacher name " + teacherName, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByDate(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDate(conn, date);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by date " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Finds class sessions that fall within a specific time range.
     * This method will find any sessions where the session's time period overlaps
     * with the given time range.
     *
     * @param startTime The start datetime of the range to search
     * @param endTime The end datetime of the range to search
     * @return A list of ClassSession objects that overlap with the specified time range
     */
    public List<ClassSession> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end time.");
            return new ArrayList<>();
        }

        if (startTime.isAfter(endTime)) {
            LOGGER.log(Level.WARNING, "Invalid time range: start time is after end time.");
            return new ArrayList<>();
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateTimeRange(conn, startTime, endTime);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by time range ("
                    + startTime + " to " + endTime + ")", e);
            return new ArrayList<>();
        }
    }

    /**
     * Overloaded method to find class sessions within a time range using an existing connection.
     *
     * @param conn An existing database connection
     * @param startTime The start datetime of the range to search
     * @param endTime The end datetime of the range to search
     * @return A list of ClassSession objects that overlap with the specified time range
     * @throws SQLException if a database access error occurs
     */
    public List<ClassSession> findByTimeRange(Connection conn, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end time.");
            return new ArrayList<>();
        }

        if (startTime.isAfter(endTime)) {
            LOGGER.log(Level.WARNING, "Invalid time range: start time is after end time.");
            return new ArrayList<>();
        }

        return internalFindByDateTimeRange(conn, startTime, endTime);
    }
}

