package src.controller;

import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import src.model.holidays.HolidaysModel;
import utils.DatabaseConnection;
import view.components.HolidaysView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class HolidaysController {
    private HolidaysModel model;
    private HolidaysView view;
    private final String currentUser;

    public HolidaysController(HolidaysView view, String currentUser) {
        // Initialize the model
        this.model = new HolidaysModel();
        this.view = view;
        this.currentUser = currentUser;

        // Set the controller in the view
        view.setController(this);
    }

    public int getCurrentYear() {
        return model.getCurrentYear();
    }

    public void changeYear(int year) {
        model.setCurrentYear(year);
        view.refreshView();
    }

    public YearMonth getYearMonth(int month) {
        return model.getYearMonth(month);
    }

    public Holiday getHolidayForDate(LocalDate date) {
        return model.getHolidayForDate(date);
    }

    public List<Holiday> getAllHolidays() {
        return model.getAllHolidays();
    }

    public List<HolidayHistory> getRecentHistory(int limit) {
        return model.getRecentHistory(limit);
    }

    public void addHoliday(Holiday holiday) {
        model.addHoliday(holiday);

        // Log the action
        String actionDesc = holiday.getName() + " [" +
                formatDate(holiday.getStartDate()) + " -> " +
                formatDate(holiday.getEndDate()) + "]";

        model.logAction(currentUser + ": Thêm mới", actionDesc);

        // Refresh the view
        view.refreshView();
    }

    public void deleteHoliday(Long id) {
        // Get the holiday details before deletion for logging
        Holiday holiday = null;
        for (Holiday h : getAllHolidays()) {
            if (h.getId().equals(id)) {
                holiday = h;
                break;
            }
        }

        if (holiday != null) {
            model.deleteHoliday(id);

            // Log the action
            String actionDesc = holiday.getName() + " [" +
                    formatDate(holiday.getStartDate()) + " -> " +
                    formatDate(holiday.getEndDate()) + "]";

            model.logAction(currentUser + ": Xóa", actionDesc);
        }

        // Refresh the view
        view.refreshView();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d",
                date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public void cleanup() {
        // Close any resources if needed
        DatabaseConnection.closeConnection();
    }
}
