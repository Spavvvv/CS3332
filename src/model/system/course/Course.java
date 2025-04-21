package src.model.system.course;

import java.util.List;
import src.model.person.Student;
import src.model.person.Teacher;

public class Course {
    private String courseId;
    private String courseName;
    private String subject;
    private CourseDate date;
    private Teacher teacher;
    private List<Student> students;
    private int totalCurrentStudent;
    private float progress;
    private String roomId;

    public Course(String courseId, String courseName, String subject, CourseDate date) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.date = date;
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public CourseDate getDate() {
        return date;
    }

    public void setDate(CourseDate date) {
        this.date = date;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
        this.totalCurrentStudent = students.size();
    }

    public int getTotalCurrentStudent() {
        return totalCurrentStudent;
    }

    public void updateTotalCurrentStudent() {
        this.totalCurrentStudent = students != null ? students.size() : 0;
    }

    public float getProgress() {
        // Update progress based on course date
        this.progress = (float) date.getProgressPercentage();
        return progress;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void addStudent(Student student) {
        students.add(student);
        updateTotalCurrentStudent();
    }

    public void removeStudent(Student student) {
        students.remove(student);
        updateTotalCurrentStudent();
    }

    @Override
    public String toString() {
        return "Course [" + courseId + "] " + courseName + " - " + subject;
    }
}