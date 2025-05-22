package src.dao.Schedule;

import src.model.system.schedule.RoomSchedule;
import src.model.system.course.Course;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for RoomSchedule operations
 */
public class RoomScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(RoomScheduleDAO.class.getName());

    /**
     * Constructor.
     */
    public RoomScheduleDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Find a room schedule by ID
     */
    public RoomSchedule findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find room schedule with null or empty ID.");
            return null;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE s.id = ? AND s.schedule_type = 'ROOM'";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RoomSchedule roomSchedule = new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );

                    // Load scheduled courses
                    loadScheduledCourses(connection, roomSchedule);

                    return roomSchedule;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding room schedule by ID: " + id, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding room schedule by ID: " + id, e);
        }

        return null;
    }

    // Helper method to load scheduled courses for a room - uses existing connection
    private void loadScheduledCourses(Connection connection, RoomSchedule roomSchedule) {
        if (connection == null || roomSchedule == null || roomSchedule.getRoomId() == null) {
            LOGGER.log(Level.WARNING, "Attempted to load scheduled courses with null connection, roomSchedule, or room ID.");
            return;
        }
        String query = "SELECT course_id, course_name, subject, start_date, end_date, room_id " +
                "FROM courses WHERE room_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, roomSchedule.getRoomId());
            try (ResultSet rs = stmt.executeQuery()) {
                List<Course> scheduledCourses = new ArrayList<>();
                while (rs.next()) {
                    Course course = new Course(
                            rs.getString("course_id"),
                            rs.getString("course_name"),
                            rs.getString("subject"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate()
                            // Assuming Course constructor or setters handle other fields if necessary
                    );
                    course.setRoomId(rs.getString("room_id"));

                    scheduledCourses.add(course);
                }
                roomSchedule.setScheduledCourses(scheduledCourses);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading scheduled courses for room: " + roomSchedule.getRoomId(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error loading scheduled courses for room: " + roomSchedule.getRoomId(), e);
        }
    }

    /**
     * Find all room schedules
     */
    public List<RoomSchedule> findAll() {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE s.schedule_type = 'ROOM'";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                RoomSchedule roomSchedule = new RoomSchedule(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getString("room_id"),
                        rs.getInt("capacity"),
                        rs.getString("room_type")
                );

                // Load scheduled courses using the same connection
                loadScheduledCourses(connection, roomSchedule);

                roomSchedules.add(roomSchedule);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all room schedules.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding all room schedules.", e);
        }

        return roomSchedules;
    }

    /**
     * Save a room schedule
     */
    public boolean save(RoomSchedule roomSchedule) {
        if (roomSchedule == null || roomSchedule.getId() == null || roomSchedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to save a null or invalid room schedule.");
            return false;
        }
        String insertScheduleSql = "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                "VALUES (?, ?, ?, ?, ?, 'ROOM')";
        String insertRoomScheduleSql = "INSERT INTO room_schedules (schedule_id, room_id, capacity, room_type) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false); // Start transaction

            try (PreparedStatement stmtSchedule = connection.prepareStatement(insertScheduleSql)) {
                stmtSchedule.setString(1, roomSchedule.getId());
                stmtSchedule.setString(2, roomSchedule.getName());
                stmtSchedule.setString(3, roomSchedule.getDescription());
                stmtSchedule.setTimestamp(4, Timestamp.valueOf(roomSchedule.getStartTime()));
                stmtSchedule.setTimestamp(5, Timestamp.valueOf(roomSchedule.getEndTime()));

                int resultSchedule = stmtSchedule.executeUpdate();

                if (resultSchedule > 0) {
                    try (PreparedStatement stmtRoomSchedule = connection.prepareStatement(insertRoomScheduleSql)) {
                        stmtRoomSchedule.setString(1, roomSchedule.getId());
                        stmtRoomSchedule.setString(2, roomSchedule.getRoomId());
                        stmtRoomSchedule.setInt(3, roomSchedule.getCapacity());
                        stmtRoomSchedule.setString(4, roomSchedule.getRoomType());

                        int resultRoomSchedule = stmtRoomSchedule.executeUpdate();

                        if (resultRoomSchedule > 0) {
                            connection.commit(); // Commit transaction
                            return true;
                        } else {
                            LOGGER.log(Level.SEVERE, "Inserting into room_schedules failed for ID: " + roomSchedule.getId() + ", rolling back.");
                            connection.rollback();
                            return false;
                        }
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Inserting into schedules failed for ID: " + roomSchedule.getId() + ", rolling back.");
                    connection.rollback();
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error during saving room schedule with ID: " + roomSchedule.getId(), e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during saving room schedule with ID: " + roomSchedule.getId(), e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } finally {
                connection.setAutoCommit(true); // Restore default
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during saving room schedule.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected database connection error during saving room schedule.", e);
            return false;
        }
    }

    /**
     * Update a room schedule
     */
    public boolean update(RoomSchedule roomSchedule) {
        if (roomSchedule == null || roomSchedule.getId() == null || roomSchedule.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null or invalid room schedule.");
            return false;
        }
        String updateScheduleSql = "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? " +
                "WHERE id = ?";
        String updateRoomScheduleSql = "UPDATE room_schedules SET room_id = ?, capacity = ?, room_type = ? " +
                "WHERE schedule_id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false); // Start transaction

            try (PreparedStatement stmtSchedule = connection.prepareStatement(updateScheduleSql)) {
                stmtSchedule.setString(1, roomSchedule.getName());
                stmtSchedule.setString(2, roomSchedule.getDescription());
                stmtSchedule.setTimestamp(3, Timestamp.valueOf(roomSchedule.getStartTime()));
                stmtSchedule.setTimestamp(4, Timestamp.valueOf(roomSchedule.getEndTime()));
                stmtSchedule.setString(5, roomSchedule.getId());

                int resultSchedule = stmtSchedule.executeUpdate();

                try (PreparedStatement stmtRoomSchedule = connection.prepareStatement(updateRoomScheduleSql)) {
                    stmtRoomSchedule.setString(1, roomSchedule.getRoomId());
                    stmtRoomSchedule.setInt(2, roomSchedule.getCapacity());
                    stmtRoomSchedule.setString(3, roomSchedule.getRoomType());
                    stmtRoomSchedule.setString(4, roomSchedule.getId());

                    int resultRoomSchedule = stmtRoomSchedule.executeUpdate();

                    if (resultSchedule > 0 && resultRoomSchedule > 0) {
                        connection.commit(); // Commit transaction
                        return true;
                    } else {
                        LOGGER.log(Level.SEVERE, "Updating room schedule failed for ID: " + roomSchedule.getId() + ", resultSchedule: " + resultSchedule + ", resultRoomSchedule: " + resultRoomSchedule + ", rolling back.");
                        connection.rollback();
                        return false;
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "SQL Error updating room schedule with ID: " + roomSchedule.getId(), e);
                    connection.rollback(); // Rollback transaction on error
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error updating base schedule for ID: " + roomSchedule.getId(), e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error updating room schedule with ID: " + roomSchedule.getId(), e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } finally {
                connection.setAutoCommit(true); // Restore default
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during updating room schedule.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected database connection error during updating room schedule.", e);
            return false;
        }
    }

    /**
     * Delete a room schedule by ID
     */
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to delete room schedule with null or empty ID.");
            return false;
        }
        String deleteRoomScheduleSql = "DELETE FROM room_schedules WHERE schedule_id = ?";
        String deleteScheduleSql = "DELETE FROM schedules WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false); // Start transaction

            try (PreparedStatement stmtRoomSchedule = connection.prepareStatement(deleteRoomScheduleSql)) {
                stmtRoomSchedule.setString(1, id);
                stmtRoomSchedule.executeUpdate(); // Execute deletion in room_schedules

                try (PreparedStatement stmtSchedule = connection.prepareStatement(deleteScheduleSql)) {
                    stmtSchedule.setString(1, id);
                    int resultSchedule = stmtSchedule.executeUpdate(); // Execute deletion in schedules

                    if (resultSchedule > 0) {
                        connection.commit(); // Commit transaction
                        return true;
                    } else {
                        LOGGER.log(Level.SEVERE, "Deleting base schedule failed for ID: " + id + ", rolling back.");
                        connection.rollback();
                        return false;
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "SQL Error deleting base schedule for ID: " + id, e);
                    connection.rollback(); // Rollback transaction on error
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error deleting room schedule details for ID: " + id, e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error deleting room schedule with ID: " + id, e);
                connection.rollback(); // Rollback transaction on error
                return false;
            } finally {
                connection.setAutoCommit(true); // Restore default
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error during deleting room schedule.", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected database connection error during deleting room schedule.", e);
            return false;
        }
    }

    /**
     * Find room schedules by room ID
     */
    public List<RoomSchedule> findByRoomId(String roomId) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (roomId == null || roomId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find room schedule by null or empty room ID.");
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.room_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RoomSchedule roomSchedule = new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );

                    // Load scheduled courses using the same connection
                    loadScheduledCourses(connection, roomSchedule);

                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding room schedules by room ID: " + roomId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding room schedules by room ID: " + roomId, e);
        }

        return roomSchedules;
    }

    /**
     * Find available room schedules for a specified time period
     * This logic assumes that 'available' means the room schedule itself overlaps
     * with the requested time period, NOT that no courses are scheduled within it.
     * The original code had commented-out logic for checking courses, which was
     * inconsistent with the SQL query filtering on the room schedule's times.
     * This version returns room schedules that are 'active' during the time period.
     * Further availability checks (e.g., no overlapping courses) would need separate logic.
     */
    public List<RoomSchedule> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
        List<RoomSchedule> availableRooms = new ArrayList<>();
        if (startTime == null || endTime == null) {
            LOGGER.log(Level.WARNING, "Attempted to find available rooms with null time period.");
            return availableRooms;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE s.schedule_type = 'ROOM' " +
                "AND s.start_time <= ? AND s.end_time >= ?"; // Room schedule is active during the period

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(startTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RoomSchedule roomSchedule = new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );

                    // Load scheduled courses using the same connection
                    loadScheduledCourses(connection, roomSchedule);

                    // Note: The original logic for checking overlapping courses was commented out
                    // If true availability (no overlapping courses) is needed, additional logic
                    // or a different SQL query would be required here.
                    // This current implementation returns rooms whose general schedule *overlaps*
                    // the requested time, not necessarily rooms that are *free* during that time.

                    availableRooms.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding available room schedules.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding available room schedules.", e);
        }

        return availableRooms;
    }

    /**
     * Assign a course to a room (by updating the course's room_id)
     */
    public boolean assignCourseToRoom(String courseId, String roomId) {
        if (courseId == null || courseId.trim().isEmpty() || roomId == null || roomId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to assign course to room with null or empty IDs.");
            return false;
        }
        String updateSql = "UPDATE courses SET room_id = ? WHERE course_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateSql)) {

            stmt.setString(1, roomId);
            stmt.setString(2, courseId);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error assigning course ID " + courseId + " to room ID " + roomId + ".", e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error assigning course ID " + courseId + " to room ID " + roomId + ".", e);
            return false;
        }
    }

    /**
     * Find rooms by type
     */
    public List<RoomSchedule> findByRoomType(String roomType) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (roomType == null || roomType.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find rooms by null or empty room type.");
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.room_type = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, roomType);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RoomSchedule roomSchedule = new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );

                    // Load scheduled courses using the same connection
                    loadScheduledCourses(connection, roomSchedule);

                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rooms by type: " + roomType, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding rooms by type: " + roomType, e);
        }

        return roomSchedules;
    }

    /**
     * Find rooms by minimum capacity
     */
    public List<RoomSchedule> findByMinimumCapacity(int minCapacity) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (minCapacity < 0) {
            LOGGER.log(Level.WARNING, "Attempted to find rooms with negative minimum capacity: " + minCapacity);
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.capacity >= ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, minCapacity);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RoomSchedule roomSchedule = new RoomSchedule(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("start_time").toLocalDateTime(),
                            rs.getTimestamp("end_time").toLocalDateTime(),
                            rs.getString("room_id"),
                            rs.getInt("capacity"),
                            rs.getString("room_type")
                    );

                    // Load scheduled courses using the same connection
                    loadScheduledCourses(connection, roomSchedule);

                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rooms by minimum capacity: " + minCapacity, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding rooms by minimum capacity: " + minCapacity, e);
        }

        return roomSchedules;
    }
}
