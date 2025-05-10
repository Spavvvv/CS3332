package src.model.system.schedule;

import java.time.LocalDateTime;

/**
 * Represents a schedule item for dashboard events
 */
public class ScheduleItem extends Schedule {
    private String title;
    private String time;

    /**
     * Constructor with basic information
     *
     * @param id Unique identifier
     * @param title Title of the schedule item
     * @param description Description of the schedule item
     * @param startTime Start time
     * @param endTime End time
     */
    public ScheduleItem(String id, String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        super(id, title, description, startTime, endTime);
        this.title = title;
        this.time = startTime.toLocalTime().toString();
    }

    /**
     * Default constructor for DAO operations
     */
    public ScheduleItem() {
        super("", "", "", LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Get the title of the schedule item
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title of the schedule item
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
        setName(title); // Update name in parent class
    }

    /**
     * Get the time of the schedule item
     *
     * @return the time
     */
    public String getTime() {
        return time;
    }

    /**
     * Set the time of the schedule item
     *
     * @param time the time to set
     */
    public void setTime(String time) {
        this.time = time;
    }
}
