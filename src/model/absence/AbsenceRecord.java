// src.model.absence.AbsenceRecord.java
package src.model.absence;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AbsenceRecord {
    private IntegerProperty displayId;
    private StringProperty studentId;
    private StringProperty studentName;
    private StringProperty className; // Class name of the session
    private ObjectProperty<LocalDate> absenceDate; // Date of the session
    private StringProperty status;
    private StringProperty note;
    private BooleanProperty called;
    private BooleanProperty approved;

    private String sessionId; // The ID of the class_session
    // isPersisted will effectively always be false if there's no absence_records table
    // It can be kept for potential future use or removed if strictly not needed.
    private boolean isPersisted;

    public AbsenceRecord(Integer displayId, String studentId, String studentName, String className,
                         LocalDate absenceDate, String status, String note,
                         boolean called, boolean approved,
                         String sessionId) { // isPersisted removed from constructor if always false
        this.displayId = new SimpleIntegerProperty(displayId == null ? 0 : displayId);
        this.studentId = new SimpleStringProperty(studentId);
        this.studentName = new SimpleStringProperty(studentName);
        this.className = new SimpleStringProperty(className);
        this.absenceDate = new SimpleObjectProperty<>(absenceDate);
        this.status = new SimpleStringProperty(status);
        this.note = new SimpleStringProperty(note);
        this.called = new SimpleBooleanProperty(called);
        this.approved = new SimpleBooleanProperty(approved);
        this.sessionId = sessionId;
        this.isPersisted = false; // Always false if no backing table for absence details
    }

    // ... (all getters, setters, and property methods remain the same) ...
    // Ensure you have these:
    public int getDisplayId() { return displayId.get(); }
    public IntegerProperty displayIdProperty() { return displayId; }
    public void setDisplayId(int displayId) { this.displayId.set(displayId); }

    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }
    public void setStudentId(String studentId) { this.studentId.set(studentId); }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }
    public void setStudentName(String studentName) { this.studentName.set(studentName); }

    public String getClassName() { return className.get(); }
    public StringProperty classNameProperty() { return className; }
    public void setClassName(String className) { this.className.set(className); }

    public LocalDate getAbsenceDate() { return absenceDate.get(); }
    public ObjectProperty<LocalDate> absenceDateProperty() { return absenceDate; }
    public void setAbsenceDate(LocalDate absenceDate) { this.absenceDate.set(absenceDate); }

    public StringProperty absenceDateFormattedProperty() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return new SimpleStringProperty(absenceDate.get() != null ? absenceDate.get().format(formatter) : "");
    }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }

    public String getNote() { return note.get(); }
    public StringProperty noteProperty() { return note; }
    public void setNote(String note) { this.note.set(note); }

    public boolean isCalled() { return called.get(); }
    public BooleanProperty calledProperty() { return called; }
    public void setCalled(boolean called) { this.called.set(called); }

    public boolean isApproved() { return approved.get(); }
    public BooleanProperty approvedProperty() { return approved; }
    public void setApproved(boolean approved) { this.approved.set(approved); }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public boolean isPersisted() { return isPersisted; } // Will always be false in this scenario
    public void setPersisted(boolean persisted) { this.isPersisted = persisted; }
}