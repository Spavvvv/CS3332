package src.model.system;
/**
 * Department class represents a department entity with room information
 */
public class Department {
    // Attributes
    private String roomID;
    private String roomName;
    private int floor;
    private int capacity;
    private String status;

    /**
     * Default constructor
     */
    public Department() {
        this.roomID = "";
        this.roomName = "";
        this.floor = 1;
        this.capacity = 0;
        this.status = "Available";
    }

    /**
     * Parameterized constructor
     *
     * @param roomID The unique identifier for the room
     * @param roomName The name of the room
     * @param floor The floor where the room is located
     * @param capacity The capacity of the room
     * @param status The current status of the room (e.g., Available, Occupied)
     */
    public Department(String roomID, String roomName, int floor, int capacity, String status) {
        this.roomID = roomID;
        this.roomName = roomName;
        this.floor = floor;
        this.capacity = capacity;
        this.status = status;
    }

    // Getters and Setters

    public String getRoomID() {
        return roomID;
    }

    public void setRoomID(String roomID) {
        this.roomID = roomID;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the room is available
     *
     * @return true if the room status is "Available", false otherwise
     */
    public boolean isAvailable() {
        return "Available".equalsIgnoreCase(status);
    }

    /**
     * Updates the status of the room
     *
     * @param isOccupied set to true if room is occupied, false otherwise
     */
    public void updateStatus(boolean isOccupied) {
        this.status = isOccupied ? "Occupied" : "Available";
    }

    /**
     * Returns a string representation of the Department object
     *
     * @return String with department information
     */
    @Override
    public String toString() {
        return "Department{" +
                "roomID='" + roomID + '\'' +
                ", roomName='" + roomName + '\'' +
                ", floor=" + floor +
                ", capacity=" + capacity +
                ", status='" + status + '\'' +
                '}';
    }
}

