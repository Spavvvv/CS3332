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

/**
 * Data Access Object (DAO) class for handling storage and retrieval of DetailsModel data.
 * Uses DatabaseConnection for managing connections and executing queries.
 */
public class DetailsDAO {

    /**
     * Saves a DetailsModel instance into the database.
     * @param detailsModel The model to save.
     */
    public void saveDetails(DetailsModel detailsModel) {
        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        ClassSession session = detailsModel.getClassSession();
        if (session != null) {
            int stt = 1;
            for (StudentGradeModel student : detailsModel.getStudentData()) {
                try {
                    double gradeValue = Double.parseDouble(student.getGrade());
                    String passStatus = student.getPass().equals("✓") ? "Passed" : "Failed";

                    DatabaseConnection.executeUpdate(
                            insertQuery,
                            session.getId(),
                            session.getCourseName(),
                            session.getTeacher(),
                            Date.valueOf(session.getDate()),
                            session.getTimeSlot(),
                            student.getStudentId(),
                            student.getFullName(),
                            gradeValue,
                            student.getGradeLevel(),
                            passStatus,
                            student.getNote()
                    );
                } catch (SQLException | NumberFormatException e) {
                    e.printStackTrace();
                }
                stt++;
            }
        }
    }

    /**
     * Gets a list of all session details (for reporting purposes)
     * @return List of session details
     */
    public List<DetailsModel> getAllSessions() {
        List<DetailsModel> allSessions = new ArrayList<>();
        Map<Long, DetailsModel> sessionMap = new HashMap<>();

        String selectQuery = "SELECT DISTINCT class_session_id, course_name, teacher, session_date, time_slot " +
                "FROM details ORDER BY session_date DESC, time_slot ASC";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery);

            while (resultSet.next()) {
                long sessionId = resultSet.getLong("class_session_id");
                String courseName = resultSet.getString("course_name");
                String teacher = resultSet.getString("teacher");
                LocalDate sessionDate = resultSet.getDate("session_date").toLocalDate();
                String timeSlot = resultSet.getString("time_slot");

                // Create ClassSession object
                ClassSession session = new ClassSession(
                        sessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                );

                // Load details for this session
                DetailsModel details = loadSessionDetails(sessionId);
                if (details == null) {
                    details = new DetailsModel(session);
                }

                allSessions.add(details);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return allSessions;
    }

    /**
     * Deletes a session by ID
     * @param sessionId Session ID to delete
     * @return True if deletion was successful
     */
    public boolean deleteSession(long sessionId) {
        String deleteQuery = "DELETE FROM details WHERE class_session_id = ?";

        try {
            int rowsAffected = DatabaseConnection.executeUpdate(deleteQuery, sessionId);
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates a student's grade in a specific class session
     * @param classSession The class session containing the student
     * @param studentId The ID of the student to update
     * @param newGrade The new grade value
     * @param note Any notes about the grade
     * @return True if update was successful
     */
    public boolean updateStudentGrade(ClassSession classSession, String studentId, double newGrade, String note) {
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
                    classSession.getId(),
                    studentId
            );

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads all class sessions and their details from the database into DetailsModel instances.
     * @return A list of DetailsModel objects.
     */
    public List<DetailsModel> loadAllDetails() {
        Map<Long, DetailsModel> detailsMap = new HashMap<>();
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "ORDER BY d.class_session_id, d.student_id";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery);

            while (resultSet.next()) {
                // Extract session data
                long sessionId = resultSet.getLong("class_session_id");
                String courseName = resultSet.getString("course_name");
                String teacher = resultSet.getString("teacher");
                LocalDate sessionDate = resultSet.getDate("session_date").toLocalDate();
                String timeSlot = resultSet.getString("time_slot");

                // Create or get ClassSession
                DetailsModel detailsModel;
                if (detailsMap.containsKey(sessionId)) {
                    detailsModel = detailsMap.get(sessionId);
                } else {
                    // Create new ClassSession with the available data
                    ClassSession session = new ClassSession(
                            sessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                    detailsMap.put(sessionId, detailsModel);
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
                double grade = resultSet.getDouble("grade");
                String gradeLevel = resultSet.getString("grade_level");
                boolean passed = "Passed".equals(resultSet.getString("pass_status"));
                String note = resultSet.getString("note");

                // Add student to model - using the count of current students as STT (order number)
                int stt = detailsModel.getStudentData().size() + 1;
                detailsModel.getStudentData().add(
                        new StudentGradeModel(stt, studentName, studentId, grade, gradeLevel, passed, note)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(detailsMap.values());
    }

    /**
     * Loads DetailsModel for a specific class session.
     * @param sessionId The ID of the class session to load details for.
     * @return DetailsModel with student grades for the specified session.
     */
    public DetailsModel loadSessionDetails(long sessionId) {
        DetailsModel detailsModel = null;
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.class_session_id = ? ORDER BY d.student_id";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, sessionId);
            int stt = 1;

            while (resultSet.next()) {
                if (detailsModel == null) {
                    // Extract session data
                    String courseName = resultSet.getString("course_name");
                    String teacher = resultSet.getString("teacher");
                    LocalDate sessionDate = resultSet.getDate("session_date").toLocalDate();
                    String timeSlot = resultSet.getString("time_slot");

                    // Create ClassSession
                    ClassSession session = new ClassSession(
                            sessionId, courseName, teacher, "Unknown", sessionDate, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
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
            e.printStackTrace();
        }

        return detailsModel;
    }

    /**
     * Loads DetailsModel for a specific course on a specific date.
     * @param courseName The name of the course.
     * @param date The date of the class session.
     * @return DetailsModel with student grades for the specified course and date.
     */
    public DetailsModel loadDetailsByNameAndDate(String courseName, LocalDate date) {
        DetailsModel detailsModel = null;
        String selectQuery = "SELECT d.*, s.id as student_db_id, s.full_name FROM details d " +
                "LEFT JOIN students s ON d.student_id = s.id " +
                "WHERE d.course_name = ? AND d.session_date = ? " +
                "ORDER BY d.student_id";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(
                    selectQuery, courseName, Date.valueOf(date)
            );
            int stt = 1;

            while (resultSet.next()) {
                if (detailsModel == null) {
                    // Extract session data
                    long sessionId = resultSet.getLong("class_session_id");
                    String teacher = resultSet.getString("teacher");
                    String timeSlot = resultSet.getString("time_slot");

                    // Create ClassSession
                    ClassSession session = new ClassSession(
                            sessionId, courseName, teacher, "Unknown", date, timeSlot
                    );

                    detailsModel = new DetailsModel(session);
                }

                // Extract student data
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
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
            e.printStackTrace();
        }

        return detailsModel;
    }

    /**
     * Deletes details for a specific class session.
     * @param classSession The ClassSession to delete details for.
     */
    public void deleteDetails(ClassSession classSession) {
        String deleteQuery = "DELETE FROM details WHERE class_session_id = ?";

        try {
            DatabaseConnection.executeUpdate(deleteQuery, classSession.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates grade and note data for a specific student in a class session.
     * @param classSession The ClassSession containing the student.
     * @param studentModel The StudentGradeModel containing updated data.
     */
    public void updateStudentDetails(ClassSession classSession, StudentGradeModel studentModel) {
        String updateQuery = "UPDATE details SET grade = ?, grade_level = ?, pass_status = ?, note = ? " +
                "WHERE class_session_id = ? AND student_id = ?";

        try {
            double grade = Double.parseDouble(studentModel.getGrade());
            String passStatus = studentModel.getPass().equals("✓") ? "Passed" : "Failed";

            DatabaseConnection.executeUpdate(
                    updateQuery,
                    grade,
                    studentModel.getGradeLevel(),
                    passStatus,
                    studentModel.getNote(),
                    classSession.getId(),
                    studentModel.getStudentId()
            );
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes details for all students in a class session with default grade values.
     * @param classSession The ClassSession to initialize.
     * @param students The list of students to add to the session.
     */
    public boolean initializeSessionDetails(ClassSession classSession, List<Student> students) {
        String insertQuery = "INSERT INTO details (class_session_id, course_name, teacher, session_date, time_slot, " +
                "student_id, student_name, grade, grade_level, pass_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int stt = 1;
        for (Student student : students) {
            try {
                // Default grade is 0.0
                double defaultGrade = 0.0;
                // Determine grade level based on the numeric grade
                String gradeLevel = DetailsModel.determineGradeLevel(defaultGrade);

                DatabaseConnection.executeUpdate(
                        insertQuery,
                        classSession.getId(),
                        classSession.getCourseName(),
                        classSession.getTeacher(),
                        Date.valueOf(classSession.getDate()),
                        classSession.getTimeSlot(),
                        student.getId(),
                        student.getName(),
                        defaultGrade,
                        gradeLevel,
                        "Failed", // Default pass status
                        "" // Default note
                );
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            stt++;
            return true;
        }
        return false;
    }

    /**
     * Gets a list of students with their average grades for a specific course.
     * @param courseName The name of the course.
     * @return A list of student models with average grades.
     */
    public List<StudentGradeModel> getStudentAverageGrades(String courseName) {
        List<StudentGradeModel> studentAverages = new ArrayList<>();
        String selectQuery = "SELECT d.student_id, d.student_name, AVG(d.grade) as avg_grade, " +
                "COUNT(d.grade) as grade_count " +
                "FROM details d " +
                "WHERE d.course_name = ? " +
                "GROUP BY d.student_id, d.student_name";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, courseName);
            int stt = 1;

            while (resultSet.next()) {
                String studentId = resultSet.getString("student_id");
                String studentName = resultSet.getString("student_name");
                double avgGrade = resultSet.getDouble("avg_grade");
                int gradeCount = resultSet.getInt("grade_count");

                if (gradeCount > 0) {
                    // Determine grade level based on average grade
                    String gradeLevel = DetailsModel.determineGradeLevel(avgGrade);
                    // Consider as passed if average grade is 5.0 or higher
                    boolean passed = avgGrade >= 5.0;
                    String note = "Average grade from " + gradeCount + " sessions";

                    StudentGradeModel studentModel = new StudentGradeModel(
                            stt, studentName, studentId, avgGrade, gradeLevel, passed, note
                    );

                    studentAverages.add(studentModel);
                    stt++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return studentAverages;
    }

    /**
     * Gets a list of students with their final grades for a specific course,
     * taking into account the weight of each session.
     * @param courseName The name of the course.
     * @param weights Map of session weights (sessionId -> weight)
     * @return A list of student models with weighted final grades.
     */
    public List<StudentGradeModel> getStudentFinalGrades(String courseName, Map<Long, Double> weights) {
        // First get all grades for the course
        Map<String, Map<Long, Double>> studentGrades = new HashMap<>();
        Map<String, String> studentNames = new HashMap<>();
        String selectQuery = "SELECT d.class_session_id, d.student_id, d.student_name, d.grade " +
                "FROM details d " +
                "WHERE d.course_name = ? ";

        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, courseName);

            while (resultSet.next()) {
                String studentId = resultSet.getString("student_id");
                long sessionId = resultSet.getLong("class_session_id");
                double grade = resultSet.getDouble("grade");
                String studentName = resultSet.getString("student_name");

                // Store student name
                studentNames.put(studentId, studentName);

                // Initialize student entry if not exists
                if (!studentGrades.containsKey(studentId)) {
                    studentGrades.put(studentId, new HashMap<>());
                }

                // Store grade
                studentGrades.get(studentId).put(sessionId, grade);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Calculate weighted grades
        List<StudentGradeModel> finalGrades = new ArrayList<>();
        int stt = 1;

        for (Map.Entry<String, Map<Long, Double>> entry : studentGrades.entrySet()) {
            String studentId = entry.getKey();
            Map<Long, Double> grades = entry.getValue();
            String studentName = studentNames.getOrDefault(studentId, "Unknown Student");

            double totalWeight = 0.0;
            double weightedSum = 0.0;

            for (Map.Entry<Long, Double> gradeEntry : grades.entrySet()) {
                long sessionId = gradeEntry.getKey();
                double grade = gradeEntry.getValue();
                double weight = weights.getOrDefault(sessionId, 1.0);

                weightedSum += grade * weight;
                totalWeight += weight;
            }

            if (totalWeight > 0) {
                double finalGrade = weightedSum / totalWeight;
                String gradeLevel = DetailsModel.determineGradeLevel(finalGrade);
                boolean passed = finalGrade >= 5.0;
                String note = "Weighted final grade";

                StudentGradeModel studentModel = new StudentGradeModel(
                        stt, studentName, studentId, finalGrade, gradeLevel, passed, note
                );

                finalGrades.add(studentModel);
                stt++;
            }
        }

        return finalGrades;
    }

    /**
     * Helper method to find a student's name by ID
     */
    private String resultSetFindStudentName(String studentId) {
        String selectQuery = "SELECT full_name FROM students WHERE id = ?";
        try {
            ResultSet resultSet = DatabaseConnection.executeQuery(selectQuery, studentId);
            if (resultSet.next()) {
                return resultSet.getString("full_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Student";
    }
}
