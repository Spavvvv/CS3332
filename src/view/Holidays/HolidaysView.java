package src.view.Holidays;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import src.controller.Holidays.HolidaysController;
import src.dao.Holidays.HolidayDAO;
import src.model.holidays.Holiday;
import src.model.holidays.HolidayHistory;
import src.view.components.Screen.BaseScreenView;

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
    private VBox legendsBoxGlobal;

    public HolidaysView() {
        super("Ngày nghỉ", "holidays");
        controller = new HolidaysController();
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
        // titleBox.setAlignment(Pos.CENTER); // Cân giữa tiêu đề nếu muốn

        // Year selector và Nút Refresh
        HBox yearSelectorAndRefreshBox = new HBox(20); // HBox chứa bộ chọn năm và nút refresh
        yearSelectorAndRefreshBox.setAlignment(Pos.CENTER);

        HBox yearSelectorBox = createYearSelector(); // Bộ chọn năm (như cũ)

        Button refreshButton = new Button("Làm mới \u21BB"); // Ký tự refresh: ↻ hoặc ↺ hoặc \u21BB
        styleNavigationButton(refreshButton); // Sử dụng style tương tự các nút điều hướng năm
        refreshButton.setOnAction(e -> {
            refreshView();
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Dữ liệu ngày nghỉ đã được làm mới.");
        });

        // Sử dụng Pane để đẩy nút Refresh về bên phải
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bố cục cho phần header (Tiêu đề, Bộ chọn năm, Nút Refresh)
        // VBox headerControls = new VBox(10);
        // HBox yearAndRefreshControls = new HBox(10, yearSelectorBox, spacer, refreshButton);
        // yearAndRefreshControls.setAlignment(Pos.CENTER_LEFT); // Hoặc CENTER_RIGHT tùy ý
        // headerControls.getChildren().addAll(titleBox, yearAndRefreshControls);
        // headerControls.setAlignment(Pos.TOP_CENTER); // Căn giữa toàn bộ header

        // Thay vì VBox, chúng ta có thể dùng BorderPane hoặc AnchorPane cho root
        // Hoặc đơn giản là thêm vào HBox yearSelectorAndRefreshBox
        yearSelectorAndRefreshBox.getChildren().addAll(yearSelectorBox, spacer, refreshButton);
        yearSelectorAndRefreshBox.setPadding(new Insets(0, 0, 20, 0));


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
        // root.getChildren().addAll(headerControls, contentContainer);
        root.getChildren().addAll(titleBox, yearSelectorAndRefreshBox, contentContainer);
    }

    private HBox createYearSelector() {
        HBox box = new HBox(15); // Tăng khoảng cách giữa các nút
        box.setAlignment(Pos.CENTER);
        // Previous year button - Cải thiện giao diện
        Button prevYearBtn = new Button("◄");
        styleNavigationButton(prevYearBtn);
        prevYearBtn.setOnAction(e -> {
            if (controller != null) {
                int prevYear = controller.getCurrentYear() - 1;
                if (prevYear >= 2020) { // Đảm bảo năm không nhỏ hơn giới hạn
                    controller.changeYear(prevYear);
                    refreshView();
                }
            }
        });
        // Year combobox - Cải thiện giao diện
        yearComboBox = new ComboBox<>();
        yearComboBox.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5;");
        yearComboBox.setPrefWidth(120);
        yearComboBox.setPrefHeight(40);
        // Add years from 2020 to 2030
        for (int year = 2020; year <= 2030; year++) {
            yearComboBox.getItems().add(year);
        }

        // Set current year
        yearComboBox.setValue(LocalDate.now().getYear());
        yearComboBox.setOnAction(e -> {
            if (controller != null && yearComboBox.getValue() != null) {
                controller.changeYear(yearComboBox.getValue());
                refreshView();
            }
        });

        // Next year button - Cải thiện giao diện
        Button nextYearBtn = new Button("►");
        styleNavigationButton(nextYearBtn);
        nextYearBtn.setOnAction(e -> {
            if (controller != null) {
                int nextYear = controller.getCurrentYear() + 1;
                if (nextYear <= 2030) { // Đảm bảo năm không vượt quá giới hạn
                    controller.changeYear(nextYear);
                    refreshView();
                }
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

            // Tạo LocalDate để kiểm tra
            LocalDate date = LocalDate.of(controller.getCurrentYear(), month, day);

            // Lấy danh sách tất cả kỳ nghỉ để kiểm tra
            List<Holiday> allHolidays = controller.getAllHolidays();
            Holiday matchingHoliday = null;

            // Kiểm tra từng kỳ nghỉ xem ngày hiện tại có thuộc kỳ nghỉ nào không
            for (Holiday holiday : allHolidays) {
                LocalDate startDate = holiday.getStartDate();
                LocalDate endDate = holiday.getEndDate();

                // Kiểm tra nếu ngày hiện tại nằm trong khoảng từ startDate đến endDate
                if ((date.isEqual(startDate) || date.isAfter(startDate))
                        && (date.isEqual(endDate) || date.isBefore(endDate))) {
                    matchingHoliday = holiday;
                    break;
                }
            }

            // Nếu tìm thấy kỳ nghỉ, tô màu cho ngày này
            if (matchingHoliday != null) {
                // Thêm vào danh sách kỳ nghỉ độc nhất
                String key = matchingHoliday.getName() + matchingHoliday.getStartDate() + matchingHoliday.getEndDate();
                uniqueHolidays.put(key, matchingHoliday);

                // Tạo StackPane để hiển thị ngày với nền màu
                StackPane dayCell = new StackPane();
                dayCell.setStyle("-fx-background-color: " + matchingHoliday.getColorHex() + "; -fx-background-radius: 3;");
                dayCell.getChildren().add(dayLabel);
                monthPane.add(dayCell, col, row);
            } else {
                // Nếu không phải ngày nghỉ, hiển thị bình thường
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

    // Trong HolidaysView.java
    private VBox createRightPanel() {
        VBox panel = new VBox(25);

        // Legends section
        this.legendsBoxGlobal = createLegendsBox(); // << GÁN CHO BIẾN THÀNH VIÊN
        // VBox legendsBox = createLegendsBox(); // Bỏ dòng này nếu đã gán ở trên

        // History section
        Label historyTitle = new Label("Lịch sử");
        historyTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        historyTitle.setTextFill(Color.BLACK);

        historyBox = new VBox(10); // historyBox đã là biến thành viên
        historyBox.setPrefWidth(350);

        panel.getChildren().addAll(this.legendsBoxGlobal, historyTitle, historyBox); // Sử dụng biến thành viên
        return panel;
    }
    private VBox createLegendsBox() {
        VBox legendsBox = new VBox(15);
        // Legends will be populated in updateLegends method
        return legendsBox;
    }

    private void updateLegends(VBox legendsBox) {
        legendsBox.getChildren().clear();
        legendsBox.setSpacing(10); // Thêm khoảng cách giữa các legend items

        // Add legends for each unique holiday
        if (uniqueHolidays.isEmpty()) {
            Label noHolidaysLabel = new Label("Không có ngày nghỉ nào được hiển thị cho năm này.");
            noHolidaysLabel.setFont(Font.font("System", 13));
            noHolidaysLabel.setTextFill(Color.GRAY);
            legendsBox.getChildren().add(noHolidaysLabel);
        } else {
            for (Holiday holiday : uniqueHolidays.values()) {
                String dateRangeText = formatDateRange(holiday.getStartDate(), holiday.getEndDate());
                HBox legendItem = createLegendItem(
                        Color.web(holiday.getColorHex()),
                        holiday.getName().toUpperCase(),
                        dateRangeText,
                        holiday // << TRUYỀN ĐỐI TƯỢNG HOLIDAY
                );
                legendsBox.getChildren().add(legendItem);
            }
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

    private HBox createLegendItem(Color color, String title, String dateRange, Holiday holidayToDelete) { // Thêm Holiday vào tham số
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT); // Căn chỉnh các item bên trong HBox

        // Color circle
        Circle colorCircle = new Circle(15); // Giảm kích thước vòng tròn một chút
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

        // Nút Xóa
        Button deleteButton = new Button("✕"); // Hoặc dùng icon
        deleteButton.setStyle(
                "-fx-background-color: #ff6b6b; " + // Màu đỏ cho nút xóa
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 10px; " +
                        "-fx-padding: 3 7 3 7; " + // Điều chỉnh padding
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;"
        );
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(
                "-fx-background-color: #e05252; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 10px; " +
                        "-fx-padding: 3 7 3 7; " +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;"
        ));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(
                "-fx-background-color: #ff6b6b; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 10px; " +
                        "-fx-padding: 3 7 3 7; " +
                        "-fx-background-radius: 4;" +
                        "-fx-cursor: hand;"
        ));

        // Xử lý sự kiện khi nhấn nút xóa
        deleteButton.setOnAction(e -> handleDeleteHolidayAction(holidayToDelete));

        // Sử dụng Pane để đẩy nút xóa về phía cuối
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Cho spacer co giãn để đẩy nút xóa

        item.getChildren().addAll(colorCircle, textBox, spacer, deleteButton);
        item.setPadding(new Insets(5, 0, 5, 0)); // Thêm padding cho mỗi item
        return item;
    }
    private void handleDeleteHolidayAction(Holiday holidayToDelete) {
        if (holidayToDelete == null || holidayToDelete.getId() == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xác định ngày nghỉ để xóa.");
            return;
        }

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận Xóa Ngày nghỉ");
        confirmationDialog.setHeaderText("Bạn có chắc chắn muốn xóa ngày nghỉ '" + holidayToDelete.getName() + "'?");
        confirmationDialog.setContentText("Hành động này sẽ xóa ngày nghỉ khỏi hệ thống và không thể hoàn tác.\n" +
                "Lưu ý: Các lịch học đã được điều chỉnh trước đó sẽ KHÔNG tự động thay đổi lại khi xóa ngày nghỉ này.");

        ButtonType buttonTypeYes = new ButtonType("Xóa");
        ButtonType buttonTypeNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmationDialog.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        // Sử dụng Stage của view làm owner cho dialog
        Stage stage = (Stage) getRoot().getScene().getWindow();
        confirmationDialog.initOwner(stage);


        confirmationDialog.showAndWait().ifPresent(response -> {
            if (response == buttonTypeYes) {
                if (controller != null) {
                    controller.deleteHoliday(holidayToDelete.getId()); // Gọi controller để xóa
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa ngày nghỉ: " + holidayToDelete.getName());
                    refreshView(); // Cập nhật lại view để hiển thị thay đổi
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Controller chưa được khởi tạo.");
                }
            }
        });
    }

    // Thêm phương thức showAlert nếu chưa có trong HolidaysView
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Đảm bảo dialog hiển thị đúng trên màn hình hiện tại
        Stage stage = (Stage) getRoot().getScene().getWindow();
        alert.initOwner(stage);

        alert.showAndWait();
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

    // Cập nhật lại phương thức refreshView để đảm bảo cập nhật đúng
    // Trong HolidaysView.java
    @Override
    public void refreshView() {
        if (controller != null) {
            updateCalendar(); // Hàm này sẽ gọi controller.getAllHolidays()

            // Cập nhật danh sách chú giải (legends)
            if (this.legendsBoxGlobal != null) {
                updateLegends(this.legendsBoxGlobal); // Sử dụng biến thành viên đã lưu
            } else {
                // Điều này không nên xảy ra nếu createRightPanel được gọi đúng trong initializeView
                System.err.println("HolidaysView: legendsBoxGlobal is null, không thể cập nhật legends.");
            }

            updateHistoryItems(); // Hàm này sẽ gọi controller.getRecentHistory()
        }
    }

    // Phương thức hỗ trợ để tạo style cho nút
    private void styleNavigationButton(Button button) {
        // Style mặc định
        button.setStyle(
                "-fx-background-color: #0095f6; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 50%; " + // Bo tròn nút
                        "-fx-min-width: 40px; " +
                        "-fx-min-height: 40px; " +
                        "-fx-max-width: 40px; " +
                        "-fx-max-height: 40px; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;" // Thêm con trỏ là bàn tay khi hover
        );
        // Thêm hiệu ứng hover
        button.setOnMouseEntered(e ->
                button.setStyle(
                        "-fx-background-color: #0086e0; " + // Màu đậm hơn khi hover
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-max-width: 40px; " +
                                "-fx-max-height: 40px; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);" // Thêm đổ bóng
                )
        );
        // Hiệu ứng khi rời chuột khỏi nút
        button.setOnMouseExited(e ->
                button.setStyle(
                        "-fx-background-color: #0095f6; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-max-width: 40px; " +
                                "-fx-max-height: 40px; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand;"
                )
        );
        // Hiệu ứng khi nhấn nút
        button.setOnMousePressed(e ->
                button.setStyle(
                        "-fx-background-color: #007ac1; " + // Màu đậm hơn nữa khi nhấn
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-max-width: 40px; " +
                                "-fx-max-height: 40px; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand; " +
                                "-fx-translate-y: 1px;" // Dịch xuống một chút khi nhấn
                )
        );
        // Trở về style ban đầu khi nhả nút
        button.setOnMouseReleased(e -> {
            if (button.isHover()) {
                // Nếu vẫn đang hover, trở về style hover
                button.setStyle(
                        "-fx-background-color: #0086e0; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-max-width: 40px; " +
                                "-fx-max-height: 40px; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"
                );
            } else {
                // Nếu không hover, trở về style mặc định
                button.setStyle(
                        "-fx-background-color: #0095f6; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 50%; " +
                                "-fx-min-width: 40px; " +
                                "-fx-min-height: 40px; " +
                                "-fx-max-width: 40px; " +
                                "-fx-max-height: 40px; " +
                                "-fx-font-size: 16px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-cursor: hand;"
                );
            }
        });
    }
    @Override
    public void onActivate() {
        super.onActivate(); // Gọi phương thức của lớp cha nếu có
        refreshView();      // Luôn làm mới view khi nó được kích hoạt
        System.out.println(getViewId() + " activated and data refreshed via onActivate.");
    }

}
