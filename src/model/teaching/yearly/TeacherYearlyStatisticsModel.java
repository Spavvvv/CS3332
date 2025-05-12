package src.model.teaching.yearly;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TeacherYearlyStatisticsModel {
    private final SimpleIntegerProperty stt;
    private final SimpleStringProperty teacherName;
    private final SimpleIntegerProperty yearSessions;
    private final SimpleDoubleProperty yearHours;
    private final SimpleIntegerProperty totalSessions;
    private final SimpleDoubleProperty totalHours;

    public TeacherYearlyStatisticsModel(int stt, String teacherName,
                                        int yearSessions, double yearHours,
                                        int totalSessions, double totalHours) {
        this.stt = new SimpleIntegerProperty(stt);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.yearSessions = new SimpleIntegerProperty(yearSessions);
        this.yearHours = new SimpleDoubleProperty(yearHours);
        this.totalSessions = new SimpleIntegerProperty(totalSessions);
        this.totalHours = new SimpleDoubleProperty(totalHours);
    }

    // Getters and property methods
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

    public int getYearSessions() {
        return yearSessions.get();
    }

    public SimpleIntegerProperty yearSessionsProperty() {
        return yearSessions;
    }

    public double getYearHours() {
        return yearHours.get();
    }

    public SimpleDoubleProperty yearHoursProperty() {
        return yearHours;
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