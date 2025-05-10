package src.model.person;

// Abstract class Person.java
public abstract class Person {
    // Common attributes
    protected String id;
    protected String name;
    protected String gender;
    protected String contactNumber;
    protected String birthday;
    protected int age;
    protected String status;
    protected String email;

    // Constructor
    public Person(String id, String name, String gender, String contactNumber, String birthday, String email) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.birthday = birthday;
        calculateAge(); // Calculate age based on birthday
        this.status = "Active";
        this.email = email;
    }

    public Person() {}

    // Abstract method that all subclasses must implement
    public abstract Role getRole();

    // Common methods
    public void calculateAge() {
        // Logic to calculate age from birthday
        // This is a simplified example
        if (birthday != null && !birthday.isEmpty()) {
            // In a real implementation, parse birthday and calculate age
            this.age = 0; // Placeholder
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.gender = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
        calculateAge(); // Recalculate age when birthday changes
    }

    public int getAge() {
        return age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ID: " + id +
                ", Name: " + name +
                ", Gender: " + gender +
                ", Contact: " + contactNumber +
                ", Birthday: " + birthday +
                ", Age: " + age +
                ", Status: " + status;
    }
}
