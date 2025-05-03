package view.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import view.BaseScreenView;

/**
 * Màn hình quản lý phòng học
 */
public class RoomView extends BaseScreenView {

    // Các thành phần UI
    private TextField txtKeyword;
    private ComboBox<String> cmbStatus;
    private ComboBox<String> cmbPageSize;
    private TableView<PhongHoc> tblPhongHoc;
    private FilteredList<PhongHoc> filteredData;

    public RoomView() {
        super("Phòng học", "classrooms");
    }

    @Override
    public void initializeView() {
        // Tạo layout chính với padding
        root.setSpacing(10);
        root.setPadding(new Insets(20));

        // Tạo tiêu đề màn hình
        Label lblTitle = new Label("Phòng học");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#1E88E5"));

        // Tạo vùng tìm kiếm và lọc
        HBox searchBar = createSearchBar();
        searchBar.setPadding(new Insets(10));
        searchBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5; -fx-background-radius: 5;");

        // Tạo bảng hiển thị danh sách phòng học
        createTableView();

        // Thêm tất cả vào layout chính
        root.getChildren().addAll(lblTitle, searchBar, tblPhongHoc);
        VBox.setVgrow(tblPhongHoc, Priority.ALWAYS);

        // Nạp dữ liệu mẫu vào bảng
        loadSampleData();
    }

    private HBox createSearchBar() {
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // Trường từ khóa
        Label lblKeyword = new Label("Từ khóa:");
        lblKeyword.setMinWidth(70);
        lblKeyword.setTextFill(Color.BLACK);
        txtKeyword = new TextField();
        txtKeyword.setPromptText("Từ khóa");
        txtKeyword.setPrefWidth(350);

        // Combo box trạng thái
        Label lblStatus = new Label("Trạng thái:");
        lblStatus.setTextFill(Color.BLACK);
        cmbStatus = new ComboBox<>();
        cmbStatus.setPromptText("Chọn");
        cmbStatus.setItems(FXCollections.observableArrayList("Tất cả", "Đang sử dụng", "Bảo trì", "Không sử dụng"));
        cmbStatus.setPrefWidth(200);

        // Combo box kích thước trang
        Label lblPageSize = new Label("Cỡ trang:");
        lblPageSize.setTextFill(Color.BLACK);
        cmbPageSize = new ComboBox<>();
        cmbPageSize.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        cmbPageSize.setValue("20");
        cmbPageSize.setPrefWidth(120);

        // Nút tìm kiếm với biểu tượng tìm kiếm đơn giản
        Button btnSearch = new Button();
        Text searchIcon = new Text("🔍"); // Unicode magnifying glass
        searchIcon.setFill(Color.WHITE);
        btnSearch.setGraphic(searchIcon);
        btnSearch.setStyle("-fx-background-color: #1E88E5; -fx-text-fill: white;");
        btnSearch.setPrefSize(40, 10);

        // Thêm sự kiện tìm kiếm
        btnSearch.setOnAction(e -> performSearch());
        txtKeyword.setOnAction(e -> performSearch());

        // Thêm khoảng trống linh hoạt trước nút tìm kiếm
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchBar.getChildren().addAll(lblKeyword, txtKeyword, lblStatus, cmbStatus, lblPageSize, cmbPageSize, spacer, btnSearch);
        return searchBar;
    }

    private void createTableView() {
        tblPhongHoc = new TableView<>();
        tblPhongHoc.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Cột STT
        TableColumn<PhongHoc, Integer> colSTT = new TableColumn<>("STT");
        colSTT.setCellValueFactory(new PropertyValueFactory<>("stt"));
        colSTT.setMaxWidth(70);
        colSTT.setMinWidth(50);
        colSTT.setSortable(false);

        // Cột Mã
        TableColumn<PhongHoc, String> colMa = new TableColumn<>("Mã");
        colMa.setCellValueFactory(new PropertyValueFactory<>("ma"));
        colMa.setMaxWidth(100);
        colMa.setMinWidth(80);

        // Cột Tên
        TableColumn<PhongHoc, String> colTen = new TableColumn<>("Tên");
        colTen.setCellValueFactory(new PropertyValueFactory<>("ten"));

        // Cột Tầng
        TableColumn<PhongHoc, Integer> colTang = new TableColumn<>("Tầng");
        colTang.setCellValueFactory(new PropertyValueFactory<>("tang"));
        colTang.setMaxWidth(100);
        colTang.setMinWidth(80);

        // Cột Sức chứa
        TableColumn<PhongHoc, Integer> colSucChua = new TableColumn<>("Sức chứa");
        colSucChua.setCellValueFactory(new PropertyValueFactory<>("sucChua"));
        colSucChua.setMaxWidth(100);
        colSucChua.setMinWidth(80);

        // Cột Trạng thái
        TableColumn<PhongHoc, String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("Sử dụng");
        });
        colTrangThai.setMaxWidth(150);
        colTrangThai.setMinWidth(120);

        // Cài đặt cell factory cho cột trạng thái để hiển thị nút
        colTrangThai.setCellFactory(column -> {
            return new TableCell<>() {
                final Button button = new Button("Sử dụng");

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty) {
                        setGraphic(null);
                    } else {
                        button.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 20;");
                        button.setPrefWidth(100);
                        setGraphic(button);
                    }
                }
            };
        });

        // Cột Chi tiết
        TableColumn<PhongHoc, Void> colChiTiet = new TableColumn<>("Chi tiết");
        colChiTiet.setCellFactory(column -> {
            return new TableCell<>() {
                final Button button = new Button();

                {
                    // Tạo biểu tượng "eye" đơn giản
                    HBox eyeIcon = createEyeIcon();
                    button.setGraphic(eyeIcon);
                    button.setStyle("-fx-background-color: transparent;");

                    button.setOnAction(event -> {
                        PhongHoc data = getTableView().getItems().get(getIndex());
                        showDetails(data);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : button);
                }
            };
        });
        colChiTiet.setMaxWidth(80);
        colChiTiet.setMinWidth(80);
        colChiTiet.setSortable(false);

        // Thêm các cột vào bảng
        tblPhongHoc.getColumns().addAll(colSTT, colMa, colTen, colTang, colSucChua, colTrangThai, colChiTiet);
    }

    /**
     * Tạo biểu tượng con mắt đơn giản thay thế cho FontAwesome
     */
    private HBox createEyeIcon() {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);

        // Tạo hình tròn ngoài
        Circle outerCircle = new Circle(9, Color.web("#2E7D32"));
        outerCircle.setStroke(Color.web("#2E7D32"));
        outerCircle.setStrokeWidth(1.5);
        outerCircle.setFill(Color.TRANSPARENT);

        // Tạo hình tròn trong (đồng tử)
        Circle innerCircle = new Circle(3, Color.web("#2E7D32"));

        // Xếp chồng các hình lên nhau
        container.getChildren().addAll(outerCircle, innerCircle);

        return container;
    }

    private void loadSampleData() {
        // Tạo dữ liệu mẫu
        ObservableList<PhongHoc> data = FXCollections.observableArrayList(
                new PhongHoc(1, "101", "Phòng sinh hoạt", 1, 7),
                new PhongHoc(2, "201", "Phòng 201", 2, 25),
                new PhongHoc(3, "202", "Phòng 202", 2, 30),
                new PhongHoc(4, "301", "Phòng 301", 3, 27),
                new PhongHoc(5, "302", "Phòng 302", 3, 30),
                new PhongHoc(6, "401", "Phòng 401", 4, 25),
                new PhongHoc(7, "402", "Phòng 402", 4, 30)
        );

        // Tạo filtered list để tìm kiếm
        filteredData = new FilteredList<>(data, p -> true);
        tblPhongHoc.setItems(filteredData);
    }

    private void performSearch() {
        String keyword = txtKeyword.getText().toLowerCase();
        String status = cmbStatus.getValue();

        filteredData.setPredicate(phongHoc -> {
            // Nếu không có từ khóa hoặc trạng thái thì hiện tất cả
            if (keyword == null || keyword.isEmpty()) {
                return true;
            }

            // Lọc theo từ khóa
            if (phongHoc.getMa().toLowerCase().contains(keyword) ||
                    phongHoc.getTen().toLowerCase().contains(keyword) ||
                    String.valueOf(phongHoc.getTang()).contains(keyword)) {
                return true;
            }

            return false;
        });
    }

    private void showDetails(PhongHoc phongHoc) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phòng học");
        alert.setHeaderText("Thông tin phòng học: " + phongHoc.getTen());

        String content = "Mã phòng: " + phongHoc.getMa() + "\n" +
                "Tên phòng: " + phongHoc.getTen() + "\n" +
                "Tầng: " + phongHoc.getTang() + "\n" +
                "Sức chứa: " + phongHoc.getSucChua() + " người";

        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void refreshView() {
        // Làm mới dữ liệu khi cần
        // loadData();
    }

    @Override
    public boolean requiresAuthentication() {
        // Yêu cầu xác thực để truy cập màn hình này
        return true;
    }

    /**
     * Lớp đối tượng dữ liệu Phòng học
     */
    public static class PhongHoc {
        private final int stt;
        private final String ma;
        private final String ten;
        private final int tang;
        private final int sucChua;

        public PhongHoc(int stt, String ma, String ten, int tang, int sucChua) {
            this.stt = stt;
            this.ma = ma;
            this.ten = ten;
            this.tang = tang;
            this.sucChua = sucChua;
        }

        public int getStt() { return stt; }
        public String getMa() { return ma; }
        public String getTen() { return ten; }
        public int getTang() { return tang; }
        public int getSucChua() { return sucChua; }
    }
}
