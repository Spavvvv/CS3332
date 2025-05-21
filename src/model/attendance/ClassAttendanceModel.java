package src.model.attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.person.Parent;
import src.model.person.Student;
import java.time.LocalDate;
import utils.DaoManager;
import src.dao.HomeworkDAO;

public class ClassAttendanceModel {
    private ObservableList<StudentAttendanceData> attendanceList = FXCollections.observableArrayList();
    private LocalDate sessionDate;
    private int sessionNumber;
    private String sessionNotes;
    private String sessionId; // Added for homework submission
    private String homeworkId; // Added for homework submission
    private String className; // Added for reference
    private String classId; // Added for reference
    private DaoManager daoManager;
    private HomeworkDAO homeworkDAO;


    public ClassAttendanceModel() {
        this.sessionDate = LocalDate.now();
        this.sessionNumber = 1;
        this.sessionNotes = "";
        this.sessionId = null;
        this.homeworkId = null;
        this.className = "";
        this.classId = "";
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
        this.classId = classId;
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
    public String getClassId() {
        return classId;
    }

    /**
     * Sets the course ID
     * @param classId The course ID to set
     */
    public void setClassId(String classId) {
        this.classId = classId;
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
     * Sends notifications to parents about attendance and homework
     */
    public void sendNotifications() {
        // Notification sending logic would be implemented here
        // This could involve email APIs, SMS gateways, or other notification services
        System.out.println("Sending notifications to parents...");

        for(StudentAttendanceData data : attendanceList) {
            Student student = data.getStudent();
            if (student == null) continue; // Skip if student data is missing

            Parent parent = student.getParent();
            if(parent != null && parent.getEmail() != null && !parent.getEmail().isEmpty()) {
                StringBuilder message = new StringBuilder();
                message.append("Kính gửi phụ huynh ").append(parent.getName()).append(",\n\n");
                message.append("Con của quý phụ huynh, ").append(student.getName()).append(", ");

                boolean attended = data.getPunctualityRating() > 0; // Simple assumption for attendance

                if(!attended) {
                    message.append("đã vắng mặt trong buổi học ngày ").append(sessionDate).append(" của lớp ").append(className).append(".\n");
                } else {
                    message.append("đã tham dự buổi học ngày ").append(sessionDate).append(" của lớp ").append(className).append(".\n");

                    if(homeworkId != null && !homeworkId.isEmpty()) { // Check if homework was assigned for this session
                        if (!data.isHomeworkSubmitted()) {
                            message.append("Lưu ý: Học viên chưa nộp bài tập về nhà (ID: ").append(homeworkId).append(").\n");
                        } else {
                            message.append("Học viên đã nộp bài tập về nhà (ID: ").append(homeworkId).append(").\n");
                            if (data.getHomeworkGrade() > 0) {
                                message.append("Điểm bài tập về nhà: ").append(data.getHomeworkGrade()).append(".\n");
                            }
                        }
                    }

                    if(!data.getStudentSessionNotes().isEmpty()) {
                        message.append("Ghi chú trong buổi học: ").append(data.getStudentSessionNotes()).append(".\n");
                    }
                }

                message.append("\nTrân trọng,\nTrung tâm [Tên Trung Tâm]"); // Replace with actual center name

                // In a real app, you'd use an email service here:
                // emailService.send(parent.getEmail(), "Thông báo tình hình học tập của " + student.getName(), message.toString());
                System.out.println("Message to " + parent.getEmail() + ":\n" + message.toString());
            } else {
                System.out.println("Không thể gửi thông báo cho phụ huynh của " + student.getName() + " do thiếu thông tin email hoặc phụ huynh.");
            }
        }
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
        System.out.println("Class: " + className + ", Class ID: " + classId);

        // --- Placeholder for actual DAO calls ---
        // Example structure:
        // StudentDAO studentDAO = new StudentDAO();
        // List<Student> studentsInClass = studentDAO.getStudentsByClass(className); // or by courseId/sessionId
        //
        // StudentAttendanceDataDAO attendanceDataDAO = new StudentAttendanceDataDAO(); // Hypothetical DAO
        // HomeworkSubmissionDAO homeworkSubmissionDAO = new HomeworkSubmissionDAO();
        //
        // int stt = 1;
        // for (Student student : studentsInClass) {
        //     // Fetch or create StudentAttendanceData for this student in this session
        //     StudentAttendanceData sad = attendanceDataDAO.findByStudentAndSession(student.getId(), sessionId);
        //     if (sad == null) {
        //         // If no record, create a default one. This depends on application logic.
        //         // For now, let's assume we only load existing records or records for enrolled students.
        //         // sad = new StudentAttendanceData(stt++, student, false, 0, 0.0, 0, "", 0);
        //         System.out.println("No attendance data found for student " + student.getName() + " in session " + sessionId);
        //         // You might want to create a default entry for all students in the class for this session.
        //         // For this example, we'll just skip or create a basic entry.
        //          sad = new StudentAttendanceData(stt++, student, false, 0,0.0,0,"",0); // Example default
        //     }
        //
        //     // If homework is associated with this session, load homework submission status
        //     if (homeworkId != null && !homeworkId.isEmpty()) {
        //         HomeworkSubmissionModel submission = homeworkSubmissionDAO.getByStudentAndHomework(student.getId(), homeworkId);
        //         if (submission != null) {
        //             sad.setHomeworkSubmission(submission); // This will update grade, submitted status etc.
        //         }
        //     }
        //     addStudentAttendance(sad);
        // }
        // --- End Placeholder ---

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