package src.controller.Reports;

import src.dao.Report.TeacherMonthlyStatisticsDAO;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics;
import src.utils.DaoManager;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

public class MonthlyTeachingStatisticsController {

    private final MonthlyTeachingStatisticsModel model;
    private final TeacherMonthlyStatisticsDAO dao;

    public MonthlyTeachingStatisticsController() {
        this.model = new MonthlyTeachingStatisticsModel();
        // Get the DAO from DaoManager instead of creating a new instance
        this.dao = DaoManager.getInstance().getTeacherMonthlyStatisticsDAO();
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
        // Calculate LocalDate range from Month and Year inputs
        LocalDate fromDate = LocalDate.of(fromYear, fromMonth, 1);
        LocalDate toDate = YearMonth.of(toYear, toMonth).atEndOfMonth();

        // Call DAO with the calculated LocalDate range and status
        ObservableList<TeacherMonthlyStatistics> statistics =
                dao.getTeachingStatistics(fromDate, toDate, status);

        model.setTeacherStatisticsList(statistics);
        return true;
    }

    /**
     * Loads initial data (current month statistics).
     */
    public void loadInitialData() {
        YearMonth currentYearMonth = YearMonth.now();
        Month currentMonth = currentYearMonth.getMonth();
        int currentYear = currentYearMonth.getYear();

        // Calculate the start and end dates for the current month
        LocalDate fromDate = LocalDate.of(currentYear, currentMonth, 1);
        LocalDate toDate = currentYearMonth.atEndOfMonth();

        // Call DAO with the current month's date range and "Tất cả" status
        ObservableList<TeacherMonthlyStatistics> statistics =
                dao.getTeachingStatistics(fromDate, toDate, "Tất cả");

        model.setTeacherStatisticsList(statistics);
    }
}
