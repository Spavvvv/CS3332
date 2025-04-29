package src.model;
import src.model.person.Student;
import src.model.system.course.Course;
import src.model.system.course.CourseDate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    private LocalTime startTime; // Thời gian bắt đầu buổi học
    private LocalTime endTime;   // Thời gian kết thúc buổi học
    private long classId; // ID của lớp học mà buổi học này thuộc về
    private List<Student> students; // Danh sách học sinh tham gia buổi học
    private CourseDate courseDate; // Thông tin về ngày của khóa học

    public ClassSession(long id, String courseName, String teacher, String room, LocalDate date, String timeSlot) {
        this.id = id;
        this.courseName = courseName;
        this.teacher = teacher;
        this.room = room;
        this.date = date;
        this.timeSlot = timeSlot;
        this.students = new ArrayList<>();
    }


    public ClassSession(long id, String courseName, String teacher, String room, LocalDate date, String timeSlot, long classId) {
        this(id, courseName, teacher, room, date, timeSlot);
        this.classId = classId;
    }


    public ClassSession(long id, String courseName, String teacher, String room, LocalDate date,
                        LocalTime startTime, LocalTime endTime, long classId) {
        this.id = id;
        this.courseName = courseName;
        this.teacher = teacher;
        this.room = room;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeSlot = formatTimeSlot(startTime, endTime);
        this.classId = classId;
        this.students = new ArrayList<>();
    }


    public ClassSession(){
        this.students = new ArrayList<>();
    }


    /**
     * Tạo chuỗi biểu diễn khung giờ từ thời gian bắt đầu và kết thúc
     * @param start thời gian bắt đầu
     * @param end thời gian kết thúc
     * @return chuỗi biểu diễn khung giờ
     */
    private String formatTimeSlot(LocalTime start, LocalTime end) {
        if (start == null || end == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return start.format(formatter) + " - " + end.format(formatter);
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
     * Lấy thời gian bắt đầu buổi học
     * @return thời gian bắt đầu
     */
    public LocalTime getStartTime() {
        return startTime;
    }


    /**
     * Thiết lập thời gian bắt đầu buổi học
     * @param startTime thời gian bắt đầu
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
        updateTimeSlot();
    }


    /**
     * Lấy thời gian kết thúc buổi học
     * @return thời gian kết thúc
     */
    public LocalTime getEndTime() {
        return endTime;
    }


    /**
     * Thiết lập thời gian kết thúc buổi học
     * @param endTime thời gian kết thúc
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
        updateTimeSlot();
    }


    /**
     * Cập nhật timeSlot dựa trên thời gian bắt đầu và kết thúc
     */
    private void updateTimeSlot() {
        if (startTime != null && endTime != null) {
            this.timeSlot = formatTimeSlot(startTime, endTime);
        }
    }


    /**
     * Lấy thông tin về ngày của khóa học
     * @return thông tin ngày khóa học
     */
    public CourseDate getCourseDate() {
        return courseDate;
    }


    /**
     * Thiết lập thông tin về ngày của khóa học
     * @param courseDate thông tin ngày khóa học
     */
    public void setCourseDate(CourseDate courseDate) {
        this.courseDate = courseDate;
    }


    /**
     * Lấy danh sách học sinh tham gia buổi học
     * @return danh sách học sinh
     */
    public List<Student> getStudents() {
        return students;
    }


    /**
     * Thiết lập danh sách học sinh tham gia buổi học
     * @param students danh sách học sinh
     */
    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
    }


    /**
     * Lấy ID của lớp học mà buổi học này thuộc về
     * @return ID của lớp học
     */
    public long getClassId() {
        return classId;
    }


    /**
     * Thiết lập ID của lớp học mà buổi học này thuộc về
     * @param classId ID của lớp học
     */
    public void setClassId(long classId) {
        this.classId = classId;
    }


    /**
     * Thêm một học sinh vào buổi học
     * @param student học sinh cần thêm
     * @return true nếu thêm thành công, false nếu học sinh đã tồn tại trong danh sách
     */
    public boolean addStudent(Student student) {
        if (student == null) return false;


        // Kiểm tra xem học sinh đã tồn tại trong danh sách chưa
        for (Student s : students) {
            if (s.getId().equals(student.getId())) {
                return false; // Học sinh đã tồn tại
            }
        }


        return students.add(student);
    }


    /**
     * Xóa một học sinh khỏi buổi học
     * @param studentId ID của học sinh cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy học sinh
     */
    public boolean removeStudent(String studentId) {
        if (studentId == null) return false;


        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getId().equals(studentId)) {
                students.remove(i);
                return true;
            }
        }


        return false;
    }


    /**
     * Kiểm tra xem học sinh có tham gia buổi học hay không
     * @param studentId ID của học sinh cần kiểm tra
     * @return true nếu học sinh tham gia, false nếu không
     */
    public boolean hasStudent(String studentId) {
        if (studentId == null) return false;


        for (Student student : students) {
            if (student.getId().equals(studentId)) {
                return true;
            }
        }


        return false;
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
     * Kiểm tra xem buổi học có diễn ra trong khoảng thời gian của CourseDate không
     * @return true nếu buổi học nằm trong khoảng thời gian khóa học, false nếu không
     */
    public boolean isWithinCourseDate() {
        if (courseDate == null) return false;
        return courseDate.isDateWithinRange(date);
    }


    /**
     * Kiểm tra xem buổi học có xung đột thời gian với buổi học khác hay không
     * @param other buổi học khác cần kiểm tra
     * @return true nếu có xung đột, false nếu không
     */
    public boolean conflicts(ClassSession other) {
        // Kiểm tra xem có cùng ngày không
        if (!this.date.equals(other.date)) {
            return false;
        }

        // Kiểm tra xem có xung đột thời gian không
        if (this.startTime == null || this.endTime == null ||
                other.startTime == null || other.endTime == null) {
            return this.timeSlot.equals(other.timeSlot); // Nếu không có thời gian, kiểm tra timeSlot
        }

        // Kiểm tra xung đột thời gian
        // Xung đột khi: thời gian bắt đầu hoặc kết thúc nằm trong khoảng thời gian của buổi học khác
        return !this.startTime.isAfter(other.endTime) && !this.endTime.isBefore(other.startTime);
    }


    /**
     * Kiểm tra xem buổi học đã qua hay chưa
     * @return true nếu buổi học đã qua, false nếu chưa
     */
    public boolean isPast() {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            return true;
        }
        if (date.isEqual(today) && endTime != null) {
            return LocalTime.now().isAfter(endTime);
        }
        return false;
    }


    /**
     * Kiểm tra xem buổi học đang diễn ra hay không
     * @return true nếu buổi học đang diễn ra, false nếu không
     */
    public boolean isOngoing() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (!date.isEqual(today)) {
            return false;
        }

        if (startTime == null || endTime == null) {
            return false;
        }

        return !now.isBefore(startTime) && !now.isAfter(endTime);
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
        String time = (startTime != null && endTime != null)
                ? startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "-" + endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                : timeSlot;

        return courseName + " - " + teacher + " - " + room + " - " + time;
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

        if (startTime != null && endTime != null) {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            sb.append("Thời gian bắt đầu: ").append(startTime.format(timeFormatter)).append("\n");
            sb.append("Thời gian kết thúc: ").append(endTime.format(timeFormatter)).append("\n");
        } else {
            sb.append("Khung giờ: ").append(timeSlot).append("\n");
        }

        sb.append("Số học sinh: ").append(students.size());
        return sb.toString();
    }


    /**
     * Tính toán thời lượng của buổi học theo phút
     * @return thời lượng buổi học theo phút, -1 nếu không có thông tin thời gian
     */
    public int getDurationMinutes() {
        if (startTime == null || endTime == null) return -1;

        int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
        int endMinutes = endTime.getHour() * 60 + endTime.getMinute();

        return endMinutes - startMinutes;
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
                classId == that.classId &&
                Objects.equals(courseName, that.courseName) &&
                Objects.equals(teacher, that.teacher) &&
                Objects.equals(room, that.room) &&
                Objects.equals(date, that.date) &&
                Objects.equals(timeSlot, that.timeSlot) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime);
    }


    /**
     Tính toán mã băm cho đối tượng
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, courseName, teacher, room, date, timeSlot, startTime, endTime, classId);
    }


    /**
     Trả về chuỗi biểu diễn cho đối tượng
     */
    @Override
    public String toString() {
        String timeInfo = (startTime != null && endTime != null)
                ? startTime + " - " + endTime
                : timeSlot;

        return "ClassSession{" +
                "id=" + id +
                ", courseName='" + courseName + '\'' +
                ", teacher='" + teacher + '\'' +
                ", room='" + room + '\'' +
                ", date=" + date +
                ", time='" + timeInfo + '\'' +
                ", classId=" + classId +
                ", studentCount=" + (students != null ? students.size() : 0) +
                '}';
    }


    /**
     Tạo một bản sao của đối tượng


     @return đối tượng ClassSession mới có cùng thuộc tính
     */
    public ClassSession copy() {
        ClassSession copy = new ClassSession();
        copy.setId(id);
        copy.setCourseName(courseName);
        copy.setTeacher(teacher);
        copy.setRoom(room);
        copy.setDate(date);
        copy.setTimeSlot(timeSlot);
        copy.setStartTime(startTime);
        copy.setEndTime(endTime);
        copy.setClassId(classId);
        copy.setCourseDate(courseDate);

        if (students != null) {
            copy.setStudents(new ArrayList<>(students));
        }

        return copy;
    }


//    public String getSessionNumber() {
//        return
//    }

//    public String getTotalSessions() {
//
//    }
}
