package src.model.Notification;

import src.dao.Person.StudentDAO;
import src.model.person.Student;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentRequestService {
    // Danh sách yêu cầu chờ xử lý
    private final List<PendingStudentRequest> pendingRequests = new ArrayList<>();
    private final StudentDAO studentDAO; // Sử dụng StudentDAO

    // Constructor
    public EnrollmentRequestService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO; // Inject StudentDAO
    }

    // Thêm yêu cầu mới từ Teacher
    public void addRequest(PendingStudentRequest request) {
        pendingRequests.add(request);
        System.out.println("Đã nhận yêu cầu thêm học viên vào danh sách chờ xử lý: " + request);
    }

    // Lấy danh sách các yêu cầu chờ để Admin xem và xử lý
    public List<PendingStudentRequest> getPendingRequests() {
        return pendingRequests;
    }

    // Admin chấp nhận yêu cầu
    public boolean approveRequest(String requestId, String address) {
        for (PendingStudentRequest request : pendingRequests) {
            if (request.getRequestId().equals(requestId)) {
                try {
                    // Tạo đối tượng Student từ thông tin trong Pending Request
                    Student student = new Student();
                    student.setId(request.getStudentId());
                    student.setName(request.getStudentName());
                    student.setGender(request.getStudentGender());
                    student.setContactNumber(request.getStudentContact());
                    student.setBirthday(request.getStudentBirthday().toString());

                    // Sử dụng phương thức createStudentAndUserTransaction
                    boolean result = studentDAO.createStudentAndUserTransaction(student, address);
                    if (result) {
                        pendingRequests.remove(request); // Xóa yêu cầu khỏi danh sách chờ xử lý
                        System.out.println("Yêu cầu đã được duyệt và sinh viên đã được thêm vào DB: " + request.getStudentName());
                        return true;
                    } else {
                        System.err.println("Lỗi khi thêm sinh viên vào database: " + request.getStudentName());
                        return false;
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi xảy ra khi xử lý yêu cầu: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        System.out.println("Yêu cầu với ID " + requestId + " không tồn tại hoặc đã được xử lý.");
        return false;
    }

    // Admin từ chối yêu cầu
    public void rejectRequest(String requestId) {
        boolean removed = pendingRequests.removeIf(request -> request.getRequestId().equals(requestId));
        if (removed) {
            System.out.println("Yêu cầu với ID " + requestId + " đã bị từ chối và xóa khỏi danh sách.");
        } else {
            System.out.println("Không tìm thấy yêu cầu để từ chối.");
        }
    }
}