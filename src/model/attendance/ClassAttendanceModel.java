package src.model.attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import src.utils.DaoManager;
import src.dao.Attendance.HomeworkDAO;

public class ClassAttendanceModel {
    private ObservableList<StudentAttendanceData> attendanceList = FXCollections.observableArrayList();
    private LocalDate sessionDate;
    private int sessionNumber;
    private String sessionNotes;
    private String sessionId; // Added for homework submission
    private String homeworkId; // Added for homework submission
    private String className; // Added for reference
    private String courseId; // Added for reference
    private DaoManager daoManager;
    private HomeworkDAO homeworkDAO;


    public ClassAttendanceModel() {
        this.sessionDate = LocalDate.now();
        this.sessionNumber = 1;
        this.sessionNotes = "";
        this.sessionId = null;
        this.homeworkId = null;
        this.className = "";
        this.courseId = "";
        daoManager = DaoManager.getInstance();
        homeworkDAO = daoManager.getHomeworkDAO();
    }

    /**
     * Constructor with session and homework IDs
     * @param sessionId The ID of the class session
     * @param homeworkId The ID of the homework assignment
     */
    public ClassAttendanceModel(String sessionId, String homeworkId) {
        this();
        this.sessionId = sessionId;
        this.homeworkId = homeworkId;
    }

    /**
     * Full constructor with all properties
     */
    public ClassAttendanceModel(String sessionId, String homeworkId, String className, String classId) {
        this(sessionId, homeworkId);
        this.className = className;
        this.courseId = classId;
    }

    public ObservableList<StudentAttendanceData> getAttendanceList() {
        return attendanceList;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getSessionNotes() {
        return sessionNotes;
    }

    public void setSessionNotes(String sessionNotes) {
        this.sessionNotes = sessionNotes;
    }

    /**
     * Gets the session ID for this attendance model
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID for this attendance model
     * @param sessionId The session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the homework ID for this attendance model
     * @return The homework ID
     */
    public String getHomeworkId() {
        return homeworkId;
    }

    /**
     * Sets the homework ID for this attendance model
     * @param homeworkId The homework ID to set
     */
    public void setHomeworkId(String homeworkId) {
        this.homeworkId = homeworkId;
    }

    /**
     * Gets the class name
     * @return The class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name
     * @param className The class name to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the course ID
     * @return The course ID
     */
    public String getCourseId() {
        return courseId;
    }

    /**
     * Sets the course ID
     * @param courseId The course ID to set
     */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    /**
     * Saves attendance data to the database
     */
    public void saveAttendanceData() {
        // Database saving logic would be implemented here
        // This typically involves calling a DAO (Data Access Object)
        System.out.println("Saving attendance data to database...");
        System.out.println("Session ID: " + sessionId);
        System.out.println("Date: " + sessionDate);

        for(StudentAttendanceData data : attendanceList) {
            System.out.println("Student: " + data.getStudent().getName() +
                    ", Homework Submitted: " + data.isHomeworkSubmitted() +
                    ", Punctuality: " + data.getPunctualityRating() +
                    ", Diligence: " + data.getDiligenceRating() +
                    ", Notes: " + data.getStudentSessionNotes());
            // In a real scenario, you would convert StudentAttendanceData to a database entity
            // and call a DAO method like: studentAttendanceDAO.save(dataEntity);
        }
    }

    /**
     * Exports attendance data to Excel
     */
    public void exportToExcel() {
        // Export to Excel logic would be implemented here using a library like Apache POI
        System.out.println("Exporting attendance data to Excel...");
        System.out.println("Class: " + className);
        System.out.println("Session: " + sessionNumber + " on " + sessionDate);
        // Iterate through attendanceList and write to an Excel sheet
    }

    /**
     * Loads real attendance and homework data from the database
     * This would replace the dummy data with actual data for the specified session
     */
    public void loadAttendanceData() {
        if(sessionId == null || sessionId.isEmpty()) {
            System.out.println("Cannot load data: Session ID is null or empty. Please set a session ID.");
            // Optionally, clear the list or show an error to the user
            // attendanceList.clear();
            return;
        }

        // Clear existing data before loading new data
        attendanceList.clear();

        // Normally we would call DAOs here to load the actual student list for the class/session,
        // and then for each student, load their attendance and homework submission data.
        System.out.println("Loading attendance data for session " + sessionId + " from database...");
        System.out.println("Class: " + className + ", Course ID: " + courseId);

        // If no actual data loading logic is implemented yet, the list will remain empty.
        // You might want to inform the user or log this.
        if (attendanceList.isEmpty()) {
            System.out.println("No student attendance data loaded. List is empty. (Actual DAO logic needed)");
        }
    }

    /**
     * Adds a student to the attendance list
     * @param data The student attendance data to add
     */
    public void addStudentAttendance(StudentAttendanceData data) {
        if (data != null) {
            attendanceList.add(data);
        }
    }

    /**
     * Removes a student from the attendance list
     * @param studentId The ID of the student to remove
     * @return True if the student was found and removed, false otherwise
     */
    public boolean removeStudentAttendance(String studentId) {
        if (studentId == null || studentId.isEmpty()) return false;
        return attendanceList.removeIf(data -> data.getStudent() != null && studentId.equals(data.getStudent().getId()));
    }

    /**
     * Updates the attendance data for a student
     * @param studentId The ID of the student to update
     * @param homeworkSubmitted Whether homework was submitted
     * @param punctualityRating The new punctuality rating
     * @param homeworkGrade The homework grade
     * @param diligenceRating The new diligence rating
     * @param notes Any additional notes
     * @param finalScore The final score
     * @return True if the student was found and updated, false otherwise
     */
    public boolean updateStudentAttendance(
            String studentId,
            boolean homeworkSubmitted,
            int punctualityRating,
            double homeworkGrade,
            int diligenceRating,
            String notes,
            int finalScore) {

        if (studentId == null || studentId.isEmpty()) return false;

        for(StudentAttendanceData data : attendanceList) {
            if(data.getStudent() != null && studentId.equals(data.getStudent().getId())) {
                data.setHomeworkSubmitted(homeworkSubmitted);
                data.setPunctualityRating(punctualityRating);
                data.setHomeworkGrade(homeworkGrade);
                data.setDiligenceRating(diligenceRating);
                data.setStudentSessionNotes(notes);
                data.setFinalNumericScore(finalScore);
                // Optionally, mark for saving: data.setModified(true);
                return true;
            }
        }
        return false; // Student not found
    }

    /**
     * Finds StudentAttendanceData by student ID
     * @param studentId The ID of the student
     * @return StudentAttendanceData if found, otherwise null
     */
    public StudentAttendanceData findStudentAttendance(String studentId) {
        if (studentId == null || studentId.isEmpty()) return null;
        for (StudentAttendanceData data : attendanceList) {
            if (data.getStudent() != null && studentId.equals(data.getStudent().getId())) {
                return data;
            }
        }
        return null;
    }
}