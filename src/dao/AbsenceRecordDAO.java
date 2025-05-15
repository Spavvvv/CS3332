
package src.dao;

import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.system.course.Course;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AbsenceRecordDAO {
    private static final Logger LOGGER = Logger.getLogger(AbsenceRecordDAO.class.getName());

    /**
     * Retrieves a list of available class sessions. (Original method largely unchanged)
     * @return A list of ClassSession objects.
     * @throws SQLException if a database access error occurs.
     */
    public List<ClassSession> getAvailableSessions() throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = "SELECT cs.session_id, cs.class_id, cs.session_date, cs.course_name, cs.teacher_name, " +
                "cs.start_time, cs.end_time, cs.room, cl.class_name AS actual_class_name " +
                "FROM class_sessions cs " +
                "JOIN classes cl ON cs.class_id = cl.class_id " +
                "ORDER BY cs.session_date DESC, cs.start_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String sessionId = rs.getString("session_id");
                String sessionSpecificClassId = rs.getString("class_id");
                LocalDate sessionDate = rs.getDate("session_date").toLocalDate();
                String courseSubjectName = rs.getString("course_name");
                String teacherName = rs.getString("teacher_name");
                String room = rs.getString("room");

                Time startTimeSql = rs.getTime("start_time");
                LocalTime sessionStartTime = (startTimeSql != null) ? startTimeSql.toLocalTime() : null;

                Time endTimeSql = rs.getTime("end_time");
                LocalTime sessionEndTime = (endTimeSql != null) ? endTimeSql.toLocalTime() : null;

                Course tempCourse = new Course(); // Assuming Course has a default constructor
                tempCourse.setCourseName(courseSubjectName); // Assuming setCourseName exists

                ClassSession session = new ClassSession(
                        sessionId, tempCourse, teacherName, room,
                        sessionDate, sessionStartTime, sessionEndTime, sessionSpecificClassId
                );
                sessions.add(session);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching available class sessions.", e);
            throw e;
        }
        return sessions;
    }

    /**
     * Finds all students for a given session_id and their attendance status from the 'attendance' table.
     * If no attendance record exists for a student, defaults are used.
     *
     * @param sessionId The ID of the class_session.
     * @return A list of AbsenceRecord objects.
     * @throws SQLException if a database access error occurs.
     */
    public List<AbsenceRecord> findBySession(String sessionId) throws SQLException {
        List<AbsenceRecord> records = new ArrayList<>();
        String sql = "SELECT " +
                "    s.id AS student_id, " +
                "    s.name AS student_name, " +
                "    cl.class_name AS actual_class_name, " +
                "    cs.session_date AS actual_session_date, " +
                "    cs.session_id AS current_session_id, " + // ensure we pass this to AbsenceRecord
                "    a.attendance_id, " +
                "    a.status AS attendance_status, " +
                "    a.notes AS attendance_notes, " +
                "    a.called AS attendance_called, " +
                "    a.has_permission AS attendance_has_permission " +
                "FROM " +
                "    class_sessions cs " +
                "JOIN " +
                "    classes cl ON cs.class_id = cl.class_id " +
                "JOIN " +
                "    students s ON cl.class_id = s.class_id " +
                "LEFT JOIN " +
                "    attendance a ON s.id = a.student_id AND cs.session_id = a.session_id " +
                "WHERE " +
                "    cs.session_id = ? " +
                "ORDER BY s.name";

        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.warning("Session ID is null or empty for findBySession. Returning empty list.");
            return records;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                int displayIdCounter = 1;
                while (rs.next()) {
                    String studentId = rs.getString("student_id");
                    String studentName = rs.getString("student_name");
                    String actualClassName = rs.getString("actual_class_name");
                    LocalDate actualSessionDate = rs.getDate("actual_session_date").toLocalDate();
                    String currentSessionId = rs.getString("current_session_id"); // Fetched for the AbsenceRecord constructor

                    String attendanceStatus;
                    String attendanceNotes;
                    boolean attendanceCalled;
                    boolean attendanceHasPermission;

                    if (rs.getString("attendance_id") != null) { // Attendance record exists
                        attendanceStatus = rs.getString("attendance_status");
                        attendanceNotes = rs.getString("attendance_notes");
                        attendanceCalled = rs.getBoolean("attendance_called");
                        attendanceHasPermission = rs.getBoolean("attendance_has_permission");
                    } else { // No attendance record, use defaults
                        attendanceStatus = "Chưa điểm danh";
                        attendanceNotes = "";
                        attendanceCalled = false;
                        attendanceHasPermission = false;
                    }

                    AbsenceRecord record = new AbsenceRecord(
                            displayIdCounter++,
                            studentId,
                            studentName,
                            actualClassName,
                            actualSessionDate,
                            attendanceStatus,
                            attendanceNotes,
                            attendanceCalled,
                            attendanceHasPermission,
                            currentSessionId // Pass session ID to the record
                    );
                    records.add(record);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding absence records by session: " + sessionId, e);
            throw e;
        }
        return records;
    }

    /**
     * Inserts or updates a single AbsenceRecord in the 'attendance' table.
     *
     * @param conn   The database connection (expects to be part of a transaction).
     * @param record The AbsenceRecord to save.
     * @return true if the operation was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    private boolean upsertAbsenceRecord(Connection conn, AbsenceRecord record) throws SQLException {
        String checkSql = "SELECT attendance_id FROM attendance WHERE student_id = ? AND session_id = ?";
        String existingAttendanceId = null;

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, record.getStudentId());
            checkStmt.setString(2, record.getSessionId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    existingAttendanceId = rs.getString("attendance_id");
                }
            }
        }

        boolean isPresent = "Có mặt".equalsIgnoreCase(record.getStatus());

        boolean hasPermission = "Có phép".equalsIgnoreCase(record.getStatus());

        if (existingAttendanceId != null) { // Update existing record
            String updateSql = "UPDATE attendance SET status = ?, notes = ?, called = ?, has_permission = ?, present = ?, absence_date = ?, record_time = CURRENT_TIMESTAMP " +
                    "WHERE attendance_id = ?";
            // Conditionally update check_in_time only if marking as present and it's not already set, or if status indicates presence
            if (isPresent) {
                updateSql = "UPDATE attendance SET status = ?, notes = ?, called = ?, has_permission = ?, present = ?, absence_date = ?, " +
                        "check_in_time = COALESCE(check_in_time, CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END), record_time = CURRENT_TIMESTAMP " +
                        "WHERE attendance_id = ?";
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, record.getStatus());
                updateStmt.setString(2, record.getNote());
                updateStmt.setBoolean(3, record.isCalled());
                updateStmt.setBoolean(4, record.isApproved()); // Maps to has_permission
                updateStmt.setBoolean(5, isPresent);
                updateStmt.setDate(6, Date.valueOf(record.getAbsenceDate()));
                if (isPresent) {
                    updateStmt.setBoolean(7, isPresent); // For the CASE statement in COALESCE
                    updateStmt.setString(8, existingAttendanceId);
                } else {
                    updateStmt.setString(7, existingAttendanceId);
                }
                return updateStmt.executeUpdate() > 0;
            }
        } else { // Insert new record
            String insertSql = "INSERT INTO attendance (attendance_id, session_id, student_id, status, notes, present, called, has_permission, absence_date, check_in_time, record_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                String newAttendanceId = UUID.randomUUID().toString();
                insertStmt.setString(1, newAttendanceId);
                insertStmt.setString(2, record.getSessionId());
                insertStmt.setString(3, record.getStudentId());
                insertStmt.setString(4, record.getStatus());
                insertStmt.setString(5, record.getNote());
                insertStmt.setBoolean(6, isPresent);
                insertStmt.setBoolean(7, record.isCalled());
                insertStmt.setBoolean(8, hasPermission); // Maps to has_permission
                insertStmt.setDate(9, Date.valueOf(record.getAbsenceDate()));
                if (isPresent) {
                    insertStmt.setTimestamp(10, Timestamp.valueOf(java.time.LocalDateTime.now()));
                } else {
                    insertStmt.setNull(10, Types.TIMESTAMP);
                }
                return insertStmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Saves a list of absence records to the database using upsert logic within a transaction.
     *
     * @param absenceRecords The list of records to save.
     * @return true if all records were saved successfully, false otherwise.
     */
    public boolean saveAbsenceRecords(List<AbsenceRecord> absenceRecords) {
        if (absenceRecords == null || absenceRecords.isEmpty()) {
            LOGGER.info("No absence records to save.");
            return true; // Nothing to do
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            for (AbsenceRecord record : absenceRecords) {
                // Assuming AbsenceRecord has getSessionId() and getStudentId()
                if (record.getSessionId() == null || record.getStudentId() == null) {
                    LOGGER.warning("Skipping record due to null sessionId or studentId: " + record.getStudentName());
                    continue;
                }
                if (!upsertAbsenceRecord(conn, record)) {
                    // Log specific record failure if desired
                    LOGGER.warning("Failed to save record for student: " + record.getStudentName() + " in session: " + record.getSessionId());
                    // Depending on requirements, you might choose to continue or rollback immediately
                }
            }

            conn.commit(); // Commit transaction
            LOGGER.info("Successfully saved " + absenceRecords.size() + " absence records.");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving batch of absence records. Rolling back transaction.", e);
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error during transaction rollback.", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore default
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection.", e);
                }
            }
        }
    }
}

