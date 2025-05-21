package src.model.homework;

import java.time.LocalDateTime;
import java.util.Objects;

public class Homework {
    private String homeworkId;
    private String classId; // ID của lớp mà bài tập này thuộc về
    private String title;
    private String description;
    private String assignedInSessionId; // ID của ClassSession mà bài tập này được giao
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Homework() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Homework(String homeworkId, String classId, String title, String description, String assignedInSessionId, LocalDateTime dueDate) {
        this.homeworkId = homeworkId;
        this.classId = classId;
        this.title = title;
        this.description = description;
        this.assignedInSessionId = assignedInSessionId;
        this.dueDate = dueDate;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getHomeworkId() {
        return homeworkId;
    }

    public void setHomeworkId(String homeworkId) {
        this.homeworkId = homeworkId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedInSessionId() {
        return assignedInSessionId;
    }

    public void setAssignedInSessionId(String assignedInSessionId) {
        this.assignedInSessionId = assignedInSessionId;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Homework homework = (Homework) o;
        return Objects.equals(homeworkId, homework.homeworkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeworkId);
    }

    @Override
    public String toString() {
        return "Homework{" +
                "homeworkId='" + homeworkId + '\'' +
                ", classId='" + classId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", assignedInSessionId='" + assignedInSessionId + '\'' +
                ", dueDate=" + dueDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}