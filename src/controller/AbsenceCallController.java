// Controller Class: AbsenceCallController.java
package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import src.model.absence.AbsenceRecord;
import view.components.AbsenceCallView;

/**
 * Controller for the Absence Call functionality
 */
public class AbsenceCallController {
    private final AbsenceCallView view;
    private ObservableList<AbsenceRecord> absenceRecords = FXCollections.observableArrayList();

    public AbsenceCallController(AbsenceCallView view) {
        this.view = view;
        initializeController();
    }

    private void initializeController() {
        // Initialize controller logic
        loadAbsenceData();

        // Set data to view
        view.setAbsenceData(absenceRecords);
    }

    /**
     * Load absence data from database or service
     */
    private void loadAbsenceData() {
        // In a real application, data would be loaded from a database or service
        // For now, we'll create some sample data
        for (int i = 1; i <= 15; i++) {
            ImageView studentImage = new ImageView();
            studentImage.setFitHeight(30);
            studentImage.setFitWidth(30);

            AbsenceRecord record = new AbsenceRecord(
                    i,
                    studentImage,
                    "Học sinh " + i,
                    "Lớp " + (i % 5 + 1),
                    "29/04/2025",
                    i % 2 == 0 ? "Vắng" : "Trễ",
                    "Lý do " + i,
                    i % 3 == 0,
                    i % 4 == 0
            );

            absenceRecords.add(record);
        }
    }

    /**
     * Save absence records to the database or service
     */
    public void saveAbsenceRecords() {
        // In a real application, this would save data to a database or service
        System.out.println("Saving " + absenceRecords.size() + " records");

        // Notify view of successful save
        // view.showSuccess("Đã lưu " + absenceRecords.size() + " bản ghi thành công!");
    }

    /**
     * Filter absence records by date range and keywords
     *
     * @param keyword Search keyword
     * @param fromDate Start date
     * @param toDate End date
     * @return Filtered list of absence records
     */
    public ObservableList<AbsenceRecord> filterRecords(String keyword, String fromDate, String toDate) {
        // In a real application, this would filter data based on criteria
        // For now, we'll just return the original list
        return absenceRecords;
    }
}
