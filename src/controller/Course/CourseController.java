package src.controller.Course;

import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Classrooms.ClassroomDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Notifications.RoomConflictException;
import src.dao.Person.CourseDAO;
import src.dao.Person.TeacherDAO;
import src.model.ClassSession;
import src.model.system.course.Course;
import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CourseController {
    private static final Logger LOGGER_SERVICE = Logger.getLogger(CourseController.class.getName());

    private CourseDAO courseDAO;
    private ClassSessionDAO classSessionDAO;
    private ClassroomDAO classroomDAO;
    private TeacherDAO teacherDAO;
    private HolidayDAO holidayDAO;
    // private ScheduleDAO scheduleDAO; // Nếu bạn dùng

    // Constructor để inject (tiêm) các DAO
    public CourseController(CourseDAO courseDAO, ClassSessionDAO classSessionDAO,
                         ClassroomDAO classroomDAO, TeacherDAO teacherDAO, HolidayDAO holidayDAO) {
        this.courseDAO = courseDAO;
        this.classSessionDAO = classSessionDAO;
        this.classroomDAO = classroomDAO;
        this.teacherDAO = teacherDAO;
        this.holidayDAO = holidayDAO;
        // this.scheduleDAO = new ScheduleDAO(); // Khởi tạo nếu cần

        // Đảm bảo CourseDAO có các dependency cần thiết
        // Ví dụ: courseDAO.setTeacherDAO(teacherDAO); (nếu CourseDAO cần)
    }


    public boolean createCourseAndGenerateSessions(Course courseToCreate) throws RoomConflictException, SQLException {
        boolean courseSuccessfullySaved = false;
        try {
            // Bước 1: Lưu Course và CourseScheduleDays
            // Gọi hàm save(Course course) của CourseDAO - hàm này tự quản lý transaction của nó.
            courseSuccessfullySaved = courseDAO.save(courseToCreate); //

            if (courseSuccessfullySaved) {
                LOGGER_SERVICE.log(Level.INFO, "Đã lưu thông tin cơ bản và lịch trình cho khóa học: " + courseToCreate.getCourseId());

                // Bước 2: Tạo ClassSessions (Transaction mới, riêng biệt cho việc tạo session)
                Connection sessionConn = null; // Connection riêng cho việc tạo session
                boolean sessionsGeneratedSuccessfully = false;
                try {
                    sessionConn = DatabaseConnection.getConnection();
                    sessionConn.setAutoCommit(false); // Bắt đầu transaction cho việc tạo session

                    List<ClassSession> generatedSessions = classSessionDAO.generateAndSaveSessionsForCourse(
                            sessionConn,
                            courseToCreate, // courseToCreate đã có ID (nếu save ở trên gán ID) và thông tin cần thiết
                            classroomDAO,
                            teacherDAO,
                            holidayDAO,
                            null // scheduleDAO - truyền null nếu không dùng
                    );

                    sessionConn.commit(); // Commit transaction của việc tạo session
                    sessionsGeneratedSuccessfully = true;
                    LOGGER_SERVICE.log(Level.INFO, "Đã tạo thành công {0} buổi học cho khóa: {1}",
                            new Object[]{generatedSessions.size(), courseToCreate.getCourseName()});
                    return true; // Toàn bộ quá trình (lưu course + tạo session) được xem là thành công

                } catch (SQLException sqleSession) {
                    DatabaseConnection.rollback(sessionConn);
                    LOGGER_SERVICE.log(Level.SEVERE, "Lỗi SQL khi tạo ClassSessions cho khóa " + courseToCreate.getCourseId() + ": " + sqleSession.getMessage(), sqleSession);
                    // Quan trọng: Khóa học đã được lưu, nhưng session thì không.
                    // Cần có cơ chế xử lý/thông báo cho người dùng về tình trạng này.
                    // Ví dụ, có thể throw một custom exception để View biết và hiển thị thông báo phù hợp.
                    throw new SQLException("Lưu khóa học thành công, nhưng lỗi khi tạo buổi học chi tiết: " + sqleSession.getMessage(), sqleSession);
                } finally {
                    DatabaseConnection.closeConnection(sessionConn);
                }
            } else {
                LOGGER_SERVICE.log(Level.WARNING, "Không thể lưu thông tin cơ bản cho khóa học: " + courseToCreate.getCourseId());
                return false; // Lưu khóa học thất bại ngay từ đầu
            }
        } catch (RoomConflictException rce) { // Lỗi từ courseDAO.save()
            LOGGER_SERVICE.log(Level.SEVERE, "Xung đột phòng khi tạo khóa học " + courseToCreate.getCourseId() + ": " + rce.getMessage());
            throw rce; // Ném lại để View xử lý
        } catch (SQLException sqleCourse) { // Lỗi SQL khác từ courseDAO.save()
            LOGGER_SERVICE.log(Level.SEVERE, "Lỗi SQL khi lưu Course " + courseToCreate.getCourseId() + ": " + sqleCourse.getMessage(), sqleCourse);
            throw sqleCourse; // Ném lại
        }
    }
}