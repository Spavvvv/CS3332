package view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import src.controller.ClassroomController;
import src.model.classroom.Classroom;
import view.BaseScreenView;

/**
 * View for Classroom Management Screen
 */
public class RoomView extends BaseScreenView {

    // UI components
    private TextField txtKeyword;
    private ComboBox<String> cmbStatus;
    private ComboBox<String> cmbPageSize;
    private TableView<Classroom> tblClassroom;

    // Controller
    private ClassroomController controller;

    public RoomView() {
        super("Phòng học", "classrooms");
        this.controller = new ClassroomController();
    }

    @Override
    public void initializeView() {
        // Main layout with padding
        root.setSpacing(10);
        root.setPadding(new Insets(20));

        // Screen title
        Label lblTitle = new Label("Phòng học");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#1E88E5"));

        // Search and filter area
        HBox searchBar = createSearchBar();
        searchBar.setPadding(new Insets(10));
        searchBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Create table view
        createTableView();

        // Add all to main layout
        root.getChildren().addAll(lblTitle, searchBar, tblClassroom);
        VBox.setVgrow(tblClassroom, Priority.ALWAYS);

        // Load data into table
        refreshView();
    }

    private HBox createSearchBar() {
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // Keyword field
        Label lblKeyword = new Label("Từ khóa:");
        lblKeyword.setMinWidth(70);
        lblKeyword.setTextFill(Color.BLACK);
        txtKeyword = new TextField();
        txtKeyword.setPromptText("Từ khóa");
        txtKeyword.setPrefWidth(350);

        // Status combobox
        Label lblStatus = new Label("Trạng thái:");
        lblStatus.setTextFill(Color.BLACK);
        cmbStatus = new ComboBox<>();
        cmbStatus.setPromptText("Chọn");
        cmbStatus.setItems(FXCollections.observableArrayList("Tất cả", "Sử dụng", "Bảo trì", "Không sử dụng"));
        cmbStatus.setValue("Tất cả");
        cmbStatus.setPrefWidth(200);

        // Page size combobox
        Label lblPageSize = new Label("Cỡ trang:");
        lblPageSize.setTextFill(Color.BLACK);
        cmbPageSize = new ComboBox<>();
        cmbPageSize.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        cmbPageSize.setValue("20");
        cmbPageSize.setPrefWidth(120);

        // Search button with simple search icon
        Button btnSearch = new Button();
        Text searchIcon = new Text("🔍"); // Unicode magnifying glass
        searchIcon.setFill(Color.WHITE);
        btnSearch.setGraphic(searchIcon);
        btnSearch.setStyle("-fx-background-color: #1E88E5; -fx-text-fill: white;");
        btnSearch.setPrefSize(40, 10);

        // Add search event
        btnSearch.setOnAction(e -> performSearch());
        txtKeyword.setOnAction(e -> performSearch());
        cmbStatus.setOnAction(e -> performSearch());

        // Add flexible space before search button
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchBar.getChildren().addAll(lblKeyword, txtKeyword, lblStatus, cmbStatus, lblPageSize, cmbPageSize, spacer, btnSearch);
        return searchBar;
    }

    private void createTableView() {
        tblClassroom = new TableView<>();
        tblClassroom.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // STT Column
        TableColumn<Classroom, Integer> colSTT = new TableColumn<>("STT");
        colSTT.setCellValueFactory(new PropertyValueFactory<>("stt"));
        colSTT.setMaxWidth(70);
        colSTT.setMinWidth(50);
        colSTT.setSortable(false);

        // Code Column
        TableColumn<Classroom, String> colMa = new TableColumn<>("Mã");
        colMa.setCellValueFactory(new PropertyValueFactory<>("ma"));
        colMa.setMaxWidth(100);
        colMa.setMinWidth(80);

        // Name Column
        TableColumn<Classroom, String> colTen = new TableColumn<>("Tên");
        colTen.setCellValueFactory(new PropertyValueFactory<>("ten"));

        // Floor Column
        TableColumn<Classroom, Integer> colTang = new TableColumn<>("Tầng");
        colTang.setCellValueFactory(new PropertyValueFactory<>("tang"));
        colTang.setMaxWidth(100);
        colTang.setMinWidth(80);

        // Capacity Column
        TableColumn<Classroom, Integer> colSucChua = new TableColumn<>("Sức chứa");
        colSucChua.setCellValueFactory(new PropertyValueFactory<>("sucChua"));
        colSucChua.setMaxWidth(100);
        colSucChua.setMinWidth(80);

        // Status Column
        TableColumn<Classroom, String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setCellValueFactory(new PropertyValueFactory<>("trangThai"));
        colTrangThai.setMaxWidth(150);
        colTrangThai.setMinWidth(120);

        // Set cell factory for status column to display button
        colTrangThai.setCellFactory(column -> {
            return new TableCell<>() {
                final Button button = new Button();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        button.setText(item);

                        // Style based on status
                        switch (item) {
                            case "Sử dụng":
                                button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
                                break;
                            case "Bảo trì":
                                button.setStyle("-fx-background-color: #FFC107; -fx-text-fill: white; -fx-background-radius: 20;");
                                break;
                            case "Không sử dụng":
                                button.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 20;");
                                break;
                            default:
                                button.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-background-radius: 20;");
                        }

                        button.setPrefWidth(100);
                        setGraphic(button);
                    }
                }
            };
        });

        // Action Column for details and edit
        TableColumn<Classroom, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setCellFactory(column -> {
            return new TableCell<>() {
                final Button btnView = new Button();
                final Button btnEdit = new Button();
                final HBox hbox = new HBox(10);

                {
                    // Details button with eye icon
                    HBox eyeIcon = createEyeIcon();
                    btnView.setGraphic(eyeIcon);
                    btnView.setStyle("-fx-background-color: transparent;");
                    btnView.setTooltip(new Tooltip("Xem chi tiết"));

                    // Edit button with pencil icon
                    Text editIcon = new Text("✏️"); // Unicode pencil
                    btnEdit.setGraphic(editIcon);
                    btnEdit.setStyle("-fx-background-color: transparent;");
                    btnEdit.setTooltip(new Tooltip("Sửa"));

                    // Add actions
                    btnView.setOnAction(event -> {
                        Classroom data = getTableView().getItems().get(getIndex());
                        showDetails(data);
                    });

                    btnEdit.setOnAction(event -> {
                        Classroom data = getTableView().getItems().get(getIndex());
                        editClassroom(data);
                    });

                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(btnView, btnEdit);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : hbox);
                }
            };
        });
        colActions.setMaxWidth(120);
        colActions.setMinWidth(100);
        colActions.setSortable(false);

        // Add columns to table
        tblClassroom.getColumns().addAll(colSTT, colMa, colTen, colTang, colSucChua, colTrangThai, colActions);
    }

    /**
     * Create a simple eye icon
     */
    private HBox createEyeIcon() {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);

        // Create outer circle
        Circle outerCircle = new Circle(9, Color.web("#2E7D32"));
        outerCircle.setStroke(Color.web("#2E7D32"));
        outerCircle.setStrokeWidth(1.5);
        outerCircle.setFill(Color.TRANSPARENT);

        // Create inner circle (pupil)
        Circle innerCircle = new Circle(3, Color.web("#2E7D32"));

        // Stack shapes
        container.getChildren().addAll(outerCircle, innerCircle);

        return container;
    }

    private void performSearch() {
        String keyword = txtKeyword.getText().trim();
        String status = cmbStatus.getValue();

        // Use controller to filter classrooms
        controller.filterClassrooms(keyword, status);

        // Refresh view to show filtered data
        refreshView();
    }

    private void showDetails(Classroom classroom) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phòng học");
        alert.setHeaderText("Thông tin phòng học: " + classroom.getTen());

        // Create formatted content
        StringBuilder content = new StringBuilder();
        content.append("Mã: ").append(classroom.getMa()).append("\n");
        content.append("Tên: ").append(classroom.getTen()).append("\n");
        content.append("Tầng: ").append(classroom.getTang()).append("\n");
        content.append("Sức chứa: ").append(classroom.getSucChua()).append(" người\n");
        content.append("Trạng thái: ").append(classroom.getTrangThai());

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private void editClassroom(Classroom classroom) {
        // Create dialog for editing
        Dialog<Classroom> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa phòng học");
        dialog.setHeaderText("Chỉnh sửa thông tin phòng " + classroom.getTen());

        // Set buttons
        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Create form fields with existing data
        TextField txtMa = new TextField(classroom.getMa());
        TextField txtTen = new TextField(classroom.getTen());
        Spinner<Integer> spnTang = new Spinner<>(1, 20, classroom.getTang());
        Spinner<Integer> spnSucChua = new Spinner<>(10, 500, classroom.getSucChua());
        ComboBox<String> cmbTrangThai = new ComboBox<>();
        cmbTrangThai.getItems().addAll("Sử dụng", "Bảo trì", "Không sử dụng");
        cmbTrangThai.setValue(classroom.getTrangThai());

        // Add fields to grid
        grid.add(new Label("Mã:"), 0, 0);
        grid.add(txtMa, 1, 0);
        grid.add(new Label("Tên:"), 0, 1);
        grid.add(txtTen, 1, 1);
        grid.add(new Label("Tầng:"), 0, 2);
        grid.add(spnTang, 1, 2);
        grid.add(new Label("Sức chứa:"), 0, 3);
        grid.add(spnSucChua, 1, 3);
        grid.add(new Label("Trạng thái:"), 0, 4);
        grid.add(cmbTrangThai, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Request focus on the first field
        txtMa.requestFocus();

        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Update classroom properties
                classroom.setMa(txtMa.getText());
                classroom.setTen(txtTen.getText());
                classroom.setTang(spnTang.getValue());
                classroom.setSucChua(spnSucChua.getValue());
                classroom.setTrangThai(cmbTrangThai.getValue());
                return classroom;
            }
            return null;
        });

        // Show dialog and process result
        dialog.showAndWait().ifPresent(updatedClassroom -> {
            // Save to database using controller
            Classroom savedClassroom = controller.saveClassroom(updatedClassroom);
            if (savedClassroom != null) {
                // Success
                refreshView();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Cập nhật phòng học thành công",
                        "Thông tin phòng học đã được cập nhật.");
            } else {
                // Error
                showAlert(Alert.AlertType.ERROR, "Lỗi",
                        "Không thể cập nhật phòng học",
                        "Có lỗi xảy ra khi lưu thông tin phòng học.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void refreshView() {
        // Set items in table view from controller's filtered list
        tblClassroom.setItems(controller.getFilteredClassrooms());
    }

    @Override
    public boolean requiresAuthentication() {
        // Require authentication to access this screen
        return true;
    }
}
