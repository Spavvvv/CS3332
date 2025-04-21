package src.model.system.course;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CourseDate {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate currentDate;  // can be used to inheritance from current days

    // Constructor
    public CourseDate(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentDate = LocalDate.now();  // default current date is today
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
        return ChronoUnit.DAYS.between(startDate, today) + 1; // +1 để bao gồm ngày hiện tại
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

    // type showing
    @Override
    public String toString() {
        return "Từ " + startDate + " đến " + endDate;
    }
}
