package src.dao;

import src.model.homework.Homework;
import utils.DatabaseConnection; // Giả sử bạn có lớp này


import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeworkDAO {

    private static final Logger LOGGER = Logger.getLogger(HomeworkDAO.class.getName());

    // Tên bảng và các cột, nên định nghĩa là hằng số để tránh lỗi chính tả
    private static final String TABLE_NAME = "homework";
    private static final String COLUMN_HOMEWORK_ID = "homework_id";
    private static final String COLUMN_CLASS_ID = "class_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_ASSIGNED_IN_SESSION_ID = "assigned_in_session_id";
    private static final String COLUMN_DUE_DATE = "due_date";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";


    public HomeworkDAO() {
        // Constructor có thể trống hoặc thực hiện khởi tạo nếu cần
    }

    private Homework mapResultSetToHomework(ResultSet rs) throws SQLException {
        Homework homework = new Homework();
        homework.setHomeworkId(rs.getString(COLUMN_HOMEWORK_ID));
        homework.setClassId(rs.getString(COLUMN_CLASS_ID));
        homework.setTitle(rs.getString(COLUMN_TITLE));
        homework.setDescription(rs.getString(COLUMN_DESCRIPTION));
        homework.setAssignedInSessionId(rs.getString(COLUMN_ASSIGNED_IN_SESSION_ID));

        Timestamp dueDateTimestamp = rs.getTimestamp(COLUMN_DUE_DATE);
        if (dueDateTimestamp != null) {
            homework.setDueDate(dueDateTimestamp.toLocalDateTime());
        }

        Timestamp createdAtTimestamp = rs.getTimestamp(COLUMN_CREATED_AT);
        if (createdAtTimestamp != null) {
            homework.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        } else {
            // Hoặc đặt một giá trị mặc định nếu DB không tự động set
            homework.setCreatedAt(LocalDateTime.now());
        }


        Timestamp updatedAtTimestamp = rs.getTimestamp(COLUMN_UPDATED_AT);
        if (updatedAtTimestamp != null) {
            homework.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
        } else {
            homework.setUpdatedAt(LocalDateTime.now());
        }

        return homework;
    }

    /**
     * Tìm bài tập theo ID.
     * @param homeworkId ID của bài tập cần tìm.
     * @return Optional chứa Homework nếu tìm thấy, ngược lại là Optional rỗng.
     * @throws SQLException Nếu có lỗi truy vấn CSDL.
     */
    public Optional<Homework> findById(String homeworkId) throws SQLException {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, homeworkId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHomework(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm tất cả các bài tập được giao trong một buổi học cụ thể.
     * Đây là phương thức quan trọng mà ClassroomAttendanceController sử dụng.
     * @param sessionId ID của buổi học.
     * @return Danh sách các Homework được giao trong buổi học đó.
     * @throws SQLException Nếu có lỗi truy vấn CSDL.
     */
    public List<Homework> findByAssignedSessionId(String sessionId) throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ASSIGNED_IN_SESSION_ID + " = ? ORDER BY " + COLUMN_CREATED_AT + " DESC"; // Sắp xếp theo ngày tạo mới nhất lên trước
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homeworkList.add(mapResultSetToHomework(rs));
                }
            }
        }
        return homeworkList;
    }

    /**
     * Tìm tất cả các bài tập của một lớp học cụ thể.
     * @param classId ID của lớp học.
     * @return Danh sách các Homework thuộc lớp đó.
     * @throws SQLException Nếu có lỗi truy vấn CSDL.
     */
    public List<Homework> findByClassId(String classId) throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_CLASS_ID + " = ? ORDER BY " + COLUMN_DUE_DATE + " ASC, " + COLUMN_CREATED_AT + " DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, classId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    homeworkList.add(mapResultSetToHomework(rs));
                }
            }
        }
        return homeworkList;
    }

    /**
     * Lấy tất cả bài tập trong CSDL.
     * @return Danh sách tất cả Homework.
     * @throws SQLException Nếu có lỗi truy vấn CSDL.
     */
    public List<Homework> findAll() throws SQLException {
        List<Homework> homeworkList = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_CLASS_ID + ", " + COLUMN_DUE_DATE + " ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                homeworkList.add(mapResultSetToHomework(rs));
            }
        }
        return homeworkList;
    }

    /**
     * Lưu một bài tập mới vào CSDL.
     * @param homework Đối tượng Homework cần lưu.
     * @return true nếu lưu thành công, false nếu thất bại.
     * @throws SQLException Nếu có lỗi CSDL.
     */
    public boolean save(Homework homework) throws SQLException {
        String sql = "INSERT INTO " + TABLE_NAME + " (" +
                COLUMN_HOMEWORK_ID + ", " + COLUMN_CLASS_ID + ", " + COLUMN_TITLE + ", " +
                COLUMN_DESCRIPTION + ", " + COLUMN_ASSIGNED_IN_SESSION_ID + ", " +
                COLUMN_DUE_DATE + ", " + COLUMN_CREATED_AT + ", " + COLUMN_UPDATED_AT + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            homework.setCreatedAt(LocalDateTime.now()); // Đảm bảo createdAt được đặt
            homework.setUpdatedAt(LocalDateTime.now()); // Đảm bảo updatedAt được đặt

            pstmt.setString(1, homework.getHomeworkId());
            pstmt.setString(2, homework.getClassId());
            pstmt.setString(3, homework.getTitle());
            pstmt.setString(4, homework.getDescription());
            pstmt.setString(5, homework.getAssignedInSessionId());
            pstmt.setTimestamp(6, homework.getDueDate() != null ? Timestamp.valueOf(homework.getDueDate()) : null);
            pstmt.setTimestamp(7, Timestamp.valueOf(homework.getCreatedAt()));
            pstmt.setTimestamp(8, Timestamp.valueOf(homework.getUpdatedAt()));

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Cập nhật thông tin một bài tập đã có trong CSDL.
     * @param homework Đối tượng Homework với thông tin đã cập nhật.
     * @return true nếu cập nhật thành công, false nếu thất bại (ví dụ: không tìm thấy homeworkId).
     * @throws SQLException Nếu có lỗi CSDL.
     */
    public boolean update(Homework homework) throws SQLException {
        String sql = "UPDATE " + TABLE_NAME + " SET " +
                COLUMN_CLASS_ID + " = ?, " +
                COLUMN_TITLE + " = ?, " +
                COLUMN_DESCRIPTION + " = ?, " +
                COLUMN_ASSIGNED_IN_SESSION_ID + " = ?, " +
                COLUMN_DUE_DATE + " = ?, " +
                COLUMN_UPDATED_AT + " = ? " +
                "WHERE " + COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            homework.setUpdatedAt(LocalDateTime.now()); // Cập nhật thời gian sửa đổi

            pstmt.setString(1, homework.getClassId());
            pstmt.setString(2, homework.getTitle());
            pstmt.setString(3, homework.getDescription());
            pstmt.setString(4, homework.getAssignedInSessionId());
            pstmt.setTimestamp(5, homework.getDueDate() != null ? Timestamp.valueOf(homework.getDueDate()) : null);
            pstmt.setTimestamp(6, Timestamp.valueOf(homework.getUpdatedAt()));
            pstmt.setString(7, homework.getHomeworkId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Xóa một bài tập khỏi CSDL dựa trên ID.
     * @param homeworkId ID của bài tập cần xóa.
     * @return true nếu xóa thành công, false nếu thất bại.
     * @throws SQLException Nếu có lỗi CSDL.
     */
    public boolean delete(String homeworkId) throws SQLException {
        // Cân nhắc: Khi xóa homework, có thể cần xóa các student_homework_submissions liên quan
        // hoặc đặt foreign key constraint với ON DELETE CASCADE/SET NULL.
        // Hiện tại, phương thức này chỉ xóa bản ghi trong bảng homework.
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_HOMEWORK_ID + " = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, homeworkId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Lưu dữ liệu điểm chuyên cần và đúng giờ vào bảng student_metrics
     * @param studentId ID của sinh viên
     * @param classId ID của lớp học
     * @param recordDate Ngày ghi nhận
     * @param awarenessScore Điểm chuyên cần
     * @param punctualityScore Điểm đúng giờ
     * @param notes Ghi chú
     * @return true nếu lưu thành công, false nếu thất bại
     * @throws SQLException nếu có lỗi xảy ra
     */
    public boolean saveStudentMetrics(String studentId, String classId, Date recordDate,
                                      double awarenessScore, double punctualityScore, String notes) throws SQLException {

        // Tạo ID cho bản ghi mới
        String metricId = UUID.randomUUID().toString();

        String sql = "INSERT INTO student_metrics (metric_id, student_id, class_id, record_date, " +
                "awareness_score, punctuality_score, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "awareness_score = VALUES(awareness_score), " +
                "punctuality_score = VALUES(punctuality_score), " +
                "notes = VALUES(notes)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, metricId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, classId);
            pstmt.setDate(4, recordDate);
            pstmt.setDouble(5, awarenessScore);
            pstmt.setDouble(6, punctualityScore);
            pstmt.setString(7, notes);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Lưu nhiều bản ghi dữ liệu điểm chuyên cần và đúng giờ trong một transaction
     * @param metricsData Danh sách các đối tượng chứa dữ liệu cần lưu
     * @return Số lượng bản ghi được lưu thành công
     * @throws SQLException nếu có lỗi xảy ra
     */
    /**
     * Lưu thông tin metrics của học sinh theo lô (batch)
     * @param metricsDataList Danh sách thông tin metrics cần lưu
     * @return Số lượng bản ghi đã được lưu
     */
    public int saveStudentMetricsBatch(List<Map<String, Object>> metricsDataList) {
        if (metricsDataList.isEmpty()) {
            return 0;
        }

        String insertQuery = "INSERT INTO student_metrics (student_id, class_id, record_date, awareness_score, " +
                "punctuality_score, notes) VALUES (?, ?, ?, ?, ?, ?)";

        String updateQuery = "UPDATE student_metrics SET record_date = ?, awareness_score = ?, " +
                "punctuality_score = ?, notes = ? WHERE metric_id = ?";

        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

            for (Map<String, Object> data : metricsDataList) {
                // Kiểm tra xem có metric_id không
                if (data.containsKey("metricId")) {
                    // Cập nhật bản ghi hiện có
                    updateStmt.setDate(1, (java.sql.Date) data.get("recordDate"));
                    updateStmt.setInt(2, (Integer) data.get("awarenessScore"));
                    updateStmt.setInt(3, (Integer) data.get("punctualityScore"));
                    updateStmt.setString(4, (String) data.get("notes"));
                    updateStmt.setLong(5, (Long) data.get("metricId"));
                    updateStmt.addBatch();
                } else {
                    // Thêm bản ghi mới
                    insertStmt.setString(1, (String) data.get("studentId"));
                    insertStmt.setString(2, (String) data.get("classId"));
                    insertStmt.setDate(3, (java.sql.Date) data.get("recordDate"));
                    insertStmt.setInt(4, (Integer) data.get("awarenessScore"));
                    insertStmt.setInt(5, (Integer) data.get("punctualityScore"));
                    insertStmt.setString(6, (String) data.get("notes"));
                    insertStmt.addBatch();
                }
            }

            // Thực thi các batch
            int[] insertCounts = insertStmt.executeBatch();
            int[] updateCounts = updateStmt.executeBatch();

            // Tính tổng số bản ghi đã xử lý
            for (int i : insertCounts) count += i;
            for (int i : updateCounts) count += i;

            return count;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving student metrics batch", e);
            return 0;
        }
    }

    /**
     * Cập nhật dữ liệu điểm chuyên cần và đúng giờ cho một sinh viên
     * @param metricId ID của bản ghi cần cập nhật
     * @param awarenessScore Điểm chuyên cần mới
     * @param punctualityScore Điểm đúng giờ mới
     * @param notes Ghi chú mới
     * @return true nếu cập nhật thành công, false nếu thất bại
     * @throws SQLException nếu có lỗi xảy ra
     */
    public boolean updateStudentMetrics(String metricId, double awarenessScore,
                                        double punctualityScore, String notes) throws SQLException {
        String sql = "UPDATE student_metrics SET awareness_score = ?, punctuality_score = ?, notes = ? " +
                "WHERE metric_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, awarenessScore);
            pstmt.setDouble(2, punctualityScore);
            pstmt.setString(3, notes);
            pstmt.setString(4, metricId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Tìm kiếm bản ghi dựa trên student_id, class_id và record_date
     * @param studentId ID của sinh viên
     * @param classId ID của lớp học
     * @param recordDate Ngày ghi nhận
     * @return Optional chứa dữ liệu nếu tìm thấy, ngược lại là Optional rỗng
     * @throws SQLException nếu có lỗi xảy ra
     */
    public Optional<Map<String, Object>> findStudentMetrics(String studentId, String classId, Date recordDate) throws SQLException {
        String sql = "SELECT * FROM student_metrics WHERE student_id = ? AND class_id = ? AND record_date = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, classId);
            pstmt.setDate(3, recordDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("metricId", rs.getString("metric_id"));
                    result.put("studentId", rs.getString("student_id"));
                    result.put("classId", rs.getString("class_id"));
                    result.put("recordDate", rs.getDate("record_date"));
                    result.put("awarenessScore", rs.getDouble("awareness_score"));
                    result.put("punctualityScore", rs.getDouble("punctuality_score"));
                    result.put("notes", rs.getString("notes"));

                    return Optional.of(result);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Lấy lịch sử điểm chuyên cần và đúng giờ của sinh viên trong một lớp học
     * @param studentId ID của sinh viên
     * @param classId ID của lớp học
     * @return Danh sách các bản ghi điểm chuyên cần và đúng giờ
     * @throws SQLException nếu có lỗi xảy ra
     */
    public List<Map<String, Object>> getStudentMetricsHistory(String studentId, String classId) throws SQLException {
        List<Map<String, Object>> metricsList = new ArrayList<>();
        String sql = "SELECT * FROM student_metrics WHERE student_id = ? AND class_id = ? ORDER BY record_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentId);
            pstmt.setString(2, classId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("metricId", rs.getString("metric_id"));
                    metrics.put("studentId", rs.getString("student_id"));
                    metrics.put("classId", rs.getString("class_id"));
                    metrics.put("recordDate", rs.getDate("record_date"));
                    metrics.put("awarenessScore", rs.getDouble("awareness_score"));
                    metrics.put("punctualityScore", rs.getDouble("punctuality_score"));
                    metrics.put("notes", rs.getString("notes"));

                    metricsList.add(metrics);
                }
            }
        }

        return metricsList;
    }

    /**
     * Lấy thông tin metrics hiện có cho các học sinh trong một lớp học
     * @param classId ID của lớp học
     * @return Danh sách các metrics dưới dạng Map
     */
    public List<Map<String, Object>> getStudentMetricsForClass(String classId) {
        List<Map<String, Object>> result = new ArrayList<>();
        String query = "SELECT metric_id, student_id FROM student_metrics WHERE class_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, classId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("metric_id", rs.getLong("metric_id"));
                record.put("student_id", rs.getString("student_id"));
                result.add(record);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting student metrics for class: " + classId, e);
        }

        return result;
    }

}