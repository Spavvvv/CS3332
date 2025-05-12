package src.controller;

import javafx.collections.FXCollections;
import src.dao.TeacherMonthlyStatisticsDAO;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

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
     * Searches for teaching statistics based on criteria (month range and status).
     * Converts Month and Year inputs to LocalDate range for the DAO.
     * @param fromMonth The starting month for the search.
     * @param fromYear The starting year for the search.
     * @param toMonth The ending month for the search.
     * @param toYear The ending year for the search.
     * @param status The status to filter by ("Tất cả", "Đã duyệt", etc.).
     * @return true if search was successful, false if there was an error.
     */
    public boolean searchStatistics(Month fromMonth, int fromYear, Month toMonth, int toYear, String status) {
        try {
            // Calculate LocalDate range from Month and Year inputs
            LocalDate fromDate = LocalDate.of(fromYear, fromMonth, 1);
            LocalDate toDate = YearMonth.of(toYear, toMonth).atEndOfMonth();

            // Call DAO with the calculated LocalDate range and status
            ObservableList<TeacherMonthlyStatistics> statistics =
                    dao.getTeachingStatistics(fromDate, toDate, status);

            model.setTeacherStatisticsList(statistics);
            return true;
        } catch (SQLException e) {
            System.err.println("Error searching statistics: " + e.getMessage());
            e.printStackTrace();
            // Do not load fallback data as it was removed from DAO.
            // You might want to clear the list or show an error message in the UI.
            model.setTeacherStatisticsList(FXCollections.observableArrayList()); // Clear the list on error
            // Optionally, you could add logic here to update a status property in the model
            // to indicate that an error occurred, which the UI could then display.
            return false;
        }
    }

    /**
     * Loads initial data (current month statistics).
     */
    public void loadInitialData() {
        YearMonth currentYearMonth = YearMonth.now();
        Month currentMonth = currentYearMonth.getMonth();
        int currentYear = currentYearMonth.getYear();

        try {
            // Calculate the start and end dates for the current month
            LocalDate fromDate = LocalDate.of(currentYear, currentMonth, 1);
            LocalDate toDate = currentYearMonth.atEndOfMonth();

            // Call DAO with the current month's date range and "Tất cả" status
            ObservableList<TeacherMonthlyStatistics> statistics =
                    dao.getTeachingStatistics(fromDate, toDate, "Tất cả");

            model.setTeacherStatisticsList(statistics);
        } catch (SQLException e) {
            System.err.println("Error loading initial data: " + e.getMessage());
            e.printStackTrace();
            // Do not load fallback data as it was removed from DAO.
            // Clear the list on error.
            model.setTeacherStatisticsList(FXCollections.observableArrayList()); // Clear the list on error
            // Optionally, indicate error in model status.
        }
    }
}
