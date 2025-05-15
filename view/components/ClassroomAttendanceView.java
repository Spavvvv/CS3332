
package view.components;

// JavaFX Imports
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

// Application-specific imports
import view.BaseScreenView;
import src.model.attendance.StudentAttendanceData;
// import view.components.StarRatingControl; // In same package

// Domain model imports
import src.model.person.Student;
import src.model.person.Parent;

import java.time.LocalDate;
import java.util.function.Consumer;

public class ClassroomAttendanceView extends BaseScreenView {

    private TableView<StudentAttendanceData> tableView;
    private ObservableList<StudentAttendanceData> attendanceList = FXCollections.observableArrayList();
    private FilteredList<StudentAttendanceData> filteredAttendanceList;

    // Filters
    private ComboBox<String> punctualityFilterCombo;
    private ComboBox<String> diligenceFilterCombo;

    // Style for black text labels/headers
    private final String blackTextStyle = "-fx-text-fill: black;";
    private final String blackBoldTextStyle = "-fx-text-fill: black; -fx-font-weight: bold;";

    private CheckBox homeworkHeaderCheckbox; // For "Select All" functionality


    public ClassroomAttendanceView() {
        super("Điểm danh và đánh giá buổi học", "classroom-attendance-view");
    }

    @Override
    public void initializeView() {
        this.root.setSpacing(15);
        this.root.setPadding(new Insets(20));
        this.root.setStyle("-fx-background-color: #f0f2f5;");

        loadDummyData();
        filteredAttendanceList = new FilteredList<>(attendanceList, p -> true);

        this.root.getChildren().addAll(
                createTopBar(),
                createNotesSection(),
                createStudentTable()
        );

        if (tableView != null) {
            tableView.setItems(filteredAttendanceList);
        }
    }

    private void loadDummyData() {
        Parent parent1 = new Parent("P001", "Phụ huynh Trần", "Nữ", "0900000001", "1980-01-01", "parent.tran@example.com", "Mẹ");
        Parent parent2 = new Parent("P002", "Phụ huynh Ngô", "Nam", "0900000002", "1975-05-10", "parent.ngo@example.com", "Cha");

        Student student1 = new Student("HV000069", "Trần Châu Hiếu", "Nam", "0912345678", "2005-03-15", "hieu.tc@example.com", parent1,"1");
        Student student2 = new Student("HV000075", "Ngô Việt Hoàng", "Nam", "0987654321", "2006-07-20", "hoang.nv@example.com", parent2, "1");
        Student student3 = new Student("HV000268", "Lê Ngọc Hoàng", "Nữ", "0911223344", "2005-11-01", "hoang.ln@example.com", parent1, "1");

        attendanceList.addAll(
                new StudentAttendanceData(1, student1, false, 2, 0.0, 3, "", 9),
                new StudentAttendanceData(2, student2, true, 5, 8.5, 5, "Tích cực", 10),
                new StudentAttendanceData(3, student3, false, 1, 0.0, 2, "", 6)
        );
    }

    private Node createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label sessionLabel = new Label("Buổi 33 (11/05)");
        sessionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        sessionLabel.setStyle(blackTextStyle);

        ComboBox<String> sessionSelector = new ComboBox<>(FXCollections.observableArrayList("13/20", "14/20", "15/20"));
        sessionSelector.setValue("13/20");

        Label dateLabelPrefix = new Label("Ngày");
        dateLabelPrefix.setStyle(blackTextStyle);
        DatePicker datePicker = new DatePicker(LocalDate.of(2025, 5, 11));
        datePicker.setPrefWidth(120);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveInfoButton = new Button("💾 Lưu thông tin");
        saveInfoButton.setStyle("-fx-background-color: #177bff; -fx-text-fill: white; -fx-font-weight: bold;");
        saveInfoButton.setOnAction(e -> saveInformation());

        Button sendNotificationButton = new Button("➤ Gửi thông báo");
        sendNotificationButton.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: bold;");
        sendNotificationButton.setOnAction(e -> sendNotification());

        Button exportExcelButton = new Button("📋 Xuất excel");
        exportExcelButton.setStyle("-fx-background-color: #5832a0; -fx-text-fill: white; -fx-font-weight: bold;");
        exportExcelButton.setOnAction(e -> exportToExcel());

        topBar.getChildren().addAll(sessionLabel, sessionSelector, dateLabelPrefix, datePicker, spacer,
                saveInfoButton, sendNotificationButton, exportExcelButton);
        return topBar;
    }

    private Node createNotesSection() {
        VBox notesBox = new VBox(5);
        notesBox.setPadding(new Insets(10));
        notesBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        Label notesTitleLabel = new Label("Ghi chú:");
        notesTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        notesTitleLabel.setStyle(blackTextStyle);

        TextArea notesTextArea = new TextArea();
        notesTextArea.setPromptText("Chủ đề:\nNội dung:\nBài tập về nhà:");
        notesTextArea.setPrefRowCount(4);
        notesTextArea.setWrapText(true);

        notesBox.getChildren().addAll(notesTitleLabel, notesTextArea);
        return notesBox;
    }

    private Label createStyledHeaderLabel(String text) {
        Label label = new Label(text);
        label.setStyle(blackBoldTextStyle);
        return label;
    }

    private Node createStudentTable() {
        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setPrefHeight(500);

        TableColumn<StudentAttendanceData, Integer> sttCol = new TableColumn<>();
        sttCol.setGraphic(createStyledHeaderLabel("STT"));
        sttCol.setCellValueFactory(new PropertyValueFactory<>("stt"));
        sttCol.setPrefWidth(40);
        sttCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, StudentAttendanceData> nameCol = new TableColumn<>();
        nameCol.setGraphic(createStyledHeaderLabel("Họ và tên"));
        nameCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        nameCol.setCellFactory(param -> new TableCell<>() {
            private final VBox contentBox = new VBox(2);
            private final Label nameLabel = new Label();
            private final Label idLabel = new Label();
            {
                nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
                idLabel.setFont(Font.font("System", FontWeight.NORMAL, 11));
                idLabel.setTextFill(Color.GRAY);
                contentBox.getChildren().addAll(nameLabel, idLabel);
            }
            @Override
            protected void updateItem(StudentAttendanceData attendanceData, boolean empty) {
                super.updateItem(attendanceData, empty);
                if (empty || attendanceData == null || attendanceData.getStudent() == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(attendanceData.getStudent().getName());
                    idLabel.setText(attendanceData.getStudent().getId());
                    setGraphic(contentBox);
                }
            }
        });
        nameCol.setPrefWidth(180);

        TableColumn<StudentAttendanceData, Boolean> homeworkCol = new TableColumn<>();
        Label btvnLabel = new Label("BTVN");
        btvnLabel.setStyle(blackBoldTextStyle);
        homeworkHeaderCheckbox = new CheckBox(); // Initialize here
        homeworkHeaderCheckbox.setOnAction(event -> { // Add "Select All" action
            boolean isSelected = homeworkHeaderCheckbox.isSelected();
            for (StudentAttendanceData item : filteredAttendanceList) { // Iterate over filtered list
                item.setHomeworkSubmitted(isSelected);
            }
            tableView.refresh(); // Refresh table to show changes in CheckBoxTableCell
        });
        VBox homeworkHeader = new VBox(5, homeworkHeaderCheckbox, btvnLabel);
        homeworkHeader.setAlignment(Pos.CENTER);
        homeworkCol.setGraphic(homeworkHeader);
        homeworkCol.setCellValueFactory(new PropertyValueFactory<>("homeworkSubmitted"));
        homeworkCol.setCellFactory(CheckBoxTableCell.forTableColumn(homeworkCol));
        homeworkCol.setEditable(true);
        homeworkCol.setPrefWidth(100);
        homeworkCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, Integer> punctualityCol = createStarRatingColumn(
                "Đi học đúng giờ",
                StudentAttendanceData::punctualityRatingProperty,
                combo -> this.punctualityFilterCombo = combo
        );
        punctualityCol.setPrefWidth(140);

        TableColumn<StudentAttendanceData, Double> homeworkGradeCol = new TableColumn<>();
        Label diemBtvnLabel = new Label("Điểm BTVN");
        diemBtvnLabel.setStyle(blackBoldTextStyle);
        Label rangeLabel = new Label("[0-10]");
        rangeLabel.setStyle(blackTextStyle);
        VBox homeworkGradeHeader = new VBox(5, diemBtvnLabel, rangeLabel);
        homeworkGradeHeader.setAlignment(Pos.CENTER);
        homeworkGradeCol.setGraphic(homeworkGradeHeader);
        homeworkGradeCol.setCellValueFactory(new PropertyValueFactory<>("homeworkGrade"));
        homeworkGradeCol.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Double>() {
            @Override public String toString(Double object) { return object == null ? "" : String.format("%.1f", object); }
            @Override public Double fromString(String string) {
                try {
                    double val = Double.parseDouble(string);
                    return Math.max(0.0, Math.min(10.0, val));
                } catch (NumberFormatException e) { return 0.0; }
            }
        }));
        homeworkGradeCol.setEditable(true);
        homeworkGradeCol.setPrefWidth(100);
        homeworkGradeCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, Integer> diligenceCol = createStarRatingColumn(
                "Chuyên cần",
                StudentAttendanceData::diligenceRatingProperty,
                combo -> this.diligenceFilterCombo = combo
        );
        diligenceCol.setPrefWidth(140);

        TableColumn<StudentAttendanceData, String> studentNotesCol = new TableColumn<>();
        studentNotesCol.setGraphic(createStyledHeaderLabel("Ghi chú"));
        studentNotesCol.setCellValueFactory(new PropertyValueFactory<>("studentSessionNotes"));
        studentNotesCol.setCellFactory(TextFieldTableCell.forTableColumn());
        studentNotesCol.setEditable(true);
        studentNotesCol.setPrefWidth(180);

        TableColumn<StudentAttendanceData, Integer> finalScoreCol = new TableColumn<>();
        finalScoreCol.setGraphic(createStyledHeaderLabel("Điểm TK"));
        finalScoreCol.setCellValueFactory(new PropertyValueFactory<>("finalNumericScore"));
        finalScoreCol.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override public String toString(Integer object) { return object == null ? "" : object.toString(); }
            @Override public Integer fromString(String string) {
                try { return Integer.parseInt(string); } catch (NumberFormatException e) { return 0; }
            }
        }));
        finalScoreCol.setEditable(true);
        finalScoreCol.setPrefWidth(80);
        finalScoreCol.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().addAll(sttCol, nameCol, homeworkCol, punctualityCol, homeworkGradeCol, diligenceCol, studentNotesCol, finalScoreCol);

        try {
            String cssPath = getClass().getResource("table-styles.css").toExternalForm();
            if (cssPath != null && !cssPath.isEmpty()) tableView.getStylesheets().add(cssPath);
            else System.err.println("Warning: table-styles.css not found or path is empty in package view.components.");
        } catch (NullPointerException e) {
            System.err.println("Warning: table-styles.css not found or error loading. Ensure it's in the classpath relative to this class.");
        }
        return tableView;
    }

    private int parseStarFilterValue(String filterValue) {
        if (filterValue == null || "Tất cả".equals(filterValue)) {
            return -1;
        }
        try {
            return Integer.parseInt(filterValue.split(" ")[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing star filter value: " + filterValue);
            return -1;
        }
    }

    private void applyFilters() {
        if (filteredAttendanceList == null || punctualityFilterCombo == null || diligenceFilterCombo == null) {
            return;
        }

        int punctualityStars = parseStarFilterValue(punctualityFilterCombo.getValue());
        int diligenceStars = parseStarFilterValue(diligenceFilterCombo.getValue());

        filteredAttendanceList.setPredicate(data -> {
            boolean punctualityMatch = true;
            if (punctualityStars != -1) {
                punctualityMatch = data.getPunctualityRating() == punctualityStars;
            }

            boolean diligenceMatch = true;
            if (diligenceStars != -1) {
                diligenceMatch = data.getDiligenceRating() == diligenceStars;
            }
            return punctualityMatch && diligenceMatch;
        });
    }

    private TableColumn<StudentAttendanceData, Integer> createStarRatingColumn(
            String headerText,
            java.util.function.Function<StudentAttendanceData, IntegerProperty> propertyExtractor,
            Consumer<ComboBox<String>> filterComboConsumer
    ) {
        TableColumn<StudentAttendanceData, Integer> column = new TableColumn<>();
        Label headerLabel = new Label(headerText);
        headerLabel.setStyle(blackBoldTextStyle);

        ComboBox<String> filterCombo = new ComboBox<>(FXCollections.observableArrayList("Tất cả", "5 Sao", "4 Sao", "3 Sao", "2 Sao", "1 Sao"));
        filterCombo.setValue("Tất cả");
        filterCombo.setOnAction(event -> applyFilters());

        filterComboConsumer.accept(filterCombo);

        VBox headerVBox = new VBox(5, headerLabel, filterCombo);
        headerVBox.setAlignment(Pos.CENTER);
        column.setGraphic(headerVBox);

        column.setCellValueFactory(cellData -> propertyExtractor.apply(cellData.getValue()).asObject());

        column.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Integer itemValue, boolean empty) {
                super.updateItem(itemValue, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    StudentAttendanceData attendanceData = (StudentAttendanceData) getTableRow().getItem();
                    IntegerProperty currentActualRatingProperty = propertyExtractor.apply(attendanceData);
                    StarRatingControl newStarRating = new StarRatingControl(currentActualRatingProperty, 5, true);
                    setGraphic(newStarRating);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        column.setEditable(true);
        return column;
    }

    private void saveInformation() {
        ObservableList<StudentAttendanceData> listToSave = attendanceList;

        if (confirm("Bạn có chắc muốn lưu thông tin cho " + listToSave.size() + " học viên?")) {
            for(StudentAttendanceData sad : listToSave) {
                System.out.println("Saving data for: " + sad.getStudent().getName() +
                        ", Homework: " + sad.isHomeworkSubmitted() +
                        ", Punctuality: " + sad.getPunctualityRating() +
                        ", Diligence: " + sad.getDiligenceRating()
                );
            }
            showSuccess("Thông tin đã được lưu (mô phỏng).");
        }
    }

    private void sendNotification() {
        if (confirm("Bạn có chắc muốn gửi thông báo?")) {
            showSuccess("Thông báo đã được gửi (mô phỏng).");
        }
    }

    private void exportToExcel() {
        showSuccess("Đã xuất ra Excel (mô phỏng).");
    }

    @Override
    public void refreshView() {
        super.refreshView();
        System.out.println(getViewId() + " refreshed.");
        // Consider if homeworkHeaderCheckbox state needs to be updated based on list items
        // after a full refresh or data load. For now, it only pushes changes down.
    }

    @Override
    public void onActivate() {
        super.onActivate();
        System.out.println(getViewId() + " activated.");
    }

    protected boolean confirm(String message) {
        System.out.println("Confirmation Dialog: " + message + " (Simulating YES)");
        return true;
    }

    // Changed visibility to public
    public void showSuccess(String message) {
        System.out.println("Success Notification: " + message);
    }
}

