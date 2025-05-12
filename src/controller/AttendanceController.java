package src.controller;

import src.model.attendance.Attendance;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.person.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// Import the DaoManager
import utils.DaoManager;
// Import the specific DAO classes if you need their types for the instance variables
import src.dao.AttendanceDAO;
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO;

/**
 * Controller for managing attendance records
 * Handles business logic between views and data access layer
 */
public class AttendanceController {
    // Keep the type declarations, but get the instances from DaoManager
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
        // The original constructor threw SQLException, but obtaining DAOs from
        // DaoManager should not throw SQLException, so the throws clause is removed.
    }

    /**
     * Get all class sessions from data source
     *
     * @return List of class sessions
     * @throws SQLException if database operation fails (delegated to DAO)
     */
    public List<ClassSession> getAllClassSessions() throws SQLException {
        // Use the classSessionDAO obtained from DaoManager
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
        // Use the classSessionDAO obtained from DaoManager
        // Filter all sessions by classId - Note: It might be more efficient if the DAO has a findByClassId method.
        // Keeping original logic that filters in controller for now.
        List<ClassSession> allSessions = classSessionDAO.findAll(); // Still requires fetching all first
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
        // Use the classSessionDAO obtained from DaoManager
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
        // Use the attendanceDAO obtained from DaoManager
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
        // Use the attendanceDAO obtained from DaoManager
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
        // This method's logic relies on getting sessions and then attendance.
        // If the DAO had a direct method to get attendance by class and date range,
        // that would be more efficient. Keeping current logic using multiple DAO calls.
        List<ClassSession> sessionsInRange = getClassSessionsByClassId(classId).stream()
                .filter(s -> {
                    LocalDate sessionDate = s.getDate();
                    return sessionDate != null && !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<Attendance> result = new ArrayList<>();
        // Use the attendanceDAO obtained from DaoManager
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
        // Use the attendanceDAO obtained from DaoManager
        // Assuming attendanceDAO has findByDateRange method that throws SQLException
        return attendanceDAO.findByDateRange(startDate, endDate);
    }

    /**
     * Generate absence records from attendance data
     *
     * @param attendances List of attendance records
     * @return List of absence records
     * @throws SQLException if database operation fails (delegated to DAOs)
     */
    public List<AbsenceRecord> generateAbsenceRecords(List<Attendance> attendances) throws SQLException {
        List<AbsenceRecord> absenceRecords = new ArrayList<>();

        // Use the studentDAO and classSessionDAO obtained from DaoManager
        for (Attendance attendance : attendances) {
            // Skip present students
            if (attendance.isPresent()) {
                continue;
            }

            Optional<Student> studentOpt = studentDAO.findById(attendance.getStudentId());
            Optional<ClassSession> sessionOpt = classSessionDAO.findById(attendance.getSessionId());

            if (studentOpt.isPresent() && sessionOpt.isPresent()) {
                Student student = studentOpt.get();
                ClassSession session = sessionOpt.get();

                // Determine absence status
                String status = attendance.hasPermission() ? "Excused" : "Absent";

                AbsenceRecord absenceRecord = new AbsenceRecord(
                        attendance.getId(),
                        null,                           // No image view yet
                        student.getName(),              // Student name
                        session.getClassName(),         // Class name
                        session.getDate() != null ? session.getDate().toString() : "N/A", // Date as string
                        status,                         // Attendance status
                        attendance.getNote(),           // Notes
                        attendance.isCalled(),          // Called status
                        attendance.hasPermission()      // Approved status
                );

                absenceRecords.add(absenceRecord);
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
        // Use the attendanceDAO obtained from DaoManager
        // Assuming findAbsentBySession takes String sessionId and throws SQLException
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
        // Use the attendanceDAO obtained from DaoManager
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
        // Get all class sessions using classSessionDAO from DaoManager
        List<ClassSession> allSessions = classSessionDAO.findAll();

        // For each session, get all attendance records for absent students not called using attendanceDAO
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
        // Use the attendanceDAO obtained from DaoManager
        // Get the attendance record
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(attendanceId);
        if (attendanceOpt.isPresent()) {
            // Update the called status
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
        // Use the attendanceDAO obtained from DaoManager
        // Get the attendance record
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(attendanceId);
        if (attendanceOpt.isPresent()) {
            // Update the permission status and note
            Attendance attendance = attendanceOpt.get();
            attendance.setHasPermission(true);
            if (note != null && !note.trim().isEmpty()) {
                attendance.setNote(note);
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
        // Use the attendanceDAO obtained from DaoManager
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
        // Use the attendanceDAO obtained from DaoManager
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
        // Use the attendanceDAO obtained from DaoManager
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
                    // Get the scheduled day from the session and check if it matches the day string
                    String sessionDay = session.getSchedule(); // Assuming getSchedule() returns a String representing day
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
            return sessions; // Return the original list if no valid search criteria
        }

        String searchTerm = keyword.toLowerCase().trim();

        return sessions.stream()
                .filter(session -> {
                    // Check if keyword matches any of these fields
                    boolean matchesClassName = session.getClassName() != null &&
                            session.getClassName().toLowerCase().contains(searchTerm);

                    boolean matchesTeacher = session.getTeacher() != null &&
                            session.getTeacher().toLowerCase().contains(searchTerm);

                    boolean matchesRoom = session.getRoom() != null &&
                            session.getRoom().toLowerCase().contains(searchTerm);

                    // Match if any of the conditions are true
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
        // Use the attendanceDAO obtained from DaoManager
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
        if (newValue == null || newValue.trim().isEmpty()) {
            return;
        }
        // Use the attendanceDAO obtained from DaoManager
        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setNote(newValue.trim());
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
        // Use the attendanceDAO obtained from DaoManager
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
        // Use the studentDAO obtained from DaoManager
        if(studentId != null ) {
            // Assuming studentDAO has a findById method that returns Optional<Student>
            Optional<Student> studentOpt = studentDAO.findById(studentId);
            return studentOpt.orElse(null);
        }
        return null;
    }
}
