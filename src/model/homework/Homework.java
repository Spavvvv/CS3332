package src.model.homework; // Hoặc package chính xác của bạn

import java.time.LocalDate; // Sử dụng LocalDate cho assigned_date
import java.time.LocalDateTime;
import java.util.Objects;

public class Homework {
    private String homeworkId;
    private String courseId;
    private String title;
    private String description;
    private LocalDate assignedDate; // MỚI: Thay thế cho dueDate, kiểu DATE trong DB
    private String status;          // MỚI: VARCHAR(50)
    private Double score;           // MỚI: DOUBLE, dùng kiểu đối tượng để cho phép null
    private LocalDateTime submissionDate; // MỚI: TIMESTAMP
    private String assignedInSessionId; // ID của ClassSession mà bài tập này được giao (giữ nguyên)

    // Constructors
    public Homework() {
        // Có thể không cần khởi tạo createdAt/updatedAt nữa vì DB không có
    }

    // Constructor đầy đủ hơn với các trường mới (bạn có thể tùy chỉnh)
    public Homework(String homeworkId, String courseId, String title, String description,
                    LocalDate assignedDate, String status, Double score, LocalDateTime submissionDate,
                    String assignedInSessionId) {
        this.homeworkId = homeworkId;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.assignedDate = assignedDate;
        this.status = status;
        this.score = score;
        this.submissionDate = submissionDate;
        this.assignedInSessionId = assignedInSessionId;
    }

    // Getters and Setters
    public String getHomeworkId() {
        return homeworkId;
    }

    public void setHomeworkId(String homeworkId) {
        this.homeworkId = homeworkId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) { // Sửa tên tham số nếu cần, nhưng hiện tại đã đúng
        this.courseId = courseId;
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

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getAssignedInSessionId() {
        return assignedInSessionId;
    }

    public void setAssignedInSessionId(String assignedInSessionId) {
        this.assignedInSessionId = assignedInSessionId;
    }

    // equals, hashCode, toString (toString cần cập nhật)
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
                ", courseId='" + courseId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", assignedDate=" + assignedDate +
                ", status='" + status + '\'' +
                ", score=" + score +
                ", submissionDate=" + submissionDate +
                ", assignedInSessionId='" + assignedInSessionId + '\'' +
                '}';
    }
}