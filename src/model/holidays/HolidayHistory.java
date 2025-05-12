package src.model.holidays;

import java.time.LocalDateTime;

public class HolidayHistory {
    private Long id;
    private String user;
    private String action;
    private LocalDateTime timestamp;

    // Default constructor for JPA/database operations
    public HolidayHistory() {
    }

    public HolidayHistory(Long id, String user, String action, LocalDateTime timestamp) {
        this.id = id;
        this.user = user;
        this.action = action;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Format timestamp for display
    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return String.format("%02d:%02d:%02d/%02d/%02d/%d",
                timestamp.getHour(), timestamp.getMinute(), timestamp.getSecond(),
                timestamp.getDayOfMonth(), timestamp.getMonthValue(), timestamp.getYear());
    }
}