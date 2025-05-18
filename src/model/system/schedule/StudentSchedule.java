
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

    public boolean checkScheduleConflict(Course newCourse) {
        // Implementation to check if adding a new course would create a conflict
        for (Course existingCourse : courses) {
            // Check if dates overlap. This uses the overlapsDateRange method from the Course model.
            // This method should compare the date ranges of the two courses.
            if (existingCourse.overlapsDateRange(newCourse)) {
                // Further check for time conflict on common days of the week
                // This part is simplified in the original comment. A full check would involve:
                // 1. Finding common days of the week.
                // 2. For those common days, checking if their LocalTime intervals overlap.
                // For now, respecting the original simplicity:
                return true; // Conflict exists if date ranges overlap
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
            // Consider making this a custom, checked exception for better error handling.
            throw new IllegalArgumentException("Cannot add course '" + course.getCourseName() + "' due to schedule conflict with an existing course.");
        }
    }

    public void removeCourse(Course course) {
        this.courses.remove(course);
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }
}

