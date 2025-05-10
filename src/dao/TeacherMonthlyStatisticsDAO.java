package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.monthly.MonthlyTeachingStatisticsModel.TeacherMonthlyStatistics;
import utils.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;

public class TeacherMonthlyStatisticsDAO {

    /**
     * Fetches monthly teaching statistics for all teachers within a date range
     */
    public ObservableList<TeacherMonthlyStatistics> getTeachingStatistics(Month fromMonth, int fromYear,
                                                                          Month toMonth, int toYear,
                                                                          String status) throws SQLException {
        ObservableList<TeacherMonthlyStatistics> statistics = FXCollections.observableArrayList();

        // Convert Month to numeric value for SQL query
        int fromMonthValue = fromMonth.getValue();
        int toMonthValue = toMonth.getValue();

        // Build the query with optional status filter
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT t.teacher_id, t.full_name, ");
        queryBuilder.append("COUNT(s.session_id) as session_count, ");
        queryBuilder.append("SUM(s.duration_hours) as total_hours ");
        queryBuilder.append("FROM teachers t ");
        queryBuilder.append("LEFT JOIN teaching_sessions s ON t.teacher_id = s.teacher_id ");
        queryBuilder.append("WHERE (s.session_date IS NULL OR (");
        queryBuilder.append("(YEAR(s.session_date) > ? OR (YEAR(s.session_date) = ? AND MONTH(s.session_date) >= ?)) AND ");
        queryBuilder.append("(YEAR(s.session_date) < ? OR (YEAR(s.session_date) = ? AND MONTH(s.session_date) <= ?))");
        queryBuilder.append(")) ");

        // Add status filter if not "Tất cả"
        if (status != null && !status.equals("Tất cả")) {
            queryBuilder.append("AND s.status = ? ");
        }

        queryBuilder.append("GROUP BY t.teacher_id, t.full_name ");
        queryBuilder.append("ORDER BY t.full_name");

        String query = queryBuilder.toString();

        try {
            ResultSet rs;
            if (status != null && !status.equals("Tất cả")) {
                rs = DatabaseConnection.executeQuery(query,
                        fromYear, fromYear, fromMonthValue,
                        toYear, toYear, toMonthValue,
                        status);
            } else {
                rs = DatabaseConnection.executeQuery(query,
                        fromYear, fromYear, fromMonthValue,
                        toYear, toYear, toMonthValue);
            }

            int stt = 1;
            while (rs.next()) {
                String teacherName = rs.getString("full_name");
                int sessions = rs.getInt("session_count");
                double hours = rs.getDouble("total_hours");

                TeacherMonthlyStatistics teacherStats = new TeacherMonthlyStatistics(
                        stt++, teacherName, sessions, hours
                );
                statistics.add(teacherStats);
            }

            // If no records found, try to get at least teacher list
            if (statistics.isEmpty()) {
                query = "SELECT teacher_id, full_name FROM teachers ORDER BY full_name";
                rs = DatabaseConnection.executeQuery(query);

                stt = 1;
                while (rs.next()) {
                    String teacherName = rs.getString("full_name");
                    TeacherMonthlyStatistics teacherStats = new TeacherMonthlyStatistics(
                            stt++, teacherName, 0, 0.0
                    );
                    statistics.add(teacherStats);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching teaching statistics: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return statistics;
    }

    /**
     * Returns a fallback dataset in case of database connectivity issues
     */
    public ObservableList<TeacherMonthlyStatistics> getFallbackData() {
        ObservableList<TeacherMonthlyStatistics> fallbackData = FXCollections.observableArrayList(
                new TeacherMonthlyStatistics(1, "Trịnh Đình Đức", 23, 21.0),
                new TeacherMonthlyStatistics(2, "Hoàng Ngọc Hà", 7, 10.5),
                new TeacherMonthlyStatistics(3, "Đinh Thị Ngọc Linh", 19, 19.5),
                new TeacherMonthlyStatistics(4, "Bùi Tuyết Mai", 18, 27.0),
                new TeacherMonthlyStatistics(5, "Nguyễn Tiến Dũng", 26, 27.0),
                new TeacherMonthlyStatistics(6, "Trần Trung Hải", 68, 63.0),
                new TeacherMonthlyStatistics(7, "Lê Quang Huy", 32, 15.0),
                new TeacherMonthlyStatistics(8, "Vũ Nhật Quang", 14, 10.5),
                new TeacherMonthlyStatistics(9, "Lê Văn Bảo", 8, 6.0),
                new TeacherMonthlyStatistics(10, "Đỗ Tiến Dũng", 7, 10.5),
                new TeacherMonthlyStatistics(11, "Nguyễn Khánh Linh", 8, 12.0),
                new TeacherMonthlyStatistics(12, "Nguyễn Thị Kim", 8, 0.0),
                new TeacherMonthlyStatistics(13, "Trần Thu Hiền", 8, 0.0),
                new TeacherMonthlyStatistics(14, "Nguyễn Lê Thanh Thủy", 3, 0.0),
                new TeacherMonthlyStatistics(15, "Hà Thị Ngọc", 18, 13.5),
                new TeacherMonthlyStatistics(16, "Phạm Quỳnh Trang", 16, 24.0),
                new TeacherMonthlyStatistics(17, "Trần Thu Hằng", 12, 18.0),
                new TeacherMonthlyStatistics(18, "Nguyễn Minh Anh", 9, 0.0),
                new TeacherMonthlyStatistics(19, "Kiều Thu Thảo", 7, 10.5)
        );
        return fallbackData;
    }
}