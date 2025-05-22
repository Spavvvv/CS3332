
package src.model.report;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.util.List; // Cần thiết cho getClassStatusesFromDAO
import java.util.ArrayList; // Có thể được sử dụng bởi DAO

import src.dao.Report.ReportDAO;

public class ReportModel {
    // Data containers
    private double attendancePercentage = 0;
    private double homeworkPercentage = 0;
    private ObservableList<ClassReportData> classReportData = FXCollections.observableArrayList();
    private final ReportDAO reportDAO;

    // Cached average values (optional, can be calculated on the fly)
    // Nếu không cache, các phương thức getAverage... sẽ tính toán mỗi khi được gọi.
    // Để đơn giản, chúng ta sẽ tính toán chúng on-the-fly trong các getters.

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

        try {
            // Load data from database through DAO
            // Giả sử reportDAO.getClassReportData trả về List<ClassReportData>
            // và chúng ta cần chuyển nó sang ObservableList nếu cần.
            // Hoặc ReportDAO có thể trả về ObservableList trực tiếp.
            List<ClassReportData> rawData = reportDAO.getClassReportData(fromDate, toDate, status);
            if (rawData != null) {
                classReportData.addAll(rawData);
            }
        } catch (Exception e) {
            // Xử lý lỗi, ví dụ: ghi log hoặc ném một RuntimeException
            System.err.println("Error loading report data from DAO: " + e.getMessage());
            e.printStackTrace();
            // Có thể ném một exception tùy chỉnh để Controller bắt và hiển thị lỗi cho người dùng
            // throw new RuntimeException("Failed to load report data", e);
        }


        // Calculate statistics based on the loaded data
        calculateStatistics();
    }

    private void calculateStatistics() {
        // Calculate percentages based on the current data
        this.attendancePercentage = calculateAttendancePercentageInternal();
        this.homeworkPercentage = calculateHomeworkPercentageInternal();
        // Các giá trị trung bình khác sẽ được tính trong các getters của chúng
    }

    // Calculate attendance percentage
    private double calculateAttendancePercentageInternal() { // Đổi thành private hoặc package-private
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

    // Calculate homework completion percentage
    private double calculateHomeworkPercentageInternal() { // Đổi thành private hoặc package-private
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

    // *** CÁC PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO ***

    /**
     * Lấy danh sách các trạng thái lớp học từ DAO.
     * Controller sẽ sử dụng danh sách này để điền vào ComboBox.
     * @return List<String> các trạng thái lớp học.
     */
    public List<String> getClassStatusesFromDAO() {
        try {
            // Giả sử reportDAO có phương thức getDistinctClassStatuses()
            // Trả về một danh sách trống nếu DAO trả về null để tránh NullPointerException trong Controller
            List<String> statuses = reportDAO.getDistinctClassStatuses();
            return statuses != null ? statuses : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching class statuses from DAO: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Trả về danh sách trống khi có lỗi
        }
    }

    /**
     * Tính toán và trả về điểm ý thức học trung bình.
     * @return double điểm ý thức học trung bình.
     */
    public double getAverageAwareness() {
        if (classReportData == null || classReportData.isEmpty()) {
            return 0.0;
        }
        double totalAwareness = 0;
        int count = 0;
        for (ClassReportData data : classReportData) {
            // Chỉ tính những lớp có dữ liệu ý thức học hợp lệ (ví dụ: > 0 nếu 0 nghĩa là không có dữ liệu)
            // Hoặc nếu awareness là một giá trị luôn có, thì không cần kiểm tra > 0.
            // Giả sử getAwareness() trả về giá trị số.
            totalAwareness += data.getAwareness();
            count++;
        }
        return count > 0 ? totalAwareness / count : 0.0;
    }

    /**
     * Tính toán và trả về điểm đúng giờ trung bình.
     * @return double điểm đúng giờ trung bình.
     */
    public double getAveragePunctuality() {
        if (classReportData == null || classReportData.isEmpty()) {
            return 0.0;
        }
        double totalPunctuality = 0;
        int count = 0;
        for (ClassReportData data : classReportData) {
            // Tương tự như awareness, giả sử getPunctuality() trả về giá trị số.
            totalPunctuality += data.getPunctuality();
            count++;
        }
        return count > 0 ? totalPunctuality / count : 0.0;
    }

    /**
     * Tính toán và trả về điểm bài tập về nhà trung bình.
     * Định dạng điểm bài tập là "X/Y" hoặc chỉ "X".
     * @return double điểm bài tập về nhà trung bình (ví dụ: trên thang điểm 10).
     */
    public double getAverageHomeworkScore() {
        if (classReportData == null || classReportData.isEmpty()) {
            return 0.0;
        }
        double totalScore = 0;
        int count = 0;
        for (ClassReportData data : classReportData) {
            String scoreStr = data.getHomeworkScore();
            if (scoreStr == null || scoreStr.trim().isEmpty() || scoreStr.equalsIgnoreCase("N/A")) {
                continue; // Bỏ qua nếu không có điểm hoặc không áp dụng
            }
            try {
                if (scoreStr.contains("/")) {
                    String[] parts = scoreStr.split("/");
                    if (parts.length > 0) { // Chỉ lấy phần tử đầu tiên làm điểm số
                        totalScore += Double.parseDouble(parts[0].trim());
                        count++;
                    }
                } else { // Nếu chỉ là một số (ví dụ: "8.5")
                    totalScore += Double.parseDouble(scoreStr.trim());
                    count++;
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse homework score: " + scoreStr + " for class " + data.getClassName());
                // Bỏ qua điểm không hợp lệ
            }
        }
        return count > 0 ? totalScore / count : 0.0;
    }

    // Data model class for report table
    public static class ClassReportData {
        private final int stt;
        private final String className;
        private final String attendance; // ví dụ: "8/10"
        private final String homework;   // ví dụ: "5/5"
        private final double awareness;  // ví dụ: 4.5 (sao)
        private final double punctuality;// ví dụ: 9.2 (điểm)
        private final String homeworkScore; // ví dụ: "8.5/10" hoặc "7"

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

