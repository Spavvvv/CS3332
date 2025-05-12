// Admin.java
package src.model.person;

public class Admin extends Person {
    private String accessLevel;

    public Admin(String id, String name, String gender, String contactNumber, String birthday, String email,
                  String accessLevel) {
        super(id, name, gender, contactNumber, birthday,email);
        this.accessLevel = accessLevel;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
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
                ", Admin ID: " + id +
                ", Access Level: " + accessLevel;
    }
}
