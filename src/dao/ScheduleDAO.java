package src.dao;

import src.model.system.schedule.Schedule;
import src.model.system.schedule.RoomSchedule;
import src.model.system.schedule.StudentSchedule;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
// import java.time.LocalDate; // Removed unused import
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for Schedule operations, combining interface and implementation
 */
public class ScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(ScheduleDAO.class.getName());

    /**
     * Constructor.
     */
    public ScheduleDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Find a schedule by ID
     * @param id The ID of the schedule to find.
     * @return The found Schedule object, or null if not found or an error occurs.
     */
    public Schedule findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedule with null or empty ID.");
            return null;
        }
        String baseQuery = "SELECT id, schedule_type FROM schedules WHERE id = ?";

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement baseStmt = connection.prepareStatement(baseQuery)) {

            baseStmt.setString(1, id);
            try (ResultSet rs = baseStmt.executeQuery()) {
                if (rs.next()) {
                    String scheduleType = rs.getString("schedule_type");

                    // Depending on the type, query the specific subclass table using the existing connection
                    if ("ROOM".equals(scheduleType)) {
                        // Pass the active connection to the helper method
                        return findRoomScheduleById(connection, id);
                    } else if ("STUDENT".equals(scheduleType)) {
                        // Pass the active connection to the helper method
                        return findStudentScheduleById(connection, id);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown schedule type found for ID: " + id + ". Type: " + scheduleType);
                        return null;
                    }
                } else {
                    LOGGER.log(Level.INFO, "No schedule found with ID: " + id);
                    return null; // Schedule ID not found
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedule by ID: " + id, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding schedule by ID: " + id, e);
            return null;
        }
    }

    // Helper method to find a RoomSchedule by ID using an existing connection
    private RoomSchedule findRoomScheduleById(Connection connection, String id) {
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE s.id = ?";

        // Use try-with-resources for PreparedStatement and ResultSet
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );
                }
                return null; // RoomSchedule details not found for this ID
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding RoomSchedule by ID: " + id, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding RoomSchedule by ID: " + id, e);
            return null;
        }
    }

    // Helper method to find a StudentSchedule by ID using an existing connection
    private StudentSchedule findStudentScheduleById(Connection connection, String id) {
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, ss.student_id " +
                "FROM schedules s " +
                "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                "WHERE s.id = ?";

        // Use try-with-resources for PreparedStatement and ResultSet
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("student_id")
                    );
                }
                return null; // StudentSchedule details not found for this ID
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding StudentSchedule by ID: " + id, e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding StudentSchedule by ID: " + id, e);
            return null;
        }
    }

    /**
     * Find all schedules
     * @return A list of all Schedule objects, or an empty list if none found or an error occurs.
     */
    public List<Schedule> findAll() {
        List<Schedule> schedules = new ArrayList<>();
        String baseQuery = "SELECT id, schedule_type FROM schedules";

        // Use try-with-resources for Connection, Statement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(baseQuery)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String scheduleType = rs.getString("schedule_type");

                Schedule schedule = null;
                // Reuse the same connection for fetching subclass details
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                } else {
                    LOGGER.log(Level.WARNING, "Unknown schedule type encountered for ID: " + id + ". Type: " + scheduleType);
                }

                if (schedule != null) {
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all schedules.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding all schedules.", e);
        }

        return schedules;
    }

    /**
     * Save a schedule
     * @param schedule The Schedule object to save.
     * @return true if the save was successful, false otherwise.
     */
    public boolean save(Schedule schedule) {
        if (schedule == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null schedule.");
            return false;
        }
        if (schedule.getId() == null || schedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save a schedule with null or empty ID.");
            return false; // Cannot save without a valid ID
        }

        String insertScheduleSql = "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        Connection connection = null; // Declare connection outside try-with-resources for rollback access

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Start transaction

            // Use try-with-resources for the PreparedStatement
            try (PreparedStatement stmt = connection.prepareStatement(insertScheduleSql)) {
                stmt.setString(1, schedule.getId());
                stmt.setString(2, schedule.getName());
                stmt.setString(3, schedule.getDescription());
                stmt.setTimestamp(4, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
                stmt.setTimestamp(5, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);

                boolean subclassSaveSuccess = false; // Flag to track if subclass save is successful

                // Determine schedule type and handle subclass-specific persistence
                if (schedule instanceof RoomSchedule roomSchedule) {
                    stmt.setString(6, "ROOM");
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        subclassSaveSuccess = saveRoomSchedule(connection, roomSchedule);
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to insert base schedule for RoomSchedule ID: " + schedule.getId());
                        subclassSaveSuccess = false; // Ensure flag is false if base insert fails
                    }
                } else if (schedule instanceof StudentSchedule studentSchedule) {
                    stmt.setString(6, "STUDENT");
                    int result = stmt.executeUpdate();
                    if (result > 0) {
                        subclassSaveSuccess = saveStudentSchedule(connection, studentSchedule);
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to insert base schedule for StudentSchedule ID: " + schedule.getId());
                        subclassSaveSuccess = false; // Ensure flag is false if base insert fails
                    }
                } else {
                    // Unknown schedule type
                    LOGGER.log(Level.SEVERE, "Attempted to save schedule with unknown type: " + schedule.getClass().getName() + " for ID: " + schedule.getId());
                    // No subclass save attempted, return false and rollback the base insert if it occurred
                    try {
                        connection.rollback();
                    } catch (SQLException rbex) {
                        LOGGER.log(Level.SEVERE, "Error during rollback after unknown schedule type save attempt.", rbex);
                    }
                    return false; // Indicate failure for unknown type
                }

                if (subclassSaveSuccess) {
                    connection.commit();
                    return true;
                } else {
                    // If we reach here, either base insert failed (logged inside instanceof blocks)
                    // or subclass save failed (logged inside helper methods).
                    LOGGER.log(Level.SEVERE, "Subclass save failed or base insert failed for schedule ID: " + schedule.getId() + ", rolling back.");
                    try {
                        connection.rollback(); // Rollback base insert if subclass save fails
                    } catch (SQLException rbex) {
                        LOGGER.log(Level.SEVERE, "Error during rollback after failed subclass save for schedule ID: " + schedule.getId(), rbex);
                    }
                    return false;
                }
            } // PreparedStatement closed by try-with-resources

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during saving schedule with ID: " + schedule.getId(), e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback(); // Rollback transaction on error
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during saving schedule with ID: " + schedule.getId(), e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback(); // Rollback transaction on error
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore default
                    connection.close(); // Close the connection
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection after saving schedule with ID: " + schedule.getId(), e);
                }
            }
        }
    }

    // Helper method to save a RoomSchedule using an existing connection (part of a transaction)
    private boolean saveRoomSchedule(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        String insertSql = "INSERT INTO room_schedules (schedule_id, room_id, capacity, room_type) " +
                "VALUES (?, ?, ?, ?)";
        // Use try-with-resources for the PreparedStatement, Connection is managed by caller
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, roomSchedule.getId());
            stmt.setString(2, roomSchedule.getRoomId());
            stmt.setInt(3, roomSchedule.getCapacity());
            stmt.setString(4, roomSchedule.getRoomType());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving RoomSchedule details for schedule ID: " + roomSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving RoomSchedule details for schedule ID: " + roomSchedule.getId(), e);
            // Wrap and re-throw as SQLException to maintain method signature for transaction handling
            throw new SQLException("Unexpected error saving RoomSchedule details", e);
        }
    }

    // Helper method to save a StudentSchedule using an existing connection (part of a transaction)
    private boolean saveStudentSchedule(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        String insertSql = "INSERT INTO student_schedules (schedule_id, student_id) " +
                "VALUES (?, ?)";
        // Use try-with-resources for the PreparedStatement, Connection is managed by caller
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, studentSchedule.getId());
            stmt.setString(2, studentSchedule.getStudentId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving StudentSchedule details for schedule ID: " + studentSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving StudentSchedule details for schedule ID: " + studentSchedule.getId(), e);
            // Wrap and re-throw as SQLException to maintain method signature for transaction handling
            throw new SQLException("Unexpected error saving StudentSchedule details", e);
        }
    }


    /**
     * Update a schedule
     * @param schedule The Schedule object to update.
     * @return true if the update was successful, false otherwise.
     */
    public boolean update(Schedule schedule) {
        if (schedule == null || schedule.getId() == null || schedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null schedule or schedule with null/empty ID.");
            return false;
        }

        String updateScheduleSql = "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? " +
                "WHERE id = ?";

        Connection connection = null; // Declare connection outside try-with-resources for rollback access

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Start transaction

            boolean baseUpdateSuccess = false;
            // Use try-with-resources for the PreparedStatement
            try (PreparedStatement stmt = connection.prepareStatement(updateScheduleSql)) {
                stmt.setString(1, schedule.getName());
                stmt.setString(2, schedule.getDescription());
                stmt.setTimestamp(3, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
                stmt.setTimestamp(4, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);
                stmt.setString(5, schedule.getId());

                int result = stmt.executeUpdate();
                baseUpdateSuccess = result > 0;
                if (!baseUpdateSuccess) {
                    LOGGER.log(Level.WARNING, "Base schedule update affected 0 rows for ID: " + schedule.getId() + ". Schedule not found or no changes.");
                }
            } // PreparedStatement closed by try-with-resources

            boolean subclassUpdateSuccess = true; // Assume true unless a specific subclass update fails
            if (baseUpdateSuccess) { // Only attempt subclass update if base update was successful or affected 0 rows (meaning it exists)
                if (schedule instanceof RoomSchedule roomSchedule) {
                    subclassUpdateSuccess = updateRoomSchedule(connection, roomSchedule);
                } else if (schedule instanceof StudentSchedule studentSchedule) {
                    subclassUpdateSuccess = updateStudentSchedule(connection, studentSchedule);
                }
                // If it's a base Schedule object without a known subclass type, subclassUpdateSuccess remains true.
            }

            if (baseUpdateSuccess && subclassUpdateSuccess) {
                connection.commit();
                LOGGER.log(Level.INFO, "Successfully updated schedule with ID: " + schedule.getId());
                return true;
            } else {
                // If baseUpdateSuccess is false, it was logged as a warning.
                // If subclassUpdateSuccess is false, the helper method logged the error.
                LOGGER.log(Level.SEVERE, "Update failed for schedule ID: " + schedule.getId() + ". Base success: " + baseUpdateSuccess + ", Subclass success: " + subclassUpdateSuccess + ", rolling back.");
                try {
                    connection.rollback(); // Rollback transaction
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error during rollback after failed update for schedule ID: " + schedule.getId(), ex);
                }
                return false;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during updating schedule with ID: " + schedule.getId(), e);
            try {
                // connection might be null here if getting connection failed
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during updating schedule with ID: " + schedule.getId(), e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore default
                    connection.close(); // Close the connection
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection after updating schedule with ID: " + schedule.getId(), e);
                }
            }
        }
    }

    // Helper method to update a RoomSchedule using an existing connection (part of a transaction)
    private boolean updateRoomSchedule(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        String updateSql = "UPDATE room_schedules SET room_id = ?, capacity = ?, room_type = ? " +
                "WHERE schedule_id = ?";
        // Use try-with-resources for the PreparedStatement, Connection is managed by caller
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, roomSchedule.getRoomId());
            stmt.setInt(2, roomSchedule.getCapacity());
            stmt.setString(3, roomSchedule.getRoomType());
            stmt.setString(4, roomSchedule.getId());

            int result = stmt.executeUpdate();
            if (result == 0) {
                LOGGER.log(Level.WARNING, "RoomSchedule details update affected 0 rows for schedule ID: " + roomSchedule.getId() + ". This might indicate a data inconsistency (base schedule exists but room_schedule entry is missing) or no changes were made.");
            }
            return result > -1; // Return true even if 0 rows affected, unless there was an error
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating RoomSchedule details for schedule ID: " + roomSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating RoomSchedule details for schedule ID: " + roomSchedule.getId(), e);
            // Wrap and re-throw as SQLException
            throw new SQLException("Unexpected error updating RoomSchedule details", e);
        }
    }

    // Helper method to update a StudentSchedule using an existing connection (part of a transaction)
    private boolean updateStudentSchedule(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        String updateSql = "UPDATE student_schedules SET student_id = ? " +
                "WHERE schedule_id = ?";
        // Use try-with-resources for the PreparedStatement, Connection is managed by caller
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, studentSchedule.getStudentId());
            stmt.setString(2, studentSchedule.getId());

            int result = stmt.executeUpdate();
            if (result == 0) {
                LOGGER.log(Level.WARNING, "StudentSchedule details update affected 0 rows for schedule ID: " + studentSchedule.getId() + ". This might indicate a data inconsistency (base schedule exists but student_schedule entry is missing) or no changes were made.");
            }
            return result > -1; // Return true even if 0 rows affected, unless there was an error
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating StudentSchedule details for schedule ID: " + studentSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating StudentSchedule details for schedule ID: " + studentSchedule.getId(), e);
            // Wrap and re-throw as SQLException
            throw new SQLException("Unexpected error updating StudentSchedule details", e);
        }
    }

    /**
     * Delete a schedule by ID
     * @param id The ID of the schedule to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete schedule with null or empty ID.");
            return false;
        }

        String typeQuery = "SELECT schedule_type FROM schedules WHERE id = ?";
        String deleteRoomScheduleSql = "DELETE FROM room_schedules WHERE schedule_id = ?";
        String deleteStudentScheduleSql = "DELETE FROM student_schedules WHERE schedule_id = ?";
        String deleteScheduleSql = "DELETE FROM schedules WHERE id = ?";

        Connection connection = null; // Declare connection outside try-with-resources for rollback access

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Start transaction

            String scheduleType = null;
            // Use try-with-resources for PreparedStatement and ResultSet
            try (PreparedStatement typeStmt = connection.prepareStatement(typeQuery)) {
                typeStmt.setString(1, id);
                try (ResultSet rs = typeStmt.executeQuery()) {
                    if (rs.next()) {
                        scheduleType = rs.getString("schedule_type");
                    } else {
                        LOGGER.log(Level.WARNING, "Attempted to delete non-existent schedule with ID: " + id);
                        return false; // Schedule ID not found, nothing to delete
                    }
                }
            } // typeStmt and rs are closed here by try-with-resources

            // Delete from specific subclass table first based on determined type
            // Use separate try-with-resources for each potential delete statement
            if ("ROOM".equals(scheduleType)) {
                try (PreparedStatement subStmt = connection.prepareStatement(deleteRoomScheduleSql)) {
                    subStmt.setString(1, id);
                    int subResult = subStmt.executeUpdate();
                    if (subResult == 0) {
                        LOGGER.log(Level.WARNING, "RoomSchedule details delete affected 0 rows for schedule ID: " + id + ". Possible data inconsistency.");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "SQL Error deleting room schedule details for ID: " + id, e);
                    throw e; // Re-throw to trigger outer catch and rollback
                }
            } else if ("STUDENT".equals(scheduleType)) {
                try (PreparedStatement subStmt = connection.prepareStatement(deleteStudentScheduleSql)) {
                    subStmt.setString(1, id);
                    int subResult = subStmt.executeUpdate();
                    if (subResult == 0) {
                        LOGGER.log(Level.WARNING, "StudentSchedule details delete affected 0 rows for schedule ID: " + id + ". Possible data inconsistency.");
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "SQL Error deleting student schedule details for ID: " + id, e);
                    throw e; // Re-throw to trigger outer catch and rollback
                }
            } else {
                // Unknown type, log a warning but proceed to delete from base table
                LOGGER.log(Level.WARNING, "Attempted to delete schedule with unknown type: " + scheduleType + " for ID: " + id + ". Proceeding with base schedule delete.");
            }

            // Then delete from base table
            // Use try-with-resources for the base delete statement
            try (PreparedStatement baseStmt = connection.prepareStatement(deleteScheduleSql)) {
                baseStmt.setString(1, id);
                int result = baseStmt.executeUpdate();

                if (result > 0) {
                    connection.commit();
                    LOGGER.log(Level.INFO, "Successfully deleted schedule with ID: " + id);
                    return true;
                } else {
                    // Base table delete failed (shouldn't happen if ID was found initially, but good practice)
                    LOGGER.log(Level.SEVERE, "Failed to delete base schedule for ID: " + id + " after subclass delete attempt.");
                    try {
                        connection.rollback(); // Rollback subclass delete as well
                    } catch (SQLException rbex) {
                        LOGGER.log(Level.SEVERE, "Error during rollback after failed base schedule delete for ID: " + id, rbex);
                    }
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error deleting base schedule for ID: " + id, e);
                throw e; // Re-throw to trigger outer catch and rollback
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during delete operation for schedule ID: " + id, e);
            try {
                // connection might be null here if getting connection failed
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + id);
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + id, ex);
            }
            return false; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during delete operation for schedule ID: " + id, e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + id);
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + id, ex);
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                    connection.close(); // Close the connection
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection after deleting schedule with ID: " + id, e);
                }
            }
        }
    }


    /**
     * Find schedules by time range
     * @param startTime The start time of the range.
     * @param endTime The end time of the range.
     * @return A list of schedules that overlap with the given time range, or an empty list if none found or an error occurs.
     */
    public List<Schedule> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Schedule> schedules = new ArrayList<>();
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null time range.");
            return schedules;
        }

        // This query logic finds schedules whose time range overlaps with the provided range.
        // (schedule.start_time <= endTime) AND (schedule.end_time >= startTime)
        String query = "SELECT id, schedule_type FROM schedules " +
                "WHERE (start_time <= ? AND end_time >= ?)";

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(endTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String scheduleType = rs.getString("schedule_type");

                    Schedule schedule = null;
                    // Reuse the same connection for fetching subclass details
                    if ("ROOM".equals(scheduleType)) {
                        schedule = findRoomScheduleById(connection, id);
                    } else if ("STUDENT".equals(scheduleType)) {
                        schedule = findStudentScheduleById(connection, id);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown schedule type encountered in time range search for ID: " + id + ". Type: " + scheduleType);
                    }

                    if (schedule != null) {
                        schedules.add(schedule);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by time range.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding schedules by time range.", e);
        }

        return schedules;
    }

    /**
     * Find schedules by name
     * @param name The name (or part of the name) to search for.
     * @return A list of schedules whose name matches or contains the search string, or an empty list if none found or an error occurs.
     */
    public List<Schedule> findByName(String name) {
        List<Schedule> schedules = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null or empty name.");
            return schedules;
        }

        String query = "SELECT id, schedule_type FROM schedules " +
                "WHERE name LIKE ?";

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, "%" + name + "%"); // Using LIKE for partial matching

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String scheduleType = rs.getString("schedule_type");

                    Schedule schedule = null;
                    // Reuse the same connection for fetching subclass details
                    if ("ROOM".equals(scheduleType)) {
                        schedule = findRoomScheduleById(connection, id);
                    } else if ("STUDENT".equals(scheduleType)) {
                        schedule = findStudentScheduleById(connection, id);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown schedule type encountered in name search for ID: " + id + ". Type: " + scheduleType);
                    }

                    if (schedule != null) {
                        schedules.add(schedule);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by name: " + name, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding schedules by name: " + name, e);
        }

        return schedules;
    }

    /**
     * Find schedules by class ID.
     * NOTE: This implementation assumes the 'classId' parameter corresponds to the
     * 'name' column in the 'schedules' table. This might need adjustment
     * based on the actual database schema and how classes are linked to schedules.
     * @param classId The ID of the class (assumed to match the 'name' column).
     * @return A list of schedules associated with the given class ID, or an empty list if none found or an error occurs.
     */
    public List<Schedule> findByClassId(String classId) {
        List<Schedule> schedules = new ArrayList<>();
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null or empty class ID.");
            return schedules;
        }

        // Assuming classId matches the 'name' column in the schedules table
        String query = "SELECT id, schedule_type FROM schedules " +
                "WHERE name = ?"; // Using '=' for exact match on assumed ID

        // Use try-with-resources for Connection, PreparedStatement, and ResultSet
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String scheduleType = rs.getString("schedule_type");

                    Schedule schedule = null;
                    // Reuse the same connection for fetching subclass details
                    if ("ROOM".equals(scheduleType)) {
                        schedule = findRoomScheduleById(connection, id);
                    } else if ("STUDENT".equals(scheduleType)) {
                        schedule = findStudentScheduleById(connection, id);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown schedule type encountered in class ID search for ID: " + id + ". Type: " + scheduleType);
                    }

                    if (schedule != null) {
                        schedules.add(schedule);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by class ID: " + classId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding schedules by class ID: " + classId, e);
        }

        return schedules;
    }
}
