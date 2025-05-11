// ReportModel.java
package src.model.report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.sql.SQLException;

import src.dao.ReportDAO;

public class ReportModel {
    // Data containers
    private double attendancePercentage = 0;
    private double homeworkPercentage = 0;
    private ObservableList<ClassReportData> classReportData = FXCollections.observableArrayList();
    private final ReportDAO reportDAO;

    public ReportModel() {
        // Initialize model with DAO
        this.reportDAO = new ReportDAO();
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

    public void loadReportData(LocalDate fromDate, LocalDate toDate, String status) {
        // Clear existing data
        classReportData.clear();

        // Load data from database through DAO
        classReportData.addAll(reportDAO.getClassReportData(fromDate, toDate, status));

        // Calculate statistics based on the loaded data
        calculateStatistics();
    }

    private void calculateStatistics() {
        // Calculate percentages based on the current data
        attendancePercentage = calculateAttendancePercentage();
        homeworkPercentage = calculateHomeworkPercentage();
    }

    // Calculate attendance percentage
    public double calculateAttendancePercentage() {
        int totalPresentDays = 0;
        int totalDays = 0;
        for (ClassReportData data : classReportData) {
            String[] attendanceParts = data.getAttendance().split("/");
            if (attendanceParts.length == 2) {
                try {
                    totalPresentDays += Integer.parseInt(attendanceParts[0]);
                    totalDays += Integer.parseInt(attendanceParts[1]);
                } catch (NumberFormatException ignored) {
                    // Ignore format errors
                }
            }
        }
        return totalDays > 0 ? (totalPresentDays / (double) totalDays) * 100 : 0;
    }

    // Calculate homework completion percentage
    public double calculateHomeworkPercentage() {
        int totalCompletedHomework = 0;
        int totalHomework = 0;
        for (ClassReportData data : classReportData) {
            String[] homeworkParts = data.getHomework().split("/");
            if (homeworkParts.length == 2) {
                try {
                    totalCompletedHomework += Integer.parseInt(homeworkParts[0]);
                    totalHomework += Integer.parseInt(homeworkParts[1]);
                } catch (NumberFormatException ignored) {
                    // Ignore format errors
                }
            }
        }
        return totalHomework > 0 ? (totalCompletedHomework / (double) totalHomework) * 100 : 0;
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