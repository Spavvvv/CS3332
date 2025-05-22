package src.controller.Dashboard;

// Remove direct import of DashboardDAO if it is only accessed via DaoManager
// import src.dao.Dashboard.DashboardDAO;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import src.model.ClassSession;
import src.model.dashboard.DashboardModel;
import src.model.system.schedule.ScheduleItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
// import java.util.UUID; // Removed unnecessary import

// Import the DaoManager
import src.utils.DaoManager;
// Import the specific DAO class if you need its type for the instance variable
import src.dao.Dashboard.DashboardDAO;


/**
 * Controller for the Dashboard - handles all data operations and business logic application
 */
public class DashboardController {
    private DashboardModel model;
    // Keep the type declaration, but get the instance from DaoManager
    private DashboardDAO dashboardDAO;

    public DashboardController() {
        model = new DashboardModel();
        // Obtain the DAO instance from the DaoManager singleton
        this.dashboardDAO = DaoManager.getInstance().getDashboardDAO();
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
            // Handle error appropriately, perhaps update the model or src.view with an error state
        }
    }

    /**
     * Fetches statistics data and updates the model
     */
    private void fetchStatistics() throws SQLException {
        // Use the dashboardDAO obtained from DaoManager
        int totalStudents = dashboardDAO.getTotalStudents();
        int totalClasses = dashboardDAO.getTotalClasses();
        // The original code calculated attendance rate based on model's todayClasses,
        // then got it from DAO if model was empty. Sticking closer to original logic
        // but ensuring DAO call if needed.
        double attendanceRate;
        if (!model.getTodayClasses().isEmpty()) {
            attendanceRate = model.calculateAttendanceRate(); // Assuming this method exists and works
        } else {
            attendanceRate = dashboardDAO.getAttendanceRate();
        }


        // Update model
        model.setTotalStudents(totalStudents);
        model.setTotalClasses(totalClasses);
        model.setAttendanceRate(attendanceRate);
    }

    /**
     * Fetches course distribution data and updates the model
     */
    private void fetchCourseDistribution() throws SQLException {
        // The original code also had logic to generate from model if todayClasses was not empty.
        // Reverting to the simpler DAO call as per the second part of the original logic
        // and assuming the DAO provides the primary data source for this.
        ObservableList<PieChart.Data> pieChartData = dashboardDAO.getCourseDistribution();
        model.setCourseDistribution(pieChartData);
    }

    /**
     * Fetches upcoming schedules and updates the model
     */
    private void fetchUpcomingSchedules() throws SQLException {
        List<ScheduleItem> scheduleItems = new ArrayList<>();

        try {
            // Get raw data from DAO
            // Use the dashboardDAO obtained from DaoManager
            List<Object[]> scheduleData = dashboardDAO.getUpcomingSchedulesData(5);

            // Convert raw data to business objects - Keeping original conversion logic
            for (Object[] row : scheduleData) {
                String id = (String) row[0];
                String title = (String) row[1];
                String description = (String) row[2];

                // Get timestamps directly and convert to LocalDateTime
                java.sql.Timestamp startTimestamp = (java.sql.Timestamp) row[3];
                java.sql.Timestamp endTimestamp = (java.sql.Timestamp) row[4];

                LocalDateTime startDateTime = startTimestamp.toLocalDateTime();
                LocalDateTime endDateTime = endTimestamp.toLocalDateTime();

                // Create business object with the schedule type (schedule type was not used in original item creation)
                // String scheduleType = (String) row[5]; // Original code fetched but didn't use this field for item creation
                ScheduleItem item = new ScheduleItem(id, title, description, startDateTime, endDateTime);
                scheduleItems.add(item);
            }
        } catch (Exception e) { // Catch any other unexpected exceptions during processing
            System.err.println("Unexpected error loading upcoming schedules: " + e.getMessage());
            e.printStackTrace();
            // Handle as needed, maybe wrap in a custom exception or rethrow RuntimeException
        }

        // Update model with processed data
        model.setScheduleItems(scheduleItems);
    }


    /**
     * Fetches today's classes and updates the model
     */
    private void fetchTodayClasses() throws SQLException {
        // Get data from DAO
        // Use the dashboardDAO obtained from DaoManager
        List<ClassSession> classSessions = dashboardDAO.getTodayClasses();

        // Update model
        model.setTodayClasses(classSessions);
    }

    /**
     * Adds a new schedule item
     * Controller handles all interactions with data sources
     *
     * @param title The title of the schedule item.
     * @param description The description of the schedule item.
     * @param date The date of the schedule item.
     * @param time The time of the schedule item.
     * @return true if the schedule item was successfully added, false otherwise.
     */
    public boolean addScheduleItem(String title, String description, LocalDate date, LocalTime time) {
        try {
            // Use model to create the business object as in the original code
            // Reverting to original logic here, assuming model handles the creation
            // and potentially temporary ID if needed before DAO save.
            ScheduleItem newItem = model.createScheduleItem(title, description, date, time); // Reverted to original call

            // Save to database through DAO
            // Use the dashboardDAO obtained from DaoManager
            boolean success = dashboardDAO.addDashboardEvent(newItem);

            if (success) {
                // Reload data to get the new ID and updated list
                refreshDashboard();
            }

            return success;
        } catch (Exception e) { // Catch any other unexpected exceptions
            System.err.println("Unexpected error adding schedule item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Finds a class session by ID within the loaded today's classes in the model.
     *
     * @param classId The ID of the class session to find.
     * @return The ClassSession object, or null if not found.
     */
    public ClassSession findClassSessionById(String classId) {
        // This method seems to search within the model's data, not the DAO.
        // Keeping original logic that searches the model.
        if (model != null && model.getTodayClasses() != null) {
            for (ClassSession session : model.getTodayClasses()) {
                if (session.getId() != null && session.getId().equals(classId)) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * Get the dashboard model
     * @return The current DashboardModel instance
     */
    public DashboardModel getModel() {
        return model;
    }
}
