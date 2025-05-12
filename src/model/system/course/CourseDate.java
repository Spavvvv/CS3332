package src.model.system.course;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CourseDate {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate currentDate;  // can be used to inheritance from current days
    private LocalTime startTime;    // start time of the course sessions
    private LocalTime endTime;      // end time of the course sessions
    private List<String> daysOfWeek; // days of the week when course is held (e.g., "Mon", "Wed", "Fri")

    // Constructor
    public CourseDate(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentDate = LocalDate.now();  // default current date is today
        this.daysOfWeek = new ArrayList<>();
        // Default time: 8am to 10am
        this.startTime = LocalTime.of(8, 0);
        this.endTime = LocalTime.of(10, 0);
    }

    // Getter and Setter
    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(LocalDate currentDate) {
        this.currentDate = currentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    /**
     * Add a day of the week to the course schedule
     * @param day the day to add (e.g., "Mon", "Tue", etc.)
     */
    public void addDayOfWeek(String day) {
        if (!daysOfWeek.contains(day)) {
            daysOfWeek.add(day);
        }
    }

    /**
     * Remove a day of the week from the course schedule
     * @param day the day to remove
     * @return true if the day was removed, false if it wasn't in the list
     */
    public boolean removeDayOfWeek(String day) {
        return daysOfWeek.remove(day);
    }

    /**
     * Get days of week as a comma-separated string
     * @return comma-separated string of days
     */
    public String getDaysOfWeekAsString() {
        return String.join(",", daysOfWeek);
    }

    /**
     * Set days of week from a comma-separated string
     * @param daysString comma-separated string of days (e.g., "Mon,Wed,Fri")
     */
    public void setDaysOfWeekFromString(String daysString) {
        if (daysString == null || daysString.trim().isEmpty()) {
            this.daysOfWeek = new ArrayList<>();
            return;
        }

        this.daysOfWeek = Arrays.stream(daysString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    // calculating total days
    public long getTotalDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include the last day
    }

    // calculating days elapsed
    public long getDaysElapsed() {
        LocalDate today = LocalDate.now();

        // if today is before start date, return 0
        if (today.isBefore(startDate)) {
            return 0;
        }

        // if today is after end date, return total days
        if (today.isAfter(endDate)) {
            return getTotalDays();
        }

        // if today is between start and end date, return days elapsed
        return ChronoUnit.DAYS.between(startDate, today) + 1; // +1 to include current day
    }

    // calculate progress percentage
    public double getProgressPercentage() {
        long totalDays = getTotalDays();
        long daysElapsed = getDaysElapsed();

        if (totalDays == 0) {
            return 0; // avoid division by zero
        }

        return (double) daysElapsed / totalDays * 100;
    }

    // check if course is completed
    public boolean isCompleted() {
        return LocalDate.now().isAfter(endDate);
    }

    // check if course has started
    public boolean hasStarted() {
        return LocalDate.now().isEqual(startDate) || LocalDate.now().isAfter(startDate);
    }

    // Check if course is currently active
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    // Check if a given date is within this course's date range
    public boolean isDateWithinRange(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // Check if this course date overlaps with another course date
    public boolean overlaps(CourseDate other) {
        return !this.startDate.isAfter(other.endDate) && !this.endDate.isBefore(other.startDate);
    }

    /**
     * Calculate the number of sessions in the course
     * @return the total number of sessions based on days of week and date range
     */
    public int getTotalSessions() {
        if (daysOfWeek.isEmpty()) {
            return 0;
        }

        // Convert day strings to day-of-week values
        List<Integer> courseDays = daysOfWeekToValues();

        int sessionCount = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Check if current date's day of week is in course days
            if (courseDays.contains(currentDate.getDayOfWeek().getValue())) {
                sessionCount++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return sessionCount;
    }

    /**
     * Convert day of week strings to day-of-week values (1-7, where 1 is Monday)
     * @return a list of day-of-week values
     */
    private List<Integer> daysOfWeekToValues() {
        List<Integer> values = new ArrayList<>();
        for (String day : daysOfWeek) {
            switch (day.toLowerCase()) {
                case "mon": values.add(1); break;
                case "tue": values.add(2); break;
                case "wed": values.add(3); break;
                case "thu": values.add(4); break;
                case "fri": values.add(5); break;
                case "sat": values.add(6); break;
                case "sun": values.add(7); break;
            }
        }
        return values;
    }

    /**
     * Get session duration in minutes
     * @return duration of each session in minutes
     */
    public long getSessionDurationMinutes() {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * Get total course hours
     * @return total hours for the course (sessions × duration)
     */
    public double getTotalCourseHours() {
        return getTotalSessions() * getSessionDurationMinutes() / 60.0;
    }

    // type showing
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("From ").append(startDate).append(" To ").append(endDate);

        if (!daysOfWeek.isEmpty()) {
            sb.append(" on ").append(String.join(", ", daysOfWeek));
        }

        if (startTime != null && endTime != null) {
            sb.append(" at ").append(startTime).append(" - ").append(endTime);
        }

        return sb.toString();
    }
}
