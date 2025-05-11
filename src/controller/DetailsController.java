package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import src.model.ClassSession;
import src.model.details.DetailsModel;
import src.model.details.DetailsModel.StudentGradeModel;
import src.model.person.Student;
import utils.DaoManager;
import src.dao.DetailsDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for managing interaction between the DetailsView and the DetailsModel.
 * Handles business logic and data flow between view and model.
 * Corrected DAO method calls for updateStudentGrade and addStudentToSession.
 */
public class DetailsController {

    private static final Logger LOGGER = Logger.getLogger(DetailsController.class.getName());

    private final DetailsDAO detailsDAO;
    private DetailsModel currentModel;

    public DetailsController() {
        this.detailsDAO = DaoManager.getInstance().getDetailsDAO();
    }

    /**
     * Get the current model
     * @return DetailsModel instance
     */
    public DetailsModel getCurrentModel() {
        return currentModel;
    }

    /**
     * Loads session details for a specific class session
     * @param sessionId The ID of the class session to load
     * @return True if session was loaded successfully, false otherwise
     */
    public boolean loadSessionDetails(String sessionId) {
        DetailsModel loadedModel = detailsDAO.loadSessionDetails(sessionId);
        if (loadedModel != null) {
            this.currentModel = loadedModel;
            return true;
        }
        this.currentModel = null;
        return false;
    }

    /**
     * Loads session details for a specific course on a specific date
     * @param courseName Course name
     * @param date Session date
     * @return True if session was loaded successfully, false otherwise
     */
    public boolean loadSessionDetails(String courseName, LocalDate date) {
        DetailsModel loadedModel = detailsDAO.loadDetailsByNameAndDate(courseName, date);
        if (loadedModel != null) {
            this.currentModel = loadedModel;
            return true;
        }
        this.currentModel = null;
        return false;
    }

    /**
     * Initializes a new class session with students
     * @param classSession The class session to initialize
     * @param students List of students to add to the session
     * @return True if initialization was successful
     */
    public boolean initializeSession(ClassSession classSession, List<Student> students) {
        boolean success = detailsDAO.initializeSessionDetails(classSession, students);
        if (success) {
            return loadSessionDetails(classSession.getId());
        }
        return false;
    }

    /**
     * Saves the current details model
     */
    public void saveSessionDetails() {
        if (currentModel == null || currentModel.getClassSession() == null) {
            LOGGER.log(Level.WARNING, "Attempted to save session details with null currentModel or ClassSession.");
            return;
        }
        detailsDAO.saveDetails(currentModel);
    }

    /**
     * Updates a student's grade in the current session
     * @param studentId Student ID
     * @param newGrade New grade value
     * @param note Optional note (can be null)
     * @return True if update was successful
     */
    public boolean updateStudentGrade(String studentId, double newGrade, String note) {
        if (currentModel == null || currentModel.getClassSession() == null) {
            LOGGER.log(Level.WARNING, "Attempted to update student grade with null currentModel or ClassSession.");
            return false;
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update student grade with null or empty student ID.");
            return false;
        }

        StudentGradeModel studentToUpdate = null;
        ObservableList<StudentGradeModel> studentDataList = currentModel.getStudentData();

        if (studentDataList != null) {
            for (StudentGradeModel student : studentDataList) {
                if (student != null && student.getStudentId() != null && student.getStudentId().equals(studentId)) {
                    studentToUpdate = student;
                    break;
                }
            }
        }


        if (studentToUpdate != null) {
            // Update properties directly on the StudentGradeModel instance in the ObservableList
            // This is for immediate UI update if bound to the ObservableList
            studentToUpdate.setGrade(newGrade);
            studentToUpdate.setGradeLevel(DetailsModel.determineGradeLevel(newGrade));
            studentToUpdate.setPassed(newGrade >= 5.0);
            studentToUpdate.setNote(note != null ? note.trim() : "");

            // Save changes to the database via DAO
            // Calling the boolean method updateStudentGrade from the provided DAO
            boolean daoSuccess = detailsDAO.updateStudentGrade(currentModel.getClassSession(), studentId, newGrade, studentToUpdate.getNote());

            if (!daoSuccess) {
                LOGGER.log(Level.SEVERE, "DAO failed to update student grade for student ID " + studentId + " in session " + currentModel.getClassSession().getId());
                // Depending on requirements, you might revert the local model change here
            }
            return daoSuccess;
        }

        LOGGER.log(Level.WARNING, "Student with ID " + studentId + " not found in current model for update.");
        return false;
    }

    /**
     * Adds a new student to the current session
     * @param student Student to add
     * @param initialGrade Initial grade
     * @param note Optional note (can be null)
     * @return True if student was added to local model and DAO call was made.
     */
    public boolean addStudentToSession(Student student, double initialGrade, String note) {
        if (currentModel == null || currentModel.getClassSession() == null || student == null || student.getId() == null || student.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to add student to session with null or invalid data.");
            return false;
        }

        ObservableList<StudentGradeModel> studentDataList = currentModel.getStudentData();

        boolean studentExists = false;
        if (studentDataList != null) {
            studentExists = studentDataList.stream()
                    .anyMatch(existingStudent -> existingStudent != null && existingStudent.getStudentId() != null && existingStudent.getStudentId().equals(student.getId()));
        }


        if (studentExists) {
            LOGGER.log(Level.INFO, "Student with ID " + student.getId() + " already exists in session " + currentModel.getClassSession().getId());
            return false;
        }

        try {
            String gradeLevel = DetailsModel.determineGradeLevel(initialGrade);
            boolean passed = initialGrade >= 5.0;

            int stt = (studentDataList != null ? studentDataList.size() : 0) + 1;

            StudentGradeModel newStudentModel = new StudentGradeModel(
                    stt,
                    student.getName() != null ? student.getName() : "Unknown Student",
                    student.getId(),
                    initialGrade,
                    gradeLevel,
                    passed,
                    note != null ? note.trim() : ""
            );

            // Add the new student directly to the model's ObservableList
            // This triggers UI updates if the list is bound.
            // The list is initialized in the constructor, so getStudentData() should not return null here
            // if currentModel is not null (checked at the start).
            if (currentModel.getStudentData() != null) { // Redundant check if constructor always initializes
                currentModel.getStudentData().add(newStudentModel);
            } else {
                // Should not happen if constructor works as expected, but as a fallback
                currentModel.setStudentData(FXCollections.observableArrayList(newStudentModel));
            }


            // Update database via DAO. Call the void method updateStudentDetails
            // as shown in the screenshot.
            // Since it's void, we cannot check for DAO success here directly.
            detailsDAO.updateStudentDetails(currentModel.getClassSession(), newStudentModel);

            // Return true indicating the student was added to the local model
            // and the database update attempt was made.
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error adding student to session ID " + currentModel.getClassSession().getId() + " for student ID " + student.getId(), e);
            // Handle potential cleanup if needed after an error
            return false;
        }
    }

    /**
     * Exports the current session details to Excel
     * @param classSession The class session to export
     * @return True if export was successful
     */
    public boolean exportSessionDetailsToExcel(ClassSession classSession) {
        if (classSession == null || classSession.getId() == null || classSession.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to export session details with null or invalid ClassSession.");
            return false;
        }
        // Assuming exportToExcel exists in the DAO and takes session ID and returns boolean.
        // Note: This method does not exist in the previously provided DetailsDAO code.
        // If your DAO has this method, this call is correct. Otherwise, it will cause an error.
        LOGGER.log(Level.WARNING, "Calling exportToExcel method in DAO, which was not present in the provided DetailsDAO code. This may cause an error.");
        // Including the call as it was in your structure, assuming it exists in your actual DAO
        return detailsDAO.exportToExcel(classSession.getId()); // Potential error here if DAO method is missing or has different signature/return type
    }

    /**
     * Gets the data series for the grade distribution chart
     * @return XYChart.Series for the bar chart
     */
    public XYChart.Series<String, Number> getGradeDistributionData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Học viên");

        if (currentModel != null && currentModel.getStudentData() != null) {
            TreeMap<String, Integer> gradeFrequency = currentModel.calculateGradeDistribution();

            for (Map.Entry<String, Integer> entry : gradeFrequency.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }

        return series;
    }

    /**
     * Gets the data for the pie chart
     * @return ObservableList of PieChart.Data
     */
    public ObservableList<PieChart.Data> getPieChartData() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        if (currentModel != null && currentModel.getStudentData() != null) {
            Map<String, Integer> categoryCounts = currentModel.calculateCategoryDistribution();

            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                if (entry.getValue() > 0) {
                    pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }
        }

        return pieChartData;
    }

    /**
     * Calculates grade statistics for the current session
     * @return Map containing average, highest, lowest, and pass rate
     */
    public Map<String, Double> calculateGradeStatistics() {
        Map<String, Double> stats = new HashMap<>();

        if (currentModel == null || currentModel.getStudentData() == null || currentModel.getStudentData().isEmpty()) {
            stats.put("average", 0.0);
            stats.put("highest", 0.0);
            stats.put("lowest", 0.0);
            stats.put("passRate", 0.0);
            return stats;
        }

        double sum = 0.0;
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;
        int passCount = 0;
        int validGradeCount = 0;

        for (DetailsModel.StudentGradeModel student : currentModel.getStudentData()) {
            if (student != null) {
                try {
                    double grade = Double.parseDouble(student.getGrade());
                    sum += grade;
                    highest = Math.max(highest, grade);
                    lowest = Math.min(lowest, grade);
                    if (student.isPassed()) {
                        passCount++;
                    }
                    validGradeCount++;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing grade data for student ID: " + student.getStudentId() + " in calculateGradeStatistics.", e);
                }
            }
        }


        int totalStudents = currentModel.getStudentData().size();
        double average = validGradeCount > 0 ? sum / validGradeCount : 0;
        double passRate = totalStudents > 0 ? (double) passCount / totalStudents * 100 : 0;

        stats.put("average", average);
        stats.put("highest", validGradeCount > 0 ? highest : 0.0);
        stats.put("lowest", validGradeCount > 0 ? lowest : 0.0);
        stats.put("passRate", passRate);

        return stats;
    }

    /**
     * Gets a list of all sessions (for reporting purposes)
     * @return List of session details
     */
    public List<DetailsModel> getAllSessions() {
        return detailsDAO.getAllSessions();
    }

    /**
     * Deletes a session by ID
     * @param sessionId Session ID to delete
     * @return True if deletion was successful
     */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete session with null or empty ID.");
            return false;
        }
        return detailsDAO.deleteSession(sessionId);
    }

    /**
     * Gets a list of students with their average grades for a specific course.
     * @param courseName The name of the course (String).
     * @return A list of student models with average grades.
     */
    public List<StudentGradeModel> getStudentAverageGrades(String courseName) {
        return detailsDAO.getStudentAverageGrades(courseName);
    }

    /**
     * Gets a list of students with their final grades for a specific course,
     * taking into account the weight of each session.
     * @param courseName The name of the course (String).
     * @param weights Map of session weights (String sessionId -> Double weight)
     * @return A list of student models with weighted final grades.
     */
    public List<StudentGradeModel> getStudentFinalGrades(String courseName, Map<String, Double> weights) {
        return detailsDAO.getStudentFinalGrades(courseName, weights);
    }

}
