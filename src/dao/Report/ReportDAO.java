package src.dao.Report;

import src.utils.DatabaseConnection;
import src.model.report.ReportModel.ClassReportData; // Đảm bảo import này đúng

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ReportDAO {

    private static final Logger LOGGER = Logger.getLogger(ReportDAO.class.getName());

    // Khối static để cấu hình logger (giữ nguyên)
    static {
        System.out.println("--- ReportDAO Static Initializer: Configuring Logger ---");
        try {
            boolean foundConsoleHandler = false;
            for (Handler existingHandler : LOGGER.getHandlers()) {
                if (existingHandler instanceof ConsoleHandler) {
                    existingHandler.setLevel(Level.INFO);
                    foundConsoleHandler = true;
                }
            }
            Logger rootLogger = Logger.getLogger("");
            for (Handler rootHandler : rootLogger.getHandlers()) {
                if (rootHandler instanceof ConsoleHandler) {
                    rootHandler.setLevel(Level.ALL);
                    System.out.println("--- ReportDAO Static Initializer: Set root ConsoleHandler level to ALL ---");
                }
            }
            if (!foundConsoleHandler && LOGGER.getHandlers().length == 0) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.INFO);
                consoleHandler.setFormatter(new SimpleFormatter());
                LOGGER.addHandler(consoleHandler);
                System.out.println("--- ReportDAO Static Initializer: Added new ConsoleHandler to LOGGER with level INFO ---");
            }
            LOGGER.setLevel(Level.INFO);
            LOGGER.setUseParentHandlers(false); // QUAN TRỌNG
            System.out.println("--- ReportDAO Static Initializer: Logger configured. Level: " + LOGGER.getLevel() + ", UseParentHandlers: " + LOGGER.getUseParentHandlers() + " ---");
            LOGGER.info("--- ReportDAO Static Initializer: Test INFO log from static block. ---");
        } catch (Exception e) {
            System.err.println("--- ReportDAO Static Initializer: Error configuring logger: " + e.getMessage() + " ---");
            e.printStackTrace();
        }
    }

    public ReportDAO() {
        LOGGER.info("DAO: ReportDAO instance created.");
        // Dòng này giúp bạn debug xem ai đang tạo ReportDAO
        new Throwable("DEBUG: ReportDAO instantiated at:").printStackTrace(System.err);
    }

    /**
     * Retrieves class report data from the database for a given date range.
     *
     * @param fromDate Starting date for the report period
     * @param toDate   Ending date for the report period
     * @return List of ClassReportData objects
     */
    public List<ClassReportData> getClassReportData(LocalDate fromDate, LocalDate toDate) { // Bỏ tham số statusFilter
        // System.out.println("DEBUG SYSOUT: ReportDAO.getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate); // Có thể bỏ bớt SYSOUT nếu LOGGER hoạt động tốt
        LOGGER.info("DAO: getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate);
        List<ClassReportData> reportData = new ArrayList<>();

        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Attempted to get class report data with null dates. Returning empty list.");
            return reportData;
        }

        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = toDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        // LOGGER.info("DAO: Formatted fromDateStr: " + fromDateStr + ", toDateStr: " + toDateStr); // Log này có thể không cần thiết mỗi lần chạy

        StringBuilder queryBuilder = new StringBuilder();
        // Đổi tên alias cho các cột count để rõ ràng hơn trong SQL và khi đọc ResultSet
        queryBuilder.append("SELECT c.course_id, c.course_name, ")
                .append("COUNT(DISTINCT cs.session_id) as sessions_held_in_period, ")
                .append("c.total_sessions as course_total_sessions_planned, ")
                .append("SUM(CASE WHEN a.present = 1 THEN 1 ELSE 0 END) as present_instances_count, ")
                // .append("COUNT(DISTINCT a.attendance_id) as total_attendance_records, ") // Có thể không cần nếu logic chính dựa trên present_instances_count
                .append("COUNT(DISTINCT h.homework_id) as distinct_homework_items_assigned, ") // Số bài tập lớn (assignments) được giao
                .append("SUM(CASE WHEN shs.is_submitted = 1 THEN 1 ELSE 0 END) as total_submission_instances, ") // Tổng số lượt nộp bài
                .append("AVG(sm.awareness_score) as avg_awareness_score, ")
                .append("AVG(sm.punctuality_score) as avg_punctuality_score, ")
                .append("AVG(shs.grade) as avg_homework_grade, ")
                .append("COUNT(DISTINCT e.student_id) as enrolled_student_count ")
                .append("FROM courses c ")
                .append("LEFT JOIN class_sessions cs ON c.course_id = cs.course_id AND cs.session_date BETWEEN ? AND ? ")
                .append("LEFT JOIN attendance a ON cs.session_id = a.session_id ")
                .append("LEFT JOIN homework h ON c.course_id = h.course_id AND h.assigned_date BETWEEN ? AND ? ")
                .append("LEFT JOIN student_homework_submissions shs ON h.homework_id = shs.homework_id ")
                .append("LEFT JOIN student_metrics sm ON c.course_id = sm.course_id AND sm.record_date BETWEEN ? AND ? ")
                .append("LEFT JOIN enrollment e ON c.course_id = e.course_id ");
        // KHÔNG CÓ ĐIỀU KIỆN WHERE c.status

        List<Object> parameters = new ArrayList<>();
        parameters.add(fromDateStr); // cho cs.session_date
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // cho h.assigned_date
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // cho sm.record_date
        parameters.add(toDateStr);

        queryBuilder.append("GROUP BY c.course_id, c.course_name, c.total_sessions ")
                .append("ORDER BY c.course_name");

        String query = queryBuilder.toString();
        //LOGGER.info("DAO: SQL Query: " + query); // Có thể comment lại nếu quá dài
        //LOGGER.info("DAO: Query parameters: " + parameters);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            //LOGGER.info("DAO: Executing query...");
            try (ResultSet rs = stmt.executeQuery()) {
                //LOGGER.info("DAO: Query executed. Processing ResultSet...");
                int counter = 1;
                while (rs.next()) {
                    //LOGGER.info("DAO: Processing row " + counter);
                    String courseName = rs.getString("course_name");
                    int studentCount = rs.getInt("enrolled_student_count");
                    int sessionsHeldInPeriod = rs.getInt("sessions_held_in_period");
                    // int totalCourseSessionsPlanned = rs.getInt("course_total_sessions_planned");

                    // Chuyên cần
                    int presentInstancesCount = rs.getInt("present_instances_count");
                    int totalPossibleAttendanceInPeriod = studentCount * sessionsHeldInPeriod;
                    String attendanceStr = presentInstancesCount + "/" + (totalPossibleAttendanceInPeriod > 0 ? totalPossibleAttendanceInPeriod : (sessionsHeldInPeriod > 0 ? sessionsHeldInPeriod : "0"));

                    // Bài tập
                    int distinctHomeworkItemsAssigned = rs.getInt("distinct_homework_items_assigned");
                    int totalSubmissionInstances = rs.getInt("total_submission_instances");

                    // SỬA ĐỔI LOGIC TÍNH "GIAO" CHO BÀI TẬP THEO YÊU CẦU: (số_bài_tập_lớn * số_học_sinh)
                    int totalHomeworkExpectedSubmissions = distinctHomeworkItemsAssigned * studentCount;
                    String homeworkStr = totalSubmissionInstances + "/" + (totalHomeworkExpectedSubmissions > 0 ? totalHomeworkExpectedSubmissions : (distinctHomeworkItemsAssigned > 0 ? distinctHomeworkItemsAssigned : "0"));

                    double avgHomeworkGrade = rs.getDouble("avg_homework_grade");
                    if (rs.wasNull()) avgHomeworkGrade = 0.0;
                    String formattedScore = String.format("%.2f/10", avgHomeworkGrade);

                    double awareness = rs.getDouble("avg_awareness_score");
                    if (rs.wasNull()) awareness = 0.0;
                    double punctuality = rs.getDouble("avg_punctuality_score");
                    if (rs.wasNull()) punctuality = 0.0;

                    ClassReportData data = new ClassReportData(
                            counter, // STT được tạo ở đây, không phải từ DB
                            courseName,
                            attendanceStr,
                            homeworkStr, // Đã cập nhật logic "Giao"
                            awareness,
                            punctuality,
                            formattedScore
                    );
                    reportData.add(data);
                    counter++;
                }
                if (counter == 1 && reportData.isEmpty()) {
                    LOGGER.info("DAO: getClassReportData - ResultSet was empty. No data rows found for the given criteria.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException retrieving class report data.", e);
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DAO: Unexpected Exception retrieving class report data.", e);
            e.printStackTrace();
        }

        LOGGER.info("DAO: getClassReportData finished. Returning " + reportData.size() + " records.");
        return reportData;
    }

    public double getAttendancePercentage(LocalDate fromDate, LocalDate toDate) {
        // Phương thức này giữ nguyên logic
        LOGGER.info("DAO: getAttendancePercentage called with fromDate: " + fromDate + ", toDate: " + toDate);
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Null dates for attendance percentage.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = toDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        String query = "SELECT " +
                "SUM(CASE WHEN a.present = 1 THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(a.attendance_id) as total_attendance " +
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.session_id " +
                "WHERE cs.session_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int presentCount = rs.getInt("present_count");
                    int totalAttendance = rs.getInt("total_attendance");
                    double percentage = totalAttendance > 0 ? (presentCount / (double) totalAttendance) * 100 : 0;
                    return percentage;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException calculating attendance percentage.", e);
            e.printStackTrace();
        }
        return 0;
    }

    public double getHomeworkPercentage(LocalDate fromDate, LocalDate toDate) {
        // Phương thức này giữ nguyên logic (tính % bài tập lớn có status 'completed')
        LOGGER.info("DAO: getHomeworkPercentage called with fromDate: " + fromDate + ", toDate: " + toDate);
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Null dates for homework percentage.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = toDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        String query = "SELECT " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_assignments_count, " +
                "COUNT(h.homework_id) as total_assignments_defined " +
                "FROM homework h " +
                "WHERE h.assigned_date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int completedAssignments = rs.getInt("completed_assignments_count");
                    int totalAssignmentsDefined = rs.getInt("total_assignments_defined");
                    double percentage = totalAssignmentsDefined > 0 ? (completedAssignments / (double) totalAssignmentsDefined) * 100 : 0;
                    return percentage;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException calculating homework percentage.", e);
            e.printStackTrace();
        }
        return 0;
    }

    // PHƯƠNG THỨC getDistinctClassStatuses() ĐÃ BỊ XÓA (do bảng courses không có cột status)
}