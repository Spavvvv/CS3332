package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import src.dao.DetailsDAO;
import src.model.ClassSession;
import src.model.details.DetailsModel;
import src.model.person.Student;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Controller class for managing interaction between the DetailsView and the DetailsModel.
 * Handles business logic and data flow between view and model.
 */
public class DetailsController {
    private DetailsDAO detailsDAO;
    private DetailsModel currentModel;

    public DetailsController() {
        this.detailsDAO = new DetailsDAO();
        this.currentModel = new DetailsModel();
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
            // Load the newly initialized session
            return loadSessionDetails(classSession.getId());
        }
        return false;
    }

    /**
     * Saves the current details model
     * @return True if save was successful, false otherwise
     */
//    public boolean saveSessionDetails() {
//        if (currentModel == null || currentModel.getClassSession() == null) {
//            return false;
//        }
//        return detailsDAO.saveSessionDetails(currentModel);
//    }

    /**
     * Updates a student's grade in the current session
     * @param studentId Student ID
     * @param newGrade New grade value
     * @param note Optional note (can be null)
     * @return True if update was successful
     */
    public boolean updateStudentGrade(String studentId, double newGrade, String note) {
        if (currentModel == null || currentModel.getClassSession() == null) {
            return false;
        }

        for (DetailsModel.StudentGradeModel student : currentModel.getStudentData()) {
            if (student.getStudentId().equals(studentId)) {
                // Update grade
                String gradeLevel = DetailsModel.determineGradeLevel(newGrade);
                boolean passed = newGrade >= 5.0;

                student.setGrade(newGrade);
                student.setGradeLevel(gradeLevel);
                student.setPassed(passed);

                if (note != null && !note.isEmpty()) {
                    student.setNote(note);
                }

                // Save to database
                detailsDAO.updateStudentGrade(currentModel.getClassSession(), studentId, newGrade, note);
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a new student to the current session
     * @param student Student to add
     * @param initialGrade Initial grade
     * @param note Optional note (can be null)
     * @return True if student was added successfully
     */
    public boolean addStudentToSession(Student student, double initialGrade, String note) {
        if (currentModel == null || currentModel.getClassSession() == null) {
            return false;
        }

        // Check if student already exists in session
        for (DetailsModel.StudentGradeModel existingStudent : currentModel.getStudentData()) {
            if (existingStudent.getStudentId().equals(student.getId())) {
                return false; // Student already exists
            }
        }

        try {
            // Determine grade level based on initial grade
            String gradeLevel = DetailsModel.determineGradeLevel(initialGrade);
            boolean passed = initialGrade >= 5.0;

            // Create new student model
            int stt = currentModel.getStudentData().size() + 1;
            DetailsModel.StudentGradeModel newStudent = new DetailsModel.StudentGradeModel(
                    stt,
                    student.getName (),
                    student.getId(),
                    initialGrade,
                    gradeLevel,
                    passed,
                    note != null ? note : ""
            );

            // Add to current model
            currentModel.getStudentData().add(newStudent);

            // Update database
            detailsDAO.updateStudentDetails(currentModel.getClassSession(), newStudent);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Exports the current session details to Excel
     * @param classSession The class session to export
     * @return True if export was successful
     */
    public boolean exportSessionDetailsToExcel(ClassSession classSession) {
        if (classSession == null) {
            return false;
        }

        try {
            // This would be implemented based on your Excel export capabilities
            // For now, it's a placeholder
            return false;
            //detailsDAO.exportToExcel(classSession.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the data series for the grade distribution chart
     * @return XYChart.Series for the bar chart
     */
    public XYChart.Series<String, Number> getGradeDistributionData() {
        // Create data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Học viên");

        // Get grade distribution from the model
        TreeMap<String, Integer> gradeFrequency = currentModel.calculateGradeDistribution();

        // Add data to series
        for (Map.Entry<String, Integer> entry : gradeFrequency.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        return series;
    }

    /**
     * Gets the data for the pie chart
     * @return ObservableList of PieChart.Data
     */
    public ObservableList<PieChart.Data> getPieChartData() {
        // Get category distribution data from the model
        Map<String, Integer> categoryCounts = currentModel.calculateCategoryDistribution();

        // Create data for chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > 0) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
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

        if (currentModel == null || currentModel.getStudentData().isEmpty()) {
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

        for (DetailsModel.StudentGradeModel student : currentModel.getStudentData()) {
            double grade = Double.parseDouble(student.getGrade());
            sum += grade;
            highest = Math.max(highest, grade);
            lowest = Math.min(lowest, grade);
            if (student.isPassed()) {
                passCount++;
            }
        }

        int totalStudents = currentModel.getStudentData().size();
        double average = totalStudents > 0 ? sum / totalStudents : 0;
        double passRate = totalStudents > 0 ? (double) passCount / totalStudents * 100 : 0;

        stats.put("average", average);
        stats.put("highest", highest);
        stats.put("lowest", lowest);
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
        return detailsDAO.deleteSession(sessionId);
    }
}
