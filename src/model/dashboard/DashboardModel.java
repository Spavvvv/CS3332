package src.model.dashboard;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import src.model.ClassSession;
import src.model.system.schedule.ScheduleItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for Dashboard that handles business entities and logic only
 */
public class DashboardModel {
    // Statistics data
    private int totalStudents;
    private int totalClasses;
    private double attendanceRate;

    // Course distribution data
    private ObservableList<PieChart.Data> courseDistribution;

    // Schedule data
    private List<ScheduleItem> scheduleItems;

    // Today's classes data
    private List<ClassSession> todayClasses;

    public DashboardModel() {
        // Initialize with default values
        totalStudents = 0;
        totalClasses = 0;
        attendanceRate = 0;

        courseDistribution = FXCollections.observableArrayList();
        scheduleItems = new ArrayList<>();
        todayClasses = new ArrayList<>();
    }

    // Getters and setters
    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public ObservableList<PieChart.Data> getCourseDistribution() {
        return courseDistribution;
    }

    public void setCourseDistribution(ObservableList<PieChart.Data> courseDistribution) {
        this.courseDistribution.clear();
        this.courseDistribution.addAll(courseDistribution);
    }

    public List<ScheduleItem> getScheduleItems() {
        return scheduleItems;
    }

    public void setScheduleItems(List<ScheduleItem> scheduleItems) {
        this.scheduleItems.clear();
        this.scheduleItems.addAll(scheduleItems);
    }

    public List<ClassSession> getTodayClasses() {
        return todayClasses;
    }

    public void setTodayClasses(List<ClassSession> todayClasses) {
        this.todayClasses.clear();
        this.todayClasses.addAll(todayClasses);
    }

    public void addScheduleItem(ScheduleItem item) {
        scheduleItems.add(item);
    }

    /**
     * Creates a new schedule item with the given parameters
     */
    public ScheduleItem createScheduleItem(String title, String description, LocalDate date, LocalTime time) {
        LocalDateTime startDateTime = LocalDateTime.of(date, time);
        LocalDateTime endDateTime = startDateTime.plusHours(1); // Default 1 hour duration

        return new ScheduleItem(null, title, description, startDateTime, endDateTime);
    }

    /**
     * Calculate attendance rate based on class sessions
     * Pure business logic
     */
    public double calculateAttendanceRate() {
        if (todayClasses.isEmpty()) {
            return 0.0;
        }

        int totalStudents = 0;
        int presentStudents = 0;

        for (ClassSession session : todayClasses) {
            if (session.getStudents() != null) {
                totalStudents += session.getStudents().size();
                // Count present students based on attendance status
                if(session.getStatus() == 1)
                presentStudents += session.getStatus();
            }
        }

        return totalStudents > 0 ? (double) presentStudents / totalStudents * 100 : 0.0;
    }

    /**
     * Generate distribution data based on today's classes
     * Pure business logic
     */
    public ObservableList<PieChart.Data> generateCourseDistribution() {
        ObservableList<PieChart.Data> distribution = FXCollections.observableArrayList();

        // Count classes by course type
        Map<String, Integer> courseCount = new HashMap<>();

        for (ClassSession session : todayClasses) {
            String courseType = null;
            // If course type is null, use course name instead
            if (courseType == null || courseType.isEmpty()) {
                courseType = session.getCourseName();
            }
            courseCount.put(courseType, courseCount.getOrDefault(courseType, 0) + 1);
        }

        // Add data to pie chart
        for (Map.Entry<String, Integer> entry : courseCount.entrySet()) {
            distribution.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        return distribution;
    }

    /**
     * Find a class session by ID
     * Business logic method - doesn't access data sources
     */
    public ClassSession findClassSessionById(String classId) {
        for (ClassSession session : todayClasses) {
            if (session.getId().equals(classId)) {
                return session;
            }
        }
        return null;
    }
}