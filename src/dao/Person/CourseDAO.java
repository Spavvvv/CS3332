package src.dao.Person;

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

/**
 * Data Access Object for Course entities.
 */
public class CourseDAO {
    private StudentDAO studentDAO;
    private TeacherDAO teacherDAO;

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

    private boolean internalInsert(Connection conn, Course course) throws SQLException {
        String sql = "INSERT INTO courses (course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            statement.setString(10, course.getClassId());
            statement.setFloat(11, course.getProgress());
            System.out.println("[DEBUG - internalInsert] dayOfWeek: " + course.getDayOfWeek());
            statement.setString(12, course.getDayOfWeek());
            return statement.executeUpdate() > 0;
        }
    }

    private boolean internalUpdate(Connection conn, Course course) throws SQLException {
        String sql = "UPDATE courses SET course_name = ?, subject = ?, start_date = ?, end_date = ?, " +
                "start_time = ?, end_time = ?, teacher_id = ?, room_id = ?, class_id = ?, " +
                "progress = ?, day_of_week = ? WHERE course_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getSubject());
            statement.setDate(3, course.getStartDate() != null ? Date.valueOf(course.getStartDate()) : null);
            statement.setDate(4, course.getEndDate() != null ? Date.valueOf(course.getEndDate()) : null);
            statement.setTime(5, course.getCourseStartTime() != null ? Time.valueOf(course.getCourseStartTime()) : null);
            statement.setTime(6, course.getCourseEndTime() != null ? Time.valueOf(course.getCourseEndTime()) : null);
            statement.setString(7, course.getTeacher() != null ? course.getTeacher().getId() : null);
            statement.setString(8, course.getRoomId());
            statement.setString(9, course.getClassId());
            statement.setFloat(10, course.getProgress());
            statement.setString(11, course.getDayOfWeek());
            statement.setString(12, course.getCourseId());
            return statement.executeUpdate() > 0;
        }
    }

    private boolean internalDelete(Connection conn, String courseId) throws SQLException {
        removeAllStudentsFromCourse(conn, courseId);
        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            return statement.executeUpdate() > 0;
        }
    }

    Course getById(Connection conn, String courseId) throws SQLException {
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, start_time, end_time, " +
                "teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE course_id = ?";
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
                "teacher_id, room_id, class_id, progress, day_of_week FROM courses";
        System.out.println("[DEBUG] getAll() - Executing SQL: " + sql);

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                courses.add(extractCourseFromResultSet(conn, resultSet));
            }
        }
        return courses;
    }

    private boolean addStudentToCourse(Connection conn, String idForEnrollmentTableClassColumn, String studentId) throws SQLException {
        System.out.println("[CourseDAO Private] addStudentToCourse được gọi. idForEnrollmentTableClassColumn: " + idForEnrollmentTableClassColumn + ", studentId: " + studentId);
        String checkDuplicateSql = "SELECT COUNT(*) FROM enrollment WHERE student_id = ? AND course_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkDuplicateSql)) {
            checkStmt.setString(1, studentId);
            checkStmt.setString(2, idForEnrollmentTableClassColumn);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.err.println("[CourseDAO Private] Lỗi: Sinh viên ID: " + studentId + " đã tồn tại trong khóa học/lớp ID: " + idForEnrollmentTableClassColumn);
                    return false;
                }
            }
        }
        System.out.println("[CourseDAO Private] Kiểm tra trùng lặp hoàn tất (sinh viên chưa có trong khóa học/lớp này).");
        String sql = "INSERT INTO enrollment (enrollment_id, student_id, course_id, enrollment_date, status) VALUES (?, ?, ?, ?, ?)";
        System.out.println("[CourseDAO Private] SQL để INSERT: " + sql);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, studentId);
            statement.setString(3, idForEnrollmentTableClassColumn);
            statement.setDate(4, Date.valueOf(LocalDate.now()));
            statement.setString(5, "Active");
            System.out.println("[CourseDAO Private] Đang thực thi INSERT...");
            int rowsAffected = statement.executeUpdate();
            System.out.println("[CourseDAO Private] Số dòng bị ảnh hưởng bởi INSERT: " + rowsAffected);
            return rowsAffected > 0;
        }
    }

    public boolean addStudentToCourse(String courseIdForEnrollment, String studentId) {
        System.out.println("[CourseDAO Public] addStudentToCourse được gọi. courseIdForEnrollment: " + courseIdForEnrollment + ", studentId: " + studentId);
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("[CourseDAO Public] Đã lấy được kết nối.");
            Course course = getById(conn, courseIdForEnrollment);
            if (course == null) {
                System.err.println("[CourseDAO Public] Lỗi: Không tìm thấy Khóa học với ID " + courseIdForEnrollment + " trong bảng 'courses'.");
                return false;
            }
            System.out.println("[CourseDAO Public] Khóa học " + courseIdForEnrollment + " đã được xác nhận tồn tại.");
            checkStudentDAODependency();
            System.out.println("[CourseDAO Public] StudentDAO dependency OK.");
            System.out.println("[CourseDAO Public] Đang kiểm tra tính hợp lệ của studentId: " + studentId);
            Student student = studentDAO.getStudentById(conn, studentId);
            // Dòng debug này có thể gây NullPointerException nếu student.getParentName() là null
            // if(student != null && student.getParentName() == null)
            // {
            //     System.out.println("toang");
            // } else if (student != null) {System.out.println(student.getParentName());}


            if (student == null) {
                System.err.println("[CourseDAO Public] Lỗi: Không tìm thấy sinh viên với ID: " + studentId + " trong bảng 'students'.");
                return false;
            }
            System.out.println("[CourseDAO Public] Đã xác nhận sinh viên tồn tại: " + student.getName());
            System.out.println("[CourseDAO Public] Đang gọi private addStudentToCourse với enrollment.course_id: " + courseIdForEnrollment);
            return addStudentToCourse(conn, courseIdForEnrollment, studentId);
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

    private boolean removeStudentFromCourse(Connection conn, String keyForEnrollment, String studentId) throws SQLException {
        String sql = "DELETE FROM enrollment WHERE course_id = ? AND student_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, keyForEnrollment);
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    private void removeAllStudentsFromCourse(Connection conn, String keyForEnrollment) throws SQLException {
        String sql = "DELETE FROM enrollment WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, keyForEnrollment);
            statement.executeUpdate();
        }
    }

    List<Course> getCoursesByTeacherId(Connection conn, String teacherId) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE teacher_id = ?";
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
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT c.course_id, c.course_name, c.subject, c.start_date, c.end_date, c.start_time, c.end_time, c.teacher_id, c.room_id, c.class_id, c.progress, c.day_of_week FROM courses c " +
                "JOIN enrollment e ON c.course_id = e.course_id " + // Nên JOIN trên e.course_id (nếu enrollment lưu course_id)
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

    List<Course> searchCourses(Connection conn, String searchTerm) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE course_name LIKE ? OR subject LIKE ?";
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

    List<Course> getCoursesBySubject(Connection conn, String subject) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE subject = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, subject);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCoursesByDateRange(Connection conn, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE " +
                "NOT (end_date < ? OR start_date > ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(startDate));
            statement.setDate(2, Date.valueOf(endDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getActiveCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE " +
                "start_date <= ? AND end_date >= ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            statement.setDate(2, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getUpcomingCourses(Connection conn) throws SQLException {
        List<Course> courses = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE start_date > ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(today));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCompletedCourses(Connection conn, LocalDate referenceDate) throws SQLException {
        List<Course> courses = new ArrayList<>();
        // Câu lệnh SELECT này cũng cần cột day_of_week nếu muốn hiển thị
        String sql = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, day_of_week FROM courses WHERE end_date < ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(referenceDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(extractCourseFromResultSet(conn, resultSet));
                }
            }
        }
        return courses;
    }

    List<Course> getCompletedCourses(Connection conn) throws SQLException {
        return getCompletedCourses(conn, LocalDate.now());
    }

    private boolean internalUpdateProgress(Connection conn, String courseId, float progress) throws SQLException {
        String sql = "UPDATE courses SET progress = ? WHERE course_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setFloat(1, progress);
            statement.setString(2, courseId);
            return statement.executeUpdate() > 0;
        }
    }

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

    public List<Student> getStudentsByCourseId(Connection conn, String courseIdFromCoursesTable) throws SQLException {
        checkStudentDAODependency();
        List<Student> students = new ArrayList<>();
        String actualClassIdForQuery = null;
        String fetchClassIdSql = "SELECT class_id FROM courses WHERE course_id = ?";
        try (PreparedStatement fetchStmt = conn.prepareStatement(fetchClassIdSql)) {
            fetchStmt.setString(1, courseIdFromCoursesTable);
            try (ResultSet rs = fetchStmt.executeQuery()) {
                if (rs.next()) {
                    actualClassIdForQuery = rs.getString("class_id");
                } else {
                    return students;
                }
            }
        }

        if (actualClassIdForQuery == null || actualClassIdForQuery.trim().isEmpty()) {
            return students;
        }

        String sql = "SELECT e.student_id FROM enrollment e WHERE e.class_id = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, actualClassIdForQuery);
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

    private Course extractCourseFromResultSet(Connection conn, ResultSet resultSet) throws SQLException {
        String courseId = resultSet.getString("course_id");
        String courseName = resultSet.getString("course_name");
        String subject = resultSet.getString("subject");

        LocalDate startDate = null;
        Date startDateSql = resultSet.getDate("start_date");
        if (startDateSql != null) {
            startDate = startDateSql.toLocalDate();
        }

        LocalDate endDate = null;
        Date endDateSql = resultSet.getDate("end_date");
        if (endDateSql != null) {
            endDate = endDateSql.toLocalDate();
        }

        Course course = new Course(courseId, courseName, subject, startDate, endDate);

        Time startTimeSql = resultSet.getTime("start_time");
        if (startTimeSql != null) {
            course.setCourseSessionStartTime(startTimeSql.toLocalTime());
        }

        Time endTimeSql = resultSet.getTime("end_time");
        if (endTimeSql != null) {
            course.setCourseSessionEndTime(endTimeSql.toLocalTime());
        }

        String teacherId = resultSet.getString("teacher_id"); // Dòng này gây lỗi nếu cột không có hoặc tên sai
        String roomId = resultSet.getString("room_id");
        String classId = resultSet.getString("class_id");
        float progress = resultSet.getFloat("progress");
        String dayOfWeek = resultSet.getString("day_of_week"); // Đảm bảo tên cột này đúng
        System.out.println("[DEBUG - extractCourse] courseId: " + courseId + ", dayOfWeek from DB: " + dayOfWeek); // THÊM DÒNG NÀY

        course.setRoomId(roomId);
        course.setClassId(classId);
        course.setProgress(progress);
        course.setDayOfWeek(dayOfWeek);

        if (teacherId != null && !teacherId.trim().isEmpty()) {
            checkTeacherDAODependency();
            if (this.teacherDAO != null) {
                Teacher teacher = this.teacherDAO.getById(conn, teacherId); // Giả định TeacherDAO có getById(conn, id)
                course.setTeacher(teacher);
            } else {
                System.err.println("[WARN] extractCourseFromResultSet - TeacherDAO is null, cannot fetch teacher for ID: " + teacherId);
            }
        }
        course.setStudents(new ArrayList<>()); // Tạm thời không load students
        return course;
    }

    public boolean save(Course course) throws RoomConflictException, SQLException {
        // 1. Kiểm tra xung đột lịch trước khi thực hiện bất kỳ thao tác CSDL nào liên quan đến việc lưu
        // Phương thức hasTimeConflictForRoom có thể ném SQLException nếu có lỗi truy vấn
        if (hasTimeConflictForRoom(course.getRoomId(), course.getDayOfWeek(), course.getCourseStartTime(), course.getCourseEndTime())) {
            // Định dạng thời gian để thông báo lỗi thân thiện hơn
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String startTimeStr = course.getCourseStartTime() != null ? course.getCourseStartTime().format(timeFormatter) : "N/A";
            String endTimeStr = course.getCourseEndTime() != null ? course.getCourseEndTime().format(timeFormatter) : "N/A";
            String roomInfo = course.getRoomId(); // Bạn có thể lấy tên phòng nếu cần và có sẵn

            // Tạo thông báo lỗi chi tiết
            String conflictMessage = "Không thể lưu: Phòng học '" + roomInfo +
                    "' đã được đăng ký vào ngày '" + course.getDayOfWeek() +
                    "' từ " + startTimeStr + " đến " + endTimeStr + ". " +
                    "Vui lòng chọn thời gian hoặc phòng khác.";
            throw new RoomConflictException(conflictMessage);
        }

        // 2. Nếu không có xung đột, tiếp tục với quá trình lưu thông thường
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            boolean success = internalInsert(conn, course); // internalInsert có thể ném SQLException

            if (success) {
                conn.commit(); // Hoàn tất transaction nếu thành công
                System.out.println("[DEBUG - CourseDAO.save] Course saved successfully: " + course.getCourseName());
                return true;
            } else {
                conn.rollback(); // Hoàn tác transaction nếu internalInsert thất bại
                System.err.println("[DEBUG - CourseDAO.save] Transaction rolled back while saving course (internalInsert failed).");
                // Nếu internalInsert trả về false mà không ném lỗi, bạn có thể coi đây là một lỗi lưu không xác định
                return false;
            }
        } catch (SQLException e) {
            // Bắt các lỗi SQL từ getConnection(), setAutoCommit(), internalInsert(), commit(), rollback()
            System.err.println("[ERROR - CourseDAO.save] SQL Error saving course: " + e.getMessage());
            e.printStackTrace();
            throw e; // Ném lại SQLException để lớp gọi có thể xử lý nếu cần
        }
        // Không cần bắt IllegalStateException ở đây nếu nó chủ yếu cho các kiểm tra phụ thuộc
    }

    public boolean update(Course course) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalUpdate(conn, course);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating course: " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return false;
        }
    }

    public boolean delete(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            boolean success = internalDelete(conn, courseId);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException e) {
            System.err.println("Error deleting course with ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return false;
        }
    }

    public Optional<Course> findById(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return Optional.ofNullable(getById(conn, courseId));
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding course by ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return Optional.empty();
        }
    }

    public List<Course> findAll() {
        System.out.println("[DEBUG] findAll() - Starting fetch.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("[DEBUG] findAll() - Database connection is null.");
                return new ArrayList<>();
            }
            System.out.println("[DEBUG] findAll() - Connection established.");
            List<Course> courses = getAll(conn);
            System.out.println("[DEBUG] findAll() - Retrieved courses: " + (courses != null ? courses.size() : "null list"));
            return courses;
        } catch (SQLException e) {
            System.err.println("[ERROR] findAll() - SQL error: " + e.getMessage() + " SQLState: " + e.getSQLState() + " ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
            return new ArrayList<>();
        } catch (IllegalStateException e) {
            System.err.println("[ERROR] findAll() - Dependency error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean removeStudentFromCourse(String courseId, String studentId) {
        String sql = "DELETE FROM enrollment WHERE course_id = ? AND student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, courseId);
            statement.setString(2, studentId);
            int rowsAffected = statement.executeUpdate();
            System.out.println("DEBUG: Rows affected by delete: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("ERROR: Không thể xóa học viên " + studentId + " khỏi khóa học " + courseId);
            e.printStackTrace();
            return false;
        }
    }

    public List<Course> findCoursesByTeacherId(String teacherId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByTeacherId(conn, teacherId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by teacher ID: " + teacherId + ": " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesByStudentId(String studentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByStudentId(conn, studentId);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses for student ID: " + studentId + ": " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> search(String searchTerm) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return searchCourses(conn, searchTerm);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error searching courses for term: '" + searchTerm + "': " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesBySubject(String subject) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesBySubject(conn, subject);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by subject: " + subject + ": " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findCoursesByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCoursesByDateRange(conn, startDate, endDate);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding courses by date range (" + startDate + " to " + endDate + "): " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findActiveCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getActiveCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding active courses: " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findUpcomingCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getUpcomingCourses(conn);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding upcoming courses: " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findCompletedCourses() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCompletedCourses(conn, LocalDate.now());
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding completed courses: " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public List<Course> findCompletedCourses(LocalDate referenceDate) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getCompletedCourses(conn, referenceDate);
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error finding completed courses: " + e.getMessage());
            e.printStackTrace(); // Thêm stack trace để debug
            return new ArrayList<>();
        }
    }

    public boolean updateProgressBasedOnDate(String courseId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            Course course = getById(conn, courseId);
            if (course == null) {
                System.err.println("Error updating progress: Course with ID " + courseId + " not found.");
                conn.rollback();
                return false;
            }
            float calculatedProgress = (float) course.calculateProgressBasedOnDate();
            boolean success = internalUpdateProgress(conn, courseId, calculatedProgress);
            if (success) conn.commit();
            else conn.rollback();
            return success;
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error updating progress for course ID: " + courseId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProgress(String courseId, float progress) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return internalUpdateProgress(conn, courseId, progress);
        } catch (SQLException e) {
            System.err.println("Error setting progress for course ID: " + courseId + " to " + progress + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasTimeConflict(Course course1, Course course2) {
        if (course1 == null || course2 == null) {
            return false;
        }
        if (!course1.overlapsDateRange(course2)) {
            return false;
        }
        LocalTime start1 = course1.getCourseStartTime();
        LocalTime end1 = course1.getCourseEndTime();
        LocalTime start2 = course2.getCourseStartTime();
        LocalTime end2 = course2.getCourseEndTime();
        if (start1 != null && end1 != null && start2 != null && end2 != null) {
            boolean timesOverlap = start1.isBefore(end2) && start2.isBefore(end1);
            return timesOverlap;
        }
        return false;
    }
    public boolean hasTimeConflictForRoom(String roomId, String dayOfWeek, LocalTime newStartTime, LocalTime newEndTime) throws SQLException {
        // Kiểm tra các tham số đầu vào cơ bản
        if (roomId == null || roomId.trim().isEmpty() ||
                dayOfWeek == null || dayOfWeek.trim().isEmpty() ||
                newStartTime == null || newEndTime == null) {
            System.err.println("[CourseDAO] Tham số không hợp lệ (null hoặc rỗng) cho hasTimeConflictForRoom.");
            return false;
        }

        String sql = "SELECT COUNT(*) FROM courses " +
                "WHERE room_id = ? AND day_of_week = ? " +
                "AND start_time < ? AND end_time > ?"; // Logic chồng chéo thời gian

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomId);
            stmt.setString(2, dayOfWeek);
            // courses.start_time < newEndTime
            stmt.setTime(3, Time.valueOf(newEndTime));
            // courses.end_time > newStartTime
            stmt.setTime(4, Time.valueOf(newStartTime));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Nếu count > 0, có xung đột
                }
            }
        }
        return false; // Mặc định không có xung đột
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
}
