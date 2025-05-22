package src.controller.Reports;

import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import src.view.Report.YearlyTeachingStatisticsView;
import src.dao.Report.TeacherYearlyStatisticsDAO;
import src.utils.DaoManager; // Import DaoManager

public class YearlyStatisticsController {
    private YearlyTeachingStatisticsView view;
    private TeacherYearlyStatisticsDAO statisticsDAO;

    // Current filter values - using a single year now
    private int currentYear;
    private String currentStatus;

    public YearlyStatisticsController(YearlyTeachingStatisticsView view) {
        this.view = view;
        // Get TeacherYearlyStatisticsDAO instance from DaoManager
        this.statisticsDAO = DaoManager.getInstance().getTeacherYearlyStatisticsDAO();

        // Default filter values
        // Assuming a default year is needed, e.g., the current year
        this.currentYear = 2025; // Set a default year, adjust as needed
        this.currentStatus = "Tất cả";
    }

    /**
     * Load statistics data based on current filter settings
     */
    public void loadData() {
        // Get data from DAO using only the current year and status
        ObservableList<TeacherYearlyStatisticsModel> data =
                statisticsDAO.getYearlyStatistics(currentYear, currentStatus);

        // Get summary data using only the current year and status
        StatisticsSummaryModel summary =
                statisticsDAO.calculateSummaryStatistics(currentYear, currentStatus);

        // Update the src.view
        view.setTableData(data);
        view.updateSummary(summary);

        // Update year label in src.view header
        view.updateYearLabel(currentYear);
    }

    /**
     * Handle search action with filter parameters
     * Modified to accept a single year and status
     */
    public void handleSearch(int year, String status) {
        // Update current filter values
        this.currentYear = year;
        this.currentStatus = status;

        // Reload data with new filters
        loadData();
    }

    /**
     * Handle Excel export action
     */
    public void handleExportExcel() {
        // In a real app, this would use a service to generate Excel with data from DAO
        // You would likely pass currentYear and currentStatus to the export service
        view.showExportingMessage("Excel");
        // Example: exportService.exportYearlyStatistics(currentYear, currentStatus);
    }

    /**
     * Handle PDF export action
     */
    public void handleExportPdf() {
        // In a real app, this would use a service to generate PDF with data from DAO
        // You would likely pass currentYear and currentStatus to the export service
        view.showExportingMessage("PDF");
        // Example: pdfService.exportYearlyStatistics(currentYear, currentStatus);
    }

    /**
     * Handle period type change (day/month/quarter/year)
     * Note: This method logic depends on how your overall navigation/src.view switching
     * is implemented in the application. The navigateToView calls are placeholders.
     */
    public void handlePeriodChange(ToggleButton selected) {
        if (selected == null) {
            // Handle case where no toggle button is selected, if necessary
            return;
        }
        String periodText = selected.getText();

        if ("Ngày".equals(periodText)) {
            // Switch to daily src.view
            // Assuming "teaching-statistics" is the identifier for the daily src.view
            view.navigateToView("teaching-statistics");
        } else if ("Tháng".equals(periodText)) {
            // Switch to monthly src.view
            // Assuming "monthly-teaching" is the identifier for the monthly src.view
            view.navigateToView("monthly-teaching");
        } else if ("Quý".equals(periodText)) {
            // Switch to quarterly src.view
            // Assuming "quarterly-teaching" is the identifier for the quarterly src.view
            view.navigateToView("quarterly-teaching");
        }
        // The "Năm" (Year) case is implicitly handled by staying on this controller's src.view
        // or could explicitly trigger loadData() if the Year toggle button is clicked
        // while already on the Yearly src.view.
    }

    /**
     * Clean up resources when controller is no longer needed
     */
    public void cleanup() {
        // The DAO connection should be managed by the DaoManager,
        // so explicit closing here might not be necessary depending on DaoManager's implementation.
        // If DaoManager has a global cleanup method, call that instead.
        // if (statisticsDAO != null) {
        //     statisticsDAO.closeConnection(); // Remove if DaoManager handles lifecycle
        // }
    }
}
