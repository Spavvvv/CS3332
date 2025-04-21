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
            // Would need a more sophisticated overlap check in real implementation
            if (startTime.isBefore(scheduledCourse.getDate().getEndDate().atTime(LocalTime.MAX)) &&
                    endTime.isAfter(scheduledCourse.getDate().getStartDate().atTime(LocalTime.MIN))) {
                return false; // Room is already occupied
            }
        }

        // Check if course type matches room type (assuming subject matches room type)
        if (!course.getSubject().equals(this.getTypeOfCourse())) {
            return false; // Course type doesn't match room type
        }

        // Allocate the shift by adding to scheduled courses
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
}
