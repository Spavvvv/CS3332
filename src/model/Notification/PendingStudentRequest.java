package src.model.Notification;
import java.time.LocalDate;

public class PendingStudentRequest {
    private String requestId; // ID duy nhất cho yêu cầu
    private String studentId; // ID của học viên
    private String studentName; // Tên học viên
    private String studentGender; // Giới tính học viên
    private String studentContact; // Số liên lạc học viên
    private LocalDate studentBirthday; // Ngày sinh học viên
    private String teacherId; // ID của Teacher gửi yêu cầu
    private boolean isApproved; // Trạng thái yêu cầu (true = đã phê duyệt, false = chờ xử lý)

    // Constructor
    public PendingStudentRequest(String requestId, String studentId, String studentName,
                                 String studentGender, String studentContact,
                                 LocalDate studentBirthday, String teacherId) {
        this.requestId = requestId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentGender = studentGender;
        this.studentContact = studentContact;
        this.studentBirthday = studentBirthday;
        this.teacherId = teacherId;
        this.isApproved = false; // Mặc định là chờ xử lý
    }

    // Getters
    public String getRequestId() {
        return requestId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentGender() {
        return studentGender;
    }

    public String getStudentContact() {
        return studentContact;
    }

    public LocalDate getStudentBirthday() {
        return studentBirthday;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public boolean isApproved() {
        return isApproved;
    }

    // Setters
    public void setApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }

    // Utility method: Hiển thị thông tin yêu cầu (Cho debug hoặc log)
    @Override
    public String toString() {
        return "PendingStudentRequest{" +
                "requestId='" + requestId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", studentName='" + studentName + '\'' +
                ", studentGender='" + studentGender + '\'' +
                ", studentContact='" + studentContact + '\'' +
                ", studentBirthday=" + studentBirthday +
                ", teacherId='" + teacherId + '\'' +
                ", isApproved=" + isApproved +
                '}';
    }
}