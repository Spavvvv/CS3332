package src.controller;

import src.dao.AttendanceDAO;
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO;
import src.model.attendance.Attendance;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.person.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing attendance records
 * Handles business logic between views and data access layer
 */
public class AttendanceController {
    private final AttendanceDAO attendanceDAO;
    private final ClassSessionDAO classSessionDAO;
    private final StudentDAO studentDAO;
    private String selectedSessionId;

    /**
     * Constructor with dependencies
     */
    public AttendanceController() throws SQLException {
        attendanceDAO = new AttendanceDAO();
        classSessionDAO = attendanceDAO.getClassSessionDAO();
        studentDAO = attendanceDAO.getStudentDAO();
    }

    /**
     * Get all class sessions from data source
     *
     * @return List of class sessions
     * @throws SQLException if database operation fails
     */
    public List<ClassSession> getAllClassSessions() throws SQLException {
        return classSessionDAO.findAll();
    }

    /**
     * Get class sessions for a specific class
     *
     * @param classId ID of the class
     * @return List of class sessions
     * @throws SQLException if database operation fails
     */
    public List<ClassSession> getClassSessionsByClassId(String classId) throws SQLException {
        // Filter all sessions by classId
        return classSessionDAO.findAll().stream()
                .filter(session -> session.getClassId().equals(classId))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific class session by ID
     *
     * @param sessionId ID of the class session
     * @return Class session object
     * @throws SQLException if database operation fails
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
     * @throws SQLException if database operation fails
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
     * @throws SQLException if database operation fails
     */
    public List<Attendance> getAttendanceByStudentId(String studentId) throws SQLException {
        return attendanceDAO.findByStudentId(studentId);
    }

    /**
     * Get attendance data for a specific date range
     *
     * @param classId ID of the class
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of attendance records
     * @throws SQLException if database operation fails
     */
    public List<Attendance> getAttendanceDataInRange(String classId, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        List<ClassSession> allSessions = getClassSessionsByClassId(classId);

        // Filter sessions by date range
        List<ClassSession> sessionsInRange = allSessions.stream()
                .filter(s -> {
                    LocalDate sessionDate = s.getDate();
                    return !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<Attendance> result = new ArrayList<>();

        // Collect attendance records for all sessions in range
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
     */
    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        return attendanceDAO.findByDateRange(startDate, endDate);
    }

    /**
     * Generate absence records from attendance data
     *
     * @param attendances List of attendance records
     * @return List of absence records
     * @throws SQLException if database operation fails
     */
    public List<AbsenceRecord> generateAbsenceRecords(List<Attendance> attendances) throws SQLException {
        List<AbsenceRecord> absenceRecords = new ArrayList<>();

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
                        session.getDate().toString(),   // Date as string
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
     */
    public List<Attendance> getAbsentStudentsBySession(long sessionId) {
        return attendanceDAO.findAbsentBySession(sessionId);
    }

    /**
     * Get all uncalled absences for a specific session
     *
     * @param sessionId Session ID
     * @return List of attendance records for absent students who haven't been called
     */
    public List<Attendance> getUncalledAbsencesForSession(String sessionId) {
        return attendanceDAO.findAbsentNotCalled(sessionId);
    }

    /**
     * Get all uncalled absences across all sessions
     *
     * @return List of attendance records for absent students who haven't been called
     * @throws SQLException if database operation fails
     */
    public List<Attendance> getUncalledAbsences() throws SQLException {
        List<Attendance> allAttendances = new ArrayList<>();
        // Get all class sessions
        List<ClassSession> allSessions = classSessionDAO.findAll();

        // For each session, get all attendance records for absent students not called
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
     * @throws SQLException if database operation fails
     */
    public boolean markAttendanceAsCalled(String attendanceId) throws SQLException {
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
     * @throws SQLException if database operation fails
     */
    public boolean excuseAbsence(String attendanceId, String note) throws SQLException {
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
     */
    public int batchSaveAttendance(List<Attendance> attendances) {
        return attendanceDAO.batchSave(attendances);
    }

    /**
     * Update a batch of attendance records
     *
     * @param attendances List of attendance records to update
     * @return Number of records successfully updated
     */
    public int batchUpdateAttendance(List<Attendance> attendances) {
        return attendanceDAO.batchUpdate(attendances);
    }

    /**
     * Get attendance statistics for a student
     *
     * @param studentId Student ID
     * @param startDate Start date
     * @param endDate End date
     * @return Attendance statistics
     */
    public AttendanceDAO.AttendanceStats getStudentAttendanceStats(String studentId, LocalDate startDate, LocalDate endDate) {
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
                    String sessionDay = session.getSchedule();
                    return sessionDay != null && sessionDay.equalsIgnoreCase(day);
                })
                .collect(Collectors.toList());
    }

    /**
     * Searches for sessions that match the given keyword in class name, subject, room,
     * or teacher name
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
     * @throws SQLException if database operation fails
     */
    public int getAbsentCount(String studentId) throws SQLException {
        List<Attendance> attendances = attendanceDAO.findByStudentId(studentId);
        return (int) attendances.stream()
                .filter(a -> !a.isPresent())
                .count();
    }

    public void updateAttendanceNote(String id, String newValue) {
        if (newValue == null || newValue.trim().isEmpty()) {
            return;
        }

        Optional<Attendance> attendanceOpt = attendanceDAO.findById(id);
        if (attendanceOpt.isPresent()) {
            Attendance attendance = attendanceOpt.get();
            attendance.setNote(newValue.trim());
            attendanceDAO.update(attendance);
        }
    }

    public void markAttendanceCalled(String id, Boolean newVal) {
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

    public Student getStudentById(String studentId) throws SQLException {
        if(studentId != null )
        return studentDAO.getStudentById(studentId);
        else
        return null;
    }
}
