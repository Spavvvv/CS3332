package src.controller;

import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import view.components.YearlyTeachingStatisticsView;
import src.dao.TeacherStatisticsDAO;

public class YearlyStatisticsController {
    private YearlyTeachingStatisticsView view;
    private TeacherStatisticsDAO statisticsDAO;

    // Current filter values
    private int currentFromYear;
    private int currentToYear;
    private String currentStatus;

    public YearlyStatisticsController(YearlyTeachingStatisticsView view) {
        this.view = view;
        this.statisticsDAO = new TeacherStatisticsDAO();

        // Default filter values
        this.currentFromYear = 2025;
        this.currentToYear = 2025;
        this.currentStatus = "Tất cả";
    }

    /**
     * Load statistics data based on current filter settings
     */
    public void loadData() {
        // Get data from DAO
        ObservableList<TeacherYearlyStatisticsModel> data =
                statisticsDAO.getYearlyStatistics(currentFromYear, currentToYear, currentStatus);

        // Get summary data
        StatisticsSummaryModel summary =
                statisticsDAO.calculateSummaryStatistics(currentFromYear, currentToYear, currentStatus);

        // Update the view
        view.setTableData(data);
        view.updateSummary(summary);

        // Update year label in view header
        view.updateYearLabel(currentToYear);
    }

    /**
     * Handle search action with filter parameters
     */
    public void handleSearch(int fromYear, int toYear, String status) {
        // Update current filter values
        this.currentFromYear = fromYear;
        this.currentToYear = toYear;
        this.currentStatus = status;

        // Reload data with new filters
        loadData();
    }

    /**
     * Handle Excel export action
     */
    public void handleExportExcel() {
        // In a real app, this would use a service to generate Excel with data from DAO
        view.showExportingMessage("Excel");
    }

    /**
     * Handle PDF export action
     */
    public void handleExportPdf() {
        // In a real app, this would use a service to generate PDF with data from DAO
        view.showExportingMessage("PDF");
    }

    /**
     * Handle print action
     */
    public void handlePrint() {
        // In a real app, this would use a print service with data from DAO
        view.showPrintingMessage();
    }

    /**
     * Handle period type change (day/month/quarter/year)
     */
    public void handlePeriodChange(ToggleButton selected) {
        if (selected.getText().equals("Ngày")) {
            // Switch to daily view
            view.navigateToView("teaching-statistics");
        } else if (selected.getText().equals("Tháng")) {
            // Switch to monthly view
            view.navigateToView("monthly-teaching");
        } else if (selected.getText().equals("Quý")) {
            // Switch to quarterly view
            view.navigateToView("quarterly-teaching");
        }
    }

    /**
     * Clean up resources when controller is no longer needed
     */
    public void cleanup() {
        if (statisticsDAO != null) {
            statisticsDAO.closeConnection();
        }
    }
}