package src.dao;

import src.model.ClassSession;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for ClassSession entity
 */
public class ClassSessionDAO {

    /**
     * Retrieves all class sessions from the database
     * @return List of ClassSession objects
     * @throws SQLException if a database access error occurs
     */
    public List<ClassSession> getAllClassSessions() throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, class_date, " +
                "start_time, end_time, class_id FROM class_sessions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ClassSession session = mapResultSetToClassSession(rs);
                sessions.add(session);
            }
        }

        return sessions;
    }

    /**
     * Retrieves a class session by its ID
     * @param id the ID of the class session
     * @return the ClassSession object, or null if not found
     * @throws SQLException if a database access error occurs
     */
    public ClassSession getClassSessionById(String id) throws SQLException {
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions WHERE session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClassSession(rs);
                }
            }
        }

        return null;
    }

    /**
     * Creates a new class session in the database
     * @param session the ClassSession object to create
     * @return the generated ID of the new class session
     * @throws SQLException if a database access error occurs
     */
    public String createClassSession(ClassSession session) throws SQLException {
        String sql = "INSERT INTO class_sessions (course_name, teacher_name, room, class_date, " +
                "start_time, end_time, class_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            prepareStatementFromClassSession(stmt, session);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getString(1);
                } else {
                    throw new SQLException("Creating class session failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Updates an existing class session in the database
     * @param session the ClassSession object to update
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateClassSession(ClassSession session) throws SQLException {
        String sql = "UPDATE class_sessions SET course_name = ?, teacher_name = ?, room = ?, " +
                "class_date = ?, start_time = ?, end_time = ?, class_id = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            prepareStatementFromClassSession(stmt, session);
            stmt.setString(8, session.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a class session from the database
     * @param id the ID of the class session to delete
     * @return true if the deletion was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteClassSession(String id) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all class sessions for a specific class
     * @param classId the ID of the class
     * @return List of ClassSession objects
     * @throws SQLException if a database access error occurs
     */
    public List<ClassSession> getSessionsByClassId(long classId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, class_date, " +
                "start_time, end_time, class_id FROM class_sessions WHERE class_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClassSession session = mapResultSetToClassSession(rs);
                    sessions.add(session);
                }
            }
        }

        return sessions;
    }

    /**
     * Maps a ResultSet to a ClassSession object
     * @param rs the ResultSet to map
     * @return the ClassSession object
     * @throws SQLException if a database access error occurs
     */
    private ClassSession mapResultSetToClassSession(ResultSet rs) throws SQLException {
        String id = rs.getString("session_id");
        String courseName = rs.getString("course_name");
        String teacher = rs.getString("teacher_name");
        String room = rs.getString("room");

        Date dateDb = rs.getDate("class_date");
        LocalDate date = null;
        if (dateDb != null) {
            date = dateDb.toLocalDate();
        }

        Time startTimeDb = rs.getTime("start_time");
        Time endTimeDb = rs.getTime("end_time");

        LocalTime startTime = null;
        LocalTime endTime = null;

        if (startTimeDb != null) {
            startTime = startTimeDb.toLocalTime();
        }

        if (endTimeDb != null) {
            endTime = endTimeDb.toLocalTime();
        }

        String classId = rs.getString("class_id");

        // Create a new ClassSession with the base data
        ClassSession session = new ClassSession();
        session.setId(id);
        session.setCourseName(courseName);
        session.setTeacher(teacher);
        session.setRoom(room);
        session.setDate(date);
        session.setClassId(classId);

        // Set the start and end times, which will automatically update the timeSlot
        if (startTime != null) {
            session.setStartTime(startTime);
        }

        if (endTime != null) {
            session.setEndTime(endTime);
        }

        return session;
    }

    /**
     * Prepares a statement with values from a ClassSession object
     * @param stmt the PreparedStatement to prepare
     * @param session the ClassSession providing the values
     * @throws SQLException if a database access error occurs
     */
    private void prepareStatementFromClassSession(PreparedStatement stmt, ClassSession session) throws SQLException {
        stmt.setString(1, session.getCourseName());
        stmt.setString(2, session.getTeacher());
        stmt.setString(3, session.getRoom());

        if (session.getDate() != null) {
            stmt.setDate(4, Date.valueOf(session.getDate()));
        } else {
            stmt.setNull(4, Types.DATE);
        }

        if (session.getStartTime() != null) {
            stmt.setTime(5, Time.valueOf(session.getStartTime()));
        } else {
            stmt.setNull(5, Types.TIME);
        }

        if (session.getEndTime() != null) {
            stmt.setTime(6, Time.valueOf(session.getEndTime()));
        } else {
            stmt.setNull(6, Types.TIME);
        }

        stmt.setString(7, session.getClassId());
    }

    /**
     * Finds a class session by its ID
     * @param sessionId the ID of the class session
     * @return Optional containing the ClassSession if found, empty Optional otherwise
     */
    public Optional<ClassSession> findById(String sessionId) {
        try {
            return Optional.ofNullable(getClassSessionById(sessionId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Finds all class sessions
     * @return List of all ClassSession objects
     */
    public List<ClassSession> findAll() {
        try {
            return getAllClassSessions();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Saves a new class session
     * @param session the ClassSession object to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(ClassSession session) {
        try {
            String id = createClassSession(session);
            session.setId(id);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing class session
     * @param session the ClassSession object to update
     * @return true if the update was successful, false otherwise
     */
    public boolean update(ClassSession session) {
        try {
            return updateClassSession(session);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a class session
     * @param session the ClassSession object to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(ClassSession session) {
        if (session == null || session.getId() == null) {
            return false;
        }
        try {
            return deleteClassSession(session.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds class sessions by class ID
     * @param classId the class ID to search for
     * @return List of ClassSession objects for the specified class
     */
    public List<ClassSession> findByClassId(long classId) {
        try {
            return getSessionsByClassId(classId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Finds class sessions within a date range
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return List of ClassSession objects within the date range
     */
    public List<ClassSession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, class_date, " +
                "start_time, end_time, class_id FROM class_sessions " +
                "WHERE class_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClassSession session = mapResultSetToClassSession(rs);
                    sessions.add(session);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sessions;
    }

    /**
     * Finds all class sessions for a specific day of the week
     * @param dayOfWeek the day of the week as a string (e.g., "MONDAY")
     * @return List of ClassSession objects for the specified day
     */
    public List<ClassSession> findByDayOfWeek(String dayOfWeek) {
        List<ClassSession> allSessions = findAll();
        List<ClassSession> filteredSessions = new ArrayList<>();

        for (ClassSession session : allSessions) {
            // Check if the session's schedule matches the requested day
            if (session.getSchedule() != null &&
                    session.getSchedule().equalsIgnoreCase(dayOfWeek)) {
                filteredSessions.add(session);
            }
        }

        return filteredSessions;
    }
}
