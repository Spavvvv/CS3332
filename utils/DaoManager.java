package utils;

import utils.DatabaseConnection;

// Assuming these DAOs follow the pattern of having a default constructor
// and setter methods for dependencies (e.g., setStudentDAO, setTeacherDAO, etc.)
import src.dao.AttendanceDAO;
import src.dao.CourseDAO;
import src.dao.ParentDAO;
import src.dao.StudentDAO;
import src.dao.TeacherDAO;
import src.dao.AbsenceRecordDAO;
import src.dao.ClassSessionDAO;
import src.dao.DashboardDAO;
import src.dao.ClassroomDAO;
import src.dao.DetailsDAO;
import src.dao.HolidayDAO;
import src.dao.ReportDAO;
import src.dao.RoomScheduleDAO;
import src.dao.ScheduleDAO;
import src.dao.StudentScheduleDAO;
import src.dao.TeachingStatisticsDAO;
import src.dao.TeacherMonthlyStatisticsDAO;
import src.dao.TeacherQuarterlyStatisticsDAO;
import src.dao.TeacherYearlyStatisticsDAO; // Import TeacherYearlyStatisticsDAO


/**
 * Manages the creation and dependency injection of DAO instances.
 * This class acts as a central singleton point for accessing configured DAO instances.
 * It is responsible for ensuring DAOs are properly wired with their dependencies.
 * Public methods in individual DAOs manage their own database connections.
 */
public class DaoManager {

    private static volatile DaoManager instance; // Use volatile for thread safety

    private StudentDAO studentDAO;
    private ParentDAO parentDAO;
    private CourseDAO courseDAO;
    private AttendanceDAO attendanceDAO;
    private TeacherDAO teacherDAO;
    private AbsenceRecordDAO absenceRecordDAO;
    private ClassSessionDAO classSessionDAO;
    private DashboardDAO dashboardDAO;
    private ClassroomDAO classroomDAO;
    private DetailsDAO detailsDAO;
    private HolidayDAO holidayDAO;
    private ReportDAO reportDAO;
    private RoomScheduleDAO roomScheduleDAO;
    private ScheduleDAO scheduleDAO;
    private StudentScheduleDAO studentScheduleDAO;
    private TeachingStatisticsDAO teachingStatisticsDAO;
    private TeacherMonthlyStatisticsDAO teacherMonthlyStatisticsDAO;
    private TeacherQuarterlyStatisticsDAO teacherQuarterlyStatisticsDAO;
    private TeacherYearlyStatisticsDAO teacherYearlyStatisticsDAO; // Add TeacherYearlyStatisticsDAO instance variable
    // Add other DAOs here

    /**
     * Private constructor to prevent direct instantiation.
     * Initializes and wires the DAO dependencies.
     */
    private DaoManager() {
        // 1. Create instances of all DAOs
        studentDAO = new StudentDAO();
        parentDAO = new ParentDAO();
        courseDAO = new CourseDAO();
        attendanceDAO = new AttendanceDAO();
        teacherDAO = new TeacherDAO();
        absenceRecordDAO = new AbsenceRecordDAO();
        classSessionDAO = new ClassSessionDAO();
        dashboardDAO = new DashboardDAO();
        classroomDAO = new ClassroomDAO();
        detailsDAO = new DetailsDAO();
        holidayDAO = new HolidayDAO();
        reportDAO = new ReportDAO();
        roomScheduleDAO = new RoomScheduleDAO();
        scheduleDAO = new ScheduleDAO();
        studentScheduleDAO = new StudentScheduleDAO();
        teachingStatisticsDAO = new TeachingStatisticsDAO();
        teacherMonthlyStatisticsDAO = new TeacherMonthlyStatisticsDAO();
        teacherQuarterlyStatisticsDAO = new TeacherQuarterlyStatisticsDAO();
        teacherYearlyStatisticsDAO = new TeacherYearlyStatisticsDAO(); // Instantiate TeacherYearlyStatisticsDAO
        // Instantiate other DAOs here

        // 2. Wire the dependencies using setter methods
        // Check the dependencies required by each DAO and set them here.

        // Setters in StudentDAO (Example dependencies)
        studentDAO.setCourseDAO(courseDAO); // If StudentDAO needs CourseDAO (e.g., to list courses student is in)
        // studentDAO.setAttendanceDAO(attendanceDAO); // If StudentDAO needs AttendanceDAO
        // studentDAO.setAbsenceRecordDAO(absenceRecordDAO); // If StudentDAO needs AbsenceRecordDAO
        //studentDAO.setClassSessionDAO(classSessionDAO); // If StudentDAO needs ClassSessionDAO
        // studentDAO.setDashboardDAO(dashboardDAO); // If StudentDAO needs DashboardDAO (unlikely)
        // studentDAO.setClassroomDAO(classroomDAO); // If StudentDAO needs ClassroomDAO (e.g., to find student's assigned classroom)
        // studentDAO.setDetailsDAO(detailsDAO); // If StudentDAO needs DetailsDAO (e.g., to get student's grades/details)
        //studentDAO.setScheduleDAO(scheduleDAO); // If StudentDAO needs ScheduleDAO (e.g., to get student's full schedule)
        //studentDAO.setStudentScheduleDAO(studentScheduleDAO); // If StudentDAO specifically needs StudentScheduleDAO


        // Setters in ParentDAO (Example dependencies)
        parentDAO.setStudentDAO(studentDAO); // If ParentDAO needs StudentDAO (e.g., to list parent's children)
        // parentDAO.setAttendanceDAO(attendanceDAO); // If ParentDAO needs AttendanceDAO
        // parentDAO.setCourseDAO(courseDAO); // If ParentDAO needs CourseDAO
        // parentDAO.setAbsenceRecordDAO(absenceRecordDAO); // If ParentDAO needs AbsenceRecordDAO
        // parentDAO.setClassSessionDAO(classSessionDAO); // If ParentDAO needs ClassSessionDAO
        // parentDAO.setDashboardDAO(dashboardDAO); // If ParentDAO needs DashboardDAO (unlikely)
        // parentDAO.setClassroomDAO(classroomDAO); // If ParentDAO needs ClassroomDAO (unlikely)
        // parentDAO.setDetailsDAO(detailsDAO); // If ParentDAO needs DetailsDAO (e.g., to see children's grades/details)
        // parentDAO.setScheduleDAO(scheduleDAO); // If ParentDAO needs ScheduleDAO (unlikely)
        //parentDAO.setStudentScheduleDAO(studentScheduleDAO); // If ParentDAO needs StudentScheduleDAO


        // Setters in CourseDAO
        courseDAO.setStudentDAO(studentDAO);
        courseDAO.setTeacherDAO(teacherDAO);
        // courseDAO.setAttendanceDAO(attendanceDAO);
        // courseDAO.setParentDAO(parentDAO);
        // courseDAO.setAbsenceRecordDAO(absenceRecordDAO);
        //courseDAO.setClassSessionDAO(classSessionDAO);
        // courseDAO.setDashboardDAO(dashboardDAO);
        // courseDAO.setClassroomDAO(classroomDAO);
        //courseDAO.setDetailsDAO(detailsDAO); // If CourseDAO needs DetailsDAO (e.g., to get course details like grades)
        // courseDAO.setScheduleDAO(scheduleDAO); // If CourseDAO needs ScheduleDAO (e.g., to find course schedule)


        // Setters in AttendanceDAO
        attendanceDAO.setStudentDAO(studentDAO);
        attendanceDAO.setClassSessionDAO(classSessionDAO);
        // attendanceDAO.setCourseDAO(courseDAO);
        // attendanceDAO.setParentDAO(parentDAO);
        // attendanceDAO.setAbsenceRecordDAO(absenceRecordDAO);
        // attendanceDAO.setDashboardDAO(dashboardDAO);
        // attendanceDAO.setClassroomDAO(classroomDAO);
        // attendanceDAO.setDetailsDAO(detailsDAO); // If AttendanceDAO needs DetailsDAO (unlikely)
        // attendanceDAO.setScheduleDAO(scheduleDAO); // If AttendanceDAO needs ScheduleDAO (unlikely)


        // Setters in TeacherDAO (Example dependencies)
        teacherDAO.setCourseDAO(courseDAO); // If TeacherDAO needs CourseDAO (e.g., to list courses taught)
        // teacherDAO.setStudentDAO(studentDAO); // If TeacherDAO needs StudentDAO
        // teacherDAO.setAttendanceDAO(attendanceDAO); // If TeacherDAO needs AttendanceDAO
        // teacherDAO.setAbsenceRecordDAO(absenceRecordDAO); // If TeacherDAO needs AbsenceRecordDAO
        //teacherDAO.setClassSessionDAO(classSessionDAO); // If TeacherDAO needs ClassSessionDAO
        // teacherDAO.setDashboardDAO(dashboardDAO); // If TeacherDAO needs DashboardDAO (unlikely)
        // teacherDAO.setClassroomDAO(classroomDAO); // If TeacherDAO needs ClassroomDAO (e.g., assigned office/classroom)
        //teacherDAO.setDetailsDAO(detailsDAO); // If TeacherDAO needs DetailsDAO (e.g., to view student grades in their courses)
        // teacherDAO.setScheduleDAO(scheduleDAO); // If TeacherDAO needs ScheduleDAO (unlikely)


        // AbsenceRecordDAO does not appear to have dependencies on other DAOs based on the provided code.
        // If it did, you would set them here.


        // ClassSessionDAO does not appear to have dependencies on other DAOs based on the provided code.
        // If it did, you would set them here.


        // DashboardDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.


        // ClassroomDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // DetailsDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // HolidayDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // ReportDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // RoomScheduleDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // ScheduleDAO does not appear to have any dependencies on other DAOs based on the provided code.
        // If it did, set them here.


        // StudentScheduleDAO does not appear to have any dependencies on other DAOs based on the provided code.
        // If it did, set them here.
        // studentScheduleDAO.setCourseDAO(courseDAO); // Example: If StudentScheduleDAO needed CourseDAO directly

        // TeachingStatisticsDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // TeacherMonthlyStatisticsDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // TeacherQuarterlyStatisticsDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // TeacherYearlyStatisticsDAO does not appear to have any dependencies on other DAOs.
        // If it did, set them here.

        // Wire other dependencies as needed
    }

    /**
     * Gets the singleton instance of DaoManager using double-checked locking
     * for thread-safe lazy initialization.
     *
     * @return The DaoManager instance.
     */
    public static DaoManager getInstance() {
        if (instance == null) { // First check (no lock)
            synchronized (DaoManager.class) { // Synchronize only if instance is null
                if (instance == null) { // Second check (within lock)
                    instance = new DaoManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get the configured StudentDAO instance.
     * @return The StudentDAO instance
     */
    public StudentDAO getStudentDAO() {
        return studentDAO;
    }

    /**
     * Get the configured ParentDAO instance.
     * @return The ParentDAO instance
     */
    public ParentDAO getParentDAO() {
        return parentDAO;
    }

    /**
     * Get the configured CourseDAO instance.
     * @return The CourseDAO instance
     */
    public CourseDAO getCourseDAO() {
        return courseDAO;
    }

    /**
     * Get the configured AttendanceDAO instance.
     * @return The AttendanceDAO instance
     */
    public AttendanceDAO getAttendanceDAO() {
        return attendanceDAO;
    }

    /**
     * Get the configured TeacherDAO instance.
     * @return The TeacherDAO instance
     */
    public TeacherDAO getTeacherDAO() {
        return teacherDAO;
    }

    /**
     * Get the configured AbsenceRecordDAO instance.
     * @return The AbsenceRecordDAO instance
     */
    public AbsenceRecordDAO getAbsenceRecordDAO() {
        return absenceRecordDAO;
    }

    /**
     * Get the configured ClassSessionDAO instance.
     * @return The ClassSessionDAO instance
     */
    public ClassSessionDAO getClassSessionDAO() {
        return classSessionDAO;
    }

    /**
     * Get the configured DashboardDAO instance.
     * @return The DashboardDAO instance
     */
    public DashboardDAO getDashboardDAO() {
        return dashboardDAO;
    }

    /**
     * Get the configured ClassroomDAO instance.
     * @return The ClassroomDAO instance
     */
    public ClassroomDAO getClassroomDAO() {
        return classroomDAO;
    }

    /**
     * Get the configured DetailsDAO instance.
     * @return The DetailsDAO instance
     */
    public DetailsDAO getDetailsDAO() {
        return detailsDAO;
    }

    /**
     * Get the configured HolidayDAO instance.
     * @return The HolidayDAO instance
     */
    public HolidayDAO getHolidayDAO() {
        return holidayDAO;
    }

    /**
     * Get the configured ReportDAO instance.
     * @return The ReportDAO instance
     */
    public ReportDAO getReportDAO() {
        return reportDAO;
    }

    /**
     * Get the configured RoomScheduleDAO instance.
     * @return The RoomScheduleDAO instance
     */
    public RoomScheduleDAO getRoomScheduleDAO() {
        return roomScheduleDAO;
    }

    /**
     * Get the configured ScheduleDAO instance.
     * @return The ScheduleDAO instance
     */
    public ScheduleDAO getScheduleDAO() {
        return scheduleDAO;
    }

    /**
     * Get the configured StudentScheduleDAO instance.
     * @return The StudentScheduleDAO instance
     */
    public StudentScheduleDAO getStudentScheduleDAO() {
        return studentScheduleDAO;
    }

    /**
     * Get the configured TeachingStatisticsDAO instance.
     * @return The TeachingStatisticsDAO instance
     */
    public TeachingStatisticsDAO getTeachingStatisticsDAO() {
        return teachingStatisticsDAO;
    }

    /**
     * Get the configured TeacherMonthlyStatisticsDAO instance.
     * @return The TeacherMonthlyStatisticsDAO instance
     */
    public TeacherMonthlyStatisticsDAO getTeacherMonthlyStatisticsDAO() {
        return teacherMonthlyStatisticsDAO;
    }

    /**
     * Get the configured TeacherQuarterlyStatisticsDAO instance.
     * @return The TeacherQuarterlyStatisticsDAO instance
     */
    public TeacherQuarterlyStatisticsDAO getTeacherQuarterlyStatisticsDAO() {
        return teacherQuarterlyStatisticsDAO;
    }

    /**
     * Get the configured TeacherYearlyStatisticsDAO instance.
     * @return The TeacherYearlyStatisticsDAO instance
     */
    public TeacherYearlyStatisticsDAO getTeacherYearlyStatisticsDAO() {
        return teacherYearlyStatisticsDAO;
    }

    // Add other getters for any other DAOs you manage
}
