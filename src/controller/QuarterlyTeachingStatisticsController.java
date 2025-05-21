package src.controller;

import src.dao.TeacherQuarterlyStatisticsDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import view.components.QuarterlyTeachingStatisticsView;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import utils.DaoManager;

import java.util.Arrays;
import java.util.List;

public class QuarterlyTeachingStatisticsController {
    private QuarterlyTeachingStatisticsView view;
    private TeacherQuarterlyStatisticsDAO dao;

    // Constants
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");

    // Default constructor for initialization by the view
    public QuarterlyTeachingStatisticsController() {
        this.dao = DaoManager.getInstance().getTeacherQuarterlyStatisticsDAO();
    }

    // Method for the view to call after initialization
    public void setView(QuarterlyTeachingStatisticsView view) {
        this.view = view;
        setupViewInitialState();
    }

    private void setupViewInitialState() {
        if (view != null) {
            view.setStatusOptions(statusOptions);
        }
    }

    /**
     * Setup event handlers - called by the view
     */
    public void setupEventHandlers() {
        // Only setup handlers if view is initialized
        if (view == null) return;

        // Setup event handlers for buttons
        view.setSearchButtonHandler(e -> view.handleSearch());
        view.setExportExcelButtonHandler(e -> handleExportExcel());
    }

    /**
     * Load initial data when view is activated
     */
    public void loadInitialData() {
        if (view == null) return;

        int initialYear = view.getSelectedYear();
        String initialStatus = view.getSelectedStatus();

        // If status is not selected yet, use the first option
        if (initialStatus == null && !statusOptions.isEmpty()) {
            initialStatus = statusOptions.get(0);
        }

        loadStatisticsData(initialYear, initialStatus);
    }

    /**
     * Fetches data from the DAO and updates the view.
     *
     * @param year the year to retrieve statistics for
     * @param currentQuarter the current quarter number (for display purposes)
     * @param status the approval status to filter by
     * @return true if data loading was successful, false otherwise
     */
    public boolean searchStatistics(int year, int currentQuarter, String status) {
        return loadStatisticsData(year, status);
    }

    /**
     * Core method to fetch data from DAO and update the view.
     * @param year The year to fetch statistics for.
     * @param status The approval status filter.
     * @return true if data was loaded successfully, false otherwise.
     */
    private boolean loadStatisticsData(int year, String status) {
        if (view == null) return false;

        boolean success = false;

        try {
            ObservableList<TeacherQuarterlyStatisticsModel> dataFromDao = dao.getTeacherStatistics(year, status);
            success = dataFromDao != null;

            final ObservableList<TeacherQuarterlyStatisticsModel> finalData =
                    (dataFromDao != null) ? dataFromDao : FXCollections.observableArrayList();

            Platform.runLater(() -> {
                view.updateTableData(finalData);
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading statistics data: " + e.getMessage());

            Platform.runLater(() -> {
                view.showAlert("Lỗi", "Đã xảy ra lỗi khi tải dữ liệu thống kê.", Alert.AlertType.ERROR);
                view.updateTableData(FXCollections.observableArrayList());
            });
            success = false;
        }

        return success;
    }

    /**
     * Handle Excel export functionality
     */
    public void handleExportExcel() {
        if (view == null) return;

        view.showSuccess("Đang xuất file Excel...");
        // TODO: Implement actual Excel export logic
    }

    /**
     * Get status options for dropdown
     */
    public List<String> getStatusOptions() {
        return statusOptions;
    }
}