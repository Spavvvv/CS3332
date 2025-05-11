package src.controller;

import src.dao.TeachingStatisticsDAO;
import javafx.collections.ObservableList;
import src.model.teaching.TeacherStatisticsModel;
import src.model.teaching.TeachingStatisticsFilter;
import utils.DaoManager; // Import DaoManager

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TeachingStatisticsController {
    private final TeachingStatisticsFilter filter;
    private final TeachingStatisticsDAO teachingStatisticsDAO;


    public TeachingStatisticsController() {
        this.filter = new TeachingStatisticsFilter();
        // Get TeachingStatisticsDAO instance from DaoManager
        this.teachingStatisticsDAO = DaoManager.getInstance().getTeachingStatisticsDAO();
    }

    public ObservableList<TeacherStatisticsModel> getTeacherStatistics() {
        return teachingStatisticsDAO.getTeacherStatistics(
                filter.getFromDate(),
                filter.getToDate(),
                filter.getStatus()
        );
    }

    public TeachingStatisticsFilter getFilter() {
        return filter;
    }

    public void updatePeriodType(String periodType) {
        filter.setPeriodType(periodType);
    }

    public void updateDateRange(LocalDate fromDate, LocalDate toDate) {
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
    }

    public void updateStatus(String status) {
        filter.setStatus(status);
    }

    public List<String> getStatusOptions() {
        return TeachingStatisticsFilter.STATUS_OPTIONS;
    }

    public boolean exportToExcel() {
        return teachingStatisticsDAO.exportToExcel(
                filter.getFromDate(),
                filter.getToDate(),
                filter.getStatus()
        );
    }

    public boolean exportToPdf() {
        return teachingStatisticsDAO.exportToPdf(
                filter.getFromDate(),
                filter.getToDate(),
                filter.getStatus()
        );
    }

    public boolean print() {
        return teachingStatisticsDAO.logPrintRequest(
                filter.getFromDate(),
                filter.getToDate(),
                filter.getStatus()
        );
    }
}
