package src.model.attendance;

import javafx.beans.property.*;
import src.model.person.Student; // Assuming Student is in this package

// This class can be a public static inner class of ClassroomAttendanceView
// or a separate public class in its own file.
public class StudentAttendanceData {
    private final Student student;
    private final IntegerProperty stt;
    private final BooleanProperty homeworkSubmitted;
    private final IntegerProperty punctualityRating;
    private final DoubleProperty homeworkGrade;
    private final IntegerProperty diligenceRating;
    private final StringProperty studentSessionNotes;
    private final IntegerProperty finalNumericScore;

    public StudentAttendanceData(int stt, Student student, boolean homeworkSubmitted,
                                 int punctualityRating, double homeworkGrade, int diligenceRating,
                                 String studentSessionNotes, int finalNumericScore) {
        this.stt = new SimpleIntegerProperty(stt);
        this.student = student;
        this.homeworkSubmitted = new SimpleBooleanProperty(homeworkSubmitted);
        this.punctualityRating = new SimpleIntegerProperty(punctualityRating);
        this.homeworkGrade = new SimpleDoubleProperty(homeworkGrade);
        this.diligenceRating = new SimpleIntegerProperty(diligenceRating);
        this.studentSessionNotes = new SimpleStringProperty(studentSessionNotes);
        this.finalNumericScore = new SimpleIntegerProperty(finalNumericScore);
    }

    public Student getStudent() { return student; }
    public int getStt() { return stt.get(); }
    public IntegerProperty sttProperty() { return stt; }
    public boolean isHomeworkSubmitted() { return homeworkSubmitted.get(); }
    public BooleanProperty homeworkSubmittedProperty() { return homeworkSubmitted; }
    public void setHomeworkSubmitted(boolean homeworkSubmitted) { this.homeworkSubmitted.set(homeworkSubmitted); }
    public int getPunctualityRating() { return punctualityRating.get(); }
    public IntegerProperty punctualityRatingProperty() { return punctualityRating; }
    public void setPunctualityRating(int punctualityRating) { this.punctualityRating.set(punctualityRating); }
    public double getHomeworkGrade() { return homeworkGrade.get(); }
    public DoubleProperty homeworkGradeProperty() { return homeworkGrade; }
    public void setHomeworkGrade(double homeworkGrade) { this.homeworkGrade.set(homeworkGrade); }
    public int getDiligenceRating() { return diligenceRating.get(); }
    public IntegerProperty diligenceRatingProperty() { return diligenceRating; }
    public void setDiligenceRating(int diligenceRating) { this.diligenceRating.set(diligenceRating); }
    public String getStudentSessionNotes() { return studentSessionNotes.get(); }
    public StringProperty studentSessionNotesProperty() { return studentSessionNotes; }
    public void setStudentSessionNotes(String studentNotes) { this.studentSessionNotes.set(studentNotes); }
    public int getFinalNumericScore() { return finalNumericScore.get(); }
    public IntegerProperty finalNumericScoreProperty() { return finalNumericScore; }
    public void setFinalNumericScore(int finalNumericScore) { this.finalNumericScore.set(finalNumericScore); }
}
