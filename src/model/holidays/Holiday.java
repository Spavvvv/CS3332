package src.model.holidays;

import java.time.LocalDate;

public class Holiday {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String colorHex;

    // Default constructor for JPA/database operations
    public Holiday() {
    }

    public Holiday(Long id, String name, LocalDate startDate, LocalDate endDate, String colorHex) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.colorHex = colorHex;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holiday holiday = (Holiday) o;
        return id != null && id.equals(holiday.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}