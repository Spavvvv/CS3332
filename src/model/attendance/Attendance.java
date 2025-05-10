package src.model.attendance;

import src.model.person.Student;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import src.model.ClassSession;

/**
 * Model lưu trữ thông tin điểm danh của học sinh
 * Mỗi bản ghi Attendance thể hiện trạng thái điểm danh của một học sinh trong một buổi học cụ thể
 */
public class Attendance {
    private long id;
    private Student student;
    private ClassSession session;
    private boolean present;
    private String note;
    private boolean called;
    private boolean hasPermission;
    private LocalDateTime checkInTime;
    private LocalDateTime recordTime;

    /**
     * Constructor mặc định
     */
    public Attendance() {
        this.present = false;
        this.note = "";
        this.called = false;
        this.hasPermission = false;
        this.recordTime = LocalDateTime.now();
    }

    /**
     * Constructor với Student và ClassSession
     *
     * @param student Học sinh được điểm danh
     * @param session Buổi học diễn ra
     */
    public Attendance(Student student, ClassSession session) {
        this();
        this.student = student;
        this.session = session;
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
     */
    public Attendance(long id, Student student, ClassSession session, boolean present,
                      String note, boolean called, boolean hasPermission,
                      LocalDateTime checkInTime, LocalDateTime recordTime) {
        this.id = id;
        this.student = student;
        this.session = session;
        this.present = present;
        this.note = note;
        this.called = called;
        this.hasPermission = hasPermission;
        this.checkInTime = checkInTime;
        this.recordTime = recordTime != null ? recordTime : LocalDateTime.now();
    }

    // Getters và Setters

    /**
     * Lấy ID của bản ghi điểm danh
     *
     * @return ID bản ghi
     */
    public long getId() {
        return id;
    }

    /**
     * Thiết lập ID của bản ghi điểm danh
     *
     * @param id ID bản ghi
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Lấy đối tượng Student được điểm danh
     *
     * @return Học sinh
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Thiết lập đối tượng Student được điểm danh
     *
     * @param student Học sinh
     */
    public void setStudent(Student student) {
        this.student = student;
    }

    /**
     * Lấy mã học sinh
     *
     * @return Mã học sinh
     */
    public String getStudentId() {
        return student != null ? student.getId() : null;
    }

    /**
     * Lấy đối tượng ClassSession
     *
     * @return Buổi học
     */
    public ClassSession getSession() {
        return session;
    }

    /**
     * Thiết lập đối tượng ClassSession
     *
     * @param session Buổi học
     */
    public void setSession(ClassSession session) {
        this.session = session;
    }

    /**
     * Lấy ID của buổi học
     *
     * @return ID buổi học
     */
    public long getSessionId() {
        return session != null ? session.getId() : 0;
    }

    /**
     * Lấy ngày diễn ra buổi học
     *
     * @return Ngày diễn ra
     */
    public LocalDate getDate() {
        return session != null ? session.getDate() : null;
    }

    /**
     * Kiểm tra học sinh có mặt hay không
     *
     * @return true nếu học sinh có mặt, ngược lại là false
     */
    public boolean isPresent() {
        return present;
    }

    /**
     * Thiết lập trạng thái có mặt của học sinh
     *
     * @param present true nếu học sinh có mặt, ngược lại là false
     */
    public void setPresent(boolean present) {
        this.present = present;
        if (present && this.checkInTime == null) {
            this.checkInTime = LocalDateTime.now();
        }
    }

    /**
     * Lấy ghi chú điểm danh
     *
     * @return Ghi chú điểm danh
     */
    public String getNote() {
        return note;
    }

    /**
     * Thiết lập ghi chú điểm danh
     *
     * @param note Ghi chú điểm danh
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Kiểm tra đã gọi điện thông báo hay chưa
     *
     * @return true nếu đã gọi điện, ngược lại là false
     */
    public boolean isCalled() {
        return called;
    }

    /**
     * Thiết lập trạng thái đã gọi điện thông báo
     *
     * @param called true nếu đã gọi điện, ngược lại là false
     */
    public void setCalled(boolean called) {
        this.called = called;
    }

    /**
     * Kiểm tra học sinh có phép hay không
     *
     * @return true nếu có phép, ngược lại là false
     */
    public boolean hasPermission() {
        return hasPermission;
    }

    /**
     * Thiết lập trạng thái có phép của học sinh
     *
     * @param hasPermission true nếu có phép, ngược lại là false
     */
    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    /**
     * Lấy thời gian học sinh điểm danh
     *
     * @return Thời gian điểm danh
     */
    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    /**
     * Thiết lập thời gian học sinh điểm danh
     *
     * @param checkInTime Thời gian điểm danh
     */
    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    /**
     * Lấy thời gian ghi nhận bản ghi
     *
     * @return Thời gian ghi nhận
     */
    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    /**
     * Thiết lập thời gian ghi nhận bản ghi
     *
     * @param recordTime Thời gian ghi nhận
     */
    public void setRecordTime(LocalDateTime recordTime) {
        this.recordTime = recordTime;
    }

    /**
     * Lấy tên học sinh
     *
     * @return Tên học sinh
     */
    public String getStudentName() {
        return student != null ? student.getName() : "";
    }

    /**
     * Lấy tên phụ huynh
     *
     * @return Tên phụ huynh
     */
    public String getParentName() {
        return student != null && student.getParent() != null ?
                student.getParent().getName() : "";
    }

    /**
     * Lấy số điện thoại phụ huynh
     *
     * @return Số điện thoại phụ huynh
     */
    public String getParentContact() {
        return student != null && student.getParent() != null ?
                student.getParent().getContactNumber() : "";
    }

    /**
     * Kiểm tra tính hợp lệ của bản ghi điểm danh
     *
     * @return true nếu bản ghi hợp lệ, ngược lại là false
     */
    public boolean isValid() {
        return student != null && session != null;
    }

    /**
     * Tạo một thông báo vắng mặt dựa trên thông tin điểm danh
     *
     * @return Chuỗi thông báo vắng mặt
     */
    public String generateAbsenceNotification() {
        if (present) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Thông báo: Học sinh ");
        sb.append(getStudentName());
        sb.append(" đã vắng mặt trong buổi học ");
        sb.append(session != null ? session.getCourseName() : "");
        sb.append(" ngày ");
        sb.append(session != null ? session.getFormattedDate() : "");

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
        return id == that.id &&
                present == that.present &&
                called == that.called &&
                hasPermission == that.hasPermission &&
                Objects.equals(student, that.student) &&
                Objects.equals(session, that.session) &&
                Objects.equals(note, that.note) &&
                Objects.equals(checkInTime, that.checkInTime) &&
                Objects.equals(recordTime, that.recordTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, student, session, present, note, called, hasPermission, checkInTime, recordTime);
    }

    @Override
    public String toString() {
        return "Attendance{" +
                "id=" + id +
                ", student=" + (student != null ? student.getId() : "null") +
                ", session=" + (session != null ? session.getId() : "null") +
                ", present=" + present +
                ", note='" + note + '\'' +
                ", called=" + called +
                ", hasPermission=" + hasPermission +
                ", checkInTime=" + checkInTime +
                ", recordTime=" + recordTime +
                '}';
    }

    public void setDate(LocalDate now) {
        if (session != null) {
            session.setDate(now);
        }
    }

    /**
     * Kiểm tra đã thông báo hay chưa (alias cho isCalled)
     * @return true nếu đã thông báo, ngược lại là false
     */
    public boolean isNotified() {
        return called;
    }

    /**
     * Thiết lập trạng thái đã thông báo
     * @param notified true nếu đã thông báo, ngược lại là false
     */
    public void setNotified(boolean notified) {
        this.called = notified;
    }

    /**
     * Thiết lập ID của buổi học
     * @param sessionId ID buổi học
     */
    public void setSessionId(long sessionId) {
        if (this.session == null) {
            this.session = new ClassSession();
        }
        this.session.setId(sessionId);
    }

    /**
     * Lấy loại vắng mặt của học sinh
     *
     * @return Loại vắng mặt (có phép/không phép)
     */
    public String getAbsenceType() {
        if (present) {
            return "Có mặt";
        } else if (hasPermission) {
            return "Vắng có phép";
        } else {
            return "Vắng không phép";
        }
    }

    /**
     * Kiểm tra có phải vắng mặt hay không
     *
     * @return true nếu học sinh vắng mặt (không có mặt), ngược lại là false
     */
    public boolean isAbsent() {
        return !present;
    }

    /**
     * Lấy trạng thái gọi điện dưới dạng văn bản
     *
     * @return Trạng thái gọi điện (Đã gọi/Chưa gọi)
     */
    public String getCallStatus() {
        return called ? "Đã gọi" : "Chưa gọi";
    }

    /**
     * Lấy thông tin lớp học
     *
     * @return Tên lớp học
     */
    public String getClassName() {
        return session != null ? session.getClassName() : "";
    }

    /**
     * Lấy thông tin môn học
     *
     * @return Tên môn học
     */
    public String getSubjectName() {
        return session != null ? session.getCourseName() : "";
    }

    /**
     * Lấy thông tin ngày vắng dưới dạng văn bản có định dạng
     *
     * @return Ngày vắng dưới dạng chuỗi đã định dạng
     */
    public String getFormattedDate() {
        return session != null ? session.getFormattedDate() : "";
    }

    /**
     * Lấy ngày trong tuần của buổi học
     *
     * @return Ngày trong tuần (Thứ 2, Thứ 3, v.v.)
     */
    public String getDayOfWeek() {
        return session != null ? session.getDayOfWeek() : "";
    }

    public void setStudentId(String studentId) {
        student.setId(studentId);
    }

    public void setStudentName(String studentName) {
        student.setName(studentName);
    }
}
