package src.dao;

import src.model.classroom.Classroom;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Classroom entity.
 * Provides methods to interact with the classroom table in the database.
 */
public class ClassroomDAO {

    /**
     * Retrieves all classrooms from the database.
     *
     * @return List of all classrooms
     */
    public List<Classroom> findAll() {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY code";

        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql);
            int stt = 1;
            while (rs.next()) {
                classrooms.add(extractClassroomFromResultSet(rs, stt++));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving classrooms: " + e.getMessage());
        }

        return classrooms;
    }

    /**
     * Finds classrooms matching the specified search criteria.
     *
     * @param keyword Keyword to search for in classroom data
     * @param status Status filter
     * @return List of matching classrooms
     */
    public List<Classroom> findBySearchCriteria(String keyword, String status) {
        List<Classroom> classrooms = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM rooms WHERE 1=1");

        // Add search conditions
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (code LIKE ? OR name LIKE ? OR CAST(floor AS CHAR) LIKE ?)");
            String searchPattern = "%" + keyword + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Add status filter
        if (status != null && !status.equals("All")) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY code");

        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql.toString(), params.toArray());
            int stt = 1;
            while (rs.next()) {
                classrooms.add(extractClassroomFromResultSet(rs, stt++));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error searching classrooms: " + e.getMessage());
        }

        return classrooms;
    }

    /**
     * Finds a classroom by its ID.
     *
     * @param id The ID of the classroom to find
     * @return The found classroom, or null if not found
     */
    public Classroom findById(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";

        try {
            ResultSet rs = DatabaseConnection.executeQuery(sql, id);
            if (rs.next()) {
                Classroom classroom = extractClassroomFromResultSet(rs, 1);
                rs.close();
                return classroom;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error finding classroom by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Saves a classroom to the database.
     * If ID is 0, inserts a new record; otherwise updates existing one.
     *
     * @param classroom The classroom to save
     * @return The saved classroom with updated ID (if inserted)
     */
    public Classroom save(Classroom classroom) {
        try {
            if (classroom.getId() == 0) {
                // Insert new classroom
                String sql = "INSERT INTO rooms (code, name, floor, capacity, status) VALUES (?, ?, ?, ?, ?)";

                // Use prepared statement to get generated keys
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, classroom.getMa());
                stmt.setString(2, classroom.getTen());
                stmt.setInt(3, classroom.getTang());
                stmt.setInt(4, classroom.getSucChua());
                stmt.setString(5, classroom.getTrangThai());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Creating classroom failed, no rows affected.");
                }

                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    classroom.setId(id);
                } else {
                    throw new SQLException("Creating classroom failed, no ID obtained.");
                }
                generatedKeys.close();
                stmt.close();
            } else {
                // Update existing classroom
                String sql = "UPDATE rooms SET code=?, name=?, floor=?, capacity=?, status=? WHERE id=?";

                int affectedRows = DatabaseConnection.executeUpdate(sql,
                        classroom.getMa(),
                        classroom.getTen(),
                        classroom.getTang(),
                        classroom.getSucChua(),
                        classroom.getTrangThai(),
                        classroom.getId()
                );

                if (affectedRows == 0) {
                    System.err.println("Classroom update failed, no matching ID found.");
                    return null;
                }
            }

            return classroom;
        } catch (SQLException e) {
            System.err.println("Error saving classroom: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates the status of a classroom.
     *
     * @param id The ID of the classroom to update
     * @param newStatus The new status value
     * @return true if successful, false otherwise
     */
    public boolean updateStatus(int id, String newStatus) {
        String sql = "UPDATE rooms SET status=? WHERE id=?";

        try {
            int affectedRows = DatabaseConnection.executeUpdate(sql, newStatus, id);
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating classroom status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a classroom from the database.
     *
     * @param id The ID of the classroom to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM rooms WHERE id=?";

        try {
            int affectedRows = DatabaseConnection.executeUpdate(sql, id);
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting classroom: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to extract classroom data from a ResultSet.
     *
     * @param rs ResultSet containing classroom data
     * @param stt Sequence number for display
     * @return Classroom object populated with data
     * @throws SQLException If a database access error occurs
     */
    private Classroom extractClassroomFromResultSet(ResultSet rs, int stt) throws SQLException {
        int id = rs.getInt("id");
        String code = rs.getString("code");
        String name = rs.getString("name");
        int floor = rs.getInt("floor");
        int capacity = rs.getInt("capacity");
        String status = rs.getString("status");

        return new Classroom(id, stt, code, name, floor, capacity, status);
    }
}
