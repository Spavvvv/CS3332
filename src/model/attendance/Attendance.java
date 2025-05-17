
package src.model.attendance;

import src.model.person.Student;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import src.model.ClassSession;

/**
 * Model lưu trữ thông tin điểm danh của học sinh
 * Mỗi bản ghi Attendance thể hiện trạng thái điểm danh của một học sinh trong một buổi học cụ thể
 */
public class Attendance {
    private String id;
    private Student student;
    private ClassSession session;
    private boolean present;
    private String note;
    private boolean called;
    private boolean hasPermission;
    private LocalDateTime checkInTime;
    private LocalDateTime recordTime;
    private String status;
    private LocalDate absenceDate; // The specific date of this attendance record

    /**
     * Constructor mặc định
     */
    public Attendance() {
        this.present = false;
        this.note = "";
        this.called = false;
        this.hasPermission = false;
        this.recordTime = LocalDateTime.now();
        this.student = new Student(); // Consider if default instantiation is always desired
        this.session = new ClassSession(); // Consider if default instantiation is always desired
        this.status = "";
        this.absenceDate = null; // Initialize absenceDate
    }

    /**
     * Constructor với Student và ClassSession
     *
     * @param student Học sinh được điểm danh
     * @param session Buổi học diễn ra
     */
    public Attendance(Student student, ClassSession session) {
        this(); // Call default constructor for initializations
        this.student = student;
        this.session = session;
        if (session != null) {
            this.absenceDate = session.getDate(); // Set absenceDate from session date
        }
    }

    /**
     * Constructor đầy đủ
     *
     * @param id            ID của bản ghi điểm danh
     * @param student       Học sinh được điểm danh
     * @param session       Buổi học diễn ra
     * @param present       Trạng thái có mặt hay không
     * @param note          Ghi chú điểm danh
     * @param called        Đã gọi điện thông báo hay chưa
     * @param hasPermission Có phép hay không
     * @param checkInTime   Thời gian học sinh điểm danh (nếu có mặt)
     * @param recordTime    Thời gian ghi nhận bản ghi
     * @param status        Trạng thái của bản ghi (e.g., 'active', 'archived')
     * @param absenceDate   Ngày cụ thể của bản ghi điểm danh này
     */
    public Attendance(String id, Student student, ClassSession session, boolean present,
                      String note, boolean called, boolean hasPermission,
                      LocalDateTime checkInTime, LocalDateTime recordTime, String status,
                      LocalDate absenceDate) { // Added absenceDate parameter
        this.id = id;
        this.student = student;
        this.session = session;
        this.present = present;
        this.note = note;
        this.called = called;
        this.hasPermission = hasPermission;
        this.checkInTime = checkInTime;
        this.recordTime = recordTime != null ? recordTime : LocalDateTime.now();
        this.status = status;
        this.absenceDate = absenceDate; // Set absenceDate
    }

    // Getters và Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getStudentId() {
        return student != null ? student.getId() : null;
    }

    public ClassSession getSession() {
        return session;
    }

    public void setSession(ClassSession session) {
        this.session = session;
        // Optionally, you could update absenceDate if the session changes and absenceDate was null
        // if (this.absenceDate == null && session != null) {
        //     this.absenceDate = session.getDate();
        // }
    }

    public String getSessionId() {
        return session != null ? session.getId() : null;
    }

    /**
     * Lấy ngày diễn ra buổi học từ đối tượng ClassSession liên quan.
     *
     * @return Ngày diễn ra buổi học, hoặc null nếu không có session.
     */
    public LocalDate getDate() {
        return session != null ? session.getDate() : null;
    }

    /**
     * Lấy ngày cụ thể của bản ghi điểm danh này.
     *
     * @return Ngày điểm danh.
     */
    public LocalDate getAbsenceDate() {
        return absenceDate;
    }

    /**
     * Thiết lập ngày cụ thể cho bản ghi điểm danh này.
     *
     * @param absenceDate Ngày điểm danh.
     */
    public void setAbsenceDate(LocalDate absenceDate) {
        this.absenceDate = absenceDate;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
        if (present && this.checkInTime == null) {
            this.checkInTime = LocalDateTime.now();
        }
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    // Redundant notes getter/setter, aliasing to getNote/setNote
    public String getNotes() {
        return getNote();
    }

    public void setNotes(String note) {
        setNote(note);
    }


    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        this.called = called;
    }

    public boolean hasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStudentName() {
        return student != null ? student.getName() : "";
    }

    public String getParentName() {
        return student != null && student.getParent() != null ?
                student.getParent().getName() : "";
    }

    public String getParentContact() {
        return student != null && student.getParent() != null ?
                student.getParent().getContactNumber() : "";
    }

    public boolean isValid() {
        // Consider if studentId and sessionId should also be checked for non-null/non-empty
        return student != null && student.getId() != null && !student.getId().trim().isEmpty() &&
                session != null && session.getId() != null && !session.getId().trim().isEmpty() &&
                absenceDate != null; // An attendance record should have a specific date.
    }

    public String generateAbsenceNotification() {
        if (present) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Thông báo: Học sinh ");
        sb.append(getStudentName());
        sb.append(" đã vắng mặt trong buổi học ");
        sb.append(session != null ? session.getCourseName() : "[Không rõ môn học]");
        sb.append(" ngày ");
        // Use absenceDate for the notification if available, otherwise fall back to session date.
        LocalDate notificationDate = this.absenceDate != null ? this.absenceDate : getDate();
        if (notificationDate != null) {
            sb.append(notificationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            sb.append("[Không rõ ngày]");
        }


        if (hasPermission) {
            sb.append(" (có phép)");
        } else {
            sb.append(" (không phép)");
        }

        if (note != null && !note.isEmpty()) {
            sb.append(". Ghi chú: ").append(note);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attendance that = (Attendance) o;
        return present == that.present &&
                called == that.called &&
                hasPermission == that.hasPermission &&
                Objects.equals(id, that.id) &&
                Objects.equals(student, that.student) && // Compares student objects
                Objects.equals(session, that.session) && // Compares session objects
                Objects.equals(note, that.note) &&
                Objects.equals(checkInTime, that.checkInTime) &&
                Objects.equals(recordTime, that.recordTime) &&
                Objects.equals(status, that.status) &&
                Objects.equals(absenceDate, that.absenceDate); // Included absenceDate
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, student, session, present, note, called, hasPermission,
                checkInTime, recordTime, status, absenceDate); // Included absenceDate
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id='" + id + '\'' +
                ", studentId=" + (student != null ? student.getId() : "null") +
                ", sessionId=" + (session != null ? session.getId() : "null") +
                ", present=" + present +
                ", absenceDate=" + absenceDate + // Included absenceDate
                ", note='" + note + '\'' +
                ", called=" + called +
                ", hasPermission=" + hasPermission +
                ", checkInTime=" + checkInTime +
                ", recordTime=" + recordTime +
                ", status='" + status + '\'' +
                '}';
    }


    public boolean isNotified() {
        return called;
    }

    public void setNotified(boolean notified) {
        this.called = notified;
    }

    public void setSessionId(String sessionId) {
        if (this.session == null) {
            this.session = new ClassSession();
        }
        this.session.setId(sessionId);
    }

    public void setStudentId(String studentId) {
        if (this.student == null) {
            this.student = new Student();
        }
        this.student.setId(studentId);
    }

    public void setStudentName(String studentName) {
        if (this.student == null) {
            this.student = new Student();
        }
        this.student.setName(studentName);
    }

    public String getAbsenceType() {
        if (present) {
            return "Có mặt";
        } else if (hasPermission) {
            return "Vắng có phép";
        } else {
            return "Vắng không phép";
        }
    }

    public boolean isAbsent() {
        return !present;
    }

    public String getCallStatus() {
        return called ? "Đã gọi" : "Chưa gọi";
    }

    public String getClassName() {
        return session != null ? session.getCourseName() : "";
    }

    public String getSubjectName() {
        return session != null ? session.getCourseName() : "";
    }

    /**
     * Lấy thông tin ngày của buổi học liên quan dưới dạng văn bản có định dạng.
     * Để lấy ngày cụ thể của bản ghi điểm danh này (this.absenceDate) đã định dạng,
     * bạn nên tạo một phương thức mới hoặc định dạng this.absenceDate trực tiếp.
     * Ví dụ: public String getFormattedAbsenceDate() {
     *     return this.absenceDate != null ? this.absenceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
     * }
     * @return Ngày của buổi học dưới dạng chuỗi đã định dạng, hoặc chuỗi rỗng.
     */
    public String getFormattedDate() {
        // This method still refers to the session's date.
        return session != null ? session.getFormattedDate() : "";
    }

    /**
     * Lấy ngày cụ thể của bản ghi điểm danh này, đã được định dạng.
     * @param formatter Đối tượng DateTimeFormatter để định dạng ngày.
     * @return Ngày điểm danh đã định dạng, hoặc chuỗi rỗng nếu absenceDate là null.
     */
    public String getFormattedAbsenceDate(DateTimeFormatter formatter) {
        if (this.absenceDate == null || formatter == null) {
            return "";
        }
        return this.absenceDate.format(formatter);
    }

    /**
     * Lấy ngày cụ thể của bản ghi điểm danh này, đã được định dạng theo "dd/MM/yyyy".
     * @return Ngày điểm danh đã định dạng, hoặc chuỗi rỗng nếu absenceDate là null.
     */
    public String getFormattedAbsenceDate() {
        if (this.absenceDate == null) {
            return "";
        }
        return this.absenceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}

