package src.controller.Reports;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.report.ReportModel;
import src.model.report.ReportModel.ClassReportData;
// Import ReportView để controller có thể tương tác
import src.view.Report.ReportView;

import java.time.LocalDate;

public class ReportController {
    private static final String DEFAULT_STATUS_FILTER = "Tất cả";

    private ReportModel model;
    private ReportView view;

    public ReportController(ReportModel model, ReportView view) {
        System.out.println("ReportController constructor: START");
        this.model = model;
        this.view = view;

        if (this.model == null) {
            System.err.println("ReportController constructor: CRITICAL - ReportModel is null!");
        }
        if (this.view == null) {
            System.err.println("ReportController constructor: CRITICAL - ReportView is null!");
        }
        System.out.println("ReportController constructor: Model and View references stored.");
        System.out.println("ReportController constructor: END.");
    }

    public void initializeEventHandlers() {
        System.out.println("ReportController.initializeEventHandlers() CALLED.");
        if (view == null) {
            System.err.println("ReportController.initializeEventHandlers: View is null. Cannot set event handlers.");
            return;
        }

        if (view.getSearchButton() != null) {
            System.out.println(">>>> SearchButton ACTION: Clicked! Attempting to call loadReportData(). <<<<");
            System.out.println("SearchButton ACTION: Clicked! Attempting to call loadReportData().");
            view.getSearchButton().setOnAction(e -> loadReportData());
        } else {
            System.err.println("ReportController.initializeEventHandlers: SearchButton is null in View.");
        }
        if (view.getExportPdfButton() != null) {
            view.getExportPdfButton().setOnAction(e -> exportToPdf());
        } else {
            System.err.println("ReportController.initializeEventHandlers: ExportPdfButton is null in View.");
        }
        if (view.getExportExcelButton() != null) {
            view.getExportExcelButton().setOnAction(e -> exportToExcel());
        } else {
            System.err.println("ReportController.initializeEventHandlers: ExportExcelButton is null in View.");
        }
        if (view.getPrintButton() != null) {
            view.getPrintButton().setOnAction(e -> printReport());
        } else {
            System.err.println("ReportController.initializeEventHandlers: PrintButton is null in View.");
        }
    }

    public void loadInitialData() {
        System.out.println("ReportController.loadInitialData() CALLED.");
        if (model == null || view == null) {
            System.err.println("ReportController.loadInitialData: Model or View is null. Aborting.");
            if(view!=null) view.showError("Lỗi khởi tạo: Model hoặc View không hợp lệ.");
            return;
        }
        loadReportData();
    }

    public void loadReportData() {

        System.out.println("ReportController.loadReportData() CALLED.");
        if (model == null || view == null) {
            System.err.println("ReportController.loadReportData: Model or View is null. Aborting.");
            if(view!=null) view.showError("Lỗi tải dữ liệu: Model hoặc View không hợp lệ.");
            return;
        }
        if (view.getFromDatePicker() == null || view.getToDatePicker() == null) {
            System.err.println("ReportController.loadReportData: UI filter components are null in View.");
            view.showError("Lỗi: Không thể tải dữ liệu do các thành phần lọc chưa sẵn sàng.");
            return;
        }

        LocalDate fromDate = view.getFromDatePicker().getValue();
        LocalDate toDate = view.getToDatePicker().getValue();


        if (fromDate == null || toDate == null) {
            view.showError("Vui lòng chọn ngày bắt đầu và ngày kết thúc.");
            view.updateReportTable(FXCollections.observableArrayList()); // Xóa bảng
            view.updateOverallMetrics(0,0,0,0,0); // Reset metrics
            return;
        }
        if (fromDate.isAfter(toDate)) {
            view.showError("Ngày bắt đầu không được sau ngày kết thúc.");
            view.updateReportTable(FXCollections.observableArrayList());
            view.updateOverallMetrics(0,0,0,0,0);
            return;
        }

        System.out.println("ReportController: Requesting model to load data with params - From: " + fromDate + ", To: " + toDate);
        try {
            // Bước 1: Yêu cầu model tải và xử lý dữ liệu
            model.loadReportData(fromDate, toDate);

            // Bước 2: Lấy dữ liệu đã xử lý từ model thông qua các getters
            double attendancePercentage = model.getAttendancePercentage();
            double homeworkPercentage = model.getHomeworkPercentage();
            ObservableList<ClassReportData> classReportDataList = model.getClassReportData();
            double avgAwareness = model.getAverageAwareness();
            double avgPunctuality = model.getAveragePunctuality();
            double avgHomeworkScore = model.getAverageHomeworkScore();

            System.out.println("ReportController: Data retrieved from model. Updating src.view.");
            // Bước 3: Cập nhật src.view với dữ liệu mới
            view.updateOverallMetrics(attendancePercentage, homeworkPercentage, avgAwareness, avgPunctuality, avgHomeworkScore);
            // Model.getClassReportData() trả về ObservableList, không cần FXCollections.observableArrayList() nữa
            // nhưng vẫn kiểm tra null cho an toàn.
            view.updateReportTable(classReportDataList != null ? classReportDataList : FXCollections.observableArrayList());

        } catch (Exception e) {
            // Exception này có thể là RuntimeException từ model hoặc lỗi không mong muốn khác
            System.err.println("Error in loadReportData (Controller level) or updating src.view: " + e.getMessage());
            e.printStackTrace();
            view.showError("Không thể tải hoặc hiển thị dữ liệu báo cáo: " + e.getMessage());
            view.updateReportTable(FXCollections.observableArrayList()); // Xóa bảng nếu có lỗi
            view.updateOverallMetrics(0,0,0,0,0); // Reset metrics
        }
    }

    public String getDefaultStatusFilter() {
        return DEFAULT_STATUS_FILTER;
    }

    private void exportToPdf() {
        if(view!=null) view.showError("Chức năng xuất PDF đang được phát triển.");
        System.out.println("ReportController: exportToPdf called.");
    }

    private void exportToExcel() {
        if(view!=null) view.showError("Chức năng xuất Excel đang được phát triển.");
        System.out.println("ReportController: exportToExcel called.");
    }

    private void printReport() {
        if(view!=null) view.showError("Chức năng in đang được phát triển.");
        System.out.println("ReportController: printReport called.");
    }
}

