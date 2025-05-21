package src.model.teaching.daily;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

// Nested class for daily statistics
public class DailyStatistics {
    private final SimpleIntegerProperty sessions;
    private final SimpleDoubleProperty hours;

    public DailyStatistics(int sessions, double hours) {
        this.sessions = new SimpleIntegerProperty(sessions);
        this.hours = new SimpleDoubleProperty(hours);
    }

    public int getSessions() {
        return sessions.get();
    }

    public SimpleIntegerProperty sessionsProperty() {
        return sessions;
    }

    public void setSessions(int sessions) {
        this.sessions.set(sessions);
    }

    public double getHours() {
        return hours.get();
    }

    public SimpleDoubleProperty hoursProperty() {
        return hours;
    }

    public void setHours(double hours) {
        this.hours.set(hours);
    }
}