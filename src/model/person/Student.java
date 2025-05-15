
package src.model.person;

import java.util.ArrayList;
import java.util.List;

//import other classes
import src.model.system.course.Course;

public class Student extends Person {
    private List<Course> currentCourses;
    private Parent parent;
    private String classId; // Added classId field

    // Updated constructor to include classId
    public Student(String id, String name, String gender, String contactNumber, String birthday,
                   String email, Parent parent, String classId) {
        super(id, name, gender, contactNumber, birthday, email);
        this.currentCourses = new ArrayList<>();
        this.parent = parent;
        this.classId = classId; // Initialize classId
    }

    // Default constructor
    public Student() {
        super();
        this.currentCourses = new ArrayList<>();
        // classId will be null by default, can be set via setter
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    // Student-specific methods
    public void enrollCourse(Course course) {
        if (currentCourses == null) {
            currentCourses = new ArrayList<>();
        }
        currentCourses.add(course);
    }

    public void withdrawCourse(Course course) {
        if (currentCourses != null) {
            currentCourses.remove(course);
        }
    }

    // Getters and Setters
    public List<Course> getCurrentCourses() {
        return currentCourses;
    }

    public void setCurrentCourses(List<Course> currentCourses) {
        this.currentCourses = currentCourses;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    // Getter and Setter for classId
    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", Class ID: " + (classId != null ? classId : "N/A") + // Added classId to toString
                ", Number of Current Courses: " + (currentCourses != null ? currentCourses.size() : 0) +
                ", Parent: " + (parent != null ? parent.getName() : "None");
    }
}

