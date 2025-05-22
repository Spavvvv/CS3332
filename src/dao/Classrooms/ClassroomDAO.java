
package src.dao.Classrooms;

import src.model.classroom.Classroom;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Classroom entity.
 * Provides methods to interact with the 'rooms' table in the database.
 */
public class ClassroomDAO {

    private static final Logger LOGGER = Logger.getLogger(ClassroomDAO.class.getName());
    private static final String TABLE_NAME = "rooms";
    private static final String COLUMN_ROOM_ID = "room_id";
    private static final String COLUMN_CODE = "code";
    private static final String COLUMN_ROOM_NAME = "room_name";
    private static final String COLUMN_FLOOR = "floor";
    private static final String COLUMN_CAPACITY = "capacity";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_ROOM_TYPE = "room_type";

    // ... (các phương thức findAll, findBySearchCriteria, findByRoomId, save, deleteByRoomId, extractClassroomFromResultSet, checkCodeExists giữ nguyên như trước) ...

    /**
     * Retrieves all classrooms from the database.
     *
     * @return List of all classrooms, or an empty list if an error occurs.
     */
    public List<Classroom> findAll() {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = String.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s ORDER BY %s",
                COLUMN_ROOM_ID, COLUMN_CODE, COLUMN_ROOM_NAME, COLUMN_FLOOR, COLUMN_CAPACITY, COLUMN_STATUS, COLUMN_ROOM_TYPE,
                TABLE_NAME, COLUMN_CODE);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            int sttCounter = 1;
            while (rs.next()) {
                Classroom classroom = extractClassroomFromResultSet(rs);
                classroom.setStt(sttCounter++);
                classrooms.add(classroom);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all classrooms", e);
        }
        return classrooms;
    }

    /**
     * Finds classrooms matching the specified search criteria.
     *
     * @param keyword Keyword to search for in classroom data (code, name)
     * @param statusFilter Status filter ("All" or null means no filter)
     * @return List of matching classrooms.
     */
    public List<Classroom> findBySearchCriteria(String keyword, String statusFilter) {
        List<Classroom> classrooms = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(String.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE 1=1",
                COLUMN_ROOM_ID, COLUMN_CODE, COLUMN_ROOM_NAME, COLUMN_FLOOR, COLUMN_CAPACITY, COLUMN_STATUS, COLUMN_ROOM_TYPE,
                TABLE_NAME));

        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlBuilder.append(String.format(" AND (LOWER(%s) LIKE LOWER(?) OR LOWER(%s) LIKE LOWER(?))", COLUMN_CODE, COLUMN_ROOM_NAME));
            String searchPattern = "%" + keyword.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (statusFilter != null && !statusFilter.equalsIgnoreCase("All") && !statusFilter.trim().isEmpty()) {
            sqlBuilder.append(String.format(" AND %s = ?", COLUMN_STATUS));
            params.add(statusFilter);
        }

        sqlBuilder.append(String.format(" ORDER BY %s", COLUMN_CODE));

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int sttCounter = 1;
                while (rs.next()) {
                    Classroom classroom = extractClassroomFromResultSet(rs);
                    classroom.setStt(sttCounter++);
                    classrooms.add(classroom);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching classrooms with keyword: " + keyword + ", status: " + statusFilter, e);
        }
        return classrooms;
    }

    /**
     * Finds a classroom by its room_id.
     *
     * @param roomId The ID (room_id) of the classroom to find.
     * @return An Optional containing the found classroom, or empty if not found.
     */
    public Optional<Classroom> findByRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = String.format("SELECT %s, %s, %s, %s, %s, %s, %s FROM %s WHERE %s = ?",
                COLUMN_ROOM_ID, COLUMN_CODE, COLUMN_ROOM_NAME, COLUMN_FLOOR, COLUMN_CAPACITY, COLUMN_STATUS, COLUMN_ROOM_TYPE,
                TABLE_NAME, COLUMN_ROOM_ID);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractClassroomFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding classroom by roomId: " + roomId, e);
        }
        return Optional.empty();
    }

    /**
     * Saves a classroom to the database (inserts if new, updates if exists based on roomId).
     *
     * @param classroom The classroom to save.
     * @return True if the operation was successful, false otherwise.
     */
    public boolean save(Classroom classroom) {
        if (classroom == null) {
            LOGGER.warning("Attempted to save a null classroom.");
            return false;
        }
        // roomId is crucial. If it's for a new classroom, it must be set before calling save.
        if (classroom.getRoomId() == null || classroom.getRoomId().trim().isEmpty()) {
            // If roomId is generated elsewhere (e.g. UUID) it should be set on classroom obj before this call
            // If roomId is based on 'code', ensure 'code' is set and unique, then set 'roomId' to 'code'
            // For this example, let's assume roomId (which is also the primary key) is derived from 'code' if it's new
            // and not yet set. This is a business logic decision.
            // A common pattern for user-defined string PKs is that they are explicitly set.
            // If 'roomId' is truly missing for a new entry, this is an issue.
            // Let's assume 'code' can act as 'roomId' if 'roomId' is not provided for a new entry.
            // THIS IS A SIMPLIFICATION AND MIGHT NEED MORE ROBUST HANDLING
            if (classroom.getCode() != null && !classroom.getCode().trim().isEmpty()) {
                // Potential logic: if roomId is null but code is present, this might be a new classroom
                // where code should be used as roomId. This requires checking if code (as roomId) exists.
                // For simplicity now, we'll require roomId to be set.
                LOGGER.warning("Attempted to save a classroom with null or empty roomId. RoomId must be provided.");
                return false;
            } else {
                LOGGER.warning("Attempted to save a classroom with null or empty roomId and code.");
                return false;
            }
        }


        Optional<Classroom> existingClassroomOpt = findByRoomId(classroom.getRoomId());
        boolean isUpdate = existingClassroomOpt.isPresent();

        String sql;
        if (isUpdate) {
            sql = String.format("UPDATE %s SET %s=?, %s=?, %s=?, %s=?, %s=?, %s=? WHERE %s=?",
                    TABLE_NAME, COLUMN_CODE, COLUMN_ROOM_NAME, COLUMN_FLOOR, COLUMN_CAPACITY, COLUMN_STATUS, COLUMN_ROOM_TYPE,
                    COLUMN_ROOM_ID);
        } else {
            // Check for code uniqueness before insert if code should be unique and not same as roomId
            if (checkCodeExists(classroom.getCode(), null)) { // null because it's a new entry
                LOGGER.warning("Attempted to insert a classroom with a code that already exists: " + classroom.getCode());
                // Optionally throw a specific exception or return a more detailed error
                return false;
            }
            sql = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    TABLE_NAME, COLUMN_ROOM_ID, COLUMN_CODE, COLUMN_ROOM_NAME, COLUMN_FLOOR, COLUMN_CAPACITY, COLUMN_STATUS, COLUMN_ROOM_TYPE);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (isUpdate) { // UPDATE
                stmt.setString(1, classroom.getCode());
                stmt.setString(2, classroom.getRoomName());
                stmt.setInt(3, classroom.getFloor());
                stmt.setInt(4, classroom.getCapacity());
                stmt.setString(5, classroom.getStatus());
                stmt.setString(6, classroom.getRoomType());
                stmt.setString(7, classroom.getRoomId()); // WHERE clause
            } else { // INSERT
                stmt.setString(1, classroom.getRoomId()); // RoomId must be provided
                stmt.setString(2, classroom.getCode());
                stmt.setString(3, classroom.getRoomName());
                stmt.setInt(4, classroom.getFloor());
                stmt.setInt(5, classroom.getCapacity());
                stmt.setString(6, classroom.getStatus());
                stmt.setString(7, classroom.getRoomType());
            }

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving classroom with roomId: " + classroom.getRoomId(), e);
            return false;
        }
    }

    /**
     * Deletes a classroom from the database by its room_id.
     *
     * @param roomId The ID (room_id) of the classroom to delete.
     * @return true if successful, false otherwise.
     */
    public boolean deleteByRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            LOGGER.warning("Attempted to delete classroom with null or empty roomId.");
            return false;
        }
        String sql = String.format("DELETE FROM %s WHERE %s = ?", TABLE_NAME, COLUMN_ROOM_ID);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting classroom with roomId: " + roomId, e);
            // Consider if this classroom is referenced by other tables (foreign key constraints)
            // If so, deletion might fail, and the error message from SQLException can be more specific.
            if (e.getSQLState().startsWith("23")) { // Integrity constraint violation
                LOGGER.log(Level.WARNING, "Cannot delete classroom with roomId: " + roomId + ". It might be referenced by other records (e.g., schedules).", e);
            }
            return false;
        }
    }

    /**
     * Helper method to extract classroom data from a ResultSet.
     */
    private Classroom extractClassroomFromResultSet(ResultSet rs) throws SQLException {
        String roomId = rs.getString(COLUMN_ROOM_ID);
        String code = rs.getString(COLUMN_CODE);
        String roomName = rs.getString(COLUMN_ROOM_NAME);
        int floor = rs.getInt(COLUMN_FLOOR);
        int capacity = rs.getInt(COLUMN_CAPACITY);
        String status = rs.getString(COLUMN_STATUS);
        String roomType = rs.getString(COLUMN_ROOM_TYPE);

        Classroom classroom = new Classroom(); // Uses the default constructor
        classroom.setRoomId(roomId);
        classroom.setCode(code);
        classroom.setRoomName(roomName);
        classroom.setFloor(floor);
        classroom.setCapacity(capacity);
        classroom.setStatus(status);
        classroom.setRoomType(roomType);
        // STT is typically set when preparing a list for display, not here.
        return classroom;
    }
    /**
     * Checks if a classroom code already exists in the database, excluding a given roomId (for updates).
     *
     * @param code The code to check.
     * @param excludeRoomId The roomId to exclude from the check (null or empty if checking for a new classroom).
     * @return true if the code exists, false otherwise.
     */
    public boolean checkCodeExists(String code, String excludeRoomId) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        StringBuilder sqlBuilder = new StringBuilder(String.format("SELECT COUNT(*) FROM %s WHERE LOWER(%s) = LOWER(?)", TABLE_NAME, COLUMN_CODE));
        List<Object> params = new ArrayList<>();
        params.add(code.trim());

        if (excludeRoomId != null && !excludeRoomId.trim().isEmpty()) {
            sqlBuilder.append(String.format(" AND %s != ?", COLUMN_ROOM_ID));
            params.add(excludeRoomId.trim());
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if code exists: " + code, e);
        }
        return false;
    }

    /**
     * Updates the status of a classroom by its room_id.
     *
     * @param roomId The ID (room_id) of the classroom to update.
     * @param newStatus The new status value.
     * @return true if successful, false otherwise.
     */
    public boolean updateStatus(String roomId, String newStatus) {
        if (roomId == null || roomId.trim().isEmpty() || newStatus == null || newStatus.trim().isEmpty()) {
            LOGGER.warning("Attempted to update status with null/empty roomId or newStatus.");
            return false;
        }
        String sql = String.format("UPDATE %s SET %s=? WHERE %s=?",
                TABLE_NAME, COLUMN_STATUS, COLUMN_ROOM_ID);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, roomId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating classroom status for roomId: " + roomId, e);
            return false;
        }
    }
}

