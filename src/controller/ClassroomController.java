
package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.dao.ClassroomDAO;
import src.model.classroom.Classroom;

import java.util.List;
// Import các lớp cần thiết khác nếu có

public class ClassroomController {

    private ClassroomDAO classroomDAO;

    public ClassroomController() {
        this.classroomDAO = new ClassroomDAO(); // Khởi tạo DAO
    }

    /**
     * Lấy danh sách các phòng học đã được lọc và có thể phân trang (hiện tại chưa phân trang).
     * Chuyển đổi List từ DAO thành ObservableList cho TableView.
     *
     * @param keyword      Từ khóa tìm kiếm (mã phòng, tên phòng).
     * @param statusFilter Lọc theo trạng thái.
     * @return ObservableList các phòng học.
     */
    public ObservableList<Classroom> getFilteredClassrooms(String keyword, String statusFilter) {
        // Trong RoomView, "Tất cả" được dùng cho cmbStatusFilter.
        // Nếu controller nhận "Tất cả", chuyển thành null hoặc chuỗi rỗng để DAO xử lý là "không lọc theo status"
        if ("Tất cả".equalsIgnoreCase(statusFilter)) {
            statusFilter = null;
        }
        List<Classroom> classroomList = classroomDAO.findBySearchCriteria(keyword, statusFilter);
        return FXCollections.observableArrayList(classroomList);
    }

    /**
     * Lưu một phòng học (thêm mới hoặc cập nhật).
     * Controller có thể chứa logic kiểm tra trước khi gọi DAO, ví dụ: kiểm tra trùng mã.
     *
     * @param classroom Đối tượng Classroom cần lưu.
     * @return true nếu lưu thành công, false nếu thất bại.
     */
    public boolean saveClassroom(Classroom classroom) {
        if (classroom == null) {
            return false;
        }

        // VALIDATION LOGIC (ví dụ: kiểm tra trùng mã)
        // Khi thêm mới (giả sử roomId được tạo từ code hoặc người dùng nhập và phải là duy nhất)
        // Hoặc khi sửa, nếu mã phòng bị thay đổi, cần kiểm tra mã mới có trùng không.
        // classroom.getRoomId() thường là khóa chính và không nên thay đổi sau khi tạo.
        // 'code' là một thuộc tính riêng có thể thay đổi.
        // Nếu 'roomId' là một UUID hoặc được DB tự sinh (không phải trường hợp này vì là VARCHAR),
        // thì logic kiểm tra sẽ khác.

        // Giả sử roomId được đặt trước khi gọi save.
        // Nếu classroom.getRoomId() là null hoặc trống cho một phòng *mới*, bạn cần một chiến lược
        // để tạo/gán nó. Ví dụ, nếu `code` là duy nhất và dùng làm `roomId`:
        if (classroom.getRoomId() == null || classroom.getRoomId().trim().isEmpty()) {
            if (classroom.getCode() != null && !classroom.getCode().trim().isEmpty()) {
                // Nếu đây là phòng mới và roomId chưa có, có thể gán code cho roomId
                // NHƯNG phải đảm bảo code này là duy nhất trong cột roomId
                if (!classroomDAO.checkCodeExists(classroom.getCode(), null) && classroomDAO.findByRoomId(classroom.getCode()).isEmpty()) {
                    classroom.setRoomId(classroom.getCode()); // Chỉ là một ví dụ, logic này cần cẩn thận
                } else {
                    // Xử lý lỗi: mã đã tồn tại hoặc không thể dùng làm ID
                    System.err.println("Mã phòng đã tồn tại hoặc không hợp lệ để làm ID.");
                    return false;
                }
            } else {
                System.err.println("Mã phòng và Room ID không được để trống cho phòng mới.");
                return false; // Cần thông tin để tạo phòng
            }
        }

        // Kiểm tra xem 'code' có bị trùng không (ngoại trừ chính nó nếu đang cập nhật)
        if (classroomDAO.checkCodeExists(classroom.getCode(), classroom.getRoomId())) {
            System.err.println("Mã phòng '" + classroom.getCode() + "' đã tồn tại cho một phòng khác.");
            // Hiển thị thông báo lỗi cho người dùng ở View thay vì chỉ in ra console
            return false;
        }


        return classroomDAO.save(classroom);
    }

    /**
     * Xóa một phòng học dựa trên roomId.
     *
     * @param roomId ID của phòng học cần xóa.
     * @return true nếu xóa thành công, false nếu thất bại.
     */
    public boolean deleteClassroom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return false;
        }
        return classroomDAO.deleteByRoomId(roomId);
    }

    /**
     * Cập nhật trạng thái của một phòng học.
     *
     * @param classroom   Đối tượng Classroom (chủ yếu để lấy roomId).
     * @param newStatus Trạng thái mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean updateClassroomStatus(Classroom classroom, String newStatus) {
        if (classroom == null || classroom.getRoomId() == null || classroom.getRoomId().trim().isEmpty() ||
                newStatus == null || newStatus.trim().isEmpty()) {
            return false;
        }
        return classroomDAO.updateStatus(classroom.getRoomId(), newStatus);
    }

    /**
     * Lấy thông tin một phòng học theo roomId.
     *
     * @param roomId ID của phòng học.
     * @return Classroom nếu tìm thấy, null nếu không.
     */
    public Classroom getClassroomByRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return null;
        }
        return classroomDAO.findByRoomId(roomId).orElse(null);
    }

    // Các phương thức khác của controller có thể được thêm vào đây
    // Ví dụ: lấy danh sách tất cả các phòng (nếu cần)
    public ObservableList<Classroom> getAllClassrooms() {
        List<Classroom> classroomList = classroomDAO.findAll();
        return FXCollections.observableArrayList(classroomList);
    }
}

