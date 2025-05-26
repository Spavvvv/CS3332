package src.model.report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import src.dao.Report.ReportDAO;
import src.utils.DaoManager; // THÊM IMPORT CHO DAO MANAGER

public class ReportModel {
    // Data containers
    private double attendancePercentage = 0;
    private double homeworkPercentage = 0;
    private ObservableList<ClassReportData> classReportData = FXCollections.observableArrayList();
    private final ReportDAO reportDAO; // Sẽ được lấy từ DaoManager
    private double trueOverallAverageHomeworkScore = 0.0;

    public ReportModel() {
        // SỬA ĐỔI: Lấy ReportDAO từ DaoManager thay vì tạo mới
        this.reportDAO = DaoManager.getInstance().getReportDAO();
        // Đảm bảo DaoManager.getInstance().getReportDAO() trả về một instance đã được khởi tạo của ReportDAO
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public double getHomeworkPercentage() {
        return homeworkPercentage;
    }

    public ObservableList<ClassReportData> getClassReportData() {
        return classReportData;
    }

    public void loadReportData(LocalDate fromDate, LocalDate toDate) {
        classReportData.clear();
        this.trueOverallAverageHomeworkScore = 0.0;
        try {
            List<ClassReportData> rawData = reportDAO.getClassReportData(fromDate, toDate);
            if (rawData != null) {
                classReportData.addAll(rawData);
            }

            ReportDAO.OverallHomeworkStats overallStats = reportDAO.getOverallHomeworkSubmissionStats(fromDate, toDate);
            if (overallStats.countOfGradedSubmissions > 0) {
                this.trueOverallAverageHomeworkScore = overallStats.sumOfGrades / overallStats.countOfGradedSubmissions;
            } else {
                this.trueOverallAverageHomeworkScore = 0.0; // Hoặc Double.NaN nếu bạn muốn biểu thị "không có dữ liệu"
            }

        } catch (Exception e) {
            System.err.println("Error loading report data from DAO: " + e.getMessage());
            e.printStackTrace();
            // Consider throwing a custom exception for the Controller to handle
            // Reset các giá trị nếu có lỗi
            classReportData.clear();
            this.trueOverallAverageHomeworkScore = 0.0;
        }
        calculateStatistics();
    }

    private void calculateStatistics() {
        this.attendancePercentage = calculateAttendancePercentageInternal();
        this.homeworkPercentage = calculateHomeworkPercentageInternal();
    }

    private double calculateAttendancePercentageInternal() {
        int totalPresentDays = 0;
        int totalDays = 0;
        for (ClassReportData data : classReportData) {
            if (data.getAttendance() == null || data.getAttendance().isEmpty()) continue;
            String[] attendanceParts = data.getAttendance().split("/");
            if (attendanceParts.length == 2) {
                try {
                    totalPresentDays += Integer.parseInt(attendanceParts[0].trim());
                    totalDays += Integer.parseInt(attendanceParts[1].trim());
                } catch (NumberFormatException ignored) {
                    // Ignore format errors
                }
            }
        }
        return totalDays > 0 ? (totalPresentDays / (double) totalDays) * 100 : 0;
    }

    private double calculateHomeworkPercentageInternal() {
        int totalCompletedHomework = 0;
        int totalHomework = 0;
        for (ClassReportData data : classReportData) {
            if (data.getHomework() == null || data.getHomework().isEmpty()) continue;
            String[] homeworkParts = data.getHomework().split("/");
            if (homeworkParts.length == 2) {
                try {
                    totalCompletedHomework += Integer.parseInt(homeworkParts[0].trim());
                    totalHomework += Integer.parseInt(homeworkParts[1].trim());
                } catch (NumberFormatException ignored) {
                    // Ignore format errors
                }
            }
        }
        return totalHomework > 0 ? (totalCompletedHomework / (double) totalHomework) * 100 : 0;
    }


    public double getAverageAwareness() {
        if (classReportData == null || classReportData.isEmpty()) {
            return 0.0;
        }
        double totalAwareness = 0;
        int count = 0;
        for (ClassReportData data : classReportData) {
            totalAwareness += data.getAwareness();
            count++;
        }
        return count > 0 ? totalAwareness / count : 0.0;
    }

    public double getAveragePunctuality() {
        if (classReportData == null || classReportData.isEmpty()) {
            return 0.0;
        }
        double totalPunctuality = 0;
        int count = 0;
        for (ClassReportData data : classReportData) {
            totalPunctuality += data.getPunctuality();
            count++;
        }
        return count > 0 ? totalPunctuality / count : 0.0;
    }

    public double getAverageHomeworkScore() {
        return this.trueOverallAverageHomeworkScore;
    }

    // Data model class for report table
    public static class ClassReportData {
        private final int stt;
        private final String className;
        private final String attendance;
        private final String homework;
        private final double awareness;
        private final double punctuality;
        private final String homeworkScore;

        public ClassReportData(int stt, String className, String attendance, String homework,
                               double awareness, double punctuality, String homeworkScore) {
            this.stt = stt;
            this.className = className;
            this.attendance = attendance;
            this.homework = homework;
            this.awareness = awareness;
            this.punctuality = punctuality;
            this.homeworkScore = homeworkScore;
        }

        public int getStt() { return stt; }
        public String getClassName() { return className; }
        public String getAttendance() { return attendance; }
        public String getHomework() { return homework; }
        public double getAwareness() { return awareness; }
        public double getPunctuality() { return punctuality; }
        public String getHomeworkScore() { return homeworkScore; }
    }
}