package src.dao;

import src.model.classroom.Classroom;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Using Optional for findById
import java.util.logging.Level;
import java.util.logging.Logger; // Using Logger instead of System.err

/**
 * Data Access Object for Classroom entity.
 * Provides methods to interact with the classroom table in the database.
 * Each public method manages its own database connection.
 */
public class ClassroomDAO {

    private static final Logger LOGGER = Logger.getLogger(ClassroomDAO.class.getName());

    /**
     * Constructor.
     */
    public ClassroomDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Retrieves all classrooms from the database.
     *
     * @return List of all classrooms, or an empty list if an error occurs.
     */
    public List<Classroom> findAll() {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = "SELECT room_id, code, room_name, floor, capacity, status FROM rooms ORDER BY code"; // Explicitly list columns

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection
             PreparedStatement stmt = conn.prepareStatement(sql); // Use PreparedStatement even for no params for consistency
             ResultSet rs = stmt.executeQuery()) { // Execute query

            while (rs.next()) {
                classrooms.add(extractClassroomFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving classrooms", e);
            // Return empty list on error
        }

        return classrooms;
    }

    /**
     * Finds classrooms matching the specified search criteria.
     *
     * @param keyword Keyword to search for in classroom data (code, name, floor)
     * @param status Status filter ("All" means no filter)
     * @return List of matching classrooms, or an empty list if an error occurs.
     */
    public List<Classroom> findBySearchCriteria(String keyword, String status) {
        List<Classroom> classrooms = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT room_id, code, room_name, floor, capacity, status FROM rooms WHERE 1=1"); // Explicitly list columns

        // Add search conditions
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Using OR for multiple column search, casting floor to CHAR/VARCHAR if needed by DB
            sql.append(" AND (LOWER(code) LIKE LOWER(?) OR LOWER(room_name) LIKE LOWER(?) OR CAST(floor AS CHAR) LIKE ?)"); // Use LOWER for case-insensitive search
            String searchPattern = "%" + keyword.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern); // Match CHAR type for floor search
        }

        // Add status filter
        if (status != null && !status.equals("All") && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY code");

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) { // Prepare statement

            // Set parameters based on the dynamic query
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                }
                // Add more types if needed
            }

            try (ResultSet rs = stmt.executeQuery()) { // Execute query
                while (rs.next()) {
                    classrooms.add(extractClassroomFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching classrooms", e);
            // Return empty list on error
        }

        return classrooms;
    }

    /**
     * Finds a classroom by its ID.
     *
     * @param id The ID of the classroom to find
     * @return An Optional containing the found classroom, or empty if not found or an error occurs.
     */
    public Optional<Classroom> findById(int id) {
        // Corrected column name to room_id to match database schema shown in the image
        String sql = "SELECT room_id, code, room_name, floor, capacity, status FROM rooms WHERE room_id = ?"; // Explicitly list columns and use room_id

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection
             PreparedStatement stmt = conn.prepareStatement(sql)) { // Prepare statement

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) { // Execute query
                if (rs.next()) {
                    return Optional.of(extractClassroomFromResultSet(rs)); // Return Optional with found classroom
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding classroom by ID: " + id, e);
            // Return empty optional on error
        }

        return Optional.empty(); // Return empty optional if not found or error
    }

    /**
     * Saves a classroom to the database.
     * If ID is 0, inserts a new record and sets the generated ID; otherwise updates existing one.
     *
     * @param classroom The classroom to save
     * @return The saved classroom with updated ID (if inserted), or null if the operation failed.
     */
    public Classroom save(Classroom classroom) {
        if (classroom == null) {
            LOGGER.warning("Attempted to save a null classroom.");
            return null;
        }

        try (Connection conn = DatabaseConnection.getConnection()) { // Get connection
            if (classroom.getId() == 0) {
                // Insert new classroom
                String sql = "INSERT INTO rooms (code, room_name, floor, capacity, status) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Prepare statement and request generated keys
                    stmt.setString(1, classroom.getMa());
                    stmt.setString(2, classroom.getTen());
                    stmt.setInt(3, classroom.getTang());
                    stmt.setInt(4, classroom.getSucChua());
                    stmt.setString(5, classroom.getTrangThai());

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("Creating classroom failed, no rows affected.");
                    }

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            // Assuming the generated key is the room_id (primary key)
                            int id = generatedKeys.getInt(1);
                            classroom.setId(id); // Set the generated ID back to the object
                        } else {
                            throw new SQLException("Creating classroom failed, no ID obtained.");
                        }
                    }
                }
            } else {
                // Update existing classroom
                // Corrected WHERE clause to use room_id
                String sql = "UPDATE rooms SET code=?, room_name=?, floor=?, capacity=?, status=? WHERE room_id=?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) { // Prepare statement
                    stmt.setString(1, classroom.getMa());
                    stmt.setString(2, classroom.getTen());
                    stmt.setInt(3, classroom.getTang());
                    stmt.setInt(4, classroom.getSucChua());
                    stmt.setString(5, classroom.getTrangThai());
                    stmt.setInt(6, classroom.getId()); // Use getId() which maps to room_id

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows == 0) {
                        LOGGER.warning("Classroom update failed, no matching ID found for ID: " + classroom.getId());
                        return null; // Indicate update failed if no rows affected
                    }
                }
            }

            return classroom; // Return the classroom object (with updated ID if inserted)
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving classroom: " + (classroom != null ? classroom.getMa() : "null"), e);
            return null; // Indicate failure
        }
    }

    /**
     * Updates the status of a classroom by ID.
     *
     * @param id The ID of the classroom to update
     * @param newStatus The new status value
     * @return true if successful (at least one row updated), false otherwise
     */
    public boolean updateStatus(int id, String newStatus) {
        if (newStatus == null || newStatus.trim().isEmpty()) {
            LOGGER.warning("Attempted to update classroom status to null or empty for ID: " + id);
            return false;
        }
        // Corrected WHERE clause to use room_id
        String sql = "UPDATE rooms SET status=? WHERE room_id=?";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection
             PreparedStatement stmt = conn.prepareStatement(sql)) { // Prepare statement

            stmt.setString(1, newStatus);
            stmt.setInt(2, id); // Use the provided id

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0; // Return true if at least one row was updated
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating classroom status for ID: " + id, e);
            return false; // Indicate failure
        }
    }

    /**
     * Deletes a classroom from the database by ID.
     *
     * @param id The ID of the classroom to delete
     * @return true if successful (at least one row deleted), false otherwise
     */
    public boolean delete(int id) {
        // Corrected WHERE clause to use room_id
        String sql = "DELETE FROM rooms WHERE room_id=?";

        try (Connection conn = DatabaseConnection.getConnection(); // Get connection
             PreparedStatement stmt = conn.prepareStatement(sql)) { // Prepare statement

            stmt.setInt(1, id); // Use the provided id

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0; // Return true if at least one row was deleted
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting classroom with ID: " + id, e);
            return false; // Indicate failure
        }
    }

    /**
     * Helper method to extract classroom data from a ResultSet.
     * Assumes the ResultSet cursor is currently on a valid row.
     *
     * @param rs ResultSet containing classroom data
     * @return Classroom object populated with data
     * @throws SQLException If a database access error occurs
     */
    private Classroom extractClassroomFromResultSet(ResultSet rs) throws SQLException {
        // Corrected column name to room_id to match database schema shown in the image
        int id = rs.getInt("room_id");
        String code = rs.getString("code");
        String name = rs.getString("room_name");
        int floor = rs.getInt("floor");
        int capacity = rs.getInt("capacity");
        String status = rs.getString("status");

        // Instantiate Classroom using the updated constructor (without stt)
        return new Classroom(id, code, name, floor, capacity, status);
    }
}
