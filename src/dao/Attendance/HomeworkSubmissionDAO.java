package src.dao.Attendance;

import src.model.attendance.HomeworkSubmissionModel;
import src.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object for HomeworkSubmissionModel.
 * Handles database operations for the student_homework_submissions table.
 */
public class HomeworkSubmissionDAO {
    private final Connection connection;

    /**
     * Constructor initializes the database connection.
     * @throws SQLException if a database error occurs
     */
    public HomeworkSubmissionDAO() throws SQLException {
        this.connection = DatabaseConnection.getConnection();
    }

    /**
     * Maps a ResultSet row to a HomeworkSubmissionModel object.
     * @param rs The ResultSet containing the data to map
     * @return A HomeworkSubmissionModel object populated with data from the ResultSet
     * @throws SQLException if a database access error occurs
     */
    private HomeworkSubmissionModel mapResultSetToModel(ResultSet rs) throws SQLException {
        HomeworkSubmissionModel model = new HomeworkSubmissionModel();

        model.setStudentSubmissionId(rs.getString("student_submission_id"));
        model.setStudentId(rs.getString("student_id"));
        model.setHomeworkId(rs.getString("homework_id"));
        model.setSubmitted(rs.getBoolean("is_submitted"));
        model.setGrade(rs.getDouble("grade"));

        Timestamp timestamp = rs.getTimestamp("submission_timestamp");
        if (timestamp != null) {
            model.setSubmissionTimestamp(timestamp.toLocalDateTime());
        }

        model.setEvaluatorNotes(rs.getString("evaluator_notes"));
        model.setCheckedInSessionId(rs.getString("checked_in_session_id"));

        return model;
    }

    /**
     * Retrieves a submission for a specific student and homework.
     * @param studentId The ID of the student
     * @param homeworkId The ID of the homework
     * @return The submission model, or null if not found
     * @throws SQLException if a database error occurs
     */
    public HomeworkSubmissionModel getByStudentAndHomework(String studentId, String homeworkId) throws SQLException {
        String sql = "SELECT * FROM student_homework_submissions WHERE student_id = ? AND homework_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, homeworkId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToModel(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves submissions by status for a specific homework assignment.
     * @param homeworkId The ID of the homework assignment
     * @param submitted The submission status to filter by (true for submitted, false for not submitted)
     * @return List of submission models matching the criteria
     * @throws SQLException if a database error occurs
     */
    public List<HomeworkSubmissionModel> getSubmissionsByStatus(String homeworkId, boolean submitted) throws SQLException {
        List<HomeworkSubmissionModel> submissions = new ArrayList<>();
        String sql = "SELECT * FROM student_homework_submissions WHERE homework_id = ? AND is_submitted = ? ORDER BY student_id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, homeworkId);
            stmt.setBoolean(2, submitted);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToModel(rs));
                }
            }
        }
        return submissions;
    }

    /**
     * Retrieves all homework submissions for a specific session and homework.
     * @param sessionId The ID of the class session where homework was checked
     * @param homeworkId The ID of the homework assignment
     * @return A list of HomeworkSubmissionModel objects for the session and homework
     * @throws SQLException if a database access error occurs
     */
    public List<HomeworkSubmissionModel> getBySessionAndHomeworkId(String sessionId, String homeworkId) throws SQLException {
        List<HomeworkSubmissionModel> submissions = new ArrayList<>();
        String sql = "SELECT * FROM student_homework_submissions WHERE checked_in_session_id = ? AND homework_id = ? ORDER BY student_id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, homeworkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToModel(rs));
                }
            }
        }
        return submissions;
    }

    /**
     * Creates a new homework submission record.
     * @param submission The submission model to create
     * @return True if successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean create(HomeworkSubmissionModel submission) throws SQLException {
        String sql = "INSERT INTO student_homework_submissions " +
                "(student_submission_id, student_id, homework_id, is_submitted, grade, " +
                "submission_timestamp, evaluator_notes, checked_in_session_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Generate a UUID if not provided
            if (submission.getStudentSubmissionId() == null || submission.getStudentSubmissionId().isEmpty()) {
                submission.setStudentSubmissionId(UUID.randomUUID().toString());
            }

            stmt.setString(1, submission.getStudentSubmissionId());
            stmt.setString(2, submission.getStudentId());
            stmt.setString(3, submission.getHomeworkId());
            stmt.setBoolean(4, submission.isSubmitted());
            stmt.setDouble(5, submission.getGrade());

            LocalDateTime timestamp = submission.getSubmissionTimestamp();
            if (timestamp != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(timestamp));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }

            stmt.setString(7, submission.getEvaluatorNotes());
            stmt.setString(8, submission.getCheckedInSessionId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Updates an existing homework submission record.
     * @param submission The submission model to update
     * @return True if successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean update(HomeworkSubmissionModel submission) throws SQLException {
        String sql = "UPDATE student_homework_submissions SET " +
                "is_submitted = ?, grade = ?, submission_timestamp = ?, " +
                "evaluator_notes = ?, checked_in_session_id = ? " +
                "WHERE student_submission_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, submission.isSubmitted());
            stmt.setDouble(2, submission.getGrade());

            LocalDateTime timestamp = submission.getSubmissionTimestamp();
            if (timestamp != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(timestamp));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }

            stmt.setString(4, submission.getEvaluatorNotes());
            stmt.setString(5, submission.getCheckedInSessionId());
            stmt.setString(6, submission.getStudentSubmissionId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Batch updates homework submission status for multiple students in a session.
     * @param sessionId The ID of the session where homework is being reviewed
     * @param homeworkId The ID of the homework assignment
     * @param studentSubmissions Map of student IDs to their submission status (true/false)
     * @return Number of records successfully updated
     * @throws SQLException if a database access error occurs
     */
    public int batchUpdateForSession(String sessionId, String homeworkId,
                                     Map<String, Boolean> studentSubmissions) throws SQLException {
        String sql = "UPDATE student_homework_submissions SET " +
                "is_submitted = ?, checked_in_session_id = ?, submission_timestamp = ? " +
                "WHERE student_id = ? AND homework_id = ?";

        boolean autoCommitStatus = connection.getAutoCommit();
        int totalUpdated = 0;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (Map.Entry<String, Boolean> entry : studentSubmissions.entrySet()) {
                    String studentId = entry.getKey();
                    Boolean isSubmitted = entry.getValue();

                    stmt.setBoolean(1, isSubmitted);
                    stmt.setString(2, sessionId);

                    if (isSubmitted) {
                        stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    } else {
                        stmt.setNull(3, Types.TIMESTAMP);
                    }

                    stmt.setString(4, studentId);
                    stmt.setString(5, homeworkId);
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                for (int result : results) {
                    if (result > 0) {
                        totalUpdated += result;
                    }
                }

                connection.commit();
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommitStatus);
        }

        return totalUpdated;
    }

    /**
     * Creates homework submission records for all students in a session.
     * @param sessionId The ID of the class session
     * @param homeworkId The ID of the homework assignment
     * @param studentIds List of student IDs who should have homework records
     * @return Number of records successfully created
     * @throws SQLException if a database access error occurs
     */
    public int createForAllStudentsInSession(String sessionId, String homeworkId,
                                             List<String> studentIds) throws SQLException {
        String sql = "INSERT INTO student_homework_submissions " +
                "(student_submission_id, student_id, homework_id, is_submitted, grade, " +
                "submission_timestamp, evaluator_notes, checked_in_session_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        boolean autoCommitStatus = connection.getAutoCommit();
        int totalCreated = 0;

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (String studentId : studentIds) {
                    // Skip if a record already exists for this student and homework
                    if (getByStudentAndHomework(studentId, homeworkId) != null) {
                        continue;
                    }

                    String submissionId = UUID.randomUUID().toString();

                    stmt.setString(1, submissionId);
                    stmt.setString(2, studentId);
                    stmt.setString(3, homeworkId);
                    stmt.setBoolean(4, false); // Default not submitted
                    stmt.setDouble(5, 0.0);    // Default grade
                    stmt.setNull(6, Types.TIMESTAMP); // No submission yet
                    stmt.setString(7, "");     // No evaluator notes
                    stmt.setString(8, sessionId);
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                for (int result : results) {
                    if (result > 0) {
                        totalCreated += result;
                    }
                }

                connection.commit();
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommitStatus);
        }

        return totalCreated;
    }

    /**
     * Saves homework submissions for a list of HomeworkSubmissionModel objects.
     * This method matches the controller's usage pattern, handling both creation and updates.
     *
     * @param submissions List of HomeworkSubmissionModel objects to save
     * @param sessionId The ID of the class session where homework is being checked
     * @return Number of records successfully processed
     * @throws SQLException if a database access error occurs
     */
    public int saveHomeworkSubmissions(List<HomeworkSubmissionModel> submissions, String sessionId) throws SQLException {
        if (submissions == null || submissions.isEmpty()) {
            return 0;
        }

        boolean autoCommitStatus = connection.getAutoCommit();
        int totalProcessed = 0;

        try {
            connection.setAutoCommit(false);

            for (HomeworkSubmissionModel submission : submissions) {
                // Make sure sessionId is set
                submission.setCheckedInSessionId(sessionId);

                // Set submission timestamp if it's being marked as submitted
                if (submission.isSubmitted() && submission.getSubmissionTimestamp() == null) {
                    submission.setSubmissionTimestamp(LocalDateTime.now());
                }

                // Check if this is an existing or new submission
                HomeworkSubmissionModel existingSubmission =
                        getByStudentAndHomework(submission.getStudentId(), submission.getHomeworkId());

                if (existingSubmission == null) {
                    // This is a new submission
                    if (create(submission)) {
                        totalProcessed++;
                    }
                } else {
                    // Update existing submission
                    submission.setStudentSubmissionId(existingSubmission.getStudentSubmissionId());
                    if (update(submission)) {
                        totalProcessed++;
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommitStatus);
        }

        return totalProcessed;
    }

    /**
     * Retrieves all homework submissions by session ID and homework ID, with student information joined.
     * This method joins the student table to get student names and other information.
     * @param sessionId The ID of the class session
     * @param homeworkId The ID of the homework assignment
     * @return A list of maps containing joined data from submissions and students
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> getBySessionAndHomeworkIdWithStudentInfo(String sessionId, String homeworkId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        String sql = "SELECT s.*, st.name as student_name " +
                "FROM student_homework_submissions s " +
                "JOIN students st ON s.student_id = st.id " +
                "WHERE s.checked_in_session_id = ? AND s.homework_id = ? " +
                "ORDER BY st.name";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, homeworkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    // Add submission data
                    row.put("studentSubmissionId", rs.getString("student_submission_id"));
                    row.put("studentId", rs.getString("student_id"));
                    row.put("homeworkId", rs.getString("homework_id"));
                    row.put("isSubmitted", rs.getBoolean("is_submitted"));
                    row.put("grade", rs.getDouble("grade"));

                    Timestamp submissionTimestamp = rs.getTimestamp("submission_timestamp");
                    row.put("submissionTimestamp", submissionTimestamp != null ?
                            submissionTimestamp.toLocalDateTime() : null);

                    row.put("evaluatorNotes", rs.getString("evaluator_notes"));
                    row.put("checkedInSessionId", rs.getString("checked_in_session_id"));
                    row.put("studentName", rs.getString("student_name"));

                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * Gets homework submissions grouped by session for reporting purposes.
     * @param homeworkId The ID of the homework to report on
     * @return A map where keys are session IDs and values are lists of submissions in that session
     * @throws SQLException if a database access error occurs
     */
    public Map<String, List<HomeworkSubmissionModel>> getByHomeworkIdGroupedBySession(String homeworkId) throws SQLException {
        Map<String, List<HomeworkSubmissionModel>> sessionSubmissions = new HashMap<>();

        String sql = "SELECT * FROM student_homework_submissions WHERE homework_id = ? ORDER BY checked_in_session_id, student_id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, homeworkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HomeworkSubmissionModel submission = mapResultSetToModel(rs);
                    String sessionId = submission.getCheckedInSessionId();

                    if (sessionId != null && !sessionId.isEmpty()) {
                        sessionSubmissions.computeIfAbsent(sessionId, k -> new ArrayList<>())
                                .add(submission);
                    }
                }
            }
        }
        return sessionSubmissions;
    }

    /**
     * Retrieves homework submissions for a specific session, including all homework assignments.
     * This is useful when reviewing all homework in a single session.
     * @param sessionId The ID of the class session
     * @return A list of HomeworkSubmissionModel objects for the session
     * @throws SQLException if a database access error occurs
     */
    public List<HomeworkSubmissionModel> getBySessionId(String sessionId) throws SQLException {
        List<HomeworkSubmissionModel> submissions = new ArrayList<>();

        String sql = "SELECT * FROM student_homework_submissions WHERE checked_in_session_id = ? ORDER BY homework_id, student_id";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(mapResultSetToModel(rs));
                }
            }
        }
        return submissions;
    }

    /**
     * Updates the submission status for a specific student's homework.
     * @param studentId The ID of the student
     * @param homeworkId The ID of the homework assignment
     * @param isSubmitted The new submission status
     * @return true if the update was successful, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateSubmissionStatus(String studentId, String homeworkId, boolean isSubmitted) throws SQLException {
        String sql = "UPDATE student_homework_submissions SET is_submitted = ?, submission_timestamp = ? WHERE student_id = ? AND homework_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, isSubmitted);

            if (isSubmitted) {
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }

            stmt.setString(3, studentId);
            stmt.setString(4, homeworkId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }


    /**
     * Retrieves all homework submissions with detailed information for a specific homework.
     * @param homeworkId The ID of the homework assignment to report on
     * @return A list of maps containing detailed information about submissions and students
     * @throws SQLException if a database error occurs
     */
    public List<Map<String, Object>> getDetailedSubmissionReport(String homeworkId) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();

        String sql = "SELECT shs.*, s.name AS student_name, h.title AS homework_title, " +
                "cs.session_date, c.class_name " +
                "FROM student_homework_submissions shs " +
                "JOIN students s ON shs.student_id = s.id " +
                "JOIN homework h ON shs.homework_id = h.homework_id " +
                "LEFT JOIN class_sessions cs ON shs.checked_in_session_id = cs.session_id " +
                "LEFT JOIN classes c ON h.class_id = c.class_id " +
                "WHERE shs.homework_id = ? " +
                "ORDER BY s.name";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, homeworkId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    // Submission data
                    row.put("studentSubmissionId", rs.getString("student_submission_id"));
                    row.put("studentId", rs.getString("student_id"));
                    row.put("homeworkId", rs.getString("homework_id"));
                    row.put("isSubmitted", rs.getBoolean("is_submitted"));
                    row.put("grade", rs.getDouble("grade"));

                    Timestamp submissionTimestamp = rs.getTimestamp("submission_timestamp");
                    row.put("submissionTimestamp", submissionTimestamp != null ?
                            submissionTimestamp.toLocalDateTime() : null);

                    row.put("evaluatorNotes", rs.getString("evaluator_notes"));
                    row.put("checkedInSessionId", rs.getString("checked_in_session_id"));

                    // Related data
                    row.put("studentName", rs.getString("student_name"));
                    row.put("homeworkTitle", rs.getString("homework_title"));
                    row.put("sessionDate", rs.getDate("session_date"));
                    row.put("className", rs.getString("class_name"));

                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * Lấy danh sách bài nộp của học viên trong một lớp học dựa trên classId.
     *
     * @param classId ID của lớp học.
     * @return Danh sách `HomeworkSubmissionModel` của các bài nộp.
     * @throws SQLException nếu lỗi xảy ra khi truy vấn database.
     */
    public List<HomeworkSubmissionModel> getSubmissionsByClassId(String classId) throws SQLException {
        List<HomeworkSubmissionModel> submissions = new ArrayList<>();
        String sql = "SELECT shs.* " +
                "FROM student_homework_submissions shs " +
                "JOIN students s ON shs.student_id = s.id " +
                "JOIN classes c ON s.class_id = c.class_id " +
                "WHERE c.class_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, classId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HomeworkSubmissionModel submission = mapResultSetToModel(rs);
                    submissions.add(submission);
                }
            }
        }

        return submissions;
    }

    public void saveOrUpdateBatch(List<HomeworkSubmissionModel> submissions) throws SQLException {
        if (submissions.isEmpty()) {
            return;
        }

        String checkExistingQuery = "SELECT student_submission_id FROM student_homework_submissions " +
                "WHERE student_id = ? AND homework_id = ?";

        String updateQuery = "UPDATE student_homework_submissions SET " +
                "is_submitted = ?, grade = ?, submission_timestamp = ?, " +
                "checked_in_session_id = ?, evaluator_notes = ? " +
                "WHERE student_submission_id = ?";

        String insertQuery = "INSERT INTO student_homework_submissions " +
                "(student_submission_id, student_id, homework_id, is_submitted, grade, " +
                "submission_timestamp, checked_in_session_id, evaluator_notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkExistingQuery);
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

            for (HomeworkSubmissionModel submission : submissions) {
                // Kiểm tra xem bản ghi đã tồn tại chưa
                checkStmt.setString(1, submission.getStudentId());
                checkStmt.setString(2, submission.getHomeworkId());

                ResultSet rs = checkStmt.executeQuery();
                boolean exists = rs.next();

                if (exists) {
                    // Cập nhật bản ghi hiện có
                    updateStmt.setBoolean(1, submission.isSubmitted());
                    updateStmt.setDouble(2, submission.getGrade());
                    updateStmt.setTimestamp(3, submission.getSubmissionTimestamp() != null ?
                            Timestamp.valueOf(submission.getSubmissionTimestamp()) : null);
                    updateStmt.setString(4, submission.getCheckedInSessionId());
                    updateStmt.setString(5, submission.getEvaluatorNotes());
                    updateStmt.setString(6, submission.getStudentSubmissionId());
                    updateStmt.addBatch();
                } else {
                    // Thêm bản ghi mới
                    insertStmt.setString(1, submission.getStudentSubmissionId());
                    insertStmt.setString(2, submission.getStudentId());
                    insertStmt.setString(3, submission.getHomeworkId());
                    insertStmt.setBoolean(4, submission.isSubmitted());
                    insertStmt.setDouble(5, submission.getGrade());
                    insertStmt.setTimestamp(6, submission.getSubmissionTimestamp() != null ?
                            Timestamp.valueOf(submission.getSubmissionTimestamp()) : null);
                    insertStmt.setString(7, submission.getCheckedInSessionId());
                    insertStmt.setString(8, submission.getEvaluatorNotes());
                    insertStmt.addBatch();
                }

                rs.close();
            }

            // Thực thi các batch
            updateStmt.executeBatch();
            insertStmt.executeBatch();
        }
    }



}