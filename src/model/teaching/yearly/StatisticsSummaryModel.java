package src.model.teaching.yearly;

public class StatisticsSummaryModel {
    private int yearSessions;
    private double yearHours;
    private int totalSessions;
    private double totalHours;

    public StatisticsSummaryModel(int yearSessions, double yearHours, int totalSessions, double totalHours) {
        this.yearSessions = yearSessions;
        this.yearHours = yearHours;
        this.totalSessions = totalSessions;
        this.totalHours = totalHours;
    }

    // Getters and setters
    public int getYearSessions() { return yearSessions; }
    public void setYearSessions(int yearSessions) { this.yearSessions = yearSessions; }

    public double getYearHours() { return yearHours; }
    public void setYearHours(double yearHours) { this.yearHours = yearHours; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
}