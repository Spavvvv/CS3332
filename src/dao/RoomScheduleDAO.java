package src.dao;

import src.model.system.schedule.RoomSchedule;
import src.model.system.course.Course;
import src.model.system.course.CourseDate;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for RoomSchedule operations
 */
public class RoomScheduleDAO {

    /**
     * Find a room schedule by ID
     */
    public RoomSchedule findById(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = DatabaseConnection.getConnection();

            // Join the schedules table with the room_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE s.id = ? AND s.schedule_type = 'ROOM'"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

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

            return null;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    // Helper method to load scheduled courses for a room
    private void loadScheduledCourses(Connection connection, RoomSchedule roomSchedule) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.prepareStatement(
                    "SELECT * FROM courses WHERE room_id = ?"
            );
            stmt.setString(1, roomSchedule.getRoomId());
            rs = stmt.executeQuery();

            List<Course> scheduledCourses = new ArrayList<>();

            while (rs.next()) {
                CourseDate date = new CourseDate(
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate()
                );

                Course course = new Course(
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getString("subject"),
                        date
                );
                course.setRoomId(rs.getString("room_id"));

                scheduledCourses.add(course);
            }

            roomSchedule.setScheduledCourses(scheduledCourses);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Find all room schedules
     */
    public List<RoomSchedule> findAll() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<RoomSchedule> roomSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            stmt = connection.createStatement();

            rs = stmt.executeQuery(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE s.schedule_type = 'ROOM'"
            );

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

                // Load scheduled courses
                loadScheduledCourses(connection, roomSchedule);

                roomSchedules.add(roomSchedule);
            }

            return roomSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Save a room schedule
     */
    public boolean save(RoomSchedule roomSchedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert into base schedule table
            stmt = connection.prepareStatement(
                    "INSERT INTO schedules (id, name, description, start_time, end_time, schedule_type) " +
                            "VALUES (?, ?, ?, ?, ?, 'ROOM')"
            );
            stmt.setString(1, roomSchedule.getId());
            stmt.setString(2, roomSchedule.getName());
            stmt.setString(3, roomSchedule.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(roomSchedule.getStartTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(roomSchedule.getEndTime()));

            int result = stmt.executeUpdate();
            stmt.close();

            if (result > 0) {
                // Insert into room_schedules table
                stmt = connection.prepareStatement(
                        "INSERT INTO room_schedules (schedule_id, room_id, capacity, room_type) " +
                                "VALUES (?, ?, ?, ?)"
                );
                stmt.setString(1, roomSchedule.getId());
                stmt.setString(2, roomSchedule.getRoomId());
                stmt.setInt(3, roomSchedule.getCapacity());
                stmt.setString(4, roomSchedule.getRoomType());

                result = stmt.executeUpdate();
            }

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Update a room schedule
     */
    public boolean update(RoomSchedule roomSchedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Update base schedule table
            stmt = connection.prepareStatement(
                    "UPDATE schedules SET name = ?, description = ?, start_time = ?, end_time = ? " +
                            "WHERE id = ?"
            );
            stmt.setString(1, roomSchedule.getName());
            stmt.setString(2, roomSchedule.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(roomSchedule.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(roomSchedule.getEndTime()));
            stmt.setString(5, roomSchedule.getId());

            int result = stmt.executeUpdate();
            stmt.close();

            if (result > 0) {
                // Update room_schedules table
                stmt = connection.prepareStatement(
                        "UPDATE room_schedules SET room_id = ?, capacity = ?, room_type = ? " +
                                "WHERE schedule_id = ?"
                );
                stmt.setString(1, roomSchedule.getRoomId());
                stmt.setInt(2, roomSchedule.getCapacity());
                stmt.setString(3, roomSchedule.getRoomType());
                stmt.setString(4, roomSchedule.getId());

                result = stmt.executeUpdate();
            }

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Delete a room schedule by ID
     */
    public boolean delete(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // Delete from room_schedules table first
            stmt = connection.prepareStatement("DELETE FROM room_schedules WHERE schedule_id = ?");
            stmt.setString(1, id);
            stmt.executeUpdate();
            stmt.close();

            // Then delete from base schedules table
            stmt = connection.prepareStatement("DELETE FROM schedules WHERE id = ?");
            stmt.setString(1, id);
            int result = stmt.executeUpdate();

            connection.commit();
            return result > 0;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Find room schedules by room ID
     */
    public List<RoomSchedule> findByRoomId(String roomId) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<RoomSchedule> roomSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE rs.room_id = ?"
            );
            stmt.setString(1, roomId);
            rs = stmt.executeQuery();

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

                // Load scheduled courses
                loadScheduledCourses(connection, roomSchedule);

                roomSchedules.add(roomSchedule);
            }

            return roomSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Find available room schedules for a specified time period
     */
    public List<RoomSchedule> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<RoomSchedule> availableRooms = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE s.schedule_type = 'ROOM' " +
                            "AND s.start_time <= ? AND s.end_time >= ?"
            );
            stmt.setTimestamp(1, Timestamp.valueOf(startTime));
            stmt.setTimestamp(2, Timestamp.valueOf(endTime));
            rs = stmt.executeQuery();

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

                // Load scheduled courses
                loadScheduledCourses(connection, roomSchedule);

                // Check if the room has any scheduled courses during the requested time
                boolean isAvailable = true;
                for (Course course : roomSchedule.getScheduledCourses()) {
                    // You would need to add start/end time to Course class
                    // This is a placeholder for the availability check logic
                    // Assuming Course has getStartTime() and getEndTime() methods
                    // if (course.getStartTime().isBefore(endTime) && course.getEndTime().isAfter(startTime)) {
                    //     isAvailable = false;
                    //     break;
                    // }
                }

                if (isAvailable) {
                    availableRooms.add(roomSchedule);
                }
            }

            return availableRooms;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Assign a course to a room
     */
    public boolean assignCourseToRoom(String courseId, String roomId) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "UPDATE courses SET room_id = ? WHERE course_id = ?"
            );
            stmt.setString(1, roomId);
            stmt.setString(2, courseId);

            int result = stmt.executeUpdate();
            return result > 0;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Find rooms by type
     */
    public List<RoomSchedule> findByRoomType(String roomType) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<RoomSchedule> roomSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE rs.room_type = ?"
            );
            stmt.setString(1, roomType);
            rs = stmt.executeQuery();

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

                // Load scheduled courses
                loadScheduledCourses(connection, roomSchedule);

                roomSchedules.add(roomSchedule);
            }

            return roomSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Find rooms by minimum capacity
     */
    public List<RoomSchedule> findByMinimumCapacity(int minCapacity) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<RoomSchedule> roomSchedules = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
                            "FROM schedules s " +
                            "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                            "WHERE rs.capacity >= ?"
            );
            stmt.setInt(1, minCapacity);
            rs = stmt.executeQuery();

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

                // Load scheduled courses
                loadScheduledCourses(connection, roomSchedule);

                roomSchedules.add(roomSchedule);
            }

            return roomSchedules;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }
}
