
package src.dao;

import src.model.ClassSession;
import src.model.attendance.Attendance;
import src.model.person.Student;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Data Access Object for the Attendance entity.
 * Follows the pattern of using a DaoManager for dependency injection and
 * managing connections per-operation via wrapper methods.
 */
public class AttendanceDAO {
    private static final Logger DAO_LOGGER = Logger.getLogger(AttendanceDAO.class.getName());

    // Dependencies - must be set externally by a DaoManager
    private StudentDAO studentDAO;
    private ClassSessionDAO sessionDAO;

    /**
     * Constructor. Dependencies are not initialized here; they must be set externally.
     */
    public AttendanceDAO() {
        // Dependencies will be set by DaoManager via setters
    }

    /**
     * Set StudentDAO - used for dependency injection.
     *
     * @param studentDAO The StudentDAO instance
     */
    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    /**
     * Set ClassSessionDAO - used for dependency injection.
     *
     * @param sessionDAO The ClassSessionDAO instance
     */
    public void setClassSessionDAO(ClassSessionDAO sessionDAO) {
        this.sessionDAO = sessionDAO;
    }

    /**
     * Check if StudentDAO dependency is set.
     */
    private void checkStudentDAODependency() {
        if (this.studentDAO == null) {
            DAO_LOGGER.warning("StudentDAO dependency not set on AttendanceDAO. Functionality requiring it may fail.");
        }
    }

    /**
     * Check if ClassSessionDAO dependency is set.
     */
    private void checkClassSessionDAODependency() {
        if (this.sessionDAO == null) {
            DAO_LOGGER.warning("ClassSessionDAO dependency not set on AttendanceDAO. Functionality requiring it may fail.");
        }
    }


    // --- Internal Methods (Package-private or Private) ---
    // These methods take a Connection as a parameter and perform the core SQL logic.

    /**
     * Internal method to save a new attendance record using an existing connection.
     * Assumes attendance_id is set on the Attendance object if it's application-generated (e.g., UUID).
     *
     * @param conn       the active database connection
     * @param attendance Attendance object to save
     * @return true if successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalSave(Connection conn, Attendance attendance) throws SQLException {
        String sql = "INSERT INTO attendance (attendance_id, student_id, session_id, present, notes, called, has_permission, " +
                "check_in_time, record_time, status, absence_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, attendance.getId()); // attendance_id
            stmt.setString(2, attendance.getStudentId());
            stmt.setString(3, attendance.getSessionId());
            stmt.setBoolean(4, attendance.isPresent());
            stmt.setString(5, attendance.getNote());
            stmt.setBoolean(6, attendance.isCalled());
            stmt.setBoolean(7, attendance.hasPermission());

            if (attendance.getCheckInTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(attendance.getCheckInTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            if (attendance.getRecordTime() != null) {
                stmt.setTimestamp(9, Timestamp.valueOf(attendance.getRecordTime()));
            } else {
                stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now())); // Default to now if null
            }

            stmt.setString(10, attendance.getStatus());

            if (attendance.getAbsenceDate() != null) {
                stmt.setDate(11, Date.valueOf(attendance.getAbsenceDate()));
            } else {
                stmt.setNull(11, Types.DATE);
            }

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Internal method to update an existing attendance record using an existing connection.
     *
     * @param conn       the active database connection
     * @param attendance Attendance object to update
     * @return true if successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalUpdate(Connection conn, Attendance attendance) throws SQLException {
        String sql = "UPDATE attendance SET student_id = ?, session_id = ?, present = ?, " +
                "notes = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ?, status = ?, absence_date = ? " +
                "WHERE attendance_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, attendance.getStudentId());
            stmt.setString(2, attendance.getSessionId());
            stmt.setBoolean(3, attendance.isPresent());
            stmt.setString(4, attendance.getNote());
            stmt.setBoolean(5, attendance.isCalled());
            stmt.setBoolean(6, attendance.hasPermission());

            if (attendance.getCheckInTime() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(attendance.getCheckInTime()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }

            if (attendance.getRecordTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
            } else {
                stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now())); // Default to now if null
            }

            stmt.setString(9, attendance.getStatus());

            if (attendance.getAbsenceDate() != null) {
                stmt.setDate(10, Date.valueOf(attendance.getAbsenceDate()));
            } else {
                stmt.setNull(10, Types.DATE);
            }
            stmt.setString(11, attendance.getId()); // attendance_id for WHERE clause

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to delete an attendance record by ID using an existing connection.
     *
     * @param conn the active database connection
     * @param id ID of the attendance record to delete (maps to attendance_id)
     * @return true if successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalDelete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM attendance WHERE attendance_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to find an attendance record by ID using an existing connection.
     *
     * @param conn the active database connection
     * @param id ID of the attendance record to find (maps to attendance_id)
     * @return Optional containing the attendance record if found
     * @throws SQLException if a database access error occurs
     */
    Optional<Attendance> internalFindById(Connection conn, String id) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE attendance_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Internal method to find attendance records by student ID using an existing connection.
     *
     * @param conn      the active database connection
     * @param studentId ID of the student
     * @return List of attendance records for the student
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindByStudentId(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE student_id = ?";
        List<Attendance> attendances = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to find attendance records by class session ID using an existing connection.
     *
     * @param conn      the active database connection
     * @param sessionId ID of the class session
     * @return List of attendance records for the class session
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindBySessionId(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE session_id = ?";
        List<Attendance> attendances = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to find attendance records by date (uses absence_date from attendance table directly).
     *
     * @param conn the active database connection
     * @param date Date to search for
     * @return List of attendance records for the specified date
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindByDate(Connection conn, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE absence_date = ?";
        List<Attendance> attendances = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to find attendance records by student ID and session ID using an existing connection.
     *
     * @param conn      the active database connection
     * @param studentId Student ID
     * @param sessionId Session ID
     * @return Optional containing the attendance record if found
     * @throws SQLException if a database access error occurs
     */
    Optional<Attendance> internalFindByStudentAndSession(Connection conn, String studentId, String sessionId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE student_id = ? AND session_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Internal method to get all attendance records using an existing connection.
     *
     * @param conn the active database connection
     * @return List of all attendance records
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindAll(Connection conn) throws SQLException {
        String sql = "SELECT * FROM attendance";
        List<Attendance> attendances = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                attendances.add(mapResultSetToAttendance(rs, conn));
            }
        }
        return attendances;
    }

    /**
     * Internal method to find absent students for a specific session using an existing connection.
     *
     * @param conn      the active database connection
     * @param sessionId Session ID (assuming String/UUID)
     * @return List of attendance records for absent students
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindAbsentBySession(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE session_id = ? AND present = FALSE";
        List<Attendance> attendances = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to find absent students who have not been called yet using an existing connection.
     *
     * @param conn      the active database connection
     * @param sessionId Session ID (assuming String/UUID)
     * @return List of attendance records for absent students who need to be called
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindAbsentNotCalled(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE session_id = ? AND present = FALSE AND called = FALSE";
        List<Attendance> attendances = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to find attendance records in a date range using an existing connection.
     * Uses 'absence_date' from attendance table.
     * @param conn      the active database connection
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records in the date range
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM attendance WHERE absence_date BETWEEN ? AND ?";
        List<Attendance> attendances = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs, conn));
                }
            }
        }
        return attendances;
    }

    /**
     * Internal method to batch save multiple attendance records using an existing connection.
     * Assumes attendance_id is set on each Attendance object.
     *
     * @param conn       the active database connection
     * @param attendances List of attendance records to save
     * @return Number of records successfully saved
     * @throws SQLException if a database access error occurs
     */
    int internalBatchSave(Connection conn, List<Attendance> attendances) throws SQLException {
        String sql = "INSERT INTO attendance (attendance_id, student_id, session_id, present, notes, called, has_permission, " +
                "check_in_time, record_time, status, absence_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int successCount = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Attendance attendance : attendances) {
                stmt.setString(1, attendance.getId()); // attendance_id
                stmt.setString(2, attendance.getStudentId());
                stmt.setString(3, attendance.getSessionId());
                stmt.setBoolean(4, attendance.isPresent());
                stmt.setString(5, attendance.getNote());
                stmt.setBoolean(6, attendance.isCalled());
                stmt.setBoolean(7, attendance.hasPermission());

                if (attendance.getCheckInTime() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(attendance.getCheckInTime()));
                } else {
                    stmt.setNull(8, Types.TIMESTAMP);
                }
                if (attendance.getRecordTime() != null) {
                    stmt.setTimestamp(9, Timestamp.valueOf(attendance.getRecordTime()));
                } else {
                    stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                }
                stmt.setString(10, attendance.getStatus());
                if (attendance.getAbsenceDate() != null) {
                    stmt.setDate(11, Date.valueOf(attendance.getAbsenceDate()));
                } else {
                    stmt.setNull(11, Types.DATE);
                }
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            for (int result : results) {
                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                    successCount++;
                }
            }
        }
        return successCount;
    }

    /**
     * Internal method to batch update multiple attendance records using an existing connection.
     *
     * @param conn       the active database connection
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     * @throws SQLException if a database access error occurs
     */
    int internalBatchUpdate(Connection conn, List<Attendance> attendances) throws SQLException {
        String sql = "UPDATE attendance SET student_id = ?, session_id = ?, present = ?, " +
                "notes = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ?, status = ?, absence_date = ? " +
                "WHERE attendance_id = ?";
        int successCount = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Attendance attendance : attendances) {
                stmt.setString(1, attendance.getStudentId());
                stmt.setString(2, attendance.getSessionId());
                stmt.setBoolean(3, attendance.isPresent());
                stmt.setString(4, attendance.getNote());
                stmt.setBoolean(5, attendance.isCalled());
                stmt.setBoolean(6, attendance.hasPermission());

                if (attendance.getCheckInTime() != null) {
                    stmt.setTimestamp(7, Timestamp.valueOf(attendance.getCheckInTime()));
                } else {
                    stmt.setNull(7, Types.TIMESTAMP);
                }
                if (attendance.getRecordTime() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
                } else {
                    stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                }
                stmt.setString(9, attendance.getStatus());
                if (attendance.getAbsenceDate() != null) {
                    stmt.setDate(10, Date.valueOf(attendance.getAbsenceDate()));
                } else {
                    stmt.setNull(10, Types.DATE);
                }
                stmt.setString(11, attendance.getId()); // attendance_id for WHERE
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            for (int result : results) {
                if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                    successCount++;
                }
            }
        }
        return successCount;
    }

    /**
     * Internal method to get attendance statistics for a student over a time period using an existing connection.
     * Uses 'absence_date' for filtering.
     * @param conn      the active database connection
     * @param studentId Student ID
     * @param startDate Start date
     * @param endDate End date
     * @return AttendanceStats object with statistics
     * @throws SQLException if a database access error occurs
     */
    AttendanceStats internalGetStudentStats(Connection conn, String studentId, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT " +
                "SUM(CASE WHEN present = TRUE THEN 1 ELSE 0 END) as present_count, " +
                "SUM(CASE WHEN present = FALSE AND has_permission = TRUE THEN 1 ELSE 0 END) as excused_count, " +
                "SUM(CASE WHEN present = FALSE AND has_permission = FALSE THEN 1 ELSE 0 END) as unexcused_count " +
                "FROM attendance " +
                "WHERE student_id = ? AND absence_date BETWEEN ? AND ?";

        AttendanceStats stats = new AttendanceStats();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.setPresentCount(rs.getInt("present_count"));
                    stats.setExcusedAbsenceCount(rs.getInt("excused_count"));
                    stats.setUnexcusedAbsenceCount(rs.getInt("unexcused_count"));
                }
            }
        }
        return stats;
    }


    /**
     * Map a ResultSet row to an Attendance object.
     *
     * @param rs ResultSet containing attendance data
     * @param conn The active database connection (passed for potential future use, currently sub-DAOs get new connections)
     * @return Attendance object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private Attendance mapResultSetToAttendance(ResultSet rs, Connection conn) throws SQLException {
        Attendance attendance = new Attendance();

        attendance.setId(rs.getString("attendance_id"));
        attendance.setPresent(rs.getBoolean("present"));
        attendance.setNote(rs.getString("notes"));
        attendance.setCalled(rs.getBoolean("called"));
        attendance.setHasPermission(rs.getBoolean("has_permission"));
        attendance.setStatus(rs.getString("status"));

        Timestamp checkInTime = rs.getTimestamp("check_in_time");
        if (checkInTime != null) {
            attendance.setCheckInTime(checkInTime.toLocalDateTime());
        }

        Timestamp recordTime = rs.getTimestamp("record_time");
        if (recordTime != null) {
            attendance.setRecordTime(recordTime.toLocalDateTime());
        }

        Date absenceSqlDate = rs.getDate("absence_date");
        if (absenceSqlDate != null) {
            attendance.setAbsenceDate(absenceSqlDate.toLocalDate());
        }

        String studentId = rs.getString("student_id");
        String sessionId = rs.getString("session_id");

        // Set IDs first, as Student/ClassSession objects might be fetched based on these.
        // This also ensures Attendance object has studentId and sessionId even if full objects can't be fetched.
        attendance.setStudentId(studentId);
        attendance.setSessionId(sessionId);

        checkStudentDAODependency();
        checkClassSessionDAODependency();

        // Fetch full Student object if DAO and ID are available
        if (studentDAO != null && studentId != null) {
            Optional<Student> studentOpt = studentDAO.findById(studentId); // Uses public findById, which gets its own connection
            studentOpt.ifPresent(attendance::setStudent);
        }

        // Fetch full ClassSession object if DAO and ID are available
        if (sessionDAO != null && sessionId != null) {
            Optional<ClassSession> sessionOpt = sessionDAO.findById(sessionId); // Uses public findById, which gets its own connection
            sessionOpt.ifPresent(attendance::setSession);
        }
        return attendance;
    }

    /**
     * Helper class to store attendance statistics
     */
    public static class AttendanceStats {
        private int presentCount = 0;
        private int excusedAbsenceCount = 0;
        private int unexcusedAbsenceCount = 0;

        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public int getExcusedAbsenceCount() { return excusedAbsenceCount; }
        public void setExcusedAbsenceCount(int excusedAbsenceCount) { this.excusedAbsenceCount = excusedAbsenceCount; }
        public int getUnexcusedAbsenceCount() { return unexcusedAbsenceCount; }
        public void setUnexcusedAbsenceCount(int unexcusedAbsenceCount) { this.unexcusedAbsenceCount = unexcusedAbsenceCount; }
        public int getTotalAbsenceCount() { return excusedAbsenceCount + unexcusedAbsenceCount; }
        public int getTotalCount() { return presentCount + excusedAbsenceCount + unexcusedAbsenceCount; }
        public double getAttendanceRate() {
            int total = getTotalCount();
            return total > 0 ? (double) presentCount / total : 0.0;
        }
    }


    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.

    public boolean save(Attendance attendance) {
        if (attendance.getId() == null || attendance.getId().trim().isEmpty()) {
            DAO_LOGGER.severe("Attempted to save Attendance with null or empty ID.");
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalSave(conn, attendance);
            if (success) {
                conn.commit();
            } else {
                conn.rollback(); // Explicit rollback on failure
            }
            return success;
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error saving attendance: " + attendance.getId(), e);
            if (conn != null) {
                try {
                    conn.rollback(); // Attempt rollback on exception
                } catch (SQLException ex) {
                    DAO_LOGGER.log(Level.SEVERE, "Error rolling back transaction for attendance: " + attendance.getId(), ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit state
                    conn.close();
                } catch (SQLException e) {
                    DAO_LOGGER.log(Level.SEVERE, "Error closing connection after saving attendance: " + attendance.getId(), e);
                }
            }
        }
    }

    public boolean update(Attendance attendance) {
        if (attendance.getId() == null || attendance.getId().trim().isEmpty()) {
            DAO_LOGGER.severe("Attempted to update Attendance with null or empty ID.");
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, attendance);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error updating attendance: " + attendance.getId(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    DAO_LOGGER.log(Level.SEVERE, "Error rolling back transaction for attendance update: " + attendance.getId(), ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    DAO_LOGGER.log(Level.SEVERE, "Error closing connection after updating attendance: " + attendance.getId(), e);
                }
            }
        }
    }

    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            DAO_LOGGER.severe("Attempted to delete Attendance with null or empty ID.");
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, id);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error deleting attendance with ID: " + id, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    DAO_LOGGER.log(Level.SEVERE, "Error rolling back transaction for attendance deletion: " + id, ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    DAO_LOGGER.log(Level.SEVERE, "Error closing connection after deleting attendance: " + id, e);
                }
            }
        }
    }

    public Optional<Attendance> findById(String id) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindById(conn, id);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by ID: " + id, e);
            return Optional.empty();
        }
    }

    public List<Attendance> findByStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByStudentId(conn, studentId);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by student ID: " + studentId, e);
            return new ArrayList<>();
        }
    }

    public List<Attendance> findBySessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindBySessionId(conn, sessionId);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by session ID: " + sessionId, e);
            return new ArrayList<>();
        }
    }

    public List<Attendance> findByDate(LocalDate date) {
        if (date == null) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDate(conn, date);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by date: " + date, e);
            return new ArrayList<>();
        }
    }

    public Optional<Attendance> findByStudentAndSession(String studentId, String sessionId) {
        if (studentId == null || studentId.trim().isEmpty() || sessionId == null || sessionId.trim().isEmpty()) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByStudentAndSession(conn, studentId, sessionId);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by student " + studentId + " and session " + sessionId, e);
            return Optional.empty();
        }
    }

    public List<Attendance> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAll(conn);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding all attendances", e);
            return new ArrayList<>();
        }
    }

    public List<Attendance> findAbsentBySession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAbsentBySession(conn, sessionId);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding absent students for session " + sessionId, e);
            return new ArrayList<>();
        }
    }

    public List<Attendance> findAbsentNotCalled(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAbsentNotCalled(conn, sessionId);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding absent students not called for session " + sessionId, e);
            return new ArrayList<>();
        }
    }

    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateRange(conn, startDate, endDate);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error finding attendance by date range (" + startDate + " to " + endDate + ")", e);
            return new ArrayList<>();
        }
    }

    public int batchSave(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) return 0;
        for (Attendance att : attendances) {
            if (att.getId() == null || att.getId().trim().isEmpty()) {
                DAO_LOGGER.severe("Attempted to batch save Attendance with null or empty ID. Aborting batch operation.");
                return 0;
            }
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int savedCount = internalBatchSave(conn, attendances);
            conn.commit();
            return savedCount;
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error batch saving attendances.", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    DAO_LOGGER.log(Level.SEVERE, "Error rolling back batch save transaction.", ex);
                }
            }
            return 0;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    DAO_LOGGER.log(Level.SEVERE, "Error closing connection after batch saving attendances.", e);
                }
            }
        }
    }

    public int batchUpdate(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) return 0;
        for (Attendance att : attendances) {
            if (att.getId() == null || att.getId().trim().isEmpty()) {
                DAO_LOGGER.severe("Attempted to batch update Attendance with null or empty ID. Aborting batch operation.");
                return 0;
            }
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int updatedCount = internalBatchUpdate(conn, attendances);
            conn.commit();
            return updatedCount;
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error batch updating attendances.", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    DAO_LOGGER.log(Level.SEVERE, "Error rolling back batch update transaction.", ex);
                }
            }
            return 0;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    DAO_LOGGER.log(Level.SEVERE, "Error closing connection after batch updating attendances.", e);
                }
            }
        }
    }

    public AttendanceStats getStudentStats(String studentId, LocalDate startDate, LocalDate endDate) {
        if (studentId == null || studentId.trim().isEmpty() || startDate == null || endDate == null) {
            DAO_LOGGER.warning("Invalid input for getStudentStats: studentId, startDate, or endDate is null/empty.");
            return new AttendanceStats(); // Return empty stats object
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalGetStudentStats(conn, studentId, startDate, endDate);
        } catch (SQLException e) {
            DAO_LOGGER.log(Level.SEVERE, "Error getting student attendance stats for student " + studentId, e);
            return new AttendanceStats(); // Return empty stats object on error
        }
    }
}
