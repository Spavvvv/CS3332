package src.dao;

import src.model.ClassSession;
import src.model.details.DetailsModel;
import src.model.details.DetailsModel.StudentGradeModel;
import src.model.person.Student;
import src.model.system.course.Course;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) class for handling storage and retrieval of DetailsModel data.
 * Manages its own database connections per public operation.
 * Modified to use String IDs for Class Sessions and use Course objects in ClassSession.
 */
public class DetailsDAO {

    private static final Logger LOGGER = Logger.getLogger(DetailsDAO.class.getName());

    /**
     * Constructor.
     */
    public DetailsDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Saves a DetailsModel instance into the database.
     * @param detailsModel The model to save.
     */
    public void saveDetails(DetailsModel detailsModel) {
        // Note: This method saves data to the 'details' table which stores course_name as a string.
        // This is consistent with the table structure inferred from the queries.
        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, room, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " + // Added room
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Added placeholder for room

        ClassSession session = detailsModel.getClassSession();
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to save details for a null ClassSession.");
            return;
        }
        // Assuming ClassSession.getId() provides the session ID (String) for the database table
        if (session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save details for a ClassSession with null or empty ID.");
            return; // Prevent saving if session ID is invalid
        }
        if (detailsModel.getStudentData() == null || detailsModel.getStudentData().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save details with no student data for session ID: " + session.getId());
            return; // No student data to save
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            for (StudentGradeModel student : detailsModel.getStudentData()) {
                if (student == null || student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping null or invalid StudentGradeModel in saveDetails for session ID: " + session.getId());
                    continue;
                }

                double gradeValue = 0.0; // Default grade value if parsing fails
                try {
                    gradeValue = Double.parseDouble(student.getGrade());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid grade format for student ID " + student.getStudentId() + " in session ID " + session.getId() + ": " + student.getGrade() + ". Using default grade 0.0.", e);
                    // gradeValue remains 0.0
                }

                String passStatus = (gradeValue >= 5.0) ? "Passed" : "Failed"; // Assuming >= 5.0 is passing
                String gradeLevel = student.getGradeLevel() != null && !student.getGradeLevel().trim().isEmpty()
                        ? student.getGradeLevel()
                        : DetailsModel.determineGradeLevel(gradeValue);

                String timeSlot = session.getTimeSlot() != null ? session.getTimeSlot() : "";
                String room = session.getRoom() != null ? session.getRoom() : "Unknown"; // Assuming ClassSession has getRoom()

                stmt.setString(1, session.getId());
                stmt.setString(2, session.getCourse() != null ? session.getCourse().getCourseName() : "Unknown Course");
                stmt.setString(3, session.getTeacher());
                stmt.setDate(4, session.getDate() != null ? Date.valueOf(session.getDate()) : null);
                stmt.setString(5, timeSlot);
                stmt.setString(6, room); // Set room
                stmt.setString(7, student.getStudentId());
                stmt.setString(8, student.getFullName());
                stmt.setDouble(9, gradeValue);
                stmt.setString(10, gradeLevel);
                stmt.setString(11, passStatus);
                stmt.setString(12, student.getNote());

                stmt.addBatch(); // Add to batch for efficiency
            }

            stmt.executeBatch(); // Execute all batched inserts
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving details batch for session ID: " + session.getId(), e);
        } catch (Exception e) { // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error saving details batch for session ID: " + session.getId(), e);
        }
    }

    /**
     * Gets a list of all session details (for reporting purposes)
     * @return List of session details
     */
    public List<DetailsModel> getAllSessions() {
        List<DetailsModel> allSessions = new ArrayList<>();

        String selectSessionsQuery = "SELECT DISTINCT class_session_id, course_name, teacher, session_date, time_slot, room " +
                "FROM details ORDER BY session_date DESC, time_slot ASC"; // Added room

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSessionsQuery);
             ResultSet resultSet = stmt.executeQuery()) {

            while (resultSet.next()) {
                String sessionId = resultSet.getString("class_session_id");
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping session entry with null or empty ID found in details table during getAllSessions.");
                    continue;
                }

                String courseName = resultSet.getString("course_name");
                String teacher = resultSet.getString("teacher");
                Date sessionSqlDate = resultSet.getDate("session_date");
                LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                String timeSlot = resultSet.getString("time_slot");
                String room = resultSet.getString("room"); // Retrieve room

                // Create a minimal Course object
                Course course = new Course();
                course.setCourseName(courseName);

                // Create ClassSession object using String ID and the created Course object
                ClassSession session = new ClassSession(
                        sessionId, course, teacher, room, sessionDate, timeSlot
                );

                // Load details for this session using the String ID
                DetailsModel details = loadSessionDetails(sessionId);

                if (details != null) {
                    allSessions.add(details);
                } else {
                    LOGGER.log(Level.INFO, "No student details found for session ID: " + sessionId + " via loadSessionDetails. Creating basic DetailsModel.");
                    allSessions.add(new DetailsModel(session));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all sessions from details.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting all sessions.", e);
        }

        return allSessions;
    }


    /**
     * Deletes a session by ID
     * @param sessionId Session ID to delete (String)
     * @return True if deletion was successful (at least one row deleted), false otherwise.
     */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete details for a null or empty session ID.");
            return false;
        }
        String deleteQuery = "DELETE FROM details WHERE class_session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

            stmt.setString(1, sessionId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting details for session ID: " + sessionId, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting session details for ID: " + sessionId, e);
            return false;
        }
    }

    /**
     * Updates a student's grade in a specific class session
     * @param classSession The class session containing the student (uses String ID)
     * @param studentId The ID of the student to update (String)
     * @param newGrade The new grade value
     * @param note Any notes about the grade
     * @return True if update was successful (at least one row updated), false otherwise.
     */
    public boolean updateStudentGrade(ClassSession classSession, String studentId, double newGrade, String note) {
        if (classSession == null || classSession.getId() == null || classSession.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update student grade for a null or invalid ClassSession.");
            return false;
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update student grade for a null or empty student ID in session ID: " + classSession.getId());
            return false;
        }
        if (note == null) { // Ensure note is not null for the statement
            note = "";
        }
        String updateQuery = "UPDATE details SET grade = ?, grade_level = ?, pass_status = ?, note = ? " +
                "WHERE class_session_id = ? AND student_id = ?";

        String gradeLevel = DetailsModel.determineGradeLevel(newGrade);
        boolean passed = newGrade >= 5.0; // Assuming >= 5.0 is passing
        String passStatus = passed ? "Passed" : "Failed";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            stmt.setDouble(1, newGrade);
            stmt.setString(2, gradeLevel);
            stmt.setString(3, passStatus);
            stmt.setString(4, note);
            stmt.setString(5, classSession.getId());
            stmt.setString(6, studentId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student grade for student ID " + studentId + " in session ID " + classSession.getId(), e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating student grade for student ID " + studentId + " in session ID " + classSession.getId(), e);
            return false;
        }
    }

    /**
     * Loads all class sessions and their details from the database into DetailsModel instances.
     * @return A list of DetailsModel objects.
     */
    public List<DetailsModel> loadAllDetails() {
        Map<String, DetailsModel> detailsMap = new HashMap<>();
        // Added 'room' to the select query and joined with students to get full_name reliably
        String selectQuery = "SELECT d.class_session_id, d.course_name, d.teacher, d.session_date, d.time_slot, d.room, " +
                "d.student_id, d.grade, d.grade_level, d.pass_status, d.note, s.full_name " + // Select student's full_name
                "FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "ORDER BY d.class_session_id, s.full_name"; // Order by student name or another stable key

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery);
             ResultSet resultSet = stmt.executeQuery()) {

            while (resultSet.next()) {
                String sessionId = resultSet.getString("class_session_id");
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping detail entry with null or empty session ID during loadAllDetails.");
                    continue;
                }

                DetailsModel detailsModel;
                if (detailsMap.containsKey(sessionId)) {
                    detailsModel = detailsMap.get(sessionId);
                } else {
                    // Extract session data for the new DetailsModel
                    String courseName = resultSet.getString("course_name");
                    String teacher = resultSet.getString("teacher");
                    Date sessionSqlDate = resultSet.getDate("session_date");
                    LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                    String timeSlot = resultSet.getString("time_slot");
                    String room = resultSet.getString("room");

                    Course course = new Course();
                    course.setCourseName(courseName);

                    ClassSession session = new ClassSession(
                            sessionId, course, teacher, room, sessionDate, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                    detailsMap.put(sessionId, detailsModel);
                }

                // Extract student data using the joined full_name
                String studentId = resultSet.getString("student_id");
                if (studentId == null || studentId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping detail entry with null/empty student ID for session ID " + sessionId + " during loadAllDetails.");
                    continue;
                }
                String studentName = resultSet.getString("full_name"); // Use name from students table
                if (studentName == null) { // Fallback if join didn't work or name is null in students table
                    studentName = "Unknown Student (ID: " + studentId + ")";
                }

                double grade = resultSet.getDouble("grade");
                String gradeLevel = resultSet.getString("grade_level");
                boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                String note = resultSet.getString("note");

                // Add student to model - STT is just for this loaded list, not a DB field
                int stt = detailsModel.getStudentData().size() + 1;
                detailsModel.getStudentData().add(
                        new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading all details from the database.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading all details.", e);
        }

        return new ArrayList<>(detailsMap.values());
    }

    /**
     * Loads DetailsModel for a specific class session.
     * @param sessionId The ID of the class session to load details for (String).
     * @return DetailsModel with student grades for the specified session, or null if not found.
     */
    public DetailsModel loadSessionDetails(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to load session details for a null or empty session ID.");
            return null;
        }
        DetailsModel detailsModel = null;
        // Added 'room' to the select query and joined with students for full_name
        String selectQuery = "SELECT d.class_session_id, d.course_name, d.teacher, d.session_date, d.time_slot, d.room, " +
                "d.student_id, d.grade, d.grade_level, d.pass_status, d.note, s.full_name " + // Select student's full_name
                "FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.class_session_id = ? ORDER BY s.full_name"; // Order by student name

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, sessionId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                int stt = 1;

                while (resultSet.next()) {
                    if (detailsModel == null) {
                        // Extract session data - Use the input sessionId
                        String currentSessionId = resultSet.getString("class_session_id"); // Should match input sessionId due to WHERE clause
                        if (currentSessionId == null || currentSessionId.trim().isEmpty()) {
                            LOGGER.log(Level.WARNING, "Found detail entry with null/empty session ID during loadSessionDetails for ID: " + sessionId + ". Skipping this entry.");
                            continue;
                        }
                        String courseName = resultSet.getString("course_name");
                        String teacher = resultSet.getString("teacher");
                        Date sessionSqlDate = resultSet.getDate("session_date");
                        LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                        String timeSlot = resultSet.getString("time_slot");
                        String room = resultSet.getString("room");

                        Course course = new Course();
                        course.setCourseName(courseName);

                        ClassSession session = new ClassSession(
                                currentSessionId, course, teacher, room, sessionDate, timeSlot
                        );
                        detailsModel = new DetailsModel(session);
                    }

                    // Extract student data using the joined full_name
                    String studentId = resultSet.getString("student_id");
                    if (studentId == null || studentId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Skipping detail entry with null/empty student ID for session ID " + sessionId + " during loadSessionDetails.");
                        continue;
                    }
                    String studentName = resultSet.getString("full_name"); // Use name from students table
                    if (studentName == null) { // Fallback if join didn't work or name is null in students table
                        studentName = "Unknown Student (ID: " + studentId + ")";
                    }

                    double grade = resultSet.getDouble("grade");
                    String gradeLevel = resultSet.getString("grade_level");
                    boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                    String note = resultSet.getString("note");

                    detailsModel.getStudentData().add(
                            new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                    );
                    stt++;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading session details for ID: " + sessionId, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading session details for ID: " + sessionId, e);
            return null;
        }

        return detailsModel;
    }

    /**
     * Loads DetailsModel for a specific course on a specific date.
     * Assumes a course/date combination uniquely identifies a session, which might not be true
     * if multiple sessions for the same course occur on the same day.
     * @param courseName The name of the course (String).
     * @param date The date of the class session.
     * @return DetailsModel with student grades for the specified course and date, or null if not found.
     */
    public DetailsModel loadDetailsByNameAndDate(String courseName, LocalDate date) {
        if (courseName == null || courseName.trim().isEmpty() || date == null) {
            LOGGER.log(Level.WARNING, "Attempted to load details by null/empty course name or null date.");
            return null;
        }
        DetailsModel detailsModel = null;
        // Added 'room' to the select query and joined with students for full_name
        String selectQuery = "SELECT d.class_session_id, d.course_name, d.teacher, d.session_date, d.time_slot, d.room, " +
                "d.student_id, d.grade, d.grade_level, d.pass_status, d.note, s.full_name " + // Select student's full_name
                "FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.course_name = ? AND d.session_date = ? " +
                "ORDER BY s.full_name"; // Order by student name

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, courseName);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet resultSet = stmt.executeQuery()) {
                int stt = 1;

                while (resultSet.next()) {
                    if (detailsModel == null) {
                        // Extract session data
                        String sessionId = resultSet.getString("class_session_id");
                        if (sessionId == null || sessionId.trim().isEmpty()) {
                            LOGGER.log(Level.WARNING, "Found detail entry with null/empty session ID during loadDetailsByNameAndDate for course: " + courseName + ", date: " + date + ". Skipping this entry.");
                            continue;
                        }
                        String teacher = resultSet.getString("teacher");
                        String timeSlot = resultSet.getString("time_slot");
                        String room = resultSet.getString("room");

                        Course course = new Course();
                        course.setCourseName(courseName);

                        ClassSession session = new ClassSession(
                                sessionId, course, teacher, room, date, timeSlot
                        );
                        detailsModel = new DetailsModel(session);
                    }

                    // Extract student data using the joined full_name
                    String studentId = resultSet.getString("student_id");
                    if (studentId == null || studentId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Skipping detail entry with null/empty student ID for course: " + courseName + ", date: " + date + " during loadDetailsByNameAndDate.");
                        continue;
                    }
                    String studentName = resultSet.getString("full_name"); // Use name from students table
                    if (studentName == null) { // Fallback if join didn't work or name is null in students table
                        studentName = "Unknown Student (ID: " + studentId + ")";
                    }
                    double grade = resultSet.getDouble("grade");
                    String gradeLevel = resultSet.getString("grade_level");
                    boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                    String note = resultSet.getString("note");

                    detailsModel.getStudentData().add(
                            new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                    );
                    stt++;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading details by name and date for course: " + courseName + ", date: " + date, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading details by name and date for course: " + courseName + ", date: " + date, e);
            return null;
        }

        return detailsModel;
    }

    /**
     * Deletes details for a specific class session.
     * @param classSession The ClassSession to delete details for (uses String ID).
     */
    public void deleteDetails(ClassSession classSession) {
        if (classSession == null || classSession.getId() == null || classSession.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete details for a null or invalid ClassSession.");
            return;
        }
        String deleteQuery = "DELETE FROM details WHERE class_session_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

            stmt.setString(1, classSession.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting details for ClassSession ID: " + classSession.getId(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting details for ClassSession ID: " + classSession.getId(), e);
        }
    }

    /**
     * Updates grade and note data for a specific student in a class session.
     * @param classSession The ClassSession containing the student (uses String ID).
     * @param studentModel The StudentGradeModel containing updated data (uses String Student ID).
     */
    public void updateStudentDetails(ClassSession classSession, StudentGradeModel studentModel) {
        if (classSession == null || classSession.getId() == null || classSession.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update student details for a null or invalid ClassSession.");
            return;
        }
        if (studentModel == null || studentModel.getStudentId() == null || studentModel.getStudentId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update null StudentGradeModel or student with null/empty ID for session ID: " + classSession.getId());
            return;
        }
        if (studentModel.getNote() == null) { // Ensure note is not null for the statement
            studentModel.setNote("");
        }
        String updateQuery = "UPDATE details SET grade = ?, grade_level = ?, pass_status = ?, note = ? " +
                "WHERE class_session_id = ? AND student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            double grade = 0.0;
            try {
                grade = Double.parseDouble(studentModel.getGrade());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid grade format for student ID " + studentModel.getStudentId() + " in session ID " + classSession.getId() + " during update: " + studentModel.getGrade() + ". Using default grade 0.0.", e);
                // grade remains 0.0
            }

            String passStatus = (grade >= 5.0) ? "Passed" : "Failed";
            String gradeLevel = studentModel.getGradeLevel() != null && !studentModel.getGradeLevel().trim().isEmpty()
                    ? studentModel.getGradeLevel()
                    : DetailsModel.determineGradeLevel(grade);

            stmt.setDouble(1, grade);
            stmt.setString(2, gradeLevel);
            stmt.setString(3, passStatus);
            stmt.setString(4, studentModel.getNote());
            stmt.setString(5, classSession.getId());
            stmt.setString(6, studentModel.getStudentId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating student details for student ID " + studentModel.getStudentId() + " in session ID " + classSession.getId(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating student details for student ID " + studentModel.getStudentId() + " in session ID " + classSession.getId(), e);
        }
    }

    /**
     * Initializes details for all students in a class session with default grade values.
     * @param classSession The ClassSession to initialize (uses String ID).
     * @param students The list of students to add to the session (uses String IDs).
     * @return True if initialization was successful for at least one student, false otherwise.
     */
    public boolean initializeSessionDetails(ClassSession classSession, List<Student> students) {
        if (classSession == null || classSession.getId() == null || classSession.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to initialize details for a null or invalid ClassSession.");
            return false;
        }
        if (students == null || students.isEmpty()) {
            LOGGER.log(Level.INFO, "Attempted to initialize details for session ID " + classSession.getId() + " with no students.");
            return true; // Nothing to initialize, considered success
        }

        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, room, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean success = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            for (Student student : students) {
                if (student == null || student.getId() == null || student.getId().trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping null or invalid student during initialization for session ID: " + classSession.getId());
                    continue;
                }
                if (student.getName() == null) {
                    LOGGER.log(Level.WARNING, "Student with ID " + student.getId() + " has a null name during initialization for session ID: " + classSession.getId() + ". Using 'Unknown Student'.");
                }

                double defaultGrade = 0.0;
                String gradeLevel = DetailsModel.determineGradeLevel(defaultGrade);

                String courseName = classSession.getCourse() != null && classSession.getCourse().getCourseName() != null
                        ? classSession.getCourse().getCourseName() : "Unknown Course";
                String timeSlot = classSession.getTimeSlot() != null ? classSession.getTimeSlot() : "";
                String room = classSession.getRoom() != null ? classSession.getRoom() : "Unknown";

                stmt.setString(1, classSession.getId());
                stmt.setString(2, courseName);
                stmt.setString(3, classSession.getTeacher());
                stmt.setDate(4, classSession.getDate() != null ? Date.valueOf(classSession.getDate()) : null);
                stmt.setString(5, timeSlot);
                stmt.setString(6, room);
                stmt.setString(7, student.getId());
                stmt.setString(8, student.getName() != null ? student.getName() : "Unknown Student");
                stmt.setDouble(9, defaultGrade);
                stmt.setString(10, gradeLevel);
                stmt.setString(11, "Failed");
                stmt.setString(12, ""); // Default note

                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            for (int count : results) {
                if (count > 0) {
                    success = true; // At least one row was affected
                    break; // No need to check further if one succeeded
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing details batch for session ID: " + classSession.getId(), e);
            success = false; // Mark as failed if batch execution fails
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error initializing details batch for session ID: " + classSession.getId(), e);
            success = false;
        }
        return success;
    }


    /**
     * Gets a list of students with their average grades for a specific course.
     * @param courseName The name of the course (String).
     * @return A list of student models with average grades.
     */
    public List<StudentGradeModel> getStudentAverageGrades(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get student average grades with null or empty course name.");
            return new ArrayList<>();
        }
        List<StudentGradeModel> studentAverages = new ArrayList<>();
        // Join with students table to get full_name reliably
        String selectQuery = "SELECT d.student_id, s.full_name, AVG(d.grade) as avg_grade, " +
                "COUNT(d.grade) as grade_count " +
                "FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.course_name = ? " +
                "GROUP BY d.student_id, s.full_name " + // Group by student_id and full_name
                "HAVING COUNT(d.grade) > 0 " + // Only include students with at least one grade
                "ORDER BY s.full_name"; // Order by student name

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, courseName);

            try (ResultSet resultSet = stmt.executeQuery()) {
                int stt = 1;

                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    if (studentId == null || studentId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Skipping student with null/empty ID in average grades for course: " + courseName + " found in query result.");
                        continue;
                    }
                    String studentName = resultSet.getString("full_name"); // Use name from students table
                    if (studentName == null) { // Fallback if join didn't work or name is null in students table
                        studentName = "Unknown Student (ID: " + studentId + ")";
                    }

                    double avgGrade = resultSet.getDouble("avg_grade");
                    int gradeCount = resultSet.getInt("grade_count"); // This will be > 0 due to HAVING clause

                    String gradeLevel = DetailsModel.determineGradeLevel(avgGrade);
                    boolean passed = avgGrade >= 5.0;
                    String note = "Average grade from " + gradeCount + " session" + (gradeCount > 1 ? "s" : "");

                    StudentGradeModel studentModel = new StudentGradeModel(
                            stt, studentName, studentId, avgGrade, gradeLevel, passed, note
                    );

                    studentAverages.add(studentModel);
                    stt++;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student average grades for course: " + courseName, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error getting student average grades for course: " + courseName, e);
        }

        return studentAverages;
    }

    /**
     * Gets a list of students with their final grades for a specific course,
     * taking into account the weight of each session.
     * @param courseName The name of the course (String).
     * @param weights Map of session weights (String sessionId -> Double weight)
     * @return A list of student models with weighted final grades.
     */
    public List<StudentGradeModel> getStudentFinalGrades(String courseName, Map<String, Double> weights) {
        if (courseName == null || courseName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get student final grades with null or empty course name.");
            return new ArrayList<>();
        }
        if (weights == null) {
            LOGGER.log(Level.WARNING, "Attempted to get student final grades with null weights map for course: " + courseName + ". Using empty map.");
            weights = new HashMap<>();
        }

        Map<String, Map<String, Double>> studentGrades = new HashMap<>();
        Map<String, String> studentNames = new HashMap<>();
        // Join with students table for full_name
        String selectQuery = "SELECT d.class_session_id, d.student_id, s.full_name, d.grade " +
                "FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.course_name = ? ";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, courseName);

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    if (studentId == null || studentId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Skipping student with null/empty ID in final grades query for course: " + courseName + " found in query result.");
                        continue;
                    }
                    String sessionId = resultSet.getString("class_session_id");
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Skipping detail entry with null/empty session ID in final grades query for course: " + courseName + ", student ID: " + studentId);
                        continue;
                    }
                    double grade = resultSet.getDouble("grade");
                    String studentName = resultSet.getString("full_name"); // Use name from students table
                    if (studentName == null) { // Fallback
                        studentName = "Unknown Student (ID: " + studentId + ")";
                    }

                    studentNames.put(studentId, studentName);

                    if (!studentGrades.containsKey(studentId)) {
                        studentGrades.put(studentId, new HashMap<>());
                    }

                    studentGrades.get(studentId).put(sessionId, grade);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving student grades for final grade calculation for course: " + courseName, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving student grades for final grade calculation for course: " + courseName, e);
        }

        List<StudentGradeModel> finalGrades = new ArrayList<>();
        int stt = 1;

        for (Map.Entry<String, Map<String, Double>> entry : studentGrades.entrySet()) {
            String studentId = entry.getKey();
            Map<String, Double> grades = entry.getValue();
            String studentName = studentNames.getOrDefault(studentId, "Unknown Student (ID: " + studentId + ")");

            double totalWeight = 0.0;
            double weightedSum = 0.0;
            int sessionCount = 0;

            for (Map.Entry<String, Double> gradeEntry : grades.entrySet()) {
                String sessionId = gradeEntry.getKey();
                double grade = gradeEntry.getValue();
                double weight = weights.getOrDefault(sessionId, 1.0);

                if (weight > 0) {
                    weightedSum += grade * weight;
                    totalWeight += weight;
                    sessionCount++;
                } else {
                    LOGGER.log(Level.FINE, "Skipping session ID " + sessionId + " for student ID " + studentId + " in final grade calculation due to zero weight.");
                }
            }

            if (totalWeight > 0) {
                double finalGrade = weightedSum / totalWeight;
                String gradeLevel = DetailsModel.determineGradeLevel(finalGrade);
                boolean passed = finalGrade >= 5.0;
                String note = "Weighted final grade from " + sessionCount + " session" + (sessionCount > 1 ? "s" : "");

                StudentGradeModel studentModel = new StudentGradeModel(
                        stt, studentName, studentId, finalGrade, gradeLevel, passed, note
                );

                finalGrades.add(studentModel);
                stt++;
            } else if (sessionCount > 0) {
                LOGGER.log(Level.WARNING, "Total weight is zero for student ID " + studentId + " in course " + courseName + ", but student attended " + sessionCount + " sessions.");
            } else {
                LOGGER.log(Level.FINE, "No weighted sessions found for student ID " + studentId + " in course " + courseName + " for final grade calculation.");
            }
        }

        return finalGrades;
    }

    /**
     * Helper method to find a student's name by ID from the details table or joined students table.
     * Note: The other methods now primarily rely on the JOIN with the students table for the most current name.
     * This method is kept but less critical if JOINs are consistently used.
     * @param studentId The ID of the student (String).
     * @return The student's name or "Unknown Student".
     */
    private String resultSetFindStudentName(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find student name with null or empty student ID.");
            return "Unknown Student";
        }
        // Prefer name from students table if possible
        String selectQuery = "SELECT s.full_name FROM students s WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, studentId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("full_name");
                    if (name != null && !name.trim().isEmpty()) {
                        return name;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student name from students table for ID: " + studentId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding student name from students table for ID: " + studentId, e);
        }

        // Fallback to details table if name not found in students table (less reliable)
        selectQuery = "SELECT student_name FROM details WHERE student_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectQuery)) {

            stmt.setString(1, studentId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("student_name");
                    if (name != null && !name.trim().isEmpty()) {
                        LOGGER.log(Level.INFO, "Found student name '" + name + "' in details table as fallback for ID: " + studentId);
                        return name;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student name from details table as fallback for ID: " + studentId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding student name from details table as fallback for ID: " + studentId, e);
        }


        return "Unknown Student (ID: " + studentId + ")";
    }

    public boolean exportToExcel(String id) {
        return false;
    }
}
