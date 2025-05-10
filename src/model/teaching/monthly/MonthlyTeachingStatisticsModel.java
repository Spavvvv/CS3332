package src.model.teaching.monthly;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public class MonthlyTeachingStatisticsModel {

    // Data structures
    private ObservableList<TeacherMonthlyStatistics> teacherStatisticsList;
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");

    // Totals for footer
    private int totalSessions = 0;
    private double totalHours = 0.0;

    public MonthlyTeachingStatisticsModel() {
        teacherStatisticsList = FXCollections.observableArrayList();
    }

    public ObservableList<TeacherMonthlyStatistics> getTeacherStatisticsList() {
        return teacherStatisticsList;
    }

    public void setTeacherStatisticsList(ObservableList<TeacherMonthlyStatistics> teacherStatisticsList) {
        this.teacherStatisticsList = teacherStatisticsList;
        calculateTotals();
    }

    private void calculateTotals() {
        totalSessions = 0;
        totalHours = 0.0;

        for (TeacherMonthlyStatistics stats : teacherStatisticsList) {
            totalSessions += stats.getSessions();
            totalHours += stats.getHours();
        }
    }

    public List<String> getStatusOptions() {
        return statusOptions;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public double getTotalHours() {
        return totalHours;
    }

    // Data model for teacher monthly statistics
    public static class TeacherMonthlyStatistics {
        // Added teacherId property as SimpleStringProperty
        private final SimpleStringProperty teacherId;
        private final SimpleStringProperty teacherName;
        private final SimpleIntegerProperty stt;
        private final SimpleIntegerProperty sessions;
        private final SimpleDoubleProperty hours;
        private final SimpleIntegerProperty totalSessions;
        private final SimpleDoubleProperty totalHours;

        // Updated constructor to include String teacherId parameter
        public TeacherMonthlyStatistics(int stt, String teacherId, String teacherName, int sessions, double hours) {
            this.stt = new SimpleIntegerProperty(stt);
            // Initialized the teacherId SimpleStringProperty
            this.teacherId = new SimpleStringProperty(teacherId);
            this.teacherName = new SimpleStringProperty(teacherName);
            this.sessions = new SimpleIntegerProperty(sessions);
            this.hours = new SimpleDoubleProperty(hours);
            this.totalSessions = new SimpleIntegerProperty(sessions); // Same as sessions for the example
            this.totalHours = new SimpleDoubleProperty(hours);       // Same as hours for the example
        }

        // Added getter and property method for teacherId
        public String getTeacherId() {
            return teacherId.get();
        }

        public SimpleStringProperty teacherIdProperty() {
            return teacherId;
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
    }
}
