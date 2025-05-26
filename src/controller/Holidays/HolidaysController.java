package src.controller.Holidays;

import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Person.CourseDAO;
import src.model.ClassSession;
import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import src.model.holidays.HolidaysModel;
import src.model.system.course.Course;
import src.utils.DaoManager;
import src.dao.Holidays.HolidayDAO;
import src.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidaysController {
    private HolidaysModel model;
    private HolidayDAO holidayDAO;
    private static final Logger LOGGER = Logger.getLogger(HolidaysController.class.getName()); // Thêm logger


    /**
     * Constructor không có tham số, khởi tạo đơn giản
     */
    public HolidaysController() {
        // Lấy HolidayDAO từ DaoManager
        //DaoManager daoManager = DaoManager.getInstance();
        //HolidayDAO holidayDAO = daoManager.getHolidayDAO();
        this.holidayDAO = DaoManager.getInstance().getHolidayDAO();

        // Khởi tạo model
        this.model = new HolidaysModel(this.holidayDAO); // << SỬA ĐỔI Ở ĐÂY
    }

    public int getCurrentYear() {
        return model.getCurrentYear();
    }

    public void changeYear(int year) {
        model.setCurrentYear(year);
    }

    public YearMonth getYearMonth(int month) {
        return model.getYearMonth(month);
    }

    public Holiday getHolidayForDate(LocalDate date) {
        return model.getHolidayForDate(date);
    }

    public List<Holiday> getAllHolidays() {
        return model.getAllHolidays();
    }

    public List<HolidayHistory> getRecentHistory(int limit) {
        return model.getRecentHistory(limit);
    }

    /**
     * Thêm holiday mới, không cần thông tin người dùng
     */
    public void addHoliday(Holiday holiday) {
        if (holiday == null) {
            LOGGER.warning("Yêu cầu thêm ngày nghỉ null.");
            return;
        }

        // Gọi trực tiếp holidayDAO.saveHoliday vì nó trả về Holiday đã lưu và tự log
        Holiday savedHoliday = this.holidayDAO.saveHoliday(holiday);

        if (savedHoliday != null && savedHoliday.getId() != null) {
            LOGGER.info("Đã thêm thành công ngày nghỉ: " + savedHoliday.getName() + " (ID: " + savedHoliday.getId() + ")");
            this.model.refreshHolidayCache();

            LOGGER.info("Kích hoạt sắp xếp lại lịch các khóa học do thêm ngày nghỉ: " + savedHoliday.getName());
            rescheduleCoursesAfterHolidayChange(savedHoliday.getStartDate(), savedHoliday.getEndDate());
        } else {
            LOGGER.warning("Không thể thêm ngày nghỉ: " + holiday.getName() + ". Việc sắp xếp lại lịch sẽ không được thực hiện.");
        }
    }

    /**
     * Xóa holiday, log và kích hoạt việc sắp xếp lại lịch học.
     */
    public void deleteHoliday(Long id) {
        if (id == null) {
            LOGGER.warning("Yêu cầu xóa ngày nghỉ với ID null.");
            return;
        }

        // Bước 1: Lấy thông tin chi tiết của ngày nghỉ TRƯỚC KHI xóa, sử dụng holidayDAO trực tiếp
        Holiday holidayToDelete = this.holidayDAO.findHolidayById(id);

        if (holidayToDelete == null) {
            LOGGER.warning("Ngày nghỉ với ID: " + id + " không tìm thấy để xóa.");
            return;
        }

        LocalDate deletedStartDate = holidayToDelete.getStartDate();
        LocalDate deletedEndDate = holidayToDelete.getEndDate();
        String deletedHolidayName = holidayToDelete.getName();

        // Bước 2: Thực hiện xóa ngày nghỉ thông qua holidayDAO trực tiếp
        boolean isSuccessfullyDeletedFromDB = this.holidayDAO.deleteHoliday(id); // HolidayDAO.deleteHoliday tự log

        if (isSuccessfullyDeletedFromDB) {
            LOGGER.info("Đã xóa thành công ngày nghỉ '" + deletedHolidayName + "' (ID: " + id + ") khỏi cơ sở dữ liệu.");
            // HolidayDAO.deleteHoliday đã tự log "SYSTEM" vào holiday_history.

            // Refresh cache của model sau khi CSDL thay đổi
            this.model.refreshHolidayCache();

            LOGGER.info("Kích hoạt sắp xếp lại lịch các khóa học do xóa ngày nghỉ: " + deletedHolidayName);
            rescheduleCoursesAfterHolidayChange(deletedStartDate, deletedEndDate);
        } else {
            LOGGER.warning("Không thể xóa ngày nghỉ '" + deletedHolidayName + "' (ID: " + id + ") khỏi cơ sở dữ liệu.");
        }
    }


    private void rescheduleCoursesAfterHolidayChange(LocalDate affectedPeriodStart, LocalDate affectedPeriodEnd) {
        LOGGER.info("Bắt đầu quá trình rescheduleCoursesAfterHolidayChange cho khoảng: " + affectedPeriodStart + " đến " + affectedPeriodEnd);
        DaoManager daoManager = DaoManager.getInstance();
        CourseDAO courseDAO = daoManager.getCourseDAO();
        ClassSessionDAO classSessionDAO = daoManager.getClassSessionDAO();
        HolidayDAO holidayDAOForReschedule = daoManager.getHolidayDAO();

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            List<Course> coursesToReschedule = courseDAO.findCoursesEndingOnOrAfter(conn, affectedPeriodStart);

            if (coursesToReschedule.isEmpty()) {
                LOGGER.info("Không có khóa học nào cần sắp xếp lại lịch.");
                conn.commit();
                return;
            }

            LOGGER.info("Tìm thấy " + coursesToReschedule.size() + " khóa học để kiểm tra và sắp xếp lại lịch.");

            for (Course course : coursesToReschedule) {
                LOGGER.info("Đang xử lý khóa học: " + course.getCourseId() + " - " + course.getCourseName());

                int deletedSessions = classSessionDAO.deleteAllSessionsByCourseId(conn, course.getCourseId());
                LOGGER.fine("Đã xóa " + deletedSessions + " session cũ của khóa " + course.getCourseId());

                // generateAndSaveSessionsForCourse trả về List<ClassSession>
                List<ClassSession> newSessions = classSessionDAO.generateAndSaveSessionsForCourse(
                        conn,
                        course,
                        daoManager.getClassroomDAO(),
                        daoManager.getTeacherDAO(),
                        holidayDAOForReschedule,
                        null
                );

                LocalDate newActualEndDate = null;
                if (newSessions != null && !newSessions.isEmpty()) {
                    // Lấy ngày của session cuối cùng.
                    // ClassSession.java có getDate() để lấy sessionDate.
                    ClassSession lastSession = newSessions.get(newSessions.size() - 1);
                    newActualEndDate = lastSession.getDate(); // SỬ DỤNG getDate() từ ClassSession.java
                }

                if (newActualEndDate != null) {
                    if (course.getEndDate() == null || !course.getEndDate().equals(newActualEndDate)) {
                        LOGGER.info("Cập nhật ngày kết thúc cho khóa " + course.getCourseId() + " từ " + course.getEndDate() + " thành " + newActualEndDate);
                        courseDAO.updateCourseEndDate(conn, course.getCourseId(), newActualEndDate);
                    } else {
                        LOGGER.info("Ngày kết thúc của khóa " + course.getCourseId() + " không thay đổi (" + newActualEndDate + ").");
                    }
                } else {
                    LOGGER.severe("KHÔNG THỂ XÁC ĐỊNH NGÀY KẾT THÚC MỚI cho khóa " + course.getCourseId() + " sau khi tạo lại session (có thể không có session nào được tạo). Transaction sẽ được rollback.");
                    throw new SQLException("Không thể tạo lại lịch hoặc xác định ngày kết thúc mới cho khóa: " + course.getCourseId() + " do không có session nào được tạo hoặc lỗi dữ liệu session.");
                }
            }

            conn.commit();
            LOGGER.info("Đã sắp xếp lại thành công lịch học cho " + coursesToReschedule.size() + " khóa học bị ảnh hưởng.");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi SQL trong quá trình sắp xếp lại lịch các khóa học. Đang rollback...", e);
            DatabaseConnection.rollback(conn);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi không mong muốn trong quá trình sắp xếp lại lịch các khóa học. Đang rollback...", e);
            DatabaseConnection.rollback(conn);
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "N/A";
        return String.format("%02d/%02d/%d",
                date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}