package src.dao.Attendance;

import src.model.homework.Homework; // Đảm bảo đường dẫn và tên lớp Model Homework là chính xác
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeworkDAO {

    private static final Logger LOGGER = Logger.getLogger(HomeworkDAO.class.getName());

    // Constants cho bảng 'homework'
    private static final String HOMEWORK_TABLE_NAME = "homework";
    private static final String HW_COLUMN_HOMEWORK_ID = "homework_id";
    private static final String HW_COLUMN_COURSE_ID = "course_id";
    private static final String HW_COLUMN_TITLE = "title";
    private static final String HW_COLUMN_DESCRIPTION = "description";
    private static final String HW_COLUMN_ASSIGNED_DATE = "assigned_date";
    private static final String HW_COLUMN_STATUS = "status";
    private static final String HW_COLUMN_SCORE = "score";
    private static final String HW_COLUMN_SUBMISSION_DATE = "submission_date";
    private static final String HW_COLUMN_ASSIGNED_IN_SESSION_ID = "assigned_in_session_id";

    // Constants cho bảng 'student_metrics'
    private static final String METRICS_TABLE_NAME = "student_metrics";
    private static final String METRICS_COLUMN_METRIC_ID = "metric_id";
    private static final String METRICS_COLUMN_STUDENT_ID = "student_id";
    private static final String METRICS_COLUMN_COURSE_ID = "course_id"; // Cột này trong student_metrics
    private static final String METRICS_COLUMN_RECORD_DATE = "record_date";
    private static final String METRICS_COLUMN_AWARENESS_SCORE = "awareness_score";
    private static final String METRICS_COLUMN_PUNCTUALITY_SCORE = "punctuality_score";
    private static final String METRICS_COLUMN_NOTES = "notes";

    public HomeworkDAO() {
        // Constructor
    }

    // =======================================================================================
    // PHƯƠNG THỨC LIÊN QUAN ĐẾN BẢNG 'homework'
    // =======================================================================================

    private Homework mapResultSetToHomeworkModel(ResultSet rs) throws SQLException {
        Homework homework = new Homework();
        homework.setHomeworkId(rs.getString(HW_COLUMN_HOMEWORK_ID));
        homework.setCourseId(rs.getString(HW_COLUMN_COURSE_ID));
        homework.setTitle(rs.getString(HW_COLUMN_TITLE));
        homework.setDescription(rs.getString(HW_COLUMN_DESCRIPTION));

        java.sql.Date assignedDateSql = rs.getDate(HW_COLUMN_ASSIGNED_DATE);
        if (assignedDateSql != null) {
            homework.setAssignedDate(assignedDateSql.toLocalDate());
        }

        homework.setStatus(rs.getString(HW_COLUMN_STATUS));

        double scoreDb = rs.getDouble(HW_COLUMN_SCORE);
        if (rs.wasNull()) {
            homework.setScore(null);
        } else {
            homework.setScore(scoreDb);
        }

        Timestamp submissionDateTimestamp = rs.getTimestamp(HW_COLUMN_SUBMISSION_DATE);
        if (submissionDateTimestamp != null) {
            homework.setSubmissionDate(submissionDateTimestamp.toLocalDateTime());
        }

        homework.setAssignedInSessionId(rs.getString(HW_COLUMN_ASSIGNED_IN_SESSION_ID));
        return homework;
    }

    /**
     * Tạo một bản ghi homework mới.
     * @param homework Đối tượng Homework cần tạo.
     * @return true nếu tạo thành công, false ngược lại.
     * @throws SQLException
     */
    public boolean create(Homework homework) throws SQLException {
        if (homework.getHomeworkId() == null || homework.getHomeworkId().isEmpty()) {
            homework.setHomeworkId(UUID.randomUUID().toString());
        }

        String sql = "INSERT INTO " + HOMEWORK_TABLE_NAME + " (" +
                HW_COLUMN_HOMEWORK_ID + ", " + HW_COLUMN_COURSE_ID + ", " + HW_COLUMN_TITLE + ", " +
                HW_COLUMN_DESCRIPTION + ", " + HW_COLUMN_ASSIGNED_DATE + ", " + HW_COLUMN_STATUS + ", " +
                HW_COLUMN_SCORE + ", " + HW_COLUMN_SUBMISSION_DATE + ", " + HW_COLUMN_ASSIGNED_IN_SESSION_ID +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setString(paramIndex++, homework.getHomeworkId());
            pstmt.setString(paramIndex++, homework.getCourseId());
            pstmt.setString(paramIndex++, homework.getTitle());
            pstmt.setString(paramIndex++, homework.getDescription());
            pstmt.setDate(paramIndex++, homework.getAssignedDate() != null ? java.sql.Date.valueOf(homework.getAssignedDate()) : null);
            pstmt.setString(paramIndex++, homework.getStatus());
            if (homework.getScore() != null) {
                pstmt.setDouble(paramIndex++, homework.getScore());
            } else {
                pstmt.setNull(paramIndex++, Types.DOUBLE);
            }
            pstmt.setTimestamp(paramIndex++, homework.getSubmissionDate() != null ? Timestamp.valueOf(homework.getSubmissionDate()) : null);
            pstmt.setString(paramIndex++, homework.getAssignedInSessionId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Lấy hoặc tạo một bản ghi homework "placeholder" cho một buổi học cụ thể.
     * homework_id trong bảng homework được giả định là chính sessionId.
     *
     * @param sessionId ID của buổi học, cũng được dùng làm homework_id cho bản ghi homework.
     * @param title Tiêu đề cho bài tập (ví dụ: "Bài tập buổi [sessionId]").
     * @param courseId ID của khóa học (bắt buộc).
     * @param assignedDate Ngày giao bài.
     * @return HomeworkModel đã tồn tại hoặc vừa được tạo.
     * @throws SQLException Nếu có lỗi cơ sở dữ liệu hoặc không thể tạo/lấy bài tập.
     */
    public Homework getOrCreateHomeworkForSession(String sessionId, String title, String courseId, LocalDate assignedDate) throws SQLException {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty to get or create homework.");
        }
        if (courseId == null || courseId.isEmpty()) {
            throw new IllegalArgumentException("Course ID cannot be null or empty for homework.");
        }

        String homeworkIdForThisSession = sessionId;
        Optional<Homework> existingOpt = findById(homeworkIdForThisSession);

        if (existingOpt.isPresent()) {
            LOGGER.log(Level.INFO, "Found existing homework with ID (based on session ID): {0}", homeworkIdForThisSession);
            return existingOpt.get();
        } else {
            LOGGER.log(Level.INFO, "No existing homework found for ID (based on session ID): {0}. Creating new placeholder homework.", homeworkIdForThisSession);
            Homework newHomework = new Homework();
            newHomework.setHomeworkId(homeworkIdForThisSession);
            newHomework.setTitle(title);
            newHomework.setDescription("Bài tập được tạo tự động cho buổi học " + sessionId); // Default description
            newHomework.setCourseId(courseId);
            newHomework.setAssignedDate(assignedDate);
            newHomework.setAssignedInSessionId(sessionId);
            newHomework.setStatus("Assigned"); // Default status
            newHomework.setScore(null);
            newHomework.setSubmissionDate(null);

            if (this.create(newHomework)) {
                LOGGER.log(Level.INFO, "Successfully created placeholder homework with ID: {0}", homeworkIdForThisSession);
                return newHomework;
            } else {
                String errorMsg = "Failed to create placeholder homework for session with ID: " + homeworkIdForThisSession;
                LOGGER.log(Level.SEVERE, errorMsg);
                throw new SQLException(errorMsg);
            }
        }
    }

    /**
     * Tìm bài tập theo ID.
     * @param homeworkId ID của bài tập cần tìm.
     * @return Optional chứa Homework nếu tìm thấy, ngược lại là Optional rỗng.
     */
    public Optional<Homework> findById(String homeworkId) throws SQLException {
        String sql = "SELECT * FROM " + HOMEWORK_TABLE_NAME + " WHERE " + HW_COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, homeworkId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHomeworkModel(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm tất cả các bài tập được giao trong một buổi học cụ thể.
     * @param sessionId ID của buổi học.
     * @return Danh sách các Homework được giao trong buổi học đó.
     */
    public List<Homework> findByAssignedSessionId(String sessionId) throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + HOMEWORK_TABLE_NAME + " WHERE " + HW_COLUMN_ASSIGNED_IN_SESSION_ID + " = ? ORDER BY " + HW_COLUMN_ASSIGNED_DATE + " DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homeworkList.add(mapResultSetToHomeworkModel(rs));
                }
            }
        }
        return homeworkList;
    }

    /**
     * Tìm tất cả các bài tập của một khóa học cụ thể.
     * @param courseId ID của khóa học.
     * @return Danh sách các Homework thuộc khóa học đó.
     */
    public List<Homework> findByCourseId(String courseId) throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + HOMEWORK_TABLE_NAME + " WHERE " + HW_COLUMN_COURSE_ID + " = ? ORDER BY " + HW_COLUMN_ASSIGNED_DATE + " ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homeworkList.add(mapResultSetToHomeworkModel(rs));
                }
            }
        }
        return homeworkList;
    }

    /**
     * Lấy tất cả bài tập trong CSDL.
     * @return Danh sách tất cả Homework.
     */
    public List<Homework> findAll() throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + HOMEWORK_TABLE_NAME + " ORDER BY " + HW_COLUMN_COURSE_ID + ", " + HW_COLUMN_ASSIGNED_DATE + " ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                homeworkList.add(mapResultSetToHomeworkModel(rs));
            }
        }
        return homeworkList;
    }

    /**
     * Cập nhật thông tin một bài tập đã có trong CSDL.
     * @param homework Đối tượng Homework với thông tin đã cập nhật.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean update(Homework homework) throws SQLException {
        String sql = "UPDATE " + HOMEWORK_TABLE_NAME + " SET " +
                HW_COLUMN_COURSE_ID + " = ?, " +
                HW_COLUMN_TITLE + " = ?, " +
                HW_COLUMN_DESCRIPTION + " = ?, " +
                HW_COLUMN_ASSIGNED_DATE + " = ?, " +
                HW_COLUMN_STATUS + " = ?, " +
                HW_COLUMN_SCORE + " = ?, " +
                HW_COLUMN_SUBMISSION_DATE + " = ?, " +
                HW_COLUMN_ASSIGNED_IN_SESSION_ID + " = ? " +
                "WHERE " + HW_COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setString(paramIndex++, homework.getCourseId());
            pstmt.setString(paramIndex++, homework.getTitle());
            pstmt.setString(paramIndex++, homework.getDescription());
            pstmt.setDate(paramIndex++, homework.getAssignedDate() != null ? java.sql.Date.valueOf(homework.getAssignedDate()) : null);
            pstmt.setString(paramIndex++, homework.getStatus());
            if (homework.getScore() != null) {
                pstmt.setDouble(paramIndex++, homework.getScore());
            } else {
                pstmt.setNull(paramIndex++, Types.DOUBLE);
            }
            pstmt.setTimestamp(paramIndex++, homework.getSubmissionDate() != null ? Timestamp.valueOf(homework.getSubmissionDate()) : null);
            pstmt.setString(paramIndex++, homework.getAssignedInSessionId());
            pstmt.setString(paramIndex, homework.getHomeworkId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Xóa một bài tập khỏi CSDL dựa trên ID.
     * @param homeworkId ID của bài tập cần xóa.
     * @return true nếu xóa thành công, false nếu thất bại.
     */
    public boolean delete(String homeworkId) throws SQLException {
        String sql = "DELETE FROM " + HOMEWORK_TABLE_NAME + " WHERE " + HW_COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, homeworkId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }


    // =======================================================================================
    // CÁC PHƯƠNG THỨC LIÊN QUAN ĐẾN student_metrics
    // LƯU Ý: NÊN TÁCH RA StudentMetricsDAO RIÊNG.
    // =======================================================================================

    /**
     * Lưu dữ liệu điểm chuyên cần và đúng giờ vào bảng student_metrics.
     * Có thể tạo mới hoặc cập nhật nếu đã tồn tại (dựa vào ON DUPLICATE KEY UPDATE của MySQL).
     * Để ON DUPLICATE KEY UPDATE hoạt động như mong đợi cho việc "update",
     * bạn cần có một UNIQUE KEY trên (student_id, course_id, record_date) trong bảng student_metrics.
     * Nếu không, nó sẽ luôn INSERT vì metric_id (PK) là UUID mới.
     * @param studentId ID của sinh viên
     * @param courseId ID của khóa học
     * @param recordDate Ngày ghi nhận
     * @param awarenessScore Điểm chuyên cần (có thể null)
     * @param punctualityScore Điểm đúng giờ (có thể null)
     * @param notes Ghi chú
     * @return true nếu lưu hoặc cập nhật thành công, false nếu thất bại.
     * @throws SQLException nếu có lỗi xảy ra.
     */
    public boolean saveStudentMetrics(String studentId, String courseId, java.sql.Date recordDate,
                                      Double awarenessScore, Double punctualityScore, String notes) throws SQLException {
        String metricId = UUID.randomUUID().toString(); // metric_id cho trường hợp INSERT
        String sql = "INSERT INTO " + METRICS_TABLE_NAME + " (" +
                METRICS_COLUMN_METRIC_ID + ", " + METRICS_COLUMN_STUDENT_ID + ", " + METRICS_COLUMN_COURSE_ID + ", " +
                METRICS_COLUMN_RECORD_DATE + ", " + METRICS_COLUMN_AWARENESS_SCORE + ", " +
                METRICS_COLUMN_PUNCTUALITY_SCORE + ", " + METRICS_COLUMN_NOTES + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " + // Giả định có UNIQUE KEY (student_id, course_id, record_date)
                METRICS_COLUMN_AWARENESS_SCORE + " = VALUES(" + METRICS_COLUMN_AWARENESS_SCORE + "), " +
                METRICS_COLUMN_PUNCTUALITY_SCORE + " = VALUES(" + METRICS_COLUMN_PUNCTUALITY_SCORE + "), " +
                METRICS_COLUMN_NOTES + " = VALUES(" + METRICS_COLUMN_NOTES + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            pstmt.setString(paramIndex++, metricId);
            pstmt.setString(paramIndex++, studentId);
            pstmt.setString(paramIndex++, courseId);
            pstmt.setDate(paramIndex++, recordDate);

            if (awarenessScore != null) pstmt.setDouble(paramIndex++, awarenessScore);
            else pstmt.setNull(paramIndex++, Types.DOUBLE);

            if (punctualityScore != null) pstmt.setDouble(paramIndex++, punctualityScore);
            else pstmt.setNull(paramIndex++, Types.DOUBLE);

            pstmt.setString(paramIndex++, notes);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Lưu thông tin metrics của học sinh theo lô (batch).
     * Tạo mới nếu "metricId" không có trong map data, cập nhật nếu có.
     * @param metricsDataList Danh sách thông tin metrics cần lưu.
     * @return Số lượng bản ghi đã được xử lý thành công.
     * @throws SQLException nếu có lỗi.
     */
    public int saveStudentMetricsBatch(List<Map<String, Object>> metricsDataList) throws SQLException {
        if (metricsDataList == null || metricsDataList.isEmpty()) {
            return 0;
        }
        String insertQuery = "INSERT INTO " + METRICS_TABLE_NAME + " (" +
                METRICS_COLUMN_METRIC_ID + ", " + METRICS_COLUMN_STUDENT_ID + ", " + METRICS_COLUMN_COURSE_ID + ", " +
                METRICS_COLUMN_RECORD_DATE + ", " + METRICS_COLUMN_AWARENESS_SCORE + ", " +
                METRICS_COLUMN_PUNCTUALITY_SCORE + ", " + METRICS_COLUMN_NOTES + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        String updateQuery = "UPDATE " + METRICS_TABLE_NAME + " SET " +
                METRICS_COLUMN_RECORD_DATE + " = ?, " + METRICS_COLUMN_AWARENESS_SCORE + " = ?, " +
                METRICS_COLUMN_PUNCTUALITY_SCORE + " = ?, " + METRICS_COLUMN_NOTES + " = ? " +
                "WHERE " + METRICS_COLUMN_METRIC_ID + " = ?";
        int successCount = 0;
        Connection conn = null;
        boolean autoCommitStatus = false;
        try {
            conn = DatabaseConnection.getConnection();
            autoCommitStatus = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                 PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

                for (Map<String, Object> data : metricsDataList) {
                    Object metricIdObj = data.get("metricId"); // Key trong map của bạn

                    if (metricIdObj != null && !metricIdObj.toString().isEmpty()) {
                        // Update
                        updateStmt.clearParameters();
                        int paramIdxUpd = 1;
                        // Chuyển đổi java.util.Date sang java.sql.Date nếu cần
                        Object recordDateObj = data.get("recordDate");
                        java.sql.Date sqlRecordDate = null;
                        if (recordDateObj instanceof java.util.Date && !(recordDateObj instanceof java.sql.Date)) {
                            sqlRecordDate = new java.sql.Date(((java.util.Date) recordDateObj).getTime());
                        } else if (recordDateObj instanceof java.sql.Date) {
                            sqlRecordDate = (java.sql.Date) recordDateObj;
                        }
                        updateStmt.setDate(paramIdxUpd++, sqlRecordDate);

                        updateStmt.setObject(paramIdxUpd++, data.get("awarenessScore")); // Cho phép null
                        updateStmt.setObject(paramIdxUpd++, data.get("punctualityScore")); // Cho phép null
                        updateStmt.setString(paramIdxUpd++, (String) data.get("notes"));
                        updateStmt.setString(paramIdxUpd++, metricIdObj.toString());
                        updateStmt.addBatch();
                    } else {
                        // Insert
                        insertStmt.clearParameters();
                        String newMetricId = UUID.randomUUID().toString();
                        int paramIdxIns = 1;
                        insertStmt.setString(paramIdxIns++, newMetricId);
                        insertStmt.setString(paramIdxIns++, (String) data.get("studentId"));
                        insertStmt.setString(paramIdxIns++, (String) data.get("courseId"));

                        Object recordDateObj = data.get("recordDate");
                        java.sql.Date sqlRecordDate = null;
                        if (recordDateObj instanceof java.util.Date && !(recordDateObj instanceof java.sql.Date)) {
                            sqlRecordDate = new java.sql.Date(((java.util.Date) recordDateObj).getTime());
                        } else if (recordDateObj instanceof java.sql.Date) {
                            sqlRecordDate = (java.sql.Date) recordDateObj;
                        }
                        insertStmt.setDate(paramIdxIns++, sqlRecordDate);

                        insertStmt.setObject(paramIdxIns++, data.get("awarenessScore")); // Cho phép null
                        insertStmt.setObject(paramIdxIns++, data.get("punctualityScore")); // Cho phép null
                        insertStmt.setString(paramIdxIns++, (String) data.get("notes"));
                        insertStmt.addBatch();
                    }
                }
                int[] insertCounts = insertStmt.executeBatch();
                int[] updateCounts = updateStmt.executeBatch();
                conn.commit();

                for (int i : insertCounts) {
                    if (i >= 0 || i == PreparedStatement.SUCCESS_NO_INFO) successCount++;
                }
                for (int i : updateCounts) {
                    if (i >= 0 || i == PreparedStatement.SUCCESS_NO_INFO) successCount++;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in saveStudentMetricsBatch, attempting rollback.", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error rolling back transaction in saveStudentMetricsBatch", ex); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommitStatus);
                    if (!conn.isClosed()) { conn.close(); }
                } catch (SQLException ex) { LOGGER.log(Level.WARNING, "Error restoring auto-commit or closing connection", ex); }
            }
        }
        return successCount;
    }

    /**
     * Cập nhật dữ liệu điểm chuyên cần và đúng giờ cho một bản ghi student_metrics cụ thể.
     * @param metricId ID của bản ghi metric cần cập nhật.
     * @param awarenessScore Điểm chuyên cần mới (có thể null).
     * @param punctualityScore Điểm đúng giờ mới (có thể null).
     * @param notes Ghi chú mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     * @throws SQLException nếu có lỗi xảy ra.
     */
    public boolean updateStudentMetrics(String metricId, Double awarenessScore,
                                        Double punctualityScore, String notes) throws SQLException {
        String sql = "UPDATE " + METRICS_TABLE_NAME + " SET " +
                METRICS_COLUMN_AWARENESS_SCORE + " = ?, " +
                METRICS_COLUMN_PUNCTUALITY_SCORE + " = ?, " +
                METRICS_COLUMN_NOTES + " = ? " +
                "WHERE " + METRICS_COLUMN_METRIC_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (awarenessScore != null) pstmt.setDouble(paramIndex++, awarenessScore);
            else pstmt.setNull(paramIndex++, Types.DOUBLE);

            if (punctualityScore != null) pstmt.setDouble(paramIndex++, punctualityScore);
            else pstmt.setNull(paramIndex++, Types.DOUBLE);

            pstmt.setString(paramIndex++, notes);
            pstmt.setString(paramIndex++, metricId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }


    /**
     * Tìm kiếm một bản ghi student_metrics cụ thể.
     * @param studentId ID của sinh viên.
     * @param courseId ID của khóa học.
     * @param recordDate Ngày ghi nhận.
     * @return Optional chứa Map dữ liệu của bản ghi metric nếu tìm thấy.
     * @throws SQLException nếu có lỗi xảy ra.
     */
    public Optional<Map<String, Object>> findStudentMetrics(String studentId, String courseId, java.sql.Date recordDate) throws SQLException {
        String sql = "SELECT * FROM " + METRICS_TABLE_NAME + " WHERE " +
                METRICS_COLUMN_STUDENT_ID + " = ? AND " +
                METRICS_COLUMN_COURSE_ID + " = ? AND " +
                METRICS_COLUMN_RECORD_DATE + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setDate(3, recordDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("metricId", rs.getString(METRICS_COLUMN_METRIC_ID));
                    result.put("studentId", rs.getString(METRICS_COLUMN_STUDENT_ID));
                    result.put("courseId", rs.getString(METRICS_COLUMN_COURSE_ID));
                    result.put("recordDate", rs.getDate(METRICS_COLUMN_RECORD_DATE));
                    result.put("awarenessScore", rs.getObject(METRICS_COLUMN_AWARENESS_SCORE) != null ? rs.getDouble(METRICS_COLUMN_AWARENESS_SCORE) : null);
                    result.put("punctualityScore", rs.getObject(METRICS_COLUMN_PUNCTUALITY_SCORE) != null ? rs.getDouble(METRICS_COLUMN_PUNCTUALITY_SCORE) : null);
                    result.put("notes", rs.getString(METRICS_COLUMN_NOTES));
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy lịch sử điểm chuyên cần và đúng giờ của sinh viên trong một khóa học.
     * @param studentId ID của sinh viên.
     * @param courseId ID của khóa học.
     * @return Danh sách các Map, mỗi Map là một bản ghi student_metrics.
     * @throws SQLException nếu có lỗi xảy ra.
     */
    public List<Map<String, Object>> getStudentMetricsHistory(String studentId, String courseId) throws SQLException {
        List<Map<String, Object>> metricsList = new ArrayList<>();
        String sql = "SELECT * FROM " + METRICS_TABLE_NAME + " WHERE " +
                METRICS_COLUMN_STUDENT_ID + " = ? AND " +
                METRICS_COLUMN_COURSE_ID + " = ? ORDER BY " + METRICS_COLUMN_RECORD_DATE + " DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("metricId", rs.getString(METRICS_COLUMN_METRIC_ID));
                    metrics.put("studentId", rs.getString(METRICS_COLUMN_STUDENT_ID));
                    metrics.put("courseId", rs.getString(METRICS_COLUMN_COURSE_ID));
                    metrics.put("recordDate", rs.getDate(METRICS_COLUMN_RECORD_DATE));
                    metrics.put("awarenessScore", rs.getObject(METRICS_COLUMN_AWARENESS_SCORE) != null ? rs.getDouble(METRICS_COLUMN_AWARENESS_SCORE) : null);
                    metrics.put("punctualityScore", rs.getObject(METRICS_COLUMN_PUNCTUALITY_SCORE) != null ? rs.getDouble(METRICS_COLUMN_PUNCTUALITY_SCORE) : null);
                    metrics.put("notes", rs.getString(METRICS_COLUMN_NOTES));
                    metricsList.add(metrics);
                }
            }
        }
        return metricsList;
    }

    /**
     * Lấy thông tin metrics (chỉ metric_id và student_id) cho các học sinh trong một khóa học.
     * @param courseId ID của khóa học.
     * @return Danh sách các Map, mỗi Map chứa "metric_id" và "student_id".
     */
    public List<Map<String, Object>> getStudentMetricsForCourse(String courseId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String query = "SELECT " + METRICS_COLUMN_METRIC_ID + ", " + METRICS_COLUMN_STUDENT_ID +
                " FROM " + METRICS_TABLE_NAME + " WHERE " + METRICS_COLUMN_COURSE_ID + " = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("metric_id", rs.getString(METRICS_COLUMN_METRIC_ID));
                    record.put("student_id", rs.getString(METRICS_COLUMN_STUDENT_ID));
                    result.add(record);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student metrics for course: " + courseId, e);
        }
        return result;
    }
}