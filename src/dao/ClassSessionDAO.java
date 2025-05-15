package src.dao;

import src.model.ClassSession;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for ClassSession entity
 * Modified to use String IDs for session and class, and adjust public API.
 */
public class ClassSessionDAO {

    // Removed the DatabaseConnection dbConnector member as it's obtained per operation

    // Dependencies - must be set externally by a DaoManager if needed
    // private CourseDAO courseDAO; // Example dependency

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public ClassSessionDAO() {
        // Dependencies will be set by DaoManager via setters if ClassSessionDAO had dependencies
    }

    // Example setter for a dependency, if ClassSessionDAO needed CourseDAO
    /*
    public void setCourseDAO(CourseDAO courseDAO) {
        this.courseDAO = courseDAO;
    }
     */


    /**
     * Retrieves all class sessions from the database using an existing connection.
     *
     * @param conn the active database connection
     * @return List of ClassSession objects
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindAll(Connection conn) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Pass the connection down to mapResultSetToClassSession
                ClassSession session = mapResultSetToClassSession(conn, rs);
                sessions.add(session);
            }
        } // Statement and ResultSet closed here

        return sessions;
    }

    /**
     * Retrieves a class session by its ID using an existing connection.
     * This is a helper for scenarios where a connection is already active (e.g., within a loop).
     *
     * @param conn the active database connection
     * @param id the ID of the class session (assuming String/UUID)
     * @return the ClassSession object, or null if not found
     * @throws SQLException if a database access error occurs
     */
    ClassSession internalFindById(Connection conn, String id) throws SQLException {
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions WHERE session_id = ?";

        // Use the passed connection, do NOT close it here
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) { // ResultSet is managed by try-with-resources
                if (rs.next()) {
                    // Pass the connection down to mapResultSetToClassSession
                    return mapResultSetToClassSession(conn, rs);
                }
            } // ResultSet and Statement closed here
        }
        return null;
    }

    /**
     * Creates a new class session in the database using an existing connection.
     *
     * @param conn the active database connection
     * @param session the ClassSession object to create
     * @return the generated ID of the new class session
     * @throws SQLException if a database access error occurs
     */
    String internalCreate(Connection conn, ClassSession session) throws SQLException {
        String sql = "INSERT INTO class_sessions (course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            prepareStatementFromClassSession(stmt, session);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Assuming the generated key is the session_id (which is a String)
                    String generatedId = generatedKeys.getString(1);
                    session.setId(generatedId); // Set the generated ID back to the object
                    return generatedId;
                } else {
                    // Depending on your DB/schema, you might need a different way to get the ID
                    // if it's not auto-generated and returned by getGeneratedKeys.
                    throw new SQLException("Creating class session failed, no ID obtained.");
                }
            }
        } // Statement closed here
    }

    /**
     * Updates an existing class session in the database using an existing connection.
     *
     * @param conn the active database connection
     * @param session the ClassSession object to update
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalUpdate(Connection conn, ClassSession session) throws SQLException {
        // Corrected WHERE clause to use session_id instead of id
        String sql = "UPDATE class_sessions SET course_name = ?, teacher_name = ?, room = ?, " +
                "session_date = ?, start_time = ?, end_time = ?, class_id = ? " +
                "WHERE session_id = ?"; // Assuming session_id is the primary key column name

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            prepareStatementFromClassSession(stmt, session);
            stmt.setString(8, session.getId()); // Set the session_id for the WHERE clause

            return stmt.executeUpdate() > 0;
        } // Statement closed here
    }

    /**
     * Deletes a class session from the database using an existing connection.
     *
     * @param conn the active database connection
     * @param id the ID of the class session to delete (assuming String/UUID)
     * @return true if the deletion was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalDelete(Connection conn, String id) throws SQLException {
        // Corrected WHERE clause to use session_id instead of id
        String sql = "DELETE FROM class_sessions WHERE session_id = ?"; // Assuming session_id is the primary key column name

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } // Statement closed here
    }

    /**
     * Retrieves all class sessions for a specific class using an existing connection.
     *
     * @param conn the active database connection
     * @param classId the ID of the class (assuming String/UUID)
     * @return List of ClassSession objects
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindByClassId(Connection conn, String classId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // Assuming class_id in the database is a String/VARCHAR type
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions WHERE class_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, classId); // Set as String

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Pass the connection down
                    ClassSession session = mapResultSetToClassSession(conn, rs);
                    sessions.add(session);
                }
            } // ResultSet and Statement closed here
        } // Statement closed here

        return sessions;
    }

    /**
     * Finds class sessions within a date range using an existing connection.
     *
     * @param conn the active database connection
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return List of ClassSession objects within the date range
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions " +
                "WHERE session_date BETWEEN ? AND ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Pass the connection down
                    ClassSession session = mapResultSetToClassSession(conn, rs);
                    sessions.add(session);
                }
            } // ResultSet and Statement closed here
        } // Statement closed here

        return sessions;
    }


    /**
     * Maps a ResultSet to a ClassSession object.
     * Accepts the active connection to allow loading related entities without closing resources.
     *
     * @param conn the active database connection
     * @param rs the ResultSet to map
     * @return the ClassSession object
     * @throws SQLException if a database access error occurs
     */
    private ClassSession mapResultSetToClassSession(Connection conn, ResultSet rs) throws SQLException {
        String id = rs.getString("session_id"); // Using "session_id" as the primary ID
        String courseName = rs.getString("course_name");
        String teacher = rs.getString("teacher_name");
        String room = rs.getString("room");

        Date dateDb = rs.getDate("session_date");
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

        String classId = rs.getString("class_id"); // Assuming this is the ID linking to the Class entity (as String)

        // Create a new ClassSession with the base data
        ClassSession session = new ClassSession();
        session.setId(id); // Setting the session_id as the ID
        session.setCourseName(courseName);
        session.setTeacher(teacher);
        session.setRoom(room);
        session.setDate(date);
        session.setClassId(classId); // Setting the class ID string

        // Set the start and end times, which will automatically update the timeSlot
        if (startTime != null) {
            session.setStartTime(startTime);
        }

        if (endTime != null) {
            session.setEndTime(endTime);
        }

        // If ClassSession had relationships to other entities that needed fetching
        // using other DAOs, you would call those DAOs here, passing the 'conn'
        // and potentially the injected dependency, e.g.:
        // Class relatedClass = classDAO.internalFindById(conn, classId).orElse(null);
        // session.setRelatedClass(relatedClass);

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

        stmt.setString(7, session.getClassId()); // Assuming class_id is String
    }


    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.
    // They delegate the core database logic to the internal methods.


    /**
     * Finds a class session by its ID (returns Optional). Manages its own connection.
     *
     * @param sessionId the ID of the class session (assuming String/UUID)
     * @return Optional containing the ClassSession if found, empty Optional otherwise
     */
    public Optional<ClassSession> findById(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and close connection here
            return Optional.ofNullable(internalFindById(conn, sessionId)); // Use the internal helper
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding class session by ID: " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Finds a class session by its ID (returns Optional) using an existing connection.
     * Use this when calling from another DAO that already has a connection open.
     *
     * @param conn the active database connection
     * @param sessionId the ID of the class session (assuming String/UUID)
     * @return Optional containing the ClassSession if found, empty Optional otherwise
     */
    public Optional<ClassSession> findById(Connection conn, String sessionId) {
        try {
            return Optional.ofNullable(internalFindById(conn, sessionId)); // Use the internal helper
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding class session by ID using existing connection: " + sessionId + ": " + e.getMessage());
            e.printStackTrace(); // Still print stack trace for debugging
            return Optional.empty();
        }
    }


    /**
     * Finds all class sessions. Manages its own connection.
     * @return List of all ClassSession objects
     */
    public List<ClassSession> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAll(conn);
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding all class sessions: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Saves a new class session. Manages its own connection.
     * @param session the ClassSession object to save
     * @return true if saving was successful, false otherwise
     */
    public boolean save(ClassSession session) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Transaction management for single save might be overkill, but good practice if related operations were involved
            // conn.setAutoCommit(false);
            String id = internalCreate(conn, session);
            // conn.commit();
            session.setId(id); // Set the ID generated by the database back to the object
            return true;
        } catch (SQLException e) {
            // if (conn != null) conn.rollback(); // Rollback on failure if auto-commit was false
            // Log the exception
            System.err.println("Error saving class session: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing class session. Manages its own connection.
     * @param session the ClassSession object to update
     * @return true if the update was successful, false otherwise
     */
    public boolean update(ClassSession session) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Transaction management if needed
            // conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, session);
            // if (success) conn.commit(); else conn.rollback();
            return success;
        } catch (SQLException e) {
            // if (conn != null) conn.rollback(); // Rollback on failure
            // Log the exception
            System.err.println("Error updating class session with ID " + session.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a class session by ID. Manages its own connection.
     *
     * @param sessionId the ID of the class session to delete (assuming String/UUID)
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            System.err.println("Attempted to delete class session with null or empty ID.");
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Transaction management if needed
            // conn.setAutoCommit(false);
            boolean success = internalDelete(conn, sessionId);
            // if (success) conn.commit(); else conn.rollback();
            return success;
        } catch (SQLException e) {
            // if (conn != null) conn.rollback(); // Rollback on failure
            // Log the exception
            System.err.println("Error deleting class session with ID " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds class sessions by class ID. Manages its own connection.
     * @param classId the class ID to search for (assuming String/UUID)
     * @return List of ClassSession objects for the specified class
     */
    public List<ClassSession> findByClassId(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            System.err.println("Attempted to find class sessions with null or empty class ID.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByClassId(conn, classId);
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding class sessions by class ID " + classId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Finds class sessions within a date range. Manages its own connection.
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return List of ClassSession objects within the date range
     */
    public List<ClassSession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            System.err.println("Attempted to find class sessions with null start or end date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateRange(conn, startDate, endDate);
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Error finding class sessions by date range (" + startDate + " to " + endDate + "): " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Finds all class sessions for a specific day of the week.
     * Note: This method retrieves ALL sessions first, then filters in memory.
     * A database query filtering by day of week might be more efficient if possible.
     * Manages its own connection indirectly via findAll().
     *
     * @param dayOfWeek the day of the week as a string (e.g., "MONDAY")
     * @return List of ClassSession objects for the specified day
     */
    public List<ClassSession> findByDayOfWeek(String dayOfWeek) {
        // This implementation fetches all sessions and filters.
        // If performance is an issue with many sessions, consider adding a 'day_of_week' column
        // or a calculated field/index in the database and query directly.
        List<ClassSession> allSessions = findAll(); // findAll manages its own connection
        List<ClassSession> filteredSessions = new ArrayList<>();

        if (dayOfWeek == null) {
            System.err.println("Attempted to find class sessions with null day of week.");
            return filteredSessions; // Return empty list if dayOfWeek is null
        }

        for (ClassSession session : allSessions) {
            // Check if the session's date's day of week matches the requested day
            if (session.getDate() != null) {
                if (session.getDate().getDayOfWeek().toString().equalsIgnoreCase(dayOfWeek)) {
                    filteredSessions.add(session);
                }
            }
        }

        return filteredSessions;
    }

    /**
     * Finds class sessions within a date and time range. Manages its own connection.
     * Note: This method converts LocalDateTime to Date and Time for the underlying SQL query.
     * For full time range accuracy, the SQL query should ideally compare timestamps.
     * The internal method internalFindByDateRange only uses LocalDate. We'll need a new internal method.
     * @param startDateTime the start date and time of the range
     * @param endDateTime the end date and time of the range
     * @return List of ClassSession objects within the date and time range
     */
    public List<ClassSession> findByTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            System.err.println("Attempted to find class sessions with null start or end datetime.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Implement a new internal method or adapt existing one
            return internalFindByTimeRange(conn, startDateTime, endDateTime);
        } catch (SQLException e) {
            System.err.println("Error finding class sessions by time range (" + startDateTime + " to " + endDateTime + "): " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    /**
     * Finds class sessions by teacher name. Manages its own connection.
     * @param teacherName the teacher name to search for
     * @return List of ClassSession objects taught by the specified teacher
     */
    public List<ClassSession> findByTeacherName(String teacherName) {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            System.err.println("Attempted to find class sessions with null or empty teacher name.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Implement a new internal method
            return internalFindByTeacherName(conn, teacherName);
        } catch (SQLException e) {
            System.err.println("Error finding class sessions by teacher name " + teacherName + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    /**
     * Finds class sessions for a specific date. Manages its own connection.
     * @param date the date to search for
     * @return List of ClassSession objects on the specified date
     */
    public List<ClassSession> findByDate(LocalDate date) {
        if (date == null) {
            System.err.println("Attempted to find class sessions with null date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Can potentially reuse or adapt internalFindByDateRange for a single date
            // A dedicated internal method for a single date might be slightly cleaner.
            return internalFindByDate(conn, date);
        } catch (SQLException e) {
            System.err.println("Error finding class sessions by date " + date + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Finds class sessions within a date and time range using an existing connection.
     * Compares based on session_date and start_time/end_time.
     *
     * @param conn the active database connection
     * @param startDateTime the start date and time of the range
     * @param endDateTime the end date and time of the range
     * @return List of ClassSession objects within the date and time range
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindByTimeRange(Connection conn, LocalDateTime startDateTime, LocalDateTime endDateTime) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // SQL to find sessions that overlap with the given time range.
        // A session (start_time, end_time) overlaps with range (range_start, range_end) if
        // (start_time < range_end AND end_time > range_start)
        // Assuming session_date stores the date, and start_time/end_time store the time part within that date.
        // Combining date and time for comparison:
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions " +
                "WHERE (session_date + start_time) < ? AND (session_date + end_time) > ?"; // This syntax depends on the specific SQL dialect (e.g., PostgreSQL needs specific syntax for adding TIME to DATE, MySQL might use DATETIME or TIMESTAMP columns)

        // A more portable way might be comparing DATE and TIME components separately, or using TIMESTAMP columns.
        // Assuming your database supports adding DATE and TIME for comparison or uses TIMESTAMP columns.
        // If using TIMESTAMP columns for start_time and end_time, the query simplifies.
        // Let's assume the columns 'start_time' and 'end_time' are actually TIMESTAMP types for easier range query.
        // If they are TIME types and session_date is DATE, you might need vendor-specific functions like `TIMESTAMP(session_date, start_time)`.
        // Let's use TIMESTAMP comparison assuming the columns support it or adjust the query for your specific DB.

        // Let's adjust the SQL query assuming start_time and end_time columns are TIMESTAMP
        String sqlAdjusted = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions " +
                "WHERE start_time < ? AND end_time > ?"; // Assumes start_time and end_time columns are TIMESTAMP

        try (PreparedStatement stmt = conn.prepareStatement(sqlAdjusted)) {

            // Set LocalDateTime parameters as Timestamp
            stmt.setTimestamp(1, Timestamp.valueOf(endDateTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startDateTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(conn, rs));
                }
            }
        }
        return sessions;
    }


    /**
     * Finds class sessions by teacher name using an existing connection.
     * Assumes teacher_name column exists in class_sessions table.
     *
     * @param conn the active database connection
     * @param teacherName the teacher name to search for
     * @return List of ClassSession objects taught by the specified teacher
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindByTeacherName(Connection conn, String teacherName) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // Assuming teacher_name in the database is a String/VARCHAR type
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions WHERE teacher_name = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacherName); // Set as String

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(conn, rs));
                }
            }
        }
        return sessions;
    }

    /**
     * Finds class sessions for a specific date using an existing connection.
     * Assumes session_date column exists in class_sessions table.
     *
     * @param conn the active database connection
     * @param date the date to search for
     * @return List of ClassSession objects on the specified date
     * @throws SQLException if a database access error occurs
     */
    List<ClassSession> internalFindByDate(Connection conn, LocalDate date) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT session_id, course_name, teacher_name, room, session_date, " +
                "start_time, end_time, class_id FROM class_sessions " +
                "WHERE session_date = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(conn, rs));
                }
            }
        }
        return sessions;
    }

}
