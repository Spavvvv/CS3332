package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import src.dao.DashboardDAO;
import src.model.ClassSession;
import src.model.dashboard.DashboardModel;
import src.model.system.schedule.ScheduleItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Controller for the Dashboard - handles all data operations and business logic application
 */
public class DashboardController {
    private DashboardModel model;
    private DashboardDAO dashboardDAO;

    public DashboardController() {
        model = new DashboardModel();
        dashboardDAO = new DashboardDAO();
    }

    /**
     * Loads all data for the dashboard from data sources
     */
    public void refreshDashboard() {
        try {
            fetchStatistics();
            fetchTodayClasses();
            fetchCourseDistribution();
            fetchUpcomingSchedules();
        } catch (SQLException e) {
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
            // Handle error appropriately
        }
    }

    /**
     * Fetches statistics data and updates the model
     */
    private void fetchStatistics() throws SQLException {
        // Retrieve data from DAO
        int totalStudents = dashboardDAO.getTotalStudents();
        int totalClasses = dashboardDAO.getTotalClasses();

        // Update model
        model.setTotalStudents(totalStudents);
        model.setTotalClasses(totalClasses);

        // Calculate attendance rate if we have class data
        if (!model.getTodayClasses().isEmpty()) {
            double calculatedRate = model.calculateAttendanceRate();
            model.setAttendanceRate(calculatedRate);
        } else {
            // If no class data yet, get from DAO
            double attendanceRate = dashboardDAO.getAttendanceRate();
            model.setAttendanceRate(attendanceRate);
        }
    }

    /**
     * Fetches course distribution data and updates the model
     */
    private void fetchCourseDistribution() throws SQLException {
        // If we have class data, generate distribution from the model
        if (!model.getTodayClasses().isEmpty()) {
            ObservableList<PieChart.Data> distribution = model.generateCourseDistribution();
            model.setCourseDistribution(distribution);
        } else {
            // Otherwise get from DAO
            ObservableList<PieChart.Data> pieChartData = dashboardDAO.getCourseDistribution();
            model.setCourseDistribution(pieChartData);
        }
    }

    /**
     * Fetches upcoming schedules and updates the model
     */
    private void fetchUpcomingSchedules() throws SQLException {
        List<ScheduleItem> scheduleItems = new ArrayList<>();

        try {
            // Get raw data from DAO
            List<Object[]> scheduleData = dashboardDAO.getUpcomingSchedulesData(5);

            // Convert raw data to business objects
            for (Object[] row : scheduleData) {
                String id = (String) row[0];
                String title = (String) row[1];
                String description = (String) row[2];

                // Get timestamps directly and convert to LocalDateTime
                java.sql.Timestamp startTimestamp = (java.sql.Timestamp) row[3];
                java.sql.Timestamp endTimestamp = (java.sql.Timestamp) row[4];

                LocalDateTime startDateTime = startTimestamp.toLocalDateTime();
                LocalDateTime endDateTime = endTimestamp.toLocalDateTime();

                // Create business object with the schedule type
                String scheduleType = (String) row[5];
                ScheduleItem item = new ScheduleItem(id, title, description, startDateTime, endDateTime);
                scheduleItems.add(item);
            }
        } catch (Exception e) {
            System.err.println("Error loading upcoming schedules: " + e.getMessage());
            e.printStackTrace();
        }

        // Update model with processed data
        model.setScheduleItems(scheduleItems);
    }


    /**
     * Fetches today's classes and updates the model
     */
    private void fetchTodayClasses() throws SQLException {
        // Get raw data from DAO
        List<ClassSession> classSessions = dashboardDAO.getTodayClasses();

        // Update model
        model.setTodayClasses(classSessions);
    }

    /**
     * Adds a new schedule item
     * Controller handles all interactions with data sources
     */
    public boolean addScheduleItem(String title, String description, LocalDate date, LocalTime time) {
        try {
            // Use model to create the business object
            ScheduleItem newItem = model.createScheduleItem(title, description, date, time);

            // Save to database through DAO
            boolean success = dashboardDAO.addDashboardEvent(newItem);

            if (success) {
                // Reload data to get the new ID
                fetchUpcomingSchedules();
            }

            return success;
        } catch (SQLException e) {
            System.err.println("Error adding schedule item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Finds a class session by ID
     * Controller method that uses model's business logic
     */
    public ClassSession findClassSessionById(long classId) {
        return model.findClassSessionById(classId);
    }

    /**
     * Get the dashboard model
     * @return The current DashboardModel instance
     */
    public DashboardModel getModel() {
        return model;
    }
}