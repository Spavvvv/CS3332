
package src.dao.ClassSession;

import src.dao.Classrooms.ClassroomDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Person.TeacherDAO;
import src.dao.Schedule.ScheduleDAO;
import src.model.ClassSession;
import src.model.system.course.Course;
import src.model.classroom.Classroom;
import src.model.person.Teacher;

import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for managing ClassSession entities in the database.
 * This class handles CRUD operations and more specialized queries for class sessions.
 *
 * Key functionality:
 * 1. Basic CRUD operations for ClassSession objects
 * 2. Batch generation of sessions for courses based on schedule parameters
 * 3. Finding sessions by various criteria (date, time range, teacher, etc.)
 * 4. Transaction management for session operations
 */
public class ClassSessionDAO {

    private static final Logger LOGGER = Logger.getLogger(ClassSessionDAO.class.getName());

    public ClassSessionDAO() {
        // Constructor
    }
    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
    /**
     * Maps a database result set row to a ClassSession object.
     *
     * @param rs ResultSet positioned at the row to map
     * @return A populated ClassSession object
     * @throws SQLException If there's an error accessing the ResultSet
     */
    private ClassSession mapResultSetToClassSession(ResultSet rs) throws SQLException {
        ClassSession session = new ClassSession();
        session.setId(rs.getString("session_id"));
        session.setCourseId(rs.getString("course_id")); // Lấy course_id từ bảng
        session.setCourseId(rs.getString("course_id"));
        session.setCourseName(rs.getString("course_name"));

        Timestamp dbStartTime = rs.getTimestamp("start_time");
        if (dbStartTime != null) {
            session.setStartTime(dbStartTime.toLocalDateTime());
        }

        Timestamp dbEndTime = rs.getTimestamp("end_time");
        if (dbEndTime != null) {
            session.setEndTime(dbEndTime.toLocalDateTime());
        }

        session.setRoom(rs.getString("room"));
        session.setTeacher(rs.getString("teacher_name"));
        session.setSessionNumber(rs.getInt("session_number"));
        session.setSessionNotes(rs.getString("session_notes"));

        return session;
    }

    /**
     * Prepares a SQL statement with values from a ClassSession object.
     *
     * @param stmt The prepared statement to populate with values
     * @param session The ClassSession object containing the values
     * @param includeIdInValues Whether to include the ID as a parameter (for INSERT vs UPDATE)
     * @throws SQLException If there's an error setting statement parameters
     */
    private void prepareStatementFromClassSession(PreparedStatement stmt, ClassSession session, boolean includeIdInValues) throws SQLException {
        int paramIndex = 1;

        if (includeIdInValues) { // Cho INSERT
            stmt.setString(paramIndex++, session.getId());
            stmt.setString(paramIndex++, session.getCourseId());
            stmt.setString(paramIndex++, session.getCourseId());
            stmt.setString(paramIndex++, session.getCourseName());

            if (session.getStartTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getStartTime()));
                stmt.setDate(paramIndex++, Date.valueOf(session.getStartTime().toLocalDate())); // session_date
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP); // start_time
                stmt.setNull(paramIndex++, Types.DATE);      // session_date
            }

            if (session.getEndTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getEndTime()));
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP); // end_time
            }

            stmt.setString(paramIndex++, session.getRoom());
            stmt.setString(paramIndex++, session.getTeacher());
            stmt.setInt(paramIndex++, session.getSessionNumber());
            // if (session.getSessionNotes() != null) { // Nếu có session_notes
            //     stmt.setString(paramIndex++, session.getSessionNotes());
            // }
        } else { // Cho UPDATE (không bao gồm session_id trong phần SET)
            stmt.setString(paramIndex++, session.getCourseId());
            stmt.setString(paramIndex++, session.getCourseId()); // class_id
            stmt.setString(paramIndex++, session.getCourseName());

            if (session.getStartTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getStartTime()));
                stmt.setDate(paramIndex++, Date.valueOf(session.getStartTime().toLocalDate())); // session_date
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP);
                stmt.setNull(paramIndex++, Types.DATE);
            }

            if (session.getEndTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getEndTime()));
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP);
            }
            stmt.setString(paramIndex++, session.getRoom());
            stmt.setString(paramIndex++, session.getTeacher());
            stmt.setInt(paramIndex++, session.getSessionNumber());
        }
    }

    /**
     * Creates a new class session record in the database.
     *
     * @param conn Active database connection
     * @param session The ClassSession to create
     * @return true if creation was successful, false otherwise
     * @throws SQLException If there's a database error
     */
    boolean internalCreate(Connection conn, ClassSession session) throws SQLException {
        // Câu lệnh SQL này có 10 cột và 10 placeholder (đã bao gồm session_notes)
        String sql = "INSERT INTO class_sessions (session_id, course_id, course_name, " +
                "start_time, session_date, end_time, room, teacher_name, session_number, session_notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 10 placeholders

        if (session.getId() == null || session.getId().trim().isEmpty()) {
            session.setId("SESS_FALLBACK_" + UUID.randomUUID().toString());
            LOGGER.log(Level.INFO, "internalCreate generated fallback session ID: {0} for course {1}",
                    new Object[]{session.getId(), session.getCourseName()});
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, session.getId());
            stmt.setString(paramIndex++, session.getCourseId());
            // Dựa trên hình ảnh bảng class_sessions, không có cột class_id riêng biệt.
            // Nếu ClassSession.getClassId() trả về giá trị của course_id hoặc một giá trị khác
            // mà không có cột tương ứng trong INSERT, đó có thể là vấn đề.
            // Tuy nhiên, lỗi hiện tại là thiếu tham số, không phải sai cột.
            // Câu SQL của bạn ở trên không có class_id riêng, nên bỏ qua.
            stmt.setString(paramIndex++, session.getCourseName());

            if (session.getStartTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getStartTime())); // start_time
                stmt.setDate(paramIndex++, Date.valueOf(session.getStartTime().toLocalDate())); // session_date
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP); // start_time
                stmt.setNull(paramIndex++, Types.DATE);      // session_date
            }

            if (session.getEndTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getEndTime())); // end_time
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP); // end_time
            }

            stmt.setString(paramIndex++, session.getRoom());
            stmt.setString(paramIndex++, session.getTeacher());
            stmt.setInt(paramIndex++, session.getSessionNumber());

            // THAM SỐ THỨ 10 BỊ THIẾU LÀ Ở ĐÂY: session_notes
            // Giả sử ClassSession model có phương thức getSessionNotes()
            // Nếu session.getSessionNotes() có thể null, bạn cần xử lý:
            if (session.getSessionNotes() != null) {
                stmt.setString(paramIndex++, session.getSessionNotes());
            } else {
                stmt.setNull(paramIndex++, Types.VARCHAR); // Hoặc Types.LONGVARCHAR nếu session_notes là TEXT
            }

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing class session in the database.
     *
     * @param conn Active database connection
     * @param session The ClassSession to update
     * @return true if update was successful, false otherwise
     * @throws SQLException If there's a database error
     */
    boolean internalUpdate(Connection conn, ClassSession session) throws SQLException {
        String sql = "UPDATE class_sessions SET course_id = ?, course_name = ?, " +
                "start_time = ?, session_date = ?, end_time = ?, room = ?, teacher_name = ?, " +
                "session_number = ?, session_notes = ?" +
                " WHERE session_id = ?"; // 9 fields để SET

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, session.getCourseId());
            stmt.setString(paramIndex++, session.getCourseName());

            if (session.getStartTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getStartTime()));
                stmt.setDate(paramIndex++, Date.valueOf(session.getStartTime().toLocalDate())); // session_date
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP);
                stmt.setNull(paramIndex++, Types.DATE);
            }

            if (session.getEndTime() != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(session.getEndTime()));
            } else {
                stmt.setNull(paramIndex++, Types.TIMESTAMP);
            }

            stmt.setString(paramIndex++, session.getRoom());
            stmt.setString(paramIndex++, session.getTeacher());
            stmt.setInt(paramIndex++, session.getSessionNumber());
            stmt.setString(paramIndex++, session.getSessionNotes()); // session_notes

            stmt.setString(paramIndex++, session.getId()); // Cho WHERE clause

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Generates and saves class sessions for a course based on its schedule parameters.
     *
     * @param conn Active database connection
     * @param course The course to generate sessions for
     * @param classroomDAO DAO for classroom operations
     * @param teacherDAO DAO for teacher operations
     * @param holidayDAO Optional DAO for holiday checking
     * @param scheduleDAO Optional DAO for room scheduling
     * @return List of generated and saved class sessions
     * @throws SQLException If there's a database error
     */
    public List<ClassSession> generateAndSaveSessionsForCourse(
            Connection conn,
            Course course,
            ClassroomDAO classroomDAO,
            TeacherDAO teacherDAO,
            HolidayDAO holidayDAO,
            ScheduleDAO scheduleDAO // scheduleDAO có thể là null nếu không dùng
    ) throws SQLException {

        List<ClassSession> generatedSessions = new ArrayList<>();

        // --- Bước 1: Kiểm tra tính hợp lệ của đối tượng Course ---
        if (course == null) {
            LOGGER.log(Level.WARNING, "Đối tượng Course là null. Không thể tạo buổi học.");
            return generatedSessions;
        }

        // Sử dụng course.getDaysOfWeekList() thay vì course.getDaysOfWeekAsString()
        // và course.getClassId() đã có sẵn trong model Course.java
        if (course.getCourseId() == null || course.getStartDate() == null ||
                course.getEndDate() == null || course.getCourseStartTime() == null || course.getCourseEndTime() == null ||
                course.getDaysOfWeekList() == null || course.getDaysOfWeekList().isEmpty()) { // SỬA Ở ĐÂY
            LOGGER.log(Level.WARNING, "Khóa học {0} thiếu các trường thông tin cần thiết cho lịch trình (ID, ngày, giờ, danh sách ngày học). Không thể tạo buổi học.",
                    course.getCourseId());
            return generatedSessions;
        }
        // Kiểm tra course.getClassId() nếu nó là bắt buộc cho ClassSession
        // Trong file Course.java bạn cung cấp, classId có thể null, nên không cần kiểm tra ở đây trừ khi ClassSession yêu cầu.
        // Nếu ClassSession yêu cầu classId không null:
        // if (course.getClassId() == null || course.getClassId().trim().isEmpty()){
        //     LOGGER.log(Level.WARNING, "Khóa học {0} thiếu Class ID. Không thể tạo buổi học.", course.getCourseId());
        //     return generatedSessions;
        // }


        if (course.getStartDate().isAfter(course.getEndDate())) {
            LOGGER.log(Level.WARNING, "Ngày bắt đầu của khóa học {0} sau ngày kết thúc. Không thể tạo buổi học.",
                    course.getCourseId());
            return generatedSessions;
        }

        // --- Bước 2: Lấy thông tin phòng học và giáo viên ---
        String actualRoomName = "N/A";
        if (classroomDAO != null && course.getRoomId() != null && !course.getRoomId().trim().isEmpty()) {
            // Giả định ClassroomDAO.findByRoomId(String roomId) tự quản lý connection
            // hoặc bạn có một phiên bản findByRoomId(Connection conn, String roomId) nếu cần dùng transaction hiện tại.
            Optional<Classroom> classroomOpt = classroomDAO.findByRoomId(course.getRoomId()); //
            if (classroomOpt.isPresent()) {
                actualRoomName = classroomOpt.get().getRoomName(); //
            } else {
                LOGGER.log(Level.WARNING, "Không tìm thấy phòng học với ID: {0} cho khóa học {1}",
                        new Object[]{course.getRoomId(), course.getCourseId()});
            }
        }

        String actualTeacherName = "N/A";
        // Ưu tiên lấy thông tin Teacher từ đối tượng Course đã có
        if (course.getTeacher() != null) { //
            actualTeacherName = course.getTeacher().getName(); // getName() từ lớp Person
            // Nếu tên không hợp lệ và có TeacherDAO, thử tải lại bằng ID (user_id)
            if ((actualTeacherName == null || actualTeacherName.trim().isEmpty() || actualTeacherName.equals("N/A")) &&
                    teacherDAO != null && course.getTeacher().getId() != null && !course.getTeacher().getId().trim().isEmpty()) {
                // Giả định TeacherDAO.findById(String userId) tự quản lý connection
                Optional<Teacher> teacherOpt = teacherDAO.findById(course.getTeacher().getId()); // getId() từ Person, là user_id
                if (teacherOpt.isPresent()) {
                    actualTeacherName = teacherOpt.get().getName();
                } else {
                    LOGGER.log(Level.WARNING, "Không tìm thấy giáo viên (khi tải lại) với user_id: {0} cho khóa học {1}.",
                            new Object[]{course.getTeacher().getId(), course.getCourseId()});
                }
            }
        } else if (teacherDAO != null && course.getTeacherId() != null && !course.getTeacherId().trim().isEmpty()) {
            // Nếu course.getTeacher() là null nhưng có course.getTeacherId() (là user_id)
            Optional<Teacher> teacherOpt = teacherDAO.findById(course.getTeacherId()); //
            if (teacherOpt.isPresent()) {
                actualTeacherName = teacherOpt.get().getName();
            } else {
                LOGGER.log(Level.WARNING, "Không tìm thấy giáo viên với user_id: {0} cho khóa học {1}",
                        new Object[]{course.getTeacherId(), course.getCourseId()});
            }
        }


        // --- Bước 3: Phân tích các ngày học trong tuần ---
        Set<DayOfWeek> scheduledDays = new HashSet<>();
        // Sử dụng getDaysOfWeekList() từ Course.java, trả về List<String> như "Mon", "Tue"
        List<String> shortDayStrings = course.getDaysOfWeekList(); //

        for (String shortDayStr : shortDayStrings) {
            // Gọi phương thức mapShortDayToDayOfWeek đã thêm vào ClassSessionDAO
            DayOfWeek dow = mapShortDayToDayOfWeek(shortDayStr);
            if (dow != null) {
                scheduledDays.add(dow);
            } else {
                // mapShortDayToDayOfWeek đã ghi log cảnh báo
                LOGGER.log(Level.SEVERE, "Chuỗi ngày không hợp lệ '{0}' trong danh sách ngày học của khóa {1}. Ngày này sẽ bị bỏ qua.",
                        new Object[]{shortDayStr, course.getCourseId()});
            }
        }

        if (scheduledDays.isEmpty()) {
            LOGGER.log(Level.WARNING, "Không có ngày học hợp lệ nào được phân tích cho khóa học {0}. Sẽ không có buổi học nào được tạo.",
                    course.getCourseId());
            return generatedSessions;
        }

        // --- Bước 4: Lặp qua các ngày để tạo buổi học ---
        LocalDate currentIterDate = course.getStartDate();
        LocalDate courseEndDate = course.getEndDate();
        LocalTime sessionStartTimeOfDay = course.getCourseStartTime(); //
        LocalTime sessionEndTimeOfDay = course.getCourseEndTime();     //
        int sessionCounter = 1;

        while (!currentIterDate.isAfter(courseEndDate)) {
            if (scheduledDays.contains(currentIterDate.getDayOfWeek())) {
                boolean isHoliday = false;
                if (holidayDAO != null) {
                    // Giả sử holidayDAO.isHoliday không yêu cầu Connection hoặc tự quản lý
                    isHoliday = holidayDAO.isHoliday(currentIterDate);
                }

                if (!isHoliday) {
                    LocalDateTime actualSessionStartDateTime = LocalDateTime.of(currentIterDate, sessionStartTimeOfDay);
                    LocalDateTime actualSessionEndDateTime = LocalDateTime.of(currentIterDate, sessionEndTimeOfDay);

                    ClassSession newSession = new ClassSession();
                    String generatedSessionId = "SESS_" +
                            course.getCourseId().replaceAll("[^a-zA-Z0-9]", "") + "_" +
                            currentIterDate.toString().replace("-", "") + "_" +
                            String.format("%03d", sessionCounter);

                    newSession.setId(generatedSessionId);
                    newSession.setCourseId(course.getCourseId());
                    newSession.setCourseId(course.getCourseId()); // Lấy classId từ Course object
                    newSession.setCourseName(course.getCourseName());
                    newSession.setStartTime(actualSessionStartDateTime); // Gán cả ngày và giờ
                    newSession.setEndTime(actualSessionEndDateTime);   // Gán cả ngày và giờ
                    newSession.setRoom(actualRoomName);
                    newSession.setTeacher(actualTeacherName);
                    newSession.setSessionNumber(sessionCounter);
                    // newSession.setSessionNotes("Ghi chú cho buổi " + sessionCounter); // Nếu bạn có trường này

                    // Gọi internalCreate đã được cập nhật để lưu session_date, class_id
                    boolean savedSuccessfully = internalCreate(conn, newSession);
                    if (savedSuccessfully) {
                        generatedSessions.add(newSession);
                        // LOGGER.log(Level.FINER, "Đã tạo và lưu buổi học: {0}", newSession.getId());
                    } else {
                        LOGGER.log(Level.WARNING, "Không thể lưu buổi học được tạo: {0} cho khóa học {1}. "
                                        + "Transaction nên được rollback bởi bên gọi.",
                                new Object[]{newSession.getId(), course.getCourseId()});
                        // Nếu việc tạo một session thất bại là nghiêm trọng, bạn có thể ném SQLException ở đây
                        // để trigger rollback ở nơi gọi.
                        // throw new SQLException("Không thể lưu buổi học đã tạo: " + newSession.getId() + " cho khóa " + course.getCourseId());
                    }
                    sessionCounter++;
                } else {
                    LOGGER.log(Level.INFO, "Bỏ qua tạo buổi học cho khóa {0} vào ngày {1} vì là ngày nghỉ.",
                            new Object[]{course.getCourseId(), currentIterDate});
                }
            }
            currentIterDate = currentIterDate.plusDays(1);
        }

        LOGGER.log(Level.INFO, "Đã cố gắng tạo {0} buổi học cho khóa {1}, lưu thành công {2} buổi.",
                new Object[]{(sessionCounter - 1), course.getCourseId(), generatedSessions.size()});
        return generatedSessions;
    }

    /**
     * Deletes future class sessions for a course.
     *
     * @param conn Active database connection
     * @param courseId The course ID to delete sessions for
     * @return Number of deleted sessions
     * @throws SQLException If there's a database error
     */
    public int deleteFutureSessionsByCourseId(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE course_id = ? AND start_time >= ?";
        int deletedRows = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDate.now().atStartOfDay()));
            deletedRows = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted {0} future sessions for course ID: {1}",
                    new Object[]{deletedRows, courseId});
        }
        return deletedRows;
    }

    /**
     * Deletes all class sessions for a course.
     *
     * @param conn Active database connection
     * @param courseId The course ID to delete sessions for
     * @return Number of deleted sessions
     * @throws SQLException If there's a database error
     */
    public int deleteAllSessionsByCourseId(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE course_id = ?";
        int deletedRows = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            deletedRows = stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Deleted {0} (all) sessions for course ID: {1}",
                    new Object[]{deletedRows, courseId});
        }
        return deletedRows;
    }

    // Base SELECT statement for consistent column selection
    private String getBaseSelectClassSessionSQL() {
        // Các cột từ hình ảnh: session_id, session_date, start_time, end_time,
        // teacher_name, course_name, room, course_id, session_number, session_notes
        return "SELECT session_id, course_id, course_name, " +
                "start_time, end_time, room, teacher_name, " +
                "session_number, session_date, session_notes" +
                " FROM class_sessions";
    }

    // Internal query methods (using provided connection)

    List<ClassSession> internalFindAll(Connection conn) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapResultSetToClassSession(rs));
            }
        }
        return sessions;
    }

    ClassSession internalFindById(Connection conn, String id) throws SQLException {
        String sql = getBaseSelectClassSessionSQL() + " WHERE session_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToClassSession(rs);
                }
            }
        }
        return null;
    }

    boolean internalDelete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM class_sessions WHERE session_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    List<ClassSession> internalFindByClassId(Connection conn, String classId /* thực chất là course_id */) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // Truy vấn cột course_id, vì không có cột class_id riêng trong bảng
        String sql = getBaseSelectClassSessionSQL() + " WHERE course_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, classId); // classId ở đây được dùng như course_id
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByCourseId(Connection conn, String courseId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE course_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE session_date BETWEEN ? AND ?"; // Sử dụng session_date
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDateTimeRange(Connection conn, LocalDateTime startDateTime, LocalDateTime endDateTime) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        // Find sessions that overlap with the given range
        String sql = getBaseSelectClassSessionSQL() + " WHERE start_time < ? AND end_time > ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(endDateTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startDateTime));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByTeacherName(Connection conn, String teacherName) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE teacher_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    List<ClassSession> internalFindByDate(Connection conn, LocalDate date) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String sql = getBaseSelectClassSessionSQL() + " WHERE DATE(start_time) = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        return sessions;
    }

    // Public wrapper methods (managing their own connections)

    public Optional<ClassSession> findById(String sessionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(internalFindById(conn, sessionId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class session by ID: " + sessionId, e);
            return Optional.empty();
        }
    }

    public Optional<ClassSession> findById(Connection conn, String sessionId) {
        try {
            return Optional.ofNullable(internalFindById(conn, sessionId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class session by ID using existing connection: " + sessionId, e);
            return Optional.empty();
        }
    }

    public List<ClassSession> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindAll(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all class sessions", e);
            return new ArrayList<>();
        }
    }

    public boolean save(ClassSession session) {
        if (session.getId() == null || session.getId().trim().isEmpty()) {
            session.setId("SESS_SAVE_" + UUID.randomUUID().toString());
            LOGGER.log(Level.INFO, "Public save method generated new session ID: {0}", session.getId());
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalCreate(conn, session);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving class session: " + session.getId(), e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public boolean update(ClassSession session) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, session);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating class session with ID " + session.getId(), e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public boolean delete(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete class session with null or empty ID.");
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, sessionId);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting class session with ID " + sessionId, e);
            DatabaseConnection.rollback(conn);
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    public List<ClassSession> findByCourseId(String courseId) {
        if (courseId == null || courseId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty course ID.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByCourseId(conn, courseId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by course ID " + courseId, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByClassId(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty class ID.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByClassId(conn, classId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by class ID " + classId, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateRange(conn, startDate, endDate);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by date range (" + startDate + " to " + endDate + ")", e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByTeacherName(String teacherName) {
        if (teacherName == null || teacherName.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null or empty teacher name.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByTeacherName(conn, teacherName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by teacher name " + teacherName, e);
            return new ArrayList<>();
        }
    }

    public List<ClassSession> findByDate(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null date.");
            return new ArrayList<>();
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDate(conn, date);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by date " + date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Finds class sessions that fall within a specific time range.
     * This method will find any sessions where the session's time period overlaps
     * with the given time range.
     *
     * @param startTime The start datetime of the range to search
     * @param endTime The end datetime of the range to search
     * @return A list of ClassSession objects that overlap with the specified time range
     */
    public List<ClassSession> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end time.");
            return new ArrayList<>();
        }

        if (startTime.isAfter(endTime)) {
            LOGGER.log(Level.WARNING, "Invalid time range: start time is after end time.");
            return new ArrayList<>();
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalFindByDateTimeRange(conn, startTime, endTime);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding class sessions by time range ("
                    + startTime + " to " + endTime + ")", e);
            return new ArrayList<>();
        }
    }

    /**
     * Overloaded method to find class sessions within a time range using an existing connection.
     *
     * @param conn An existing database connection
     * @param startTime The start datetime of the range to search
     * @param endTime The end datetime of the range to search
     * @return A list of ClassSession objects that overlap with the specified time range
     * @throws SQLException if a database access error occurs
     */
    public List<ClassSession> findByTimeRange(Connection conn, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find class sessions with null start or end time.");
            return new ArrayList<>();
        }

        if (startTime.isAfter(endTime)) {
            LOGGER.log(Level.WARNING, "Invalid time range: start time is after end time.");
            return new ArrayList<>();
        }

        return internalFindByDateTimeRange(conn, startTime, endTime);
    }

    /**
     * Tìm số thứ tự buổi học (session_number) lớn nhất cho một khóa học (courseId)
     * mà ngày diễn ra (DATE(start_time)) nhỏ hơn hoặc bằng ngày hiện tại được cung cấp.
     * Điều này đại diện cho "buổi học hiện tại" về mặt tiến độ.
     *
     * @param courseId ID của khóa học.
     * @param currentDate Ngày hiện tại để so sánh.
     * @return Optional chứa số buổi học hiện tại (có thể là 0 nếu không có buổi nào phù hợp),
     * hoặc Optional.empty() nếu có lỗi.
     */
    public Optional<Integer> findCurrentSessionNumberByDate(String courseId, LocalDate currentDate) {
        if (courseId == null || courseId.trim().isEmpty() || currentDate == null) {
            LOGGER.log(Level.WARNING, "courseId hoặc currentDate không hợp lệ để tìm current session number.");
            return Optional.of(0);
        }

        String sql = "SELECT MAX(session_number) FROM class_sessions WHERE course_id = ? AND session_date <= ?"; // SỬA Ở ĐÂY

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, courseId);
            stmt.setDate(2, java.sql.Date.valueOf(currentDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int maxSessionNumber = rs.getInt(1);
                    if (rs.wasNull()) {
                        return Optional.of(0);
                    }
                    return Optional.of(maxSessionNumber);
                } else {
                    return Optional.of(0);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm số buổi học hiện tại cho course ID: " + courseId + " theo ngày: " + currentDate, e);
            return Optional.empty();
        }
    }
    private DayOfWeek mapShortDayToDayOfWeek(String shortDay) {
        if (shortDay == null) return null;
        switch (shortDay.trim().toUpperCase()) {
            case "MON": return DayOfWeek.MONDAY;
            case "TUE": return DayOfWeek.TUESDAY;
            case "WED": return DayOfWeek.WEDNESDAY;
            case "THU": return DayOfWeek.THURSDAY;
            case "FRI": return DayOfWeek.FRIDAY;
            case "SAT": return DayOfWeek.SATURDAY;
            case "SUN": return DayOfWeek.SUNDAY;
            default:
                LOGGER.log(Level.WARNING, "Phát hiện chuỗi ngày viết tắt không hợp lệ: {0}", shortDay);
                return null;
        }
    }

    /**
     * TÌM CÁC BUỔI HỌC CỦA MỘT GIÁO VIÊN (THEO TÊN) TRONG MỘT KHOẢNG NGÀY.
     *
     * @param teacherName Tên của giáo viên.
     * @param startDate Ngày bắt đầu của khoảng thời gian.
     * @param endDate Ngày kết thúc của khoảng thời gian.
     * @return Danh sách các ClassSession.
     * @throws SQLException
     */
    public List<ClassSession> findByTeacherNameAndDateRange(String teacherName, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        if (teacherName == null || teacherName.trim().isEmpty() || startDate == null || endDate == null) {
            LOGGER.warning("Invalid parameters for findByTeacherNameAndDateRange.");
            return sessions;
        }

        // Đảm bảo getBaseSelectClassSessionSQL() lấy tất cả các cột cần thiết
        // và bảng class_sessions có cột "teacher_name"
        String sql = getBaseSelectClassSessionSQL() +
                " WHERE teacher_name = ? AND session_date BETWEEN ? AND ? " + // Sử dụng session_date và teacher_name
                "ORDER BY start_time ASC"; // Sắp xếp theo thời gian bắt đầu

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacherName);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToClassSession(rs));
                }
            }
        }
        LOGGER.log(Level.INFO, "Found {0} sessions for teacher ''{1}'' between {2} and {3}",
                new Object[]{sessions.size(), teacherName, startDate, endDate});
        return sessions;
    }

}

