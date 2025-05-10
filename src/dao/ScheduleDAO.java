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

/**
 * DAO for Schedule operations, combining interface and implementation
 */
public class ScheduleDAO {

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
                    "SELECT * FROM schedules WHERE id = ?"
            );
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String scheduleType = rs.getString("schedule_type");

                // Depending on the type, query the specific subclass table
                if ("ROOM".equals(scheduleType)) {
                    schedule = findRoomScheduleById(connection, id);
                } else if ("STUDENT".equals(scheduleType)) {
                    schedule = findStudentScheduleById(connection, id);
                }
            }

            return schedule;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    // Helper method to find a RoomSchedule by ID
    private RoomSchedule findRoomScheduleById(Connection connection, String id) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Join the schedules table with the room_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.*, rs.room_id, rs.capacity, rs.room_type " +
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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    // Helper method to find a StudentSchedule by ID
    private StudentSchedule findStudentScheduleById(Connection connection, String id) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Join the schedules table with the student_schedules table
            stmt = connection.prepareStatement(
                    "SELECT s.*, ss.student_id " +
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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Save a schedule
     */
    public boolean save(Schedule schedule) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

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
            stmt.setTimestamp(4, Timestamp.valueOf(schedule.getStartTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(schedule.getEndTime()));

            // Determine schedule type and handle subclass-specific persistence
            if (schedule instanceof RoomSchedule) {
                stmt.setString(6, "ROOM");
                int result = stmt.executeUpdate();

                if (result > 0) {
                    saveRoomSchedule(connection, (RoomSchedule) schedule);
                }
            } else if (schedule instanceof StudentSchedule) {
                stmt.setString(6, "STUDENT");
                int result = stmt.executeUpdate();

                if (result > 0) {
                    saveStudentSchedule(connection, (StudentSchedule) schedule);
                }
            } else {
                // Unknown schedule type
                return false;
            }

            connection.commit();
            return true;
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
        } finally {
            if (stmt != null) stmt.close();
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
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Update a schedule
     */
    public boolean update(Schedule schedule) throws SQLException {
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
            stmt.setString(1, schedule.getName());
            stmt.setString(2, schedule.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(schedule.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(schedule.getEndTime()));
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
        } finally {
            if (stmt != null) stmt.close();
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
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Delete a schedule by ID
     */
    public boolean delete(String id) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // First determine the schedule type
            stmt = connection.prepareStatement("SELECT schedule_type FROM schedules WHERE id = ?");
            stmt.setString(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String scheduleType = rs.getString("schedule_type");
                rs.close();
                stmt.close();

                // Delete from specific subclass table first
                if ("ROOM".equals(scheduleType)) {
                    stmt = connection.prepareStatement("DELETE FROM room_schedules WHERE schedule_id = ?");
                    stmt.setString(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                } else if ("STUDENT".equals(scheduleType)) {
                    stmt = connection.prepareStatement("DELETE FROM student_schedules WHERE schedule_id = ?");
                    stmt.setString(1, id);
                    stmt.executeUpdate();
                    stmt.close();
                }

                // Then delete from base table
                stmt = connection.prepareStatement("DELETE FROM schedules WHERE id = ?");
                stmt.setString(1, id);
                int result = stmt.executeUpdate();

                connection.commit();
                return result > 0;
            }

            return false;
        } catch (SQLException e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ex) {
                throw new SQLException("Error during rollback", ex);
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
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

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules " +
                            "WHERE (start_time <= ? AND end_time >= ?) " +
                            "OR (start_time >= ? AND start_time <= ?) " +
                            "OR (end_time >= ? AND end_time <= ?)"
            );
            stmt.setTimestamp(1, Timestamp.valueOf(endTime));
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));
            stmt.setTimestamp(3, Timestamp.valueOf(startTime));
            stmt.setTimestamp(4, Timestamp.valueOf(endTime));
            stmt.setTimestamp(5, Timestamp.valueOf(startTime));
            stmt.setTimestamp(6, Timestamp.valueOf(endTime));

            rs = stmt.executeQuery();

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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
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

        try {
            connection = DatabaseConnection.getConnection();

            stmt = connection.prepareStatement(
                    "SELECT id, schedule_type FROM schedules " +
                            "WHERE name LIKE ?"
            );
            stmt.setString(1, "%" + name + "%");

            rs = stmt.executeQuery();

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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }
}
