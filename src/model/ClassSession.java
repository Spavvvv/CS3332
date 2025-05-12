package src.model;

import src.model.person.Student;
import src.model.system.course.Course;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a class session
 */
public class ClassSession {
    private String id;
    private String courseName;
    private String teacher;
    private String room;
    private LocalDate date;
    private String timeSlot;
    private LocalTime startTime; // Start time of the class session
    private LocalTime endTime;   // End time of the class session
    private String classId; // ID of the class that this session belongs to
    private List<Student> students; // List of students attending the session
    private Course course; // Reference to Course object

    public ClassSession(String id, Course course, String teacher, String room, LocalDate date, String timeSlot) {
        this.id = id;
        this.courseName = course.getCourseName();
        this.teacher = teacher;
        this.room = room;
        this.date = date;
        this.timeSlot = timeSlot;
        this.course = course;
        this.students = new ArrayList<>();
    }

    public ClassSession(String id, Course course, String teacher, String room, LocalDate date, String timeSlot, String classId) {
        this(id, course, teacher, room, date, timeSlot);
        this.classId = classId;
    }

    public ClassSession(String id, Course course, String teacher, String room, LocalDate date,
                        LocalTime startTime, LocalTime endTime, String classId) {
        this.id = id;
        this.courseName = course.getCourseName();
        this.teacher = teacher;
        this.room = room;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeSlot = formatTimeSlot(startTime, endTime);
        this.classId = classId;
        this.course = course;
        this.students = new ArrayList<>();
    }

    public ClassSession() {
        this.students = new ArrayList<>();
    }

    private String formatTimeSlot(LocalTime start, LocalTime end) {
        if (start == null || end == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + " - " + end.format(formatter);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        updateTimeSlot();
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        updateTimeSlot();
    }

    private void updateTimeSlot() {
        if (startTime != null && endTime != null) {
            this.timeSlot = formatTimeSlot(startTime, endTime);
        }
    }

    /**
     * Gets the Course object associated with this class session
     * @return the Course object
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Sets the Course object for this class session
     * @param course the Course object
     */
    public void setCourse(Course course) {
        this.course = course;
        this.courseName = course.getCourseName(); // Ensure the course name is updated
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public boolean addStudent(Student student) {
        if (student == null) return false;
        for (Student s : students) {
            if (s.getId().equals(student.getId())) {
                return false; // Student already exists
            }
        }
        return students.add(student);
    }

    public boolean removeStudent(String studentId) {
        if (studentId == null) return false;
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getId().equals(studentId)) {
                students.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean hasStudent(String studentId) {
        if (studentId == null) return false;
        for (Student student : students) {
            if (student.getId().equals(studentId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInDateRange(LocalDate fromDate, LocalDate toDate) {
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    public boolean isWithinCourseDate() {
        if (course == null) return false;
        return course.getStartDate().isBefore(date) && course.getEndDate().isAfter(date);
    }

    public boolean conflicts(ClassSession other) {
        if (!this.date.equals(other.date)) {
            return false;
        }

        if (this.startTime == null || this.endTime == null ||
                other.startTime == null || other.endTime == null) {
            return this.timeSlot.equals(other.timeSlot);
        }

        return !this.startTime.isAfter(other.endTime) && !this.endTime.isBefore(other.startTime);
    }

    public boolean isPast() {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return true;
        }
        if (date.isEqual(today) && endTime != null) {
            return LocalTime.now().isAfter(endTime);
        }
        return false;
    }

    public boolean isOngoing() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (!date.isEqual(today)) {
            return false;
        }

        if (startTime == null || endTime == null) {
            return false;
        }

        return !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    public String getSummary() {
        String time = (startTime != null && endTime != null)
                ? startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "-" + endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                : timeSlot;

        return courseName + " - " + teacher + " - " + room + " - " + time;
    }

    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Course: ").append(courseName).append("\n");
        sb.append("Teacher: ").append(teacher).append("\n");
        sb.append("Room: ").append(room).append("\n");
        sb.append("Date: ").append(getFormattedDate()).append("\n");

        if (startTime != null && endTime != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            sb.append("Start Time: ").append(startTime.format(timeFormatter)).append("\n");
            sb.append("End Time: ").append(endTime.format(timeFormatter)).append("\n");
        } else {
            sb.append("Time Slot: ").append(timeSlot).append("\n");
        }

        sb.append("Student Count: ").append(students.size());
        return sb.toString();
    }

    public int getDurationMinutes() {
        if (startTime == null || endTime == null) return -1;

        int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
        int endMinutes = endTime.getHour() * 60 + endTime.getMinute();

        return endMinutes - startMinutes;
    }

    public boolean isTaughtBy(String teacherName) {
        return teacher != null && teacher.equals(teacherName);
    }

    public String getDayOfWeek() {
        int dayValue = date.getDayOfWeek().getValue();
        switch (dayValue) {
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            case 6: return "Saturday";
            case 7: return "Sunday";
            default: return "";
        }
    }

    public String getClassName() {
        return courseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassSession that = (ClassSession) o;
        return id.equals(that.id) &&
                classId.equals(that.classId) &&
                Objects.equals(courseName, that.courseName) &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(room, that.room) &&
                Objects.equals(date, that.date) &&
                Objects.equals(timeSlot, that.timeSlot) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courseName, teacher, room, date, timeSlot, startTime, endTime, classId);
    }

    @Override
    public String toString() {
        String timeInfo = (startTime != null && endTime != null)
                ? startTime + " - " + endTime
                : timeSlot;

        return "ClassSession{" +
                "id='" + id + '\'' +
                ", courseName='" + courseName + '\'' +
                ", teacher='" + teacher + '\'' +
                ", room='" + room + '\'' +
                ", date=" + date +
                ", time='" + timeInfo + '\'' +
                ", classId='" + classId + '\'' +
                ", studentCount=" + (students != null ? students.size() : 0) +
                '}';
    }

    public ClassSession copy() {
        ClassSession copy = new ClassSession();
        copy.setId(id);
        copy.setCourseName(courseName);
        copy.setTeacher(teacher);
        copy.setRoom(room);
        copy.setDate(date);
        copy.setTimeSlot(timeSlot);
        copy.setStartTime(startTime);
        copy.setEndTime(endTime);
        copy.setClassId(classId);
        copy.setCourse(course);

        if (students != null) {
            copy.setStudents(new ArrayList<>(students));
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

    public String getSessionDate() {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getSchedule() {
        return timeSlot;
    }
}
