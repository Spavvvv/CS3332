package src.model.teaching;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class TeachingStatisticsFilter {
    private String periodType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status;

    public static final List<String> STATUS_OPTIONS = Arrays.asList("Tất cả", "Đã duyệt", "Chưa duyệt", "Từ chối");

    public TeachingStatisticsFilter() {
        this.periodType = "day";
        this.fromDate = LocalDate.now().minusMonths(1);
        this.toDate = LocalDate.now();
        this.status = "Tất cả";
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}