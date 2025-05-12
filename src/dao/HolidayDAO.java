// File: dao/HolidayDAO.java
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
        // Handle cases where start_date or end_date is in the specified year, or the holiday spans the entire year
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays " +
                "WHERE (STRFTIME('%Y', start_date) = ? OR STRFTIME('%Y', end_date) = ?) " + // Use STRFTIME for year extraction (SQLite/MySQL compatible)
                "OR (start_date <= ? AND end_date >= ?)"; // Handle holidays spanning the year

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(year));
            stmt.setString(2, String.valueOf(year));
            stmt.setDate(3, Date.valueOf(LocalDate.of(year, 12, 31)));
            stmt.setDate(4, Date.valueOf(LocalDate.of(year, 1, 1)));
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


    public Holiday saveHoliday(Holiday holiday) {
        if (holiday == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null holiday.");
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
                } else {
                    throw new SQLException("Creating holiday failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting holiday: " + holiday.getName(), e);
            holiday = null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error inserting holiday: " + holiday.getName(), e);
            holiday = null; // Indicate failure
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

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating holiday with ID: " + holiday.getId(), e);
            holiday = null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating holiday with ID: " + holiday.getId(), e);
            holiday = null; // Indicate failure
        }
        return holiday;
    }

    public void deleteHoliday(Long id) {
        if (id == null) {
            LOGGER.log(Level.WARNING, "Attempted to delete holiday with null ID.");
            return;
        }
        String sql = "DELETE FROM holidays WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding history by ID: " + id, e);
        }
        return null;
    }

    public List<HolidayHistory> findAllHistory() {
        List<HolidayHistory> histories = new ArrayList<>();
        String sql = "SELECT id, user, action, timestamp FROM holiday_history ORDER BY timestamp DESC";
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error finding recent history with limit: " + limit, e);
        }
        return histories;
    }

    public HolidayHistory saveHistory(HolidayHistory history) {
        if (history == null) {
            LOGGER.log(Level.WARNING, "Attempted to save a null history entry.");
            return null;
        }
        if (history.getId() == null) {
            return insertHistory(history);
        } else {
            return updateHistory(history);
        }
    }

    private HolidayHistory insertHistory(HolidayHistory history) {
        String sql = "INSERT INTO holiday_history (user, action, timestamp) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
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
            history = null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error inserting history for user: " + history.getUser(), e);
            history = null; // Indicate failure
        }
        return history;
    }

    private HolidayHistory updateHistory(HolidayHistory history) {
        String sql = "UPDATE holiday_history SET user = ?, action = ?, timestamp = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, history.getUser());
            stmt.setString(2, history.getAction());
            stmt.setTimestamp(3, Timestamp.valueOf(history.getTimestamp()));
            stmt.setLong(4, history.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating history with ID: " + history.getId(), e);
            history = null; // Indicate failure
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating history with ID: " + history.getId(), e);
            history = null; // Indicate failure
        }
        return history;
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
