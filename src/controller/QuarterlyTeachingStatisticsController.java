package src.controller;

import src.dao.TeacherQuarterlyStatisticsDAO;
import javafx.collections.FXCollections; // Added FXCollections import
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import view.components.QuarterlyTeachingStatisticsView;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
// Removed Preferences import as state is handled by view/NavigationController

public class QuarterlyTeachingStatisticsController {
    private final QuarterlyTeachingStatisticsView view;
    private final TeacherQuarterlyStatisticsDAO dao;

    // Constants
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    // Removed quarters list as view no longer uses range selectors

    // Removed Preferences field

    public QuarterlyTeachingStatisticsController(QuarterlyTeachingStatisticsView view) {
        this.view = view;
        this.dao = new TeacherQuarterlyStatisticsDAO();
        // Initial setup of view elements that don't depend on data or events
        setupViewInitialState();
        // Event handlers and data loading are now initiated by the view's onActivate
    }

    private void setupViewInitialState() {
        // Setup static view elements that the controller provides
        view.setStatusOptions(statusOptions);
        // Note: Initial year selection and toggle state are handled by the view's onActivate
    }

    /**
     * Called by the view's onActivate method to set up event handlers.
     */
    public void setupEventHandlers() {
        // Setup event handlers for buttons and toggles
        view.setSearchButtonHandler(e -> handleSearch());
        view.setExportExcelButtonHandler(e -> handleExportExcel());
        view.setExportPdfButtonHandler(e -> handleExportPdf());
        view.setPrintButtonHandler(e -> handlePrint());

        // Toggle handlers - navigation and state saving is handled by the view
        // The controller does NOT handle toggle selection logic here, only search triggering
        view.setDayToggleHandler(e -> { if (view.isDayToggleSelected()) view.handlePeriodChange(view.dayToggle); });
        view.setMonthToggleHandler(e -> { if (view.isMonthToggleSelected()) view.handlePeriodChange(view.monthToggle); });
        view.setQuarterToggleHandler(e -> { /* Stay on this view */ }); // Quarter toggle stays here
        view.setYearToggleHandler(e -> { if (view.isYearToggleSelected()) view.handlePeriodChange(view.yearToggle); });

        // Add listener to the year ComboBox to trigger search when year changes
        if (view.yearComboBox != null) { // Added null check
            view.yearComboBox.setOnAction(event -> handleSearch());
        }

        // Add listener to the status ComboBox to trigger search when status changes
        if (view.statusComboBox != null) { // Added null check
            view.statusComboBox.setOnAction(event -> handleSearch());
        }
    }


    /**
     * Called by the view's onActivate method to load initial data.
     */
    public void loadInitialData() {
        // Get the initial state from the view
        int initialYear = view.getSelectedYear(); // View defaults to current year
        String initialStatus = view.getSelectedStatus(); // View defaults to first status option

        // Load data for the initial state
        loadStatisticsData(initialYear, initialStatus);
    }

    /**
     * Handles the search action triggered by the Search button or filter changes.
     */
    private void handleSearch() {
        // Get the current filter criteria from the view
        int selectedYear = view.getSelectedYear();
        String selectedStatus = view.getSelectedStatus();

        // Validate input (basic check)
        if (selectedYear <= 0 || selectedStatus == null || selectedStatus.trim().isEmpty()) {
            view.showAlert("Lỗi nhập liệu", "Vui lòng chọn năm và trạng thái.", Alert.AlertType.WARNING);
            return;
        }

        // Load data based on the selected criteria
        loadStatisticsData(selectedYear, selectedStatus);
    }

    /**
     * Fetches data from the DAO and updates the view.
     *
     * @param year the year to retrieve statistics for
     * @param status the approval status to filter by ("Tất cả" for all statuses)
     * @return true if data loading was successful, false otherwise
     */
    public boolean searchStatistics(int year, int currentQuarterPlaceholder, String status) {
        // The currentQuarterPlaceholder is ignored here as the DAO gets all quarters
        // The view will use its own logic to display the relevant quarter
        return loadStatisticsData(year, status);
    }


    /**
     * Core method to fetch data from DAO and update the view.
     * @param year The year to fetch statistics for.
     * @param status The approval status filter.
     * @return true if data was loaded successfully, false otherwise.
     */
    private boolean loadStatisticsData(int year, String status) {
        ObservableList<TeacherQuarterlyStatisticsModel> dataFromDao = null; // Use a new variable
        boolean success = false; // Track success outside the try block
        try {
            dataFromDao = dao.getTeacherStatistics(year, status);
            success = dataFromDao != null; // Set success based on DAO result

            // Update the view on the JavaFX application thread
            final ObservableList<TeacherQuarterlyStatisticsModel> finalData = dataFromDao; // Make it effectively final for the lambda
            Platform.runLater(() -> {
                if (finalData != null) {
                    view.updateTableData(finalData); // updateTableData calls updateSummaryRow internally
                    // The view now dynamically updates headers and summary based on current data and selected year/quarter
                } else {
                    // Clear the table and summary if data is null (e.g., on error)
                    view.updateTableData(FXCollections.observableArrayList());
                    view.updateSummaryRow(); // Update summary with empty data
                }
            });

        } catch (Exception e) {
            // Log the error (replace with proper logging)
            e.printStackTrace();
            System.err.println("Error loading statistics data: " + e.getMessage());

            // Show error message on the JavaFX application thread
            Platform.runLater(() -> {
                view.showAlert("Lỗi", "Đã xảy ra lỗi khi tải dữ liệu thống kê.", Alert.AlertType.ERROR);
                // Clear the table and summary on error
                view.updateTableData(FXCollections.observableArrayList());
                view.updateSummaryRow(); // Update summary with empty data
            });
            success = false; // Indicate failure
        }
        return success; // Return the success status
    }


    private void handleExportExcel() {
        view.showSuccess("Đang xuất file Excel...");
        // TODO: Implement Excel export logic
        // Get data from DAO or directly from view.statisticsTable.getItems()
        // TeacherQuarterlyStatisticsDAO exportDao = new TeacherQuarterlyStatisticsDAO();
        // ObservableList<TeacherQuarterlyStatisticsModel> dataToExport = exportDao.getTeacherStatistics(view.getSelectedYear(), view.getSelectedStatus());
        // Call an export service/utility
    }

    private void handleExportPdf() {
        view.showSuccess("Đang xuất file PDF...");
        // TODO: Implement PDF export logic
        // Get data similar to Excel export
        // Call an export service/utility
    }

    private void handlePrint() {
        view.showSuccess("Đang chuẩn bị in...");
        // TODO: Implement Print logic
        // Get data similar to Excel export
        // Call a printing service/utility
    }

    // Removed saveToggleState and loadSavedToggleState as they are handled by the view/NavigationController
    // Removed navigateToView as it's handled by the view's handlePeriodChange method
}
