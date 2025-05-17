
package src.model.system.schedule;

//import other classes
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import src.model.system.course.Course;


// Room Schedule class
public class RoomSchedule extends Schedule {
    private String roomId;
    private int capacity;
    private String roomType;
    private List<Course> scheduledCourses;

    public RoomSchedule(String id, String name, String description, LocalDateTime startTime, LocalDateTime endTime,
                        String roomId, int capacity, String roomType) {
        super(id, name, description, startTime, endTime);
        this.roomId = roomId;
        this.capacity = capacity;
        this.roomType = roomType;
        this.scheduledCourses = new ArrayList<>();
    }

    // Methods specific to RoomSchedule
    public String getTypeOfCourse() {
        // Return the appropriate course type suitable for this room
        return this.roomType;
    }

    public boolean allocateShift(Course course, LocalDateTime startTime, LocalDateTime endTime) {
        // Check if the room is free at the requested time
        for (Course scheduledCourse : scheduledCourses) {
            // This checks if the proposed (startTime, endTime) interval for the new course
            // overlaps with the (startDate at 00:00, endDate at 23:59:59) of an existing scheduledCourse.
            // This is a coarse check and might need refinement for precise time slot checking including days of the week.
            if (scheduledCourse.getStartDate() != null && scheduledCourse.getEndDate() != null &&
                    startTime.isBefore(scheduledCourse.getEndDate().atTime(LocalTime.MAX)) &&
                    endTime.isAfter(scheduledCourse.getStartDate().atTime(LocalTime.MIN))) {
                return false; // Room is already occupied at some point during the scheduled course's date range
            }
        }

        // Check if course type matches room type (assuming subject matches room type)
        if (course.getSubject() != null && !course.getSubject().equals(this.getTypeOfCourse())) {
            return false; // Course type doesn't match room type
        }

        // Allocate the shift by adding to scheduled courses
        // Note: This adds the generic 'course' object. If the 'startTime' and 'endTime'
        // are specific to this instance, you might need a different structure or to update the course object.
        scheduledCourses.add(course);
        return true;
    }

    public boolean checkCapacity(int requiredCapacity) {
        return this.capacity >= requiredCapacity;
    }

    // Getters and setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public List<Course> getScheduledCourses() {
        return scheduledCourses;
    }

    public void setScheduledCourses(List<Course> scheduledCourses) {
        this.scheduledCourses = scheduledCourses;
    }
}

