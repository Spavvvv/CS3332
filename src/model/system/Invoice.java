package src.model.system;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


//import other classes
import src.model.person.Teacher;

/**
 * Invoice class represents an invoice entity for courses
 */
public class Invoice {
    // Attributes
    private LocalDate date;
    private List<String> coursesName;
    private Teacher teacher;
    private List<String> coursesID;
    private int totalCurrentStudent;
    private double process;

    /**
     * Default constructor
     */
    public Invoice() {
        this.date = LocalDate.now();
        this.coursesName = new ArrayList<>();
        this.teacher = null;
        this.coursesID = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.process = 0.0;
    }

    /**
     * Parameterized constructor
     *
     * @param date The date of the invoice
     * @param coursesName List of course names
     * @param teacher The authorized teacher
     * @param coursesID List of course IDs
     * @param totalCurrentStudent Total number of current students
     * @param process The process completion percentage
     */
    public Invoice(LocalDate date, List<String> coursesName, Teacher teacher,
                   List<String> coursesID, int totalCurrentStudent, double process) {
        this.date = date;
        this.coursesName = coursesName;
        this.teacher = teacher;
        this.coursesID = coursesID;
        this.totalCurrentStudent = totalCurrentStudent;
        this.process = process;
    }

    // Getters and Setters

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<String> getCoursesName() {
        return coursesName;
    }

    public void setCoursesName(List<String> coursesName) {
        this.coursesName = coursesName;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public List<String> getCoursesID() {
        return coursesID;
    }

    public void setCoursesID(List<String> coursesID) {
        this.coursesID = coursesID;
    }

    public int getTotalCurrentStudent() {
        return totalCurrentStudent;
    }

    public void setTotalCurrentStudent(int totalCurrentStudent) {
        this.totalCurrentStudent = totalCurrentStudent;
    }

    public double getProcess() {
        return process;
    }

    public void setProcess(double process) {
        this.process = process;
    }

    /**
     * Add a course to the invoice
     *
     * @param courseName Name of the course
     * @param courseID ID of the course
     */
    public void addCourse(String courseName, String courseID) {
        this.coursesName.add(courseName);
        this.coursesID.add(courseID);
    }

    /**
     * Calculate the total process based on students and completion
     *
     * @return The calculated process percentage
     */
    public double calculateProcess() {
        // This is a placeholder implementation
        // Actual implementation would depend on business logic
        if (totalCurrentStudent == 0) {
            return 0.0;
        }
        // Example logic: process might be influenced by number of students
        // and other factors specific to your application
        return process;
    }

    /**
     * Returns a string representation of the Invoice object
     *
     * @return String with invoice information
     */
    @Override
    public String toString() {
        return "Invoice{" +
                "date=" + date +
                ", coursesName=" + coursesName +
                ", teacher=" + (teacher != null ? teacher.getName() : "none") +
                ", coursesID=" + coursesID +
                ", totalCurrentStudent=" + totalCurrentStudent +
                ", process=" + process + "%" +
                '}';
    }
}

