package src.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.yearly.TeacherYearlyStatisticsModel;
import src.model.teaching.yearly.StatisticsSummaryModel;
import utils.DatabaseConnection;

public class TeacherStatisticsDAO {
    private Connection connection;

    public TeacherStatisticsDAO() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get yearly teaching statistics by year range and approval status
     *
     * @param fromYear Start year for statistics
     * @param toYear End year for statistics
     * @param status Approval status filter ("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối")
     * @return Observable list of teacher statistics models
     */
    public ObservableList<TeacherYearlyStatisticsModel> getYearlyStatistics(int fromYear, int toYear, String status) {
        ObservableList<TeacherYearlyStatisticsModel> statistics = FXCollections.observableArrayList();

        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT t.teacher_id, t.teacher_name, ");
            query.append("COUNT(CASE WHEN s.year = ? THEN s.session_id END) AS year_sessions, ");
            query.append("SUM(CASE WHEN s.year = ? THEN s.hours END) AS year_hours, ");
            query.append("COUNT(s.session_id) AS total_sessions, ");
            query.append("SUM(s.hours) AS total_hours ");
            query.append("FROM teachers t ");
            query.append("LEFT JOIN teaching_sessions s ON t.teacher_id = s.teacher_id ");
            query.append("WHERE s.year BETWEEN ? AND ? ");

            // Add status filter if not "Tất cả"
            if (!status.equals("Tất cả")) {
                query.append("AND s.status = ? ");
            }

            query.append("GROUP BY t.teacher_id, t.teacher_name ");
            query.append("ORDER BY t.teacher_name");

            PreparedStatement statement = connection.prepareStatement(query.toString());
            statement.setInt(1, toYear); // Current year for specific stats
            statement.setInt(2, toYear); // Current year for specific stats
            statement.setInt(3, fromYear);
            statement.setInt(4, toYear);

            if (!status.equals("Tất cả")) {
                statement.setString(5, status);
            }

            ResultSet resultSet = statement.executeQuery();

            int stt = 1;
            while (resultSet.next()) {
                String teacherName = resultSet.getString("teacher_name");
                int yearSessions = resultSet.getInt("year_sessions");
                double yearHours = resultSet.getDouble("year_hours");
                int totalSessions = resultSet.getInt("total_sessions");
                double totalHours = resultSet.getDouble("total_hours");

                TeacherYearlyStatisticsModel model = new TeacherYearlyStatisticsModel(
                        stt++, teacherName, yearSessions, yearHours, totalSessions, totalHours
                );

                statistics.add(model);
            }

            resultSet.close();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return statistics;
    }

    /**
     * Calculate summary statistics for all teachers within a date range
     *
     * @param fromYear Start year for statistics
     * @param toYear End year for statistics
     * @param status Approval status filter ("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối")
     * @return Statistics summary model with totals
     */
    public StatisticsSummaryModel calculateSummaryStatistics(int fromYear, int toYear, String status) {
        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT ");
            query.append("COUNT(CASE WHEN year = ? THEN session_id END) AS year_sessions, ");
            query.append("SUM(CASE WHEN year = ? THEN hours END) AS year_hours, ");
            query.append("COUNT(session_id) AS total_sessions, ");
            query.append("SUM(hours) AS total_hours ");
            query.append("FROM teaching_sessions ");
            query.append("WHERE year BETWEEN ? AND ? ");

            // Add status filter if not "Tất cả"
            if (!status.equals("Tất cả")) {
                query.append("AND status = ? ");
            }

            PreparedStatement statement = connection.prepareStatement(query.toString());
            statement.setInt(1, toYear);
            statement.setInt(2, toYear);
            statement.setInt(3, fromYear);
            statement.setInt(4, toYear);

            if (!status.equals("Tất cả")) {
                statement.setString(5, status);
            }

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int yearSessions = resultSet.getInt("year_sessions");
                double yearHours = resultSet.getDouble("year_hours");
                int totalSessions = resultSet.getInt("total_sessions");
                double totalHours = resultSet.getDouble("total_hours");

                return new StatisticsSummaryModel(yearSessions, yearHours, totalSessions, totalHours);
            }

            resultSet.close();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Return empty summary if query fails
        return new StatisticsSummaryModel(0, 0, 0, 0);
    }

    /**
     * Close the database connection when done
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}