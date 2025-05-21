package utils;

import src.dao.*;
import utils.DatabaseConnection;

// Existing DAO imports
// Add the new DAO import


/**
 * Manages the creation and dependency injection of DAO instances.
 * This class acts as a central singleton point for accessing configured DAO instances.
 * It is responsible for ensuring DAOs are properly wired with their dependencies.
 * Public methods in individual DAOs manage their own database connections.
 */
public class DaoManager {

    private static volatile DaoManager instance; // Use volatile for thread safety

    // Existing DAO instance variables
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
    private TeacherYearlyStatisticsDAO teacherYearlyStatisticsDAO;
    private HomeworkDAO homeworkDAO;

    // Add the new DAO instance variable
    private HomeworkSubmissionDAO homeworkSubmissionDAO;

    /**
     * Private constructor to prevent direct instantiation.
     * Initializes and wires the DAO dependencies.
     */
    private DaoManager() {
        // Create instances of all DAOs
        try {
            // Existing DAO instances
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
            teacherYearlyStatisticsDAO = new TeacherYearlyStatisticsDAO();
            homeworkDAO = new HomeworkDAO();
            // Initialize the new HomeworkSubmissionDAO
            homeworkSubmissionDAO = new HomeworkSubmissionDAO();

        } catch (Exception e) {
            // Handle the SQLException that might be thrown by the HomeworkSubmissionDAO constructor
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize DAOs: " + e.getMessage());
        }

        // Existing dependency wiring
        studentDAO.setParentDAO(parentDAO);
        studentDAO.setCourseDAO(courseDAO);

        parentDAO.setStudentDAO(studentDAO);

        courseDAO.setStudentDAO(studentDAO);
        courseDAO.setTeacherDAO(teacherDAO);

        attendanceDAO.setStudentDAO(studentDAO);
        attendanceDAO.setClassSessionDAO(classSessionDAO);

        teacherDAO.setCourseDAO(courseDAO);

        // Wiring any dependencies needed for HomeworkSubmissionDAO
        // If HomeworkSubmissionDAO needs dependencies on other DAOs, set them here
        // For example:
        // homeworkSubmissionDAO.setAttendanceDAO(attendanceDAO);
        // homeworkSubmissionDAO.setStudentDAO(studentDAO);
        // homeworkSubmissionDAO.setClassSessionDAO(classSessionDAO);
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

    // Existing DAO getters...

    /**
     * Get the configured HomeworkSubmissionDAO instance.
     * @return The HomeworkSubmissionDAO instance
     */
    public HomeworkSubmissionDAO getHomeworkSubmissionDAO() {
        return homeworkSubmissionDAO;
    }

    // Other existing getters...
    public StudentDAO getStudentDAO() {
        return studentDAO;
    }

    public ParentDAO getParentDAO() {
        return parentDAO;
    }

    public CourseDAO getCourseDAO() {
        return courseDAO;
    }

    public AttendanceDAO getAttendanceDAO() {
        return attendanceDAO;
    }

    public TeacherDAO getTeacherDAO() {
        return teacherDAO;
    }

    public AbsenceRecordDAO getAbsenceRecordDAO() {
        return absenceRecordDAO;
    }

    public ClassSessionDAO getClassSessionDAO() {
        return classSessionDAO;
    }

    public DashboardDAO getDashboardDAO() {
        return dashboardDAO;
    }

    public ClassroomDAO getClassroomDAO() {
        return classroomDAO;
    }

    public DetailsDAO getDetailsDAO() {
        return detailsDAO;
    }

    public HolidayDAO getHolidayDAO() {
        return holidayDAO;
    }

    public ReportDAO getReportDAO() {
        return reportDAO;
    }

    public RoomScheduleDAO getRoomScheduleDAO() {
        return roomScheduleDAO;
    }

    public ScheduleDAO getScheduleDAO() {
        return scheduleDAO;
    }

    public StudentScheduleDAO getStudentScheduleDAO() {
        return studentScheduleDAO;
    }

    public TeachingStatisticsDAO getTeachingStatisticsDAO() {
        return teachingStatisticsDAO;
    }

    public TeacherMonthlyStatisticsDAO getTeacherMonthlyStatisticsDAO() {
        return teacherMonthlyStatisticsDAO;
    }

    public TeacherQuarterlyStatisticsDAO getTeacherQuarterlyStatisticsDAO() {
        return teacherQuarterlyStatisticsDAO;
    }

    public TeacherYearlyStatisticsDAO getTeacherYearlyStatisticsDAO() {
        return teacherYearlyStatisticsDAO;
    }

    public HomeworkDAO getHomeworkDAO() {
        return homeworkDAO;
    }
}