package src.dao;

import src.model.absence.AbsenceRecord;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for AbsenceRecord operations using the 'attendance' table.
 * Retrieves data by LEFT JOINING with 'students', 'class_sessions', and 'classes'.
 * Verified JOIN logic based on all provided schemas and foreign keys.
 * Handles cases where student_id or session_id might not exist in foreign tables.
 */
public class AbsenceRecordDAO {

    private static final Logger LOGGER = Logger.getLogger(AbsenceRecordDAO.class.getName());

    // Định dạng ngày tháng
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * Find all absence records from the attendance table, including those with missing student/class data.
     */
    public List<AbsenceRecord> findAll() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<AbsenceRecord> records = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            stmt = connection.createStatement();

            // Corrected SQL query using LEFT JOINs based on foreign keys: attendance -> students, attendance -> class_sessions -> classes
            String sql = "SELECT a.attendance_id, s.name, c.class_name, a.absence_date, " +
                    "a.status, a.notes, a.called, a.approved " +
                    "FROM attendance a " +
                    "LEFT JOIN students s ON a.student_id = s.id " +
                    "LEFT JOIN class_sessions cs ON a.session_id = cs.session_id " +
                    "LEFT JOIN classes c ON cs.class_id = c.class_id " + // Join class_sessions to classes on class_id
                    "ORDER BY a.absence_date DESC, s.name";

            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String recordId = rs.getString("attendance_id");
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                // Get data - these might be null if LEFT JOIN failed
                String studentName = rs.getString("name"); // From students table
                String className = rs.getString("class_name"); // From classes table via class_sessions
                String status = rs.getString("status"); // From attendance table
                String notes = rs.getString("notes"); // From attendance table
                boolean called = rs.getBoolean("called"); // From attendance table (getBoolean returns false for SQL NULL)
                boolean approved = rs.getBoolean("approved"); // From attendance table (getBoolean returns false for SQL NULL)

                // Create AbsenceRecord object. The constructor and model should handle null strings.
                AbsenceRecord record = new AbsenceRecord(
                        recordId,
                        studentName, // Might be null
                        className,   // Might be null
                        formattedDate,
                        status,      // Might be null
                        notes,       // Might be null
                        called,
                        approved
                );

                records.add(record);
            }

            return records;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all absence records with LEFT JOIN.", e);
            throw e;
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Statement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Find absence records from the attendance table by filters, including those with missing student/class data.
     */
    public List<AbsenceRecord> findByFilters(String keyword, LocalDate fromDate, LocalDate toDate) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<AbsenceRecord> records = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            // Corrected SQL query with filters using LEFT JOINs through class_sessions
            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT a.attendance_id, s.name, c.class_name, a.absence_date, " +
                            "a.status, a.notes, a.called, a.approved " +
                            "FROM attendance a " +
                            "LEFT JOIN students s ON a.student_id = s.id " +
                            "LEFT JOIN class_sessions cs ON a.session_id = cs.session_id " +
                            "LEFT JOIN classes c ON cs.class_id = c.class_id " + // Join class_sessions to classes on class_id
                            "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();

            if (keyword != null && !keyword.isEmpty()) {
                // Use COALESCE or check for NULL in the DB query if needed, but LIKE handles NULL by not matching.
                // Keeping current LIKE logic which is fine for NULLs (they won't match).
                sqlBuilder.append("AND (s.name LIKE ? OR c.class_name LIKE ? OR a.notes LIKE ?) ");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }

            if (fromDate != null) {
                sqlBuilder.append("AND a.absence_date >= ? ");
                params.add(fromDate.format(DB_DATE_FORMATTER));
            }

            if (toDate != null) {
                sqlBuilder.append("AND a.absence_date <= ? ");
                params.add(toDate.format(DB_DATE_FORMATTER));
            }

            sqlBuilder.append("ORDER BY a.absence_date DESC, s.name");

            stmt = connection.prepareStatement(sqlBuilder.toString());

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            rs = stmt.executeQuery();

            while (rs.next()) {
                String recordId = rs.getString("attendance_id");
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                // Get data - these might be null if LEFT JOIN failed
                String studentName = rs.getString("name"); // From students table
                String className = rs.getString("class_name"); // From classes table via class_sessions
                String status = rs.getString("status"); // From attendance table
                String notes = rs.getString("notes"); // From attendance table
                boolean called = rs.getBoolean("called"); // From attendance table (getBoolean returns false for SQL NULL)
                boolean approved = rs.getBoolean("approved"); // From attendance table (getBoolean returns false for SQL NULL)

                // Create AbsenceRecord object. The constructor and model should handle null strings.
                AbsenceRecord record = new AbsenceRecord(
                        recordId,
                        studentName, // Might be null
                        className,   // Might be null
                        formattedDate,
                        status,      // Might be null
                        notes,       // Might be null
                        called,
                        approved
                );

                records.add(record);
            }

            return records;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding absence records with filters using LEFT JOIN.", e);
            throw e;
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Update an absence record in the attendance table.
     * Assumes AbsenceRecord.getId() returns the String representation of the attendance_id.
     * This method operates directly on the attendance table and is not affected by the join logic.
     */
    public boolean update(AbsenceRecord record) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        if (record == null || record.getId() == null || record.getId().trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update a null or invalid AbsenceRecord.");
            return false;
        }

        try {
            connection = DatabaseConnection.getConnection();

            String sql = "UPDATE attendance SET called = ?, approved = ? WHERE attendance_id = ?";

            stmt = connection.prepareStatement(sql);
            stmt.setBoolean(1, record.isCalled());
            stmt.setBoolean(2, record.isApproved());
            stmt.setString(3, record.getId());

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating absence record (attendance table) with ID: " + record.getId(), e);
            throw e;
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Update multiple absence records in the attendance table in a batch.
     * Assumes AbsenceRecord.getId() returns the String representation of the attendance_id.
     * This method operates directly on the attendance table and is not affected by the join logic.
     */
    public boolean updateBatch(List<AbsenceRecord> records) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        if (records == null || records.isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update an empty list of absence records.");
            return true;
        }

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            String sql = "UPDATE attendance SET called = ?, approved = ? WHERE attendance_id = ?";
            stmt = connection.prepareStatement(sql);

            for (AbsenceRecord record : records) {
                if (record == null || record.getId() == null || record.getId().trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping null or invalid AbsenceRecord in batch update.");
                    continue;
                }

                stmt.setBoolean(1, record.isCalled());
                stmt.setBoolean(2, record.isApproved());
                stmt.setString(3, record.getId());
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            connection.commit();

            boolean allSuccessful = true;
            for (int result : results) {
                if (result < 0 && result != Statement.SUCCESS_NO_INFO) {
                    allSuccessful = false;
                    LOGGER.log(Level.WARNING, "Batch update resulted in non-zero or non-success_no_info status for one or more records.");
                }
            }

            return allSuccessful;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    LOGGER.log(Level.SEVERE, "Batch update failed, performing rollback.", e);
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error during rollback after batch update failure.", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Error executing batch update for absence records.", e);
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error resetting auto-commit to true.", e);
                }
            }
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Format date from database format (yyyy-MM-dd) to display format (dd/MM/yyyy)
     */
    private String formatDateFromDb(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) {
            return "";
        }

        try {
            LocalDate date = LocalDate.parse(dbDate, DB_DATE_FORMATTER);
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error formatting date from DB: " + dbDate, e);
            return dbDate; // Return original date string on error
        }
    }
}
