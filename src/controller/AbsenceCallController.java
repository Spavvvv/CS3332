
package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.ClassSession;
import src.model.absence.AbsenceRecord;
import src.model.attendance.Attendance;
import src.model.person.Student;
import utils.DaoManager;
import src.dao.StudentDAO; // Assuming you still need this for student list by class

import view.components.AbsenceCallView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime; // For check_in_time
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID; // For generating attendance_id
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AbsenceCallController {

    private static final Logger LOGGER = Logger.getLogger(AbsenceCallController.class.getName());

    private final AbsenceCallView view;
    private final AttendanceController attendanceController; // Your existing controller for attendance ops
    private final StudentDAO studentDAO; // For fetching students by class

    private final ObservableList<AbsenceRecord> absenceData;
    private final ObservableList<ClassSession> sessionListData;

    private boolean dataChanged = false;

    // Status constants from your original controller, can be refined based on DB values
    private static final String STATUS_CHUA_DIEM_DANH = "Chưa điểm danh";
    private static final String STATUS_CO_MAT = "Có mặt";
    private static final String STATUS_VANG = "Vắng";
    private static final String STATUS_XIN_PHEP = "Xin phép";
    private static final String STATUS_DI_TRE = "Đi trễ";


    public AbsenceCallController(AbsenceCallView view) {
        this.view = Objects.requireNonNull(view, "AbsenceCallView cannot be null");
        // Assuming AttendanceController is responsible for all CRUD on 'attendance' table
        this.attendanceController = new AttendanceController();
        this.studentDAO = DaoManager.getInstance().getStudentDAO();
        this.absenceData = FXCollections.observableArrayList();
        this.sessionListData = FXCollections.observableArrayList();
    }

    public void initializeController() {
        view.setSessionItems(sessionListData);
        view.setAbsenceTableItems(absenceData);
        loadSessionList();
        // applyFilters(); // Let's call this after a session is selected or by default
    }

    public void loadSessionList() {
        try {
            // Assuming attendanceController.getAllClassSessions() fetches from class_sessions
            List<ClassSession> sessionsFromDb = attendanceController.getClassSessionsByClassId(
                    view.getMainController().getCurrentClassId());
            sessionListData.setAll(sessionsFromDb);
            if (view != null && !sessionListData.isEmpty()) {
                // Optionally select the first session and load its data
                // view.selectSession(sessionListData.get(0));
                // applyFilters();
            } else if (view != null) {
                view.showInfo("Không có buổi học nào được tìm thấy.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load session list.", e);
            if (view != null) view.showError("Lỗi tải danh sách buổi học: " + e.getMessage());
        }
    }

    public void applyFilters() {
        if (view == null) return;

        ClassSession selectedSession = view.getSelectedSession();

        if (dataChanged) {
            // Here you would typically ask the user if they want to discard changes
            // For now, let's assume they discard. In a real app, use a confirmation dialog.
            // if (!view.showConfirmation("Bạn có thay đổi chưa lưu. Bỏ qua các thay đổi này?")) {
            //     return; // User chose not to discard, so don't proceed with filter change
            // }
            LOGGER.info("Unsaved changes detected. For now, proceeding with filter change.");
        }


        if (selectedSession != null && selectedSession.getId() != null) {
            loadAbsenceRecordsForSession(selectedSession);
        } else {
            absenceData.clear();
            if (view != null) view.refreshTable();
            setDataChanged(false);
        }
    }

    private void loadAbsenceRecordsForSession(ClassSession session) {
        if (session == null || session.getId() == null || session.getClassId() == null) {
            LOGGER.warning("Invalid session (null or missing ID/ClassID) provided for loading records.");
            absenceData.clear();
            if (view != null) view.refreshTable();
            return;
        }

        String sessionId = session.getId();
        String classId = session.getClassId(); // ID of the class (e.g. "10A1")
        LocalDate sessionDate = session.getDate();
        // Assuming session.getCourseName() gives the subject or general course name,
        // and you want the specific class name (like "10A1") for the AbsenceRecord's className field.
        // You might need to fetch the actual class name if session.getCourseName() isn't it.
        // For now, let's assume session.getClassName() or a lookup is needed if courseName is not the specific class identifier.
        // If ClassSession has a getActualClassName() or similar, use that.
        // For this example, I'll assume you can get a suitable display class name from the session or via student.
        // String displayClassNameForRecord = session.getCourseName(); // Placeholder - adjust if needed.


        try {
            List<Student> studentsInClass = studentDAO.findByClassId(classId);
            if (studentsInClass == null) studentsInClass = new ArrayList<>();

            // Fetch existing attendance records from the 'attendance' table for this session
            List<Attendance> existingAttendancesForSession = attendanceController.getAttendanceBySessionId(sessionId);
            Map<String, Attendance> attendanceMap = existingAttendancesForSession.stream()
                    .filter(att -> att.getStudentId() != null)
                    .collect(Collectors.toMap(Attendance::getStudentId, Function.identity(), (att1, att2) -> att1));

            absenceData.clear();
            int displayIdCounter = 1;

            for (Student student : studentsInClass) {
                if (student == null || student.getId() == null) continue;

                Attendance studentAttendance = attendanceMap.get(student.getId());
                String status;
                String note = "";
                boolean called = false;
                boolean approved = false; // Maps to has_permission in DB
                // String studentDisplayClassName = student.getClassName(); // Assuming student has a class name

                if (studentAttendance != null) {
                    // Map DB fields to AbsenceRecord fields
                    status = studentAttendance.getStatus(); // Directly use status from DB
                    note = studentAttendance.getNotes() == null ? "" : studentAttendance.getNotes();
                    called = studentAttendance.isCalled();
                    approved = studentAttendance.hasPermission();
                } else {
                    status = STATUS_CHUA_DIEM_DANH; // Default for new records
                }

                AbsenceRecord record = new AbsenceRecord(
                        displayIdCounter++,
                        student.getId(),
                        student.getName(),
                        session.getCourseName(), // Using student's class name for the record
                        sessionDate,
                        status,
                        note,
                        called,
                        approved,
                        sessionId // Storing sessionId in AbsenceRecord is crucial for saving
                );

                // Add listeners to mark dataChanged when properties are modified in the UI
                record.statusProperty().addListener((obs, oldV, newV) -> setDataChanged(true));
                record.noteProperty().addListener((obs, oldV, newV) -> setDataChanged(true));
                record.calledProperty().addListener((obs, oldV, newV) -> setDataChanged(true));
                record.approvedProperty().addListener((obs, oldV, newV) -> setDataChanged(true));

                absenceData.add(record);
            }

            setDataChanged(false); // Reset after loading
            if (view != null) view.refreshTable();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load absence records for session: " + sessionId, e);
            if (view != null) view.showError("Lỗi tải danh sách điểm danh: " + e.getMessage());
        }
    }

    public void refreshData() {
        ClassSession currentSession = null;
        if (view != null) {
            currentSession = view.getSelectedSession();
        }

        if (dataChanged) {
            // Ask for confirmation before discarding changes
            LOGGER.info("Unsaved changes detected during refresh. For now, proceeding.");
            // if (!view.showConfirmation("Bạn có thay đổi chưa lưu. Tiếp tục làm mới và bỏ qua thay đổi?")) {
            //     return;
            // }
        }

        loadSessionList(); // Reloads ClassSession list

        if (currentSession != null && view != null) {
            final String currentSessionId = currentSession.getId();
            sessionListData.stream()
                    .filter(cs -> cs.getId() != null && cs.getId().equals(currentSessionId))
                    .findFirst()
                    .ifPresent(view::selectSession); // Re-select the session in the view
        }
        // applyFilters will be called by the view's session selector listener,
        // or if not, call it explicitly if a session is selected.
        if (view.getSelectedSession() != null) {
            loadAbsenceRecordsForSession(view.getSelectedSession()); // Reload records for the re-selected session
        } else {
            absenceData.clear(); // Clear if no session is selected after refresh
            if(view != null) view.refreshTable();
            setDataChanged(false);
        }
    }

    public void handleSaveChanges() {
        if (view == null) return;

        if (!dataChanged) {
            view.showInfo("Không có thay đổi nào để lưu.");
            return;
        }

        ClassSession currentSession = view.getSelectedSession();
        if (currentSession == null || currentSession.getId() == null) {
            LOGGER.warning("Cannot save changes, no session selected or session ID is null.");
            view.showError("Vui lòng chọn một buổi học để lưu.");
            return;
        }

        String currentSessionId = currentSession.getId();
        LocalDate currentSessionDate = currentSession.getDate(); // From class_sessions table

        List<Attendance> recordsToUpsert = new ArrayList<>();

        for (AbsenceRecord ar : absenceData) {
            // Only process records that have been interacted with beyond "Chưa điểm danh" or if they have notes/flags
            boolean isDefaultUnchanged = STATUS_CHUA_DIEM_DANH.equals(ar.getStatus()) &&
                    !ar.isCalled() &&
                    !ar.isApproved() &&
                    (ar.getNote() == null || ar.getNote().isEmpty());

            // If you want to save "Chưa điểm danh" as an explicit record, remove `!isDefaultUnchanged`
            // For now, we only save if it's not the pristine default state.
            // However, if an existing record was loaded, it should be updated even if set back to "Chưa điểm danh".
            // The logic for this check might need refinement based on whether AbsenceRecord has an "isDirty" or "isPersisted" flag.

            // Let's simplify: attempt to upsert all records from the view.
            // The DAO/AttendanceController's upsert logic should handle insert vs update.

            Attendance attendance = new Attendance();
            // attendance.setAttendanceId(); // This will be set if updating, or generated if inserting by AttendanceController/DAO
            attendance.setSessionId(currentSessionId);
            attendance.setStudentId(ar.getStudentId());
            attendance.setStatus(ar.getStatus()); // Store the status string directly
            attendance.setNotes(ar.getNote());
            attendance.setCalled(ar.isCalled());
            attendance.setHasPermission(ar.isApproved());
            attendance.setAbsenceDate(currentSessionDate); // Date of the session

            // Determine 'present' based on status
            if (STATUS_CO_MAT.equals(ar.getStatus()) || STATUS_DI_TRE.equals(ar.getStatus())) {
                attendance.setPresent(true);
                // Set check_in_time only if marking as present/late and it's not already set
                // This logic should ideally be in AttendanceController or DAO if it needs to check existing DB value
                // For now, if present/late, we can assume a check-in occurs now.
                attendance.setCheckInTime(LocalDateTime.now());
            } else {
                attendance.setPresent(false);
                attendance.setCheckInTime(null); // Clear check-in time if not present
            }
            // record_time is usually set by DB (CURRENT_TIMESTAMP)

            recordsToUpsert.add(attendance);
        }

        if (recordsToUpsert.isEmpty()) {
            view.showInfo("Không có thông tin điểm danh hợp lệ để lưu.");
            setDataChanged(false);
            return;
        }

        try {
            // Assuming attendanceController has a method to handle batch upsert
            // This method would iterate, check if exists, then insert or update.
            int successCount = attendanceController.batchUpsertAttendance(recordsToUpsert);

            view.showInfo("Đã lưu thành công " + successCount + " bản ghi điểm danh.");
            setDataChanged(false);
            // Refresh data from DB to ensure view is consistent
            loadAbsenceRecordsForSession(currentSession); // Reload the current session's data

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving attendance data.", e);
            view.showError("Lỗi khi lưu dữ liệu điểm danh: " + e.getMessage());
        }
    }


    public void setDataChanged(boolean changed) {
        this.dataChanged = changed;
        // You could notify the view to enable/disable save button, e.g., view.setSaveButtonEnabled(changed);
    }



}
