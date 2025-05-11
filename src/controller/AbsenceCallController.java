package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import src.model.absence.AbsenceRecord;
import view.components.AbsenceCallView;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

// Import the DaoManager
import utils.DaoManager;
// Import the specific DAO class if you need its type for the instance variable
import src.dao.AbsenceRecordDAO;


/**
 * Controller for the Absence Call functionality
 */
public class AbsenceCallController {

    private static final Logger LOGGER = Logger.getLogger(AbsenceCallController.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy"); // Formatter for view dates

    private final AbsenceCallView view;
    // Keep the type declaration, but get the instance from DaoManager
    private final AbsenceRecordDAO absenceRecordDAO;
    private ObservableList<AbsenceRecord> absenceRecords = FXCollections.observableArrayList();

    public AbsenceCallController(AbsenceCallView view) {
        this.view = view;
        // Initialize the DAO using the DaoManager singleton
        this.absenceRecordDAO = DaoManager.getInstance().getAbsenceRecordDAO();
        initializeController();
    }

    private void initializeController() {
        // Initialize controller logic
        loadAbsenceData();

        // Set data to view
        view.setAbsenceData(absenceRecords);
    }

    /**
     * Load absence data from database using DAO
     */
    private void loadAbsenceData() {
        absenceRecords.clear(); // Clear existing data
        try {
            List<AbsenceRecord> loadedRecords = absenceRecordDAO.findAll();
            absenceRecords.addAll(loadedRecords);
            LOGGER.log(Level.INFO, "Successfully loaded {0} absence records.", loadedRecords.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading absence data from database.", e);
            // Optionally, show an error message to the user via the view
            view.showError("Lỗi tải dữ liệu vắng mặt."); // Assuming view has showError method
        } catch (Exception e) { // Catch any other unexpected exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error loading absence data.", e);
            view.showError("Đã xảy ra lỗi không mong muốn khi tải dữ liệu.");
        }
    }

    /**
     * Save absence records to the database using DAO
     */
    public void saveAbsenceRecords() {
        if (absenceRecords.isEmpty()) {
            LOGGER.log(Level.INFO, "No absence records to save.");
            view.showError("Không có bản ghi nào để lưu."); // Assuming view has showInfo method
            return;
        }
        try {
            boolean success = absenceRecordDAO.updateBatch(absenceRecords);
            if (success) {
                LOGGER.log(Level.INFO, "Successfully saved {0} absence records.", absenceRecords.size());
                view.showSuccess("Đã lưu " + absenceRecords.size() + " bản ghi thành công!"); // Assuming view has showSuccess method
            } else {
                LOGGER.log(Level.WARNING, "Batch update completed, but not all records were reported as updated successfully.");
                view.showError("Lưu dữ liệu hoàn thành, nhưng có thể một số bản ghi không được cập nhật."); // Assuming view has showWarning
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving absence data to database.", e);
            view.showError("Lỗi lưu dữ liệu vắng mặt.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error saving absence data.", e);
            view.showError("Đã xảy ra lỗi không mong muốn khi lưu dữ liệu.");
        }
    }

    /**
     * Filter absence records by date range and keywords using DAO
     *
     * @param keyword Search keyword
     * @param fromDateString Start date as String (dd/MM/yyyy)
     * @param toDateString End date as String (dd/MM/yyyy)
     * @return Filtered list of absence records
     */
    public ObservableList<AbsenceRecord> filterRecords(String keyword, String fromDateString, String toDateString) {
        LocalDate fromDate = null;
        LocalDate toDate = null;

        try {
            if (fromDateString != null && !fromDateString.trim().isEmpty()) {
                fromDate = LocalDate.parse(fromDateString, DATE_FORMATTER);
            }
            if (toDateString != null && !toDateString.trim().isEmpty()) {
                toDate = LocalDate.parse(toDateString, DATE_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            LOGGER.log(Level.WARNING, "Invalid date format provided for filtering. Expected dd/MM/yyyy.", e);
            view.showError("Định dạng ngày không hợp lệ. Vui lòng sử dụng định dạng dd/MM/yyyy.");
            // Return the current list or an empty list if dates are invalid
            return absenceRecords; // Or FXCollections.observableArrayList()
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error parsing dates for filtering.", e);
            view.showError("Đã xảy ra lỗi khi xử lý ngày tháng.");
            return absenceRecords; // Or FXCollections.observableArrayList()
        }


        absenceRecords.clear(); // Clear existing data for filtered results
        try {
            List<AbsenceRecord> filteredList = absenceRecordDAO.findByFilters(keyword, fromDate, toDate);
            absenceRecords.addAll(filteredList);
            LOGGER.log(Level.INFO, "Filtered absence records. Found {0} records.", filteredList.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error filtering absence data from database.", e);
            view.showError("Lỗi lọc dữ liệu vắng mặt.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error filtering absence data.", e);
            view.showError("Đã xảy ra lỗi không mong muốn khi lọc dữ liệu.");
        }


        return absenceRecords; // Return the updated observable list
    }

    // Note: The original loadAbsenceData method for dummy data is removed
    // as the controller now interacts with the DAO.
    // If dummy data is still needed for testing/development without a DB,
    // you can create a separate method or conditional logic.

    // Example of creating dummy data with String ID (not used in final version interacting with DAO)
    /*
    private void generateDummyDataWithStringUtils() {
        for (int i = 1; i <= 15; i++) {
            ImageView studentImage = new ImageView();
            studentImage.setFitHeight(30);
            studentImage.setFitWidth(30);

            // Generate a unique String ID for dummy data
            String recordId = "dummy-rec-" + i; // Or use UUID.randomUUID().toString();

            AbsenceRecord record = new AbsenceRecord(
                    recordId, // Pass String ID
                    studentImage,
                    "Học sinh " + i,
                    "Lớp " + (i % 5 + 1),
                    "29/04/2025", // Assuming this date format is parsed correctly by AbsenceRecord model or DAO
                    i % 2 == 0 ? "Vắng" : "Trễ",
                    "Lý do " + i,
                    i % 3 == 0,
                    i % 4 == 0
            );

            absenceRecords.add(record);
        }
        LOGGER.log(Level.INFO, "Generated {0} dummy absence records.", absenceRecords.size());
    }
    */
}
