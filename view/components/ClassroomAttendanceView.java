package view.components;

import javafx.beans.property.IntegerProperty;
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
import view.BaseScreenView;
import src.model.attendance.StudentAttendanceData;
import src.controller.ClassroomAttendanceController;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function; // Sửa lỗi import Function

public class ClassroomAttendanceView extends BaseScreenView {
    private TableView<StudentAttendanceData> tableView;
    // filteredAttendanceList sẽ được cung cấp bởi Controller
    // private FilteredList<StudentAttendanceData> filteredAttendanceList;
    private final ClassroomAttendanceController controller;

    // UI Controls
    private ComboBox<String> punctualityFilterCombo;
    private ComboBox<String> diligenceFilterCombo;
    private CheckBox homeworkHeaderCheckbox;
    private DatePicker datePicker;
    private TextArea notesTextArea;
    private ComboBox<Integer> sessionSelector;

    private final String blackTextStyle = "-fx-text-fill: black;";
    private final String blackBoldTextStyle = "-fx-text-fill: black; -fx-font-weight: bold;";

    public ClassroomAttendanceView() {
        super("Điểm danh và đánh giá buổi học", "classroom-attendance-view");
        this.controller = new ClassroomAttendanceController(this);
        // initializeView() được gọi bởi BaseScreenView
    }

    /**
     * Phương thức này sẽ được gọi bởi một lớp quản lý (ví dụ: NavigationManager)
     * để cung cấp context (classId) cho màn hình này và kích hoạt tải dữ liệu.
     * @param classId ID của lớp học để hiển thị.
     */
    public void activateWithContext(String classId) {
        System.out.println("View: activateWithContext called with ClassID: " + classId);
        if (controller != null) {
            // Yêu cầu Controller tải dữ liệu với classId mới
            controller.loadContextForClass(classId);
        } else {
            showError("Lỗi nghiêm trọng: Controller chưa được khởi tạo.");
        }
    }


    @Override
    public void initializeView() {
        this.root.setSpacing(15);
        this.root.setPadding(new Insets(20));
        this.root.setStyle("-fx-background-color: #f0f2f5;");

        // TableView sẽ được tạo, nhưng danh sách sẽ được đặt bởi Controller
        Node tableNode = createStudentTable();

        this.root.getChildren().addAll(
                createTopBar(),
                createNotesSection(),
                tableNode,
                createBottomBar()
        );

        activateWithContext(mainController.getCurrentClassId());
    }

    /**
     * Được gọi bởi Controller để cung cấp danh sách dữ liệu (đã được lọc) cho TableView.
     */
    public void setFilteredAttendanceList(FilteredList<StudentAttendanceData> filteredList) {
        // this.filteredAttendanceList = filteredList; // Không cần lưu trữ tham chiếu này nữa
        if (tableView != null) {
            tableView.setItems(filteredList);
            tableView.refresh();
        } else {
            System.err.println("View: tableView is null when trying to setFilteredAttendanceList.");
        }
    }

    private void loadDummyData() {
        Parent parent1 = new Parent("P001", "Phụ huynh Trần", "Nữ", "0900000001", "1980-01-01", "parent.tran@example.com", "Mẹ");
        Parent parent2 = new Parent("P002", "Phụ huynh Ngô", "Nam", "0900000002", "1975-05-10", "parent.ngo@example.com", "Cha");

//        Student student1 = new Student("HV000069", "Trần Châu Hiếu", "Nam", "0912345678", "2005-03-15", "hieu.tc@example.com", parent1,"1");
//        Student student2 = new Student("HV000075", "Ngô Việt Hoàng", "Nam", "0987654321", "2006-07-20", "hoang.nv@example.com", parent2, "1");
//        Student student3 = new Student("HV000268", "Lê Ngọc Hoàng", "Nữ", "0911223344", "2005-11-01", "hoang.ln@example.com", parent1, "1");
//
//        attendanceList.addAll(
//                new StudentAttendanceData(1, student1, false, 2, 0.0, 3, "", 9),
//                new StudentAttendanceData(2, student2, true, 5, 8.5, 5, "Tích cực", 10),
//                new StudentAttendanceData(3, student3, false, 1, 0.0, 2, "", 6)
//        );

    public void setAvailableSessions(ObservableList<Integer> availableSessions, Integer currentSessionToSelect) {
        System.out.println("View: setAvailableSessions called. Sessions: " + availableSessions + ", CurrentToSelect: " + currentSessionToSelect);
        if (sessionSelector == null) {
            System.err.println("View: CRITICAL - sessionSelector is NULL in setAvailableSessions.");
            return;
        }

        if (availableSessions == null || availableSessions.isEmpty()) {
            sessionSelector.setItems(FXCollections.observableArrayList());
            sessionSelector.setPromptText("Không có buổi");
            sessionSelector.setValue(null);
        } else {
            sessionSelector.setItems(availableSessions);
            if (currentSessionToSelect != null && availableSessions.contains(currentSessionToSelect)) {
                sessionSelector.setValue(currentSessionToSelect);
            } else if (!availableSessions.isEmpty()) {
                sessionSelector.setValue(availableSessions.get(0));
            } else {
                sessionSelector.setValue(null);
            }
        }
    }


    private Node createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        sessionSelector = new ComboBox<>();
        sessionSelector.setPromptText("Chọn buổi");
        sessionSelector.setOnAction(e -> {
            if (controller != null) controller.handleSessionChange();
        });

        Label sessionLabel = new Label("Buổi:");
        sessionLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        sessionLabel.setStyle(blackTextStyle);

        Label dateLabelPrefix = new Label("Ngày");
        dateLabelPrefix.setStyle(blackTextStyle);

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(120);
        datePicker.setOnAction(e -> {
            if (controller != null) controller.updateSessionDate();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("← Quay lại");
        backButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold;");
        backButton.setOnAction(e -> {
            navigationController.goBack();
        });

        Button saveInfoButton = new Button("💾 Lưu thông tin");
        saveInfoButton.setStyle("-fx-background-color: #177bff; -fx-text-fill: white; -fx-font-weight: bold;");
        saveInfoButton.setOnAction(e -> {
            if (controller != null) controller.saveInformation();
        });

        Button exportExcelButton = new Button("📋 Xuất excel");
        exportExcelButton.setStyle("-fx-background-color: #5832a0; -fx-text-fill: white; -fx-font-weight: bold;");
        exportExcelButton.setOnAction(e -> {
            if (controller != null) controller.exportToExcel();
        });

        topBar.getChildren().addAll(backButton, sessionLabel, sessionSelector, dateLabelPrefix, datePicker, spacer,
                saveInfoButton, exportExcelButton);
        return topBar;
    }

    private Node createBottomBar() {
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(5, 0, 0, 0));

        Button saveHomeworkButton = new Button("Lưu điểm bài tập");
        saveHomeworkButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        saveHomeworkButton.setOnAction(e -> {
            if (controller != null) controller.saveHomeworkSubmissions();
        });

        bottomBar.getChildren().add(saveHomeworkButton);
        return bottomBar;
    }

    private Node createNotesSection() {
        VBox notesBox = new VBox(5);
        notesBox.setPadding(new Insets(10));
        notesBox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");
        Label notesTitleLabel = new Label("Ghi chú buổi học:");
        notesTitleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        notesTitleLabel.setStyle(blackTextStyle);
        notesTextArea = new TextArea();
        notesTextArea.setPromptText("Chủ đề buổi học...\nNội dung chính...\nBài tập về nhà (nếu có)...");
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
        // Controller sẽ cung cấp danh sách dữ liệu cho TableView sau
        // tableView.setItems(this.filteredAttendanceList); // Không set ở đây nữa

        TableColumn<StudentAttendanceData, StudentAttendanceData> nameCol = new TableColumn<>();
        nameCol.setGraphic(createStyledHeaderLabel("Họ và tên"));
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue()));
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
                    idLabel.setText("ID: " + attendanceData.getStudent().getId());
                    setGraphic(contentBox);
                }
            }
        });
        nameCol.setPrefWidth(180);

        TableColumn<StudentAttendanceData, Boolean> homeworkCol = new TableColumn<>();
        Label btvnLabel = new Label("BTVN");
        btvnLabel.setStyle(blackBoldTextStyle);
        homeworkHeaderCheckbox = new CheckBox();
        homeworkHeaderCheckbox.setOnAction(e -> {
            if (controller != null) controller.updateAllHomeworkStatus();
        });
        VBox homeworkHeader = new VBox(5, homeworkHeaderCheckbox, btvnLabel);
        homeworkHeader.setAlignment(Pos.CENTER);
        homeworkCol.setGraphic(homeworkHeader);

        // Thay đổi: Sửa cách thiết lập cellValueFactory để sử dụng BooleanProperty
        homeworkCol.setCellValueFactory(cellData ->
                cellData.getValue().homeworkSubmittedProperty());

        // Thay đổi: Sử dụng cách khởi tạo CheckBoxTableCell đúng
        homeworkCol.setCellFactory(CheckBoxTableCell.forTableColumn(index ->
                tableView.getItems().get(index).homeworkSubmittedProperty()));

        homeworkCol.setEditable(true);
        homeworkCol.setPrefWidth(100);
        homeworkCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, Integer> punctualityCol = createStarRatingColumn(
                "Đúng giờ", StudentAttendanceData::punctualityRatingProperty,
                combo -> this.punctualityFilterCombo = combo);
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
                    double val = Double.parseDouble(string.replace(",", "."));
                    return Math.max(0.0, Math.min(10.0, val));
                } catch (NumberFormatException e) { return 0.0; }
            }
        }));
        homeworkGradeCol.setEditable(true);
        homeworkGradeCol.setPrefWidth(100);
        homeworkGradeCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<StudentAttendanceData, Integer> diligenceCol = createStarRatingColumn(
                "Chuyên cần", StudentAttendanceData::diligenceRatingProperty,
                combo -> this.diligenceFilterCombo = combo);
        diligenceCol.setPrefWidth(140);

        TableColumn<StudentAttendanceData, String> studentNotesCol = new TableColumn<>();
        studentNotesCol.setGraphic(createStyledHeaderLabel("Ghi chú HV"));
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

        tableView.getColumns().addAll(nameCol, homeworkCol, punctualityCol, homeworkGradeCol, diligenceCol, studentNotesCol, finalScoreCol);
        return tableView;
    }

    public int getPunctualityFilterValue() {
        if (punctualityFilterCombo == null || punctualityFilterCombo.getValue() == null) return -1;
        return parseStarFilterValue(punctualityFilterCombo.getValue());
    }

    public int getDiligenceFilterValue() {
        if (diligenceFilterCombo == null || diligenceFilterCombo.getValue() == null) return -1;
        return parseStarFilterValue(diligenceFilterCombo.getValue());
    }

    private int parseStarFilterValue(String filterValue) {
        if (filterValue == null || "Tất cả".equals(filterValue)) return -1;
        try { return Integer.parseInt(filterValue.split(" ")[0]); }
        catch (Exception e) { System.err.println("Error parsing star filter: " + filterValue); return -1; }
    }

    private TableColumn<StudentAttendanceData, Integer> createStarRatingColumn(
            String headerText,
            Function<StudentAttendanceData, IntegerProperty> propertyExtractor, // Sửa import
            Consumer<ComboBox<String>> filterComboConsumer) {
        TableColumn<StudentAttendanceData, Integer> column = new TableColumn<>();
        Label headerLabel = new Label(headerText);
        headerLabel.setStyle(blackBoldTextStyle);
        ComboBox<String> filterCombo = new ComboBox<>(FXCollections.observableArrayList("Tất cả", "5 Sao", "4 Sao", "3 Sao", "2 Sao", "1 Sao"));
        filterCombo.setValue("Tất cả");
        filterComboConsumer.accept(filterCombo);
        filterCombo.setOnAction(e -> {
            if (controller != null) controller.applyFilters();
        });
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
                    StudentAttendanceData attendanceData = getTableRow().getItem();
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

    public LocalDate getSelectedDate() { return datePicker != null ? datePicker.getValue() : LocalDate.now(); }
    public String getSessionNotes() { return notesTextArea != null ? notesTextArea.getText() : ""; }
    public boolean isHomeworkSelectAllChecked() { return homeworkHeaderCheckbox != null && homeworkHeaderCheckbox.isSelected(); }
    public Integer getSelectedSessionNumber() { return sessionSelector != null ? sessionSelector.getValue() : null; }

    public void refreshTable() { if (tableView != null) tableView.refresh(); }
    @Override public void refreshView() { super.refreshView(); refreshTable(); }
    public void setSelectedDate(LocalDate date) { if (datePicker != null && date != null) datePicker.setValue(date); }
    public void setSessionNotes(String notes) { if (notesTextArea != null) notesTextArea.setText(notes != null ? notes : "");}

    public boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(response -> response == ButtonType.YES).isPresent();
    }

    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText("Thành công");
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Lỗi");
        alert.showAndWait();
    }
}