package src.controller;

import src.dao.ClassroomDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.classroom.Classroom;

/**
 * Controller for the classroom management functionality.
 * Handles the business logic and mediates between the view and the DAO.
 */
public class ClassroomController {
    private final ClassroomDAO classroomDAO;
    private ObservableList<Classroom> classrooms;
    private ObservableList<Classroom> filteredClassrooms;

    public ClassroomController() {
        this.classroomDAO = new ClassroomDAO();
        this.classrooms = FXCollections.observableArrayList();
        this.filteredClassrooms = FXCollections.observableArrayList();
        loadClassroomsFromDB();
    }

    /**
     * Loads all classrooms from the database.
     */
    public void loadClassroomsFromDB() {
        classrooms.clear();
        classrooms.addAll(classroomDAO.findAll());
        filteredClassrooms.clear();
        filteredClassrooms.addAll(classrooms);
    }

    /**
     * Filters classrooms based on search criteria.
     *
     * @param keyword Keyword to search for
     * @param status Status filter
     */
    public void filterClassrooms(String keyword, String status) {
        filteredClassrooms.clear();
        filteredClassrooms.addAll(classroomDAO.findBySearchCriteria(keyword, status));
    }

    /**
     * Saves a classroom to the database.
     *
     * @param classroom The classroom to save
     * @return The saved classroom with updated ID (if it was a new classroom)
     */
    public Classroom saveClassroom(Classroom classroom) {
        return classroomDAO.save(classroom);
    }

    /**
     * Updates the status of a classroom.
     *
     * @param classroom The classroom to update
     * @param newStatus The new status
     * @return true if successful, false otherwise
     */
    public boolean updateClassroomStatus(Classroom classroom, String newStatus) {
        return classroomDAO.updateStatus(classroom.getId(), newStatus);
    }

    /**
     * Deletes a classroom from the database.
     *
     * @param classroom The classroom to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteClassroom(Classroom classroom) {
        return classroomDAO.delete(classroom.getId());
    }

    /**
     * Gets all classrooms.
     *
     * @return Observable list of all classrooms
     */
    public ObservableList<Classroom> getClassrooms() {
        return classrooms;
    }

    /**
     * Gets filtered classrooms.
     *
     * @return Observable list of filtered classrooms
     */
    public ObservableList<Classroom> getFilteredClassrooms() {
        return filteredClassrooms;
    }
}
