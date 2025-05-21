package src.model.person;

import java.util.ArrayList;
import java.util.List;
import src.model.system.course.Course;

public class Student extends Person {
    private List<Course> currentCourses;
    private String parentName;
    private String parentPhoneNumber;
    private String userId; // Maintained as an attribute

    // Constructor without userId (complies with intent)
    public Student(String id, String name, String gender, String contactNumber, String birthday, String email) {
        super(id, name, gender, contactNumber, birthday, email);
        this.currentCourses = new ArrayList<>();
        this.parentName = "";
        this.parentPhoneNumber = "";
        // userId is auto-generated and not passed to the constructor
        this.userId = null;
    }

    // No-argument constructor
    public Student() {
        super();
        this.currentCourses = new ArrayList<>();
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    // Getters and Setters for courses
    public List<Course> getCurrentCourses() {
        return currentCourses;
    }

    public void setCurrentCourses(List<Course> currentCourses) {
        this.currentCourses = currentCourses;
    }

    // Getters and Setters for parent-related attributes
    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentPhoneNumber() {
        return parentPhoneNumber;
    }

    public void setParentPhoneNumber(String parentPhoneNumber) {
        this.parentPhoneNumber = parentPhoneNumber;
    }
    // Getters and Setters for userId
    public String getUserId() {
        return userId; // To be set externally if required
    }

    public void setUserId(String userId) {
        this.userId = userId; // Set when obtained from database or external logic
    }

    // Getters/Setters for status (already existing)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", Number of Current Courses: " + (currentCourses != null ? currentCourses.size() : 0) +
                ", Parent Name: " + (parentName != null ? parentName : "None") +
                ", Parent Phone: " + (parentPhoneNumber != null ? parentPhoneNumber : "None");
    }
}

