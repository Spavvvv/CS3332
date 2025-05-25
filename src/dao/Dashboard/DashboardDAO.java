package src.dao.Dashboard;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import src.model.ClassSession;
import src.model.report.ReportModel;
import src.model.system.schedule.ScheduleItem;
import src.utils.DatabaseConnection;
import src.model.system.course.Course;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
// Các import không cần thiết cho logger đã được lược bỏ nếu bạn có cấu hình chung

public class DashboardDAO {

    private static final Logger LOGGER = Logger.getLogger(DashboardDAO.class.getName());

    public DashboardDAO() {
        LOGGER.info("DAO: DashboardDAO instance created.");
    }

    public boolean addDashboardEvent(ScheduleItem scheduleItem) {
        String query = "INSERT INTO schedules (name, description, start_time, end_time, schedule_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, scheduleItem.getTitle());
            stmt.setString(2, scheduleItem.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(scheduleItem.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(scheduleItem.getEndTime()));
            stmt.setString(5, "event");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding dashboard event: " + (scheduleItem != null ? scheduleItem.getTitle() : "null event"), e);
            return false;
        }
    }

    public List<ClassSession> getTodayClasses() {
        String query = "SELECT cs.session_id, cs.course_name, cs.teacher_name, cs.room, cs.session_date, " +
                "cs.start_time, cs.end_time, cs.course_id as actual_course_id, cs.session_number " +
                "FROM class_sessions cs " +
                "WHERE cs.session_date = ?";

        List<ClassSession> classes = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sessionId = rs.getString("session_id");
                    String courseNameFromSession = rs.getString("course_name");
                    String teacherNameFromSession = rs.getString("teacher_name");
                    String roomFromSession = rs.getString("room");
                    Date dateDb = rs.getDate("session_date");
                    LocalDate sessionDate = (dateDb != null) ? dateDb.toLocalDate() : null;
                    Timestamp startTimeDbTs = rs.getTimestamp("start_time");
                    LocalDateTime sessionStartTime = (startTimeDbTs != null) ? startTimeDbTs.toLocalDateTime() : null;
                    Timestamp endTimeDbTs = rs.getTimestamp("end_time");
                    LocalDateTime sessionEndTime = (endTimeDbTs != null) ? endTimeDbTs.toLocalDateTime() : null;
                    String actualCourseId = rs.getString("actual_course_id");
                    int sessionNumber = rs.getInt("session_number");

                    Course courseForSession = new Course();
                    courseForSession.setCourseId(actualCourseId);
                    courseForSession.setCourseName(courseNameFromSession);


                    ClassSession classSession = new ClassSession(sessionId, courseForSession, teacherNameFromSession, roomFromSession,
                            sessionDate, sessionStartTime, sessionEndTime,
                            actualCourseId, sessionNumber);
                    classes.add(classSession);

                    //DEBUG
                    //System.out.println("Course_Name: "+ classSession.getCourse().getCourseName());
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving today's class sessions.", e);
        }
        return classes;
    }

    public int getTotalStudents() {
        // Giả sử bảng students có cột status để chỉ sinh viên đang hoạt động
        String query = "SELECT COUNT(*) FROM students WHERE status = 'active'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total active students.", e);
        }
        return 0;
    }

    /**
     * Gets the total number of courses in the system.
     *
     * @return The total number of courses
     */
    public int getTotalCourses() { // Đổi tên phương thức cho rõ ràng
        // SỬA ĐỔI: Đếm tất cả các khóa học từ bảng 'courses'
        String query = "SELECT COUNT(*) FROM courses";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total courses.", e);
        }
        return 0;
    }

    public double getAttendanceRate() {
        String query = "SELECT " +
                "SUM(CASE WHEN status = 'Có mặt' THEN 1 ELSE 0 END) AS present_count, " +
                "COUNT(*) AS total_count FROM attendance";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                int presentCount = rs.getInt("present_count");
                int totalCount = rs.getInt("total_count");
                if (totalCount > 0) {
                    return ((double) presentCount / totalCount) * 100.0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting attendance rate.", e);
        }
        return 0.0;
    }

    /**
     * Retrieves class report data from the database
     *
     * @param fromDate Starting date for the report period
     * @param toDate   Ending date for the report period
     * @return List of ClassReportData objects
     */
    public List<ReportModel.ClassReportData> getClassReportData(LocalDate fromDate, LocalDate toDate) { // Bỏ tham số statusFilter
        System.out.println("DEBUG SYSOUT: ReportDAO.getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate);
        LOGGER.info("DAO: getClassReportData called with fromDate: " + fromDate + ", toDate: " + toDate);
        List<ReportModel.ClassReportData> reportData = new ArrayList<>();

        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Attempted to get class report data with null dates. Returning empty list.");
            return reportData;
        }

        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = toDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        LOGGER.info("DAO: Formatted fromDateStr: " + fromDateStr + ", toDateStr: " + toDateStr);

        // SỬA ĐỔI: Bỏ statusCondition và các logic liên quan đến status của course
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT c.course_id, c.course_name, ")
                .append("COUNT(DISTINCT cs.session_id) as total_sessions_held, ")
                .append("c.total_sessions as total_course_sessions, ")
                .append("SUM(CASE WHEN a.present = 1 THEN 1 ELSE 0 END) as present_count, ")
                .append("COUNT(DISTINCT a.attendance_id) as total_attendance_records, ")
                .append("COUNT(DISTINCT h.homework_id) as total_homework_assigned, ")
                .append("SUM(CASE WHEN shs.is_submitted = 1 THEN 1 ELSE 0 END) as submitted_homework_count, ")
                .append("AVG(sm.awareness_score) as awareness, ")
                .append("AVG(sm.punctuality_score) as punctuality, ")
                .append("AVG(shs.grade) as average_homework_grade, ")
                .append("COUNT(DISTINCT e.student_id) as student_count ")
                .append("FROM courses c ") // Đã đổi sang courses
                .append("LEFT JOIN class_sessions cs ON c.course_id = cs.course_id AND cs.session_date BETWEEN ? AND ? ")
                .append("LEFT JOIN attendance a ON cs.session_id = a.session_id ")
                .append("LEFT JOIN homework h ON c.course_id = h.course_id AND h.assigned_date BETWEEN ? AND ? ")
                .append("LEFT JOIN student_homework_submissions shs ON h.homework_id = shs.homework_id ")
                .append("LEFT JOIN student_metrics sm ON c.course_id = sm.course_id AND sm.record_date BETWEEN ? AND ? ")
                .append("LEFT JOIN enrollment e ON c.course_id = e.course_id ");
        // KHÔNG CÒN WHERE c.status = ?

        List<Object> parameters = new ArrayList<>();
        parameters.add(fromDateStr);
        parameters.add(toDateStr);
        parameters.add(fromDateStr);
        parameters.add(toDateStr);
        parameters.add(fromDateStr);
        parameters.add(toDateStr);
        // Không còn tham số cho status

        queryBuilder.append("GROUP BY c.course_id, c.course_name, c.total_sessions ")
                .append("ORDER BY c.course_name");

        String query = queryBuilder.toString();
        LOGGER.info("DAO: SQL Query: " + query);
        LOGGER.info("DAO: Query parameters: " + parameters);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int counter = 1;
                while (rs.next()) {
                    String courseName = rs.getString("course_name");
                    int studentCount = rs.getInt("student_count");
                    int sessionsHeldInPeriod = rs.getInt("total_sessions_held");
                    int presentCountRaw = rs.getInt("present_count");
                    int totalPossibleAttendanceInPeriod = studentCount * sessionsHeldInPeriod;
                    String attendanceStr = presentCountRaw + "/" + (totalPossibleAttendanceInPeriod > 0 ? totalPossibleAttendanceInPeriod : sessionsHeldInPeriod);

                    int totalHomeworkAssignedInPeriod = rs.getInt("total_homework_assigned");
                    int submittedHomeworkCountRaw = rs.getInt("submitted_homework_count");
                    int totalHomeworkSubmissionsPossible = studentCount * totalHomeworkAssignedInPeriod;
                    String homeworkStr = submittedHomeworkCountRaw + "/" + (totalHomeworkSubmissionsPossible > 0 ? totalHomeworkSubmissionsPossible : totalHomeworkAssignedInPeriod);

                    double avgHomeworkGrade = rs.getDouble("average_homework_grade");
                    if (rs.wasNull()) avgHomeworkGrade = 0.0;
                    String formattedScore = String.format("%.2f/10", avgHomeworkGrade);

                    double awareness = rs.getDouble("awareness");
                    if (rs.wasNull()) awareness = 0.0;
                    double punctuality = rs.getDouble("punctuality");
                    if (rs.wasNull()) punctuality = 0.0;

                    ReportModel.ClassReportData data = new ReportModel.ClassReportData(
                            counter++,
                            courseName,
                            attendanceStr,
                            homeworkStr,
                            awareness,
                            punctuality,
                            formattedScore
                    );
                    reportData.add(data);
                }
                if (counter == 1 && reportData.isEmpty()) { // Kiểm tra thêm reportData.isEmpty() cho chắc
                    LOGGER.info("DAO: ResultSet was empty. No data rows found for the given criteria.");
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


    public double getHomeworkPercentage(LocalDate fromDate, LocalDate toDate) {
        LOGGER.info("DAO: getHomeworkPercentage called with fromDate: " + fromDate + ", toDate: " + toDate);
        if (fromDate == null || toDate == null) {
            LOGGER.log(Level.WARNING, "DAO: Null dates for homework percentage.");
            return 0;
        }
        String fromDateStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = toDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        String query = "SELECT " +
                "SUM(CASE WHEN h.status = 'completed' THEN 1 ELSE 0 END) as completed_count, " +
                "COUNT(h.homework_id) as total_homework_assigned " +
                "FROM homework h " +
                "WHERE h.assigned_date BETWEEN ? AND ?";
        LOGGER.info("DAO: Homework Percentage SQL Query: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fromDateStr);
            stmt.setString(2, toDateStr);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int completedCount = rs.getInt("completed_count");
                    int totalHomeworkAssigned = rs.getInt("total_homework_assigned");
                    double percentage = totalHomeworkAssigned > 0 ? (completedCount / (double) totalHomeworkAssigned) * 100 : 0;
                    LOGGER.info("DAO: Homework - completedAssignments: " + completedCount + ", totalAssignments: " + totalHomeworkAssigned + ", percentage: " + percentage);
                    return percentage;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DAO: SQLException calculating homework percentage.", e);
            e.printStackTrace();
        }
        return 0;
    }

    // SỬA ĐỔI: Xóa phương thức getDistinctClassStatuses vì bảng courses không có cột status
    // public List<String> getDistinctClassStatuses() { ... }


    public ObservableList<PieChart.Data> getCourseDistribution() {
        String query = "SELECT c.course_name, COUNT(cs.session_id) as session_count " +
                "FROM courses c " +
                "LEFT JOIN class_sessions cs ON c.course_id = cs.course_id " +
                "GROUP BY c.course_name " +
                "HAVING COUNT(cs.session_id) > 0";

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String courseName = rs.getString("course_name");
                int sessionCount = rs.getInt("session_count");
                pieChartData.add(new PieChart.Data(courseName, sessionCount));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting course distribution data.", e);
        }
        return pieChartData;
    }

    public List<Object[]> getUpcomingSchedulesData(int limit) {
        String query = "SELECT id, name, description, start_time, end_time, schedule_type " +
                "FROM schedules " +
                "WHERE start_time >= CURRENT_TIMESTAMP " +
                "ORDER BY start_time ASC " +
                "LIMIT ?";

        List<Object[]> scheduleData = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[6];
                    row[0] = rs.getString("id");
                    row[1] = rs.getString("name");
                    row[2] = rs.getString("description");
                    Timestamp startTs = rs.getTimestamp("start_time");
                    row[3] = (startTs != null) ? startTs.toLocalDateTime() : null;
                    Timestamp endTs = rs.getTimestamp("end_time");
                    row[4] = (endTs != null) ? endTs.toLocalDateTime() : null;
                    row[5] = rs.getString("schedule_type");
                    scheduleData.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting upcoming schedules data.", e);
        }
        return scheduleData;
    }

    public int getTotalClasses() {
        String query = "SELECT COUNT(*) FROM courses";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total classes.", e);
        }
        return 0;
    }
}