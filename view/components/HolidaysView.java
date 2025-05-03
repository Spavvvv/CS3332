package view.components;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class HolidaysView extends BaseScreenView {

    private IntegerProperty currentYear = new SimpleIntegerProperty();
    private ComboBox<Integer> yearComboBox;
    private GridPane calendarGrid;
    private VBox historyBox;

    // Maps to store holiday information
    private Map<LocalDate, Holiday> holidays = new HashMap<>();

    public HolidaysView() {
        super("Ngày nghỉ", "holidays");
        currentYear.set(2025); // Default to 2025 as shown in the image
    }

    @Override
    public void initializeView() {
        // Initialize holidays data
        initializeHolidays();

        // Main layout
        root.setSpacing(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        // Title
        Label titleLabel = new Label("Ngày nghỉ");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 30));
        titleLabel.setTextFill(Color.rgb(0, 149, 246));
        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        // Year selector
        HBox yearSelectorBox = createYearSelector();
        HBox yearContainer = new HBox(yearSelectorBox);
        yearContainer.setAlignment(Pos.CENTER);
        yearContainer.setPadding(new Insets(0, 0, 20, 0));

        // Main content area
        HBox contentContainer = new HBox(30);

        // Calendar grid (left side)
        calendarGrid = createCalendarGrid();
        VBox calendarContainer = new VBox(calendarGrid);
        calendarContainer.setPrefWidth(800);

        // Right panel with legends and history
        VBox rightPanel = createRightPanel();
        rightPanel.setPrefWidth(350);

        contentContainer.getChildren().addAll(calendarContainer, rightPanel);

        // Add all components to root
        root.getChildren().addAll(titleBox, yearContainer, contentContainer);

        // Update calendar when year changes
        currentYear.addListener((obs, oldVal, newVal) -> updateCalendar());

        // Initial calendar update
        updateCalendar();
    }

    private HBox createYearSelector() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        // Previous year button
        Button prevYearBtn = new Button("◄");
        prevYearBtn.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #0095f6; -fx-background-radius: 5;");
        prevYearBtn.setPrefWidth(40);
        prevYearBtn.setPrefHeight(40);
        prevYearBtn.setOnAction(e -> currentYear.set(currentYear.get() - 1));

        // Year combobox
        yearComboBox = new ComboBox<>();
        yearComboBox.setStyle("-fx-background-color: #f0f2f5; -fx-font-size: 16px; -fx-font-weight: bold;");
        yearComboBox.setPrefWidth(120);
        yearComboBox.setPrefHeight(40);

        // Add years from 2020 to 2030
        for (int year = 2020; year <= 2030; year++) {
            yearComboBox.getItems().add(year);
        }
        yearComboBox.setValue(currentYear.get());
        yearComboBox.setOnAction(e -> currentYear.set(yearComboBox.getValue()));

        // Next year button
        Button nextYearBtn = new Button("►");
        nextYearBtn.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #0095f6; -fx-background-radius: 5;");
        nextYearBtn.setPrefWidth(40);
        nextYearBtn.setPrefHeight(40);
        nextYearBtn.setOnAction(e -> currentYear.set(currentYear.get() + 1));

        box.getChildren().addAll(prevYearBtn, yearComboBox, nextYearBtn);
        return box;
    }

    private GridPane createCalendarGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Create 4x3 grid for months
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int month = row * 3 + col + 1;
                GridPane monthPane = createMonthPane(month);
                monthPane.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: white;");
                grid.add(monthPane, col, row);
            }
        }

        return grid;
    }

    private GridPane createMonthPane(int monthNumber) {
        GridPane monthPane = new GridPane();
        monthPane.setPadding(new Insets(10));
        monthPane.setVgap(5);
        monthPane.setHgap(5);

        // Month title
        Label monthTitle = new Label("Tháng " + monthNumber);
        monthTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        monthTitle.setAlignment(Pos.CENTER);
        monthTitle.setMaxWidth(Double.MAX_VALUE);
        monthTitle.setTextFill(Color.BLACK);
        GridPane.setColumnSpan(monthTitle, 7);
        monthPane.add(monthTitle, 0, 0);

        // Day headers (CN, T2, T3, etc.)
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setTextAlignment(TextAlignment.CENTER);
            dayLabel.setPrefWidth(25);
            dayLabel.setTextFill(Color.BLACK);
            monthPane.add(dayLabel, i, 1);
        }

        return monthPane;
    }

    private void updateCalendar() {
        // Update year selector
        yearComboBox.setValue(currentYear.get());

        // Clear and rebuild calendar grid
        calendarGrid.getChildren().clear();

        // Recreate the month panes
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int month = row * 3 + col + 1;
                GridPane monthPane = createMonthPane(month);
                monthPane.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: white;");

                // Fill the month with days
                fillMonthWithDays(monthPane, month, currentYear.get());

                calendarGrid.add(monthPane, col, row);
            }
        }
    }

    private void fillMonthWithDays(GridPane monthPane, int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstOfMonth = yearMonth.atDay(1);

        // Determine which day of the week the month starts on (0 = Sunday, 1 = Monday, etc.)
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 (Monday) through 7 (Sunday)
        if (firstDayOfWeek == 7) firstDayOfWeek = 0; // Convert Sunday from 7 to 0

        int daysInMonth = yearMonth.lengthOfMonth();
        int row = 2; // Start at row 2 (after month title and day headers)
        int col = firstDayOfWeek;

        // Add days to the month pane
        for (int day = 1; day <= daysInMonth; day++) {
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setPrefWidth(25);
            dayLabel.setPrefHeight(25);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setTextAlignment(TextAlignment.CENTER);
            dayLabel.setTextFill(Color.BLACK);

            // Check if this date is a holiday
            LocalDate date = LocalDate.of(year, month, day);
            if (holidays.containsKey(date)) {
                Holiday holiday = holidays.get(date);
                StackPane dayCell = new StackPane();
                dayCell.setStyle("-fx-background-color: " + holiday.colorHex + "; -fx-background-radius: 3;");
                dayCell.getChildren().add(dayLabel);
                monthPane.add(dayCell, col, row);
            } else {
                monthPane.add(dayLabel, col, row);
            }

            // Move to the next column
            col++;

            // If we reach the end of the week, move to the next row
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(25);

        // Legends section
        VBox legendsBox = createLegendsBox();

        // History section
        Label historyTitle = new Label("Lịch sử");
        historyTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        historyTitle.setTextFill(Color.BLACK); // Set text color to black

        historyBox = new VBox(10);
        historyBox.setPrefWidth(350);

        // Add history items
        addHistoryItem("iclass.quanly@gmail.com: Thêm mới",
                "Nghỉ tết dương lịch 2025[01/01/2025 -> 01/01/2025]",
                "17:27:12/20/12/2024");

        addHistoryItem("iclass.quanly@gmail.com: Thêm mới",
                "NGHỈ TẾT ÂM LỊCH [26/01/2025 -> 02/02/2025]",
                "17:28:26/20/12/2024");

        addHistoryItem("iclass.quanly@gmail.com: Thêm mới",
                "Giỗ tổ Hùng Vương [10/04/2025 -> 10/04/2025]",
                "17:29:27/20/12/2024");

        panel.getChildren().addAll(legendsBox, historyTitle, historyBox);
        return panel;
    }

    private VBox createLegendsBox() {
        VBox legendsBox = new VBox(15);

        // New Year holiday legend
        HBox newYearLegend = createLegendItem(
                Color.rgb(220, 35, 65),
                "NGHỈ TẾT DƯƠNG LỊCH 2025",
                "01/01/2025 - 01/01/2025"
        );

        // Lunar New Year holiday legend
        HBox lunarNewYearLegend = createLegendItem(
                Color.rgb(62, 187, 79),
                "NGHỈ TẾT ÂM LỊCH",
                "26/01/2025 - 02/02/2025"
        );

        // Hung Kings Commemoration legend
        HBox hungKingsLegend = createLegendItem(
                Color.rgb(255, 204, 0),
                "GIỖ TỔ HÙNG VƯƠNG",
                "10/04/2025 - 10/04/2025"
        );

        legendsBox.getChildren().addAll(newYearLegend, lunarNewYearLegend, hungKingsLegend);
        return legendsBox;
    }

    // And make sure the legend text is also black:
    private HBox createLegendItem(Color color, String title, String dateRange) {
        HBox item = new HBox(15);

        // Color circle
        Circle colorCircle = new Circle(20);
        colorCircle.setFill(color);

        // Text info
        VBox textBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.BLACK); // Set text color to black

        Label dateLabel = new Label(dateRange);
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setTextFill(Color.BLACK); // Set text color to black

        textBox.getChildren().addAll(titleLabel, dateLabel);
        item.getChildren().addAll(colorCircle, textBox);

        return item;
    }

    // You'll also need to update the addHistoryItem method:
    private void addHistoryItem(String user, String action, String timestamp) {
        HBox item = new HBox(15);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-border-color: #f0f2f5; -fx-border-width: 0 0 1 0; -fx-background-color: #ffffff;");

        // User avatar
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(Color.rgb(51, 0, 111));

        Label initials = new Label("IQ");
        initials.setTextFill(Color.WHITE);
        initials.setFont(Font.font("System", FontWeight.BOLD, 12));

        avatar.getChildren().addAll(avatarCircle, initials);

        // Text content
        VBox content = new VBox(5);
        Label userLabel = new Label(user);
        userLabel.setFont(Font.font("System", 12));
        userLabel.setTextFill(Color.BLACK); // Set text color to black

        Label actionLabel = new Label(action);
        actionLabel.setFont(Font.font("System", 14));
        actionLabel.setTextFill(Color.BLACK); // Set text color to black

        Label timeLabel = new Label(timestamp);
        timeLabel.setFont(Font.font("System", 12));
        timeLabel.setTextFill(Color.GRAY); // Keep timestamp gray as in the image

        content.getChildren().addAll(userLabel, actionLabel, timeLabel);

        item.getChildren().addAll(avatar, content);
        historyBox.getChildren().add(item);
    }

    private void initializeHolidays() {
        // New Year's Day
        addHoliday("Tết Dương Lịch",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 1),
                "#dc2341");

        // Lunar New Year (Tet)
        addHoliday("Tết Âm Lịch",
                LocalDate.of(2025, 1, 26),
                LocalDate.of(2025, 2, 2),
                "#3ebb4f");

        // Hung Kings Commemoration Day
        addHoliday("Giỗ Tổ Hùng Vương",
                LocalDate.of(2025, 4, 10),
                LocalDate.of(2025, 4, 10),
                "#ffcc00");

        // Add any other days shown as highlighted in the image
    }

    private void addHoliday(String name, LocalDate startDate, LocalDate endDate, String colorHex) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            holidays.put(current, new Holiday(name, startDate, endDate, colorHex));
            current = current.plusDays(1);
        }
    }

    // Holiday class
    private class Holiday {
        String name;
        LocalDate startDate;
        LocalDate endDate;
        String colorHex;

        public Holiday(String name, LocalDate startDate, LocalDate endDate, String colorHex) {
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
            this.colorHex = colorHex;
        }
    }

    @Override
    public void refreshView() {
        updateCalendar();
    }
}
