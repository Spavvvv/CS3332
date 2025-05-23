
// File: controller/ReportController.java (Hoặc src.controller.Reports.ReportController theo import của bạn)
package src.controller.Reports; // Hoặc package tương ứng của bạn

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.report.ReportModel;
import src.model.report.ReportModel.ClassReportData;
// Import ReportView để controller có thể tương tác
import src.view.Report.ReportView; // Điều chỉnh package nếu cần

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
        loadClassStatuses();
        loadReportData();
    }

    private void loadClassStatuses() {
        try {
            System.out.println("ReportController: Attempting to load class statuses from model.");
            // Gọi phương thức từ model
            List<String> statuses = model.getClassStatusesFromDAO();
            // Model đã xử lý trả về ArrayList rỗng nếu có lỗi hoặc null từ DAO

            if (!statuses.contains(DEFAULT_STATUS_FILTER) && DEFAULT_STATUS_FILTER.equals("Tất cả")) {
                statuses.add(0, DEFAULT_STATUS_FILTER);
            }
            System.out.println("ReportController: Calling src.view.setClassStatusOptions with " + statuses.size() + " items.");
            view.setClassStatusOptions(statuses);
        } catch (Exception e) {
            // Mặc dù model đã xử lý, thêm một lớp catch ở đây để phòng ngừa
            System.err.println("Unexpected error loading class statuses in Controller: " + e.getMessage());
            e.printStackTrace();
            view.showError("Lỗi không mong muốn khi tải danh sách trạng thái: " + e.getMessage());
            view.setClassStatusOptions(new ArrayList<>(List.of(DEFAULT_STATUS_FILTER))); // Cung cấp giá trị mặc định
        }
    }

    public void loadReportData() {
        System.out.println("ReportController.loadReportData() CALLED.");
        if (model == null || view == null) {
            System.err.println("ReportController.loadReportData: Model or View is null. Aborting.");
            if(view!=null) view.showError("Lỗi tải dữ liệu: Model hoặc View không hợp lệ.");
            return;
        }
        if (view.getFromDatePicker() == null || view.getToDatePicker() == null || view.getStatusComboBox() == null) {
            System.err.println("ReportController.loadReportData: UI filter components are null in View.");
            view.showError("Lỗi: Không thể tải dữ liệu do các thành phần lọc chưa sẵn sàng.");
            return;
        }

        LocalDate fromDate = view.getFromDatePicker().getValue();
        LocalDate toDate = view.getToDatePicker().getValue();
        String status = view.getStatusComboBox().getValue();

        if (status == null && view.getStatusComboBox().getItems() != null && !view.getStatusComboBox().getItems().isEmpty()) {
            if(view.getStatusComboBox().getItems().contains(DEFAULT_STATUS_FILTER)) {
                status = DEFAULT_STATUS_FILTER;
            } else {
                status = view.getStatusComboBox().getItems().get(0);
            }
            view.getStatusComboBox().setValue(status);
        } else if (status == null) {
            status = DEFAULT_STATUS_FILTER;
        }

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

        System.out.println("ReportController: Requesting model to load data with params - From: " + fromDate + ", To: " + toDate + ", Status: " + status);
        try {
            // Bước 1: Yêu cầu model tải và xử lý dữ liệu
            model.loadReportData(fromDate, toDate, status);

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

