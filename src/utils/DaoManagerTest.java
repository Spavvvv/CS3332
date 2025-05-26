package src.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Giả sử các lớp DAO của bạn nằm trong package src.dao.*
// Nếu chúng ở nơi khác, bạn cần điều chỉnh các import này cho phù hợp.
// Để các test này biên dịch được, bạn cần có các lớp DAO này (ít nhất là các constructor rỗng hoặc mặc định của chúng).
import src.dao.Attendance.AbsenceRecordDAO;
import src.dao.Attendance.AttendanceDAO;
import src.dao.Attendance.HomeworkDAO;
import src.dao.Attendance.HomeworkSubmissionDAO;
import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Classrooms.ClassroomDAO;
import src.dao.Dashboard.DashboardDAO;
import src.dao.Details.DetailsDAO;
import src.dao.Holidays.HolidayDAO;
import src.dao.Person.CourseDAO;
import src.dao.Person.ParentDAO;
import src.dao.Person.StudentDAO;
import src.dao.Person.TeacherDAO;
import src.dao.Report.*;
import src.dao.Schedule.RoomScheduleDAO;
import src.dao.Schedule.ScheduleDAO;
import src.utils.DaoManager; // Import lớp DaoManager của bạn

/**
 * Unit tests cho lớp DaoManager.
 */
class DaoManagerTest {

    /**
     * Kiểm tra xem phương thức getInstance() có trả về cùng một instance mỗi lần gọi không (tính singleton).
     */
    @Test
    void testGetInstance_returnsSameInstance() {
        DaoManager instance1 = DaoManager.getInstance();
        DaoManager instance2 = DaoManager.getInstance();

        assertNotNull(instance1, "Instance 1 không nên là null.");
        assertNotNull(instance2, "Instance 2 không nên là null.");
        assertSame(instance1, instance2, "Cả hai instance phải là cùng một đối tượng.");
    }

    /**
     * Kiểm tra xem các DAO có được khởi tạo và có thể truy cập thông qua các getter không.
     * Test này cũng gián tiếp kiểm tra việc khởi tạo thành công của DaoManager.
     */
    @Test
    void testDaoInitialization_allDaosAreAccessible() {
        DaoManager daoManager = DaoManager.getInstance();

        assertNotNull(daoManager, "DaoManager instance không nên là null.");

        // Kiểm tra một vài DAO đại diện, bao gồm cả DAO mới được thêm vào
        assertNotNull(daoManager.getStudentDAO(), "StudentDAO không nên là null.");
        assertNotNull(daoManager.getCourseDAO(), "CourseDAO không nên là null.");
        assertNotNull(daoManager.getAttendanceDAO(), "AttendanceDAO không nên là null.");
        assertNotNull(daoManager.getHomeworkDAO(), "HomeworkDAO không nên là null.");
        assertNotNull(daoManager.getHomeworkSubmissionDAO(), "HomeworkSubmissionDAO không nên là null."); // DAO mới
        assertNotNull(daoManager.getTeacherDAO(), "TeacherDAO không nên là null.");
        assertNotNull(daoManager.getClassSessionDAO(), "ClassSessionDAO không nên là null.");
        // Bạn có thể thêm các assertNotNull cho tất cả các DAO khác nếu muốn kiểm tra toàn diện
        assertNotNull(daoManager.getParentDAO(), "ParentDAO không nên là null.");
        assertNotNull(daoManager.getAbsenceRecordDAO(), "AbsenceRecordDAO không nên là null.");
        assertNotNull(daoManager.getDashboardDAO(), "DashboardDAO không nên là null.");
        assertNotNull(daoManager.getClassroomDAO(), "ClassroomDAO không nên là null.");
        assertNotNull(daoManager.getDetailsDAO(), "DetailsDAO không nên là null.");
        assertNotNull(daoManager.getHolidayDAO(), "HolidayDAO không nên là null.");
        assertNotNull(daoManager.getReportDAO(), "ReportDAO không nên là null.");
        assertNotNull(daoManager.getRoomScheduleDAO(), "RoomScheduleDAO không nên là null.");
        assertNotNull(daoManager.getScheduleDAO(), "ScheduleDAO không nên là null.");
        assertNotNull(daoManager.getTeachingStatisticsDAO(), "TeachingStatisticsDAO không nên là null.");
        assertNotNull(daoManager.getTeacherMonthlyStatisticsDAO(), "TeacherMonthlyStatisticsDAO không nên là null.");
        assertNotNull(daoManager.getTeacherQuarterlyStatisticsDAO(), "TeacherQuarterlyStatisticsDAO không nên là null.");
        assertNotNull(daoManager.getTeacherYearlyStatisticsDAO(), "TeacherYearlyStatisticsDAO không nên là null.");
    }

    /**
     * Kiểm tra cụ thể việc HomeworkSubmissionDAO được khởi tạo chính xác.
     * Test này hơi thừa nếu đã có testDaoInitialization_allDaosAreAccessible bao gồm nó,
     * nhưng có thể hữu ích nếu bạn muốn tập trung vào một DAO cụ thể.
     */
    @Test
    void testGetHomeworkSubmissionDAO_returnsNonNullInstance() {
        DaoManager daoManager = DaoManager.getInstance();
        HomeworkSubmissionDAO homeworkSubmissionDAO = daoManager.getHomeworkSubmissionDAO();

        assertNotNull(homeworkSubmissionDAO, "HomeworkSubmissionDAO không nên là null sau khi lấy từ DaoManager.");
    }
}