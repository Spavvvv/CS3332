package src.model.person;

// Parent.java
import java.util.ArrayList;
import java.util.List;

public class Parent extends Person {
    private List<Student> children;
    private String relationship; // e.g., "Father", "Mother", "Guardian"

    public Parent(String id, String name, String gender, String contactNumber, String birthday,
                  String email, String relationship) {
        super(id, name, gender, contactNumber, birthday, email);
        this.relationship = relationship;
        this.children = new ArrayList<>();
    }

    public Parent() {}

    @Override
    public Role getRole() {
        return Role.PARENT;
    }

    // Parent-specific methods


    // Getters and Setters
    public List<Student> getChildren() {
        return children;
    }

    public void setChildren(List<Student> children) {
        this.children = children;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", Role: " + getRole() +
                ", Relationship: " + relationship +
                ", Number of Children: " + (children != null ? children.size() : 0);
    }
}

