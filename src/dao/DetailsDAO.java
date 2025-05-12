package src.dao;

import src.model.ClassSession;
import src.model.details.DetailsModel;
import src.model.details.DetailsModel.StudentGradeModel;
import src.model.person.Student;
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
 * Uses DatabaseConnection for managing connections and executing queries.
 * Modified to use String IDs for Class Sessions.
 */
public class DetailsDAO {

    private static final Logger LOGGER = Logger.getLogger(DetailsDAO.class.getName());

    /**
     * Saves a DetailsModel instance into the database.
     * @param detailsModel The model to save.
     */
    public void saveDetails(DetailsModel detailsModel) {
        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        ClassSession session = detailsModel.getClassSession();
        if (session == null) {
            LOGGER.log(Level.WARNING, "Attempted to save details for a null ClassSession.");
            return;
        }
        if (session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save details for a ClassSession with null or empty ID. Course: " + session.getCourseName());
            return;
        }
        if (detailsModel.getStudentData() == null || detailsModel.getStudentData().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save details with no student data for session ID: " + session.getId());
            return;
        }


        for (StudentGradeModel student : detailsModel.getStudentData()) {
            if (student == null) {
                LOGGER.log(Level.WARNING, "Skipping null StudentGradeModel in saveDetails for session ID: " + session.getId());
                continue;
            }
            if (student.getStudentId() == null || student.getStudentId().trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Skipping StudentGradeModel with null or empty student ID for session ID: " + session.getId() + ". Student Name: " + student.getFullName());
                continue;
            }

            try {
                // Assuming getGrade() can return a String that can be parsed to double
                double gradeValue = 0.0; // Default grade value if parsing fails
                try {
                    gradeValue = Double.parseDouble(student.getGrade());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Invalid grade format for student ID " + student.getStudentId() + " in session ID " + session.getId() + ": " + student.getGrade(), e);
                    // Use default gradeValue of 0.0
                }

                String passStatus = student.getPass() != null && student.getPass().equals("✓") ? "Passed" : "Failed";
                String gradeLevel = student.getGradeLevel() != null ? student.getGradeLevel() : DetailsModel.determineGradeLevel(gradeValue); // Use determined level if model doesn't provide

                DatabaseConnection.executeUpdate(
                        insertQuery,
                        session.getId(), // Use String ID
                        session.getCourseName(),
                        session.getTeacher(),
                        session.getDate() != null ? Date.valueOf(session.getDate()) : null,
                        session.getTimeSlot(),
                        student.getStudentId(),
                        student.getFullName(),
                        gradeValue,
                        gradeLevel,
                        passStatus,
                        student.getNote()
                );
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error saving details for student ID " + student.getStudentId() + " in session ID " + session.getId(), e);
            } catch (Exception e) { // Catch any other unexpected exceptions
                LOGGER.log(Level.SEVERE, "Unexpected error saving details for student ID " + student.getStudentId() + " in session ID " + session.getId(), e);
            }
        }
    }

    /**
     * Gets a list of all session details (for reporting purposes)
     * @return List of session details
     */
    public List<DetailsModel> getAllSessions() {
        List<DetailsModel> allSessions = new ArrayList<>();
        // Use String as key for session ID map
        Map<String, DetailsModel> sessionMap = new HashMap<>(); // Changed Map key to String

        // Selecting distinct sessions first to create the base DetailsModel objects
        String selectSessionsQuery = "SELECT DISTINCT class_session_id, course_name, teacher, session_date, time_slot " +
                "FROM details ORDER BY session_date DESC, time_slot ASC";

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectSessionsQuery)) {

            while (resultSet.next()) {
                // Retrieve session ID as String
                String sessionId = resultSet.getString("class_session_id"); // Changed to getString
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping session with null or empty ID found in details table.");
                    continue;
                }

                String courseName = resultSet.getString("course_name");
                String teacher = resultSet.getString("teacher");
                Date sessionSqlDate = resultSet.getDate("session_date");
                LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                String timeSlot = resultSet.getString("time_slot");

                // Create ClassSession object using String ID
                ClassSession session = new ClassSession(
                        sessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                );

                // Load details for this session using the String ID
                DetailsModel details = loadSessionDetails(sessionId);
                if (details != null) {
                    allSessions.add(details);
                } else {
                    // If loadSessionDetails returns null (e.g., no students found), create a basic model
                    LOGGER.log(Level.INFO, "No student details found for session ID: " + sessionId + ". Creating basic DetailsModel.");
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
     * @return True if deletion was successful
     */
    public boolean deleteSession(String sessionId) { // Changed parameter type to String
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete details for a null or empty session ID.");
            return false;
        }
        String deleteQuery = "DELETE FROM details WHERE class_session_id = ?";

        try {
            int rowsAffected = DatabaseConnection.executeUpdate(deleteQuery, sessionId); // Pass String ID
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
     * @return True if update was successful
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
        String updateQuery = "UPDATE details SET grade = ?, grade_level = ?, pass_status = ?, note = ? " +
                "WHERE class_session_id = ? AND student_id = ?";

        String gradeLevel = DetailsModel.determineGradeLevel(newGrade);
        boolean passed = newGrade >= 5.0;
        String passStatus = passed ? "Passed" : "Failed";

        try {
            int rowsAffected = DatabaseConnection.executeUpdate(
                    updateQuery,
                    newGrade,
                    gradeLevel,
                    passStatus,
                    note,
                    classSession.getId(), // Use String ID
                    studentId
            );

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
        // Use String as key for session ID map
        Map<String, DetailsModel> detailsMap = new HashMap<>(); // Changed Map key to String
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "ORDER BY d.class_session_id, d.student_id";

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery)) {

            while (resultSet.next()) {
                // Extract session data using String ID
                String sessionId = resultSet.getString("class_session_id"); // Changed to getString
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping detail entry with null or empty session ID.");
                    continue;
                }
                String courseName = resultSet.getString("course_name");
                String teacher = resultSet.getString("teacher");
                Date sessionSqlDate = resultSet.getDate("session_date");
                LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                String timeSlot = resultSet.getString("time_slot");

                // Create or get DetailsModel using String ID
                DetailsModel detailsModel;
                if (detailsMap.containsKey(sessionId)) {
                    detailsModel = detailsMap.get(sessionId);
                } else {
                    // Create new ClassSession with the available data using String ID
                    ClassSession session = new ClassSession(
                            sessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                    // Put into map using String ID
                    detailsMap.put(sessionId, detailsModel); // Changed Map key to String
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name"); // Assuming 'student_name' column exists or is handled by join
                if (studentName == null && resultSet.getString("full_name") != null) {
                    studentName = resultSet.getString("full_name"); // Use student name from join if available
                } else if (studentName == null) {
                    studentName = "Unknown Student"; // Default name if neither is available
                }

                double grade = resultSet.getDouble("grade");
                String gradeLevel = resultSet.getString("grade_level");
                boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                String note = resultSet.getString("note");

                // Add student to model - using the count of current students as STT (order number)
                // This calculation of STT is not guaranteed to be stable across reloads
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
    public DetailsModel loadSessionDetails(String sessionId) { // Parameter type is String
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to load session details for a null or empty session ID.");
            return null;
        }
        DetailsModel detailsModel = null;
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.class_session_id = ? ORDER BY d.student_id";

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, sessionId)) { // Pass String ID
            int stt = 1;

            while (resultSet.next()) {
                if (detailsModel == null) {
                    // Extract session data - Use the input sessionId which is String
                    // The other session data is redundant but retrieved for completeness or if input sessionId is not guaranteed valid
                    String currentSessionId = resultSet.getString("class_session_id"); // Retrieve as String
                    if (currentSessionId == null || currentSessionId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Found detail entry with null/empty session ID during loadSessionDetails for ID: " + sessionId);
                        // Decide how to handle - maybe skip this row or log and continue
                        continue; // Skip this detail row
                    }
                    String courseName = resultSet.getString("course_name");
                    String teacher = resultSet.getString("teacher");
                    Date sessionSqlDate = resultSet.getDate("session_date");
                    LocalDate sessionDate = sessionSqlDate != null ? sessionSqlDate.toLocalDate() : null;
                    String timeSlot = resultSet.getString("time_slot");

                    // Create ClassSession using String ID
                    ClassSession session = new ClassSession(
                            currentSessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
                if (studentName == null && resultSet.getString("full_name") != null) {
                    studentName = resultSet.getString("full_name");
                } else if (studentName == null) {
                    studentName = "Unknown Student";
                }
                double grade = resultSet.getDouble("grade");
                String gradeLevel = resultSet.getString("grade_level");
                boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                String note = resultSet.getString("note");

                // Add student to model
                detailsModel.getStudentData().add(
                        new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                );
                stt++;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading session details for ID: " + sessionId, e);
            return null; // Return null or throw exception on error
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
     * @param courseName The name of the course.
     * @param date The date of the class session.
     * @return DetailsModel with student grades for the specified course and date, or null if not found.
     */
    public DetailsModel loadDetailsByNameAndDate(String courseName, LocalDate date) {
        if (courseName == null || courseName.trim().isEmpty() || date == null) {
            LOGGER.log(Level.WARNING, "Attempted to load details by null/empty course name or null date.");
            return null;
        }
        DetailsModel detailsModel = null;
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.course_name = ? AND d.session_date = ? " +
                "ORDER BY d.student_id";

        try (ResultSet resultSet = DatabaseConnection.executeQuery(
                selectQuery, courseName, Date.valueOf(date)
        )) {
            int stt = 1;

            while (resultSet.next()) {
                if (detailsModel == null) {
                    // Extract session data using String ID
                    String sessionId = resultSet.getString("class_session_id"); // Changed to getString
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        LOGGER.log(Level.WARNING, "Found detail entry with null/empty session ID during loadDetailsByNameAndDate for course: " + courseName + ", date: " + date);
                        continue; // Skip this detail row
                    }
                    String teacher = resultSet.getString("teacher");
                    String timeSlot = resultSet.getString("time_slot");

                    // Create ClassSession using String ID
                    ClassSession session = new ClassSession(
                            sessionId, courseName, teacher, "Unknown", date, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
                if (studentName == null && resultSet.getString("full_name") != null) {
                    studentName = resultSet.getString("full_name");
                } else if (studentName == null) {
                    studentName = "Unknown Student";
                }
                double grade = resultSet.getDouble("grade");
                String gradeLevel = resultSet.getString("grade_level");
                boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                String note = resultSet.getString("note");

                // Add student to model
                detailsModel.getStudentData().add(
                        new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                );
                stt++;
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

        try {
            DatabaseConnection.executeUpdate(deleteQuery, classSession.getId()); // Pass String ID
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
        String updateQuery = "UPDATE details SET grade = ?, grade_level = ?, pass_status = ?, note = ? " +
                "WHERE class_session_id = ? AND student_id = ?";

        try {
            // Assuming getGrade() returns a String that can be parsed to double
            double grade = 0.0;
            try {
                grade = Double.parseDouble(studentModel.getGrade());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid grade format for student ID " + studentModel.getStudentId() + " in session ID " + classSession.getId() + " during update: " + studentModel.getGrade(), e);
                // Use default grade 0.0
            }

            String passStatus = studentModel.getPass() != null && studentModel.getPass().equals("✓") ? "Passed" : "Failed";
            String gradeLevel = studentModel.getGradeLevel() != null ? studentModel.getGradeLevel() : DetailsModel.determineGradeLevel(grade);


            DatabaseConnection.executeUpdate(
                    updateQuery,
                    grade,
                    gradeLevel,
                    passStatus,
                    studentModel.getNote(),
                    classSession.getId(), // Use String ID
                    studentModel.getStudentId() // Use String ID
            );
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
            LOGGER.log(Level.WARNING, "Attempted to initialize details for session ID " + classSession.getId() + " with no students.");
            // Depending on requirements, returning true might be acceptable if no students means nothing to initialize
            return true; // Or false if initialization must involve students
        }

        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        boolean success = false; // Track if any student details were successfully inserted
        for (Student student : students) {
            if (student == null || student.getId() == null || student.getId().trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Skipping null or invalid student during initialization for session ID: " + classSession.getId());
                continue;
            }
            try {
                // Default grade is 0.0
                double defaultGrade = 0.0;
                String gradeLevel = DetailsModel.determineGradeLevel(defaultGrade);

                DatabaseConnection.executeUpdate(
                        insertQuery,
                        classSession.getId(), // Use String ID
                        classSession.getCourseName(),
                        classSession.getTeacher(),
                        classSession.getDate() != null ? Date.valueOf(classSession.getDate()) : null,
                        classSession.getTimeSlot(),
                        student.getId(), // Use String ID
                        student.getName(),
                        defaultGrade,
                        gradeLevel,
                        "Failed", // Default pass status
                        "" // Default note
                );
                success = true; // At least one student was inserted successfully
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error initializing details for student ID " + student.getId() + " in session ID " + classSession.getId(), e);
                // Decide whether to continue or break on error. Continuing allows partial initialization.
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error initializing details for student ID " + student.getId() + " in session ID " + classSession.getId(), e);
            }
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
        String selectQuery = "SELECT d.student_id, d.student_name, AVG(d.grade) as avg_grade, " +
                "COUNT(d.grade) as grade_count " +
                "FROM details d " +
                "WHERE d.course_name = ? " +
                "GROUP BY d.student_id, d.student_name " +
                "ORDER BY d.student_name"; // Added ordering

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, courseName)) {
            int stt = 1;

            while (resultSet.next()) {
                String studentId = resultSet.getString("student_id"); // Retrieve as String
                if (studentId == null || studentId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping student with null/empty ID in average grades for course: " + courseName);
                    continue;
                }
                String studentName = resultSet.getString("student_name");
                if (studentName == null) studentName = "Unknown Student";

                double avgGrade = resultSet.getDouble("avg_grade");
                int gradeCount = resultSet.getInt("grade_count");

                if (gradeCount > 0) {
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
     * @param weights Map of session weights (String sessionId -> Double weight) - Changed key type to String
     * @return A list of student models with weighted final grades.
     */
    public List<StudentGradeModel> getStudentFinalGrades(String courseName, Map<String, Double> weights) { // Changed Map key type to String
        if (courseName == null || courseName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to get student final grades with null or empty course name.");
            return new ArrayList<>();
        }
        if (weights == null) {
            LOGGER.log(Level.WARNING, "Attempted to get student final grades with null weights map for course: " + courseName);
            weights = new HashMap<>(); // Use an empty map if null
        }

        // First get all grades for the course
        // Use String for session ID key
        Map<String, Map<String, Double>> studentGrades = new HashMap<>(); // Changed inner Map key to String
        Map<String, String> studentNames = new HashMap<>();
        String selectQuery = "SELECT d.class_session_id, d.student_id, d.student_name, d.grade " +
                "FROM details d " +
                "WHERE d.course_name = ? ";

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, courseName)) {

            while (resultSet.next()) {
                String studentId = resultSet.getString("student_id"); // Retrieve as String
                if (studentId == null || studentId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping student with null/empty ID in final grades query for course: " + courseName);
                    continue;
                }
                String sessionId = resultSet.getString("class_session_id"); // Changed to getString
                if (sessionId == null || sessionId.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping detail entry with null/empty session ID in final grades query for course: " + courseName + ", student ID: " + studentId);
                    continue;
                }
                double grade = resultSet.getDouble("grade");
                String studentName = resultSet.getString("student_name");
                if (studentName == null) studentName = "Unknown Student";


                // Store student name
                studentNames.put(studentId, studentName);

                // Initialize student entry if not exists
                if (!studentGrades.containsKey(studentId)) {
                    // Use String for the session ID key
                    studentGrades.put(studentId, new HashMap<>()); // Changed inner Map key to String
                }

                // Store grade using String session ID
                studentGrades.get(studentId).put(sessionId, grade); // Put String session ID
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving student grades for final grade calculation for course: " + courseName, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving student grades for final grade calculation for course: " + courseName, e);
        }


        // Calculate weighted grades
        List<StudentGradeModel> finalGrades = new ArrayList<>();
        int stt = 1;

        // Iterate through students
        for (Map.Entry<String, Map<String, Double>> entry : studentGrades.entrySet()) { // Changed inner Map key to String
            String studentId = entry.getKey();
            // Use String for session ID key
            Map<String, Double> grades = entry.getValue(); // Changed Map key to String
            String studentName = studentNames.getOrDefault(studentId, "Unknown Student");

            double totalWeight = 0.0;
            double weightedSum = 0.0;
            int sessionCount = 0; // Track how many sessions contribute

            // Iterate through session grades for the student
            for (Map.Entry<String, Double> gradeEntry : grades.entrySet()) { // Changed Map key to String
                String sessionId = gradeEntry.getKey(); // Get String session ID
                double grade = gradeEntry.getValue();
                double weight = weights.getOrDefault(sessionId, 1.0); // Get weight using String session ID

                weightedSum += grade * weight;
                totalWeight += weight;
                sessionCount++;
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
                // If total weight is 0 but there are sessions, handle this case (e.g., all weights were 0)
                LOGGER.log(Level.WARNING, "Total weight is zero for student ID " + studentId + " in course " + courseName + " with " + sessionCount + " sessions.");
                // You might want to add the student with a default or zero grade, or skip them.
                // For now, they are skipped as totalWeight > 0 condition is not met.
            } else {
                // No sessions found for this student in this course
                LOGGER.log(Level.INFO, "No sessions found for student ID " + studentId + " in course " + courseName + " for final grade calculation.");
            }
        }

        return finalGrades;
    }


    /**
     * Helper method to find a student's name by ID from the details table.
     * Note: Relying solely on details table might be inconsistent if student names change.
     * Joining with students table (as done in load methods) is more robust.
     * This method seems redundant given the joins in other methods.
     */
    private String resultSetFindStudentName(String studentId) { // Parameter is String
        if (studentId == null || studentId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find student name with null or empty student ID.");
            return "Unknown Student";
        }
        String selectQuery = "SELECT student_name FROM details WHERE student_id = ? LIMIT 1"; // Get name from any entry

        try (ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, studentId)) { // Pass String ID
            if (resultSet.next()) {
                String name = resultSet.getString("student_name");
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding student name from details for ID: " + studentId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding student name from details for ID: " + studentId, e);
        }
        // Fallback if not found or name is null/empty in details
        return "Unknown Student";
    }
}
