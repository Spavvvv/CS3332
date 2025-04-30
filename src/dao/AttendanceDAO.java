package src.dao;

import src.model.attendance.Attendance;
import src.model.ClassSession;
import src.model.person.Student;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Access Object cho lớp Attendance
 * Cung cấp các phương thức để tương tác với cơ sở dữ liệu
 */
public class AttendanceDAO {
    private Connection connection;

    /**
     * Constructor mặc định, khởi tạo kết nối đến database
     */
    public AttendanceDAO() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối đến cơ sở dữ liệu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Constructor với Connection có sẵn
     *
     * @param connection Kết nối đến cơ sở dữ liệu
     */
    public AttendanceDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * Lấy danh sách điểm danh theo ID buổi học
     *
     * @param sessionId ID của buổi học
     * @return Danh sách điểm danh cho buổi học đó
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public List<Attendance> getBySessionId(long sessionId) throws SQLException {
        List<Attendance> attendances = new ArrayList<>();

        String sql = "SELECT a.*, s.id as student_id, s.name as student_name, " +
                "c.id as session_id, c.class_id, c.subject_id, c.date " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions c ON a.session_id = c.id " +
                "WHERE a.session_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = mapResultSetToAttendance(rs);
                    attendances.add(attendance);
                }
            }
        }

        return attendances;
    }

    /**
     * Lấy danh sách điểm danh theo ID học sinh
     *
     * @param studentId ID của học sinh
     * @return Danh sách điểm danh của học sinh đó
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public List<Attendance> getByStudentId(long studentId) throws SQLException {
        List<Attendance> attendances = new ArrayList<>();

        String sql = "SELECT a.*, s.id as student_id, s.name as student_name, " +
                "c.id as session_id, c.class_id, c.subject_id, c.date " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions c ON a.session_id = c.id " +
                "WHERE a.student_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = mapResultSetToAttendance(rs);
                    attendances.add(attendance);
                }
            }
        }

        return attendances;
    }

    /**
     * Lấy danh sách điểm danh theo ID lớp học
     *
     * @param classId ID của lớp học
     * @return Danh sách điểm danh cho lớp học đó
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public List<Attendance> getByClassId(long classId) throws SQLException {
        List<Attendance> attendances = new ArrayList<>();

        String sql = "SELECT a.*, s.id as student_id, s.name as student_name, " +
                "c.id as session_id, c.class_id, c.subject_id, c.date " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions c ON a.session_id = c.id " +
                "WHERE c.class_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = mapResultSetToAttendance(rs);
                    attendances.add(attendance);
                }
            }
        }

        return attendances;
    }

    /**
     * Lấy thông tin điểm danh theo ID
     *
     * @param id ID của bản ghi điểm danh
     * @return Đối tượng điểm danh hoặc null nếu không tìm thấy
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public Attendance getById(long id) throws SQLException {
        String sql = "SELECT a.*, s.id as student_id, s.name as student_name, " +
                "c.id as session_id, c.class_id, c.subject_id, c.date " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions c ON a.session_id = c.id " +
                "WHERE a.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAttendance(rs);
                }
            }
        }

        return null;
    }

    /**
     * Thêm một bản ghi điểm danh mới
     *
     * @param attendance Đối tượng điểm danh cần thêm
     * @return true nếu thêm thành công, ngược lại là false
     */
    public boolean add(Attendance attendance) {
        String sql = "INSERT INTO attendance (student_id, session_id, present, note, called, " +
                "has_permission, check_in_time, record_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            populatePreparedStatement(stmt, attendance);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            // Lấy ID được sinh tự động
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    attendance.setId(generatedKeys.getLong(1));
                }
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm bản ghi điểm danh: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin điểm danh
     *
     * @param attendance Đối tượng điểm danh cần cập nhật
     * @return true nếu cập nhật thành công, ngược lại là false
     */
    public boolean update(Attendance attendance) {
        String sql = "UPDATE attendance SET student_id = ?, session_id = ?, present = ?, " +
                "note = ?, called = ?, has_permission = ?, check_in_time = ?, record_time = ? " +
                "WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            populatePreparedStatement(stmt, attendance);
            stmt.setLong(9, attendance.getId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật bản ghi điểm danh: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa bản ghi điểm danh
     *
     * @param id ID của bản ghi điểm danh cần xóa
     * @return true nếu xóa thành công, ngược lại là false
     */
    public boolean delete(long id) {
        String sql = "DELETE FROM attendance WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa bản ghi điểm danh: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách điểm danh theo khoảng thời gian
     *
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Danh sách điểm danh trong khoảng thời gian
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public List<Attendance> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
        List<Attendance> attendances = new ArrayList<>();

        String sql = "SELECT a.*, s.id as student_id, s.name as student_name, " +
                "c.id as session_id, c.class_id, c.subject_id, c.date " +
                "FROM attendance a " +
                "JOIN students s ON a.student_id = s.id " +
                "JOIN class_sessions c ON a.session_id = c.id " +
                "WHERE c.date BETWEEN ? AND ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance attendance = mapResultSetToAttendance(rs);
                    attendances.add(attendance);
                }
            }
        }

        return attendances;
    }

    /**
     * Lấy thống kê điểm danh theo học sinh
     *
     * @param studentId ID của học sinh
     * @return Map<String, Integer> với key là loại điểm danh, value là số lượng
     * @throws SQLException Nếu có lỗi xảy ra khi truy vấn
     */
    public Map<String, Integer> getAttendanceStatsByStudent(long studentId) throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("present", 0);
        stats.put("absent_with_permission", 0);
        stats.put("absent_without_permission", 0);

        String sql = "SELECT present, has_permission, COUNT(*) as count " +
                "FROM attendance " +
                "WHERE student_id = ? " +
                "GROUP BY present, has_permission";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, studentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean present = rs.getBoolean("present");
                    boolean hasPermission = rs.getBoolean("has_permission");
                    int count = rs.getInt("count");

                    if (present) {
                        stats.put("present", stats.get("present") + count);
                    } else if (hasPermission) {
                        stats.put("absent_with_permission", stats.get("absent_with_permission") + count);
                    } else {
                        stats.put("absent_without_permission", stats.get("absent_without_permission") + count);
                    }
                }
            }
        }

        return stats;
    }

    /**
     * Đóng kết nối đến cơ sở dữ liệu
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Phương thức hỗ trợ

    /**
     * Chuyển đổi ResultSet thành đối tượng Attendance
     *
     * @param rs ResultSet chứa dữ liệu cần chuyển đổi
     * @return Đối tượng Attendance
     * @throws SQLException Nếu có lỗi xảy ra khi truy cập dữ liệu
     */
    private Attendance mapResultSetToAttendance(ResultSet rs) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setId(rs.getLong("id"));

        // Tạo và thiết lập đối tượng Student
        Student student = new Student();
        student.setId("student_id");
        // Thiết lập các thuộc tính khác của student nếu có
        if (rs.getString("student_name") != null) {
            student.setName(rs.getString("student_name"));
        }
        attendance.setStudent(student);

        // Tạo và thiết lập đối tượng ClassSession
        ClassSession session = new ClassSession();
        session.setId(rs.getLong("session_id"));
        session.setClassId(rs.getLong("class_id"));
        session.setClassId(rs.getLong("subject_id"));
        // Thiết lập các thuộc tính khác của session nếu có
        if (rs.getTimestamp("date") != null) {
            session.setDate(rs.getTimestamp("date").toLocalDateTime().toLocalDate());
        }
        attendance.setSession(session);

        attendance.setPresent(rs.getBoolean("present"));
        attendance.setNote(rs.getString("note"));
        attendance.setCalled(rs.getBoolean("called"));
        attendance.setHasPermission(rs.getBoolean("has_permission"));

        // Xử lý các giá trị null cho timestamp
        Timestamp checkInTime = rs.getTimestamp("check_in_time");
        if (checkInTime != null) {
            attendance.setCheckInTime(checkInTime.toLocalDateTime());
        }

        Timestamp recordTime = rs.getTimestamp("record_time");
        if (recordTime != null) {
            attendance.setRecordTime(recordTime.toLocalDateTime());
        }

        return attendance;
    }

    /**
     * Đổ dữ liệu từ đối tượng Attendance vào PreparedStatement
     *
     * @param stmt PreparedStatement cần đổ dữ liệu
     * @param attendance Đối tượng Attendance chứa dữ liệu
     * @throws SQLException Nếu có lỗi xảy ra khi gán dữ liệu
     */
    private void populatePreparedStatement(PreparedStatement stmt, Attendance attendance) throws SQLException {
        // Gán các giá trị cho PreparedStatement
        stmt.setString(1, attendance.getStudent().getId());
        stmt.setLong(2, attendance.getSession().getId());
        stmt.setBoolean(3, attendance.isPresent());
        stmt.setString(4, attendance.getNote());
        stmt.setBoolean(5, attendance.isCalled());
        stmt.setBoolean(6, attendance.hasPermission());

        // Xử lý các giá trị null cho timestamp
        if (attendance.getCheckInTime() != null) {
            stmt.setTimestamp(7, Timestamp.valueOf(attendance.getCheckInTime()));
        } else {
            stmt.setNull(7, Types.TIMESTAMP);
        }

        if (attendance.getRecordTime() != null) {
            stmt.setTimestamp(8, Timestamp.valueOf(attendance.getRecordTime()));
        } else {
            stmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
        }
    }
}
