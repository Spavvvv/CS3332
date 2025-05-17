
package src.controller;

import src.model.attendance.Attendance;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.person.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.UUID; // <<<< IMPORT ADDED HERE

// Import the DaoManager
import utils.DaoManager;
// Import the specific DAO classes
import src.dao.AttendanceDAO;
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO;

/**
 * Controller for managing attendance records and generating absence reports.
 * Handles business logic between views and data access layer.
 * batchUpsertAttendance now uses findByStudentAndSession to differentiate
 * inserts vs updates, and generates UUIDs for new Attendance records
 * as their attendance_id is VARCHAR(50).
 */
public class AttendanceController {

    // Declare the Logger
    private static final Logger LOGGER = Logger.getLogger(AttendanceController.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AttendanceDAO attendanceDAO;
    private final ClassSessionDAO classSessionDAO;
    private final StudentDAO studentDAO;
    private String selectedSessionId;

    /**
     * Constructor. Obtains DAO instances from DaoManager.
     */
    public AttendanceController() {
        // Obtain DAO instances from the DaoManager singleton
        this.attendanceDAO = DaoManager.getInstance().getAttendanceDAO();
        this.classSessionDAO = DaoManager.getInstance().getClassSessionDAO();
        this.studentDAO = DaoManager.getInstance().getStudentDAO();
    }

    /**
     * Get all class sessions from data source
     *
     * @return List of class sessions
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<ClassSession> getAllClassSessions() throws SQLException {
        return classSessionDAO.findAll();
    }

    /**
     * Get class sessions for a specific class
     *
     * @param classId ID of the class
     * @return List of class sessions
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<ClassSession> getClassSessionsByClassId(String classId) throws SQLException {
        List<ClassSession> allSessions = classSessionDAO.findAll();
        return allSessions.stream()
                .filter(session -> session.getClassId() != null && session.getClassId().equals(classId))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific class session by ID
     *
     * @param sessionId ID of the class session
     * @return Class session object or null if not found
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public ClassSession getClassSessionById(String sessionId) throws SQLException {
        Optional<ClassSession> session = classSessionDAO.findById(sessionId);
        return session.orElse(null);
    }

    /**
     * Get attendance records for a specific class session
     *
     * @param sessionId ID of the class session
     * @return List of attendance records
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getAttendanceBySessionId(String sessionId) throws SQLException {
        selectedSessionId = sessionId;
        return attendanceDAO.findBySessionId(sessionId);
    }

    /**
     * Get attendance records for a specific student
     *
     * @param studentId ID of the student
     * @return List of attendance records
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getAttendanceByStudentId(String studentId) throws SQLException {
        return attendanceDAO.findByStudentId(studentId);
    }

    /**
     * Get attendance data for a specific date range for a class
     *
     * @param classId Start date of the range
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of attendance records
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getAttendanceDataInRange(String classId, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<ClassSession> sessionsInRange = getClassSessionsByClassId(classId).stream()
                .filter(s -> {
                    LocalDate sessionDate = s.getDate();
                    return sessionDate != null && !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<Attendance> result = new ArrayList<>();
        for (ClassSession session : sessionsInRange) {
            List<Attendance> sessionAttendance = attendanceDAO.findBySessionId(session.getId());
            result.addAll(sessionAttendance);
        }

        return result;
    }

    /**
     * Get attendance records for a specific date range directly from DAO
     *
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of attendance records
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        return attendanceDAO.findByDateRange(startDate, endDate);
    }

    /**
     * Get all absent students for a session
     *
     * @param sessionId Session ID
     * @return List of attendance records for absent students
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getAbsentStudentsBySession(String sessionId) throws SQLException {
        return attendanceDAO.findAbsentBySession(sessionId);
    }

    /**
     * Get all uncalled absences for a specific session
     *
     * @param sessionId Session ID
     * @return List of attendance records for absent students who haven't been called
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<Attendance> getUncalledAbsencesForSession(String sessionId) throws SQLException {
        return attendanceDAO.findAbsentNotCalled(sessionId);
    }

    /**
     * Get all uncalled absences across all sessions
     *
     * @return List of attendance records for absent students who haven't been called
     * @throws SQLException if database operation fails (delegated to DAOs)
     */
    public List<Attendance> getUncalledAbsences() throws SQLException {
        List<Attendance> allAttendances = new ArrayList<>();
        List<ClassSession> allSessions = classSessionDAO.findAll();

        for (ClassSession session : allSessions) {
            List<Attendance> absentNotCalled = attendanceDAO.findAbsentNotCalled(session.getId());
            allAttendances.addAll(absentNotCalled);
        }

        return allAttendances;
    }

    /**
     * Mark an attendance record as called
     *
     * @param attendanceId ID of the attendance record
     * @return true if successful, false otherwise
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public boolean markAttendanceAsCalled(String attendanceId) throws SQLException {
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(attendanceId);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setCalled(true);
            return attendanceDAO.update(attendance);
        }
        return false;
    }

    /**
     * Mark an attendance record as excused (with permission)
     *
     * @param attendanceId ID of the attendance record
     * @param note Note for the excused absence
     * @return true if successful, false otherwise
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public boolean excuseAbsence(String attendanceId, String note) throws SQLException {
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(attendanceId);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setHasPermission(true);
            if (note != null) {
                attendance.setNote(note.trim());
            }
            return attendanceDAO.update(attendance);
        }
        return false;
    }

    /**
     * Get the selected session ID
     *
     * @return Selected session ID
     */
    public String getSelectedSessionId() {
        return selectedSessionId;
    }

    /**
     * Set the selected session ID
     *
     * @param sessionId Session ID to select
     */
    public void setSelectedSessionId(String sessionId) {
        this.selectedSessionId = sessionId;
    }

    /**
     * Save a batch of new attendance records.
     * Assumes all records in the list are new and have their IDs (e.g., UUIDs) already set.
     *
     * @param attendances List of new attendance records to save.
     * @return Number of records successfully saved.
     * @throws SQLException if database operation fails (delegated to DAO).
     */
    public int batchSaveAttendance(List<Attendance> attendances) throws SQLException {
        if (attendances == null || attendances.isEmpty()) {
            return 0;
        }
        // Optional: Add a check here to ensure all attendances have non-null/non-empty IDs
        // if you want an extra layer of safety before calling the DAO.
        for (Attendance att : attendances) {
            if (att.getId() == null || att.getId().trim().isEmpty()) {
                LOGGER.log(Level.SEVERE, "CRITICAL: Attempting to batchSave an Attendance object without an ID. Student: " + att.getStudentId() + ", Session: " + att.getSessionId());
                // Depending on desired strictness, you could throw an exception here
                // or rely on the DAO to handle it (as it seemed to do before).
                // For now, this log will highlight if it happens.
            }
        }
        return attendanceDAO.batchSave(attendances);
    }

    /**
     * Update a batch of existing attendance records.
     * Assumes all records in the list exist in the DB and have their correct IDs.
     *
     * @param attendances List of attendance records to update.
     * @return Number of records successfully updated.
     * @throws SQLException if database operation fails (delegated to DAO).
     */
    public int batchUpdateAttendance(List<Attendance> attendances) throws SQLException {
        if (attendances == null || attendances.isEmpty()) {
            return 0;
        }
        return attendanceDAO.batchUpdate(attendances);
    }

    /**
     * Performs a batch upsert (insert or update) of attendance records.
     * For each provided record, it checks if an attendance entry already exists
     * for the given student and session using `attendanceDAO.findByStudentAndSession`.
     * If it exists, the existing record is updated.
     * If it does not exist, a new record is created with a generated UUID as its `attendance_id`.
     *
     * @param recordsToUpsert List of Attendance records (potentially from UI, may or may not have an ID)
     *                       to be inserted or updated. Each record must have studentId and sessionId.
     * @return The total number of records successfully inserted or updated.
     * @throws SQLException if a database error occurs.
     */
    public int batchUpsertAttendance(List<Attendance> recordsToUpsert) throws SQLException {
        if (recordsToUpsert == null || recordsToUpsert.isEmpty()) {
            LOGGER.log(Level.INFO, "batchUpsertAttendance called with null or empty list.");
            return 0;
        }

        List<Attendance> recordsToInsert = new ArrayList<>();
        List<Attendance> recordsToUpdate = new ArrayList<>();

        for (Attendance uiRecord : recordsToUpsert) {
            if (uiRecord.getStudentId() == null || uiRecord.getStudentId().trim().isEmpty() ||
                    uiRecord.getSessionId() == null || uiRecord.getSessionId().trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Skipping record in batchUpsertAttendance due to missing studentId or sessionId.");
                continue; // Skip records that don't have the necessary keys for lookup
            }

            Optional<Attendance> existingRecordOpt = attendanceDAO.findByStudentAndSession(
                    uiRecord.getStudentId(), uiRecord.getSessionId());

            if (existingRecordOpt.isPresent()) {
                // Record EXISTS - this is an UPDATE
                Attendance dbRecord = existingRecordOpt.get();

                // Update fields of the existing database record (dbRecord) with values from uiRecord
                dbRecord.setPresent(uiRecord.isPresent());
                dbRecord.setCalled(uiRecord.isCalled());
                dbRecord.setHasPermission(uiRecord.hasPermission());
                dbRecord.setNote(uiRecord.getNote());
                dbRecord.setStatus(uiRecord.getStatus()); // Assuming Attendance has setStatus
                // dbRecord.setAbsenceDate(uiRecord.getAbsenceDate()); // If this can change or needs to be set
                // dbRecord.setCheckInTime(uiRecord.getCheckInTime()); // If this can change

                // Add any other fields from your Attendance model that might be changed from the UI
                // IMPORTANT: We use dbRecord because it has the correct, existing attendance_id.
                recordsToUpdate.add(dbRecord);
                LOGGER.log(Level.FINER, "Record for student {0}, session {1} marked for UPDATE with ID {2}", new Object[]{uiRecord.getStudentId(), uiRecord.getSessionId(), dbRecord.getId()});

            } else {
                // Record DOES NOT EXIST - this is an INSERT
                // Generate a new attendance_id (UUID) for this new record because attendance_id is VARCHAR(50)
                // and must be application-generated.
                String newId = UUID.randomUUID().toString();
                uiRecord.setId(newId); // Set the generated ID on the uiRecord

                // uiRecord already contains all the data from the UI (status, notes, etc.)
                recordsToInsert.add(uiRecord);
                LOGGER.log(Level.FINER, "Record for student {0}, session {1} marked for INSERT with new ID {2}", new Object[]{uiRecord.getStudentId(), uiRecord.getSessionId(), newId});
            }
        }

        int successfulOperations = 0;

        if (!recordsToInsert.isEmpty()) {
            LOGGER.log(Level.INFO, "Batch inserting {0} new attendance records.", recordsToInsert.size());
            successfulOperations += batchSaveAttendance(recordsToInsert); // Using existing batchSaveAttendance
        }

        if (!recordsToUpdate.isEmpty()) {
            LOGGER.log(Level.INFO, "Batch updating {0} existing attendance records.", recordsToUpdate.size());
            successfulOperations += batchUpdateAttendance(recordsToUpdate); // Using existing batchUpdateAttendance
        }

        LOGGER.log(Level.INFO, "Batch upsert completed. Total successful operations: {0}", successfulOperations);
        return successfulOperations;
    }


    /**
     * Get attendance statistics for a student
     *
     * @param studentId Student ID
     * @param startDate Start date
     * @param endDate End date
     * @return Attendance statistics
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public AttendanceDAO.AttendanceStats getStudentAttendanceStats(String studentId, LocalDate startDate, LocalDate endDate) throws SQLException {
        return attendanceDAO.getStudentStats(studentId, startDate, endDate);
    }

    /**
     * Filters the provided list of class sessions by day of week
     *
     * @param sessions The list of sessions to filter
     * @param day The day of week string (e.g., "MONDAY", "TUESDAY", etc.)
     * @return A filtered list of sessions that match the specified day
     */
    public List<ClassSession> filterSessionsByDay(List<ClassSession> sessions, String day) {
        if (sessions == null || day == null) {
            return new ArrayList<>();
        }

        return sessions.stream()
                .filter(session -> {
                    String sessionDay = session.getDayOfWeek();
                    return sessionDay != null && sessionDay.equalsIgnoreCase(day);
                })
                .collect(Collectors.toList());
    }

    /**
     * Searches for sessions that match the given keyword in class name, teacher name,
     * or room
     *
     * @param sessions The list of sessions to search within
     * @param keyword The keyword to search for
     * @return A filtered list of sessions that match the search criteria
     */
    public List<ClassSession> searchSessions(List<ClassSession> sessions, String keyword) {
        if (sessions == null || keyword == null || keyword.trim().isEmpty()) {
            return sessions; // Return original list if no keyword or sessions
        }

        String searchTerm = keyword.toLowerCase().trim();

        return sessions.stream()
                .filter(session -> {
                    boolean matchesClassName = session.getCourseName() != null &&
                            session.getCourseName().toLowerCase().contains(searchTerm);

                    boolean matchesTeacher = session.getTeacher() != null &&
                            session.getTeacher().toLowerCase().contains(searchTerm);

                    boolean matchesRoom = session.getRoom() != null &&
                            session.getRoom().toLowerCase().contains(searchTerm);

                    return matchesClassName || matchesTeacher || matchesRoom;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get the total number of absences for a student
     *
     * @param studentId ID of the student
     * @return Number of absences
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public int getAbsentCount(String studentId) throws SQLException {
        List<Attendance> attendances = attendanceDAO.findByStudentId(studentId);
        return (int) attendances.stream()
                .filter(a -> !a.isPresent()) // Assuming isPresent() means attended
                .count();
    }

    /**
     * Updates the note for a specific attendance record.
     *
     * @param id The ID of the attendance record.
     * @param newValue The new note value.
     * @throws SQLException if a database error occurs.
     */
    public void updateAttendanceNote(String id, String newValue) throws SQLException {
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            if (newValue != null) {
                attendance.setNote(newValue.trim());
            } else {
                attendance.setNote(null);
            }
            attendanceDAO.update(attendance);
        }
    }

    /**
     * Marks an attendance record as called or uncalled.
     *
     * @param id The ID of the attendance record.
     * @param newVal The new called status (true for called, false for uncalled).
     * @throws SQLException if a database error occurs.
     */
    public void markAttendanceCalled(String id, Boolean newVal) throws SQLException {
        if (newVal == null) { // Or handle as an error/warning, or decide a default (e.g., false)
            return;
        }
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setCalled(newVal);
            attendanceDAO.update(attendance);
        }
    }

    /**
     * Gets a Student object by their ID.
     *
     * @param studentId The ID of the student.
     * @return The Student object, or null if not found.
     * @throws SQLException if a database error occurs.
     */
    public Student getStudentById(String studentId) throws SQLException {
        if(studentId != null && !studentId.trim().isEmpty()) {
            Optional<Student> studentOpt = studentDAO.findById(studentId);
            return studentOpt.orElse(null);
        }
        return null;
    }
}

