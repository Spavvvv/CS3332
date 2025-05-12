package src.model.teaching;

import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

public class MonthlyTeachingStatisticsFilter {
    private String periodType;
    private YearMonth fromYearMonth;
    private YearMonth toYearMonth;
    private String status;

    // Reuse the same status options for consistency
    public static final List<String> STATUS_OPTIONS =
            TeachingStatisticsFilter.STATUS_OPTIONS;

    public MonthlyTeachingStatisticsFilter() {
        this.periodType = "month";
        // Default to current month
        YearMonth current = YearMonth.now();
        this.fromYearMonth = current;
        this.toYearMonth = current;
        this.status = "Tất cả";
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public YearMonth getFromYearMonth() {
        return fromYearMonth;
    }

    public void setFromYearMonth(YearMonth fromYearMonth) {
        this.fromYearMonth = fromYearMonth;
    }

    public void setFromYearMonth(int year, Month month) {
        this.fromYearMonth = YearMonth.of(year, month);
    }

    public YearMonth getToYearMonth() {
        return toYearMonth;
    }

    public void setToYearMonth(YearMonth toYearMonth) {
        this.toYearMonth = toYearMonth;
    }

    public void setToYearMonth(int year, Month month) {
        this.toYearMonth = YearMonth.of(year, month);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
