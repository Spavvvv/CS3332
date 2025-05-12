// Model Class: AbsenceRecord.java
package src.model.absence;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.ImageView;

/**
 * Model class representing an absence record
 */
public class AbsenceRecord {
    private final String id;
    private final ImageView image;
    private final String name;
    private final String className;
    private final String date;
    private final String attendance;
    private final String note;
    private final BooleanProperty called = new SimpleBooleanProperty();
    private final BooleanProperty approved = new SimpleBooleanProperty();

    public AbsenceRecord(String id, ImageView image, String name, String className,
                         String date, String attendance, String note,
                         boolean called, boolean approved) {
        this.id = id;
        this.image = image;
        this.name = name;
        this.className = className;
        this.date = date;
        this.attendance = attendance;
        this.note = note;
        this.called.set(called);
        this.approved.set(approved);
    }

    // Getters and Setters
    public String getId() { return id; }

    public ImageView getImage() { return image; }

    public String getName() { return name; }

    public String getClassName() { return className; }

    public String getDate() { return date; }

    public String getAttendance() { return attendance; }

    public String getNote() { return note; }

    public boolean isCalled() { return called.get(); }

    public BooleanProperty calledProperty() { return called; }

    public void setCalled(boolean called) { this.called.set(called); }

    public boolean isApproved() { return approved.get(); }

    public BooleanProperty approvedProperty() { return approved; }

    public void setApproved(boolean approved) { this.approved.set(approved); }
}