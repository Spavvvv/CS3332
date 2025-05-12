package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import src.model.ClassSession;
import src.model.person.Student;
import utils.DaoManager; // Import the DaoManager
import view.components.ExamsView;

import java.sql.Connection; // Import Connection
import java.sql.SQLException; // Import SQLException
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.DatabaseConnection; // Import DatabaseConnection

// Import the DAOs needed by this controller
import src.dao.ClassSessionDAO;
import src.dao.StudentDAO; // Assuming StudentDAO is needed to get students for sessions

/**
 * Controller for the Exams functionality with database integration
 * Modified to interact with DaoManager for DAO instances and manage database connections.
 */
public class ExamsController {
    private static final Logger LOGGER = Logger.getLogger(ExamsController.class.getName());

    private final ExamsView view;
    private final DaoManager daoManager; // Reference to the DaoManager
    private final ClassSessionDAO classSessionDAO; // Reference to the ClassSessionDAO
    private final StudentDAO studentDAO; // Reference to the StudentDAO

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
     */
    public ExamsController(ExamsView view) {
        this.view = view;
        // Obtain the DaoManager and required DAO instances
        this.daoManager = DaoManager.getInstance();
        this.classSessionDAO = daoManager.getClassSessionDAO();
        this.studentDAO = daoManager.getStudentDAO(); // Get StudentDAO instance

        view.setController(this);
        initializeController();
    }

    private void initializeController() {
        // Initialize the filtered list
        filteredSessions = new FilteredList<>(sessionsList, p -> true);

        // Set the filtered list to the view
        view.setSessionData(filteredSessions);
    }

    /**
     * Load exam data from the database using the ClassSessionDAO and StudentDAO,
     * managing the database connection.
     */
    public void loadData() {
        sessionsList.clear(); // Clear existing data
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and manage connection
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Database connection is null. Cannot load data.");
                view.showError("Could not connect to the database.");
                return;
            }

            // Fetch class sessions from database using the DAO with the connection
            List<ClassSession> sessions = classSessionDAO.findAll();

            // Fetch all students if needed for sessions (this might be inefficient depending on use case)
            // Assuming StudentDAO has a findAll method that accepts a Connection
            List<Student> allStudents = studentDAO.findAll();

            // For each session, set its students (as per original logic, though potentially inefficient)
            for (ClassSession session : sessions) {
                // Set the list of all students on each session object
                // A more efficient approach might load students per session when needed.
                session.setStudents(allStudents);
            }

            // Add all sessions to the observable list
            sessionsList.addAll(sessions);

            // Reset filter
            resetFilter();

            // Refresh the view
            view.refreshView();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading exam data using DAO.", e);
            view.showError("Database error loading exam data: " + e.getMessage());
        } catch (Exception e) { // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "An unexpected error occurred during data loading.", e);
            view.showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Perform search based on keyword and date range.
     * This logic operates on the filtered list in memory.
     */
    public void performSearch(String keyword, LocalDate fromDate, LocalDate toDate) {
        filteredSessions.setPredicate(session -> {
            boolean matchesKeyword = true;
            boolean matchesDateRange = true;

            if (keyword != null && !keyword.isEmpty()) {
                String lowerCaseKeyword = keyword.toLowerCase();
                if (session != null) {
                    matchesKeyword = (session.getCourseName() != null && session.getCourseName().toLowerCase().contains(lowerCaseKeyword)) ||
                            (session.getRoom() != null && session.getRoom().toLowerCase().contains(lowerCaseKeyword)) ||
                            (session.getTeacher() != null && session.getTeacher().toLowerCase().contains(lowerCaseKeyword));
                } else {
                    matchesKeyword = false; // A null session doesn't match
                }
            }

            if (session != null && session.getDate() != null) { // Check if session and date are not null
                if (fromDate != null) {
                    matchesDateRange = !session.getDate().isBefore(fromDate);
                }

                if (toDate != null) {
                    matchesDateRange = matchesDateRange && !session.getDate().isAfter(toDate);
                }
            } else {
                matchesDateRange = false; // A null session or date doesn't match the range
            }

            return matchesKeyword && matchesDateRange;
        });
    }

    /**
     * Reset the filter to show all sessions.
     */
    public void resetFilter() {
        filteredSessions.setPredicate(p -> true);
    }

    /**
     * Get the display string for a status code.
     */
    public String getStatusDisplay(int statusCode) {
        return STATUS_MAP.getOrDefault(statusCode, "Unknown");
    }

    /**
     * Update page size.
     */
    public void updatePageSize(int pageSize) {
        // This remains unchanged as it's not DAO related.
        System.out.println("Page size updated to: " + pageSize);
    }

    /**
     * Navigate to scores view for a session, loading detailed data
     * using the ClassSessionDAO and StudentDAO, managing the connection.
     */
    public void showScores(ClassSession session) {
        if (session == null || session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to show scores for null or invalid session.");
            view.showError("Cannot show details for an invalid session.");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and manage connection
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Database connection is null. Cannot show scores.");
                view.showError("Could not connect to the database.");
                return;
            }

            // Load detailed session data from database using the DAO with the connection
            Optional<ClassSession> detailedSessionOpt = classSessionDAO.findById(conn, session.getId());

            if (detailedSessionOpt.isPresent()) {
                ClassSession detailedSession = detailedSessionOpt.get();

                // Load students for this specific session using the StudentDAO with the connection
                // Assuming StudentDAO has a findBySessionId method that accepts a Connection
                List<Student> studentsForSession = studentDAO.findBySessionId(conn, detailedSession.getId());
                detailedSession.setStudents(studentsForSession);


                // Set selected session in main controller for details view
                if (view != null && view.getMainController() != null && view.getNavigationController() != null) {
                    view.getMainController().setSessionDetail(detailedSession);
                    view.getNavigationController().navigateTo("details-view");
                } else {
                    LOGGER.log(Level.SEVERE, "View, MainController, or NavigationController is null. Cannot navigate.");
                    view.showError("Navigation error.");
                }

            } else {
                LOGGER.log(Level.WARNING, "Session details not found for ID: " + session.getId() + " using DAO.");
                view.showError("Could not load session details.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading session details using DAO for ID: " + session.getId(), e);
            view.showError("Database error loading session details: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred while loading session details for ID: " + session.getId(), e);
            view.showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Delete a session from the database using the ClassSessionDAO,
     * managing the database connection.
     */
    public void deleteSession(ClassSession session) {
        if (session == null || session.getId() == null || session.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete null or invalid session.");
            view.showError("Cannot delete an invalid session.");
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection()) { // Get and manage connection
            if (conn == null) {
                LOGGER.log(Level.SEVERE, "Database connection is null. Cannot delete session.");
                view.showError("Could not connect to the database.");
                return;
            }

            // Delete session using the DAO with the connection
            boolean success = classSessionDAO.delete(session.getId());

            if (success) {
                // Remove the session from the observable list directly for better performance
                sessionsList.remove(session);
                // The FilteredList will automatically update
                view.refreshView(); // Ensure view is refreshed

            } else {
                LOGGER.log(Level.SEVERE, "DAO failed to delete session with ID: " + session.getId());
                view.showError("Could not delete session.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting session with ID: " + session.getId() + " using DAO.", e);
            view.showError("Database error deleting session: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred while deleting session with ID: " + session.getId(), e);
            view.showError("An unexpected error occurred: " + e.getMessage());
        }
    }


}
