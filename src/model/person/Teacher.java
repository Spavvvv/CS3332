package src.model.person;

import java.util.ArrayList;
import java.util.List;

public class Teacher extends Person {
    private String userId;
    private String address;
    private List<String> subjects;

    public Teacher(String id, String name, String gender, String contactNumber, String birthday, String email,
                   String userId, String address) {
        super(id, name, gender, contactNumber, birthday, email);
        this.userId = userId;
        this.address = address;
        this.subjects = new ArrayList<>();

        System.out.println("Teacher constructor - id (from Person): " + super.id);
        System.out.println("Teacher constructor - userId: " + this.userId);
    }

    @Override
    public Role getRole() {
        return Role.TEACHER;
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
    public String getUserId() {
        System.out.println("Teacher.getUserId() called - returning: " + this.userId);
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", User ID: " + userId +
                ", Address: " + address +
                ", Subjects: " + subjects;
    }
}