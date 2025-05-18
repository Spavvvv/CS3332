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
import view.components.ClassList.ClassListScreenView;

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

    private Teacher teacher; // Holds the Teacher object. teacher_id is accessible via teacher.getId()
    private String roomId;   // Foreign key to rooms/classrooms table
    private String classId;  // Foreign key to classes table (student group/cohort)

    private List<Student> students; // Transient or managed by enrollment
    private int totalCurrentStudent; // Derived from students list or enrollment
    private float progress; // Variable to store progress loaded from DB

    // Constructor with classId
    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate,
                  LocalTime startTime, LocalTime endTime, List<String> daysOfWeek, String roomId, String classId, Teacher teacher) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysOfWeek = daysOfWeek != null ? new ArrayList<>(daysOfWeek) : new ArrayList<>();
        this.roomId = roomId;
        this.classId = classId; // Initialize classId
        this.teacher = teacher;

        this.students = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
    }

    // Simpler constructor, might be used when not all details are immediately available
    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;

        this.daysOfWeek = new ArrayList<>();
        this.startTime = null;
        this.endTime = null;
        this.roomId = null;
        this.classId = null; // Initialize classId
        this.teacher = null;

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
        // classId, roomId, teacher will be null by default
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

    /**
     * Gets the ID of the teacher assigned to this course.
     * Returns an empty string if no teacher is assigned or if teacher object is null.
     * @return Teacher's ID or empty string.
     */
    public String getTeacherId() {
        if (teacher != null) {
            return teacher.getId(); // Assumes Teacher model has getId()
        }
        return ""; // Or null, depending on how you want to handle no teacher
    }

    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public void setStudents(List<Student> students) {
        if (students != null) {
            this.students = new ArrayList<>(students);
            this.totalCurrentStudent = this.students.size();
        } else {
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

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /**
     * Gets the ID of the class (student group/cohort) associated with this course.
     * This ID links to the 'classes' table.
     * @return The class ID.
     */
    public String getClassId() {
        return classId;
    }

    /**
     * Sets the ID of the class (student group/cohort) associated with this course.
     * @param classId The class ID from the 'classes' table.
     */
    public void setClassId(String classId) {
        this.classId = classId;
    }


    public void addStudent(Student student) {
        if (this.students == null) {
            this.students = new ArrayList<>();
        }
        if (student != null && !this.students.contains(student)) {
            this.students.add(student);
            updateTotalCurrentStudent();
        }
    }

    public void removeStudent(Student student) {
        if (this.students != null && student != null) {
            if (this.students.remove(student)) {
                updateTotalCurrentStudent();
            }
        }
    }

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

    // Renamed from getStartTime to avoid conflict if a general "get start time of course entity" was meant
    public LocalTime getCourseStartTime() {
        return startTime;
    }

    // Renamed from setStartTime
    public void setCourseSessionStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    // Renamed from getEndTime
    public LocalTime getCourseEndTime() {
        return endTime;
    }

    // Renamed from setEndTime
    public void setCourseSessionEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getDaysOfWeekList() { // Renamed to avoid confusion with getDaysOfWeekAsString
        return new ArrayList<>(daysOfWeek);
    }

    public void setDaysOfWeekList(List<String> daysOfWeek) { // Renamed
        if (daysOfWeek != null) {
            this.daysOfWeek = new ArrayList<>(daysOfWeek);
        } else {
            this.daysOfWeek = new ArrayList<>();
        }
    }

    public void addDayOfWeek(String day) {
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

    public boolean removeDayOfWeek(String day) {
        if (this.daysOfWeek != null && day != null && !day.trim().isEmpty()) {
            return daysOfWeek.remove(day.trim());
        }
        return false;
    }

    /**
     * Get days of week as a comma-separated string (e.g., "Mon,Wed,Fri").
     * This is suitable for display or for storage in a database column
     * if the column expects this format.
     * The ClassSessionDAO will take this string, convert to uppercase, and then split.
     * @return comma-separated string of days, or empty string if none.
     */
    public String getDaysOfWeekAsString() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return "";
        }
        // Example: if daysOfWeek is ["Mon", "Wed"], this returns "Mon,Wed"
        // The ClassSessionDAO will take this and convert to "MONDAY,WEDNESDAY"
        return String.join(",", daysOfWeek);
    }

    public void setDaysOfWeekFromString(String daysString) {
        if (daysString == null || daysString.trim().isEmpty()) {
            this.daysOfWeek = new ArrayList<>();
            return;
        }
        this.daysOfWeek = Arrays.stream(daysString.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public long getTotalDaysInRange() {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public long getDaysElapsedInRange() {
        if (startDate == null || endDate == null) return 0;
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return 0;
        if (today.isAfter(endDate)) return getTotalDaysInRange();
        return ChronoUnit.DAYS.between(startDate, today) + 1;
    }

    public double calculateProgressBasedOnDate() {
        long totalDays = getTotalDaysInRange();
        long daysElapsed = getDaysElapsedInRange();
        if (totalDays <= 0) return 0.0;
        if (daysElapsed > totalDays) daysElapsed = totalDays;
        return (double) daysElapsed / totalDays * 100.0;
    }

    public boolean isCompleted() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    public boolean hasStarted() {
        return startDate != null && !LocalDate.now().isBefore(startDate);
    }

    public boolean isActive() {
        if (startDate == null || endDate == null) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    public boolean isDateWithinRange(LocalDate date) {
        if (startDate == null || endDate == null || date == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean overlapsDateRange(Course other) {
        if (this.startDate == null || this.endDate == null || other.getStartDate() == null || other.getEndDate() == null) {
            return false;
        }
        return !this.endDate.isBefore(other.getStartDate()) && !this.startDate.isAfter(other.getEndDate());
    }

    public int getTotalSessions() {
        if (startDate == null || endDate == null || daysOfWeek == null || daysOfWeek.isEmpty() || startDate.isAfter(endDate)) {
            return 0;
        }
        List<java.time.DayOfWeek> courseDaysEnum = daysOfWeekToEnumValues(); // Changed to use java.time.DayOfWeek
        if (courseDaysEnum.isEmpty()) return 0;

        int sessionCount = 0;
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (courseDaysEnum.contains(currentDate.getDayOfWeek())) {
                sessionCount++;
            }
            currentDate = currentDate.plusDays(1);
        }
        return sessionCount;
    }

    /**
     * Convert day of week strings (e.g., "Mon", "MONDAY") to java.time.DayOfWeek enum values.
     * This makes comparison more robust.
     * @return a list of java.time.DayOfWeek enum values
     */
    private List<java.time.DayOfWeek> daysOfWeekToEnumValues() {
        List<java.time.DayOfWeek> values = new ArrayList<>();
        if (daysOfWeek == null) return values;
        for (String day : daysOfWeek) {
            if (day != null) {
                try {
                    // Attempt to parse common abbreviations and full names
                    String upperDay = day.trim().toUpperCase();
                    if (upperDay.length() >= 3) {
                        String threeLetterDay = upperDay.substring(0,3);
                        switch (threeLetterDay) {
                            case "MON": values.add(java.time.DayOfWeek.MONDAY); break;
                            case "TUE": values.add(java.time.DayOfWeek.TUESDAY); break;
                            case "WED": values.add(java.time.DayOfWeek.WEDNESDAY); break;
                            case "THU": values.add(java.time.DayOfWeek.THURSDAY); break;
                            case "FRI": values.add(java.time.DayOfWeek.FRIDAY); break;
                            case "SAT": values.add(java.time.DayOfWeek.SATURDAY); break;
                            case "SUN": values.add(java.time.DayOfWeek.SUNDAY); break;
                            default: // Try full name parsing if abbreviation fails
                                values.add(java.time.DayOfWeek.valueOf(upperDay)); break;
                        }
                    } else {
                        values.add(java.time.DayOfWeek.valueOf(upperDay)); // For full names like MONDAY
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Could not parse day of week: " + day);
                    // Optionally log this error or handle it as appropriate
                }
            }
        }
        return values.stream().distinct().collect(Collectors.toList()); // Ensure uniqueness
    }


    public long getSessionDurationMinutes() {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    public double getTotalCourseHours() {
        long sessionDuration = getSessionDurationMinutes();
        int totalSessions = getTotalSessions();
        if (sessionDuration <= 0 || totalSessions <= 0) return 0.0;
        return (double) totalSessions * sessionDuration / 60.0;
    }

    @Override
    public String toString() {
        return "Course [" + courseId + "] " + courseName + " - " + subject +
                ", Dates: " + startDate + " to " + endDate +
                ", Days: " + getDaysOfWeekAsString() +
                ", Times: " + (startTime != null ? startTime : "N/A") + " - " + (endTime != null ? endTime : "N/A") +
                ", RoomID: " + (roomId != null ? roomId : "N/A") +
                ", ClassID: " + (classId != null ? classId : "N/A") + // Added classId
                ", Teacher: " + (teacher != null ? teacher.getName() : "N/A") + // Assumes teacher.getName()
                ", Progress: " + String.format("%.2f%%", progress) +
                ", Students: " + totalCurrentStudent;
    }

    // Removed: public Course getDate() { return this; } - This was confusing.

    // These are already present or were renamed slightly
    // public LocalTime getCourseStartTime() { return startTime; } // Now getCourseSessionStartTime()
    // public LocalTime getCourseEndTime() { return endTime; } // Now getCourseSessionEndTime()

    // Equals and HashCode (consider which fields define uniqueness for a Course)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(courseId, course.courseId); // Primary key equality
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId); // Primary key hashcode
    }

    //actually, it's kinda strange when a getDate method return object... idk @@
    public Course getDate() {
        return this;
    }
}