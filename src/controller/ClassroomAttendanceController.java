package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import src.dao.ClassSessionDAO;
import src.dao.HomeworkDAO;
import src.dao.HomeworkSubmissionDAO;
import src.dao.StudentDAO;
import src.model.ClassSession;
import src.model.attendance.ClassAttendanceModel;
import src.model.attendance.HomeworkSubmissionModel;
import src.model.attendance.StudentAttendanceData;
import src.model.person.Student;
import view.components.ClassroomAttendanceView;
import utils.DaoManager;

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

        // The model holding attendance data
        this.attendanceModel = new ClassAttendanceModel();
        this.classSessions = new ArrayList<>();
    }

    /**
     * Load students and their metrics based on the class ID from a class session.
     *
     * @param classId The class ID for which students should be loaded.
     */
    public void loadContextForClass(String classId) {
        if (classId == null || classId.trim().isEmpty()) {
            view.showError("Invalid class ID provided. Cannot load data.");
            return;
        }

        // Fetch students from the database based on classId
        LOGGER.log(Level.INFO, "Fetching students for classId: {0}", classId);
        List<Student> students = studentDAO.findByClassId(classId);
        LOGGER.log(Level.INFO, "Fetched {0} students for classId: {1}.", new Object[]{students.size(), classId});

        // Tải danh sách buổi học
        loadSessionsForClass(classId);

        // Lấy buổi học đầu tiên nếu có
        String currentSessionId = null;
        if (!classSessions.isEmpty()) {
            ClassSession firstSession = classSessions.get(0);
            currentSessionId = firstSession.getId();
            attendanceModel.setSessionId(currentSessionId);
            attendanceModel.setSessionDate(firstSession.getStartTime().toLocalDate());
        }

        // Prepare the attendance data for each student
        ObservableList<StudentAttendanceData> attendanceList = FXCollections.observableArrayList();

        // Lấy dữ liệu bài tập về nhà cho buổi học hiện tại (nếu có)
        Map<String, HomeworkSubmissionModel> homeworkMap = new HashMap<>();
        if (currentSessionId != null) {
            try {
                List<HomeworkSubmissionModel> submissions = homeworkSubmissionDAO.getBySessionId(currentSessionId);
                LOGGER.log(Level.INFO, "Loaded {0} homework submissions for sessionId: {1}",
                        new Object[]{submissions.size(), currentSessionId});

                // Tạo map để dễ tra cứu theo ID học sinh
                for (HomeworkSubmissionModel submission : submissions) {
                    homeworkMap.put(submission.getStudentId(), submission);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error loading homework submissions", e);
            }
        }

        for (Student student : students) {
            // Lấy lịch sử metrics gần nhất từ database cho mỗi học sinh
            List<StudentAttendanceData> metricsHistory = studentDAO.getStudentMetricsHistory(student.getId(), classId);

            StudentAttendanceData studentData;

            // Nếu có lịch sử, lấy bản ghi gần nhất
            if (!metricsHistory.isEmpty()) {
                studentData = metricsHistory.get(0);
                LOGGER.log(Level.INFO, "Loaded latest metrics for student {0}: punctuality={1}, diligence={2}",
                        new Object[]{student.getId(), studentData.getPunctualityRating(), studentData.getDiligenceRating()});
            } else {
                // Nếu không có lịch sử, tạo mới với các giá trị mặc định
                studentData = new StudentAttendanceData(student);
                LOGGER.log(Level.INFO, "No metrics history found for student: {0}, using default values", student.getId());
            }

            // Thêm thông tin bài tập về nhà (nếu có)
            HomeworkSubmissionModel hwSubmission = homeworkMap.get(student.getId());
            if (hwSubmission != null) {
                // Điểm quan trọng: Sử dụng isSubmitted() từ model để xác định trạng thái checkbox
                studentData.setHomeworkSubmitted(hwSubmission.isSubmitted());
                studentData.setHomeworkGrade(hwSubmission.getGrade());
                studentData.setSubmissionDate(hwSubmission.getSubmissionTimestamp());
                LOGGER.log(Level.INFO, "Added homework data for student {0}: submitted={1}, grade={2}",
                        new Object[]{student.getId(), hwSubmission.isSubmitted(), hwSubmission.getGrade()});
            } else {
                // Mặc định nếu không có thông tin bài tập
                studentData.setHomeworkSubmitted(false);
                studentData.setHomeworkGrade(0.0);
                studentData.setSubmissionDate(null);
            }

            attendanceList.add(studentData);
        }

        // Set the list to the model and notify the view
        attendanceModel.getAttendanceList().clear();
        attendanceModel.getAttendanceList().addAll(attendanceList);
        attendanceModel.setClassId(classId);

        // Attach filtered list to the view
        FilteredList<StudentAttendanceData> filteredList = new FilteredList<>(attendanceModel.getAttendanceList());
        view.setFilteredAttendanceList(filteredList);

        // Notify the view that the data loading is complete
        view.showSuccess("Attendance data loaded successfully.");
    }

    /**
     * Tải danh sách buổi học cho lớp và cập nhật ComboBox số buổi.
     *
     * @param classId ID của lớp cần tải buổi học
     */
    private void loadSessionsForClass(String classId) {
        LOGGER.log(Level.INFO, "Loading sessions for classId: {0}", classId);
        try {
            // Lấy danh sách buổi học từ DB
            classSessions = classSessionDAO.findByClassId(classId);
            LOGGER.log(Level.INFO, "Loaded {0} sessions for classId: {1}",
                    new Object[]{classSessions.size(), classId});

            if (classSessions.isEmpty()) {
                // Nếu không có buổi học, thông báo và đặt list rỗng
                LOGGER.log(Level.INFO, "No sessions found for classId: {0}", classId);
                view.setAvailableSessions(FXCollections.observableArrayList(), null);
                return;
            }

            // Tạo danh sách số buổi (1, 2, 3,...) dựa trên số lượng buổi học
            ObservableList<Integer> sessionNumbers = FXCollections.observableArrayList(
                    IntStream.rangeClosed(1, classSessions.size())
                            .boxed()
                            .collect(Collectors.toList())
            );

            // Chọn buổi đầu tiên là mặc định
            Integer defaultSession = sessionNumbers.isEmpty() ? null : sessionNumbers.get(0);

            // Cập nhật ComboBox trong view
            view.setAvailableSessions(sessionNumbers, defaultSession);

            // Nếu có buổi học, tải thông tin buổi đầu tiên
            if (defaultSession != null) {
                ClassSession firstSession = classSessions.get(0);
                attendanceModel.setSessionId(firstSession.getId());
                attendanceModel.setSessionDate(firstSession.getStartTime().toLocalDate());
                view.setSessionNotes(firstSession.getCourseName());
                view.setSelectedDate(firstSession.getStartTime().toLocalDate());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading sessions for classId: " + classId, e);
            view.showError("Failed to load class sessions: " + e.getMessage());
        }
    }

    /**
     * Save modified attendance information back to the database including metrics.
     */
    public void saveInformation() {
        try {
            LOGGER.log(Level.INFO, "Saving attendance information for classId: {0}", attendanceModel.getClassId());

            // Lấy thông tin lớp và ngày từ model
            String classId = attendanceModel.getClassId();
            String sessionId = attendanceModel.getSessionId();
            LocalDate recordDate = attendanceModel.getSessionDate();
            List<StudentAttendanceData> attendanceList = attendanceModel.getAttendanceList();

            // Lưu thông tin điểm chuyên cần, đúng giờ và ghi chú
            HomeworkDAO homeworkDAO = DaoManager.getInstance().getHomeworkDAO();

            // Trước hết, lấy tất cả các metrics hiện có cho các học sinh trong lớp này
            Map<String, Long> existingMetricsMap = new HashMap<>();
            List<Map<String, Object>> existingMetrics = homeworkDAO.getStudentMetricsForClass(classId);
            for (Map<String, Object> metric : existingMetrics) {
                String studentId = (String) metric.get("student_id");
                Long metricId = (Long) metric.get("metric_id");
                existingMetricsMap.put(studentId, metricId);
            }

            List<Map<String, Object>> metricsDataList = new ArrayList<>();

            // Duyệt qua danh sách điểm danh và tạo dữ liệu để lưu
            for (StudentAttendanceData data : attendanceList) {
                String studentId = data.getStudent().getId();

                // Xử lý dữ liệu metrics (chuyên cần và đúng giờ)
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("studentId", studentId);
                metrics.put("classId", classId);
                metrics.put("recordDate", java.sql.Date.valueOf(recordDate));
                metrics.put("awarenessScore", data.getDiligenceRating());
                metrics.put("punctualityScore", data.getPunctualityRating());
                metrics.put("notes", data.getStudentSessionNotes());

                // Thêm metric_id nếu đã tồn tại cho học sinh này
                if (existingMetricsMap.containsKey(studentId)) {
                    metrics.put("metricId", existingMetricsMap.get(studentId));
                }

                metricsDataList.add(metrics);
            }

            // Lưu dữ liệu metrics vào database
            int metricsCount = homeworkDAO.saveStudentMetricsBatch(metricsDataList);
            LOGGER.log(Level.INFO, "Saved {0} student metrics records.", metricsCount);

            // Lưu dữ liệu điểm danh thông thường (gốc)
            attendanceModel.saveAttendanceData();

            // Gọi saveHomeworkSubmissions() riêng để lưu thông tin bài tập về nhà
            saveHomeworkSubmissions();

            // Thông báo thành công
            view.showSuccess("All data saved to the database successfully!");
            LOGGER.log(Level.INFO, "All attendance and related data saved successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save attendance data: " + e.getMessage(), e);
            view.showError("Failed to save attendance data: " + e.getMessage());
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
        view.showSuccess("Session date updated to: " + selectedDate);
    }

    public void saveHomeworkSubmissions() {
        if (attendanceModel.getSessionId() == null || attendanceModel.getSessionId().isEmpty()) {
            view.showError("No session ID found. Cannot save homework submissions.");
            LOGGER.log(Level.WARNING, "Cannot save homework submissions because sessionId is null or empty.");
            return;
        }

        try {
            LOGGER.log(Level.INFO, "Saving homework submissions for sessionId: {0}", attendanceModel.getSessionId());

            // Trước tiên, lấy tất cả các bản ghi hiện có của buổi học này để giữ ID
            Map<String, HomeworkSubmissionModel> existingSubmissions = new HashMap<>();
            List<HomeworkSubmissionModel> currentSubmissions = homeworkSubmissionDAO.getBySessionId(attendanceModel.getSessionId());

            for (HomeworkSubmissionModel submission : currentSubmissions) {
                // Tạo key bằng cách kết hợp studentId và homeworkId
                String key = submission.getStudentId() + "_" + submission.getHomeworkId();
                existingSubmissions.put(key, submission);
            }

            List<HomeworkSubmissionModel> submissionsToSave = new ArrayList<>();
            for (StudentAttendanceData data : attendanceModel.getAttendanceList()) {
                String studentId = data.getStudent().getId();
                String homeworkId = attendanceModel.getSessionId();
                String key = studentId + "_" + homeworkId;

                // Kiểm tra xem có submission hiện có không
                HomeworkSubmissionModel submission = existingSubmissions.containsKey(key)
                        ? existingSubmissions.get(key)
                        : new HomeworkSubmissionModel();

                // Nếu là submission mới, cần tạo ID
                if (submission.getStudentSubmissionId() == null || submission.getStudentSubmissionId().isEmpty()) {
                    submission.setStudentSubmissionId(UUID.randomUUID().toString());
                }

                // Cập nhật các thông tin khác
                submission.setStudentId(studentId);
                submission.setHomeworkId(homeworkId);
                submission.setSubmitted(data.isHomeworkSubmitted());  // Sử dụng trường này với is_submitted trong DB
                submission.setGrade(data.getHomeworkGrade());
                submission.setSubmissionTimestamp(data.getSubmissionDate() != null
                        ? data.getSubmissionDate()
                        : LocalDateTime.now());
                submission.setCheckedInSessionId(attendanceModel.getSessionId());

                submissionsToSave.add(submission);

                LOGGER.log(Level.INFO, "Prepared submission for studentId: {0} with homeworkId: {1}, submissionId: {2}",
                        new Object[]{studentId, homeworkId, submission.getStudentSubmissionId()});
            }

            // Save or update the homework submissions in batch
            homeworkSubmissionDAO.saveOrUpdateBatch(submissionsToSave);
            LOGGER.log(Level.INFO, "Saved {0} homework submissions to the database.", submissionsToSave.size());
            view.showSuccess("Homework submissions saved successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save homework submissions.", e);
            view.showError("Failed to save homework submissions: " + e.getMessage());
        }
    }

    /**
     * Handle session change from view and reload data.
     */
    public void handleSessionChange() {
        Integer sessionNumber = view.getSelectedSessionNumber();
        if (sessionNumber == null) {
            view.showError("No session selected.");
            LOGGER.log(Level.WARNING, "No session selected during session change.");
            return;
        }

        LOGGER.log(Level.INFO, "Handling session change for session number: {0}", sessionNumber);

        // Tính toán index của session (số buổi học bắt đầu từ 1 nhưng index bắt đầu từ 0)
        int sessionIndex = sessionNumber - 1;

        // Kiểm tra index có hợp lệ không
        if (sessionIndex < 0 || sessionIndex >= classSessions.size()) {
            LOGGER.log(Level.WARNING, "Invalid session index: {0}, total sessions: {1}",
                    new Object[]{sessionIndex, classSessions.size()});
            view.showError("Invalid session number selected.");
            return;
        }

        // Lấy thông tin buổi học từ danh sách đã lưu
        ClassSession session = classSessions.get(sessionIndex);
        LOGGER.log(Level.INFO, "Loaded session details: {0}", session.toString());

        // Cập nhật thông tin buổi học vào model và view
        view.setSessionNotes(session.getCourseName());
        view.setSelectedDate(session.getStartTime().toLocalDate());
        attendanceModel.setSessionDate(session.getStartTime().toLocalDate());
        attendanceModel.setSessionId(session.getId());

        // Tải dữ liệu bài tập về nhà cho buổi học này nếu cần
        LOGGER.log(Level.INFO, "Calling loadHomeworkSubmissionsForSession with sessionId: {0}", session.getId());
        loadHomeworkSubmissionsForSession(session.getId());

        view.refreshView();
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
     * Update all homework statuses based on the header checkbox in the view.
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
     * Apply filters for punctuality and diligence based on view settings.
     * Filters the currently loaded students based on their metrics.
     */
    public void applyFilters() {
        int punctualityFilter = view.getPunctualityFilterValue();
        int diligenceFilter = view.getDiligenceFilterValue();
        String classId = view.getMainController().getCurrentClassId();
        if (classId == null || classId.isEmpty()) {
            LOGGER.log(Level.WARNING, "Cannot apply filters: No class selected");
            view.showError("Vui lòng chọn một lớp học trước khi lọc");
            return;
        }
        LOGGER.log(Level.INFO, "Applying filters: punctuality >= {0}, diligence >= {1} for class {2}",
                new Object[]{punctualityFilter, diligenceFilter, classId});
        // Lấy danh sách học sinh trong lớp
        List<Student> students = studentDAO.findByClassId(classId);
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

        // Cập nhật view
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

            // Cập nhật view để hiển thị dữ liệu mới
            view.refreshTable();
            LOGGER.log(Level.INFO, "Đã tải dữ liệu bài tập cho buổi học: {0}", sessionId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải dữ liệu bài tập về nhà", e);
            view.showError("Đã xảy ra lỗi khi tải dữ liệu bài tập về nhà: " + e.getMessage());
        }
    }

}