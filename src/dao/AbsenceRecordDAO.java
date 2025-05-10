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

/**
 * DAO for AbsenceRecord operations
 */
public class AbsenceRecordDAO {

    // Định dạng ngày tháng
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Lưu trữ ánh xạ giữa id hiển thị và id thực trong database
    private final Map<Integer, Integer> displayToDbIdMap = new HashMap<>();

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

            int counter = 1;
            displayToDbIdMap.clear(); // Xóa dữ liệu cũ

            while (rs.next()) {
                // Lưu ánh xạ giữa counter (id hiển thị) và id thực trong database
                int dbId = rs.getInt("id");
                displayToDbIdMap.put(counter, dbId);

                // Tạo ImageView từ đường dẫn hình ảnh
                String imagePath = rs.getString("image_path");
                ImageView studentImage = createStudentImageView(imagePath);

                // Format date from database (assuming database stores in yyyy-MM-dd format)
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                AbsenceRecord record = new AbsenceRecord(
                        counter++,
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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
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
                params.add(fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            if (toDate != null) {
                sqlBuilder.append("AND a.absence_date <= ? ");
                params.add(toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            sqlBuilder.append("ORDER BY a.absence_date DESC, s.student_name");

            stmt = connection.prepareStatement(sqlBuilder.toString());

            // Gán các tham số
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            rs = stmt.executeQuery();

            int counter = 1;
            displayToDbIdMap.clear(); // Xóa dữ liệu cũ

            while (rs.next()) {
                // Lưu ánh xạ giữa counter (id hiển thị) và id thực trong database
                int dbId = rs.getInt("id");
                displayToDbIdMap.put(counter, dbId);

                // Tạo ImageView từ đường dẫn hình ảnh
                String imagePath = rs.getString("image_path");
                ImageView studentImage = createStudentImageView(imagePath);

                // Format date from database
                String dbDate = rs.getString("absence_date");
                String formattedDate = formatDateFromDb(dbDate);

                AbsenceRecord record = new AbsenceRecord(
                        counter++,
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
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Update an absence record
     */
    public boolean update(AbsenceRecord record) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();

            // Update chỉ cập nhật các trường có thể thay đổi: called và approved
            String sql = "UPDATE absences SET called = ?, approved = ? WHERE id = ?";

            stmt = connection.prepareStatement(sql);
            stmt.setBoolean(1, record.isCalled());
            stmt.setBoolean(2, record.isApproved());

            // Lấy ID thực trong database từ ID hiển thị
            Integer dbId = displayToDbIdMap.get(record.getId());
            if (dbId == null) {
                throw new SQLException("Không tìm thấy ID database cho bản ghi có ID hiển thị: " + record.getId());
            }

            stmt.setInt(3, dbId);

            int result = stmt.executeUpdate();
            return result > 0;
        } finally {
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
        }
    }

    /**
     * Update multiple absence records
     */
    public boolean updateBatch(List<AbsenceRecord> records) throws SQLException {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            String sql = "UPDATE absences SET called = ?, approved = ? WHERE id = ?";
            stmt = connection.prepareStatement(sql);

            for (AbsenceRecord record : records) {
                stmt.setBoolean(1, record.isCalled());
                stmt.setBoolean(2, record.isApproved());

                // Lấy ID thực trong database từ ID hiển thị
                Integer dbId = displayToDbIdMap.get(record.getId());
                if (dbId == null) {
                    throw new SQLException("Không tìm thấy ID database cho bản ghi có ID hiển thị: " + record.getId());
                }

                stmt.setInt(3, dbId);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            connection.commit();

            // Kiểm tra kết quả
            for (int result : results) {
                if (result <= 0) {
                    return false;
                }
            }

            return true;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) stmt.close();
            if (connection != null) connection.close();
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
                Image image = new Image("file:" + imagePath, true);
                imageView.setImage(image);
            } catch (Exception e) {
                System.err.println("Không thể tải hình ảnh: " + imagePath);
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
            LocalDate date = LocalDate.parse(dbDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            System.err.println("Lỗi khi chuyển đổi định dạng ngày: " + e.getMessage());
            return dbDate; // Trả về nguyên dạng nếu có lỗi
        }
    }
}
