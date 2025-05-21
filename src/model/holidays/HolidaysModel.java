package src.model.holidays;

import src.dao.HolidayDAO;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HolidaysModel {
    private IntegerProperty currentYear = new SimpleIntegerProperty();
    private final HolidayDAO holidayDAO;
    private Map<LocalDate, Holiday> holidayCache;

    public HolidaysModel() {
        this.holidayDAO = new HolidayDAO();
        this.holidayCache = new HashMap<>();
        currentYear.set(LocalDate.now().getYear()); // Default to current year

        // Load initial data
        refreshHolidayCache();

        // Update cache when year changes
        currentYear.addListener((obs, oldVal, newVal) -> refreshHolidayCache());
    }

    public void refreshHolidayCache() {
        holidayCache.clear();
        List<Holiday> holidays = holidayDAO.findHolidaysByYear(getCurrentYear());

        for (Holiday holiday : holidays) {
            cacheHoliday(holiday);
        }
    }

    private void cacheHoliday(Holiday holiday) {
        LocalDate current = holiday.getStartDate();
        while (!current.isAfter(holiday.getEndDate())) {
            holidayCache.put(current, holiday);
            current = current.plusDays(1);
        }
    }

    public IntegerProperty currentYearProperty() {
        return currentYear;
    }

    public int getCurrentYear() {
        return currentYear.get();
    }

    public void setCurrentYear(int year) {
        currentYear.set(year);
        System.out.println("Year changed to " + year);
    }

    public Holiday getHolidayForDate(LocalDate date) {
        return holidayCache.get(date);
    }

    public boolean isHoliday(LocalDate date) {
        return holidayCache.containsKey(date);
    }

    public List<Holiday> getAllHolidays() {
        return holidayDAO.findHolidaysByYear(getCurrentYear());
    }

    public List<HolidayHistory> getRecentHistory(int limit) {
        return holidayDAO.findRecentHistory(limit);
    }

    public void addHoliday(Holiday holiday) {
        holidayDAO.saveHoliday(holiday);
        refreshHolidayCache();
    }

    public void deleteHoliday(Long id) {
        holidayDAO.deleteHoliday(id);
        refreshHolidayCache();
    }

    public void logAction(String user, String action) {
        HolidayHistory history = new HolidayHistory();
        history.setUser(user);
        history.setAction(action);
        history.setTimestamp(LocalDateTime.now());
    }

    // Calendar utility method
    public YearMonth getYearMonth(int month) {
        return YearMonth.of(getCurrentYear(), month);
    }
}
