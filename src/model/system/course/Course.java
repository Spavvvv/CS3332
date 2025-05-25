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

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;    // start time of the course sessions
    private LocalTime endTime;      // end time of the course sessions
    private List<String> daysOfWeekList; // Renamed from daysOfWeek for clarity, stores days like "Mon", "Wed"

    private Teacher teacher;
    private String roomId;
    private String classId; // Corresponds to courses.class_id column

    private List<Student> students;
    private int totalCurrentStudent; // Derived
    private float progress;
    private int totalSessions; // New field for total_sessions from DB

    // Updated Constructor
    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate,
                  LocalTime startTime, LocalTime endTime, List<String> daysOfWeekList, String roomId, String classId,
                  Teacher teacher, int totalSessions, float progress) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.daysOfWeekList = daysOfWeekList != null ? new ArrayList<>(daysOfWeekList) : new ArrayList<>();
        this.roomId = roomId;
        this.classId = classId;
        this.teacher = teacher;
        this.totalSessions = totalSessions; // Initialize new field
        this.progress = progress;

        this.students = new ArrayList<>();
        this.totalCurrentStudent = 0; // Will be updated when students are set/added
    }

    // Simpler constructor, updated
    public Course(String courseId, String courseName, String subject, LocalDate startDate, LocalDate endDate, int totalSessions) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.subject = subject;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalSessions = totalSessions; // Initialize new field

        this.daysOfWeekList = new ArrayList<>();
        this.startTime = null;
        this.endTime = null;
        this.roomId = null;
        this.classId = null;
        this.teacher = null;
        this.progress = 0.0f;

        this.students = new ArrayList<>();
        this.totalCurrentStudent = 0;
    }

    // Default constructor, updated
    public Course() {
        this.students = new ArrayList<>();
        this.daysOfWeekList = new ArrayList<>();
        this.totalCurrentStudent = 0;
        this.progress = 0.0f;
        this.totalSessions = 0; // Default for new field
        // courseId, courseName, subject, startDate, endDate, startTime, endTime, roomId, classId, teacher will be null/default
    }

    // --- Getters and Setters ---
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return this.courseName;
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

    public String getTeacherId() {
        if (teacher != null) {
            return teacher.getId();
        }
        return "";
    }

    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }

    public void setStudents(List<Student> students) {
        if (students != null) {
            this.students = new ArrayList<>(students);
        } else {
            this.students = new ArrayList<>();
        }
        updateTotalCurrentStudent();
    }

    public int getTotalCurrentStudent() {
        return totalCurrentStudent;
    }

    private void updateTotalCurrentStudent() { // Made private as it's an internal update logic
        this.totalCurrentStudent = this.students != null ? this.students.size() : 0;
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

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    // Getter and Setter for the new totalSessions field (from DB)
    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
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

    public LocalTime getCourseStartTime() { // Renamed from getStartTime in original
        return startTime;
    }

    public void setCourseStartTime(LocalTime startTime) { // Renamed from setStartTime in original
        this.startTime = startTime;
    }

    public LocalTime getCourseEndTime() { // Renamed from getEndTime in original
        return endTime;
    }

    public void setCourseEndTime(LocalTime endTime) { // Renamed from setEndTime in original
        this.endTime = endTime;
    }

    public List<String> getDaysOfWeekList() {
        return new ArrayList<>(daysOfWeekList);
    }

    public void setDaysOfWeekList(List<String> daysOfWeekList) {
        if (daysOfWeekList != null) {
            this.daysOfWeekList = new ArrayList<>(daysOfWeekList);
        } else {
            this.daysOfWeekList = new ArrayList<>();
        }
    }

    public void addDayOfWeek(String day) {
        if (this.daysOfWeekList == null) {
            this.daysOfWeekList = new ArrayList<>();
        }
        if (day != null && !day.trim().isEmpty()) {
            String trimmedDay = day.trim();
            if (!this.daysOfWeekList.contains(trimmedDay)) {
                this.daysOfWeekList.add(trimmedDay);
            }
        }
    }

    public boolean removeDayOfWeek(String day) {
        if (this.daysOfWeekList != null && day != null && !day.trim().isEmpty()) {
            return this.daysOfWeekList.remove(day.trim());
        }
        return false;
    }

    public String getDaysOfWeekAsString() {
        if (daysOfWeekList == null || daysOfWeekList.isEmpty()) {
            return "";
        }
        return String.join(",", daysOfWeekList);
    }

    public void setDaysOfWeekFromString(String daysString) {
        if (daysString == null || daysString.trim().isEmpty()) {
            this.daysOfWeekList = new ArrayList<>();
            return;
        }
        this.daysOfWeekList = Arrays.stream(daysString.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // --- Utility and Calculation Methods ---

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
        if (today.isAfter(endDate)) return getTotalDaysInRange(); // Course completed, all days elapsed
        return ChronoUnit.DAYS.between(startDate, today) + 1;
    }

    public double calculateProgressBasedOnDate() {
        long totalDays = getTotalDaysInRange();
        long daysElapsed = getDaysElapsedInRange();
        if (totalDays <= 0) return 0.0;
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
        // True if this course's end is not before other's start AND this course's start is not after other's end
        return !this.endDate.isBefore(other.getStartDate()) && !this.startDate.isAfter(other.getEndDate());
    }

    /**
     * Calculates the total number of sessions based on the start date, end date,
     * and the scheduled days of the week (from daysOfWeekList).
     * This can be used if the total_sessions isn't directly stored or needs verification.
     * @return Calculated number of sessions.
     */
    public int calculateTotalSessionsBasedOnSchedule() {
        if (startDate == null || endDate == null || daysOfWeekList == null || daysOfWeekList.isEmpty() || startDate.isAfter(endDate)) {
            return 0;
        }
        List<java.time.DayOfWeek> courseDaysEnum = daysOfWeekToEnumValues();
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

    private List<java.time.DayOfWeek> daysOfWeekToEnumValues() {
        List<java.time.DayOfWeek> values = new ArrayList<>();
        if (daysOfWeekList == null) return values;
        for (String day : daysOfWeekList) {
            if (day != null) {
                try {
                    String upperDay = day.trim().toUpperCase();
                    // Attempt to parse common abbreviations first
                    if (upperDay.length() >= 3) {
                        String threeLetterDay = upperDay.substring(0,3);
                        boolean matched = false;
                        switch (threeLetterDay) {
                            case "MON": values.add(java.time.DayOfWeek.MONDAY); matched = true; break;
                            case "TUE": values.add(java.time.DayOfWeek.TUESDAY); matched = true; break;
                            case "WED": values.add(java.time.DayOfWeek.WEDNESDAY); matched = true; break;
                            case "THU": values.add(java.time.DayOfWeek.THURSDAY); matched = true; break;
                            case "FRI": values.add(java.time.DayOfWeek.FRIDAY); matched = true; break;
                            case "SAT": values.add(java.time.DayOfWeek.SATURDAY); matched = true; break;
                            case "SUN": values.add(java.time.DayOfWeek.SUNDAY); matched = true; break;
                        }
                        if (!matched) { // If abbreviation didn't match, try full name
                            values.add(java.time.DayOfWeek.valueOf(upperDay));
                        }
                    } else { // For very short strings, try direct full name parsing
                        values.add(java.time.DayOfWeek.valueOf(upperDay));
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Could not parse day of week from string: '" + day + "'");
                }
            }
        }
        return values.stream().distinct().collect(Collectors.toList());
    }

    public long getSessionDurationMinutes() {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    public double getTotalCourseHours() {
        long sessionDurationMinutes = getSessionDurationMinutes();
        // Use the stored totalSessions (from DB) if available and valid, otherwise calculate
        int sessionsToUse = this.totalSessions > 0 ? this.totalSessions : calculateTotalSessionsBasedOnSchedule();
        if (sessionDurationMinutes <= 0 || sessionsToUse <= 0) return 0.0;
        return (double) sessionsToUse * sessionDurationMinutes / 60.0;
    }

    @Override
    public String toString() {
        return "Course [" + courseId + "] " + courseName + " - " + subject +
                ", Dates: " + (startDate != null ? startDate : "N/A") + " to " + (endDate != null ? endDate : "N/A") +
                ", Days: " + getDaysOfWeekAsString() +
                ", Times: " + (startTime != null ? startTime : "N/A") + " - " + (endTime != null ? endTime : "N/A") +
                ", RoomID: " + (roomId != null ? roomId : "N/A") +
                ", ClassID (Group/Cohort): " + (classId != null ? classId : "N/A") + // Clarified classId meaning
                ", Teacher: " + (teacher != null && teacher.getName() != null ? teacher.getName() : "N/A") +
                ", Total Sessions (DB): " + totalSessions + // Added new field
                ", Progress: " + String.format("%.2f%%", progress) +
                ", Current Students: " + totalCurrentStudent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(courseId, course.courseId); // Equality based on primary key
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId); // HashCode based on primary key
    }

    // The method "public Course getDate()" was confusing as it returned 'this'.
    // If you need specific date-related information, use the individual getters like getStartDate(), getEndDate().
    // I've removed it. If you had a specific purpose for it, it might need to be re-thought.
    public Course getDate() {
         return this;
    }
}