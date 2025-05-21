package src.model.attendance;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;

/**
 * Model class representing a student's homework submission record
 * Corresponds to the student_homework_submissions table in the database
 */
public class HomeworkSubmissionModel {
    private final StringProperty studentSubmissionId;
    private final StringProperty studentId;
    private final StringProperty homeworkId;
    private final BooleanProperty submitted;
    private final DoubleProperty grade;
    private final ObjectProperty<LocalDateTime> submissionTimestamp;
    private final StringProperty evaluatorNotes;
    private final StringProperty checkedInSessionId;


    private final StringProperty submissionId;
    private final StringProperty sessionId;
    private final StringProperty notes;
    private final ObjectProperty<Integer> punctualityRating;
    private final ObjectProperty<Integer> diligenceRating;
    private final ObjectProperty<Integer> finalScore;
    private final ObjectProperty<LocalDateTime> submittedAt;

    /**
     * Constructor for creating a new homework submission record
     */
    public HomeworkSubmissionModel(String studentSubmissionId, String studentId, String homeworkId,
                                   boolean submitted, Double grade, LocalDateTime submissionTimestamp,
                                   String evaluatorNotes, String checkedInSessionId) {
        this.studentSubmissionId = new SimpleStringProperty(studentSubmissionId);
        this.studentId = new SimpleStringProperty(studentId);
        this.homeworkId = new SimpleStringProperty(homeworkId);
        this.submitted = new SimpleBooleanProperty(submitted);
        this.grade = new SimpleDoubleProperty(grade != null ? grade : 0.0);
        this.submissionTimestamp = new SimpleObjectProperty<>(submissionTimestamp);
        this.evaluatorNotes = new SimpleStringProperty(evaluatorNotes);
        this.checkedInSessionId = new SimpleStringProperty(checkedInSessionId);

        // Initialize new properties
        this.submissionId = new SimpleStringProperty(studentSubmissionId); // Reusing studentSubmissionId
        this.sessionId = new SimpleStringProperty(checkedInSessionId);     // Reusing checkedInSessionId
        this.notes = new SimpleStringProperty(evaluatorNotes);             // Reusing evaluatorNotes
        this.punctualityRating = new SimpleObjectProperty<>(null);
        this.diligenceRating = new SimpleObjectProperty<>(null);
        this.finalScore = new SimpleObjectProperty<>(null);
        this.submittedAt = new SimpleObjectProperty<>(submissionTimestamp); // Reusing submissionTimestamp
    }

    /**
     * Default constructor for creating a new empty homework submission
     */
    public HomeworkSubmissionModel() {
        this(null, null, null, false, 0.0, null, null, null);
    }

    // Getters and setters with JavaFX property pattern

    public String getStudentSubmissionId() {
        return studentSubmissionId.get();
    }

    public StringProperty studentSubmissionIdProperty() {
        return studentSubmissionId;
    }

    public void setStudentSubmissionId(String studentSubmissionId) {
        this.studentSubmissionId.set(studentSubmissionId);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId.set(studentId);
    }

    public String getHomeworkId() {
        return homeworkId.get();
    }

    public StringProperty homeworkIdProperty() {
        return homeworkId;
    }

    public void setHomeworkId(String homeworkId) {
        this.homeworkId.set(homeworkId);
    }

    public boolean isSubmitted() {
        return submitted.get();
    }

    public BooleanProperty submittedProperty() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted.set(submitted);
    }

    public double getGrade() {
        return grade.get();
    }

    public DoubleProperty gradeProperty() {
        return grade;
    }

    public void setGrade(double grade) {
        this.grade.set(grade);
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp.get();
    }

    public ObjectProperty<LocalDateTime> submissionTimestampProperty() {
        return submissionTimestamp;
    }

    public void setSubmissionTimestamp(LocalDateTime submissionTimestamp) {
        this.submissionTimestamp.set(submissionTimestamp);
    }

    public String getEvaluatorNotes() {
        return evaluatorNotes.get();
    }

    public StringProperty evaluatorNotesProperty() {
        return evaluatorNotes;
    }

    public void setEvaluatorNotes(String evaluatorNotes) {
        this.evaluatorNotes.set(evaluatorNotes);
    }

    public String getCheckedInSessionId() {
        return checkedInSessionId.get();
    }

    public StringProperty checkedInSessionIdProperty() {
        return checkedInSessionId;
    }

    public void setCheckedInSessionId(String checkedInSessionId) {
        this.checkedInSessionId.set(checkedInSessionId);
    }

    @Override
    public String toString() {
        return "HomeworkSubmissionModel{" +
                "studentSubmissionId=" + getStudentSubmissionId() +
                ", studentId=" + getStudentId() +
                ", homeworkId=" + getHomeworkId() +
                ", submitted=" + isSubmitted() +
                ", grade=" + getGrade() +
                ", submissionTimestamp=" + getSubmissionTimestamp() +
                '}';
    }

    /**
     * Gets the submission ID.
     * @return The submission ID
     */
    public String getSubmissionId() {
        return submissionId.get();
    }
    /**
     * Sets the submission ID.
     * @param submissionId The submission ID to set
     */
    public void setSubmissionId(String submissionId) {
        this.submissionId.set(submissionId);
    }
    /**
     * Gets the session ID.
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId.get();
    }
    /**
     * Sets the session ID.
     * @param sessionId The session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId.set(sessionId);
    }
    /**
     * Gets the student notes.
     * @return The notes
     */
    public String getNotes() {
        return notes.get();
    }
    /**
     * Sets the student notes.
     * @param notes The notes to set
     */
    public void setNotes(String notes) {
        this.notes.set(notes);
    }
    /**
     * Gets the punctuality rating.
     * @return The punctuality rating
     */
    public Integer getPunctualityRating() {
        return punctualityRating.get();
    }
    /**
     * Sets the punctuality rating.
     * @param rating The rating to set
     */
    public void setPunctualityRating(Integer rating) {
        this.punctualityRating.set(rating);
    }
    /**
     * Gets the diligence rating.
     * @return The diligence rating
     */
    public Integer getDiligenceRating() {
        return diligenceRating.get();
    }
    /**
     * Sets the diligence rating.
     * @param rating The rating to set
     */
    public void setDiligenceRating(Integer rating) {
        this.diligenceRating.set(rating);
    }
    /**
     * Gets the final score.
     * @return The final score
     */
    public Integer getFinalScore() {
        return finalScore.get();
    }
    /**
     * Sets the final score.
     * @param score The score to set
     */
    public void setFinalScore(Integer score) {
        this.finalScore.set(score);
    }
    /**
     * Gets the submission timestamp.
     * @return The submission timestamp
     */
    public LocalDateTime getSubmittedAt() {
        return submittedAt.get();
    }
    /**
     * Sets the submission timestamp.
     * @param timestamp The timestamp to set
     */
    public void setSubmittedAt(LocalDateTime timestamp) {
        this.submittedAt.set(timestamp);
    }
}
