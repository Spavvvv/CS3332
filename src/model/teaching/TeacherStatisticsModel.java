package src.model.teaching;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import src.model.teaching.daily.DailyStatistics;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class TeacherStatisticsModel {
    // Individual teacher statistics data
    private final SimpleIntegerProperty stt;
    private final SimpleStringProperty teacherName;
    private final SimpleStringProperty teacherId;
    private final Map<LocalDate, DailyStatistics> dailyStats;
    private final SimpleIntegerProperty totalSessions; // Tổng số buổi dạy từ dailyStats
    private final SimpleDoubleProperty totalHours;    // Tổng số giờ dạy từ dailyStats

    // Constructor for daily src.view
    public TeacherStatisticsModel(int stt, String teacherId, String teacherName) {
        this.stt = new SimpleIntegerProperty(stt);
        this.teacherId = new SimpleStringProperty(teacherId);
        this.teacherName = new SimpleStringProperty(teacherName);
        this.dailyStats = new HashMap<>();
        this.totalSessions = new SimpleIntegerProperty(0);
        this.totalHours = new SimpleDoubleProperty(0.0);
    }

    // Getters and setters for daily src.view
    public int getStt() {
        return stt.get();
    }

    public double getTotalHours() {
        return totalHours.get();
    }

    // Daily statistics methods
    public void addDailyStatistic(LocalDate date, int sessions, double hours) {
        dailyStats.put(date, new DailyStatistics(sessions, hours));
        updateTotals(); // Cập nhật totalSessions và totalHours mỗi khi thêm dữ liệu ngày
    }

    public DailyStatistics getDailyStatistic(LocalDate date) {
        return dailyStats.getOrDefault(date, new DailyStatistics(0, 0.0));
    }

    /**
     * Cập nhật tổng số buổi và tổng số giờ dựa trên dailyStats.
     * Phương thức này đảm bảo totalSessions và totalHours luôn chứa tổng chính xác
     * của các buổi và giờ trong dailyStats.
     */
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

    // Thêm vào class TeacherStatisticsModel
    public SimpleStringProperty teacherNameProperty() {
        return teacherName;
    }

    public SimpleIntegerProperty totalSessionsProperty() {
        return totalSessions;
    }

    public String getTeacherName() {
        return teacherName.get();
    }

}