package src.dao;

import src.model.system.schedule.Schedule;
import src.model.system.schedule.RoomSchedule;
import src.model.system.schedule.StudentSchedule;
import src.model.system.course.Course;
import src.model.system.course.CourseDate;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
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
     * Find a schedule by ID
     */
    public Schedule findById(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Schedule schedule = null;

        try {
            connection = DatabaseConnection.getConnection();

            // First query the schedule base table
            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules WHERE id = ?"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String scheduleType = rs.getString("schedule_type");

                // Depending on the type, query the specific subclass table using the existing connection
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }
            }

            return schedule;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedule by ID: " + id, e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    // Helper method to find a RoomSchedule by ID
    private RoomSchedule findRoomScheduleById(Connection connection, String id) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Join the schedules table with the room_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE s.id = ?"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

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
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding RoomSchedule by ID: " + id, e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }

    // Helper method to find a StudentSchedule by ID
    private StudentSchedule findStudentScheduleById(Connection connection, String id) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Join the schedules table with the student_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.id, s.name, s.description, s.start_time, s.end_time, ss.student_id " +
                            "FROM schedules s " +
                            "JOIN student_schedules ss ON s.id = ss.schedule_id " +
                            "WHERE s.id = ?"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

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
            return null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding StudentSchedule by ID: " + id, e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }

    /**
     * Find all schedules
     */
    public List<Schedule> findAll() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<Schedule> schedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            stmt = connection.createStatement();
            rs = stmt.executeQuery("SELECT id, schedule_type FROM schedules");

            while (rs.next()) {
                String id = rs.getString("id");
                String scheduleType = rs.getString("schedule_type");

                Schedule schedule = null;
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }

                if (schedule != null) {
                    schedules.add(schedule);
                }
            }

            return schedules;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all schedules", e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Statement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Save a schedule
     */
    public boolean save(Schedule schedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        if (schedule == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null schedule.");
            return false;
        }
        if (schedule.getId() == null || schedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save a schedule with null or empty ID.");
            return false; // Cannot save without a valid ID
        }


        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert into base schedule table
            stmt = connection.prepareStatement(
                    "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setString(1, schedule.getId());
            stmt.setString(2, schedule.getName());
            stmt.setString(3, schedule.getDescription());
            stmt.setTimestamp(4, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
            stmt.setTimestamp(5, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);

            boolean subclassSaveSuccess = false; // Flag to track if subclass save is successful

            // Determine schedule type and handle subclass-specific persistence
            if (schedule instanceof RoomSchedule) {
                stmt.setString(6, "ROOM");
                int result = stmt.executeUpdate();
                if (result > 0) {
                    subclassSaveSuccess = saveRoomSchedule(connection, (RoomSchedule) schedule);
                }
            } else if (schedule instanceof StudentSchedule) {
                stmt.setString(6, "STUDENT");
                int result = stmt.executeUpdate();
                if (result > 0) {
                    subclassSaveSuccess = saveStudentSchedule(connection, (StudentSchedule) schedule);
                }
            } else {
                // Unknown schedule type
                LOGGER.log(Level.SEVERE, "Attempted to save schedule with unknown type: " + schedule.getClass().getName());
                connection.rollback(); // Rollback the base insert
                return false;
            }

            if (subclassSaveSuccess) {
                connection.commit();
                return true;
            } else {
                LOGGER.log(Level.SEVERE, "Failed to save subclass schedule for schedule ID: " + schedule.getId());
                connection.rollback(); // Rollback base insert if subclass save fails
                return false;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving schedule with ID: " + schedule.getId(), e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            throw e;
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing Connection", e);
                }
            }
        }
    }

    // Helper method to save a RoomSchedule
    private boolean saveRoomSchedule(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(
                    "INSERT INTO room_schedules (schedule_id, room_id, capacity, room_type) " +
                            "VALUES (?, ?, ?, ?)"
            );
            stmt.setString(1, roomSchedule.getId());
            stmt.setString(2, roomSchedule.getRoomId());
            stmt.setInt(3, roomSchedule.getCapacity());
            stmt.setString(4, roomSchedule.getRoomType());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving RoomSchedule for schedule ID: " + roomSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }

    // Helper method to save a StudentSchedule
    private boolean saveStudentSchedule(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(
                    "INSERT INTO student_schedules (schedule_id, student_id) " +
                            "VALUES (?, ?)"
            );
            stmt.setString(1, studentSchedule.getId());
            stmt.setString(2, studentSchedule.getStudentId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving StudentSchedule for schedule ID: " + studentSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }


    /**
     * Update a schedule
     */
    public boolean update(Schedule schedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        if (schedule == null || schedule.getId() == null || schedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null schedule or schedule with null/empty ID.");
            return false;
        }

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Update base schedule table
            stmt = connection.prepareStatement(
                    "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? " +
                            "WHERE id = ?"
            );
            stmt.setString(1, schedule.getName());
            stmt.setString(2, schedule.getDescription());
            stmt.setTimestamp(3, schedule.getStartTime() != null ? Timestamp.valueOf(schedule.getStartTime()) : null);
            stmt.setTimestamp(4, schedule.getEndTime() != null ? Timestamp.valueOf(schedule.getEndTime()) : null);
            stmt.setString(5, schedule.getId());

            int result = stmt.executeUpdate();

            if (result > 0) {
                if (schedule instanceof RoomSchedule) {
                    updateRoomSchedule(connection, (RoomSchedule) schedule);
                } else if (schedule instanceof StudentSchedule) {
                    updateStudentSchedule(connection, (StudentSchedule) schedule);
                }
            }

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating schedule with ID: " + schedule.getId(), e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + schedule.getId());
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + schedule.getId(), ex);
            }
            throw e;
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing Connection", e);
                }
            }
        }
    }

    // Helper method to update a RoomSchedule
    private boolean updateRoomSchedule(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(
                    "UPDATE room_schedules SET room_id = ?, capacity = ?, room_type = ? " +
                            "WHERE schedule_id = ?"
            );
            stmt.setString(1, roomSchedule.getRoomId());
            stmt.setInt(2, roomSchedule.getCapacity());
            stmt.setString(3, roomSchedule.getRoomType());
            stmt.setString(4, roomSchedule.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating RoomSchedule for schedule ID: " + roomSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }

    // Helper method to update a StudentSchedule
    private boolean updateStudentSchedule(Connection connection, StudentSchedule studentSchedule) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(
                    "UPDATE student_schedules SET student_id = ? " +
                            "WHERE schedule_id = ?"
            );
            stmt.setString(1, studentSchedule.getStudentId());
            stmt.setString(2, studentSchedule.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating StudentSchedule for schedule ID: " + studentSchedule.getId(), e);
            throw e; // Re-throw to allow rollback in calling method
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
        }
    }

    /**
     * Delete a schedule by ID
     */
    public boolean delete(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete schedule with null or empty ID.");
            return false;
        }

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // First determine the schedule type
            stmt = connection.prepareStatement("SELECT schedule_type FROM schedules WHERE id = ?");
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            boolean success = false; // Track overall success

            if (rs.next()) {
                String scheduleType = rs.getString("schedule_type");
                rs.close();
                stmt.close();

                // Delete from specific subclass table first
                if ("ROOM".equals(scheduleType)) {
                    stmt = connection.prepareStatement("DELETE FROM room_schedules WHERE schedule_id = ?");
                    stmt.setString(1, id);
                    stmt.executeUpdate(); // Execute, but result doesn't strictly determine success of base delete
                    if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
                } else if ("STUDENT".equals(scheduleType)) {
                    stmt = connection.prepareStatement("DELETE FROM student_schedules WHERE schedule_id = ?");
                    stmt.setString(1, id);
                    stmt.executeUpdate(); // Execute, but result doesn't strictly determine success of base delete
                    if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
                } else {
                    // Unknown type, proceed to delete from base table
                    LOGGER.log(Level.WARNING, "Attempted to delete schedule with unknown type: " + scheduleType + " for ID: " + id);
                }

                // Then delete from base table
                stmt = connection.prepareStatement("DELETE FROM schedules WHERE id = ?");
                stmt.setString(1, id);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    connection.commit();
                    success = true;
                } else {
                    // Base table delete failed
                    LOGGER.log(Level.SEVERE, "Failed to delete base schedule for ID: " + id);
                    connection.rollback(); // Rollback subclass delete as well
                    success = false;
                }
            } else {
                // Schedule ID not found
                LOGGER.log(Level.WARNING, "Attempted to delete non-existent schedule with ID: " + id);
                success = false;
            }

            return success;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting schedule with ID: " + id, e);
            try {
                if (connection != null) {
                    LOGGER.log(Level.INFO, "Performing rollback for schedule ID: " + id);
                    connection.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error during rollback for schedule ID: " + id, ex);
            }
            throw e;
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing Connection", e);
                }
            }
        }
    }


    /**
     * Find schedules by time range
     */
    public List<Schedule> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Schedule> schedules = new ArrayList<>();

        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null time range.");
            return new ArrayList<>();
        }

        try {
            connection = DatabaseConnection.getConnection();

            // This query logic needs to be correct for overlapping time ranges.
            // A common pattern is: (start1 <= end2) AND (end1 >= start2)
            // Here, (schedule.start_time <= endTime) AND (schedule.end_time >= startTime)
            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules " +
                            "WHERE (start_time <= ? AND end_time >= ?)"
            );
            stmt.setTimestamp(1, Timestamp.valueOf(endTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));


            rs = stmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String scheduleType = rs.getString("schedule_type");

                Schedule schedule = null;
                // Reuse the same connection for fetching subclass details
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }

                if (schedule != null) {
                    schedules.add(schedule);
                }
            }

            return schedules;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by time range.", e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Find schedules by name
     */
    public List<Schedule> findByName(String name) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Schedule> schedules = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null or empty name.");
            return new ArrayList<>();
        }

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules " +
                            "WHERE name LIKE ?"
            );
            stmt.setString(1, "%" + name + "%"); // Using LIKE for partial matching

            rs = stmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String scheduleType = rs.getString("schedule_type");

                Schedule schedule = null;
                // Reuse the same connection for fetching subclass details
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }

                if (schedule != null) {
                    schedules.add(schedule);
                }
            }

            return schedules;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by name: " + name, e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Find schedules by class ID.
     * NOTE: This implementation assumes the 'classId' parameter corresponds to the
     * 'name' column in the 'schedules' table. This might need adjustment
     * based on the actual database schema and how classes are linked to schedules.
     * @param classId The ID of the class (assumed to match the 'name' column).
     * @return A list of schedules associated with the given class ID.
     * @throws SQLException If a database access error occurs.
     */
    public List<Schedule> findByClassId(String classId) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Schedule> schedules = new ArrayList<>();

        if (classId == null || classId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find schedules with null or empty class ID.");
            return new ArrayList<>();
        }

        try {
            connection = DatabaseConnection.getConnection();

            // Assuming classId matches the 'name' column in the schedules table
            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules " +
                            "WHERE name = ?" // Using '=' for exact match on assumed ID
            );
            stmt.setString(1, classId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("id");
                String scheduleType = rs.getString("schedule_type");

                Schedule schedule = null;
                // Reuse the same connection for fetching subclass details
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }

                if (schedule != null) {
                    schedules.add(schedule);
                }
            }

            return schedules;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding schedules by class ID: " + classId, e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }
}
