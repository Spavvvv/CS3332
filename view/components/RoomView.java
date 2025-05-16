
package view.components;

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
    private ComboBox<String> cmbStatusFilter;
    private ComboBox<String> cmbPageSize;
    private TableView<Classroom> tblClassroom;

    // Controller
    private ClassroomController controller;

    public RoomView() {
        super("Quản lý Phòng học", "classrooms");
    }

    @Override
    public void initializeView() {
        // Khởi tạo controller nếu chưa có, ví dụ:
        if (this.controller == null) {
            this.controller = new ClassroomController();
        }

        // Main layout with padding
        root.setSpacing(10);
        root.setPadding(new Insets(20));

        // Screen title
        Label lblTitle = new Label("Danh sách Phòng học");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#1E88E5"));

        // Search and filter area
        HBox searchBar = createSearchBar();
        searchBar.setPadding(new Insets(10));
        searchBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Create table view
        createTableView();

        // Add button (ví dụ)
        Button btnAddRoom = new Button("Thêm phòng học");
        btnAddRoom.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        btnAddRoom.setOnAction(e -> addNewClassroom());
        HBox bottomControls = new HBox(btnAddRoom);
        bottomControls.setAlignment(Pos.CENTER_RIGHT);
        bottomControls.setPadding(new Insets(10, 0, 0, 0));

        // Add all to main layout
        root.getChildren().addAll(lblTitle, searchBar, tblClassroom, bottomControls);
        VBox.setVgrow(tblClassroom, Priority.ALWAYS);

        // Load data into table
        refreshView();
    }

    private HBox createSearchBar() {
        HBox searchBarLayout = new HBox(15);
        searchBarLayout.setAlignment(Pos.CENTER_LEFT);

        // Keyword field
        Label lblKeyword = new Label("Từ khóa:");
        lblKeyword.setMinWidth(70);
        lblKeyword.setTextFill(Color.BLACK);
        txtKeyword = new TextField();
        txtKeyword.setPromptText("Nhập mã, tên phòng...");
        txtKeyword.setPrefWidth(300);

        // Status combobox
        Label lblStatusFilter = new Label("Trạng thái:");
        lblStatusFilter.setTextFill(Color.BLACK);
        cmbStatusFilter = new ComboBox<>();
        cmbStatusFilter.setPromptText("Tất cả trạng thái");
        cmbStatusFilter.setItems(FXCollections.observableArrayList("Tất cả", "Active", "Inactive", "Maintenance"));
        cmbStatusFilter.setValue("Tất cả");
        cmbStatusFilter.setPrefWidth(180);

        // Page size combobox (Optional, nếu bạn có phân trang)
        Label lblPageSize = new Label("Hiện thị:");
        lblPageSize.setTextFill(Color.BLACK);
        cmbPageSize = new ComboBox<>();
        cmbPageSize.setItems(FXCollections.observableArrayList("10 dòng", "20 dòng", "50 dòng", "100 dòng"));
        cmbPageSize.setValue("20 dòng");
        cmbPageSize.setPrefWidth(100);

        // Search button with simple search icon
        Button btnSearch = new Button();
        Text searchIcon = new Text("🔍");
        searchIcon.setFill(Color.WHITE);
        btnSearch.setGraphic(searchIcon);
        btnSearch.setStyle("-fx-background-color: #007bff; -fx-text-fill: white;");
        btnSearch.setPrefSize(40, 30);
        btnSearch.setTooltip(new Tooltip("Tìm kiếm"));

        // Add search event
        btnSearch.setOnAction(e -> refreshView());
        txtKeyword.setOnAction(e -> refreshView());
        cmbStatusFilter.setOnAction(e -> refreshView());
        cmbPageSize.setOnAction(e -> refreshView());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchBarLayout.getChildren().addAll(lblKeyword, txtKeyword, lblStatusFilter, cmbStatusFilter, lblPageSize, cmbPageSize, spacer, btnSearch);
        return searchBarLayout;
    }

    private void createTableView() {
        tblClassroom = new TableView<>();
        tblClassroom.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Apply default style to the TableView header
        tblClassroom.setStyle("-fx-table-header-background: #f0f0f0;");

        // Common style for all column headers - make the text black and bold
        String headerStyle = "-fx-font-weight: bold; -fx-text-fill: black; -fx-alignment: CENTER;";


// STT Column
        TableColumn<Classroom, Integer> colSTT = new TableColumn<>();
        setBlackHeaderText(colSTT, "STT");
        colSTT.setCellValueFactory(new PropertyValueFactory<>("stt"));
        colSTT.setPrefWidth(50);
        colSTT.setMaxWidth(70);
        colSTT.setSortable(false);

// Code Column - Mã phòng
        TableColumn<Classroom, String> colCode = new TableColumn<>();
        setBlackHeaderText(colCode, "Mã Phòng");
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colCode.setPrefWidth(100);
        colCode.setMinWidth(80);

// Name Column - Tên phòng
        TableColumn<Classroom, String> colRoomName = new TableColumn<>();
        setBlackHeaderText(colRoomName, "Tên Phòng");
        colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colRoomName.setPrefWidth(250);
        colRoomName.setMinWidth(150);

// Floor Column - Tầng
        TableColumn<Classroom, Integer> colFloor = new TableColumn<>();
        setBlackHeaderText(colFloor, "Tầng");
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colFloor.setPrefWidth(80);
        colFloor.setMaxWidth(100);

// Capacity Column - Sức chứa
        TableColumn<Classroom, Integer> colCapacity = new TableColumn<>();
        setBlackHeaderText(colCapacity, "Sức chứa");
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCapacity.setPrefWidth(100);
        colCapacity.setMaxWidth(120);

// Status Column - Trạng thái
        TableColumn<Classroom, String> colStatus = new TableColumn<>();
        setBlackHeaderText(colStatus, "Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(150);
        colStatus.setMinWidth(120);


        // Set cell factory for status column with improved design
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Create an HBox to hold the status indicator and text
                    HBox statusBox = new HBox(8);
                    statusBox.setAlignment(Pos.CENTER);

                    // Create a circle indicator
                    Circle statusIndicator = new Circle(6);

                    // Create a label for the status text
                    Label statusLabel = new Label(item);
                    statusLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

                    // Style based on status
                    if ("Active".equalsIgnoreCase(item) || "Sử dụng".equalsIgnoreCase(item)) {
                        statusIndicator.setFill(Color.web("#28a745"));
                        statusLabel.setTextFill(Color.web("#28a745"));
                    } else if ("Maintenance".equalsIgnoreCase(item) || "Bảo trì".equalsIgnoreCase(item)) {
                        statusIndicator.setFill(Color.web("#ffc107"));
                        statusLabel.setTextFill(Color.web("#856404"));
                    } else if ("Inactive".equalsIgnoreCase(item) || "Không sử dụng".equalsIgnoreCase(item)) {
                        statusIndicator.setFill(Color.web("#dc3545"));
                        statusLabel.setTextFill(Color.web("#dc3545"));
                    } else {
                        statusIndicator.setFill(Color.web("#6c757d"));
                        statusLabel.setTextFill(Color.web("#6c757d"));
                    }

                    // Add components to the HBox
                    statusBox.getChildren().addAll(statusIndicator, statusLabel);

                    // Border and background
                    statusBox.setPadding(new Insets(3, 10, 3, 10));
                    statusBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15px; -fx-border-radius: 15px; -fx-border-color: #dee2e6;");

                    setGraphic(statusBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Action Column for details and edit
        TableColumn<Classroom, Void> colActions = new TableColumn<>();
        setBlackHeaderText(colActions,"Thao tác");
        colActions.setCellFactory(column -> new TableCell<>() {
            final Button btnView = new Button();
            final Button btnEdit = new Button("🔨");
            final Button btnDelete = new Button("❌");
            final HBox hbox = new HBox(10);

            {
                // View button with eye icon
                Text eyeIcon = new Text("👁");
                btnView.setGraphic(eyeIcon);
                btnView.setStyle("-fx-background-color: transparent; -fx-padding: 5;");
                btnView.setTooltip(new Tooltip("Xem chi tiết"));

                // Edit button
                btnEdit.setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-text-fill: #007bff;");
                btnEdit.setTooltip(new Tooltip("Chỉnh sửa"));

                // Delete button
                btnDelete.setStyle("-fx-background-color: transparent; -fx-padding: 5; -fx-text-fill: #dc3545;");
                btnDelete.setTooltip(new Tooltip("Xóa phòng"));

                btnView.setOnAction(event -> {
                    Classroom data = getTableView().getItems().get(getIndex());
                    showDetails(data);
                });

                btnEdit.setOnAction(event -> {
                    Classroom data = getTableView().getItems().get(getIndex());
                    editClassroom(data);
                });

                btnDelete.setOnAction(event -> {
                    Classroom data = getTableView().getItems().get(getIndex());
                    deleteClassroom(data);
                });

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnView, btnEdit, btnDelete);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
        colActions.setPrefWidth(120);
        colActions.setMaxWidth(150);
        colActions.setSortable(false);
        colActions.setStyle(headerStyle);

        tblClassroom.getColumns().addAll(colSTT, colCode, colRoomName, colFloor, colCapacity, colStatus, colActions);
        tblClassroom.setPlaceholder(new Label("Không có dữ liệu phòng học."));
    }

    private void addNewClassroom() {
        editClassroom(null);
    }

    private void showDetails(Classroom classroom) {
        if (classroom == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phòng học");
        alert.setHeaderText("Thông tin phòng học: " + classroom.getRoomName());

        StringBuilder content = new StringBuilder();
        content.append("Mã phòng: ").append(classroom.getCode()).append("\n");
        content.append("Tên phòng: ").append(classroom.getRoomName()).append("\n");
        content.append("Tầng: ").append(classroom.getFloor()).append("\n");
        content.append("Sức chứa: ").append(classroom.getCapacity()).append(" người\n");
        content.append("Trạng thái: ").append(classroom.getStatus());

        alert.setContentText(content.toString());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private void editClassroom(Classroom classroom) {
        boolean isEditMode = classroom != null;
        Dialog<Classroom> dialog = new Dialog<>();
        dialog.setTitle(isEditMode ? "Chỉnh sửa phòng học" : "Thêm phòng học mới");
        dialog.setHeaderText(isEditMode ? "Chỉnh sửa thông tin phòng: " + classroom.getRoomName() : "Nhập thông tin phòng học mới");

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtCode = new TextField(isEditMode ? classroom.getCode() : "");
        txtCode.setPromptText("Ví dụ: P101");
        TextField txtRoomName = new TextField(isEditMode ? classroom.getRoomName() : "");
        txtRoomName.setPromptText("Ví dụ: Phòng học lý thuyết 1");
        Spinner<Integer> spnFloor = new Spinner<>(1, 20, isEditMode ? classroom.getFloor() : 1);
        Spinner<Integer> spnCapacity = new Spinner<>(10, 500, isEditMode ? classroom.getCapacity() : 30, 5);
        ComboBox<String> cmbStatusEditor = new ComboBox<>();
        cmbStatusEditor.setItems(FXCollections.observableArrayList("Active", "Inactive", "Maintenance"));
        if (isEditMode) {
            cmbStatusEditor.setValue(classroom.getStatus());
        } else {
            cmbStatusEditor.setValue("Active");
        }

        grid.add(new Label("Mã phòng (*):"), 0, 0); grid.add(txtCode, 1, 0);
        grid.add(new Label("Tên phòng (*):"), 0, 1); grid.add(txtRoomName, 1, 1);
        grid.add(new Label("Tầng:"), 0, 2); grid.add(spnFloor, 1, 2);
        grid.add(new Label("Sức chứa:"), 0, 3); grid.add(spnCapacity, 1, 3);
        grid.add(new Label("Trạng thái:"), 0, 4); grid.add(cmbStatusEditor, 1, 4);

        dialog.getDialogPane().setContent(grid);
        txtCode.requestFocus();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (txtCode.getText().trim().isEmpty() || txtRoomName.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Mã phòng và Tên phòng không được để trống.", null);
                    return null;
                }

                // Create or update the classroom object
                Classroom result;
                if (isEditMode) {
                    result = classroom; // Use existing object to keep ID
                } else {
                    result = new Classroom(); // Create new
                }

                result.setCode(txtCode.getText().trim());
                result.setRoomName(txtRoomName.getText().trim());
                result.setFloor(spnFloor.getValue());
                result.setCapacity(spnCapacity.getValue());
                result.setStatus(cmbStatusEditor.getValue());

                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newClassroom -> {
            boolean success;
            if (isEditMode) {
                success = controller.saveClassroom(newClassroom);
                if (success) {
                    refreshView();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật phòng học thành công.", null);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật phòng học.", "Có thể mã phòng đã tồn tại hoặc có lỗi xảy ra.");
                }
            } else {
                success = controller.saveClassroom(newClassroom);
                if (success) {
                    refreshView();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thêm phòng học mới thành công.", null);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm phòng học mới.", "Có thể mã phòng đã tồn tại hoặc có lỗi xảy ra.");
                }
            }
        });
    }

    private void deleteClassroom(Classroom classroom) {
        if (classroom == null) return;

        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Xác nhận xóa");
        confirmationDialog.setHeaderText("Xóa phòng học " + classroom.getRoomName());
        confirmationDialog.setContentText("Bạn có chắc chắn muốn xóa phòng học này không?");

        // Customize buttons
        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Hủy bỏ", ButtonBar.ButtonData.NO);
        confirmationDialog.getButtonTypes().setAll(btnYes, btnNo);

        confirmationDialog.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                boolean success = controller.deleteClassroom(classroom.getRoomId());
                if (success) {
                    refreshView();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xóa phòng học thành công.", null);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa phòng học.", "Phòng học có thể đang được sử dụng hoặc có lỗi xảy ra.");
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        if (content != null && !content.isEmpty()) {
            alert.setContentText(content);
        }
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    @Override
    public void refreshView() {
        // Lấy các giá trị từ filter
        String keyword = txtKeyword.getText().trim();
        String statusFilter = cmbStatusFilter.getValue();
        if ("Tất cả".equals(statusFilter)) {
            statusFilter = null;
        }

        tblClassroom.setItems(controller.getFilteredClassrooms(keyword, statusFilter));
        tblClassroom.refresh();
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    // Method to set black header text
    private void setBlackHeaderText(TableColumn<Classroom, ?> column, String text) {
        Label label = new Label(text);
        label.setTextFill(Color.BLACK);
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        column.setGraphic(label);
    }

}

