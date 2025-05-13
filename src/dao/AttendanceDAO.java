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
 * Data Access Object for the Attendance entity.
 * Follows the pattern of using a DaoManager for dependency injection and
 * managing connections per-operation via wrapper methods.
 */
public class AttendanceDAO {

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
     *
     * @throws IllegalStateException if StudentDAO dependency is not set.
     */
    private void checkStudentDAODependency() {
        if (this.studentDAO == null) {
            System.err.println("Warning: StudentDAO dependency not set on AttendanceDAO. Functionality requiring it may fail.");
        }
    }

    /**
     * Check if ClassSessionDAO dependency is set.
     *
     * @throws IllegalStateException if ClassSessionDAO dependency is not set.
     */
    private void checkClassSessionDAODependency() {
        if (this.sessionDAO == null) {
            System.err.println("Warning: ClassSessionDAO dependency not set on AttendanceDAO. Functionality requiring it may fail.");
        }
    }


    // --- Internal Methods (Package-private or Private) ---
    // These methods take a Connection as a parameter and perform the core SQL logic.
    // They typically throw SQLException and rely on dependencies already being set.

    /**
     * Internal method to save a new attendance record using an existing connection.
     *
     * @param conn       the active database connection
     * @param attendance Attendance object to save
     * @return true if successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalSave(Connection conn, Attendance attendance) throws SQLException {
        // Added 'status' column
        String sql = "INSERT INTO attendance (student_id, session_id, present, notes, called, has_permission, " +
                "check_in_time, record_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            // Allow database default for record_time if null in object
            if (attendance.getRecordTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            // Set status
            stmt.setString(9, attendance.getStatus());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        // Assuming the first generated key is the attendance_id
                        attendance.setId(generatedKeys.getString(1));
                        return true;
                    } else {
                        // If no generated keys were returned but rows were affected, still consider it a success
                        // Depending on DB and schema, you might need to query for the ID differently
                        return true;
                    }
                }
            }
            return false;
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
        // Added 'status', changed WHERE clause to use 'attendance_id'
        String sql = "UPDATE attendance SET student_id = ?, session_id = ?, present = ?, " +
                "notes = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ?, status = ? " +
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

            // Allow database default for record_time if null in object (though updates might behave differently)
            if (attendance.getRecordTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }

            // Set status
            stmt.setString(9, attendance.getStatus());

            // Set attendance_id for WHERE clause
            stmt.setString(10, attendance.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Internal method to delete an attendance record by ID using an existing connection.
     *
     * @param conn the active database connection
     * @param id ID of the attendance record to delete (assuming String/UUID) - maps to attendance_id
     * @return true if successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean internalDelete(Connection conn, String id) throws SQLException {
        // Changed WHERE clause to use 'attendance_id'
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
     * @param id ID of the attendance record to find (assuming String/UUID) - maps to attendance_id
     * @return Optional containing the attendance record if found
     * @throws SQLException if a database access error occurs
     */
    Optional<Attendance> internalFindById(Connection conn, String id) throws SQLException {
        // Changed WHERE clause to use 'attendance_id'
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
     * @param sessionId ID of the class session (assuming String/UUID)
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
     * Internal method to find attendance records by date (uses session date) using an existing connection.
     *
     * @param conn the active database connection
     * @param date Date to search for
     * @return List of attendance records for the specified date
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindByDate(Connection conn, LocalDate date) throws SQLException {
        String sql = "SELECT a.* FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.id " +
                "WHERE cs.date = ?";
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
     * @param sessionId Session ID (assuming String/UUID)
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
     *
     * @param conn      the active database connection
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records in the date range
     * @throws SQLException if a database access error occurs
     */
    List<Attendance> internalFindByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT a.* FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.id " +
                "WHERE cs.date BETWEEN ? AND ?";
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
     * Manages the transaction for the batch.
     *
     * @param conn       the active database connection
     * @param attendances List of attendance records to save
     * @return Number of records successfully saved
     * @throws SQLException if a database access error occurs
     */
    int internalBatchSave(Connection conn, List<Attendance> attendances) throws SQLException {
        // Added 'status' column
        String sql = "INSERT INTO attendance (student_id, session_id, present, notes, called, has_permission, " +
                "check_in_time, record_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int successCount = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

                // Allow database default for record_time if null in object
                if (attendance.getRecordTime() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
                } else {
                    stmt.setNull(8, Types.TIMESTAMP);
                }

                // Set status
                stmt.setString(9, attendance.getStatus());

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();

            // Process batch results and update success count
            for (int result : results) {
                if (result >= 0) { // SUCCESS_NO_INFO or rows updated
                    successCount++;
                }
            }

            // Attempt to get generated IDs and set them on the attendance objects
            // Note: Getting generated keys for batch inserts can be database-specific
            // and might not return keys for every row in some drivers/databases.
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int index = 0;
                while (generatedKeys.next() && index < attendances.size()) {
                    // Assuming the order of generated keys matches the order of addBatch calls
                    // and the first generated key is the attendance_id
                    try {
                        attendances.get(index).setId(generatedKeys.getString(1));
                        index++;
                    } catch (SQLException e) {
                        System.err.println("Warning: Could not retrieve generated key for batch insert at index " + index + ": " + e.getMessage());
                        index++;
                    }
                }
            }

        }
        return successCount;
    }

    /**
     * Internal method to batch update multiple attendance records using an existing connection.
     * Manages the transaction for the batch.
     *
     * @param conn       the active database connection
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     * @throws SQLException if a database access error occurs
     */
    int internalBatchUpdate(Connection conn, List<Attendance> attendances) throws SQLException {
        // Added 'status', changed WHERE clause to use 'attendance_id'
        String sql = "UPDATE attendance SET student_id = ?, session_id = ?, present = ?, " +
                "notes = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ?, status = ? " +
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

                // Allow database default for record_time if null in object
                if (attendance.getRecordTime() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
                } else {
                    stmt.setNull(8, Types.TIMESTAMP);
                }

                // Set status
                stmt.setString(9, attendance.getStatus());

                // Set attendance_id for WHERE clause
                stmt.setString(10, attendance.getId());

                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();

            // Process batch results and update success count
            for (int result : results) {
                if (result >= 0) { // SUCCESS_NO_INFO or rows updated
                    successCount++;
                }
            }
        }
        return successCount;
    }


    /**
     * Internal method to get attendance statistics for a student over a time period using an existing connection.
     *
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
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.id " +
                "WHERE a.student_id = ? AND cs.date BETWEEN ? AND ?";

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
     * This method uses the injected DAOs to potentially load related Student and ClassSession objects.
     * It requires a connection if the dependent DAOs' internal methods require a connection.
     * Since mapResultSetToAttendance is called within internal methods that already have a connection,
     * we pass the connection down.
     *
     * @param rs ResultSet containing attendance data
     * @param conn The active database connection (needed for dependent DAO calls if they use internal methods)
     * @return Attendance object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private Attendance mapResultSetToAttendance(ResultSet rs, Connection conn) throws SQLException {
        Attendance attendance = new Attendance();

        // Set basic attendance properties - get 'attendance_id' instead of 'id'
        attendance.setId(rs.getString("attendance_id"));
        attendance.setPresent(rs.getBoolean("present"));
        attendance.setNote(rs.getString("notes"));
        attendance.setCalled(rs.getBoolean("called"));
        attendance.setHasPermission(rs.getBoolean("has_permission"));
        // Get 'status'
        attendance.setStatus(rs.getString("status"));

        // Handle timestamps that may be null
        Timestamp checkInTime = rs.getTimestamp("check_in_time");
        if (checkInTime != null) {
            attendance.setCheckInTime(checkInTime.toLocalDateTime());
        }

        Timestamp recordTime = rs.getTimestamp("record_time");
        if (recordTime != null) {
            attendance.setRecordTime(recordTime.toLocalDateTime());
        }


        // Load related entities using injected DAOs and the current connection
        String studentId = rs.getString("student_id");
        String sessionId = rs.getString("session_id");

        // Set the IDs directly first, in case loading the full objects fails
        attendance.setStudentId(studentId);
        attendance.setSessionId(sessionId);

        // Use injected DAOs to get related objects.
        // We use the internal methods of the dependent DAOs, passing the connection.
        checkStudentDAODependency();
        checkClassSessionDAODependency();

        if (studentDAO != null) {
            // Assuming StudentDAO has a findById(Connection conn, String id) method that takes a connection
            // If StudentDAO's findById is a public wrapper method that gets its own connection,
            // passing 'conn' here is incorrect. Assuming it follows the internal/public pattern.
            Optional<Student> student = studentDAO.findById(conn, studentId);
            student.ifPresent(attendance::setStudent);
        }

        if (sessionDAO != null) {
            // Assuming ClassSessionDAO has a findById(Connection conn, String id) method that takes a connection
            // If ClassSessionDAO's findById is a public wrapper method, passing 'conn' here is incorrect.
            // Assuming it follows the internal/public pattern.
            Optional<ClassSession> session = sessionDAO.findById(conn, sessionId);
            session.ifPresent(attendance::setSession);
        }

        return attendance;
    }

    /**
     * Helper class to store attendance statistics
     * (Kept as in the original code)
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


    // --- Public Wrapper Methods ---
    // These methods manage their own connection and handle exceptions.
    // They delegate the core database logic to the internal methods.

    /**
     * Save a new attendance record. Manages its own connection and transaction.
     *
     * @param attendance Attendance object to save
     * @return true if successful, false otherwise
     */
    public boolean save(Attendance attendance) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalSave(conn, attendance);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error saving attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing attendance record. Manages its own connection and transaction.
     *
     * @param attendance Attendance object to update
     * @return true if successful, false otherwise
     */
    public boolean update(Attendance attendance) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalUpdate(conn, attendance);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error updating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete an attendance record by ID. Manages its own connection and transaction.
     *
     * @param id ID of the attendance record to delete (assuming String/UUID) - maps to attendance_id
     * @return true if successful, false otherwise
     */
    public boolean delete(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            boolean success = internalDelete(conn, id);
            if (success) {
                conn.commit(); // Commit if successful
            } else {
                conn.rollback(); // Rollback if failed
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting attendance with ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find an attendance record by ID. Manages its own connection.
     *
     * @param id ID of the attendance record to find (assuming String/UUID) - maps to attendance_id
     * @return Optional containing the attendance record if found
     */
    public Optional<Attendance> findById(String id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindById(conn, id);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by ID: " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Find attendance records by student ID. Manages its own connection.
     *
     * @param studentId ID of the student
     * @return List of attendance records for the student
     */
    public List<Attendance> findByStudentId(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByStudentId(conn, studentId);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by student ID: " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find attendance records by class session ID. Manages its own connection.
     *
     * @param sessionId ID of the class session (assuming String/UUID)
     * @return List of attendance records for the class session
     */
    public List<Attendance> findBySessionId(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindBySessionId(conn, sessionId);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by session ID: " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find attendance records by date (uses session date). Manages its own connection.
     *
     * @param date Date to search for
     * @return List of attendance records for the specified date
     */
    public List<Attendance> findByDate(LocalDate date) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDate(conn, date);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by date: " + date + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find attendance records by student ID and session ID. Manages its own connection.
     *
     * @param studentId Student ID
     * @param sessionId Session ID (assuming String/UUID)
     * @return Optional containing the attendance record if found
     */
    public Optional<Attendance> findByStudentAndSession(String studentId, String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByStudentAndSession(conn, studentId, sessionId);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by student " + studentId + " and session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Get all attendance records. Manages its own connection.
     *
     * @return List of all attendance records
     */
    public List<Attendance> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAll(conn);
        } catch (SQLException e) {
            System.err.println("Error finding all attendances: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find absent students for a specific session. Manages its own connection.
     *
     * @param sessionId Session ID (assuming String/UUID)
     * @return List of attendance records for absent students
     */
    public List<Attendance> findAbsentBySession(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAbsentBySession(conn, sessionId);
        } catch (SQLException e) {
            System.err.println("Error finding absent students for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find absent students who have not been called yet. Manages its own connection.
     *
     * @param sessionId Session ID (assuming String/UUID)
     * @return List of attendance records for absent students who need to be called
     */
    public List<Attendance> findAbsentNotCalled(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAbsentNotCalled(conn, sessionId);
        } catch (SQLException e) {
            System.err.println("Error finding absent students not called for session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Find attendance records in a date range. Manages its own connection.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return List of attendance records in the date range
     * @throws SQLException if a database access error occurs
     */
    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateRange(conn, startDate, endDate);
        } catch (SQLException e) {
            System.err.println("Error finding attendance by date range (" + startDate + " to " + endDate + "): " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Batch save multiple attendance records. Manages its own connection and transaction.
     *
     * @param attendances List of attendance records to save
     * @return Number of records successfully saved
     */
    public int batchSave(List<Attendance> attendances) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            int savedCount = internalBatchSave(conn, attendances);
            // It's better to commit even if not all were saved, unless atomicity of the whole batch is critical
            conn.commit(); // Commit whatever was successful
            conn.setAutoCommit(true); // Restore default commit behavior
            return savedCount;
        } catch (SQLException e) {
            System.err.println("Error batch saving attendances: " + e.getMessage());
            e.printStackTrace();
            // Rollback would be handled by the finally block or the try-with-resources closing conn
            return 0;
        }
    }

    /**
     * Batch update multiple attendance records. Manages its own connection and transaction.
     *
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     */
    public int batchUpdate(List<Attendance> attendances) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            int updatedCount = internalBatchUpdate(conn, attendances);
            // It's better to commit even if not all were updated, unless atomicity of the whole batch is critical
            conn.commit(); // Commit whatever was successful
            conn.setAutoCommit(true); // Restore default commit behavior
            return updatedCount;
        } catch (SQLException e) {
            System.err.println("Error batch updating attendances: " + e.getMessage());
            e.printStackTrace();
            // Rollback would be handled by the finally block or the try-with-resources closing conn
            return 0;
        }
    }

    /**
     * Gets attendance statistics for a student over a time period. Manages its own connection.
     *
     * @param studentId Student ID
     * @param startDate Start date
     * @param endDate End date
     * @return AttendanceStats object with statistics. Returns empty stats on error.
     */
    public AttendanceStats getStudentStats(String studentId, LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalGetStudentStats(conn, studentId, startDate, endDate);
        } catch (SQLException e) {
            System.err.println("Error getting student attendance stats for student " + studentId + " (" + startDate + " to " + endDate + "): " + e.getMessage());
            e.printStackTrace();
            return new AttendanceStats(); // Return empty stats on error
        }
    }
}
