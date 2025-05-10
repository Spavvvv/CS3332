package src.controller;

import src.dao.TeacherMonthlyStatisticsDAO;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics;

import java.sql.SQLException;
import java.time.Month;

public class MonthlyTeachingStatisticsController {

    private final MonthlyTeachingStatisticsModel model;
    private final TeacherMonthlyStatisticsDAO dao;

    public MonthlyTeachingStatisticsController() {
        this.model = new MonthlyTeachingStatisticsModel();
        this.dao = new TeacherMonthlyStatisticsDAO();
    }

    public MonthlyTeachingStatisticsModel getModel() {
        return model;
    }

    /**
     * Searches for teaching statistics based on criteria
     * @return true if search was successful, false if there was an error
     */
    public boolean searchStatistics(Month fromMonth, int fromYear, Month toMonth, int toYear, String status) {
        try {
            ObservableList<TeacherMonthlyStatistics> statistics =
                    dao.getTeachingStatistics(fromMonth, fromYear, toMonth, toYear, status);
            model.setTeacherStatisticsList(statistics);
            return true;
        } catch (SQLException e) {
            System.err.println("Error searching statistics: " + e.getMessage());
            // Load fallback data in case of database error
            model.setTeacherStatisticsList(dao.getFallbackData());
            return false;
        }
    }

    /**
     * Loads initial data (current month statistics)
     */
    public void loadInitialData() {
        Month currentMonth = Month.APRIL; // For example purposes
        int currentYear = 2025;

        try {
            ObservableList<TeacherMonthlyStatistics> statistics =
                    dao.getTeachingStatistics(currentMonth, currentYear, currentMonth, currentYear, "Tất cả");
            model.setTeacherStatisticsList(statistics);
        } catch (SQLException e) {
            System.err.println("Error loading initial data: " + e.getMessage());
            // Load fallback data in case of database error
            model.setTeacherStatisticsList(dao.getFallbackData());
        }
    }
}