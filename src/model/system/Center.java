package src.model.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import src.model.person.Admin;
import src.model.person.Parent;
import src.model.person.Student;
import src.model.person.Teacher;
import src.model.system.course.Course;

import src.model.system.schedule.RoomSchedule;
import src.model.system.Department;

/**
 * Represents an educational center with various accounts and information.
 * Based on the entity diagram showing Account Roles and Center Information.
 */
public class Center {
    // Center basic information
    private String centerId;
    private String centerName;
    private String address;
    private String description;

    // Contact Information
    private String managerNumber;
    private String managerEmail;

    // Account Role collections
    private List<Admin> adminAccounts;
    private List<Teacher> teacherAccounts;
    private List<Student> studentAccounts;
    private List<Parent> parentAccounts;

    // Course information
    private List<Course> courses;
    private Map<String, RoomSchedule> roomSchedules;
    private Map<String, Department> departments;

    /**
     * Constructor for Center
     */
    public Center(String centerId, String centerName, String address) {
        this.centerId = centerId;
        this.centerName = centerName;
        this.address = address;

        // Initialize collections
        this.adminAccounts = new ArrayList<>();
        this.teacherAccounts = new ArrayList<>();
        this.studentAccounts = new ArrayList<>();
        this.parentAccounts = new ArrayList<>();
        this.courses = new ArrayList<>();
        this.roomSchedules = new HashMap<>();
        this.departments = new HashMap<>();
    }

    // Getters and Setters
    public String getCenterId() {
        return centerId;
    }

    public void setCenterId(String centerId) {
        this.centerId = centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagerNumber() {
        return managerNumber;
    }

    public void setManagerNumber(String managerNumber) {
        this.managerNumber = managerNumber;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }

    // Account Management Methods

    /**
     * Add an admin account to the center
     * @param admin The admin to add
     */
    public void addAdminAccount(Admin admin) {
        adminAccounts.add(admin);
    }

    /**
     * Remove an admin account from the center
     * @param admin The admin to remove
     * @return true if removed, false if not found
     */
    public boolean removeAdminAccount(Admin admin) {
        return adminAccounts.remove(admin);
    }

    /**
     * Add a teacher account to the center
     * @param teacher The teacher to add
     */
    public void addTeacherAccount(Teacher teacher) {
        teacherAccounts.add(teacher);
    }

    /**
     * Remove a teacher account from the center
     * @param teacher The teacher to remove
     * @return true if removed, false if not found
     */
    public boolean removeTeacherAccount(Teacher teacher) {
        return teacherAccounts.remove(teacher);
    }

    /**
     * Add a student account to the center
     * @param student The student to add
     */
    public void addStudentAccount(Student student) {
        studentAccounts.add(student);
    }

    /**
     * Remove a student account from the center
     * @param student The student to remove
     * @return true if removed, false if not found
     */
    public boolean removeStudentAccount(Student student) {
        return studentAccounts.remove(student);
    }

    /**
     * Add a parent account to the center
     * @param parent The parent to add
     */
    public void addParentAccount(Parent parent) {
        parentAccounts.add(parent);
    }

    /**
     * Remove a parent account from the center
     * @param parent The parent to remove
     * @return true if removed, false if not found
     */
    public boolean removeParentAccount(Parent parent) {
        return parentAccounts.remove(parent);
    }

    // Course and Department Management Methods

    /**
     * Add a course to the center
     * @param course The course to add
     */
    public void addCourse(Course course) {
        courses.add(course);
    }

    /**
     * Remove a course from the center
     * @param course The course to remove
     * @return true if removed, false if not found
     */
    public boolean removeCourse(Course course) {
        return courses.remove(course);
    }

    /**
     * Add a room schedule to the center
     * @param roomSchedule The room schedule to add
     */
    public void addRoomSchedule(RoomSchedule roomSchedule) {
        roomSchedules.put(roomSchedule.getRoomId(), roomSchedule);
    }

    /**
     * Get a room schedule by room ID
     * @param roomId The room ID
     * @return The room schedule or null if not found
     */
    public RoomSchedule getRoomSchedule(String roomId) {
        return roomSchedules.get(roomId);
    }

    /**
     * Add a department to the center
     * @param department The department to add
     */
    public void addDepartment(Department department) {
        departments.put(department.getRoomID(), department);
    }

    /**
     * Get a department by ID
     * @param departmentId The department ID
     * @return The department or null if not found
     */
    public Department getDepartment(String departmentId) {
        return departments.get(departmentId);
    }

    // Center Information Methods

    /**
     * Get the total number of students in the center
     * @return The total number of students
     */
    public int getTotalStudents() {
        return studentAccounts.size();
    }

    /**
     * Get the number of active students (students currently enrolled in courses)
     * @return The number of active students
     */
    public int getActiveStudents() {
        int activeCount = 0;
        for (Student student : studentAccounts) {
            // A student is considered active if they are enrolled in at least one course
            // This would need to be implemented based on your actual Student class
            if (isStudentActive(student)) {
                activeCount++;
            }
        }
        return activeCount;
    }

    /**
     * Helper method to check if a student is active
     * @param student The student to check
     * @return true if active, false otherwise
     */
    private boolean isStudentActive(Student student) {
        // Implementation depends on your Student class structure
        // For example, if Student has a list of courses:
        // return !student.getCourses().isEmpty();

        // Placeholder implementation
        return true;
    }

    /**
     * Get students with expired enrollment
     * @return List of students with expired enrollment
     */
    public List<Student> getStudentsOutDate() {
        List<Student> outDateStudents = new ArrayList<>();

        for (Student student : studentAccounts) {
            // Check if the student's enrollment is expired
            // This would need to be implemented based on your actual Student class
            if (isStudentOutDate(student)) {
                outDateStudents.add(student);
            }
        }

        return outDateStudents;
    }

    /**
     * Helper method to check if a student's enrollment is expired
     * @param student The student to check
     * @return true if expired, false otherwise
     */
    private boolean isStudentOutDate(Student student) {
        // Implementation depends on your Student class structure
        // For example, if Student has an enrollment end date:
        // return student.getEnrollmentEndDate().isBefore(LocalDate.now());

        // Placeholder implementation
        return false;
    }

    /**
     * Get the total number of classes/courses in the center
     * @return The total number of classes
     */
    public int getTotalClasses() {
        return courses.size();
    }

    /**
     * Get the number of available classes (classes that are not full)
     * @return The number of available classes
     */
    public int getAvailableClasses() {
        int availableCount = 0;
        for (Course course : courses) {
            // A class is considered available if it has space for more students
            // This would need to be implemented based on your actual Course class
            if (isClassAvailable(course)) {
                availableCount++;
            }
        }
        return availableCount;
    }

    /**
     * Helper method to check if a class is available
     * @param course The course to check
     * @return true if available, false otherwise
     */
    private boolean isClassAvailable(Course course) {
        // Implementation depends on your Course class structure
        // For example, if Course has a capacity and current enrollment:
        // return course.getTotalCurrentStudent() < course.getCapacity();

        // Using the getTotalCurrentStudent method from your Course class
        // Assuming a default capacity of 30 students per course
        return course.getTotalCurrentStudent() < 30;
    }

    /**
     * Get the number of unavailable classes (classes that are full)
     * @return The number of unavailable classes
     */
    public int getUnavailableClasses() {
        return getTotalClasses() - getAvailableClasses();
    }

    /**
     * Get all admin accounts
     * @return List of admin accounts
     */
    public List<Admin> getAdminAccounts() {
        return new ArrayList<>(adminAccounts);
    }

    /**
     * Get all teacher accounts
     * @return List of teacher accounts
     */
    public List<Teacher> getTeacherAccounts() {
        return new ArrayList<>(teacherAccounts);
    }

    /**
     * Get all student accounts
     * @return List of student accounts
     */
    public List<Student> getStudentAccounts() {
        return new ArrayList<>(studentAccounts);
    }

    /**
     * Get all parent accounts
     * @return List of parent accounts
     */
    public List<Parent> getParentAccounts() {
        return new ArrayList<>(parentAccounts);
    }

    /**
     * Get all courses
     * @return List of courses
     */
    public List<Course> getCourses() {
        return new ArrayList<>(courses);
    }

    @Override
    public String toString() {
        return "Center [" + centerId + "] " + centerName +
                "\nTotal Students: " + getTotalStudents() +
                "\nActive Students: " + getActiveStudents() +
                "\nTotal Classes: " + getTotalClasses() +
                "\nAvailable Classes: " + getAvailableClasses();
    }
}
