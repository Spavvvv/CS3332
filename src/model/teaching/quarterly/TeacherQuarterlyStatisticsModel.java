package src.model.teaching.quarterly;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TeacherQuarterlyStatisticsModel {
    private final SimpleStringProperty teacherName;
    private final SimpleIntegerProperty stt;
    private final SimpleIntegerProperty q1Sessions;
    private final SimpleDoubleProperty q1Hours;
    private final SimpleIntegerProperty q2Sessions;
    private final SimpleDoubleProperty q2Hours;
    private final SimpleIntegerProperty q3Sessions; // Added Q3 sessions
    private final SimpleDoubleProperty q3Hours;   // Added Q3 hours
    private final SimpleIntegerProperty q4Sessions; // Added Q4 sessions
    private final SimpleDoubleProperty q4Hours;   // Added Q4 hours
    private final SimpleIntegerProperty totalSessions;
    private final SimpleDoubleProperty totalHours;

    // Updated constructor to include all four quarters
    public TeacherQuarterlyStatisticsModel(int stt, String teacherName,
                                           int q1Sessions, double q1Hours,
                                           int q2Sessions, double q2Hours,
                                           int q3Sessions, double q3Hours,
                                           int q4Sessions, double q4Hours) {
        this.stt = new SimpleIntegerProperty(stt);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.q1Sessions = new SimpleIntegerProperty(q1Sessions);
        this.q1Hours = new SimpleDoubleProperty(q1Hours);
        this.q2Sessions = new SimpleIntegerProperty(q2Sessions);
        this.q2Hours = new SimpleDoubleProperty(q2Hours);
        this.q3Sessions = new SimpleIntegerProperty(q3Sessions);
        this.q3Hours = new SimpleDoubleProperty(q3Hours);
        this.q4Sessions = new SimpleIntegerProperty(q4Sessions);
        this.q4Hours = new SimpleDoubleProperty(q4Hours);

        // Updated total calculation to sum all four quarters
        this.totalSessions = new SimpleIntegerProperty(q1Sessions + q2Sessions + q3Sessions + q4Sessions);
        this.totalHours = new SimpleDoubleProperty(q1Hours + q2Hours + q3Hours + q4Hours);
    }

    // --- Getters and Property methods ---

    public String getTeacherName() {
        return teacherName.get();
    }

    public SimpleStringProperty teacherNameProperty() {
        return teacherName;
    }

    public int getStt() {
        return stt.get();
    }

    public SimpleIntegerProperty sttProperty() {
        return stt;
    }

    public int getQ1Sessions() {
        return q1Sessions.get();
    }

    public SimpleIntegerProperty q1SessionsProperty() {
        return q1Sessions;
    }

    public double getQ1Hours() {
        return q1Hours.get();
    }

    public SimpleDoubleProperty q1HoursProperty() {
        return q1Hours;
    }

    public int getQ2Sessions() {
        return q2Sessions.get();
    }

    public SimpleIntegerProperty q2SessionsProperty() {
        return q2Sessions;
    }

    public double getQ2Hours() {
        return q2Hours.get();
    }

    public SimpleDoubleProperty q2HoursProperty() {
        return q2Hours;
    }

    // Added getters and property methods for Q3
    public int getQ3Sessions() {
        return q3Sessions.get();
    }

    public SimpleIntegerProperty q3SessionsProperty() {
        return q3Sessions;
    }

    public double getQ3Hours() {
        return q3Hours.get();
    }

    public SimpleDoubleProperty q3HoursProperty() {
        return q3Hours;
    }

    // Added getters and property methods for Q4
    public int getQ4Sessions() {
        return q4Sessions.get();
    }

    public SimpleIntegerProperty q4SessionsProperty() {
        return q4Sessions;
    }

    public double getQ4Hours() {
        return q4Hours.get();
    }

    public SimpleDoubleProperty q4HoursProperty() {
        return q4Hours;
    }

    public int getTotalSessions() {
        return totalSessions.get();
    }

    public SimpleIntegerProperty totalSessionsProperty() {
        return totalSessions;
    }

    public double getTotalHours() {
        return totalHours.get();
    }

    public SimpleDoubleProperty totalHoursProperty() {
        return totalHours;
    }
}
