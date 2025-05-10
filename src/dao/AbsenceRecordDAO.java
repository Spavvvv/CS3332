package src.dao;

import src.model.absence.AbsenceRecord;
import utils.DatabaseConnection;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for AbsenceRecord operations.
 * Modified to use String IDs for Absence Records, corresponding to the database primary key.
 */
public class AbsenceRecordDAO {

    private static final Logger LOGGER = Logger.getLogger(AbsenceRecordDAO.class.getName());

    // Định dạng ngày tháng
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    /**
     * Find all absence records
     */
    public List<AbsenceRecord> findAll() throws SQLException {
        Connection connection = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<AbsenceRecord> records = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();
            stmt = connection.createStatement();

            String sql = "SELECT a.id, s.student_name, c.class_name, a.absence_date, " +
                    "a.status, a.note, a.called, a.approved, s.image_path " +
                    "FROM absences a " +
                    "JOIN students s ON a.student_id = s.id " +
                    "JOIN classes c ON s.class_id = c.id " +
                    "ORDER BY a.absence_date DESC, s.student_name";

            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                // Lấy ID thực trong database và chuyển đổi sang String
                int dbId = rs.getInt("id");
                String recordId = String.valueOf(dbId); // Use String ID from DB

                // Tạo ImageView từ đường dẫn hình ảnh
                String imagePath = rs.getString("image_path");
                ImageView studentImage = createStudentImageView(imagePath);

                // Format date from database (assuming database stores in yyyy-MM-dd format)
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                AbsenceRecord record = new AbsenceRecord(
                        recordId, // Pass String ID
                        studentImage,
                        rs.getString("student_name"),
                        rs.getString("class_name"),
                        formattedDate,
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getBoolean("called"),
                        rs.getBoolean("approved")
                );

                records.add(record);
            }

            return records;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all absence records.", e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Statement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Find absence records by filters
     */
    public List<AbsenceRecord> findByFilters(String keyword, LocalDate fromDate, LocalDate toDate) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<AbsenceRecord> records = new ArrayList<>();

        try {
            connection = DatabaseConnection.getConnection();

            StringBuilder sqlBuilder = new StringBuilder(
                    "SELECT a.id, s.student_name, c.class_name, a.absence_date, " +
                            "a.status, a.note, a.called, a.approved, s.image_path " +
                            "FROM absences a " +
                            "JOIN students s ON a.student_id = s.id " +
                            "JOIN classes c ON s.class_id = c.id " +
                            "WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();

            // Thêm điều kiện tìm kiếm nếu có keyword
            if (keyword != null && !keyword.isEmpty()) {
                sqlBuilder.append("AND (s.student_name LIKE ? OR c.class_name LIKE ? OR a.note LIKE ?) ");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
                params.add("%" + keyword + "%");
            }

            // Thêm điều kiện lọc theo ngày
            if (fromDate != null) {
                sqlBuilder.append("AND a.absence_date >= ? ");
                params.add(fromDate.format(DB_DATE_FORMATTER)); // Use DB format for query
            }

            if (toDate != null) {
                sqlBuilder.append("AND a.absence_date <= ? ");
                params.add(toDate.format(DB_DATE_FORMATTER)); // Use DB format for query
            }

            sqlBuilder.append("ORDER BY a.absence_date DESC, s.student_name");

            stmt = connection.prepareStatement(sqlBuilder.toString());

            // Gán các tham số
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            rs = stmt.executeQuery();

            while (rs.next()) {
                // Lấy ID thực trong database và chuyển đổi sang String
                int dbId = rs.getInt("id");
                String recordId = String.valueOf(dbId); // Use String ID from DB

                // Tạo ImageView từ đường dẫn hình ảnh
                String imagePath = rs.getString("image_path");
                ImageView studentImage = createStudentImageView(imagePath);

                // Format date from database
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                AbsenceRecord record = new AbsenceRecord(
                        recordId, // Pass String ID
                        studentImage,
                        rs.getString("student_name"),
                        rs.getString("class_name"),
                        formattedDate,
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getBoolean("called"),
                        rs.getBoolean("approved")
                );

                records.add(record);
            }

            return records;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding absence records by filters.", e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (rs != null) { try { rs.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing ResultSet", e); }}
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Update an absence record.
     * Assumes AbsenceRecord.getId() returns the String representation of the database ID.
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

            // Update chỉ cập nhật các trường có thể thay đổi: called và approved
            String sql = "UPDATE absences SET called = ?, approved = ? WHERE id = ?";

            stmt = connection.prepareStatement(sql);
            stmt.setBoolean(1, record.isCalled());
            stmt.setBoolean(2, record.isApproved());

            // Lấy ID từ AbsenceRecord (đã được giả định là String DB ID) và chuyển lại sang int
            int dbId;
            try {
                dbId = Integer.parseInt(record.getId());
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "Invalid ID format for AbsenceRecord update: " + record.getId(), e);
                throw new SQLException("Invalid ID format for update: " + record.getId(), e);
            }

            stmt.setInt(3, dbId);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating absence record with ID: " + record.getId(), e);
            throw e; // Re-throw the exception after logging
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e); }}
            if (connection != null) { try { connection.close(); } catch (SQLException e) { LOGGER.log(Level.WARNING, "Error closing Connection", e); }}
        }
    }

    /**
     * Update multiple absence records in a batch.
     * Assumes AbsenceRecord.getId() returns the String representation of the database ID.
     */
    public boolean updateBatch(List<AbsenceRecord> records) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        if (records == null || records.isEmpty()) {
            LOGGER.log(Level.WARNING, "Attempted to update an empty list of absence records.");
            return true; // Consider successful if nothing to update
        }

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            String sql = "UPDATE absences SET called = ?, approved = ? WHERE id = ?";
            stmt = connection.prepareStatement(sql);

            for (AbsenceRecord record : records) {
                if (record == null || record.getId() == null || record.getId().trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "Skipping null or invalid AbsenceRecord in batch update.");
                    continue; // Skip this record and continue with the batch
                }

                stmt.setBoolean(1, record.isCalled());
                stmt.setBoolean(2, record.isApproved());

                // Lấy ID từ AbsenceRecord (đã được giả định là String DB ID) và chuyển lại sang int
                int dbId;
                try {
                    dbId = Integer.parseInt(record.getId());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, "Invalid ID format for AbsenceRecord in batch update: " + record.getId(), e);
                    // Decide how to handle parse errors in batch - throw, skip, log?
                    // Throwing stops the whole batch. Skipping allows others to succeed. Logging is essential.
                    throw new SQLException("Invalid ID format for batch update: " + record.getId(), e); // Throw to stop batch on bad ID
                }

                stmt.setInt(3, dbId);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            connection.commit();

            // Optional: Check results array for successful updates
            boolean allSuccessful = true;
            for (int result : results) {
                if (result < 0 && result != Statement.SUCCESS_NO_INFO) { // Check for errors or no rows affected
                    allSuccessful = false;
                    // Log specific failures if needed, though executeBatch exception often covers this
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
            throw e; // Re-throw the exception after logging and rollback
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
     * Create a student ImageView from image path
     */
    private ImageView createStudentImageView(String imagePath) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(30);
        imageView.setFitWidth(30);

        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                // Tải hình ảnh từ đường dẫn
                // Use ClassLoader to find the resource, which is generally safer
                // if the image is within the classpath, or keep "file:" for external paths.
                // Assuming external path based on "file:" prefix in original code.
                Image image = new Image("file:" + imagePath, true);
                imageView.setImage(image);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load image from path: " + imagePath, e);
                // Có thể đặt hình ảnh mặc định ở đây
            }
        }

        return imageView;
    }

    /**
     * Format date from database format (yyyy-MM-dd) to display format (dd/MM/yyyy)
     */
    private String formatDateFromDb(String dbDate) {
        if (dbDate == null || dbDate.isEmpty()) {
            return "";
        }

        try {
            // Chuyển đổi từ định dạng yyyy-MM-dd sang dd/MM/yyyy
            LocalDate date = LocalDate.parse(dbDate, DB_DATE_FORMATTER); // Use DB_DATE_FORMATTER
            return date.format(DATE_FORMATTER); // Format to display format
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error formatting date from DB: " + dbDate, e);
            return dbDate; // Trả về nguyên dạng nếu có lỗi
        }
    }
}
