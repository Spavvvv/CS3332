// Teacher.java
package src.model.person;

import java.util.ArrayList;
import java.util.List;

public class Teacher extends Person {
    private String teacherId;
    private List<String> subjects;
    private String position;

    public Teacher(String id, String name, String gender, String contactNumber, String birthday,
                   String teacherId, String position) {
        super(id, name, gender, contactNumber, birthday);
        this.teacherId = teacherId;
        this.position = position;
        this.subjects = new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "Teacher";
    }

    // Teacher-specific methods
    public void addSubject(String subject) {
        if (subjects == null) {
            subjects = new ArrayList<>();
        }
        subjects.add(subject);
    }

    public void removeSubject(String subject) {
        if (subjects != null) {
            subjects.remove(subject);
        }
    }

    // Getters and Setters
    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", Teacher ID: " + teacherId +
                ", Position: " + position +
                ", Subjects: " + subjects;
    }
}
