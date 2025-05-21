package src.model.attendance;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import src.model.person.Student;
import java.time.LocalDateTime;

/**
 * Represents attendance data for a student in a class session
 */
public class StudentAttendanceData {
    private Student student;
    private final BooleanProperty homeworkSubmitted = new SimpleBooleanProperty(false);
    private javafx.beans.property.IntegerProperty punctualityRating;
    private javafx.beans.property.IntegerProperty diligenceRating;
    private double homeworkGrade;
    private String studentSessionNotes;
    private int finalNumericScore;
    private LocalDateTime submissionDate; // Added field for homework submission date/time

    /**
     * Constructor for StudentAttendanceData
     * @param student The student object
     */
    public StudentAttendanceData(Student student) {
        this.student = student;
        this.punctualityRating = new javafx.beans.property.SimpleIntegerProperty(0);
        this.diligenceRating = new javafx.beans.property.SimpleIntegerProperty(0);
        this.homeworkGrade = 0.0;
        this.studentSessionNotes = "";
        this.finalNumericScore = 0;
        this.submissionDate = null; // Initialize as null until a submission is recorded
    }

    /**
     * Gets the student
     * @return The student
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Sets the student
     * @param student The student to set
     */
    public void setStudent(Student student) {
        this.student = student;
    }

    /**
     * Gets the homeworkSubmitted property
     * @return The homeworkSubmitted property
     */
    public BooleanProperty homeworkSubmittedProperty() {
        return homeworkSubmitted;
    }

    /**
     * Checks if homework was submitted
     * @return true if homework was submitted, false otherwise
     */
    public boolean isHomeworkSubmitted() {
        return homeworkSubmitted.get();
    }

    /**
     * Sets the homework submission status
     * @param submitted true if homework was submitted, false otherwise
     */
    public void setHomeworkSubmitted(boolean submitted) {
        this.homeworkSubmitted.set(submitted);

        // When homework is submitted, automatically set submission date to now if not already set
        if (submitted && this.submissionDate == null) {
            this.submissionDate = LocalDateTime.now();
        }
    }

    /**
     * Gets the date/time when the homework was submitted
     * @return The submission date/time, or null if not submitted
     */
    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    /**
     * Sets the submission date/time
     * @param submissionDate The submission date/time to set
     */
    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    /**
     * Gets the punctuality rating property
     * @return The punctuality rating property
     */
    public javafx.beans.property.IntegerProperty punctualityRatingProperty() {
        return punctualityRating;
    }

    /**
     * Gets the punctuality rating value
     * @return The punctuality rating value
     */
    public int getPunctualityRating() {
        return punctualityRating.get();
    }

    /**
     * Sets the punctuality rating
     * @param rating The rating to set
     */
    public void setPunctualityRating(int rating) {
        this.punctualityRating.set(rating);
    }

    /**
     * Gets the diligence rating property
     * @return The diligence rating property
     */
    public javafx.beans.property.IntegerProperty diligenceRatingProperty() {
        return diligenceRating;
    }

    /**
     * Gets the diligence rating value
     * @return The diligence rating value
     */
    public int getDiligenceRating() {
        return diligenceRating.get();
    }

    /**
     * Sets the diligence rating
     * @param rating The rating to set
     */
    public void setDiligenceRating(int rating) {
        this.diligenceRating.set(rating);
    }

    /**
     * Gets the homework grade
     * @return The homework grade
     */
    public double getHomeworkGrade() {
        return homeworkGrade;
    }

    /**
     * Sets the homework grade
     * @param homeworkGrade The homework grade to set
     */
    public void setHomeworkGrade(double homeworkGrade) {
        this.homeworkGrade = homeworkGrade;
    }

    /**
     * Gets the student session notes
     * @return The student session notes
     */
    public String getStudentSessionNotes() {
        return studentSessionNotes;
    }

    /**
     * Sets the student session notes
     * @param studentSessionNotes The notes to set
     */
    public void setStudentSessionNotes(String studentSessionNotes) {
        this.studentSessionNotes = studentSessionNotes;
    }

    /**
     * Gets the final numeric score
     * @return The final numeric score
     */
    public int getFinalNumericScore() {
        return finalNumericScore;
    }

    /**
     * Sets the final numeric score
     * @param finalNumericScore The score to set
     */
    public void setFinalNumericScore(int finalNumericScore) {
        this.finalNumericScore = finalNumericScore;
    }

    /**
     * Calculates a composite score based on punctuality, diligence, and homework
     * @return A calculated composite score
     */
    public int calculateCompositeScore() {
        // This is a sample calculation, adjust according to your grading policy
        double punctualityWeight = 0.3;
        double diligenceWeight = 0.3;
        double homeworkWeight = 0.4;

        double weightedScore =
                (getPunctualityRating() * punctualityWeight) +
                        (getDiligenceRating() * diligenceWeight) +
                        (homeworkGrade * homeworkWeight / 2); // Assuming homework is on a 10-point scale

        return (int) Math.round(weightedScore);
    }

    /**
     * Updates the final score based on the calculated composite score
     */
    public void updateFinalScore() {
        this.finalNumericScore = calculateCompositeScore();
    }
}