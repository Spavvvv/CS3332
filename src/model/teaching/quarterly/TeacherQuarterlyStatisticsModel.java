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
    private final SimpleIntegerProperty totalSessions;
    private final SimpleDoubleProperty totalHours;

    public TeacherQuarterlyStatisticsModel(int stt, String teacherName,
                                           int q1Sessions, double q1Hours,
                                           int q2Sessions, double q2Hours) {
        this.stt = new SimpleIntegerProperty(stt);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.q1Sessions = new SimpleIntegerProperty(q1Sessions);
        this.q1Hours = new SimpleDoubleProperty(q1Hours);
        this.q2Sessions = new SimpleIntegerProperty(q2Sessions);
        this.q2Hours = new SimpleDoubleProperty(q2Hours);
        this.totalSessions = new SimpleIntegerProperty(q1Sessions + q2Sessions);
        this.totalHours = new SimpleDoubleProperty(q1Hours + q2Hours);
    }

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