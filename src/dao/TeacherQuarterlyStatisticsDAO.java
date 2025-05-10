package src.dao;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.teaching.quarterly.TeacherQuarterlyStatisticsModel;
import java.sql.*;
import utils.DatabaseConnection;

public class TeacherQuarterlyStatisticsDAO {

    /**
     * Gets teacher statistics data based on filters
     *
     * @param fromQuarter the starting quarter
     * @param fromYear the starting year
     * @param toQuarter the ending quarter
     * @param toYear the ending year
     * @param status the approval status
     * @return a list of teacher statistics
     */
    public ObservableList<TeacherQuarterlyStatisticsModel> getTeacherStatistics(
            int fromQuarter, int fromYear, int toQuarter, int toYear, String status) {

        ObservableList<TeacherQuarterlyStatisticsModel> statistics = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = buildStatisticsQuery(status);
            PreparedStatement pstmt = conn.prepareStatement(query);

            setQueryParameters(pstmt, fromQuarter, fromYear, toQuarter, toYear, status);

            ResultSet rs = pstmt.executeQuery();
            int stt = 1;

            while (rs.next()) {
                String teacherName = rs.getString("teacher_name");
                int q1Sessions = rs.getInt("q1_sessions");
                double q1Hours = rs.getDouble("q1_hours");
                int q2Sessions = rs.getInt("q2_sessions");
                double q2Hours = rs.getDouble("q2_hours");

                statistics.add(new TeacherQuarterlyStatisticsModel(
                        stt++, teacherName, q1Sessions, q1Hours, q2Sessions, q2Hours));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving teacher statistics: " + e.getMessage());
        }

        return statistics;
    }

    /**
     * Gets summary data for the total row
     *
     * @param fromQuarter the starting quarter
     * @param fromYear the starting year
     * @param toQuarter the ending quarter
     * @param toYear the ending year
     * @param status the approval status
     * @return an array of summary values [q1Sessions, q1Hours, q2Sessions, q2Hours, totalSessions, totalHours]
     */
    public double[] getStatisticsSummary(
            int fromQuarter, int fromYear, int toQuarter, int toYear, String status) {

        double[] summaryData = new double[6]; // [q1Sessions, q1Hours, q2Sessions, q2Hours, totalSessions, totalHours]

        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = buildSummaryQuery(status);
            PreparedStatement pstmt = conn.prepareStatement(query);

            setQueryParameters(pstmt, fromQuarter, fromYear, toQuarter, toYear, status);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                summaryData[0] = rs.getDouble("total_q1_sessions");
                summaryData[1] = rs.getDouble("total_q1_hours");
                summaryData[2] = rs.getDouble("total_q2_sessions");
                summaryData[3] = rs.getDouble("total_q2_hours");
                summaryData[4] = rs.getDouble("total_sessions");
                summaryData[5] = rs.getDouble("total_hours");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error retrieving statistics summary: " + e.getMessage());
        }

        return summaryData;
    }

    private String buildStatisticsQuery(String status) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT t.teacher_name, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.sessions ELSE 0 END) AS q1_sessions, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.hours ELSE 0 END) AS q1_hours, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.sessions ELSE 0 END) AS q2_sessions, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.hours ELSE 0 END) AS q2_hours ");
        query.append("FROM teachers t ");
        query.append("JOIN teaching_sessions ts ON t.teacher_id = ts.teacher_id ");
        query.append("WHERE ((ts.quarter = ? AND ts.year = ?) OR (ts.quarter = ? AND ts.year = ?)) ");

        if (!status.equals("Tất cả")) {
            query.append("AND ts.approval_status = ? ");
        }

        query.append("GROUP BY t.teacher_id, t.teacher_name ");
        query.append("ORDER BY t.teacher_name");

        return query.toString();
    }

    private String buildSummaryQuery(String status) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.sessions ELSE 0 END) AS total_q1_sessions, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.hours ELSE 0 END) AS total_q1_hours, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.sessions ELSE 0 END) AS total_q2_sessions, ");
        query.append("SUM(CASE WHEN ts.quarter = ? AND ts.year = ? THEN ts.hours ELSE 0 END) AS total_q2_hours, ");
        query.append("SUM(ts.sessions) AS total_sessions, ");
        query.append("SUM(ts.hours) AS total_hours ");
        query.append("FROM teaching_sessions ts ");
        query.append("WHERE ((ts.quarter = ? AND ts.year = ?) OR (ts.quarter = ? AND ts.year = ?)) ");

        if (!status.equals("Tất cả")) {
            query.append("AND ts.approval_status = ? ");
        }

        return query.toString();
    }

    private void setQueryParameters(PreparedStatement pstmt, int fromQuarter, int fromYear,
                                    int toQuarter, int toYear, String status) throws SQLException {
        // For the first quarter values
        pstmt.setInt(1, fromQuarter);
        pstmt.setInt(2, fromYear);

        // For the first quarter hours
        pstmt.setInt(3, fromQuarter);
        pstmt.setInt(4, fromYear);

        // For the second quarter values
        pstmt.setInt(5, toQuarter);
        pstmt.setInt(6, toYear);

        // For the second quarter hours
        pstmt.setInt(7, toQuarter);
        pstmt.setInt(8, toYear);

        // For WHERE clause quarter/year criteria
        pstmt.setInt(9, fromQuarter);
        pstmt.setInt(10, fromYear);
        pstmt.setInt(11, toQuarter);
        pstmt.setInt(12, toYear);

        // If status is specified
        if (!status.equals("Tất cả")) {
            pstmt.setString(13, status);
        }
    }
}