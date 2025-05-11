package src.model.system.course;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import src.model.person.Student;
import src.model.person.Teacher;

public class Course {
    private String courseId;
    private String courseName;
    private String subject;

    // Fields previously in CourseDate, now in Course
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;    // start time of the course sessions
    private LocalTime endTime;      // end time of the course sessions
    private List<String> daysOfWeek; // days of the week when course is held (e.g., "Mon", "Wed", "Fri")

    private Teacher teacher;
    private List<Student> students;
    private int totalCurrentStudent;
    private float progress; // Variable to store progress loaded from DB
    private String roomId;

    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;

        // Initialize fields with defaults or empty lists
        this.daysOfWeek = new ArrayList<>();
        // Default times, assuming they might be set later or retrieved from DB
        this.startTime = null;
        this.endTime = null;

        this.students = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
    }

    // Constructor with more parameters including times and daysOfWeek
    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate,
                  LocalTime startTime, LocalTime endTime, List<String> daysOfWeek) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysOfWeek = daysOfWeek != null ? new ArrayList<>(daysOfWeek) : new ArrayList<>();

        this.students = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
    }

    // Default constructor
    public Course() {
        this.students = new ArrayList<>();
        this.daysOfWeek = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
    }

    // --- Getters and Setters for Course fields ---
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public List<Student> getStudents() {
        // Return a copy to prevent external modification of the internal list
        return new ArrayList<>(students);
    }

    public void setStudents(List<Student> students) {
        // Ensure the incoming list is not null before assigning
        if (students != null) {
            this.students = new ArrayList<>(students); // Store a copy
            this.totalCurrentStudent = this.students.size();
        } else {
            // Handle null input by initializing an empty list and resetting count
            this.students = new ArrayList<>();
            this.totalCurrentStudent = 0;
        }
    }

    public int getTotalCurrentStudent() {
        return totalCurrentStudent;
    }

    public void updateTotalCurrentStudent() {
        this.totalCurrentStudent = students != null ? students.size() : 0;
    }

    /**
     * Gets the current progress of the course.
     * This value is typically loaded from the database.
     *
     * @return the course progress as a float
     */
    public float getProgress() {
        return progress; // Return the stored progress value
    }

    /**
     * Sets the progress of the course.
     * This method should be used to update the progress value,
     * for example, when loading from the database or after calculating it.
     *
     * @param progress the new progress value
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void addStudent(Student student) {
        // Check if the students list is initialized
        if (this.students == null) {
            this.students = new ArrayList<>();
        }
        // Check if the student is not already in the list before adding
        if (student != null && !this.students.contains(student)) {
            this.students.add(student);
            updateTotalCurrentStudent();
        }
    }

    public void removeStudent(Student student) {
        // Check if the students list is initialized and contains the student
        if (this.students != null && student != null) {
            if (this.students.remove(student)) {
                updateTotalCurrentStudent();
            }
        }
    }

    // --- Getters and Setters for fields moved from CourseDate ---
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
        // Return a copy to prevent external modification of the internal list
        return new ArrayList<>(daysOfWeek);
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        // Ensure the incoming list is not null before assigning
        if (daysOfWeek != null) {
            this.daysOfWeek = new ArrayList<>(daysOfWeek); // Store a copy
        } else {
            this.daysOfWeek = new ArrayList<>(); // Initialize with an empty list if null
        }
    }

    // --- Methods previously in CourseDate, now in Course ---

    /**
     * Add a day of the week to the course schedule
     * @param day the day to add (e.g., "Mon", "Tue", etc.)
     */
    public void addDayOfWeek(String day) {
        // Ensure daysOfWeek list is initialized
        if (this.daysOfWeek == null) {
            this.daysOfWeek = new ArrayList<>();
        }
        if (day != null && !day.trim().isEmpty()) {
            String trimmedDay = day.trim();
            if (!daysOfWeek.contains(trimmedDay)) {
                daysOfWeek.add(trimmedDay);
            }
        }
    }

    /**
     * Remove a day of the week from the course schedule
     * @param day the day to remove
     * @return true if the day was removed, false if it wasn't in the list
     */
    public boolean removeDayOfWeek(String day) {
        if (this.daysOfWeek != null && day != null && !day.trim().isEmpty()) {
            return daysOfWeek.remove(day.trim());
        }
        return false;
    }

    /**
     * Get days of week as a comma-separated string
     * @return comma-separated string of days
     */
    public String getDaysOfWeekAsString() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return "";
        }
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
                .filter(day -> !day.isEmpty()) // Filter out empty strings resulting from split
                .distinct() // Ensure no duplicate days
                .collect(Collectors.toList());
    }

    // calculating total days in the date range
    public long getTotalDaysInRange() {
        if (startDate == null || endDate == null) {
            return 0; // Cannot calculate if dates are null
        }
        // Ensure startDate is not after endDate
        if (startDate.isAfter(endDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include the last day
    }

    // calculating days elapsed in the date range up to today
    public long getDaysElapsedInRange() {
        if (startDate == null || endDate == null) {
            return 0; // Cannot calculate if dates are null
        }

        LocalDate today = LocalDate.now();

        // if today is before start date, return 0 elapsed
        if (today.isBefore(startDate)) {
            return 0;
        }

        // if today is after end date, return total days in range
        if (today.isAfter(endDate)) {
            return getTotalDaysInRange();
        }

        // if today is between start and end date, return days elapsed
        return ChronoUnit.DAYS.between(startDate, today) + 1; // +1 to include current day
    }

    /**
     * Calculates the progress percentage based on the current date relative to the course dates.
     * This is a calculation based on the date range, not the progress value stored in the database.
     *
     * @return the calculated progress as a double
     */
    public double calculateProgressBasedOnDate() {
        long totalDays = getTotalDaysInRange();
        long daysElapsed = getDaysElapsedInRange();

        if (totalDays <= 0) { // Handle totalDays being 0 or negative
            return 0.0; // avoid division by zero or incorrect percentage
        }

        // Ensure daysElapsed does not exceed totalDays
        if (daysElapsed > totalDays) {
            daysElapsed = totalDays;
        }

        return (double) daysElapsed / totalDays * 100.0;
    }

    // check if course is completed based on end date
    public boolean isCompleted() {
        if (endDate == null) {
            return false; // Cannot be completed if end date is null
        }
        return LocalDate.now().isAfter(endDate);
    }

    // check if course has started based on start date
    public boolean hasStarted() {
        if (startDate == null) {
            return false; // Cannot have started if start date is null
        }
        return LocalDate.now().isEqual(startDate) || LocalDate.now().isAfter(startDate);
    }

    // Check if course is currently active based on date range
    public boolean isActive() {
        if (startDate == null || endDate == null) {
            return false; // Cannot be active if dates are null
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    // Check if a given date is within this course's date range
    public boolean isDateWithinRange(LocalDate date) {
        if (startDate == null || endDate == null || date == null) {
            return false; // Cannot check range if any date is null
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // Check if this course date range overlaps with another course's date range
    public boolean overlapsDateRange(Course other) {
        if (this.startDate == null || this.endDate == null || other.getStartDate() == null || other.getEndDate() == null) {
            return false; // Cannot check overlap if any date is null
        }
        // Check for non-overlap: Does this course end before the other starts OR does this course start after the other ends?
        boolean nonOverlap = this.endDate.isBefore(other.getStartDate()) || this.startDate.isAfter(other.getEndDate());
        return !nonOverlap; // If they don't non-overlap, they must overlap
    }


    /**
     * Calculate the number of sessions in the course based on date range and days of week.
     *
     * @return the total number of sessions
     */
    public int getTotalSessions() {
        if (startDate == null || endDate == null || daysOfWeek == null || daysOfWeek.isEmpty()) {
            return 0;
        }

        List<Integer> courseDays = daysOfWeekToValues();
        if (courseDays.isEmpty()) {
            return 0; // No sessions if no valid days are set
        }

        int sessionCount = 0;
        LocalDate currentDate = startDate;

        // Ensure startDate is not after endDate
        if (startDate.isAfter(endDate)) {
            return 0;
        }

        while (!currentDate.isAfter(endDate)) {
            if (courseDays.contains(currentDate.getDayOfWeek().getValue())) {
                sessionCount++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return sessionCount;
    }

    /**
     * Convert day of week strings (e.g., "Mon") to day-of-week values (1-7, where 1 is Monday)
     *
     * @return a list of day-of-week values
     */
    private List<Integer> daysOfWeekToValues() {
        List<Integer> values = new ArrayList<>();
        if (daysOfWeek == null) {
            return values; // Return empty list if daysOfWeek is null
        }
        for (String day : daysOfWeek) {
            if (day != null) {
                switch (day.trim().toLowerCase()) {
                    case "mon": values.add(1); break;
                    case "tue": values.add(2); break;
                    case "wed": values.add(3); break;
                    case "thu": values.add(4); break;
                    case "fri": values.add(5); break;
                    case "sat": values.add(6); break;
                    case "sun": values.add(7); break;
                }
            }
        }
        return values;
    }

    /**
     * Get session duration in minutes
     *
     * @return duration of each session in minutes, or 0 if times are null or invalid
     */
    public long getSessionDurationMinutes() {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return 0; // Cannot calculate duration if times are null or start is after end
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * Get total course hours based on calculated sessions and session duration.
     *
     * @return total hours for the course (sessions × duration), or 0.0 if calculation is not possible
     */
    public double getTotalCourseHours() {
        long sessionDuration = getSessionDurationMinutes();
        int totalSessions = getTotalSessions();

        if (sessionDuration <= 0 || totalSessions <= 0) {
            return 0.0;
        }

        return (double) totalSessions * sessionDuration / 60.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Course [").append(courseId).append("] ").append(courseName).append(" - ").append(subject);
        sb.append(" From ").append(startDate).append(" To ").append(endDate);

        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            sb.append(" on ").append(getDaysOfWeekAsString());
        }

        if (startTime != null && endTime != null) {
            sb.append(" at ").append(startTime).append(" - ").append(endTime);
        }
        sb.append(", Room: ").append(roomId);
        sb.append(", Progress: ").append(String.format("%.2f%%", progress));
        sb.append(", Students: ").append(totalCurrentStudent);


        return sb.toString();
    }

    //actually, it's kinda strange when a getDate method return object... idk @@
    public Course getDate() {
        return this;
    }

    // Removed the getDate() method as CourseDate is no longer used.
}
