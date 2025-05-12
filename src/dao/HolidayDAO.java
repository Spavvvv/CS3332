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

public class HolidayDAO {
    // Holiday-related methods
    public Holiday findHolidayById(Long id) {
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return holidays;
    }

    public List<Holiday> findHolidaysByYear(int year) {
        List<Holiday> holidays = new ArrayList<>();
        String sql = "SELECT id, name, start_date, end_date, color_hex FROM holidays " +
                "WHERE YEAR(start_date) = ? OR YEAR(end_date) = ? " +
                "OR (start_date <= ? AND end_date >= ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, year);
            stmt.setDate(3, Date.valueOf(LocalDate.of(year, 12, 31)));
            stmt.setDate(4, Date.valueOf(LocalDate.of(year, 1, 1)));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    holidays.add(mapResultSetToHoliday(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return holidays;
    }

    public Holiday saveHoliday(Holiday holiday) {
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return holiday;
    }

    public void deleteHoliday(Long id) {
        String sql = "DELETE FROM holidays WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Holiday mapResultSetToHoliday(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        LocalDate startDate = rs.getDate("start_date").toLocalDate();
        LocalDate endDate = rs.getDate("end_date").toLocalDate();
        String colorHex = rs.getString("color_hex");

        return new Holiday(id, name, startDate, endDate, colorHex);
    }

    // History-related methods
    public HolidayHistory findHistoryById(Long id) {
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return histories;
    }

    public List<HolidayHistory> findRecentHistory(int limit) {
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
            e.printStackTrace();
        }
        return histories;
    }

    public HolidayHistory saveHistory(HolidayHistory history) {
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return history;
    }

    private HolidayHistory mapResultSetToHistory(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String user = rs.getString("user");
        String action = rs.getString("action");
        LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

        return new HolidayHistory(id, user, action, timestamp);
    }
}
