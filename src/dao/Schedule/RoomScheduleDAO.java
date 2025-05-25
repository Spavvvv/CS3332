package src.dao.Schedule;

import src.model.system.schedule.RoomSchedule;
import src.model.system.course.Course;
import src.utils.DatabaseConnection;
import src.dao.Person.CourseDAO; // THÊM IMPORT
import src.dao.Person.TeacherDAO; // CourseDAO sẽ cần TeacherDAO, đảm bảo nó được thiết lập
import src.dao.Person.StudentDAO; // CourseDAO sẽ cần StudentDAO, đảm bảo nó được thiết lập
import src.utils.DaoManager; // Hoặc cách bạn quản lý DAO instances

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoomScheduleDAO {

    private static final Logger LOGGER = Logger.getLogger(RoomScheduleDAO.class.getName());
    private CourseDAO courseDAO ;
    //private final DaoManager DaoManager;

    /**
     * Constructor with CourseDAO dependency.
     */
    public RoomScheduleDAO() {
        //this.courseDAO = DaoManager.getInstance().getCourseDAO();
    }

    public void setCourseDAO(CourseDAO courseDAO){
        this.courseDAO = courseDAO;
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
        // SỬA ĐỔI SQL: Lấy tất cả các cột cần thiết cho CourseDAO.extractCourseFromResultSet
        String query = "SELECT course_id, course_name, subject, start_date, end_date, " +
                "start_time, end_time, teacher_id, room_id, class_id, progress, total_sessions " +
                "FROM courses WHERE room_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, roomSchedule.getRoomId());
            try (ResultSet rs = stmt.executeQuery()) {
                List<Course> scheduledCourses = new ArrayList<>();
                while (rs.next()) {
                    // SỬA ĐỔI: Sử dụng courseDAO để tạo đối tượng Course đầy đủ
                    // Đảm bảo rằng courseDAO đã được khởi tạo và các dependency của nó (TeacherDAO) cũng đã được set
                    Course course = this.courseDAO.extractCourseFromResultSet(connection, rs);
                    if (course != null) {
                        scheduledCourses.add(course);
                    }
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

    // Các phương thức save, update, delete, findByRoomId, findAvailableRooms, assignCourseToRoom,
    // findByRoomType, findByMinimumCapacity không trực tiếp tạo đối tượng Course từ ResultSet,
    // nên chúng không cần thay đổi lớn liên quan đến cấu trúc Course.
    // Phương thức assignCourseToRoom chỉ cập nhật room_id trong bảng courses.

    // ... (Giữ nguyên các phương thức save, update, delete, findByRoomId, findAvailableRooms, assignCourseToRoom, findByRoomType, findByMinimumCapacity)
    // Đảm bảo rằng các phương thức này không có lỗi tiềm ẩn nào khác liên quan đến Course.
    // Ví dụ, nếu chúng có logic tạo đối tượng Course, logic đó cũng cần được xem xét.
    // Tuy nhiên, dựa trên code bạn cung cấp, chúng chủ yếu thao tác với bảng schedules và room_schedules.

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

        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

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
                            connection.commit();
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
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection or transaction error during saving room schedule.", e);
            // Rollback nếu connection đã được thiết lập và lỗi xảy ra trước khi rollback trong khối try-catch nội bộ
            if (connection != null) {
                try {
                    if (!connection.getAutoCommit()) { // Chỉ rollback nếu transaction đang được quản lý thủ công
                        connection.rollback();
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction on outer catch.", ex);
                }
            }
            return false;
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            LOGGER.log(Level.SEVERE, "Unexpected error during saving room schedule with ID: " + (roomSchedule != null ? roomSchedule.getId() : "UNKNOWN"), e);
            if (connection != null) {
                try {
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction on outer unexpected error catch.", ex);
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restoring auto-commit or closing connection.", e);
                }
            }
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
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement stmtSchedule = connection.prepareStatement(updateScheduleSql)) {
                stmtSchedule.setString(1, roomSchedule.getName());
                stmtSchedule.setString(2, roomSchedule.getDescription());
                stmtSchedule.setTimestamp(3, Timestamp.valueOf(roomSchedule.getStartTime()));
                stmtSchedule.setTimestamp(4, Timestamp.valueOf(roomSchedule.getEndTime()));
                stmtSchedule.setString(5, roomSchedule.getId());
                int resultSchedule = stmtSchedule.executeUpdate();

                // Dù resultSchedule có thể là 0 (nếu không có gì thay đổi trong schedules),
                // vẫn tiếp tục cập nhật room_schedules.
                // Việc kiểm tra cả hai > 0 là để đảm bảo cả hai đều thành công nếu có thay đổi.
                // Nếu chỉ một trong hai bảng cần cập nhật, logic này có thể cần điều chỉnh.
                try (PreparedStatement stmtRoomSchedule = connection.prepareStatement(updateRoomScheduleSql)) {
                    stmtRoomSchedule.setString(1, roomSchedule.getRoomId());
                    stmtRoomSchedule.setInt(2, roomSchedule.getCapacity());
                    stmtRoomSchedule.setString(3, roomSchedule.getRoomType());
                    stmtRoomSchedule.setString(4, roomSchedule.getId());
                    int resultRoomSchedule = stmtRoomSchedule.executeUpdate();

                    // Commit nếu ít nhất một trong hai câu lệnh update có tác động hoặc không có lỗi
                    // Hoặc, nếu bạn muốn chỉ commit khi cả hai đều có result > 0 (nghĩa là cả hai đều thực sự update rows)
                    // thì giữ nguyên: if (resultSchedule > 0 && resultRoomSchedule > 0)
                    // Để an toàn hơn, có thể chỉ cần không có lỗi là commit, vì update không thay đổi row cũng không phải là lỗi
                    connection.commit();
                    return true; // Giả sử thành công nếu không có exception

                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "SQL Error updating room_schedules for ID: " + roomSchedule.getId(), e);
                    connection.rollback();
                    return false;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Error updating schedules for ID: " + roomSchedule.getId(), e);
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection or transaction error during updating room schedule.", e);
            if (connection != null) { try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error rolling back.", ex); }}
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating room schedule.", e);
            if (connection != null) { try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error rolling back.", ex); }}
            return false;
        }
        finally {
            if (connection != null) { try { connection.setAutoCommit(true); connection.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error restoring/closing conn.", e); }}
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
        // Order of deletion might matter based on foreign key constraints
        // If schedules.id is FK in room_schedules, delete from room_schedules first.
        String deleteRoomScheduleSql = "DELETE FROM room_schedules WHERE schedule_id = ?";
        String deleteScheduleSql = "DELETE FROM schedules WHERE id = ? AND schedule_type = 'ROOM'"; // Ensure only ROOM type is deleted
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement stmtRoomSchedule = connection.prepareStatement(deleteRoomScheduleSql)) {
                stmtRoomSchedule.setString(1, id);
                stmtRoomSchedule.executeUpdate(); // Có thể không có dòng nào bị xóa nếu không tồn tại, không phải lỗi
            }
            // Tiếp tục xóa trong bảng schedules ngay cả khi không có gì trong room_schedules
            try (PreparedStatement stmtSchedule = connection.prepareStatement(deleteScheduleSql)) {
                stmtSchedule.setString(1, id);
                int resultSchedule = stmtSchedule.executeUpdate();
                if (resultSchedule > 0) { // Chỉ thành công nếu bảng chính có dòng bị xóa
                    connection.commit();
                    return true;
                } else {
                    // Nếu không có gì trong schedules (ví dụ ID sai hoặc type sai), rollback
                    // Hoặc nếu việc không tìm thấy bản ghi để xóa không được coi là lỗi, thì có thể commit.
                    // Hiện tại, coi như phải xóa được từ bảng 'schedules' mới là thành công.
                    LOGGER.log(Level.WARNING, "Deleting from schedules failed or ID not found: " + id + ", rolling back.");
                    connection.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error deleting room schedule ID: " + id, e);
            if (connection != null) { try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error rolling back.", ex); }}
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting room schedule.", e);
            if (connection != null) { try { if (!connection.getAutoCommit()) connection.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Error rolling back.", ex); }}
            return false;
        } finally {
            if (connection != null) { try { connection.setAutoCommit(true); connection.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error restoring/closing conn.", e); }}
        }
    }

    public List<RoomSchedule> findByRoomId(String roomId) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (roomId == null || roomId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find room schedule by null or empty room ID.");
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.room_id = ? AND s.schedule_type = 'ROOM'"; // Thêm s.schedule_type

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
                    loadScheduledCourses(connection, roomSchedule);
                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding room schedules by room ID: " + roomId, e);
        }
        return roomSchedules;
    }

    public List<RoomSchedule> findAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
        List<RoomSchedule> availableRooms = new ArrayList<>();
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            LOGGER.log(Level.WARNING, "Attempted to find available rooms with invalid time period.");
            return availableRooms;
        }
        // Câu lệnh này tìm các RoomSchedule có lịch hoạt động CHỒNG CHÉO với khoảng thời gian yêu cầu.
        // Nó không kiểm tra xem có Course nào đã chiếm lịch trong RoomSchedule đó hay không.
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE s.schedule_type = 'ROOM' " +
                "AND s.start_time < ? AND s.end_time > ?"; // Room schedule overlaps with [startTime, endTime]

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setTimestamp(1, Timestamp.valueOf(endTime)); // s.start_time < requestedEndTime
            stmt.setTimestamp(2, Timestamp.valueOf(startTime)); // s.end_time > requestedStartTime

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
                    loadScheduledCourses(connection, roomSchedule); // Tải các course đang dùng phòng này
                    availableRooms.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding available room schedules.", e);
        }
        return availableRooms;
    }

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
        }
        return false;
    }


    public List<RoomSchedule> findByRoomType(String roomType) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (roomType == null || roomType.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to find rooms by null or empty room type.");
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.room_type = ? AND s.schedule_type = 'ROOM'"; // Thêm s.schedule_type

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
                    loadScheduledCourses(connection, roomSchedule);
                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rooms by type: " + roomType, e);
        }
        return roomSchedules;
    }

    public List<RoomSchedule> findByMinimumCapacity(int minCapacity) {
        List<RoomSchedule> roomSchedules = new ArrayList<>();
        if (minCapacity < 0) {
            LOGGER.log(Level.WARNING, "Attempted to find rooms with negative minimum capacity: " + minCapacity);
            return roomSchedules;
        }
        String query = "SELECT s.id, s.name, s.description, s.start_time, s.end_time, rs.room_id, rs.capacity, rs.room_type " +
                "FROM schedules s " +
                "JOIN room_schedules rs ON s.id = rs.schedule_id " +
                "WHERE rs.capacity >= ? AND s.schedule_type = 'ROOM'"; // Thêm s.schedule_type

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
                    loadScheduledCourses(connection, roomSchedule);
                    roomSchedules.add(roomSchedule);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rooms by minimum capacity: " + minCapacity, e);
        }
        return roomSchedules;
    }
}