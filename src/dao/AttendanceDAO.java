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

/**
 * Data Access Object for the Attendance entity
 * Handles database operations for attendance records
 */
public class AttendanceDAO {
    private final DatabaseConnection dbConnector;
    private final StudentDAO studentDAO;
    private final ClassSessionDAO sessionDAO;

    /**
     * Constructor with dependency injection
     */
    public AttendanceDAO() throws SQLException {
        dbConnector = new DatabaseConnection();
        studentDAO = new StudentDAO();
        sessionDAO = new ClassSessionDAO();
    }

    /**
     * Save a new attendance record to the database
     *
     * @param attendance Attendance object to save
     * @return true if successful, false otherwise
     */
    public boolean save(Attendance attendance) {
        String sql = "INSERT INTO attendance (student_id, class_session_id, present, note, called, has_permission, " +
                "check_in_time, record_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    attendance.setId(generatedKeys.getString(1));
                    return true;
                }
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Error saving attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing attendance record
     *
     * @param attendance Attendance object to update
     * @return true if successful, false otherwise
     */
    public boolean update(Attendance attendance) {
        String sql = "UPDATE attendance SET student_id = ?, class_session_id = ?, present = ?, " +
                "note = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ? " +
                "WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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

            stmt.setString(9, attendance.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an attendance record by ID
     *
     * @param id ID of the attendance record to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(long id) {
        String sql = "DELETE FROM attendance WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find an attendance record by ID
     *
     * @param id ID of the attendance record to find
     * @return Optional containing the attendance record if found
     */
    public Optional<Attendance> findById(String id) {
        String sql = "SELECT * FROM attendance WHERE id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Find attendance records by student ID
     *
     * @param studentId ID of the student
     * @return List of attendance records for the student
     */
    public List<Attendance> findByStudentId(String studentId) {
        String sql = "SELECT * FROM attendance WHERE student_id = ?";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by student ID: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find attendance records by class session ID
     *
     * @param sessionId ID of the class session
     * @return List of attendance records for the class session
     */
    public List<Attendance> findBySessionId(String sessionId) {
        String sql = "SELECT * FROM attendance WHERE session_id = ?";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by session ID: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find attendance records by date (uses session date)
     *
     * @param date Date to search for
     * @return List of attendance records for the specified date
     */
    public List<Attendance> findByDate(LocalDate date) {
        // This assumes there's a join with class_sessions or the date field is in the attendance table
        String sql = "SELECT a.* FROM attendance a " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "WHERE cs.date = ?";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by date: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find attendance records by student ID and session ID
     *
     * @param studentId Student ID
     * @param sessionId Session ID
     * @return Optional containing the attendance record if found
     */
    public Optional<Attendance> findByStudentAndSession(String studentId, long sessionId) {
        String sql = "SELECT * FROM attendance WHERE student_id = ? AND class_session_id = ?";

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);
            stmt.setLong(2, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by student and session: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Get all attendance records
     *
     * @return List of all attendance records
     */
    public List<Attendance> findAll() {
        String sql = "SELECT * FROM attendance";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                attendances.add(mapResultSetToAttendance(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding all attendances: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find absent students for a specific session
     *
     * @param sessionId Session ID
     * @return List of attendance records for absent students
     */
    public List<Attendance> findAbsentBySession(long sessionId) {
        String sql = "SELECT * FROM attendance WHERE class_session_id = ? AND present = FALSE";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding absent students: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find absent students who have not been called yet
     *
     * @param sessionId Session ID
     * @return List of attendance records for absent students who need to be called
     */
    public List<Attendance> findAbsentNotCalled(String sessionId) {
        String sql = "SELECT * FROM attendance WHERE class_session_id = ? AND present = FALSE AND called = FALSE";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding absent students not called: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Find attendance records in a date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records in the date range
     */
    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT a.* FROM attendance a " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "WHERE cs.date BETWEEN ? AND ?";
        List<Attendance> attendances = new ArrayList<>();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendances.add(mapResultSetToAttendance(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error finding attendance by date range: " + e.getMessage());
            e.printStackTrace();
        }

        return attendances;
    }

    /**
     * Map a ResultSet row to an Attendance object
     *
     * @param rs ResultSet containing attendance data
     * @return Attendance object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();

        // Set basic attendance properties
        attendance.setId(rs.getString("session_id"));
        attendance.setPresent(rs.getBoolean("present"));
        attendance.setNote(rs.getString("note"));
        attendance.setCalled(rs.getBoolean("called"));
        attendance.setHasPermission(rs.getBoolean("has_permission"));

        // Handle timestamps that may be null
        Timestamp checkInTime = rs.getTimestamp("check_in_time");
        if (checkInTime != null) {
            attendance.setCheckInTime(checkInTime.toLocalDateTime());
        }

        Timestamp recordTime = rs.getTimestamp("record_time");
        if (recordTime != null) {
            attendance.setRecordTime(recordTime.toLocalDateTime());
        }

        // Load related entities
        String studentId = rs.getString("student_id");
        String sessionId = rs.getString("class_session_id");

        // Get student and session objects
        Optional<Student> student = studentDAO.findById(studentId);
        Optional<ClassSession> session = sessionDAO.findById(sessionId);

        // Set related entities if they exist
        student.ifPresent(attendance::setStudent);
        session.ifPresent(attendance::setSession);

        // If related entities aren't found, at least set the IDs
        if (student.isEmpty()) {
            attendance.setStudentId(studentId);
        }

        if (session.isEmpty()) {
            attendance.setSessionId(sessionId);
        }

        return attendance;
    }

    /**
     * Batch save multiple attendance records
     *
     * @param attendances List of attendance records to save
     * @return Number of records successfully saved
     */
    public int batchSave(List<Attendance> attendances) {
        String sql = "INSERT INTO attendance (student_id, class_session_id, present, note, called, has_permission, " +
                "check_in_time, record_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int count = 0;

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            conn.setAutoCommit(false);

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

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            for (int result : results) {
                if (result > 0) {
                    count++;
                }
            }

            // Get generated IDs and set them on the attendance objects
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int index = 0;
                while (generatedKeys.next() && index < attendances.size()) {
                    attendances.get(index).setId(generatedKeys.getString(1));
                    index++;
                }
            }

            conn.setAutoCommit(true);

        } catch (SQLException e) {
            System.err.println("Error batch saving attendances: " + e.getMessage());
            e.printStackTrace();
        }

        return count;
    }

    /**
     * Batch update multiple attendance records
     *
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     */
    public int batchUpdate(List<Attendance> attendances) {
        String sql = "UPDATE attendance SET student_id = ?, class_session_id = ?, present = ?, " +
                "note = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ? " +
                "WHERE id = ?";
        int count = 0;

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

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

                stmt.setString(9, attendance.getId());

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            for (int result : results) {
                if (result > 0) {
                    count++;
                }
            }

            conn.setAutoCommit(true);

        } catch (SQLException e) {
            System.err.println("Error batch updating attendances: " + e.getMessage());
            e.printStackTrace();
        }

        return count;
    }

    /**
     * Gets attendance statistics for a student over a time period
     *
     * @param studentId Student ID
     * @param startDate Start date
     * @param endDate End date
     * @return Map with statistics (present, absent, excused)
     */
    public AttendanceStats getStudentStats(String studentId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "SUM(CASE WHEN present = TRUE THEN 1 ELSE 0 END) as present_count, " +
                "SUM(CASE WHEN present = FALSE AND has_permission = TRUE THEN 1 ELSE 0 END) as excused_count, " +
                "SUM(CASE WHEN present = FALSE AND has_permission = FALSE THEN 1 ELSE 0 END) as unexcused_count " +
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.class_session_id = cs.id " +
                "WHERE a.student_id = ? AND cs.date BETWEEN ? AND ?";

        AttendanceStats stats = new AttendanceStats();

        try (Connection conn = dbConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

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

        } catch (SQLException e) {
            System.err.println("Error getting student attendance stats: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Helper class to store attendance statistics
     */
    public static class AttendanceStats {
        private int presentCount;
        private int excusedAbsenceCount;
        private int unexcusedAbsenceCount;

        public int getPresentCount() {
            return presentCount;
        }

        public void setPresentCount(int presentCount) {
            this.presentCount = presentCount;
        }

        public int getExcusedAbsenceCount() {
            return excusedAbsenceCount;
        }

        public void setExcusedAbsenceCount(int excusedAbsenceCount) {
            this.excusedAbsenceCount = excusedAbsenceCount;
        }

        public int getUnexcusedAbsenceCount() {
            return unexcusedAbsenceCount;
        }

        public void setUnexcusedAbsenceCount(int unexcusedAbsenceCount) {
            this.unexcusedAbsenceCount = unexcusedAbsenceCount;
        }

        public int getTotalAbsenceCount() {
            return excusedAbsenceCount + unexcusedAbsenceCount;
        }

        public int getTotalCount() {
            return presentCount + excusedAbsenceCount + unexcusedAbsenceCount;
        }

        public double getAttendanceRate() {
            int total = getTotalCount();
            return total > 0 ? (double) presentCount / total : 0.0;
        }
    }

    public StudentDAO getStudentDAO() {
        return studentDAO;
    }

    public ClassSessionDAO getClassSessionDAO() {
        return sessionDAO;
    }
}
