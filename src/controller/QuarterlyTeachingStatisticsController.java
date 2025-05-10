package src.controller;

import src.dao.TeacherQuarterlyStatisticsDAO;
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import view.components.QuarterlyTeachingStatisticsView;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class QuarterlyTeachingStatisticsController {
    private final QuarterlyTeachingStatisticsView view;
    private final TeacherQuarterlyStatisticsDAO dao;

    // Constants
    private final List<String> statusOptions = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");
    private final List<Integer> quarters = Arrays.asList(1, 2, 3, 4);
    private static final String TOGGLE_STATE_KEY = "statistics_view_type";

    // User preferences for persistence
    private final Preferences prefs = Preferences.userNodeForPackage(QuarterlyTeachingStatisticsController.class);

    public QuarterlyTeachingStatisticsController(QuarterlyTeachingStatisticsView view) {
        this.view = view;
        this.dao = new TeacherQuarterlyStatisticsDAO();
        initController();
    }

    private void initController() {
        // Setup view with data
        view.setQuartersList(quarters);
        view.setStatusOptions(statusOptions);

        // Setup event handlers
        view.setSearchButtonHandler(e -> handleSearch());
        view.setExportExcelButtonHandler(e -> handleExportExcel());
        view.setExportPdfButtonHandler(e -> handleExportPdf());
        view.setPrintButtonHandler(e -> handlePrint());

        // Toggle handlers
        view.setDayToggleHandler(e -> {
            if (view.isDayToggleSelected()) {
                saveToggleState("Ngày");
                navigateToView("teaching-statistics");
            }
        });

        view.setMonthToggleHandler(e -> {
            if (view.isMonthToggleSelected()) {
                saveToggleState("Tháng");
                navigateToView("monthly-teaching");
            }
        });

        view.setQuarterToggleHandler(e -> {
            if (view.isQuarterToggleSelected()) {
                saveToggleState("Quý");
            }
        });

        view.setYearToggleHandler(e -> {
            if (view.isYearToggleSelected()) {
                saveToggleState("Năm");
                navigateToView("yearly-teaching");
            }
        });

        // Load saved toggle state if available
        loadSavedToggleState();

        // Load initial data
        loadData();
    }

    public void loadData() {
        int fromQuarter = view.getFromQuarter();
        int fromYear = view.getFromYear();
        int toQuarter = view.getToQuarter();
        int toYear = view.getToYear();
        String status = view.getSelectedStatus();

        ObservableList<TeacherQuarterlyStatisticsModel> data =
                dao.getTeacherStatistics(fromQuarter, fromYear, toQuarter, toYear, status);

        double[] summaryData =
                dao.getStatisticsSummary(fromQuarter, fromYear, toQuarter, toYear, status);

        view.updateTableData(data);
        view.updateSummaryRow(summaryData);
    }

    private void handleSearch() {
        // Load data based on search criteria
        loadData();
    }

    private void handleExportExcel() {
        view.showSuccessMessage("Đang xuất file Excel...");
        // Implementation for Excel export would go here
        // Would use data from the DAO to generate the Excel file
    }

    private void handleExportPdf() {
        view.showSuccessMessage("Đang xuất file PDF...");
        // Implementation for PDF export would go here
        // Would use data from the DAO to generate the PDF file
    }

    private void handlePrint() {
        view.showSuccessMessage("Đang chuẩn bị in...");
        // Implementation for printing would go here
        // Would use data from the DAO to generate printable content
    }

    private void saveToggleState(String state) {
        prefs.put(TOGGLE_STATE_KEY, state);
    }

    private void loadSavedToggleState() {
        String savedState = prefs.get(TOGGLE_STATE_KEY, "Quý");
        view.selectToggleByState(savedState);
    }

    private void navigateToView(String viewName) {
        // Navigation logic would be implemented here
        // This would depend on your application's navigation framework
    }
}