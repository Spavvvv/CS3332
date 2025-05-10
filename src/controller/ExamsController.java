package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import src.model.ClassSession;
import src.model.person.Student;
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO;
import view.components.ExamsView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Exams functionality with database integration
 */
public class ExamsController {
    private static final Logger LOGGER = Logger.getLogger(ExamsController.class.getName());

    private final ExamsView view;
    private final ClassSessionDAO classSessionDAO;
    private final StudentDAO studentDAO;
    private ObservableList<ClassSession> sessionsList = FXCollections.observableArrayList();
    private FilteredList<ClassSession> filteredSessions;

    // Status mapping
    private static final Map<Integer, String> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(0, "Chưa bắt đầu");
        STATUS_MAP.put(1, "Khởi tạo");
        STATUS_MAP.put(2, "Đang diễn ra");
        STATUS_MAP.put(3, "Đã kết thúc");
        STATUS_MAP.put(4, "Đã hủy");
    }

    /**
     * Constructor for ExamsController
     * @param view The ExamsView instance
     * @throws SQLException If there is an error connecting to the database
     */
    public ExamsController(ExamsView view) throws SQLException {
        this.view = view;
        try {
            this.classSessionDAO = new ClassSessionDAO(); // Initialize DAOs
            this.studentDAO = new StudentDAO();
            view.setController(this);
            initializeController();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing DAOs", e);
            view.showError("Could not initialize data access: " + e.getMessage());
            throw e; // Re-throw to let calling code handle it
        }
    }

    private void initializeController() {
        // Initialize the filtered list
        filteredSessions = new FilteredList<>(sessionsList, p -> true);

        // Set the filtered list to the view
        view.setSessionData(filteredSessions);
    }

    /**
     * Load exam data from the database
     */
    public void loadData() {
        try {
            // Clear existing data
            sessionsList.clear();

            // Fetch class sessions from database
            List<ClassSession> sessions = classSessionDAO.getAllClassSessions();

            // For each session, load its students
            for (ClassSession session : sessions) {
                List<Student> students = studentDAO.getAllStudents();
                session.setStudents(students);
            }

            // Add all sessions to the observable list
            sessionsList.addAll(sessions);

            // Reset filter
            resetFilter();

            // Refresh the view
            view.refreshView();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading class sessions from database", e);
            view.showError("Could not load exam data: " + e.getMessage());
        }
    }

    /**
     * Perform search based on keyword and date range
     */
    public void performSearch(String keyword, LocalDate fromDate, LocalDate toDate) {
        filteredSessions.setPredicate(session -> {
            boolean matchesKeyword = true;
            boolean matchesDateRange = true;

            if (keyword != null && !keyword.isEmpty()) {
                String lowerCaseKeyword = keyword.toLowerCase();
                matchesKeyword = session.getCourseName().toLowerCase().contains(lowerCaseKeyword) ||
                        session.getRoom().toLowerCase().contains(lowerCaseKeyword) ||
                        session.getTeacher().toLowerCase().contains(lowerCaseKeyword);
            }

            if (fromDate != null) {
                matchesDateRange = !session.getDate().isBefore(fromDate);
            }

            if (toDate != null) {
                matchesDateRange = matchesDateRange && !session.getDate().isAfter(toDate);
            }

            return matchesKeyword && matchesDateRange;
        });
    }

    /**
     * Reset the filter to show all sessions
     */
    public void resetFilter() {
        filteredSessions.setPredicate(p -> true);
    }

    /**
     * Get the display string for a status code
     */
    public String getStatusDisplay(int statusCode) {
        return STATUS_MAP.getOrDefault(statusCode, "Unknown");
    }

    /**
     * Update page size
     */
    public void updatePageSize(int pageSize) {
        // In a real application, this would update the pagination
        System.out.println("Page size updated to: " + pageSize);
    }

    /**
     * Navigate to scores view for a session
     */
    public void showScores(ClassSession session) {
        try {
            // Load detailed session data if needed
            ClassSession detailedSession = classSessionDAO.getClassSessionById(session.getId());
            List<Student> students = studentDAO.getAllStudents();
            detailedSession.setStudents(students);

            // Set selected session in main controller for details view
            view.getMainController().setSessionDetail(detailedSession);
            view.getNavigationController().navigateTo("details-view");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading session details", e);
            view.showError("Could not load session details: " + e.getMessage());
        }
    }

    /**
     * Save or update a session in the database
     */
    public void saveSession(ClassSession session) {
        try {
            if (session.getId() > 0) {
                // Update existing session
                classSessionDAO.updateClassSession(session);
            } else {
                // Create new session
                long newId = classSessionDAO.createClassSession(session);
                session.setId(newId);
            }

            // Refresh data
            loadData();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving session", e);
            view.showError("Could not save session: " + e.getMessage());
        }
    }

    /**
     * Delete a session from the database
     */
    public void deleteSession(ClassSession session) {
        try {
            classSessionDAO.deleteClassSession(session.getId());

            // Refresh data
            loadData();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting session", e);
            view.showError("Could not delete session: " + e.getMessage());
        }
    }
}
