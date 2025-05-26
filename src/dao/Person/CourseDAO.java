package src.dao.Person;

import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Notifications.RoomConflictException;
import src.model.system.course.Course;
import src.model.person.Student;
import src.model.person.Teacher;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class CourseDAO {
    private StudentDAO studentDAO;
    private TeacherDAO teacherDAO;
    private static final Logger LOGGER = Logger.getLogger(CourseDAO.class.getName());


    public CourseDAO() {
        // Dependencies will be set by DaoManager
    }

    public void setStudentDAO(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
    }

    public void setTeacherDAO(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    public TeacherDAO getTeacherDAO() {
        return this.teacherDAO;
    }

    private void checkStudentDAODependency() {
        if (this.studentDAO == null) {
            throw new IllegalStateException("StudentDAO dependency has not been set on CourseDAO.");
        }
    }

    private void checkTeacherDAODependency() {
        if (this.teacherDAO == null) {
            throw new IllegalStateException("TeacherDAO dependency has not been set on CourseDAO.");
        }
    }

    // Helper to convert string day ("Mon", "Tue") to numeric for coursescheduledays
    // Assumes 1=Monday, ..., 7=Sunday (consistent with java.time.DayOfWeek.getValue())
    private int mapStringDayToNumeric(String dayString) {
        if (dayString == null) return 0; // Invalid
        switch (dayString.trim().toUpperCase().substring(0, Math.min(dayString.trim().length(), 3))) {
            case "MON": return 1;
            case "TUE": return 2;
            case "WED": return 3;
            case "THU": return 4;
            case "FRI": return 5;
            case "SAT": return 6;
            case "SUN": return 7;
            default: return 0; // Invalid
        }
    }

    // Helper to convert numeric day from coursescheduledays to string ("Mon", "Tue")
    private String mapNumericDayToString(int numericDay) {
        switch (numericDay) {
            case 1: return "Mon";
            case 2: return "Tue";
            case 3: return "Wed";
            case 4: return "Thu";
            case 5: return "Fri";
            case 6: return "Sat";
            case 7: return "Sun";
            default: return null; // Invalid
        }
    }


    private void saveCourseScheduleDays(Connection conn, Course course) throws SQLException {
        // 1. Delete existing schedule days for this course
        String deleteSql = "DELETE FROM coursescheduledays WHERE course_id = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setString(1, course.getCourseId());
            deleteStmt.executeUpdate();
        }

        // 2. Insert new schedule days
        if (course.getDaysOfWeekList() != null && !course.getDaysOfWeekList().isEmpty()) {
            String insertSql = "INSERT INTO coursescheduledays (course_id, day_of_week_numeric) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (String dayString : course.getDaysOfWeekList()) {
                    int numericDay = mapStringDayToNumeric(dayString);
                    if (numericDay > 0) { // Ensure valid day
                        insertStmt.setString(1, course.getCourseId());
                        insertStmt.setInt(2, numericDay);
                        insertStmt.addBatch();
                    }
                }
                insertStmt.executeBatch();
            }
        }
    }

    private List<String> loadCourseScheduleDays(Connection conn, String courseId) throws SQLException {
        List<String> daysOfWeekList = new ArrayList<>();
        String sql = "SELECT day_of_week_numeric FROM coursescheduledays WHERE course_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String dayString = mapNumericDayToString(rs.getInt("day_of_week_numeric"));
                    if (dayString != null) {
                        daysOfWeekList.add(dayString);
                    }
                }
            }
        }
        return daysOfWeekList;
    }
    public List<Course> findCoursesEndingOnOrAfter(Connection conn, LocalDate date) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu SQL này sẽ lấy các khóa học có ngày kết thúc NULL (chưa xác định/đang diễn ra)
        // HOẶC có ngày kết thúc lớn hơn hoặc bằng ngày được cung cấp.
        String sql = "SELECT * FROM courses WHERE end_date IS NULL OR end_date >= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date)); // Sửa: Dùng java.sql.Date
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Giả sử bạn đã có phương thức extractCourseFromResultSet(Connection conn, ResultSet rs)
                    // và nó xử lý đúng việc TeacherDAO có thể null hoặc cần được inject.
                    courses.add(extractCourseFromResultSet(conn, rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm các khóa học kết thúc sau hoặc bằng ngày: " + date, e);
            throw e; // Ném lại lỗi để phương thức gọi có thể xử lý (ví dụ: rollback transaction)
        }
        return courses;
    }

    public boolean updateCourseEndDate(Connection conn, String courseId, LocalDate newEndDate) throws SQLException {
        String sql = "UPDATE courses SET end_date = ? WHERE course_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (newEndDate != null) {
                stmt.setDate(1, Date.valueOf(newEndDate));
            } else {
                stmt.setNull(1, Types.DATE); // Cho phép đặt end_date thành NULL nếu cần
            }
            stmt.setString(2, courseId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                LOGGER.log(Level.INFO, "Cập nhật thành công end_date cho course {0} thành {1}", new Object[]{courseId, newEndDate});
                return true;
            } else {
                // Có thể không tìm thấy course_id hoặc giá trị không thay đổi.
                // Không nhất thiết là lỗi nghiêm trọng, nhưng cần ghi log.
                LOGGER.log(Level.WARNING, "Không tìm thấy course {0} để cập nhật end_date hoặc end_date không thay đổi.", courseId);
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật end_date cho course " + courseId, e);
            throw e; // Ném lại lỗi để phương thức gọi xử lý
        }
    }


    private boolean internalInsert(Connection conn, Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, progress, total_sessions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // CORRECTED: Now has 11 placeholders // Adjusted placeholder count
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseId());
            statement.setString(2, course.getCourseName());
            statement.setString(3, course.getSubject());
            statement.setDate(4, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(5, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setTime(6, course.getCourseStartTime() != null ? Time.valueOf(course.getCourseStartTime()) : null);
            statement.setTime(7, course.getCourseEndTime() != null ? Time.valueOf(course.getCourseEndTime()) : null);
            statement.setString(8, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(9, course.getRoomId());
            statement.setFloat(10, course.getProgress());
            statement.setInt(11, course.getTotalSessions()); // Use new totalSessions field

            boolean courseInserted = statement.executeUpdate() > 0;
            if (courseInserted) {
                saveCourseScheduleDays(conn, course); // Save schedule days after main course insert
            }
            return courseInserted;
        }
    }

    private boolean internalUpdate(Connection conn, Course course) throws SQLException {
        String sql = "UPDATE courses SET course_name = ?, subject = ?, start_date = ?, end_date = ?, " +
                "start_time = ?, end_time = ?, teacher_id = ?, room_id = ?" +
                "progress = ?, total_sessions = ? WHERE course_id = ?"; // Added total_sessions, removed day_of_week

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getSubject());
            statement.setDate(3, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(4, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setTime(5, course.getCourseStartTime() != null ? Time.valueOf(course.getCourseStartTime()) : null);
            statement.setTime(6, course.getCourseEndTime() != null ? Time.valueOf(course.getCourseEndTime()) : null);
            statement.setString(7, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(8, course.getRoomId());
            statement.setString(9, course.getCourseId());
            statement.setFloat(10, course.getProgress());
            statement.setInt(11, course.getTotalSessions()); // Use new totalSessions field
            statement.setString(12, course.getCourseId());

            boolean courseUpdated = statement.executeUpdate() > 0;
            if (courseUpdated) {
                saveCourseScheduleDays(conn, course); // Update schedule days after main course update
            }
            return courseUpdated;
        }
    }

    private boolean internalDelete(Connection conn, String courseId) throws SQLException {
        // Bước 0: (Tùy chọn) Khởi tạo ClassSessionDAO nếu không được inject
        // ClassSessionDAO classSessionDAOForDelete = new ClassSessionDAO(); // Hoặc lấy từ this.classSessionDAO

        // Bước 1: Xóa tất cả các ClassSession liên quan đến courseId này.
        // Giả sử ClassSessionDAO có phương thức deleteAllSessionsByCourseId(Connection conn, String courseId)
        // và phương thức này tự quản lý việc xóa trong bảng class_sessions.
        // Nếu phương thức đó chưa có, bạn cần tạo nó trong ClassSessionDAO.
        // Bạn đã có hàm public int deleteAllSessionsByCourseId(Connection conn, String courseId) trong ClassSessionDAO.

        // Để gọi được, bạn cần một instance của ClassSessionDAO.
        // Cách 1: Tạo mới (nếu ClassSessionDAO không có dependency phức tạp)
        ClassSessionDAO classSessionDAOInstance = new ClassSessionDAO();
        int sessionsDeleted = classSessionDAOInstance.deleteAllSessionsByCourseId(conn, courseId);
        System.out.println("Đã xóa " + sessionsDeleted + " buổi học liên quan đến khóa học ID: " + courseId);


        // Bước 2: Xóa các bản ghi lịch trình (coursescheduledays)
        // (Dòng này đã có trong code của bạn, giữ nguyên)
        String deleteScheduleSql = "DELETE FROM coursescheduledays WHERE course_id = ?";
        try (PreparedStatement deleteScheduleStmt = conn.prepareStatement(deleteScheduleSql)) {
            deleteScheduleStmt.setString(1, courseId);
            deleteScheduleStmt.executeUpdate();
        }

        // Bước 3: Xóa sinh viên khỏi các bản ghi danh (enrollment)
        // (Dòng này đã có trong code của bạn, giữ nguyên)
        removeAllStudentsFromCourse(conn, courseId);

        // Bước 4: Xóa Khóa học (courses)
        // (Dòng này đã có trong code của bạn, giữ nguyên)
        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    Course getById(Connection conn, String courseId) throws SQLException {
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, start_time, end_time, " +
                "teacher_id, room_id, progress, total_sessions FROM courses WHERE course_id = ?"; // Added total_sessions, removed day_of_week
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return extractCourseFromResultSet(conn, resultSet);
                }
            }
        }
        return null;
    }

    List<Course> getAll(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, start_time, end_time, " +
                "teacher_id, room_id, progress, total_sessions FROM courses"; // Added total_sessions, removed day_of_week
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                courses.add(extractCourseFromResultSet(conn, resultSet));
            }
        }
        return courses;
    }


    public Course extractCourseFromResultSet(Connection conn, ResultSet resultSet) throws SQLException {
        String courseId = resultSet.getString("course_id");
        String courseName = resultSet.getString("course_name");
        String subject = resultSet.getString("subject");
        LocalDate startDate = resultSet.getDate("start_date") != null ? resultSet.getDate("start_date").toLocalDate() : null;
        LocalDate endDate = resultSet.getDate("end_date") != null ? resultSet.getDate("end_date").toLocalDate() : null;
        LocalTime startTime = resultSet.getTime("start_time") != null ? resultSet.getTime("start_time").toLocalTime() : null;
        LocalTime endTime = resultSet.getTime("end_time") != null ? resultSet.getTime("end_time").toLocalTime() : null;
        String teacherId = resultSet.getString("teacher_id");
        String roomId = resultSet.getString("room_id");
        float progress = resultSet.getFloat("progress");
        int totalSessions = resultSet.getInt("total_sessions"); // Read new field

        Teacher teacher = null;
        if (teacherId != null && !teacherId.trim().isEmpty()) {
            checkTeacherDAODependency();
            if (this.teacherDAO != null) {
                teacher = this.teacherDAO.getById(conn, teacherId);
            } else {
                System.err.println("[WARN] extractCourseFromResultSet - TeacherDAO is null for teacherId: " + teacherId);
            }
        }

        List<String> daysOfWeekList = loadCourseScheduleDays(conn, courseId); // Load schedule days

        // Use the comprehensive constructor from the updated Course class
        Course course = new Course(courseId, courseName, subject, startDate, endDate,
                startTime, endTime, daysOfWeekList, roomId,
                teacher, totalSessions, progress);

        // Students are typically loaded separately or on-demand, not in this basic extraction
        // course.setStudents(getStudentsByCourseId(conn, courseId)); // Example if you want to load them here
        return course;
    }

    // --- Methods related to student enrollment (largely unchanged but ensure consistency) ---
    // ... (addStudentToCourse, removeStudentFromCourse, removeAllStudentsFromCourse, getStudentIdsByCourseId)
    // Ensure that `addStudentToCourse` and `removeStudentFromCourse` use the correct `course_id`
    // for the `enrollment` table, which seems to be the case already.

    private boolean addStudentToCourse(Connection conn, String courseId, String studentId) throws SQLException {
        String checkDuplicateSql = "SELECT COUNT(*) FROM enrollment WHERE student_id = ? AND course_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkDuplicateSql)) {
            checkStmt.setString(1, studentId);
            checkStmt.setString(2, courseId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("[CourseDAO Private] Lỗi: Sinh viên ID: " + studentId + " đã tồn tại trong khóa học ID: " + courseId);
                    return false; // Already enrolled
                }
            }
        }
        String sql = "INSERT INTO enrollment (enrollment_id, student_id, course_id, enrollment_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, studentId);
            statement.setString(3, courseId); // This is the course_id from courses table
            statement.setDate(4, Date.valueOf(LocalDate.now()));
            statement.setString(5, "Active");
            return statement.executeUpdate() > 0;
        }
    }

    public boolean addStudentToCourse(String courseId, String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if course exists
            Course course = getById(conn, courseId); // Ensure course exists
            if (course == null) {
                System.err.println("[CourseDAO Public] Lỗi: Không tìm thấy Khóa học với ID " + courseId);
                return false;
            }
            // Check if student exists
            checkStudentDAODependency();
            Student student = studentDAO.getStudentById(conn, studentId); // Ensure student exists
            if (student == null) {
                System.err.println("[CourseDAO Public] Lỗi: Không tìm thấy sinh viên với ID: " + studentId);
                return false;
            }
            return addStudentToCourse(conn, courseId, studentId); // Call private method
        } catch (SQLException e) {
            System.err.println("[CourseDAO Public] Gặp SQLException: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        } catch (IllegalStateException e) {
            System.err.println("[CourseDAO Public] Gặp IllegalStateException (dependency): " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    // `removeAllStudentsFromCourse` should use course_id directly with enrollment table
    private void removeAllStudentsFromCourse(Connection conn, String courseId) throws SQLException {
        String sql = "DELETE FROM enrollment WHERE course_id = ?"; // Assuming enrollment refers to course_id
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.executeUpdate();
        }
    }

    // Public method might need to be adjusted if the key for enrollment was different
    public boolean removeStudentFromCourse(String courseId, String studentId) {
        String sql = "DELETE FROM enrollment WHERE course_id = ? AND student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId); // courseId from 'courses' table
            statement.setString(2, studentId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("ERROR: Không thể xóa học viên " + studentId + " khỏi khóa học " + courseId);
            e.printStackTrace();
            return false;
        }
    }


    // --- Other find methods (need to update their SELECT queries) ---

    List<Course> getCoursesByTeacherId(Connection conn, String teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, progress, total_sessions FROM courses WHERE teacher_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, teacherId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCoursesByStudentId(Connection conn, String studentId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.course_id, c.course_name, c.subject, c.start_date, c.end_date, c.start_time, c.end_time, c.teacher_id, c.room_id, c.progress, c.total_sessions FROM courses c " +
                "JOIN enrollment e ON c.course_id = e.course_id " +
                "WHERE e.student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }
    // ... (searchCourses, getCoursesBySubject, etc. - all SELECTs need `total_sessions` and no `day_of_week`)
    // Example for searchCourses:
    List<Course> searchCourses(Connection conn, String searchTerm) throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, progress, total_sessions FROM courses WHERE course_name LIKE ? OR subject LIKE ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    // The getStudentsByCourseId method's logic was a bit complex with actualClassIdForQuery
    // If enrollment table directly links student_id with courses.course_id, it's simpler.
    // The previous logic was using courses.course_id to query enrollment.course_id which might be incorrect
    // if enrollment.course_id was meant to be enrollment.course_id
    public List<Student> getStudentsByCourseId(Connection conn, String courseId) throws SQLException {
        checkStudentDAODependency();
        List<Student> students = new ArrayList<>();
        // Assuming enrollment table has a course_id column that refers to courses.course_id
        String sql = "SELECT e.student_id FROM enrollment e WHERE e.course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("student_id");
                    Student student = this.studentDAO.getStudentById(conn, studentId);
                    if (student != null) {
                        students.add(student);
                    }
                }
            }
        }
        return students;
    }


    // --- Conflict Checking and Save/Update/Delete with Transaction ---
    /**
     * Checks if there's a time conflict for a given room, for a specific day of the week,
     * start time, and end time, excluding a specific course (e.g., when updating).
     * @param roomId ID of the room.
     * @param dayOfWeekNumeric Numeric representation of the day of the week (1=Mon, 7=Sun).
     * @param newStartTime The start time of the potential session.
     * @param newEndTime The end time of the potential session.
     * @param excludingCourseId The ID of the course to exclude from the check (null if new course).
     * @return true if a conflict exists, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean hasTimeConflictForRoomAndDay(Connection conn, String roomId, int dayOfWeekNumeric,
                                                LocalTime newStartTime, LocalTime newEndTime, String excludingCourseId) throws SQLException {
        if (roomId == null || roomId.trim().isEmpty() || dayOfWeekNumeric < 1 || dayOfWeekNumeric > 7 ||
                newStartTime == null || newEndTime == null || newStartTime.isAfter(newEndTime)) {
            System.err.println("[CourseDAO] Invalid parameters for hasTimeConflictForRoomAndDay.");
            return true; // Treat invalid input as conflict or throw IllegalArgumentException
        }

        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT COUNT(c.course_id) FROM courses c " +
                        "JOIN coursescheduledays csd ON c.course_id = csd.course_id " +
                        "WHERE c.room_id = ? AND csd.day_of_week_numeric = ? " +
                        "AND c.start_time < ? AND c.end_time > ?"); // Time overlap logic

        if (excludingCourseId != null && !excludingCourseId.trim().isEmpty()) {
            sqlBuilder.append(" AND c.course_id <> ?");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, roomId);
            stmt.setInt(paramIndex++, dayOfWeekNumeric);
            stmt.setTime(paramIndex++, Time.valueOf(newEndTime));   // existing_start_time < new_end_time
            stmt.setTime(paramIndex++, Time.valueOf(newStartTime)); // existing_end_time > new_start_time
            if (excludingCourseId != null && !excludingCourseId.trim().isEmpty()) {
                stmt.setString(paramIndex, excludingCourseId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Conflict if count > 0
                }
            }
        }
        return false; // No conflict by default
    }

    public boolean save(Course course) throws RoomConflictException, SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check for conflicts for each scheduled day
                if (course.getRoomId() != null && course.getCourseStartTime() != null && course.getCourseEndTime() != null && course.getDaysOfWeekList() != null) {
                    for (String dayStr : course.getDaysOfWeekList()) {
                        int numericDay = mapStringDayToNumeric(dayStr);
                        if (numericDay > 0) {
                            if (hasTimeConflictForRoomAndDay(conn, course.getRoomId(), numericDay,
                                    course.getCourseStartTime(), course.getCourseEndTime(), null)) { // null for excludingCourseId as it's a new course
                                conn.rollback();
                                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                                String startTimeStr = course.getCourseStartTime().format(timeFormatter);
                                String endTimeStr = course.getCourseEndTime().format(timeFormatter);
                                throw new RoomConflictException("Phòng học '" + course.getRoomId() +
                                        "' đã được đăng ký vào ngày '" + dayStr +
                                        "' từ " + startTimeStr + " đến " + endTimeStr + ".");
                            }
                        }
                    }
                }

                boolean success = internalInsert(conn, course);
                if (success) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException | RoomConflictException e) {
                conn.rollback();
                throw e; // Re-throw to be handled by caller
            }
        }
    }

    public boolean update(Course course) throws RoomConflictException, SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check for conflicts for each scheduled day, excluding the current course being updated
                if (course.getRoomId() != null && course.getCourseStartTime() != null && course.getCourseEndTime() != null && course.getDaysOfWeekList() != null) {
                    for (String dayStr : course.getDaysOfWeekList()) {
                        int numericDay = mapStringDayToNumeric(dayStr);
                        if (numericDay > 0) {
                            if (hasTimeConflictForRoomAndDay(conn, course.getRoomId(), numericDay,
                                    course.getCourseStartTime(), course.getCourseEndTime(), course.getCourseId())) {
                                conn.rollback();
                                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                                String startTimeStr = course.getCourseStartTime().format(timeFormatter);
                                String endTimeStr = course.getCourseEndTime().format(timeFormatter);
                                throw new RoomConflictException("Phòng học '" + course.getRoomId() +
                                        "' đã được đăng ký vào ngày '" + dayStr +
                                        "' từ " + startTimeStr + " đến " + endTimeStr + ". Cập nhật thất bại.");
                            }
                        }
                    }
                }

                boolean success = internalUpdate(conn, course);
                if (success) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException | RoomConflictException e) {
                conn.rollback();
                throw e; // Re-throw
            }
        }
    }


    public boolean delete(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, courseId);
            if (success) {
                conn.commit();
            } else {
                conn.rollback();
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting course with ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            // Attempt to rollback on any exception during the delete process
            try (Connection conn = DatabaseConnection.getConnection()) { if(conn != null && !conn.getAutoCommit()) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        }
    }


    public Optional<Course> findById(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getById(conn, courseId));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding course by ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Course> findAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAll(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("[ERROR] findAll() - error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    // --- Other find methods (public wrappers) ---
    // (findCoursesByTeacherId, findCoursesByStudentId, search, findCoursesBySubject etc. are mostly wrappers)
    // They will benefit from the updated getAll/getById and extractCourseFromResultSet

    // The findTotalSessionsForClass method is removed as the 'classes' table was dropped.
    // If you need to get total_sessions for a specific course, use findById(courseId).map(Course::getTotalSessions)

    // Other existing public methods (updateProgress, assignTeacher, etc.)
    // need to be reviewed to ensure they still make sense with the schema changes.
    // For brevity, I'm focusing on the core CRUD and schedule handling.
    // Make sure all public methods use try-with-resources for Connection.

    // Example: ensure other find methods are also updated in their SQL
    public List<Course> findCoursesByTeacherId(String teacherId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByTeacherId(conn, teacherId); // getCoursesByTeacherId internal method already updated
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by teacher ID: " + teacherId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    // ... and so on for other find methods. The key is that their internal SQL and extractCourseFromResultSet are now correct.

    // updateProgress methods seem okay as they only touch the 'progress' column.
    private boolean internalUpdateProgress(Connection conn, String courseId, float progress) throws SQLException {
        String sql = "UPDATE courses SET progress = ? WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setFloat(1, progress);
            statement.setString(2, courseId);
            return statement.executeUpdate() > 0;
        }
    }
    public boolean updateProgress(String courseId, float progress) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // For a single update like this, auto-commit can be true, or manage transaction if preferred
            return internalUpdateProgress(conn, courseId, progress);
        } catch (SQLException e) {
            System.err.println("Error setting progress for course ID: " + courseId + " to " + progress + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public boolean updateProgressBasedOnDate(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Manage transaction
            Course course = getById(conn, courseId); // Fetches course with schedule days
            if (course == null) {
                System.err.println("Error updating progress: Course with ID " + courseId + " not found.");
                conn.rollback();
                return false;
            }
            // calculateProgressBasedOnDate should use course.getDaysOfWeekList() or total_sessions if appropriate
            // The current Course.calculateProgressBasedOnDate() uses date range only, which might be fine.
            float calculatedProgress = (float) course.calculateProgressBasedOnDate();
            boolean success = internalUpdateProgress(conn, courseId, calculatedProgress);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating progress for course ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            try (Connection conn = DatabaseConnection.getConnection()) { if(conn != null && !conn.getAutoCommit()) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        }
    }


    public boolean assignTeacherToCourse(String courseId, String teacherId) throws SQLException {
        String sql = "UPDATE courses SET teacher_id = ? WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teacherId);
            stmt.setString(2, courseId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeTeacherFromCourse(String courseId) throws SQLException {
        String sql = "UPDATE courses SET teacher_id = NULL WHERE course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Lấy danh sách ID của các sinh viên đã đăng ký vào một khóa học cụ thể.
     *
     * @param conn     Connection đến cơ sở dữ liệu.
     * @param courseId ID của khóa học.
     * @return Danh sách các student_id.
     * @throws SQLException nếu có lỗi truy vấn CSDL.
     */
    public List<String> getStudentIdsByCourseId(Connection conn, String courseId) throws SQLException {
        List<String> studentIds = new ArrayList<>();
        String sql = "SELECT student_id FROM enrollment WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    studentIds.add(resultSet.getString("student_id"));
                }
            }
        }
        return studentIds;
    }

    // Removed hasTimeConflict as it was not fully defined and conflict is handled by hasTimeConflictForRoomAndDay now.
}