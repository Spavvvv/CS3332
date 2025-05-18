
package src.dao;

import src.model.system.schedule.Schedule;
import src.model.system.schedule.RoomSchedule;
import src.model.system.schedule.StudentSchedule;
import utils.DatabaseConnection; // Assuming this utility class provides Connection objects

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for Schedule operations, managing persistence for Schedule and its subclasses.
 * This implementation uses JDBC and assumes a relational database schema where:
 * - A base 'schedules' table stores common schedule attributes and a 'schedule_type'.
 * - Specific tables like 'room_schedules' and 'student_schedules' store subclass-specific attributes,
 *   linking back to the 'schedules' table via a schedule_id.
 */
public class ScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(ScheduleDAO.class.getName());

    private static final String SCHEDULE_TYPE_ROOM = "ROOM";
    private static final String SCHEDULE_TYPE_STUDENT = "STUDENT";
    // Add other types like SCHEDULE_TYPE_ITEM if ScheduleItem becomes persistent

    /**
     * Default constructor.
     */
    public ScheduleDAO() {
        // Constructor for potential dependency injection in the future
    }

    /**
     * Finds a schedule by its ID.
     * Determines the specific type of schedule and fetches its full details.
     * @param id The ID of the schedule to find.
     * @return The found Schedule object (as RoomSchedule or StudentSchedule),
     *         or null if not found or an error occurs.
     */
    public Schedule findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedule with null or empty ID.");
            return null;
        }
        String baseQuery = "SELECT id, name, description, start_time, end_time, schedule_type FROM schedules WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement baseStmt = connection.prepareStatement(baseQuery)) {

            baseStmt.setString(1, id);
            try (ResultSet rs = baseStmt.executeQuery()) {
                if (rs.next()) {
                    String scheduleType = rs.getString("schedule_type");
                    // Pass the ResultSet and connection to helper methods to avoid re-querying base fields
                    if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                        return findRoomScheduleDetails(connection, rs);
                    } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                        return findStudentScheduleDetails(connection, rs);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown schedule type '" + scheduleType + "' found for ID: " + id);
                        // Optionally, could return a base Schedule object if that's meaningful
                        // return new Schedule(rs.getString("id"), rs.getString("name"), ...);
                        return null;
                    }
                } else {
                    LOGGER.log(Level.INFO, "No schedule found with ID: " + id);
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedule by ID: " + id, e);
            return null;
        } catch (Exception e) { // Catch broader exceptions from DatabaseConnection or other issues
            LOGGER.log(Level.SEVERE, "Unexpected error finding schedule by ID: " + id, e);
            return null;
        }
    }

    // Helper method to hydrate a RoomSchedule from a ResultSet containing base schedule info
    private RoomSchedule findRoomScheduleDetails(Connection connection, ResultSet baseRs) throws SQLException {
        String scheduleId = baseRs.getString("id");
        String query = "SELECT room_id, capacity, room_type FROM room_schedules WHERE schedule_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RoomSchedule(
                            scheduleId,
                            baseRs.getString("name"),
                            baseRs.getString("description"),
                            baseRs.getTimestamp("start_time") != null ? baseRs.getTimestamp("start_time").toLocalDateTime() : null,
                            baseRs.getTimestamp("end_time") != null ? baseRs.getTimestamp("end_time").toLocalDateTime() : null,
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );
                }
                LOGGER.log(Level.WARNING, "RoomSchedule details not found for schedule ID: " + scheduleId + ". Data inconsistency possible.");
                return null; // Should not happen if schedule_type was ROOM and foreign keys are enforced
            }
        }
    }

    // Helper method to hydrate a StudentSchedule from a ResultSet containing base schedule info
    private StudentSchedule findStudentScheduleDetails(Connection connection, ResultSet baseRs) throws SQLException {
        String scheduleId = baseRs.getString("id");
        String query = "SELECT student_id FROM student_schedules WHERE schedule_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentSchedule(
                            scheduleId,
                            baseRs.getString("name"),
                            baseRs.getString("description"),
                            baseRs.getTimestamp("start_time") != null ? baseRs.getTimestamp("start_time").toLocalDateTime() : null,
                            baseRs.getTimestamp("end_time") != null ? baseRs.getTimestamp("end_time").toLocalDateTime() : null,
                            rs.getString("student_id")
                    );
                }
                LOGGER.log(Level.WARNING, "StudentSchedule details not found for schedule ID: " + scheduleId + ". Data inconsistency possible.");
                return null; // Should not happen if schedule_type was STUDENT and foreign keys are enforced
            }
        }
    }

    // Original findById helpers, kept for compatibility if used directly elsewhere,
    // but findById now uses more efficient helpers above.
    private RoomSchedule findRoomScheduleById(Connection connection, String id) throws SQLException {
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s JOIN room_schedules rs ON s.id = rs.schedule_id WHERE s.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new RoomSchedule(
                            rs.getString("id"), rs.getString("name"), rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"), rs.getInt("capacity"), rs.getString("room_type"));
                }
                return null;
            }
        }
    }

    private StudentSchedule findStudentScheduleById(Connection connection, String id) throws SQLException {
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, ss.student_id " +
                "FROM schedules s JOIN student_schedules ss ON s.id = ss.schedule_id WHERE s.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StudentSchedule(
                            rs.getString("id"), rs.getString("name"), rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("student_id"));
                }
                return null;
            }
        }
    }


    /**
     * Retrieves all schedules from the database.
     * Note: This method can lead to N+1 queries (1 query for all base schedules, then N queries for
     * subclass details). For performance with large datasets, consider fetching all base schedules,
     * then all RoomSchedule details in one query, all StudentSchedule details in another, and then
     * merging them in memory.
     * @return A list of all Schedule objects, or an empty list if none found or an error occurs.
     */
    public List<Schedule> findAll() {
        List<Schedule> schedules = new ArrayList<>();
        // Query all base schedule fields needed by the findXXXScheduleDetails helpers
        String baseQuery = "SELECT id, name, description, start_time, end_time, schedule_type FROM schedules";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(baseQuery)) {

            while (rs.next()) {
                String scheduleType = rs.getString("schedule_type");
                Schedule schedule = null;

                if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                    schedule = findRoomScheduleDetails(connection, rs);
                } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                    schedule = findStudentScheduleDetails(connection, rs);
                } else {
                    LOGGER.log(Level.WARNING, "findAll: Unknown schedule type '" + scheduleType + "' for ID: " + rs.getString("id"));
                }

                if (schedule != null) {
                    schedules.add(schedule);
                } else if (scheduleType != null && (scheduleType.equals(SCHEDULE_TYPE_ROOM) || scheduleType.equals(SCHEDULE_TYPE_STUDENT))) {
                    // This case implies findXXXScheduleDetails returned null, possibly due to missing subclass record.
                    // It's already logged by the helper.
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
     * Saves a schedule (either new or an update if existing logic is INSERT OR UPDATE).
     * This implementation assumes it's for new schedules. Use `update` for existing ones.
     * Manages a transaction to ensure atomicity for base and subclass table insertions.
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
            return false;
        }

        String insertScheduleSql = "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = connection.prepareStatement(insertScheduleSql)) {
                stmt.setString(1, schedule.getId());
                stmt.setString(2, schedule.getName());
                stmt.setString(3, schedule.getDescription());
                stmt.setTimestamp(4, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
                stmt.setTimestamp(5, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);

                boolean subclassSaveSuccess = false;
                String scheduleTypeForDb = null;

                if (schedule instanceof RoomSchedule roomSchedule) {
                    scheduleTypeForDb = SCHEDULE_TYPE_ROOM;
                    stmt.setString(6, scheduleTypeForDb);
                    int baseResult = stmt.executeUpdate();
                    if (baseResult > 0) {
                        subclassSaveSuccess = saveRoomScheduleDetails(connection, roomSchedule);
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to insert base schedule for RoomSchedule ID: " + schedule.getId());
                    }
                } else if (schedule instanceof StudentSchedule studentSchedule) {
                    scheduleTypeForDb = SCHEDULE_TYPE_STUDENT;
                    stmt.setString(6, scheduleTypeForDb);
                    int baseResult = stmt.executeUpdate();
                    if (baseResult > 0) {
                        subclassSaveSuccess = saveStudentScheduleDetails(connection, studentSchedule);
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to insert base schedule for StudentSchedule ID: " + schedule.getId());
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Attempted to save schedule of unknown type: " + schedule.getClass().getName());
                    connection.rollback(); // Rollback if type is unknown before any DML
                    return false;
                }

                if (subclassSaveSuccess) {
                    connection.commit();
                    return true;
                } else {
                    LOGGER.log(Level.SEVERE, "Subclass save failed for schedule ID: " + schedule.getId() + " (Type: " + scheduleTypeForDb + "). Rolling back.");
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error saving schedule ID: " + schedule.getId(), e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving schedule ID: " + schedule.getId(), e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing connection for schedule ID: " + (schedule != null ? schedule.getId() : "N/A"), e);
            }
        }
    }

    // Helper to save RoomSchedule specific details
    private boolean saveRoomScheduleDetails(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        String insertSql = "INSERT INTO room_schedules (schedule_id, room_id, capacity, room_type) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, roomSchedule.getId());
            stmt.setString(2, roomSchedule.getRoomId());
            stmt.setInt(3, roomSchedule.getCapacity());
            stmt.setString(4, roomSchedule.getRoomType());
            return stmt.executeUpdate() > 0;
        }
    }

    // Helper to save StudentSchedule specific details
    private boolean saveStudentScheduleDetails(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        String insertSql = "INSERT INTO student_schedules (schedule_id, student_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            stmt.setString(1, studentSchedule.getId());
            stmt.setString(2, studentSchedule.getStudentId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing schedule.
     * Manages a transaction to ensure atomicity for base and subclass table updates.
     * @param schedule The Schedule object to update. Its ID must exist in the database.
     * @return true if the update was successful, false otherwise.
     */
    public boolean update(Schedule schedule) {
        if (schedule == null || schedule.getId() == null || schedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update null schedule or schedule with null/empty ID.");
            return false;
        }

        String updateScheduleSql = "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            boolean baseUpdateSuccess;
            try (PreparedStatement stmt = connection.prepareStatement(updateScheduleSql)) {
                stmt.setString(1, schedule.getName());
                stmt.setString(2, schedule.getDescription());
                stmt.setTimestamp(3, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
                stmt.setTimestamp(4, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);
                stmt.setString(5, schedule.getId());
                int baseResult = stmt.executeUpdate();
                baseUpdateSuccess = baseResult > 0;
                if (!baseUpdateSuccess) {
                    LOGGER.log(Level.WARNING, "Base schedule update affected 0 rows for ID: " + schedule.getId() + ". Schedule might not exist or no changes to base fields.");
                }
            }

            boolean subclassUpdateSuccess = true; // Assume true if no subclass update needed or subclass update succeeds
            if (baseUpdateSuccess) { // Or if you want to update subclass even if base had 0 rows affected: (baseResult >= 0)
                if (schedule instanceof RoomSchedule roomSchedule) {
                    subclassUpdateSuccess = updateRoomScheduleDetails(connection, roomSchedule);
                } else if (schedule instanceof StudentSchedule studentSchedule) {
                    subclassUpdateSuccess = updateStudentScheduleDetails(connection, studentSchedule);
                }
                // If not a known subclass, subclassUpdateSuccess remains true.
            }


            if (baseUpdateSuccess && subclassUpdateSuccess) {
                connection.commit();
                return true;
            } else {
                // If baseUpdateSuccess is false, it means the schedule ID might not exist in `schedules` table, or no base fields changed.
                // If subclassUpdateSuccess is false, it means subclass update failed or affected 0 rows critically.
                LOGGER.log(Level.WARNING, "Update failed or resulted in no changes for schedule ID: " + schedule.getId() + ". Base success: " + baseUpdateSuccess + ", Subclass success: " + subclassUpdateSuccess + ". Rolling back.");
                connection.rollback();
                return false; // False if base OR subclass update explicitly failed or if base not found
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error updating schedule ID: " + schedule.getId(), e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating schedule ID: " + schedule.getId(), e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing connection for schedule ID: " + (schedule != null ? schedule.getId() : "N/A"), e);
            }
        }
    }

    private boolean updateRoomScheduleDetails(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        String updateSql = "UPDATE room_schedules SET room_id = ?, capacity = ?, room_type = ? WHERE schedule_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, roomSchedule.getRoomId());
            stmt.setInt(2, roomSchedule.getCapacity());
            stmt.setString(3, roomSchedule.getRoomType());
            stmt.setString(4, roomSchedule.getId());
            int result = stmt.executeUpdate();
            if (result == 0) {
                LOGGER.log(Level.INFO, "RoomSchedule details update affected 0 rows for ID: " + roomSchedule.getId() + ". Room details may not exist or no changes made.");
            }
            return result >= 0; // Success if no error, even if 0 rows affected (idempotent or no change)
        }
    }

    private boolean updateStudentScheduleDetails(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        String updateSql = "UPDATE student_schedules SET student_id = ? WHERE schedule_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, studentSchedule.getStudentId());
            stmt.setString(2, studentSchedule.getId());
            int result = stmt.executeUpdate();
            if (result == 0) {
                LOGGER.log(Level.INFO, "StudentSchedule details update affected 0 rows for ID: " + studentSchedule.getId() + ". Student details may not exist or no changes made.");
            }
            return result >= 0; // Success if no error
        }
    }

    /**
     * Deletes a schedule by ID.
     * Manages a transaction to ensure atomicity for base and subclass table deletions.
     * @param id The ID of the schedule to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete schedule with null or empty ID.");
            return false;
        }

        String typeQuery = "SELECT schedule_type FROM schedules WHERE id = ?";
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            String scheduleType = null;
            try (PreparedStatement typeStmt = connection.prepareStatement(typeQuery)) {
                typeStmt.setString(1, id);
                try (ResultSet rs = typeStmt.executeQuery()) {
                    if (rs.next()) {
                        scheduleType = rs.getString("schedule_type");
                    } else {
                        LOGGER.log(Level.WARNING, "Delete: Schedule not found with ID: " + id + ". Nothing to delete.");
                        // No need to rollback as no DML operations performed yet.
                        return false;
                    }
                }
            }

            // Delete from specific subclass table first
            if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                try (PreparedStatement subStmt = connection.prepareStatement("DELETE FROM room_schedules WHERE schedule_id = ?")) {
                    subStmt.setString(1, id);
                    subStmt.executeUpdate(); // Result doesn't strictly matter, foreign key might cascade or entry might not exist
                }
            } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                try (PreparedStatement subStmt = connection.prepareStatement("DELETE FROM student_schedules WHERE schedule_id = ?")) {
                    subStmt.setString(1, id);
                    subStmt.executeUpdate();
                }
            } else {
                LOGGER.log(Level.WARNING, "Delete: Unknown schedule type '" + scheduleType + "' for ID: " + id + ". Attempting to delete from base table only.");
            }

            // Then delete from base table
            try (PreparedStatement baseStmt = connection.prepareStatement("DELETE FROM schedules WHERE id = ?")) {
                baseStmt.setString(1, id);
                int result = baseStmt.executeUpdate();
                if (result > 0) {
                    connection.commit();
                    return true;
                } else {
                    // Should not happen if ID was found by typeQuery, implies an issue.
                    LOGGER.log(Level.SEVERE, "Failed to delete base schedule for ID: " + id + " after subclass delete attempt. Rolling back.");
                    connection.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error deleting schedule ID: " + id, e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + id, ex);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting schedule ID: " + id, e);
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + id, ex);
            }
            return false;
        } finally {
            try {
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing connection for schedule ID: " + id, e);
            }
        }
    }

    /**
     * Finds schedules that overlap with the given time range.
     * A schedule overlaps if (schedule.startTime <= query.endTime) AND (schedule.endTime >= query.startTime).
     * Note: This can also suffer from N+1 query problem similar to findAll.
     * @param startTime The start time of the range.
     * @param endTime The end time of the range.
     * @return A list of schedules within the range.
     */
    public List<Schedule> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Schedule> schedules = new ArrayList<>();
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            LOGGER.log(Level.WARNING, "Invalid time range provided for search.");
            return schedules;
        }

        String query = "SELECT id, name, description, start_time, end_time, schedule_type FROM schedules " +
                "WHERE start_time <= ? AND end_time >= ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(endTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String scheduleType = rs.getString("schedule_type");
                    Schedule schedule = null;
                    if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                        schedule = findRoomScheduleDetails(connection, rs);
                    } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                        schedule = findStudentScheduleDetails(connection, rs);
                    } else {
                        LOGGER.log(Level.WARNING, "findByTimeRange: Unknown schedule type '" + scheduleType + "' for ID: " + rs.getString("id"));
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
     * Finds schedules by name (case-insensitive partial match).
     * Note: This can also suffer from N+1 query problem.
     * @param name The name (or part of the name) to search for.
     * @return A list of matching schedules.
     */
    public List<Schedule> findByName(String name) {
        List<Schedule> schedules = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            LOGGER.log(Level.INFO, "Find by name called with null or empty name.");
            return schedules;
        }

        String query = "SELECT id, name, description, start_time, end_time, schedule_type FROM schedules WHERE LOWER(name) LIKE LOWER(?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, "%" + name + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String scheduleType = rs.getString("schedule_type");
                    Schedule schedule = null;
                    if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                        schedule = findRoomScheduleDetails(connection, rs);
                    } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                        schedule = findStudentScheduleDetails(connection, rs);
                    } else {
                        LOGGER.log(Level.WARNING, "findByName: Unknown schedule type '" + scheduleType + "' for ID: " + rs.getString("id"));
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
     * Finds schedules by class ID (exact match on 'name' column).
     * Note: The assumption that class ID maps to schedule 'name' is based on original code.
     * This can also suffer from N+1 query problem.
     * @param classId The class ID to search for (matched against schedule name).
     * @return A list of matching schedules.
     */
    public List<Schedule> findByClassId(String classId) {
        List<Schedule> schedules = new ArrayList<>();
        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.INFO, "Find by class ID called with null or empty class ID.");
            return schedules;
        }

        String query = "SELECT id, name, description, start_time, end_time, schedule_type FROM schedules WHERE name = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String scheduleType = rs.getString("schedule_type");
                    Schedule schedule = null;
                    if (SCHEDULE_TYPE_ROOM.equals(scheduleType)) {
                        schedule = findRoomScheduleDetails(connection, rs);
                    } else if (SCHEDULE_TYPE_STUDENT.equals(scheduleType)) {
                        schedule = findStudentScheduleDetails(connection, rs);
                    } else {
                        LOGGER.log(Level.WARNING, "findByClassId: Unknown schedule type '" + scheduleType + "' for ID: " + rs.getString("id"));
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

