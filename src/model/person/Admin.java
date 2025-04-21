// Admin.java
package src.model.person;

public class Admin extends Person {
    private String adminId;
    private String department;
    private String accessLevel;

    public Admin(String id, String name, String gender, String contactNumber, String birthday,
                 String adminId, String department, String accessLevel) {
        super(id, name, gender, contactNumber, birthday);
        this.adminId = adminId;
        this.department = department;
        this.accessLevel = accessLevel;
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    // Admin-specific methods
    public boolean hasAccess(String requiredLevel) {
        // Simple access level check (could be more sophisticated)
        if (accessLevel.equals("Full")) {
            return true;
        } else if (accessLevel.equals("Medium")) {
            return !requiredLevel.equals("Full");
        } else {
            return requiredLevel.equals("Basic");
        }
    }

    // Getters and Setters
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", Admin ID: " + adminId +
                ", Department: " + department +
                ", Access Level: " + accessLevel;
    }
}
