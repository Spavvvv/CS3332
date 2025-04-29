package src.model;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
/**
 Đối tượng đại diện cho một buổi học
 */
public class ClassSession {
    private long id;
    private String courseName;
    private String teacher;
    private String room;
    private LocalDate date;
    private String timeSlot;
    public ClassSession(long id, String courseName, String teacher, String room, LocalDate date, String timeSlot) {
        this.id = id;
        this.courseName = courseName;
        this.teacher = teacher;
        this.room = room;
        this.date = date;
        this.timeSlot = timeSlot;
    }

    public ClassSession(){

    }
    // Getters and setters
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getCourseName() {
        return courseName;
    }
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    public String getTeacher() {
        return teacher;
    }
    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
    public String getRoom() {
        return room;
    }
    public void setRoom(String room) {
        this.room = room;
    }
    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public String getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }
    /**
     Kiểm tra xem buổi học có diễn ra trong khoảng thời gian từ fromDate đến toDate hay không

     @param fromDate ngày bắt đầu
     @param toDate ngày kết thúc
     @return true nếu buổi học nằm trong khoảng thời gian, false nếu không
     */
    public boolean isInDateRange(LocalDate fromDate, LocalDate toDate) {
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    /**
     Trả về chuỗi định dạng ngày theo mẫu dd/MM/yyyy

     @return chuỗi biểu diễn ngày
     */
    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    /**
     Trả về thông tin tổng quan về buổi học

     @return chuỗi thông tin buổi học
     */
    public String getSummary() {
        return courseName + " - " + teacher + " - " + room;
    }

    /**
     Trả về thông tin chi tiết về buổi học

     @return chuỗi thông tin chi tiết
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Khóa học: ").append(courseName).append("\n");
        sb.append("Giảng viên: ").append(teacher).append("\n");
        sb.append("Phòng: ").append(room).append("\n");
        sb.append("Ngày: ").append(getFormattedDate()).append("\n");
        sb.append("Khung giờ: ").append(timeSlot);
        return sb.toString();
    }

    /**
     Kiểm tra xem buổi học có phải của giáo viên cụ thể hay không

     @param teacherName tên giáo viên cần kiểm tra
     @return true nếu đúng là buổi học của giáo viên, false nếu không
     */
    public boolean isTaughtBy(String teacherName) {
        return teacher != null && teacher.equals(teacherName);
    }

    /**
     Trả về ngày trong tuần của buổi học (thứ 2, thứ 3,...)

     @return chuỗi biểu diễn ngày trong tuần
     */
    public String getDayOfWeek() {
        int dayValue = date.getDayOfWeek().getValue();
        switch (dayValue) {
            case 1: return "Thứ 2";
            case 2: return "Thứ 3";
            case 3: return "Thứ 4";
            case 4: return "Thứ 5";
            case 5: return "Thứ 6";
            case 6: return "Thứ 7";
            case 7: return "Chủ nhật";
            default: return "";
        }
    }

    /**
     * Trả về tên của lớp học (alias cho getCourseName cho tương thích với code hiện tại)
     *
     * @return tên của lớp học
     */
    public String getClassName() {
        return courseName;
    }

    /**
     So sánh buổi học với một đối tượng khác
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassSession that = (ClassSession) o;
        return id == that.id &&
                Objects.equals(courseName, that.courseName) &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(room, that.room) &&
                Objects.equals(date, that.date) &&
                Objects.equals(timeSlot, that.timeSlot);
    }

    /**
     Tính toán mã băm cho đối tượng
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, courseName, teacher, room, date, timeSlot);
    }

    /**
     Trả về chuỗi biểu diễn cho đối tượng
     */
    @Override
    public String toString() {
        return "ClassSession{" +
                "id=" + id +
                ", courseName='" + courseName + '\'' +
                ", teacher='" + teacher + '\'' +
                ", room='" + room + '\'' +
                ", date=" + date +
                ", timeSlot='" + timeSlot + '\'' +
                '}';
    }


    /**
     Tạo một bản sao của đối tượng

     @return đối tượng ClassSession mới có cùng thuộc tính
     */
    public ClassSession copy() {
        return new ClassSession(id, courseName, teacher, room, date, timeSlot);
    }
}


