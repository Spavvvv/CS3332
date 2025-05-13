package src.controller;

import src.model.attendance.Attendance;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.person.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level; // Import Level
import java.util.logging.Logger; // Import Logger
import java.util.stream.Collectors;

// Import the DaoManager
import utils.DaoManager;
// Import the specific DAO classes if you need their types for the instance variables
import src.dao.AttendanceDAO;
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO;

/**
 * Controller for managing attendance records and generating absence reports.
 * Handles business logic between views and data access layer.
 * Modified to align with AbsenceRecord model without ImageView and use Attendance table data.
 * Added Logger for error/warning reporting.
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
     * Generate absence records from attendance data.
     * Filters for absent students and retrieves related student and class information.
     *
     * @param attendances List of attendance records
     * @return List of absence records (without image)
     * @throws SQLException if database operation fails (delegated to DAOs)
     */
    public List<AbsenceRecord> generateAbsenceRecords(List<Attendance> attendances) throws SQLException {
        List<AbsenceRecord> absenceRecords = new ArrayList<>();

        for (Attendance attendance : attendances) {
            // Only generate AbsenceRecord for students who were not present
            if (attendance.isPresent()) {
                continue;
            }

            Optional<Student> studentOpt = studentDAO.findById(attendance.getStudentId());
            Optional<ClassSession> sessionOpt = classSessionDAO.findById(attendance.getSessionId());

            if (studentOpt.isPresent() && sessionOpt.isPresent()) {
                Student student = studentOpt.get();
                ClassSession session = sessionOpt.get();

                String absenceStatusString = attendance.hasPermission() ? "Excused" : "Absent";

                String formattedDate = session.getDate() != null ? session.getDate().format(DATE_FORMATTER) : "N/A";

                AbsenceRecord absenceRecord = new AbsenceRecord(
                        attendance.getId(),
                        student.getName(),
                        session.getClassName(),
                        formattedDate,
                        absenceStatusString,
                        attendance.getNote(),
                        attendance.isCalled(),
                        attendance.hasPermission()
                );

                absenceRecords.add(absenceRecord);
            } else {
                // Log a warning if student or session data is missing for an attendance record
                LOGGER.log(Level.WARNING, "Missing student or session data for attendance record ID: " + attendance.getId());
            }
        }

        return absenceRecords;
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
     * Save a batch of attendance records
     *
     * @param attendances List of attendance records to save
     * @return Number of records successfully saved
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public int batchSaveAttendance(List<Attendance> attendances) throws SQLException {
        return attendanceDAO.batchSave(attendances);
    }

    /**
     * Update a batch of attendance records
     *
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public int batchUpdateAttendance(List<Attendance> attendances) throws SQLException {
        return attendanceDAO.batchUpdate(attendances);
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
                    String sessionDay = session.getSchedule();
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
            return sessions;
        }

        String searchTerm = keyword.toLowerCase().trim();

        return sessions.stream()
                .filter(session -> {
                    boolean matchesClassName = session.getClassName() != null &&
                            session.getClassName().toLowerCase().contains(searchTerm);

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
                .filter(a -> !a.isPresent())
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
        if (newValue != null) {
            newValue = newValue.trim();
        }
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setNote(newValue);
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
        if (newVal == null) {
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
