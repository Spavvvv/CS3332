
package src.dao;

import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HolidayDAO {

    private static final Logger LOGGER = Logger.getLogger(HolidayDAO.class.getName());

    /**
     * Constructor.
     */
    public HolidayDAO() {
        // No dependencies to inject for this DAO based on current implementation
    }

    // Holiday-related methods
    public Holiday findHolidayById(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to find holiday with null ID.");
            return null;
        }
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHoliday(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding holiday by ID: " + id, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding holiday by ID: " + id, e);
        }
        return null;
    }

    public List<Holiday> findAllHolidays() {
        List<Holiday> holidays = new ArrayList<>();
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                holidays.add(mapResultSetToHoliday(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all holidays.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding all holidays.", e);
        }
        return holidays;
    }

    public List<Holiday> findHolidaysByYear(int year) {
        List<Holiday> holidays = new ArrayList<>();
        // For standard SQL, direct date comparison is better than STRFTIME if columns are DATE/TIMESTAMP
        // Assumes start_date and end_date are DATE or TIMESTAMP type
        // Holiday overlaps if:
        // (start_date is in year) OR (end_date is in year) OR (holiday spans the entire year)
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays " +
                "WHERE (start_date >= ? AND start_date <= ?) " + // Starts within the year
                "OR (end_date >= ? AND end_date <= ?) " +       // Ends within the year
                "OR (start_date < ? AND end_date > ?)";        // Spans the entire year

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(yearStart));
            stmt.setDate(2, Date.valueOf(yearEnd));
            stmt.setDate(3, Date.valueOf(yearStart));
            stmt.setDate(4, Date.valueOf(yearEnd));
            stmt.setDate(5, Date.valueOf(yearStart)); // For spanning condition: start_date < yearStart
            stmt.setDate(6, Date.valueOf(yearEnd));   // For spanning condition: end_date > yearEnd

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holidays.add(mapResultSetToHoliday(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding holidays by year: " + year, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding holidays by year: " + year, e);
        }
        return holidays;
    }

    /**
     * Checks if a given date falls within any holiday period.
     * Uses the provided database connection.
     *
     * @param conn The database connection to use for the query.
     * @param date The date to check.
     * @return true if the date is a holiday, false otherwise.
     */
    public boolean isHoliday(Connection conn, LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "isHoliday check called with null date.");
            return false;
        }
        if (conn == null) {
            LOGGER.log(Level.SEVERE, "isHoliday check called with null connection. Cannot perform query.");
            return false; // Or throw an IllegalArgumentException
        }

        // Query to see if the date falls between any holiday's start_date and end_date (inclusive)
        String sql = "SELECT 1 FROM holidays WHERE start_date <= ? AND end_date >= ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // If a row is found, it's a holiday
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if date " + date + " is a holiday.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error checking if date " + date + " is a holiday.", e);
        }
        return false; // Default to false if error or not found
    }

    /**
     * Checks if a given date falls within any holiday period.
     * This version creates its own database connection.
     *
     * @param date The date to check.
     * @return true if the date is a holiday, false otherwise.
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            LOGGER.log(Level.WARNING, "isHoliday check called with null date.");
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            return isHoliday(conn, date); // Delegate to the version that takes a connection
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obtaining connection for isHoliday check for date " + date, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error obtaining connection for isHoliday check for date " + date, e);
        }
        return false;
    }


    public Holiday saveHoliday(Holiday holiday) {
        if (holiday == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null holiday.");
            return null;
        }
        // Consider validating holiday.getStartDate() and holiday.getEndDate() are not null
        if (holiday.getStartDate() == null || holiday.getEndDate() == null) {
            LOGGER.log(Level.WARNING, "Attempted to save holiday with null start or end date.");
            return null;
        }
        if (holiday.getId() == null) {
            return insertHoliday(holiday);
        } else {
            return updateHoliday(holiday);
        }
    }

    private Holiday insertHoliday(Holiday holiday) {
        String sql = "INSERT INTO holidays (name, start_date, end_date, color_hex) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
                    // Log history after successful insert
                    logHolidayChange(conn, "SYSTEM", "Created holiday: " + holiday.getName() + " (ID: " + holiday.getId() + ")");
                } else {
                    throw new SQLException("Creating holiday failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting holiday: " + holiday.getName(), e);
            return null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error inserting holiday: " + holiday.getName(), e);
            return null; // Indicate failure
        }
        return holiday;
    }

    private Holiday updateHoliday(Holiday holiday) {
        String sql = "UPDATE holidays SET name = ?, start_date = ?, end_date = ?, color_hex = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, holiday.getName());
            stmt.setDate(2, Date.valueOf(holiday.getStartDate()));
            stmt.setDate(3, Date.valueOf(holiday.getEndDate()));
            stmt.setString(4, holiday.getColorHex());
            stmt.setLong(5, holiday.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                // Log history after successful update
                logHolidayChange(conn, "SYSTEM", "Updated holiday: " + holiday.getName() + " (ID: " + holiday.getId() + ")");
            } else {
                LOGGER.log(Level.INFO, "Update holiday with ID: " + holiday.getId() + " affected 0 rows. May not exist or no changes made.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating holiday with ID: " + holiday.getId(), e);
            return null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating holiday with ID: " + holiday.getId(), e);
            return null; // Indicate failure
        }
        return holiday;
    }

    public void deleteHoliday(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete holiday with null ID.");
            return;
        }
        String holidayNameForLog = "";
        // Optionally, fetch the holiday name before deleting for logging purposes
        Holiday holidayToDelete = findHolidayById(id); // Uses its own connection
        if (holidayToDelete != null) {
            holidayNameForLog = holidayToDelete.getName();
        }


        String sql = "DELETE FROM holidays WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0 && holidayToDelete != null) {
                logHolidayChange(conn, "SYSTEM", "Deleted holiday: " + holidayNameForLog + " (ID: " + id + ")");
            } else if (affectedRows == 0) {
                LOGGER.log(Level.INFO, "Delete holiday with ID: " + id + " affected 0 rows. Holiday might not exist.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting holiday with ID: " + id, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting holiday with ID: " + id, e);
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

    // History-related methods
    public HolidayHistory findHistoryById(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to find history with null ID.");
            return null;
        }
        String sql = "SELECT id, user_name, action, timestamp FROM holiday_history WHERE id = ?"; // Changed 'user' to 'user_name' for clarity
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding history by ID: " + id, e);
        }
        return null;
    }

    public List<HolidayHistory> findAllHistory() {
        List<HolidayHistory> histories = new ArrayList<>();
        String sql = "SELECT id, user_name, action, timestamp FROM holiday_history ORDER BY timestamp DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                histories.add(mapResultSetToHistory(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all history.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding all history.", e);
        }
        return histories;
    }

    public List<HolidayHistory> findRecentHistory(int limit) {
        if (limit <= 0) {
            LOGGER.log(Level.WARNING, "Attempted to find recent history with non-positive limit: " + limit);
            return new ArrayList<>();
        }
        List<HolidayHistory> histories = new ArrayList<>();
        String sql = "SELECT id, user_name, action, timestamp FROM holiday_history ORDER BY timestamp DESC LIMIT ?";
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding recent history with limit: " + limit, e);
        }
        return histories;
    }

    // Centralized method to log holiday changes to history table using a provided connection
    private void logHolidayChange(Connection conn, String user, String action) {
        if (conn == null) {
            LOGGER.log(Level.SEVERE, "Cannot log holiday change, provided connection is null.");
            return;
        }
        HolidayHistory history = new HolidayHistory(null, user, action, LocalDateTime.now()); // ID will be auto-generated
        // Use the provided connection for inserting history to maintain transaction if needed
        String sql = "INSERT INTO holiday_history (user_name, action, timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, history.getUser());
            stmt.setString(2, history.getAction());
            stmt.setTimestamp(3, Timestamp.valueOf(history.getTimestamp()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error logging holiday change: " + action, e);
        }
    }

    // Public method to save history, creating its own connection
    // This might be used if saving history is an independent action
    public HolidayHistory saveHistory(HolidayHistory history) {
        if (history == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null history entry.");
            return null;
        }
        if (history.getId() == null) {
            return insertHistory(history); // insertHistory creates its own connection
        } else {
            return updateHistory(history); // updateHistory creates its own connection
        }
    }

    // insertHistory and updateHistory now manage their own connections if called directly
    // If called from saveHoliday/deleteHoliday/updateHoliday, those methods pass their connection to logHolidayChange
    private HolidayHistory insertHistory(HolidayHistory history) {
        String sql = "INSERT INTO holiday_history (user_name, action, timestamp) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); // Manages its own connection
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, history.getUser());
            stmt.setString(2, history.getAction());
            stmt.setTimestamp(3, Timestamp.valueOf(history.getTimestamp()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating history failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    history.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating history failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting history for user: " + history.getUser(), e);
            return null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error inserting history for user: " + history.getUser(), e);
            return null; // Indicate failure
        }
        return history;
    }

    private HolidayHistory updateHistory(HolidayHistory history) {
        String sql = "UPDATE holiday_history SET user_name = ?, action = ?, timestamp = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); // Manages its own connection
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, history.getUser());
            stmt.setString(2, history.getAction());
            stmt.setTimestamp(3, Timestamp.valueOf(history.getTimestamp()));
            stmt.setLong(4, history.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating history with ID: " + history.getId(), e);
            return null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating history with ID: " + history.getId(), e);
            return null; // Indicate failure
        }
        return history;
    }

    private HolidayHistory mapResultSetToHistory(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String user = rs.getString("user_name"); // Changed 'user' to 'user_name'
        String action = rs.getString("action");
        Timestamp timestampSql = rs.getTimestamp("timestamp");
        LocalDateTime timestamp = timestampSql != null ? timestampSql.toLocalDateTime() : null;

        return new HolidayHistory(id, user, action, timestamp);
    }
}

