
package src.dao;

import utils.DatabaseConnection;
import src.model.report.ReportModel.ClassReportData;

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

    static {
        // Cố gắng cấu hình logger này để hiển thị INFO ra console
        // Điều này hữu ích nếu cấu hình logger mặc định của ứng dụng không hiển thị INFO
        System.out.println("--- ReportDAO Static Initializer: Configuring Logger ---");
        try {
            // Bỏ các handler hiện có để tránh trùng lặp nếu có
            // và để đảm bảo handler của chúng ta được sử dụng với cài đặt mong muốn.
            // Điều này có thể hơi mạnh tay nếu có các handler khác được cấu hình ở nơi khác.
            // Một cách tiếp cận nhẹ nhàng hơn là kiểm tra xem đã có ConsoleHandler phù hợp chưa.
            boolean foundConsoleHandler = false;
            for (Handler existingHandler : LOGGER.getHandlers()) {
                if (existingHandler instanceof ConsoleHandler) {
                    existingHandler.setLevel(Level.INFO); // Đảm bảo nó ở mức INFO
                    foundConsoleHandler = true;
                }
            }
            // Nếu logger cha (root logger) đã có ConsoleHandler, log vẫn có thể xuất hiện
            // ngay cả khi logger này không có handler trực tiếp. Kiểm tra Root Logger:
            Logger rootLogger = Logger.getLogger("");
            for (Handler rootHandler : rootLogger.getHandlers()) {
                if (rootHandler instanceof ConsoleHandler) {
                    rootHandler.setLevel(Level.ALL); // Đặt root console handler ở mức ALL để bắt mọi thứ cho mục đích debug
                    // Hoặc Level.INFO nếu bạn chỉ muốn INFO trở lên
                    System.out.println("--- ReportDAO Static Initializer: Set root ConsoleHandler level to ALL ---");
                }
            }


            if (!foundConsoleHandler && LOGGER.getHandlers().length == 0) {
                // Chỉ thêm handler mới nếu LOGGER này chưa có ConsoleHandler nào
                // và không có handler nào cả (để tránh ghi đè handler tùy chỉnh nếu có)
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.INFO); // Đặt mức cho handler
                consoleHandler.setFormatter(new SimpleFormatter()); // Định dạng đơn giản cho log
                LOGGER.addHandler(consoleHandler);
                System.out.println("--- ReportDAO Static Initializer: Added new ConsoleHandler to LOGGER with level INFO ---");
            }

            LOGGER.setLevel(Level.INFO); // Đặt mức cho logger này
            LOGGER.setUseParentHandlers(false); // Quan trọng: Ngăn log bị xử lý (và có thể bị lọc) bởi logger cha
            // nếu logger cha có cài đặt khác.
            // Tuy nhiên, nếu root logger là nguồn duy nhất của console output,
            // bạn có thể cần đặt thành true hoặc cấu hình root logger.
            // Đối với thử nghiệm này, false với handler riêng của nó là tốt.

            System.out.println("--- ReportDAO Static Initializer: Logger configured. Level: " + LOGGER.getLevel() + ", UseParentHandlers: " + LOGGER.getUseParentHandlers() + " ---");
            LOGGER.info("--- ReportDAO Static Initializer: Test INFO log from static block. ---");
        } catch (Exception e) {
            System.err.println("--- ReportDAO Static Initializer: Error configuring logger: " + e.getMessage() + " ---");
            e.printStackTrace();
        }
    }


    /**
     * Constructor.
     */
    public ReportDAO() {
        // No dependencies to inject for this DAO based on current implementation
        LOGGER.info("DAO: ReportDAO instance created.");
    }

    /**
     * Retrieves class report data from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @param status Status filter for classes
     * @return List of ClassReportData objects
     */
    public List<ClassReportData> getClassReportData(LocalDate fromDate, LocalDate toDate, String status) {
        // Sử dụng System.out.println để đảm bảo chúng ta thấy gì đó ngay cả khi logger không hoạt động như ý
        System.out.println("DEBUG SYSOUT: ReportDAO.getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate + ", status: " + status);
        LOGGER.info("DAO: getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate + ", status: " + status);
        List<ClassReportData> reportData = new ArrayList<>();

        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Attempted to get class report data with null dates. Returning empty list.");
            System.out.println("DEBUG SYSOUT: ReportDAO.getClassReportData - null dates, returning empty list.");
            return reportData; // Return empty list for invalid input
        }

        // Format dates for SQL query
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LOGGER.info("DAO: Formatted fromDateStr: " + fromDateStr + ", toDateStr: " + toDateStr);

        // Build the status condition for the SQL query
        String statusCondition = "1=1"; // Default to include all
        if (status != null && !status.equals("Tất cả")) {
            statusCondition = "c.status = ?";
        }
        LOGGER.info("DAO: Status condition: " + statusCondition);

        // SQL query to get class report data
        String query = "SELECT c.class_id, c.class_name, " +
                "COUNT(DISTINCT a.session_id) as total_sessions, " +
                "SUM(CASE WHEN a.status = 'Có mặt' THEN 1 ELSE 0 END) as present_count, " +
                "COUNT(DISTINCT a.attendance_id) as total_attendance_records, " +
                "COUNT(DISTINCT h.homework_id) as total_homework, " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_homework, " +
                "AVG(sm.awareness_score) as awareness, " +
                "AVG(sm.punctuality_score) as punctuality, " +
                "AVG(h.score) as homework_score, " +
                "COUNT(DISTINCT s.id) as student_count " +
                "FROM classes c " +
                "LEFT JOIN class_sessions cs ON c.class_id = cs.class_id " +
                "AND cs.session_date BETWEEN ? AND ? " +
                "LEFT JOIN attendance a ON cs.session_id = a.session_id " +
                "LEFT JOIN homework h ON c.class_id = h.class_id " +
                "AND h.assigned_date BETWEEN ? AND ? " +
                "LEFT JOIN student_metrics sm ON c.class_id = sm.class_id " +
                "AND sm.record_date BETWEEN ? AND ? " +
                "LEFT JOIN students s ON c.class_id = s.class_id " +
                "WHERE " + statusCondition + " " +
                "GROUP BY c.class_id, c.class_name " +
                "ORDER BY c.class_name";
        LOGGER.info("DAO: SQL Query: " + query);


        // Prepare parameters based on whether status filter is applied
        List<String> parameters = new ArrayList<>();
        parameters.add(fromDateStr); // session_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // homework assigned_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        parameters.add(fromDateStr); // student_metrics record_date BETWEEN ? AND ?
        parameters.add(toDateStr);
        if (status != null && !status.equals("Tất cả")) {
            parameters.add(status); // status = ?
        }
        LOGGER.info("DAO: Query parameters: " + parameters);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            LOGGER.info("DAO: Database connection obtained and PreparedStatement created.");

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
                LOGGER.info("DAO: Setting parameter " + (i + 1) + ": " + parameters.get(i));
            }

            LOGGER.info("DAO: Executing query...");
            try (ResultSet rs = stmt.executeQuery()) {
                LOGGER.info("DAO: Query executed. Processing ResultSet...");
                int counter = 1;
                while (rs.next()) {
                    LOGGER.info("DAO: Processing row " + counter);
                    // Calculate attendance string representation
                    int presentCount = rs.getInt("present_count");
                    int totalSessions = rs.getInt("total_sessions");
                    int studentCount = rs.getInt("student_count");
                    int totalAttendancePossible = totalSessions * studentCount;
                    String attendance = presentCount + "/" + (totalAttendancePossible > 0 ? totalAttendancePossible : 0);
                    LOGGER.info("DAO: Row " + counter + " - presentCount: " + presentCount + ", totalSessions: " + totalSessions + ", studentCount: " + studentCount + ", attendance: " + attendance);


                    // Calculate homework string representation
                    int completedHomework = rs.getInt("completed_homework");
                    int totalHomework = rs.getInt("total_homework");
                    String homework = completedHomework + "/" + totalHomework;
                    LOGGER.info("DAO: Row " + counter + " - completedHomework: " + completedHomework + ", totalHomework: " + totalHomework + ", homework: " + homework);


                    // Format homework score
                    double homeworkScoreRaw = rs.getDouble("homework_score");
                    String formattedScore = String.format("%.2f/10", homeworkScoreRaw);
                    LOGGER.info("DAO: Row " + counter + " - homeworkScoreRaw: " + homeworkScoreRaw + ", formattedScore: " + formattedScore);

                    String className = rs.getString("class_name");
                    double awareness = rs.getDouble("awareness");
                    double punctuality = rs.getDouble("punctuality");
                    LOGGER.info("DAO: Row " + counter + " - className: " + className + ", awareness: " + awareness + ", punctuality: " + punctuality);

                    // Create ClassReportData object
                    ClassReportData data = new ClassReportData(
                            counter, // STT
                            className,
                            attendance,
                            homework,
                            awareness,
                            punctuality,
                            formattedScore
                    );
                    LOGGER.info("DAO: Row " + counter + " - Created ClassReportData object: " + data.getClassName() + " | " + data.getAttendance()); // Log some key fields
                    reportData.add(data);
                    counter++;
                }
                if (counter == 1) { // No rows processed
                    LOGGER.info("DAO: ResultSet was empty. No data rows found for the given criteria.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException retrieving class report data.", e);
            System.err.println("DEBUG SYSOUT: SQLException in getClassReportData: " + e.getMessage()); // Thêm SYSOUT cho lỗi
            e.printStackTrace(); // In stack trace ra System.err
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DAO: Unexpected Exception retrieving class report data.", e);
            System.err.println("DEBUG SYSOUT: Exception in getClassReportData: " + e.getMessage()); // Thêm SYSOUT cho lỗi
            e.printStackTrace(); // In stack trace ra System.err
        }

        LOGGER.info("DAO: getClassReportData finished. Returning " + reportData.size() + " records.");
        System.out.println("DEBUG SYSOUT: ReportDAO.getClassReportData finished. Returning " + reportData.size() + " records.");
        return reportData;
    }

    /**
     * Gets the attendance statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of attendance (present records / total records)
     */
    public double getAttendancePercentage(LocalDate fromDate, LocalDate toDate) {
        System.out.println("DEBUG SYSOUT: ReportDAO.getAttendancePercentage called.");
        LOGGER.info("DAO: getAttendancePercentage called with fromDate: " + fromDate + ", toDate: " + toDate);
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Attempted to get attendance percentage with null dates.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LOGGER.info("DAO: Formatted dates for attendance percentage: " + fromDateStr + ", " + toDateStr);


        String query = "SELECT " +
                "SUM(CASE WHEN a.status = 'Có mặt' THEN 1 ELSE 0 END) as present_count, " + // Đảm bảo giá trị 'Có mặt' đúng với DB
                "COUNT(a.attendance_id) as total_attendance " +
                "FROM attendance a " +
                "JOIN class_sessions cs ON a.session_id = cs.session_id " +
                "WHERE cs.session_date BETWEEN ? AND ?";
        LOGGER.info("DAO: Attendance Percentage SQL Query: " + query);


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            LOGGER.info("DAO: Database connection obtained for attendance percentage.");

            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);
            LOGGER.info("DAO: Setting parameters for attendance percentage: " + fromDateStr + ", " + toDateStr);

            LOGGER.info("DAO: Executing attendance percentage query...");
            try (ResultSet rs = stmt.executeQuery()) {
                LOGGER.info("DAO: Attendance percentage query executed.");
                if (rs.next()) {
                    int presentCount = rs.getInt("present_count");
                    int totalAttendance = rs.getInt("total_attendance");
                    LOGGER.info("DAO: Attendance - presentCount: " + presentCount + ", totalAttendance: " + totalAttendance);
                    double percentage = totalAttendance > 0 ? (presentCount / (double) totalAttendance) * 100 : 0;
                    LOGGER.info("DAO: Calculated attendance percentage: " + percentage);
                    return percentage;
                } else {
                    LOGGER.info("DAO: No data found for attendance percentage calculation.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException calculating attendance percentage.", e);
            System.err.println("DEBUG SYSOUT: SQLException in getAttendancePercentage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DAO: Unexpected error calculating attendance percentage.", e);
            System.err.println("DEBUG SYSOUT: Exception in getAttendancePercentage: " + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.info("DAO: Returning 0 for attendance percentage due to error or no data.");
        return 0;
    }

    /**
     * Gets the homework completion statistics from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate Ending date for the report period
     * @return Percentage of homework completion (completed records / total records)
     */
    public double getHomeworkPercentage(LocalDate fromDate, LocalDate toDate) {
        System.out.println("DEBUG SYSOUT: ReportDAO.getHomeworkPercentage called.");
        LOGGER.info("DAO: getHomeworkPercentage called with fromDate: " + fromDate + ", toDate: " + toDate);
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Attempted to get homework percentage with null dates.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String toDateStr = toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LOGGER.info("DAO: Formatted dates for homework percentage: " + fromDateStr + ", " + toDateStr);

        String query = "SELECT " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_count, " +
                "COUNT(h.homework_id) as total_homework " +
                "FROM homework h " +
                "WHERE h.assigned_date BETWEEN ? AND ?";
        LOGGER.info("DAO: Homework Percentage SQL Query: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            LOGGER.info("DAO: Database connection obtained for homework percentage.");

            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);
            LOGGER.info("DAO: Setting parameters for homework percentage: " + fromDateStr + ", " + toDateStr);

            LOGGER.info("DAO: Executing homework percentage query...");
            try (ResultSet rs = stmt.executeQuery()) {
                LOGGER.info("DAO: Homework percentage query executed.");
                if (rs.next()) {
                    int completedCount = rs.getInt("completed_count");
                    int totalHomework = rs.getInt("total_homework");
                    LOGGER.info("DAO: Homework - completedCount: " + completedCount + ", totalHomework: " + totalHomework);
                    double percentage = totalHomework > 0 ? (completedCount / (double) totalHomework) * 100 : 0;
                    LOGGER.info("DAO: Calculated homework percentage: " + percentage);
                    return percentage;
                } else {
                    LOGGER.info("DAO: No data found for homework percentage calculation.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException calculating homework percentage.", e);
            System.err.println("DEBUG SYSOUT: SQLException in getHomeworkPercentage: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DAO: Unexpected error calculating homework percentage.", e);
            System.err.println("DEBUG SYSOUT: Exception in getHomeworkPercentage: " + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.info("DAO: Returning 0 for homework percentage due to error or no data.");
        return 0;
    }

    /**
     * Gets the list of distinct class statuses from the database.
     * Renamed from getClassStatuses to match ReportModel's expectation.
     *
     * @return List of class status values
     */
    public List<String> getDistinctClassStatuses() { // ĐỔI TÊN PHƯƠNG THỨC TẠI ĐÂY
        System.out.println("DEBUG SYSOUT: ReportDAO.getDistinctClassStatuses called.");
        LOGGER.info("DAO: getDistinctClassStatuses called.");
        List<String> statuses = new ArrayList<>();
        // "Tất cả" không nên được thêm cố định ở đây nếu logic trong ReportModel đã xử lý việc thêm nó.
        // Hoặc nếu thêm ở đây, thì ReportModel không cần thêm nữa.
        // Để nhất quán, không thêm "Tất cả" ở DAO, để Model quản lý danh sách filter trên UI.
        // statuses.add("Tất cả"); // Tạm thời comment lại, để Model xử lý

        String query = "SELECT DISTINCT status FROM classes WHERE status IS NOT NULL AND status != '' ORDER BY status";
        LOGGER.info("DAO: Class Statuses SQL Query: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            LOGGER.info("DAO: Database connection obtained for class statuses. Query executed.");
            while (rs.next()) {
                String statusValue = rs.getString("status");
                if (statusValue != null && !statusValue.trim().isEmpty()) { // Kiểm tra lại để chắc chắn
                    statuses.add(statusValue);
                    LOGGER.info("DAO: Added status: " + statusValue);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException retrieving class statuses.", e);
            System.err.println("DEBUG SYSOUT: SQLException in getDistinctClassStatuses: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DAO: Unexpected error retrieving class statuses.", e);
            System.err.println("DEBUG SYSOUT: Exception in getDistinctClassStatuses: " + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.info("DAO: getDistinctClassStatuses finished. Returning " + statuses.size() + " statuses.");
        System.out.println("DEBUG SYSOUT: ReportDAO.getDistinctClassStatuses finished. Returning " + statuses.size() + " statuses.");
        return statuses;
    }
}

