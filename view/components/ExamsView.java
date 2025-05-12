package view.components;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.util.Callback;
import src.model.ClassSession;
import view.BaseScreenView;
import src.controller.ExamsController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * View class for the Exams screen
 */
public class ExamsView extends BaseScreenView {

    // UI Components
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private TextField keywordField;
    private ComboBox<Integer> pageSizeComboBox;
    private Button searchButton;
    private TableView<ClassSession> sessionTable;

    // Data references (view shouldn't own data)
    private FilteredList<ClassSession> filteredSessions;

    // Controller reference
    private ExamsController controller;

    public ExamsView() {
        super("Kỳ thi", "exams");
    }

    /**
     * Set the controller for this view
     */
    public void setController(ExamsController controller) {
        this.controller = controller;
    }

    @Override
    public void initializeView() {
        // Initialize the filter section
        initializeFilterSection();

        // Initialize the table
        initializeSessionTable();

        // Add components to root
        root.getChildren().addAll(
                createTitleBar(),
                createFilterSection(),
                sessionTable
        );

        root.setSpacing(15);
        root.setPadding(new Insets(20));
    }

    private HBox createTitleBar() {
        Label titleLabel = new Label("Kỳ thi");
        titleLabel.getStyleClass().add("view-title");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0D6EFD;");

        HBox titleBar = new HBox(titleLabel);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        return titleBar;
    }

    private void initializeFilterSection() {
        // From date picker with calendar icon
        fromDatePicker = new DatePicker();
        fromDatePicker.setPromptText("Từ");

        // To date picker with calendar icon
        toDatePicker = new DatePicker();
        toDatePicker.setPromptText("Đến");

        // Keyword field
        keywordField = new TextField();
        keywordField.setPromptText("Từ khóa");

        // Page size combo box
        pageSizeComboBox = new ComboBox<>();
        pageSizeComboBox.getItems().addAll(10, 20, 50, 100);
        pageSizeComboBox.setValue(20);
        pageSizeComboBox.setPromptText("Cỡ trang");

        // Search button
        searchButton = new Button();
        searchButton.setText("");
        searchButton.setGraphic(createSearchIcon());
        searchButton.setStyle("-fx-background-color: #0D6EFD; -fx-background-radius: 0;");
        searchButton.setPrefWidth(50);
        searchButton.setPrefHeight(38);

        // Add search action
        searchButton.setOnAction(e -> {
            if (controller != null) {
                controller.performSearch(
                        keywordField.getText(),
                        fromDatePicker.getValue(),
                        toDatePicker.getValue()
                );
            }
        });
    }

    private Node createSearchIcon() {
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        return searchIcon;
    }

    private HBox createFilterSection() {
        HBox filterContainer = new HBox();
        filterContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 0;");
        filterContainer.setPadding(new Insets(15));
        filterContainer.setSpacing(0); // Remove spacing between components

        // Create a common style for label backgrounds
        String labelStyle = "-fx-background-color: #e9ecef; -fx-padding: 10 15 10 15; -fx-text-fill: #212529;";
        String fieldStyle = "-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 0;";

        // From date section
        Label fromLabel = new Label("Từ:");
        fromLabel.setStyle(labelStyle);
        fromLabel.setPrefWidth(70);
        fromLabel.setPrefHeight(38);
        fromLabel.setAlignment(Pos.CENTER_LEFT);

        fromDatePicker.setStyle(fieldStyle);
        fromDatePicker.setPrefHeight(38);

        // Create button for calendar icon
        Button calendarBtn1 = new Button("📅");
        calendarBtn1.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 0;");
        calendarBtn1.setPrefHeight(38);

        HBox fromBox = new HBox(fromLabel, fromDatePicker, calendarBtn1);
        fromBox.setStyle("-fx-border-color: transparent; -fx-border-width: 0 10 0 0;"); // Add right margin

        // To date section
        Label toLabel = new Label("Đến:");
        toLabel.setStyle(labelStyle);
        toLabel.setPrefWidth(70);
        toLabel.setPrefHeight(38);
        toLabel.setAlignment(Pos.CENTER_LEFT);

        toDatePicker.setStyle(fieldStyle);
        toDatePicker.setPrefHeight(38);

        // Create button for calendar icon
        Button calendarBtn2 = new Button("📅");
        calendarBtn2.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 0;");
        calendarBtn2.setPrefHeight(38);

        // Add dropdown arrow
        Button toDropdownBtn = new Button("▼");
        toDropdownBtn.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 0;");
        toDropdownBtn.setPrefHeight(38);

        HBox toBox = new HBox(toLabel, toDatePicker, calendarBtn2, toDropdownBtn);
        toBox.setStyle("-fx-border-color: transparent; -fx-border-width: 0 10 0 0;"); // Add right margin

        // Keyword section
        Label keywordLabel = new Label("Từ khóa:");
        keywordLabel.setStyle(labelStyle);
        keywordLabel.setPrefWidth(70);
        keywordLabel.setPrefHeight(38);
        keywordLabel.setAlignment(Pos.CENTER_LEFT);

        keywordField.setStyle(fieldStyle);
        keywordField.setPrefHeight(38);

        // Add filter button to keyword field
        Button filterBtn = new Button("▼");
        filterBtn.setStyle("-fx-background-color: white; -fx-border-color: #ced4da; -fx-border-radius: 0;");
        filterBtn.setPrefHeight(38);

        HBox keywordBox = new HBox(keywordLabel, keywordField, filterBtn);
        keywordBox.setStyle("-fx-border-color: transparent; -fx-border-width: 0 10 0 0;"); // Add right margin

        // Page size section
        Label pageSizeLabel = new Label("Cỡ trang");
        pageSizeLabel.setStyle("-fx-padding: 10 10 10 10; -fx-text-fill: #212529;");
        pageSizeLabel.setPrefHeight(38);
        pageSizeLabel.setAlignment(Pos.CENTER_LEFT);

        pageSizeComboBox.setStyle(fieldStyle);
        pageSizeComboBox.setPrefHeight(38);
        pageSizeComboBox.setOnAction(e -> {
            if (controller != null) {
                controller.updatePageSize(pageSizeComboBox.getValue());
            }
        });

        HBox pageSizeBox = new HBox(pageSizeLabel, pageSizeComboBox);

        // Create a container for page size and search button with no spacing
        HBox rightControls = new HBox(0, pageSizeBox, searchButton);

        // Set proportional sizes for each section
        HBox.setHgrow(fromBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(toBox, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(keywordBox, javafx.scene.layout.Priority.ALWAYS);

        // Set preferred widths
        fromDatePicker.setPrefWidth(120);
        toDatePicker.setPrefWidth(120);
        keywordField.setPrefWidth(180);
        pageSizeComboBox.setPrefWidth(70);

        // Add all components to filter container
        filterContainer.getChildren().addAll(fromBox, toBox, keywordBox, rightControls);

        return filterContainer;
    }

    private void initializeSessionTable() {
        sessionTable = new TableView<>();
        sessionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sessionTable.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 0; -fx-background-color: white;");

        // Create columns
        TableColumn<ClassSession, Integer> sttColumn = new TableColumn<>("STT");
        sttColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(sessionTable.getItems().indexOf(cellData.getValue()) + 1));
        sttColumn.setPrefWidth(60);
        sttColumn.setResizable(false);
        sttColumn.setStyle("-fx-alignment: CENTER;");

        // Add sort indicators
        sttColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, String> nameColumn = new TableColumn<>("Tên kỳ thi");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCourseName()));
        nameColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, String> dateColumn = new TableColumn<>("Ngày bắt đầu");
        dateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDate();
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });
        dateColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, String> locationColumn = new TableColumn<>("Cơ sở");
        locationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRoom()));
        locationColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, Integer> studentCountColumn = new TableColumn<>("Học viên");
        studentCountColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getStudents().size()));
        studentCountColumn.setStyle("-fx-alignment: CENTER;");
        studentCountColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, String> statusColumn = new TableColumn<>("Trạng thái");
        statusColumn.setCellValueFactory(cellData -> {
            // Status mapping should be in controller or a util class
            return new SimpleStringProperty(controller != null ?
                    controller.getStatusDisplay(cellData.getValue().getStatus()) : "Khởi tạo");
        });
        statusColumn.setGraphic(createSortIcon());

        TableColumn<ClassSession, Void> scoreColumn = new TableColumn<>("Điểm");
        scoreColumn.setCellFactory(getScoreButtonCellFactory());

        // Add columns to table
        sessionTable.getColumns().addAll(
                sttColumn, nameColumn, dateColumn, locationColumn,
                studentCountColumn, statusColumn, scoreColumn
        );

        // Style row color
        sessionTable.setRowFactory(tv -> new TableRow<ClassSession>() {
            @Override
            protected void updateItem(ClassSession session, boolean empty) {
                super.updateItem(session, empty);
                if (session == null || empty) {
                    setStyle("");
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #e9f5fe;"); // Light blue for even rows
                } else {
                    setStyle("-fx-background-color: white;");
                }
            }
        });

        sessionTable.setPrefHeight(300);
    }

    private Node createSortIcon() {
        Label sortIcon = new Label("↕");
        sortIcon.setStyle("-fx-font-size: 8px; -fx-text-fill: #6c757d;");
        return sortIcon;
    }

    private Callback<TableColumn<ClassSession, Void>, TableCell<ClassSession, Void>> getScoreButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<ClassSession, Void> call(TableColumn<ClassSession, Void> param) {
                return new TableCell<>() {
                    private final Button scoreBtn = new Button("Điểm →");
                    {
                        scoreBtn.setStyle("-fx-text-fill: #0D6EFD; -fx-border-color: #0D6EFD; -fx-background-color: transparent; " +
                                "-fx-border-radius: 20; -fx-background-radius: 20; -fx-min-width: 100;");
                        scoreBtn.setOnAction(event -> {
                            ClassSession session = getTableView().getItems().get(getIndex());
                            if (controller != null) {
                                controller.showScores(session);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(scoreBtn);
                        }
                    }
                };
            }
        };
    }

    /**
     * Set the data to be displayed in the table
     */
    public void setSessionData(FilteredList<ClassSession> filteredData) {
        this.filteredSessions = filteredData;
        sessionTable.setItems(filteredSessions);
    }

    /**
     * Get UI component values for controller use
     */
    public String getKeyword() {
        return keywordField.getText();
    }

    public LocalDate getFromDate() {
        return fromDatePicker.getValue();
    }

    public LocalDate getToDate() {
        return toDatePicker.getValue();
    }

    public Integer getPageSize() {
        return pageSizeComboBox.getValue();
    }

    @Override
    public void refreshView() {
        if (sessionTable != null) {
            sessionTable.refresh();
        }
    }

    @Override
    public void onShow() {
        super.onShow();
        if (controller != null) {
            controller.loadData();
        }
    }
}