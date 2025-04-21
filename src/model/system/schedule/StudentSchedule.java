package src.model.system.schedule;

//import other classes
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import src.model.system.course.Course;


// Student Schedule class
public class StudentSchedule extends Schedule {
    private String studentId;
    private List<Course> courses;

    public StudentSchedule(String id, String name, String description, LocalDateTime startTime, LocalDateTime endTime, String studentId) {
        super(id, name, description, startTime, endTime);
        this.studentId = studentId;
        this.courses = new ArrayList<>();
    }

    // Methods specific to StudentSchedule
    public int getCoursesTimePerDay(LocalDate day) {
        // Implementation to calculate total course time for a specific day
        int totalMinutes = 0;
        for (Course course : courses) {
            if (course.getDate().isDateWithinRange(day)) {
                // Assuming a standard course time per day (e.g. 2 hours = 120 minutes)
                totalMinutes += 120;
            }
        }
        return totalMinutes;
    }

    public boolean checkScheduleConflict(Course newCourse) {
        // Implementation to check if adding a new course would create a conflict
        for (Course existingCourse : courses) {
            // Check if dates overlap
            if (existingCourse.getDate().overlaps(newCourse.getDate())) {
                // For simplicity, assuming time conflict if date overlaps
                // In a real system, you'd check specific time slots
                return true; // Conflict exists
            }
        }
        return false; // No conflict
    }

    // Getters and setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course course) {
        if (!checkScheduleConflict(course)) {
            this.courses.add(course);
        } else {
            throw new IllegalArgumentException("Cannot add course due to schedule conflict");
        }
    }

    public void removeCourse(Course course) {
        this.courses.remove(course);
    }
}
