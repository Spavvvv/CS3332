package src.dao.Holidays;

import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayDAO {

    private static final Logger LOGGER = Logger.getLogger(HolidayDAO.class.getName());
    private LocalDate lastExpiredCheck = null;

    // Cache for holidays to reduce database queries
    private final Map<LocalDate, Boolean> holidayStatusCache = new ConcurrentHashMap<>();
    private final Map<LocalDate, Holiday> holidayCache = new ConcurrentHashMap<>();
    private final Map<Long, Holiday> holidayByIdCache = new ConcurrentHashMap<>();
    private List<Holiday> allHolidaysCache = null;
    private final Map<Integer, List<Holiday>> holidaysByYearCache = new ConcurrentHashMap<>();

    // Cache invalidation timestamp
    private LocalDateTime cacheLastInvalidated = LocalDateTime.now();
    private static final long CACHE_TTL_MINUTES = 30; // Time-to-live for cache

    /**
     * Constructor.
     */
    public HolidayDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    /**
     * Invalidates all caches if they're older than TTL
     */
    private void invalidateCacheIfNeeded() {
        if (LocalDateTime.now().isAfter(cacheLastInvalidated.plusMinutes(CACHE_TTL_MINUTES))) {
            clearAllCaches();
        }
    }

    /**
     * Clears all caches when data is modified or TTL is reached
     */
    private void clearAllCaches() {
        holidayStatusCache.clear();
        holidayCache.clear();
        holidayByIdCache.clear();
        allHolidaysCache = null;
        holidaysByYearCache.clear();
        cacheLastInvalidated = LocalDateTime.now();
    }

    /**
     * Kiểm tra và xóa các sự kiện đã quá hạn, lưu lại thông tin vào bảng history.
     * Phương thức này được gọi mỗi khi ứng dụng tải dữ liệu sự kiện.
     */
    public void cleanupExpiredHolidays() {
        // Only check once per day
        LocalDate today = LocalDate.now();

        if (lastExpiredCheck != null && lastExpiredCheck.equals(today)) {
            return; // Skip if we already checked today
        }

        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Bulk delete approach for better performance
            String bulkDeleteSql = "DELETE FROM holidays WHERE end_date < ?";
            String findExpiredSql = "SELECT id, name, start_date, end_date FROM holidays WHERE end_date < ?";

            List<Holiday> expiredHolidays = new ArrayList<>();

            // First get list of expired holidays for logging
            try (PreparedStatement findStmt = conn.prepareStatement(findExpiredSql)) {
                findStmt.setDate(1, Date.valueOf(today));

                try (ResultSet rs = findStmt.executeQuery()) {
                    while (rs.next()) {
                        Long id = rs.getLong("id");
                        String name = rs.getString("name");
                        LocalDate startDate = rs.getDate("start_date").toLocalDate();
                        LocalDate endDate = rs.getDate("end_date").toLocalDate();

                        Holiday holiday = new Holiday();
                        holiday.setId(id);
                        holiday.setName(name);
                        holiday.setStartDate(startDate);
                        holiday.setEndDate(endDate);

                        expiredHolidays.add(holiday);
                    }
                }
            }

            if (expiredHolidays.isEmpty()) {
                LOGGER.log(Level.FINE, "No expired holidays found");
                conn.commit();
                lastExpiredCheck = today;
                return;
            }

            // Then perform bulk delete
            try (PreparedStatement deleteStmt = conn.prepareStatement(bulkDeleteSql)) {
                deleteStmt.setDate(1, Date.valueOf(today));
                int deletedCount = deleteStmt.executeUpdate();

                // Log each deleted holiday
                for (Holiday holiday : expiredHolidays) {
                    logHolidayChange(conn, "SYSTEM",
                            "Deleted expired holiday: " + holiday.getName() +
                                    " (ID: " + holiday.getId() + ", " +
                                    holiday.getStartDate() + " - " + holiday.getEndDate() + ")");
                }

                conn.commit();
                LOGGER.log(Level.INFO, "Successfully removed " + deletedCount + " expired holidays");
            }

            // Update the last check date
            lastExpiredCheck = today;

            // Clear caches after modifying data
            clearAllCaches();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error removing expired holidays", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    /**
     * Kiểm tra xem một ngày cụ thể có phải là ngày lễ không.
     *
     * @param date Ngày cần kiểm tra
     * @return true nếu là ngày lễ, false nếu không phải
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to check holiday status with null date.");
            return false;
        }

        invalidateCacheIfNeeded();

        // Check cache first
        if (holidayStatusCache.containsKey(date)) {
            return holidayStatusCache.get(date);
        }

        String sql = "SELECT 1 FROM holidays WHERE ? BETWEEN start_date AND end_date LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));

            boolean result = false;
            try (ResultSet rs = stmt.executeQuery()) {
                result = rs.next();
            }

            // Cache the result
            holidayStatusCache.put(date, result);
            return result;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if date is a holiday: " + date, e);
        }

        return false;
    }

    /**
     * Get the holiday information for a specific date if it exists
     *
     * @param date The date to check
     * @return Holiday object if the date is a holiday, or null if not
     */
    public Holiday getHolidayByDate(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "Attempted to get holiday with null date.");
            return null;
        }

        invalidateCacheIfNeeded();

        // Check cache first
        if (holidayCache.containsKey(date)) {
            return holidayCache.get(date);
        }

        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays " +
                "WHERE ? BETWEEN start_date AND end_date LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Holiday holiday = mapResultSetToHoliday(rs);
                    // Cache the result
                    holidayCache.put(date, holiday);
                    return holiday;
                } else {
                    // Cache null result too
                    holidayCache.put(date, null);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting holiday information for date: " + date, e);
        }

        return null;
    }

    // Holiday-related methods
    public Holiday findHolidayById(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to find holiday with null ID.");
            return null;
        }

        invalidateCacheIfNeeded();

        // Check cache first
        if (holidayByIdCache.containsKey(id)) {
            return holidayByIdCache.get(id);
        }

        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Holiday holiday = mapResultSetToHoliday(rs);
                    // Cache the result
                    holidayByIdCache.put(id, holiday);
                    return holiday;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding holiday by ID: " + id, e);
        }
        return null;
    }

    /**
     * Finds all holidays, first cleaning up any expired holidays.
     */
    public List<Holiday> findAllHolidays() {
        // Optional cleanup - could be controlled by a parameter
        if (lastExpiredCheck == null || !lastExpiredCheck.equals(LocalDate.now())) {
            cleanupExpiredHolidays();
        }

        invalidateCacheIfNeeded();

        // Check cache first
        if (allHolidaysCache != null) {
            return new ArrayList<>(allHolidaysCache);
        }

        List<Holiday> holidays = new ArrayList<>();
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                holidays.add(mapResultSetToHoliday(rs));
            }

            // Cache the result
            allHolidaysCache = new ArrayList<>(holidays);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all holidays.", e);
        }
        return holidays;
    }

    /**
     * Finds holidays by year, first cleaning up any expired holidays if needed.
     */
    public List<Holiday> findHolidaysByYear(int year) {
        // Optional cleanup - could be controlled by a parameter
        if (lastExpiredCheck == null || !lastExpiredCheck.equals(LocalDate.now())) {
            cleanupExpiredHolidays();
        }

        invalidateCacheIfNeeded();

        // Check cache first
        if (holidaysByYearCache.containsKey(year)) {
            return new ArrayList<>(holidaysByYearCache.get(year));
        }

        List<Holiday> holidays = new ArrayList<>();

        // Optimized query with indexed date comparison
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays " +
                "WHERE (YEAR(start_date) = ? OR YEAR(end_date) = ?) " +
                "OR (start_date <= ? AND end_date >= ?)";

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, year);
            stmt.setDate(3, Date.valueOf(yearEnd));
            stmt.setDate(4, Date.valueOf(yearStart));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holidays.add(mapResultSetToHoliday(rs));
                }
            }

            // Cache the result
            holidaysByYearCache.put(year, new ArrayList<>(holidays));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding holidays by year: " + year, e);
        }
        return holidays;
    }

    /**
     * Insert a new holiday
     */
    public Holiday saveHoliday(Holiday holiday) {
        if (holiday == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null holiday.");
            return null;
        }

        Holiday result = holiday.getId() == null ? insertHoliday(holiday) : updateHoliday(holiday);

        // Clear caches after modifying data
        if (result != null) {
            clearAllCaches();
        }

        return result;
    }

    private Holiday insertHoliday(Holiday holiday) {
        String sql = "INSERT INTO holidays (name, start_date, end_date, color_hex) VALUES (?, ?, ?, ?)";
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, holiday.getName());
                stmt.setDate(2, Date.valueOf(holiday.getStartDate()));
                stmt.setDate(3, Date.valueOf(holiday.getEndDate()));
                stmt.setString(4, holiday.getColorHex());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating holiday failed, no rows affected.");
                }

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        holiday.setId(generatedKeys.getLong(1));

                        // Log the action
                        logHolidayChange(conn, "SYSTEM",
                                "Created new holiday: " + holiday.getName() +
                                        " (ID: " + holiday.getId() + ", " +
                                        holiday.getStartDate() + " - " + holiday.getEndDate() + ")");

                        conn.commit();
                    } else {
                        conn.rollback();
                        throw new SQLException("Creating holiday failed, no ID obtained.");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting holiday: " + holiday.getName(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
        return holiday;
    }

    private Holiday updateHoliday(Holiday holiday) {
        String sql = "UPDATE holidays SET name = ?, start_date = ?, end_date = ?, color_hex = ? WHERE id = ?";
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, holiday.getName());
                stmt.setDate(2, Date.valueOf(holiday.getStartDate()));
                stmt.setDate(3, Date.valueOf(holiday.getEndDate()));
                stmt.setString(4, holiday.getColorHex());
                stmt.setLong(5, holiday.getId());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    // Log the action
                    logHolidayChange(conn, "SYSTEM",
                            "Updated holiday: " + holiday.getName() +
                                    " (ID: " + holiday.getId() + ", " +
                                    holiday.getStartDate() + " - " + holiday.getEndDate() + ")");

                    conn.commit();
                } else {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Holiday with ID: " + holiday.getId() + " not found for update.");
                    return null;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating holiday with ID: " + holiday.getId(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
        return holiday;
    }

    /**
     * Delete a holiday by ID
     */
    public boolean deleteHoliday(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete holiday with null ID.");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // First get the holiday for logging
            Holiday holidayToDelete = null;
            String selectSql = "SELECT name FROM holidays WHERE id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setLong(1, id);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        holidayToDelete = new Holiday();
                        holidayToDelete.setId(id);
                        holidayToDelete.setName(name);
                    }
                }
            }

            // If holiday exists, delete it
            if (holidayToDelete != null) {
                String deleteSql = "DELETE FROM holidays WHERE id = ?";
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setLong(1, id);
                    int affectedRows = deleteStmt.executeUpdate();

                    if (affectedRows > 0) {
                        // Log the action
                        logHolidayChange(conn, "SYSTEM",
                                "Deleted holiday: " + holidayToDelete.getName() +
                                        " (ID: " + id + ")");

                        conn.commit();

                        // Clear caches after modifying data
                        clearAllCaches();

                        return true;
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            } else {
                conn.rollback();
                LOGGER.log(Level.WARNING, "Holiday with ID: " + id + " not found for deletion.");
                return false;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting holiday with ID: " + id, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error rolling back transaction", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error closing connection", e);
                }
            }
        }
    }

    private Holiday mapResultSetToHoliday(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        Date startDateSql = rs.getDate("start_date");
        Date endDateSql = rs.getDate("end_date");
        LocalDate startDate = startDateSql != null ? startDateSql.toLocalDate() : null;
        LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;
        String colorHex = rs.getString("color_hex");

        return new Holiday(id, name, startDate, endDate, colorHex);
    }

    // History-related methods with prepared statement batching
    private static final int BATCH_SIZE = 100;

    public HolidayHistory findHistoryById(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to find history with null ID.");
            return null;
        }
        String sql = "SELECT id, user, action, timestamp FROM holiday_history WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistory(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding history by ID: " + id, e);
        }
        return null;
    }

    public List<HolidayHistory> findAllHistory() {
        List<HolidayHistory> histories = new ArrayList<>();
        String sql = "SELECT id, user, action, timestamp FROM holiday_history ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                     ResultSet.TYPE_FORWARD_ONLY,
                     ResultSet.CONCUR_READ_ONLY)) {
            // Set fetch size for better performance with large result sets
            stmt.setFetchSize(BATCH_SIZE);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    histories.add(mapResultSetToHistory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all history.", e);
        }
        return histories;
    }

    public List<HolidayHistory> findRecentHistory(int limit) {
        if (limit <= 0) {
            LOGGER.log(Level.WARNING, "Attempted to find recent history with non-positive limit: " + limit);
            return new ArrayList<>();
        }
        List<HolidayHistory> histories = new ArrayList<>();
        String sql = "SELECT id, user, action, timestamp FROM holiday_history ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    histories.add(mapResultSetToHistory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding recent history with limit: " + limit, e);
        }
        return histories;
    }

    // Centralized method to log holiday changes to history table using a provided connection
    private void logHolidayChange(Connection conn, String user, String action) {
        if (conn == null) {
            LOGGER.log(Level.SEVERE, "Cannot log holiday change, provided connection is null.");
            return;
        }
        String sql = "INSERT INTO holiday_history (user, action, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user);
            stmt.setString(2, action);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error logging holiday change: " + action, e);
        }
    }

    private HolidayHistory mapResultSetToHistory(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String user = rs.getString("user");
        String action = rs.getString("action");
        Timestamp timestampSql = rs.getTimestamp("timestamp");
        LocalDateTime timestamp = timestampSql != null ? timestampSql.toLocalDateTime() : null;

        return new HolidayHistory(id, user, action, timestamp);
    }
}