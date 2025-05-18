
package src.model;

import src.model.person.Student;
import src.model.system.course.Course; // Corrected import path
import java.time.LocalDate;
import java.time.LocalDateTime; // Changed from LocalTime
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a class session
 */
public class ClassSession {
    private String id; // session_id
    private String courseId; // Foreign key to Course (academic course)
    private String classId; // ID of the class (e.g., student group or cohort) that this session belongs to

    // This field maps to class_sessions.course_name, which, per FK, is classes.class_name (cohort name)
    private String courseName; // Name of the Class/Cohort, denormalized from classes.class_name

    private String teacherName; // Denormalized teacher name, from teachers.name
    private String roomName;    // Denormalized room name/identifier, from rooms.room_name (maps to 'room' column)

    private LocalDate sessionDate; // The specific date of the session
    private LocalDateTime startTime; // Start date and time of the class session
    private LocalDateTime endTime;   // End date and time of the class session
    private String timeSlot; // Derived from startTime and endTime (e.g., "HH:mm - HH:mm")

    private int sessionNumber; // e.g., 1st, 2nd session in a course series

    private transient Course course; // Reference to the academic Course object
    private transient List<Student> students; // List of students attending the session

    // Default constructor
    public ClassSession() {
        this.students = new ArrayList<>();
    }

    /**
     * Constructor for full manual setup.
     * @param courseName The name of the class/cohort (from classes.class_name).
     */
    public ClassSession(String id, String courseId, String classId, String courseName, // This is cohort name
                        String teacherName, String roomName, LocalDate sessionDate,
                        LocalDateTime startTime, LocalDateTime endTime,
                        int sessionNumber) {
        this.id = id;
        this.courseId = courseId; // Academic course ID
        this.classId = classId;   // Cohort/Group ID
        this.courseName = courseName; // This is the cohort name
        this.teacherName = teacherName;
        this.roomName = roomName;
        this.sessionDate = sessionDate;
        this.setStartTime(startTime);
        this.setEndTime(endTime);
        this.sessionNumber = sessionNumber;
        this.students = new ArrayList<>();
    }


    /**
     * Constructor focused on Course object and essential details for generation.
     * Note: this.courseName (cohort name) is not set via the Course object here.
     * It needs to be set separately if creating a new session to be persisted.
     */
    public ClassSession(String id, Course course, String teacherName, String roomName, LocalDate sessionDate,
                        LocalDateTime startTime, LocalDateTime endTime, String classId, int sessionNumber) {
        this.id = id;
        this.setCourse(course); // Sets this.course and this.courseId
        this.teacherName = teacherName;
        this.roomName = roomName;
        this.sessionDate = sessionDate;
        this.setStartTime(startTime);
        this.setEndTime(endTime);
        this.classId = classId;
        this.sessionNumber = sessionNumber;
        this.students = new ArrayList<>();
        // this.courseName (cohort name) is NOT set here. It must be set via setCourseName() if needed for persistence.
    }


    private String formatTimeSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.toLocalTime().format(formatter) + " - " + end.toLocalTime().format(formatter);
    }

    private void updateTimeSlot() {
        if (this.startTime != null && this.endTime != null) {
            this.timeSlot = formatTimeSlot(this.startTime, this.endTime);
            if (this.sessionDate == null && this.startTime !=null) {
                this.sessionDate = this.startTime.toLocalDate();
            }
        } else {
            this.timeSlot = "";
        }
    }

    // --- Getters and Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the ID of the academic course this session belongs to.
     * @return Academic course ID.
     */
    public String getCourseId() {
        return courseId;
    }

    /**
     * Sets the ID of the academic course this session belongs to.
     * @param courseId Academic course ID.
     */
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    /**
     * Gets the name of the class/cohort (e.g., "Morning Batch A").
     * This maps to the 'course_name' column in 'class_sessions' table,
     * which is foreign-keyed to 'classes.class_name'.
     * @return The name of the class/cohort.
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * Sets the name of the class/cohort.
     * @param courseName The name of the class/cohort.
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacher() {
        return teacherName;
    }

    public void setTeacher(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getRoom() {
        return roomName;
    }

    public void setRoom(String roomName) {
        this.roomName = roomName;
    }

    public LocalDate getDate() {
        if (sessionDate == null && startTime != null) {
            return startTime.toLocalDate();
        }
        return sessionDate;
    }

    private void setSessionDate(LocalDate sessionDate) { // Private setter for internal use by copy() and setDate()
        this.sessionDate = sessionDate;
        if (this.startTime != null) {
            LocalTime st = this.startTime.toLocalTime();
            this.startTime = LocalDateTime.of(sessionDate, st);
        }
        if (this.endTime != null) {
            LocalTime et = this.endTime.toLocalTime();
            this.endTime = LocalDateTime.of(sessionDate, et);
        }
    }

    public void setDate(LocalDate sessionDate) {
        setSessionDate(sessionDate); // Uses the private setter
        updateTimeSlot();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        if (startTime != null) {
            // Automatically update sessionDate if startTime is set
            if (this.sessionDate == null || !this.sessionDate.equals(startTime.toLocalDate())) {
                setSessionDate(startTime.toLocalDate()); // Use private setter to avoid re-updating timeslot before endTime is also set
            }
        }
        updateTimeSlot(); // Update timeslot after startTime potentially changed
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        // No need to update sessionDate from endTime
        updateTimeSlot(); // Update timeslot after endTime potentially changed
    }

    public String getTimeSlot() {
        if (timeSlot == null || timeSlot.isEmpty()) {
            updateTimeSlot();
        }
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }
    public int getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    /**
     * Gets the academic Course object associated with this session.
     * @return The Course object.
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the academic Course object for this session.
     * This method sets the transient Course field and the courseId.
     * It does NOT set this.courseName (which is the cohort name) from the Course object.
     * @param course The academic Course object.
     */
    public void setCourse(Course course) {
        this.course = course;
        if (course != null) {
            this.courseId = course.getCourseId();
            // REMOVED: this.courseName = course.getCourseName();
            // this.courseName is the COHORT name, sourced from class_sessions.course_name (FK to classes.class_name)
            // It should be set via setCourseName() or by the DAO.
        } else {
            this.courseId = null;
        }
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
    }


    // --- Utility Methods ---

    public boolean addStudent(Student student) {
        if (student == null || student.getId() == null) return false;
        if (this.students == null) this.students = new ArrayList<>();
        for (Student s : students) {
            if (s.getId().equals(student.getId())) {
                return false;
            }
        }
        return students.add(student);
    }

    public boolean removeStudent(String studentId) {
        if (studentId == null || this.students == null) return false;
        return this.students.removeIf(student -> student.getId().equals(studentId));
    }

    public boolean hasStudent(String studentId) {
        if (studentId == null || this.students == null) return false;
        for (Student student : students) {
            if (student.getId().equals(studentId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate currentSessionDate = getDate();
        if (currentSessionDate == null) return false;
        return !currentSessionDate.isBefore(fromDate) && !currentSessionDate.isAfter(toDate);
    }

    public boolean isWithinCourseDate() {
        if (course == null || course.getStartDate() == null || course.getEndDate() == null) return false;
        LocalDate currentSessionDate = getDate();
        if (currentSessionDate == null) return false;
        return !currentSessionDate.isBefore(course.getStartDate()) && !currentSessionDate.isAfter(course.getEndDate());
    }

    public boolean conflicts(ClassSession other) {
        if (other == null || this.startTime == null || this.endTime == null ||
                other.startTime == null || other.endTime == null) {
            return false;
        }
        if (!Objects.equals(this.getDate(), other.getDate())) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    public boolean isPast() {
        LocalDateTime now = LocalDateTime.now();
        return endTime != null && endTime.isBefore(now);
    }

    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null &&
                !now.isBefore(startTime) && now.isBefore(endTime); // Corrected: now should be before endTime
    }

    public String getFormattedDate() {
        LocalDate currentSessionDate = getDate();
        if (currentSessionDate == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return currentSessionDate.format(formatter);
    }

    /**
     * Provides a summary string. Note: `courseName` here refers to the cohort name.
     * To get the academic course name, use `getCourse().getCourseName()`.
     * @return Summary string.
     */
    public String getSummary() {
        // If you want academic course name: (course != null ? course.getCourseName() : "N/A Course")
        return (this.courseName != null ? this.courseName : "N/A Cohort") // Clarified this is cohort name
                + " - " + (teacherName != null ? teacherName : "N/A Teacher")
                + " - " + (roomName != null ? roomName : "N/A Room")
                + " - " + getTimeSlot();
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        // Displaying academic course name if available from the Course object
        sb.append("Academic Course: ").append(course != null && course.getCourseName() != null ? course.getCourseName() : (courseId != null ? "ID: "+courseId : "N/A")).append("\n");
        sb.append("Cohort/Class Name: ").append(courseName != null ? courseName : "N/A").append("\n"); // This is from class_sessions.course_name
        sb.append("Teacher: ").append(teacherName != null ? teacherName : "N/A").append("\n");
        sb.append("Room: ").append(roomName != null ? roomName : "N/A").append("\n");
        sb.append("Date: ").append(getFormattedDate()).append("\n");
        sb.append("Time: ").append(getTimeSlot()).append("\n");
        sb.append("Session Number: ").append(sessionNumber > 0 ? sessionNumber : "N/A").append("\n");
        if (students != null) {
            sb.append("Student Count: ").append(students.size()).append("\n");
        }
        return sb.toString();
    }

    public long getDurationMinutes() {
        if (startTime == null || endTime == null) return -1;
        java.time.Duration duration = java.time.Duration.between(startTime, endTime);
        return duration.toMinutes();
    }

    public boolean isTaughtBy(String teacherNameParam) {
        return this.teacherName != null && this.teacherName.equals(teacherNameParam);
    }

    public String getDayOfWeek() {
        LocalDate currentSessionDate = getDate();
        if (currentSessionDate == null) return "";
        return currentSessionDate.getDayOfWeek().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassSession that = (ClassSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // Clarify courseName is cohort name
        return "ClassSession{" +
                "id='" + id + '\'' +
                ", cohortName='" + courseName + '\'' + // Renamed in toString for clarity
                (course != null && course.getCourseName() != null ? ", academicCourseName='" + course.getCourseName() + '\'' : "") +
                ", sessionDate=" + getFormattedDate() +
                ", timeSlot='" + getTimeSlot() + '\'' +
                ", sessionNumber=" + sessionNumber +
                '}';
    }

    public ClassSession copy() {
        ClassSession copy = new ClassSession();
        copy.setId(this.id);
        copy.setCourseId(this.courseId);
        copy.setClassId(this.classId);
        copy.setCourseName(this.courseName); // Copies the cohort name
        copy.setTeacher(this.teacherName);
        copy.setRoom(this.roomName);
        // Use the public setDate to ensure consistent date part updates for startTime/endTime
        if (this.sessionDate != null) {
            copy.setDate(this.sessionDate);
        }
        // Explicitly set start and end times if they exist, after date is set.
        // setDate might clear time parts if it re-initializes LocalDateTime from LocalDate + LocalTime.
        // The current setDate maintains time parts, but being explicit is safer.
        if (this.startTime != null) copy.setStartTime(LocalDateTime.from(this.startTime));
        if (this.endTime != null) copy.setEndTime(LocalDateTime.from(this.endTime));

        copy.setSessionNumber(this.sessionNumber);

        if (this.course != null) {
            copy.setCourse(this.course); // Sets transient course and courseId. Does not touch copy.courseName.
        }
        if (this.students != null) {
            copy.setStudents(new ArrayList<>(this.students));
        }
        return copy;
    }

    public int getStatus() {
        if (isPast()) {
            return 0; // Past
        } else if (isOngoing()) {
            return 1; // Ongoing
        } else {
            return 2; // Upcoming
        }
    }
}

