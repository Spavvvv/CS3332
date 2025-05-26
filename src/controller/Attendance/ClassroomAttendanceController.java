package src.controller.Attendance;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import src.dao.ClassSession.ClassSessionDAO;
import src.dao.Attendance.HomeworkDAO;
import src.dao.Attendance.HomeworkSubmissionDAO;
import src.dao.Person.StudentDAO;

import src.model.ClassSession;
import src.model.attendance.ClassAttendanceModel;
import src.model.attendance.HomeworkSubmissionModel;
import src.model.attendance.StudentAttendanceData;
import src.model.homework.Homework;
import src.model.person.Student;
import src.view.Attendance.ClassroomAttendanceView;
import src.utils.DaoManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Controller for managing attendance functionality in ClassroomAttendanceView.
 * Handles loading data from the database, modifying, adding, saving changes, and interacting with models/DAOs.
 */
public class ClassroomAttendanceController {

    private final ClassroomAttendanceView view;
    private final ClassSessionDAO classSessionDAO;
    private final StudentDAO studentDAO;
    private final HomeworkSubmissionDAO homeworkSubmissionDAO;
    private final HomeworkDAO homeworkDAO;
    private final ClassAttendanceModel attendanceModel;
    private List<ClassSession> classSessions; // Thêm biến lưu trữ danh sách buổi học

    // Logger for debugging
    private static final Logger LOGGER = Logger.getLogger(ClassroomAttendanceController.class.getName());

    public ClassroomAttendanceController(ClassroomAttendanceView view) {
        this.view = view;

        // Using DaoManager to get singleton instances of DAOs
        DaoManager daoManager = DaoManager.getInstance();
        this.classSessionDAO = daoManager.getClassSessionDAO();
        this.studentDAO = daoManager.getStudentDAO();
        this.homeworkSubmissionDAO = daoManager.getHomeworkSubmissionDAO();
        this.homeworkDAO = daoManager.getHomeworkDAO();

        // The model holding attendance data
        this.attendanceModel = new ClassAttendanceModel();
        this.classSessions = new ArrayList<>();
    }

    /**
     * Load students and their metrics based on the class ID from a class session.
     *
     * @param classId The class ID for which students should be loaded.
     */
    public void loadContextForClass(String classId) { // classId ở đây có lẽ nên là courseId
        if (classId == null || classId.trim().isEmpty()) {
            view.showError("Invalid ID provided. Cannot load data."); // Sửa thông báo chung hơn
            return;
        }

        // Đặt courseId cho attendanceModel ngay từ đầu
        attendanceModel.setCourseId(classId); // Giả sử classId truyền vào thực chất là courseId

        LOGGER.log(Level.INFO, "Fetching students for courseId: {0}", classId);
        List<Student> students = studentDAO.findByCourseId(classId); // Giả sử findByCourseId là đúng
        LOGGER.log(Level.INFO, "Fetched {0} students for courseId: {1}.", new Object[]{students.size(), classId});

        // Tải danh sách buổi học
        loadSessionsForClass(classId); // Phương thức này đã gọi view.setAvailableSessions và cập nhật view/model cho session đầu tiên

        // Sau khi loadSessionsForClass, attendanceModel.getSessionId() và attendanceModel.getSessionDate()
        // đã được đặt cho buổi học đầu tiên (nếu có).
        // Và view.setSessionNotes(), view.setSelectedDate() cũng đã được gọi trong loadSessionsForClass cho buổi đầu tiên.
        // => KHÔNG CẦN LÀM GÌ THÊM Ở ĐÂY ĐỂ HIỂN THỊ NOTES CHO BUỔI ĐẦU TIÊN NỮA
        // NẾU loadSessionsForClass đã làm đúng.

        // Tuy nhiên, logic tải StudentAttendanceData hiện tại đang lấy "lịch sử metrics gần nhất"
        // Nó nên lấy metrics và homework cho buổi học *hiện tại* (buổi đầu tiên mặc định)

        // Gọi reloadDataForSelectedSession để tải dữ liệu chi tiết cho buổi học đầu tiên
        if (attendanceModel.getSessionId() != null && !attendanceModel.getSessionId().isEmpty()) {
            reloadDataForSelectedSession(attendanceModel.getSessionId(), attendanceModel.getCourseId());
            view.showSuccess("Attendance data loaded successfully."); // Thông báo này có thể để ở cuối reloadDataForSelectedSession
        } else {
            // Không có buổi học nào, xóa dữ liệu bảng
            attendanceModel.getAttendanceList().clear();
            // view.setFilteredAttendanceList(... với danh sách rỗng ...); // Cần cập nhật view
            FilteredList<StudentAttendanceData> emptyFilteredList = new FilteredList<>(attendanceModel.getAttendanceList(), p -> true);
            view.setFilteredAttendanceList(emptyFilteredList);
            view.showError("No sessions available for this course.");
        }
    }

    /**
     * Tải danh sách buổi học cho lớp và cập nhật ComboBox số buổi.
     *
     * @param courseId ID của lớp cần tải buổi học
     */
    private void loadSessionsForClass(String courseId) { // Đổi classId thành courseId nếu đúng ngữ cảnh
        LOGGER.log(Level.INFO, "Loading sessions for courseId: {0}", courseId);
        try {
            classSessions = classSessionDAO.findByCourseId(courseId); // Giả sử có findByCourseId
            LOGGER.log(Level.INFO, "Loaded {0} sessions for courseId: {1}",
                    new Object[]{classSessions.size(), courseId});

            if (classSessions.isEmpty()) {
                LOGGER.log(Level.INFO, "No sessions found for courseId: {0}", courseId);
                view.setAvailableSessions(FXCollections.observableArrayList(), null);
                // Xóa thông tin session cũ trên view
                view.setSessionNotes("");
                view.setSelectedDate(null);
                attendanceModel.setSessionId(null);
                attendanceModel.setSessionDate(null);
                return;
            }

            ObservableList<Integer> sessionNumbers = FXCollections.observableArrayList(
                    IntStream.rangeClosed(1, classSessions.size())
                            .boxed()
                            .collect(Collectors.toList())
            );
            Integer defaultSessionNumber = sessionNumbers.isEmpty() ? null : sessionNumbers.get(0);
            view.setAvailableSessions(sessionNumbers, defaultSessionNumber);

            if (defaultSessionNumber != null) {
                ClassSession firstSession = classSessions.get(0); // Buổi học đầu tiên (index 0)
                attendanceModel.setSessionId(firstSession.getId());
                attendanceModel.setSessionDate(firstSession.getStartTime().toLocalDate());

                String notesForFirstSession = firstSession.getSessionNotes();
                view.setSessionNotes(notesForFirstSession != null ? notesForFirstSession : "");
                view.setSelectedDate(firstSession.getStartTime().toLocalDate());

                view.setSelectedDate(firstSession.getStartTime().toLocalDate());
                LOGGER.log(Level.INFO, "Default session (ID: {0}) info set to view. Notes: ''{1}''", new Object[]{firstSession.getId(), notesForFirstSession});
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading sessions for courseId: " + courseId, e);
            view.showError("Failed to load class sessions: " + e.getMessage());
        }
    }

    /**
     * Save modified attendance information back to the database including metrics.
     */
    public void saveInformation() {
        try {
            LOGGER.log(Level.INFO, "Saving all information for courseId: {0}, sessionId: {1}",
                    new Object[]{attendanceModel.getCourseId(), attendanceModel.getSessionId()});

            // Lấy thông tin lớp, buổi học và ngày từ model
            String courseId = attendanceModel.getCourseId();
            String sessionId = attendanceModel.getSessionId();
            LocalDate recordDate = attendanceModel.getSessionDate(); // Ngày của buổi học đang xử lý
            List<StudentAttendanceData> attendanceList = attendanceModel.getAttendanceList();

            // Lấy nội dung ghi chú buổi học từ View
            String notesFromView = view.getSessionNotes();

            // === BƯỚC 1: LƯU GHI CHÚ BUỔI HỌC (SESSION NOTES) ===
            if (sessionId != null && !sessionId.isEmpty()) {
                ClassSession sessionToUpdate = null;
                if (this.classSessions != null) { // Đảm bảo classSessions đã được tải
                    for (ClassSession cs : this.classSessions) {
                        if (cs.getId() != null && cs.getId().equals(sessionId)) {
                            sessionToUpdate = cs;
                            break;
                        }
                    }
                }

                if (sessionToUpdate != null) {
                    // Giả sử ClassSession model có phương thức setSessionNotes()
                    // và bảng class_sessions có cột tương ứng để lưu ghi chú này.
                    sessionToUpdate.setSessionNotes(notesFromView); // Cập nhật ghi chú vào đối tượng ClassSession

                    // Giả sử ClassSessionDAO có phương thức update(ClassSession session)
                    boolean notesUpdated = classSessionDAO.update(sessionToUpdate);
                    if (notesUpdated) {
                        LOGGER.log(Level.INFO, "Session notes updated successfully for sessionId: {0}", sessionId);
                    } else {
                        LOGGER.log(Level.WARNING, "Failed to update session notes for sessionId: {0} (DAO returned false). Session object might not have been found by DAO or no changes made.", sessionId);
                        // Bạn có thể cân nhắc hiển thị một cảnh báo nhỏ cho người dùng ở đây nếu cần
                        // view.showWarning("Could not save session notes details.");
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Could not find ClassSession object in controller's list for current sessionId: {0}. Session notes not saved.", sessionId);
                    // Điều này có thể xảy ra nếu danh sách classSessions không được đồng bộ hoặc sessionId không hợp lệ
                }
            } else {
                LOGGER.log(Level.WARNING, "SessionId is null or empty, cannot save session notes.");
            }
            // =======================================================

            // === BƯỚC 2: LƯU STUDENT METRICS (Chuyên cần, Đúng giờ, Ghi chú HV) ===
            // Trước hết, lấy tất cả các metrics hiện có cho các học sinh trong lớp này để update nếu có
            Map<String, String> existingMetricsMap = new HashMap<>();
            // Giả sử homeworkDAO.getStudentMetricsForCourse trả về List<Map<String, Object>>
            // với key "student_id" và "metric_id" (là String)
            if (courseId != null && !courseId.isEmpty()) { // Chỉ lấy metrics nếu courseId hợp lệ
                List<Map<String, Object>> existingMetrics = homeworkDAO.getStudentMetricsForCourse(courseId);
                for (Map<String, Object> metric : existingMetrics) {
                    String studentId = (String) metric.get("student_id");
                    String metricId = (String) metric.get("metric_id"); // metric_id là String
                    if (studentId != null && metricId != null) {
                        existingMetricsMap.put(studentId, metricId);
                    }
                }
            }


            List<Map<String, Object>> metricsDataList = new ArrayList<>();
            if (recordDate == null) { // Cần recordDate để lưu metrics
                LOGGER.log(Level.SEVERE, "Record date is null. Cannot save student metrics.");
                view.showError("Session date is not set. Cannot save student metrics.");
                // Không nên tiếp tục nếu không có recordDate
            } else {
                for (StudentAttendanceData data : attendanceList) {
                    String studentId = data.getStudent().getId();

                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put("studentId", studentId);
                    metrics.put("courseId", courseId); // courseId của buổi học
                    metrics.put("recordDate", java.sql.Date.valueOf(recordDate)); // Ngày ghi nhận metrics
                    metrics.put("awarenessScore", data.getDiligenceRating()); // Điểm chuyên cần
                    metrics.put("punctualityScore", data.getPunctualityRating()); // Điểm đúng giờ
                    metrics.put("notes", data.getStudentSessionNotes()); // Ghi chú của riêng học viên đó

                    // Thêm metric_id nếu đã tồn tại cho học sinh này (để DAO biết update thay vì insert)
                    if (existingMetricsMap.containsKey(studentId)) {
                        metrics.put("metricId", existingMetricsMap.get(studentId));
                    }
                    metricsDataList.add(metrics);
                }

                if (!metricsDataList.isEmpty()) {
                    try {
                        int metricsCount = homeworkDAO.saveStudentMetricsBatch(metricsDataList);
                        LOGGER.log(Level.INFO, "Saved/Updated {0} student metrics records.", metricsCount);
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Error saving student metrics batch", e);
                        view.showError("Error saving student metrics: " + e.getMessage());
                        // Quyết định xem có ném lại lỗi hoặc dừng ở đây không
                    }
                }
            }
            // ==========================================================

            // === BƯỚC 3: LƯU HOMEWORK SUBMISSIONS (Bài tập về nhà) ===
            // (Phương thức saveHomeworkSubmissions đã được bạn viết và gọi ở nơi khác,
            // hoặc nếu bạn muốn gọi nó ở đây thì đảm bảo nó không gây xung đột)
            // Nếu saveHomeworkSubmissions được gọi riêng biệt thì không cần gọi lại ở đây.
            // Nếu nó là một phần của "Lưu thông tin" tổng thể thì gọi ở đây:
            try {
                saveHomeworkSubmissions(); // Đảm bảo phương thức này xử lý lỗi nội bộ hoặc ném để catch ở ngoài
            } catch (Exception e) { // Bắt Exception chung nếu saveHomeworkSubmissions có thể ném nhiều loại
                LOGGER.log(Level.SEVERE, "Error during saveHomeworkSubmissions call within saveInformation", e);
                view.showError("Error saving homework data: " + e.getMessage());
            }
            // =======================================================

            // attendanceModel.saveAttendanceData(); // Gọi nếu phương thức này có nhiệm vụ khác chưa được xử lý

            view.showSuccess("All information saving process completed.");
            LOGGER.log(Level.INFO, "All information saving process completed.");

        } catch (Exception e) { // Bắt Exception chung cho toàn bộ quá trình
            LOGGER.log(Level.SEVERE, "Failed to save all information: " + e.getMessage(), e);
            view.showError("Failed to save all information: " + e.getMessage());
        }
    }

    /**
     * Update the session date when the date picker value is changed.
     */
    public void updateSessionDate() {
        LocalDate selectedDate = view.getSelectedDate();
        if (selectedDate == null) {
            view.showError("No date selected.");
            LOGGER.log(Level.WARNING, "No date selected in updateSessionDate.");
            return;
        }
        LOGGER.log(Level.INFO, "Updating session date to: {0}", selectedDate);
        // Update the date in the attendance model
        attendanceModel.setSessionDate(selectedDate);
        //view.showSuccess("Session date updated to: " + selectedDate);
    }

    public void saveHomeworkSubmissions() {
        if (attendanceModel.getSessionId() == null || attendanceModel.getSessionId().isEmpty()) {
            view.showError("No session ID found. Cannot save homework submissions.");
            LOGGER.log(Level.WARNING, "Cannot save homework submissions because sessionId is null or empty.");
            return;
        }

        String sessionId = attendanceModel.getSessionId();
        String courseId = attendanceModel.getCourseId();

        if (courseId == null || courseId.isEmpty()) {
            view.showError("No course ID found for the session. Cannot save homework submissions.");
            LOGGER.log(Level.WARNING, "Cannot save homework submissions because courseId is null or empty for session: " + sessionId);
            return;
        }

        // Biến để lưu homework object, sẽ được dùng để cập nhật điểm trung bình
        Homework homeworkToUpdate = null;
        String determinedHomeworkId = null;

        try {
            LOGGER.log(Level.INFO, "Attempting to save homework submissions for sessionId: {0}", sessionId);

            // Bước 1: Đảm bảo bản ghi Homework tồn tại và lấy ID
            // (Giữ nguyên logic getOrCreateHomeworkForSession của bạn)
            LocalDate sessionActualDate = attendanceModel.getSessionDate();
            if (sessionActualDate == null) {
                LOGGER.log(Level.SEVERE, "Session actual date is null. Cannot proceed with saving homework submissions for session: " + sessionId);
                view.showError("Session date not available. Cannot save homework submissions.");
                return;
            }
            String homeworkTitle = "Bài tập buổi " + sessionActualDate.toString();
            // Sử dụng assignedDate từ sessionActualDate cho getOrCreateHomeworkForSession
            homeworkToUpdate = homeworkDAO.getOrCreateHomeworkForSession(sessionId, homeworkTitle, courseId, sessionActualDate);

            if (homeworkToUpdate == null || homeworkToUpdate.getHomeworkId() == null || homeworkToUpdate.getHomeworkId().isEmpty()) {
                LOGGER.log(Level.SEVERE, "Failed to get or create a valid homework record for session: " + sessionId);
                view.showError("Could not establish a valid homework entry for this session.");
                return;
            }
            determinedHomeworkId = homeworkToUpdate.getHomeworkId();
            LOGGER.log(Level.INFO, "Using homework_id: {0} for session: {1}", new Object[]{determinedHomeworkId, sessionId});

            // Bước 2: Lấy các bản ghi nộp bài hiện có cho homework_id này (để so sánh và chuẩn bị update/insert)
            Map<String, HomeworkSubmissionModel> existingSubmissionsMap = new HashMap<>();
            List<HomeworkSubmissionModel> submissionsForThisHomeworkFromDb = homeworkSubmissionDAO.getSubmissionsByHomeworkId(determinedHomeworkId);
            for (HomeworkSubmissionModel sub : submissionsForThisHomeworkFromDb) {
                existingSubmissionsMap.put(sub.getStudentId(), sub);
            }
            LOGGER.log(Level.INFO, "Fetched {0} existing submissions for homework_id: {1}", new Object[]{existingSubmissionsMap.size(), determinedHomeworkId});

            // Bước 3: Chuẩn bị danh sách các bài nộp cần lưu (tạo mới hoặc cập nhật)
            List<HomeworkSubmissionModel> submissionsToSave = new ArrayList<>();
            for (StudentAttendanceData data : attendanceModel.getAttendanceList()) {
                String studentId = data.getStudent().getId();
                HomeworkSubmissionModel submission = existingSubmissionsMap.get(studentId);

                if (submission == null) {
                    submission = new HomeworkSubmissionModel();
                    // student_submission_id sẽ được tạo trong saveOrUpdateBatch nếu cần
                    submission.setStudentId(studentId);
                    submission.setHomeworkId(determinedHomeworkId);
                }
                // Cập nhật thông tin từ StudentAttendanceData
                submission.setSubmitted(data.isHomeworkSubmitted());
                submission.setGrade(data.getHomeworkGrade());
                submission.setSubmissionTimestamp(data.getSubmissionDate() != null
                        ? data.getSubmissionDate()
                        : (data.isHomeworkSubmitted() ? LocalDateTime.now() : null));
                submission.setCheckedInSessionId(sessionId);
                // submission.setEvaluatorNotes(...); // Nếu có trường này trong StudentAttendanceData

                submissionsToSave.add(submission);
            }

            // Bước 4: Lưu hoặc cập nhật các bài nộp theo lô
            if (!submissionsToSave.isEmpty()) {
                // Giả sử saveOrUpdateBatch trong HomeworkSubmissionDAO đã được sửa để tự tạo student_submission_id
                homeworkSubmissionDAO.saveOrUpdateBatch(submissionsToSave);
                LOGGER.log(Level.INFO, "Successfully processed {0} homework submissions for homework_id: {1}.",
                        new Object[]{submissionsToSave.size(), determinedHomeworkId});

                // === BƯỚC 5: TÍNH TOÁN VÀ CẬP NHẬT ĐIỂM TRUNG BÌNH CHO HOMEWORK ===
                // Lấy lại tất cả các bài nộp (đã được cập nhật) cho homework này để tính điểm TB
                List<HomeworkSubmissionModel> updatedSubmissions = homeworkSubmissionDAO.getSubmissionsByHomeworkId(determinedHomeworkId);
                double totalScore = 0;
                int submittedCount = 0;
                for (HomeworkSubmissionModel sub : updatedSubmissions) {
                    if (sub.isSubmitted() && sub.getGrade() != 0) { // Chỉ tính những bài đã nộp và có điểm
                        totalScore += sub.getGrade();
                        submittedCount++;
                    }
                }

                Double averageScore = null;
                if (submittedCount > 0) {
                    averageScore = totalScore / submittedCount;
                    // Làm tròn đến 1 chữ số thập phân nếu muốn
                    // averageScore = Math.round(averageScore * 10.0) / 10.0;
                }

                // Cập nhật đối tượng homeworkToUpdate (đã lấy ở Bước 1)
                homeworkToUpdate.setScore(averageScore); // Cột 'score' trong bảng 'homework' giờ là điểm trung bình
                homeworkToUpdate.setSubmissionDate(LocalDateTime.now()); // Thời gian hoàn tất việc cập nhật điểm này
                // Bạn có thể cập nhật status của homework nếu cần, ví dụ: "Graded"
                // homeworkToUpdate.setStatus("Graded");

                boolean homeworkUpdated = homeworkDAO.update(homeworkToUpdate); // Gọi DAO để cập nhật bảng homework
                if (homeworkUpdated) {
                    LOGGER.log(Level.INFO, "Homework table updated with average score: {0} for homework_id: {1}",
                            new Object[]{averageScore, determinedHomeworkId});
                } else {
                    LOGGER.log(Level.WARNING, "Failed to update homework table with average score for homework_id: {0}", determinedHomeworkId);
                }
                // ================================================================

                view.showSuccess("Homework submissions and average score saved successfully.");

            } else {
                LOGGER.log(Level.INFO, "No homework submissions to save for homework_id: {0}.", determinedHomeworkId);
                view.showSuccess("No changes in homework submissions to save.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save homework submissions for session " + sessionId, e);
            view.showError("Failed to save homework submissions: " + e.getMessage());
        } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra
            LOGGER.log(Level.SEVERE, "An unexpected error occurred while saving homework submissions for session " + sessionId, e);
            view.showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Handle session change from src.view and reload data.
     */
    public void handleSessionChange() {
        Integer sessionNumber = view.getSelectedSessionNumber();
        if (sessionNumber == null) {
            // view.showError("No session selected."); // Có thể không cần báo lỗi nếu đây là trạng thái bình thường
            LOGGER.log(Level.WARNING, "No session selected during session change or list is empty.");
            // Xóa dữ liệu trên bảng nếu không có session nào được chọn
            attendanceModel.getAttendanceList().clear();
            view.setFilteredAttendanceList(new FilteredList<>(attendanceModel.getAttendanceList(), p -> true));
            view.setSessionNotes("");
            view.setSelectedDate(null); // Hoặc một ngày mặc định
            return;
        }

        LOGGER.log(Level.INFO, "Handling session change for session number: {0}", sessionNumber);
        int sessionIndex = sessionNumber - 1;

        if (sessionIndex < 0 || sessionIndex >= classSessions.size()) {
            LOGGER.log(Level.WARNING, "Invalid session index: {0}, total sessions: {1}",
                    new Object[]{sessionIndex, classSessions.size()});
            view.showError("Invalid session number selected.");
            attendanceModel.getAttendanceList().clear(); // Xóa dữ liệu cũ
            return;
        }

        ClassSession selectedSession = classSessions.get(sessionIndex);
        LOGGER.log(Level.INFO, "Selected session details: ID={0}, Date={1}",
                new Object[]{selectedSession.getId(), selectedSession.getStartTime().toLocalDate()});

        // Cập nhật thông tin chung của buổi học lên Model và View
        attendanceModel.setSessionId(selectedSession.getId());
        attendanceModel.setSessionDate(selectedSession.getStartTime().toLocalDate());
        //attendanceModel.setCourseId(...); // Đảm bảo courseId trong attendanceModel cũng đúng nếu cần

        // Cập nhật View với thông tin của buổi học mới
        // view.setSessionNotes(selectedSession.getNotes()); // Lấy notes của session nếu có
        // Hoặc nếu bạn muốn hiển thị tên khóa học ở phần notes của view:
        view.setSessionNotes(selectedSession.getSessionNotes()); // Giả sử ClassSession có getTopicOrDescription()
        view.setSelectedDate(selectedSession.getStartTime().toLocalDate());

        // Tải lại TOÀN BỘ dữ liệu học sinh (bao gồm metrics và BTVN) cho buổi học mới này
        reloadDataForSelectedSession(selectedSession.getId(), attendanceModel.getCourseId());

        view.refreshView(); // Đảm bảo view được cập nhật sau khi model thay đổi
    }

    /**
     * Export attendance data to Excel.
     */
    public void exportToExcel() {
        try {
            LOGGER.log(Level.INFO, "Exporting attendance data to Excel.");
            attendanceModel.exportToExcel();
            view.showSuccess("Attendance data exported to Excel successfully!");
            LOGGER.log(Level.INFO, "Attendance data exported to Excel successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to export attendance data to Excel.", e);
            view.showError("Failed to export attendance data: " + e.getMessage());
        }
    }

    /**
     * Update all homework statuses based on the header checkbox in the src.view.
     */
    public void updateAllHomeworkStatus() {
        boolean selectAll = view.isHomeworkSelectAllChecked();
        LOGGER.log(Level.INFO, "Updating all homework submission statuses to: {0}", selectAll);

        for (StudentAttendanceData data : attendanceModel.getAttendanceList()) {
            data.setHomeworkSubmitted(selectAll);
            LOGGER.log(Level.INFO, "Updated homework submission status for studentId: {0} to: {1}",
                    new Object[]{data.getStudent().getId(), selectAll});
        }
        view.refreshTable();
    }

    /**
     * Apply filters for punctuality and diligence based on src.view settings.
     * Filters the currently loaded students based on their metrics.
     */
    public void applyFilters() {
        int punctualityFilter = view.getPunctualityFilterValue();
        int diligenceFilter = view.getDiligenceFilterValue();
        String classId = view.getMainController().getCurrentCourseId();
        if (classId == null || classId.isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot apply filters: No class selected");
            view.showError("Vui lòng chọn một lớp học trước khi lọc");
            return;
        }
        LOGGER.log(Level.INFO, "Applying filters: punctuality >= {0}, diligence >= {1} for class {2}",
                new Object[]{punctualityFilter, diligenceFilter, classId});
        // Lấy danh sách học sinh trong lớp
        List<Student> students = studentDAO.findByCourseId(classId);
        if (students.isEmpty()) {
            LOGGER.log(Level.INFO, "No students found in class {0}", classId);
            ObservableList<StudentAttendanceData> emptyList = FXCollections.observableArrayList();
            FilteredList<StudentAttendanceData> emptyFilteredList = new FilteredList<>(emptyList);
            view.setFilteredAttendanceList(emptyFilteredList);
            return;
        }
        // Áp dụng bộ lọc bằng cách xử lý riêng từng học sinh
        List<StudentAttendanceData> filteredStudents = new ArrayList<>();
        for (Student student : students) {
            List<StudentAttendanceData> metrics = studentDAO.getStudentMetricsHistory(student.getId(), classId);

            // Nếu không có dữ liệu metrics hoặc không có bộ lọc, thêm học sinh vào kết quả
            if (metrics.isEmpty()) {
                if (punctualityFilter <= 0 && diligenceFilter <= 0) {
                    filteredStudents.add(new StudentAttendanceData(student));
                }
                continue;
            }

            // Lấy bản ghi gần nhất
            StudentAttendanceData latestMetrics = metrics.get(0);

            // Kiểm tra điều kiện lọc
            boolean punctualityMatch = punctualityFilter <= 0 || latestMetrics.getPunctualityRating() >= punctualityFilter;
            boolean diligenceMatch = diligenceFilter <= 0 || latestMetrics.getDiligenceRating() >= diligenceFilter;

            if (punctualityMatch && diligenceMatch) {
                filteredStudents.add(latestMetrics);
            }
        }
        LOGGER.log(Level.INFO, "Filter applied: {0} students match criteria", filteredStudents.size());
        // Chuyển danh sách đã lọc thành ObservableList và FilteredList
        ObservableList<StudentAttendanceData> observableList = FXCollections.observableArrayList(filteredStudents);
        FilteredList<StudentAttendanceData> filteredList = new FilteredList<>(observableList);

        // Cập nhật src.view
        view.setFilteredAttendanceList(filteredList);
    }

    /**
     * Tải dữ liệu bài tập về nhà cho một buổi học cụ thể
     * @param sessionId ID của buổi học cần tải dữ liệu
     */
    public void loadHomeworkSubmissionsForSession(String sessionId) {
        try {
            // Lấy danh sách bài tập của buổi học
            List<HomeworkSubmissionModel> submissions = homeworkSubmissionDAO.getBySessionId(sessionId);

            LOGGER.log(Level.INFO, "Retrieved {0} homework submissions for sessionId: {1}",
                    new Object[]{submissions.size(), sessionId});

            Map<String, HomeworkSubmissionModel> submissionMap = new HashMap<>();

            // Đưa dữ liệu vào map để dễ truy xuất theo ID học sinh
            for (HomeworkSubmissionModel submission : submissions) {
                submissionMap.put(submission.getStudentId(), submission);
                LOGGER.log(Level.INFO, "Student {0}: Grade = {1}, Submitted = {2}",
                        new Object[]{submission.getStudentId(), submission.getGrade(), submission.isSubmitted()});
            }

            // Cập nhật dữ liệu bài tập vào mô hình sinh viên
            for (StudentAttendanceData studentData : attendanceModel.getAttendanceList()) {
                String studentId = studentData.getStudent().getId();
                HomeworkSubmissionModel submission = submissionMap.get(studentId);

                if (submission != null) {
                    // Chính sửa ở đây: sử dụng trạng thái submitted từ submission model
                    // để cập nhật vào studentData
                    studentData.setHomeworkSubmitted(submission.isSubmitted());
                    studentData.setHomeworkGrade(submission.getGrade());
                    studentData.setSubmissionDate(submission.getSubmissionTimestamp());
                } else {
                    // Nếu chưa có dữ liệu bài tập, thiết lập giá trị mặc định
                    studentData.setHomeworkSubmitted(false);
                    studentData.setHomeworkGrade(0.0);
                    studentData.setSubmissionDate(null);
                }
            }

            // Cập nhật src.view để hiển thị dữ liệu mới
            view.refreshTable();
            LOGGER.log(Level.INFO, "Đã tải dữ liệu bài tập cho buổi học: {0}", sessionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu bài tập về nhà", e);
            view.showError("Đã xảy ra lỗi khi tải dữ liệu bài tập về nhà: " + e.getMessage());
        }
    }

    /**
     * Tải lại toàn bộ dữ liệu điểm danh, metrics, và bài tập về nhà cho tất cả học sinh
     * của một courseId cụ thể, cho một sessionId cụ thể.
     *
     * @param sessionId ID của buổi học cần tải dữ liệu.
     * @param courseId  ID của khóa học.
     */
    private void reloadDataForSelectedSession(String sessionId, String courseId) {
        //LOGGER.log(Level.INFO, "---- ENTERING reloadDataForSelectedSession ---- sessionId: {0}, courseId: {1}", new Object[]{sessionId, courseId});
        if (sessionId == null || courseId == null || sessionId.isEmpty() || courseId.isEmpty()) {
            //LOGGER.log(Level.WARNING, "reloadData: Session ID or Course ID is null/empty. Aborting. SessionId: ''{0}'', CourseId: ''{1}''", new Object[]{sessionId, courseId});
            attendanceModel.getAttendanceList().clear();
            // Cập nhật view với danh sách rỗng
            FilteredList<StudentAttendanceData> emptyFilteredList = new FilteredList<>(attendanceModel.getAttendanceList(), p -> true);
            view.setFilteredAttendanceList(emptyFilteredList);
            // view.refreshTable(); // Không cần nếu setFilteredAttendanceList đã refresh
            return;
        }

        try {
            // 1. Lấy danh sách học sinh của khóa học
            List<Student> students = studentDAO.findByCourseId(courseId);
            //LOGGER.log(Level.INFO, "reloadData: studentDAO.findByCourseId(''{0}'') returned {1} students.", new Object[]{courseId, students.size()});

            if (students.isEmpty()) {
                //LOGGER.log(Level.INFO, "reloadData: No students found for courseId: {0}. Clearing attendance list.", courseId);
                attendanceModel.getAttendanceList().clear();
                FilteredList<StudentAttendanceData> emptyFilteredList = new FilteredList<>(attendanceModel.getAttendanceList(), p -> true);
                view.setFilteredAttendanceList(emptyFilteredList);
                return;
            }

            // 2. Chuẩn bị dữ liệu cho homework của session này
            LocalDate sessionActualDate = attendanceModel.getSessionDate();
            //LOGGER.log(Level.INFO, "reloadData: Using sessionActualDate from attendanceModel: {0} for sessionId: {1}", new Object[]{sessionActualDate, sessionId});
            if (sessionActualDate == null) {
                //LOGGER.log(Level.SEVERE, "reloadData: sessionActualDate is NULL. Cannot fetch metrics or create/get homework. Aborting for sessionId: {0}", sessionId);
                attendanceModel.getAttendanceList().clear();
                FilteredList<StudentAttendanceData> emptyFilteredList = new FilteredList<>(attendanceModel.getAttendanceList(), p -> true);
                view.setFilteredAttendanceList(emptyFilteredList);
                return;
            }

            Homework homeworkForSession = homeworkDAO.getOrCreateHomeworkForSession(
                    sessionId,
                    "Bài tập buổi " + sessionActualDate.toString(),
                    courseId,
                    sessionActualDate
            );
            String actualHomeworkId = null;
            if (homeworkForSession != null && homeworkForSession.getHomeworkId() != null) {
                actualHomeworkId = homeworkForSession.getHomeworkId();
                //LOGGER.log(Level.INFO, "reloadData: Determined actualHomeworkId: {0} for sessionId: {1}", new Object[]{actualHomeworkId, sessionId});
            } else {
                //LOGGER.log(Level.WARNING, "reloadData: homeworkForSession or its ID is null for sessionId: {0}. Homework related data might be missing.", sessionId);
                // Quyết định xem có nên dừng ở đây không nếu không có homeworkId.
                // Hiện tại, code sẽ tiếp tục và homeworkSubmissionsMap sẽ rỗng.
            }


            // 3. Lấy tất cả homework submissions cho homework_id này một lần để tối ưu
            Map<String, HomeworkSubmissionModel> homeworkSubmissionsMap = new HashMap<>();
            if (actualHomeworkId != null && !actualHomeworkId.isEmpty()) {
                List<HomeworkSubmissionModel> submissions = homeworkSubmissionDAO.getSubmissionsByHomeworkId(actualHomeworkId);
                for (HomeworkSubmissionModel sub : submissions) {
                    homeworkSubmissionsMap.put(sub.getStudentId(), sub);
                }
                //LOGGER.log(Level.INFO, "reloadData: Fetched {0} submissions for actualHomeworkId: {1}", new Object[]{homeworkSubmissionsMap.size(), actualHomeworkId});
            } else {
                //LOGGER.log(Level.INFO, "reloadData: actualHomeworkId is null or empty, skipping fetch for homework submissions.");
            }

            // 4. Tạo danh sách StudentAttendanceData mới
            ObservableList<StudentAttendanceData> newAttendanceDataList = FXCollections.observableArrayList();
            for (Student student : students) {
                StudentAttendanceData sad = new StudentAttendanceData(student);
                //LOGGER.log(Level.INFO, "reloadData [LOOP START]: Processing studentId: {0}, Name: {1}", new Object[]{student.getId(), student.getName()});

                // 4a. Lấy Student Metrics (Punctuality, Diligence, Notes) cho session này
                Optional<Map<String, Object>> metricsOpt = homeworkDAO.findStudentMetrics(
                        student.getId(),
                        courseId,
                        java.sql.Date.valueOf(sessionActualDate) // Chuyển LocalDate sang java.sql.Date
                );

                if (metricsOpt.isPresent()) {
                    Map<String, Object> metrics = metricsOpt.get();
                    Integer punctualityScoreVal = metrics.get("punctualityScore") != null ? ((Number) metrics.get("punctualityScore")).intValue() : 0;
                    Integer awarenessScoreVal = metrics.get("awarenessScore") != null ? ((Number) metrics.get("awarenessScore")).intValue() : 0;
                    String studentNotesVal = (String) metrics.get("notes");

                    sad.punctualityRatingProperty().set(punctualityScoreVal);
                    sad.diligenceRatingProperty().set(awarenessScoreVal);
                    sad.setStudentSessionNotes(studentNotesVal);
                    //LOGGER.log(Level.INFO, "reloadData [LOOP]: Metrics LOADED for student {0}: P={1}, D={2}, Notes=''{3}''", new Object[]{student.getId(), punctualityScoreVal, awarenessScoreVal, studentNotesVal});
                } else {
                    sad.punctualityRatingProperty().set(0); // Giá trị mặc định
                    sad.diligenceRatingProperty().set(0); // Giá trị mặc định
                    sad.setStudentSessionNotes("");     // Giá trị mặc định
                    //LOGGER.log(Level.INFO, "reloadData [LOOP]: No metrics found for student {0} on {1}. Using defaults (P=0, D=0, Notes='').", new Object[]{student.getId(), sessionActualDate});
                }

                // 4b. Lấy Homework Submission cho homework_id của session này
                HomeworkSubmissionModel hwSubmission = homeworkSubmissionsMap.get(student.getId());
                if (hwSubmission != null) {
                    sad.setHomeworkSubmitted(hwSubmission.isSubmitted());
                    sad.setHomeworkGrade(hwSubmission.getGrade());
                    sad.setSubmissionDate(hwSubmission.getSubmissionTimestamp());
                    //LOGGER.log(Level.INFO, "reloadData [LOOP]: HW Sub LOADED for student {0}: Submitted={1}, Grade={2}", new Object[]{student.getId(), sad.isHomeworkSubmitted(), sad.getHomeworkGrade()});
                } else {
                    sad.setHomeworkSubmitted(false);
                    sad.setHomeworkGrade(0.0); // Hoặc 0.0 nếu bạn muốn mặc định là số
                    sad.setSubmissionDate(null);
                    //LOGGER.log(Level.INFO, "reloadData [LOOP]: No HW Sub for student {0} for homeworkId: {1}.", new Object[]{student.getId(), actualHomeworkId});
                }
                // Gán homeworkId hiện tại vào StudentAttendanceData để biết nó thuộc bài tập nào (nếu cần)
                if (actualHomeworkId != null) {
                    // sad.setCurrentHomeworkId(actualHomeworkId); // Bỏ comment nếu bạn có trường này trong StudentAttendanceData
                }

                newAttendanceDataList.add(sad);
                //LOGGER.log(Level.INFO, "reloadData [LOOP END]: Added/Updated StudentAttendanceData for studentId: {0}", student.getId());
            }

            // 5. Cập nhật model chính và thông báo cho view
            //LOGGER.log(Level.INFO, "reloadData: Prepared {0} StudentAttendanceData objects in newAttendanceDataList.", newAttendanceDataList.size());
            attendanceModel.getAttendanceList().setAll(newAttendanceDataList);
            //LOGGER.log(Level.INFO, "reloadData: attendanceModel.getAttendanceList() updated with {0} items.", attendanceModel.getAttendanceList().size());

        } catch (SQLException e) {
            //LOGGER.log(Level.SEVERE, "reloadData: SQL Error for session: " + sessionId, e);
            view.showError("Failed to load data for new session: " + e.getMessage());
            attendanceModel.getAttendanceList().clear(); // Xóa dữ liệu nếu có lỗi
        } catch (Exception e) { // Bắt các lỗi runtime khác có thể xảy ra
            //LOGGER.log(Level.SEVERE, "reloadData: Unexpected error for session: " + sessionId, e);
            view.showError("An unexpected error occurred while loading session data: " + e.getMessage());
            attendanceModel.getAttendanceList().clear();
        }
        finally {
            // Luôn cập nhật view, ngay cả khi danh sách rỗng hoặc có lỗi (để xóa dữ liệu cũ)
            FilteredList<StudentAttendanceData> newFilteredList = new FilteredList<>(attendanceModel.getAttendanceList(), p -> true);
            view.setFilteredAttendanceList(newFilteredList);
            // view.refreshTable(); // Có thể không cần thiết nếu setFilteredAttendanceList đủ
            //LOGGER.log(Level.INFO, "---- EXITING reloadDataForSelectedSession ---- List size in view: {0}", newFilteredList.size());
        }
    }

}