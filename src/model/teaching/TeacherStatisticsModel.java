package src.model.teaching;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class TeacherStatisticsModel {
    // Individual teacher statistics data
    private final SimpleIntegerProperty stt;
    private final SimpleStringProperty teacherName;
    // Changed teacherId to SimpleStringProperty
    private final SimpleStringProperty teacherId;
    private final Map<LocalDate, DailyStatistics> dailyStats;
    private final SimpleIntegerProperty totalSessions;
    private final SimpleDoubleProperty totalHours;

    // Collection of teacher statistics (for monthly view)
    private ObservableList<TeacherMonthlyStatistic> teacherMonthlyStatisticsList;

    // Aggregated totals for all teachers
    private int aggregatedTotalSessions = 0;
    private double aggregatedTotalHours = 0.0;

    // Constructor for daily view
    // Changed teacherId parameter type to String
    public TeacherStatisticsModel(int stt, String teacherId, String teacherName) {
        this.stt = new SimpleIntegerProperty(stt);
        // Initializing SimpleStringProperty with the String teacherId
        this.teacherId = new SimpleStringProperty(teacherId);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.dailyStats = new HashMap<>();
        this.totalSessions = new SimpleIntegerProperty(0);
        this.totalHours = new SimpleDoubleProperty(0.0);
        this.teacherMonthlyStatisticsList = FXCollections.observableArrayList();
    }

    // Default constructor for monthly view (adjusted for String ID, though default often has no ID)
    public TeacherStatisticsModel() {
        this.stt = new SimpleIntegerProperty(0);
        // Initializing SimpleStringProperty with an empty string
        this.teacherId = new SimpleStringProperty("");
        this.teacherName = new SimpleStringProperty("");
        this.dailyStats = new HashMap<>();
        this.totalSessions = new SimpleIntegerProperty(0);
        this.totalHours = new SimpleDoubleProperty(0.0);
        this.teacherMonthlyStatisticsList = FXCollections.observableArrayList();
    }

    // Getters and setters for daily view
    public int getStt() {
        return stt.get();
    }

    public SimpleIntegerProperty sttProperty() {
        return stt;
    }

    // Changed getter return type to String
    public String getTeacherId() {
        return teacherId.get();
    }

    // Changed property return type to SimpleStringProperty
    public SimpleStringProperty teacherIdProperty() {
        return teacherId;
    }

    public String getTeacherName() {
        return teacherName.get();
    }

    public SimpleStringProperty teacherNameProperty() {
        return teacherName;
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

    // Daily statistics methods
    public void addDailyStatistic(LocalDate date, int sessions, double hours) {
        dailyStats.put(date, new DailyStatistics(sessions, hours));
        updateTotals();
    }

    public DailyStatistics getDailyStatistic(LocalDate date) {
        return dailyStats.getOrDefault(date, new DailyStatistics(0, 0.0));
    }

    private void updateTotals() {
        int sessionSum = 0;
        double hourSum = 0.0;

        for (DailyStatistics stats : dailyStats.values()) {
            sessionSum += stats.getSessions();
            hourSum += stats.getHours();
        }

        totalSessions.set(sessionSum);
        totalHours.set(hourSum);
    }

    // Monthly statistics methods
    public void setTeacherMonthlyStatisticsList(ObservableList<TeacherMonthlyStatistic> teacherStatisticsList) {
        this.teacherMonthlyStatisticsList = teacherStatisticsList;
        calculateAggregatedTotals();
    }

    private void calculateAggregatedTotals() {
        aggregatedTotalSessions = 0;
        aggregatedTotalHours = 0.0;

        for (TeacherMonthlyStatistic stat : teacherMonthlyStatisticsList) {
            aggregatedTotalSessions += stat.getSessions();
            aggregatedTotalHours += stat.getHours();
        }
    }

    public ObservableList<TeacherMonthlyStatistic> getTeacherMonthlyStatisticsList() {
        return teacherMonthlyStatisticsList;
    }

    public int getAggregatedTotalSessions() {
        return aggregatedTotalSessions;
    }

    public double getAggregatedTotalHours() {
        return aggregatedTotalHours;
    }

    // Nested classes

    // Nested class for daily statistics
    public static class DailyStatistics {
        private final SimpleIntegerProperty sessions;
        private final SimpleDoubleProperty hours;

        public DailyStatistics(int sessions, double hours) {
            this.sessions = new SimpleIntegerProperty(sessions);
            this.hours = new SimpleDoubleProperty(hours);
        }

        public int getSessions() {
            return sessions.get();
        }

        public SimpleIntegerProperty sessionsProperty() {
            return sessions;
        }

        public void setSessions(int sessions) {
            this.sessions.set(sessions);
        }

        public double getHours() {
            return hours.get();
        }

        public SimpleDoubleProperty hoursProperty() {
            return hours;
        }

        public void setHours(double hours) {
            this.hours.set(hours);
        }
    }

    // Data model class for teacher monthly statistics
    public static class TeacherMonthlyStatistic {
        private final SimpleStringProperty teacherName;
        private final SimpleIntegerProperty stt;
        private final SimpleIntegerProperty sessions;
        private final SimpleDoubleProperty hours;
        private final SimpleIntegerProperty totalSessions;
        private final SimpleDoubleProperty totalHours;

        public TeacherMonthlyStatistic(int stt, String teacherName, int sessions, double hours) {
            this.stt = new SimpleIntegerProperty(stt);
            this.teacherName = new SimpleStringProperty(teacherName);
            this.sessions = new SimpleIntegerProperty(sessions);
            this.hours = new SimpleDoubleProperty(hours);
            this.totalSessions = new SimpleIntegerProperty(sessions); // Same as sessions initially
            this.totalHours = new SimpleDoubleProperty(hours);       // Same as hours initially
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

        public int getSessions() {
            return sessions.get();
        }

        public SimpleIntegerProperty sessionsProperty() {
            return sessions;
        }

        public double getHours() {
            return hours.get();
        }

        public SimpleDoubleProperty hoursProperty() {
            return hours;
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

        public void setStt(int i) {
            stt.set(i);
        }
    }
}
