package src.model.details;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import src.model.ClassSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

public class DetailsModel {
    // Grade classification constants
    private static final double GRADE_MAT_GOC_MAX = 1.0;
    private static final double GRADE_YEU_MIN = 2.0;
    private static final double GRADE_YEU_MAX = 3.9;
    private static final double GRADE_TB_MIN = 4.0;
    private static final double GRADE_TB_MAX = 5.9;
    private static final double GRADE_TB_KHA_MIN = 6.0;
    private static final double GRADE_TB_KHA_MAX = 6.9;
    private static final double GRADE_KHA_MIN = 7.0;
    private static final double GRADE_KHA_MAX = 7.9;
    private static final double GRADE_TOT_MIN = 8.0;
    private static final double GRADE_TOT_MAX = 8.9;
    private static final double GRADE_GIOI_MIN = 9.0;

    // Category colors
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("Mất gốc", "#3b82f6");    // Xanh dương
        CATEGORY_COLORS.put("Yếu", "#ec4899");        // Hồng
        CATEGORY_COLORS.put("Trung bình", "#f97316"); // Cam
        CATEGORY_COLORS.put("Trung bình - Khá", "#fcd34d"); // Vàng
        CATEGORY_COLORS.put("Khá", "#14b8a6");        // Xanh lá mạ
        CATEGORY_COLORS.put("Tốt", "#a78bfa");        // Tím
        CATEGORY_COLORS.put("Giỏi", "#9ca3af");       // Xám
    }

    private ClassSession classSession;
    private ObservableList<StudentGradeModel> studentData;

    public DetailsModel() {
        this.studentData = FXCollections.observableArrayList();
    }

    public DetailsModel(ClassSession classSession) {
        this.classSession = classSession;
        this.studentData = FXCollections.observableArrayList();
    }

    public ClassSession getClassSession() {
        return classSession;
    }

    public void setClassSession(ClassSession classSession) {
        this.classSession = classSession;
    }

    public ObservableList<StudentGradeModel> getStudentData() {
        return studentData;
    }

    public Map<String, String> getCategoryColors() {
        return CATEGORY_COLORS;
    }

    /**
     * Calculate grade distribution data for the bar chart
     */
    public TreeMap<String, Integer> calculateGradeDistribution() {
        TreeMap<String, Integer> gradeFrequency = new TreeMap<>();

        // Initialize all grade levels with 0 count
        for (double i = 0; i <= 10; i += 0.5) {
            String grade = (i == Math.floor(i)) ? String.format("%.0f", i) : String.format("%.1f", i);
            gradeFrequency.put(grade, 0);
        }

        // Count frequency of each grade from the data
        for (StudentGradeModel student : studentData) {
            String gradeStr = student.getGrade();
            gradeFrequency.put(gradeStr, gradeFrequency.getOrDefault(gradeStr, 0) + 1);
        }

        return gradeFrequency;
    }

    /**
     * Calculate category distribution data for the pie chart
     */
    public Map<String, Integer> calculateCategoryDistribution() {
        Map<String, Integer> categoryCounts = new HashMap<>();

        // Initialize categories with 0 counts
        for (String category : CATEGORY_COLORS.keySet()) {
            categoryCounts.put(category, 0);
        }

        // Count students in each category
        for (StudentGradeModel student : studentData) {
            String gradeLevel = student.getGradeLevel();
            categoryCounts.put(gradeLevel, categoryCounts.getOrDefault(gradeLevel, 0) + 1);
        }

        return categoryCounts;
    }

    /**
     * Create XYChart series for grade distribution chart
     * @return XYChart.Series for the bar chart
     */
    public XYChart.Series<String, Number> getGradeDistributionData() {
        // Create data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Học viên");

        // Get grade distribution from the model
        TreeMap<String, Integer> gradeFrequency = calculateGradeDistribution();

        // Add data to series
        for (Map.Entry<String, Integer> entry : gradeFrequency.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        return series;
    }

    /**
     * Get data for the pie chart
     * @return ObservableList of PieChart.Data
     */
    public ObservableList<PieChart.Data> getPieChartData() {
        // Get category distribution data
        Map<String, Integer> categoryCounts = calculateCategoryDistribution();

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
     * Calculate grade statistics for the current session
     * @return Map containing average, highest, lowest, and pass rate
     */
    public Map<String, Double> calculateGradeStatistics() {
        Map<String, Double> stats = new HashMap<>();

        if (studentData == null || studentData.isEmpty()) {
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

        for (StudentGradeModel student : studentData) {
            double grade = parseGrade(student.getGrade());
            sum += grade;
            highest = Math.max(highest, grade);
            lowest = Math.min(lowest, grade);
            if (student.isPassed()) {
                passCount++;
            }
        }

        int totalStudents = studentData.size();
        double average = totalStudents > 0 ? sum / totalStudents : 0;
        double passRate = totalStudents > 0 ? (double) passCount / totalStudents * 100 : 0;

        stats.put("average", average);
        stats.put("highest", highest);
        stats.put("lowest", lowest);
        stats.put("passRate", passRate);

        return stats;
    }

    /**
     * Parse string grade to double
     * @param gradeStr String grade
     * @return double grade value
     */
    private double parseGrade(String gradeStr) {
        try {
            return Double.parseDouble(gradeStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Update a student's grade in the current model
     * @param studentId Student ID
     * @param newGrade New grade value
     * @param note Optional note (can be null)
     * @return True if update was successful
     */
    public boolean updateStudentGrade(String studentId, double newGrade, String note) {
        for (StudentGradeModel student : studentData) {
            if (student.getStudentId().equals(studentId)) {
                // Create a new student model with updated values
                int stt = student.getStt();
                String fullName = student.getFullName();
                String gradeLevel = determineGradeLevel(newGrade);
                boolean passed = newGrade >= 5.0;
                String actualNote = note != null ? note : student.getNote();

                // Remove old student and add updated one
                int index = studentData.indexOf(student);
                if (index != -1) {
                    studentData.remove(index);
                    studentData.add(index, new StudentGradeModel(
                            stt,
                            fullName,
                            studentId,
                            newGrade,
                            gradeLevel,
                            passed,
                            actualNote
                    ));
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Add a new student to the model
     * @param fullName Student's full name
     * @param studentId Student ID
     * @param initialGrade Initial grade
     * @param note Optional note (can be null)
     * @return True if student was added successfully
     */
    public boolean addStudent(String fullName, String studentId, double initialGrade, String note) {
        // Check if student already exists
        for (StudentGradeModel existingStudent : studentData) {
            if (existingStudent.getStudentId().equals(studentId)) {
                return false; // Student already exists
            }
        }

        try {
            // Determine grade level based on initial grade
            String gradeLevel = determineGradeLevel(initialGrade);
            boolean passed = initialGrade >= 5.0;

            // Create new student model
            int stt = studentData.size() + 1;
            StudentGradeModel newStudent = new StudentGradeModel(
                    stt,
                    fullName,
                    studentId,
                    initialGrade,
                    gradeLevel,
                    passed,
                    note != null ? note : ""
            );

            // Add to current model
            studentData.add(newStudent);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all students in the session
     * @return List of student models
     */
    public List<StudentGradeModel> getAllStudents() {
        List<StudentGradeModel> students = new ArrayList<>();
        students.addAll(studentData);
        return students;
    }

    /**
     * Get a student by ID
     * @param studentId Student ID to find
     * @return StudentGradeModel or null if not found
     */
    public StudentGradeModel getStudentById(String studentId) {
        for (StudentGradeModel student : studentData) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
    }

    /**
     * Get pass rate for the session
     * @return Pass rate percentage
     */
    public double getPassRate() {
        if (studentData.isEmpty()) {
            return 0.0;
        }

        int passed = 0;
        for (StudentGradeModel student : studentData) {
            if (student.isPassed()) {
                passed++;
            }
        }

        return (double) passed / studentData.size() * 100.0;
    }

    /**
     * Get average grade for the session
     * @return Average grade
     */
    public double getAverageGrade() {
        if (studentData.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (StudentGradeModel student : studentData) {
            sum += parseGrade(student.getGrade());
        }

        return sum / studentData.size();
    }

    /**
     * Setter for studentData. This method was requested.
     * @param studentData The new list of student data to set.
     */
    public void setStudentData(ObservableList<StudentGradeModel> studentData) {
        this.studentData = studentData;
    }

    /**
     * Student Grade Model class for data representation
     */
    public static class StudentGradeModel {
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty studentId;
        private final SimpleStringProperty grade;
        private final SimpleStringProperty gradeLevel;
        private final SimpleStringProperty pass;
        private final SimpleStringProperty note;
        private final int stt;

        public StudentGradeModel(int stt, String fullName, String studentId,
                                 double grade, String gradeLevel, boolean pass, String note) {
            this.stt = stt;
            this.fullName = new SimpleStringProperty(fullName);
            this.studentId = new SimpleStringProperty(studentId);

            // Format grade
            String gradeStr;
            if (grade == Math.floor(grade)) {
                gradeStr = String.format("%.0f", grade);
            } else {
                gradeStr = String.format("%.1f", grade);
            }

            this.grade = new SimpleStringProperty(gradeStr);
            this.gradeLevel = new SimpleStringProperty(gradeLevel);
            this.pass = new SimpleStringProperty(pass ? "✓" : "✗");
            this.note = new SimpleStringProperty(note);
        }

        // Getters
        public int getStt() { return stt; }
        public String getFullName() { return fullName.get(); }
        public String getStudentId() { return studentId.get(); }
        public String getGrade() { return grade.get(); }
        public String getGradeLevel() { return gradeLevel.get(); }
        public String getPass() { return pass.get(); }
        public String getNote() { return note.get(); }

        // Setters for mutable properties (updating note, can't change stt)
        public void setNote(String note) { this.note.set(note); }

        // Method to update grade properties
        public void setGrade(double newGrade) {
            // Format grade
            String gradeStr;
            if (newGrade == Math.floor(newGrade)) {
                gradeStr = String.format("%.0f", newGrade);
            } else {
                gradeStr = String.format("%.1f", newGrade);
            }
            this.grade.set(gradeStr);
        }

        public void setGradeLevel(String gradeLevel) {
            this.gradeLevel.set(gradeLevel);
        }

        public void setPassed(boolean isPassed) {
            this.pass.set(isPassed ? "✓" : "✗");
        }

        // Property getters for JavaFX binding
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty studentIdProperty() { return studentId; }
        public SimpleStringProperty gradeProperty() { return grade; }
        public SimpleStringProperty gradeLevelProperty() { return gradeLevel; }
        public SimpleStringProperty passProperty() { return pass; }
        public SimpleStringProperty noteProperty() { return note; }

        public boolean isPassed() {
            return "✓".equals(getPass());
        }
    }

    /**
     * Determine grade level based on numerical grade
     */
    public static String determineGradeLevel(double grade) {
        if (grade <= GRADE_MAT_GOC_MAX) {
            return "Mất gốc";
        } else if (grade >= GRADE_YEU_MIN && grade <= GRADE_YEU_MAX) {
            return "Yếu";
        } else if (grade >= GRADE_TB_MIN && grade <= GRADE_TB_MAX) {
            return "Trung bình";
        } else if (grade >= GRADE_TB_KHA_MIN && grade <= GRADE_TB_KHA_MAX) {
            return "Trung bình - Khá";
        } else if (grade >= GRADE_KHA_MIN && grade <= GRADE_KHA_MAX) {
            return "Khá";
        } else if (grade >= GRADE_TOT_MIN && grade <= GRADE_TOT_MAX) {
            return "Tốt";
        } else if (grade >= GRADE_GIOI_MIN) {
            return "Giỏi";
        } else {
            return "Unknown";
        }
    }
}
