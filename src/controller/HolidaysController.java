package src.controller;

import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import src.model.holidays.HolidaysModel;
import utils.DaoManager;
import src.dao.HolidayDAO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class HolidaysController {
    private HolidaysModel model;

    /**
     * Constructor không có tham số, khởi tạo đơn giản
     */
    public HolidaysController() {
        // Lấy HolidayDAO từ DaoManager
        DaoManager daoManager = DaoManager.getInstance();
        HolidayDAO holidayDAO = daoManager.getHolidayDAO();

        // Khởi tạo model
        this.model = new HolidaysModel();
    }

    public int getCurrentYear() {
        return model.getCurrentYear();
    }

    public void changeYear(int year) {
        model.setCurrentYear(year);
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

    /**
     * Thêm holiday mới, không cần thông tin người dùng
     */
    public void addHoliday(Holiday holiday) {
        model.addHoliday(holiday);

        // Log thao tác với mô tả không có currentUser
        String actionDesc = holiday.getName() + " [" +
                formatDate(holiday.getStartDate()) + " -> " +
                formatDate(holiday.getEndDate()) + "]";

        model.logAction("Thêm mới", actionDesc);
    }

    /**
     * Xóa holiday, không cần thông tin người dùng
     */
    public void deleteHoliday(Long id) {
        // Lấy thông tin holiday trước khi xóa để log
        Holiday holiday = null;
        for (Holiday h : getAllHolidays()) {
            if (h.getId().equals(id)) {
                holiday = h;
                break;
            }
        }

        if (holiday != null) {
            model.deleteHoliday(id);

            // Log thao tác với mô tả không có currentUser
            String actionDesc = holiday.getName() + " [" +
                    formatDate(holiday.getStartDate()) + " -> " +
                    formatDate(holiday.getEndDate()) + "]";

            model.logAction("Xóa", actionDesc);
        }
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d",
                date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}