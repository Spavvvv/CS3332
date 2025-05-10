package src.controller;

import java.time.LocalDate;
import src.model.report.ReportModel;
import view.components.ReportView;

public class ReportController {
    private final ReportModel model;
    private final ReportView view;

    public ReportController(ReportModel model, ReportView view) {
        this.model = model;
        this.view = view;
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        // Set search button to load report data from the DAO via the model
        view.getSearchButton().setOnAction(e -> loadReportData());

        // Additional event handlers for export or print functionality
        view.getExportPdfButton().setOnAction(e -> exportToPdf());
        view.getExportExcelButton().setOnAction(e -> exportToExcel());
        view.getPrintButton().setOnAction(e -> printReport());

        // On start, load initial data
        loadReportData();
    }

    private void loadReportData() {
        LocalDate fromDate = view.getFromDatePicker().getValue();
        LocalDate toDate = view.getToDatePicker().getValue();
        String status = view.getStatusComboBox().getValue();

        // Load report data using the DAO integrated in model
        model.loadReportData(fromDate, toDate, status);

        // Update view with data and calculated metrics
        view.updateMetrics(model.getAttendancePercentage(), model.getHomeworkPercentage());
        view.updateReportTable(model.getClassReportData());
    }

    private void exportToPdf() {
        // Implementation for exporting report to PDF
        view.showError("PDF export feature is under development.");
    }

    private void exportToExcel() {
        // Implementation for exporting report to Excel
        view.showError("Excel export feature is under development.");
    }

    private void printReport() {
        // Implementation for printing the report
        view.showError("Print feature is under development.");
    }
}
