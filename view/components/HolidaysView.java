package view.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import src.controller.HolidaysController;
import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import view.BaseScreenView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HolidaysView extends BaseScreenView {

    private HolidaysController controller;
    private ComboBox<Integer> yearComboBox;
    private GridPane calendarGrid;
    private VBox historyBox;
    private Map<String, Holiday> uniqueHolidays = new HashMap<>();

    public HolidaysView() {
        super("Ngày nghỉ", "holidays");
        // Controller will be set later via setController method
    }

    public void setController(HolidaysController controller) {
        this.controller = controller;
        refreshView();
    }

    @Override
    public void initializeView() {
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
    }

    private HBox createYearSelector() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        // Previous year button
        Button prevYearBtn = new Button("◄");
        prevYearBtn.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #0095f6; -fx-background-radius: 5;");
        prevYearBtn.setPrefWidth(40);
        prevYearBtn.setPrefHeight(40);
        prevYearBtn.setOnAction(e -> {
            if (controller != null) {
                controller.changeYear(controller.getCurrentYear() - 1);
            }
        });

        // Year combobox
        yearComboBox = new ComboBox<>();
        yearComboBox.setStyle("-fx-background-color: #f0f2f5; -fx-font-size: 16px; -fx-font-weight: bold;");
        yearComboBox.setPrefWidth(120);
        yearComboBox.setPrefHeight(40);

        // Add years from 2020 to 2030
        for (int year = 2020; year <= 2030; year++) {
            yearComboBox.getItems().add(year);
        }

        // Set current year (will be updated when controller is set)
        yearComboBox.setValue(LocalDate.now().getYear());

        yearComboBox.setOnAction(e -> {
            if (controller != null) {
                controller.changeYear(yearComboBox.getValue());
            }
        });

        // Next year button
        Button nextYearBtn = new Button("►");
        nextYearBtn.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #0095f6; -fx-background-radius: 5;");
        nextYearBtn.setPrefWidth(40);
        nextYearBtn.setPrefHeight(40);
        nextYearBtn.setOnAction(e -> {
            if (controller != null) {
                controller.changeYear(controller.getCurrentYear() + 1);
            }
        });

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
        if (controller == null) return;

        // Update year selector
        yearComboBox.setValue(controller.getCurrentYear());

        // Clear and rebuild calendar grid
        calendarGrid.getChildren().clear();
        uniqueHolidays.clear();

        // Recreate the month panes
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                int month = row * 3 + col + 1;
                GridPane monthPane = createMonthPane(month);
                monthPane.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: white;");

                // Fill the month with days
                fillMonthWithDays(monthPane, month);

                calendarGrid.add(monthPane, col, row);
            }
        }
    }

    private void fillMonthWithDays(GridPane monthPane, int month) {
        if (controller == null) return;

        YearMonth yearMonth = controller.getYearMonth(month);
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
            LocalDate date = LocalDate.of(controller.getCurrentYear(), month, day);
            Holiday holiday = controller.getHolidayForDate(date);

            if (holiday != null) {
                // Add to unique holidays for legend
                String key = holiday.getName() + holiday.getStartDate() + holiday.getEndDate();
                uniqueHolidays.put(key, holiday);

                StackPane dayCell = new StackPane();
                dayCell.setStyle("-fx-background-color: " + holiday.getColorHex() + "; -fx-background-radius: 3;");
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
        historyTitle.setTextFill(Color.BLACK);

        historyBox = new VBox(10);
        historyBox.setPrefWidth(350);

        panel.getChildren().addAll(legendsBox, historyTitle, historyBox);
        return panel;
    }

    private VBox createLegendsBox() {
        VBox legendsBox = new VBox(15);
        // Legends will be populated in updateLegends method
        return legendsBox;
    }

    private void updateLegends(VBox legendsBox) {
        legendsBox.getChildren().clear();

        // Add legends for each unique holiday
        for (Holiday holiday : uniqueHolidays.values()) {
            String dateRangeText = formatDateRange(holiday.getStartDate(), holiday.getEndDate());
            HBox legendItem = createLegendItem(
                    Color.web(holiday.getColorHex()),
                    holiday.getName().toUpperCase(),
                    dateRangeText
            );
            legendsBox.getChildren().add(legendItem);
        }
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return formatDate(start);
        } else {
            return formatDate(start) + " - " + formatDate(end);
        }
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private HBox createLegendItem(Color color, String title, String dateRange) {
        HBox item = new HBox(15);

        // Color circle
        Circle colorCircle = new Circle(20);
        colorCircle.setFill(color);

        // Text info
        VBox textBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.BLACK);

        Label dateLabel = new Label(dateRange);
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setTextFill(Color.BLACK);

        textBox.getChildren().addAll(titleLabel, dateLabel);
        item.getChildren().addAll(colorCircle, textBox);

        return item;
    }

    private void updateHistoryItems() {
        if (controller == null) return;

        historyBox.getChildren().clear();

        // Add history items from controller
        List<HolidayHistory> historyItems = controller.getRecentHistory(10); // Get 10 most recent items
        for (HolidayHistory history : historyItems) {
            addHistoryItemToView(history);
        }
    }

    private void addHistoryItemToView(HolidayHistory history) {
        HBox item = new HBox(15);
        item.setPadding(new Insets(15));
        item.setStyle("-fx-border-color: #f0f2f5; -fx-border-width: 0 0 1 0; -fx-background-color: #ffffff;");

        // User avatar
        StackPane avatar = new StackPane();
        Circle avatarCircle = new Circle(20);
        avatarCircle.setFill(Color.rgb(51, 0, 111));

        // Get initials from user name
        String initials = getInitials(history.getUser());
        Label initialsLabel = new Label(initials);
        initialsLabel.setTextFill(Color.WHITE);
        initialsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        avatar.getChildren().addAll(avatarCircle, initialsLabel);

        // Text content
        VBox content = new VBox(5);
        Label userLabel = new Label(history.getUser());
        userLabel.setFont(Font.font("System", 12));
        userLabel.setTextFill(Color.BLACK);

        Label actionLabel = new Label(history.getAction());
        actionLabel.setFont(Font.font("System", 14));
        actionLabel.setTextFill(Color.BLACK);

        // Format timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedTime = history.getTimestamp().format(formatter);

        Label timeLabel = new Label(formattedTime);
        timeLabel.setFont(Font.font("System", 12));
        timeLabel.setTextFill(Color.GRAY);

        content.getChildren().addAll(userLabel, actionLabel, timeLabel);

        item.getChildren().addAll(avatar, content);
        historyBox.getChildren().add(item);
    }

    private String getInitials(String userName) {
        if (userName == null || userName.isEmpty()) {
            return "?";
        }

        String[] parts = userName.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
    }

    @Override
    public void refreshView() {
        if (controller != null) {
            updateCalendar();

            // Find and update legends box
            for (javafx.scene.Node node : root.getChildren()) {
                if (node instanceof HBox) {
                    HBox contentContainer = (HBox) node;
                    for (javafx.scene.Node contentNode : contentContainer.getChildren()) {
                        if (contentNode instanceof VBox) {
                            VBox rightPanel = (VBox) contentNode;
                            if (rightPanel.getChildren().size() > 0 && rightPanel.getChildren().get(0) instanceof VBox) {
                                VBox legendsBox = (VBox) rightPanel.getChildren().get(0);
                                updateLegends(legendsBox);
                            }
                        }
                    }
                }
            }

            updateHistoryItems();
        }
    }
}
